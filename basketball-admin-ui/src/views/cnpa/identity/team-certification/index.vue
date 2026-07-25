<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="球队编号" prop="teamNo">
        <el-input v-model="queryParams.teamNo" placeholder="请输入球队编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球队名称" prop="teamName">
        <el-input v-model="queryParams.teamName" placeholder="请输入球队名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" placeholder="审核状态" clearable style="width: 200px">
          <el-option v-for="item in TEAM_AUDIT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="球队编号" prop="teamNo" min-width="140" />
      <el-table-column label="球队名称" prop="teamName" min-width="140" />
      <el-table-column label="审核状态" prop="auditStatus" width="100">
        <template #default="scope">
          <dict-tag :options="TEAM_AUDIT_STATUS_OPTIONS" :value="scope.row.auditStatus" />
        </template>
      </el-table-column>
      <el-table-column label="成员数" prop="currentMemberCount" width="80" />
      <el-table-column label="创建时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:teamAudit:query']">详情</el-button>
          <el-button link type="warning" icon="EditPen" @click="handleStartReview(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="scope.row.auditStatus === 0">开始审核</el-button>
          <el-button link type="success" icon="Check" @click="openApprove(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="[0, 3].includes(scope.row.auditStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="[0, 3].includes(scope.row.auditStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球队审核详情" size="640px" append-to-body>
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="2" border>
          <el-descriptions-item label="球队编号">{{ detail.teamNo }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ detail.auditStatusName }}</el-descriptions-item>
          <el-descriptions-item label="球队名称">{{ detail.teamName }}</el-descriptions-item>
          <el-descriptions-item label="简称">{{ detail.shortName }}</el-descriptions-item>
          <el-descriptions-item label="地区">{{ detail.regionName }}</el-descriptions-item>
          <el-descriptions-item label="游戏终端">{{ detail.gameTerminal }}</el-descriptions-item>
          <el-descriptions-item label="成员数">{{ detail.currentMemberCount }}</el-descriptions-item>
          <el-descriptions-item label="队长会员ID">{{ detail.captainMemberId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="队长球员ID">{{ detail.captainPlayerId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">校验项</el-divider>
        <el-space direction="vertical" fill style="width: 100%">
          <el-alert :title="validationFlags.captainAssigned ? '已指定队长' : '未指定队长'" :type="validationFlags.captainAssigned ? 'success' : 'error'" :closable="false" />
          <el-alert :title="validationFlags.hasMembers ? '已有球队成员' : '暂无球队成员'" :type="validationFlags.hasMembers ? 'success' : 'warning'" :closable="false" />
          <el-alert title="审核通过时将校验：队长已完成球员认证且具备有效队长资格" type="info" :closable="false" />
        </el-space>

        <el-divider content-position="left">审核记录</el-divider>
        <el-table :data="auditRecords" size="small">
          <el-table-column label="操作" prop="auditAction" width="120" />
          <el-table-column label="审核人" prop="auditorName" width="100" />
          <el-table-column label="备注" prop="auditRemark" min-width="160" />
          <el-table-column label="时间" prop="auditTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
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

<script setup lang="ts" name="BasketTeamAudit">
import {
  approveTeamAudit,
  getTeamAudit,
  listTeamAudit,
  listTeamAuditRecords,
  rejectTeamAudit,
  startTeamAuditReview
} from '@/api/basket/teamAudit'
import type {
  TeamAuditApproveReq,
  TeamAuditDetail,
  TeamAuditListItem,
  TeamAuditQuery,
  TeamAuditRecord,
  TeamAuditRejectReq
} from '@/types'
import { TEAM_AUDIT_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<TeamAuditListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<TeamAuditDetail>()
const auditRecords = ref<TeamAuditRecord[]>([])
const currentId = ref<number>()
const approveOpen = ref(false)
const rejectOpen = ref(false)
const approveForm = ref<TeamAuditApproveReq>({})
const rejectForm = ref<TeamAuditRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const queryParams = ref<TeamAuditQuery>({
  pageNum: 1,
  pageSize: 10
})

const validationFlags = computed(() => ({
  captainAssigned: !!(detail.value?.captainMemberId && detail.value?.captainPlayerId),
  hasMembers: (detail.value?.currentMemberCount || 0) > 0
}))

function getList() {
  loading.value = true
  listTeamAudit(queryParams.value).then(response => {
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

function handleDetail(row: TeamAuditListItem) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = undefined
  auditRecords.value = []
  Promise.all([
    getTeamAudit(row.id),
    listTeamAuditRecords(row.id)
  ]).then(([detailRes, auditRes]) => {
    detail.value = detailRes.data
    auditRecords.value = auditRes.data || []
  }).finally(() => {
    detailLoading.value = false
  })
}

function handleStartReview(row: TeamAuditListItem) {
  proxy.$modal.confirm('确认开始审核该球队？').then(() => startTeamAuditReview(row.id)).then(() => {
    proxy.$modal.msgSuccess('已开始审核')
    getList()
  }).catch(() => {})
}

function openApprove(row: TeamAuditListItem) {
  currentId.value = row.id
  approveForm.value = { auditRemark: '' }
  approveOpen.value = true
}

function submitApprove() {
  if (!currentId.value) return
  proxy.$modal.confirm('确认通过该球队审核？').then(() =>
    approveTeamAudit(currentId.value!, approveForm.value)
  ).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    approveOpen.value = false
    getList()
  }).catch(() => {})
}

function openReject(row: TeamAuditListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    proxy.$modal.confirm('确认驳回该球队审核？').then(() =>
      rejectTeamAudit(currentId.value!, rejectForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
    }).catch(() => {})
  })
}

getList()
</script>
