<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { PlayerMatchTaskItem } from '@/api/matchData'
import { resubmitTeamPlayerStats, submitTeamPlayerStats } from '@/api/matchData'
import type { MatchDataAction } from '@/utils/matchDataActions'
import {
  buildTeamPlayerStatsPayload,
  cloneEditableStats,
  formatMatchScheduleText,
  isMatchDataEditable,
  resolveCaptainPermissionHint,
  resolveMyTeamSide,
  resolveSubmitDisabledReason,
  validateEditableStats,
  type EditablePlayerStatRow,
} from '@/utils/matchDataTabHelpers'
import MatchPlayerStatsPanel from './MatchPlayerStatsPanel.vue'

const props = defineProps<{
  match: PlayerMatchTaskItem
  message: string
  actions: MatchDataAction[]
  identityText: string
}>()

const emit = defineEmits<{
  action: [path?: string]
  submitted: []
}>()

const editing = ref(false)
const submitting = ref(false)
const draftStats = ref<EditablePlayerStatRow[]>([])

const myTeamSide = computed(() => resolveMyTeamSide(props.match))
const canEnterEdit = computed(() => isMatchDataEditable(props.match))
const permissionHint = computed(() => resolveCaptainPermissionHint(props.match))
const matchScheduleText = computed(() => formatMatchScheduleText(props.match))
const submitDisabledReason = computed(() => {
  if (canEnterEdit.value) return ''
  return resolveSubmitDisabledReason(props.match)
})

const editButtonLabel = computed(() => {
  if (props.match.canResubmit) return '编辑本队数据'
  return '录入本队数据'
})

const submitButtonLabel = computed(() => {
  if (props.match.canResubmit) return '重新提交比赛数据'
  return '提交比赛数据'
})

const myTeamStatsSource = computed(() => {
  if (myTeamSide.value === 'HOME') return props.match.homePlayerStatList
  if (myTeamSide.value === 'AWAY') return props.match.awayPlayerStatList
  return []
})

watch(
  () => props.match.matchId,
  () => {
    editing.value = false
    draftStats.value = []
  },
)

function startEdit() {
  if (!canEnterEdit.value) return
  draftStats.value = cloneEditableStats(myTeamStatsSource.value)
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  draftStats.value = []
}

function updateStat(playerId: string, field: keyof EditablePlayerStatRow, value: number) {
  const row = draftStats.value.find((item) => item.playerId === playerId)
  if (!row) return
  if (field === 'points' || field === 'rebounds' || field === 'assists' || field === 'steals'
    || field === 'blocks' || field === 'turnovers' || field === 'fouls') {
    row[field] = value
  }
}

