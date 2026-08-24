<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStanding, getV4PublicTeamList } from '@/api/v4Public'
import type { Team } from '../types/events'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { toEventTeam } from '../utils/mapPublic'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const ctx = inject<SeasonContext>('publicSeasonContext')!

type LeagueTeam = Team & { sourceTeamId?: string }

const pageSize = 10
const currentPage = ref(0)
const teams = ref<LeagueTeam[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const seasonOptions = computed(() =>
  ctx.seasons.value.map((item) => ({
    value: String(item.seasonId),
    label: item.seasonName || item.competitionSeasonName || `赛季 ${item.seasonId}`,
  })),
)

const selectedSeason = computed({
  get: () => ctx.seasonId.value,
  set: (value: string) => {
    if (value) ctx.selectSeason(value)
  },
})

const totalPages = computed(() => Math.max(1, Math.ceil(teams.value.length / pageSize)))

const visibleTeams = computed(() => {
  const start = currentPage.value * pageSize
  return teams.value.slice(start, start + pageSize)
})

async function loadTeams(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    teams.value = []
    state.value = 'empty'
    currentPage.value = 0
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const teamRes = await getV4PublicTeamList(divisionId)
    let rows = teamRes.data ?? []
    if (!rows.length) {
      const standingRes = await getV4PublicStanding(divisionId)
      rows = (standingRes.data ?? []).map((row) => ({
        eventTeamId: row.eventTeamId,
        teamName: row.teamName || '',
        teamShortName: row.teamName || '',
        teamLogoUrl: row.teamLogoUrl || '',
      }))
    }
    if (current !== reqId) return
    teams.value = rows.map((row) => ({
      ...toEventTeam({
        teamId: row.eventTeamId,
        teamName: row.teamName,
        teamLogoUrl: row.teamLogoUrl,
        disqualified: Boolean(row.disqualified || row.eventTeamStatus === 'DISQUALIFIED'),
      }),
      sourceTeamId: row.sourceTeamId,
    }))
    currentPage.value = 0
    state.value = teams.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    teams.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载战队失败'
  }
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void loadTeams(divisionId)
  },
  { immediate: true },
)

watch(totalPages, (pages) => {
  if (currentPage.value >= pages) {
    currentPage.value = Math.max(0, pages - 1)
  }
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
          <h2 class="section-head-title">参赛球队</h2>
          <span class="section-head-sub">{{ ctx.currentTournament?.value?.tournamentName || ctx.divisionDetail?.value?.seriesName || 'Teams' }}</span>
        </div>
        <div class="head-actions">
          <label class="season-select">
            <select v-model="selectedSeason">
              <option v-for="season in seasonOptions" :key="season.value" :value="season.value">
                {{ season.label }}
              </option>
            </select>
          </label>
        </div>
      </div>

      <div class="carousel">
        <button type="button" class="carousel-arrow" aria-label="上一页" @click="goPrev">‹</button>

        <div class="team-grid">
          <router-link
            v-for="team in visibleTeams"
            :key="team.id"
            :to="{
              path: `/tournament/team/${team.teamId}/overview`,
              query: ctx.divisionId.value ? { divisionId: ctx.divisionId.value } : {},
            }"
            class="team-card team-card-link"
          >
            <TeamLogo :team="team" size="lg" />
            <h3>
              {{ team.name }}
              <em v-if="team.disqualified" class="dq-tag">赛事资格已取消</em>
            </h3>
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
  border: 1px solid rgba(17, 17, 17, 0.14);
  border-radius: 2px;
  background: #fff;
  color: #111;
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
  border: 1px solid rgba(17, 17, 17, 0.14);
  border-radius: 2px;
  background: #fff;
  color: #111;
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

.dq-tag {
  display: block;
  margin-top: 6px;
  font-style: normal;
  font-size: 12px;
  color: #f87171;
}

.team-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 22px 12px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: #fff;
  transition: border-color 0.2s ease, transform 0.2s ease, opacity 0.2s ease;

  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 800;
    color: #111;
  }

  &:hover {
    border-color: rgba(225, 6, 19, 0.45);
    transform: translateY(-2px);
  }
}

.team-card-link {
  color: inherit;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    opacity: 0.88;
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
