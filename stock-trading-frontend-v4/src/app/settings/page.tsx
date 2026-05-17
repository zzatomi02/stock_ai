'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { Eye, EyeOff } from 'lucide-react'
import { AppShell } from '@/components/layout/AppShell'
import { EmergencyStopButton } from '@/components/ops/EmergencyStopButton'
import { apiGet, apiPost } from '@/lib/api'

type Mode = 'paper' | 'real'

type ModeForm = {
  appKey: string
  accountNo: string
  productCode: string
  hasAppSecret: boolean
} | null

type LinkStatus = {
  userId: string
  paper: 'linked' | 'unlinked'
  real: 'linked' | 'unlinked'
  paperForm: ModeForm
  realForm: ModeForm
}

function readCookie(name: string): string {
  const m = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return decodeURIComponent(m?.[1] || '')
}

function emptyFields() {
  return { appKey: '', appSecret: '', accountNo: '', productCode: '01' }
}

export default function SettingsPage() {
  const [userId, setUserId] = useState('')
  const [status, setStatus] = useState<LinkStatus | null>(null)
  const [msg, setMsg] = useState('')
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState<Record<Mode, ReturnType<typeof emptyFields>>>({
    paper: emptyFields(),
    real: emptyFields(),
  })
  const [appSecretVisible, setAppSecretVisible] = useState<Record<Mode, boolean>>({
    paper: false,
    real: false,
  })

  function applyFormFromStatus(s: LinkStatus) {
    setForm({
      paper: s.paperForm
        ? {
            appKey: s.paperForm.appKey,
            appSecret: '',
            accountNo: s.paperForm.accountNo,
            productCode: s.paperForm.productCode || '01',
          }
        : emptyFields(),
      real: s.realForm
        ? {
            appKey: s.realForm.appKey,
            appSecret: '',
            accountNo: s.realForm.accountNo,
            productCode: s.realForm.productCode || '01',
          }
        : emptyFields(),
    })
  }

  async function loadStatus() {
    if (!readCookie('user-id')) {
      setStatus(null)
      return
    }
    try {
      const s = await apiGet<LinkStatus>('/broker/kis/credentials/status')
      setStatus(s)
      applyFormFromStatus(s)
    } catch {
      setStatus(null)
    }
  }

  useEffect(() => {
    const id = readCookie('user-id')
    setUserId(id)
    void loadStatus()
  }, [])

  async function saveCredential(mode: Mode) {
    const v = form[mode]
    if (!userId.trim()) {
      setMsg('먼저 로그인하여 사용자 쿠키(user-id)가 있어야 합니다.')
      return
    }
    if (!v.appKey.trim()) {
      setMsg(`${mode} appKey 를 입력하세요.`)
      return
    }
    const f = status?.[mode === 'paper' ? 'paperForm' : 'realForm']
    const canSkipSecret = f?.hasAppSecret && !v.appSecret.trim()
    if (!canSkipSecret && !v.appSecret.trim()) {
      setMsg(`${mode} appSecret 을 입력하세요. (이미 저장된 경우 비우면 이전 시크릿이 유지됩니다.)`)
      return
    }
    setLoading(true)
    setMsg('')
    try {
      await apiPost('/broker/kis/credentials', {
        mode,
        appKey: v.appKey.trim(),
        appSecret: v.appSecret.trim(),
        accountNo: v.accountNo.trim(),
        productCode: v.productCode.trim() || '01',
      })
      setMsg(`${mode} 연동 정보 저장 완료`)
      setForm((prev) => ({
        ...prev,
        [mode]: { ...prev[mode], appSecret: '' },
      }))
      await loadStatus()
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  return (
    <AppShell>
      <section className="card">
        <h1>설정</h1>
        <p style={{ marginTop: 8, marginBottom: 12, fontSize: 14 }}>
          <Link href="/settings/llm-prompts" style={{ color: '#93c5fd' }}>
            LLM 뉴스 질문 템플릿 (ChatGPT)
          </Link>
          <span style={{ color: '#667085' }}> — system/user 프롬프트 편집·미리보기</span>
        </p>
        <p style={{ marginTop: 8, color: '#475467' }}>
          트레이딩 기능을 처음 사용할 때 사용자별 KIS 연동 정보를 등록하세요. 저장 후 새로고침해도 appKey·계좌·상품코드는 다시
          불러옵니다. appSecret 은 보안을 위해 다시 보여주지 않으며, 변경할 때만 입력하세요.
        </p>
        <div className="card" style={{ marginTop: 12 }}>
          <h3>로그인 사용자</h3>
          <p style={{ color: '#667085' }}>
            {userId ? `현재 사용자 ID: ${userId}` : '로그인이 필요합니다. 먼저 로그인/회원가입을 진행하세요.'}
          </p>
          {!userId ? <Link href="/auth" className="button">로그인 / 회원가입</Link> : null}
          {status ? (
            <p style={{ marginTop: 8, color: '#667085' }}>
              연동 상태 - paper: <b>{status.paper}</b>, real: <b>{status.real}</b>
            </p>
          ) : null}
        </div>

        <div className="grid-2" style={{ marginTop: 12 }}>
          {(['paper', 'real'] as const).map((mode) => {
            const f = status?.[mode === 'paper' ? 'paperForm' : 'realForm']
            return (
              <div className="card" key={mode}>
                <h3>{mode === 'paper' ? '모의투자' : '실전투자'} 연동</h3>
                <div style={{ display: 'grid', gap: 8, marginTop: 8 }}>
                  <input
                    value={form[mode].appKey}
                    onChange={(e) => setForm((p) => ({ ...p, [mode]: { ...p[mode], appKey: e.target.value } }))}
                    placeholder="appKey"
                    style={{ padding: '10px 12px', borderRadius: 8, border: '1px solid #D0D5DD' }}
                  />
                  <div style={{ position: 'relative' }}>
                    <input
                      value={form[mode].appSecret}
                      onChange={(e) => setForm((p) => ({ ...p, [mode]: { ...p[mode], appSecret: e.target.value } }))}
                      placeholder={f?.hasAppSecret ? 'appSecret (비워두면 기존 값 유지)' : 'appSecret'}
                      type={appSecretVisible[mode] ? 'text' : 'password'}
                      autoComplete="off"
                      style={{
                        width: '100%',
                        boxSizing: 'border-box',
                        padding: '10px 44px 10px 12px',
                        borderRadius: 8,
                        border: '1px solid #D0D5DD',
                      }}
                    />
                    <button
                      type="button"
                      aria-label={appSecretVisible[mode] ? 'appSecret 가리기' : 'appSecret 보기'}
                      aria-pressed={appSecretVisible[mode]}
                      onClick={() =>
                        setAppSecretVisible((p) => ({
                          ...p,
                          [mode]: !p[mode],
                        }))
                      }
                      style={{
                        position: 'absolute',
                        right: 6,
                        top: '50%',
                        transform: 'translateY(-50%)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        width: 36,
                        height: 36,
                        padding: 0,
                        border: 'none',
                        borderRadius: 6,
                        background: 'transparent',
                        color: '#667085',
                        cursor: 'pointer',
                      }}
                    >
                      {appSecretVisible[mode] ? <EyeOff size={20} strokeWidth={1.75} /> : <Eye size={20} strokeWidth={1.75} />}
                    </button>
                  </div>
                  <input
                    value={form[mode].accountNo}
                    onChange={(e) => setForm((p) => ({ ...p, [mode]: { ...p[mode], accountNo: e.target.value } }))}
                    placeholder="계좌번호 8자리"
                    style={{ padding: '10px 12px', borderRadius: 8, border: '1px solid #D0D5DD' }}
                  />
                  <input
                    value={form[mode].productCode}
                    onChange={(e) => setForm((p) => ({ ...p, [mode]: { ...p[mode], productCode: e.target.value } }))}
                    placeholder="상품코드 (기본 01)"
                    style={{ padding: '10px 12px', borderRadius: 8, border: '1px solid #D0D5DD' }}
                  />
                  <button className="button" disabled={loading} onClick={() => saveCredential(mode)}>
                    {loading ? '저장 중...' : '연동 정보 저장'}
                  </button>
                </div>
              </div>
            )
          })}
        </div>
        {msg ? <p style={{ marginTop: 12, color: '#475467' }}>{msg}</p> : null}
        <hr style={{ margin: '24px 0', border: 'none', borderTop: '1px solid #D0D5DD' }} />
        <h2 style={{ fontSize: 16 }}>긴급 정지</h2>
        <p style={{ fontSize: 13, color: '#667085' }}>활성화 시 KIS 주문·자동매매가 차단됩니다.</p>
        <EmergencyStopButton />
      </section>
    </AppShell>
  )
}
