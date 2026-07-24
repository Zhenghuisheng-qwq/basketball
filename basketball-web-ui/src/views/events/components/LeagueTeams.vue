<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getPublicSeasonStanding } from '@/api/publicStanding'
import type { PublicStandingItemVo } from '@/api/types/public'
import { displayText } from '@/utils/publicDisplay'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { standingToTeam } from '../utils/mapPublic'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const ctx = inject<SeasonContext>('publicSeasonContext')!

const pageSize = 10
const currentPage = ref(0)
const teams = ref<PublicStandingItemVo[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const totalPages = computed(() => Math.max(1, Math.ceil(teams.value.length / pageSize)))

const visibleTeams = computed(() => {
  const start = currentPage.value * pageSize
  return teams.value.slice(start, start + pageSize).map(standingToTeam)
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

async function loadTeams(seasonId: string) {
  const current = ++reqId
  if (!seasonId) {
    teams.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getPublicSeasonStanding(seasonId)
    if (current !== reqId) return
    teams.value = res.data ?? []
    currentPage.value = 0
    state.value = teams.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    teams.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载联赛战队失败'
  }
}

watch(
  () => ctx.seasonId.value,
  (sid) => {
    void loadTeams(sid)
  },
  { immediate: true },
)
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
              <select
                :value="ctx.seasonId.value"
                @change="ctx.selectSeason(($event.target as HTMLSelectElement).value)"
              >
                <option
                  v-for="season in ctx.seasons.value"
                  :key="season.seasonId"
                  :value="season.seasonId"
                >
                  {{ displayText(season.seasonName) }}
                </option>
              </select>
            </label>
          </div>
        </div>

        <div v-if="state === 'loading'" class="league-teams__state">加载中...</div>
        <div v-else-if="state === 'error'" class="league-teams__state is-error">{{ errorMessage }}</div>
        <div v-else-if="state === 'empty'" class="league-teams__state">暂无联赛战队</div>
        <template v-else>
          <div class="carousel">
            <button type="button" class="carousel-arrow" aria-label="上一页" @click="goPrev">‹</button>

            <div class="team-grid">
              <router-link
                v-for="team in visibleTeams"
                :key="team.teamId"
                :to="`/tournament/team/${team.teamId}/overview`"
                class="team-card team-card-link"
              >
                <TeamLogo :team="team" size="lg" />
                <h3>{{ team.name }}</h3>
              </router-link>
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
        </template>
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
  border-radius: 4px;
  background: #151515;
  color: #fff;
}

.league-teams__state {
  padding: 40px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 600;

  &.is-error {
    color: #e10613;
  }
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
  border-radius: 50%;
  background: transparent;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.team-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: #111;
  color: #fff;
  text-decoration: none;

  h3 {
    margin: 0;
    font-size: 14px;
    text-align: center;
  }
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
}

.dot {
  width: 8px;
  height: 8px;
  border: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  cursor: pointer;

  &.active {
    background: #e10613;
  }
}

@media (max-width: 992px) {
  .team-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .team-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
