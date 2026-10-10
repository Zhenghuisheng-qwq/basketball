<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import defaultHeroImage from '@/assets/icon/home_top.png'

const { t } = useI18n()

export interface HomeHeroBanner {
  id: string
  image: string
  title?: string
  subtitle?: string
}

const props = defineProps<{
  banners: HomeHeroBanner[]
  loading?: boolean
  errorMessage?: string
}>()

const activeIndex = ref(0)
const activeBanner = computed(() => props.banners[activeIndex.value] ?? null)
const backgroundImage = computed(() => activeBanner.value?.image || defaultHeroImage)

function selectBanner(index: number) {
  activeIndex.value = index
}

function previousBanner() {
  if (!props.banners.length) return
  activeIndex.value = (activeIndex.value - 1 + props.banners.length) % props.banners.length
}

function nextBanner() {
  if (!props.banners.length) return
  activeIndex.value = (activeIndex.value + 1) % props.banners.length
}
</script>

<template>
  <section class="home-hero" aria-labelledby="home-hero-title">
    <div class="site-shell home-hero__shell">
      <div class="home-hero__frame">
        <img
          class="home-hero__image"
          :src="backgroundImage"
          :alt="activeBanner?.title || t('home.heroAlt')"
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
