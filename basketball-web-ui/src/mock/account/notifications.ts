import type { AccountNotification } from '@/types/account'



export const NOTIFICATIONS_STORAGE_KEY_PREFIX = 'cnpa_mock_notifications_'



function buildNotification(partial: AccountNotification): AccountNotification {

  return partial

}



export const defaultNotificationsByUserId: Record<number, AccountNotification[]> = {

  10001: [

    buildNotification({

      id: 'n-10001-1',

      userId: 10001,

      type: 'SYSTEM',

      title: '欢迎加入 CNPA',

      summary: '你的账号已创建，完善资料后可申请球员身份认证。',

      content:

        '欢迎加入 CNPA 中国职业电竞篮球联赛。完成球员认证后，即可建立长期有效的球员档案并参与赛事。',

      createdAt: '2026-07-17T08:35:00+08:00',

      read: false,

      pendingAction: false,

      actionType: 'NONE',

      actionTargetId: '',

      metadata: { source: 'CNPA 系统' },

    }),

    buildNotification({

      id: 'n-10001-2',

      userId: 10001,

      type: 'SECURITY',

      title: '邮箱验证成功',

      summary: '你的注册邮箱已完成验证。',

      content: '邮箱验证成功。邮箱验证不等于球员认证，完成球员认证后方可参与赛事。',

      createdAt: '2026-07-17T09:10:00+08:00',

      read: false,

      pendingAction: false,

      actionType: 'NONE',

      actionTargetId: '',

      metadata: { source: '账号系统' },

    }),

    buildNotification({

      id: 'n-10001-3',

      userId: 10001,

      type: 'VERIFICATION',

      title: '完成球员认证后即可参与赛事',

      summary: '申请球员认证，建立永久球员身份。',

      content:

        '完成一次球员认证后，将获得不可修改的永久球员 ID，并可报名赛事、接受球队邀请。',

      createdAt: '2026-07-18T10:00:00+08:00',

      read: true,

      pendingAction: true,

      actionType: 'VIEW_PLAYER_IDENTITY',

      actionTargetId: '',

      metadata: { source: '球员身份' },

    }),

    buildNotification({

      id: 'n-10001-4',

      userId: 10001,

      type: 'VERIFICATION',

      title: '球员认证审核中',

      summary: '你的球员认证申请正在审核。',

      content: '球员认证申请已提交，管理员审核期间不能重复提交。',

      createdAt: '2026-07-20T16:40:00+08:00',

      read: true,

      pendingAction: false,

      actionType: 'VIEW_PLAYER_IDENTITY',

      actionTargetId: '',

      metadata: { source: '球员身份' },

    }),

    buildNotification({

      id: 'n-10001-5',

      userId: 10001,

      type: 'VERIFICATION',

      title: '球员认证未通过',

      summary: '请查看管理员备注后重新提交。',

      content:

        '现有证明材料不足以确认平台身份，请修改资料后重新提交。',

      createdAt: '2026-07-20T18:25:00+08:00',

      read: true,

      pendingAction: true,

      actionType: 'RESUBMIT_VERIFICATION',

      actionTargetId: '',

      metadata: { source: '球员身份' },

    }),

    buildNotification({

      id: 'n-10001-6',

      userId: 10001,

      type: 'VERIFICATION',

      title: '球员认证审核通过',

      summary: '永久球员身份已生效。',

      content: '球员认证已通过，永久球员 ID 已生成，认证资料长期有效。',

      createdAt: '2026-07-21T11:00:00+08:00',

      read: true,

      pendingAction: true,

      actionType: 'VIEW_PLAYER_IDENTITY',

      actionTargetId: '',

      metadata: { source: '球员身份' },

    }),

    buildNotification({

      id: 'n-10001-7',

      userId: 10001,

      type: 'VERIFICATION',

      title: '平台与游戏 ID 修改审核中',

      summary: 'PS 平台游戏 ID 变更申请审核中。',

      content: '审核通过前将继续使用当前已生效的游戏 ID，审核中的修改不会出现在资料变更记录中。',

      createdAt: '2026-07-22T09:30:00+08:00',

      read: true,

      pendingAction: true,

      actionType: 'VIEW_PLATFORMS',

      actionTargetId: '',

      metadata: { source: '球员身份' },

    }),

    buildNotification({

      id: 'n-10001-8',

      userId: 10001,

      type: 'TEAM',

      title: '收到球队邀请',

      summary: '2026 CNPA Arena S1 · 示例球队 D',

      content: '你收到球队邀请，完成球员认证后可接受邀请并进入正式阵容。',

      createdAt: '2026-07-18T14:20:00+08:00',

      read: true,

      pendingAction: true,

      actionType: 'VIEW_MY_TOURNAMENTS',

      actionTargetId: 'inv-arena-d',

      metadata: { source: '赛事系统' },

    }),

    buildNotification({

      id: 'n-10001-9',

      userId: 10001,

      type: 'EVENT',

      title: '赛事资格受限通知',

      summary: '2026 CNPA League 参赛资格已取消。',

      content:

        '本届赛事参赛资格已取消。重新申请球员认证不能解除资格限制；永久球员 ID、历史比赛与荣誉记录不会删除。',

      createdAt: '2026-07-19T16:00:00+08:00',

      read: true,

      pendingAction: true,

      actionType: 'VIEW_ELIGIBILITY',

      actionTargetId: '2026-league',

      metadata: { source: '赛事系统' },

    }),

    buildNotification({
      id: 'n-10001-10',
      userId: 10001,
      type: 'EVENT',
      title: '2026 CNPA Open 个人报名已提交',
      summary: '你的 Open 个人报名已进入审核队列。',
      content: '2026 CNPA Open 个人报名已提交，管理员审核完成后状态将自动更新。',
      createdAt: '2026-07-21T11:25:00+08:00',
      read: true,
      pendingAction: true,
      actionType: 'VIEW_OPEN_APPLICATION',
      actionTargetId: '2026-open',
      metadata: { source: '赛事报名' },
    }),

    buildNotification({
      id: 'n-10001-11',
      userId: 10001,
      type: 'EVENT',
      title: '2026 CNPA Open 报名正在审核',
      summary: '个人报名资料审核中，请耐心等待。',
      content: '管理员正在核对球员认证与赛事资格，审核期间不能重复提交。',
      createdAt: '2026-07-21T14:00:00+08:00',
      read: true,
      pendingAction: false,
      actionType: 'VIEW_OPEN_APPLICATION',
      actionTargetId: '2026-open',
      metadata: { source: '赛事报名' },
    }),

    buildNotification({
      id: 'n-10001-12',
      userId: 10001,
      type: 'EVENT',
      title: '2026 CNPA Open 报名审核未通过',
      summary: '请核对报名资料后在截止前重新提交。',
      content: '当前报名资格未通过，请核对报名资料后重新提交。未通过不影响永久球员身份。',
      createdAt: '2026-07-21T18:25:00+08:00',
      read: true,
      pendingAction: true,
      actionType: 'RESUBMIT_OPEN_APPLICATION',
      actionTargetId: '2026-open',
      metadata: { source: '赛事报名' },
    }),

    buildNotification({
      id: 'n-10001-13',
      userId: 10001,
      type: 'EVENT',
      title: '2026 CNPA Open 报名审核通过',
      summary: '正式参赛资格已生效，等待分组。',
      content: 'Open 个人报名审核已通过，你已进入 2026 CNPA Open 正式参赛名单。',
      createdAt: '2026-07-21T19:00:00+08:00',
      read: true,
      pendingAction: true,
      actionType: 'VIEW_EVENT',
      actionTargetId: '2026-open',
      metadata: { source: '赛事报名' },
    }),

    buildNotification({
      id: 'n-10001-14',
      userId: 10001,
      type: 'EVENT',
      title: '2026 CNPA Open 报名即将截止',
      summary: '报名截止 2026-08-20 23:59，请尽快完成报名。',
      content: '2026 CNPA Open 个人报名即将截止，已完成球员认证的用户请尽快提交报名。',
      createdAt: '2026-08-18T10:00:00+08:00',
      read: true,
      pendingAction: true,
      actionType: 'VIEW_MY_TOURNAMENTS',
      actionTargetId: '2026-open',
      metadata: { source: '赛事报名' },
    }),

  ],

  10002: [

    buildNotification({

      id: 'n-10002-1',

      userId: 10002,

      type: 'TEAM',

      title: '球队管理提醒',

      summary: '你当前管理球队 RNG，请及时确认阵容。',

      content: '作为队长，请及时在球队管理页面确认赛事阵容与报名信息。',

      createdAt: '2026-01-18T08:00:00+08:00',

      read: false,

      pendingAction: false,

      actionType: 'NONE',

      actionTargetId: '',

      metadata: { source: '球队系统' },

    }),

  ],

  10004: [

    buildNotification({

      id: 'n-10004-1',

      userId: 10004,

      type: 'VERIFICATION',

      title: '球员认证未通过',

      summary: '你的认证申请未通过，请查看审核意见后重新提交。',

      content:

        '游戏 ID 截图信息不完整，暂时无法确认账号归属。请重新上传包含平台账号名称与完整账号页面的清晰截图。',

      createdAt: '2025-12-06T11:05:00+08:00',

      read: false,

      pendingAction: true,

      actionType: 'RESUBMIT_VERIFICATION',

      actionTargetId: '',

      metadata: { source: '认证中心' },

    }),

  ],

}



export function notificationsStorageKey(userId: number): string {

  return `${NOTIFICATIONS_STORAGE_KEY_PREFIX}${userId}`

}



export function getDefaultNotifications(userId: number): AccountNotification[] {

  const list = defaultNotificationsByUserId[userId]

  if (list) return JSON.parse(JSON.stringify(list))

  return []

}



export function countUnread(notifications: AccountNotification[]): number {

  return notifications.filter((item) => !item.read).length

}



export function countPending(notifications: AccountNotification[]): number {

  return notifications.filter((item) => item.pendingAction && !item.read).length

}


