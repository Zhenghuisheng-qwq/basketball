import type { V4PublicStageItem } from '@/api/types/v4Public'
import { translate } from '@/i18n'

export type StageRendererKind = 'ROUND_ROBIN' | 'GROUP_STAGE' | 'SINGLE_ELIMINATION' | 'PLAYOFF'

export type StageLike = Pick<
  V4PublicStageItem,
  | 'stageId'
  | 'stageCode'
  | 'stageName'
  | 'stageType'
  | 'organizationMode'
  | 'competitionFormat'
  | 'executionStatus'
  | 'currentStage'
>

function norm(value?: string | null): string {
  return String(value || '').trim().toUpperCase()
}

export function resolveStageRendererKind(stage?: StageLike | null): StageRendererKind {
  if (!stage) return 'ROUND_ROBIN'

  const org = norm(stage.organizationMode)
  const format = norm(stage.competitionFormat)
  const type = norm(stage.stageType)
  const code = norm(stage.stageCode)

  if (org === 'GROUPED' || type === 'GROUP' || code === 'GROUP') {
    return 'GROUP_STAGE'
  }

  const elimination =
    format.includes('ELIM')
    || type === 'KNOCKOUT'
    || type === 'SEMI_FINAL'
    || type === 'FINAL'
    || type.includes('PLAYOFF')
    || code === 'KNOCKOUT'
    || code.startsWith('PLAYOFF')

  if (elimination) {
    if (code === 'PLAYOFF' || code.startsWith('PLAYOFF') || type.includes('PLAYOFF')) {
      return 'PLAYOFF'
    }
    return 'SINGLE_ELIMINATION'
  }

  return 'ROUND_ROBIN'
}

export function isBracketStage(stage?: StageLike | null): boolean {
  const kind = resolveStageRendererKind(stage)
  return kind === 'PLAYOFF' || kind === 'SINGLE_ELIMINATION'
}

export function isGroupedStage(stage?: StageLike | null): boolean {
  return resolveStageRendererKind(stage) === 'GROUP_STAGE'
}

export function stageSelectorLabel(stage?: StageLike | null): string {
  const named = String(stage?.stageName || '').trim()
  if (named) return named
  switch (resolveStageRendererKind(stage)) {
    case 'GROUP_STAGE':
      return translate('eventsPage.stage.group')
    case 'SINGLE_ELIMINATION':
      return translate('eventsPage.stage.elimination')
    case 'PLAYOFF':
      return translate('eventsPage.stage.playoff')
    default:
      return translate('events.regularSeason')
  }
}

export function stageHeading(stage?: StageLike | null): { title: string; titleEn: string } {
  switch (resolveStageRendererKind(stage)) {
    case 'GROUP_STAGE':
      return { title: translate('eventsPage.stage.groupTitle'), titleEn: 'GROUP STAGE' }
    case 'SINGLE_ELIMINATION':
      return { title: translate('eventsPage.stage.eliminationTitle'), titleEn: 'ELIMINATION BRACKET' }
    case 'PLAYOFF':
      return { title: translate('eventsPage.stage.playoffTitle'), titleEn: 'PLAYOFF BRACKET' }
    default:
      return { title: translate('events.regularSeason'), titleEn: 'REGULAR SEASON' }
  }
}

export function stageEmptyText(stage?: StageLike | null): string {
  switch (resolveStageRendererKind(stage)) {
    case 'GROUP_STAGE':
      return translate('eventsPage.stage.groupEmpty')
    case 'SINGLE_ELIMINATION':
      return translate('eventsPage.stage.eliminationEmpty')
    case 'PLAYOFF':
      return translate('eventsPage.stage.playoffEmpty')
    default:
      return translate('eventsPage.stage.regularEmpty')
  }
}

const LIVE_STATUS = new Set(['RUNNING', 'RESULT_PENDING_CONFIRM'])
const STARTED_STATUS = new Set(['RUNNING', 'RESULT_PENDING_CONFIRM', 'COMPLETED', 'SCHEDULE_GENERATED'])

export function pickDefaultStage<T extends StageLike>(stages: T[]): T | null {
  if (!stages.length) return null

  const current = stages.find((item) => item.currentStage)
  if (current) return current

  const live = stages.find((item) => LIVE_STATUS.has(norm(item.executionStatus)))
  if (live) return live

  const started = [...stages].reverse().find((item) => STARTED_STATUS.has(norm(item.executionStatus)))
  if (started) return started

  return stages[0]
}
