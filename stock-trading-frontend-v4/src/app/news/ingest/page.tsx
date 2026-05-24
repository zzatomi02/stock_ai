'use client'

import Link from 'next/link'
import { useEffect, useMemo, useState } from 'react'
import { AppShell } from '@/components/layout/AppShell'
import { apiDelete, apiGet, apiPost, apiPut } from '@/lib/api'
import { notifyError, notifySuccess } from '@/lib/toast'

type NewsSource = {
  id: number
  name: string
  baseUrl: string
  parserKey: string
  enabled: boolean
  fetchIntervalSec: number
  maxArticlesPerRun: number
  requestHeadersJson: string
  selectorConfigJson: string
}

type NewsSourcePayload = {
  name: string
  baseUrl: string
  parserKey: string
  enabled: boolean
  fetchIntervalSec: number
  maxArticlesPerRun: number
  requestHeadersJson: string
  selectorConfigJson: string
}

const PARSER_OPTIONS = [
  { key: 'NAVER_BREAKING_SECTION', label: 'NAVER_BREAKING_SECTION (네이버 섹션)' },
  { key: 'DAUM_ECONOMY_HOME', label: 'DAUM_ECONOMY_HOME (다음 경제)' },
] as const

export default function NewsIngestPage() {
  const [sources, setSources] = useState<NewsSource[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [form, setForm] = useState<NewsSourcePayload>({
    name: '',
    baseUrl: '',
    parserKey: 'NAVER_BREAKING_SECTION',
    enabled: true,
    fetchIntervalSec: 300,
    maxArticlesPerRun: 30,
    requestHeadersJson: '',
    selectorConfigJson: '',
  })
  const [loading, setLoading] = useState(false)
  const [running, setRunning] = useState(false)

  const selected = useMemo(
    () => sources.find((s) => s.id === selectedId) ?? null,
    [sources, selectedId]
  )

  async function loadSources() {
    const list = await apiGet<NewsSource[]>('/news/sources')
    setSources(list)
  }

  useEffect(() => {
    void (async () => {
      try {
        await loadSources()
      } catch (error) {
        notifyError(error)
      }
    })()
  }, [])

  async function createSource() {
    setLoading(true)
    try {
      const created = await apiPost<NewsSource>('/news/sources', form)
      await loadSources()
      setSelectedId(created.id)
      notifySuccess('뉴스 소스가 추가되었습니다.')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function updateSource() {
    if (!selected) return
    setLoading(true)
    try {
      await apiPut<NewsSource>(`/news/sources/${selected.id}`, form)
      await loadSources()
      notifySuccess('뉴스 소스를 수정했습니다.')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function deleteSource() {
    if (!selected) return
    if (!confirm(`소스 "${selected.name}"를 삭제할까요?`)) return
    setLoading(true)
    try {
      await apiDelete(`/news/sources/${selected.id}`)
      await loadSources()
      setSelectedId(null)
      setForm({
        name: '',
        baseUrl: '',
        parserKey: 'NAVER_BREAKING_SECTION',
        enabled: true,
        fetchIntervalSec: 300,
        maxArticlesPerRun: 30,
        requestHeadersJson: '',
        selectorConfigJson: '',
      })
      notifySuccess('뉴스 소스를 삭제했습니다.')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function runOne() {
    if (!selected) return
    setRunning(true)
    try {
      const out = await apiPost<{ sourceId: number; saved: number }>(`/news/sources/${selected.id}/run`, {})
      notifySuccess(`수집 실행 완료: 신규 저장 ${out.saved}건`)
    } catch (error) {
      notifyError(error)
    } finally {
      setRunning(false)
    }
  }

  async function runAll() {
    setRunning(true)
    try {
      const out = await apiPost<{ saved: number }>(`/news/sources/run-all`, {})
      notifySuccess(`전체 소스 수집 실행 완료: 신규 저장 ${out.saved}건`)
    } catch (error) {
      notifyError(error)
    } finally {
      setRunning(false)
    }
  }

  function selectSource(source: NewsSource) {
    setSelectedId(source.id)
    setForm({
      name: source.name,
      baseUrl: source.baseUrl,
      parserKey: source.parserKey,
      enabled: source.enabled,
      fetchIntervalSec: source.fetchIntervalSec,
      maxArticlesPerRun: source.maxArticlesPerRun,
      requestHeadersJson: source.requestHeadersJson ?? '',
      selectorConfigJson: source.selectorConfigJson ?? '',
    })
  }

  return (
    <AppShell>
      <section className="card">
        <h1 style={{ marginTop: 0 }}>뉴스 소스 URL 관리</h1>
        <p style={{ color: '#8b9dc7', fontSize: 14, lineHeight: 1.6 }}>
          원하는 URL을 소스로 등록(CRUD)하고, 소스별/전체 수집을 실행할 수 있습니다. 파서는 소스 구조에 맞게{' '}
          <code>parserKey</code>를 선택하세요.
        </p>
        <p style={{ marginTop: 8, fontSize: 13 }}>
          <Link href="/news" style={{ color: '#93c5fd' }}>
            뉴스·키워드 점수(CRUD)
          </Link>
          {' · '}
          <Link href="/settings/llm-prompts" style={{ color: '#93c5fd' }}>
            LLM 질문 템플릿
          </Link>
          {' · '}
          <span style={{ color: '#667085' }}>종목 예시: </span>
          <Link href="/market/005930" style={{ color: '#93c5fd' }}>
            종목 상세
          </Link>
        </p>

        <div style={{ marginTop: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          <button type="button" className="button" disabled={running} onClick={() => void runAll()}>
            {running ? '실행 중…' : '전체 소스 수집 실행'}
          </button>
          <button type="button" className="button" disabled={running || !selected} onClick={() => void runOne()}>
            {running ? '실행 중…' : '선택 소스 수집 실행'}
          </button>
          <button type="button" className="button ghost" disabled={loading} onClick={() => void loadSources()}>
            목록 새로고침
          </button>
        </div>

        <div style={{ display: 'grid', gap: 12, marginTop: 16 }}>
          <div style={{ display: 'grid', gap: 8 }}>
            <div style={{ fontSize: 12, color: '#8b9dc7' }}>등록된 소스</div>
            <div style={{ display: 'grid', gap: 8 }}>
              {sources.map((s) => (
                <button
                  key={s.id}
                  type="button"
                  className="button ghost"
                  onClick={() => selectSource(s)}
                  style={{
                    textAlign: 'left',
                    borderColor: selectedId === s.id ? '#3b82f6' : undefined,
                  }}
                >
                  [{s.enabled ? 'ON' : 'OFF'}] {s.name} · {s.parserKey}
                </button>
              ))}
              {sources.length === 0 ? <div style={{ color: '#667085', fontSize: 13 }}>등록된 소스가 없습니다.</div> : null}
            </div>
          </div>

          <label>
            <div style={{ fontSize: 12, color: '#8b9dc7' }}>소스 이름</div>
            <input className="input" value={form.name} onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))} />
          </label>
          <label>
            <div style={{ fontSize: 12, color: '#8b9dc7' }}>목록 URL (baseUrl)</div>
            <input className="input" value={form.baseUrl} onChange={(e) => setForm((f) => ({ ...f, baseUrl: e.target.value }))} />
          </label>
          <label>
            <div style={{ fontSize: 12, color: '#8b9dc7' }}>파서 키 (parserKey)</div>
            <select
              className="input"
              value={form.parserKey}
              onChange={(e) => setForm((f) => ({ ...f, parserKey: e.target.value }))}
            >
              {PARSER_OPTIONS.map((p) => (
                <option key={p.key} value={p.key}>
                  {p.label}
                </option>
              ))}
            </select>
          </label>
          <div style={{ display: 'grid', gap: 8, gridTemplateColumns: '1fr 1fr 1fr' }}>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>갱신 간격(초)</div>
              <input
                type="number"
                className="input"
                min={10}
                max={86400}
                value={form.fetchIntervalSec}
                onChange={(e) => setForm((f) => ({ ...f, fetchIntervalSec: Number(e.target.value) || 300 }))}
              />
            </label>
            <label>
              <div style={{ fontSize: 12, color: '#8b9dc7' }}>회당 최대 기사수</div>
              <input
                type="number"
                className="input"
                min={1}
                max={300}
                value={form.maxArticlesPerRun}
                onChange={(e) => setForm((f) => ({ ...f, maxArticlesPerRun: Number(e.target.value) || 30 }))}
              />
            </label>
            <label style={{ display: 'flex', alignItems: 'end' }}>
              <span style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                <input
                  type="checkbox"
                  checked={form.enabled}
                  onChange={(e) => setForm((f) => ({ ...f, enabled: e.target.checked }))}
                />
                enabled
              </span>
            </label>
          </div>

          <label>
            <div style={{ fontSize: 12, color: '#8b9dc7' }}>요청 헤더 JSON (선택)</div>
            <textarea
              className="input"
              rows={3}
              value={form.requestHeadersJson}
              onChange={(e) => setForm((f) => ({ ...f, requestHeadersJson: e.target.value }))}
            />
          </label>
          <label>
            <div style={{ fontSize: 12, color: '#8b9dc7' }}>선택자 설정 JSON (선택)</div>
            <textarea
              className="input"
              rows={3}
              value={form.selectorConfigJson}
              onChange={(e) => setForm((f) => ({ ...f, selectorConfigJson: e.target.value }))}
            />
          </label>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button type="button" className="button" disabled={loading} onClick={() => void createSource()}>
              {loading ? '처리 중…' : '새 소스 추가'}
            </button>
            <button type="button" className="button" disabled={loading || !selected} onClick={() => void updateSource()}>
              {loading ? '처리 중…' : '선택 소스 수정'}
            </button>
            <button type="button" className="button ghost" disabled={loading || !selected} onClick={() => void deleteSource()}>
              선택 소스 삭제
            </button>
          </div>
        </div>

      </section>
    </AppShell>
  )
}
