<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="变更编号" prop="changeNo">
        <el-input v-model="queryParams.changeNo" placeholder="变更编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="原/新昵称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="用户" prop="submitUser">
        <el-input v-model="queryParams.submitUser" placeholder="昵称/邮箱/球员编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStage">
        <el-select v-model="queryParams.auditStage" placeholder="审核状态" clearable style="width: 140px">
          <el-option v-for="item in CNPA_AUDIT_STATUS_FILTER_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="-"
          start-placeholder="开始"
          end-placeholder="结束"
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

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="用户" min-width="160">
        <template #default="scope">
          <div>{{ scope.row.submitUserName || '-' }}</div>
          <div class="sub-text">{{ scope.row.playerNo || scope.row.accountNo || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="当前昵称" prop="oldNickname" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="申请昵称" prop="newNickname" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="头像变更" width="140" align="center">
        <template #default="scope">
          <span v-if="!avatarChanged(scope.row)" class="sub-text">未变更</span>
          <div v-else class="avatar-pair">
            <el-image
              v-if="scope.row.oldAvatarUrl"
              :src="scope.row.oldAvatarUrl"
              fit="cover"
              class="avatar-thumb"
              :preview-src-list="previewList(scope.row)"
              :initial-index="0"
            />
            <span v-else class="sub-text">-</span>
            <span>→</span>
            <el-image
              v-if="scope.row.newAvatarUrl"
              :src="scope.row.newAvatarUrl"
              fit="cover"
              class="avatar-thumb"
              :preview-src-list="previewList(scope.row)"
              :initial-index="scope.row.oldAvatarUrl ? 1 : 0"
            />
            <span v-else class="sub-text">-</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="变更内容" prop="changeSummary" width="100" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="scope">
          <el-tag :type="auditTagType(scope.row.auditStatus)" size="small">{{ scope.row.auditStatusName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:memberProfileChange:query']">查看</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:memberProfileChange:audit']" v-if="isPending(scope.row.auditStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:memberProfileChange:audit']" v-if="isPending(scope.row.auditStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="用户资料变更审核" size="640px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="变更编号">{{ detail.changeNo }}</el-descriptions-item>
          <el-descriptions-item label="球员编号">{{ detail.playerNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户">{{ detail.submitUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.submitUserEmail || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核状态">{{ detail.auditStatusName }}</el-descriptions-item>
        </el-descriptions>

        <div class="compare-grid">
          <div class="compare-col">
            <h4>原资料</h4>
            <p>昵称：{{ detail.oldNickname || '-' }}</p>
            <p>头像：</p>
            <el-image
              v-if="detail.oldAvatarUrl"
              :src="detail.oldAvatarUrl"
              fit="contain"
              style="width: 120px; height: 120px"
              :preview-src-list="[detail.oldAvatarUrl]"
            />
            <span v-else class="sub-text">未设置</span>
          </div>
          <div class="compare-col">
            <h4>申请资料</h4>
            <p>昵称：{{ nicknameChanged(detail) ? (detail.newNickname || '-') : '未变更' }}</p>
            <p>头像：</p>
            <el-image
              v-if="avatarChanged(detail) && detail.newAvatarUrl"
              :src="detail.newAvatarUrl"
              fit="contain"
              style="width: 120px; height: 120px"
              :preview-src-list="[detail.newAvatarUrl]"
            />
            <span v-else class="sub-text">{{ avatarChanged(detail) ? '未设置' : '未变更' }}</span>
          </div>
        </div>
        <p v-if="detail.auditRemark" class="audit-remark">审核意见：{{ detail.auditRemark }}</p>
        <div v-if="isPending(detail.auditStatus)" class="drawer-actions">
          <el-button type="danger" @click="openReject(detail)">驳回</el-button>
          <el-button type="primary" @click="handleApprove(detail)">通过</el-button>
        </div>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectOpen" title="驳回资料变更" width="480px" append-to-body>
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectRef" label-width="88px">
        <el-form-item label="驳回原因" prop="auditRemark">
          <el-input v-model="rejectForm.auditRemark" type="textarea" :rows="4" placeholder="例如：昵称包含不合适内容 / 头像不符合社区规范" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketMemberProfileChange">
import { getCurrentInstance, ref } from 'vue'
import {
  approveMemberProfileChange,
  getMemberProfileChange,
  listMemberProfileChange,
  rejectMemberProfileChange
} from '@/api/basket/memberProfileChange'
import type {
  MemberProfileChangeDetail,
  MemberProfileChangeListItem,
  MemberProfileChangeQuery,
  MemberProfileChangeRejectReq
} from '@/types'
import { CNPA_AUDIT_STATUS_FILTER_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<MemberProfileChangeListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<MemberProfileChangeDetail>()
const rejectOpen = ref(false)
const rejectTargetId = ref<number>()
const dateRange = ref<string[]>([])

const queryParams = ref<MemberProfileChangeQuery>({
  pageNum: 1,
  pageSize: 10,
  auditStage: 'PENDING'
})

const rejectForm = ref<MemberProfileChangeRejectReq>({
  auditRemark: ''
})

const rejectRules = {
  auditRemark: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
}

function isPending(status?: number) {
  return status === 1
}

function auditTagType(status?: number) {
  if (status === 1) return 'warning'
  if (status === 3) return 'success'
  if (status === 4) return 'danger'
  return 'info'
}

function previewList(row: MemberProfileChangeListItem) {
  return [row.oldAvatarUrl, row.newAvatarUrl].filter(Boolean) as string[]
}

function avatarChanged(row: { changeSummary?: string; oldAvatarUrl?: string; newAvatarUrl?: string }) {
  if (row.changeSummary?.includes('头像')) return true
  if (!row.changeSummary && row.oldAvatarUrl && row.newAvatarUrl && row.oldAvatarUrl !== row.newAvatarUrl) return true
  return false
}

function nicknameChanged(row: { changeSummary?: string; oldNickname?: string; newNickname?: string }) {
  if (row.changeSummary?.includes('昵称')) return true
  if (!row.changeSummary && (row.oldNickname || '') !== (row.newNickname || '')) return true
  return false
}

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
  params.beginTime = dateRange.value?.[0]
  params.endTime = dateRange.value?.[1]
  listMemberProfileChange(params).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
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

function handleDetail(row: MemberProfileChangeListItem) {
  getMemberProfileChange(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleApprove(row: MemberProfileChangeListItem | MemberProfileChangeDetail) {
  proxy.$modal.confirm(`确认通过「${row.newNickname || row.oldNickname}」的资料变更？通过后将正式生效。`).then(() =>
    approveMemberProfileChange(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('已通过')
    detailOpen.value = false
    getList()
  }).catch(() => {})
}

function openReject(row: MemberProfileChangeListItem | MemberProfileChangeDetail) {
  rejectTargetId.value = row.id
  rejectForm.value.auditRemark = ''
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !rejectTargetId.value) return
    rejectMemberProfileChange(rejectTargetId.value, rejectForm.value).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      detailOpen.value = false
      getList()
    })
  })
}

getList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.sub-text { color: #909399; font-size: 12px; }
.avatar-pair {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.avatar-thumb {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 1px solid #ebeef5;
}
.compare-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 20px;
}
.compare-col {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}
.audit-remark {
  margin-top: 16px;
  color: #c45656;
}
.drawer-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
