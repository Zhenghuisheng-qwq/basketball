<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="赛季ID">
        <el-input-number v-model="query.seasonId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item label="提交状态">
        <el-select v-model="query.submissionStatus" clearable style="width: 140px">
          <el-option label="审核中" :value="1" />
          <el-option label="已通过" :value="2" />
          <el-option label="已驳回" :value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:matchAudit:list']">查询</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="Check" :disabled="batchMultiple" @click="handleBatchApprove" v-hasPermi="['basket:matchAudit:approve']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Close" :disabled="batchMultiple" @click="openBatchReject" v-hasPermi="['basket:matchAudit:reject']">批量驳回</el-button>
      </el-col>
    </el-row>

    <el-table ref="tableRef" :data="rows" v-loading="loading" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" :selectable="rowSelectable" />
      <el-table-column label="提交编号" prop="submissionNo" min-width="140" />
      <el-table-column label="比赛编号" prop="matchNo" min-width="120" />
      <el-table-column label="版本" prop="versionNo" width="70" />
      <el-table-column label="主队" prop="homeTeamName" min-width="110" />
      <el-table-column label="客队" prop="awayTeamName" min-width="110" />
      <el-table-column label="比分" width="90">
        <template #default="scope">{{ scope.row.homeScore }} - {{ scope.row.awayScore }}</template>
      </el-table-column>
      <el-table-column label="提交球队" prop="submitterTeamName" min-width="110" />
      <el-table-column label="提交人" prop="submitterMemberName" min-width="100" />
      <el-table-column label="状态" prop="submissionStatusName" width="100" />
      <el-table-column label="提交时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.submittedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.submissionId)">详情/审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="detailOpen" title="比赛数据提交审核" width="920px" top="5vh">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small" class="mb8">
          <el-descriptions-item label="提交编号">{{ detail.submissionNo }}</el-descriptions-item>
          <el-descriptions-item label="版本">v{{ detail.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.submissionStatusName }}</el-descriptions-item>
          <el-descriptions-item label="比赛">{{ detail.homeTeamName }} vs {{ detail.awayTeamName }}</el-descriptions-item>
          <el-descriptions-item label="比分">{{ detail.homeScore }} - {{ detail.awayScore }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.submitterMemberName }} / {{ detail.submitterTeamName }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">分节比分</el-divider>
        <el-table :data="detail.periods || []" size="small" border>
          <el-table-column label="节次" prop="periodNo" width="70" />
          <el-table-column label="类型" prop="periodType" width="90" />
          <el-table-column label="主队" prop="homeScore" />
          <el-table-column label="客队" prop="awayScore" />
        </el-table>

        <el-divider content-position="left">球队数据</el-divider>
        <el-table :data="detail.teamStats || []" size="small" border>
          <el-table-column label="球队ID" prop="teamId" width="90" />
          <el-table-column label="得分" prop="points" width="70" />
          <el-table-column label="篮板" prop="rebounds" width="70" />
          <el-table-column label="助攻" prop="assists" width="70" />
        </el-table>

        <el-divider content-position="left">球员数据</el-divider>
        <el-table :data="detail.playerStats || []" size="small" border max-height="240">
          <el-table-column label="球员ID" prop="playerId" width="90" />
          <el-table-column label="球队ID" prop="teamId" width="90" />
          <el-table-column label="得分" prop="points" width="70" />
          <el-table-column label="篮板" prop="rebounds" width="70" />
          <el-table-column label="助攻" prop="assists" width="70" />
        </el-table>

        <el-divider content-position="left">材料</el-divider>
        <div class="material-preview-list">
          <div v-for="m in detail.materials || []" :key="m.fileId" class="material-preview-item">
            <p>{{ m.materialType }} · {{ m.materialName || m.fileId }}</p>
            <el-image
              v-if="m.fileUrl"
              :src="m.fileUrl"
              :preview-src-list="[m.fileUrl]"
              fit="cover"
              style="width: 160px; height: 100px"
            />
            <span v-else class="text-muted">fileId: {{ m.fileId }}</span>
          </div>
        </div>

        <el-divider content-position="left">版本历史</el-divider>
        <el-table :data="detail.versions || []" size="small" border max-height="160">
          <el-table-column label="版本" prop="versionNo" width="70" />
          <el-table-column label="状态" prop="submissionStatus" width="90" />
          <el-table-column label="提交时间" prop="submittedTime" width="170" />
          <el-table-column label="驳回原因" prop="rejectReason" />
        </el-table>

        <el-divider content-position="left">审核记录</el-divider>
        <el-table :data="detail.auditLogs || []" size="small" border max-height="160">
          <el-table-column label="动作" prop="auditAction" width="100" />
          <el-table-column label="状态" prop="auditStatus" width="100" />
          <el-table-column label="审核人" prop="auditorName" width="120" />
          <el-table-column label="时间" prop="auditTime" width="170" />
        </el-table>

        <el-form v-if="detail.canAudit" class="mt8">
          <el-form-item label="审核备注">
            <el-input v-model="auditRemark" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <el-alert v-else-if="detail.cannotAuditReason" :title="detail.cannotAuditReason" type="info" show-icon :closable="false" />
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关闭</el-button>
        <el-button type="danger" :disabled="!detail?.canAudit" @click="openReject" v-hasPermi="['basket:matchAudit:reject']">驳回</el-button>
        <el-button type="success" :disabled="!detail?.canAudit" :loading="approving" @click="submitApprove" v-hasPermi="['basket:matchAudit:approve']">审核通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectOpen" title="驳回比赛数据" width="480px" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="rejecting">确认驳回</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="batchRejectOpen" title="批量驳回比赛数据" width="480px" append-to-body>
      <el-input v-model="batchRejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="batchRejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitBatchReject" :loading="batchRejecting">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageSubmissionAudit,
  getSubmissionDetail,
  approveSubmission,
  rejectSubmission,
  batchApproveSubmission,
  batchRejectSubmission
} from '@/api/basket/matchAudit'
import { parseTime } from '@/utils/ruoyi'
import { buildBatchAuditPayload, formatBatchAuditMessage, useBatchAuditSelection } from '@/utils/batchAudit'

