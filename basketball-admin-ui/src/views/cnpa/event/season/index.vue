<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="所属赛事">
        <el-select v-model="selectedTournamentId" placeholder="请选择赛事" filterable style="width: 280px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList" :disabled="!selectedTournamentId">查询</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:season:add']" :disabled="!selectedTournamentId">新增赛季</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="赛季编号" prop="seasonNo" min-width="140" />
      <el-table-column label="赛季名称" prop="seasonName" min-width="140" />
      <el-table-column label="赛季年份" prop="seasonYear" width="90" />
      <el-table-column label="状态" prop="seasonStatus" width="100">
        <template #default="scope">
          <dict-tag :options="SEASON_STATUS_OPTIONS" :value="scope.row.seasonStatus" />
        </template>
      </el-table-column>
      <el-table-column label="报名开始" prop="registrationStartTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.registrationStartTime) }}</template>
      </el-table-column>
      <el-table-column label="报名结束" prop="registrationEndTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.registrationEndTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="420" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:season:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:season:edit']">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:season:publish']">发布</el-button>
          <el-button link type="success" icon="Unlock" @click="handleOpenRegistration(scope.row)" v-hasPermi="['basket:season:openRegistration']" v-if="[0, 4].includes(scope.row.seasonStatus)">开启报名</el-button>
          <el-button link type="warning" icon="Lock" @click="handleCloseRegistration(scope.row)" v-hasPermi="['basket:season:closeRegistration']" v-if="scope.row.seasonStatus === 1">截止报名</el-button>
          <el-button link type="primary" icon="VideoPlay" @click="handleStart(scope.row)" v-hasPermi="['basket:season:start']" v-if="[1, 4].includes(scope.row.seasonStatus)">开始</el-button>
          <el-button link type="info" icon="CircleCheck" @click="handleFinish(scope.row)" v-hasPermi="['basket:season:finish']" v-if="scope.row.seasonStatus === 2">结束</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:season:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailOpen" title="赛季详情" size="640px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="赛季编号">{{ detail.seasonNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.seasonStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛季名称">{{ detail.seasonName }}</el-descriptions-item>
        <el-descriptions-item label="赛季年份">{{ detail.seasonYear }}</el-descriptions-item>
        <el-descriptions-item label="报名开始">{{ parseTime(detail.registrationStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="报名结束">{{ parseTime(detail.registrationEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="赛季开始">{{ parseTime(detail.seasonStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="赛季结束">{{ parseTime(detail.seasonEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="最大队伍数">{{ detail.maxTeamCount }}</el-descriptions-item>
        <el-descriptions-item label="阵容人数">{{ detail.minRosterCount }} ~ {{ detail.maxRosterCount }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="赛季名称" prop="seasonName">
              <el-input v-model="form.seasonName" placeholder="请输入赛季名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛季年份">
              <el-input-number v-model="form.seasonYear" :min="2000" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名开始">
              <el-date-picker v-model="form.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="报名结束">
              <el-date-picker v-model="form.registrationEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛季开始">
              <el-date-picker v-model="form.seasonStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛季结束">
              <el-date-picker v-model="form.seasonEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大队伍数">
              <el-input-number v-model="form.maxTeamCount" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最小阵容">
              <el-input-number v-model="form.minRosterCount" :min="5" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大阵容">
              <el-input-number v-model="form.maxRosterCount" :min="5" controls-position="right" style="width: 100%" />
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

<script setup lang="ts" name="BasketSeason">
import {
  addSeason,
  closeSeasonRegistration,
  delSeason,
  finishSeason,
  getSeason,
  listSeasonByTournament,
  openSeasonRegistration,
  publishSeason,
  startSeason,
  updateSeason
} from '@/api/basket/season'
import { listTournament } from '@/api/basket/tournament'
import type { SeasonCreateReq, SeasonDetail, SeasonListItem, TournamentListItem } from '@/types'
import { SEASON_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const dataList = ref<SeasonListItem[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const selectedTournamentId = ref<number>()
const detailOpen = ref(false)
const detail = ref<SeasonDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()

const form = ref<SeasonCreateReq>({
  seasonName: '',
  publicStatus: 1,
  minRosterCount: 5,
  maxRosterCount: 12
})

const rules = {
  seasonName: [{ required: true, message: '赛季名称不能为空', trigger: 'blur' }]
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows
    if (!selectedTournamentId.value && res.rows.length > 0) {
      selectedTournamentId.value = res.rows[0].id
      getList()
    }
  })
}

function getList() {
  if (!selectedTournamentId.value) return
  loading.value = true
  listSeasonByTournament(selectedTournamentId.value).then(res => {
    dataList.value = res.data || []
  }).finally(() => {
    loading.value = false
  })
}

function handleTournamentChange() {
  getList()
}

function resetFormData() {
  form.value = {
    seasonName: '',
    seasonYear: new Date().getFullYear(),
    publicStatus: 1,
    minRosterCount: 5,
    maxRosterCount: 12
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增赛季'
  dialogOpen.value = true
}

function handleUpdate(row: SeasonListItem) {
  resetFormData()
  currentId.value = row.id
  getSeason(row.id).then(res => {
    const data = res.data!
    form.value = {
      seasonName: data.seasonName || '',
      seasonYear: data.seasonYear,
      registrationStartTime: data.registrationStartTime,
      registrationEndTime: data.registrationEndTime,
      seasonStartTime: data.seasonStartTime,
      seasonEndTime: data.seasonEndTime,
      venueId: data.venueId,
      maxTeamCount: data.maxTeamCount,
      minRosterCount: data.minRosterCount,
      maxRosterCount: data.maxRosterCount,
      publicStatus: 1,
      description: data.description
    }
    dialogTitle.value = '修改赛季'
    dialogOpen.value = true
  })
}

function handleDetail(row: SeasonListItem) {
  getSeason(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  if (!selectedTournamentId.value) return
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateSeason(currentId.value, form.value)
      : addSeason(selectedTournamentId.value, form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: SeasonListItem) {
  proxy.$modal.confirm(`确认发布赛季「${row.seasonName}」？`).then(() => publishSeason(row.id)).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  }).catch(() => {})
}

function handleOpenRegistration(row: SeasonListItem) {
  proxy.$modal.confirm(`确认开启赛季「${row.seasonName}」报名？`).then(() => openSeasonRegistration(row.id)).then(() => {
    proxy.$modal.msgSuccess('已开启报名')
    getList()
  }).catch(() => {})
}

function handleCloseRegistration(row: SeasonListItem) {
  proxy.$modal.confirm(`确认截止赛季「${row.seasonName}」报名？`).then(() => closeSeasonRegistration(row.id)).then(() => {
    proxy.$modal.msgSuccess('已截止报名')
    getList()
  }).catch(() => {})
}

function handleStart(row: SeasonListItem) {
  proxy.$modal.confirm(`确认开始赛季「${row.seasonName}」？`).then(() => startSeason(row.id)).then(() => {
    proxy.$modal.msgSuccess('赛季已开始')
    getList()
  }).catch(() => {})
}

function handleFinish(row: SeasonListItem) {
  proxy.$modal.confirm(`确认结束赛季「${row.seasonName}」？`).then(() => finishSeason(row.id)).then(() => {
    proxy.$modal.msgSuccess('赛季已结束')
    getList()
  }).catch(() => {})
}

function handleDelete(row: SeasonListItem) {
  proxy.$modal.confirm(`确认删除赛季「${row.seasonName}」？`).then(() => delSeason(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

loadTournaments()
</script>
