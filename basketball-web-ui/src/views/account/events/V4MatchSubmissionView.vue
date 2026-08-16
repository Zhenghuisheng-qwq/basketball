<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import FullMatchGameEditor from '@/components/account/events/FullMatchGameEditor.vue'
import {
  getV4MatchSubmissionDetail,
  resubmitV4MatchSubmission,
  saveV4MatchSubmissionDraft,
  submitV4MatchSubmission,
} from '@/api/v4MatchSubmission'
import { getAccountFileUrl, uploadAccountFile } from '@/api/accountFile'
import type {
  V4MatchSubmissionDetail,
  V4MatchSubmissionEligiblePlayer,
  V4MatchSubmissionPlayerStat,
  V4MatchSubmissionTeamStat,
} from '@/api/types/v4MatchSubmission'
import { v4SubmissionStatusLabel } from '@/utils/v4AccountEventsMappers'
import { v4BestOfLabel, v4MatchSeriesStatusLabel, v4PlatformLabel } from '@/utils/v4Display'
import { formatDateTimeShort } from '@/utils/participationMappers'
import { requiresPeriodScores, validateGame } from '@/utils/fullMatchSubmissionRules'
import type { V4MatchSubmissionPeriodScore } from '@/api/types/v4SeriesSubmission'

const MAX_SCREENSHOTS = 6
const PLAYER_STAT_FIELDS = ['points', 'rebound', 'assist', 'steal', 'block', 'foul', 'turnover'] as const

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const previewUrl = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
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

const skipped = computed(() => Boolean(detail.value?.seriesContext?.skipped))
const isFullMatch = computed(() => (detail.value?.submissionMode || 'WINNER_FULL_MATCH') !== 'LEGACY_TEAM_SIDE')
const emptyPlayer = (): V4MatchSubmissionPlayerStat => ({
  eventPlayerId: '', points: 0, rebound: 0, assist: 0, steal: 0, block: 0, foul: 0, turnover: 0,
  fieldGoalMade: 0, fieldGoalAttempt: 0, threePointMade: 0, threePointAttempt: 0, freeThrowMade: 0, freeThrowAttempt: 0,
})
function normalizePeriods(rows?: Array<{ periodNo: number; homeScore?: number; awayScore?: number }>): V4MatchSubmissionPeriodScore[] {
  if (!rows?.length) {
    return [1, 2, 3, 4].map((periodNo) => ({ periodNo, homeScore: 0, awayScore: 0 }))
  }
  return rows.map((row) => ({
    periodNo: row.periodNo,
    homeScore: row.homeScore ?? 0,
    awayScore: row.awayScore ?? 0,
  }))
}

const fullForm = ref({
  homeStats: [emptyPlayer(), emptyPlayer(), emptyPlayer(), emptyPlayer(), emptyPlayer()] as V4MatchSubmissionPlayerStat[],
  awayStats: [emptyPlayer(), emptyPlayer(), emptyPlayer(), emptyPlayer(), emptyPlayer()] as V4MatchSubmissionPlayerStat[],
  periods: normalizePeriods(),
})
const fullValidation = computed(() => {
  if (!detail.value) return { errors: [] as string[], homeScore: 0, awayScore: 0, pass: false }
  return validateGame({
    gameLabel: '本场',
    homeName: detail.value.homeTeamName || '主队',
    awayName: detail.value.awayTeamName || '客队',
    homePlayers: fullForm.value.homeStats,
    awayPlayers: fullForm.value.awayStats,
    periods: fullForm.value.periods,
    materialCount: form.value.materialFileIds.length,
    requireComplete: true,
    gameMode: detail.value.gameMode,
  })
})

const eligiblePlayers = computed<V4MatchSubmissionEligiblePlayer[]>(() => detail.value?.eligiblePlayers || [])

const lineupMin = computed(() => {
  if (detail.value?.lineupMinCount && detail.value.lineupMinCount > 0) return detail.value.lineupMinCount
  return (detail.value?.gameMode || '').toUpperCase() === 'THREE_V_THREE' ? 3 : 5
})

const lineupMax = computed(() => {
  const rosterMax = detail.value?.lineupMaxCount && detail.value.lineupMaxCount > 0 ? detail.value.lineupMaxCount : 8
  const playerMax = eligiblePlayers.value.length || rosterMax
  return Math.max(1, Math.min(rosterMax, playerMax))
})

