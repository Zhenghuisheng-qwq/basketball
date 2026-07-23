import type {
  ChampionPageResult,
  ChampionRecord,
  ChampionRosterMember,
  EventHonorRecord,
  EventMvpRecord,
  HallTournament,
  HonorPlayer,
  TournamentWeek,
  WeeklyBestRecord,
} from '../types'

interface PlayerSeed {
  id: number
  name: string
  teamId: string
  teamName: string
  teamAbbr: string
  position: HonorPlayer['position']
}

const players: PlayerSeed[] = [
  { id: 10001, name: 'KNIGHT', teamId: 'rng', teamName: 'RNG', teamAbbr: 'RNG', position: 'PG' },
  { id: 10002, name: 'TITAN_King', teamId: 'titan', teamName: 'TITAN', teamAbbr: 'TITAN', position: 'SF' },
  { id: 10003, name: 'FIRE', teamId: 'phoenix', teamName: 'PHOENIX', teamAbbr: 'PHOENIX', position: 'SG' },
  { id: 10004, name: 'ZERO', teamId: 'dragon', teamName: 'DRAGON', teamAbbr: 'DRAGON', position: 'PF' },
  { id: 10005, name: 'RAIN', teamId: 'legend', teamName: 'LEGEND', teamAbbr: 'LEGEND', position: 'C' },
  { id: 10006, name: 'ELK', teamId: 'wolves', teamName: 'WOLVES', teamAbbr: 'WOLVES', position: 'PG' },
  { id: 10007, name: 'ALLSTAR_Melo', teamId: 'all-star', teamName: 'ALL STAR', teamAbbr: 'ALL STAR', position: 'SF' },
  { id: 10008, name: 'CLOUD', teamId: 'omega', teamName: 'OMEGA', teamAbbr: 'OMEGA', position: 'SG' },
  { id: 10009, name: 'MOON', teamId: 'beast', teamName: 'BEAST', teamAbbr: 'BEAST', position: 'PF' },
  { id: 10010, name: 'JET', teamId: 'unity', teamName: 'UNITY', teamAbbr: 'UNITY', position: 'C' },
]

const defensivePlayers: HonorPlayer[] = [
  toHonorPlayer(10006, 'SHARK', 'wolves', 'WOLVES', 'PG'),
  toHonorPlayer(10008, 'CLOUD', 'omega', 'OMEGA', 'SG'),
  toHonorPlayer(10002, 'ACE', 'titan', 'TITAN', 'SF'),
  toHonorPlayer(10010, 'MOON', 'unity', 'UNITY', 'PF'),
  toHonorPlayer(10009, 'JET', 'beast', 'BEAST', 'C'),
]

function getPlayer(id: number): PlayerSeed {
  return players.find((item) => item.id === id) ?? players[0]!
}

function toHonorPlayer(
  id: number,
  displayName?: string,
  teamId?: string,
  teamAbbr?: string,
  position?: HonorPlayer['position'],
): HonorPlayer {
  const seed = getPlayer(id)
  return {
    playerId: seed.id,
    playerName: displayName ?? seed.name,
    playerAvatarUrl: '',
    teamId: teamId ?? seed.teamId,
    teamName: teamAbbr ?? seed.teamName,
    teamAbbr: teamAbbr ?? seed.teamAbbr,
    teamLogoUrl: '',
    position: position ?? seed.position,
  }
}

function buildAllTournamentTeam(): HonorPlayer[] {
  return [
    toHonorPlayer(10001, 'KNIGHT', 'rng', 'RNG', 'PG'),
    toHonorPlayer(10006, 'ELK', 'wolves', 'WOLVES', 'SG'),
    toHonorPlayer(10003, 'FIRE', 'phoenix', 'PHOENIX', 'SF'),
    toHonorPlayer(10004, 'ZERO', 'dragon', 'DRAGON', 'PF'),
    toHonorPlayer(10005, 'RAIN', 'legend', 'LEGEND', 'C'),
  ]
}

