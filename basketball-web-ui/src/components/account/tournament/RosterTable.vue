<script setup lang="ts">
import type { RosterMember } from '@/types/eventTeam'

defineProps<{
  members: RosterMember[]
  showActions?: boolean
}>()

const emit = defineEmits<{
  remove: [playerId: string]
}>()
</script>

<template>
  <div class="platform-table-wrap">
    <table class="platform-table roster-table">
      <thead>
        <tr>
          <th>球员</th>
          <th>平台和游戏 ID</th>
          <th>球队身份</th>
          <th>加入时间</th>
          <th>当前状态</th>
          <th v-if="showActions">允许的操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="member in members" :key="member.playerId">
          <td>
            <div class="roster-table__player">
              <div class="roster-table__avatar" aria-hidden="true">
                <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
              </div>
              <div>
                <strong>{{ member.nickname }}</strong>
                <span>{{ member.position }}</span>
              </div>
            </div>
          </td>
          <td>{{ member.platform }} / {{ member.gameId }}</td>
          <td>{{ member.roleText }}</td>
          <td>{{ member.joinTime }}</td>
          <td>{{ member.statusText }}</td>
          <td v-if="showActions">
            <button
              v-if="member.canRemove"
              type="button"
              class="account-btn-secondary account-btn-secondary--compact"
              @click="emit('remove', member.playerId)"
            >
              申请移除
            </button>
            <button
              v-else
              type="button"
              class="account-btn-secondary account-btn-secondary--compact"
              disabled
              :title="member.removeDisabledReason"
            >
              {{ member.removeDisabledReason || '不可操作' }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped lang="scss">
.roster-table__player {
  display: flex;
  align-items: center;
  gap: 10px;

  strong {
    display: block;
  }

  span {
    font-size: 12px;
    color: #666;
  }
}

.roster-table__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #ececec;
  display: grid;
  place-items: center;
  color: #777;
}
</style>
