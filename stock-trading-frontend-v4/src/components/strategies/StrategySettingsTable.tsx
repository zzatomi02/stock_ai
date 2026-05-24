'use client'

import { useCallback, useEffect, useState } from 'react'
import { OnOffBadge } from '@/components/strategies/OnOffBadge'
import { apiGet, apiPost, apiPut } from '@/lib/api'
import { notifyError, notifySuccess } from '@/lib/toast'
import { strategyLabel, todayKst } from '@/lib/strategy-labels'

type MasterRow = {
  strategyType: string
  strategyName: string
  description?: string
  isEnabled: boolean
  sortOrder: number
}

type RuntimeRow = {
  strategyType: string
  isEnabled: boolean
  reason?: string
}

export function StrategySettingsTable() {
  const [rows, setRows] = useState<MasterRow[]>([])
  const [runtime, setRuntime] = useState<Record<string, RuntimeRow>>({})
  const [loading, setLoading] = useState(false)
  const [editType, setEditType] = useState<string | null>(null)
  const [editBase, setEditBase] = useState(true)
  const [editToday, setEditToday] = useState(true)
  const [editReason, setEditReason] = useState('')

  const tradeDate = todayKst()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [masters, runtimes] = await Promise.all([
        apiGet<MasterRow[]>('/strategy-settings'),
        apiGet<RuntimeRow[]>(`/strategy-runtime-settings?tradeDate=${tradeDate}`),
      ])
      setRows(masters)
      const map: Record<string, RuntimeRow> = {}
      for (const r of runtimes) {
        map[r.strategyType] = r
      }
      setRuntime(map)
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }, [tradeDate])

  useEffect(() => {
    load()
  }, [load])

  function todayEnabled(type: string, base: boolean) {
    const rt = runtime[type]
    if (rt) return rt.isEnabled
    return base
  }

  function openEdit(row: MasterRow) {
    setEditType(row.strategyType)
    setEditBase(row.isEnabled)
    setEditToday(todayEnabled(row.strategyType, row.isEnabled))
    setEditReason(runtime[row.strategyType]?.reason ?? '')
  }

  async function saveEdit() {
    if (!editType) return
    setLoading(true)
    try {
      await apiPut(`/strategy-settings/${editType}/enabled`, { isEnabled: editBase })
      await apiPost('/strategy-runtime-settings', {
        tradeDate,
        strategyType: editType,
        isEnabled: editToday,
        reason: editReason || (editToday ? '오늘 활성' : '오늘 임시 중지'),
        updatedBy: 'ui',
      })
      setEditType(null)
      await load()
      notifySuccess('저장되었습니다.')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function quickTodayOff(strategyType: string) {
    setLoading(true)
    try {
      await apiPost('/strategy-runtime-settings', {
        tradeDate,
        strategyType,
        isEnabled: false,
        reason: '오늘만 OFF',
        updatedBy: 'ui',
      })
      await load()
      notifySuccess(`${strategyLabel(strategyType)} 오늘 OFF`)
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <p style={{ marginTop: 8, color: '#667085', fontSize: 14 }}>
        거래일 {tradeDate} · 오늘 임시 설정이 없으면 기본 ON/OFF를 따릅니다.
      </p>
      <div style={{ marginTop: 12, overflowX: 'auto' }}>
        <table className="table">
          <thead>
            <tr>
              <th>순서</th>
              <th>전략</th>
              <th>기본 사용</th>
              <th>오늘 사용</th>
              <th>설명</th>
              <th>관리</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              const today = todayEnabled(r.strategyType, r.isEnabled)
              return (
                <tr key={r.strategyType}>
                  <td>{r.sortOrder}</td>
                  <td>
                    <strong>{strategyLabel(r.strategyType, r.strategyName)}</strong>
                    <div style={{ fontSize: 12, color: '#667085' }}>{r.strategyType}</div>
                  </td>
                  <td>
                    <OnOffBadge on={r.isEnabled} />
                  </td>
                  <td>
                    <OnOffBadge on={today} />
                  </td>
                  <td style={{ maxWidth: 280, color: '#475467' }}>{r.description ?? '—'}</td>
                  <td>
                    <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                      <button className="button" type="button" disabled={loading} onClick={() => openEdit(r)}>
                        수정
                      </button>
                      <button
                        className="button"
                        type="button"
                        disabled={loading || !today}
                        onClick={() => quickTodayOff(r.strategyType)}
                      >
                        오늘만 OFF
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {editType ? (
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
          onClick={() => setEditType(null)}
        >
          <div
            className="card"
            style={{ width: '100%', maxWidth: 420 }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ marginTop: 0 }}>{strategyLabel(editType)} 설정</h3>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 12 }}>
              <input type="checkbox" checked={editBase} onChange={(e) => setEditBase(e.target.checked)} />
              기본 사용 (strategy_master)
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
              <input type="checkbox" checked={editToday} onChange={(e) => setEditToday(e.target.checked)} />
              오늘 사용 ({tradeDate})
            </label>
            <textarea
              value={editReason}
              onChange={(e) => setEditReason(e.target.value)}
              placeholder="오늘 임시 사유"
              rows={3}
              style={{
                width: '100%',
                marginTop: 12,
                padding: 10,
                borderRadius: 8,
                border: '1px solid #D0D5DD',
              }}
            />
            <div style={{ display: 'flex', gap: 8, marginTop: 16, justifyContent: 'flex-end' }}>
              <button className="button" type="button" onClick={() => setEditType(null)}>
                취소
              </button>
              <button className="button" type="button" disabled={loading} onClick={saveEdit}>
                저장
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </>
  )
}
