'use client'

import Link from 'next/link'
import { AppShell } from '@/components/layout/AppShell'
import { DateRangeFilter } from '@/components/common/DateRangeFilter'
import { apiDelete, apiGet, apiPost, apiPut } from '@/lib/api'
import { notifyError, notifyInfo, notifySuccess, notifyWarning } from '@/lib/toast'
import { useCallback, useEffect, useState } from 'react'

type KeywordRule = {
  id: number
  keyword: string
  score: number
  polarity: string
  active: boolean
  description?: string | null
  createdAt?: string
}

type ScorePreview = {
  keywordScore: number
  aiScore: number
  finalScore: number
  matchedKeywords: string[]
}

const emptyForm = () => ({
  keyword: '',
  score: 5,
  polarity: 'positive',
  description: '',
  active: true,
})

export default function NewsPage() {
  const [rules, setRules] = useState<KeywordRule[]>([])
  const [loading, setLoading] = useState(true)

  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editForm, setEditForm] = useState(emptyForm)

  const [pvStock, setPvStock] = useState('005930')
  const [pvTitle, setPvTitle] = useState('삼성전자, 반도체 회복 기대')
  const [pvSummary, setPvSummary] = useState('거래소에 MOU 체결 소식이 전해지며 수주 기대감이 커졌다.')
  const [preview, setPreview] = useState<ScorePreview | null>(null)
  const [pvLoading, setPvLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const r = await apiGet<KeywordRule[]>('/news/rules')
      setRules(Array.isArray(r) ? r : [])
    } catch (error) {
      notifyError(error)
      setRules([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function createRule() {
    if (!form.keyword.trim()) {
      notifyWarning('키워드를 입력하세요.')
      return
    }
    try {
      await apiPost('/news/rules', {
        keyword: form.keyword.trim(),
        score: form.score,
        polarity: form.polarity.trim(),
        description: form.description.trim() || undefined,
      })
      notifySuccess('규칙이 추가되었습니다. (백엔드 로그: 【NEWS-KEYWORD】)')
      setForm(emptyForm())
      await load()
    } catch (error) {
      notifyError(error)
    }
  }

  function startEdit(r: KeywordRule) {
    setEditingId(r.id)
    setEditForm({
      keyword: r.keyword,
      score: r.score,
      polarity: r.polarity,
      description: r.description ?? '',
      active: r.active,
    })
  }

  async function saveEdit() {
    if (editingId == null) return
    try {
      await apiPut(`/news/rules/${editingId}`, {
        keyword: editForm.keyword.trim(),
        score: editForm.score,
        polarity: editForm.polarity.trim(),
        description: editForm.description.trim() || null,
        active: editForm.active,
      })
      notifySuccess('규칙이 수정되었습니다.')
      setEditingId(null)
      await load()
    } catch (error) {
      notifyError(error)
    }
  }

  async function remove(id: number) {
    if (!window.confirm('이 키워드 규칙을 삭제할까요?')) return
    try {
      await apiDelete(`/news/rules/${id}`)
      if (editingId === id) setEditingId(null)
      notifySuccess('삭제되었습니다.')
      await load()
    } catch (error) {
      notifyError(error)
    }
  }

  async function runPreview() {
    setPreview(null)
    setPvLoading(true)
    try {
      const r = await apiPost<ScorePreview>('/news/score/preview', {
        stockCode: pvStock,
        title: pvTitle,
        summary: pvSummary,
      })
      setPreview(r)
      notifyInfo('미리보기 완료 (저장 아님). 콘솔에 【NEWS-KEYWORD】 키워드 미리보기 로그가 남습니다.')
    } catch (error) {
      notifyError(error)
    } finally {
      setPvLoading(false)
    }
  }

  return (
    <AppShell>
      <DateRangeFilter />
      <section className="card" style={{ marginBottom: 20 }}>
        <h1 style={{ marginTop: 0 }}>뉴스 · 키워드 점수 (CRUD)</h1>
        <p style={{ color: '#8b9dc7', fontSize: 14, lineHeight: 1.5 }}>
          제목+요약에 <b>포함(부분일치, 소문자)</b>된 활성 규칙의 <code>score</code>를 50에 더해 0~100으로 만듭니다. GPT와는
          별도입니다.{' '}
          <Link href="/settings/llm-prompts" style={{ color: '#93c5fd' }}>
            LLM 질문 템플릿
          </Link>
          {' · '}
          <Link href="/news/ingest" style={{ color: '#93c5fd' }}>
            Naver 뉴스 수집
          </Link>
        </p>
        <p style={{ marginTop: 8 }}>
          <button type="button" className="button button--ghost" onClick={() => void load()} disabled={loading}>
            {loading ? '로딩…' : '목록 새로고침'}
          </button>
        </p>
      </section>

      <div className="grid-2" style={{ marginBottom: 20 }}>
        <div className="card">
          <h2 style={{ marginTop: 0 }}>새 규칙 추가</h2>
          <div style={{ display: 'grid', gap: 8, maxWidth: 420 }}>
            <label>
              <span style={{ fontSize: 12, color: '#8b9dc7' }}>키워드 (소문자로 비교)</span>
              <input
                className="input"
                value={form.keyword}
                onChange={(e) => setForm((f) => ({ ...f, keyword: e.target.value }))}
                placeholder="mou, 상장, 악재 …"
              />
            </label>
            <label>
              <span style={{ fontSize: 12, color: '#8b9dc7' }}>가산 점수(음수로 악재 가능)</span>
              <input
                type="number"
                className="input"
                value={form.score}
                onChange={(e) => setForm((f) => ({ ...f, score: Number(e.target.value) }))}
              />
            </label>
            <label>
              <span style={{ fontSize: 12, color: '#8b9dc7' }}>성격 (positive/negative 등)</span>
              <input
                className="input"
                value={form.polarity}
                onChange={(e) => setForm((f) => ({ ...f, polarity: e.target.value }))}
              />
            </label>
            <label>
              <span style={{ fontSize: 12, color: '#8b9dc7' }}>설명(선택)</span>
              <input
                className="input"
                value={form.description}
                onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
              />
            </label>
            <button type="button" className="button" onClick={() => void createRule()}>
              등록
            </button>
          </div>
        </div>
        <div className="card">
          <h2 style={{ marginTop: 0 }}>점수 시뮬(저장 없음)</h2>
          <p style={{ color: '#8b9dc7', fontSize: 12 }}>활성 규칙만 반영. GPT 점수는 여기엔 없습니다.</p>
          <div style={{ display: 'grid', gap: 8, maxWidth: 480 }}>
            <input
              className="input"
              value={pvStock}
              onChange={(e) => setPvStock(e.target.value)}
              placeholder="종목코드"
            />
            <input
              className="input"
              value={pvTitle}
              onChange={(e) => setPvTitle(e.target.value)}
              placeholder="제목"
            />
            <textarea
              className="input"
              rows={3}
              value={pvSummary}
              onChange={(e) => setPvSummary(e.target.value)}
              placeholder="요약"
            />
            <button type="button" className="button" disabled={pvLoading} onClick={() => void runPreview()}>
              {pvLoading ? '계산 중…' : '미리보기'}
            </button>
            {preview ? (
              <div style={{ fontSize: 14, lineHeight: 1.6 }}>
                <div>
                  <b>keywordScore</b> {preview.keywordScore} (고정 50 + 규칙 합, 클램프)
                </div>
                <div>
                  <b>finalScore</b> (키·AI 평균) {preview.finalScore}
                </div>
                <div>
                  <b>매칭</b> {(preview.matchedKeywords || []).join(', ') || '없음'}
                </div>
              </div>
            ) : null}
          </div>
        </div>
      </div>

      <section className="card">
        <h2 style={{ marginTop: 0 }}>규칙 목록 (수정/삭제)</h2>
        <div style={{ overflowX: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>키워드</th>
                <th>가산</th>
                <th>성격</th>
                <th>활성</th>
                <th>설명</th>
                <th>동작</th>
              </tr>
            </thead>
            <tbody>
              {loading && rules.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                    불러오는 중…
                  </td>
                </tr>
              ) : rules.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: '#667085' }}>
                    규칙이 없습니다.
                  </td>
                </tr>
              ) : (
                rules.map((r) => (
                  <tr key={r.id}>
                    <td style={{ fontFamily: 'ui-monospace,monospace' }}>{r.id}</td>
                    <td>{r.keyword}</td>
                    <td>{r.score}</td>
                    <td>{r.polarity}</td>
                    <td>{r.active ? '예' : '아니오'}</td>
                    <td style={{ maxWidth: 200, fontSize: 12, wordBreak: 'break-word' }}>{r.description || '—'}</td>
                    <td>
                      {editingId === r.id ? null : (
                        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                          <button type="button" className="button button--ghost" onClick={() => startEdit(r)}>
                            편집
                          </button>
                          <button type="button" className="button button--danger" onClick={() => void remove(r.id)}>
                            삭제
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {editingId != null ? (
        <section className="card" style={{ marginTop: 20 }}>
          <h2>편집 (id={editingId})</h2>
          <div style={{ display: 'grid', gap: 8, maxWidth: 480 }}>
            <input
              className="input"
              value={editForm.keyword}
              onChange={(e) => setEditForm((f) => ({ ...f, keyword: e.target.value }))}
            />
            <input
              type="number"
              className="input"
              value={editForm.score}
              onChange={(e) => setEditForm((f) => ({ ...f, score: Number(e.target.value) }))}
            />
            <input
              className="input"
              value={editForm.polarity}
              onChange={(e) => setEditForm((f) => ({ ...f, polarity: e.target.value }))}
            />
            <input
              className="input"
              value={editForm.description}
              onChange={(e) => setEditForm((f) => ({ ...f, description: e.target.value }))}
            />
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#b8c5da' }}>
              <input
                type="checkbox"
                checked={editForm.active}
                onChange={(e) => setEditForm((f) => ({ ...f, active: e.target.checked }))}
              />
              활성(비활성 시 키워드 점수에서 제외)
            </label>
            <div style={{ display: 'flex', gap: 8 }}>
              <button type="button" className="button" onClick={() => void saveEdit()}>
                저장
              </button>
              <button type="button" className="button button--ghost" onClick={() => setEditingId(null)}>
                취소
              </button>
            </div>
          </div>
        </section>
      ) : null}
    </AppShell>
  )
}
