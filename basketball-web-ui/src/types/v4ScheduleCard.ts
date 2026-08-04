import type { RouteLocationRaw } from 'vue-router'
import type { V4MyScheduleItem } from '@/api/types/v4PlayerEvents'

export type ScheduleSubmissionTone = 'pending' | 'review' | 'approved' | 'rejected' | 'cancelled' | 'abnormal' | 'neutral'

export type ScheduleCardAccent =
  | 'default'
  | 'upcoming'
  | 'finished'
  | 'my-pending-submit'
  | 'my-pending-review'
  | 'my-rejected'
  | 'waiting-opponent'
  | 'confirmed'
  | 'abnormal'

export interface ScheduleSubmissionSideView {
  sideLabel: string
  statusLabel: string
  tone: ScheduleSubmissionTone
  detail?: string
}

export interface ScheduleMatchActionView {
  label: string
  kind: 'primary' | 'secondary' | 'text'
  route: RouteLocationRaw
}

export interface ScheduleMatchActionsView {
  primary?: ScheduleMatchActionView
  secondary?: ScheduleMatchActionView
  hint?: string
  readOnlyLabel?: string
}

export interface ScheduleVersusTeamView {
  eventTeamId?: string
  name: string
  shortName: string
  logoUrl: string
  isMine: boolean
  isWinner?: boolean
}

export interface ScheduleVersusCenterView {
  mode: 'upcoming' | 'score' | 'status' | 'playing'
  dateText?: string
  timeText?: string
  homeScore?: number
  awayScore?: number
  statusText?: string
  countdownHint?: string
}

export interface ScheduleEventHeaderView {
  seasonTitle: string
  divisionLine: string
  roundLine: string
  matchStatusLabel: string
  matchStatusTone: 'default' | 'finished' | 'pending' | 'danger' | 'warning'
}

export interface ScheduleMatchCardViewModel {
  key: string
  accent: ScheduleCardAccent
  isNextMatch?: boolean
  header: ScheduleEventHeaderView
  home: ScheduleVersusTeamView
  away: ScheduleVersusTeamView
  center: ScheduleVersusCenterView
  mySubmission: ScheduleSubmissionSideView
  opponentSubmission: ScheduleSubmissionSideView
  actions: ScheduleMatchActionsView
  matchNo?: string
  raw: V4MyScheduleItem
}

export interface ScheduleListSection {
  key: string
  title: string
  items: ScheduleMatchCardViewModel[]
}
