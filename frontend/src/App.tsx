import { useEffect, useMemo, useRef, useState } from 'react'
import { Activity, ArrowDownRight, ArrowUpRight, Check, Clipboard, Clock3, ExternalLink, FileDown, FileUp, Link2, LoaderCircle, MoreHorizontal, MousePointer2, Plus, Search, ShieldCheck, Trash2, X } from 'lucide-react'
import { ApiError, createShortUrl, getApiBaseUrl, getUrlStats, type UrlStats } from './lib/api'
import { exportDashboard, importDashboard, readDashboard, writeDashboard, type SavedLink } from './lib/storage'
import { startPolling } from './lib/polling'

type Notice = { id: number; type: 'success' | 'warning' | 'error'; message: string }

function App() {
  const initial = useMemo(() => readDashboard(), [])
  const [links, setLinks] = useState<SavedLink[]>(initial.links)
  const [storageWarning, setStorageWarning] = useState(initial.warning ?? '')
  const [url, setUrl] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [formError, setFormError] = useState('')
  const [canRetry, setCanRetry] = useState(false)
  const [busy, setBusy] = useState(false)
  const [slow, setSlow] = useState(false)
  const [notices, setNotices] = useState<Notice[]>([])
  const [search, setSearch] = useState('')
  const [activeTab, setActiveTab] = useState<'all' | 'active' | 'expired'>('all')
  const [renameId, setRenameId] = useState<string | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const [stats, setStats] = useState<Record<string, UrlStats>>({})
  const [statsLoading, setStatsLoading] = useState<string | null>(null)
  const [importError, setImportError] = useState('')
  const fileInput = useRef<HTMLInputElement>(null)
  const noticeSequence = useRef(0)

  function notify(type: Notice['type'], message: string) {
    setNotices((current) => {
      if (current.some((notice) => notice.message === message)) return current
      const next = [...current, { id: ++noticeSequence.current, type, message }]
      return next.slice(-3)
    })
  }

  useEffect(() => {
    if (storageWarning) return
    try { writeDashboard(links) } catch {
      setStorageWarning('Browser storage is unavailable or full. Your current changes may not persist after refresh.')
    }
  }, [links, storageWarning]) // preserve unreadable data instead of overwriting it

  

  useEffect(() => {
    let cancelled = false

    async function loadAllStats() {
      const results = await Promise.all(
        links.map(async (link) => {
          try {
            const result = await getUrlStats(link.shortCode)
            return { shortCode: link.shortCode, result }
          } catch {
            // Keep existing statistics if one request fails.
            return null
          }
        }),
      )

      if (cancelled) return

      setStats((current) => {
        const next = { ...current }

        for (const item of results) {
          if (item) {
            next[item.shortCode] = item.result
          }
        }

        return next
      })
    }

    if (links.length === 0) return

    const stopPolling = startPolling(loadAllStats, 5_000)

    return () => {
      cancelled = true
      stopPolling()
    }
  }, [links])



  const filtered = links.filter((link) => {
    const q = search.toLowerCase()
    const matchesSearch = !q || `${link.displayName} ${link.originalUrl} ${link.shortUrl}`.toLowerCase().includes(q)
    const expired = Boolean(link.expiresAt && Date.parse(link.expiresAt) <= Date.now())
    return matchesSearch && (activeTab === 'all' || (activeTab === 'expired' ? expired : !expired))
  })
  const totalClicks = Object.values(stats).reduce((sum, item) => sum + item.clickCount, 0)

  function validateUrl(value: string) {
    try {
      const parsed = new URL(value)
      return (parsed.protocol === 'http:' || parsed.protocol === 'https:') && Boolean(parsed.hostname)
    } catch { return false }
  }

  async function handleCreate(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFormError('')
    setCanRetry(false)
    if (!validateUrl(url.trim())) { setFormError('Enter a complete URL beginning with https:// or http://.'); return }
    if (expiresAt && !Number.isFinite(Date.parse(expiresAt))) { setFormError('Choose a valid expiration date and time.'); return }
    if (expiresAt && Date.parse(expiresAt) <= Date.now()) { setFormError('Expiration must be in the future.'); return }
    setBusy(true); setSlow(false)
    try {
      const expirationIso = expiresAt ? new Date(expiresAt).toISOString() : undefined
      const result = await createShortUrl(url.trim(), expirationIso, () => setSlow(true))
      const saved: SavedLink = {
        id: crypto.randomUUID(), shortCode: result.shortCode, shortUrl: result.shortUrl,
        originalUrl: url.trim(), displayName: new URL(url.trim()).hostname,
        createdAt: new Date().toISOString(), expiresAt: result.expiresAt ?? expirationIso ?? null,
      }
      setLinks((current) => [saved, ...current.filter((item) => item.shortCode !== saved.shortCode)])
      setUrl(''); setExpiresAt('')
      notify('success', 'Short link created and saved to this browser.')
    } catch (error) {
      const message = error instanceof ApiError ? error.message : 'Something went wrong while creating the link.'
      setFormError(message)
      setCanRetry(error instanceof ApiError && error.kind === 'timeout')
      notify(error instanceof ApiError && error.kind === 'network' ? 'warning' : 'error', message)
    } finally { setBusy(false); setSlow(false) }
  }

  async function copy(value: string) {
    try { await navigator.clipboard.writeText(value); notify('success', 'Copied to clipboard.') }
    catch { notify('error', 'Clipboard access failed. Select and copy the link manually.') }
  }

  function rename(link: SavedLink) {
    const name = renameValue.trim()
    if (!name) { notify('warning', 'Display name cannot be empty.'); return }
    setLinks((current) => current.map((item) => item.id === link.id ? { ...item, displayName: name.slice(0, 160) } : item))
    setRenameId(null); notify('success', 'Display name updated.')
  }

  function remove(link: SavedLink) {
    if (!window.confirm(`Remove “${link.displayName}” from this browser dashboard? The server-side short link will not be deleted.`)) return
    setLinks((current) => current.filter((item) => item.id !== link.id))
    setStats((current) => { const next = { ...current }; delete next[link.shortCode]; return next })
    notify('success', 'Removed from this browser dashboard. The short link still exists on the server.')
  }

  async function loadStats(link: SavedLink) {
    setStatsLoading(link.shortCode)
    try { const result = await getUrlStats(link.shortCode, () => notify('warning', 'Statistics are taking longer than usual; the service may be waking up.')); setStats((current) => ({ ...current, [link.shortCode]: result })) }
    catch (error) { notify('error', error instanceof Error ? error.message : 'Could not load statistics.') }
    finally { setStatsLoading(null) }
  }

  function downloadExport() {
    const blob = new Blob([exportDashboard(links)], { type: 'application/json' })
    const href = URL.createObjectURL(blob); const anchor = document.createElement('a')
    anchor.href = href; anchor.download = `shortly-dashboard-${new Date().toISOString().slice(0, 10)}.json`; anchor.click(); URL.revokeObjectURL(href)
    notify('success', 'Dashboard export downloaded.')
  }

  async function handleImport(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]; if (!file) return
    setImportError('')
    try {
      const parsed = importDashboard(await file.text())
      if (!window.confirm(`Import ${parsed.length} links? This replaces the current browser dashboard.`)) return
      setLinks(parsed); setStorageWarning(''); notify('success', `Imported ${parsed.length} links.`)
    } catch (error) { setImportError(error instanceof Error ? error.message : 'Could not import this file.') }
    finally { event.target.value = '' }
  }

  const activeCount = links.filter((link) => !link.expiresAt || Date.parse(link.expiresAt) > Date.now()).length

  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="#top" aria-label="Shortly home"><span className="brand-mark"><Link2 size={21}/></span><span>shortly<span className="brand-dot">.</span></span></a>
      <div className="workspace-label">WORKSPACE</div>
      <button className="nav-item selected"><Activity size={17}/> Overview <span className="nav-pill">01</span></button>
      <button className="nav-item" onClick={() => document.getElementById('links-library')?.scrollIntoView({ behavior: 'smooth' })}><Link2 size={17}/> My links</button>
      <div className="sidebar-bottom">
        <div className="local-card"><ShieldCheck size={18}/><div><strong>Private by default</strong><p>Your dashboard lives in this browser. No account or sync.</p></div></div>
        <div className="user-chip"><div className="avatar">S</div><div><strong>Personal workspace</strong><span>Local dashboard</span></div><MoreHorizontal size={18}/></div>
      </div>
    </aside>

    <main id="top" className="main-content">
      <header className="topbar"><div className="breadcrumb">Workspace <span>/</span> <strong>Overview</strong></div><div className="topbar-right"><span className="connection-dot"/> <span>API configured</span><button className="icon-button" title="API origin" onClick={() => notify('warning', `API origin: ${getApiBaseUrl()}`)}><Activity size={17}/></button></div></header>
      <div className="page-wrap">
        <section className="welcome-row"><div><div className="eyebrow">YOUR LINK WORKSPACE</div><h1>Make every link <span>count.</span></h1><p className="subheading">Create, organize, and keep track of your links — all in one place.</p></div><div className="date-chip"><Clock3 size={15}/>{new Intl.DateTimeFormat(undefined, { weekday: 'short', month: 'short', day: 'numeric' }).format(new Date())}</div></section>

        {storageWarning && <div className="inline-alert warning" role="alert"><ShieldCheck size={18}/><span>{storageWarning}</span></div>}

        <div className="inline-alert warning" role="status">
        <Activity size={18} />
        <span>
          This demo runs on free-tier hosting. If the backend has been idle,
          the first request may take longer while the server wakes up.
          Subsequent requests should be faster.
        </span>
      </div>

        <section className="stats-grid" aria-label="Dashboard summary">
          <div className="metric-card"><div className="metric-top"><span>Total links</span><span className="metric-icon blue"><Link2 size={18}/></span></div><div className="metric-number">{links.length.toLocaleString()}</div><div className="metric-foot"><span className="metric-note">Saved in this browser</span></div></div>
          <div className="metric-card"><div className="metric-top"><span>Active links</span><span className="metric-icon green"><ArrowUpRight size={18}/></span></div><div className="metric-number">{activeCount.toLocaleString()}</div><div className="metric-foot"><span className="status-dot green-dot"/>Not expired</div></div>
          <div className="metric-card"><div className="metric-top"><span>Tracked clicks</span><span className="metric-icon violet"><MousePointer2 size={18}/></span></div><div className="metric-number">{totalClicks.toLocaleString()}</div><div className="metric-foot"><span className="metric-note">For links whose stats you loaded</span></div></div>
        </section>

        <section className="create-panel">
          <div className="panel-heading"><div className="heading-icon"><Plus size={19}/></div><div><h2>Shorten a new link</h2><p>Paste a long URL and get a shareable short link in seconds.</p></div><span className="secure-label"><ShieldCheck size={14}/> No account needed</span></div>
          <form id="create-form" onSubmit={handleCreate} noValidate>
            <label htmlFor="long-url">Destination URL</label>
            <div className="url-input-wrap"><Link2 size={18}/><input id="long-url" type="url" placeholder="https://your-long-link.com/article" value={url} onChange={(e) => { setUrl(e.target.value); if (formError) setFormError(''); setCanRetry(false) }} disabled={busy} aria-describedby={formError ? 'form-error' : undefined}/><button className="primary-button" type="submit" disabled={busy}>{busy ? <><LoaderCircle className="spin" size={17}/> Creating…</> : <><Plus size={17}/> Create short link</>}</button></div>
            <div className="form-bottom"><div className="expiry-field"><label htmlFor="expiry">Expiration <span>(optional)</span></label><input id="expiry" type="datetime-local" value={expiresAt} onChange={(e) => setExpiresAt(e.target.value)} disabled={busy}/></div><div className="form-hint"><ShieldCheck size={15}/> Your links are stored on the server; dashboard names stay local.</div></div>
            {formError && <div className="form-error" id="form-error" role="alert"><X size={16}/><span>{formError}</span>{canRetry && <button className="retry-button" type="button" onClick={() => (document.getElementById('create-form') as HTMLFormElement | null)?.requestSubmit()}>Retry</button>}</div>}
            {busy && <div className="request-status" role="status" aria-live="polite"><LoaderCircle size={16} className="spin"/><span>{
              slow 
              ? 'The backend may be waking up after being idle on free-tier hosting. Please wait…'
               : 'Contacting the backend. The first request may take longer because the free-tier service may be waking up.'
              }</span></div>}
          </form>
        </section>

        <section className="library-section" id="links-library">
          <div className="library-heading"><div><h2>Your links <span className="count-badge">{links.length}</span></h2><p>Manage the links saved in this browser.</p></div><div className="library-actions"><button className="secondary-button" onClick={downloadExport}><FileDown size={16}/> Export</button><button className="secondary-button" onClick={() => fileInput.current?.click()}><FileUp size={16}/> Import</button><input ref={fileInput} type="file" accept="application/json,.json" hidden onChange={handleImport}/></div></div>
          {importError && <div className="inline-alert error" role="alert">{importError}<button onClick={() => setImportError('')} aria-label="Dismiss import error"><X size={16}/></button></div>}
          <div className="library-toolbar"><div className="tabs" role="tablist" aria-label="Filter links"><button className={activeTab === 'all' ? 'tab active' : 'tab'} onClick={() => setActiveTab('all')}>All links</button><button className={activeTab === 'active' ? 'tab active' : 'tab'} onClick={() => setActiveTab('active')}>Active</button><button className={activeTab === 'expired' ? 'tab active' : 'tab'} onClick={() => setActiveTab('expired')}>Expired</button></div><div className="search-wrap"><Search size={16}/><input aria-label="Search links" placeholder="Search links..." value={search} onChange={(e) => setSearch(e.target.value)}/></div></div>
          {filtered.length === 0 ? <div className="empty-state"><div className="empty-icon"><Link2 size={25}/></div><h3>{links.length === 0 ? 'Your link library is empty' : 'No links match your search'}</h3><p>{links.length === 0 ? 'Create your first short link above. It will appear here for easy access.' : 'Try another search or switch the filter.'}</p>{links.length === 0 && <button className="secondary-button" onClick={() => document.getElementById('long-url')?.focus()}><Plus size={16}/> Create your first link</button>}</div> : <div className="link-list">{filtered.map((link) => {
            const expired = Boolean(link.expiresAt && Date.parse(link.expiresAt) <= Date.now())
            const stat = stats[link.shortCode]
            return <article className="link-row" key={link.id}><div className="link-favicon">{link.displayName.slice(0, 1).toUpperCase()}</div><div className="link-details">{renameId === link.id ? <form className="rename-form" onSubmit={(e) => { e.preventDefault(); rename(link) }}><input autoFocus aria-label="Display name" maxLength={160} value={renameValue} onChange={(e) => setRenameValue(e.target.value)}/><button className="mini-action confirm" aria-label="Save name"><Check size={15}/></button><button type="button" className="mini-action" aria-label="Cancel rename" onClick={() => setRenameId(null)}><X size={15}/></button></form> : <div className="link-title-line"><h3>{link.displayName}</h3><button className="text-action" onClick={() => { setRenameId(link.id); setRenameValue(link.displayName) }}>Rename</button></div>}<a className="short-url" href={link.shortUrl} target="_blank" rel="noreferrer">{link.shortUrl} <ExternalLink size={12}/></a><p className="destination" title={link.originalUrl}>{link.originalUrl}</p><div className="link-meta"><span className={expired ? 'link-status expired' : 'link-status'}><span/> {expired ? 'Expired' : 'Active'}</span>{link.expiresAt && <span>Expires {new Date(link.expiresAt).toLocaleDateString()}</span>}{stat && <span><MousePointer2 size={12}/> {stat.clickCount} clicks</span>}</div></div><div className="row-actions"><button className="square-action" title="Copy short link" aria-label={`Copy ${link.displayName} short link`} onClick={() => copy(link.shortUrl)}><Clipboard size={16}/></button><button className="square-action" title="Load statistics" aria-label={`Load statistics for ${link.displayName}`} disabled={statsLoading === link.shortCode} onClick={() => loadStats(link)}>{statsLoading === link.shortCode ? <LoaderCircle size={16} className="spin"/> : <Activity size={16}/>}</button><button className="square-action danger-action" title="Remove from dashboard" aria-label={`Remove ${link.displayName}`} onClick={() => remove(link)}><Trash2 size={16}/></button></div></article>
          })}</div>}
          <div className="library-footer"><span>Showing {filtered.length} of {links.length} links</span><span><ShieldCheck size={14}/> Browser-local dashboard</span></div>
        </section>
        <footer className="footer"><span>Shortly <span className="brand-dot">.</span> Simple links, less clutter.</span><span>Dashboard data stays on this device <ArrowDownRight size={14}/></span></footer>
      </div>
    </main>
    <div className="toast-stack" aria-live="polite" aria-atomic="false">{notices.map((notice) => <div className={`toast ${notice.type}`} role={notice.type === 'error' ? 'alert' : 'status'} key={notice.id}><span className="toast-symbol">{notice.type === 'success' ? <Check size={17}/> : notice.type === 'warning' ? <Activity size={17}/> : <X size={17}/>}</span><span>{notice.message}</span><button onClick={() => setNotices((current) => current.filter((item) => item.id !== notice.id))} aria-label="Dismiss notification"><X size={15}/></button></div>)}</div>
  </div>
}

export default App
