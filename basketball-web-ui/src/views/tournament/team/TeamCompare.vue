<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TeamSelectorDialog from '@/components/team/TeamSelectorDialog.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import PaperTear from '@/components/common/PaperTear.vue'
import { getCompareData, getTeamById } from '@/mock/team'
import type { TeamCompareBarStat, TeamStage } from '@/types/team'
import './styles/team.scss'

const route = useRoute()
const router = useRouter()

const selectorVisible = ref(false)
const selectorSide = ref<'left' | 'right'>('left')

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

const teamPair = computed(() => {
  let left = normalizeQueryTeam(route.query.left, 'rng')
  let right = normalizeQueryTeam(route.query.right, 'titan')

  left = getTeamById(left).id
  right = getTeamById(right).id

  if (left === right) {
    right = left === 'titan' ? 'rng' : 'titan'
  }

  return { left, right }
})

const leftTeam = computed(() => getTeamById(teamPair.value.left))
const rightTeam = computed(() => getTeamById(teamPair.value.right))

const compareData = computed(() =>
  getCompareData(teamPair.value.left, teamPair.value.right),
)

const rightOverview = computed(() =>
  getCompareData(teamPair.value.right, teamPair.value.left).overview,
)

const overviewCards = computed(() => [
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
])

function normalizeQueryTeam(value: unknown, fallback: string): string {
  if (typeof value !== 'string' || !value.trim()) return fallback
  return value.toLowerCase().replace(/_/g, '-')
}

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
  const { left, right } = teamPair.value

  if (selectorSide.value === 'left') {
    router.replace({
      path: route.path,
      query: { left: teamId, right },
    })
  } else {
    router.replace({
      path: route.path,
      query: { left, right: teamId },
    })
  }

  closeSelector()
}
</script>

