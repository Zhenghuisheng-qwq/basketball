<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { getV4PublicMatchList } from '@/api/v4Public'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, initials } from '@/utils/publicDisplay'
import { hasMatchSeriesId, publicContestDetailLabel, publicContestDetailPath } from '@/utils/v4Display'
import type { PublicTeamRosterItemVo } from '@/api/types/public'
import type { TeamMatchRecord } from '@/types/team'
import {
  PUBLIC_TEAM_PAGE_KEY,
  type PublicTeamPageContext,
} from './composables/usePublicTeamPage'
import { formatTeamMatchWhen, mapV4MatchToTeamRecord, opponentAsTeam } from './utils/mapTeamMatch'
import DisciplineStatusPanel from '@/components/discipline/DisciplineStatusPanel.vue'

const ctx = inject(PUBLIC_TEAM_PAGE_KEY) as PublicTeamPageContext
const router = useRouter()
const { t } = useI18n()

const rosterPageSize = 5
const rosterPage = ref(0)
const brokenAvatarIds = ref<Record<string, boolean>>({})
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
    matchError.value = err instanceof Error ? err.message : t('publicPages.team.loadMatchesFailed')
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
  return formatTeamMatchWhen(match)
}

function getStatusLabel(match: TeamMatchRecord): string {
  if (match.status === 'scheduled') return t('publicPages.team.notStarted')
  if (match.resultLabel) return match.resultLabel
  if (match.result === 'win') return t('publicPages.team.win')
  if (match.result === 'loss') return t('publicPages.team.loss')
  return t('publicPages.team.finished')
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

function hasRosterAvatar(player: PublicTeamRosterItemVo): boolean {
  return Boolean(player.avatarUrl) && !brokenAvatarIds.value[String(player.playerId)]
}

function markBrokenAvatar(playerId: string | number) {
  brokenAvatarIds.value = { ...brokenAvatarIds.value, [String(playerId)]: true }
}

function getPlayerAbbrev(player: PublicTeamRosterItemVo): string {
  const name = displayText(player.playerName, '')
  const tagged = name.match(/P\d+$/i)
  if (tagged) return tagged[0].toUpperCase()
  if (player.jerseyNo) return String(player.jerseyNo)
  const digits = name.match(/(\d+)$/)
  if (digits) return digits[1]
  return initials(player.playerName)
}

function formatCompetition(text: string) {
  const raw = (text || '').trim()
  const matched = raw.match(/^(BO\d+)\s*(.*)$/i)
  if (matched) {
    return { series: matched[1].toUpperCase(), meta: matched[2].trim() }
  }
  return { series: '', meta: raw || '--' }
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


function matchQuery() {
  return {
    ...(ctx.divisionId.value ? { divisionId: ctx.divisionId.value } : {}),
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

function goSeriesDetail(match: TeamMatchRecord) {
  const path = publicContestDetailPath({
    unitType: 'MATCH_SERIES',
    matchSeriesId: match.matchSeriesId,
  })
  if (!path) return
  router.push({
    path,
    query: matchQuery(),
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
      :title="t('publicPages.team.eventStatus')"
    />
    <section class="overview-section roster-section">
      <div class="section-head">
        <div class="section-head-group">
          <h2 class="section-head-title">{{ t('publicPages.team.seasonRoster') }}</h2>
          <span class="section-head-sub">Current Roster</span>
        </div>
      </div>

      <div v-if="ctx.state.value === 'loading'" class="overview-empty">{{ t('publicPages.team.loading') }}</div>
      <div v-else-if="ctx.state.value === 'error'" class="overview-empty is-error">
        {{ ctx.errorMessage.value || t('publicPages.team.loadRosterFailed') }}
      </div>
      <div v-else-if="roster.length === 0" class="overview-empty">{{ t('publicPages.team.noRoster') }}</div>

      <template v-else>
        <div class="roster-wrap" :class="{ 'is-paged': rosterTotalPages > 1 }">
          <button
            v-if="rosterTotalPages > 1"
            type="button"
            class="roster-arrow roster-arrow--left"
            :aria-label="t('publicPages.team.prevPage')"
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
                  v-if="hasRosterAvatar(player)"
                  :src="player.avatarUrl || undefined"
                  :alt="displayText(player.playerName)"
                  class="player-avatar__img"
                  @error="markBrokenAvatar(player.playerId)"
                />
                <span v-else class="player-avatar__placeholder">{{ getPlayerAbbrev(player) }}</span>
              </div>

              <div class="player-name">{{ displayText(player.playerName) }}</div>
              <span v-if="isCaptain(player)" class="captain-badge">{{ t('publicPages.team.captain') }}</span>
              <div class="player-position">{{ displayText(player.positionCode, '--') }}</div>
            </article>
          </div>

          <button
            v-if="rosterTotalPages > 1"
            type="button"
            class="roster-arrow roster-arrow--right"
            :aria-label="t('publicPages.team.nextPage')"
            @click="goRosterNext"
          >
            ›
          </button>

          <div v-if="rosterTotalPages > 1" class="carousel-dots">
            <button
              v-for="index in rosterTotalPages"
              :key="index"
              type="button"
              class="dot"
              :class="{ active: rosterPage === index - 1 }"
              :aria-label="t('publicPages.team.pageN', { page: index })"
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
            <h2 class="section-head-title">{{ t('publicPages.team.matchLog') }}</h2>
            <span class="section-head-sub">Match History</span>
          </div>
        </div>
      </div>

      <div v-if="matchState === 'loading'" class="overview-empty">{{ t('publicPages.team.loading') }}</div>
      <div v-else-if="matchState === 'error'" class="overview-empty is-error">
        {{ matchError || t('publicPages.team.loadMatchesFailed') }}
      </div>
      <div v-else-if="recentMatches.length === 0" class="overview-empty">{{ t('publicPages.team.noMatches') }}</div>

      <template v-else>
        <div class="match-list">
          <div class="match-table">
            <div class="match-history-grid match-table__head">
              <span>{{ t('publicPages.team.status') }}</span>
              <span>{{ t('publicPages.team.dateTime') }}</span>
              <span>{{ t('publicPages.team.opponent') }}</span>
              <span>{{ t('publicPages.team.score') }}</span>
              <span>{{ t('publicPages.team.stage') }}</span>
              <span>{{ t('publicPages.team.view') }}</span>
            </div>

            <article v-for="match in pagedRecentMatches" :key="match.id" class="match-history-grid match-table__row">
              <span class="match-table__status" :class="getStatusClass(match)">
                {{ getStatusLabel(match) }}
              </span>

              <span class="match-table__datetime">{{ formatDateTime(match) }}</span>

              <div class="match-table__opponent">
                <TeamLogo :team="getOpponent(match)" size="sm" class="match-table__opponent-logo" />
                <span :title="getOpponent(match).name">{{ getOpponent(match).name }}</span>
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

              <span class="match-table__stage">
                <strong v-if="formatCompetition(match.competition).series">{{
                  formatCompetition(match.competition).series
                }}</strong>
                <em>{{ formatCompetition(match.competition).meta }}</em>
              </span>

              <div class="match-table__actions">
                <span v-if="match.status === 'scheduled'" class="action-link is-disabled">{{ t('publicPages.team.comingSoon') }}</span>
                <template v-else>
                  <button type="button" class="action-link" @click="goMatchStats(match)">
                    {{ t('publicPages.team.boxScore') }}
                  </button>
                  <button type="button" class="action-link" @click="goMatchDetail(match)">
                    {{ publicContestDetailLabel('MATCH') }} →
                  </button>
                  <button
                    v-if="hasMatchSeriesId(match.matchSeriesId)"
                    type="button"
                    class="action-link"
                    @click="goSeriesDetail(match)"
                  >
                    {{ publicContestDetailLabel('MATCH_SERIES') }} →
                  </button>
                </template>
              </div>
            </article>
          </div>
        </div>

        <nav class="match-pagination" :aria-label="t('publicPages.team.matchPagination')">
          <button
            type="button"
            class="match-pagination__nav"
            :disabled="matchCurrentPage <= 1"
            @click="goMatchPrevPage"
          >
            {{ t('publicPages.team.prevPage') }}
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
            {{ t('publicPages.team.nextPage') }}
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
