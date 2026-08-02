import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { PageResult, PlayerMatchTaskItem } from '@/api/matchData'

export type MyScheduleMatchStatusGroup = 'PENDING' | 'RUNNING' | 'FINISHED' | 'CANCELLED' | 'ALL'

export interface MyScheduleQuery {
  seasonId?: string
  matchStatusGroup?: MyScheduleMatchStatusGroup
  startDate?: string
  endDate?: string
  sortOrder?: 'ASC' | 'DESC'
  pageNum?: number
  pageSize?: number
}

export interface MySchedulePageResult extends PageResult<PlayerMatchTaskItem> {
  nextMatch?: PlayerMatchTaskItem | null
  certificationRequired?: boolean
  noParticipation?: boolean
  pageNum?: number
  pageSize?: number
}

export interface MyScheduleEventOption {
  seasonId?: string
  eventCode?: string
  eventName?: string
  seasonName?: string
}

export function getMySchedules(params?: MyScheduleQuery) {
  return request.get<any, ApiResult<MySchedulePageResult>>('/basket/player/my-schedules', { params })
}

export function getMyScheduleEventOptions() {
  return request.get<any, ApiResult<MyScheduleEventOption[]>>('/basket/player/my-schedules/event-options')
}
