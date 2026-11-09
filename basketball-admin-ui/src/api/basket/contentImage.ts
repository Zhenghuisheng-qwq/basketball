import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface ContentImageResp {
  fileId: number
  accessUrl: string
  originalName?: string
}

export function uploadContentImage(file: File, usageType: string): Promise<AjaxResult<ContentImageResp>> {
  const data = new FormData()
  data.append('file', file)
  data.append('usageType', usageType)
  return request({
    url: '/basket/content-image/upload',
    method: 'post',
    data,
    headers: {
      repeatSubmit: false
    }
  })
}
