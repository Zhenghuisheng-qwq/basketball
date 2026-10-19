<template>
  <div class="app-container">
    <el-alert title="赛程结构：只处理谁和谁打，不在此页填写日期时间" type="info" :closable="false" show-icon class="mb8" />
    <DivisionEventSelector v-if="!divisionId" entry="structure" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <DivisionWorkspaceNav />
      <WorkflowBlockedState
        v-if="blocked"
        :title="blocked"
        detail="请先完成赛事规则发布后再生成赛程结构。"
        action-text="去配置赛事规则"
        @action="goPublishRule"
      />
      <div v-else>
        <el-alert v-if="structureHint" :type="structureHintType" :closable="false" show-icon class="mb8" :title="structureHint" />
        <el-alert v-if="hasStaleStructure" type="error" :closable="false" show-icon class="mb8" title="当前赛程结构已因参赛名单变化失效，请重新生成。" />
        <el-button v-if="isSuperAdmin" class="mb8" type="danger" plain @click="rollbackUnstarted">退回未开赛赛程</el-button>
        <DoubleEliminationDemoPanel v-if="hasDoubleElim" class="mb8" />

    <div class="schedule-stage-panel">
        <el-form :inline="true" class="mb8">
          <el-form-item>
            <el-button type="primary" icon="Refresh" @click="loadStageList">刷新阶段</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="stageLoading" :data="stageList" border>
          <el-table-column label="顺序" prop="stageOrder" width="60" align="center" />
          <el-table-column label="阶段" prop="stageName" min-width="100" />
          <el-table-column label="类型" width="90">
            <template #default="scope">{{ v4StageTypeLabel(scope.row.stageType) }}</template>
          </el-table-column>
          <el-table-column label="赛制" width="100">
            <template #default="scope">{{ v4CompetitionFormatLabel(scope.row.competitionFormat) }}</template>
          </el-table-column>
          <el-table-column label="Seed" width="120">
            <template #default="scope">{{ scope.row.seedRule || '-' }}</template>
          </el-table-column>
          <el-table-column label="Draw" width="130">
            <template #default="scope">{{ scope.row.drawRule || '-' }}</template>
          </el-table-column>
          <el-table-column label="抽签状态" width="130">
            <template #default="scope">{{ stagePairingLabel(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="Bye" width="120">
            <template #default="scope">{{ scope.row.byeRule || '-' }}</template>
          </el-table-column>
          <el-table-column label="BO" width="140">
            <template #default="scope">{{ formatStageBo(scope.row) }}</template>
          </el-table-column>
          <el-table-column label="执行状态" width="120">
            <template #default="scope">
              <el-tag :type="v4StageExecutionStatusTagType(scope.row.executionStatus)" size="small">
                {{ v4StageExecutionStatusLabel(scope.row.executionStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="参赛队" prop="participantCount" width="70" align="center" />
          <el-table-column label="占位" width="90" align="center">
            <template #default="scope">
              <el-tag v-if="scope.row.hasUnresolvedPlaceholders" type="warning" size="small">待解析</el-tag>
              <el-tag v-else-if="scope.row.participantResolved" type="success" size="small">已解析</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="640" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleStagePreview(scope.row)">预览</el-button>
              <el-button link type="primary" v-if="scope.row.competitionFormat === 'GROUP_STAGE'" @click="openGroupPanel(scope.row)">小组</el-button>
              <el-tooltip v-if="needsPairingPrep(scope.row)" :disabled="!pairingPrepBlockReason(scope.row)" :content="pairingPrepBlockReason(scope.row)" placement="top">
                <span>
                  <el-button link type="primary" :disabled="!!pairingPrepBlockReason(scope.row)" @click="openPairingWorkspace(scope.row)">结构准备</el-button>
                </span>
              </el-tooltip>
              <el-button link type="warning" v-if="canResolvePlaceholders(scope.row)" @click="doResolveFromList(scope.row)">解析占位</el-button>
              <el-tooltip v-if="showGenerateAction(scope.row)" :disabled="!generateBlockReason(scope.row)" :content="generateBlockReason(scope.row)" placement="top">
                <span>
                  <el-button link type="success" :disabled="!!generateBlockReason(scope.row)" @click="handleStageGenerate(scope.row)">生成</el-button>
                </span>
              </el-tooltip>
              <el-button link type="warning" v-if="scope.row.executionStatus === 'SCHEDULE_GENERATED'" @click="handleStageCancel(scope.row)">作废</el-button>
              <el-button
                link type="warning"
                v-if="canAdjustPairing(scope.row)"
                v-hasPermi="['basket:v4Stage:exception']"
                @click="openPairingAdjust(scope.row)"
              >调整对阵</el-button>
              <el-button link type="primary" v-if="scope.row.executionStatus === 'RUNNING'" @click="handleCheckCompletion(scope.row)">检查完成</el-button>
              <el-button link type="primary" v-if="['RUNNING','RESULT_PENDING_CONFIRM'].includes(scope.row.executionStatus)" @click="handleQualPreview(scope.row)">晋级预览</el-button>
              <el-button
                link type="warning"
                v-if="canManualAdvance(scope.row)"
                v-hasPermi="['basket:v4Stage:exception']"
                @click="openManualAdvance(scope.row)"
              >直接晋级</el-button>
              <el-button link type="success" v-if="scope.row.executionStatus === 'RESULT_PENDING_CONFIRM'" @click="handleQualConfirm(scope.row)">确认晋级</el-button>
              <el-button link type="warning" v-if="scope.row.executionStatus === 'COMPLETED'" @click="handleQualCancel(scope.row)">撤销晋级确认</el-button>
            </template>
          </el-table-column>
        </el-table>
    </div>

    <el-dialog v-model="stagePreviewVisible" title="赛程生成预览" width="640px">
      <el-descriptions v-if="stagePreview" :column="2" border size="small">
        <el-descriptions-item label="参赛队伍">{{ stagePreview.teamCount }}</el-descriptions-item>
        <el-descriptions-item label="每队场数">{{ stagePreview.matchesPerTeam ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="总比赛/系列">{{ stagePreview.totalMatches }}</el-descriptions-item>
        <el-descriptions-item label="轮次数">{{ stagePreview.roundCount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="轮空">{{ stagePreview.hasBye ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item v-if="stagePreview.bracketCapacity" label="Bracket Capacity">{{ stagePreview.bracketCapacity }}</el-descriptions-item>
        <el-descriptions-item v-if="stagePreview.byeCount != null" label="BYE">{{ stagePreview.byeCount }}</el-descriptions-item>
        <el-descriptions-item v-if="stagePreview.firstRoundActual != null" label="首轮实际对阵">{{ stagePreview.firstRoundActual }}</el-descriptions-item>
        <el-descriptions-item v-if="stagePreview.totalActualSeries != null" label="总实际 Series">{{ stagePreview.totalActualSeries }}</el-descriptions-item>
      </el-descriptions>
      <el-alert v-for="(w, i) in (stagePreview?.warnings || [])" :key="i" :title="w" type="warning" class="mt8" :closable="false" />
    </el-dialog>

    <el-dialog v-model="pairingVisible" :title="isGroupRankWorkspace(pairingStage) ? 'GROUP_CROSS / 占位签位' : 'Seed / Draw / Slot'" width="920px">
      <el-alert class="mb8" type="info" :closable="false" show-icon
        :title="pairingAlertTitle" />
      <el-alert v-if="pairingVo?.hasUnresolvedPlaceholders" class="mb8" type="warning" :closable="false" show-icon
        title="淘汰结构已固定。小组赛未结束或晋级未确认前，签位仍是占位来源，等待小组赛结果。" />
      <div class="mb8">
        <el-button size="small" @click="loadPairingWorkspace">刷新</el-button>
        <el-button size="small" type="primary" v-if="pairingStage?.seedRule === 'ADMIN_SEED'" @click="saveSeeds">保存种子</el-button>
        <el-button size="small" type="primary" v-if="pairingStage?.seedRule === 'EXTERNAL_RANKING'" @click="saveRanking">保存排名</el-button>
        <el-button size="small" type="success" v-if="pairingStage?.drawRule === 'RANDOM_CONFIRM'" @click="doGenerateDraw">生成抽签</el-button>
        <el-button size="small" v-if="pairingStage?.drawRule === 'RANDOM_CONFIRM'" @click="doRedraw">重抽</el-button>
        <el-button size="small" type="success" v-if="pairingStage?.pairingRule === 'GROUP_CROSS'" @click="doGenerateGroupCross">生成 GROUP_CROSS</el-button>
        <el-button size="small" type="success" v-if="showManualSlotTable" @click="saveManual">保存签位</el-button>
        <el-button size="small" type="warning" @click="doConfirmDraw">确认</el-button>
        <el-button size="small" type="primary" v-if="pairingVo?.hasUnresolvedPlaceholders" @click="doResolvePlaceholders">解析占位</el-button>
      </div>
      <el-form v-if="pairingStage?.seedRule === 'ADMIN_SEED'" label-width="90px" class="mb8">
        <el-form-item label="种子(最多4)">
          <el-select v-model="seedTeamIds" multiple filterable :multiple-limit="4" placeholder="选择种子队（最多4支）" style="width:100%">
            <el-option v-for="t in pairingTeams" :key="t.eventTeamId" :label="teamOptionLabel(t)" :value="t.eventTeamId" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table v-if="pairingStage?.seedRule === 'EXTERNAL_RANKING'" :data="rankingItems" border size="small" max-height="240" class="mb8">
        <el-table-column label="球队" min-width="160">
          <template #default="scope">{{ rankingTeamName(scope.row.eventTeamId) }}</template>
        </el-table-column>
        <el-table-column label="外部排名" width="140">
          <template #default="scope">
            <el-input-number v-model="scope.row.rankingNo" :min="1" :max="pairingTeams.length" controls-position="right" />
          </template>
        </el-table-column>
      </el-table>
      <el-table v-if="showEventTeamManualTable" :data="manualSlots" border size="small" max-height="280">
        <el-table-column label="Slot" prop="slotNo" width="70" />
        <el-table-column label="BYE" width="80">
          <template #default="scope">
            <el-switch v-model="scope.row.bye" @change="() => { if (scope.row.bye) scope.row.eventTeamId = undefined }" />
          </template>
        </el-table-column>
        <el-table-column label="球队">
          <template #default="scope">
            <el-select v-model="scope.row.eventTeamId" :disabled="scope.row.bye" clearable filterable placeholder="球队" style="width:100%">
              <el-option v-for="t in pairingTeams" :key="'m'+t.eventTeamId" :label="teamOptionLabel(t)" :value="t.eventTeamId" />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
      <el-table v-if="showGroupRankManualTable" :data="manualSlots" border size="small" max-height="280">
        <el-table-column label="Slot" prop="slotNo" width="70" />
        <el-table-column label="BYE" width="80">
          <template #default="scope">
            <el-switch v-model="scope.row.bye" @change="() => { if (scope.row.bye) { scope.row.sourceGroupId = undefined; scope.row.sourceRank = undefined } }" />
          </template>
        </el-table-column>
        <el-table-column label="小组">
          <template #default="scope">
            <el-select v-model="scope.row.sourceGroupId" :disabled="scope.row.bye" clearable filterable placeholder="小组" style="width:100%">
              <el-option v-for="g in groupLayouts" :key="'g'+g.groupId" :label="g.groupName || (g.groupCode + '组')" :value="g.groupId" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="名次" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.sourceRank" :disabled="scope.row.bye" :min="1" :max="2" controls-position="right" />
          </template>
        </el-table-column>
      </el-table>
      <div v-for="round in (pairingVo?.bracketPreview?.rounds || [])" :key="round.roundNo" class="bracket-round">
        <div class="bracket-round__title">{{ round.roundName }} · {{ round.boRole }} · BO{{ round.bestOf }}</div>
        <div v-for="pair in (round.pairings || [])" :key="pair.slot" class="bracket-pair">
          <span>{{ slotLabel(pair.home) }}</span>
          <span class="vs">vs</span>
          <span>{{ slotLabel(pair.away) }}</span>
          <el-tag v-if="pair.byeAdvance" size="small" type="info">BYE 晋级</el-tag>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="qualPreviewVisible" title="晋级预览" width="640px">
      <el-table :data="qualPreviewItems" border size="small">
        <el-table-column label="球队" prop="teamName" />
        <el-table-column label="小组" prop="groupName" width="80" />
        <el-table-column label="排名" prop="sourceRank" width="60" align="center" />
        <el-table-column label="晋级类型" prop="qualificationTypeName" width="120" />
        <el-table-column label="目标阶段" prop="targetStageName" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="manualAdvanceVisible" title="人工直接晋级" width="480px">
      <el-alert type="info" :closable="false" show-icon class="mb8"
        :title="`阶段：${manualAdvanceStage?.stageName || '-'}（${manualAdvanceStage?.participantCount || 0} 队）`" />
      <el-form label-width="90px">
        <el-form-item label="目标球队" required>
          <el-select v-model="manualAdvanceTeamId" filterable placeholder="选择球队" style="width:100%">
            <el-option v-for="t in manualAdvanceTeams" :key="t.eventTeamId" :label="teamOptionLabel(t)" :value="t.eventTeamId" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="manualAdvanceReason" type="textarea" rows="3" placeholder="必填，将记录审计" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualAdvanceVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualAdvance">确认直接晋级</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pairingAdjustVisible" title="调整对阵（草稿）" width="640px">
      <el-alert type="warning" :closable="false" show-icon class="mb8"
        title="仅草稿赛程可调整。已发布或已有正式结果的对阵不可修改。" />
      <div v-for="(slot, idx) in pairingSlots" :key="idx" class="pairing-row">
        <span class="pairing-row__label">对阵 {{ idx + 1 }}</span>
        <el-select v-model="slot.homeEventTeamId" filterable placeholder="主队" style="width:180px">
          <el-option v-for="t in pairingStageTeams" :key="'h'+t.eventTeamId" :label="t.teamName" :value="t.eventTeamId" />
        </el-select>
        <span>vs</span>
        <el-select v-model="slot.awayEventTeamId" filterable placeholder="客队" style="width:180px">
          <el-option v-for="t in pairingStageTeams" :key="'a'+t.eventTeamId" :label="t.teamName" :value="t.eventTeamId" />
        </el-select>
      </div>
      <el-form class="mt8" label-width="90px">
        <el-form-item label="原因" required>
          <el-input v-model="pairingAdjustReason" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pairingAdjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPairingAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupPanelVisible" title="小组赛状态" width="880px">
      <el-alert class="mb8" type="info" :closable="false" show-icon
        :title="`分组 ${groupPanelStatus || '-'} · 积分榜 ${groupPanelStandingHint}`" />
      <div v-for="group in groupPanelGroups" :key="group.groupId" class="group-card">
        <div class="group-card__title">{{ group.groupName || ((group.groupCode || '') + '组') }}</div>
        <el-table :data="group.rows" border size="small">
          <el-table-column label="球队" min-width="140">
            <template #default="scope">{{ scope.row.teamName || ('#' + scope.row.eventTeamId) }}</template>
          </el-table-column>
          <el-table-column label="排名" prop="rankNo" width="70" align="center" />
          <el-table-column label="胜/负" width="80" align="center">
            <template #default="scope">{{ scope.row.winCount ?? '-' }}/{{ scope.row.loseCount ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="积分" prop="standingPoints" width="70" align="center" />
        </el-table>
      </div>
    </el-dialog>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Structure">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import DivisionWorkspaceNav from '../components/DivisionWorkspaceNav.vue'
import WorkflowBlockedState from '../components/WorkflowBlockedState.vue'
import DoubleEliminationDemoPanel from '../components/DoubleEliminationDemoPanel.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'
import {
  cancelV4StageDraftSchedule, cancelV4StageQualificationConfirm, checkV4StageCompletion, confirmV4StageQualification,
  confirmV4Draw, generateV4Draw, generateV4GroupCross, generateV4StageSchedule, getV4PairingExecution,
  listV4StageExecution, listV4StageStandings, listV4StageTeams, previewV4StageQualification,
  previewV4StageSchedule, redrawV4Draw, resolveV4Placeholders, saveV4AdminSeeds, saveV4ExternalRanking,
  saveV4ManualSlots, type BasketDivisionStageExecutionVo, type BracketSlotView,
  type StagePairingExecutionVo, type StageSchedulePreviewVo, type StageTeamItemVo
} from '@/api/cnpa/event-v4/stage-execution'
import { getV4DivisionGroupingBatchDetail, listV4DivisionGroupingBatch } from '@/api/cnpa/event-v4/division-grouping'
import { adjustV4Pairing, manualAdvanceV4, type PairingSlotReq } from '@/api/cnpa/event-v4/stage-exception'
import { getV4ScheduleBatch } from '@/api/cnpa/event-v4/schedule-batch'
import { rollbackUnstartedSchedule } from '@/api/cnpa/event-v4/format-template'
import { checkRole } from '@/utils/permission'
import { listV4MatchSeriesByStage } from '@/api/cnpa/event-v4/match-series'
import {
  v4CompetitionFormatLabel,
  v4PlaceholderProductLabel,
  v4StageExecutionStatusLabel,
  v4StageExecutionStatusTagType,
  v4StageTypeLabel
} from '@/utils/v4AdminDisplay'

const router = useRouter()
const { divisionId, overview, blocked, selectDivision, clearDivision, loadOverview } = useDivisionWorkspace('structure')
const isSuperAdmin = computed(() => checkRole(['admin']))
const stageDivisionId = computed(() => divisionId.value)
const hasStaleStructure = computed(() => (overview.value?.stages || []).some(s => s.structureStatus === 'STALE'))
const hasDoubleElim = computed(() => (overview.value?.stages || []).some(s => s.competitionFormat === 'DOUBLE_ELIMINATION'))
const ruleLocked = computed(() => Boolean(overview.value?.rule?.locked))
const rosterLocked = computed(() => Boolean(overview.value?.roster?.locked))
const structureHint = computed(() => {
  if (!ruleLocked.value) return '待发布规则预览：当前展示最新规则草稿的 Stage Graph，锁定规则后才能生成正式结构。'
  if (!rosterLocked.value) return '等待锁定最终参赛名单'
  return ''
})
const structureHintType = computed(() => (ruleLocked.value ? 'warning' : 'info'))

function formatStageBo(row: BasketDivisionStageExecutionVo) {
  const parts = [`BO${row.bestOf ?? '-'}`]
  if (row.semifinalBestOf != null) parts.push(`SF BO${row.semifinalBestOf}`)
  if (row.finalBestOf != null) parts.push(`Final BO${row.finalBestOf}`)
  return parts.join(' / ')
}

function goPublishRule() {
  router.push({ path: '/event/event-v4-publish', query: divisionId.value ? { divisionId: String(divisionId.value) } : {} })
}

const stageLoading = ref(false)
const stageList = ref<BasketDivisionStageExecutionVo[]>([])
const stagePreviewVisible = ref(false)
const stagePreview = ref<StageSchedulePreviewVo | null>(null)
const qualPreviewVisible = ref(false)
const qualPreviewItems = ref<any[]>([])

const manualAdvanceVisible = ref(false)
const manualAdvanceStage = ref<BasketDivisionStageExecutionVo>()
const manualAdvanceTeams = ref<StageTeamItemVo[]>([])
const manualAdvanceTeamId = ref<number>()
const manualAdvanceReason = ref('')

const pairingAdjustVisible = ref(false)
const pairingAdjustStage = ref<BasketDivisionStageExecutionVo>()
const pairingStageTeams = ref<StageTeamItemVo[]>([])
const pairingSlots = ref<PairingSlotReq[]>([])
const pairingAdjustReason = ref('')

const pairingVisible = ref(false)
const pairingStage = ref<BasketDivisionStageExecutionVo>()
const pairingVo = ref<StagePairingExecutionVo | null>(null)
const pairingTeams = ref<StageTeamItemVo[]>([])
const seedTeamIds = ref<number[]>([])
const rankingItems = ref<Array<{ eventTeamId: number; rankingNo: number }>>([])
const manualSlots = ref<Array<{
  slotNo: number
  eventTeamId?: number
  bye: boolean
  sourceType?: string
  sourceGroupId?: number
  sourceRank?: number
}>>([])
const groupLayouts = ref<Array<{ groupId: number; groupCode?: string; groupName?: string }>>([])
const groupPanelVisible = ref(false)
const groupPanelStatus = ref('')
const groupPanelStandingHint = ref('未加载')
const groupPanelGroups = ref<Array<{
  groupId: number
  groupCode?: string
  groupName?: string
  rows: Array<{ eventTeamId?: number; teamName?: string; rankNo?: number; winCount?: number; loseCount?: number; standingPoints?: number }>
}>>([])

function teamOptionLabel(t: StageTeamItemVo) {
  return t.seedNo != null ? `#${t.seedNo} ${t.teamName}` : (t.teamName || String(t.eventTeamId))
}

function generateBlockReason(row: BasketDivisionStageExecutionVo) {
  if (!ruleLocked.value) return '规则未锁定'
  if (!rosterLocked.value) return '名单未锁定'
  const formal = overview.value?.roster?.formalCount
  if (formal != null && formal < 2) return '正式参赛单位不足'
  const stageCap = overview.value?.stages?.find(s => s.stageId === row.stageId)?.capabilities
  if (stageCap && stageCap.canGenerateStructure === false) {
    return stageCap.blockReason || '当前不能生成赛程结构'
  }
  if (overview.value?.capabilities && overview.value.capabilities.canGenerateStructure === false) {
    return overview.value.capabilities.blockReason || '当前不能生成赛程结构'
  }
  return ''
}

function showGenerateAction(row: BasketDivisionStageExecutionVo) {
  return ['READY', 'DRAFT', 'SCHEDULE_GENERATED'].includes(row.executionStatus || '') && row.competitionFormat !== 'DOUBLE_ELIMINATION'
}

function canGenerate(row: BasketDivisionStageExecutionVo) {
  return showGenerateAction(row) && !generateBlockReason(row)
}

function previousGroupStage(row?: BasketDivisionStageExecutionVo | null) {
  if (!row) return undefined
  return stageList.value
    .filter(s => (s.stageOrder || 0) < (row.stageOrder || 0) && s.competitionFormat === 'GROUP_STAGE')
    .sort((a, b) => (b.stageOrder || 0) - (a.stageOrder || 0))[0]
}

function isGroupRankWorkspace(row?: BasketDivisionStageExecutionVo | null) {
  if (!row || row.competitionFormat !== 'SINGLE_ELIMINATION') return false
  return row.pairingRule === 'GROUP_CROSS' || (row.pairingRule === 'MANUAL' && !!previousGroupStage(row))
}

function needsPairingPrep(row: BasketDivisionStageExecutionVo) {
  if (row.competitionFormat !== 'SINGLE_ELIMINATION') return false
  if (isGroupRankWorkspace(row)) return true
  return row.drawRule === 'RANDOM_CONFIRM' || row.drawRule === 'MANUAL_SLOT' || row.seedRule === 'ADMIN_SEED' || row.seedRule === 'EXTERNAL_RANKING'
}

function pairingPrepBlockReason(_row?: BasketDivisionStageExecutionVo) {
  if (!rosterLocked.value) return '请先锁定最终参赛名单。'
  const formal = overview.value?.roster?.formalCount
  if (formal == null || formal <= 0) {
    return '当前锁定名单没有正式参赛球队，请返回报名与参赛名单检查名单分配。'
  }
  return ''
}

function canResolvePlaceholders(row: BasketDivisionStageExecutionVo) {
  return !!row.hasUnresolvedPlaceholders
}

const showGroupRankManualTable = computed(() =>
  !!pairingStage.value && isGroupRankWorkspace(pairingStage.value) && pairingStage.value.pairingRule === 'MANUAL'
)
const showEventTeamManualTable = computed(() =>
  pairingStage.value?.drawRule === 'MANUAL_SLOT' && !isGroupRankWorkspace(pairingStage.value)
)
const showManualSlotTable = computed(() => showGroupRankManualTable.value || showEventTeamManualTable.value)
const pairingAlertTitle = computed(() => {
  const stage = pairingStage.value
  const vo = pairingVo.value
  if (isGroupRankWorkspace(stage)) {
    return `占位 ${vo?.participantCount || 0} · capacity ${vo?.bracketCapacity || '-'} · BYE ${vo?.byeCount ?? '-'} · ${stage?.pairingRule || '-'} / ${vo?.resolutionStatus || 'PENDING'}`
  }
  const drawState = vo?.status === 'CONFIRMED'
    ? '抽签已确认'
    : '抽签尚未确认'
  return `参赛 ${vo?.participantCount || 0} · capacity ${vo?.bracketCapacity || '-'} · BYE ${vo?.byeCount ?? '-'} · ${stage?.seedRule || '-'} / ${stage?.drawRule || '-'} / ${stage?.byeRule || '-'} · ${drawState}`
})

function canManualAdvance(row: BasketDivisionStageExecutionVo) {
  return row.competitionFormat === 'SINGLE_ELIMINATION'
    && !!row.nextStageCode
    && ['RUNNING', 'SCHEDULE_GENERATED', 'RESULT_PENDING_CONFIRM'].includes(row.executionStatus || '')
}

function canAdjustPairing(row: BasketDivisionStageExecutionVo) {
  return row.competitionFormat === 'SINGLE_ELIMINATION'
    && row.executionStatus === 'SCHEDULE_GENERATED'
    && !!row.scheduleBatchId
}

function stagePairingLabel(row: BasketDivisionStageExecutionVo) {
  return overview.value?.stages?.find(s => s.stageId === row.stageId)?.pairingStatusName || '-'
}

function loadStageList() {
  if (!stageDivisionId.value) return Promise.resolve()
  stageLoading.value = true
  return listV4StageExecution(stageDivisionId.value).then(res => {
    stageList.value = res.data || []
    if (pairingStage.value?.stageId) {
      const fresh = stageList.value.find(s => s.stageId === pairingStage.value?.stageId)
      if (fresh) pairingStage.value = fresh
    }
  }).finally(() => { stageLoading.value = false })
}

async function refreshAuthoritativeWorkspace() {
  await Promise.all([loadOverview(), loadStageList(), loadPairingWorkspace()])
}

function handleStagePreview(row: BasketDivisionStageExecutionVo) {
  previewV4StageSchedule(row.stageId).then(res => {
    stagePreview.value = res.data
    stagePreviewVisible.value = true
  })
}

function handleStageGenerate(row: BasketDivisionStageExecutionVo) {
  const reason = generateBlockReason(row)
  if (reason) {
    ElMessage.warning(reason)
    return
  }
  ElMessageBox.confirm('确认生成该阶段草稿赛程？', '提示').then(() => {
    generateV4StageSchedule(row.stageId).then(async res => {
      ElMessage.success('生成成功，批次ID=' + res.data)
      await refreshAuthoritativeWorkspace()
    })
  })
}

function rollbackUnstarted() {
  if (!divisionId.value) return
  ElMessageBox.confirm('将删除本赛区尚未开赛的赛程和系列赛，并把规则退回可编辑。已开赛、已有统计或已有提交的比赛不会删除。确认退回？', '提示', { type: 'warning' }).then(() => {
    return rollbackUnstartedSchedule(Number(divisionId.value))
  }).then(() => {
    ElMessage.success('已退回。可以修改规则，锁定后再重新抽签、生成赛程。')
    loadOverview()
    loadStageList()
  }).catch(() => undefined)
}

function handleStageCancel(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('确认作废该赛程？已撤回、且没有正式比赛结果的赛程也会删除。', '提示', { type: 'warning' }).then(() => {
    cancelV4StageDraftSchedule(row.stageId).then(() => {
      ElMessage.success('已作废')
      loadStageList()
    })
  })
}

function handleCheckCompletion(row: BasketDivisionStageExecutionVo) {
  checkV4StageCompletion(row.stageId).then(() => {
    ElMessage.success('阶段完成检查已执行')
    loadStageList()
  })
}

function handleQualPreview(row: BasketDivisionStageExecutionVo) {
  previewV4StageQualification(row.stageId).then(res => {
    qualPreviewItems.value = res.data?.items || res.data || []
    qualPreviewVisible.value = true
  })
}

function handleQualConfirm(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('确认晋级结果？将初始化下一阶段参赛队伍', '提示').then(() => {
    confirmV4StageQualification(row.stageId).then(() => {
      ElMessage.success('晋级已确认')
      loadStageList()
    })
  })
}

function handleQualCancel(row: BasketDivisionStageExecutionVo) {
  ElMessageBox.confirm('撤销晋级确认？下一阶段若尚未生成赛程，将回退到待确认。', '提示').then(() => {
    cancelV4StageQualificationConfirm(row.stageId).then(() => {
      ElMessage.success('已撤销晋级确认')
      loadStageList()
    })
  })
}

async function openManualAdvance(row: BasketDivisionStageExecutionVo) {
  manualAdvanceStage.value = row
  manualAdvanceTeamId.value = undefined
  manualAdvanceReason.value = ''
  const res = await listV4StageTeams(row.stageId)
  manualAdvanceTeams.value = res.data || []
  if (!manualAdvanceTeams.value.length) {
    ElMessage.warning('当前阶段尚无参赛队伍')
    return
  }
  manualAdvanceVisible.value = true
}

function submitManualAdvance() {
  if (!manualAdvanceStage.value?.stageId || !manualAdvanceTeamId.value || !manualAdvanceReason.value?.trim()) {
    ElMessage.warning('请选择球队并填写原因')
    return
  }
  ElMessageBox.confirm('确认将该球队设为直接晋级？', '二次确认', { type: 'warning' }).then(() => {
    manualAdvanceV4({
      stageId: manualAdvanceStage.value!.stageId,
      eventTeamId: manualAdvanceTeamId.value!,
      reason: manualAdvanceReason.value.trim()
    }).then(() => {
      ElMessage.success('已写入人工晋级预览')
      manualAdvanceVisible.value = false
      handleQualPreview(manualAdvanceStage.value!)
      loadStageList()
    })
  })
}

async function openPairingAdjust(row: BasketDivisionStageExecutionVo) {
  pairingAdjustStage.value = row
  pairingAdjustReason.value = ''
  const teamsRes = await listV4StageTeams(row.stageId)
  pairingStageTeams.value = teamsRes.data || []
  pairingSlots.value = []

  const seriesRes = await listV4MatchSeriesByStage(row.stageId)
  const seriesList = seriesRes.data || []
  if (seriesList.length > 0) {
    pairingSlots.value = seriesList.map(s => ({
      bracketSlot: s.bracketSlot ?? 0,
      homeEventTeamId: s.homeEventTeamId!,
      awayEventTeamId: s.awayEventTeamId!
    }))
  } else if (row.scheduleBatchId) {
    const batchRes = await getV4ScheduleBatch(row.scheduleBatchId)
    const matches = (batchRes.data?.matches || []).filter((m: any) => m.roundNo === 1 && !m.seriesId)
    const slotMap = new Map<number, PairingSlotReq>()
    for (const m of matches) {
      const slot = m.bracketSlot ?? 0
      if (!slotMap.has(slot)) {
        slotMap.set(slot, {
          bracketSlot: slot,
          homeEventTeamId: m.homeEventTeamId,
          awayEventTeamId: m.awayEventTeamId
        })
      }
    }
    pairingSlots.value = [...slotMap.values()].sort((a, b) => a.bracketSlot - b.bracketSlot)
  }
  if (!pairingSlots.value.length) {
    ElMessage.warning('未找到可调整的对阵')
    return
  }
  pairingAdjustVisible.value = true
}

function submitPairingAdjust() {
  const stage = pairingAdjustStage.value
  if (!stage?.stageId || !stage.scheduleBatchId) return
  if (!pairingAdjustReason.value?.trim()) {
    ElMessage.warning('请填写原因')
    return
  }
  const used = new Set<number>()
  for (const slot of pairingSlots.value) {
    if (!slot.homeEventTeamId || !slot.awayEventTeamId) {
      ElMessage.warning('对阵双方不能为空')
      return
    }
    if (slot.homeEventTeamId === slot.awayEventTeamId) {
      ElMessage.warning('对阵双方不能相同')
      return
    }
    if (used.has(slot.homeEventTeamId) || used.has(slot.awayEventTeamId)) {
      ElMessage.warning('每支球队只能出现一次')
      return
    }
    used.add(slot.homeEventTeamId)
    used.add(slot.awayEventTeamId)
  }
  ElMessageBox.confirm('确认调整对阵？将重建草稿 Match/Series。', '二次确认', { type: 'warning' }).then(() => {
    adjustV4Pairing({
      stageId: stage.stageId,
      slots: pairingSlots.value,
      reason: pairingAdjustReason.value.trim()
    }).then(() => {
      ElMessage.success('对阵已调整')
      pairingAdjustVisible.value = false
      loadStageList()
    })
  })
}

watch(divisionId, () => {
  stageList.value = []
  if (divisionId.value) loadStageList()
}, { immediate: true })

async function openPairingWorkspace(row: BasketDivisionStageExecutionVo) {
  const blockedReason = pairingPrepBlockReason(row)
  if (blockedReason) {
    ElMessage.warning(blockedReason)
    return
  }
  pairingStage.value = row
  const teamsRes = await listV4StageTeams(row.stageId).catch(() => ({ data: [] }))
  pairingTeams.value = teamsRes.data || []
  await loadGroupLayouts(row)
  await loadPairingWorkspace()
  if (!pairingTeams.value.length && !(pairingVo.value?.participantCount)) {
    ElMessage.warning('当前没有可参与赛程结构的正式参赛球队，请先完成报名、名单分配并锁定最终参赛名单。')
    pairingVisible.value = false
    return
  }
  pairingVisible.value = true
}

async function loadGroupLayouts(row: BasketDivisionStageExecutionVo) {
  groupLayouts.value = []
  const groupStage = previousGroupStage(row)
  if (!groupStage || !stageDivisionId.value) return
  const listRes = await listV4DivisionGroupingBatch({
    divisionId: stageDivisionId.value,
    stageId: groupStage.stageId,
    groupingStatus: 'LOCKED',
    pageNum: 1,
    pageSize: 1
  })
  const batchId = listRes.rows?.[0]?.groupingBatchId
  if (!batchId) return
  const detail = await getV4DivisionGroupingBatchDetail(batchId)
  groupLayouts.value = (detail.data?.groups || []).map(g => ({
    groupId: g.groupId!,
    groupCode: g.groupCode,
    groupName: g.groupName
  }))
}

async function loadPairingWorkspace() {
  if (!pairingStage.value) return
  const res = await getV4PairingExecution(pairingStage.value.stageId)
  pairingVo.value = res.data
  seedTeamIds.value = (pairingVo.value?.entries || [])
    .filter(e => e.seedNo && e.seedNo <= 4 && e.eventTeamId)
    .sort((a, b) => (a.seedNo || 0) - (b.seedNo || 0))
    .map(e => e.eventTeamId!)
  const rankFromEntries = (pairingVo.value?.entries || []).filter(e => e.eventTeamId && e.rankingNo)
  if (rankFromEntries.length === pairingTeams.value.length) {
    rankingItems.value = rankFromEntries.map(e => ({ eventTeamId: e.eventTeamId!, rankingNo: e.rankingNo! }))
  } else {
    rankingItems.value = pairingTeams.value.map((t, idx) => ({ eventTeamId: t.eventTeamId, rankingNo: idx + 1 }))
  }
  const capacity = pairingVo.value?.bracketCapacity || 0
  if (showManualSlotTable.value) {
    if (pairingVo.value?.entries?.length) {
      manualSlots.value = pairingVo.value.entries.map(e => ({
        slotNo: e.slotNo,
        eventTeamId: e.eventTeamId,
        bye: !!e.bye,
        sourceType: e.sourceType,
        sourceGroupId: e.sourceGroupId,
        sourceRank: e.sourceRank
      }))
    } else {
      manualSlots.value = Array.from({ length: capacity }, (_, i) => ({ slotNo: i + 1, bye: false }))
    }
  }
}

function slotLabel(slot?: BracketSlotView) {
  if (!slot) return '-'
  if (slot.bye || slot.sourceType === 'BYE') return '轮空'
  const source = slot.sourceDisplay || slot.displayName || slot.displayCode || ''
  const team = slot.resolvedEventTeamName || (slot.resolutionStatus === 'RESOLVED' ? slot.teamName : '')
  if (source && team) return `已确定：${source} → ${team}`
  if (source) return v4PlaceholderProductLabel(slot.resolutionStatus, slot.sourceType, source)
  const seed = slot.seedNo != null ? `Seed${slot.seedNo} ` : ''
  return `${seed}${slot.teamName || slot.eventTeamId || '-'}`
}

function rankingTeamName(eventTeamId: number) {
  return pairingTeams.value.find(t => t.eventTeamId === eventTeamId)?.teamName || String(eventTeamId)
}

async function saveSeeds() {
  if (!pairingStage.value) return
  await saveV4AdminSeeds(pairingStage.value.stageId, seedTeamIds.value.slice(0, 4))
  ElMessage.success('种子已保存')
  await loadPairingWorkspace()
}

async function saveRanking() {
  if (!pairingStage.value) return
  await saveV4ExternalRanking(pairingStage.value.stageId, rankingItems.value)
  ElMessage.success('外部排名已按当前列表顺序保存')
  await loadPairingWorkspace()
}

async function doGenerateDraw() {
  if (!pairingStage.value) return
  await generateV4Draw(pairingStage.value.stageId)
  await refreshAuthoritativeWorkspace()
  if (pairingVo.value?.status === 'CONFIRMED') {
    ElMessage.error('抽签预览尚未确认，不能显示为已确认')
    return
  }
  ElMessage.success('抽签已生成，刷新不会重抽')
}

async function doRedraw() {
  if (!pairingStage.value) return
  await ElMessageBox.confirm('重抽将保留历史执行并使旧结构 STALE', '提示', { type: 'warning' })
  await redrawV4Draw(pairingStage.value.stageId)
  ElMessage.success('已重抽')
  await loadPairingWorkspace()
}

async function saveManual() {
  if (!pairingStage.value) return
  await saveV4ManualSlots(pairingStage.value.stageId, manualSlots.value)
  ElMessage.success('手动签位已保存')
  await loadPairingWorkspace()
}

async function doConfirmDraw() {
  if (!pairingStage.value?.stageId) return
  await confirmV4Draw(pairingStage.value.stageId)
  await refreshAuthoritativeWorkspace()
  if (pairingVo.value?.status !== 'CONFIRMED') {
    ElMessage.error('服务端未确认当前阶段抽签，工作台未改为已确认')
    return
  }
  ElMessage.success('抽签已确认，可以生成结构')
}

async function doGenerateGroupCross() {
  if (!pairingStage.value) return
  await generateV4GroupCross(pairingStage.value.stageId)
  ElMessage.success('GROUP_CROSS 占位已生成')
  await loadPairingWorkspace()
}

async function doResolvePlaceholders() {
  if (!pairingStage.value) return
  await ElMessageBox.confirm('将按已确认 GROUP_TOP_N 解析占位，签位不会重抽。', '解析占位')
  await resolveV4Placeholders(pairingStage.value.stageId)
  ElMessage.success('占位已解析')
  await loadPairingWorkspace()
  loadStageList()
}

async function doResolveFromList(row: BasketDivisionStageExecutionVo) {
  await ElMessageBox.confirm('将按已确认 GROUP_TOP_N 解析占位，签位不会重抽。', '解析占位')
  await resolveV4Placeholders(row.stageId)
  ElMessage.success('占位已解析')
  loadStageList()
}

async function openGroupPanel(row: BasketDivisionStageExecutionVo) {
  groupPanelVisible.value = true
  groupPanelGroups.value = []
  groupPanelStatus.value = '-'
  groupPanelStandingHint.value = '加载中'
  if (!stageDivisionId.value) return
  const listRes = await listV4DivisionGroupingBatch({
    divisionId: stageDivisionId.value,
    stageId: row.stageId,
    pageNum: 1,
    pageSize: 1
  })
  const batch = listRes.rows?.[0]
  groupPanelStatus.value = batch?.groupingStatus || '未分组'
  if (!batch?.groupingBatchId) {
    groupPanelStandingHint.value = '无分组批次'
    return
  }
  const [detail, standingRes] = await Promise.all([
    getV4DivisionGroupingBatchDetail(batch.groupingBatchId),
    listV4StageStandings(row.stageId).catch(() => ({ data: [] }))
  ])
  const standingScopes = standingRes.data || []
  const rankByTeam = new Map<number, any>()
  for (const scope of standingScopes) {
    for (const item of (scope.rows || [])) {
      if (item.eventTeamId) rankByTeam.set(item.eventTeamId, item)
    }
  }
  groupPanelGroups.value = (detail.data?.groups || []).map(g => ({
    groupId: g.groupId!,
    groupCode: g.groupCode,
    groupName: g.groupName,
    rows: (g.teams || []).map(t => {
      const standing = rankByTeam.get(t.eventTeamId!)
      return {
        eventTeamId: t.eventTeamId,
        teamName: t.teamNameSnapshot,
        rankNo: standing?.rankNo,
        winCount: standing?.winCount,
        loseCount: standing?.loseCount,
        standingPoints: standing?.standingPoints
      }
    })
  }))
  groupPanelStandingHint.value = standingScopes.length ? '已加载' : '暂无积分榜'
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
.pairing-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.pairing-row__label { width: 64px; color: #606266; font-size: 13px; }
.bracket-round { margin-top: 12px; }
.bracket-round__title { font-weight: 600; margin-bottom: 6px; }
.bracket-pair { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; font-size: 13px; }
.vs { color: #909399; }
.group-card { margin-bottom: 12px; }
.group-card__title { font-weight: 600; margin-bottom: 6px; }
</style>
