<script setup lang="ts">
import { computed } from 'vue'
import PlayerLink from '@/components/player/PlayerLink.vue'
import { getAllPlayerProfiles } from '@/views/player/detail/mock/playerDetail'
import type { Match } from '../types/events'

defineProps<{
  visible: boolean
  match: Match | null
}>()

const emit = defineEmits<{
  close: []
}>()

const mockPlayers = computed(() =>
  getAllPlayerProfiles().slice(0, 5).map((player, index) => ({
    playerId: player.id,
    playerName: player.displayName,
    points: 18 + index * 2,
    rebounds: 4 + index,
    assists: 7 + index,
  })),
)
</script>

<template>
  <div v-if="visible && match" class="modal-mask" @click.self="emit('close')">
    <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="stats-title">
      <button type="button" class="modal-close" aria-label="关闭" @click="emit('close')">×</button>
      <h3 id="stats-title">技术统计</h3>
      <p class="modal-meta">比赛 ID：{{ match.id }}</p>

      <div class="stats-table-wrap">
        <table class="stats-table">
          <thead>
            <tr>
              <th>球员</th>
              <th>得分</th>
              <th>篮板</th>
              <th>助攻</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="player in mockPlayers" :key="player.playerId">
              <td>
                <PlayerLink
                  :player-id="player.playerId"
                  :player-name="player.playerName"
                  tab="matches"
                />
              </td>
              <td>{{ player.points }}</td>
              <td>{{ player.rebounds }}</td>
              <td>{{ player.assists }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="modal-tip">Mock 技术统计，点击球员名称进入球员详情。</p>
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
  width: min(640px, 100%);
  padding: 28px;
  background: #111;
  border: 1px solid rgba(255, 255, 255, 0.12);
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
  margin: 20px 0;
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

.modal-tip {
  margin: 0;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
}
</style>
