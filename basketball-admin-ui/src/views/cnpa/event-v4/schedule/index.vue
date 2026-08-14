<template>
  <div class="app-container">
    <el-alert title="日期与时间排期：只处理什么时候打，不在此页改对阵或抽签" type="info" :closable="false" show-icon class="mb8" />
    <DivisionEventSelector v-if="!divisionId" entry="schedule" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <WorkflowBlockedState
        v-if="blocked"
        :title="blocked"
        detail="请先完成赛事规则发布后再排期。"
        action-text="去发布赛事规则"
        @action="goPublishRule"
      />
      <div v-else>
        <el-alert v-if="hasStaleStructure" type="error" :closable="false" class="mb8" title="当前赛程结构已因参赛名单变化失效，请重新生成。当前旧结构不可排期。" />
        <el-tabs v-model="activeStageId" @tab-change="onStageChange">
          <el-tab-pane v-for="stage in overview?.stages || []" :key="stage.stageId" :label="stage.stageName" :name="String(stage.stageId)" />
        </el-tabs>
        <el-empty v-if="!currentStage" description="没有可排期的赛段" />
        <StageScheduleBoard
          v-else
          embedded
          hide-publish
          :stage="boardStage"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Schedule">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import WorkflowBlockedState from '../components/WorkflowBlockedState.vue'
import StageScheduleBoard from './StageScheduleBoard.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'
import { listV4StageExecution, type BasketDivisionStageExecutionVo } from '@/api/cnpa/event-v4/stage-execution'

const router = useRouter()
const { divisionId, overview, blocked, selectDivision, clearDivision } = useDivisionWorkspace('schedule')
const activeStageId = ref('')
const stageRows = ref<BasketDivisionStageExecutionVo[]>([])
const hasStaleStructure = computed(() => (overview.value?.stages || []).some(s => s.structureStatus === 'STALE'))
const currentStage = computed(() => (overview.value?.stages || []).find(s => String(s.stageId) === activeStageId.value))
const boardStage = computed(() => stageRows.value.find(s => String(s.stageId) === activeStageId.value) || currentStage.value as BasketDivisionStageExecutionVo)

watch(() => overview.value?.stages, (stages) => {
  if (stages?.length && !stages.some(s => String(s.stageId) === activeStageId.value)) {
    activeStageId.value = String(stages[0].stageId)
  }
  if (divisionId.value) {
    listV4StageExecution(divisionId.value).then(res => { stageRows.value = res.data || [] })
  }
}, { immediate: true })

function onStageChange() {
  return
}

function goPublishRule() {
  router.push({ path: '/event/event-v4-publish', query: { divisionId: String(divisionId.value) } })
}
</script>

