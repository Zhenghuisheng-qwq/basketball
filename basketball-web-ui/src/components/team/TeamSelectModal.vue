<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { getPublicDataCenterSeasons, getPublicDataCenterTeamRanking } from '@/api/publicDataCenter'
import { getPublicSeasonStanding } from '@/api/publicStanding'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { displayText } from '@/utils/publicDisplay'
import {
  mapRankToTeamBase,
  mapStandingToTeamBase,
} from '@/utils/publicTeamMap'
import type { TeamBase } from '@/types/team'

const props = withDefaults(
  defineProps<{
    visible: boolean
    title?: string
    subtitle?: string
    competitionText?: string
    disabledTeamIds?: string[]
    selectedTeamIds?: string[]
    sideText?: string
    teams?: TeamBase[]
    teamsState?: 'loading' | 'success' | 'empty' | 'error'
  }>(),
  {
    title: '选择球队',
    subtitle: '点击球队即可完成更换',
    competitionText: '公开赛季 · 常规赛',
    disabledTeamIds: () => [],
    selectedTeamIds: () => [],
    sideText: '正在选择对比球队',
    teams: undefined,
    teamsState: 'success',
  },
)

const emit = defineEmits<{
  close: []
  select: [teamId: string]
}>()

const fetchedTeams = ref<TeamBase[]>([])
const fetchState = ref<'idle' | 'loading' | 'success' | 'empty' | 'error'>('idle')
let reqId = 0

const resolvedTeams = computed(() => props.teams ?? fetchedTeams.value)

const listState = computed(() => {
  if (props.teams) return props.teamsState
  return fetchState.value === 'idle' ? 'loading' : fetchState.value
})

const disabledSet = computed(() => new Set(props.disabledTeamIds))
const selectedSet = computed(() => new Set(props.selectedTeamIds))

function isDisabled(teamId: string): boolean {
  return disabledSet.value.has(teamId)
}

function isSelected(teamId: string): boolean {
  return selectedSet.value.has(teamId)
}

function handleSelect(team: TeamBase) {
  if (isDisabled(team.id)) return
  emit('select', team.id)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close')
  }
}

async function loadTeams() {
  if (props.teams) return
  const current = ++reqId
  fetchState.value = 'loading'
  try {
    const seasonsRes = await getPublicDataCenterSeasons()
    if (current !== reqId) return
    const season = seasonsRes.data?.[0]
    if (!season?.seasonId) {
      fetchedTeams.value = []
      fetchState.value = 'empty'
      return
    }

    let teams: TeamBase[] = []
    try {
      const standingRes = await getPublicSeasonStanding(season.seasonId)
      if (current !== reqId) return
      teams = (standingRes.data ?? []).map((row) =>
        mapStandingToTeamBase(row, season.seasonName),
      )
    } catch {
      const rankingRes = await getPublicDataCenterTeamRanking({ seasonId: season.seasonId })
      if (current !== reqId) return
      teams = (rankingRes.data ?? []).map((row) => mapRankToTeamBase(row, season.seasonName))
    }

    fetchedTeams.value = teams
    fetchState.value = teams.length ? 'success' : 'empty'
  } catch {
    if (current !== reqId) return
    fetchedTeams.value = []
    fetchState.value = 'error'
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', handleKeydown)
      if (!props.teams) {
        void loadTeams()
      }
      return
    }

    document.body.style.overflow = ''
    window.removeEventListener('keydown', handleKeydown)
  },
)