const form = ref({
  homeScore: 0,
  awayScore: 0,
  submitRemark: '',
  materialFileIds: [] as string[],
  teamStat: emptyTeamStat(),
  playerStats: [] as V4MatchSubmissionPlayerStat[],
})

function emptyTeamStat(): V4MatchSubmissionTeamStat {
  return {
    score: 0,
    rebound: 0,
    assist: 0,
    steal: 0,
    block: 0,
    turnover: 0,
    foul: 0,
    fieldGoalMade: 0,
    fieldGoalAttempt: 0,
    threePointMade: 0,
    threePointAttempt: 0,
    freeThrowMade: 0,
    freeThrowAttempt: 0,
  }
}

function emptyPlayerRow(): V4MatchSubmissionPlayerStat {
  return {
    eventPlayerId: '',
    points: 0,
    rebound: 0,
    assist: 0,
    steal: 0,
    block: 0,
    turnover: 0,
    foul: 0,
  }
}

function playerLabel(player: V4MatchSubmissionEligiblePlayer): string {
  const name = player.playerName || player.gameId || '未命名球员'
  const extra = [player.jerseyNo ? `#${player.jerseyNo}` : '', player.isCaptain ? '队长' : '']
    .filter(Boolean)
    .join(' · ')
  return extra ? `${name}（${extra}）` : name
}

function availablePlayers(index: number): V4MatchSubmissionEligiblePlayer[] {
  const selected = new Set(
    form.value.playerStats
      .map((row, i) => (i === index ? '' : String(row.eventPlayerId || '')))
      .filter(Boolean),
  )
  return eligiblePlayers.value.filter((player) => !selected.has(String(player.eventPlayerId)))
}

function ensurePlayerRows(existing: V4MatchSubmissionPlayerStat[]) {
  const rows = existing.map((item) => ({ ...emptyPlayerRow(), ...item, eventPlayerId: String(item.eventPlayerId || '') }))
  const minRows = Math.min(lineupMin.value, lineupMax.value)
  while (rows.length < minRows) rows.push(emptyPlayerRow())
  form.value.playerStats = rows.slice(0, lineupMax.value)
}

function addPlayerStatRow() {
  if (form.value.playerStats.length >= lineupMax.value) {
    ElMessage.warning(`最多添加 ${lineupMax.value} 名球员`)
    return
  }
  form.value.playerStats.push(emptyPlayerRow())
}

function removePlayerStatRow(index: number) {
  if (form.value.playerStats.length <= 1) return
  form.value.playerStats.splice(index, 1)
}

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
    form.value.teamStat = { ...emptyTeamStat(), ...(detail.value.teamStat || {}) }
    ensurePlayerRows(detail.value.playerStats || [])
    const homeStats = (detail.value.homePlayerStats || []).map((row) => ({ ...emptyPlayer(), ...row, eventPlayerId: String(row.eventPlayerId || '') }))
    const awayStats = (detail.value.awayPlayerStats || []).map((row) => ({ ...emptyPlayer(), ...row, eventPlayerId: String(row.eventPlayerId || '') }))
    while (homeStats.length < 5) homeStats.push(emptyPlayer())
    while (awayStats.length < 5) awayStats.push(emptyPlayer())
    fullForm.value.homeStats = homeStats
    fullForm.value.awayStats = awayStats
    fullForm.value.periods = normalizePeriods(detail.value.periodScores)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function buildSavePayload() {
  if (isFullMatch.value) {
    return {
      matchId: matchId.value,
      submitEventTeamId: submitEventTeamId.value,
      homeScore: fullValidation.value.homeScore,
      awayScore: fullValidation.value.awayScore,
      submitRemark: form.value.submitRemark,
      materialFileIds: form.value.materialFileIds,
      homePlayerStats: fullForm.value.homeStats.filter((row) => String(row.eventPlayerId || '').trim()),
      awayPlayerStats: fullForm.value.awayStats.filter((row) => String(row.eventPlayerId || '').trim()),
      periodScores: requiresPeriodScores(detail.value?.gameMode) ? fullForm.value.periods : [],
      submissionMode: 'WINNER_FULL_MATCH',
      draftComplete: Boolean(fullValidation.value.pass),
    }
  }
  const playerStats = form.value.playerStats
    .filter((row) => String(row.eventPlayerId || '').trim())
    .map((row) => ({
      ...row,
      eventPlayerId: String(row.eventPlayerId),
    }))
  return {
    matchId: matchId.value,
    submitEventTeamId: submitEventTeamId.value,
    homeScore: form.value.homeScore,
    awayScore: form.value.awayScore,
    submitRemark: form.value.submitRemark,
    materialFileIds: form.value.materialFileIds,
    teamStat: form.value.teamStat,
    playerStats,
  }
}

