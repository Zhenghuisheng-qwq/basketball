<script setup lang="ts">
import { ref } from 'vue'
import { seasonLeaders } from '../mock'
import { getTeamById } from '../mock/teams.mock'
import TeamLogo from './TeamLogo.vue'

const imageErrors = ref<Record<number, boolean>>({})

function handlePortraitError(id: number) {
  imageErrors.value[id] = true
}
</script>

<template>
  <section class="season-leaders events-section events-section-dark">
    <div class="events-inner">
      <div class="section-head">
        <div>
          <h2 class="section-head-title">高光选手</h2>
          <span class="section-head-sub">Season Leaders</span>
        </div>
        <button type="button" class="section-head-action">
          查看完整球员数据
          <span>→</span>
        </button>
      </div>

      <div class="leader-grid">
        <article v-for="leader in seasonLeaders" :key="leader.id" class="leader-card">
          <div class="leader-info">
            <span class="leader-title">{{ leader.title }}</span>
            <div class="leader-value sport-num">
              {{ leader.value }}
              <small>{{ leader.unit }}</small>
            </div>
            <strong class="leader-name">{{ leader.playerName }}</strong>
            <div v-if="getTeamById(leader.teamId)" class="leader-team">
              <TeamLogo :team="getTeamById(leader.teamId)!" size="sm" />
              <span>{{ getTeamById(leader.teamId)!.name }}</span>
            </div>
          </div>

          <div class="leader-portrait">
            <img
              v-if="!imageErrors[leader.id]"
              :src="leader.portrait"
              :alt="leader.playerName"
              @error="handlePortraitError(leader.id)"
            />
            <div v-else class="leader-silhouette" aria-hidden="true" />
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.season-leaders {
  padding: 56px 0;
}

.leader-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.leader-card {
  display: grid;
  grid-template-columns: 1fr 120px;
  min-height: 180px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #111;
  overflow: hidden;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: var(--cnpa-red);
    box-shadow: 0 12px 32px rgba(225, 6, 19, 0.12);
  }
}

.leader-info {
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.leader-title {
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}

.leader-value {
  font-size: clamp(36px, 3.6vw, 48px);
  line-height: 1;
  color: var(--cnpa-red);

  small {
    margin-left: 6px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.72);
  }
}

.leader-name {
  font-size: 18px;
  font-weight: 900;
}

.leader-team {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}

.leader-portrait {
  position: relative;
  background: linear-gradient(180deg, rgba(225, 6, 19, 0.08) 0%, rgba(0, 0, 0, 0.4) 100%);

  img,
  .leader-silhouette {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top center;
  }
}

.leader-silhouette {
  background:
    radial-gradient(circle at 50% 28%, rgba(255, 255, 255, 0.18) 0%, transparent 34%),
    linear-gradient(180deg, #2a2a2a 0%, #111 100%);
  clip-path: polygon(28% 100%, 72% 100%, 68% 38%, 50% 24%, 32% 38%);
}

@media (max-width: 1200px) {
  .leader-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .leader-grid {
    grid-template-columns: 1fr;
  }

  .leader-card {
    grid-template-columns: 1fr 96px;
  }
}
</style>