onUnmounted(() => {
  document.body.style.overflow = ''
  window.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="team-select-mask" @click.self="emit('close')">
      <div
        class="team-select-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="team-select-title"
        @click.stop
      >
        <div class="team-select-modal__header">
          <div class="team-select-modal__heading">
            <h2 id="team-select-title" class="team-select-modal__title">{{ title }}</h2>
            <p class="team-select-modal__subtitle">{{ subtitle }}</p>
            <p v-if="sideText" class="team-select-modal__side-text">{{ sideText }}</p>
          </div>

          <div class="team-select-modal__actions">
            <span class="team-select-modal__meta">{{ competitionText }}</span>
            <button
              type="button"
              class="team-select-modal__close"
              aria-label="关闭"
              @click="emit('close')"
            >
              ×
            </button>
          </div>
        </div>

        <div v-if="listState === 'loading'" class="team-select-modal__state">加载中...</div>
        <div v-else-if="listState === 'error'" class="team-select-modal__state is-error">
          加载球队列表失败
        </div>
        <div v-else-if="resolvedTeams.length === 0" class="team-select-modal__state">
          暂无可选球队
        </div>

        <div v-else class="team-select-grid">
          <button
            v-for="team in resolvedTeams"
            :key="team.id"
            type="button"
            class="team-select-card"
            :class="{
              'is-selected': isSelected(team.id),
              'is-disabled': isDisabled(team.id),
            }"
            :disabled="isDisabled(team.id)"
            @click="handleSelect(team)"
          >
            <TeamLogo :team="team" size="selector" class="team-select-card__logo" />
            <span class="team-select-card__name">{{ displayText(team.name) }}</span>
            <span v-if="isSelected(team.id)" class="team-select-card__badge">已在对比</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
.team-select-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: grid;
  place-items: center;
  padding: 32px;
  background: rgba(0, 0, 0, 0.68);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.team-select-modal {
  width: min(1024px, calc(100vw - 64px));
  max-height: min(86vh, 780px);
  overflow: auto;
  padding: 32px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 2px;
  background: linear-gradient(
    180deg,
    #ebe9e9 0%,
    var(--cnpa-paper-bg) 45%,
    #e7e5e5 100%
  );
  box-shadow: 0 28px 72px rgba(0, 0, 0, 0.32);
  box-sizing: border-box;
}

.team-select-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}

.team-select-modal__title {
  margin: 0 0 8px;
  font-size: clamp(24px, 2.2vw, 28px);
  font-weight: 900;
  color: var(--cnpa-paper-text);
  letter-spacing: -0.02em;
}

.team-select-modal__subtitle {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--cnpa-paper-text-muted);
}

.team-select-modal__side-text {
  margin: 10px 0 0;
  font-size: 13px;
  font-weight: 800;
  color: var(--cnpa-red);
}

.team-select-modal__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.team-select-modal__meta {
  font-size: 13px;
  font-weight: 800;
  color: var(--cnpa-paper-text-muted);
  white-space: nowrap;
}

.team-select-modal__close {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.28);
  color: var(--cnpa-paper-text);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.team-select-modal__state {
  padding: 48px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;
  font-weight: 700;

  &.is-error {
    color: var(--cnpa-red);
  }
}

.team-select-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.team-select-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 142px;
  padding: 20px 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.28);
  color: var(--cnpa-paper-text);
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease, opacity 0.2s ease;

  &:hover:not(:disabled) {
    border-color: rgba(217, 0, 18, 0.55);
    transform: translateY(-2px);
  }

  &.is-disabled,
  &:disabled {
    cursor: not-allowed;
    opacity: 0.46;
  }

  &.is-selected:not(:disabled) {
    border-color: rgba(0, 0, 0, 0.16);
  }
}

.team-select-card__logo {
  flex-shrink: 0;
}

.team-select-card__name {
  font-size: 13px;
  font-weight: 900;
  text-align: center;
  line-height: 1.2;
}

.team-select-card__badge {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 7px;
  border-radius: 2px;
  background: rgba(0, 0, 0, 0.08);
  color: var(--cnpa-paper-text-muted);
  font-size: 10px;
  font-weight: 800;
  line-height: 1;
}

@media (max-width: 768px) {
  .team-select-mask {
    padding: 16px;
  }

  .team-select-modal {
    width: calc(100vw - 32px);
    padding: 24px 20px;
  }

  .team-select-modal__header {
    flex-direction: column;
  }

  .team-select-modal__actions {
    width: 100%;
    justify-content: space-between;
  }

  .team-select-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }
}
</style>
