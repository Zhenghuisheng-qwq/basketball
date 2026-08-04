import request from '@/utils/request'
import type { AjaxResult, TableDataInfo } from '@/types'

export interface BasketDivisionGroupingBatchQuery {
  pageNum?: number
  pageSize?: number
  divisionId?: number
  stageId?: number
  groupingStatus?: string
}

export interface BasketDivisionGroupingBatchListVo {
  groupingBatchId?: number
  divisionId?: number
  divisionName?: string
  stageId?: number
  stageCode?: string
  stageName?: string
  batchVersion?: number
  groupingMode?: string
  groupingStatus?: string
  groupCount?: number
  resultSource?: string
  confirmedTime?: string
  createTime?: string
}

export interface BasketDivisionGroupTeamVo {
  assignmentId?: number
  eventTeamId?: number
  teamNameSnapshot?: string
  seedNo?: number
  drawOrder?: number
  assignmentSource?: string
}

export interface BasketDivisionGroupVo {
  groupId?: number
  groupCode?: string
  groupName?: string
  groupOrder?: number
  qualifyCount?: number
  teams?: BasketDivisionGroupTeamVo[]
}

export interface BasketDivisionGroupingBatchDetailVo extends BasketDivisionGroupingBatchListVo {
  groups?: BasketDivisionGroupVo[]
}

export interface BasketDivisionGroupingBatchCreateReq {
  divisionId?: number
  stageId?: number
  resultSource?: string
}

export interface BasketDivisionGroupUpdateReq {
  groupName?: string
  qualifyCount?: number
}

export interface BasketDivisionGroupTeamAssignReq {
  eventTeamId?: number
  seedNo?: number
  drawOrder?: number
  assignmentSource?: string
}

export function listV4DivisionGroupingBatch(query: BasketDivisionGroupingBatchQuery): Promise<TableDataInfo<BasketDivisionGroupingBatchListVo[]>> {
  return request({ url: '/basket/v4/admin/division-grouping-batches/list', method: 'get', params: query })
}

export function getV4DivisionGroupingBatchDetail(groupingBatchId: number): Promise<AjaxResult<BasketDivisionGroupingBatchDetailVo>> {
  return request({ url: `/basket/v4/admin/division-grouping-batches/${groupingBatchId}`, method: 'get' })
}

export function createV4DivisionGroupingBatch(data: BasketDivisionGroupingBatchCreateReq): Promise<AjaxResult<number>> {
  return request({ url: '/basket/v4/admin/division-grouping-batches', method: 'post', data })
}

export function updateV4DivisionGroup(groupingBatchId: number, groupId: number, data: BasketDivisionGroupUpdateReq): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/division-grouping-batches/${groupingBatchId}/groups/${groupId}`, method: 'put', data })
}

export function assignV4DivisionGroupTeam(groupingBatchId: number, groupId: number, data: BasketDivisionGroupTeamAssignReq): Promise<AjaxResult<number>> {
  return request({ url: `/basket/v4/admin/division-grouping-batches/${groupingBatchId}/groups/${groupId}/teams`, method: 'post', data })
}

export function removeV4DivisionGroupAssignment(groupingBatchId: number, assignmentId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/division-grouping-batches/${groupingBatchId}/assignments/${assignmentId}`, method: 'delete' })
}

export function lockV4DivisionGroupingBatch(groupingBatchId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/division-grouping-batches/${groupingBatchId}/lock`, method: 'post' })
}

export function cancelV4DivisionGroupingBatch(groupingBatchId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/division-grouping-batches/${groupingBatchId}/cancel`, method: 'post' })
}
