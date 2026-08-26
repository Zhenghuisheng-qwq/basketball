<script setup lang="ts">
import { ref, watch } from 'vue'
import PlayerLink from '@/components/player/PlayerLink.vue'
import { getV4PublicMatchDetail } from '@/api/v4Public'
import type { PublicMatchDetailVo, PublicMatchPlayerStatVo } from '@/api/types/public'
import { displayNumber, displayText, formatPercent } from '@/utils/publicDisplay'
import type { Match } from '../types/events'

const props = defineProps<{
  visible: boolean
  match: Match | null
}>()

const emit = defineEmits<{
  close: []
}>()

const detail = ref<PublicMatchDetailVo | null>(null)
const players = ref<PublicMatchPlayerStatVo[]>([])
const state = ref<'idle' | 'loading' | 'success' | 'empty' | 'error'>('idle')
const errorMessage = ref('')
let reqId = 0

async function loadStats(matchId: string) {
  const current = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getV4PublicMatchDetail(matchId)
    if (current !== reqId) return
    const raw = res.data
    if (!raw) {
      detail.value = null
      players.value = []
      state.value = 'error'
      errorMessage.value = '暂无技术统计'
      return
    }
    players.value = (raw.playerStats ?? []).map((item) => ({
      playerId: String(item.sourcePlayerId || '').trim(),
      playerName: item.playerName,
      teamId: item.eventTeamId,
      teamName: item.teamName,
      points: item.points,
      rebounds: item.rebound,
      assists: item.assist,
      steals: item.steal,
      blocks: item.block,
      turnovers: item.turnover,
      // 比赛球员统计正式表无投篮字段
      fieldGoalPct: null,
      threePointPct: null,
      freeThrowPct: null,
    })) as PublicMatchPlayerStatVo[]
    detail.value = {
      matchId: raw.matchId,
      scheduledStartTime: raw.matchTime,
      matchStatus: raw.matchStatus,
      homeTeamName: raw.homeTeam?.teamName,
      awayTeamName: raw.awayTeam?.teamName,
      homeScore: raw.homeScore,
      awayScore: raw.awayScore,
      venueName: raw.venueText,
      statsAvailable: players.value.length > 0,
      playerStats: players.value,
    } as PublicMatchDetailVo
    if (!players.value.length) {
      state.value = 'empty'
      return
    }
    state.value = 'success'
  } catch (err) {
    if (current !== reqId) return
    detail.value = null
    players.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载技术统计失败'
  }
}

watch(
  () => [props.visible, props.match?.matchId] as const,
  ([visible, matchId]) => {
    if (visible && matchId) {
      void loadStats(matchId)
    } else if (!visible) {
      detail.value = null
      players.value = []
      state.value = 'idle'
    }
  },
)
</script>

<template>
  <div v-if="visible && match" class="modal-mask" @click.self="emit('close')">
    <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="stats-title">
      <button type="button" class="modal-close" aria-label="关闭" @click="emit('close')">×</button>
      <h3 id="stats-title">技术统计</h3>
      <p class="modal-meta">
        {{ displayText(match.awayTeamName) }} vs {{ displayText(match.homeTeamName) }}
      </p>

      <div v-if="state === 'loading'" class="modal-state">加载中...</div>
      <div v-else-if="state === 'error'" class="modal-state is-error">{{ errorMessage }}</div>
      <div v-else-if="state === 'empty'" class="modal-state">暂无已确认的技术统计</div>
      <div v-else class="stats-table-wrap">
        <table class="stats-table">
          <thead>
            <tr>
              <th>球员</th>
              <th>球队</th>
              <th>得分</th>
              <th>篮板</th>
              <th>助攻</th>
              <th>投篮%</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="player in players" :key="`${player.playerId}-${player.teamId}`">
              <td>
                <PlayerLink
                  v-if="player.playerId"
                  :player-id="player.playerId"
                  :player-name="displayText(player.playerName)"
                  tab="matches"
                />
                <template v-else>{{ displayText(player.playerName) }}</template>
              </td>
              <td>{{ displayText(player.teamName) }}</td>
              <td>{{ displayNumber(player.points) }}</td>
              <td>{{ displayNumber(player.rebounds) }}</td>
              <td>{{ displayNumber(player.assists) }}</td>
              <td>{{ formatPercent(player.fieldGoalPct) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.72);
}

.modal-card {
  position: relative;
  width: min(760px, 100%);
  max-height: min(80vh, 720px);
  overflow: auto;
  padding: 28px;
  background: #111;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
}

.modal-meta {
  color: var(--cnpa-text-secondary);
  font-size: 13px;
}

.stats-table-wrap {
  margin: 20px 0 0;
  overflow-x: auto;
}

.stats-table {
  width: 100%;
  border-collapse: collapse;

  th,
  td {
    padding: 10px 12px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    text-align: left;
    font-size: 14px;
  }

  th {
    color: rgba(255, 255, 255, 0.72);
    font-weight: 500;
  }
}

.modal-state {
  padding: 28px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);

  &.is-error {
    color: #e10613;
  }
}
</style>
