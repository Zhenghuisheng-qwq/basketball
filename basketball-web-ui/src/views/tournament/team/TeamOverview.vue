<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getV4PublicMatchList } from '@/api/v4Public'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, initials } from '@/utils/publicDisplay'
import type { PublicTeamRosterItemVo } from '@/api/types/public'
import type { TeamMatchRecord } from '@/types/team'
import {
  PUBLIC_TEAM_PAGE_KEY,
  type PublicTeamPageContext,
} from './composables/usePublicTeamPage'
import { mapV4MatchToTeamRecord, opponentAsTeam } from './utils/mapTeamMatch'
import DisciplineStatusPanel from '@/components/discipline/DisciplineStatusPanel.vue'

const ctx = inject(PUBLIC_TEAM_PAGE_KEY) as PublicTeamPageContext
const router = useRouter()

const rosterPageSize = 5
const rosterPage = ref(0)
const matchPageSize = 5
const matchCurrentPage = ref(1)

const matchRecords = ref<TeamMatchRecord[]>([])
const matchState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const matchError = ref('')
let matchReqId = 0

const roster = computed(() => ctx.roster.value)
const recentMatches = computed(() => matchRecords.value)

const matchTotalPages = computed(() =>
  Math.max(1, Math.ceil(recentMatches.value.length / matchPageSize)),
)

const pagedRecentMatches = computed(() => {
  const start = (matchCurrentPage.value - 1) * matchPageSize
  return recentMatches.value.slice(start, start + matchPageSize)
})

const rosterTotalPages = computed(() =>
  Math.max(1, Math.ceil(roster.value.length / rosterPageSize)),
)

const visibleRoster = computed(() => {
  const start = rosterPage.value * rosterPageSize
  return roster.value.slice(start, start + rosterPageSize)
})

async function loadMatches(teamId: string, divisionId: string) {
  const reqId = ++matchReqId
  if (!teamId || !divisionId) {
    matchRecords.value = []
    matchState.value = 'empty'
    return
  }
  matchState.value = 'loading'
  matchError.value = ''
  try {
    const res = await getV4PublicMatchList({ divisionId, eventTeamId: teamId })
    if (reqId !== matchReqId) return
    const rows = res.data ?? []
    matchRecords.value = rows.map((item) => mapV4MatchToTeamRecord(item, teamId))
    matchState.value = matchRecords.value.length ? 'success' : 'empty'
    matchCurrentPage.value = 1
  } catch (err) {
    if (reqId !== matchReqId) return
    matchRecords.value = []
    matchState.value = 'error'
    matchError.value = err instanceof Error ? err.message : '加载比赛失败'
  }
}

watch(
  () => [ctx.teamId.value, ctx.divisionId.value] as const,
  ([teamId, divisionId]) => {
    void loadMatches(teamId, divisionId)
  },
  { immediate: true },
)

function formatDateTime(match: TeamMatchRecord): string {
  if (!match.date) return '--'
  const parts = match.date.split('-')
  const month = parts[1] || '--'
  const day = parts[2] || '--'
  return `${month}-${day} ${match.time || ''}`.trim()
}

