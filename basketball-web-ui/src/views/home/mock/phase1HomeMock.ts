import homeTop from '@/assets/icon/home_top.png'

export type HomeCompetitionFilter = 'ALL' | 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'

export interface HomeBannerMock {
  id: string
  image: string
  label: string
  accent: 'red' | 'gold' | 'blue'
}

export interface HomeCurrentEventMock {
  id: string
  name: string
  season: string
  status: string
  stage: string
  teamCount: number
  matchCount: number
}

export interface HomeFocusTeamMock {
  id: string
  competition: Exclude<HomeCompetitionFilter, 'ALL'>
  name: string
  logoUrl: string
  metricLabel: string
  metricValue: string
  description: string
}

export interface HomeCommunityMock {
  id: string
  name: string
  description: string
  available: boolean
}

export const homeCompetitionFilters: Array<{
  key: HomeCompetitionFilter
  label: string
}> = [
  { key: 'ALL', label: '全部赛事' },
  { key: 'LEAGUE', label: 'League' },
  { key: 'ARENA', label: 'Arena' },
  { key: 'CUP', label: 'Cup' },
  { key: 'OPEN', label: 'Open' },
]

export const bannerList: HomeBannerMock[] = [
  { id: 'brand', image: homeTop, label: 'CNPA 品牌视觉', accent: 'red' },
  { id: 'season', image: homeTop, label: '赛季赛事视觉', accent: 'gold' },
  { id: 'community', image: homeTop, label: '篮球电竞社区', accent: 'blue' },
]

export const currentEventMock: HomeCurrentEventMock = {
  id: 'phase1-current-event',
  name: 'CNPA 2026 春季赛',
  season: '2026 春季赛季',
  status: '进行中',
  stage: '常规赛阶段',
  teamCount: 16,
  matchCount: 48,
}

export const focusTeamMock: HomeFocusTeamMock[] = [
  {
    id: 'phase1-focus-league',
    competition: 'LEAGUE',
    name: 'League 焦点战队',
    logoUrl: '',
    metricLabel: '本赛季胜率',
    metricValue: '78%',
    description: '以稳定攻防表现成为本阶段关注焦点。',
  },
  {
    id: 'phase1-focus-arena',
    competition: 'ARENA',
    name: 'Arena 焦点战队',
    logoUrl: '',
    metricLabel: '场均得分',
    metricValue: '82.4',
    description: '快速转换与外线火力带来强势比赛节奏。',
  },
  {
    id: 'phase1-focus-cup',
    competition: 'CUP',
    name: 'Cup 焦点战队',
    logoUrl: '',
    metricLabel: '连胜场次',
    metricValue: '06',
    description: '淘汰赛阶段保持高质量竞技状态。',
  },
  {
    id: 'phase1-focus-open',
    competition: 'OPEN',
    name: 'Open 焦点战队',
    logoUrl: '',
    metricLabel: '关键篮板',
    metricValue: '14.8',
    description: '在开放赛事中展现积极拼抢与团队协作。',
  },
]

export const communityList: HomeCommunityMock[] = [
  { id: 'kook', name: 'KOOK', description: '加入赛事实时交流', available: false },
  { id: 'wechat', name: '微信群', description: '社群入口即将开放', available: false },
  { id: 'bilibili', name: 'B站', description: '关注赛事内容更新', available: false },
  { id: 'douyin', name: '抖音', description: '获取更多精彩片段', available: false },
]
