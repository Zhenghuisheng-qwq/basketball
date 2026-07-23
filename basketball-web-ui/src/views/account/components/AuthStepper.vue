<script setup lang="ts">
defineProps<{
  current: 1 | 2 | 3
}>()

const steps = [
  { id: 1, num: '01', label: '创建账号' },
  { id: 2, num: '02', label: '邮箱验证' },
  { id: 3, num: '03', label: '完成' },
] as const
</script>

<template>
  <div class="auth-steps" aria-label="注册步骤">
    <template v-for="(step, index) in steps" :key="step.id">
      <div
        class="auth-step"
        :class="{
          'auth-step--active': current === step.id,
          'auth-step--done': current > step.id,
        }"
      >
        <strong>{{ current > step.id ? '✓' : step.num }}</strong>
        <span>{{ step.label }}</span>
      </div>
      <span v-if="index < steps.length - 1" class="auth-step-line" aria-hidden="true" />
    </template>
  </div>
</template>