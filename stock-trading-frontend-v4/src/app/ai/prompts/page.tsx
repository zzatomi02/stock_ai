'use client'

import { useCallback, useEffect, useState } from 'react'
import Link from 'next/link'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet, apiPost } from '@/lib/api'

type Prompt = {
  id: number
  promptCode: string
  promptName: string
  promptType: string
  modelName: string
  systemPrompt: string
  userPrompt: string
  temperature: number
  isActive: boolean
  version: number
}

type TestResult = {
  testLogId: number
  rawResponse?: string
  impactScore: number
  success: boolean
  errorMessage?: string
}

export default function AiPromptsPage() {
  const [list, setList] = useState<Prompt[]>([])
  const [editing, setEditing] = useState<Partial<Prompt> | null>(null)
  const [testOut, setTestOut] = useState<TestResult | null>(null)
  const [msg, setMsg] = useState('')

  const load = useCallback(async () => {
    setList(await apiGet<Prompt[]>('/ai/prompts'))
  }, [])

  useEffect(() => {
    load()
  }, [load])

  async function saveNew() {
    if (!editing) return
    await apiPost('/ai/prompts', {
      promptCode: editing.promptCode || 'NEWS_ANALYSIS',
      promptName: editing.promptName || '새 프롬프트',
      promptType: editing.promptType || 'NEWS',
      modelName: editing.modelName || 'gpt-4o-mini',
      systemPrompt: editing.systemPrompt,
      userPrompt: editing.userPrompt,
      temperature: editing.temperature ?? 0.2,
    })
    setEditing(null)
    setMsg('등록됨. 활성화는 목록에서 «활성»을 누르세요.')
    await load()
  }

  async function newVersion(id: number) {
    if (!editing) return
    await apiPost(`/ai/prompts/${id}/version`, {
      promptName: editing.promptName,
      systemPrompt: editing.systemPrompt,
      userPrompt: editing.userPrompt,
      temperature: editing.temperature,
    })
    setMsg('새 버전이 생성되었습니다.')
    await load()
  }

  async function activate(id: number) {
    await apiPost(`/ai/prompts/${id}/activate`, {})
    setMsg('활성화됨')
    await load()
  }

  async function runTest(id: number) {
    const r = await apiPost<TestResult>(`/ai/prompts/${id}/test`, {
      stockCode: '005930',
      stockName: '삼성전자',
      title: '대규모 공급계약 체결',
      summary: '해외 고객사와 장기 공급계약을 맺었다는 보도',
    })
    setTestOut(r)
  }

  return (
    <AppShell>
      <section className="card">
        <h1>GPT 프롬프트 템플릿 (DB 관리)</h1>
        <p style={{ color: '#667085', fontSize: 13 }}>
          소스코드에 하드코딩하지 않습니다. 버전별 관리·활성화·테스트 결과는 DB에 저장됩니다.
        </p>
        {msg ? <p style={{ color: '#6ee7b7' }}>{msg}</p> : null}
        <button type="button" className="button" style={{ marginBottom: 12 }} onClick={() => setEditing({ promptCode: 'NEWS_ANALYSIS', promptName: '뉴스 분석', userPrompt: '{{title}}\n{{summary}}', systemPrompt: '', temperature: 0.2 })}>
          새 프롬프트
        </button>
        {editing ? (
          <div className="card" style={{ marginBottom: 16, background: '#0f1728' }}>
            <input className="input" placeholder="promptCode" value={editing.promptCode || ''} onChange={(e) => setEditing({ ...editing, promptCode: e.target.value })} />
            <textarea className="input" rows={4} placeholder="systemPrompt" value={editing.systemPrompt || ''} onChange={(e) => setEditing({ ...editing, systemPrompt: e.target.value })} style={{ marginTop: 8 }} />
            <textarea className="input" rows={4} placeholder="userPrompt" value={editing.userPrompt || ''} onChange={(e) => setEditing({ ...editing, userPrompt: e.target.value })} style={{ marginTop: 8 }} />
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <button type="button" className="button" onClick={saveNew}>등록</button>
              {editing.id ? <button type="button" className="button button--ghost" onClick={() => newVersion(editing.id!)}>새 버전</button> : null}
            </div>
          </div>
        ) : null}
        <table className="table">
          <thead>
            <tr>
              <th>코드</th>
              <th>이름</th>
              <th>v</th>
              <th>활성</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {list.map((p) => (
              <tr key={p.id}>
                <td>{p.promptCode}</td>
                <td>{p.promptName}</td>
                <td>{p.version}</td>
                <td>{p.isActive ? 'ON' : 'OFF'}</td>
                <td style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  <button type="button" className="button button--ghost" onClick={() => setEditing(p)}>편집</button>
                  {!p.isActive ? <button type="button" className="button" onClick={() => activate(p.id)}>활성</button> : null}
                  <button type="button" className="button button--ghost" onClick={() => runTest(p.id)}>테스트</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {testOut ? (
          <div className="card" style={{ marginTop: 16 }}>
            <h3>테스트 결과 (log #{testOut.testLogId})</h3>
            <p>impactScore: {testOut.impactScore} · success: {String(testOut.success)}</p>
            <pre style={{ whiteSpace: 'pre-wrap', fontSize: 12 }}>{testOut.rawResponse || testOut.errorMessage}</pre>
          </div>
        ) : null}
        <p style={{ marginTop: 12 }}>
          <Link href="/settings/llm-prompts">레거시 LLM 템플릿 화면</Link>
        </p>
      </section>
    </AppShell>
  )
}
