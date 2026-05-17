'use client'

import { useEffect, useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'

function readCookie(name: string): string {
  const m = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return decodeURIComponent(m?.[1] || '')
}

function clearCookie(name: string) {
  document.cookie = `${name}=; path=/; max-age=0; samesite=lax`
}

export function UserMenu() {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const [userName, setUserName] = useState('')
  const [userId, setUserId] = useState('')
  const boxRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setUserName(readCookie('user-name'))
    setUserId(readCookie('user-id'))
  }, [])

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!boxRef.current) return
      if (!boxRef.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('click', onDocClick)
    return () => document.removeEventListener('click', onDocClick)
  }, [])

  function logout() {
    clearCookie('user-id')
    clearCookie('user-name')
    setOpen(false)
    router.push('/auth')
    router.refresh()
  }

  if (!userId) {
    return (
      <div className="user-menu-wrap">
        <Link href="/auth" className="button">로그인</Link>
      </div>
    )
  }

  const nick = userName || '회원'
  return (
    <div className="user-menu-wrap" ref={boxRef}>
      <button type="button" className="user-menu-button" onClick={() => setOpen((v) => !v)}>
        {nick}님, 환영합니다
      </button>
      {open ? (
        <div className="user-menu-dropdown">
          <Link href="/settings" className="user-menu-item" onClick={() => setOpen(false)}>
            내정보
          </Link>
          <Link href="/auth" className="user-menu-item" onClick={() => setOpen(false)}>
            계정 관리
          </Link>
          <button type="button" className="user-menu-item user-menu-item-danger" onClick={logout}>
            로그아웃
          </button>
        </div>
      ) : null}
    </div>
  )
}
