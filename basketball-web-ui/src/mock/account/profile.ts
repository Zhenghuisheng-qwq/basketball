import type { AccountProfile } from '@/types/account'
import { buildUserNo } from './account'
import { maskEmail } from '@/composables/useRegisterFlow'

export const PROFILE_STORAGE_KEY_PREFIX = 'cnpa_mock_account_profile_'

export function buildDefaultProfile(params: {
  userId: number
  email: string
  nickname: string
  avatarUrl?: string
  emailVerified?: boolean
  createdAt?: string
}): AccountProfile {
  return {
    userId: params.userId,
    nickname: params.nickname,
    userNo: buildUserNo(params.userId),
    email: params.email,
    maskedEmail: maskEmail(params.email),
    avatarUrl: params.avatarUrl || '',
    emailVerified: params.emailVerified ?? true,
    createdAt: params.createdAt || '2025-11-08T10:00:00+08:00',
  }
}

export const defaultProfilesByUserId: Record<number, AccountProfile> = {
  10001: buildDefaultProfile({
    userId: 10001,
    email: 'cnpa2026@qq.com',
    nickname: 'CNPA_2026',
    createdAt: '2025-10-12T08:30:00+08:00',
  }),
  10002: buildDefaultProfile({
    userId: 10002,
    email: 'captain@cnpa.com',
    nickname: 'RNG_01',
    createdAt: '2025-08-20T14:00:00+08:00',
  }),
  10003: buildDefaultProfile({
    userId: 10003,
    email: 'player.pending@cnpa.com',
    nickname: 'PendingPlayer',
    createdAt: '2026-01-05T09:00:00+08:00',
  }),
  10004: buildDefaultProfile({
    userId: 10004,
    email: 'player.rejected@cnpa.com',
    nickname: 'RejectedPlayer',
    createdAt: '2025-12-01T11:00:00+08:00',
  }),
  10005: buildDefaultProfile({
    userId: 10005,
    email: 'player.change@cnpa.com',
    nickname: 'ChangePending',
    createdAt: '2025-09-15T16:00:00+08:00',
  }),
}

export function profileStorageKey(userId: number): string {
  return `${PROFILE_STORAGE_KEY_PREFIX}${userId}`
}

export function getDefaultProfile(userId: number, email: string, nickname: string): AccountProfile {
  if (defaultProfilesByUserId[userId]) {
    return { ...defaultProfilesByUserId[userId] }
  }
  return buildDefaultProfile({ userId, email, nickname })
}
