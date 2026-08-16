import type {
  V4MatchSubmissionEligiblePlayer,
  V4MatchSubmissionPlayerStat,
  V4MatchSubmissionTeamStat,
} from '@/api/types/v4MatchSubmission'

export interface V4MatchSubmissionPeriodScore {
  periodNo: number
  homeScore?: number
  awayScore?: number
}

export interface V4SeriesSubmissionGame {
  matchId: string
  seriesGameNo?: number
  matchNo?: string
  submissionId?: string
  submissionStatus?: string
  draftComplete?: number
  homeScore?: number
  awayScore?: number
  submitRemark?: string
  required?: boolean
  skipped?: boolean
  homeTeamStat?: V4MatchSubmissionTeamStat
  awayTeamStat?: V4MatchSubmissionTeamStat
  homePlayerStats?: V4MatchSubmissionPlayerStat[]
  awayPlayerStats?: V4MatchSubmissionPlayerStat[]
  periodScores?: V4MatchSubmissionPeriodScore[]
  materialFileIds?: string[]
}

export interface V4SeriesSubmissionWorkspace {
  matchSeriesId: string
  seriesCode?: string
  bestOf?: number
  roundNo?: number
  seasonTitle?: string
  divisionName?: string
  stageName?: string
  homeEventTeamId?: string
  homeTeamName?: string
  awayEventTeamId?: string
  awayTeamName?: string
  seriesSubmissionId?: string
  versionNo?: number
  status?: string
  declaredHomeWinCount?: number
  declaredAwayWinCount?: number
  actualGameCount?: number
  submitEventTeamId?: string
  submitTeamName?: string
  submitTime?: string
  rejectReason?: string
  auditRemark?: string
  completedGameCount?: number
  canEdit?: boolean
  canSubmitSeries?: boolean
  homeEligiblePlayers?: V4MatchSubmissionEligiblePlayer[]
  awayEligiblePlayers?: V4MatchSubmissionEligiblePlayer[]
  games?: V4SeriesSubmissionGame[]
}

export interface V4SeriesSubmissionDeclareReq {
  matchSeriesId: string
  declaredHomeWinCount: number
  declaredAwayWinCount: number
}

export interface V4SeriesSubmissionSaveGameReq {
  matchSeriesId: string
  matchId: string
  submitEventTeamId?: string
  declaredHomeWinCount?: number
  declaredAwayWinCount?: number
  homeScore?: number
  awayScore?: number
  homePlayerStats?: V4MatchSubmissionPlayerStat[]
  awayPlayerStats?: V4MatchSubmissionPlayerStat[]
  periodScores?: V4MatchSubmissionPeriodScore[]
  materialFileIds?: string[]
  submitRemark?: string
  draftComplete?: boolean
}

export interface V4SeriesSubmissionIdReq {
  seriesSubmissionId?: string
  matchSeriesId?: string
}

export interface V4SeriesSubmissionResubmitReq {
  oldSeriesSubmissionId: string
  matchSeriesId?: string
  declaredHomeWinCount?: number
  declaredAwayWinCount?: number
}
