<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStageBracket, getV4PublicStageSeries } from '@/api/v4Public'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import {
  groupMatchItemsToRounds,
  groupPublicSeriesToRounds,
  toMirroredBracket,
  type BracketRoundColumn,
} from '../utils/playoffBracketDisplay'
import PlayoffSeriesCard from './PlayoffSeriesCard.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const props = withDefaults(
  defineProps<{
    focusStageId?: string
    title?: string
    titleEn?: string
    emptyText?: string
    hideHeading?: boolean
  }>(),
  {
    title: '淘汰赛对阵图',
    titleEn: 'ELIMINATION BRACKET',
    emptyText: '淘汰赛对阵暂未生成',
    hideHeading: false,
  },
)

const ctx = inject<SeasonContext>('publicSeasonContext')!

const rounds = ref<BracketRoundColumn[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

async function load(stageId?: string) {
  const current = ++reqId
  if (!stageId) {
    rounds.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const seriesRes = await getV4PublicStageSeries(stageId)
    let next = groupPublicSeriesToRounds(seriesRes.data ?? [])

    if (!next.length) {
      const matchRes = await getV4PublicStageBracket(stageId)
      next = groupMatchItemsToRounds(matchRes.data ?? [])
    }

    if (current !== reqId) return
    rounds.value = next
    state.value = next.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    rounds.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载对阵图失败'
  }
}

watch(
  () => props.focusStageId,
  (stageId) => {
    void load(stageId)
  },
  { immediate: true },
)

// halfSize = playoffParticipantCount / 2; membership follows real MatchSeries, not qualificationCount.
const layout = computed(() => toMirroredBracket(rounds.value))

const leagueName = computed(() => {
  const name = ctx.overview.value?.tournamentName || ctx.seasonDetail.value?.seriesName || ''
  return String(name).trim()
})

const boardMinWidth = computed(() => {
  const columns = layout.value.left.length + layout.value.right.length + 1
  const connectors = Math.max(columns - 1, 0)
  return `${columns * 236 + connectors * 34}px`
})

function railKind(fromCount: number, toCount: number): 'merge' | 'line' {
  return fromCount > toCount ? 'merge' : 'line'
}

function railUnits(fromCount: number, toCount: number): number {
  if (fromCount > toCount && toCount > 0) return toCount
  return 1
}

function leftGap(index: number): { from: number; to: number } {
  return {
    from: layout.value.left[index]?.series.length || 0,
    to: layout.value.left[index + 1]?.series.length || 1,
  }
}

function rightGap(index: number): { from: number; to: number } {
  if (index === 0) {
    return { from: 1, to: layout.value.right[0]?.series.length || 1 }
  }
  return {
    from: layout.value.right[index]?.series.length || 0,
    to: layout.value.right[index - 1]?.series.length || 1,
  }
}

function isConfirmedAdvance(item?: { seriesStatus?: string; winnerTeamId?: string } | null): boolean {
  if (!item) return false
  const status = String(item.seriesStatus || '').toUpperCase()
  return status === 'COMPLETED' && Boolean(item.winnerTeamId)
}

function railUnitAdvanced(
  series: Array<{ seriesStatus?: string; winnerTeamId?: string }>,
  unitIndex: number,
  fromCount: number,
  toCount: number,
): boolean {
  if (fromCount > toCount && toCount > 0) {
    const pair = series.slice(unitIndex * 2, unitIndex * 2 + 2)
    return pair.some((item) => isConfirmedAdvance(item))
  }
  return series.some((item) => isConfirmedAdvance(item))
}
</script>

<template>
  <section class="playoff-bracket">
    <header v-if="!hideHeading" class="playoff-bracket__heading">
      <p v-if="leagueName" class="playoff-bracket__league">{{ leagueName }}</p>
      <h3>
        <i aria-hidden="true" />
        {{ title }}
        <i aria-hidden="true" />
      </h3>
      <em v-if="titleEn" class="playoff-bracket__en">{{ titleEn }}</em>
    </header>

    <p v-if="state === 'loading'" class="playoff-bracket__state">加载对阵图...</p>
    <p v-else-if="state === 'error'" class="playoff-bracket__state">{{ errorMessage }}</p>
    <p v-else-if="state === 'empty'" class="playoff-bracket__state">{{ emptyText }}</p>

    <div v-else class="playoff-bracket__viewport">
      <div class="playoff-bracket__board" :style="{ minWidth: boardMinWidth }">
        <div class="bracket-half">
          <div class="bracket-half__cols">
            <template v-for="(column, index) in layout.left" :key="column.key">
              <div class="bracket-round">
                <div class="bracket-round__head">
                  <strong>{{ column.label }}</strong>
                  <em>{{ column.labelEn }}</em>
                </div>
                <div
                  v-for="item in column.series"
                  :key="item.id"
                  class="bracket-round__slot"
                >
                  <PlayoffSeriesCard :series="item" />
                </div>
              </div>
              <div class="bracket-rail" aria-hidden="true">
                <div class="bracket-rail__head" />
                <div class="bracket-rail__body">
                  <i
                    v-for="unit in railUnits(leftGap(index).from, leftGap(index).to)"
                    :key="`${column.key}-u-${unit}`"
                    :class="[
                      'bracket-rail__unit',
                      `bracket-rail__unit--${railKind(leftGap(index).from, leftGap(index).to)}`,
                      {
                        'is-advanced': railUnitAdvanced(
                          column.series,
                          unit - 1,
                          leftGap(index).from,
                          leftGap(index).to,
                        ),
                      },
                    ]"
                  />
                </div>
              </div>
            </template>
          </div>
        </div>

        <div class="bracket-final">
          <div class="bracket-round__head bracket-round__head--final">
            <strong>总决赛</strong>
            <em>FINAL</em>
          </div>
          <div class="bracket-final__body">
            <PlayoffSeriesCard v-if="layout.final" :series="layout.final" emphasize />
          </div>
        </div>

        <div class="bracket-half">
          <div class="bracket-half__cols">
            <template v-for="(column, index) in layout.right" :key="column.key">
              <div class="bracket-rail is-rtl" aria-hidden="true">
                <div class="bracket-rail__head" />
                <div class="bracket-rail__body">
                  <i
                    v-for="unit in railUnits(rightGap(index).from, rightGap(index).to)"
                    :key="`${column.key}-u-${unit}`"
                    :class="[
                      'bracket-rail__unit',
                      `bracket-rail__unit--${railKind(rightGap(index).from, rightGap(index).to)}`,
                      {
                        'is-advanced': railUnitAdvanced(
                          column.series,
                          Math.max(unit - 1, 0),
                          rightGap(index).from,
                          rightGap(index).to,
                        ),
                      },
                    ]"
                  />
                </div>
              </div>
              <div class="bracket-round">
                <div class="bracket-round__head">
                  <strong>{{ column.label }}</strong>
                  <em>{{ column.labelEn }}</em>
                </div>
                <div
                  v-for="item in column.series"
                  :key="item.id"
                  class="bracket-round__slot"
                >
                  <PlayoffSeriesCard :series="item" />
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.playoff-bracket {
  --bracket-line: rgba(17, 17, 17, 0.28);
  --bracket-line-on: var(--cnpa-red, #e10613);
  width: 100%;
}

.playoff-bracket__heading {
  margin: 0 0 8px;
  text-align: center;
}

.playoff-bracket__league {
  margin: 0 0 4px;
  color: rgba(17, 17, 17, 0.42);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.playoff-bracket__en {
  display: block;
  margin-top: 4px;
  color: var(--cnpa-red, #e10613);
  font-size: 11px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.playoff-bracket__heading h3 {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  color: #111;
  font-size: clamp(22px, 1.8vw, 30px);
  font-weight: 900;
  line-height: 1.1;

  i {
    display: block;
    width: 28px;
    height: 1px;
    background: var(--cnpa-red, #e10613);
    opacity: 0.55;
  }
}

.playoff-bracket__state {
  margin: 0;
  padding: 16px 0;
  color: rgba(17, 17, 17, 0.55);
  font-size: 13px;
}

.playoff-bracket__viewport {
  overflow-x: auto;
  overflow-y: hidden;
  overscroll-behavior-x: contain;
  -webkit-overflow-scrolling: touch;
  padding: 10px 2px 8px;
  border: 1px solid rgba(17, 17, 17, 0.06);
  border-radius: 4px;
  background: linear-gradient(180deg, #f8f7f5 0%, #f1efec 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
}

.playoff-bracket__board {
  display: flex;
  align-items: stretch;
  justify-content: center;
  width: max-content;
  max-width: 1320px;
  min-height: 0;
  margin-inline: auto;
  padding: 8px 8px 18px;
}

.bracket-half {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.bracket-half__cols {
  flex: 1;
  display: flex;
  align-items: stretch;
  min-height: 320px;
}

.bracket-round {
  flex: 1 1 228px;
  max-width: 268px;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  padding: 0 6px;
  background: linear-gradient(180deg, rgba(17, 17, 17, 0.008), transparent 48%);
  box-shadow: inset 0 1px 0 rgba(17, 17, 17, 0.04);
}

.bracket-round__head,
.bracket-rail__head {
  min-height: 36px;
  margin-bottom: 0;
}

.bracket-round__head {
  text-align: center;
  padding-top: 6px;

  strong {
    display: block;
    color: #111;
    font-size: 15px;
    font-weight: 800;
    line-height: 1.15;
  }

  em {
    display: block;
    margin-top: 1px;
    color: var(--cnpa-red, #e10613);
    font-size: 10px;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.08em;
  }
}

.bracket-round__head--final {
  padding-top: 4px;

  strong {
    font-size: 17px;
  }
}

.bracket-round__slot {
  flex: 1;
  display: flex;
  align-items: center;
  min-height: 96px;
  padding: 8px 0;
}

.bracket-final {
  position: relative;
  flex: 0 0 292px;
  max-width: 320px;
  display: flex;
  flex-direction: column;
  min-width: 268px;
  padding: 0 8px;
  background: radial-gradient(
    ellipse 58% 36% at 50% 64%,
    rgba(225, 6, 19, 0.016),
    transparent 72%
  );
  box-shadow: inset 0 1px 0 rgba(17, 17, 17, 0.05);
}

.bracket-final__body {
  flex: 1;
  display: flex;
  align-items: center;
}

.bracket-rail {
  flex: 0 0 32px;
  min-width: 32px;
  display: flex;
  flex-direction: column;
}

.bracket-rail__body {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.bracket-rail__unit {
  position: relative;
  flex: 1;
  min-height: 88px;
}

.bracket-rail__unit--line::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1.5px;
  background: var(--bracket-line);
  transform: translateY(-50%);
}

.bracket-rail__unit--merge::before {
  content: '';
  position: absolute;
  top: 25%;
  bottom: 25%;
  left: 0;
  width: 50%;
  border: 1.5px solid var(--bracket-line);
  border-left: 0;
}

.bracket-rail__unit--merge::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  right: 0;
  height: 1.5px;
  background: var(--bracket-line);
  transform: translateY(-50%);
}

.bracket-rail.is-rtl {
  transform: scaleX(-1);
}

.bracket-rail__unit.is-advanced.bracket-rail__unit--merge::before {
  border-color: var(--bracket-line-on);
}

.bracket-rail__unit.is-advanced.bracket-rail__unit--line::after,
.bracket-rail__unit.is-advanced.bracket-rail__unit--merge::after {
  background: var(--bracket-line-on);
}

@media (max-width: 1439px) {
  .bracket-round {
    max-width: 246px;
    min-width: 208px;
    flex-basis: 208px;
  }

  .bracket-final {
    flex-basis: 268px;
    min-width: 248px;
  }
}

@media (max-width: 1279px) {
  .bracket-round {
    max-width: 228px;
    min-width: 198px;
    flex-basis: 198px;
  }

  .bracket-final {
    flex-basis: 252px;
    min-width: 236px;
  }

  .bracket-rail {
    flex-basis: 28px;
    min-width: 28px;
  }
}

@media (max-width: 1024px) {
  .playoff-bracket__viewport {
    padding-inline: 0;
  }
}
</style>
