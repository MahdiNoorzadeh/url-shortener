export type CreateUrlResponse = {
  shortCode: string
  shortUrl: string
  expiresAt?: string | null
}

export type UrlStats = {
  shortCode: string
  originalUrl: string
  clickCount: number
  createdAt: string
  expiresAt?: string | null
}

export type ApiErrorBody = {
  status?: number
  errorCode?: string
  message?: string
  timestamp?: string
}

const apiBase = ((import.meta as ImportMeta & { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/+$/, '')
const REQUEST_TIMEOUT_MS = 20_000
const SLOW_REQUEST_MS = 4_000

export class ApiError extends Error {
  constructor(message: string, public readonly kind: 'http' | 'network' | 'timeout', public readonly status?: number, public readonly errorCode?: string) {
    super(message)
    this.name = 'ApiError'
  }
}

type RequestOptions = { method?: string; body?: unknown; onSlow?: () => void; signal?: AbortSignal }

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort('timeout'), REQUEST_TIMEOUT_MS)
  const slow = window.setTimeout(() => options.onSlow?.(), SLOW_REQUEST_MS)
  const abortFromCaller = () => controller.abort(options.signal?.reason)
  options.signal?.addEventListener('abort', abortFromCaller, { once: true })

  try {
    let response: Response
    try {
      response = await fetch(`${apiBase}${path}`, {
        method: options.method ?? 'GET',
        headers: options.body ? { 'Content-Type': 'application/json' } : undefined,
        body: options.body ? JSON.stringify(options.body) : undefined,
        signal: controller.signal,
      })
    } catch {
      if (controller.signal.aborted) {
        if (controller.signal.reason === 'timeout' || !options.signal?.aborted) {
          throw new ApiError('The request took too long. The service may be waking up or temporarily unavailable.', 'timeout')
        }
      }
      throw new ApiError('Could not reach the API. Check your connection and whether the backend is running.', 'network')
    }

    if (!response.ok) {
      let body: ApiErrorBody = {}
      try { body = await response.json() as ApiErrorBody } catch { /* response may not contain JSON */ }
      const safeMessage = body.errorCode === 'URL_NOT_FOUND' ? 'This short link was not found.'
        : body.errorCode === 'URL_EXPIRED' ? 'This short link has expired.'
        : body.errorCode === 'INVALID_EXPIRATION_TIME' ? 'Choose an expiration time in the future.'
        : body.errorCode === 'VALIDATION_ERROR' ? (body.message?.slice(0, 240) || 'Check the submitted URL and try again.')
        : body.errorCode === 'INVALID_REQUEST' ? 'The API could not read the request. Check the input and try again.'
        : response.status === 404 ? 'This short link was not found.'
        : response.status === 410 ? 'This short link has expired.'
        : `The API returned HTTP ${response.status}.`
      throw new ApiError(safeMessage, 'http', response.status, body.errorCode)
    }
    return await response.json() as T
  } finally {
    window.clearTimeout(timeout)
    window.clearTimeout(slow)
    options.signal?.removeEventListener('abort', abortFromCaller)
  }
}

export function createShortUrl(url: string, expiresAt?: string, onSlow?: () => void) {
  return request<CreateUrlResponse>('/api/v1/urls', { method: 'POST', body: { url, ...(expiresAt ? { expiresAt } : {}) }, onSlow })
}

export function getUrlStats(shortCode: string, onSlow?: () => void) {
  return request<UrlStats>(`/api/v1/urls/${encodeURIComponent(shortCode)}/stats`, { onSlow })
}

export function getApiBaseUrl() { return apiBase }
