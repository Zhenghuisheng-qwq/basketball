import type { AccountEventTab, MyEventsTabKey } from '@/types/accountEvents'
import type { MyEventsRoleMode } from '@/api/types/myEvents'

export const accountEventTabsAll: AccountEventTab[] = [
  { key: 'available', label: '全部赛事', title: '当前开放赛事', subtitle: 'ALL EVENTS' },
  { key: 'applications', label: '球队报名', title: '球队报名记录', subtitle: 'TEAM REGISTRATIONS' },
  { key: 'joined', label: '我的阵容', title: '我的参赛阵容', subtitle: 'MY ROSTER' },
  { key: 'schedule', label: '我的赛程', title: '我的赛程安排', subtitle: 'MY SCHEDULE' },
]

export const accountEventTabsCaptain: AccountEventTab[] = [
  { key: 'available', label: '可报名赛事', title: '当前开放赛事', subtitle: 'AVAILABLE EVENTS' },
  { key: 'applications', label: '我的报名', title: '我的报名', subtitle: 'MY APPLICATIONS' },
  { key: 'joined', label: '已参加赛事', title: '已参加赛事', subtitle: 'PARTICIPATING EVENTS' },
  { key: 'schedule', label: '我的赛程', title: '我的赛程安排', subtitle: 'MY SCHEDULE' },
]

export function getTabsForRole(roleMode?: MyEventsRoleMode): AccountEventTab[] {
  if (roleMode === 'TEAM_CAPTAIN') {
    return accountEventTabsCaptain
  }
  if (roleMode === 'NO_TEAM') {
    return accountEventTabsAll.filter((tab) => tab.key === 'available')
  }
  return accountEventTabsAll
}

export function getTabMeta(tab: string, roleMode?: MyEventsRoleMode) {
  const tabs = getTabsForRole(roleMode)
  return tabs.find((item) => item.key === tab) || tabs[0]
}

export function getBannerCopy(tab: string, roleMode?: MyEventsRoleMode): { title: string; description: string } {
  if (roleMode === 'NO_TEAM') {
    return {
      title: '当前开放赛事',
      description: '你可以查看当前公开赛事。报名操作由具备对应赛事资格的队长完成。',
    }
  }
  if (roleMode === 'TEAM_MEMBER') {
    if (tab === 'applications') {
      return {
        title: '球队报名记录',
        description: '你当前是球队成员，可查看球队报名状态。赛事报名由队长统一操作。',
      }
    }
    if (tab === 'joined') {
      return {
        title: '我的参赛阵容',
        description: '查看自己在已报名赛事中的阵容入选情况。',
      }
    }
    if (tab === 'schedule') {
      return {
        title: '我的赛程安排',
        description: '聚合查看你在全部已参加赛事中的比赛安排。',
      }
    }
    return {
      title: '当前开放赛事',
      description: '你当前是球队成员，可查看球队报名状态。赛事报名由队长统一操作。',
    }
  }
  if (roleMode === 'TEAM_CAPTAIN') {
    if (tab === 'applications') {
      return {
        title: '已提交的报名申请记录',
        description: '包含审核中、已通过、已驳回、已撤回与已取消等全部状态。',
      }
    }
    if (tab === 'joined') {
      return {
        title: '正式参赛与历史赛事关系',
        description: '根据本届身份进入对应页面。',
      }
    }
    if (tab === 'schedule') {
      return {
        title: '我的赛程安排',
        description: '跨赛事查看待比赛、已结束等全部赛程，单赛事详情仍可从「已参加赛事」进入。',
      }
    }
    return {
      title: '当前开放赛事',
      description: '你可以查看全部赛事，只有已获得报名资格的赛事可以报名。',
    }
  }
  return getBannerCopyLegacy(tab)
}

function getBannerCopyLegacy(tab: string): { title: string; description: string } {
  if (tab === 'applications') {
    return {
      title: '已提交的报名申请记录',
      description: '包含审核中、已通过、已驳回、已撤回与已取消等全部状态',
    }
  }
  if (tab === 'joined') {
    return {
      title: '正式参赛与历史赛事关系',
      description: '根据本届身份进入对应页面',
    }
  }
  return {
    title: '当前开放赛事',
    description: '查看符合账号资格的报名入口',
  }
}

export function getFooterNote(tab: string, roleMode?: MyEventsRoleMode): string {
  if (roleMode === 'NO_TEAM') {
    return '加入球队后，可由队长统一报名赛事。'
  }
  if (roleMode === 'TEAM_MEMBER') {
    if (tab === 'applications') {
      return '当前球队暂无赛事报名记录时，请等待队长报名。'
    }
    return '普通成员不能代表球队报名，如需参赛请联系队长。'
  }
  if (roleMode === 'TEAM_CAPTAIN') {
    if (tab === 'applications') {
      return '报名申请与正式参赛关系分别展示；审核通过后同一赛事可同时出现在「我的报名」与「已参加赛事」。'
    }
    return '队长权限只绑定具体赛事与当前球队；未授权赛事仅可查看不可报名。'
  }
  return getFooterNoteLegacy(tab)
}

function getFooterNoteLegacy(tab: string): string {
  if (tab === 'applications') {
    return '报名申请与正式参赛关系分别展示；审核通过后同一赛事可同时出现在「我的报名」与「已参加赛事」。'
  }
  if (tab === 'joined') {
    return '赛事队长权限只绑定具体赛事与赛事球队；历史赛事为只读。'
  }
  return '名额释放且未截止时，报名入口会重新开放。'
}

export function getEmptyMessage(tab: MyEventsTabKey, roleMode?: MyEventsRoleMode): string {
  if (tab === 'available') {
    if (roleMode === 'NO_TEAM') {
      return '当前暂无可参与赛事'
    }
    if (roleMode === 'TEAM_CAPTAIN') {
      return '暂无可报名赛事'
    }
    return '当前暂无可参与赛事'
  }
  if (tab === 'applications') {
    if (roleMode === 'TEAM_MEMBER') {
      return '当前球队暂无赛事报名记录，请等待队长报名。'
    }
    return '暂无报名记录'
  }
  if (tab === 'schedule') {
    return '当前筛选条件下暂无比赛'
  }
  return '暂无参赛记录'
}

/** @deprecated 使用 getTabsForRole */
export const accountEventTabs = accountEventTabsAll
