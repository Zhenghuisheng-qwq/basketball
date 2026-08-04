import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type {
  V4AvailableDivision,
  V4MyEvent,
  V4MyRegistration,
  V4MySchedulePage,
  V4PersonalAvailableDivision,
  V4PersonalFormationBindTeamReq,
  V4PersonalRegistrationIdReq,
  V4PersonalRegistrationSaveReq,
  V4RegistrationIdReq,
  V4RegistrationSaveReq,
  V4MyPersonalRegistration,
  V4PersonalFormationTask,
} from '@/api/types/v4PlayerEvents'

export function getV4AvailableDivisions() {
  return request.get<any, ApiResult<V4AvailableDivision[]>>('/basket/v4/player/registration/available-divisions')
}

export function getV4MyRegistrations() {
  return request.get<any, ApiResult<V4MyRegistration[]>>('/basket/v4/player/registration/my-list')
}

export function getV4RegistrationDetail(registrationId: string) {
  return request.get<any, ApiResult<V4RegistrationDetail>>(`/basket/v4/player/registration/${registrationId}/detail`)
}

export function saveV4Registration(data: V4RegistrationSaveReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/registration/save', data)
}

export function submitV4Registration(data: V4RegistrationIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/registration/submit', data)
}

export function withdrawV4Registration(data: V4RegistrationIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/registration/withdraw', data)
}

export function getV4MyEvents() {
  return request.get<any, ApiResult<V4MyEvent[]>>('/basket/v4/player/my-events')
}

export function getV4MySchedule(params?: {
  seasonId?: string
  divisionId?: string
  status?: string
  includeFinished?: boolean
  onlyPendingSubmit?: boolean
}) {
  return request.get<any, ApiResult<V4MySchedulePage>>('/basket/v4/player/my-schedule', { params })
}

export function getV4PersonalAvailableDivisions() {
  return request.get<any, ApiResult<V4PersonalAvailableDivision[]>>('/basket/v4/player/personal-registration/available-divisions')
}

export function saveV4PersonalRegistration(data: V4PersonalRegistrationSaveReq) {
  return request.post<any, ApiResult<string>>('/basket/v4/player/personal-registration/save', data)
}

export function submitV4PersonalRegistration(data: V4PersonalRegistrationIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/personal-registration/submit', data)
}

export function withdrawV4PersonalRegistration(data: V4PersonalRegistrationIdReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/personal-registration/withdraw', data)
}

export function getV4MyPersonalRegistrations() {
  return request.get<any, ApiResult<V4MyPersonalRegistration[]>>('/basket/v4/player/personal-registration/my-list')
}

export function getV4PersonalFormationTasks() {
  return request.get<any, ApiResult<V4PersonalFormationTask[]>>('/basket/v4/player/personal-formation/tasks')
}

export function getV4PersonalFormationDetail(drawTeamId: string) {
  return request.get<any, ApiResult<V4PersonalFormationTask>>(`/basket/v4/player/personal-formation/tasks/${drawTeamId}`)
}

export function bindV4PersonalFormationTeam(data: V4PersonalFormationBindTeamReq) {
  return request.post<any, ApiResult<void>>('/basket/v4/player/personal-formation/bind-team', data)
}

export function confirmV4PersonalFormation(drawTeamId: string) {
  return request.post<any, ApiResult<void>>(`/basket/v4/player/personal-formation/tasks/${drawTeamId}/confirm`)
}
