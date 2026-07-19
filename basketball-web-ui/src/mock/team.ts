import type {
  TeamBase,
  TeamCompareBarStat,
  TeamCompareData,
  TeamMatchRecord,
  TeamPlayerAverageStat,
  TeamPlayerTotalStat,
  TeamRosterPlayer,
  TeamSeasonAverages,
  TeamSingleMatchStats,
  TeamStage,
} from '@/types/team'

export type { TeamStage, TeamMatchStatus } from '@/types/team'

function createTeam(
  id: string,
  code: string,
  opts: Partial<TeamBase> & Pick<TeamBase, 'wins' | 'losses' | 'ranking'>,
): TeamBase {
  return {
    id,
    code,
    name: code,
    shortName: code.length > 6 ? code.slice(0, 3) : code,
    logoUrl: '',
    largeLogoUrl: '',
    captainName: `${code.replace(/\s/g, '')}_01`,
    platform: 'PS5 / Xbox',
    currentStage: 'regular',
    rosterCount: 8,
    verified: true,
    seasonName: '2026 CNPA League',
    seasonYear: 2026,
    description: 'CNPA 2026 赛季参赛战队',
    themeColor: '#c9a360',
    secondaryColor: '#111111',
    ...opts,
  }
}

export const teams: TeamBase[] = [
  {
    id: 'rng',
    code: 'RNG',
    name: 'RNG',
    shortName: 'RNG',
    logoUrl: '',
    largeLogoUrl: '',
    captainName: 'RNG_01',
    platform: 'PS5 / Xbox',
    currentStage: 'regular',
    rosterCount: 8,
    verified: true,
    seasonName: '2026 CNPA League',
    seasonYear: 2026,
    description: 'CNPA 2026 赛季参赛战队',
    wins: 12,
    losses: 2,
    ranking: 1,
    themeColor: '#c49a42',
    secondaryColor: '#111111',
  },
  createTeam('titan', 'TITAN', {
    wins: 10,
    losses: 4,
    ranking: 3,
    themeColor: '#3f8cff',
  }),
  createTeam('wolves', 'WOLVES', { wins: 5, losses: 9, ranking: 8 }),
  createTeam('phoenix', 'PHOENIX', {
    wins: 9,
    losses: 5,
    ranking: 2,
    themeColor: '#e85d04',
  }),
  createTeam('all-star', 'ALL STAR', { shortName: 'ALL', wins: 6, losses: 8, ranking: 6 }),
  createTeam('dragon', 'DRAGON', { wins: 7, losses: 7, ranking: 4, themeColor: '#d62828' }),
  createTeam('legend', 'LEGEND', { wins: 6, losses: 8, ranking: 5 }),
  createTeam('omega', 'OMEGA', { wins: 6, losses: 8, ranking: 7, themeColor: '#7209b7' }),
  createTeam('beast', 'BEAST', { shortName: 'BST', wins: 5, losses: 9, ranking: 9 }),
  createTeam('unity', 'UNITY', { shortName: 'UNT', wins: 3, losses: 11, ranking: 10 }),
  createTeam('phx-fire', 'PHX_Fire', { shortName: 'PHF', wins: 4, losses: 10, ranking: 11 }),
  createTeam('eik', 'EIK', { wins: 4, losses: 10, ranking: 12 }),
  createTeam('storm', 'STORM', { shortName: 'STM', wins: 0, losses: 14, ranking: 16 }),
  createTeam('vortex', 'VORTEX', { shortName: 'VTX', wins: 1, losses: 13, ranking: 14 }),
  createTeam('knights', 'KNIGHTS', { shortName: 'KTS', wins: 3, losses: 11, ranking: 13 }),
  createTeam('invictus', 'INVICTUS', { shortName: 'INV', wins: 2, losses: 12, ranking: 15 }),
]

const teamMap = new Map(teams.map((team) => [team.id, team]))

const defaultTeam = teams[0]!

export function getTeamById(teamId: string): TeamBase {
  const normalized = teamId.toLowerCase().replace(/_/g, '-')
  return teamMap.get(normalized) ?? defaultTeam
}

export function getAllTeams(): TeamBase[] {
  return teams
}

