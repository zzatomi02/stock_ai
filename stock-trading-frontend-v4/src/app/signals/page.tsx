import Link from 'next/link'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet } from '@/lib/api'

type Signal = {
  id: number
  stockCode: string
  stockName?: string
  side: string
  status: string
  signalGrade?: string
  finalScore?: number
  keywordScore?: number
  aiScore?: number
  llmActionHint?: string
  llmSummary?: string
  rejectedReason?: string
  createdAt?: string
}

export default async function SignalsPage({
  searchParams,
}: {
  searchParams?: Promise<{ tab?: string }>
}) {
  const sp = (await searchParams) || {}
  const tab = sp.tab || 'candidates'
  let buy: Signal[] = []
  let sell: Signal[] = []
  let rejected: Signal[] = []
  let graded: Signal[] = []
  try {
    if (tab === 'rejected') {
      rejected = await apiGet<Signal[]>('/signals/rejected?limit=100')
    } else if (['S', 'A', 'B', 'C'].includes(tab.toUpperCase())) {
      graded = await apiGet<Signal[]>(`/signals/grades/${tab.toUpperCase()}?limit=50`)
    } else {
      buy = await apiGet<Signal[]>('/signals?side=BUY&status=CANDIDATE&limit=50')
      sell = await apiGet<Signal[]>('/signals?side=SELL&status=CANDIDATE&limit=50')
    }
  } catch {
    /* empty */
  }

  function tableRows(rows: Signal[]) {
    if (!rows.length) {
      return (
        <tr>
          <td colSpan={8} style={{ textAlign: 'center', color: '#667085' }}>
            데이터 없음
          </td>
        </tr>
      )
    }
    return rows.map((r) => (
      <tr key={r.id}>
        <td>{r.side}</td>
        <td>
          <Link href={`/market/${r.stockCode}`}>{r.stockName || r.stockCode}</Link>
        </td>
        <td>{r.signalGrade ?? '-'}</td>
        <td>{r.finalScore ?? '-'}</td>
        <td>{r.keywordScore ?? '-'}</td>
        <td>{r.aiScore ?? '-'}</td>
        <td>{r.llmActionHint ?? '-'}</td>
        <td>{r.llmSummary || r.rejectedReason || '-'}</td>
      </tr>
    ))
  }

  return (
    <AppShell>
      <section className="card">
        <h1>매매 시그널</h1>
        <p style={{ marginTop: 6, color: '#667085', fontSize: 13 }}>
          관찰 모드에서는 후보만 생성되며 실제 주문은 실행되지 않습니다.
        </p>
        <div className="tabs" style={{ marginBottom: 16 }}>
          <Link href="/signals" className={tab === 'candidates' ? 'tab tab--active' : 'tab'}>후보</Link>
          <Link href="/signals?tab=S" className={tab === 'S' ? 'tab tab--active' : 'tab'}>S</Link>
          <Link href="/signals?tab=A" className={tab === 'A' ? 'tab tab--active' : 'tab'}>A</Link>
          <Link href="/signals?tab=B" className={tab === 'B' ? 'tab tab--active' : 'tab'}>B</Link>
          <Link href="/signals?tab=C" className={tab === 'C' ? 'tab tab--active' : 'tab'}>C</Link>
          <Link href="/signals?tab=rejected" className={tab === 'rejected' ? 'tab tab--active' : 'tab'}>제외</Link>
        </div>
        {['S', 'A', 'B', 'C'].includes(tab.toUpperCase()) ? (
          <table className="table">
            <thead>
              <tr>
                <th>구분</th><th>종목</th><th>등급</th><th>점수</th><th>키워드</th><th>AI</th><th>힌트</th><th>요약</th>
              </tr>
            </thead>
            <tbody>{tableRows(graded)}</tbody>
          </table>
        ) : tab === 'rejected' ? (
          <table className="table">
            <thead>
              <tr>
                <th>구분</th>
                <th>종목</th>
                <th>등급</th>
                <th>점수</th>
                <th>키워드</th>
                <th>AI</th>
                <th>힌트</th>
                <th>사유</th>
              </tr>
            </thead>
            <tbody>{tableRows(rejected)}</tbody>
          </table>
        ) : (
          <>
            <h2 style={{ fontSize: 16, marginTop: 8 }}>매수 후보 ({buy.length})</h2>
            <table className="table">
              <thead>
              <tr>
                <th>구분</th>
                <th>종목</th>
                <th>등급</th>
                <th>종합</th>
                <th>키워드</th>
                <th>AI</th>
                <th>힌트</th>
                <th>요약</th>
              </tr>
            </thead>
            <tbody>{tableRows(buy)}</tbody>
            </table>
            <h2 style={{ fontSize: 16, marginTop: 24 }}>매도 경고 ({sell.length})</h2>
            <table className="table">
              <thead>
                <tr>
                  <th>구분</th>
                  <th>종목</th>
                  <th>종합</th>
                  <th>키워드</th>
                  <th>AI</th>
                  <th>힌트</th>
                  <th>요약</th>
                </tr>
              </thead>
              <tbody>{tableRows(sell)}</tbody>
            </table>
          </>
        )}
      </section>
    </AppShell>
  )
}
