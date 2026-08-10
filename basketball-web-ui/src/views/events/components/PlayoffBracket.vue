<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStageBracket, getV4PublicStageList } from '@/api/v4Public'
import type { V4PublicStageItem } from '@/api/types/v4Public'
import type { PlayoffSeries } from '../types/events'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { v4BracketToPlayoffSeries, type BracketStageBucket } from '../utils/v4PublicAdapters'
import BracketLineConnector from './BracketLineConnector.vue'
import BracketMergeConnector from './BracketMergeConnector.vue'
import PlayoffSeriesCard from './PlayoffSeriesCard.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

defineProps<{
  /** 当前选中的淘汰阶段；用于标题提示，Bracket 仍合并全部淘汰 Stage */
  focusStageId?: string
}>()

const ctx = inject<SeasonContext>('publicSeasonContext')!

const playoffSeries = ref<PlayoffSeries[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

function isEliminationStage(stage: V4PublicStageItem): boolean {
  const type = `${stage.competitionFormat || ''} ${stage.stageType || ''}`.toUpperCase()
  return type.includes('ELIM') || type.includes('KNOCK') || type.includes('PLAYOFF') || type.includes('FINAL')
}

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    playoffSeries.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const stageRes = await getV4PublicStageList(divisionId)
    const stages = [...(stageRes.data ?? [])]
      .filter(isEliminationStage)
      .sort((a, b) => (a.stageOrder ?? 0) - (b.stageOrder ?? 0))
    if (!stages.length) {
      if (current !== reqId) return
      playoffSeries.value = []
      state.value = 'empty'
      return
    }
    const buckets: BracketStageBucket[] = await Promise.all(
      stages.map(async (stage) => {
        const bracketRes = await getV4PublicStageBracket(stage.stageId)
        return {
          stageId: stage.stageId,
          stageOrder: stage.stageOrder,
          stageName: stage.stageName,
          matches: bracketRes.data ?? [],
        }
      }),
    )
    if (current !== reqId) return
    playoffSeries.value = v4BracketToPlayoffSeries(buckets)
    state.value = playoffSeries.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    playoffSeries.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载对阵图失败'
  }
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void load(divisionId)
  },
  { immediate: true },
)

const leftQuarter = computed(() =>
  playoffSeries.value.filter((item) => item.round === 'quarterfinal' && item.side === 'left'),
)
const rightQuarter = computed(() =>
  playoffSeries.value.filter((item) => item.round === 'quarterfinal' && item.side === 'right'),
)
const leftSemi = computed(() =>
  playoffSeries.value.find((item) => item.round === 'semifinal' && item.side === 'left'),
)
const rightSemi = computed(() =>
  playoffSeries.value.find((item) => item.round === 'semifinal' && item.side === 'right'),
)
const finalSeries = computed(() => playoffSeries.value.find((item) => item.round === 'final'))
</script>
<template>
  <section class="playoff-bracket">
    <div class="playoff-bracket__heading">
      <span class="playoff-bracket__league">CNPA LEAGUE</span>
      <h3 class="playoff-bracket__title">
        <i aria-hidden="true" />
        季后赛对阵图
        <i aria-hidden="true" />
      </h3>
    </div>

    <p v-if="state === 'loading'" class="playoff-bracket__scroll-hint" style="display: block">加载对阵图...</p>
    <p v-else-if="state === 'error'" class="playoff-bracket__scroll-hint" style="display: block">{{ errorMessage }}</p>
    <p v-else-if="state === 'empty'" class="playoff-bracket__scroll-hint" style="display: block">暂无淘汰赛对阵</p>
    <p v-else class="playoff-bracket__scroll-hint">左右滑动查看完整对阵</p>

    <div v-if="state === 'success'" class="playoff-bracket__viewport">
      <div class="playoff-bracket__board">
        <div class="playoff-bracket__round playoff-bracket__round--quarter">
          <div v-if="leftQuarter[0]" class="bracket-match bracket-match--row-top">
            <PlayoffSeriesCard :series="leftQuarter[0]" />
          </div>
          <div v-if="leftQuarter[1]" class="bracket-match bracket-match--row-bottom">
            <PlayoffSeriesCard :series="leftQuarter[1]" />
          </div>
        </div>

        <BracketMergeConnector direction="ltr" />

        <div class="playoff-bracket__round playoff-bracket__round--semifinal">
          <div v-if="leftSemi" class="bracket-match bracket-match--row-mid">
            <PlayoffSeriesCard :series="leftSemi" variant="semifinal" />
          </div>
        </div>

        <BracketLineConnector direction="ltr" />

        <div class="playoff-bracket__round playoff-bracket__round--final">
          <div v-if="finalSeries" class="bracket-match bracket-match--row-mid">
            <PlayoffSeriesCard :series="finalSeries" variant="final" />
          </div>
        </div>

        <BracketLineConnector direction="ltr" />

        <div class="playoff-bracket__round playoff-bracket__round--semifinal">
          <div v-if="rightSemi" class="bracket-match bracket-match--row-mid">
            <PlayoffSeriesCard :series="rightSemi" variant="semifinal" />
          </div>
        </div>

        <BracketMergeConnector direction="rtl" />

        <div class="playoff-bracket__round playoff-bracket__round--quarter">
          <div v-if="rightQuarter[0]" class="bracket-match bracket-match--row-top">
            <PlayoffSeriesCard :series="rightQuarter[0]" />
          </div>
          <div v-if="rightQuarter[1]" class="bracket-match bracket-match--row-bottom">
            <PlayoffSeriesCard :series="rightQuarter[1]" />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>
