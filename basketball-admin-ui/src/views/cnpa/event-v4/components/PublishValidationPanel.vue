<template>
  <div>
    <el-alert
      :type="preview?.canPublish ? 'success' : 'error'"
      :closable="false"
      :title="preview?.canPublish ? '检查通过，可以发布' : '存在阻断问题，不能发布'"
      class="mb8"
    />
    <div v-for="(e, i) in (preview?.errors || [])" :key="'e' + i" class="issue error">阻断问题：{{ e.message }}</div>
    <div v-for="(w, i) in (preview?.warnings || [])" :key="'w' + i" class="issue warn">警告：{{ w.message }}</div>
    <el-empty v-if="!(preview?.errors?.length || preview?.warnings?.length)" description="没有校验问题" />
  </div>
</template>

<script setup lang="ts">
import type { StagePublishPreviewVo } from '@/api/cnpa/event-v4/stage-execution'

defineProps<{ preview?: StagePublishPreviewVo | null }>()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.issue { font-size: 13px; margin-bottom: 6px; }
.issue.error { color: #f56c6c; }
.issue.warn { color: #e6a23c; }
</style>
