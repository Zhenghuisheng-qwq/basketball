<script setup lang="ts">
defineProps<{
  current: 1 | 2 | 3
}>()

const steps = [
  { id: 1, label: '01 创建账号' },
  { id: 2, label: '02 邮箱验证' },
  { id: 3, label: '03 完成' },
] as const
</script>

<template>
  <ol class="register-steps">
    <li
      v-for="step in steps"
      :key="step.id"
      class="register-steps__item"
      :class="{
        'is-active': current === step.id,
        'is-done': current > step.id,
      }"
    >
      {{ step.label }}
    </li>
  </ol>
</template>

<style scoped lang="scss">
.register-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0 0 28px;
  padding: 0;
  list-style: none;
}

.register-steps__item {
  padding: 8px 14px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.55);
  color: var(--cnpa-paper-text-muted);
  font-size: 12px;
  font-weight: 700;

  &.is-active {
    border-color: var(--cnpa-red);
    background: rgba(225, 6, 19, 0.08);
    color: var(--cnpa-red);
  }

  &.is-done {
    color: var(--cnpa-paper-text);
  }
}
</style>
