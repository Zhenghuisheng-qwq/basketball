<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import FreeAgentSearchPanel from '@/components/account/tournament/FreeAgentSearchPanel.vue'
import TransferProgress from '@/components/account/tournament/TransferProgress.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const inviteDisabled = computed(
  () =>
    eventTeamStore.rosterRemaining <= 0
    || eventTeamStore.freeAgentRemaining <= 0
    || !eventTeamStore.tournament.transferWindowOpen,
)

function handleInvite(playerId: string) {
  if (eventTeamStore.sendFreeAgentInvitation(playerId)) {
    ElMessage.success('邀请已发送，等待球员确认（Mock）')
  } else {
    ElMessage.warning('当前无法发送邀请')
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
      :status-text="`转会窗口开放 · 截止 ${eventTeamStore.tournament.transferWindowEndTime}`"
      status-type="warning"
    />

    <div class="event-team-stats-bar">
      <span>本届已补充 {{ eventTeamStore.tournament.freeAgentUsed }} / {{ eventTeamStore.tournament.freeAgentQuota }}</span>
      <span>当前阵容 {{ eventTeamStore.rosterCount }} / {{ eventTeamStore.eventTeam.rosterMax }}</span>
      <span>剩余名额 {{ eventTeamStore.rosterRemaining }}</span>
    </div>

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="搜索自由球员">
        <FreeAgentSearchPanel
          :disabled="inviteDisabled"
          :search-fn="eventTeamStore.searchFreeAgentPool"
          @invite="handleInvite"
        />
      </AccountPanel>

      <AccountPanel title="邀请状态">
        <div class="free-agent-invite">
          <strong>{{ eventTeamStore.freeAgentInvite.playerName }}</strong>
          <p>位置：{{ eventTeamStore.freeAgentInvite.position }}</p>
          <AccountStatusBadge type="warning" :text="eventTeamStore.freeAgentInvite.statusText" />
          <TransferProgress :steps="eventTeamStore.freeAgentInvite.steps" />
        </div>
      </AccountPanel>
    </div>

    <AccountPanel title="自由球员补充规则">
      <ul class="event-team-rules">
        <li>仅在常规赛或小组赛转会窗口开放。</li>
        <li>每支球队每届最多补充 2 名自由球员。</li>
        <li>非窗口管理员特殊操作仍计入 2 人限额。</li>
        <li>季后赛、淘汰赛和 Cup 锁定后不能补充。</li>
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
