import type {
  PublicTeamDetailVo,
  PublicTeamRankItemVo,
  PublicTeamRosterItemVo,
  PublicTeamSeasonStatVo,
} from '@/api/types/public'
import type {
  V4PublicDivisionDetail,
  V4PublicEventPlayerItem,
  V4PublicEventTeamDetail,
  V4PublicEventTeamItem,
  V4PublicStandingItem,
  V4PublicTeamStatItem,
} from '@/api/types/v4Public'
import { translate } from '@/i18n'
import { displayText, toNumber } from '@/utils/publicDisplay'
import { calculateAverage } from '@/utils/statAverage'
import { boxScoreGames, teamRecordGames } from '@/utils/teamRecordGames'
import { v4PlatformLabel } from '@/utils/v4Display'
import type { TeamBase, TeamStage } from '@/types/team'

export function mapV4PlayerToRoster(player: V4PublicEventPlayerItem): PublicTeamRosterItemVo {
  const sourcePlayerId = String(player.sourcePlayerId || '').trim()
  return {
    // 公开球员详情路由必须用永久 Player ID
    playerId: sourcePlayerId,
    eventPlayerId: String(player.eventPlayerId || ''),
    playerName: player.playerName,
    avatarUrl: player.avatarUrl,
    positionCode: player.position,
    jerseyNo: player.jerseyNo,
    roleType: toNumber(player.isCaptain, 0) === 1 ? 'CAPTAIN' : undefined,
  }
}

export function mapV4TeamDetailToPublic(
  detail: V4PublicEventTeamDetail,
  extras?: {
    divisionId?: string
    divisionName?: string
    seasonId?: string
    seasonTitle?: string
    seriesName?: string
    seriesCode?: string
  },
): PublicTeamDetailVo {
  const name = displayText(detail.teamName, translate('publicPages.team.unknownTeam'))
  return {
    teamId: String(detail.eventTeamId),
    teamName: name,
    shortName: displayText(detail.teamShortName || name.slice(0, 3), name.slice(0, 3)),
    logoUrl: detail.teamLogoUrl?.trim() || '',
    platformCode: detail.platformCode,
    currentMemberCount: detail.players?.length ?? 0,
    teamStatus: detail.eventTeamStatus === 'ACTIVE' ? 1 : 0,
    divisionId: extras?.divisionId || (detail.divisionId ? String(detail.divisionId) : undefined),
    divisionName: extras?.divisionName || detail.divisionName,
    seasonId: extras?.seasonId || (detail.seasonId ? String(detail.seasonId) : undefined),
    seasonName: extras?.seasonTitle || detail.seasonName,
    seriesName: extras?.seriesName || detail.seriesName,
    seriesCode: extras?.seriesCode || detail.seriesCode,
    sourceTeamId: detail.sourceTeamId ? String(detail.sourceTeamId) : undefined,
    gameMode: detail.gameMode,
    eventTeamStatus: detail.eventTeamStatus,
    disqualified: Boolean(detail.disqualified),
    activeDisciplineSummary: detail.activeDisciplineSummary || [],
    description: detail.description,
  }
}

export function standingDecidedGames(row?: { winCount?: number | null; loseCount?: number | null } | null): number {
  return toNumber(row?.winCount, 0) + toNumber(row?.loseCount, 0)
}

/** 同一球队有多条积分榜时，用已赛场次更多的那条，名次只在场次相同时比较。 */
export function preferStandingRow<T extends {
  winCount?: number | null
  loseCount?: number | null
  rankNo?: number | null
}>(current: T | undefined, next: T): T {
  if (!current) return next
  const gameDiff = standingDecidedGames(next) - standingDecidedGames(current)
  if (gameDiff !== 0) return gameDiff > 0 ? next : current
  return toNumber(next.rankNo, 9999) < toNumber(current.rankNo, 9999) ? next : current
}

