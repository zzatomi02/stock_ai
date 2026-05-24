'use client'

import { useCallback, useEffect, useState } from 'react'
import { OnOffBadge } from '@/components/strategies/OnOffBadge'
import { apiGet, apiPut } from '@/lib/api'
import { notifyError, notifySuccess } from '@/lib/toast'
import { strategyLabel, timeWindowLabel } from '@/lib/strategy-labels'

type Row = {
  id: number
  strategyType: string
  marketTimeWindow: string
  windowName?: string
  startTime: string
  endTime: string
  isEnabled: boolean
  weightMultiplier: number | string
  minScoreOverride?: number | string | null
  maxTradeCount?: number | null
  description?: string
}

export function StrategyTimeWindowsTable() {
  const [rows, setRows] = useState<Row[]>([])
  const [filterStrategy, setFilterStrategy] = useState('')
  const [filterWindow, setFilterWindow] = useState('')
  const [loading, setLoading] = useState(false)
  const [edit, setEdit] = useState<Row | null>(null)
  const [form, setForm] = useState({
    isEnabled: true,
    weightMultiplier: '1',
    minScoreOverride: '',
    maxTradeCount: '',
    description: '',
  })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const q = new URLSearchParams()
      if (filterStrategy) q.set('strategyType', filterStrategy)
      if (filterWindow) q.set('marketTimeWindow', filterWindow)
      const path = `/strategy-time-windows${q.toString() ? `?${q}` : ''}`
      setRows(await apiGet<Row[]>(path))
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }, [filterStrategy, filterWindow])

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
      maxTradeCount: row.maxTradeCount != null ? String(row.maxTradeCount) : '',
      description: row.description ?? '',
    })
  }

  async function save() {
    if (!edit) return
    setLoading(true)
    try {
      await apiPut(`/strategy-time-windows/${edit.id}`, {
        isEnabled: form.isEnabled,
        weightMultiplier: Number(form.weightMultiplier),
        minScoreOverride: form.minScoreOverride ? Number(form.minScoreOverride) : null,
        maxTradeCount: form.maxTradeCount ? Number(form.maxTradeCount) : null,
        description: form.description || null,
      })
      setEdit(null)
      await load()
      notifySuccess('저장되었습니다.')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  function fmtNum(v: number | string | null | undefined) {
    if (v == null || v === '') return '—'
    const n = typeof v === 'number' ? v : parseFloat(String(v))
    return Number.isFinite(n) ? n.toFixed(2) : String(v)
  }

  function fmtTime(t: string) {
    if (!t) return '—'
    return t.length >= 5 ? t.slice(0, 5) : t
  }

  return (
    <>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12 }}>
        <input
          value={filterStrategy}
          onChange={(e) => setFilterStrategy(e.target.value)}
          placeholder="전략 코드"
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD', width: 140 }}
        />
        <input
          value={filterWindow}
          onChange={(e) => setFilterWindow(e.target.value)}
          placeholder="시간대 코드"
          style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid #D0D5DD', width: 160 }}
        />
        <button className="button" type="button" disabled={loading} onClick={load}>
          조회
        </button>
      </div>
      <div style={{ marginTop: 12, overflowX: 'auto' }}>
        <table className="table">
          <thead>
            <tr>
              <th>전략</th>
              <th>시간대</th>
              <th>시작</th>
              <th>종료</th>
              <th>사용</th>
              <th>가중치</th>
              <th>최소점수</th>
              <th>최대매매</th>
              <th>설명</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>{strategyLabel(r.strategyType)}</td>
                <td>
                  <div>{timeWindowLabel(r.marketTimeWindow, r.windowName)}</div>
                  <div style={{ fontSize: 11, color: '#667085' }}>{r.marketTimeWindow}</div>
                </td>
                <td>{fmtTime(r.startTime)}</td>
                <td>{fmtTime(r.endTime)}</td>
                <td>
                  <OnOffBadge on={r.isEnabled} />
                </td>
                <td>{fmtNum(r.weightMultiplier)}</td>
                <td>{fmtNum(r.minScoreOverride)}</td>
                <td>{r.maxTradeCount ?? '—'}</td>
                <td style={{ maxWidth: 200, color: '#475467' }}>{r.description ?? '—'}</td>
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
              {strategyLabel(edit.strategyType)} · {timeWindowLabel(edit.marketTimeWindow)}
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
                style={{ display: 'block', width: '100%', marginTop: 4, padding: 8, borderRadius: 8, border: '1px solid #D0D5DD' }}
              />
            </label>
            <label style={{ display: 'block', marginTop: 12, fontSize: 14 }}>
              최대 매매 횟수
              <input
                type="number"
                min={0}
                value={form.maxTradeCount}
                onChange={(e) => setForm((f) => ({ ...f, maxTradeCount: e.target.value }))}
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

