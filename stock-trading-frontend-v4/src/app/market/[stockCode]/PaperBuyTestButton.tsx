'use client'

import { useState } from 'react'
import { apiPost } from '@/lib/api'

/** KIS 주문 본문 (output 등 중첩 가능) */
type KisBody = { rt_cd?: string; msg1?: string; msg2?: string; [key: string]: unknown }

/** 백엔드 `ApiResponse.data` — 주문 API */
type BrokerOrderData = {
  attemptId: number
  clientOrderKey: string
  kis: KisBody
}

function readRtCd(k: KisBody | undefined): string {
  if (!k || typeof k !== 'object') return ''
  if (k.rt_cd != null) return String(k.rt_cd)
  const out = (k as { output?: { rt_cd?: string } }).output
  if (out && typeof out === 'object' && out.rt_cd != null) return String(out.rt_cd)
  return ''
}

export function PaperBuyTestButton({ stockCode, stockName }: { stockCode: string; stockName: string }) {
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const [qtyInput, setQtyInput] = useState('1')
  const [reason, setReason] = useState('')

  function parseQty(): number {
    const n = parseInt(qtyInput, 10)
    if (Number.isNaN(n)) return 1
    return Math.max(1, Math.min(99999, n))
  }

  function buildPath(side: 'buy' | 'sell'): string {
    const q = parseQty()
    const base = `/broker/kis/order/${side}?pdno=${encodeURIComponent(stockCode)}&qty=${q}`
    const r = reason.trim()
    if (!r) return base
    return `${base}&reason=${encodeURIComponent(r)}`
  }

  async function place(side: 'buy' | 'sell') {
    const verb = side === 'buy' ? '매수' : '매도'
    const q = parseQty()
    if (
      !window.confirm(
        `모의투자로 ${stockName}(${stockCode}) 시장가 ${q}주 ${verb} API를 호출할까요?\n(이 버튼은 항상 모의투자만 사용합니다. 상단 투자모드와 별도입니다.)`
      )
    ) {
      return
    }
    setLoading(true)
    setMsg(null)
    setErr(null)
    try {
      const path = buildPath(side)
      const data = await apiPost<BrokerOrderData>(path, {}, undefined, { tradingMode: 'paper' })
      const kis = data?.kis
      const code = readRtCd(kis)
      const attempt = data?.attemptId != null ? ` · 시도 #${data.attemptId}` : ''
      if (String(code) === '0') {
        setMsg(`모의투자 시장가 ${verb} 접수( rt_cd=0 )${attempt}`)
      } else {
        const m1 = kis?.msg1 != null ? String(kis.msg1) : ''
        setMsg(
          m1 ||
            `KIS: rt_cd=${code || '—'}${attempt} — ${JSON.stringify(kis ?? data).slice(0, 500)}`
        )
      }
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <label style={{ display: 'block', fontSize: 12, color: '#8b9dc7', marginBottom: 4 }}>
        수량 (기본 1, 백엔드 기본값 없음)
      </label>
      <input
        type="number"
        className="input"
        min={1}
        max={99999}
        value={qtyInput}
        onChange={(e) => setQtyInput(e.target.value)}
        style={{ maxWidth: 120, marginBottom: 10 }}
      />
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 10 }}>
        <button type="button" className="button" disabled={loading} onClick={() => place('buy')}>
          {loading ? '요청 중…' : '모의 시장가 매수'}
        </button>
        <button
          type="button"
          className="button button--danger"
          disabled={loading}
          onClick={() => place('sell')}
        >
          {loading ? '요청 중…' : '모의 시장가 매도'}
        </button>
      </div>
      <label style={{ display: 'block', fontSize: 12, color: '#8b9dc7', marginBottom: 4 }}>
        주문 사유(선택) — DB·이력에 reasoning으로 저장
      </label>
      <textarea
        className="input"
        rows={2}
        placeholder="예: 수동 점검, 전략 메모, JSON 한 줄…"
        value={reason}
        onChange={(e) => setReason(e.target.value)}
        style={{ resize: 'vertical', minHeight: 48, fontFamily: 'inherit' }}
      />
      {msg ? <p style={{ color: '#027a48', marginTop: 8, fontSize: 14 }}>{msg}</p> : null}
      {err ? <p style={{ color: '#b42318', marginTop: 8, fontSize: 14 }}>{err}</p> : null}
      <p style={{ color: '#667085', fontSize: 12, marginTop: 6 }}>
        로그인(쿠키 user-id)과 설정에 저장한 <b>모의투자</b> KIS appKey / appSecret / 계좌가 있어야 합니다.
      </p>
    </div>
  )
}
