<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useTeamStore } from '@/stores/team'
import {
  JOINABLE_PLATFORM_TABS,
  PLATFORM_IDENTITY_STATUS_LABELS,
  type JoinablePlatformCode,
  type PlatformIdentityStatusCode,
} from '@/api/types/teamJoinInvite'
import { GAME_PLATFORM_LABELS } from '@/types/account'
import { mapGameTerminalToPlatform } from '@/utils/accountMappers'

const props = defineProps<{
  initialPlatform?: JoinablePlatformCode
}>()

const teamStore = useTeamStore()

const activePlatform = ref<JoinablePlatformCode>(props.initialPlatform ?? 'PC')
const joinKeyword = ref('')
const applyReason = ref('')

const joinableContext = computed(() => teamStore.joinableContext)
const platformIdentityStatus = computed(
  () => (joinableContext.value?.platformIdentityStatus ?? 'UNBOUND') as PlatformIdentityStatusCode,
)
const platformIdentityLabel = computed(
  () => PLATFORM_IDENTITY_STATUS_LABELS[platformIdentityStatus.value] ?? '未知',
)
const pageDisabledReason = computed(
  () => joinableContext.value?.disabledReason || joinableContext.value?.emptyReason || '',
)

function platformTabLabel(code: JoinablePlatformCode) {
  return GAME_PLATFORM_LABELS[mapGameTerminalToPlatform(code)] ?? code
}

function resolveApplyButtonText(team: { canApply?: boolean; disabledReason?: string; cannotApplyReason?: string }) {
  const reason = team.disabledReason || team.cannotApplyReason
  if (reason === '申请审核中') return '申请审核中'
  return '申请加入'
}

function isApplyDisabled(team: { canApply?: boolean }) {
  if (joinableContext.value?.hasActiveTeam) return true
  if (joinableContext.value?.canApply === false) return true
  return team.canApply === false
}

function applyTooltip(team: { disabledReason?: string; cannotApplyReason?: string }) {
  if (joinableContext.value?.disabledReason) return joinableContext.value.disabledReason
  return team.disabledReason || team.cannotApplyReason || undefined
}

async function loadCurrentPlatform() {
  await teamStore.loadJoinableTeams({
    platformCode: activePlatform.value,
    keyword: joinKeyword.value.trim() || undefined,
  })
}

async function switchPlatform(code: JoinablePlatformCode) {
  activePlatform.value = code
  await loadCurrentPlatform()
}

async function searchJoinable() {
  await loadCurrentPlatform()
}

async function applyJoin(teamId: string) {
  try {
    await teamStore.applyJoinTeamAction(teamId, applyReason.value.trim() || undefined)
    ElMessage.success('加入申请已提交')
    applyReason.value = ''
    await teamStore.loadMyJoinApplications()
    await loadCurrentPlatform()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '申请失败')
  }
}

watch(
  () => props.initialPlatform,
  (platform) => {
    if (platform && JOINABLE_PLATFORM_TABS.includes(platform)) {
      activePlatform.value = platform
    }
  },
)

defineExpose({ loadCurrentPlatform, activePlatform })
</script>

<template>
  <AccountPanel title="可加入球队">
    <nav class="team-platform-tabs" aria-label="游戏平台">
      <button
        v-for="code in JOINABLE_PLATFORM_TABS"
        :key="code"
        type="button"
        class="team-platform-tabs__item"
        :class="{ 'team-platform-tabs__item--active': activePlatform === code }"
        @click="switchPlatform(code)"
      >
        {{ platformTabLabel(code) }}
      </button>
    </nav>

    <div class="team-platform-status">
      <p>
        <strong>当前平台：</strong>{{ platformTabLabel(activePlatform) }}
        <span class="team-platform-status__badge">{{ platformIdentityLabel }}</span>
      </p>
      <p v-if="joinableContext?.hasActiveTeam">
        已在该平台加入球队：{{ joinableContext.currentTeamName || joinableContext.currentTeamId }}
      </p>
      <p v-else-if="pageDisabledReason" class="team-platform-status__hint">{{ pageDisabledReason }}</p>
    </div>

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
            <th>平台</th>
            <th>人数</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="team in teamStore.joinableTeams" :key="team.teamId">
            <td>{{ team.teamName }}</td>
            <td>{{ team.regionName || '-' }}</td>
            <td>{{ team.platformCode || team.gameTerminal || '-' }}</td>
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
      :message="teamStore.joinableEmptyReason || `当前暂无${platformTabLabel(activePlatform)}平台可加入的公开球队`"
    />

    <label class="team-inline-field">
      申请说明
      <textarea v-model="applyReason" rows="2" maxlength="200" placeholder="可选，向队长说明加入理由" />
    </label>
  </AccountPanel>
</template>

<style scoped>
.team-platform-tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.team-platform-tabs__item {
  border: 1px solid var(--account-border, #d9dee8);
  background: #fff;
  border-radius: 999px;
  padding: 0.35rem 0.9rem;
  cursor: pointer;
}

.team-platform-tabs__item--active {
  border-color: var(--account-primary, #1d4ed8);
  color: var(--account-primary, #1d4ed8);
  font-weight: 600;
}

.team-platform-status {
  margin-bottom: 1rem;
  color: var(--account-text-muted, #5f6b7a);
}

.team-platform-status__badge {
  display: inline-block;
  margin-left: 0.5rem;
  padding: 0.1rem 0.5rem;
  border-radius: 999px;
  background: #eef2ff;
  color: #1d4ed8;
  font-size: 0.85rem;
}

.team-platform-status__hint {
  color: #b45309;
}
</style>
