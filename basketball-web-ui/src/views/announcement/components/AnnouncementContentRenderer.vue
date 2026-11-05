<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { renderPublicMarkdown } from '@/utils/publicMarkdown'

const props = defineProps<{
  text?: string | null
}>()

const { t } = useI18n()

const html = computed(() => renderPublicMarkdown(props.text))
</script>

<template>
  <div class="announcement-content-renderer">
    <div v-if="html" class="announcement-content-renderer__html" v-html="html"></div>
    <p v-else class="announcement-content-renderer__empty">{{ t('publicPages.announcement.noBody') }}</p>
  </div>
</template>

<style scoped lang="scss">
.announcement-content-renderer {
  color: #272727;
  font-size: 16px;
  line-height: 1.9;
  word-break: break-word;
}

.announcement-content-renderer__empty {
  margin: 0;
  color: var(--cnpa-paper-text-secondary, #666);
}

.announcement-content-renderer__html {
  :deep(p) {
    margin: 0 0 16px;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4) {
    margin: 28px 0 12px;
    color: #111;
    font-weight: 800;
    line-height: 1.35;
  }

  :deep(h1) {
    font-size: 28px;
  }

  :deep(h2) {
    font-size: 22px;
  }

  :deep(h3),
  :deep(h4) {
    font-size: 18px;
  }

  :deep(strong) {
    font-weight: 800;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0 0 16px;
    padding-left: 1.4em;
  }

  :deep(li) {
    margin: 0 0 8px;
  }

  :deep(img),
  :deep(video) {
    max-width: 100%;
    height: auto;
  }

  :deep(a) {
    color: #111;
  }
}
</style>
