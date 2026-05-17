import { z } from 'zod'
import { unstable_noStore as noStore } from 'next/cache'

/**
 * - RSC/Node 서버: 백엔드로 직접 fetch (Next 자기 자신 3000번으로 루프백하면 Docker 등에서 실패할 수 있음).
 * - 브라우저: 동일 출처 `/api/proxy` → Route Handler 가 백엔드로 전달.
 */
function normalizeServerBackendBase(url: string): string {
  try {
    const u = new URL(url)
    if (u.hostname === 'localhost') u.hostname = '127.0.0.1'
    return u.href.replace(/\/$/, '')
  } catch {
    return url
  }
}

/**
 * 서버 전용: Spring `/api` 베이스.
 * - Docker 프론트 컨테이너: INTERNAL_API_BASE_URL=http://backend:8080/api + RUNNING_IN_DOCKER=true
 * - 호스트에서 pnpm dev: `backend` 호스트명이 없어 fetch 실패 → RUNNING_IN_DOCKER 가 아니면 127.0.0.1 로 치환
 */
function serverBackendBase(): string {
  let raw =
    process.env.INTERNAL_API_BASE_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    'http://127.0.0.1:8080/api'

  const inDocker = process.env.RUNNING_IN_DOCKER === 'true'
  if (!inDocker) {
    try {
      const u = new URL(raw)
      if (u.hostname === 'backend') {
        u.hostname = '127.0.0.1'
        raw = u.toString()
      }
    } catch {
      /* ignore */
    }
  }

  return normalizeServerBackendBase(raw)
}

function joinUrl(path: string): string {
  const p = path.startsWith('/') ? path : `/${path}`
  if (typeof window === 'undefined') {
    return `${serverBackendBase()}${p}`
  }
  return `/api/proxy${p}`
}

