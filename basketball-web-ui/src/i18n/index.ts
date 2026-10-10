import { createI18n } from 'vue-i18n'
import { resolveInitialLocale } from '@/i18n/localePreference'
import enUS from '@/i18n/messages/en-US'
import zhCN from '@/i18n/messages/zh-CN'

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: resolveInitialLocale(),
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
  },
  missingWarn: import.meta.env.DEV,
  fallbackWarn: import.meta.env.DEV,
})

export function translate(key: string, params?: Record<string, unknown>): string {
  return params ? String(i18n.global.t(key, params)) : String(i18n.global.t(key))
}
