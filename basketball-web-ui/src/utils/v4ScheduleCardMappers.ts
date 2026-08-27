import type { V4MyScheduleItem } from '@/api/types/v4PlayerEvents'
import type {
  ScheduleCardAccent,
  ScheduleListSection,
  ScheduleMatchActionsView,
  ScheduleMatchCardViewModel,
  ScheduleSubmissionSideView,
  ScheduleSubmissionTone,
  ScheduleVersusCenterView,
} from '@/types/v4ScheduleCard'

const FINISHED = 'FINISHED'
const PLAYING = 'PLAYING'
const SCHEDULED = 'SCHEDULED'
const CANCELLED = 'CANCELLED'
const FORFEITED = 'FORFEITED'

function normalizeStatus(value?: string | null): string {
  return (value || '').trim().toUpperCase()
}

function isAbnormalMatchStatus(status: string): boolean {
  return status === CANCELLED || status === FORFEITED
}

function isFinishedMatch(status: string): boolean {
  return status === FINISHED
}

function isUpcomingMatch(status: string): boolean {
  return !isFinishedMatch(status) && !isAbnormalMatchStatus(status) && status !== PLAYING
}

export function formatScheduleMatchDate(value?: string | null): string {
  if (!value) return '时间待定'
  const normalized = value.replace('T', ' ').trim()
  const datePart = normalized.slice(0, 10)
  if (!/^\d{4}-\d{2}-\d{2}$/.test(datePart)) return '时间待定'
  return datePart
}

export function formatScheduleMatchTime(value?: string | null): string {
  if (!value) return ''
  const normalized = value.replace('T', ' ').trim()
  const timePart = normalized.slice(11, 16)
  if (!/^\d{2}:\d{2}$/.test(timePart)) return ''
  return timePart
}

export function formatScheduleMatchDateTime(value?: string | null): string {
  const date = formatScheduleMatchDate(value)
  const time = formatScheduleMatchTime(value)
  if (date === '时间待定') return date
  return time ? `${date} ${time}` : date
}

export function buildTeamShortName(name?: string | null): string {
  const trimmed = (name || '').trim()
  if (!trimmed) return '--'
  const latinTail = trimmed.match(/([A-Za-z0-9]+)$/)?.[1]
  if (latinTail && latinTail.length <= 4) return latinTail.toUpperCase()
  if (trimmed.length <= 3) return trimmed.toUpperCase()
  return trimmed.slice(0, 2).toUpperCase()
}

export function scheduleMatchStatusLabel(status?: string | null): string {
  switch (normalizeStatus(status)) {
    case PLAYING:
      return '比赛中'
    case FINISHED:
      return '已结束'
    case CANCELLED:
      return '已取消'
    case FORFEITED:
      return '已弃赛'
    case SCHEDULED:
    default:
      return '未开始'
  }
}

export function scheduleSubmissionLabel(
  status: string | null | undefined,
  side: 'my' | 'opponent',
): string {
  const normalized = normalizeStatus(status)
  if (!normalized) return side === 'my' ? '待提交' : '等待提交'
  switch (normalized) {
    case 'DRAFT':
      return side === 'my' ? '待提交' : '等待提交'
    case 'PENDING_REVIEW':
      return '审核中'
    case 'APPROVED':
      return '已审核通过'
    case 'REJECTED':
      return '已驳回'
    case 'SUPERSEDED':
      return '已作废'
    case 'CANCELLED':
      return '已取消'
    default:
      return side === 'my' ? '待提交' : '等待提交'
  }
}

export function scheduleSubmissionTone(
  status: string | null | undefined,
  side: 'my' | 'opponent',
): ScheduleSubmissionTone {
  const normalized = normalizeStatus(status)
  if (!normalized || normalized === 'DRAFT') return 'pending'
  switch (normalized) {
    case 'PENDING_REVIEW':
      return 'review'
    case 'APPROVED':
      return 'approved'
    case 'REJECTED':
      return 'rejected'
    case 'CANCELLED':
    case 'SUPERSEDED':
      return 'cancelled'
    default:
      return side === 'opponent' ? 'pending' : 'pending'
  }
}

function buildDivisionLine(row: V4MyScheduleItem): string {
  const division = (row.divisionName || '').trim()
  if (division) return division.includes('赛区') ? division : `${division} 赛区`
  return '赛区待定'
}

