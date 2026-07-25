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
      title: '尚未正式获得参赛资格的报名记录',
      description: '完成对应操作后进入正式参赛关系',
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
    return '正式通过后记录自动移入「已参加赛事」；队员不能主动退出已确认组队关系。'
  }
  if (tab === 'joined') {
    return '赛事队长权限只绑定具体赛事与赛事球队；历史赛事为只读。'
  }
  return '名额释放且未截止时，报名入口会重新开放。'
}
