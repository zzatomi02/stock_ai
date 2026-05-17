import Link from 'next/link'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet } from '@/lib/api'

type Signal = {
  id: number
  stockCode: string
  stockName?: string
  side: string
  finalScore?: number
  llmSummary?: string
}

export default async function DashboardPage() {
  let dashboard: any = {
    evaluationAmount: 0,
    todayProfitAmount: 0,
    todayProfitRate: 0,
    autoTrading: false,
    emergencyStop: false,
    executionPhase: 'OBSERVE',
    observeOnly: true,
    buyCandidateCount: 0,
    sellWarningCount: 0,
    buyCandidates: [] as Signal[],
    sellWarnings: [] as Signal[],
    dataStatus: 'UNAVAILABLE',
    dataReason: '',
  }
  let dashboardError = ''
  try {
    dashboard = await apiGet<any>('/dashboard')
  } catch (e) {
    dashboardError = e instanceof Error ? e.message : String(e)
  }
  let top100: any[] = []
  let top100Error = ''
  try {
    const snapshot = await apiGet<{ rows?: any[] }>('/market/top100?type=volume')
    top100 = Array.isArray(snapshot?.rows) ? snapshot.rows : []
  } catch (e) {
    top100Error = e instanceof Error ? e.message : String(e)
  }

  const buyList: Signal[] = dashboard.buyCandidates || []
  const sellList: Signal[] = dashboard.sellWarnings || []

  return (
    <AppShell>
      <header className="dash-hero">
        <h1>대시보드</h1>
        <p>
          AI 뉴스 기반 투자 시그널 콘솔입니다. 현재 단계에서는 매매 <strong>후보</strong>만 생성되며 실제 주문은
          실행되지 않습니다.
        </p>
      </header>

      <section className="grid-4">
        <div className="card">
          <div>실행 단계</div>
          <h2>{dashboard.executionPhase}</h2>
        </div>
        <div className="card">
          <div>매수 후보</div>
          <h2>{dashboard.buyCandidateCount ?? 0}</h2>
        </div>
        <div className="card">
          <div>매도 경고</div>
          <h2>{dashboard.sellWarningCount ?? 0}</h2>
        </div>
        <div className="card">
          <div>긴급 정지</div>
          <h2>{dashboard.emergencyStop ? 'ON' : 'OFF'}</h2>
        </div>
      </section>

      <section className="grid-2" style={{ marginTop: 12 }}>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2>오늘의 AI 매수 후보</h2>
            <Link href="/signals" className="button">
              전체
            </Link>
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>종목</th>
                <th>점수</th>
                <th>요약</th>
              </tr>
            </thead>
            <tbody>
              {buyList.map((r) => (
                <tr key={r.id}>
                  <td>
                    <Link href={`/market/${r.stockCode}`}>{r.stockName || r.stockCode}</Link>
                  </td>
                  <td>{r.finalScore ?? '-'}</td>
                  <td>{r.llmSummary || '-'}</td>
                </tr>
              ))}
              {!buyList.length ? (
                <tr>
                  <td colSpan={3} style={{ textAlign: 'center', color: '#667085' }}>
                    후보 없음 — 뉴스 수집·GPT 분석 후 생성됩니다.
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h2>오늘의 AI 매도 경고</h2>
            <Link href="/signals?tab=rejected" className="button">
              제외 사유
            </Link>
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>종목</th>
                <th>점수</th>
                <th>요약</th>
              </tr>
            </thead>
            <tbody>
              {sellList.map((r) => (
                <tr key={r.id}>
                  <td>
                    <Link href={`/market/${r.stockCode}`}>{r.stockName || r.stockCode}</Link>
                  </td>
                  <td>{r.finalScore ?? '-'}</td>
                  <td>{r.llmSummary || '-'}</td>
                </tr>
              ))}
              {!sellList.length ? (
                <tr>
                  <td colSpan={3} style={{ textAlign: 'center', color: '#667085' }}>
                    경고 없음
                  </td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>

      <section className="grid-4" style={{ marginTop: 12 }}>
        <div className="card">
          <div>총 평가금액</div>
          <h2>{dashboard.evaluationAmount.toLocaleString()}원</h2>
        </div>
        <div className="card">
          <div>오늘 손익</div>
          <h2>{dashboard.todayProfitAmount.toLocaleString()}원</h2>
        </div>
        <div className="card">
          <div>손익률</div>
          <h2>{dashboard.todayProfitRate}%</h2>
        </div>
        <div className="card">
          <div>자동매매</div>
          <h2>{dashboard.autoTrading ? 'ON' : 'OFF'}</h2>
        </div>
      </section>

      {dashboardError || dashboard.dataStatus === 'UNAVAILABLE' ? (
        <section className="card" style={{ marginTop: 12 }}>
          <p style={{ margin: 0, color: '#b42318' }}>
            KIS 잔고 요약: {dashboardError || dashboard.dataReason || '연동 확인'}
            {dashboard.observeOnly ? ' (관찰 모드에서는 주문 불가)' : ''}
          </p>
        </section>
      ) : null}

      <section className="card" style={{ marginTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2>거래량 Top100</h2>
          <Link href="/market" className="button">
            전체 보기
          </Link>
        </div>
        {top100Error ? (
          <p style={{ margin: '8px 0 12px', color: '#b42318' }}>
            거래량 Top100 조회 실패: KIS 인증 설정을 확인하세요.
          </p>
        ) : null}
        <table className="table">
          <thead>
            <tr>
              <th>순위</th>
              <th>종목</th>
              <th>현재가</th>
              <th>등락률</th>
              <th>뉴스</th>
              <th>AI</th>
              <th>종합</th>
            </tr>
          </thead>
          <tbody>
            {top100.slice(0, 10).map((row) => (
              <tr key={row.stockCode}>
                <td>{row.rank}</td>
                <td>
                  <Link href={`/market/${row.stockCode}`}>{row.stockName}</Link>
                </td>
                <td>{row.price.toLocaleString()}</td>
                <td>{row.changeRate}%</td>
                <td>{row.newsScore}</td>
                <td>{row.aiScore}</td>
                <td>{row.finalScore}</td>
              </tr>
            ))}
            {!top100.length ? (
              <tr>
                <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                  표시할 데이터가 없습니다.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
