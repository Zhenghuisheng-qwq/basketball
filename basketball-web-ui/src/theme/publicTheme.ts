import { reactive } from 'vue'
import type { Router, RouteLocationNormalized } from 'vue-router'
import { getPublicSiteTheme } from '@/api/publicTheme'
import type { PublicSiteThemeResp } from '@/api/types/public'

const THEME_VARS = [
  '--theme-primary',
  '--theme-secondary',
  '--theme-accent',
  '--theme-content',
  '--theme-card',
  '--theme-table-head',
  '--theme-border',
  '--theme-text',
  '--theme-text-muted',
  '--theme-on-accent',
  '--theme-primary-hover',
  '--theme-primary-active',
  '--theme-focus',
  '--theme-disabled',
  '--theme-bg',
  '--theme-bg-image',
  '--theme-bg-position',
  '--theme-bg-attachment',
] as const

export const publicThemeState = reactive({
  ready: false,
  custom: false,
})

let pendingCustom = false
let pendingVars: Record<string, string> = {}
let inflight: Promise<void> | null = null
let narrowMedia: MediaQueryList | null = null
let narrowHandler: (() => void) | null = null

export function installPublicTheme(router: Router) {
  router.beforeEach((to) => {
    inflight = syncPublicTheme(to)
    return inflight
  })
}

export async function ensurePublicThemeApplied(route: RouteLocationNormalized, el: HTMLElement | null) {
  if (inflight) await inflight
  else await syncPublicTheme(route)
  paint(el)
  if (resolveThemeQuery(route).skip || el) {
    publicThemeState.ready = true
    publicThemeState.custom = pendingCustom
  }
}

export function resolveThemeQuery(route: RouteLocationNormalized) {
  const path = route.path
  if (
    path.startsWith('/account')
    || path.startsWith('/login')
    || path.startsWith('/register')
    || path.startsWith('/forgot-password')
  ) {
    return { skip: true, scope: '', seasonId: '' }
  }
  if (path === '/' || path === '/home') return { skip: false, scope: 'HOME', seasonId: '' }
  if (
    path.startsWith('/data')
    || path.startsWith('/hall-of-fame')
    || path.startsWith('/announcements')
    || path.startsWith('/partners')
  ) {
    return { skip: false, scope: 'BASE', seasonId: '' }
  }
  const param = route.params.seasonId
  const query = route.query.seasonId
  const seasonId = typeof param === 'string' && param.trim()
    ? param.trim()
    : (typeof query === 'string' ? query.trim() : '')
  if (seasonId) return { skip: false, scope: 'SEASON', seasonId }
  return { skip: false, scope: 'BASE', seasonId: '' }
}

async function syncPublicTheme(route: RouteLocationNormalized) {
  const target = resolveThemeQuery(route)
  if (target.skip) {
    pendingCustom = false
    pendingVars = {}
    paint(document.querySelector('.site-main'))
    publicThemeState.custom = false
    publicThemeState.ready = true
    return
  }
  let theme: PublicSiteThemeResp
  try {
    const res = await getPublicSiteTheme(target.scope, target.seasonId || undefined)
    theme = res.data ?? { configured: false, source: 'DEFAULT' }
  } catch {
    theme = { configured: false, source: 'DEFAULT' }
  }
  pendingCustom = !!theme.configured
  pendingVars = theme.configured ? varsOf(theme) : {}
  const el = document.querySelector<HTMLElement>('.site-main')
  if (el) {
    paint(el)
    publicThemeState.custom = pendingCustom
    publicThemeState.ready = true
  }
  watchNarrow(theme)
  probeBackground(theme)
}

function watchNarrow(theme: PublicSiteThemeResp) {
  if (typeof window === 'undefined') return
  if (narrowMedia && narrowHandler) narrowMedia.removeEventListener('change', narrowHandler)
  narrowMedia = window.matchMedia('(max-width: 600px)')
  narrowHandler = () => {
    if (!pendingCustom) return
    pendingVars['--theme-bg-image'] = imageValue(theme, narrowMedia?.matches)
    paint(document.querySelector('.site-main'))
  }
  narrowMedia.addEventListener('change', narrowHandler)
}

