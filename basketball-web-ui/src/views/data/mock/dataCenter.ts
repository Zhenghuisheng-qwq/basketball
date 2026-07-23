export type DataType = 'team' | 'player'
export type StatisticType = 'average' | 'total'
export type PlayerPosition = 'PG' | 'SG' | 'SF' | 'PF' | 'C'

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

export interface TeamStatistics {
  id: number
  rank: number
  teamId: number
  teamKey: string
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
  averageFieldGoalsMade: number
  averageFieldGoalsAttempted: number
  averageThreePointsMade: number
  averageThreePointsAttempted: number
  averageFreeThrowsMade: number
  averageFreeThrowsAttempted: number
  fieldGoalsMade: number
  fieldGoalsAttempted: number
  threePointsMade: number
  threePointsAttempted: number
  freeThrowsMade: number
  freeThrowsAttempted: number
}

export interface PlayerStatistics {
  id: number
  playerId: number
  rank: number
  playerName: string
  teamId: number
  teamKey: string
  teamName: string
  teamAbbr: string
  teamLogoUrl: string
  logoColor: string
  position: PlayerPosition
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
  averageFieldGoalsMade: number
  averageFieldGoalsAttempted: number
  averageThreePointsMade: number
  averageThreePointsAttempted: number
  averageFreeThrowsMade: number
  averageFreeThrowsAttempted: number
  fieldGoalsMade: number
  fieldGoalsAttempted: number
  threePointsMade: number
  threePointsAttempted: number
  freeThrowsMade: number
  freeThrowsAttempted: number
}

export interface TableColumn {
  key: string
  label: string
  align?: 'left' | 'center'
  width?: string
  cellClass?: string
}

export const DEFAULT_TOURNAMENT_ID = '2026-league-s1'

export const tournamentGroups: TournamentGroup[] = [
  {
    season: '2026赛季',
    tournaments: [
      { id: '2026-league-s1', name: 'CNPA League S1', season: '2026赛季', label: '2026 CNPA League S1' },
      { id: '2026-cup', name: 'CNPA Cup', season: '2026赛季', label: '2026 CNPA Cup' },
      { id: '2026-arena', name: 'CNPA Arena', season: '2026赛季', label: '2026 CNPA Arena' },
      { id: '2026-open', name: 'CNPA Open', season: '2026赛季', label: '2026 CNPA Open' },
    ],
  },
  {
    season: '2025赛季',
    tournaments: [
      { id: '2025-league-s2', name: 'CNPA League S2', season: '2025赛季', label: '2025 CNPA League S2' },
      { id: '2025-league-s1', name: 'CNPA League S1', season: '2025赛季', label: '2025 CNPA League S1' },
      { id: '2025-cup', name: 'CNPA Cup', season: '2025赛季', label: '2025 CNPA Cup' },
      { id: '2025-open', name: 'CNPA Open', season: '2025赛季', label: '2025 CNPA Open' },
    ],
  },
]