function isDockerBackendUrl(url: string): boolean {
  return process.env.RUNNING_IN_DOCKER === 'true' && url.startsWith('http://backend:8080/')
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function fetchWithStartupRetry(url: string, init: RequestInit): Promise<Response> {
  const attempts = isDockerBackendUrl(url) ? 12 : 1
  let lastError: unknown
  for (let i = 0; i < attempts; i += 1) {
    try {
      return await fetch(url, init)
    } catch (e) {
      lastError = e
      if (i === attempts - 1) break
      await sleep(1000)
    }
  }
  throw lastError instanceof Error ? lastError : new Error(String(lastError))
}

const TRADING_MODE_HEADER = 'X-Trading-Mode'
const USER_ID_HEADER = 'X-User-Id'

function backendAuthHeaders(): HeadersInit {
  const username = process.env.BACKEND_BASIC_USERNAME
  const password = process.env.BACKEND_BASIC_PASSWORD
  if (!username || !password || typeof window !== 'undefined') return {}
  return { Authorization: `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}` }
}

async function tradingModeHeaders(overrideMode?: 'paper' | 'real'): Promise<HeadersInit> {
  if (typeof window === 'undefined') {
    const { cookies } = await import('next/headers')
    const store = await cookies()
    const m = store.get('trading-mode')?.value
    const userId = store.get('user-id')?.value?.trim()
    const fromCookie = m === 'real' || m === 'paper' ? m : 'paper'
    const mode = overrideMode ?? fromCookie
    return {
      [TRADING_MODE_HEADER]: mode,
      ...(userId ? { [USER_ID_HEADER]: userId } : {}),
      ...backendAuthHeaders(),
    }
  }
  const match = document.cookie.match(/(?:^|; )trading-mode=([^;]*)/)
  const v = match?.[1]
  const userMatch = document.cookie.match(/(?:^|; )user-id=([^;]*)/)
  const userId = decodeURIComponent(userMatch?.[1] || '').trim()
  const fromCookie = v === 'real' || v === 'paper' ? v : 'paper'
  const mode = overrideMode ?? fromCookie
  return {
    [TRADING_MODE_HEADER]: mode,
    ...(userId ? { [USER_ID_HEADER]: userId } : {}),
  }
}

const apiEnvelopeSchema = z.object({
  success: z.boolean().optional(),
  data: z.unknown().optional(),
  message: z.string().nullable().optional(),
})

export function parseApiData<T>(text: string, schema?: z.ZodType<T>): T {
  const envelope = apiEnvelopeSchema.parse(JSON.parse(text))
  const data = envelope.data
  return schema ? schema.parse(data) : (data as T)
}

export async function apiGet<T>(path: string, schema?: z.ZodType<T>): Promise<T> {
  if (typeof window === 'undefined') noStore()
  const url = joinUrl(path)
  let res: Response
  try {
    res = await fetchWithStartupRetry(url, { cache: 'no-store', headers: await tradingModeHeaders() })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(`API 연결 실패: ${url} — 백엔드(8080) 기동·INTERNAL_API_BASE_URL(Docker) 확인. (${msg})`, {
      cause: e,
    })
  }
  const text = await res.text()
  if (!res.ok) {
    let detail = ''
    try {
      const j = JSON.parse(text) as { message?: string }
      if (j?.message) detail = `: ${j.message}`
    } catch {
      /* ignore */
    }
    throw new Error(`API HTTP ${res.status}: ${url}${detail}`)
  }
  if (!text) return undefined as T
  try {
    return parseApiData<T>(text, schema)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(`API 응답 형식이 올바르지 않습니다 (HTTP ${res.status}): ${msg}`)
  }
}

export async function apiPost<T>(
  path: string,
  body: unknown,
  schema?: z.ZodType<T>,
  opts?: { tradingMode?: 'paper' | 'real' }
): Promise<T> {
  if (typeof window === 'undefined') noStore()
  const url = joinUrl(path)
  let res: Response
  try {
    res = await fetchWithStartupRetry(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(await tradingModeHeaders(opts?.tradingMode)),
      },
      body: JSON.stringify(body),
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(`API 연결 실패: ${url} (${msg})`, { cause: e })
  }
  const text = await res.text()
  if (!res.ok) {
    let detail = ''
    try {
      const j = JSON.parse(text) as { message?: string }
      if (j?.message) detail = `: ${j.message}`
    } catch {
      /* ignore */
    }
    throw new Error(`API HTTP ${res.status}: ${url}${detail}`)
  }
  if (!text) return undefined as T
  try {
    return parseApiData<T>(text, schema)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(`API 응답 형식이 올바르지 않습니다 (HTTP ${res.status}): ${msg}`)
  }
}

export async function apiPut<T>(
  path: string,
  body: unknown,
  schema?: z.ZodType<T>,
  opts?: { tradingMode?: 'paper' | 'real' }
): Promise<T> {
  if (typeof window === 'undefined') noStore()
  const url = joinUrl(path)
  const res = await fetchWithStartupRetry(url, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...(await tradingModeHeaders(opts?.tradingMode)),
    },
    body: JSON.stringify(body),
  })
  const text = await res.text()
  if (!res.ok) {
    let detail = ''
    try {
      const j = JSON.parse(text) as { message?: string }
      if (j?.message) detail = `: ${j.message}`
    } catch {
      /* ignore */
    }
    throw new Error(`API HTTP ${res.status}: ${url}${detail}`)
  }
  if (!text) return undefined as T
  try {
    return parseApiData<T>(text, schema)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(`API 응답 형식이 올바르지 않습니다 (HTTP ${res.status}): ${msg}`)
  }
}

export async function apiDelete<T>(path: string, schema?: z.ZodType<T>): Promise<T> {
  if (typeof window === 'undefined') noStore()
  const url = joinUrl(path)
  const res = await fetchWithStartupRetry(url, {
    method: 'DELETE',
    cache: 'no-store',
    headers: await tradingModeHeaders(),
  })
  const text = await res.text()
  if (!res.ok) {
    let detail = ''
    try {
      const j = JSON.parse(text) as { message?: string }
      if (j?.message) detail = `: ${j.message}`
    } catch {
      /* ignore */
    }
    throw new Error(`API HTTP ${res.status}: ${url}${detail}`)
  }
  if (!text) return undefined as T
  try {
    return parseApiData<T>(text, schema)
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    throw new Error(`API 응답 형식이 올바르지 않습니다 (HTTP ${res.status}): ${msg}`)
  }
}
