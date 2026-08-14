<template>
  <div v-if="overview" class="version-strip">
    <el-tag size="small" :type="ruleTagType">规则：{{ ruleText }}</el-tag>
    <el-tag size="small" :type="rosterTagType">名单：{{ rosterText }}</el-tag>
    <el-tag size="small" :type="structureTagType">结构：{{ structureText }}</el-tag>
    <el-tag size="small" type="info">排期：{{ scheduleText }}</el-tag>
    <el-tag size="small">发布：{{ publishText }}</el-tag>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { DivisionWorkflowOverviewVo } from '@/api/cnpa/event-v4/overview'

const props = defineProps<{ overview?: DivisionWorkflowOverviewVo }>()

const ruleLocked = computed(() => Boolean(props.overview?.rule?.locked))
const rosterLocked = computed(() => Boolean(props.overview?.roster?.locked))
const stages = computed(() => props.overview?.stages || [])
const hasActiveStructure = computed(() => stages.value.some(s => s.structureStatus === 'ACTIVE' && s.executionVersion != null))
const hasStaleStructure = computed(() => stages.value.some(s => s.structureStatus === 'STALE'))
const scheduledCount = computed(() => stages.value.reduce((sum, s) => sum + (s.scheduledUnits || 0), 0))
const totalUnits = computed(() => stages.value.reduce((sum, s) => sum + (s.totalUnits || 0), 0))
const hasPublish = computed(() => stages.value.some(s => s.publishStatus === 'PUBLISHED' || s.publishNo != null))

const ruleText = computed(() => {
  const no = props.overview?.rule?.ruleVersionNo
  if (ruleLocked.value) return no != null ? `已发布 V${no}` : '已发布'
  if (no != null) return `待发布 V${no}`
  return '尚未发布'
})
const rosterText = computed(() => {
  const no = props.overview?.roster?.currentRosterVersionNo
  if (rosterLocked.value) return no != null ? `已锁定 V${no}` : '已锁定'
  return '待锁定'
})
const structureText = computed(() => {
  if (hasStaleStructure.value) return '已失效，需重新生成'
  if (hasActiveStructure.value) {
    const ver = stages.value.map(s => s.executionVersion).filter((v): v is number => v != null)
    return ver.length ? `已生成 V${Math.max(...ver)}` : '已生成'
  }
  return rosterLocked.value ? '待生成' : '等待名单锁定'
})
const scheduleText = computed(() => {
  if (!hasActiveStructure.value) return '等待结构生成'
  if (totalUnits.value > 0 && scheduledCount.value >= totalUnits.value) return '已完成'
  if (scheduledCount.value > 0) return `${scheduledCount.value}/${totalUnits.value}`
  return '待排期'
})
const publishText = computed(() => {
  if (hasPublish.value) {
    const nos = stages.value.map(s => s.publishNo).filter((v): v is number => v != null)
    return nos.length ? `已发布 #${Math.max(...nos)}` : '已发布'
  }
  if (!hasActiveStructure.value) return '等待结构生成'
  if (totalUnits.value > 0 && scheduledCount.value >= totalUnits.value) return '待发布'
  return '等待排期完成'
})

const ruleTagType = computed(() => ruleLocked.value ? 'success' : 'info')
const rosterTagType = computed(() => rosterLocked.value ? 'success' : 'warning')
const structureTagType = computed(() => hasStaleStructure.value ? 'danger' : (hasActiveStructure.value ? 'success' : 'info'))
</script>

<style scoped>
.version-strip { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
</style>
