<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import compareIcon1 from '@/assets/icon/tournament/compare_1.png'
import compareIcon2 from '@/assets/icon/tournament/compare_2.png'
import compareIcon3 from '@/assets/icon/tournament/compare_3.png'
import compareIcon4 from '@/assets/icon/tournament/compare_4.png'
import vsIcon from '@/assets/icon/tournament/vs.png'
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

function getTeamNameSizeClass(name: string) {
  return {
    'compare-team__name--medium': name.length >= 7,
    'compare-team__name--long': name.length >= 10,
  }
}
</script>

<template>
  <div class="team-compare-page">
    <div class="site-shell team-compare-page__shell">
      <section class="compare-hero">
        <div class="compare-hero__bg" :style="compareHeroBgStyle" aria-hidden="true" />
        <div class="compare-hero__overlay" aria-hidden="true" />

        <div class="compare-hero__inner team-compare-content">
          <nav class="compare-hero__crumb" aria-label="面包屑">
            <router-link to="/tournament">赛事</router-link>
            <span>/</span>
            <router-link to="/tournament">CNPA League</router-link>
            <span>/</span>
            <b>球队对比</b>
          </nav>

          <div class="compare-hero__battle team-compare-main">
            <div class="compare-team compare-team--left">
              <div class="compare-team__logo">
                <TeamLogo :team="leftTeam" size="xl" />
              </div>
              <div class="compare-team__info">
                <div class="compare-team__name" :class="getTeamNameSizeClass(leftTeam.name)">
                  {{ leftTeam.name }}
                </div>
                <div class="compare-team__record">
                  {{ getRecordText(leftTeam.wins, leftTeam.losses) }}
                </div>
                <div class="compare-team__rank">
                  {{ stageLabels[leftTeam.currentStage] }}第{{ leftTeam.ranking }}
                </div>
              </div>
            </div>

            <div class="compare-center compare-vs-area">
              <img :src="vsIcon" alt="VS" class="compare-vs-img match-vs" />
            </div>

            <div class="compare-team compare-team--right">
              <template v-if="rightTeam">
                <div class="compare-team__logo">
                  <TeamLogo :team="rightTeam" size="xl" />
                </div>
                <div class="compare-team__info">
                  <div class="compare-team__name" :class="getTeamNameSizeClass(rightTeam.name)">
                    {{ rightTeam.name }}
                  </div>
                  <div class="compare-team__record">
                    {{ getRecordText(rightTeam.wins, rightTeam.losses) }}
                  </div>
                  <div class="compare-team__rank">
                    {{ stageLabels[rightTeam.currentStage] }}第{{ rightTeam.ranking }}
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="compare-team__logo compare-team__logo--placeholder" aria-hidden="true">
                  ?
                </div>
                <div class="compare-team__info">
                  <div class="compare-team__name compare-team__name--placeholder">请选择对比球队</div>
                  <div class="compare-team__record compare-team__record--placeholder">
                    点击更换球队或从弹窗中选择
                  </div>
                </div>
              </template>
            </div>

            <div class="compare-hero__foot compare-hero__foot--left">
              <div class="compare-hero__foot-align compare-hero__foot-align--left">
                <span class="compare-hero__foot-logo-spacer" aria-hidden="true" />
                <button type="button" class="compare-team__button" @click="openSelector('left')">
                  更换球队
                </button>
              </div>
            </div>

            <div class="compare-hero__foot compare-hero__foot--center">
              <span class="compare-season-pill match-season-tag">{{ compareCompetitionText }}</span>
            </div>

            <div class="compare-hero__foot compare-hero__foot--right">
              <div class="compare-hero__foot-align compare-hero__foot-align--right">
                <span class="compare-hero__foot-logo-spacer" aria-hidden="true" />
                <button type="button" class="compare-team__button" @click="openSelector('right')">
                  更换球队
                </button>
              </div>
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
  z-index: 1;
  overflow: hidden;
  width: 100%;
  background: #050505;
  color: #f1eee8;

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

