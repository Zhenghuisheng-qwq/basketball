import type {
  V4AvailableDivision,
  V4MyEvent,
  V4MyPersonalRegistration,
  V4MyRegistration,
  V4PersonalAvailableDivision,
} from '@/api/types/v4PlayerEvents'
import type { AvailableEvent, MyApplication, ParticipatingEvent } from '@/types/accountEvents'
import { translate } from '@/i18n'
import { formatDateTimeShort } from '@/utils/participationMappers'
import { businessDateTime } from '@/utils/businessDateTime'
import { v4DivisionStatusLabel, v4PlatformLabel } from '@/utils/v4Display'

export function v4RegistrationStatusLabel(status?: string | null, participationRole?: string | null): string {
  const code = (status || '').toUpperCase()
  const role = (participationRole || '').toUpperCase()
  if (code === 'APPROVED' && role === 'FORMAL') {
    return translate('accountEvents.common.formalEntry')
  }
  if (code === 'APPROVED' && role && role !== 'FORMAL') {
    return translate('accountEvents.common.approvedWaitingRoster')
  }
  switch (code) {
    case 'DRAFT':
      return translate('accountEvents.common.draft')
    case 'PENDING_REVIEW':
      return translate('accountEvents.common.pendingReview')
    case 'REJECTED':
      return translate('accountEvents.common.rejected')
    case 'WITHDRAWN':
      return translate('accountEvents.common.withdrawn')
    case 'APPROVED':
      return translate('accountEvents.common.approved')
    case 'CANCELLED':
      return translate('accountEvents.common.cancelled')
    default:
      return status || translate('accountEvents.common.unknown')
  }
}

export function v4SubmissionStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return translate('accountEvents.common.awaitingSubmit')
    case 'PENDING_REVIEW':
      return translate('accountEvents.common.pendingReview')
    case 'REJECTED':
      return translate('accountEvents.common.rejected')
    case 'APPROVED':
      return translate('accountEvents.common.approved')
    case 'SUPERSEDED':
      return translate('accountEvents.common.superseded')
    case 'CANCELLED':
      return translate('accountEvents.common.cancelled')
    default:
      return status ? translate('accountEvents.common.notSubmitted') : translate('accountEvents.common.notSubmitted')
  }
}

export function v4GrantStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE':
      return translate('accountEvents.common.granted')
    case 'REVOKED':
      return translate('accountEvents.common.revoked')
    case 'EXPIRED':
      return translate('accountEvents.common.expired')
    default:
      return status ? translate('accountEvents.common.noGrant') : translate('accountEvents.common.noGrant')
  }
}

export function v4PersonalRegistrationStatusLabel(status?: string | null): string {
  switch ((status || '').toUpperCase()) {
    case 'DRAFT':
      return translate('accountEvents.common.draft')
    case 'PENDING_REVIEW':
      return translate('accountEvents.common.awaitingReview')
    case 'APPROVED':
      return translate('accountEvents.common.approvedWaitingFormation')
    case 'REJECTED':
      return translate('accountEvents.common.rejected')
    case 'WITHDRAWN':
      return translate('accountEvents.common.withdrawn')
    case 'ASSIGNED':
      return translate('accountEvents.common.assignedTempTeam')
    case 'CONVERTED':
      return translate('accountEvents.common.lockedTempTeam')
    default:
      return status || translate('accountEvents.common.unknown')
  }
}

