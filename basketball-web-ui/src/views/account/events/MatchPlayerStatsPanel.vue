<script setup lang="ts">
import { computed, ref } from 'vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import type { MatchDetailPlayerStat } from '@/api/matchData'
import type { EditablePlayerStatRow, MatchTeamSide } from '@/utils/matchDataTabHelpers'
import { formatPositionLabel } from '@/constants/rosterRules'
import { sanitizeStatInput } from '@/utils/matchDataTabHelpers'

const props = defineProps<{
  homeTeamName?: string
  awayTeamName?: string
  homeStats?: MatchDetailPlayerStat[]
  awayStats?: MatchDetailPlayerStat[]
  myTeamSide?: MatchTeamSide | null
  editing?: boolean
  editStats?: EditablePlayerStatRow[]
}>()

const emit = defineEmits<{
  'update-stat': [playerId: string, field: keyof EditablePlayerStatRow, value: number]
}>()

const homePage = ref(1)
const awayPage = ref(1)
const homePageSize = ref(10)
const awayPageSize = ref(10)

const statFields = ['points', 'rebounds', 'assists', 'steals', 'blocks', 'turnovers', 'fouls'] as const

const homeRows = computed(() => props.homeStats || [])
const awayRows = computed(() => props.awayStats || [])

const homePageRows = computed(() => {
  const start = (homePage.value - 1) * homePageSize.value
  return homeRows.value.slice(start, start + homePageSize.value)
})

const awayPageRows = computed(() => {
  const start = (awayPage.value - 1) * awayPageSize.value
  return awayRows.value.slice(start, start + awayPageSize.value)
})

const editPageRows = computed(() => {
  const rows = props.editStats || []
  const start = (props.myTeamSide === 'HOME' ? homePage.value - 1 : awayPage.value - 1)
    * (props.myTeamSide === 'HOME' ? homePageSize.value : awayPageSize.value)
  const size = props.myTeamSide === 'HOME' ? homePageSize.value : awayPageSize.value
  return rows.slice(start, start + size)
})

function updateHomePageSize(size: number) {
  homePageSize.value = size
  homePage.value = 1
}

function updateAwayPageSize(size: number) {
  awayPageSize.value = size
  awayPage.value = 1
}

function statCell(value?: number | null) {
  return value == null ? '-' : String(value)
}

function formatPosition(position?: string) {
  if (!position || position === '-') return '-'
  return formatPositionLabel(position)
}

function displayName(row: MatchDetailPlayerStat) {
  return row.playerName || row.playerNo || '-'
}

function isEditableSide(side: MatchTeamSide) {
  return Boolean(props.editing && props.myTeamSide === side)
}

function onStatInput(playerId: string | undefined, field: typeof statFields[number], raw: string) {
  if (!playerId) return
  const max = field === 'fouls' ? 6 : 999
  emit('update-stat', playerId, field, sanitizeStatInput(raw, max))
}

function sideBadge(side: MatchTeamSide) {
  if (props.myTeamSide === side && props.editing) return '可编辑'
  if (props.myTeamSide === side) return '本队'
  return '只读'
}
</script>

