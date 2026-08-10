export type EventTab = 'overview' | 'schedule' | 'stage'

export type StageType = 'regular' | 'playoffs'

export type MatchStatus = 'scheduled' | 'live' | 'finished' | 'cancelled'

export interface Team {
  id: number
  /** V2 EventTeam 主键 */
  teamId: string
  code: string
  name: string
  shortName: string
  logo: string
  badgeKey?: string
  wins: number
  losses: number
}

export interface PlayerLeader {
  id: number
  title: string
  value: number
  unit: 'PPG' | 'APG' | 'RPG'
  playerName: string
  teamId: string
  teamName?: string
  teamLogo?: string
  portrait: string
}

export interface Match {
  id: number
  matchId: string
  season: string
  date: string
  startTime: string
  /** @deprecated 兼容原型数字 id */
  homeTeamId: number
  /** @deprecated 兼容原型数字 id */
  awayTeamId: number
  homeTeamKey: string
  awayTeamKey: string
  homeTeamName: string
  awayTeamName: string
  homeTeamLogo: string
  awayTeamLogo: string
  homeScore?: number
  awayScore?: number
  venue: string
  status: MatchStatus
  week: number
  matchStatus?: number
  stageName?: string
}

export interface Standing {
  rank: number
  teamId: string
  teamName: string
  teamLogo: string
  wins: number
  losses: number
  winRate: number
  pointDiff: number
  recent: Array<'W' | 'L'>
}

export interface CompetitionStage {
  key: string
  index: string
  name: string
  period: string
  fields: Array<{
    label: string
    value: string
  }>
}

export interface PlayoffSeries {
  id: string
  round: 'quarterfinal' | 'semifinal' | 'final'
  side: 'left' | 'right' | 'center'
  topTeamId: string
  bottomTeamId: string
  topTeamName: string
  bottomTeamName: string
  topTeamLogo: string
  bottomTeamLogo: string
  topScore: number
  bottomScore: number
  winnerTeamId: string
  topSeed?: number | string
  bottomSeed?: number | string
}

export interface LeagueInfo {
  name: string
  tag: string
  description: string
  season: string
  seasonPeriod: string
  teamCount: string
  teamCountDesc: string
  currentStage: string
  platform: string
}

export interface LeagueSummary {
  seasonName: string
  seasonPeriod: string
  teamCount: number
  teamDescription: string
  currentStageName: string
  platformName: string
}

export type LeagueHeroMetaIcon = 'calendar' | 'trophy' | 'stage' | 'gamepad'

export interface LeagueHeroMetaItem {
  key: 'season' | 'teams' | 'stage' | 'platform'
  icon: LeagueHeroMetaIcon
  title: string
  value: string
}

export interface CommunityLink {
  id: string
  title: string
  description: string
  icon: 'kook' | 'qq' | 'bilibili' | 'douyin'
  url: string
}

export interface CalendarDay {
  date: string
  day: number
  isCurrentMonth: boolean
  isWeekend: boolean
  isSelected: boolean
}

export interface CalendarMonth {
  year: number
  month: number
  label: string
  days: CalendarDay[]
}