function buildRoundLine(row: V4MyScheduleItem): string {
  if (row.matchSeriesId && row.bestOf && row.bestOf > 1) {
    const gamePart = row.seriesGameNo ? `Game ${row.seriesGameNo}/${row.bestOf}` : `BO${row.bestOf}`
    const setScore = row.seriesHomeWinCount != null && row.seriesAwayWinCount != null
      ? ` · 局分 ${row.seriesHomeWinCount}:${row.seriesAwayWinCount}`
      : ''
    return `${gamePart}${setScore}`
  }
  if (row.roundNo) return `第 ${row.roundNo} 轮`
  return '轮次待定'
}

function buildMatchStatusTone(status: string): ScheduleMatchCardViewModel['header']['matchStatusTone'] {
  if (status === FINISHED) return 'finished'
  if (status === PLAYING) return 'warning'
  if (isAbnormalMatchStatus(status)) return 'danger'
  if (status === SCHEDULED || !status) return 'default'
  return 'default'
}

function canSubmitData(row: V4MyScheduleItem): boolean {
  return Boolean(row.canSubmitData ?? row.canSubmit)
}

function buildSubmissionSide(
  row: V4MyScheduleItem,
  side: 'my' | 'opponent',
): ScheduleSubmissionSideView {
  const status = side === 'my' ? row.mySubmissionStatus : row.opponentSubmissionStatus
  const detail = side === 'my' && normalizeStatus(status) === 'REJECTED' && row.latestRejectReason
    ? row.latestRejectReason
    : undefined
  return {
    sideLabel: side === 'my' ? '本方提交' : '对方提交',
    statusLabel: scheduleSubmissionLabel(status, side),
    tone: scheduleSubmissionTone(status, side),
    detail,
  }
}

function resolveWinnerSide(row: V4MyScheduleItem): { home?: boolean; away?: boolean } {
  if (!isFinishedMatch(normalizeStatus(row.matchStatus))) return {}
  const winner = String(row.winnerEventTeamId || '')
  if (!winner) return {}
  if (winner === String(row.homeEventTeamId || '')) return { home: true, away: false }
  if (winner === String(row.awayEventTeamId || '')) return { home: false, away: true }
  return {}
}

function buildCenterView(row: V4MyScheduleItem): ScheduleVersusCenterView {
  const matchStatus = normalizeStatus(row.matchStatus)

  if (isAbnormalMatchStatus(matchStatus)) {
    return {
      mode: 'status',
      statusText: scheduleMatchStatusLabel(matchStatus),
    }
  }

  if (matchStatus === PLAYING) {
    return {
      mode: 'playing',
      homeScore: row.homeScore ?? undefined,
      awayScore: row.awayScore ?? undefined,
      statusText: '比赛中',
      dateText: formatScheduleMatchDate(row.matchTime),
      timeText: formatScheduleMatchTime(row.matchTime),
    }
  }

  if (isFinishedMatch(matchStatus)) {
    if (row.homeScore != null && row.awayScore != null) {
      return {
        mode: 'score',
        homeScore: row.homeScore,
        awayScore: row.awayScore,
        statusText: '比赛已结束',
      }
    }
    return {
      mode: 'status',
      statusText: '比赛已结束',
    }
  }

  return {
    mode: 'upcoming',
    dateText: formatScheduleMatchDate(row.matchTime),
    timeText: formatScheduleMatchTime(row.matchTime) || undefined,
    statusText: '未开始',
    countdownHint: buildCountdownHint(row.matchTime),
  }
}

function buildCountdownHint(matchTime?: string | null): string | undefined {
  if (!matchTime) return undefined
  const target = Date.parse(matchTime.replace(' ', 'T'))
  if (Number.isNaN(target)) return undefined
  const diffMs = target - Date.now()
  if (diffMs <= 0) return undefined
  const hours = Math.floor(diffMs / (1000 * 60 * 60))
  if (hours < 24) {
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60))
    return `距开赛 ${hours} 小时 ${minutes} 分钟`
  }
  const days = Math.floor(hours / 24)
  return `距开赛 ${days} 天`
}

