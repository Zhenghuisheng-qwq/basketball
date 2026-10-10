<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useAppLocale } from '@/composables/useAppLocale'
import type { AppLocale } from '@/i18n/localePreference'

withDefaults(defineProps<{
  block?: boolean
}>(), {
  block: false,
})

const { t } = useI18n()
const { locale, setLocale } = useAppLocale()

const options: Array<{ value: AppLocale; labelKey: 'common.chinese' | 'common.english' }> = [
  { value: 'zh-CN', labelKey: 'common.chinese' },
  { value: 'en-US', labelKey: 'common.english' },
]

function select(next: AppLocale) {
  setLocale(next)
}
</script>

<template>
  <div
    class="locale-switch"
    :class="{ 'locale-switch--block': block }"
    role="group"
    :aria-label="t('common.language')"
  >
    <button
      v-for="option in options"
      :key="option.value"
      type="button"
      class="locale-switch__btn"
      :class="{ 'is-active': locale === option.value }"
      :aria-pressed="locale === option.value"
      @click="select(option.value)"
    >
      {{ t(option.labelKey) }}
    </button>
  </div>
</template>

<style scoped lang="scss">
.locale-switch {
  display: inline-flex;
  align-items: stretch;
  height: 32px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.2);
}

.locale-switch__btn {
  min-width: 40px;
  padding: 0 8px;
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-family: inherit;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;

  & + & {
    border-left: 1px solid rgba(255, 255, 255, 0.16);
  }

  &.is-active {
    background: rgba(255, 255, 255, 0.14);
    color: #fff;
  }

  &:hover {
    color: #fff;
  }
}

.locale-switch--block {
  display: flex;
  width: calc(100% - (var(--site-margin-inline) * 2));
  height: 40px;
  margin: 8px var(--site-margin-inline) 4px;

  .locale-switch__btn {
    flex: 1;
    height: 40px;
    font-size: 13px;
  }
}
</style>
