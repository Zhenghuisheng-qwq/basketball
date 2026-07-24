<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="所属赛事">
        <el-select v-model="selectedTournamentId" placeholder="请选择赛事" filterable clearable style="width: 260px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季">
        <el-select v-model="selectedSeasonId" placeholder="请选择赛季" filterable style="width: 260px" @change="loadDetail">
          <el-option v-for="item in seasonOptions" :key="item.seasonId || item.id" :label="item.seasonName" :value="item.seasonId || item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadDetail" :disabled="!selectedSeasonId" v-hasPermi="['basket:dataSettlement:query']">查询摘要</el-button>
        <el-button type="warning" icon="CircleCheck" @click="handleValidate" :disabled="!selectedSeasonId" v-hasPermi="['basket:dataSettlement:query']">校验</el-button>
        <el-button type="danger" icon="Refresh" @click="handleRebuild" :disabled="!selectedSeasonId" v-hasPermi="['basket:dataSettlement:rebuild']" :loading="rebuilding">重算赛季</el-button>
      </el-form-item>
    </el-form>

    <el-row v-if="summary" :gutter="16" class="mb8">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">已确认比赛</div>
          <div class="stat-value">{{ summary.confirmedMatchCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">未完成 / 重开 / 取消</div>
          <div class="stat-value">{{ summary.unfinishedMatchCount ?? 0 }} / {{ summary.reopenedMatchCount ?? 0 }} / {{ summary.cancelledMatchCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">积分榜 / 球队统计 / 球员统计</div>
          <div class="stat-value">{{ summary.standingCount ?? 0 }} / {{ summary.teamSeasonStatCount ?? 0 }} / {{ summary.playerSeasonStatCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-label">球队榜 / 球员榜</div>
          <div class="stat-value">{{ summary.teamRankCount ?? 0 }} / {{ summary.playerRankCount ?? 0 }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-descriptions v-if="summary" :column="3" border class="mb8">
      <el-descriptions-item label="赛事">{{ summary.tournamentName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="赛季">{{ summary.seasonName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="赛季状态">
        <dict-tag :options="SEASON_STATUS_OPTIONS" :value="summary.seasonStatus" />
      </el-descriptions-item>
      <el-descriptions-item label="积分规则">{{ summary.standingRuleConfigured ? '已配置' : '未配置' }}</el-descriptions-item>
      <el-descriptions-item label="最近结算时间">{{ parseTime(summary.lastSettlementTime) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最近操作人">{{ summary.lastOperatorName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="积分榜时间">{{ parseTime(summary.lastStandingTime) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="球队统计时间">{{ parseTime(summary.lastTeamStatTime) || '-' }}</el-descriptions-item>
      <el-descriptions-item label="球员统计时间">{{ parseTime(summary.lastPlayerStatTime) || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-alert
      v-if="validation"
      :title="validation.passed ? '校验通过，可执行重算' : `校验未通过，共 ${validation.errorCount ?? 0} 项问题`"
      :type="validation.passed ? 'success' : 'error'"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-divider v-if="validation?.errors?.length" content-position="left">校验错误列表</el-divider>
    <el-table v-if="validation?.errors?.length" :data="validation.errors" v-loading="validating" border>
      <el-table-column label="比赛ID" prop="matchId" width="100" />
      <el-table-column label="比赛编号" prop="matchNo" min-width="140" />
      <el-table-column label="原因" prop="reason" min-width="280" :show-overflow-tooltip="true" />
    </el-table>

    <el-empty v-if="!summary && !loading" description="请选择赛事与赛季后查询结算摘要" />
  </div>
</template>

<script setup lang="ts" name="BasketDataSettlement">
import {
  getSeasonSettlementSummary,
  listSeasonSettlementSummaries,
  rebuildSeasonSettlement,
  validateSeasonSettlement
} from '@/api/basket/dataSettlement'
import { listSeasonByTournament } from '@/api/basket/season'
import { listTournament } from '@/api/basket/tournament'
import type {
  SeasonListItem,
  SeasonSettlementSummary,
  SettlementValidationResult,
  TournamentListItem
} from '@/types'
import { SEASON_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const validating = ref(false)
const rebuilding = ref(false)
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<(SeasonListItem & { seasonId?: number })[]>([])
const selectedTournamentId = ref<number>()
const selectedSeasonId = ref<number>()
const summary = ref<SeasonSettlementSummary>()
const validation = ref<SettlementValidationResult>()

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows || []
    if (tournamentOptions.value.length > 0) {
      selectedTournamentId.value = tournamentOptions.value[0].id
      loadSeasons()
    }
  })
}

function loadSeasons() {
  summary.value = undefined
  validation.value = undefined
  selectedSeasonId.value = undefined
  seasonOptions.value = []
  if (!selectedTournamentId.value) return
  Promise.all([
    listSeasonByTournament(selectedTournamentId.value),
    listSeasonSettlementSummaries(selectedTournamentId.value)
  ]).then(([seasonRes, summaryRes]) => {
    const seasons = seasonRes.data || []
    const summaries = summaryRes.data || []
    if (summaries.length > 0) {
      seasonOptions.value = summaries.map(s => ({
        id: s.seasonId,
        seasonId: s.seasonId,
        seasonName: s.seasonName,
        seasonStatus: s.seasonStatus
      }))
    } else {
      seasonOptions.value = seasons
    }
    selectedSeasonId.value = seasonOptions.value[0]?.seasonId || seasonOptions.value[0]?.id
    if (selectedSeasonId.value) {
      loadDetail()
    }
  })
}

function handleTournamentChange() {
  loadSeasons()
}

function loadDetail() {
  if (!selectedSeasonId.value) return
  loading.value = true
  validation.value = undefined
  getSeasonSettlementSummary(selectedSeasonId.value).then(res => {
    summary.value = res.data
  }).finally(() => {
    loading.value = false
  })
}

function handleValidate() {
  if (!selectedSeasonId.value) return
  validating.value = true
  validateSeasonSettlement(selectedSeasonId.value).then(res => {
    validation.value = res.data
    if (res.data?.passed) {
      proxy.$modal.msgSuccess('校验通过')
    } else {
      proxy.$modal.msgWarning(`校验未通过，共 ${res.data?.errorCount ?? 0} 项问题`)
    }
  }).finally(() => {
    validating.value = false
  })
}

function handleRebuild() {
  if (!selectedSeasonId.value) return
  proxy.$modal.confirm('重算将覆盖当前赛季的积分榜、赛季统计和排行榜结果。确认继续？').then(() => {
    rebuilding.value = true
    return rebuildSeasonSettlement(selectedSeasonId.value!)
  }).then(res => {
    const result = res.data
    if (result?.success === false) {
      proxy.$modal.msgError(result.message || '重算失败')
    } else {
      proxy.$modal.msgSuccess(result?.message || '重算完成')
    }
    loadDetail()
    handleValidate()
  }).catch(() => {}).finally(() => {
    rebuilding.value = false
  })
}

loadTournaments()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.stat-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
}
</style>
