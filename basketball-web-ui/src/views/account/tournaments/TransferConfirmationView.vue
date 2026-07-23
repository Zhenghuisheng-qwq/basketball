<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import TransferProgress from '@/components/account/tournament/TransferProgress.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const transferId = computed(() => String(route.params.transferId || 'tf-001'))
const transfer = computed(() => eventTeamStore.getTransfer(transferId.value))

const stepperSteps = computed(() => [
  { label: '目标队长申请', state: 'done' as const },
  { label: '球员本人确认', state: 'done' as const },
  { label: '原队长确认', state: 'current' as const, subLabel: '当前' },
  { label: '管理员审核', state: 'upcoming' as const },
])

const progressList = computed(() => {
  if (!transfer.value) return []
  return transfer.value.steps.map((step) => ({
    label: step.text,
    status: step.status === 'done' ? `已完成 ${step.time || ''}`.trim() : step.status === 'current' ? '待确认' : '尚未开始',
  }))
})
</script>

<template>
  <div v-if="transfer" class="account-view">
    <AccountPageHeader
      zh-title="转会确认"
      en-title="TRANSFER CONFIRMATION"
      :breadcrumb-items="[...breadcrumbRoot, { label: '转会管理', to: teamPath('/transfers') }, { label: '转会确认' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      team-code="转会"
      :description="`${transfer.playerName}：${transfer.fromTeamCode} → ${transfer.toTeamCode}`"
      status-text="确认中"
      status-type="warning"
    />

    <AccountStepper :steps="stepperSteps" />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="转会申请">
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">球员</span><span>{{ transfer.playerName }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">位置</span><span>{{ transfer.position }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">比赛平台</span><span>{{ transfer.platform }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ transfer.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">原球队</span><span>{{ transfer.fromTeamCode }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">目标球队</span><span>{{ transfer.toTeamCode }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">目标队长</span><span>{{ transfer.targetCaptainName }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">申请时间</span><span>{{ transfer.applyTime || '-' }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="确认进度">
        <TransferProgress :steps="transfer.steps" />
        <ul class="event-team-progress-list">
          <li v-for="item in progressList" :key="item.label">
            <strong>{{ item.label }}</strong>
            <span>{{ item.status }}</span>
          </li>
        </ul>
      </AccountPanel>
    </div>

    <AccountPanel title="数据归属说明">
      <p class="event-team-muted">
        管理员审核通过前，{{ transfer.playerName }} 仍属于 {{ transfer.fromTeamCode }}；通过时原关系关闭并激活 {{ transfer.toTeamCode }} 关系。
        转会前数据归原球队，转会后数据归新球队。
      </p>
      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/transfers'))">
          返回转会管理
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.event-team-progress-list {
  list-style: none;
  margin: 12px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;

  li {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;
  }

  span {
    color: #666;
  }
}
</style>
