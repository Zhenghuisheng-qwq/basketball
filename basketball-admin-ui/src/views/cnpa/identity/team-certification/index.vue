<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="球队编号" prop="teamNo">
        <el-input v-model="queryParams.teamNo" placeholder="请输入球队编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球队名称" prop="teamName">
        <el-input v-model="queryParams.teamName" placeholder="请输入球队名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
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
        <el-button type="success" plain icon="Check" :disabled="batchMultiple" @click="handleBatchApprove" v-hasPermi="['basket:teamAudit:audit']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Close" :disabled="batchMultiple" @click="openBatchReject" v-hasPermi="['basket:teamAudit:audit']">批量驳回</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table ref="tableRef" v-loading="loading" :data="dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" :selectable="rowSelectable" />
      <el-table-column label="Logo" width="72" align="center">
        <template #default="scope">
          <el-image
            v-if="scope.row.logoUrl"
            :src="scope.row.logoUrl"
            fit="cover"
            class="team-logo-thumb team-logo-clickable"
            @click.stop="previewLogo(scope.row.logoUrl)"
          />
          <el-avatar v-else :size="40" shape="square">{{ (scope.row.teamName || '队').slice(0, 1) }}</el-avatar>
        </template>
      </el-table-column>
      <el-table-column label="球队编号" prop="teamNo" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="球队名称" prop="teamName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="球队简称" prop="shortName" min-width="100" :show-overflow-tooltip="true" />
      <el-table-column label="赛事系列" prop="seriesName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="赛季" prop="seasonName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="Division" prop="divisionName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="平台" prop="platformCode" width="80" />
      <el-table-column label="模式" prop="gameMode" width="80" />
      <el-table-column label="球队状态" prop="teamStatusName" width="110" :show-overflow-tooltip="true" />
      <el-table-column label="招募状态" prop="publicStatus" width="110">
        <template #default="scope">
          <dict-tag :options="TEAM_RECRUIT_STATUS_OPTIONS" :value="scope.row.publicStatus" />
        </template>
      </el-table-column>
      <el-table-column label="审核状态" prop="auditStatusName" width="100">
        <template #default="scope">
          <el-tag :type="teamAuditTagType(scope.row.auditStatus)" size="small">
            {{ scope.row.auditStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="成员数" prop="currentMemberCount" width="80" />
      <el-table-column label="创建时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:teamAudit:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="canTeamAudit(scope.row.auditStatus)">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-hasPermi="['basket:teamAudit:audit']" v-if="canTeamAudit(scope.row.auditStatus)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球队审核详情" size="860px" append-to-body>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-header">
            <el-image
              v-if="detail.logoUrl"
              :src="detail.logoUrl"
              fit="cover"
              class="team-logo-large team-logo-clickable"
              @click.stop="previewLogo(detail.logoUrl)"
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
            <el-descriptions-item label="赛事系列">{{ detail.seriesName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="赛季">{{ detail.seasonName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="Division">{{ detail.divisionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="平台 / 模式">{{ [detail.platformCode, detail.gameMode].filter(Boolean).join(' · ') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="球队状态">{{ detail.teamStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="招募状态">{{ detail.publicStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地区">{{ detail.regionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成员数">{{ detail.currentMemberCount ?? 0 }} 人</el-descriptions-item>
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
            <el-descriptions-item label="队长授权">
              {{ detail.captainQualificationValid ? '有效' : '无效或未确认' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">当前球队成员</el-divider>
          <el-table :data="detail.members || []" size="small" max-height="360" empty-text="暂无成员">
            <el-table-column label="昵称" prop="nickname" min-width="120" />
            <el-table-column label="球员编号" prop="playerNo" min-width="130" />
            <el-table-column label="Game ID" min-width="140" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.gameId || '未填写' }}</template>
            </el-table-column>
            <el-table-column label="球员 ID" prop="playerId" min-width="160" show-overflow-tooltip />
            <el-table-column label="角色" prop="roleTypeName" width="80" />
          </el-table>
          <p class="detail-hint">Game ID 是该赛区平台下已认证的游戏身份，不是球员 ID。这里展示的是当前球队成员，不改写历史认证记录。</p>

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
            <el-alert :title="validationFlags.captainQualificationValid ? '队长具备有效队长授权' : '队长缺少有效队长授权'" :type="validationFlags.captainQualificationValid ? 'success' : 'warning'" :closable="false" />
            <el-alert :title="validationFlags.hasMembers ? '已有球队成员' : '暂无球队成员'" :type="validationFlags.hasMembers ? 'success' : 'warning'" :closable="false" />
            <el-alert :title="validationFlags.teamActive ? '球队状态正常（当前赛事有效）' : (detail.teamStatusName || '球队不可用于当前赛事')" :type="validationFlags.teamActive ? 'success' : 'error'" :closable="false" />
            <el-alert :title="validationFlags.hasLogo ? '已上传球队 Logo' : '未上传球队 Logo'" :type="validationFlags.hasLogo ? 'success' : 'info'" :closable="false" />
            <el-alert title="审核通过时将校验：球队名称、简称、比赛终端、Logo 合规性，以及队长认证与队长授权" type="info" :closable="false" />
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

    <el-dialog title="批量驳回球队审核" v-model="batchRejectOpen" width="480px" append-to-body>
      <el-form ref="batchRejectRef" :model="batchRejectForm" :rules="rejectRules" label-width="88px">
        <el-form-item label="驳回原因" prop="rejectReason">
          <el-input v-model="batchRejectForm.rejectReason" type="textarea" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="batchRejectForm.auditRemark" type="textarea" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchRejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitBatchReject">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logoPreviewOpen" title="球队 Logo" width="520px" append-to-body destroy-on-close class="team-logo-preview-dialog">
      <div class="team-logo-preview-wrap">
        <img v-if="logoPreviewUrl" :src="logoPreviewUrl" alt="球队 Logo" class="team-logo-preview-img" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketTeamAudit">
import {
  approveTeamAudit,
  batchApproveTeamAudit,
  batchRejectTeamAudit,
  getTeamAudit,
  listTeamAudit,
  listTeamAuditRecords,
  rejectTeamAudit
} from '@/api/basket/teamAudit'
import { buildBatchAuditPayload, formatBatchAuditMessage, useBatchAuditSelection } from '@/utils/batchAudit'
import type {
  TeamAuditDetail,
  TeamAuditListItem,
  TeamAuditQuery,
  TeamAuditRecord,
  TeamAuditRejectReq
} from '@/types'
import { CNPA_AUDIT_STATUS_FILTER_OPTIONS, TEAM_RECRUIT_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()
const tableRef = ref()

const dataList = ref<TeamAuditListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<TeamAuditDetail>()
const auditRecords = ref<TeamAuditRecord[]>([])
const currentId = ref<number>()
const rejectOpen = ref(false)
const batchRejectOpen = ref(false)
const logoPreviewOpen = ref(false)
const logoPreviewUrl = ref('')
const rejectForm = ref<TeamAuditRejectReq>({ rejectReason: '' })
const batchRejectForm = ref<TeamAuditRejectReq>({ rejectReason: '' })
const rejectRules = {
  rejectReason: [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
}

const {
  multiple: batchMultiple,
  handleSelectionChange,
  rowSelectable,
  clearSelection,
  getAuditableIds
} = useBatchAuditSelection<TeamAuditListItem>({
  getRowId: row => row.id,
  canAudit: row => canTeamAudit(row.auditStatus)
})

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

function canTeamAudit(status?: number) {
  return status === 0 || status === 3
}

function teamAuditTagType(status?: number) {
  if (status === 1) return 'success'
  if (status === 0 || status === 3) return 'warning'
  if (status === 2) return 'danger'
  return 'info'
}

function mapAuditAction(action?: string) {
  if (action === 'START_REVIEW') return '开始审核'
  if (action === 'APPROVE') return '通过'
  if (action === 'REJECT') return '驳回'
  if (action === 'SUBMIT') return '提交认证'
  if (action === 'RESUBMIT') return '重新提交认证'
  return action || '-'
}

function previewLogo(url?: string) {
  if (!url) return
  logoPreviewUrl.value = url
  logoPreviewOpen.value = true
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

function handleApprove(row: TeamAuditListItem) {
  approveTeamAudit(row.id, {}).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
    if (detailOpen.value && detail.value?.id === row.id) {
      handleDetail(row)
    }
  })
}

function openReject(row: TeamAuditListItem) {
  currentId.value = row.id
  rejectForm.value = { rejectReason: '', auditRemark: '' }
  rejectOpen.value = true
}

function submitReject() {
  proxy.$refs.rejectRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    rejectTeamAudit(currentId.value!, rejectForm.value).then(() => {
      proxy.$modal.msgSuccess('已驳回')
      rejectOpen.value = false
      getList()
      if (detailOpen.value && detail.value?.id === currentId.value) {
        handleDetail({ id: currentId.value } as TeamAuditListItem)
      }
    })
  })
}

function handleBatchApprove() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  batchApproveTeamAudit(buildBatchAuditPayload(ids)).then(res => {
    proxy.$modal.msgSuccess(formatBatchAuditMessage(res.data))
    clearSelection()
    tableRef.value?.clearSelection()
    getList()
  })
}

function openBatchReject() {
  const ids = getAuditableIds()
  if (!ids.length) {
    proxy.$modal.msgWarning('请选择待审核记录')
    return
  }
  batchRejectForm.value = { rejectReason: '', auditRemark: '' }
  batchRejectOpen.value = true
}

function submitBatchReject() {
  proxy.$refs.batchRejectRef.validate((valid: boolean) => {
    if (!valid) return
    const ids = getAuditableIds()
    if (!ids.length) return
    const payload = buildBatchAuditPayload(ids, {
      rejectReason: batchRejectForm.value.rejectReason,
      auditRemark: batchRejectForm.value.auditRemark || undefined
    })
    batchRejectTeamAudit(payload).then(res => {
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

<style scoped lang="scss">
.team-logo-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  overflow: hidden;
  display: inline-flex;
  vertical-align: middle;

  :deep(.el-image__inner) {
    width: 100%;
    height: 100%;
  }
}

.team-logo-clickable {
  cursor: zoom-in;

  :deep(.el-image__inner) {
    cursor: zoom-in;
  }
}

.team-logo-large {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;

  :deep(.el-image__inner) {
    width: 100%;
    height: 100%;
  }
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

.detail-hint {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.team-logo-preview-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  max-height: 70vh;
  overflow: auto;
}

.team-logo-preview-img {
  display: block;
  max-width: 100%;
  max-height: 65vh;
  object-fit: contain;
}
</style>
