<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import PlayerLink from '@/components/player/PlayerLink.vue'
import {
  formatRate,
  formatStatNumber,
  formatWinLoss,
  getPlayerColumns,
  getTeamColumns,
  type DataType,
  type PlayerStatistics,
  type StatisticType,
  type TeamStatistics,
} from '../types'

const props = defineProps<{
  dataType: DataType
  statisticType: StatisticType
  rows: Array<TeamStatistics | PlayerStatistics>
  loading?: boolean
  error?: string
  emptyText?: string
  sortKey?: string
  sortDir?: 'asc' | 'desc'
}>()

const emit = defineEmits<{
  sort: [key: string, dir: 'asc' | 'desc']
}>()

const { t } = useI18n()
const brokenLogos = reactive<Record<string, boolean>>({})

const columns = computed(() =>
  props.dataType === 'team'
    ? getTeamColumns(props.statisticType)
    : getPlayerColumns(props.statisticType),
)

function columnSorted(key: string) {
  if (props.sortKey === 'totalPoints') {
    if (props.statisticType === 'total') return key === 'points'
    return key === 'rank'
  }
  return props.sortKey === key
}

const isAverage = computed(() => props.statisticType === 'average')

function formatOptionalStat(value: number | null | undefined, average: boolean): string {
  if (value === null || value === undefined) return '--'
  return formatStatNumber(value, average)
}

function isTeamRow(row: TeamStatistics | PlayerStatistics): row is TeamStatistics {
  return 'teamAbbr' in row && !('playerName' in row)
}

function isPlayerRow(row: TeamStatistics | PlayerStatistics): row is PlayerStatistics {
  return 'playerName' in row
}

function rankClass(rank: number): string {
  return rank <= 3 ? 'is-top' : ''
}

function logoBroken(key: string): boolean {
  return Boolean(brokenLogos[key])
}

function handleLogoError(key: string) {
  brokenLogos[key] = true
}

function showLogo(url: string | undefined, key: string): boolean {
  return Boolean(url?.trim()) && !logoBroken(key)
}

function cellValue(row: TeamStatistics | PlayerStatistics, key: string): string {
  if (isTeamRow(row)) {
    switch (key) {
      case 'games':
        return String(row.games)
      case 'winLoss':
        return formatWinLoss(row.wins, row.losses)
      case 'winRate':
        return formatRate(row.winRate)
      case 'points':
        return formatStatNumber(isAverage.value ? row.averagePoints : row.totalPoints, isAverage.value)
      case 'rebounds':
        return formatStatNumber(
          isAverage.value ? row.averageRebounds : row.totalRebounds,
          isAverage.value,
        )
      case 'assists':
        return formatStatNumber(
          isAverage.value ? row.averageAssists : row.totalAssists,
          isAverage.value,
        )
      case 'steals':
        return formatStatNumber(isAverage.value ? row.averageSteals : row.totalSteals, isAverage.value)
      case 'blocks':
        return formatStatNumber(isAverage.value ? row.averageBlocks : row.totalBlocks, isAverage.value)
      case 'turnovers':
        return formatStatNumber(
          isAverage.value ? row.averageTurnovers : row.totalTurnovers,
          isAverage.value,
        )
      case 'fieldGoals':
        return formatRate(row.fieldGoalPct)
      case 'threePoints':
        return formatRate(row.threePointPct)
      case 'freeThrows':
        return formatRate(row.freeThrowPct)
      default:
        return ''
    }
  }

  if (isPlayerRow(row)) {
    switch (key) {
      case 'position':
        return row.position || '--'
      case 'games':
        return String(row.games)
      case 'fouls':
        return formatOptionalStat(isAverage.value ? row.averageFouls : row.totalFouls, isAverage.value)
      case 'points':
        return formatStatNumber(isAverage.value ? row.averagePoints : row.totalPoints, isAverage.value)
      case 'rebounds':
        return formatStatNumber(
          isAverage.value ? row.averageRebounds : row.totalRebounds,
          isAverage.value,
        )
      case 'assists':
        return formatStatNumber(
          isAverage.value ? row.averageAssists : row.totalAssists,
          isAverage.value,
        )
      case 'steals':
        return formatStatNumber(isAverage.value ? row.averageSteals : row.totalSteals, isAverage.value)
      case 'blocks':
        return formatStatNumber(isAverage.value ? row.averageBlocks : row.totalBlocks, isAverage.value)
      case 'turnovers':
        return formatStatNumber(
          isAverage.value ? row.averageTurnovers : row.totalTurnovers,
          isAverage.value,
        )
      case 'fieldGoals':
        return formatRate(row.fieldGoalPct)
      case 'threePoints':
        return formatRate(row.threePointPct)
      case 'freeThrows':
        return formatRate(row.freeThrowPct)
      default:
        return ''
    }
  }

  return ''
}
</script>

