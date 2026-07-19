<script setup lang="ts">
import { computed, ref } from 'vue'
import { teams } from '../mock'
import { seasonOptions } from '../mock/league.mock'
import TeamLogo from './TeamLogo.vue'

const pageSize = 10
const currentPage = ref(0)
const selectedSeason = ref(seasonOptions[0])

const totalPages = computed(() => Math.ceil(teams.length / pageSize))

const visibleTeams = computed(() => {
  const start = currentPage.value * pageSize
  return teams.slice(start, start + pageSize)
})

function goPrev() {
  currentPage.value = (currentPage.value - 1 + totalPages.value) % totalPages.value
}

function goNext() {
  currentPage.value = (currentPage.value + 1) % totalPages.value
}

function goPage(index: number) {
  currentPage.value = index
}
</script>

<template>
  <div class="site-shell">
    <section class="league-teams events-section-panel">
      <div class="site-shell-inner">
      <div class="section-head">
        <div class="section-head-group">
          <h2 class="section-head-title">联赛战队</h2>
          <span class="section-head-sub">League Teams</span>
        </div>
        <div class="head-actions">
          <label class="season-select">
            <select v-model="selectedSeason">
              <option v-for="season in seasonOptions" :key="season" :value="season">
                {{ season }}
              </option>
            </select>
          </label>
        </div>
      </div>

      <div class="carousel">
        <button type="button" class="carousel-arrow" aria-label="上一页" @click="goPrev">‹</button>

        <div class="team-grid">
          <article v-for="team in visibleTeams" :key="team.id" class="team-card">
            <TeamLogo :team="team" size="lg" />
            <h3>{{ team.name }}</h3>
          </article>
        </div>

        <button type="button" class="carousel-arrow" aria-label="下一页" @click="goNext">›</button>
      </div>

      <div class="carousel-dots">
        <button
          v-for="index in totalPages"
          :key="index"
          type="button"
          class="dot"
          :class="{ active: currentPage === index - 1 }"
          :aria-label="`第 ${index} 页`"
          @click="goPage(index - 1)"
        />
      </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.league-teams {
  padding: 56px 0;
}

.head-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.season-select select {
  min-width: 132px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 2px;
  background: #151515;
  color: #fff;
  font-family: inherit;
  font-size: 13px;
}

.carousel {
  display: grid;
  grid-template-columns: 40px 1fr 40px;
  gap: 12px;
  align-items: center;
}

.carousel-arrow {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.04);
  color: #fff;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.team-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 22px 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #0d0d0d;
  transition: border-color 0.2s ease, transform 0.2s ease;

  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 800;
  }

  &:hover {
    border-color: rgba(225, 6, 19, 0.45);
    transform: translateY(-2px);
  }
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 22px;
}

.dot {
  width: 8px;
  height: 8px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;

  &.active {
    background: var(--cnpa-red);
    transform: scale(1.2);
  }
}

@media (max-width: 1200px) {
  .team-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .team-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .head-actions {
    flex-direction: column;
    align-items: flex-end;
  }
}

@media (max-width: 768px) {
  .team-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .carousel {
    grid-template-columns: 32px 1fr 32px;
  }
}
</style>
