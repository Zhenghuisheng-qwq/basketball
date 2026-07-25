<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="赛事编号" prop="tournamentNo">
        <el-input v-model="queryParams.tournamentNo" placeholder="请输入赛事编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛事名称" prop="tournamentName">
        <el-input v-model="queryParams.tournamentName" placeholder="请输入赛事名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="tournamentStatus">
        <el-select v-model="queryParams.tournamentStatus" placeholder="赛事状态" clearable style="width: 200px">
          <el-option v-for="item in TOURNAMENT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:tournament:add']" :disabled="!seasonOptions.length">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-alert
      v-if="seasonOptionsError"
      type="error"
      :closable="false"
      show-icon
      class="mb8"
      title="赛季选项加载失败"
      :description="seasonOptionsError"
    />

    <el-alert
      v-else-if="!seasonOptions.length"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
      title="请先创建赛季"
      description="创建赛事前需要先在「赛季管理」中创建顶层赛季。"
    />

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="赛事编号" prop="tournamentNo" min-width="140" />
      <el-table-column label="赛事名称" prop="tournamentName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="所属赛季" min-width="140">
        <template #default="scope">
          {{ scope.row.competitionSeasonName || '-' }}
          <span v-if="scope.row.competitionSeasonYear" class="sub-text">({{ scope.row.competitionSeasonYear }})</span>
        </template>
      </el-table-column>
      <el-table-column label="比赛终端" prop="gameTerminal" width="100" />
      <el-table-column label="状态" prop="tournamentStatus" width="100">
        <template #default="scope">
          <dict-tag :options="TOURNAMENT_STATUS_OPTIONS" :value="scope.row.tournamentStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="displayOrder" width="80" />
      <el-table-column label="创建时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:tournament:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:tournament:edit']">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:tournament:publish']" v-if="[0, 3].includes(scope.row.tournamentStatus)">发布</el-button>
          <el-button link type="warning" icon="Download" @click="handleUnpublish(scope.row)" v-hasPermi="['basket:tournament:publish']" v-if="scope.row.tournamentStatus === 1">下架</el-button>
          <el-button link type="info" icon="CircleClose" @click="handleClose(scope.row)" v-hasPermi="['basket:tournament:close']" v-if="[1, 3].includes(scope.row.tournamentStatus)">关闭</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:tournament:remove']" v-if="scope.row.tournamentStatus === 3">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赛事详情" size="640px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="赛事编号">{{ detail.tournamentNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.tournamentStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛事名称">{{ detail.tournamentName }}</el-descriptions-item>
        <el-descriptions-item label="所属赛季">{{ detail.competitionSeasonName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ detail.shortName }}</el-descriptions-item>
        <el-descriptions-item label="英文名">{{ detail.englishName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.tournamentType }}</el-descriptions-item>
        <el-descriptions-item label="游戏终端">{{ detail.gameTerminal }}</el-descriptions-item>
        <el-descriptions-item label="报名开始">{{ parseTime(detail.registrationStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="报名结束">{{ parseTime(detail.registrationEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="比赛开始">{{ parseTime(detail.competitionStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="比赛结束">{{ parseTime(detail.competitionEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="赛制">{{ detail.formatCode }}</el-descriptions-item>
        <el-descriptions-item label="主办方">{{ detail.organizerName }}</el-descriptions-item>
        <el-descriptions-item label="公开状态">{{ detail.publicStatus === 1 ? '公开' : '不公开' }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="所属赛季" prop="competitionSeasonId">
              <el-select v-model="form.competitionSeasonId" placeholder="请选择所属赛季" filterable style="width: 100%" :disabled="!!currentId && !canEditSeason">
                <el-option v-for="item in seasonOptions" :key="item.id" :label="`${item.seasonName} (${item.seasonYear})`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛事名称" prop="tournamentName">
              <el-input v-model="form.tournamentName" placeholder="请输入赛事名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称">
              <el-input v-model="form.shortName" placeholder="请输入简称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="英文名">
              <el-input v-model="form.englishName" placeholder="请输入英文名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛事类型">
              <el-input v-model="form.tournamentType" placeholder="请输入赛事类型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="游戏终端">
              <el-input v-model="form.gameTerminal" placeholder="请输入游戏终端" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛制">
              <el-input v-model="form.formatCode" placeholder="请输入赛制编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主办方">
              <el-input v-model="form.organizerName" placeholder="请输入主办方" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.displayOrder" :min="0" controls-position="right" style="width: 100%" />
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
          <el-col :span="12">
            <el-form-item label="报名开始" prop="registrationStartTime">
              <el-date-picker v-model="form.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名结束" prop="registrationEndTime">
              <el-date-picker v-model="form.registrationEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="比赛开始" prop="competitionStartTime">
              <el-date-picker v-model="form.competitionStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="比赛结束" prop="competitionEndTime">
              <el-date-picker v-model="form.competitionEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="简介">
              <el-input v-model="form.description" type="textarea" placeholder="请输入简介" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>

<script setup lang="ts" name="BasketTournament">
import {
  addTournament,
  closeTournament,
  delTournament,
  getTournament,
  listTournament,
  publishTournament,
  unpublishTournament,
  updateTournament
} from '@/api/basket/tournament'
import { listCompetitionSeason, listCompetitionSeasonOptions } from '@/api/basket/competitionSeason'
import type {
  CompetitionSeasonListItem,
  TournamentCreateReq,
  TournamentDetail,
  TournamentListItem,
  TournamentQuery
} from '@/types'
import { TOURNAMENT_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<TournamentListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<TournamentDetail>()
const seasonOptions = ref<CompetitionSeasonListItem[]>([])
const seasonOptionsError = ref('')
const canEditSeason = ref(true)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()

const queryParams = ref<TournamentQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<TournamentCreateReq>({
  competitionSeasonId: undefined as unknown as number,
  tournamentName: '',
  publicStatus: 1,
  displayOrder: 0
})

const rules = {
  competitionSeasonId: [{ required: true, message: '请选择所属赛季', trigger: 'change' }],
  tournamentName: [{ required: true, message: '赛事名称不能为空', trigger: 'blur' }],
  registrationStartTime: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEndTime: [{ required: true, message: '请选择报名结束时间', trigger: 'change' }],
  competitionStartTime: [{ required: true, message: '请选择比赛开始时间', trigger: 'change' }],
  competitionEndTime: [{ required: true, message: '请选择比赛结束时间', trigger: 'change' }]
}

async function loadSeasonOptions() {
  seasonOptionsError.value = ''
  try {
    const res = await listCompetitionSeasonOptions()
    seasonOptions.value = res.data || []
    return
  } catch {
    // options 接口失败时，回退到赛季列表接口
  }
  try {
    const res = await listCompetitionSeason({ pageNum: 1, pageSize: 500 })
    seasonOptions.value = res.rows || []
  } catch (error: unknown) {
    seasonOptions.value = []
    seasonOptionsError.value = error instanceof Error ? error.message : '加载赛季选项失败'
  }
}

function getList() {
  loading.value = true
  listTournament(queryParams.value).then(response => {
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
  proxy.resetForm('queryRef')
  handleQuery()
}

function resetFormData() {
  form.value = {
    competitionSeasonId: seasonOptions.value[0]?.id as number,
    tournamentName: '',
    shortName: '',
    englishName: '',
    tournamentType: 'LEAGUE',
    gameTerminal: 'PC',
    formatCode: '5V5',
    organizerName: '',
    description: '',
    registrationStartTime: '',
    registrationEndTime: '',
    competitionStartTime: '',
    competitionEndTime: '',
    publicStatus: 1,
    displayOrder: 0
  }
  canEditSeason.value = true
  proxy.resetForm('formRef')
}

function handleAdd() {
  if (!seasonOptions.value.length) {
    proxy.$modal.msgWarning('请先创建赛季')
    return
  }
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增赛事'
  dialogOpen.value = true
}

function handleUpdate(row: TournamentListItem) {
  resetFormData()
  currentId.value = row.id
  getTournament(row.id).then(res => {
    const data = res.data!
    form.value = {
      competitionSeasonId: data.competitionSeasonId as number,
      tournamentName: data.tournamentName || '',
      shortName: data.shortName,
      englishName: data.englishName,
      tournamentType: data.tournamentType,
      gameTerminal: data.gameTerminal,
      formatCode: data.formatCode,
      organizerName: data.organizerName,
      description: data.description,
      registrationStartTime: data.registrationStartTime,
      registrationEndTime: data.registrationEndTime,
      competitionStartTime: data.competitionStartTime,
      competitionEndTime: data.competitionEndTime,
      publicStatus: data.publicStatus ?? 1,
      displayOrder: data.displayOrder ?? 0
    }
    canEditSeason.value = data.tournamentStatus === 3
    dialogTitle.value = '修改赛事'
    dialogOpen.value = true
  })
}

function handleDetail(row: TournamentListItem) {
  getTournament(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateTournament(currentId.value, form.value)
      : addTournament(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: TournamentListItem) {
  proxy.$modal.confirm(`确认发布赛事「${row.tournamentName}」？`).then(() => publishTournament(row.id)).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  }).catch(() => {})
}

function handleUnpublish(row: TournamentListItem) {
  proxy.$modal.confirm(`确认下架赛事「${row.tournamentName}」？`).then(() => unpublishTournament(row.id)).then(() => {
    proxy.$modal.msgSuccess('下架成功')
    getList()
  }).catch(() => {})
}

function handleClose(row: TournamentListItem) {
  proxy.$modal.confirm(`确认关闭赛事「${row.tournamentName}」？`).then(() => closeTournament(row.id)).then(() => {
    proxy.$modal.msgSuccess('关闭成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row: TournamentListItem) {
  proxy.$modal.confirm(`确认删除草稿赛事「${row.tournamentName}」？`).then(() => delTournament(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
loadSeasonOptions()

onActivated(() => {
  loadSeasonOptions()
})
</script>
