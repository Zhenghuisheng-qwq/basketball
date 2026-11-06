<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import HonorPlaque from '@/components/honor/HonorPlaque.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { PlayerDetailData, PlayerHonor, PlayerTeamHistory } from '../types'

defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()
const { t } = useI18n()

function teamPath(item: PlayerTeamHistory) {
  return {
    path: `/tournament/team/${item.teamId}/overview`,
    query: item.divisionId ? { divisionId: item.divisionId } : {},
  }
}

function honorTone(honor: PlayerHonor): 'champion' | 'runner' | 'third' | 'award' {
  const code = (honor.honorCode || '').toUpperCase()
  if (code === 'CHAMPION') return 'champion'
  if (code === 'RUNNER_UP') return 'runner'
  if (code === 'THIRD_PLACE') return 'third'
  return 'award'
}

function honorMark(honor: PlayerHonor): 'trophy' | 'shield' | 'star' {
  const code = (honor.honorCode || '').toUpperCase()
  if (code === 'WEEKLY_BEST') return 'star'
  if (code === 'ALL_TOURNAMENT' || code === 'ALL_DEFENSIVE' || code === 'MVP') return 'shield'
  return 'trophy'
}

function honorDate(honor: PlayerHonor): string {
  if (honor.honorDate) return honor.honorDate
  return /^\d{4}/.test(honor.honorValue || '') ? honor.honorValue || '' : ''
}

function openHonor(tournamentId?: string) {
  if (!tournamentId) return
  router.push({
    path: '/hall-of-fame',
    query: {
      tab: 'event',
      tournamentId,
    },
  })
}
</script>

<template>
  <div class="player-detail-career">
    <header class="player-detail-career__intro">
      <h2 class="player-detail-career__title">
        {{ t('publicPages.player.careerTitle') }}
        <span>PLAYER CAREER</span>
      </h2>
      <p class="player-detail-career__desc">
        {{ t('publicPages.player.careerDesc') }}
      </p>
    </header>

    <section class="player-detail-career__block" aria-labelledby="player-career-honors">
      <div class="player-detail-career__block-head">
        <div class="player-detail-career__block-title">
          <h3 id="player-career-honors">{{ t('publicPages.player.honors') }}</h3>
          <span>PLAYER HONORS</span>
        </div>
        <p class="player-detail-career__block-meta">
          {{ t('publicPages.player.honorCount', { count: detail.honors.length }) }}
        </p>
      </div>

      <div v-if="detail.honors.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noHonors') }}</div>

      <div v-else class="player-detail-career__honor-list">
        <HonorPlaque
          v-for="honor in detail.honors"
          :key="honor.id"
          :season="honor.seasonName"
          :title="honor.honorName"
          :date="honorDate(honor)"
          :tone="honorTone(honor)"
          :mark="honorMark(honor)"
          :clickable="Boolean(honor.tournamentId)"
          @select="openHonor(honor.tournamentId)"
        />
      </div>
    </section>

    <section class="player-detail-career__block" aria-labelledby="player-career-teams">
      <div class="player-detail-career__block-head">
        <div class="player-detail-career__block-title">
          <h3 id="player-career-teams">{{ t('publicPages.player.teamsPlayed') }}</h3>
          <span>TEAM HISTORY</span>
        </div>
      </div>

      <div v-if="detail.teamHistory.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noTeamHistory') }}</div>

      <ol v-else class="career-timeline">
        <li
          v-for="item in detail.teamHistory"
          :key="item.id"
          class="career-timeline__item"
        >
          <div class="career-timeline__rail" aria-hidden="true">
            <span class="career-timeline__dot" />
          </div>
          <article class="career-timeline__card" :class="{ 'is-current': item.current }">
            <span v-if="item.current" class="career-timeline__tag">{{ t('publicPages.player.current') }}</span>
            <span class="career-timeline__date">{{ item.period }}</span>
            <router-link
              v-if="item.teamId"
              :to="teamPath(item)"
              class="career-timeline__identity"
            >
              <TeamLogo
                :team="{
                  id: item.teamId,
                  name: item.teamName,
                  shortName: item.teamName,
                  logoUrl: item.teamLogoUrl,
                }"
                size="md"
              />
              <strong>{{ item.teamName }}</strong>
            </router-link>
            <div v-else class="career-timeline__identity">
              <TeamLogo
                :team="{
                  id: item.teamId,
                  name: item.teamName,
                  shortName: item.teamName,
                  logoUrl: item.teamLogoUrl,
                }"
                size="md"
              />
              <strong>{{ item.teamName }}</strong>
            </div>
            <span class="career-timeline__desc">{{ item.description }}</span>
          </article>
        </li>
      </ol>
    </section>

    <section class="player-detail-career__block" aria-labelledby="player-career-names">
      <div class="player-detail-career__block-head">
        <div class="player-detail-career__block-title">
          <h3 id="player-career-names">{{ t('publicPages.player.playerNames') }}</h3>
          <span>PLAYER ID HISTORY</span>
        </div>
      </div>

      <div v-if="detail.nameHistory.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noNameHistory') }}</div>

      <ol v-else class="career-timeline">
        <li
          v-for="item in detail.nameHistory"
          :key="item.id"
          class="career-timeline__item"
        >
          <div class="career-timeline__rail" aria-hidden="true">
            <span class="career-timeline__dot" />
          </div>
          <article class="career-timeline__card" :class="{ 'is-current': item.current }">
            <span v-if="item.current" class="career-timeline__tag">{{ t('publicPages.player.nameInUse') }}</span>
            <span class="career-timeline__date">{{ item.period }}</span>
            <strong class="career-timeline__name">{{ item.playerName }}</strong>
            <span class="career-timeline__desc">{{ item.description }}</span>
          </article>
        </li>
      </ol>

      <p class="player-detail-career__note">
        {{ t('publicPages.player.careerNote') }}
      </p>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-career__intro {
  margin-bottom: 28px;
}

