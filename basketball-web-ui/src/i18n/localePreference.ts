export const LOCALE_STORAGE_KEY = 'cnpa_web_locale'

export const APP_LOCALES = ['zh-CN', 'en-US'] as const

export type AppLocale = (typeof APP_LOCALES)[number]

export function isAppLocale(value: string | null | undefined): value is AppLocale {
  return value === 'zh-CN' || value === 'en-US'
}

export function readSavedLocale(): AppLocale | null {
  try {
    const raw = localStorage.getItem(LOCALE_STORAGE_KEY)
    return isAppLocale(raw) ? raw : null
  } catch {
    return null
  }
}

export function detectBrowserLocale(languages: readonly string[] = []): AppLocale {
  const list = languages.length > 0
    ? languages
    : (typeof navigator !== 'undefined'
      ? (navigator.languages?.length ? navigator.languages : [navigator.language])
      : [])

  for (const language of list) {
    const value = String(language || '').toLowerCase()
    if (value.startsWith('en')) return 'en-US'
    if (value.startsWith('zh')) return 'zh-CN'
  }
  return 'zh-CN'
}

/** Saved preference, then browser language, then zh-CN. Does not write storage. */
export function resolveInitialLocale(): AppLocale {
  return readSavedLocale() ?? detectBrowserLocale()
}

export function persistLocale(locale: AppLocale): void {
  localStorage.setItem(LOCALE_STORAGE_KEY, locale)
}

export function applyDocumentLang(locale: AppLocale): void {
  document.documentElement.lang = locale
}
