/** 官网公开接口 VO，字段对齐 basketball-web/.../response/publicapi */

export interface PublicMatchItemVo {
  matchId: string
  matchNo?: string | null
  seasonId?: string | null
  stageId?: string | null
  stageName?: string | null
  groupId?: string | null
  groupName?: string | null
  homeTeamId?: string | null
  homeTeamName?: string | null
  homeTeamLogoUrl?: string | null
  awayTeamId?: string | null
  awayTeamName?: string | null
  awayTeamLogoUrl?: string | null
  homeScore?: number | null
  awayScore?: number | null
  matchStatus?: number | null
  dataStatus?: number | null
  venueName?: string | null
  scheduledStartTime?: string | null
  actualEndTime?: string | null
}

export interface PublicMatchTeamStatVo {
  teamId?: string | null
  teamName?: string | null
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  turnovers?: number | null
  fouls?: number | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
}

export interface PublicMatchPlayerStatVo {
  playerId?: string | null
  playerName?: string | null
  avatarUrl?: string | null
  teamId?: string | null
  teamName?: string | null
  startedFlag?: number | null
  minutesSeconds?: number | null
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  turnovers?: number | null
  fouls?: number | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
  efficiency?: number | string | null
}

export interface PublicMatchDetailVo extends PublicMatchItemVo {
  seasonName?: string | null
  winnerTeamId?: string | null
  statsAvailable?: boolean | null
  actualStartTime?: string | null
  teamStats?: PublicMatchTeamStatVo[] | null
  playerStats?: PublicMatchPlayerStatVo[] | null
}

export interface PublicTeamRankItemVo {
  seasonId?: string | null
  rankType?: string | null
  rankTypeName?: string | null
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  rankNo?: number | null
  previousRank?: number | null
  statValue?: number | string | null
  snapshotDate?: string | null
}

export interface PublicPlayerRankItemVo {
  seasonId?: string | null
  rankType?: string | null
  rankTypeName?: string | null
  playerId: string
  playerName?: string | null
  avatarUrl?: string | null
  teamId?: string | null
  teamName?: string | null
  rankNo?: number | null
  previousRank?: number | null
  statValue?: number | string | null
  snapshotDate?: string | null
}

export interface PublicSeasonSummaryVo {
  seasonId?: string | null
  seasonName?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  seasonStatus?: number | null
  teamCount?: number | null
  finishedMatchCount?: number | null
  standingCount?: number | null
  teamStatCount?: number | null
  playerStatCount?: number | null
}

export interface PublicHomeVo {
  recentMatches?: PublicMatchItemVo[] | null
  teamRankings?: PublicTeamRankItemVo[] | null
  playerRankings?: PublicPlayerRankItemVo[] | null
  seasonSummary?: PublicSeasonSummaryVo | null
  hotTeamSourceNote?: string | null
}

export interface PublicTournamentItemVo {
  tournamentId: string
  tournamentNo?: string | null
  tournamentName?: string | null
  shortName?: string | null
  tournamentType?: string | null
  gameTerminal?: string | null
  formatCode?: string | null
  organizerName?: string | null
  tournamentStatus?: number | null
  logoUrl?: string | null
  coverUrl?: string | null
  displayOrder?: number | null
  updateTime?: string | null
}

export interface PublicTournamentDetailVo {
  tournamentId: string
  tournamentNo?: string | null
  tournamentName?: string | null
  shortName?: string | null
  englishName?: string | null
  tournamentType?: string | null
  gameTerminal?: string | null
  formatCode?: string | null
  organizerName?: string | null
  description?: unknown
  tournamentStatus?: number | null
  logoUrl?: string | null
  coverUrl?: string | null
}

export interface PublicSeasonItemVo {
  seasonId: string
  seasonNo?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  seasonName?: string | null
  seasonYear?: string | null
  seasonStatus?: number | null
  registrationStartTime?: string | null
  registrationEndTime?: string | null
  seasonStartTime?: string | null
  seasonEndTime?: string | null
  maxTeamCount?: number | null
}

export interface PublicSeasonOverviewVo {
  seasonId: string
  seasonName?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  seasonStatus?: number | null
  teamCount?: number | null
  finishedMatchCount?: number | null
  publicMatchCount?: number | null
  standingCount?: number | null
  description?: unknown
}

export interface PublicStandingItemVo {
  standingId?: string | null
  seasonId?: string | null
  stageId?: string | null
  stageName?: string | null
  groupId?: string | null
  groupName?: string | null
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  rankNo?: number | null
  matchesPlayed?: number | null
  wins?: number | null
  losses?: number | null
  pointsFor?: number | null
  pointsAgainst?: number | null
  pointDiff?: number | null
  standingPoints?: number | string | null
  winRate?: number | string | null
  streakType?: string | null
  streakCount?: number | null
  qualifiedFlag?: number | null
}

export interface PublicTeamSeasonStatVo {
  seasonId?: string | null
  stageId?: string | null
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  games?: number | null
  wins?: number | null
  losses?: number | null
  pointsFor?: number | null
  pointsAgainst?: number | null
  avgPoints?: number | string | null
  avgPointsAgainst?: number | string | null
  avgRebounds?: number | string | null
  avgAssists?: number | string | null
  avgSteals?: number | string | null
  avgBlocks?: number | string | null
  avgTurnovers?: number | string | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
  winRate?: number | string | null
}

export interface PublicPlayerSeasonStatVo {
  seasonId?: string | null
  stageId?: string | null
  teamId?: string | null
  teamName?: string | null
  playerId: string
  playerName?: string | null
  avatarUrl?: string | null
  games?: number | null
  starts?: number | null
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  avgMinutes?: number | string | null
  avgPoints?: number | string | null
  avgRebounds?: number | string | null
  avgAssists?: number | string | null
  avgSteals?: number | string | null
  avgBlocks?: number | string | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
  avgEfficiency?: number | string | null
}

export interface PublicTeamDetailVo {
  teamId: string
  teamNo?: string | null
  teamName?: string | null
  shortName?: string | null
  englishName?: string | null
  logoUrl?: string | null
  regionCode?: string | null
  regionName?: string | null
  gameTerminal?: string | null
  description?: unknown
  teamStatus?: number | null
  currentMemberCount?: number | null
  establishedDate?: string | null
}

export interface PublicTeamRosterItemVo {
  playerId: string
  playerName?: string | null
  avatarUrl?: string | null
  positionCode?: string | null
  jerseyNo?: string | null
  roleType?: string | null
  rosterType?: string | null
  certificationStatus?: number | null
}

export interface PublicTeamSeasonHistoryVo {
  seasonId: string
  seasonName?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  displayName?: string | null
  finalRank?: number | null
  seasonStatus?: number | null
  games?: number | null
  wins?: number | null
  losses?: number | null
  winRate?: number | string | null
}

export interface PublicTeamCompareVo {
  leftTeam?: PublicTeamDetailVo | null
  rightTeam?: PublicTeamDetailVo | null
  leftStat?: PublicTeamSeasonStatVo | null
  rightStat?: PublicTeamSeasonStatVo | null
  seasonId?: string | null
  seasonName?: string | null
}

export interface PublicPlayerDetailVo {
  playerId: string
  playerNo?: string | null
  displayName?: string | null
  avatarUrl?: string | null
  positionCode?: string | null
  dominantHand?: string | null
  heightCm?: number | string | null
  weightKg?: number | string | null
  currentTeamId?: string | null
  currentTeamName?: string | null
  certificationStatus?: number | null
  playerStatus?: number | null
  profilePublic?: boolean | null
  privacyMessage?: string | null
}