function buildActions(row: V4MyScheduleItem): ScheduleMatchActionsView {
  const matchStatus = normalizeStatus(row.matchStatus)
  const myStatus = normalizeStatus(row.mySubmissionStatus)
  const opponentStatus = normalizeStatus(row.opponentSubmissionStatus)
  const submitRoute = row.bestOf && row.bestOf > 1 && row.matchSeriesId
    ? {
        name: 'AccountV4SeriesSubmission',
        params: { matchSeriesId: row.matchSeriesId },
      }
    : {
        name: 'AccountV4MatchSubmission',
        params: { matchId: row.matchId },
        query: { submitEventTeamId: row.myEventTeamId },
      }
  const detailRoute = {
    name: 'V4MatchDetail',
    params: { matchId: row.matchId },
  } as const

  if (isAbnormalMatchStatus(matchStatus)) {
    return {
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
      readOnlyLabel: scheduleMatchStatusLabel(matchStatus),
    }
  }

  if (canSubmitData(row)) {
    const label = myStatus === 'REJECTED' ? '修改并重新提交' : '提交比赛数据'
    return {
      primary: {
        label,
        kind: 'primary',
        route: submitRoute,
      },
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
    }
  }

  if (myStatus === 'PENDING_REVIEW') {
    return {
      primary: {
        label: '查看提交详情',
        kind: 'secondary',
        route: submitRoute,
      },
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
    }
  }

  if (myStatus === 'APPROVED' && opponentStatus !== 'APPROVED') {
    return {
      secondary: {
        label: '查看提交详情',
        kind: 'secondary',
        route: submitRoute,
      },
      hint: '本方数据已完成，当前无需操作，等待对方提交。',
    }
  }

  if (myStatus === 'APPROVED' && opponentStatus === 'APPROVED') {
    return {
      primary: {
        label: '查看双方提交',
        kind: 'secondary',
        route: detailRoute,
      },
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
      hint: '比赛数据已确认',
    }
  }

  if (isUpcomingMatch(matchStatus)) {
    return {
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
    }
  }

  if (isFinishedMatch(matchStatus) && myStatus) {
    return {
      primary: {
        label: '查看提交详情',
        kind: 'secondary',
        route: submitRoute,
      },
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
      readOnlyLabel: row.cannotSubmitReason || undefined,
    }
  }

  if (isFinishedMatch(matchStatus)) {
    return {
      secondary: {
        label: '查看本场详情',
        kind: 'secondary',
        route: detailRoute,
      },
      readOnlyLabel: row.cannotSubmitReason || undefined,
    }
  }

  return {
    secondary: {
      label: '查看本场详情',
      kind: 'secondary',
      route: detailRoute,
    },
    readOnlyLabel: row.cannotSubmitReason || undefined,
  }
}

function resolveAccent(row: V4MyScheduleItem): ScheduleCardAccent {
  const matchStatus = normalizeStatus(row.matchStatus)
  const myStatus = normalizeStatus(row.mySubmissionStatus)
  const opponentStatus = normalizeStatus(row.opponentSubmissionStatus)

  if (isAbnormalMatchStatus(matchStatus)) return 'abnormal'
  if (canSubmitData(row)) return 'my-pending-submit'
  if (myStatus === 'PENDING_REVIEW') return 'my-pending-review'
  if (myStatus === 'REJECTED') return 'my-rejected'
  if (myStatus === 'APPROVED' && opponentStatus === 'APPROVED') return 'confirmed'
  if (myStatus === 'APPROVED') return 'waiting-opponent'
  if (isFinishedMatch(matchStatus)) return 'finished'
  return 'upcoming'
}

function needsUserAction(row: V4MyScheduleItem): boolean {
  return canSubmitData(row)
}

function compareUpcoming(a: V4MyScheduleItem, b: V4MyScheduleItem): number {
  const timeA = Date.parse((a.matchTime || '').replace(' ', 'T'))
  const timeB = Date.parse((b.matchTime || '').replace(' ', 'T'))
  const safeA = Number.isNaN(timeA) ? Number.MAX_SAFE_INTEGER : timeA
  const safeB = Number.isNaN(timeB) ? Number.MAX_SAFE_INTEGER : timeB
  if (safeA !== safeB) return safeA - safeB
  return String(a.matchId).localeCompare(String(b.matchId))
}