const teamSeed = [
  { teamName: 'RNG', teamAbbr: 'RNG', teamKey: 'rangers', logoColor: '#c62828', games: 12, wins: 10, losses: 2, avg: [98.5, 42.3, 24.1, 8.5, 5.2, 12.8], shotAvg: [38.6, 79.2, 12.4, 32.1, 18.9, 24.5], shotTotal: [528, 974, 149, 385, 227, 294] },
  { teamName: 'TITAN', teamAbbr: 'TITAN', teamKey: 'titan', logoColor: '#c9a35a', games: 12, wins: 9, losses: 3, avg: [95.2, 40.8, 22.6, 7.9, 4.8, 13.1], shotAvg: [36.8, 77.4, 11.6, 30.8, 17.4, 22.9], shotTotal: [442, 929, 139, 370, 209, 275] },
  { teamName: 'PHOENIX', teamAbbr: 'PHOENIX', teamKey: 'phoenix', logoColor: '#e65100', games: 12, wins: 8, losses: 4, avg: [92.8, 39.5, 21.4, 7.4, 4.5, 13.5], shotAvg: [35.4, 76.1, 11.1, 30.2, 16.8, 22.1], shotTotal: [425, 913, 133, 362, 202, 265] },
  { teamName: 'DRAGON', teamAbbr: 'DRAGON', teamKey: 'dragon', logoColor: '#2e7d32', games: 12, wins: 7, losses: 5, avg: [90.1, 38.9, 20.2, 7.1, 4.2, 13.8], shotAvg: [34.2, 75.3, 10.4, 29.1, 16.1, 21.4], shotTotal: [410, 904, 125, 349, 193, 257] },
  { teamName: 'LEGEND', teamAbbr: 'LEGEND', teamKey: 'legend', logoColor: '#6a1b9a', games: 12, wins: 7, losses: 5, avg: [88.6, 37.6, 19.8, 6.8, 4.0, 14.0], shotAvg: [33.5, 74.6, 10.1, 28.7, 15.6, 20.8], shotTotal: [402, 895, 121, 344, 187, 250] },
  { teamName: 'WOLVES', teamAbbr: 'WOLVES', teamKey: 'wolves', logoColor: '#1565c0', games: 12, wins: 6, losses: 6, avg: [86.4, 36.8, 19.1, 6.5, 3.8, 14.2], shotAvg: [32.6, 73.8, 9.7, 28.0, 15.1, 20.2], shotTotal: [391, 886, 116, 336, 181, 242] },
  { teamName: 'ALL STAR', teamAbbr: 'ALL STAR', teamKey: 'allstar', logoColor: '#f9a825', games: 12, wins: 6, losses: 6, avg: [84.9, 36.1, 18.6, 6.2, 3.6, 14.4], shotAvg: [31.9, 73.1, 9.4, 27.5, 14.7, 19.7], shotTotal: [383, 877, 113, 330, 176, 236] },
  { teamName: 'OMEGA', teamAbbr: 'OMEGA', teamKey: 'omega', logoColor: '#3949ab', games: 12, wins: 5, losses: 7, avg: [83.2, 35.4, 18.0, 5.9, 3.4, 14.7], shotAvg: [31.2, 72.4, 9.0, 26.9, 14.2, 19.1], shotTotal: [374, 869, 108, 323, 170, 229] },
  { teamName: 'BEAST', teamAbbr: 'BEAST', teamKey: 'beast', logoColor: '#4e342e', games: 12, wins: 4, losses: 8, avg: [81.5, 34.6, 17.3, 5.6, 3.2, 15.0], shotAvg: [30.4, 71.6, 8.6, 26.2, 13.7, 18.5], shotTotal: [365, 859, 103, 314, 164, 222] },
  { teamName: 'UNITY', teamAbbr: 'UNITY', teamKey: 'unity', logoColor: '#00838f', games: 12, wins: 3, losses: 9, avg: [79.8, 33.9, 16.7, 5.3, 3.0, 15.3], shotAvg: [29.7, 70.8, 8.2, 25.5, 13.2, 17.9], shotTotal: [356, 850, 98, 306, 158, 215] },
  { teamName: 'YETI', teamAbbr: 'YETI', teamKey: 'yeti', logoColor: '#546e7a', games: 12, wins: 3, losses: 9, avg: [78.1, 33.2, 16.1, 5.0, 2.8, 15.5], shotAvg: [29.0, 70.0, 7.9, 24.9, 12.8, 17.4], shotTotal: [348, 840, 95, 299, 154, 209] },
  { teamName: 'TRIPLE', teamAbbr: 'TRIPLE', teamKey: 'triple', logoColor: '#ad1457', games: 12, wins: 2, losses: 10, avg: [76.4, 32.5, 15.5, 4.8, 2.6, 15.8], shotAvg: [28.3, 69.2, 7.5, 24.2, 12.3, 16.8], shotTotal: [340, 830, 90, 290, 148, 202] },
  { teamName: 'KNIGHT', teamAbbr: 'KNIGHT', teamKey: 'knight', logoColor: '#455a64', games: 12, wins: 2, losses: 10, avg: [74.9, 31.8, 15.0, 4.5, 2.5, 16.0], shotAvg: [27.7, 68.5, 7.2, 23.6, 11.9, 16.3], shotTotal: [332, 822, 86, 283, 143, 196] },
  { teamName: 'VORTEX', teamAbbr: 'VORTEX', teamKey: 'vortex', logoColor: '#00695c', games: 12, wins: 2, losses: 10, avg: [73.2, 31.1, 14.4, 4.3, 2.3, 16.2], shotAvg: [27.0, 67.7, 6.9, 23.0, 11.5, 15.8], shotTotal: [324, 812, 83, 276, 138, 190] },
  { teamName: 'NOVA', teamAbbr: 'NOVA', teamKey: 'nova', logoColor: '#ef6c00', games: 12, wins: 1, losses: 11, avg: [71.6, 30.4, 13.9, 4.0, 2.1, 16.5], shotAvg: [26.3, 66.9, 6.5, 22.3, 11.0, 15.2], shotTotal: [316, 803, 78, 268, 132, 182] },
  { teamName: 'STORM', teamAbbr: 'STORM', teamKey: 'storm', logoColor: '#283593', games: 12, wins: 1, losses: 11, avg: [70.1, 29.8, 13.4, 3.8, 2.0, 16.7], shotAvg: [25.7, 66.2, 6.2, 21.7, 10.6, 14.7], shotTotal: [308, 794, 74, 260, 127, 176] },
  { teamName: 'EIK', teamAbbr: 'EIK', teamKey: 'eik', logoColor: '#5d4037', games: 12, wins: 1, losses: 11, avg: [68.5, 29.1, 12.9, 3.6, 1.9, 16.9], shotAvg: [25.0, 65.4, 5.9, 21.1, 10.2, 14.2], shotTotal: [300, 785, 71, 253, 122, 170] },
  { teamName: 'KNIGHTS', teamAbbr: 'KNIGHTS', teamKey: 'knights', logoColor: '#37474f', games: 12, wins: 0, losses: 12, avg: [66.8, 28.4, 12.3, 3.3, 1.7, 17.2], shotAvg: [24.3, 64.6, 5.5, 20.4, 9.8, 13.6], shotTotal: [292, 775, 66, 245, 118, 163] },
  { teamName: 'INVICTUS', teamAbbr: 'INVICTUS', teamKey: 'invictus', logoColor: '#b71c1c', games: 12, wins: 0, losses: 12, avg: [65.2, 27.7, 11.8, 3.1, 1.6, 17.4], shotAvg: [23.7, 63.9, 5.2, 19.8, 9.4, 13.1], shotTotal: [284, 767, 62, 238, 113, 157] },
  { teamName: 'AETHER', teamAbbr: 'AETHER', teamKey: 'aether', logoColor: '#00897b', games: 12, wins: 0, losses: 12, avg: [63.7, 27.0, 11.3, 2.9, 1.4, 17.6], shotAvg: [23.0, 63.1, 4.9, 19.1, 9.0, 12.6], shotTotal: [276, 757, 59, 229, 108, 151] },
  { teamName: 'PULSE', teamAbbr: 'PULSE', teamKey: 'pulse', logoColor: '#d84315', games: 11, wins: 4, losses: 7, avg: [82.4, 35.0, 17.8, 5.8, 3.3, 14.6], shotAvg: [30.9, 72.0, 8.9, 26.6, 14.0, 18.8], shotTotal: [340, 792, 98, 293, 154, 207] },
  { teamName: 'BLAZE', teamAbbr: 'BLAZE', teamKey: 'blaze', logoColor: '#e53935', games: 11, wins: 5, losses: 6, avg: [85.1, 36.4, 18.9, 6.3, 3.7, 14.1], shotAvg: [32.1, 73.5, 9.5, 27.8, 14.9, 19.9], shotTotal: [353, 809, 105, 306, 164, 219] },
  { teamName: 'SHADOW', teamAbbr: 'SHADOW', teamKey: 'shadow', logoColor: '#212121', games: 11, wins: 3, losses: 8, avg: [77.3, 32.8, 15.8, 4.9, 2.7, 15.6], shotAvg: [28.6, 69.6, 7.7, 24.6, 12.6, 17.1], shotTotal: [315, 766, 85, 271, 139, 188] },
  { teamName: 'ORBIT', teamAbbr: 'ORBIT', teamKey: 'orbit', logoColor: '#0277bd', games: 11, wins: 4, losses: 7, avg: [80.6, 34.1, 16.9, 5.4, 3.1, 14.9], shotAvg: [30.1, 71.1, 8.4, 25.9, 13.4, 18.1], shotTotal: [331, 782, 92, 285, 147, 199] },
] as const

