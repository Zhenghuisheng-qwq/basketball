<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="所属赛事">
        <el-select v-model="filterTournamentId" placeholder="请选择赛事" filterable clearable style="width: 180px" @change="handleFilterTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季" prop="seasonId">
        <el-select v-model="queryParams.seasonId" placeholder="请选择赛季" filterable clearable style="width: 160px" @change="handleFilterSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段" prop="stageId">
        <el-select v-model="queryParams.stageId" placeholder="请选择阶段" filterable clearable style="width: 160px" @change="handleFilterStageChange">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="轮次" prop="roundId">
        <el-select v-model="queryParams.roundId" placeholder="请选择轮次" filterable clearable style="width: 160px" @change="handleFilterRoundChange">
          <el-option v-for="item in roundOptions" :key="item.id" :label="item.roundName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="分组" prop="groupId">
        <el-select v-model="queryParams.groupId" placeholder="请选择分组" filterable clearable style="width: 160px">
          <el-option v-for="item in groupOptions" :key="item.id" :label="item.groupName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="比赛状态" prop="matchStatus">
        <el-select v-model="queryParams.matchStatus" placeholder="比赛状态" clearable style="width: 140px">
          <el-option v-for="item in MATCH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="发布状态" clearable style="width: 140px">
          <el-option v-for="item in MATCH_PUBLISH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队关键词">
        <el-input v-model="queryParams.teamKeyword" placeholder="主/客队名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="比赛时间">
        <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" style="width: 240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="24">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="比赛管理"
          description="查看具体比赛、比赛状态与基础比分，支持编排、延期、开始、取消等比赛生命周期操作。赛程下架后，比赛状态将显示为「已下架」，需重新发布赛程后再操作。"
        />
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:match:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="displayList">
      <el-table-column label="比赛编号" prop="matchNo" min-width="130" />
      <el-table-column label="主队" prop="homeTeamName" min-width="120" />
      <el-table-column label="客队" prop="awayTeamName" min-width="120" />
      <el-table-column label="比分" width="90">
        <template #default="scope">{{ scope.row.homeScore ?? '-' }} : {{ scope.row.awayScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="阶段/轮次" min-width="140">
        <template #default="scope">{{ scope.row.stageName || '-' }} / {{ scope.row.roundName || '-' }}</template>
      </el-table-column>
      <el-table-column label="场馆" prop="venueName" min-width="100" />
      <el-table-column label="计划开始" prop="scheduledStartTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="operationalStatusName" width="100">
        <template #default="scope">
          <el-tag :type="resolveOperationalTagType(scope.row)" disable-transitions>
            {{ scope.row.operationalStatusName || scope.row.matchStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" prop="publishStatusName" width="100" />
      <el-table-column label="操作" align="center" width="420" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:match:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:match:edit']" v-if="scope.row.canEdit">修改</el-button>
          <el-button link type="success" icon="Calendar" @click="openSchedule(scope.row)" v-hasPermi="['basket:match:schedule']" v-if="scope.row.canSchedule">编排</el-button>
          <el-button link type="warning" icon="Timer" @click="openPostpone(scope.row)" v-hasPermi="['basket:match:postpone']" v-if="scope.row.canPostpone">延期</el-button>
          <el-button link type="info" icon="VideoPlay" @click="handleStart(scope.row)" v-hasPermi="['basket:match:start']" v-if="scope.row.canStart">开始</el-button>
          <el-button link type="danger" icon="Close" @click="openCancel(scope.row)" v-hasPermi="['basket:match:cancel']" v-if="scope.row.canCancel">取消</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:match:remove']" v-if="scope.row.canDelete">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赛程详情" size="680px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="比赛编号">{{ detail.matchNo }}</el-descriptions-item>
        <el-descriptions-item label="运营状态">{{ detail.operationalStatusName || detail.matchStatusName }}</el-descriptions-item>
        <el-descriptions-item label="发布状态">{{ detail.publishStatusName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="比赛状态">{{ detail.matchStatusName }}</el-descriptions-item>
        <el-descriptions-item label="主队">{{ detail.homeTeamName }}</el-descriptions-item>
        <el-descriptions-item label="客队">{{ detail.awayTeamName }}</el-descriptions-item>
        <el-descriptions-item label="比分">{{ detail.homeScore ?? '-' }} : {{ detail.awayScore ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据状态">{{ detail.dataStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ detail.stageName }}</el-descriptions-item>
        <el-descriptions-item label="轮次">{{ detail.roundName }}</el-descriptions-item>
        <el-descriptions-item label="分组">{{ detail.groupName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="场馆">{{ detail.venueName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划开始">{{ parseTime(detail.scheduledStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="计划结束">{{ parseTime(detail.scheduledEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="取消原因" :span="2">{{ detail.cancelReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12" v-if="!currentId">
            <el-form-item label="赛季" prop="seasonId">
              <el-select v-model="form.seasonId" placeholder="请选择赛季" filterable style="width: 100%" @change="handleFormSeasonChange">
                <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阶段">
              <el-select v-model="form.stageId" placeholder="请选择阶段" filterable clearable style="width: 100%" @change="handleFormStageChange">
                <el-option v-for="item in formStageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="轮次">
              <el-select v-model="form.roundId" placeholder="请选择轮次" filterable clearable style="width: 100%" @change="handleFormRoundChange">
                <el-option v-for="item in formRoundOptions" :key="item.id" :label="item.roundName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分组">
              <el-select v-model="form.groupId" placeholder="请选择分组" filterable clearable style="width: 100%">
                <el-option v-for="item in formGroupOptions" :key="item.id" :label="item.groupName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主队ID" prop="homeTeamId">
              <el-input-number v-model="form.homeTeamId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客队ID" prop="awayTeamId">
              <el-input-number v-model="form.awayTeamId" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="场馆">
              <el-select v-model="form.venueId" placeholder="请选择场馆" filterable clearable style="width: 100%">
                <el-option v-for="item in venueOptions" :key="item.id" :label="item.venueName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortNo" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划开始">
              <el-date-picker v-model="form.scheduledStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划结束">
              <el-date-picker v-model="form.scheduledEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="比赛编排" v-model="scheduleOpen" width="520px" append-to-body>
      <el-form :model="scheduleForm" label-width="100px">
        <el-form-item label="场馆">
          <el-select v-model="scheduleForm.venueId" placeholder="请选择场馆" filterable clearable style="width: 100%">
            <el-option v-for="item in venueOptions" :key="item.id" :label="item.venueName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="scheduleForm.scheduledStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="scheduleForm.scheduledEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleOpen = false">取消</el-button>
        <el-button type="primary" @click="submitSchedule">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="比赛延期" v-model="postponeOpen" width="520px" append-to-body>
      <el-form :model="postponeForm" label-width="100px">
        <el-form-item label="延期原因">
          <el-input v-model="postponeForm.reason" type="textarea" placeholder="请输入延期原因" />
        </el-form-item>
        <el-form-item label="场馆">
          <el-select v-model="postponeForm.venueId" placeholder="请选择场馆" filterable clearable style="width: 100%">
            <el-option v-for="item in venueOptions" :key="item.id" :label="item.venueName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="postponeForm.scheduledStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="postponeForm.scheduledEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="postponeOpen = false">取消</el-button>
        <el-button type="primary" @click="submitPostpone">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="取消比赛" v-model="cancelOpen" width="480px" append-to-body>
      <el-form :model="cancelForm" label-width="88px">
        <el-form-item label="取消原因">
          <el-input v-model="cancelForm.reason" type="textarea" placeholder="请输入取消原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCancel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketMatch">
import {
  addMatch,
  cancelMatch,
  delMatch,
  getMatch,
  listMatch,
  listMatchVenues,
  postponeMatch,
  scheduleMatch,
  startMatch,
  updateMatch
} from '@/api/basket/matchSchedule'
import { listGroupByStage } from '@/api/basket/tournamentGroup'
import { listRoundByStage } from '@/api/basket/tournamentRound'
import { listStage } from '@/api/basket/tournamentStage'
import { listSeasonByTournament } from '@/api/basket/season'
import { listTournament } from '@/api/basket/tournament'
import type {
  GroupListItem,
  MatchCancelReq,
  MatchCreateReq,
  MatchDetail,
  MatchListItem,
  MatchPostponeReq,
  MatchQuery,
  MatchScheduleReq,
  RoundListItem,
  SeasonListItem,
  StageListItem,
  TournamentListItem,
  VenueSimple
} from '@/types'
import { MATCH_PUBLISH_STATUS_OPTIONS, MATCH_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()
const route = useRoute()

const dataList = ref<MatchListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dateRange = ref<string[]>([])

const filterTournamentId = ref<number>()
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const stageOptions = ref<StageListItem[]>([])
const roundOptions = ref<RoundListItem[]>([])
const groupOptions = ref<GroupListItem[]>([])
const venueOptions = ref<VenueSimple[]>([])

const formSeasonOptions = ref<SeasonListItem[]>([])
const formStageOptions = ref<StageListItem[]>([])
const formRoundOptions = ref<RoundListItem[]>([])
const formGroupOptions = ref<GroupListItem[]>([])

const detailOpen = ref(false)
const detail = ref<MatchDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()

const scheduleOpen = ref(false)
const postponeOpen = ref(false)
const cancelOpen = ref(false)
const scheduleForm = ref<MatchScheduleReq>({})
const postponeForm = ref<MatchPostponeReq>({})
const cancelForm = ref<MatchCancelReq>({})

const queryParams = ref<MatchQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<MatchCreateReq>({
  seasonId: undefined as unknown as number,
  homeTeamId: undefined as unknown as number,
  awayTeamId: undefined as unknown as number,
  publicStatus: 1,
  sortNo: 0
})

const rules = {
  seasonId: [{ required: true, message: '请选择赛季', trigger: 'change' }],
  homeTeamId: [{ required: true, message: '请输入主队ID', trigger: 'blur' }],
  awayTeamId: [{ required: true, message: '请输入客队ID', trigger: 'blur' }]
}

const displayList = computed(() => {
  const keyword = queryParams.value.teamKeyword?.trim().toLowerCase()
  if (!keyword) return dataList.value
  return dataList.value.filter(row =>
    (row.homeTeamName || '').toLowerCase().includes(keyword) ||
    (row.awayTeamName || '').toLowerCase().includes(keyword)
  )
})

function resolveOperationalTagType(row: MatchListItem) {
  const label = row.operationalStatusName || row.matchStatusName
  if (label === '已下架') return 'warning'
  if (label === '已取消') return 'info'
  if (label === '进行中') return 'success'
  if (label === '已结束') return 'success'
  return ''
}

function loadVenues() {
  listMatchVenues().then(res => {
    venueOptions.value = res.data || []
  })
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows
  })
}

function handleFilterTournamentChange(val?: number) {
  queryParams.value.seasonId = undefined
  queryParams.value.stageId = undefined
  queryParams.value.roundId = undefined
  queryParams.value.groupId = undefined
  stageOptions.value = []
  roundOptions.value = []
  groupOptions.value = []
  if (!val) {
    seasonOptions.value = []
    return
  }
  listSeasonByTournament(val).then(res => {
    seasonOptions.value = res.data || []
  })
}

function handleFilterSeasonChange(val?: number) {
  queryParams.value.stageId = undefined
  queryParams.value.roundId = undefined
  queryParams.value.groupId = undefined
  roundOptions.value = []
  groupOptions.value = []
  if (!val) {
    stageOptions.value = []
    return
  }
  listStage({ pageNum: 1, pageSize: 500, seasonId: val }).then(res => {
    stageOptions.value = res.rows
  })
}

function handleFilterStageChange(val?: number) {
  queryParams.value.roundId = undefined
  queryParams.value.groupId = undefined
  groupOptions.value = []
  if (!val) {
    roundOptions.value = []
    return
  }
  listRoundByStage(val).then(res => {
    roundOptions.value = res.data || []
  })
  listGroupByStage(val).then(res => {
    groupOptions.value = res.data || []
  })
}

function handleFilterRoundChange(val?: number) {
  if (!val || queryParams.value.stageId) return
}

function getList() {
  loading.value = true
  const params = proxy.addDateRange({ ...queryParams.value }, dateRange.value)
  listMatch(params).then(response => {
    dataList.value = response.rows
    total.value = response.total
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  filterTournamentId.value = undefined
  seasonOptions.value = []
  stageOptions.value = []
  roundOptions.value = []
  groupOptions.value = []
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function resetFormData() {
  form.value = {
    seasonId: queryParams.value.seasonId || (undefined as unknown as number),
    stageId: queryParams.value.stageId,
    roundId: queryParams.value.roundId,
    groupId: queryParams.value.groupId,
    venueId: undefined,
    homeTeamId: undefined as unknown as number,
    awayTeamId: undefined as unknown as number,
    scheduledStartTime: '',
    scheduledEndTime: '',
    sortNo: 0,
    publicStatus: 1
  }
  proxy.resetForm('formRef')
}

async function loadFormCascadeFromSeason(seasonId?: number) {
  formStageOptions.value = []
  formRoundOptions.value = []
  formGroupOptions.value = []
  if (!seasonId) return
  const stageRes = await listStage({ pageNum: 1, pageSize: 500, seasonId })
  formStageOptions.value = stageRes.rows
}

async function handleFormSeasonChange(seasonId?: number) {
  form.value.stageId = undefined
  form.value.roundId = undefined
  form.value.groupId = undefined
  await loadFormCascadeFromSeason(seasonId)
}

async function handleFormStageChange(stageId?: number) {
  form.value.roundId = undefined
  form.value.groupId = undefined
  formRoundOptions.value = []
  formGroupOptions.value = []
  if (!stageId) return
  const [roundRes, groupRes] = await Promise.all([
    listRoundByStage(stageId),
    listGroupByStage(stageId)
  ])
  formRoundOptions.value = roundRes.data || []
  formGroupOptions.value = groupRes.data || []
}

function handleFormRoundChange() {}

async function loadFormSeasonOptions() {
  if (filterTournamentId.value) {
    const res = await listSeasonByTournament(filterTournamentId.value)
    formSeasonOptions.value = res.data || []
    return
  }
  if (tournamentOptions.value.length === 0) return
  const res = await listSeasonByTournament(tournamentOptions.value[0].id)
  formSeasonOptions.value = res.data || []
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增赛程'
  loadFormSeasonOptions().then(() => {
    if (form.value.seasonId) {
      loadFormCascadeFromSeason(form.value.seasonId).then(() => {
        if (form.value.stageId) handleFormStageChange(form.value.stageId)
      })
    }
  })
  dialogOpen.value = true
}

function handleUpdate(row: MatchListItem) {
  resetFormData()
  currentId.value = row.id
  getMatch(row.id).then(async res => {
    const data = res.data!
    form.value = {
      seasonId: data.seasonId!,
      stageId: data.stageId,
      roundId: data.roundId,
      groupId: data.groupId,
      venueId: data.venueId,
      homeTeamId: data.homeTeamId!,
      awayTeamId: data.awayTeamId!,
      scheduledStartTime: data.scheduledStartTime,
      scheduledEndTime: data.scheduledEndTime,
      sortNo: data.sortNo ?? 0,
      publicStatus: data.publicStatus ?? 1
    }
    await loadFormCascadeFromSeason(data.seasonId)
    if (data.stageId) await handleFormStageChange(data.stageId)
    dialogTitle.value = '修改赛程'
    dialogOpen.value = true
  })
}

function handleDetail(row: MatchListItem) {
  getMatch(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateMatch(currentId.value, form.value)
      : addMatch(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function openSchedule(row: MatchListItem) {
  currentId.value = row.id
  scheduleForm.value = {
    venueId: row.venueId,
    scheduledStartTime: row.scheduledStartTime,
    scheduledEndTime: row.scheduledEndTime
  }
  scheduleOpen.value = true
}

function submitSchedule() {
  if (!currentId.value) return
  scheduleMatch(currentId.value, scheduleForm.value).then(() => {
    proxy.$modal.msgSuccess('编排成功')
    scheduleOpen.value = false
    getList()
  })
}

function openPostpone(row: MatchListItem) {
  currentId.value = row.id
  postponeForm.value = {
    venueId: row.venueId,
    scheduledStartTime: row.scheduledStartTime,
    scheduledEndTime: row.scheduledEndTime
  }
  postponeOpen.value = true
}

function submitPostpone() {
  if (!currentId.value) return
  postponeMatch(currentId.value, postponeForm.value).then(() => {
    proxy.$modal.msgSuccess('延期成功')
    postponeOpen.value = false
    getList()
  })
}

function openCancel(row: MatchListItem) {
  currentId.value = row.id
  cancelForm.value = { reason: '' }
  cancelOpen.value = true
}

function submitCancel() {
  if (!currentId.value) return
  cancelMatch(currentId.value, cancelForm.value).then(() => {
    proxy.$modal.msgSuccess('已取消')
    cancelOpen.value = false
    getList()
  })
}

function handleStart(row: MatchListItem) {
  proxy.$modal.confirm(`确认开始比赛「${row.matchNo}」？`).then(() => startMatch(row.id)).then(() => {
    proxy.$modal.msgSuccess('比赛已开始')
    getList()
  }).catch(() => {})
}

function handleDelete(row: MatchListItem) {
  proxy.$modal.confirm(`确认删除比赛「${row.matchNo}」？`).then(() => delMatch(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

loadTournaments()
loadVenues()
if (route.query.seasonId) {
  queryParams.value.seasonId = Number(route.query.seasonId)
}
getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
