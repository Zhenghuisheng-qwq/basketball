<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const history = computed(() => eventTeamStore.rosterHistory)

onMounted(async () => {
  if (eventTeamStore.seasonId) {
    await eventTeamStore.loadRosterHistory(eventTeamStore.seasonId)
  }
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="阵容变更历史"
      en-title="ROSTER CHANGE HISTORY"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队详情', to: teamPath() }, { label: '变更历史' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      description="正式阵容成员可查看本页面，球队管理操作仅队长可用。"
      status-text="历史记录"
      status-type="info"
    />

    <AccountPanel title="变更记录">
      <p v-if="eventTeamStore.loading" class="event-team-muted">加载中…</p>
      <p v-else-if="!history.length" class="event-team-muted">暂无阵容变更记录</p>
      <div v-else class="platform-table-wrap">
        <table class="platform-table">
          <thead>
            <tr>
              <th>类型</th>
              <th>球员</th>
              <th>变更前</th>
              <th>变更后</th>
              <th>操作人</th>
              <th>生效时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in history" :key="item.recordId">
              <td>{{ item.changeTypeText }}</td>
              <td>{{ item.playerId }}</td>
              <td>{{ item.beforeSnapshot }}</td>
              <td>{{ item.afterSnapshot }}</td>
              <td>{{ item.operatorName }}</td>
              <td>{{ item.effectiveTime }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/roster'))">
          返回当前阵容
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
