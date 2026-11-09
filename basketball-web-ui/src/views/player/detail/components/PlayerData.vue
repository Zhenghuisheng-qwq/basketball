<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { normalizeDataMode, type PlayerDataMode } from '@/utils/playerNavigate'
import type { PlayerCareerStatistics, PlayerDetailData } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
}>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const allStages = 'all'

const dataMode = ref<PlayerDataMode>(normalizeDataMode(route.query.dataMode))
const statMode = ref<'average' | 'total'>('average')
const seasonFilter = ref(allStages)
const selectedStageId = ref(props.detail.seasonStageOptions[0]?.id ?? '')
const dropdownOpen = ref(false)
const dropdownRef = ref<HTMLElement | null>(null)

const selectedStage = computed(
  () =>
    props.detail.seasonStageOptions.find((item) => item.id === selectedStageId.value) ??
    props.detail.seasonStageOptions[0],
)

const careerRows = computed(() => {
  let rows = props.detail.careerStats
  if (seasonFilter.value !== allStages) {
    rows = rows.filter((row) => row.seasonId === seasonFilter.value)
  }
  return rows
})

const careerAverageRow = computed(() => {
  const rows = careerRows.value
  if (rows.length === 0) return null
  const totalGames = rows.reduce((sum, row) => sum + row.games, 0)
  const avg = (key: keyof PlayerCareerStatistics) =>
    rows.reduce((sum, row) => sum + Number(row[key] ?? 0), 0) / rows.length
  const sumNullable = (key: keyof PlayerCareerStatistics) => {
    const values = rows
      .map((row) => row[key])
      .filter((value): value is number => value !== null && value !== undefined && Number.isFinite(Number(value)))
      .map(Number)
    if (!values.length) return null
    return values.reduce((sum, value) => sum + value, 0)
  }
  const madeFg = sumNullable('fieldGoalMade')
  const attFg = sumNullable('fieldGoalAttempted')
  const madeTp = sumNullable('threePointsMade')
  const attTp = sumNullable('threePointsAttempted')
  const madeFt = sumNullable('freeThrowsMade')
  const attFt = sumNullable('freeThrowsAttempted')
  return {
    seasonId: '',
    divisionId: '',
    tournamentName: t('publicPages.player.careerAverage'),
    teamId: '',
    teamLogoUrl: '',
    teamName: '-',
    platform: '-',
    games: totalGames,
    points: avg('points'),
    rebounds: avg('rebounds'),
    assists: avg('assists'),
    steals: avg('steals'),
    blocks: avg('blocks'),
    turnovers: avg('turnovers'),
    fouls: avg('fouls'),
    fieldGoalMade: madeFg,
    fieldGoalAttempted: attFg,
    threePointsMade: madeTp,
    threePointsAttempted: attTp,
    freeThrowsMade: madeFt,
    freeThrowsAttempted: attFt,
    fieldGoalPercentage: shootingPctFromTotals(madeFg, attFg),
    threePointPercentage: shootingPctFromTotals(madeTp, attTp),
    freeThrowPercentage: shootingPctFromTotals(madeFt, attFt),
  }
})

const seasonStatRows = computed(() => {
  if (!selectedStage.value) return []
  return props.detail.careerStats.filter((row) => row.seasonId === selectedStage.value.id)
})

function shootingPctFromTotals(made: number | null, attempt: number | null): number | null {
  if (made == null || attempt == null || !Number.isFinite(made) || !Number.isFinite(attempt)) return null
  if (attempt <= 0) return null
  return (made / attempt) * 100
}

function formatStat(value: number | null | undefined, isPct = false): string {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--'
  }
  const num = Number(value)
  if (isPct) {
    const pct = num <= 1 ? num * 100 : num
    return `${pct.toFixed(1)}%`
  }
  if (statMode.value === 'average') return Number.isInteger(num) ? String(num) : num.toFixed(1)
  return String(Math.round(num))
}

