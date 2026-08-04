import { createRouter, createWebHistory } from 'vue-router'

import { resolveAuthenticatedEntryRedirect } from '@/utils/auth'
import { useUserStore } from '@/stores/user'



const router = createRouter({

  history: createWebHistory(import.meta.env.BASE_URL),

  routes: [

    {

      path: '/',

      redirect: '/home',

    },

    {

      path: '/home',

      name: 'Home',

      component: () => import('@/views/home/index.vue'),

      meta: { title: '首页' },

    },

    {

      path: '/tournament',

      name: 'Tournament',

      component: () => import('@/views/events/v4/V4EventsListPage.vue'),

      meta: { title: '赛事' },

    },

    {

      path: '/events',

      redirect: (to) => ({

        path: '/tournament',

        query: to.query,

      }),

    },

    {

      path: '/events/:seasonId',

      name: 'V4SeasonDetail',

      component: () => import('@/views/events/v4/V4SeasonDetailPage.vue'),

      meta: { title: '赛事详情' },

    },

    {

      path: '/events/match/:matchId',

      name: 'V4MatchDetail',

      component: () => import('@/views/events/v4/V4MatchDetailPage.vue'),

      meta: { title: '比赛详情' },

    },

        {

      path: '/data',

      name: 'Data',

      component: () => import('@/views/data/V4DataPage.vue'),

      meta: { title: '数据' },

    },

        {

      path: '/player/:playerId',

      name: 'PlayerDetail',

      component: () => import('@/views/player/detail/index.vue'),

      meta: { title: '球员详情' },

    },

    {

      path: '/hall-of-fame',

      name: 'HallOfFame',

      component: () => import('@/views/hall-of-fame/index.vue'),

      meta: { title: '名人堂' },

    },

    {

      path: '/announcements',

      name: 'AnnouncementList',

      component: () => import('@/views/announcement/index.vue'),

      meta: { title: '公告中心' },

    },

    {

      path: '/announcements/:announcementId',

      name: 'AnnouncementDetail',

      component: () => import('@/views/announcement/detail.vue'),

      meta: { title: '公告详情' },

    },

    {

      path: '/partners',

      name: 'Partners',

      component: () => import('@/views/partners/index.vue'),

      meta: { title: '合作伙伴' },

    },

    {

      path: '/notice',

      redirect: '/announcements',

    },

    {

      path: '/register',

      component: () => import('@/layouts/MemberAuthLayout.vue'),

      meta: { title: '注册', fullBleed: true, layout: 'member-auth' },

      children: [

        {

          path: '',

          name: 'RegisterCreate',

          component: () => import('@/views/account/RegisterCreate.vue'),

          meta: { title: '创建账号', guestOnly: true, authContentVariant: 'register' },

        },

        {

          path: 'success',

          name: 'RegisterSuccess',

          component: () => import('@/views/account/RegisterSuccess.vue'),

          meta: { title: '注册完成', authContentVariant: 'complete' },

        },

      ],

    },

    {

      path: '/login',

      component: () => import('@/layouts/MemberAuthLayout.vue'),

      meta: { title: '登录', guestOnly: true, fullBleed: true, layout: 'member-auth' },

      children: [

        {

          path: '',

          name: 'Login',

          component: () => import('@/views/auth/login/index.vue'),

          meta: { title: '账号登录', guestOnly: true, authContentVariant: 'login' },

        },

      ],

    },

    {

      path: '/forgot-password',

      component: () => import('@/layouts/MemberAuthLayout.vue'),

      meta: { title: '找回密码', guestOnly: true, fullBleed: true, layout: 'member-auth' },

      children: [

        {

          path: '',

          name: 'ForgotPassword',

          component: () => import('@/views/account/ForgotPassword.vue'),

          meta: { title: '找回密码', guestOnly: true, authContentVariant: 'login' },

        },

      ],

    },

    {

      path: '/account',

      component: () => import('@/layouts/MemberAccountLayout.vue'),

      meta: { title: '个人中心', requiresAuth: true },

      children: [

        {

          path: '',

          redirect: { name: 'AccountOverview' },

        },

        {

          path: 'overview',

          name: 'AccountOverview',

          component: () => import('@/views/account/overview/index.vue'),

          meta: { title: '个人中心' },

        },

        {

          path: 'profile',

          name: 'AccountProfile',

          component: () => import('@/views/account/profile/index.vue'),

          meta: { title: '个人资料' },

        },

        {

          path: 'player-verification',

          redirect: { name: 'AccountPlayerIdentity' },

        },

        {

          path: 'player-identity',

          name: 'AccountPlayerIdentity',

          component: () => import('@/views/account/player-identity/index.vue'),

          meta: { title: '球员身份' },

        },

        {

          path: 'player-identity/apply',

          name: 'AccountPlayerIdentityApply',

          component: () => import('@/views/account/player-identity/apply.vue'),

          meta: { title: '球员认证申请' },

        },

        {

          path: 'player-identity/status',

          name: 'AccountPlayerIdentityStatusRedirect',

          component: () => import('@/views/account/player-identity/status-redirect.vue'),

          meta: { title: '球员认证状态' },

        },

        {

          path: 'player-identity/status/pending',

          name: 'AccountPlayerIdentityStatusPending',

          component: () => import('@/views/account/player-identity/status.vue'),

          props: { statusMode: 'pending' },

          meta: { title: '球员认证审核中' },

        },

        {

          path: 'player-identity/status/rejected',

          name: 'AccountPlayerIdentityStatusRejected',

          component: () => import('@/views/account/player-identity/status.vue'),

          props: { statusMode: 'rejected' },

          meta: { title: '球员认证未通过' },

        },

        {

          path: 'player-identity/status/approved',

          name: 'AccountPlayerIdentityStatusApproved',

          component: () => import('@/views/account/player-identity/status.vue'),

          props: { statusMode: 'approved' },

          meta: { title: '球员认证已通过' },

        },

        {

          path: 'player-identity/pending',

          redirect: { name: 'AccountPlayerIdentityStatusPending' },

        },

        {

          path: 'player-identity/rejected',

          redirect: { name: 'AccountPlayerIdentityStatusRejected' },

        },

        {

          path: 'player-identity/approved',

          redirect: { name: 'AccountPlayerIdentityStatusApproved' },

        },

        {

          path: 'player-identity/platforms',

          name: 'AccountPlatformGameId',

          component: () => import('@/views/account/player-identity/platforms.vue'),

          meta: { title: '平台与游戏 ID' },

        },

        {

          path: 'player-identity/change-history',

          name: 'AccountIdentityChangeHistory',

          component: () => import('@/views/account/player-identity/change-history.vue'),

          meta: { title: '资料变更记录' },

        },

        {

          path: 'player-identity/history',

          redirect: { name: 'AccountIdentityChangeHistory' },

        },

        {

          path: 'player-identity/eligibility',

          name: 'AccountPlayerEligibility',

          component: () => import('@/views/account/player-identity/eligibility.vue'),

          meta: { title: '球员资格状态' },

        },

        {

          path: 'events',

          redirect: { name: 'AccountMyEvents' },

        },

        {

          path: 'events/open/register',

          redirect: { name: 'AccountMyEvents', query: { tab: 'available' } },

        },

        {

          path: 'events/open/status',

          redirect: { name: 'AccountMyEvents', query: { tab: 'applications' } },

        },

                                                                {

          path: 'my-events',

          name: 'AccountMyEvents',

          component: () => import('@/views/account/events/MyEventsView.vue'),

          meta: { title: '我的赛事' },

        },

        {

          path: 'my-events/v4/registration/:divisionId',

          name: 'AccountV4Registration',

          component: () => import('@/views/account/events/V4RegistrationFlow.vue'),

          meta: { title: '球队报名' },

        },

        {

          path: 'my-events/v4/personal-registration/:divisionId',

          name: 'AccountV4PersonalRegistration',

          component: () => import('@/views/account/events/V4PersonalRegistrationFlow.vue'),

          meta: { title: '个人报名' },

        },

        {

          path: 'my-events/v4/match-submission/:matchId',

          name: 'AccountV4MatchSubmission',

          component: () => import('@/views/account/events/V4MatchSubmissionView.vue'),

          meta: { title: '比赛数据提交' },

        },

        {

          path: 'my-events/2026-open/individual-registration',

          redirect: { name: 'AccountMyEvents', query: { tab: 'available' } },

        },

        {

          path: 'my-events/2026-open/application-status',

          redirect: { name: 'AccountMyEvents', query: { tab: 'available' } },

        },

        {

          path: 'my-events/:eventTeamId/matches',

          redirect: { name: 'AccountMyEvents', query: { tab: 'schedule' } },

        },

        {

          path: 'my-events/:eventTeamId/matches/:matchId',

          redirect: (to) => ({
            name: 'AccountV4MatchSubmission',
            params: { matchId: to.params.matchId },
          }),

        },

        {

          path: 'my-events/:tournamentId/team-registration',

          redirect: { name: 'AccountMyEvents', query: { tab: 'available' } },

        },

        {

          path: 'my-events/:tournamentId/team-application',

          redirect: { name: 'AccountMyEvents', query: { tab: 'applications' } },

        },

                                                                                                                                                                                                                {

          path: 'notifications',

          name: 'AccountNotifications',

          component: () => import('@/views/account/notifications/NotificationsView.vue'),

          meta: { title: '消息通知' },

        },

        {

          path: 'notifications/:notificationId',

          name: 'AccountNotificationDetail',

          component: () => import('@/views/account/notifications/NotificationDetailView.vue'),

          meta: { title: '通知详情' },

        },

        {

          path: 'settings',

          name: 'AccountSettings',

          component: () => import('@/views/account/settings/index.vue'),

          meta: { title: '账号设置' },

        },

        {

          path: 'security',

          name: 'AccountSecurity',

          redirect: { name: 'AccountSettings', query: { tab: 'security' } },

        },

        {

          path: 'team',

          name: 'AccountMyTeam',

          component: () => import('@/views/account/team/index.vue'),

          meta: { title: '我的球队' },

        },

        {

          path: 'team/:teamId/member/:teamMemberId',

          name: 'AccountTeamMemberDetail',

          component: () => import('@/views/account/team/TeamMemberDetailView.vue'),

          meta: { title: '队员资料' },

        },

        {

          path: 'team-relations',

          redirect: { name: 'AccountMyTeam' },

        },

      ],

    },

    {

      path: '/tournament/team/compare',

      name: 'TeamCompare',

      component: () => import('@/views/tournament/team/TeamCompare.vue'),

      meta: { title: '球队对比' },

    },

    {

      path: '/tournament/team/:teamId',

      component: () => import('@/views/tournament/team/TeamLayout.vue'),

      meta: { title: '球队详情' },

      redirect: (to) => `/tournament/team/${to.params.teamId}/overview`,

      children: [

        {

          path: 'overview',

          name: 'TeamOverview',

          component: () => import('@/views/tournament/team/TeamOverview.vue'),

          meta: { title: '球队总览' },

        },

        {

          path: 'matches',

          name: 'TeamMatches',

          component: () => import('@/views/tournament/team/TeamMatches.vue'),

          meta: { title: '球队比赛' },

        },

        {

          path: 'stats',

          name: 'TeamStats',

          component: () => import('@/views/tournament/team/TeamStats.vue'),

          meta: { title: '球队数据' },

        },

        {

          path: 'archive',

          name: 'TeamArchive',

          component: () => import('@/views/tournament/team/TeamArchive.vue'),

          meta: { title: '球队动态' },

        },

      ],

    },

  ],

})



router.beforeEach(async (to) => {
  const userStore = useUserStore()
  await userStore.ensureInitialized()

  const loggedIn = userStore.isAuthenticated
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const guestOnly = to.matched.some((record) => record.meta.guestOnly)

  if (requiresAuth && !loggedIn) {
    return {
      name: 'Login',
      query: {
        redirect: to.fullPath,
      },
    }
  }

  if (guestOnly && loggedIn) {
    return {
      path: resolveAuthenticatedEntryRedirect(to.query.redirect),
    }
  }

  return true
})



router.afterEach((to) => {

  const title = (to.meta.title as string) || 'CNPA'

  document.title = `${title} - CNPA 篮球赛事平台`

})



export default router

