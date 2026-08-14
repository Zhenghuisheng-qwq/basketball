<template>
  <div class="app-container">
    <el-alert title="赛事工作台：查看当前赛事运营进度，并进入规则、结构、排期或发布。" type="info" :closable="false" show-icon class="mb8" />
    <DivisionEventSelector v-if="!divisionId" entry="overview" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <DivisionWorkspaceNav />
      <WorkflowBlockedState
        v-if="blocked"
        :title="blocked"
        detail="请先到赛事规则完成并锁定后再继续。"
        action-text="去配置赛事规则"
        @action="goPublish"
      />
      <div v-else v-loading="loading">
        <div class="mb8">
          <el-button type="primary" @click="goPublish">去配置规则</el-button>
          <el-button @click="goRoster">去整理名单</el-button>
        </div>
        <el-alert
          v-if="overview?.roster"
          class="mb8"
          type="info"
          :closable="false"
          :title="rosterHint"
        />
        <el-empty v-if="!(overview?.stages || []).length" description="当前赛事还没有比赛阶段" />
        <StageStatusCard
          v-for="stage in overview?.stages || []"
          :key="stage.stageId"
          :stage="stage"
          @goto="target => goStage(target)"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Overview">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import DivisionWorkspaceNav from '../components/DivisionWorkspaceNav.vue'
import WorkflowBlockedState from '../components/WorkflowBlockedState.vue'
import StageStatusCard from '../components/StageStatusCard.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'

const router = useRouter()
const { divisionId, overview, loading, blocked, selectDivision, clearDivision } = useDivisionWorkspace('overview')

const rosterHint = computed(() => {
  const roster = overview.value?.roster
  if (!roster) return '参赛名单：尚未整理'
  const version = roster.currentRosterVersionNo != null ? ` V${roster.currentRosterVersionNo}` : ''
  return `参赛名单：${roster.rosterStatusName || roster.rosterStatus || '-'}${version}`
})

function goPublish() {
  router.push({ path: '/event/event-v4-publish', query: divisionId.value ? { divisionId: String(divisionId.value) } : {} })
}

function goRoster() {
  router.push({ path: '/event/event-v4-registration-audit', query: divisionId.value ? { divisionId: String(divisionId.value) } : {} })
}

function goStage(target: 'structure' | 'schedule' | 'publish-preview') {
  const map: Record<string, string> = {
    structure: '/event/event-v4-structure',
    schedule: '/event/event-v4-schedule',
    'publish-preview': '/event/event-v4-publish-preview'
  }
  router.push({ path: map[target], query: { divisionId: String(divisionId.value) } })
}
</script>

