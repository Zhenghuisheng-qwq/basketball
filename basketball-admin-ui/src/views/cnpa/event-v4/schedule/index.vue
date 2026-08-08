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
          <el-table-column label="操作" width="560" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleStagePreview(scope.row)">预览</el-button>
              <el-button link type="success" v-if="canGenerate(scope.row)" @click="handleStageGenerate(scope.row)">生成</el-button>
              <el-button link type="success" v-if="scope.row.executionStatus === 'SCHEDULE_GENERATED'" @click="handleStagePublish(scope.row)">发布</el-button>
              <el-button link type="warning" v-if="scope.row.executionStatus === 'SCHEDULE_GENERATED'" @click="handleStageCancel(scope.row)">作废</el-button>
              <el-button
                link type="warning"
                v-if="canAdjustPairing(scope.row)"
                v-hasPermi="['basket:v4Stage:exception']"
                @click="openPairingAdjust(scope.row)"
              >调整对阵</el-button>
              <el-button link type="primary" v-if="scope.row.executionStatus === 'RUNNING'" @click="handleCheckCompletion(scope.row)">检查完成</el-button>
              <el-button link type="primary" v-if="['RUNNING','RESULT_PENDING_CONFIRM'].includes(scope.row.executionStatus)" @click="handleQualPreview(scope.row)">晋级预览</el-button>
              <el-button
                link type="warning"
                v-if="canManualAdvance(scope.row)"
                v-hasPermi="['basket:v4Stage:exception']"
                @click="openManualAdvance(scope.row)"
              >直接晋级</el-button>
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

    <el-dialog v-model="manualAdvanceVisible" title="人工直接晋级" width="480px">
      <el-alert type="info" :closable="false" show-icon class="mb8"
        :title="`阶段：${manualAdvanceStage?.stageName || '-'}（${manualAdvanceStage?.participantCount || 0} 队）`" />
      <el-form label-width="90px">
        <el-form-item label="目标球队" required>
          <el-select v-model="manualAdvanceTeamId" filterable placeholder="选择球队" style="width:100%">
            <el-option v-for="t in manualAdvanceTeams" :key="t.eventTeamId" :label="teamOptionLabel(t)" :value="t.eventTeamId" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="manualAdvanceReason" type="textarea" rows="3" placeholder="必填，将记录审计" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualAdvanceVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualAdvance">确认直接晋级</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pairingAdjustVisible" title="调整对阵（草稿）" width="640px">
      <el-alert type="warning" :closable="false" show-icon class="mb8"
        title="仅草稿赛程可调整。已发布或已有正式结果的对阵不可修改。" />
      <div v-for="(slot, idx) in pairingSlots" :key="idx" class="pairing-row">
        <span class="pairing-row__label">对阵 {{ idx + 1 }}</span>
        <el-select v-model="slot.homeEventTeamId" filterable placeholder="主队" style="width:180px">
          <el-option v-for="t in pairingStageTeams" :key="'h'+t.eventTeamId" :label="t.teamName" :value="t.eventTeamId" />
        </el-select>
        <span>vs</span>
        <el-select v-model="slot.awayEventTeamId" filterable placeholder="客队" style="width:180px">
          <el-option v-for="t in pairingStageTeams" :key="'a'+t.eventTeamId" :label="t.teamName" :value="t.eventTeamId" />
        </el-select>
      </div>
      <el-form class="mt8" label-width="90px">
        <el-form-item label="原因" required>
          <el-input v-model="pairingAdjustReason" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pairingAdjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPairingAdjust">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Schedule">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  cancelV4StageDraftSchedule, checkV4StageCompletion, confirmV4StageQualification,
  generateV4StageSchedule, listV4StageExecution, listV4StageTeams, previewV4StageQualification,
  previewV4StageSchedule, publishV4StageSchedule,
  type BasketDivisionStageExecutionVo, type StageSchedulePreviewVo, type StageTeamItemVo
} from '@/api/cnpa/event-v4/stage-execution'
import { adjustV4Pairing, manualAdvanceV4, type PairingSlotReq } from '@/api/cnpa/event-v4/stage-exception'
import { getV4ScheduleBatch } from '@/api/cnpa/event-v4/schedule-batch'
import { listV4MatchSeriesByStage } from '@/api/cnpa/event-v4/match-series'
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

const manualAdvanceVisible = ref(false)
const manualAdvanceStage = ref<BasketDivisionStageExecutionVo>()
const manualAdvanceTeams = ref<StageTeamItemVo[]>([])
const manualAdvanceTeamId = ref<number>()
const manualAdvanceReason = ref('')

const pairingAdjustVisible = ref(false)
const pairingAdjustStage = ref<BasketDivisionStageExecutionVo>()
const pairingStageTeams = ref<StageTeamItemVo[]>([])
const pairingSlots = ref<PairingSlotReq[]>([])
const pairingAdjustReason = ref('')

const seasonOptions = ref<V4AdminOptionVo[]>([])

function formatDivisionLabel(item: V4AdminOptionVo) {
  const count = item.eventTeamCount != null ? ` · ${item.eventTeamCount} 队` : ''
  return item.platformCode ? `${item.label}（${item.platformCode}${count}）` : `${item.label}${count}`
}

