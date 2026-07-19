import { createRouter, createWebHistory } from 'vue-router'

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
      path: '/notice',
      name: 'Notice',
      component: () => import('@/views/notice/index.vue'),
      meta: { title: '公告' },
    },
    {
      path: '/register',
      component: () => import('@/layouts/AccountLayout.vue'),
      meta: { title: '注册' },
      children: [
        {
          path: '',
          name: 'RegisterCreate',
          component: () => import('@/views/account/RegisterCreate.vue'),
          meta: { title: '创建账号' },
        },
        {
          path: 'verify-email',
          name: 'RegisterVerifyEmail',
          component: () => import('@/views/account/RegisterVerifyEmail.vue'),
          meta: { title: '邮箱验证' },
        },
        {
          path: 'success',
          name: 'RegisterSuccess',
          component: () => import('@/views/account/RegisterSuccess.vue'),
          meta: { title: '注册完成' },
        },
      ],
    },
    {
      path: '/account',
      name: 'AccountCenter',
      component: () => import('@/views/account/AccountCenter.vue'),
      meta: { title: '个人中心' },
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

router.afterEach((to) => {
  const title = (to.meta.title as string) || 'CNPA'
  document.title = `${title} - CNPA 篮球赛事平台`
})

export default router
