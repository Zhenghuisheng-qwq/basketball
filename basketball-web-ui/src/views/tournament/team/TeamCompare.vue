<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import compareIcon1 from '@/assets/icon/tournament/compare_1.png'
import compareIcon2 from '@/assets/icon/tournament/compare_2.png'
import compareIcon3 from '@/assets/icon/tournament/compare_3.png'
import compareIcon4 from '@/assets/icon/tournament/compare_4.png'
import TeamSelectModal from '@/components/team/TeamSelectModal.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import PaperTear from '@/components/common/PaperTear.vue'
import { getCompareData, getTeamById } from '@/mock/team'
import type { TeamCompareBarStat, TeamCompareData, TeamStage } from '@/types/team'
import './styles/team.scss'

const route = useRoute()
const router = useRouter()

const selectorVisible = ref(false)
const selectorSide = ref<'left' | 'right'>('right')

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

function normalizeQueryTeam(value: unknown): string {
  if (typeof value !== 'string' || !value.trim()) return ''
  return getTeamById(value.toLowerCase().replace(/_/g, '-')).id
}

const leftTeamId = computed(() => normalizeQueryTeam(route.query.left) || 'rng')
const rightTeamId = computed(() => normalizeQueryTeam(route.query.right))

const hasValidPair = computed(
  () => Boolean(rightTeamId.value) && leftTeamId.value !== rightTeamId.value,
)

const leftTeam = computed(() => getTeamById(leftTeamId.value))
const rightTeam = computed(() =>
  rightTeamId.value ? getTeamById(rightTeamId.value) : null,
)

const compareCompetitionText = computed(
  () => `${leftTeam.value.seasonName} · ${stageLabels[leftTeam.value.currentStage]}`,
)

const compareHeroBgStyle = computed(() => ({
  backgroundImage: `url(${teamBackground})`,
}))

const overviewIconMap: Record<string, string> = {
  胜率: compareIcon1,
  场均得分: compareIcon2,
  场均失分: compareIcon3,
  场均净胜分: compareIcon4,
}

const modalSideText = computed(() => {
  if (selectorSide.value === 'left') return '正在更换左侧球队'
  if (!rightTeamId.value) return '正在选择对比球队'
  return '正在更换右侧球队'
})

const modalDisabledTeamIds = computed(() => {
  if (selectorSide.value === 'left') {
    return rightTeamId.value ? [rightTeamId.value] : []
  }
  return [leftTeamId.value]
})

const modalSelectedTeamIds = computed(() => {
  const ids = [leftTeamId.value]
  if (rightTeamId.value) ids.push(rightTeamId.value)
  return ids
})

const compareData = computed<TeamCompareData | null>(() => {
  if (!hasValidPair.value || !rightTeamId.value) return null
  return getCompareData(leftTeamId.value, rightTeamId.value)
})

const rightOverview = computed(() => {
  if (!hasValidPair.value || !rightTeamId.value) return null
  return getCompareData(rightTeamId.value, leftTeamId.value).overview
})

const overviewCards = computed(() => {
  if (!compareData.value || !rightOverview.value) return []

  return [
    {
      label: '胜率',
      left: `${compareData.value.overview.winRate.toFixed(1)}%`,
      right: `${rightOverview.value.winRate.toFixed(1)}%`,
      leftRaw: compareData.value.overview.winRate,
      rightRaw: rightOverview.value.winRate,
      higherIsBetter: true,
    },
    {
      label: '场均得分',
      left: compareData.value.overview.avgPoints.toFixed(1),
      right: rightOverview.value.avgPoints.toFixed(1),
      leftRaw: compareData.value.overview.avgPoints,
      rightRaw: rightOverview.value.avgPoints,
      higherIsBetter: true,
    },
    {
      label: '场均失分',
      left: compareData.value.overview.avgPointsAgainst.toFixed(1),
      right: rightOverview.value.avgPointsAgainst.toFixed(1),
      leftRaw: compareData.value.overview.avgPointsAgainst,
      rightRaw: rightOverview.value.avgPointsAgainst,
      higherIsBetter: false,
    },
    {
      label: '场均净胜分',
      left: formatSigned(compareData.value.overview.avgPointDiff),
      right: formatSigned(rightOverview.value.avgPointDiff),
      leftRaw: compareData.value.overview.avgPointDiff,
      rightRaw: rightOverview.value.avgPointDiff,
      higherIsBetter: true,
    },
  ]
})