function teamOptionLabel(t: StageTeamItemVo) {
  return t.seedNo != null ? `#${t.seedNo} ${t.teamName}` : (t.teamName || String(t.eventTeamId))
}

function canGenerate(row: BasketDivisionStageExecutionVo) {
  return ['READY', 'DRAFT'].includes(row.executionStatus || '')
}

function canManualAdvance(row: BasketDivisionStageExecutionVo) {
  return row.competitionFormat === 'SINGLE_ELIMINATION'
    && !!row.nextStageCode
    && ['RUNNING', 'SCHEDULE_GENERATED', 'RESULT_PENDING_CONFIRM'].includes(row.executionStatus || '')
}

function canAdjustPairing(row: BasketDivisionStageExecutionVo) {
  return row.competitionFormat === 'SINGLE_ELIMINATION'
    && row.executionStatus === 'SCHEDULE_GENERATED'
    && !!row.scheduleBatchId
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
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

async function openManualAdvance(row: BasketDivisionStageExecutionVo) {
  manualAdvanceStage.value = row
  manualAdvanceTeamId.value = undefined
  manualAdvanceReason.value = ''
  const res = await listV4StageTeams(row.stageId)
  manualAdvanceTeams.value = res.data || []
  if (!manualAdvanceTeams.value.length) {
    ElMessage.warning('当前阶段尚无参赛队伍')
    return
  }
  manualAdvanceVisible.value = true
}

function submitManualAdvance() {
  if (!manualAdvanceStage.value?.stageId || !manualAdvanceTeamId.value || !manualAdvanceReason.value?.trim()) {
    ElMessage.warning('请选择球队并填写原因')
    return
  }
  ElMessageBox.confirm('确认将该球队设为直接晋级？', '二次确认', { type: 'warning' }).then(() => {
    manualAdvanceV4({
      stageId: manualAdvanceStage.value!.stageId,
      eventTeamId: manualAdvanceTeamId.value!,
      reason: manualAdvanceReason.value.trim()
    }).then(() => {
      ElMessage.success('已写入人工晋级预览')
      manualAdvanceVisible.value = false
      handleQualPreview(manualAdvanceStage.value!)
      loadStageList()
    })
  })
}

async function openPairingAdjust(row: BasketDivisionStageExecutionVo) {
  pairingAdjustStage.value = row
  pairingAdjustReason.value = ''
  const teamsRes = await listV4StageTeams(row.stageId)
  pairingStageTeams.value = teamsRes.data || []
  pairingSlots.value = []

  const seriesRes = await listV4MatchSeriesByStage(row.stageId)
  const seriesList = seriesRes.data || []
  if (seriesList.length > 0) {
    pairingSlots.value = seriesList.map(s => ({
      bracketSlot: s.bracketSlot ?? 0,
      homeEventTeamId: s.homeEventTeamId!,
      awayEventTeamId: s.awayEventTeamId!
    }))
  } else if (row.scheduleBatchId) {
    const batchRes = await getV4ScheduleBatch(row.scheduleBatchId)
    const matches = (batchRes.data?.matches || []).filter((m: any) => m.roundNo === 1 && !m.seriesId)
    const slotMap = new Map<number, PairingSlotReq>()
    for (const m of matches) {
      const slot = m.bracketSlot ?? 0
      if (!slotMap.has(slot)) {
        slotMap.set(slot, {
          bracketSlot: slot,
          homeEventTeamId: m.homeEventTeamId,
          awayEventTeamId: m.awayEventTeamId
        })
      }
    }
    pairingSlots.value = [...slotMap.values()].sort((a, b) => a.bracketSlot - b.bracketSlot)
  }
  if (!pairingSlots.value.length) {
    ElMessage.warning('未找到可调整的对阵')
    return
  }
  pairingAdjustVisible.value = true
}

function submitPairingAdjust() {
  const stage = pairingAdjustStage.value
  if (!stage?.stageId || !stage.scheduleBatchId) return
  if (!pairingAdjustReason.value?.trim()) {
    ElMessage.warning('请填写原因')
    return
  }
  const used = new Set<number>()
  for (const slot of pairingSlots.value) {
    if (!slot.homeEventTeamId || !slot.awayEventTeamId) {
      ElMessage.warning('对阵双方不能为空')
      return
    }
    if (slot.homeEventTeamId === slot.awayEventTeamId) {
      ElMessage.warning('对阵双方不能相同')
      return
    }
    if (used.has(slot.homeEventTeamId) || used.has(slot.awayEventTeamId)) {
      ElMessage.warning('每支球队只能出现一次')
      return
    }
    used.add(slot.homeEventTeamId)
    used.add(slot.awayEventTeamId)
  }
  ElMessageBox.confirm('确认调整对阵？将重建草稿 Match/Series。', '二次确认', { type: 'warning' }).then(() => {
    adjustV4Pairing({
      stageId: stage.stageId,
      slots: pairingSlots.value,
      reason: pairingAdjustReason.value.trim()
    }).then(() => {
      ElMessage.success('对阵已调整')
      pairingAdjustVisible.value = false
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
.mt8 { margin-top: 8px; }
.pairing-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.pairing-row__label { width: 64px; color: #606266; font-size: 13px; }
</style>
