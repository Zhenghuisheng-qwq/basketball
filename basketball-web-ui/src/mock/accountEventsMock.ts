import type {
  AccountEventTab,
  AvailableEvent,
  MyApplication,
  MyEventsSummary,
  ParticipatingEvent,
} from '@/types/accountEvents'

export const accountEventTabs: AccountEventTab[] = [
  {
    key: 'available',
    label: '可参与赛事',
    title: '可参与赛事',
    subtitle: 'AVAILABLE EVENTS',
  },
  {
    key: 'applications',
    label: '我的报名',
    title: '我的报名',
    subtitle: 'MY APPLICATIONS',
  },
  {
    key: 'joined',
    label: '已参加赛事',
    title: '已参加赛事',
    subtitle: 'PARTICIPATING EVENTS',
  },
]

export const myEventsSummary: MyEventsSummary = {
  availableCount: 2,
  applicationPendingCount: 3,
  joinedRelationCount: 3,
}

export const availableEvents: AvailableEvent[] = [
  {
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platform: 'Xbox',
    registrationType: 'TEAM',
    registrationTypeText: '球队报名',
    deadline: '2026-07-28',
    quotaUsed: 18,
    quotaTotal: 24,
    quotaFull: false,
    eligibilityStatus: 'ELIGIBLE',
    actionText: '发起球队报名',
    actionType: 'TEAM_REGISTRATION',
  },
  {
    tournamentId: '2026-open',
    tournamentName: '2026 CNPA Open',
    platform: 'Xbox',
    registrationType: 'INDIVIDUAL',
    registrationTypeText: '个人报名',
    deadline: '2026-08-20',
    quotaUsed: 86,
    quotaTotal: 120,
    quotaFull: false,
    eligibilityStatus: 'ELIGIBLE',
    actionText: '个人报名',
    actionType: 'INDIVIDUAL_REGISTRATION',
  },
  {
    tournamentId: '2026-arena',
    tournamentName: '2026 CNPA Arena',
    platform: 'Xbox',
    registrationType: 'TEAM',
    registrationTypeText: '球队报名',
    deadline: '',
    quotaUsed: 24,
    quotaTotal: 24,
    quotaFull: true,
    eligibilityStatus: 'FULL',
    actionText: '名额已满',
    actionType: 'DISABLED',
  },
]

export const myApplications: MyApplication[] = [
  {
    applicationId: 'app-cup-invite-001',
    tournamentId: '2026-cup',
    tournamentName: '2026 CNPA Cup',
    platform: 'Xbox',
    applicationType: 'TEAM_INVITE',
    applicationTypeText: '球队邀请',
    status: 'WAIT_CONFIRM_INVITE',
    statusText: '待确认邀请',
    invitedTeamCode: 'RNG',
    actionText: '确认邀请',
    actionType: 'CONFIRM_INVITE',
    pending: true,
  },
  {
    applicationId: 'app-league-team-001',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platform: 'Xbox',
    applicationType: 'TEAM_REGISTRATION',
    applicationTypeText: '球队报名',
    status: 'GROUPING',
    statusText: '组队中',
    confirmedCount: 5,
    rosterMax: 8,
    actionText: '查看组队',
    actionType: 'VIEW_TEAM_APPLICATION',
  },
  {
    applicationId: 'app-open-individual-001',
    tournamentId: '2026-open',
    tournamentName: '2026 CNPA Open',
    platform: 'Xbox',
    applicationType: 'INDIVIDUAL_REGISTRATION',
    applicationTypeText: '个人报名',
    status: 'ADMIN_REVIEWING',
    statusText: '管理员审核中',
    submitTime: '2026-07-20 16:38',
    actionText: '查看报名',
    actionType: 'VIEW_APPLICATION_STATUS',
  },
]

export const participatingEvents: ParticipatingEvent[] = [
  {
    relationId: 'rel-league-rng-captain',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platform: 'Xbox',
    participationType: 'TEAM',
    participationTypeText: '球队 RNG',
    identityType: 'EVENT_CAPTAIN',
    identityText: '赛事队长',
    status: 'ONGOING',
    statusText: '进行中',
    pendingTaskCount: 1,
    pendingTaskText: '有待处理任务',
    actionText: '管理本届球队',
    actionType: 'MANAGE_EVENT_TEAM',
    actionRoute: '/account/my-tournaments/2026-league-s1/team',
    primaryAction: true,
  },
  {
    relationId: 'rel-open-individual',
    tournamentId: '2026-open',
    tournamentName: '2026 CNPA Open',
    platform: 'Xbox',
    participationType: 'INDIVIDUAL',
    participationTypeText: '个人参赛',
    identityType: 'INDIVIDUAL_PLAYER',
    identityText: '个人参赛',
    status: 'APPROVED_WAIT_GROUP',
    statusText: '审核通过 · 等待分组',
    actionText: '查看赛事关系',
    actionType: 'VIEW_EVENT_RELATION',
    actionRoute: '/account/my-events/2026-open/application-status',
  },
  {
    relationId: 'rel-2025-league-wolves',
    tournamentId: '2025-league',
    tournamentName: '2025 CNPA League',
    platform: 'Xbox',
    participationType: 'TEAM',
    participationTypeText: '球队 WOLVES',
    identityType: 'TEAM_MEMBER',
    identityText: '队员',
    status: 'FINISHED',
    statusText: '赛事结束',
    actionText: '查看公开赛事记录',
    actionType: 'VIEW_PUBLIC_RECORD',
    actionRoute: '/tournament',
  },
]

export function getTabMeta(tab: string) {
  return accountEventTabs.find((item) => item.key === tab) || accountEventTabs[0]
}

export function getBannerStatus(tab: string): { text: string; type: 'success' | 'warning' | 'info' | 'danger' } {
  if (tab === 'applications') {
    return { text: `待处理 ${myEventsSummary.applicationPendingCount} 项`, type: 'warning' }
  }
  if (tab === 'joined') {
    return { text: `正式关系 ${myEventsSummary.joinedRelationCount} 项`, type: 'success' }
  }
  return { text: `可参与 ${myEventsSummary.availableCount} 项`, type: 'info' }
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