watch(
  () => [route.query.left, route.query.right] as const,
  ([left, right]) => {
    const leftId = normalizeQueryTeam(left) || 'rng'
    const rightId = normalizeQueryTeam(right)

    if (!rightId || leftId === rightId) {
      selectorSide.value = 'right'
      selectorVisible.value = true

      if (leftId === rightId && rightId) {
        router.replace({
          path: route.path,
          query: { left: leftId },
        })
      }
    }
  },
  { immediate: true },
)

function formatSigned(value: number): string {
  return value > 0 ? `+${value.toFixed(1)}` : value.toFixed(1)
}

function getRecordText(wins: number, losses: number): string {
  return `${wins}胜${losses}负`
}

function getBarWidth(value: number, other: number): number {
  const max = Math.max(value, other, 0.0001)
  return (value / max) * 100
}

function isWinner(stat: TeamCompareBarStat, side: 'left' | 'right'): boolean {
  if (stat.higherIsBetter) {
    if (stat.left === stat.right) return true
    return side === 'left' ? stat.left > stat.right : stat.right > stat.left
  }
  if (stat.left === stat.right) return true
  return side === 'left' ? stat.left < stat.right : stat.right < stat.left
}

function isOverviewWinner(
  card: (typeof overviewCards.value)[number],
  side: 'left' | 'right',
): boolean {
  if (card.higherIsBetter) {
    if (card.leftRaw === card.rightRaw) return true
    return side === 'left' ? card.leftRaw > card.rightRaw : card.rightRaw > card.leftRaw
  }
  if (card.leftRaw === card.rightRaw) return true
  return side === 'left' ? card.leftRaw < card.rightRaw : card.rightRaw < card.leftRaw
}

function formatBarValue(stat: TeamCompareBarStat, side: 'left' | 'right'): string {
  const value = side === 'left' ? stat.left : stat.right
  if (stat.key.endsWith('Pct')) return `${value.toFixed(1)}%`
  return value.toFixed(1)
}

function openSelector(side: 'left' | 'right') {
  selectorSide.value = side
  selectorVisible.value = true
}

function closeSelector() {
  selectorVisible.value = false
}

function handleTeamSelect(teamId: string) {
  if (selectorSide.value === 'left') {
    if (teamId === rightTeamId.value) return
    router.replace({
      path: route.path,
      query: { left: teamId, right: rightTeamId.value },
    })
  } else {
    if (teamId === leftTeamId.value) return
    router.replace({
      path: route.path,
      query: { left: leftTeamId.value, right: teamId },
    })
  }

  closeSelector()
}
</script>

