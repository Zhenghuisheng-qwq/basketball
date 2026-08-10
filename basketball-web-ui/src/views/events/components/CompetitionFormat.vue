<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStageList } from '@/api/v4Public'
import type { V4PublicStageItem } from '@/api/types/v4Public'
import PaperSection from '@/components/common/PaperSection.vue'
import { displayText } from '@/utils/publicDisplay'
import { v4FormatTypeLabel, v4PlatformLabel } from '@/utils/v4Display'
import type { CompetitionStage } from '../types/events'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'

type PaperEdge = 'none' | 'top' | 'bottom' | 'both'
type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const props = withDefaults(
  defineProps<{
    currentStage?: string
    paperEdge?: PaperEdge
    embedded?: boolean
  }>(),
  {
    currentStage: 'regular',
    paperEdge: 'both',
    embedded: false,
  },
)

const ctx = inject<SeasonContext>('publicSeasonContext')!

const apiStages = ref<V4PublicStageItem[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
let reqId = 0

const activeKey = computed(() => props.currentStage || 'regular')

function mapStageKey(stage: V4PublicStageItem, index: number): string {
  const type = (stage.stageType || stage.competitionFormat || '').toUpperCase()
  if (type.includes('FINAL')) return 'finals'
  if (type.includes('ELIM') || type.includes('PLAYOFF') || type.includes('KNOCK')) return 'playoffs'
  if (type.includes('ROUND') || type.includes('REGULAR') || type.includes('GROUP')) return 'regular'
  return ['regular', 'playoffs', 'finals'][index] || `stage-${index + 1}`
}

function buildFallbackStages(): CompetitionStage[] {
  const division = ctx.divisionDetail.value
  const teamCount = division?.eventTeamCount ?? division?.maxTeamCount
  const formatLabel = v4FormatTypeLabel(division?.formatType)
  const platformLabel = v4PlatformLabel(division?.platformCode)
  const genericFields = [
    { label: '参赛队伍', value: teamCount != null ? `${teamCount} 支` : '--' },
    { label: '赛制', value: displayText(formatLabel) },
    { label: '比赛平台', value: displayText(platformLabel) },
  ]

  return [
    {
      key: 'regular',
      index: '01',
      name: '常规赛',
      period: displayText(division?.matchStartTime?.slice(0, 10)),
      fields: genericFields,
    },
    {
      key: 'playoffs',
      index: '02',
      name: '季后赛',
      period: '--',
      fields: [
        { label: '晋级规则', value: '常规赛排名前列战队晋级' },
        { label: '赛制', value: displayText(formatLabel) },
      ],
    },
    {
      key: 'finals',
      index: '03',
      name: '总决赛',
      period: displayText(division?.matchEndTime?.slice(0, 10)),
      fields: [
        { label: '冠军归属', value: '季后赛最终胜者' },
        { label: '比赛平台', value: displayText(platformLabel) },
      ],
    },
  ]
}

function mapApiStage(stage: V4PublicStageItem, index: number): CompetitionStage {
  const division = ctx.divisionDetail.value
  return {
    key: mapStageKey(stage, index),
    index: String((stage.stageOrder ?? index + 1)).padStart(2, '0'),
    name: displayText(stage.stageName || stage.stageTypeName, '阶段'),
    period: displayText(stage.executionStatusName),
    fields: [
      { label: '赛制', value: displayText(stage.competitionFormatName || v4FormatTypeLabel(stage.competitionFormat)) },
      { label: '阶段状态', value: displayText(stage.executionStatusName) },
      {
        label: '参赛队伍',
        value:
          division?.eventTeamCount != null
            ? `${division.eventTeamCount} 支`
            : '--',
      },
    ],
  }
}

const competitionStages = computed<CompetitionStage[]>(() => {
  if (apiStages.value.length) {
    return apiStages.value.map(mapApiStage)
  }
  return buildFallbackStages()
})

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    apiStages.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  try {
    const res = await getV4PublicStageList(divisionId)
    if (current !== reqId) return
    apiStages.value = res.data ?? []
    state.value = 'success'
  } catch {
    if (current !== reqId) return
    apiStages.value = []
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

      <div class="stage-steps">
        <div
          v-for="(stage, index) in competitionStages"
          :key="stage.key"
          class="stage-step"
          :class="{ active: stage.key === activeKey }"
        >
          <span class="step-index">{{ stage.index }}</span>
          <span class="step-name">{{ stage.name }}</span>
          <span v-if="index < competitionStages.length - 1" class="stage-line" aria-hidden="true" />
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

  <div v-else class="site-shell competition-format-shell">
    <PaperSection :edge="paperEdge" tone="gray" class="competition-format">
      <div class="competition-format__inner">
        <div class="competition-format__header">
          <div class="section-head-group">
            <h2 class="cnpa-paper-section__title">赛事详情</h2>
            <span class="cnpa-paper-section__subtitle">Competition Format</span>
          </div>
        </div>

        <div class="stage-steps">
          <div
            v-for="(stage, index) in competitionStages"
            :key="stage.key"
            class="stage-step"
            :class="{ active: stage.key === activeKey }"
          >
            <span class="step-index">{{ stage.index }}</span>
            <span class="step-name">{{ stage.name }}</span>
            <span v-if="index < competitionStages.length - 1" class="stage-line" aria-hidden="true" />
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
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(32px, 2.8vw, 48px);
}

.competition-format--embedded .competition-format__inner {
  padding-top: clamp(20px, 2vw, 28px);
}

.competition-format__header {
  margin-bottom: 24px;
}

.stage-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 28px;
}

.stage-step {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  color: var(--cnpa-paper-text-muted);

  &.active {
    color: var(--cnpa-paper-text);

    .step-index {
      border-color: var(--cnpa-red);
      color: var(--cnpa-red);
    }

    .step-line {
      background: rgba(225, 6, 19, 0.28);

      &::after {
        border-left-color: var(--cnpa-red);
      }
    }
  }
}

.step-index {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(17, 17, 17, 0.18);
  border-radius: 50%;
  font-size: 13px;
  font-weight: 900;
}

.step-name {
  font-size: 15px;
  font-weight: 800;
}

.step-line {
  position: absolute;
  right: -6px;
  top: 50%;
  width: calc(100% - 120px);
  max-width: 120px;
  height: 1px;
  background: rgba(17, 17, 17, 0.14);
  transform: translate(100%, -50%);

  &::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    border-top: 4px solid transparent;
    border-bottom: 4px solid transparent;
    border-left: 6px solid rgba(17, 17, 17, 0.28);
  }
}

.competition-format__grid,
.stage-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stage-card {
  padding: 24px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.58);
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;

  &.active {
    border-color: rgba(225, 6, 19, 0.42);
    box-shadow: 0 10px 28px rgba(225, 6, 19, 0.08);
  }

  header {
    margin-bottom: 18px;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 6px;
      font-size: 20px;
      color: var(--cnpa-paper-text);
    }

    span {
      font-size: 13px;
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
  grid-template-columns: 92px 1fr;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(17, 17, 17, 0.08);

  dt {
    margin: 0;
    font-size: 13px;
    color: var(--cnpa-paper-text-muted);
  }

  dd {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--cnpa-paper-text);
  }
}

@media (max-width: 992px) {
  .competition-format__grid,
  .stage-cards,
  .stage-steps {
    grid-template-columns: 1fr;
  }

  .step-line {
    display: none;
  }
}

@media (max-width: 768px) {
  .competition-format__inner {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>