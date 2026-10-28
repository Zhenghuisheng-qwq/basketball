import request from '@/utils/request'
import type {
  AjaxResult,
  HonorGenerationBatch,
  HonorGenerationPreview,
  HonorRecordAddReq,
  HonorRecordDetail,
  HonorRecordEditReq,
  HonorRecordListItem,
  HonorRecordQuery,
  HonorRecordRevokeReq,
  TableDataInfo
} from '@/types'

export function listTeamHonor(query: HonorRecordQuery): Promise<TableDataInfo<HonorRecordListItem[]>> {
  return request({
    url: '/basket/team-honor/list',
    method: 'get',
    params: query
  })
}

export function getTeamHonor(id: number): Promise<AjaxResult<HonorRecordDetail>> {
  return request({
    url: `/basket/team-honor/${id}`,
    method: 'get'
  })
}

export function addTeamHonor(data: HonorRecordAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/team-honor',
    method: 'post',
    data
  })
}

export function updateTeamHonor(id: number, data: HonorRecordEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/team-honor/${id}`,
    method: 'put',
    data
  })
}

export function publishTeamHonor(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/team-honor/${id}/publish`,
    method: 'post'
  })
}

export function revokeTeamHonor(id: number, data: HonorRecordRevokeReq): Promise<AjaxResult> {
  return request({
    url: `/basket/team-honor/${id}/revoke`,
    method: 'post',
    data
  })
}

export function replaceTeamHonorLineup(id: number, data: Array<{
  playerId: number
  teamId?: number
  roleType?: string
  positionCode?: string
  sortNo?: number
}>): Promise<AjaxResult> {
  return request({
    url: `/basket/team-honor/${id}/lineup`,
    method: 'put',
    data
  })
}

export function previewHonorGeneration(seasonId: number): Promise<AjaxResult<HonorGenerationPreview>> {
  return request({
    url: `/basket/honor-generation/preview/${seasonId}`,
    method: 'get'
  })
}

export function confirmHonorGeneration(seasonId: number): Promise<AjaxResult<HonorGenerationBatch>> {
  return request({
    url: '/basket/honor-generation/confirm',
    method: 'post',
    data: { seasonId }
  })
}

export function delTeamHonor(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/team-honor/${id}`,
    method: 'delete'
  })
}
