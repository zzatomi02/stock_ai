'use client'

import { useCallback, useEffect, useState } from 'react'
import { apiGet, apiPost } from '@/lib/api'
import { notifyError, notifyInfo, notifySuccess } from '@/lib/toast'

type StockRecommendationItem = {
  id: number
  stockCode: string
  stockName?: string
  status: string
  bestAdjustedScore?: number | string
  signalGrade?: string
  strategyCodes?: string
  reasonSummary?: string
  notificationSent?: boolean
}

type RecommendationListResponse = {
  items: StockRecommendationItem[]
  phase?: string
}

type SyncResponse = {
  created?: number
  notified?: number
  message?: string
}

const STATUS_TABS = [
  'ALL',
  'RECOMMENDED',
  'PENDING_APPROVAL',
  'ORDERED',
  'REJECTED',
  'ORDER_FAILED',
] as const

function statusColor(status: string): string {
  if (status === 'PENDING_APPROVAL') return '#B54708'
  if (status === 'ORDERED') return '#175CD3'
  if (status === 'RECOMMENDED') return '#067647'
  if (status === 'REJECTED' || status === 'ORDER_FAILED') return '#B42318'
  return '#475467'
}

export function StockRecommendationsPanel() {
  const [statusFilter, setStatusFilter] = useState<(typeof STATUS_TABS)[number]>('ALL')
  const [recommendations, setRecommendations] = useState<StockRecommendationItem[]>([])
  const [executionPhase, setExecutionPhase] = useState('')
  const [loading, setLoading] = useState(false)

  const loadRecommendations = useCallback(async () => {
    setLoading(true)
    try {
      const queryString = statusFilter === 'ALL' ? '' : `?status=${statusFilter}`
      const listResponse = await apiGet<RecommendationListResponse>(
        `/recommendations/stocks${queryString}`,
      )
      setRecommendations(listResponse.items ?? [])
      setExecutionPhase(listResponse.phase ?? '')
      notifyInfo(`오늘 ${listResponse.items?.length ?? 0}건 · 단계 ${listResponse.phase ?? ''}`)
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }, [statusFilter])

  useEffect(() => {
    void loadRecommendations()
  }, [loadRecommendations])

  async function syncFromSignals() {
    setLoading(true)
    try {
      const syncResponse = await apiPost<SyncResponse>('/recommendations/sync', {})
      notifySuccess(
        `동기화 완료 — 신규 ${syncResponse.created ?? 0}건 · 알림 ${syncResponse.notified ?? 0}건`,
      )
      await loadRecommendations()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function approveRecommendation(recommendationId: number) {
    setLoading(true)
    try {
      await apiPost(`/recommendations/stocks/${recommendationId}/approve`, {}, undefined, {
        tradingMode: executionPhase.startsWith('REAL') ? 'real' : 'paper',
      })
      notifySuccess(`#${recommendationId} 승인·주문 요청 완료`)
      await loadRecommendations()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function rejectRecommendation(recommendationId: number) {
    setLoading(true)
    try {
      await apiPost(
        `/recommendations/stocks/${recommendationId}/reject?reason=사용자거절`,
        {},
      )
      notifySuccess(`#${recommendationId} 거절 처리`)
      await loadRecommendations()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="card">
      <h1>종목 추천</h1>
      <p style={{ marginTop: 8, color: '#667085', fontSize: 14 }}>
        전략 시그널(CANDIDATE BUY)을 <strong>종목 단위</strong>로 묶은 추천 목록입니다. OBSERVE는 목록만, 알림·승인
        모드는 카카오·메일·문자(설정) 알림 후 승인 시 주문합니다.
      </p>

      <div style={{ marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        {STATUS_TABS.map((tab) => (
          <button
            key={tab}
            type="button"
            className={statusFilter === tab ? 'tab tab--active' : 'tab'}
            disabled={loading}
            onClick={() => setStatusFilter(tab)}
          >
            {tab === 'ALL' ? '전체' : tab}
          </button>
        ))}
      </div>

      <div style={{ marginTop: 12, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button className="button" type="button" disabled={loading} onClick={syncFromSignals}>
          시그널 → 추천 동기화
        </button>
        <button className="button" type="button" disabled={loading} onClick={loadRecommendations}>
          새로고침
        </button>
      </div>

      {recommendations.length > 0 ? (
        <div style={{ marginTop: 16, overflowX: 'auto' }}>
          <table className="table" style={{ fontSize: 14 }}>
            <thead>
              <tr>
                <th>종목</th>
                <th>점수</th>
                <th>등급</th>
                <th>전략</th>
                <th>상태</th>
                <th>알림</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {recommendations.map((recommendation) => (
                <tr key={recommendation.id}>
                  <td>
                    <strong>{recommendation.stockName ?? recommendation.stockCode}</strong>
                    <div style={{ color: '#667085', fontSize: 12 }}>{recommendation.stockCode}</div>
                  </td>
                  <td>{recommendation.bestAdjustedScore ?? '—'}</td>
                  <td>{recommendation.signalGrade ?? '—'}</td>
                  <td style={{ maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {recommendation.strategyCodes ?? '—'}
                  </td>
                  <td style={{ color: statusColor(recommendation.status), fontWeight: 600 }}>
                    {recommendation.status}
                  </td>
                  <td>{recommendation.notificationSent ? '발송' : '—'}</td>
                  <td>
                    {recommendation.status === 'PENDING_APPROVAL' ? (
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button
                          className="button"
                          type="button"
                          disabled={loading}
                          onClick={() => approveRecommendation(recommendation.id)}
                        >
                          승인·주문
                        </button>
                        <button
                          className="button button--danger"
                          type="button"
                          disabled={loading}
                          onClick={() => rejectRecommendation(recommendation.id)}
                        >
                          거절
                        </button>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : !loading ? (
        <p style={{ marginTop: 16, color: '#667085' }}>
          추천 종목이 없습니다. 전략 시그널 생성 후 동기화하세요.
        </p>
      ) : null}
    </section>
  )
}
