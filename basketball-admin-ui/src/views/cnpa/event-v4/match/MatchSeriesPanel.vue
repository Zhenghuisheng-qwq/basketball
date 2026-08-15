<template>
  <div v-loading="loading" class="match-series-panel">
    <el-empty v-if="!loading && !seriesList.length" description="当前范围暂无 BO3/BO5/BO7 系列赛" />
    <el-table v-else :data="seriesList" border row-key="id">
      <el-table-column type="expand">
        <template #default="scope">
          <div class="series-games">
            <div v-for="game in scope.row.games || []" :key="game.matchId" class="series-game-row">
              <span class="series-game-row__label">Game {{ game.seriesGameNo }} / {{ scope.row.bestOf }}</span>
              <span class="series-game-row__teams">{{ scope.row.homeTeamName || '待定' }} vs {{ scope.row.awayTeamName || '待定' }}</span>
              <span v-if="game.skipped" class="series-game-row__skip">{{ game.skippedReason || '无需进行' }}</span>
              <span v-else-if="game.homeScore != null" class="series-game-row__score">{{ formatV4MatchScore(game.homeScore, game.awayScore) }}</span>
              <span v-else class="series-game-row__pending">{{ game.matchStatusName || '待比赛' }}</span>
              <el-tag size="small">{{ game.matchStatusName }}</el-tag>
              <el-button link type="primary" size="small" @click="$emit('openMatch', game.matchId)">详情</el-button>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="赛事" prop="seasonName" min-width="140" />
      <el-table-column label="Division" prop="divisionName" min-width="150" />
      <el-table-column label="Stage" prop="stageName" min-width="90" />
      <el-table-column label="Round" width="80" align="center">
        <template #default="scope">{{ scope.row.roundName || (`R${scope.row.roundNo ?? '-'}`) }}</template>
      </el-table-column>
      <el-table-column label="Series Code" prop="seriesCode" min-width="170" />
      <el-table-column label="主队" min-width="120">
        <template #default="scope">{{ scope.row.homeTeamName || '待定' }}</template>
      </el-table-column>
      <el-table-column label="客队" min-width="120">
        <template #default="scope">{{ scope.row.awayTeamName || '待定' }}</template>
      </el-table-column>
      <el-table-column label="BO" width="70" align="center">
        <template #default="scope">BO{{ scope.row.bestOf }}</template>
      </el-table-column>
      <el-table-column label="系列赛比分" width="100" align="center">
        <template #default="scope">{{ scope.row.seriesScore || `${scope.row.homeWinCount ?? 0}:${scope.row.awayWinCount ?? 0}` }}</template>
      </el-table-column>
      <el-table-column label="日期" width="110" align="center">
        <template #default="scope">{{ scope.row.scheduledDate || '待安排' }}</template>
      </el-table-column>
      <el-table-column label="时间" width="90" align="center">
        <template #default="scope">{{ formatStartTime(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4SeriesStatusTagType(scope.row.seriesStatus)" size="small">
            {{ scope.row.seriesStatusName || v4SeriesStatusLabel(scope.row.seriesStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" width="100" align="center">
        <template #default="scope">{{ scope.row.publishStatusName || scope.row.publishStatus || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openSeriesDetail(scope.row)">详情</el-button>
          <el-tooltip
            v-if="!isSeriesFinished(scope.row)"
            :content="adjustDisabledReason(scope.row) || '调整系列赛日期时间'"
            placement="top"
          >
            <el-button
              link
              type="primary"
              :disabled="!canAdjust(scope.row)"
              @click="openAdjust(scope.row)"
            >调整时间</el-button>
          </el-tooltip>
          <el-button
            v-if="canResolveManually(scope.row)"
            link
            type="danger"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openManualResolve(scope.row)"
          >管理员裁决</el-button>
          <el-button
            v-if="scope.row.seriesStatus !== 'COMPLETED'"
            link
            type="warning"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openSeriesForfeit(scope.row)"
          >弃赛</el-button>
          <el-button
            v-else-if="scope.row.resolutionType === 'FORFEIT' || scope.row.resolutionType === 'MANUAL'"
            link
            type="info"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openRevokeForfeit(scope.row)"
          >{{ scope.row.resolutionType === 'MANUAL' ? '撤销裁决' : '撤销弃赛' }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailVisible" title="系列赛详情" width="560px">
      <el-descriptions v-if="detailSeries" :column="2" border size="small">
        <el-descriptions-item label="赛事">{{ detailSeries.seasonName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Division">{{ detailSeries.divisionName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赛段">{{ detailSeries.stageName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="轮次">{{ detailSeries.roundName || (`R${detailSeries.roundNo ?? '-'}`) }}</el-descriptions-item>
        <el-descriptions-item label="Series Code" :span="2">{{ detailSeries.seriesCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对阵" :span="2">{{ detailSeries.homeTeamName || '待定' }} vs {{ detailSeries.awayTeamName || '待定' }}</el-descriptions-item>
        <el-descriptions-item label="BO">BO{{ detailSeries.bestOf }}</el-descriptions-item>
        <el-descriptions-item label="比分">{{ detailSeries.seriesScore || '0:0' }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ detailSeries.scheduledDate || '待安排' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ formatStartTime(detailSeries.scheduledStartTime) }}</el-descriptions-item>
      </el-descriptions>
      <div class="series-games mt8">
        <div v-for="game in detailSeries?.games || []" :key="game.matchId" class="series-game-row">
          <span class="series-game-row__label">Game {{ game.seriesGameNo }}</span>
          <span v-if="game.skipped" class="series-game-row__skip">{{ game.skippedReason || '无需进行' }}</span>
          <span v-else-if="game.homeScore != null" class="series-game-row__score">{{ formatV4MatchScore(game.homeScore, game.awayScore) }}</span>
          <span v-else class="series-game-row__pending">{{ game.matchStatusName || '待比赛' }}</span>
          <el-button v-if="game.matchId" link type="primary" size="small" @click="$emit('openMatch', game.matchId)">打开比赛</el-button>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="adjustVisible" title="调整时间" width="520px">
      <el-form label-width="100px">
        <el-form-item label="赛事">{{ adjustSeries?.seasonName || '-' }}</el-form-item>
        <el-form-item label="Division">{{ adjustSeries?.divisionName || '-' }}</el-form-item>
        <el-form-item label="赛段">{{ adjustSeries?.stageName || '-' }}</el-form-item>
        <el-form-item label="轮次">{{ adjustSeries?.roundName || (`R${adjustSeries?.roundNo ?? '-'}`) }}</el-form-item>
        <el-form-item label="Series Code">{{ adjustSeries?.seriesCode || '-' }}</el-form-item>
        <el-form-item label="主队">{{ adjustSeries?.homeTeamName || '待定' }}</el-form-item>
        <el-form-item label="客队">{{ adjustSeries?.awayTeamName || '待定' }}</el-form-item>
        <el-form-item label="BO">BO{{ adjustSeries?.bestOf }}</el-form-item>
        <el-form-item label="当前日期">{{ adjustSeries?.scheduledDate || '待安排' }}</el-form-item>
        <el-form-item label="当前时间">{{ formatStartTime(adjustSeries?.scheduledStartTime) }}</el-form-item>
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

    <el-dialog v-model="forfeitVisible" title="判定系列赛弃赛" width="480px">
      <el-alert type="warning" :closable="false" show-icon class="mb8"
        :title="`BO${forfeitSeries?.bestOf}：${forfeitSeries?.homeTeamName} vs ${forfeitSeries?.awayTeamName}`" />
      <el-form label-width="100px">
        <el-form-item label="弃赛方" required>
          <el-radio-group v-model="forfeitTeamId">
            <el-radio :value="forfeitSeries?.homeEventTeamId">{{ forfeitSeries?.homeTeamName }}</el-radio>
            <el-radio :value="forfeitSeries?.awayEventTeamId">{{ forfeitSeries?.awayTeamName }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="forfeitReason" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forfeitVisible = false">取消</el-button>
        <el-button type="danger" @click="submitSeriesForfeit">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="manualVisible" title="系列赛管理员裁决" width="520px">
      <el-alert type="error" :closable="false" show-icon class="mb8"
        title="仅用于赛事仲裁 / 违规判定等异常场景。不会改写 Game 比分。" />
      <el-form label-width="110px">
        <el-form-item label="Series">{{ manualSeries?.seriesCode || '-' }}</el-form-item>
        <el-form-item label="轮次">{{ manualSeries?.roundName || (`R${manualSeries?.roundNo ?? '-'}`) }}</el-form-item>
        <el-form-item label="主队">{{ manualSeries?.homeTeamName || '-' }}</el-form-item>
        <el-form-item label="客队">{{ manualSeries?.awayTeamName || '-' }}</el-form-item>
        <el-form-item label="BO">BO{{ manualSeries?.bestOf }}</el-form-item>
        <el-form-item label="当前比分">{{ manualSeries?.seriesScore || '0:0' }}</el-form-item>
        <el-form-item label="当前状态">{{ manualSeries?.seriesStatusName || manualSeries?.seriesStatus }}</el-form-item>
        <el-form-item label="裁决胜方" required>
          <el-radio-group v-model="manualWinnerId">
            <el-radio :value="manualSeries?.homeEventTeamId">{{ manualSeries?.homeTeamName }}</el-radio>
            <el-radio :value="manualSeries?.awayEventTeamId">{{ manualSeries?.awayTeamName }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="裁决类型" required>
          <el-select v-model="manualDecisionType" style="width:100%">
            <el-option label="赛事仲裁" value="ARBITRATION" />
            <el-option label="违规判定" value="VIOLATION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="manualReason" type="textarea" rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manualVisible = false">取消</el-button>
        <el-button type="danger" @click="submitManualResolve">确认裁决</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { forfeitV4Series, resolveV4SeriesManually, revokeV4SeriesResolution } from '@/api/cnpa/event-v4/stage-exception'
import { rescheduleV4StageSchedule } from '@/api/cnpa/event-v4/stage-execution'
import { listV4MatchSeriesByDivision, listV4MatchSeriesByStage, type BasketMatchSeriesDetailVo } from '@/api/cnpa/event-v4/match-series'
import { formatV4MatchScore, v4SeriesStatusLabel, v4SeriesStatusTagType } from '@/utils/v4AdminDisplay'

const props = defineProps<{ stageId?: number; divisionId?: number }>()
defineEmits<{ openMatch: [matchId?: number] }>()

const loading = ref(false)
const seriesList = ref<BasketMatchSeriesDetailVo[]>([])
const forfeitVisible = ref(false)
const forfeitSeries = ref<BasketMatchSeriesDetailVo>()
const forfeitTeamId = ref<number>()
const forfeitReason = ref('')
const detailVisible = ref(false)
const detailSeries = ref<BasketMatchSeriesDetailVo>()
const adjustVisible = ref(false)
const adjustSeries = ref<BasketMatchSeriesDetailVo>()
const adjustDate = ref('')
const adjustTime = ref('')
const adjustReason = ref('')
const manualVisible = ref(false)
const manualSeries = ref<BasketMatchSeriesDetailVo>()
const manualWinnerId = ref<number>()
const manualDecisionType = ref('ARBITRATION')
const manualReason = ref('')

async function load() {
  if (!props.stageId && !props.divisionId) {
    seriesList.value = []
    return
  }
  loading.value = true
  try {
    const res = props.stageId
      ? await listV4MatchSeriesByStage(props.stageId)
      : await listV4MatchSeriesByDivision(props.divisionId!)
    seriesList.value = res.data || []
  } finally {
    loading.value = false
  }
}

watch(() => [props.stageId, props.divisionId], () => { void load() }, { immediate: true })

function formatStartTime(value?: string) {
  if (!value) return '待安排'
  return value.length >= 8 ? value.slice(0, 5) : value
}

function isSeriesFinished(series: BasketMatchSeriesDetailVo) {
  const status = String(series.seriesStatus || '').toUpperCase()
  return status === 'COMPLETED' || status === 'CANCELLED' || status === 'FINISHED' || status === 'FORFEITED'
}

function isSeriesStarted(series: BasketMatchSeriesDetailVo) {
  const status = String(series.seriesStatus || '').toUpperCase()
  if (status === 'RUNNING' || status === 'IN_PROGRESS') return true
  return (series.games || []).some((game) => {
    const matchStatus = String(game.matchStatus || '').toUpperCase()
    return matchStatus === 'PLAYING' || matchStatus === 'IN_PROGRESS' || matchStatus === 'FINISHED' || matchStatus === 'FORFEITED'
  })
}

function canAdjust(series: BasketMatchSeriesDetailVo) {
  if (series.canAdjustSchedule != null) return Boolean(series.canAdjustSchedule)
  return series.publishStatus === 'PUBLISHED' && !isSeriesFinished(series) && !isSeriesStarted(series)
}

function adjustDisabledReason(series: BasketMatchSeriesDetailVo) {
  if (series.adjustScheduleBlockReason) return series.adjustScheduleBlockReason
  if (isSeriesFinished(series)) return '系列赛已结束，不能修改日期时间'
  if (isSeriesStarted(series)) return '该系列赛已经开始，不能使用普通时间调整。'
  return ''
}

function openSeriesDetail(series: BasketMatchSeriesDetailVo) {
  detailSeries.value = series
  detailVisible.value = true
}

function openAdjust(series: BasketMatchSeriesDetailVo) {
  if (!canAdjust(series)) {
    ElMessage.warning(adjustDisabledReason(series) || '当前不能调整时间')
    return
  }
  adjustSeries.value = series
  adjustDate.value = series.scheduledDate || ''
  adjustTime.value = formatStartTime(series.scheduledStartTime) === '待安排' ? '21:00' : formatStartTime(series.scheduledStartTime)
  adjustReason.value = ''
  adjustVisible.value = true
}

function submitAdjust() {
  if (!adjustSeries.value?.stageId || !adjustSeries.value.id) {
    ElMessage.error('缺少赛段或系列赛信息')
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
  rescheduleV4StageSchedule(adjustSeries.value.stageId, {
    unitType: 'MATCH_SERIES',
    unitId: adjustSeries.value.id,
    scheduledDate: adjustDate.value,
    scheduledStartTime: adjustTime.value,
    reason: adjustReason.value.trim()
  }).then(() => {
    ElMessage.success('已调整系列赛时间')
    adjustVisible.value = false
    void load()
  })
}

function openSeriesForfeit(series: BasketMatchSeriesDetailVo) {
  forfeitSeries.value = series
  forfeitTeamId.value = series.awayEventTeamId
  forfeitReason.value = ''
  forfeitVisible.value = true
}

function submitSeriesForfeit() {
  if (!forfeitSeries.value?.id || !forfeitTeamId.value || !forfeitReason.value?.trim()) {
    ElMessage.warning('请选择弃赛方并填写原因')
    return
  }
  ElMessageBox.confirm('确认判定整个系列赛弃赛？', '二次确认', { type: 'warning' }).then(() => {
    forfeitV4Series({
      seriesId: forfeitSeries.value!.id!,
      forfeitEventTeamId: forfeitTeamId.value!,
      reason: forfeitReason.value.trim()
    }).then(() => {
      ElMessage.success('已判定系列赛弃赛')
      forfeitVisible.value = false
      void load()
    })
  })
}

function canResolveManually(series: BasketMatchSeriesDetailVo) {
  if (series.canResolveSeriesManually != null) return Boolean(series.canResolveSeriesManually)
  return !!series.homeEventTeamId && !!series.awayEventTeamId
    && series.seriesStatus !== 'CANCELLED'
    && series.resolutionType !== 'FORFEIT'
    && series.resolutionType !== 'MANUAL'
}

function openManualResolve(series: BasketMatchSeriesDetailVo) {
  manualSeries.value = series
  manualWinnerId.value = series.homeEventTeamId
  manualDecisionType.value = 'ARBITRATION'
  manualReason.value = ''
  manualVisible.value = true
}

function submitManualResolve() {
  if (!manualSeries.value?.id || !manualWinnerId.value || !manualReason.value.trim()) {
    ElMessage.warning('请选择胜方并填写原因')
    return
  }
  ElMessageBox.confirm('确认进行系列赛异常裁决？此操作将直接产生 Series Winner 并尝试晋级，不会改写各局比分。', '二次确认', { type: 'warning' }).then(() => {
    resolveV4SeriesManually({
      seriesId: manualSeries.value!.id!,
      winnerEventTeamId: manualWinnerId.value!,
      decisionType: manualDecisionType.value,
      reason: manualReason.value.trim()
    }).then(() => {
      ElMessage.success('已完成系列赛管理员裁决')
      manualVisible.value = false
      void load()
    })
  })
}

function openRevokeForfeit(series: BasketMatchSeriesDetailVo) {
  ElMessageBox.prompt('请填写撤销原因', '撤销系列赛弃赛判定', {
    confirmButtonText: '确认撤销',
    cancelButtonText: '取消',
    inputPattern: /.{2,}/,
    inputErrorMessage: '原因至少2个字符'
  }).then(({ value }) => {
    revokeV4SeriesResolution({ seriesId: series.id!, reason: value.trim() }).then(() => {
      ElMessage.success('已撤销弃赛判定，系列赛将根据正式局数重新计算')
      void load()
    })
  }).catch(() => {})
}

defineExpose({ reload: load })
</script>

<style scoped>
.match-series-panel { margin-bottom: 16px; }
.series-games { padding: 8px 16px 12px 48px; display: flex; flex-direction: column; gap: 8px; }
.series-game-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 13px; }
.series-game-row__label { font-weight: 600; min-width: 90px; }
.series-game-row__score { color: #606266; }
.series-game-row__skip { color: #909399; font-style: italic; }
.series-game-row__pending { color: #909399; }
.mb8 { margin-bottom: 8px; }
.mt8 { margin-top: 8px; }
</style>
