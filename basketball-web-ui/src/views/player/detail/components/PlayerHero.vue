<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { iconAssets } from '@/constants/iconAssets'
import TeamLogo from '@/components/team/TeamLogo.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import type { PlayerProfile } from '../types'
const teamBackground = iconAssets['tournament/team_background.png']
const teamIcon = iconAssets['team/master.png']
const platformIcon = iconAssets['person/platform.png']
const positionIcon = iconAssets['person/site.png']
const stageIcon = iconAssets['person/stage.png']

const props = defineProps<{
  profile: PlayerProfile
}>()

const { t } = useI18n()

const heroBgStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg, rgba(0,0,0,0.82) 0%, rgba(0,0,0,0.55) 48%, rgba(0,0,0,0.32) 100%), url(${teamBackground})`,
  backgroundPosition: 'center',
}))

const teamForLogo = computed(() => ({
  id: props.profile.currentTeamId,
  name: props.profile.currentTeamName,
  shortName: props.profile.currentTeamAbbr,
  logoUrl: props.profile.currentTeamLogoUrl,
  code: props.profile.currentTeamAbbr,
}))

const seasonYear = computed(() => {
  const matched = String(props.profile.seasonName || '').match(/\d{4}/)
  return matched?.[0] || ''
})

const registrationLine = computed(() => {
  if (!props.profile.verified) return 'CNPA PLAYER'
  return seasonYear.value
    ? t('publicPages.player.registeredSeason', { year: seasonYear.value })
    : t('publicPages.player.registered')
})

const identityLine = computed(() => {
  const team = props.profile.currentTeamAbbr || props.profile.currentTeamName
  const position = props.profile.position
  const parts: string[] = []
  if (team && team !== '--') parts.push(t('publicPages.player.teamIdentity', { team }))
  if (position && position !== '--') parts.push(t('publicPages.player.positionIdentity', { position }))
  return parts.join('  |  ')
})

const infoItems = computed(() => [
  {
    key: 'team',
    label: t('publicPages.player.teamLabel'),
    value: props.profile.currentTeamName,
    icon: teamIcon,
  },
  {
    key: 'platform',
    label: t('publicPages.player.platform'),
    value: props.profile.platform,
    icon: platformIcon,
  },
  {
    key: 'position',
    label: t('publicPages.player.position'),
    value: props.profile.position,
    icon: positionIcon,
  },
  {
    key: 'stage',
    label: t('publicPages.player.currentStage'),
    value: props.profile.currentStage,
    icon: stageIcon,
  },
].filter((item) => item.key !== 'stage' || Boolean(item.value && item.value !== '--')))
</script>
<template>
  <section class="player-detail-hero" :aria-label="t('publicPages.player.heroAria')">
    <div class="player-detail-hero__bg" :style="heroBgStyle" aria-hidden="true" />
    <div class="player-detail-hero__logo-bg" aria-hidden="true">
      <TeamLogo :team="teamForLogo" size="hero" />
    </div>

    <div class="player-detail-hero__inner">
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
              :title="t('publicPages.player.teamDqTitle')"
            >{{ t('publicPages.player.teamDqBadge') }}</span>
            <span
              v-else-if="profile.verified && !profile.playerPenaltyTags?.length"
              class="player-detail-hero__badge"
            >{{ t('publicPages.player.verified') }}</span>
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
          <img v-if="item.icon" :src="item.icon" alt="" class="player-detail-hero__info-icon" />
          <div class="player-detail-hero__info-copy">
            <span class="player-detail-hero__info-label">{{ item.label }}</span>
            <strong class="player-detail-hero__info-value">{{ item.value }}</strong>
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
  padding: 36px 0 0;
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
