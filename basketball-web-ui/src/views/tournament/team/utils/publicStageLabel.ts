import type { V4PublicStageItem } from '@/api/types/v4Public'
import { translate } from '@/i18n'
import type { TeamStage } from '@/types/team'

const LIVE_STATUS = new Set(['RUNNING', 'RESULT_PENDING_CONFIRM'])
const CLOSED_STATUS = new Set(['COMPLETED', 'CANCELLED'])

export function isLivePublicStage(stage?: V4PublicStageItem | null): boolean {
  if (!stage) return false
  const flag = stage.currentStage as unknown
  if (flag === true || flag === 1 || flag === '1' || flag === 'true') return true
  return LIVE_STATUS.has(String(stage.executionStatus || '').trim().toUpperCase())
}

export function publicStageDisplayName(stage: V4PublicStageItem): string {
  const name = stage.stageName?.trim()
  if (name) return name
  const type = `${stage.stageType || ''} ${stage.stageCode || ''} ${stage.competitionFormat || ''}`.toUpperCase()
  if (type.includes('GROUP')) return translate('publicPages.team.groupStage')
  if (type.includes('KNOCKOUT') || type.includes('PLAYOFF') || type.includes('ELIMINATION')) {
    return translate('publicPages.team.knockout')
  }
  if (type.includes('REGULAR') || type.includes('ROUND_ROBIN')) return translate('publicPages.team.regular')
  return stage.stageTypeName?.trim() || translate('publicPages.team.notStarted')
}

/** 当前阶段文案只来自当前生效规则快照。没有进行中的阶段时不虚构「常规赛」。 */
export function resolvePublicStageLabel(stages?: V4PublicStageItem[] | null): string {
  const list = stages ?? []
  if (!list.length) return translate('publicPages.team.stagePending')
  const live = list.find((stage) => isLivePublicStage(stage))
  if (live) return publicStageDisplayName(live)
  const closed = list.every((stage) => CLOSED_STATUS.has(String(stage.executionStatus || '').trim().toUpperCase()))
  return translate(closed ? 'publicPages.team.finished' : 'publicPages.team.notStarted')
}

export function resolveTeamStageKind(stages?: V4PublicStageItem[] | null): TeamStage {
  const live = (stages ?? []).find((stage) => isLivePublicStage(stage))
  if (!live) return 'regular'
  const type = `${live.stageType || ''} ${live.stageCode || ''} ${live.competitionFormat || ''}`.toUpperCase()
  if (type.includes('KNOCKOUT') || type.includes('PLAYOFF') || type.includes('ELIMINATION')) return 'playoffs'
  return 'regular'
}

export function isKnockoutStage(stageType?: string | null, competitionFormat?: string | null): boolean {
  const type = `${stageType || ''} ${competitionFormat || ''}`.toUpperCase()
  return type.includes('KNOCKOUT') || type.includes('PLAYOFF') || type.includes('ELIMINATION')
}
