<template>
  <div class="app-container">
    <el-alert title="统计榜单：支持按阶段/小组查看积分榜，排名依据来自结构化 rank reason" type="info" :closable="false" show-icon class="mb8" />

    <el-form :inline="true" class="mb8">
      <el-form-item label="赛季">
        <el-select v-model="seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="divisionId" clearable filterable placeholder="选择赛区" style="width: 220px" :disabled="!seasonId" @change="onDivisionChange">
          <el-option v-for="item in divisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段" v-if="stageOptions.length">
        <el-select v-model="stageId" clearable filterable placeholder="选择阶段" style="width: 200px" @change="loadStageStandings">
          <el-option v-for="item in stageOptions" :key="item.stageId" :label="`${item.stageOrder}. ${item.stageName}`" :value="item.stageId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadAll">查询</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="积分榜" name="standing">
        <template v-if="stageStandingScopes.length">
          <div v-for="scope in stageStandingScopes" :key="scope.groupScopeKey" class="mb8">
            <div class="scope-title">{{ scope.groupId ? `小组 #${scope.groupId}` : '总积分榜' }}
              <el-tag v-if="scope.manualRankPending" type="warning" size="small">需人工排序</el-tag>
            </div>
            <el-table :data="scope.rows" border size="small">
              <el-table-column label="排名" prop="rankNo" width="70" align="center" />
              <el-table-column label="球队" prop="teamName" />
              <el-table-column label="胜" prop="winCount" width="60" align="center" />
              <el-table-column label="负" prop="loseCount" width="60" align="center" />
              <el-table-column label="净胜分" prop="pointDiff" width="80" align="center" />
              <el-table-column label="积分" prop="standingPoints" width="80" align="center" />
              <el-table-column label="排名依据" min-width="160">
                <template #default="scope">{{ scope.row.decisiveReasonText || '-' }}</template>
              </el-table-column>
              <el-table-column label="人工" width="80" align="center">
                <template #default="scope">
                  <el-tag v-if="scope.row.manualRankPending" type="warning" size="small">待处理</el-tag>
                  <span v-else-if="scope.row.manualRankNo">#{{ scope.row.manualRankNo }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
        <el-table v-else :data="standingList" border size="small">
          <el-table-column label="排名" prop="rankNo" width="70" align="center" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="胜" prop="winCount" width="60" align="center" />
          <el-table-column label="负" prop="loseCount" width="60" align="center" />
          <el-table-column label="得分" prop="pointFor" width="80" align="center" />
          <el-table-column label="失分" prop="pointAgainst" width="80" align="center" />
          <el-table-column label="净胜分" prop="pointDiff" width="80" align="center" />
          <el-table-column label="积分" prop="standingPoints" width="80" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球队统计" name="team">
        <el-table :data="teamStatList" border size="small">
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="场次" prop="matchCount" width="70" align="center" />
          <el-table-column label="胜" prop="winCount" width="60" align="center" />
          <el-table-column label="负" prop="loseCount" width="60" align="center" />
          <el-table-column label="场均得分" prop="avgPointsFor" width="90" align="center" />
          <el-table-column label="场均失分" prop="avgPointsAgainst" width="90" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球员统计" name="player">
        <el-table :data="playerStatList" border size="small">
          <el-table-column label="球员" prop="playerName" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="场次" prop="matchCount" width="70" align="center" />
          <el-table-column label="得分" prop="points" width="70" align="center" />
          <el-table-column label="场均得分" prop="avgPoints" width="90" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="排行榜" name="rank">
        <el-form :inline="true" class="mb8">
          <el-form-item label="类型">
            <el-select v-model="rankType" style="width: 180px">
              <el-option label="球队积分榜" value="TEAM_STANDING" />
              <el-option label="球员得分" value="PLAYER_SCORE" />
              <el-option label="球员助攻" value="PLAYER_ASSIST" />
              <el-option label="球员篮板" value="PLAYER_REBOUND" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button @click="loadRank">刷新排行榜</el-button></el-form-item>
        </el-form>
        <el-table :data="rankList" border size="small">
          <el-table-column label="排名" prop="rankNo" width="70" align="center" />
          <el-table-column label="类型" width="140">
            <template #default="scope">{{ v4RankTypeLabel(scope.row.rankType) }}</template>
          </el-table-column>
          <el-table-column label="球员/球队" min-width="120">
            <template #default="scope">{{ scope.row.playerName || scope.row.teamName || '-' }}</template>
          </el-table-column>
          <el-table-column label="数值" prop="rankValue" width="90" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Stats">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listV4PlayerStat, listV4Rank, listV4Standing, listV4TeamStat } from '@/api/cnpa/event-v4/match-submission-audit'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import { listV4StageExecution, listV4StageStandings } from '@/api/cnpa/event-v4/stage-execution'
import { v4RankTypeLabel } from '@/utils/v4AdminDisplay'

const seasonId = ref<number>()
const divisionId = ref<number>()
const stageId = ref<number>()
const activeTab = ref('standing')
const standingList = ref<any[]>([])
const stageStandingScopes = ref<any[]>([])
const stageOptions = ref<any[]>([])
const teamStatList = ref<any[]>([])
const playerStatList = ref<any[]>([])
const rankList = ref<any[]>([])
const rankType = ref('TEAM_STANDING')
const seasonOptions = ref<V4AdminOptionVo[]>([])
const divisionOptions = ref<V4AdminOptionVo[]>([])

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadDivisions(sid?: number) {
  divisionOptions.value = []
  stageOptions.value = []
  stageId.value = undefined
  if (!sid) return
  const res = await listV4AdminDivisionOptions(sid)
  divisionOptions.value = res.data || []
}

async function onDivisionChange(did?: number) {
  stageOptions.value = []
  stageId.value = undefined
  stageStandingScopes.value = []
  if (!did) return
  const res = await listV4StageExecution(did)
  stageOptions.value = res.data || []
  if (stageOptions.value.length === 1) {
    stageId.value = stageOptions.value[0].stageId
    loadStageStandings()
  }
}

function onSeasonChange(sid?: number) {
  divisionId.value = undefined
  loadDivisions(sid)
}

function loadStageStandings() {
  if (!stageId.value) {
    stageStandingScopes.value = []
    return
  }
  listV4StageStandings(stageId.value).then(res => {
    stageStandingScopes.value = res.data || []
  })
}

function loadAll() {
  if (!divisionId.value) {
    ElMessage.warning('请选择平台赛区')
    return
  }
  const q = { divisionId: divisionId.value }
  if (stageId.value) {
    loadStageStandings()
  } else {
    listV4Standing(q).then(res => { standingList.value = res.data || [] })
  }
  listV4TeamStat(q).then(res => { teamStatList.value = res.data || [] })
  listV4PlayerStat(q).then(res => { playerStatList.value = res.data || [] })
  loadRank()
}

function loadRank() {
  if (!divisionId.value) return
  listV4Rank({ divisionId: divisionId.value, rankType: rankType.value }).then(res => { rankList.value = res.data || [] })
}

onMounted(async () => {
  await loadSeasonOptions()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.scope-title { font-weight: 600; margin-bottom: 6px; }
</style>
