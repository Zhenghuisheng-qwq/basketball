import type { PlayerVerification, PlayerVerificationStatus } from '@/types/account'

export const VERIFICATION_STORAGE_KEY_PREFIX = 'cnpa_mock_player_verification_'

function buildApplicationNo(userId: number, seq = 1): string {
  return `PV-${String(userId).padStart(6, '0')}-${String(seq).padStart(3, '0')}`
}

function baseVerification(userId: number, status: PlayerVerificationStatus): PlayerVerification {
  return {
    id: `pv-${userId}`,
    userId,
    status,
    primaryPlatform: 'PC',
    gameId: '',
    gameIdScreenshotUrl: '',
    gamePosition: 'PG',
    commonPosition: 'SG',
    backupContact: '',
    submittedAt: '',
    reviewedAt: '',
    effectiveAt: '',
    reviewerComment: '',
    rejectedFields: [],
    applicationNo: buildApplicationNo(userId),
  }
}

export const defaultVerificationsByUserId: Record<number, PlayerVerification> = {
  10001: baseVerification(10001, 'NOT_APPLIED'),
  10002: {
    ...baseVerification(10002, 'APPROVED'),
    primaryPlatform: 'PS5',
    gameId: 'RNG_01_2K',
    gameIdScreenshotUrl: '',
    gamePosition: 'SF',
    commonPosition: 'SF',
    backupContact: '微信：RNG01',
    submittedAt: '2025-08-22T10:00:00+08:00',
    reviewedAt: '2025-08-24T15:30:00+08:00',
    effectiveAt: '2025-08-24T15:30:00+08:00',
    currentEffectiveData: {
      primaryPlatform: 'PS5',
      gameId: 'RNG_01_2K',
      gameIdScreenshotUrl: '',
      gamePosition: 'SF',
      commonPosition: 'SF',
      backupContact: '微信：RNG01',
    },
  },
  10003: {
    ...baseVerification(10003, 'PENDING'),
    primaryPlatform: 'XBOX',
    gameId: 'Pending_2K_ID',
    gameIdScreenshotUrl: '',
    gamePosition: 'PG',
    commonPosition: 'PG',
    backupContact: 'QQ：123456',
    submittedAt: '2026-01-10T14:20:00+08:00',
    applicationNo: buildApplicationNo(10003, 1),
  },
  10004: {
    ...baseVerification(10004, 'REJECTED'),
    primaryPlatform: 'PC',
    gameId: 'Rejected_ID',
    gameIdScreenshotUrl: '',
    gamePosition: 'C',
    commonPosition: 'PF',
    backupContact: '',
    submittedAt: '2025-12-05T09:00:00+08:00',
    reviewedAt: '2025-12-06T11:00:00+08:00',
    reviewerComment:
      '游戏 ID 截图信息不完整，暂时无法确认账号归属。请重新上传包含平台账号名称与完整账号页面的清晰截图。',
    rejectedFields: ['gameIdScreenshotUrl'],
    applicationNo: buildApplicationNo(10004, 1),
  },
  10005: {
    ...baseVerification(10005, 'CHANGE_PENDING'),
    primaryPlatform: 'PC',
    gameId: 'ChangePlayer_2K',
    gameIdScreenshotUrl: '',
    gamePosition: 'SG',
    commonPosition: 'SG',
    backupContact: '微信：Change01',
    submittedAt: '2026-01-15T10:00:00+08:00',
    reviewedAt: '2025-09-20T12:00:00+08:00',
    effectiveAt: '2025-09-20T12:00:00+08:00',
    currentEffectiveData: {
      primaryPlatform: 'PC',
      gameId: 'ChangePlayer_2K',
      gameIdScreenshotUrl: '',
      gamePosition: 'SG',
      commonPosition: 'SG',
      backupContact: '微信：Change01',
    },
    pendingChangeData: {
      primaryPlatform: 'PS5',
      gameId: 'ChangePlayer_PS5',
      gameIdScreenshotUrl: '',
      gamePosition: 'SF',
      commonPosition: 'SF',
      backupContact: '微信：Change01',
    },
    applicationNo: buildApplicationNo(10005, 2),
  },
}

export function verificationStorageKey(userId: number): string {
  return `${VERIFICATION_STORAGE_KEY_PREFIX}${userId}`
}

export function getDefaultVerification(userId: number): PlayerVerification {
  if (defaultVerificationsByUserId[userId]) {
    return JSON.parse(JSON.stringify(defaultVerificationsByUserId[userId]))
  }
  return baseVerification(userId, 'NOT_APPLIED')
}

export function applyDevVerificationOverride(
  verification: PlayerVerification,
  override: PlayerVerificationStatus | null,
): PlayerVerification {
  if (!import.meta.env.DEV || !override) return verification
  const next = getDefaultVerification(verification.userId)
  next.status = override
  if (override === 'APPROVED' || override === 'CHANGE_PENDING') {
    next.currentEffectiveData = {
      primaryPlatform: 'PC',
      gameId: 'DEV_PLAYER_ID',
      gameIdScreenshotUrl: '',
      gamePosition: 'PG',
      commonPosition: 'SG',
      backupContact: '',
    }
  }
  if (override === 'CHANGE_PENDING') {
    next.pendingChangeData = {
      primaryPlatform: 'PS5',
      gameId: 'DEV_CHANGE_ID',
      gameIdScreenshotUrl: '',
      gamePosition: 'SF',
      commonPosition: 'SF',
      backupContact: '',
    }
  }
  if (override === 'REJECTED') {
    next.reviewerComment = '开发调试：认证未通过示例。'
    next.rejectedFields = ['gameIdScreenshotUrl']
    next.reviewedAt = new Date().toISOString()
  }
  if (override === 'PENDING') {
    next.submittedAt = new Date().toISOString()
    next.primaryPlatform = 'PC'
    next.gameId = 'DEV_PENDING_ID'
    next.gamePosition = 'PG'
    next.commonPosition = 'PG'
  }
  return next
}
