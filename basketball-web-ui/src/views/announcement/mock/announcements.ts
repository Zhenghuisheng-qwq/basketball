import type {
  Announcement,
  AnnouncementCategory,
  AnnouncementCategoryCode,
  AnnouncementContentBlock,
  AnnouncementFilterCategory,
  AnnouncementListQuery,
  AnnouncementListResult,
  AnnouncementStatus,
} from '../types'

export const ANNOUNCEMENT_CATEGORIES: AnnouncementCategory[] = [
  { code: 'EVENT', name: '赛事公告' },
  { code: 'RULE', name: '规则公告' },
  { code: 'PENALTY', name: '处罚公示' },
  { code: 'PLATFORM', name: '平台公告' },
]

export const ANNOUNCEMENT_FILTER_TABS: Array<{ code: AnnouncementFilterCategory; name: string }> = [
  { code: 'ALL', name: '全部' },
  ...ANNOUNCEMENT_CATEGORIES.map((item) => ({ code: item.code, name: item.name })),
]

const categoryNameMap = Object.fromEntries(
  ANNOUNCEMENT_CATEGORIES.map((item) => [item.code, item.name]),
) as Record<AnnouncementCategoryCode, string>

function blocks(...items: AnnouncementContentBlock[]): AnnouncementContentBlock[] {
  return items
}

function createScheduleAdjustmentContent(): AnnouncementContentBlock[] {
  return blocks(
    { type: 'PARAGRAPH', text: '各参赛球队：' },
    {
      type: 'PARAGRAPH',
      text: '因赛事直播安排调整，原定于 7 月 24 日进行的季后赛比赛时间现作如下调整。',
    },
    { type: 'HEADING', title: '赛程调整信息', level: 2 },
    {
      type: 'INFO_TABLE',
      rows: [
        { label: '对阵', value: 'PHOENIX VS TITAN' },
        { label: '原比赛时间', value: '2026 年 7 月 24 日 20:00' },
        { label: '调整后时间', value: '2026 年 7 月 25 日 20:00', highlight: true },
      ],
    },
    {
      type: 'PARAGRAPH',
      text: '除上述比赛时间外，其他赛程安排保持不变。请相关球队及时调整备赛计划，并按照新时间准时进入比赛。',
    },
    {
      type: 'PARAGRAPH',
      text: '如有疑问，请由球队队长通过个人中心联系赛事管理员。',
    },
    { type: 'PARAGRAPH', text: '特此通知。' },
  )
}

function createRuleContent(title: string): AnnouncementContentBlock[] {
  return blocks(
    { type: 'PARAGRAPH', text: `本公告就 ${title} 作出说明，请全体参赛人员认真阅读并遵守。` },
    { type: 'HEADING', title: '适用范围', level: 2 },
    {
      type: 'UNORDERED_LIST',
      items: ['适用于 CNPA 全站公开赛事', '适用于所有已注册球队与认证球员', '适用于赛事进行期间及报名阶段'],
    },
    { type: 'HEADING', title: '重点条款', level: 2 },
    {
      type: 'ORDERED_LIST',
      items: [
        '参赛球队需在比赛前完成阵容锁定',
        '比赛数据需在比赛结束后 24 小时内提交',
        '违规行为将按照竞赛规则进行处理',
      ],
    },
    {
      type: 'LINK',
      label: '查看 CNPA 赛事中心',
      url: '/tournament',
    },
  )
}

function createPenaltyContent(caseName: string): AnnouncementContentBlock[] {
  return blocks(
    { type: 'PARAGRAPH', text: `经赛事纪律委员会调查，${caseName} 相关情况如下：` },
    {
      type: 'INFO_TABLE',
      rows: [
        { label: '涉及赛事', value: '2026 CNPA League S1' },
        { label: '处理结果', value: '警告并扣除积分 1 分', highlight: true },
        { label: '生效时间', value: '2026 年 7 月 18 日' },
      ],
    },
    { type: 'PARAGRAPH', text: '如对处理结果有异议，可在 3 个工作日内向赛事管理组提交申诉材料。' },
  )
}