.team-compare-content,
.compare-hero__inner {
  position: relative;
  z-index: 3;
  min-height: auto;
  display: flex;
  flex-direction: column;
  padding-top: 18px;
  padding-bottom: clamp(12px, 1.4vw, 18px);
  box-sizing: border-box;
}

.compare-hero__crumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin: 0 0 clamp(45px, 4.5vw, 65px);
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

.team-compare-main,
.compare-hero__battle {
  flex: 0 0 auto;
  display: grid;
  grid-template-columns: 43% 14% 43%;
  grid-template-rows: auto auto;
  align-items: end;
  row-gap: clamp(8px, 1.2vw, 14px);
  width: 100%;
  margin: 0 auto;
  transform: translateY(-18px);
  box-sizing: border-box;
  overflow: visible;
}

.compare-team--left {
  grid-column: 1;
  grid-row: 1;
}

.compare-center,
.compare-vs-area {
  grid-column: 2;
  grid-row: 1;
  align-self: center;
}

.compare-team--right {
  grid-column: 3;
  grid-row: 1;
}

.compare-hero__foot {
  display: flex;
  align-items: center;
  min-height: 0;
}

.compare-hero__foot--left {
  grid-column: 1;
  grid-row: 2;
  justify-content: flex-end;
}

.compare-hero__foot--center {
  grid-column: 2;
  grid-row: 2;
  justify-content: center;
}

.compare-hero__foot--right {
  grid-column: 3;
  grid-row: 2;
  justify-content: flex-start;
}

.compare-hero__foot-align {
  display: flex;
  align-items: center;
  gap: clamp(20px, 2.4vw, 46px);
  width: min(92%, 560px);
}

.compare-hero__foot-align--left {
  justify-content: flex-start;
  margin-left: auto;
  padding-right: clamp(2%, 3vw, 7%);
}

.compare-hero__foot-align--right {
  justify-content: flex-start;
  margin-right: auto;
  padding-left: clamp(2%, 3vw, 7%);
}

.compare-hero__foot-logo-spacer {
  flex: 0 0 auto;
  width: clamp(118px, 9vw, 178px);
  height: 0;
}

.compare-team {
  display: flex;
  align-items: center;
  gap: clamp(20px, 2.4vw, 46px);
  width: min(94%, 540px);
  min-width: 0;
  overflow: visible;
}

.compare-team--left {
  justify-self: end;
  width: min(92%, 560px);
  justify-content: flex-end;
  padding-right: clamp(2%, 3vw, 7%);
  margin-left: auto;
}

.compare-team--right {
  justify-self: start;
  width: min(92%, 560px);
  justify-content: flex-start;
  padding-left: clamp(2%, 3vw, 7%);
  margin-right: auto;
}

.compare-team__logo {
  flex: 0 0 auto;
  width: clamp(118px, 9vw, 178px);
  height: clamp(118px, 9vw, 178px);

  :deep(.team-logo) {
    width: 100%;
    height: 100%;
    box-shadow: 0 10px 28px rgba(0, 0, 0, 0.32);
  }

  :deep(.team-logo--xl) {
    width: 100%;
    height: 100%;

    .team-logo__fallback {
      font-size: clamp(26px, 2.2vw, 34px);
      letter-spacing: 0.06em;
    }
  }

  :deep(.team-logo--xl .badge) {
    transform: scale(2.78);
  }
}

.compare-team__logo--placeholder {
  display: grid;
  place-items: center;
  border: 2px dashed rgba(255, 255, 255, 0.28);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.42);
  font-size: clamp(36px, 3vw, 48px);
  font-weight: 900;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.32);
}

.compare-team__info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1 1 auto;
  width: auto;
  min-width: 0;
  overflow: visible;
}

