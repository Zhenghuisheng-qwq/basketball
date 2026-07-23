<script setup lang="ts">
import { computed, onUnmounted, watch } from 'vue'
import { getAllTeams, getTeamById } from '@/mock/team'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { TeamBase, TeamStage } from '@/types/team'

const props = defineProps<{
  visible: boolean
  side: 'left' | 'right'
  leftTeamId: string
  rightTeamId: string
}>()

const emit = defineEmits<{
  close: []
  select: [teamId: string]
}>()

const stageLabels: Record<TeamStage, string> = {
  regular: '常规赛',
  playoffs: '季后赛',
}

const teamOrder = [
  'rng',
  'wolves',
  'phoenix',
  'all-star',
  'titan',
  'dragon',
  'legend',
  'omega',
  'beast',
  'unity',
  'phx-fire',
  'eik',
  'storm',
  'vortex',
  'knights',
  'invictus',
]

const teamMap = computed(() => new Map(getAllTeams().map((team) => [team.id, team])))

const orderedTeams = computed(() =>
  teamOrder
    .map((id) => teamMap.value.get(id))
    .filter((team): team is TeamBase => Boolean(team)),
)

const headerMeta = computed(() => {
  const team = getTeamById(props.leftTeamId)
  return `${team.seasonName} · ${stageLabels[team.currentStage]}`
})

function isInCompare(teamId: string): boolean {
  return teamId === props.leftTeamId || teamId === props.rightTeamId
}

function handleSelect(team: TeamBase) {
  if (isInCompare(team.id)) return
  emit('select', team.id)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    emit('close')
  }
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      document.body.style.overflow = 'hidden'
      window.addEventListener('keydown', handleKeydown)
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
    <div v-if="visible" class="team-selector-mask" @click.self="emit('close')">
      <div
        class="team-selector-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="team-selector-title"
        @click.stop
      >
        <div class="team-selector-dialog__header">
          <div class="team-selector-dialog__heading">
            <h2 id="team-selector-title" class="team-selector-dialog__title">选择球队</h2>
            <p class="team-selector-dialog__subtitle">点击球队即可完成更换</p>
          </div>

          <div class="team-selector-dialog__actions">
            <span class="team-selector-dialog__meta">{{ headerMeta }}</span>
            <button
              type="button"
              class="team-selector-dialog__close"
              aria-label="关闭"
              @click="emit('close')"
            >
              ×
            </button>
          </div>
        </div>

        <p class="team-selector-dialog__side-hint">
          正在更换{{ side === 'left' ? '左侧' : '右侧' }}球队
        </p>

        <div class="team-selector-grid">
          <button
            v-for="team in orderedTeams"
            :key="team.id"
            type="button"
            class="team-selector-card"
            :class="{ 'is-in-compare': isInCompare(team.id) }"
            :disabled="isInCompare(team.id)"
            @click="handleSelect(team)"
          >
            <TeamLogo :team="team" size="selector" class="team-selector-card__logo" />
            <span class="team-selector-card__name">{{ team.name }}</span>
            <span v-if="isInCompare(team.id)" class="team-selector-card__badge">已在对比</span>
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
.team-selector-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.72);
}

.team-selector-dialog {
  width: min(920px, 100%);
  max-height: min(84vh, 760px);
  overflow: auto;
  padding: 24px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: var(--cnpa-paper-bg);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.28);
}

.team-selector-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.team-selector-dialog__title {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 900;
  color: var(--cnpa-paper-text);
}

.team-selector-dialog__subtitle {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--cnpa-paper-text-muted);
}

.team-selector-dialog__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.team-selector-dialog__meta {
  font-size: 12px;
  font-weight: 700;
  color: var(--cnpa-paper-text-muted);
  white-space: nowrap;
}

.team-selector-dialog__close {
  width: 32px;
  height: 32px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.72);
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

.team-selector-dialog__side-hint {
  margin: 0 0 20px;
  font-size: 13px;
  font-weight: 700;
  color: var(--cnpa-red);
}

.team-selector-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.team-selector-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-height: 132px;
  padding: 18px 12px 14px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: rgba(255, 255, 255, 0.85);
  color: var(--cnpa-paper-text);
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s ease, transform 0.2s ease, opacity 0.2s ease;

  &:hover:not(:disabled) {
    border-color: var(--gold);
    transform: translateY(-2px);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.48;
  }

  &.is-in-compare {
    border-color: rgba(17, 17, 17, 0.16);
  }
}

.team-selector-card__logo {
  margin-bottom: 0;
}

.team-selector-card__name {
  font-size: 13px;
  font-weight: 800;
  text-align: center;
}

.team-selector-card__badge {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 6px;
  border-radius: 2px;
  background: rgba(17, 17, 17, 0.08);
  color: var(--cnpa-paper-text-muted);
  font-size: 10px;
  font-weight: 800;
}

@media (max-width: 768px) {
  .team-selector-dialog__header {
    flex-direction: column;
  }

  .team-selector-dialog__actions {
    width: 100%;
    justify-content: space-between;
  }

  .team-selector-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