function createPlatformContent(feature: string): AnnouncementContentBlock[] {
  return blocks(
    { type: 'PARAGRAPH', text: `CNPA 官网已更新 ${feature}，欢迎球员与球队管理员体验使用。` },
    {
      type: 'UNORDERED_LIST',
      items: ['登录后可查看个人数据与球队动态', '支持按赛季筛选历史记录', '移动端浏览体验已同步优化'],
    },
    {
      type: 'ATTACHMENT',
      attachmentId: 'att-platform-guide',
      fileName: 'CNPA_平台功能说明.pdf',
      fileSize: '1.8 MB',
      url: '#',
    },
  )
}

interface AnnouncementSeed {
  id: number
  title: string
  subtitle?: string
  categoryCode: AnnouncementCategoryCode
  pinned?: boolean
  status?: AnnouncementStatus
  scopeType: Announcement['scopeType']
  tournamentId?: string
  tournamentName?: string
  platformName?: string
  publishTime: string
  publishDateText: string
  publisherName?: string
  summary: string
  contentBlocks?: AnnouncementContentBlock[]
  replacementAnnouncementId?: number
  replacementAnnouncementTitle?: string
  invalidReason?: string
  attachments?: Announcement['attachments']
}

const featuredSeeds: AnnouncementSeed[] = [
  {
    id: 10001,
    title: '2026 CNPA League S1',
    subtitle: '季后赛赛程调整通知',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-07-20T10:00:00',
    publishDateText: '2026.07.20',
    publisherName: 'CNPA 赛事管理组',
    summary: '因直播安排调整，季后赛 PHOENIX VS TITAN 比赛时间变更。',
    contentBlocks: createScheduleAdjustmentContent(),
  },
  {
    id: 10002,
    title: 'CNPA 赛事通用竞赛规则',
    subtitle: '2026年7月修订版',
    categoryCode: 'RULE',
    pinned: true,
    status: 'ACTIVE',
    scopeType: 'GLOBAL',
    publishTime: '2026-07-15T09:00:00',
    publishDateText: '2026.07.15',
    publisherName: 'CNPA 规则委员会',
    summary: '2026 年 7 月修订版通用竞赛规则正式发布，现行有效。',
    contentBlocks: createRuleContent('CNPA 赛事通用竞赛规则（2026年7月修订版）'),
    attachments: [
      {
        id: 1,
        fileName: 'CNPA_通用竞赛规则_202607.pdf',
        fileSize: '2.4 MB',
        fileType: 'pdf',
        url: '#',
      },
    ],
  },
  {
    id: 10003,
    title: '2026 CNPA League S1 季后赛赛程与参赛说明',
    categoryCode: 'EVENT',
    pinned: true,
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-07-16T11:00:00',
    publishDateText: '2026.07.16',
    publisherName: 'CNPA 赛事管理组',
    summary: '公布 2026 CNPA League S1 季后赛赛程安排与参赛注意事项。',
    contentBlocks: blocks(
      { type: 'PARAGRAPH', text: '2026 CNPA League S1 季后赛即将开赛，现将赛程与参赛说明公布如下。' },
      {
        type: 'INFO_TABLE',
        title: '季后赛基本信息',
        rows: [
          { label: '参赛球队', value: '常规赛前 8 名' },
          { label: '赛制', value: '单败淘汰 BO5' },
          { label: '首轮开赛', value: '2026 年 7 月 22 日', highlight: true },
        ],
      },
      {
        type: 'ORDERED_LIST',
        items: ['请各队提前 30 分钟进入候场', '首发阵容需与报名名单一致', '比赛期间禁止中途更换账号'],
      },
    ),
  },
  {
    id: 10004,
    title: 'CNPA 官网账号与球员认证功能正式开放',
    categoryCode: 'PLATFORM',
    pinned: true,
    scopeType: 'GLOBAL',
    publishTime: '2026-07-10T08:30:00',
    publishDateText: '2026.07.10',
    publisherName: 'CNPA 平台运营组',
    summary: '官网账号注册与球员认证功能正式上线，欢迎体验。',
    contentBlocks: createPlatformContent('账号注册与球员认证功能'),
  },
  {
    id: 10005,
    title: 'CNPA League 比赛数据上传与审核规则',
    categoryCode: 'RULE',
    status: 'ACTIVE',
    scopeType: 'GLOBAL',
    publishTime: '2026-07-12T14:00:00',
    publishDateText: '2026.07.12',
    publisherName: 'CNPA 规则委员会',
    summary: '明确比赛数据上传时限、审核流程与异常处理规范。',
    contentBlocks: createRuleContent('比赛数据上传与审核规则'),
  },
  {
    id: 10006,
    title: '常规赛第八轮部分比赛时间调整通知',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-07-08T16:00:00',
    publishDateText: '2026.07.08',
    publisherName: 'CNPA 赛事管理组',
    summary: '常规赛第八轮 2 场比赛时间因转播协调作出调整。',
    contentBlocks: createScheduleAdjustmentContent(),
  },
  {
    id: 10007,
    title: '关于 PHOENIX 对阵 TITAN 比赛异常情况的处理决定',
    categoryCode: 'PENALTY',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-07-05T18:00:00',
    publishDateText: '2026.07.05',
    publisherName: 'CNPA 纪律委员会',
    summary: '公布 PHOENIX 对阵 TITAN 比赛异常情况调查与处理结果。',
    contentBlocks: createPenaltyContent('PHOENIX 对阵 TITAN 比赛异常情况'),
  },
  {
    id: 10008,
    title: 'CNPA 球员转会与自由球员补充规则（旧版）',
    categoryCode: 'RULE',
    status: 'REPLACED',
    scopeType: 'GLOBAL',
    publishTime: '2026-05-20T10:00:00',
    publishDateText: '2026.05.20',
    publisherName: 'CNPA 规则委员会',
    summary: '旧版球员转会与自由球员补充规则，已被新版本替代。',
    replacementAnnouncementId: 10028,
    replacementAnnouncementTitle: 'CNPA 球员转会与自由球员补充规则（2026年7月版）',
    contentBlocks: createRuleContent('球员转会与自由球员补充规则（旧版）'),
  },
  {
    id: 10009,
    title: '2026 CNPA Open 个人报名正式开启',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-open',
    tournamentName: '2026 CNPA Open',
    platformName: 'PC赛事',
    publishTime: '2026-06-28T09:00:00',
    publishDateText: '2026.06.28',
    publisherName: 'CNPA 赛事管理组',
    summary: '2026 CNPA Open 个人报名通道正式开放。',
    contentBlocks: blocks(
      { type: 'PARAGRAPH', text: '2026 CNPA Open 个人报名现已开启，欢迎符合条件的球员参与。' },
      {
        type: 'INFO_TABLE',
        rows: [
          { label: '报名截止', value: '2026 年 7 月 15 日', highlight: true },
          { label: '赛事平台', value: 'PC' },
          { label: '赛制', value: '单败淘汰' },
        ],
      },
    ),
  },
  {
    id: 10010,
    title: 'CNPA 赛事报名与阵容锁定规则',
    categoryCode: 'RULE',
    status: 'EXPIRED',
    scopeType: 'GLOBAL',
    publishTime: '2026-03-01T10:00:00',
    publishDateText: '2026.03.01',
    publisherName: 'CNPA 规则委员会',
    summary: '2026 赛季初期报名与阵容锁定规则，现已失效。',
    invalidReason: '已被 2026 年 7 月修订版通用竞赛规则取代。',
    contentBlocks: createRuleContent('赛事报名与阵容锁定规则'),
  },
  {
    id: 10011,
    title: '官网数据中心历史赛季查询功能更新',
    categoryCode: 'PLATFORM',
    scopeType: 'PLATFORM',
    platformName: '平台公告',
    publishTime: '2026-07-02T11:00:00',
    publishDateText: '2026.07.02',
    publisherName: 'CNPA 平台运营组',
    summary: '数据中心新增历史赛季筛选与对比能力。',
    contentBlocks: createPlatformContent('数据中心历史赛季查询功能'),
  },
  {
    id: 10012,
    title: 'WOLVES 战队消极比赛行为处罚公示',
    categoryCode: 'PENALTY',
    status: 'ACTIVE',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-06-20T15:00:00',
    publishDateText: '2026.06.20',
    publisherName: 'CNPA 纪律委员会',
    summary: 'WOLVES 战队因消极比赛被处以警告及积分扣除。',
    contentBlocks: createPenaltyContent('WOLVES 战队消极比赛行为'),
  },
  {
    id: 10013,
    title: '2026 CNPA Arena 夏季邀请赛报名须知',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-arena',
    tournamentName: '2026 CNPA Arena',
    platformName: '主机赛事',
    publishTime: '2026-06-15T10:00:00',
    publishDateText: '2026.06.15',
    publisherName: 'CNPA 赛事管理组',
    summary: 'Arena 夏季邀请赛报名流程与资格说明。',
    contentBlocks: createRuleContent('Arena 夏季邀请赛报名须知'),
  },
  {
    id: 10014,
    title: 'DRAGON 队员账号共享行为处罚决定',
    categoryCode: 'PENALTY',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-06-10T17:00:00',
    publishDateText: '2026.06.10',
    publisherName: 'CNPA 纪律委员会',
    summary: 'DRAGON 队员因账号共享被禁赛 1 场。',
    contentBlocks: createPenaltyContent('DRAGON 队员账号共享行为'),
  },
  {
    id: 10015,
    title: 'CNPA 比赛录像回放申请流程说明',
    categoryCode: 'PLATFORM',
    scopeType: 'GLOBAL',
    publishTime: '2026-06-05T09:00:00',
    publishDateText: '2026.06.05',
    publisherName: 'CNPA 平台运营组',
    summary: '说明如何申请比赛录像回放及审核时效。',
    contentBlocks: createPlatformContent('比赛录像回放申请流程'),
  },
  {
    id: 10016,
    title: '2026 CNPA Cup 八强对阵出炉',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-cup',
    tournamentName: '2026 CNPA Cup',
    platformName: '主机赛事',
    publishTime: '2026-05-28T20:00:00',
    publishDateText: '2026.05.28',
    publisherName: 'CNPA 赛事管理组',
    summary: '2026 CNPA Cup 八强对阵与比赛时间公布。',
    contentBlocks: blocks(
      { type: 'PARAGRAPH', text: '2026 CNPA Cup 八强对阵已确定，具体赛程如下。' },
      {
        type: 'INFO_TABLE',
        rows: [
          { label: 'QF1', value: 'TITAN VS LEGEND' },
          { label: 'QF2', value: 'PHOENIX VS WOLVES', highlight: true },
        ],
      },
    ),
  },
  {
    id: 10017,
    title: 'CNPA 反作弊系统升级公告',
    categoryCode: 'PLATFORM',
    status: 'ACTIVE',
    scopeType: 'GLOBAL',
    publishTime: '2026-05-22T10:00:00',
    publishDateText: '2026.05.22',
    publisherName: 'CNPA 平台运营组',
    summary: '反作弊检测能力升级，提升赛事公平性。',
    contentBlocks: createPlatformContent('反作弊系统'),
  },
  {
    id: 10018,
    title: 'LEGEND 战队迟到弃权处理公示',
    categoryCode: 'PENALTY',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-05-18T14:00:00',
    publishDateText: '2026.05.18',
    publisherName: 'CNPA 纪律委员会',
    summary: 'LEGEND 战队因迟到弃权被判负。',
    contentBlocks: createPenaltyContent('LEGEND 战队迟到弃权'),
  },
  {
    id: 10019,
    title: 'CNPA 3v3 模式竞赛补充规则',
    categoryCode: 'RULE',
    status: 'ACTIVE',
    scopeType: 'GLOBAL',
    publishTime: '2026-05-10T11:00:00',
    publishDateText: '2026.05.10',
    publisherName: 'CNPA 规则委员会',
    summary: '3v3 模式特殊规则与积分计算方式说明。',
    contentBlocks: createRuleContent('3v3 模式竞赛补充规则'),
  },
  {
    id: 10020,
    title: '2026 CNPA Masters 参赛名单公示',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-masters',
    tournamentName: '2026 CNPA Masters',
    platformName: '主机赛事',
    publishTime: '2026-05-01T09:00:00',
    publishDateText: '2026.05.01',
    publisherName: 'CNPA 赛事管理组',
    summary: 'Masters 赛事最终参赛名单公示。',
    contentBlocks: createRuleContent('Masters 参赛名单公示'),
  },
  {
    id: 10021,
    title: '官网移动端适配优化完成',
    categoryCode: 'PLATFORM',
    scopeType: 'PLATFORM',
    platformName: '平台公告',
    publishTime: '2026-04-25T10:00:00',
    publishDateText: '2026.04.25',
    publisherName: 'CNPA 平台运营组',
    summary: '官网主要页面已完成移动端布局优化。',
    contentBlocks: createPlatformContent('移动端适配优化'),
  },
  {
    id: 10022,
    title: 'RANGERS 战队违规使用 Mod 处罚公示',
    categoryCode: 'PENALTY',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-open',
    tournamentName: '2026 CNPA Open',
    platformName: 'PC赛事',
    publishTime: '2026-04-18T16:00:00',
    publishDateText: '2026.04.18',
    publisherName: 'CNPA 纪律委员会',
    summary: 'RANGERS 战队因违规 Mod 被取消比赛资格。',
    contentBlocks: createPenaltyContent('RANGERS 战队违规使用 Mod'),
  },
  {
    id: 10023,
    title: '2026 赛季全明星周末活动预告',
    categoryCode: 'EVENT',
    scopeType: 'GLOBAL',
    publishTime: '2026-04-10T12:00:00',
    publishDateText: '2026.04.10',
    publisherName: 'CNPA 赛事管理组',
    summary: '全明星周末活动时间与参与方式预告。',
    contentBlocks: createRuleContent('全明星周末活动'),
  },
  {
    id: 10024,
    title: 'CNPA 旧版积分规则（2025赛季）',
    categoryCode: 'RULE',
    status: 'EXPIRED',
    scopeType: 'GLOBAL',
    publishTime: '2025-12-01T10:00:00',
    publishDateText: '2025.12.01',
    publisherName: 'CNPA 规则委员会',
    summary: '2025 赛季积分规则，仅供历史查阅。',
    invalidReason: '2026 赛季已启用全新积分体系。',
    contentBlocks: createRuleContent('旧版积分规则（2025赛季）'),
  },
  {
    id: 10025,
    title: '球员实名认证流程调整通知',
    categoryCode: 'PLATFORM',
    scopeType: 'GLOBAL',
    publishTime: '2026-04-02T09:00:00',
    publishDateText: '2026.04.02',
    publisherName: 'CNPA 平台运营组',
    summary: '球员实名认证新增邮箱验证步骤。',
    contentBlocks: createPlatformContent('球员实名认证流程'),
  },
  {
    id: 10026,
    title: '2026 CNPA League S1 常规赛 MVP 评选说明',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-03-25T10:00:00',
    publishDateText: '2026.03.25',
    publisherName: 'CNPA 赛事管理组',
    summary: '常规赛 MVP 评选标准与投票时间说明。',
    contentBlocks: createRuleContent('常规赛 MVP 评选'),
  },
  {
    id: 10027,
    title: 'TITAN 教练席违规发言处罚公示',
    categoryCode: 'PENALTY',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-league-s1',
    tournamentName: '2026 CNPA League S1',
    platformName: '主机赛事',
    publishTime: '2026-03-18T15:00:00',
    publishDateText: '2026.03.18',
    publisherName: 'CNPA 纪律委员会',
    summary: 'TITAN 教练席违规发言被处以罚款。',
    contentBlocks: createPenaltyContent('TITAN 教练席违规发言'),
  },
  {
    id: 10028,
    title: 'CNPA 球员转会与自由球员补充规则（2026年7月版）',
    categoryCode: 'RULE',
    status: 'ACTIVE',
    scopeType: 'GLOBAL',
    publishTime: '2026-07-01T10:00:00',
    publishDateText: '2026.07.01',
    publisherName: 'CNPA 规则委员会',
    summary: '最新版球员转会与自由球员补充规则，现行有效。',
    contentBlocks: createRuleContent('球员转会与自由球员补充规则（2026年7月版）'),
  },
  {
    id: 10029,
    title: '2026 CNPA Global Finals 区域预选赛启动',
    categoryCode: 'EVENT',
    scopeType: 'TOURNAMENT',
    tournamentId: '2026-global-finals',
    tournamentName: '2026 CNPA Global Finals',
    platformName: '主机赛事',
    publishTime: '2026-03-10T09:00:00',
    publishDateText: '2026.03.10',
    publisherName: 'CNPA 赛事管理组',
    summary: 'Global Finals 区域预选赛报名启动。',
    contentBlocks: createRuleContent('Global Finals 区域预选赛'),
  },
  {
    id: 10030,
    title: 'CNPA 官网维护通知（7月12日）',
    categoryCode: 'PLATFORM',
    scopeType: 'PLATFORM',
    platformName: '平台公告',
    publishTime: '2026-07-11T18:00:00',
    publishDateText: '2026.07.11',
    publisherName: 'CNPA 平台运营组',
    summary: '7 月 12 日凌晨官网将进行系统维护。',
    contentBlocks: blocks(
      { type: 'PARAGRAPH', text: '为提升系统稳定性，CNPA 官网将于以下时段进行维护。' },
      {
        type: 'INFO_TABLE',
        rows: [
          { label: '维护时间', value: '2026 年 7 月 12 日 02:00 - 06:00', highlight: true },
          { label: '影响范围', value: '官网全部公开页面' },
        ],
      },
    ),
  },
]

