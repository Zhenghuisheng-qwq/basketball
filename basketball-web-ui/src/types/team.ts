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
  seriesName?: string
  seriesCode?: string
  divisionName?: string
  disqualified?: boolean
  eventTeamStatus?: string
  activeDisciplineSummary?: import('@/api/types/v4Public').PublicActiveDisciplineSummary[]
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
  stageHeadline?: string
  stageMeta?: string
  playoffGame?: string
  resultType?: string
  resultReason?: string
  resultLabel?: string
  originalTeamScore?: number
  originalOpponentScore?: number
  seriesScoreText?: string
  matchSeriesId?: string
  bestOf?: number
}

export interface TeamSeasonAverages {
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  /** null = 当前 Public Team Stat 未提供犯规 */
  fouls: number | null
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
  /** null = 当前 Public Player Stat 未提供犯规 */
  fouls: number | null
  turnovers: number
  /** null = 无投篮数据源 */
  fgPct: number | null
  tpPct: number | null
  ftPct: number | null
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
  /** null = 单场接口未提供犯规 */
  fouls: number | null
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
  fouls: number | null
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
