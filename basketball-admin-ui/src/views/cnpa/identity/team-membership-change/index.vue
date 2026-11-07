<!--
Menu SQL: docs/sql/cnpa-team-membership-change.sql
parent_id=2151 账号与认证
-->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="球队编号" prop="teamNo">
        <el-input v-model="queryParams.teamNo" placeholder="球队编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球队名称" prop="teamName">
        <el-input v-model="queryParams.teamName" placeholder="球队名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛事" prop="divisionName">
        <el-input v-model="queryParams.divisionName" placeholder="赛事名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员" prop="playerKeyword">
        <el-input v-model="queryParams.playerKeyword" placeholder="昵称或球员编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="操作类型" prop="changeType">
        <el-select v-model="queryParams.changeType" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="changeStatus">
        <el-select v-model="queryParams.changeStatus" placeholder="全部" clearable style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row class="mb8">
      <el-alert
        title="本页汇总各届赛事的成员操作记录。每条操作只影响所标注赛事的阵容，历史赛事记录单独保留。是否需要管理员审核，以该赛事自己的报名和阵容规则为准。绑错赛事的待审记录不能直接通过，需要在当前赛事重新邀请。"
        type="info"
        :closable="false"
        show-icon
      />
    </el-row>

    <el-table v-loading="loading" :data="dataList" row-key="changeNo">
      <el-table-column label="编号" prop="changeNo" min-width="150" show-overflow-tooltip />
      <el-table-column label="球队" min-width="160">
        <template #default="scope">
          <div>{{ scope.row.teamName || '-' }}</div>
          <div class="sub-text">{{ scope.row.teamNo || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="赛事" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <div>{{ scope.row.divisionName || '待核实' }}</div>
          <div v-if="eventMismatch(scope.row)" class="sub-text">球队当前赛事：{{ scope.row.teamDivisionName }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="changeTypeName" width="110" />
      <el-table-column label="对象" min-width="140">
        <template #default="scope">
          <div>{{ scope.row.subjectName || '-' }}</div>
          <div class="sub-text">{{ scope.row.subjectPlayerNo || '-' }}</div>
          <div class="sub-text">{{ scope.row.gameId || '未填写' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="发起" min-width="120">
        <template #default="scope">
          <div>{{ scope.row.initiatorRoleName || '-' }}</div>
          <div class="sub-text">{{ scope.row.initiatorName || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.changeStatus)" size="small">{{ scope.row.changeStatusName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="影响" prop="impactSummary" min-width="220" show-overflow-tooltip />
      <el-table-column label="时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-if="scope.row.id" v-hasPermi="['basket:teamMembershipChange:query']">详情</el-button>
          <el-button link type="success" icon="Check" @click="handleApprove(scope.row)" v-if="canApprove(scope.row)" v-hasPermi="['basket:teamMembershipChange:audit']">通过</el-button>
          <el-button link type="danger" icon="Close" @click="openReject(scope.row)" v-if="canReject(scope.row)" v-hasPermi="['basket:teamMembershipChange:audit']">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="成员变更详情" size="720px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="编号">{{ detail.changeNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.changeStatusName }}</el-descriptions-item>
          <el-descriptions-item label="球队">{{ detail.teamName }} / {{ detail.teamNo }}</el-descriptions-item>
          <el-descriptions-item label="赛事">{{ detail.divisionName || '待核实' }}</el-descriptions-item>
          <el-descriptions-item label="球队当前赛事">{{ detail.teamDivisionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报名">{{ detail.registrationId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="阶段">{{ detail.effectStageName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.changeTypeName }}</el-descriptions-item>
          <el-descriptions-item label="发起">{{ detail.initiatorRoleName }} {{ detail.initiatorName }}</el-descriptions-item>
          <el-descriptions-item label="球员">{{ detail.subjectName }} / {{ detail.subjectPlayerNo }}</el-descriptions-item>
          <el-descriptions-item label="认证 / Game ID">{{ detail.certificationStatusName || '-' }} / {{ detail.gameId || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="变更前">{{ detail.beforeSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="变更后">{{ detail.afterSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="影响" :span="2">{{ detail.impactSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="名单口径" :span="2">{{ detail.rosterGapSummary || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ detail.auditUserName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核说明">{{ detail.auditRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ parseTime(detail.effectiveTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectOpen" title="驳回成员变更" width="480px" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="TeamMembershipChange">
import { getCurrentInstance, reactive, ref, toRefs } from 'vue'
import {
  approveTeamMembershipChange,
  getTeamMembershipChange,
  listTeamMembershipChange,
  rejectTeamMembershipChange
} from '@/api/basket/teamMembershipChange'

const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const showSearch = ref(true)
const dataList = ref<any[]>([])
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<any>(null)
const rejectOpen = ref(false)
const rejectReason = ref('')
const rejectId = ref<number | string | null>(null)

const typeOptions = [
  { label: '邀请入队', value: 'INVITE_JOIN' },
  { label: '申请入队', value: 'APPLY_JOIN' },
  { label: '队长移除', value: 'CAPTAIN_REMOVE' },
  { label: '主动离队', value: 'VOLUNTARY_LEAVE' }
]
const statusOptions = [
  { label: '待用户确认', value: 'PENDING_USER' },
  { label: '待队长处理', value: 'PENDING_CAPTAIN' },
  { label: '待管理员审核', value: 'PENDING_ADMIN' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '已生效', value: 'EFFECTIVE' },
  { label: '历史上线前记录', value: 'HISTORICAL' }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    teamNo: undefined as string | undefined,
    teamName: undefined as string | undefined,
    divisionName: undefined as string | undefined,
    playerKeyword: undefined as string | undefined,
    changeType: undefined as string | undefined,
    changeStatus: undefined as string | undefined
  }
})
const { queryParams } = toRefs(data)

function statusTag(status: string) {
  if (status === 'PENDING_ADMIN' || status === 'PENDING_USER' || status === 'PENDING_CAPTAIN') return 'warning'
  if (status === 'APPROVED' || status === 'EFFECTIVE') return 'success'
  if (status === 'REJECTED') return 'danger'
  return 'info'
}

function eventMismatch(row: any) {
  if (!row?.divisionId || !row?.teamDivisionId) return false
  return String(row.divisionId) !== String(row.teamDivisionId)
}

function canReject(row: any) {
  return Boolean(row && row.id && row.changeStatus === 'PENDING_ADMIN' && row.legacyRecord !== 1)
}

function canApprove(row: any) {
  return canReject(row) && Boolean(row.divisionId) && !eventMismatch(row)
}

function getList() {
  loading.value = true
  listTeamMembershipChange(queryParams.value).then((res: any) => {
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
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleDetail(row: any) {
  getTeamMembershipChange(row.id).then((res: any) => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleApprove(row: any) {
  proxy.$modal.confirm('确认通过该成员变更？通过后才会写入永久球队和当前合法参赛名单。').then(() => {
    return approveTeamMembershipChange(row.id, {})
  }).then(() => {
    proxy.$modal.msgSuccess('已通过')
    getList()
  }).catch(() => {})
}

function openReject(row: any) {
  rejectId.value = row.id
  rejectReason.value = ''
  rejectOpen.value = true
}

function submitReject() {
  if (!rejectReason.value.trim()) {
    proxy.$modal.msgError('请填写驳回原因')
    return
  }
  rejectTeamMembershipChange(rejectId.value as number, { rejectReason: rejectReason.value.trim() }).then(() => {
    proxy.$modal.msgSuccess('已驳回')
    rejectOpen.value = false
    getList()
  })
}

getList()
</script>

<style scoped>
.sub-text { color: #909399; font-size: 12px; }
.mb8 { margin-bottom: 8px; }
</style>
