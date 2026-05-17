'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'

const links = [
  { href: '/strategy-settings', label: '전략 설정' },
  { href: '/market-strategy-settings', label: '시장별 설정' },
  { href: '/strategy-time-windows', label: '시간대별 설정' },
  { href: '/enabled-strategies-now', label: '현재 활성 전략' },
  { href: '/strategies', label: '분석·오케스트레이션' },
]

export function StrategyNavLinks() {
  const pathname = usePathname()
  return (
    <nav style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12 }}>
      {links.map(({ href, label }) => {
        const active = pathname === href
        return (
          <Link
            key={href}
            href={href}
            style={{
              padding: '6px 12px',
              borderRadius: 8,
              fontSize: 13,
              fontWeight: 500,
              textDecoration: 'none',
              border: `1px solid ${active ? '#84CAFF' : '#D0D5DD'}`,
              background: active ? '#EFF8FF' : '#fff',
              color: active ? '#175CD3' : '#344054',
            }}
          >
            {label}
          </Link>
        )
      })}
    </nav>
  )
}
