<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getPublicTeamMatchPage } from '@/api/publicTeam'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { TeamMatchRecord, TeamMatchStatus, TeamStage } from '@/types/team'
import {
  PUBLIC_TEAM_PAGE_KEY,
  type PublicTeamPageContext,
} from './composables/usePublicTeamPage'
import { mapPublicMatchToTeamRecord, opponentAsTeam } from './utils/mapTeamMatch'

type StatusFilter = 'all' | TeamMatchStatus
type StageFilter = 'all' | TeamStage

const pageSize = 8

const ctx = inject(PUBLIC_TEAM_PAGE_KEY) as PublicTeamPageContext
const router = useRouter()

const allMatches = ref<TeamMatchRecord[]>([])
const loadState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const teamId = computed(() => ctx.teamId.value)

const statusFilter = ref<StatusFilter>('all')
const competitionFilter = ref('all')
const stageFilter = ref<StageFilter>('all')
const currentPage = ref(1)

const competitionOptions = computed(() => {
  const values = new Set<string>()
  for (const match of allMatches.value) {
    if (match.competition) values.add(match.competition)
  }
  return ['all', ...Array.from(values)]
})

const stageOptions: Array<{ value: StageFilter; label: string }> = [
  { value: 'all', label: '全部赛段' },
  { value: 'regular', label: '常规赛' },
  { value: 'playoffs', label: '季后赛' },
]

const statusTabs: Array<{ value: StatusFilter; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'scheduled', label: '未开始' },
  { value: 'finished', label: '已结束' },
]

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

const filteredMatches = computed(() => {
  return allMatches.value
    .filter((match) => {
      if (statusFilter.value !== 'all' && match.status !== statusFilter.value) {
        return false
      }
      if (
        competitionFilter.value !== 'all' &&
        match.competition !== competitionFilter.value
      ) {
        return false
      }
      if (stageFilter.value !== 'all' && match.stage !== stageFilter.value) {
        return false
      }
      return true
    })
    .sort((a, b) => {
      const dateCompare = (a.date || '').localeCompare(b.date || '')
      return dateCompare !== 0 ? dateCompare : (a.time || '').localeCompare(b.time || '')
    })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / pageSize)))

const paginatedMatches = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMatches.value.slice(start, start + pageSize)
})

async function loadMatches() {
  const id = teamId.value
  const current = ++reqId
  if (!id) {
    allMatches.value = []
    loadState.value = 'empty'
    return
  }
  loadState.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getPublicTeamMatchPage(id, { currentPage: 1, pageSize: 200 })
    if (current !== reqId) return
    const records = res.data?.records ?? []
    allMatches.value = records.map((item) => mapPublicMatchToTeamRecord(item, id))
    loadState.value = allMatches.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    allMatches.value = []
    loadState.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载比赛失败'
  }
}

watch(teamId, () => {
  void loadMatches()
}, { immediate: true })

watch([statusFilter, competitionFilter, stageFilter], () => {
  currentPage.value = 1
})

watch(totalPages, (pages) => {
  if (currentPage.value > pages) {
    currentPage.value = pages
  }
})

function getOpponent(match: TeamMatchRecord) {
  return opponentAsTeam(match)
}

function formatDateTime(match: TeamMatchRecord): string {
  if (!match.date) return match.time || '--'
  const [, month, day] = match.date.split('-')
  return `${month}-${day} ${match.time || ''}`.trim()
}

function getStatusLabel(match: TeamMatchRecord): string {
  if (match.status === 'scheduled') return '未开始'
  if (match.result === 'win') return '胜利'
  if (match.result === 'loss') return '失败'
  return '已结束'
}

function getStatusClass(match: TeamMatchRecord): string {
  if (match.status === 'scheduled') return 'is-scheduled'
  if (match.result === 'win') return 'is-win'
  if (match.result === 'loss') return 'is-loss'
  return 'is-finished'
}

function displayScore(value: number | undefined): number {
  return value ?? 0
}

function goMatchStats(match: TeamMatchRecord) {
  router.push({
    path: `/tournament/team/${teamId.value}/stats`,
    query: { matchId: match.id },
  })
}

function goPrevPage() {
  if (currentPage.value > 1) currentPage.value -= 1
}

function goNextPage() {
  if (currentPage.value < totalPages.value) currentPage.value += 1
}