<template>
  <div class="team-compare-page">
    <div class="site-shell team-compare-page__shell">
      <section class="compare-hero">
        <div class="compare-hero__bg" aria-hidden="true" />

        <div class="compare-hero__inner">
          <div class="compare-hero__teams">
            <div class="compare-team compare-team--left">
              <TeamLogo :team="leftTeam" size="xl" class="compare-team__logo" />
              <h2 class="compare-team__name">{{ leftTeam.name }}</h2>
              <p class="compare-team__record">
                {{ getRecordText(leftTeam.wins, leftTeam.losses) }}
              </p>
              <p class="compare-team__rank">
                {{ stageLabels[leftTeam.currentStage] }}第{{ leftTeam.ranking }}
              </p>
              <button type="button" class="compare-team__change" @click="openSelector('left')">
                更换球队
              </button>
            </div>

            <div class="compare-hero__center">
              <strong class="compare-hero__vs">VS</strong>
              <p class="compare-hero__meta">
                {{ leftTeam.seasonName }} · {{ stageLabels[leftTeam.currentStage] }}
              </p>
            </div>

            <div class="compare-team compare-team--right">
              <TeamLogo :team="rightTeam" size="xl" class="compare-team__logo" />
              <h2 class="compare-team__name">{{ rightTeam.name }}</h2>
              <p class="compare-team__record">
                {{ getRecordText(rightTeam.wins, rightTeam.losses) }}
              </p>
              <p class="compare-team__rank">
                {{ stageLabels[rightTeam.currentStage] }}第{{ rightTeam.ranking }}
              </p>
              <button type="button" class="compare-team__change" @click="openSelector('right')">
                更换球队
              </button>
            </div>
          </div>
        </div>
      </section>

      <div class="compare-paper cnpa-paper-section cnpa-paper-section--light">
        <PaperTear position="top" tone="light" />
        <div class="compare-paper__body">
          <div class="site-shell-inner compare-content">
            <div class="compare-section-head">
              <div class="section-head-group">
                <h2 class="section-head-title">常规赛数据对比</h2>
                <span class="section-head-sub">Regular Season Comparison</span>
              </div>
              <p class="compare-section-note">统计范围：已审核完成比赛</p>
            </div>

            <div class="overview-cards">
              <article v-for="card in overviewCards" :key="card.label" class="overview-card">
                <h3 class="overview-card__label">{{ card.label }}</h3>
                <div class="overview-card__values">
                  <span
                    class="overview-card__value overview-card__value--left"
                    :class="{ 'is-winner': isOverviewWinner(card, 'left') }"
                  >
                    {{ leftTeam.code }} {{ card.left }}
                  </span>
                  <span
                    class="overview-card__value overview-card__value--right"
                    :class="{ 'is-winner': isOverviewWinner(card, 'right') }"
                  >
                    {{ rightTeam.code }} {{ card.right }}
                  </span>
                </div>
              </article>
            </div>

            <div class="compare-block">
              <h3 class="compare-block__title">基础数据</h3>
              <div class="compare-bars">
                <div v-for="stat in compareData.basics" :key="stat.key" class="compare-bar-row">
                  <div class="compare-bar-row__label">{{ stat.label }}</div>
                  <div class="compare-bar-row__bars">
                    <div class="compare-bar compare-bar--left">
                      <span
                        class="compare-bar__value"
                        :class="{ 'is-winner': isWinner(stat, 'left') }"
                      >
                        {{ formatBarValue(stat, 'left') }}
                      </span>
                      <div class="compare-bar__track">
                        <div
                          class="compare-bar__fill compare-bar__fill--left"
                          :class="{ 'is-winner': isWinner(stat, 'left') }"
                          :style="{ width: `${getBarWidth(stat.left, stat.right)}%` }"
                        />
                      </div>
                    </div>
                    <div class="compare-bar compare-bar--right">
                      <div class="compare-bar__track">
                        <div
                          class="compare-bar__fill compare-bar__fill--right"
                          :class="{ 'is-winner': isWinner(stat, 'right') }"
                          :style="{ width: `${getBarWidth(stat.right, stat.left)}%` }"
                        />
                      </div>
                      <span
                        class="compare-bar__value"
                        :class="{ 'is-winner': isWinner(stat, 'right') }"
                      >
                        {{ formatBarValue(stat, 'right') }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="compare-block">
              <h3 class="compare-block__title">投篮表现</h3>
              <div class="compare-bars">
                <div v-for="stat in compareData.shooting" :key="stat.key" class="compare-bar-row">
                  <div class="compare-bar-row__label">{{ stat.label }}</div>
                  <div class="compare-bar-row__bars">
                    <div class="compare-bar compare-bar--left">
                      <span
                        class="compare-bar__value"
                        :class="{ 'is-winner': isWinner(stat, 'left') }"
                      >
                        {{ formatBarValue(stat, 'left') }}
                      </span>
                      <div class="compare-bar__track">
                        <div
                          class="compare-bar__fill compare-bar__fill--left"
                          :class="{ 'is-winner': isWinner(stat, 'left') }"
                          :style="{ width: `${getBarWidth(stat.left, stat.right)}%` }"
                        />
                      </div>
                    </div>
                    <div class="compare-bar compare-bar--right">
                      <div class="compare-bar__track">
                        <div
                          class="compare-bar__fill compare-bar__fill--right"
                          :class="{ 'is-winner': isWinner(stat, 'right') }"
                          :style="{ width: `${getBarWidth(stat.right, stat.left)}%` }"
                        />
                      </div>
                      <span
                        class="compare-bar__value"
                        :class="{ 'is-winner': isWinner(stat, 'right') }"
                      >
                        {{ formatBarValue(stat, 'right') }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <p class="compare-footnote">
              高亮代表该项表现更优；失误、犯规为数值越低越优
            </p>
          </div>
        </div>
      </div>
    </div>

    <TeamSelectorDialog
      :visible="selectorVisible"
      :side="selectorSide"
      :left-team-id="teamPair.left"
      :right-team-id="teamPair.right"
      @close="closeSelector"
      @select="handleTeamSelect"
    />
  </div>
</template>

<style scoped lang="scss">
.compare-hero {
  position: relative;
  overflow: hidden;
  width: 100%;
  background: #050505;
  color: #fff;
}

.compare-hero__bg {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, rgba(225, 6, 19, 0.22) 0%, transparent 42%),
    radial-gradient(circle at 80% 0%, rgba(63, 140, 255, 0.12) 0%, transparent 45%),
    linear-gradient(180deg, #0a0a0a 0%, #050505 100%);
  pointer-events: none;
}

.compare-hero__inner {
  position: relative;
  z-index: 1;
  padding-block: 48px 36px;
}

.compare-hero__teams {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 24px;
  align-items: center;
}

.compare-team {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.compare-team--right {
  justify-self: end;
}

.compare-team--left {
  justify-self: start;
}

.compare-team__logo {
  margin-bottom: 16px;
}

.compare-team__name {
  margin: 0 0 8px;
  font-size: clamp(28px, 3vw, 36px);
  font-weight: 900;
  letter-spacing: 0.04em;
}

.compare-team__record,
.compare-team__rank {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.72);
}

.compare-team__change {
  margin-top: 14px;
  min-width: 108px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    background: rgba(225, 6, 19, 0.16);
  }
}

.compare-hero__center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding-inline: 12px;
}