export function mapV4PersonalDivisionToAvailableEvent(item: V4PersonalAvailableDivision): AvailableEvent {
  const canRegister = Boolean(item.canRegister)
  const status = (item.myRegistrationStatus || '').toUpperCase()
  let actionType: AvailableEvent['actionType'] = 'DISABLED'
  let actionText = translate('accountEvents.common.cannotRegister')
  if (canRegister) {
    actionType = 'INDIVIDUAL_REGISTRATION'
    actionText = translate('accountEvents.common.personalRegistration')
  } else if (status === 'DRAFT' || status === 'REJECTED' || status === 'WITHDRAWN') {
    actionType = 'MANAGE_REGISTRATION'
    actionText = status === 'DRAFT' ? translate('accountEvents.common.continueEdit') : translate('accountEvents.common.reviseResubmit')
  } else if (status === 'PENDING_REVIEW') {
    actionType = 'VIEW_REGISTRATION'
    actionText = translate('accountEvents.common.viewRegistration')
  } else if (status === 'APPROVED' || status === 'ASSIGNED') {
    actionType = 'VIEW_REGISTRATION'
    actionText = translate('accountEvents.common.viewProgress')
  } else if (status === 'CONVERTED') {
    actionType = 'GO_JOINED'
    actionText = translate('accountEvents.common.enterEvent')
  }
  return {
    divisionId: item.divisionId,
    seasonId: item.seasonId,
    tournamentName: item.seriesName || item.seasonTitle || translate('accountEvents.common.event'),
    seasonName: item.seasonTitle,
    displayTitle: item.divisionName,
    platform: v4PlatformLabel(item.platformCode),
    platformCode: item.platformCode,
    registrationType: 'INDIVIDUAL',
    registrationTypeText: translate('accountEvents.common.personalRegistration'),
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
    disabledReason: item.canRegister
      ? undefined
      : (item.cannotRegisterReasonCode
        ? teamRegistrationEntryActionText(item.cannotRegisterReasonCode, item.cannotRegisterReason)
        : item.cannotRegisterReason),
    canRegister,
    registrationStatus: item.myRegistrationStatus,
  }
}

export function mapV4PersonalRegistrationToApplication(item: V4MyPersonalRegistration): MyApplication {
  const status = (item.registrationStatus || '').toUpperCase()
  const assigned = Boolean(item.assignedDrawTeamId)
  const locked = Boolean(item.formationLocked) || status === 'CONVERTED'
  const pending = status === 'PENDING_REVIEW' || status === 'ASSIGNED'
  let actionText = translate('accountEvents.common.viewDetail')
  if (status === 'DRAFT' || status === 'REJECTED' || status === 'WITHDRAWN') {
    actionText = status === 'DRAFT' ? translate('accountEvents.common.continueEdit') : translate('accountEvents.common.reviseResubmit')
  }
  let statusText = v4PersonalRegistrationStatusLabel(status)
  let statusHint = item.rejectReason || undefined
  if (locked) {
    statusText = translate('accountEvents.common.lockedTempTeam')
    statusHint = item.assignedTemporaryTeamName
      ? (item.assignedCaptainName
        ? translate('accountEvents.common.tempTeamLine', { name: item.assignedTemporaryTeamName, captain: item.assignedCaptainName })
        : translate('accountEvents.common.tempTeamNamed', { name: item.assignedTemporaryTeamName }))
      : translate('accountEvents.common.tempTeamReady')
  } else if (assigned || status === 'ASSIGNED') {
    statusText = translate('accountEvents.common.assignedTempTeam')
    statusHint = item.assignedTemporaryTeamName
      ? translate('accountEvents.common.assignedTempLine', { name: item.assignedTemporaryTeamName })
      : translate('accountEvents.common.assignedTempTeam')
  } else if (status === 'APPROVED') {
    statusText = translate('accountEvents.common.approvedWaitingFormation')
    statusHint = translate('accountEvents.common.approvedWaitingFormation')
  }
  return {
    applicationId: item.personalRegistrationId,
    divisionId: item.divisionId || '',
    tournamentName: item.seasonTitle || translate('accountEvents.common.event'),
    platformGroupLabel: item.divisionName,
    platform: v4PlatformLabel(item.platformCode),
    applicationType: 'INDIVIDUAL',
    applicationTypeText: translate('accountEvents.common.personalRegistration'),
    status,
    statusText,
    pending,
    actionText,
    actionType: 'VIEW_APPLICATION_STATUS',
    submitTime: formatDateTimeShort(item.submitTime),
    statusHint,
  }
}

