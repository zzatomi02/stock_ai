'use client'

import { useCallback, useEffect, useState } from 'react'
import { OnOffBadge } from '@/components/strategies/OnOffBadge'
import { apiGet } from '@/lib/api'
import { notifyError } from '@/lib/toast'
import { marketConditionLabel, strategyLabel, timeWindowLabel } from '@/lib/strategy-labels'

type StrategyItem = {
  strategyType: string
  enabled: boolean
  marketWeightMultiplier?: number | string
  timeWeightMultiplier?: number | string
  finalWeightMultiplier?: number | string
  minScore?: number
  selectedReason?: string
  excludedReason?: string
}

type EnabledNow = {
  tradeDate: string
  currentTime: string
  marketCondition: string
  marketTimeWindow: string
  strategies: StrategyItem[]
}

function num(v: number | string | null | undefined) {
  if (v == null) return '—'
  const n = typeof v === 'number' ? v : parseFloat(String(v))
  return Number.isFinite(n) ? n.toFixed(2) : String(v)
}

export function EnabledStrategiesNowPanel() {
  const [data, setData] = useState<EnabledNow | null>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setData(await apiGet<EnabledNow>('/strategies/enabled-now'))
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    const t = setInterval(load, 60_000)
    return () => clearInterval(t)
  }, [load])

  const enabled = data?.strategies.filter((s) => s.enabled) ?? []
  const excluded = data?.strategies.filter((s) => !s.enabled) ?? []

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <p style={{ margin: 0, color: '#667085', fontSize: 14 }}>1분마다 자동 갱신</p>
        <button className="button" type="button" disabled={loading} onClick={load}>
          새로고침
        </button>
      </div>
      {data ? (
        <>
          <div
            style={{
              marginTop: 16,
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
              gap: 12,
            }}
          >
            <InfoCard label="현재 시간" value={data.currentTime} />
            <InfoCard label="거래일" value={data.tradeDate} />
            <InfoCard label="시장 상태" value={marketConditionLabel(data.marketCondition)} />
            <InfoCard label="시간대" value={timeWindowLabel(data.marketTimeWindow)} />
          </div>
          <section style={{ marginTop: 24 }}>
            <h3 style={{ marginBottom: 12 }}>실행 가능 전략 ({enabled.length})</h3>
            {enabled.length === 0 ? (
              <p style={{ color: '#667085' }}>현재 실행 가능한 전략이 없습니다.</p>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {enabled.map((s) => (
                  <li
                    key={s.strategyType}
                    style={{
                      padding: '12px 14px',
                      marginBottom: 8,
                      borderRadius: 10,
                      background: '#ECFDF3',
                      border: '1px solid #ABEFC6',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                      <strong>{strategyLabel(s.strategyType)}</strong>
                      <OnOffBadge on />
                      <span style={{ fontSize: 13, color: '#067647' }}>
                        최종 가중치 {num(s.finalWeightMultiplier)}
                      </span>
                    </div>
                    <p style={{ marginTop: 6, marginBottom: 0, fontSize: 13, color: '#475467' }}>
                      시장 {num(s.marketWeightMultiplier)} × 시간 {num(s.timeWeightMultiplier)}
                      {s.minScore != null ? ` · 최소점수 ${s.minScore}` : ''}
                    </p>
                    {s.selectedReason ? (
                      <p style={{ marginTop: 4, marginBottom: 0, fontSize: 12, color: '#667085' }}>{s.selectedReason}</p>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </section>
          <section style={{ marginTop: 24 }}>
            <h3 style={{ marginBottom: 12 }}>제외 전략 ({excluded.length})</h3>
            {excluded.length === 0 ? (
              <p style={{ color: '#667085' }}>제외된 전략이 없습니다.</p>
            ) : (
              <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
                {excluded.map((s) => (
                  <li
                    key={s.strategyType}
                    style={{
                      padding: '12px 14px',
                      marginBottom: 8,
                      borderRadius: 10,
                      background: '#F9FAFB',
                      border: '1px solid #EAECF0',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <strong>{strategyLabel(s.strategyType)}</strong>
                      <OnOffBadge on={false} />
                    </div>
                    <p style={{ marginTop: 4, marginBottom: 0, fontSize: 13, color: '#B42318' }}>
                      {s.excludedReason ?? '비활성'}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      ) : (
        <p style={{ marginTop: 12, color: '#667085' }}>{loading ? '불러오는 중…' : '데이터 없음'}</p>
      )}
    </>
  )
}

function InfoCard({ label, value }: { label: string; value: string }) {
  return (
    <article
      style={{
        padding: 14,
        borderRadius: 10,
        background: '#F9FAFB',
        border: '1px solid #EAECF0',
      }}
    >
      <p style={{ margin: 0, fontSize: 12, color: '#667085' }}>{label}</p>
      <p style={{ margin: '4px 0 0', fontSize: 18, fontWeight: 700 }}>{value}</p>
    </article>
  )
}
