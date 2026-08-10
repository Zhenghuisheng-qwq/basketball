<script setup lang="ts">
import type { LeagueHeroMetaIcon, LeagueHeroMetaItem } from '../types/events'

defineProps<{
  items: LeagueHeroMetaItem[]
}>()

const iconLabels: Record<LeagueHeroMetaIcon, string> = {
  calendar: '赛季',
  trophy: '战队',
  stage: '阶段',
  gamepad: '平台',
}
</script>
<template>
  <div class="league-hero-meta" aria-label="赛事基本信息">
    <div v-for="item in items" :key="item.key" class="league-hero-meta__item">
      <div class="league-hero-meta__icon" aria-hidden="true">
        <svg
          v-if="item.icon === 'calendar'"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
        >
          <rect x="3.5" y="5.5" width="17" height="15" rx="1.5" />
          <path d="M8 3.5v3M16 3.5v3M3.5 10h17" stroke-linecap="round" />
        </svg>
        <svg
          v-else-if="item.icon === 'trophy'"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
        >
          <path d="M7 4h10v3a5 5 0 0 1-10 0V4Z" />
          <path d="M9 17h6M10 20h4M12 12v5" stroke-linecap="round" />
          <path d="M5 4H4a2 2 0 0 0 2 2M19 4h1a2 2 0 0 1-2 2" stroke-linecap="round" />
        </svg>
        <svg
          v-else-if="item.icon === 'stage'"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
        >
          <circle cx="12" cy="12" r="8.5" />
          <path d="M12 8v4l2.5 2.5" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <svg
          v-else
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
        >
          <path
            d="M8 11h8a3 3 0 0 0 0-6H9a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h6a3 3 0 0 0 0-6H8"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </div>

      <div class="league-hero-meta__text">
        <strong class="league-hero-meta__title">{{ item.title }}</strong>
        <span class="league-hero-meta__value">{{ item.value }}</span>
      </div>

      <span class="sr-only">{{ iconLabels[item.icon] }}</span>
    </div>
  </div>
</template>
<style scoped lang="scss">
.league-hero-meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: stretch;
  gap: clamp(8px, 0.75vw, 12px);
  width: 100%;
  padding: 0;
  color: #f5f5f5;
  background: transparent;
  box-sizing: border-box;
}

.league-hero-meta__item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  column-gap: clamp(5px, 0.5vw, 8px);
  min-width: 0;
  min-height: clamp(56px, 4.2vw, 68px);
  padding: clamp(10px, 0.85vw, 14px) clamp(10px, 0.9vw, 14px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  background: rgba(0, 0, 0, 0.38);
  backdrop-filter: blur(10px) saturate(120%);
  -webkit-backdrop-filter: blur(10px) saturate(120%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.06);
  box-sizing: border-box;
}

.league-hero-meta__icon {
  width: clamp(20px, 1.5vw, 26px);
  height: clamp(20px, 1.5vw, 26px);
  flex: none;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  color: rgba(255, 255, 255, 0.86);

  svg {
    display: block;
    width: 100%;
    height: 100%;
  }
}

.league-hero-meta__text {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.league-hero-meta__title {
  display: block;
  overflow: hidden;
  color: #f5f5f5;
  font-size: clamp(12px, 0.95vw, 16px);
  font-weight: 700;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.league-hero-meta__value {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: rgba(255, 255, 255, 0.74);
  font-size: clamp(11px, 0.82vw, 14px);
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (min-width: 992px) and (max-width: 1199px) {
  .league-hero-meta__item {
    column-gap: 5px;
    padding: 10px;
  }

  .league-hero-meta__icon {
    width: 20px;
    height: 20px;
  }
}

@media (max-width: 991px) {
  .league-hero-meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    padding: 0;
    background: transparent;
  }

  .league-hero-meta__item {
    min-height: 58px;
    padding: 10px 12px;
  }
}

@media (max-width: 767px) {
  .league-hero-meta {
    padding: 0;
  }

  .league-hero-meta__icon {
    width: 22px;
    height: 22px;
  }

  .league-hero-meta__title {
    font-size: 13px;
  }

  .league-hero-meta__value {
    font-size: 12px;
  }
}

@media (max-width: 420px) {
  .league-hero-meta {
    grid-template-columns: 1fr;
  }
}
</style>