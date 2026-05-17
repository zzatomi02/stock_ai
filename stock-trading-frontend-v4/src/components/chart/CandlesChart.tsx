'use client'
import { useEffect, useRef } from 'react'
import { CandlestickSeries, createChart } from 'lightweight-charts'
import type { CandlestickData, UTCTimestamp } from 'lightweight-charts'

/** lightweight-charts: time 은 오름차순이어야 하며 동일 time 중복 불가 */
function prepareCandles(
  candles: Array<{ time: string | number; open: number; high: number; low: number; close: number; volume?: number }>
): CandlestickData<UTCTimestamp>[] {
  const rows = candles.map((c) => {
    const sec = parseCandleTimeSec(c.time)
    return { sec, open: c.open, high: c.high, low: c.low, close: c.close }
  })
  rows.sort((a, b) => a.sec - b.sec)
  const bySec = new Map<number, (typeof rows)[0]>()
  for (const r of rows) bySec.set(r.sec, r)
  return [...bySec.values()]
    .sort((a, b) => a.sec - b.sec)
    .map((r) => ({
      time: r.sec as UTCTimestamp,
      open: r.open,
      high: r.high,
      low: r.low,
      close: r.close,
    }))
}

function parseCandleTimeSec(t: string | number): number {
  if (typeof t === 'number') {
    return t > 1e12 ? Math.floor(t / 1000) : t
  }
  const s = String(t).trim()
  if (/^\d+$/.test(s)) {
    const n = parseInt(s, 10)
    return n > 1e12 ? Math.floor(n / 1000) : n
  }
  const ms = Date.parse(s)
  if (Number.isNaN(ms)) return 0
  return Math.floor(ms / 1000)
}

export function CandlesChart({ candles }: { candles: Array<{ time: string; open: number; high: number; low: number; close: number; volume: number }> }) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!ref.current) return
    ref.current.innerHTML = ''
    const chart = createChart(ref.current, { width: ref.current.clientWidth, height: 360, layout: { background: { color: '#121b2f' }, textColor: '#dce6ff' }, grid: { vertLines: { color: '#223150' }, horzLines: { color: '#223150' } } })
    const series = chart.addSeries(CandlestickSeries)
    series.setData(prepareCandles(candles))
    const resize = () => chart.applyOptions({ width: ref.current?.clientWidth || 700 })
    window.addEventListener('resize', resize)
    return () => { window.removeEventListener('resize', resize); chart.remove() }
  }, [candles])
  return <div ref={ref} className="card" />
}