.compare-hero__vs {
  font-size: clamp(36px, 4vw, 48px);
  font-weight: 900;
  letter-spacing: 0.08em;
  color: var(--cnpa-red);
}

.compare-hero__meta {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.58);
  white-space: nowrap;
}

.compare-content {
  padding-block: 40px 64px;
  color: var(--cnpa-paper-text);
}

.compare-section-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;

  .section-head-title {
    color: var(--cnpa-paper-text);
  }

  .section-head-sub {
    color: var(--cnpa-paper-text-muted);
  }
}

.compare-section-note {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--cnpa-paper-text-muted);
}

.overview-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 36px;
}

.overview-card {
  padding: 18px 14px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.overview-card__label {
  margin: 0 0 14px;
  font-size: 13px;
  font-weight: 800;
  color: var(--cnpa-paper-text-muted);
}

.overview-card__values {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.overview-card__value {
  font-size: 14px;
  font-weight: 700;
  color: rgba(17, 17, 17, 0.55);

  &.is-winner {
    color: var(--cnpa-red);
    font-weight: 900;
  }
}

.compare-block + .compare-block {
  margin-top: 32px;
}

.compare-block__title {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.compare-bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.compare-bar-row {
  padding: 16px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.72);
}

.compare-bar-row__label {
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
  color: var(--cnpa-paper-text);
}

.compare-bar-row__bars {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.compare-bar {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 10px;
  align-items: center;
}

.compare-bar--right {
  grid-template-columns: 1fr auto;
}

.compare-bar__value {
  min-width: 52px;
  font-size: 13px;
  font-weight: 700;
  color: rgba(17, 17, 17, 0.55);

  &.is-winner {
    color: var(--cnpa-red);
    font-weight: 900;
  }
}

.compare-bar--right .compare-bar__value {
  text-align: right;
}

.compare-bar__track {
  height: 10px;
  background: rgba(17, 17, 17, 0.08);
  overflow: hidden;
}

.compare-bar--right .compare-bar__track {
  direction: rtl;
}

.compare-bar__fill {
  height: 100%;
  opacity: 0.72;
  transition: width 0.2s ease;

  &--left {
    background: var(--gold);
  }

  &--right {
    background: var(--blue);
  }

  &.is-winner {
    opacity: 1;
    box-shadow: 0 0 0 1px rgba(17, 17, 17, 0.08);
  }
}

.compare-footnote {
  margin: 24px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--cnpa-paper-text-muted);
}

@media (max-width: 992px) {
  .compare-hero__teams {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .compare-team--left,
  .compare-team--right {
    justify-self: center;
  }

  .overview-cards {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .compare-bar-row__bars {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .overview-cards {
    grid-template-columns: 1fr;
  }
}
</style>
