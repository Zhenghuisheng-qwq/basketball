import type {
  PublicHonorItemResp,
  PublicMatchItemVo,
  PublicPlayerDetailVo,
  PublicPlayerSeasonStatVo,
} from '@/api/types/public'
import type { V4PublicMatchPlayerStatItem } from '@/api/types/v4Public'
import { translate } from '@/i18n'
import { displayText, formatDateTime, toNumber } from '@/utils/publicDisplay'
import { v4PlatformLabel } from '@/utils/v4Display'
import type {
  PlayerCareerStatistics,
  PlayerDetailData,
  PlayerHonor,
  PlayerMatchRecord,
  PlayerNameHistory,
  PlayerProfile,
  PlayerSeasonStageOption,
  PlayerTeamHistory,
} from './types'
import { EMPTY_HONOR_SUMMARY } from './types'

export function mapPublicPlayerProfile(
  detail: PublicPlayerDetailVo,
  options?: { divisionId?: string | null },
): PlayerProfile {
  const displayName = displayText(detail.displayName, translate('publicPages.player.unknownPlayer'))
  const currentTeams = (detail.currentTeams || []).map((item) => {
    const teamName = displayText(item.teamName, '--')
    const eventContext = [item.seasonName, item.divisionName].filter(Boolean).join(' · ')
    return {
      teamId: String(item.eventTeamId || item.teamId || ''),
      teamName,
      shortName: displayText(item.shortName, teamName).slice(0, 8),
      logoUrl: item.logoUrl?.trim() || '',
      eventContext: eventContext || translate('publicPages.player.currentEventTeam'),
      divisionId: item.divisionId ? String(item.divisionId) : '',
      seriesName: item.seriesName || '',
      seriesCode: item.seriesCode || '',
      seasonName: item.seasonName || '',
      divisionName: item.divisionName || '',
      platformCode: item.platformCode || '',
      gameMode: item.gameMode || '',
      eventTeamId: item.eventTeamId ? String(item.eventTeamId) : String(item.teamId || ''),
      disqualified: Boolean(item.disqualified),
      seasonId: item.seasonId ? String(item.seasonId) : '',
    }
  })
  const queryDivisionId = String(options?.divisionId || '').trim()
  const eventScopedTeams = currentTeams.filter((item) => item.eventTeamId)
  const primary = (queryDivisionId
    ? currentTeams.find((item) => String(item.divisionId) === queryDivisionId)
    : undefined) || eventScopedTeams[0] || currentTeams[0]
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
    platform: v4PlatformLabel(primary?.platformCode) || '--',
    currentStage: translate('publicPages.player.regular'),
    seriesName: primary?.seriesName || '',
    seriesCode: primary?.seriesCode || '',
    seasonName: primary?.seasonName || '',
    divisionName: primary?.divisionName || '',
    divisionId: primary?.divisionId || queryDivisionId,
    eventTeamId: primary?.eventTeamId || primary?.teamId || '',
    eventPlayerId: '',
    eventTeamDisqualified: Boolean(primary?.disqualified),
    seasonId: primary?.seasonId || '',
    activeDisciplineSummary: (detail.activeDisciplineSummary || []).filter((item) =>
      inPlayerEventScope(item, primary)),
    playerPenaltyTags: (detail.activeDisciplineSummary || [])
      .filter((item) => inPlayerEventScope(item, primary) && item.targetType === 'PLAYER')
      .map((item) => ({
        tagLabel: item.tagLabel || item.penaltyLabel || translate('publicPages.player.penalized'),
        penaltyLabel: item.penaltyLabel,
        statusLabel: item.statusLabel,
        effectiveStartTime: item.effectiveStartTime,
        description: item.description,
        publicReason: item.publicReason,
        seasonId: item.seasonId,
      })),
  }
}

function inPlayerEventScope(
  item: { seasonId?: string | number | null; divisionId?: string | number | null },
  primary?: { seasonId?: string; divisionId?: string },
): boolean {
  const seasonId = primary?.seasonId || ''
  const divisionId = primary?.divisionId || ''
  if (item.divisionId && divisionId && String(item.divisionId) !== divisionId) {
    return false
  }
  if (item.seasonId && seasonId && String(item.seasonId) !== seasonId) {
    return false
  }
  if ((seasonId || divisionId) && !item.seasonId && !item.divisionId) {
    return false
  }
  return true
}

