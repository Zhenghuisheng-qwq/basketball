<template>
  <div v-loading="loading" class="match-series-panel">
    <el-empty v-if="!loading && !seriesList.length" description="当前阶段暂无 BO3/BO5 系列赛" />
    <div v-for="series in seriesList" :key="series.id" class="series-card">
      <div class="series-card__header">
        <div class="series-card__title">
          <span class="series-card__stage">{{ series.stageName || '淘汰赛' }}</span>
          <el-tag size="small" type="warning">BO{{ series.bestOf }}</el-tag>
          <span class="series-card__code">{{ series.seriesCode }}</span>
        </div>
        <el-tag :type="v4SeriesStatusTagType(series.seriesStatus)" size="small">
          {{ series.seriesStatusName || v4SeriesStatusLabel(series.seriesStatus) }}
        </el-tag>
      </div>

      <div class="series-card__versus">
        <div class="series-card__team">{{ series.homeTeamName }}</div>
        <div class="series-card__center">
          <div class="series-card__set-score">局分 {{ series.homeWinCount ?? 0 }} : {{ series.awayWinCount ?? 0 }}</div>
          <div class="series-card__required">先胜 {{ series.requiredWins }} 局</div>
        </div>
        <div class="series-card__team series-card__team--away">{{ series.awayTeamName }}</div>
      </div>

      <div v-if="series.winnerTeamName" class="series-card__winner">
        {{ series.finalStage ? '冠军' : '胜者' }}：{{ series.winnerTeamName }}
        <el-tag v-if="series.resolutionType && series.resolutionType !== 'NORMAL'" size="small" type="warning" class="ml8">
          {{ v4SeriesResolutionLabel(series.resolutionType) }}
        </el-tag>
        <el-tag v-if="series.qualificationStatusName" size="small" class="ml8">{{ series.qualificationStatusName }}</el-tag>
      </div>

      <div v-if="series.seriesStatus !== 'COMPLETED'" class="series-card__actions">
        <el-button size="small" type="warning" v-hasPermi="['basket:v4Stage:exception']" @click="openSeriesForfeit(series)">判定系列赛弃赛</el-button>
      </div>
      <div v-else-if="series.resolutionType === 'FORFEIT'" class="series-card__actions">
        <el-button size="small" type="info" v-hasPermi="['basket:v4Stage:exception']" @click="openRevokeForfeit(series)">撤销弃赛判定</el-button>
      </div>

      <div class="series-card__games">
        <div v-for="game in series.games || []" :key="game.matchId" class="series-game-row">
          <span class="series-game-row__label">Game {{ game.seriesGameNo }} / {{ series.bestOf }}</span>
          <span class="series-game-row__teams">{{ series.homeTeamName }} vs {{ series.awayTeamName }}</span>
          <span v-if="game.skipped" class="series-game-row__skip">{{ game.skippedReason || '无需进行' }}</span>
          <span v-else-if="game.homeScore != null" class="series-game-row__score">单场比分 {{ formatV4MatchScore(game.homeScore, game.awayScore) }}</span>
          <span v-else class="series-game-row__pending">{{ game.matchStatusName || '待比赛' }}</span>
          <el-tag size="small" class="series-game-row__status">{{ game.matchStatusName }}</el-tag>
          <el-button link type="primary" size="small" @click="$emit('openMatch', game.matchId)">详情</el-button>
        </div>
      </div>
    </div>

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
import { listV4MatchSeriesByStage, type BasketMatchSeriesDetailVo } from '@/api/cnpa/event-v4/match-series'
import { formatV4MatchScore, v4SeriesResolutionLabel, v4SeriesStatusLabel, v4SeriesStatusTagType } from '@/utils/v4AdminDisplay'

const props = defineProps<{ stageId?: number }>()
defineEmits<{ openMatch: [matchId?: number] }>()

const loading = ref(false)
const seriesList = ref<BasketMatchSeriesDetailVo[]>([])
const forfeitVisible = ref(false)
const forfeitSeries = ref<BasketMatchSeriesDetailVo>()
const forfeitTeamId = ref<number>()
const forfeitReason = ref('')

async function load() {
  if (!props.stageId) {
    seriesList.value = []
    return
  }
  loading.value = true
  try {
    const res = await listV4MatchSeriesByStage(props.stageId)
    seriesList.value = res.data || []
  } finally {
    loading.value = false
  }
}

watch(() => props.stageId, () => { void load() }, { immediate: true })

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
.match-series-panel { display: flex; flex-direction: column; gap: 16px; margin-bottom: 16px; }
.series-card { border: 1px solid var(--el-border-color); border-radius: 8px; padding: 16px; background: #fafafa; }
.series-card__header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.series-card__title { display: flex; align-items: center; gap: 8px; font-weight: 600; }
.series-card__code { color: #909399; font-size: 12px; font-weight: normal; }
.series-card__versus { display: grid; grid-template-columns: 1fr auto 1fr; gap: 12px; align-items: center; margin-bottom: 12px; }
.series-card__team { font-size: 15px; font-weight: 600; }
.series-card__team--away { text-align: right; }
.series-card__center { text-align: center; }
.series-card__set-score { font-size: 20px; font-weight: 700; color: #303133; }
.series-card__required { font-size: 12px; color: #909399; margin-top: 4px; }
.series-card__winner { margin-bottom: 12px; color: #67c23a; font-size: 13px; }
.series-card__games { border-top: 1px dashed var(--el-border-color); padding-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.series-game-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 13px; }
.series-game-row__label { font-weight: 600; min-width: 90px; }
.series-game-row__score { color: #606266; }
.series-game-row__skip { color: #909399; font-style: italic; }
.series-game-row__pending { color: #909399; }
.ml8 { margin-left: 8px; }
</style>
