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
        <el-select v-model="queryParams.stageId" placeholder="全部阶段" clearable filterable style="width: 180px" @change="handleStageChange">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="分组">
        <el-select v-model="queryParams.groupId" placeholder="全部分组" clearable filterable style="width: 160px">
          <el-option v-for="item in groupOptions" :key="item.id" :label="item.groupName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队名称">
        <el-input v-model="queryParams.teamName" placeholder="球队名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery" :disabled="!queryParams.seasonId">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="danger" icon="Refresh" @click="handleRebuild" :disabled="!queryParams.seasonId" v-hasPermi="['basket:standing:rebuild']">重算赛季</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="排名" prop="rankNo" width="70" />
      <el-table-column label="球队" prop="teamName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="阶段" prop="stageName" min-width="110" :show-overflow-tooltip="true" />
      <el-table-column label="分组" prop="groupName" min-width="100" :show-overflow-tooltip="true" />
      <el-table-column label="场次" prop="matchesPlayed" width="70" />
      <el-table-column label="胜" prop="wins" width="60" />
      <el-table-column label="负" prop="losses" width="60" />
      <el-table-column label="积分" prop="standingPoints" width="80" />
      <el-table-column label="胜率" prop="winRate" width="80" />
      <el-table-column label="得分" prop="pointsFor" width="70" />
      <el-table-column label="失分" prop="pointsAgainst" width="70" />
      <el-table-column label="净胜" prop="pointDiff" width="70" />
      <el-table-column label="连胜/负" width="90">
        <template #default="scope">{{ scope.row.streakType || '-' }}{{ scope.row.streakCount ?? '' }}</template>
      </el-table-column>
      <el-table-column label="出线" prop="qualifiedFlag" width="70">
        <template #default="scope">{{ scope.row.qualifiedFlag === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="快照时间" prop="snapshotTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.snapshotTime) }}</template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="BasketStanding">
import { listSeasonByTournament } from '@/api/basket/season'
import { listStanding, rebuildStandingSeason } from '@/api/basket/standing'
import { listTournament } from '@/api/basket/tournament'
import { listGroupByStage } from '@/api/basket/tournamentGroup'
import { listStage } from '@/api/basket/tournamentStage'
import type {
  GroupListItem,
  SeasonListItem,
  StageListItem,
  StandingListItem,
  StandingQuery,
  TournamentListItem
} from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const total = ref(0)
const dataList = ref<StandingListItem[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const stageOptions = ref<StageListItem[]>([])
const groupOptions = ref<GroupListItem[]>([])
const selectedTournamentId = ref<number>()

const queryParams = ref<StandingQuery>({
  pageNum: 1,
  pageSize: 20,
  seasonId: undefined,
  stageId: undefined,
  groupId: undefined,
  teamName: undefined
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
  groupOptions.value = []
  queryParams.value.seasonId = undefined
  queryParams.value.stageId = undefined
  queryParams.value.groupId = undefined
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

function loadGroups() {
  groupOptions.value = []
  queryParams.value.groupId = undefined
  if (!queryParams.value.stageId) return
  listGroupByStage(queryParams.value.stageId).then(res => {
    groupOptions.value = res.data || []
  })
}

function handleTournamentChange() {
  loadSeasons()
}

function handleSeasonChange() {
  queryParams.value.stageId = undefined
  queryParams.value.groupId = undefined
  groupOptions.value = []
  loadStages()
  handleQuery()
}

function handleStageChange() {
  loadGroups()
}

function getList() {
  if (!queryParams.value.seasonId) return
  loading.value = true
  listStanding(queryParams.value).then(res => {
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
  queryParams.value.groupId = undefined
  queryParams.value.teamName = undefined
  groupOptions.value = []
  handleQuery()
}

function handleRebuild() {
  if (!queryParams.value.seasonId) return
  proxy.$modal.confirm('重算将覆盖当前赛季的积分榜、赛季统计和排行榜结果。确认继续？').then(() =>
    rebuildStandingSeason(queryParams.value.seasonId!)
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