const tableRef = ref()
const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = reactive({
  seasonId: undefined as number | undefined,
  submissionStatus: 1 as number | undefined,
  pageNum: 1,
  pageSize: 10
})

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const currentSubmissionId = ref<number>()
const auditRemark = ref('')
const approving = ref(false)

const rejectOpen = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)

const batchRejectOpen = ref(false)
const batchRejectReason = ref('')
const batchRejecting = ref(false)

const {
  multiple: batchMultiple,
  handleSelectionChange,
  rowSelectable,
  clearSelection,
  getAuditableIds
} = useBatchAuditSelection<any>({
  getRowId: row => row.submissionId,
  canAudit: row => row.submissionStatus === 1
})

async function loadList() {
  loading.value = true
  try {
    const res = await pageSubmissionAudit({ ...query })
    rows.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function openDetail(submissionId: number) {
  currentSubmissionId.value = submissionId
  detailOpen.value = true
  detailLoading.value = true
  auditRemark.value = ''
  try {
    const res = await getSubmissionDetail(submissionId)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function submitApprove() {
  if (!currentSubmissionId.value) return
  await ElMessageBox.confirm('确认审核通过并同步正式数据？', '提示', { type: 'warning' })
  approving.value = true
  try {
    await approveSubmission(currentSubmissionId.value, { auditRemark: auditRemark.value || undefined })
    ElMessage.success('审核通过')
    detailOpen.value = false
    loadList()
  } finally {
    approving.value = false
  }
}

function openReject() {
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!currentSubmissionId.value) return
  rejecting.value = true
  try {
    await rejectSubmission(currentSubmissionId.value, {
      rejectReason: rejectReason.value.trim(),
      auditRemark: auditRemark.value || undefined
    })
    ElMessage.success('已驳回')
    rejectOpen.value = false
    detailOpen.value = false
    loadList()
  } finally {
    rejecting.value = false
  }
}

async function handleBatchApprove() {
  const ids = getAuditableIds()
  if (!ids.length) {
    ElMessage.warning('请选择待审核记录')
    return
  }
  try {
    await ElMessageBox.confirm(`确认批量通过 ${ids.length} 条比赛数据提交？`, '提示', { type: 'warning' })
    const res = await batchApproveSubmission(buildBatchAuditPayload(ids))
    ElMessage.success(formatBatchAuditMessage(res.data))
    clearSelection()
    tableRef.value?.clearSelection()
    loadList()
  } catch {
    // cancelled or failed
  }
}

function openBatchReject() {
  const ids = getAuditableIds()
  if (!ids.length) {
    ElMessage.warning('请选择待审核记录')
    return
  }
  batchRejectReason.value = ''
  batchRejectOpen.value = true
}

async function submitBatchReject() {
  if (!batchRejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  const ids = getAuditableIds()
  if (!ids.length) return
  batchRejecting.value = true
  try {
    const res = await batchRejectSubmission({
      ids,
      rejectReason: batchRejectReason.value.trim()
    })
    ElMessage.success(formatBatchAuditMessage(res.data))
    batchRejectOpen.value = false
    clearSelection()
    tableRef.value?.clearSelection()
    loadList()
  } finally {
    batchRejecting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.material-preview-list { display: flex; flex-wrap: wrap; gap: 12px; }
.material-preview-item { border: 1px solid #eee; padding: 8px; border-radius: 4px; }
.text-muted { color: #999; font-size: 12px; }
</style>
