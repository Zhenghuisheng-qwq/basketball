<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import PlayerMatchTable from './PlayerMatchTable.vue'
import DisciplineStatusPanel from '@/components/discipline/DisciplineStatusPanel.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { PlayerCareerStatistics, PlayerDetailData } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
  blankWithoutGames?: boolean
}>()

const router = useRouter()
const { t } = useI18n()

const seasonStat = computed<PlayerCareerStatistics | null>(() => {
  const divisionId = String(props.detail.profile.divisionId || '')
  return props.detail.careerStats.find((row) => String(row.divisionId) === divisionId)
    || props.detail.careerStats[0]
    || null
})

const seasonBoxes = computed(() => {
  const row = seasonStat.value
  if (!row || (props.blankWithoutGames && !(Number(row.games) > 0))) return []
  return [
    { key: 'games', label: t('publicPages.player.games'), value: formatStat(row.games, false), accent: false },
    { key: 'pts', label: t('publicPages.player.avgPoints'), value: formatStat(row.points, true), accent: true },
    { key: 'reb', label: t('publicPages.player.avgRebounds'), value: formatStat(row.rebounds, true), accent: false },
    { key: 'ast', label: t('publicPages.player.avgAssists'), value: formatStat(row.assists, true), accent: false },
  ]
})

const honorItems = computed(() => {
  const seasonId = props.detail.profile.seasonId
  if (!seasonId) return props.detail.honors
  return props.detail.honors.filter((item) => !item.seasonId || item.seasonId === seasonId)
})

const countBoxes = computed(() => {
  const count = (code: string) =>
    honorItems.value.filter((item) => (item.honorCode || '').toUpperCase() === code).length
  return [
    { key: 'games', icon: 'court', label: t('publicPages.player.gamesCount'), count: props.blankWithoutGames && !(Number(seasonStat.value?.games) > 0) ? '--' : (seasonStat.value?.games ?? '--'), unit: t('publicPages.player.gamesUnit') },
    { key: 'WEEKLY_BEST', icon: 'star', label: t('publicPages.player.weeklyPlayer'), count: count('WEEKLY_BEST'), unit: t('publicPages.player.timesUnit') },
    { key: 'ALL_TOURNAMENT', icon: 'laurel', label: t('publicPages.hall.allTournament'), count: count('ALL_TOURNAMENT'), unit: t('publicPages.player.timesUnit') },
    { key: 'ALL_DEFENSIVE', icon: 'shield', label: t('publicPages.hall.allDefensive'), count: count('ALL_DEFENSIVE'), unit: t('publicPages.player.timesUnit') },
  ]
})

const honorMedals = computed(() => {
  const count = (code: string) =>
    honorItems.value.filter((item) => (item.honorCode || '').toUpperCase() === code).length
  return [
    { key: 'CHAMPION', label: t('publicPages.hall.championFallback'), count: count('CHAMPION') },
    { key: 'RUNNER_UP', label: t('publicPages.player.runnerUp'), count: count('RUNNER_UP') },
    { key: 'THIRD_PLACE', label: t('publicPages.player.thirdPlace'), count: count('THIRD_PLACE') },
  ]
})

function formatStat(value: number | null | undefined, average: boolean): string {
  if (value == null || Number.isNaN(Number(value))) return '--'
  const num = Number(value)
  if (!average || Number.isInteger(num)) return String(num)
  return num.toFixed(1)
}

function goTab(tab: 'matches' | 'honors') {
  router.replace({ query: { ...router.currentRoute.value.query, tab } })
}
</script>

