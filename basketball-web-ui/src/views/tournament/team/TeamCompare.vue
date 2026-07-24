<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import compareIcon1 from '@/assets/icon/tournament/compare_1.png'
import compareIcon2 from '@/assets/icon/tournament/compare_2.png'
import compareIcon3 from '@/assets/icon/tournament/compare_3.png'
import compareIcon4 from '@/assets/icon/tournament/compare_4.png'
import vsIcon from '@/assets/icon/tournament/vs.png'
import ComparisonMetricRow from '@/components/team/ComparisonMetricRow.vue'
import ComparisonSummaryCard from '@/components/team/ComparisonSummaryCard.vue'
import TeamSelectModal from '@/components/team/TeamSelectModal.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import PaperTear from '@/components/common/PaperTear.vue'
import { getPublicDataCenterSeasons, getPublicDataCenterTeamRanking } from '@/api/publicDataCenter'
import { getPublicSeasonStanding } from '@/api/publicStanding'
import { getPublicTeamCompare } from '@/api/publicTeam'
import type { PublicTeamCompareVo } from '@/api/types/public'
import type { TeamBase, TeamCompareData, TeamStage } from '@/types/team'
import type { MetricFormatType } from '@/utils/compareMetrics'
import { displayText, toNumber } from '@/utils/publicDisplay'
import {
  mapPublicTeamToBase,
  mapRankToTeamBase,
  mapStandingToTeamBase,
  placeholderTeamBase,
} from '@/utils/publicTeamMap'
import { compareOverviewValues, mapPublicTeamCompareVo } from './utils/mapTeamCompare'
import './styles/team.scss'

const route = useRoute()
const router = useRouter()

const selectorVisible = ref(false)
const selectorSide = ref<'left' | 'right'>('right')

const compareVo = ref<PublicTeamCompareVo | null>(null)
const compareData = ref<TeamCompareData | null>(null)
const compareState = ref<'idle' | 'loading' | 'success' | 'empty' | 'error'>('idle')
const compareErrorMessage = ref('')

const teamOptions = ref<TeamBase[]>([])
const teamsLoadState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const seasonName = ref('')

let compareReqId = 0
let teamsReqId = 0

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

function normalizeQueryTeam(value: unknown): string {
  if (typeof value !== 'string' || !value.trim()) return ''
  const id = value.trim()
  return /^\d+$/.test(id) ? id : ''
}

const leftTeamId = computed(() => normalizeQueryTeam(route.query.left))
const rightTeamId = computed(() => normalizeQueryTeam(route.query.right))

const hasValidPair = computed(
  () => Boolean(leftTeamId.value) && Boolean(rightTeamId.value) && leftTeamId.value !== rightTeamId.value,
)

const leftTeam = computed<TeamBase>(() => {
  if (compareVo.value?.leftTeam) {
    return mapPublicTeamToBase(compareVo.value.leftTeam, {
      wins: compareVo.value.leftStat?.wins,
      losses: compareVo.value.leftStat?.losses,
      seasonName: compareVo.value.seasonName,
    })
  }
  const found = teamOptions.value.find((team) => team.id === leftTeamId.value)
  return found || (leftTeamId.value ? placeholderTeamBase(leftTeamId.value) : placeholderTeamBase('0'))
})

const rightTeam = computed<TeamBase | null>(() => {
  if (!rightTeamId.value) return null
  if (compareVo.value?.rightTeam) {
    return mapPublicTeamToBase(compareVo.value.rightTeam, {
      wins: compareVo.value.rightStat?.wins,
      losses: compareVo.value.rightStat?.losses,
      seasonName: compareVo.value.seasonName,
    })
  }
  return teamOptions.value.find((team) => team.id === rightTeamId.value) || placeholderTeamBase(rightTeamId.value)
})

const compareCompetitionText = computed(() => {
  const season = displayText(compareVo.value?.seasonName || seasonName.value, '公开赛季')
  return `${season} · ${stageLabels.regular}`
})

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
  return leftTeamId.value ? [leftTeamId.value] : []
})

const modalSelectedTeamIds = computed(() => {
  const ids: string[] = []
  if (leftTeamId.value) ids.push(leftTeamId.value)
  if (rightTeamId.value) ids.push(rightTeamId.value)
  return ids
})

