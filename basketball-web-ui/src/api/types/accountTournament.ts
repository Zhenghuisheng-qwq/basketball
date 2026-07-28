export interface AccountTournamentItemDto {
  registrationId: string
  registrationNo?: string
  tournamentId?: string
  tournamentName: string
  seasonId?: string
  seasonName?: string
  platformCode?: string
  platformName?: string
  teamId?: string
  teamName?: string
  teamLogoUrl?: string
  registrationStatus?: number
  rosterRole?: string
  isStarting?: number
  validationStatus?: number
  submittedTime?: string
  tournamentStartTime?: string
  tournamentEndTime?: string
  nextMatch?: unknown
}
