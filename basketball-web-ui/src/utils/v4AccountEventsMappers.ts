import type {
  V4AvailableDivision,
  V4MyEvent,
  V4MyPersonalRegistration,
  V4MyRegistration,
  V4PersonalAvailableDivision,
} from '@/api/types/v4PlayerEvents'
import type { AvailableEvent, MyApplication, ParticipatingEvent } from '@/types/accountEvents'
import { formatDateTimeShort } from '@/utils/participationMappers'
import { v4PlatformLabel } from '@/utils/v4Display'

export function v4RegistrationStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return '草稿'
    case 'PENDING_REVIEW':
      return '审核中'
    case 'REJECTED':
      return '已驳回'
    case 'WITHDRAWN':
      return '已撤回'
    case 'APPROVED':
      return '已通过'
    case 'CANCELLED':
      return '已取消'
    default:
      return status || '未知'
  }
}

export function v4SubmissionStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return '待提交'
    case 'PENDING_REVIEW':
      return '审核中'
    case 'REJECTED':
      return '已驳回'
    case 'APPROVED':
      return '已通过'
    case 'SUPERSEDED':
      return '已作废'
    case 'CANCELLED':
      return '已取消'
    default:
      return status ? '未提交' : '未提交'
  }
}

export function v4GrantStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE':
      return '已授权'
    case 'REVOKED':
      return '已撤销'
    case 'EXPIRED':
      return '已过期'
    default:
      return status ? '无授权' : '无授权'
  }
}

export function v4PersonalRegistrationStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return '草稿'
    case 'PENDING_REVIEW':
      return '待审核'
    case 'APPROVED':
      return '已通过'
    case 'REJECTED':
      return '已驳回'
    case 'WITHDRAWN':
      return '已撤回'
    case 'ASSIGNED':
      return '已分配'
    case 'CONVERTED':
      return '已转为正式参赛'
    default:
      return status || '未知'
  }
}

export function mapV4PersonalDivisionToAvailableEvent(item: V4PersonalAvailableDivision): AvailableEvent {
  const canRegister = Boolean(item.canRegister)
  const status = (item.myRegistrationStatus || '').toUpperCase()
  let actionType: AvailableEvent['actionType'] = 'DISABLED'
  let actionText = '不可报名'
  if (canRegister) {
    actionType = 'INDIVIDUAL_REGISTRATION'
    actionText = '个人报名'
  } else if (status === 'DRAFT' || status === 'REJECTED' || status === 'WITHDRAWN') {
    actionType = 'MANAGE_REGISTRATION'
    actionText = status === 'DRAFT' ? '继续编辑' : '修改重提'
  } else if (status === 'PENDING_REVIEW') {
    actionType = 'VIEW_REGISTRATION'
    actionText = '查看报名'
  } else if (status === 'APPROVED' || status === 'ASSIGNED') {
    actionType = 'VIEW_REGISTRATION'
    actionText = '查看进度'
  } else if (status === 'CONVERTED') {
    actionType = 'GO_JOINED'
    actionText = '进入赛事'
  }
  return {
    divisionId: item.divisionId,
    seasonId: item.seasonId,
    tournamentName: item.seriesName || item.seasonTitle || '赛事',
    seasonName: item.seasonTitle,
    displayTitle: item.divisionName,
    platform: v4PlatformLabel(item.platformCode),
    platformCode: item.platformCode,
    registrationType: 'INDIVIDUAL',
    registrationTypeText: '个人报名',
    deadline: item.registrationEndTime || '',
    signupStartTime: item.registrationStartTime,
    matchStartTime: item.matchStartTime,
    matchEndTime: item.matchEndTime,
    quotaUsed: item.approvedPersonCount ?? 0,
    quotaTotal: item.maxPersonCount ?? 0,
    quotaFull: item.remainingPersonCount != null ? item.remainingPersonCount <= 0 : false,
    eligibilityStatus: canRegister ? 'ELIGIBLE' : 'APPLIED',
    actionText,
    actionType,
    disabledReason: item.cannotRegisterReason,
    canRegister,
    registrationStatus: item.myRegistrationStatus,
  }
}

export function mapV4PersonalRegistrationToApplication(item: V4MyPersonalRegistration): MyApplication {
  const status = (item.registrationStatus || '').toUpperCase()
  const pending = status === 'PENDING_REVIEW' || status === 'ASSIGNED'
  let actionText = '查看详情'
  if (status === 'DRAFT' || status === 'REJECTED' || status === 'WITHDRAWN') {
    actionText = status === 'DRAFT' ? '继续编辑' : '修改重提'
  }
  return {
    applicationId: item.personalRegistrationId,
    divisionId: item.divisionId || '',
    tournamentName: item.seasonTitle || '赛事',
    platformGroupLabel: item.divisionName,
    platform: v4PlatformLabel(item.platformCode),
    applicationType: 'INDIVIDUAL',
    applicationTypeText: '个人报名',
    status,
    statusText: v4PersonalRegistrationStatusLabel(status),
    pending,
    actionText,
    actionType: 'VIEW_APPLICATION_STATUS',
    submitTime: formatDateTimeShort(item.submitTime),
    statusHint: status === 'ASSIGNED'
      ? '已分配临时队伍，请等待队长完成组队'
      : status === 'APPROVED'
        ? '审核已通过，将由运营线下抽签组队'
        : item.rejectReason || undefined,
  }
}

