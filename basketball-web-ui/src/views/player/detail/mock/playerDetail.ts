export interface PlayerProfile {
  id: number
  playerName: string
  displayName: string
  avatarUrl: string
  badgeUrl: string
  verified: boolean
  registrationText: string
  currentTeamId: string
  currentTeamName: string
  currentTeamAbbr: string
  currentTeamLogoUrl: string
  teamKey: string
  logoColor: string
  position: string
  platform: string
  currentStage: string
}

export interface PlayerHonorSummary {
  appearances: number
  weeklyBestCount: number
  allStarCount: number
  bestLineupCount: number
  championCount: number
  runnerUpCount: number
  thirdPlaceCount: number
}

export interface PlayerHonor {
  id: number
  seasonName: string
  honorName: string
  honorDate: string
  imageUrl: string
  tournamentId?: string
}

export interface PlayerMatchRecord {
  id: number
  matchId: number
  date: string
  tournamentName: string
  stageName: string
  teamName: string
  teamId: string
  teamLogoUrl: string
  opponentName: string
  opponentId: string
  opponentLogoUrl: string
  result: 'WIN' | 'LOSS'
  teamScore: number
  opponentScore: number
  minutes: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
  fieldGoalsMade: number
  fieldGoalsAttempted: number
  threePointsMade: number
  threePointsAttempted: number
  freeThrowsMade: number
  freeThrowsAttempted: number
}

export interface PlayerCareerStatistics {
  tournamentId: number
  tournamentName: string
  teamId: string
  teamName: string
  teamLogoUrl: string
  platform: string
  games: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
  fieldGoalPercentage: number
  threePointPercentage: number
  freeThrowPercentage: number
}

export interface PlayerSeasonStageOption {
  id: string
  label: string
  tournamentName: string
  stageLabel: string
  games: number
}

export interface PlayerSeasonGame {
  id: number
  date: string
  opponentName: string
  result: 'WIN' | 'LOSS'
  teamScore: number
  opponentScore: number
  points: number
  rebounds: number
  assists: number
  steals: number
  blocks: number
  turnovers: number
  fouls: number
  fieldGoalsMade: number
  fieldGoalsAttempted: number
  threePointsMade: number
  threePointsAttempted: number
  freeThrowsMade: number
  freeThrowsAttempted: number
}

export interface PlayerTeamHistory {
  id: number
  startDate: string
  endDate: string
  teamId: string
  teamName: string
  teamLogoUrl: string
  description: string
  current: boolean
}

export interface PlayerNameHistory {
  id: number
  startDate: string
  endDate: string
  playerName: string
  description: string
  current: boolean
}

export interface PlayerDetailData {
  profile: PlayerProfile
  honorSummary: PlayerHonorSummary
  honors: PlayerHonor[]
  recentMatches: PlayerMatchRecord[]
  allMatches: PlayerMatchRecord[]
  careerStats: PlayerCareerStatistics[]
  seasonStageOptions: PlayerSeasonStageOption[]
  seasonGames: Record<string, PlayerSeasonGame[]>
  teamHistory: PlayerTeamHistory[]
  nameHistory: PlayerNameHistory[]
}

interface PlayerSeed {
  id: number
  playerName: string
  displayName: string
  teamId: string
  teamName: string
  teamAbbr: string
  teamKey: string
  logoColor: string
  position: string
  platform: string
  avg: [number, number, number]
}

