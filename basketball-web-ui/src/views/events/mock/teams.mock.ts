import type { Team } from '../types/events'

/** 球队 Logo 占位：后续替换为 @/assets/images/teams/*.png */
const teamLogo = (code: string) => `/images/teams/${code.toLowerCase()}.png`

export const teams: Team[] = [
  { id: 1, code: 'RNG', name: 'RNG', shortName: 'RNG', logo: teamLogo('rng'), badgeKey: 'rangers', wins: 12, losses: 2 },
  { id: 2, code: 'TITAN', name: 'TITAN', shortName: 'TITAN', logo: teamLogo('titan'), badgeKey: 'titan', wins: 10, losses: 4 },
  { id: 3, code: 'PHOENIX', name: 'PHOENIX', shortName: 'PHX', logo: teamLogo('phoenix'), badgeKey: 'phoenix', wins: 9, losses: 5 },
  { id: 4, code: 'DRAGON', name: 'DRAGON', shortName: 'DRG', logo: teamLogo('dragon'), badgeKey: 'dragon', wins: 7, losses: 7 },
  { id: 5, code: 'LEGEND', name: 'LEGEND', shortName: 'LEG', logo: teamLogo('legend'), badgeKey: 'legend', wins: 6, losses: 8 },
  { id: 6, code: 'WOLVES', name: 'WOLVES', shortName: 'WLV', logo: teamLogo('wolves'), badgeKey: 'wolves', wins: 5, losses: 9 },
  { id: 7, code: 'ALL STAR', name: 'ALL STAR', shortName: 'AST', logo: teamLogo('allstar'), wins: 6, losses: 8 },
  { id: 8, code: 'OMEGA', name: 'OMEGA', shortName: 'OMG', logo: teamLogo('omega'), wins: 6, losses: 8 },
  { id: 9, code: 'BEAST', name: 'BEAST', shortName: 'BST', logo: teamLogo('beast'), wins: 5, losses: 9 },
  { id: 10, code: 'UNITY', name: 'UNITY', shortName: 'UNT', logo: teamLogo('unity'), wins: 3, losses: 11 },
  { id: 11, code: 'YETI', name: 'YETI', shortName: 'YET', logo: teamLogo('yeti'), wins: 3, losses: 11 },
  { id: 12, code: 'TRIPLE', name: 'TRIPLE', shortName: 'TRP', logo: teamLogo('triple'), wins: 2, losses: 12 },
  { id: 13, code: 'KNIGHT', name: 'KNIGHT', shortName: 'KNT', logo: teamLogo('knight'), wins: 2, losses: 12 },
  { id: 14, code: 'VORTEX', name: 'VORTEX', shortName: 'VTX', logo: teamLogo('vortex'), wins: 1, losses: 13 },
  { id: 15, code: 'NOVA', name: 'NOVA', shortName: 'NVA', logo: teamLogo('nova'), wins: 1, losses: 13 },
  { id: 16, code: 'STORM', name: 'STORM', shortName: 'STM', logo: teamLogo('storm'), wins: 0, losses: 14 },
  { id: 17, code: 'PHX_Fire', name: 'PHX_Fire', shortName: 'PHF', logo: teamLogo('phx_fire'), badgeKey: 'phoenix', wins: 4, losses: 10 },
  { id: 18, code: 'EIK', name: 'EIK', shortName: 'EIK', logo: teamLogo('eik'), wins: 4, losses: 10 },
  { id: 19, code: 'KNIGHTS', name: 'KNIGHTS', shortName: 'KTS', logo: teamLogo('knights'), wins: 3, losses: 11 },
  { id: 20, code: 'INVICTUS', name: 'INVICTUS', shortName: 'INV', logo: teamLogo('invictus'), wins: 2, losses: 12 },
]

export function getTeamById(id: number): Team | undefined {
  return teams.find((team) => team.id === id)
}

export function getTeamMap(): Map<number, Team> {
  return new Map(teams.map((team) => [team.id, team]))
}
