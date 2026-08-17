<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
      message: '暂无已结束赛程',
      subtitle: '比赛结束后会显示在这里，也可切换「全部赛程」查看全部场次。',
    }
  }
  if (scheduleFilter.value === 'all') {
    return {
      message: '暂无赛程',
      subtitle: '通过报名审核并生成赛程后，比赛将显示在这里。',
    }
  }
  return {
    message: '暂无待比赛赛程',
    subtitle: '当前没有即将开始的比赛，可切换「已结束」或「全部赛程」查看历史比赛。',
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
    loadError.value = '赛程加载失败，请稍后重试'
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
  return row.seriesName || row.seasonTitle || 'CNPA 赛事'
}

function resolveSeasonLine(row: V4MyScheduleItem): string {
  const parts = [row.seasonTitle, row.divisionName || v4PlatformLabel(row.platformCode)].filter(Boolean)
  return parts.join(' · ')
}

function resolveRoundLine(row: V4MyScheduleItem): string {
  const parts = [row.stageName, row.groupName, row.roundName].filter(Boolean)
  if (row.bestOf && row.bestOf > 1 && row.seriesGameNo) {
    parts.push(`${v4BestOfLabel(row.bestOf)} · Game ${row.seriesGameNo}`)
  }
  if (parts.length) return parts.join(' · ')
  if (row.roundNo) return `常规赛第 ${row.roundNo} 轮`
  return '常规赛'
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

function isSkippedGame(row: V4MyScheduleItem): boolean {
  return row.userActionType === 'NONE' || Boolean(row.cannotSubmitReason && row.cannotSubmitReason.includes('无需进行'))
}

function gameActionText(row: V4MyScheduleItem): string {
  if (isSkippedGame(row)) return '无需进行'
  return row.userActionText || '查看提交'
}

function formatMatchDate(value?: string): string {
  if (!value) return '时间待定'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '时间待定'
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${month}月${day}日`
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
        message="完成球员认证后，可在此查看个人赛程安排。"
        subtitle="认证通过后可聚合查看全部已参加赛事的比赛安排"
        large
      />
      <button type="button" class="account-btn-primary v4-schedule-panel__empty-action" @click="goCertification">
        前往球员认证
      </button>
    </div>

    <template v-else>
      <section class="v4-schedule-overview" aria-label="赛程概览">
        <div class="v4-schedule-overview__grid">
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ upcomingCount }}</span>
            <span class="v4-schedule-overview__label">待比赛</span>
          </div>
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ pendingSubmitCount }}</span>
            <span class="v4-schedule-overview__label">待提交</span>
          </div>
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ reviewingCount }}</span>
            <span class="v4-schedule-overview__label">审核中</span>
          </div>
          <div class="v4-schedule-overview__stat">
            <span class="v4-schedule-overview__value">{{ finishedCount }}</span>
            <span class="v4-schedule-overview__label">已结束</span>
          </div>
        </div>
      </section>

      <div class="v4-schedule-panel__toolbar">
        <div class="v4-schedule-segment" role="tablist" aria-label="赛程筛选">
          <button
            type="button"
            class="v4-schedule-segment__btn"
            :class="{ 'v4-schedule-segment__btn--active': scheduleFilter === 'upcoming' }"
            @click="scheduleFilter = 'upcoming'"
          >
            待比赛
            <span class="v4-schedule-segment__count">{{ upcomingCount }}</span>
          </button>
          <button
            type="button"
            class="v4-schedule-segment__btn"
            :class="{ 'v4-schedule-segment__btn--active': scheduleFilter === 'all' }"
            @click="scheduleFilter = 'all'"
          >
            全部赛程
            <span class="v4-schedule-segment__count">{{ totalCount }}</span>
          </button>
          <button
            type="button"
            class="v4-schedule-segment__btn"
            :class="{ 'v4-schedule-segment__btn--active': scheduleFilter === 'finished' }"
            @click="scheduleFilter = 'finished'"
          >
            已结束
            <span class="v4-schedule-segment__count">{{ finishedCount }}</span>
          </button>
        </div>
      </div>

      <div v-if="loading" class="v4-schedule-skeleton" aria-hidden="true">
        <div v-for="idx in 3" :key="idx" class="v4-schedule-skeleton__card" />
      </div>

      <div v-else-if="loadError" class="v4-schedule-panel__error-box">
        <p>{{ loadError }}</p>
        <button type="button" class="account-btn-secondary" @click="loadList">重新加载</button>
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
                <p class="v4-match-card__meta">{{ resolveSeasonLine(block.rows[0]) }} · {{ block.rows[0].roundName || resolveRoundLine(block.rows[0]) }}</p>
                <p class="v4-series-card__code">{{ block.rows[0].matchSeriesCode || '系列赛' }} · {{ v4BestOfLabel(block.rows[0].bestOf) }}</p>
              </div>
              <span class="v4-match-card__status">{{ block.rows[0].matchSeriesStatusName || v4MatchSeriesStatusLabel(block.rows[0].matchSeriesStatus) }}</span>
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
                  <span v-else>未开赛</span>
                  <span>{{ v4MatchStatusLabel(row.matchStatus) }}</span>
                  <span>{{ v4SubmissionStatusLabel(row.mySubmissionStatus) }}</span>
                </div>
                <div class="v4-series-game__action">
                  <span v-if="isSkippedGame(row)" class="v4-series-game__skip">无需进行</span>
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
                <span class="v4-match-card__center-label">已结束</span>
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
              <span>比赛时间：{{ formatDateTimeShort(block.row.matchTime) || '时间待定' }}</span>
              <span v-if="block.row.matchNo">场次：{{ block.row.matchNo }}</span>
            </div>

            <div class="v4-match-card__submission-row">
              <div class="v4-submission-chip" :class="`v4-submission-chip--${submissionTone(block.row.mySubmissionStatus)}`">
                <span class="v4-submission-chip__label">本方提交</span>
                <strong>{{ v4SubmissionStatusLabel(block.row.mySubmissionStatus) }}</strong>
              </div>
              <div class="v4-submission-chip" :class="`v4-submission-chip--${submissionTone(block.row.opponentSubmissionStatus)}`">
                <span class="v4-submission-chip__label">对方提交</span>
                <strong>{{ v4SubmissionStatusLabel(block.row.opponentSubmissionStatus) }}</strong>
              </div>
            </div>

            <p v-if="block.row.latestRejectReason" class="v4-match-card__reject">
              驳回原因：{{ block.row.latestRejectReason }}
            </p>
            <p v-else-if="block.row.cannotSubmitReason && !canPrimaryAction(block.row)" class="v4-match-card__hint">
              {{ block.row.cannotSubmitReason }}
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
        赛程数据来自您正式参加的赛事。比赛数据提交权限以具体赛区、参赛队伍及当前比赛权限为准。
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