function teamRegistrationEntryActionText(reasonCode?: string, reasonMessage?: string) {
  const code = (reasonCode || '').toUpperCase()
  switch (code) {
    case 'NO_CAPTAIN_GRANT':
      return translate('accountEvents.common.noCaptainGrant')
    case 'NO_TEAM':
      return translate('accountEvents.common.noTeamYet')
    case 'TEAM_NOT_CERTIFIED':
      return translate('accountEvents.common.teamNotCertified')
    case 'TEAM_DIVISION_MISMATCH':
      return translate('accountEvents.common.teamMismatch')
    case 'TEAM_ARCHIVED':
      return translate('accountEvents.common.teamArchived')
    case 'REGISTRATION_CLOSED':
      return translate('accountEvents.common.registrationClosed')
    case 'NOT_STARTED':
    case 'REGISTRATION_NOT_STARTED':
      return translate('accountEvents.common.registrationNotStarted')
    case 'ALREADY_PENDING':
    case 'ALREADY_APPROVED':
      return translate('accountEvents.common.viewRegistration')
    case 'QUOTA_FULL':
    case 'PERSONAL_QUOTA_FULL':
      return translate('accountEvents.common.quotaFull')
    case 'PLAYER_NOT_ELIGIBLE':
    case 'PLAYER_NOT_CERTIFIED':
      return translate('errors.PLAYER_NOT_CERTIFIED')
    case 'PERSONAL_REGISTRATION_EXISTS':
    case 'PERSONAL_REVIEW_PENDING':
    case 'APPLICATION_PENDING':
      return translate('errors.APPLICATION_PENDING')
    case 'ALREADY_TEAM_REGISTERED':
      return translate('errors.ALREADY_TEAM_REGISTERED')
    case 'PERSONAL_ALREADY_DONE':
      return translate('accountEvents.common.approved')
    case 'REGISTRATION_NOT_OPEN':
      return translate('errors.REGISTRATION_NOT_OPEN')
    default:
      return reasonMessage ? translate('accountEvents.common.cannotRegister') : translate('accountEvents.common.cannotRegister')
  }
}

export function mapV4DivisionToAvailableEvent(item: V4AvailableDivision): AvailableEvent {
  const canRegister = Boolean(item.canRegister)
  const canView = Boolean(item.canViewRegistration) || Boolean(item.myRegistrationId || item.myRegistrationStatus)
  const status = (item.myRegistrationStatus || '').toUpperCase()
  const windowCode = (item.registrationWindowStatus || '').toUpperCase()
  const windowLabel = windowCode === 'NOT_STARTED'
    ? translate('accountEvents.common.registrationNotStarted')
    : windowCode === 'CLOSED'
      ? translate('accountEvents.common.registrationClosed')
      : windowCode === 'OPEN'
        ? translate('accountEvents.common.registrationOpen')
        : (item.registrationWindowStatusLabel || translate('accountEvents.common.registrationOpen'))

  let actionType: AvailableEvent['actionType'] = 'DISABLED'
  let actionText = translate('accountEvents.common.cannotRegister')
  if (canRegister) {
    if (status === 'DRAFT') {
      actionType = 'MANAGE_REGISTRATION'
      actionText = translate('accountEvents.common.continueEdit')
    } else if (status === 'REJECTED' || status === 'WITHDRAWN') {
      actionType = 'MANAGE_REGISTRATION'
      actionText = translate('accountEvents.common.reviseResubmit')
    } else {
      actionType = 'TEAM_REGISTRATION'
      actionText = translate('accountEvents.common.goRegister')
    }
  } else if (status === 'APPROVED') {
    actionType = 'GO_JOINED'
    actionText = translate('accountEvents.common.enterEvent')
  } else if (canView || status === 'PENDING_REVIEW' || status === 'DRAFT' || status === 'REJECTED' || status === 'WITHDRAWN') {
    actionType = 'VIEW_REGISTRATION'
    actionText = translate('accountEvents.common.viewRegistration')
  } else {
    actionText = teamRegistrationEntryActionText(item.cannotRegisterReasonCode, item.cannotRegisterReason)
  }

  const reasonFromCode = item.cannotRegisterReasonCode
    ? teamRegistrationEntryActionText(item.cannotRegisterReasonCode, item.cannotRegisterReason)
    : undefined
  const disabledReason = canRegister
    ? undefined
    : (reasonFromCode
      || item.cannotRegisterReason
      || (windowCode === 'CLOSED' && item.registrationEndTime
        ? translate('accountEvents.common.closedAt', { time: formatDateTimeShort(item.registrationEndTime) })
        : undefined))

  return {
    divisionId: String(item.divisionId ?? ''),
    seasonId: item.seasonId != null ? String(item.seasonId) : undefined,
    tournamentName: item.seriesName || item.seasonTitle || translate('accountEvents.common.event'),
    seasonName: item.seasonTitle,
    platform: v4PlatformLabel(item.platformCode),
    platformCode: item.platformCode,
    registrationType: 'TEAM',
    registrationTypeText: translate('accountEvents.common.teamRegistration'),
    deadline: item.registrationEndTime || '',
    signupStartTime: item.registrationStartTime,
    matchStartTime: item.matchStartTime,
    matchEndTime: item.matchEndTime,
    quotaUsed: item.approvedTeamCount ?? 0,
    quotaTotal: item.maxTeamCount ?? 0,
    quotaFull: item.remainingTeamCount != null && item.remainingTeamCount <= 0,
    eligibilityStatus: canRegister ? 'ELIGIBLE' : (status === 'APPROVED' ? 'JOINED' : 'NOT_VERIFIED'),
    actionText,
    actionType,
    disabledReason,
    canRegister,
    teamName: item.grantTeamName,
    teamRegistrationStatusText: status ? v4RegistrationStatusLabel(status, item.participationRole) : translate('accountEvents.common.notRegistered'),
    captainGrantStatusText: v4GrantStatusLabel(item.myGrantStatus),
    registrationStatus: item.myRegistrationStatus,
    registrationId: item.myRegistrationId != null ? String(item.myRegistrationId) : undefined,
    eventStatus: windowLabel,
    eventStatusCode: windowCode || undefined,
    isCaptain: Boolean(item.grantTeamId),
    hasCaptainGrant: item.myGrantStatus === 'ACTIVE',
  }
}

