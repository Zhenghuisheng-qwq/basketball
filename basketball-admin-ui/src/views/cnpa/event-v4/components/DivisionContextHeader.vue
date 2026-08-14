<template>
  <el-card v-if="overview?.context" shadow="never" class="mb8">
    <div class="ctx-head">
      <div>
        <div class="ctx-title">当前赛事：{{ overview.context.divisionName }}</div>
        <div class="ctx-sub">
          {{ overview.context.seriesName || overview.context.seriesCode }}
          · {{ overview.context.seasonTitle }}
          · {{ overview.context.templateType || 'CUSTOM' }}
          <span v-if="overview.context.templateCode"> / {{ overview.context.templateCode }}</span>
        </div>
      </div>
      <el-button v-if="showSwitch" @click="$emit('switch')">切换赛事</el-button>
    </div>
    <el-descriptions :column="4" border size="small" class="mt8">
      <el-descriptions-item label="报名方式">{{ overview.context.registrationModeName }}</el-descriptions-item>
      <el-descriptions-item label="比赛模式">{{ overview.context.gameModeName }}</el-descriptions-item>
      <el-descriptions-item label="平台">{{ overview.context.platformName || overview.context.platformCode }}</el-descriptions-item>
      <el-descriptions-item label="工作流">{{ overview.context.rosterWorkflowModeName || '-' }}</el-descriptions-item>
    </el-descriptions>
    <VersionStatusStrip :overview="overview" />
    <el-alert v-if="overview.workflowHint" class="mt8" type="info" :closable="false" :title="overview.workflowHint" />
  </el-card>
</template>

<script setup lang="ts">
import type { DivisionWorkflowOverviewVo } from '@/api/cnpa/event-v4/overview'
import VersionStatusStrip from './VersionStatusStrip.vue'

defineProps<{
  overview?: DivisionWorkflowOverviewVo
  showSwitch?: boolean
}>()
defineEmits<{ switch: [] }>()
</script>

<style scoped>
.ctx-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.ctx-title { font-size: 16px; font-weight: 600; }
.ctx-sub { color: #909399; margin-top: 4px; }
.mt8 { margin-top: 8px; }
.mb8 { margin-bottom: 8px; }
</style>
