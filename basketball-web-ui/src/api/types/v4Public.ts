export interface V4PublicDivisionBrief {
  divisionId: string
  platformCode: string
  divisionName: string
  divisionStatus: string
  maxTeamCount?: number
  lineupMinCount?: number
  lineupMaxCount?: number
}

export interface V4PublicSeriesItem {
  seriesId: string
  seriesCode: string
  seriesName: string
  seriesDesc?: string
  iconFileId?: string
  iconUrl?: string
  coverFileId?: string
  coverUrl?: string
  bannerFileId?: string
  bannerUrl?: string
  enabled?: number
  seasonCount?: number
}

export interface V4PublicSeasonItem {
  seasonId: string
  seriesId: string
  seriesCode: string
  seriesName: string
  seasonYear?: number
  seasonName?: string
  displayTitle: string
  description?: string
  coverFileId?: string
  coverUrl?: string
  bannerFileId?: string
  bannerUrl?: string
  registrationMode?: string
  formatType?: string
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
  seasonStatus?: string
  divisionCount?: number
  platformCodes?: string[]
  gameModes?: string[]
  registrationModes?: string[]
  registrationOpenDivisionCount?: number
  nearestRegistrationEndTime?: string
  divisions: V4PublicDivisionBrief[]
}

export interface V4PublicSeasonDetail extends V4PublicSeasonItem {
  defaultDivisionId?: string
  eventTeamCount?: number
  matchCount?: number
  finishedMatchCount?: number
}

export interface V4PublicDivisionDetail {
  divisionId: string
  seasonId: string
  seriesCode?: string
  seriesName?: string
  seasonTitle?: string
  seasonName?: string
  platformCode: string
  divisionName: string
  registrationMode?: string
  formatType?: string
  maxTeamCount?: number
  lineupMinCount?: number
  lineupMaxCount?: number
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
  divisionStatus?: string
  eventTeamCount?: number
  matchCount?: number
  finishedMatchCount?: number
}

export interface V4PublicEventTeamItem {
  eventTeamId: string
  sourceTeamId?: string
  teamName: string
  teamShortName?: string
  eventTeamNo?: string
  teamLogoFileId?: string
  teamLogoUrl?: string
  captainPlayerId?: string
  captainName?: string
  platformCode?: string
  eventTeamStatus?: string
  eventTeamStatusName?: string
  disqualified?: boolean
  temporaryTeam?: boolean
  divisionId?: string
  divisionName?: string
  seasonId?: string
  seasonName?: string
  seriesCode?: string
  seriesName?: string
  gameMode?: string
}

export interface V4PublicEventPlayerItem {
  eventPlayerId: string
  sourcePlayerId?: string
  playerName: string
  avatarFileId?: string
  avatarUrl?: string
  gameIdSnapshot?: string
  platformCode?: string
  position?: string
  jerseyNo?: string
  isCaptain?: number
  isStarter?: number
}

export interface PublicActiveDisciplineSummary {
  targetType?: string
  penaltyCode?: string
  penaltyLabel?: string
  tagLabel?: string
  description?: string
  status?: string
  statusLabel?: string
  effectiveStartTime?: string
  divisionId?: string
  divisionName?: string
  seasonId?: string
  seasonName?: string
  seriesName?: string
  publicReason?: string | null
}

export interface V4PublicEventTeamDetail extends V4PublicEventTeamItem {
  players: V4PublicEventPlayerItem[]
  activeDisciplineSummary?: PublicActiveDisciplineSummary[]
}

export interface V4PublicMatchItem {
  matchId: string
  seasonId: string
  divisionId: string
  roundNo?: number
  matchNo?: string
  matchTime?: string
  homeEventTeamId?: string
  homeTeamName?: string
  homeSourceType?: string
  homeSourceDisplay?: string
  homeTeamLogoFileId?: string
  homeTeamLogoUrl?: string
  awayEventTeamId?: string
  awayTeamName?: string
  awaySourceType?: string
  awaySourceDisplay?: string
  awayTeamLogoFileId?: string
  awayTeamLogoUrl?: string
  homeScore?: number
  awayScore?: number
  originalHomeScore?: number
  originalAwayScore?: number
  winnerEventTeamId?: string
  matchStatus?: string
  publishStatus?: string
  venueText?: string
  matchSeriesId?: string
  seriesGameNo?: number
  bestOf?: number
  seriesHomeWinCount?: number
  seriesAwayWinCount?: number
  seriesStatus?: string
  seriesStatusName?: string
  resultType?: string
  resultReason?: string
  homeDisqualified?: boolean
  awayDisqualified?: boolean
  homeResultLabel?: string
  awayResultLabel?: string
}

