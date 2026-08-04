<template>
  <div class="app-container">
    <el-alert title="[已废弃] 旧比赛数据审核，请使用「数据审核」菜单" type="warning" :closable="false" show-icon class="mb8" />
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
      <el-table-column label="版本" prop="versionNo" width="70">
        <template #default="scope">V{{ scope.row.versionNo }}</template>
      </el-table-column>
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

    <MatchDataAuditDetailDialog
      v-model="detailOpen"
      :submission-id="currentSubmissionId"
      @refreshed="loadList"
    />

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
import { ElMessage } from 'element-plus'
import {
  pageSubmissionAudit,
  batchApproveSubmission,
  batchRejectSubmission
} from '@/api/basket/matchAudit'
import MatchDataAuditDetailDialog from '@/components/basket/MatchDataAuditDetailDialog.vue'
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
const currentSubmissionId = ref<number>()

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

function openDetail(submissionId: number) {
  currentSubmissionId.value = submissionId
  detailOpen.value = true
}

async function handleBatchApprove() {
  const ids = getAuditableIds()
  if (!ids.length) {
    ElMessage.warning('请选择待审核记录')
    return
  }
  const res = await batchApproveSubmission(buildBatchAuditPayload(ids))
  ElMessage.success(formatBatchAuditMessage(res.data))
  clearSelection()
  tableRef.value?.clearSelection()
  loadList()
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
</style>