.compare-team__name {
  display: block;
  width: max-content;
  max-width: 100%;
  margin: 0 0 16px;
  color: #f1eee8;
  font-size: clamp(48px, 3.6vw, 66px);
  font-weight: 800;
  line-height: 0.95;
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: visible;
  text-overflow: clip;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
}

.compare-team__name--medium {
  font-size: clamp(42px, 3.1vw, 58px);
  letter-spacing: 0;
}

.compare-team__name--long {
  font-size: clamp(34px, 2.6vw, 48px);
  letter-spacing: -1px;
}

.compare-team__name--placeholder {
  font-size: clamp(24px, 2.4vw, 32px);
  white-space: normal;
  line-height: 1.15;
}

.compare-team__record {
  margin: 0;
  font-size: clamp(22px, 1.7vw, 28px);
  line-height: 1.3;
  font-weight: 700;
  color: #f6f2eb;
}

.compare-team__rank {
  margin: 4px 0 0;
  font-size: clamp(21px, 1.6vw, 27px);
  line-height: 1.3;
  font-weight: 700;
  color: #cfc7b8;
}

.compare-team__record--placeholder {
  margin-top: 8px;
  margin-bottom: 0;
  font-size: clamp(14px, 1.2vw, 16px);
  font-weight: 600;
  color: rgba(236, 232, 224, 0.58);
  line-height: 1.4;
  white-space: normal;
}

.compare-team__button {
  margin-top: 0;
  height: 46px;
  padding: 0 26px;
  border: 1px solid rgba(184, 130, 27, 0.88);
  border-radius: 0;
  background: rgba(8, 8, 8, 0.35);
  color: #d8c8a7;
  font-size: 17px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
  transition:
    border-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;

  &:hover {
    border-color: rgba(196, 145, 40, 0.95);
    color: #e2d4b3;
    box-shadow: 0 0 10px rgba(184, 130, 27, 0.15);
  }
}

.compare-center,
.compare-vs-area {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  justify-self: center;
}

.compare-vs-img,
.match-vs {
  display: block;
  width: clamp(160px, 13vw, 210px);
  height: auto;
  max-width: 100%;
  object-fit: contain;
  flex-shrink: 0;
  user-select: none;
  pointer-events: none;
}

