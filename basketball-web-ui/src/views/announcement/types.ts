export type AnnouncementFilterCategory = string

export interface AnnouncementCategory {
  code: string
  name: string
}

export interface AnnouncementListItem {
  id: string
  title: string
  categoryCode: string
  categoryName: string
  pinned: boolean
  summary: string
  authorName: string
  publishTime: string
  publishDateText: string
  viewCount: number
}

export interface AnnouncementDetail extends AnnouncementListItem {
  contentText: string
}

export interface AnnouncementListState {
  category: AnnouncementFilterCategory
  keyword: string
  page: number
}
