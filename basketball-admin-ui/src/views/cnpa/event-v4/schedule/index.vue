<template>
  <div class="app-container">
    <el-alert title="赛程管理：支持按 Division 阶段生成循环赛/小组赛/单败淘汰赛程，确认后发布" type="info" :closable="false" show-icon class="mb8" />

    <el-tabs v-model="mainTab">
      <el-tab-pane label="阶段执行" name="stage">
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
      </el-tab-pane>

      <el-tab-pane label="历史赛程（只读）" name="batch">
    <el-alert title="历史旧赛程：仅可查看和作废草稿，新建 Division 请使用「阶段执行」入口" type="warning" :closable="false" show-icon class="mb8" />
    <el-form :model="query" :inline="true" class="mb8">
      <el-form-item label="赛季">
        <el-select v-model="query.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onQuerySeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="query.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!query.seasonId">
          <el-option v-for="item in queryDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="query.publishStatus" clearable placeholder="全部" style="width:120px">
          <el-option v-for="item in PUBLISH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
        <el-button type="success" icon="Plus" v-if="legacyGenerateAllowed" @click="openGenerate">生成草稿</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="赛季" prop="seasonTitle" min-width="120" />
      <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
      <el-table-column label="平台" prop="platformCode" width="70" />
      <el-table-column label="赛制" prop="formatType" width="140" align="center">
        <template #default="scope">
          <el-tag size="small" class="nowrap-tag">{{ v4FormatTypeLabel(scope.row.formatType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="队数" prop="teamCount" width="60" align="center" />
      <el-table-column label="场数" prop="matchCount" width="60" align="center" />
      <el-table-column label="生成状态" prop="generateStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4GenerateStatusTagType(scope.row.generateStatus)" size="small" class="nowrap-tag" :title="scope.row.generateStatus">
            {{ v4GenerateStatusLabel(scope.row.generateStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" prop="publishStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4PublishStatusTagType(scope.row.publishStatus)" size="small" class="nowrap-tag" :title="scope.row.publishStatus">
            {{ v4PublishStatusLabel(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生成时间" prop="generateTime" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="viewDetail(scope.row)">详情</el-button>
          <el-button link type="success" v-if="scope.row.publishStatus === 'DRAFT'" @click="handlePublish(scope.row)">发布</el-button>
          <el-button link type="danger" v-if="scope.row.publishStatus === 'DRAFT'" @click="handleCancel(scope.row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="generateVisible" title="生成草稿赛程" width="520px">
      <el-form :model="generateForm" label-width="120px">
        <el-form-item label="选择赛季" required>
          <el-select v-model="generateSeasonId" filterable placeholder="选择赛季" style="width:100%" @change="onGenerateSeasonChange">
            <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台赛区" required>
          <el-select v-model="generateForm.divisionId" filterable placeholder="选择赛区" style="width:100%" :disabled="!generateSeasonId" @change="onGenerateDivisionChange">
            <el-option v-for="item in formDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参赛队伍数">
          <el-input :model-value="selectedDivisionTeamCount" disabled />
        </el-form-item>
        <el-form-item label="预计比赛场数">
          <el-input :model-value="expectedMatchCount" disabled />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="generateForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <div v-if="generateDisabledReason" class="generate-tip">{{ generateDisabledReason }}</div>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!!generateDisabledReason" @click="submitGenerate">确认生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="批次详情" size="60%">
      <el-descriptions v-if="detail" :column="2" border size="small" class="mb8">
        <el-descriptions-item label="批次ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="平台赛区">{{ detail.divisionName }}</el-descriptions-item>
        <el-descriptions-item label="队数/场数">{{ detail.teamCount }} / {{ detail.matchCount }}</el-descriptions-item>
        <el-descriptions-item label="规则">{{ detail.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="发布状态">{{ v4PublishStatusLabel(detail.publishStatus) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.matches || []" border size="small">
        <el-table-column label="轮次" prop="roundNo" width="60" />
        <el-table-column label="编号" prop="matchNo" min-width="130" />
        <el-table-column label="时间" prop="matchTime" width="160" />
        <el-table-column label="主队" prop="homeTeamName" min-width="100" />
        <el-table-column label="客队" prop="awayTeamName" min-width="100" />
        <el-table-column label="状态" width="90">
          <template #default="scope">{{ v4MatchStatusLabel(scope.row.matchStatus) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
      </el-tab-pane>
    </el-tabs>

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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelV4ScheduleBatch, generateV4ScheduleBatch, getV4ScheduleBatch,
  listV4ScheduleBatch, publishV4ScheduleBatch,
  type BasketScheduleBatchDetailVo, type BasketScheduleBatchQuery
} from '@/api/cnpa/event-v4/schedule-batch'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  cancelV4StageDraftSchedule, checkV4StageCompletion, confirmV4StageQualification,
  generateV4StageSchedule, listV4StageExecution, previewV4StageQualification,
  previewV4StageSchedule, publishV4StageSchedule,
  type BasketDivisionStageExecutionVo, type StageSchedulePreviewVo
} from '@/api/cnpa/event-v4/stage-execution'
import {
  V4_PUBLISH_STATUS_FILTER_OPTIONS,
  calcRoundRobinMatchCount,
  v4CompetitionFormatLabel,
  v4FormatTypeLabel,
  v4GenerateStatusLabel,
  v4GenerateStatusTagType,
  v4MatchStatusLabel,
  v4PublishStatusLabel,
  v4PublishStatusTagType,
  v4StageExecutionStatusLabel,
  v4StageExecutionStatusTagType,
  v4StageTypeLabel
} from '@/utils/v4AdminDisplay'

const mainTab = ref('stage')
const stageLoading = ref(false)
const stageList = ref<BasketDivisionStageExecutionVo[]>([])
const stageSeasonId = ref<number>()
const stageDivisionId = ref<number>()
const stageDivisionOptions = ref<V4AdminOptionVo[]>([])
const stagePreviewVisible = ref(false)
const stagePreview = ref<StageSchedulePreviewVo | null>(null)
const qualPreviewVisible = ref(false)
const qualPreviewItems = ref<any[]>([])

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<BasketScheduleBatchQuery>({ pageNum: 1, pageSize: 10 })
const generateVisible = ref(false)
const generateForm = reactive({ divisionId: undefined as number | undefined, remark: '' })
const generateSeasonId = ref<number>()
const detailVisible = ref(false)
const detail = ref<BasketScheduleBatchDetailVo | null>(null)
const seasonOptions = ref<V4AdminOptionVo[]>([])
const queryDivisionOptions = ref<V4AdminOptionVo[]>([])
const formDivisionOptions = ref<V4AdminOptionVo[]>([])
const PUBLISH_OPTIONS = V4_PUBLISH_STATUS_FILTER_OPTIONS

const selectedDivision = computed(() => formDivisionOptions.value.find(d => d.id === generateForm.divisionId))
const selectedQueryDivision = computed(() => queryDivisionOptions.value.find(d => d.id === query.divisionId))
const legacyGenerateAllowed = computed(() => {
  if (query.divisionId && selectedQueryDivision.value?.formatSnapshotId) return false
  return true
})
const selectedDivisionTeamCount = computed(() => selectedDivision.value?.eventTeamCount ?? 0)
const expectedMatchCount = computed(() => calcRoundRobinMatchCount(selectedDivisionTeamCount.value))
const generateDisabledReason = computed(() => {
  if (!generateForm.divisionId) return '请选择平台赛区'
  if (selectedDivision.value?.formatSnapshotId) return '该 Division 已启用动态赛制，请使用阶段执行入口'
  if (selectedDivisionTeamCount.value < 2) return '参赛队伍少于 2 支，无法生成单循环赛程'
  return ''
})

function formatDivisionLabel(item: V4AdminOptionVo) {
  const count = item.eventTeamCount != null ? ` · ${item.eventTeamCount} 队` : ''
  return item.platformCode ? `${item.label}（${item.platformCode}${count}）` : `${item.label}${count}`
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadQueryDivisions(seasonId?: number) {
  queryDivisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  queryDivisionOptions.value = res.data || []
}

async function loadFormDivisions(seasonId?: number) {
  formDivisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  formDivisionOptions.value = res.data || []
}

function onQuerySeasonChange(seasonId?: number) {
  query.divisionId = undefined
  loadQueryDivisions(seasonId)
}

function onGenerateSeasonChange(seasonId?: number) {
  generateForm.divisionId = undefined
  loadFormDivisions(seasonId)
}

function onGenerateDivisionChange() {
  // counts derived from selectedDivision
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

function loadList() {
  loading.value = true
  listV4ScheduleBatch(query).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function openGenerate() {
  generateSeasonId.value = query.seasonId
  generateForm.divisionId = query.divisionId
  generateForm.remark = ''
  loadFormDivisions(generateSeasonId.value)
  generateVisible.value = true
}

function submitGenerate() {
  if (generateDisabledReason.value) {
    ElMessage.warning(generateDisabledReason.value)
    return
  }
  generateV4ScheduleBatch({ divisionId: generateForm.divisionId!, remark: generateForm.remark }).then(res => {
    ElMessage.success('生成成功，批次ID=' + res.data)
    generateVisible.value = false
    loadList()
  })
}

function viewDetail(row: any) {
  getV4ScheduleBatch(row.id).then(res => {
    detail.value = res.data
    detailVisible.value = true
  })
}

function handlePublish(row: any) {
  ElMessageBox.confirm('确认发布该赛程？发布后平台赛区将变为已排赛', '提示').then(() => {
    publishV4ScheduleBatch(row.id).then(() => {
      ElMessage.success('发布成功')
      loadList()
    })
  })
}

function handleCancel(row: any) {
  ElMessageBox.confirm('确认作废该草稿批次？关联草稿比赛将一并删除', '提示', { type: 'warning' }).then(() => {
    cancelV4ScheduleBatch(row.id).then(() => {
      ElMessage.success('已作废')
      loadList()
    })
  })
}

onMounted(async () => {
  await loadSeasonOptions()
  loadList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
.generate-tip { color: var(--el-color-warning); font-size: 13px; margin: 0 0 8px 120px; }
</style>
