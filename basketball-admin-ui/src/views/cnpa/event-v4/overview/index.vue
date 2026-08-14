<template>
  <div class="app-container">
    <el-alert title="赛程管理概览：查看当前赛事各赛段进度，不在此页生成或改时间" type="info" :closable="false" show-icon class="mb8" />
    <DivisionEventSelector v-if="!divisionId" entry="overview" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <WorkflowBlockedState
        v-if="blocked"
        :title="blocked"
        detail="请先到发布赛事完成并锁定赛事规则。"
        action-text="去发布赛事规则"
        @action="goPublish"
      />
      <div v-else v-loading="loading">
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
import { useRouter } from 'vue-router'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import WorkflowBlockedState from '../components/WorkflowBlockedState.vue'
import StageStatusCard from '../components/StageStatusCard.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'

const router = useRouter()
const { divisionId, overview, loading, blocked, selectDivision, clearDivision } = useDivisionWorkspace('overview')

function goPublish() {
  router.push({ path: '/event/event-v4-publish', query: divisionId.value ? { divisionId: String(divisionId.value) } : {} })
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

