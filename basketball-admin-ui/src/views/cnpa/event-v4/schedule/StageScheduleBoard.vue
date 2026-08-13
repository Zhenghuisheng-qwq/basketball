<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="1100px" destroy-on-close @opened="reload">
    <el-descriptions v-if="summary" :column="4" border size="small" class="mb8">
      <el-descriptions-item label="排期">{{ summary.scheduledUnits ?? 0 }} / {{ summary.totalUnits ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="Structure">{{ summary.structureStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Publish">{{ summary.publishStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Revision">E{{ summary.executionVersion ?? 0 }} / S{{ summary.scheduleRevision ?? 0 }} / P{{ summary.publishNo ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="Rule">{{ summary.ruleStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Roster">{{ summary.rosterStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="Pairing">{{ summary.pairingStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="占位">{{ summary.unresolvedUnits ?? 0 }} TBD</el-descriptions-item>
    </el-descriptions>

    <div class="mb8">
      <el-radio-group v-model="filterStatus" size="small" @change="reload">
        <el-radio-button value="ALL">全部</el-radio-button>
        <el-radio-button value="UNSCHEDULED">未排期</el-radio-button>
        <el-radio-button value="SCHEDULED">已排期</el-radio-button>
      </el-radio-group>
      <el-button class="ml8" size="small" @click="reload">刷新</el-button>
      <el-button size="small" type="primary" :disabled="!canEdit" @click="openBatch">按轮批量排期</el-button>
      <el-button size="small" type="success" v-if="summary?.canRecommend" :disabled="!canEdit" @click="openRecommend">生成推荐排期</el-button>
      <el-button size="small" type="warning" @click="openPublishPreview">预览发布</el-button>
      <el-button size="small" type="success" v-if="summary?.publishStatus !== 'PUBLISHED'" @click="doPublish">检查并发布</el-button>
      <el-button size="small" type="danger" v-if="summary?.publishStatus === 'PUBLISHED'" @click="openWithdraw">撤回发布</el-button>
    </div>

    <el-table v-loading="loading" :data="units" border size="small" max-height="460" @selection-change="onSelect">
      <el-table-column v-if="canEdit" type="selection" width="42" />
      <el-table-column label="轮次" prop="roundNo" width="70" />
      <el-table-column label="BO" prop="bestOf" width="60" />
      <el-table-column label="对阵" min-width="220">
        <template #default="scope">
          {{ scope.row.homeParticipantName || '待定' }} vs {{ scope.row.awayParticipantName || '待定' }}
          <el-tag v-if="scope.row.participantResolutionStatus === 'TBD'" type="warning" size="small" class="ml8">TBD</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="日期" width="120">
        <template #default="scope">{{ scope.row.scheduledDate || '-' }}</template>
      </el-table-column>
      <el-table-column label="时间" width="90">
        <template #default="scope">{{ formatTime(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="来源" width="100">
        <template #default="scope">{{ scope.row.scheduleSource || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">{{ scope.row.scheduleStatus === 'SCHEDULED' ? '已排期' : '未排期' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="scope">
          <el-button link type="primary" :disabled="!scope.row.canEditSchedule" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="warning" :disabled="!scope.row.canEditSchedule" @click="doClear(scope.row)">清除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="canEdit && selected.length" class="mt8">
      <el-button size="small" type="primary" @click="batchSelected">给选中场次排期</el-button>
    </div>

    <el-dialog v-model="editVisible" title="编辑排期" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="日期"><el-date-picker v-model="editDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="开始时间"><el-time-picker v-model="editTime" value-format="HH:mm" format="HH:mm" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchVisible" title="批量排期" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item v-if="batchRound != null" label="轮次"><el-input-number v-model="batchRound" :min="1" /></el-form-item>
        <el-form-item v-else label="已选">{{ selected.length }} 场</el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="batchDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="开始时间"><el-time-picker v-model="batchTime" value-format="HH:mm" format="HH:mm" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recommendVisible" title="推荐排期预览" width="720px" append-to-body>
      <el-alert class="mb8" type="info" :closable="false" :title="`将更新 ${recommend?.proposedCount ?? 0} 场，跳过已排期 ${recommend?.skippedManualCount ?? 0} 场。默认不覆盖手工排期。`" />
      <el-checkbox v-model="overwriteScheduled">重新生成并覆盖已排期</el-checkbox>
      <el-button class="ml8" size="small" @click="loadRecommend">刷新预览</el-button>
      <el-table :data="recommend?.units || []" border size="small" max-height="360" class="mt8">
        <el-table-column label="轮次" prop="roundNo" width="70" />
        <el-table-column label="对阵" min-width="200">
          <template #default="scope">{{ scope.row.homeParticipantName }} vs {{ scope.row.awayParticipantName }}</template>
        </el-table-column>
        <el-table-column label="建议日期" prop="scheduledDate" width="120" />
        <el-table-column label="建议时间" width="90">
          <template #default="scope">{{ formatTime(scope.row.scheduledStartTime) }}</template>
        </el-table-column>
        <el-table-column label="来源" prop="scheduleSource" width="110" />
      </el-table>
      <template #footer>
        <el-button @click="recommendVisible = false">取消</el-button>
        <el-button type="primary" @click="applyRecommend">确认应用</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishVisible" title="发布预览" width="720px" append-to-body>
      <el-alert :type="publishPreview?.canPublish ? 'success' : 'error'" :closable="false"
        :title="publishPreview?.canPublish ? '可以发布' : '存在阻断项，不能发布'" class="mb8" />
      <div v-for="(e, i) in (publishPreview?.errors || [])" :key="'e'+i" class="issue error">ERROR：{{ e.message }}</div>
      <div v-for="(w, i) in (publishPreview?.warnings || [])" :key="'w'+i" class="issue warn">WARNING：{{ w.message }}</div>
      <template #footer>
        <el-button @click="publishVisible = false">关闭</el-button>
        <el-button type="success" :disabled="!publishPreview?.canPublish" @click="doPublish">检查并发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="withdrawVisible" title="撤回发布" width="480px" append-to-body>
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
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyV4ScheduleRecommend,
  assignV4StageSchedule,
  batchAssignV4StageSchedule,
  clearV4StageSchedule,
  listV4StageScheduleUnits,
  previewV4ScheduleRecommend,
  previewV4StagePublish,
  publishV4StageSchedule,
  republishV4StageSchedule,
  withdrawV4StageSchedule,
  type BasketDivisionStageExecutionVo,
  type ScheduleRecommendationPreviewVo,
  type ScheduleUnitView,
  type StagePublishPreviewVo,
  type StageScheduleSummaryVo
} from '@/api/cnpa/event-v4/stage-execution'

const props = defineProps<{ stage?: BasketDivisionStageExecutionVo | null }>()
const emit = defineEmits(['updated'])
const visible = defineModel<boolean>({ default: false })

const loading = ref(false)
const filterStatus = ref('ALL')
const summary = ref<StageScheduleSummaryVo>()
const units = ref<ScheduleUnitView[]>([])
const selected = ref<ScheduleUnitView[]>([])
const editVisible = ref(false)
const editRow = ref<ScheduleUnitView>()
const editDate = ref('')
const editTime = ref('')
const batchVisible = ref(false)
const batchRound = ref<number | null>(null)
const batchDate = ref('')
const batchTime = ref('')
const recommendVisible = ref(false)
const overwriteScheduled = ref(false)
const recommend = ref<ScheduleRecommendationPreviewVo>()
const publishVisible = ref(false)
const publishPreview = ref<StagePublishPreviewVo>()
const withdrawVisible = ref(false)
const withdrawReason = ref('')

const dialogTitle = computed(() => `赛段排期 / 发布 · ${props.stage?.stageName || ''}`)
const canEdit = computed(() => summary.value?.publishStatus !== 'PUBLISHED' && summary.value?.structureStatus === 'ACTIVE')

function formatTime(v?: string) {
  return v ? String(v).slice(0, 5) : '-'
}

function reload() {
  if (!props.stage?.stageId) return
  loading.value = true
  const status = filterStatus.value === 'ALL' ? undefined : filterStatus.value
  listV4StageScheduleUnits(props.stage.stageId, status).then(res => {
    summary.value = res.data?.summary
    units.value = res.data?.units || []
  }).finally(() => { loading.value = false })
}

function onSelect(rows: ScheduleUnitView[]) {
  selected.value = rows
}

function openEdit(row: ScheduleUnitView) {
  editRow.value = row
  editDate.value = row.scheduledDate || ''
  editTime.value = formatTime(row.scheduledStartTime) === '-' ? '21:00' : formatTime(row.scheduledStartTime)
  editVisible.value = true
}

function submitEdit() {
  if (!props.stage?.stageId || !editRow.value) return
  assignV4StageSchedule(props.stage.stageId, {
    unitType: editRow.value.unitType,
    unitId: editRow.value.unitId,
    scheduledDate: editDate.value,
    scheduledStartTime: editTime.value
  }).then(() => {
    ElMessage.success('已排期')
    editVisible.value = false
    reload()
    emit('updated')
  })
}

function doClear(row: ScheduleUnitView) {
  if (!props.stage?.stageId) return
  clearV4StageSchedule(props.stage.stageId, { unitType: row.unitType, unitId: row.unitId }).then(() => {
    ElMessage.success('已清除排期')
    reload()
    emit('updated')
  })
}

function openBatch() {
  batchRound.value = 1
  batchDate.value = ''
  batchTime.value = '21:00'
  selected.value = []
  batchVisible.value = true
}

function batchSelected() {
  batchRound.value = null
  batchDate.value = ''
  batchTime.value = '21:00'
  batchVisible.value = true
}

function submitBatch() {
  if (!props.stage?.stageId) return
  const payload: any = { scheduledDate: batchDate.value, scheduledStartTime: batchTime.value }
  if (batchRound.value != null) payload.roundNo = batchRound.value
  else payload.items = selected.value.map(u => ({ unitType: u.unitType, unitId: u.unitId }))
  batchAssignV4StageSchedule(props.stage.stageId, payload).then(() => {
    ElMessage.success('批量排期成功')
    batchVisible.value = false
    reload()
    emit('updated')
  })
}

function openRecommend() {
  overwriteScheduled.value = false
  recommendVisible.value = true
  loadRecommend()
}

function loadRecommend() {
  if (!props.stage?.stageId) return
  previewV4ScheduleRecommend(props.stage.stageId, overwriteScheduled.value).then(res => {
    recommend.value = res.data
  })
}

function applyRecommend() {
  if (!props.stage?.stageId) return
  applyV4ScheduleRecommend(props.stage.stageId, overwriteScheduled.value).then(() => {
    ElMessage.success('已应用推荐排期')
    recommendVisible.value = false
    reload()
    emit('updated')
  })
}

function openPublishPreview() {
  if (!props.stage?.stageId) return
  previewV4StagePublish(props.stage.stageId).then(res => {
    publishPreview.value = res.data
    publishVisible.value = true
  })
}

function doPublish() {
  if (!props.stage?.stageId) return
  const action = summary.value?.publishStatus === 'WITHDRAWN' ? republishV4StageSchedule : publishV4StageSchedule
  ElMessageBox.confirm('确认按当前排期发布该赛段？', '发布').then(() => {
    action(props.stage!.stageId).then(() => {
      ElMessage.success('发布成功')
      publishVisible.value = false
      reload()
      emit('updated')
    })
  })
}

function openWithdraw() {
  withdrawReason.value = ''
  withdrawVisible.value = true
}

function doWithdraw() {
  if (!props.stage?.stageId) return
  if (!withdrawReason.value.trim()) {
    ElMessage.error('撤回原因必填')
    return
  }
  withdrawV4StageSchedule(props.stage.stageId, withdrawReason.value.trim()).then(() => {
    ElMessage.success('已撤回')
    withdrawVisible.value = false
    reload()
    emit('updated')
  })
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.ml8 { margin-left: 8px; }
.issue { font-size: 13px; margin-bottom: 4px; }
.issue.error { color: #f56c6c; }
.issue.warn { color: #e6a23c; }
</style>
