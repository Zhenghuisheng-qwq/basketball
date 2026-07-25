import request from '@/utils/request'
import type {
  AjaxResult,
  HonorGenerationBatch,
  HonorGenerationPreview,
  TableDataInfo
} from '@/types'

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

export function revokeHonorGenerationBatch(batchId: number): Promise<AjaxResult> {
  return request({
    url: `/basket/honor-generation/batch/${batchId}/revoke`,
    method: 'post'
  })
}

export function listHonorGenerationBatches(seasonId: number): Promise<AjaxResult<HonorGenerationBatch[]>> {
  return request({
    url: `/basket/honor-generation/batch/list/${seasonId}`,
    method: 'get'
  })
}
