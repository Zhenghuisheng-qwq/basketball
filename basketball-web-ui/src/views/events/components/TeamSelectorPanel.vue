<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import { getV4PublicStanding } from '@/api/v4Public'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import type { Team } from '../types/events'
import { standingToTeam } from '../utils/mapPublic'
import { v4StandingToPublic } from '../utils/v4PublicAdapters'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

defineProps<{
  selectedTeamId: number | null
}>()

const emit = defineEmits<{
  select: [teamId: number | null]
}>()

const ctx = inject<SeasonContext>('publicSeasonContext')!
const teams = ref<Team[]>([])

async function loadTeams(divisionId: string) {
  if (!divisionId) {
    teams.value = []
    return
  }
  try {
    const res = await getV4PublicStanding(divisionId)
    teams.value = (res.data ?? []).map((row) =>
      standingToTeam(v4StandingToPublic(row, ctx.seasonId.value)),
    )
  } catch {
    teams.value = []
  }
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void loadTeams(divisionId)
  },
  { immediate: true },
)
</script>
<template>
  <div
    class="team-panel schedule-panel cnpa-paper-section cnpa-paper-section--gray cnpa-paper-floating-panel"
    @click.stop
  >
    <h3>选择参赛战队</h3>
    <div class="team-list">
      <button
        type="button"
        class="team-item"
        :class="{ active: selectedTeamId === null }"
        @click="emit('select', null)"
      >
        <span class="team-all">ALL</span>
        <span>全部战队</span>
        <span v-if="selectedTeamId === null" class="check">●</span>
      </button>

      <button
        v-for="team in teams"
        :key="team.id"
        type="button"
        class="team-item"
        :class="{ active: selectedTeamId === team.id }"
        @click="emit('select', team.id)"
      >
        <TeamLogo :team="team" size="sm" />
        <span>{{ team.name }}</span>
        <span v-if="selectedTeamId === team.id" class="check">●</span>
      </button>
    </div>
  </div>
</template>
<style scoped lang="scss">
.team-panel {
  width: min(920px, calc(100vw - 32px));
  padding: 20px;

  h3 {
    margin: 0 0 16px;
    font-size: 16px;
    color: #111;
  }
}

.team-list {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 10px;
}

.team-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-height: 92px;
  padding: 10px 8px;
  border: 1px solid rgba(10, 10, 10, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: #111;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover,
  &.active {
    border-color: var(--cnpa-red);
    box-shadow: inset 0 0 0 1px rgba(225, 6, 19, 0.18);
  }
}

.team-all {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #111;
  color: #fff;
  font-size: 11px;
}

.check {
  position: absolute;
  top: 6px;
  right: 8px;
  color: var(--cnpa-red);
  font-size: 10px;
}

@media (max-width: 992px) {
  .team-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .team-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
