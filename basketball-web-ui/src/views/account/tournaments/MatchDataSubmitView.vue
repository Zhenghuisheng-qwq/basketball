<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import {
  getSubmitDetail,
  getResubmitDetail,
  submitMatchData,
  resubmitMatchData
} from '@/api/matchData'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const matchId = computed(() => Number(route.params.matchId))
const isResubmit = computed(() => route.name === 'AccountMatchDataResubmit')

const loading = ref(false)
const submitting = ref(false)
const detail = ref<Record<string, any> | null>(null)

const form = ref({
  homeScore: 0,
  awayScore: 0,
  submitRemark: '',
  periodScores: [] as any[],
  teamStats: [] as any[],
  playerStats: [] as any[],
  materialFileIds: [] as number[]
})

async function loadDetail() {
  loading.value = true
  try {
    const res = isResubmit.value
      ? await getResubmitDetail(matchId.value)
      : await getSubmitDetail(matchId.value)
    const data = res.data as any
    detail.value = data
    if (!data?.canSubmit && !isResubmit.value) {
      ElMessage.warning(data?.cannotSubmitReason || '当前不可提交')
    }
    form.value.homeScore = Number(data?.homeScore ?? 0)
    form.value.awayScore = Number(data?.awayScore ?? 0)
    form.value.periodScores = data?.periodScores || defaultPeriods()
    form.value.teamStats = data?.teamStats || []
    form.value.playerStats = data?.playerStats || []
    form.value.materialFileIds = (data?.materialFileIds || []).map(Number).filter(Boolean)
  } finally {
    loading.value = false
  }
}

function defaultPeriods() {
  return [1, 2, 3, 4].map((n) => ({
    periodNo: n,
    periodType: 'REGULAR',
    homeScore: 0,
    awayScore: 0
  }))
}

async function handleSubmit() {
  if (!form.value.materialFileIds.length) {
    ElMessage.warning('请至少上传一张比赛结算截图')
    return
  }
  if (!form.value.teamStats.length || !form.value.playerStats.length) {
    ElMessage.warning('缺少球队或球员统计数据，请刷新页面重试')
    return
  }
  // 同步球队得分与最终比分
  const homeTeamId = detail.value?.homeTeamId
  const awayTeamId = detail.value?.awayTeamId
  for (const stat of form.value.teamStats) {
    if (String(stat.teamId) === String(homeTeamId)) {
      stat.points = form.value.homeScore
    } else if (String(stat.teamId) === String(awayTeamId)) {
      stat.points = form.value.awayScore
    }
  }
  submitting.value = true
  try {
    const payload = {
      homeScore: form.value.homeScore,
      awayScore: form.value.awayScore,
      periodScores: form.value.periodScores,
      teamStats: form.value.teamStats,
      playerStats: form.value.playerStats,
      materialFileIds: form.value.materialFileIds.map(String),
      remark: form.value.submitRemark
    }
    if (isResubmit.value) {
      await resubmitMatchData(matchId.value, payload)
    } else {
      await submitMatchData(matchId.value, payload)
    }
    ElMessage.success('提交成功，等待管理员审核')
    router.push(teamPath('/match-data'))
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await eventTeamStore.ensureLoaded(String(route.params.tournamentId))
  loadDetail()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="isResubmit ? '重新提交比赛数据' : '提交比赛数据'"
      en-title="MATCH DATA SUBMISSION"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: isResubmit ? '重新提交' : '提交' }]"
    />

    <AccountPanel v-loading="loading" :title="detail?.homeTeamName + ' vs ' + detail?.awayTeamName">
      <p v-if="detail?.cannotSubmitReason" class="event-team-muted">{{ detail.cannotSubmitReason }}</p>

      <div class="form-grid">
        <label>主队得分<input v-model.number="form.homeScore" type="number" min="0" /></label>
        <label>客队得分<input v-model.number="form.awayScore" type="number" min="0" /></label>
      </div>

      <h4>分节比分</h4>
      <div v-for="p in form.periodScores" :key="p.periodNo" class="form-grid">
        <span>第{{ p.periodNo }}节</span>
        <label>主<input v-model.number="p.homeScore" type="number" min="0" /></label>
        <label>客<input v-model.number="p.awayScore" type="number" min="0" /></label>
      </div>

      <h4>材料文件ID（RESULT_SCREENSHOT，逗号分隔）</h4>
      <input
        :value="form.materialFileIds.join(',')"
        placeholder="例: 1001,1002"
        @input="form.materialFileIds = String(($event.target as HTMLInputElement).value).split(',').filter(Boolean).map(Number)"
      />

      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/match-data'))">返回</button>
        <button type="button" class="account-btn-primary" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中...' : '提交审核' }}
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
  margin-bottom: 12px;
}

input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}
</style>
