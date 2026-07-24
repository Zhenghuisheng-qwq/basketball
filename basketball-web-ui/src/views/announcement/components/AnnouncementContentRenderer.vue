<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  text?: string | null
}>()

const paragraphs = computed(() => {
  const raw = props.text ?? ''
  if (!raw.trim()) return []
  return raw
    .replace(/\r\n/g, '\n')
    .split(/\n{2,}/)
    .map((part) => part.trim())
    .filter(Boolean)
})
</script>

<template>
  <div class="announcement-content-renderer">
    <p v-if="paragraphs.length === 0" class="announcement-content-renderer__empty">暂无正文内容</p>
    <p
      v-for="(paragraph, index) in paragraphs"
      :key="index"
      class="announcement-content-renderer__paragraph"
    >
      {{ paragraph }}
    </p>
  </div>
</template>

<style scoped lang="scss">
.announcement-content-renderer {
  color: #272727;
  font-size: 16px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

.announcement-content-renderer__paragraph {
  margin: 0 0 24px;
  white-space: pre-wrap;
}

.announcement-content-renderer__empty {
  margin: 0;
  color: var(--cnpa-paper-text-secondary, #666);
}
</style>
