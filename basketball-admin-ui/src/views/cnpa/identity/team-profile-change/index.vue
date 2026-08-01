<!--
Menu SQL (run separately, parent_id=2151 账号与认证):
INSERT INTO sys_menu (...) VALUES (2215, '球队资料变更审核', 2151, 6, 'team-profile-change', 'cnpa/identity/team-profile-change/index', ...);
-->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="变更编号" prop="changeNo">
        <el-input v-model="queryParams.changeNo" placeholder="请输入变更编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球队名称" prop="teamName">
        <el-input v-model="queryParams.teamName" placeholder="请输入球队名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="提交人" prop="submitUser">
        <el-input v-model="queryParams.submitUser" placeholder="昵称或邮箱" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStage">
        <el-select v-model="queryParams.auditStage" placeholder="审核状态" clearable style="width: 200px">
          <el-option v-for="item in CNPA_AUDIT_STATUS_FILTER_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 320px"
        />
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
      <el-table-column label="变更编号" prop="changeNo" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="球队名称" prop="teamName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="提交人" min-width="160">
        <template #default="scope">
          <div>{{ scope.row.submitUserName || '-' }}</div>
          <div class="sub-text">{{ scope.row.submitUserEmail || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="变更内容" prop="changeSummary" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="审核状态" prop="auditStatusName" width="100">
        <template #default="scope">
          <el-tag :type="auditTagType(scope.row.auditStatus)" size="small">
            {{ scope.row.auditStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" prop="auditTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:teamProfileChange:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:teamProfileChange:audit']" v-if="isPending(scope.row.auditStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:teamProfileChange:audit']" v-if="isPending(scope.row.auditStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球队资料变更详情" size="720px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border class="detail-desc">
          <el-descriptions-item label="变更编号">{{ detail.changeNo }}</el-descriptions-item>
          <el-descriptions-item label="球队名称">{{ detail.teamName }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.submitUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ detail.auditStatusName }}</el-descriptions-item>
          <el-descriptions-item label="审核时间">{{ parseTime(detail.auditTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="compare-grid">
          <div class="compare-col">
            <h4>变更前</h4>
            <p>球队名称：{{ detail.oldTeamName || '-' }}</p>
            <p>球队简称：{{ detail.oldTeamShortName || '-' }}</p>
            <p>球队介绍：{{ detail.oldDescription || '暂无' }}</p>
            <p>公开状态：{{ publicStatusText(detail.oldPublicStatus) }}</p>
            <p>Logo：</p>
            <el-image v-if="detail.oldLogoUrl" :src="detail.oldLogoUrl" fit="contain" style="width: 80px; height: 80px" />
            <span v-else class="sub-text">未设置</span>
          </div>
          <div class="compare-col">
            <h4>变更后</h4>
            <p>球队名称：{{ detail.newTeamName || '-' }}</p>
            <p>球队简称：{{ detail.newTeamShortName || '-' }}</p>
            <p>球队介绍：{{ detail.newDescription || '暂无' }}</p>
            <p>公开状态：{{ publicStatusText(detail.newPublicStatus) }}</p>
            <p>Logo：</p>
            <el-image v-if="detail.newLogoUrl" :src="detail.newLogoUrl" fit="contain" style="width: 80px; height: 80px" />
            <span v-else class="sub-text">未设置</span>
          </div>
        </div>
        <p v-if="detail.auditRemark" class="audit-remark">审核备注：{{ detail.auditRemark }}</p>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectOpen" title="驳回球队资料变更" width="480px" append-to-body>
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectRef" label-width="88px">
        <el-form-item label="驳回原因" prop="auditRemark">
          <el-input v-model="rejectForm.auditRemark" type="textarea" rows="4" placeholder="请填写驳回原因，将告知队长" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketTeamProfileChange">
import { getCurrentInstance, ref } from 'vue'
import {
  approveTeamProfileChange,
  getTeamProfileChange,
  listTeamProfileChange,
  rejectTeamProfileChange
} from '@/api/basket/teamProfileChange'
import type {
  TeamProfileChangeDetail,
  TeamProfileChangeListItem,
  TeamProfileChangeQuery,
  TeamProfileChangeRejectReq
} from '@/types'
import { CNPA_AUDIT_STATUS_FILTER_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<TeamProfileChangeListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<TeamProfileChangeDetail>()
const currentId = ref<number>()
const rejectOpen = ref(false)
const dateRange = ref<string[]>([])

const queryParams = ref<TeamProfileChangeQuery>({
  pageNum: 1,
  pageSize: 10,
  auditStage: 'PENDING'
})

const rejectForm = ref<TeamProfileChangeRejectReq>({ auditRemark: '' })
const rejectRules = {
  auditRemark: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

function isPending(status?: number) {
  return status === 1 || status === 2
}

function auditTagType(status?: number) {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 4) return 'danger'
  return 'info'
}

function publicStatusText(status?: number) {
  return status === 1 ? '公开' : '非公开'
}

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  params.beginTime = dateRange.value?.[0]
  params.endTime = dateRange.value?.[1]
  listTeamProfileChange(params).then(response => {
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
  dateRange.value = []
  proxy.resetForm('queryRef')
  queryParams.value.auditStage = 'PENDING'
  handleQuery()
}

function handleDetail(row: TeamProfileChangeListItem) {
  getTeamProfileChange(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleApprove(row: TeamProfileChangeListItem) {
  proxy.$modal.confirm('确认通过该球队资料变更？通过后将覆盖正式球队资料。').then(() => {
    return approveTeamProfileChange(row.id, {})
  }).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
  }).catch(() => undefined)
}

function openReject(row: TeamProfileChangeListItem) {
  currentId.value = row.id
  rejectForm.value = { auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    rejectTeamProfileChange(currentId.value, rejectForm.value).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
    })
  })
}

getList()
</script>

<style scoped>
.sub-text {
  color: #909399;
  font-size: 12px;
}

.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.compare-col h4 {
  margin: 0 0 8px;
}

.audit-remark {
  margin-top: 16px;
  color: #606266;
}
</style>
