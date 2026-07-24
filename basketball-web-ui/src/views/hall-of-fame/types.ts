export type HallTab = 'weekly' | 'event' | 'champions'

export type PlayerPosition = 'PG' | 'SG' | 'SF' | 'PF' | 'C'

export type RosterRole = 'STARTER' | 'SUBSTITUTE'

export interface ChampionRosterMember {
  id: string
  tournamentId: string
  teamId: string
  playerId: number | string
  playerName: string
  playerAvatarUrl: string
  position: string
  role: RosterRole
  captain: boolean
  appearances: number
  sortOrder: number
}
