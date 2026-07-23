<script setup lang="ts">
import { ref } from 'vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { FreeAgent } from '@/types/eventTeam'

const props = defineProps<{
  disabled?: boolean
  searchFn: (query: string) => FreeAgent[]
}>()

const emit = defineEmits<{
  invite: [playerId: string]
}>()

const query = ref('')
const results = ref<FreeAgent[]>([])

function handleSearch() {
  results.value = props.searchFn(query.value)
}

function handleInvite(playerId: string) {
  emit('invite', playerId)
}
</script>

<template>
  <div class="free-agent-search">
    <label class="free-agent-search__label">搜索自由球员</label>
    <div class="free-agent-search__bar">
      <input
        v-model="query"
        type="text"
        class="identity-apply-form__input"
        placeholder="输入永久球员 ID 或游戏 ID"
        @keyup.enter="handleSearch"
      />
      <button type="button" class="account-btn-secondary" @click="handleSearch">搜索</button>
    </div>

    <div v-if="results.length" class="free-agent-search__results">
      <div v-for="agent in results" :key="agent.playerId" class="free-agent-search__item">
        <div>
          <strong>{{ agent.nickname }}</strong>
          <p>位置：{{ agent.position }} · 平台：{{ agent.platform }} · 游戏 ID：{{ agent.gameId }}</p>
          <AccountStatusBadge type="default" :text="agent.statusText" />
        </div>
        <button
          type="button"
          class="account-btn-primary"
          :disabled="disabled"
          @click="handleInvite(agent.playerId)"
        >
          发送邀请
        </button>
      </div>
    </div>
    <p v-else-if="query" class="free-agent-search__empty">未找到符合条件的本届自由球员</p>
  </div>
</template>

<style scoped lang="scss">
.free-agent-search {
  display: flex;
  flex-direction: column;
  gap: 14px;

  &__label {
    font-weight: 800;
  }

  &__bar {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;

    input {
      flex: 1;
      min-width: 200px;
    }
  }

  &__results {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__item {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 12px;
    padding: 14px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    background: rgba(0, 0, 0, 0.02);

    p {
      margin: 6px 0;
      font-size: 13px;
      color: #666;
    }
  }

  &__empty {
    margin: 0;
    font-size: 13px;
    color: #666;
  }
}
</style>