interface OverviewMetricDef {
  key: string
  label: string
  formatType: MetricFormatType
  higherIsBetter: boolean
  field: 'winRate' | 'avgPoints' | 'avgPointsAgainst' | 'avgPointDiff'
}

const overviewMetricDefs: OverviewMetricDef[] = [
  {
    key: 'winRate',
    label: '胜率',
    formatType: 'percent',
    higherIsBetter: true,
    field: 'winRate',
  },
  {
    key: 'avgPoints',
    label: '场均得分',
    formatType: 'decimal',
    higherIsBetter: true,
    field: 'avgPoints',
  },
  {
    key: 'avgPointsAgainst',
    label: '场均失分',
    formatType: 'decimal',
    higherIsBetter: false,
    field: 'avgPointsAgainst',
  },
  {
    key: 'avgPointDiff',
    label: '场均净胜分',
    formatType: 'signed',
    higherIsBetter: true,
    field: 'avgPointDiff',
  },
]

const overviewCards = computed(() => {
  if (!compareVo.value) return []
  const overview = compareOverviewValues(compareVo.value)

  return overviewMetricDefs.map((metric) => ({
    ...metric,
    icon: overviewIconMap[metric.label],
    leftValue: overview.left[metric.field],
    rightValue: overview.right[metric.field],
  }))
})

const comparisonHeadingMeta = computed(() => {
  const leftGames = toNumber(compareVo.value?.leftStat?.games, leftTeam.value.wins + leftTeam.value.losses)
  const rightGames = toNumber(
    compareVo.value?.rightStat?.games,
    rightTeam.value ? rightTeam.value.wins + rightTeam.value.losses : 0,
  )

  return {
    scope: `统计范围：${displayText(compareVo.value?.seasonName || seasonName.value, '公开赛季')} 常规赛已审核比赛`,
    games: `${leftTeam.value.code} ${leftGames}场 · ${rightTeam.value?.code ?? '--'} ${rightGames}场`,
  }
})

async function loadTeamOptions() {
  const current = ++teamsReqId
  teamsLoadState.value = 'loading'
  try {
    const seasonsRes = await getPublicDataCenterSeasons()
    if (current !== teamsReqId) return
    const seasons = seasonsRes.data ?? []
    const season = seasons[0]
    seasonName.value = displayText(season?.seasonName, '')

    if (!season?.seasonId) {
      teamOptions.value = []
      teamsLoadState.value = 'empty'
      return
    }

    let teams: TeamBase[] = []
    try {
      const standingRes = await getPublicSeasonStanding(season.seasonId)
      if (current !== teamsReqId) return
      teams = (standingRes.data ?? []).map((row) => mapStandingToTeamBase(row, season.seasonName))
    } catch {
      const rankingRes = await getPublicDataCenterTeamRanking({ seasonId: season.seasonId })
      if (current !== teamsReqId) return
      teams = (rankingRes.data ?? []).map((row) => mapRankToTeamBase(row, season.seasonName))
    }

    teamOptions.value = teams
    teamsLoadState.value = teams.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== teamsReqId) return
    teamOptions.value = []
    teamsLoadState.value = 'error'
  }
}

async function loadCompareData() {
  const left = leftTeamId.value
  const right = rightTeamId.value
  const current = ++compareReqId

  if (!hasValidPair.value) {
    compareVo.value = null
    compareData.value = null
    compareState.value = 'idle'
    return
  }

  compareState.value = 'loading'
  compareErrorMessage.value = ''
  try {
    const res = await getPublicTeamCompare({
      leftTeamId: left,
      rightTeamId: right,
    })
    if (current !== compareReqId) return
    compareVo.value = res.data ?? null
    compareData.value = compareVo.value ? mapPublicTeamCompareVo(compareVo.value) : null
    compareState.value = compareData.value ? 'success' : 'empty'
  } catch (err) {
    if (current !== compareReqId) return
    compareVo.value = null
    compareData.value = null
    compareState.value = 'error'
    compareErrorMessage.value = err instanceof Error ? err.message : '加载对比数据失败'
  }
}

watch(
  () => [route.query.left, route.query.right] as const,
  ([left, right]) => {
    const leftId = normalizeQueryTeam(left)
    const rightId = normalizeQueryTeam(right)

    if (!leftId || !rightId || leftId === rightId) {
      selectorSide.value = !leftId ? 'left' : 'right'
      selectorVisible.value = true

      if (leftId && rightId && leftId === rightId) {
        router.replace({
          path: route.path,
          query: { left: leftId },
        })
      }
    }

    void loadCompareData()
  },
  { immediate: true },
)

