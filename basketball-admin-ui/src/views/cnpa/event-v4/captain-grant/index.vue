<template>
  <div class="app-container">
    <el-alert title="V4 队长授权：按 Division 授予长期球队队长提交报名权限" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="queryParams" :inline="true" class="mb8">
      <el-form-item label="Season ID">
        <el-input-number v-model="queryParams.seasonId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="Division ID">
        <el-input-number v-model="queryParams.divisionId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="Team ID">
        <el-input-number v-model="queryParams.teamId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.grantStatus" clearable style="width: 140px">
          <el-option v-for="item in GRANT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openAdd" v-hasPermi="['basket:v4CaptainGrant:add']">新增授权</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="Season" prop="seasonTitle" min-width="140" />
      <el-table-column label="Division" prop="divisionName" min-width="120" />
      <el-table-column label="平台" prop="platformCode" width="80" />
      <el-table-column label="球队" prop="teamName" min-width="120" />
      <el-table-column label="队长球员" prop="playerName" min-width="120" />
      <el-table-column label="状态" prop="grantStatus" width="100" />
      <el-table-column label="有效期" min-width="180">
        <template #default="scope">{{ formatRange(scope.row.validFrom, scope.row.validTo) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="danger" @click="handleRevoke(scope.row)" v-hasPermi="['basket:v4CaptainGrant:revoke']" v-if="scope.row.grantStatus === 'ACTIVE'">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="addOpen" title="新增 V4 队长授权" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="Division ID" prop="divisionId">
          <el-input-number v-model="form.divisionId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Team ID" prop="teamId">
          <el-input-number v-model="form.teamId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Player ID" prop="playerId">
          <el-input-number v-model="form.playerId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="Member ID">
          <el-input-number v-model="form.memberId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addOpen = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4CaptainGrant">
import { addV4CaptainGrant, listV4CaptainGrant, revokeV4CaptainGrant, type BasketCaptainGrantCreateReq, type BasketCaptainGrantQuery } from '@/api/cnpa/event-v4/captain-grant'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const total = ref(0)
const dataList = ref<any[]>([])
const addOpen = ref(false)
const formRef = ref()
const queryParams = reactive<BasketCaptainGrantQuery>({ pageNum: 1, pageSize: 10 })
const form = reactive<BasketCaptainGrantCreateReq>({})
const GRANT_STATUS_OPTIONS = [
  { label: '有效', value: 'ACTIVE' },
  { label: '已撤销', value: 'REVOKED' },
  { label: '已过期', value: 'EXPIRED' }
]
const rules = {
  divisionId: [{ required: true, message: 'Division ID 不能为空', trigger: 'blur' }],
  teamId: [{ required: true, message: 'Team ID 不能为空', trigger: 'blur' }],
  playerId: [{ required: true, message: 'Player ID 不能为空', trigger: 'blur' }]
}

function formatRange(start?: string, end?: string) {
  if (!start && !end) return '-'
  return `${parseTime(start) || '-'} ~ ${parseTime(end) || '-'}`
}

function getList() {
  loading.value = true
  listV4CaptainGrant(queryParams).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, teamId: undefined, playerId: undefined, grantStatus: undefined })
  getList()
}

function openAdd() {
  Object.assign(form, { divisionId: undefined, teamId: undefined, playerId: undefined, memberId: undefined, remark: undefined })
  addOpen.value = true
}

function submitAdd() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    addV4CaptainGrant(form).then(() => {
      proxy.$modal.msgSuccess('授权成功')
      addOpen.value = false
      getList()
    })
  })
}

function handleRevoke(row: any) {
  proxy.$modal.confirm(`确认撤销授权 #${row.id}？`).then(() => revokeV4CaptainGrant(row.id)).then(() => {
    proxy.$modal.msgSuccess('已撤销')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
