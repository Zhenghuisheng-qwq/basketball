<template>
  <div class="app-container">
    <el-alert
      title="技术统计榜与积分榜分开。这里配置资格、试算并发布公开快照。赛程发布不会自动公开技术榜。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />
    <DivisionEventSelector v-if="!divisionId" entry="overview" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <DivisionWorkspaceNav />
      <div v-loading="loading">
        <el-form :inline="true" class="mb8">
          <el-form-item label="统计范围">
            <el-select v-model="scopeKey" style="width: 240px" @change="onScopeChange">
              <el-option v-for="item in scopes" :key="item.scopeKey" :label="item.stageName || item.scopeKey" :value="item.scopeKey" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-tag v-if="current?.doubleElimination" type="danger">双败暂不可发布</el-tag>
            <el-tag v-if="draft" class="ml8">草稿 {{ draft.lifecycle }} v{{ draft.versionNo }}</el-tag>
            <el-tag v-if="active" type="success" class="ml8">生效 v{{ active.versionNo }}</el-tag>
          </el-form-item>
        </el-form>

        <el-form label-width="140px" :disabled="!draft || draft.lifecycle === 'READY'">
          <el-form-item label="上榜范围">
            <el-radio-group v-model="config.rankingAudience">
              <el-radio label="QUALIFIED_ONLY">只排达标对象</el-radio>
              <el-radio label="ALL_PARTICIPANTS">全部参赛对象</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="球队最低局数">
            <el-input-number v-model="config.team.minimumTeamGames" :min="0" />
          </el-form-item>
          <el-form-item label="球队最低轮次">
            <el-input-number v-model="config.team.minimumTeamPlayedRounds" :min="0" :disabled="current?.roundLimitsForbidden" />
          </el-form-item>
          <el-form-item label="球队最低系列赛">
            <el-input-number v-model="config.team.minimumTeamPlayedSeries" :min="0" />
          </el-form-item>
          <el-form-item label="应赛履行率">
            <el-input-number v-model="config.team.minimumDueFixtureRate" :min="0" :max="1" :step="0.05" :precision="2" />
          </el-form-item>
          <el-form-item label="球员须属达标队">
            <el-switch v-model="config.player.requireEligibleTeam" />
          </el-form-item>
          <el-form-item label="球员最低局数">
            <el-input-number v-model="config.player.minimumPlayerGames" :min="0" />
          </el-form-item>
          <el-form-item label="球员出场比例">
            <el-input-number v-model="config.player.minimumPlayerTeamRate" :min="0" :max="1" :step="0.05" :precision="2" />
          </el-form-item>
          <el-form-item label="命中率出手门槛">
            <div>
              <div v-for="metric in metricKeys" :key="metric" class="metric-row">
                <span>{{ metricLabel(metric) }}</span>
                <el-switch v-model="config.metricGates[metric].enabled" />
                <el-input-number v-model="config.metricGates[metric].minimumAttemptsPerAppearance" :min="0" />
                <span>次/场</span>
              </div>
            </div>
          </el-form-item>
        </el-form>

        <div class="mb8">
          <el-button v-hasPermi="['basket:v4Leaderboard:edit']" type="primary" :disabled="!!draft" @click="createDraft">新建草稿</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:edit']" :disabled="!draft" @click="saveDraft">保存</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:edit']" :disabled="!draft" @click="loadTemplate">载入模板</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:validate']" :disabled="!draft" @click="validateDraft">校验</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:edit']" :disabled="draft?.lifecycle !== 'READY'" @click="reopenDraft">退回草稿</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:view']" :disabled="!draft" @click="preview">试算</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:publish']" type="success" :disabled="draft?.lifecycle !== 'READY'" @click="publish">发布</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:recalc']" @click="recalculate">重算已发布榜</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:archive']" :disabled="!active" @click="archive">归档</el-button>
          <el-button v-hasPermi="['basket:v4Leaderboard:publish']" @click="featureDivision">设为首页榜</el-button>
        </div>

        <el-alert v-if="previewBody" class="mb8" type="success" :closable="false">
          <template #title>
            试算：球队 {{ previewBody.teamEligible }}/{{ previewBody.teamCount }} 达标，球员 {{ previewBody.playerEligible }}/{{ previewBody.playerCount }} 达标
            <span v-if="previewBody.emptyReason === 'NO_ELIGIBLE_SUBJECTS'">，没有达标球队</span>
          </template>
          <div v-if="diffLines.length">规则差异：{{ diffLines.join('；') }}</div>
        </el-alert>

        <el-table v-if="entries.length" :data="entries" border size="small">
          <el-table-column label="对象" prop="subjectType" width="90" />
          <el-table-column label="球队" prop="eventTeamId" width="120" />
          <el-table-column label="球员" prop="eventPlayerId" width="120" />
          <el-table-column label="达标" width="80">
            <template #default="s">{{ s.row.eligible === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="未达标原因" prop="failedConditions" min-width="180" />
          <el-table-column label="有效局" prop="validGames" width="80" />
          <el-table-column label="轮次" prop="playedRounds" width="70" />
          <el-table-column label="系列赛" prop="playedSeries" width="80" />
          <el-table-column label="得分" prop="points" width="70" />
          <el-table-column label="名次" prop="rankNo" width="70" />
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" name="EventV4Leaderboard">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionWorkspaceNav from '../components/DivisionWorkspaceNav.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'
import {
  archiveV4Leaderboard,
  createV4LeaderboardDraft,
  getV4LeaderboardScopes,
  listV4LeaderboardEligibility,
  loadV4LeaderboardTemplate,
  previewV4Leaderboard,
  publishV4Leaderboard,
  recalculateV4Leaderboard,
  reopenV4LeaderboardDraft,
  saveV4LeaderboardDraft,
  updateV4FeaturedDivision,
  validateV4LeaderboardDraft,
  type LeaderboardConfig,
  type LeaderboardPolicy,
  type LeaderboardScopeRow
} from '@/api/cnpa/event-v4/leaderboard'

