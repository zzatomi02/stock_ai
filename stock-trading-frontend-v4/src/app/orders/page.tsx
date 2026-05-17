'use client'

import { useCallback, useEffect, useState } from 'react'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet } from '@/lib/api'

type DailyCcnlRow = {
  orderDate?: string
  orderTime?: string
  stockCode?: string
  stockName?: string
  sideLabel?: string
  sellBuyCode?: string
  quantity?: number
  price?: number
  rawJsonSubset?: string
}

type ExecutionsView = {
  raw?: unknown
  rows?: DailyCcnlRow[]
}

type OrderAttempt = {
  id: number
  clientOrderKey: string
  stockCode: string
  side: string
  quantity: number
  mode: string
  rawResponse?: string | null
  reasoning?: string | null
  createdAt?: string
}

function fmtTime(iso?: string): string {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
}

export default function OrdersPage() {
  const [exec, setExec] = useState<ExecutionsView | null>(null)
  const [orders, setOrders] = useState<OrderAttempt[]>([])
  const [ready, setReady] = useState(false)
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)

  const load = useCallback(async () => {
    setErr(null)
    setLoading(true)
    try {
      const [v, o] = await Promise.all([
        apiGet<ExecutionsView>('/broker/kis/executions/daily/view'),
        apiGet<OrderAttempt[]>('/broker/kis/orders/recent?limit=50'),
      ])
      setExec(v ?? null)
      setOrders(Array.isArray(o) ? o : [])
    } catch (e) {
      setExec({ rows: [] })
      setOrders([])
      setErr(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
      setReady(true)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const rows = exec?.rows ?? []
  const list = orders

  return (
    <AppShell>
      <section className="card">
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, marginBottom: 16 }}>
          <h1 style={{ margin: 0, flex: '1 1 auto' }}>주문 / 체결 내역</h1>
          <button type="button" className="button button--ghost" disabled={loading} onClick={() => void load()}>
            {loading ? '불러오는 중…' : '새로고침'}
          </button>
        </div>
        {err ? (
          <p style={{ color: '#f87171', fontSize: 14, marginBottom: 12 }}>
            {err}
            <br />
            <span style={{ color: '#9fb2d9', fontSize: 12 }}>
              KIS 권한·쿠키(로그인)·모의/실전 설정을 확인하세요. 백엔드가 8080에서 떠 있어야 합니다.
            </span>
          </p>
        ) : null}

        <h2 style={{ fontSize: 16, margin: '0 0 8px' }}>앱에 기록된 주문 시도(최근)</h2>
        <p style={{ color: '#8b9dc7', fontSize: 12, marginBottom: 8 }}>
          시장가 매수·매도 시도마다 DB에 남깁니다. <code>reasoning</code>은 주문 시 선택 입력한 사유입니다.
        </p>
        <div style={{ overflowX: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>시간</th>
                <th>종목</th>
                <th>구분</th>
                <th>수량</th>
                <th>모드</th>
                <th>시도 ID</th>
                <th>사유</th>
              </tr>
            </thead>
            <tbody>
              {!ready || loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                    불러오는 중…
                  </td>
                </tr>
              ) : list.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                    기록이 없습니다. 종목 상세에서 모의 시장가 주문을 실행해 보세요.
                  </td>
                </tr>
              ) : (
                list.map((a) => (
                  <tr key={a.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{fmtTime(a.createdAt)}</td>
                    <td>{a.stockCode}</td>
                    <td>{a.side}</td>
                    <td>{a.quantity}</td>
                    <td>{a.mode}</td>
                    <td style={{ fontSize: 12, fontFamily: 'ui-monospace, monospace' }}>{a.id}</td>
                    <td style={{ maxWidth: 280, fontSize: 12, wordBreak: 'break-word' }}>
                      {a.reasoning ? a.reasoning : '—'}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card" style={{ marginTop: 20 }}>
        <h2 style={{ fontSize: 16, margin: '0 0 8px' }}>KIS 일별 체결(파싱 뷰)</h2>
        <p style={{ color: '#8b9dc7', fontSize: 12, marginBottom: 8 }}>
          증권사 HTS와 동일 API 기준의 당일 체결을 표로 정리한 값입니다. KIS 응답이 비어 있으면 행이 없을 수
          있습니다.
        </p>
        <div style={{ overflowX: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>일자</th>
                <th>시간</th>
                <th>종목코드</th>
                <th>종목명</th>
                <th>매매</th>
                <th>가격</th>
                <th>수량</th>
              </tr>
            </thead>
            <tbody>
              {!ready || loading ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                    불러오는 중…
                  </td>
                </tr>
              ) : rows.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                    체결 행이 없습니다(당일 체결이 없거나, 아직 집계되지 않았을 수 있습니다).
                  </td>
                </tr>
              ) : (
                rows.map((r, i) => (
                  <tr key={`${r.orderDate}-${r.orderTime}-${r.stockCode}-${i}`}>
                    <td>{r.orderDate ?? '—'}</td>
                    <td>{r.orderTime ?? '—'}</td>
                    <td>{r.stockCode ?? '—'}</td>
                    <td>{r.stockName ?? '—'}</td>
                    <td>{r.sideLabel ?? r.sellBuyCode ?? '—'}</td>
                    <td style={{ textAlign: 'right' }}>{r.price != null ? r.price.toLocaleString() : '—'}</td>
                    <td style={{ textAlign: 'right' }}>{r.quantity != null ? r.quantity : '—'}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <details style={{ marginTop: 12 }}>
          <summary style={{ cursor: 'pointer', color: '#8b9dc7', fontSize: 13 }}>KIS 원문 JSON(raw) 보기</summary>
          <pre
            style={{
              marginTop: 8,
              padding: 12,
              background: '#0a1020',
              borderRadius: 12,
              fontSize: 11,
              overflow: 'auto',
              maxHeight: 240,
            }}
          >
            {exec?.raw != null ? JSON.stringify(exec.raw, null, 2) : '—'}
          </pre>
        </details>
      </section>
    </AppShell>
  )
}
