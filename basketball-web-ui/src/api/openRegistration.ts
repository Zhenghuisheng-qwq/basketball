import {
  getV4MyPersonalRegistrations,
  getV4PersonalAvailableDivisions,
  saveV4PersonalRegistration,
  submitV4PersonalRegistration,
  withdrawV4PersonalRegistration,
} from '@/api/v4PlayerEvents'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  OpenAvailableSeasonItemDto,
  OpenRegistrationDetailDto,
  OpenRegistrationDraftReq,
  OpenRegistrationItemDto,
  OpenRegistrationWithdrawReq,
} from '@/api/types/openRegistration'
import type { V4MyPersonalRegistration, V4PersonalAvailableDivision } from '@/api/types/v4PlayerEvents'

function paginate<T>(items: T[], currentPage = 1, pageSize = 10): PageResult<T> {
  const current = Math.max(1, currentPage)
  const size = Math.max(1, pageSize)
  const total = items.length
  const pages = Math.max(1, Math.ceil(total / size))
  const start = (current - 1) * size
  return {
    records: items.slice(start, start + size),
    total,
    size,
    current,
    pages,
  }
}

function mapPersonalStatus(status?: string): { code: number; text: string } {
  switch (status) {
    case 'DRAFT':
      return { code: 0, text: '草稿' }
    case 'PENDING_REVIEW':
      return { code: 1, text: '待审核' }
    case 'APPROVED':
    case 'ASSIGNED':
    case 'CONVERTED':
      return { code: 3, text: '已通过' }
    case 'REJECTED':
      return { code: 4, text: '已驳回' }
    case 'WITHDRAWN':
      return { code: 5, text: '已撤回' }
    default:
      return { code: 0, text: '草稿' }
  }
}

function mapAvailableDivision(item: V4PersonalAvailableDivision): OpenAvailableSeasonItemDto {
  const status = mapPersonalStatus(item.myRegistrationStatus)
  return {
    seasonId: item.divisionId,
    seasonName: item.seasonTitle || item.divisionName,
    tournamentId: item.seasonId,
    tournamentName: item.seriesName || item.seasonTitle || item.divisionName,
    registrationStartTime: item.registrationStartTime,
    registrationEndTime: item.registrationEndTime,
    hasRegistration: Boolean(item.myRegistrationStatus),
    registrationId: status.code === 0 ? undefined : item.divisionId,
  }
}

function mapPersonalRegistration(item: V4MyPersonalRegistration): OpenRegistrationItemDto {
  const status = mapPersonalStatus(item.registrationStatus)
  return {
    registrationId: item.personalRegistrationId,
    registrationNo: item.personalRegistrationId,
    seasonId: item.divisionId || item.seasonId,
    seasonName: item.seasonTitle || item.divisionName,
    tournamentName: item.seasonTitle || item.divisionName,
    platform: item.platformCode,
    registrationStatus: status.code,
    registrationStatusText: status.text,
    canEdit: item.registrationStatus === 'DRAFT' || item.registrationStatus === 'REJECTED',
    canSubmit: item.registrationStatus === 'DRAFT' || item.registrationStatus === 'REJECTED',
    canWithdraw: item.registrationStatus === 'PENDING_REVIEW',
    submittedTime: item.submitTime,
  }
}

function mapPersonalRegistrationDetail(item: V4MyPersonalRegistration): OpenRegistrationDetailDto {
  const mapped = mapPersonalRegistration(item)
  return {
    ...mapped,
    positionCode: item.preferredPosition,
    remark: item.rejectReason,
    registrationEndTime: undefined,
  }
}

async function findPersonalRegistration(registrationId: string) {
  const res = await getV4MyPersonalRegistrations()
  const item = (res.data || []).find((row) => row.personalRegistrationId === registrationId)
  if (!item) {
    throw new Error('未找到 Open 报名信息')
  }
  return item
}

export async function getOpenAvailableSeasonPage(params?: { currentPage?: number; pageSize?: number }) {
  const res = await getV4PersonalAvailableDivisions()
  const items = (res.data || []).map(mapAvailableDivision)
  return {
    ...res,
    data: paginate(items, params?.currentPage, params?.pageSize),
  } as ApiResult<PageResult<OpenAvailableSeasonItemDto>>
}

export async function createOpenRegistrationDraft(body: OpenRegistrationDraftReq) {
  const res = await saveV4PersonalRegistration({
    divisionId: body.seasonId,
    preferredPosition: body.positionCode,
  })
  const registrationId = res.data
  const saved = await findPersonalRegistration(registrationId)
  return {
    ...res,
    data: mapPersonalRegistrationDetail(saved),
  } as ApiResult<OpenRegistrationDetailDto>
}

export async function getOpenRegistrationDetail(registrationId: string) {
  const item = await findPersonalRegistration(registrationId)
  return {
    code: 200,
    data: mapPersonalRegistrationDetail(item),
  } as ApiResult<OpenRegistrationDetailDto>
}

export async function submitOpenRegistration(registrationId: string) {
  await submitV4PersonalRegistration({ personalRegistrationId: registrationId })
  const item = await findPersonalRegistration(registrationId)
  return {
    code: 200,
    data: mapPersonalRegistrationDetail(item),
  } as ApiResult<OpenRegistrationDetailDto>
}

export async function withdrawOpenRegistration(
  registrationId: string,
  _body?: OpenRegistrationWithdrawReq,
) {
  await withdrawV4PersonalRegistration({ personalRegistrationId: registrationId })
  const item = await findPersonalRegistration(registrationId)
  return {
    code: 200,
    data: mapPersonalRegistrationDetail(item),
  } as ApiResult<OpenRegistrationDetailDto>
}

export async function getOpenRegistrationPage(params?: {
  currentPage?: number
  pageSize?: number
  status?: number
}) {
  const res = await getV4MyPersonalRegistrations()
  let items = (res.data || []).map(mapPersonalRegistration)
  if (params?.status != null) {
    items = items.filter((item) => item.registrationStatus === params.status)
  }
  return {
    ...res,
    data: paginate(items, params?.currentPage, params?.pageSize),
  } as ApiResult<PageResult<OpenRegistrationItemDto>>
}
