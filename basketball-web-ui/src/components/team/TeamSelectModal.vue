<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getV4PublicTeamList } from '@/api/v4Public'
import TeamLogo from '@/components/team/TeamLogo.vue'
import { displayText } from '@/utils/publicDisplay'
import type { TeamBase } from '@/types/team'
import { mapV4EventTeamToBase } from '@/views/tournament/team/utils/mapV4PublicTeam'

const props = withDefaults(
  defineProps<{
    visible: boolean
    title?: string
    subtitle?: string
    competitionText?: string
    currentTeamId?: string
    disabledTeamIds?: string[]
    selectedTeamIds?: string[]
    teams?: TeamBase[]
    teamsState?: 'loading' | 'success' | 'empty' | 'error'
    divisionId?: string
  }>(),
  {
    title: '',
    subtitle: '',
    competitionText: '',
    currentTeamId: '',
    disabledTeamIds: () => [],
    selectedTeamIds: () => [],
    teams: undefined,
    teamsState: 'success',
    divisionId: '',
  },
)

const emit = defineEmits<{
  close: []
  select: [teamId: string]
}>()

const { t } = useI18n()
const heading = computed(() => props.title || t('publicPages.team.selectCompareTitle'))
const subheading = computed(() => props.subtitle || t('publicPages.team.selectCompareSubtitle'))

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

const hasComparableTeams = computed(() =>
  resolvedTeams.value.some((team) => team.id !== props.currentTeamId),
)

function isDisabled(teamId: string): boolean {
  return disabledSet.value.has(teamId)
}

function isCurrent(teamId: string): boolean {
  return Boolean(props.currentTeamId) && teamId === props.currentTeamId && isDisabled(teamId)
}

function isSelected(teamId: string): boolean {
  return selectedSet.value.has(teamId) && !isCurrent(teamId)
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
  const divisionId = props.divisionId.trim()
  const current = ++reqId
  if (!divisionId) {
    fetchedTeams.value = []
    fetchState.value = 'empty'
    return
  }

  fetchState.value = 'loading'
  try {
    const teamRes = await getV4PublicTeamList(divisionId)
    if (current !== reqId) return
    const teams = (teamRes.data ?? []).map((team) => mapV4EventTeamToBase(team))
    fetchedTeams.value = teams
    fetchState.value = teams.length ? 'success' : 'empty'
  } catch {
    if (current !== reqId) return
    fetchedTeams.value = []
    fetchState.value = 'error'
  }
}

watch(
  () => [props.visible, props.divisionId] as const,
  ([visible]) => {
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
            <h2 id="team-select-title" class="team-select-modal__title">{{ heading }}</h2>
            <p class="team-select-modal__subtitle">{{ subheading }}</p>
          </div>

          <div class="team-select-modal__actions">
            <span v-if="competitionText" class="team-select-modal__meta">{{ competitionText }}</span>
            <button
              type="button"
              class="team-select-modal__close"
              :aria-label="t('publicPages.team.close')"
              @click="emit('close')"
            >
              ×
            </button>
          </div>
        </div>

        <div class="team-select-modal__body">
          <div v-if="listState === 'loading'" class="team-select-modal__state">{{ t('publicPages.team.loading') }}</div>
          <div v-else-if="listState === 'error'" class="team-select-modal__state is-error">
            {{ t('publicPages.team.loadTeamListFailed') }}
          </div>
          <div v-else-if="!hasComparableTeams" class="team-select-modal__state">
            {{ t('publicPages.team.noCompareTeams') }}
          </div>

          <div v-else class="team-select-grid">
            <button
              v-for="team in resolvedTeams"
              :key="team.id"
              type="button"
              class="team-select-card"
              :class="{
                'is-selected': isSelected(team.id),
                'is-current': isCurrent(team.id),
                'is-disabled': isDisabled(team.id),
              }"
              :disabled="isDisabled(team.id)"
              @click="handleSelect(team)"
            >
              <TeamLogo :team="team" size="selector" class="team-select-card__logo" />
              <span class="team-select-card__name">{{ displayText(team.name) }}</span>
              <span v-if="isCurrent(team.id)" class="team-select-card__badge">{{ t('publicPages.team.currentTeam') }}</span>
            </button>
          </div>
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
  display: flex;
  flex-direction: column;
  width: min(1024px, calc(100vw - 64px));
  max-height: min(78vh, 680px);
  overflow: hidden;
  padding: 28px 32px 24px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 4px;
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
  flex-shrink: 0;
  margin-bottom: 20px;
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

.team-select-modal__actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.team-select-modal__meta {
  font-size: 12px;
  font-weight: 600;
  color: #8a8680;
  white-space: nowrap;
}

.team-select-modal__close {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.28);
  color: var(--cnpa-paper-text);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s ease, color 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.team-select-modal__body {
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
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
  gap: 14px;
}

.team-select-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 124px;
  padding: 16px 12px 14px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 4px;
  background: #f7f4ef;
  color: var(--cnpa-paper-text);
  cursor: pointer;
  font-family: inherit;
  transition: border-color 0.2s ease, color 0.2s ease;

  &:hover:not(:disabled) {
    border-color: var(--cnpa-red);

    .team-select-card__name {
      font-weight: 900;
    }
  }

  &.is-current,
  &.is-disabled,
  &:disabled {
    cursor: default;
    opacity: 0.72;
  }

  &.is-selected:not(:disabled) {
    border-color: rgba(0, 0, 0, 0.22);
  }
}

.team-select-card__logo {
  flex-shrink: 0;
}

.team-select-card__name {
  font-size: 15px;
  font-weight: 800;
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
    padding: 22px 18px 18px;
  }

  .team-select-modal__header {
    flex-direction: column;
  }

  .team-select-modal__actions {
    width: 100%;
    justify-content: space-between;
  }

  .team-select-modal__meta {
    white-space: normal;
  }

  .team-select-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}
</style>
