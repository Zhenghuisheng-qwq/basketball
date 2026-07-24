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
      <el-form-item label="状态" prop="registrationStatus">
        <el-select v-model="queryParams.registrationStatus" placeholder="报名状态" clearable style="width: 200px">
          <el-option v-for="item in REGISTRATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="赛季ID" prop="seasonId" width="90" />
      <el-table-column label="球队ID" prop="teamId" width="90" />
      <el-table-column label="状态" prop="registrationStatus" width="100">
        <template #default="scope">
          <dict-tag :options="REGISTRATION_STATUS_OPTIONS" :value="scope.row.registrationStatus" />
        </template>
      </el-table-column>
      <el-table-column label="阵容人数" prop="lineupCount" width="90" />
      <el-table-column label="提交时间" prop="submittedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submittedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:registrationAudit:query']">详情</el-button>
          <el-button link type="info" icon="Refresh" @click="handleRevalidate(scope.row)" v-hasPermi="['basket:registrationAudit:audit']">重新校验</el-button>
          <el-button link type="warning" icon="EditPen" @click="handleStartReview(scope.row)" v-hasPermi="['basket:registrationAudit:audit']" v-if="scope.row.registrationStatus === 1">开始审核</el-button>
          <el-button link type="success" icon="Check" @click="openApprove(scope.row)" v-hasPermi="['basket:registrationAudit:audit']" v-if="[1, 2].includes(scope.row.registrationStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:registrationAudit:audit']" v-if="[1, 2].includes(scope.row.registrationStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="报名审核详情" size="760px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="报名编号">{{ detail.registrationNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.registrationStatusName }}</el-descriptions-item>
        <el-descriptions-item label="赛季ID">{{ detail.seasonId }}</el-descriptions-item>
        <el-descriptions-item label="球队ID">{{ detail.teamId }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ detail.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detail.contactMobile }}</el-descriptions-item>
        <el-descriptions-item label="阵容人数">{{ detail.lineupCount }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ parseTime(detail.submittedTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="mb8 mt16">
        <el-button type="primary" plain icon="Refresh" @click="loadRoster" v-hasPermi="['basket:registrationAudit:audit']">重新校验阵容</el-button>
      </div>

      <el-divider content-position="left">报名阵容</el-divider>
      <el-table :data="roster" size="small" v-loading="rosterLoading">
        <el-table-column label="球员ID" prop="playerId" width="90" />
        <el-table-column label="会员ID" prop="memberId" width="90" />
        <el-table-column label="角色" prop="rosterRole" width="90" />
        <el-table-column label="球衣号" prop="jerseyNo" width="80" />
        <el-table-column label="队长" prop="isCaptain" width="70">
          <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="校验状态" prop="validationStatus" width="100">
          <template #default="scope">
            <dict-tag :options="REGISTRATION_VALIDATION_STATUS_OPTIONS" :value="scope.row.validationStatus" />
          </template>
        </el-table-column>
        <el-table-column label="失败原因" prop="rejectReason" min-width="160" />
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

<script setup lang="ts" name="BasketRegistrationAudit">
import {
  approveRegistrationAudit,
  getRegistrationAudit,
  listRegistrationAudit,
  listRegistrationAuditRecords,
  rejectRegistrationAudit,
  revalidateRegistrationAudit,
  startRegistrationAuditReview
} from '@/api/basket/registrationAudit'
import type {
  RegistrationAuditApproveReq,
  RegistrationAuditDetail,
  RegistrationAuditListItem,
  RegistrationAuditQuery,
  RegistrationAuditRecord,
  RegistrationAuditRejectReq,
  RegistrationPlayer
} from '@/types'
import { REGISTRATION_STATUS_OPTIONS, REGISTRATION_VALIDATION_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<RegistrationAuditListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<RegistrationAuditDetail>()
const roster = ref<RegistrationPlayer[]>([])
const rosterLoading = ref(false)
const auditRecords = ref<RegistrationAuditRecord[]>([])
const currentId = ref<number>()
const approveOpen = ref(false)
const rejectOpen = ref(false)
const approveForm = ref<RegistrationAuditApproveReq>({})
const rejectForm = ref<RegistrationAuditRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const queryParams = ref<RegistrationAuditQuery>({
  pageNum: 1,
  pageSize: 10
})

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
  handleQuery()
}

function loadRoster() {
  if (!currentId.value) return
  rosterLoading.value = true
  revalidateRegistrationAudit(currentId.value).then(res => {
    roster.value = res.data || []
    proxy.$modal.msgSuccess('校验完成')
  }).finally(() => {
    rosterLoading.value = false
  })
}

function handleDetail(row: RegistrationAuditListItem) {
  currentId.value = row.id
  detailOpen.value = true
  roster.value = []
  auditRecords.value = []
  getRegistrationAudit(row.id).then(res => {
    detail.value = res.data
  })
  listRegistrationAuditRecords(row.id).then(res => {
    auditRecords.value = res.data || []
  })
  loadRoster()
}

function handleRevalidate(row: RegistrationAuditListItem) {
  currentId.value = row.id
  rosterLoading.value = true
  revalidateRegistrationAudit(row.id).then(res => {
    roster.value = res.data || []
    proxy.$modal.msgSuccess('校验完成')
  }).finally(() => {
    rosterLoading.value = false
  })
}

function handleStartReview(row: RegistrationAuditListItem) {
  proxy.$modal.confirm('确认开始审核该报名？').then(() => startRegistrationAuditReview(row.id)).then(() => {
    proxy.$modal.msgSuccess('已开始审核')
    getList()
  }).catch(() => {})
}

function openApprove(row: RegistrationAuditListItem) {
  currentId.value = row.id
  approveForm.value = { auditRemark: '' }
  approveOpen.value = true
}

function submitApprove() {
  if (!currentId.value) return
  proxy.$modal.confirm('确认通过该报名审核？').then(() =>
    approveRegistrationAudit(currentId.value!, approveForm.value)
  ).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    approveOpen.value = false
    getList()
  }).catch(() => {})
}

function openReject(row: RegistrationAuditListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    proxy.$modal.confirm('确认驳回该报名审核？').then(() =>
      rejectRegistrationAudit(currentId.value!, rejectForm.value)
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
.mt16 {
  margin-top: 16px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
