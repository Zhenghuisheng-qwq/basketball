<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

export type StageOption = {
  value: string
  label: string
}

const props = defineProps<{
  modelValue: string
  options: StageOption[]
  variant?: 'dark' | 'paper'
  contextLabel?: string
  stageTitle?: string
  stageTitleEn?: string
  stageLive?: boolean
}>()

const { t } = useI18n()
const hasCenter = computed(() => Boolean(props.stageTitle))

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
    :class="[
      `stage-progress-header--${variant || 'paper'}`,
      { 'stage-progress-header--centered': hasCenter },
    ]"
  >
    <div class="stage-progress-header__titles">
      <h2 class="stage-progress-header__title">{{ t('eventsPage.stage.progress') }}</h2>
      <span class="stage-progress-header__subtitle">STAGE PROGRESS</span>
    </div>

    <div v-if="hasCenter" class="stage-progress-header__center">
      <p v-if="contextLabel || stageLive" class="stage-progress-header__context">
        <span v-if="contextLabel">{{ contextLabel }}</span>
        <span v-if="contextLabel && stageLive" class="stage-progress-header__context-sep" aria-hidden="true">·</span>
        <span v-if="stageLive" class="stage-progress-header__live-inline">
          <i aria-hidden="true" />
          {{ t('match.live') }}
        </span>
      </p>
      <h3 class="stage-progress-header__stage">{{ stageTitle }}</h3>
      <em v-if="stageTitleEn" class="stage-progress-header__stage-en">{{ stageTitleEn }}</em>
    </div>

    <div v-if="props.options.length > 1" class="stage-progress-header__selector">
      <label class="sr-only" for="stage-progress-select">{{ t('eventsPage.aria.selectStage') }}</label>
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
    <div v-else-if="hasCenter" class="stage-progress-header__selector" aria-hidden="true" />
  </header>
</template>
<style scoped lang="scss">
.stage-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: clamp(16px, 1.6vw, 22px);
}

.stage-progress-header--centered {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  column-gap: 16px;
}

.stage-progress-header__title {
  margin: 0;
  font-size: clamp(26px, 2.2vw, 31px);
  font-weight: 800;
  letter-spacing: 0.04em;
  line-height: 1.1;
}

.stage-progress-header__subtitle {
  display: block;
  margin-top: 6px;
  font-size: clamp(13px, 1.2vw, 16px);
  font-weight: 800;
  font-style: italic;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.stage-progress-header--paper {
  .stage-progress-header__title {
    color: #2a2a2a;
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

.stage-progress-header__center {
  min-width: 0;
  text-align: center;
}

.stage-progress-header__context {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 0 0 4px;
  color: rgba(17, 17, 17, 0.38);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.stage-progress-header__context-sep {
  letter-spacing: 0;
  opacity: 0.55;
}

.stage-progress-header__live-inline {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--cnpa-red, #e10613);
  letter-spacing: 0.12em;

  i {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: currentColor;
  }
}

.stage-progress-header__stage {
  margin: 0;
  color: #111;
  font-size: clamp(20px, 1.7vw, 28px);
  font-weight: 900;
  line-height: 1.15;
}

.stage-progress-header__stage-en {
  display: block;
  margin-top: 4px;
  color: var(--cnpa-red, #e10613);
  font-size: 11px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.stage-progress-header__selector {
  flex: none;
  align-self: center;
  justify-self: end;
}

.stage-progress-header__select {
  width: clamp(148px, 12vw, 196px);
  height: 38px;
  padding: 0 32px 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  background:
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='8' viewBox='0 0 12 8'%3E%3Cpath fill='%23ffffff' d='M1 1.5 6 6.5 11 1.5'/%3E%3C/svg%3E")
      no-repeat right 11px center / 11px 7px,
    #111111;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
  font-family: inherit;
  cursor: pointer;
  appearance: none;
  transition: border-color 0.2s ease;

  &:hover,
  &:focus-visible {
    border-color: var(--cnpa-red, #e10613);
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
  .stage-progress-header,
  .stage-progress-header--centered {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .stage-progress-header__center {
    text-align: left;
  }

  .stage-progress-header__context {
    justify-content: flex-start;
  }

  .stage-progress-header__selector {
    align-self: flex-end;
  }
}
</style>
