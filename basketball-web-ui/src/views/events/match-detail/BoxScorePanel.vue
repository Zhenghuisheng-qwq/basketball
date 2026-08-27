<script setup lang="ts">
import { computed } from 'vue'
import type { Team } from '@/views/events/types/events'
import type { V4PublicMatchPlayerStatItem, V4PublicMatchTeamStatItem } from '@/api/types/v4Public'
import TeamLogo from '@/views/events/components/TeamLogo.vue'
import {
  formatShooting,
  formatStatCell,
  officialTeamTotal,
  playersOfTeam,
  toBoxScoreRow,
} from './matchDetailDisplay'

const props = withDefaults(defineProps<{
  teams: Team[]
  selectedTeamId: string
  players: V4PublicMatchPlayerStatItem[]
  teamStats?: V4PublicMatchTeamStatItem[]
  title?: string
  subtitle?: string
  hint?: string
}>(), {
  title: '比赛技术统计',
  subtitle: 'BOX SCORE',
  hint: '',
})

const emit = defineEmits<{
  'update:selectedTeamId': [value: string]
  goPlayer: [sourcePlayerId?: string]
}>()

const currentTeam = computed(() => props.teams.find((team) => String(team.teamId) === String(props.selectedTeamId)) || props.teams[0])

const rows = computed(() => {
  const teamId = currentTeam.value?.teamId
  return playersOfTeam(props.players, teamId).map(toBoxScoreRow)
})

const totalRow = computed(() => {
  const teamId = currentTeam.value?.teamId
  const teamStat = (props.teamStats || []).find((row) => String(row.eventTeamId) === String(teamId))
  return officialTeamTotal(rows.value, teamStat)
})
</script>

<template>
  <section class="box-score">
    <div class="box-score__head">
      <div>
        <h2>{{ title }}</h2>
        <span>{{ subtitle }}</span>
        <small v-if="hint">{{ hint }}</small>
      </div>
      <div class="box-score__teams" role="tablist" aria-label="球队">
        <button
          v-for="team in teams"
          :key="team.teamId || team.name"
          type="button"
          class="box-score__team"
          :class="{ 'is-active': String(selectedTeamId) === String(team.teamId) }"
          @click="emit('update:selectedTeamId', String(team.teamId))"
        >
          <TeamLogo :team="team" size="sm" />
          <span>{{ team.name }}</span>
        </button>
      </div>
    </div>

    <div v-if="!rows.length" class="box-score__empty">暂无已确认的球员技术统计</div>
    <div v-else class="box-score__scroll">
      <table class="box-score__table">
        <thead>
          <tr>
            <th>球员</th>
            <th>得分</th>
            <th>篮板</th>
            <th>助攻</th>
            <th>抢断</th>
            <th>盖帽</th>
            <th>犯规</th>
            <th>失误</th>
            <th>投篮</th>
            <th>三分</th>
            <th>罚球</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.eventPlayerId">
            <td>
              <button
                v-if="row.sourcePlayerId"
                type="button"
                class="box-score__player"
                @click="emit('goPlayer', row.sourcePlayerId)"
              >
                {{ row.playerName }}
              </button>
              <span v-else>{{ row.playerName }}</span>
            </td>
            <td class="is-pts">{{ formatStatCell(row.points) }}</td>
            <td>{{ formatStatCell(row.rebound) }}</td>
            <td>{{ formatStatCell(row.assist) }}</td>
            <td>{{ formatStatCell(row.steal) }}</td>
            <td>{{ formatStatCell(row.block) }}</td>
            <td>{{ formatStatCell(row.foul) }}</td>
            <td>{{ formatStatCell(row.turnover) }}</td>
            <td>{{ formatShooting(row.fieldGoalMade, row.fieldGoalAttempt) }}</td>
            <td>{{ formatShooting(row.threePointMade, row.threePointAttempt) }}</td>
            <td>{{ formatShooting(row.freeThrowMade, row.freeThrowAttempt) }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr class="is-total">
            <td>{{ totalRow.playerName }}</td>
            <td class="is-pts">{{ formatStatCell(totalRow.points) }}</td>
            <td>{{ formatStatCell(totalRow.rebound) }}</td>
            <td>{{ formatStatCell(totalRow.assist) }}</td>
            <td>{{ formatStatCell(totalRow.steal) }}</td>
            <td>{{ formatStatCell(totalRow.block) }}</td>
            <td>{{ formatStatCell(totalRow.foul) }}</td>
            <td>{{ formatStatCell(totalRow.turnover) }}</td>
            <td>{{ formatShooting(totalRow.fieldGoalMade, totalRow.fieldGoalAttempt) }}</td>
            <td>{{ formatShooting(totalRow.threePointMade, totalRow.threePointAttempt) }}</td>
            <td>{{ formatShooting(totalRow.freeThrowMade, totalRow.freeThrowAttempt) }}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  </section>
</template>

<style scoped lang="scss">
.box-score__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 22px;
    font-weight: 900;
  }

  span {
    display: block;
    margin-top: 4px;
    color: var(--cnpa-red, #e51422);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 1.4px;
  }

  small {
    display: block;
    margin-top: 6px;
    color: #888;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0;
  }
}

.box-score__teams {
  display: flex;
  gap: 8px;
}

.box-score__team {
  min-height: 40px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid #ddd;
  background: #fff;
  color: #222;
  font-size: 13px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;

  &.is-active {
    background: #111;
    border-color: #111;
    color: #fff;
  }
}

.box-score__empty {
  padding: 36px 16px;
  text-align: center;
  color: #888;
  background: #f7f5f2;
}

.box-score__scroll {
  overflow-x: auto;
  max-width: 100%;
}

.box-score__table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;

  th,
  td {
    padding: 11px 10px;
    text-align: center;
    font-size: 14px;
    border-bottom: 1px solid #eceae7;
  }

  th:first-child,
  td:first-child {
    text-align: left;
    min-width: 120px;
  }

  thead th {
    background: #111;
    color: #fff;
    font-size: 12px;
    font-weight: 800;
  }

  .is-pts {
    color: var(--cnpa-red, #e51422);
    font-weight: 800;
  }

  .is-total {
    background: #efece7;
    font-weight: 800;
  }
}

.box-score__player {
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 720px) {
  .box-score__head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
