<template>
  <div class="app-container">
    <el-alert title="V4 统计榜单：按 Division 隔离，仅统计 FINISHED + PUBLISHED 比赛" type="info" :closable="false" show-icon class="mb8" />

    <el-form :inline="true" class="mb8">
      <el-form-item label="Division ID"><el-input-number v-model="divisionId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadAll">查询</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="积分榜 Standing" name="standing">
        <el-table :data="standingList" border size="small">
          <el-table-column label="排名" prop="rankNo" width="70" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="胜" prop="winCount" width="60" />
          <el-table-column label="负" prop="loseCount" width="60" />
          <el-table-column label="得分" prop="pointFor" width="80" />
          <el-table-column label="失分" prop="pointAgainst" width="80" />
          <el-table-column label="净胜分" prop="pointDiff" width="80" />
          <el-table-column label="积分" prop="standingPoints" width="80" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球队统计 TeamStat" name="team">
        <el-table :data="teamStatList" border size="small">
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="场次" prop="matchCount" width="70" />
          <el-table-column label="胜" prop="winCount" width="60" />
          <el-table-column label="负" prop="loseCount" width="60" />
          <el-table-column label="场均得分" prop="avgPointsFor" width="90" />
          <el-table-column label="篮板" prop="rebound" width="70" />
          <el-table-column label="助攻" prop="assist" width="70" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球员统计 PlayerStat" name="player">
        <el-table :data="playerStatList" border size="small">
          <el-table-column label="球员" prop="playerName" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="场次" prop="matchCount" width="70" />
          <el-table-column label="得分" prop="points" width="70" />
          <el-table-column label="场均得分" prop="avgPoints" width="90" />
          <el-table-column label="篮板" prop="rebound" width="70" />
          <el-table-column label="助攻" prop="assist" width="70" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="排名 Rank" name="rank">
        <el-form :inline="true" class="mb8">
          <el-form-item label="类型">
            <el-select v-model="rankType" style="width: 180px">
              <el-option label="球队积分榜" value="TEAM_STANDING" />
              <el-option label="球员得分" value="PLAYER_SCORE" />
              <el-option label="球员助攻" value="PLAYER_ASSIST" />
              <el-option label="球员篮板" value="PLAYER_REBOUND" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button @click="loadRank">刷新 Rank</el-button></el-form-item>
        </el-form>
        <el-table :data="rankList" border size="small">
          <el-table-column label="排名" prop="rankNo" width="70" />
          <el-table-column label="类型" prop="rankType" width="140" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="球员" prop="playerName" />
          <el-table-column label="值" prop="rankValue" width="90" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Stats">
import { ref } from 'vue'
import { listV4PlayerStat, listV4Rank, listV4Standing, listV4TeamStat } from '@/api/cnpa/event-v4/match-submission-audit'

const divisionId = ref<number | undefined>(5)
const activeTab = ref('standing')
const standingList = ref<any[]>([])
const teamStatList = ref<any[]>([])
const playerStatList = ref<any[]>([])
const rankList = ref<any[]>([])
const rankType = ref('TEAM_STANDING')

function loadAll() {
  if (!divisionId.value) return
  const q = { divisionId: divisionId.value }
  listV4Standing(q).then(res => { standingList.value = res.data || [] })
  listV4TeamStat(q).then(res => { teamStatList.value = res.data || [] })
  listV4PlayerStat(q).then(res => { playerStatList.value = res.data || [] })
  loadRank()
}

function loadRank() {
  if (!divisionId.value) return
  listV4Rank({ divisionId: divisionId.value, rankType: rankType.value }).then(res => { rankList.value = res.data || [] })
}

loadAll()
</script>
