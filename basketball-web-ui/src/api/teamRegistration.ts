import { getTeamMemberPage } from '@/api/teamMember'
import {
  getV4AvailableDivisions,
  getV4MyRegistrations,
  getV4RegistrationDetail,
  saveV4Registration,
  submitV4Registration,
  withdrawV4Registration,
} from '@/api/v4PlayerEvents'
import type { ApiResult, PageResult } from '@/api/types/common'
import type {
  AvailableSeasonItemDto,
  AvailableSeasonPageDto,
  RegistrationRosterCandidateDto,
  SeasonPlatformItemDto,
  TeamRegistrationAuditItemDto,
  TeamRegistrationDetailDto,
  TeamRegistrationDraftReq,
  TeamRegistrationItemDto,
  TeamRegistrationRemindPlatformResultDto,
  TeamRegistrationRosterReq,
  TeamRegistrationUpdateReq,
  TeamRegistrationWithdrawReq,
} from '@/api/types/teamRegistration'
import type { TeamMemberItemDto } from '@/api/types/teamMember'
import type { V4AvailableDivision, V4MyRegistration, V4RegistrationDetail } from '@/api/types/v4PlayerEvents'

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

function mapRegistrationStatus(status?: string): { code: number; text: string } {
  switch (status) {
    case 'DRAFT':
      return { code: 0, text: '待提交' }
    case 'PENDING_REVIEW':
      return { code: 1, text: '审核中' }
    case 'APPROVED':
      return { code: 3, text: '已通过' }
    case 'REJECTED':
      return { code: 4, text: '已驳回' }
    case 'WITHDRAWN':
      return { code: 5, text: '已撤回' }
    case 'CANCELLED':
      return { code: 6, text: '已取消' }
    default:
      return { code: 0, text: '待提交' }
  }
}

function mapAvailableDivision(item: V4AvailableDivision, teamId: string): AvailableSeasonItemDto {
  const status = mapRegistrationStatus(item.myRegistrationStatus)
  return {
    seasonId: item.divisionId,
    seasonName: item.seasonTitle || item.divisionName,
    tournamentId: item.seasonId,
    tournamentName: item.seriesName || item.seasonTitle || item.divisionName,
    platformCode: item.platformCode,
    canRegister: item.canRegister,
    cannotRegisterReason: item.cannotRegisterReason,
    alreadyRegistered: Boolean(item.myRegistrationStatus),
    hasRegistration: Boolean(item.myRegistrationStatus),
    registrationId: status.code === 0 ? undefined : item.divisionId,
    registrationStartTime: item.registrationStartTime,
    registrationEndTime: item.registrationEndTime,
    minRosterCount: item.lineupMinCount,
    maxRosterCount: item.lineupMaxCount,
    teamId,
    userTeamId: teamId,
    hasCaptainGrant: item.myGrantStatus === 'APPROVED' || item.myGrantStatus === 'ACTIVE',
    captainGrantStatus: item.myGrantStatus,
    matchStartTime: item.matchStartTime,
    matchEndTime: item.matchEndTime,
  }
}

function mapRegistrationItem(item: V4MyRegistration): TeamRegistrationItemDto {
  const status = mapRegistrationStatus(item.registrationStatus)
  return {
    registrationId: item.registrationId,
    registrationNo: item.registrationId,
    seasonId: item.divisionId || item.seasonId,
    seasonName: item.seasonTitle || item.divisionName,
    tournamentName: item.seasonTitle || item.divisionName,
    platformCode: item.platformCode,
    registrationStatus: status.code,
    registrationStatusText: status.text,
    lineupCount: item.lineupCount,
    canEdit: item.registrationStatus === 'DRAFT' || item.registrationStatus === 'REJECTED',
    canSubmit: item.registrationStatus === 'DRAFT' || item.registrationStatus === 'REJECTED',
    canWithdraw: item.registrationStatus === 'PENDING_REVIEW',
    submittedTime: item.submitTime,
  }
}

