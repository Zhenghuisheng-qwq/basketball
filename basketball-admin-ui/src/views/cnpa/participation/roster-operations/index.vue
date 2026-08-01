<template>
  <div class="app-container">
    <el-card shadow="never" class="mb16">
      <template #header>
        <span>阵容特殊操作</span>
      </template>
      <el-row :gutter="24">
        <el-col :span="12">
          <div class="section-title">强制移除球员</div>
          <el-form ref="forceRemoveRef" :model="forceRemoveForm" :rules="forceRemoveRules" label-width="100px">
            <el-form-item label="阵容ID" prop="rosterId">
              <el-input v-model.number="forceRemoveForm.rosterId" placeholder="请输入生效中的阵容ID" />
            </el-form-item>
            <el-form-item label="操作原因" prop="operateReason">
              <el-input v-model="forceRemoveForm.operateReason" type="textarea" placeholder="请输入操作原因" />
            </el-form-item>
            <el-form-item>
              <el-button type="danger" @click="submitForceRemove" v-hasPermi="['basket:rosterOperation:edit']">强制移除</el-button>
            </el-form-item>
          </el-form>
        </el-col>
        <el-col :span="12">
          <div class="section-title">更新阵容角色</div>
          <el-form ref="updateRoleRef" :model="updateRoleForm" :rules="updateRoleRules" label-width="100px">
            <el-form-item label="阵容ID" prop="rosterId">
              <el-input v-model.number="updateRoleForm.rosterId" placeholder="请输入生效中的阵容ID" />
            </el-form-item>
            <el-form-item label="角色">
              <el-input v-model="updateRoleForm.rosterRole" placeholder="如 PG/SG/SF/PF/C" />
            </el-form-item>
            <el-form-item label="是否首发">
              <el-radio-group v-model="updateRoleForm.isStarting">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="是否替补">
              <el-radio-group v-model="updateRoleForm.isSubstitute">
                <el-radio :value="1">是</el-radio>
                <el-radio :value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="操作原因" prop="operateReason">
              <el-input v-model="updateRoleForm.operateReason" type="textarea" placeholder="请输入操作原因" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitUpdateRole" v-hasPermi="['basket:rosterOperation:edit']">更新角色</el-button>
            </el-form-item>
          </el-form>
        </el-col>
      </el-row>
    </el-card>

    <el-divider content-position="left">操作日志</el-divider>

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="赛季ID" prop="seasonId">
        <el-input v-model="queryParams.seasonId" placeholder="请输入赛季ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛事球队ID" prop="eventTeamId">
        <el-input v-model="queryParams.eventTeamId" placeholder="请输入赛事球队ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作类型" prop="operationType">
        <el-select v-model="queryParams.operationType" placeholder="操作类型" clearable style="width: 200px">
          <el-option v-for="item in ROSTER_OPERATION_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="操作类型" prop="operationType" width="120">
        <template #default="scope">
          <dict-tag :options="ROSTER_OPERATION_TYPE_OPTIONS" :value="scope.row.operationType" />
        </template>
      </el-table-column>
      <el-table-column label="赛季ID" prop="seasonId" width="90" />
      <el-table-column label="赛事球队ID" prop="eventTeamId" width="110" />
      <el-table-column label="目标阵容ID" prop="targetRosterId" width="110" />
      <el-table-column label="目标球员ID" prop="targetPlayerId" width="110" />
      <el-table-column label="变更前" prop="beforeValue" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="变更后" prop="afterValue" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="操作原因" prop="operateReason" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="操作人" prop="operatorName" width="100" />
      <el-table-column label="生效时间" prop="effectiveTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.effectiveTime) }}</template>
      </el-table-column>
      <el-table-column label="记录时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="BasketRosterOperation">
import {
  forceRemoveRoster,
  listRosterOperationLogs,
  updateRosterRole
} from '@/api/basket/rosterOperation'
import type {
  RosterOperationForceRemoveReq,
  RosterOperationLogItem,
  RosterOperationLogQuery,
  RosterOperationUpdateRoleReq
} from '@/types'
import { ROSTER_OPERATION_TYPE_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<RosterOperationLogItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)

const forceRemoveForm = ref<Partial<RosterOperationForceRemoveReq>>({ operateReason: '' })
const updateRoleForm = ref<Partial<RosterOperationUpdateRoleReq>>({
  rosterRole: '',
  isStarting: 0,
  isSubstitute: 0,
  operateReason: ''
})

const forceRemoveRules = {
  rosterId: [{ required: true, message: '阵容ID不能为空', trigger: 'blur' }],
  operateReason: [{ required: true, message: '操作原因不能为空', trigger: 'blur' }]
}

const updateRoleRules = {
  rosterId: [{ required: true, message: '阵容ID不能为空', trigger: 'blur' }],
  operateReason: [{ required: true, message: '操作原因不能为空', trigger: 'blur' }]
}

const queryParams = ref<RosterOperationLogQuery>({
  pageNum: 1,
  pageSize: 10
})

function getList() {
  loading.value = true
  listRosterOperationLogs(queryParams.value).then(response => {
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

function submitForceRemove() {
  proxy.$refs.forceRemoveRef.validate((valid: boolean) => {
    if (!valid) return
    proxy.$modal.confirm('确认强制移除该阵容球员？此操作不可撤销。').then(() =>
      forceRemoveRoster(forceRemoveForm.value as RosterOperationForceRemoveReq)
    ).then(() => {
      proxy.$modal.msgSuccess('强制移除成功')
      proxy.resetForm('forceRemoveRef')
      getList()
    }).catch(() => {})
  })
}

function submitUpdateRole() {
  proxy.$refs.updateRoleRef.validate((valid: boolean) => {
    if (!valid) return
    proxy.$modal.confirm('确认更新该阵容角色？').then(() =>
      updateRosterRole(updateRoleForm.value as RosterOperationUpdateRoleReq)
    ).then(() => {
      proxy.$modal.msgSuccess('角色更新成功')
      proxy.resetForm('updateRoleRef')
      getList()
    }).catch(() => {})
  })
}

getList()
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.mb8 {
  margin-bottom: 8px;
}
.section-title {
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
