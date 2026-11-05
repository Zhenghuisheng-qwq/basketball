import request from '@/utils/request'
import type {
  AjaxResult,
  MailNoticeItem,
  MailNoticeQuery,
  MailNoticeRecipient,
  MailNoticeRecipientQuery,
  MailNoticeSaveReq,
  TableDataInfo
} from '@/types'

export function listMailNotice(query: MailNoticeQuery): Promise<TableDataInfo<MailNoticeItem[]>> {
  return request({
    url: '/basket/mail-notice/list',
    method: 'get',
    params: query
  })
}

export function getMailNotice(id: number): Promise<AjaxResult<MailNoticeItem>> {
  return request({
    url: `/basket/mail-notice/${id}`,
    method: 'get'
  })
}

export function addMailNotice(data: MailNoticeSaveReq): Promise<AjaxResult<number>> {
  return request({
    url: '/basket/mail-notice',
    method: 'post',
    data
  })
}

export function updateMailNotice(id: number, data: MailNoticeSaveReq): Promise<AjaxResult> {
  return request({
    url: `/basket/mail-notice/${id}`,
    method: 'put',
    data
  })
}

export function delMailNotice(id: number): Promise<AjaxResult> {
  return request({
    url: `/basket/mail-notice/${id}`,
    method: 'delete'
  })
}

export function sendMailNotice(id: number): Promise<AjaxResult<MailNoticeItem>> {
  return request({
    url: `/basket/mail-notice/${id}/send`,
    method: 'post',
    timeout: 120000
  })
}

export function listMailNoticeRecipients(id: number, query: MailNoticeRecipientQuery): Promise<TableDataInfo<MailNoticeRecipient[]>> {
  return request({
    url: `/basket/mail-notice/${id}/recipients`,
    method: 'get',
    params: query
  })
}
