import { maskEmail } from '@/composables/useRegisterFlow'
import type { CurrentAccount, AccountRole, PlayerVerificationStatus } from '@/types/account'

export const MOCK_ACCOUNT_STORAGE_PREFIX = 'cnpa_mock_account_'

export function buildUserNo(userId: number): string {
  return `CNPA-U-${String(userId).padStart(6, '0')}`
}

export function buildPlayerNo(userId: number): string {
  return `CNPA-P-${String(userId).padStart(6, '0')}`
}

export function buildCurrentAccount(params: {
  userId: number
  email: string
  nickname: string
  avatarUrl?: string
  accountRole: AccountRole
  emailVerified?: boolean
  playerVerificationStatus: PlayerVerificationStatus
  playerNo?: string
  unreadNotificationCount?: number
  createdAt?: string
  lastLoginAt?: string
}): CurrentAccount {
  const masked = maskEmail(params.email)
  const playerNo =
    params.playerNo
    ?? ((params.playerVerificationStatus === 'APPROVED' || params.playerVerificationStatus === 'CHANGE_PENDING')
      ? buildPlayerNo(params.userId)
      : '')
  return {
    userId: params.userId,
    userNo: buildUserNo(params.userId),
    playerNo,
    nickname: params.nickname,
    email: params.email,
    maskedEmail: masked,
    avatarUrl: params.avatarUrl || '',
    accountRole: params.accountRole,
    emailVerified: params.emailVerified ?? true,
    playerVerificationStatus: params.playerVerificationStatus,
    unreadNotificationCount: params.unreadNotificationCount ?? 0,
    createdAt: params.createdAt || '2025-11-08T10:00:00+08:00',
    lastLoginAt: params.lastLoginAt || new Date().toISOString(),
  }
}

export const defaultAccountsByUserId: Record<number, CurrentAccount> = {
  10001: buildCurrentAccount({
    userId: 10001,
    email: 'zlz@example.com',
    nickname: 'ZLZyoung',
    accountRole: 'BASIC_USER',
    playerVerificationStatus: 'NOT_APPLIED',
    unreadNotificationCount: 2,
    createdAt: '2026-07-17T00:00:00+08:00',
  }),
  10002: buildCurrentAccount({
    userId: 10002,
    email: 'captain@cnpa.com',
    nickname: 'RNG_01',
    accountRole: 'CAPTAIN',
    playerVerificationStatus: 'APPROVED',
    unreadNotificationCount: 1,
    createdAt: '2025-08-20T14:00:00+08:00',
  }),
  10003: buildCurrentAccount({
    userId: 10003,
    email: 'player.pending@cnpa.com',
    nickname: 'PendingPlayer',
    accountRole: 'BASIC_USER',
    playerVerificationStatus: 'PENDING',
    unreadNotificationCount: 0,
    createdAt: '2026-01-05T09:00:00+08:00',
  }),
  10004: buildCurrentAccount({
    userId: 10004,
    email: 'player.rejected@cnpa.com',
    nickname: 'RejectedPlayer',
    accountRole: 'BASIC_USER',
    playerVerificationStatus: 'REJECTED',
    unreadNotificationCount: 1,
    createdAt: '2025-12-01T11:00:00+08:00',
  }),
  10005: buildCurrentAccount({
    userId: 10005,
    email: 'player.change@cnpa.com',
    nickname: 'ChangePending',
    accountRole: 'PLAYER',
    playerVerificationStatus: 'CHANGE_PENDING',
    unreadNotificationCount: 0,
    createdAt: '2025-09-15T16:00:00+08:00',
  }),
}

export function getDefaultAccount(userId: number, email: string, nickname: string): CurrentAccount {
  if (defaultAccountsByUserId[userId]) {
    return { ...defaultAccountsByUserId[userId] }
  }
  return buildCurrentAccount({
    userId,
    email,
    nickname,
    accountRole: 'BASIC_USER',
    playerVerificationStatus: 'NOT_APPLIED',
    unreadNotificationCount: 0,
  })
}

export function accountStorageKey(userId: number): string {
  return `${MOCK_ACCOUNT_STORAGE_PREFIX}current_${userId}`
}
