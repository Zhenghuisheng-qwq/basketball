import type { HallTab } from '@/views/hall-of-fame/types'

const VALID_TABS: HallTab[] = ['weekly', 'event', 'champions']

export function normalizeHallTab(tab: unknown): HallTab {
  if (typeof tab === 'string' && VALID_TABS.includes(tab as HallTab)) {
    return tab as HallTab
  }
  return 'weekly'
}

export function parsePositiveInt(value: unknown, fallback: number): number {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 1) return fallback
  return Math.floor(parsed)
}

export function parseQueryString(value: unknown, fallback = 'ALL'): string {
  if (typeof value !== 'string' || !value.trim()) return fallback
  return value.trim()
}
