import type { PageDomain } from '../common'

export interface MailNoticeQuery extends PageDomain {
  sendStatus?: number
  keyword?: string
}

export interface MailNoticeSaveReq {
  title: string
  contentHtml: string
}

export interface MailNoticeItem {
  id: number
  title?: string
  contentHtml?: string
  sendStatus?: number
  sendStatusName?: string
  recipientCount?: number
  successCount?: number
  failCount?: number
  createBy?: string
  createTime?: string
  updateTime?: string
  sendTime?: string
}

export interface MailNoticeRecipientQuery extends PageDomain {
  sendStatus?: number
  keyword?: string
}

export interface MailNoticeRecipient {
  id: number
  noticeId?: number
  memberId?: number
  username?: string
  nickname?: string
  email?: string
  sendStatus?: number
  sendStatusName?: string
  failReason?: string
  sendTime?: string
}
