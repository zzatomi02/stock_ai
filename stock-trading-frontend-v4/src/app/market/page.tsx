import Link from 'next/link'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet } from '@/lib/api'

const TABS = [
  { key: 'volume', label: '거래량' },
  { key: 'rise', label: '상승률' },
  { key: 'marketcap', label: '시가총액' },
  { key: 'ai', label: 'AI 추천' },
] as const

export default async function MarketPage({
  searchParams,
}: {
  searchParams?: Promise<{ type?: string }>
}) {
  const sp = (await searchParams) || {}
  const selected = TABS.some((t) => t.key === sp.type) ? (sp.type as typeof TABS[number]['key']) : 'volume'
  const snapshot = await apiGet<{
    type: string
    rows: any[]
    lastUpdatedAt: string
    stale: boolean
    refreshIntervalSec: number
  }>(`/market/top100?type=${selected}`)
  const top100 = snapshot?.rows ?? []
  return (
    <AppShell>
      <section className="card">
        <h1>시장 Top100</h1>
        <p style={{ marginTop: 6, color: '#667085', fontSize: 13 }}>
          최근 업데이트: {snapshot?.lastUpdatedAt ? new Date(snapshot.lastUpdatedAt).toLocaleTimeString('ko-KR', { hour12: false }) : '-'}
          {' · '}갱신주기 {snapshot?.refreshIntervalSec ?? 30}초
          {snapshot?.stale ? ' · 이전 스냅샷 표시중' : ''}
        </p>
        <div className="tabs" style={{ marginBottom: 12 }}>
          {TABS.map((tab) => (
            <Link
              key={tab.key}
              href={`/market?type=${tab.key}`}
              className={`tab ${selected === tab.key ? 'active' : ''}`}
            >
              {tab.label}
            </Link>
          ))}
        </div>
        <table className="table">
          <thead><tr><th>순위</th><th>종목명</th><th>현재가</th><th>등락률</th><th>거래량</th><th>뉴스</th><th>AI</th><th>종합</th></tr></thead>
          <tbody>
            {top100.map((row) => (
              <tr key={row.stockCode}><td>{row.rank}</td><td><Link href={`/market/${row.stockCode}`}>{row.stockName}</Link></td><td>{row.price.toLocaleString()}</td><td>{row.changeRate}%</td><td>{row.volume.toLocaleString()}</td><td>{row.newsScore}</td><td>{row.aiScore}</td><td>{row.finalScore}</td></tr>
            ))}
            {top100.length === 0 ? (
              <tr>
                <td colSpan={8} style={{ textAlign: 'center', color: '#667085' }}>
                  데이터가 없습니다.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
