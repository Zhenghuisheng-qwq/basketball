export type MyEventsTabKey = 'available' | 'applications' | 'joined'

export type AvailableEligibilityStatus = 'ELIGIBLE' | 'FULL' | 'NOT_VERIFIED' | 'APPLIED' | 'JOINED'

export type AvailableActionType =
  | 'TEAM_REGISTRATION'
  | 'INDIVIDUAL_REGISTRATION'
  | 'DISABLED'
  | 'GO_APPLICATIONS'
  | 'GO_JOINED'

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
  platform: string
  registrationType: 'TEAM' | 'INDIVIDUAL'
  registrationTypeText: string
  deadline: string
  quotaUsed: number
  quotaTotal: number
  quotaFull: boolean
  eligibilityStatus: AvailableEligibilityStatus
  actionText: string
  actionType: AvailableActionType
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
