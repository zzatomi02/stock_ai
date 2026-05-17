import { AppShell } from '@/components/layout/AppShell'
import { StrategyNavLinks } from '@/components/strategies/StrategyNavLinks'
import { StrategySettingsTable } from '@/components/strategies/StrategySettingsTable'

export default function StrategySettingsPage() {
  return (
    <AppShell>
      <section className="card">
        <h1>전략 설정</h1>
        <p style={{ marginTop: 8, color: '#667085' }}>
          전략별 기본 ON/OFF와 오늘만 임시 설정을 관리합니다.
        </p>
        <StrategyNavLinks />
        <StrategySettingsTable />
      </section>
    </AppShell>
  )
}
