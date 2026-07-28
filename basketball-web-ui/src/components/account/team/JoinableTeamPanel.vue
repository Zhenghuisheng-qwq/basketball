<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useTeamStore } from '@/stores/team'

const teamStore = useTeamStore()

const joinKeyword = ref('')
const applyReason = ref('')

function resolveApplyButtonText(team: { canApply?: boolean; disabledReason?: string; cannotApplyReason?: string }) {
  const reason = team.disabledReason || team.cannotApplyReason
  if (reason === '申请审核中') return '申请审核中'
  return '申请加入'
}

function isApplyDisabled(team: { canApply?: boolean }) {
  if (teamStore.joinableContext?.hasActiveTeam) return true
  if (teamStore.joinableContext?.canApply === false) return true
  return team.canApply === false
}

function applyTooltip(team: { disabledReason?: string; cannotApplyReason?: string }) {
  if (teamStore.joinableContext?.disabledReason) {
    return teamStore.joinableContext.disabledReason
  }
  return team.disabledReason || team.cannotApplyReason || undefined
}

async function loadJoinableTeams() {
  await teamStore.loadJoinableTeams({
    keyword: joinKeyword.value.trim() || undefined,
  })
}

async function searchJoinable() {
  await loadJoinableTeams()
}

async function applyJoin(teamId: string) {
  try {
    await teamStore.applyJoinTeamAction(teamId, applyReason.value.trim() || undefined)
    ElMessage.success('加入申请已提交')
    applyReason.value = ''
    await teamStore.loadMyJoinApplications()
    await loadJoinableTeams()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '申请失败')
  }
}

defineExpose({ loadCurrentPlatform: loadJoinableTeams, loadJoinableTeams })
</script>

<template>
  <AccountPanel title="可加入球队">
    <p v-if="teamStore.joinableContext?.hasActiveTeam" class="team-joinable-hint">
      当前账号已加入球队，不能重复加入。
    </p>
    <p v-else-if="teamStore.joinableContext?.disabledReason" class="team-joinable-hint team-joinable-hint--warn">
      {{ teamStore.joinableContext.disabledReason }}
    </p>

    <div class="team-search-row">
      <input v-model="joinKeyword" type="search" placeholder="搜索球队名称" />
      <button type="button" class="account-btn-secondary" @click="searchJoinable">搜索</button>
    </div>

    <div v-if="teamStore.joinableTeams.length" class="team-table-wrap">
      <table class="team-table">
        <thead>
          <tr>
            <th>球队</th>
            <th>地区</th>
            <th>人数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="team in teamStore.joinableTeams" :key="team.teamId">
            <td>{{ team.teamName }}</td>
            <td>{{ team.regionName || '-' }}</td>
            <td>{{ team.currentMemberCount ?? 0 }}/{{ team.maxMemberCount ?? '-' }}</td>
            <td>
              <button
                type="button"
                class="account-btn-primary team-table__btn"
                :disabled="isApplyDisabled(team)"
                :title="applyTooltip(team)"
                @click="applyJoin(team.teamId)"
              >
                {{ resolveApplyButtonText(team) }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <AccountEmptyState
      v-else
      :message="teamStore.joinableEmptyReason || '当前暂无可加入的公开球队'"
    />

    <label class="team-inline-field">
      申请说明
      <textarea v-model="applyReason" rows="2" maxlength="200" placeholder="可选，向队长说明加入理由" />
    </label>
  </AccountPanel>
</template>

<style scoped>
.team-joinable-hint {
  margin: 0 0 1rem;
  color: var(--account-text-muted, #5f6b7a);
}

.team-joinable-hint--warn {
  color: #b45309;
}
</style>