function goPage(page: number) {
  currentPage.value = page
}
</script>

<template>
  <div class="team-matches">
    <div class="team-matches__toolbar">
      <div class="section-head-group">
        <h2 class="section-head-title">球队比赛</h2>
        <span class="section-head-sub">Team Matches</span>
      </div>

      <div class="match-filters">
        <div class="status-tabs">
          <button
            v-for="tab in statusTabs"
            :key="tab.value"
            type="button"
            class="status-tab"
            :class="{ active: statusFilter === tab.value }"
            @click="statusFilter = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="filter-selects">
          <label class="filter-select">
            <select v-model="competitionFilter">
              <option value="all">全部赛事</option>
              <option
                v-for="option in competitionOptions.filter((item) => item !== 'all')"
                :key="option"
                :value="option"
              >
                {{ option }}
              </option>
            </select>
          </label>

          <label class="filter-select">
            <select v-model="stageFilter">
              <option v-for="option in stageOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </label>
        </div>
      </div>
    </div>

    <div v-if="loadState === 'loading'" class="matches-empty">加载中...</div>
    <div v-else-if="loadState === 'error'" class="matches-empty is-error">
      {{ errorMessage || '加载比赛失败' }}
    </div>
    <div v-else-if="filteredMatches.length === 0" class="matches-empty">暂无符合条件的比赛</div>

    <template v-else>
      <div class="match-list">
        <div class="match-table">
          <div class="match-table__head">
            <span>状态</span>
            <span>日期时间</span>
            <span>对手</span>
            <span>比分</span>
            <span>赛段</span>
            <span>查看</span>
          </div>

          <article v-for="match in paginatedMatches" :key="match.id" class="match-table__row">
            <span class="match-table__status" :class="getStatusClass(match)">
              {{ getStatusLabel(match) }}
            </span>

            <span class="match-table__datetime">{{ formatDateTime(match) }}</span>

            <div class="match-table__opponent">
              <TeamLogo :team="getOpponent(match)" size="md" class="match-table__opponent-logo" />
              <span>{{ getOpponent(match).name }}</span>
            </div>

            <template v-if="match.status === 'scheduled'">
              <span class="match-table__score is-vs">VS</span>
            </template>
            <template v-else>
              <span class="match-table__score">
                <span
                  class="score-number"
                  :class="{ 'is-winner': match.result === 'win' }"
                >
                  {{ displayScore(match.teamScore) }}
                </span>
                <span class="score-separator">:</span>
                <span
                  class="score-number"
                  :class="{ 'is-winner': match.result === 'loss' }"
                >
                  {{ displayScore(match.opponentScore) }}
                </span>
              </span>
            </template>

            <span class="match-table__stage">{{ stageLabels[match.stage] }}</span>

            <div class="match-table__actions">
              <button
                v-if="match.status === 'scheduled'"
                type="button"
                class="action-btn action-btn--disabled"
                disabled
              >
                敬请期待
              </button>
              <button
                v-else
                type="button"
                class="action-btn"
                @click="goMatchStats(match)"
              >
                查看数据
              </button>
            </div>
          </article>
        </div>
      </div>

      <div class="pagination">
        <button
          type="button"
          class="pagination-btn"
          :disabled="currentPage <= 1"
          @click="goPrevPage"
        >
          上一页
        </button>

        <div class="pagination-pages">
          <button
            v-for="page in totalPages"
            :key="page"
            type="button"
            class="pagination-page"
            :class="{ active: currentPage === page }"
            @click="goPage(page)"
          >
            {{ page }}
          </button>
        </div>

        <button
          type="button"
          class="pagination-btn"
          :disabled="currentPage >= totalPages"
          @click="goNextPage"
        >
          下一页
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.team-matches {
  .section-head-title {
    font-size: clamp(34px, 3.2vw, 38px);
    font-weight: 900;
    color: var(--cnpa-paper-text);
  }

  .section-head-sub {
    color: #e51b23;
    font-style: italic;
    text-transform: uppercase;
    font-weight: 800;
    letter-spacing: 0.02em;
  }
}

.team-matches__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
  flex-wrap: wrap;
}

.match-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.status-tabs {
  display: inline-flex;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 2px;
  overflow: hidden;
}

