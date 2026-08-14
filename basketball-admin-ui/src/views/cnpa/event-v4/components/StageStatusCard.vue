<template>
  <el-card shadow="never" class="stage-card">
    <div class="stage-card__title">{{ stage.stageName }} · {{ stage.competitionFormatName }}</div>
    <el-descriptions :column="2" border size="small" class="mt8">
      <el-descriptions-item label="参赛来源">{{ stage.participantSource || '-' }}</el-descriptions-item>
      <el-descriptions-item label="参赛数">{{ stage.participantCount ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="赛程结构">{{ stage.structureStatusName || '尚未生成' }}</el-descriptions-item>
      <el-descriptions-item label="结构版本">{{ stage.executionVersion != null ? 'V' + stage.executionVersion : '-' }}</el-descriptions-item>
      <el-descriptions-item label="抽签/对阵">{{ stage.pairingStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="晋级">{{ stage.qualificationStatusName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="排期">{{ stage.scheduledUnits ?? 0 }} / {{ stage.totalUnits ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="占位">{{ stage.resolvedPlaceholders ?? 0 }} 已确定 / {{ stage.unresolvedPlaceholders ?? 0 }} 待确定</el-descriptions-item>
      <el-descriptions-item label="赛程发布">{{ stage.publishStatusName }} {{ stage.publishNo ? '#' + stage.publishNo : '' }}</el-descriptions-item>
      <el-descriptions-item label="结果进度">{{ stage.finishedCount ?? 0 }} / {{ stage.resultTotal ?? 0 }}</el-descriptions-item>
    </el-descriptions>
    <el-alert class="mt8" :type="hintType" :closable="false" :title="stage.workflowHint || stage.capabilities?.blockReason || '进行中'" />
    <div class="mt8">
      <el-button size="small" type="primary" @click="$emit('goto', 'structure')">去生成结构</el-button>
      <el-button size="small" @click="$emit('goto', 'schedule')">去排期</el-button>
      <el-button size="small" type="success" @click="$emit('goto', 'publish-preview')">去发布</el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StageWorkflowSummaryVo } from '@/api/cnpa/event-v4/overview'

const props = defineProps<{ stage: StageWorkflowSummaryVo }>()
defineEmits<{ goto: [target: 'structure' | 'schedule' | 'publish-preview'] }>()

const hintType = computed(() => {
  const hint = props.stage.workflowHint || props.stage.capabilities?.blockReason || ''
  if (props.stage.publishStatus === 'PUBLISHED') return 'success'
  if (props.stage.structureStatus === 'STALE' || hint.includes('ERROR') || hint.includes('失效')) return 'error'
  if (hint.includes('未') || hint.includes('等待') || hint.includes('尚未')) return 'warning'
  return 'info'
})
</script>

<style scoped>
.stage-card { margin-bottom: 12px; }
.stage-card__title { font-weight: 600; }
.mt8 { margin-top: 8px; }
</style>