export interface V4PublicMatchTeamStatItem {
  eventTeamId: string
  teamName?: string
  isWinner?: number
  score?: number
  pointsFor?: number
  pointsAgainst?: number
  fieldGoalMade?: number | null
  fieldGoalAttempt?: number | null
  threePointMade?: number | null
  threePointAttempt?: number | null
  freeThrowMade?: number | null
  freeThrowAttempt?: number | null
  rebound?: number | null
  assist?: number | null
  steal?: number | null
  block?: number | null
  turnover?: number | null
  foul?: number | null
}

export interface V4PublicMatchPlayerStatItem {
  eventPlayerId: string
  sourcePlayerId?: string
  playerName?: string
  eventTeamId?: string
  teamName?: string
  points?: number | null
  rebound?: number | null
  assist?: number | null
  steal?: number | null
  block?: number | null
  turnover?: number | null
  foul?: number | null
  fieldGoalMade?: number | null
  fieldGoalAttempt?: number | null
  threePointMade?: number | null
  threePointAttempt?: number | null
  freeThrowMade?: number | null
  freeThrowAttempt?: number | null
  minutes?: number | null
}

export interface V4PublicMatchDetail {
  matchId: string
  seriesId?: string
  seriesName?: string
  seriesCode?: string
  seasonId: string
  seasonName?: string
  divisionId: string
  divisionName?: string
  stageId?: string
  stageName?: string
  stageType?: string
  roundNo?: number
  matchNo?: string
  matchTime?: string
  matchStatus?: string
  matchStatusName?: string
  publishStatus?: string
  venueText?: string
  replayUrl?: string
  videoUrl?: string
  homeScore?: number
  awayScore?: number
  originalHomeScore?: number
  originalAwayScore?: number
  winnerEventTeamId?: string
  homeTeam?: V4PublicEventTeamDetail
  awayTeam?: V4PublicEventTeamDetail
  teamStats: V4PublicMatchTeamStatItem[]
  playerStats: V4PublicMatchPlayerStatItem[]
  matchSeriesId?: string
  seriesGameNo?: number
  bestOf?: number
  seriesContext?: V4PublicMatchSeriesDetail
  resultType?: string
  resultReason?: string
  homeDisqualified?: boolean
  awayDisqualified?: boolean
  homeResultLabel?: string
  awayResultLabel?: string
  gameMode?: string
  periodScores?: Array<{ periodNo?: number; homeScore?: number; awayScore?: number }>
}

export interface V4PublicMatchSeriesGame {
  matchId?: string
  matchCode?: string
  seriesGameNo?: number
  matchStatus?: string
  matchStatusName?: string
  homeScore?: number
  awayScore?: number
  winnerEventTeamId?: string
  scheduledTime?: string
  skipped?: boolean
  skippedReason?: string
  currentGame?: boolean
  statsAvailable?: boolean
  replayUrl?: string
  videoUrl?: string
}

export interface V4PublicMatchSeriesDetail {
  id?: string
  seriesCode?: string
  divisionId?: string
  stageId?: string
  stageName?: string
  roundNo?: number
  roundName?: string
  homeTeamName?: string
  awayTeamName?: string
  bestOf?: number
  requiredWins?: number
  homeWinCount?: number
  awayWinCount?: number
  seriesStatus?: string
  seriesStatusName?: string
  winnerEventTeamId?: string
  homeEventTeamId?: string
  homeTeamShortName?: string
  homeTeamLogoFileId?: string
  homeTeamLogoUrl?: string
  awayEventTeamId?: string
  awayTeamShortName?: string
  awayTeamLogoFileId?: string
  awayTeamLogoUrl?: string
  winnerTeamName?: string
  finalStage?: boolean
  resultLabel?: string
  resultSummary?: string
  resultReason?: string
  resolutionType?: string
  resolutionTypeName?: string
  homeDisqualified?: boolean
  awayDisqualified?: boolean
  seasonId?: string
  seasonName?: string
  eventSeriesCode?: string
  eventSeriesName?: string
  divisionName?: string
  gameMode?: string
  scheduledDate?: string
  scheduledStartTime?: string
  games?: V4PublicMatchSeriesGame[]
  teamStats?: V4PublicMatchTeamStatItem[]
  playerStats?: V4PublicMatchPlayerStatItem[]
}

export interface V4PublicStandingItem {
  rankNo?: number
  eventTeamId: string
  teamName?: string
  teamLogoFileId?: string
  teamLogoUrl?: string
  winCount?: number
  loseCount?: number
  pointFor?: number
  pointAgainst?: number
  pointDiff?: number
  standingPoints?: number
  winRate?: number
  stageId?: string
  groupId?: string
  qualificationStatus?: string
  eventTeamStatus?: string
  disqualified?: boolean
}

