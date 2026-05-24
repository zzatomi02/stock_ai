'use client'

import { useEffect, useState } from 'react'
import { AppShell } from '@/components/layout/AppShell'
import { apiDelete, apiGet, apiPost } from '@/lib/api'
import { notifyError, notifySuccess, notifyWarning } from '@/lib/toast'

type Item = {
  id: number
  stockCode: string
  stockName?: string
}

function readCookie(name: string): string {
  const m = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return decodeURIComponent(m?.[1] || '')
}

export default function WatchlistPage() {
  const [items, setItems] = useState<Item[]>([])
  const [code, setCode] = useState('')
  const [name, setName] = useState('')

  async function load() {
    if (!readCookie('user-id')) {
      notifyWarning('설정에서 user-id 쿠키를 먼저 등록하세요.')
      setItems([])
      return
    }
    try {
      setItems(await apiGet<Item[]>('/watchlist'))
    } catch (error) {
      notifyError(error)
    }
  }

  useEffect(() => {
    load()
  }, [])

  async function add() {
    try {
      await apiPost('/watchlist', { stockCode: code.trim(), stockName: name.trim() || undefined })
      setCode('')
      setName('')
      await load()
      notifySuccess('관심종목이 추가되었습니다.')
    } catch (error) {
      notifyError(error)
    }
  }

  async function remove(stockCode: string) {
    try {
      await apiDelete(`/watchlist/${stockCode}`)
      await load()
      notifySuccess('삭제되었습니다.')
    } catch (error) {
      notifyError(error)
    }
  }

  return (
    <AppShell>
      <section className="card">
        <h1>관심종목</h1>
        <p style={{ color: '#667085', fontSize: 13 }}>user-id 쿠키 기준으로 저장됩니다.</p>
        <form
          style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '12px 0' }}
          onSubmit={(e) => {
            e.preventDefault()
            add()
          }}
        >
          <input className="input" placeholder="종목코드" value={code} onChange={(e) => setCode(e.target.value)} />
          <input className="input" placeholder="종목명(선택)" value={name} onChange={(e) => setName(e.target.value)} />
          <button type="submit" className="button">
            추가
          </button>
        </form>
        <table className="table">
          <thead>
            <tr>
              <th>코드</th>
              <th>이름</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((w) => (
              <tr key={w.id}>
                <td>{w.stockCode}</td>
                <td>{w.stockName || '-'}</td>
                <td>
                  <button type="button" className="button button--ghost" onClick={() => remove(w.stockCode)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {!items.length ? (
              <tr>
                <td colSpan={3} style={{ textAlign: 'center', color: '#667085' }}>
                  등록된 관심종목이 없습니다.
                </td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
