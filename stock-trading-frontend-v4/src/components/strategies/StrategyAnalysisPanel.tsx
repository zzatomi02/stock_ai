'use client'

import { useCallback, useState } from 'react'
import { apiGet, apiPost } from '@/lib/api'
import { notifyError, notifyInfo, notifySuccess } from '@/lib/toast'

type SignalSummary = {
  id: number
  strategyCode: string
  stockCode?: string
  stockName?: string
  side?: string
  adjustedFinalScore: number | string
  signalGrade?: string
  status: string
  marketConditionLabel?: string
  marketTimeWindowLabel?: string
}

type SignalDetail = SignalSummary & {
  newsArticleId?: number
  tradeDate?: string
  marketCondition?: string
  marketTimeWindow?: string
  rawFinalScore: number | string
  marketWeightMultiplier: number | string
  timeWeightMultiplier: number | string
  finalWeightMultiplier: number | string
  minScoreThreshold?: number
  strategySelectedReason?: string | null
  strategyExcludedReason?: string | null
  reasonMessage?: string
  executionLog?: string
  createdAt?: string
}

type SignalsListResponse = {
  tradeDate?: string
  status?: string
  items: SignalSummary[]
}

function num(v: number | string | null | undefined): string {
  if (v == null) return '—'
  const n = typeof v === 'number' ? v : parseFloat(String(v))
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

export function StrategyAnalysisPanel() {
  const [articleId, setArticleId] = useState('')
  const [hours, setHours] = useState('24')
  const [loading, setLoading] = useState(false)
  const [signals, setSignals] = useState<SignalSummary[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [detail, setDetail] = useState<SignalDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)

  const loadSignals = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiGet<SignalsListResponse>('/strategies/analysis/signals?status=CANDIDATE')
      setSignals(data.items ?? [])
      notifyInfo(`오늘 CANDIDATE ${data.items?.length ?? 0}건`)
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }, [])

  async function openDetail(id: number) {
    setSelectedId(id)
    setDetailLoading(true)
    setDetail(null)
    try {
      const d = await apiGet<SignalDetail>(`/strategies/analysis/signals/${id}`)
      setDetail(d)
    } catch (error) {
      notifyError(error)
    } finally {
      setDetailLoading(false)
    }
  }

  function closeDetail() {
    setSelectedId(null)
    setDetail(null)
  }

  async function analyzeOne() {
    if (!articleId.trim()) return
    setLoading(true)
    try {
      await apiPost<unknown>(`/strategies/analysis/article/${articleId.trim()}`, {})
      notifySuccess('기사별 전략 분석 완료')
      await loadSignals()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function scanRecent() {
    setLoading(true)
    try {
      const data = await apiPost<{ candidateSignals?: number }>(
        `/strategies/analysis/scan-recent?hours=${hours}`,
        {},
      )
      notifyInfo(`최근 기사 스캔 완료 (후보 ${data.candidateSignals ?? 0}건)`)
      await loadSignals()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="card" style={{ marginTop: 12 }}>
      <h2>전략 분석 실행 (16단계 파이프라인)</h2>
      <p style={{ marginTop: 6, color: '#667085', fontSize: 14 }}>
        목록에는 <strong>adjusted 점수</strong>를 표시합니다. 상세·AI·Risk는{' '}
        <a href="/strategy-signals" style={{ color: '#1570EF' }}>
          전략 시그널
        </a>
        페이지를 이용하세요.
      </p>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12, alignItems: 'center' }}>
        <input
          value={articleId}
          onChange={(e) => setArticleId(e.target.value)}
          placeholder="뉴스 articleId"
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD', width: 140 }}
        />
        <button className="button" type="button" disabled={loading} onClick={analyzeOne}>
          기사 분석
        </button>
        <input
          value={hours}
          onChange={(e) => setHours(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD', width: 60 }}
        />
        <button className="button" type="button" disabled={loading} onClick={scanRecent}>
          최근 N시간 스캔
        </button>
        <button className="button" type="button" disabled={loading} onClick={loadSignals}>
          오늘 후보 조회
        </button>
      </div>
      {signals.length > 0 ? (
        <div style={{ marginTop: 16, overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #EAECF0', textAlign: 'left' }}>
                <th style={{ padding: '8px 10px' }}>종목</th>
                <th style={{ padding: '8px 10px' }}>전략</th>
                <th style={{ padding: '8px 10px' }}>adjusted</th>
                <th style={{ padding: '8px 10px' }}>등급</th>
                <th style={{ padding: '8px 10px' }}>시장</th>
                <th style={{ padding: '8px 10px' }}>시간대</th>
              </tr>
            </thead>
            <tbody>
              {signals.map((s) => (
                <tr
                  key={s.id}
                  onClick={() => openDetail(s.id)}
                  style={{
                    borderBottom: '1px solid #F2F4F7',
                    cursor: 'pointer',
                    background: selectedId === s.id ? '#F0F9FF' : undefined,
                  }}
                >
                  <td style={{ padding: '8px 10px' }}>{s.stockName ?? s.stockCode ?? '—'}</td>
                  <td style={{ padding: '8px 10px' }}>{s.strategyCode}</td>
                  <td style={{ padding: '8px 10px', fontWeight: 600 }}>{num(s.adjustedFinalScore)}</td>
                  <td style={{ padding: '8px 10px' }}>{s.signalGrade ?? '—'}</td>
                  <td style={{ padding: '8px 10px', color: '#667085' }}>{s.marketConditionLabel ?? '—'}</td>
                  <td style={{ padding: '8px 10px', color: '#667085' }}>{s.marketTimeWindowLabel ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {selectedId != null ? (
        <div
          style={{
            marginTop: 16,
            padding: 16,
            borderRadius: 12,
            border: '1px solid #D0D5DD',
            background: '#FAFAFA',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h3 style={{ margin: 0, fontSize: 16 }}>가중치 적용 상세</h3>
            <button className="button" type="button" onClick={closeDetail}>
              닫기
            </button>
          </div>
          {detailLoading ? (
            <p style={{ marginTop: 12, color: '#667085' }}>불러오는 중…</p>
          ) : detail ? (
            <div style={{ marginTop: 12, fontSize: 14, lineHeight: 1.7 }}>
              <p>
                <strong>{detail.stockName}</strong> · {detail.strategyCode} · {detail.status}
              </p>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                  gap: 12,
                  marginTop: 12,
                }}
              >
                <ScoreBox label="raw 점수" value={num(detail.rawFinalScore)} />
                <ScoreBox label="시장 가중치" value={num(detail.marketWeightMultiplier)} />
                <ScoreBox label="시간 가중치" value={num(detail.timeWeightMultiplier)} />
                <ScoreBox label="최종 배수" value={num(detail.finalWeightMultiplier)} highlight />
                <ScoreBox label="adjusted 점수" value={num(detail.adjustedFinalScore)} highlight />
              </div>
              <p style={{ marginTop: 12, color: '#475467' }}>
                계산: adjusted = min(100, raw × 시장가중치 × 시간가중치) = min(100,{' '}
                {num(detail.rawFinalScore)} × {num(detail.marketWeightMultiplier)} ×{' '}
                {num(detail.timeWeightMultiplier)}) → <strong>{num(detail.adjustedFinalScore)}</strong>
              </p>
              <p style={{ color: '#667085' }}>
                시장: {detail.marketConditionLabel ?? detail.marketCondition} · 시간:{' '}
                {detail.marketTimeWindowLabel ?? detail.marketTimeWindow}
              </p>
              {detail.strategySelectedReason ? (
                <p style={{ marginTop: 8 }}>
                  <span style={{ color: '#067647' }}>선정 사유:</span> {detail.strategySelectedReason}
                </p>
              ) : null}
              {detail.strategyExcludedReason ? (
                <p style={{ marginTop: 8 }}>
                  <span style={{ color: '#B42318' }}>제외 사유:</span> {detail.strategyExcludedReason}
                </p>
              ) : null}
              {detail.executionLog ? (
                <pre
                  style={{
                    marginTop: 12,
                    padding: 12,
                    background: '#fff',
                    borderRadius: 8,
                    fontSize: 11,
                    overflow: 'auto',
                    maxHeight: 200,
                  }}
                >
                  {detail.executionLog}
                </pre>
              ) : null}
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  )
}

function ScoreBox({
  label,
  value,
  highlight,
}: {
  label: string
  value: string
  highlight?: boolean
}) {
  return (
    <div
      style={{
        padding: 12,
        borderRadius: 8,
        background: highlight ? '#EFF8FF' : '#fff',
        border: `1px solid ${highlight ? '#B2DDFF' : '#EAECF0'}`,
      }}
    >
      <div style={{ fontSize: 12, color: '#667085' }}>{label}</div>
      <div style={{ fontSize: 20, fontWeight: 700, marginTop: 4 }}>{value}</div>
    </div>
  )
}