function buildTeamRow(
  index: number,
  seed: (typeof teamSeed)[number],
): TeamStatistics {
  const [ap, ar, aa, as, ab, at] = seed.avg
  const [afgm, afga, a3pm, a3pa, aftm, afta] = seed.shotAvg
  const [fgm, fga, tpm, tpa, ftm, fta] = seed.shotTotal
  return {
    id: index + 1,
    rank: index + 1,
    teamId: index + 1,
    teamKey: seed.teamKey,
    teamName: seed.teamName,
    teamAbbr: seed.teamAbbr,
    logoUrl: '',
    logoColor: seed.logoColor,
    games: seed.games,
    wins: seed.wins,
    losses: seed.losses,
    winRate: Number(((seed.wins / seed.games) * 100).toFixed(1)),
    averagePoints: ap,
    averageRebounds: ar,
    averageAssists: aa,
    averageSteals: as,
    averageBlocks: ab,
    averageTurnovers: at,
    totalPoints: Math.round(ap * seed.games),
    totalRebounds: Math.round(ar * seed.games),
    totalAssists: Math.round(aa * seed.games),
    totalSteals: Math.round(as * seed.games),
    totalBlocks: Math.round(ab * seed.games),
    totalTurnovers: Math.round(at * seed.games),
    averageFieldGoalsMade: afgm,
    averageFieldGoalsAttempted: afga,
    averageThreePointsMade: a3pm,
    averageThreePointsAttempted: a3pa,
    averageFreeThrowsMade: aftm,
    averageFreeThrowsAttempted: afta,
    fieldGoalsMade: fgm,
    fieldGoalsAttempted: fga,
    threePointsMade: tpm,
    threePointsAttempted: tpa,
    freeThrowsMade: ftm,
    freeThrowsAttempted: fta,
  }
}

