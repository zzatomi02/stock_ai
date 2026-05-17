import { AppShell } from '@/components/layout/AppShell'
import { StrategyAnalysisPanel } from '@/components/strategies/StrategyAnalysisPanel'
import { StrategyNavLinks } from '@/components/strategies/StrategyNavLinks'
import { StrategyOrchestrationPanel } from '@/components/strategies/StrategyOrchestrationPanel'
import { apiGet } from '@/lib/api'

export default async function StrategiesPage() {
  const rows = await apiGet<any[]>('/strategies').catch(() => [])
  return (
    <AppShell>
      <section className="card">
        <h1>전략 관리</h1>
        <p style={{ marginTop: 8, color: '#667085' }}>
          시장·시간대·오늘 임시 설정을 조합해 최종 매매 전략이 결정됩니다.
        </p>
        <StrategyNavLinks />
        {rows.length > 0 ? (
          <table className="table" style={{ marginTop: 12 }}>
            <thead>
              <tr>
                <th>코드</th>
                <th>이름</th>
                <th>활성</th>
                <th>자동매매</th>
                <th>최대매수</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id ?? r.code}>
                  <td>{r.code ?? '-'}</td>
                  <td>{r.name}</td>
                  <td>{String(r.enabled)}</td>
                  <td>{String(r.autoTradeEnabled)}</td>
                  <td>{r.maxBuyAmount?.toLocaleString?.() ?? r.maxBuyAmount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          <p style={{ marginTop: 12, color: '#667085' }}>백엔드 기동 후 전략 시드가 자동 등록됩니다.</p>
        )}
      </section>
      <StrategyOrchestrationPanel />
      <StrategyAnalysisPanel />
    </AppShell>
  )
}