export interface V4PublicStageItem {
  stageId: string
  stageCode?: string
  stageName?: string
  stageOrder?: number
  stageType?: string
  stageTypeName?: string
  organizationMode?: string
  organizationModeName?: string
  competitionFormat?: string
  competitionFormatName?: string
  executionStatus?: string
  executionStatusName?: string
  currentStage?: boolean
  advancementType?: string
  advancementCount?: number
  bestOf?: number
  nextStageCode?: string
}

export interface V4PublicStageStandingScope {
  groupScopeKey?: string
  groupId?: string
  groupCode?: string
  groupName?: string
  qualifyCount?: number
  rows?: V4PublicStandingItem[]
}

export interface V4PublicTeamStatItem {
  eventTeamId: string
  teamName?: string
  teamLogoFileId?: string
  teamLogoUrl?: string
  matchCount?: number
  winCount?: number
  loseCount?: number
  pointsFor?: number
  pointsAgainst?: number
  avgPointsFor?: number
  avgPointsAgainst?: number
  rebound?: number
  assist?: number
  steal?: number
  block?: number
  turnover?: number
  /** 当前 V4 Public team-stat VO 未下发 */
  foul?: number | null
}

export interface V4PublicPlayerStatItem {
  eventPlayerId: string
  sourcePlayerId?: string
  eventTeamId?: string
  playerName?: string
  teamName?: string
  matchCount?: number
  points?: number
  avgPoints?: number
  rebound?: number
  avgRebound?: number
  assist?: number
  avgAssist?: number
  steal?: number
  block?: number
  turnover?: number
  /** 当前 V4 Public player-stat VO 未下发 */
  foul?: number | null
  fieldGoalMade?: number | null
  fieldGoalAttempt?: number | null
  threePointMade?: number | null
  threePointAttempt?: number | null
  freeThrowMade?: number | null
  freeThrowAttempt?: number | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
}

export interface V4PublicRankItem {
  rankNo?: number
  rankType?: string
  targetType?: string
  eventTeamId?: string
  eventPlayerId?: string
  sourcePlayerId?: string
  teamName?: string
  playerName?: string
  rankValue?: number
}

export interface V4PublicScheduleItem {
  id: string
  unitType: 'MATCH' | 'MATCH_SERIES'
  stageId?: string
  stageName?: string
  roundNo?: number
  roundName?: string
  seriesCode?: string
  matchNo?: string
  homeEventTeamId?: string
  homeTeam?: string
  homeTeamShortName?: string
  homeTeamCode?: string
  homeTeamLogoFileId?: string
  homeTeamLogoUrl?: string
  awayEventTeamId?: string
  awayTeam?: string
  awayTeamShortName?: string
  awayTeamCode?: string
  awayTeamLogoFileId?: string
  awayTeamLogoUrl?: string
  bestOf?: number
  seriesScore?: string
  gameProgress?: string
  scheduleDate?: string
  startTime?: string
  status?: string
  statusName?: string
  placeholderSource?: string
  published?: boolean
  homeWinCount?: number
  awayWinCount?: number
  winnerEventTeamId?: string
  requiredWins?: number
  gamesPlayed?: number
  resultType?: string
  resultReason?: string
  homeDisqualified?: boolean
  awayDisqualified?: boolean
  homeScore?: number
  awayScore?: number
  homeResultLabel?: string
  awayResultLabel?: string
  replayUrl?: string
  videoUrl?: string
  hasOfficialStats?: boolean
}

export type V4SeriesCode = 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'

export type V4SeasonDetailTab = 'overview' | 'teams' | 'schedule' | 'standing' | 'stats'

export interface V4PublicTeamNameHistoryItem {
  changeId?: string
  oldName?: string
  newName?: string
  submittedAt?: string
  approvedAt?: string
}

export interface V4PublicTeamLogoHistoryItem {
  changeId?: string
  oldLogoFileId?: string
  newLogoFileId?: string
  oldLogoUrl?: string
  newLogoUrl?: string
  submittedAt?: string
  approvedAt?: string
}

export interface V4PublicTeamHonorItem {
  honorId?: string
  honorTitle?: string
  competitionName?: string
  awardDate?: string
}

export interface V4PublicTeamArchive {
  eventTeamId?: string
  sourceTeamId?: string
  nameHistory?: V4PublicTeamNameHistoryItem[]
  logoHistory?: V4PublicTeamLogoHistoryItem[]
  honors?: V4PublicTeamHonorItem[]
}
