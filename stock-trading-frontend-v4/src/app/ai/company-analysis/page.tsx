'use client'

import { useCallback, useState } from 'react'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet, apiPost } from '@/lib/api'
import { notifyError, notifyInfo, notifySuccess } from '@/lib/toast'

type Bundle = {
  stockCode: string
  tradeDate: string
  source: string
  liveApiCalled: boolean
  bestAnalysis?: Record<string, unknown>
  consensus?: Record<string, unknown>
  providerResults?: Record<string, unknown>[]
}

export default function AiCompanyAnalysisPage() {
  const [stockCode, setStockCode] = useState('005930')
  const [bundle, setBundle] = useState<Bundle | null>(null)
  const [loading, setLoading] = useState(false)

  const loadCached = useCallback(async () => {
    setLoading(true)
    try {
      const data = await apiGet<Bundle>(`/ai/cached/company/${stockCode}/bundle`)
      setBundle(data)
      notifyInfo('DB 캐시 조회 완료 (장중 안전)')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }, [stockCode])

  const enqueueBatch = async () => {
    try {
      await apiPost(`/ai/platform/jobs/company-analysis?stockCode=${stockCode}&timing=MANUAL`, {})
      notifySuccess('분석 작업 큐 등록됨 — 장외/수동 실행 시 API 호출')
    } catch (error) {
      notifyError(error)
    }
  }

  return (
    <AppShell>
      <div className="space-y-6 p-6">
        <h1 className="text-2xl font-semibold text-white">AI 기업 분석 (캐시)</h1>
        <p className="text-sm text-slate-400">
          장중 매매 판단은 저장된 결과만 조회합니다. 새 AI 분석은 장전·장마감·수동(장외)에 실행하세요.
        </p>
        <div className="flex flex-wrap items-end gap-3">
          <label className="text-sm text-slate-300">
            종목코드
            <input
              className="ml-2 rounded border border-slate-600 bg-slate-900 px-3 py-2 text-white"
              value={stockCode}
              onChange={(e) => setStockCode(e.target.value)}
            />
          </label>
          <button
            type="button"
            onClick={loadCached}
            disabled={loading}
            className="rounded bg-emerald-600 px-4 py-2 text-sm text-white hover:bg-emerald-500 disabled:opacity-50"
          >
            캐시 조회
          </button>
          <button
            type="button"
            onClick={enqueueBatch}
            className="rounded bg-indigo-600 px-4 py-2 text-sm text-white hover:bg-indigo-500"
          >
            분석 작업 등록
          </button>
        </div>
        {bundle && (
          <pre className="overflow-auto rounded-lg border border-slate-700 bg-slate-950 p-4 text-xs text-slate-200">
            {JSON.stringify(bundle, null, 2)}
          </pre>
        )}
      </div>
    </AppShell>
  )
}
