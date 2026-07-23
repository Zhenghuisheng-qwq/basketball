<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import EligibilityChecklist from '@/components/account/tournament/EligibilityChecklist.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const player = computed(() => eventTeamStore.mockInitiateTransferPlayer)

const targetAfter = computed(() => eventTeamStore.rosterCount + 1)
const sourceAfter = computed(() => player.value.fromTeamRosterCount - 1)

function submitTransfer() {
  if (!eventTeamStore.canSubmitTransfer) {
    ElMessage.warning('当前不满足转会提交条件')
    return
  }
  if (eventTeamStore.submitTransferApplication()) {
    ElMessage.success('转会申请已提交（Mock）')
    router.push(teamPath('/transfers/tf-001'))
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
      :description="`${eventTeamStore.eventTeam.platform} · 目标球队队长 ${eventTeamStore.eventTeam.captainName}`"
      :status-text="`窗口开放 · 截止 ${eventTeamStore.tournament.transferWindowEndTime}`"
      status-type="success"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="已选球员">
        <div class="transfer-player-card">
          <strong>{{ player.playerName }}</strong>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">位置</span><span>{{ player.position }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">赛事平台</span><span>{{ player.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ player.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">原球队</span><span>{{ player.fromTeamCode }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">目标球队</span><span>{{ player.toTeamCode }}</span></div>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="转会前后阵容">
        <div class="transfer-roster-compare">
          <div>
            <strong>原球队 {{ player.fromTeamCode }}</strong>
            <p>当前阵容 {{ player.fromTeamRosterCount }} / {{ eventTeamStore.eventTeam.rosterMax }}</p>
            <p>转出后 {{ sourceAfter }} / {{ eventTeamStore.eventTeam.rosterMax }}</p>
          </div>
          <span class="transfer-roster-compare__arrow" aria-hidden="true">→</span>
          <div>
            <strong>目标球队 {{ player.toTeamCode }}</strong>
            <p>当前阵容 {{ eventTeamStore.rosterCount }} / {{ eventTeamStore.eventTeam.rosterMax }}</p>
            <p>转入后 {{ targetAfter }} / {{ eventTeamStore.eventTeam.rosterMax }}</p>
          </div>
        </div>
      </AccountPanel>
    </div>

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="资格检查">
        <EligibilityChecklist :checks="eventTeamStore.transferEligibilityChecks" />
      </AccountPanel>

      <AccountPanel title="提交确认">
        <p>本次申请由目标球队队长发起</p>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">目标球队队长</span><span>{{ eventTeamStore.eventTeam.captainName }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">目标球队</span><span>{{ eventTeamStore.eventTeam.teamCode }}</span></div>
        </div>
        <div class="profile-version__actions">
          <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/transfers'))">
            返回转会管理
          </button>
          <button
            type="button"
            class="account-btn-primary"
            :disabled="!eventTeamStore.canSubmitTransfer"
            @click="submitTransfer"
          >
            提交转会申请
          </button>
        </div>
        <p class="event-team-muted">提交后依次等待球员本人、原球队队长及管理员确认。</p>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.transfer-roster-compare {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 12px;
  align-items: center;

  p {
    margin: 4px 0 0;
    font-size: 13px;
    color: #666;
  }

  &__arrow {
    font-size: 24px;
    color: var(--account-red, #d30910);
    font-weight: 900;
  }
}

@media (max-width: 767px) {
  .transfer-roster-compare {
    grid-template-columns: 1fr;
  }
}
</style>
