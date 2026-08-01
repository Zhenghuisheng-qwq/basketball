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
import { uploadAccountFile, getAccountFileUrl } from '@/api/accountFile'
import { useEventTeamPage } from '@/composables/useEventTeamPage'
import { validateMatchDataSubmitForm } from '@/utils/matchDataValidation'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage({ requireCaptain: true })

const matchId = computed(() => Number(route.params.matchId))
const isResubmit = computed(() => route.name === 'AccountMatchDataResubmit')

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const detail = ref<Record<string, any> | null>(null)
const materialPreviews = ref<{ fileId: number; name: string; url: string }[]>([])

const form = ref({
  homeScore: 0,
  awayScore: 0,
  submitRemark: '',
  periodScores: [] as any[],
  teamStats: [] as any[],
  playerStats: [] as any[],
  materialFileIds: [] as number[]
})

const homeTeamId = computed(() => String(detail.value?.homeTeamId || ''))
const awayTeamId = computed(() => String(detail.value?.awayTeamId || ''))

const homePlayerStats = computed(() => form.value.playerStats.filter((p) => String(p.teamId) === homeTeamId.value))
const awayPlayerStats = computed(() => form.value.playerStats.filter((p) => String(p.teamId) === awayTeamId.value))

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
    materialPreviews.value = form.value.materialFileIds.map((id) => ({
      fileId: id,
      name: `截图 #${id}`,
      url: getAccountFileUrl(id)
    }))
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

function syncTeamPointsFromFinal() {
  for (const stat of form.value.teamStats) {
    if (String(stat.teamId) === homeTeamId.value) stat.points = form.value.homeScore
    if (String(stat.teamId) === awayTeamId.value) stat.points = form.value.awayScore
  }
}

