import { NextRequest, NextResponse } from 'next/server'

export const dynamic = 'force-dynamic'

function backendBase(): string {
  return (
    process.env.INTERNAL_API_BASE_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    'http://127.0.0.1:8080/api'
  ).replace(/\/$/, '')
}

function allowedPrefixes(): string[] {
  return (
    process.env.PROXY_ALLOWED_PREFIXES ||
    'auth,market,dashboard,strategies,strategy,news,statistics,backtest,ai,broker,ops'
  )
    .split(',')
    .map((prefix) => prefix.trim().replace(/^\/+|\/+$/g, ''))
    .filter(Boolean)
}

function resolveSuffix(path: string[] | undefined): string | null {
  if (!path?.length || path.some((segment) => segment === '..' || segment.includes('/'))) {
    return null
  }

  const suffix = path.join('/')
  const first = path[0]
  return allowedPrefixes().includes(first) ? suffix : null
}

function forwardTradingMode(req: NextRequest): HeadersInit {
  const tm = req.headers.get('x-trading-mode')
  if (tm === 'paper' || tm === 'real') return { 'X-Trading-Mode': tm }
  return {}
}

function forwardUserId(req: NextRequest): HeadersInit {
  const userId = req.headers.get('x-user-id')?.trim()
  if (!userId) return {}
  return { 'X-User-Id': userId }
}

function backendAuthHeaders(): HeadersInit {
  const username = process.env.BACKEND_BASIC_USERNAME
  const password = process.env.BACKEND_BASIC_PASSWORD
  if (!username || !password) return {}
  return { Authorization: `Basic ${Buffer.from(`${username}:${password}`).toString('base64')}` }
}

function proxyHeaders(req: NextRequest, contentType?: string | null): HeadersInit {
  return {
    ...(contentType ? { 'content-type': contentType } : {}),
    ...forwardTradingMode(req),
    ...forwardUserId(req),
    ...backendAuthHeaders(),
  }
}

export async function GET(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params
  const suffix = resolveSuffix(path)
  if (!suffix) {
    return NextResponse.json({ success: false, data: null, message: '허용되지 않은 프록시 경로입니다.' }, { status: 403 })
  }
  const url = `${backendBase()}/${suffix}${req.nextUrl.search}`
  try {
    const res = await fetch(url, { cache: 'no-store', headers: proxyHeaders(req) })
    const text = await res.text()
    return new NextResponse(text, {
      status: res.status,
      headers: { 'content-type': res.headers.get('content-type') || 'application/json' },
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return NextResponse.json(
      { success: false, data: null, message: `백엔드 연결 실패: ${url} (${msg})` },
      { status: 502 },
    )
  }
}

export async function POST(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  const { path } = await context.params
  const suffix = resolveSuffix(path)
  if (!suffix) {
    return NextResponse.json({ success: false, data: null, message: '허용되지 않은 프록시 경로입니다.' }, { status: 403 })
  }
  const url = `${backendBase()}/${suffix}${req.nextUrl.search}`
  try {
    const body = await req.text()
    const res = await fetch(url, {
      method: 'POST',
      headers: proxyHeaders(req, req.headers.get('content-type') || 'application/json'),
      body: body || undefined,
      cache: 'no-store',
    })
    const text = await res.text()
    return new NextResponse(text, {
      status: res.status,
      headers: { 'content-type': res.headers.get('content-type') || 'application/json' },
    })
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e)
    return NextResponse.json(
      { success: false, data: null, message: `백엔드 연결 실패: ${url} (${msg})` },
      { status: 502 },
    )
  }
}