.status-tab {
  min-width: 92px;
  height: 44px;
  padding: 0 16px;
  border: none;
  border-right: 1px solid rgba(0, 0, 0, 0.1);
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  border-radius: 0;
  transition: background 0.2s ease, color 0.2s ease;

  &:last-child {
    border-right: none;
  }

  &.active {
    background: var(--cnpa-red);
    color: #fff;
  }
}

.filter-selects {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.filter-select select {
  height: 44px;
  padding: 0 12px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 2px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text);
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
}

.filter-select:first-child select {
  min-width: 210px;
  max-width: 240px;
}

.filter-select:last-child select {
  min-width: 150px;
  max-width: 170px;
}

.matches-empty {
  padding: 48px 16px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: rgba(25, 25, 25, 0.025);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;

  &.is-error {
    color: var(--cnpa-red);
  }
}

.match-list {
  overflow-x: auto;
}

.match-table {
  border: 1px solid var(--cnpa-paper-border);
  background: rgba(25, 25, 25, 0.03);
  overflow: hidden;
}

.match-table__head,
.match-table__row {
  display: grid;
  grid-template-columns: 120px 190px minmax(260px, 1fr) 150px 140px 260px;
  align-items: center;
  column-gap: 12px;
  padding: 0 28px;
}

.match-table__head > * {
  justify-self: center;
  text-align: center;
}

.match-table__head {
  min-height: 54px;
  background: var(--cnpa-paper-surface);
  color: #171717;
  font-size: 16px;
  font-weight: 900;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.match-table__row {
  min-height: 76px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(25, 25, 25, 0.025);
  transition: background 0.2s ease;

  &:first-of-type {
    border-top: none;
  }

  &:hover {
    background: rgba(25, 25, 25, 0.035);
  }
}

.match-table__status {
  justify-self: center;
  text-align: center;
  font-size: 18px;
  font-weight: 900;

  &.is-win {
    color: var(--cnpa-red);
  }

  &.is-loss {
    color: #333;
  }

  &.is-scheduled,
  &.is-finished {
    color: #666;
  }
}

.match-table__datetime {
  justify-self: center;
  text-align: center;
  font-size: 17px;
  font-weight: 800;
  color: #555;
  letter-spacing: 0.02em;
}

.match-table__opponent {
  justify-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-width: 0;
  font-size: 16px;
  font-weight: 800;
  color: #333;

  span {
    white-space: nowrap;
  }
}

.match-table__opponent-logo {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
}

.match-table__score {
  justify-self: center;
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 7px;
  font-size: 28px;
  font-weight: 900;
  line-height: 1;
  color: #111;
}

.score-number {
  color: #111;
}

.score-number.is-winner {
  color: var(--cnpa-red);
}

.score-separator {
  color: #111;
}

.match-table__score.is-vs {
  font-size: 24px;
  color: #111;
}

.match-table__stage {
  justify-self: center;
  text-align: center;
  font-size: 16px;
  font-weight: 800;
  color: #555;
}

.match-table__actions {
  justify-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.action-btn {
  min-width: 92px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: var(--cnpa-paper-surface);
  color: #222;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
  border-radius: 2px;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
    background: var(--cnpa-paper-surface);
  }
}

.action-btn--disabled,
.action-btn:disabled {
  color: #666;
  background: var(--cnpa-paper-surface);
  border-color: rgba(0, 0, 0, 0.12);
  cursor: not-allowed;
}

.pagination {
  margin-top: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.pagination-btn {
  border: none;
  background: transparent;
  color: #777;
  min-width: auto;
  height: 32px;
  padding: 0 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s ease;

  &:hover:not(:disabled) {
    color: var(--cnpa-red);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
}

.pagination-pages {
  display: flex;
  gap: 8px;
}

.pagination-page {
  width: 30px;
  min-width: 30px;
  height: 30px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  background: var(--cnpa-paper-surface);
  color: #333;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
  border-radius: 2px;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    border-color: var(--cnpa-red);
    color: #fff;
  }

  &:hover:not(.active) {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

@media (max-width: 992px) {
  .team-matches__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .match-filters {
    width: 100%;
    justify-content: flex-start;
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
  }

  .status-tabs {
    width: auto;
  }

  .status-tab {
    flex: none;
  }

  .filter-selects {
    width: auto;
  }

  .filter-select select {
    width: auto;
    max-width: none;
  }

  .match-table {
    min-width: 1140px;
  }
}
</style>