async function onPickScreenshot(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadAccountFile(file, 'MATCH_DATA_SCREENSHOT')
    const fileId = Number(res.data?.fileId)
    if (!fileId) throw new Error('上传失败')
    form.value.materialFileIds.push(fileId)
    materialPreviews.value.push({
      fileId,
      name: res.data?.originalName || file.name,
      url: getAccountFileUrl(fileId)
    })
    ElMessage.success('截图上传成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function removeMaterial(fileId: number) {
  form.value.materialFileIds = form.value.materialFileIds.filter((id) => id !== fileId)
  materialPreviews.value = materialPreviews.value.filter((m) => m.fileId !== fileId)
}

async function handleSubmit() {
  if (submitting.value) return
  syncTeamPointsFromFinal()
  const errors = validateMatchDataSubmitForm(form.value, {
    homeTeamId: detail.value?.homeTeamId,
    awayTeamId: detail.value?.awayTeamId,
    homeTeamName: detail.value?.homeTeamName,
    awayTeamName: detail.value?.awayTeamName,
    rosterPlayerIds: (detail.value?.rosters || []).map((r: any) => r.playerId)
  })
  if (errors.length) {
    ElMessage.warning(errors[0])
    return
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
    router.push(teamPath(`/match-data/${matchId.value}/status`))
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

    <AccountPanel v-loading="loading" :title="(detail?.homeTeamName || '主队') + ' vs ' + (detail?.awayTeamName || '客队')">
      <p v-if="detail?.scheduledStartTime" class="event-team-muted">比赛时间：{{ detail.scheduledStartTime }}</p>
      <p v-if="detail?.cannotSubmitReason" class="event-team-muted warn">{{ detail.cannotSubmitReason }}</p>

      <h4>最终比分</h4>
      <div class="form-grid">
        <label>{{ detail?.homeTeamName || '主队' }}<input v-model.number="form.homeScore" type="number" min="0" @change="syncTeamPointsFromFinal" /></label>
        <label>{{ detail?.awayTeamName || '客队' }}<input v-model.number="form.awayScore" type="number" min="0" @change="syncTeamPointsFromFinal" /></label>
      </div>

      <h4>分节比分</h4>
      <div v-for="p in form.periodScores" :key="p.periodNo" class="form-grid period-row">
        <span>第{{ p.periodNo }}节</span>
        <label>主<input v-model.number="p.homeScore" type="number" min="0" /></label>
        <label>客<input v-model.number="p.awayScore" type="number" min="0" /></label>
      </div>

      <h4>球队数据</h4>
      <div v-for="stat in form.teamStats" :key="stat.teamId" class="stat-block">
        <strong>{{ String(stat.teamId) === homeTeamId ? detail?.homeTeamName : detail?.awayTeamName }}</strong>
        <div class="form-grid stat-grid">
          <label>得分<input v-model.number="stat.points" type="number" min="0" /></label>
          <label>篮板<input v-model.number="stat.rebounds" type="number" min="0" /></label>
          <label>助攻<input v-model.number="stat.assists" type="number" min="0" /></label>
          <label>抢断<input v-model.number="stat.steals" type="number" min="0" /></label>
          <label>盖帽<input v-model.number="stat.blocks" type="number" min="0" /></label>
          <label>失误<input v-model.number="stat.turnovers" type="number" min="0" /></label>
          <label>犯规<input v-model.number="stat.fouls" type="number" min="0" /></label>
          <label>投篮命中<input v-model.number="stat.fieldGoalsMade" type="number" min="0" /></label>
          <label>投篮出手<input v-model.number="stat.fieldGoalsAttempted" type="number" min="0" /></label>
          <label>三分命中<input v-model.number="stat.threePointsMade" type="number" min="0" /></label>
          <label>三分出手<input v-model.number="stat.threePointsAttempted" type="number" min="0" /></label>
          <label>罚球命中<input v-model.number="stat.freeThrowsMade" type="number" min="0" /></label>
          <label>罚球出手<input v-model.number="stat.freeThrowsAttempted" type="number" min="0" /></label>
        </div>
      </div>

      <h4>球员数据 — {{ detail?.homeTeamName }}</h4>
      <div v-for="p in homePlayerStats" :key="p.playerId" class="stat-block compact">
        <strong>{{ p.playerId }}</strong>
        <div class="form-grid stat-grid">
          <label>得分<input v-model.number="p.points" type="number" min="0" /></label>
          <label>篮板<input v-model.number="p.rebounds" type="number" min="0" /></label>
          <label>助攻<input v-model.number="p.assists" type="number" min="0" /></label>
          <label>抢断<input v-model.number="p.steals" type="number" min="0" /></label>
          <label>盖帽<input v-model.number="p.blocks" type="number" min="0" /></label>
          <label>投篮命中<input v-model.number="p.fieldGoalsMade" type="number" min="0" /></label>
          <label>投篮出手<input v-model.number="p.fieldGoalsAttempted" type="number" min="0" /></label>
        </div>
      </div>

      <h4>球员数据 — {{ detail?.awayTeamName }}</h4>
      <div v-for="p in awayPlayerStats" :key="p.playerId" class="stat-block compact">
        <strong>{{ p.playerId }}</strong>
        <div class="form-grid stat-grid">
          <label>得分<input v-model.number="p.points" type="number" min="0" /></label>
          <label>篮板<input v-model.number="p.rebounds" type="number" min="0" /></label>
          <label>助攻<input v-model.number="p.assists" type="number" min="0" /></label>
          <label>抢断<input v-model.number="p.steals" type="number" min="0" /></label>
          <label>盖帽<input v-model.number="p.blocks" type="number" min="0" /></label>
          <label>投篮命中<input v-model.number="p.fieldGoalsMade" type="number" min="0" /></label>
          <label>投篮出手<input v-model.number="p.fieldGoalsAttempted" type="number" min="0" /></label>
        </div>
      </div>

      <h4>比赛结算截图</h4>
      <div class="material-row">
        <label class="account-btn-secondary upload-btn">
          {{ uploading ? '上传中...' : '上传截图' }}
          <input type="file" accept="image/*" hidden :disabled="uploading" @change="onPickScreenshot" />
        </label>
        <div v-for="m in materialPreviews" :key="m.fileId" class="material-item">
          <img :src="m.url" :alt="m.name" class="material-thumb" />
          <button type="button" class="link-btn" @click="removeMaterial(m.fileId)">移除</button>
        </div>
      </div>

      <label class="remark-row">备注<textarea v-model="form.submitRemark" rows="2" placeholder="可选" /></label>

      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/match-data'))">返回</button>
        <button type="button" class="account-btn-primary" :disabled="submitting || uploading" @click="handleSubmit">
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

.stat-grid {
  grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
}

.period-row {
  grid-template-columns: 80px 1fr 1fr;
  align-items: center;
}

.stat-block {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--account-border, #eee);
}

.stat-block.compact strong {
  font-size: 13px;
}

input, textarea {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-top: 4px;
}

.warn {
  color: #c45656;
}

.material-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.upload-btn {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.material-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.material-thumb {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.link-btn {
  background: none;
  border: none;
  color: #c45656;
  cursor: pointer;
  font-size: 12px;
}

.remark-row {
  display: block;
  margin-bottom: 16px;
}

h4 {
  margin: 16px 0 8px;
  font-size: 14px;
}
</style>
