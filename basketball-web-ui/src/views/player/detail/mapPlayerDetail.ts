import type {
  PublicHonorItemResp,
  PublicMatchItemVo,
  PublicPlayerDetailVo,
  PublicPlayerSeasonStatVo,
} from '@/api/types/public'
import { displayText, formatDateTime, toNumber } from '@/utils/publicDisplay'
import { v4GameModeLabel, v4PlatformLabel } from '@/utils/v4Display'
import type {
  PlayerCareerStatistics,
  PlayerDetailData,
  PlayerHonor,
  PlayerMatchRecord,
  PlayerProfile,
  PlayerSeasonStageOption,
  PlayerTeamHistory,
} from './types'
import { EMPTY_HONOR_SUMMARY } from './types'

export function mapPublicPlayerProfile(detail: PublicPlayerDetailVo): PlayerProfile {
  const displayName = displayText(detail.displayName, '未知球员')
  const currentTeams = (detail.currentTeams || []).map((item) => {
    const teamName = displayText(item.teamName, '--')
    const eventContext = [item.seriesName, item.seasonName, item.divisionName, item.platformCode, item.gameMode]
      .filter(Boolean)
      .join(' · ')
    return {
      teamId: String(item.teamId || ''),
      teamName,
      shortName: displayText(item.shortName, teamName).slice(0, 8),
      logoUrl: item.logoUrl?.trim() || '',
      eventContext: eventContext || '当前赛事球队',
    }
  })
  const primary = currentTeams[0]
  const teamName = primary?.teamName || displayText(detail.currentTeamName, '--')
  const verified = toNumber(detail.certificationStatus, 0) === 3 || toNumber(detail.certificationStatus, 0) === 1
  return {
    id: String(detail.playerId),
    playerName: displayName,
    displayName,
    avatarUrl: detail.avatarUrl?.trim() || '',
    badgeUrl: '',
    verified,
    registrationText: verified ? 'CNPA REGISTERED PLAYER' : 'CNPA PLAYER',
    currentTeamId: primary?.teamId || String(detail.currentTeamId || ''),
    currentTeamName: teamName,
    currentTeamAbbr: (primary?.shortName || teamName).slice(0, 8),
    currentTeamLogoUrl: primary?.logoUrl || detail.currentTeamLogoUrl?.trim() || '',
    currentTeams,
    teamKey: primary?.teamId || String(detail.currentTeamId || ''),
    logoColor: '#c9a360',
    position: displayText(detail.positionCode, '--'),
    platform: '--',
    currentStage: '常规赛',
  }
}

export function mapPublicPlayerMatch(
  item: PublicMatchItemVo,
  playerTeamId: string,
  playerTeamName: string,
): PlayerMatchRecord {
  const isHome = String(item.homeTeamId || '') === String(playerTeamId)
  const opponentId = String((isHome ? item.awayTeamId : item.homeTeamId) || '')
  const opponentName = displayText(isHome ? item.awayTeamName : item.homeTeamName, '对手')
  const opponentLogoUrl = (isHome ? item.awayTeamLogoUrl : item.homeTeamLogoUrl)?.trim() || ''
  const teamScore = toNumber(isHome ? item.homeScore : item.awayScore, 0)
  const opponentScore = toNumber(isHome ? item.awayScore : item.homeScore, 0)
  const finished = toNumber(item.matchStatus, 0) === 3
  const result: PlayerMatchRecord['result'] =
    finished && teamScore >= opponentScore ? (teamScore > opponentScore ? 'WIN' : 'LOSS') : 'LOSS'
  const dt = formatDateTime(item.scheduledStartTime)
  return {
    id: String(item.matchId),
    matchId: String(item.matchId),
    date: dt.date === '--' ? '' : dt.date,
    tournamentName: displayText(item.groupName, '公开赛事'),
    stageName: displayText(item.stageName, '--'),
    teamName: playerTeamName,
    teamId: playerTeamId,
    teamLogoUrl: (isHome ? item.homeTeamLogoUrl : item.awayTeamLogoUrl)?.trim() || '',
    opponentName,
    opponentId,
    opponentLogoUrl,
    result,
    teamScore,
    opponentScore,
    minutes: 0,
    points: 0,
    rebounds: 0,
    assists: 0,
    steals: 0,
    blocks: 0,
    turnovers: 0,
    fouls: 0,
    // 比赛级球员投篮字段当前无正式数据源
    fieldGoalsMade: null,
    fieldGoalsAttempted: null,
    threePointsMade: null,
    threePointsAttempted: null,
    freeThrowsMade: null,
    freeThrowsAttempted: null,
  }
}