const { overview, selectDivision, clearDivision, divisionId } = useDivisionWorkspace('leaderboard')
const loading = ref(false)
const scopes = ref<LeaderboardScopeRow[]>([])
const scopeKey = ref('')
const previewBody = ref<Record<string, any> | null>(null)
const entries = ref<Record<string, any>[]>([])
const metricKeys = ['FG_PCT', 'THREE_PCT', 'FT_PCT']

function emptyConfig(): LeaderboardConfig {
  return {
    rankingAudience: 'QUALIFIED_ONLY',
    overallStageIds: [],
    team: { minimumTeamGames: 0, minimumTeamPlayedRounds: 0, minimumTeamPlayedSeries: 0, minimumDueFixtureRate: null },
    player: {
      requireEligibleTeam: true,
      minimumPlayerGames: 0,
      minimumPlayerPlayedRounds: 0,
      minimumPlayerPlayedSeries: 0,
      minimumPlayerTeamRate: 0.5
    },
    metricGates: {
      FG_PCT: { enabled: false, minimumAttemptsPerAppearance: 5 },
      THREE_PCT: { enabled: false, minimumAttemptsPerAppearance: 2 },
      FT_PCT: { enabled: false, minimumAttemptsPerAppearance: 1 }
    },
    averageAndTotalSameEligibility: true
  }
}

const config = ref<LeaderboardConfig>(emptyConfig())
const current = computed(() => scopes.value.find(item => item.scopeKey === scopeKey.value))
const draft = computed(() => current.value?.draft || null)
const active = computed(() => current.value?.active || null)
const diffLines = computed(() => (previewBody.value?.diff?.changes || []) as string[])

function metricLabel(metric: string) {
  if (metric === 'FG_PCT') return '投篮'
  if (metric === 'THREE_PCT') return '三分'
  return '罚球'
}

