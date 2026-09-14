import assert from 'node:assert/strict'
import {
  isBracketStage,
  pickDefaultStage,
  resolveStageRendererKind,
  stageEmptyText,
  stageHeading,
  stageSelectorLabel,
  type StageLike,
} from './stageRenderer'

function stage(partial: Partial<StageLike>): StageLike {
  return {
    stageId: partial.stageId || '1',
    stageCode: partial.stageCode,
    stageName: partial.stageName,
    stageType: partial.stageType,
    organizationMode: partial.organizationMode,
    competitionFormat: partial.competitionFormat,
    executionStatus: partial.executionStatus,
    currentStage: partial.currentStage,
  }
}

assert.equal(
  resolveStageRendererKind(stage({
    stageCode: 'REGULAR',
    stageType: 'REGULAR',
    organizationMode: 'UNGROUPED',
    competitionFormat: 'ROUND_ROBIN',
  })),
  'ROUND_ROBIN',
)

assert.equal(
  resolveStageRendererKind(stage({
    stageCode: 'GROUP',
    stageType: 'GROUP',
    organizationMode: 'GROUPED',
    competitionFormat: 'ROUND_ROBIN',
  })),
  'GROUP_STAGE',
)

assert.equal(
  resolveStageRendererKind(stage({
    stageCode: 'KNOCKOUT',
    stageType: 'KNOCKOUT',
    organizationMode: 'UNGROUPED',
    competitionFormat: 'SINGLE_ELIMINATION',
  })),
  'SINGLE_ELIMINATION',
)

assert.equal(
  resolveStageRendererKind(stage({
    stageCode: 'PLAYOFF',
    stageType: 'KNOCKOUT',
    organizationMode: 'UNGROUPED',
    competitionFormat: 'SINGLE_ELIMINATION',
  })),
  'PLAYOFF',
)

assert.equal(
  resolveStageRendererKind(stage({
    stageCode: 'PLAYOFF_16',
    stageType: 'KNOCKOUT',
    competitionFormat: 'SINGLE_ELIMINATION',
  })),
  'PLAYOFF',
)

assert.equal(isBracketStage(stage({ stageCode: 'REGULAR', competitionFormat: 'ROUND_ROBIN' })), false)
assert.equal(isBracketStage(stage({ stageCode: 'KNOCKOUT', competitionFormat: 'SINGLE_ELIMINATION' })), true)

assert.equal(stageSelectorLabel(stage({ stageName: '淘汰赛', stageCode: 'KNOCKOUT' })), '淘汰赛')
assert.equal(stageSelectorLabel(stage({ stageCode: 'PLAYOFF', stageType: 'KNOCKOUT' })), '季后赛')
assert.deepEqual(stageHeading(stage({ stageCode: 'KNOCKOUT', competitionFormat: 'SINGLE_ELIMINATION' })), {
  title: '淘汰赛对阵图',
  titleEn: 'ELIMINATION BRACKET',
})
assert.deepEqual(stageHeading(stage({ stageCode: 'PLAYOFF', competitionFormat: 'SINGLE_ELIMINATION' })), {
  title: '季后赛对阵图',
  titleEn: 'PLAYOFF BRACKET',
})
assert.equal(stageEmptyText(stage({ stageCode: 'KNOCKOUT' })), '淘汰赛对阵暂未生成')
assert.equal(stageEmptyText(stage({ stageCode: 'PLAYOFF' })), '季后赛对阵暂未生成')
assert.equal(stageEmptyText(stage({ stageCode: 'GROUP', organizationMode: 'GROUPED' })), '小组赛数据暂未生成')

const league = [
  stage({ stageId: 'r', stageCode: 'REGULAR', executionStatus: 'COMPLETED' }),
  stage({ stageId: 'p', stageCode: 'PLAYOFF', executionStatus: 'RUNNING', currentStage: true }),
]
assert.equal(pickDefaultStage(league)?.stageId, 'p')

const completed = [
  stage({ stageId: 'g', stageCode: 'GROUP', executionStatus: 'COMPLETED' }),
  stage({ stageId: 'k', stageCode: 'KNOCKOUT', executionStatus: 'COMPLETED' }),
]
assert.equal(pickDefaultStage(completed)?.stageId, 'k')

const onlyKnockout = [stage({ stageId: 'k', stageCode: 'KNOCKOUT', executionStatus: 'DRAFT' })]
assert.equal(pickDefaultStage(onlyKnockout)?.stageId, 'k')
