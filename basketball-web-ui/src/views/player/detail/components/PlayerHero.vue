<script setup lang="ts">
import { computed } from 'vue'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import TeamLogo from '@/components/team/TeamLogo.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import type { PlayerProfile } from '../types'

const props = defineProps<{
  profile: PlayerProfile
}>()

const heroBgStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg, rgba(0,0,0,0.82) 0%, rgba(0,0,0,0.55) 42%, rgba(0,0,0,0.28) 100%), url(${teamBackground})`,
}))

const teamForLogo = computed(() => ({
  id: props.profile.currentTeamId,
  name: props.profile.currentTeamName,
  shortName: props.profile.currentTeamAbbr,
  logoUrl: props.profile.currentTeamLogoUrl,
  code: props.profile.currentTeamAbbr,
}))

const infoItems = computed(() => [
  { label: '所属战队', value: props.profile.currentTeamName, link: `/tournament/team/${props.profile.currentTeamId}/overview` },
  { label: '比赛平台', value: props.profile.platform },
  { label: '主要位置', value: props.profile.position },
  { label: '当前阶段', value: props.profile.currentStage },
])
</script>

<template>
  <section class="player-detail-hero" aria-label="球员详情头部">
    <div class="player-detail-hero__bg" :style="heroBgStyle" aria-hidden="true" />
    <div class="player-detail-hero__accent" aria-hidden="true" />

    <div class="player-detail-hero__inner">
      <nav class="player-detail-hero__breadcrumb" aria-label="面包屑">
        <router-link to="/tournament">赛事</router-link>
        <span>/</span>
        <router-link to="/tournament">CNPA League</router-link>
        <span>/</span>
        <router-link :to="`/tournament/team/${profile.currentTeamId}/overview`">
          {{ profile.currentTeamAbbr }}
        </router-link>
        <span>/</span>
        <span class="is-current">{{ profile.displayName }}</span>
      </nav>

      <div class="player-detail-hero__main">
        <div class="player-detail-hero__left">
          <PlayerAvatar :name="profile.displayName" :avatar-url="profile.avatarUrl" size="hero" tone="gold" />
          <div class="player-detail-hero__summary">
            <div class="player-detail-hero__title-row">
              <h1 class="player-detail-hero__name">{{ profile.displayName }}</h1>
              <span v-if="profile.verified" class="player-detail-hero__badge">已认证</span>
            </div>
            <p class="player-detail-hero__registration">{{ profile.registrationText }}</p>
            <div class="player-detail-hero__meta">
              <span>所属战队 {{ profile.currentTeamName }}</span>
              <span class="player-detail-hero__meta-sep">·</span>
              <span>主要位置 {{ profile.position }}</span>
            </div>
          </div>
        </div>

        <div class="player-detail-hero__logo-bg" aria-hidden="true">
          <TeamLogo :team="teamForLogo" size="hero" />
        </div>
      </div>

      <div class="player-detail-hero__info-bar">
        <div
          v-for="(item, index) in infoItems"
          :key="item.label"
          class="player-detail-hero__info-item"
        >
          <span class="player-detail-hero__info-label">{{ item.label }}</span>
          <router-link v-if="item.link" :to="item.link" class="player-detail-hero__info-value is-link">
            {{ item.value }}
          </router-link>
          <strong v-else class="player-detail-hero__info-value">{{ item.value }}</strong>
          <span v-if="index < infoItems.length - 1" class="player-detail-hero__info-divider" aria-hidden="true" />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.player-detail-hero {
  position: relative;
  width: 100%;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 2px 2px 0 0;
}

.player-detail-hero__bg,
.player-detail-hero__accent {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.player-detail-hero__bg {
  background-size: cover;
  background-position: center;
}

.player-detail-hero__accent {
  background:
    linear-gradient(135deg, transparent 58%, rgba(201, 163, 90, 0.08) 58%, rgba(201, 163, 90, 0.02) 72%, transparent 72%),
    radial-gradient(circle at 88% 78%, rgba(225, 6, 19, 0.18), transparent 34%);
}

.player-detail-hero__inner {
  position: relative;
  z-index: 2;
  padding: 22px 0 0;
}

.player-detail-hero__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;

  a {
    color: rgba(255, 255, 255, 0.72);
    text-decoration: none;

    &:hover {
      color: #fff;
    }
  }

  .is-current {
    color: #fff;
    font-weight: 700;
  }
}

.player-detail-hero__main {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  min-height: 160px;
  padding-bottom: 24px;
}

.player-detail-hero__left {
  display: flex;
  align-items: center;
  gap: 22px;
  min-width: 0;
}

.player-detail-hero__summary {
  min-width: 0;
}

.player-detail-hero__title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.player-detail-hero__name {
  margin: 0;
  font-size: clamp(34px, 3.4vw, 48px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.02em;
}

.player-detail-hero__badge {
  padding: 4px 10px;
  border: 1px solid rgba(201, 163, 90, 0.72);
  background: rgba(201, 163, 90, 0.12);
  color: #f1d59a;
  font-size: 12px;
  font-weight: 800;
}

.player-detail-hero__registration {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
}

.player-detail-hero__meta {
  margin-top: 12px;
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
}

.player-detail-hero__meta-sep {
  margin: 0 8px;
}

.player-detail-hero__logo-bg {
  position: absolute;
  right: 0;
  top: 50%;
  opacity: 0.16;
  transform: translateY(-50%) scale(1.15);
  filter: grayscale(0.2);
  pointer-events: none;
}

.player-detail-hero__info-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(0, 0, 0, 0.34);
}

.player-detail-hero__info-item {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 18px;
}

.player-detail-hero__info-label {
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
}

.player-detail-hero__info-value {
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  text-decoration: none;

  &.is-link:hover {
    color: #f1d59a;
  }
}

.player-detail-hero__info-divider {
  position: absolute;
  top: 18px;
  right: 0;
  width: 1px;
  height: calc(100% - 36px);
  background: rgba(255, 255, 255, 0.12);
}

@media (max-width: 991px) {
  .player-detail-hero__info-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-detail-hero__logo-bg {
    opacity: 0.1;
    transform: translateY(-50%) scale(0.9);
  }
}

@media (max-width: 767px) {
  .player-detail-hero__main {
    flex-direction: column;
    align-items: flex-start;
  }

  .player-detail-hero__left {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
