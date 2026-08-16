<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import FullMatchGameEditor from '@/components/account/events/FullMatchGameEditor.vue'
import {
  declareV4SeriesScore,
  getV4SeriesSubmissionWorkspace,
  resubmitV4SeriesSubmission,
  saveV4SeriesGame,
  submitV4SeriesSubmission,
} from '@/api/v4SeriesSubmission'
import { getAccountFileUrl, uploadAccountFile } from '@/api/accountFile'
import type { V4MatchSubmissionPlayerStat } from '@/api/types/v4MatchSubmission'
import type { V4MatchSubmissionPeriodScore, V4SeriesSubmissionWorkspace } from '@/api/types/v4SeriesSubmission'
import { seriesOptions, validateGame } from '@/utils/fullMatchSubmissionRules'
import { v4SubmissionStatusLabel } from '@/utils/v4AccountEventsMappers'

const route = useRoute()
const router = useRouter()
const matchSeriesId = computed(() => String(route.params.matchSeriesId || ''))
const loading = ref(false)
const saving = ref(false)
const workspace = ref<V4SeriesSubmissionWorkspace | null>(null)
const current = ref(0)
const previewUrl = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const declared = ref('3-1')

const emptyPlayer = (): V4MatchSubmissionPlayerStat => ({
  eventPlayerId: '', points: 0, rebound: 0, assist: 0, steal: 0, block: 0, foul: 0, turnover: 0,
  fieldGoalMade: 0, fieldGoalAttempt: 0, threePointMade: 0, threePointAttempt: 0, freeThrowMade: 0, freeThrowAttempt: 0,
})

const drafts = ref<Record<string, {
  homeStats: V4MatchSubmissionPlayerStat[]
  awayStats: V4MatchSubmissionPlayerStat[]
  periods: V4MatchSubmissionPeriodScore[]
  materialFileIds: string[]
  submitRemark: string
}>>({})
const dirtyIds = ref<Record<string, boolean>>({})

const canEdit = computed(() => Boolean(workspace.value?.canEdit))
const bestOf = computed(() => workspace.value?.bestOf || 5)
const options = computed(() => seriesOptions(bestOf.value))
const actualCount = computed(() => {
  const [home, away] = declared.value.split('-').map(Number)
  return (home || 0) + (away || 0)
})
const games = computed(() => (workspace.value?.games || []).filter((g) => (g.seriesGameNo || 0) <= actualCount.value))
const currentGame = computed(() => games.value[current.value] || games.value[0])
const currentDraft = computed(() => currentGame.value ? drafts.value[String(currentGame.value.matchId)] : null)
function isGameComplete(game: { matchId?: string; draftComplete?: number }) {
  return Boolean(game.draftComplete) && !dirtyIds.value[String(game.matchId)]
}

const completedCount = computed(() => games.value.filter((game) => isGameComplete(game)).length)
const canSubmitAll = computed(() =>
  Boolean(workspace.value?.canEdit)
  && games.value.length > 0
  && games.value.every((game) => isGameComplete(game)),
)

const currentValidation = computed(() => {
  if (!currentGame.value || !currentDraft.value || !workspace.value) return { errors: [] as string[], homeScore: 0, awayScore: 0, pass: false, status: 'empty' as const }
  return validateGame({
    gameLabel: `G${currentGame.value.seriesGameNo}`,
    homeName: workspace.value.homeTeamName || '主队',
    awayName: workspace.value.awayTeamName || '客队',
    homePlayers: currentDraft.value.homeStats,
    awayPlayers: currentDraft.value.awayStats,
    periods: currentDraft.value.periods,
    materialCount: currentDraft.value.materialFileIds.length,
    requireComplete: true,
  })
})

function ensureDrafts(ws: V4SeriesSubmissionWorkspace) {
  for (const game of ws.games || []) {
    const id = String(game.matchId)
    if (drafts.value[id]) continue
    const homeStats = (game.homePlayerStats || []).map((row) => ({ ...emptyPlayer(), ...row, eventPlayerId: String(row.eventPlayerId || '') }))
    const awayStats = (game.awayPlayerStats || []).map((row) => ({ ...emptyPlayer(), ...row, eventPlayerId: String(row.eventPlayerId || '') }))
    while (homeStats.length < 5) homeStats.push(emptyPlayer())
    while (awayStats.length < 5) awayStats.push(emptyPlayer())
    drafts.value[id] = {
      homeStats,
      awayStats,
      periods: game.periodScores?.length ? game.periodScores : [1, 2, 3, 4].map((periodNo) => ({ periodNo, homeScore: 0, awayScore: 0 })),
      materialFileIds: (game.materialFileIds || []).map(String),
      submitRemark: game.submitRemark || '',
    }
  }
}