<template>
  <div class="team-compare-page">
    <div class="site-shell team-compare-page__shell">
      <section class="compare-hero">
        <div class="compare-hero__bg" :style="compareHeroBgStyle" aria-hidden="true" />
        <div class="compare-hero__overlay" aria-hidden="true" />

        <div class="compare-hero__inner">
          <nav class="compare-hero__crumb" aria-label="面包屑">
            <router-link to="/tournament">赛事</router-link>
            <span>/</span>
            <router-link to="/tournament">CNPA League</router-link>
            <span>/</span>
            <b>球队对比</b>
          </nav>

          <div class="compare-hero__battle team-compare-inner">
            <div class="compare-team-side compare-team-side--left team-side team-side-left">
              <TeamLogo :team="leftTeam" size="xl" class="compare-team-side__logo" />
              <div class="compare-team-info">
                <h2 class="compare-team-name">{{ leftTeam.name }}</h2>
                <p class="compare-team-record">
                  {{ getRecordText(leftTeam.wins, leftTeam.losses) }}
                </p>
                <p class="compare-team-rank">
                  {{ stageLabels[leftTeam.currentStage] }}第{{ leftTeam.ranking }}
                </p>
                <button type="button" class="compare-switch-btn" @click="openSelector('left')">
                  更换球队
                </button>
              </div>
            </div>

            <div class="compare-vs-area team-center">
              <span class="compare-vs match-vs">VS</span>
              <span class="compare-season-pill">{{ compareCompetitionText }}</span>
            </div>

            <div class="compare-team-side compare-team-side--right team-side team-side-right">
              <template v-if="rightTeam">
                <TeamLogo :team="rightTeam" size="xl" class="compare-team-side__logo" />
                <div class="compare-team-info">
                  <h2 class="compare-team-name">{{ rightTeam.name }}</h2>
                  <p class="compare-team-record">
                    {{ getRecordText(rightTeam.wins, rightTeam.losses) }}
                  </p>
                  <p class="compare-team-rank">
                    {{ stageLabels[rightTeam.currentStage] }}第{{ rightTeam.ranking }}
                  </p>
                  <button type="button" class="compare-switch-btn" @click="openSelector('right')">
                    更换球队
                  </button>
                </div>
              </template>

              <template v-else>
                <div class="compare-team-side__placeholder" aria-hidden="true">?</div>
                <div class="compare-team-info">
                  <h2 class="compare-team-name compare-team-name--placeholder">请选择对比球队</h2>
                  <p class="compare-team-record compare-team-record--placeholder">
                    点击更换球队或从弹窗中选择
                  </p>
                  <button type="button" class="compare-switch-btn" @click="openSelector('right')">
                    更换球队
                  </button>
                </div>
              </template>
            </div>
          </div>
        </div>
      </section>

      <div class="compare-paper">
        <PaperTear position="top" tone="light" />
        <div class="compare-paper__body">
          <div class="site-shell-inner compare-content">
            <div class="compare-section-title">
              <h1 class="compare-section-title__cn">常规赛数据对比</h1>
              <span class="compare-section-title__en">Regular Season Comparison</span>
            </div>
            <p class="compare-scope">统计范围：已审核完成比赛</p>

            <div v-if="!hasValidPair" class="compare-empty">请选择对比球队后查看数据</div>

            <template v-else-if="compareData && rightTeam">
              <section class="compare-top-stats">
                <article v-for="card in overviewCards" :key="card.label" class="compare-card">
                  <div class="compare-card__head">
                    <img
                      v-if="overviewIconMap[card.label]"
                      :src="overviewIconMap[card.label]"
                      :alt="card.label"
                      class="compare-card__icon"
                    />
                    <span>{{ card.label }}</span>
                  </div>
                  <div class="compare-card__values">
                    <strong
                      class="compare-card__value compare-card__value--left"
                      :class="{ 'is-winner': isOverviewWinner(card, 'left') }"
                    >
                      {{ card.left }}
                    </strong>
                    <strong
                      class="compare-card__value compare-card__value--right"
                      :class="{ 'is-winner': isOverviewWinner(card, 'right') }"
                    >
                      {{ card.right }}
                    </strong>
                  </div>
                  <div class="compare-card__teams">
                    <span>{{ leftTeam.code }}</span>
                    <i aria-hidden="true" />
                    <span>{{ rightTeam.code }}</span>
                  </div>
                </article>
              </section>

              <section class="compare-block">
                <div class="compare-block-title">
                  <h2>基础数据</h2>
                </div>
                <div class="metric-grid">
                  <div class="metric-side metric-side--left">
                    <div v-for="stat in compareData.basics" :key="stat.key" class="metric-row">
                      <strong
                        class="metric-value"
                        :class="{
                          'is-winner': isWinner(stat, 'left'),
                          'is-muted': !isWinner(stat, 'left'),
                        }"
                      >
                        {{ formatBarValue(stat, 'left') }}
                      </strong>
                      <span class="bar-track">
                        <span
                          class="bar-fill bar-fill--left"
                          :class="{ 'is-gray': !isWinner(stat, 'left') }"
                          :style="{ width: `${getBarWidth(stat.left, stat.right)}%` }"
                        />
                      </span>
                    </div>
                  </div>
                  <div class="metric-labels">
                    <span v-for="stat in compareData.basics" :key="stat.key">{{ stat.label }}</span>
                  </div>
                  <div class="metric-side metric-side--right">
                    <div v-for="stat in compareData.basics" :key="stat.key" class="metric-row">
                      <span class="bar-track">
                        <span
                          class="bar-fill bar-fill--right"
                          :class="{ 'is-gray': !isWinner(stat, 'right') }"
                          :style="{ width: `${getBarWidth(stat.right, stat.left)}%` }"
                        />
                      </span>
                      <strong
                        class="metric-value"
                        :class="{
                          'is-winner': isWinner(stat, 'right'),
                          'is-muted': !isWinner(stat, 'right'),
                        }"
                      >
                        {{ formatBarValue(stat, 'right') }}
                      </strong>
                    </div>
                  </div>
                </div>
              </section>

              <section class="compare-block">
                <div class="compare-block-title">
                  <h2>投篮表现</h2>
                </div>
                <div class="metric-grid">
                  <div class="metric-side metric-side--left">
                    <div v-for="stat in compareData.shooting" :key="stat.key" class="metric-row">
                      <strong
                        class="metric-value"
                        :class="{
                          'is-winner': isWinner(stat, 'left'),
                          'is-muted': !isWinner(stat, 'left'),
                        }"
                      >
                        {{ formatBarValue(stat, 'left') }}
                      </strong>
                      <span class="bar-track">
                        <span
                          class="bar-fill bar-fill--left"
                          :class="{ 'is-gray': !isWinner(stat, 'left') }"
                          :style="{ width: `${getBarWidth(stat.left, stat.right)}%` }"
                        />
                      </span>
                    </div>
                  </div>
                  <div class="metric-labels">
                    <span v-for="stat in compareData.shooting" :key="stat.key">{{ stat.label }}</span>
                  </div>
                  <div class="metric-side metric-side--right">
                    <div v-for="stat in compareData.shooting" :key="stat.key" class="metric-row">
                      <span class="bar-track">
                        <span
                          class="bar-fill bar-fill--right"
                          :class="{ 'is-gray': !isWinner(stat, 'right') }"
                          :style="{ width: `${getBarWidth(stat.right, stat.left)}%` }"
                        />
                      </span>
                      <strong
                        class="metric-value"
                        :class="{
                          'is-winner': isWinner(stat, 'right'),
                          'is-muted': !isWinner(stat, 'right'),
                        }"
                      >
                        {{ formatBarValue(stat, 'right') }}
                      </strong>
                    </div>
                  </div>
                </div>
              </section>

              <p class="compare-note">▼ 高亮代表该项表现更优；失误、犯规为数值越低越优</p>
            </template>
          </div>
        </div>
      </div>
    </div>

    <TeamSelectModal
      :visible="selectorVisible"
      title="选择球队"
      subtitle="点击球队即可完成更换"
      :competition-text="compareCompetitionText"
      :disabled-team-ids="modalDisabledTeamIds"
      :selected-team-ids="modalSelectedTeamIds"
      :side-text="modalSideText"
      @close="closeSelector"
      @select="handleTeamSelect"
    />
  </div>
