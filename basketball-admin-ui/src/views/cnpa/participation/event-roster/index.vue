<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="赛季ID" prop="seasonId">
        <el-input v-model="queryParams.seasonId" placeholder="请输入赛季ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赛事球队ID" prop="eventTeamId">
        <el-input v-model="queryParams.eventTeamId" placeholder="请输入赛事球队ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员ID" prop="playerId">
        <el-input v-model="queryParams.playerId" placeholder="请输入球员ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="阵容状态" prop="rosterStatus">
        <el-select v-model="queryParams.rosterStatus" placeholder="阵容状态" clearable style="width: 200px">
          <el-option v-for="item in SEASON_ROSTER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
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
      <el-table-column label="阵容编号" prop="rosterNo" min-width="140" />
      <el-table-column label="赛事球队" min-width="160">
        <template #default="scope">
          <div>{{ scope.row.eventTeamName || '-' }}</div>
          <div class="sub-text">{{ scope.row.eventTeamNo }}</div>
        </template>
      </el-table-column>
      <el-table-column label="球员" min-width="160">
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
      <el-table-column label="队长" prop="isCaptain" width="70">
        <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="来源" prop="sourceTypeName" width="110" />
      <el-table-column label="状态" prop="rosterStatus" width="100">
        <template #default="scope">
          <dict-tag :options="SEASON_ROSTER_STATUS_OPTIONS" :value="scope.row.rosterStatus" />
        </template>
      </el-table-column>
      <el-table-column label="生效时间" prop="effectiveTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.effectiveTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Clock" @click="handleChangeHistory(scope.row)" v-hasPermi="['basket:seasonRoster:query']">变更历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="historyOpen" title="阵容变更历史" size="760px" append-to-body>
      <el-table :data="changeHistory" size="small" v-loading="historyLoading">
        <el-table-column label="变更类型" prop="changeTypeName" width="120" />
        <el-table-column label="源球队ID" prop="sourceEventTeamId" width="100" />
        <el-table-column label="目标球队ID" prop="targetEventTeamId" width="110" />
        <el-table-column label="变更前" prop="beforeSnapshot" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="变更后" prop="afterSnapshot" min-width="140" :show-overflow-tooltip="true" />
        <el-table-column label="操作人" prop="operatorName" width="100" />
        <el-table-column label="原因" prop="operateReason" min-width="120" :show-overflow-tooltip="true" />
        <el-table-column label="生效时间" prop="effectiveTime" width="160">
          <template #default="scope">{{ parseTime(scope.row.effectiveTime) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="BasketSeasonRosterAdmin">
import { listSeasonRoster, listSeasonRosterChangeHistory } from '@/api/basket/seasonRoster'
import type { SeasonRosterChangeRecord, SeasonRosterListItem, SeasonRosterQuery } from '@/types'
import { SEASON_ROSTER_STATUS_OPTIONS } from '@/types'

const dataList = ref<SeasonRosterListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const historyOpen = ref(false)
const historyLoading = ref(false)
const changeHistory = ref<SeasonRosterChangeRecord[]>([])

const queryParams = ref<SeasonRosterQuery>({
  pageNum: 1,
  pageSize: 10
})

const { proxy } = getCurrentInstance()

function getList() {
  loading.value = true
  listSeasonRoster(queryParams.value).then(response => {
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

function handleChangeHistory(row: SeasonRosterListItem) {
  if (!row.playerId || !row.seasonId) {
    proxy.$modal.msgWarning('缺少球员或赛季信息')
    return
  }
  historyOpen.value = true
  historyLoading.value = true
  changeHistory.value = []
  listSeasonRosterChangeHistory(row.playerId, row.seasonId).then(res => {
    changeHistory.value = res.data || []
  }).finally(() => {
    historyLoading.value = false
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
