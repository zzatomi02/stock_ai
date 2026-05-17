import { AppShell } from '@/components/layout/AppShell'
import { MarketStrategySettingsTable } from '@/components/strategies/MarketStrategySettingsTable'
import { StrategyNavLinks } from '@/components/strategies/StrategyNavLinks'

export default function MarketStrategySettingsPage() {
  return (
    <AppShell>
      <section className="card">
        <h1>시장별 전략 설정</h1>
        <p style={{ marginTop: 8, color: '#667085' }}>
          시장 상태별 전략 ON/OFF, 가중치, 최소점수 override를 수정합니다.
        </p>
        <StrategyNavLinks />
        <MarketStrategySettingsTable />
      </section>
    </AppShell>
  )
}
