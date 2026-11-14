<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { AvailableEvent } from '@/types/accountEvents'
import type { MyEventsRoleMode } from '@/api/types/myEvents'
import { formatDateTimeShort } from '@/utils/participationMappers'
import { resolveEventStatusTag } from '@/utils/eventStatusUi'

const props = defineProps<{
  events: AvailableEvent[]
  roleMode?: MyEventsRoleMode
}>()

const emit = defineEmits<{
  action: [event: AvailableEvent, actionType?: AvailableEvent['actionType']]
}>()

const { t } = useI18n()

const isNoTeam = computed(() => props.roleMode === 'NO_TEAM')
const isCaptain = computed(() => props.roleMode === 'TEAM_CAPTAIN')

const rowClass = computed(() => {
  if (isNoTeam.value) return 'event-table-row--no-team'
  if (isCaptain.value) return 'event-table-row--captain'
  return 'event-table-row--member'
})

const headClass = computed(() => {
  if (isNoTeam.value) return 'event-table-head--no-team'
  if (isCaptain.value) return 'event-table-head--captain'
  return 'event-table-head--member'
})

function isPrimaryDisabled(event: AvailableEvent) {
  if ([
    'VIEW_REGISTRATION',
    'MANAGE_REGISTRATION',
    'VIEW_ROSTER',
    'CONTACT_CAPTAIN',
    'JOIN_TEAM',
    'RE_REGISTER',
    'ROSTER_NOT_SELECTED',
  ].includes(event.actionType)) {
    return false
  }
  if (event.actionType === 'REMIND_CAPTAIN') {
    return !event.canRemindCaptain
  }
  return !event.canRegister
}

function isPrimaryAction(event: AvailableEvent) {
  return event.canRegister || [
    'TEAM_REGISTRATION',
    'RE_REGISTER',
    'MANAGE_REGISTRATION',
  ].includes(event.actionType)
}

function signupRange(event: AvailableEvent) {
  const start = formatDateTimeShort(event.signupStartTime)
  const end = formatDateTimeShort(event.deadline)
  if (start === '-' && end === '-') return '-'
  return `${start} ~ ${end}`
}

function matchRange(event: AvailableEvent) {
  const start = formatDateTimeShort(event.matchStartTime)
  const end = formatDateTimeShort(event.matchEndTime)
  if (start === '-' && end === '-') return '-'
  return `${start} ~ ${end}`
}

function platformLabel(event: AvailableEvent) {
  if (!event.platform || event.platform === '-') return '-'
  const platform = event.platform.replace(/端$/, '')
  return t('accountEvents.available.platformClient', { platform })
}
</script>

<template>
  <div class="event-list-card">
    <div class="event-table-head" :class="headClass">
      <span>{{ t('accountEvents.available.name') }}</span>
      <span>{{ t('accountEvents.available.action') }}</span>
      <span>{{ t('accountEvents.available.season') }}</span>
      <span>{{ t('accountEvents.available.platform') }}</span>
      <span>{{ t('accountEvents.available.signupTime') }}</span>
      <span>{{ t('accountEvents.available.matchTime') }}</span>
      <span>{{ t('accountEvents.available.eventStatus') }}</span>
      <template v-if="isNoTeam">
        <span>{{ t('accountEvents.available.myStatus') }}</span>
      </template>
      <template v-else>
        <span>{{ t('accountEvents.available.myTeam') }}</span>
        <span>{{ t('accountEvents.available.teamRegistration') }}</span>
        <span>{{ t('accountEvents.available.myRoster') }}</span>
        <span>{{ t('accountEvents.available.captainGrant') }}</span>
      </template>
    </div>

    <div
      v-for="event in events"
      :key="`${event.operationalSeasonId || event.seasonId || event.divisionId}`"
      class="event-table-row"
      :class="rowClass"
    >
      <div class="event-table-row__name-cell">
        <strong class="event-table-row__name">{{ event.tournamentName }}</strong>
        <span v-if="event.displayTitle" class="event-table-row__subtitle">{{ event.displayTitle }}</span>
      </div>
      <div class="event-table-row__action">
        <button
          type="button"
          :class="isPrimaryAction(event) ? 'account-btn-primary event-table-row__btn' : 'account-btn-secondary event-table-row__btn'"
          :disabled="isPrimaryDisabled(event)"
          :title="event.disabledReason"
          @click="emit('action', event)"
        >
          {{ event.actionText }}
        </button>
        <span
          v-if="isPrimaryDisabled(event) && event.disabledReason"
          class="event-table-row__disabled-reason"
        >
          {{ event.disabledReason }}
        </span>
      </div>
      <span>{{ event.seasonName || event.competitionSeasonName || '-' }}</span>
      <span>{{ platformLabel(event) }}</span>
      <span>{{ signupRange(event) }}</span>
      <span>{{ matchRange(event) }}</span>
      <span>
        <span
          class="event-status-tag"
          :class="`event-status-tag--${resolveEventStatusTag(event).variant}`"
        >
          {{ resolveEventStatusTag(event).label }}
        </span>
      </span>

      <template v-if="isNoTeam">
        <span>{{ event.userMembershipStatusText || t('accountEvents.available.notInTeam') }}</span>
      </template>
      <template v-else>
        <span>{{ event.teamName || '-' }}</span>
        <span>{{ event.teamRegistrationStatusText || t('accountEvents.available.notRegistered') }}</span>
        <span>{{ event.userRosterStatusText || t('accountEvents.available.noRoster') }}</span>
        <span>{{ isCaptain ? (event.captainGrantStatusText || '-') : t('accountEvents.available.captainOnly') }}</span>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.event-list-card {
  width: 100%;
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.28);
  overflow-x: auto;
}