function formatShootingPct(
  row: Pick<
    PlayerCareerStatistics,
    | 'fieldGoalPercentage'
    | 'threePointPercentage'
    | 'freeThrowPercentage'
    | 'fieldGoalMade'
    | 'fieldGoalAttempted'
    | 'threePointsMade'
    | 'threePointsAttempted'
    | 'freeThrowsMade'
    | 'freeThrowsAttempted'
  >,
  kind: 'fg' | 'tp' | 'ft',
): string {
  const attempt = kind === 'fg'
    ? row.fieldGoalAttempted
    : kind === 'tp'
      ? row.threePointsAttempted
      : row.freeThrowsAttempted
  const made = kind === 'fg'
    ? row.fieldGoalMade
    : kind === 'tp'
      ? row.threePointsMade
      : row.freeThrowsMade
  const apiPct = kind === 'fg'
    ? row.fieldGoalPercentage
    : kind === 'tp'
      ? row.threePointPercentage
      : row.freeThrowPercentage
  if (attempt === 0) return '—'
  const computed = shootingPctFromTotals(made ?? null, attempt ?? null)
  if (computed != null) return `${computed.toFixed(1)}%`
  if (apiPct == null || Number.isNaN(Number(apiPct))) return '--'
  const num = Number(apiPct)
  const pct = num <= 1 ? num * 100 : num
  return `${pct.toFixed(1)}%`
}

function setDataMode(mode: PlayerDataMode) {
  dataMode.value = mode
  router.replace({
    query: {
      ...route.query,
      dataMode: mode,
    },
  })
}

function selectStage(id: string) {
  selectedStageId.value = id
  dropdownOpen.value = false
}

function toggleDropdown() {
  dropdownOpen.value = !dropdownOpen.value
}

function handleDocumentClick(event: MouseEvent) {
  if (!dropdownOpen.value || !dropdownRef.value) return
  if (!dropdownRef.value.contains(event.target as Node)) dropdownOpen.value = false
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') dropdownOpen.value = false
}

watch(
  () => route.query.dataMode,
  (value) => {
    dataMode.value = normalizeDataMode(value)
  },
)

watch(
  () => props.detail.profile.divisionId,
  (divisionId) => {
    const id = String(divisionId || '')
    if (id && props.detail.seasonStageOptions.some((item) => item.id === id)) {
      selectedStageId.value = id
    }
  },
)

