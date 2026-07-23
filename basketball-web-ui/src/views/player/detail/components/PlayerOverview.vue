<script setup lang="ts">
import { useRouter } from 'vue-router'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import PlayerMatchTable from './PlayerMatchTable.vue'
import type { PlayerDetailData } from '../mock/playerDetail'

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

        <div class="player-detail-overview__stats">
          <div class="player-detail-stat-cards">
            <article class="player-detail-stat-card">
              <span class="player-detail-stat-card__icon">⬡</span>
              <strong>{{ detail.honorSummary.appearances }}</strong>
              <span>出场次数</span>
            </article>
            <article class="player-detail-stat-card">
              <span class="player-detail-stat-card__icon">⬡</span>
              <strong>{{ detail.honorSummary.weeklyBestCount }}</strong>
              <span>周最佳球员</span>
            </article>
            <article class="player-detail-stat-card">
              <span class="player-detail-stat-card__icon">⬡</span>
              <strong>{{ detail.honorSummary.allStarCount }}</strong>
              <span>全明星球员</span>
            </article>
            <article class="player-detail-stat-card">
              <span class="player-detail-stat-card__icon">⬡</span>
              <strong>{{ detail.honorSummary.bestLineupCount }}</strong>
              <span>最佳阵容</span>
            </article>
          </div>

          <div class="player-detail-medals">
            <div class="player-detail-medal">
              <span class="player-detail-medal__icon player-detail-medal__icon--gold">1</span>
              <strong>{{ detail.honorSummary.championCount }}</strong>
              <span>冠军</span>
            </div>
            <div class="player-detail-medal">
              <span class="player-detail-medal__icon player-detail-medal__icon--silver">2</span>
              <strong>{{ detail.honorSummary.runnerUpCount }}</strong>
              <span>亚军</span>
            </div>
            <div class="player-detail-medal">
              <span class="player-detail-medal__icon player-detail-medal__icon--bronze">3</span>
              <strong>{{ detail.honorSummary.thirdPlaceCount }}</strong>
              <span>季军</span>
            </div>
          </div>
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
  gap: 18px;
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
