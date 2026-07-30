import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { AvailableSeasonPageDto } from '@/api/types/teamRegistration'
import type { CompetitionSeasonOptionDto, MyEventsContextDto, MyEventsSeasonPageQuery } from '@/api/types/myEvents'

export function getMyEventsContext() {
  return request.get<any, ApiResult<MyEventsContextDto>>('/basket/account/my-events/context')
}

export function getMyEventsCompetitionSeasonOptions() {
  return request.get<any, ApiResult<CompetitionSeasonOptionDto[]>>(
    '/basket/account/my-events/competition-season/options',
  )
}

export function getMyEventsSeasonPage(params?: MyEventsSeasonPageQuery) {
  return request.get<any, ApiResult<AvailableSeasonPageDto>>('/basket/account/my-events/season/page', {
    params,
  })
}

export function remindCaptainToRegister(teamId: string, body: { seasonId: string }) {
  return request.post<any, ApiResult<null>>(`/basket/team/${teamId}/registration/remind-captain`, body)
}

export function getMemberTeamEventPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  return request.get<any, ApiResult<import('@/api/types/myEvents').TeamEventRecordPageDto>>(
    `/basket/team/${teamId}/registration/member/event/page`,
    { params },
  )
}

export function getMemberAvailableSeasonPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  return request.get<any, ApiResult<AvailableSeasonPageDto>>(
    `/basket/team/${teamId}/registration/member/available-season/page`,
    { params },
  )
}