<template>
  <div class="player-detail-overview">
    <DisciplineStatusPanel
      :items="detail.profile.activeDisciplineSummary || []"
      :title="t('publicPages.player.penaltyStatus')"
    />

    <section class="player-detail-overview__section" aria-labelledby="player-event-stats">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 id="player-event-stats" class="player-detail-section-head__title">{{ t('publicPages.player.eventStats') }}</h2>
          <span class="player-detail-section-head__sub">Event Stats</span>
        </div>
      </div>

      <div v-if="seasonBoxes.length" class="player-event-stats">
        <div
          v-for="item in seasonBoxes"
          :key="item.key"
          class="player-event-stats__item"
          :class="{ 'is-accent': item.accent }"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div v-else class="player-overview-empty">
        <strong>{{ t('publicPages.player.noEventStats') }}</strong>
        <span>{{ t('publicPages.player.noEventStatsHint') }}</span>
      </div>
    </section>

    <section class="player-detail-overview__section" aria-labelledby="player-honors">
      <div class="honor-head">
        <div class="honor-head__title">
          <h2 id="player-honors">{{ t('publicPages.player.honors') }}</h2>
          <span>Player Honors</span>
        </div>
        <p>{{ t('publicPages.player.honorsDesc') }}</p>
        <button type="button" class="player-detail-section-head__action" @click="goTab('honors')">
          {{ t('publicPages.player.viewAllHonors') }}
        </button>
      </div>

      <div class="player-honor-board">
        <article class="player-honor-board__id">
          <span class="player-honor-board__ribbon">CNPA</span>
          <TeamLogo
            :team="{
              id: detail.profile.currentTeamId || detail.profile.id,
              name: detail.profile.currentTeamName || detail.profile.displayName,
              shortName: detail.profile.currentTeamAbbr || detail.profile.displayName,
              logoUrl: detail.profile.currentTeamLogoUrl,
            }"
            size="xl"
          />
          <strong>{{ detail.profile.displayName }}</strong>
          <span v-if="detail.profile.position">{{ detail.profile.position }}</span>
          <span>{{ detail.profile.currentTeamName || t('publicPages.player.noTeamHistory') }}</span>
          <em>{{ t('publicPages.player.registeredMark') }}</em>
        </article>
        <div class="player-honor-board__main">
          <div class="player-honor-board__counts">
            <div v-for="item in countBoxes" :key="item.key">
              <svg v-if="item.icon === 'court'" viewBox="0 0 48 48" aria-hidden="true">
                <rect x="6" y="8" width="36" height="32" rx="2" fill="none" stroke="currentColor" stroke-width="2" />
                <path d="M6 24h36M24 8v32M14 24a10 10 0 0 0 20 0M14 24a10 10 0 0 1 20 0" fill="none" stroke="currentColor" stroke-width="2" />
              </svg>
              <svg v-else-if="item.icon === 'star'" viewBox="0 0 48 48" aria-hidden="true">
                <path d="M24 8l4.2 9.4 10.2.8-7.8 6.4 2.4 10L24 29.6 15 34.6l2.4-10L9.6 18.2l10.2-.8z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
              </svg>
              <svg v-else-if="item.icon === 'laurel'" viewBox="0 0 48 48" aria-hidden="true">
                <path d="M16 36c-6-4-8-12-6-20 6 2 10 8 12 16M32 36c6-4 8-12 6-20-6 2-10 8-12 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                <path d="M24 12l2.4 5.4 5.8.4-4.4 3.6 1.4 5.6L24 24.2 18.8 27l1.4-5.6-4.4-3.6 5.8-.4z" fill="currentColor" />
              </svg>
              <svg v-else viewBox="0 0 48 48" aria-hidden="true">
                <path d="M24 8l14 6v10c0 8.4-6 14.6-14 18-8-3.4-14-9.6-14-18V14z" fill="none" stroke="currentColor" stroke-width="2" stroke-linejoin="round" />
                <path d="M18 24l4 4 8-9" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              <span>{{ item.label }}</span>
              <strong>{{ item.count }}<small v-if="item.unit && item.count !== '--'">{{ item.unit }}</small></strong>
            </div>
          </div>
          <div class="player-honor-medals">
            <span class="player-honor-medals__label">{{ t('publicPages.hall.eventHonors') }}</span>
            <div v-for="item in honorMedals" :key="item.key" class="player-honor-medals__item" :class="`is-${item.key.toLowerCase()}`">
              <svg viewBox="0 0 64 64" aria-hidden="true">
                <path d="M22 8h20l-4 16H26z" class="medal-ribbon" />
                <circle cx="32" cy="38" r="16" class="medal-face" />
                <circle cx="32" cy="38" r="11" fill="none" class="medal-ring" />
                <path d="M32 30l2.1 4.6 5 .4-3.8 3.2 1.2 4.9L32 40.6l-4.5 2.5 1.2-4.9-3.8-3.2 5-.4z" class="medal-star" />
              </svg>
              <div>
                <span>{{ item.label }}</span>
                <strong>{{ item.count }}</strong>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="player-detail-overview__section" aria-labelledby="player-recent-matches">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 id="player-recent-matches" class="player-detail-section-head__title">{{ t('publicPages.player.recentMatches') }}</h2>
          <span class="player-detail-section-head__sub">Recent Matches</span>
        </div>
        <button
          type="button"
          class="player-detail-section-head__action player-detail-overview__all-matches"
          @click="goTab('matches')"
        >
          {{ t('publicPages.player.viewAllMatches') }}
        </button>
      </div>

      <div class="player-recent-panel">
        <PlayerMatchTable :rows="detail.recentMatches" compact />
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-overview__section + .player-detail-overview__section {
  margin-top: 40px;
}

