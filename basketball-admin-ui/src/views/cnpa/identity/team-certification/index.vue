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
      <el-table-column label="Logo" width="72" align="center">
        <template #default="scope">
          <el-image
            v-if="scope.row.logoUrl"
            :src="scope.row.logoUrl"
            fit="cover"
            class="team-logo-thumb"
            :preview-src-list="[scope.row.logoUrl]"
          />
          <el-avatar v-else :size="40" shape="square">{{ (scope.row.teamName || '队').slice(0, 1) }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="球队编号" prop="teamNo" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="球队名称" prop="teamName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="球队简称" prop="shortName" min-width="100" :show-overflow-tooltip="true" />
      <el-table-column label="比赛终端" prop="gameTerminal" width="100" />
      <el-table-column label="招募状态" prop="publicStatus" width="110">
        <template #default="scope">
          <dict-tag :options="TEAM_RECRUIT_STATUS_OPTIONS" :value="scope.row.publicStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审核状态" prop="auditStatus" width="100">
        <template #default="scope">
          <dict-tag :options="TEAM_AUDIT_STATUS_OPTIONS" :value="scope.row.auditStatus" />
        </template>
      </el-table-column>
      <el-table-column label="成员数" prop="currentMemberCount" width="80" />
      <el-table-column label="创建时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:teamAudit:query']">详情</el-button>
          <el-button link type="warning" icon="EditPen" @click="handleStartReview(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="scope.row.auditStatus === 0">开始审核</el-button>
          <el-button link type="success" icon="Check" @click="openApprove(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="[0, 3].includes(scope.row.auditStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="[0, 3].includes(scope.row.auditStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球队审核详情" size="720px" append-to-body>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-header">
            <el-image
              v-if="detail.logoUrl"
              :src="detail.logoUrl"
              fit="cover"
              class="team-logo-large"
              :preview-src-list="[detail.logoUrl]"
            />
            <el-avatar v-else :size="72" shape="square">{{ (detail.teamName || '队').slice(0, 1) }}</el-avatar>
            <div>
              <div class="detail-name">{{ detail.teamName || '-' }}</div>
              <div class="sub-text">{{ detail.teamNo || '-' }} · {{ detail.shortName || '无简称' }}</div>
            </div>
          </div>

          <el-divider content-position="left">基础信息</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="球队编号">{{ detail.teamNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="审核状态">{{ detail.auditStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="球队名称">{{ detail.teamName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="球队简称">{{ detail.shortName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="比赛终端">{{ detail.gameTerminal || '-' }}</el-descriptions-item>
            <el-descriptions-item label="招募状态">{{ detail.publicStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地区">{{ detail.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成员数">{{ detail.currentMemberCount ?? 0 }}/{{ detail.maxMemberCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ parseTime(detail.updateTime) }}</el-descriptions-item>
            <el-descriptions-item label="球队介绍" :span="2">{{ detail.description || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">队长信息</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="队长用户 ID">{{ detail.captainMemberId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="队长昵称">{{ detail.captainNickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="队长邮箱">{{ detail.captainEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item label="队长球员编号">{{ detail.captainPlayerNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="队长认证状态">{{ detail.captainCertStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="队长资格">
              {{ detail.captainQualificationValid ? '有效' : '无效或未确认' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">成员信息</el-divider>
          <el-table :data="detail.members || []" size="small" empty-text="暂无成员">
            <el-table-column label="昵称" prop="nickname" min-width="120" />
            <el-table-column label="球员编号" prop="playerNo" min-width="120" />
            <el-table-column label="角色" prop="roleTypeName" width="80" />
          </el-table>

          <el-divider content-position="left">审核信息</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="当前审核状态">{{ detail.auditStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime || detail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="审核时间">{{ parseTime(detail.auditTime) }}</el-descriptions-item>
            <el-descriptions-item label="审核人">{{ detail.auditUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="detail.auditStatus === 2" label="驳回原因" :span="2">
              {{ detail.rejectReason || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail.auditRemark" label="审核备注" :span="2">
              {{ detail.auditRemark }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">校验项</el-divider>
          <el-space direction="vertical" fill style="width: 100%">
            <el-alert :title="validationFlags.captainAssigned ? '已指定队长' : '未指定队长'" :type="validationFlags.captainAssigned ? 'success' : 'error'" :closable="false" />
            <el-alert :title="validationFlags.captainCertified ? '队长已完成球员认证' : '队长未完成球员认证'" :type="validationFlags.captainCertified ? 'success' : 'error'" :closable="false" />
            <el-alert :title="validationFlags.captainQualificationValid ? '队长具备有效队长资格' : '队长缺少有效队长资格'" :type="validationFlags.captainQualificationValid ? 'success' : 'warning'" :closable="false" />
            <el-alert :title="validationFlags.hasMembers ? '已有球队成员' : '暂无球队成员'" :type="validationFlags.hasMembers ? 'success' : 'warning'" :closable="false" />
            <el-alert :title="validationFlags.teamActive ? '球队状态正常' : '球队已解散或不可用'" :type="validationFlags.teamActive ? 'success' : 'error'" :closable="false" />
            <el-alert :title="validationFlags.hasLogo ? '已上传球队 Logo' : '未上传球队 Logo'" :type="validationFlags.hasLogo ? 'success' : 'info'" :closable="false" />
            <el-alert title="审核通过时将校验：球队名称、简称、比赛终端、Logo 合规性，以及队长认证与队长资格" type="info" :closable="false" />
          </el-space>

          <el-divider content-position="left">审核记录</el-divider>
          <el-table :data="auditRecords" size="small">
            <el-table-column label="操作" prop="auditAction" width="120">
              <template #default="scope">{{ mapAuditAction(scope.row.auditAction) }}</template>
            </el-table-column>
            <el-table-column label="审核人" prop="auditorName" width="100" />
            <el-table-column label="备注" prop="auditRemark" min-width="160" />
            <el-table-column label="时间" prop="auditTime" width="160">
              <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
            </el-table-column>
          </el-table>
        </template>
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
import { TEAM_AUDIT_STATUS_OPTIONS, TEAM_RECRUIT_STATUS_OPTIONS } from '@/types'

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
  captainAssigned: !!detail.value?.captainAssigned,
  captainCertified: !!detail.value?.captainCertified,
  captainQualificationValid: !!detail.value?.captainQualificationValid,
  hasMembers: (detail.value?.currentMemberCount || 0) > 0,
  teamActive: detail.value?.teamActive !== false,
  hasLogo: !!detail.value?.logoUrl
}))

function mapAuditAction(action?: string) {
  if (action === 'START_REVIEW') return '开始审核'
  if (action === 'APPROVE') return '通过'
  if (action === 'REJECT') return '驳回'
  return action || '-'
}

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
    if (detailOpen.value && detail.value?.id === currentId.value) {
      handleDetail({ id: currentId.value } as TeamAuditListItem)
    }
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
      if (detailOpen.value && detail.value?.id === currentId.value) {
        handleDetail({ id: currentId.value } as TeamAuditListItem)
      }
    }).catch(() => {})
  })
}

getList()
</script>

<style scoped lang="scss">
.team-logo-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
}

.team-logo-large {
  width: 72px;
  height: 72px;
  border-radius: 8px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.detail-name {
  font-size: 18px;
  font-weight: 600;
}

.sub-text {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.detail-desc {
  margin-bottom: 8px;
}
</style>
