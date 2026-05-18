'use client'

import { useCallback, useEffect, useState } from 'react'
import { AppShell } from '@/components/layout/AppShell'
import { apiDelete, apiGet, apiPost } from '@/lib/api'

type Keyword = {
  id: number
  keyword: string
  keywordType: string
  category?: string
  weight: number
  description?: string
  isActive: boolean
}

export default function NewsKeywordsPage() {
  const [list, setList] = useState<Keyword[]>([])
  const [form, setForm] = useState({ keyword: '', keywordType: 'POSITIVE', weight: 10, description: '' })
  const [msg, setMsg] = useState('')

  const load = useCallback(async () => {
    try {
      setList(await apiGet<Keyword[]>('/news/keywords'))
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function add() {
    await apiPost('/news/keywords', form)
    setForm({ keyword: '', keywordType: 'POSITIVE', weight: 10, description: '' })
    await load()
  }

  return (
    <AppShell>
      <section className="card">
        <h1>뉴스 키워드 관리</h1>
        <p style={{ color: '#667085', fontSize: 13 }}>긍정/부정/리스크 키워드는 DB에서 관리됩니다. 매칭 시 이력이 저장됩니다.</p>
        {msg ? <p style={{ color: '#b42318' }}>{msg}</p> : null}
        <form
          style={{ display: 'flex', gap: 8, flexWrap: 'wrap', margin: '12px 0' }}
          onSubmit={(e) => {
            e.preventDefault()
            add()
          }}
        >
          <input className="input" placeholder="키워드" value={form.keyword} onChange={(e) => setForm({ ...form, keyword: e.target.value })} />
          <select className="select" value={form.keywordType} onChange={(e) => setForm({ ...form, keywordType: e.target.value })} style={{ width: 140 }}>
            <option value="POSITIVE">긍정</option>
            <option value="NEGATIVE">부정</option>
            <option value="RISK">리스크</option>
          </select>
          <input className="input" type="number" placeholder="가중치" value={form.weight} onChange={(e) => setForm({ ...form, weight: Number(e.target.value) })} style={{ width: 100 }} />
          <button type="submit" className="button">
            등록
          </button>
        </form>
        <table className="table">
          <thead>
            <tr>
              <th>키워드</th>
              <th>유형</th>
              <th>가중치</th>
              <th>설명</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {list.map((k) => (
              <tr key={k.id}>
                <td>{k.keyword}</td>
                <td>{k.keywordType}</td>
                <td>{k.weight}</td>
                <td>{k.description || '-'}</td>
                <td>
                  <button type="button" className="button button--ghost" onClick={() => apiDelete(`/news/keywords/${k.id}`).then(load)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </AppShell>
  )
}
