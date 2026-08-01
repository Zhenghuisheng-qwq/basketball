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

      <el-col :span="24">

        <el-alert

          type="info"

          :closable="false"

          show-icon

          title="阵容锁定说明"

          description="报名审核通过后会自动生成正式赛事球队（EventTeam）与正式赛事球员（EventPlayer）快照，并自动锁定阵容，无需管理员手动锁定。若历史数据显示“未锁定（需修复）”，可使用一次性修复按钮。"

        />

      </el-col>

      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

    </el-row>



    <el-table v-loading="loading" :data="dataList">

      <el-table-column label="赛事球队编号" prop="eventTeamNo" min-width="140" />

      <el-table-column label="球队名称快照" prop="teamNameSnapshot" min-width="140" :show-overflow-tooltip="true" />

      <el-table-column label="赛季/赛事" min-width="160">

        <template #default="scope">

          <div>{{ scope.row.seasonName || '-' }}</div>

          <div class="sub-text">{{ scope.row.tournamentName || '-' }}</div>

        </template>

      </el-table-column>

      <el-table-column label="游戏端" prop="gameTerminal" width="90" />

      <el-table-column label="参赛状态" prop="participationStatus" width="100">

        <template #default="scope">

          <dict-tag :options="PARTICIPATION_STATUS_OPTIONS" :value="scope.row.participationStatus" />

        </template>

      </el-table-column>

      <el-table-column label="阵容状态" prop="rosterLockStatus" width="120">

        <template #default="scope">

          <dict-tag :options="ROSTER_LOCK_STATUS_OPTIONS" :value="scope.row.rosterLockStatus" />

          <div v-if="scope.row.rosterLockNeedsRepair" class="sub-text warn-text">未锁定异常</div>

        </template>

      </el-table-column>

      <el-table-column label="队长" min-width="120">

        <template #default="scope">

          <div>{{ scope.row.captainNameSnapshot || '-' }}</div>

          <div class="sub-text">ID: {{ scope.row.captainPlayerId || '-' }}</div>

        </template>

      </el-table-column>

      <el-table-column label="审核通过时间" prop="approvedTime" width="160">

        <template #default="scope">{{ parseTime(scope.row.approvedTime) }}</template>

      </el-table-column>

      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">

        <template #default="scope">

          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:eventTeam:query']">详情</el-button>

          <el-button link type="primary" icon="User" @click="handleViewRoster(scope.row)" v-hasPermi="['basket:seasonRoster:query']">查看阵容</el-button>

          <el-button

            v-if="scope.row.rosterLockNeedsRepair"

            link

            type="warning"

            icon="Tools"

            @click="handleRepairRosterLock(scope.row)"

            v-hasPermi="['basket:eventTeam:edit']"

          >修复锁定</el-button>

          <el-button link type="primary" icon="Edit" @click="openParticipationStatus(scope.row)" v-hasPermi="['basket:eventTeam:edit']">参赛状态</el-button>

        </template>

      </el-table-column>

    </el-table>



    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />



    <el-drawer v-model="detailOpen" title="赛事球队详情" size="720px" append-to-body>

      <el-descriptions v-if="detail" :column="2" border>

        <el-descriptions-item label="赛事球队编号">{{ detail.eventTeamNo }}</el-descriptions-item>

        <el-descriptions-item label="球队编号">{{ detail.teamNo || '-' }}</el-descriptions-item>

        <el-descriptions-item label="球队名称快照">{{ detail.teamNameSnapshot || '-' }}</el-descriptions-item>

        <el-descriptions-item label="简称快照">{{ detail.shortNameSnapshot || '-' }}</el-descriptions-item>

        <el-descriptions-item label="地区快照">{{ detail.teamRegionSnapshot || '-' }}</el-descriptions-item>

        <el-descriptions-item label="赛季">{{ detail.seasonName || detail.seasonId }}</el-descriptions-item>

        <el-descriptions-item label="赛事">{{ detail.tournamentName || '-' }}</el-descriptions-item>

        <el-descriptions-item label="游戏端快照">{{ detail.gameTerminal || '-' }}</el-descriptions-item>

        <el-descriptions-item label="参赛状态">{{ detail.participationStatusName }}</el-descriptions-item>

        <el-descriptions-item label="阵容锁定">{{ detail.rosterLockStatusName }}</el-descriptions-item>

        <el-descriptions-item label="队长快照">{{ detail.captainNameSnapshot || '-' }}</el-descriptions-item>

        <el-descriptions-item label="队长球员ID">{{ detail.captainPlayerId || '-' }}</el-descriptions-item>

        <el-descriptions-item label="报名来源">{{ detail.registrationSource || '-' }}</el-descriptions-item>

        <el-descriptions-item label="锁定时间">{{ parseTime(detail.rosterLockedTime) }}</el-descriptions-item>

        <el-descriptions-item label="审核通过时间">{{ parseTime(detail.approvedTime) }}</el-descriptions-item>

        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>

        <el-descriptions-item label="简介快照" :span="2">{{ detail.descriptionSnapshot || '-' }}</el-descriptions-item>

      </el-descriptions>

    </el-drawer>



    <el-drawer v-model="rosterOpen" :title="`赛事阵容 - ${currentTeamName}`" size="860px" append-to-body>

      <el-table v-loading="rosterLoading" :data="rosterList" size="small">

        <el-table-column label="球员快照" min-width="140">

          <template #default="scope">

            <div>{{ scope.row.playerNicknameSnapshot || '-' }}</div>

            <div class="sub-text">ID: {{ scope.row.playerId }}</div>

          </template>

        </el-table-column>

        <el-table-column label="平台/游戏ID" min-width="140">

          <template #default="scope">

            <div>{{ scope.row.platformSnapshot || '-' }}</div>

            <div class="sub-text">{{ scope.row.gameIdDisplaySnapshot || '-' }}</div>

          </template>

        </el-table-column>

        <el-table-column label="角色" prop="rosterRole" width="80" />

        <el-table-column label="队长" width="70">

          <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>

        </el-table-column>

        <el-table-column label="锁定" width="80">

          <template #default="scope">{{ scope.row.lockStatus === 1 ? '已锁定' : '未锁定' }}</template>

        </el-table-column>

      </el-table>

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

  repairEventTeamRosterLock,

  updateEventTeamParticipationStatus

} from '@/api/basket/eventTeam'

import { listSeasonRoster } from '@/api/basket/seasonRoster'

import type {

  EventTeamDetail,

  EventTeamListItem,

  EventTeamParticipationStatusReq,

  EventTeamQuery,

  SeasonRosterListItem

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

const rosterOpen = ref(false)

const rosterLoading = ref(false)

const rosterList = ref<SeasonRosterListItem[]>([])

const currentTeamName = ref('')

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



function handleViewRoster(row: EventTeamListItem) {

  currentTeamName.value = row.teamNameSnapshot || row.eventTeamNo || String(row.id)

  rosterOpen.value = true

  rosterLoading.value = true

  rosterList.value = []

  listSeasonRoster({ pageNum: 1, pageSize: 100, eventTeamId: row.id }).then(res => {

    rosterList.value = res.rows || []

  }).finally(() => {

    rosterLoading.value = false

  })

}



function handleRepairRosterLock(row: EventTeamListItem) {

  proxy.$modal.confirm(`确认修复球队「${row.teamNameSnapshot}」的阵容锁定状态？`).then(() => repairEventTeamRosterLock(row.id)).then(() => {

    proxy.$modal.msgSuccess('阵容锁定已修复')

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

.warn-text {

  color: var(--el-color-warning);

}

</style>


