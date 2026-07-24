<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { PlayerDetailData } from '../types'

defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()

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
    <section class="player-detail-career__section">
      <div class="player-detail-section-head">
        <div>
          <div class="player-detail-section-head__group">
            <h2 class="player-detail-section-head__title">球员荣誉</h2>
            <span class="player-detail-section-head__sub">Player Honors</span>
          </div>
          <p class="player-detail-section-head__desc">
            个人及团队荣誉 · {{ detail.honors.length }}
          </p>
        </div>
      </div>

      <div v-if="detail.honors.length === 0" class="player-detail-empty-inline">暂无荣誉记录</div>

      <div v-else class="player-detail-career__honor-list">
        <article
          v-for="honor in detail.honors"
          :key="honor.id"
          class="player-detail-career-honor"
          :class="{ 'is-clickable': honor.tournamentId }"
          role="button"
          tabindex="0"
          @click="openHonor(honor.tournamentId)"
          @keydown.enter="openHonor(honor.tournamentId)"
        >
          <div class="player-detail-career-honor__trophy" aria-hidden="true">🏆</div>
          <div>
            <span class="player-detail-career-honor__season">{{ honor.seasonName }}</span>
            <strong class="player-detail-career-honor__title">{{ honor.honorName }}</strong>
            <span class="player-detail-career-honor__date">{{ honor.honorDate }}</span>
          </div>
        </article>
      </div>
    </section>

    <section class="player-detail-career__section">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 class="player-detail-section-head__title">效力队伍</h2>
          <span class="player-detail-section-head__sub">Team History</span>
        </div>
      </div>

      <div v-if="detail.teamHistory.length === 0" class="player-detail-empty-inline">暂无效力记录</div>

      <div v-else class="player-detail-timeline player-detail-timeline--horizontal">
        <article
          v-for="item in detail.teamHistory"
          :key="item.id"
          class="player-detail-timeline__item"
        >
          <span v-if="item.current" class="player-detail-timeline__tag">当前</span>
          <span class="player-detail-timeline__date">{{ item.startDate }} - {{ item.endDate }}</span>
          <router-link :to="`/tournament/team/${item.teamId}/overview`" class="player-detail-timeline__title">
            {{ item.teamName }}
          </router-link>
          <span class="player-detail-timeline__desc">{{ item.description }}</span>
        </article>
      </div>
    </section>

    <section class="player-detail-career__section">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 class="player-detail-section-head__title">选手名称</h2>
          <span class="player-detail-section-head__sub">Player ID History</span>
        </div>
      </div>

      <div class="player-detail-timeline">
        <article
          v-for="item in detail.nameHistory"
          :key="item.id"
          class="player-detail-timeline__item player-detail-timeline__item--vertical"
        >
          <span v-if="item.current" class="player-detail-timeline__tag">当前使用</span>
          <span class="player-detail-timeline__date">{{ item.startDate }} - {{ item.endDate }}</span>
          <strong class="player-detail-timeline__title">{{ item.playerName }}</strong>
          <span class="player-detail-timeline__desc">{{ item.description }}</span>
        </article>
      </div>

      <p class="player-detail-career__note">
        仅展示经赛事审核并实际使用过的球员生涯记录
      </p>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-career__section + .player-detail-career__section {
  margin-top: 40px;
}

.player-detail-career__honor-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.player-detail-career-honor {
  display: flex;
  gap: 14px;
  padding: 18px;
  border: 1px solid rgba(201, 163, 90, 0.34);
  background: linear-gradient(180deg, rgba(17, 17, 17, 0.96), rgba(8, 8, 8, 0.98));
  color: #f1d59a;

  &.is-clickable {
    cursor: pointer;
    transition: border-color 0.18s ease;

    &:hover {
      border-color: rgba(225, 6, 19, 0.55);
    }
  }
}

.player-detail-career-honor__trophy {
  font-size: 28px;
  line-height: 1;
}

.player-detail-career-honor__season {
  display: block;
  margin-bottom: 6px;
  color: rgba(241, 213, 154, 0.72);
  font-size: 12px;
}

.player-detail-career-honor__title {
  display: block;
  margin-bottom: 6px;
  font-size: 18px;
}

.player-detail-career-honor__date {
  color: rgba(241, 213, 154, 0.72);
  font-size: 12px;
}

.player-detail-timeline {
  display: grid;
  gap: 16px;
}

.player-detail-timeline--horizontal {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.player-detail-timeline__item {
  position: relative;
  padding: 18px;
  border: 1px solid var(--cnpa-paper-border);
  background: transparent;
}

.player-detail-timeline__tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 2px 8px;
  background: var(--cnpa-red);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

.player-detail-timeline__date {
  display: block;
  margin-bottom: 8px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-timeline__title {
  display: block;
  margin-bottom: 8px;
  color: var(--cnpa-paper-text);
  font-size: 18px;
  font-weight: 800;
  text-decoration: none;

  &:hover {
    color: var(--cnpa-red);
  }
}

.player-detail-timeline__desc {
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
}

.player-detail-career__note {
  margin: 16px 0 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-empty-inline {
  padding: 24px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
}

@media (max-width: 991px) {
  .player-detail-career__honor-list,
  .player-detail-timeline--horizontal {
    grid-template-columns: 1fr;
  }
}
</style>
