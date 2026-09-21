<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStageList, getV4PublicStageSeries } from '@/api/v4Public'
import type { V4PublicMatchSeriesDetail, V4PublicStageItem } from '@/api/types/v4Public'
import PaperSection from '@/components/common/PaperSection.vue'
import { displayText } from '@/utils/publicDisplay'
import {
  v4BestOfLabel,
  v4FormatTypeLabel,
  v4StageExecutionStatusLabel,
} from '@/utils/v4Display'
import type { CompetitionStage } from '../types/events'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { countSeriesParticipants, summarizeRoundBestOf } from '../utils/playoffBracketDisplay'

type PaperEdge = 'none' | 'top' | 'bottom' | 'both'
type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const props = withDefaults(
  defineProps<{
    currentStage?: string
    paperEdge?: PaperEdge
    embedded?: boolean
  }>(),
  {
    currentStage: '',
    paperEdge: 'both',
    embedded: false,
  },
)

const ctx = inject<SeasonContext>('publicSeasonContext')!

const apiStages = ref<V4PublicStageItem[]>([])
const seriesByStage = ref<Record<string, V4PublicMatchSeriesDetail[]>>({})
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
let reqId = 0

const activeKey = computed(() => props.currentStage || '')

function isEliminationStage(stage: V4PublicStageItem): boolean {
  const type = `${stage.competitionFormat || ''} ${stage.stageType || ''}`.toUpperCase()
  return type.includes('ELIM') || type.includes('KNOCK') || type.includes('PLAYOFF') || type.includes('FINAL')
}

function mapApiStage(stage: V4PublicStageItem, index: number): CompetitionStage {
  const division = ctx.divisionDetail.value
  const series = seriesByStage.value[String(stage.stageId)] || []
  const statusLabel = v4StageExecutionStatusLabel(stage.executionStatus, stage.executionStatusName)
  const fields = [
    { label: '赛制', value: displayText(stage.competitionFormatName || v4FormatTypeLabel(stage.competitionFormat)) },
    { label: '阶段状态', value: statusLabel },
  ]
  const playoffTeams = countSeriesParticipants(series)
  const teamCount = playoffTeams || division?.eventTeamCount
  if (teamCount != null && teamCount > 0) {
    fields.push({ label: '参赛队伍', value: `${teamCount}支` })
  }
  const roundLines = summarizeRoundBestOf(series)
  if (roundLines.length) {
    for (const line of roundLines) {
      fields.push({ label: line.label, value: line.bestOfLabel })
    }
  } else if (!isEliminationStage(stage) && stage.bestOf && stage.bestOf > 0) {
    fields.push({ label: '比赛规则', value: v4BestOfLabel(stage.bestOf) })
  }
  if (stage.advancementCount && stage.advancementCount > 0) {
    fields.push({ label: '晋级名额', value: `${stage.advancementCount}支` })
  }
  return {
    key: String(stage.stageId || `stage-${index + 1}`),
    index: String((stage.stageOrder ?? index + 1)).padStart(2, '0'),
    name: displayText(stage.stageName || stage.stageTypeName, '阶段'),
    period: statusLabel,
    statusCode: String(stage.executionStatus || '').toUpperCase(),
    fields,
  }
}

