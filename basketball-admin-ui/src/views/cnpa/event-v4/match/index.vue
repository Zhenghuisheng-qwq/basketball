<template>
  <div class="app-container">
    <el-alert title="比赛管理：发布后的比赛执行与特殊改期。系列赛时间在「系列赛」Tab 调整；赛前整体排期请回赛事工作台。" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="query" :inline="true" class="mb8">
      <el-form-item label="视图">
        <el-radio-group v-model="viewMode">
          <el-radio-button value="match">单场列表</el-radio-button>
          <el-radio-button value="series">系列赛</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="赛季">
        <el-select v-model="query.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="query.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!query.seasonId">
          <el-option v-for="item in divisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="批次">
        <el-input-number v-model="query.scheduleBatchId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="轮次">
        <el-input-number v-model="query.roundNo" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="query.publishStatus" clearable style="width:120px">
          <el-option v-for="item in PUBLISH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="比赛状态">
        <el-select v-model="query.matchStatus" clearable style="width:120px">
          <el-option v-for="item in MATCH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队名称">
        <el-input v-model="query.teamName" clearable placeholder="主/客队名称" />
      </el-form-item>
      <el-form-item v-if="viewMode === 'series'" label="阶段">
        <el-select v-model="seriesStageId" clearable filterable placeholder="选择阶段" style="width: 200px" :disabled="!query.divisionId">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
      </el-form-item>
    </el-form>

    <MatchSeriesPanel
      v-if="viewMode === 'series'"
      ref="seriesPanelRef"
      :stage-id="seriesStageId"
      :division-id="query.divisionId"
      @open-match="openEditById"
    />

    <el-table v-if="viewMode === 'match'" v-loading="loading" :data="list" border>
      <el-table-column label="比赛ID" prop="id" width="80" />
      <el-table-column label="轮次" prop="roundNo" width="60" align="center" />
      <el-table-column label="Series Code" prop="seriesCode" min-width="160">
        <template #default="scope">{{ scope.row.seriesCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="BO" width="70" align="center">
        <template #default="scope">{{ scope.row.bestOf ? `BO${scope.row.bestOf}` : '-' }}</template>
      </el-table-column>
      <el-table-column label="Game No" width="90" align="center">
        <template #default="scope">{{ scope.row.seriesGameNo ? `Game ${scope.row.seriesGameNo}` : '-' }}</template>
      </el-table-column>
      <el-table-column label="比赛编号" prop="matchNo" min-width="130" />
      <el-table-column label="比赛时间" prop="matchTime" width="160" />
      <el-table-column label="主队" prop="homeTeamName" min-width="100" />
      <el-table-column label="客队" prop="awayTeamName" min-width="100" />
      <el-table-column label="比分" width="100" align="center">
        <template #default="scope">
          <span v-if="scope.row.matchStatus === 'FORFEITED'">判负</span>
          <span v-else>{{ formatV4MatchScore(scope.row.homeScore, scope.row.awayScore) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="平台赛区" prop="divisionName" min-width="110" />
      <el-table-column label="比赛状态" prop="matchStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4MatchStatusTagType(scope.row.matchStatus)" size="small" class="nowrap-tag" :title="scope.row.matchStatus">
            {{ v4MatchStatusLabel(scope.row.matchStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" prop="publishStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4PublishStatusTagType(scope.row.publishStatus)" size="small" class="nowrap-tag" :title="scope.row.publishStatus">
            {{ v4PublishStatusLabel(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right" align="right" class-name="match-ops-col">
        <template #default="scope">
          <div class="match-ops">
          <el-button link type="primary" @click="openEdit(scope.row)">
            {{ scope.row.publishStatus === 'DRAFT' ? '编辑' : '详情' }}
          </el-button>
          <el-tooltip
            v-if="isStandaloneMatch(scope.row)"
            :content="adjustDisabledReason(scope.row) || '调整独立比赛时间'"
            placement="top"
          >
            <el-button
              link
              type="primary"
              :disabled="!canAdjustMatch(scope.row)"
              @click="openAdjust(scope.row)"
            >调整时间</el-button>
          </el-tooltip>
          <el-button
            link
            type="success"
            v-if="canEnterResult(scope.row)"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openResult(scope.row, 'enter')"
          >管理员录入结果</el-button>
          <el-button
            link
            type="warning"
            v-if="canCorrectResult(scope.row)"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openResult(scope.row, 'correct')"
          >修正比赛结果</el-button>
          <el-button
            link
            type="danger"
            v-if="canReopenSubmission(scope.row)"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openReopen(scope.row)"
          >退回重传</el-button>
          <el-button
            link
            type="warning"
            v-if="canForfeitRow(scope.row)"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openForfeit(scope.row)"
          >判负</el-button>
          <el-button
            link
            type="danger"
            v-if="canRevokeForfeitRow(scope.row)"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openRevokeForfeit(scope.row)"
          >撤销判负</el-button>
          <el-button link type="danger" v-if="scope.row.publishStatus === 'DRAFT'" @click="handleDelete(scope.row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-if="viewMode === 'match'" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="forfeitVisible" title="判定本局弃赛/判负" width="480px">
      <el-alert type="warning" :closable="false" show-icon class="mb8"
        :title="`将对 ${forfeitRow?.homeTeamName} vs ${forfeitRow?.awayTeamName} 执行判负，胜者为对方球队。`" />
      <el-form label-width="100px">
        <el-form-item label="判负球队" required>
          <el-radio-group v-model="forfeitLoserId">
            <el-radio :value="forfeitRow?.homeEventTeamId">{{ forfeitRow?.homeTeamName }}</el-radio>
            <el-radio :value="forfeitRow?.awayEventTeamId">{{ forfeitRow?.awayTeamName }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="forfeitReason" type="textarea" rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forfeitVisible = false">取消</el-button>
        <el-button type="danger" @click="submitForfeit">确认判负</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="revokeForfeitVisible" title="撤销判负" width="480px">
      <el-alert type="warning" :closable="false" show-icon class="mb8"
        :title="`将撤销 ${revokeForfeitRow?.homeTeamName} vs ${revokeForfeitRow?.awayTeamName} 的判负，比赛恢复为未结算。`" />
      <el-form label-width="100px">
        <el-form-item label="原因" required>
          <el-input v-model="revokeForfeitReason" type="textarea" rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeForfeitVisible = false">取消</el-button>
        <el-button type="primary" :loading="revokeForfeitSaving" @click="submitRevokeForfeit">确认撤销</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adjustVisible" title="调整时间" width="520px">
      <el-form label-width="100px">
        <el-form-item label="赛事">{{ adjustRow?.seasonTitle || '-' }}</el-form-item>
        <el-form-item label="Division">{{ adjustRow?.divisionName || '-' }}</el-form-item>
        <el-form-item label="Stage / Round">R{{ adjustRow?.roundNo ?? '-' }}</el-form-item>
        <el-form-item label="比赛编号">{{ adjustRow?.matchNo || adjustRow?.id || '-' }}</el-form-item>
        <el-form-item label="主队">{{ adjustRow?.homeTeamName || '待定' }}</el-form-item>
        <el-form-item label="客队">{{ adjustRow?.awayTeamName || '待定' }}</el-form-item>
        <el-form-item label="当前日期">{{ currentMatchDate(adjustRow) }}</el-form-item>
        <el-form-item label="当前时间">{{ currentMatchClock(adjustRow) }}</el-form-item>
        <el-form-item label="新日期" required>
          <el-date-picker v-model="adjustDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="新时间" required>
          <el-time-picker v-model="adjustTime" value-format="HH:mm" format="HH:mm" />
        </el-form-item>
        <el-form-item label="调整原因" required>
          <el-input v-model="adjustReason" type="textarea" :rows="3" placeholder="必填，例如：双方协商延期" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">调整比赛时间</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" :title="editForm.publishStatus === 'DRAFT' ? '编辑草稿比赛' : '比赛详情'" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="比赛时间"><el-date-picker v-model="editForm.matchTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" :disabled="editForm.publishStatus !== 'DRAFT'" /></el-form-item>
        <el-form-item label="场地"><el-input v-model="editForm.venueText" :disabled="editForm.publishStatus !== 'DRAFT'" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :disabled="editForm.publishStatus !== 'DRAFT'" /></el-form-item>
        <el-form-item label="比赛录像">
          <el-input v-model="editForm.replayUrl" placeholder="B站等 http/https 外链" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" :title="resultMode === 'correct' ? '修正比赛结果' : '管理员录入结果'" width="560px">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        class="mb8"
        :title="resultMode === 'correct'
          ? '修正只会改两队总分，并清掉已入库的球员明细。原队长提交还在。要改每个球员的数据，请改用「退回重传」。'
          : '管理员录入后将直接形成正式比赛结果，并重新计算相关积分、统计和系列赛进度。'"
      />
      <el-form label-width="110px">
        <el-form-item label="赛事">{{ resultRow?.seasonTitle || '-' }}</el-form-item>
        <el-form-item label="Division">{{ resultRow?.divisionName || '-' }}</el-form-item>
        <el-form-item label="Stage / Round">R{{ resultRow?.roundNo ?? '-' }}</el-form-item>
        <el-form-item label="比赛">{{ resultRow?.matchNo || resultRow?.id || '-' }}</el-form-item>
        <el-form-item label="主队">{{ resultRow?.homeTeamName || '-' }}</el-form-item>
        <el-form-item label="客队">{{ resultRow?.awayTeamName || '-' }}</el-form-item>
        <template v-if="resultMode === 'correct'">
          <el-form-item label="原比分">{{ formatV4MatchScore(resultRow?.homeScore, resultRow?.awayScore) }}</el-form-item>
          <el-form-item label="原 Winner">{{ originalWinnerName(resultRow) }}</el-form-item>
          <el-form-item label="原状态">{{ v4MatchStatusLabel(resultRow?.matchStatus) }}</el-form-item>
        </template>
        <el-form-item label="主队比分" required>
          <el-input-number v-model="resultHomeScore" :min="0" :max="300" controls-position="right" />
        </el-form-item>
        <el-form-item label="客队比分" required>
          <el-input-number v-model="resultAwayScore" :min="0" :max="300" controls-position="right" />
        </el-form-item>
        <el-form-item label="新 Winner">{{ computedWinnerName() }}</el-form-item>
        <el-form-item :label="resultMode === 'correct' ? '修正原因' : '原因'" required>
          <el-input v-model="resultReason" type="textarea" rows="3" placeholder="必填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="resultRemark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resultVisible = false">取消</el-button>
        <el-button type="primary" @click="submitResult">{{ resultMode === 'correct' ? '确认修正' : '确认录入' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reopenVisible" title="退回队长重传" width="560px">
      <el-alert type="warning" :closable="false" show-icon class="mb8" :title="reopenHint" />
      <el-form label-width="100px">
        <el-form-item label="比赛">{{ reopenRow?.matchNo || reopenRow?.id || '-' }}</el-form-item>
        <el-form-item label="对阵">{{ reopenRow?.homeTeamName || '-' }} vs {{ reopenRow?.awayTeamName || '-' }}</el-form-item>
        <el-form-item label="当前比分">{{ formatV4MatchScore(reopenRow?.homeScore, reopenRow?.awayScore) }}</el-form-item>
        <el-form-item label="退回原因" required>
          <el-input v-model="reopenReason" type="textarea" rows="3" placeholder="必填，例如：误通过，需队长按正确数据重交" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reopenVisible = false">取消</el-button>
        <el-button type="danger" :loading="reopenSaving" @click="submitReopen">确认退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Match">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { correctV4OfficialResult, enterV4OfficialResult, forfeitV4Match, reopenV4ApprovedSubmission, revokeV4MatchForfeit } from '@/api/cnpa/event-v4/stage-exception'
import { deleteV4Match, getV4Match, listV4Match, updateV4Match } from '@/api/cnpa/event-v4/schedule-batch'
import { listV4StageExecution, rescheduleV4StageSchedule } from '@/api/cnpa/event-v4/stage-execution'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import MatchSeriesPanel from './MatchSeriesPanel.vue'
import {
  V4_MATCH_STATUS_FILTER_OPTIONS,
  V4_PUBLISH_STATUS_FILTER_OPTIONS,
  formatV4MatchScore,
  v4MatchStatusLabel,
  v4MatchStatusTagType,
  v4PublishStatusLabel,
  v4PublishStatusTagType
} from '@/utils/v4AdminDisplay'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10 })
const editVisible = ref(false)
const editId = ref<number>()
const editForm = reactive({ matchTime: '', venueText: '', remark: '', replayUrl: '', publishStatus: '' })
const resultVisible = ref(false)
const resultMode = ref<'enter' | 'correct'>('enter')
const resultRow = ref<any>()
const resultHomeScore = ref<number>()
const resultAwayScore = ref<number>()
const resultReason = ref('')
const resultRemark = ref('')
const seasonOptions = ref<V4AdminOptionVo[]>([])
const divisionOptions = ref<V4AdminOptionVo[]>([])
const PUBLISH_OPTIONS = V4_PUBLISH_STATUS_FILTER_OPTIONS
const MATCH_STATUS_OPTIONS = V4_MATCH_STATUS_FILTER_OPTIONS
const viewMode = ref<'match' | 'series'>('match')
const seriesStageId = ref<number>()
const stageOptions = ref<Array<{ id: number; stageName: string }>>([])
const seriesPanelRef = ref<InstanceType<typeof MatchSeriesPanel>>()
const forfeitVisible = ref(false)
const forfeitRow = ref<any>()
const forfeitLoserId = ref<number>()
const forfeitReason = ref('')
const revokeForfeitVisible = ref(false)
const revokeForfeitRow = ref<any>()
const revokeForfeitReason = ref('')
const revokeForfeitSaving = ref(false)
const adjustVisible = ref(false)
const adjustRow = ref<any>()
const adjustDate = ref('')
const adjustTime = ref('')
const adjustReason = ref('')
const reopenVisible = ref(false)
const reopenSaving = ref(false)
const reopenRow = ref<any>()
const reopenReason = ref('')

watch(viewMode, () => {
  if (viewMode.value === 'series') {
    seriesPanelRef.value?.reload()
  }
})

watch(() => query.divisionId, async (divisionId) => {
  seriesStageId.value = undefined
  stageOptions.value = []
  if (!divisionId) return
  const res = await listV4StageExecution(divisionId)
  stageOptions.value = (res.data || []).map((s: any) => ({
    id: s.stageId ?? s.id,
    stageName: s.stageName,
    stageType: s.stageType,
    bestOf: s.bestOf
  }))
  const knockout = stageOptions.value.find((s: any) => s.stageType === 'KNOCKOUT' || (s.bestOf && s.bestOf > 1))
  seriesStageId.value = (knockout || stageOptions.value[0])?.id
})

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadDivisions(seasonId?: number) {
  divisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  divisionOptions.value = res.data || []
}

function onSeasonChange(seasonId?: number) {
  query.divisionId = undefined
  loadDivisions(seasonId)
}

function loadList() {
  if (viewMode.value === 'series') {
    seriesPanelRef.value?.reload()
    return
  }
  loading.value = true
  listV4Match(query).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function openEditById(matchId?: number) {
  if (!matchId) return
  getV4Match(matchId).then(res => {
    if (res.data) openEdit(res.data)
  })
}

function openEdit(row: any) {
  editId.value = row.id
  editForm.matchTime = row.matchTime
  editForm.venueText = row.venueText || ''
  editForm.remark = row.remark || ''
  editForm.replayUrl = row.replayUrl || ''
  editForm.publishStatus = row.publishStatus || ''
  editVisible.value = true
}

function canEnterResult(row: any) {
  if (row?.canEnterOfficialResult != null) return Boolean(row.canEnterOfficialResult)
  return row?.publishStatus === 'PUBLISHED'
    && (row?.matchStatus === 'SCHEDULED' || row?.matchStatus === 'PLAYING')
    && row?.homeEventTeamId && row?.awayEventTeamId
}

function canCorrectResult(row: any) {
  if (row?.canCorrectOfficialResult != null) return Boolean(row.canCorrectOfficialResult)
  return row?.publishStatus === 'PUBLISHED' && row?.matchStatus === 'FINISHED'
}

function canReopenSubmission(row: any) {
  return row?.publishStatus === 'PUBLISHED' && row?.matchStatus === 'FINISHED'
}

const reopenHint = computed(() => {
  const row = reopenRow.value
  if (row?.seriesId || (row?.bestOf && row.bestOf > 1)) {
    return '这场属于系列赛。退回会删掉整个系列赛里队长已通过的提交，并撤销已经生效的比分、球员明细和积分。队长再打开是空白表，需要重新填写后提交。下一轮已经开打时不能退回。'
  }
  return '退回会删掉队长这份已通过的提交，并撤销本场正式比分、球员明细和积分。队长再打开是空白表，需要重新填写后提交。'
})

function canForfeitRow(row: any) {
  if (row?.matchStatus === 'FORFEITED') return false
  if (!row?.homeEventTeamId || !row?.awayEventTeamId) return false
  if (row?.canForfeitMatch != null) return Boolean(row.canForfeitMatch)
  return row?.matchStatus === 'SCHEDULED' || row?.matchStatus === 'PLAYING'
}

function canRevokeForfeitRow(row: any) {
  return row?.matchStatus === 'FORFEITED'
}

function originalWinnerName(row: any) {
  if (!row) return '-'
  if (row.winnerEventTeamId && row.winnerEventTeamId === row.homeEventTeamId) return row.homeTeamName
  if (row.winnerEventTeamId && row.winnerEventTeamId === row.awayEventTeamId) return row.awayTeamName
  return '-'
}

function computedWinnerName() {
  if (resultHomeScore.value == null || resultAwayScore.value == null) return '-'
  if (resultHomeScore.value === resultAwayScore.value) return '平局（按赛制校验）'
  return resultHomeScore.value > resultAwayScore.value
    ? (resultRow.value?.homeTeamName || '主队')
    : (resultRow.value?.awayTeamName || '客队')
}

function openResult(row: any, mode: 'enter' | 'correct') {
  resultMode.value = mode
  resultRow.value = row
  resultHomeScore.value = mode === 'correct' ? row.homeScore : undefined
  resultAwayScore.value = mode === 'correct' ? row.awayScore : undefined
  resultReason.value = ''
  resultRemark.value = ''
  resultVisible.value = true
}

function submitResult() {
  if (!resultRow.value?.id || resultHomeScore.value == null || resultAwayScore.value == null || !resultReason.value.trim()) {
    ElMessage.warning('请填写比分和原因')
    return
  }
  const confirmText = resultMode.value === 'correct'
    ? '确认修正正式比赛结果？将重新计算积分、统计和系列赛进度。'
    : '管理员录入后将直接形成正式比赛结果，并重新计算相关积分、统计和系列赛进度。'
  ElMessageBox.confirm(confirmText, '二次确认', { type: 'warning' }).then(() => {
    const payload = {
      matchId: resultRow.value.id,
      homeScore: resultHomeScore.value!,
      awayScore: resultAwayScore.value!,
      reason: resultReason.value.trim(),
      remark: resultRemark.value.trim() || undefined
    }
    const api = resultMode.value === 'correct' ? correctV4OfficialResult : enterV4OfficialResult
    api(payload).then(() => {
      ElMessage.success(resultMode.value === 'correct' ? '已修正比赛结果' : '已录入正式结果')
      resultVisible.value = false
      loadList()
    })
  })
}

function submitEdit() {
  if (!editId.value) return
  const payload = editForm.publishStatus === 'DRAFT'
    ? { ...editForm }
    : { replayUrl: editForm.replayUrl }
  updateV4Match(editId.value, payload).then(() => {
    ElMessage.success('已保存')
    editVisible.value = false
    loadList()
  })
}

function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除该草稿比赛？', '提示', { type: 'warning' }).then(() => {
    deleteV4Match(row.id).then(() => {
      ElMessage.success('已删除')
      loadList()
    })
  })
}

function isChildMatch(row: any) {
  const seriesId = Number(row?.seriesId)
  return Number.isFinite(seriesId) && seriesId > 0
}

function isStandaloneMatch(row: any) {
  if (row?.standaloneMatch != null) return Boolean(row.standaloneMatch)
  return !isChildMatch(row)
}

function isNotStartedMatch(row: any) {
  const status = String(row?.matchStatus || '').toUpperCase()
  return status === 'SCHEDULED' || status === 'READY' || status === 'NOT_STARTED'
}

function canAdjustMatch(row: any) {
  if (!isStandaloneMatch(row)) return false
  if (row?.canAdjustSchedule != null) return Boolean(row.canAdjustSchedule)
  return row?.publishStatus === 'PUBLISHED' && isNotStartedMatch(row)
}

function adjustDisabledReason(row: any) {
  if (row?.adjustScheduleBlockReason) return row.adjustScheduleBlockReason
  if (isChildMatch(row)) return '该比赛属于 BO 系列赛，请前往『系列赛』调整整个系列赛时间。'
  if (row?.publishStatus !== 'PUBLISHED') return '赛段尚未发布，请前往赛事工作台调整排期。'
  if (row?.matchStatus === 'PLAYING' || row?.matchStatus === 'IN_PROGRESS') return '比赛已经开始，不能使用普通时间调整。'
  if (row?.matchStatus === 'FINISHED' || row?.matchStatus === 'FORFEITED' || row?.matchStatus === 'CANCELLED') {
    return '比赛已结束，不能修改日期时间'
  }
  return ''
}

function currentMatchDate(row: any) {
  const raw = String(row?.matchTime || '')
  return raw.length >= 10 ? raw.slice(0, 10) : '待安排'
}

function currentMatchClock(row: any) {
  const raw = String(row?.matchTime || '')
  return raw.length >= 16 ? raw.slice(11, 16) : '待安排'
}

function openAdjust(row: any) {
  if (!canAdjustMatch(row)) {
    ElMessage.warning(adjustDisabledReason(row) || '当前不能调整时间')
    return
  }
  if (!row.stageId) {
    ElMessage.error('缺少赛段信息，无法调整时间')
    return
  }
  adjustRow.value = row
  adjustDate.value = currentMatchDate(row) === '待安排' ? '' : currentMatchDate(row)
  adjustTime.value = currentMatchClock(row) === '待安排' ? '21:00' : currentMatchClock(row)
  adjustReason.value = ''
  adjustVisible.value = true
}

function submitAdjust() {
  if (!adjustRow.value?.stageId || !adjustRow.value?.id) {
    ElMessage.error('缺少赛段或比赛信息')
    return
  }
  if (isChildMatch(adjustRow.value)) {
    ElMessage.error('该比赛属于 BO 系列赛，请前往『系列赛』调整整个系列赛时间。')
    return
  }
  if (!adjustDate.value || !adjustTime.value) {
    ElMessage.error('请填写新的日期和时间')
    return
  }
  if (!adjustReason.value.trim()) {
    ElMessage.error('调整原因必填')
    return
  }
  rescheduleV4StageSchedule(adjustRow.value.stageId, {
    unitType: 'MATCH',
    unitId: adjustRow.value.id,
    scheduledDate: adjustDate.value,
    scheduledStartTime: adjustTime.value,
    reason: adjustReason.value.trim()
  }).then(() => {
    ElMessage.success('已调整比赛时间')
    adjustVisible.value = false
    loadList()
  })
}

function openForfeit(row: any) {
  forfeitRow.value = row
  forfeitLoserId.value = row.awayEventTeamId
  forfeitReason.value = ''
  forfeitVisible.value = true
}

function submitForfeit() {
  if (!forfeitRow.value?.id || !forfeitLoserId.value || !forfeitReason.value?.trim()) {
    ElMessage.warning('请选择判负球队并填写原因')
    return
  }
  ElMessageBox.confirm('确认执行判负？此操作将记录审计日志。', '二次确认', { type: 'warning' }).then(() => {
    forfeitV4Match({
      matchId: forfeitRow.value.id,
      loserEventTeamId: forfeitLoserId.value!,
      reason: forfeitReason.value.trim()
    }).then(() => {
      ElMessage.success('已判定弃赛/判负')
      forfeitVisible.value = false
      loadList()
    })
  })
}

function openReopen(row: any) {
  reopenRow.value = row
  reopenReason.value = ''
  reopenVisible.value = true
}

function submitReopen() {
  if (!reopenRow.value?.id || reopenReason.value.trim().length < 2) {
    ElMessage.warning('请填写退回原因')
    return
  }
  const series = Boolean(reopenRow.value.seriesId || (reopenRow.value.bestOf && reopenRow.value.bestOf > 1))
  ElMessageBox.confirm(
    series ? '确认删除整个系列赛的队长提交？正式结果会一起撤销，队长需要重新填写。' : '确认删除本场队长提交？正式结果会一起撤销，队长需要重新填写。',
    '二次确认',
    { type: 'warning' }
  ).then(() => {
    reopenSaving.value = true
    reopenV4ApprovedSubmission({
      matchId: reopenRow.value.id,
      reason: reopenReason.value.trim()
    }).then(() => {
      ElMessage.success('已删除原提交。请让队长重新填写并提交，再审核通过。')
      reopenVisible.value = false
      loadList()
    }).finally(() => {
      reopenSaving.value = false
    })
  }).catch(() => { /* 取消二次确认 */ })
}

function openRevokeForfeit(row: any) {
  revokeForfeitRow.value = row
  revokeForfeitReason.value = ''
  revokeForfeitVisible.value = true
}

function submitRevokeForfeit() {
  if (!revokeForfeitRow.value?.id || !revokeForfeitReason.value?.trim()) {
    ElMessage.warning('请填写撤销原因')
    return
  }
  ElMessageBox.confirm('确认撤销判负？比赛将恢复为未结算，并重新计算积分/系列赛。', '二次确认', { type: 'warning' }).then(() => {
    revokeForfeitSaving.value = true
    revokeV4MatchForfeit({
      matchId: revokeForfeitRow.value.id,
      reason: revokeForfeitReason.value.trim()
    }).then(() => {
      ElMessage.success('已撤销判负')
      revokeForfeitVisible.value = false
      loadList()
    }).finally(() => {
      revokeForfeitSaving.value = false
    })
  })
}

onMounted(async () => {
  await loadSeasonOptions()
  loadList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
.match-ops {
  display: flex;
  justify-content: flex-end;
  flex-wrap: nowrap;
  gap: 2px;
  white-space: nowrap;
}
:deep(.match-ops-col) {
  background: var(--el-bg-color, #fff);
}
:deep(.match-ops-col .cell) {
  overflow: hidden;
}
</style>
