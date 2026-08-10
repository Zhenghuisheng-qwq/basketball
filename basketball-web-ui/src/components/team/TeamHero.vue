<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useRouter } from 'vue-router'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import TeamLogo from '@/components/team/TeamLogo.vue'
import TeamSelectModal from '@/components/team/TeamSelectModal.vue'
import type { TeamBase, TeamStage } from '@/types/team'
import {
  PUBLIC_TEAM_PAGE_KEY,
} from '@/views/tournament/team/composables/usePublicTeamPage'

const props = defineProps<{
  team: TeamBase
  teamId: string
}>()

const router = useRouter()
const compareModalVisible = ref(false)
const teamPageCtx = inject(PUBLIC_TEAM_PAGE_KEY, null)

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

const stageLabel = computed(() => stageLabels[props.team.currentStage])

const recordText = computed(() => `${props.team.wins}胜${props.team.losses}负`)

const compareCompetitionText = computed(
  () => `${props.team.seasonName} · ${stageLabels[props.team.currentStage]}`,
)

function openCompareModal() {
  compareModalVisible.value = true
}

function handleSelectCompareTeam(targetTeamId: string) {
  if (targetTeamId === props.teamId) return
  compareModalVisible.value = false
  router.push({
    path: '/tournament/team/compare',
    query: {
      left: props.teamId,
      right: targetTeamId,
      ...(teamPageCtx?.seasonId.value ? { seasonId: teamPageCtx.seasonId.value } : {}),
      ...(teamPageCtx?.divisionId.value ? { divisionId: teamPageCtx.divisionId.value } : {}),
    },
  })
}

const heroBgStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg, rgba(0, 0, 0, 0.72) 0%, rgba(0, 0, 0, 0.38) 45%, rgba(0, 0, 0, 0.15) 100%), url(${teamBackground})`,
}))

type HeroInfoIcon = 'captain' | 'gamepad' | 'stage' | 'roster'

interface HeroInfoCard {
  key: string
  icon: HeroInfoIcon
  label: string
  value: string
}

const infoCards = computed<HeroInfoCard[]>(() => [
  {
    key: 'captain',
    icon: 'captain' as const,
    label: '队长',
    value: props.team.captainName,
  },
  {
    key: 'platform',
    icon: 'gamepad' as const,
    label: '比赛平台',
    value: props.team.platform?.trim() || '--',
  },
  {
    key: 'stage',
    icon: 'stage' as const,
    label: '当前阶段',
    value: stageLabel.value,
  },
  {
    key: 'roster',
    icon: 'roster' as const,
    label: '注册阵容',
    value: `${props.team.rosterCount} 人`,
  },
])
</script>
<template>
  <section class="team-hero" aria-label="球队详情头部">
    <div class="team-hero__bg" :style="heroBgStyle" aria-hidden="true" />

    <div class="team-hero__inner">
      <nav class="team-hero__breadcrumb" aria-label="面包屑">
        <router-link to="/tournament" class="team-hero__crumb">赛事</router-link>
        <span class="team-hero__crumb-sep">/</span>
        <router-link to="/tournament" class="team-hero__crumb">CNPA League</router-link>
        <span class="team-hero__crumb-sep">/</span>
        <span class="team-hero__crumb team-hero__crumb--current">{{ team.name }}</span>
      </nav>

      <div class="team-hero__main">
        <div class="team-hero__logo-wrap">
          <TeamLogo :team="team" size="hero" />
        </div>

        <div class="team-hero__summary">
          <div class="team-hero__title-row">
            <h1 class="team-hero__name">{{ team.name }}</h1>
            <span v-if="team.verified" class="team-hero__badge">已认证</span>
          </div>

          <p class="team-hero__desc">{{ team.description }}</p>

          <div class="team-hero__stats">
            <span class="team-hero__stat">{{ stageLabel }} {{ recordText }}</span>
            <span class="team-hero__stat-divider">|</span>
            <span class="team-hero__stat">排名第{{ team.ranking }}</span>
          </div>

          <button type="button" class="team-hero__compare-btn" @click="openCompareModal">
            球队对比
          </button>
        </div>
      </div>

      <div class="team-hero__info-panel" aria-label="球队基本信息">
        <div
          v-for="card in infoCards"
          :key="card.key"
          class="team-hero__info-card"
        >
          <div class="team-hero__info-icon" aria-hidden="true">
            <svg
              v-if="card.icon === 'captain'"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.8"
            >
              <circle cx="12" cy="8" r="3.5" />
              <path d="M6 20v-1.5a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4V20" stroke-linecap="round" />
            </svg>
            <svg
              v-else-if="card.icon === 'gamepad'"
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
            <svg
              v-else-if="card.icon === 'stage'"
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
              <circle cx="9" cy="9" r="3" />
              <circle cx="16" cy="10" r="2.5" />
              <path d="M4 19v-1a4 4 0 0 1 4-4h1M14 14a3.5 3.5 0 0 1 3.5 3.5V19" stroke-linecap="round" />
            </svg>
          </div>

          <div class="team-hero__info-content">
            <div class="team-hero__info-label">{{ card.label }}</div>
            <div class="team-hero__info-value">{{ card.value }}</div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <TeamSelectModal
    :visible="compareModalVisible"
    title="选择球队"
    subtitle="点击球队即可完成更换"
    :competition-text="compareCompetitionText"
    :disabled-team-ids="[teamId]"
    :selected-team-ids="[teamId]"
    side-text="正在选择对比球队"
    @close="compareModalVisible = false"
    @select="handleSelectCompareTeam"
  />
</template>
<style scoped lang="scss">
.team-hero {
  --team-hero-content-offset: clamp(36px, 4vw, 86px);
  --team-hero-main-offset: clamp(56px, 5vw, 108px);

  position: relative;
  overflow: hidden;
  width: 100%;
  background: #050505;
  color: #fff;
}

.team-hero__bg {
  position: absolute;
  inset: 0;
  background-color: #050505;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  pointer-events: none;
}

.team-hero__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  padding: 24px 0 0;
  box-sizing: border-box;
}

.team-hero__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0 0 24px;
  padding-left: var(--team-hero-content-offset);
  font-size: 14px;
  color: rgba(255, 255, 255, 0.52);
}

.team-hero__crumb {
  color: inherit;
  transition: color 0.2s ease;

  &:hover {
    color: rgba(255, 255, 255, 0.88);
  }

  &--current {
    color: #fff;
    font-weight: 600;
  }
}

.team-hero__crumb-sep {
  opacity: 0.38;
}

.team-hero__main {
  display: flex;
  align-items: center;
  gap: clamp(42px, 4vw, 64px);
  margin-top: 40px;
  margin-left: var(--team-hero-main-offset);
  position: relative;
  z-index: 2;
}

.team-hero__logo-wrap {
  flex-shrink: 0;

  :deep(.team-logo--hero) {
    width: clamp(148px, 10vw, 180px);
    height: clamp(148px, 10vw, 180px);

    .team-logo__fallback {
      font-size: clamp(58px, 4.8vw, 82px);
      letter-spacing: 0.06em;
      border-radius: 4px;
    }

    .badge {
      transform: scale(2.35);
    }
  }
}

.team-hero__summary {
  flex: 1;
  min-width: 0;
}

.team-hero__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.team-hero__name {
  margin: 0;
  font-size: clamp(48px, 4.4vw, 68px);
  font-weight: 900;
  letter-spacing: 1px;
  line-height: 1;
  color: #fff;
}

.team-hero__badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(24, 180, 107, 0.45);
  border-radius: 2px;
  background: rgba(24, 180, 107, 0.1);
  color: #6ee7a8;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}

.team-hero__desc {
  margin: 0 0 20px;
  font-size: 17px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.78);
}

.team-hero__stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: clamp(17px, 1.1vw, 18px);
  font-weight: 800;
  color: #fff;
}

.team-hero__stat-divider {
  color: rgba(255, 255, 255, 0.35);
}

.team-hero__compare-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 138px;
  height: 40px;
  margin-top: 18px;
  border: 1px solid rgba(220, 55, 45, 0.55);
  border-radius: 2px;
  background: linear-gradient(180deg, rgba(112, 18, 18, 0.92) 0%, rgba(65, 8, 8, 0.92) 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.04em;
  cursor: pointer;
  font-family: inherit;
  transition: filter 0.2s ease, transform 0.2s ease, border-color 0.2s ease;

  &:hover {
    filter: brightness(1.12);
    transform: translateY(-1px);
    border-color: rgba(255, 80, 65, 0.72);
  }
}

.team-hero__info-panel {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  align-items: stretch;
  width: 100%;
  max-width: 100%;
  height: 86px;
  margin: 44px 0 30px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 2px;
  background: rgba(10, 8, 8, 0.66);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  box-sizing: border-box;
  overflow: hidden;
}

.team-hero__info-card {
  display: flex;
  align-items: center;
  column-gap: 14px;
  min-width: 0;
  height: 100%;
  padding: 0 clamp(24px, 2.4vw, 34px);
  background: transparent;
  box-sizing: border-box;

  &:not(:first-child) {
    border-left: 1px solid rgba(255, 255, 255, 0.1);
  }
}

.team-hero__info-icon {
  flex: none;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 3px;
  border: 1px solid rgba(214, 172, 88, 0.45);
  border-radius: 2px;
  box-sizing: border-box;
  color: #d6ac58;
  opacity: 0.95;

  svg {
    display: block;
    width: 100%;
    height: 100%;
  }
}

.team-hero__info-content {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.team-hero__info-label {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-hero__info-value {
  overflow: hidden;
  margin-top: 4px;
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .team-hero {
    --team-hero-main-offset: 36px;
  }

  .team-hero__logo-wrap :deep(.team-logo--hero) {
    width: 160px;
    height: 160px;

    .team-logo__fallback {
      font-size: clamp(52px, 4.2vw, 68px);
    }

    .badge {
      transform: scale(2.1);
    }
  }

  .team-hero__main {
    gap: clamp(36px, 3.5vw, 52px);
    margin-top: 36px;
  }
}

@media (max-width: 992px) {
  .team-hero {
    --team-hero-main-offset: 20px;
  }

  .team-hero__main {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 991px) {
  .team-hero {
    --team-hero-content-offset: 20px;
  }

  .team-hero__logo-wrap :deep(.team-logo--hero) {
    width: 148px;
    height: 148px;

    .team-logo__fallback {
      font-size: 50px;
    }

    .badge {
      transform: scale(1.95);
    }
  }

  .team-hero__info-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    height: auto;
  }

  .team-hero__info-card {
    min-height: 80px;
    padding-block: 14px;

    &:nth-child(odd) {
      border-left: none;
    }

    &:nth-child(even) {
      border-left: 1px solid rgba(255, 255, 255, 0.1);
    }

    &:nth-child(n + 3) {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }
  }
}

@media (max-width: 768px) {
  .team-hero {
    --team-hero-content-offset: 16px;
  }

  .team-hero__main {
    gap: 24px;
    margin-top: 32px;
  }

  .team-hero__name {
    font-size: clamp(40px, 10vw, 52px);
  }

  .team-hero__info-panel {
    margin-top: 36px;
    margin-bottom: 24px;
  }
}

@media (max-width: 420px) {
  .team-hero__info-panel {
    grid-template-columns: 1fr;
  }

  .team-hero__info-card {
    border-left: none !important;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    &:first-child {
      border-top: none;
    }
  }
}
</style>