function getStatusLabel(match: TeamMatchRecord): string {
  if (match.status === 'scheduled') return '未开始'
  if (match.resultLabel) return match.resultLabel
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

function goRosterPrev() {
  if (!rosterTotalPages.value) return
  rosterPage.value =
    (rosterPage.value - 1 + rosterTotalPages.value) % rosterTotalPages.value
}

function goRosterNext() {
  if (!rosterTotalPages.value) return
  rosterPage.value = (rosterPage.value + 1) % rosterTotalPages.value
}

function goRosterPage(index: number) {
  rosterPage.value = index
}

function getPlayerNumber(player: PublicTeamRosterItemVo): string {
  if (player.jerseyNo) return String(player.jerseyNo).padStart(2, '0')
  const match = displayText(player.playerName, '').match(/(\d+)$/)
  return match?.[1]?.padStart(2, '0') || initials(player.playerName)
}

function isCaptain(player: PublicTeamRosterItemVo): boolean {
  return (player.roleType || '').toUpperCase().includes('CAPTAIN')
}

function openPlayerDetail(player: PublicTeamRosterItemVo) {
  if (!player.playerId) return
  goToPlayerDetail(router, player.playerId, 'overview', {
    ...(ctx.divisionId.value ? { divisionId: ctx.divisionId.value } : {}),
    ...(ctx.seasonId.value ? { seasonId: ctx.seasonId.value } : {}),
  })
}

function getOpponent(match: TeamMatchRecord) {
  return opponentAsTeam(match as TeamMatchRecord & { opponentName?: string; opponentLogoUrl?: string })
}

function goMatchStats(match: TeamMatchRecord) {
  router.push({
    path: `/tournament/team/${ctx.teamId.value}/stats`,
    query: {
      matchId: match.id,
      ...(ctx.divisionId.value ? { divisionId: ctx.divisionId.value } : {}),
      ...(ctx.seasonId.value ? { seasonId: ctx.seasonId.value } : {}),
    },
  })
}

function goMatchPrevPage() {
  if (matchCurrentPage.value > 1) matchCurrentPage.value -= 1
}

function goMatchNextPage() {
  if (matchCurrentPage.value < matchTotalPages.value) matchCurrentPage.value += 1
}

function goMatchPage(page: number) {
  if (page >= 1 && page <= matchTotalPages.value) matchCurrentPage.value = page
}
</script>

<template>
  <div class="team-overview">
    <DisciplineStatusPanel
      :items="ctx.teamView.value?.activeDisciplineSummary || ctx.detail.value?.activeDisciplineSummary || []"
      title="赛事状态"
    />
    <section class="overview-section roster-section">
      <div class="section-head">
        <div class="section-head-group">
          <h2 class="section-head-title">本赛季阵容</h2>
          <span class="section-head-sub">Current Roster</span>
        </div>
      </div>

      <div v-if="ctx.state.value === 'loading'" class="overview-empty">加载中...</div>
      <div v-else-if="ctx.state.value === 'error'" class="overview-empty is-error">
        {{ ctx.errorMessage.value || '加载阵容失败' }}
      </div>
      <div v-else-if="roster.length === 0" class="overview-empty">暂无阵容数据</div>

      <template v-else>
        <div class="roster-wrap">
          <button
            type="button"
            class="roster-arrow roster-arrow--left"
            aria-label="上一页"
            @click="goRosterPrev"
          >
            ‹
          </button>

          <div class="roster-grid">
            <article
              v-for="player in visibleRoster"
              :key="player.playerId"
              class="roster-card"
              role="button"
              tabindex="0"
              @click="openPlayerDetail(player)"
              @keydown.enter="openPlayerDetail(player)"
            >
              <div class="player-avatar">
                <img
                  v-if="player.avatarUrl"
                  :src="player.avatarUrl"
                  :alt="displayText(player.playerName)"
                  class="player-avatar__img"
                />
                <span v-else class="player-avatar__placeholder">{{ getPlayerNumber(player) }}</span>
              </div>

              <div class="player-name">{{ displayText(player.playerName) }}</div>
              <span v-if="isCaptain(player)" class="captain-badge">队长</span>
              <div class="player-position">{{ displayText(player.positionCode, '--') }}</div>
            </article>
          </div>

          <button
            type="button"
            class="roster-arrow roster-arrow--right"
            aria-label="下一页"
            @click="goRosterNext"
          >
            ›
          </button>

          <div class="carousel-dots">
            <button
              v-for="index in rosterTotalPages"
              :key="index"
              type="button"
              class="dot"
              :class="{ active: rosterPage === index - 1 }"
              :aria-label="`第 ${index} 页`"
              @click="goRosterPage(index - 1)"
            />
          </div>
        </div>
      </template>
    </section>

    <section class="overview-section overview-section--matches">
      <div class="match-head">
        <div class="section-head">
          <div class="section-head-group">
            <h2 class="section-head-title">比赛记录</h2>
            <span class="section-head-sub">Match History</span>
          </div>
        </div>
      </div>

      <div v-if="matchState === 'loading'" class="overview-empty">加载中...</div>
      <div v-else-if="matchState === 'error'" class="overview-empty is-error">
        {{ matchError || '加载比赛失败' }}
      </div>
      <div v-else-if="recentMatches.length === 0" class="overview-empty">暂无比赛记录</div>

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

            <article v-for="match in pagedRecentMatches" :key="match.id" class="match-table__row">
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
                  <span class="score-number" :class="{ 'is-winner': match.result === 'win' }">
                    {{ match.teamScore ?? 0 }}
                  </span>
                  <span class="score-separator">:</span>
                  <span class="score-number" :class="{ 'is-winner': match.result === 'loss' }">
                    {{ match.opponentScore ?? 0 }}
                  </span>
                </span>
              </template>

              <span class="match-table__stage">{{ match.competition }}</span>

              <div class="match-table__actions">
                <button
                  v-if="match.status === 'scheduled'"
                  type="button"
                  class="action-btn action-btn--disabled"
                  disabled
                >
                  敬请期待
                </button>
                <button v-else type="button" class="action-btn" @click="goMatchStats(match)">
                  查看数据
                </button>
              </div>
            </article>
          </div>
        </div>

        <nav class="match-pagination" aria-label="比赛记录分页">
          <button
            type="button"
            class="match-pagination__nav"
            :disabled="matchCurrentPage <= 1"
            @click="goMatchPrevPage"
          >
            上一页
          </button>

          <div class="match-pagination__pages">
            <button
              v-for="page in matchTotalPages"
              :key="page"
              type="button"
              class="match-pagination__page"
              :class="{ active: matchCurrentPage === page }"
              @click="goMatchPage(page)"
            >
              {{ page }}
            </button>
          </div>

          <button
            type="button"
            class="match-pagination__nav"
            :disabled="matchCurrentPage >= matchTotalPages"
            @click="goMatchNextPage"
          >
            下一页
          </button>
        </nav>
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use './styles/team-overview.scss';

.overview-empty.is-error {
  color: #e10613;
}
</style>
