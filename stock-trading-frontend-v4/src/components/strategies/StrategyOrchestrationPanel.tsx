'use client'

import { useCallback, useEffect, useState } from 'react'
import { apiGet, apiPatch, apiPut } from '@/lib/api'

type Snapshot = {
  evaluatedAt: string
  tradeDate: string
  marketMoodScore: number
  marketRegime: string
  marketRegimeLabel: string
  currentTimeSlot: { code: string; label: string } | null
  activeBuyStrategies: string[]
  strategies: Array<{
    code: string
    label: string
    category: string
    baseEnabled: boolean
    todayOverride: boolean
    todayEnabled: boolean
    marketRegime: string
    marketWeight: number
    timeSlotLabel: string
    timeWeight: number
    combinedWeight: number
    effectiveEnabled: boolean
    allowBuy: boolean
    reasonSummary: string
  }>
}

export function StrategyOrchestrationPanel() {
  const [snap, setSnap] = useState<Snapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setMsg('')
    try {
      const data = await apiGet<Snapshot>('/strategies/orchestration/snapshot')
      setSnap(data)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function toggleBase(code: string, enabled: boolean) {
    setLoading(true)
    try {
      await apiPatch(`/strategies/orchestration/${code}/enabled?enabled=${enabled}`)
      await load()
      setMsg(`${code} 기본 ${enabled ? 'ON' : 'OFF'}`)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  async function toggleToday(code: string, enabled: boolean) {
    setLoading(true)
    try {
      await apiPut(`/strategies/orchestration/${code}/today?enabled=${enabled}`, {})
      await load()
      setMsg(`${code} 오늘만 ${enabled ? 'ON' : 'OFF'}`)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  if (!snap) {
    return (
      <section className="card">
        <h2>전략 오케스트레이션</h2>
        <p style={{ color: '#667085' }}>{loading ? '불러오는 중…' : msg || '데이터 없음'}</p>
        <button className="button" type="button" onClick={load} disabled={loading}>
          새로고침
        </button>
      </section>
    )
  }

  return (
    <section className="card" style={{ marginTop: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <h2>전략 오케스트레이션</h2>
          <p style={{ marginTop: 6, color: '#667085', fontSize: 14 }}>
            최종 실행 = 기본 ON/OFF × 오늘 임시 × 시장({snap.marketRegimeLabel}) × 시간(
            {snap.currentTimeSlot?.label ?? '장외'})
          </p>
        </div>
        <button className="button" type="button" onClick={load} disabled={loading}>
          새로고침
        </button>
      </div>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12 }}>
        <span style={{ padding: '4px 10px', borderRadius: 6, background: '#F2F4F7', fontSize: 13 }}>
          분위기 {snap.marketMoodScore}
        </span>
        <span style={{ padding: '4px 10px', borderRadius: 6, background: '#F2F4F7', fontSize: 13 }}>
          매수허용 {snap.activeBuyStrategies.length}개
        </span>
        {snap.activeBuyStrategies.map((c) => (
          <span
            key={c}
            style={{ padding: '4px 10px', borderRadius: 6, background: '#ECFDF3', color: '#027A48', fontSize: 13 }}
          >
            {c}
          </span>
        ))}
      </div>

      <div style={{ overflowX: 'auto', marginTop: 12 }}>
        <table className="table">
          <thead>
            <tr>
              <th>전략</th>
              <th>기본</th>
              <th>오늘</th>
              <th>가중치</th>
              <th>매수</th>
              <th>요약</th>
            </tr>
          </thead>
          <tbody>
            {snap.strategies.map((s) => (
              <tr key={s.code}>
                <td>
                  <strong>{s.label}</strong>
                  <br />
                  <span style={{ fontSize: 12, color: '#667085' }}>{s.code}</span>
                </td>
                <td>
                  <button
                    type="button"
                    className="button"
                    style={{ padding: '4px 10px', fontSize: 12 }}
                    disabled={loading}
                    onClick={() => toggleBase(s.code, !s.baseEnabled)}
                  >
                    {s.baseEnabled ? 'ON' : 'OFF'}
                  </button>
                </td>
                <td>
                  <button
                    type="button"
                    className="button"
                    style={{ padding: '4px 10px', fontSize: 12 }}
                    disabled={loading}
                    onClick={() => toggleToday(s.code, !s.todayEnabled)}
                  >
                    {s.todayEnabled ? 'ON' : 'OFF'}
                  </button>
                  {s.todayOverride ? <span style={{ fontSize: 11, color: '#667085' }}> 임시</span> : null}
                </td>
                <td>{s.combinedWeight}</td>
                <td style={{ color: s.allowBuy ? '#027A48' : '#B42318', fontWeight: 600 }}>
                  {s.allowBuy ? '허용' : '차단'}
                </td>
                <td style={{ fontSize: 12, color: '#475467', maxWidth: 280 }}>{s.reasonSummary}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {msg ? <p style={{ marginTop: 10, color: '#475467' }}>{msg}</p> : null}
    </section>
  )
}
