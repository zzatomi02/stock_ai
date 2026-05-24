import { AppShell } from '@/components/layout/AppShell'
import { StockRecommendationsPanel } from '@/components/recommendations/StockRecommendationsPanel'

export default function RecommendationsPage() {
  return (
    <AppShell>
      <StockRecommendationsPanel />
    </AppShell>
  )
}
