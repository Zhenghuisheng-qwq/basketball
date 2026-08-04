<template>
  <div class="app-container">
    <el-alert title="参赛队伍快照（只读）：审核通过后自动生成参赛球队与球员数据" type="info" :closable="false" show-icon class="mb8" />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="参赛球队" name="team">
        <el-form :model="teamQuery" :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="teamQuery.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onTeamSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="teamQuery.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!teamQuery.seasonId">
              <el-option v-for="item in teamDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadTeams">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="teamLoading" :data="teamList" border>
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="赛季" prop="seasonTitle" min-width="140" />
          <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="球队" prop="teamNameSnapshot" min-width="140" />
          <el-table-column label="队长" prop="captainPlayerName" min-width="100" />
          <el-table-column label="编号" prop="eventTeamNo" width="120" />
          <el-table-column label="状态" prop="eventTeamStatus" width="100" align="center">
            <template #default="scope">
              <el-tag size="small" class="nowrap-tag">{{ scope.row.eventTeamStatus === 'ACTIVE' ? '有效' : scope.row.eventTeamStatus }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button link type="primary" @click="viewPlayers(scope.row)">球员</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="teamTotal" v-model:page="teamQuery.pageNum" v-model:limit="teamQuery.pageSize" @pagination="loadTeams" />
      </el-tab-pane>

      <el-tab-pane label="参赛球员" name="player">
        <el-form :model="playerQuery" :inline="true" class="mb8">
          <el-form-item label="平台赛区">
            <el-select v-model="playerQuery.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px">
              <el-option v-for="item in playerDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadPlayers">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="playerLoading" :data="playerList" border>
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="球队" prop="teamNameSnapshot" min-width="120" />
          <el-table-column label="球员" prop="playerNameSnapshot" min-width="120" />
          <el-table-column label="Game ID" prop="gameIdSnapshot" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="位置" prop="position" width="80" />
          <el-table-column label="队长" width="70" align="center">
            <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="eventPlayerStatus" width="90" align="center">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.eventPlayerStatus === 'ACTIVE' ? '有效' : scope.row.eventPlayerStatus }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="playerTotal" v-model:page="playerQuery.pageNum" v-model:limit="playerQuery.pageSize" @pagination="loadPlayers" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4EventTeam">
import { onMounted, reactive, ref } from 'vue'
import { listV4EventPlayer, listV4EventTeam, type BasketEventPlayerQuery, type BasketEventTeamQuery } from '@/api/cnpa/event-v4/event-team'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'

const activeTab = ref('team')
const teamLoading = ref(false)
const playerLoading = ref(false)
const teamList = ref<any[]>([])
const playerList = ref<any[]>([])
const teamTotal = ref(0)
const playerTotal = ref(0)
const teamQuery = reactive<BasketEventTeamQuery>({ pageNum: 1, pageSize: 10 })
const playerQuery = reactive<BasketEventPlayerQuery>({ pageNum: 1, pageSize: 10 })
const seasonOptions = ref<V4AdminOptionVo[]>([])
const teamDivisionOptions = ref<V4AdminOptionVo[]>([])
const playerDivisionOptions = ref<V4AdminOptionVo[]>([])

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
  if (seasonOptions.value.length) {
    const allDivisions = await Promise.all(seasonOptions.value.map(s => listV4AdminDivisionOptions(s.id)))
    playerDivisionOptions.value = allDivisions.flatMap(r => r.data || [])
  }
}

async function loadTeamDivisions(seasonId?: number) {
  teamDivisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  teamDivisionOptions.value = res.data || []
}

function onTeamSeasonChange(seasonId?: number) {
  teamQuery.divisionId = undefined
  loadTeamDivisions(seasonId)
}

function loadTeams() {
  teamLoading.value = true
  listV4EventTeam(teamQuery).then(res => {
    teamList.value = res.rows || []
    teamTotal.value = res.total || 0
  }).finally(() => { teamLoading.value = false })
}

function loadPlayers() {
  playerLoading.value = true
  listV4EventPlayer(playerQuery).then(res => {
    playerList.value = res.rows || []
    playerTotal.value = res.total || 0
  }).finally(() => { playerLoading.value = false })
}

function viewPlayers(row: any) {
  activeTab.value = 'player'
  playerQuery.eventTeamId = row.id
  playerQuery.divisionId = row.divisionId
  playerQuery.pageNum = 1
  loadPlayers()
}

onMounted(async () => {
  await loadSeasonOptions()
  loadTeams()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
</style>