.player-detail-overview__section {
  padding: 0;
  box-sizing: border-box;
}

.honor-head {
  margin-bottom: 18px;
}

.honor-head__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;

  &::before {
    content: '';
    width: 4px;
    height: 22px;
    align-self: center;
    background: var(--cnpa-red, #e10613);
  }

  h2 {
    margin: 0;
    color: var(--cnpa-paper-text);
    font-size: clamp(26px, 2.2vw, 32px);
    font-weight: 900;
    line-height: 1;
  }

  span {
    color: var(--cnpa-red, #e10613);
    font-size: 13px;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.12em;
    text-transform: uppercase;
  }
}

.honor-head p {
  margin: 8px 0 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.player-event-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #fff;
}

.player-event-stats__item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  min-height: 64px;
  padding: 16px 20px;
  border-right: 1px solid rgba(17, 17, 17, 0.06);

  &:last-child {
    border-right: 0;
  }

  span {
    color: #777;
    font-size: 13px;
    font-weight: 700;
  }

  strong {
    color: #171717;
    font-size: 24px;
    font-weight: 900;
    line-height: 1;
  }

  &.is-accent strong {
    color: var(--cnpa-red, #e10613);
  }
}

.player-overview-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-height: 80px;
  padding: 16px 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #f7f5f2;
  box-sizing: border-box;

  strong {
    color: #333;
    font-size: 14px;
    font-weight: 800;
  }

  span {
    color: #888;
    font-size: 12px;
    font-weight: 600;
  }
}

.player-honor-board {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: stretch;
}

