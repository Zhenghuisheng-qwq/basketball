export type MyEventsTabKey = 'available' | 'applications' | 'joined'

export type AvailableEligibilityStatus = 'ELIGIBLE' | 'FULL' | 'NOT_VERIFIED' | 'APPLIED' | 'JOINED'

export type AvailableActionType =
  | 'TEAM_REGISTRATION'
  | 'INDIVIDUAL_REGISTRATION'
  | 'DISABLED'
  | 'GO_APPLICATIONS'
  | 'GO_JOINED'
  | 'JOIN_TEAM'
  | 'CONTACT_CAPTAIN'
  | 'REMIND_CAPTAIN'
  | 'VIEW_REGISTRATION'
  | 'MANAGE_REGISTRATION'
  | 'VIEW_EVENT'
  | 'VIEW_ROSTER'
  | 'PENDING_REVIEW'
  | 'GRANT_EXPIRED'

export interface AccountEventTab {
  key: MyEventsTabKey
  label: string
  title: string
  subtitle: string
}

export interface AvailableEvent {
  tournamentId: string
  seasonId?: string
  tournamentName: string
  seasonName?: string
  platform: string
  openedPlatforms?: string[]
  registrationType: 'TEAM' | 'INDIVIDUAL'
  registrationTypeText: string
  deadline: string
  signupStartTime?: string
  matchStartTime?: string
  matchEndTime?: string
  quotaUsed: number
  quotaTotal: number
  quotaFull: boolean
  eligibilityStatus: AvailableEligibilityStatus
  actionText: string
  actionType: AvailableActionType
  actionCode?: string
  disabledReason?: string
  canRegister?: boolean
  teamName?: string
  teamRegistrationStatusText?: string
  userRosterStatusText?: string
  captainGrantStatusText?: string
  registrationStatus?: string
  canRemindCaptain?: boolean
  remindedRecently?: boolean
  availableRegisterPlatforms?: string[]
  eventStatus?: string
  registrationId?: string
}

export type ApplicationActionType =
  | 'CONFIRM_INVITE'
  | 'VIEW_TEAM_APPLICATION'
  | 'VIEW_APPLICATION_STATUS'
  | 'RESUBMIT'

export interface MyApplication {
  applicationId: string
  tournamentId: string
  tournamentName: string
  platform: string
  applicationType: string
  applicationTypeText: string
  status: string
  statusText: string
  statusHint?: string
  invitedTeamCode?: string
  confirmedCount?: number
  rosterMax?: number
  submitTime?: string
  actionText: string
  actionType: ApplicationActionType
  pending?: boolean
  showAction?: boolean
  platformGroupLabel?: string
}

export type ParticipatingActionType =
  | 'MANAGE_EVENT_TEAM'
  | 'VIEW_EVENT_RELATION'
  | 'VIEW_PUBLIC_RECORD'

export interface ParticipatingEvent {
  relationId: string
  tournamentId: string
  tournamentName: string
  platform: string
  participationType: string
  participationTypeText: string
  identityType: string
  identityText: string
  status: string
  statusText: string
  pendingTaskCount?: number
  pendingTaskText?: string
  actionText: string
  actionType: ParticipatingActionType
  actionRoute?: string
  primaryAction?: boolean
}

export interface MyEventsSummary {
  availableCount: number
  applicationPendingCount: number
  joinedRelationCount: number
}
