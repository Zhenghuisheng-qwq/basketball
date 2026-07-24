import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { FileUploadDto } from '@/api/types/accountFile'

export function uploadAccountFile(file: File, usageType: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('usageType', usageType)
  return request.post<any, ApiResult<FileUploadDto>>('/basket/account/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getAccountFileUrl(fileId: string | number) {
  const base = import.meta.env.VITE_API_BASE_URL || ''
  return `${base}/basket/account/file/${fileId}`
}

export function deleteAccountFile(fileId: string | number) {
  return request.delete<any, ApiResult<null>>(`/basket/account/file/${fileId}`)
}
