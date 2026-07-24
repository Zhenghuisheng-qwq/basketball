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
  teamKey: string
  logoColor: string
  position: string
  platform: string
  currentStage: string
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
  teamScore: number
  opponentScore: number
  minutes: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
  fieldGoalsMade: number
  fieldGoalsAttempted: number
  threePointsMade: number
  threePointsAttempted: number
  freeThrowsMade: number
  freeThrowsAttempted: number
}

export interface PlayerCareerStatistics {
  seasonId: string
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
  fieldGoalPercentage: number
  threePointPercentage: number
  freeThrowPercentage: number
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
