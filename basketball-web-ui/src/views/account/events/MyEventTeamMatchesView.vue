<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import { getEventTeamDetail, getEventTeamMatches } from '@/api/eventParticipation'
import type { EventTeamDetailDto } from '@/api/types/eventParticipation'
import type { PlayerMatchTaskItem } from '@/api/matchData'
import { useEventTeamStore } from '@/stores/eventTeam'
import { useAccountStore } from '@/stores/account'
import { buildMatchDetailRoute } from '@/utils/participationMappers'
import { resolveMatchStatusDisplayText, shouldDisplayMatchAsFinished } from '@/utils/matchDataTabHelpers'

const props = defineProps<{
  eventTeamId?: string
  legacySeasonId?: string
}>()

const route = useRoute()
const router = useRouter()
const eventTeamStore = useEventTeamStore()
const accountStore = useAccountStore()

const loading = ref(false)
const detail = ref<EventTeamDetailDto | null>(null)
const rows = ref<PlayerMatchTaskItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const matchStatus = ref<number | ''>('')
const dataStatus = ref<number | ''>('')
const beginDate = ref('')
const endDate = ref('')

const resolvedEventTeamId = computed(() =>
  props.eventTeamId
  || (route.params.eventTeamId as string | undefined)
  || eventTeamStore.eventTeamId
  || '')

const isCaptain = computed(() => Boolean(detail.value?.currentUserIsCaptain ?? eventTeamStore.isCaptain))

const breadcrumbItems = computed(() => [
  { label: '首页', to: '/home' },
  { label: '个人中心', to: '/account/overview' },
  { label: '我的赛事', to: '/account/my-events?tab=joined' },
  { label: detail.value?.tournamentName || detail.value?.seasonName || '我的比赛' },
])

function formatMyDataStatus(row: PlayerMatchTaskItem): string {
  const myText = row.mySubmissionStatusText || row.submissionStatusText || row.dataStatusText
  if (myText) return myText
  return row.statusText || '未提交'
}

function formatOpponentDataStatus(row: PlayerMatchTaskItem): string {
  return row.opponentSubmissionStatusText || '-'
}

function formatScore(row: PlayerMatchTaskItem) {
  if (!shouldDisplayMatchAsFinished(row)) return '-'
  if (row.homeScore == null || row.awayScore == null) return '-'
  return `${row.homeScore} : ${row.awayScore}`
}

async function ensureContext() {
  if (!accountStore.isVerifiedPlayer) {
    ElMessage.warning('请先完成球员身份认证')
    router.replace('/account/player-identity')
    return false
  }
  let eventTeamId = resolvedEventTeamId.value
  if (!eventTeamId && props.legacySeasonId) {
    await eventTeamStore.ensureLoaded(props.legacySeasonId)
    eventTeamId = eventTeamStore.eventTeamId
  }
  if (!eventTeamId) {
    ElMessage.warning('未找到赛事球队')
    router.replace('/account/my-events?tab=joined')
    return false
  }
  const detailRes = await getEventTeamDetail(eventTeamId)
  detail.value = detailRes.data ?? null
  if (props.legacySeasonId && detail.value?.seasonId) {
    await eventTeamStore.ensureLoaded(detail.value.seasonId)
  }
  return true
}