function mapRegistrationDetail(detail: V4RegistrationDetail): TeamRegistrationDetailDto {
  const item = mapRegistrationItem(detail)
  const status = mapRegistrationStatus(detail.registrationStatus)
  return {
    ...item,
    teamId: detail.teamId,
    teamName: detail.teamName,
    registrationStatusText: status.text,
    rosterItems: (detail.players || []).map((player) => ({
      teamMemberId: player.playerId,
      playerId: player.playerId,
      playerNo: '',
      gameNickname: player.playerName,
      rosterRole: player.isCaptain ? 'CAPTAIN' : 'PLAYER',
      isStarting: 0,
      isCaptain: player.isCaptain,
      sortNo: player.sortOrder,
    })),
    selectedRosterCount: detail.lineupCount ?? detail.players?.length ?? 0,
    canDeleteDraft: detail.registrationStatus === 'DRAFT',
  }
}

function mapMemberToCandidate(member: TeamMemberItemDto): RegistrationRosterCandidateDto {
  const hasPlatformId = Boolean(member.gameAccount || member.gameIdSummary || member.platformGameIds?.length)
  return {
    teamMemberId: member.teamMemberId,
    playerId: member.playerId,
    playerNo: member.playerNo,
    gameNickname: member.nickname || member.gameNickname,
    nickname: member.nickname,
    positionCode: member.positionCode,
    positionName: member.positionName,
    jerseyNo: member.jerseyNo,
    roleType: member.roleType,
    platformGameId: member.gameAccount || member.gameIdSummary,
    platformIdentityStatus: hasPlatformId ? 'EFFECTIVE' : 'MISSING',
    platformIdentityStatusText: hasPlatformId ? '已绑定' : '未绑定',
    canSelect: member.certificationStatus === 3 && hasPlatformId,
    cannotSelectReason: member.certificationStatus !== 3
      ? '未完成球员认证'
      : !hasPlatformId
        ? '未绑定平台游戏 ID'
        : undefined,
  }
}

async function loadRegistrationDetail(registrationId: string) {
  const res = await getV4RegistrationDetail(registrationId)
  return mapRegistrationDetail(res.data)
}

async function resolvePlayerIds(
  teamId: string,
  rosterItems: TeamRegistrationRosterReq['rosterItems'],
): Promise<string[]> {
  const res = await getTeamMemberPage(teamId, { currentPage: 1, pageSize: 200 })
  const memberMap = new Map(
    (res.data?.records || []).map((member) => [member.teamMemberId, member.playerId]),
  )
  const playerIds = rosterItems
    .map((item) => memberMap.get(item.teamMemberId) || item.teamMemberId)
    .filter(Boolean)
  if (!playerIds.length) {
    throw new Error('请至少选择一名球员')
  }
  return playerIds
}

export async function getAvailableSeasonPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  const res = await getV4AvailableDivisions()
  const items = (res.data || [])
    .filter((item) => !item.grantTeamId || item.grantTeamId === teamId)
    .map((item) => mapAvailableDivision(item, teamId))
  const page = paginate(items, params?.currentPage, params?.pageSize)
  return {
    ...res,
    data: {
      ...page,
      emptyReason: page.records.length ? undefined : '暂无可报名赛事',
    },
  } as ApiResult<AvailableSeasonPageDto>
}

export async function getTeamRegistrationPage(
  teamId: string,
  params?: { currentPage?: number; pageSize?: number; status?: number },
) {
  const res = await getV4MyRegistrations()
  let items = (res.data || [])
    .filter((item) => item.teamId === teamId)
    .map(mapRegistrationItem)
  if (params?.status != null) {
    items = items.filter((item) => item.registrationStatus === params.status)
  }
  return {
    ...res,
    data: paginate(items, params?.currentPage, params?.pageSize),
  } as ApiResult<PageResult<TeamRegistrationItemDto>>
}

export async function createTeamRegistrationDraft(teamId: string, body: TeamRegistrationDraftReq) {
  const res = await saveV4Registration({
    divisionId: body.seasonId,
    teamId,
    playerIds: [],
  })
  const detail = await loadRegistrationDetail(res.data)
  return {
    ...res,
    data: detail,
  } as ApiResult<TeamRegistrationDetailDto>
}

