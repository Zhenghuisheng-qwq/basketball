import request from '@/utils/request'
import type {
  AjaxResult,
  ScheduleEventContext,
  ScheduleGeneratePreview,
  ScheduleGenerateReq,
  ScheduleGenerateResult,
  ScheduleMatchCreateReq,
  ScheduleMatchDetailItem,
  ScheduleMatchListItem,
  ScheduleMatchQuery,
  ScheduleMatchUpdateReq,
  SchedulePublishReq,
  ScheduleRuleDetail,
  ScheduleRuleListItem,
  ScheduleScoreSaveReq,
  TableDataInfo
} from '@/types'

export function listScheduleRule(): Promise<AjaxResult<ScheduleRuleListItem[]>> {
  return request({ url: '/basket/schedule/rule/list', method: 'get' })
}

export function getScheduleRuleDetail(ruleId: number): Promise<AjaxResult<ScheduleRuleDetail>> {
  return request({ url: `/basket/schedule/rule/${ruleId}`, method: 'get' })
}

export function getScheduleEventContext(eventId: number): Promise<AjaxResult<ScheduleEventContext>> {
  return request({ url: `/basket/schedule/manage/event/${eventId}/context`, method: 'get' })
}

export function previewScheduleGenerate(data: ScheduleGenerateReq): Promise<AjaxResult<ScheduleGeneratePreview>> {
  return request({ url: '/basket/schedule/manage/preview', method: 'post', data })
}

export function generateScheduleDraft(data: ScheduleGenerateReq): Promise<AjaxResult<ScheduleGenerateResult>> {
  return request({ url: '/basket/schedule/manage/generate', method: 'post', data })
}

export function publishSchedule(data: SchedulePublishReq): Promise<AjaxResult> {
  return request({ url: '/basket/schedule/manage/publish', method: 'post', data })
}

export function unpublishSchedule(data: SchedulePublishReq): Promise<AjaxResult> {
  return request({ url: '/basket/schedule/manage/unpublish', method: 'post', data })
}

export function deleteScheduleDraft(eventId: number, reason?: string): Promise<AjaxResult> {
  return request({ url: `/basket/schedule/manage/draft/${eventId}`, method: 'delete', params: { reason } })
}

export function listScheduleMatch(query: ScheduleMatchQuery): Promise<TableDataInfo<ScheduleMatchListItem[]>> {
  return request({ url: '/basket/schedule/manage/match/list', method: 'get', params: query })
}

export function createScheduleMatch(data: ScheduleMatchCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/schedule/manage/match', method: 'post', data })
}

export function updateScheduleMatch(matchId: number, data: ScheduleMatchUpdateReq): Promise<AjaxResult> {
  return request({ url: `/basket/schedule/manage/match/${matchId}`, method: 'put', data })
}

export function getScheduleMatchDetail(matchId: number): Promise<AjaxResult<ScheduleMatchDetailItem>> {
  return request({ url: `/basket/schedule/manage/match/${matchId}`, method: 'get' })
}

export function swapScheduleMatch(matchId: number, reason?: string): Promise<AjaxResult> {
  return request({ url: `/basket/schedule/manage/match/${matchId}/swap`, method: 'post', params: { reason } })
}

export function deleteScheduleMatch(matchId: number, reason?: string): Promise<AjaxResult> {
  return request({ url: `/basket/schedule/manage/match/${matchId}`, method: 'delete', params: { reason } })
}

export function saveScheduleScore(matchId: number, data: ScheduleScoreSaveReq): Promise<AjaxResult> {
  return request({ url: `/basket/schedule/manage/match/${matchId}/score`, method: 'post', data })
}
