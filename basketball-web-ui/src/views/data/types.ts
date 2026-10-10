import { translate } from '@/i18n'

export type DataType = 'team' | 'player'
export type StatisticType = 'average' | 'total'

export interface TournamentOption {
  id: string
  name: string
  season: string
  label: string
}

export interface TournamentGroup {
  season: string
  tournaments: TournamentOption[]
}

export interface TableColumn {
  key: string
  label: string
  align?: 'left' | 'center'
  width?: string
  cellClass?: string
}

export interface TeamStatistics {
  id: string
  rank: number
  teamId: string
  teamName: string
  teamAbbr: string
  logoUrl: string
  logoColor: string
  games: number
  wins: number
  losses: number
  winRate: number
  averagePoints: number
  averageRebounds: number
  averageAssists: number
  averageSteals: number
  averageBlocks: number
  averageTurnovers: number
  totalPoints: number
  totalRebounds: number
  totalAssists: number
  totalSteals: number
  totalBlocks: number
  totalTurnovers: number
  fieldGoalPct: number | null
  threePointPct: number | null
  freeThrowPct: number | null
}

export interface PlayerStatistics {
  id: string
  playerId: string
  rank: number
  playerName: string
  teamId: string
  teamName: string
  teamAbbr: string
  teamLogoUrl: string
  logoColor: string
  position: string
  games: number
  averagePoints: number
  averageRebounds: number
  averageAssists: number
  averageSteals: number
  averageBlocks: number
  averageTurnovers: number
  totalPoints: number
  totalRebounds: number
  totalAssists: number
  totalSteals: number
  totalBlocks: number
  totalTurnovers: number
  fieldGoalPct: number | null
  threePointPct: number | null
  freeThrowPct: number | null
}

export function formatRate(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return '--'
  const pct = num <= 1 ? num * 100 : num
  return `${pct.toFixed(1)}%`
}

export function formatWinLoss(wins: number, losses: number): string {
  return `${wins}/${losses}`
}

export function formatStatNumber(value: number | string, average: boolean): string {
  const num = Number(value)
  if (Number.isNaN(num)) return '0'
  if (average) return Number.isInteger(num) ? String(num) : num.toFixed(1)
  return String(Math.round(num))
}

function dataColumn(key: string): string {
  return translate(`publicPages.data.columns.${key}`)
}

export function getTeamColumns(statisticType: StatisticType): TableColumn[] {
  const isAverage = statisticType === 'average'
  return [
    { key: 'rank', label: dataColumn('rank'), width: '60px' },
    { key: 'team', label: dataColumn('team'), align: 'left', width: '145px', cellClass: 'is-team' },
    { key: 'games', label: dataColumn('games'), width: '70px' },
    { key: 'winLoss', label: dataColumn('winLoss'), width: '70px' },
    { key: 'winRate', label: dataColumn('winRate'), width: '72px' },
    { key: 'points', label: dataColumn(isAverage ? 'avgPoints' : 'totalPoints'), width: '78px' },
    { key: 'rebounds', label: dataColumn(isAverage ? 'avgRebounds' : 'totalRebounds'), width: '78px' },
    { key: 'assists', label: dataColumn(isAverage ? 'avgAssists' : 'totalAssists'), width: '78px' },
    { key: 'steals', label: dataColumn(isAverage ? 'avgSteals' : 'totalSteals'), width: '78px' },
    { key: 'blocks', label: dataColumn(isAverage ? 'avgBlocks' : 'totalBlocks'), width: '78px' },
    { key: 'turnovers', label: dataColumn(isAverage ? 'avgTurnovers' : 'totalTurnovers'), width: '78px' },
    { key: 'fieldGoals', label: dataColumn('fgPct'), width: '72px' },
    { key: 'threePoints', label: dataColumn('tpPct'), width: '72px' },
    { key: 'freeThrows', label: dataColumn('ftPct'), width: '72px' },
  ]
}

export function getPlayerColumns(statisticType: StatisticType): TableColumn[] {
  const isAverage = statisticType === 'average'
  return [
    { key: 'rank', label: dataColumn('rank'), width: '60px' },
    { key: 'player', label: dataColumn('player'), align: 'left', width: '165px', cellClass: 'is-player' },
    { key: 'team', label: dataColumn('team'), align: 'left', width: '145px', cellClass: 'is-team' },
    { key: 'position', label: dataColumn('position'), width: '70px' },
    { key: 'games', label: dataColumn('games'), width: '70px' },
    { key: 'points', label: dataColumn(isAverage ? 'avgPoints' : 'totalPoints'), width: '78px' },
    { key: 'rebounds', label: dataColumn(isAverage ? 'avgRebounds' : 'totalRebounds'), width: '78px' },
    { key: 'assists', label: dataColumn(isAverage ? 'avgAssists' : 'totalAssists'), width: '78px' },
    { key: 'steals', label: dataColumn(isAverage ? 'avgSteals' : 'totalSteals'), width: '78px' },
    { key: 'blocks', label: dataColumn(isAverage ? 'avgBlocks' : 'totalBlocks'), width: '78px' },
    { key: 'fieldGoals', label: dataColumn('fgPct'), width: '72px' },
    { key: 'threePoints', label: dataColumn('tpPct'), width: '72px' },
    { key: 'freeThrows', label: dataColumn('ftPct'), width: '72px' },
  ]
}
