import { computed, type ComputedRef } from 'vue'
import { leagueSummary } from '../mock'
import type { LeagueHeroMetaItem, LeagueSummary } from '../types/events'

function safeText(value: string | number | undefined | null): string {
  if (value === 0) return '0'
  if (value === undefined || value === null || value === '') return '--'
  return String(value)
}

export function buildHeroMetaItems(summary: LeagueSummary): LeagueHeroMetaItem[] {
  return [
    {
      key: 'season',
      icon: 'calendar',
      title: safeText(summary.seasonName),
      value: safeText(summary.seasonPeriod),
    },
    {
      key: 'teams',
      icon: 'trophy',
      title: `${safeText(summary.teamCount)} 支战队`.replace('-- 支战队', '--'),
      value: safeText(summary.teamDescription),
    },
    {
      key: 'stage',
      icon: 'stage',
      title: '当前阶段',
      value: safeText(summary.currentStageName),
    },
    {
      key: 'platform',
      icon: 'gamepad',
      title: '比赛平台',
      value: safeText(summary.platformName),
    },
  ]
}

export function useLeagueHeroMeta(
  summary: LeagueSummary = leagueSummary,
): { heroMetaItems: ComputedRef<LeagueHeroMetaItem[]> } {
  const heroMetaItems = computed(() => buildHeroMetaItems(summary))
  return { heroMetaItems }
}
