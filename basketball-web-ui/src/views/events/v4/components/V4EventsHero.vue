<script setup lang="ts">
import PublicHero from '@/components/layout/PublicHero.vue'
import { V4_SERIES_FILTERS } from '@/utils/v4Display'

defineProps<{
  activeSeries: string
  seasonCount: number
  registrationOpenDivisionCount: number
}>()

const emit = defineEmits<{
  'select-series': [code: string]
}>()
</script>

<template>
  <PublicHero variant="competition" aria-label="CNPA 赛事中心">
    <nav class="v4-events-hero__breadcrumb" aria-label="面包屑">
      <router-link to="/" class="v4-events-hero__crumb">首页</router-link>
      <span class="v4-events-hero__crumb-sep">/</span>
      <span class="v4-events-hero__crumb v4-events-hero__crumb--current">赛事</span>
    </nav>

    <div class="v4-events-hero__intro">
      <p class="v4-events-hero__eyebrow">COMPETITION CENTER</p>
      <h1 class="v4-events-hero__title">赛事中心</h1>
      <p class="v4-events-hero__desc">
        选择你的赛场，参与 CNPA PC 与 PS/Xbox 线上篮球赛事。
      </p>
    </div>

    <ul class="v4-events-hero__meta" aria-label="赛事概览">
      <li class="v4-events-hero__meta-item">
        <div class="v4-events-hero__meta-text">
          <strong class="v4-events-hero__meta-title">开放赛季</strong>
          <span class="v4-events-hero__meta-value">{{ seasonCount }}</span>
        </div>
      </li>
      <li class="v4-events-hero__meta-item">
        <div class="v4-events-hero__meta-text">
          <strong class="v4-events-hero__meta-title">报名中赛区</strong>
          <span class="v4-events-hero__meta-value">{{ registrationOpenDivisionCount }}</span>
        </div>
      </li>
      <li class="v4-events-hero__meta-item">
        <div class="v4-events-hero__meta-text">
          <strong class="v4-events-hero__meta-title">支持平台</strong>
          <span class="v4-events-hero__meta-value">PC / PS-Xbox</span>
        </div>
      </li>
      <li class="v4-events-hero__meta-item">
        <div class="v4-events-hero__meta-text">
          <strong class="v4-events-hero__meta-title">比赛模式</strong>
          <span class="v4-events-hero__meta-value">5V5 / 3V3</span>
        </div>
      </li>
    </ul>

    <template #after>
      <nav class="v4-events-hero__tabs" role="tablist" aria-label="赛事系列筛选">
        <button
          v-for="item in V4_SERIES_FILTERS"
          :key="item.code || 'all'"
          type="button"
          role="tab"
          class="v4-events-hero__tab"
          :class="{ 'is-active': activeSeries === item.code }"
          :aria-selected="activeSeries === item.code"
          @click="emit('select-series', item.code)"
        >
          {{ item.label }}
        </button>
      </nav>
    </template>
  </PublicHero>
</template>

<style scoped lang="scss">
.v4-events-hero__breadcrumb {
  position: absolute;
  top: clamp(18px, 2.4vw, 28px);
  left: 0;
  z-index: 3;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 13px;
  font-weight: 700;
}

.v4-events-hero__crumb {
  color: rgba(255, 255, 255, 0.72);
  text-decoration: none;
  transition: color 0.2s ease;

  &:hover {
    color: #fff;
  }

  &--current {
    color: #fff;
  }
}

.v4-events-hero__crumb-sep {
  color: rgba(255, 255, 255, 0.36);
}

.v4-events-hero__intro {
  position: absolute;
  top: clamp(52px, 6vw, 88px);
  left: 0;
  z-index: 3;
  max-width: min(100%, 560px);
}

