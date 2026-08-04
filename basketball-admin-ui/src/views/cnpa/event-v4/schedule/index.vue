<template>
  <div class="app-container">
    <el-alert title="赛程管理：支持按 Division 阶段生成循环赛/小组赛/单败淘汰赛程，确认后发布" type="info" :closable="false" show-icon class="mb8" />

    <div class="schedule-stage-panel">
        <el-form :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="stageSeasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onStageSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="stageDivisionId" clearable filterable placeholder="选择赛区" style="width: 220px" :disabled="!stageSeasonId" @change="loadStageList">
              <el-option v-for="item in stageDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Refresh" @click="loadStageList">刷新阶段</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="stageLoading" :data="stageList" border>
          <el-table-column label="顺序" prop="stageOrder" width="60" align="center" />
          <el-table-column label="阶段" prop="stageName" min-width="100" />
          <el-table-column label="类型" width="90">
            <template #default="scope">{{ v4StageTypeLabel(scope.row.stageType) }}</template>
          </el-table-column>
          <el-table-column label="赛制" width="100">
            <template #default="scope">{{ v4CompetitionFormatLabel(scope.row.competitionFormat) }}</template>
          </el-table-column>
          <el-table-column label="执行状态" width="120">
            <template #default="scope">
              <el-tag :type="v4StageExecutionStatusTagType(scope.row.executionStatus)" size="small">
                {{ v4StageExecutionStatusLabel(scope.row.executionStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="参赛队" prop="participantCount" width="70" align="center" />
          <el-table-column label="操作" width="420" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleStagePreview(scope.row)">预览</el-button>
              <el-button link type="success" v-if="canGenerate(scope.row)" @click="handleStageGenerate(scope.row)">生成</el-button>
              <el-button link type="success" v-if="scope.row.executionStatus === 'SCHEDULE_GENERATED'" @click="handleStagePublish(scope.row)">发布</el-button>
              <el-button link type="warning" v-if="scope.row.executionStatus === 'SCHEDULE_GENERATED'" @click="handleStageCancel(scope.row)">作废</el-button>
              <el-button link type="primary" v-if="scope.row.executionStatus === 'RUNNING'" @click="handleCheckCompletion(scope.row)">检查完成</el-button>
              <el-button link type="primary" v-if="['RUNNING','RESULT_PENDING_CONFIRM'].includes(scope.row.executionStatus)" @click="handleQualPreview(scope.row)">晋级预览</el-button>
              <el-button link type="success" v-if="scope.row.executionStatus === 'RESULT_PENDING_CONFIRM'" @click="handleQualConfirm(scope.row)">确认晋级</el-button>
            </template>
          </el-table-column>
        </el-table>
    </div>

    <el-dialog v-model="stagePreviewVisible" title="赛程生成预览" width="520px">
      <el-descriptions v-if="stagePreview" :column="1" border size="small">
        <el-descriptions-item label="参赛队伍">{{ stagePreview.teamCount }}</el-descriptions-item>
        <el-descriptions-item label="每队场数">{{ stagePreview.matchesPerTeam ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="总比赛数">{{ stagePreview.totalMatches }}</el-descriptions-item>
        <el-descriptions-item label="轮次数">{{ stagePreview.roundCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="轮空">{{ stagePreview.hasBye ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-for="(w, i) in (stagePreview?.warnings || [])" :key="i" :title="w" type="warning" class="mt8" :closable="false" />
    </el-dialog>

    <el-dialog v-model="qualPreviewVisible" title="晋级预览" width="640px">
      <el-table :data="qualPreviewItems" border size="small">
        <el-table-column label="球队" prop="teamName" />
        <el-table-column label="小组" prop="groupName" width="80" />
        <el-table-column label="排名" prop="sourceRank" width="60" align="center" />
        <el-table-column label="晋级类型" prop="qualificationTypeName" width="120" />
        <el-table-column label="目标阶段" prop="targetStageName" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Schedule">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  cancelV4StageDraftSchedule, checkV4StageCompletion, confirmV4StageQualification,
  generateV4StageSchedule, listV4StageExecution, previewV4StageQualification,
  previewV4StageSchedule, publishV4StageSchedule,
  type BasketDivisionStageExecutionVo, type StageSchedulePreviewVo
} from '@/api/cnpa/event-v4/stage-execution'
import {
  v4CompetitionFormatLabel,
  v4StageExecutionStatusLabel,
  v4StageExecutionStatusTagType,
  v4StageTypeLabel
} from '@/utils/v4AdminDisplay'

const stageLoading = ref(false)
const stageList = ref<BasketDivisionStageExecutionVo[]>([])
const stageSeasonId = ref<number>()
const stageDivisionId = ref<number>()
const stageDivisionOptions = ref<V4AdminOptionVo[]>([])
const stagePreviewVisible = ref(false)
const stagePreview = ref<StageSchedulePreviewVo | null>(null)
const qualPreviewVisible = ref(false)
const qualPreviewItems = ref<any[]>([])

const seasonOptions = ref<V4AdminOptionVo[]>([])



function formatDivisionLabel(item: V4AdminOptionVo) {
  const count = item.eventTeamCount != null ? ` · ${item.eventTeamCount} 队` : ''
  return item.platformCode ? `${item.label}（${item.platformCode}${count}）` : `${item.label}${count}`
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}



function canGenerate(row: BasketDivisionStageExecutionVo) {
  return ['READY', 'DRAFT'].includes(row.executionStatus || '')
}

async function onStageSeasonChange(seasonId?: number) {
  stageDivisionId.value = undefined
  stageDivisionOptions.value = []
  stageList.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  stageDivisionOptions.value = res.data || []
}

function loadStageList() {
  if (!stageDivisionId.value) return
  stageLoading.value = true
  listV4StageExecution(stageDivisionId.value).then(res => {
    stageList.value = res.data || []
  }).finally(() => { stageLoading.value = false })
}

function handleStagePreview(row: BasketDivisionStageExecutionVo) {
  previewV4StageSchedule(row.stageId).then(res => {
    stagePreview.value = res.data
    stagePreviewVisible.value = true
  })
}

function handleStageGenerate(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('确认生成该阶段草稿赛程？', '提示').then(() => {
    generateV4StageSchedule(row.stageId).then(res => {
      ElMessage.success('生成成功，批次ID=' + res.data)
      loadStageList()
    })
  })
}

function handleStagePublish(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('确认发布该阶段赛程？', '提示').then(() => {
    publishV4StageSchedule(row.stageId).then(() => {
      ElMessage.success('发布成功')
      loadStageList()
    })
  })
}

function handleStageCancel(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('确认作废草稿赛程？', '提示', { type: 'warning' }).then(() => {
    cancelV4StageDraftSchedule(row.stageId).then(() => {
      ElMessage.success('已作废')
      loadStageList()
    })
  })
}

function handleCheckCompletion(row: BasketDivisionStageExecutionVo) {
  checkV4StageCompletion(row.stageId).then(() => {
    ElMessage.success('阶段完成检查已执行')
    loadStageList()
  })
}

function handleQualPreview(row: BasketDivisionStageExecutionVo) {
  previewV4StageQualification(row.stageId).then(res => {
    qualPreviewItems.value = res.data?.items || res.data || []
    qualPreviewVisible.value = true
  })
}

function handleQualConfirm(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('确认晋级结果？将初始化下一阶段参赛队伍', '提示').then(() => {
    confirmV4StageQualification(row.stageId).then(() => {
      ElMessage.success('晋级已确认')
      loadStageList()
    })
  })
}


onMounted(async () => {
  await loadSeasonOptions()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
</style>
