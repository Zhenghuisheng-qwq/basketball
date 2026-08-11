<script setup lang="ts">
import { computed } from 'vue'
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
  return event.platform.endsWith('端') ? event.platform : `${event.platform}端`
}
</script>

<template>
  <div class="event-list-card">
    <div class="event-table-head" :class="headClass">
      <span>赛事名称</span>
      <span>赛季</span>
      <span>游戏端</span>
      <span>报名时间</span>
      <span>比赛时间</span>
      <span>赛事状态</span>
      <template v-if="isNoTeam">
        <span>我的状态</span>
      </template>
      <template v-else>
        <span>我的球队</span>
        <span>球队报名</span>
        <span>我的阵容</span>
        <span>队长授权</span>
      </template>
      <span>操作</span>
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
        <span>{{ event.userMembershipStatusText || '未加入球队' }}</span>
      </template>
      <template v-else>
        <span>{{ event.teamName || '-' }}</span>
        <span>{{ event.teamRegistrationStatusText || '未报名' }}</span>
        <span>{{ event.userRosterStatusText || '暂无阵容' }}</span>
        <span>{{ isCaptain ? (event.captainGrantStatusText || '-') : '仅队长可操作' }}</span>
      </template>

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
    minmax(180px, 1.2fr)
    minmax(90px, 0.7fr)
    minmax(80px, 0.5fr)
    minmax(140px, 0.9fr)
    minmax(140px, 0.9fr)
    minmax(90px, 0.6fr)
    minmax(100px, 0.6fr)
    minmax(140px, 0.8fr);
}

.event-table-head--member,
.event-table-row--member {
  grid-template-columns:
    minmax(180px, 1.1fr)
    minmax(90px, 0.7fr)
    minmax(80px, 0.5fr)
    minmax(130px, 0.85fr)
    minmax(130px, 0.85fr)
    minmax(90px, 0.6fr)
    minmax(90px, 0.6fr)
    minmax(80px, 0.5fr)
    minmax(80px, 0.5fr)
    minmax(90px, 0.55fr)
    minmax(140px, 0.9fr);
}

.event-table-head--captain,
.event-table-row--captain {
  grid-template-columns:
    minmax(180px, 1.1fr)
    minmax(90px, 0.7fr)
    minmax(80px, 0.5fr)
    minmax(130px, 0.85fr)
    minmax(130px, 0.85fr)
    minmax(90px, 0.6fr)
    minmax(90px, 0.6fr)
    minmax(80px, 0.5fr)
    minmax(80px, 0.5fr)
    minmax(100px, 0.6fr)
    minmax(140px, 0.9fr);
}

.event-table-head {
  min-height: 56px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.14);
  background: rgba(0, 0, 0, 0.04);
  color: rgba(0, 0, 0, 0.62);
  font-size: 13px;
  font-weight: 800;

  span:last-child {
    text-align: right;
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
    justify-content: flex-end;
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
    text-align: right;
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