</template>

<style scoped lang="scss">
.compare-hero {
  position: relative;
  min-height: clamp(420px, 38vw, 460px);
  overflow: hidden;
  width: 100%;
  padding: clamp(22px, 2vw, 30px) 0 clamp(28px, 2.4vw, 36px);
  background: #050505;
  color: #f4f0e8;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    z-index: 1;
    background:
      radial-gradient(
        circle at center,
        rgba(120, 0, 0, 0.08) 0%,
        rgba(0, 0, 0, 0.18) 40%,
        rgba(0, 0, 0, 0.5) 100%
      ),
      linear-gradient(
        90deg,
        rgba(0, 0, 0, 0.6) 0%,
        rgba(0, 0, 0, 0.12) 48%,
        rgba(0, 0, 0, 0.28) 100%
      );
    pointer-events: none;
  }
}

.compare-hero__bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-color: #050505;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  pointer-events: none;
}

.compare-hero__overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  opacity: 0.42;
  background:
    linear-gradient(112deg, transparent 0 54%, rgba(150, 20, 15, 0.35) 54.5%, rgba(160, 20, 15, 0.22) 67%, transparent 67.5%),
    linear-gradient(126deg, transparent 0 76%, rgba(208, 164, 60, 0.38) 76.2%, transparent 76.8%);
  pointer-events: none;
}

.compare-hero__inner {
  position: relative;
  z-index: 3;
}

.compare-hero__crumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin: 0 0 clamp(20px, 2.4vw, 32px);
  font-size: clamp(15px, 1.1vw, 17px);
  font-weight: 700;
  color: rgba(236, 232, 224, 0.76);

  a {
    color: inherit;
    text-decoration: none;
    transition: color 0.2s ease;

    &:hover {
      color: rgba(242, 238, 230, 0.92);
    }
  }

  span {
    color: rgba(196, 165, 120, 0.62);
  }

  b {
    color: #f2eee6;
    font-weight: 800;
  }
}