export const teamStatisticsList: TeamStatistics[] = teamSeed.map((seed, index) =>
  buildTeamRow(index, seed),
)

const playerSeed = [
  { playerName: 'RNG_TitanX', teamName: 'RNG', teamAbbr: 'RNG', teamKey: 'rangers', logoColor: '#c62828', position: 'PG' as PlayerPosition, games: 12, avg: [28.6, 5.2, 8.4, 2.1, 0.4, 3.2], shotAvg: [10.2, 19.3, 3.4, 8.1, 4.8, 5.9], shotTotal: [122, 232, 41, 97, 58, 71] },
  { playerName: 'TITAN_King', teamName: 'TITAN', teamAbbr: 'TITAN', teamKey: 'titan', logoColor: '#c9a35a', position: 'SF' as PlayerPosition, games: 12, avg: [26.4, 7.1, 4.8, 1.8, 0.9, 2.8], shotAvg: [9.6, 18.7, 2.9, 7.4, 4.3, 5.4], shotTotal: [115, 224, 35, 89, 52, 65] },
  { playerName: 'PHX_Fire', teamName: 'PHOENIX', teamAbbr: 'PHOENIX', teamKey: 'phoenix', logoColor: '#e65100', position: 'SG' as PlayerPosition, games: 12, avg: [24.8, 4.6, 5.5, 1.9, 0.3, 2.6], shotAvg: [8.9, 17.8, 3.6, 8.8, 3.4, 4.2], shotTotal: [107, 214, 43, 106, 41, 50] },
  { playerName: 'DRG_Lion', teamName: 'DRAGON', teamAbbr: 'DRAGON', teamKey: 'dragon', logoColor: '#2e7d32', position: 'PF' as PlayerPosition, games: 12, avg: [23.1, 9.4, 3.2, 1.2, 1.6, 2.4], shotAvg: [8.7, 16.4, 1.4, 4.1, 4.3, 5.6], shotTotal: [104, 197, 17, 49, 52, 67] },
  { playerName: 'LEG_Zeus', teamName: 'LEGEND', teamAbbr: 'LEGEND', teamKey: 'legend', logoColor: '#6a1b9a', position: 'C' as PlayerPosition, games: 12, avg: [21.8, 11.2, 2.6, 0.9, 2.4, 2.9], shotAvg: [8.4, 15.1, 0.6, 1.8, 4.4, 6.1], shotTotal: [101, 181, 7, 22, 53, 73] },
  { playerName: 'WOLVES_Ace', teamName: 'WOLVES', teamAbbr: 'WOLVES', teamKey: 'wolves', logoColor: '#1565c0', position: 'PG' as PlayerPosition, games: 12, avg: [20.9, 4.1, 7.6, 2.3, 0.2, 3.4], shotAvg: [7.5, 16.2, 2.8, 7.5, 3.1, 3.9], shotTotal: [90, 194, 34, 90, 37, 47] },
  { playerName: 'ALLSTAR_Melo', teamName: 'ALL STAR', teamAbbr: 'ALL STAR', teamKey: 'allstar', logoColor: '#f9a825', position: 'SF' as PlayerPosition, games: 12, avg: [19.7, 6.3, 4.1, 1.5, 0.7, 2.5], shotAvg: [7.1, 15.6, 2.4, 6.6, 3.1, 3.8], shotTotal: [85, 187, 29, 79, 37, 46] },
  { playerName: 'OMG_Nova', teamName: 'OMEGA', teamAbbr: 'OMEGA', teamKey: 'omega', logoColor: '#3949ab', position: 'SG' as PlayerPosition, games: 12, avg: [18.6, 3.8, 4.9, 1.7, 0.3, 2.7], shotAvg: [6.6, 14.9, 2.9, 7.7, 2.5, 3.1], shotTotal: [79, 179, 35, 92, 30, 37] },
  { playerName: 'BEAST_Hawk', teamName: 'BEAST', teamAbbr: 'BEAST', teamKey: 'beast', logoColor: '#4e342e', position: 'PF' as PlayerPosition, games: 12, avg: [17.4, 8.6, 2.8, 1.1, 1.3, 2.3], shotAvg: [6.8, 13.7, 1.1, 3.4, 2.7, 3.8], shotTotal: [82, 164, 13, 41, 32, 46] },
  { playerName: 'UNITY_Jay', teamName: 'UNITY', teamAbbr: 'UNITY', teamKey: 'unity', logoColor: '#00838f', position: 'PG' as PlayerPosition, games: 12, avg: [16.8, 3.5, 6.9, 1.9, 0.2, 3.1], shotAvg: [5.9, 13.8, 2.2, 6.3, 2.8, 3.5], shotTotal: [71, 166, 26, 76, 34, 42] },
  { playerName: 'YETI_Frost', teamName: 'YETI', teamAbbr: 'YETI', teamKey: 'yeti', logoColor: '#546e7a', position: 'C' as PlayerPosition, games: 12, avg: [15.9, 10.1, 2.1, 0.7, 2.0, 2.6], shotAvg: [6.1, 12.4, 0.4, 1.4, 3.3, 4.7], shotTotal: [73, 149, 5, 17, 40, 56] },
  { playerName: 'TRP_Flash', teamName: 'TRIPLE', teamAbbr: 'TRIPLE', teamKey: 'triple', logoColor: '#ad1457', position: 'SG' as PlayerPosition, games: 12, avg: [15.2, 3.9, 3.8, 1.4, 0.3, 2.4], shotAvg: [5.4, 12.9, 2.3, 6.5, 2.1, 2.7], shotTotal: [65, 155, 28, 78, 25, 32] },
  { playerName: 'KNT_Blade', teamName: 'KNIGHT', teamAbbr: 'KNIGHT', teamKey: 'knight', logoColor: '#455a64', position: 'SF' as PlayerPosition, games: 12, avg: [14.6, 5.7, 3.3, 1.3, 0.6, 2.2], shotAvg: [5.2, 12.1, 1.9, 5.5, 2.3, 2.9], shotTotal: [62, 145, 23, 66, 28, 35] },
  { playerName: 'VTX_Spin', teamName: 'VORTEX', teamAbbr: 'VORTEX', teamKey: 'vortex', logoColor: '#00695c', position: 'PG' as PlayerPosition, games: 12, avg: [14.1, 3.2, 6.2, 1.8, 0.2, 2.9], shotAvg: [4.9, 12.0, 2.0, 5.9, 2.3, 2.8], shotTotal: [59, 144, 24, 71, 28, 34] },
  { playerName: 'NVA_Spark', teamName: 'NOVA', teamAbbr: 'NOVA', teamKey: 'nova', logoColor: '#ef6c00', position: 'SG' as PlayerPosition, games: 12, avg: [13.5, 3.6, 3.5, 1.2, 0.2, 2.1], shotAvg: [4.7, 11.6, 2.1, 5.8, 2.0, 2.5], shotTotal: [56, 139, 25, 70, 24, 30] },
  { playerName: 'STM_Bolt', teamName: 'STORM', teamAbbr: 'STORM', teamKey: 'storm', logoColor: '#283593', position: 'PF' as PlayerPosition, games: 12, avg: [12.9, 7.8, 2.4, 0.9, 1.1, 2.0], shotAvg: [5.0, 11.0, 0.8, 2.6, 2.1, 3.0], shotTotal: [60, 132, 10, 31, 25, 36] },
  { playerName: 'EIK_Ridge', teamName: 'EIK', teamAbbr: 'EIK', teamKey: 'eik', logoColor: '#5d4037', position: 'C' as PlayerPosition, games: 12, avg: [12.3, 9.2, 1.8, 0.6, 1.7, 2.3], shotAvg: [4.8, 10.2, 0.3, 1.1, 2.4, 3.5], shotTotal: [58, 122, 4, 13, 29, 42] },
  { playerName: 'KTS_Shield', teamName: 'KNIGHTS', teamAbbr: 'KNIGHTS', teamKey: 'knights', logoColor: '#37474f', position: 'SF' as PlayerPosition, games: 12, avg: [11.8, 5.1, 2.9, 1.0, 0.5, 1.9], shotAvg: [4.3, 10.5, 1.5, 4.4, 1.7, 2.2], shotTotal: [52, 126, 18, 53, 20, 26] },
  { playerName: 'INV_Prime', teamName: 'INVICTUS', teamAbbr: 'INVICTUS', teamKey: 'invictus', logoColor: '#b71c1c', position: 'PG' as PlayerPosition, games: 12, avg: [11.2, 2.9, 5.4, 1.5, 0.1, 2.7], shotAvg: [3.9, 10.1, 1.7, 5.1, 1.7, 2.1], shotTotal: [47, 121, 20, 61, 20, 25] },
  { playerName: 'AET_Wave', teamName: 'AETHER', teamAbbr: 'AETHER', teamKey: 'aether', logoColor: '#00897b', position: 'SG' as PlayerPosition, games: 12, avg: [10.6, 3.3, 3.1, 1.1, 0.2, 1.8], shotAvg: [3.7, 9.6, 1.6, 4.7, 1.6, 2.0], shotTotal: [44, 115, 19, 56, 19, 24] },
  { playerName: 'PLS_Rhythm', teamName: 'PULSE', teamAbbr: 'PULSE', teamKey: 'pulse', logoColor: '#d84315', position: 'SF' as PlayerPosition, games: 11, avg: [18.2, 5.8, 3.7, 1.4, 0.6, 2.3], shotAvg: [6.5, 14.2, 2.2, 6.0, 3.0, 3.7], shotTotal: [72, 156, 24, 66, 33, 41] },
  { playerName: 'BLZ_Inferno', teamName: 'BLAZE', teamAbbr: 'BLAZE', teamKey: 'blaze', logoColor: '#e53935', position: 'PF' as PlayerPosition, games: 11, avg: [17.6, 8.1, 2.9, 1.0, 1.2, 2.2], shotAvg: [6.7, 13.5, 1.2, 3.5, 3.0, 4.1], shotTotal: [74, 149, 13, 39, 33, 45] },
  { playerName: 'SHD_Night', teamName: 'SHADOW', teamAbbr: 'SHADOW', teamKey: 'shadow', logoColor: '#212121', position: 'PG' as PlayerPosition, games: 11, avg: [16.1, 3.4, 6.4, 1.8, 0.2, 2.8], shotAvg: [5.6, 13.1, 2.1, 6.0, 2.8, 3.4], shotTotal: [62, 144, 23, 66, 31, 37] },
  { playerName: 'ORB_Comet', teamName: 'ORBIT', teamAbbr: 'ORBIT', teamKey: 'orbit', logoColor: '#0277bd', position: 'C' as PlayerPosition, games: 11, avg: [15.4, 9.6, 2.0, 0.7, 1.8, 2.4], shotAvg: [5.9, 11.8, 0.5, 1.5, 3.1, 4.4], shotTotal: [65, 130, 6, 17, 34, 48] },
] as const

