'use client'

import { useCallback, useEffect, useState } from 'react'
import { apiGet, apiPatch } from '@/lib/api'
import { notifyError, notifySuccess } from '@/lib/toast'

type RecommendationRules = {
  minGrade: string
  minAdjustedScore: number
  maxPerDay: number
  dedupeByStock: boolean
  notifyOnNew: boolean
}

type NotificationChannels = {
  telegram: boolean
  discord: boolean
  email: boolean
  kakao: boolean
  kakaoWebhookUrl: string
  sms: boolean
  smsWebhookUrl: string
  emailTo: string
}

type SettingsResponse = {
  phase: string
  phaseLabel: string
  orderQty: number
  autoOrderScanEnabled: boolean
  autoOrderScanIntervalMs: number
  marketHoursOnly: boolean
  realTradingEnabled: boolean
  recommendation: RecommendationRules
  notification: NotificationChannels
}

const PHASE_OPTIONS = [
  { value: 'OBSERVE', label: '관찰 — 분석·종목 추천만' },
  { value: 'PAPER_ALERT', label: '모의 — 추천 알림 + 승인 후 주문' },
  { value: 'PAPER_AUTO', label: '모의 — 자동매매' },
  { value: 'REAL_ALERT', label: '실전 — 추천 알림 + 승인 후 주문' },
  { value: 'REAL_AUTO', label: '실전 — 자동매매' },
] as const

