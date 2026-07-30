<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { getMatchDataStatus } from '@/api/matchData'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const matchId = computed(() => route.params.matchId as string)
const loading = ref(false)
const status = ref<any>(null)

const isPending = computed(() => status.value?.submissionStatus === 1)
const isApproved = computed(() => status.value?.submissionStatus === 2)
const isRejected = computed(() => status.value?.submissionStatus === 3)

const hasActiveDispute = computed(() => {
  const s = status.value?.currentDisputeStatus
  return s === 'PENDING' || s === 'PROCESSING'
})

const disputeExpired = computed(() => {
  if (status.value?.canDispute) return false
  if (!isApproved.value) return false
  const reason = status.value?.cannotDisputeReason || ''
  return reason.includes('过期') || reason.includes('窗口') || reason.includes('结束')
})

async function loadStatus() {
  loading.value = true
  try {
    const res = await getMatchDataStatus(Number(matchId.value))
    status.value = res.data
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await eventTeamStore.ensureLoaded(String(route.params.tournamentId))
  loadStatus()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="比赛数据状态"
      en-title="MATCH DATA STATUS"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: '状态' }]"
    />

    <AccountPanel v-loading="loading" title="审核进度">
      <template v-if="status">
        <div class="status-badge" :class="{ pending: isPending, approved: isApproved, rejected: isRejected }">
          {{ status.statusText || status.submissionStatusText }}
        </div>

        <p v-if="status.homeTeamName && status.awayTeamName">
          <strong>{{ status.homeTeamName }} vs {{ status.awayTeamName }}</strong>
        </p>
        <p v-if="status.homeScore != null"><strong>提交比分：</strong>{{ status.homeScore }} - {{ status.awayScore }}</p>
        <p v-if="status.submissionNo"><strong>提交编号：</strong>{{ status.submissionNo }} · v{{ status.versionNo }}</p>
        <p v-if="status.submittedTime"><strong>提交时间：</strong>{{ status.submittedTime }}</p>
        <p v-if="status.reviewedTime"><strong>审核时间：</strong>{{ status.reviewedTime }}</p>
        <p v-if="status.reviewedByName"><strong>审核人：</strong>{{ status.reviewedByName }}</p>
        <p v-if="status.rejectReason" class="reject-reason"><strong>驳回原因：</strong>{{ status.rejectReason }}</p>

        <p v-if="isPending" class="event-team-muted">数据审核中，暂不可编辑。请等待管理员处理。</p>
        <p v-if="isApproved" class="event-team-muted">审核已通过，数据已公开。</p>
        <p v-if="disputeExpired" class="warn">争议期已结束，请联系管理员修正数据。</p>
        <p v-else-if="isApproved && status.disputeDeadlineTime" class="event-team-muted">
          争议截止：{{ status.disputeDeadlineTime }}
        </p>

        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/match-data'))">返回列表</button>
          <button
            v-if="status.canResubmit"
            type="button"
            class="account-btn-primary"
            @click="router.push(teamPath(`/match-data/${matchId}/resubmit`))"
          >
            修改并重新提交
          </button>
          <button
            v-if="status.canViewPublicData"
            type="button"
            class="account-btn-primary"
            disabled
            title="公开数据页 P1"
          >
            查看公开数据（P1）
          </button>
          <button
            v-if="hasActiveDispute || status.currentDisputeId"
            type="button"
            class="account-btn-secondary"
            @click="router.push(teamPath(`/match-data/${matchId}/dispute-status`))"
          >
            查看争议状态
          </button>
          <button
            v-else-if="status.canDispute"
            type="button"
            class="account-btn-primary"
            @click="router.push(teamPath(`/match-data/${matchId}/dispute`))"
          >
            发起数据争议
          </button>
          <button
            v-if="status.currentExceptionId || status.currentApplicationId"
            type="button"
            class="account-btn-secondary"
            @click="router.push(teamPath(`/match-data/${matchId}/exception-status`))"
          >
            查看异常状态
          </button>
          <button
            v-else-if="status.canSubmitException"
            type="button"
            class="account-btn-secondary"
            @click="router.push(teamPath(`/match-data/${matchId}/exception`))"
          >
            异常比赛申请
          </button>
        </div>
      </template>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.status-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 4px;
  font-weight: 600;
  margin-bottom: 12px;
  background: #f0f0f0;
}

.status-badge.pending {
  background: #fff7e6;
  color: #d48806;
}

.status-badge.approved {
  background: #f6ffed;
  color: #389e0d;
}

.status-badge.rejected {
  background: #fff1f0;
  color: #cf1322;
}

.reject-reason,
.warn {
  color: #cf1322;
}
</style>
