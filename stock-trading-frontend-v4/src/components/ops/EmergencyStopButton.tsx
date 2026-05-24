'use client'

import { useEffect, useState } from 'react'
import { apiGet, apiPost } from '@/lib/api'
import { notifyError, notifySuccess } from '@/lib/toast'

export function EmergencyStopButton() {
  const [on, setOn] = useState(false)

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
      notifySuccess(next ? '긴급 정지 ON — 자동매매·주문 차단' : '긴급 정지 해제')
    } catch (error) {
      notifyError(error)
    }
  }

  return (
    <div style={{ marginTop: 12 }}>
      <button type="button" className={`button${on ? '' : ' button--danger'}`} onClick={toggle}>
        {on ? '긴급 정지 해제' : '긴급 자동매매 중지'}
      </button>
    </div>
  )
}