.v4-events-hero__eyebrow {
  margin: 0 0 8px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.v4-events-hero__title {
  margin: 0 0 10px;
  color: #fff;
  font-size: clamp(32px, 4.8vw, 52px);
  font-weight: 900;
  line-height: 1.08;
  text-shadow: 0 2px 18px rgba(0, 0, 0, 0.45);
}

.v4-events-hero__desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
  font-size: clamp(14px, 1.6vw, 17px);
  line-height: 1.65;
  text-shadow: 0 1px 12px rgba(0, 0, 0, 0.42);
}

/* 磨砂统计卡片：复用 master LeagueHeroMeta 质感 */
.v4-events-hero__meta {
  --hero-meta-right: 3.8%;
  --hero-meta-bottom: 5%;
  --hero-meta-width: 43%;

  position: absolute;
  right: var(--hero-meta-right);
  bottom: var(--hero-meta-bottom);
  left: auto;
  z-index: 3;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: stretch;
  gap: clamp(8px, 0.75vw, 12px);
  width: var(--hero-meta-width);
  margin: 0;
  padding: 0;
  list-style: none;
  box-sizing: border-box;
}

.v4-events-hero__meta-item {
  display: flex;
  align-items: center;
  min-width: 0;
  min-height: clamp(56px, 4.2vw, 68px);
  padding: clamp(10px, 0.85vw, 14px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  background: rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(10px) saturate(120%);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  box-sizing: border-box;
}

.v4-events-hero__meta-text {
  flex: 1;
  min-width: 0;
}

.v4-events-hero__meta-title {
  display: block;
  overflow: hidden;
  color: #f5f5f5;
  font-size: clamp(12px, 0.95vw, 16px);
  font-weight: 700;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.v4-events-hero__meta-value {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.74);
  font-size: clamp(11px, 0.82vw, 14px);
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 系列筛选：复用 master LeagueTabs */
.v4-events-hero__tabs {
  display: inline-flex;
  align-items: stretch;
  width: fit-content;
  max-width: 100%;
  min-height: 64px;
  margin-top: 0;
  background: #080808;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-top: none;
  border-radius: 0 0 2px 2px;
  box-sizing: border-box;
}

.v4-events-hero__tab {
  position: relative;
  flex: 0 0 auto;
  min-width: clamp(88px, 8vw, 120px);
  padding: 0 clamp(20px, 2vw, 36px);
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s ease, background 0.2s ease;

  &:hover {
    color: #fff;
    background: rgba(225, 6, 19, 0.08);
  }

  &.is-active {
    color: #fff;
    background: linear-gradient(180deg, rgba(225, 6, 19, 0.28) 0%, rgba(8, 8, 8, 0.2) 100%);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 3px;
      background: var(--cnpa-red, #e10613);
    }
  }
}

@media (min-width: 1600px) {
  .v4-events-hero__meta {
    --hero-meta-width: 42%;
  }
}

@media (min-width: 1200px) and (max-width: 1365px) {
  .v4-events-hero__meta {
    --hero-meta-width: 48%;
  }
}

@media (min-width: 992px) and (max-width: 1199px) {
  .v4-events-hero__meta {
    --hero-meta-width: 52%;
  }
}

@media (max-width: 991px) {
  .v4-events-hero__meta {
    --hero-meta-right: 3.2%;
    --hero-meta-bottom: 3.5%;
    --hero-meta-width: min(92%, 520px);
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .v4-events-hero__breadcrumb {
    position: relative;
    top: auto;
    left: auto;
    padding-top: 12px;
  }

  .v4-events-hero__intro {
    position: relative;
    top: auto;
    left: auto;
    max-width: none;
    padding: 18px 0 12px;
  }

  .v4-events-hero__meta {
    position: relative;
    right: auto;
    bottom: auto;
    width: 100%;
    margin-top: 8px;
  }

  .v4-events-hero__tabs {
    display: flex;
    width: 100%;
    min-height: 56px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .v4-events-hero__tab {
    min-width: 72px;
    padding: 0 16px;
    font-size: 14px;
  }
}

@media (max-width: 420px) {
  .v4-events-hero__meta {
    grid-template-columns: 1fr;
  }
}
</style>
