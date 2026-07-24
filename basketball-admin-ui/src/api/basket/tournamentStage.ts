import request from '@/utils/request'
import type {
  AjaxResult,
  StageCreateReq,
  StageDetail,
  StageListItem,
  StageQuery,
  StageUpdateReq,
  TableDataInfo
} from '@/types'

export function listStage(query: StageQuery): Promise<TableDataInfo<StageListItem[]>> {
  return request({
    url: '/basket/stage/list',
    method: 'get',
    params: query
  })
}

export function getStage(stageId: number): Promise<AjaxResult<StageDetail>> {
  return request({
    url: `/basket/stage/${stageId}`,
    method: 'get'
  })
}

export function addStage(data: StageCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/stage',
    method: 'post',
    data
  })
}

export function updateStage(stageId: number, data: StageUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/stage/${stageId}`,
    method: 'put',
    data
  })
}

export function activateStage(stageId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/stage/${stageId}/activate`,
    method: 'post'
  })
}

export function finishStage(stageId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/stage/${stageId}/finish`,
    method: 'post'
  })
}

export function cancelStage(stageId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/stage/${stageId}/cancel`,
    method: 'post'
  })
}

export function delStage(stageId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/stage/${stageId}`,
    method: 'delete'
  })
}