const playerSeeds: PlayerSeed[] = [
  { id: 10001, playerName: 'RNG_TitanX', displayName: 'RNG_01', teamId: 'rng', teamName: 'RNG', teamAbbr: 'RNG', teamKey: 'rangers', logoColor: '#c62828', position: 'PG', platform: 'PS5 / Xbox', avg: [21, 5, 9] },
  { id: 10002, playerName: 'TITAN_King', displayName: 'TITAN_King', teamId: 'titan', teamName: 'TITAN', teamAbbr: 'TITAN', teamKey: 'titan', logoColor: '#c9a35a', position: 'SF', platform: 'PS5 / Xbox', avg: [18, 7, 5] },
  { id: 10003, playerName: 'PHX_Fire', displayName: 'PHX_Fire', teamId: 'phoenix', teamName: 'PHOENIX', teamAbbr: 'PHOENIX', teamKey: 'phoenix', logoColor: '#e65100', position: 'SG', platform: 'PS5 / Xbox', avg: [24, 5, 6] },
  { id: 10004, playerName: 'DRG_Lion', displayName: 'DRG_Lion', teamId: 'dragon', teamName: 'DRAGON', teamAbbr: 'DRAGON', teamKey: 'dragon', logoColor: '#2e7d32', position: 'PF', platform: 'PS5 / Xbox', avg: [23, 9, 3] },
  { id: 10005, playerName: 'LEG_Zeus', displayName: 'LEG_Zeus', teamId: 'legend', teamName: 'LEGEND', teamAbbr: 'LEGEND', teamKey: 'legend', logoColor: '#6a1b9a', position: 'C', platform: 'PS5 / Xbox', avg: [22, 11, 3] },
  { id: 10006, playerName: 'WOLVES_Ace', displayName: 'WOLVES_Ace', teamId: 'wolves', teamName: 'WOLVES', teamAbbr: 'WOLVES', teamKey: 'wolves', logoColor: '#1565c0', position: 'PG', platform: 'PS5 / Xbox', avg: [21, 4, 8] },
  { id: 10007, playerName: 'ALLSTAR_Melo', displayName: 'ALLSTAR_Melo', teamId: 'all-star', teamName: 'ALL STAR', teamAbbr: 'ALL STAR', teamKey: 'allstar', logoColor: '#f9a825', position: 'SF', platform: 'PS5 / Xbox', avg: [20, 6, 4] },
  { id: 10008, playerName: 'OMG_Nova', displayName: 'OMG_Nova', teamId: 'omega', teamName: 'OMEGA', teamAbbr: 'OMEGA', teamKey: 'omega', logoColor: '#3949ab', position: 'SG', platform: 'PS5 / Xbox', avg: [19, 4, 5] },
  { id: 10009, playerName: 'BEAST_Hawk', displayName: 'BEAST_Hawk', teamId: 'beast', teamName: 'BEAST', teamAbbr: 'BEAST', teamKey: 'beast', logoColor: '#4e342e', position: 'PF', platform: 'PS5 / Xbox', avg: [17, 9, 3] },
  { id: 10010, playerName: 'UNITY_Jay', displayName: 'UNITY_Jay', teamId: 'unity', teamName: 'UNITY', teamAbbr: 'UNITY', teamKey: 'unity', logoColor: '#00838f', position: 'PG', platform: 'PS5 / Xbox', avg: [17, 4, 7] },
]

const opponents = [
  { id: 'wolves', name: 'WOLVES' },
  { id: 'titan', name: 'TITAN' },
  { id: 'phoenix', name: 'PHOENIX' },
  { id: 'dragon', name: 'DRAGON' },
  { id: 'legend', name: 'LEGEND' },
  { id: 'omega', name: 'OMEGA' },
  { id: 'rng', name: 'RNG' },
  { id: 'all-star', name: 'ALL STAR' },
]

const recentMatchTemplate = [
  { date: '2026-06-10', opponent: 'WOLVES', result: 'WIN' as const, score: [96, 78], line: [21, 5, 9] },
  { date: '2026-06-07', opponent: 'TITAN', result: 'WIN' as const, score: [101, 88], line: [18, 4, 11] },
  { date: '2026-06-04', opponent: 'PHOENIX', result: 'WIN' as const, score: [112, 95], line: [24, 6, 8] },
  { date: '2026-05-31', opponent: 'DRAGON', result: 'LOSS' as const, score: [89, 94], line: [16, 5, 7] },
  { date: '2026-05-28', opponent: 'LEGEND', result: 'WIN' as const, score: [103, 86], line: [20, 4, 10] },
]

function buildProfile(seed: PlayerSeed): PlayerProfile {
  return {
    id: seed.id,
    playerName: seed.playerName,
    displayName: seed.displayName,
    avatarUrl: '',
    badgeUrl: '',
    verified: true,
    registrationText: 'CNPA 2026 赛季注册球员',
    currentTeamId: seed.teamId,
    currentTeamName: seed.teamName,
    currentTeamAbbr: seed.teamAbbr,
    currentTeamLogoUrl: '',
    teamKey: seed.teamKey,
    logoColor: seed.logoColor,
    position: seed.position,
    platform: seed.platform,
    currentStage: '常规赛',
  }
}