function readConfig(policy: LeaderboardPolicy | null | undefined) {
  const next = emptyConfig()
  if (!policy?.configJson) {
    config.value = next
    return
  }
  try {
    const parsed = JSON.parse(policy.configJson)
    config.value = {
      ...next,
      ...parsed,
      team: { ...next.team, ...(parsed.team || {}) },
      player: { ...next.player, ...(parsed.player || {}) },
      metricGates: { ...next.metricGates, ...(parsed.metricGates || {}) }
    }
  } catch {
    config.value = next
  }
}

async function loadScopes() {
  if (!divisionId.value) return
  loading.value = true
  try {
    const res = await getV4LeaderboardScopes(divisionId.value)
    scopes.value = res.data?.scopes || []
    if (!scopes.value.some(item => item.scopeKey === scopeKey.value)) {
      scopeKey.value = scopes.value[0]?.scopeKey || ''
    }
    readConfig(draft.value || active.value)
  } finally {
    loading.value = false
  }
}

function onScopeChange() {
  previewBody.value = null
  entries.value = []
  readConfig(draft.value || active.value)
}

async function createDraft() {
  if (!divisionId.value || !scopeKey.value) return
  const res = await createV4LeaderboardDraft(divisionId.value, scopeKey.value)
  ElMessage.success('草稿已创建')
  await loadScopes()
  readConfig(res.data)
}

async function saveDraft() {
  if (!divisionId.value || !draft.value) return
  await saveV4LeaderboardDraft(divisionId.value, draft.value.id, draft.value.expectedLock, config.value)
  ElMessage.success('草稿已保存')
  await loadScopes()
}

async function loadTemplate() {
  if (!divisionId.value || !draft.value) return
  const res = await loadV4LeaderboardTemplate(divisionId.value, draft.value.id, draft.value.expectedLock)
  ElMessage.success('已载入模板')
  await loadScopes()
  readConfig(res.data)
}

async function validateDraft() {
  if (!divisionId.value || !draft.value) return
  await validateV4LeaderboardDraft(divisionId.value, draft.value.id, draft.value.expectedLock)
  ElMessage.success('校验通过')
  await loadScopes()
}

async function reopenDraft() {
  if (!divisionId.value || !draft.value) return
  await reopenV4LeaderboardDraft(divisionId.value, draft.value.id, draft.value.expectedLock)
  ElMessage.success('已退回草稿')
  await loadScopes()
}

async function preview() {
  if (!divisionId.value || !draft.value) return
  const res = await previewV4Leaderboard(divisionId.value, draft.value.id)
  previewBody.value = res.data || null
  const runId = Number(res.data?.runId)
  if (runId) {
    const rows = await listV4LeaderboardEligibility(divisionId.value, runId, undefined, 'ALL')
    entries.value = rows.data || []
  }
}

async function publish() {
  if (!divisionId.value || !draft.value) return
  const reason = await ElMessageBox.prompt('发布说明', '发布技术统计榜', { inputValue: '发布公开快照' })
  await publishV4Leaderboard(
    divisionId.value,
    draft.value.id,
    active.value?.versionNo || 0,
    draft.value.expectedLock,
    reason.value
  )
  ElMessage.success('已发布')
  previewBody.value = null
  await loadScopes()
}

async function recalculate() {
  if (!divisionId.value) return
  const stageId = current.value?.stageId || undefined
  await recalculateV4Leaderboard(divisionId.value, stageId ?? undefined)
  ElMessage.success('已按当前生效规则重算')
}

async function archive() {
  if (!divisionId.value || !active.value) return
  const reason = await ElMessageBox.prompt('归档说明', '归档生效规则')
  await archiveV4Leaderboard(divisionId.value, scopeKey.value, active.value.expectedLock, reason.value)
  ElMessage.success('已归档，公开快照仍保留到下一次发布')
  await loadScopes()
}

async function featureDivision() {
  if (!divisionId.value) return
  await updateV4FeaturedDivision(divisionId.value, '设为首页数据焦点')
  ElMessage.success('首页将读取该赛区已发布的全赛事榜')
}

watch(divisionId, () => {
  scopeKey.value = ''
  previewBody.value = null
  entries.value = []
  void loadScopes()
}, { immediate: true })
</script>

<style scoped>
.metric-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.ml8 { margin-left: 8px; }
</style>
