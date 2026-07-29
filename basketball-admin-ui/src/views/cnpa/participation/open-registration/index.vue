<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="报名编号" prop="registrationNo">
        <el-input v-model="queryParams.registrationNo" placeholder="请输入报名编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛季ID" prop="seasonId">
        <el-input v-model="queryParams.seasonId" placeholder="请输入赛季ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStage">
        <el-select v-model="queryParams.auditStage" placeholder="审核状态" clearable style="width: 200px">
          <el-option v-for="item in CNPA_AUDIT_STATUS_FILTER_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="报名编号" prop="registrationNo" min-width="140" />
      <el-table-column label="赛季" min-width="140">
        <template #default="scope">
          <div>{{ scope.row.seasonName || '-' }}</div>
          <div class="sub-text">ID: {{ scope.row.seasonId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="球员" min-width="160">
        <template #default="scope">
          <div>{{ scope.row.playerNickname || '-' }}</div>
          <div class="sub-text">ID: {{ scope.row.playerId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="平台" prop="platform" width="90" />
      <el-table-column label="游戏ID" prop="displayGameId" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="registrationStatusName" width="100">
        <template #default="scope">
          <el-tag :type="openRegistrationTagType(scope.row.registrationStatus)" size="small">
            {{ scope.row.registrationStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submittedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submittedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:openRegistrationAudit:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="openApprove(scope.row)" v-hasPermi="['basket:openRegistrationAudit:audit']" v-if="canOpenRegistrationAudit(scope.row.registrationStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:openRegistrationAudit:audit']" v-if="canOpenRegistrationAudit(scope.row.registrationStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="Open报名审核详情" size="720px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="报名编号">{{ detail.registrationNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.registrationStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName || detail.seasonId }}</el-descriptions-item>
        <el-descriptions-item label="球员编号">{{ detail.playerNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球员昵称">{{ detail.playerNickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detail.positionCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="平台">{{ detail.platform || '-' }}</el-descriptions-item>
        <el-descriptions-item label="游戏ID">{{ detail.displayGameId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ parseTime(detail.submittedTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ parseTime(detail.auditTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">审核记录</el-divider>
      <el-table :data="auditRecords" size="small">
        <el-table-column label="操作" prop="auditAction" width="120" />
        <el-table-column label="审核人" prop="auditorName" width="100" />
        <el-table-column label="备注" prop="auditRemark" min-width="160" />
        <el-table-column label="时间" prop="auditTime" width="160">
          <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog title="审核通过" v-model="approveOpen" width="480px" append-to-body>
      <el-form :model="approveForm" label-width="88px">
        <el-form-item label="审核备注">
          <el-input v-model="approveForm.auditRemark" type="textarea" placeholder="请输入审核备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveOpen = false">取消</el-button>
        <el-button type="primary" @click="submitApprove">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="审核驳回" v-model="rejectOpen" width="480px" append-to-body>
      <el-form ref="rejectRef" :model="rejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input v-model="rejectForm.rejectReason" type="textarea" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="rejectForm.auditRemark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketOpenRegistrationAudit">
import {
  approveOpenRegistrationAudit,
  getOpenRegistrationAudit,
  listOpenRegistrationAudit,
  listOpenRegistrationAuditRecords,
  rejectOpenRegistrationAudit
} from '@/api/basket/openRegistrationAudit'
import type {
  OpenRegistrationAuditApproveReq,
  OpenRegistrationAuditDetail,
  OpenRegistrationAuditListItem,
  OpenRegistrationAuditQuery,
  OpenRegistrationAuditRecord,
  OpenRegistrationAuditRejectReq
} from '@/types'
import { CNPA_AUDIT_STATUS_FILTER_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<OpenRegistrationAuditListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<OpenRegistrationAuditDetail>()
const auditRecords = ref<OpenRegistrationAuditRecord[]>([])
const currentId = ref<number>()
const approveOpen = ref(false)
const rejectOpen = ref(false)
const approveForm = ref<OpenRegistrationAuditApproveReq>({})
const rejectForm = ref<OpenRegistrationAuditRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const queryParams = ref<OpenRegistrationAuditQuery>({
  pageNum: 1,
  pageSize: 10,
  auditStage: 'PENDING'
})

function canOpenRegistrationAudit(status?: number) {
  return status === 1 || status === 2
}

function openRegistrationTagType(status?: number) {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 4) return 'danger'
  if (status === 5 || status === 6) return 'info'
  return 'info'
}

function getList() {
  loading.value = true
  listOpenRegistrationAudit(queryParams.value).then(response => {
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
  queryParams.value.auditStage = undefined
  handleQuery()
}

function handleDetail(row: OpenRegistrationAuditListItem) {
  currentId.value = row.id
  detailOpen.value = true
  auditRecords.value = []
  getOpenRegistrationAudit(row.id).then(res => {
    detail.value = res.data
  })
  listOpenRegistrationAuditRecords(row.id).then(res => {
    auditRecords.value = res.data || []
  })
}

function openApprove(row: OpenRegistrationAuditListItem) {
  currentId.value = row.id
  approveForm.value = { auditRemark: '' }
  approveOpen.value = true
}

function submitApprove() {
  if (!currentId.value) return
  proxy.$modal.confirm('确认通过该Open报名审核？').then(() =>
    approveOpenRegistrationAudit(currentId.value!, approveForm.value)
  ).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    approveOpen.value = false
    getList()
  }).catch(() => {})
}

function openReject(row: OpenRegistrationAuditListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    proxy.$modal.confirm('确认驳回该Open报名审核？').then(() =>
      rejectOpenRegistrationAudit(currentId.value!, rejectForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
    }).catch(() => {})
  })
}

getList()
</script>

<style scoped>
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
