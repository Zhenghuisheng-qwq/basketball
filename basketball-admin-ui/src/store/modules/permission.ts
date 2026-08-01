import auth from '@/plugins/auth'
import router, { constantRoutes, dynamicRoutes } from '@/router'
import { getRouters } from '@/api/menu'
import Layout from '@/layout/index.vue'
import ParentView from '@/components/ParentView/index.vue'
import InnerLink from '@/layout/components/InnerLink/index.vue'

// 匹配views里面所有的.vue文件
const modules = import.meta.glob('./../../views/**/*.vue')

const usePermissionStore = defineStore(
  'permission',
  {
    state: () => ({
      routes: [] as any[],
      addRoutes: [] as any[],
      defaultRoutes: [] as any[],
      topbarRouters: [] as any[],
      sidebarRouters: [] as any[]
    }),
    actions: {
      setRoutes(routes: any[]) {
        this.addRoutes = routes
        this.routes = constantRoutes.concat(routes)
      },
      setDefaultRoutes(routes: any[]) {
        this.defaultRoutes = constantRoutes.concat(routes)
      },
      setTopbarRoutes(routes: any[]) {
        this.topbarRouters = routes
      },
      setSidebarRouters(routes: any[]) {
        this.sidebarRouters = routes
      },
      generateRoutes(roles?: any[]): Promise<any[]> {
        return new Promise(resolve => {
          // 向后端请求路由数据
          getRouters().then(res => {
            const sdata = JSON.parse(JSON.stringify(res.data))
            const rdata = JSON.parse(JSON.stringify(res.data))
            const defaultData = JSON.parse(JSON.stringify(res.data))
            applyCnpaMenuTitleOverrides(sdata)
            applyCnpaMenuTitleOverrides(rdata)
            applyCnpaMenuTitleOverrides(defaultData)
            applyCnpaSidebarMenuStructure(sdata)
            applyCnpaSidebarMenuStructure(defaultData)
            applyCnpaCompetitionMenuStructure(sdata, false)
            applyCnpaCompetitionMenuStructure(defaultData, false)
            applyCnpaCompetitionMenuStructure(rdata, true)
            const sidebarRoutes = filterAsyncRouter(sdata)
            const rewriteRoutes = filterAsyncRouter(rdata, false, true)
            const defaultRoutes = filterAsyncRouter(defaultData)
            const asyncRoutes = filterDynamicRoutes(dynamicRoutes)
            asyncRoutes.forEach(route => { router.addRoute(route) })
            this.setRoutes(rewriteRoutes)
            this.setSidebarRouters(constantRoutes.concat(sidebarRoutes))
            this.setDefaultRoutes(sidebarRoutes)
            this.setTopbarRoutes(defaultRoutes)
            resolve(rewriteRoutes)
          })
        })
      }
    }
  })

const ROSTER_CHANGE_CHILD_PATHS = ['transfer-audit', 'free-agent-audit', 'captain-transfer', 'roster-operations']

const CNPA_MENU_TITLE_MAP: Record<string, string> = {
  '赛事阵容管理': '正式参赛球员',
  '正式赛事阵容': '正式参赛球员',
  '转会审核': '阵容变更审核',
  '自由球员补员': '自由球员补录',
  '队长移交审核': '赛事队长移交审核',
  '阵容特殊操作': '特殊阵容调整',
  '球队与球员处罚': '处罚管理',
  '赛事管理员授权': '赛事运营授权'
}

/** 同步后台菜单显示名，避免仅改 sys_menu 才能生效的文案调整。 */
function applyCnpaMenuTitleOverrides(routes: any[]) {
  routes.forEach(route => {
    const title = route.meta?.title
    if (title && CNPA_MENU_TITLE_MAP[title]) {
      route.meta.title = CNPA_MENU_TITLE_MAP[title]
    }
    if (route.children?.length) {
      applyCnpaMenuTitleOverrides(route.children)
    }
  })
}

