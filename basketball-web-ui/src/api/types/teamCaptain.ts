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
  invalidReason?: string
}
