<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import {
  getV4MatchSubmissionDetail,
  resubmitV4MatchSubmission,
  saveV4MatchSubmissionDraft,
  submitV4MatchSubmission,
} from '@/api/v4MatchSubmission'
import type { V4MatchSubmissionDetail, V4MatchSubmissionPlayerStat } from '@/api/types/v4MatchSubmission'
import { v4SubmissionStatusLabel } from '@/utils/v4AccountEventsMappers'
import { v4PlatformLabel } from '@/utils/v4Display'
import { formatDateTimeShort } from '@/utils/participationMappers'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const detail = ref<V4MatchSubmissionDetail | null>(null)

const matchId = computed(() => String(route.params.matchId || ''))
const submitEventTeamId = computed(() => String(route.query.submitEventTeamId || detail.value?.submitEventTeamId || ''))

const isReadonly = computed(() => {
  const status = (detail.value?.submissionStatus || '').toUpperCase()
  return status === 'PENDING_REVIEW' || status === 'APPROVED'
})

const canEdit = computed(() => {
  const status = (detail.value?.submissionStatus || 'DRAFT').toUpperCase()
  return status === 'DRAFT' || status === 'REJECTED' || !detail.value?.submissionId
})

const form = ref({
  homeScore: 0,
  awayScore: 0,
  submitRemark: '',
  materialFileIds: [] as string[],
  teamStat: {
    score: 0,
    rebound: 0,
    assist: 0,
    steal: 0,
    block: 0,
    turnover: 0,
    foul: 0,
  },
  playerStats: [] as V4MatchSubmissionPlayerStat[],
})

