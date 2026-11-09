/** 官网公开接口 VO，字段对齐 basketball-web/.../response/publicapi */

export interface PublicMatchItemVo {
  matchId: string
  matchNo?: string | null
  seasonId?: string | null
  seasonName?: string | null
  seriesId?: string | null
  seriesCode?: string | null
  seriesName?: string | null
  divisionId?: string | null
  divisionName?: string | null
  stageId?: string | null
  stageName?: string | null
  groupId?: string | null
  groupName?: string | null
  homeTeamId?: string | null
  homeTeamName?: string | null
  homeTeamLogoUrl?: string | null
  awayTeamId?: string | null
  awayTeamName?: string | null
  awayTeamLogoUrl?: string | null
  homeScore?: number | null
  awayScore?: number | null
  winnerTeamId?: string | null
  matchStatus?: string | null
  dataStatus?: number | null
  venueName?: string | null
  scheduledStartTime?: string | null
  actualEndTime?: string | null
  resultType?: string | null
  resultReason?: string | null
  homeResultLabel?: string | null
  awayResultLabel?: string | null
  homeDisqualified?: boolean | null
  awayDisqualified?: boolean | null
  originalHomeScore?: number | null
  originalAwayScore?: number | null
}

export interface PublicMatchTeamStatVo {
  teamId?: string | null
  teamName?: string | null
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  turnovers?: number | null
  fouls?: number | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
}

export interface PublicMatchPlayerStatVo {
  playerId?: string | null
  playerName?: string | null
  avatarUrl?: string | null
  teamId?: string | null
  teamName?: string | null
  startedFlag?: number | null
  minutesSeconds?: number | null
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  turnovers?: number | null
  fouls?: number | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
  efficiency?: number | string | null
}

export interface PublicMatchDetailVo extends PublicMatchItemVo {
  seasonName?: string | null
  winnerTeamId?: string | null
  statsAvailable?: boolean | null
  actualStartTime?: string | null
  teamStats?: PublicMatchTeamStatVo[] | null
  playerStats?: PublicMatchPlayerStatVo[] | null
}

export interface PublicTeamRankItemVo {
  seasonId?: string | null
  rankType?: string | null
  rankTypeName?: string | null
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  rankNo?: number | null
  previousRank?: number | null
  statValue?: number | string | null
  snapshotDate?: string | null
}

export interface PublicPlayerRankItemVo {
  seasonId?: string | null
  rankType?: string | null
  rankTypeName?: string | null
  playerId: string
  playerName?: string | null
  avatarUrl?: string | null
  teamId?: string | null
  teamName?: string | null
  rankNo?: number | null
  previousRank?: number | null
  statValue?: number | string | null
  snapshotDate?: string | null
}

export interface PublicSeasonSummaryVo {
  seasonId?: string | null
  seasonName?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  seasonStatus?: number | null
  teamCount?: number | null
  matchCount?: number | null
  finishedMatchCount?: number | null
  standingCount?: number | null
  teamStatCount?: number | null
  playerStatCount?: number | null
}

export interface PublicSocialLinkResp {
  id: string
  platformCode: string
  title?: string | null
  titleEn?: string | null
  subtitle?: string | null
  subtitleEn?: string | null
  buttonText?: string | null
  buttonTextEn?: string | null
  jumpUrl?: string | null
  sortOrder?: number | null
}

export interface PublicSiteThemeResp {
  configured?: boolean
  source?: string | null
  bgColor?: string | null
  bgImageUrl?: string | null
  mobileBgImageUrl?: string | null
  bgDisplay?: string | null
  colors?: Record<string, string> | null
}

export interface PublicPageHeroResp {
  configured?: boolean
  pageKey?: string | null
  seasonId?: string | null
  mode?: string | null
  imageUrl?: string | null
  mobileImageUrl?: string | null
  focusX?: number | null
  focusY?: number | null
  maskOpacity?: number | null
  altText?: string | null
}

export interface PublicHomeBannerResp {
  bannerId: string
  title?: string | null
  titleEn?: string | null
  subtitle?: string | null
  subtitleEn?: string | null
  imageUrl?: string | null
  mobileImageUrl?: string | null
  altText?: string | null
  positionCode?: string | null
  targetType?: string | null
  targetValue?: string | null
  sortNo?: number | null
}

export interface PublicAnnouncementCategoryResp {
  categoryCode: string
  categoryName?: string | null
}

export interface PublicAnnouncementItemResp {
  announcementId: string
  announcementNo?: string | null
  categoryCode?: string | null
  categoryName?: string | null
  title?: string | null
  titleEn?: string | null
  summary?: string | null
  summaryEn?: string | null
  coverUrl?: string | null
  authorName?: string | null
  isTop?: number | null
  isImportant?: number | null
  publishTime?: string | null
  viewCount?: number | string | null
}

