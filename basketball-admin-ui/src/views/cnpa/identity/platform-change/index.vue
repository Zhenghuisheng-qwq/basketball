<!--
Menu SQL (run separately, parent_id=2151 账号与认证):
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component,
  query, route_name, is_frame, is_cache, menu_type, visible, status,
  perms, icon, create_by, create_time, remark
) VALUES
(2211, '平台游戏ID变更', 2151, 5, 'platform-change', 'cnpa/identity/platform-change/index', '', 'BasketPlayerPlatformChange', 1, 0, 'C', '0', '0', 'basket:playerPlatformChange:list', 'edit', 'admin', sysdate(), 'CNPA平台游戏ID变更审核');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2211);
-->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="申请编号" prop="requestNo">
        <el-input v-model="queryParams.requestNo" placeholder="请输入申请编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" placeholder="请输入邮箱" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员编号" prop="playerNo">
        <el-input v-model="queryParams.playerNo" placeholder="请输入球员编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="游戏平台" clearable style="width: 200px">
          <el-option v-for="item in GAME_PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="变更类型" prop="changeType">
        <el-select v-model="queryParams.changeType" placeholder="变更类型" clearable style="width: 200px">
          <el-option v-for="item in PLATFORM_CHANGE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" :disabled="batchMultiple" @click="handleBatchApprove" v-hasPermi="['basket:playerPlatformChange:audit']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Close" :disabled="batchMultiple" @click="openBatchReject" v-hasPermi="['basket:playerPlatformChange:audit']">批量驳回</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" :selectable="rowSelectable" />
      <el-table-column label="申请编号" prop="requestNo" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="用户" min-width="180">
        <template #default="scope">
          <div>{{ scope.row.nickname || '-' }}</div>
          <div class="sub-text">{{ scope.row.email || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="球员编号" prop="playerNo" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="变更类型" prop="changeType" width="110">
        <template #default="scope">
          <dict-tag :options="PLATFORM_CHANGE_TYPE_OPTIONS" :value="scope.row.changeType" />
        </template>
      </el-table-column>
      <el-table-column label="平台" prop="platform" width="90" />
      <el-table-column label="原游戏ID" prop="oldDisplayGameId" min-width="120" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.oldDisplayGameId || '-' }}</template>
      </el-table-column>
      <el-table-column label="新游戏ID" prop="newDisplayGameId" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="requestStatusName" width="100">
        <template #default="scope">
          <el-tag :type="platformChangeTagType(scope.row.requestStatus)" size="small">
            {{ scope.row.requestStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:playerPlatformChange:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:playerPlatformChange:audit']" v-if="scope.row.requestStatus === 0">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:playerPlatformChange:audit']" v-if="scope.row.requestStatus === 0">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="平台变更详情" size="640px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="申请编号">{{ detail.requestNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.requestStatusName }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="球员编号">{{ detail.playerNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="变更类型">{{ detail.changeTypeName || detail.changeType }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ detail.platform || '-' }}</el-descriptions-item>
          <el-descriptions-item label="原游戏ID">{{ detail.oldDisplayGameId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="新游戏ID">{{ detail.newDisplayGameId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ parseTime(detail.reviewedTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核备注" :span="2">{{ detail.reviewRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">证明材料</el-divider>
        <div v-if="detail.proofFileId" class="proof-actions">
          <el-button link type="primary" @click="handlePreviewFile(detail.proofFileId)">预览证明材料</el-button>
        </div>
        <el-empty v-else description="暂无证明材料" />
      </template>
    </el-drawer>

    <el-dialog title="审核驳回" v-model="rejectOpen" width="480px" append-to-body>
      <el-form ref="rejectRef" :model="rejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请填写驳回原因，将展示给申请人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="批量驳回平台变更" v-model="batchRejectOpen" width="480px" append-to-body>
      <el-form ref="batchRejectRef" :model="batchRejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input
            v-model="batchRejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请填写驳回原因，将展示给申请人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitBatchReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketPlayerPlatformChange">
import {
  approvePlayerPlatformChange,
  batchApprovePlayerPlatformChange,
  batchRejectPlayerPlatformChange,
  getPlayerPlatformChange,
  listPlayerPlatformChange,
  rejectPlayerPlatformChange
} from '@/api/basket/playerPlatformChange'
import { previewPrivateFile } from '@/api/basket/privateFile'
import { buildBatchAuditPayload, formatBatchAuditMessage, useBatchAuditSelection } from '@/utils/batchAudit'
import type {
  PlayerPlatformChangeDetail,
  PlayerPlatformChangeListItem,
  PlayerPlatformChangeQuery,
  PlayerPlatformChangeRejectReq
} from '@/types'
import {
  GAME_PLATFORM_OPTIONS,
  CNPA_AUDIT_STATUS_FILTER_OPTIONS,
  PLATFORM_CHANGE_TYPE_OPTIONS
} from '@/types'

const { proxy } = getCurrentInstance()
const tableRef = ref()

const dataList = ref<PlayerPlatformChangeListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<PlayerPlatformChangeDetail>()
const currentId = ref<number>()
const rejectOpen = ref(false)
const batchRejectOpen = ref(false)

const queryParams = ref<PlayerPlatformChangeQuery>({
  pageNum: 1,
  pageSize: 10,
  auditStage: 'PENDING'
})

const rejectForm = ref<PlayerPlatformChangeRejectReq>({ rejectReason: '' })
const batchRejectForm = ref<PlayerPlatformChangeRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '请填写驳回原因，将告知申请人', trigger: 'blur' }]
}

const {
  multiple: batchMultiple,
  handleSelectionChange,
  rowSelectable,
  clearSelection,
  getAuditableIds
} = useBatchAuditSelection<PlayerPlatformChangeListItem>({
  getRowId: row => row.id,
  canAudit: row => row.requestStatus === 0
})

function platformChangeTagType(status?: number) {
  if (status === 1) return 'success'
  if (status === 0) return 'warning'
  if (status === 2) return 'danger'
  return 'info'
}

function getList() {
  loading.value = true
  listPlayerPlatformChange(queryParams.value).then(response => {
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

function handleDetail(row: PlayerPlatformChangeListItem) {
  getPlayerPlatformChange(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handlePreviewFile(fileId?: number) {
  if (fileId) {
    previewPrivateFile(fileId)
  }
}

function handleApprove(row: PlayerPlatformChangeListItem) {
  approvePlayerPlatformChange(row.id, {}).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
  })
}

function openReject(row: PlayerPlatformChangeListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    rejectPlayerPlatformChange(currentId.value, rejectForm.value).then(() => {
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
  proxy.$modal.confirm(`确认批量通过 ${ids.length} 条平台变更申请？`).then(() =>
    batchApprovePlayerPlatformChange(buildBatchAuditPayload(ids))
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
  batchRejectForm.value = { rejectReason: '' }
  batchRejectOpen.value = true
}

function submitBatchReject() {
  proxy.$refs.batchRejectRef.validate((valid: boolean) => {
    if (!valid) return
    const ids = getAuditableIds()
    if (!ids.length) return
    batchRejectPlayerPlatformChange(buildBatchAuditPayload(ids, {
      rejectReason: batchRejectForm.value.rejectReason
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

.detail-desc {
  margin-bottom: 8px;
}

.proof-actions {
  min-height: 40px;
  display: flex;
  align-items: center;
}
</style>