<style scoped lang="scss">
.playoff-bracket {
  --bracket-line-color: rgba(225, 6, 19, 0.78);
  --bracket-line-width: 1px;
  --round-one-width: clamp(210px, 16vw, 250px);
  --semifinal-width: clamp(185px, 14vw, 220px);
  --final-width: clamp(220px, 17vw, 260px);
  --merge-line-width: clamp(24px, 2.4vw, 38px);
  --final-line-width: clamp(24px, 2.6vw, 42px);
  width: 100%;
}

.playoff-bracket__heading {
  margin: 6px 0 clamp(14px, 1.2vw, 22px);
  text-align: center;
}

.playoff-bracket__league {
  display: block;
  color: #202020;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.playoff-bracket__title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 6px 0 0;
  color: #111111;
  font-size: clamp(26px, 2vw, 36px);
  font-weight: 900;
  line-height: 1.1;

  i {
    width: clamp(18px, 1.6vw, 28px);
    height: 2px;
    background: #e10613;
    transform: rotate(-18deg);
    border-radius: 1px;

    &:last-child {
      transform: rotate(18deg);
    }
  }
}

.playoff-bracket__scroll-hint {
  display: none;
  margin: 0 0 10px;
  color: rgba(17, 17, 17, 0.52);
  font-size: 12px;
  text-align: center;
}

.playoff-bracket__viewport {
  position: relative;
  width: 100%;
  margin-top: 0;
}

.playoff-bracket__board {
  display: grid;
  grid-template-columns:
    var(--round-one-width)
    var(--merge-line-width)
    var(--semifinal-width)
    var(--final-line-width)
    var(--final-width)
    var(--final-line-width)
    var(--semifinal-width)
    var(--merge-line-width)
    var(--round-one-width);
  grid-template-rows:
    minmax(76px, auto)
    minmax(24px, 1fr)
    minmax(76px, auto);
  align-items: stretch;
  justify-content: center;
  width: 100%;
  max-width: 1280px;
  height: clamp(360px, 31vw, 420px);
  min-height: 360px;
  margin: 0 auto;
}

.playoff-bracket__round {
  display: grid;
  grid-template-rows: subgrid;
  grid-row: 1 / -1;
  min-height: 0;
}

.playoff-bracket__round--quarter {
  grid-template-rows: subgrid;
  padding-block: clamp(16px, 2vw, 30px);
  box-sizing: border-box;
}

.playoff-bracket__round--semifinal,
.playoff-bracket__round--final {
  display: flex;
  align-items: center;
  grid-row: 1 / -1;
}

.bracket-match {
  position: relative;
  width: 100%;
  min-width: 0;
}

.playoff-bracket__round--quarter .bracket-match--row-top {
  grid-row: 1;
  align-self: start;
}

.playoff-bracket__round--quarter .bracket-match--row-bottom {
  grid-row: 3;
  align-self: end;
}

.playoff-bracket__round--semifinal .bracket-match,
.playoff-bracket__round--final .bracket-match {
  width: 100%;
}

@media (min-width: 1600px) {
  .playoff-bracket__board {
    max-width: 1340px;
    --round-one-width: clamp(235px, 15vw, 250px);
    --semifinal-width: clamp(205px, 13vw, 220px);
    --final-width: clamp(240px, 16vw, 260px);
    --merge-line-width: clamp(32px, 2.2vw, 38px);
    --final-line-width: clamp(34px, 2.4vw, 42px);
  }
}

@media (min-width: 1366px) and (max-width: 1599px) {
  .playoff-bracket__board {
    max-width: 1220px;
    --round-one-width: clamp(220px, 15.5vw, 235px);
    --semifinal-width: clamp(190px, 13vw, 205px);
    --final-width: clamp(225px, 15.5vw, 240px);
    --merge-line-width: clamp(28px, 2.2vw, 34px);
    --final-line-width: clamp(30px, 2.4vw, 36px);
    height: clamp(350px, 30vw, 400px);
  }
}

@media (max-width: 1365px) and (min-width: 1200px) {
  .playoff-bracket__board {
    max-width: 1160px;
    min-width: 1120px;
    --round-one-width: clamp(200px, 14vw, 220px);
    --semifinal-width: clamp(178px, 12vw, 195px);
    --final-width: clamp(210px, 14.5vw, 230px);
    --merge-line-width: clamp(24px, 2vw, 30px);
    --final-line-width: clamp(24px, 2.2vw, 32px);
  }
}

@media (max-width: 1199px) and (min-width: 1100px) {
  .playoff-bracket__board {
    max-width: 1120px;
    min-width: 1120px;
    --round-one-width: 200px;
    --semifinal-width: 178px;
    --final-width: 210px;
    --merge-line-width: 24px;
    --final-line-width: 26px;
  }
}

@media (max-width: 1100px) {
  .playoff-bracket__scroll-hint {
    display: block;
  }

  .playoff-bracket__viewport {
    overflow-x: auto;
    overflow-y: hidden;
    overscroll-behavior-x: contain;
    -webkit-overflow-scrolling: touch;
    padding-bottom: 4px;

    &::before,
    &::after {
      content: '';
      position: absolute;
      top: 0;
      bottom: 0;
      width: 24px;
      pointer-events: none;
      z-index: 2;
    }

    &::before {
      left: 0;
      background: linear-gradient(90deg, rgba(239, 238, 235, 0.95), transparent);
    }

    &::after {
      right: 0;
      background: linear-gradient(270deg, rgba(239, 238, 235, 0.95), transparent);
    }
  }

  .playoff-bracket__board {
    min-width: 1120px;
    max-width: none;
    margin-inline: auto;
  }
}

@media (max-width: 767px) {
  .playoff-bracket__board {
    min-width: 1080px;
  }
}
</style>