export type HallTab = 'weekly' | 'event' | 'champions'

export type TournamentSeriesCode = 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'

export type TournamentPlatformCode =
  | 'CONSOLE'
  | 'PC'
  | 'MOBILE'
  | 'PS5'
  | 'XBOX'
  | 'CROSS_PLATFORM'

export type TournamentStatus = 'UPCOMING' | 'ONGOING' | 'FINISHED'

export type PlayerPosition = 'PG' | 'SG' | 'SF' | 'PF' | 'C'

export type RosterRole = 'STARTER' | 'SUBSTITUTE'

export interface HallTournament {
  id: string
  seasonYear: number
  seasonName: string
  fullName: string
  shortName: string
  seriesCode: TournamentSeriesCode
  seriesName: string
  platformCode: TournamentPlatformCode
  platformName: string
  startDate: string
  endDate: string
  status: TournamentStatus
  published: boolean
  hasWeeklyBest: boolean
}

export interface TournamentWeek {
  id: string
  tournamentId: string
  weekNo: number
  weekName: string
  startDate: string
  endDate: string
  published: boolean
}

export interface WeeklyBestRecord {
  id: string
  tournamentId: string
  weekId: string
  weekNo: number
  playerId: number
  playerName: string
  playerAvatarUrl: string
  teamId: string
  teamName: string
  teamAbbr: string
  teamLogoUrl: string
  games: number
  averagePoints: number
  averageRebounds: number
  averageAssists: number
  averageSteals: number
  fieldGoalPercentage: number
  published: boolean
}

export interface ChampionRosterMember {
  id: string
  tournamentId: string
  teamId: string
  playerId: number
  playerName: string
  playerAvatarUrl: string
  position: PlayerPosition
  role: RosterRole
  captain: boolean
  appearances: number
  sortOrder: number
}

export interface HonorPlayer {
  playerId: number
  playerName: string
  playerAvatarUrl: string
  teamId: string
  teamName: string
  teamAbbr: string
  teamLogoUrl: string
  position: PlayerPosition
}

export interface EventMvpRecord extends HonorPlayer {
  tournamentId: string
  averagePoints: number
  averageRebounds: number
  averageAssists: number
  averageSteals: number
  fieldGoalPercentage: number
  published: boolean
}

export interface EventHonorRecord {
  tournamentId: string
  championTeamId: string
  championTeamName: string
  championTeamAbbr: string
  championTeamLogoUrl: string
  championDate: string
  trophyImageUrl: string
  description: string
  published: boolean
  mvp: EventMvpRecord | null
  allTournamentTeam: HonorPlayer[]
  allDefensiveTeam: HonorPlayer[]
  allTournamentPublished: boolean
  allDefensivePublished: boolean
  championRoster: ChampionRosterMember[]
}

export interface ChampionRecord {
  id: string
  tournamentId: string
  seasonYear: number
  seriesCode: TournamentSeriesCode
  seriesName: string
  tournamentName: string
  platformCode: TournamentPlatformCode
  platformName: string
  championDate: string
  teamId: string
  teamName: string
  teamAbbr: string
  teamLogoUrl: string
}

export interface ChampionFilterOptions {
  years: Array<{ value: string; label: string }>
  platforms: Array<{ value: string; label: string }>
  series: Array<{ value: string; label: string }>
}

export interface ChampionPageResult {
  items: ChampionRecord[]
  total: number
  totalPages: number
  page: number
  pageSize: number
  groupedByYear: Array<{ year: number; items: ChampionRecord[] }>
}
