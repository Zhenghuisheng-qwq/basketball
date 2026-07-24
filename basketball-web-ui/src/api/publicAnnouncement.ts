import request from '@/api/request'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  PublicAnnouncementAdjacentResp,
  PublicAnnouncementCategoryResp,
  PublicAnnouncementDetailResp,
  PublicAnnouncementItemResp,
} from '@/api/types/public'

export function getPublicAnnouncementCategories() {
  return request.get<any, ApiResult<PublicAnnouncementCategoryResp[]>>(
    '/basket/public/announcement/category',
  )
}

export function getPublicAnnouncementPage(params?: {
  currentPage?: number
  pageSize?: number
  categoryCode?: string
  keyword?: string
  sortField?: string
  sortOrder?: string
}) {
  return request.get<any, ApiResult<PageResult<PublicAnnouncementItemResp>>>(
    '/basket/public/announcement/page',
    { params },
  )
}

export function getPublicAnnouncementDetail(id: string | number) {
  return request.get<any, ApiResult<PublicAnnouncementDetailResp>>(
    `/basket/public/announcement/${id}`,
  )
}

export function getPublicAnnouncementPreviousNext(id: string | number) {
  return request.get<any, ApiResult<PublicAnnouncementAdjacentResp>>(
    `/basket/public/announcement/${id}/previous-next`,
  )
}