export function mapPublicPlayerMatch(
  item: PublicMatchItemVo,
  playerTeamId: string,
  playerTeamName: string,
): PlayerMatchRecord {
  const isHome = String(item.homeTeamId || '') === String(playerTeamId)
  const opponentId = String((isHome ? item.awayTeamId : item.homeTeamId) || '')
  const opponentName = displayText(isHome ? item.awayTeamName : item.homeTeamName, translate('publicPages.player.opponent'))
  const opponentLogoUrl = (isHome ? item.awayTeamLogoUrl : item.homeTeamLogoUrl)?.trim() || ''
  const teamScore = isHome ? item.homeScore : item.awayScore
  const opponentScore = isHome ? item.awayScore : item.homeScore
  const status = String(item.matchStatus || '').toUpperCase()
  const resultType = String(item.resultType || '').toUpperCase()
  const cancelled = status === 'CANCELLED'
  const forfeited = status === 'FORFEITED' || resultType === 'FORFEIT'
  const dq = resultType === 'EVENT_DISQUALIFICATION'
  const finished = status === 'FINISHED' || forfeited || dq
  const winnerId = String(item.winnerTeamId || '')
  const playerSideId = String((isHome ? item.homeTeamId : item.awayTeamId) || '')
  const playerWon = Boolean(winnerId) && (winnerId === String(playerTeamId) || winnerId === playerSideId)
  const opponentWon = Boolean(winnerId) && !playerWon
  let result: PlayerMatchRecord['result'] = 'UNDECIDED'
  if (!cancelled && finished && playerWon) result = 'WIN'
  else if (!cancelled && finished && opponentWon) result = 'LOSS'
  const dt = formatDateTime(item.scheduledStartTime)
  return {
    id: String(item.matchId),
    matchId: String(item.matchId),
    date: dt.date === '--' ? '' : dt.date,
    tournamentName: displayText(item.groupName, translate('publicPages.player.publicEvent')),
    stageName: displayText(item.stageName, '--'),
    teamName: playerTeamName,
    teamId: playerTeamId,
    teamLogoUrl: (isHome ? item.homeTeamLogoUrl : item.awayTeamLogoUrl)?.trim() || '',
    opponentName,
    opponentId,
    opponentLogoUrl,
    result,
    resultLabel: result === 'UNDECIDED'
      ? (cancelled ? translate('match.cancelled') : translate('publicPages.player.undecided'))
      : dq
        ? (result === 'WIN' ? translate('publicPages.player.opponentDqLoss') : translate('publicPages.player.teamDqLoss'))
        : forfeited
          ? (result === 'WIN' ? translate('publicPages.player.judgedWin') : translate('publicPages.player.judgedLoss'))
          : undefined,
    originalTeamScore: (isHome ? item.originalHomeScore : item.originalAwayScore) ?? undefined,
    originalOpponentScore: (isHome ? item.originalAwayScore : item.originalHomeScore) ?? undefined,
    teamScore: teamScore ?? 0,
    opponentScore: opponentScore ?? 0,
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
  const season = displayText(row.seasonName || row.seriesName, translate('publicPages.player.publicSeason'))
  const division = displayText(row.divisionName, '')
  if (division && division !== '--' && !season.includes(division)) {
    return `${season} · ${division}`
  }
  return season
}

function nullableInt(value: number | string | null | undefined): number | null {
  if (value === null || value === undefined || value === '') return null
  const num = Number(value)
  return Number.isFinite(num) ? num : null
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
    fieldGoalMade: nullableInt(row.fieldGoalMade),
    fieldGoalAttempted: nullableInt(row.fieldGoalAttempt),
    threePointsMade: nullableInt(row.threePointMade),
    threePointsAttempted: nullableInt(row.threePointAttempt),
    freeThrowsMade: nullableInt(row.freeThrowMade),
    freeThrowsAttempted: nullableInt(row.freeThrowAttempt),
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
      stageLabel: displayText(row.divisionName, translate('publicPages.player.regular')),
      games: toNumber(row.games, 0),
    }
  })
}

function seasonPeriodLabel(row: Pick<PublicPlayerSeasonStatVo, 'seasonName' | 'seriesName'>): string {
  return displayText(row.seasonName || row.seriesName, '')
}

