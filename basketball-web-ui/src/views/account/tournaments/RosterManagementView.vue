<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import RosterTable from '@/components/account/tournament/RosterTable.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

function handleRemove(playerId: string) {
  if (eventTeamStore.requestRemoveMember(playerId)) {
    ElMessage.success('已提交移除申请（Mock）')
  } else {
    ElMessage.warning('当前不可移除该球员')
  }
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="阵容管理"
      en-title="ROSTER MANAGEMENT"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队管理', to: teamPath() }, { label: '阵容管理' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      description="管理本届赛事当前有效阵容"
      :status-text="eventTeamStore.tournament.transferWindowOpen ? '转会窗口开放' : '转会窗口关闭'"
      status-type="warning"
    />

    <AccountPanel title="当前阵容">
      <div class="roster-toolbar">
        <div class="roster-toolbar__stats">
          <span>当前阵容 {{ eventTeamStore.rosterCount }} / {{ eventTeamStore.eventTeam.rosterMax }}</span>
          <span>剩余 {{ eventTeamStore.rosterRemaining }} 人</span>
        </div>
        <button type="button" class="account-btn-primary" @click="router.push(teamPath('/free-agents'))">
          补充自由球员
        </button>
      </div>

      <RosterTable
        :members="eventTeamStore.roster"
        show-actions
        @remove="handleRemove"
      />

      <ul class="event-team-rules">
        <li>报名锁定后球员不能主动退出。</li>
        <li>转会窗口期队长可以申请移除。</li>
        <li>非窗口期只能由管理员处理。</li>
        <li>季后赛、淘汰赛和 Cup 锁定后不能补充空缺。</li>
      </ul>

      <div class="account-panel__action">
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