function probeBackground(theme: PublicSiteThemeResp) {
  const url = (narrowMedia?.matches && theme.mobileBgImageUrl) || theme.bgImageUrl || ''
  if (!/^https?:\/\//i.test(url)) return
  const probe = new Image()
  probe.onload = () => undefined
  probe.onerror = () => {
    if (!pendingVars['--theme-bg-image']?.includes(url)) return
    pendingVars['--theme-bg-image'] = 'none'
    paint(document.querySelector('.site-main'))
  }
  probe.src = url
}

function paint(el: HTMLElement | null) {
  if (!el) return
  if (!pendingCustom) {
    el.removeAttribute('data-theme')
    THEME_VARS.forEach((name) => el.style.removeProperty(name))
    return
  }
  el.setAttribute('data-theme', 'custom')
  THEME_VARS.forEach((name) => {
    const value = pendingVars[name]
    if (value) el.style.setProperty(name, value)
    else el.style.removeProperty(name)
  })
}

function varsOf(theme: PublicSiteThemeResp) {
  const colors = theme.colors || {}
  const primary = hex(colors.primary) || '#e10613'
  const derived = derive(primary)
  const display = theme.bgDisplay || 'TOP_CENTER'
  return {
    '--theme-primary': primary,
    '--theme-secondary': hex(colors.secondary) || '#080808',
    '--theme-accent': hex(colors.accent) || primary,
    '--theme-content': hex(colors.content) || '#e9e7e7',
    '--theme-card': hex(colors.card) || '#ffffff',
    '--theme-table-head': hex(colors.tableHead) || '#111111',
    '--theme-border': hex(colors.border) || 'rgba(25, 25, 25, 0.11)',
    '--theme-text': hex(colors.text) || '#171717',
    '--theme-text-muted': hex(colors.textMuted) || '#666666',
    '--theme-on-accent': hex(colors.onAccent) || '#ffffff',
    '--theme-primary-hover': derived.hover,
    '--theme-primary-active': derived.active,
    '--theme-focus': derived.focus,
    '--theme-disabled': derived.disabled,
    '--theme-bg': hex(theme.bgColor) || '#050505',
    '--theme-bg-image': imageValue(theme, narrowMedia?.matches),
    '--theme-bg-position': display === 'TOP_CENTER' ? 'center top' : 'center center',
    '--theme-bg-attachment': display === 'FIXED' ? 'fixed' : 'scroll',
  }
}

function imageValue(theme: PublicSiteThemeResp, narrow?: boolean) {
  const mobile = narrow && theme.mobileBgImageUrl ? theme.mobileBgImageUrl : ''
  const url = mobile || theme.bgImageUrl || ''
  if (!/^https?:\/\//i.test(url)) return 'none'
  return `url("${url.replace(/"/g, '')}")`
}

function hex(value?: string | null) {
  if (!value) return ''
  return /^#[0-9a-fA-F]{6}$/.test(value) ? value : ''
}

function derive(primary: string) {
  return {
    hover: mix(primary, '#ffffff', 0.16),
    active: mix(primary, '#000000', 0.18),
    focus: rgba(primary, 0.4),
    disabled: mix(primary, '#9b9b9b', 0.62),
  }
}

function mix(source: string, target: string, amount: number) {
  const a = rgb(source)
  const b = rgb(target)
  if (!a || !b) return source
  const channel = (from: number, to: number) => Math.max(0, Math.min(255, Math.round(from + (to - from) * amount)))
  return `#${[channel(a.r, b.r), channel(a.g, b.g), channel(a.b, b.b)].map((item) => item.toString(16).padStart(2, '0')).join('')}`
}

function rgba(source: string, alpha: number) {
  const color = rgb(source)
  if (!color) return source
  return `rgba(${color.r}, ${color.g}, ${color.b}, ${alpha})`
}

function rgb(value: string) {
  const matched = /^#([0-9a-fA-F]{6})$/.exec(value)
  if (!matched) return null
  const n = Number.parseInt(matched[1], 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}
