<template>
  <el-empty :description="title">
    <p class="blocked-detail">{{ detail }}</p>
    <el-button v-if="actionText" type="primary" @click="$emit('action')">{{ actionText }}</el-button>
  </el-empty>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  detail?: string
  actionText?: string
}>()
defineEmits<{ action: [] }>()
</script>

<style scoped>
.blocked-detail { color: #909399; margin-bottom: 12px; }
</style>