export function mapV4TeamStatToSeasonStat(
  item: V4PublicTeamStatItem,
  extras?: {
    seasonId?: string
    divisionId?: string
  },
): PublicTeamSeasonStatVo {
  const wins = toNumber(item.winCount, 0)
  const losses = toNumber(item.loseCount, 0)
  const recordGames = teamRecordGames(item.matchCount, wins, losses)
  const boxGames = boxScoreGames(item.matchCount, wins, losses)

  return {
    seasonId: extras?.seasonId,
    teamId: String(item.eventTeamId),
    teamName: item.teamName,
    teamLogoUrl: item.teamLogoUrl,
    games: recordGames,
    wins,
    losses,
    pointsFor: item.pointsFor,
    pointsAgainst: item.pointsAgainst,
    avgPoints: item.avgPointsFor ?? calculateAverage(item.pointsFor, boxGames),
    avgPointsAgainst: item.avgPointsAgainst ?? calculateAverage(item.pointsAgainst, boxGames),
    avgRebounds: calculateAverage(item.rebound, boxGames),
    avgAssists: calculateAverage(item.assist, boxGames),
    avgSteals: calculateAverage(item.steal, boxGames),
    avgBlocks: calculateAverage(item.block, boxGames),
    avgTurnovers: calculateAverage(item.turnover, boxGames),
    avgFouls: item.foul != null ? calculateAverage(item.foul, boxGames) : undefined,
    winRate: calculateAverage(wins, recordGames),
  }
}

export function mapV4StandingToRank(
  row: V4PublicStandingItem,
  seasonId?: string,
): PublicTeamRankItemVo {
  return {
    seasonId,
    teamId: String(row.eventTeamId),
    teamName: row.teamName,
    teamLogoUrl: row.teamLogoUrl,
    rankNo: row.rankNo,
  }
}

export function mapV4EventTeamToBase(
  team: V4PublicEventTeamItem | V4PublicEventTeamDetail,
  extras?: {
    wins?: number
    losses?: number
    ranking?: number
    seasonName?: string
    divisionLabel?: string
    currentStage?: TeamStage
    rosterCount?: number
  },
): TeamBase {
  const name = displayText(team.teamName, translate('publicPages.team.unknownTeam'))
  const shortName = displayText(team.teamShortName || name.slice(0, 3), name.slice(0, 3))
  const platform = v4PlatformLabel(team.platformCode)
  const seasonName = displayText(
    extras?.seasonName
      ? extras.divisionLabel
        ? `${extras.seasonName} · ${extras.divisionLabel}`
        : extras.seasonName
      : extras?.divisionLabel,
    translate('publicPages.team.publicSeason'),
  )

  return {
    id: String(team.eventTeamId),
    code: shortName,
    name,
    shortName,
    logoUrl: team.teamLogoUrl?.trim() || '',
    largeLogoUrl: team.teamLogoUrl?.trim() || '',
    captainName: displayText(team.captainName, '--'),
    platform,
    currentStage: extras?.currentStage ?? 'regular',
    rosterCount: extras?.rosterCount ?? ('players' in team ? team.players.length : 0),
    verified: team.eventTeamStatus === 'ACTIVE',
    seasonName,
    seasonYear: new Date().getFullYear(),
    description: translate('publicPages.team.noBio'),
    wins: toNumber(extras?.wins, 0),
    losses: toNumber(extras?.losses, 0),
    ranking: toNumber(extras?.ranking, 0),
    themeColor: '#c9a360',
    secondaryColor: '#111111',
  }
}

export function mapV4StandingToTeamBase(
  row: V4PublicStandingItem,
  extras?: {
    seasonName?: string
    divisionLabel?: string
    platformCode?: string
    captainName?: string
  },
): TeamBase {
  return mapV4EventTeamToBase(
    {
      eventTeamId: row.eventTeamId,
      teamName: row.teamName || '',
      teamLogoUrl: row.teamLogoUrl,
      platformCode: extras?.platformCode,
      captainName: extras?.captainName,
    },
    {
      wins: row.winCount,
      losses: row.loseCount,
      ranking: row.rankNo,
      seasonName: extras?.seasonName,
      divisionLabel: extras?.divisionLabel,
    },
  )
}

export function divisionContextLabel(division?: V4PublicDivisionDetail | null): string {
  if (!division) return ''
  const platform = v4PlatformLabel(division.platformCode)
  const name = displayText(division.divisionName, '')
  return name ? `${platform} · ${name}` : platform
}
