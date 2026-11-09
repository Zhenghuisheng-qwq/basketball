import { computed, onBeforeUnmount, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { getPublicPageHero } from '@/api/publicHero'
import type { PublicPageHeroResp } from '@/api/types/public'

export function usePublicPageHero(
  pageKey: MaybeRefOrGetter<string>,
  seasonId?: MaybeRefOrGetter<string | number | null | undefined>,
) {
  const hero = ref<PublicPageHeroResp | null>(null)
  const failed = ref(false)
  const narrow = ref(false)
  let seq = 0

  const media = typeof window === 'undefined' ? null : window.matchMedia('(max-width: 600px)')
  const syncNarrow = () => {
    narrow.value = !!media?.matches
  }
  syncNarrow()
  media?.addEventListener('change', syncNarrow)
  onBeforeUnmount(() => media?.removeEventListener('change', syncNarrow))

  watch(
    () => [toValue(pageKey), String(toValue(seasonId) ?? '').trim()] as const,
    async ([key, season]) => {
      const current = ++seq
      failed.value = false
      hero.value = null
      if (!key || (key === 'EVENT_SEASON' && !season)) return
      try {
        const res = await getPublicPageHero(key, key === 'EVENT_SEASON' ? season : undefined)
        if (current !== seq) return
        hero.value = res.data ?? null
      } catch {
        if (current === seq) hero.value = null
      }
    },
    { immediate: true },
  )

  const imageUrl = computed(() => {
    if (failed.value || !hero.value?.configured) return ''
    if (narrow.value && hero.value.mobileImageUrl) return hero.value.mobileImageUrl
    return hero.value.imageUrl || ''
  })

  watch(imageUrl, (url) => {
    if (!url) return
    const probe = new Image()
    probe.onload = () => undefined
    probe.onerror = () => {
      if (imageUrl.value === url) failed.value = true
    }
    probe.src = url
  })

  const configured = computed(() => !failed.value && !!hero.value?.configured && !!imageUrl.value)
  const fullImage = computed(() => configured.value && hero.value?.mode === 'FULL_IMAGE')
  const objectPosition = computed(() => `${hero.value?.focusX ?? 50}% ${hero.value?.focusY ?? 50}%`)
  const maskOpacity = computed(() => {
    const value = Number(hero.value?.maskOpacity ?? 40)
    if (!Number.isFinite(value)) return 40
    return Math.max(0, Math.min(100, value))
  })
  const altText = computed(() => hero.value?.altText || '')

  return { configured, fullImage, imageUrl, objectPosition, maskOpacity, altText }
}