watch(
  () => props.detail.seasonStageOptions,
  (options) => {
    if (!options.some((item) => item.id === selectedStageId.value)) {
      selectedStageId.value = options[0]?.id ?? ''
    }
  },
  { immediate: true },
)

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="player-detail-data">
    <header class="player-detail-data__intro">
      <h2 class="player-detail-data__title">
        {{ t('publicPages.player.playerData') }}
        <span>PLAYER DATA</span>
      </h2>
      <p class="player-detail-data__desc">
        {{ t('publicPages.player.playerDataDesc') }}
      </p>
    </header>

    <div class="player-detail-data__mode-tabs" role="tablist" :aria-label="t('publicPages.player.dataScope')">
      <button
        type="button"
        class="player-detail-data__mode-tab"
        :class="{ 'is-active': dataMode === 'career' }"
        @click="setDataMode('career')"
      >
        {{ t('publicPages.player.careerData') }}
      </button>
      <button
        type="button"
        class="player-detail-data__mode-tab"
        :class="{ 'is-active': dataMode === 'season' }"
        @click="setDataMode('season')"
      >
        {{ t('publicPages.player.eventData') }}
      </button>
    </div>

    <template v-if="dataMode === 'career'">
      <section class="player-detail-data__panel" :aria-label="t('publicPages.player.careerTable')">
        <div class="player-detail-data__panel-head">
          <strong>{{ t('publicPages.player.careerData') }}</strong>
          <div class="player-detail-stat-switch" role="group" :aria-label="t('publicPages.player.statType')">
            <button
              type="button"
              class="player-detail-stat-switch__btn"
              :class="{ 'is-active': statMode === 'average' }"
              @click="statMode = 'average'"
            >
              {{ t('publicPages.player.perGame') }}
            </button>
            <button
              type="button"
              class="player-detail-stat-switch__btn"
              :class="{ 'is-active': statMode === 'total' }"
              @click="statMode = 'total'"
            >
              {{ t('publicPages.player.cumulative') }}
            </button>
          </div>
        </div>

        <div class="player-detail-data__panel-filter">
          <label class="player-detail-data__filter-label" for="player-career-stage-filter">{{ t('publicPages.player.statStage') }}</label>
          <select id="player-career-stage-filter" v-model="seasonFilter">
            <option :value="allStages">{{ t('publicPages.player.allStages') }}</option>
            <option
              v-for="row in detail.careerStats"
              :key="row.seasonId"
              :value="row.seasonId"
            >
              {{ row.tournamentName || t('publicPages.player.seasonWithId', { id: row.seasonId }) }}
            </option>
          </select>
          <span class="player-detail-data__hint">{{ t('publicPages.player.perGameHint') }}</span>
        </div>

        <div v-if="careerRows.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noCareerData') }}</div>

        <div v-else class="player-detail-table-scroll">
          <table class="player-detail-table player-detail-table--career">
            <colgroup>
              <col class="col-event" />
              <col class="col-team" />
              <col class="col-platform" />
              <col class="col-gp" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-shooting" />
              <col class="col-shooting" />
              <col class="col-shooting" />
            </colgroup>
            <thead>
              <tr>
                <th class="col-event">{{ t('publicPages.player.event') }}</th>
                <th class="col-team">{{ t('publicPages.player.team') }}</th>
                <th class="col-platform">{{ t('publicPages.player.platformCol') }}</th>
                <th class="col-gp">{{ t('publicPages.player.gp') }} <small>GP</small></th>
                <th class="col-stat">{{ t('publicPages.player.points') }} <small>PTS</small></th>
                <th class="col-stat">{{ t('publicPages.player.rebounds') }} <small>REB</small></th>
                <th class="col-stat">{{ t('publicPages.player.assists') }} <small>AST</small></th>
                <th class="col-stat">{{ t('publicPages.player.steals') }} <small>STL</small></th>
                <th class="col-stat">{{ t('publicPages.player.blocks') }} <small>BLK</small></th>
                <th class="col-stat">{{ t('publicPages.player.turnovers') }} <small>TOV</small></th>
                <th class="col-stat">{{ t('publicPages.player.fouls') }} <small>PF</small></th>
                <th class="col-shooting">{{ t('publicPages.player.fgPct') }} <small>FG</small></th>
                <th class="col-shooting">{{ t('publicPages.player.tpPct') }} <small>3P</small></th>
                <th class="col-shooting">{{ t('publicPages.player.ftPct') }} <small>FT</small></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in careerRows" :key="`${row.seasonId}-${row.teamId}`">
                <td class="col-event" :title="row.tournamentName">{{ row.tournamentName }}</td>
                <td class="col-team">
                  <router-link
                    v-if="row.teamId"
                    :to="`/tournament/team/${row.teamId}/overview`"
                    class="player-detail-team-link player-data-team"
                  >
                    <TeamLogo
                      :team="{ id: row.teamId, name: row.teamName, shortName: row.teamName, logoUrl: row.teamLogoUrl }"
                      size="xs"
                    />
                    <span :title="row.teamName">{{ row.teamName }}</span>
                  </router-link>
                  <span v-else class="player-detail-team-link player-data-team" :title="row.teamName">{{ row.teamName }}</span>
                </td>
                <td class="col-platform">{{ row.platform }}</td>
                <td class="col-gp">{{ row.games }}</td>
                <td class="col-stat is-accent">{{ formatStat(row.points) }}</td>
                <td class="col-stat">{{ formatStat(row.rebounds) }}</td>
                <td class="col-stat is-accent">{{ formatStat(row.assists) }}</td>
                <td class="col-stat is-accent">{{ formatStat(row.steals) }}</td>
                <td class="col-stat">{{ formatStat(row.blocks) }}</td>
                <td class="col-stat">{{ formatStat(row.turnovers) }}</td>
                <td class="col-stat">{{ formatStat(row.fouls) }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(row, 'fg') }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(row, 'tp') }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(row, 'ft') }}</td>
              </tr>
              <tr v-if="careerAverageRow" class="is-summary">
                <td class="col-event">{{ careerAverageRow.tournamentName }}</td>
                <td class="col-team">-</td>
                <td class="col-platform">-</td>
                <td class="col-gp">{{ careerAverageRow.games }}</td>
                <td class="col-stat is-accent">{{ formatStat(careerAverageRow.points) }}</td>
                <td class="col-stat">{{ formatStat(careerAverageRow.rebounds) }}</td>
                <td class="col-stat is-accent">{{ formatStat(careerAverageRow.assists) }}</td>
                <td class="col-stat is-accent">{{ formatStat(careerAverageRow.steals) }}</td>
                <td class="col-stat">{{ formatStat(careerAverageRow.blocks) }}</td>
                <td class="col-stat">{{ formatStat(careerAverageRow.turnovers) }}</td>
                <td class="col-stat">{{ formatStat(careerAverageRow.fouls) }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(careerAverageRow, 'fg') }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(careerAverageRow, 'tp') }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(careerAverageRow, 'ft') }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <p v-if="careerRows.length" class="player-detail-data__source">
          {{ t('publicPages.player.dataSource') }}
        </p>
      </section>
    </template>

    <template v-else>
      <section class="player-detail-data__panel" :aria-label="t('publicPages.player.eventTable')">
        <div class="player-detail-data__panel-head">
          <strong>{{ selectedStage?.tournamentName || t('publicPages.player.eventData') }}</strong>
          <span class="player-detail-data__panel-meta">
            {{ t('publicPages.player.stageGames', { stage: selectedStage?.stageLabel || t('publicPages.player.regular'), count: selectedStage?.games || 0 }) }}
          </span>
        </div>

        <div class="player-detail-data__panel-filter">
          <div ref="dropdownRef" class="player-detail-stage-selector" :class="{ 'is-open': dropdownOpen }">
            <button type="button" class="player-detail-stage-selector__trigger" @click.stop="toggleDropdown">
              {{ selectedStage?.label || t('publicPages.player.noSeason') }}
              <span class="player-detail-stage-selector__arrow" aria-hidden="true" />
            </button>
            <div v-show="dropdownOpen" class="player-detail-stage-selector__dropdown">
              <button
                v-for="option in detail.seasonStageOptions"
                :key="option.id"
                type="button"
                class="player-detail-stage-selector__option"
                :class="{ 'is-selected': option.id === selectedStageId }"
                @click="selectStage(option.id)"
              >
                {{ option.label }}
                <span class="player-detail-stage-selector__check" aria-hidden="true" />
              </button>
            </div>
          </div>
        </div>

        <div v-if="seasonStatRows.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noEventData') }}</div>

        <div v-else class="player-detail-table-scroll">
          <table class="player-detail-table player-detail-table--career player-detail-table--season">
            <colgroup>
              <col class="col-team" />
              <col class="col-gp" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-stat" />
              <col class="col-shooting" />
              <col class="col-shooting" />
              <col class="col-shooting" />
            </colgroup>
            <thead>
              <tr>
                <th class="col-team">{{ t('publicPages.player.team') }}</th>
                <th class="col-gp">{{ t('publicPages.player.gp') }}</th>
                <th class="col-stat">{{ t('publicPages.player.points') }}</th>
                <th class="col-stat">{{ t('publicPages.player.rebounds') }}</th>
                <th class="col-stat">{{ t('publicPages.player.assists') }}</th>
                <th class="col-stat">{{ t('publicPages.player.steals') }}</th>
                <th class="col-stat">{{ t('publicPages.player.blocks') }}</th>
                <th class="col-shooting">{{ t('publicPages.player.fgPct') }}</th>
                <th class="col-shooting">{{ t('publicPages.player.tpPct') }}</th>
                <th class="col-shooting">{{ t('publicPages.player.ftPct') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in seasonStatRows" :key="row.seasonId">
                <td class="col-team" :title="row.teamName">
                  <span class="player-data-team-name">{{ row.teamName }}</span>
                </td>
                <td class="col-gp">{{ row.games }}</td>
                <td class="col-stat is-accent">{{ formatStat(row.points) }}</td>
                <td class="col-stat">{{ formatStat(row.rebounds) }}</td>
                <td class="col-stat is-accent">{{ formatStat(row.assists) }}</td>
                <td class="col-stat is-accent">{{ formatStat(row.steals) }}</td>
                <td class="col-stat">{{ formatStat(row.blocks) }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(row, 'fg') }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(row, 'tp') }}</td>
                <td class="col-shooting is-accent">{{ formatShootingPct(row, 'ft') }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-data {
  box-sizing: border-box;
}

.player-detail-data__intro {
  margin-bottom: 22px;
}

.player-detail-data__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
  font-size: clamp(28px, 2.6vw, 36px);
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--cnpa-paper-text);

  span {
    color: var(--cnpa-red);
    font-size: 0.42em;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.14em;
  }
}

