<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import {
  getMyScheduleEventOptions,
  getMySchedules,
  type MyScheduleEventOption,
  type MyScheduleMatchStatusGroup,
} from '@/api/mySchedule'
import type { PlayerMatchTaskItem } from '@/api/matchData'
import { useAccountStore } from '@/stores/account'
import {
  buildMatchDetailRoute,
  buildMatchDataListRoute,
} from '@/utils/participationMappers'
import { canSubmitMatchData, resolveMatchStatusDisplayText } from '@/utils/matchDataTabHelpers'

const props = defineProps<{
  active: boolean
}>()

const router = useRouter()
const accountStore = useAccountStore()

const loading = ref(false)
const loadError = ref('')
const rows = ref<PlayerMatchTaskItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const nextMatch = ref<PlayerMatchTaskItem | null>(null)
const certificationRequired = ref(false)
const noParticipation = ref(false)
const eventOptions = ref<MyScheduleEventOption[]>([])

const matchStatusGroup = ref<MyScheduleMatchStatusGroup>('PENDING')
const seasonId = ref('')
const beginDate = ref('')
const endDate = ref('')
const sortOrder = ref<'ASC' | 'DESC'>('ASC')
const quickRange = ref<'ALL' | '7D' | '30D'>('ALL')

const isVerified = computed(() => accountStore.isVerifiedPlayer)

const statusOptions: { value: MyScheduleMatchStatusGroup; label: string }[] = [
  { value: 'PENDING', label: '待比赛' },
  { value: 'RUNNING', label: '进行中' },
  { value: 'FINISHED', label: '已结束' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'ALL', label: '全部' },
]

