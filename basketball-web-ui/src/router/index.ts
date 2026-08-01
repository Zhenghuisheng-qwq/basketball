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

      component: () => import('@/views/events/EventsPage.vue'),

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

      path: '/data',

      name: 'Data',

      component: () => import('@/views/data/index.vue'),

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

          redirect: { name: 'AccountOpenApplicationApply' },

        },

        {

          path: 'events/open/status',

          redirect: { name: 'AccountOpenApplicationStatusRedirect' },

        },

        {

          path: 'events/team/manage',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'events/team/roster',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'events/team/free-agent',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'events/team/transfer',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'events/team/transfer/create',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'events/team/transfer/detail',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'events/team/captain-transfer',

          redirect: { name: 'AccountMyEvents', query: { tab: 'joined' } },

        },

        {

          path: 'my-events',

          name: 'AccountMyEvents',

          component: () => import('@/views/account/events/MyEventsView.vue'),

          meta: { title: '我的赛事' },

        },

        {

          path: 'my-events/2026-open/individual-registration',

          redirect: { name: 'AccountOpenApplicationApply' },

        },

        {

          path: 'my-events/2026-open/application-status',

          redirect: { name: 'AccountOpenApplicationStatusRedirect' },

        },

        {

          path: 'my-events/:tournamentId/team-registration',

          name: 'AccountTeamRegistration',

          component: () => import('@/views/account/events/TeamRegistrationFlow.vue'),

          meta: { title: '球队报名' },

        },

        {

          path: 'my-events/:tournamentId/team-application',

          name: 'AccountTeamApplicationPlaceholder',

          component: () => import('@/views/account/events/TeamApplicationPlaceholder.vue'),

          meta: { title: '球队报名组队' },

        },

        {

          path: 'my-tournaments',

          redirect: () => ({ path: '/account/my-events' }),

        },

        {

          path: 'my-tournaments/list',

          name: 'AccountMyTournamentsLegacy',

          component: () => import('@/views/account/my-tournaments/index.vue'),

          meta: { title: '我的赛事' },

        },

        {

          path: 'my-tournaments/:tournamentId/team',

          name: 'AccountEventTeamManagement',

          component: () => import('@/views/account/tournaments/EventTeamManagementView.vue'),

          meta: { title: '本届球队管理' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data',

          name: 'AccountMatchDataList',

          component: () => import('@/views/account/tournaments/MatchDataListView.vue'),

          meta: { title: '我的比赛' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/submit',

          name: 'AccountMatchDataSubmit',

          component: () => import('@/views/account/tournaments/MatchDataSubmitView.vue'),

          meta: { title: '提交比赛数据' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/resubmit',

          name: 'AccountMatchDataResubmit',

          component: () => import('@/views/account/tournaments/MatchDataSubmitView.vue'),

          meta: { title: '重新提交比赛数据' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/status',

          name: 'AccountMatchDataStatus',

          component: () => import('@/views/account/tournaments/MatchDataStatusView.vue'),

          meta: { title: '比赛数据状态' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/dispute',

          name: 'AccountMatchDataDispute',

          component: () => import('@/views/account/tournaments/MatchDataDisputeView.vue'),

          meta: { title: '发起数据争议' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/dispute-status',

          name: 'AccountMatchDataDisputeStatus',

          component: () => import('@/views/account/tournaments/MatchDataDisputeStatusView.vue'),

          meta: { title: '争议状态' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/exception',

          name: 'AccountMatchExceptionSubmit',

          component: () => import('@/views/account/tournaments/MatchExceptionSubmitView.vue'),

          meta: { title: '异常比赛申请' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/match-data/:matchId/exception-status',

          name: 'AccountMatchExceptionStatus',

          component: () => import('@/views/account/tournaments/MatchExceptionStatusView.vue'),

          meta: { title: '异常申请状态' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/roster',

          name: 'AccountEventTeamRoster',

          component: () => import('@/views/account/tournaments/RosterManagementView.vue'),

          meta: { title: '阵容管理' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/roster-history',

          name: 'AccountEventTeamRosterHistory',

          component: () => import('@/views/account/tournaments/RosterHistoryView.vue'),

          meta: { title: '阵容变更历史' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/profile/edit',

          name: 'AccountEventTeamProfileEdit',

          component: () => import('@/views/account/tournaments/EditEventTeamProfileView.vue'),

          meta: { title: '修改球队资料' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/free-agents',

          name: 'AccountEventTeamFreeAgents',

          component: () => import('@/views/account/tournaments/FreeAgentAdditionView.vue'),

          meta: { title: '自由球员补充' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/transfers',

          name: 'AccountEventTeamTransfers',

          component: () => import('@/views/account/tournaments/TransferManagementView.vue'),

          meta: { title: '转会管理' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/transfers/initiate',

          name: 'AccountEventTeamTransferInitiate',

          component: () => import('@/views/account/tournaments/InitiateTransferView.vue'),

          meta: { title: '发起转会' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/transfers/:transferId',

          name: 'AccountEventTeamTransferDetail',

          component: () => import('@/views/account/tournaments/TransferConfirmationView.vue'),

          meta: { title: '转会确认' },

        },

        {

          path: 'my-tournaments/:tournamentId/team/captain-transfer',

          name: 'AccountEventTeamCaptainTransfer',

          component: () => import('@/views/account/tournaments/CaptainTransferView.vue'),

          meta: { title: '队长移交' },

        },

        {

          path: 'my-tournaments/open',

          name: 'AccountOpenRegistration',

          redirect: { name: 'AccountOpenApplicationStatusRedirect' },

        },

        {

          path: 'my-tournaments/open/apply',

          name: 'AccountOpenApplicationApply',

          component: () => import('@/views/account/my-tournaments/open/apply.vue'),

          meta: { title: 'Open 个人报名' },

        },

        {

          path: 'my-tournaments/open/status',

          name: 'AccountOpenApplicationStatusRedirect',

          component: () => import('@/views/account/my-tournaments/open/index.vue'),

          meta: { title: 'Open 报名状态' },

        },

        {

          path: 'my-tournaments/open/status/pending',

          name: 'AccountOpenApplicationPending',

          component: () => import('@/views/account/my-tournaments/open/status.vue'),

          props: { statusMode: 'pending' },

          meta: { title: 'Open 报名审核中' },

        },

        {

          path: 'my-tournaments/open/status/rejected',

          name: 'AccountOpenApplicationRejected',

          component: () => import('@/views/account/my-tournaments/open/status.vue'),

          props: { statusMode: 'rejected' },

          meta: { title: 'Open 报名未通过' },

        },

        {

          path: 'my-tournaments/open/status/approved',

          name: 'AccountOpenApplicationApproved',

          component: () => import('@/views/account/my-tournaments/open/status.vue'),

          props: { statusMode: 'approved' },

          meta: { title: 'Open 报名已通过' },

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

