export type TeamStage = 'regular' | 'playoffs'

export type TeamMatchStatus = 'scheduled' | 'finished'

export interface TeamBase {
  id: string
  code: string
  name: string
  shortName: string
  logoUrl: string
  largeLogoUrl: string
  captainName: string
  platform: string
  currentStage: TeamStage
  rosterCount: number
  verified: boolean
  seasonName: string
  seasonYear: number
  description: string
  wins: number
  losses: number
  ranking: number
  themeColor: string
  secondaryColor: string
}

export type TeamMatchResult = 'win' | 'loss'

export type TeamSubTab = 'overview' | 'matches' | 'stats' | 'archive'

export type PlayerStatsMode = 'average' | 'total'

export type PlayoffStatsMode = 'summary' | 'single'

export interface TeamDetail {
  id: string
  code: string
  name: string
  shortName: string
  logo: string
  largeLogo: string
  badgeKey?: string
  eventTeamId?: number
  captainName: string
  platform: string
  currentStage: string
  rosterCount: number
  verified: boolean
  seasonName: string
  seasonYear: number
  description: string
  wins: number
  losses: number
  ranking: number
  themeColor: string
  secondaryColor: string
}

export interface TeamRosterPlayer {
  id: string
  teamId: string
  nickname: string
  position: string
  avatar: string
  isCaptain?: boolean
}

export interface TeamMatchRecord {
  id: string
  teamId: string
  opponentId: string
  opponentName?: string
  opponentLogoUrl?: string
  date: string
  time: string
  teamScore?: number
  opponentScore?: number
  result?: TeamMatchResult
  status: TeamMatchStatus
  stage: TeamStage
  competition: string
  playoffGame?: string
}

export interface TeamSeasonAverages {
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  fouls: number
  turnovers: number
}

export interface TeamPlayerAverageStat {
  playerId: string
  nickname: string
  avatar: string
  games: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  fouls: number
  turnovers: number
  fgPct: number
  tpPct: number
  ftPct: number
}

export interface TeamPlayerTotalStat extends Omit<TeamPlayerAverageStat, 'games'> {
  games: number
}

export interface TeamSingleMatchPlayerStat {
  playerId: string
  nickname: string
  avatar: string
  grade: string
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  fouls: number
  turnovers: number
  fg: string
  tp: string
  ft: string
}

export interface TeamSingleMatchTotal {
  grade: string
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  fouls: number
  turnovers: number
  fg: string
  tp: string
  ft: string
}

export interface TeamSingleMatchStats {
  matchId: string
  label: string
  players: TeamSingleMatchPlayerStat[]
  teamTotal: TeamSingleMatchTotal
}

export interface TeamHonor {
  id: string
  competitionName: string
  honorTitle: string
  date: string
  isLatest?: boolean
}

export interface TeamNameHistoryItem {
  id: string
  period: string
  name: string
  note: string
  isCurrent?: boolean
}

export interface TeamLogoHistoryItem {
  id: string
  period: string
  label: string
  logo: string
  isCurrent?: boolean
}

export interface TeamArchiveData {
  honors: TeamHonor[]
  nameHistory: TeamNameHistoryItem[]
  logoHistory: TeamLogoHistoryItem[]
}

export interface TeamCompareOverviewStat {
  winRate: number
  avgPoints: number
  avgPointsAgainst: number
  avgPointDiff: number
}

export interface TeamCompareBarStat {
  key: string
  label: string
  left: number
  right: number
  higherIsBetter: boolean
}

export interface TeamCompareData {
  overview: TeamCompareOverviewStat
  basics: TeamCompareBarStat[]
  shooting: TeamCompareBarStat[]
}

export interface TeamFilterOption {
  value: string
  label: string
}
