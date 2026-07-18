import type { LeagueInfo, CommunityLink, CompetitionStage } from '../types/events'

export const leagueInfo: LeagueInfo = {
  name: 'CNPA LEAGUE',
  tag: '5V5 官方职业联赛',
  description:
    'CNPA League 是 CNPA 体系中最高级别的 5V5 职业联赛，汇聚全国顶尖战队，争夺年度总冠军荣耀。',
  season: '2026 赛季',
  seasonPeriod: '1月 - 9月',
  teamCount: '16 支战队',
  teamCountDesc: '角逐冠军',
  currentStage: '常规赛',
  platform: 'PS5 / Xbox',
}

export const competitionStages: CompetitionStage[] = [
  {
    key: 'regular',
    index: '01',
    name: '常规赛',
    period: '2026.01 - 2026.05',
    fields: [
      { label: '参赛战队', value: '16 支战队' },
      { label: '赛制', value: '双循环 BO3' },
      { label: '每队场次', value: '30 场' },
      { label: '晋级名额', value: '前 8 名获得季后赛资格' },
    ],
  },
  {
    key: 'playoffs',
    index: '02',
    name: '季后赛',
    period: '2026.05 - 2026.07',
    fields: [
      { label: '晋级战队', value: '8 支战队' },
      { label: '赛制', value: '单败淘汰 BO5' },
      { label: '晋级规则', value: '胜者晋级下一轮，败者淘汰' },
    ],
  },
  {
    key: 'finals',
    index: '03',
    name: '总决赛',
    period: '2026.08 - 2026.09',
    fields: [
      { label: '晋级战队', value: '季后赛胜者' },
      { label: '赛制', value: '七局四胜 BO7' },
      { label: '冠军荣誉', value: 'CNPA League 总冠军' },
    ],
  },
]

export const communityLinks: CommunityLink[] = [
  {
    id: 'kook',
    title: '加入 KOOK',
    description: '实时语音互动，与战队粉丝一起观赛',
    icon: 'kook',
    url: '#',
  },
  {
    id: 'qq',
    title: '加入 QQ 群',
    description: '获取赛事资讯与活动通知',
    icon: 'qq',
    url: '#',
  },
  {
    id: 'bilibili',
    title: '关注 B 站',
    description: '观看官方赛事直播与精彩集锦',
    icon: 'bilibili',
    url: '#',
  },
  {
    id: 'douyin',
    title: '关注抖音',
    description: '追踪赛事热点与球员动态',
    icon: 'douyin',
    url: '#',
  },
]

export const seasonOptions = ['2026 赛季', '2025 赛季']
