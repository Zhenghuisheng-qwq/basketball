import type { AvailableEvent } from '@/types/accountEvents'

export type EventStatusTagVariant =
  | 'not-started'
  | 'open'
  | 'closed'
  | 'match-not-started'
  | 'in-progress'
  | 'ended'
  | 'cancelled'
  | 'default'

export interface EventStatusTagView {
  variant: EventStatusTagVariant
  label: string
}

export function resolveEventStatusTag(event: AvailableEvent): EventStatusTagView {
  const code = (event.eventStatusCode || '').toUpperCase()
  const label = event.eventStatus || '-'

  if (code === 'NOT_STARTED' || label.includes('报名未开始')) {
    return { variant: 'not-started', label: label === '-' ? '报名未开始' : label }
  }
  if (code === 'OPEN' || label.includes('报名中')) {
    return { variant: 'open', label: label === '-' ? '报名中' : label }
  }
  if (code === 'IN_PROGRESS' || label.includes('进行中') || label.includes('比赛中')) {
    return { variant: 'in-progress', label: label === '-' ? '比赛中' : label }
  }
  if (code === 'ENDED' || label.includes('已结束')) {
    return { variant: 'ended', label: label === '-' ? '已结束' : label }
  }
  if (code === 'CANCELLED' || label.includes('已取消')) {
    return { variant: 'cancelled', label: label === '-' ? '已取消' : label }
  }
  if (code === 'CLOSED' || label.includes('报名已截止') || label.includes('报名已结束')) {
    // 报名窗口关闭与比赛是否开始解耦：列表「赛事状态」优先展示报名窗口语义
    return { variant: 'closed', label: label === '-' || label.includes('报名已结束') ? '报名已截止' : label }
  }
  if (label.includes('比赛未开始')) {
    return { variant: 'match-not-started', label }
  }
  return { variant: 'default', label }
}
