'use client'

import { useEffect, useState } from 'react'
import { apiGet } from '@/lib/api'

type PlatformStatus = {
  executionPhase?: string
  observeOnly?: boolean
}

const LABELS: Record<string, string> = {
  OBSERVE: '관찰 모드 — 매매 후보만 생성, 주문 없음',
  VIRTUAL: '가상매매 모드 — DB 가상 체결만',
  KIS_PAPER: 'KIS 모의투자 — 모의 API 주문 허용',
  REAL_MANUAL: '실전 수동 승인 — 승인 후 주문(준비 중)',
  REAL_AUTO: '제한적 실전 자동매매',
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
  const label = LABELS[phase] || phase

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
