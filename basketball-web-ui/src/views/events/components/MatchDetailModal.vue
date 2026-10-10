<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getV4PublicMatchDetail } from '@/api/v4Public'
import type { PublicMatchDetailVo } from '@/api/types/public'
import {
  displayText,
  formatDateTime,
  matchStatusLabel,
} from '@/utils/publicDisplay'
import { formatScore } from '@/utils/v4Display'
import type { Match } from '../types/events'

const props = defineProps<{
  visible: boolean
  match: Match | null
}>()

const emit = defineEmits<{
  close: []
}>()

const { t } = useI18n()

type MatchDetailView = PublicMatchDetailVo & {
  officialStatus?: string
  resultType?: string
  homeResultLabel?: string
  awayResultLabel?: string
}

const detail = ref<MatchDetailView | null>(null)
const state = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
const errorMessage = ref('')
let reqId = 0
const scoreText = computed(() => {
  if (!detail.value) return 'VS'
  return formatScore(detail.value.homeScore, detail.value.awayScore, detail.value.officialStatus, {
    homeLabel: detail.value.homeResultLabel,
    awayLabel: detail.value.awayResultLabel,
    resultType: detail.value.resultType,
  })
})

async function loadDetail(matchId: string) {
  const current = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getV4PublicMatchDetail(matchId)
    if (current !== reqId) return
    const raw = res.data
    if (!raw) {
      detail.value = null
      state.value = 'error'
      errorMessage.value = ''
      return
    }
    detail.value = {
      matchId: raw.matchId,
      scheduledStartTime: raw.matchTime,
      matchStatus: raw.matchStatus,
      venueName: raw.venueText,
      seasonName: props.match?.season,
      stageName: raw.seriesContext?.stageName,
      homeTeamName: raw.homeTeam?.teamName || props.match?.homeTeamName,
      awayTeamName: raw.awayTeam?.teamName || props.match?.awayTeamName,
      homeScore: raw.homeScore,
      awayScore: raw.awayScore,
      officialStatus: raw.matchStatus,
      resultType: raw.resultType,
      homeResultLabel: raw.homeResultLabel,
      awayResultLabel: raw.awayResultLabel,
    } as MatchDetailView
    state.value = 'success'
  } catch (err) {
    if (current !== reqId) return
    detail.value = null
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : t('eventsPage.schedule.loadDetailFailed')
  }
}

watch(
  () => [props.visible, props.match?.matchId] as const,
  ([visible, matchId]) => {
    if (visible && matchId) {
      void loadDetail(matchId)
    } else if (!visible) {
      detail.value = null
      state.value = 'idle'
    }
  },
)
</script>

<template>
  <div v-if="visible && match" class="modal-mask" @click.self="emit('close')">
    <div class="modal-card" role="dialog" aria-modal="true" aria-labelledby="match-detail-title">
      <button type="button" class="modal-close" :aria-label="t('eventsPage.aria.close')" @click="emit('close')">×</button>
      <h3 id="match-detail-title">{{ t('eventsPage.schedule.detail') }}</h3>

      <div v-if="state === 'loading'" class="modal-state">{{ t('common.loading') }}</div>
      <div v-else-if="state === 'error'" class="modal-state is-error">{{ errorMessage || t('eventsPage.schedule.noDetail') }}</div>
      <template v-else-if="detail">
        <p class="modal-meta">
          {{ formatDateTime(detail.scheduledStartTime).full }} ·
          {{ displayText(detail.venueName, '--') }} ·
          {{ matchStatusLabel(detail.matchStatus) }}
        </p>
        <p v-if="detail.stageName || detail.seasonName" class="modal-meta">
          {{ displayText(detail.seasonName) }}
          <template v-if="detail.stageName"> · {{ detail.stageName }}</template>
        </p>
        <div class="modal-versus sport-num">
          <span>{{ displayText(detail.awayTeamName) }}</span>
          <strong>
            {{ scoreText }}
          </strong>
          <span>{{ displayText(detail.homeTeamName) }}</span>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.72);
}

.modal-card {
  position: relative;
  width: min(520px, 100%);
  padding: 28px;
  background: #111;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: #fff;
}

.modal-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: #fff;
  font-size: 24px;
  cursor: pointer;
}

.modal-meta {
  color: var(--cnpa-text-secondary);
  font-size: 13px;
}

.modal-versus {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 24px 0 0;
  font-size: 28px;
}

.modal-state {
  padding: 28px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);

  &.is-error {
    color: #e10613;
  }
}
</style>
