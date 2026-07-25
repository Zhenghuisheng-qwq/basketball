import type {
  IdentityPlatform,
  PlayerIdentity,
  PlayerIdentityStatus,
  PlayerVerification,
  PlayerVerificationStatus,
} from '@/types/account'

export function buildPlayerNo(userId: number): string {
  return `CNPA-P-${String(userId).padStart(6, '0')}`
}

export function mapVerificationPlatform(platform: string): IdentityPlatform {
  if (platform === 'PS5' || platform === 'PS') return 'PS'
  if (platform === 'XBOX') return 'XBOX'
  return 'PC'
}

export function mapIdentityPlatformToVerification(platform: IdentityPlatform): 'PC' | 'PS5' | 'XBOX' {
  if (platform === 'PS') return 'PS5'
  if (platform === 'XBOX') return 'XBOX'
  return 'PC'
}

export function buildPlayerIdentity(
  verification: PlayerVerification,
  nickname: string,
  eligibilityLimited = false,
): PlayerIdentity {
  const statusMap: Record<PlayerVerificationStatus, PlayerIdentityStatus> = {
    NOT_APPLIED: 'NOT_SUBMITTED',
    PENDING: 'PENDING',
    REJECTED: 'REJECTED',
    APPROVED: 'APPROVED',
    WITHDRAWN: 'NOT_SUBMITTED',
    CHANGE_PENDING: 'APPROVED',
  }
  let status = statusMap[verification.status]
  if (eligibilityLimited && (verification.status === 'APPROVED' || verification.status === 'CHANGE_PENDING')) {
    status = 'ELIGIBILITY_LIMITED'
  }
  const playerNo =
    verification.status === 'APPROVED' || verification.status === 'CHANGE_PENDING'
      ? buildPlayerNo(verification.userId)
      : ''

  return {
    status,
    playerNo,
    playerName: nickname,
    avatarUrl: '',
    platform: verification.primaryPlatform ? mapVerificationPlatform(verification.primaryPlatform) : '',
    gameId: verification.gameId,
    position: verification.gamePosition || verification.commonPosition || '',
    proofImageUrl: verification.gameIdScreenshotUrl,
    submitTime: verification.submittedAt ? verification.submittedAt.replace('T', ' ').slice(0, 16) : '',
    reviewTime: verification.reviewedAt ? verification.reviewedAt.replace('T', ' ').slice(0, 16) : '',
    effectiveTime: verification.effectiveAt ? verification.effectiveAt.replace('T', ' ').slice(0, 16) : '',
    reviewRemark: verification.reviewerComment,
    permanent: verification.status === 'APPROVED' || verification.status === 'CHANGE_PENDING',
    eligibilityLimited,
  }
}
