export interface CaptainQualificationDto {
  hasQualification?: boolean
  qualificationNo?: string
  status?: number
  statusText?: string
  paymentConfirmed?: number
  validFrom?: string
  validTo?: string
  isCurrent?: number
  canCreateTeam?: boolean
  canManageTeam?: boolean
  canManageCurrentTeam?: boolean
  cannotManageReason?: string
  canInheritTeam?: boolean
  grantRevoked?: boolean
  runtimeValid?: boolean
  invalidReason?: string
  divisionId?: string
  divisionName?: string
  platformCode?: string
  gameMode?: string
  seriesName?: string
  seasonId?: string
  seasonName?: string
  teamId?: string
  teamName?: string
}