onMounted(() => {
  void loadTeamOptions()
})

function getRecordText(wins: number, losses: number): string {
  return `${wins}胜${losses}负`
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
      query: { left: teamId, ...(rightTeamId.value ? { right: rightTeamId.value } : {}) },
    })
  } else {
    if (teamId === leftTeamId.value) return
    router.replace({
      path: route.path,
      query: { left: leftTeamId.value || teamId, right: teamId },
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
                <div v-if="leftTeam.ranking > 0" class="compare-team__rank">
                  {{ stageLabels[leftTeam.currentStage] }}第{{ leftTeam.ranking }}
                </div>
              </div>
            </div>

            <div class="compare-center compare-vs-area">
              <img :src="vsIcon" alt="VS" class="compare-vs-img match-vs-image" />
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
                  <div v-if="rightTeam.ranking > 0" class="compare-team__rank">
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
          <div class="site-shell-inner compare-content team-comparison">
            <header class="comparison-heading">
              <div class="comparison-heading__main">
                <h2>常规赛数据对比</h2>
                <span>REGULAR SEASON COMPARISON</span>
              </div>

              <div v-if="hasValidPair" class="comparison-heading__meta">
                <span>{{ comparisonHeadingMeta.scope }}</span>
                <span>{{ comparisonHeadingMeta.games }}</span>
              </div>
            </header>

            <div v-if="!hasValidPair" class="compare-empty">请选择对比球队后查看数据</div>

            <div v-else-if="compareState === 'loading'" class="compare-empty">加载中...</div>

            <div v-else-if="compareState === 'error'" class="compare-empty is-error">
              {{ compareErrorMessage || '加载对比数据失败' }}
            </div>

            <div v-else-if="compareState === 'empty' || !compareData" class="compare-empty">
              暂无对比数据
            </div>

            <template v-else-if="compareData && rightTeam">
              <section class="comparison-section">
                <div class="summary-grid">
                  <ComparisonSummaryCard
                    v-for="card in overviewCards"
                    :key="card.key"
                    :label="card.label"
                    :icon="card.icon"
                    :left-value="card.leftValue"
                    :right-value="card.rightValue"
                    :left-team-code="leftTeam.code"
                    :right-team-code="rightTeam.code"
                    :higher-is-better="card.higherIsBetter"
                    :format-type="card.formatType"
                  />
                </div>

                <section class="metric-section">
                  <h3 class="metric-section__title">基础数据</h3>
                  <div class="metric-section__rows">
                    <ComparisonMetricRow
                      v-for="stat in compareData.basics"
                      :key="stat.key"
                      :stat="stat"
                    />
                  </div>
                </section>

                <section class="metric-section">
                  <h3 class="metric-section__title">投篮表现</h3>
                  <div class="metric-section__rows">
                    <ComparisonMetricRow
                      v-for="stat in compareData.shooting"
                      :key="stat.key"
                      :stat="stat"
                    />
                  </div>
                </section>

                <div class="comparison-note">
                  <span class="comparison-note__icon" aria-hidden="true">▼</span>
                  <span>
                    数值越高通常代表表现越优；场均失分、场均失误和场均犯规为数值越低越优。
                  </span>
                </div>
              </section>
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
      :teams="teamOptions"
      :teams-state="teamsLoadState"
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
  row-gap: clamp(4px, 0.8vw, 10px);
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
  gap: clamp(24px, 2.1vw, 34px);
}

.compare-vs-img,
.match-vs-image {
  display: block;
  width: clamp(108px, 8.5vw, 142px);
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
  min-height: 46px;
  padding: 0 22px;
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
  padding-block: clamp(16px, 1.6vw, 24px) clamp(56px, 5vw, 72px);
  color: var(--cnpa-paper-text);
}

.compare-empty.is-error {
  color: var(--cnpa-red);
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
  .match-vs-image {
    width: clamp(130px, 12vw, 142px);
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
  .compare-season-pill,
  .match-season-tag {
    min-width: 0;
    width: 100%;
    padding-left: 10px;
    padding-right: 10px;
    font-size: 15px;
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
  .match-vs-image {
    width: clamp(108px, 14vw, 132px);
  }
}

@media (max-width: 640px) {
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