function buildMatches(seed: PlayerSeed, count: number): PlayerMatchRecord[] {
  const [ap, ar, aa] = seed.avg
  const records: PlayerMatchRecord[] = []

  for (let i = 0; i < count; i += 1) {
    const tpl = recentMatchTemplate[i % recentMatchTemplate.length]
    const opponent = opponents.find((item) => item.name === tpl.opponent) ?? opponents[i % opponents.length]
    const win = tpl.result === 'WIN'
    const variance = (i % 3) - 1
    records.push({
      id: seed.id * 100 + i + 1,
      matchId: 5000 + seed.id * 10 + i,
      date: tpl.date.replace('2026', String(2026 - Math.floor(i / 5))),
      tournamentName: i % 3 === 0 ? '2026 CNPA League' : i % 3 === 1 ? '2026 CNPA Cup' : '2026 CNPA Open',
      stageName: i % 4 === 0 ? '季后赛' : '常规赛',
      teamName: seed.teamName,
      teamId: seed.teamId,
      teamLogoUrl: '',
      opponentName: opponent.name,
      opponentId: opponent.id,
      opponentLogoUrl: '',
      result: i % 7 === 0 ? 'LOSS' : tpl.result,
      teamScore: win ? tpl.score[0] : tpl.score[1],
      opponentScore: win ? tpl.score[1] : tpl.score[0],
      minutes: 32 + (i % 6),
      points: ap + variance + (i % 2),
      rebounds: ar + (variance > 0 ? 1 : 0),
      assists: aa + variance,
      steals: 1 + (i % 3),
      blocks: seed.position === 'C' || seed.position === 'PF' ? 1 + (i % 2) : i % 4 === 0 ? 1 : 0,
      turnovers: 2 + (i % 2),
      fouls: 2 + (i % 3),
      fieldGoalsMade: 8 + (i % 4),
      fieldGoalsAttempted: 16 + (i % 5),
      threePointsMade: 2 + (i % 3),
      threePointsAttempted: 6 + (i % 4),
      freeThrowsMade: 3 + (i % 2),
      freeThrowsAttempted: 4 + (i % 2),
    })
  }

  return records
}

function buildCareerStats(seed: PlayerSeed): PlayerCareerStatistics[] {
  const base = seed.avg
  return [
    {
      tournamentId: 1,
      tournamentName: '2026 CNPA League · 常规赛',
      teamId: seed.teamId,
      teamName: seed.teamName,
      teamLogoUrl: '',
      platform: seed.platform,
      games: 12,
      points: base[0] + 0.3 * (seed.id % 3),
      rebounds: base[1],
      assists: base[2],
      steals: 1.4,
      blocks: seed.position === 'C' ? 1.8 : 0.6,
      turnovers: 2.8,
      fouls: 2.4,
      fieldGoalPercentage: 48.6,
      threePointPercentage: 37.2,
      freeThrowPercentage: 81.5,
    },
    {
      tournamentId: 2,
      tournamentName: '2025 CNPA Cup',
      teamId: seed.id === 10001 ? 'titan' : seed.teamId,
      teamName: seed.id === 10001 ? 'TITAN' : seed.teamName,
      teamLogoUrl: '',
      platform: 'PS5',
      games: 9,
      points: base[0] - 1.1,
      rebounds: base[1] - 0.3,
      assists: base[2] - 0.8,
      steals: 1.2,
      blocks: 0.5,
      turnovers: 2.5,
      fouls: 2.1,
      fieldGoalPercentage: 46.2,
      threePointPercentage: 35.8,
      freeThrowPercentage: 78.4,
    },
    {
      tournamentId: 3,
      tournamentName: '2025 CNPA Open',
      teamId: seed.id === 10008 ? 'omega' : 'dragon',
      teamName: seed.id === 10008 ? 'OMEGA' : 'DRAGON',
      teamLogoUrl: '',
      platform: 'Xbox',
      games: 11,
      points: base[0] - 2.7,
      rebounds: base[1] - 0.8,
      assists: base[2] - 1.6,
      steals: 1.0,
      blocks: 0.4,
      turnovers: 2.3,
      fouls: 2.0,
      fieldGoalPercentage: 44.8,
      threePointPercentage: 33.6,
      freeThrowPercentage: 76.2,
    },
    {
      tournamentId: 4,
      tournamentName: '2024 CNPA League',
      teamId: 'dragon',
      teamName: 'DRAGON',
      teamLogoUrl: '',
      platform: 'PS5',
      games: 10,
      points: base[0] - 3.5,
      rebounds: base[1] - 1.1,
      assists: base[2] - 1.9,
      steals: 0.9,
      blocks: 0.3,
      turnovers: 2.1,
      fouls: 1.9,
      fieldGoalPercentage: 43.5,
      threePointPercentage: 32.4,
      freeThrowPercentage: 74.8,
    },
    {
      tournamentId: 5,
      tournamentName: '2024 CNPA Cup',
      teamId: 'dragon',
      teamName: 'DRAGON',
      teamLogoUrl: '',
      platform: 'PS5',
      games: 7,
      points: base[0] - 4.4,
      rebounds: base[1] - 1.3,
      assists: base[2] - 2.5,
      steals: 0.8,
      blocks: 0.2,
      turnovers: 2.0,
      fouls: 1.8,
      fieldGoalPercentage: 42.1,
      threePointPercentage: 31.2,
      freeThrowPercentage: 72.6,
    },
  ]
}

