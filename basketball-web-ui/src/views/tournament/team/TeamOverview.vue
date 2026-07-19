<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getTeamById,
  getTeamRecentMatches,
  getTeamRoster,
} from '@/mock/team'
import TeamLogo from '@/components/team/TeamLogo.vue'
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

const matchPageNumbers = computed(() =>
  Array.from({ length: Math.max(2, matchTotalPages.value) }, (_, index) => index + 1),
)

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

const resultLabels = {
  win: '胜利',
  loss: '失败',
} as const

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
    <section class="overview-section">
      <div class="section-head">
        <div class="section-head-group">
          <h2 class="section-head-title">本赛季阵容</h2>
          <span class="section-head-sub">Current Roster</span>
        </div>
      </div>

      <div v-if="roster.length === 0" class="overview-empty">暂无阵容数据</div>

      <template v-else>
        <div class="carousel">
          <button
            type="button"
            class="carousel-arrow"
            aria-label="上一页"
            @click="goRosterPrev"
          >
            ‹
          </button>

          <div class="roster-grid">
            <article v-for="player in visibleRoster" :key="player.id" class="player-card">
              <div class="player-card__avatar-wrap">
                <img
                  v-if="player.avatar"
                  :src="player.avatar"
                  :alt="player.nickname"
                  class="player-card__avatar-img"
                />
                <div v-else class="player-card__avatar-placeholder">
                  {{ getPlayerNumber(player) }}
                </div>
              </div>

              <div class="player-card__body">
                <div class="player-card__name-row">
                  <h3 class="player-card__name">{{ player.nickname }}</h3>
                  <span v-if="player.isCaptain" class="player-card__captain">队长</span>
                </div>
                <p class="player-card__position">{{ player.position }}</p>
              </div>
            </article>
          </div>

          <button
            type="button"
            class="carousel-arrow"
            aria-label="下一页"
            @click="goRosterNext"
          >
            ›
          </button>
        </div>

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
      </template>
    </section>

    <section class="overview-section overview-section--matches match-history">
      <div class="section-head">
        <div class="section-head-group">
          <h2 class="section-head-title">比赛记录</h2>
          <span class="section-head-sub">Match History</span>
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
        <div class="match-table-wrap">
          <div class="match-table">
          <div class="match-table__head">
            <span>状态</span>
            <span>日期</span>
            <span>对手</span>
            <span>比分</span>
            <span>赛段</span>
            <span>查看</span>
          </div>

          <article v-for="match in pagedRecentMatches" :key="match.id" class="match-table__row">
            <span
              class="match-table__result"
              :class="match.result === 'win' ? 'is-win' : 'is-loss'"
            >
              {{ match.result ? resultLabels[match.result] : '-' }}
            </span>

            <span class="match-table__date">{{ match.date }}</span>

            <div class="match-table__opponent">
              <TeamLogo :team="getOpponent(match)" size="xs" class="match-table__opponent-logo" />
              <span class="match-table__opponent-name">{{ getOpponent(match).name }}</span>
            </div>

            <div class="match-table__score">
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
            </div>

            <span class="match-table__stage">{{ stageLabels[match.stage] }}</span>

            <div class="match-table__actions">
              <button type="button" class="action-btn" @click="goMatchStats(match)">
                技术统计
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
              v-for="page in matchPageNumbers"
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
  .section-head-title {
    color: var(--cnpa-paper-text);
  }

  .section-head-sub {
    color: var(--cnpa-red);
    font-size: clamp(14px, 1.4vw, 18px);
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.02em;
    text-transform: uppercase;
  }
}

.section-meta {
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
  white-space: nowrap;
}

.overview-section--matches {
  margin-top: clamp(52px, 4vw, 64px);
  padding-bottom: 48px;
  overflow: visible;

  .section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    margin-bottom: 0;
    overflow: visible;
  }

  .section-head-group {
    gap: clamp(14px, 1.2vw, 18px);
    align-items: baseline;
  }

  .section-head-title {
    font-size: clamp(34px, 2.6vw, 38px);
    line-height: 1.15;
  }

  .section-head-sub {
    font-size: clamp(14px, 1.3vw, 17px);
    letter-spacing: 0.04em;
  }

  .match-filter {
    flex-shrink: 0;
    min-width: 260px;
    max-width: 300px;
  }
}