<template>
  <div class="data-table-scroll">
    <div v-if="error" class="data-table-state is-error">{{ error }}</div>
    <table v-else class="data-table">
      <thead>
        <tr>
          <th
            v-for="column in columns"
            :key="column.key"
            :class="[`is-${column.align || 'center'}`, column.cellClass]"
            :style="column.width ? { width: column.width, minWidth: column.width } : undefined"
            :aria-sort="columnSorted(column.key) ? (props.sortDir === 'asc' ? 'ascending' : 'descending') : 'none'"
          >
            <span class="data-table__sort">
              <span class="data-table__sort-label">{{ column.label }}</span>
              <span class="data-table__dirs">
                <button
                  type="button"
                  class="data-table__dir"
                  :class="{ 'is-active': columnSorted(column.key) && props.sortDir === 'asc' }"
                  :aria-label="t('publicPages.data.sortAsc', { column: column.label })"
                  @click="emit('sort', column.key, 'asc')"
                >↑</button>
                <button
                  type="button"
                  class="data-table__dir"
                  :class="{ 'is-active': columnSorted(column.key) && props.sortDir === 'desc' }"
                  :aria-label="t('publicPages.data.sortDesc', { column: column.label })"
                  @click="emit('sort', column.key, 'desc')"
                >↓</button>
              </span>
            </span>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td class="data-table-state" :colspan="columns.length">{{ t('publicPages.data.loading') }}</td>
        </tr>
        <tr v-else-if="rows.length === 0">
          <td class="data-table-state" :colspan="columns.length">{{ emptyText || t('publicPages.data.empty') }}</td>
        </tr>
        <template v-else>
        <tr v-for="row in rows" :key="`${dataType}-${row.id}`">
          <td
            v-for="column in columns"
            :key="column.key"
            :class="[`is-${column.align || 'center'}`, column.cellClass]"
          >
            <template v-if="column.key === 'rank'">
              <span class="data-table__rank" :class="rankClass(row.rank)">{{ row.rank }}</span>
            </template>

            <template v-else-if="column.key === 'team' && isTeamRow(row)">
              <router-link :to="`/tournament/team/${row.teamId}/overview`" class="data-table__team">
                <img
                  v-if="showLogo(row.logoUrl, `team-${row.id}`)"
                  :src="row.logoUrl"
                  :alt="row.teamName"
                  class="data-table__logo-img"
                  @error="handleLogoError(`team-${row.id}`)"
                />
                <span class="data-table__name">{{ row.teamName }}</span>
              </router-link>
            </template>

            <template v-else-if="column.key === 'player' && isPlayerRow(row)">
              <div class="data-table__team">
                <img
                  v-if="showLogo(row.avatarUrl, `player-avatar-${row.id}`)"
                  :src="row.avatarUrl"
                  :alt="row.playerName"
                  class="data-table__logo-img data-table__logo-img--avatar"
                  @error="handleLogoError(`player-avatar-${row.id}`)"
                />
                <PlayerLink :player-id="row.playerId" :player-name="row.playerName" tab="data" />
              </div>
            </template>

            <template v-else-if="column.key === 'team' && isPlayerRow(row)">
              <div class="data-table__team">
                <img
                  v-if="showLogo(row.teamLogoUrl, `player-team-${row.id}`)"
                  :src="row.teamLogoUrl"
                  :alt="row.teamName"
                  class="data-table__logo-img"
                  @error="handleLogoError(`player-team-${row.id}`)"
                />
                <span class="data-table__name">{{ row.teamAbbr }}</span>
              </div>
            </template>

            <template v-else>
              {{ cellValue(row, column.key) }}
            </template>
          </td>
        </tr>
        </template>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
.data-table-scroll {
  width: 100%;
  overflow-x: auto;
  overflow-y: visible;
}

.data-table-state {
  padding: 48px 16px;
  text-align: center;
  color: rgba(17, 17, 17, 0.5);
  font-weight: 700;

  &.is-error {
    color: #e10613;
  }
}

.data-table {
  width: 100%;
  min-width: 1100px;
  border-collapse: collapse;
  table-layout: fixed;
  background: transparent;
  color: var(--cnpa-paper-text);
}

.data-table thead th {
  height: 50px;
  padding: 0 4px;
  background: #111;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
  vertical-align: middle;
  border: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.data-table__sort {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.data-table thead th.is-left .data-table__sort,
.data-table thead th.is-team .data-table__sort,
.data-table thead th.is-player .data-table__sort {
  justify-content: flex-start;
}

.data-table__sort-label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.data-table__dirs {
  display: inline-flex;
  flex-direction: column;
  flex-shrink: 0;
}

.data-table__dir {
  width: 14px;
  height: 11px;
  padding: 0;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.38);
  font-size: 10px;
  font-weight: 800;
  line-height: 11px;
  cursor: pointer;

  &.is-active {
    color: #ff1717;
  }
}

.data-table tbody td {
  height: 56px;
  padding: 0 8px;
  background: transparent;
  border-bottom: 1px solid var(--cnpa-paper-border);
  font-size: 13px;
  font-weight: 500;
  line-height: 1.2;
  white-space: nowrap;
  vertical-align: middle;
  color: var(--cnpa-paper-text);
  overflow: hidden;
  text-overflow: ellipsis;
}

.data-table tbody tr:last-child td {
  border-bottom: 0;
}

.data-table tbody tr:hover td {
  background: rgba(25, 25, 25, 0.03);
}

.data-table .is-center {
  text-align: center;
}

.data-table .is-left,
.data-table .is-team,
.data-table .is-player {
  text-align: left;
}

.data-table__rank {
  display: inline-block;
  min-width: 1.2em;
  font-size: 18px;
  font-weight: 800;
  color: var(--cnpa-paper-text);
  line-height: 1;

  &.is-top {
    color: #ff1717;
  }
}

.data-table__team {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  max-width: 100%;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.data-table__logo-img,
.data-table__logo-placeholder {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
}

.data-table__logo-img {
  display: block;
  object-fit: contain;
  background: transparent;

  &--avatar {
    object-fit: cover;
    border-radius: 50%;
  }
}

.data-table__logo-placeholder {
  display: grid;
  place-items: center;
  border-radius: 4px;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1;
  box-sizing: border-box;
}

.data-table__name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-paper-text);
}
</style>
