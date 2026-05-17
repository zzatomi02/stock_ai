import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type TradingMode = 'paper' | 'real'

const COOKIE = 'trading-mode'
const ONE_YEAR = 60 * 60 * 24 * 365

export function setTradingModeCookie(mode: TradingMode) {
  if (typeof document === 'undefined') return
  document.cookie = `${COOKIE}=${mode}; path=/; max-age=${ONE_YEAR}; SameSite=Lax`
}

export const useTradingModeStore = create(
  persist<{ mode: TradingMode; setMode: (m: TradingMode) => void }>(
    (set) => ({
      mode: 'paper',
      setMode: (m) => {
        setTradingModeCookie(m)
        set({ mode: m })
      },
    }),
    {
      name: 'stock-trading-mode',
      onRehydrateStorage: () => (state) => {
        if (state?.mode) setTradingModeCookie(state.mode as TradingMode)
      },
    },
  ),
)