const generatedTemplates: Array<{
  categoryCode: AnnouncementCategoryCode
  titlePrefix: string
  scopeType: Announcement['scopeType']
  tournamentId?: string
  tournamentName?: string
  platformName?: string
}> = [
  { categoryCode: 'EVENT', titlePrefix: '赛事动态', scopeType: 'TOURNAMENT', tournamentId: '2026-league-s1', tournamentName: '2026 CNPA League S1', platformName: '主机赛事' },
  { categoryCode: 'EVENT', titlePrefix: '赛程通知', scopeType: 'TOURNAMENT', tournamentId: '2026-open', tournamentName: '2026 CNPA Open', platformName: 'PC赛事' },
  { categoryCode: 'RULE', titlePrefix: '规则说明', scopeType: 'GLOBAL' },
  { categoryCode: 'PENALTY', titlePrefix: '处罚通报', scopeType: 'TOURNAMENT', tournamentId: '2026-league-s1', tournamentName: '2026 CNPA League S1', platformName: '主机赛事' },
  { categoryCode: 'PLATFORM', titlePrefix: '平台更新', scopeType: 'PLATFORM', platformName: '平台公告' },
]

function buildAnnouncement(seed: AnnouncementSeed): Announcement {
  return {
    id: seed.id,
    title: seed.title,
    subtitle: seed.subtitle,
    categoryCode: seed.categoryCode,
    categoryName: categoryNameMap[seed.categoryCode],
    pinned: seed.pinned ?? false,
    status: seed.status ?? 'NORMAL',
    scopeType: seed.scopeType,
    tournamentId: seed.tournamentId,
    tournamentName: seed.tournamentName,
    platformName: seed.platformName,
    publishTime: seed.publishTime,
    publishDateText: seed.publishDateText,
    publisherName: seed.publisherName ?? 'CNPA 官方',
    summary: seed.summary,
    contentBlocks: seed.contentBlocks ?? [{ type: 'PARAGRAPH', text: seed.summary }],
    replacementAnnouncementId: seed.replacementAnnouncementId,
    replacementAnnouncementTitle: seed.replacementAnnouncementTitle,
    invalidReason: seed.invalidReason,
    attachments: seed.attachments,
  }
}

