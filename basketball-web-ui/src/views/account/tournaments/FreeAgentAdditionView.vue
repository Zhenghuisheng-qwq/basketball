<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage({ requireCaptain: true })

const playerId = ref('')
const targetRosterRole = ref('MEMBER')
const applyReason = ref('')
const submitting = ref(false)

const inviteDisabled = computed(
  () =>
    eventTeamStore.rosterRemaining <= 0
    || eventTeamStore.freeAgentRemaining <= 0
    || !eventTeamStore.tournament.transferWindowOpen,
)

async function handleSubmit() {
  if (!playerId.value.trim()) {
    ElMessage.warning('请输入待补球员 ID')
    return
  }
  submitting.value = true
  try {
    const ok = await eventTeamStore.submitFreeAgentApplication({
      playerId: playerId.value.trim(),
      targetRosterRole: targetRosterRole.value,
      applyReason: applyReason.value.trim() || undefined,
    })
    if (ok) {
      ElMessage.success('自由球员补员申请已提交')
    } else {
      ElMessage.warning('当前无法提交补员申请')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="自由球员补充"
      en-title="FREE AGENT ADDITION"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队管理', to: teamPath() }, { label: '自由球员补充' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      :description="`${eventTeamStore.tournament.phaseName} · 自由球员补充`"
      :status-text="eventTeamStore.tournament.transferWindowOpen ? `窗口开放 · 截止 ${eventTeamStore.tournament.transferWindowEndTime}` : '窗口关闭'"
      status-type="warning"
    />

    <div class="event-team-stats-bar">
      <span>本届已补充 {{ eventTeamStore.tournament.freeAgentUsed }} / {{ eventTeamStore.tournament.freeAgentQuota }}</span>
      <span>当前阵容 {{ eventTeamStore.rosterCount }} / {{ eventTeamStore.eventTeam.rosterMax }}</span>
      <span>剩余名额 {{ eventTeamStore.rosterRemaining }}</span>
    </div>

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="提交补员申请">
        <div class="free-agent-form">
          <label>
            <span>球员 ID</span>
            <input v-model="playerId" type="text" class="identity-apply-form__input" :disabled="inviteDisabled" />
          </label>
          <label>
            <span>目标身份</span>
            <select v-model="targetRosterRole" class="identity-apply-form__input" :disabled="inviteDisabled">
              <option value="MEMBER">队员</option>
              <option value="STARTER">首发</option>
              <option value="SUBSTITUTE">替补</option>
            </select>
          </label>
          <label>
            <span>申请原因</span>
            <textarea v-model="applyReason" rows="3" class="identity-apply-form__input" :disabled="inviteDisabled" />
          </label>
        </div>
        <div class="account-panel__action">
          <button
            type="button"
            class="account-btn-primary"
            :disabled="inviteDisabled || submitting"
            @click="handleSubmit"
          >
            提交补员申请
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="最近申请">
        <div v-if="eventTeamStore.freeAgentInvite.inviteId" class="free-agent-invite">
          <strong>{{ eventTeamStore.freeAgentInvite.playerName }}</strong>
          <p>位置：{{ eventTeamStore.freeAgentInvite.position }}</p>
          <p>{{ eventTeamStore.freeAgentInvite.statusText }}</p>
        </div>
        <p v-else class="event-team-muted">暂无进行中的补员申请</p>
      </AccountPanel>
    </div>

    <AccountPanel title="自由球员补充规则">
      <ul class="event-team-rules">
        <li>仅在转会窗口开放时可提交补员申请。</li>
        <li>每支球队每届有自由球员补充配额限制。</li>
        <li>提交后需等待管理员审核。</li>
      </ul>
      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/roster'))">
          返回阵容管理
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.free-agent-form {
  display: grid;
  gap: 12px;

  label {
    display: grid;
    gap: 6px;

    span {
      font-size: 13px;
      font-weight: 700;
    }
  }
}

.free-agent-invite {
  display: flex;
  flex-direction: column;
  gap: 10px;

  strong {
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #666;
  }
}
</style>
