import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketPersonalDrawBatchQuery {
  pageNum?: number
  pageSize?: number
  divisionId?: number
  batchStatus?: string
}

export interface BasketPersonalDrawBatchListVo {
  drawBatchId?: number
  divisionId?: number
  divisionName?: string
  batchCode?: string
  batchName?: string
  batchVersion?: number
  batchStatus?: string
  resultSource?: string
  resultRemark?: string
  unassignedPolicy?: string
  confirmedTime?: string
  createTime?: string
}

export interface BasketPersonalDrawAssignmentVo {
  assignmentId?: number
  personalRegistrationId?: number
  playerId?: number
  playerName?: string
  assignmentRole?: string
  joinStatus?: string
  sortOrder?: number
}

export interface BasketPersonalDrawTeamVo {
  drawTeamId?: number
  drawBatchId?: number
  divisionId?: number
  temporaryTeamCode?: string
  temporaryTeamName?: string
  captainPersonalRegistrationId?: number
  captainPlayerId?: number
  captainPlayerName?: string
  expectedMemberCount?: number
  actualMemberCount?: number
  longTermTeamId?: number
  longTermTeamName?: string
  convertedRegistrationId?: number
  formationStatus?: string
  confirmedTime?: string
  assignments?: BasketPersonalDrawAssignmentVo[]
}

export interface BasketPersonalDrawBatchDetailVo extends BasketPersonalDrawBatchListVo {
  unassignedRemark?: string
  teams?: BasketPersonalDrawTeamVo[]
}

export interface BasketPersonalDrawBatchCreateReq {
  divisionId?: number
  batchCode?: string
  batchName?: string
  resultSource?: string
  resultRemark?: string
}

export interface BasketPersonalDrawTeamCreateReq {
  temporaryTeamCode?: string
  temporaryTeamName?: string
  captainPersonalRegistrationId?: number
  expectedMemberCount?: number
}

export interface BasketPersonalDrawAssignmentCreateReq {
  personalRegistrationId?: number
  assignmentRole?: string
  sortOrder?: number
}

export function listV4PersonalDrawBatch(query: BasketPersonalDrawBatchQuery): Promise<TableDataInfo<BasketPersonalDrawBatchListVo[]>> {
  return request({ url: '/basket/v4/admin/personal-draw-batches/list', method: 'get', params: query })
}

export function getV4PersonalDrawBatchDetail(drawBatchId: number): Promise<AjaxResult<BasketPersonalDrawBatchDetailVo>> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/${drawBatchId}`, method: 'get' })
}

export function createV4PersonalDrawBatch(data: BasketPersonalDrawBatchCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/admin/personal-draw-batches', method: 'post', data })
}

export function createV4PersonalDrawTeam(drawBatchId: number, data: BasketPersonalDrawTeamCreateReq): Promise<AjaxResult<number>> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/${drawBatchId}/teams`, method: 'post', data })
}

export function addV4PersonalDrawAssignment(drawBatchId: number, drawTeamId: number, data: BasketPersonalDrawAssignmentCreateReq): Promise<AjaxResult<number>> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/${drawBatchId}/teams/${drawTeamId}/assignments`, method: 'post', data })
}

export function removeV4PersonalDrawAssignment(drawBatchId: number, assignmentId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/${drawBatchId}/assignments/${assignmentId}`, method: 'delete' })
}

export function confirmV4PersonalDrawBatch(drawBatchId: number, data?: { unassignedPolicy?: string; unassignedRemark?: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/${drawBatchId}/confirm`, method: 'post', data })
}

export function cancelV4PersonalDrawBatch(drawBatchId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/${drawBatchId}/cancel`, method: 'post' })
}

export function bindV4PersonalFormationTeam(drawTeamId: number, data: { longTermTeamId: number }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/teams/${drawTeamId}/bind-team`, method: 'post', data })
}

export function confirmV4PersonalFormation(drawTeamId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-draw-batches/teams/${drawTeamId}/confirm`, method: 'post' })
}