function compareFinishedDesc(a: V4MyScheduleItem, b: V4MyScheduleItem): number {
  return -compareUpcoming(a, b)
}

export function mapScheduleRowToCardView(
  row: V4MyScheduleItem,
  logoResolver: (fileId?: string | null) => string,
): ScheduleMatchCardViewModel {
  const matchStatus = normalizeStatus(row.matchStatus)
  const winner = resolveWinnerSide(row)
  const myEventTeamId = row.myEventTeamId || ''

  return {
    key: `${row.matchId}-${row.myEventTeamId || 'none'}`,
    accent: resolveAccent(row),
    header: {
      seasonTitle: (row.seasonTitle || '赛事').trim(),
      divisionLine: buildDivisionLine(row),
      roundLine: buildRoundLine(row),
      matchStatusLabel: scheduleMatchStatusLabel(matchStatus),
      matchStatusTone: buildMatchStatusTone(matchStatus),
    },
    home: {
      eventTeamId: row.homeEventTeamId,
      name: row.homeTeamName || '主队',
      shortName: buildTeamShortName(row.homeTeamName),
      logoUrl: logoResolver(row.homeTeamLogoFileId),
      isMine: myEventTeamId === row.homeEventTeamId,
      isWinner: winner.home,
    },
    away: {
      eventTeamId: row.awayEventTeamId,
      name: row.awayTeamName || '客队',
      shortName: buildTeamShortName(row.awayTeamName),
      logoUrl: logoResolver(row.awayTeamLogoFileId),
      isMine: myEventTeamId === row.awayEventTeamId,
      isWinner: winner.away,
    },
    center: buildCenterView(row),
    mySubmission: buildSubmissionSide(row, 'my'),
    opponentSubmission: buildSubmissionSide(row, 'opponent'),
    actions: buildActions(row),
    matchNo: row.matchNo || undefined,
    raw: row,
  }
}

export function buildScheduleListSections(
  rows: V4MyScheduleItem[],
  includeFinished: boolean,
  logoResolver: (fileId?: string | null) => string,
): ScheduleListSection[] {
  const cards = rows.map((row) => mapScheduleRowToCardView(row, logoResolver))
  const cardByKey = new Map(cards.map((card) => [card.key, card]))

  const pendingRows = rows.filter(needsUserAction).sort(compareUpcoming)
  const upcomingRows = rows
    .filter((row) => {
      const status = normalizeStatus(row.matchStatus)
      return !isFinishedMatch(status) && !isAbnormalMatchStatus(status) && !needsUserAction(row)
    })
    .sort(compareUpcoming)
  const finishedRows = rows
    .filter((row) => isFinishedMatch(normalizeStatus(row.matchStatus)))
    .sort(compareFinishedDesc)
  const abnormalRows = rows
    .filter((row) => isAbnormalMatchStatus(normalizeStatus(row.matchStatus)))
    .sort(compareFinishedDesc)

  const sections: ScheduleListSection[] = []

  const pushSection = (key: string, title: string, sourceRows: V4MyScheduleItem[]) => {
    const items = sourceRows
      .map((row) => cardByKey.get(`${row.matchId}-${row.myEventTeamId || 'none'}`))
      .filter((item): item is ScheduleMatchCardViewModel => Boolean(item))
    if (items.length) sections.push({ key, title, items })
  }

  if (!includeFinished) {
    pushSection('pending', '待处理', pendingRows)
    pushSection('upcoming', '待比赛', upcomingRows)
    if (sections[0]?.items[0]) sections[0].items[0].isNextMatch = true
    else if (sections[1]?.items[0]) sections[1].items[0].isNextMatch = true
    return sections
  }

  pushSection('pending', '待处理', pendingRows)
  pushSection('upcoming', '近期赛程', upcomingRows)
  pushSection('finished', '历史赛程', finishedRows)
  pushSection('abnormal', '异常赛程', abnormalRows)

  const firstUpcoming = sections.find((section) => section.key === 'upcoming')?.items[0]
    || sections.find((section) => section.key === 'pending')?.items[0]
  if (firstUpcoming) firstUpcoming.isNextMatch = true

  return sections
}

export function scheduleNeedsUserAction(row: V4MyScheduleItem): boolean {
  return needsUserAction(row)
}