async function loadDetail() {
  loading.value = true
  try {
    const res = await getV4MatchSubmissionDetail(matchId.value, submitEventTeamId.value || undefined)
    detail.value = res.data || null
    if (!detail.value) return
    form.value.homeScore = detail.value.homeScore ?? 0
    form.value.awayScore = detail.value.awayScore ?? 0
    form.value.submitRemark = detail.value.submitRemark || ''
    form.value.materialFileIds = (detail.value.materialFileIds || []).map(String)
    if (detail.value.teamStat) {
      form.value.teamStat = { ...form.value.teamStat, ...detail.value.teamStat }
    }
    form.value.playerStats = (detail.value.playerStats || []).map((item) => ({ ...item }))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function buildSavePayload() {
  return {
    matchId: matchId.value,
    submitEventTeamId: submitEventTeamId.value,
    homeScore: form.value.homeScore,
    awayScore: form.value.awayScore,
    submitRemark: form.value.submitRemark,
    materialFileIds: form.value.materialFileIds,
    teamStat: form.value.teamStat,
    playerStats: form.value.playerStats,
  }
}

async function handleSaveDraft() {
  if (!canEdit.value) return
  saving.value = true
  try {
    const res = await saveV4MatchSubmissionDraft(buildSavePayload())
    if (res.data) {
      detail.value = { ...detail.value!, submissionId: String(res.data), submissionStatus: 'DRAFT' }
    }
    ElMessage.success('草稿已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmitAudit() {
  if (!canEdit.value) return
  saving.value = true
  try {
    const saveRes = await saveV4MatchSubmissionDraft(buildSavePayload())
    const submissionId = String(saveRes.data || detail.value?.submissionId || '')
    if (!submissionId) throw new Error('请先保存草稿')
    await submitV4MatchSubmission({ submissionId })
    ElMessage.success('已提交审核')
    await loadDetail()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    saving.value = false
  }
}

async function handleResubmit() {
  if ((detail.value?.submissionStatus || '').toUpperCase() !== 'REJECTED') return
  submitting.value = true
  try {
    await resubmitV4MatchSubmission({
      ...buildSavePayload(),
      oldSubmissionId: String(detail.value?.submissionId || ''),
    })
    ElMessage.success('已重新提交草稿，请再次提交审核')
    await loadDetail()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重提失败')
  } finally {
    submitting.value = false
  }
}

function addPlayerStatRow() {
  form.value.playerStats.push({
    eventPlayerId: '',
    points: 0,
    rebound: 0,
    assist: 0,
    steal: 0,
    block: 0,
    turnover: 0,
    foul: 0,
  })
}

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="比赛数据提交"
      en-title="MATCH DATA SUBMISSION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-events?tab=schedule' },
        { label: '比赛数据' },
      ]"
    />

    <AccountStatusBanner
      title="V4 比赛数据提交"
      :description="detail ? `${detail.homeTeamName} VS ${detail.awayTeamName}` : '加载中'"
      :status-text="v4SubmissionStatusLabel(detail?.submissionStatus)"
      :status-type="detail?.submissionStatus === 'APPROVED' ? 'success' : detail?.submissionStatus === 'REJECTED' ? 'danger' : 'info'"
    />

    <p v-if="loading" class="v4-sub-loading">加载中…</p>

    <template v-else-if="detail">
      <AccountPanel title="比赛信息">
        <p>{{ detail.divisionName }} · {{ v4PlatformLabel(detail.platformCode) }}</p>
        <p>比赛时间：{{ formatDateTimeShort(detail.matchTime) }}</p>
        <p v-if="detail.rejectReason" class="v4-sub-reject">驳回原因：{{ detail.rejectReason }}</p>
        <p v-if="isReadonly" class="v4-sub-readonly">当前状态只读，不可覆盖保存。</p>
      </AccountPanel>

      <AccountPanel title="总比分">
        <div class="v4-sub-scores">
          <label>
            主队 {{ detail.homeTeamName }}
            <input v-model.number="form.homeScore" type="number" min="0" :disabled="!canEdit" />
          </label>
          <label>
            客队 {{ detail.awayTeamName }}
            <input v-model.number="form.awayScore" type="number" min="0" :disabled="!canEdit" />
          </label>
        </div>
      </AccountPanel>

      <AccountPanel title="本方球队统计">
        <div class="v4-sub-grid">
          <label v-for="field in ['score', 'rebound', 'assist', 'steal', 'block', 'turnover', 'foul'] as const" :key="field">
            {{ field }}
            <input v-model.number="form.teamStat[field]" type="number" min="0" :disabled="!canEdit" />
          </label>
        </div>
      </AccountPanel>

      <AccountPanel title="本方球员统计">
        <div v-for="(row, index) in form.playerStats" :key="index" class="v4-sub-player-row">
          <input v-model="row.eventPlayerId" placeholder="eventPlayerId" :disabled="!canEdit" />
          <input v-model.number="row.points" type="number" min="0" placeholder="得分" :disabled="!canEdit" />
          <input v-model.number="row.rebound" type="number" min="0" placeholder="篮板" :disabled="!canEdit" />
          <input v-model.number="row.assist" type="number" min="0" placeholder="助攻" :disabled="!canEdit" />
        </div>
        <button v-if="canEdit" type="button" class="account-btn-secondary" @click="addPlayerStatRow">添加球员行</button>
      </AccountPanel>

      <AccountPanel title="材料与备注">
        <label class="v4-sub-remark">
          材料 fileId（逗号分隔，占位）
          <input
            :value="form.materialFileIds.join(',')"
            :disabled="!canEdit"
            @input="form.materialFileIds = ($event.target as HTMLInputElement).value.split(',').map((s) => s.trim()).filter(Boolean)"
          />
        </label>
        <label class="v4-sub-remark">
          提交备注
          <textarea v-model="form.submitRemark" rows="3" :disabled="!canEdit" />
        </label>
      </AccountPanel>

      <div class="v4-sub-actions">
        <button v-if="canEdit" type="button" class="account-btn-secondary" :disabled="saving" @click="handleSaveDraft">保存草稿</button>
        <button v-if="canEdit" type="button" class="account-btn-primary" :disabled="submitting" @click="handleSubmitAudit">提交审核</button>
        <button
          v-if="detail.submissionStatus === 'REJECTED'"
          type="button"
          class="account-btn-primary"
          :disabled="submitting"
          @click="handleResubmit"
        >
          驳回后重提
        </button>
        <button type="button" class="account-btn-secondary" @click="router.push('/account/my-events?tab=schedule')">返回我的赛程</button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.v4-sub-loading {
  color: #696969;
}

.v4-sub-reject {
  color: #b42318;
}

.v4-sub-readonly {
  color: #666;
}

.v4-sub-scores,
.v4-sub-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;

  label {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 13px;
  }

  input {
    height: 40px;
    padding: 0 10px;
    border: 1px solid rgba(0, 0, 0, 0.18);
  }
}

.v4-sub-player-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(80px, 1fr));
  gap: 8px;
  margin-bottom: 8px;
}

.v4-sub-remark {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;

  textarea,
  input {
    padding: 8px 10px;
    border: 1px solid rgba(0, 0, 0, 0.18);
  }
}

.v4-sub-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}
</style>
