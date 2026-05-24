import { AppShell } from '@/components/layout/AppShell'
import Link from 'next/link'
import { apiGet } from '@/lib/api'

export default async function PortfolioPage() {
  let rows: Array<{ stockCode: string; stockName: string; qty: number; avg: number; now: number; profitRate: number }> = []
  let message = ''
  try {
    const res = await apiGet<any>('/broker/kis/balance')
    if (res?.error) {
      message = String(res.error)
    } else {
      const arr = Array.isArray(res?.output1) ? res.output1 : []
      rows = arr
        .map((r: any) => {
          const qty = Number(String(r.hldg_qty ?? '0').replace(/,/g, '')) || 0
          const avg = Number(String(r.pchs_avg_pric ?? '0').replace(/,/g, '')) || 0
          const now = Number(String(r.prpr ?? r.stck_prpr ?? '0').replace(/,/g, '')) || 0
          const profitRate = Number(String(r.evlu_pfls_rt ?? '0').replace(/,/g, '')) || 0
          return {
            stockCode: String(r.pdno ?? '').trim(),
            stockName: String(r.prdt_name ?? r.hts_kor_isnm ?? '').trim(),
            qty,
            avg,
            now,
            profitRate,
          }
        })
        .filter((r: any) => r.qty > 0)
    }
  } catch (error) {
    message = error instanceof Error ? error.message : String(error)
  }

  return (
    <AppShell>
      <section className="card">
        <h1>보유잔고</h1>
        {message ? (
          <p style={{ marginTop: 8, color: '#b42318' }}>
            {message.includes('계좌번호')
              ? '계좌번호가 설정되지 않았습니다. 설정 페이지에서 모의/실전 계좌번호를 입력하세요.'
              : `보유잔고 조회 실패: ${message}`}
          </p>
        ) : null}
        {message?.includes('계좌번호') ? (
          <div style={{ marginTop: 8 }}>
            <Link href="/settings" className="button">설정으로 이동</Link>
          </div>
        ) : null}
        <table className="table">
          <thead>
            <tr>
              <th>코드</th>
              <th>종목</th>
              <th>수량</th>
              <th>평단</th>
              <th>현재가</th>
              <th>손익률</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={`${r.stockCode}-${r.stockName}`}>
                <td>{r.stockCode}</td>
                <td>{r.stockName || r.stockCode}</td>
                <td>{r.qty.toLocaleString()}</td>
                <td>{r.avg.toLocaleString()}</td>
                <td>{r.now.toLocaleString()}</td>
                <td>{r.profitRate}%</td>
              </tr>
            ))}
            {rows.length === 0 ? (
              <tr>
                <td colSpan={6} style={{ textAlign: 'center', color: '#667085' }}>
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
