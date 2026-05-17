import { create } from 'zustand'

type Range = 'today' | 'week' | 'month' | 'custom'

interface FilterState {
  range: Range
  keyword: string
  from?: string
  to?: string
  setRange: (range: Range) => void
  setKeyword: (keyword: string) => void
  setDates: (from?: string, to?: string) => void
}

export const useFilterStore = create<FilterState>((set) => ({
  range: 'today',
  keyword: '',
  setRange: (range) => set({ range }),
  setKeyword: (keyword) => set({ keyword }),
  setDates: (from, to) => set({ from, to }),
}))
