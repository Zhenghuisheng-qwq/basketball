<script setup lang="ts">
import { computed } from 'vue'
import type { Team } from '@/views/events/types/events'
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
</script>

<template>
  <section class="period-board">
    <div class="period-board__head">
      <h2>单节比分</h2>
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
            <th>{{ leftTeam.name }}</th>
            <td
              v-for="column in columns"
              :key="`away-${column.key}`"
              :class="{ 'is-total': column.key === 'total' }"
            >
              {{ cell('away', column) }}
            </td>
          </tr>
          <tr>
            <th>{{ rightTeam.name }}</th>
            <td
              v-for="column in columns"
              :key="`home-${column.key}`"
              :class="{ 'is-total': column.key === 'total' }"
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
  margin-bottom: 28px;
}

.period-board__head {
  margin-bottom: 12px;

  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 800;
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
  background: #fff;

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
    letter-spacing: 0.6px;
  }

  tbody th {
    text-align: left;
    min-width: 140px;
  }

  .is-total {
    font-weight: 800;
  }
}
</style>
