<script setup lang="ts">
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { hasMatchSeriesId, publicContestDetailLabel, publicContestDetailPath } from '@/utils/v4Display'
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
  if (
    row.fieldGoalsMade == null
    && row.fieldGoalsAttempted == null
    && !row.points
    && !row.rebounds
    && !row.assists
  ) {
    return '--'
  }
  return `${row.points}分 · ${row.rebounds}板 · ${row.assists}助`
}

function scoreView(row: PlayerMatchRecord): { left: string; right: string; text: string } {
  const text = scoreText(row)
  const matched = String(text).match(/^(\d+)\s*[:：\-]\s*(\d+)$/)
  if (matched) return { left: matched[1], right: matched[2], text: '' }
  return { left: '', right: '', text }
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
  <div v-if="compact" class="player-recent-matches">
    <div class="player-recent-match-grid player-recent-match-head" aria-hidden="true">
      <span>日期</span>
      <span>对手</span>
      <span>比赛结果</span>
      <span>个人数据</span>
      <span>赛段</span>
      <span>查看</span>
    </div>

    <article
      v-for="row in rows"
      :key="row.id"
      class="player-recent-match-grid player-recent-match-row"
    >
      <span>{{ row.date }}</span>

      <div class="player-recent-match-opponent">
        <TeamLogo :team="opponentTeam(row)" size="xs" />
        <span :title="row.opponentName">{{ row.opponentName }}</span>
      </div>

      <span class="player-detail-result" :class="row.result === 'WIN' ? 'is-win' : 'is-loss'">
        <em>{{ resultLabel(row) }}</em>
        <strong v-if="scoreView(row).left" class="player-detail-score">
          <b :class="{ 'is-winner': row.result === 'WIN' }">{{ scoreView(row).left }}</b>
          <i>:</i>
          <b>{{ scoreView(row).right }}</b>
        </strong>
        <strong v-else>{{ scoreView(row).text }}</strong>
      </span>

      <span>{{ formatLine(row) }}</span>
      <span>{{ row.stageName }}</span>

      <div v-if="showView" class="player-detail-actions">
        <button type="button" class="player-detail-action" @click="openMatchDetail(row.matchId)">
          {{ publicContestDetailLabel('MATCH') }}
        </button>
        <button
          v-if="hasMatchSeriesId(row.matchSeriesId)"
          type="button"
          class="player-detail-action player-detail-action--series"
          @click="openSeriesDetail(row.matchSeriesId)"
        >
          {{ publicContestDetailLabel('MATCH_SERIES') }}
        </button>
      </div>
      <span v-else />
    </article>
  </div>

  <div v-else class="player-detail-table-scroll">
    <table class="player-detail-table player-match-history-table">
      <colgroup>
        <col class="col-date" />
        <col class="col-opponent" />
        <col class="col-result" />
        <col class="col-stage" />
        <col class="col-minutes" />
        <col class="col-stat" />
        <col class="col-stat" />
        <col class="col-stat" />
        <col class="col-stat" />
        <col class="col-stat" />
        <col class="col-stat" />
        <col class="col-shooting" />
        <col class="col-shooting" />
        <col class="col-shooting" />
        <col class="col-action" />
      </colgroup>
      <thead>
        <tr>
          <th class="col-date">日期</th>
          <th class="col-opponent">对手</th>
          <th class="col-result">比赛结果</th>
          <th class="col-stage">赛段</th>
          <th class="col-minutes">出场时间</th>
          <th class="col-stat">得分</th>
          <th class="col-stat">篮板</th>
          <th class="col-stat">助攻</th>
          <th class="col-stat">抢断</th>
          <th class="col-stat">盖帽</th>
          <th class="col-stat">失误</th>
          <th class="col-shooting">投篮</th>
          <th class="col-shooting">三分</th>
          <th class="col-shooting">罚球</th>
          <th class="col-action">查看</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row.id">
          <td class="col-date">{{ row.date }}</td>
          <td class="col-opponent">
            <span class="player-detail-opponent">
              <TeamLogo :team="opponentTeam(row)" size="xs" />
              <span :title="row.opponentName">{{ row.opponentName }}</span>
            </span>
          </td>
          <td class="col-result">
            <span class="player-detail-result" :class="row.result === 'WIN' ? 'is-win' : 'is-loss'">
              <em>{{ resultLabel(row) }}</em>
              <strong v-if="scoreView(row).left" class="player-detail-score">
                <b :class="{ 'is-winner': row.result === 'WIN' }">{{ scoreView(row).left }}</b>
                <i>:</i>
                <b>{{ scoreView(row).right }}</b>
              </strong>
              <strong v-else>{{ scoreView(row).text }}</strong>
            </span>
          </td>
          <td class="col-stage">{{ row.stageName }}</td>
          <td class="col-minutes">{{ row.minutes }}</td>
          <td class="col-stat">{{ row.points }}</td>
          <td class="col-stat">{{ row.rebounds }}</td>
          <td class="col-stat">{{ row.assists }}</td>
          <td class="col-stat">{{ row.steals }}</td>
          <td class="col-stat">{{ row.blocks }}</td>
          <td class="col-stat">{{ row.turnovers }}</td>
          <td class="col-shooting">{{ formatShot(row.fieldGoalsMade, row.fieldGoalsAttempted) }}</td>
          <td class="col-shooting">{{ formatShot(row.threePointsMade, row.threePointsAttempted) }}</td>
          <td class="col-shooting">{{ formatShot(row.freeThrowsMade, row.freeThrowsAttempted) }}</td>
          <td class="col-action">
            <div v-if="showView" class="player-detail-actions">
              <button type="button" class="player-detail-action" @click="openMatchDetail(row.matchId)">
                {{ publicContestDetailLabel('MATCH') }}
              </button>
              <button
                v-if="hasMatchSeriesId(row.matchSeriesId)"
                type="button"
                class="player-detail-action player-detail-action--series"
                @click="openSeriesDetail(row.matchSeriesId)"
              >
                {{ publicContestDetailLabel('MATCH_SERIES') }}
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-recent-matches {
  width: 100%;
  overflow-x: auto;
}

.player-recent-match-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  align-items: center;

  > * {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 0;
    padding: 0 8px;
    box-sizing: border-box;
    text-align: center;
  }
}