export function mapV4RegistrationToApplication(item: V4MyRegistration): MyApplication {
  const status = (item.registrationStatus || '').toUpperCase()
  const pending = status === 'PENDING_REVIEW'
  let actionText = translate('accountEvents.common.viewDetail')
  let actionType: MyApplication['actionType'] = 'VIEW_APPLICATION_STATUS'
  let showAction = true

  if (status === 'DRAFT') {
    actionText = translate('accountEvents.common.continueEdit')
    actionType = 'VIEW_TEAM_APPLICATION'
  } else if (status === 'REJECTED') {
    actionText = translate('accountEvents.common.reviseAndResubmit')
    actionType = 'RESUBMIT'
  } else if (status === 'WITHDRAWN') {
    actionText = translate('accountEvents.common.submitAgain')
    actionType = 'RESUBMIT'
  } else if (status === 'APPROVED') {
    actionText = item.participationRole === 'FORMAL' ? translate('accountEvents.common.enterEvent') : translate('accountEvents.common.viewDetail')
    actionType = 'VIEW_APPLICATION_STATUS'
  } else if (status === 'PENDING_REVIEW') {
    actionText = translate('accountEvents.common.pendingReview')
    showAction = false
  } else if (status === 'CANCELLED') {
    showAction = false
  }

  const statusHint = status === 'PENDING_REVIEW'
    ? translate('accountEvents.common.reviewNoQuota')
    : item.rejectReason
      ? translate('accountEvents.common.rejectReason', { reason: item.rejectReason })
      : undefined

  return {
    applicationId: item.registrationId,
    divisionId: item.divisionId || item.seasonId || '',
    tournamentName: item.seasonTitle || translate('accountEvents.common.event'),
    platform: v4PlatformLabel(item.platformCode),
    applicationType: 'TEAM',
    applicationTypeText: translate('accountEvents.common.teamRegistration'),
    status,
    statusText: v4RegistrationStatusLabel(status, item.participationRole),
    statusHint,
    submitTime: formatDateTimeShort(item.submitTime),
    actionText,
    actionType,
    pending,
    showAction,
    platformGroupLabel: item.divisionName,
    confirmedCount: item.currentEligibleCount ?? item.lineupCount,
  }
}

