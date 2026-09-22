import { getToken } from '@/utils/auth'

const baseURL = import.meta.env.VITE_APP_BASE_API

export function resolveSubmissionMaterialUrl(fileId: string | number) {
  return baseURL + '/basket/v4/match-submission-audit/material/' + String(fileId)
}

export function resolvePrivateFileUrl(fileId: string | number) {
  return baseURL + '/basket/private-file/' + String(fileId)
}

export async function fetchAuthedImageUrl(url: string): Promise<string> {
  const res = await fetch(url, {
    headers: {
      Authorization: 'Bearer ' + getToken(),
      Accept: 'image/*',
    },
  })
  if (!res.ok) {
    throw new Error('file load failed')
  }
  const type = (res.headers.get('content-type') || '').toLowerCase()
  const buf = await res.arrayBuffer()
  if (!buf || buf.byteLength < 8) {
    throw new Error('file content invalid')
  }
  if (type.includes('application/json') || type.includes('text/html')) {
    throw new Error('file response invalid')
  }
  return URL.createObjectURL(new Blob([buf], { type: type.startsWith('image/') ? type : 'image/jpeg' }))
}
