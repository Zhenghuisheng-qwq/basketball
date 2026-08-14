<template>
  <div class="app-container">
    <el-alert title="赛程发布：只检查并发布，不修改规则、名单或对阵" type="info" :closable="false" show-icon class="mb8" />
    <DivisionEventSelector v-if="!divisionId" entry="publish-preview" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <DivisionWorkspaceNav />
      <WorkflowBlockedState
        v-if="blocked"
        :title="blocked"
        detail="请先完成赛事规则发布后再进入发布中心。"
        action-text="去配置赛事规则"
        @action="goPublishRule"
      />
      <div v-else v-loading="loading">
        <el-tabs v-model="activeStageId" @tab-change="onStageChange">
          <el-tab-pane v-for="stage in overview?.stages || []" :key="stage.stageId" :label="stage.stageName" :name="String(stage.stageId)" />
        </el-tabs>
        <el-empty v-if="!currentStage" description="没有可发布的赛段" />
        <template v-else>
          <el-descriptions :column="3" border size="small" class="mb8">
            <el-descriptions-item label="赛事规则">{{ overview?.rule?.ruleStatusName }}</el-descriptions-item>
            <el-descriptions-item label="参赛名单">{{ overview?.roster?.rosterStatusName }} {{ overview?.roster?.currentRosterVersionNo != null ? 'V' + overview?.roster?.currentRosterVersionNo : '' }}</el-descriptions-item>
            <el-descriptions-item label="赛程结构">{{ currentStage.structureStatusName }} {{ currentStage.executionVersion != null ? 'V' + currentStage.executionVersion : '' }}</el-descriptions-item>
            <el-descriptions-item label="抽签/对阵">{{ currentStage.pairingStatusName }}</el-descriptions-item>
            <el-descriptions-item label="排期">{{ currentStage.scheduledUnits ?? 0 }} / {{ currentStage.totalUnits ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="占位">{{ currentStage.resolvedPlaceholders ?? 0 }} 已确定 / {{ currentStage.unresolvedPlaceholders ?? 0 }} 待确定</el-descriptions-item>
            <el-descriptions-item label="发布状态">{{ currentStage.publishStatusName }}</el-descriptions-item>
            <el-descriptions-item label="发布版本">{{ currentStage.publishNo ? '#' + currentStage.publishNo : '-' }}</el-descriptions-item>
            <el-descriptions-item label="排期修订">{{ currentStage.scheduleRevision != null ? 'R' + currentStage.scheduleRevision : '-' }}</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="currentStage.structureStatus === 'STALE'" type="error" :closable="false" class="mb8" title="当前赛程结构已因参赛名单变化失效，请重新生成。" />
          <el-alert v-if="currentStage.publishStatus === 'WITHDRAWN'" type="warning" :closable="false" class="mb8" title="结构不变，排期修订已变化。调整时间后可直接重新发布，无需重新生成结构。" />
          <div class="mb8">
            <el-button type="warning" @click="loadPreview">预览检查</el-button>
            <el-button type="success" :disabled="!preview?.canPublish || currentStage.publishStatus === 'PUBLISHED'" @click="doPublish">
              {{ currentStage.publishStatus === 'WITHDRAWN' ? '重新发布' : '检查并发布' }}
            </el-button>
            <el-button type="danger" v-if="currentStage.publishStatus === 'PUBLISHED'" @click="openWithdraw">撤回发布</el-button>
          </div>
          <PublishValidationPanel :preview="preview" />
          <div class="mt8">
            <div class="records-title">发布记录</div>
            <el-table :data="records" border size="small">
              <el-table-column label="操作" prop="actionType" width="120" />
              <el-table-column label="Publish No" prop="publishNo" width="110" />
              <el-table-column label="排期修订" prop="scheduleRevision" width="100" />
              <el-table-column label="状态" prop="publishStatus" width="110" />
              <el-table-column label="操作人" prop="operatorId" width="100" />
              <el-table-column label="时间" prop="operationTime" min-width="160" />
              <el-table-column label="原因" prop="reason" min-width="160" />
            </el-table>
            <el-empty v-if="!records.length" description="尚无发布/撤回记录" :image-size="60" />
          </div>
        </template>
      </div>
      <el-dialog v-model="withdrawVisible" title="撤回发布" width="480px">
        <el-form label-width="90px">
          <el-form-item label="原因" required>
            <el-input v-model="withdrawReason" type="textarea" rows="3" placeholder="必填" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="withdrawVisible = false">取消</el-button>
          <el-button type="danger" @click="doWithdraw">确认撤回</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4PublishPreview">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import DivisionWorkspaceNav from '../components/DivisionWorkspaceNav.vue'
import WorkflowBlockedState from '../components/WorkflowBlockedState.vue'
import PublishValidationPanel from '../components/PublishValidationPanel.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'
import {
  listV4StagePublishRecords,
  previewV4StagePublish,
  publishV4StageSchedule,
  republishV4StageSchedule,
  withdrawV4StageSchedule,
  type StagePublishPreviewVo,
  type StagePublishRecordVo
} from '@/api/cnpa/event-v4/stage-execution'

const router = useRouter()
const { divisionId, overview, loading, blocked, selectDivision, clearDivision, loadOverview } = useDivisionWorkspace('publish-preview')
const activeStageId = ref('')
const preview = ref<StagePublishPreviewVo>()
const records = ref<StagePublishRecordVo[]>([])
const withdrawVisible = ref(false)
const withdrawReason = ref('')

const currentStage = computed(() => (overview.value?.stages || []).find(s => String(s.stageId) === activeStageId.value))

watch(() => overview.value?.stages, (stages) => {
  if (stages?.length && !stages.some(s => String(s.stageId) === activeStageId.value)) {
    activeStageId.value = String(stages[0].stageId)
    loadPreview()
    loadRecords()
  }
}, { immediate: true })

function onStageChange() {
  preview.value = undefined
  loadPreview()
  loadRecords()
}

function loadPreview() {
  const id = Number(activeStageId.value)
  if (!id) return
  previewV4StagePublish(id).then(res => { preview.value = res.data })
}

function loadRecords() {
  const id = Number(activeStageId.value)
  if (!id) return
  listV4StagePublishRecords(id).then(res => { records.value = res.data || [] })
}

function doPublish() {
  const id = Number(activeStageId.value)
  if (!id) return
  const action = currentStage.value?.publishStatus === 'WITHDRAWN' ? republishV4StageSchedule : publishV4StageSchedule
  ElMessageBox.confirm('确认按当前排期发布该赛段？不会修改规则、名单或对阵。', '发布').then(() => {
    action(id).then(() => {
      ElMessage.success('发布成功')
      loadPreview()
      loadRecords()
      loadOverview()
    })
  })
}

function openWithdraw() {
  withdrawReason.value = ''
  withdrawVisible.value = true
}

function doWithdraw() {
  const id = Number(activeStageId.value)
  if (!id) return
  if (!withdrawReason.value.trim()) {
    ElMessage.error('撤回原因必填')
    return
  }
  withdrawV4StageSchedule(id, withdrawReason.value.trim()).then(() => {
    ElMessage.success('已撤回')
    withdrawVisible.value = false
    loadPreview()
    loadRecords()
    loadOverview()
  })
}

function goPublishRule() {
  router.push({ path: '/event/event-v4-publish', query: { divisionId: String(divisionId.value) } })
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.records-title { font-weight: 600; margin-bottom: 8px; }
</style>