export interface PublicAnnouncementDetailResp extends PublicAnnouncementItemResp {
  /** 正文纯文本，勿当 HTML 渲染 */
  contentText?: string | null
  contentTextEn?: string | null
}

export interface PublicAnnouncementAdjacentResp {
  previous?: PublicAnnouncementItemResp | null
  next?: PublicAnnouncementItemResp | null
}

export interface PublicPartnerResp {
  partnerId: string
  partnerName?: string | null
  logoUrl?: string | null
  websiteUrl?: string | null
  partnerType?: string | null
  sortNo?: number | null
}

export interface PublicPartnerSupportRecordResp {
  recordId?: string | number | null
  partnerId?: string | number | null
  partnerName?: string | null
  partnerLogoUrl?: string | null
  websiteUrl?: string | null
  supportType?: string | null
  supportTypeText?: string | null
  supportAmount?: number | string | null
  supportDisplayText?: string | null
  supportDate?: string | null
  publicDescription?: string | null
}

export interface PublicPartnerSummaryResp {
  supportRecordCount?: number | string | null
  totalCashAmount?: number | string | null
}

export interface PublicPartnerPageResp {
  total?: number | string | null
  rows?: PublicPartnerSupportRecordResp[] | null
}

export interface PublicPartnersResp {
  summary?: PublicPartnerSummaryResp | null
  page?: PublicPartnerPageResp | null
}

export interface PublicHonorLineupItemResp {
  lineupId?: string | null
  teamId?: string | null
  playerId?: string | null
  playerName?: string | null
  avatarUrl?: string | null
  roleType?: string | null
  jerseyNo?: string | null
  positionCode?: string | null
  sortNo?: number | null
}

export interface PublicHonorItemResp {
  honorId: string
  tournamentId?: string | null
  tournamentName?: string | null
  seasonId?: string | null
  seasonName?: string | null
  seasonNameEn?: string | null
  honorCode?: string | null
  honorName?: string | null
  honorScope?: string | null
  recipientType?: string | null
  teamId?: string | null
  teamName?: string | null
  teamLogoUrl?: string | null
  eventTeamId?: string | null
  platformCode?: string | null
  playerId?: string | null
  playerName?: string | null
  playerAvatarUrl?: string | null
  honorTitle?: string | null
  honorTitleEn?: string | null
  honorSubtitle?: string | null
  honorSubtitleEn?: string | null
  honorValue?: string | null
  awardDate?: string | null
  coverUrl?: string | null
  seriesHonorIconUrl?: string | null
  displayOrder?: number | null
}

export interface PublicHonorDetailResp extends PublicHonorItemResp {
  lineup?: PublicHonorLineupItemResp[] | null
}

export interface PublicHallOverviewResp {
  tournamentHonorCount?: number | null
  playerHonorCount?: number | null
  teamHonorCount?: number | null
  latestTournamentHonors?: PublicHonorItemResp[] | null
  latestPlayerHonors?: PublicHonorItemResp[] | null
  latestTeamHonors?: PublicHonorItemResp[] | null
}

export interface PublicSeasonPlacementResp {
  seasonId: string
  seasonName?: string | null
  published?: boolean | null
  championTeamId?: string | null
  championTeamName?: string | null
  championTeamLogoUrl?: string | null
  runnerUpTeamId?: string | null
  runnerUpTeamName?: string | null
  runnerUpTeamLogoUrl?: string | null
  championHonorId?: string | null
}

export interface PublicHomePlayerLeaderVo {
  eventPlayerId?: string | null
  sourcePlayerId?: string | null
  playerName?: string | null
  avatarUrl?: string | null
  eventTeamId?: string | null
  teamName?: string | null
  teamLogoUrl?: string | null
  matchCount?: number | null
  value?: number | string | null
}

export interface PublicHomeFocusTeamVo {
  eventTeamId?: string | null
  teamName?: string | null
  teamLogoUrl?: string | null
  weekMatchCount?: number | null
  weekWinCount?: number | null
  weekLoseCount?: number | null
  weekPointDiff?: number | null
  weekAvgPointDiff?: number | string | null
  weekWinRate?: number | string | null
  recentForm?: string[] | null
}

export interface PublicHomeDataFocusVo {
  seriesCode?: string | null
  seasonId?: string | null
  divisionId?: string | null
  pointsLeader?: PublicHomePlayerLeaderVo | null
  assistsLeader?: PublicHomePlayerLeaderVo | null
  reboundsLeader?: PublicHomePlayerLeaderVo | null
  focusTeam?: PublicHomeFocusTeamVo | null
}

