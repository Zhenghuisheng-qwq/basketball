<script setup lang="ts">
import type { AvailableEvent } from '@/types/accountEvents'
import type { MyEventsRoleMode } from '@/api/types/myEvents'
import { formatDateTimeShort } from '@/utils/participationMappers'

defineProps<{
  events: AvailableEvent[]
  roleMode?: MyEventsRoleMode
}>()

const emit = defineEmits<{
  action: [event: AvailableEvent, actionType?: AvailableEvent['actionType']]
}>()

function isPrimaryDisabled(event: AvailableEvent) {
  if (event.actionType === 'REMIND_CAPTAIN') {
    return !event.canRemindCaptain
  }
  if (['VIEW_REGISTRATION', 'MANAGE_REGISTRATION'].includes(event.actionType)) {
    return false
  }
  return !event.canRegister
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
</script>

<template>
  <div class="event-list-card">
    <div class="event-table-head">
      <span>赛事名称</span>
      <span>赛季</span>
      <span>报名时间</span>
      <span>比赛时间</span>
      <span>开放参赛端</span>
      <span>赛事状态</span>
      <span>我的球队</span>
      <span>球队报名</span>
      <span>我的阵容</span>
      <span>队长授权</span>
      <span>操作</span>
    </div>

    <div
      v-for="event in events"
      :key="`${event.seasonId || event.tournamentId}`"
      class="event-table-row"
    >
      <strong class="event-table-row__name">{{ event.tournamentName }}</strong>
      <span>{{ event.seasonName || '-' }}</span>
      <span>{{ signupRange(event) }}</span>
      <span>{{ matchRange(event) }}</span>
      <span>{{ event.platform }}</span>
      <span>{{ event.eventStatus || '-' }}</span>
      <span>{{ roleMode === 'NO_TEAM' ? '-' : (event.teamName || '-') }}</span>
      <span>{{ roleMode === 'NO_TEAM' ? '-' : (event.teamRegistrationStatusText || '-') }}</span>
      <span>{{ roleMode === 'NO_TEAM' ? '-' : (event.userRosterStatusText || '-') }}</span>
      <span>{{ roleMode === 'NO_TEAM' ? '-' : (event.captainGrantStatusText || '-') }}</span>
      <div class="event-table-row__action">
        <template v-if="roleMode === 'TEAM_MEMBER'">
          <button type="button" class="account-btn-secondary event-table-row__btn" @click="emit('action', event, 'VIEW_EVENT')">
            查看赛事
          </button>
          <button type="button" class="account-btn-secondary event-table-row__btn" @click="emit('action', event, 'VIEW_REGISTRATION')">
            报名状态
          </button>
          <button type="button" class="account-btn-secondary event-table-row__btn" @click="emit('action', event, 'VIEW_ROSTER')">
            查看阵容
          </button>
        </template>
        <button
          type="button"
          :class="isPrimaryDisabled(event) ? 'account-btn-secondary event-table-row__btn' : 'account-btn-primary event-table-row__btn'"
          :disabled="isPrimaryDisabled(event)"
          :title="event.disabledReason"
          @click="emit('action', event)"
        >
          {{ event.actionText }}
        </button>
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
  grid-template-columns:
    minmax(160px, 1.1fr)
    minmax(90px, 0.7fr)
    minmax(140px, 0.9fr)
    minmax(140px, 0.9fr)
    minmax(100px, 0.6fr)
    minmax(80px, 0.5fr)
    minmax(90px, 0.6fr)
    minmax(80px, 0.5fr)
    minmax(80px, 0.5fr)
    minmax(90px, 0.6fr)
    minmax(220px, 1fr);
  align-items: center;
  column-gap: 10px;
  padding: 0 16px;
  box-sizing: border-box;
  min-width: 1320px;
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

  &__name {
    font-size: 16px;
    font-weight: 800;
    color: #111;
    line-height: 1.3;
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
}
</style>
