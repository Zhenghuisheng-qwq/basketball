<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="赛季名称">
        <el-input v-model="queryParams.seasonName" placeholder="请输入赛季名称" clearable style="width: 200px" @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="赛季年份">
        <el-input-number v-model="queryParams.seasonYear" :min="2000" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.seasonStatus" placeholder="赛季状态" clearable style="width: 160px">
          <el-option v-for="item in COMPETITION_SEASON_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">查询</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:season:add']">新增赛季</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="赛季编号" prop="seasonNo" min-width="140" />
      <el-table-column label="赛季名称" prop="seasonName" min-width="140" />
      <el-table-column label="赛季年份" prop="seasonYear" width="90" />
      <el-table-column label="状态" prop="seasonStatus" width="100">
        <template #default="scope">
          <dict-tag :options="COMPETITION_SEASON_STATUS_OPTIONS" :value="scope.row.seasonStatus" />
        </template>
      </el-table-column>
      <el-table-column label="开始时间" prop="startTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" prop="endTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="关联赛事" prop="tournamentCount" width="90" />
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:season:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:season:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:season:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赛季详情" size="640px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="赛季编号">{{ detail.seasonNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.seasonStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛季名称">{{ detail.seasonName }}</el-descriptions-item>
        <el-descriptions-item label="赛季年份">{{ detail.seasonYear }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ parseTime(detail.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ parseTime(detail.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="关联赛事">{{ detail.tournamentCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="公开状态">{{ detail.publicStatus === 1 ? '公开' : '不公开' }}</el-descriptions-item>
        <el-descriptions-item label="说明" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="赛季名称" prop="seasonName">
              <el-input v-model="form.seasonName" placeholder="例如：2026 赛季" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛季年份" prop="seasonYear">
              <el-input-number v-model="form.seasonYear" :min="2000" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="赛季状态">
              <el-select v-model="form.seasonStatus" style="width: 100%">
                <el-option v-for="item in COMPETITION_SEASON_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
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
            <el-form-item label="说明">
              <el-input v-model="form.description" type="textarea" placeholder="请输入赛季说明" />
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

<script setup lang="ts" name="BasketCompetitionSeason">
import {
  addCompetitionSeason,
  delCompetitionSeason,
  getCompetitionSeason,
  listCompetitionSeason,
  updateCompetitionSeason
} from '@/api/basket/competitionSeason'
import type {
  CompetitionSeasonCreateReq,
  CompetitionSeasonDetail,
  CompetitionSeasonListItem,
  CompetitionSeasonQuery
} from '@/types'
import { COMPETITION_SEASON_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const total = ref(0)
const dataList = ref<CompetitionSeasonListItem[]>([])
const detailOpen = ref(false)
const detail = ref<CompetitionSeasonDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()

const queryParams = ref<CompetitionSeasonQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<CompetitionSeasonCreateReq>({
  seasonName: '',
  seasonYear: new Date().getFullYear(),
  seasonStatus: 0,
  publicStatus: 1
})

const rules = {
  seasonName: [{ required: true, message: '赛季名称不能为空', trigger: 'blur' }],
  seasonYear: [{ required: true, message: '赛季年份不能为空', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

function getList() {
  loading.value = true
  listCompetitionSeason(queryParams.value).then(res => {
    dataList.value = res.rows
    total.value = res.total
  }).finally(() => {
    loading.value = false
  })
}

function resetFormData() {
  form.value = {
    seasonName: '',
    seasonYear: new Date().getFullYear(),
    seasonStatus: 0,
    publicStatus: 1,
    description: ''
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增赛季'
  dialogOpen.value = true
}

function handleUpdate(row: CompetitionSeasonListItem) {
  resetFormData()
  currentId.value = row.id
  getCompetitionSeason(row.id).then(res => {
    const data = res.data!
    form.value = {
      seasonName: data.seasonName || '',
      seasonYear: data.seasonYear,
      startTime: data.startTime,
      endTime: data.endTime,
      seasonStatus: data.seasonStatus,
      publicStatus: data.publicStatus ?? 1,
      description: data.description
    }
    dialogTitle.value = '修改赛季'
    dialogOpen.value = true
  })
}

function handleDetail(row: CompetitionSeasonListItem) {
  getCompetitionSeason(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateCompetitionSeason(currentId.value, form.value)
      : addCompetitionSeason(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handleDelete(row: CompetitionSeasonListItem) {
  proxy.$modal.confirm(`确认删除赛季「${row.seasonName}」？`).then(() => delCompetitionSeason(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>