async function handleSaveDraft() {
  if (!canEdit.value || skipped.value) return
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
  if (!canEdit.value || skipped.value) return
  if (isFullMatch.value && !fullValidation.value.pass) {
    ElMessage.warning(fullValidation.value.errors[0] || '请先填写完整比赛数据')
    return
  }
  if (!isFullMatch.value && (form.value.homeScore == null || form.value.awayScore == null)) {
    ElMessage.warning('请填写最终比赛比分')
    return
  }
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

function triggerUpload() {
  if (!canEdit.value || form.value.materialFileIds.length >= MAX_SCREENSHOTS) return
  fileInput.value?.click()
}

async function handleUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length || !canEdit.value) return
  const remain = MAX_SCREENSHOTS - form.value.materialFileIds.length
  if (remain <= 0) {
    ElMessage.warning(`最多上传 ${MAX_SCREENSHOTS} 张截图`)
    return
  }
  uploading.value = true
  try {
    for (const file of files.slice(0, remain)) {
      if (!file.type.startsWith('image/')) {
        ElMessage.error('请上传图片文件')
        continue
      }
      if (file.size > 8 * 1024 * 1024) {
        ElMessage.error('单张截图不能超过 8MB')
        continue
      }
      const res = await uploadAccountFile(file, 'MATCH_DATA_SCREENSHOT')
      const fileId = String(res.data?.fileId || '')
      if (fileId) form.value.materialFileIds.push(fileId)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploading.value = false
  }
}

function removeScreenshot(index: number) {
  if (!canEdit.value) return
  form.value.materialFileIds.splice(index, 1)
}

