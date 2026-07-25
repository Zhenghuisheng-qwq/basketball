<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import EventTeamSummaryCard from '@/components/account/tournament/EventTeamSummaryCard.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const tournament = computed(() => eventTeamStore.tournament)
const eventTeam = computed(() => eventTeamStore.eventTeam)
const pendingTasks = computed(() => eventTeamStore.pendingTasks)

const operations = computed(() => [
  {
    key: 'history',
    title: '变更历史',
    desc: '查看阵容变更记录',
    action: '查看历史',
    path: teamPath('/roster-history'),
  },
  {
    key: 'roster',
    title: '阵容管理',
    desc: `当前阵容 ${eventTeamStore.rosterCount} / ${eventTeam.value.rosterMax}`,
    action: '进入阵容管理',
    path: teamPath('/roster'),
  },
  {
    key: 'match',
    title: '比赛数据',
    desc: `${pendingTasks.value.matchDataTodoCount} 场待提交`,
    action: '查看比赛任务',
    path: '',
    mockOnly: true,
  },
  {
    key: 'transfer',
    title: '转会管理',
    desc: tournament.value.transferWindowOpen
      ? `转会窗口开放 · 截止 ${tournament.value.transferWindowEndTime}`
      : '转会窗口已关闭',
    action: '进入转会管理',
    path: teamPath('/transfers'),
  },
  {
    key: 'captain',
    title: '队长移交',
    desc: `当前队长 ${eventTeam.value.captainName}`,
    action: '发起队长移交',
    path: teamPath('/captain-transfer'),
  },
])

function go(path: string, mockOnly?: boolean) {
  if (mockOnly) {
    ElMessage.info('比赛数据任务将在后续版本开放')
    return
  }
  router.push(path)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="本届球队管理"
      en-title="EVENT TEAM MANAGEMENT"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队管理' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="tournament.tournamentName"
      :team-code="eventTeam.teamCode"
      description="队长权限仅对本届赛事有效"
      :status-text="`${tournament.phaseName}进行中`"
      status-type="success"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="赛事与球队">
        <EventTeamSummaryCard :event-team="eventTeam" :roster-count="eventTeamStore.rosterCount" />
      </AccountPanel>

      <AccountPanel title="待处理事项">
        <ul class="event-team-todo-list">
          <li>{{ pendingTasks.transferConfirmCount }} 项转会确认</li>
          <li>{{ pendingTasks.matchDataTodoCount }} 场比赛数据待提交</li>
        </ul>
        <p class="event-team-muted">请在对应管理入口处理</p>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="go(teamPath('/transfers'))">
            前往转会管理
          </button>
        </div>
      </AccountPanel>
    </div>

    <AccountPanel title="本届球队操作">
      <div class="event-team-ops-grid">
        <div v-for="item in operations" :key="item.key" class="event-team-op-card">
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
          <button type="button" class="account-btn-primary" @click="go(item.path, item.mockOnly)">
            {{ item.action }}
          </button>
        </div>
      </div>
      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="go(teamPath('/profile/edit'))">
          修改本届球队资料
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
