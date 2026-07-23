<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getTeamById,
  getTeamRecentMatches,
  getTeamRoster,
} from '@/mock/team'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import type { TeamMatchRecord, TeamRosterPlayer, TeamStage } from '@/types/team'

const route = useRoute()
const router = useRouter()

const rosterPageSize = 5
const rosterPage = ref(0)

const matchPageSize = 5
const matchCurrentPage = ref(1)
const matchStageFilter = ref<TeamStage>('regular')

const teamId = computed(() => route.params.teamId as string)
const team = computed(() => getTeamById(teamId.value))
const roster = computed(() => getTeamRoster(teamId.value))
const recentMatches = computed(() => getTeamRecentMatches(teamId.value))

const filteredRecentMatches = computed(() =>
  recentMatches.value.filter((match) => match.stage === matchStageFilter.value),
)

const matchTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredRecentMatches.value.length / matchPageSize)),
)

const pagedRecentMatches = computed(() => {
  const start = (matchCurrentPage.value - 1) * matchPageSize
  return filteredRecentMatches.value.slice(start, start + matchPageSize)
})

const rosterTotalPages = computed(() =>
  Math.max(1, Math.ceil(roster.value.length / rosterPageSize)),
)

const visibleRoster = computed(() => {
  const start = rosterPage.value * rosterPageSize
  return roster.value.slice(start, start + rosterPageSize)
})

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
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

function goRosterPrev() {
  rosterPage.value =
    (rosterPage.value - 1 + rosterTotalPages.value) % rosterTotalPages.value
}

function goRosterNext() {
  rosterPage.value = (rosterPage.value + 1) % rosterTotalPages.value
}

function goRosterPage(index: number) {
  rosterPage.value = index
}

function getPlayerNumber(player: TeamRosterPlayer): string {
  const match = player.nickname.match(/(\d+)$/)
  return match ? match[1].padStart(2, '0') : player.nickname.slice(-2)
}

function openPlayerDetail(player: TeamRosterPlayer) {
  goToPlayerDetail(router, player.id, 'overview')
}

function getOpponent(match: TeamMatchRecord) {
  return getTeamById(match.opponentId)
}

function goMatchStats(match: TeamMatchRecord) {
  router.push({
    path: `/tournament/team/${teamId.value}/stats`,
    query: { matchId: match.id },
  })
}

function goMatchPrevPage() {
  if (matchCurrentPage.value > 1) {
    matchCurrentPage.value -= 1
  }
}

function goMatchNextPage() {
  if (matchCurrentPage.value < matchTotalPages.value) {
    matchCurrentPage.value += 1
  }
}

function goMatchPage(page: number) {
  if (page >= 1 && page <= matchTotalPages.value) {
    matchCurrentPage.value = page
  }
}

function onMatchStageChange() {
  matchCurrentPage.value = 1
}
</script>

<template>
  <div class="team-overview">
    <section class="overview-section roster-section">
      <div class="section-head">
        <div class="section-head-group">
          <h2 class="section-head-title">本赛季阵容</h2>
          <span class="section-head-sub">Current Roster</span>
        </div>
      </div>

      <div v-if="roster.length === 0" class="overview-empty">暂无阵容数据</div>

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
              :key="player.id"
              class="roster-card"
              role="button"
              tabindex="0"
              @click="openPlayerDetail(player)"
              @keydown.enter="openPlayerDetail(player)"
            >
              <div class="player-avatar">
                <img
                  v-if="player.avatar"
                  :src="player.avatar"
                  :alt="player.nickname"
                  class="player-avatar__img"
                />
                <span v-else class="player-avatar__placeholder">{{ getPlayerNumber(player) }}</span>
              </div>

              <div class="player-name">{{ player.nickname }}</div>
              <span v-if="player.isCaptain" class="captain-badge">队长</span>
              <div class="player-position">{{ player.position }}</div>
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

        <label class="match-filter">
          <span class="match-filter__sr-only">赛季赛段筛选</span>
          <select
            v-model="matchStageFilter"
            class="match-filter__select"
            @change="onMatchStageChange"
          >
            <option value="regular">{{ team.seasonName }} · 常规赛</option>
            <option value="playoffs">{{ team.seasonName }} · 季后赛</option>
          </select>
        </label>
      </div>

      <div v-if="filteredRecentMatches.length === 0" class="overview-empty">暂无比赛记录</div>

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
.team-overview {
  display: flex;
  flex-direction: column;
}

.overview-section {
  .section-head {
    display: flex;
    align-items: flex-end;
    flex-wrap: wrap;
    gap: clamp(10px, 1.2vw, 16px);
    margin: 0 0 clamp(14px, 1.8vw, 22px);
  }

  .section-head-group {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: clamp(10px, 1.2vw, 16px);
    min-width: 0;
  }

  .section-head-title {
    margin: 0;
    color: var(--cnpa-paper-text);
    font-size: clamp(30px, 3vw, 34px);
    line-height: 1;
    font-weight: 900;
    letter-spacing: -0.02em;
  }

  .section-head-sub {
    color: #e51b23;
    font-size: clamp(14px, 1.3vw, 18px);
    line-height: 1.1;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.02em;
    text-transform: uppercase;
    white-space: nowrap;
  }
}

.roster-section {
  margin-top: clamp(14px, 1.8vw, 24px);
}

.overview-section--matches {
  margin-top: clamp(24px, 2.8vw, 36px);
  padding-bottom: clamp(28px, 3.2vw, 40px);
}