const rngRoster: TeamRosterPlayer[] = [
  { id: 'rng-p01', teamId: 'rng', nickname: 'RNG_01', position: 'PG', avatar: '', isCaptain: true },
  { id: 'rng-p02', teamId: 'rng', nickname: 'RNG_02', position: 'SG', avatar: '' },
  { id: 'rng-p03', teamId: 'rng', nickname: 'RNG_03', position: 'SF', avatar: '' },
  { id: 'rng-p04', teamId: 'rng', nickname: 'RNG_04', position: 'PF', avatar: '' },
  { id: 'rng-p05', teamId: 'rng', nickname: 'RNG_05', position: 'C', avatar: '' },
  { id: 'rng-p06', teamId: 'rng', nickname: 'RNG_06', position: 'PG', avatar: '' },
  { id: 'rng-p07', teamId: 'rng', nickname: 'RNG_07', position: 'SF', avatar: '' },
  { id: 'rng-p08', teamId: 'rng', nickname: 'RNG_08', position: 'PF', avatar: '' },
]

const rosterMap = new Map<string, TeamRosterPlayer[]>([['rng', rngRoster]])

export function getTeamRoster(teamId: string): TeamRosterPlayer[] {
  const normalized = teamId.toLowerCase().replace(/_/g, '-')
  return rosterMap.get(normalized) ?? []
}

const rngRecentMatches: TeamMatchRecord[] = [
  {
    id: 'rng-rm-01',
    teamId: 'rng',
    opponentId: 'wolves',
    date: '2026-06-10',
    time: '20:00',
    teamScore: 96,
    opponentScore: 78,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-02',
    teamId: 'rng',
    opponentId: 'titan',
    date: '2026-06-07',
    time: '20:00',
    teamScore: 101,
    opponentScore: 88,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-03',
    teamId: 'rng',
    opponentId: 'phoenix',
    date: '2026-06-04',
    time: '20:00',
    teamScore: 112,
    opponentScore: 95,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-04',
    teamId: 'rng',
    opponentId: 'dragon',
    date: '2026-05-31',
    time: '20:00',
    teamScore: 89,
    opponentScore: 94,
    result: 'loss',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-05',
    teamId: 'rng',
    opponentId: 'legend',
    date: '2026-05-28',
    time: '20:00',
    teamScore: 103,
    opponentScore: 86,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-p01',
    teamId: 'rng',
    opponentId: 'titan',
    date: '2026-06-18',
    time: '20:00',
    teamScore: 108,
    opponentScore: 102,
    result: 'win',
    status: 'finished',
    stage: 'playoffs',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-p02',
    teamId: 'rng',
    opponentId: 'phoenix',
    date: '2026-06-15',
    time: '20:00',
    teamScore: 97,
    opponentScore: 101,
    result: 'loss',
    status: 'finished',
    stage: 'playoffs',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-rm-p03',
    teamId: 'rng',
    opponentId: 'dragon',
    date: '2026-06-12',
    time: '20:00',
    teamScore: 115,
    opponentScore: 89,
    result: 'win',
    status: 'finished',
    stage: 'playoffs',
    competition: '2026 CNPA League',
  },
]

const recentMatchesMap = new Map<string, TeamMatchRecord[]>([['rng', rngRecentMatches]])

export function getTeamRecentMatches(teamId: string): TeamMatchRecord[] {
  const normalized = teamId.toLowerCase().replace(/_/g, '-')
  return recentMatchesMap.get(normalized) ?? []
}

