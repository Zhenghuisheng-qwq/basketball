<template>
  <div class="app-container">
    <el-form :model="queryParams" :inline="true" label-width="88px" class="mb8">
      <el-form-item label="所属赛事">
        <el-select v-model="selectedTournamentId" placeholder="请选择赛事" filterable style="width: 220px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季">
        <el-select v-model="queryParams.seasonId" placeholder="请选择赛季" filterable style="width: 200px" @change="handleSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段">
        <el-select v-model="queryParams.stageId" placeholder="全部/赛季汇总" clearable filterable style="width: 180px">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="球队名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery" :disabled="!queryParams.seasonId">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="danger" icon="Refresh" @click="handleRebuild" :disabled="!queryParams.seasonId" v-hasPermi="['basket:teamSeasonStat:rebuild']">重算赛季</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="球队" prop="teamName" min-width="140" :show-overflow-tooltip="true" fixed />
      <el-table-column label="阶段" prop="stageName" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="场次" prop="games" width="70" />
      <el-table-column label="胜" prop="wins" width="60" />
      <el-table-column label="负" prop="losses" width="60" />
      <el-table-column label="胜率" prop="winRate" width="80" />
      <el-table-column label="得分" prop="pointsFor" width="70" />
      <el-table-column label="失分" prop="pointsAgainst" width="70" />
      <el-table-column label="场均得分" prop="avgPoints" width="90" />
      <el-table-column label="场均失分" prop="avgPointsAgainst" width="90" />
      <el-table-column label="场均篮板" prop="avgRebounds" width="90" />
      <el-table-column label="场均助攻" prop="avgAssists" width="90" />
      <el-table-column label="场均抢断" prop="avgSteals" width="90" />
      <el-table-column label="场均盖帽" prop="avgBlocks" width="90" />
      <el-table-column label="投篮%" prop="fieldGoalPct" width="80" />
      <el-table-column label="三分%" prop="threePointPct" width="80" />
      <el-table-column label="罚球%" prop="freeThrowPct" width="80" />
      <el-table-column label="计算时间" prop="lastCalculatedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.lastCalculatedTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="BasketTeamSeasonStat">
import { listSeasonByTournament } from '@/api/basket/season'
import { listTeamSeasonStat, rebuildTeamSeasonStat } from '@/api/basket/teamSeasonStat'
import { listTournament } from '@/api/basket/tournament'
import { listStage } from '@/api/basket/tournamentStage'
import type {
  SeasonListItem,
  StageListItem,
  TeamSeasonStatListItem,
  TeamSeasonStatQuery,
  TournamentListItem
} from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const total = ref(0)
const dataList = ref<TeamSeasonStatListItem[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const stageOptions = ref<StageListItem[]>([])
const selectedTournamentId = ref<number>()

const queryParams = ref<TeamSeasonStatQuery>({
  pageNum: 1,
  pageSize: 20,
  seasonId: undefined,
  stageId: undefined,
  keyword: undefined
})

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows || []
    if (tournamentOptions.value.length > 0) {
      selectedTournamentId.value = tournamentOptions.value[0].id
      loadSeasons()
    }
  })
}

function loadSeasons() {
  seasonOptions.value = []
  stageOptions.value = []
  queryParams.value.seasonId = undefined
  queryParams.value.stageId = undefined
  dataList.value = []
  if (!selectedTournamentId.value) return
  listSeasonByTournament(selectedTournamentId.value).then(res => {
    seasonOptions.value = res.data || []
    queryParams.value.seasonId = seasonOptions.value[0]?.id
    if (queryParams.value.seasonId) {
      loadStages()
      getList()
    }
  })
}

function loadStages() {
  stageOptions.value = []
  if (!queryParams.value.seasonId) return
  listStage({ pageNum: 1, pageSize: 200, seasonId: queryParams.value.seasonId }).then(res => {
    stageOptions.value = res.rows || []
  })
}

function handleTournamentChange() {
  loadSeasons()
}

function handleSeasonChange() {
  queryParams.value.stageId = undefined
  loadStages()
  handleQuery()
}

function getList() {
  if (!queryParams.value.seasonId) return
  loading.value = true
  listTeamSeasonStat(queryParams.value).then(res => {
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
  queryParams.value.stageId = undefined
  queryParams.value.keyword = undefined
  handleQuery()
}

function handleRebuild() {
  if (!queryParams.value.seasonId) return
  proxy.$modal.confirm('重算将覆盖当前赛季的积分榜、赛季统计和排行榜结果。确认继续？').then(() =>
    rebuildTeamSeasonStat(queryParams.value.seasonId!)
  ).then(() => {
    proxy.$modal.msgSuccess('重算完成')
    getList()
  }).catch(() => {})
}

loadTournaments()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