function generateExtraAnnouncements(startId: number, count: number): Announcement[] {
  const items: Announcement[] = []
  for (let index = 0; index < count; index += 1) {
    const template = generatedTemplates[index % generatedTemplates.length]
    const day = String(Math.max(1, 28 - (index % 28))).padStart(2, '0')
    const month = String(Math.max(1, 6 - Math.floor(index / 8))).padStart(2, '0')
    const id = startId + index
    items.push(
      buildAnnouncement({
        id,
        title: `${template.titlePrefix} #${index + 1}`,
        categoryCode: template.categoryCode,
        scopeType: template.scopeType,
        tournamentId: template.tournamentId,
        tournamentName: template.tournamentName,
        platformName: template.platformName,
        publishTime: `2026-0${month}-${day}T12:00:00`,
        publishDateText: `2026.0${month}.${day}`,
        summary: `${template.titlePrefix} 相关说明，请参赛人员留意。`,
        contentBlocks: createRuleContent(`${template.titlePrefix} #${index + 1}`),
      }),
    )
  }
  return items
}

export const announcements: Announcement[] = [
  ...featuredSeeds.map(buildAnnouncement),
  ...generateExtraAnnouncements(10031, 12),
]

const announcementMap = new Map(announcements.map((item) => [item.id, item]))

