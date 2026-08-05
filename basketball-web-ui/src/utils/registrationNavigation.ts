import type { Router } from 'vue-router'
import type { AvailableEvent, MyApplication } from '@/types/accountEvents'

export function goToRegistrationDetail(router: Router, registrationId: string) {
  router.push({
    path: '/account/team',
    query: {
      tab: 'registration',
      registrationId,
    },
  })
}

/** 进入已有报名记录的填写/修改流程（携带 registrationId，避免空赛季列表） */
export function goToRegistrationEditFlow(
  router: Router,
  params: {
    registrationId: string
    seasonId?: string
    tournamentId?: string
    platformCode?: string
  },
) {
  const seasonKey = params.seasonId || params.tournamentId
  if (!seasonKey) {
    goToRegistrationDetail(router, params.registrationId)
    return
  }
  router.push({
    path: `/account/my-events/${seasonKey}/team-registration`,
    query: {
      registrationId: params.registrationId,
      ...(params.platformCode ? { platformCode: params.platformCode } : {}),
    },
  })
}

/** 已通过报名 → 本届正式赛事球队管理 */
export function goToEventTeamManagement(
  router: Router,
  params: { seasonId?: string; tournamentId?: string },
) {
  const seasonKey = params.seasonId || params.tournamentId
  if (!seasonKey) return
  router.push(`/account/my-tournaments/${seasonKey}/team`)
}

export function goToNewTeamRegistration(
  router: Router,
  event: Pick<AvailableEvent, 'seasonId' | 'tournamentId' | 'platformCode'>,
  platformCode?: string,
) {
  const seasonKey = event.seasonId || event.tournamentId
  if (!seasonKey) return
  const resolvedPlatform = platformCode || event.platformCode
  router.push({
    path: `/account/my-events/${seasonKey}/team-registration`,
    query: {
      ...(resolvedPlatform ? { platformCode: resolvedPlatform } : {}),
      ...(event.seasonId ? { seasonId: event.seasonId } : {}),
    },
  })
}

export function goToEventRoster(
  router: Router,
  event: Pick<AvailableEvent, 'seasonId' | 'tournamentId'>,
) {
  const seasonKey = event.seasonId || event.tournamentId
  if (!seasonKey) return
  router.push(`/account/my-tournaments/${seasonKey}/team/roster`)
}

/** 可报名赛事列表中的操作按钮跳转 */
export function navigateAvailableRegistrationAction(
  router: Router,
  event: AvailableEvent,
  actionType?: AvailableEvent['actionType'],
) {
  const action = actionType || event.actionType
  const registrationId = event.registrationId

  if (action === 'VIEW_EVENT') {
    router.push(`/events/${event.tournamentId || event.seasonId}`)
    return
  }

  if (action === 'VIEW_ROSTER') {
    goToEventRoster(router, event)
    return
  }

  if (action === 'VIEW_REGISTRATION') {
    if (registrationId) {
      goToRegistrationDetail(router, registrationId)
      return
    }
    router.replace('/account/my-events?tab=applications')
    return
  }

  if (
    action === 'MANAGE_REGISTRATION'
    || action === 'RE_REGISTER'
    || action === 'TEAM_REGISTRATION'
  ) {
    if (registrationId) {
      goToRegistrationEditFlow(router, {
        registrationId,
        seasonId: event.seasonId,
        tournamentId: event.tournamentId,
        platformCode: event.platformCode,
      })
      return
    }
    goToNewTeamRegistration(router, event, event.platformCode)
    return
  }
}

/** 我的报名列表中的操作按钮跳转 */
export function navigateApplicationAction(router: Router, application: MyApplication) {
  if (application.applicationType === 'INDIVIDUAL') {
    return false
  }

  if (application.applicationId) {
    if (
      application.actionType === 'VIEW_APPLICATION_STATUS'
      || application.actionType === 'VIEW_TEAM_APPLICATION'
    ) {
      goToRegistrationDetail(router, application.applicationId)
      return true
    }
  }

  if (application.actionType === 'VIEW_TEAM_APPLICATION' && application.tournamentId) {
    goToNewTeamRegistration(router, {
      seasonId: application.tournamentId,
      tournamentId: application.tournamentId,
    })
    return true
  }

  return false
}
