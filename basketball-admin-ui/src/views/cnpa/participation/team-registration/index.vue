<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="报名编号" prop="registrationNo">
        <el-input v-model="queryParams.registrationNo" placeholder="请输入报名编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛季ID" prop="seasonId">
        <el-input v-model="queryParams.seasonId" placeholder="请输入赛季ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球队ID" prop="teamId">
        <el-input v-model="queryParams.teamId" placeholder="请输入球队ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
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
      <el-col :span="24">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="球队报名审核"
          description="用于处理球队报名审核，审核通过后将生成正式赛事球队与正式参赛球员快照。"
        />
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" :disabled="batchMultiple" @click="handleBatchApprove" v-hasPermi="['basket:registrationAudit:audit']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Close" :disabled="batchMultiple" @click="openBatchReject" v-hasPermi="['basket:registrationAudit:audit']">批量驳回</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" :selectable="rowSelectable" />
      <el-table-column label="报名编号" prop="registrationNo" min-width="140" />
      <el-table-column label="赛事名称" prop="tournamentName" min-width="140" show-overflow-tooltip />
      <el-table-column label="球队名称" prop="teamName" min-width="120" show-overflow-tooltip />
      <el-table-column label="队长" prop="captainName" min-width="100" show-overflow-tooltip />
      <el-table-column label="阵容人数" prop="lineupCount" width="90" />
      <el-table-column label="状态" prop="registrationStatusName" width="100">
        <template #default="scope">
          <el-tag :type="registrationAuditTagType(scope.row.registrationStatus)" size="small">
            {{ scope.row.registrationStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submittedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submittedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:registrationAudit:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:registrationAudit:audit']" v-if="canRegistrationAudit(scope.row.registrationStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:registrationAudit:audit']" v-if="canRegistrationAudit(scope.row.registrationStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="报名审核详情" size="760px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="报名编号">{{ detail.registrationNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.registrationStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛事名称">{{ detail.tournamentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球队名称">{{ detail.teamName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="队长">{{ detail.captainName || detail.contactName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="阵容人数">{{ detail.lineupCount }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ parseTime(detail.submittedTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ parseTime(detail.auditTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">{{ detail.rejectReason }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">报名阵容</el-divider>
      <el-table :data="roster" size="small">
        <el-table-column label="球员昵称" prop="playerNickname" min-width="100" />
        <el-table-column label="球员编号" prop="playerNo" min-width="100" />
        <el-table-column label="位置" prop="positionText" min-width="120">
          <template #default="scope">
            {{ scope.row.positionText || formatPosition(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="球衣号" prop="jerseyNo" width="80" />
        <el-table-column label="球队角色" width="90">
          <template #default="scope">{{ scope.row.isCaptain === 1 ? '队长' : '队员' }}</template>
        </el-table-column>
        <el-table-column label="阵容角色" width="90">
          <template #default="scope">{{ scope.row.isStarting === 1 ? '首发' : '替补' }}</template>
        </el-table-column>
        <el-table-column label="球员状态" prop="validationStatus" width="100">
          <template #default="scope">
            <dict-tag :options="REGISTRATION_VALIDATION_STATUS_OPTIONS" :value="scope.row.validationStatus" />
          </template>
        </el-table-column>
      </el-table>

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

    <el-dialog title="批量驳回报名审核" v-model="batchRejectOpen" width="480px" append-to-body>
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

<script setup lang="ts" name="BasketRegistrationAudit">
import {
  approveRegistrationAudit,
  batchApproveRegistrationAudit,
  batchRejectRegistrationAudit,
  getRegistrationAudit,
  listRegistrationAudit,
  listRegistrationAuditRecords,
  rejectRegistrationAudit
} from '@/api/basket/registrationAudit'
import { buildBatchAuditPayload, formatBatchAuditMessage, useBatchAuditSelection } from '@/utils/batchAudit'
import type {
  RegistrationAuditDetail,
  RegistrationAuditListItem,
  RegistrationAuditQuery,
  RegistrationAuditRecord,
  RegistrationAuditRejectReq,
  RegistrationPlayer
} from '@/types'
import { CNPA_AUDIT_STATUS_FILTER_OPTIONS, REGISTRATION_VALIDATION_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()
const tableRef = ref()

const dataList = ref<RegistrationAuditListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<RegistrationAuditDetail>()
const roster = ref<RegistrationPlayer[]>([])
const auditRecords = ref<RegistrationAuditRecord[]>([])
const currentId = ref<number>()
const rejectOpen = ref(false)
const batchRejectOpen = ref(false)
const rejectForm = ref<RegistrationAuditRejectReq>({ rejectReason: '' })
const batchRejectForm = ref<RegistrationAuditRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const {
  multiple: batchMultiple,
  handleSelectionChange,
  rowSelectable,
  clearSelection,
  getAuditableIds
} = useBatchAuditSelection<RegistrationAuditListItem>({
  getRowId: row => row.id,
  canAudit: row => canRegistrationAudit(row.registrationStatus)
})

const queryParams = ref<RegistrationAuditQuery>({
  pageNum: 1,
  pageSize: 10,
  auditStage: 'PENDING'
})

function canRegistrationAudit(status?: number) {
  return status === 1 || status === 2
}

function registrationAuditTagType(status?: number) {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 4) return 'danger'
  if (status === 5 || status === 6) return 'info'
  return 'info'
}

function formatPosition(row: RegistrationPlayer) {
  const POSITION_LABELS: Record<string, string> = {
    PG: '控球后卫',
    SG: '得分后卫',
    SF: '小前锋',
    PF: '大前锋',
    C: '中锋',
  }
  const code = row.positionCode?.trim().toUpperCase()
  if (!code) return '-'
  const name = row.positionName || POSITION_LABELS[code]
  return name ? `${code}：${name}` : code
}

function getList() {
  loading.value = true
  listRegistrationAudit(queryParams.value).then(response => {
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

function handleDetail(row: RegistrationAuditListItem) {
  currentId.value = row.id
  detailOpen.value = true
  roster.value = []
  auditRecords.value = []
  getRegistrationAudit(row.id).then(res => {
    detail.value = res.data
    roster.value = res.data?.players || []
  })
  listRegistrationAuditRecords(row.id).then(res => {
    auditRecords.value = res.data || []
  })
}

function handleApprove(row: RegistrationAuditListItem) {
  currentId.value = row.id
  approveRegistrationAudit(row.id).then(() => {
    proxy.$modal.msgSuccess('审核通过成功')
    getList()
    if (detailOpen.value && detail.value?.id === row.id) {
      handleDetail(row)
    }
  })
}

function openReject(row: RegistrationAuditListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    rejectRegistrationAudit(currentId.value!, rejectForm.value).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
      if (detailOpen.value && detail.value?.id === currentId.value) {
        getRegistrationAudit(currentId.value!).then(res => {
          detail.value = res.data
          roster.value = res.data?.players || []
        })
      }
    })
  })
}

function handleBatchApprove() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  batchApproveRegistrationAudit(buildBatchAuditPayload(ids)).then(res => {
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
    batchRejectRegistrationAudit(buildBatchAuditPayload(ids, {
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
.mb8 {
  margin-bottom: 8px;
}
</style>
