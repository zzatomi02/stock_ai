import { AppShell } from '@/components/layout/AppShell'
import { EnabledStrategiesNowPanel } from '@/components/strategies/EnabledStrategiesNowPanel'
import { StrategyNavLinks } from '@/components/strategies/StrategyNavLinks'

export default function EnabledStrategiesNowPage() {
  return (
    <AppShell>
      <section className="card">
        <h1>현재 활성 전략</h1>
        <p style={{ marginTop: 8, color: '#667085' }}>
          현재 시장·시간대 기준으로 실행 가능한 전략과 제외 사유를 확인합니다.
        </p>
        <StrategyNavLinks />
        <EnabledStrategiesNowPanel />
      </section>
    </AppShell>
  )
}
