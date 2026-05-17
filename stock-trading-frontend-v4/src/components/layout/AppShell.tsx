import { ReactNode } from 'react'
import { Sidebar } from './Sidebar'
import { ExecutionPhaseBanner } from './ExecutionPhaseBanner'
import { TradingModeBar } from './TradingModeBar'

export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className="app-shell">
      <Sidebar />
      <div className="app-main">
        <TradingModeBar />
        <ExecutionPhaseBanner />
        {children}
      </div>
    </div>
  )
}
