import request from '@/utils/request'
import type {
  AjaxResult,
  HonorRecordAddReq,
  HonorRecordDetail,
  HonorRecordEditReq,
  HonorRecordListItem,
  HonorRecordQuery,
  HonorRecordRevokeReq,
  PlayerHonorCandidate,
  PlayerHonorCandidateQuery,
  TableDataInfo
} from '@/types'

export function listPlayerHonor(query: HonorRecordQuery): Promise<TableDataInfo<HonorRecordListItem[]>> {
  return request({
    url: '/basket/player-honor/list',
    method: 'get',
    params: query
  })
}

export function listPlayerHonorCandidate(query: PlayerHonorCandidateQuery): Promise<AjaxResult<PlayerHonorCandidate[]>> {
  return request({
    url: '/basket/player-honor/candidate',
    method: 'get',
    params: query
  })
}

export function getPlayerHonor(id: number): Promise<AjaxResult<HonorRecordDetail>> {
  return request({
    url: `/basket/player-honor/${id}`,
    method: 'get'
  })
}

export function addPlayerHonor(data: HonorRecordAddReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/player-honor',
    method: 'post',
    data
  })
}

export function updatePlayerHonor(id: number, data: HonorRecordEditReq): Promise<AjaxResult> {
  return request({
    url: `/basket/player-honor/${id}`,
    method: 'put',
    data
  })
}

export function publishPlayerHonor(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/player-honor/${id}/publish`,
    method: 'post'
  })
}

export function revokePlayerHonor(id: number, data: HonorRecordRevokeReq): Promise<AjaxResult> {
  return request({
    url: `/basket/player-honor/${id}/revoke`,
    method: 'post',
    data
  })
}

export function delPlayerHonor(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/player-honor/${id}`,
    method: 'delete'
  })
}
