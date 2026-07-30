<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { getExceptionStatus } from '@/api/matchException'
import { getAccountFileUrl } from '@/api/accountFile'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const matchId = computed(() => route.params.matchId as string)
const loading = ref(false)
const status = ref<any>(null)

const isPending = computed(() =>
  status.value?.applicationStatus === 'PENDING' || status.value?.applicationStatus === 'PROCESSING'
)
const isApproved = computed(() => status.value?.applicationStatus === 'APPROVED')
const isRejected = computed(() => status.value?.applicationStatus === 'REJECTED')

const materialUrls = computed(() =>
  (status.value?.materialFileIds || []).map((id: string | number) => getAccountFileUrl(id))
)

async function loadStatus() {
  loading.value = true
  try {
    const res = await getExceptionStatus(matchId.value)
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
      zh-title="异常申请状态"
      en-title="EXCEPTION STATUS"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: '异常状态' }]"
    />

    <AccountPanel v-loading="loading" title="裁定进度">
      <template v-if="status">
        <div
          class="status-badge"
          :class="{ pending: isPending, approved: isApproved, rejected: isRejected }"
        >
          {{ status.applicationStatusText || status.applicationStatus }}
        </div>

        <p v-if="status.homeTeamName && status.awayTeamName">
          <strong>{{ status.homeTeamName }} vs {{ status.awayTeamName }}</strong>
        </p>
        <p v-if="status.applicationNo"><strong>申请编号：</strong>{{ status.applicationNo }}</p>
        <p v-if="status.exceptionTypeText || status.exceptionType">
          <strong>异常类型：</strong>{{ status.exceptionTypeText || status.exceptionType }}
        </p>
        <p v-if="status.exceptionReason"><strong>异常说明：</strong>{{ status.exceptionReason }}</p>
        <p v-if="status.submittedTime"><strong>提交时间：</strong>{{ status.submittedTime }}</p>
        <p v-if="status.processedTime"><strong>处理时间：</strong>{{ status.processedTime }}</p>
        <p v-if="status.processedByName"><strong>处理人：</strong>{{ status.processedByName }}</p>
        <p v-if="status.decisionTypeText || status.decisionType">
          <strong>裁定类型：</strong>{{ status.decisionTypeText || status.decisionType }}
        </p>
        <p v-if="status.decisionResult"><strong>裁定说明：</strong>{{ status.decisionResult }}</p>
        <p v-if="status.rejectReason" class="reject-reason"><strong>驳回原因：</strong>{{ status.rejectReason }}</p>

        <div v-if="materialUrls.length" class="material-row">
          <img v-for="(url, idx) in materialUrls" :key="idx" :src="url" class="material-thumb" alt="证明材料" />
        </div>

        <p v-if="isPending" class="event-team-muted">异常申请审核中，请等待管理员裁定。</p>
        <p v-if="isApproved" class="event-team-muted">异常申请已裁定完成。</p>
        <p v-if="isRejected" class="event-team-muted">异常申请已驳回。</p>
      </template>
      <p v-else-if="!loading" class="event-team-muted">暂无异常申请记录</p>

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
