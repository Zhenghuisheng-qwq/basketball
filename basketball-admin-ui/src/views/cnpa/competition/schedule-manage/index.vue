<template>
  <div class="app-container">
    <el-alert type="warning" title="旧域已废弃" description="请使用「赛程管理」菜单。本页面调用旧 Schedule Service，R4 起不再维护。" :closable="false" show-icon class="mb8" />
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="赛程管理"
      description="基于正式赛事球队（参赛中且阵容已锁定）生成循环赛草稿赛程，管理员确认发布后前台展示。取消发布后可重新生成草稿；已结束或有数据的历史比赛会保留。"
    />

    <el-alert
      v-if="eventContext?.hasLegacyUnpublishedMatches"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
      title="检测到未发布的历史比赛数据"
      :description="legacyAlertDescription"
    />

    <el-card shadow="never" class="mb8">
      <template #header><span>赛事与规则</span></template>
      <el-form :inline="true" label-width="88px">
        <el-form-item label="所属赛事">
          <el-select v-model="filterTournamentId" placeholder="请选择赛事" filterable clearable style="width: 180px" @change="handleTournamentChange">
            <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛季">
          <el-select v-model="selectedEventId" placeholder="请选择赛季" filterable clearable style="width: 180px" @change="handleEventChange">
            <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select v-model="selectedStageId" placeholder="请选择阶段" filterable clearable style="width: 160px">
            <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛程规则">
          <el-select v-model="selectedRuleId" placeholder="请选择规则" filterable style="width: 220px" @change="loadRuleDetail">
            <el-option v-for="item in ruleOptions" :key="item.id" :label="`${item.ruleName} (${item.ruleVersion})`" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="eventContext" title="参赛球队概览" :column="4" border size="small" class="mb8">
        <el-descriptions-item label="正式参赛球队">{{ eventContext.eventTeamCount ?? 0 }} 支</el-descriptions-item>
        <el-descriptions-item label="阵容已锁定">{{ eventContext.lockedTeamCount ?? 0 }} 支</el-descriptions-item>
        <el-descriptions-item label="游戏端">{{ eventContext.platformDisplayName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前赛程">{{ eventContext.scheduleStatusText || '未生成' }}</el-descriptions-item>
        <el-descriptions-item label="预计轮次">{{ eventContext.expectedRoundCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="预计比赛数">{{ eventContext.expectedMatchCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="可清理赛程">{{ eventContext.deletableDraftMatchCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="可生成">{{ eventContext.canGenerateSchedule ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>

      <el-space wrap>
        <el-button
          v-if="!eventContext?.hasPublishedSchedule"
          type="primary"
          :disabled="!canGenerate"
          @click="openGenerateConfirm"
          v-hasPermi="['basket:schedule:generate']"
        >{{ generateButtonLabel }}</el-button>
        <el-button
          v-if="!eventContext?.hasPublishedSchedule"
          type="success"
          :disabled="!selectedEventId || !hasDraftContent"
          @click="handlePublish"
          v-hasPermi="['basket:schedule:publish']"
        >发布赛程</el-button>
        <el-button
          v-if="eventContext?.hasPublishedSchedule"
          type="warning"
          @click="handleUnpublish"
          v-hasPermi="['basket:schedule:publish']"
        >取消发布</el-button>
        <el-button
          v-if="!eventContext?.hasPublishedSchedule"
          type="danger"
          :disabled="!canDeleteDraft"
          @click="handleDeleteDraft"
          v-hasPermi="['basket:schedule:remove']"
        >删除草稿</el-button>
        <el-button
          v-if="!eventContext?.hasPublishedSchedule"
          type="primary"
          plain
          icon="Plus"
          :disabled="!selectedEventId"
          @click="openMatchForm()"
          v-hasPermi="['basket:schedule:add']"
        >新增草稿比赛</el-button>
      </el-space>
    </el-card>

    <el-form :model="queryParams" :inline="true" v-show="showSearch" label-width="88px" class="mb8">
      <el-form-item label="阶段">
        <el-select v-model="queryParams.stageId" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="游戏端">
        <el-input v-model="queryParams.platformCode" placeholder="平台编码" clearable style="width: 120px" />
      </el-form-item>
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
      <el-form-item label="数据范围">
        <el-select v-model="listScope" style="width: 160px" @change="handleScopeChange">
          <el-option label="有效赛程" value="valid" />
          <el-option label="含历史脏数据" value="all" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期范围">
        <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
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
      <el-table-column label="比赛日期" width="120">
        <template #default="scope">{{ formatMatchDate(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="星期" width="80">
        <template #default="scope">{{ formatWeekday(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="时间" width="80">
        <template #default="scope">{{ formatMatchTime(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="主队" min-width="140">
        <template #default="scope">{{ displayTeamName(scope.row.homeTeamName, scope.row.dataAbnormal) }}</template>
      </el-table-column>
      <el-table-column label="客队" min-width="140">
        <template #default="scope">{{ displayTeamName(scope.row.awayTeamName, scope.row.dataAbnormal) }}</template>
      </el-table-column>
      <el-table-column label="比赛状态" prop="matchStatusName" width="100" />
      <el-table-column label="发布状态" prop="publishStatusName" width="100" />
      <el-table-column label="操作" align="center" width="320" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['basket:schedule:query']">详情</el-button>
          <el-button link type="primary" @click="openMatchForm(scope.row)" v-if="scope.row.canEdit" v-hasPermi="['basket:schedule:edit']">编辑</el-button>
          <el-button link type="warning" @click="handleSwap(scope.row)" v-if="scope.row.canSwap" v-hasPermi="['basket:schedule:edit']">主客互换</el-button>
          <el-button link type="danger" @click="handleDeleteMatch(scope.row)" v-if="scope.row.canDelete" v-hasPermi="['basket:schedule:delete']">删除</el-button>
          <el-button link type="success" @click="openScoreForm(scope.row)" v-if="scope.row.canScore" v-hasPermi="['basket:schedule:score']">录入比分</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="generateOpen" title="确认生成草稿赛程" width="560px" append-to-body>
      <el-descriptions v-if="generatePreview" :column="1" border>
        <el-descriptions-item label="赛事">{{ generatePreview.tournamentName }} / {{ generatePreview.seasonName }}</el-descriptions-item>
        <el-descriptions-item label="游戏端">{{ generatePreview.platformCode }}</el-descriptions-item>
        <el-descriptions-item label="正式参赛球队">{{ generatePreview.eventTeamCount }} 支（阵容已锁定 {{ generatePreview.lockedTeamCount }} 支）</el-descriptions-item>
        <el-descriptions-item label="预计轮次">{{ generatePreview.expectedRoundCount }}</el-descriptions-item>
        <el-descriptions-item label="预计比赛数">{{ generatePreview.expectedMatchCount }}</el-descriptions-item>
        <el-descriptions-item label="规则">{{ generatePreview.ruleName }} ({{ generatePreview.ruleVersion }})</el-descriptions-item>
      </el-descriptions>
      <el-alert
        v-if="hasDraftContent || eventContext?.hasLegacyUnpublishedMatches"
        class="mt8"
        type="warning"
        :closable="false"
        title="覆盖生成将删除当前可清理草稿后重建；已结束/已取消历史比赛不会被删除。"
      />
      <template #footer>
        <el-button @click="generateOpen = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="confirmGenerate">确认生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="比赛详情" size="560px" append-to-body>
      <el-descriptions v-if="matchDetail" :column="1" border>
        <el-descriptions-item label="比赛编号">{{ matchDetail.matchNo }}</el-descriptions-item>
        <el-descriptions-item label="轮次">{{ matchDetail.roundNo }}</el-descriptions-item>
        <el-descriptions-item label="比赛时间">{{ parseTime(matchDetail.scheduledStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="主队">{{ displayTeamName(matchDetail.homeTeamName, matchDetail.dataAbnormal) }}</el-descriptions-item>
        <el-descriptions-item label="客队">{{ displayTeamName(matchDetail.awayTeamName, matchDetail.dataAbnormal) }}</el-descriptions-item>
        <el-descriptions-item label="比分">{{ matchDetail.homeScore ?? '-' }} : {{ matchDetail.awayScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="比赛状态">{{ matchDetail.matchStatusName }}</el-descriptions-item>
        <el-descriptions-item label="发布状态">{{ matchDetail.publishStatusName }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ matchDetail.scheduleRemark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="matchOpen" :title="matchForm.id ? '编辑草稿比赛' : '新增草稿比赛'" width="560px" append-to-body>
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
        <el-form-item label="比赛日期" prop="matchDate">
          <el-date-picker
            v-model="matchForm.matchDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="周二/周四/周六"
            style="width: 100%"
            :disabled-date="disableNonMatchWeekday"
          />
        </el-form-item>
        <el-form-item label="比赛时间" prop="matchSlot">
          <el-select v-model="matchForm.matchSlot" placeholder="21:00 或 22:00" style="width: 100%">
            <el-option v-for="item in TIME_SLOT_OPTIONS" :key="item" :label="item" :value="item" />
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

<script setup lang="ts" name="BasketScheduleManage">
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { listTournament } from '@/api/basket/tournament'
import { listSeasonByTournament } from '@/api/basket/season'
import { listStage } from '@/api/basket/tournamentStage'
import { listEventTeam } from '@/api/basket/eventTeam'
import {
  createScheduleMatch,
  deleteScheduleDraft,
  deleteScheduleMatch,
  generateScheduleDraft,
  getScheduleEventContext,
  getScheduleMatchDetail,
  getScheduleRuleDetail,
  listScheduleMatch,
  listScheduleRule,
  previewScheduleGenerate,
  publishSchedule,
  saveScheduleScore,
  swapScheduleMatch,
  unpublishSchedule,
  updateScheduleMatch
} from '@/api/basket/scheduleManage'
import type {
  EventTeamListItem,
  ScheduleEventContext,
  ScheduleGeneratePreview,
  ScheduleMatchDetailItem,
  ScheduleMatchListItem,
  ScheduleRuleDetail,
  ScheduleRuleListItem,
  SeasonListItem,
  StageListItem,
  TournamentListItem
} from '@/types'

const { proxy } = getCurrentInstance()!
const WEEKDAY_LABELS = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
const ALLOWED_WEEKDAYS = [2, 4, 6]
const TIME_SLOT_OPTIONS = ['21:00', '22:00']
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
const dateRange = ref<string[]>([])
const listScope = ref<'valid' | 'all'>('valid')

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
const detailOpen = ref(false)
const matchDetail = ref<ScheduleMatchDetailItem>()
const currentScoreMatchId = ref<number>()

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  seasonId: undefined as number | undefined,
  stageId: undefined as number | undefined,
  platformCode: undefined as string | undefined,
  matchStatus: undefined as number | undefined,
  publishStatus: undefined as number | undefined,
  scheduleBatchId: undefined as number | undefined,
  excludeLegacy: true as boolean | undefined,
  beginTime: undefined as string | undefined,
  endTime: undefined as string | undefined
})

const matchForm = reactive<any>({
  id: undefined,
  homeEventTeamId: undefined,
  awayEventTeamId: undefined,
  stageId: undefined,
  roundNo: 1,
  matchDate: '',
  matchSlot: '21:00',
  remark: '',
  reason: ''
})

const scoreForm = reactive({ homeScore: 0, awayScore: 0, reason: '' })

const matchRules = {
  homeEventTeamId: [{ required: true, message: '请选择主队', trigger: 'change' }],
  awayEventTeamId: [{ required: true, message: '请选择客队', trigger: 'change' }],
  stageId: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  matchDate: [{ required: true, message: '请选择比赛日期', trigger: 'change' }],
  matchSlot: [{ required: true, message: '请选择比赛时间', trigger: 'change' }]
}

const scoreRules = {
  homeScore: [{ required: true, message: '请输入主队比分', trigger: 'blur' }],
  awayScore: [{ required: true, message: '请输入客队比分', trigger: 'blur' }]
}

const canGenerate = computed(() =>
  !!selectedEventId.value && !!selectedRuleId.value && !!eventContext.value?.canGenerateSchedule
)

/** 正式批次草稿/已下架，或可清理的草稿/下架比赛 */
const hasDraftContent = computed(() => {
  if (!eventContext.value) return false
  if (eventContext.value.hasDraftSchedule) return true
  return (eventContext.value.deletableDraftMatchCount ?? 0) > 0
})

const canDeleteDraft = computed(() =>
  !!eventContext.value?.canDeleteRegeneratableDraft || hasDraftContent.value
)

const isUnpublishedSchedule = computed(() => eventContext.value?.latestBatchPublishStatus === 2)

const generateButtonLabel = computed(() =>
  isUnpublishedSchedule.value || hasDraftContent.value ? '重新生成草稿赛程' : '生成草稿赛程'
)

const legacyAlertDescription = computed(() => {
  const count = eventContext.value?.legacyUnpublishedMatchCount ?? 0
  return `当前赛季有 ${count} 场未发布历史比赛（多为已结束/已取消或缺少 EventTeam）。默认列表仅显示有效赛程；可切换「含历史脏数据」查看。删除草稿不会误删这些历史比赛，覆盖生成也不会删除它们。`
})

function parseTime(val?: string) {
  return proxy?.parseTime ? proxy.parseTime(val) : val
}

function formatMatchDate(val?: string) {
  if (!val) return '-'
  return val.substring(0, 10)
}

function formatMatchTime(val?: string) {
  if (!val || val.length < 16) return '-'
  return val.substring(11, 16)
}

function formatWeekday(val?: string) {
  if (!val) return '-'
  const date = new Date(val.replace(/-/g, '/'))
  return WEEKDAY_LABELS[date.getDay() === 0 ? 7 : date.getDay()] || '-'
}

function displayTeamName(name?: string, dataAbnormal?: number) {
  if (name && name.trim()) return name
  if (dataAbnormal === 1) return '历史数据异常'
  return '历史数据异常'
}

function disableNonMatchWeekday(date: Date) {
  const day = date.getDay() === 0 ? 7 : date.getDay()
  return !ALLOWED_WEEKDAYS.includes(day)
}

function buildMatchTime(date?: string, slot?: string) {
  if (!date || !slot) return ''
  return `${date} ${slot}:00`
}

function splitMatchTime(val?: string) {
  if (!val) return { matchDate: '', matchSlot: '21:00' }
  const slot = val.substring(11, 16)
  return {
    matchDate: val.substring(0, 10),
    matchSlot: TIME_SLOT_OPTIONS.includes(slot) ? slot : '21:00'
  }
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
    listEventTeam({ pageNum: 1, pageSize: 500, seasonId: selectedEventId.value, participationStatus: 1, rosterLockStatus: 1 })
  ])
  eventContext.value = ctxRes.data
  stageOptions.value = stageRes.rows || []
  eventTeamOptions.value = teamRes.rows || []
  if (!selectedStageId.value && stageOptions.value.length) {
    selectedStageId.value = stageOptions.value[0].id
  }
  applyDefaultListScope()
  getList()
}

function applyDefaultListScope() {
  listScope.value = 'valid'
  queryParams.excludeLegacy = true
  queryParams.scheduleBatchId = eventContext.value?.latestBatchId
  if (eventContext.value?.hasPublishedSchedule) {
    queryParams.publishStatus = 1
  } else if (eventContext.value?.latestBatchPublishStatus === 2) {
    queryParams.publishStatus = 2
  } else if (eventContext.value?.hasDraftSchedule || (eventContext.value?.deletableDraftMatchCount ?? 0) > 0) {
    queryParams.publishStatus = 0
  } else {
    queryParams.publishStatus = undefined
  }
}

function handleScopeChange() {
  if (listScope.value === 'valid') {
    queryParams.excludeLegacy = true
    queryParams.scheduleBatchId = eventContext.value?.latestBatchId
  } else {
    queryParams.excludeLegacy = false
    queryParams.scheduleBatchId = undefined
  }
  handleQuery()
}

async function loadRules() {
  const res = await listScheduleRule()
  ruleOptions.value = res.data || []
  const v2 = ruleOptions.value.find(item => item.ruleVersion === 'V2')
  selectedRuleId.value = v2?.id || ruleOptions.value[0]?.id
  if (selectedRuleId.value) loadRuleDetail()
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
  queryParams.beginTime = dateRange.value?.[0] ? `${dateRange.value[0]} 00:00:00` : undefined
  queryParams.endTime = dateRange.value?.[1] ? `${dateRange.value[1]} 23:59:59` : undefined
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
  queryParams.stageId = undefined
  queryParams.matchStatus = undefined
  dateRange.value = []
  applyDefaultListScope()
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
  if (isUnpublishedSchedule.value || eventContext.value?.scheduleStatusText === '已下架') {
    await proxy?.$modal.confirm('当前赛程已下架，重新生成将清理可重置的下架赛程并生成新草稿（已结束/有数据的历史比赛会保留），是否继续？')
  } else if (hasDraftContent.value) {
    await proxy?.$modal.confirm('当前已存在草稿赛程，重新生成将删除可清理草稿后重建（历史已结束/已取消比赛不会删除），是否继续？')
  } else if (eventContext.value?.hasLegacyUnpublishedMatches) {
    await proxy?.$modal.confirm('当前存在未发布历史脏数据，将生成新的正式草稿赛程（不会删除历史脏数据），是否继续？')
  }
  generateLoading.value = true
  try {
    await generateScheduleDraft({
      eventId: selectedEventId.value,
      ruleId: selectedRuleId.value,
      stageId: selectedStageId.value,
      forceRegenerate: hasDraftContent.value || isUnpublishedSchedule.value
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
  await proxy?.$modal.confirm('确认取消发布已发布赛程？')
  await unpublishSchedule({ eventId: selectedEventId.value })
  proxy?.$modal.msgSuccess('已取消发布')
  await handleEventChange()
}

async function handleDeleteDraft() {
  if (!selectedEventId.value) return
  await proxy?.$modal.confirm('确认删除当前可清理草稿赛程？已结束/已取消历史比赛不会被删除。')
  await deleteScheduleDraft(selectedEventId.value, '管理员删除草稿')
  proxy?.$modal.msgSuccess('删除成功')
  await handleEventChange()
}

async function openDetail(row: ScheduleMatchListItem) {
  const res = await getScheduleMatchDetail(row.id)
  matchDetail.value = res.data
  detailOpen.value = true
}

function openMatchForm(row?: ScheduleMatchListItem) {
  const timeParts = splitMatchTime(row?.scheduledStartTime)
  Object.assign(matchForm, {
    id: row?.id,
    homeEventTeamId: row?.homeEventTeamId,
    awayEventTeamId: row?.awayEventTeamId,
    stageId: row?.stageId || selectedStageId.value,
    roundNo: row?.roundNo || 1,
    matchDate: timeParts.matchDate,
    matchSlot: timeParts.matchSlot,
    remark: row?.scheduleRemark || '',
    reason: ''
  })
  matchOpen.value = true
}

function submitMatchForm() {
  proxy?.$refs.matchFormRef.validate(async (valid: boolean) => {
    if (!valid || !selectedEventId.value) return
    const matchTime = buildMatchTime(matchForm.matchDate, matchForm.matchSlot)
    if (matchForm.id) {
      await updateScheduleMatch(matchForm.id, {
        stageId: matchForm.stageId,
        roundNo: matchForm.roundNo,
        homeEventTeamId: matchForm.homeEventTeamId,
        awayEventTeamId: matchForm.awayEventTeamId,
        matchTime,
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
        matchTime,
        publishStatus: 0,
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

async function handleSwap(row: ScheduleMatchListItem) {
  await proxy?.$modal.confirm(`确认互换 ${row.homeTeamName} 与 ${row.awayTeamName} 的主客队？`)
  await swapScheduleMatch(row.id, '管理员主客队互换')
  proxy?.$modal.msgSuccess('互换成功')
  getList()
}

async function handleDeleteMatch(row: ScheduleMatchListItem) {
  await proxy?.$modal.confirm('确认删除该草稿比赛？')
  await deleteScheduleMatch(row.id, '管理员删除草稿比赛')
  proxy?.$modal.msgSuccess('删除成功')
  getList()
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

<style scoped>
.mt8 {
  margin-top: 8px;
}
</style>
