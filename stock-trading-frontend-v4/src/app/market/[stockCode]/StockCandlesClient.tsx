'use client'

import { useCallback, useEffect, useState } from 'react'
import { CandlesChart } from '@/components/chart/CandlesChart'
import { apiGet } from '@/lib/api'

type TabId = '1m' | '1h' | '1d' | '1w' | '1mo'

const TABS: { id: TabId; label: string }[] = [
  { id: '1m', label: '분봉' },
  { id: '1h', label: '시봉' },
  { id: '1d', label: '일봉' },
  { id: '1w', label: '주봉' },
  { id: '1mo', label: '월봉' },
]

type CandlesResponse = { tf: string; candles: Array<Record<string, number | string>> }

export function StockCandlesClient({ stockCode }: { stockCode: string }) {
  const [tf, setTf] = useState<TabId>('1d')
  const [candles, setCandles] = useState<
    Array<{ time: string; open: number; high: number; low: number; close: number; volume: number }>
  >([])
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setErr('')
    try {
      const data = await apiGet<CandlesResponse>(`/market/stocks/${encodeURIComponent(stockCode)}/candles?tf=${tf}`)
      const list = (data as CandlesResponse).candles
      if (!Array.isArray(list)) {
        setCandles([])
        return
      }
      setCandles(
        list.map((c) => ({
          time: String(c.time ?? ''),
          open: Number(c.open),
          high: Number(c.high),
          low: Number(c.low),
          close: Number(c.close),
          volume: Number(c.volume ?? 0),
        }))
      )
    } catch (e) {
      setCandles([])
      setErr(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }, [stockCode, tf])

  useEffect(() => {
    void load()
  }, [load])

  return (
    <section className="card" style={{ marginTop: 12 }}>
      <h3 style={{ marginBottom: 8 }}>캔들</h3>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', marginBottom: 12 }}>
        {TABS.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTf(t.id)}
            className="button"
            style={{
              opacity: tf === t.id ? 1 : 0.65,
              fontWeight: tf === t.id ? 600 : 400,
            }}
          >
            {t.label}
          </button>
        ))}
        {loading ? <span style={{ color: '#667085', fontSize: 14 }}>불러오는 중…</span> : null}
      </div>
      {err ? <p style={{ color: '#b42318', marginBottom: 8 }}>{err}</p> : null}
      <p style={{ color: '#667085', fontSize: 12, marginBottom: 8 }}>
        분봉·시봉은 당일 위주(한국투자 API)이며, 분봉은 연속 조회로 당일 구간을 이어 붙입니다. 시봉은 60분 단위(기술 API 미수신
        시 분봉을 집계)입니다.
      </p>
      {candles.length > 0 ? (
        <CandlesChart
          candles={candles.map((c) => ({
            time: c.time,
            open: c.open,
            high: c.high,
            low: c.low,
            close: c.close,
            volume: c.volume,
          }))}
        />
      ) : !loading && !err ? (
        <p style={{ textAlign: 'center', color: '#667085' }}>선택한 봉에 대한 데이터가 없습니다.</p>
      ) : null}
    </section>
  )
}
