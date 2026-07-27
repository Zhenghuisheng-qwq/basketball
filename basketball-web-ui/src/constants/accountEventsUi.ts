import type { AccountEventTab } from '@/types/accountEvents'

export const accountEventTabs: AccountEventTab[] = [
  { key: 'available', label: '可参与赛事', title: '可参与赛事', subtitle: 'AVAILABLE EVENTS' },
  { key: 'applications', label: '我的报名', title: '我的报名', subtitle: 'MY APPLICATIONS' },
  { key: 'joined', label: '已参加赛事', title: '已参加赛事', subtitle: 'PARTICIPATING EVENTS' },
]

export function getTabMeta(tab: string) {
  return accountEventTabs.find((item) => item.key === tab) || accountEventTabs[0]
}

export function getBannerCopy(tab: string): { title: string; description: string } {
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

export function getFooterNote(tab: string): string {
  if (tab === 'applications') {
    return '报名申请与正式参赛关系分别展示；审核通过后同一赛事可同时出现在「我的报名」与「已参加赛事」。'
  }
  if (tab === 'joined') {
    return '赛事队长权限只绑定具体赛事与赛事球队；历史赛事为只读。'
  }
  return '名额释放且未截止时，报名入口会重新开放。'
}