.player-detail-data__desc {
  margin: 8px 0 0;
  max-width: 640px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.player-detail-data__mode-tabs {
  display: inline-flex;
  margin: 0 0 22px;
  overflow: hidden;
  border: 1px solid var(--cnpa-public-border);
  border-radius: 4px;
  background: #fff;
}

.player-detail-data__mode-tab {
  position: relative;
  min-width: 124px;
  height: 42px;
  margin: 0;
  padding: 0 22px;
  border: 0;
  background: #fff;
  color: #666;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;

  &.is-active {
    background: #171717;
    color: #fff;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 3px;
      background: var(--cnpa-red);
    }
  }
}

.player-detail-data__panel {
  overflow: hidden;
  border: 1px solid var(--cnpa-public-border);
  background: #fff;
}

.player-detail-data__panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 56px;
  padding: 0 20px;
  background: #171717;
  color: #fff;
}

.player-detail-data__panel-head strong {
  font-size: 16px;
  font-weight: 800;
}

.player-detail-data__panel-meta {
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
  font-weight: 700;
}

.player-detail-data__panel-head .player-detail-stat-switch {
  gap: 6px;
}

.player-detail-data__panel-head .player-detail-stat-switch__btn {
  min-width: 64px;
  height: 32px;
  padding: 0 14px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: rgba(255, 255, 255, 0.55);

  &.is-active {
    background: var(--cnpa-red);
    color: #fff;
    box-shadow: none;
  }
}

