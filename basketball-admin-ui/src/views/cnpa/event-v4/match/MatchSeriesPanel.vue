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
      <el-table-column label="Series ID" prop="id" width="90" />
      <el-table-column label="赛事" prop="seasonName" min-width="140" />
      <el-table-column label="Division" prop="divisionName" min-width="160" />
      <el-table-column label="Stage" prop="stageName" min-width="100" />
      <el-table-column label="Round" width="80" align="center">
        <template #default="scope">{{ scope.row.roundName || (`R${scope.row.roundNo ?? '-'}`) }}</template>
      </el-table-column>
      <el-table-column label="Series Code" prop="seriesCode" min-width="180" />
      <el-table-column label="主队" min-width="120">
        <template #default="scope">{{ scope.row.homeTeamName || '待定' }}</template>
      </el-table-column>
      <el-table-column label="客队" min-width="120">
        <template #default="scope">{{ scope.row.awayTeamName || '待定' }}</template>
      </el-table-column>
      <el-table-column label="BO" width="70" align="center">
        <template #default="scope">BO{{ scope.row.bestOf }}</template>
      </el-table-column>
      <el-table-column label="Series Score" width="110" align="center">
        <template #default="scope">{{ scope.row.seriesScore || `${scope.row.homeWinCount ?? 0}:${scope.row.awayWinCount ?? 0}` }}</template>
      </el-table-column>
      <el-table-column label="Schedule Date" width="120" align="center">
        <template #default="scope">{{ scope.row.scheduledDate || '待安排' }}</template>
      </el-table-column>
      <el-table-column label="Start Time" width="100" align="center">
        <template #default="scope">{{ formatStartTime(scope.row.scheduledStartTime) }}</template>
      </el-table-column>
      <el-table-column label="Series Status" width="110" align="center">
        <template #default="scope">
          <el-tag :type="v4SeriesStatusTagType(scope.row.seriesStatus)" size="small">
            {{ scope.row.seriesStatusName || v4SeriesStatusLabel(scope.row.seriesStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Publish Status" width="110" align="center">
        <template #default="scope">{{ scope.row.publishStatusName || scope.row.publishStatus || '-' }}</template>
      </el-table-column>
      <el-table-column label="Games Played" width="110" align="center">
        <template #default="scope">{{ scope.row.gamesPlayed ?? 0 }} / {{ scope.row.bestOf ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.seriesStatus !== 'COMPLETED'"
            link
            type="warning"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openSeriesForfeit(scope.row)"
          >弃赛</el-button>
          <el-button
            v-else-if="scope.row.resolutionType === 'FORFEIT'"
            link
            type="info"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openRevokeForfeit(scope.row)"
          >撤销弃赛</el-button>
        </template>
      </el-table-column>
    </el-table>

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
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { forfeitV4Series, revokeV4SeriesResolution } from '@/api/cnpa/event-v4/stage-exception'
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
</style>