async function loadList() {
  const eventTeamId = resolvedEventTeamId.value || detail.value?.eventTeamId
  if (!eventTeamId) return
  loading.value = true
  try {
    const res = await getEventTeamMatches(eventTeamId, {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      matchStatus: matchStatus.value === '' ? undefined : matchStatus.value,
      dataStatus: dataStatus.value === '' ? undefined : dataStatus.value,
      beginTime: beginDate.value ? `${beginDate.value} 00:00:00` : undefined,
      endTime: endDate.value ? `${endDate.value} 23:59:59` : undefined,
    })
    rows.value = res.data?.records || []
    total.value = Number(res.data?.total ?? 0)
    if (res.data?.current) {
      pageNum.value = Number(res.data.current)
    }
    if (res.data?.size) {
      pageSize.value = Number(res.data.size)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handlePageChange(page: number) {
  pageNum.value = page
  void loadList()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  pageNum.value = 1
  void loadList()
}

function handleSearch() {
  pageNum.value = 1
  void loadList()
}

function handleResetFilters() {
  matchStatus.value = ''
  dataStatus.value = ''
  beginDate.value = ''
  endDate.value = ''
  pageNum.value = 1
  void loadList()
}

function goMatchDetail(row: PlayerMatchTaskItem) {
  const eventTeamId = resolvedEventTeamId.value || detail.value?.eventTeamId || row.eventTeamId
  if (!eventTeamId || !row.matchId) return
  router.push(buildMatchDetailRoute(eventTeamId, row.matchId))
}

onMounted(async () => {
  loading.value = true
  try {
    if (!(await ensureContext())) return
    await loadList()
  } finally {
    loading.value = false
  }
})

watch(() => resolvedEventTeamId.value, () => {
  if (resolvedEventTeamId.value) {
    pageNum.value = 1
    void loadList()
  }
})
</script>

<template>
  <div class="account-view my-event-matches-view">
    <AccountPageHeader
      zh-title="我的比赛"
      en-title="MY MATCHES"
      :breadcrumb-items="breadcrumbItems"
    />

    <EventTeamHeroStrip
      :tournament-name="detail?.tournamentName || detail?.seasonName || '赛事'"
      :team-code="detail?.eventTeamNo || detail?.teamName || '我的球队'"
      description="查看本届赛事球队全部比赛安排"
      :status-text="`${isCaptain ? '队长' : '队员'} · 赛程已发布`"
      status-type="success"
    />

    <AccountPanel title="比赛列表">
      <div class="my-event-matches-view__filters">
        <label>
          比赛状态
          <select v-model="matchStatus">
            <option value="">全部</option>
            <option :value="1">未开始</option>
            <option :value="3">已结束</option>
            <option :value="5">已取消</option>
          </select>
        </label>
        <label>
          数据状态
          <select v-model="dataStatus">
            <option value="">全部</option>
            <option :value="0">未提交</option>
            <option :value="1">审核中</option>
            <option :value="2">已通过</option>
            <option :value="4">已驳回</option>
          </select>
        </label>
        <label>
          开始日期
          <input v-model="beginDate" type="date" />
        </label>
        <label>
          结束日期
          <input v-model="endDate" type="date" />
        </label>
        <button type="button" class="account-btn-primary" @click="handleSearch">筛选</button>
        <button type="button" class="account-btn-secondary" @click="handleResetFilters">重置</button>
      </div>

      <div v-loading="loading" class="my-event-matches-view__table-wrap">
        <AccountEmptyState
          v-if="!rows.length && !loading"
          message="暂无比赛记录"
          subtitle="当前赛事赛程尚未发布，发布后将在此展示。"
          large
        />

        <table v-else class="my-event-matches-view__table">
          <thead>
            <tr>
              <th>轮次</th>
              <th>日期</th>
              <th>星期</th>
              <th>时间</th>
              <th>主队</th>
              <th>客队</th>
              <th>对手</th>
              <th>比赛状态</th>
              <th>比分</th>
              <th>我方数据</th>
              <th>对方数据</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.matchId">
              <td>{{ row.roundNo ? `第 ${row.roundNo} 轮` : '-' }}</td>
              <td>{{ row.matchDate || '-' }}</td>
              <td>{{ row.weekday || '-' }}</td>
              <td>{{ row.matchTime || '-' }}</td>
              <td>{{ row.homeTeamNameSnapshot || row.homeTeamName || '-' }}</td>
              <td>{{ row.awayTeamNameSnapshot || row.awayTeamName || '-' }}</td>
              <td>{{ row.opponentTeamNameSnapshot || '-' }}</td>
              <td>{{ resolveMatchStatusDisplayText(row) }}</td>
              <td>{{ formatScore(row) }}</td>
              <td>{{ formatMyDataStatus(row) }}</td>
              <td>{{ formatOpponentDataStatus(row) }}</td>
              <td>
                <button
                  type="button"
                  class="account-btn-secondary my-event-matches-view__action-btn"
                  @click="goMatchDetail(row)"
                >
                  查看详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <MyEventsPagination
        v-if="total > 0"
        :page="pageNum"
        :page-size="pageSize"
        :total="total"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.my-event-matches-view {
  &__filters {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 16px;
    align-items: end;
    margin-bottom: 20px;

    label {
      display: flex;
      flex-direction: column;
      gap: 6px;
      font-size: 13px;
      color: #666;
    }

    select,
    input[type='date'] {
      min-width: 140px;
      height: 40px;
      padding: 0 10px;
      border: 1px solid rgba(0, 0, 0, 0.18);
      border-radius: 2px;
      background: #fff;
    }
  }

  &__table-wrap {
    width: 100%;
    overflow-x: auto;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;

    th,
    td {
      padding: 12px 10px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
      text-align: left;
      vertical-align: middle;
      white-space: nowrap;
    }

    th {
      font-size: 13px;
      color: #666;
      font-weight: 800;
    }
  }

  &__action-btn {
    min-width: 96px;
    min-height: 36px;
    height: 36px;
    padding: 0 12px;
    font-size: 13px;
  }

  :deep(.my-events-pagination) {
    margin-top: 20px;
  }
}
</style>
