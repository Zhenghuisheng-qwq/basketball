import type { PageResult } from '@/api/types/common'

export type MyEventsRoleMode = 'NO_TEAM' | 'TEAM_MEMBER' | 'TEAM_CAPTAIN'

export interface MyEventsContextDto {
  hasTeam?: boolean
  teamId?: string
  teamName?: string
  teamRole?: 'CAPTAIN' | 'MEMBER' | 'NONE'
  isTeamCaptain?: boolean
  hasActiveCaptainGrant?: boolean
  captainGrantSeasonId?: string
  captainGrantStatus?: string
  captainGrantStatusText?: string
  captainGrantExpireTime?: string
  currentRoleMode?: MyEventsRoleMode
  canManageTeamRegistration?: boolean
  teamCertificationStatus?: number
  activeCaptainGrantSeasonIds?: string[]
  mode?: MyEventsRoleMode
}

export interface TeamEventRecordItemDto {
  registrationId: string
  seasonId?: string
  tournamentId?: string
  tournamentName?: string
  seasonName?: string
  registrationStatus?: number
  registrationStatusText?: string
  auditStatusText?: string
  rosterStatusText?: string
  participationStatusText?: string
  submitTime?: string
  auditTime?: string
  rejectReason?: string
}

export type TeamEventRecordPageDto = PageResult<TeamEventRecordItemDto>

export type MyEventsActionCode =
  | 'JOIN_TEAM'
  | 'CONTACT_CAPTAIN'
  | 'REMIND_CAPTAIN'
  | 'NO_GRANT'
  | 'NOT_STARTED'
  | 'CLOSED'
  | 'REGISTER'
  | 'REGISTERED'
  | 'VIEW_REGISTRATION'
  | 'MANAGE_REGISTRATION'
  | 'DISABLED'
