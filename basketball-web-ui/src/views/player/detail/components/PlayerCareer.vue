<script setup lang="ts">
import { useRouter } from 'vue-router'
import cupIcon from '@/assets/icon/home/cup.png'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { PlayerDetailData, PlayerHonor, PlayerTeamHistory } from '../types'

defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()

function honorMeta(honor: PlayerHonor): string {
  return honor.honorValue || honor.honorDate || ''
}

function teamPath(item: PlayerTeamHistory) {
  return {
    path: `/tournament/team/${item.teamId}/overview`,
    query: item.divisionId ? { divisionId: item.divisionId } : {},
  }
}

function honorIconSrc(honor: PlayerHonor): string {
  return honor.imageUrl || cupIcon
}

function handleHonorIconError(event: Event) {
  const img = event.target as HTMLImageElement
  if (img.dataset.fallback === '1') return
  img.dataset.fallback = '1'
  img.src = cupIcon
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
        球员生涯
        <span>PLAYER CAREER</span>
      </h2>
      <p class="player-detail-career__desc">
        记录球员在 CNPA 公开赛事中的荣誉、效力队伍与选手名称变更
      </p>
    </header>

    <section class="player-detail-career__block" aria-labelledby="player-career-honors">
      <div class="player-detail-career__block-head">
        <div class="player-detail-career__block-title">
          <h3 id="player-career-honors">球员荣誉</h3>
          <span>PLAYER HONORS</span>
        </div>
        <p class="player-detail-career__block-meta">
          个人及团队荣誉 {{ detail.honors.length }}
        </p>
      </div>

      <div v-if="detail.honors.length === 0" class="player-detail-empty-inline">暂无荣誉记录</div>

      <div v-else class="player-detail-career__honor-list">
        <article
          v-for="honor in detail.honors"
          :key="honor.id"
          class="career-honor"
          :class="{ 'is-clickable': honor.tournamentId }"
          :role="honor.tournamentId ? 'button' : undefined"
          :tabindex="honor.tournamentId ? 0 : undefined"
          @click="openHonor(honor.tournamentId)"
          @keydown.enter="openHonor(honor.tournamentId)"
        >
          <div class="career-honor__icon" aria-hidden="true">
            <img :src="honorIconSrc(honor)" alt="" @error="handleHonorIconError" />
          </div>
          <div class="career-honor__body">
            <span class="career-honor__season">{{ honor.seasonName }}</span>
            <strong class="career-honor__name">{{ honor.honorName }}</strong>
            <span v-if="honorMeta(honor)" class="career-honor__meta">{{ honorMeta(honor) }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="player-detail-career__block" aria-labelledby="player-career-teams">
      <div class="player-detail-career__block-head">
        <div class="player-detail-career__block-title">
          <h3 id="player-career-teams">效力队伍</h3>
          <span>TEAM HISTORY</span>
        </div>
      </div>

      <div v-if="detail.teamHistory.length === 0" class="player-detail-empty-inline">暂无效力记录</div>

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
            <span v-if="item.current" class="career-timeline__tag">当前</span>
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
          <h3 id="player-career-names">选手名称</h3>
          <span>PLAYER ID HISTORY</span>
        </div>
      </div>

      <ol class="career-timeline">
        <li
          v-for="item in detail.nameHistory"
          :key="item.id"
          class="career-timeline__item"
        >
          <div class="career-timeline__rail" aria-hidden="true">
            <span class="career-timeline__dot" />
          </div>
          <article class="career-timeline__card" :class="{ 'is-current': item.current }">
            <span v-if="item.current" class="career-timeline__tag">当前使用</span>
            <span class="career-timeline__date">{{ item.period }}</span>
            <strong class="career-timeline__name">{{ item.playerName }}</strong>
            <span class="career-timeline__desc">{{ item.description }}</span>
          </article>
        </li>
      </ol>

      <p class="player-detail-career__note">
        仅展示经赛事审核并实际使用过的球员生涯记录
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

.career-honor {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 118px;
  padding: 18px 20px;
  border: 1px solid rgba(201, 163, 90, 0.48);
  background: linear-gradient(180deg, #1c1a16 0%, #0f0e0c 100%);
  color: #f3e3c2;
  box-sizing: border-box;

  &.is-clickable {
    cursor: pointer;

    &:hover {
      border-color: rgba(225, 6, 19, 0.55);
    }
  }
}

.career-honor__icon {
  display: grid;
  flex: 0 0 56px;
  place-items: center;
  width: 56px;
  height: 56px;

  img {
    width: 48px;
    height: 48px;
    object-fit: contain;
  }
}

.career-honor__body {
  min-width: 0;
}

.career-honor__season,
.career-honor__meta {
  display: block;
  color: rgba(243, 227, 194, 0.62);
  font-size: 12px;
  font-weight: 700;
}

.career-honor__name {
  display: block;
  margin: 6px 0;
  color: #e8c56a;
  font-size: clamp(20px, 1.8vw, 26px);
  font-weight: 900;
  line-height: 1.15;
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
  min-height: 156px;
  padding: 20px 18px 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.72);
  box-sizing: border-box;
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
}

.career-timeline__identity {
  display: flex;
  align-items: center;
  gap: 12px;
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

.career-honor:focus-visible,
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