export function mapV4DivisionToAvailableEvent(item: V4AvailableDivision): AvailableEvent {
  const canRegister = Boolean(item.canRegister)
  const status = (item.myRegistrationStatus || '').toUpperCase()
  let actionType: AvailableEvent['actionType'] = 'DISABLED'
  let actionText = '不可报名'
  if (canRegister) {
    actionType = 'TEAM_REGISTRATION'
    actionText = '去报名'
  } else if (status === 'DRAFT' || status === 'REJECTED' || status === 'WITHDRAWN') {
    actionType = 'MANAGE_REGISTRATION'
    actionText = status === 'DRAFT' ? '继续编辑' : '修改重提'
  } else if (status === 'PENDING_REVIEW') {
    actionType = 'VIEW_REGISTRATION'
    actionText = '查看报名'
  } else if (status === 'APPROVED') {
    actionType = 'GO_JOINED'
    actionText = '进入赛事'
  }

  return {
    divisionId: item.divisionId,
    seasonId: item.seasonId,
    tournamentName: item.seriesName || item.seasonTitle || '赛事',
    seasonName: item.seasonTitle,
    platform: v4PlatformLabel(item.platformCode),
    platformCode: item.platformCode,
    registrationType: 'TEAM',
    registrationTypeText: '球队报名',
    deadline: item.registrationEndTime || '',
    signupStartTime: item.registrationStartTime,
    matchStartTime: item.matchStartTime,
    matchEndTime: item.matchEndTime,
    quotaUsed: item.approvedTeamCount ?? 0,
    quotaTotal: item.maxTeamCount ?? 0,
    quotaFull: item.remainingTeamCount != null && item.remainingTeamCount <= 0,
    eligibilityStatus: canRegister ? 'ELIGIBLE' : 'NOT_VERIFIED',
    actionText,
    actionType,
    disabledReason: item.cannotRegisterReason,
    canRegister,
    teamName: item.grantTeamName,
    captainGrantStatusText: v4GrantStatusLabel(item.myGrantStatus),
    registrationStatus: item.myRegistrationStatus,
    eventStatus: '报名中',
    eventStatusCode: 'REGISTRATION_OPEN',
    isCaptain: Boolean(item.grantTeamId),
    hasCaptainGrant: item.myGrantStatus === 'ACTIVE',
  }
}

export function mapV4RegistrationToApplication(item: V4MyRegistration): MyApplication {
  const status = (item.registrationStatus || '').toUpperCase()
  const pending = status === 'PENDING_REVIEW'
  let actionText = '查看详情'
  let actionType: MyApplication['actionType'] = 'VIEW_APPLICATION_STATUS'
  let showAction = true

  if (status === 'DRAFT') {
    actionText = '继续编辑'
    actionType = 'VIEW_TEAM_APPLICATION'
  } else if (status === 'REJECTED') {
    actionText = '修改后重提'
    actionType = 'RESUBMIT'
  } else if (status === 'WITHDRAWN') {
    actionText = '重新提交'
    actionType = 'RESUBMIT'
  } else if (status === 'APPROVED') {
    actionText = '进入赛事'
    actionType = 'VIEW_APPLICATION_STATUS'
  } else if (status === 'PENDING_REVIEW') {
    actionText = '审核中'
    showAction = false
  } else if (status === 'CANCELLED') {
    showAction = false
  }

  const statusHint = status === 'PENDING_REVIEW'
    ? '审核中不占名额，请耐心等待审核结果'
    : item.rejectReason
      ? `驳回原因：${item.rejectReason}`
      : undefined

  return {
    applicationId: item.registrationId,
    divisionId: item.divisionId || item.seasonId || '',
    tournamentName: item.seasonTitle || '赛事',
    platform: v4PlatformLabel(item.platformCode),
    applicationType: 'TEAM',
    applicationTypeText: '球队报名',
    status,
    statusText: v4RegistrationStatusLabel(status),
    statusHint,
    submitTime: formatDateTimeShort(item.submitTime),
    actionText,
    actionType,
    pending,
    showAction,
    platformGroupLabel: item.divisionName,
    confirmedCount: item.lineupCount,
  }
}

export function mapV4EventToParticipating(item: V4MyEvent): ParticipatingEvent {
  const isCaptain = item.roleInTeam === 'CAPTAIN'
  const pendingCount = item.pendingSubmissionCount ?? 0
  return {
    relationId: item.eventTeamId,
    eventTeamId: item.eventTeamId,
    divisionId: item.divisionId,
    tournamentName: item.seriesName || item.seasonTitle || '赛事',
    platform: `${item.seasonTitle || ''} / ${v4PlatformLabel(item.platformCode)} / ${item.divisionName || ''}`.replace(/^ \/ | \/ $/g, ''),
    participationType: 'TEAM',
    participationTypeText: '球队参赛',
    identityType: item.roleInTeam || 'PLAYER',
    identityText: isCaptain ? '队长' : '队员',
    status: item.eventTeamStatus || '',
    statusText: item.divisionStatus || '进行中',
    pendingTaskCount: pendingCount,
    pendingTaskText: pendingCount > 0 ? `${pendingCount} 场比赛数据待提交` : undefined,
    actionText: '查看赛程',
    actionType: 'VIEW_EVENT_RELATION',
    actionRoute: `/account/my-events?tab=schedule&divisionId=${item.divisionId}`,
    isCaptain,
    rosterLocked: true,
    showMatchDataEntry: pendingCount > 0 || (item.matchCount ?? 0) > 0,
    matchDataActionText: pendingCount > 0 ? '提交比赛数据' : '查看比赛数据',
    matchDataActionRoute: `/account/my-events?tab=schedule&divisionId=${item.divisionId}${pendingCount > 0 ? '&onlyPendingSubmit=1' : ''}`,
    matchDataActionType: pendingCount > 0 ? 'SUBMIT_MATCH_DATA' : 'VIEW_MATCH_DATA_LIST',
    matchDataPrimary: pendingCount > 0,
  }
}
