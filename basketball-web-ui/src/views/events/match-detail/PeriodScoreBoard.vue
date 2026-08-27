<script setup lang="ts">
import { computed } from 'vue'
import type { Team } from '@/views/events/types/events'
import TeamLogo from '@/views/events/components/TeamLogo.vue'
import {
  formatExactScore,
  periodColumns,
  periodTotal,
  periodValue,
} from './matchDetailDisplay'

const props = defineProps<{
  leftTeam: Team
  rightTeam: Team
  periods: Array<{ periodNo?: number; homeScore?: number; awayScore?: number }>
  leftTotal?: number | null
  rightTotal?: number | null
  winnerEventTeamId?: string | null
}>()

const columns = computed(() => periodColumns(props.periods))

function cell(teamSide: 'away' | 'home', column: { key: string; periodNo?: number }): string {
  if (column.key === 'total') {
    const official = teamSide === 'away' ? props.leftTotal : props.rightTotal
    const fallback = periodTotal(props.periods, teamSide === 'away' ? 'away' : 'home')
    return formatExactScore(official ?? fallback)
  }
  return formatExactScore(periodValue(props.periods, Number(column.periodNo), teamSide === 'away' ? 'away' : 'home'))
}

function isWinner(team: Team): boolean {
  const winner = String(props.winnerEventTeamId || '').trim()
  const id = String(team.teamId || '').trim()
  if (winner && id) return winner === id
  const left = props.leftTotal
  const right = props.rightTotal
  if (left == null || right == null || left === right) return false
  return (team === props.leftTeam && left > right) || (team === props.rightTeam && right > left)
}
</script>

<template>
  <section class="period-board">
    <div class="period-board__head">
      <h2>单节比分</h2>
      <span>QUARTER SCORE</span>
    </div>
    <div class="period-board__scroll">
      <table class="period-board__table">
        <thead>
          <tr>
            <th></th>
            <th v-for="column in columns" :key="column.key">{{ column.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th>
              <span class="period-board__team">
                <TeamLogo :team="leftTeam" size="sm" />
                <em>{{ leftTeam.name }}</em>
              </span>
            </th>
            <td
              v-for="column in columns"
              :key="'away-' + column.key"
              :class="{ 'is-total': column.key === 'total', 'is-win': column.key === 'total' && isWinner(leftTeam) }"
            >
              {{ cell('away', column) }}
            </td>
          </tr>
          <tr>
            <th>
              <span class="period-board__team">
                <TeamLogo :team="rightTeam" size="sm" />
                <em>{{ rightTeam.name }}</em>
              </span>
            </th>
            <td
              v-for="column in columns"
              :key="'home-' + column.key"
              :class="{ 'is-total': column.key === 'total', 'is-win': column.key === 'total' && isWinner(rightTeam) }"
            >
              {{ cell('home', column) }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped lang="scss">
.period-board {
  margin-bottom: 32px;
}

.period-board__head {
  margin-bottom: 14px;

  h2 {
    margin: 0;
    font-size: clamp(22px, 2vw, 28px);
    font-weight: 900;
  }

  span {
    display: block;
    margin-top: 6px;
    color: var(--cnpa-red, #e51422);
    font-size: 12px;
    font-weight: 800;
    font-style: italic;
    letter-spacing: 0.16em;
    text-transform: uppercase;
  }
}

.period-board__scroll {
  overflow-x: auto;
  max-width: 100%;
}

.period-board__table {
  width: 100%;
  min-width: 520px;
  border-collapse: collapse;
  overflow: hidden;
  border-radius: 10px;

  th,
  td {
    padding: 12px 14px;
    text-align: center;
    font-size: 15px;
    font-weight: 700;
    border-bottom: 1px solid #eceae7;
  }

  thead th {
    background: #111;
    color: #fff;
    font-size: 12px;
    letter-spacing: 0.08em;
  }

  tbody th {
    text-align: left;
    min-width: 148px;
    background: transparent;
    font-weight: 800;
  }

  .is-total {
    font-weight: 800;
  }

  .is-win {
    color: var(--cnpa-red, #e51422);
    font-weight: 900;
  }
}

.period-board__team {
  display: inline-flex;
  align-items: center;
  gap: 10px;

  em {
    font-style: normal;
  }
}
</style>
