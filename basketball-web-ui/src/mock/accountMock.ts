/**
 * 个人中心 Mock 数据统一导出入口。
 * 实际数据按模块拆分在 mock/account/ 目录，此处便于按规范引用。
 */
export {
  getDefaultAccount,
  accountStorageKey,
} from './account/account'

export {
  getDefaultProfile,
  profileStorageKey,
} from './account/profile'

export {
  getDefaultVerification,
  verificationStorageKey,
  applyDevVerificationOverride,
} from './account/verification'

export {
  getDefaultNotifications,
  notificationsStorageKey,
  countUnread,
} from './account/notifications'

export {
  getDefaultTeamRelations,
  teamRelationsStorageKey,
} from './account/teamRelations'

export {
  getDefaultSecurity,
  securityStorageKey,
  validatePasswordRules,
} from './account/security'

export {
  buildPlayerNo,
  buildPlayerIdentity,
  getDefaultPlatformGameIds,
  getDefaultIdentityChangeHistory,
  getDefaultEligibility,
  platformGameIdsStorageKey,
  eligibilityStorageKey,
  mockPlatformGameIds,
  mockIdentityChangeHistory,
  mockEligibilityLimit,
  mockRejectedApplication,
  mockPlayerApplication,
} from './playerIdentityMock'

export {
  getDefaultMyTournaments,
  myTournamentsStorageKey,
  mockMyTournaments,
} from './tournamentAccountMock'

export {
  mockOpenTournament,
  mockCertifiedPlayerForOpen,
  mockOpenApplicationDraft,
  mockOpenApplicationPending,
  mockOpenApplicationRejected,
  mockOpenApplicationApproved,
  getDefaultOpenApplication,
  openApplicationStorageKey,
  buildOpenApplicationNo,
  isOpenRegistrationDeadlinePassed,
} from './openRegistrationMock'

export type { MockAuthSession, MockLoginResult } from './auth'