export function getAnnouncementById(id: number): Announcement | undefined {
  return announcementMap.get(id)
}

export function getAnnouncementScopeLabel(item: Announcement): string {
  if (item.scopeType === 'GLOBAL') return '全站公告'
  if (item.scopeType === 'PLATFORM') return item.platformName ?? '平台公告'
  if (item.tournamentName && item.platformName) {
    return `${item.tournamentName} · ${item.platformName}`
  }
  return item.tournamentName ?? item.platformName ?? '全站公告'
}

function sortAnnouncements(list: Announcement[]): Announcement[] {
  return [...list].sort((a, b) => {
    if (a.pinned !== b.pinned) return a.pinned ? -1 : 1
    return b.publishTime.localeCompare(a.publishTime)
  })
}

function matchKeyword(item: Announcement, keyword: string): boolean {
  if (!keyword) return true
  const lower = keyword.toLowerCase()
  const fields = [item.title, item.subtitle, item.tournamentName, item.summary]
  return fields.some((field) => field?.toLowerCase().includes(lower))
}

export function queryAnnouncements(query: AnnouncementListQuery): AnnouncementListResult {
  const pageSize = query.pageSize ?? 10
  let filtered = announcements

  if (query.category !== 'ALL') {
    filtered = filtered.filter((item) => item.categoryCode === query.category)
  }

  if (query.keyword.trim()) {
    filtered = filtered.filter((item) => matchKeyword(item, query.keyword.trim()))
  }

  filtered = sortAnnouncements(filtered)

  const total = filtered.length
  const totalPages = Math.max(1, Math.ceil(total / pageSize))
  const page = Math.min(Math.max(1, query.page), totalPages)
  const start = (page - 1) * pageSize

  return {
    items: filtered.slice(start, start + pageSize),
    total,
    page,
    pageSize,
    totalPages,
  }
}

export function getFeaturedAnnouncements(limit = 5): Announcement[] {
  return sortAnnouncements(announcements).slice(0, limit)
}

export function getAnnouncementsByTournamentId(tournamentId: string, limit = 5): Announcement[] {
  return sortAnnouncements(
    announcements.filter((item) => item.tournamentId === tournamentId),
  ).slice(0, limit)
}
