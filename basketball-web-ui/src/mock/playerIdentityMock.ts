import type {
  EligibilityLimit,
  IdentityChangeRecord,
  IdentityPlatform,
  PlatformGameIdRecord,
  PlayerApplication,
  PlayerIdentity,
  PlayerIdentityStatus,
} from '@/types/account'
import type { PlayerVerification, PlayerVerificationStatus } from '@/types/account'

export const PLAYER_IDENTITY_STORAGE_PREFIX = 'cnpa_mock_player_identity_'
export const PLATFORM_GAME_IDS_STORAGE_PREFIX = 'cnpa_mock_platform_game_ids_'
export const ELIGIBILITY_STORAGE_PREFIX = 'cnpa_mock_eligibility_'

export function buildPlayerNo(userId: number): string {
  return `CNPA-P-${String(userId).padStart(6, '0')}`
}

export function buildIdentityApplicationNo(userId: number): string {
  const date = '20260720'
  return `CNPA-PV-${date}-${String(userId).padStart(5, '0')}`
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

export const mockPlayerApplication: PlayerApplication = {
  applicationNo: 'CNPA-PV-20260720-00128',
  status: 'PENDING',
  playerName: 'ZLZyoung',
  platform: 'XBOX',
  gameId: 'ZLZyoung2K',
  position: 'PG',
  proofCount: 1,
  submitTime: '2026-07-20 16:38',
  reviewTime: '',
  reviewRemark: '',
}

export const mockRejectedApplication: PlayerApplication = {
  applicationNo: 'CNPA-PV-20260720-00128',
  status: 'REJECTED',
  playerName: 'ZLZyoung',
  platform: 'XBOX',
  gameId: 'ZLZyoung2K',
  position: 'PG',
  proofCount: 1,
  submitTime: '2026-07-20 16:38',
  reviewTime: '2026-07-20 18:20',
  reviewRemark: '现有证明材料不足以确认平台身份，请修改资料后重新提交。',
  rejectedFields: ['proofImage'],
}

export const mockPlatformGameIds: PlatformGameIdRecord[] = [
  {
    platform: 'XBOX',
    gameId: 'ZLZyoung2K',
    status: 'EFFECTIVE',
    statusText: '已生效',
    locked: true,
    lockText: '赛事锁定中',
  },
  {
    platform: 'PS',
    gameId: 'ZLZyoungPS',
    status: 'PENDING',
    statusText: '修改审核中',
    locked: false,
    lockText: '',
  },
  {
    platform: 'PC',
    gameId: '',
    status: 'UNBOUND',
    statusText: '未绑定',
    locked: false,
    lockText: '',
  },
]

export const mockIdentityChangeHistory: IdentityChangeRecord[] = [
  {
    effectiveTime: '2026-06-01 14:20',
    platform: 'XBOX',
    oldGameId: 'ZLZyoung',
    newGameId: 'ZLZyoung2K',
  },
  {
    effectiveTime: '2026-03-15 10:40',
    platform: 'PS',
    oldGameId: 'ZLZyoungPSN',
    newGameId: 'ZLZyoungPS',
  },
  {
    effectiveTime: '2025-11-08 19:30',
    platform: 'XBOX',
    oldGameId: 'ZLZyoung2K25',
    newGameId: 'ZLZyoung',
  },
]

export const mockEligibilityLimit: EligibilityLimit = {
  playerNo: 'CNPA-P-010001',
  status: 'LIMITED',
  statusText: '参赛资格受限',
  tournamentName: '2026 CNPA League',
  permanentIdentityStatus: '继续保留',
  resultTitle: '本届赛事参赛资格已取消',
  resultStatus: '已生效',
  reason: '当前不能加入该赛事球队或取得本届赛事参赛资格。',
  notice: '重新申请球员认证不能解除资格限制；永久球员 ID、历史比赛与荣誉记录不会删除。',
}

function buildPlayerIdentityFromVerification(
  verification: PlayerVerification,
  nickname: string,
  eligibilityLimited: boolean,
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

export function getDefaultPlatformGameIds(userId: number): PlatformGameIdRecord[] {
  if (userId === 10002 || userId === 10005) {
    return JSON.parse(JSON.stringify(mockPlatformGameIds))
  }
  if (userId === 10001) {
    return [
      { platform: 'PC', gameId: '', status: 'UNBOUND', statusText: '未绑定', locked: false, lockText: '' },
      { platform: 'PS', gameId: '', status: 'UNBOUND', statusText: '未绑定', locked: false, lockText: '' },
      { platform: 'XBOX', gameId: '', status: 'UNBOUND', statusText: '未绑定', locked: false, lockText: '' },
    ]
  }
  return JSON.parse(JSON.stringify(mockPlatformGameIds))
}

export function getDefaultIdentityChangeHistory(userId: number): IdentityChangeRecord[] {
  if (userId === 10002 || userId === 10005) {
    return JSON.parse(JSON.stringify(mockIdentityChangeHistory))
  }
  return []
}

export function getDefaultEligibility(userId: number): EligibilityLimit {
  const base = { ...mockEligibilityLimit, playerNo: buildPlayerNo(userId) }
  if (userId === 10002 || userId === 10005) {
    return { ...base, status: 'LIMITED' }
  }
  return { ...base, status: 'OK', statusText: '参赛资格正常' }
}

export function buildPlayerIdentity(
  verification: PlayerVerification,
  nickname: string,
  eligibilityLimited = false,
): PlayerIdentity {
  return buildPlayerIdentityFromVerification(verification, nickname, eligibilityLimited)
}

export function playerIdentityStorageKey(userId: number): string {
  return `${PLAYER_IDENTITY_STORAGE_PREFIX}${userId}`
}

export function platformGameIdsStorageKey(userId: number): string {
  return `${PLATFORM_GAME_IDS_STORAGE_PREFIX}${userId}`
}

export function eligibilityStorageKey(userId: number): string {
  return `${ELIGIBILITY_STORAGE_PREFIX}${userId}`
}

export function applyDevEligibilityOverride(eligibilityLimited: boolean, queryLimited: boolean): boolean {
  if (queryLimited) return true
  return eligibilityLimited
}
