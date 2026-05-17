import { AppShell } from '@/components/layout/AppShell'
import { StrategyNavLinks } from '@/components/strategies/StrategyNavLinks'
import { StrategyTimeWindowsTable } from '@/components/strategies/StrategyTimeWindowsTable'

export default function StrategyTimeWindowsPage() {
  return (
    <AppShell>
      <section className="card">
        <h1>시간대별 전략 설정</h1>
        <p style={{ marginTop: 8, color: '#667085' }}>
          장중 시간대별 전략 ON/OFF, 가중치, 최소점수, 최대 매매 횟수를 설정합니다.
        </p>
        <StrategyNavLinks />
        <StrategyTimeWindowsTable />
      </section>
    </AppShell>
  )
}
