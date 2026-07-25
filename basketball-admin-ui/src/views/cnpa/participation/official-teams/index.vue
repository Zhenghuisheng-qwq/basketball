<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="赛季ID" prop="seasonId">
        <el-input v-model="queryParams.seasonId" placeholder="请输入赛季ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛事球队编号" prop="eventTeamNo">
        <el-input v-model="queryParams.eventTeamNo" placeholder="请输入赛事球队编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球队名称" prop="teamName">
        <el-input v-model="queryParams.teamName" placeholder="请输入球队名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="参赛状态" prop="participationStatus">
        <el-select v-model="queryParams.participationStatus" placeholder="参赛状态" clearable style="width: 200px">
          <el-option v-for="item in PARTICIPATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="阵容锁定" prop="rosterLockStatus">
        <el-select v-model="queryParams.rosterLockStatus" placeholder="阵容锁定状态" clearable style="width: 200px">
          <el-option v-for="item in ROSTER_LOCK_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="赛事球队编号" prop="eventTeamNo" min-width="140" />
      <el-table-column label="球队名称" prop="teamNameSnapshot" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="赛季" min-width="140">
        <template #default="scope">
          <div>{{ scope.row.seasonName || '-' }}</div>
          <div class="sub-text">ID: {{ scope.row.seasonId }}</div>
        </template>
      </el-table-column>
      <el-table-column label="参赛状态" prop="participationStatus" width="100">
        <template #default="scope">
          <dict-tag :options="PARTICIPATION_STATUS_OPTIONS" :value="scope.row.participationStatus" />
        </template>
      </el-table-column>
      <el-table-column label="阵容锁定" prop="rosterLockStatus" width="100">
        <template #default="scope">
          <dict-tag :options="ROSTER_LOCK_STATUS_OPTIONS" :value="scope.row.rosterLockStatus" />
        </template>
      </el-table-column>
      <el-table-column label="队长球员ID" prop="captainPlayerId" width="110" />
      <el-table-column label="批准时间" prop="approvedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.approvedTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:eventTeam:query']">详情</el-button>
          <el-button link type="warning" icon="Lock" @click="handleLockRoster(scope.row)" v-hasPermi="['basket:eventTeam:edit']" v-if="scope.row.rosterLockStatus === 0">锁定阵容</el-button>
          <el-button link type="success" icon="Unlock" @click="handleUnlockRoster(scope.row)" v-hasPermi="['basket:eventTeam:edit']" v-if="scope.row.rosterLockStatus === 1">解锁阵容</el-button>
          <el-button link type="primary" icon="Edit" @click="openParticipationStatus(scope.row)" v-hasPermi="['basket:eventTeam:edit']">参赛状态</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赛事球队详情" size="720px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="赛事球队编号">{{ detail.eventTeamNo }}</el-descriptions-item>
        <el-descriptions-item label="球队编号">{{ detail.teamNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球队名称">{{ detail.teamNameSnapshot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="简称">{{ detail.shortNameSnapshot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName || detail.seasonId }}</el-descriptions-item>
        <el-descriptions-item label="游戏平台">{{ detail.gameTerminal || '-' }}</el-descriptions-item>
        <el-descriptions-item label="参赛状态">{{ detail.participationStatusName }}</el-descriptions-item>
        <el-descriptions-item label="阵容锁定">{{ detail.rosterLockStatusName }}</el-descriptions-item>
        <el-descriptions-item label="队长球员ID">{{ detail.captainPlayerId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报名来源">{{ detail.registrationSource || '-' }}</el-descriptions-item>
        <el-descriptions-item label="锁定时间">{{ parseTime(detail.rosterLockedTime) }}</el-descriptions-item>
        <el-descriptions-item label="批准时间">{{ parseTime(detail.approvedTime) }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ detail.descriptionSnapshot || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog title="更新参赛状态" v-model="statusOpen" width="480px" append-to-body>
      <el-form ref="statusRef" :model="statusForm" :rules="statusRules" label-width="88px">
        <el-form-item label="参赛状态" prop="participationStatus">
          <el-select v-model="statusForm.participationStatus" placeholder="请选择参赛状态" style="width: 100%">
            <el-option v-for="item in PARTICIPATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因说明">
          <el-input v-model="statusForm.reason" type="textarea" placeholder="请输入原因说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusOpen = false">取消</el-button>
        <el-button type="primary" @click="submitParticipationStatus">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketEventTeamAdmin">
import {
  getEventTeam,
  listEventTeam,
  lockEventTeamRoster,
  unlockEventTeamRoster,
  updateEventTeamParticipationStatus
} from '@/api/basket/eventTeam'
import type {
  EventTeamDetail,
  EventTeamListItem,
  EventTeamParticipationStatusReq,
  EventTeamQuery
} from '@/types'
import { PARTICIPATION_STATUS_OPTIONS, ROSTER_LOCK_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<EventTeamListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<EventTeamDetail>()
const currentId = ref<number>()
const statusOpen = ref(false)
const statusForm = ref<EventTeamParticipationStatusReq>({ participationStatus: 1 })
const statusRules = {
  participationStatus: [{ required: true, message: '请选择参赛状态', trigger: 'change' }]
}

const queryParams = ref<EventTeamQuery>({
  pageNum: 1,
  pageSize: 10
})

function getList() {
  loading.value = true
  listEventTeam(queryParams.value).then(response => {
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

function handleDetail(row: EventTeamListItem) {
  getEventTeam(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleLockRoster(row: EventTeamListItem) {
  proxy.$modal.confirm(`确认锁定球队「${row.teamNameSnapshot}」的阵容？`).then(() => lockEventTeamRoster(row.id)).then(() => {
    proxy.$modal.msgSuccess('阵容已锁定')
    getList()
  }).catch(() => {})
}

function handleUnlockRoster(row: EventTeamListItem) {
  proxy.$modal.confirm(`确认解锁球队「${row.teamNameSnapshot}」的阵容？`).then(() => unlockEventTeamRoster(row.id)).then(() => {
    proxy.$modal.msgSuccess('阵容已解锁')
    getList()
  }).catch(() => {})
}

function openParticipationStatus(row: EventTeamListItem) {
  currentId.value = row.id
  statusForm.value = { participationStatus: row.participationStatus || 1, reason: '' }
  statusOpen.value = true
}

function submitParticipationStatus() {
  proxy.$refs.statusRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    updateEventTeamParticipationStatus(currentId.value!, statusForm.value).then(() => {
      proxy.$modal.msgSuccess('参赛状态已更新')
      statusOpen.value = false
      getList()
    })
  })
}

getList()
</script>

<style scoped>
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