<template>
  <div class="match-player-stats">
    <section class="match-player-stats__block">
      <header class="match-player-stats__head">
        <div class="match-player-stats__title-wrap">
          <h3>主队球员数据</h3>
          <span
            class="match-player-stats__badge"
            :class="{ 'match-player-stats__badge--edit': isEditableSide('HOME') }"
          >
            {{ sideBadge('HOME') }}
          </span>
        </div>
        <span>{{ homeTeamName || '主队' }} · {{ homeRows.length }} 人</span>
      </header>
      <div v-if="!homeRows.length && !isEditableSide('HOME')" class="match-player-stats__empty">暂无主队球员数据</div>
      <template v-else>
        <div class="match-player-stats__table-wrap">
          <table class="match-player-stats__table">
            <thead>
              <tr>
                <th>球员</th>
                <th>位置</th>
                <th>游戏 ID</th>
                <th v-for="field in statFields" :key="`home-${field}`">{{ field === 'points' ? '得分' : field === 'rebounds' ? '篮板' : field === 'assists' ? '助攻' : field === 'steals' ? '抢断' : field === 'blocks' ? '盖帽' : field === 'turnovers' ? '失误' : '犯规' }}</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in isEditableSide('HOME') ? editPageRows : homePageRows"
                :key="row.playerId || row.playerNo"
              >
                <td>
                  <div class="match-player-stats__player">
                    <PlayerAvatar
                      :name="displayName(row)"
                      :avatar-url="row.avatarUrl"
                      :player-no="row.playerNo"
                      :player-id="row.playerId"
                      size="sm"
                    />
                    <div>
                      <strong>{{ displayName(row) }}</strong>
                      <span class="match-player-stats__sub">{{ row.playerNo || '-' }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ formatPosition(row.position) }}</td>
                <td class="match-player-stats__ellipsis" :title="row.gameId || '-'">{{ row.gameId || '-' }}</td>
                <td v-for="field in statFields" :key="`home-cell-${row.playerId}-${field}`">
                  <input
                    v-if="isEditableSide('HOME')"
                    class="match-player-stats__input"
                    type="number"
                    min="0"
                    :max="field === 'fouls' ? 6 : 999"
                    step="1"
                    :value="(row as EditablePlayerStatRow)[field]"
                    @input="onStatInput(row.playerId, field, ($event.target as HTMLInputElement).value)"
                  />
                  <span v-else>{{ statCell(row[field]) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <MyEventsPagination
          :page="homePage"
          :page-size="homePageSize"
          :total="isEditableSide('HOME') ? (editStats?.length || 0) : homeRows.length"
          @update:page="homePage = $event"
          @update:page-size="updateHomePageSize"
        />
      </template>
    </section>

    <section class="match-player-stats__block">
      <header class="match-player-stats__head">
        <div class="match-player-stats__title-wrap">
          <h3>客队球员数据</h3>
          <span
            class="match-player-stats__badge"
            :class="{ 'match-player-stats__badge--edit': isEditableSide('AWAY') }"
          >
            {{ sideBadge('AWAY') }}
          </span>
        </div>
        <span>{{ awayTeamName || '客队' }} · {{ awayRows.length }} 人</span>
      </header>
      <div v-if="!awayRows.length && !isEditableSide('AWAY')" class="match-player-stats__empty">暂无客队球员数据</div>
      <template v-else>
        <div class="match-player-stats__table-wrap">
          <table class="match-player-stats__table">
            <thead>
              <tr>
                <th>球员</th>
                <th>位置</th>
                <th>游戏 ID</th>
                <th v-for="field in statFields" :key="`away-${field}`">{{ field === 'points' ? '得分' : field === 'rebounds' ? '篮板' : field === 'assists' ? '助攻' : field === 'steals' ? '抢断' : field === 'blocks' ? '盖帽' : field === 'turnovers' ? '失误' : '犯规' }}</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in isEditableSide('AWAY') ? editPageRows : awayPageRows"
                :key="row.playerId || row.playerNo"
              >
                <td>
                  <div class="match-player-stats__player">
                    <PlayerAvatar
                      :name="displayName(row)"
                      :avatar-url="row.avatarUrl"
                      :player-no="row.playerNo"
                      :player-id="row.playerId"
                      size="sm"
                    />
                    <div>
                      <strong>{{ displayName(row) }}</strong>
                      <span class="match-player-stats__sub">{{ row.playerNo || '-' }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ formatPosition(row.position) }}</td>
                <td class="match-player-stats__ellipsis" :title="row.gameId || '-'">{{ row.gameId || '-' }}</td>
                <td v-for="field in statFields" :key="`away-cell-${row.playerId}-${field}`">
                  <input
                    v-if="isEditableSide('AWAY')"
                    class="match-player-stats__input"
                    type="number"
                    min="0"
                    :max="field === 'fouls' ? 6 : 999"
                    step="1"
                    :value="(row as EditablePlayerStatRow)[field]"
                    @input="onStatInput(row.playerId, field, ($event.target as HTMLInputElement).value)"
                  />
                  <span v-else>{{ statCell(row[field]) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <MyEventsPagination
          :page="awayPage"
          :page-size="awayPageSize"
          :total="isEditableSide('AWAY') ? (editStats?.length || 0) : awayRows.length"
          @update:page="awayPage = $event"
          @update:page-size="updateAwayPageSize"
        />
      </template>
    </section>
  </div>
</template>

<style scoped lang="scss">
.match-player-stats {
  display: grid;
  grid-template-columns: 1fr;
  gap: 24px;
}

.match-player-stats__block {
  min-width: 0;
}

.match-player-stats__head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 800;
    color: #111;
  }

  span {
    font-size: 13px;
    color: #666;
  }
}

.match-player-stats__title-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.match-player-stats__badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  color: #666;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(0, 0, 0, 0.03);
}

.match-player-stats__badge--edit {
  color: #c80a10;
  border-color: rgba(200, 10, 16, 0.35);
  background: rgba(200, 10, 16, 0.08);
}

.match-player-stats__empty {
  padding: 28px 0;
  text-align: center;
  color: #666;
  font-size: 14px;
  border: 1px dashed rgba(0, 0, 0, 0.12);
  border-radius: 2px;
}

.match-player-stats__table-wrap {
  overflow-x: auto;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 2px;
  background: #fff;
}

.match-player-stats__table {
  width: 100%;
  min-width: 780px;
  border-collapse: collapse;
  font-size: 13px;

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    text-align: left;
    vertical-align: middle;
    color: #111;
    white-space: nowrap;
  }

  th {
    font-size: 12px;
    font-weight: 800;
    color: #555;
    background: rgba(0, 0, 0, 0.03);
  }

  tbody tr:last-child td {
    border-bottom: none;
  }
}

.match-player-stats__player {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;

  strong {
    display: block;
    margin-bottom: 2px;
  }
}

.match-player-stats__sub {
  display: block;
  font-size: 11px;
  color: #888;
}

.match-player-stats__input {
  width: 72px;
  max-width: 100%;
  height: 32px;
  padding: 0 8px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 2px;
  font-size: 13px;
  font-weight: 600;
  color: #111;
  background: #fff;
  box-sizing: border-box;

  &:focus {
    outline: none;
    border-color: rgba(200, 10, 16, 0.45);
    box-shadow: 0 0 0 2px rgba(200, 10, 16, 0.08);
  }
}

.match-player-stats__ellipsis {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
