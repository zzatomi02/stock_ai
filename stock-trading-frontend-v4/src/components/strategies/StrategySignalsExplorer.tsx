'use client'

import Link from 'next/link'
import { useCallback, useEffect, useState } from 'react'
import { apiGet, apiPost } from '@/lib/api'

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
  ruleBasedScore?: number
  aiOverallScore?: number | string
  aiBuyScore?: number | string
  aiRiskScore?: number | string
  aiBlendedScore?: number | string
  aiDecision?: string
  aiSummary?: string
  aiCompanyAnalysisId?: number
  riskCheckPassed?: boolean
  riskBlockReason?: string
  createdAt?: string
}

type SignalsListResponse = {
  tradeDate?: string
  status?: string
  items: SignalSummary[]
}

const STATUS_TABS = ['CANDIDATE', 'REJECTED', 'EXCLUDED'] as const

function num(v: number | string | null | undefined): string {
  if (v == null) return '—'
  const n = typeof v === 'number' ? v : parseFloat(String(v))
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

function statusColor(status: string): string {
  if (status === 'CANDIDATE') return '#067647'
  if (status === 'REJECTED') return '#B42318'
  return '#B54708'
}

export function StrategySignalsExplorer() {
  const [status, setStatus] = useState<(typeof STATUS_TABS)[number]>('CANDIDATE')
  const [signals, setSignals] = useState<SignalSummary[]>([])
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState('')
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [detail, setDetail] = useState<SignalDetail | null>(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [articleId, setArticleId] = useState('')

  const loadSignals = useCallback(async () => {
    setLoading(true)
    setMsg('')
    try {
      const data = await apiGet<SignalsListResponse>(
        `/strategies/analysis/signals?status=${status}`,
      )
      setSignals(data.items ?? [])
      setMsg(`${data.tradeDate ?? '오늘'} · ${status} ${data.items?.length ?? 0}건`)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [status])

  useEffect(() => {
    loadSignals()
    setSelectedId(null)
    setDetail(null)
  }, [loadSignals])

  async function openDetail(id: number) {
    setSelectedId(id)
    setDetailLoading(true)
    setDetail(null)
    try {
      const d = await apiGet<SignalDetail>(`/strategies/analysis/signals/${id}`)
      setDetail(d)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setDetailLoading(false)
    }
  }

  async function analyzeArticle() {
    if (!articleId.trim()) return
    setLoading(true)
    try {
      await apiPost(`/strategies/analysis/article/${articleId.trim()}`, {})
      setMsg('기사 분석 완료')
      await loadSignals()
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="card">
      <h1>전략 시그널</h1>
      <p style={{ marginTop: 8, color: '#667085', fontSize: 14 }}>
        <code>strategy_signal</code> — 전략별 Rule·Risk·AI 블렌드 결과입니다.{' '}
        <Link href="/signals" style={{ color: '#1570EF' }}>
          레거시 매매 시그널
        </Link>
        과는 별도입니다.
      </p>

      <div style={{ marginTop: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            className={status === tab ? 'tab tab--active' : 'tab'}
            disabled={loading}
            onClick={() => setStatus(tab)}
          >
            {tab}
          </button>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12, alignItems: 'center' }}>
        <input
          value={articleId}
          onChange={(e) => setArticleId(e.target.value)}
          placeholder="뉴스 articleId"
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD', width: 140 }}
        />
        <button className="button" type="button" disabled={loading} onClick={analyzeArticle}>
          기사 분석
        </button>
        <button className="button" type="button" disabled={loading} onClick={loadSignals}>
          새로고침
        </button>
      </div>

      {msg ? <p style={{ marginTop: 10, color: '#475467', fontSize: 14 }}>{msg}</p> : null}

      {signals.length > 0 ? (
        <div style={{ marginTop: 16, overflowX: 'auto' }}>
          <table className="table" style={{ fontSize: 14 }}>
            <thead>
              <tr>
                <th>종목</th>
                <th>전략</th>
                <th>방향</th>
                <th>점수</th>
                <th>등급</th>
                <th>상태</th>
                <th>시장</th>
                <th>시간대</th>
              </tr>
            </thead>
            <tbody>
              {signals.map((s) => (
                <tr
                  key={s.id}
                  onClick={() => openDetail(s.id)}
                  style={{
                    cursor: 'pointer',
                    background: selectedId === s.id ? '#F0F9FF' : undefined,
                  }}
                >
                  <td>{s.stockName ?? s.stockCode ?? '—'}</td>
                  <td>{s.strategyCode}</td>
                  <td>{s.side ?? '—'}</td>
                  <td style={{ fontWeight: 600 }}>{num(s.adjustedFinalScore)}</td>
                  <td>{s.signalGrade ?? '—'}</td>
                  <td style={{ color: statusColor(s.status), fontWeight: 500 }}>{s.status}</td>
                  <td style={{ color: '#667085' }}>{s.marketConditionLabel ?? '—'}</td>
                  <td style={{ color: '#667085' }}>{s.marketTimeWindowLabel ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <DetailPanel
            selectedId={selectedId}
            detailLoading={detailLoading}
            detail={detail}
            onClose={() => {
              setSelectedId(null)
              setDetail(null)
            }}
          />
        </div>
      ) : !loading ? (
        <p style={{ marginTop: 16, color: '#667085' }}>표시할 시그널이 없습니다.</p>
      ) : null}
    </section>
  )
}

function DetailPanel({
  selectedId,
  detailLoading,
  detail,
  onClose,
}: {
  selectedId: number | null
  detailLoading: boolean
  detail: SignalDetail | null
  onClose: () => void
}) {
  if (selectedId == null) return null
  return (
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
        <h3 style={{ margin: 0, fontSize: 16 }}>시그널 상세 #{selectedId}</h3>
        <button className="button" type="button" onClick={onClose}>
          닫기
        </button>
      </div>
      {detailLoading ? (
        <p style={{ marginTop: 12, color: '#667085' }}>불러오는 중…</p>
      ) : detail ? (
        <div style={{ marginTop: 12, fontSize: 14, lineHeight: 1.7 }}>
          <p>
            <strong>{detail.stockName}</strong> ({detail.stockCode}) · {detail.strategyCode} ·{' '}
            <span style={{ color: statusColor(detail.status) }}>{detail.status}</span>
          </p>
          {detail.newsArticleId ? (
            <p style={{ color: '#667085' }}>
              뉴스 기사 ID: {detail.newsArticleId} · 거래일 {detail.tradeDate ?? '—'}
            </p>
          ) : null}

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(140px, 1fr))',
              gap: 10,
              marginTop: 12,
            }}
          >
            <ScoreBox label="Rule 점수" value={detail.ruleBasedScore != null ? String(detail.ruleBasedScore) : '—'} />
            <ScoreBox label="raw" value={num(detail.rawFinalScore)} />
            <ScoreBox label="adjusted" value={num(detail.adjustedFinalScore)} highlight />
            <ScoreBox label="AI blended" value={num(detail.aiBlendedScore)} highlight />
            <ScoreBox label="시장 가중" value={num(detail.marketWeightMultiplier)} />
            <ScoreBox label="시간 가중" value={num(detail.timeWeightMultiplier)} />
          </div>

          <div style={{ marginTop: 16, padding: 12, background: '#fff', borderRadius: 8, border: '1px solid #EAECF0' }}>
            <strong>AI · Risk</strong>
            <p style={{ margin: '8px 0 0' }}>
              결정: {detail.aiDecision ?? '—'} · overall {num(detail.aiOverallScore)} · buy{' '}
              {num(detail.aiBuyScore)} · risk {num(detail.aiRiskScore)}
            </p>
            <p style={{ margin: '4px 0 0', color: '#667085' }}>
              리스크 통과: {detail.riskCheckPassed === true ? '예' : detail.riskCheckPassed === false ? '아니오' : '—'}
              {detail.riskBlockReason ? ` — ${detail.riskBlockReason}` : ''}
            </p>
            {detail.aiSummary ? (
              <p style={{ marginTop: 8, whiteSpace: 'pre-wrap' }}>{detail.aiSummary}</p>
            ) : null}
            {detail.aiCompanyAnalysisId ? (
              <p style={{ marginTop: 6, fontSize: 13 }}>
                <Link href={`/ai/company-analysis?stock=${detail.stockCode}`} style={{ color: '#1570EF' }}>
                  AI 분석 #{detail.aiCompanyAnalysisId}
                </Link>
              </p>
            ) : null}
          </div>

          {detail.strategySelectedReason ? (
            <p style={{ marginTop: 12 }}>
              <span style={{ color: '#067647' }}>선정:</span> {detail.strategySelectedReason}
            </p>
          ) : null}
          {detail.strategyExcludedReason ? (
            <p style={{ marginTop: 8 }}>
              <span style={{ color: '#B42318' }}>제외:</span> {detail.strategyExcludedReason}
            </p>
          ) : null}
          {detail.reasonMessage ? (
            <p style={{ marginTop: 8, color: '#475467' }}>{detail.reasonMessage}</p>
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
                maxHeight: 240,
              }}
            >
              {detail.executionLog}
            </pre>
          ) : null}
        </div>
      ) : null}
    </div>
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
        padding: 10,
        borderRadius: 8,
        background: highlight ? '#EFF8FF' : '#fff',
        border: `1px solid ${highlight ? '#B2DDFF' : '#EAECF0'}`,
      }}
    >
      <div style={{ fontSize: 11, color: '#667085' }}>{label}</div>
      <div style={{ fontSize: 18, fontWeight: 700, marginTop: 4 }}>{value}</div>
    </div>
  )
}