.player-detail-career__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
  font-size: clamp(28px, 2.6vw, 36px);
  font-weight: 900;
  letter-spacing: 0.02em;
  color: var(--cnpa-paper-text);

  span {
    color: var(--cnpa-red);
    font-size: 0.42em;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.14em;
  }
}

.player-detail-career__desc {
  margin: 8px 0 0;
  max-width: 640px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.player-detail-career__block + .player-detail-career__block {
  margin-top: 40px;
}

.player-detail-career__block-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.player-detail-career__block-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;

  &::before {
    content: '';
    width: 4px;
    height: 22px;
    background: var(--cnpa-red);
    flex-shrink: 0;
  }

  h3 {
    margin: 0;
    font-size: 22px;
    font-weight: 900;
    line-height: 1;
    color: var(--cnpa-paper-text);
  }

  span {
    color: var(--cnpa-red);
    font-size: 12px;
    font-weight: 800;
    font-style: italic;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    line-height: 1;
  }
}

.player-detail-career__block-meta {
  margin: 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.player-detail-career__honor-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.career-timeline {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.career-timeline__item {
  position: relative;
  min-width: 0;
  padding-top: 28px;
}

.career-timeline__rail {
  position: absolute;
  top: 8px;
  right: 0;
  left: 0;
  height: 2px;
  background: rgba(17, 17, 17, 0.12);
}

.career-timeline__dot {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 12px;
  height: 12px;
  border: 2px solid #171717;
  border-radius: 50%;
  background: #fff;
  transform: translate(-50%, -50%);
}

.career-timeline__item:first-child .career-timeline__rail {
  left: 50%;
}

.career-timeline__item:last-child .career-timeline__rail {
  right: 50%;
}

.career-timeline__item:only-child .career-timeline__rail {
  background: transparent;
}

.career-timeline__card {
  position: relative;
  min-height: 168px;
  padding: 20px 18px 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.86);
  box-sizing: border-box;
  text-align: center;
}

.career-timeline__tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 8px;
  background: var(--cnpa-red);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
}

.career-timeline__date {
  display: block;
  margin-bottom: 14px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.career-timeline__identity {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  color: inherit;
  text-decoration: none;

  strong {
    min-width: 0;
    color: var(--cnpa-paper-text);
    font-size: 20px;
    font-weight: 900;
    line-height: 1.2;
    word-break: break-word;
  }

  &:hover strong {
    color: var(--cnpa-red);
  }
}

.career-timeline__name {
  display: block;
  margin: 8px 0 12px;
  text-align: center;
  color: var(--cnpa-paper-text);
  font-size: 22px;
  font-weight: 900;
  line-height: 1.2;
  word-break: break-word;
}

.career-timeline__desc {
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.player-detail-career__note {
  margin: 16px 0 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-empty-inline {
  padding: 28px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

.career-timeline__identity:focus-visible {
  outline: 2px solid rgba(225, 6, 19, 0.4);
  outline-offset: 2px;
}

@media (max-width: 991px) {
  .player-detail-career__honor-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .player-detail-career__block-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .career-timeline {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .career-timeline__item {
    padding-top: 0;
    padding-left: 28px;
  }

  .career-timeline__rail {
    top: 0;
    right: auto;
    bottom: -16px;
    left: 5px;
    width: 2px;
    height: auto;
  }

  .career-timeline__dot {
    top: 28px;
    left: 0;
  }

  .career-timeline__item:first-child .career-timeline__rail,
  .career-timeline__item:last-child .career-timeline__rail {
    left: 5px;
    right: auto;
  }

  .career-timeline__item:first-child .career-timeline__rail {
    top: 28px;
  }

  .career-timeline__item:last-child .career-timeline__rail {
    bottom: auto;
    height: 28px;
  }

  .career-timeline__item:only-child .career-timeline__rail {
    display: none;
  }
}
</style>