export interface PublicHomeVo {
  recentMatches?: PublicMatchItemVo[] | null
  teamRankings?: PublicTeamRankItemVo[] | null
  playerRankings?: PublicPlayerRankItemVo[] | null
  seasonSummary?: PublicSeasonSummaryVo | null
  hotTeamSourceNote?: string | null
  banners?: PublicHomeBannerResp[] | null
  latestAnnouncements?: PublicAnnouncementItemResp[] | null
  partners?: PublicPartnerResp[] | null
}

export interface PublicTournamentItemVo {
  tournamentId: string
  tournamentNo?: string | null
  tournamentName?: string | null
  shortName?: string | null
  tournamentType?: string | null
  gameTerminal?: string | null
  formatCode?: string | null
  organizerName?: string | null
  tournamentStatus?: number | null
  logoUrl?: string | null
  coverUrl?: string | null
  displayOrder?: number | null
  updateTime?: string | null
  competitionSeasonId?: string | null
  competitionSeasonName?: string | null
  competitionSeasonYear?: number | null
  registrationStartTime?: string | null
  registrationEndTime?: string | null
  registrationStatus?: 'NOT_STARTED' | 'OPEN' | 'CLOSED' | 'ENDED' | string | null
}

export interface PublicTournamentDetailVo {
  tournamentId: string
  tournamentNo?: string | null
  tournamentName?: string | null
  shortName?: string | null
  englishName?: string | null
  tournamentType?: string | null
  gameTerminal?: string | null
  formatCode?: string | null
  organizerName?: string | null
  description?: unknown
  tournamentStatus?: number | null
  logoUrl?: string | null
  coverUrl?: string | null
  competitionSeasonId?: string | null
  competitionSeasonName?: string | null
  competitionSeasonYear?: number | null
}

export interface PublicSeasonItemVo {
  seasonId: string
  seasonNo?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  seasonName?: string | null
  seasonYear?: string | null
  seasonStatus?: number | null
  registrationStartTime?: string | null
  registrationEndTime?: string | null
  seasonStartTime?: string | null
  seasonEndTime?: string | null
  maxTeamCount?: number | null
  competitionSeasonId?: string | null
  competitionSeasonName?: string | null
  competitionSeasonYear?: number | null
}

export interface PublicSeasonOverviewVo {
  seasonId: string
  seasonName?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  seasonStatus?: number | null
  teamCount?: number | null
  finishedMatchCount?: number | null
  publicMatchCount?: number | null
  standingCount?: number | null
  description?: unknown
  competitionSeasonId?: string | null
  competitionSeasonName?: string | null
  competitionSeasonYear?: number | null
}

export interface PublicStandingItemVo {
  standingId?: string | null
  seasonId?: string | null
  stageId?: string | null
  stageName?: string | null
  groupId?: string | null
  groupName?: string | null
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  rankNo?: number | null
  matchesPlayed?: number | null
  wins?: number | null
  losses?: number | null
  pointsFor?: number | null
  pointsAgainst?: number | null
  pointDiff?: number | null
  standingPoints?: number | string | null
  winRate?: number | string | null
  streakType?: string | null
  streakCount?: number | null
  disqualified?: boolean | null
  qualifiedFlag?: number | null
}

export interface PublicTeamSeasonStatVo {
  seasonId?: string | null
  stageId?: string | null
  teamId: string
  teamName?: string | null
  teamLogoUrl?: string | null
  games?: number | null
  wins?: number | null
  losses?: number | null
  pointsFor?: number | null
  pointsAgainst?: number | null
  avgPoints?: number | string | null
  avgPointsAgainst?: number | string | null
  avgRebounds?: number | string | null
  avgAssists?: number | string | null
  avgSteals?: number | string | null
  avgBlocks?: number | string | null
  avgTurnovers?: number | string | null
  /** 当前 V4 Public team-stat 未下发；有值才展示 */
  avgFouls?: number | string | null
  fieldGoalPct?: number | string | null
  threePointPct?: number | string | null
  freeThrowPct?: number | string | null
  winRate?: number | string | null
}

