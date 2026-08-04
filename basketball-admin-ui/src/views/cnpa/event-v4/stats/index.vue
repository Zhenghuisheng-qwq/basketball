<template>
  <div class="app-container">
    <el-alert title="统计榜单：支持按阶段/小组查看积分榜，排名依据来自结构化 rank reason" type="info" :closable="false" show-icon class="mb8" />

    <el-form :inline="true" class="mb8">
      <el-form-item label="赛季">
        <el-select v-model="seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="divisionId" clearable filterable placeholder="选择赛区" style="width: 220px" :disabled="!seasonId" @change="onDivisionChange">
          <el-option v-for="item in divisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="阶段" v-if="stageOptions.length">
        <el-select v-model="stageId" clearable filterable placeholder="选择阶段" style="width: 200px" @change="loadStageStandings">
          <el-option v-for="item in stageOptions" :key="item.stageId" :label="`${item.stageOrder}. ${item.stageName}`" :value="item.stageId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadAll">查询</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="积分榜" name="standing">
        <template v-if="stageStandingScopes.length">
          <div v-for="scope in stageStandingScopes" :key="scope.groupScopeKey ?? 0" class="mb8">
            <div class="scope-title">
              {{ scope.groupName || (scope.groupId ? `小组 #${scope.groupId}` : '总积分榜') }}
              <el-tag v-if="scope.manualRankPending" type="warning" size="small">需人工排序</el-tag>
              <el-button
                v-if="canShowManualRank(scope)"
                type="warning"
                size="small"
                class="ml8"
                @click="openManualRankDialog(scope)"
              >人工处理排名</el-button>
            </div>
            <el-table :data="scope.rows" border size="small">
              <el-table-column label="排名" prop="rankNo" width="70" align="center" />
              <el-table-column label="球队" prop="teamName" />
              <el-table-column label="已赛" width="60" align="center">
                <template #default="s">{{ (s.row.winCount ?? 0) + (s.row.loseCount ?? 0) }}</template>
              </el-table-column>
              <el-table-column label="胜" prop="winCount" width="60" align="center" />
              <el-table-column label="负" prop="loseCount" width="60" align="center" />
              <el-table-column label="得分" prop="pointFor" width="70" align="center" />
              <el-table-column label="失分" prop="pointAgainst" width="70" align="center" />
              <el-table-column label="净胜分" prop="pointDiff" width="80" align="center" />
              <el-table-column label="积分" prop="standingPoints" width="80" align="center" />
              <el-table-column label="排名依据" min-width="160">
                <template #default="s">{{ s.row.decisiveReasonText || '-' }}</template>
              </el-table-column>
              <el-table-column label="人工" width="100" align="center">
                <template #default="s">
                  <el-tag v-if="s.row.manualRankPending" type="warning" size="small">待处理</el-tag>
                  <span v-else-if="s.row.manualRankNo">#{{ s.row.manualRankNo }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
        <el-table v-else :data="standingList" border size="small">
          <el-table-column label="排名" prop="rankNo" width="70" align="center" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="胜" prop="winCount" width="60" align="center" />
          <el-table-column label="负" prop="loseCount" width="60" align="center" />
          <el-table-column label="得分" prop="pointFor" width="80" align="center" />
          <el-table-column label="失分" prop="pointAgainst" width="80" align="center" />
          <el-table-column label="净胜分" prop="pointDiff" width="80" align="center" />
          <el-table-column label="积分" prop="standingPoints" width="80" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球队统计" name="team">
        <el-table :data="teamStatList" border size="small">
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="场次" prop="matchCount" width="70" align="center" />
          <el-table-column label="胜" prop="winCount" width="60" align="center" />
          <el-table-column label="负" prop="loseCount" width="60" align="center" />
          <el-table-column label="场均得分" prop="avgPointsFor" width="90" align="center" />
          <el-table-column label="场均失分" prop="avgPointsAgainst" width="90" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="球员统计" name="player">
        <el-table :data="playerStatList" border size="small">
          <el-table-column label="球员" prop="playerName" />
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="场次" prop="matchCount" width="70" align="center" />
          <el-table-column label="得分" prop="points" width="70" align="center" />
          <el-table-column label="场均得分" prop="avgPoints" width="90" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="排行榜" name="rank">
        <el-form :inline="true" class="mb8">
          <el-form-item label="类型">
            <el-select v-model="rankType" style="width: 180px">
              <el-option label="球队积分榜" value="TEAM_STANDING" />
              <el-option label="球员得分" value="PLAYER_SCORE" />
              <el-option label="球员助攻" value="PLAYER_ASSIST" />
              <el-option label="球员篮板" value="PLAYER_REBOUND" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button @click="loadRank">刷新排行榜</el-button></el-form-item>
        </el-form>
        <el-table :data="rankList" border size="small">
          <el-table-column label="排名" prop="rankNo" width="70" align="center" />
          <el-table-column label="类型" width="140">
            <template #default="scope">{{ v4RankTypeLabel(scope.row.rankType) }}</template>
          </el-table-column>
          <el-table-column label="球员/球队" min-width="120">
            <template #default="scope">{{ scope.row.playerName || scope.row.teamName || '-' }}</template>
          </el-table-column>
          <el-table-column label="数值" prop="rankValue" width="90" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="manualRankVisible" title="人工处理同分排名" width="860px" destroy-on-close @closed="resetManualRankForm">
      <el-descriptions :column="2" border size="small" class="mb8">
        <el-descriptions-item label="赛区">{{ selectedDivisionLabel }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ selectedStageLabel }}</el-descriptions-item>
        <el-descriptions-item label="小组" :span="2">{{ manualRankScopeLabel }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="manualRankUndecidedRule"
        :title="`未能区分的规则：${manualRankUndecidedRule}`"
        type="warning"
        :closable="false"
        show-icon
        class="mb8"
      />

      <el-table :data="manualRankForm.items" border size="small" class="mb8">
        <el-table-column label="球队" prop="teamName" min-width="120" />
        <el-table-column label="当前排名" prop="rankNo" width="80" align="center" />
        <el-table-column label="积分" prop="standingPoints" width="70" align="center" />
        <el-table-column label="已赛" width="60" align="center">
          <template #default="s">{{ (s.row.winCount ?? 0) + (s.row.loseCount ?? 0) }}</template>
        </el-table-column>
        <el-table-column label="胜/负" width="80" align="center">
          <template #default="s">{{ s.row.winCount ?? 0 }}/{{ s.row.loseCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="净胜分" prop="pointDiff" width="80" align="center" />
        <el-table-column label="人工序号" width="120" align="center">
          <template #default="s">
            <el-input-number v-model="s.row.manualRankNo" :min="1" :max="manualRankForm.items.length" controls-position="right" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="排名依据" min-width="140">
          <template #default="s">
            <div v-for="(reason, idx) in s.row.rankReasons || []" :key="idx" class="reason-line">
              <el-tag v-if="reason.isDecisive" type="success" size="small">决定性</el-tag>
              <el-tag v-else type="info" size="small">{{ reason.ruleCode }}</el-tag>
              {{ reason.reasonText }}
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-form label-width="100px">
        <el-form-item label="排序原因" required>
          <el-input v-model="manualRankForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写本次人工排序原因" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="manualRankVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualRankSaving" @click="submitManualRank">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Stats">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listV4PlayerStat, listV4Rank, listV4Standing, listV4TeamStat } from '@/api/cnpa/event-v4/match-submission-audit'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  applyV4StageManualRank,
  listV4StageExecution,
  listV4StageStandings,
  type BasketDivisionStageExecutionVo,
} from '@/api/cnpa/event-v4/stage-execution'
import { checkPermi } from '@/utils/permission'
import { v4RankTypeLabel } from '@/utils/v4AdminDisplay'

