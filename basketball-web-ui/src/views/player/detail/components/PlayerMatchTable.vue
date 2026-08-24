<script setup lang="ts">
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { PlayerMatchRecord } from '../types'

withDefaults(
  defineProps<{
    rows: PlayerMatchRecord[]
    compact?: boolean
    showView?: boolean
  }>(),
  {
    compact: false,
    showView: true,
  },
)

const router = useRouter()

function formatResult(row: PlayerMatchRecord): string {
  if (row.resultLabel) {
    if (row.seriesScoreText) return `${row.resultLabel} ${row.seriesScoreText}`
    if (row.originalTeamScore != null && row.originalOpponentScore != null) {
      return `${row.resultLabel}（原比分 ${row.originalTeamScore}:${row.originalOpponentScore} 成绩已取消）`
    }
    return row.resultLabel
  }
  if (row.seriesScoreText) {
    return `${row.result === 'WIN' ? '胜利' : '失败'} ${row.seriesScoreText}`
  }
  const prefix = row.result === 'WIN' ? '胜利' : '失败'
  return `${prefix} ${row.teamScore}:${row.opponentScore}`
}

function formatLine(row: PlayerMatchRecord): string {
  return `${row.points}分 ${row.rebounds}板 ${row.assists}助`
}

function formatShot(made: number | null | undefined, attempted: number | null | undefined): string {
  if (made === null || attempted === null || made === undefined || attempted === undefined) {
    return '--'
  }
  // 无出手记录时不展示假 0/0
  if (Number(attempted) === 0 && Number(made) === 0) {
    return '--'
  }
  return `${made}/${attempted}`
}

function goTeam(teamId: string) {
  const divisionId = String(router.currentRoute.value.query.divisionId || '').trim()
  router.push({
    path: `/tournament/team/${teamId}/overview`,
    query: divisionId ? { divisionId } : {},
  })
}

function openStats(matchId: string) {
  router.push({ path: '/tournament', query: { tab: 'schedule', stats: matchId } })
}
</script>

<template>
  <div class="player-detail-table-scroll">
    <table class="player-detail-table">
      <thead>
        <tr v-if="compact">
          <th>日期</th>
          <th>对手</th>
          <th>比赛结果</th>
          <th>个人数据</th>
          <th>赛段</th>
          <th>查看</th>
        </tr>
        <tr v-else>
          <th>日期</th>
          <th>赛事</th>
          <th>对手</th>
          <th>战队</th>
          <th>比赛结果</th>
          <th>出场时间</th>
          <th>得分</th>
          <th>篮板</th>
          <th>助攻</th>
          <th>抢断</th>
          <th>盖帽</th>
          <th>失误</th>
          <th>投篮</th>
          <th>三分</th>
          <th>罚球</th>
          <th>查看</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <template v-if="compact">
            <td>{{ row.date }}</td>
            <td>{{ row.opponentName }}</td>
            <td :class="row.result === 'WIN' ? 'player-detail-result--win' : 'player-detail-result--loss'">
              {{ formatResult(row) }}
            </td>
            <td>{{ formatLine(row) }}</td>
            <td>{{ row.stageName }}</td>
            <td>
              <button v-if="showView" type="button" class="player-detail-view-btn" @click="openStats(row.matchId)">
                技术统计
              </button>
            </td>
          </template>
          <template v-else>
            <td>{{ row.date }}</td>
            <td>{{ row.tournamentName }}</td>
            <td>{{ row.opponentName }}</td>
            <td class="is-left">
              <button type="button" class="player-detail-team-link" @click="goTeam(row.teamId)">
                <TeamLogo
                  :team="{ id: row.teamId, name: row.teamName, shortName: row.teamName, logoUrl: row.teamLogoUrl }"
                  size="xs"
                />
                <span>{{ row.teamName }}</span>
              </button>
            </td>
            <td :class="row.result === 'WIN' ? 'player-detail-result--win' : 'player-detail-result--loss'">
              {{ formatResult(row) }}
            </td>
            <td>{{ row.minutes }}</td>
            <td>{{ row.points }}</td>
            <td>{{ row.rebounds }}</td>
            <td>{{ row.assists }}</td>
            <td>{{ row.steals }}</td>
            <td>{{ row.blocks }}</td>
            <td>{{ row.turnovers }}</td>
            <td>{{ formatShot(row.fieldGoalsMade, row.fieldGoalsAttempted) }}</td>
            <td>{{ formatShot(row.threePointsMade, row.threePointsAttempted) }}</td>
            <td>{{ formatShot(row.freeThrowsMade, row.freeThrowsAttempted) }}</td>
            <td>
              <button v-if="showView" type="button" class="player-detail-view-btn" @click="openStats(row.matchId)">
                技术统计
              </button>
            </td>
          </template>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-view-btn {
  border-color: var(--cnpa-public-border);
  background: var(--cnpa-public-card-bg);
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.player-detail-view-btn:hover {
  background: var(--cnpa-public-card-bg-soft);
}
</style>
