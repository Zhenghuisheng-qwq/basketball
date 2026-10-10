<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { Team } from '@/views/events/types/events'
import type { V4PublicMatchSeriesGame } from '@/api/types/v4Public'
import { formatExactScore, isPresent } from './matchDetailDisplay'

const props = defineProps<{
  games: V4PublicMatchSeriesGame[]
  leftTeam: Team
  rightTeam: Team
  activeMatchId?: string
}>()

const emit = defineEmits<{
  select: [matchId: string]
}>()

const { t } = useI18n()

function teamId(team: Team): string {
  return String(team.teamId || '').trim()
}

function teamShort(team: Team): string {
  return String(team.shortName || team.code || team.name || '').trim()
}

function gameTone(game: V4PublicMatchSeriesGame): 'completed' | 'live' | 'scheduled' | 'cancelled' {
  if (game.skipped) return 'cancelled'
  const status = String(game.matchStatus || '').toUpperCase()
  if (status === 'CANCELLED' || status === 'CANCELED') return 'cancelled'
  if (status === 'PLAYING' || status === 'LIVE' || status === 'IN_PROGRESS') return 'live'
  if (status === 'FINISHED' || status === 'COMPLETED' || status === 'FORFEITED' || status === 'FORFEIT') {
    return 'completed'
  }
  return 'scheduled'
}

function toneLabel(game: V4PublicMatchSeriesGame): string {
  if (game.skipped) return game.skippedReason || t('eventsPage.match.notNeeded')
  const tone = gameTone(game)
  if (tone === 'live') return t('match.live')
  if (tone === 'cancelled') return game.matchStatusName || t('match.cancelled')
  if (tone === 'scheduled') return t('match.notStarted')
  return ''
}

function leftScore(game: V4PublicMatchSeriesGame): number | null {
  return isPresent(game.awayScore) ? Number(game.awayScore) : null
}

function rightScore(game: V4PublicMatchSeriesGame): number | null {
  return isPresent(game.homeScore) ? Number(game.homeScore) : null
}

function winnerTeam(game: V4PublicMatchSeriesGame): Team | null {
  const winner = String(game.winnerEventTeamId || '').trim()
  if (!winner) return null
  if (winner === teamId(props.leftTeam)) return props.leftTeam
  if (winner === teamId(props.rightTeam)) return props.rightTeam
  return null
}

function gameDate(value?: string): string {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    const part = raw.replace('T', ' ').slice(0, 10)
    if (/^\d{4}-\d{2}-\d{2}$/.test(part)) return `${part.slice(5, 7)}.${part.slice(8, 10)}`
    return ''
  }
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}.${day}`
}

function canSelect(game: V4PublicMatchSeriesGame): boolean {
  return Boolean(game.matchId) && !game.skipped && gameTone(game) !== 'cancelled'
}

function onSelect(game: V4PublicMatchSeriesGame) {
  if (!canSelect(game) || !game.matchId) return
  emit('select', String(game.matchId))
}
</script>

<template>
  <section class="series-progress" :aria-label="t('eventsPage.aria.seriesProgress')">
    <div class="series-progress__head">
      <h2>{{ t('eventsPage.match.seriesProgress') }}</h2>
      <span>SERIES GAMES</span>
    </div>
    <div class="series-progress__track">
      <button
        v-for="game in games"
        :key="game.matchId || `g-${game.seriesGameNo}`"
        type="button"
        class="series-progress__node"
        :class="[
          `is-${gameTone(game)}`,
          {
            'is-active': activeMatchId && String(game.matchId) === String(activeMatchId),
            'is-disabled': !canSelect(game),
          },
        ]"
        :disabled="!canSelect(game)"
        @click="onSelect(game)"
      >
        <strong class="series-progress__game">G{{ game.seriesGameNo }}</strong>
        <p class="series-progress__score">
          <template v-if="gameTone(game) === 'completed' || gameTone(game) === 'live'">
            <span :class="{ 'is-win': winnerTeam(game)?.teamId === leftTeam.teamId }">{{ formatExactScore(leftScore(game)) }}</span>
            <span class="series-progress__colon">:</span>
            <span :class="{ 'is-win': winnerTeam(game)?.teamId === rightTeam.teamId }">{{ formatExactScore(rightScore(game)) }}</span>
          </template>
          <template v-else>-- : --</template>
        </p>
        <p class="series-progress__result">
          <span v-if="gameTone(game) === 'live'" class="series-progress__live" aria-hidden="true" />
          <template v-if="winnerTeam(game)">{{ t('eventsPage.match.won', { name: teamShort(winnerTeam(game)!) }) }}</template>
          <template v-else>{{ toneLabel(game) }}</template>
        </p>
        <p v-if="gameDate(game.scheduledTime)" class="series-progress__date">{{ gameDate(game.scheduledTime) }}</p>
      </button>
    </div>
  </section>
</template>

<style scoped lang="scss">
.series-progress {
  margin: 28px 0 0;
  color: #fff;
}

.series-progress__head {
  margin-bottom: 14px;

  h2 {
    margin: 0;
    font-size: 16px;
    font-weight: 800;
    letter-spacing: 0.04em;
  }

  span {
    display: block;
    margin-top: 4px;
    color: rgba(255, 255, 255, 0.42);
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.14em;
  }
}

.series-progress__track {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(108px, 1fr));
  gap: 8px;
}

.series-progress__node {
  min-width: 0;
  padding: 12px 8px 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.35);
  color: inherit;
  text-align: center;
  cursor: pointer;
  font-family: inherit;

  &.is-disabled {
    cursor: default;
    opacity: 0.72;
  }

  &.is-active {
    border-color: var(--cnpa-red, #e51422);
  }

  &.is-live {
    border-color: rgba(229, 20, 34, 0.55);
  }
}

.series-progress__game {
  display: block;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  color: rgba(255, 255, 255, 0.72);
}

.series-progress__score {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  margin: 8px 0 6px;
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
}

.series-progress__colon {
  color: rgba(255, 255, 255, 0.36);
}

.series-progress__score .is-win {
  color: var(--cnpa-red, #e51422);
  font-weight: 900;
}

.series-progress__live {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 5px;
  border-radius: 50%;
  background: var(--cnpa-red, #e51422);
  vertical-align: 1px;
}

.series-progress__result,
.series-progress__date {
  margin: 0;
  line-height: 1.4;
}

.series-progress__result {
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 700;
}

.series-progress__node.is-completed .series-progress__result {
  color: var(--cnpa-red, #e51422);
}

.series-progress__date {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.42);
  font-size: 11px;
  font-weight: 600;
}

@media (max-width: 720px) {
  .series-progress__track {
    display: flex;
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .series-progress__node {
    flex: 0 0 112px;
  }
}
</style>
