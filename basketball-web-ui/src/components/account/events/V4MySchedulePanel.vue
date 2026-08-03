<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import EventStatusBadge from '@/components/account/events/EventStatusBadge.vue'
import SafeImage from '@/components/common/SafeImage.vue'
import { getAccountFileUrl } from '@/api/accountFile'
import { getV4MySchedule } from '@/api/v4PlayerEvents'
import type { V4MyScheduleItem } from '@/api/types/v4PlayerEvents'
import { useAccountStore } from '@/stores/account'
import { v4MatchStatusLabel, v4PlatformLabel, formatScore } from '@/utils/v4Display'
import { v4SubmissionStatusLabel } from '@/utils/v4AccountEventsMappers'
import { formatDateTimeShort } from '@/utils/participationMappers'

const props = defineProps<{
  active: boolean
}>()

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const loading = ref(false)
const loadError = ref('')
const rows = ref<V4MyScheduleItem[]>([])
const includeFinished = ref(false)

const isVerified = computed(() => accountStore.isVerifiedPlayer)

const initialDivisionId = computed(() =>
  typeof route.query.divisionId === 'string' ? route.query.divisionId : '',
)
const initialOnlyPending = computed(() => route.query.onlyPendingSubmit === '1')

const divisionFilter = ref('')
const seasonFilter = ref('')

async function loadList() {
  if (!props.active) return
  if (!isVerified.value) {
    rows.value = []
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getV4MySchedule({
      seasonId: seasonFilter.value || undefined,
      divisionId: divisionFilter.value || undefined,
      includeFinished: includeFinished.value,
      onlyPendingSubmit: initialOnlyPending.value || undefined,
    })
    rows.value = res.data || []
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    rows.value = []
  } finally {
    loading.value = false
  }
}

function resolveMyTeamName(row: V4MyScheduleItem): string {
  if (row.myEventTeamId === row.homeEventTeamId) return row.homeTeamName || '-'
  if (row.myEventTeamId === row.awayEventTeamId) return row.awayTeamName || '-'
  return '-'
}

function resolveOpponentName(row: V4MyScheduleItem): string {
  if (row.myEventTeamId === row.homeEventTeamId) return row.awayTeamName || '-'
  if (row.myEventTeamId === row.awayEventTeamId) return row.homeTeamName || '-'
  return '-'
}

function resolveMyScore(row: V4MyScheduleItem): string {
  if (row.myEventTeamId === row.homeEventTeamId) {
    return formatScore(row.homeScore, row.awayScore, row.matchStatus)
  }
  if (row.myEventTeamId === row.awayEventTeamId) {
    return formatScore(row.awayScore, row.homeScore, row.matchStatus)
  }
  return formatScore(row.homeScore, row.awayScore, row.matchStatus)
}

