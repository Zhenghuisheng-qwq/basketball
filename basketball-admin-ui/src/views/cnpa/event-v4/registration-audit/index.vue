<template>
  <div class="app-container">
    <el-alert title="V4 报名审核：审核通过后生成 EventTeam / EventPlayer 快照" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="queryParams" :inline="true" class="mb8">
      <el-form-item label="Season ID">
        <el-input-number v-model="queryParams.seasonId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="Division ID">
        <el-input-number v-model="queryParams.divisionId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.registrationStatus" clearable style="width: 160px">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队名称">
        <el-input v-model="queryParams.teamName" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="报名ID" prop="registrationId" width="90" />
      <el-table-column label="Season" prop="seasonTitle" min-width="140" />
      <el-table-column label="Division" prop="divisionName" min-width="120" />
      <el-table-column label="平台" prop="platformCode" width="80" />
      <el-table-column label="球队" prop="teamName" min-width="120" />
      <el-table-column label="队长" prop="captainPlayerName" min-width="100" />
      <el-table-column label="人数" prop="lineupCount" width="70" />
      <el-table-column label="状态" prop="registrationStatus" width="120" />
      <el-table-column label="提交时间" prop="submitTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['basket:v4RegistrationAudit:query']">详情</el-button>
          <el-button link type="success" @click="handleApprove(scope.row)" v-hasPermi="['basket:v4RegistrationAudit:approve']" v-if="scope.row.registrationStatus === 'PENDING_REVIEW'">通过</el-button>
          <el-button link type="danger" @click="openReject(scope.row)" v-hasPermi="['basket:v4RegistrationAudit:reject']" v-if="scope.row.registrationStatus === 'PENDING_REVIEW'">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="V4 报名详情" size="760px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报名ID">{{ detail.registrationId }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.registrationStatus }}</el-descriptions-item>
          <el-descriptions-item label="Season">{{ detail.seasonTitle }}</el-descriptions-item>
          <el-descriptions-item label="Division">{{ detail.divisionName }}</el-descriptions-item>
          <el-descriptions-item label="球队">{{ detail.teamName }}</el-descriptions-item>
          <el-descriptions-item label="队长">{{ detail.captainPlayerName }}</el-descriptions-item>
          <el-descriptions-item label="阵容人数">{{ detail.lineupCount }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">阵容</el-divider>
        <el-table :data="detail.players || []" size="small" border>
          <el-table-column label="球员" prop="playerName" />
          <el-table-column label="Game ID" prop="gameIdSnapshot" />
          <el-table-column label="位置" prop="position" />
          <el-table-column label="队长" width="70">
            <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <el-divider content-position="left">审核流水</el-divider>
        <el-table :data="detail.audits || []" size="small" border>
          <el-table-column label="动作" prop="auditAction" width="100" />
          <el-table-column label="结果" prop="auditResult" width="100" />
          <el-table-column label="前状态" prop="beforeStatus" width="120" />
          <el-table-column label="后状态" prop="afterStatus" width="120" />
          <el-table-column label="时间" prop="auditTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" prop="auditRemark" min-width="140" />
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectOpen" title="驳回报名" width="480px" append-to-body>
      <el-form :model="rejectForm" label-width="90px">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectForm.rejectReason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4RegistrationAudit">
import {
  approveV4RegistrationAudit,
  getV4RegistrationAuditDetail,
  listV4RegistrationAudit,
  rejectV4RegistrationAudit,
  type BasketRegistrationAuditDetailVo,
  type BasketRegistrationAuditQuery
} from '@/api/cnpa/event-v4/registration-audit'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const total = ref(0)
const dataList = ref<any[]>([])
const detailOpen = ref(false)
const rejectOpen = ref(false)
const detail = ref<BasketRegistrationAuditDetailVo>()
const currentId = ref<number>()
const rejectForm = reactive({ rejectReason: '' })
const queryParams = reactive<BasketRegistrationAuditQuery>({ pageNum: 1, pageSize: 10 })
const STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已撤回', value: 'WITHDRAWN' }
]

function getList() {
  loading.value = true
  listV4RegistrationAudit(queryParams).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, registrationStatus: undefined, teamName: undefined, captainPlayerName: undefined })
  getList()
}

function openDetail(row: any) {
  getV4RegistrationAuditDetail(row.registrationId).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleApprove(row: any) {
  proxy.$modal.confirm(`确认通过报名 #${row.registrationId}？`).then(() => approveV4RegistrationAudit(row.registrationId)).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
  }).catch(() => {})
}

function openReject(row: any) {
  currentId.value = row.registrationId
  rejectForm.rejectReason = ''
  rejectOpen.value = true
}

function submitReject() {
  if (!rejectForm.rejectReason?.trim()) {
    proxy.$modal.msgError('驳回原因不能为空')
    return
  }
  rejectV4RegistrationAudit(currentId.value!, { rejectReason: rejectForm.rejectReason.trim() }).then(() => {
    proxy.$modal.msgSuccess('已驳回')
    rejectOpen.value = false
    getList()
  })
}

getList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
