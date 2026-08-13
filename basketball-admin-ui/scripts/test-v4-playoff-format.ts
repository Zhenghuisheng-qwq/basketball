import {
  buildPlayoffBracketModel,
  buildV4PlayoffStages,
  findPlayoffBlock,
  inferPlayoffSeedSource,
  isV4PlayoffTeamCount,
  playoffFirstRoundSeedLabels,
  playoffFirstRoundSeedOrder,
  resolvePlayoffInputTeamCount,
  validateV4PlayoffExpansion
} from '../src/utils/v4PlayoffFormat.ts'

let failed = 0

function assert(cond: unknown, msg: string) {
  if (!cond) {
    failed += 1
    console.error('FAIL:', msg)
  } else {
    console.log('PASS:', msg)
  }
}

function assertEq<T>(actual: T, expected: T, msg: string) {
  const ok = JSON.stringify(actual) === JSON.stringify(expected)
  assert(ok, `${msg} expected=${JSON.stringify(expected)} actual=${JSON.stringify(actual)}`)
}

assert(isV4PlayoffTeamCount(4) && isV4PlayoffTeamCount(8) && isV4PlayoffTeamCount(16), 'supported counts')
assert(!isV4PlayoffTeamCount(6) && !isV4PlayoffTeamCount(12), 'unsupported counts')

assertEq(playoffFirstRoundSeedOrder(4), [1, 4, 2, 3], '4-team seed order')
assertEq(playoffFirstRoundSeedOrder(8), [1, 8, 4, 5, 2, 7, 3, 6], '8-team seed order')
assertEq(
  playoffFirstRoundSeedOrder(16),
  [1, 16, 4, 13, 5, 12, 8, 9, 6, 11, 7, 10, 2, 15, 3, 14],
  '16-team seed order'
)

const four = buildV4PlayoffStages({ teamCount: 4, bestOfList: [1, 3] })
assertEq(four.length, 2, '4 teams -> 2 stages')
assertEq(four.map((s) => s.stage.stageName), ['4强赛', '总决赛'], '4-team names')
assertEq(four.map((s) => s.stage.bestOf), [1, 3], '4-team BO')
assertEq(four[0].stage.advancementType, 'WINNER_ADVANCE', '4-team first WINNER_ADVANCE')
assertEq(four[1].stage.advancementType, 'NONE', '4-team final NONE')
assert(four.every((s) => s.stage.pairingRule === 'PLAYOFF_BRACKET'), '4-team PLAYOFF_BRACKET')

const eight = buildV4PlayoffStages({ teamCount: 8 })
assertEq(eight.length, 3, '8 teams -> 3 stages')
assertEq(eight.map((s) => s.stage.stageName), ['8强赛', '4强赛', '总决赛'], '8-team names')

const sixteen = buildV4PlayoffStages({
  teamCount: 16,
  bestOfList: [1, 3, 5, 3],
  seedSource: 'TEAM_CREATE_TIME'
})
assertEq(sixteen.length, 4, '16 teams -> 4 stages')
assertEq(sixteen.map((s) => s.stage.stageName), ['16强赛', '8强赛', '4强赛', '总决赛'], '16-team names')
assertEq(sixteen.map((s) => s.stage.bestOf), [1, 3, 5, 3], '16-team BO 1/3/5/3')
assertEq(sixteen[0].stage.advancementType, 'WINNER_ADVANCE', 'stage1 WINNER_ADVANCE')
assertEq(sixteen[1].stage.advancementType, 'WINNER_ADVANCE', 'stage2 WINNER_ADVANCE')
assertEq(sixteen[2].stage.advancementType, 'WINNER_ADVANCE', 'stage3 WINNER_ADVANCE')
assertEq(sixteen[3].stage.advancementType, 'NONE', 'stage4 NONE')
assertEq(sixteen[0].stage.pairingRule, 'PLAYOFF_BRACKET', 'first pairing')
assert(sixteen.slice(1).every((s) => s.stage.pairingRule === 'PLAYOFF_BRACKET'), 'later pairing inherit')

