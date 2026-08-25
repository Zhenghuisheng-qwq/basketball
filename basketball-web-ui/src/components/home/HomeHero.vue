<script setup lang="ts">
import { computed, ref } from 'vue'
import defaultHeroImage from '@/assets/icon/home_top.png'

export interface HomeHeroBanner {
  id: string
  image: string
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
    <div class="home-hero__background" aria-hidden="true">
      <img
        class="home-hero__image"
        :src="backgroundImage"
        alt=""
        fetchpriority="high"
        draggable="false"
      />
      <div class="home-hero__veil" />
      <div class="home-hero__texture" />
    </div>

    <div class="site-shell home-hero__shell">
      <div class="home-hero__brand">
        <h1 id="home-hero-title" class="home-hero__title">CNPA</h1>
        <p class="home-hero__english">CHINA PRO-AM ASSOCIATION</p>
        <p class="home-hero__positioning">中国玩家篮球竞技赛事平台</p>
        <p class="home-hero__description">
          <span>CNPA 以赛事为核心</span>
          <span>连接球员、战队、内容</span>
          <span>构建属于中国玩家的篮球电竞赛事平台</span>
        </p>
      </div>
    </div>

    <div
      v-if="props.banners.length > 1"
      class="home-hero__controls"
      role="group"
      aria-label="CNPA 宣传视觉轮播控制"
    >
      <button
        type="button"
        class="home-hero__arrow home-hero__arrow--previous"
        aria-label="上一张宣传视觉"
        @click="previousBanner"
      >
        <span aria-hidden="true">&#8249;</span>
      </button>

      <div class="home-hero__dots" role="tablist" aria-label="选择宣传视觉">
        <button
          v-for="(banner, index) in props.banners"
          :key="banner.id"
          type="button"
          class="home-hero__dot"
          :class="{ 'home-hero__dot--active': activeIndex === index }"
          role="tab"
          :aria-selected="activeIndex === index"
          :aria-label="`切换到第 ${index + 1} 张宣传视觉`"
          @click="selectBanner(index)"
        />
      </div>

      <button
        type="button"
        class="home-hero__arrow home-hero__arrow--next"
        aria-label="下一张宣传视觉"
        @click="nextBanner"
      >
        <span aria-hidden="true">&#8250;</span>
      </button>
    </div>

    <p v-if="props.loading" class="home-hero__status" aria-live="polite">品牌视觉加载中</p>
    <p v-else-if="props.errorMessage" class="home-hero__status home-hero__status--error" aria-live="polite">
      宣传视觉暂不可用，已展示品牌视觉
    </p>
  </section>
</template>
