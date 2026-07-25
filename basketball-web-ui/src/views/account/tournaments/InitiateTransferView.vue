<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import EligibilityChecklist from '@/components/account/tournament/EligibilityChecklist.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const sourceEventTeamId = ref('')
const applyReason = ref('')
const submitting = ref(false)

async function submitTransfer() {
  if (!sourceEventTeamId.value.trim()) {
    ElMessage.warning('请输入原球队赛事 ID')
    return
  }
  if (!eventTeamStore.canSubmitTransfer) {
    ElMessage.warning('当前不满足转会提交条件')
    return
  }
  submitting.value = true
  try {
    const ok = await eventTeamStore.submitTransferApplication({
      sourceEventTeamId: sourceEventTeamId.value.trim(),
      applyReason: applyReason.value.trim() || undefined,
    })
    if (ok) {
      ElMessage.success('转会申请已提交')
      router.push(teamPath('/transfers'))
    } else {
      ElMessage.warning('提交失败')
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
      zh-title="发起转会"
      en-title="INITIATE TRANSFER"
      :breadcrumb-items="[...breadcrumbRoot, { label: '转会管理', to: teamPath('/transfers') }, { label: '发起转会' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      :description="`${eventTeamStore.eventTeam.platform} · 目标球队 ${eventTeamStore.eventTeam.teamCode}`"
      :status-text="eventTeamStore.tournament.transferWindowOpen ? `窗口开放 · 截止 ${eventTeamStore.tournament.transferWindowEndTime}` : '窗口关闭'"
      status-type="success"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="转会信息">
        <div class="transfer-form">
          <label>
            <span>原球队赛事 ID</span>
            <input v-model="sourceEventTeamId" type="text" class="identity-apply-form__input" placeholder="sourceEventTeamId" />
          </label>
          <label>
            <span>目标球队赛事 ID</span>
            <input :value="eventTeamStore.eventTeamId" type="text" class="identity-apply-form__input" disabled />
          </label>
          <label>
            <span>申请原因</span>
            <textarea v-model="applyReason" rows="3" class="identity-apply-form__input" />
          </label>
        </div>
      </AccountPanel>

      <AccountPanel title="资格检查">
        <EligibilityChecklist :checks="eventTeamStore.transferEligibilityChecks" />
        <p class="event-team-muted">当前阵容 {{ eventTeamStore.rosterCount }} / {{ eventTeamStore.eventTeam.rosterMax }}</p>
      </AccountPanel>
    </div>

    <AccountPanel title="提交确认">
      <div class="profile-version__actions">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/transfers'))">
          返回转会管理
        </button>
        <button
          type="button"
          class="account-btn-primary"
          :disabled="!eventTeamStore.canSubmitTransfer || submitting"
          @click="submitTransfer"
        >
          提交转会申请
        </button>
      </div>
      <p class="event-team-muted">提交后需等待相关方及管理员审核。</p>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.transfer-form {
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

.profile-version__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
