<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getV4PublicMatchList } from '@/api/v4Public'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { TeamMatchRecord, TeamMatchStatus, TeamStage } from '@/types/team'
import {
  PUBLIC_TEAM_PAGE_KEY,
  type PublicTeamPageContext,
} from './composables/usePublicTeamPage'
import { mapV4MatchToTeamRecord, opponentAsTeam } from './utils/mapTeamMatch'
import { publicContestDetailPath } from '@/utils/v4Display'

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

const divisionId = computed(() => ctx.divisionId.value)

async function loadMatches() {
  const id = teamId.value
  const divId = divisionId.value
  const current = ++reqId
  if (!id || !divId) {
    allMatches.value = []
    loadState.value = 'empty'
    return
  }
  loadState.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getV4PublicMatchList({ divisionId: divId, eventTeamId: id })
    if (current !== reqId) return
    const records = res.data ?? []
    allMatches.value = records.map((item) => mapV4MatchToTeamRecord(item, id))
    loadState.value = allMatches.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    allMatches.value = []
    loadState.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载比赛失败'
  }
}

watch([teamId, divisionId], () => {
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
  if (match.resultLabel) return match.resultLabel
  if (match.result === 'win') return '胜利'
  if (match.result === 'loss') return '失败'
  if (match.teamScore != null && match.opponentScore != null) return '进行中'
  return '已结束'
}

function getStatusClass(match: TeamMatchRecord): string {
  if (match.status === 'scheduled') return 'is-scheduled'
  if (match.result === 'win') return 'is-win'
  if (match.result === 'loss') return 'is-loss'
  if (match.teamScore != null && match.opponentScore != null) return 'is-live'
  return 'is-finished'
}

function goMatchStats(match: TeamMatchRecord) {
  router.push({
    path: `/tournament/team/${teamId.value}/stats`,
    query: {
      matchId: match.id,
      ...(divisionId.value ? { divisionId: divisionId.value } : {}),
    },
  })
}


function matchQuery() {
  return {
    ...(divisionId.value ? { divisionId: divisionId.value } : {}),
    ...(ctx.seasonId.value ? { seasonId: ctx.seasonId.value } : {}),
  }
}

function goMatchDetail(match: TeamMatchRecord) {
  if (match.status === 'scheduled') return
  const path = publicContestDetailPath({
    unitType: 'MATCH',
    matchId: match.id,
  })
  if (!path) return
  router.push({
    path,
    query: matchQuery(),
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
              <TeamLogo :team="getOpponent(match)" size="sm" class="match-table__opponent-logo" />
              <span>{{ getOpponent(match).name }}</span>
            </div>

            <template v-if="match.status === 'scheduled'">
              <span class="match-table__score is-vs">VS</span>
            </template>
            <template v-else>
              <span class="match-table__score">
                <template v-if="match.resultLabel">
                  <span class="score-number">{{ match.resultLabel }}</span>
                  <small
                    v-if="match.originalTeamScore != null && match.originalOpponentScore != null"
                    class="score-original"
                  >
                    原比分 {{ match.originalTeamScore }}:{{ match.originalOpponentScore }} 成绩已取消
                  </small>
                </template>
                <template v-else-if="match.teamScore != null && match.opponentScore != null">
                <span
                  class="score-number"
                  :class="{ 'is-winner': match.result === 'win' }"
                >
                  {{ match.teamScore }}
                </span>
                <span class="score-separator">:</span>
                <span
                  class="score-number"
                  :class="{ 'is-winner': match.result === 'loss' }"
                >
                  {{ match.opponentScore }}
                </span>
                </template>
                <template v-else>
                  <span class="score-number">{{ getStatusLabel(match) }}</span>
                </template>
              </span>
            </template>

            <span class="match-table__stage">
              <strong>{{ match.stageHeadline || stageLabels[match.stage] }}</strong>
              <em v-if="match.stageMeta">{{ match.stageMeta }}</em>
            </span>

            <div class="match-table__actions">
              <span v-if="match.status === 'scheduled'" class="action-link is-disabled">敬请期待</span>
              <template v-else>
                <button type="button" class="action-link" @click="goMatchStats(match)">
                  数据统计
                </button>
                <button type="button" class="action-link action-link--strong" @click="goMatchDetail(match)">
                  比赛详情 →
                </button>
              </template>
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
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  box-sizing: border-box;
}

.section-head-group {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  min-width: 0;
}

.section-head-title {
  position: relative;
  margin: 0;
  padding-left: 12px;
  color: var(--cnpa-paper-text);
  font-size: 28px;
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.02em;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 3px;
    bottom: 3px;
    width: 4px;
    background: var(--cnpa-red);
  }
}

.section-head-sub {
  color: #e51b23;
  font-size: 16px;
  line-height: 1;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.02em;
  text-transform: uppercase;
  white-space: nowrap;
}

.team-matches__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px 24px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.match-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.status-tabs {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.status-tab {
  min-width: 72px;
  height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: 2px;
  background: #fff;
  color: #333;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s ease, color 0.2s ease, border-color 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    border-color: var(--cnpa-red);
    color: #fff;
  }
}

.filter-selects {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;

  &::after {
    content: '▾';
    position: absolute;
    right: 10px;
    color: #777;
    font-size: 10px;
    pointer-events: none;
  }
}

.filter-select select {
  height: 36px;
  padding: 0 28px 0 12px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: 2px;
  background: #fff;
  color: #171717;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  appearance: none;
  -webkit-appearance: none;
  cursor: pointer;

  &:focus {
    outline: none;
  }
}

.filter-select:first-child select {
  min-width: 132px;
  max-width: 168px;
}

.filter-select:last-child select {
  min-width: 108px;
  max-width: 140px;
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
  min-width: 880px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: #fff;
  overflow: hidden;
}

.match-table__head,
.match-table__row {
  display: grid;
  grid-template-columns: 0.7fr 1.4fr 2.7fr 1.5fr 1.55fr 1.15fr;
  align-items: center;
  column-gap: 8px;
  padding: 0 22px 0 16px;
}

.match-table__head {
  min-height: 42px;
  background: #f3f2f0;
  color: #171717;
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);

  > :nth-child(1),
  > :nth-child(4) {
    justify-self: center;
    text-align: center;
  }

  > :nth-child(2),
  > :nth-child(3),
  > :nth-child(5) {
    justify-self: start;
    text-align: left;
  }

  > :nth-child(6) {
    justify-self: end;
    text-align: right;
  }
}

.match-table__row {
  min-height: 60px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;

  &:first-of-type {
    border-top: none;
  }

  &:nth-of-type(even) {
    background: #faf9f7;
  }

  &:hover {
    background: #f6f1f1;
  }
}

.match-table__status {
  justify-self: center;
  text-align: center;
  font-size: 15px;
  font-weight: 800;

  &.is-win,
  &.is-live {
    color: var(--cnpa-red);
  }

  &.is-loss {
    color: #444;
  }

  &.is-scheduled,
  &.is-finished {
    color: #888;
  }
}

.match-table__datetime {
  justify-self: start;
  text-align: left;
  font-size: 14px;
  font-weight: 600;
  color: #666;
  letter-spacing: 0.02em;
}

.match-table__opponent {
  justify-self: start;
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  gap: 9px;
  min-width: 0;
  font-size: 15px;
  font-weight: 700;
  color: #333;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.match-table__opponent-logo {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
}

.match-table__opponent :deep(.team-logo) {
  width: 32px;
  height: 32px;
}

.match-table__score {
  justify-self: center;
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  min-width: 5.2em;
  font-size: 22px;
  font-weight: 900;
  line-height: 1;
  color: #111;
  font-variant-numeric: tabular-nums;
}

.score-number {
  min-width: 1.25em;
  color: #111;
  text-align: center;
  font-variant-numeric: tabular-nums;

  &.is-winner {
    color: var(--cnpa-red);
  }
}

.score-original {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
  color: #666;
}

.score-separator {
  color: #111;
}

.match-table__score.is-vs {
  font-size: 20px;
  color: #111;
}

.match-table__stage {
  justify-self: start;
  display: inline-flex;
  align-items: baseline;
  justify-content: flex-start;
  gap: 6px;
  min-width: 0;
  font-size: 13px;
  color: #8a8a8a;

  strong {
    color: #222;
    font-size: 13px;
    font-weight: 600;
  }

  em {
    color: #8a8a8a;
    font-size: 13px;
    font-style: normal;
    font-weight: 500;
  }
}

.match-table__actions {
  justify-self: end;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
}

.action-link {
  border: none;
  padding: 0;
  background: transparent;
  color: #555;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s ease;

  &:hover:not(.is-disabled) {
    color: var(--cnpa-red);
  }

  &--strong {
    color: #333;
  }

  &.is-disabled {
    color: #999;
    cursor: default;
  }
}

.pagination {
  margin-top: 18px;
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
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: transparent;
  color: #9a9a9a;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  border-radius: 2px;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    border-color: var(--cnpa-red);
    color: #fff;
    font-weight: 800;
  }

  &:hover:not(.active) {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.status-tab:focus-visible,
.filter-select select:focus-visible,
.action-link:focus-visible,
.pagination-btn:focus-visible,
.pagination-page:focus-visible {
  outline: 2px solid rgba(225, 6, 19, 0.4);
  outline-offset: 2px;
}

@media (max-width: 992px) {
  .team-matches__toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .match-filters {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .match-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .status-tabs,
  .filter-selects {
    width: 100%;
  }

  .filter-select,
  .filter-select select {
    width: 100%;
    max-width: none;
  }
}
</style>