.compare-hero__battle,
.team-compare-inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(200px, 18vw, 260px) minmax(0, 1fr);
  align-items: center;
  column-gap: clamp(24px, 3vw, 48px);
  min-height: clamp(320px, 28vw, 380px);
  max-width: min(100%, 1380px);
  margin-inline: auto;
  padding-inline: clamp(8px, 1.2vw, 20px);
  box-sizing: border-box;
}

.compare-team-side,
.team-side {
  display: flex;
  align-items: center;
  gap: clamp(20px, 2.4vw, 36px);
  min-width: 0;
}

.compare-team-side--left,
.team-side-left {
  justify-content: flex-end;

  .compare-team-info {
    text-align: right;
  }
}

.compare-team-side--right,
.team-side-right {
  flex-direction: row-reverse;
  justify-content: flex-end;

  .compare-team-info {
    text-align: left;
  }
}

.compare-team-side__logo {
  flex-shrink: 0;

  :deep(.team-logo) {
    box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
  }

  :deep(.team-logo--xl) {
    width: clamp(150px, 12vw, 210px);
    height: clamp(150px, 12vw, 210px);

    .team-logo__fallback {
      font-size: clamp(32px, 2.8vw, 44px);
      letter-spacing: 0.08em;
    }
  }

  :deep(.team-logo--xl .badge) {
    transform: scale(3.28);
  }
}

.compare-team-side__placeholder {
  flex-shrink: 0;
  width: clamp(150px, 12vw, 210px);
  height: clamp(150px, 12vw, 210px);
  display: grid;
  place-items: center;
  border: 2px dashed rgba(255, 255, 255, 0.28);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.42);
  font-size: clamp(42px, 3.5vw, 58px);
  font-weight: 900;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.35);
}

.compare-team-info {
  min-width: 0;
  max-width: 100%;
}

.compare-team-name {
  margin: 0 0 clamp(16px, 1.6vw, 20px);
  max-width: min(100%, 480px);
  font-size: clamp(36px, 4.2vw, 72px);
  line-height: 0.98;
  font-weight: 900;
  letter-spacing: 1px;
  color: #f4f0e8;
  text-shadow: 0 4px 14px rgba(0, 0, 0, 0.35);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.compare-team-side--left .compare-team-name {
  margin-left: auto;
}

.compare-team-side--right .compare-team-name {
  margin-right: auto;
}

.compare-team-name--placeholder {
  font-size: clamp(28px, 2.8vw, 40px);
  letter-spacing: 0.5px;
  white-space: normal;
}

.compare-team-record,
.compare-team-rank {
  margin: 0;
  font-size: clamp(20px, 1.7vw, 28px);
  line-height: 1.2;
  font-weight: 800;
  letter-spacing: 0.5px;
}

.compare-team-record {
  margin-bottom: clamp(4px, 0.5vw, 8px);
  color: #f8f5ef;
}

.compare-team-rank {
  margin-bottom: clamp(24px, 2vw, 28px);
  color: #d8c5a0;
}

.compare-team-record--placeholder {
  margin-bottom: clamp(24px, 2vw, 28px);
  font-size: clamp(15px, 1.3vw, 18px);
  font-weight: 700;
  color: rgba(236, 232, 224, 0.58);
  letter-spacing: 0;
}

.compare-switch-btn {
  height: clamp(44px, 3.6vw, 48px);
  padding: 0 clamp(22px, 2vw, 28px);
  border: 1px solid rgba(206, 151, 45, 0.9);
  border-radius: 2px;
  background: rgba(10, 10, 10, 0.36);
  color: #e7cf93;
  font-size: clamp(16px, 1.2vw, 18px);
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    border-color: rgba(227, 174, 65, 0.95);
    color: #f2dea0;
    box-shadow: 0 0 14px rgba(227, 174, 65, 0.2);
  }
}

.compare-vs-area,
.team-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  align-self: center;
  min-width: 0;
}

