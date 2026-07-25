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
      <el-form-item label="状态" prop="applicationStatus">
        <el-select v-model="queryParams.applicationStatus" placeholder="认证状态" clearable style="width: 200px">
          <el-option v-for="item in PLAYER_CERT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="申请编号" prop="applicationNo" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="用户" min-width="180">
        <template #default="scope">
          <div class="user-cell">
            <el-avatar v-if="scope.row.avatarUrl" :src="scope.row.avatarUrl" :size="32" />
            <el-avatar v-else :size="32">{{ (scope.row.nickname || '?').slice(0, 1) }}</el-avatar>
            <div class="user-meta">
              <div>{{ scope.row.nickname || '-' }}</div>
              <div class="sub-text">{{ scope.row.email || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="游戏平台" prop="gameTerminal" width="100" />
      <el-table-column label="游戏ID" prop="gameAccount" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="位置" prop="positionCode" width="80" />
      <el-table-column label="状态" prop="applicationStatus" width="100">
        <template #default="scope">
          <dict-tag :options="PLAYER_CERT_STATUS_OPTIONS" :value="scope.row.applicationStatus" />
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
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:playerCert:query']">详情</el-button>
          <el-button link type="warning" icon="EditPen" @click="handleStartReview(scope.row)" v-hasPermi="['basket:playerCert:audit']" v-if="scope.row.applicationStatus === 1">开始审核</el-button>
          <el-button link type="success" icon="Check" @click="openApprove(scope.row)" v-hasPermi="['basket:playerCert:audit']" v-if="[1, 2].includes(scope.row.applicationStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:playerCert:audit']" v-if="scope.row.applicationStatus === 2">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球员认证详情" size="720px" append-to-body>
      <template v-if="detail">
        <div class="detail-header">
          <el-avatar v-if="detail.avatarUrl" :src="detail.avatarUrl" :size="56" />
          <el-avatar v-else :size="56">{{ (detail.nickname || '?').slice(0, 1) }}</el-avatar>
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
          <el-descriptions-item label="常用位置">{{ detail.positionCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submittedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ parseTime(detail.reviewedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ detail.reviewRemark || '-' }}</el-descriptions-item>
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

    <el-dialog title="审核通过" v-model="approveOpen" width="480px" append-to-body>
      <el-form ref="approveRef" :model="approveForm" label-width="88px">
        <el-form-item label="位置编码">
          <el-input v-model="approveForm.positionCode" placeholder="可选，覆盖申请位置" />
        </el-form-item>
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

<script setup lang="ts" name="BasketPlayerCert">
import {
  approvePlayerCert,
  getPlayerCert,
  listPlayerCert,
  listPlayerCertAuditRecords,
  rejectPlayerCert,
  startPlayerCertReview
} from '@/api/basket/playerCert'
import { previewPrivateFile } from '@/api/basket/privateFile'
import type {
  PlayerCertApproveReq,
  PlayerCertAuditRecord,
  PlayerCertDetail,
  PlayerCertListItem,
  PlayerCertMaterial,
  PlayerCertQuery,
  PlayerCertRejectReq
} from '@/types'
import { PLAYER_CERT_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

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
const approveOpen = ref(false)
const rejectOpen = ref(false)

const queryParams = ref<PlayerCertQuery>({
  pageNum: 1,
  pageSize: 10,
  applicationNo: undefined,
  nickname: undefined,
  email: undefined,
  gameAccount: undefined,
  applicationStatus: undefined
})

const approveForm = ref<PlayerCertApproveReq>({})
const rejectForm = ref<PlayerCertRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const proofMaterials = computed(() => materials.value)

function isImage(mimeType?: string) {
  return !!mimeType && mimeType.startsWith('image/')
}

function getList() {
  loading.value = true
  listPlayerCert(queryParams.value).then(response => {
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

function handleStartReview(row: PlayerCertListItem) {
  proxy.$modal.confirm('确认开始审核该申请？').then(() => startPlayerCertReview(row.id)).then(() => {
    proxy.$modal.msgSuccess('已开始审核')
    getList()
  }).catch(() => {})
}

function openApprove(row: PlayerCertListItem) {
  currentId.value = row.id
  approveForm.value = { auditRemark: '' }
  approveOpen.value = true
}

function submitApprove() {
  if (!currentId.value) return
  approvePlayerCert(currentId.value, approveForm.value).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    approveOpen.value = false
    getList()
  })
}

function openReject(row: PlayerCertListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    rejectPlayerCert(currentId.value, rejectForm.value).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
    })
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