function buildMvp(tournamentId: string, playerId = 10001): EventMvpRecord {
  const player = toHonorPlayer(playerId, 'KNIGHT', 'rng', 'RNG', 'PG')
  return {
    ...player,
    tournamentId,
    averagePoints: 26.8,
    averageRebounds: 7.9,
    averageAssists: 9.6,
    averageSteals: 2.1,
    fieldGoalPercentage: 60.4,
    published: true,
  }
}

function buildRoster(tournamentId: string, teamId: string): ChampionRosterMember[] {
  const rosterPlayers = [
    { id: 10001, role: 'STARTER' as const, captain: true, appearances: 12 },
    { id: 10002, role: 'STARTER' as const, captain: false, appearances: 12 },
    { id: 10003, role: 'STARTER' as const, captain: false, appearances: 11 },
    { id: 10004, role: 'STARTER' as const, captain: false, appearances: 12 },
    { id: 10005, role: 'STARTER' as const, captain: false, appearances: 10 },
    { id: 10006, role: 'SUBSTITUTE' as const, captain: false, appearances: 8 },
    { id: 10007, role: 'SUBSTITUTE' as const, captain: false, appearances: 7 },
    { id: 10008, role: 'SUBSTITUTE' as const, captain: false, appearances: 6 },
  ]

  return rosterPlayers.map((item, index) => {
    const seed = getPlayer(item.id)
    return {
      id: `${tournamentId}-roster-${index + 1}`,
      tournamentId,
      teamId,
      playerId: seed.id,
      playerName: seed.name,
      playerAvatarUrl: '',
      position: seed.position,
      role: item.role,
      captain: item.captain,
      appearances: item.appearances,
      sortOrder: index + 1,
    }
  })
}

