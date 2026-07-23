<script setup lang="ts">
defineProps<{
  current: 1 | 2 | 3
}>()

const steps = [
  { id: 1, label: '创建账号', short: '01' },
  { id: 2, label: '邮箱验证', short: '02' },
  { id: 3, label: '完成', short: '03' },
] as const
</script>

<template>
  <ol class="auth-stepper" aria-label="注册步骤">
    <li
      v-for="(step, index) in steps"
      :key="step.id"
      class="auth-stepper__item"
      :class="{
        'auth-stepper__item--active': current === step.id,
        'auth-stepper__item--done': current > step.id,
      }"
    >
      <span class="auth-stepper__index">
        <span v-if="current > step.id" class="auth-stepper__check" aria-hidden="true">✓</span>
        <span v-else>{{ step.short }}</span>
      </span>
      <span class="auth-stepper__label">{{ step.label }}</span>
      <span v-if="index < steps.length - 1" class="auth-stepper__line" aria-hidden="true" />
    </li>
  </ol>
</template>

<style scoped lang="scss">
.auth-stepper {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1fr);
  align-items: center;
  gap: clamp(12px, 1.6vw, 28px);
  width: 100%;
  margin: 0 0 clamp(24px, 3vw, 44px);
  padding: 0;
  list-style: none;
}

.auth-stepper__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  color: #555;
  font-weight: 700;
}

.auth-stepper__item--active {
  color: #d5080d;
}

.auth-stepper__item--done {
  color: #151515;
}

.auth-stepper__index {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  width: clamp(28px, 2.5vw, 36px);
  height: clamp(28px, 2.5vw, 36px);
  border: 1px solid rgba(15, 15, 15, 0.25);
  border-radius: 50%;
  font-size: clamp(18px, 1.7vw, 28px);
  line-height: 1;
}

.auth-stepper__item--active .auth-stepper__index {
  border-color: #d5080d;
  background: rgba(213, 8, 13, 0.08);
}

.auth-stepper__item--done .auth-stepper__index {
  border-color: #151515;
  background: rgba(21, 21, 21, 0.06);
}

.auth-stepper__check {
  font-size: clamp(14px, 1.2vw, 18px);
  font-weight: 900;
}

.auth-stepper__label {
  font-size: clamp(12px, 1vw, 16px);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.auth-stepper__line {
  display: none;
}

@media (max-width: 767px) {
  .auth-stepper {
    gap: 8px;
  }

  .auth-stepper__label {
    font-size: 11px;
  }
}
</style>
