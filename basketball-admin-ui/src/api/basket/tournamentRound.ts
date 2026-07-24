import request from '@/utils/request'
import type {
  AjaxResult,
  RoundCreateReq,
  RoundDetail,
  RoundListResult,
  RoundUpdateReq
} from '@/types'

export function listRoundByStage(stageId: number): Promise<RoundListResult> {
  return request({
    url: `/basket/stage/${stageId}/round/list`,
    method: 'get'
  })
}

export function getRound(roundId: number): Promise<AjaxResult<RoundDetail>> {
  return request({
    url: `/basket/round/${roundId}`,
    method: 'get'
  })
}

export function addRound(stageId: number, data: RoundCreateReq): Promise<AjaxResult<number>> {
  return request({
    url: `/basket/stage/${stageId}/round`,
    method: 'post',
    data
  })
}

export function updateRound(roundId: number, data: RoundUpdateReq): Promise<AjaxResult> {
  return request({
    url: `/basket/round/${roundId}`,
    method: 'put',
    data
  })
}

export function activateRound(roundId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/round/${roundId}/activate`,
    method: 'post'
  })
}

export function finishRound(roundId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/round/${roundId}/finish`,
    method: 'post'
  })
}

export function cancelRound(roundId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/round/${roundId}/cancel`,
    method: 'post'
  })
}

export function delRound(roundId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/round/${roundId}`,
    method: 'delete'
  })
}
