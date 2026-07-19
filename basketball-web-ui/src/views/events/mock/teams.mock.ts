import type { Team } from '../types/events'

/** 球队 Logo 占位：后续替换为 @/assets/images/teams/*.png */
const teamLogo = (code: string) => `/images/teams/${code.toLowerCase()}.png`

export const teams: Team[] = [
  { id: 1, teamId: 'rng', code: 'RNG', name: 'RNG', shortName: 'RNG', logo: teamLogo('rng'), badgeKey: 'rangers', wins: 12, losses: 2 },
  { id: 2, teamId: 'titan', code: 'TITAN', name: 'TITAN', shortName: 'TITAN', logo: teamLogo('titan'), badgeKey: 'titan', wins: 10, losses: 4 },
  { id: 3, teamId: 'phoenix', code: 'PHOENIX', name: 'PHOENIX', shortName: 'PHOENIX', logo: teamLogo('phoenix'), badgeKey: 'phoenix', wins: 9, losses: 5 },
  { id: 4, teamId: 'dragon', code: 'DRAGON', name: 'DRAGON', shortName: 'DRAGON', logo: teamLogo('dragon'), badgeKey: 'dragon', wins: 7, losses: 7 },
  { id: 5, teamId: 'legend', code: 'LEGEND', name: 'LEGEND', shortName: 'LEGEND', logo: teamLogo('legend'), badgeKey: 'legend', wins: 6, losses: 8 },
  { id: 6, teamId: 'wolves', code: 'WOLVES', name: 'WOLVES', shortName: 'WOLVES', logo: teamLogo('wolves'), badgeKey: 'wolves', wins: 5, losses: 9 },
  { id: 7, teamId: 'all-star', code: 'ALL STAR', name: 'ALL STAR', shortName: 'ALL STAR', logo: teamLogo('allstar'), badgeKey: 'allstar', wins: 6, losses: 8 },
  { id: 8, teamId: 'omega', code: 'OMEGA', name: 'OMEGA', shortName: 'OMEGA', logo: teamLogo('omega'), badgeKey: 'omega', wins: 6, losses: 8 },
  { id: 9, teamId: 'beast', code: 'BEAST', name: 'BEAST', shortName: 'BST', logo: teamLogo('beast'), wins: 5, losses: 9 },
  { id: 10, teamId: 'unity', code: 'UNITY', name: 'UNITY', shortName: 'UNT', logo: teamLogo('unity'), wins: 3, losses: 11 },
  { id: 11, teamId: 'yeti', code: 'YETI', name: 'YETI', shortName: 'YET', logo: teamLogo('yeti'), wins: 3, losses: 11 },
  { id: 12, teamId: 'triple', code: 'TRIPLE', name: 'TRIPLE', shortName: 'TRP', logo: teamLogo('triple'), wins: 2, losses: 12 },
  { id: 13, teamId: 'knight', code: 'KNIGHT', name: 'KNIGHT', shortName: 'KNT', logo: teamLogo('knight'), wins: 2, losses: 12 },
  { id: 14, teamId: 'vortex', code: 'VORTEX', name: 'VORTEX', shortName: 'VTX', logo: teamLogo('vortex'), wins: 1, losses: 13 },
  { id: 15, teamId: 'nova', code: 'NOVA', name: 'NOVA', shortName: 'NVA', logo: teamLogo('nova'), wins: 1, losses: 13 },
  { id: 16, teamId: 'storm', code: 'STORM', name: 'STORM', shortName: 'STM', logo: teamLogo('storm'), wins: 0, losses: 14 },
  { id: 17, teamId: 'phx-fire', code: 'PHX_Fire', name: 'PHX_Fire', shortName: 'PHF', logo: teamLogo('phx_fire'), badgeKey: 'phoenix', wins: 4, losses: 10 },
  { id: 18, teamId: 'eik', code: 'EIK', name: 'EIK', shortName: 'EIK', logo: teamLogo('eik'), wins: 4, losses: 10 },
  { id: 19, teamId: 'knights', code: 'KNIGHTS', name: 'KNIGHTS', shortName: 'KTS', logo: teamLogo('knights'), wins: 3, losses: 11 },
  { id: 20, teamId: 'invictus', code: 'INVICTUS', name: 'INVICTUS', shortName: 'INV', logo: teamLogo('invictus'), wins: 2, losses: 12 },
]

export function getTeamById(id: number): Team | undefined {
  return teams.find((team) => team.id === id)
}

export function getTeamMap(): Map<number, Team> {
  return new Map(teams.map((team) => [team.id, team]))
}