.match-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: clamp(12px, 1.4vw, 18px);
  margin-bottom: clamp(10px, 1.2vw, 14px);

  .section-head {
    margin: 0;
  }
}

.match-filter {
  position: relative;
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  min-width: 260px;
  max-width: 300px;
  height: 42px;
  border: 1px solid rgba(0, 0, 0, 0.15);
  border-radius: 2px;
  background: var(--cnpa-paper-surface);
  box-sizing: border-box;

  &::after {
    content: '⌄';
    position: absolute;
    right: 18px;
    color: rgba(17, 17, 17, 0.55);
    font-size: 18px;
    line-height: 1;
    pointer-events: none;
    transform: translateY(-1px);
  }
}

.match-filter__sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.match-filter__select {
  width: 100%;
  height: 100%;
  padding: 0 36px 0 18px;
  border: none;
  border-radius: 2px;
  background: transparent;
  color: var(--cnpa-paper-text);
  font-size: 14px;
  font-weight: 900;
  font-family: inherit;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;

  &:focus {
    outline: none;
  }

  &:focus-visible {
    outline: 2px solid rgba(225, 6, 19, 0.35);
    outline-offset: 1px;
  }
}

.overview-empty {
  padding: 40px 16px;
  text-align: center;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: rgba(25, 25, 25, 0.025);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.roster-wrap {
  position: relative;
  margin: 0 clamp(14px, 1.6vw, 22px);
}

.roster-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: clamp(14px, 1.6vw, 22px);
  padding: 0 clamp(10px, 1.2vw, 18px);
}

.roster-card {
  min-height: clamp(220px, 18vw, 250px);
  padding-top: clamp(24px, 2.2vw, 34px);
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 2px;
  background: rgba(25, 25, 25, 0.03);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  box-sizing: border-box;
  cursor: pointer;
  transition: border-color 0.16s ease, transform 0.16s ease;

  &:hover {
    border-color: rgba(225, 6, 19, 0.45);
    transform: translateY(-2px);
  }
}

.player-avatar {
  width: 108px;
  height: 108px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  position: relative;
  border: 3px solid rgba(193, 134, 32, 0.75);
  box-shadow:
    0 12px 20px rgba(0, 0, 0, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.15);
  background:
    radial-gradient(circle at 35% 28%, rgba(255, 255, 255, 0.18), transparent 35%),
    linear-gradient(135deg, #111, #2b2b2b);
  overflow: hidden;

  &::after {
    content: '';
    position: absolute;
    inset: 12px;
    border-radius: 50%;
    border: 1px solid rgba(255, 255, 255, 0.08);
    pointer-events: none;
  }
}

.player-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.player-avatar__placeholder {
  color: #fff;
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.player-name {
  margin-top: 21px;
  color: var(--cnpa-paper-text);
  font-size: clamp(18px, 1.6vw, 22px);
  line-height: 1;
  font-weight: 900;
}

.captain-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
  min-width: 52px;
  height: 26px;
  padding: 0 10px;
  color: #fff;
  background: var(--cnpa-red);
  font-size: 13px;
  font-weight: 900;
  border-radius: 2px;
  box-shadow: 0 5px 10px rgba(217, 8, 20, 0.22);
}

.player-position {
  margin-top: 13px;
  color: var(--cnpa-paper-text);
  font-size: clamp(16px, 1.4vw, 20px);
  line-height: 1;
  font-weight: 900;
}

.roster-arrow {
  position: absolute;
  top: 118px;
  width: 28px;
  height: 46px;
  border: none;
  background: transparent;
  color: rgba(0, 0, 0, 0.72);
  font-size: 56px;
  line-height: 1;
  font-weight: 300;
  cursor: pointer;
  transition: color 0.2s ease;

  &:hover {
    color: var(--cnpa-red);
  }
}

.roster-arrow--left {
  left: -28px;
}

.roster-arrow--right {
  right: -28px;
}

.carousel-dots {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 11px;
  margin-top: clamp(12px, 1.4vw, 18px);
}

.dot {
  width: 10px;
  height: 10px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.22);
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    transform: scale(1.05);
  }
}

.match-list {
  overflow-x: auto;
}

.match-table {
  width: 100%;
  min-width: 1140px;
  border: 1px solid var(--cnpa-paper-border);
  background: rgba(25, 25, 25, 0.03);
  overflow: hidden;
  box-sizing: border-box;
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

  > * {
    justify-self: center;
    align-self: center;
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

  &.is-winner {
    color: var(--cnpa-red);
  }
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
  border-radius: 2px;
  background: var(--cnpa-paper-surface);
  color: #222;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
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

.match-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: clamp(18px, 2vw, 26px);
}

.match-pagination__nav {
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

.match-pagination__pages {
  display: flex;
  align-items: center;
  gap: 8px;
}

.match-pagination__page {
  width: 30px;
  min-width: 30px;
  height: 30px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 2px;
  background: var(--cnpa-paper-surface);
  color: #333;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
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

@media (max-width: 1200px) {
  .roster-grid {
    min-width: 980px;
  }

  .roster-wrap {
    overflow-x: auto;
    margin-left: 0;
    margin-right: 0;
  }
}

@media (max-width: 768px) {
  .match-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .match-filter {
    width: 100%;
    max-width: none;
  }

  .roster-arrow--left {
    left: 0;
  }

  .roster-arrow--right {
    right: 0;
  }
}
</style>
