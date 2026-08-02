import type {
  PublicHonorItemResp,
  PublicMatchItemVo,
  PublicPlayerDetailVo,
  PublicPlayerSeasonStatVo,
} from '@/api/types/public'
import { displayText, formatDateTime, toNumber } from '@/utils/publicDisplay'
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
  const teamName = displayText(detail.currentTeamName, '--')
  const verified = toNumber(detail.certificationStatus, 0) === 1
  return {
    id: String(detail.playerId),
    playerName: displayName,
    displayName,
    avatarUrl: detail.avatarUrl?.trim() || '',
    badgeUrl: '',
    verified,
    registrationText: verified ? 'CNPA REGISTERED PLAYER' : 'CNPA PLAYER',
    currentTeamId: String(detail.currentTeamId || ''),
    currentTeamName: teamName,
    currentTeamAbbr: teamName.length > 4 ? teamName.slice(0, 4) : teamName,
    currentTeamLogoUrl: detail.currentTeamLogoUrl?.trim() || '',
    teamKey: String(detail.currentTeamId || ''),
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
    fieldGoalsMade: 0,
    fieldGoalsAttempted: 0,
    threePointsMade: 0,
    threePointsAttempted: 0,
    freeThrowsMade: 0,
    freeThrowsAttempted: 0,
  }
}

export function mapPublicPlayerSeasonStats(
  rows: PublicPlayerSeasonStatVo[],
): PlayerCareerStatistics[] {
  return rows.map((row) => ({
    seasonId: String(row.seasonId || ''),
    tournamentName: displayText(row.teamName ? `赛季 ${row.seasonId}` : row.seasonId, '公开赛季'),
    teamId: String(row.teamId || ''),
    teamName: displayText(row.teamName, '--'),
    teamLogoUrl: row.teamLogoUrl?.trim() || '',
    platform: '--',
    games: toNumber(row.games, 0),
    points: toNumber(row.avgPoints ?? row.points, 0),
    rebounds: toNumber(row.avgRebounds ?? row.rebounds, 0),
    assists: toNumber(row.avgAssists ?? row.assists, 0),
    steals: toNumber(row.avgSteals ?? row.steals, 0),
    blocks: toNumber(row.avgBlocks ?? row.blocks, 0),
    turnovers: 0,
    fouls: 0,
    fieldGoalPercentage: toNumber(row.fieldGoalPct, 0),
    threePointPercentage: toNumber(row.threePointPct, 0),
    freeThrowPercentage: toNumber(row.freeThrowPct, 0),
  }))
}

export function mapSeasonStageOptions(rows: PublicPlayerSeasonStatVo[]): PlayerSeasonStageOption[] {
  return rows.map((row, index) => ({
    id: String(row.seasonId || row.stageId || index),
    label: displayText(row.teamName ? `${row.teamName} · 赛季 ${row.seasonId}` : `赛季 ${row.seasonId}`, '公开赛季'),
    tournamentName: displayText(row.teamName, '公开赛季'),
    stageLabel: displayText(row.stageId ? `阶段 ${row.stageId}` : '常规赛', '常规赛'),
    games: toNumber(row.games, 0),
  }))
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
