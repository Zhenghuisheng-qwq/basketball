import type { RouteLocationNormalized } from 'vue-router'
import { i18n } from '@/i18n'

const routeTitleKeys: Record<string, string> = {
  Home: 'nav.titles.home',
  UserAgreement: 'legal.userAgreement.title',
  PrivacyPolicy: 'legal.privacy.title',
  Tournament: 'nav.titles.events',
  V4SeasonDetail: 'nav.titles.eventDetail',
  V4MatchDetail: 'nav.titles.matchDetail',
  V4MatchSeriesDetail: 'nav.titles.seriesDetail',
  Data: 'nav.titles.data',
  PlayerDetail: 'nav.titles.playerDetail',
  HallOfFame: 'nav.titles.hallOfFame',
  AnnouncementList: 'nav.titles.announcements',
  AnnouncementDetail: 'nav.titles.announcementDetail',
  Partners: 'nav.titles.partners',
  RegisterCreate: 'nav.titles.registerCreate',
  RegisterSuccess: 'nav.titles.registerSuccess',
  Login: 'nav.titles.loginAccount',
  ForgotPassword: 'nav.titles.forgotPassword',
  AccountOverview: 'nav.titles.account',
  AccountProfile: 'nav.titles.profile',
  AccountPlayerIdentity: 'nav.titles.playerIdentity',
  AccountPlayerIdentityApply: 'nav.titles.playerIdentityApply',
  AccountPlayerIdentityStatusRedirect: 'nav.titles.playerIdentityStatus',
  AccountPlayerIdentityStatusPending: 'nav.titles.playerIdentityPending',
  AccountPlayerIdentityStatusRejected: 'nav.titles.playerIdentityRejected',
  AccountPlayerIdentityStatusApproved: 'nav.titles.playerIdentityApproved',
  AccountPlatformGameId: 'nav.titles.platformGameId',
  AccountIdentityChangeHistory: 'nav.titles.identityHistory',
  AccountPlayerEligibility: 'nav.titles.eligibility',
  AccountMyEvents: 'nav.titles.myEvents',
  AccountV4Registration: 'nav.titles.teamRegistration',
  AccountV4PersonalRegistration: 'nav.titles.personalRegistration',
  AccountV4MatchSubmission: 'nav.titles.matchSubmission',
  AccountV4SeriesSubmission: 'nav.titles.seriesSubmission',
  AccountNotifications: 'nav.titles.notifications',
  AccountNotificationDetail: 'nav.titles.notificationDetail',
  AccountSettings: 'nav.titles.settings',
  AccountMyTeam: 'nav.titles.myTeam',
  AccountTeamMemberDetail: 'nav.titles.teamMember',
  TeamCompare: 'nav.titles.teamCompare',
  TeamOverview: 'nav.titles.teamOverview',
  TeamMatches: 'nav.titles.teamMatches',
  TeamStats: 'nav.titles.teamStats',
  TeamArchive: 'nav.titles.teamArchive',
}

export function applyRouteDocumentTitle(to: RouteLocationNormalized): void {
  const name = typeof to.name === 'string' ? to.name : ''
  const key = routeTitleKeys[name]
  const page = key
    ? String(i18n.global.t(key))
    : String(to.meta.title || i18n.global.t('common.brandShort'))
  document.title = String(i18n.global.t('common.documentTitle', { page }))
}