function divisionStatusText(status?: string | null): string {
  const label = v4DivisionStatusLabel(status)
  return label && label !== '--' ? label : translate('accountEvents.common.inProgress')
}

function wallClockText(value?: string | null): string {
  const parsed = businessDateTime(value)
  if (!parsed) return String(value || '')
  return parsed.time && parsed.time !== '--' ? `${parsed.date} ${parsed.time}` : parsed.date
}

export function mapV4EventToParticipating(item: V4MyEvent): ParticipatingEvent {
  const isCaptain = item.roleInTeam === 'CAPTAIN'
  const pendingCount = item.pendingSubmissionCount ?? 0
  const stageLabel = item.legacyEvent
    ? translate('accountEvents.common.historical')
    : [item.currentStageName, item.currentGroupName].filter(Boolean).join(' · ') || undefined
  const rankLabel = item.standingRank != null
    ? (item.standingPoints != null
      ? translate('accountEvents.common.rankPoints', { rank: item.standingRank, points: item.standingPoints })
      : translate('accountEvents.common.rank', { rank: item.standingRank }))
    : undefined
  const qualLabel = item.qualificationStatusName && item.qualificationStatusName !== item.qualificationStatus
    ? item.qualificationStatusName
    : item.qualificationStatus === 'CONFIRMED'
      ? translate('accountEvents.common.qualified')
      : item.qualificationStatus === 'PREVIEW'
        ? translate('accountEvents.common.qualificationPreview')
        : undefined
  return {
    relationId: item.eventTeamId,
    eventTeamId: item.eventTeamId,
    divisionId: item.divisionId,
    tournamentName: item.seriesName || item.seasonTitle || translate('accountEvents.common.event'),
    platform: `${item.seasonTitle || ''} / ${item.platformName || item.platformCode || ''} / ${item.divisionName || ''}`.replace(/^ \/ | \/ $/g, ''),
    participationType: 'TEAM',
    participationTypeText: translate('accountEvents.common.teamEntry'),
    identityType: item.roleInTeam || 'PLAYER',
    identityText: isCaptain ? translate('accountEvents.common.captainRole') : translate('accountEvents.common.memberRole'),
    status: item.eventTeamStatus || '',
    statusText: item.legacyEvent
      ? translate('accountEvents.common.historical')
      : divisionStatusText(item.divisionStatus),
    pendingTaskCount: pendingCount,
    pendingTaskText: pendingCount > 0 ? translate('accountEvents.common.pendingMatchData', { count: pendingCount }) : undefined,
    stageSummary: stageLabel,
    rankSummary: rankLabel,
    qualificationSummary: qualLabel,
    nextMatchSummary: item.nextMatchTime ? translate('accountEvents.common.nextMatch', { time: wallClockText(item.nextMatchTime) }) : undefined,
    latestMatchSummary: item.latestMatchResult ? translate('accountEvents.common.latestMatch', { result: item.latestMatchResult }) : undefined,
    actionText: translate('accountEvents.common.viewSchedule'),
    actionType: 'VIEW_EVENT_RELATION',
    actionRoute: `/account/my-events?tab=schedule&divisionId=${item.divisionId}`,
    isCaptain,
    rosterLocked: true,
    showMatchDataEntry: pendingCount > 0 || (item.matchCount ?? 0) > 0,
    matchDataActionText: pendingCount > 0 ? translate('accountEvents.common.submitMatchData') : translate('accountEvents.common.viewMatchData'),
    matchDataActionRoute: `/account/my-events?tab=schedule&divisionId=${item.divisionId}${pendingCount > 0 ? '&onlyPendingSubmit=1' : ''}`,
    matchDataActionType: pendingCount > 0 ? 'SUBMIT_MATCH_DATA' : 'VIEW_MATCH_DATA_LIST',
    matchDataPrimary: pendingCount > 0,
  }
}
