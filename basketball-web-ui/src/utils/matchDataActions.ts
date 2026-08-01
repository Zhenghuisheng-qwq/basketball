import type { PlayerMatchTaskItem } from '@/api/matchData'
import { MATCH_STATUS_FINISHED, MATCH_DATA_STATUS, SUBMISSION_STATUS_PENDING } from '@/utils/participationMappers'
import { resolveMatchDataTabMessage } from '@/utils/matchDataTabHelpers'
import {
  buildMatchDataDisputeRoute,
  buildMatchDataExceptionRoute,
  buildMatchDataStatusRoute,
} from '@/utils/participationMappers'

export interface MatchDataAction {
  key: string
  label: string
  path?: string
  primary?: boolean
  disabled?: boolean
  hint?: string
}

export function resolveMatchDataCardMessage(match: PlayerMatchTaskItem | null | undefined): string {
  return resolveMatchDataTabMessage(match)
}

/** 仅返回需要跳转外链的操作；录入/提交在本 Tab 内完成 */
export function resolveMatchDataActions(match: PlayerMatchTaskItem | null | undefined): MatchDataAction[] {
  if (!match?.matchId) return []

  const seasonId = match.seasonId || ''
  const matchId = match.matchId
  const isCaptain = Boolean(match.isCaptain)
  const actions: MatchDataAction[] = []

  if (match.canViewData) {
    actions.push({
      key: 'view-data',
      label: '查看审核详情',
      path: buildMatchDataStatusRoute(seasonId, matchId),
    })
  } else if (isCaptain && match.canViewStatus) {
    actions.push({
      key: 'view-status',
      label: '查看审核状态',
      path: buildMatchDataStatusRoute(seasonId, matchId),
    })
  }

  if (isCaptain && match.canDispute) {
    actions.push({
      key: 'dispute',
      label: '发起数据争议',
      path: buildMatchDataDisputeRoute(seasonId, matchId),
    })
  }

  if (isCaptain && match.canSubmitException) {
    actions.push({
      key: 'exception',
      label: '异常比赛申请',
      path: buildMatchDataExceptionRoute(seasonId, matchId),
    })
  }

  if (!isCaptain && !match.canViewData) {
    if (match.submissionStatus === SUBMISSION_STATUS_PENDING || match.dataStatus === MATCH_DATA_STATUS.SUBMITTED) {
      actions.push({
        key: 'member-pending',
        label: '审核中',
        disabled: true,
      })
    }
  }

  if (isCaptain && match.matchStatus !== MATCH_STATUS_FINISHED && !match.canViewData) {
    actions.push({
      key: 'submit-disabled',
      label: '提交比赛数据',
      disabled: true,
      hint: '比赛结束后可提交数据',
    })
  }

  return actions
}
