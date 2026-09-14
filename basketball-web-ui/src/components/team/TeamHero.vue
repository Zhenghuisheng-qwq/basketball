<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import teamBackground from '@/assets/icon/tournament/team_background.png'
import captainIcon from '@/assets/icon/team/master.png'
import platformIcon from '@/assets/icon/team/platform.png'
import stageIcon from '@/assets/icon/team/stage.png'
import rosterIcon from '@/assets/icon/team/lineup.png'
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

const route = useRoute()
const router = useRouter()
const compareModalVisible = ref(false)
const teamPageCtx = inject(PUBLIC_TEAM_PAGE_KEY, null)

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

const stageLabel = computed(() => stageLabels[props.team.currentStage])

const recordText = computed(() => `${props.team.wins}胜${props.team.losses}负`)

const dqSummary = computed(() => (props.team.activeDisciplineSummary || []).find((item) => item.penaltyCode === 'EVENT_DISQUALIFICATION')
  || (props.team.disqualified ? {
    penaltyLabel: '取消赛事资格',
    statusLabel: '生效中',
    effectiveStartTime: '',
    description: '该球队在本届赛事中的参赛资格已取消。',
  } : null))

const dqHint = computed(() => {
  if (!dqSummary.value) return ''
  const time = dqSummary.value.effectiveStartTime ? dqSummary.value.effectiveStartTime.slice(0, 10) : ''
  return [dqSummary.value.penaltyLabel, dqSummary.value.statusLabel, time].filter(Boolean).join(' · ')
})

const heroDescription = computed(() => {
  if (props.team.disqualified) {
    return dqSummary.value?.description || '该球队在本届赛事中的参赛资格已取消。'
  }
  return props.team.description
})

const compareDivisionId = computed(() =>
  String(
    teamPageCtx?.divisionId.value
    || teamPageCtx?.detail.value?.divisionId
    || route.query.divisionId
    || '',
  ).trim(),
)

const compareContextText = computed(() => {
  const season = props.team.seasonName?.trim() || ''
  const platform = props.team.platform?.trim() || ''
  const division = props.team.divisionName?.trim() || ''
  let eventPart = ''
  if (platform && division) {
    eventPart = division.includes(platform) ? division : `${platform} / ${division}`
  } else {
    eventPart = division || platform
  }
  if (season && eventPart) return `${season} · ${eventPart}`
  return season || eventPart
})

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
      ...(compareDivisionId.value ? { divisionId: compareDivisionId.value } : {}),
    },
  })
}

const heroBgStyle = computed(() => ({
  backgroundImage: `linear-gradient(90deg, rgba(0, 0, 0, 0.72) 0%, rgba(0, 0, 0, 0.38) 45%, rgba(0, 0, 0, 0.15) 100%), url(${teamBackground})`,
}))

interface HeroInfoCard {
  key: string
  icon: string
  label: string
  value: string
}

const infoCards = computed<HeroInfoCard[]>(() => [
  {
    key: 'captain',
    icon: captainIcon,
    label: '队长',
    value: props.team.captainName,
  },
  {
    key: 'platform',
    icon: platformIcon,
    label: '比赛平台',
    value: props.team.platform?.trim() || '--',
  },
  {
    key: 'stage',
    icon: stageIcon,
    label: '当前阶段',
    value: stageLabel.value,
  },
  {
    key: 'roster',
    icon: rosterIcon,
    label: '注册阵容',
    value: `${props.team.rosterCount} 人`,
  },
])
</script>
<template>
  <section class="team-hero" aria-label="球队详情头部">
    <div class="team-hero__bg" :style="heroBgStyle" aria-hidden="true" />
    <div class="team-hero__watermark" aria-hidden="true">
      <TeamLogo :team="team" size="hero" />
    </div>

    <div class="team-hero__inner">
      <div class="team-hero__main">
        <div class="team-hero__logo-wrap">
          <TeamLogo :team="team" size="hero" />
        </div>

        <div class="team-hero__summary">
          <div class="team-hero__title-row">
            <h1 class="team-hero__name">{{ team.name }}</h1>
            <span
              v-if="team.disqualified"
              class="team-hero__badge team-hero__badge--dq"
              :title="dqHint"
            >赛事资格已取消</span>
            <span v-else-if="team.verified" class="team-hero__badge">已认证</span>
          </div>
          <p v-if="team.disqualified && dqHint" class="team-hero__dq-hint">{{ dqHint }}</p>

          <p class="team-hero__desc">{{ heroDescription }}</p>

          <div class="team-hero__stats">
            <span class="team-hero__stat">{{ recordText }}</span>
            <span class="team-hero__stat-divider">|</span>
            <span class="team-hero__stat">排名第{{ team.ranking }}</span>
            <button type="button" class="team-hero__compare-btn" @click="openCompareModal">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
                <path d="M7 7h11l-3-3M17 17H6l3 3" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
              球队对比
            </button>
          </div>
        </div>
      </div>

      <div class="team-hero__info-panel" aria-label="球队基本信息">
        <div
          v-for="card in infoCards"
          :key="card.key"
          class="team-hero__info-card"
          :data-info="card.key"
        >
          <div class="team-hero__info-icon" aria-hidden="true">
            <img :src="card.icon" alt="" />
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
    :division-id="compareDivisionId"
    :current-team-id="teamId"
    :disabled-team-ids="[teamId]"
    :competition-text="compareContextText"
    @close="compareModalVisible = false"
    @select="handleSelectCompareTeam"
  />
