export const STORAGE_KEY = 'shortly.dashboard.v1'
export const EXPORT_VERSION = 1

export type SavedLink = {
  id: string
  shortCode: string
  shortUrl: string
  originalUrl: string
  displayName: string
  createdAt: string
  expiresAt: string | null
}

type DashboardFile = { version: 1; exportedAt?: string; links: SavedLink[] }

function isString(value: unknown, max = 5000): value is string {
  return typeof value === 'string' && value.length <= max
}

function validLink(value: unknown): value is SavedLink {
  if (!value || typeof value !== 'object') return false
  const v = value as Record<string, unknown>
  return isString(v.id, 100) && v.id.length > 0 && isString(v.shortCode, 100) && /^[a-zA-Z0-9_-]+$/.test(v.shortCode)
    && isString(v.shortUrl) && isSafeHttpUrl(v.shortUrl) && isString(v.originalUrl) && isSafeHttpUrl(v.originalUrl)
    && isString(v.displayName, 160) && isString(v.createdAt, 100)
    && (v.expiresAt === null || isString(v.expiresAt, 100))
}

export function isSafeHttpUrl(value: string): boolean {
  try { const url = new URL(value); return url.protocol === 'http:' || url.protocol === 'https:' } catch { return false }
}

export function parseLinks(value: unknown): SavedLink[] {
  if (!Array.isArray(value) || value.length > 5000 || !value.every(validLink)) throw new Error('The dashboard data is invalid or contains unsupported entries.')
  const seen = new Set<string>()
  return value.filter((link) => { if (seen.has(link.id)) return false; seen.add(link.id); return true })
}

export function readDashboard(): { links: SavedLink[]; warning?: string } {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return { links: [] }
    const parsed: unknown = JSON.parse(raw)
    if (!parsed || typeof parsed !== 'object' || (parsed as { version?: unknown }).version !== 1) throw new Error('unsupported version')
    return { links: parseLinks((parsed as { links?: unknown }).links) }
  } catch {
    return { links: [], warning: 'Your saved dashboard could not be read. Existing browser data was left untouched; export or recover it before saving new changes.' }
  }
}

export function writeDashboard(links: SavedLink[]): void {
  const valid = parseLinks(links)
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ version: 1, links: valid }))
}

export function exportDashboard(links: SavedLink[]): string {
  return JSON.stringify({ version: EXPORT_VERSION, exportedAt: new Date().toISOString(), links: parseLinks(links) }, null, 2)
}

export function importDashboard(text: string): SavedLink[] {
  if (text.length > 5_000_000) throw new Error('The import file is too large.')
  const parsed: unknown = JSON.parse(text)
  if (!parsed || typeof parsed !== 'object' || (parsed as DashboardFile).version !== EXPORT_VERSION) throw new Error('Unsupported dashboard export version.')
  return parseLinks((parsed as DashboardFile).links)
}
