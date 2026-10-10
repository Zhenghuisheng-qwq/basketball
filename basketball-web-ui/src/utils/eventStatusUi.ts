import { translate } from '@/i18n'
import type { AvailableEvent } from '@/types/accountEvents'

export type EventStatusTagVariant =
  | 'not-started'
  | 'open'
  | 'closed'
  | 'match-not-started'
  | 'in-progress'
  | 'ended'
  | 'cancelled'
  | 'default'

export interface EventStatusTagView {
  variant: EventStatusTagVariant
  label: string
}

const STATUS_BY_CODE: Record<string, { variant: EventStatusTagVariant; labelKey: string }> = {
  NOT_STARTED: { variant: 'not-started', labelKey: 'accountEvents.common.registrationNotStarted' },
  OPEN: { variant: 'open', labelKey: 'accountEvents.common.registrationOpen' },
  CLOSED: { variant: 'closed', labelKey: 'accountEvents.common.registrationClosed' },
  IN_PROGRESS: { variant: 'in-progress', labelKey: 'accountEvents.common.inProgress' },
  ENDED: { variant: 'ended', labelKey: 'accountEvents.common.finished' },
  FINISHED: { variant: 'ended', labelKey: 'accountEvents.common.finished' },
  CANCELLED: { variant: 'cancelled', labelKey: 'match.cancelled' },
}

export function resolveEventStatusTag(event: AvailableEvent): EventStatusTagView {
  const code = (event.eventStatusCode || '').toUpperCase()
  const known = STATUS_BY_CODE[code]
  if (known) return { variant: known.variant, label: translate(known.labelKey) }
  return { variant: 'default', label: event.eventStatus || '-' }
}