export interface PublicPlayerSeasonStatVo {
  seriesId?: string | null
  seriesName?: string | null
  seasonId?: string | null
  seasonName?: string | null
  divisionId?: string | null
  divisionName?: string | null
  divisionStatus?: string | null
  matchEndTime?: string | null
  platformCode?: string | null
  gameMode?: string | null
  stageId?: string | null
  teamId?: string | null
  eventTeamId?: string | null
  teamName?: string | null
  teamLogoUrl?: string | null
  playerId: string
  eventPlayerId?: string | null
  sourcePlayerId?: string | null
  playerName?: string | null
  avatarUrl?: string | null
  games?: number | null
  hasOfficialStat?: boolean | null
  starts?: number | null
  points?: number | null
  rebounds?: number | null
  assists?: number | null
  steals?: number | null
  blocks?: number | null
  turnovers?: number | null
  fouls?: number | null
  avgMinutes?: number | string | null
  avgPoints?: number | string | null
  avgRebounds?: number | string | null
  avgAssists?: number | string | null
  avgSteals?: number | string | null
  avgBlocks?: number | string | null
  avgTurnovers?: number | string | null
  avgFouls?: number | string | null
  fieldGoalMade?: number | null
  fieldGoalAttempt?: number | null
  fieldGoalPct?: number | string | null
  threePointMade?: number | null
  threePointAttempt?: number | null
  threePointPct?: number | string | null
  freeThrowMade?: number | null
  freeThrowAttempt?: number | null
  freeThrowPct?: number | string | null
  avgEfficiency?: number | string | null
}

export interface PublicTeamDetailVo {
  teamId: string
  teamNo?: string | null
  teamName?: string | null
  shortName?: string | null
  englishName?: string | null
  logoUrl?: string | null
  regionCode?: string | null
  regionName?: string | null
  gameTerminal?: string | null
  description?: unknown
  teamStatus?: number | null
  teamStatusText?: string | null
  auditStatus?: number | null
  currentMemberCount?: number | null
  divisionId?: string | null
  divisionName?: string | null
  divisionStatus?: string | null
  platformCode?: string | null
  gameMode?: string | null
  seasonId?: string | null
  seasonName?: string | null
  seriesId?: string | null
  seriesName?: string | null
  seriesCode?: string | null
  eventTeamStatus?: string | null
  disqualified?: boolean | null
  activeDisciplineSummary?: import('./v4Public').PublicActiveDisciplineSummary[] | null
  legacyTeam?: boolean | null
  establishedDate?: string | null
}

export interface PublicTeamRosterItemVo {
  /** 永久球员 ID，用于 /player/{id} */
  playerId: string
  /** 赛事球员快照 ID，仅用于赛事内关联，不可用于公开球员路由 */
  eventPlayerId?: string | null
  playerName?: string | null
  avatarUrl?: string | null
  positionCode?: string | null
  jerseyNo?: string | null
  roleType?: string | null
  rosterType?: string | null
  certificationStatus?: number | null
}

export interface PublicTeamSeasonHistoryVo {
  seasonId: string
  seasonName?: string | null
  tournamentId?: string | null
  tournamentName?: string | null
  displayName?: string | null
  finalRank?: number | null
  seasonStatus?: number | null
  games?: number | null
  wins?: number | null
  losses?: number | null
  winRate?: number | string | null
}

export interface PublicTeamCompareVo {
  leftTeam?: PublicTeamDetailVo | null
  rightTeam?: PublicTeamDetailVo | null
  leftStat?: PublicTeamSeasonStatVo | null
  rightStat?: PublicTeamSeasonStatVo | null
  seasonId?: string | null
  seasonName?: string | null
}

export interface PublicPlayerCurrentTeamVo {
  teamId?: string | null
  teamName?: string | null
  shortName?: string | null
  logoUrl?: string | null
  divisionId?: string | null
  divisionName?: string | null
  platformCode?: string | null
  gameMode?: string | null
  seasonId?: string | null
  seasonName?: string | null
  seriesName?: string | null
  seriesCode?: string | null
  myRole?: string | null
  eventTeamId?: string | null
  disqualified?: boolean | null
  eventTeamStatus?: string | null
  eventTeamStatusName?: string | null
}

export interface PublicPlayerDetailVo {
  playerId: string
  playerNo?: string | null
  displayName?: string | null
  avatarUrl?: string | null
  positionCode?: string | null
  dominantHand?: string | null
  heightCm?: number | string | null
  weightKg?: number | string | null
  /** @deprecated 兼容字段；请用 currentTeams */
  currentTeamId?: string | null
  /** @deprecated 兼容字段；请用 currentTeams */
  currentTeamName?: string | null
  /** @deprecated 兼容字段；请用 currentTeams */
  currentTeamLogoUrl?: string | null
  currentTeams?: PublicPlayerCurrentTeamVo[] | null
  activeDisciplineSummary?: import('./v4Public').PublicActiveDisciplineSummary[] | null
  certificationStatus?: number | null
  playerStatus?: number | null
  profilePublic?: boolean | null
  privacyMessage?: string | null
}
