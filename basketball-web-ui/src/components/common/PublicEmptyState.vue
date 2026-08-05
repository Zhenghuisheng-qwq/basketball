<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="public-empty-state" role="status">
    <div class="public-empty-state__icon" aria-hidden="true">
      <span />
      <span />
      <span />
    </div>
    <p class="public-empty-state__title">{{ title }}</p>
    <p v-if="description" class="public-empty-state__desc">{{ description }}</p>
  </div>
</template>

<style scoped lang="scss">
.public-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: clamp(280px, 36vw, 360px);
  padding: 40px 24px;
  border: 1px dashed var(--paper-border, rgba(20, 20, 20, 0.1));
  background: rgba(255, 255, 255, 0.42);
  text-align: center;
}

.public-empty-state__icon {
  display: grid;
  grid-template-columns: repeat(3, 10px);
  gap: 6px;
  margin-bottom: 4px;

  span {
    width: 10px;
    height: 10px;
    border: 2px solid rgba(21, 21, 21, 0.18);
    transform: rotate(45deg);
  }
}

.public-empty-state__title {
  margin: 0;
  color: var(--ink-color, #151515);
  font-size: 16px;
  font-weight: 800;
}

.public-empty-state__desc {
  margin: 0;
  max-width: 420px;
  color: var(--muted-color, #747474);
  font-size: 14px;
  line-height: 1.65;
}
</style>
