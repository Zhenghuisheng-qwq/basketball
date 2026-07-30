<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { getDisputeStatus } from '@/api/matchDispute'
import { getAccountFileUrl } from '@/api/accountFile'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const matchId = computed(() => route.params.matchId as string)
const loading = ref(false)
const status = ref<any>(null)

const isPending = computed(() => status.value?.disputeStatus === 'PENDING' || status.value?.disputeStatus === 'PROCESSING')
const isUpheld = computed(() => status.value?.disputeStatus === 'UPHELD')
const isRejected = computed(() => status.value?.disputeStatus === 'REJECTED')

const materialUrls = computed(() =>
  (status.value?.materialFileIds || []).map((id: string | number) => getAccountFileUrl(id))
)

async function loadStatus() {
  loading.value = true
  try {
    const res = await getDisputeStatus(matchId.value)
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
      zh-title="争议处理状态"
      en-title="DISPUTE STATUS"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: '争议状态' }]"
    />

    <AccountPanel v-loading="loading" title="争议进度">
      <template v-if="status">
        <div
          class="status-badge"
          :class="{ pending: isPending, approved: isUpheld, rejected: isRejected }"
        >
          {{ status.disputeStatusText || status.disputeStatus }}
        </div>

        <p v-if="status.homeTeamName && status.awayTeamName">
          <strong>{{ status.homeTeamName }} vs {{ status.awayTeamName }}</strong>
        </p>
        <p v-if="status.homeScore != null"><strong>正式比分：</strong>{{ status.homeScore }} - {{ status.awayScore }}</p>
        <p v-if="status.disputeNo"><strong>争议编号：</strong>{{ status.disputeNo }}</p>
        <p v-if="status.disputeTypeText || status.disputeType">
          <strong>争议类型：</strong>{{ status.disputeTypeText || status.disputeType }}
        </p>
        <p v-if="status.disputeReason"><strong>争议说明：</strong>{{ status.disputeReason }}</p>
        <p v-if="status.submittedTime"><strong>提交时间：</strong>{{ status.submittedTime }}</p>
        <p v-if="status.deadlineTime"><strong>争议截止：</strong>{{ status.deadlineTime }}</p>
        <p v-if="status.processedTime"><strong>处理时间：</strong>{{ status.processedTime }}</p>
        <p v-if="status.processedByName"><strong>处理人：</strong>{{ status.processedByName }}</p>
        <p v-if="status.processResult"><strong>处理结果：</strong>{{ status.processResult }}</p>
        <p v-if="status.rejectReason" class="reject-reason"><strong>驳回原因：</strong>{{ status.rejectReason }}</p>

        <div v-if="materialUrls.length" class="material-row">
          <img v-for="(url, idx) in materialUrls" :key="idx" :src="url" class="material-thumb" alt="证明材料" />
        </div>

        <p v-if="isPending" class="event-team-muted">争议审核中，请等待管理员处理。</p>
        <p v-if="isUpheld" class="event-team-muted">争议已采纳，正式数据已修正。</p>
        <p v-if="isRejected" class="event-team-muted">争议已驳回。</p>
      </template>
      <p v-else-if="!loading" class="event-team-muted">暂无争议记录</p>

      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath(`/match-data/${matchId}/status`))">
          返回状态
        </button>
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/match-data'))">
          返回列表
        </button>
      </div>
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

.reject-reason {
  color: #cf1322;
}

.material-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 12px 0;
}

.material-thumb {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #ddd;
}
</style>
