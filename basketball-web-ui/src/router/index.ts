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
  ],
})

router.afterEach((to) => {
  const title = (to.meta.title as string) || 'CNPA'
  document.title = `${title} - CNPA 篮球赛事平台`
})

export default router