const rngMatches: TeamMatchRecord[] = [
  {
    id: 'rng-m-20260516',
    teamId: 'rng',
    opponentId: 'unity',
    date: '2026-05-16',
    time: '20:30',
    status: 'scheduled',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260520',
    teamId: 'rng',
    opponentId: 'all-star',
    date: '2026-05-20',
    time: '19:00',
    status: 'scheduled',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260528',
    teamId: 'rng',
    opponentId: 'legend',
    date: '2026-05-28',
    time: '20:00',
    teamScore: 103,
    opponentScore: 86,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260531',
    teamId: 'rng',
    opponentId: 'dragon',
    date: '2026-05-31',
    time: '19:30',
    teamScore: 89,
    opponentScore: 94,
    result: 'loss',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260604',
    teamId: 'rng',
    opponentId: 'phoenix',
    date: '2026-06-04',
    time: '20:30',
    teamScore: 112,
    opponentScore: 95,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260607',
    teamId: 'rng',
    opponentId: 'titan',
    date: '2026-06-07',
    time: '19:00',
    teamScore: 101,
    opponentScore: 88,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260610',
    teamId: 'rng',
    opponentId: 'wolves',
    date: '2026-06-10',
    time: '20:00',
    teamScore: 96,
    opponentScore: 78,
    result: 'win',
    status: 'finished',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
  {
    id: 'rng-m-20260618',
    teamId: 'rng',
    opponentId: 'omega',
    date: '2026-06-18',
    time: '20:00',
    status: 'scheduled',
    stage: 'regular',
    competition: '2026 CNPA League',
  },
]

const matchesMap = new Map<string, TeamMatchRecord[]>([['rng', rngMatches]])

export function getTeamMatches(teamId: string): TeamMatchRecord[] {
  const normalized = teamId.toLowerCase().replace(/_/g, '-')
  return matchesMap.get(normalized) ?? []
}

const seasonStats: Record<string, Partial<Record<TeamStage, TeamSeasonAverages>>> = {
  rng: {
    regular: {
      points: 78.1,
      rebounds: 21.2,
      assists: 18.0,
      steals: 4.7,
      blocks: 1.2,
      fouls: 9.7,
      turnovers: 5.5,
    },
    playoffs: {
      points: 81.2,
      rebounds: 22.8,
      assists: 19.4,
      steals: 5.1,
      blocks: 1.5,
      fouls: 9.2,
      turnovers: 5.0,
    },
  },
}

const playerAverageStats: Record<string, Partial<Record<TeamStage, TeamPlayerAverageStat[]>>> = {
  rng: {
    regular: [
      {
        playerId: 'rng-p01',
        nickname: 'RNG_01',
        avatar: '',
        games: 14,
        points: 18.6,
        rebounds: 3.2,
        assists: 8.1,
        steals: 1.4,
        blocks: 0.1,
        fouls: 2.0,
        turnovers: 1.7,
        fgPct: 54.2,
        tpPct: 43.5,
        ftPct: 82.1,
      },
      {
        playerId: 'rng-p02',
        nickname: 'RNG_02',
        avatar: '',
        games: 14,
        points: 15.3,
        rebounds: 2.8,
        assists: 3.5,
        steals: 1.1,
        blocks: 0.2,
        fouls: 1.8,
        turnovers: 1.5,
        fgPct: 48.6,
        tpPct: 38.2,
        ftPct: 76.4,
      },
      {
        playerId: 'rng-p03',
        nickname: 'RNG_03',
        avatar: '',
        games: 13,
        points: 11.2,
        rebounds: 4.5,
        assists: 2.1,
        steals: 0.9,
        blocks: 0.3,
        fouls: 2.2,
        turnovers: 1.9,
        fgPct: 46.8,
        tpPct: 35.6,
        ftPct: 71.2,
      },
      {
        playerId: 'rng-p04',
        nickname: 'RNG_04',
        avatar: '',
        games: 14,
        points: 13.8,
        rebounds: 6.2,
        assists: 1.8,
        steals: 0.8,
        blocks: 1.1,
        fouls: 2.4,
        turnovers: 1.6,
        fgPct: 51.3,
        tpPct: 32.4,
        ftPct: 68.5,
      },
      {
        playerId: 'rng-p05',
        nickname: 'RNG_05',
        avatar: '',
        games: 14,
        points: 12.5,
        rebounds: 8.4,
        assists: 1.2,
        steals: 0.6,
        blocks: 1.4,
        fouls: 2.1,
        turnovers: 1.4,
        fgPct: 55.8,
        tpPct: 28.6,
        ftPct: 74.3,
      },
      {
        playerId: 'rng-p06',
        nickname: 'RNG_06',
        avatar: '',
        games: 12,
        points: 9.8,
        rebounds: 2.1,
        assists: 4.6,
        steals: 0.7,
        blocks: 0.1,
        fouls: 1.5,
        turnovers: 1.2,
        fgPct: 44.2,
        tpPct: 36.8,
        ftPct: 65.0,
      },
      {
        playerId: 'rng-p07',
        nickname: 'RNG_07',
        avatar: '',
        games: 13,
        points: 10.5,
        rebounds: 3.6,
        assists: 2.4,
        steals: 0.5,
        blocks: 0.2,
        fouls: 1.9,
        turnovers: 1.8,
        fgPct: 42.5,
        tpPct: 33.1,
        ftPct: 62.8,
      },
      {
        playerId: 'rng-p08',
        nickname: 'RNG_08',
        avatar: '',
        games: 11,
        points: 8.2,
        rebounds: 5.1,
        assists: 1.0,
        steals: 0.4,
        blocks: 0.5,
        fouls: 1.7,
        turnovers: 1.1,
        fgPct: 40.8,
        tpPct: 29.5,
        ftPct: 58.6,
      },
    ],
    playoffs: [
      {
        playerId: 'rng-p01',
        nickname: 'RNG_01',
        avatar: '',
        games: 3,
        points: 19.7,
        rebounds: 2.3,
        assists: 8.7,
        steals: 1.3,
        blocks: 0.0,
        fouls: 2.3,
        turnovers: 1.7,
        fgPct: 56.1,
        tpPct: 45.8,
        ftPct: 80.0,
      },
      {
        playerId: 'rng-p02',
        nickname: 'RNG_02',
        avatar: '',
        games: 3,
        points: 16.3,
        rebounds: 2.0,
        assists: 3.7,
        steals: 1.0,
        blocks: 0.3,
        fouls: 1.3,
        turnovers: 1.0,
        fgPct: 50.2,
        tpPct: 39.5,
        ftPct: 78.6,
      },
      {
        playerId: 'rng-p03',
        nickname: 'RNG_03',
        avatar: '',
        games: 3,
        points: 8.7,
        rebounds: 4.0,
        assists: 1.3,
        steals: 1.0,
        blocks: 0.0,
        fouls: 2.0,
        turnovers: 1.3,
        fgPct: 48.5,
        tpPct: 33.3,
        ftPct: 50.0,
      },
      {
        playerId: 'rng-p04',
        nickname: 'RNG_04',
        avatar: '',
        games: 3,
        points: 13.3,
        rebounds: 5.7,
        assists: 1.0,
        steals: 1.0,
        blocks: 1.0,
        fouls: 2.0,
        turnovers: 0.7,
        fgPct: 52.4,
        tpPct: 41.2,
        ftPct: 0.0,
      },
      {
        playerId: 'rng-p05',
        nickname: 'RNG_05',
        avatar: '',
        games: 3,
        points: 11.7,
        rebounds: 9.7,
        assists: 2.7,
        steals: 0.3,
        blocks: 0.3,
        fouls: 1.0,
        turnovers: 1.0,
        fgPct: 54.2,
        tpPct: 0.0,
        ftPct: 0.0,
      },
    ],
  },
}

const playerTotalStats: Record<string, Partial<Record<TeamStage, TeamPlayerTotalStat[]>>> = {
  rng: {
    regular: [
      {
        playerId: 'rng-p01',
        nickname: 'RNG_01',
        avatar: '',
        games: 14,
        points: 260,
        rebounds: 45,
        assists: 113,
        steals: 20,
        blocks: 1,
        fouls: 28,
        turnovers: 24,
        fgPct: 54.2,
        tpPct: 43.5,
        ftPct: 82.1,
      },
      {
        playerId: 'rng-p02',
        nickname: 'RNG_02',
        avatar: '',
        games: 14,
        points: 214,
        rebounds: 39,
        assists: 49,
        steals: 15,
        blocks: 3,
        fouls: 25,
        turnovers: 21,
        fgPct: 48.6,
        tpPct: 38.2,
        ftPct: 76.4,
      },
      {
        playerId: 'rng-p03',
        nickname: 'RNG_03',
        avatar: '',
        games: 13,
        points: 146,
        rebounds: 59,
        assists: 27,
        steals: 12,
        blocks: 4,
        fouls: 29,
        turnovers: 25,
        fgPct: 46.8,
        tpPct: 35.6,
        ftPct: 71.2,
      },
      {
        playerId: 'rng-p04',
        nickname: 'RNG_04',
        avatar: '',
        games: 14,
        points: 193,
        rebounds: 87,
        assists: 25,
        steals: 11,
        blocks: 15,
        fouls: 34,
        turnovers: 22,
        fgPct: 51.3,
        tpPct: 32.4,
        ftPct: 68.5,
      },
      {
        playerId: 'rng-p05',
        nickname: 'RNG_05',
        avatar: '',
        games: 14,
        points: 175,
        rebounds: 118,
        assists: 17,
        steals: 8,
        blocks: 20,
        fouls: 29,
        turnovers: 20,
        fgPct: 55.8,
        tpPct: 28.6,
        ftPct: 74.3,
      },
      {
        playerId: 'rng-p06',
        nickname: 'RNG_06',
        avatar: '',
        games: 12,
        points: 118,
        rebounds: 25,
        assists: 55,
        steals: 8,
        blocks: 1,
        fouls: 18,
        turnovers: 14,
        fgPct: 44.2,
        tpPct: 36.8,
        ftPct: 65.0,
      },
      {
        playerId: 'rng-p07',
        nickname: 'RNG_07',
        avatar: '',
        games: 13,
        points: 137,
        rebounds: 47,
        assists: 31,
        steals: 7,
        blocks: 3,
        fouls: 25,
        turnovers: 23,
        fgPct: 42.5,
        tpPct: 33.1,
        ftPct: 62.8,
      },
      {
        playerId: 'rng-p08',
        nickname: 'RNG_08',
        avatar: '',
        games: 11,
        points: 90,
        rebounds: 56,
        assists: 11,
        steals: 4,
        blocks: 6,
        fouls: 19,
        turnovers: 12,
        fgPct: 40.8,
        tpPct: 29.5,
        ftPct: 58.6,
      },
    ],
    playoffs: [
      {
        playerId: 'rng-p01',
        nickname: 'RNG_01',
        avatar: '',
        games: 3,
        points: 59,
        rebounds: 7,
        assists: 26,
        steals: 4,
        blocks: 0,
        fouls: 7,
        turnovers: 5,
        fgPct: 56.1,
        tpPct: 45.8,
        ftPct: 80.0,
      },
      {
        playerId: 'rng-p02',
        nickname: 'RNG_02',
        avatar: '',
        games: 3,
        points: 49,
        rebounds: 6,
        assists: 11,
        steals: 3,
        blocks: 1,
        fouls: 4,
        turnovers: 3,
        fgPct: 50.2,
        tpPct: 39.5,
        ftPct: 78.6,
      },
      {
        playerId: 'rng-p03',
        nickname: 'RNG_03',
        avatar: '',
        games: 3,
        points: 26,
        rebounds: 12,
        assists: 4,
        steals: 3,
        blocks: 0,
        fouls: 6,
        turnovers: 4,
        fgPct: 48.5,
        tpPct: 33.3,
        ftPct: 50.0,
      },
      {
        playerId: 'rng-p04',
        nickname: 'RNG_04',
        avatar: '',
        games: 3,
        points: 40,
        rebounds: 17,
        assists: 3,
        steals: 3,
        blocks: 3,
        fouls: 6,
        turnovers: 2,
        fgPct: 52.4,
        tpPct: 41.2,
        ftPct: 0.0,
      },
      {
        playerId: 'rng-p05',
        nickname: 'RNG_05',
        avatar: '',
        games: 3,
        points: 35,
        rebounds: 29,
        assists: 8,
        steals: 1,
        blocks: 1,
        fouls: 3,
        turnovers: 3,
        fgPct: 54.2,
        tpPct: 0.0,
        ftPct: 0.0,
      },
    ],
  },
}

const playoffSingleMatchStats: Record<string, Record<string, TeamSingleMatchStats>> = {
  rng: {
    'rng-po-g1': {
      matchId: 'rng-po-g1',
      label: '季后赛 G1',
      players: [
        {
          playerId: 'rng-p01',
          nickname: 'RNG_01',
          avatar: '',
          grade: 'A-',
          points: 20,
          rebounds: 0,
          assists: 9,
          steals: 1,
          blocks: 0,
          fouls: 3,
          turnovers: 2,
          fg: '7/13',
          tp: '6/9',
          ft: '0/0',
        },
        {
          playerId: 'rng-p02',
          nickname: 'RNG_02',
          avatar: '',
          grade: 'B+',
          points: 16,
          rebounds: 0,
          assists: 3,
          steals: 1,
          blocks: 0,
          fouls: 1,
          turnovers: 1,
          fg: '6/9',
          tp: '3/6',
          ft: '1/1',
        },
        {
          playerId: 'rng-p03',
          nickname: 'RNG_03',
          avatar: '',
          grade: 'B-',
          points: 6,
          rebounds: 3,
          assists: 0,
          steals: 1,
          blocks: 0,
          fouls: 2,
          turnovers: 1,
          fg: '3/3',
          tp: '0/0',
          ft: '0/2',
        },
        {
          playerId: 'rng-p04',
          nickname: 'RNG_04',
          avatar: '',
          grade: 'B+',
          points: 12,
          rebounds: 4,
          assists: 0,
          steals: 1,
          blocks: 1,
          fouls: 2,
          turnovers: 0,
          fg: '4/9',
          tp: '4/6',
          ft: '0/0',
        },
        {
          playerId: 'rng-p05',
          nickname: 'RNG_05',
          avatar: '',
          grade: 'A-',
          points: 10,
          rebounds: 13,
          assists: 7,
          steals: 0,
          blocks: 0,
          fouls: 1,
          turnovers: 1,
          fg: '5/8',
          tp: '0/2',
          ft: '0/0',
        },
      ],
      teamTotal: {
        grade: 'B+',
        points: 64,
        rebounds: 20,
        assists: 19,
        steals: 4,
        blocks: 1,
        fouls: 9,
        turnovers: 5,
        fg: '25/42',
        tp: '13/23',
        ft: '1/3',
      },
    },
  },
}

function normalizeTeamId(teamId: string): string {
  return teamId.toLowerCase().replace(/_/g, '-')
}

export function getTeamSeasonStats(teamId: string, stage: TeamStage): TeamSeasonAverages | null {
  return seasonStats[normalizeTeamId(teamId)]?.[stage] ?? null
}

export function getTeamPlayerAverageStats(
  teamId: string,
  stage: TeamStage,
): TeamPlayerAverageStat[] {
  return playerAverageStats[normalizeTeamId(teamId)]?.[stage] ?? []
}

export function getTeamPlayerTotalStats(teamId: string, stage: TeamStage): TeamPlayerTotalStat[] {
  return playerTotalStats[normalizeTeamId(teamId)]?.[stage] ?? []
}

export function getTeamSingleMatchStats(
  teamId: string,
  matchId: string,
): TeamSingleMatchStats | null {
  const teamStats = playoffSingleMatchStats[normalizeTeamId(teamId)]
  if (!teamStats) return null
  return teamStats[matchId] ?? teamStats['rng-po-g1'] ?? null
}

interface CompareSideSnapshot {
  overview: {
    winRate: number
    avgPoints: number
    avgPointsAgainst: number
    avgPointDiff: number
  }
  basics: {
    rebounds: number
    assists: number
    steals: number
    blocks: number
    turnovers: number
    fouls: number
  }
  shooting: {
    fgPct: number
    tpPct: number
    ftPct: number
  }
}

const compareStats: Record<string, Record<string, CompareSideSnapshot>> = {
  'rng|titan': {
    rng: {
      overview: {
        winRate: 85.7,
        avgPoints: 78.1,
        avgPointsAgainst: 69.8,
        avgPointDiff: 8.3,
      },
      basics: {
        rebounds: 21.2,
        assists: 18.0,
        steals: 4.7,
        blocks: 1.2,
        turnovers: 5.5,
        fouls: 9.7,
      },
      shooting: {
        fgPct: 51.6,
        tpPct: 39.4,
        ftPct: 81.3,
      },
    },
    titan: {
      overview: {
        winRate: 71.4,
        avgPoints: 75.6,
        avgPointsAgainst: 72.4,
        avgPointDiff: 3.2,
      },
      basics: {
        rebounds: 22.4,
        assists: 16.8,
        steals: 5.2,
        blocks: 1.5,
        turnovers: 6.3,
        fouls: 8.9,
      },
      shooting: {
        fgPct: 49.8,
        tpPct: 37.2,
        ftPct: 78.6,
      },
    },
  },
}

function buildCompareSide(teamId: string): CompareSideSnapshot {
  const team = getTeamById(teamId)
  const season = getTeamSeasonStats(teamId, 'regular')
  const games = team.wins + team.losses || 1
  const winRate = Math.round((team.wins / games) * 1000) / 10
  const avgPoints = season?.points ?? Math.round((72 + (16 - team.ranking) * 0.6) * 10) / 10
  const avgPointDiff =
    season != null
      ? Math.round((avgPoints - 70 - team.ranking * 0.35) * 10) / 10
      : Math.round((team.wins - team.losses) * 0.45 * 10) / 10
  const avgPointsAgainst = Math.round((avgPoints - avgPointDiff) * 10) / 10

  const basics = season ?? {
    points: avgPoints,
    rebounds: 20 + (16 - team.ranking) * 0.15,
    assists: 16 + (16 - team.ranking) * 0.12,
    steals: 4.2 + (16 - team.ranking) * 0.05,
    blocks: 1 + (16 - team.ranking) * 0.02,
    fouls: 9.5 - (16 - team.ranking) * 0.04,
    turnovers: 6.2 - (16 - team.ranking) * 0.05,
  }

  return {
    overview: {
      winRate,
      avgPoints,
      avgPointsAgainst,
      avgPointDiff,
    },
    basics: {
      rebounds: basics.rebounds,
      assists: basics.assists,
      steals: basics.steals,
      blocks: basics.blocks,
      turnovers: basics.turnovers,
      fouls: basics.fouls,
    },
    shooting: {
      fgPct: Math.round((48 + (16 - team.ranking) * 0.28) * 10) / 10,
      tpPct: Math.round((35 + (16 - team.ranking) * 0.18) * 10) / 10,
      ftPct: Math.round((74 + (16 - team.ranking) * 0.35) * 10) / 10,
    },
  }
}

function createBarStat(
  key: string,
  label: string,
  left: number,
  right: number,
  higherIsBetter: boolean,
): TeamCompareBarStat {
  return { key, label, left, right, higherIsBetter }
}

function assembleCompareData(
  leftSide: CompareSideSnapshot,
  rightSide: CompareSideSnapshot,
): TeamCompareData {
  return {
    overview: {
      winRate: leftSide.overview.winRate,
      avgPoints: leftSide.overview.avgPoints,
      avgPointsAgainst: leftSide.overview.avgPointsAgainst,
      avgPointDiff: leftSide.overview.avgPointDiff,
    },
    basics: [
      createBarStat(
        'rebounds',
        '场均篮板',
        leftSide.basics.rebounds,
        rightSide.basics.rebounds,
        true,
      ),
      createBarStat(
        'assists',
        '场均助攻',
        leftSide.basics.assists,
        rightSide.basics.assists,
        true,
      ),
      createBarStat(
        'steals',
        '场均抢断',
        leftSide.basics.steals,
        rightSide.basics.steals,
        true,
      ),
      createBarStat(
        'blocks',
        '场均盖帽',
        leftSide.basics.blocks,
        rightSide.basics.blocks,
        true,
      ),
      createBarStat(
        'turnovers',
        '场均失误',
        leftSide.basics.turnovers,
        rightSide.basics.turnovers,
        false,
      ),
      createBarStat(
        'fouls',
        '场均犯规',
        leftSide.basics.fouls,
        rightSide.basics.fouls,
        false,
      ),
    ],
    shooting: [
      createBarStat(
        'fgPct',
        '投篮命中率',
        leftSide.shooting.fgPct,
        rightSide.shooting.fgPct,
        true,
      ),
      createBarStat(
        'tpPct',
        '三分命中率',
        leftSide.shooting.tpPct,
        rightSide.shooting.tpPct,
        true,
      ),
      createBarStat(
        'ftPct',
        '罚球命中率',
        leftSide.shooting.ftPct,
        rightSide.shooting.ftPct,
        true,
      ),
    ],
  }
}

export function getCompareData(leftTeamId: string, rightTeamId: string): TeamCompareData {
  const left = normalizeTeamId(leftTeamId)
  const right = normalizeTeamId(rightTeamId)
  const pairKey = [left, right].sort().join('|')
  const pairStats = compareStats[pairKey]

  if (pairStats?.[left] && pairStats?.[right]) {
    return assembleCompareData(pairStats[left], pairStats[right])
  }

  return assembleCompareData(buildCompareSide(left), buildCompareSide(right))
}