.match-filter {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: 220px;
  height: 40px;
  border: 1px solid rgba(0, 0, 0, 0.16);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.92);
  box-sizing: border-box;

  &::after {
    content: '˅';
    position: absolute;
    right: 14px;
    color: rgba(17, 17, 17, 0.45);
    font-size: 14px;
    line-height: 1;
    pointer-events: none;
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
  padding: 0 36px 0 16px;
  border: none;
  border-radius: 2px;
  background: transparent;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 700;
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
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.55);
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.carousel {
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  gap: 12px;
  align-items: center;
}

.carousel-arrow {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.72);
  color: var(--cnpa-paper-text);
  font-size: 24px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.roster-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.player-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px 12px 18px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.72);
  transition: border-color 0.2s ease, transform 0.2s ease;

  &:hover {
    border-color: rgba(225, 6, 19, 0.35);
    transform: translateY(-2px);
  }
}

.player-card__avatar-wrap {
  width: 72px;
  height: 72px;
}

.player-card__avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.player-card__avatar-placeholder {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: linear-gradient(135deg, #1a1a1a 0%, #3a3a3a 100%);
  color: #fff;
  font-size: 18px;
  font-weight: 900;
}

.player-card__body {
  width: 100%;
  text-align: center;
}

.player-card__name-row {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
}

.player-card__name {
  margin: 0;
  font-size: 14px;
  font-weight: 800;
  color: var(--cnpa-paper-text);
}

.player-card__captain {
  padding: 2px 6px;
  border-radius: 2px;
  background: rgba(225, 6, 19, 0.12);
  color: var(--cnpa-red);
  font-size: 11px;
  font-weight: 800;
}

.player-card__position {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 22px;
}

.dot {
  width: 8px;
  height: 8px;
  border: none;
  border-radius: 50%;
  background: rgba(17, 17, 17, 0.18);
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    transform: scale(1.2);
  }
}

.match-table-wrap {
  width: 100%;
  margin-top: 20px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.match-table {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.58);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.035);
  overflow: hidden;
  box-sizing: border-box;
}

.match-table__head,
.match-table__row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  align-items: center;
  column-gap: 8px;
  padding: 0 18px;
}

.match-table__head {
  min-height: 44px;
  background: linear-gradient(180deg, #111 0%, #050505 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 800;

  > * {
    justify-self: center;
    align-self: center;
    text-align: center;
  }
}

.match-table__row {
  min-height: 68px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  transition: background 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.72);
  }

  > * {
    justify-self: center;
    align-self: center;
  }
}

.match-table__result {
  text-align: center;
  font-size: 16px;

  &.is-win {
    color: #d90012;
    font-weight: 900;
  }

  &.is-loss {
    color: #111;
    font-weight: 900;
  }
}

.match-table__date {
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: var(--cnpa-paper-text);
}

.match-table__opponent {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-width: 0;
  max-width: 100%;
  font-size: 15px;
  font-weight: 800;
  color: var(--cnpa-paper-text);
}

.match-table__opponent-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-table__opponent-logo {
  flex-shrink: 0;
}

.match-table__score {
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  min-width: auto;
  font-size: 28px;
  font-weight: 900;
  line-height: 1;
}

.score-number {
  font-size: inherit;
  font-weight: 900;
  color: #111;
  letter-spacing: 0.02em;
  line-height: 1;

  &.is-winner {
    color: #d90012;
  }
}

.score-separator {
  font-size: inherit;
  font-weight: 900;
  color: #111;
  line-height: 1;
}

.match-table__stage {
  text-align: center;
  font-size: 15px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
}

.match-table__actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.action-btn {
  width: 80px;
  height: 32px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.22);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.72);
  color: #222;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: #d90012;
    color: #d90012;
    background: #fff;
  }
}

.match-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 28px;
}

.match-pagination__nav {
  border: none;
  background: transparent;
  color: rgba(17, 17, 17, 0.58);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s ease;

  &:hover:not(:disabled) {
    color: #d90012;
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
  min-width: 30px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid rgba(0, 0, 0, 0.16);
  border-radius: 2px;
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &.active {
    border-color: #d90012;
    background: #d90012;
    color: #fff;
  }

  &:hover:not(.active) {
    border-color: rgba(217, 0, 18, 0.45);
    color: #d90012;
  }
}

@media (max-width: 1200px) {
  .roster-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .roster-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .match-table {
    min-width: 900px;
  }
}

@media (max-width: 768px) {
  .roster-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .carousel {
    grid-template-columns: 32px 1fr 32px;
  }

  .section-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .overview-section--matches .section-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
