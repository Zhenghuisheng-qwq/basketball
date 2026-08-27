<script setup lang="ts">
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { publicContestDetailLabel, publicContestDetailPath } from '@/utils/v4Display'
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

function resultLabel(row: PlayerMatchRecord): string {
  if (row.resultLabel) return row.resultLabel
  return row.result === 'WIN' ? '胜利' : '失败'
}

function scoreText(row: PlayerMatchRecord): string {
  if (row.seriesScoreText) return row.seriesScoreText
  if (row.originalTeamScore != null && row.originalOpponentScore != null) {
    return `${row.originalTeamScore} : ${row.originalOpponentScore}`
  }
  return `${row.teamScore} : ${row.opponentScore}`
}

function formatLine(row: PlayerMatchRecord): string {
  return `${row.points}分 ${row.rebounds}板 ${row.assists}助`
}

function formatShot(made: number | null | undefined, attempted: number | null | undefined): string {
  if (made === null || attempted === null || made === undefined || attempted === undefined) {
    return '--'
  }
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

function openMatchDetail(matchId: string) {
  const id = String(matchId || '').trim()
  if (!id) return
  router.push(publicContestDetailPath({ unitType: 'MATCH', matchId: id }) || `/events/match/${id}`)
}

function openSeriesDetail(seriesId?: string) {
  const path = publicContestDetailPath({
    unitType: 'MATCH_SERIES',
    matchSeriesId: seriesId,
  })
  if (!path) return
  router.push(path)
}

function opponentTeam(row: PlayerMatchRecord) {
  return {
    id: row.opponentId,
    name: row.opponentName,
    shortName: row.opponentName,
    logoUrl: row.opponentLogoUrl,
  }
}
</script>

<template>
  <div class="player-detail-table-scroll" :class="{ 'is-compact': compact }">
    <table class="player-detail-table" :class="{ 'is-compact': compact }">
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
            <td class="is-left">
              <span class="player-detail-opponent">
                <TeamLogo :team="opponentTeam(row)" size="xs" />
                <span>{{ row.opponentName }}</span>
              </span>
            </td>
            <td>
              <span class="player-detail-result" :class="row.result === 'WIN' ? 'is-win' : 'is-loss'">
                <em>{{ resultLabel(row) }}</em>
                <strong>{{ scoreText(row) }}</strong>
              </span>
            </td>
            <td>{{ formatLine(row) }}</td>
            <td>{{ row.stageName }}</td>
            <td>
              <div v-if="showView" class="player-detail-actions">
                <button type="button" class="player-detail-action" @click="openMatchDetail(row.matchId)">
                  {{ publicContestDetailLabel('MATCH') }} →
                </button>
                <button
                  v-if="row.matchSeriesId"
                  type="button"
                  class="player-detail-action"
                  @click="openSeriesDetail(row.matchSeriesId)"
                >
                  {{ publicContestDetailLabel('MATCH_SERIES') }} →
                </button>
              </div>
            </td>
          </template>
          <template v-else>
            <td>{{ row.date }}</td>
            <td>{{ row.tournamentName }}</td>
            <td class="is-left">
              <span class="player-detail-opponent">
                <TeamLogo :team="opponentTeam(row)" size="xs" />
                <span>{{ row.opponentName }}</span>
              </span>
            </td>
            <td class="is-left">
              <button type="button" class="player-detail-team-link" @click="goTeam(row.teamId)">
                <TeamLogo
                  :team="{ id: row.teamId, name: row.teamName, shortName: row.teamName, logoUrl: row.teamLogoUrl }"
                  size="xs"
                />
                <span>{{ row.teamName }}</span>
              </button>
            </td>
            <td>
              <span class="player-detail-result" :class="row.result === 'WIN' ? 'is-win' : 'is-loss'">
                <em>{{ resultLabel(row) }}</em>
                <strong>{{ scoreText(row) }}</strong>
              </span>
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
              <div v-if="showView" class="player-detail-actions">
                <button type="button" class="player-detail-action" @click="openMatchDetail(row.matchId)">
                  {{ publicContestDetailLabel('MATCH') }} →
                </button>
              </div>
            </td>
          </template>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-table-scroll.is-compact {
  border: 0;
  box-shadow: none;
  background: transparent;
}

.player-detail-opponent {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.player-detail-result {
  display: inline-flex;
  align-items: baseline;
  gap: 8px;

  em {
    font-style: normal;
    font-size: 13px;
    font-weight: 800;
  }

  strong {
    font-size: 15px;
    font-weight: 800;
  }

  &.is-win em,
  &.is-win strong {
    color: var(--cnpa-red, #e10613);
  }

  &.is-loss em,
  &.is-loss strong {
    color: #333;
  }
}

.player-detail-actions {
  display: inline-flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.player-detail-action {
  border: 0;
  padding: 0;
  background: transparent;
  color: #555;
  font-size: 13px;
  font-weight: 700;
  font-family: inherit;
  cursor: pointer;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}
</style>