function buildPlayerRow(
  index: number,
  seed: (typeof playerSeed)[number],
): PlayerStatistics {
  const [ap, ar, aa, as, ab, at] = seed.avg
  const [afgm, afga, a3pm, a3pa, aftm, afta] = seed.shotAvg
  const [fgm, fga, tpm, tpa, ftm, fta] = seed.shotTotal
  return {
    id: index + 1,
    playerId: 10001 + index,
    rank: index + 1,
    playerName: seed.playerName,
    teamId: index + 1,
    teamKey: seed.teamKey,
    teamName: seed.teamName,
    teamAbbr: seed.teamAbbr,
    teamLogoUrl: '',
    logoColor: seed.logoColor,
    position: seed.position,
    games: seed.games,
    averagePoints: ap,
    averageRebounds: ar,
    averageAssists: aa,
    averageSteals: as,
    averageBlocks: ab,
    averageTurnovers: at,
    totalPoints: Math.round(ap * seed.games),
    totalRebounds: Math.round(ar * seed.games),
    totalAssists: Math.round(aa * seed.games),
    totalSteals: Math.round(as * seed.games),
    totalBlocks: Math.round(ab * seed.games),
    totalTurnovers: Math.round(at * seed.games),
    averageFieldGoalsMade: afgm,
    averageFieldGoalsAttempted: afga,
    averageThreePointsMade: a3pm,
    averageThreePointsAttempted: a3pa,
    averageFreeThrowsMade: aftm,
    averageFreeThrowsAttempted: afta,
    fieldGoalsMade: fgm,
    fieldGoalsAttempted: fga,
    threePointsMade: tpm,
    threePointsAttempted: tpa,
    freeThrowsMade: ftm,
    freeThrowsAttempted: fta,
  }
}