interface StageStandingRow {
  eventTeamId: number
  teamName?: string
  rankNo?: number
  winCount?: number
  loseCount?: number
  pointFor?: number
  pointAgainst?: number
  pointDiff?: number
  standingPoints?: number
  manualRankPending?: number
  manualRankNo?: number
  manualRankReason?: string
  decisiveReasonText?: string
  rankReasons?: Array<{ ruleCode?: string; reasonText?: string; isDecisive?: number }>
}

interface StageStandingScope {
  groupScopeKey?: number
  groupId?: number
  groupCode?: string
  groupName?: string
  manualRankPending?: boolean
  rows: StageStandingRow[]
}

const seasonId = ref<number>()
const divisionId = ref<number>()
const stageId = ref<number>()
const activeTab = ref('standing')
const standingList = ref<any[]>([])
const stageStandingScopes = ref<StageStandingScope[]>([])
const stageOptions = ref<BasketDivisionStageExecutionVo[]>([])
const teamStatList = ref<any[]>([])
const playerStatList = ref<any[]>([])
const rankList = ref<any[]>([])
const rankType = ref('TEAM_STANDING')
const seasonOptions = ref<V4AdminOptionVo[]>([])
const divisionOptions = ref<V4AdminOptionVo[]>([])

const manualRankVisible = ref(false)
const manualRankSaving = ref(false)
const manualRankScope = ref<StageStandingScope | null>(null)
const manualRankForm = ref<{
  groupScopeKey?: number
  reason: string
  items: Array<StageStandingRow & { manualRankNo: number }>
}>({ reason: '', items: [] })

const currentStage = computed(() => stageOptions.value.find(s => s.stageId === stageId.value))
const selectedDivisionLabel = computed(() => divisionOptions.value.find(d => d.id === divisionId.value)?.label || '-')
const selectedStageLabel = computed(() => {
  const stage = currentStage.value
  return stage ? `${stage.stageOrder}. ${stage.stageName}` : '-'
})
const manualRankScopeLabel = computed(() => {
  const scope = manualRankScope.value
  if (!scope) return '-'
  return scope.groupName || (scope.groupId ? `小组 #${scope.groupId}` : '总积分榜')
})
const manualRankUndecidedRule = computed(() => {
  const pending = manualRankForm.value.items
  if (!pending.length) return ''
  const reasons = pending[0]?.rankReasons || []
  const undecided = reasons.filter(r => !r.isDecisive).pop()
  return undecided?.reasonText || pending[0]?.decisiveReasonText || ''
})

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