function buildSeasonOptions(): PlayerSeasonStageOption[] {
  return [
    { id: '2026-league-regular', label: '2026 CNPA League · 常规赛', tournamentName: '2026 CNPA League', stageLabel: '常规赛', games: 7 },
    { id: '2026-league-playoffs', label: '2026 CNPA League · 季后赛', tournamentName: '2026 CNPA League', stageLabel: '季后赛', games: 5 },
    { id: '2026-arena', label: '2026 CNPA Arena', tournamentName: '2026 CNPA Arena', stageLabel: '常规赛', games: 4 },
    { id: '2026-cup', label: '2026 CNPA Cup', tournamentName: '2026 CNPA Cup', stageLabel: '淘汰赛', games: 6 },
    { id: '2026-open', label: '2026 CNPA Open', tournamentName: '2026 CNPA Open', stageLabel: '公开赛', games: 5 },
  ]
}

function buildSeasonGames(seed: PlayerSeed): Record<string, PlayerSeasonGame[]> {
  const options = buildSeasonOptions()
  const games: Record<string, PlayerSeasonGame[]> = {}
  options.forEach((option, optionIndex) => {
    games[option.id] = Array.from({ length: option.games }, (_, index) => {
      const tpl = recentMatchTemplate[index % recentMatchTemplate.length]
      const win = tpl.result === 'WIN'
      return {
        id: seed.id * 1000 + optionIndex * 10 + index,
        date: tpl.date,
        opponentName: opponents[(index + optionIndex) % opponents.length].name,
        result: index % 5 === 0 ? 'LOSS' : tpl.result,
        teamScore: win ? tpl.score[0] : tpl.score[1],
        opponentScore: win ? tpl.score[1] : tpl.score[0],
        points: tpl.line[0] + (index % 2),
        rebounds: tpl.line[1],
        assists: tpl.line[2],
        steals: 1 + (index % 2),
        blocks: index % 3,
        turnovers: 2 + (index % 2),
        fouls: 2 + (index % 2),
        fieldGoalsMade: 8 + index,
        fieldGoalsAttempted: 16 + index,
        threePointsMade: 2 + (index % 3),
        threePointsAttempted: 6 + (index % 3),
        freeThrowsMade: 3,
        freeThrowsAttempted: 4,
      }
    })
  })
  return games
}

function buildHonors(seed: PlayerSeed): PlayerHonor[] {
  return [
    { id: 1, seasonName: '2024 CNPA Open', honorName: '总冠军', honorDate: '2024.09.28', imageUrl: '', tournamentId: '2024-open' },
    { id: 2, seasonName: '2025 CNPA Cup', honorName: '最佳阵容', honorDate: '2025.08.16', imageUrl: '', tournamentId: '2025-cup' },
    {
      id: 3,
      seasonName: '2026 CNPA League',
      honorName: seed.id === 10001 ? '周最佳球员' : '全明星球员',
      honorDate: `第 ${8 + (seed.id % 3)} 周`,
      imageUrl: '',
      tournamentId: '2026-league-s1',
    },
  ]
}

