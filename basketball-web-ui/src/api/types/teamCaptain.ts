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
  seasonId?: string
  seasonName?: string
  teamId?: string
  teamName?: string
}
