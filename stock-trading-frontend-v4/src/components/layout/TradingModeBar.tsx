'use client'

import { useRouter } from 'next/navigation'
import { FlaskConical, Landmark } from 'lucide-react'
import { useTradingModeStore, type TradingMode } from '@/stores/trading-mode-store'
import { UserMenu } from './UserMenu'

export function TradingModeBar() {
  const mode = useTradingModeStore((s) => s.mode)
  const setMode = useTradingModeStore((s) => s.setMode)
  const router = useRouter()

  function select(next: TradingMode) {
    if (next === mode) return
    setMode(next)
    router.refresh()
  }

  return (
    <div className="trading-mode-bar" role="toolbar" aria-label="매매 구분">
      <span className="trading-mode-bar-label">매매 구분</span>
      <div className="trading-mode-toggle">
        <button
          type="button"
          className={`trading-mode-btn${mode === 'paper' ? ' trading-mode-btn--active' : ''}`}
          onClick={() => select('paper')}
          aria-pressed={mode === 'paper'}
        >
          <FlaskConical size={16} aria-hidden />
          모의투자
        </button>
        <button
          type="button"
          className={`trading-mode-btn${mode === 'real' ? ' trading-mode-btn--active trading-mode-btn--danger' : ''}`}
          onClick={() => select('real')}
          aria-pressed={mode === 'real'}
        >
          <Landmark size={16} aria-hidden />
          실전투자
        </button>
      </div>
      <div className="trading-mode-spacer" />
      <UserMenu />
      {mode === 'real' && <span className="trading-mode-hint">실전 계좌·API 키가 설정된 환경에서만 사용하세요.</span>}
    </div>
  )
}
