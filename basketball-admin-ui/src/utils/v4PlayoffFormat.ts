/**
 * Playoff stage expansion + bracket preview helpers.
 * First-round seed order MUST match PlayoffBracketSupport.firstRoundSeedOrder.
 */

export const PLAYOFF_SUPPORTED_TEAM_COUNTS = [4, 8, 16] as const

/** Same as PlayoffBracketSupport.firstRoundSeedOrder */
export const PLAYOFF_FIRST_ROUND_SEED_ORDER: Record<4 | 8 | 16, number[]> = {
  4: [1, 4, 2, 3],
  8: [1, 8, 4, 5, 2, 7, 3, 6],
  16: [1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14]
}

export type V4PlayoffSeedSource = 'TEAM_CREATE_TIME' | 'STANDING'
export type V4PlayoffBestOf = 1 | 3 | 5

export interface V4PlayoffStageLike {
  id?: number
  stageCode?: string
  stageName?: string
  stageOrder?: number
  stageType?: string
  organizationMode?: string
  competitionFormat?: string
  groupingMode?: string
  groupCount?: number
  roundRobinCycles?: number
  bestOf?: number
  winPoints?: number
  lossPoints?: number
  advancementType?: string
  advancementCount?: number
  pairingRule?: string
  nextStageCode?: string
}

export interface V4PlayoffStagePayload {
  stage: {
    stageCode: string
    stageName: string
    stageOrder: number
    stageType: string
    organizationMode: string
    competitionFormat: string
    groupingMode: string
    bestOf: number
    advancementType: string
    pairingRule: string
    nextStageCode?: string
    requiresManualConfirmation: number
  }
}

export interface V4PlayoffBlock<T extends V4PlayoffStageLike = V4PlayoffStageLike> {
  startIndex: number
  stages: T[]
  teamCount: number
}

export function isV4PlayoffTeamCount(n: number): boolean {
  return n === 4 || n === 8 || n === 16
}

export function playoffFirstRoundSeedOrder(teamCount: number): number[] {
  if (teamCount === 4 || teamCount === 8 || teamCount === 16) {
    return PLAYOFF_FIRST_ROUND_SEED_ORDER[teamCount]
  }
  return []
}

export function buildV4PlayoffRoundSizes(teamCount: number): number[] {
  if (!isV4PlayoffTeamCount(teamCount)) return []
  const sizes: number[] = []
  let n = teamCount
  while (n >= 2) {
    sizes.push(n)
    n = n / 2
  }
  return sizes
}

export function formatV4PlayoffStageName(teamCount: number): string {
  if (teamCount <= 2) return '总决赛'
  if (teamCount === 4) return '4强赛'
  if (teamCount === 8) return '8强赛'
  if (teamCount === 16) return '16强赛'
  return `${teamCount}强赛`
}

export function formatV4PlayoffSeedSourceLabel(source?: string | null): string {
  if ((source || '').toUpperCase() === 'STANDING') return '按积分赛排名'
  return '按球队创建时间顺序'
}

export function normalizeV4PlayoffBestOf(value?: number | null): V4PlayoffBestOf {
  if (value === 3 || value === 5) return value
  return 1
}

