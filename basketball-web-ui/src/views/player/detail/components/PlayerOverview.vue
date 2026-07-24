<script setup lang="ts">
import { useRouter } from 'vue-router'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import PlayerMatchTable from './PlayerMatchTable.vue'
import type { PlayerDetailData } from '../types'

defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()

function goAllMatches() {
  router.replace({ query: { ...router.currentRoute.value.query, tab: 'matches' } })
}
</script>

<template>
  <div class="player-detail-overview">
    <section class="player-detail-overview__section">
      <div class="player-detail-section-head">
        <div>
          <div class="player-detail-section-head__group">
            <h2 class="player-detail-section-head__title">球员荣誉</h2>
            <span class="player-detail-section-head__sub">Player Honors</span>
          </div>
          <p class="player-detail-section-head__desc">
            记录球员参加 CNPA 赛事以来获得的个人与团队荣誉
          </p>
        </div>
      </div>

      <div class="player-detail-overview__honor-grid">
        <article class="player-detail-honor-card">
          <span class="player-detail-honor-card__tag">CNPA</span>
          <PlayerAvatar
            :name="detail.profile.displayName"
            :avatar-url="detail.profile.avatarUrl"
            size="card"
            tone="gold"
          />
          <strong class="player-detail-honor-card__name">{{ detail.profile.displayName }}</strong>
          <span class="player-detail-honor-card__position">{{ detail.profile.position }}</span>
          <span class="player-detail-honor-card__team">{{ detail.profile.currentTeamAbbr }}</span>
          <span class="player-detail-honor-card__registered">CNPA REGISTERED PLAYER</span>
        </article>

        <div
          v-if="detail.honors.length === 0"
          class="player-detail-overview__stats player-detail-overview__stats--empty"
        >
          <p>暂无荣誉记录</p>
          <span>球员荣誉发布后将在此展示</span>
        </div>
        <div v-else class="player-detail-overview__stats">
          <article
            v-for="honor in detail.honors.slice(0, 4)"
            :key="honor.id"
            class="player-detail-overview__honor-item"
          >
            <strong>{{ honor.honorName }}</strong>
            <span>{{ honor.seasonName }}</span>
            <span>{{ honor.honorDate }}</span>
          </article>
        </div>
      </div>
    </section>

    <section class="player-detail-overview__section">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 class="player-detail-section-head__title">最近比赛</h2>
          <span class="player-detail-section-head__sub">Recent Matches</span>
        </div>
        <button type="button" class="player-detail-section-head__action" @click="goAllMatches">
          查看全部比赛 →
        </button>
      </div>

      <PlayerMatchTable :rows="detail.recentMatches" compact />
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-overview__section + .player-detail-overview__section {
  margin-top: 36px;
}

.player-detail-overview__honor-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 24px;
}

.player-detail-overview__stats--empty {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  min-height: 180px;
  padding: 24px;
  border: 1px dashed rgba(17, 17, 17, 0.16);
  background: rgba(255, 255, 255, 0.45);
  color: rgba(17, 17, 17, 0.55);

  p {
    margin: 0 0 8px;
    font-size: 16px;
    font-weight: 800;
    color: rgba(17, 17, 17, 0.72);
  }

  span {
    font-size: 13px;
  }
}

.player-detail-honor-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 18px 20px;
  border: 1px solid rgba(201, 163, 90, 0.42);
  background:
    linear-gradient(180deg, rgba(17, 17, 17, 0.96) 0%, rgba(8, 8, 8, 0.98) 100%);
  color: #f1d59a;
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 12px), 96% 100%, 4% 100%, 0 calc(100% - 12px));
}

.player-detail-honor-card__tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 8px;
  background: var(--cnpa-red);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

.player-detail-honor-card__name {
  font-size: 18px;
}

.player-detail-honor-card__position,
.player-detail-honor-card__team {
  font-size: 12px;
  color: rgba(241, 213, 154, 0.72);
}

.player-detail-honor-card__registered {
  margin-top: 8px;
  font-size: 10px;
  letter-spacing: 0.12em;
}

.player-detail-overview__stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.player-detail-overview__honor-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 14px 16px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.45);

  strong {
    color: #111;
    font-size: 15px;
    font-weight: 800;
  }

  span {
    color: rgba(17, 17, 17, 0.55);
    font-size: 12px;
    font-weight: 600;
  }
}

.player-detail-stat-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.player-detail-stat-card,
.player-detail-medal {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  padding: 16px;
  border: 1px solid var(--cnpa-paper-border);
  background: transparent;
}

.player-detail-stat-card__icon {
  color: var(--cnpa-red);
  font-size: 18px;
  line-height: 1;
}

.player-detail-stat-card strong,
.player-detail-medal strong {
  font-size: 24px;
  line-height: 1;
}

.player-detail-stat-card span:last-child,
.player-detail-medal span:last-child {
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
}

.player-detail-medals {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.player-detail-medal__icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #111;
  font-size: 12px;
  font-weight: 900;
}

.player-detail-medal__icon--gold {
  background: linear-gradient(145deg, #f1d59a, #c9a35a);
}

.player-detail-medal__icon--silver {
  background: linear-gradient(145deg, #ececec, #a8a8a8);
}

.player-detail-medal__icon--bronze {
  background: linear-gradient(145deg, #d7a06a, #8b5a2b);
}

@media (max-width: 991px) {
  .player-detail-overview__honor-grid {
    grid-template-columns: 1fr;
  }

  .player-detail-stat-cards,
  .player-detail-medals {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .player-detail-stat-cards,
  .player-detail-medals {
    grid-template-columns: 1fr;
  }
}
</style>