const competitionStages = computed<CompetitionStage[]>(() => apiStages.value.map(mapApiStage))

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    apiStages.value = []
    seriesByStage.value = {}
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  try {
    const res = await getV4PublicStageList(divisionId)
    const stages = res.data ?? []
    const knockout = stages.filter(isEliminationStage)
    const buckets = await Promise.all(
      knockout.map(async (stage) => {
        const seriesRes = await getV4PublicStageSeries(stage.stageId)
        return [String(stage.stageId), seriesRes.data ?? []] as const
      }),
    )
    if (current !== reqId) return
    apiStages.value = stages
    seriesByStage.value = Object.fromEntries(buckets)
    state.value = 'success'
  } catch {
    if (current !== reqId) return
    apiStages.value = []
    seriesByStage.value = {}
    state.value = 'error'
  }
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void load(divisionId)
  },
  { immediate: true },
)
</script>
<template>
  <div v-if="embedded" class="competition-format competition-format--embedded">
    <div class="competition-format__inner">
      <div class="competition-format__header">
        <div class="section-head-group">
          <h2 class="cnpa-paper-section__title">赛事详情</h2>
          <span class="cnpa-paper-section__subtitle">Competition Format</span>
        </div>
      </div>

      <div class="competition-format__body">
        <div class="stage-steps">
          <div
            v-for="(stage, index) in competitionStages"
            :key="stage.key"
            class="stage-step"
            :class="{
              active: stage.key === activeKey,
              'is-done': stage.statusCode === 'COMPLETED',
            }"
          >
            <span class="step-index">{{ stage.index }}</span>
            <span class="step-name">{{ stage.name }}</span>
            <span v-if="index < competitionStages.length - 1" class="step-line" aria-hidden="true" />
          </div>
        </div>

        <div class="stage-cards competition-format__grid">
          <article
            v-for="stage in competitionStages"
            :key="stage.key"
            class="stage-card"
            :class="{ active: stage.key === activeKey }"
          >
            <header>
              <h3>
                <span class="stage-card__index">{{ stage.index }}</span>
                {{ stage.name }}
              </h3>
              <span>{{ stage.period }}</span>
            </header>
            <dl>
              <div v-for="field in stage.fields" :key="field.label" class="field-row">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </div>
    </div>
  </div>

  <div v-else class="site-shell competition-format-shell">
    <PaperSection :edge="paperEdge" tone="gray" class="competition-format">
      <div class="competition-format__inner">
        <div class="competition-format__header">
          <div class="section-head-group">
            <h2 class="cnpa-paper-section__title">赛事详情</h2>
            <span class="cnpa-paper-section__subtitle">Competition Format</span>
          </div>
        </div>

        <div class="competition-format__body">
          <div class="stage-steps">
            <div
              v-for="(stage, index) in competitionStages"
              :key="stage.key"
              class="stage-step"
              :class="{
                active: stage.key === activeKey,
                'is-done': stage.statusCode === 'COMPLETED',
              }"
            >
              <span class="step-index">{{ stage.index }}</span>
              <span class="step-name">{{ stage.name }}</span>
              <span v-if="index < competitionStages.length - 1" class="step-line" aria-hidden="true" />
            </div>
          </div>

          <div class="stage-cards competition-format__grid">
            <article
              v-for="stage in competitionStages"
              :key="stage.key"
              class="stage-card"
              :class="{ active: stage.key === activeKey }"
            >
              <header>
                <h3>
                  <span class="stage-card__index">{{ stage.index }}</span>
                  {{ stage.name }}
                </h3>
                <span>{{ stage.period }}</span>
              </header>
              <dl>
                <div v-for="field in stage.fields" :key="field.label" class="field-row">
                  <dt>{{ field.label }}</dt>
                  <dd>{{ field.value }}</dd>
                </div>
              </dl>
            </article>
          </div>
        </div>
      </div>
    </PaperSection>
  </div>
</template>
<style scoped lang="scss">
.competition-format-shell {
  margin-top: 0;
}

.competition-format {
  margin-top: 0;
}

.competition-format__inner {
  padding:
    clamp(20px, 1.8vw, 32px)
    var(--cnpa-public-content-gutter)
    clamp(24px, 2.2vw, 36px);
}

.competition-format--embedded .competition-format__inner {
  padding-inline: 0;
  padding-top: clamp(6px, 0.8vw, 12px);
}

.competition-format__header {
  margin-bottom: 14px;
}

.competition-format__body {
  max-width: 760px;
  margin-inline: auto;
}

.stage-steps {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 8px;
  margin-bottom: 14px;
}

.stage-step {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 2px 0;
  color: rgba(17, 17, 17, 0.42);

  &.is-done {
    color: rgba(17, 17, 17, 0.48);
  }

  &.active {
    color: var(--cnpa-paper-text);

    .step-index {
      border-color: var(--cnpa-red);
      color: var(--cnpa-red);
    }

    .step-line {
      background: rgba(225, 6, 19, 0.4);
    }
  }
}

.step-index {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(17, 17, 17, 0.18);
  border-radius: 50%;
  font-size: 12px;
  font-weight: 900;
}

.step-name {
  font-size: 14px;
  font-weight: 800;
}

.step-line {
  width: 36px;
  height: 1px;
  margin: 0 6px;
  background: rgba(17, 17, 17, 0.16);
}

.competition-format__grid,
.stage-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 360px));
  justify-content: center;
  gap: 12px;
}

.stage-card {
  padding: 12px 14px 10px;
  border: 1px solid #dedede;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 1px 0 rgba(17, 17, 17, 0.03);

  &.active {
    border-color: var(--cnpa-red, #e10613);
    box-shadow: 0 4px 12px rgba(225, 6, 19, 0.06);
  }

  header {
    margin-bottom: 6px;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 2px;
      font-size: 17px;
      color: var(--cnpa-paper-text);
    }

    span {
      font-size: 12px;
      color: var(--cnpa-paper-text-muted);
    }
  }
}

.stage-card__index {
  font-size: 14px;
  font-weight: 900;
  color: var(--cnpa-red);
}

.field-row {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 10px;
  align-items: baseline;
  padding: 4px 0;
  border-bottom: 1px solid rgba(17, 17, 17, 0.07);

  dt {
    margin: 0;
    font-size: 12px;
    color: var(--cnpa-paper-text-muted);
  }

  dd {
    margin: 0;
    font-size: 13px;
    font-weight: 700;
    color: var(--cnpa-paper-text);
  }
}

@media (max-width: 992px) {
  .competition-format__grid,
  .stage-cards {
    grid-template-columns: minmax(0, 1fr);
  }

  .stage-steps {
    flex-direction: column;
    align-items: flex-start;
  }

  .step-line {
    display: none;
  }
}

@media (max-width: 768px) {
  .competition-format:not(.competition-format--embedded) .competition-format__inner {
    padding-left: var(--cnpa-public-content-gutter);
    padding-right: var(--cnpa-public-content-gutter);
  }
}
</style>
