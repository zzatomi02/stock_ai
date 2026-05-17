'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import type { ReactNode } from 'react'
import {
  BarChart3,
  CircleUserRound,
  FileText,
  LayoutDashboard,
  LineChart,
  ListOrdered,
  Newspaper,
  Rss,
  Settings,
  Sparkles,
  Star,
  Wallet,
  Zap,
} from 'lucide-react'

type Item = { label: string; href: string; icon: ReactNode }

const main: Item[] = [{ label: '대시보드', href: '/', icon: <LayoutDashboard size={18} /> }]

const market: Item[] = [
  { label: '시장 · Top100', href: '/market', icon: <LineChart size={18} /> },
  { label: '보유 잔고', href: '/portfolio', icon: <Wallet size={18} /> },
  { label: '주문 내역', href: '/orders', icon: <ListOrdered size={18} /> },
]

const insight: Item[] = [
  { label: '매매 시그널', href: '/signals', icon: <Zap size={18} /> },
  { label: '관심종목', href: '/watchlist', icon: <Star size={18} /> },
  { label: '전략 관리', href: '/strategies', icon: <Sparkles size={18} /> },
  { label: '전략 설정', href: '/strategy-settings', icon: <Sparkles size={18} /> },
  { label: '시장별 전략', href: '/market-strategy-settings', icon: <Sparkles size={18} /> },
  { label: '시간대별 전략', href: '/strategy-time-windows', icon: <Sparkles size={18} /> },
  { label: '활성 전략', href: '/enabled-strategies-now', icon: <Zap size={18} /> },
  { label: '뉴스 분석', href: '/news', icon: <Newspaper size={18} /> },
  { label: '뉴스 소스 관리', href: '/news/ingest', icon: <Rss size={18} /> },
  { label: '거래 통계', href: '/statistics', icon: <BarChart3 size={18} /> },
]

const system: Item[] = [
  { label: '설정', href: '/settings', icon: <Settings size={18} /> },
  { label: 'GPT 프롬프트', href: '/ai/prompts', icon: <FileText size={18} /> },
  { label: 'AI 기업 분석', href: '/ai/company-analysis', icon: <Sparkles size={18} /> },
  { label: '뉴스 키워드', href: '/news/keywords', icon: <FileText size={18} /> },
  { label: '로그인 / 회원가입', href: '/auth', icon: <CircleUserRound size={18} /> },
]

function isActive(pathname: string, href: string) {
  if (href === '/') return pathname === '/'
  return pathname === href || pathname.startsWith(`${href}/`)
}

function NavBlock({ title, items, pathname }: { title: string; items: Item[]; pathname: string }) {
  return (
    <div className="sidebar-block">
      <div className="sidebar-block-title">{title}</div>
      <nav className="sidebar-nav">
        {items.map(({ label, href, icon }) => {
          const active = isActive(pathname, href)
          return (
            <Link key={href} href={href} className={`sidebar-link${active ? ' sidebar-link--active' : ''}`}>
              <span className="sidebar-link-icon" aria-hidden>
                {icon}
              </span>
              <span>{label}</span>
            </Link>
          )
        })}
      </nav>
    </div>
  )
}

export function Sidebar() {
  const pathname = usePathname()
  return (
    <aside className="sidebar" aria-label="StockAI 메뉴">
      <div className="sidebar-brand">
        <Link href="/" className="sidebar-logo">
          <span className="sidebar-logo-mark">SA</span>
          <span className="sidebar-logo-text">
            <span className="sidebar-logo-name">StockAI</span>
            <span className="sidebar-logo-sub">주식 AI 콘솔</span>
          </span>
        </Link>
      </div>
      <div className="sidebar-scroll">
        <NavBlock title="메인" items={main} pathname={pathname} />
        <NavBlock title="시장 · 주문" items={market} pathname={pathname} />
        <NavBlock title="AI · 분석" items={insight} pathname={pathname} />
        <NavBlock title="시스템" items={system} pathname={pathname} />
      </div>
      <div className="sidebar-foot">
        <span className="sidebar-foot-muted">v0.1 · Next 15</span>
      </div>
    </aside>
  )
}
