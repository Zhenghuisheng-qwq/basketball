<template>
  <div v-if="overview" class="version-strip">
    <el-tag size="small">规则版本 {{ overview.rule?.ruleVersionNo != null ? 'V' + overview.rule.ruleVersionNo : '-' }}</el-tag>
    <el-tag size="small" type="success">名单版本 {{ overview.roster?.currentRosterVersionNo != null ? 'V' + overview.roster.currentRosterVersionNo : '-' }}</el-tag>
    <el-tag size="small" type="warning">结构 {{ structureText }}</el-tag>
    <el-tag size="small" type="info">排期修订 {{ scheduleText }}</el-tag>
    <el-tag size="small">发布 {{ publishText }}</el-tag>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DivisionWorkflowOverviewVo } from '@/api/cnpa/event-v4/overview'

const props = defineProps<{ overview?: DivisionWorkflowOverviewVo }>()

const structureText = computed(() => {
  const stages = props.overview?.stages || []
  const ver = stages.map(s => s.executionVersion).filter((v): v is number => v != null)
  return ver.length ? 'V' + Math.max(...ver) : '-'
})
const scheduleText = computed(() => {
  const stages = props.overview?.stages || []
  const ver = stages.map(s => s.scheduleRevision).filter((v): v is number => v != null)
  return ver.length ? 'R' + Math.max(...ver) : '-'
})
const publishText = computed(() => {
  const stages = props.overview?.stages || []
  const nos = stages.map(s => s.publishNo).filter((v): v is number => v != null)
  return nos.length ? '#' + Math.max(...nos) : '-'
})
</script>

<style scoped>
.version-strip { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
</style>
