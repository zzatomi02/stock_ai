'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { AppShell } from '@/components/layout/AppShell'
import { apiGet, apiPost } from '@/lib/api'
import { notifyError, notifyInfo, notifySuccess, notifyWarning } from '@/lib/toast'

type AuthUser = {
  userId: string
  username: string
  displayName: string
  email?: string
}

type ResetStep = 'idle' | 'code-sent'

function writeCookie(name: string, value: string) {
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=31536000; samesite=lax`
}

function clearCookie(name: string) {
  document.cookie = `${name}=; path=/; max-age=0; samesite=lax`
}

const inputStyle = {
  padding: '10px 12px',
  borderRadius: 8,
  border: '1px solid #D0D5DD',
} as const

export default function AuthPage() {
  const router = useRouter()
  const [loading, setLoading] = useState(false)
  const [signup, setSignup] = useState({ username: '', password: '', email: '', displayName: '' })
  const [usernameCheck, setUsernameCheck] = useState<{
    available: boolean
    message: string
    verifiedUsername: string
  } | null>(null)
  const [usernameChecking, setUsernameChecking] = useState(false)
  const [login, setLogin] = useState({ username: '', password: '' })
  const [me, setMe] = useState<AuthUser | null>(null)

  const [resetStep, setResetStep] = useState<ResetStep>('idle')
  const [reset, setReset] = useState({ email: '', code: '', newPassword: '', confirmPassword: '' })
  const [findUsername, setFindUsername] = useState({ email: '' })

  function onSignupUsernameChange(username: string) {
    setSignup((p) => ({ ...p, username }))
    setUsernameCheck(null)
  }

  async function checkUsernameDuplicate() {
    const raw = signup.username.trim()
    if (raw.length < 3) {
      setUsernameCheck({
        available: false,
        message: '아이디는 3자 이상 입력하세요.',
        verifiedUsername: raw,
      })
      return
    }
    setUsernameChecking(true)
    try {
      const res = await apiGet<{ available: boolean; username: string }>(
        `/auth/check-username?username=${encodeURIComponent(raw)}`,
      )
      setUsernameCheck({
        available: res.available,
        message: res.available ? '사용 가능한 아이디입니다.' : '이미 사용 중인 아이디입니다.',
        verifiedUsername: res.username,
      })
    } catch (error) {
      setUsernameCheck({
        available: false,
        message: error instanceof Error ? error.message : String(error),
        verifiedUsername: raw,
      })
    } finally {
      setUsernameChecking(false)
    }
  }

  const usernameVerified =
    usernameCheck != null &&
    usernameCheck.verifiedUsername === signup.username.trim() &&
    usernameCheck.available

  async function doSignup() {
    if (!usernameVerified) {
      notifyWarning('아이디 중복 확인을 먼저 해 주세요. (사용 가능해야 회원가입할 수 있습니다)')
      return
    }
    setLoading(true)
    try {
      const user = await apiPost<AuthUser>('/auth/signup', signup)
      writeCookie('user-id', user.userId)
      writeCookie('user-name', user.displayName || user.username)
      notifySuccess(`회원가입 및 로그인 완료: ${user.username}`)
      router.push('/settings')
      router.refresh()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function doLogin() {
    setLoading(true)
    try {
      const user = await apiPost<AuthUser>('/auth/login', login)
      writeCookie('user-id', user.userId)
      writeCookie('user-name', user.displayName || user.username)
      notifySuccess(`로그인 완료: ${user.username}`)
      router.push('/settings')
      router.refresh()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function requestFindUsername() {
    setLoading(true)
    try {
      const res = await apiPost<{ sent: boolean; message: string; devMailConfigured?: boolean }>(
        '/auth/find-username',
        { email: findUsername.email },
      )
      const devHint =
        res.devMailConfigured === false
          ? ' (메일 미설정: 백엔드 로그 [FIND-USERNAME] 확인)'
          : ''
      notifySuccess((res.message || '아이디 안내 메일을 발송했습니다.') + devHint)
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function requestResetCode() {
    setLoading(true)
    try {
      const res = await apiPost<{ sent: boolean; message: string; devMailConfigured?: boolean }>(
        '/auth/password-reset/request',
        { email: reset.email },
      )
      setResetStep('code-sent')
      const devHint =
        res.devMailConfigured === false
          ? ' (메일 미설정: 백엔드 로그에서 [PASSWORD-RESET] 인증번호를 확인하세요.)'
          : ''
      notifySuccess((res.message || '인증번호를 발송했습니다.') + devHint)
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function confirmReset() {
    if (reset.newPassword !== reset.confirmPassword) {
      notifyWarning('새 비밀번호가 일치하지 않습니다.')
      return
    }
    setLoading(true)
    try {
      await apiPost('/auth/password-reset/confirm', {
        email: reset.email,
        code: reset.code,
        newPassword: reset.newPassword,
      })
      setResetStep('idle')
      setReset({ email: reset.email, code: '', newPassword: '', confirmPassword: '' })
      notifySuccess('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.')
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  async function loadMe() {
    setLoading(true)
    try {
      const user = await apiGet<AuthUser>('/auth/me')
      setMe(user)
      notifyInfo(`현재 로그인 사용자: ${user.username}`)
    } catch (error) {
      setMe(null)
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  function logout() {
    clearCookie('user-id')
    clearCookie('user-name')
    setMe(null)
    notifyInfo('로그아웃되었습니다.')
    router.refresh()
  }

  return (
    <AppShell>
      <section className="card">
        <h1>로그인 / 회원가입</h1>
        <p style={{ marginTop: 8, color: '#667085' }}>
          회원가입 시 이메일을 등록하면 아이디·비밀번호 찾기를 사용할 수 있습니다.
        </p>

        <div className="grid-2" style={{ marginTop: 12 }}>
          <div className="card">
            <h3>회원가입</h3>
            <div style={{ display: 'grid', gap: 8 }}>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'stretch' }}>
                <input
                  value={signup.username}
                  onChange={(e) => onSignupUsernameChange(e.target.value)}
                  placeholder="아이디 (3~40자)"
                  style={{ ...inputStyle, flex: '1 1 140px', minWidth: 0 }}
                />
                <button
                  type="button"
                  className="button"
                  disabled={loading || usernameChecking || signup.username.trim().length < 3}
                  onClick={checkUsernameDuplicate}
                  style={{ flex: '0 0 auto', whiteSpace: 'nowrap' }}
                >
                  {usernameChecking ? '확인 중…' : '중복 확인'}
                </button>
              </div>
              {usernameCheck && usernameCheck.verifiedUsername === signup.username.trim() ? (
                <p
                  style={{
                    margin: 0,
                    fontSize: 13,
                    color: usernameCheck.available ? '#027A48' : '#B42318',
                  }}
                >
                  {usernameCheck.message}
                </p>
              ) : signup.username.trim().length > 0 && signup.username.trim().length < 3 ? (
                <p style={{ margin: 0, fontSize: 13, color: '#B54708' }}>아이디는 3자 이상입니다.</p>
              ) : null}
              <input
                value={signup.email}
                type="email"
                onChange={(e) => setSignup((p) => ({ ...p, email: e.target.value }))}
                placeholder="이메일 (비밀번호 찾기용)"
                style={inputStyle}
              />
              <input
                value={signup.password}
                type="password"
                onChange={(e) => setSignup((p) => ({ ...p, password: e.target.value }))}
                placeholder="비밀번호 (6자 이상)"
                style={inputStyle}
              />
              <input
                value={signup.displayName}
                onChange={(e) => setSignup((p) => ({ ...p, displayName: e.target.value }))}
                placeholder="표시 이름 (선택)"
                style={inputStyle}
              />
              <button className="button" disabled={loading || !usernameVerified} onClick={doSignup}>
                회원가입
              </button>
            </div>
          </div>

          <div className="card">
            <h3>로그인</h3>
            <div style={{ display: 'grid', gap: 8 }}>
              <input
                value={login.username}
                onChange={(e) => setLogin((p) => ({ ...p, username: e.target.value }))}
                placeholder="아이디"
                style={inputStyle}
              />
              <input
                value={login.password}
                type="password"
                onChange={(e) => setLogin((p) => ({ ...p, password: e.target.value }))}
                placeholder="비밀번호"
                style={inputStyle}
              />
              <button className="button" disabled={loading} onClick={doLogin}>
                로그인
              </button>
            </div>
          </div>
        </div>

        <div className="grid-2" style={{ marginTop: 12 }}>
          <div className="card">
            <h3>아이디 찾기</h3>
            <p style={{ marginTop: 4, color: '#667085', fontSize: 14 }}>
              가입 시 등록한 이메일로 아이디를 보내 드립니다.
            </p>
            <div style={{ display: 'grid', gap: 8, marginTop: 8 }}>
              <input
                value={findUsername.email}
                type="email"
                onChange={(e) => setFindUsername({ email: e.target.value })}
                placeholder="가입 이메일"
                style={inputStyle}
              />
              <button
                className="button"
                disabled={loading || !findUsername.email.trim()}
                onClick={requestFindUsername}
              >
                아이디 메일 받기
              </button>
            </div>
          </div>

          <div className="card">
            <h3>비밀번호 찾기</h3>
            <p style={{ marginTop: 4, color: '#667085', fontSize: 14 }}>
              가입 시 등록한 이메일로 6자리 인증번호를 보냅니다. 10분 이내 입력해 주세요.
            </p>
            <div style={{ display: 'grid', gap: 8, marginTop: 8 }}>
            <input
              value={reset.email}
              type="email"
              onChange={(e) => setReset((p) => ({ ...p, email: e.target.value }))}
              placeholder="가입 이메일"
              style={inputStyle}
              disabled={resetStep === 'code-sent'}
            />
            {resetStep === 'code-sent' ? (
              <>
                <input
                  value={reset.code}
                  onChange={(e) =>
                    setReset((p) => ({ ...p, code: e.target.value.replace(/\D/g, '').slice(0, 6) }))
                  }
                  placeholder="인증번호 6자리"
                  inputMode="numeric"
                  maxLength={6}
                  style={inputStyle}
                />
                <input
                  value={reset.newPassword}
                  type="password"
                  onChange={(e) => setReset((p) => ({ ...p, newPassword: e.target.value }))}
                  placeholder="새 비밀번호 (6자 이상)"
                  style={inputStyle}
                />
                <input
                  value={reset.confirmPassword}
                  type="password"
                  onChange={(e) => setReset((p) => ({ ...p, confirmPassword: e.target.value }))}
                  placeholder="새 비밀번호 확인"
                  style={inputStyle}
                />
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <button className="button" disabled={loading} onClick={confirmReset}>
                    비밀번호 변경
                  </button>
                  <button
                    className="button"
                    type="button"
                    disabled={loading}
                    onClick={() => {
                      setResetStep('idle')
                      setReset((p) => ({ ...p, code: '', newPassword: '', confirmPassword: '' }))
                    }}
                  >
                    처음부터
                  </button>
                </div>
              </>
            ) : (
              <button className="button" disabled={loading || !reset.email} onClick={requestResetCode}>
                인증번호 받기
              </button>
            )}
            {resetStep === 'code-sent' ? (
              <button
                type="button"
                className="button"
                style={{ background: 'transparent', color: '#475467', border: '1px solid #D0D5DD' }}
                disabled={loading}
                onClick={requestResetCode}
              >
                인증번호 다시 받기
              </button>
            ) : null}
            </div>
          </div>
        </div>

        <div className="card" style={{ marginTop: 12 }}>
          <h3>현재 세션 확인</h3>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button className="button" disabled={loading} onClick={loadMe}>
              내 정보 조회
            </button>
            <button className="button" disabled={loading} onClick={logout}>
              로그아웃
            </button>
          </div>
          {me ? (
            <p style={{ marginTop: 8, color: '#475467' }}>
              userId={me.userId}, username={me.username}, email={me.email || '(없음)'}
            </p>
          ) : null}
        </div>
      </section>
    </AppShell>
  )
}
