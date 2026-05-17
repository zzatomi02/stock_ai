'use client'

import { useCallback, useEffect, useState } from 'react'
import { OnOffBadge } from '@/components/strategies/OnOffBadge'
import { apiGet, apiPut } from '@/lib/api'
import { MARKET_CONDITION_LABELS, marketConditionLabel, strategyLabel } from '@/lib/strategy-labels'

type Row = {
  id: number
  marketCondition: string
  strategyType: string
  isEnabled: boolean
  weightMultiplier: number | string
  minScoreOverride?: number | string | null
  description?: string
}

export function MarketStrategySettingsTable() {
  const [rows, setRows] = useState<Row[]>([])
  const [filterMarket, setFilterMarket] = useState('')
  const [filterStrategy, setFilterStrategy] = useState('')
  const [loading, setLoading] = useState(false)
  const [msg, setMsg] = useState('')
  const [edit, setEdit] = useState<Row | null>(null)
  const [form, setForm] = useState({
    isEnabled: true,
    weightMultiplier: '1',
    minScoreOverride: '',
    description: '',
  })

  const load = useCallback(async () => {
    setLoading(true)
    setMsg('')
    try {
      const q = new URLSearchParams()
      if (filterMarket) q.set('marketCondition', filterMarket)
      if (filterStrategy) q.set('strategyType', filterStrategy)
      const path = `/market-condition-strategies${q.toString() ? `?${q}` : ''}`
      const data = await apiGet<Row[]>(path)
      setRows(data)
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [filterMarket, filterStrategy])

  useEffect(() => {
    load()
  }, [load])

  function openEdit(row: Row) {
    setEdit(row)
    setForm({
      isEnabled: row.isEnabled,
      weightMultiplier: String(row.weightMultiplier ?? 1),
      minScoreOverride:
        row.minScoreOverride != null && row.minScoreOverride !== '' ? String(row.minScoreOverride) : '',
      description: row.description ?? '',
    })
  }

  async function save() {
    if (!edit) return
    setLoading(true)
    try {
      await apiPut(`/market-condition-strategies/${edit.id}`, {
        isEnabled: form.isEnabled,
        weightMultiplier: Number(form.weightMultiplier),
        minScoreOverride: form.minScoreOverride ? Number(form.minScoreOverride) : null,
        description: form.description || null,
      })
      setEdit(null)
      await load()
      setMsg('저장되었습니다.')
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  function fmtNum(v: number | string | null | undefined) {
    if (v == null || v === '') return '—'
    const n = typeof v === 'number' ? v : parseFloat(String(v))
    return Number.isFinite(n) ? n.toFixed(2) : String(v)
  }

  return (
    <>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12, alignItems: 'center' }}>
        <select
          value={filterMarket}
          onChange={(e) => setFilterMarket(e.target.value)}
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD' }}
        >
          <option value="">전체 시장상태</option>
          {Object.entries(MARKET_CONDITION_LABELS).map(([k, v]) => (
            <option key={k} value={k}>
              {v}
            </option>
          ))}
        </select>
        <input
          value={filterStrategy}
          onChange={(e) => setFilterStrategy(e.target.value)}
          placeholder="전략 코드 필터"
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD', width: 160 }}
        />
        <button className="button" type="button" disabled={loading} onClick={load}>
          조회
        </button>
      </div>
      {msg ? <p style={{ marginTop: 8, color: '#475467' }}>{msg}</p> : null}
      <div style={{ marginTop: 12, overflowX: 'auto' }}>
        <table className="table">
          <thead>
            <tr>
              <th>시장상태</th>
              <th>전략</th>
              <th>사용</th>
              <th>가중치</th>
              <th>최소점수</th>
              <th>설명</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{marketConditionLabel(r.marketCondition)}</td>
                <td>{strategyLabel(r.strategyType)}</td>
                <td>
                  <OnOffBadge on={r.isEnabled} />
                </td>
                <td>{fmtNum(r.weightMultiplier)}</td>
                <td>{fmtNum(r.minScoreOverride)}</td>
                <td style={{ maxWidth: 240, color: '#475467' }}>{r.description ?? '—'}</td>
                <td>
                  <button className="button" type="button" disabled={loading} onClick={() => openEdit(r)}>
                    수정
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {edit ? (
        <div
          role="dialog"
          style={{
            position: 'fixed',
            inset: 0,
            background: 'rgba(15,23,40,0.55)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
            padding: 16,
          }}
          onClick={() => setEdit(null)}
        >
          <div className="card" style={{ width: '100%', maxWidth: 440 }} onClick={(e) => e.stopPropagation()}>
            <h3 style={{ marginTop: 0 }}>
              {marketConditionLabel(edit.marketCondition)} · {strategyLabel(edit.strategyType)}
            </h3>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 12 }}>
              <input
                type="checkbox"
                checked={form.isEnabled}
                onChange={(e) => setForm((f) => ({ ...f, isEnabled: e.target.checked }))}
              />
              사용
            </label>
            <label style={{ display: 'block', marginTop: 12, fontSize: 14 }}>
              가중치
              <input
                type="number"
                step="0.01"
                min={0}
                max={2}
                value={form.weightMultiplier}
                onChange={(e) => setForm((f) => ({ ...f, weightMultiplier: e.target.value }))}
                style={{ display: 'block', width: '100%', marginTop: 4, padding: 8, borderRadius: 8, border: '1px solid #D0D5DD' }}
              />
            </label>
            <label style={{ display: 'block', marginTop: 12, fontSize: 14 }}>
              최소점수 override
              <input
                type="number"
                value={form.minScoreOverride}
                onChange={(e) => setForm((f) => ({ ...f, minScoreOverride: e.target.value }))}
                placeholder="비우면 기본값"
                style={{ display: 'block', width: '100%', marginTop: 4, padding: 8, borderRadius: 8, border: '1px solid #D0D5DD' }}
              />
            </label>
            <label style={{ display: 'block', marginTop: 12, fontSize: 14 }}>
              설명
              <textarea
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
                rows={2}
                style={{ display: 'block', width: '100%', marginTop: 4, padding: 8, borderRadius: 8, border: '1px solid #D0D5DD' }}
              />
            </label>
            <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
              <button className="button" type="button" onClick={() => setEdit(null)}>
                취소
              </button>
              <button className="button" type="button" disabled={loading} onClick={save}>
                저장
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
