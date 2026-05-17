'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { AppShell } from '@/components/layout/AppShell'
import { apiDelete, apiGet, apiPost, apiPut } from '@/lib/api'

type TemplateRow = {
  id: number
  name: string
  systemPrompt: string
  userMessageTemplate: string
  openaiModel: string
  temperature: number
  defaultTemplate: boolean
  createdAt?: string | null
  updatedAt?: string | null
}

type PreviewResult = {
  templateId: number
  templateName: string
  model: string
  rawContent: string
  parsedScore: number
}

const PLACEHOLDERS = ['{{stockCode}}', '{{stockName}}', '{{title}}', '{{summary}}'] as const

export default function LlmPromptsPage() {
  const [list, setList] = useState<TemplateRow[]>([])
  const [loading, setLoading] = useState(true)
  const [err, setErr] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const [editing, setEditing] = useState<TemplateRow | null>(null)
  const [isNew, setIsNew] = useState(false)

  const [pvTitle, setPvTitle] = useState('삼성전자, 4분기 청신호')
  const [pvSummary, setPvSummary] = useState('담당 씨: 메모리 반등 기대, 실적 턴어라운드…')
  const [pvStock, setPvStock] = useState('005930')
  const [pvName, setPvName] = useState('삼성전자')
  const [preview, setPreview] = useState<PreviewResult | null>(null)
  const [prevLoading, setPrevLoading] = useState(false)

  const load = useCallback(async () => {
    setErr(null)
    setLoading(true)
    try {
      const rows = await apiGet<TemplateRow[]>('/llm/question-templates')
      setList(Array.isArray(rows) ? rows : [])
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e))
      setList([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  function openNew() {
    setIsNew(true)
    setEditing({
      id: 0,
      name: 'my-template',
      systemPrompt: '당신은 한국 뉴스를 분석하고 JSON으로 score(0-100)만 답합니다.',
      userMessageTemplate: '[{{stockCode}}] {{title}}\n\n{{summary}}',
      openaiModel: 'gpt-4o-mini',
      temperature: 0.2,
      defaultTemplate: false,
    })
  }

  function openEdit(t: TemplateRow) {
    setIsNew(false)
    setEditing({ ...t })
  }

  async function save() {
    if (!editing) return
    setMsg(null)
    setErr(null)
    try {
      if (isNew) {
        await apiPost<{ id: number }>('/llm/question-templates', {
          name: editing.name,
          systemPrompt: editing.systemPrompt,
          userMessageTemplate: editing.userMessageTemplate,
          openaiModel: editing.openaiModel,
          temperature: editing.temperature,
        })
        setMsg('새 템플릿이 저장되었습니다. 기본으로 쓰려면 목록에서 «기본으로»를 누르세요.')
      } else {
        await apiPut(`/llm/question-templates/${editing.id}`, {
          name: editing.name,
          systemPrompt: editing.systemPrompt,
          userMessageTemplate: editing.userMessageTemplate,
          openaiModel: editing.openaiModel,
          temperature: editing.temperature,
        })
        setMsg('저장되었습니다.')
      }
      setEditing(null)
      setIsNew(false)
      await load()
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e))
    }
  }

  async function setDefault(id: number) {
    setMsg(null)
    setErr(null)
    try {
      await apiPost(`/llm/question-templates/${id}/set-default`, {})
      setMsg('기본 템플릿이 지정되었습니다.')
      await load()
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e))
    }
  }

  async function remove(id: number) {
    if (!window.confirm('이 템플릿을 삭제할까요?')) return
    setMsg(null)
    setErr(null)
    try {
      await apiDelete(`/llm/question-templates/${id}`)
      if (editing?.id === id) setEditing(null)
      setMsg('삭제되었습니다.')
      await load()
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e))
    }
  }

  async function runPreview(useTemplateId: number | null) {
    setPreview(null)
    setPrevLoading(true)
    setErr(null)
    try {
      const body: Record<string, unknown> = {
        title: pvTitle,
        summary: pvSummary,
        stockCode: pvStock,
        stockName: pvName,
      }
      if (useTemplateId != null) {
        body.templateId = useTemplateId
      }
      const r = await apiPost<PreviewResult>('/llm/question-templates/preview', body)
      setPreview(r)
      setMsg('미리보기가 완료되었습니다. (백엔드 콘솔에 【OPENAI-LLM】 로그를 확인하세요.)')
    } catch (e) {
      setErr(e instanceof Error ? e.message : String(e))
    } finally {
      setPrevLoading(false)
    }
  }

  return (
    <AppShell>
      <section className="card">
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 12, marginBottom: 12 }}>
          <h1 style={{ margin: 0, flex: '1 1 auto' }}>LLM 뉴스 질문 템플릿 (CRUD)</h1>
          <Link href="/news" className="button button--ghost" style={{ textAlign: 'center' }}>
            뉴스·키워드 점수
          </Link>
          <Link href="/settings" className="button button--ghost" style={{ textAlign: 'center' }}>
            ← 설정
          </Link>
          <button type="button" className="button" onClick={() => void load()} disabled={loading}>
            {loading ? '로딩…' : '목록 새로고침'}
          </button>
        </div>
        <p style={{ color: '#8b9dc7', fontSize: 14, lineHeight: 1.5, margin: '0 0 16px' }}>
          <b>systemPrompt</b>와 <b>userMessageTemplate</b>을 DB(JPA)에 저장합니다. 사용자 템플릿에는{' '}
          {PLACEHOLDERS.join(', ')} 를 넣을 수 있으며, 뉴스 ingest·미리보기 시 치환됩니다. API 키는 서버
          환경 변수 <code>OPENAI_API_KEY</code> (또는 <code>app.openai.api-key</code>)로만 둡니다.
        </p>
        {err ? (
          <p style={{ color: '#f87171', marginBottom: 12, fontSize: 14 }}>
            {err}
            <br />
            <span style={{ color: '#9fb2d9', fontSize: 12 }}>
              수집 후 자동 LLM·비용: <code>app.openai.analyze-on-ingest</code> (로컬 기본 false)
            </span>
          </p>
        ) : null}
        {msg ? <p style={{ color: '#6ee7b7', marginBottom: 12, fontSize: 14 }}>{msg}</p> : null}

        <div className="card" style={{ background: 'rgba(15, 23, 42, 0.5)', marginBottom: 20 }}>
          <h3 style={{ marginTop: 0 }}>치환·프롬프트 품질 가이드</h3>
          <ul style={{ margin: 0, paddingLeft: 18, color: '#b8c5da', fontSize: 13, lineHeight: 1.6 }}>
            <li>
              <b>JSON만 출력</b>하도록 system에 고정 스키마( score, events, sentiment, rationale )를 쓰면 파싱이
              안정적입니다.
            </li>
            <li>user 템플릿 끝에 &quot;JSON만, 마크다운 금지&quot; 한 줄을 추가하면 응답 품질이 올라가는
              경우가 많습니다.
            </li>
            <li>모델: 비용·속도 <code>gpt-4o-mini</code>, 품질 <code>gpt-4o</code> 등으로 바꿀 수 있습니다.</li>
          </ul>
        </div>

        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginBottom: 20 }}>
          <button type="button" className="button" onClick={openNew}>
            + 새 템플릿
          </button>
        </div>

        <div style={{ overflowX: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>이름</th>
                <th>모델</th>
                <th>기본</th>
                <th>동작</th>
              </tr>
            </thead>
            <tbody>
              {loading && list.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: '#667085' }}>
                    불러오는 중…
                  </td>
                </tr>
              ) : list.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ textAlign: 'center', color: '#667085' }}>
                    템플릿이 없습니다. «새 템플릿» 또는 백엔드 기동 시 기본 seed를 확인하세요.
                  </td>
                </tr>
              ) : (
                list.map((t) => (
                  <tr key={t.id}>
                    <td style={{ fontFamily: 'ui-monospace,monospace' }}>{t.id}</td>
                    <td>{t.name}</td>
                    <td style={{ fontSize: 12 }}>{t.openaiModel}</td>
                    <td>{t.defaultTemplate ? '★' : '—'}</td>
                    <td style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                      <button type="button" className="button button--ghost" onClick={() => openEdit(t)}>
                        편집
                      </button>
                      <button type="button" className="button button--ghost" onClick={() => void setDefault(t.id)} disabled={t.defaultTemplate}>
                        기본으로
                      </button>
                      <button
                        type="button"
                        className="button button--danger"
                        onClick={() => void remove(t.id)}
                        disabled={t.defaultTemplate && list.length === 1}
                      >
                        삭제
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {editing ? (
        <section className="card" style={{ marginTop: 20 }}>
          <h2 style={{ marginTop: 0 }}>{isNew ? '새 템플릿' : `편집 #${editing.id}`}</h2>
          <div style={{ display: 'grid', gap: 10, maxWidth: 900 }}>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>이름(고유)</div>
              <input
                className="input"
                value={editing.name}
                onChange={(e) => setEditing({ ...editing, name: e.target.value })}
                disabled={!isNew}
              />
            </label>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>OpenAI 모델</div>
              <input
                className="input"
                value={editing.openaiModel}
                onChange={(e) => setEditing({ ...editing, openaiModel: e.target.value })}
              />
            </label>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>temperature (0~2)</div>
              <input
                className="input"
                type="number"
                step={0.1}
                min={0}
                max={2}
                value={editing.temperature}
                onChange={(e) => setEditing({ ...editing, temperature: Number(e.target.value) })}
              />
            </label>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>systemPrompt</div>
              <textarea
                className="input"
                rows={5}
                style={{ fontFamily: 'ui-monospace,monospace', fontSize: 13, resize: 'vertical' }}
                value={editing.systemPrompt}
                onChange={(e) => setEditing({ ...editing, systemPrompt: e.target.value })}
              />
            </label>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>userMessageTemplate</div>
              <textarea
                className="input"
                rows={10}
                style={{ fontFamily: 'ui-monospace,monospace', fontSize: 13, resize: 'vertical' }}
                value={editing.userMessageTemplate}
                onChange={(e) => setEditing({ ...editing, userMessageTemplate: e.target.value })}
              />
            </label>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <button type="button" className="button" onClick={() => void save()}>
                저장
              </button>
              <button type="button" className="button button--ghost" onClick={() => setEditing(null)}>
                취소
              </button>
            </div>
          </div>
        </section>
      ) : null}

      <section className="card" style={{ marginTop: 20 }}>
        <h2 style={{ marginTop: 0 }}>미리보기 (OpenAI 실호출)</h2>
        <p style={{ color: '#8b9dc7', fontSize: 13 }}>
          아래 샘플로 치환 후 GPT를 호출합니다. 백엔드 로그에 <code>【OPENAI-LLM】</code>·<code>【LLM-PREVIEW】</code> 가
          출력됩니다.
        </p>
        <div style={{ display: 'grid', gap: 8, maxWidth: 800 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
            <label>
              <span style={{ fontSize: 12, color: '#8b9dc7' }}>stockCode</span>
              <input className="input" value={pvStock} onChange={(e) => setPvStock(e.target.value)} />
            </label>
            <label>
              <span style={{ fontSize: 12, color: '#8b9dc7' }}>stockName</span>
              <input className="input" value={pvName} onChange={(e) => setPvName(e.target.value)} />
            </label>
          </div>
          <label>
            <span style={{ fontSize: 12, color: '#8b9dc7' }}>제목</span>
            <input className="input" value={pvTitle} onChange={(e) => setPvTitle(e.target.value)} />
          </label>
          <label>
            <span style={{ fontSize: 12, color: '#8b9dc7' }}>요약</span>
            <textarea
              className="input"
              rows={3}
              value={pvSummary}
              onChange={(e) => setPvSummary(e.target.value)}
            />
          </label>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            <button
              type="button"
              className="button"
              disabled={prevLoading}
              onClick={() => void runPreview(null)}
            >
              {prevLoading ? '요청 중…' : '기본 템플릿으로 미리보기'}
            </button>
            {list
              .filter((t) => t.id)
              .map((t) => (
                <button
                  key={t.id}
                  type="button"
                  className="button button--ghost"
                  disabled={prevLoading}
                  onClick={() => void runPreview(t.id)}
                >
                  # {t.id} {t.name}
                </button>
              ))}
          </div>
        </div>
        {preview ? (
          <div
            style={{
              marginTop: 16,
              padding: 12,
              background: '#0a1020',
              borderRadius: 12,
              border: '1px solid #223150',
            }}
          >
            <div style={{ fontSize: 12, color: '#8b9dc7', marginBottom: 8 }}>
              템플릿: {preview.templateName} (id={preview.templateId}) · 모델: {preview.model} · 파싱 score:{' '}
              <b style={{ color: '#6ee7b7' }}>{preview.parsedScore}</b>
            </div>
            <pre
              style={{
                margin: 0,
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                fontSize: 12,
                color: '#e2e8f0',
                maxHeight: 360,
                overflow: 'auto',
              }}
            >
              {preview.rawContent}
            </pre>
          </div>
        ) : null}
      </section>
    </AppShell>
  )
}
