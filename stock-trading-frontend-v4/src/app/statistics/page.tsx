'use client'
import { useEffect, useMemo, useState } from 'react'
import { AppShell } from '@/components/layout/AppShell'
import { DateRangeFilter } from '@/components/common/DateRangeFilter'
import { apiGet } from '@/lib/api'
import { useFilterStore } from '@/store/filters'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, LineChart, Line } from 'recharts'

export default function StatisticsPage() {
  const { range, keyword, from, to } = useFilterStore()
  const [data, setData] = useState<any | null>(null)
  const query = useMemo(() => {
    const p = new URLSearchParams({ range })
    if (keyword) p.set('keyword', keyword)
    if (from) p.set('from', from)
    if (to) p.set('to', to)
    return p.toString()
  }, [range, keyword, from, to])
  useEffect(() => { apiGet(`/statistics/trades?${query}`).then(setData) }, [query])
  return (
    <AppShell>
      <DateRangeFilter />
      {data && <>
        <section className="grid-4">
          <div className="card"><div>총 거래</div><h2>{data.summary.totalTrades}</h2></div>
          <div className="card"><div>승/패</div><h2>{data.summary.winCount} / {data.summary.lossCount}</h2></div>
          <div className="card"><div>평균 수익률</div><h2>{Number(data.summary.avgProfitRate).toFixed(2)}%</h2></div>
          <div className="card"><div>총 손익</div><h2>{Number(data.summary.totalProfitAmount).toLocaleString()}원</h2></div>
        </section>
        <section className="grid-2">
          <div className="card" style={{ height: 360 }}>
            <h3>기간별 손익</h3>
            <ResponsiveContainer width="100%" height="100%"><LineChart data={data.timeline}><XAxis dataKey="tradeDate" /><YAxis /><Tooltip /><Line type="monotone" dataKey="profitAmount" /></LineChart></ResponsiveContainer>
          </div>
          <div className="card" style={{ height: 360 }}>
            <h3>키워드 성과</h3>
            <ResponsiveContainer width="100%" height="100%"><BarChart data={data.keywords}><XAxis dataKey="keyword" /><YAxis /><Tooltip /><Bar dataKey="totalProfitAmount" /></BarChart></ResponsiveContainer>
          </div>
        </section>
      </>}
    </AppShell>
  )
}
