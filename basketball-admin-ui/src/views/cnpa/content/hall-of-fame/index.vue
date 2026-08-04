<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="球队荣誉" name="team" lazy>
        <TeamHonorPanel />
      </el-tab-pane>
      <el-tab-pane label="球员荣誉" name="player" lazy>
        <PlayerHonorPanel />
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

const activeTab = ref('team')
</script>
