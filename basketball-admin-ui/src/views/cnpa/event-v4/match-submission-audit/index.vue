<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" class="mb8">
      <el-tab-pane label="单场提交审核" name="game" />
      <el-tab-pane label="系列赛审核" name="series" />
    </el-tabs>
    <el-alert v-if="activeTab === 'game'" title="比赛数据审核" description="按每场比赛的每支队伍独立审核；一方审核中不影响另一方提交。旧单队 Submission 继续在此审核。" type="info" :closable="false" show-icon class="mb8" />
    <el-alert v-else title="系列赛审核" description="BO3/BO5/BO7 由获胜队长提交整个系列赛后，在此一次性 Approve / Reject，不再逐局点通过。" type="info" :closable="false" show-icon class="mb8" />

    <div v-show="activeTab === 'game'">
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
          <el-descriptions-item label="2PM/2PA">{{ detail.teamStat.fieldGoalMade ?? 0 }}/{{ detail.teamStat.fieldGoalAttempt ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="3PM/3PA">{{ detail.teamStat.threePointMade ?? 0 }}/{{ detail.teamStat.threePointAttempt ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="FTM/FTA">{{ detail.teamStat.freeThrowMade ?? 0 }}/{{ detail.teamStat.freeThrowAttempt ?? 0 }}</el-descriptions-item>
        </el-descriptions>
        <p v-if="detail.periodScores?.length" class="mb8">节比分：{{ detail.periodScores.map(p => `Q${p.periodNo} ${p.homeScore}:${p.awayScore}`).join(' / ') }}</p>
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
          <template v-for="item in detail.materials" :key="item.fileId">
            <img
              v-if="materialUrls[String(item.fileId)]"
              :src="materialUrls[String(item.fileId)]"
              class="audit-shot"
              alt="比赛截图"
              @click="openShotPreview(item.fileId)"
            />
            <div v-else class="audit-shot audit-shot--pending">加载中</div>
          </template>
        </div>
        <el-image-viewer
          v-if="previewVisible && materialPreviewList.length"
          :url-list="materialPreviewList"
          :initial-index="previewIndex"
          @close="previewVisible = false"
        />
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
    </div>

    <div v-show="activeTab === 'series'">
      <el-form :inline="true" class="mb8">
        <el-form-item label="状态">
          <el-select v-model="seriesQuery.status" clearable style="width: 180px">
            <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="系列赛编号"><el-input v-model="seriesQuery.seriesCode" clearable /></el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="getSeriesList">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="seriesLoading" :data="seriesList" border>
        <el-table-column label="任务ID" prop="seriesSubmissionId" width="90" />
        <el-table-column label="系列赛" prop="seriesCode" min-width="140" />
        <el-table-column label="赛制" width="80"><template #default="scope">BO{{ scope.row.bestOf }}</template></el-table-column>
        <el-table-column label="对阵" min-width="180"><template #default="scope">{{ scope.row.homeTeamName }} vs {{ scope.row.awayTeamName }}</template></el-table-column>
        <el-table-column label="声明比分" width="110"><template #default="scope">{{ scope.row.declaredHomeWinCount }} : {{ scope.row.declaredAwayWinCount }}</template></el-table-column>
        <el-table-column label="提交方" prop="submitTeamName" min-width="110" />
        <el-table-column label="状态" width="130">
          <template #default="scope">
            <el-tag :type="v4SubmissionStatusTagType(scope.row.status)" size="small">{{ v4SubmissionStatusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="scope">
            <el-button link type="primary" @click="openSeriesDetail(scope.row)">详情</el-button>
            <el-button link type="success" v-if="scope.row.status === 'PENDING_REVIEW'" @click="handleSeriesApprove(scope.row)">通过系列赛</el-button>
            <el-button link type="danger" v-if="scope.row.status === 'PENDING_REVIEW'" @click="openSeriesReject(scope.row)">驳回系列赛</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination :total="seriesTotal" v-model:page="seriesQuery.pageNum" v-model:limit="seriesQuery.pageSize" @pagination="getSeriesList" />
      <el-drawer v-model="seriesDetailOpen" title="系列赛提交详情" size="920px" append-to-body>
        <template v-if="seriesDetail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="系列赛">{{ seriesDetail.seriesCode }} / BO{{ seriesDetail.bestOf }}</el-descriptions-item>
            <el-descriptions-item label="声明比分">{{ seriesDetail.declaredHomeWinCount }} : {{ seriesDetail.declaredAwayWinCount }}</el-descriptions-item>
            <el-descriptions-item label="对阵">{{ seriesDetail.homeTeamName }} vs {{ seriesDetail.awayTeamName }}</el-descriptions-item>
            <el-descriptions-item label="提交方">{{ seriesDetail.submitTeamName }}</el-descriptions-item>
            <el-descriptions-item label="声明胜方">{{ seriesDetail.declaredWinnerTeamName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ v4SubmissionStatusLabel(seriesDetail.status) }}</el-descriptions-item>
            <el-descriptions-item label="版本">v{{ seriesDetail.versionNo }}</el-descriptions-item>
            <el-descriptions-item v-if="seriesDetail.rejectReason" label="驳回原因" :span="2">{{ seriesDetail.rejectReason }}</el-descriptions-item>
          </el-descriptions>
          <el-tabs>
            <el-tab-pane v-for="game in seriesDetail.games || []" :key="game.submissionId" :label="`G${game.seriesGameNo} ${game.homeScore}:${game.awayScore}`">
              <p>最终比分 {{ game.homeScore }} : {{ game.awayScore }}</p>
              <p v-if="game.periodScores?.length">节比分：{{ game.periodScores.map(p => `Q${p.periodNo} ${p.homeScore}:${p.awayScore}`).join(' / ') }}</p>
              <p v-if="game.submitRemark">备注：{{ game.submitRemark }}</p>
              <el-alert v-if="game.validationErrors?.length" type="error" :closable="false" class="mb8" :title="game.validationErrors[0]">
                <ul>
                  <li v-for="item in game.validationErrors" :key="item">{{ item }}</li>
                </ul>
              </el-alert>
              <el-divider content-position="left">自动球队汇总</el-divider>
              <el-descriptions :column="4" border size="small" class="mb8">
                <el-descriptions-item :label="`${seriesDetail.homeTeamName} PTS`">{{ game.homeTeamStat?.score ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="REB">{{ game.homeTeamStat?.rebound ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="2PM/2PA">{{ game.homeTeamStat?.fieldGoalMade ?? 0 }}/{{ game.homeTeamStat?.fieldGoalAttempt ?? 0 }}</el-descriptions-item>
                <el-descriptions-item :label="`${seriesDetail.awayTeamName} PTS`">{{ game.awayTeamStat?.score ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="REB">{{ game.awayTeamStat?.rebound ?? '-' }}</el-descriptions-item>
                <el-descriptions-item label="2PM/2PA">{{ game.awayTeamStat?.fieldGoalMade ?? 0 }}/{{ game.awayTeamStat?.fieldGoalAttempt ?? 0 }}</el-descriptions-item>
              </el-descriptions>
              <el-divider content-position="left">{{ seriesDetail.homeTeamName }} 球员</el-divider>
              <el-table :data="game.homePlayerStats || []" size="small" border>
                <el-table-column label="球员" prop="playerName" min-width="110" />
                <el-table-column label="PTS" prop="points" width="60" />
                <el-table-column label="REB" prop="rebound" width="60" />
                <el-table-column label="AST" prop="assist" width="60" />
                <el-table-column label="STL" prop="steal" width="60" />
                <el-table-column label="BLK" prop="block" width="60" />
                <el-table-column label="PF" prop="foul" width="60" />
                <el-table-column label="TO" prop="turnover" width="60" />
                <el-table-column label="2PM" prop="fieldGoalMade" width="60" />
                <el-table-column label="2PA" prop="fieldGoalAttempt" width="60" />
                <el-table-column label="3PM" prop="threePointMade" width="60" />
                <el-table-column label="3PA" prop="threePointAttempt" width="60" />
                <el-table-column label="FTM" prop="freeThrowMade" width="60" />
                <el-table-column label="FTA" prop="freeThrowAttempt" width="60" />
              </el-table>
              <el-divider content-position="left">{{ seriesDetail.awayTeamName }} 球员</el-divider>
              <el-table :data="game.awayPlayerStats || []" size="small" border>
                <el-table-column label="球员" prop="playerName" min-width="110" />
                <el-table-column label="PTS" prop="points" width="60" />
                <el-table-column label="REB" prop="rebound" width="60" />
                <el-table-column label="AST" prop="assist" width="60" />
                <el-table-column label="STL" prop="steal" width="60" />
                <el-table-column label="BLK" prop="block" width="60" />
                <el-table-column label="PF" prop="foul" width="60" />
                <el-table-column label="TO" prop="turnover" width="60" />
                <el-table-column label="2PM" prop="fieldGoalMade" width="60" />
                <el-table-column label="2PA" prop="fieldGoalAttempt" width="60" />
                <el-table-column label="3PM" prop="threePointMade" width="60" />
                <el-table-column label="3PA" prop="threePointAttempt" width="60" />
                <el-table-column label="FTM" prop="freeThrowMade" width="60" />
                <el-table-column label="FTA" prop="freeThrowAttempt" width="60" />
              </el-table>
              <p class="mt8">截图 {{ game.materials?.length || 0 }} 张</p>
              <div class="series-shots">
                <el-image
                  v-for="shot in game.materials || []"
                  :key="shot.fileId"
                  :src="seriesMaterialUrls[String(shot.fileId)] || ''"
                  :preview-src-list="(game.materials || []).map(item => seriesMaterialUrls[String(item.fileId)] || '').filter(Boolean)"
                  fit="cover"
                  style="width: 120px; height: 80px; margin-right: 8px"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>
      </el-drawer>
    </div>

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
import {
  approveV4SeriesSubmissionAudit,
  getV4SeriesSubmissionAuditDetail,
  listV4SeriesSubmissionAudit,
  rejectV4SeriesSubmissionAudit,
  type V4SeriesSubmissionAuditDetailVo,
  type V4SeriesSubmissionAuditListVo
} from '@/api/cnpa/event-v4/series-submission-audit'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  V4_SUBMISSION_STATUS_FILTER_OPTIONS,
  formatV4MatchScore,
  v4SubmissionStatusLabel,
  v4SubmissionStatusTagType
} from '@/utils/v4AdminDisplay'
import { parseTime } from '@/utils/ruoyi'
import { fetchAuthedImageUrl, resolveSubmissionMaterialUrl } from '@/utils/resolveAttachmentUrl'

const STATUS_OPTIONS = V4_SUBMISSION_STATUS_FILTER_OPTIONS
const activeTab = ref('game')
const seriesLoading = ref(false)
const seriesTotal = ref(0)
const seriesList = ref<V4SeriesSubmissionAuditListVo[]>([])
const seriesQuery = reactive({ pageNum: 1, pageSize: 10, status: undefined as string | undefined, seriesCode: undefined as string | undefined })
const seriesDetailOpen = ref(false)
const seriesDetail = ref<V4SeriesSubmissionAuditDetailVo | null>(null)
const seriesMaterialUrls = ref<Record<string, string>>({})
const seriesRejectId = ref(0)

const loading = ref(false)
const total = ref(0)
const dataList = ref<V4MatchSubmissionAuditListVo[]>([])
const queryParams = reactive({ pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, matchId: undefined, submissionStatus: undefined, teamName: undefined, matchNo: undefined })
const detailOpen = ref(false)
const detail = ref<V4MatchSubmissionAuditDetailVo | null>(null)
const previewVisible = ref(false)
const previewIndex = ref(0)
const materialUrls = ref<Record<string, string>>({})
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

function openShotPreview(fileId: number | string) {
  const materials = detail.value?.materials || []
  const idx = materials.findIndex((item) => String(item.fileId) === String(fileId))
  previewIndex.value = idx >= 0 ? idx : 0
  previewVisible.value = true
}

function revokeMaterialUrls() {
  materialObjectUrls.value.forEach((url) => URL.revokeObjectURL(url))
  materialObjectUrls.value = []
  materialUrls.value = {}
  materialPreviewList.value = []
}

async function loadMaterialPreviews(detailVo: V4MatchSubmissionAuditDetailVo) {
  revokeMaterialUrls()
  const next: Record<string, string> = {}
  const preview: string[] = []
  for (const item of detailVo.materials || []) {
    if (!item.fileId) continue
    const key = String(item.fileId)
    try {
      const url = await fetchAuthedImageUrl(resolveSubmissionMaterialUrl(key))
      next[key] = url
      preview.push(url)
      materialObjectUrls.value.push(url)
    } catch {
      next[key] = ''
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
  const req = { rejectReason: rejectForm.rejectReason, auditRemark: rejectForm.auditRemark }
  const task = seriesRejectId.value
    ? rejectV4SeriesSubmissionAudit(seriesRejectId.value, req)
    : rejectV4MatchSubmissionAudit(rejectForm.submissionId, req)
  task.then(() => {
    ElMessage.success('已驳回')
    rejectOpen.value = false
    seriesRejectId.value = 0
    getList()
    getSeriesList()
  })
}

function getSeriesList() {
  seriesLoading.value = true
  listV4SeriesSubmissionAudit(seriesQuery).then(res => {
    seriesList.value = res.rows || []
    seriesTotal.value = res.total || 0
  }).finally(() => { seriesLoading.value = false })
}

function openSeriesDetail(row: V4SeriesSubmissionAuditListVo) {
  getV4SeriesSubmissionAuditDetail(row.seriesSubmissionId).then(async res => {
    seriesDetail.value = res.data || null
    seriesDetailOpen.value = true
    seriesMaterialUrls.value = {}
    for (const game of res.data?.games || []) {
      for (const item of game.materials || []) {
        const key = String(item.fileId || '')
        if (!key || seriesMaterialUrls.value[key]) continue
        try {
          seriesMaterialUrls.value[key] = await fetchAuthedImageUrl(resolveSubmissionMaterialUrl(key))
        } catch {
          /* keep empty */
        }
      }
    }
  })
}

function handleSeriesApprove(row: V4SeriesSubmissionAuditListVo) {
  ElMessageBox.confirm('确认一次性通过整个系列赛并按各局正式结算？', '提示').then(() => {
    return approveV4SeriesSubmissionAudit(row.seriesSubmissionId)
  }).then(() => {
    ElMessage.success('系列赛已通过')
    getSeriesList()
  })
}

function openSeriesReject(row: V4SeriesSubmissionAuditListVo) {
  seriesRejectId.value = row.seriesSubmissionId
  rejectForm.submissionId = 0
  rejectForm.rejectReason = ''
  rejectForm.auditRemark = ''
  rejectOpen.value = true
}

onMounted(async () => {
  await loadSeasonOptions()
  getList()
  getSeriesList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
.audit-shots { display: flex; flex-wrap: wrap; gap: 12px; }
.audit-shot { width: 140px; height: 90px; border: 1px solid #ebeef5; object-fit: cover; cursor: pointer; }
.audit-shot--pending { display: flex; align-items: center; justify-content: center; color: #909399; font-size: 12px; }
</style>
