<script setup lang="ts">
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import TransferCard from '@/components/account/tournament/TransferCard.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage({ requireCaptain: true })

function viewTransfer(transferId: string) {
  router.push(teamPath(`/transfers/${transferId}`))
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="转会管理"
      en-title="TRANSFER MANAGEMENT"
      :breadcrumb-items="[...breadcrumbRoot, { label: '转会管理' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      :description="`${eventTeamStore.eventTeam.platform} · 当前队长 ${eventTeamStore.eventTeam.captainName} · 转会窗口截止 ${eventTeamStore.tournament.transferWindowEndTime}`"
      status-text="窗口开放"
      status-type="success"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel :title="`转入申请 ${eventTeamStore.incomingTransfers.length}`">
        <TransferCard
          v-for="item in eventTeamStore.incomingTransfers"
          :key="item.transferId"
          :transfer="item"
          direction-label="转入"
          @view="viewTransfer"
        />
        <p v-if="!eventTeamStore.incomingTransfers.length" class="event-team-muted">暂无转入申请</p>
      </AccountPanel>

      <AccountPanel :title="`转出申请 ${eventTeamStore.outgoingTransfers.length}`">
        <TransferCard
          v-for="item in eventTeamStore.outgoingTransfers"
          :key="item.transferId"
          :transfer="item"
          direction-label="转出"
          @view="viewTransfer"
        />
        <p v-if="!eventTeamStore.outgoingTransfers.length" class="event-team-muted">暂无转出申请</p>
      </AccountPanel>
    </div>

    <AccountPanel title="转会规则">
      <ul class="event-team-rules">
        <li>正式转会只能由目标球队队长发起。</li>
        <li>普通邀请不能绕过转会流程。</li>
      </ul>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-primary" @click="router.push(teamPath('/transfers/initiate'))">
          发起转会
        </button>
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath())">
          返回球队管理
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
