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
import { displayText, toNumber } from '@/utils/publicDisplay'
import { calculateAverage } from '@/utils/statAverage'
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
  const name = displayText(detail.teamName, '未知球队')
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
    gameMode: detail.gameMode,
    eventTeamStatus: detail.eventTeamStatus,
    disqualified: Boolean(detail.disqualified),
    activeDisciplineSummary: detail.activeDisciplineSummary || [],
  }
}

export function mapV4TeamStatToSeasonStat(
  item: V4PublicTeamStatItem,
  extras?: {
    seasonId?: string
    divisionId?: string
  },
): PublicTeamSeasonStatVo {
  const games = toNumber(item.matchCount, 0)
  const wins = toNumber(item.winCount, 0)
  const losses = toNumber(item.loseCount, 0)

  return {
    seasonId: extras?.seasonId,
    teamId: String(item.eventTeamId),
    teamName: item.teamName,
    teamLogoUrl: item.teamLogoUrl,
    games,
    wins,
    losses,
    pointsFor: item.pointsFor,
    pointsAgainst: item.pointsAgainst,
    avgPoints: item.avgPointsFor ?? calculateAverage(item.pointsFor, games),
    avgPointsAgainst: item.avgPointsAgainst ?? calculateAverage(item.pointsAgainst, games),
    avgRebounds: calculateAverage(item.rebound, games),
    avgAssists: calculateAverage(item.assist, games),
    avgSteals: calculateAverage(item.steal, games),
    avgBlocks: calculateAverage(item.block, games),
    avgTurnovers: calculateAverage(item.turnover, games),
    avgFouls: item.foul != null ? calculateAverage(item.foul, games) : undefined,
    winRate: calculateAverage(wins, games),
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
  const name = displayText(team.teamName, '未知球队')
  const shortName = displayText(team.teamShortName || name.slice(0, 3), name.slice(0, 3))
  const platform = v4PlatformLabel(team.platformCode)
  const seasonName = displayText(
    extras?.seasonName
      ? extras.divisionLabel
        ? `${extras.seasonName} · ${extras.divisionLabel}`
        : extras.seasonName
      : extras?.divisionLabel,
    '公开赛季',
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
    description: '暂无球队简介',
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
