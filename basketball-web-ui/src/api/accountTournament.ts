import { getV4MyEvents } from '@/api/v4PlayerEvents'
import type { ApiResult, PageResult } from '@/api/types/common'
import type { AccountTournamentItemDto } from '@/api/types/accountTournament'
import type { V4MyEvent } from '@/api/types/v4PlayerEvents'

const APPROVED_REGISTRATION_STATUS = 3

function buildNextMatchSummary(event: V4MyEvent) {
  if (!event.nextMatchId && !event.nextMatchTime) {
    return undefined
  }
  return {
    matchId: event.nextMatchId,
    matchTime: event.nextMatchTime,
    stageName: event.currentStageName,
    groupName: event.currentGroupName,
    standingRank: event.standingRank,
    standingPoints: event.standingPoints,
    qualificationStatus: event.qualificationStatus,
    divisionId: event.divisionId,
    divisionName: event.divisionName,
  }
}

function mapV4MyEventToTournamentItem(event: V4MyEvent): AccountTournamentItemDto {
  return {
    registrationId: event.eventTeamId,
    registrationNo: event.eventTeamId,
    tournamentId: event.seasonId,
    tournamentName: event.seriesName || event.seasonTitle || '',
    seasonId: event.seasonId,
    seasonName: event.seasonTitle,
    platformCode: event.platformCode,
    platformName: event.platformName || event.platformCode,
    teamId: event.eventTeamId,
    teamName: event.eventTeamName || event.teamName,
    registrationStatus: APPROVED_REGISTRATION_STATUS,
    rosterRole: event.roleInTeam,
    nextMatch: buildNextMatchSummary(event),
  }
}

function filterItems(
  items: AccountTournamentItemDto[],
  status?: number,
  keyword?: string,
): AccountTournamentItemDto[] {
  let filtered = items
  if (status != null) {
    filtered = filtered.filter((item) => item.registrationStatus === status)
  }
  const normalizedKeyword = keyword?.trim().toLowerCase()
  if (normalizedKeyword) {
    filtered = filtered.filter((item) =>
      [item.registrationNo, item.tournamentName, item.seasonName, item.teamName]
        .some((value) => value?.toLowerCase().includes(normalizedKeyword)),
    )
  }
  return filtered
}

function paginate<T>(items: T[], currentPage = 1, pageSize = 10): PageResult<T> {
  const current = Math.max(1, currentPage)
  const size = Math.max(1, pageSize)
  const total = items.length
  const pages = Math.max(1, Math.ceil(total / size))
  const start = (current - 1) * size
  return {
    records: items.slice(start, start + size),
    total,
    size,
    current,
    pages,
  }
}

export async function getAccountTournamentPage(params: {
  currentPage?: number
  pageSize?: number
  status?: number
  keyword?: string
}) {
  const res = await getV4MyEvents()
  const items = (res.data || []).map(mapV4MyEventToTournamentItem)
  const filtered = filterItems(items, params.status, params.keyword)
  return {
    ...res,
    data: paginate(filtered, params.currentPage, params.pageSize),
  } as ApiResult<PageResult<AccountTournamentItemDto>>
}

export async function getAccountTournamentDetail(registrationId: string | number) {
  const res = await getAccountTournamentPage({ currentPage: 1, pageSize: 1000 })
  const targetId = String(registrationId)
  const item = (res.data.records || []).find((record) => record.registrationId === targetId)
  if (!item) {
    throw new Error('未找到赛事报名信息')
  }
  return {
    ...res,
    data: item,
  } as ApiResult<AccountTournamentItemDto>
}
