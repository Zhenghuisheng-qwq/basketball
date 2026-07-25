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
      <el-form-item label="排行维度">
        <el-select v-model="queryParams.rankType" placeholder="排行维度" style="width: 180px" @change="handleQuery">
          <el-option v-for="item in rankTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryParams.keyword" placeholder="球队/球员" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery" :disabled="!queryParams.seasonId">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="danger" icon="Refresh" @click="handleRebuild" :disabled="!queryParams.seasonId" v-hasPermi="['basket:ranking:rebuild']">重算赛季</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="球队榜" name="team">
        <el-table v-loading="loading" :data="teamList" border>
          <el-table-column label="排名" prop="rankNo" width="70" />
          <el-table-column label="球队" prop="teamName" min-width="160" :show-overflow-tooltip="true" />
          <el-table-column label="维度" prop="rankTypeName" width="120" />
          <el-table-column label="统计值" prop="statValue" width="100" />
          <el-table-column label="场次" prop="games" width="70" />
          <el-table-column label="较上次" prop="rankChange" width="90">
            <template #default="scope">
              <span v-if="scope.row.rankChange == null">-</span>
              <span v-else-if="scope.row.rankChange > 0" style="color: #67c23a">↑{{ scope.row.rankChange }}</span>
              <span v-else-if="scope.row.rankChange < 0" style="color: #f56c6c">↓{{ Math.abs(scope.row.rankChange) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="快照日期" prop="snapshotDate" width="160">
            <template #default="scope">{{ parseTime(scope.row.snapshotDate, '{y}-{m}-{d}') }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球员榜" name="player">
        <el-table v-loading="loading" :data="playerList" border>
          <el-table-column label="排名" prop="rankNo" width="70" />
          <el-table-column label="球员" prop="displayName" min-width="120" :show-overflow-tooltip="true" />
          <el-table-column label="球队" prop="teamName" min-width="140" :show-overflow-tooltip="true" />
          <el-table-column label="维度" prop="rankTypeName" width="120" />
          <el-table-column label="统计值" prop="statValue" width="100" />
          <el-table-column label="场次" prop="games" width="70" />
          <el-table-column label="较上次" prop="rankChange" width="90">
            <template #default="scope">
              <span v-if="scope.row.rankChange == null">-</span>
              <span v-else-if="scope.row.rankChange > 0" style="color: #67c23a">↑{{ scope.row.rankChange }}</span>
              <span v-else-if="scope.row.rankChange < 0" style="color: #f56c6c">↓{{ Math.abs(scope.row.rankChange) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="快照日期" prop="snapshotDate" width="160">
            <template #default="scope">{{ parseTime(scope.row.snapshotDate, '{y}-{m}-{d}') }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup lang="ts" name="BasketRanking">
import { listPlayerRanking, listTeamRanking, rebuildRankingSeason } from '@/api/basket/ranking'
import { listSeasonByTournament } from '@/api/basket/season'
import { listTournament } from '@/api/basket/tournament'
import type {
  PlayerRankListItem,
  RankingQuery,
  SeasonListItem,
  TeamRankListItem,
  TournamentListItem
} from '@/types'
import { PLAYER_RANK_TYPE_OPTIONS, TEAM_RANK_TYPE_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const total = ref(0)
const activeTab = ref<'team' | 'player'>('team')
const teamList = ref<TeamRankListItem[]>([])
const playerList = ref<PlayerRankListItem[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const selectedTournamentId = ref<number>()

const queryParams = ref<RankingQuery>({
  pageNum: 1,
  pageSize: 20,
  seasonId: undefined,
  rankType: TEAM_RANK_TYPE_OPTIONS[0].value,
  keyword: undefined
})

const rankTypeOptions = computed(() =>
  activeTab.value === 'team' ? TEAM_RANK_TYPE_OPTIONS : PLAYER_RANK_TYPE_OPTIONS
)

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
  queryParams.value.seasonId = undefined
  teamList.value = []
  playerList.value = []
  if (!selectedTournamentId.value) return
  listSeasonByTournament(selectedTournamentId.value).then(res => {
    seasonOptions.value = res.data || []
    queryParams.value.seasonId = seasonOptions.value[0]?.id
    if (queryParams.value.seasonId) {
      getList()
    }
  })
}

function handleTournamentChange() {
  loadSeasons()
}

function handleSeasonChange() {
  handleQuery()
}

function handleTabChange() {
  queryParams.value.rankType = rankTypeOptions.value[0]?.value
  handleQuery()
}

function getList() {
  if (!queryParams.value.seasonId) return
  loading.value = true
  const req = activeTab.value === 'team'
    ? listTeamRanking(queryParams.value)
    : listPlayerRanking(queryParams.value)
  req.then(res => {
    if (activeTab.value === 'team') {
      teamList.value = (res.rows || []) as TeamRankListItem[]
    } else {
      playerList.value = (res.rows || []) as PlayerRankListItem[]
    }
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
  queryParams.value.keyword = undefined
  queryParams.value.rankType = rankTypeOptions.value[0]?.value
  handleQuery()
}

function handleRebuild() {
  if (!queryParams.value.seasonId) return
  proxy.$modal.confirm('重算将覆盖当前赛季的积分榜、赛季统计和排行榜结果。确认继续？').then(() =>
    rebuildRankingSeason(queryParams.value.seasonId!)
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