function canSubmit(row: V4MyScheduleItem): boolean {
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

watch(includeFinished, () => {
  void loadList()
})
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
      <div class="v4-schedule-panel__toolbar">
        <div class="v4-schedule-panel__tabs">
          <button
            type="button"
            class="v4-schedule-panel__tab"
            :class="{ 'v4-schedule-panel__tab--active': !includeFinished }"
            @click="includeFinished = false"
          >
            未比赛
          </button>
          <button
            type="button"
            class="v4-schedule-panel__tab"
            :class="{ 'v4-schedule-panel__tab--active': includeFinished }"
            @click="includeFinished = true"
          >
            全部赛程
          </button>
        </div>
        <p class="v4-schedule-panel__hint">按比赛时间从近到远排序；对方审核中不影响本方提交入口。</p>
      </div>

      <p v-if="loadError" class="v4-schedule-panel__error">{{ loadError }}</p>
      <p v-else-if="loading" class="v4-schedule-panel__loading">加载中…</p>

      <AccountEmptyState
        v-else-if="!rows.length"
        :message="includeFinished ? '暂无赛程记录' : '暂无待进行比赛'"
        subtitle="报名审核通过并发布赛程后，比赛将显示在这里"
        large
      />

      <div v-else class="v4-schedule-panel__cards">
        <article v-for="row in rows" :key="`${row.matchId}-${row.myEventTeamId}`" class="v4-schedule-card">
          <div class="v4-schedule-card__head">
            <div>
              <p class="v4-schedule-card__season">{{ row.seasonTitle }}</p>
              <p class="v4-schedule-card__meta">
                {{ v4PlatformLabel(row.platformCode) }} · {{ row.divisionName }}
                <template v-if="row.roundNo"> · 第 {{ row.roundNo }} 轮</template>
              </p>
            </div>
            <EventStatusBadge
              :text="v4MatchStatusLabel(row.matchStatus)"
              :finished="row.matchStatus === 'FINISHED'"
              :pending="row.mySubmissionStatus === 'PENDING_REVIEW'"
            />
          </div>

          <div class="v4-schedule-card__versus">
            <div class="v4-schedule-card__team">
              <SafeImage :src="row.homeTeamLogoFileId ? getAccountFileUrl(row.homeTeamLogoFileId) : ''" :alt="row.homeTeamName || ''" :initials="(row.homeTeamName || '--').slice(0, 2)" :size="40" class="v4-schedule-card__logo" />
              <span>{{ row.homeTeamName }}</span>
            </div>
            <div class="v4-schedule-card__score">{{ resolveMyScore(row) }}</div>
            <div class="v4-schedule-card__team">
              <SafeImage :src="row.awayTeamLogoFileId ? getAccountFileUrl(row.awayTeamLogoFileId) : ''" :alt="row.awayTeamName || ''" :initials="(row.awayTeamName || '--').slice(0, 2)" :size="40" class="v4-schedule-card__logo" />
              <span>{{ row.awayTeamName }}</span>
            </div>
          </div>

          <div class="v4-schedule-card__info">
            <span>我的球队：{{ resolveMyTeamName(row) }}</span>
            <span>对手：{{ resolveOpponentName(row) }}</span>
            <span>时间：{{ formatDateTimeShort(row.matchTime) }}</span>
            <span>本方数据：{{ v4SubmissionStatusLabel(row.mySubmissionStatus) }}</span>
            <span v-if="row.opponentSubmissionStatus">对方数据：{{ v4SubmissionStatusLabel(row.opponentSubmissionStatus) }}</span>
          </div>

          <div class="v4-schedule-card__actions">
            <button
              v-if="canSubmit(row)"
              type="button"
              class="account-btn-primary"
              @click="goSubmit(row)"
            >
              提交数据
            </button>
            <button
              v-else-if="row.mySubmissionStatus"
              type="button"
              class="account-btn-secondary"
              @click="goSubmit(row)"
            >
              查看提交
            </button>
            <span v-if="!canSubmit(row) && row.cannotSubmitReason" class="v4-schedule-card__reason">
              {{ row.cannotSubmitReason }}
            </span>
          </div>
        </article>
      </div>
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
    margin-bottom: 16px;
  }

  &__tabs {
    display: flex;
    gap: 8px;
    margin-bottom: 8px;
  }

  &__tab {
    height: 36px;
    padding: 0 16px;
    border: 1px solid rgba(0, 0, 0, 0.18);
    background: #fff;
    cursor: pointer;

    &--active {
      border-color: #b42318;
      color: #b42318;
      font-weight: 700;
    }
  }

  &__hint {
    margin: 0;
    font-size: 13px;
    color: #666;
  }

  &__error {
    color: #b42318;
    font-size: 13px;
  }

  &__loading {
    color: #696969;
    font-size: 14px;
  }

  &__cards {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

.v4-schedule-card {
  padding: 20px 24px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.35);
  border-radius: 4px;

  &__head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 16px;
  }

  &__season {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 800;
  }

  &__meta {
    margin: 0;
    font-size: 13px;
    color: #666;
  }

  &__versus {
    display: grid;
    grid-template-columns: 1fr auto 1fr;
    gap: 12px;
    align-items: center;
    margin-bottom: 14px;
  }

  &__team {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 700;
    min-width: 0;

    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &:last-child {
      flex-direction: row-reverse;
      text-align: right;
    }
  }

  &__logo {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    flex-shrink: 0;
  }

  &__score {
    font-size: 22px;
    font-weight: 900;
    text-align: center;
  }

  &__info {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 16px;
    font-size: 13px;
    color: #555;
    margin-bottom: 14px;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 10px;
  }

  &__reason {
    font-size: 13px;
    color: #888;
  }
}

@media (max-width: 767px) {
  .v4-schedule-card__versus {
    grid-template-columns: 1fr;
    text-align: center;
  }

  .v4-schedule-card__team,
  .v4-schedule-card__team:last-child {
    flex-direction: column;
    text-align: center;
  }
}
</style>