export const playerStatisticsList: PlayerStatistics[] = playerSeed.map((seed, index) =>
  buildPlayerRow(index, seed),
)

export function getTournamentById(id: string): TournamentOption | undefined {
  for (const group of tournamentGroups) {
    const found = group.tournaments.find((item) => item.id === id)
    if (found) return found
  }
  return undefined
}

export function formatRate(value: number): string {
  return `${value.toFixed(1)}%`
}

export function formatWinLoss(wins: number, losses: number): string {
  return `${wins}/${losses}`
}

export function formatMadeAttempted(made: number, attempted: number, average: boolean): string {
  if (average) {
    return `${made.toFixed(1)}/${attempted.toFixed(1)}`
  }
  return `${made}/${attempted}`
}

export function formatStatNumber(value: number, average: boolean): string {
  if (average) {
    return Number.isInteger(value) ? String(value) : value.toFixed(1)
  }
  return String(Math.round(value))
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
    { key: 'fieldGoals', label: '投篮命中/出手', width: '110px' },
    { key: 'threePoints', label: '三分命中/出手', width: '110px' },
    { key: 'freeThrows', label: '罚球命中/出手', width: '110px' },
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
    { key: 'turnovers', label: isAverage ? '场均失误' : '总失误', width: '78px' },
    { key: 'fieldGoals', label: '投篮命中/出手', width: '110px' },
    { key: 'threePoints', label: '三分命中/出手', width: '110px' },
    { key: 'freeThrows', label: '罚球命中/出手', width: '110px' },
  ]
}