function buildTeamHistory(seed: PlayerSeed): PlayerTeamHistory[] {
  if (seed.id === 10001) {
    return [
      { id: 1, startDate: '2024.01', endDate: '2024.12', teamId: 'dragon', teamName: 'DRAGON', teamLogoUrl: '', description: '首支参赛队伍', current: false },
      { id: 2, startDate: '2025.01', endDate: '2025.12', teamId: 'titan', teamName: 'TITAN', teamLogoUrl: '', description: '转会加盟', current: false },
      { id: 3, startDate: '2026.01', endDate: '至今', teamId: 'rng', teamName: 'RNG', teamLogoUrl: '', description: '当前效力队伍', current: true },
    ]
  }
  return [
    { id: 1, startDate: '2025.01', endDate: '2025.12', teamId: seed.teamId, teamName: seed.teamName, teamLogoUrl: '', description: '首次注册参赛', current: false },
    { id: 2, startDate: '2026.01', endDate: '至今', teamId: seed.teamId, teamName: seed.teamName, teamLogoUrl: '', description: '当前效力队伍', current: true },
  ]
}

function buildNameHistory(seed: PlayerSeed): PlayerNameHistory[] {
  if (seed.id === 10001) {
    return [
      { id: 1, startDate: '2024.01', endDate: '2024.08', playerName: 'ROOKIE_01', description: '首次注册名称', current: false },
      { id: 2, startDate: '2024.09', endDate: '2025.12', playerName: 'RNG_KING', description: '曾用名称', current: false },
      { id: 3, startDate: '2026.01', endDate: '至今', playerName: seed.displayName, description: '当前名称', current: true },
    ]
  }
  return [
    { id: 1, startDate: '2025.01', endDate: '2025.12', playerName: `${seed.teamAbbr}_ROOKIE`, description: '首次注册名称', current: false },
    { id: 2, startDate: '2026.01', endDate: '至今', playerName: seed.displayName, description: '当前名称', current: true },
  ]
}

function buildDetail(seed: PlayerSeed): PlayerDetailData {
  const allMatches = buildMatches(seed, 18)
  return {
    profile: buildProfile(seed),
    honorSummary: {
      appearances: 42,
      weeklyBestCount: 4 + (seed.id % 4),
      allStarCount: 2 + (seed.id % 3),
      bestLineupCount: 3 + (seed.id % 3),
      championCount: seed.id % 3 === 0 ? 2 : 1,
      runnerUpCount: 1,
      thirdPlaceCount: 1,
    },
    honors: buildHonors(seed),
    recentMatches: allMatches.slice(0, 5),
    allMatches,
    careerStats: buildCareerStats(seed),
    seasonStageOptions: buildSeasonOptions(),
    seasonGames: buildSeasonGames(seed),
    teamHistory: buildTeamHistory(seed),
    nameHistory: buildNameHistory(seed),
  }
}

const playerDetailMap = new Map<number, PlayerDetailData>(
  playerSeeds.map((seed) => [seed.id, buildDetail(seed)]),
)

export function getPlayerDetail(playerId: number | null | undefined): PlayerDetailData | null {
  if (!playerId) return null
  return playerDetailMap.get(playerId) ?? null
}

export function getAllPlayerProfiles(): PlayerProfile[] {
  return playerSeeds.map((seed) => buildProfile(seed))
}

export function getPlayerIdByName(name: string): number | null {
  const normalized = name.trim().toLowerCase()
  const seed = playerSeeds.find(
    (item) =>
      item.playerName.toLowerCase() === normalized ||
      item.displayName.toLowerCase() === normalized,
  )
  return seed?.id ?? null
}

export function searchPlayerProfiles(keyword: string, limit = 8): PlayerProfile[] {
  const query = keyword.trim().toLowerCase()
  if (!query) return []

  return getAllPlayerProfiles()
    .filter(
      (item) =>
        item.displayName.toLowerCase().includes(query) ||
        item.playerName.toLowerCase().includes(query) ||
        item.currentTeamAbbr.toLowerCase().includes(query),
    )
    .slice(0, limit)
}

export const PLAYER_DETAIL_COUNT = playerSeeds.length
