import request from '@/api/request'
import type { ApiResult } from '@/api/types/common'
import type { FileUploadDto } from '@/api/types/accountFile'
import { getToken } from '@/utils/auth'

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

/** 私有截图必须带登录态拉取，不能直接当 img src。 */
export async function fetchAccountFileBlobUrl(fileId: string | number): Promise<string> {
  const token = getToken()
  const res = await fetch(getAccountFileUrl(fileId), {
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      Accept: 'image/*',
    },
  })
  if (!res.ok) {
    throw new Error('文件加载失败')
  }
  const buf = await res.arrayBuffer()
  if (!buf || buf.byteLength < 8) {
    throw new Error('文件内容无效')
  }
  const type = res.headers.get('content-type') || ''
  if (type && !type.toLowerCase().startsWith('image/')) {
    throw new Error('文件不是图片')
  }
  return URL.createObjectURL(new Blob([buf], { type: type || 'image/jpeg' }))
}

export function deleteAccountFile(fileId: string | number) {
  return request.delete<any, ApiResult<null>>(`/basket/account/file/${fileId}`)
}
