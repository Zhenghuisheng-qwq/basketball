<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import SafeImage from '@/components/common/SafeImage.vue'
import { getAccountFileUrl } from '@/api/accountFile'
import { getV4MySchedule } from '@/api/v4PlayerEvents'
import type { V4MyScheduleItem, V4MyScheduleSummary } from '@/api/types/v4PlayerEvents'
import { useAccountStore } from '@/stores/account'
import { v4BestOfLabel, v4MatchSeriesStatusLabel, v4MatchStatusLabel, v4PlatformLabel } from '@/utils/v4Display'
import { v4SubmissionStatusLabel } from '@/utils/v4AccountEventsMappers'
import { formatDateTimeShort } from '@/utils/participationMappers'

const props = defineProps<{
  active: boolean
}>()

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const accountStore = useAccountStore()

type ScheduleFilter = 'upcoming' | 'all' | 'finished'

const loading = ref(false)
const loadError = ref('')
const rows = ref<V4MyScheduleItem[]>([])
const summary = ref<V4MyScheduleSummary>({})
const scheduleFilter = ref<ScheduleFilter>('upcoming')

const isVerified = computed(() => accountStore.isVerifiedPlayer)

const initialDivisionId = computed(() =>
  typeof route.query.divisionId === 'string' ? route.query.divisionId : '',
)
const initialOnlyPending = computed(() => route.query.onlyPendingSubmit === '1')

const divisionFilter = ref('')

const upcomingCount = computed(() => summary.value.upcomingCount ?? 0)
const totalCount = computed(() => summary.value.totalCount ?? rows.value.length)
const pendingSubmitCount = computed(() => summary.value.pendingSubmitCount ?? 0)
const reviewingCount = computed(() => summary.value.reviewingCount ?? 0)
const finishedCount = computed(() => summary.value.finishedCount ?? 0)

function isFinishedMatch(row: V4MyScheduleItem): boolean {
  return (row.matchStatus || '').toUpperCase() === 'FINISHED'
}

const displayedRows = computed(() => {
  let list = rows.value
  if (initialOnlyPending.value) {
    list = list.filter((row) => {
      const status = (row.mySubmissionStatus || '').toUpperCase()
      return !status || status === 'DRAFT' || status === 'REJECTED'
    })
  }
  if (scheduleFilter.value === 'upcoming') {
    return list.filter((row) => !isFinishedMatch(row))
  }
  if (scheduleFilter.value === 'finished') {
    return list.filter((row) => isFinishedMatch(row))
  }
  return list
})

const emptyCopy = computed(() => {
  if (scheduleFilter.value === 'finished') {
    return {
      message: t('accountEvents.schedule.emptyFinished'),
      subtitle: t('accountEvents.schedule.emptyFinishedHint'),
    }
  }
  if (scheduleFilter.value === 'all') {
    return {
      message: t('accountEvents.schedule.emptyAll'),
      subtitle: t('accountEvents.schedule.emptyAllHint'),
    }
  }
  return {
    message: t('accountEvents.schedule.emptyUpcoming'),
    subtitle: t('accountEvents.schedule.emptyUpcomingHint'),
  }
})

