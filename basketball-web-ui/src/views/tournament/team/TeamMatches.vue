<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

function getScoreText(match: TeamMatchRecord): string {
  if (match.status === 'scheduled') return 'VS'
  return `${match.teamScore}:${match.opponentScore}`
}

function handleReserveClick() {
  ElMessage.info('预约功能暂未开放')
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
    <div class="section-head">
      <div class="section-head-group">
        <h2 class="section-head-title">球队比赛</h2>
        <span class="section-head-sub">Team Matches</span>
      </div>
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
                <TeamLogo :team="getOpponent(match)" size="sm" class="match-table__opponent-logo" />
                <span>{{ getOpponent(match).name }}</span>
              </div>

              <span
                class="match-table__score"
                :class="{ 'is-vs': match.status === 'scheduled' }"
              >
                {{ getScoreText(match) }}
              </span>

              <span class="match-table__stage">{{ stageLabels[match.stage] }}</span>

              <div class="match-table__actions">
                <template v-if="match.status === 'scheduled'">
                  <button type="button" class="action-btn" @click="handleReserveClick">
                    预约
                  </button>
                </template>
                <template v-else>
                  <button type="button" class="action-btn" @click="goMatchStats(match)">
                    技术统计
                  </button>
                </template>
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
    color: var(--cnpa-paper-text);
  }

  .section-head-sub {
    color: var(--cnpa-paper-text-muted);
  }
}

.match-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}

.status-tabs {
  display: inline-flex;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.status-tab {
  min-width: 88px;
  height: 40px;
  padding: 0 16px;
  border: none;
  border-right: 1px solid rgba(17, 17, 17, 0.1);
  background: transparent;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

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
  min-width: 160px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.85);
  color: var(--cnpa-paper-text);
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
}

.matches-empty {
  padding: 48px 16px;
  text-align: center;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.55);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.match-groups {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.match-group__title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: #111;
  color: #fff;
}

.match-group__bar {
  flex-shrink: 0;
  width: 4px;
  height: 18px;
  background: var(--cnpa-red);
}

.match-group__title h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.match-table {
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.72);
}

.match-table__head,
.match-table__row {
  display: grid;
  grid-template-columns: 72px 120px 1.2fr 100px 88px 180px;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
}

.match-table__head {
  min-height: 48px;
  background: rgba(255, 255, 255, 0.55);
  color: var(--cnpa-paper-text-muted);
  font-size: 12px;
  font-weight: 800;
}

.match-table__row {
  min-height: 88px;
  border-top: 1px solid rgba(17, 17, 17, 0.1);
  transition: background 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.85);
  }
}

.match-table__status {
  font-size: 13px;
  font-weight: 800;

  &.is-win {
    color: var(--cnpa-red);
  }

  &.is-loss {
    color: rgba(17, 17, 17, 0.45);
  }

  &.is-scheduled,
  &.is-finished {
    color: var(--cnpa-paper-text-muted);
  }
}

.match-table__datetime {
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-paper-text);
}

.match-table__opponent {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  font-size: 13px;
  font-weight: 800;
  color: var(--cnpa-paper-text);
}

.match-table__opponent-logo {
  flex-shrink: 0;
}

.match-table__score {
  font-size: 18px;
  font-weight: 900;
  color: var(--cnpa-paper-text);

  &.is-vs {
    color: var(--cnpa-paper-text-muted);
  }
}

.match-table__stage {
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.match-table__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  height: 32px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 32px;
}

.pagination-btn {
  min-width: 88px;
  height: 36px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover:not(:disabled) {
    border-color: var(--cnpa-red);
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
  min-width: 36px;
  height: 36px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

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
  .match-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-selects {
    width: 100%;
  }

  .filter-select select {
    width: 100%;
  }

  .match-table__head {
    display: none;
  }

  .match-table__row {
    grid-template-columns: 1fr;
    padding: 16px;
    gap: 10px;
  }

  .match-table__actions {
    flex-direction: row;
    flex-wrap: wrap;
  }
}
</style>
