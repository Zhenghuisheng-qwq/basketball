<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import type { HonorPlayer } from '../types'

const props = defineProps<{
  player: HonorPlayer
}>()

const router = useRouter()

const team = computed(() => ({
  id: props.player.teamId,
  name: props.player.teamName,
  shortName: props.player.teamAbbr,
  logoUrl: props.player.teamLogoUrl,
}))

const teamLabel = computed(() => props.player.teamAbbr || props.player.teamName)

const cardLabel = computed(
  () => `${props.player.playerName}，位置 ${props.player.position}，球队 ${teamLabel.value}`,
)

function openPlayer() {
  goToPlayerDetail(router, props.player.playerId, 'overview')
}

function openTeam(event: Event) {
  event.stopPropagation()
  router.push(`/tournament/team/${props.player.teamId}/overview`)
}
</script>

<template>
  <article
    class="hall-lineup-card"
    role="button"
    tabindex="0"
    :title="player.position"
    :aria-label="cardLabel"
    @click="openPlayer"
    @keydown.enter.prevent="openPlayer"
  >
    <div class="hall-lineup-card__visual">
      <HallPlayerSilhouette
        :name="player.playerName"
        :avatar-url="player.playerAvatarUrl"
        size="lineup"
      />
    </div>

    <div class="hall-lineup-card__content">
      <div class="hall-lineup-card__name">{{ player.playerName }}</div>
      <button
        type="button"
        class="hall-lineup-card__team"
        :aria-label="`查看球队 ${teamLabel}`"
        @click="openTeam"
        @keydown.enter.stop="openTeam"
      >
        <span class="hall-lineup-card__logo">
          <TeamLogo :team="team" size="sm" />
        </span>
        <span class="hall-lineup-card__team-name">{{ teamLabel }}</span>
      </button>
    </div>
  </article>
</template>

<style scoped lang="scss">
.hall-lineup-card {
  position: relative;
  width: 100%;
  aspect-ratio: 0.68;
  min-height: 310px;
  overflow: hidden;
  border-radius: 7px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #080808;
  cursor: pointer;
  transition: border-color 180ms ease, box-shadow 180ms ease;

  &::after {
    content: '';
    position: absolute;
    inset: 38% 0 0;
    z-index: 1;
    pointer-events: none;
    background: linear-gradient(
      180deg,
      transparent 0%,
      rgba(5, 5, 5, 0.3) 26%,
      rgba(5, 5, 5, 0.92) 73%,
      #050505 100%
    );
  }

  &:hover {
    border-color: rgba(226, 22, 31, 0.75);
    box-shadow: 0 0 18px rgba(226, 22, 31, 0.16);
  }

  &:focus-visible {
    outline: 2px solid rgba(231, 25, 34, 0.8);
    outline-offset: 2px;
  }
}

.hall-lineup-card__visual {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 30%, rgba(255, 255, 255, 0.13), transparent 27%),
    radial-gradient(circle at 50% 45%, rgba(160, 0, 0, 0.18), transparent 55%),
    #090909;

  :deep(.hall-player-visual) {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
  }
}

.hall-lineup-card__content {
  position: absolute;
  z-index: 2;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 0 16px 18px;
  text-align: center;
}

.hall-lineup-card__name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #fff;
  font-size: clamp(21px, 1.7vw, 29px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.hall-lineup-card__team {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  margin: 12px 0 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #f3f3f3;
  font: inherit;
  cursor: pointer;

  &:hover {
    color: var(--hall-red, #e71922);
  }
}

.hall-lineup-card__logo {
  display: grid;
  place-items: center;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;

  :deep(.team-logo),
  :deep(.team-logo--sm) {
    width: 28px;
    height: 28px;
  }
}

.hall-lineup-card__team-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: clamp(14px, 1.1vw, 16px);
  font-weight: 700;
}

@media (max-width: 767px) {
  .hall-lineup-card {
    min-height: 280px;
  }

  .hall-lineup-card__name {
    font-size: 18px;
  }

  .hall-lineup-card__logo {
    flex-basis: 22px;
    width: 22px;
    height: 22px;

    :deep(.team-logo),
    :deep(.team-logo--sm) {
      width: 22px;
      height: 22px;
    }
  }

  .hall-lineup-card__team-name {
    font-size: 14px;
  }

  .hall-lineup-card__content {
    padding: 0 12px 16px;
  }
}

@media (max-width: 479px) {
  .hall-lineup-card {
    min-height: 300px;
  }
}
</style>