async function loadWorkspace() {
  loading.value = true
  try {
    const res = await getV4SeriesSubmissionWorkspace(matchSeriesId.value)
    workspace.value = res.data || null
    if (!workspace.value) return
    if (workspace.value.declaredHomeWinCount != null && workspace.value.declaredAwayWinCount != null) {
      declared.value = `${workspace.value.declaredHomeWinCount}-${workspace.value.declaredAwayWinCount}`
    } else {
      const need = Math.floor((workspace.value.bestOf || 5) / 2) + 1
      declared.value = `${need}-0`
    }
    ensureDrafts(workspace.value)
    dirtyIds.value = {}
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function declaredPair() {
  const [home, away] = declared.value.split('-').map(Number)
  return { declaredHomeWinCount: home, declaredAwayWinCount: away }
}

async function persistDeclared() {
  await declareV4SeriesScore({ matchSeriesId: matchSeriesId.value, ...declaredPair() })
}

async function onDeclaredChange() {
  if (!canEdit.value) return
  try {
    await persistDeclared()
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '系列赛比分确认失败')
  }
}

function markCurrentDirty() {
  if (!currentGame.value) return
  dirtyIds.value = { ...dirtyIds.value, [String(currentGame.value.matchId)]: true }
}

async function saveCurrent(complete: boolean) {
  if (!canEdit.value || !currentGame.value || !currentDraft.value) return
  if (complete && !currentValidation.value.pass) {
    ElMessage.error(currentValidation.value.errors[0] || '请先填写完整比赛数据')
    return
  }
  saving.value = true
  try {
    await persistDeclared()
    await saveV4SeriesGame({
      matchSeriesId: matchSeriesId.value,
      matchId: String(currentGame.value.matchId),
      ...declaredPair(),
      homeScore: currentValidation.value.homeScore,
      awayScore: currentValidation.value.awayScore,
      homePlayerStats: currentDraft.value.homeStats.filter((row) => String(row.eventPlayerId || '').trim()),
      awayPlayerStats: currentDraft.value.awayStats.filter((row) => String(row.eventPlayerId || '').trim()),
      periodScores: currentDraft.value.periods,
      materialFileIds: currentDraft.value.materialFileIds,
      submitRemark: currentDraft.value.submitRemark,
      draftComplete: complete,
    })
    ElMessage.success(complete ? '本场已完成' : '本场已保存')
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function saveAndNext() {
  await saveCurrent(true)
  if (current.value < games.value.length - 1) current.value += 1
}

async function submitSeries() {
  if (!canSubmitAll.value || !workspace.value) return
  saving.value = true
  try {
    await persistDeclared()
    await submitV4SeriesSubmission({
      seriesSubmissionId: workspace.value.seriesSubmissionId,
      matchSeriesId: matchSeriesId.value,
    })
    ElMessage.success('已提交整个系列赛审核')
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    saving.value = false
  }
}

async function resubmit() {
  if (!workspace.value?.seriesSubmissionId) return
  saving.value = true
  try {
    await resubmitV4SeriesSubmission({
      oldSeriesSubmissionId: String(workspace.value.seriesSubmissionId),
      matchSeriesId: matchSeriesId.value,
      ...declaredPair(),
    })
    ElMessage.success('已生成新版本草稿，请修改后再次提交')
    await loadWorkspace()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '重提失败')
  } finally {
    saving.value = false
  }
}

async function handleUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length || !currentDraft.value || !canEdit.value) return
  for (const file of files) {
    const res = await uploadAccountFile(file, 'MATCH_DATA_SCREENSHOT')
    const fileId = String(res.data?.fileId || '')
    if (fileId) currentDraft.value.materialFileIds.push(fileId)
  }
  markCurrentDirty()
}

onMounted(() => { void loadWorkspace() })
</script>

