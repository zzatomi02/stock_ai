import { NextRequest, NextResponse } from 'next/server'

export const dynamic = 'force-dynamic'

const DEFAULT_ALLOWED_PREFIXES =
  'auth,market,dashboard,strategies,strategy,strategy-settings,strategy-runtime-settings,strategy-time-windows,strategy-config-changes,market-condition-strategies,news,statistics,backtest,ai,broker,ops,signals,watchlist,risk,llm,orders,trading-flow,disclosures'

function backendBase(): string {
  return (
    process.env.INTERNAL_API_BASE_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL ||
    'http://127.0.0.1:8080/api'
  ).replace(/\/$/, '')
}

function allowedPrefixes(): string[] {
  return (process.env.PROXY_ALLOWED_PREFIXES || DEFAULT_ALLOWED_PREFIXES)
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

async function forwardRequest(
  req: NextRequest,
  context: { params: Promise<{ path: string[] }> },
  method: string,
) {
  const { path } = await context.params
  const suffix = resolveSuffix(path)
  if (!suffix) {
    return NextResponse.json(
      { success: false, data: null, message: '허용되지 않은 프록시 경로입니다.' },
      { status: 403 },
    )
  }
  const url = `${backendBase()}/${suffix}${req.nextUrl.search}`
  const hasBody = method !== 'GET' && method !== 'HEAD'
  try {
    const res = await fetch(url, {
      method,
      headers: proxyHeaders(req, hasBody ? req.headers.get('content-type') || 'application/json' : null),
      body: hasBody ? await req.text() : undefined,
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

export async function GET(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forwardRequest(req, context, 'GET')
}

export async function POST(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forwardRequest(req, context, 'POST')
}

export async function PUT(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forwardRequest(req, context, 'PUT')
}

export async function PATCH(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forwardRequest(req, context, 'PATCH')
}

export async function DELETE(req: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return forwardRequest(req, context, 'DELETE')
}