async function handleSubmit() {
  if (!editing.value || submitting.value) return
  const error = validateEditableStats(draftStats.value)
  if (error) {
    ElMessage.warning(error)
    return
  }
  submitting.value = true
  try {
    const payload = buildTeamPlayerStatsPayload(props.match, draftStats.value)
    const matchId = Number(props.match.matchId)
    if (props.match.canResubmit) {
      await resubmitTeamPlayerStats(matchId, payload)
    } else {
      await submitTeamPlayerStats(matchId, payload)
    }
    ElMessage.success('比赛数据已提交，等待管理员审核')
    editing.value = false
    draftStats.value = []
    emit('submitted')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

function statCell(value?: number | null) {
  return value == null ? '-' : String(value)
}
</script>

<template>
  <div class="match-data-panel">
    <div class="match-data-panel__status-bar">
      <div class="match-data-panel__status-item">
        <span class="match-data-panel__label">比赛状态</span>
        <span class="account-status-badge account-status-badge--default">
          {{ match.matchStatusText || '-' }}
        </span>
      </div>
      <div class="match-data-panel__status-item">
        <span class="match-data-panel__label">数据状态</span>
        <span class="account-status-badge account-status-badge--default">
          {{ match.dataStatusText || match.statusText || '未提交' }}
        </span>
      </div>
      <div class="match-data-panel__status-item">
        <span class="match-data-panel__label">当前身份</span>
        <span class="account-status-badge account-status-badge--default">{{ identityText }}</span>
      </div>
      <div class="match-data-panel__status-item">
        <span class="match-data-panel__label">比赛时间</span>
        <span class="match-data-panel__time">{{ matchScheduleText }}</span>
      </div>
      <div
        v-if="match.matchStatus === 3 && match.homeScore != null && match.awayScore != null"
        class="match-data-panel__score"
      >
        正式比分 {{ statCell(match.homeScore) }} : {{ statCell(match.awayScore) }}
      </div>
    </div>

    <div class="match-data-panel__message-box">
      <p class="match-data-panel__message">{{ message }}</p>
      <p v-if="permissionHint" class="match-data-panel__hint-line">{{ permissionHint }}</p>
      <p v-if="submitDisabledReason && !canEnterEdit && !editing" class="match-data-panel__hint-line match-data-panel__hint-line--warn">
        {{ submitDisabledReason }}
      </p>
      <p v-if="match.rejectReason" class="match-data-panel__reject">
        驳回原因：{{ match.rejectReason }}
      </p>
    </div>

    <div class="match-data-panel__actions">
      <template v-if="canEnterEdit && !editing">
        <button type="button" class="account-btn-primary" @click="startEdit">
          {{ editButtonLabel }}
        </button>
      </template>

      <template v-if="editing">
        <button
          type="button"
          class="account-btn-primary"
          :disabled="submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : submitButtonLabel }}
        </button>
        <button
          type="button"
          class="account-btn-secondary"
          :disabled="submitting"
          @click="cancelEdit"
        >
          取消编辑
        </button>
      </template>

      <template v-for="action in actions" :key="action.key">
        <button
          v-if="!action.disabled && action.path"
          type="button"
          class="account-btn-secondary"
          @click="emit('action', action.path)"
        >
          {{ action.label }}
        </button>
        <button
          v-else-if="action.disabled"
          type="button"
          class="account-btn-secondary"
          disabled
        >
          {{ action.label }}
        </button>
        <p v-if="action.hint" class="match-data-panel__hint">{{ action.hint }}</p>
      </template>
    </div>

    <MatchPlayerStatsPanel
      :home-team-name="match.homeTeamNameSnapshot || match.homeTeamName"
      :away-team-name="match.awayTeamNameSnapshot || match.awayTeamName"
      :home-stats="match.homePlayerStatList"
      :away-stats="match.awayPlayerStatList"
      :my-team-side="myTeamSide"
      :editing="editing"
      :edit-stats="draftStats"
      @update-stat="updateStat"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.match-data-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.match-data-panel__status-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px 20px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 2px;
  background: linear-gradient(180deg, rgba(8, 8, 8, 0.04) 0%, rgba(255, 255, 255, 0.9) 100%);
}

.match-data-panel__status-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.match-data-panel__label {
  font-size: 12px;
  color: #777;
  font-weight: 700;
}

.match-data-panel__score {
  margin-left: auto;
  font-size: 14px;
  font-weight: 800;
  color: #c80a10;
}

.match-data-panel__message-box {
  padding: 0 2px;
}

.match-data-panel__message {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: #222;
  font-weight: 600;
}

.match-data-panel__hint-line {
  margin: 8px 0 0;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}

.match-data-panel__hint-line--warn {
  color: #ad6800;
}

.match-data-panel__time {
  font-size: 13px;
  font-weight: 700;
  color: #222;
}

.match-data-panel__reject {
  margin: 10px 0 0;
  color: #cf1322;
  font-size: 13px;
  line-height: 1.6;
}

.match-data-panel__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.match-data-panel__hint {
  width: 100%;
  margin: 0;
  font-size: 12px;
  color: #888;
}

@media (max-width: 767px) {
  .match-data-panel__score {
    margin-left: 0;
    width: 100%;
  }
}
</style>