<template>
  <div class="account-view ss-page">
    <AccountPageHeader
      zh-title="系列赛数据提交"
      en-title="SERIES DATA SUBMISSION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-events?tab=schedule' },
        { label: '系列赛数据' },
      ]"
    />
    <AccountStatusBanner
      title="系列赛数据提交"
      :description="workspace ? `${workspace.homeTeamName} VS ${workspace.awayTeamName}` : '加载中'"
      :status-text="v4SubmissionStatusLabel(workspace?.status)"
      :status-type="workspace?.status === 'APPROVED' ? 'success' : workspace?.status === 'REJECTED' ? 'danger' : 'info'"
    />
    <p v-if="loading">加载中…</p>
    <template v-else-if="workspace && currentGame && currentDraft">
      <p v-if="workspace.rejectReason" class="ss-reject">驳回原因：{{ workspace.rejectReason }}</p>
      <section class="ss-card">
        <div class="ss-title">系列赛最终结果</div>
        <div class="ss-row">
          <strong>{{ workspace.homeTeamName }}</strong>
          <select v-model="declared" :disabled="!canEdit" @change="onDeclaredChange">
            <option v-for="item in options" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
          <strong>{{ workspace.awayTeamName }}</strong>
        </div>
        <p>赛制 BO{{ bestOf }} · 实际完成 {{ actualCount }} 场 · 胜者 {{ Number(declared.split('-')[0]) > Number(declared.split('-')[1]) ? workspace.homeTeamName : workspace.awayTeamName }}</p>
      </section>
      <section class="ss-tabs">
        <button
          v-for="(game, index) in games"
          :key="game.matchId"
          type="button"
          :class="{ active: index === current }"
          @click="current = index"
        >
          <div>G{{ game.seriesGameNo }}</div>
          <small>{{ isGameComplete(game) ? '已完成' : '待完善' }}</small>
        </button>
      </section>
      <div class="ss-workspace">
        <section class="ss-card">
          <div class="ss-title">G{{ currentGame.seriesGameNo }} · 比赛数据</div>
          <FullMatchGameEditor
            :game-label="`G${currentGame.seriesGameNo}`"
            :home-name="workspace.homeTeamName || '主队'"
            :away-name="workspace.awayTeamName || '客队'"
            :home-players="workspace.homeEligiblePlayers || []"
            :away-players="workspace.awayEligiblePlayers || []"
            :home-stats="currentDraft.homeStats"
            :away-stats="currentDraft.awayStats"
            :periods="currentDraft.periods"
            :material-file-ids="currentDraft.materialFileIds"
            :readonly="!canEdit"
            @update:home-stats="currentDraft.homeStats = $event; markCurrentDirty()"
            @update:away-stats="currentDraft.awayStats = $event; markCurrentDirty()"
            @update:periods="currentDraft.periods = $event; markCurrentDirty()"
            @upload="fileInput?.click()"
            @remove-material="currentDraft.materialFileIds.splice($event, 1); markCurrentDirty()"
            @preview="previewUrl = $event"
          />
          <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="handleUpload" />
          <div class="ss-actions">
            <button type="button" class="account-btn-secondary" :disabled="current === 0" @click="current -= 1">上一场</button>
            <button v-if="canEdit" type="button" class="account-btn-secondary" :disabled="saving" @click="saveCurrent(false)">保存本场</button>
            <button v-if="canEdit" type="button" class="account-btn-primary" :disabled="saving || !currentValidation.pass" @click="saveCurrent(true)">保存本场并完成</button>
            <button v-if="canEdit" type="button" class="account-btn-primary" :disabled="saving || !currentValidation.pass" @click="saveAndNext">保存并进入下一场</button>
          </div>
        </section>
        <aside class="ss-card ss-progress">
          <div class="ss-title">系列赛提交进度</div>
          <div v-for="game in games" :key="`p-${game.matchId}`" class="ss-pitem">
            <span>G{{ game.seriesGameNo }}</span>
            <b :class="isGameComplete(game) ? 'ok' : 'wait'">{{ isGameComplete(game) ? '✓ 已完成' : '! 待完善' }}</b>
          </div>
          <p>{{ completedCount }} / {{ actualCount }}</p>
          <button
            v-if="canEdit"
            type="button"
            class="account-btn-primary ss-submit"
            :disabled="!canSubmitAll || saving"
            @click="submitSeries"
          >
            提交整个系列赛审核
          </button>
          <button
            v-if="workspace.status === 'REJECTED'"
            type="button"
            class="account-btn-secondary ss-submit"
            :disabled="saving"
            @click="resubmit"
          >
            驳回后重提
          </button>
          <button type="button" class="account-btn-secondary ss-submit" @click="router.push('/account/my-events?tab=schedule')">返回我的赛程</button>
        </aside>
      </div>
    </template>
    <div v-if="previewUrl" class="ss-preview" @click.self="previewUrl = ''">
      <img :src="getAccountFileUrl(previewUrl)" alt="预览" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
:global(.member-account-layout:has(.ss-page)),
:global(.account-center-vars:has(.ss-page)) {
  --account-content-max: 1680px;
}
.ss-card { background: rgba(255,255,255,.55); border: 1px solid #d7d2cc; border-radius: 7px; padding: 16px; margin-bottom: 14px; }
.ss-title { font-weight: 900; margin-bottom: 10px; }
.ss-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.ss-tabs { display: grid; grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); gap: 8px; margin-bottom: 14px; }
.ss-tabs button { border: 1px solid #cfc9c3; background: #fff; padding: 10px; text-align: left; }
.ss-tabs button.active { border-color: #b90000; }
.ss-workspace { display: grid; grid-template-columns: minmax(0,1fr) 280px; gap: 14px; }
.ss-progress { position: sticky; top: 80px; }
.ss-pitem { display: flex; justify-content: space-between; border-bottom: 1px solid #eee; padding: 6px 0; }
.ss-pitem .ok { color: #148518; }
.ss-pitem .wait { color: #e47d00; }
.ss-actions, .ss-submit { display: flex; gap: 10px; margin-top: 14px; }
.ss-submit { width: 100%; }
.ss-reject { color: #b42318; }
.ss-preview { position: fixed; inset: 0; z-index: 40; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,.72); img { max-width: 90vw; max-height: 90vh; } }
@media (max-width: 1100px) { .ss-workspace { grid-template-columns: 1fr; } }
</style>
