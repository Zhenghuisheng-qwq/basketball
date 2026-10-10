import { defineStore } from 'pinia'
import { ref } from 'vue'
import { i18n } from '@/i18n'
import {
  applyDocumentLang,
  persistLocale,
  resolveInitialLocale,
  type AppLocale,
} from '@/i18n/localePreference'

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<AppLocale>(resolveInitialLocale())

  function apply(next: AppLocale, persist: boolean) {
    locale.value = next
    i18n.global.locale.value = next
    applyDocumentLang(next)
    if (persist) persistLocale(next)
  }

  function init() {
    apply(resolveInitialLocale(), false)
  }

  function setLocale(next: AppLocale) {
    if (locale.value === next) return
    apply(next, true)
  }

  return {
    locale,
    init,
    setLocale,
  }
})
