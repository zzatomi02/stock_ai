'use client'
import { useFilterStore } from '@/store/filters'

export function DateRangeFilter() {
  const { range, from, to, keyword, setRange, setDates, setKeyword } = useFilterStore()
  return (
    <div className="card" style={{ display: 'grid', gap: 12 }}>
      <div className="tabs">
        {['today', 'week', 'month', 'custom'].map((v) => (
          <button key={v} className={`tab ${range === v ? 'active' : ''}`} onClick={() => setRange(v as never)}>{v === 'today' ? '당일' : v === 'week' ? '1주일' : v === 'month' ? '1개월' : '직접선택'}</button>
        ))}
      </div>
      {range === 'custom' && (
        <div className="grid-2">
          <input className="input" type="date" value={from || ''} onChange={(e) => setDates(e.target.value, to)} />
          <input className="input" type="date" value={to || ''} onChange={(e) => setDates(from, e.target.value)} />
        </div>
      )}
      <input className="input" placeholder="키워드 검색 (예: MOU, 합병)" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
    </div>
  )
}