const contextLine = computed(() => {
  const d = detail.value
  if (!d) return ''
  const parts = [d.seasonTitle, d.divisionName || v4PlatformLabel(d.platformCode), d.roundName || (d.roundNo != null ? `第 ${d.roundNo} 轮` : '')]
  return parts.filter(Boolean).join(' · ')
})

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <div class="account-view fm-page">
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
      title="比赛数据提交"
      :description="detail ? `${detail.homeTeamName} VS ${detail.awayTeamName}` : '加载中'"
      :status-text="v4SubmissionStatusLabel(detail?.submissionStatus)"
      :status-type="detail?.submissionStatus === 'APPROVED' ? 'success' : detail?.submissionStatus === 'REJECTED' ? 'danger' : 'info'"
    />

    <p v-if="loading" class="v4-sub-loading">加载中…</p>

    <template v-else-if="detail">
      <AccountPanel title="比赛信息">
        <p>{{ contextLine }}</p>
        <p>比赛时间：{{ formatDateTimeShort(detail.matchTime) }}</p>
        <p v-if="detail.seriesContext" class="v4-sub-series">
          {{ detail.seriesContext.seriesCode || '系列赛' }}
          · {{ v4BestOfLabel(detail.seriesContext.bestOf) }}
          · Game {{ detail.seriesContext.seriesGameNo }}
          · 系列赛当前比分：{{ detail.seriesContext.homeWinCount ?? 0 }} : {{ detail.seriesContext.awayWinCount ?? 0 }}
          · {{ detail.seriesContext.seriesStatusName || v4MatchSeriesStatusLabel(detail.seriesContext.seriesStatus) }}
        </p>
        <p v-if="detail.rejectReason" class="v4-sub-reject">驳回原因：{{ detail.rejectReason }}</p>
        <p v-if="skipped" class="v4-sub-readonly">{{ detail.seriesContext?.skippedReason || '该系列赛已决出胜负，本局无需进行' }}</p>
        <p v-else-if="isReadonly" class="v4-sub-readonly">当前状态只读，不可覆盖保存。</p>
        <input ref="fileInput" type="file" accept="image/*" multiple class="v4-sub-file" @change="handleUpload" />
      </AccountPanel>

      <AccountPanel v-if="isFullMatch" title="双方完整比赛数据">
        <FullMatchGameEditor
          game-label="本场"
          :home-name="detail.homeTeamName || '主队'"
          :away-name="detail.awayTeamName || '客队'"
          :home-players="detail.homeEligiblePlayers || eligiblePlayers"
          :away-players="detail.awayEligiblePlayers || []"
          :home-stats="fullForm.homeStats"
          :away-stats="fullForm.awayStats"
          :periods="fullForm.periods"
          :material-file-ids="form.materialFileIds"
          :game-mode="detail.gameMode"
          :readonly="!canEdit || skipped"
          @update:home-stats="fullForm.homeStats = $event"
          @update:away-stats="fullForm.awayStats = $event"
          @update:periods="fullForm.periods = $event"
          @upload="triggerUpload"
          @remove-material="removeScreenshot"
          @preview="previewUrl = getAccountFileUrl($event)"
        />
      </AccountPanel>

      <AccountPanel v-if="!isFullMatch" title="对阵与总比分">
        <p class="v4-sub-hint">请填写最终比赛比分。本轮仍按手工总比分结算，不会自动汇总球员得分。</p>
        <div class="v4-sub-scores">
          <label>
            主队 {{ detail.homeTeamName }}
            <input v-model.number="form.homeScore" type="number" min="0" :disabled="!canEdit || skipped" />
          </label>
          <label>
            客队 {{ detail.awayTeamName }}
            <input v-model.number="form.awayScore" type="number" min="0" :disabled="!canEdit || skipped" />
          </label>
        </div>
      </AccountPanel>

      <AccountPanel v-if="!isFullMatch" title="本方球队技术统计">
        <p class="v4-sub-team-name">{{ detail.submitTeamName || '本方球队' }}</p>
        <div class="v4-sub-grid">
          <label v-for="field in ['score', 'rebound', 'assist', 'steal', 'block', 'turnover', 'foul'] as const" :key="field">
            {{ field === 'score' ? '得分' : field === 'rebound' ? '篮板' : field === 'assist' ? '助攻' : field === 'steal' ? '抢断' : field === 'block' ? '盖帽' : field === 'turnover' ? '失误' : '犯规' }}
            <input v-model.number="form.teamStat[field]" type="number" min="0" :disabled="!canEdit || skipped" />
          </label>
        </div>
        <div class="v4-sub-grid v4-sub-grid--shooting">
          <label>FGM<input v-model.number="form.teamStat.fieldGoalMade" type="number" min="0" :disabled="!canEdit || skipped" /></label>
          <label>FGA<input v-model.number="form.teamStat.fieldGoalAttempt" type="number" min="0" :disabled="!canEdit || skipped" /></label>
          <label>3PM<input v-model.number="form.teamStat.threePointMade" type="number" min="0" :disabled="!canEdit || skipped" /></label>
          <label>3PA<input v-model.number="form.teamStat.threePointAttempt" type="number" min="0" :disabled="!canEdit || skipped" /></label>
          <label>FTM<input v-model.number="form.teamStat.freeThrowMade" type="number" min="0" :disabled="!canEdit || skipped" /></label>
          <label>FTA<input v-model.number="form.teamStat.freeThrowAttempt" type="number" min="0" :disabled="!canEdit || skipped" /></label>
        </div>
      </AccountPanel>

      <AccountPanel v-if="!isFullMatch" title="本方球员技术统计">
        <div class="v4-sub-table-wrap">
          <table class="v4-sub-table">
            <thead>
              <tr>
                <th>#</th>
                <th>球员</th>
                <th>PTS</th>
                <th>REB</th>
                <th>AST</th>
                <th>STL</th>
                <th>BLK</th>
                <th>FLS</th>
                <th>TO</th>
                <th v-if="canEdit && !skipped"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in form.playerStats" :key="index">
                <td>{{ index + 1 }}</td>
                <td>
                  <select v-model="row.eventPlayerId" :disabled="!canEdit || skipped">
                    <option value="">选择球员</option>
                    <option
                      v-for="player in availablePlayers(index)"
                      :key="String(player.eventPlayerId)"
                      :value="String(player.eventPlayerId)"
                    >
                      {{ playerLabel(player) }}
                    </option>
                  </select>
                </td>
                <td v-for="field in PLAYER_STAT_FIELDS" :key="field">
                  <input v-model.number="row[field]" type="number" min="0" :disabled="!canEdit || skipped" />
                </td>
                <td v-if="canEdit && !skipped">
                  <button type="button" class="account-btn-secondary v4-sub-mini" @click="removePlayerStatRow(index)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <button
          v-if="canEdit && !skipped"
          type="button"
          class="account-btn-secondary"
          :disabled="form.playerStats.length >= lineupMax"
          @click="addPlayerStatRow"
        >
          添加球员
        </button>
      </AccountPanel>

      <AccountPanel v-if="!isFullMatch" title="比赛截图 / 比赛凭证">
        <p class="v4-sub-hint">请上传本局比赛结果或技术统计截图，供管理员审核。最多 {{ MAX_SCREENSHOTS }} 张。</p>
        <button
          v-if="canEdit && !skipped"
          type="button"
          class="account-btn-secondary"
          :disabled="uploading || form.materialFileIds.length >= MAX_SCREENSHOTS"
          @click="triggerUpload"
        >
          {{ uploading ? '上传中…' : '+ 上传截图' }}
        </button>
        <div v-if="form.materialFileIds.length" class="v4-sub-shots">
          <figure v-for="(fileId, index) in form.materialFileIds" :key="fileId" class="v4-sub-shot">
            <button type="button" class="v4-sub-shot__thumb" @click="previewUrl = getAccountFileUrl(fileId)">
              <img :src="getAccountFileUrl(fileId)" alt="比赛截图" />
            </button>
            <button v-if="canEdit && !skipped" type="button" class="v4-sub-shot__del" @click="removeScreenshot(index)">删除</button>
          </figure>
        </div>
      </AccountPanel>

      <AccountPanel title="备注">
        <textarea v-model="form.submitRemark" rows="3" :disabled="!canEdit || skipped" class="v4-sub-textarea" />
      </AccountPanel>

      <div class="v4-sub-actions">
        <button v-if="canEdit && !skipped" type="button" class="account-btn-secondary" :disabled="saving" @click="handleSaveDraft">保存草稿</button>
        <button v-if="canEdit && !skipped" type="button" class="account-btn-primary" :disabled="submitting || saving || (isFullMatch && !fullValidation.pass)" @click="handleSubmitAudit">提交审核</button>
        <button
          v-if="detail.submissionStatus === 'REJECTED' && !skipped"
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

    <div v-if="previewUrl" class="v4-sub-preview" @click.self="previewUrl = ''">
      <img :src="previewUrl" alt="截图预览" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