function formatDateInput(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function applyQuickRange(range: 'ALL' | '7D' | '30D') {
  quickRange.value = range
  if (range === 'ALL') {
    beginDate.value = ''
    endDate.value = ''
    return
  }
  const today = new Date()
  beginDate.value = formatDateInput(today)
  const end = new Date(today)
  end.setDate(end.getDate() + (range === '7D' ? 7 : 30))
  endDate.value = formatDateInput(end)
}

function resolveHomeAwayText(row: PlayerMatchTaskItem): string {
  const myId = row.myEventTeamId
  if (!myId) return '-'
  if (myId === row.homeEventTeamId) return '主队'
  if (myId === row.awayEventTeamId) return '客队'
  return '-'
}

function resolveMyTeamName(row: PlayerMatchTaskItem): string {
  return row.myTeamNameSnapshot || row.myTeamName || '-'
}

function resolveOpponentName(row: PlayerMatchTaskItem): string {
  return row.opponentTeamNameSnapshot || '-'
}

function formatScore(row: PlayerMatchTaskItem): string {
  if (row.matchStatus !== 3 && !resolveMatchStatusDisplayText(row).includes('已结束')) {
    return '-'
  }
  if (row.homeScore == null || row.awayScore == null) return '-'
  const myId = row.myEventTeamId
  if (myId && myId === row.homeEventTeamId) {
    return `${row.homeScore} : ${row.awayScore}`
  }
  if (myId && myId === row.awayEventTeamId) {
    return `${row.awayScore} : ${row.homeScore}`
  }
  return `${row.homeScore} : ${row.awayScore}`
}

function formatCountdown(match: PlayerMatchTaskItem | null): string {
  if (!match?.scheduledStartTime) return '比赛时间待定'
  const target = new Date(match.scheduledStartTime).getTime()
  if (Number.isNaN(target)) return '比赛时间待定'
  const diff = target - Date.now()
  if (diff <= 0) return '比赛已开始或结束'
  const hours = Math.floor(diff / 3600000)
  const minutes = Math.floor((diff % 3600000) / 60000)
  if (hours >= 24) {
    const days = Math.floor(hours / 24)
    return `距离开赛还有 ${days} 天 ${hours % 24} 小时`
  }
  return `距离开赛还有 ${hours} 小时 ${minutes} 分钟`
}

function eventLabel(option: MyScheduleEventOption): string {
  const name = option.eventName || option.seasonName || '赛事'
  const season = option.seasonName && option.eventName ? ` / ${option.seasonName}` : ''
  const code = option.eventCode ? ` (${option.eventCode})` : ''
  return `${name}${season}${code}`
}

function rowEventLabel(row: PlayerMatchTaskItem): string {
  const parts = [row.eventName, row.stageName].filter(Boolean)
  return parts.length ? parts.join(' · ') : '-'
}

async function loadEventOptions() {
  if (!isVerified.value) {
    eventOptions.value = []
    return
  }
  try {
    const res = await getMyScheduleEventOptions()
    eventOptions.value = res.data || []
  } catch {
    eventOptions.value = []
  }
}

async function loadList() {
  if (!props.active) return
  if (!isVerified.value) {
    certificationRequired.value = true
    rows.value = []
    total.value = 0
    nextMatch.value = null
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getMySchedules({
      seasonId: seasonId.value || undefined,
      matchStatusGroup: matchStatusGroup.value,
      startDate: beginDate.value || undefined,
      endDate: endDate.value || undefined,
      sortOrder: sortOrder.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    })
    const data = res.data
    rows.value = data?.records || []
    total.value = Number(data?.total ?? 0)
    nextMatch.value = data?.nextMatch ?? null
    certificationRequired.value = Boolean(data?.certificationRequired)
    noParticipation.value = Boolean(data?.noParticipation)
    if (data?.pageNum) pageNum.value = Number(data.pageNum)
    if (data?.pageSize) pageSize.value = Number(data.pageSize)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
    rows.value = []
    total.value = 0
    nextMatch.value = null
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  if (beginDate.value && endDate.value && beginDate.value > endDate.value) {
    ElMessage.warning('开始日期不能晚于结束日期')
    return
  }
  pageNum.value = 1
  void loadList()
}

function handleReset() {
  matchStatusGroup.value = 'PENDING'
  seasonId.value = ''
  beginDate.value = ''
  endDate.value = ''
  sortOrder.value = 'ASC'
  quickRange.value = 'ALL'
  pageNum.value = 1
  void loadList()
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

function goMatchDetail(row: PlayerMatchTaskItem) {
  const eventTeamId = row.myEventTeamId || row.eventTeamId
  if (!eventTeamId || !row.matchId) return
  router.push(buildMatchDetailRoute(eventTeamId, row.matchId))
}

function goEvent(row: PlayerMatchTaskItem) {
  const eventTeamId = row.myEventTeamId || row.eventTeamId
  if (!eventTeamId) return
  router.push(buildMatchDataListRoute(eventTeamId))
}

function goRoster(row: PlayerMatchTaskItem) {
  const seasonKey = row.seasonId
  if (!seasonKey) return
  router.push(`/account/my-tournaments/${seasonKey}/team/roster`)
}

function goCertification() {
  router.push('/account/player-identity')
}

function goAvailableEvents() {
  router.push('/account/my-events')
}

watch([beginDate, endDate], () => {
  quickRange.value = 'ALL'
})

watch(
  () => props.active,
  (active) => {
    if (active) {
      void loadEventOptions()
      void loadList()
    }
  },
  { immediate: true },
)

watch(matchStatusGroup, (value) => {
  if (value === 'FINISHED') {
    sortOrder.value = 'DESC'
  } else if (value === 'PENDING') {
    sortOrder.value = 'ASC'
  }
})

onMounted(() => {
  if (props.active) {
    void loadEventOptions()
  }
})
</script>

<template>
  <div class="my-schedule-panel">
    <div v-if="certificationRequired || !isVerified" class="my-schedule-panel__empty-wrap">
      <AccountEmptyState
        message="完成球员认证后，可在此查看个人赛程安排。"
        subtitle="认证通过后可聚合查看全部已参加赛事的比赛安排"
        large
      />
      <button type="button" class="account-btn-primary my-schedule-panel__empty-action" @click="goCertification">
        前往球员认证
      </button>
    </div>

    <template v-else>
      <div v-if="nextMatch && matchStatusGroup === 'PENDING'" class="my-schedule-panel__next">
        <div class="my-schedule-panel__next-head">
          <span class="my-schedule-panel__next-label">下一场比赛</span>
          <span class="my-schedule-panel__next-countdown">{{ formatCountdown(nextMatch) }}</span>
        </div>
        <div class="my-schedule-panel__next-meta">
          {{ rowEventLabel(nextMatch) }}
          <template v-if="nextMatch.roundNo"> · 第 {{ nextMatch.roundNo }} 轮</template>
        </div>
        <div class="my-schedule-panel__next-time">
          {{ nextMatch.matchDate || '-' }}
          {{ nextMatch.weekday || '' }}
          {{ nextMatch.matchTime || '待定' }}
        </div>
        <div class="my-schedule-panel__next-versus">
          <span>{{ resolveMyTeamName(nextMatch) }}</span>
          <span class="my-schedule-panel__vs">VS</span>
          <span>{{ resolveOpponentName(nextMatch) }}</span>
        </div>
        <div class="my-schedule-panel__next-tags">
          <span>{{ resolveHomeAwayText(nextMatch) }}</span>
          <span>{{ resolveMatchStatusDisplayText(nextMatch) }}</span>
        </div>
        <div class="my-schedule-panel__next-actions">
          <button type="button" class="account-btn-primary" @click="goMatchDetail(nextMatch)">查看比赛详情</button>
          <button type="button" class="account-btn-secondary" @click="goEvent(nextMatch)">查看赛事</button>
          <button type="button" class="account-btn-secondary" @click="goRoster(nextMatch)">查看阵容</button>
        </div>
      </div>

      <div v-else-if="matchStatusGroup === 'PENDING' && !loading && rows.length === 0" class="my-schedule-panel__next my-schedule-panel__next--empty">
        暂无待进行比赛
      </div>

      <div class="my-schedule-panel__filters">
        <label>
          比赛状态
          <select v-model="matchStatusGroup">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label>
          所属赛事
          <select v-model="seasonId">
            <option value="">全部赛事</option>
            <option v-for="item in eventOptions" :key="item.seasonId" :value="item.seasonId">
              {{ eventLabel(item) }}
            </option>
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
        <label>
          排序
          <select v-model="sortOrder">
            <option value="ASC">比赛时间升序</option>
            <option value="DESC">比赛时间降序</option>
          </select>
        </label>
        <div class="my-schedule-panel__filter-actions">
          <button type="button" class="account-btn-primary" :disabled="loading" @click="handleSearch">查询</button>
          <button type="button" class="account-btn-secondary" :disabled="loading" @click="handleReset">重置</button>
        </div>
      </div>

      <div class="my-schedule-panel__quick">
        <button
          type="button"
          class="my-schedule-panel__quick-btn"
          :class="{ 'my-schedule-panel__quick-btn--active': quickRange === '7D' }"
          @click="applyQuickRange('7D'); handleSearch()"
        >
          未来 7 天
        </button>
        <button
          type="button"
          class="my-schedule-panel__quick-btn"
          :class="{ 'my-schedule-panel__quick-btn--active': quickRange === '30D' }"
          @click="applyQuickRange('30D'); handleSearch()"
        >
          未来 30 天
        </button>
        <button
          type="button"
          class="my-schedule-panel__quick-btn"
          :class="{ 'my-schedule-panel__quick-btn--active': quickRange === 'ALL' && !beginDate && !endDate }"
          @click="applyQuickRange('ALL'); handleSearch()"
        >
          全部时间
        </button>
      </div>

      <p v-if="loadError" class="my-schedule-panel__error">{{ loadError }}</p>
      <p v-else-if="loading" class="my-schedule-panel__loading">加载中…</p>

      <div v-else-if="noParticipation" class="my-schedule-panel__empty-wrap">
        <AccountEmptyState
          message="你暂未参加任何赛事。"
          subtitle="完成报名并进入正式阵容后，可在此查看赛程"
          large
        />
        <button type="button" class="account-btn-primary my-schedule-panel__empty-action" @click="goAvailableEvents">
          查看可报名赛事
        </button>
      </div>

      <div v-else-if="!rows.length" class="my-schedule-panel__empty-wrap">
        <AccountEmptyState
          message="当前筛选条件下暂无比赛。"
          :subtitle="eventOptions.length ? '可尝试重置筛选或切换比赛状态' : '当前暂无已发布的比赛安排，请等待赛事方发布赛程。'"
          large
        />
        <button type="button" class="account-btn-secondary my-schedule-panel__empty-action" @click="handleReset">
          重置筛选
        </button>
      </div>

      <template v-else>
        <div class="my-schedule-panel__table-wrap">
          <table class="my-schedule-panel__table">
            <thead>
              <tr>
                <th>轮次</th>
                <th>日期</th>
                <th>星期</th>
                <th>时间</th>
                <th>所属赛事</th>
                <th>我的球队</th>
                <th>主客场</th>
                <th>对手</th>
                <th>比赛状态</th>
                <th>比分</th>
                <th>数据状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in rows" :key="`${row.matchId}-${row.myEventTeamId}`">
                <td>{{ row.roundNo ? `第 ${row.roundNo} 轮` : '-' }}</td>
                <td>{{ row.matchDate || '-' }}</td>
                <td>{{ row.weekday || '-' }}</td>
                <td>{{ row.matchTime || '待定' }}</td>
                <td :title="rowEventLabel(row)">{{ rowEventLabel(row) }}</td>
                <td :title="resolveMyTeamName(row)">{{ resolveMyTeamName(row) }}</td>
                <td>{{ resolveHomeAwayText(row) }}</td>
                <td :title="resolveOpponentName(row)">{{ resolveOpponentName(row) }}</td>
                <td>{{ resolveMatchStatusDisplayText(row) }}</td>
                <td>{{ formatScore(row) }}</td>
                <td>{{ row.dataStatusText || '-' }}</td>
                <td class="my-schedule-panel__actions">
                  <button
                    v-if="canSubmitMatchData(row)"
                    type="button"
                    class="account-btn-primary"
                    @click="goMatchDetail(row)"
                  >
                    提交比赛数据
                  </button>
                  <button type="button" class="account-btn-secondary" @click="goMatchDetail(row)">查看详情</button>
                  <button type="button" class="account-btn-secondary" @click="goEvent(row)">查看赛事</button>
                  <button type="button" class="account-btn-secondary" @click="goRoster(row)">查看阵容</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <MyEventsPagination
          :page="pageNum"
          :page-size="pageSize"
          :total="total"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </template>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.my-schedule-panel {
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

  &__next {
    margin-bottom: 20px;
    padding: 20px 24px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    background: #fff;

    &--empty {
      color: #666;
      font-size: 14px;
    }
  }

  &__next-head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    flex-wrap: wrap;
    margin-bottom: 8px;
  }

  &__next-label {
    font-size: 16px;
    font-weight: 800;
    color: #111;
  }

  &__next-countdown {
    font-size: 13px;
    color: #b42318;
    font-weight: 700;
  }

  &__next-meta,
  &__next-time {
    font-size: 14px;
    color: #444;
    margin-bottom: 6px;
  }

  &__next-versus {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;
    font-size: 18px;
    font-weight: 800;
    margin: 12px 0;
  }

  &__vs {
    color: #999;
    font-size: 14px;
    font-weight: 700;
  }

  &__next-tags {
    display: flex;
    gap: 12px;
    font-size: 13px;
    color: #666;
    margin-bottom: 14px;
  }

  &__next-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  &__filters {
    display: flex;
    flex-wrap: wrap;
    gap: 12px 16px;
    align-items: end;
    margin-bottom: 12px;

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

  &__filter-actions {
    display: flex;
    gap: 10px;
  }

  &__quick {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
  }

  &__quick-btn {
    height: 32px;
    padding: 0 12px;
    border: 1px solid rgba(0, 0, 0, 0.18);
    background: #fff;
    font-size: 13px;
    cursor: pointer;

    &--active {
      border-color: #b42318;
      color: #b42318;
      font-weight: 700;
    }
  }

  &__error {
    color: #b42318;
    font-size: 13px;
    margin-bottom: 12px;
  }

  &__loading {
    color: #696969;
    font-size: 14px;
    margin-bottom: 12px;
  }

  &__table-wrap {
    width: 100%;
    overflow-x: auto;
  }

  &__table {
    width: 100%;
    min-width: 1100px;
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

    td:nth-child(5),
    td:nth-child(6),
    td:nth-child(8) {
      max-width: 160px;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    min-width: 220px;

    .account-btn-secondary {
      min-height: 34px;
      height: 34px;
      padding: 0 10px;
      font-size: 12px;
    }
  }
}
</style>