function nullablePct(value: number | string | null | undefined): number | null {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function buildSeasonLabel(row: PublicPlayerSeasonStatVo): string {
  const season = displayText(row.seasonName || row.seriesName, '公开赛季')
  const platform = v4PlatformLabel(row.platformCode)
  const mode = v4GameModeLabel(row.gameMode)
  return [season, platform, mode].filter((part) => part && part !== '--').join(' · ')
}

export function mapPublicPlayerSeasonStats(
  rows: PublicPlayerSeasonStatVo[],
): PlayerCareerStatistics[] {
  return rows.map((row) => ({
    // 用 divisionId 作为赛季选择主键，避免同 Season 多 Division 合并
    seasonId: String(row.divisionId || row.seasonId || ''),
    divisionId: String(row.divisionId || ''),
    tournamentName: buildSeasonLabel(row),
    teamId: String(row.eventTeamId || row.teamId || ''),
    teamName: displayText(row.teamName, '--'),
    teamLogoUrl: row.teamLogoUrl?.trim() || '',
    platform: v4PlatformLabel(row.platformCode),
    games: toNumber(row.games, 0),
    points: toNumber(row.avgPoints ?? row.points, 0),
    rebounds: toNumber(row.avgRebounds ?? row.rebounds, 0),
    assists: toNumber(row.avgAssists ?? row.assists, 0),
    steals: toNumber(row.avgSteals ?? row.steals, 0),
    blocks: toNumber(row.avgBlocks ?? row.blocks, 0),
    turnovers: toNumber(row.avgTurnovers ?? row.turnovers, 0),
    fouls: toNumber(row.fouls, 0),
    fieldGoalPercentage: nullablePct(row.fieldGoalPct),
    threePointPercentage: nullablePct(row.threePointPct),
    freeThrowPercentage: nullablePct(row.freeThrowPct),
  }))
}

export function mapSeasonStageOptions(rows: PublicPlayerSeasonStatVo[]): PlayerSeasonStageOption[] {
  return rows.map((row, index) => {
    const label = buildSeasonLabel(row)
    return {
      id: String(row.divisionId || row.seasonId || row.stageId || index),
      label,
      tournamentName: label,
      stageLabel: displayText(row.divisionName, '常规赛'),
      games: toNumber(row.games, 0),
    }
  })
}

export function buildPlayerTeamHistory(profile: PlayerProfile): PlayerTeamHistory[] {
  if (!profile.currentTeamId) return []
  return [
    {
      id: 1,
      startDate: '--',
      endDate: '至今',
      teamId: profile.currentTeamId,
      teamName: profile.currentTeamName,
      teamLogoUrl: profile.currentTeamLogoUrl,
      description: '当前所属球队',
      current: true,
    },
  ]
}

export function mapPublicPlayerHonors(rows: PublicHonorItemResp[]): PlayerHonor[] {
  return rows.map((item) => ({
    id: String(item.honorId),
    seasonName: displayText(item.seasonName || item.tournamentName, '公开赛季'),
    honorName: displayText(item.honorTitle || item.honorName, '球员荣誉'),
    honorDate: formatDateTime(item.awardDate).date,
    imageUrl: item.coverUrl || item.playerAvatarUrl || '',
    tournamentId: item.tournamentId ? String(item.tournamentId) : undefined,
  }))
}

export function buildEmptyPlayerDetail(profile: PlayerProfile): PlayerDetailData {
  return {
    profile,
    honorSummary: { ...EMPTY_HONOR_SUMMARY },
    honors: [],
    recentMatches: [],
    allMatches: [],
    careerStats: [],
    seasonStageOptions: [],
    teamHistory: buildPlayerTeamHistory(profile),
    nameHistory: [
      {
        id: 1,
        startDate: '--',
        endDate: '至今',
        playerName: profile.displayName,
        description: '当前使用名称',
        current: true,
      },
    ],
  }
}