</template>
<style scoped lang="scss">
.team-hero {
  --team-hero-content-offset: clamp(36px, 4vw, 86px);
  --team-hero-gold: #d6ac58;

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

.team-hero__watermark {
  position: absolute;
  right: clamp(-24px, 2vw, 40px);
  top: 46%;
  z-index: 0;
  width: clamp(280px, 34vw, 460px);
  height: clamp(280px, 34vw, 460px);
  opacity: 0.12;
  filter: grayscale(0.15);
  pointer-events: none;
  transform: translateY(-50%);

  :deep(.team-logo) {
    width: 100%;
    height: 100%;
  }

  :deep(.team-logo--hero) {
    width: 100%;
    height: 100%;
  }

  :deep(.badge) {
    transform: scale(5.4);
  }
}

.team-hero__inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 50px 0 0;
  box-sizing: border-box;
}

.team-hero__main {
  display: flex;
  align-items: center;
  gap: clamp(28px, 3.6vw, 56px);
  margin: 8px 0 36px;
  padding-left: var(--team-hero-content-offset);
  padding-right: var(--team-hero-content-offset);
  position: relative;
  z-index: 2;
}

.team-hero__logo-wrap {
  flex: none;
  display: grid;
  place-items: center;

  :deep(.team-logo--hero) {
    width: clamp(132px, 12vw, 176px);
    height: clamp(132px, 12vw, 176px);

    .team-logo__fallback {
      font-size: clamp(48px, 4vw, 72px);
      letter-spacing: 0.04em;
      border-radius: 4px;
    }

    .badge {
      transform: scale(2.2);
    }
  }
}

.team-hero__summary {
  flex: 1;
  min-width: 0;
  max-width: 720px;
}

.team-hero__title-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}

.team-hero__name {
  margin: 0;
  font-size: clamp(44px, 4.2vw, 64px);
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.02;
  color: #fff;
}

.team-hero__badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 9px;
  border: 1px solid rgba(225, 6, 19, 0.7);
  border-radius: 999px;
  background: #e10613;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
  box-sizing: border-box;

  &--dq {
    border-radius: 2px;
    border-color: rgba(225, 6, 19, 0.55);
    background: rgba(225, 6, 19, 0.14);
    color: #ff8b8b;
  }
}

.team-hero__dq-hint {
  margin: 0 0 10px;
  color: rgba(255, 139, 139, 0.78);
  font-size: 12px;
  font-weight: 600;
}

.team-hero__desc {
  margin: 0 0 18px;
  max-width: 52ch;
  font-size: 16px;
  line-height: 1.5;
  color: rgba(255, 255, 255, 0.72);
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.team-hero__stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 16px;
  margin: 0;
  font-size: 16px;
  font-weight: 800;
  color: #fff;
}

.team-hero__stat-divider {
  color: rgba(255, 255, 255, 0.32);
}

.team-hero__compare-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 36px;
  padding: 0 14px;
  margin-left: 4px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.02em;
  cursor: pointer;
  font-family: inherit;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;

  svg {
    width: 16px;
    height: 16px;
  }

  &:hover {
    border-color: rgba(225, 6, 19, 0.7);
    background: rgba(225, 6, 19, 0.16);
  }

  &:focus-visible {
    outline: 2px solid #fff;
    outline-offset: 3px;
  }
}

.team-hero__info-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  align-items: stretch;
  width: 100%;
  min-height: 82px;
  margin-top: 8px;
  border-top: 1px solid rgba(214, 172, 88, 0.22);
  background: rgba(8, 8, 8, 0.55);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-sizing: border-box;
}

.team-hero__info-card {
  position: relative;
  display: flex;
  align-items: center;
  column-gap: 12px;
  min-width: 0;
  padding: 16px clamp(20px, 2.4vw, 34px);
  box-sizing: border-box;

  &:not(:first-child)::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    width: 1px;
    height: 36px;
    background: rgba(214, 172, 88, 0.4);
    transform: translateY(-50%);
  }
}

.team-hero__info-icon {
  flex: none;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;

  img {
    display: block;
    width: 40px;
    height: 40px;
    object-fit: contain;
    mix-blend-mode: screen;
    transform-origin: center center;
  }
}

.team-hero__info-card[data-info='captain'] img {
  transform: scale(0.98);
}

.team-hero__info-card[data-info='platform'] img {
  transform: scale(1.12);
}

.team-hero__info-card[data-info='stage'] img {
  transform: scale(0.84);
}

.team-hero__info-card[data-info='roster'] img {
  transform: scale(0.9);
}

.team-hero__info-content {
  flex: 1;
  min-width: 0;
}

.team-hero__info-label {
  overflow: hidden;
  margin: 0;
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-hero__info-value {
  overflow: hidden;
  margin-top: 5px;
  color: #fff;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 1280px) {
  .team-hero {
    --team-hero-content-offset: 36px;
  }
}

@media (max-width: 992px) {
  .team-hero {
    --team-hero-content-offset: 20px;
  }

  .team-hero__watermark {
    opacity: 0.08;
    width: 240px;
    height: 240px;
  }

  .team-hero__main {
    flex-direction: column;
    align-items: flex-start;
    margin-bottom: 28px;
  }

  .team-hero__info-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .team-hero__info-card {
    &:nth-child(odd)::before {
      display: none;
    }

    &:nth-child(n + 3) {
      border-top: 1px solid rgba(214, 172, 88, 0.22);
    }
  }
}

@media (max-width: 768px) {
  .team-hero {
    --team-hero-content-offset: 16px;
  }

  .team-hero__name {
    font-size: clamp(36px, 10vw, 48px);
  }

  .team-hero__watermark {
    display: none;
  }
}

@media (max-width: 420px) {
  .team-hero__info-panel {
    grid-template-columns: 1fr;
  }

  .team-hero__info-card {
    &::before {
      display: none;
    }

    border-top: 1px solid rgba(214, 172, 88, 0.22);

    &:first-child {
      border-top: none;
    }
  }

  .team-hero__info-value {
    white-space: normal;
  }
}
</style>