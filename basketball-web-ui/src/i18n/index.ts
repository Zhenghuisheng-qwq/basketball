import { createI18n } from 'vue-i18n'
import { resolveInitialLocale } from '@/i18n/localePreference'
import enUS from '@/i18n/messages/en-US'
import zhCN from '@/i18n/messages/zh-CN'

function readMessage(tree: unknown, key: string): string | null {
  const value = key.split('.').reduce<unknown>((node, part) => {
    if (node && typeof node === 'object' && part in (node as Record<string, unknown>)) {
      return (node as Record<string, unknown>)[part]
    }
    return undefined
  }, tree)
  return typeof value === 'string' ? value : null
}

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
  fallbackWarn: false,
  missing: (_locale, key) => readMessage(zhCN, key) ?? '',
})

export function translate(key: string, params?: Record<string, unknown>): string {
  return params ? String(i18n.global.t(key, params)) : String(i18n.global.t(key))
}
