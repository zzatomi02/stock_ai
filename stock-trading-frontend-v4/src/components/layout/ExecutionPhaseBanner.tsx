'use client'

import { useEffect, useState } from 'react'
import { apiGet } from '@/lib/api'

type PlatformStatus = {
  executionPhase?: string
  executionPhaseLabel?: string
  observeOnly?: boolean
}

const LABELS: Record<string, string> = {
  OBSERVE: '관찰 — 종목 추천 목록만',
  VIRTUAL: '관찰(레거시)',
  KIS_PAPER: '모의 — 추천 알림 + 승인',
  PAPER_ALERT: '모의 — 추천 알림 + 승인 후 주문',
  PAPER_AUTO: '모의 — 자동매매',
  REAL_MANUAL: '실전 — 추천 알림 + 승인',
  REAL_ALERT: '실전 — 추천 알림 + 승인 후 주문',
  REAL_AUTO: '실전 — 자동매매',
}

/** 상단 실행 단계 배너. 페이지당 1회만 조회 (서버 컴포넌트 매 렌더 시 반복 호출 방지). */
export function ExecutionPhaseBanner() {
  const [status, setStatus] = useState<PlatformStatus | null>(null)

  useEffect(() => {
    let cancelled = false
    apiGet<PlatformStatus>('/signals/platform')
      .then((p) => {
        if (!cancelled) setStatus(p)
      })
      .catch(() => {
        if (!cancelled) setStatus({ executionPhase: 'OBSERVE', observeOnly: true })
      })
    return () => {
      cancelled = true
    }
  }, [])

  const phase = status?.executionPhase || 'OBSERVE'
  const observeOnly = status?.observeOnly ?? true
  const label = status?.executionPhaseLabel || LABELS[phase] || phase

  return (
    <div
      className={`execution-phase-banner${observeOnly ? ' execution-phase-banner--observe' : ''}`}
      role="status"
    >
      <strong>{phase}</strong>
      <span>{label}</span>
    </div>
  )
}