:global(.member-account-layout:has(.fm-page)),
:global(.account-center-vars:has(.fm-page)) {
  --account-content-max: 1680px;
}

.v4-sub-loading,
.v4-sub-hint,
.v4-sub-team-name {
  color: #696969;
}

.v4-sub-hint {
  margin-bottom: 12px;
  font-size: 13px;
}

.v4-sub-team-name {
  margin-bottom: 10px;
  font-weight: 600;
  color: #111;
}

.v4-sub-series,
.v4-sub-reject {
  margin-top: 8px;
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
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
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

.v4-sub-grid--shooting {
  margin-top: 12px;
}

.v4-sub-table-wrap {
  overflow-x: auto;
  margin-bottom: 12px;
}

.v4-sub-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
  font-size: 13px;

  th,
  td {
    padding: 8px 6px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    text-align: left;
  }

  input,
  select {
    width: 100%;
    min-width: 64px;
    height: 36px;
    padding: 0 6px;
    border: 1px solid rgba(0, 0, 0, 0.18);
  }
}

.v4-sub-mini {
  padding: 0 8px;
  height: 32px;
}

.v4-sub-file {
  display: none;
}

.v4-sub-shots {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 14px;
}

.v4-sub-shot {
  margin: 0;
  width: 120px;
}

.v4-sub-shot__thumb {
  display: block;
  width: 120px;
  height: 80px;
  padding: 0;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: #f6f4f0;
  cursor: pointer;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.v4-sub-shot__del {
  margin-top: 6px;
  border: 0;
  background: none;
  color: #b42318;
  cursor: pointer;
}

.v4-sub-textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid rgba(0, 0, 0, 0.18);
}

.v4-sub-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.v4-sub-preview {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.72);

  img {
    max-width: 90vw;
    max-height: 90vh;
  }
}
</style>
