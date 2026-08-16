<template>
  <div class="app-container">
    <el-alert title="比赛数据审核" description="按每场比赛的每支队伍独立审核；一方审核中不影响另一方提交。" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="queryParams" :inline="true" class="mb8">
      <el-form-item label="赛季">
        <el-select v-model="queryParams.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="queryParams.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!queryParams.seasonId">
          <el-option v-for="item in divisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.submissionStatus" clearable style="width: 180px">
          <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队"><el-input v-model="queryParams.teamName" clearable /></el-form-item>
      <el-form-item label="比赛编号"><el-input v-model="queryParams.matchNo" clearable /></el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="提交ID" prop="submissionId" width="90" />
      <el-table-column label="比赛编号" prop="matchNo" width="120" />
      <el-table-column label="轮次" prop="roundNo" width="70" align="center" />
      <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
      <el-table-column label="对阵" min-width="180">
        <template #default="scope">{{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}</template>
      </el-table-column>
      <el-table-column label="提交方" prop="submitTeamName" min-width="110" />
      <el-table-column label="提交人" prop="submitterName" width="100" />
      <el-table-column label="版本" prop="versionNo" width="70" align="center" />
      <el-table-column label="状态" prop="submissionStatus" width="130" align="center">
        <template #default="scope">
          <el-tag :type="v4SubmissionStatusTagType(scope.row.submissionStatus)" size="small" class="nowrap-tag" :title="scope.row.submissionStatus">
            {{ v4SubmissionStatusLabel(scope.row.submissionStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submitTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['basket:v4MatchSubmissionAudit:query']">详情</el-button>
          <el-button link type="success" @click="handleApprove(scope.row)" v-hasPermi="['basket:v4MatchSubmissionAudit:approve']" v-if="scope.row.submissionStatus === 'PENDING_REVIEW'">通过</el-button>
          <el-button link type="danger" @click="openReject(scope.row)" v-hasPermi="['basket:v4MatchSubmissionAudit:reject']" v-if="scope.row.submissionStatus === 'PENDING_REVIEW'">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="比赛数据提交详情" size="820px" append-to-body>
      <template v-if="detail">
        <el-alert
          v-if="detail.seriesContext"
          type="info"
          :closable="false"
          show-icon
          class="mb8"
          :title="`系列赛 ${detail.seriesContext.seriesCode || ''}：${detail.seriesContext.homeTeamName} vs ${detail.seriesContext.awayTeamName}`"
        >
          <template #default>
            <div>赛制：BO{{ detail.seriesContext.bestOf }} · Game {{ currentSeriesGameNo(detail) }} / {{ detail.seriesContext.bestOf }}</div>
            <div>当前局分：{{ detail.seriesContext.homeTeamName }} {{ detail.seriesContext.homeWinCount ?? 0 }} : {{ detail.seriesContext.awayWinCount ?? 0 }} {{ detail.seriesContext.awayTeamName }}</div>
            <div>Series 状态：{{ detail.seriesContext.seriesStatusName }}（审核的是单局，不是整个系列赛）</div>
          </template>
        </el-alert>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="提交ID">{{ detail.submissionId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="v4SubmissionStatusTagType(detail.submissionStatus)" size="small">{{ v4SubmissionStatusLabel(detail.submissionStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="比赛编号">{{ detail.matchNo }} / 第{{ detail.roundNo }}轮</el-descriptions-item>
          <el-descriptions-item label="版本">v{{ detail.versionNo }}</el-descriptions-item>
          <el-descriptions-item label="主队">{{ detail.homeTeamName }}</el-descriptions-item>
          <el-descriptions-item label="客队">{{ detail.awayTeamName }}</el-descriptions-item>
          <el-descriptions-item label="比分">{{ formatV4MatchScore(detail.homeScore, detail.awayScore) }}</el-descriptions-item>
          <el-descriptions-item label="提交方">{{ detail.submitTeamName }}</el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.submitterName }}</el-descriptions-item>
          <el-descriptions-item label="提交角色">{{ detail.submitterRole }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.submitRemark || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">{{ detail.rejectReason }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">本方球队统计</el-divider>
        <el-descriptions v-if="detail.teamStat" :column="4" border size="small">
          <el-descriptions-item label="得分">{{ detail.teamStat.score }}</el-descriptions-item>
          <el-descriptions-item label="篮板">{{ detail.teamStat.rebound }}</el-descriptions-item>
          <el-descriptions-item label="助攻">{{ detail.teamStat.assist }}</el-descriptions-item>
          <el-descriptions-item label="抢断">{{ detail.teamStat.steal }}</el-descriptions-item>
          <el-descriptions-item label="盖帽">{{ detail.teamStat.block }}</el-descriptions-item>
          <el-descriptions-item label="失误">{{ detail.teamStat.turnover }}</el-descriptions-item>
          <el-descriptions-item label="犯规">{{ detail.teamStat.foul }}</el-descriptions-item>
          <el-descriptions-item label="FGM/FGA">{{ detail.teamStat.fieldGoalMade ?? 0 }}/{{ detail.teamStat.fieldGoalAttempt ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="3PM/3PA">{{ detail.teamStat.threePointMade ?? 0 }}/{{ detail.teamStat.threePointAttempt ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="FTM/FTA">{{ detail.teamStat.freeThrowMade ?? 0 }}/{{ detail.teamStat.freeThrowAttempt ?? 0 }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">本方球员统计</el-divider>
        <el-table :data="detail.playerStats || []" size="small" border>
          <el-table-column label="球员" prop="playerName" min-width="120" />
          <el-table-column label="PTS" prop="points" width="70" />
          <el-table-column label="REB" prop="rebound" width="70" />
          <el-table-column label="AST" prop="assist" width="70" />
          <el-table-column label="STL" prop="steal" width="70" />
          <el-table-column label="BLK" prop="block" width="70" />
          <el-table-column label="FLS" prop="foul" width="70" />
          <el-table-column label="TO" prop="turnover" width="70" />
        </el-table>
        <el-divider content-position="left">比赛截图</el-divider>
        <div v-if="detail.materials?.length" class="audit-shots">
          <el-image
            v-for="item in detail.materials"
            :key="item.fileId"
            :src="materialUrls[item.fileId]"
            :preview-src-list="materialPreviewList"
            fit="cover"
            class="audit-shot"
          />
        </div>
        <el-empty v-else description="未上传截图" :image-size="60" />
        <el-divider content-position="left">审核记录</el-divider>
        <el-table :data="detail.historyVersions || []" size="small" border>
          <el-table-column label="版本" prop="versionNo" width="70" />
          <el-table-column label="状态" width="140">
            <template #default="scope">{{ v4SubmissionStatusLabel(scope.row.submissionStatus) }}</template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submitTime">
            <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="rejectOpen" title="驳回提交" width="480px" append-to-body>
      <el-form :model="rejectForm" label-width="90px">
        <el-form-item label="驳回原因" required><el-input v-model="rejectForm.rejectReason" type="textarea" /></el-form-item>
        <el-form-item label="审核备注"><el-input v-model="rejectForm.auditRemark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4MatchSubmissionAudit">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveV4MatchSubmissionAudit,
  getV4MatchSubmissionAuditDetail,
  listV4MatchSubmissionAudit,
  rejectV4MatchSubmissionAudit,
  type V4MatchSubmissionAuditDetailVo,
  type V4MatchSubmissionAuditListVo
} from '@/api/cnpa/event-v4/match-submission-audit'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  V4_SUBMISSION_STATUS_FILTER_OPTIONS,
  formatV4MatchScore,
  v4SubmissionStatusLabel,
  v4SubmissionStatusTagType
} from '@/utils/v4AdminDisplay'
import { parseTime } from '@/utils/ruoyi'
import axios from 'axios'
import { getToken } from '@/utils/auth'

const baseURL = import.meta.env.VITE_APP_BASE_API

const STATUS_OPTIONS = V4_SUBMISSION_STATUS_FILTER_OPTIONS

const loading = ref(false)
const total = ref(0)
const dataList = ref<V4MatchSubmissionAuditListVo[]>([])
const queryParams = reactive({ pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, matchId: undefined, submissionStatus: undefined, teamName: undefined, matchNo: undefined })
const detailOpen = ref(false)
const detail = ref<V4MatchSubmissionAuditDetailVo | null>(null)
const materialUrls = ref<Record<number, string>>({})
const materialPreviewList = ref<string[]>([])
const materialObjectUrls = ref<string[]>([])
const rejectOpen = ref(false)
const rejectForm = reactive({ submissionId: 0, rejectReason: '', auditRemark: '' })
const seasonOptions = ref<V4AdminOptionVo[]>([])
const divisionOptions = ref<V4AdminOptionVo[]>([])

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
  queryParams.divisionId = undefined
  loadDivisions(seasonId)
}

function getList() {
  loading.value = true
  listV4MatchSubmissionAudit(queryParams).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, seasonId: undefined, divisionId: undefined, matchId: undefined, submissionStatus: undefined, teamName: undefined, matchNo: undefined })
  divisionOptions.value = []
  getList()
}

function openDetail(row: V4MatchSubmissionAuditListVo) {
  loadDetail(row.submissionId)
}

function revokeMaterialUrls() {
  materialObjectUrls.value.forEach((url) => URL.revokeObjectURL(url))
  materialObjectUrls.value = []
  materialUrls.value = {}
  materialPreviewList.value = []
}

async function loadMaterialPreviews(detailVo: V4MatchSubmissionAuditDetailVo) {
  revokeMaterialUrls()
  const next: Record<number, string> = {}
  const preview: string[] = []
  for (const item of detailVo.materials || []) {
    if (!item.fileId) continue
    try {
      const res = await axios.get(`${baseURL}/basket/v4/match-submission-audit/material/${item.fileId}`, {
        responseType: 'blob',
        headers: { Authorization: 'Bearer ' + getToken() }
      })
      const url = URL.createObjectURL(res.data)
      next[item.fileId] = url
      preview.push(url)
      materialObjectUrls.value.push(url)
    } catch {
      next[item.fileId] = ''
    }
  }
  materialUrls.value = next
  materialPreviewList.value = preview
}

function loadDetail(submissionId: number) {
  getV4MatchSubmissionAuditDetail(submissionId).then(async res => {
    detail.value = res.data
    detailOpen.value = true
    if (res.data) {
      await loadMaterialPreviews(res.data)
    }
  })
}

function currentSeriesGameNo(detailVo: V4MatchSubmissionAuditDetailVo): number | string {
  const games = detailVo.seriesContext?.games || []
  const current = games.find(g => g.matchId === detailVo.matchId)
  return current?.seriesGameNo ?? '-'
}

function handleApprove(row: V4MatchSubmissionAuditListVo) {
  ElMessageBox.confirm('确认审核通过并写入官方结果、重算统计？', '提示').then(() => {
    return approveV4MatchSubmissionAudit(row.submissionId)
  }).then(() => {
    ElMessage.success('审核通过')
    getList()
    if (detailOpen.value && detail.value?.submissionId === row.submissionId) {
      loadDetail(row.submissionId)
    }
  })
}

function openReject(row: V4MatchSubmissionAuditListVo) {
  rejectForm.submissionId = row.submissionId
  rejectForm.rejectReason = ''
  rejectForm.auditRemark = ''
  rejectOpen.value = true
}

function submitReject() {
  rejectV4MatchSubmissionAudit(rejectForm.submissionId, { rejectReason: rejectForm.rejectReason, auditRemark: rejectForm.auditRemark }).then(() => {
    ElMessage.success('已驳回')
    rejectOpen.value = false
    getList()
  })
}

onMounted(async () => {
  await loadSeasonOptions()
  getList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
.audit-shots { display: flex; flex-wrap: wrap; gap: 12px; }
.audit-shot { width: 140px; height: 90px; border: 1px solid #ebeef5; }
</style>
