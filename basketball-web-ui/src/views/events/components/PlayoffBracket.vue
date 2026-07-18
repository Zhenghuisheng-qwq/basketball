<script setup lang="ts">
import { computed } from 'vue'
import { playoffSeries } from '../mock'
import { getTeamById } from '../mock/teams.mock'
import type { PlayoffSeries } from '../types/events'
import TeamLogo from './TeamLogo.vue'

const leftQuarter = computed(() =>
  playoffSeries.filter((item) => item.round === 'quarterfinal' && item.side === 'left'),
)
const rightQuarter = computed(() =>
  playoffSeries.filter((item) => item.round === 'quarterfinal' && item.side === 'right'),
)
const leftSemi = computed(() =>
  playoffSeries.find((item) => item.round === 'semifinal' && item.side === 'left'),
)
const rightSemi = computed(() =>
  playoffSeries.find((item) => item.round === 'semifinal' && item.side === 'right'),
)
const finalSeries = computed(() => playoffSeries.find((item) => item.round === 'final'))

function teamName(teamId: number) {
  return getTeamById(teamId)?.name || '--'
}

function isWinner(series: PlayoffSeries, teamId: number) {
  return series.winnerTeamId === teamId
}
</script>

<template>
  <section class="playoff-bracket">
    <div class="bracket-scroll">
      <div class="bracket-grid">
        <div class="bracket-side left">
          <article v-for="series in leftQuarter" :key="series.id" class="series-card">
            <div
              class="series-team"
              :class="{
                winner: isWinner(series, series.topTeamId),
                loser: !isWinner(series, series.topTeamId),
              }"
            >
              <TeamLogo v-if="getTeamById(series.topTeamId)" :team="getTeamById(series.topTeamId)!" size="sm" />
              <span>{{ teamName(series.topTeamId) }}</span>
              <strong>{{ series.topScore }}</strong>
            </div>
            <div class="series-divider" />
            <div
              class="series-team"
              :class="{
                winner: isWinner(series, series.bottomTeamId),
                loser: !isWinner(series, series.bottomTeamId),
              }"
            >
              <TeamLogo
                v-if="getTeamById(series.bottomTeamId)"
                :team="getTeamById(series.bottomTeamId)!"
                size="sm"
              />
              <span>{{ teamName(series.bottomTeamId) }}</span>
              <strong>{{ series.bottomScore }}</strong>
            </div>
          </article>

          <article v-if="leftSemi" class="series-card semi-card">
            <div
              class="series-team"
              :class="{
                winner: isWinner(leftSemi, leftSemi.topTeamId),
                loser: !isWinner(leftSemi, leftSemi.topTeamId),
              }"
            >
              <TeamLogo v-if="getTeamById(leftSemi.topTeamId)" :team="getTeamById(leftSemi.topTeamId)!" size="sm" />
              <span>{{ teamName(leftSemi.topTeamId) }}</span>
              <strong>{{ leftSemi.topScore }}</strong>
            </div>
            <div class="series-divider" />
            <div
              class="series-team"
              :class="{
                winner: isWinner(leftSemi, leftSemi.bottomTeamId),
                loser: !isWinner(leftSemi, leftSemi.bottomTeamId),
              }"
            >
              <TeamLogo
                v-if="getTeamById(leftSemi.bottomTeamId)"
                :team="getTeamById(leftSemi.bottomTeamId)!"
                size="sm"
              />
              <span>{{ teamName(leftSemi.bottomTeamId) }}</span>
              <strong>{{ leftSemi.bottomScore }}</strong>
            </div>
          </article>
        </div>

        <div class="bracket-center">
          <div class="final-title">
            <span>CNPA LEAGUE</span>
            <strong>季后赛对阵图</strong>
          </div>
          <article v-if="finalSeries" class="series-card final-card">
            <div
              class="series-team"
              :class="{
                winner: isWinner(finalSeries, finalSeries.topTeamId),
                loser: !isWinner(finalSeries, finalSeries.topTeamId),
              }"
            >
              <TeamLogo
                v-if="getTeamById(finalSeries.topTeamId)"
                :team="getTeamById(finalSeries.topTeamId)!"
                size="sm"
              />
              <span>{{ teamName(finalSeries.topTeamId) }}</span>
              <strong>{{ finalSeries.topScore }}</strong>
            </div>
            <div class="series-divider" />
            <div
              class="series-team"
              :class="{
                winner: isWinner(finalSeries, finalSeries.bottomTeamId),
                loser: !isWinner(finalSeries, finalSeries.bottomTeamId),
              }"
            >
              <TeamLogo
                v-if="getTeamById(finalSeries.bottomTeamId)"
                :team="getTeamById(finalSeries.bottomTeamId)!"
                size="sm"
              />
              <span>{{ teamName(finalSeries.bottomTeamId) }}</span>
              <strong>{{ finalSeries.bottomScore }}</strong>
            </div>
            <div class="champion-label">总冠军</div>
          </article>
        </div>

        <div class="bracket-side right">
          <article v-for="series in rightQuarter" :key="series.id" class="series-card">
            <div
              class="series-team"
              :class="{
                winner: isWinner(series, series.topTeamId),
                loser: !isWinner(series, series.topTeamId),
              }"
            >
              <TeamLogo v-if="getTeamById(series.topTeamId)" :team="getTeamById(series.topTeamId)!" size="sm" />
              <span>{{ teamName(series.topTeamId) }}</span>
              <strong>{{ series.topScore }}</strong>
            </div>
            <div class="series-divider" />
            <div
              class="series-team"
              :class="{
                winner: isWinner(series, series.bottomTeamId),
                loser: !isWinner(series, series.bottomTeamId),
              }"
            >
              <TeamLogo
                v-if="getTeamById(series.bottomTeamId)"
                :team="getTeamById(series.bottomTeamId)!"
                size="sm"
              />
              <span>{{ teamName(series.bottomTeamId) }}</span>
              <strong>{{ series.bottomScore }}</strong>
            </div>
          </article>

          <article v-if="rightSemi" class="series-card semi-card">
            <div
              class="series-team"
              :class="{
                winner: isWinner(rightSemi, rightSemi.topTeamId),
                loser: !isWinner(rightSemi, rightSemi.topTeamId),
              }"
            >
              <TeamLogo v-if="getTeamById(rightSemi.topTeamId)" :team="getTeamById(rightSemi.topTeamId)!" size="sm" />
              <span>{{ teamName(rightSemi.topTeamId) }}</span>
              <strong>{{ rightSemi.topScore }}</strong>
            </div>
            <div class="series-divider" />
            <div
              class="series-team"
              :class="{
                winner: isWinner(rightSemi, rightSemi.bottomTeamId),
                loser: !isWinner(rightSemi, rightSemi.bottomTeamId),
              }"
            >
              <TeamLogo
                v-if="getTeamById(rightSemi.bottomTeamId)"
                :team="getTeamById(rightSemi.bottomTeamId)!"
                size="sm"
              />
              <span>{{ teamName(rightSemi.bottomTeamId) }}</span>
              <strong>{{ rightSemi.bottomScore }}</strong>
            </div>
          </article>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.playoff-bracket {
  padding-top: 8px;
}

