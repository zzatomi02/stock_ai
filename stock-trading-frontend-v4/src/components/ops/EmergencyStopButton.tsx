'use client'

import { useEffect, useState } from 'react'
import { apiGet, apiPost } from '@/lib/api'

export function EmergencyStopButton() {
  const [on, setOn] = useState(false)
  const [msg, setMsg] = useState('')

  useEffect(() => {
    refresh()
  }, [])

  async function refresh() {
    try {
      const v = await apiGet<boolean>('/ops/emergency-stop')
      setOn(!!v)
    } catch {
      /* ignore */
    }
  }

  async function toggle() {
    const next = !on
    try {
      await apiPost(`/ops/emergency-stop?enabled=${next}`, {})
      setOn(next)
      setMsg(next ? '긴급 정지 ON — 자동매매·주문 차단' : '긴급 정지 해제')
    } catch (e) {
      setMsg(e instanceof Error ? e.message : String(e))
    }
  }

  return (
    <div style={{ marginTop: 12 }}>
      <button type="button" className={`button${on ? '' : ' button--danger'}`} onClick={toggle}>
        {on ? '긴급 정지 해제' : '긴급 자동매매 중지'}
      </button>
      {msg ? <p style={{ marginTop: 8, fontSize: 13, color: '#9fb2d9' }}>{msg}</p> : null}
    </div>
  )
}
