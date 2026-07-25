<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="所属赛事">
        <el-select v-model="filterTournamentId" placeholder="请选择赛事" filterable clearable style="width: 200px" @change="handleTournamentFilterChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属赛季" prop="seasonId">
        <el-select v-model="queryParams.seasonId" placeholder="请选择赛季" filterable clearable style="width: 200px" @change="handleQuery">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段名称" prop="stageName">
        <el-input v-model="queryParams.stageName" placeholder="请输入阶段名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="stageStatus">
        <el-select v-model="queryParams.stageStatus" placeholder="阶段状态" clearable style="width: 200px">
          <el-option v-for="item in STAGE_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:stage:add']" :disabled="!queryParams.seasonId">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="阶段编号" prop="stageNo" min-width="130" />
      <el-table-column label="阶段名称" prop="stageName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="赛事" prop="tournamentName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="赛季" prop="seasonName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="类型" prop="stageType" width="90" />
      <el-table-column label="状态" prop="stageStatus" width="100">
        <template #default="scope">
          <dict-tag :options="STAGE_STATUS_OPTIONS" :value="scope.row.stageStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sequenceNo" width="70" />
      <el-table-column label="开始时间" prop="startTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="380" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:stage:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:stage:edit']">修改</el-button>
          <el-button link type="primary" icon="List" @click="openRoundDrawer(scope.row)" v-hasPermi="['basket:round:list']">轮次</el-button>
          <el-button link type="success" icon="VideoPlay" @click="handleActivate(scope.row)" v-hasPermi="['basket:stage:status']" v-if="scope.row.stageStatus === 0">激活</el-button>
          <el-button link type="info" icon="CircleCheck" @click="handleFinish(scope.row)" v-hasPermi="['basket:stage:status']" v-if="scope.row.stageStatus === 1">结束</el-button>
          <el-button link type="warning" icon="CircleClose" @click="handleCancel(scope.row)" v-hasPermi="['basket:stage:status']" v-if="[0, 1].includes(scope.row.stageStatus)">取消</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:stage:remove']" v-if="scope.row.stageStatus === 0">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赛事阶段详情" size="640px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="阶段编号">{{ detail.stageNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.stageStatusName }}</el-descriptions-item>
        <el-descriptions-item label="阶段名称">{{ detail.stageName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.stageType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赛制">{{ detail.formatType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detail.sequenceNo }}</el-descriptions-item>
        <el-descriptions-item label="赛事">{{ detail.tournamentName }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ parseTime(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ parseTime(detail.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="晋级规则" :span="2">{{ detail.qualificationRuleText || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-drawer v-model="roundDrawerOpen" :title="`轮次管理 - ${currentStage?.stageName || ''}`" size="860px" append-to-body>
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleRoundAdd" v-hasPermi="['basket:round:add']">新增轮次</el-button>
        </el-col>
      </el-row>
      <el-table v-loading="roundLoading" :data="roundList" size="small">
        <el-table-column label="轮次编号" prop="roundNo" min-width="120" />
        <el-table-column label="轮次名称" prop="roundName" min-width="120" />
        <el-table-column label="类型" prop="roundType" width="90" />
        <el-table-column label="状态" prop="roundStatus" width="100">
          <template #default="scope">
            <dict-tag :options="ROUND_STATUS_OPTIONS" :value="scope.row.roundStatus" />
          </template>
        </el-table-column>
        <el-table-column label="排序" prop="sequenceNo" width="70" />
        <el-table-column label="操作" align="center" width="320">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleRoundUpdate(scope.row)" v-hasPermi="['basket:round:edit']">修改</el-button>
            <el-button link type="success" icon="VideoPlay" @click="handleRoundActivate(scope.row)" v-hasPermi="['basket:round:status']" v-if="scope.row.roundStatus === 0">激活</el-button>
            <el-button link type="info" icon="CircleCheck" @click="handleRoundFinish(scope.row)" v-hasPermi="['basket:round:status']" v-if="[1, 2].includes(scope.row.roundStatus)">结束</el-button>
            <el-button link type="warning" icon="CircleClose" @click="handleRoundCancel(scope.row)" v-hasPermi="['basket:round:status']" v-if="[0, 1, 2].includes(scope.row.roundStatus)">取消</el-button>
            <el-button link type="danger" icon="Delete" @click="handleRoundDelete(scope.row)" v-hasPermi="['basket:round:remove']" v-if="scope.row.roundStatus === 0">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12" v-if="!currentId">
            <el-form-item label="所属赛季" prop="seasonId">
              <el-select v-model="form.seasonId" placeholder="请选择赛季" filterable style="width: 100%">
                <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阶段名称" prop="stageName">
              <el-input v-model="form.stageName" placeholder="请输入阶段名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="阶段类型">
              <el-input v-model="form.stageType" placeholder="请输入阶段类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛制">
              <el-input v-model="form.formatType" placeholder="请输入赛制" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sequenceNo" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="公开状态">
              <el-radio-group v-model="form.publicStatus">
                <el-radio :value="1">公开</el-radio>
                <el-radio :value="0">不公开</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="晋级规则">
              <el-input v-model="form.qualificationRuleText" type="textarea" placeholder="请输入晋级规则" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog :title="roundDialogTitle" v-model="roundDialogOpen" width="600px" append-to-body>
      <el-form ref="roundFormRef" :model="roundForm" :rules="roundRules" label-width="100px">
        <el-form-item label="轮次名称" prop="roundName">
          <el-input v-model="roundForm.roundName" placeholder="请输入轮次名称" />
        </el-form-item>
        <el-form-item label="轮次类型">
          <el-input v-model="roundForm.roundType" placeholder="请输入轮次类型" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="roundForm.sequenceNo" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="场次(Bo)">
          <el-input-number v-model="roundForm.bestOf" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="roundForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="roundForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roundDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitRoundForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketStage">
import {
  activateStage,
  addStage,
  cancelStage,
  delStage,
  finishStage,
  getStage,
  listStage,
  updateStage
} from '@/api/basket/tournamentStage'
import {
  activateRound,
  addRound,
  cancelRound,
  delRound,
  finishRound,
  getRound,
  listRoundByStage,
  updateRound
} from '@/api/basket/tournamentRound'
import { listSeasonByTournament } from '@/api/basket/season'
import { listTournament } from '@/api/basket/tournament'
import type {
  RoundCreateReq,
  RoundListItem,
  SeasonListItem,
  StageCreateReq,
  StageDetail,
  StageListItem,
  StageQuery,
  TournamentListItem
} from '@/types'
import { ROUND_STATUS_OPTIONS, STAGE_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<StageListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<StageDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const filterTournamentId = ref<number>()
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const formSeasonOptions = ref<SeasonListItem[]>([])

const roundDrawerOpen = ref(false)
const roundLoading = ref(false)
const roundList = ref<RoundListItem[]>([])
const currentStage = ref<StageListItem>()
const roundDialogOpen = ref(false)
const roundDialogTitle = ref('')
const currentRoundId = ref<number>()

const queryParams = ref<StageQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<StageCreateReq>({
  seasonId: undefined as unknown as number,
  stageName: '',
  publicStatus: 1,
  sequenceNo: 0
})

const roundForm = ref<RoundCreateReq>({
  roundName: '',
  bestOf: 1,
  sequenceNo: 0
})

const rules = {
  seasonId: [{ required: true, message: '请选择赛季', trigger: 'change' }],
  stageName: [{ required: true, message: '阶段名称不能为空', trigger: 'blur' }]
}

const roundRules = {
  roundName: [{ required: true, message: '轮次名称不能为空', trigger: 'blur' }]
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows
  })
}

function loadSeasonOptions(tournamentId?: number) {
  if (!tournamentId) {
    seasonOptions.value = []
    return Promise.resolve()
  }
  return listSeasonByTournament(tournamentId).then(res => {
    seasonOptions.value = res.data || []
  })
}

function handleTournamentFilterChange(val?: number) {
  queryParams.value.seasonId = undefined
  loadSeasonOptions(val).then(() => handleQuery())
}

function getList() {
  loading.value = true
  listStage(queryParams.value).then(response => {
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
  proxy.resetForm('queryRef')
  handleQuery()
}

function resetFormData() {
  form.value = {
    seasonId: queryParams.value.seasonId || (undefined as unknown as number),
    stageName: '',
    stageType: '',
    formatType: '',
    startTime: '',
    endTime: '',
    sequenceNo: 0,
    qualificationRuleText: '',
    publicStatus: 1
  }
  proxy.resetForm('formRef')
}

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
  dialogTitle.value = '新增赛事阶段'
  loadFormSeasonOptions()
  dialogOpen.value = true
}

function handleUpdate(row: StageListItem) {
  resetFormData()
  currentId.value = row.id
  getStage(row.id).then(res => {
    const data = res.data!
    form.value = {
      seasonId: data.seasonId!,
      stageName: data.stageName || '',
      stageType: data.stageType,
      formatType: data.formatType,
      startTime: data.startTime,
      endTime: data.endTime,
      sequenceNo: data.sequenceNo ?? 0,
      qualificationRuleText: data.qualificationRuleText,
      publicStatus: data.publicStatus ?? 1
    }
    dialogTitle.value = '修改赛事阶段'
    dialogOpen.value = true
  })
}

function handleDetail(row: StageListItem) {
  getStage(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateStage(currentId.value, form.value)
      : addStage(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handleActivate(row: StageListItem) {
  proxy.$modal.confirm(`确认激活阶段「${row.stageName}」？`).then(() => activateStage(row.id)).then(() => {
    proxy.$modal.msgSuccess('激活成功')
    getList()
  }).catch(() => {})
}

function handleFinish(row: StageListItem) {
  proxy.$modal.confirm(`确认结束阶段「${row.stageName}」？`).then(() => finishStage(row.id)).then(() => {
    proxy.$modal.msgSuccess('已结束')
    getList()
  }).catch(() => {})
}

function handleCancel(row: StageListItem) {
  proxy.$modal.confirm(`确认取消阶段「${row.stageName}」？`).then(() => cancelStage(row.id)).then(() => {
    proxy.$modal.msgSuccess('已取消')
    getList()
  }).catch(() => {})
}

function handleDelete(row: StageListItem) {
  proxy.$modal.confirm(`确认删除阶段「${row.stageName}」？`).then(() => delStage(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function openRoundDrawer(row: StageListItem) {
  currentStage.value = row
  roundDrawerOpen.value = true
  loadRoundList()
}

function loadRoundList() {
  if (!currentStage.value) return
  roundLoading.value = true
  listRoundByStage(currentStage.value.id).then(res => {
    roundList.value = res.data || []
  }).finally(() => {
    roundLoading.value = false
  })
}

function resetRoundForm() {
  roundForm.value = {
    roundName: '',
    roundType: '',
    sequenceNo: 0,
    bestOf: 1
  }
  proxy.resetForm('roundFormRef')
}

function handleRoundAdd() {
  resetRoundForm()
  currentRoundId.value = undefined
  roundDialogTitle.value = '新增轮次'
  roundDialogOpen.value = true
}

function handleRoundUpdate(row: RoundListItem) {
  resetRoundForm()
  currentRoundId.value = row.id
  getRound(row.id).then(res => {
    const data = res.data!
    roundForm.value = {
      roundName: data.roundName || '',
      roundType: data.roundType,
      sequenceNo: data.sequenceNo ?? 0,
      bestOf: data.bestOf ?? 1,
      startTime: data.startTime,
      endTime: data.endTime
    }
    roundDialogTitle.value = '修改轮次'
    roundDialogOpen.value = true
  })
}

function submitRoundForm() {
  if (!currentStage.value) return
  proxy.$refs.roundFormRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentRoundId.value
      ? updateRound(currentRoundId.value, roundForm.value)
      : addRound(currentStage.value!.id, roundForm.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentRoundId.value ? '修改成功' : '新增成功')
      roundDialogOpen.value = false
      loadRoundList()
    })
  })
}

function handleRoundActivate(row: RoundListItem) {
  proxy.$modal.confirm(`确认激活轮次「${row.roundName}」？`).then(() => activateRound(row.id)).then(() => {
    proxy.$modal.msgSuccess('激活成功')
    loadRoundList()
  }).catch(() => {})
}

function handleRoundFinish(row: RoundListItem) {
  proxy.$modal.confirm(`确认结束轮次「${row.roundName}」？`).then(() => finishRound(row.id)).then(() => {
    proxy.$modal.msgSuccess('已结束')
    loadRoundList()
  }).catch(() => {})
}

function handleRoundCancel(row: RoundListItem) {
  proxy.$modal.confirm(`确认取消轮次「${row.roundName}」？`).then(() => cancelRound(row.id)).then(() => {
    proxy.$modal.msgSuccess('已取消')
    loadRoundList()
  }).catch(() => {})
}

function handleRoundDelete(row: RoundListItem) {
  proxy.$modal.confirm(`确认删除轮次「${row.roundName}」？`).then(() => delRound(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    loadRoundList()
  }).catch(() => {})
}

loadTournaments()
getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