.bracket-scroll {
  overflow-x: auto;
  padding-bottom: 8px;
}

.bracket-grid {
  min-width: 1100px;
  display: grid;
  grid-template-columns: 1fr 280px 1fr;
  gap: 32px;
  align-items: center;
}

.bracket-side {
  display: flex;
  flex-direction: column;
  gap: 48px;
  position: relative;
}

.bracket-side.left::after,
.bracket-side.right::after {
  content: '';
  position: absolute;
  top: 18%;
  bottom: 18%;
  width: 1px;
  background: var(--cnpa-red);
  opacity: 0.45;
}

.bracket-side.left::after {
  right: -16px;
}

.bracket-side.right::after {
  left: -16px;
}

.bracket-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.final-title {
  text-align: center;

  span {
    display: block;
    font-size: 12px;
    letter-spacing: 0.18em;
    color: var(--cnpa-text-secondary);
  }

  strong {
    font-size: 20px;
  }
}

.series-card {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #111;
}

.final-card {
  width: 100%;
  border-color: var(--cnpa-red);
  box-shadow: 0 0 24px rgba(225, 6, 19, 0.16);
}

.series-team {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 6px 0;
  font-size: 13px;
  font-weight: 800;
}

.series-team.winner {
  color: var(--cnpa-red);
}

.series-team.loser {
  opacity: 0.45;
}

.series-divider {
  height: 1px;
  margin: 4px 0;
  background: rgba(255, 255, 255, 0.08);
}

.semi-card {
  align-self: center;
  width: 220px;
}

.champion-label {
  margin-top: 10px;
  text-align: center;
  color: var(--cnpa-red);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

@media (max-width: 1100px) {
  .bracket-scroll {
    margin: 0 calc(var(--site-inner-padding) * -1);
    padding: 0 var(--site-inner-padding) 8px;
  }
}
</style>
