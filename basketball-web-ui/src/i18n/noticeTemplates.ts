export interface NoticeCopy {
  title: string
  body: string
}

type LocaleCopy = Record<'zh-CN' | 'en-US', NoticeCopy>

const NOTICE_TEMPLATES: Record<string, LocaleCopy> = {
  TEAM_INVITATION_RECEIVED: {
    'zh-CN': { title: '球队邀请', body: '您收到来自球队「{teamName}」的邀请' },
    'en-US': { title: 'Team invitation', body: 'You received an invitation from {teamName}.' },
  },
  TEAM_JOIN_APPLICATION_RECEIVED: {
    'zh-CN': { title: '球队加入申请', body: '收到新的加入球队申请' },
    'en-US': { title: 'Team join request', body: '{teamName} received a new request to join.' },
  },
  TEAM_JOIN_APPLICATION_APPROVED: {
    'zh-CN': { title: '加入申请已通过', body: '您的加入球队申请已通过' },
    'en-US': { title: 'Join request approved', body: 'Your request to join {teamName} was approved.' },
  },
  TEAM_JOIN_APPLICATION_REJECTED: {
    'zh-CN': { title: '加入申请已驳回', body: '您的加入球队申请已被驳回\n{remark}' },
    'en-US': { title: 'Join request declined', body: 'Your request to join {teamName} was declined.\n{remark}' },
  },
  TEAM_JOIN_SUCCEEDED: {
    'zh-CN': { title: '加入球队成功', body: '您已成功加入球队「{teamName}」' },
    'en-US': { title: 'Joined the team', body: 'You joined {teamName}.' },
  },
  PLAYER_CERT_APPROVED: {
    'zh-CN': { title: '球员认证已通过', body: '您的球员认证申请已通过审核。' },
    'en-US': { title: 'Player verification approved', body: 'Your player verification was approved.' },
  },
  PLAYER_CERT_REJECTED: {
    'zh-CN': { title: '球员认证已驳回', body: '您的球员认证申请已被驳回：{remark}' },
    'en-US': { title: 'Player verification declined', body: 'Your player verification was declined: {remark}' },
  },
  TEAM_AUDIT_APPROVED: {
    'zh-CN': { title: '球队审核已通过', body: '您的球队「{teamName}」已通过审核。' },
    'en-US': { title: 'Team review approved', body: '{teamName} passed review.' },
  },
  TEAM_AUDIT_REJECTED: {
    'zh-CN': { title: '球队审核已驳回', body: '{remark}' },
    'en-US': { title: 'Team review declined', body: '{remark}' },
  },
  PROFILE_CHANGE_APPROVED: {
    'zh-CN': { title: '资料修改已通过', body: '你的{summary}修改已审核通过，公开资料已更新。' },
    'en-US': { title: 'Profile update approved', body: 'Your profile update was approved and is now public.' },
  },
  PROFILE_CHANGE_REJECTED: {
    'zh-CN': { title: '资料修改未通过', body: '你的资料修改未通过审核：{remark}' },
    'en-US': { title: 'Profile update declined', body: 'Your profile update was declined: {remark}' },
  },
  PLATFORM_CHANGE_APPROVED: {
    'zh-CN': { title: '平台游戏ID变更已通过', body: '您的平台游戏ID变更申请已通过审核。' },
    'en-US': { title: 'Game ID change approved', body: 'Your platform game ID change was approved.' },
  },
  PLATFORM_CHANGE_REJECTED: {
    'zh-CN': { title: '平台游戏ID变更已驳回', body: '您的平台游戏ID变更申请已被驳回：{remark}' },
    'en-US': { title: 'Game ID change declined', body: 'Your platform game ID change was declined: {remark}' },
  },
  TEAM_PROFILE_CHANGE_APPROVED: {
    'zh-CN': { title: '球队资料变更已通过', body: '您的球队「{teamName}」资料变更已通过审核，正式资料已更新。' },
    'en-US': { title: 'Team profile approved', body: 'The profile update for {teamName} was approved.' },
  },
  TEAM_PROFILE_CHANGE_REJECTED: {
    'zh-CN': { title: '球队资料变更未通过', body: '您的球队「{teamName}」资料变更未通过审核，原因：{remark}' },
    'en-US': { title: 'Team profile declined', body: 'The profile update for {teamName} was declined: {remark}' },
  },
  PERSONAL_REGISTRATION_REJECTED: {
    'zh-CN': { title: '个人报名已驳回', body: '您在「{divisionName}」的个人报名未通过审核，原因：{remark}' },
    'en-US': { title: 'Individual registration declined', body: 'Your individual registration for {divisionName} was declined: {remark}' },
  },
  TEAM_REGISTRATION_REJECTED: {
    'zh-CN': { title: '球队报名已驳回', body: '{teamLabel}在「{divisionName}」的报名未通过审核，原因：{remark}' },
    'en-US': { title: 'Team registration declined', body: 'The team registration in {divisionName} was declined: {remark}' },
  },
}

export function parseNoticeParams(raw?: string | null): Record<string, string> {
  if (!raw) return {}
  try {
    const parsed = JSON.parse(raw) as Record<string, unknown>
    const params: Record<string, string> = {}
    for (const [key, value] of Object.entries(parsed)) {
      params[key] = value == null ? '' : String(value)
    }
    return params
  } catch {
    return {}
  }
}

export function renderNotice(
  templateCode: string | null | undefined,
  paramsJson: string | null | undefined,
  locale: string,
): NoticeCopy | null {
  if (!templateCode) return null
  const template = NOTICE_TEMPLATES[templateCode]
  if (!template) return null
  const copy = locale.toLowerCase().startsWith('en') ? template['en-US'] : template['zh-CN']
  const params = parseNoticeParams(paramsJson)
  if (templateCode === 'PROFILE_CHANGE_APPROVED' && !params.summary) {
    params.summary = '资料'
  }
  return {
    title: fill(copy.title, params),
    body: fill(copy.body, params),
  }
}

export function displayNoticeText(
  templateCode: string | null | undefined,
  storedText: string | null | undefined,
  renderedText: string | null | undefined,
): string {
  if (!templateCode || !renderedText) return storedText || ''
  return renderedText
}

function fill(template: string, params: Record<string, string>): string {
  const filled = template.replace(/\{(\w+)\}/g, (_, key: string) => params[key] ?? '')
  return filled.replace(/[ \t]+\n/g, '\n').replace(/\n{2,}/g, '\n').trim()
}

export const NOTICE_TEMPLATE_CODES = Object.keys(NOTICE_TEMPLATES)
