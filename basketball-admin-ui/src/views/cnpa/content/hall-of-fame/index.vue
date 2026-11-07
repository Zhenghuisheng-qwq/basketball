<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="周最佳" name="weekly" lazy>
        <PlayerHonorPanel :locked-codes="weeklyCodes" />
      </el-tab-pane>
      <el-tab-pane label="赛事荣誉" name="event" lazy>
        <PlayerHonorPanel :locked-codes="eventCodes" />
      </el-tab-pane>
      <el-tab-pane label="历届冠军" name="champions" lazy>
        <TeamHonorPanel :locked-codes="championCodes" />
      </el-tab-pane>
      <el-tab-pane label="球员荣誉" name="player" lazy>
        <PlayerHonorPanel :locked-codes="playerCodes" />
      </el-tab-pane>
      <el-tab-pane label="荣誉头像" name="icon" lazy>
        <HonorIconPanel />
      </el-tab-pane>
      <el-tab-pane label="球队荣誉" name="team" lazy>
        <TeamHonorPanel :locked-codes="teamCodes" />
      </el-tab-pane>
      <el-tab-pane label="名人堂展示" name="hall" lazy>
        <HallOfFameDisplayPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="BasketHallOfFame">
import { ref } from 'vue'
import TeamHonorPanel from '../team-honor/index.vue'
import PlayerHonorPanel from '../player-honor/index.vue'
import HallOfFameDisplayPanel from './HallOfFameDisplayPanel.vue'
import HonorIconPanel from './HonorIconPanel.vue'

const activeTab = ref('weekly')
const weeklyCodes = ['WEEKLY_BEST']
const eventCodes = ['MVP', 'ALL_TOURNAMENT', 'ALL_DEFENSIVE']
const championCodes = ['CHAMPION', 'RUNNER_UP', 'THIRD_PLACE']
const playerCodes = ['PLAYER_HONOR', 'SCORING_CHAMPION', 'REBOUND_CHAMPION', 'ASSIST_CHAMPION', 'STEAL_CHAMPION', 'BLOCK_CHAMPION']
const teamCodes = ['TEAM_HONOR']
</script>
