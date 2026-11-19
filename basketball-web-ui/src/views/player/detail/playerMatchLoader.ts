import { getV4PublicMatchDetail, getV4PublicMatchList } from '@/api/v4Public'
import {
  applyPlayerBoxScore,
  findPlayerBoxScore,
  mapV4ItemToPlayerMatch,
} from './mapPlayerDetail'
import type { PlayerMatchRecord, PlayerProfile, PlayerSeasonStageOption } from './types'

export async function fetchPlayerMatchesForEvents(
  events: PlayerSeasonStageOption[],
): Promise<PlayerMatchRecord[]> {
  const selected = events.filter((event): event is PlayerSeasonStageOption & { eventTeamId: string } =>
    Boolean(event.id && event.eventTeamId),
  )
  if (!selected.length) return []
  const lists = await Promise.allSettled(selected.map((event) => getV4PublicMatchList({
    divisionId: event.id,
    eventTeamId: event.eventTeamId,
  })))
  const rejected = lists.find((item) => item.status === 'rejected')
  const rows = lists.flatMap((item, index) => {
    if (item.status !== 'fulfilled') return []
    const event = selected[index]
    return (item.value.data ?? []).flatMap((match) => {
      const row = mapV4ItemToPlayerMatch(match, {
        eventTeamId: event.eventTeamId || '',
        teamName: event.teamName || '',
        teamLogoUrl: event.teamLogoUrl,
        seriesName: event.seriesName,
        seasonName: event.seasonName || event.label,
      })
      return row ? [row] : []
    })
  })
  if (!rows.length && rejected?.status === 'rejected') {
    throw rejected.reason
  }
  return rows
}

export async function hydratePlayerMatches(
  rows: PlayerMatchRecord[],
  profile: Pick<PlayerProfile, 'id' | 'displayName' | 'eventTeamId' | 'eventPlayerId'>,
  isCurrent: () => boolean,
  patch: (matchId: string, row: PlayerMatchRecord) => void,
) {
  const queue = rows.filter((row) => row.status !== 'scheduled' && row.matchId)
  const workerCount = Math.min(4, queue.length)
  if (!workerCount) return
  await Promise.all(Array.from({ length: workerCount }, async () => {
    while (queue.length) {
      const row = queue.shift()
      if (!row || !isCurrent()) return
      try {
        const res = await getV4PublicMatchDetail(row.matchId)
        if (!isCurrent()) return
        patch(String(row.matchId), applyPlayerBoxScore(row, findPlayerBoxScore(res.data?.playerStats, profile)))
      } catch {
        // 单场失败不影响列表
      }
    }
  }))
}
