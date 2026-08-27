export interface PlayerCurrentTeam {
  teamId: string
  teamName: string
  shortName: string
  logoUrl: string
  eventContext: string
  divisionId?: string
  seriesName?: string
  seriesCode?: string
  seasonName?: string
  divisionName?: string
  platformCode?: string
  gameMode?: string
  eventTeamId?: string
  disqualified?: boolean
  seasonId?: string
}

export interface PlayerProfile {
  id: string
  playerName: string
  displayName: string
  avatarUrl: string
  badgeUrl: string
  verified: boolean
  registrationText: string
  currentTeamId: string
  currentTeamName: string
  currentTeamAbbr: string
  currentTeamLogoUrl: string
  currentTeams: PlayerCurrentTeam[]
  teamKey: string
  logoColor: string
  position: string
  platform: string
  currentStage: string
  seriesName?: string
  seriesCode?: string
  seasonName?: string
  divisionName?: string
  divisionId?: string
  eventTeamId?: string
  eventPlayerId?: string
  eventTeamDisqualified?: boolean
  seasonId?: string
  playerPenaltyTags?: Array<{
    tagLabel: string
    penaltyLabel?: string
    statusLabel?: string
    effectiveStartTime?: string
    description?: string
    publicReason?: string | null
    seasonId?: string
  }>
  activeDisciplineSummary?: import('@/api/types/v4Public').PublicActiveDisciplineSummary[]
}

export interface PlayerHonorSummary {
  appearances: number
  weeklyBestCount: number
  allStarCount: number
  bestLineupCount: number
  championCount: number
  runnerUpCount: number
  thirdPlaceCount: number
}

export interface PlayerHonor {
  id: string
  seasonName: string
  honorName: string
  honorDate: string
  imageUrl: string
  tournamentId?: string
}

export interface PlayerMatchRecord {
  id: string
  matchId: string
  date: string
  tournamentName: string
  stageName: string
  teamName: string
  teamId: string
  teamLogoUrl: string
  opponentName: string
  opponentId: string
  opponentLogoUrl: string
  result: 'WIN' | 'LOSS'
  resultLabel?: string
  teamScore: number
  opponentScore: number
  originalTeamScore?: number
  originalOpponentScore?: number
  seriesScoreText?: string
  matchSeriesId?: string
  minutes: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
  fieldGoalsMade: number | null
  fieldGoalsAttempted: number | null
  threePointsMade: number | null
  threePointsAttempted: number | null
  freeThrowsMade: number | null
  freeThrowsAttempted: number | null
}

export interface PlayerCareerStatistics {
  seasonId: string
  divisionId: string
  tournamentName: string
  teamId: string
  teamName: string
  teamLogoUrl: string
  platform: string
  games: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
  /** null = 无投篮数据源，页面显示 --；0 = 真实 0% */
  fieldGoalPercentage: number | null
  threePointPercentage: number | null
  freeThrowPercentage: number | null
}

export interface PlayerSeasonStageOption {
  id: string
  label: string
  tournamentName: string
  stageLabel: string
  games: number
}

export interface PlayerTeamHistory {
  id: number
  startDate: string
  endDate: string
  teamId: string
  teamName: string
  teamLogoUrl: string
  description: string
  current: boolean
}

export interface PlayerNameHistory {
  id: number
  startDate: string
  endDate: string
  playerName: string
  description: string
  current: boolean
}

export interface PlayerDetailData {
  profile: PlayerProfile
  honorSummary: PlayerHonorSummary
  honors: PlayerHonor[]
  recentMatches: PlayerMatchRecord[]
  allMatches: PlayerMatchRecord[]
  careerStats: PlayerCareerStatistics[]
  seasonStageOptions: PlayerSeasonStageOption[]
  teamHistory: PlayerTeamHistory[]
  nameHistory: PlayerNameHistory[]
}

export const EMPTY_HONOR_SUMMARY: PlayerHonorSummary = {
  appearances: 0,
  weeklyBestCount: 0,
  allStarCount: 0,
  bestLineupCount: 0,
  championCount: 0,
  runnerUpCount: 0,
  thirdPlaceCount: 0,
}
