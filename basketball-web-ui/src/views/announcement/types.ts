export type AnnouncementCategoryCode = 'EVENT' | 'RULE' | 'PENALTY' | 'PLATFORM'

export type AnnouncementFilterCategory = 'ALL' | AnnouncementCategoryCode

export type AnnouncementStatus = 'NORMAL' | 'ACTIVE' | 'REPLACED' | 'EXPIRED'

export type AnnouncementScopeType = 'GLOBAL' | 'TOURNAMENT' | 'PLATFORM'

export interface AnnouncementCategory {
  code: AnnouncementCategoryCode
  name: string
}

export interface AnnouncementParagraphBlock {
  type: 'PARAGRAPH'
  text: string
  bold?: boolean
  emphasis?: boolean
}

export interface AnnouncementHeadingBlock {
  type: 'HEADING'
  title: string
  level: 2 | 3
}

export interface AnnouncementInfoTableBlock {
  type: 'INFO_TABLE'
  title?: string
  rows: Array<{
    label: string
    value: string
    highlight?: boolean
  }>
}

export interface AnnouncementListBlock {
  type: 'ORDERED_LIST' | 'UNORDERED_LIST'
  items: string[]
}

export interface AnnouncementLinkBlock {
  type: 'LINK'
  label: string
  url: string
}

export interface AnnouncementAttachmentBlock {
  type: 'ATTACHMENT'
  attachmentId: string
  fileName: string
  fileSize: string
  url: string
}

export interface AnnouncementDividerBlock {
  type: 'DIVIDER'
}

export interface AnnouncementNoticeBlock {
  type: 'NOTICE'
  text: string
  tone?: 'info' | 'warning'
}

export type AnnouncementContentBlock =
  | AnnouncementParagraphBlock
  | AnnouncementHeadingBlock
  | AnnouncementInfoTableBlock
  | AnnouncementListBlock
  | AnnouncementLinkBlock
  | AnnouncementAttachmentBlock
  | AnnouncementDividerBlock
  | AnnouncementNoticeBlock

export interface AnnouncementAttachment {
  id: number
  fileName: string
  fileSize: string
  fileType: string
  url: string
}

export interface Announcement {
  id: number
  title: string
  subtitle?: string
  categoryCode: AnnouncementCategoryCode
  categoryName: string
  pinned: boolean
  status: AnnouncementStatus
  scopeType: AnnouncementScopeType
  tournamentId?: string
  tournamentName?: string
  platformCode?: string
  platformName?: string
  publishTime: string
  publishDateText: string
  publisherName: string
  summary: string
  contentBlocks: AnnouncementContentBlock[]
  replacementAnnouncementId?: number
  replacementAnnouncementTitle?: string
  invalidReason?: string
  attachments?: AnnouncementAttachment[]
}

export interface AnnouncementListQuery {
  category: AnnouncementFilterCategory
  keyword: string
  page: number
  pageSize?: number
}

export interface AnnouncementListResult {
  items: Announcement[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface AnnouncementListState {
  category: AnnouncementFilterCategory
  keyword: string
  page: number
}