function formatCareerPeriod(labels: string[], current: boolean): string {
  const start = labels[0] || ''
  const end = labels[labels.length - 1] || ''
  if (!start && current) return translate('publicPages.player.present')
  if (!start) return '--'
  if (current) return `${start} — ${translate('publicPages.player.present')}`
  if (end && end !== start) return `${start} — ${end}`
  return start
}

export function buildPlayerTeamHistory(
  profile: PlayerProfile,
  rows: PublicPlayerSeasonStatVo[] = [],
): PlayerTeamHistory[] {
  type Stint = {
    teamId: string
    teamName: string
    teamLogoUrl: string
    divisionId?: string
    labels: string[]
    current: boolean
  }
  const byKey = new Map<string, Stint>()
  const sorted = [...rows].sort((a, b) =>
    String(a.seasonName || a.seasonId || '').localeCompare(String(b.seasonName || b.seasonId || '')),
  )
  for (const row of sorted) {
    const teamName = displayText(row.teamName, '')
    const teamId = String(row.eventTeamId || row.teamId || '')
    if (!teamId && (!teamName || teamName === '--')) continue
    const key = (teamName && teamName !== '--' ? teamName : teamId).toLowerCase()
    const label = seasonPeriodLabel(row)
    const existing = byKey.get(key)
    if (existing) {
      if (label && !existing.labels.includes(label)) existing.labels.push(label)
      if (!existing.teamLogoUrl && row.teamLogoUrl) existing.teamLogoUrl = row.teamLogoUrl.trim()
      continue
    }
    byKey.set(key, {
      teamId: teamId || profile.eventTeamId || profile.currentTeamId,
      teamName: teamName || profile.currentTeamName || '--',
      teamLogoUrl: row.teamLogoUrl?.trim() || '',
      divisionId: row.divisionId ? String(row.divisionId) : '',
      labels: label ? [label] : [],
      current: false,
    })
  }

  const currentTeamId = profile.eventTeamId || profile.currentTeamId
  const currentName = profile.currentTeamName
  const hasCurrent = [...byKey.values()].some((item) =>
    item.teamId === currentTeamId
    || item.teamId === profile.currentTeamId
    || (currentName && currentName !== '--' && item.teamName === currentName),
  )
  if (!hasCurrent && (currentTeamId || (currentName && currentName !== '--'))) {
    byKey.set(currentTeamId || currentName, {
      teamId: currentTeamId,
      teamName: currentName || '--',
      teamLogoUrl: profile.currentTeamLogoUrl,
      divisionId: profile.divisionId,
      labels: profile.seasonName ? [profile.seasonName] : [],
      current: true,
    })
  }

  const list = [...byKey.values()]
  for (const item of list) {
    item.current = Boolean(
      item.teamId
      && (item.teamId === profile.eventTeamId || item.teamId === profile.currentTeamId),
    ) || Boolean(currentName && currentName !== '--' && item.teamName === currentName)
  }
  if (!list.some((item) => item.current) && list.length) {
    list[list.length - 1].current = true
  }

  return list.map((item, index) => {
    let description = translate('publicPages.player.participatingTeam')
    if (item.current) description = translate('publicPages.player.currentRosterTeam')
    else if (index === 0) description = translate('publicPages.player.firstTeam')
    return {
      id: index + 1,
      startDate: item.labels[0] || '--',
      endDate: item.current ? translate('publicPages.player.present') : (item.labels[item.labels.length - 1] || '--'),
      period: formatCareerPeriod(item.labels, item.current),
      teamId: item.teamId,
      teamName: item.teamName,
      teamLogoUrl: item.teamLogoUrl,
      divisionId: item.divisionId,
      description,
      current: item.current,
    }
  })
}