export const hallTournaments: HallTournament[] = [
  {
    id: '2026-league-s1',
    seasonYear: 2026,
    seasonName: '2026 CNPA League S1',
    fullName: '2026 CNPA League S1',
    shortName: 'League S1',
    seriesCode: 'LEAGUE',
    seriesName: 'CNPA League',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2026-01-10',
    endDate: '2026-06-04',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: true,
  },
  {
    id: '2026-arena-s1',
    seasonYear: 2026,
    seasonName: '2026 CNPA Arena S1',
    fullName: '2026 CNPA Arena S1',
    shortName: 'Arena S1',
    seriesCode: 'ARENA',
    seriesName: 'CNPA Arena',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2026-02-01',
    endDate: '2026-05-18',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: true,
  },
  {
    id: '2026-cup',
    seasonYear: 2026,
    seasonName: '2026 CNPA Cup',
    fullName: '2026 CNPA Cup',
    shortName: 'Cup',
    seriesCode: 'CUP',
    seriesName: 'CNPA Cup',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2026-03-01',
    endDate: '2026-04-12',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2026-open',
    seasonYear: 2026,
    seasonName: '2026 CNPA Open',
    fullName: '2026 CNPA Open',
    shortName: 'Open',
    seriesCode: 'OPEN',
    seriesName: 'CNPA Open',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2026-02-10',
    endDate: '2026-03-08',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2025-league-s2',
    seasonYear: 2025,
    seasonName: '2025 CNPA League S2',
    fullName: '2025 CNPA League S2',
    shortName: 'League S2',
    seriesCode: 'LEAGUE',
    seriesName: 'CNPA League',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2025-07-01',
    endDate: '2025-12-06',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: true,
  },
  {
    id: '2025-arena-s2',
    seasonYear: 2025,
    seasonName: '2025 CNPA Arena S2',
    fullName: '2025 CNPA Arena S2',
    shortName: 'Arena S2',
    seriesCode: 'ARENA',
    seriesName: 'CNPA Arena',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2025-08-01',
    endDate: '2025-10-19',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: true,
  },
  {
    id: '2025-cup',
    seasonYear: 2025,
    seasonName: '2025 CNPA Cup',
    fullName: '2025 CNPA Cup',
    shortName: 'Cup',
    seriesCode: 'CUP',
    seriesName: 'CNPA Cup',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2025-06-01',
    endDate: '2025-08-11',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2025-open',
    seasonYear: 2025,
    seasonName: '2025 CNPA Open',
    fullName: '2025 CNPA Open',
    shortName: 'Open',
    seriesCode: 'OPEN',
    seriesName: 'CNPA Open',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2025-04-01',
    endDate: '2025-06-15',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
]

const tournamentMap = new Map(hallTournaments.map((item) => [item.id, item]))

function buildWeeks(tournamentId: string, count: number): TournamentWeek[] {
  const ranges = [
    ['2026-01-10', '2026-01-16'],
    ['2026-01-17', '2026-01-23'],
    ['2026-01-24', '2026-01-30'],
    ['2026-01-31', '2026-02-06'],
    ['2026-02-07', '2026-02-13'],
  ]

  return Array.from({ length: count }, (_, index) => {
    const weekNo = index + 1
    const range = ranges[index] ?? ranges[ranges.length - 1]!
    return {
      id: `${tournamentId}-w${weekNo}`,
      tournamentId,
      weekNo,
      weekName: `第 ${weekNo} 比赛周`,
      startDate: range[0],
      endDate: range[1],
      published: true,
    }
  })
}

const tournamentWeeks: TournamentWeek[] = [
  ...buildWeeks('2026-league-s1', 5),
  ...buildWeeks('2026-arena-s1', 5),
  ...buildWeeks('2025-league-s2', 5),
  ...buildWeeks('2025-arena-s2', 5),
]

const weeklyBestRecords: WeeklyBestRecord[] = [
  {
    id: 'wb-2026-league-s1-w5',
    tournamentId: '2026-league-s1',
    weekId: '2026-league-s1-w5',
    weekNo: 5,
    playerId: 10001,
    playerName: 'KNIGHT',
    playerAvatarUrl: '',
    teamId: 'rng',
    teamName: 'RNG',
    teamAbbr: 'RNG',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 28.6,
    averageRebounds: 8.4,
    averageAssists: 10.4,
    averageSteals: 2.3,
    fieldGoalPercentage: 61.8,
    published: true,
  },
  {
    id: 'wb-2026-league-s1-w4',
    tournamentId: '2026-league-s1',
    weekId: '2026-league-s1-w4',
    weekNo: 4,
    playerId: 10003,
    playerName: 'FIRE',
    playerAvatarUrl: '',
    teamId: 'phoenix',
    teamName: 'PHOENIX',
    teamAbbr: 'PHOENIX',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 27.1,
    averageRebounds: 6.2,
    averageAssists: 7.8,
    averageSteals: 1.9,
    fieldGoalPercentage: 58.4,
    published: true,
  },
  {
    id: 'wb-2026-league-s1-w3',
    tournamentId: '2026-league-s1',
    weekId: '2026-league-s1-w3',
    weekNo: 3,
    playerId: 10006,
    playerName: 'ELK',
    playerAvatarUrl: '',
    teamId: 'wolves',
    teamName: 'WOLVES',
    teamAbbr: 'WOLVES',
    teamLogoUrl: '',
    games: 3,
    averagePoints: 24.3,
    averageRebounds: 5.6,
    averageAssists: 9.1,
    averageSteals: 2.0,
    fieldGoalPercentage: 55.2,
    published: true,
  },
  {
    id: 'wb-2026-league-s1-w2',
    tournamentId: '2026-league-s1',
    weekId: '2026-league-s1-w2',
    weekNo: 2,
    playerId: 10002,
    playerName: 'TITAN_King',
    playerAvatarUrl: '',
    teamId: 'titan',
    teamName: 'TITAN',
    teamAbbr: 'TITAN',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 26.0,
    averageRebounds: 7.4,
    averageAssists: 6.5,
    averageSteals: 1.7,
    fieldGoalPercentage: 57.9,
    published: true,
  },
  {
    id: 'wb-2026-league-s1-w1',
    tournamentId: '2026-league-s1',
    weekId: '2026-league-s1-w1',
    weekNo: 1,
    playerId: 10004,
    playerName: 'ZERO',
    playerAvatarUrl: '',
    teamId: 'dragon',
    teamName: 'DRAGON',
    teamAbbr: 'DRAGON',
    teamLogoUrl: '',
    games: 3,
    averagePoints: 23.8,
    averageRebounds: 9.1,
    averageAssists: 4.2,
    averageSteals: 1.1,
    fieldGoalPercentage: 54.6,
    published: true,
  },
  {
    id: 'wb-2026-arena-s1-w5',
    tournamentId: '2026-arena-s1',
    weekId: '2026-arena-s1-w5',
    weekNo: 5,
    playerId: 10002,
    playerName: 'TITAN_King',
    playerAvatarUrl: '',
    teamId: 'titan',
    teamName: 'TITAN',
    teamAbbr: 'TITAN',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 25.4,
    averageRebounds: 6.8,
    averageAssists: 8.2,
    averageSteals: 2.0,
    fieldGoalPercentage: 59.1,
    published: true,
  },
  {
    id: 'wb-2026-arena-s1-w4',
    tournamentId: '2026-arena-s1',
    weekId: '2026-arena-s1-w4',
    weekNo: 4,
    playerId: 10003,
    playerName: 'FIRE',
    playerAvatarUrl: '',
    teamId: 'phoenix',
    teamName: 'PHOENIX',
    teamAbbr: 'PHOENIX',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 24.9,
    averageRebounds: 5.4,
    averageAssists: 6.9,
    averageSteals: 1.8,
    fieldGoalPercentage: 56.7,
    published: true,
  },
  {
    id: 'wb-2025-league-s2-w5',
    tournamentId: '2025-league-s2',
    weekId: '2025-league-s2-w5',
    weekNo: 5,
    playerId: 10006,
    playerName: 'ELK',
    playerAvatarUrl: '',
    teamId: 'wolves',
    teamName: 'WOLVES',
    teamAbbr: 'WOLVES',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 27.2,
    averageRebounds: 5.1,
    averageAssists: 8.8,
    averageSteals: 2.4,
    fieldGoalPercentage: 60.2,
    published: true,
  },
  {
    id: 'wb-2025-league-s2-w3',
    tournamentId: '2025-league-s2',
    weekId: '2025-league-s2-w3',
    weekNo: 3,
    playerId: 10001,
    playerName: 'KNIGHT',
    playerAvatarUrl: '',
    teamId: 'rng',
    teamName: 'RNG',
    teamAbbr: 'RNG',
    teamLogoUrl: '',
    games: 3,
    averagePoints: 26.5,
    averageRebounds: 7.2,
    averageAssists: 9.3,
    averageSteals: 2.1,
    fieldGoalPercentage: 58.8,
    published: true,
  },
  {
    id: 'wb-2025-arena-s2-w4',
    tournamentId: '2025-arena-s2',
    weekId: '2025-arena-s2-w4',
    weekNo: 4,
    playerId: 10008,
    playerName: 'CLOUD',
    playerAvatarUrl: '',
    teamId: 'omega',
    teamName: 'OMEGA',
    teamAbbr: 'OMEGA',
    teamLogoUrl: '',
    games: 4,
    averagePoints: 22.6,
    averageRebounds: 4.8,
    averageAssists: 7.4,
    averageSteals: 1.6,
    fieldGoalPercentage: 53.9,
    published: true,
  },
  {
    id: 'wb-2025-arena-s2-w2',
    tournamentId: '2025-arena-s2',
    weekId: '2025-arena-s2-w2',
    weekNo: 2,
    playerId: 10010,
    playerName: 'JET',
    playerAvatarUrl: '',
    teamId: 'unity',
    teamName: 'UNITY',
    teamAbbr: 'UNITY',
    teamLogoUrl: '',
    games: 3,
    averagePoints: 21.4,
    averageRebounds: 8.9,
    averageAssists: 3.1,
    averageSteals: 1.0,
    fieldGoalPercentage: 52.4,
    published: true,
  },
]

const eventHonors: EventHonorRecord[] = [
  {
    tournamentId: '2026-league-s1',
    championTeamId: 'rng',
    championTeamName: 'RNG',
    championTeamAbbr: 'RNG',
    championTeamLogoUrl: '',
    championDate: '2026.06.04',
    trophyImageUrl: '',
    description: '2026 CNPA 联赛第一赛季',
    published: true,
    mvp: buildMvp('2026-league-s1'),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: true,
    allDefensivePublished: true,
    championRoster: buildRoster('2026-league-s1', 'rng'),
  },
  {
    tournamentId: '2026-arena-s1',
    championTeamId: 'titan',
    championTeamName: 'TITAN',
    championTeamAbbr: 'TITAN',
    championTeamLogoUrl: '',
    championDate: '2026.05.18',
    trophyImageUrl: '',
    description: '2026 CNPA Arena 第一赛季',
    published: true,
    mvp: buildMvp('2026-arena-s1', 10002),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: true,
    allDefensivePublished: true,
    championRoster: buildRoster('2026-arena-s1', 'titan'),
  },
  {
    tournamentId: '2026-cup',
    championTeamId: 'phoenix',
    championTeamName: 'PHOENIX',
    championTeamAbbr: 'PHOENIX',
    championTeamLogoUrl: '',
    championDate: '2026.04.12',
    trophyImageUrl: '',
    description: '2026 CNPA 杯赛',
    published: true,
    mvp: buildMvp('2026-cup', 10003),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: true,
    allDefensivePublished: false,
    championRoster: buildRoster('2026-cup', 'phoenix'),
  },
  {
    tournamentId: '2026-open',
    championTeamId: 'dragon',
    championTeamName: 'DRAGON',
    championTeamAbbr: 'DRAGON',
    championTeamLogoUrl: '',
    championDate: '2026.03.08',
    trophyImageUrl: '',
    description: '2026 CNPA 公开赛',
    published: true,
    mvp: null,
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: [],
    allTournamentPublished: true,
    allDefensivePublished: false,
    championRoster: buildRoster('2026-open', 'dragon'),
  },
  {
    tournamentId: '2025-league-s2',
    championTeamId: 'wolves',
    championTeamName: 'WOLVES',
    championTeamAbbr: 'WOLVES',
    championTeamLogoUrl: '',
    championDate: '2025.12.06',
    trophyImageUrl: '',
    description: '2025 CNPA 联赛第二赛季',
    published: true,
    mvp: buildMvp('2025-league-s2', 10006),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: true,
    allDefensivePublished: true,
    championRoster: buildRoster('2025-league-s2', 'wolves'),
  },
  {
    tournamentId: '2025-arena-s2',
    championTeamId: 'omega',
    championTeamName: 'OMEGA',
    championTeamAbbr: 'OMEGA',
    championTeamLogoUrl: '',
    championDate: '2025.10.19',
    trophyImageUrl: '',
    description: '2025 CNPA Arena 第二赛季',
    published: true,
    mvp: buildMvp('2025-arena-s2', 10008),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: false,
    allDefensivePublished: true,
    championRoster: buildRoster('2025-arena-s2', 'omega'),
  },
  {
    tournamentId: '2025-cup',
    championTeamId: 'unity',
    championTeamName: 'UNITY',
    championTeamAbbr: 'UNITY',
    championTeamLogoUrl: '',
    championDate: '2025.08.11',
    trophyImageUrl: '',
    description: '2025 CNPA 杯赛',
    published: true,
    mvp: buildMvp('2025-cup', 10010),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: true,
    allDefensivePublished: true,
    championRoster: buildRoster('2025-cup', 'unity'),
  },
  {
    tournamentId: '2025-open',
    championTeamId: 'beast',
    championTeamName: 'BEAST',
    championTeamAbbr: 'BEAST',
    championTeamLogoUrl: '',
    championDate: '2025.06.15',
    trophyImageUrl: '',
    description: '2025 CNPA 公开赛',
    published: true,
    mvp: buildMvp('2025-open', 10009),
    allTournamentTeam: buildAllTournamentTeam(),
    allDefensiveTeam: defensivePlayers,
    allTournamentPublished: true,
    allDefensivePublished: true,
    championRoster: buildRoster('2025-open', 'beast'),
  },
]

const championRecords: ChampionRecord[] = [
  createChampion('cr-01', '2026-league-s1', '2026.06.04', 'rng', 'RNG'),
  createChampion('cr-02', '2026-arena-s1', '2026.05.18', 'titan', 'TITAN'),
  createChampion('cr-03', '2026-cup', '2026.04.12', 'phoenix', 'PHOENIX'),
  createChampion('cr-04', '2026-open', '2026.03.08', 'dragon', 'DRAGON'),
  createChampion('cr-05', '2025-league-s2', '2025.12.06', 'wolves', 'WOLVES'),
  createChampion('cr-06', '2025-arena-s2', '2025.10.19', 'omega', 'OMEGA'),
  createChampion('cr-07', '2025-cup', '2025.08.11', 'unity', 'UNITY'),
  createChampion('cr-08', '2025-open', '2025.06.15', 'beast', 'BEAST'),
  createChampion('cr-09', '2024-league-s1', '2024.11.20', 'legend', 'LEGEND', 2024),
  createChampion('cr-10', '2024-arena-s1', '2024.10.08', 'phoenix', 'PHOENIX', 2024),
  createChampion('cr-11', '2024-cup', '2024.08.16', 'titan', 'TITAN', 2024),
  createChampion('cr-12', '2024-open', '2024.06.28', 'wolves', 'WOLVES', 2024),
  createChampion('cr-13', '2023-league-s1', '2023.12.12', 'dragon', 'DRAGON', 2023),
  createChampion('cr-14', '2023-arena-s1', '2023.10.02', 'beast', 'BEAST', 2023),
  createChampion('cr-15', '2023-cup', '2023.08.05', 'rng', 'RNG', 2023),
  createChampion('cr-16', '2023-open', '2023.05.18', 'omega', 'OMEGA', 2023),
]

function createChampion(
  id: string,
  tournamentId: string,
  championDate: string,
  teamId: string,
  teamAbbr: string,
  seasonYear?: number,
): ChampionRecord {
  const tournament = getTournamentById(tournamentId) ?? hallTournaments[0]!
  return {
    id,
    tournamentId,
    seasonYear: seasonYear ?? tournament.seasonYear,
    seriesCode: tournament.seriesCode,
    seriesName: tournament.seriesName,
    tournamentName: tournament.fullName,
    platformCode: tournament.platformCode,
    platformName: tournament.platformName,
    championDate,
    teamId,
    teamName: teamAbbr,
    teamAbbr,
    teamLogoUrl: '',
  }
}

const extraTournaments: HallTournament[] = [
  {
    id: '2024-league-s1',
    seasonYear: 2024,
    seasonName: '2024 CNPA League S1',
    fullName: '2024 CNPA League S1',
    shortName: 'League S1',
    seriesCode: 'LEAGUE',
    seriesName: 'CNPA League',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2024-07-01',
    endDate: '2024-11-20',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2024-arena-s1',
    seasonYear: 2024,
    seasonName: '2024 CNPA Arena S1',
    fullName: '2024 CNPA Arena S1',
    shortName: 'Arena S1',
    seriesCode: 'ARENA',
    seriesName: 'CNPA Arena',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2024-08-01',
    endDate: '2024-10-08',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2024-cup',
    seasonYear: 2024,
    seasonName: '2024 CNPA Cup',
    fullName: '2024 CNPA Cup',
    shortName: 'Cup',
    seriesCode: 'CUP',
    seriesName: 'CNPA Cup',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2024-06-01',
    endDate: '2024-08-16',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2024-open',
    seasonYear: 2024,
    seasonName: '2024 CNPA Open',
    fullName: '2024 CNPA Open',
    shortName: 'Open',
    seriesCode: 'OPEN',
    seriesName: 'CNPA Open',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2024-04-01',
    endDate: '2024-06-28',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2023-league-s1',
    seasonYear: 2023,
    seasonName: '2023 CNPA League S1',
    fullName: '2023 CNPA League S1',
    shortName: 'League S1',
    seriesCode: 'LEAGUE',
    seriesName: 'CNPA League',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2023-07-01',
    endDate: '2023-12-12',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2023-arena-s1',
    seasonYear: 2023,
    seasonName: '2023 CNPA Arena S1',
    fullName: '2023 CNPA Arena S1',
    shortName: 'Arena S1',
    seriesCode: 'ARENA',
    seriesName: 'CNPA Arena',
    platformCode: 'CONSOLE',
    platformName: '主机',
    startDate: '2023-08-01',
    endDate: '2023-10-02',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2023-cup',
    seasonYear: 2023,
    seasonName: '2023 CNPA Cup',
    fullName: '2023 CNPA Cup',
    shortName: 'Cup',
    seriesCode: 'CUP',
    seriesName: 'CNPA Cup',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2023-06-01',
    endDate: '2023-08-05',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
  {
    id: '2023-open',
    seasonYear: 2023,
    seasonName: '2023 CNPA Open',
    fullName: '2023 CNPA Open',
    shortName: 'Open',
    seriesCode: 'OPEN',
    seriesName: 'CNPA Open',
    platformCode: 'PC',
    platformName: 'PC',
    startDate: '2023-03-01',
    endDate: '2023-05-18',
    status: 'FINISHED',
    published: true,
    hasWeeklyBest: false,
  },
]

extraTournaments.forEach((item) => tournamentMap.set(item.id, item))
hallTournaments.push(...extraTournaments)

extraTournaments.forEach((item) => {
  eventHonors.push({
    tournamentId: item.id,
    championTeamId: championRecords.find((record) => record.tournamentId === item.id)?.teamId ?? 'rng',
    championTeamName: championRecords.find((record) => record.tournamentId === item.id)?.teamName ?? 'RNG',
    championTeamAbbr: championRecords.find((record) => record.tournamentId === item.id)?.teamAbbr ?? 'RNG',
    championTeamLogoUrl: '',
    championDate: championRecords.find((record) => record.tournamentId === item.id)?.championDate ?? '2024.01.01',
    trophyImageUrl: '',
    description: item.fullName,
    published: true,
    mvp: null,
    allTournamentTeam: [],
    allDefensiveTeam: [],
    allTournamentPublished: false,
    allDefensivePublished: false,
    championRoster: buildRoster(item.id, championRecords.find((record) => record.tournamentId === item.id)?.teamId ?? 'rng'),
  })
})

export function getAllHallTournaments(): HallTournament[] {
  return [...hallTournaments]
}

export function getWeeklyTournaments(): HallTournament[] {
  return hallTournaments.filter((item) => item.hasWeeklyBest && item.published)
}

export function getEventTournaments(): HallTournament[] {
  return hallTournaments.filter((item) => item.published)
}

export function getTournamentById(id: string | undefined | null): HallTournament | null {
  if (!id) return null
  return tournamentMap.get(id) ?? null
}

export function getDefaultWeeklyTournamentId(): string {
  return '2026-league-s1'
}

export function getDefaultEventTournamentId(): string {
  return '2026-league-s1'
}

export function resolveTournamentId(id: string | undefined | null, fallback: string): string {
  if (id && tournamentMap.has(id)) return id
  return fallback
}

export function getPublishedWeeks(tournamentId: string): TournamentWeek[] {
  return tournamentWeeks
    .filter((item) => item.tournamentId === tournamentId && item.published)
    .sort((a, b) => a.weekNo - b.weekNo)
}

export function getLatestPublishedWeekNo(tournamentId: string): number {
  const weeks = getPublishedWeeks(tournamentId)
  return weeks[weeks.length - 1]?.weekNo ?? 1
}

export function resolveWeekNo(tournamentId: string, week: number | undefined | null): number {
  const weeks = getPublishedWeeks(tournamentId)
  if (!weeks.length) return 1
  if (week && weeks.some((item) => item.weekNo === week)) return week
  return weeks[weeks.length - 1]!.weekNo
}

export function getWeeklyBestRecord(
  tournamentId: string,
  weekNo: number,
): WeeklyBestRecord | null {
  return (
    weeklyBestRecords.find(
      (item) => item.tournamentId === tournamentId && item.weekNo === weekNo && item.published,
    ) ?? null
  )
}

export function getWeekByNo(tournamentId: string, weekNo: number): TournamentWeek | null {
  return getPublishedWeeks(tournamentId).find((item) => item.weekNo === weekNo) ?? null
}

export function getEventHonor(tournamentId: string): EventHonorRecord | null {
  return eventHonors.find((item) => item.tournamentId === tournamentId) ?? null
}

export function getChampionFilterOptions() {
  const years = Array.from(new Set(championRecords.map((item) => item.seasonYear)))
    .sort((a, b) => b - a)
    .map((year) => ({ value: String(year), label: String(year) }))

  return {
    years: [{ value: 'ALL', label: '全部年份' }, ...years],
    platforms: [
      { value: 'ALL', label: '全部平台' },
      { value: 'CONSOLE', label: '主机' },
      { value: 'PC', label: 'PC' },
    ],
    series: [
      { value: 'ALL', label: '全部赛事' },
      { value: 'LEAGUE', label: 'CNPA League' },
      { value: 'ARENA', label: 'CNPA Arena' },
      { value: 'CUP', label: 'CNPA Cup' },
      { value: 'OPEN', label: 'CNPA Open' },
    ],
  }
}

function matchesPlatform(record: ChampionRecord, platform: string): boolean {
  if (platform === 'ALL') return true
  if (platform === 'CONSOLE') {
    return ['CONSOLE', 'PS5', 'XBOX', 'CROSS_PLATFORM', 'MOBILE'].includes(record.platformCode)
  }
  return record.platformCode === platform
}

export function queryChampionRecords(
  year: string,
  platform: string,
  series: string,
  page: number,
  pageSize = 8,
): ChampionPageResult {
  const filtered = championRecords
    .filter((item) => (year === 'ALL' ? true : item.seasonYear === Number(year)))
    .filter((item) => matchesPlatform(item, platform))
    .filter((item) => (series === 'ALL' ? true : item.seriesCode === series))
    .sort((a, b) => {
      if (b.seasonYear !== a.seasonYear) return b.seasonYear - a.seasonYear
      return b.championDate.localeCompare(a.championDate)
    })

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const safePage = Math.min(Math.max(page, 1), totalPages)
  const start = (safePage - 1) * pageSize
  const items = filtered.slice(start, start + pageSize)

  const groupedMap = new Map<number, ChampionRecord[]>()
  items.forEach((item) => {
    const list = groupedMap.get(item.seasonYear) ?? []
    list.push(item)
    groupedMap.set(item.seasonYear, list)
  })

  const groupedByYear = Array.from(groupedMap.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([yearValue, yearItems]) => ({ year: yearValue, items: yearItems }))

  return {
    items,
    total,
    totalPages,
    page: safePage,
    pageSize,
    groupedByYear,
  }
}

export function getHallTournamentIdBySeasonName(seasonName: string): string | null {
  const normalized = seasonName.trim().toLowerCase()
  const tournament = hallTournaments.find(
    (item) =>
      item.fullName.toLowerCase() === normalized ||
      item.seasonName.toLowerCase() === normalized ||
      item.shortName.toLowerCase() === normalized,
  )
  return tournament?.id ?? null
}

export const HALL_STATS = {
  tournamentCount: hallTournaments.length,
  weeklyBestCount: weeklyBestRecords.length,
  eventHonorCount: eventHonors.filter((item) => item.published).length,
  championCount: championRecords.length,
}
