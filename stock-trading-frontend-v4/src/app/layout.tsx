import type { Metadata } from 'next'
import { AppToaster } from '@/components/providers/AppToaster'
import './globals.css'

export const metadata: Metadata = {
  title: 'StockAI · 주식 AI 콘솔',
  description: '시세, 주문, 전략, 뉴스·통계를 한 화면에서 관리합니다.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <AppToaster />
        {children}
      </body>
    </html>
  )
}
