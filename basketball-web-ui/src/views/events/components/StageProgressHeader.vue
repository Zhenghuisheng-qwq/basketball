<script setup lang="ts">
export type StageOption = {
  value: string
  label: string
}

const props = defineProps<{
  modelValue: string
  options: StageOption[]
  variant?: 'dark' | 'paper'
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

function onStageChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:modelValue', value)
}
</script>
<template>
  <header
    class="stage-progress-header"
    :class="[`stage-progress-header--${variant || 'paper'}`]"
  >
    <div class="stage-progress-header__titles">
      <h2 class="stage-progress-header__title">赛事进度</h2>
      <span class="stage-progress-header__subtitle">STAGE PROGRESS</span>
    </div>

    <div class="stage-progress-header__selector">
      <label class="sr-only" for="stage-progress-select">选择阶段</label>
      <select
        id="stage-progress-select"
        class="stage-progress-header__select"
        :value="modelValue"
        @change="onStageChange"
      >
        <option v-for="option in props.options" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
    </div>
  </header>
</template>
<style scoped lang="scss">
.stage-progress-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
  margin-bottom: clamp(18px, 2vw, 28px);
}

.stage-progress-header__title {
  margin: 0;
  font-size: clamp(28px, 2.1vw, 38px);
  font-weight: 900;
  line-height: 1.1;
}

.stage-progress-header__subtitle {
  display: block;
  margin-top: 6px;
  font-size: clamp(12px, 0.9vw, 15px);
  font-weight: 800;
  font-style: italic;
  letter-spacing: 0.04em;
}

.stage-progress-header--paper {
  .stage-progress-header__title {
    color: #111111;
  }

  .stage-progress-header__subtitle {
    color: #e10613;
  }
}

.stage-progress-header--dark {
  .stage-progress-header__title {
    color: #f5f5f5;
  }

  .stage-progress-header__subtitle {
    color: var(--cnpa-red);
  }
}

.stage-progress-header__selector {
  flex: none;
}

.stage-progress-header__select {
  width: clamp(160px, 14vw, 220px);
  height: 46px;
  padding: 0 36px 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 3px;
  background:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%23ffffff' d='M1 1.5 6 6.5 11 1.5'/%3E%3C/svg%3E")
      no-repeat right 12px center / 12px 8px,
    #111111;
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  appearance: none;
  transition: border-color 0.2s ease;

  &:hover,
  &:focus-visible {
    border-color: rgba(225, 6, 19, 0.45);
    outline: none;
  }

  option {
    color: #111111;
    background: #ffffff;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 767px) {
  .stage-progress-header {
    flex-direction: column;
    gap: 16px;
  }

  .stage-progress-header__selector {
    align-self: flex-end;
  }
}
</style>