function isScoringStage(stage?: BasketDivisionStageExecutionVo) {
  if (!stage) return false
  return stage.competitionFormat === 'ROUND_ROBIN' || stage.stageType === 'GROUP'
}

function canShowManualRank(scope: StageStandingScope) {
  if (!divisionId.value || !stageId.value || !scope.manualRankPending) return false
  if (!checkPermi(['basket:v4Standing:manualRank'])) return false
  const stage = currentStage.value
  if (!stage || !isScoringStage(stage)) return false
  if (stage.executionStatus === 'COMPLETED') return false
  return true
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadDivisions(sid?: number) {
  divisionOptions.value = []
  stageOptions.value = []
  stageId.value = undefined
  if (!sid) return
  const res = await listV4AdminDivisionOptions(sid)
  divisionOptions.value = res.data || []
}

async function onDivisionChange(did?: number) {
  stageOptions.value = []
  stageId.value = undefined
  stageStandingScopes.value = []
  if (!did) return
  const res = await listV4StageExecution(did)
  stageOptions.value = res.data || []
  if (stageOptions.value.length === 1) {
    stageId.value = stageOptions.value[0].stageId
    loadStageStandings()
  }
}

function onSeasonChange(sid?: number) {
  divisionId.value = undefined
  loadDivisions(sid)
}

function loadStageStandings() {
  if (!stageId.value) {
    stageStandingScopes.value = []
    return
  }
  listV4StageStandings(stageId.value).then(res => {
    stageStandingScopes.value = res.data || []
  })
}

function loadAll() {
  if (!divisionId.value) {
    ElMessage.warning('请选择平台赛区')
    return
  }
  const q = { divisionId: divisionId.value }
  if (stageId.value) {
    loadStageStandings()
  } else {
    listV4Standing(q).then(res => { standingList.value = res.data || [] })
  }
  listV4TeamStat(q).then(res => { teamStatList.value = res.data || [] })
  listV4PlayerStat(q).then(res => { playerStatList.value = res.data || [] })
  loadRank()
}

function loadRank() {
  if (!divisionId.value) return
  listV4Rank({ divisionId: divisionId.value, rankType: rankType.value }).then(res => { rankList.value = res.data || [] })
}

function openManualRankDialog(scope: StageStandingScope) {
  const pendingRows = (scope.rows || []).filter(r => r.manualRankPending === 1)
  if (!pendingRows.length) {
    ElMessage.warning('当前分组无需人工排名')
    return
  }
  manualRankScope.value = scope
  const baseRank = Math.min(...pendingRows.map(r => r.rankNo ?? 999))
  manualRankForm.value = {
    groupScopeKey: scope.groupScopeKey && scope.groupScopeKey > 0 ? scope.groupScopeKey : undefined,
    reason: pendingRows.find(r => r.manualRankReason)?.manualRankReason || '',
    items: pendingRows.map((row, idx) => ({
      ...row,
      manualRankNo: row.manualRankNo && row.manualRankNo > 0 ? row.manualRankNo : baseRank + idx,
    })),
  }
  manualRankVisible.value = true
}

function resetManualRankForm() {
  manualRankScope.value = null
  manualRankForm.value = { reason: '', items: [] }
}

async function submitManualRank() {
  if (!stageId.value) return
  const reason = manualRankForm.value.reason?.trim()
  if (!reason) {
    ElMessage.warning('请填写人工排序原因')
    return
  }
  const items = manualRankForm.value.items
  if (!items.length) {
    ElMessage.warning('无待处理球队')
    return
  }
  const rankNos = items.map(i => i.manualRankNo)
  if (rankNos.some(n => !n || n <= 0)) {
    ElMessage.warning('人工排名序号不能为空')
    return
  }
  if (new Set(rankNos).size !== rankNos.length) {
    ElMessage.warning('人工排名序号不能重复')
    return
  }
  manualRankSaving.value = true
  try {
    await applyV4StageManualRank(stageId.value, {
      groupScopeKey: manualRankForm.value.groupScopeKey,
      reason,
      items: items.map(i => ({ eventTeamId: i.eventTeamId, manualRankNo: i.manualRankNo })),
    })
    ElMessage.success('人工排名已保存；如有未确认晋级预览，请重新生成晋级预览')
    manualRankVisible.value = false
    loadStageStandings()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    manualRankSaving.value = false
  }
}

onMounted(async () => {
  await loadSeasonOptions()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.ml8 { margin-left: 8px; }
.scope-title { font-weight: 600; margin-bottom: 6px; display: flex; align-items: center; flex-wrap: wrap; gap: 8px; }
.reason-line { font-size: 12px; line-height: 1.5; margin-bottom: 4px; }
</style>