const rrThenPlayoff = buildV4PlayoffStages({
  teamCount: 8,
  seedSource: 'STANDING',
  startOrder: 2,
  existingCodes: ['REGULAR']
})
assertEq(rrThenPlayoff.length, 3, 'RR -> playoff 8 teams 3 stages')
assert(rrThenPlayoff[0].stage.stageCode.includes('_STD'), 'STANDING suffix on first playoff stage')
assertEq(inferPlayoffSeedSource(rrThenPlayoff[0].stage, false), 'STANDING', 'infer STANDING')

const pure = buildV4PlayoffStages({ teamCount: 16, seedSource: 'TEAM_CREATE_TIME', startOrder: 1 })
assertEq(inferPlayoffSeedSource(pure[0].stage, true), 'TEAM_CREATE_TIME', 'pure playoff TEAM_CREATE_TIME')

assertEq(resolvePlayoffInputTeamCount([{ advancementType: 'OVERALL_TOP_N', advancementCount: 8 }], 16), 8, 'RR advance 8')
assertEq(resolvePlayoffInputTeamCount([{ advancementType: 'GROUP_TOP_N', groupCount: 4, advancementCount: 2 }], 16), 8, 'group 4x2=8')

const mismatch = validateV4PlayoffExpansion(
  [{ advancementType: 'OVERALL_TOP_N', advancementCount: 8, competitionFormat: 'ROUND_ROBIN' }],
  sixteen.map((s) => s.stage),
  16
)
assert(!mismatch.valid, '8 vs 16 mismatch invalid')
assert(mismatch.messages.some((m) => m.includes('8')), 'mismatch mentions 8')

const unsupported = validateV4PlayoffExpansion([], [], 12)
assert(!unsupported.valid, '12 teams unsupported')

const standingMissing = validateV4PlayoffExpansion([], sixteen.map((s) => ({ ...s.stage, stageCode: 'PLAYOFF_16_STD' })), 16)
assert(!standingMissing.valid, 'STANDING without prefix invalid')

const block = findPlayoffBlock(sixteen.map((s) => s.stage))
assert(block?.teamCount === 16 && block.stages.length === 4, 'detect 16-team playoff block')

const labels = playoffFirstRoundSeedLabels(16)
;[
  '1号种子', '16号种子', '4号种子', '13号种子', '5号种子', '12号种子',
  '8号种子', '9号种子', '6号种子', '11号种子', '7号种子', '10号种子',
  '2号种子', '15号种子', '3号种子', '14号种子'
].forEach((label, i) => assertEq(labels[i], label, `seed label ${i}`))

const model = buildPlayoffBracketModel({ teamCount: 16, bestOfList: [1, 1, 3, 5] })
assert(!!model, '16 bracket model')
assertEq(model!.rounds.length, 4, '16 bracket 4 columns')
assertEq(model!.rounds[0].matches.length, 8, '16 first round 8 matches')
assertEq(model!.rounds[0].matches[0].top.seedNo, 1, 'S1 1')
assertEq(model!.rounds[0].matches[0].bottom.seedNo, 16, 'S1 16')
assertEq(model!.rounds[0].matches[1].top.seedNo, 4, 'S2 4')
assertEq(model!.rounds[0].matches[1].bottom.seedNo, 13, 'S2 13')
assertEq(model!.rounds[2].bestOf, 3, 'semi BO3 live')
assertEq(model!.rounds[3].bestOf, 5, 'final BO5 live')
assert(model!.connectors.length > 0, 'connectors exist')

const model8 = buildPlayoffBracketModel({ teamCount: 8 })
assertEq(model8!.rounds.length, 3, '8 bracket 3 columns')
assertEq(model8!.rounds[0].matches[0].top.seedNo, 1, '8 S1 1')
assertEq(model8!.rounds[0].matches[0].bottom.seedNo, 8, '8 S1 8')

const model4 = buildPlayoffBracketModel({ teamCount: 4 })
assertEq(model4!.rounds.length, 2, '4 bracket 2 columns')
assertEq(model4!.rounds[0].matches[0].top.seedNo, 1, '4 S1 1')
assertEq(model4!.rounds[0].matches[0].bottom.seedNo, 4, '4 S1 4')
assertEq(model4!.rounds[0].matches[1].top.seedNo, 2, '4 S2 2')
assertEq(model4!.rounds[0].matches[1].bottom.seedNo, 3, '4 S2 3')

if (failed) {
  console.error(`\n${failed} failed`)
  process.exit(1)
}
console.log('\nAll playoff format tests passed')
