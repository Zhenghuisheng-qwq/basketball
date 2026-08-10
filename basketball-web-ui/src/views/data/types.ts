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

export function getTeamColumns(statisticType: StatisticType): TableColumn[] {
  const isAverage = statisticType === 'average'
  return [
    { key: 'rank', label: '排名', width: '60px' },
    { key: 'team', label: '战队', align: 'left', width: '145px', cellClass: 'is-team' },
    { key: 'games', label: '场次', width: '70px' },
    { key: 'winLoss', label: '胜/负', width: '70px' },
    { key: 'winRate', label: '胜率', width: '72px' },
    { key: 'points', label: isAverage ? '场均得分' : '总得分', width: '78px' },
    { key: 'rebounds', label: isAverage ? '场均篮板' : '总篮板', width: '78px' },
    { key: 'assists', label: isAverage ? '场均助攻' : '总助攻', width: '78px' },
    { key: 'steals', label: isAverage ? '场均抢断' : '总抢断', width: '78px' },
    { key: 'blocks', label: isAverage ? '场均盖帽' : '总盖帽', width: '78px' },
    { key: 'turnovers', label: isAverage ? '场均失误' : '总失误', width: '78px' },
    { key: 'fieldGoals', label: '投篮%', width: '72px' },
    { key: 'threePoints', label: '三分%', width: '72px' },
    { key: 'freeThrows', label: '罚球%', width: '72px' },
  ]
}

export function getPlayerColumns(statisticType: StatisticType): TableColumn[] {
  const isAverage = statisticType === 'average'
  return [
    { key: 'rank', label: '排名', width: '60px' },
    { key: 'player', label: '球员', align: 'left', width: '165px', cellClass: 'is-player' },
    { key: 'team', label: '战队', align: 'left', width: '145px', cellClass: 'is-team' },
    { key: 'position', label: '位置', width: '70px' },
    { key: 'games', label: '场次', width: '70px' },
    { key: 'points', label: isAverage ? '场均得分' : '总得分', width: '78px' },
    { key: 'rebounds', label: isAverage ? '场均篮板' : '总篮板', width: '78px' },
    { key: 'assists', label: isAverage ? '场均助攻' : '总助攻', width: '78px' },
    { key: 'steals', label: isAverage ? '场均抢断' : '总抢断', width: '78px' },
    { key: 'blocks', label: isAverage ? '场均盖帽' : '总盖帽', width: '78px' },
    { key: 'fieldGoals', label: '投篮%', width: '72px' },
    { key: 'threePoints', label: '三分%', width: '72px' },
    { key: 'freeThrows', label: '罚球%', width: '72px' },
  ]
}