.player-detail-data__panel-filter {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 20px;
  background: #f6f5f4;
  border-bottom: 1px solid var(--cnpa-public-border);
}

.player-detail-data__filter-label {
  color: #555;
  font-size: 13px;
  font-weight: 800;
}

.player-detail-data__panel-filter select {
  min-width: 180px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--cnpa-public-border);
  border-radius: 3px;
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
}

.player-detail-data__hint {
  margin-left: auto;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-data__source {
  margin: 0;
  padding: 12px 20px 16px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-data__panel .player-detail-table-scroll {
  border: 0;
  box-shadow: none;
}

.player-detail-data__panel .player-detail-table--career {
  width: 100%;
  min-width: 1240px;
  table-layout: fixed;

  col.col-event { width: 200px; }
  col.col-team { width: 168px; }
  col.col-platform { width: 80px; }
  col.col-gp { width: 64px; }
  col.col-stat { width: 56px; }
  col.col-shooting { width: 72px; }

  .col-event,
  .col-team {
    overflow: hidden;
    max-width: 0;
    text-align: left;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .col-platform,
  .col-gp,
  .col-stat,
  .col-shooting {
    overflow: hidden;
    text-align: center;
    white-space: nowrap;
  }
}

.player-detail-data__panel .player-detail-table--season {
  min-width: 920px;

  col.col-team { width: 200px; }
  col.col-gp { width: 72px; }
  col.col-stat { width: 72px; }
  col.col-shooting { width: 88px; }
}

.player-data-team {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  max-width: 100%;

  :deep(.team-logo),
  :deep(.team-logo--xs) {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
  }

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.player-data-team-name {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-detail-data__panel .player-detail-table--career thead th {
  height: 42px;
  background: #f0eeec;
  color: #555;
  font-size: 12px;
  font-weight: 800;
  border-bottom: 1px solid var(--cnpa-public-border);

  small {
    margin-left: 4px;
    color: #999;
    font-size: 10px;
    font-weight: 700;
    letter-spacing: 0.04em;
  }
}

.player-detail-data__panel .player-detail-table--career tbody td {
  background: #fff;
}

.player-detail-data__panel .player-detail-table--career tbody tr:nth-child(even) td {
  background: #f7f6f5;
}

.player-detail-data__panel .player-detail-table--career tbody tr:hover td {
  background: #f3f1ee;
}

.player-detail-data__panel .player-detail-table--career .is-accent {
  color: var(--cnpa-red);
  font-weight: 800;
}

.player-detail-data__panel .player-detail-table--career tbody tr.is-summary td {
  background: #ebe6dc;
  font-weight: 800;
}

.player-detail-empty-inline {
  margin: 16px 20px 20px;
  padding: 32px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.player-detail-stage-selector {
  position: relative;
  width: min(100%, 320px);
}

.player-detail-stage-selector__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 4px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.player-detail-stage-selector__arrow {
  width: 8px;
  height: 8px;
  border-right: 1.5px solid #666;
  border-bottom: 1.5px solid #666;
  transform: rotate(45deg);
}

.player-detail-stage-selector__dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 20;
  width: 100%;
  padding: 8px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 4px;
  background: var(--cnpa-paper-surface);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
}

.player-detail-stage-selector__option {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 34px;
  margin: 0 0 4px;
  padding: 0 34px 0 10px;
  border: 0;
  border-radius: 3px;
  background: transparent;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  text-align: left;
  cursor: pointer;

  &:hover {
    background: rgba(25, 25, 25, 0.04);
  }

  &.is-selected {
    background: rgba(225, 6, 19, 0.08);
    color: var(--cnpa-red);
  }
}

.player-detail-stage-selector__check {
  position: absolute;
  right: 10px;
  display: none;
  width: 14px;
  height: 14px;
  border: 1.5px solid var(--cnpa-red);
  border-radius: 50%;

  &::before {
    content: '';
    position: absolute;
    left: 3px;
    top: 2px;
    width: 5px;
    height: 3px;
    border-left: 1.5px solid var(--cnpa-red);
    border-bottom: 1.5px solid var(--cnpa-red);
    transform: rotate(-45deg);
  }
}

.player-detail-stage-selector__option.is-selected .player-detail-stage-selector__check {
  display: block;
}

.player-detail-data__mode-tab:focus-visible,
.player-detail-data__panel-filter select:focus-visible,
.player-detail-stage-selector__trigger:focus-visible,
.player-detail-stage-selector__option:focus-visible {
  outline: 2px solid rgba(225, 6, 19, 0.4);
  outline-offset: 2px;
}

@media (max-width: 767px) {
  .player-detail-data__panel-head,
  .player-detail-data__panel-filter {
    align-items: flex-start;
    flex-direction: column;
  }

  .player-detail-data__hint {
    margin-left: 0;
  }

  .player-detail-data__panel-filter select,
  .player-detail-stage-selector {
    width: 100%;
    min-width: 0;
  }
}
</style>