async function loadList() {
  if (!props.active) return
  if (!isVerified.value) {
    rows.value = []
    summary.value = {}
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4MySchedule({
      seasonId: undefined,
      divisionId: divisionFilter.value || undefined,
      includeFinished: true,
      onlyPendingSubmit: initialOnlyPending.value || undefined,
    })
    summary.value = res.data?.summary || {}
    rows.value = res.data?.items || []
  } catch {
    loadError.value = t('accountEvents.schedule.loadFailed')
    rows.value = []
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function teamInitials(name?: string | null, shortName?: string | null): string {
  const source = (shortName || name || '--').trim()
  return source.slice(0, 2).toUpperCase()
}

function resolveEventTitle(row: V4MyScheduleItem): string {
  return row.seriesName || row.seasonTitle || t('accountEvents.schedule.cnpaEvent')
}

function resolveSeasonLine(row: V4MyScheduleItem): string {
  const parts = [row.seasonTitle, row.divisionName || v4PlatformLabel(row.platformCode)].filter(Boolean)
  return parts.join(' · ')
}

function resolveRoundLine(row: V4MyScheduleItem): string {
  const parts = [row.stageName, row.groupName, roundLabel(row)].filter(Boolean)
  if (row.bestOf && row.bestOf > 1 && row.seriesGameNo) {
    parts.push(`${v4BestOfLabel(row.bestOf)} · Game ${row.seriesGameNo}`)
  }
  if (parts.length) return parts.join(' · ')
  if (row.roundNo) return t('accountEvents.common.regularRound', { round: row.roundNo })
  return t('accountEvents.common.regularSeason')
}

function roundLabel(row: V4MyScheduleItem): string {
  if (!row.roundNo || row.roundNo <= 0) return row.groupName || row.roundName || ''
  const stage = `${row.stageCode || ''} ${row.stageType || ''}`.toUpperCase()
  if (stage.includes('KNOCKOUT') || stage.includes('PLAYOFF')) {
    const teamsInRound = 1 << row.roundNo
    if (teamsInRound === 8) return t('accountEvents.schedule.quarterfinal')
    if (teamsInRound === 4) return t('accountEvents.schedule.semifinal')
    if (teamsInRound === 2) return t('accountEvents.schedule.final')
    return t('accountEvents.schedule.knockoutRound', { round: row.roundNo })
  }
  if (stage.includes('GROUP')) {
    return row.groupName
      ? t('accountEvents.schedule.groupRound', { group: row.groupName, round: row.roundNo })
      : t('accountEvents.schedule.groupRoundPlain', { round: row.roundNo })
  }
  if (row.roundNo) return t('accountEvents.common.regularRound', { round: row.roundNo })
  return row.roundName || ''
}

function seriesStatusText(row: V4MyScheduleItem): string {
  const code = (row.matchSeriesStatus || '').toUpperCase()
  if (['PENDING', 'READY', 'RUNNING', 'COMPLETED', 'CANCELLED'].includes(code)) {
    return v4MatchSeriesStatusLabel(code)
  }
  return row.matchSeriesStatusName || v4MatchSeriesStatusLabel(row.matchSeriesStatus)
}

function isSeriesSkipped(row: V4MyScheduleItem): boolean {
  if (!row.matchSeriesId || (row.matchSeriesStatus || '').toUpperCase() !== 'COMPLETED') return false
  const status = (row.matchStatus || '').toUpperCase()
  return status !== 'FINISHED' && status !== 'FORFEITED' && status !== 'CANCELLED'
}

type ScheduleBlock =
  | { kind: 'match'; key: string; row: V4MyScheduleItem }
  | { kind: 'series'; key: string; rows: V4MyScheduleItem[] }

const scheduleBlocks = computed<ScheduleBlock[]>(() => {
  const used = new Set<string>()
  const blocks: ScheduleBlock[] = []
  for (const row of displayedRows.value) {
    const seriesId = row.matchSeriesId ? String(row.matchSeriesId) : ''
    if (seriesId) {
      if (used.has(seriesId)) continue
      used.add(seriesId)
      const rows = displayedRows.value
        .filter((item) => String(item.matchSeriesId || '') === seriesId)
        .slice()
        .sort((a, b) => (a.seriesGameNo || 0) - (b.seriesGameNo || 0))
      blocks.push({ kind: 'series', key: `series-${seriesId}`, rows })
      continue
    }
    blocks.push({ kind: 'match', key: `match-${row.matchId}-${row.myEventTeamId}`, row })
  }
  return blocks
})

function gameActionText(row: V4MyScheduleItem): string {
  const matchStatus = (row.matchStatus || '').toUpperCase()
  const submission = (row.mySubmissionStatus || '').toUpperCase()
  if (matchStatus === 'CANCELLED') return t('accountEvents.schedule.actionCancelled')
  if (isSeriesSkipped(row)) return t('accountEvents.common.notNeeded')
  if (submission === 'PENDING_REVIEW') return t('accountEvents.schedule.actionViewSubmission')
  if (submission === 'APPROVED') return t('accountEvents.schedule.actionViewData')
  if (submission === 'REJECTED') return t('accountEvents.schedule.actionResubmit')
  if ((row.userActionType || '').toUpperCase() === 'SUBMIT') {
    return submission === 'DRAFT'
      ? t('accountEvents.schedule.actionContinue')
      : t('accountEvents.schedule.actionSubmit')
  }
  if ((row.userActionType || '').toUpperCase() === 'VIEW_SUBMISSION') {
    return t('accountEvents.schedule.actionViewSubmissionDetail')
  }
  return t('accountEvents.schedule.actionViewDetail')
}

function cannotSubmitText(row: V4MyScheduleItem): string {
  const matchStatus = (row.matchStatus || '').toUpperCase()
  const submission = (row.mySubmissionStatus || '').toUpperCase()
  if (!row.myEventTeamId) return t('accountEvents.schedule.reasonNotYourTeam')
  if (row.publishStatus && row.publishStatus.toUpperCase() !== 'PUBLISHED') return t('accountEvents.schedule.reasonUnpublished')
  if (matchStatus === 'CANCELLED') return t('accountEvents.schedule.reasonCancelled')
  if (isSeriesSkipped(row)) return t('accountEvents.schedule.reasonSeriesDecided')
  if (submission === 'PENDING_REVIEW') return t('accountEvents.schedule.reasonInReview')
  if (submission === 'APPROVED') return t('accountEvents.schedule.reasonApproved')
  if (matchStatus === 'PLAYING') return t('accountEvents.schedule.reasonPlaying')
  if (matchStatus && !['FINISHED', 'FORFEITED', 'CANCELLED', 'PLAYING'].includes(matchStatus) && !row.canSubmit) {
    return t('accountEvents.schedule.reasonNotEnded')
  }
  return row.cannotSubmitReason || ''
}

function formatMatchDate(value?: string): string {
  if (!value) return t('accountEvents.common.timeTbd')
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return t('accountEvents.common.timeTbd')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return t('accountEvents.schedule.monthDay', { month, day })
}

function formatMatchClock(value?: string): string {
  if (!value) return '--:--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--:--'
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${hour}:${minute}`
}

function showScore(row: V4MyScheduleItem): boolean {
  return row.matchStatus === 'FINISHED' || row.matchStatus === 'FORFEITED'
}

function formatCenterScore(row: V4MyScheduleItem): string {
  const home = row.homeScore ?? 0
  const away = row.awayScore ?? 0
  return `${home} : ${away}`
}

function matchStatusTone(status?: string): 'default' | 'live' | 'finished' | 'muted' {
  switch ((status || '').toUpperCase()) {
    case 'PLAYING':
      return 'live'
    case 'FINISHED':
    case 'FORFEITED':
      return 'finished'
    case 'CANCELLED':
      return 'muted'
    default:
      return 'default'
  }
}

function submissionTone(status?: string | null): 'pending' | 'review' | 'approved' | 'rejected' | 'muted' {
  switch ((status || '').toUpperCase()) {
    case 'PENDING_REVIEW':
      return 'review'
    case 'APPROVED':
      return 'approved'
    case 'REJECTED':
      return 'rejected'
    case 'DRAFT':
    case '':
      return 'pending'
    default:
      return 'muted'
  }
}

function canPrimaryAction(row: V4MyScheduleItem): boolean {
  return Boolean(row.canSubmitData ?? row.canSubmit)
}

function goSubmit(row: V4MyScheduleItem) {
  if (!row.matchId || !row.myEventTeamId) return
  router.push({
    name: 'AccountV4MatchSubmission',
    params: { matchId: row.matchId },
    query: { submitEventTeamId: row.myEventTeamId },
  })
}

function goCertification() {
  router.push('/account/player-identity')
}

function handlePrimaryAction(row: V4MyScheduleItem) {
  goSubmit(row)
}

watch(
  () => props.active,
  (active) => {
    if (active) {
      divisionFilter.value = initialDivisionId.value
      void loadList()
    }
  },
  { immediate: true },
)

watch(
  () => [route.query.divisionId, route.query.onlyPendingSubmit],
  () => {
    if (!props.active) return
    divisionFilter.value = initialDivisionId.value
    void loadList()
  },
)
</script>

<template>
  <div class="v4-schedule-panel">
    <div v-if="!isVerified" class="v4-schedule-panel__empty-wrap">
      <AccountEmptyState
        :message="t('accountEvents.schedule.verifyMessage')"
        :subtitle="t('accountEvents.schedule.verifySubtitle')"
        large
      />
      <button type="button" class="account-btn-primary v4-schedule-panel__empty-action" @click="goCertification">
        {{ t('accountEvents.schedule.goVerify') }}
      </button>
    </div>

    <template v-else>
      <section class="v4-schedule-overview" :aria-label="t('accountEvents.schedule.overviewAria')">
        <div class="v4-schedule-overview__grid">
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ upcomingCount }}</span>
            <span class="v4-schedule-overview__label">{{ t('accountEvents.schedule.upcoming') }}</span>
          </div>
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ pendingSubmitCount }}</span>
            <span class="v4-schedule-overview__label">{{ t('accountEvents.schedule.pendingSubmit') }}</span>
          </div>
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ reviewingCount }}</span>
            <span class="v4-schedule-overview__label">{{ t('accountEvents.schedule.reviewing') }}</span>
          </div>
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ finishedCount }}</span>
            <span class="v4-schedule-overview__label">{{ t('accountEvents.schedule.finished') }}</span>
          </div>
        </div>
      </section>

      <div class="v4-schedule-panel__toolbar">
        <div class="v4-schedule-segment" role="tablist" :aria-label="t('accountEvents.schedule.filterAria')">
          <button
            type="button"
            class="v4-schedule-segment__btn"
            :class="{ 'v4-schedule-segment__btn--active': scheduleFilter === 'upcoming' }"
            @click="scheduleFilter = 'upcoming'"
          >
            {{ t('accountEvents.schedule.upcoming') }}
            <span class="v4-schedule-segment__count">{{ upcomingCount }}</span>
          </button>
          <button
            type="button"
            class="v4-schedule-segment__btn"
            :class="{ 'v4-schedule-segment__btn--active': scheduleFilter === 'all' }"
            @click="scheduleFilter = 'all'"
          >
            {{ t('accountEvents.schedule.all') }}
            <span class="v4-schedule-segment__count">{{ totalCount }}</span>
          </button>
          <button
            type="button"
            class="v4-schedule-segment__btn"
            :class="{ 'v4-schedule-segment__btn--active': scheduleFilter === 'finished' }"
            @click="scheduleFilter = 'finished'"
          >
            {{ t('accountEvents.schedule.finished') }}
            <span class="v4-schedule-segment__count">{{ finishedCount }}</span>
          </button>
        </div>
      </div>

      <div v-if="loading" class="v4-schedule-skeleton" aria-hidden="true">
        <div v-for="idx in 3" :key="idx" class="v4-schedule-skeleton__card" />
      </div>

      <div v-else-if="loadError" class="v4-schedule-panel__error-box">
        <p>{{ loadError }}</p>
        <button type="button" class="account-btn-secondary" @click="loadList">{{ t('accountEvents.common.reload') }}</button>
      </div>

      <AccountEmptyState
        v-else-if="!displayedRows.length"
        :message="emptyCopy.message"
        :subtitle="emptyCopy.subtitle"
        large
      />

      <div v-else class="v4-schedule-panel__cards">
        <template v-for="block in scheduleBlocks" :key="block.key">
          <section v-if="block.kind === 'series'" class="v4-series-card">
            <header class="v4-series-card__head">
              <div>
                <p class="v4-match-card__series">{{ resolveEventTitle(block.rows[0]) }}</p>
                <p class="v4-match-card__meta">{{ resolveSeasonLine(block.rows[0]) }} · {{ roundLabel(block.rows[0]) || resolveRoundLine(block.rows[0]) }}</p>
                <p class="v4-series-card__code">{{ block.rows[0].matchSeriesCode || t('accountEvents.common.series') }} · {{ v4BestOfLabel(block.rows[0].bestOf) }}</p>
              </div>
              <span class="v4-match-card__status">{{ seriesStatusText(block.rows[0]) }}</span>
            </header>
            <div class="v4-series-card__score">
              <strong>{{ block.rows[0].homeTeamName }}</strong>
              <em>{{ block.rows[0].seriesHomeWinCount ?? 0 }} : {{ block.rows[0].seriesAwayWinCount ?? 0 }}</em>
              <strong>{{ block.rows[0].awayTeamName }}</strong>
            </div>
            <div class="v4-series-card__games">
              <article v-for="row in block.rows" :key="`${row.matchId}-${row.myEventTeamId}`" class="v4-series-game">
                <div class="v4-series-game__meta">
                  <strong>Game {{ row.seriesGameNo }}</strong>
                  <span>{{ row.homeTeamName }} vs {{ row.awayTeamName }}</span>
                  <span v-if="showScore(row)">{{ formatCenterScore(row) }}</span>
                  <span v-else>{{ t('accountEvents.common.notStarted') }}</span>
                  <span>{{ v4MatchStatusLabel(row.matchStatus) }}</span>
                  <span>{{ v4SubmissionStatusLabel(row.mySubmissionStatus) }}</span>
                </div>
                <div class="v4-series-game__action">
                  <span v-if="isSeriesSkipped(row)" class="v4-series-game__skip">{{ t('accountEvents.common.notNeeded') }}</span>
                  <button
                    v-else-if="canPrimaryAction(row)"
                    type="button"
                    class="account-btn-primary"
                    @click="goSubmit(row)"
                  >
                    {{ gameActionText(row) }}
                  </button>
                  <button v-else type="button" class="account-btn-secondary" @click="goSubmit(row)">
                    {{ gameActionText(row) }}
                  </button>
                </div>
              </article>
            </div>
          </section>

        <article
          v-else-if="block.kind === 'match'"
          class="v4-match-card"
        >
          <header class="v4-match-card__head">
            <div class="v4-match-card__event">
              <p class="v4-match-card__series">{{ resolveEventTitle(block.row) }}</p>
              <p class="v4-match-card__meta">{{ resolveSeasonLine(block.row) }}</p>
              <p class="v4-match-card__round">{{ resolveRoundLine(block.row) }}</p>
            </div>
            <span
              class="v4-match-card__status"
              :class="`v4-match-card__status--${matchStatusTone(block.row.matchStatus)}`"
            >
              {{ v4MatchStatusLabel(block.row.matchStatus) }}
            </span>
          </header>

          <div class="v4-match-card__versus">
            <div class="v4-match-card__team v4-match-card__team--home">
              <SafeImage
                :src="block.row.homeTeamLogoFileId ? getAccountFileUrl(block.row.homeTeamLogoFileId) : ''"
                :alt="block.row.homeTeamName || ''"
                :initials="teamInitials(block.row.homeTeamName, block.row.homeTeamShortName)"
                :size="52"
                class="v4-match-card__logo"
              />
              <div class="v4-match-card__team-text">
                <strong :title="block.row.homeTeamName">{{ block.row.homeTeamName }}</strong>
                <span v-if="block.row.homeTeamShortName">{{ block.row.homeTeamShortName }}</span>
              </div>
            </div>

            <div class="v4-match-card__center">
              <template v-if="showScore(block.row)">
                <p class="v4-match-card__score">{{ formatCenterScore(block.row) }}</p>
                <span class="v4-match-card__center-label">{{ t('accountEvents.common.finished') }}</span>
              </template>
              <template v-else>
                <p class="v4-match-card__date">{{ formatMatchDate(block.row.matchTime) }}</p>
                <p class="v4-match-card__clock">{{ formatMatchClock(block.row.matchTime) }}</p>
                <span class="v4-match-card__vs">VS</span>
              </template>
            </div>

            <div class="v4-match-card__team v4-match-card__team--away">
              <SafeImage
                :src="block.row.awayTeamLogoFileId ? getAccountFileUrl(block.row.awayTeamLogoFileId) : ''"
                :alt="block.row.awayTeamName || ''"
                :initials="teamInitials(block.row.awayTeamName, block.row.awayTeamShortName)"
                :size="52"
                class="v4-match-card__logo"
              />
              <div class="v4-match-card__team-text">
                <strong :title="block.row.awayTeamName">{{ block.row.awayTeamName }}</strong>
                <span v-if="block.row.awayTeamShortName">{{ block.row.awayTeamShortName }}</span>
              </div>
            </div>
          </div>

          <div class="v4-match-card__footer">
            <div class="v4-match-card__info">
              <span>{{ t('accountEvents.schedule.matchTime', { time: formatDateTimeShort(block.row.matchTime) || t('accountEvents.common.timeTbd') }) }}</span>
              <span v-if="block.row.matchNo">{{ t('accountEvents.schedule.matchNo', { no: block.row.matchNo }) }}</span>
            </div>

            <div class="v4-match-card__submission-row">
              <div class="v4-submission-chip" :class="`v4-submission-chip--${submissionTone(block.row.mySubmissionStatus)}`">
                <span class="v4-submission-chip__label">{{ t('accountEvents.schedule.mySubmission') }}</span>
                <strong>{{ v4SubmissionStatusLabel(block.row.mySubmissionStatus) }}</strong>
              </div>
              <div class="v4-submission-chip" :class="`v4-submission-chip--${submissionTone(block.row.opponentSubmissionStatus)}`">
                <span class="v4-submission-chip__label">{{ t('accountEvents.schedule.opponentSubmission') }}</span>
                <strong>{{ v4SubmissionStatusLabel(block.row.opponentSubmissionStatus) }}</strong>
              </div>
            </div>

            <p v-if="block.row.latestRejectReason" class="v4-match-card__reject">
              {{ t('accountEvents.common.rejectReason', { reason: block.row.latestRejectReason }) }}
            </p>
            <p v-else-if="cannotSubmitText(block.row) && !canPrimaryAction(block.row)" class="v4-match-card__hint">
              {{ cannotSubmitText(block.row) }}
            </p>

            <div class="v4-match-card__actions">
              <button
                v-if="canPrimaryAction(block.row)"
                type="button"
                class="account-btn-primary"
                @click="handlePrimaryAction(block.row)"
              >
                {{ gameActionText(block.row) }}
              </button>
              <button
                v-else
                type="button"
                class="account-btn-secondary"
                @click="goSubmit(block.row)"
              >
                {{ gameActionText(block.row) }}
              </button>
            </div>
          </div>
        </article>
        </template>
      </div>
      <p class="v4-schedule-panel__footnote">
        {{ t('accountEvents.footer.schedule') }}
      </p>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.v4-schedule-panel {
  width: 100%;
  min-width: 0;

  &__empty-wrap {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  &__empty-action {
    min-width: 140px;
  }

  &__toolbar {
    margin-bottom: 18px;
  }

  &__error-box {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px 18px;
    border: 1px solid rgba(180, 35, 24, 0.25);
    background: rgba(180, 35, 24, 0.06);
    color: #8f1d13;
    font-size: 14px;
  }

  &__cards {
    display: flex;
    flex-direction: column;
    gap: 18px;
  }

  &__footnote {
    margin: 18px 0 0;
    font-size: 13px;
    color: #666;
    line-height: 1.7;
  }
}

.v4-series-card {
  padding: 18px 20px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);

  &__head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
  }

  &__code {
    margin-top: 4px;
    font-size: 13px;
    color: #666;
  }

  &__score {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 16px;
    margin: 8px 0 16px;
    font-size: 18px;

    em {
      font-style: normal;
      font-size: 28px;
      font-weight: 700;
    }
  }

  &__games {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }
}

.v4-series-game {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid rgba(0, 0, 0, 0.06);

  &__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 14px;
    font-size: 13px;
  }

  &__skip {
    color: #888;
    font-size: 13px;
  }
}

.v4-schedule-overview {
  margin-bottom: 18px;
  padding: 18px 22px;
  background:
    linear-gradient(135deg, rgba(18, 18, 18, 0.96), rgba(36, 8, 8, 0.92)),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.03) 0 2px, transparent 2px 8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05);

  &__grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
  }

  &__stat {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  &__value {
    font-size: 28px;
    line-height: 1;
    font-weight: 900;
    color: #fff;
  }

  &__label {
    font-size: 13px;
    color: rgba(255, 255, 255, 0.72);
    letter-spacing: 0.04em;
  }
}

.v4-schedule-segment {
  display: inline-flex;
  padding: 4px;
  background: rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(0, 0, 0, 0.12);
  gap: 4px;

  &__btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-height: 40px;
    padding: 0 18px;
    border: 0;
    background: transparent;
    color: #444;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.2s ease, color 0.2s ease;

    &--active {
      background: #111;
      color: #fff;
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
    }
  }

  &__count {
    font-size: 12px;
    opacity: 0.82;
  }
}

.v4-schedule-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;

  &__card {
    height: 220px;
    background: linear-gradient(90deg, rgba(0, 0, 0, 0.05) 25%, rgba(0, 0, 0, 0.08) 37%, rgba(0, 0, 0, 0.05) 63%);
    background-size: 400% 100%;
    animation: v4-schedule-shimmer 1.4s ease infinite;
  }
}

@keyframes v4-schedule-shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: 0 0; }
}

.v4-match-card {
  padding: 22px 24px;
  background: rgba(255, 255, 255, 0.42);
  border: 1px solid rgba(0, 0, 0, 0.12);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.04);

  &__head {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;
  }

  &__series {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 900;
    color: #111;
  }

  &__meta,
  &__round {
    margin: 0;
    font-size: 13px;
    color: #666;
    line-height: 1.5;
  }

  &__status {
    flex-shrink: 0;
    align-self: flex-start;
    padding: 6px 12px;
    border-radius: 999px;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.04em;

    &--default {
      background: rgba(214, 138, 22, 0.14);
      color: #9a5d00;
    }

    &--live {
      background: rgba(180, 35, 24, 0.14);
      color: #b42318;
    }

    &--finished {
      background: rgba(0, 0, 0, 0.08);
      color: #555;
    }

    &--muted {
      background: rgba(0, 0, 0, 0.05);
      color: #888;
    }
  }

  &__versus {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
    gap: 18px;
    align-items: center;
    margin-bottom: 18px;
  }

  &__team {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;

    &--away {
      flex-direction: row-reverse;
      text-align: right;
    }
  }

  &__team-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    strong {
      font-size: 16px;
      font-weight: 800;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    span {
      font-size: 12px;
      color: #777;
      letter-spacing: 0.06em;
    }
  }

  &__logo {
    flex-shrink: 0;
  }

  &__center {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-width: 96px;
    text-align: center;
  }

  &__score {
    margin: 0;
    font-size: 34px;
    line-height: 1;
    font-weight: 900;
    color: #111;
  }

  &__date {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: #333;
  }

  &__clock {
    margin: 2px 0 6px;
    font-size: 24px;
    line-height: 1;
    font-weight: 900;
    color: #111;
  }

  &__vs {
    font-size: 13px;
    font-weight: 900;
    letter-spacing: 0.18em;
    color: #b42318;
  }

  &__center-label {
    margin-top: 4px;
    font-size: 12px;
    color: #777;
  }

  &__footer {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid rgba(0, 0, 0, 0.08);
  }

  &__info {
    display: flex;
    flex-wrap: wrap;
    gap: 8px 16px;
    font-size: 13px;
    color: #666;
  }

  &__submission-row {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  &__reject,
  &__hint {
    margin: 0;
    font-size: 13px;
    line-height: 1.6;
  }

  &__reject {
    color: #8f1d13;
  }

  &__hint {
    color: #777;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
}

.v4-submission-chip {
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.55);

  &__label {
    display: block;
    margin-bottom: 4px;
    font-size: 12px;
    color: #777;
  }

  strong {
    font-size: 14px;
    font-weight: 800;
  }

  &--pending {
    border-color: rgba(180, 35, 24, 0.22);
    strong { color: #b42318; }
  }

  &--review {
    border-color: rgba(214, 138, 22, 0.28);
    strong { color: #b56a00; }
  }

  &--approved {
    border-color: rgba(21, 112, 57, 0.22);
    strong { color: #157039; }
  }

  &--rejected {
    border-color: rgba(120, 16, 16, 0.24);
    strong { color: #781010; }
  }

  &--muted strong {
    color: #666;
  }
}

@media (max-width: 900px) {
  .v4-schedule-overview__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .v4-match-card__versus {
    grid-template-columns: 1fr;
  }

  .v4-match-card__team,
  .v4-match-card__team--away {
    flex-direction: row;
    text-align: left;
  }

  .v4-match-card__submission-row {
    grid-template-columns: 1fr;
  }
}
</style>