export function buildPlayerNameHistory(
  profile: PlayerProfile,
  rows: PublicPlayerSeasonStatVo[] = [],
): PlayerNameHistory[] {
  type Entry = { playerName: string; labels: string[]; current: boolean }
  const entries: Entry[] = []
  const sorted = [...rows].sort((a, b) =>
    String(a.seasonName || a.seasonId || '').localeCompare(String(b.seasonName || b.seasonId || '')),
  )
  for (const row of sorted) {
    const playerName = displayText(row.playerName, '')
    if (!playerName || playerName === '--') continue
    const label = seasonPeriodLabel(row)
    const last = entries[entries.length - 1]
    if (last && last.playerName === playerName) {
      if (label && !last.labels.includes(label)) last.labels.push(label)
      continue
    }
    entries.push({
      playerName,
      labels: label ? [label] : [],
      current: false,
    })
  }

  const currentName = displayText(profile.displayName, translate('publicPages.player.unknownPlayer'))
  if (!entries.some((item) => item.playerName === currentName)) {
    entries.push({
      playerName: currentName,
      labels: profile.seasonName ? [profile.seasonName] : [],
      current: true,
    })
  }
  if (!entries.length) {
    entries.push({
      playerName: currentName,
      labels: [],
      current: true,
    })
  }

  entries.forEach((item) => {
    item.current = item.playerName === currentName
  })
  if (!entries.some((item) => item.current)) {
    entries[entries.length - 1].current = true
  }

  return entries.map((item, index) => {
    let description = translate('publicPages.player.formerName')
    if (item.current) description = translate('publicPages.player.currentName')
    else if (index === 0) description = translate('publicPages.player.firstRegisteredName')
    return {
      id: index + 1,
      startDate: item.labels[0] || '--',
      endDate: item.current ? translate('publicPages.player.present') : (item.labels[item.labels.length - 1] || '--'),
      period: formatCareerPeriod(item.labels, item.current),
      playerName: item.playerName,
      description,
      current: item.current,
    }
  })
}

export function mapPublicPlayerHonors(rows: PublicHonorItemResp[]): PlayerHonor[] {
  return rows.map((item) => {
    const honorDate = formatDateTime(item.awardDate).date
    return {
      id: String(item.honorId),
      seasonName: displayText(item.tournamentName || item.seasonName, translate('publicPages.player.publicSeason')),
      honorName: displayText(item.honorTitle || item.honorName, translate('publicPages.player.playerHonor')),
      honorDate: honorDate === '--' ? '' : honorDate,
      honorValue: displayText(item.honorValue || item.honorSubtitle, ''),
      honorCode: item.honorCode || '',
      imageUrl: item.coverUrl?.trim() || '',
      tournamentId: item.tournamentId ? String(item.tournamentId) : undefined,
    }
  })
}

export function findPlayerBoxScore(
  stats: V4PublicMatchPlayerStatItem[] | undefined,
  profile: Pick<PlayerProfile, 'id' | 'displayName' | 'eventTeamId' | 'eventPlayerId'>,
): V4PublicMatchPlayerStatItem | undefined {
  const list = stats || []
  const bySource = list.find((item) => String(item.sourcePlayerId || '') === String(profile.id))
  if (bySource) return bySource
  if (profile.eventPlayerId) {
    const byEvent = list.find((item) => String(item.eventPlayerId || '') === String(profile.eventPlayerId))
    if (byEvent) return byEvent
  }
  return list.find((item) => {
    const sameName = String(item.playerName || '') === profile.displayName
    if (!sameName) return false
    if (!profile.eventTeamId) return true
    return String(item.eventTeamId || '') === String(profile.eventTeamId)
  })
}

export function applyPlayerBoxScore(
  row: PlayerMatchRecord,
  stat?: V4PublicMatchPlayerStatItem,
): PlayerMatchRecord {
  if (!stat) return row
  return {
    ...row,
    minutes: stat.minutes == null ? row.minutes : toNumber(stat.minutes, row.minutes),
    points: toNumber(stat.points, row.points),
    rebounds: toNumber(stat.rebound, row.rebounds),
    assists: toNumber(stat.assist, row.assists),
    steals: toNumber(stat.steal, row.steals),
    blocks: toNumber(stat.block, row.blocks),
    turnovers: toNumber(stat.turnover, row.turnovers),
    fouls: stat.foul == null ? row.fouls : toNumber(stat.foul, row.fouls),
    fieldGoalsMade: stat.fieldGoalMade ?? row.fieldGoalsMade,
    fieldGoalsAttempted: stat.fieldGoalAttempt ?? row.fieldGoalsAttempted,
    threePointsMade: stat.threePointMade ?? row.threePointsMade,
    threePointsAttempted: stat.threePointAttempt ?? row.threePointsAttempted,
    freeThrowsMade: stat.freeThrowMade ?? row.freeThrowsMade,
    freeThrowsAttempted: stat.freeThrowAttempt ?? row.freeThrowsAttempted,
  }
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
    nameHistory: buildPlayerNameHistory(profile),
  }
}
