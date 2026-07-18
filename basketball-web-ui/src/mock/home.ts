export interface MatchItem {
  id: number
  date: string
  round: string
  homeTeam: TeamItem
  awayTeam: TeamItem
}

export interface TeamItem {
  key: string
  name: string
}

export interface TeamRankItem {
  rank: number
  teamKey: string
  teamName: string
  wins: number
  losses: number
  winRate: string
  streak: string
}

export interface PlayerRankItem {
  rank: number
  name: string
  team: string
  ppg: string
}

export interface StatItem {
  value: string
  label: string
}

export const mockMatches: MatchItem[] = [
  {
    id: 1,
    date: '05.18 周六 19:00',
    round: '常规赛 R05',
    homeTeam: { key: 'titan', name: 'TITAN' },
    awayTeam: { key: 'phoenix', name: 'PHOENIX' },
  },
  {
    id: 2,
    date: '05.18 周六 21:00',
    round: '常规赛 R05',
    homeTeam: { key: 'wolves', name: 'WOLVES' },
    awayTeam: { key: 'legend', name: 'LEGEND' },
  },
  {
    id: 3,
    date: '05.19 周日 19:00',
    round: '常规赛 R05',
    homeTeam: { key: 'dragon', name: 'DRAGON' },
    awayTeam: { key: 'rangers', name: 'RANGERS' },
  },
]

export const mockTeams: TeamItem[] = [
  { key: 'titan', name: 'TITAN' },
  { key: 'phoenix', name: 'PHOENIX' },
  { key: 'wolves', name: 'WOLVES' },
  { key: 'dragon', name: 'DRAGON' },
  { key: 'legend', name: 'LEGEND' },
  { key: 'rangers', name: 'RANGERS' },
]

export const mockTeamRanks: TeamRankItem[] = [
  { rank: 1, teamKey: 'titan', teamName: 'TITAN', wins: 12, losses: 2, winRate: '85.7%', streak: '5连胜' },
  { rank: 2, teamKey: 'wolves', teamName: 'WOLVES', wins: 11, losses: 3, winRate: '78.6%', streak: '3连胜' },
  { rank: 3, teamKey: 'phoenix', teamName: 'PHOENIX', wins: 10, losses: 4, winRate: '71.4%', streak: '2连胜' },
  { rank: 4, teamKey: 'dragon', teamName: 'DRAGON', wins: 8, losses: 6, winRate: '57.1%', streak: '1连胜' },
  { rank: 5, teamKey: 'legend', teamName: 'LEGEND', wins: 7, losses: 7, winRate: '50.0%', streak: '2连胜' },
]

export const mockPlayerRanks: PlayerRankItem[] = [
  { rank: 1, name: 'CNPA_TitanX', team: 'TITAN', ppg: '28.4 PPG' },
  { rank: 2, name: 'Wolf_King', team: 'WOLVES', ppg: '24.1 PPG' },
  { rank: 3, name: 'Phx_Fire', team: 'PHOENIX', ppg: '22.3 PPG' },
  { rank: 4, name: 'Drg_Lion', team: 'DRAGON', ppg: '21.8 PPG' },
  { rank: 5, name: 'Leg_Zeus', team: 'LEGEND', ppg: '20.4 PPG' },
]

export const mockStats: StatItem[] = [
  { value: '16', label: '支参赛战队' },
  { value: '320+', label: '职业电竞选手' },
  { value: '120+', label: '全年比赛场次' },
  { value: '500W+', label: '年度曝光人次' },
]
