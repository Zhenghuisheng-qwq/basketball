<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="处罚管理"
      description="用于记录和处理球员、球队在赛事中的纪律处罚，包括警告、禁赛、取消资格、扣分和处罚解除。处罚记录将进入对应球员或球队档案，并影响报名、参赛或阵容资格。本页不处理比赛数据审核、阵容变更审核或赛程调整。"
    />

    <el-tabs v-model="activeTab" class="mb8" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="球员处罚" name="PLAYER" />
      <el-tab-pane label="球队处罚" name="TEAM" />
    </el-tabs>

    <el-form :inline="true" class="mb8">
      <el-form-item label="所属赛季">
        <el-select
          v-model="query.seasonId"
          clearable
          filterable
          remote
          reserve-keyword
          :remote-method="searchQuerySeasons"
          :loading="querySeasonLoading"
          placeholder="搜索系列 / 赛季 / 赛区"
          style="width: 280px"
        >
          <el-option v-for="item in querySeasonOptions" :key="item.id" :label="item.label" :value="item.id">
            <div class="selector-option">
              <div>{{ item.label }}</div>
              <div v-if="item.secondaryLabel || item.status" class="selector-option__sub">
                <span v-if="item.secondaryLabel">{{ item.secondaryLabel }}</span>
                <span v-if="item.status">{{ formatSeasonStatus(item.status) }}</span>
              </div>
            </div>
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.recordStatus" clearable style="width: 140px">
          <el-option v-for="item in DISCIPLINE_RECORD_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="处罚编号/标题" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:discipline:list']">查询</el-button>
        <el-button type="success" icon="Plus" @click="openCreate" v-hasPermi="['basket:discipline:add']">新增处罚</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="displayRows" v-loading="loading" border>
      <el-table-column label="处罚编号" prop="recordNo" min-width="150" fixed />
      <el-table-column label="处罚对象" min-width="160">
        <template #default="scope">{{ formatDisciplineTargetName(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="对象类型" width="100">
        <template #default="scope">{{ formatDisciplineTargetType(scope.row.targetType) }}</template>
      </el-table-column>
      <el-table-column label="所属赛事" min-width="160">
        <template #default="scope">{{ formatDisciplineSeasonLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="所属 Division" min-width="130">
        <template #default="scope">{{ formatDisciplineDivisionLabel(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="处罚类型" width="130">
        <template #default="scope">{{ formatDisciplinePenaltyCode(scope.row.penaltyCode) }}</template>
      </el-table-column>
      <el-table-column label="处罚原因摘要" min-width="150" show-overflow-tooltip>
        <template #default="scope">{{ formatReasonSummary(scope.row.reasonText) }}</template>
      </el-table-column>
      <el-table-column label="生效时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.effectiveStartTime) }}</template>
      </el-table-column>
      <el-table-column label="失效时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.effectiveEndTime) }}</template>
      </el-table-column>
      <el-table-column label="业务状态" width="100">
        <template #default="scope">
          <el-tag size="small" :type="businessStatusTagType(scope.row.businessStatus || scope.row.recordStatus)">
            {{ formatDisciplineBusinessStatus(scope.row) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="执行状态" width="110">
        <template #default="scope">
          <el-tag v-if="scope.row.executionStatus" size="small" :type="executionStatusTagType(scope.row.executionStatus)">
            {{ formatDisciplineExecutionStatus(scope.row.executionStatus) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="赛事影响" min-width="220">
        <template #default="scope">{{ formatEventImpactSummary(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="公告状态" width="90">
        <template #default="scope">{{ formatAnnouncementStatus(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetail(scope.row.id)" v-hasPermi="['basket:discipline:query']">详情</el-button>
          <el-button link type="success" v-if="scope.row.recordStatus === 'DRAFT'" @click="doActivate(scope.row.id)" v-hasPermi="['basket:discipline:activate']">生效</el-button>
          <el-button link type="danger" v-if="scope.row.recordStatus === 'ACTIVE'" @click="doRevoke(scope.row.id)" v-hasPermi="['basket:discipline:revoke']">撤销处罚</el-button>
          <el-button link type="warning" v-if="canRetryExecution(scope.row)" @click="doRetryExecution(scope.row.id)" v-hasPermi="['basket:discipline:activate']">重试执行</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !displayRows.length" description="暂无处罚记录，可点击「新增处罚」创建" />

    <pagination :total="displayTotal" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="createOpen" title="新增处罚" width="680px" destroy-on-close>
      <el-form label-width="108px">
        <el-form-item label="处罚对象类型" required>
          <el-select v-model="form.targetType" style="width: 100%" @change="onTargetTypeChange">
            <el-option v-for="item in DISCIPLINE_TARGET_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="所属赛季" required>
          <el-select
            v-model="form.seasonId"
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="searchFormSeasons"
            :loading="formSeasonLoading"
            placeholder="搜索系列 / 赛季 / 赛区"
            style="width: 100%"
            @change="onSeasonChange"
          >
            <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.label" :value="item.id">
              <div class="selector-option">
                <div>{{ item.label }}</div>
                <div v-if="item.secondaryLabel || item.status" class="selector-option__sub">
                  <span v-if="item.secondaryLabel">{{ item.secondaryLabel }}</span>
                  <span v-if="item.status">{{ formatSeasonStatus(item.status) }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.targetType === 'PLAYER'" label="球员" required>
          <el-select
            v-model="playerSelectId"
            filterable
            remote
            reserve-keyword
            clearable
            :disabled="!form.seasonId"
            :remote-method="searchFormPlayers"
            :loading="formPlayerLoading"
            :placeholder="form.seasonId ? '搜索球员姓名 / CNPA ID / 游戏ID' : '请先选择所属赛季'"
            style="width: 100%"
            @change="onPlayerChange"
            @visible-change="onPlayerVisible"
          >
            <el-option
              v-for="item in formPlayerOptions"
              :key="String(item.eventPlayerId || item.id)"
              :label="item.label"
              :value="item.eventPlayerId || item.id"
            >
              <div class="selector-option">
                <div>{{ item.label }}</div>
                <div v-if="item.secondaryLabel" class="selector-option__sub">{{ item.secondaryLabel }}</div>
              </div>
            </el-option>
          </el-select>
          <div v-if="form.seasonId && !formPlayerLoading && playerSearched && !formPlayerOptions.length" class="selector-empty">暂无可处罚球员</div>
        </el-form-item>

        <el-form-item v-if="form.targetType === 'PLAYER' && selectedTeamName" label="所属球队">
          <el-input :model-value="selectedTeamName" disabled />
        </el-form-item>

        <el-form-item v-if="isTeamTarget" label="球队" required>
          <el-select
            v-model="teamSelectId"
            filterable
            remote
            reserve-keyword
            clearable
            :disabled="!form.seasonId"
            :remote-method="searchFormTeams"
            :loading="formTeamLoading"
            :placeholder="form.seasonId ? '搜索球队名称 / 简称' : '请先选择所属赛季'"
            style="width: 100%"
            @change="onTeamChange"
            @visible-change="onTeamVisible"
          >
            <el-option v-for="item in formTeamOptions" :key="item.id" :label="item.label" :value="item.id">
              <div class="selector-option">
                <div>{{ item.label }}</div>
                <div v-if="item.secondaryLabel" class="selector-option__sub">{{ item.secondaryLabel }}</div>
              </div>
            </el-option>
          </el-select>
          <div v-if="form.seasonId && !formTeamLoading && teamSearched && !formTeamOptions.length" class="selector-empty">暂无可处罚球队</div>
        </el-form-item>

        <el-form-item label="处罚类型" required>
          <el-select v-model="form.disciplineType" style="width: 100%">
            <el-option v-for="item in DISCIPLINE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="处罚措施" required>
          <el-select v-model="form.penaltyCode" style="width: 100%" @change="onPenaltyCodeChange">
            <el-option v-for="item in DISCIPLINE_PENALTY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="例如：比赛消极行为警告" maxlength="256" show-word-limit />
        </el-form-item>

        <el-form-item label="处罚原因" required>
          <el-input v-model="form.reasonText" type="textarea" :rows="3" placeholder="请填写处罚原因" />
        </el-form-item>

        <el-form-item v-if="showsMatchSuspension(form.penaltyCode)" label="禁赛场次" required>
          <el-input-number v-model="form.matchSuspensionCount" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="showsPointDeduction(form.penaltyCode)" label="扣除积分" required>
          <el-input-number v-model="form.pointDeduction" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>

        <el-form-item v-if="showsDateRange(form.penaltyCode)" label="生效时间" required>
          <el-date-picker
            v-model="form.effectiveStartTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择生效时间"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item v-if="showsDateRange(form.penaltyCode)" label="结束时间" required>
          <el-date-picker
            v-model="form.effectiveEndTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="处罚详情" size="680px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="处罚编号">{{ detail.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="处罚对象">{{ formatDisciplineTargetName(detail) }}</el-descriptions-item>
          <el-descriptions-item label="对象类型">{{ formatDisciplineTargetType(detail.targetType) }}</el-descriptions-item>
          <el-descriptions-item label="所属赛事">{{ formatDisciplineSeasonLabel(detail) }}</el-descriptions-item>
          <el-descriptions-item label="所属 Division">{{ formatDisciplineDivisionLabel(detail) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.targetType === 'PLAYER' && detail.teamName" label="所属球队">{{ detail.teamName }}</el-descriptions-item>
          <el-descriptions-item label="处罚类型">{{ formatDisciplinePenaltyCode(detail.penaltyCode) }}</el-descriptions-item>
          <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
          <el-descriptions-item label="业务状态">
            <el-tag size="small" :type="businessStatusTagType(detail.recordStatus)">{{ formatDisciplineBusinessStatus(detail) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="执行状态">
            <el-tag v-if="detail.executionStatus" size="small" :type="executionStatusTagType(detail.executionStatus)">
              {{ formatDisciplineExecutionStatus(detail.executionStatus) }}
            </el-tag>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="处罚原因">{{ detail.reasonText }}</el-descriptions-item>
          <el-descriptions-item v-if="showsMatchSuspension(detail.penaltyCode)" label="禁赛场次">{{ detail.matchSuspensionCount }}</el-descriptions-item>
          <el-descriptions-item v-if="showsMatchSuspension(detail.penaltyCode)" label="剩余禁赛">{{ detail.matchSuspensionRemaining }}</el-descriptions-item>
          <el-descriptions-item v-if="showsPointDeduction(detail.penaltyCode) || detail.pointDeduction" label="扣除积分">{{ detail.pointDeduction }}</el-descriptions-item>
          <el-descriptions-item label="生效时间">{{ parseTime(detail.effectiveStartTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="失效时间">{{ parseTime(detail.effectiveEndTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item label="公告状态">{{ formatAnnouncementStatus(detail) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions v-if="detail.eventImpact || detail.penaltyCode === 'EVENT_DISQUALIFICATION'" :column="1" border class="mt16">
          <template #title>赛事执行结果</template>
          <el-descriptions-item label="EventTeam Status">{{ eventTeamStatusText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="Match Auto Forfeit">{{ detail.eventImpact?.futureMatchForfeitCount ?? detail.executionMatchForfeitCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="Series Auto Forfeit">{{ detail.eventImpact?.seriesForfeitCount ?? detail.executionSeriesForfeitCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="Qualification Removed">{{ detail.eventImpact?.qualificationRemovedCount ?? detail.executionQualificationRemovedCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="Warning">{{ detail.eventImpact?.warningCount ?? detail.executionWarningCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="Executed Time">{{ parseTime(detail.eventImpact?.executedTime || detail.executedTime) || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.eventImpact?.warnings?.length" label="管理员提示">
            <div v-for="(warn, idx) in detail.eventImpact.warnings" :key="idx">{{ warn }}</div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.eventImpact?.errorMessage || detail.executionError" label="失败原因">{{ detail.eventImpact?.errorMessage || detail.executionError }}</el-descriptions-item>
        </el-descriptions>

        <el-table
          v-if="(detail.executionDetails || detail.eventImpact?.details || []).length"
          :data="detail.executionDetails || detail.eventImpact?.details || []"
          border
          class="mt16"
          size="small"
        >
          <el-table-column label="动作" prop="actionType" min-width="200" />
          <el-table-column label="对象类型" prop="targetType" width="120" />
          <el-table-column label="对象ID" prop="targetId" width="100" />
          <el-table-column label="结果" prop="executionStatus" width="90" />
          <el-table-column label="说明" prop="resultMessage" min-width="220" show-overflow-tooltip />
        </el-table>

        <div class="mt16 drawer-actions">
          <el-button v-if="detail.recordStatus === 'DRAFT'" type="success" @click="doActivate(detail.id)" v-hasPermi="['basket:discipline:activate']">生效</el-button>
          <el-button v-if="detail.recordStatus === 'ACTIVE'" type="danger" @click="doRevoke(detail.id)" v-hasPermi="['basket:discipline:revoke']">撤销处罚</el-button>
          <el-button v-if="canRetryExecution(detail)" type="warning" @click="doRetryExecution(detail.id)" v-hasPermi="['basket:discipline:activate']">重试执行</el-button>
          <el-button type="primary" plain @click="doGenerateAnnouncement" v-hasPermi="['basket:announcement:add']">生成处罚公告</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="BasketDisciplinePenalty">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  activateDiscipline,
  createDiscipline,
  generateDisciplineAnnouncementDraft,
  getDiscipline,
  retryDisciplineExecution,
  listDiscipline,
  revokeDiscipline,
  searchDisciplinePlayers,
  searchDisciplineSeasons,
  searchDisciplineTeams,
  type DisciplineSelectorOption
} from '@/api/basket/discipline'
import {
  DISCIPLINE_PENALTY_OPTIONS,
  DISCIPLINE_RECORD_STATUS_OPTIONS,
  DISCIPLINE_TARGET_TYPE_OPTIONS,
  DISCIPLINE_TYPE_OPTIONS,
  formatDisciplinePenaltyCode,
  formatDisciplineSeasonLabel,
  formatDisciplineDivisionLabel,
  formatDisciplineBusinessStatus,
  formatDisciplineTargetName,
  formatDisciplineTargetType,
  formatDisciplineType,
  formatDisciplineExecutionStatus,
  formatAnnouncementStatus,
  formatEventImpactSummary,
  formatReasonSummary,
  executionStatusTagType,
  businessStatusTagType,
  formatSeasonStatus,
  showsDateRange,
  showsMatchSuspension,
  showsPointDeduction
} from '@/utils/disciplineDisplay'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const submitting = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const activeTab = ref('ALL')
const query = reactive<{ pageNum: number; pageSize: number; seasonId?: number; recordStatus?: string; keyword?: string; targetType?: string }>({
  pageNum: 1,
  pageSize: 10
})
const createOpen = ref(false)
const detailOpen = ref(false)
const detail = ref<any>(null)

const querySeasonLoading = ref(false)
const formSeasonLoading = ref(false)
const formPlayerLoading = ref(false)
const formTeamLoading = ref(false)
const querySeasonOptions = ref<DisciplineSelectorOption[]>([])
const formSeasonOptions = ref<DisciplineSelectorOption[]>([])
const formPlayerOptions = ref<DisciplineSelectorOption[]>([])
const formTeamOptions = ref<DisciplineSelectorOption[]>([])
const playerSearched = ref(false)
const teamSearched = ref(false)
const selectedTeamName = ref('')
const teamSelectId = ref<number | string>()
const playerSelectId = ref<number | string>()

const form = reactive<any>({
  targetType: 'PLAYER',
  seasonId: undefined,
  playerId: undefined,
  memberId: undefined,
  teamId: undefined,
  eventTeamId: undefined,
  disciplineType: 'DISCIPLINE',
  penaltyCode: 'WARNING',
  title: '',
  reasonText: '',
  matchSuspensionCount: 0,
  pointDeduction: 0,
  effectiveStartTime: undefined,
  effectiveEndTime: undefined
})

const TEAM_TARGET_TYPES = ['PERMANENT_TEAM', 'EVENT_TEAM']
const isTeamTarget = computed(() => TEAM_TARGET_TYPES.includes(form.targetType))

const displayRows = computed(() => {
  if (activeTab.value !== 'TEAM') return rows.value
  return rows.value.filter(item => TEAM_TARGET_TYPES.includes(item.targetType))
})

const displayTotal = computed(() => {
  if (activeTab.value !== 'TEAM') return total.value
  return displayRows.value.length
})

let querySeasonTimer: ReturnType<typeof setTimeout> | undefined
let formSeasonTimer: ReturnType<typeof setTimeout> | undefined
let formPlayerTimer: ReturnType<typeof setTimeout> | undefined
let formTeamTimer: ReturnType<typeof setTimeout> | undefined

function eventTeamStatusText(row: any) {
  const status = row?.eventImpact?.eventTeamStatus
  if (status === 'DISQUALIFIED') return '已取消'
  if (row?.penaltyCode === 'EVENT_DISQUALIFICATION' && row?.recordStatus === 'ACTIVE') return '已取消'
  if (row?.recordStatus === 'REVOKED') return '已恢复参赛资格'
  return '-'
}

function debounceRun(timerHolder: 'querySeason' | 'formSeason' | 'formPlayer' | 'formTeam', fn: () => void, wait = 300) {
  const map = {
    querySeason: () => { clearTimeout(querySeasonTimer); querySeasonTimer = setTimeout(fn, wait) },
    formSeason: () => { clearTimeout(formSeasonTimer); formSeasonTimer = setTimeout(fn, wait) },
    formPlayer: () => { clearTimeout(formPlayerTimer); formPlayerTimer = setTimeout(fn, wait) },
    formTeam: () => { clearTimeout(formTeamTimer); formTeamTimer = setTimeout(fn, wait) }
  }
  map[timerHolder]()
}

async function fetchSeasons(keyword: string, target: 'query' | 'form') {
  const loadingRef = target === 'query' ? querySeasonLoading : formSeasonLoading
  const optionsRef = target === 'query' ? querySeasonOptions : formSeasonOptions
  loadingRef.value = true
  try {
    const res = await searchDisciplineSeasons({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
    optionsRef.value = res.rows || []
  } finally {
    loadingRef.value = false
  }
}

function searchQuerySeasons(keyword: string) {
  debounceRun('querySeason', () => fetchSeasons(keyword, 'query'))
}

function searchFormSeasons(keyword: string) {
  debounceRun('formSeason', () => fetchSeasons(keyword, 'form'))
}

async function fetchPlayers(keyword = '') {
  if (!form.seasonId) {
    formPlayerOptions.value = []
    return
  }
  formPlayerLoading.value = true
  try {
    const res = await searchDisciplinePlayers({
      seasonId: form.seasonId,
      keyword: keyword || undefined,
      pageNum: 1,
      pageSize: 20
    })
    formPlayerOptions.value = res.rows || []
    playerSearched.value = true
  } finally {
    formPlayerLoading.value = false
  }
}

function searchFormPlayers(keyword: string) {
  debounceRun('formPlayer', () => fetchPlayers(keyword))
}

function onPlayerVisible(visible: boolean) {
  if (visible && form.seasonId && !formPlayerOptions.value.length) {
    fetchPlayers('')
  }
}

async function fetchTeams(keyword = '') {
  if (!form.seasonId || !isTeamTarget.value) {
    formTeamOptions.value = []
    return
  }
  formTeamLoading.value = true
  try {
    const res = await searchDisciplineTeams({
      seasonId: form.seasonId,
      targetType: form.targetType,
      keyword: keyword || undefined,
      pageNum: 1,
      pageSize: 20
    })
    formTeamOptions.value = res.rows || []
    teamSearched.value = true
  } finally {
    formTeamLoading.value = false
  }
}

function searchFormTeams(keyword: string) {
  debounceRun('formTeam', () => fetchTeams(keyword))
}

function onTeamVisible(visible: boolean) {
  if (visible && form.seasonId && isTeamTarget.value && !formTeamOptions.value.length) {
    fetchTeams('')
  }
}

function clearTargetSelection() {
  form.playerId = undefined
  form.memberId = undefined
  form.teamId = undefined
  form.eventTeamId = undefined
  teamSelectId.value = undefined
  playerSelectId.value = undefined
  selectedTeamName.value = ''
  formPlayerOptions.value = []
  formTeamOptions.value = []
  playerSearched.value = false
  teamSearched.value = false
}

function onTargetTypeChange() {
  clearTargetSelection()
}

function onSeasonChange() {
  clearTargetSelection()
}

function onPlayerChange(selectKey?: number | string) {
  const selected = formPlayerOptions.value.find(item => String(item.eventPlayerId || item.id) === String(selectKey))
  form.playerId = selected?.id
  form.memberId = selected?.memberId
  form.teamId = selected?.teamId
  form.eventTeamId = selected?.eventTeamId
  selectedTeamName.value = selected?.teamName || ''
}

function onTeamChange(id?: number) {
  const selected = formTeamOptions.value.find(item => item.id === id)
  if (form.targetType === 'EVENT_TEAM') {
    form.eventTeamId = id
    form.teamId = selected?.teamId
  } else {
    form.teamId = id
    form.eventTeamId = undefined
  }
  form.playerId = undefined
  form.memberId = undefined
  selectedTeamName.value = selected?.teamName || selected?.label || ''
}

function onPenaltyCodeChange() {
  if (showsMatchSuspension(form.penaltyCode)) {
    form.matchSuspensionCount = form.matchSuspensionCount > 0 ? form.matchSuspensionCount : 1
    form.pointDeduction = 0
    form.effectiveStartTime = undefined
    form.effectiveEndTime = undefined
  } else if (showsPointDeduction(form.penaltyCode)) {
    form.pointDeduction = form.pointDeduction > 0 ? form.pointDeduction : 1
    form.matchSuspensionCount = 0
    form.effectiveStartTime = undefined
    form.effectiveEndTime = undefined
  } else if (showsDateRange(form.penaltyCode)) {
    form.matchSuspensionCount = 0
    form.pointDeduction = 0
  } else {
    form.matchSuspensionCount = 0
    form.pointDeduction = 0
    form.effectiveStartTime = undefined
    form.effectiveEndTime = undefined
  }
}

function handleTabChange() {
  query.pageNum = 1
  if (activeTab.value === 'PLAYER') {
    query.targetType = 'PLAYER'
  } else {
    query.targetType = undefined
  }
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const res = await listDiscipline(query)
    rows.value = res.rows || []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    targetType: 'PLAYER',
    seasonId: query.seasonId,
    playerId: undefined,
    memberId: undefined,
    teamId: undefined,
    eventTeamId: undefined,
    disciplineType: 'DISCIPLINE',
    penaltyCode: 'WARNING',
    title: '',
    reasonText: '',
    matchSuspensionCount: 0,
    pointDeduction: 0,
    effectiveStartTime: undefined,
    effectiveEndTime: undefined
  })
  teamSelectId.value = undefined
  playerSelectId.value = undefined
  selectedTeamName.value = ''
  formPlayerOptions.value = []
  formTeamOptions.value = []
  playerSearched.value = false
  teamSearched.value = false
  formSeasonOptions.value = querySeasonOptions.value.slice()
  if (query.seasonId && !formSeasonOptions.value.some(item => item.id === query.seasonId)) {
    fetchSeasons('', 'form')
  } else if (!formSeasonOptions.value.length) {
    fetchSeasons('', 'form')
  }
  createOpen.value = true
}

async function submitCreate() {
  if (!form.targetType) {
    ElMessage.warning('请选择处罚对象类型')
    return
  }
  if (!form.seasonId) {
    ElMessage.warning('请选择所属赛季')
    return
  }
  if (form.targetType === 'PLAYER' && !form.playerId) {
    ElMessage.warning('请选择球员')
    return
  }
  if (isTeamTarget.value && !teamSelectId.value) {
    ElMessage.warning('请选择球队')
    return
  }
  if (!form.disciplineType || !form.penaltyCode) {
    ElMessage.warning('请选择处罚类型与措施')
    return
  }
  if (!form.title || !form.reasonText) {
    ElMessage.warning('请填写标题与处罚原因')
    return
  }
  if (showsMatchSuspension(form.penaltyCode) && (!form.matchSuspensionCount || form.matchSuspensionCount < 1)) {
    ElMessage.warning('禁赛场次至少为 1')
    return
  }
  if (showsPointDeduction(form.penaltyCode) && (!form.pointDeduction || form.pointDeduction < 1)) {
    ElMessage.warning('请填写扣除积分')
    return
  }
  if (showsPointDeduction(form.penaltyCode) && !form.teamId) {
    ElMessage.warning('扣分处罚需要关联球队，请选择带有球队上下文的对象')
    return
  }
  if (showsDateRange(form.penaltyCode)) {
    if (!form.effectiveStartTime || !form.effectiveEndTime) {
      ElMessage.warning('按日期禁赛必须填写生效时间与结束时间')
      return
    }
    if (new Date(form.effectiveStartTime).getTime() >= new Date(form.effectiveEndTime).getTime()) {
      ElMessage.warning('生效时间必须早于结束时间')
      return
    }
  }

  const payload: any = {
    targetType: form.targetType,
    seasonId: form.seasonId,
    disciplineType: form.disciplineType,
    penaltyCode: form.penaltyCode,
    title: form.title,
    reasonText: form.reasonText,
    matchSuspensionCount: showsMatchSuspension(form.penaltyCode) ? form.matchSuspensionCount : 0,
    pointDeduction: showsPointDeduction(form.penaltyCode) ? form.pointDeduction : 0,
    effectiveStartTime: showsDateRange(form.penaltyCode) ? form.effectiveStartTime : undefined,
    effectiveEndTime: showsDateRange(form.penaltyCode) ? form.effectiveEndTime : undefined,
    idempotencyKey: `UI-${Date.now()}`
  }

  if (form.targetType === 'PLAYER') {
    payload.playerId = form.playerId
    payload.memberId = form.memberId
    payload.teamId = form.teamId
    payload.eventTeamId = form.eventTeamId
  } else if (form.targetType === 'PERMANENT_TEAM') {
    payload.teamId = form.teamId
  } else if (form.targetType === 'EVENT_TEAM') {
    payload.eventTeamId = form.eventTeamId
    payload.teamId = form.teamId
  }

  submitting.value = true
  try {
    await createDiscipline(payload)
    ElMessage.success('处罚已创建')
    createOpen.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function showDetail(id: number) {
  const res = await getDiscipline(id)
  detail.value = res.data
  detailOpen.value = true
}

async function doActivate(id: number) {
  await ElMessageBox.confirm('确认使该处罚生效？取消赛事资格将自动处理未结束比赛与系列赛，不会改写已正式结束的历史结果。', '提示')
  await activateDiscipline(id)
  ElMessage.success('处罚已生效')
  loadList()
}

function canRetryExecution(row?: { penaltyCode?: string; executionStatus?: string; eventImpact?: { executionStatus?: string } }) {
  if (!row || row.penaltyCode !== 'EVENT_DISQUALIFICATION') return false
  const status = row.eventImpact?.executionStatus || row.executionStatus
  return status === 'FAILED' || status === 'PARTIAL'
}

async function doRetryExecution(id: number) {
  await ElMessageBox.confirm('确认重试该处罚的赛事自动处理？已成功判负的比赛不会重复处理。', '提示')
  await retryDisciplineExecution(id)
  ElMessage.success('已重试赛事自动处理')
  await showDetail(id)
  loadList()
}

async function doRevoke(id: number) {
  const { value } = await ElMessageBox.prompt('请输入解除原因', '撤销处罚', { inputType: 'textarea' })
  await revokeDiscipline(id, { revokeReason: value })
  ElMessage.success('处罚已撤销')
  if (detail.value?.id === id) {
    await showDetail(id)
  }
  loadList()
}

async function doGenerateAnnouncement() {
  if (!detail.value?.id) return
  const { value } = await ElMessageBox.prompt(
    '可填写对外公开说明。内部处罚原因不会自动带入公告，避免后台备注泄漏。留空则仅生成结构化草稿。',
    '生成处罚公告草稿',
    { inputType: 'textarea', inputPlaceholder: '可选：公开说明', confirmButtonText: '生成草稿', inputValidator: () => true }
  )
  const res = await generateDisciplineAnnouncementDraft(detail.value.id, { publicStatement: value || undefined })
  const data = res.data || {}
  ElMessage.success(data.created === false ? '已关联现有公告草稿' : '已生成公告草稿，不会自动发布')
  await showDetail(detail.value.id)
  loadList()
}

onMounted(async () => {
  await fetchSeasons('', 'query')
  loadList()
})
</script>

<style scoped>
.selector-option {
  line-height: 1.3;
  padding: 4px 0;
}
.selector-option__sub {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.selector-empty {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.drawer-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>