/** 将阵容锁定后的特殊处理菜单从「报名与阵容」拆出为独立一级菜单（仅影响侧边栏展示，不改路由注册）。 */
function applyCnpaSidebarMenuStructure(routes: any[]) {
  const participationIndex = routes.findIndex(
    route => route.path === 'participation' || route.meta?.title === '报名与阵容'
  )
  if (participationIndex === -1) {
    return
  }

  const participation = routes[participationIndex]
  if (!participation.children?.length) {
    return
  }

  const rosterChangeChildren: any[] = []
  const registrationChildren: any[] = []

  participation.children.forEach((child: any) => {
    const segment = String(child.path || '').replace(/^\//, '').split('/').pop() || ''
    if (ROSTER_CHANGE_CHILD_PATHS.includes(segment)) {
      rosterChangeChildren.push({
        ...child,
        path: `/participation/${segment}`
      })
    } else {
      registrationChildren.push(child)
    }
  })

  if (!rosterChangeChildren.length) {
    return
  }

  participation.children = registrationChildren

  routes.splice(participationIndex + 1, 0, {
    path: 'roster-change',
    component: 'Layout',
    alwaysShow: true,
    redirect: 'noRedirect',
    meta: { title: '阵容变更处理', icon: 'component' },
    children: rosterChangeChildren
  })
}

const HIDDEN_COMPETITION_EXEC_PATHS = ['score', 'lineup']
/** 历史挂在「比赛管理」下、需并入「比赛执行」的子菜单 path 段。 */
const COMPETITION_MERGE_CHILD_PATHS = [
  'match-data-audit',
  'match-dispute',
  'match-abnormal',
  'match-exception'
]
const COMPETITION_EXEC_CHILD_ORDER = [
  'schedule',
  'match-manage',
  'match-data-entry',
  'match-data-audit',
  'match-dispute',
  'match-exception',
  'match-abnormal',
  'score',
  'lineup'
]

function getRoutePathSegment(path: string): string {
  return String(path || '').replace(/^\//, '').split('/').pop() || ''
}

/** 将旧 stub「异常比赛处理」统一到真实 M07 页面 match-exception。 */
function normalizeCompetitionExecChild(child: any) {
  const segment = getRoutePathSegment(child.path)
  if (segment !== 'match-abnormal') {
    return { child, segment }
  }
  return {
    segment: 'match-exception',
    child: {
      ...child,
      path: child.path?.startsWith('/') ? '/competition-exec/match-exception' : 'match-exception',
      component: 'cnpa/competition/match-exception/index',
      name: 'BasketMatchException',
      meta: { ...child.meta, title: '异常比赛处理', icon: child.meta?.icon || 'bug' }
    }
  }
}

/** 合并比赛执行/比赛管理菜单，隐藏重复入口；侧边栏与路由注册均落到 /competition-exec/*。 */
function applyCnpaCompetitionMenuStructure(routes: any[], forRouter: boolean) {
  const execIndex = routes.findIndex(
    route =>
      route.path === 'competition-exec' ||
      route.path === '/competition-exec' ||
      route.meta?.title === '比赛执行'
  )
  if (execIndex === -1) {
    return
  }

  const execMenu = routes[execIndex]
  if (!execMenu.children?.length) {
    return
  }

  execMenu.children = execMenu.children.map((child: any) => {
    const { child: normalized, segment } = normalizeCompetitionExecChild(child)
    if (HIDDEN_COMPETITION_EXEC_PATHS.includes(segment)) {
      return { ...normalized, hidden: true }
    }
    if (segment === 'schedule') {
      return {
        ...normalized,
        component: 'cnpa/competition/schedule-manage/index',
        name: normalized.name === 'BasketMatch' ? 'BasketScheduleManage' : (normalized.name || 'BasketScheduleManage'),
        meta: { ...normalized.meta, title: '赛程管理' }
      }
    }
    return normalized
  })

  if (!execMenu.children.some((child: any) => getRoutePathSegment(child.path) === 'match-manage')) {
    execMenu.children.push({
      path: forRouter ? 'match-manage' : '/competition-exec/match-manage',
      component: 'cnpa/competition/schedule/index',
      name: 'BasketMatchManage',
      meta: { title: '比赛管理', icon: 'list' }
    })
  }

  const existingSegments = new Set(
    execMenu.children.map((child: any) => getRoutePathSegment(child.path))
  )

  const mgmtIndex = routes.findIndex(
    route =>
      route.path === 'competition' ||
      route.path === '/competition' ||
      route.meta?.title === '比赛管理'
  )
  if (mgmtIndex !== -1) {
    const mgmtMenu = routes[mgmtIndex]
    const mergedChildren = (mgmtMenu.children || [])
      .filter((child: any) => COMPETITION_MERGE_CHILD_PATHS.includes(getRoutePathSegment(child.path)))
      .map((child: any) => {
        const { child: normalized, segment } = normalizeCompetitionExecChild(child)
        // 侧边栏必须指向 competition-exec，避免与同名路由覆盖后的实际注册路径不一致导致 404
        if (forRouter) {
          return { ...normalized, path: segment }
        }
        return { ...normalized, path: `/competition-exec/${segment}` }
      })
      .filter((child: any) => !existingSegments.has(getRoutePathSegment(child.path)))

    execMenu.children.push(...mergedChildren)

    // 路由侧移除已迁移子菜单，避免同 name 二次注册覆盖 /competition-exec/* 
    mgmtMenu.children = (mgmtMenu.children || []).filter(
      (child: any) => !COMPETITION_MERGE_CHILD_PATHS.includes(getRoutePathSegment(child.path))
    )

    if (forRouter) {
      mgmtMenu.hidden = true
    } else {
      routes.splice(mgmtIndex, 1)
    }
  }

  // 隐藏孤儿「异常比赛」菜单（历史 parent 2200 不存在时会顶层露出）
  routes.forEach((route: any) => {
    const segment = getRoutePathSegment(route.path)
    if (
      (segment === 'match-exception' || route.name === 'MatchException') &&
      route.meta?.title === '异常比赛'
    ) {
      route.hidden = true
    }
  })

  execMenu.children.sort((a: any, b: any) => {
    const orderA = COMPETITION_EXEC_CHILD_ORDER.indexOf(getRoutePathSegment(a.path))
    const orderB = COMPETITION_EXEC_CHILD_ORDER.indexOf(getRoutePathSegment(b.path))
    return (orderA === -1 ? 999 : orderA) - (orderB === -1 ? 999 : orderB)
  })
}

// 遍历后台传来的路由字符串，转换为组件对象
function filterAsyncRouter(asyncRouterMap: any[], lastRouter = false, type = false) {
  return asyncRouterMap.filter(route => {
    if (type && route.children) {
      route.children = filterChildren(route.children)
    }
    if (route.component) {
      // Layout ParentView 组件特殊处理
      if (route.component === 'Layout') {
        route.component = Layout
      } else if (route.component === 'ParentView') {
        route.component = ParentView
      } else if (route.component === 'InnerLink') {
        route.component = InnerLink
      } else {
        route.component = loadView(route.component)
      }
    }
    if (route.children != null && route.children && route.children.length) {
      route.children = filterAsyncRouter(route.children, route, type)
    } else {
      delete route['children']
      delete route['redirect']
    }
    return true
  })
}

function filterChildren(childrenMap: any[], lastRouter: any = false) {
  var children: any[] = []
  childrenMap.forEach(el => {
    el.path = lastRouter ? lastRouter.path + '/' + el.path : el.path
    if (el.children && el.children.length && el.component === 'ParentView') {
      children = children.concat(filterChildren(el.children, el))
    } else {
      children.push(el)
    }
  })
  return children
}

// 动态路由遍历，验证是否具备权限
export function filterDynamicRoutes(routes: any[]): any[] {
  const res: any[] = []
  routes.forEach(route => {
    if (route.permissions) {
      if (auth.hasPermiOr(route.permissions)) {
        res.push(route)
      }
    } else if (route.roles) {
      if (auth.hasRoleOr(route.roles)) {
        res.push(route)
      }
    }
  })
  return res
}

export const loadView = (view: string): any => {
  let res
  for (const path in modules) {
    const dir = path.split('views/')[1].split('.vue')[0]
    if (dir === view) {
      res = () => modules[path]()
    }
  }
  return res
}

export default usePermissionStore
