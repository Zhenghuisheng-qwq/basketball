<script setup lang="ts">
import { computed } from 'vue'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import platformIcon from '@/assets/icon/person/platform.png'
import positionIcon from '@/assets/icon/person/site.png'
import stageIcon from '@/assets/icon/person/stage.png'
import TeamLogo from '@/components/team/TeamLogo.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import type { PlayerProfile } from '../types'

const props = defineProps<{
  profile: PlayerProfile
}>()

const heroBgStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg, rgba(0,0,0,0.82) 0%, rgba(0,0,0,0.55) 48%, rgba(0,0,0,0.32) 100%), url(${teamBackground})`,
}))

const teamForLogo = computed(() => ({
  id: props.profile.currentTeamId,
  name: props.profile.currentTeamName,
  shortName: props.profile.currentTeamAbbr,
  logoUrl: props.profile.currentTeamLogoUrl,
  code: props.profile.currentTeamAbbr,
}))

const eventName = computed(() =>
  props.profile.seasonName || props.profile.seriesName || '--',
)

const seasonYear = computed(() => {
  const matched = String(props.profile.seasonName || '').match(/\d{4}/)
  return matched?.[0] || ''
})

const registrationLine = computed(() => {
  if (!props.profile.verified) return 'CNPA PLAYER'
  return seasonYear.value
    ? `CNPA ${seasonYear.value} 赛季注册球员`
    : 'CNPA 赛季注册球员'
})

const identityLine = computed(() => {
  const team = props.profile.currentTeamAbbr || props.profile.currentTeamName
  const position = props.profile.position
  const parts: string[] = []
  if (team && team !== '--') parts.push(`所属战队 ${team}`)
  if (position && position !== '--') parts.push(`主打位置 ${position}`)
  return parts.join('  |  ')
})

const teamPath = computed(() => {
  const teamId = props.profile.eventTeamId || props.profile.currentTeamId
  if (!teamId) return undefined
  return {
    path: `/tournament/team/${teamId}/overview`,
    query: props.profile.divisionId ? { divisionId: props.profile.divisionId } : {},
  }
})

const infoItems = computed(() => [
  {
    key: 'team',
    label: '所属战队',
    value: props.profile.currentTeamAbbr || props.profile.currentTeamName,
    secondary: eventName.value,
    logo: true,
  },
  {
    key: 'platform',
    label: '比赛平台',
    value: props.profile.platform,
    icon: platformIcon,
  },
  {
    key: 'position',
    label: '主打位置',
    value: props.profile.position,
    icon: positionIcon,
  },
  {
    key: 'stage',
    label: '当前阶段',
    value: props.profile.currentStage,
    icon: stageIcon,
  },
])
</script>
<template>
  <section class="player-detail-hero" aria-label="球员详情头部">
    <div class="player-detail-hero__bg" :style="heroBgStyle" aria-hidden="true" />
    <div class="player-detail-hero__logo-bg" aria-hidden="true">
      <TeamLogo :team="teamForLogo" size="hero" />
    </div>

    <div class="player-detail-hero__inner">
      <nav class="player-detail-hero__breadcrumb" aria-label="面包屑">
        <router-link to="/tournament">赛事</router-link>
        <template v-if="profile.seriesName">
          <span>/</span>
          <router-link
            :to="{
              path: '/tournament',
              query: {
                ...(profile.seriesCode ? { series: String(profile.seriesCode).toUpperCase() } : {}),
                ...(profile.divisionId ? { divisionId: profile.divisionId } : {}),
              },
            }"
          >
            {{ profile.seriesName }}
          </router-link>
        </template>
        <template v-if="profile.seasonName && profile.seasonName !== profile.seriesName">
          <span>/</span>
          <span>{{ profile.seasonName }}</span>
        </template>
        <span>/</span>
        <router-link v-if="teamPath" :to="teamPath">{{ profile.currentTeamAbbr }}</router-link>
        <span v-else>{{ profile.currentTeamAbbr }}</span>
        <span>/</span>
        <span class="is-current">{{ profile.displayName }}</span>
      </nav>

      <div class="player-detail-hero__main">
        <PlayerAvatar
          :name="profile.displayName"
          :avatar-url="profile.avatarUrl"
          :player-id="profile.id"
          size="hero"
          tone="gold"
        />
        <div class="player-detail-hero__summary">
          <div class="player-detail-hero__title-row">
            <h1 class="player-detail-hero__name">{{ profile.displayName }}</h1>
            <span
              v-for="tag in profile.playerPenaltyTags"
              :key="tag.tagLabel"
              class="player-detail-hero__badge player-detail-hero__badge--penalty"
              :title="[tag.penaltyLabel, tag.statusLabel, tag.effectiveStartTime?.slice(0, 10)].filter(Boolean).join(' · ')"
            >{{ tag.tagLabel }}</span>
            <span
              v-if="profile.eventTeamDisqualified"
              class="player-detail-hero__badge player-detail-hero__badge--dq"
              title="所属球队在本届赛事中的参赛资格已取消"
            >所属球队赛事资格已取消</span>
            <span
              v-else-if="profile.verified && !profile.playerPenaltyTags?.length"
              class="player-detail-hero__badge"
            >✓ 已认证</span>
          </div>
          <p class="player-detail-hero__registration">{{ registrationLine }}</p>
          <p v-if="identityLine" class="player-detail-hero__identity">{{ identityLine }}</p>
        </div>
      </div>

      <div class="player-detail-hero__info-bar">
        <div
          v-for="item in infoItems"
          :key="item.key"
          class="player-detail-hero__info-item"
        >
          <span v-if="item.logo" class="player-detail-hero__info-logo">
            <TeamLogo :team="teamForLogo" size="sm" />
          </span>
          <img v-else-if="item.icon" :src="item.icon" alt="" class="player-detail-hero__info-icon" />
          <div class="player-detail-hero__info-copy">
            <span class="player-detail-hero__info-label">{{ item.label }}</span>
            <strong class="player-detail-hero__info-value">{{ item.value }}</strong>
            <em v-if="item.secondary" class="player-detail-hero__info-secondary">{{ item.secondary }}</em>
          </div>
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
  background: #050505;
  color: #fff;
}

.player-detail-hero__bg,
.player-detail-hero__logo-bg {
  position: absolute;
  pointer-events: none;
}

.player-detail-hero__bg {
  inset: 0;
  background-size: cover;
  background-position: center;
}

.player-detail-hero__logo-bg {
  right: clamp(-8px, 2vw, 24px);
  top: 46%;
  z-index: 0;
  width: clamp(180px, 22vw, 280px);
  height: clamp(180px, 22vw, 280px);
  opacity: 0.08;
  filter: grayscale(0.2);
  transform: translateY(-50%);

  :deep(.team-logo),
  :deep(.team-logo--hero) {
    width: 100%;
    height: 100%;
  }
}

.player-detail-hero__inner {
  position: relative;
  z-index: 2;
  padding: 18px 0 0;
}

.player-detail-hero__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0 0 18px;
  color: rgba(255, 255, 255, 0.56);
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
  display: flex;
  align-items: center;
  gap: clamp(18px, 2.4vw, 28px);
  min-width: 0;
  padding-bottom: 18px;
}

.player-detail-hero :deep(.player-avatar--hero) {
  flex: 0 0 clamp(128px, 11vw, 156px);
  width: clamp(128px, 11vw, 156px);
  height: clamp(128px, 11vw, 156px);
  border: 2px solid rgba(201, 163, 90, 0.92);
  box-shadow: 0 0 0 4px rgba(201, 163, 90, 0.16);
}

.player-detail-hero__summary {
  min-width: 0;
}

.player-detail-hero__title-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.player-detail-hero__name {
  margin: 0;
  font-size: clamp(42px, 3.6vw, 52px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0;
}

.player-detail-hero__info-logo {
  display: grid;
  flex: 0 0 40px;
  place-items: center;
  width: 40px;
  height: 40px;

  :deep(.team-logo) {
    width: 40px;
    height: 40px;
  }
}

.player-detail-hero__badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--cnpa-red, #e10613);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;

  &--penalty,
  &--dq {
    background: rgba(225, 6, 19, 0.18);
    border: 1px solid rgba(225, 6, 19, 0.55);
    color: #ff8b8b;
  }
}

.player-detail-hero__registration {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.player-detail-hero__identity {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.68);
  font-size: 14px;
  font-weight: 700;
}

.player-detail-hero__info-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-height: 68px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(8, 8, 8, 0.52);
}

.player-detail-hero__info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  padding: 12px 16px;
  border-right: 1px solid rgba(255, 255, 255, 0.1);

  &:last-child {
    border-right: 0;
  }
}

.player-detail-hero__info-icon {
  width: 40px;
  height: 40px;
  object-fit: contain;
  flex-shrink: 0;
  mix-blend-mode: screen;
}

.player-detail-hero__info-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}

.player-detail-hero__info-label {
  color: rgba(255, 255, 255, 0.46);
  font-size: 12px;
  font-weight: 600;
}

.player-detail-hero__info-value {
  overflow: hidden;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-detail-hero__info-secondary {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1279px) {
  .player-detail-hero__info-bar {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .player-detail-hero__main {
    flex-direction: column;
    align-items: flex-start;
  }

  .player-detail-hero__info-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-detail-hero__info-item:nth-child(2n) {
    border-right: 0;
  }
}
</style>
