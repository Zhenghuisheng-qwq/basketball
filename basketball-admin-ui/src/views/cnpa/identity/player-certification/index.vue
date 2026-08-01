<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="申请编号" prop="applicationNo">
        <el-input v-model="queryParams.applicationNo" placeholder="请输入申请编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="请输入用户昵称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" placeholder="请输入用户邮箱" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="游戏账号" prop="gameAccount">
        <el-input v-model="queryParams.gameAccount" placeholder="请输入游戏账号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="auditStage">
        <el-select v-model="queryParams.auditStage" placeholder="审核状态" clearable style="width: 200px">
          <el-option v-for="item in CNPA_AUDIT_STATUS_FILTER_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      class="mb8"
      type="info"
      :closable="false"
      show-icon
      title="游戏平台和游戏ID用于确认球员在游戏中的身份，不代表球员只能参加该平台赛事。"
    />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" :disabled="batchMultiple" @click="handleBatchApprove" v-hasPermi="['basket:playerCert:audit']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Close" :disabled="batchMultiple" @click="openBatchReject" v-hasPermi="['basket:playerCert:audit']">批量驳回</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" :selectable="rowSelectable" />
      <el-table-column label="申请编号" prop="applicationNo" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="用户" min-width="180">
        <template #default="scope">
          <div class="user-cell">
            <el-avatar :src="resolveMemberAvatarUrl(scope.row.avatarUrl)" :size="32">
              {{ (scope.row.nickname || '?').slice(0, 1) }}
            </el-avatar>
            <div class="user-meta">
              <div>{{ scope.row.nickname || '-' }}</div>
              <div class="sub-text">{{ scope.row.email || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="游戏平台" prop="gameTerminal" width="100">
        <template #default="scope">{{ scope.row.gameTerminal || '未选择' }}</template>
      </el-table-column>
      <el-table-column label="游戏ID" prop="gameAccount" min-width="120" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.gameAccount || '未填写' }}</template>
      </el-table-column>
      <el-table-column label="位置" prop="positionCode" width="80">
        <template #default="scope">{{ scope.row.positionCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="applicationStatusName" width="100">
        <template #default="scope">
          <el-tag :type="auditStatusTagType(scope.row.applicationStatus)" size="small">
            {{ scope.row.applicationStatusName || resolvePlayerCertAuditLabel(scope.row.applicationStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submittedTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.submittedTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核时间" prop="reviewedTime" width="160">
        <template #default="scope">
          <span>{{ parseTime(scope.row.reviewedTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:playerCert:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:playerCert:audit']" v-if="canAudit(scope.row.applicationStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:playerCert:audit']" v-if="canAudit(scope.row.applicationStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球员认证详情" size="720px" append-to-body>
      <template v-if="detail">
        <div class="detail-header">
          <el-avatar :src="resolveMemberAvatarUrl(detail.avatarUrl)" :size="56">
            {{ (detail.nickname || '?').slice(0, 1) }}
          </el-avatar>
          <div>
            <div class="detail-name">{{ detail.nickname || '-' }}</div>
            <div class="sub-text">{{ detail.email || '-' }}</div>
          </div>
        </div>

        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="申请编号">{{ detail.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.applicationStatusName }}</el-descriptions-item>
          <el-descriptions-item label="游戏平台">{{ detail.gameTerminal || '-' }}</el-descriptions-item>
          <el-descriptions-item label="游戏ID">{{ detail.gameAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="游戏昵称">{{ detail.gameNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="常用位置">{{ detail.positionCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submittedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ parseTime(detail.reviewedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detailAuditorName }}</el-descriptions-item>
          <el-descriptions-item label="驳回原因" v-if="detail.applicationStatus === 4" :span="2">{{ detail.reviewRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" v-else-if="detail.applicationStatus === 3" :span="2">{{ detail.reviewRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <el-divider content-position="left">游戏账号证明</el-divider>
      <div v-loading="detailLoading">
        <div v-if="proofMaterials.length" class="proof-grid">
          <div v-for="item in proofMaterials" :key="item.materialId || item.fileId" class="proof-item">
            <div class="proof-label">{{ item.materialName || item.materialType || '证明材料' }}</div>
            <el-image
              v-if="item.accessUrl && isImage(item.mimeType)"
              :src="item.accessUrl"
              fit="contain"
              class="proof-image"
              :preview-src-list="[item.accessUrl]"
            />
            <div v-else class="proof-actions">
              <el-button link type="primary" @click="handlePreviewFile(item.fileId)" v-if="item.fileId">预览文件</el-button>
              <span v-else>-</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无认证材料" />
      </div>

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

    <el-dialog title="确认驳回认证？" v-model="rejectOpen" width="480px" append-to-body>
      <el-form ref="rejectRef" :model="rejectForm" label-width="88px">
        <el-form-item label="驳回原因">
          <el-input v-model="rejectForm.rejectReason" type="textarea" :rows="3" placeholder="选填，不填将使用默认原因：认证信息未通过审核" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
    <el-dialog title="批量驳回认证" v-model="batchRejectOpen" width="480px" append-to-body>
      <el-form label-width="88px">
        <el-form-item label="驳回原因">
          <el-input v-model="batchRejectReason" type="textarea" :rows="3" placeholder="选填，不填将使用默认原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitBatchReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketPlayerCert">
import {
  approvePlayerCert,
  batchApprovePlayerCert,
  batchRejectPlayerCert,
  getPlayerCert,
  listPlayerCert,
  listPlayerCertAuditRecords,
  rejectPlayerCert
} from '@/api/basket/playerCert'
import { previewPrivateFile } from '@/api/basket/privateFile'
import type {
  PlayerCertAuditRecord,
  PlayerCertDetail,
  PlayerCertListItem,
  PlayerCertMaterial,
  PlayerCertQuery,
  PlayerCertRejectReq
} from '@/types'
import { CNPA_AUDIT_STATUS_FILTER_OPTIONS } from '@/types'
import { applyTablePage } from '@/utils/table'
import { resolveMemberAvatarUrl } from '@/utils/memberAvatar'
import { buildBatchAuditPayload, formatBatchAuditMessage, useBatchAuditSelection } from '@/utils/batchAudit'

const { proxy } = getCurrentInstance()
const tableRef = ref()

const dataList = ref<PlayerCertListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<PlayerCertDetail>()
const materials = ref<PlayerCertMaterial[]>([])
const auditRecords = ref<PlayerCertAuditRecord[]>([])
const currentId = ref<number>()
const rejectOpen = ref(false)
const batchRejectOpen = ref(false)
const batchRejectReason = ref('')

const {
  multiple: batchMultiple,
  handleSelectionChange,
  rowSelectable,
  clearSelection,
  getAuditableIds
} = useBatchAuditSelection<PlayerCertListItem>({
  getRowId: row => row.id,
  canAudit: row => canAudit(row.applicationStatus)
})

const queryParams = ref<PlayerCertQuery>({
  pageNum: 1,
  pageSize: 10,
  applicationNo: undefined,
  nickname: undefined,
  email: undefined,
  gameAccount: undefined,
  applicationStatus: undefined
})

const rejectForm = ref<PlayerCertRejectReq>({ rejectReason: '' })

const proofMaterials = computed(() => materials.value)

const detailAuditorName = computed(() => {
  const finalRecord = [...auditRecords.value].reverse().find(item =>
    item.auditAction === 'APPROVE' || item.auditAction === 'REJECT'
  )
  return finalRecord?.auditorName || '-'
})

function canAudit(status?: number) {
  return status === 1 || status === 2
}

function resolvePlayerCertAuditLabel(status?: number) {
  if (status === 1 || status === 2) return '待审核'
  if (status === 3) return '已通过'
  if (status === 4) return '已驳回'
  if (status === 5) return '已取消'
  return '-'
}

function auditStatusTagType(status?: number) {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 4) return 'danger'
  return 'info'
}

function isImage(mimeType?: string) {
  return !!mimeType && mimeType.startsWith('image/')
}

function getList() {
  loading.value = true
  listPlayerCert(queryParams.value).then(response => {
    applyTablePage(response, dataList, total)
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

function handleDetail(row: PlayerCertListItem) {
  currentId.value = row.id
  detailOpen.value = true
  detailLoading.value = true
  detail.value = undefined
  materials.value = []
  auditRecords.value = []
  Promise.all([
    getPlayerCert(row.id),
    listPlayerCertAuditRecords(row.id)
  ]).then(([detailRes, auditRes]) => {
    detail.value = detailRes.data
    materials.value = detailRes.data?.materials || []
    auditRecords.value = auditRes.data || []
  }).finally(() => {
    detailLoading.value = false
  })
}

function handlePreviewFile(fileId?: number) {
  if (fileId) {
    previewPrivateFile(fileId)
  }
}

function handleApprove(row: PlayerCertListItem) {
  proxy.$modal.confirm('确认通过认证？通过后该用户将成为已认证球员。').then(() => {
    return approvePlayerCert(row.id, {})
  }).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
    if (detailOpen.value && currentId.value === row.id) {
      handleDetail(row)
    }
  }).catch(() => {})
}

function openReject(row: PlayerCertListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '' }
  rejectOpen.value = true
}

function submitReject() {
  if (!currentId.value) return
  const payload: PlayerCertRejectReq = {}
  if (rejectForm.value.rejectReason?.trim()) {
    payload.rejectReason = rejectForm.value.rejectReason.trim()
  }
  rejectPlayerCert(currentId.value, payload).then(() => {
    proxy.$modal.msgSuccess('已驳回')
    rejectOpen.value = false
    getList()
    if (detailOpen.value) {
      const row = dataList.value.find(item => item.id === currentId.value)
      if (row) {
        handleDetail(row)
      }
    }
  })
}

function handleBatchApprove() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  proxy.$modal.confirm(`确认批量通过 ${ids.length} 条认证申请？`).then(() =>
    batchApprovePlayerCert(buildBatchAuditPayload(ids))
  ).then(res => {
    proxy.$modal.msgSuccess(formatBatchAuditMessage(res.data))
    clearSelection()
    tableRef.value?.clearSelection()
    getList()
  }).catch(() => {})
}

function openBatchReject() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  batchRejectReason.value = ''
  batchRejectOpen.value = true
}

function submitBatchReject() {
  const ids = getAuditableIds()
  if (!ids.length) return
  const payload = buildBatchAuditPayload(ids)
  if (batchRejectReason.value.trim()) {
    payload.rejectReason = batchRejectReason.value.trim()
  }
  batchRejectPlayerCert(payload).then(res => {
    proxy.$modal.msgSuccess(formatBatchAuditMessage(res.data))
    batchRejectOpen.value = false
    clearSelection()
    tableRef.value?.clearSelection()
    getList()
  })
}

getList()
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-meta {
  min-width: 0;
}

.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-name {
  font-size: 16px;
  font-weight: 600;
}

.detail-desc {
  margin-bottom: 8px;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.proof-item {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
}

.proof-label {
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.proof-image {
  width: 100%;
  min-height: 160px;
  border-radius: 4px;
}

.proof-actions {
  min-height: 80px;
  display: flex;
  align-items: center;
}
</style>
