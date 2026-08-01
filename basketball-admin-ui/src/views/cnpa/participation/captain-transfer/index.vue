<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="赛季ID" prop="seasonId">
        <el-input v-model="queryParams.seasonId" placeholder="请输入赛季ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="requestStatus">
        <el-select v-model="queryParams.requestStatus" placeholder="申请状态" clearable style="width: 200px">
          <el-option v-for="item in ROSTER_CHANGE_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
          title="赛事队长移交审核"
          description="用于处理具体赛事中的队长身份移交，队长权限仅在对应赛事范围内生效。"
        />
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" :disabled="batchMultiple" @click="handleBatchApprove" v-hasPermi="['basket:rosterChangeAudit:audit']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Close" :disabled="batchMultiple" @click="openBatchReject" v-hasPermi="['basket:rosterChangeAudit:audit']">批量驳回</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" :selectable="rowSelectable" />
      <el-table-column label="申请编号" prop="requestNo" min-width="140" />
      <el-table-column label="赛季" min-width="140">
        <template #default="scope">
          <div>{{ scope.row.seasonName || '-' }}</div>
          <div class="sub-text">ID: {{ scope.row.seasonId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="原队长" min-width="140">
        <template #default="scope">
          <div>{{ scope.row.playerNickname || '-' }}</div>
          <div class="sub-text">ID: {{ scope.row.playerId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="球队ID" prop="sourceEventTeamId" width="100" />
      <el-table-column label="状态" prop="requestStatus" width="100">
        <template #default="scope">
          <dict-tag :options="ROSTER_CHANGE_STATUS_OPTIONS" :value="scope.row.requestStatus" />
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:rosterChangeAudit:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:rosterChangeAudit:audit']" v-if="scope.row.requestStatus === 0">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:rosterChangeAudit:audit']" v-if="scope.row.requestStatus === 0">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赛事队长移交审核详情" size="720px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="申请编号">{{ detail.requestNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.requestStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName || detail.seasonId }}</el-descriptions-item>
        <el-descriptions-item label="变更类型">{{ detail.changeTypeName || detail.changeType }}</el-descriptions-item>
        <el-descriptions-item label="原队长">{{ detail.playerNickname || detail.playerId }}</el-descriptions-item>
        <el-descriptions-item label="新队长球员ID">{{ detail.newCaptainPlayerId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球队ID">{{ detail.sourceEventTeamId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请人用户ID">{{ detail.applicantMemberId || detail.applicantUserId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ detail.applyReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ parseTime(detail.reviewedTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核备注" :span="2">{{ detail.reviewRemark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="驳回原因" :span="2">{{ detail.rejectReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

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

    <el-dialog title="批量驳回赛事队长移交申请" v-model="batchRejectOpen" width="480px" append-to-body>
      <el-form ref="batchRejectRef" :model="batchRejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input v-model="batchRejectForm.rejectReason" type="textarea" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="batchRejectForm.auditRemark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitBatchReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketCaptainTransferAudit">
import {
  approveRosterChangeAudit,
  batchApproveRosterChangeAudit,
  batchRejectRosterChangeAudit,
  getRosterChangeAudit,
  listRosterChangeAudit,
  rejectRosterChangeAudit
} from '@/api/basket/rosterChange'
import { buildBatchAuditPayload, formatBatchAuditMessage, useBatchAuditSelection } from '@/utils/batchAudit'
import type {
  RosterChangeAuditDetail,
  RosterChangeAuditListItem,
  RosterChangeAuditQuery,
  RosterChangeAuditRejectReq
} from '@/types'
import { ROSTER_CHANGE_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()
const tableRef = ref()

const dataList = ref<RosterChangeAuditListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<RosterChangeAuditDetail>()
const currentId = ref<number>()
const rejectOpen = ref(false)
const batchRejectOpen = ref(false)
const rejectForm = ref<RosterChangeAuditRejectReq>({ rejectReason: '' })
const batchRejectForm = ref<RosterChangeAuditRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const {
  multiple: batchMultiple,
  handleSelectionChange,
  rowSelectable,
  clearSelection,
  getAuditableIds
} = useBatchAuditSelection<RosterChangeAuditListItem>({
  getRowId: row => row.id,
  canAudit: row => row.requestStatus === 0
})

const queryParams = ref<RosterChangeAuditQuery>({
  pageNum: 1,
  pageSize: 10,
  changeType: 'CAPTAIN_TRANSFER'
})

function getList() {
  loading.value = true
  listRosterChangeAudit(queryParams.value).then(response => {
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
  queryParams.value.changeType = 'CAPTAIN_TRANSFER'
  handleQuery()
}

function handleDetail(row: RosterChangeAuditListItem) {
  getRosterChangeAudit(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleApprove(row: RosterChangeAuditListItem) {
  approveRosterChangeAudit(row.id, {}).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
  })
}

function openReject(row: RosterChangeAuditListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    rejectRosterChangeAudit(currentId.value!, rejectForm.value).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
    })
  })
}

function handleBatchApprove() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  batchApproveRosterChangeAudit(buildBatchAuditPayload(ids)).then(res => {
    proxy.$modal.msgSuccess(formatBatchAuditMessage(res.data))
    clearSelection()
    tableRef.value?.clearSelection()
    getList()
  })
}

function openBatchReject() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  batchRejectForm.value = { rejectReason: '', auditRemark: '' }
  batchRejectOpen.value = true
}

function submitBatchReject() {
  proxy.$refs.batchRejectRef.validate((valid: boolean) => {
    if (!valid) return
    const ids = getAuditableIds()
    if (!ids.length) return
    batchRejectRosterChangeAudit(buildBatchAuditPayload(ids, {
      rejectReason: batchRejectForm.value.rejectReason,
      auditRemark: batchRejectForm.value.auditRemark || undefined
    })).then(res => {
      proxy.$modal.msgSuccess(formatBatchAuditMessage(res.data))
      batchRejectOpen.value = false
      clearSelection()
      tableRef.value?.clearSelection()
      getList()
    })
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