.compare-vs,
.match-vs {
  position: relative;
  display: inline-block;
  font-size: clamp(72px, 6vw, 100px);
  line-height: 0.9;
  font-style: italic;
  font-weight: 900;
  letter-spacing: -5px;
  transform: skew(-10deg);
  background: linear-gradient(180deg, #ff493f 0%, #ed111d 48%, #a9000b 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  clip-path: polygon(1% 0, 99% 1%, 100% 99%, 0 100%);
  filter:
    drop-shadow(0 0 8px rgba(255, 30, 30, 0.28))
    drop-shadow(0 8px 10px rgba(0, 0, 0, 0.55));

  &::before {
    content: 'VS';
    position: absolute;
    inset: 0;
    z-index: -2;
    color: rgba(55, 0, 0, 0.55);
    -webkit-text-fill-color: rgba(55, 0, 0, 0.55);
    transform: translate(3px, 7px) skew(2deg);
    clip-path: polygon(0 4%, 100% 0, 97% 100%, 3% 96%);
  }

  &::after {
    content: 'VS';
    position: absolute;
    inset: 0;
    z-index: -1;
    color: rgba(90, 0, 0, 0.7);
    -webkit-text-fill-color: rgba(90, 0, 0, 0.7);
    transform: translate(5px, 5px);
  }
}

.compare-season-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: clamp(28px, 2.8vw, 36px);
  min-width: 0;
  max-width: 100%;
  height: clamp(48px, 4vw, 54px);
  padding: 0 clamp(28px, 2.4vw, 36px);
  border: 1px solid rgba(196, 139, 32, 0.9);
  border-radius: 2px;
  color: #f0e9dc;
  font-size: clamp(18px, 1.35vw, 21px);
  font-weight: 700;
  background: rgba(8, 8, 8, 0.55);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 4px 16px rgba(0, 0, 0, 0.28);
  white-space: nowrap;
}

.compare-content {
  padding-block: 40px 64px;
  color: var(--cnpa-paper-text);
}

.compare-section-title {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 18px;
}

