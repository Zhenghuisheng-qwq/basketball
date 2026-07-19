<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTeamById, getTeamMatches } from '@/mock/team'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { TeamMatchRecord, TeamMatchStatus, TeamStage } from '@/types/team'

type StatusFilter = 'all' | TeamMatchStatus
type StageFilter = 'all' | TeamStage

interface MonthGroup {
  key: string
  label: string
  matches: TeamMatchRecord[]
}

const pageSize = 8

const route = useRoute()
const router = useRouter()

const teamId = computed(() => route.params.teamId as string)
const allMatches = computed(() => getTeamMatches(teamId.value))

const statusFilter = ref<StatusFilter>('all')
const competitionFilter = ref('2026 CNPA League')
const stageFilter = ref<StageFilter>('all')
const currentPage = ref(1)

const competitionOptions = ['2026 CNPA League']

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
      if (competitionFilter.value && match.competition !== competitionFilter.value) {
        return false
      }
      if (stageFilter.value !== 'all' && match.stage !== stageFilter.value) {
        return false
      }
      return true
    })
    .sort((a, b) => {
      const dateCompare = a.date.localeCompare(b.date)
      return dateCompare !== 0 ? dateCompare : a.time.localeCompare(b.time)
    })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / pageSize)))

const paginatedMatches = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMatches.value.slice(start, start + pageSize)
})

const groupedMatches = computed<MonthGroup[]>(() => {
  const groups = new Map<string, MonthGroup>()

  for (const match of paginatedMatches.value) {
    const [year, month] = match.date.split('-')
    const key = `${year}-${month}`
    const existing = groups.get(key)

    if (existing) {
      existing.matches.push(match)
      continue
    }

    groups.set(key, {
      key,
      label: `${year}年${Number(month)}月`,
      matches: [match],
    })
  }

  return Array.from(groups.values())
})

watch([statusFilter, competitionFilter, stageFilter], () => {
  currentPage.value = 1
})

watch(totalPages, (pages) => {
  if (currentPage.value > pages) {
    currentPage.value = pages
  }
})

function getOpponent(match: TeamMatchRecord) {
  return getTeamById(match.opponentId)
}

function formatDateTime(match: TeamMatchRecord): string {
  const [, month, day] = match.date.split('-')
  return `${month}-${day} ${match.time}`
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
              <option v-for="option in competitionOptions" :key="option" :value="option">
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

    <div v-if="filteredMatches.length === 0" class="matches-empty">暂无符合条件的比赛</div>

    <template v-else>
      <div class="match-groups">
        <section v-for="group in groupedMatches" :key="group.key" class="match-group">
          <div class="match-group__title">
            <span class="match-group__bar" aria-hidden="true" />
            <h3>{{ group.label }}</h3>
          </div>

          <div class="match-table">
              <div class="match-table__head">
                <span>状态</span>
                <span>日期时间</span>
                <span>对手</span>
                <span>比分</span>
                <span>赛段</span>
                <span>查看</span>
              </div>

              <article v-for="match in group.matches" :key="match.id" class="match-table__row">
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
                      {{ match.teamScore }}
                    </span>
                    <span class="score-separator">:</span>
                    <span
                      class="score-number"
                      :class="{ 'is-winner': match.result === 'loss' }"
                    >
                      {{ match.opponentScore }}
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
        </section>
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
    color: var(--cnpa-red);
    font-style: italic;
    text-transform: uppercase;
    font-weight: 900;
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
  background: rgba(255, 255, 255, 0.62);
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
  background: rgba(255, 255, 255, 0.62);
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
  background: rgba(255, 255, 255, 0.22);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.match-groups {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.match-group {
  overflow-x: auto;
}

.match-group__title {
  height: 48px;
  padding: 0 22px;
  display: flex;
  align-items: center;
  background: linear-gradient(180deg, #111 0%, #050505 100%);
  color: #fff;
  border-radius: 2px 2px 0 0;
  margin-bottom: 0;
}

.match-group__bar {
  flex-shrink: 0;
  width: 4px;
  height: 22px;
  background: var(--cnpa-red);
  margin-right: 12px;
}

.match-group__title h3 {
  margin: 0;
  font-size: clamp(22px, 2vw, 24px);
  font-weight: 900;
}

.match-table {
  border: 1px solid var(--cnpa-paper-border);
  border-top: none;
  background: rgba(255, 255, 255, 0.32);
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
  background: rgba(255, 255, 255, 0.46);
  color: #171717;
  font-size: 16px;
  font-weight: 900;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.match-table__row {
  min-height: 76px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.24);
  transition: background 0.2s ease;

  &:first-of-type {
    border-top: none;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.38);
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
  background: rgba(255, 255, 255, 0.62);
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
    background: rgba(255, 255, 255, 0.78);
  }
}

.action-btn--disabled,
.action-btn:disabled {
  color: #666;
  background: rgba(255, 255, 255, 0.48);
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
  background: rgba(255, 255, 255, 0.62);
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
