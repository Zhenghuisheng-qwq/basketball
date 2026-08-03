<template>
  <div class="app-container">
    <el-alert title="V4 赛事队伍快照（只读）：审核通过后自动生成 EventTeam / EventPlayer" type="info" :closable="false" show-icon class="mb8" />

    <el-tabs v-model="activeTab">
      <el-tab-pane label="赛事球队" name="team">
        <el-form :model="teamQuery" :inline="true" class="mb8">
          <el-form-item label="Season ID"><el-input-number v-model="teamQuery.seasonId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item label="Division ID"><el-input-number v-model="teamQuery.divisionId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item label="球队ID"><el-input-number v-model="teamQuery.sourceTeamId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadTeams">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="teamLoading" :data="teamList" border>
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="Season" prop="seasonTitle" min-width="140" />
          <el-table-column label="Division" prop="divisionName" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="队名快照" prop="teamNameSnapshot" min-width="140" />
          <el-table-column label="队长" prop="captainPlayerName" min-width="100" />
          <el-table-column label="编号" prop="eventTeamNo" width="120" />
          <el-table-column label="状态" prop="eventTeamStatus" width="90" />
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button link type="primary" @click="viewPlayers(scope.row)">球员</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="teamTotal" v-model:page="teamQuery.pageNum" v-model:limit="teamQuery.pageSize" @pagination="loadTeams" />
      </el-tab-pane>

      <el-tab-pane label="赛事球员" name="player">
        <el-form :model="playerQuery" :inline="true" class="mb8">
          <el-form-item label="EventTeam ID"><el-input-number v-model="playerQuery.eventTeamId" :min="1" controls-position="right" /></el-form-item>
          <el-form-item label="Division ID"><el-input-number v-model="playerQuery.divisionId" :min="1" controls-position="right" /></el-form-item>
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
          <el-table-column label="队长" width="70">
            <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="eventPlayerStatus" width="90" />
        </el-table>
        <pagination :total="playerTotal" v-model:page="playerQuery.pageNum" v-model:limit="playerQuery.pageSize" @pagination="loadPlayers" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4EventTeam">
import { listV4EventPlayer, listV4EventTeam, type BasketEventPlayerQuery, type BasketEventTeamQuery } from '@/api/cnpa/event-v4/event-team'

const activeTab = ref('team')
const teamLoading = ref(false)
const playerLoading = ref(false)
const teamList = ref<any[]>([])
const playerList = ref<any[]>([])
const teamTotal = ref(0)
const playerTotal = ref(0)
const teamQuery = reactive<BasketEventTeamQuery>({ pageNum: 1, pageSize: 10 })
const playerQuery = reactive<BasketEventPlayerQuery>({ pageNum: 1, pageSize: 10 })

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
  playerQuery.pageNum = 1
  loadPlayers()
}

loadTeams()
loadPlayers()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
