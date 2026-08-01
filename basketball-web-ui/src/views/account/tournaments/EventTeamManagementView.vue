<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import EventTeamSummaryCard from '@/components/account/tournament/EventTeamSummaryCard.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const PERMISSION_HINT = '正式阵容成员可查看本页面，球队管理操作仅队长可用。'

const tournament = computed(() => eventTeamStore.tournament)
const eventTeam = computed(() => eventTeamStore.eventTeam)
const pendingTasks = computed(() => eventTeamStore.pendingTasks)
const isCaptain = computed(() => eventTeamStore.isCaptain)

const viewOperations = computed(() => [
  {
    key: 'roster',
    title: '当前阵容',
    desc: `当前阵容 ${eventTeamStore.rosterCount} / ${eventTeam.value.rosterMax}`,
    action: '查看阵容',
    path: teamPath('/roster'),
  },
  {
    key: 'history',
    title: '变更历史',
    desc: '查看阵容变更记录',
    action: '查看历史',
    path: teamPath('/roster-history'),
  },
  {
    key: 'match',
    title: '比赛列表',
    desc: isCaptain.value
      ? `${pendingTasks.value.matchDataTodoCount} 场待提交`
      : '查看本队全部比赛',
    action: '查看比赛',
    path: teamPath('/match-data'),
  },
])

const manageOperations = computed(() => [
  {
    key: 'roster-manage',
    title: '阵容管理',
    desc: `当前阵容 ${eventTeamStore.rosterCount} / ${eventTeam.value.rosterMax}`,
    action: '进入阵容管理',
    path: teamPath('/roster'),
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

const operations = computed(() => (isCaptain.value ? manageOperations.value : viewOperations.value))

function go(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="正式赛事球队详情"
      en-title="EVENT TEAM DETAIL"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队详情' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="tournament.tournamentName"
      :team-code="eventTeam.teamCode"
      :description="PERMISSION_HINT"
      :status-text="`${tournament.phaseName}进行中 · ${isCaptain ? '赛事队长' : '正式阵容成员'}`"
      status-type="success"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="赛事与球队">
        <EventTeamSummaryCard :event-team="eventTeam" :roster-count="eventTeamStore.rosterCount" />
      </AccountPanel>

      <AccountPanel v-if="isCaptain" title="待处理事项">
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

      <AccountPanel v-else title="我的权限">
        <p class="event-team-muted">{{ PERMISSION_HINT }}</p>
        <ul class="event-team-todo-list">
          <li>可查看本届赛事球队详情与当前阵容</li>
          <li>可查看队友基础信息与比赛记录</li>
          <li>管理操作需由赛事队长执行</li>
        </ul>
      </AccountPanel>
    </div>

    <AccountPanel :title="isCaptain ? '球队管理' : '球队信息'">
      <div class="event-team-ops-grid">
        <div v-for="item in operations" :key="item.key" class="event-team-op-card">
          <strong>{{ item.title }}</strong>
          <p>{{ item.desc }}</p>
          <button type="button" class="account-btn-primary" @click="go(item.path)">
            {{ item.action }}
          </button>
        </div>
      </div>
      <div v-if="isCaptain" class="account-panel__action">
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