.player-honor-board__id {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 280px;
  padding: 42px 22px 22px;
  border: 1px solid #d4af5a;
  background:
    radial-gradient(circle at 50% 22%, rgba(212, 175, 90, 0.2), transparent 42%),
    linear-gradient(180deg, #16120e 0%, #070707 100%);
  box-shadow:
    inset 0 0 0 8px #100e0c,
    inset 0 0 0 9px rgba(212, 175, 90, 0.85),
    inset 0 0 0 16px #100e0c;
  color: #f6e7c3;
  text-align: center;
}

.player-honor-board__ribbon {
  position: absolute;
  top: 10px;
  left: 50%;
  z-index: 1;
  width: 58px;
  height: 30px;
  padding-top: 4px;
  background: #e10613;
  color: #fff;
  font-size: 10px;
  font-style: normal;
  font-weight: 900;
  letter-spacing: 0.08em;
  line-height: 1;
  text-align: center;
  transform: translateX(-50%);
  clip-path: polygon(0 0, 100% 0, 100% 68%, 50% 100%, 0 68%);
}

.player-honor-board__id {

  strong {
    margin-top: 12px;
    color: #f3e6c4;
    font-size: 28px;
    font-weight: 900;
    line-height: 1.1;
  }

  span:not(.player-honor-board__ribbon) {
    color: rgba(243, 227, 194, 0.72);
    font-size: 14px;
    font-weight: 700;
  }

  em {
    margin-top: 18px;
    color: rgba(243, 227, 194, 0.48);
    font-size: 11px;
    font-style: normal;
    font-weight: 800;
    letter-spacing: 0.16em;
  }
}

.player-honor-board__main {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.player-honor-board__counts {
  display: grid;
  flex: 1;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #fff;

  div {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;
    min-height: 168px;
    padding: 16px 8px;
    border-right: 1px solid rgba(17, 17, 17, 0.06);
    text-align: center;

    &:last-child {
      border-right: 0;
    }
  }

  svg {
    width: 36px;
    height: 36px;
    color: #e10613;
  }

  span {
    color: #666;
    font-size: 14px;
    font-weight: 700;
  }

  strong {
    color: #e10613;
    font-size: 40px;
    font-weight: 900;
    line-height: 1;
  }

  small {
    margin-left: 4px;
    font-size: 16px;
    font-weight: 800;
  }
}

.player-honor-medals {
  display: grid;
  grid-template-columns: 92px repeat(3, minmax(0, 1fr));
  align-items: center;
  min-height: 96px;
  padding: 8px 12px 8px 18px;
  background: #111;
  color: #fff;
}

.player-honor-medals__label {
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
  font-weight: 800;
}

.player-honor-medals__item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;

  svg {
    width: 52px;
    height: 52px;
    flex: 0 0 52px;
  }

  .medal-ribbon { fill: #8a6230; }
  .medal-face { fill: #e8c56a; }
  .medal-ring { stroke: #fff6dc; stroke-width: 1.5; }
  .medal-star { fill: #8a6230; }

  div {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  span {
    color: rgba(255, 255, 255, 0.88);
    font-size: 14px;
    font-weight: 800;
  }

  strong {
    color: #f0d48a;
    font-size: 30px;
    font-weight: 900;
    line-height: 1;
  }

  &.is-runner_up {
    .medal-ribbon { fill: #5c6774; }
    .medal-face { fill: #d5dde6; }
    .medal-star { fill: #5c6774; }

    strong { color: #e6edf4; }
  }

  &.is-third_place {
    .medal-ribbon { fill: #6b3b24; }
    .medal-face { fill: #e0a06a; }
    .medal-star { fill: #6b3b24; }

    strong { color: #e7b184; }
  }
}

.player-recent-panel {
  overflow-x: auto;
  overflow-y: hidden;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.55);
}

.player-detail-overview__all-matches {
  min-height: 40px;
  padding: 10px 4px 10px 16px;
}

@media (max-width: 1279px) {
  .player-event-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-event-stats__item:nth-child(2n) {
    border-right: 0;
  }

  .player-event-stats__item:nth-child(-n + 2) {
    border-bottom: 1px solid rgba(17, 17, 17, 0.06);
  }
}

@media (max-width: 1100px) {
  .player-honor-board {
    grid-template-columns: 1fr;
  }

  .player-honor-board__counts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-honor-board__counts div:nth-child(2n) {
    border-right: 0;
  }

  .player-honor-board__counts div:nth-child(-n + 2) {
    border-bottom: 1px solid rgba(17, 17, 17, 0.06);
  }
}

@media (max-width: 767px) {
  .player-event-stats,
  .player-honor-board,
  .player-honor-board__counts,
  .player-honor-medals {
    grid-template-columns: 1fr;
  }

  .player-honor-medals__item {
    justify-content: flex-start;
    padding: 8px 0;
  }
}
</style>