.compare-season-pill,
.match-season-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 260px;
  max-width: min(100%, 330px);
  width: fit-content;
  height: 46px;
  padding: 0 26px;
  border: 1px solid rgba(185, 132, 31, 0.88);
  border-radius: 0;
  background: rgba(8, 8, 8, 0.5);
  color: #e9e3d8;
  font-size: 18px;
  font-weight: 700;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.25);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.compare-content {
  padding-block: clamp(16px, 1.6vw, 24px) 64px;
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

@media (min-width: 1600px) {
  .team-compare-main,
  .compare-hero__battle {
    grid-template-columns: 43% 14% 43%;
  }

  .compare-team--left {
    padding-right: 6%;
  }

  .compare-team--right {
    padding-left: 6%;
  }
}

@media (max-width: 1599px) {
  .team-compare-main,
  .compare-hero__battle {
    grid-template-columns: 42% 16% 42%;
  }

  .compare-team--left {
    padding-right: 3%;
  }

  .compare-team--right {
    padding-left: 3%;
  }
}

@media (max-width: 1350px) {
  .team-compare-main,
  .compare-hero__battle {
    grid-template-columns: 41% 18% 41%;
  }

  .compare-team {
    width: 96%;
    gap: clamp(16px, 2vw, 28px);
  }

  .compare-team--left {
    padding-right: 1%;
  }

  .compare-team--right {
    padding-left: 1%;
  }

  .compare-team__logo,
  .compare-team__logo--placeholder {
    width: clamp(105px, 8vw, 138px);
    height: clamp(105px, 8vw, 138px);
  }

  .compare-team__logo :deep(.team-logo--xl .badge) {
    transform: scale(2.16);
  }

  .compare-team__name {
    font-size: clamp(40px, 3.4vw, 54px);
  }

  .compare-team__name--medium {
    font-size: clamp(36px, 3vw, 46px);
  }

  .compare-team__name--long {
    font-size: clamp(30px, 2.6vw, 40px);
  }
}

@media (max-width: 1200px) {
  .team-compare-main,
  .compare-hero__battle {
    grid-template-columns: 40% 20% 40%;
    transform: translateY(-12px);
  }

  .compare-team {
    width: 100%;
    gap: 16px;
  }

  .compare-team__name {
    font-size: clamp(34px, 4vw, 44px);
  }

  .compare-team__name--medium {
    font-size: clamp(30px, 3.6vw, 38px);
  }

  .compare-team__name--long {
    font-size: clamp(26px, 3.2vw, 34px);
  }

  .compare-team__logo,
  .compare-team__logo--placeholder {
    width: clamp(90px, 8vw, 112px);
    height: clamp(90px, 8vw, 112px);
  }

  .compare-team__logo :deep(.team-logo--xl .badge) {
    transform: scale(1.75);
  }

  .compare-vs-img,
  .match-vs {
    width: clamp(130px, 12vw, 168px);
  }

  .compare-season-pill,
  .match-season-tag {
    min-width: 0;
    width: 100%;
    padding-left: 10px;
    padding-right: 10px;
    font-size: 15px;
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
  .team-compare-content,
  .compare-hero__inner {
    min-height: auto;
    padding-bottom: 32px;
  }

  .team-compare-main,
  .compare-hero__battle {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: clamp(20px, 4vw, 28px);
    transform: none;
  }

  .compare-team,
  .compare-team--left,
  .compare-team--right {
    justify-self: center;
    width: min(100%, 540px);
    margin-inline: auto;
    justify-content: center;
    padding-inline: 0;
  }

  .compare-hero__foot {
    width: min(100%, 540px);
    min-height: 0;
  }

  .compare-team--left {
    order: 1;
  }

  .compare-hero__foot--left {
    order: 2;
    justify-content: center;
  }

  .compare-center,
  .compare-vs-area {
    order: 3;
  }

  .compare-hero__foot--center {
    order: 4;
  }

  .compare-team--right {
    order: 5;
  }

  .compare-hero__foot--right {
    order: 6;
    justify-content: center;
  }

  .compare-hero__foot-align {
    width: 100%;
    padding-inline: 0;
    margin-inline: 0;
    justify-content: center;
  }

  .compare-hero__foot-logo-spacer {
    display: none;
  }

  .compare-team__info {
    align-items: flex-start;
    min-width: 0;
    overflow: visible;
  }

  .compare-team__name {
    font-size: clamp(32px, 7vw, 44px);
  }

  .compare-team__name--medium {
    font-size: clamp(28px, 6vw, 38px);
  }

  .compare-team__name--long {
    font-size: clamp(24px, 5.5vw, 32px);
  }

  .compare-vs-img,
  .match-vs {
    width: clamp(112px, 14vw, 142px);
  }
}

@media (max-width: 640px) {
  .compare-top-stats {
    grid-template-columns: 1fr;
  }

  .compare-season-pill,
  .match-season-tag {
    min-width: 0;
    width: 100%;
    max-width: min(100%, 300px);
    font-size: 16px;
    padding-inline: 18px;
  }

  .compare-team {
    gap: clamp(16px, 4vw, 24px);
  }

  .compare-team__logo,
  .compare-team__logo--placeholder {
    width: clamp(100px, 22vw, 120px);
    height: clamp(100px, 22vw, 120px);
  }

  .compare-team__logo :deep(.team-logo--xl .badge) {
    transform: scale(1.875);
  }

  .compare-team__record,
  .compare-team__rank {
    font-size: clamp(17px, 4vw, 22px);
  }

  .compare-team__button {
    height: 42px;
    font-size: 15px;
    padding-inline: 20px;
  }
}
</style>
