<template>
  <div class="app-container">
    <el-card shadow="never" class="mb8">
      <template #header><span>赛事与规则</span></template>
      <el-form :inline="true" label-width="88px">
        <el-form-item label="所属赛事">
          <el-select v-model="filterTournamentId" placeholder="请选择赛事" filterable clearable style="width: 180px" @change="handleTournamentChange">
            <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛季(eventId)">
          <el-select v-model="selectedEventId" placeholder="请选择赛季" filterable clearable style="width: 180px" @change="handleEventChange">
            <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛程规则">
          <el-select v-model="selectedRuleId" placeholder="请选择规则" filterable style="width: 220px" @change="loadRuleDetail">
            <el-option v-for="item in ruleOptions" :key="item.id" :label="`${item.ruleName} (${item.ruleVersion})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="selectedStageId" placeholder="请选择阶段" filterable clearable style="width: 160px">
            <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="eventContext" :column="4" border size="small" class="mb8">
        <el-descriptions-item label="赛事名称">{{ eventContext.tournamentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ eventContext.seasonName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="游戏端">{{ eventContext.platformDisplayName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参赛球队">{{ eventContext.eventTeamCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已有比赛">{{ eventContext.existingMatchCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="草稿赛程">{{ eventContext.hasDraftSchedule ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="已发布">{{ eventContext.hasPublishedSchedule ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>

      <el-descriptions v-if="ruleDetail" title="规则配置" :column="2" border size="small" class="mb8">
        <el-descriptions-item label="规则编码">{{ ruleDetail.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="规则版本">{{ ruleDetail.ruleVersion }}</el-descriptions-item>
        <el-descriptions-item label="比赛日">
          {{ (ruleDetail.weekdays || []).map(w => w.weekdayName).join('、') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="时间段">
          {{ (ruleDetail.timeSlots || []).map(t => t.startTime).join('、') || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ ruleDetail.remark || '常规赛单循环，对阵由策略生成，比赛时间来自配置表' }}</el-descriptions-item>
      </el-descriptions>

      <el-space wrap>
        <el-button type="primary" :disabled="!canGenerate" @click="openGenerateConfirm" v-hasPermi="['basket:schedule:generate']">生成草稿赛程</el-button>
        <el-button type="success" :disabled="!selectedEventId || !eventContext?.hasDraftSchedule" @click="handlePublish" v-hasPermi="['basket:schedule:publish']">发布赛程</el-button>
        <el-button type="warning" :disabled="!eventContext?.hasPublishedSchedule" @click="handleUnpublish" v-hasPermi="['basket:schedule:publish']">下架赛程</el-button>
        <el-button type="danger" :disabled="!eventContext?.hasDraftSchedule" @click="handleDeleteDraft" v-hasPermi="['basket:schedule:remove']">删除草稿</el-button>
        <el-button type="primary" plain icon="Plus" :disabled="!selectedEventId" @click="openMatchForm()" v-hasPermi="['basket:schedule:add']">手动创建比赛</el-button>
      </el-space>
    </el-card>

    <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="88px" class="mb8">
      <el-form-item label="比赛状态">
        <el-select v-model="queryParams.matchStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in MATCH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="queryParams.publishStatus" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in PUBLISH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="matchList">
      <el-table-column label="轮次" prop="roundNo" width="70" />
      <el-table-column label="阶段" prop="stageName" min-width="100" />
      <el-table-column label="比赛时间" prop="scheduledStartTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="主队" prop="homeTeamName" min-width="120" />
      <el-table-column label="客队" prop="awayTeamName" min-width="120" />
      <el-table-column label="比分" width="90">
        <template #default="scope">{{ scope.row.homeScore ?? '-' }} : {{ scope.row.awayScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="比赛状态" prop="matchStatusName" width="100" />
      <el-table-column label="发布状态" prop="publishStatusName" width="100" />
      <el-table-column label="规则" min-width="140">
        <template #default="scope">{{ scope.row.ruleCode || '-' }} / {{ scope.row.ruleVersion || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openMatchForm(scope.row)" v-hasPermi="['basket:schedule:edit']">调整</el-button>
          <el-button link type="success" @click="openScoreForm(scope.row)" v-hasPermi="['basket:schedule:score']">录入比分</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="generateOpen" title="确认生成草稿赛程" width="520px" append-to-body>
      <el-descriptions v-if="generatePreview" :column="1" border>
        <el-descriptions-item label="赛事">{{ generatePreview.tournamentName }} / {{ generatePreview.seasonName }}</el-descriptions-item>
        <el-descriptions-item label="游戏端">{{ generatePreview.platformCode }}</el-descriptions-item>
        <el-descriptions-item label="参赛球队">{{ generatePreview.eventTeamCount }}</el-descriptions-item>
        <el-descriptions-item label="预计比赛数">{{ generatePreview.expectedMatchCount }}</el-descriptions-item>
        <el-descriptions-item label="规则">{{ generatePreview.ruleName }} ({{ generatePreview.ruleVersion }})</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="generateOpen = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="confirmGenerate">确认生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="matchOpen" :title="matchForm.id ? '调整比赛' : '手动创建比赛'" width="560px" append-to-body>
      <el-form ref="matchFormRef" :model="matchForm" :rules="matchRules" label-width="100px">
        <el-form-item label="主队" prop="homeEventTeamId">
          <el-select v-model="matchForm.homeEventTeamId" filterable placeholder="选择主队 EventTeam" style="width: 100%">
            <el-option v-for="item in eventTeamOptions" :key="item.id" :label="item.teamNameSnapshot" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="客队" prop="awayEventTeamId">
          <el-select v-model="matchForm.awayEventTeamId" filterable placeholder="选择客队 EventTeam" style="width: 100%">
            <el-option v-for="item in eventTeamOptions" :key="item.id" :label="item.teamNameSnapshot" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段" prop="stageId">
          <el-select v-model="matchForm.stageId" filterable placeholder="选择阶段" style="width: 100%">
            <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="轮次" prop="roundNo">
          <el-input-number v-model="matchForm.roundNo" :min="1" />
        </el-form-item>
        <el-form-item label="比赛时间" prop="matchTime">
          <el-date-picker v-model="matchForm.matchTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="比赛状态">
          <el-select v-model="matchForm.matchStatus" style="width: 100%">
            <el-option v-for="item in MATCH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="matchForm.publishStatus" style="width: 100%">
            <el-option v-for="item in PUBLISH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="matchForm.remark" type="textarea" />
        </el-form-item>
        <el-form-item v-if="matchForm.id" label="调整原因">
          <el-input v-model="matchForm.reason" type="textarea" placeholder="修改主客队或时间时建议填写" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="matchOpen = false">取消</el-button>
        <el-button type="primary" @click="submitMatchForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scoreOpen" title="录入比分" width="420px" append-to-body>
      <el-form ref="scoreFormRef" :model="scoreForm" :rules="scoreRules" label-width="90px">
        <el-form-item label="主队比分" prop="homeScore">
          <el-input-number v-model="scoreForm.homeScore" :min="0" />
        </el-form-item>
        <el-form-item label="客队比分" prop="awayScore">
          <el-input-number v-model="scoreForm.awayScore" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="scoreForm.reason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scoreOpen = false">取消</el-button>
        <el-button type="primary" @click="submitScore">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { listTournament } from '@/api/basket/tournament'
import { listSeasonByTournament } from '@/api/basket/season'
import { listStage } from '@/api/basket/tournamentStage'
import { listEventTeam } from '@/api/basket/eventTeam'
import {
  createScheduleMatch,
  deleteScheduleDraft,
  generateScheduleDraft,
  getScheduleEventContext,
  getScheduleRuleDetail,
  listScheduleMatch,
  listScheduleRule,
  previewScheduleGenerate,
  publishSchedule,
  saveScheduleScore,
  unpublishSchedule,
  updateScheduleMatch
} from '@/api/basket/scheduleManage'
import type {
  EventTeamListItem,
  ScheduleEventContext,
  ScheduleGeneratePreview,
  ScheduleMatchListItem,
  ScheduleRuleDetail,
  ScheduleRuleListItem,
  SeasonListItem,
  StageListItem,
  TournamentListItem
} from '@/types'

const { proxy } = getCurrentInstance()!

const MATCH_STATUS_OPTIONS = [
  { label: '已编排/未开始', value: 1 },
  { label: '进行中', value: 2 },
  { label: '已结束', value: 3 },
  { label: '已延期', value: 4 },
  { label: '已取消', value: 5 }
]
const PUBLISH_STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '已发布', value: 1 },
  { label: '已下架', value: 2 }
]

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const matchList = ref<ScheduleMatchListItem[]>([])

const filterTournamentId = ref<number>()
const selectedEventId = ref<number>()
const selectedRuleId = ref<number>()
const selectedStageId = ref<number>()

const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const stageOptions = ref<StageListItem[]>([])
const ruleOptions = ref<ScheduleRuleListItem[]>([])
const eventTeamOptions = ref<EventTeamListItem[]>([])

const eventContext = ref<ScheduleEventContext>()
const ruleDetail = ref<ScheduleRuleDetail>()

const generateOpen = ref(false)
const generateLoading = ref(false)
const generatePreview = ref<ScheduleGeneratePreview>()

const matchOpen = ref(false)
const scoreOpen = ref(false)
const currentScoreMatchId = ref<number>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  seasonId: undefined as number | undefined,
  matchStatus: undefined as number | undefined,
  publishStatus: undefined as number | undefined
})

const matchForm = reactive<any>({
  id: undefined,
  homeEventTeamId: undefined,
  awayEventTeamId: undefined,
  stageId: undefined,
  roundNo: 1,
  matchTime: '',
  matchStatus: 1,
  publishStatus: 0,
  remark: '',
  reason: ''
})

const scoreForm = reactive({ homeScore: 0, awayScore: 0, reason: '' })

const matchRules = {
  homeEventTeamId: [{ required: true, message: '请选择主队', trigger: 'change' }],
  awayEventTeamId: [{ required: true, message: '请选择客队', trigger: 'change' }],
  stageId: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  matchTime: [{ required: true, message: '请选择比赛时间', trigger: 'change' }]
}

const scoreRules = {
  homeScore: [{ required: true, message: '请输入主队比分', trigger: 'blur' }],
  awayScore: [{ required: true, message: '请输入客队比分', trigger: 'blur' }]
}

const canGenerate = computed(() =>
  !!selectedEventId.value && !!selectedRuleId.value && !eventContext.value?.hasPublishedSchedule
)

function parseTime(val?: string) {
  return proxy?.parseTime ? proxy.parseTime(val) : val
}

async function loadTournaments() {
  const res = await listTournament({ pageNum: 1, pageSize: 200 })
  tournamentOptions.value = res.rows || []
}

async function handleTournamentChange() {
  selectedEventId.value = undefined
  seasonOptions.value = []
  if (!filterTournamentId.value) return
  const res = await listSeasonByTournament(filterTournamentId.value)
  seasonOptions.value = res.data || []
}

async function handleEventChange() {
  queryParams.seasonId = selectedEventId.value
  eventContext.value = undefined
  eventTeamOptions.value = []
  stageOptions.value = []
  if (!selectedEventId.value) return
  const [ctxRes, stageRes, teamRes] = await Promise.all([
    getScheduleEventContext(selectedEventId.value),
    listStage({ pageNum: 1, pageSize: 200, seasonId: selectedEventId.value }),
    listEventTeam({ pageNum: 1, pageSize: 500, seasonId: selectedEventId.value, participationStatus: 1 })
  ])
  eventContext.value = ctxRes.data
  stageOptions.value = stageRes.rows || []
  eventTeamOptions.value = teamRes.rows || []
  if (!selectedStageId.value && stageOptions.value.length) {
    selectedStageId.value = stageOptions.value[0].id
  }
  getList()
}

async function loadRules() {
  const res = await listScheduleRule()
  ruleOptions.value = res.data || []
  if (ruleOptions.value.length && !selectedRuleId.value) {
    selectedRuleId.value = ruleOptions.value[0].id
    loadRuleDetail()
  }
}

async function loadRuleDetail() {
  if (!selectedRuleId.value) {
    ruleDetail.value = undefined
    return
  }
  const res = await getScheduleRuleDetail(selectedRuleId.value)
  ruleDetail.value = res.data
}

function getList() {
  if (!queryParams.seasonId) {
    matchList.value = []
    total.value = 0
    return
  }
  loading.value = true
  listScheduleMatch(queryParams).then(res => {
    matchList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  queryParams.matchStatus = undefined
  queryParams.publishStatus = undefined
  handleQuery()
}

async function openGenerateConfirm() {
  if (!selectedEventId.value || !selectedRuleId.value) return
  const res = await previewScheduleGenerate({
    eventId: selectedEventId.value,
    ruleId: selectedRuleId.value,
    stageId: selectedStageId.value
  })
  generatePreview.value = res.data
  generateOpen.value = true
}

async function confirmGenerate() {
  if (!selectedEventId.value || !selectedRuleId.value) return
  generateLoading.value = true
  try {
    await generateScheduleDraft({
      eventId: selectedEventId.value,
      ruleId: selectedRuleId.value,
      stageId: selectedStageId.value,
      forceRegenerate: !!eventContext.value?.hasDraftSchedule
    })
    proxy?.$modal.msgSuccess('草稿赛程生成成功')
    generateOpen.value = false
    await handleEventChange()
  } finally {
    generateLoading.value = false
  }
}

async function handlePublish() {
  if (!selectedEventId.value) return
  await proxy?.$modal.confirm('确认发布当前赛事草稿赛程？发布后官网将展示。')
  await publishSchedule({ eventId: selectedEventId.value })
  proxy?.$modal.msgSuccess('发布成功')
  await handleEventChange()
}

async function handleUnpublish() {
  if (!selectedEventId.value) return
  await proxy?.$modal.confirm('确认下架已发布赛程？')
  await unpublishSchedule({ eventId: selectedEventId.value })
  proxy?.$modal.msgSuccess('下架成功')
  await handleEventChange()
}

async function handleDeleteDraft() {
  if (!selectedEventId.value) return
  await proxy?.$modal.confirm('确认删除当前草稿赛程？')
  await deleteScheduleDraft(selectedEventId.value, '管理员删除草稿')
  proxy?.$modal.msgSuccess('删除成功')
  await handleEventChange()
}

function openMatchForm(row?: ScheduleMatchListItem) {
  Object.assign(matchForm, {
    id: row?.id,
    homeEventTeamId: row?.homeEventTeamId,
    awayEventTeamId: row?.awayEventTeamId,
    stageId: row?.stageId || selectedStageId.value,
    roundNo: row?.roundNo || 1,
    matchTime: row?.scheduledStartTime || '',
    matchStatus: row?.matchStatus ?? 1,
    publishStatus: row?.publishStatus ?? 0,
    remark: row?.scheduleRemark || '',
    reason: ''
  })
  matchOpen.value = true
}

function submitMatchForm() {
  proxy?.$refs.matchFormRef.validate(async (valid: boolean) => {
    if (!valid || !selectedEventId.value) return
    if (matchForm.id) {
      await updateScheduleMatch(matchForm.id, {
        stageId: matchForm.stageId,
        roundNo: matchForm.roundNo,
        homeEventTeamId: matchForm.homeEventTeamId,
        awayEventTeamId: matchForm.awayEventTeamId,
        matchTime: matchForm.matchTime,
        matchStatus: matchForm.matchStatus,
        publishStatus: matchForm.publishStatus,
        remark: matchForm.remark,
        reason: matchForm.reason
      })
      proxy?.$modal.msgSuccess('更新成功')
    } else {
      await createScheduleMatch({
        eventId: selectedEventId.value,
        stageId: matchForm.stageId,
        roundNo: matchForm.roundNo,
        homeEventTeamId: matchForm.homeEventTeamId,
        awayEventTeamId: matchForm.awayEventTeamId,
        matchTime: matchForm.matchTime,
        matchStatus: matchForm.matchStatus,
        publishStatus: matchForm.publishStatus,
        remark: matchForm.remark
      })
      proxy?.$modal.msgSuccess('创建成功')
    }
    matchOpen.value = false
    getList()
    if (selectedEventId.value) {
      const ctxRes = await getScheduleEventContext(selectedEventId.value)
      eventContext.value = ctxRes.data
    }
  })
}

function openScoreForm(row: ScheduleMatchListItem) {
  currentScoreMatchId.value = row.id
  scoreForm.homeScore = row.homeScore ?? 0
  scoreForm.awayScore = row.awayScore ?? 0
  scoreForm.reason = ''
  scoreOpen.value = true
}

function submitScore() {
  proxy?.$refs.scoreFormRef.validate(async (valid: boolean) => {
    if (!valid || !currentScoreMatchId.value) return
    if (scoreForm.homeScore === scoreForm.awayScore) {
      proxy?.$modal.msgError('篮球比赛不允许平局')
      return
    }
    await saveScheduleScore(currentScoreMatchId.value, { ...scoreForm })
    proxy?.$modal.msgSuccess('比分保存成功')
    scoreOpen.value = false
    getList()
  })
}

onMounted(async () => {
  await Promise.all([loadTournaments(), loadRules()])
})
</script>
