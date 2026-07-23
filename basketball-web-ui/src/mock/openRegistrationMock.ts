import type { CertifiedPlayerForOpen, OpenApplication, OpenTournament } from '@/types/account'

export const OPEN_REGISTRATION_STORAGE_PREFIX = 'cnpa_mock_open_application_'

export const mockOpenTournament: OpenTournament = {
  id: '2026-open',
  name: '2026 CNPA Open',
  shortName: 'CNPA Open',
  seasonName: '2026 CNPA Open',
  type: 'OPEN',
  platform: 'Xbox',
  registrationType: 'INDIVIDUAL',
  registrationTypeText: '个人报名',
  registrationDeadline: '2026-08-20 23:59',
  quotaTotal: 120,
  quotaUsed: 86,
  quotaLeft: 34,
  status: 'REGISTRATION_OPEN',
  statusText: '报名开放',
}

export const mockCertifiedPlayerForOpen: CertifiedPlayerForOpen = {
  userNo: 'CNPA-U-000128',
  playerNo: 'CNPA-P-000128',
  nickname: 'ZLZyoung',
  avatarUrl: '',
  identityStatus: 'APPROVED',
  identityStatusText: '认证球员',
  platform: 'Xbox',
  gameId: 'ZLZyoung2K',
}

export const mockOpenApplicationDraft: OpenApplication = {
  applicationNo: '',
  tournamentId: '2026-open',
  tournamentName: '2026 CNPA Open',
  applicationType: 'INDIVIDUAL',
  applicationTypeText: '个人报名',
  status: 'NOT_SUBMITTED',
  statusText: '未提交',
  playerNo: 'CNPA-P-000128',
  nickname: 'ZLZyoung',
  platform: 'Xbox',
  gameId: 'ZLZyoung2K',
  submitTime: '',
  reviewTime: '',
  reviewRemark: '',
}

export const mockOpenApplicationPending: OpenApplication = {
  applicationNo: 'CNPA-OR-20260721-00128',
  tournamentId: '2026-open',
  tournamentName: '2026 CNPA Open',
  applicationType: 'INDIVIDUAL',
  applicationTypeText: '个人报名',
  status: 'PENDING',
  statusText: '审核中',
  playerNo: 'CNPA-P-000128',
  nickname: 'ZLZyoung',
  platform: 'Xbox',
  gameId: 'ZLZyoung2K',
  submitTime: '2026-07-21 11:20',
  reviewTime: '',
  reviewRemark: '',
}

export const mockOpenApplicationRejected: OpenApplication = {
  applicationNo: 'CNPA-OR-20260721-00128',
  tournamentId: '2026-open',
  tournamentName: '2026 CNPA Open',
  applicationType: 'INDIVIDUAL',
  applicationTypeText: '个人报名',
  status: 'REJECTED',
  statusText: '未通过',
  playerNo: 'CNPA-P-000128',
  nickname: 'ZLZyoung',
  platform: 'Xbox',
  gameId: 'ZLZyoung2K',
  submitTime: '2026-07-21 11:20',
  reviewTime: '2026-07-21 18:20',
  reviewRemark: '当前报名资格未通过，请核对报名资料后重新提交。',
}

export const mockOpenApplicationApproved: OpenApplication = {
  applicationNo: 'CNPA-OR-20260721-00128',
  tournamentId: '2026-open',
  tournamentName: '2026 CNPA Open',
  applicationType: 'INDIVIDUAL',
  applicationTypeText: '个人报名',
  status: 'APPROVED',
  statusText: '审核通过',
  participationStatus: 'ACTIVE',
  participationStatusText: '正式参赛',
  groupingStatusText: '等待分组',
  playerNo: 'CNPA-P-000128',
  nickname: 'ZLZyoung',
  platform: 'Xbox',
  gameId: 'ZLZyoung2K',
  submitTime: '2026-07-21 11:20',
  reviewTime: '2026-07-21 18:20',
  reviewRemark: '',
}

export function openApplicationStorageKey(userId: number): string {
  return `${OPEN_REGISTRATION_STORAGE_PREFIX}${userId}`
}

export function buildOpenApplicationNo(userId: number): string {
  return `CNPA-OR-20260721-${String(userId).padStart(5, '0')}`
}

export function getDefaultOpenApplication(userId: number): OpenApplication {
  const draft = { ...mockOpenApplicationDraft, playerNo: `CNPA-P-${String(userId).padStart(6, '0')}` }
  return JSON.parse(JSON.stringify(draft))
}

export function applyDevOpenApplicationOverride(
  application: OpenApplication,
  override: OpenApplication['status'] | null,
  userId: number,
): OpenApplication {
  if (!import.meta.env.DEV || !override || override === 'NOT_SUBMITTED') {
    return application
  }
  const templates: Record<string, OpenApplication> = {
    PENDING: mockOpenApplicationPending,
    REJECTED: mockOpenApplicationRejected,
    APPROVED: mockOpenApplicationApproved,
    CANCELLED: { ...mockOpenApplicationRejected, status: 'CANCELLED', statusText: '已取消' },
    CLOSED: { ...mockOpenApplicationDraft, status: 'CLOSED', statusText: '报名关闭' },
  }
  const base = templates[override] || application
  return {
    ...JSON.parse(JSON.stringify(base)),
    playerNo: `CNPA-P-${String(userId).padStart(6, '0')}`,
    applicationNo: buildOpenApplicationNo(userId),
  }
}

export function isOpenRegistrationDeadlinePassed(deadline: string): boolean {
  const normalized = deadline.replace(' ', 'T')
  const deadlineMs = Date.parse(`${normalized}:00+08:00`)
  if (Number.isNaN(deadlineMs)) return false
  return Date.now() > deadlineMs
}
