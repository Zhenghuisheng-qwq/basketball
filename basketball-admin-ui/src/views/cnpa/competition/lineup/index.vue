<template>
  <div class="app-container">
    <el-alert type="warning" title="旧域已废弃" description="旧比赛阵容管理已废弃，请使用新版赛事管理菜单。" :closable="false" show-icon class="mb8" />
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="比赛ID">
        <el-input-number v-model="matchId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadMatchInfo" :disabled="!matchId">加载比赛</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="matchInfo" :column="3" border class="mb8">
      <el-descriptions-item label="比赛编号">{{ matchInfo.matchNo }}</el-descriptions-item>
      <el-descriptions-item label="主队">{{ matchInfo.homeTeamName }}</el-descriptions-item>
      <el-descriptions-item label="客队">{{ matchInfo.awayTeamName }}</el-descriptions-item>
    </el-descriptions>

    <el-tabs v-if="matchInfo" v-model="activeTeamTab" @tab-change="loadLineup">
      <el-tab-pane :label="`主队 - ${matchInfo.homeTeamName}`" name="home" />
      <el-tab-pane :label="`客队 - ${matchInfo.awayTeamName}`" name="away" />
    </el-tabs>

    <el-row v-if="matchInfo" :gutter="16">
      <el-col :span="10">
        <el-divider content-position="left">可选球员</el-divider>
        <el-table :data="availablePlayers" size="small" v-loading="availableLoading" max-height="420" @selection-change="handleAvailableSelection">
          <el-table-column type="selection" width="45" />
          <el-table-column label="球员" prop="playerName" min-width="100" />
          <el-table-column label="编号" prop="playerNo" width="80" />
          <el-table-column label="球衣号" prop="jerseyNo" width="70" />
        </el-table>
        <el-button type="primary" plain class="mt8" @click="addSelectedPlayers" v-hasPermi="['basket:lineup:edit']">加入名单</el-button>
      </el-col>
      <el-col :span="14">
        <el-divider content-position="left">出场名单</el-divider>
        <el-table :data="lineupPlayers" size="small" v-loading="lineupLoading" max-height="420">
          <el-table-column label="球员" prop="playerName" min-width="90" />
          <el-table-column label="球衣号" width="100">
            <template #default="scope">
              <el-input v-model="scope.row.jerseyNo" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="首发" width="70">
            <template #default="scope">
              <el-switch v-model="scope.row.isStarting" :active-value="1" :inactive-value="0" />
            </template>
          </el-table-column>
          <el-table-column label="队长" width="70">
            <template #default="scope">
              <el-switch v-model="scope.row.captainFlag" :active-value="1" :inactive-value="0" @change="handleCaptainChange(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="位置" width="90">
            <template #default="scope">
              <el-input v-model="scope.row.positionCode" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="scope">
              <el-button link type="danger" icon="Delete" @click="removePlayer(scope.$index)" v-hasPermi="['basket:lineup:edit']" />
            </template>
          </el-table-column>
        </el-table>
        <div class="mt8">
          <el-button type="primary" @click="saveLineup" v-hasPermi="['basket:lineup:edit']" :loading="saving">保存名单</el-button>
          <el-button type="success" @click="confirmLineup" v-hasPermi="['basket:lineup:confirm']">确认名单</el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="BasketMatchLineup">
import { getMatch } from '@/api/basket/matchSchedule'
import {
  confirmMatchLineup,
  getMatchLineup,
  listAvailableMatchPlayers,
  saveMatchLineup
} from '@/api/basket/matchLineup'
import type {
  AvailableMatchPlayer,
  MatchDetail,
  MatchLineupPlayer,
  MatchLineupPlayerItem
} from '@/types'

const { proxy } = getCurrentInstance()
const route = useRoute()

const matchId = ref<number>()
const matchInfo = ref<MatchDetail>()
const activeTeamTab = ref('home')
const availablePlayers = ref<AvailableMatchPlayer[]>([])
const availableSelection = ref<AvailableMatchPlayer[]>([])
const lineupPlayers = ref<MatchLineupPlayer[]>([])
const availableLoading = ref(false)
const lineupLoading = ref(false)
const saving = ref(false)

const currentTeamId = computed(() => {
  if (!matchInfo.value) return undefined
  return activeTeamTab.value === 'home' ? matchInfo.value.homeTeamId : matchInfo.value.awayTeamId
})

function loadMatchInfo() {
  if (!matchId.value) return
  getMatch(matchId.value).then(res => {
    matchInfo.value = res.data
    loadLineup()
  })
}

function loadLineup() {
  if (!matchId.value || !currentTeamId.value) return
  lineupLoading.value = true
  availableLoading.value = true
  getMatchLineup(matchId.value, currentTeamId.value).then(res => {
    lineupPlayers.value = (res.data?.players || []).map(p => ({ ...p }))
  }).finally(() => {
    lineupLoading.value = false
  })
  listAvailableMatchPlayers(matchId.value, currentTeamId.value).then(res => {
    availablePlayers.value = res.data || []
  }).finally(() => {
    availableLoading.value = false
  })
}

function handleAvailableSelection(rows: AvailableMatchPlayer[]) {
  availableSelection.value = rows
}

function addSelectedPlayers() {
  const existing = new Set(lineupPlayers.value.map(p => p.registrationPlayerId))
  availableSelection.value.forEach(p => {
    if (!p.registrationPlayerId || existing.has(p.registrationPlayerId)) return
    lineupPlayers.value.push({
      registrationPlayerId: p.registrationPlayerId,
      playerId: p.playerId,
      playerName: p.playerName,
      playerNo: p.playerNo,
      jerseyNo: p.jerseyNo,
      rosterRole: p.rosterRole,
      isStarting: p.isStarting ?? 0,
      captainFlag: p.isCaptain ?? 0,
      positionCode: '',
      sortNo: lineupPlayers.value.length + 1
    })
  })
}

function removePlayer(index: number) {
  lineupPlayers.value.splice(index, 1)
}

function handleCaptainChange(row: MatchLineupPlayer) {
  if (row.captainFlag !== 1) return
  lineupPlayers.value.forEach(p => {
    if (p !== row) p.captainFlag = 0
  })
}

function buildSavePayload(): MatchLineupPlayerItem[] {
  return lineupPlayers.value.map((p, index) => ({
    registrationPlayerId: p.registrationPlayerId,
    jerseyNo: p.jerseyNo,
    isStarting: p.isStarting,
    captainFlag: p.captainFlag,
    rosterRole: p.rosterRole,
    positionCode: p.positionCode,
    sortNo: index + 1
  }))
}

function saveLineup() {
  if (!matchId.value || !currentTeamId.value) return
  saving.value = true
  saveMatchLineup(matchId.value, currentTeamId.value, { players: buildSavePayload() }).then(() => {
    proxy.$modal.msgSuccess('保存成功')
    loadLineup()
  }).finally(() => {
    saving.value = false
  })
}

function confirmLineup() {
  if (!matchId.value || !currentTeamId.value) return
  proxy.$modal.confirm('确认提交该队出场名单？提交后将不可随意修改。').then(() =>
    saveMatchLineup(matchId.value!, currentTeamId.value!, { players: buildSavePayload() })
  ).then(() => confirmMatchLineup(matchId.value!, currentTeamId.value!)).then(() => {
    proxy.$modal.msgSuccess('名单已确认')
    loadLineup()
  }).catch(() => {})
}

if (route.query.matchId) {
  matchId.value = Number(route.query.matchId)
  loadMatchInfo()
}
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.mt8 {
  margin-top: 8px;
}
</style>