.player-recent-match-head {
  min-height: 42px;
  background: #111;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.player-recent-match-row {
  min-height: 52px;
  border-bottom: 1px solid var(--cnpa-paper-border, rgba(17, 17, 17, 0.08));
  background: transparent;
}

.player-recent-match-opponent {
  gap: 8px;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.team-logo) {
    flex-shrink: 0;
    width: 32px;
    height: 32px;
    overflow: hidden;
  }

  :deep(.team-logo__img) {
    object-fit: contain;
  }
}

.player-recent-matches .player-detail-result {
  align-items: center;
  gap: 6px;
}

.player-detail-table:not(.is-compact) {
  min-width: 1180px;

  .col-date { width: 9%; }
  .col-opponent { width: 12%; }
  .col-result { width: 10%; }
  .col-stage { width: 10%; }
  .col-minutes { width: 6%; }
  .col-stat { width: 5%; }
  .col-shooting { width: 5%; }
  .col-action { width: 156px; }
}

.player-match-history-table {
  thead th,
  tbody td {
    text-align: center;
    vertical-align: middle;
    line-height: 1.3;
  }
}

.player-match-history-table .col-opponent,
.player-match-history-table .col-stage {
  overflow: hidden;
}

.player-match-history-table .player-detail-opponent {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  max-width: 100%;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.team-logo) {
    display: block;
    flex-shrink: 0;
    align-self: center;
    width: 32px;
    height: 32px;
    overflow: hidden;
  }

  :deep(.team-logo__img) {
    display: block;
    object-fit: contain;
  }
}

.player-match-history-table .player-detail-result {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.player-match-history-table .player-detail-score {
  align-items: center;
}

.player-match-history-table .player-detail-actions {
  justify-content: center;
  align-items: center;
}

.player-detail-opponent {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  max-width: 100%;

  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.player-detail-result.is-win,
.player-detail-result.is-loss {
  color: inherit;
}

.player-detail-result {
  display: inline-flex;
  align-items: baseline;
  gap: 10px;
  color: inherit;

  em {
    font-style: normal;
    font-size: 12px;
    font-weight: 800;
  }

  strong {
    font-size: 15px;
    font-weight: 800;
    color: #171717;
  }

  &.is-win em {
    color: var(--cnpa-red, #e10613);
  }

  &.is-loss em {
    color: #666;
  }
}

.player-detail-score {
  display: inline-flex;
  align-items: baseline;
  gap: 5px;

  b {
    font-weight: 800;
    color: #171717;
  }

  i {
    font-style: normal;
    font-weight: 700;
    color: #999;
  }

  .is-winner {
    color: var(--cnpa-red, #e10613);
  }
}

.player-detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  align-items: center;
  gap: 7px;
}

.player-detail-action {
  flex: 0 0 auto;
  height: 28px;
  padding: 0 9px;
  border: 1px solid #c8c4bf;
  border-radius: 2px;
  background: #fff;
  color: #222;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition: color 150ms ease, border-color 150ms ease, background-color 150ms ease;

  &:hover {
    border-color: var(--cnpa-red, #e10613);
    color: var(--cnpa-red, #e10613);
    background: rgba(225, 6, 19, 0.04);
  }

  &--series {
    border-color: rgba(225, 6, 19, 0.28);
  }
}

</style>
