import { AppShell } from '@/components/layout/AppShell'
import { apiGet } from '@/lib/api'
import { StockCandlesClient } from './StockCandlesClient'
import { PaperBuyTestButton } from './PaperBuyTestButton'

type NewsArticle = {
  id: number
  title: string
  summary: string | null
  sourceName: string | null
  articleUrl: string | null
  publishedAt: string | null
  collectedAt: string | null
  keywordScore: number
  aiScore: number
}

function fmtNewsTime(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' })
}

export default async function StockDetailPage({
  params,
}: {
  params: Promise<{ stockCode: string }>
}) {
  const { stockCode } = await params
  const data = await apiGet<any>(`/market/stocks/${stockCode}`)

  let news: NewsArticle[] = []
  try {
    const list = await apiGet<NewsArticle[]>(
      `/news/articles?stockCode=${encodeURIComponent(String(data.stockCode))}&limit=15`
    )
    news = Array.isArray(list) ? list : []
  } catch {
    news = []
  }

  return (
    <AppShell>
      <section className="card">
        <h1>{data.stockName} ({data.stockCode})</h1>
        <div className="grid-4">
          <div className="card"><div>현재가</div><h2>{data.price.toLocaleString()}원</h2></div>
          <div className="card"><div>등락률</div><h2>{data.changeRate}%</h2></div>
          <div className="card"><div>뉴스 점수</div><h2>{data.newsScore}</h2></div>
          <div className="card"><div>AI 점수</div><h2>{data.aiScore}</h2></div>
        </div>
      </section>
      <StockCandlesClient stockCode={String(data.stockCode)} />
      <section className="card" style={{ marginTop: 20 }}>
        <h3>관련 뉴스(DB)</h3>
        <p style={{ color: '#8b9dc7', fontSize: 13, marginTop: 4 }}>
          네이버 검색·ingest로 DB에 쌓인 기사만 표시됩니다. 비어 있으면 백엔드{' '}
          <code style={{ fontSize: 12 }}>POST /api/news/ingest?query=...&amp;stockCode=...</code> 로 먼저 수집할 수
          있습니다.
        </p>
        {news.length === 0 ? (
          <p style={{ color: '#667085', marginTop: 12 }}>표시할 뉴스가 없습니다.</p>
        ) : (
          <ul style={{ listStyle: 'none', margin: '12px 0 0', padding: 0, display: 'grid', gap: 12 }}>
            {news.map((a) => (
              <li
                key={a.id}
                style={{
                  border: '1px solid #223150',
                  borderRadius: 12,
                  padding: 12,
                  background: 'rgba(15, 23, 42, 0.4)',
                }}
              >
                <div style={{ fontSize: 12, color: '#8b9dc7', marginBottom: 4 }}>
                  {a.sourceName ? `${a.sourceName} · ` : ''}
                  {fmtNewsTime(a.publishedAt) || '일시 미상'}
                  {' · '}
                  키워드 {a.keywordScore} / AI {a.aiScore}
                </div>
                {a.articleUrl ? (
                  <a
                    href={a.articleUrl}
                    target="_blank"
                    rel="noreferrer"
                    style={{ color: '#93c5fd', fontWeight: 600, fontSize: 15 }}
                  >
                    {a.title}
                  </a>
                ) : (
                  <span style={{ fontWeight: 600 }}>{a.title}</span>
                )}
                {a.summary ? <p style={{ margin: '8px 0 0', fontSize: 13, color: '#b8c5da' }}>{a.summary}</p> : null}
              </li>
            ))}
          </ul>
        )}
      </section>
      <section className="grid-2" style={{ marginTop: 20 }}>
        <div className="card"><h3>긍정 키워드</h3><div className="tabs">{Array.isArray(data.positiveKeywords) && data.positiveKeywords.length > 0 ? data.positiveKeywords.map((k:string)=><span key={k} className="badge">{k}</span>) : <span style={{ color: '#667085' }}>데이터가 없습니다.</span>}</div></div>
        <div className="card">
          <h3>자동매매 참고</h3>
          <p>차트 / 뉴스 / AI 점수를 종합해 최종 {data.finalScore}점입니다.</p>
          <PaperBuyTestButton stockCode={String(data.stockCode)} stockName={String(data.stockName)} />
        </div>
      </section>
    </AppShell>
  )
}