export function ExecutionModeSettings() {
  const [settings, setSettings] = useState<SettingsResponse | null>(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    try {
      const s = await apiGet<SettingsResponse>('/recommendations/settings')
      setSettings(s)
    } catch (error) {
      notifyError(error)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  async function save() {
    if (!settings) return
    setLoading(true)
    try {
      await apiPatch('/recommendations/settings', {
        phase: settings.phase,
        orderQty: settings.orderQty,
        autoOrderScanEnabled: settings.autoOrderScanEnabled,
        autoOrderScanIntervalMs: settings.autoOrderScanIntervalMs,
        marketHoursOnly: settings.marketHoursOnly,
        realTradingEnabled: settings.realTradingEnabled,
        recommendation: settings.recommendation,
        notification: settings.notification,
      })
      notifySuccess('저장되었습니다. 상단 배너를 새로고침하면 반영됩니다.')
      await load()
    } catch (error) {
      notifyError(error)
    } finally {
      setLoading(false)
    }
  }

  if (!settings) {
    return <p style={{ color: '#667085' }}>실행 설정 불러오는 중…</p>
  }

  return (
    <div className="card" style={{ marginTop: 16 }}>
      <h2 style={{ fontSize: 16 }}>실행 모드 · 종목 추천</h2>
      <p style={{ fontSize: 13, color: '#667085', marginTop: 6 }}>
        DB에 저장되며 재시작해도 유지됩니다. 종목 추천 규칙·알림 채널을 여기서 바꿀 수 있습니다.
      </p>

      <label style={{ display: 'block', marginTop: 12, fontSize: 13, color: '#344054' }}>실행 단계</label>
      <select
        value={settings.phase}
        onChange={(e) => setSettings({ ...settings, phase: e.target.value })}
        style={{ width: '100%', maxWidth: 480, padding: 10, borderRadius: 8, border: '1px solid #D0D5DD' }}
      >
        {PHASE_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <p style={{ fontSize: 12, color: '#667085', marginTop: 4 }}>{settings.phaseLabel}</p>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12, marginTop: 16 }}>
        <Field label="주문 수량">
          <input
            type="number"
            min={1}
            value={settings.orderQty}
            onChange={(e) => setSettings({ ...settings, orderQty: Number(e.target.value) || 1 })}
            style={inputStyle}
          />
        </Field>
        <Field label="최저 등급">
          <select
            value={settings.recommendation.minGrade}
            onChange={(e) =>
              setSettings({
                ...settings,
                recommendation: { ...settings.recommendation, minGrade: e.target.value },
              })
            }
            style={inputStyle}
          >
            {['S', 'A', 'B', 'C'].map((g) => (
              <option key={g} value={g}>
                {g}
              </option>
            ))}
          </select>
        </Field>
        <Field label="최소 점수">
          <input
            type="number"
            value={settings.recommendation.minAdjustedScore}
            onChange={(e) =>
              setSettings({
                ...settings,
                recommendation: {
                  ...settings.recommendation,
                  minAdjustedScore: Number(e.target.value) || 0,
                },
              })
            }
            style={inputStyle}
          />
        </Field>
        <Field label="일일 추천 상한">
          <input
            type="number"
            value={settings.recommendation.maxPerDay}
            onChange={(e) =>
              setSettings({
                ...settings,
                recommendation: { ...settings.recommendation, maxPerDay: Number(e.target.value) || 1 },
              })
            }
            style={inputStyle}
          />
        </Field>
      </div>

      <div style={{ marginTop: 12, display: 'flex', flexWrap: 'wrap', gap: 16 }}>
        <Check
          label="자동 주문 스캔 (AUTO 모드)"
          checked={settings.autoOrderScanEnabled}
          onChange={(v) => setSettings({ ...settings, autoOrderScanEnabled: v })}
        />
        <Check
          label="장중만 주문 (09:00~15:20)"
          checked={settings.marketHoursOnly}
          onChange={(v) => setSettings({ ...settings, marketHoursOnly: v })}
        />
        <Check
          label="신규 추천 시 알림"
          checked={settings.recommendation.notifyOnNew}
          onChange={(v) =>
            setSettings({
              ...settings,
              recommendation: { ...settings.recommendation, notifyOnNew: v },
            })
          }
        />
        <Check
          label="종목당 1건"
          checked={settings.recommendation.dedupeByStock}
          onChange={(v) =>
            setSettings({
              ...settings,
              recommendation: { ...settings.recommendation, dedupeByStock: v },
            })
          }
        />
        <Check
          label="실전 주문 허용 (위험)"
          checked={settings.realTradingEnabled}
          onChange={(v) => setSettings({ ...settings, realTradingEnabled: v })}
        />
      </div>

      <h3 style={{ fontSize: 14, marginTop: 20 }}>알림 채널</h3>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
        <Check
          label="텔레그램"
          checked={settings.notification.telegram}
          onChange={(v) =>
            setSettings({ ...settings, notification: { ...settings.notification, telegram: v } })
          }
        />
        <Check
          label="디스코드"
          checked={settings.notification.discord}
          onChange={(v) =>
            setSettings({ ...settings, notification: { ...settings.notification, discord: v } })
          }
        />
        <Check
          label="이메일"
          checked={settings.notification.email}
          onChange={(v) =>
            setSettings({ ...settings, notification: { ...settings.notification, email: v } })
          }
        />
        <Check
          label="카카오(webhook)"
          checked={settings.notification.kakao}
          onChange={(v) =>
            setSettings({ ...settings, notification: { ...settings.notification, kakao: v } })
          }
        />
        <Check
          label="문자(webhook)"
          checked={settings.notification.sms}
          onChange={(v) =>
            setSettings({ ...settings, notification: { ...settings.notification, sms: v } })
          }
        />
      </div>
      <input
        placeholder="추천 알림 이메일 (비우면 발신 계정)"
        value={settings.notification.emailTo}
        onChange={(e) =>
          setSettings({
            ...settings,
            notification: { ...settings.notification, emailTo: e.target.value },
          })
        }
        style={{ ...inputStyle, marginTop: 8, maxWidth: 480 }}
      />
      <input
        placeholder="카카오 webhook URL"
        value={settings.notification.kakaoWebhookUrl}
        onChange={(e) =>
          setSettings({
            ...settings,
            notification: { ...settings.notification, kakaoWebhookUrl: e.target.value },
          })
        }
        style={{ ...inputStyle, marginTop: 8, maxWidth: 480 }}
      />
      <input
        placeholder="SMS webhook URL"
        value={settings.notification.smsWebhookUrl}
        onChange={(e) =>
          setSettings({
            ...settings,
            notification: { ...settings.notification, smsWebhookUrl: e.target.value },
          })
        }
        style={{ ...inputStyle, marginTop: 8, maxWidth: 480 }}
      />

      <button className="button" type="button" style={{ marginTop: 16 }} disabled={loading} onClick={save}>
        {loading ? '저장 중…' : '실행·추천 설정 저장'}
      </button>
    </div>
  )
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '8px 10px',
  borderRadius: 8,
  border: '1px solid #D0D5DD',
  boxSizing: 'border-box',
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ fontSize: 13, color: '#344054' }}>
      {label}
      <div style={{ marginTop: 4 }}>{children}</div>
    </label>
  )
}

function Check({
  label,
  checked,
  onChange,
}: {
  label: string
  checked: boolean
  onChange: (v: boolean) => void
}) {
  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
      {label}
    </label>
  )
}
