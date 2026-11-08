<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import defaultHeroImage from '@/assets/icon/home_top.png'

const { t } = useI18n()

const ROTATE_MS = 5000
const SWIPE_PX = 48

export interface HomeHeroBanner {
  id: string
  image: string
  mobileImage?: string
  alt?: string
  title?: string
  subtitle?: string
  targetType?: string
  targetValue?: string
}

const props = defineProps<{
  banners: HomeHeroBanner[]
  loading?: boolean
  errorMessage?: string
}>()

const emit = defineEmits<{
  open: [banner: HomeHeroBanner]
}>()

const activeIndex = ref(0)
const paused = ref(false)
const narrow = ref(false)
let timer: number | null = null
let media: MediaQueryList | null = null
let touchStartX = 0

const activeBanner = computed(() => props.banners[activeIndex.value] ?? null)
const backgroundImage = computed(() => {
  const banner = activeBanner.value
  if (!banner) return defaultHeroImage
  if (narrow.value && banner.mobileImage) return banner.mobileImage
  return banner.image || defaultHeroImage
})
const imageAlt = computed(() => activeBanner.value?.alt || activeBanner.value?.title || t('home.heroAlt'))
const canOpen = computed(() => {
  const banner = activeBanner.value
  if (!banner?.targetValue?.trim()) return false
  return (banner.targetType || 'NONE').toUpperCase() !== 'NONE'
})

function selectBanner(index: number) {
  activeIndex.value = index
  restart()
}

function previousBanner() {
  if (props.banners.length < 2) return
  activeIndex.value = (activeIndex.value - 1 + props.banners.length) % props.banners.length
  restart()
}

function nextBanner() {
  if (props.banners.length < 2) return
  activeIndex.value = (activeIndex.value + 1) % props.banners.length
  restart()
}

function stop() {
  if (timer != null) {
    window.clearInterval(timer)
    timer = null
  }
}

function start() {
  stop()
  if (props.banners.length < 2 || paused.value) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  timer = window.setInterval(() => {
    activeIndex.value = (activeIndex.value + 1) % props.banners.length
  }, ROTATE_MS)
}

function restart() {
  start()
}

function pause() {
  paused.value = true
  stop()
}

function resume() {
  paused.value = false
  start()
}

function onMediaChange(event: MediaQueryListEvent) {
  narrow.value = event.matches
}

function onTouchStart(event: TouchEvent) {
  touchStartX = event.changedTouches[0]?.clientX ?? 0
  pause()
}

function onTouchEnd(event: TouchEvent) {
  const delta = (event.changedTouches[0]?.clientX ?? 0) - touchStartX
  if (delta <= -SWIPE_PX) nextBanner()
  else if (delta >= SWIPE_PX) previousBanner()
  resume()
}

function onFocusOut(event: FocusEvent) {
  const next = event.relatedTarget
  const current = event.currentTarget
  if (next instanceof Node && current instanceof HTMLElement && current.contains(next)) return
  resume()
}

function onFrameClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (target?.closest('button')) return
  if (!canOpen.value || !activeBanner.value) return
  emit('open', activeBanner.value)
}

watch(
  () => props.banners.length,
  (length) => {
    if (activeIndex.value >= length) activeIndex.value = 0
    start()
  },
)

onMounted(() => {
  media = window.matchMedia('(max-width: 600px)')
  narrow.value = media.matches
  media.addEventListener('change', onMediaChange)
  start()
})

onBeforeUnmount(() => {
  stop()
  media?.removeEventListener('change', onMediaChange)
})
</script>

<template>
  <section class="home-hero" aria-labelledby="home-hero-title">
    <div class="site-shell home-hero__shell">
      <div
        class="home-hero__frame"
        :class="{ 'home-hero__frame--linked': canOpen }"
        @mouseenter="pause"
        @mouseleave="resume"
        @focusin="pause"
        @focusout="onFocusOut"
        @touchstart.passive="onTouchStart"
        @touchend="onTouchEnd"
        @click="onFrameClick"
      >
        <img
          :key="`${activeBanner?.id || 'default'}-${backgroundImage}`"
          class="home-hero__image"
          :src="backgroundImage"
          :alt="imageAlt"
          fetchpriority="high"
          draggable="false"
        />
        <img
          class="home-hero__tear"
          src="@/assets/icon/sizhi.png"
          alt=""
          aria-hidden="true"
          draggable="false"
        />
        <h1 id="home-hero-title" class="home-hero__sr-only">CNPA</h1>
        <p v-if="activeBanner?.title || activeBanner?.subtitle" class="home-hero__caption">
          <strong v-if="activeBanner?.title">{{ activeBanner.title }}</strong>
          <span v-if="activeBanner?.subtitle">{{ activeBanner.subtitle }}</span>
        </p>

        <div
          v-if="props.banners.length > 1"
          class="home-hero__controls"
          role="group"
          :aria-label="t('home.heroControls')"
        >
          <button
            type="button"
            class="home-hero__arrow home-hero__arrow--previous"
            :aria-label="t('home.heroPrev')"
            @click="previousBanner"
          >
            <span aria-hidden="true">&#8249;</span>
          </button>

          <div class="home-hero__dots" role="tablist" :aria-label="t('home.heroDots')">
            <button
              v-for="(banner, index) in props.banners"
              :key="banner.id"
              type="button"
              class="home-hero__dot"
              :class="{ 'home-hero__dot--active': activeIndex === index }"
              role="tab"
              :aria-selected="activeIndex === index"
              :aria-label="t('home.heroSlide', { index: index + 1 })"
              @click="selectBanner(index)"
            />
          </div>

          <button
            type="button"
            class="home-hero__arrow home-hero__arrow--next"
            :aria-label="t('home.heroNext')"
            @click="nextBanner"
          >
            <span aria-hidden="true">&#8250;</span>
          </button>
        </div>

        <p v-if="props.loading" class="home-hero__status" aria-live="polite">{{ t('home.heroLoading') }}</p>
        <p v-else-if="props.errorMessage" class="home-hero__status home-hero__status--error" aria-live="polite">
          {{ t('home.heroFallback') }}
        </p>
      </div>
    </div>
  </section>
</template>