.event-table-head,
.event-table-row {
  display: grid;
  align-items: center;
  column-gap: 10px;
  padding: 0 16px;
  box-sizing: border-box;
  min-width: 1180px;
}

.event-table-head--no-team,
.event-table-row--no-team {
  grid-template-columns:
    minmax(160px, 1.2fr)
    120px
    minmax(90px, 0.7fr)
    minmax(80px, 0.5fr)
    minmax(140px, 0.9fr)
    minmax(140px, 0.9fr)
    minmax(90px, 0.6fr)
    minmax(100px, 0.6fr);
}

.event-table-head--member,
.event-table-row--member,
.event-table-head--captain,
.event-table-row--captain {
  grid-template-columns:
    minmax(160px, 1.1fr)
    120px
    minmax(90px, 0.7fr)
    minmax(80px, 0.5fr)
    minmax(130px, 0.85fr)
    minmax(130px, 0.85fr)
    minmax(90px, 0.6fr)
    minmax(90px, 0.6fr)
    minmax(80px, 0.5fr)
    minmax(80px, 0.5fr)
    minmax(100px, 0.6fr);
}

.event-table-head {
  min-height: 56px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.14);
  background: rgba(0, 0, 0, 0.04);
  color: rgba(0, 0, 0, 0.62);
  font-size: 13px;
  font-weight: 800;

  span:nth-child(2) {
    text-align: left;
  }
}

.event-table-row {
  min-height: 84px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.12);
  font-size: 14px;
  color: #222;

  &:last-child {
    border-bottom: 0;
  }

  &__name-cell {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  &__name {
    font-size: 16px;
    font-weight: 800;
    color: #111;
    line-height: 1.3;
  }

  &__subtitle {
    font-size: 12px;
    color: rgba(0, 0, 0, 0.55);
    line-height: 1.4;
  }

  &__action {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-start;
    gap: 6px;
  }

  &__btn {
    min-width: 88px;
    min-height: 36px;
    padding: 0 10px;
    font-size: 13px;
  }

  &__disabled-reason {
    width: 100%;
    text-align: left;
    font-size: 12px;
    color: #b42318;
    line-height: 1.3;
  }
}

.event-status-tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 3px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;

  &--not-started {
    color: #666;
    background: #f0f0f0;
  }

  &--open {
    color: #1f7a1f;
    background: #e8f5e9;
  }

  &--closed {
    color: #b45309;
    background: #fff3e0;
  }

  &--match-not-started {
    color: #1565c0;
    background: #e3f2fd;
  }

  &--in-progress {
    color: #c62828;
    background: #ffebee;
  }

  &--ended {
    color: #eceff1;
    background: #37474f;
  }

  &--cancelled {
    color: #cfd8dc;
    background: #455a64;
  }

  &--default {
    color: #555;
    background: #f5f5f5;
  }
}
</style>
