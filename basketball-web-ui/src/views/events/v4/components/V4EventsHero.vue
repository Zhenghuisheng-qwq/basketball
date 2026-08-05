<script setup lang="ts">
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
  <section class="public-hero v4-events-list-hero" aria-label="CNPA 赛事中心">
    <div class="public-hero__bg" aria-hidden="true" />
    <div class="public-hero__overlay" aria-hidden="true" />

    <div class="site-shell public-hero__inner">
      <nav class="public-hero__breadcrumb" aria-label="面包屑">
        <router-link to="/" class="public-hero__crumb">首页</router-link>
        <span class="public-hero__crumb-sep">/</span>
        <span class="public-hero__crumb public-hero__crumb--current">赛事</span>
      </nav>

      <p class="public-hero__eyebrow">COMPETITION CENTER</p>
      <h1 class="public-hero__title">赛事中心</h1>
      <p class="public-hero__desc">
        选择你的赛场，参与 CNPA PC 与 PS/Xbox 线上篮球赛事。
      </p>

      <ul class="v4-events-list-hero__stats" aria-label="赛事概览">
        <li>
          <span class="v4-events-list-hero__stat-value">{{ seasonCount }}</span>
          <span class="v4-events-list-hero__stat-label">开放赛季</span>
        </li>
        <li>
          <span class="v4-events-list-hero__stat-value">{{ registrationOpenDivisionCount }}</span>
          <span class="v4-events-list-hero__stat-label">报名中赛区</span>
        </li>
        <li>
          <span class="v4-events-list-hero__stat-value">PC / PS-Xbox</span>
          <span class="v4-events-list-hero__stat-label">支持平台</span>
        </li>
        <li>
          <span class="v4-events-list-hero__stat-value">5V5 / 3V3</span>
          <span class="v4-events-list-hero__stat-label">比赛模式</span>
        </li>
      </ul>

      <div class="v4-events-list-hero__filter" role="tablist" aria-label="赛事系列筛选">
        <button
          v-for="item in V4_SERIES_FILTERS"
          :key="item.code || 'all'"
          type="button"
          role="tab"
          class="v4-events-list-hero__filter-btn"
          :class="{ 'is-active': activeSeries === item.code }"
          :aria-selected="activeSeries === item.code"
          @click="emit('select-series', item.code)"
        >
          {{ item.label }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.v4-events-list-hero__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: clamp(10px, 1.6vw, 18px);
  margin: clamp(22px, 3vw, 32px) 0 0;
  padding: 0;
  list-style: none;
}

.v4-events-list-hero__stats li {
  padding: 14px 16px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: rgba(0, 0, 0, 0.28);
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
}

.v4-events-list-hero__stat-value {
  display: block;
  color: #fff;
  font-size: clamp(15px, 1.5vw, 18px);
  font-weight: 900;
  line-height: 1.2;
}

.v4-events-list-hero__stat-label {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  font-weight: 700;
}

.v4-events-list-hero__filter {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: clamp(22px, 3vw, 30px);
}

.v4-events-list-hero__filter-btn {
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  background: rgba(0, 0, 0, 0.24);
  color: rgba(255, 255, 255, 0.88);
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.04em;
  cursor: pointer;
  clip-path: polygon(0 0, calc(100% - 8px) 0, 100% 8px, 100% 100%, 8px 100%, 0 calc(100% - 8px));
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;

  &:hover {
    border-color: rgba(255, 255, 255, 0.48);
    color: #fff;
  }

  &.is-active {
    border-color: #fff;
    background: rgba(225, 6, 19, 0.92);
    color: #fff;
    box-shadow: inset 0 -3px 0 rgba(0, 0, 0, 0.28);
  }
}

@media (max-width: 1023px) {
  .v4-events-list-hero__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .v4-events-list-hero__filter {
    flex-wrap: nowrap;
    overflow-x: auto;
    padding-bottom: 4px;
    scrollbar-width: none;
  }

  .v4-events-list-hero__filter-btn {
    flex: 0 0 auto;
  }
}
</style>
