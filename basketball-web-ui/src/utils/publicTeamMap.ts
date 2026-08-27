import type {
  PublicMatchItemVo,
  PublicStandingItemVo,
  PublicTeamDetailVo,
  PublicTeamRankItemVo,
  PublicTeamRosterItemVo,
  PublicTeamSeasonStatVo,
} from '@/api/types/public'
import { descriptionToText, displayText, formatDateTime, mapMatchStatus, toNumber } from '@/utils/publicDisplay'
import { v4GameModeLabel, v4PlatformLabel } from '@/utils/v4Display'
import type { TeamBase, TeamMatchRecord, TeamRosterPlayer, TeamSeasonAverages } from '@/types/team'

export function mapPublicTeamToBase(
  detail: PublicTeamDetailVo,
  extras?: {
    wins?: number | null
    losses?: number | null
    ranking?: number | null
    seasonName?: string | null
  },
): TeamBase {
  const name = displayText(detail.teamName, '未知球队')
  const shortName = displayText(detail.shortName || detail.teamNo || name, name).slice(0, 8)
  return {
    id: String(detail.teamId),
    code: shortName,
    name,
    shortName,
    logoUrl: detail.logoUrl?.trim() || '',
    largeLogoUrl: detail.logoUrl?.trim() || '',
    captainName: '--',
    platform: v4PlatformLabel(detail.platformCode || detail.gameTerminal),
    currentStage: 'regular',
    rosterCount: toNumber(detail.currentMemberCount, 0),
    // 认证态用 auditStatus，勿把 ACTIVE 当“有效俱乐部”
    verified: detail.auditStatus === 1,
    seasonName: displayText(
      extras?.seasonName
        || [
          detail.seriesName,
          detail.seasonName,
          detail.divisionName,
          detail.platformCode ? v4PlatformLabel(detail.platformCode) : '',
          detail.gameMode ? v4GameModeLabel(detail.gameMode) : '',
        ]
          .filter(Boolean)
          .join(' · '),
      detail.legacyTeam ? '历史球队' : '当前赛事',
    ),
    seasonYear: new Date().getFullYear(),
    description: descriptionToText(detail.description) || '暂无球队简介',
    wins: toNumber(extras?.wins, 0),
    losses: toNumber(extras?.losses, 0),
    ranking: toNumber(extras?.ranking, 0),
    themeColor: '#c9a360',
    secondaryColor: '#111111',
  }
}

export function mapPublicRoster(item: PublicTeamRosterItemVo, teamId: string): TeamRosterPlayer {
  return {
    id: String(item.playerId),
    teamId,
    nickname: displayText(item.playerName, '未知球员'),
    position: displayText(item.positionCode, '--'),
    avatar: item.avatarUrl?.trim() || '',
    isCaptain: (item.roleType || '').toUpperCase() === 'CAPTAIN',
  }
}

export function mapPublicTeamMatch(item: PublicMatchItemVo, teamId: string): TeamMatchRecord {
  const isHome = String(item.homeTeamId || '') === String(teamId)
  const opponentId = String((isHome ? item.awayTeamId : item.homeTeamId) || '')
  const opponentName = displayText(isHome ? item.awayTeamName : item.homeTeamName, '对手')
  const teamScore = isHome ? item.homeScore : item.awayScore
  const opponentScore = isHome ? item.awayScore : item.homeScore
  const status = mapMatchStatus(item.matchStatus)
  const forfeited = String(item.resultType || '').toUpperCase() === 'FORFEIT'
    || String(item.resultType || '').toUpperCase() === 'EVENT_DISQUALIFICATION'
  const finished = status === 'finished' || status === 'live' || forfeited
  const winnerId = String(item.winnerTeamId || '')
  const playerSideId = String((isHome ? item.homeTeamId : item.awayTeamId) || '')
  let result: TeamMatchRecord['result']
  if (finished && winnerId && (winnerId === String(teamId) || winnerId === playerSideId)) {
    result = 'win'
  } else if (finished && winnerId) {
    result = 'loss'
  } else if (finished && teamScore != null && opponentScore != null) {
    result = teamScore > opponentScore ? 'win' : teamScore < opponentScore ? 'loss' : undefined
  }
  const dt = formatDateTime(item.scheduledStartTime)
  return {
    id: String(item.matchId),
    teamId: String(teamId),
    opponentId,
    date: dt.date === '--' ? '' : dt.date,
    time: dt.time === '--' ? '' : dt.time,
    teamScore: teamScore ?? undefined,
    opponentScore: opponentScore ?? undefined,
    result,
    status: status === 'finished' || status === 'live' ? 'finished' : 'scheduled',
    stage: 'regular',
    competition: displayText(item.stageName || item.groupName, '公开赛事'),
    playoffGame: opponentName,
  }
}

export function mapTeamSeasonAverages(stat?: PublicTeamSeasonStatVo | null): TeamSeasonAverages | null {
  if (!stat) return null
  return {
    points: toNumber(stat.avgPoints, 0),
    rebounds: toNumber(stat.avgRebounds, 0),
    assists: toNumber(stat.avgAssists, 0),
    steals: toNumber(stat.avgSteals, 0),
    blocks: toNumber(stat.avgBlocks, 0),
    fouls: stat.avgFouls != null ? toNumber(stat.avgFouls, 0) : null,
    turnovers: toNumber(stat.avgTurnovers, 0),
  }
}

export function mapStandingToTeamBase(
  row: PublicStandingItemVo,
  seasonName?: string | null,
): TeamBase {
  return mapPublicTeamToBase(
    {
      teamId: row.teamId,
      teamName: row.teamName,
      logoUrl: row.teamLogoUrl,
    },
    {
      wins: row.wins,
      losses: row.losses,
      ranking: row.rankNo,
      seasonName,
    },
  )
}

export function mapRankToTeamBase(
  row: PublicTeamRankItemVo,
  seasonName?: string | null,
): TeamBase {
  return mapPublicTeamToBase(
    {
      teamId: row.teamId,
      teamName: row.teamName,
      logoUrl: row.teamLogoUrl,
    },
    {
      ranking: row.rankNo,
      seasonName,
    },
  )
}

export function placeholderTeamBase(teamId: string): TeamBase {
  return {
    id: teamId,
    code: '--',
    name: '加载中',
    shortName: '--',
    logoUrl: '',
    largeLogoUrl: '',
    captainName: '--',
    platform: '--',
    currentStage: 'regular',
    rosterCount: 0,
    verified: false,
    seasonName: '--',
    seasonYear: new Date().getFullYear(),
    description: '',
    wins: 0,
    losses: 0,
    ranking: 0,
    themeColor: '#c9a360',
    secondaryColor: '#111111',
  }
}