.compare-section-title__cn {
  margin: 0;
  font-size: clamp(28px, 2.4vw, 32px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: -1px;
}

.compare-section-title__en {
  color: var(--cnpa-red);
  font-size: 13px;
  line-height: 1.05;
  font-style: italic;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.compare-scope {
  margin: 14px 0 0;
  color: rgba(0, 0, 0, 0.55);
  font-size: 14px;
  font-weight: 700;
}

.compare-empty {
  margin-top: 26px;
  padding: 48px 16px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: rgba(255, 255, 255, 0.22);
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.compare-top-stats {
  margin-top: 26px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.compare-card {
  min-height: 146px;
  padding: 24px 20px 15px;
  background: rgba(255, 255, 255, 0.33);
  border: 1px solid rgba(0, 0, 0, 0.13);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
  box-sizing: border-box;
}

.compare-card__head {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  min-height: 38px;
  color: #111;
  font-size: 17px;
  font-weight: 900;
}

.compare-card__icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  object-fit: contain;
  display: block;
}

.compare-card__values {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  align-items: end;
  gap: 18px;
}

.compare-card__value {
  text-align: center;
  font-size: clamp(22px, 2vw, 28px);
  line-height: 1;
  font-weight: 900;
  color: #777;

  &--left.is-winner {
    color: var(--gold);
  }

  &--right.is-winner {
    color: #174b92;
  }
}

.compare-card__teams {
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.16);
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  gap: 12px;
  color: rgba(0, 0, 0, 0.58);
  font-size: 13px;
  font-weight: 800;
  text-align: center;

  i {
    width: 1px;
    height: 16px;
    background: rgba(0, 0, 0, 0.22);
    display: block;
    margin: 0 auto;
  }
}

.compare-block {
  margin-top: 42px;
}

.compare-block-title {
  display: flex;
  align-items: center;
  gap: 12px;

  &::before {
    content: '';
    width: 4px;
    height: 28px;
    background: var(--cnpa-red);
    flex-shrink: 0;
  }

  h2 {
    margin: 0;
    font-size: clamp(24px, 2vw, 28px);
    line-height: 1;
    font-weight: 900;
  }
}

.metric-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: 1fr 120px 1fr;
  gap: 24px;
  align-items: center;
}

.metric-side {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.metric-row {
  height: 40px;
  display: grid;
  align-items: center;
  gap: 13px;
  padding: 0 16px;
  background: rgba(255, 255, 255, 0.28);
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
  box-sizing: border-box;
}

.metric-side--left .metric-row {
  grid-template-columns: 66px 1fr;
}

.metric-side--right .metric-row {
  grid-template-columns: 1fr 66px;
}

.metric-value {
  font-size: 21px;
  line-height: 1;
  font-weight: 900;
}

.metric-side--left .metric-value {
  color: var(--gold);

  &.is-muted {
    color: #787878;
  }
}

.metric-side--right .metric-value {
  color: #174b92;
  text-align: right;

  &.is-muted {
    color: #787878;
  }
}

.bar-track {
  height: 14px;
  background: rgba(0, 0, 0, 0.08);
  border-radius: 3px;
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: 3px;
  transition: width 0.2s ease;

  &--left {
    background: #c0963e;
  }

  &--right {
    background: #174c9f;
    margin-left: auto;
  }

  &.is-gray {
    background: rgba(0, 0, 0, 0.13);
  }
}

.metric-labels {
  display: flex;
  flex-direction: column;
  gap: 13px;
  text-align: center;

  span {
    height: 40px;
    display: grid;
    place-items: center;
    font-size: 18px;
    font-weight: 900;
    color: #111;
  }
}

.compare-note {
  margin: 27px 0 0 24px;
  color: rgba(0, 0, 0, 0.62);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 1px;
}

@media (max-width: 1280px) {
  .compare-hero__battle,
  .team-compare-inner {
    grid-template-columns: minmax(0, 1fr) clamp(180px, 16vw, 220px) minmax(0, 1fr);
    column-gap: clamp(20px, 2.5vw, 36px);
  }

  .compare-team-name {
    font-size: clamp(42px, 3.8vw, 58px);
  }

  .compare-team-side__logo :deep(.team-logo--xl),
  .compare-team-side__placeholder {
    width: clamp(140px, 12vw, 190px);
    height: clamp(140px, 12vw, 190px);
  }

  .compare-team-side__logo :deep(.team-logo--xl .badge) {
    transform: scale(2.97);
  }
}

@media (max-width: 1024px) {
  .compare-hero__battle,
  .team-compare-inner {
    grid-template-columns: minmax(0, 1fr) 200px minmax(0, 1fr);
    column-gap: clamp(18px, 2vw, 28px);
  }

  .compare-team-name {
    font-size: clamp(36px, 4vw, 54px);
  }

  .compare-vs,
  .match-vs {
    font-size: clamp(64px, 7vw, 88px);
  }

  .compare-team-side__logo :deep(.team-logo--xl),
  .compare-team-side__placeholder {
    width: clamp(130px, 13vw, 170px);
    height: clamp(130px, 13vw, 170px);
  }

  .compare-team-side__logo :deep(.team-logo--xl .badge) {
    transform: scale(2.66);
  }
}

@media (max-width: 992px) {
  .compare-top-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .metric-labels {
    order: -1;
  }
}

@media (max-width: 720px) {
  .compare-hero {
    min-height: auto;
  }

  .compare-hero__battle,
  .team-compare-inner {
    grid-template-columns: 1fr;
    gap: clamp(20px, 5vw, 28px);
    min-height: auto;
    padding-bottom: clamp(24px, 4vw, 32px);
  }

  .compare-team-side,
  .compare-team-side--left,
  .compare-team-side--right,
  .team-side,
  .team-side-left,
  .team-side-right {
    flex-direction: row;
    justify-content: center;

    .compare-team-info {
      text-align: center;
    }
  }

  .compare-team-side--left .compare-team-name,
  .compare-team-side--right .compare-team-name {
    margin-inline: auto;
  }

  .compare-vs-area,
  .team-center {
    order: 2;
  }

  .compare-team-name {
    font-size: clamp(32px, 8vw, 44px);
    max-width: 100%;
  }

  .compare-vs,
  .match-vs {
    font-size: clamp(56px, 14vw, 72px);
  }
}

@media (max-width: 640px) {
  .compare-top-stats {
    grid-template-columns: 1fr;
  }

  .compare-season-pill {
    width: 100%;
    max-width: min(100%, 320px);
    font-size: clamp(15px, 3.8vw, 18px);
    padding-inline: clamp(16px, 4vw, 24px);
  }

  .compare-team-side,
  .team-side {
    gap: clamp(14px, 3.5vw, 20px);
  }

  .compare-team-side__logo :deep(.team-logo--xl),
  .compare-team-side__placeholder {
    width: clamp(110px, 26vw, 140px);
    height: clamp(110px, 26vw, 140px);
  }

  .compare-team-side__logo :deep(.team-logo--xl .badge) {
    transform: scale(2.19);
  }

  .compare-team-record,
  .compare-team-rank {
    font-size: clamp(17px, 4.2vw, 22px);
  }

  .compare-switch-btn {
    height: 42px;
    font-size: 15px;
  }
}
</style>