export function isV4PlayoffBracketStage(stage?: V4PlayoffStageLike | null): boolean {
  if (!stage) return false
  return (stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION'
    && (stage.pairingRule || '').toUpperCase() === 'PLAYOFF_BRACKET'
}

export function isV4StandingCapableStage(stage?: V4PlayoffStageLike | null): boolean {
  return (stage?.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN'
}

export function buildV4PlayoffBracketPreviewLines(teamCount: number): string[] {
  const order = playoffFirstRoundSeedOrder(teamCount)
  if (!order.length) return []
  const lines: string[] = []
  for (let i = 0; i < order.length; i += 2) {
    lines.push(`Seed${order[i]} vs Seed${order[i + 1]}`)
  }
  return lines
}

export function resolvePlayoffInputTeamCount(
  prefixStages: V4PlayoffStageLike[],
  maxTeamCount: number
): number {
  if (!prefixStages.length) return maxTeamCount
  const last = prefixStages[prefixStages.length - 1]
  const adv = (last.advancementType || '').toUpperCase()
  if (adv === 'GROUP_TOP_N' && last.groupCount && last.advancementCount) {
    return last.groupCount * last.advancementCount
  }
  if (adv === 'OVERALL_TOP_N' && last.advancementCount) {
    return last.advancementCount
  }
  return maxTeamCount
}

export function findPlayoffBlock<T extends V4PlayoffStageLike>(stages: T[]): V4PlayoffBlock<T> | null {
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  let start = -1
  for (let i = 0; i < sorted.length; i += 1) {
    if (isV4PlayoffBracketStage(sorted[i])) {
      start = i
      break
    }
  }
  if (start < 0) return null
  let end = start
  while (end + 1 < sorted.length && isV4PlayoffBracketStage(sorted[end + 1])) {
    end += 1
  }
  const block = sorted.slice(start, end + 1)
  if (block.length < 2) return null
  const teamCount = 2 ** block.length
  if (!isV4PlayoffTeamCount(teamCount)) return null
  if (buildV4PlayoffRoundSizes(teamCount).length !== block.length) return null
  return { startIndex: start, stages: block, teamCount }
}

export function inferPlayoffSeedSource(
  firstPlayoffStage: V4PlayoffStageLike | undefined,
  isFirstOverall: boolean
): V4PlayoffSeedSource {
  const code = (firstPlayoffStage?.stageCode || '').toUpperCase()
  if (code.includes('_STD')) return 'STANDING'
  if (code.includes('_TCT')) return 'TEAM_CREATE_TIME'
  if (!isFirstOverall) return 'STANDING'
  return 'TEAM_CREATE_TIME'
}

export interface BuildV4PlayoffStagesInput {
  teamCount: number
  bestOfList?: number[]
  seedSource?: V4PlayoffSeedSource
  startOrder?: number
  existingCodes?: string[]
}

function allocPlayoffStageCode(preferred: string, used: Set<string>): string {
  const upper = preferred.toUpperCase()
  if (!used.has(upper)) {
    used.add(upper)
    return preferred
  }
  let n = 2
  while (used.has(`${upper}_${n}`)) n += 1
  const next = `${preferred}_${n}`
  used.add(next.toUpperCase())
  return next
}

export function buildV4PlayoffStages(input: BuildV4PlayoffStagesInput): V4PlayoffStagePayload[] {
  const teamCount = input.teamCount
  if (!isV4PlayoffTeamCount(teamCount)) return []
  const sizes = buildV4PlayoffRoundSizes(teamCount)
  const startOrder = input.startOrder ?? 1
  const seedSource: V4PlayoffSeedSource = input.seedSource || 'TEAM_CREATE_TIME'
  const used = new Set((input.existingCodes || []).map((c) => c.toUpperCase()))
  const codes: string[] = []

  for (let i = 0; i < sizes.length; i += 1) {
    const size = sizes[i]
    const isFinal = i === sizes.length - 1
    const isFirst = i === 0
    let preferred = isFinal ? 'PLAYOFF_FINAL' : `PLAYOFF_${size}`
    if (isFirst) {
      if (seedSource === 'STANDING') preferred = `${preferred}_STD`
      else if (startOrder > 1) preferred = `${preferred}_TCT`
    }
    codes.push(allocPlayoffStageCode(preferred, used))
  }

  return sizes.map((size, idx) => {
    const isFinal = idx === sizes.length - 1
    return {
      stage: {
        stageCode: codes[idx],
        stageName: formatV4PlayoffStageName(size),
        stageOrder: startOrder + idx,
        stageType: isFinal ? 'FINAL' : 'KNOCKOUT',
        organizationMode: 'UNGROUPED',
        competitionFormat: 'SINGLE_ELIMINATION',
        groupingMode: 'NONE',
        bestOf: normalizeV4PlayoffBestOf(input.bestOfList?.[idx]),
        advancementType: isFinal ? 'NONE' : 'WINNER_ADVANCE',
        pairingRule: 'PLAYOFF_BRACKET',
        nextStageCode: isFinal ? undefined : codes[idx + 1],
        requiresManualConfirmation: 0
      }
    }
  })
}

export interface PlayoffValidateMessage {
  valid: boolean
  messages: string[]
}

export function validateV4PlayoffExpansion(
  prefixStages: V4PlayoffStageLike[],
  playoffStages: V4PlayoffStageLike[],
  maxTeamCount: number
): PlayoffValidateMessage {
  const messages: string[] = []
  const inputCount = resolvePlayoffInputTeamCount(prefixStages, maxTeamCount)
  if (!isV4PlayoffTeamCount(inputCount)) {
    messages.push('当前标准季后赛支持 4 / 8 / 16 支队伍。其它人数后续结合 BYE 处理。')
  }
  const block = findPlayoffBlock([...prefixStages, ...playoffStages])
  if (block && isV4PlayoffTeamCount(inputCount) && block.teamCount !== inputCount) {
    messages.push(`上一阶段晋级 ${inputCount} 支球队，当前季后赛应使用 ${inputCount} 队结构。`)
  }
  const first = playoffStages[0] || block?.stages[0]
  const seed = inferPlayoffSeedSource(first, prefixStages.length === 0)
  if (seed === 'STANDING' && !prefixStages.some(isV4StandingCapableStage)) {
    messages.push('当前无可用积分排名阶段。')
  }
  for (const stage of playoffStages) {
    const bo = stage.bestOf ?? 1
    if (![1, 3, 5].includes(bo)) {
      messages.push(`「${stage.stageName || '季后赛阶段'}」BO 规则仅支持 BO1 / BO3 / BO5`)
    }
  }
  return { valid: messages.length === 0, messages }
}

export interface PlayoffBracketSlot {
  kind: 'seed' | 'winner' | 'champion'
  seedNo?: number
  sourceMatchNo?: number
  label: string
}

export interface PlayoffBracketMatchView {
  matchNo: number
  top: PlayoffBracketSlot
  bottom: PlayoffBracketSlot
  y: number
  label?: string
}

export interface PlayoffBracketRoundView {
  size: number
  name: string
  advance: number
  bestOf: number
  matches: PlayoffBracketMatchView[]
  x: number
}

export interface PlayoffBracketConnector {
  x: number
  y: number
  width: number
  height: number
  type: 'h' | 'v'
}

export interface PlayoffBracketModel {
  teamCount: number
  seedSource: V4PlayoffSeedSource
  seedSourceLabel: string
  rounds: PlayoffBracketRoundView[]
  champion: { x: number; y: number; label: string }
  connectors: PlayoffBracketConnector[]
  width: number
  height: number
  matchHeight: number
  matchWidth: number
}

export const PLAYOFF_BRACKET_LAYOUT = {
  matchHeight: 56,
  matchWidth: 220,
  roundGap: 16,
  columnGap: 80,
  championWidth: 140,
  headerHeight: 68,
  padX: 12,
  padY: 8
}

function matchTop(round: number, index: number, matchH: number, gap: number): number {
  if (round === 0) return index * (matchH + gap)
  const a = matchTop(round - 1, index * 2, matchH, gap)
  const b = matchTop(round - 1, index * 2 + 1, matchH, gap)
  return (a + b) / 2
}

function firstRoundSlots(teamCount: number): PlayoffBracketSlot[] {
  return playoffFirstRoundSeedOrder(teamCount).map((seedNo) => ({
    kind: 'seed' as const,
    seedNo,
    label: `${seedNo}号种子`
  }))
}

function winnerSlot(sourceMatchNo: number): PlayoffBracketSlot {
  return {
    kind: 'winner',
    sourceMatchNo,
    label: `第${sourceMatchNo}场胜者`
  }
}

function roundMatchLabel(roundIndex: number, matchIndex: number, roundCount: number): string | undefined {
  if (roundIndex === roundCount - 2 && roundCount >= 3) {
    return matchIndex === 0 ? '上半区' : '下半区'
  }
  return undefined
}

export function buildPlayoffBracketModel(options: {
  teamCount: number
  bestOfList?: number[]
  roundNames?: string[]
  seedSource?: V4PlayoffSeedSource
}): PlayoffBracketModel | null {
  const teamCount = options.teamCount
  if (!isV4PlayoffTeamCount(teamCount)) return null
  const sizes = buildV4PlayoffRoundSizes(teamCount)
  const { matchHeight, matchWidth, roundGap, columnGap, championWidth, headerHeight, padX, padY } = PLAYOFF_BRACKET_LAYOUT
  const seedSource: V4PlayoffSeedSource = options.seedSource || 'TEAM_CREATE_TIME'
  const firstSlots = firstRoundSlots(teamCount)

  const rounds: PlayoffBracketRoundView[] = sizes.map((size, roundIndex) => {
    const matchCount = size / 2
    const matches: PlayoffBracketMatchView[] = []
    for (let i = 0; i < matchCount; i += 1) {
      const matchNo = i + 1
      let top: PlayoffBracketSlot
      let bottom: PlayoffBracketSlot
      if (roundIndex === 0) {
        top = firstSlots[i * 2]
        bottom = firstSlots[i * 2 + 1]
      } else {
        top = winnerSlot(i * 2 + 1)
        bottom = winnerSlot(i * 2 + 2)
      }
      matches.push({
        matchNo,
        top,
        bottom,
        y: headerHeight + padY + matchTop(roundIndex, i, matchHeight, roundGap),
        label: roundMatchLabel(roundIndex, i, sizes.length)
      })
    }
    return {
      size,
      name: options.roundNames?.[roundIndex] || formatV4PlayoffStageName(size),
      advance: size / 2,
      bestOf: normalizeV4PlayoffBestOf(options.bestOfList?.[roundIndex]),
      matches,
      x: padX + roundIndex * (matchWidth + columnGap)
    }
  })

  const connectors: PlayoffBracketConnector[] = []
  for (let r = 0; r < rounds.length - 1; r += 1) {
    const from = rounds[r]
    const to = rounds[r + 1]
    const xRight = from.x + matchWidth
    const xMid = from.x + matchWidth + columnGap / 2
    const xNext = to.x
    for (let i = 0; i < to.matches.length; i += 1) {
      const a = from.matches[i * 2]
      const b = from.matches[i * 2 + 1]
      const t = to.matches[i]
      const yA = a.y + matchHeight / 2
      const yB = b.y + matchHeight / 2
      const yT = t.y + matchHeight / 2
      const yMin = Math.min(yA, yB)
      const yMax = Math.max(yA, yB)
      connectors.push({ type: 'h', x: xRight, y: yA, width: xMid - xRight, height: 0 })
      connectors.push({ type: 'h', x: xRight, y: yB, width: xMid - xRight, height: 0 })
      connectors.push({ type: 'v', x: xMid, y: yMin, width: 0, height: yMax - yMin })
      connectors.push({ type: 'h', x: xMid, y: yT, width: xNext - xMid, height: 0 })
    }
  }

  const last = rounds[rounds.length - 1]
  const lastMatch = last.matches[0]
  const championX = last.x + matchWidth + columnGap
  const championY = lastMatch.y
  const yC = lastMatch.y + matchHeight / 2
  connectors.push({
    type: 'h',
    x: last.x + matchWidth,
    y: yC,
    width: championX - (last.x + matchWidth),
    height: 0
  })

  const lastRoundBottom = lastMatch.y + matchHeight
  const firstRoundBottom = rounds[0].matches[rounds[0].matches.length - 1].y + matchHeight
  const contentBottom = Math.max(lastRoundBottom, firstRoundBottom)

  return {
    teamCount,
    seedSource,
    seedSourceLabel: formatV4PlayoffSeedSourceLabel(seedSource),
    rounds,
    champion: { x: championX, y: championY, label: '冠军' },
    connectors,
    width: championX + championWidth + padX,
    height: contentBottom + padY + 8,
    matchHeight,
    matchWidth
  }
}

export function playoffFirstRoundSeedLabels(teamCount: number): string[] {
  return playoffFirstRoundSeedOrder(teamCount).map((n) => `${n}号种子`)
}