export async function getTeamRegistrationDetail(registrationId: string) {
  const detail = await loadRegistrationDetail(registrationId)
  return {
    code: 200,
    data: detail,
  } as ApiResult<TeamRegistrationDetailDto>
}

export async function updateTeamRegistration(
  registrationId: string,
  body: TeamRegistrationUpdateReq,
) {
  const detail = await loadRegistrationDetail(registrationId)
  return {
    code: 200,
    data: {
      ...detail,
      contactName: body.contactName ?? detail.contactName,
      contactMobileMasked: body.contactMobile ?? detail.contactMobileMasked,
      contactEmailMasked: body.contactEmail ?? detail.contactEmailMasked,
      remark: body.remark ?? detail.remark,
    },
  } as ApiResult<TeamRegistrationDetailDto>
}

export async function saveTeamRegistrationRoster(
  registrationId: string,
  body: TeamRegistrationRosterReq,
) {
  const current = await loadRegistrationDetail(registrationId)
  if (!current.teamId) {
    throw new Error('缺少球队信息，无法保存阵容')
  }
  const playerIds = await resolvePlayerIds(current.teamId, body.rosterItems)
  await saveV4Registration({
    divisionId: current.seasonId || '',
    teamId: current.teamId,
    playerIds,
  })
  const detail = await loadRegistrationDetail(registrationId)
  return {
    code: 200,
    data: detail,
  } as ApiResult<TeamRegistrationDetailDto>
}

export async function submitTeamRegistration(registrationId: string) {
  await submitV4Registration({ registrationId })
  const detail = await loadRegistrationDetail(registrationId)
  return {
    code: 200,
    data: detail,
  } as ApiResult<TeamRegistrationDetailDto>
}

export async function withdrawTeamRegistration(
  registrationId: string,
  _body?: TeamRegistrationWithdrawReq,
) {
  await withdrawV4Registration({ registrationId })
  const detail = await loadRegistrationDetail(registrationId)
  return {
    code: 200,
    data: detail,
  } as ApiResult<TeamRegistrationDetailDto>
}

export async function deleteTeamRegistrationDraft(registrationId: string) {
  const detail = await loadRegistrationDetail(registrationId)
  if (detail.registrationStatus === 0) {
    await withdrawV4Registration({ registrationId })
  }
  return {
    code: 200,
    data: null,
  } as ApiResult<null>
}

export async function getTeamRegistrationAuditPage(
  _registrationId: string,
  params?: { currentPage?: number; pageSize?: number },
) {
  const page = paginate<TeamRegistrationAuditItemDto>([], params?.currentPage, params?.pageSize)
  return {
    code: 200,
    data: page,
  } as ApiResult<PageResult<TeamRegistrationAuditItemDto>>
}

export async function getSeasonPlatforms(seasonId: string) {
  const res = await getV4AvailableDivisions()
  const platforms = new Map<string, SeasonPlatformItemDto>()
  for (const item of res.data || []) {
    if (item.divisionId !== seasonId && item.seasonId !== seasonId) continue
    if (!item.platformCode) continue
    platforms.set(item.platformCode, {
      platformCode: item.platformCode,
      platformName: item.platformCode,
      open: item.canRegister !== false,
      disabledReason: item.cannotRegisterReason,
    })
  }
  return {
    ...res,
    data: Array.from(platforms.values()),
  } as ApiResult<SeasonPlatformItemDto[]>
}

export async function getRegistrationRosterCandidates(registrationId: string) {
  const detail = await loadRegistrationDetail(registrationId)
  if (!detail.teamId) {
    return { code: 200, data: [] } as ApiResult<RegistrationRosterCandidateDto[]>
  }
  const res = await getTeamMemberPage(detail.teamId, { currentPage: 1, pageSize: 200 })
  return {
    ...res,
    data: (res.data?.records || []).map(mapMemberToCandidate),
  } as ApiResult<RegistrationRosterCandidateDto[]>
}

export async function remindMemberPlatformGameId(
  _registrationId: string,
  _teamMemberId: string,
) {
  return {
    code: 200,
    data: {
      success: false,
      message: 'V4 暂不支持平台 ID 提醒，请联系球员自行绑定',
    },
  } as ApiResult<TeamRegistrationRemindPlatformResultDto>
}
