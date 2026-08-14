<template>
  <div class="app-container">
    <el-alert title="参赛队伍快照（只读）：审核通过后自动生成参赛球队与球员数据；赛事分组用于将参赛球队分配到赛制阶段小组" type="info" :closable="false" show-icon class="mb8" />

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="参赛球队" name="team">
        <el-form :model="teamQuery" :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="teamQuery.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onTeamSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="teamQuery.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!teamQuery.seasonId">
              <el-option v-for="item in teamDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadTeams">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="teamLoading" :data="teamList" border>
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="赛季" prop="seasonTitle" min-width="140" />
          <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="正式参赛队（快照）" prop="teamNameSnapshot" min-width="140" />
          <el-table-column label="来源" width="130">
            <template #default="scope">
              <el-tag v-if="scope.row.sourceTeamId" size="small">长期球队 #{{ scope.row.sourceTeamId }}</el-tag>
              <el-tag v-else type="warning" size="small">临时参赛队</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="队长" prop="captainPlayerName" min-width="100" />
          <el-table-column label="编号" prop="eventTeamNo" width="120" />
          <el-table-column label="状态" prop="eventTeamStatus" width="100" align="center">
            <template #default="scope">
              <el-tag size="small" class="nowrap-tag">{{ scope.row.eventTeamStatus === 'ACTIVE' ? '有效' : (scope.row.eventTeamStatus === 'INACTIVE' ? '无效' : scope.row.eventTeamStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="scope">
              <el-button link type="primary" @click="openTeamDetail(scope.row)">详情</el-button>
              <el-button link type="primary" @click="viewPlayers(scope.row)">球员</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="teamTotal" v-model:page="teamQuery.pageNum" v-model:limit="teamQuery.pageSize" @pagination="loadTeams" />
      </el-tab-pane>

      <el-tab-pane label="参赛球员" name="player">
        <el-form :model="playerQuery" :inline="true" class="mb8">
          <el-form-item label="平台赛区">
            <el-select v-model="playerQuery.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px">
              <el-option v-for="item in playerDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadPlayers">搜索</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="playerLoading" :data="playerList" border>
          <el-table-column label="ID" prop="id" width="70" />
          <el-table-column label="球队" prop="teamNameSnapshot" min-width="120" />
          <el-table-column label="球员" prop="playerNameSnapshot" min-width="120" />
          <el-table-column label="Game ID" prop="gameIdSnapshot" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="位置" prop="position" width="80" />
          <el-table-column label="队长" width="70" align="center">
            <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="状态" prop="eventPlayerStatus" width="90" align="center">
            <template #default="scope">
              <el-tag size="small">{{ scope.row.eventPlayerStatus === 'ACTIVE' ? '有效' : scope.row.eventPlayerStatus }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="playerTotal" v-model:page="playerQuery.pageNum" v-model:limit="playerQuery.pageSize" @pagination="loadPlayers" />
      </el-tab-pane>

      <el-tab-pane label="赛事分组" name="grouping">
        <el-form :model="groupingQuery" :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="groupingSeasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onGroupingSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="groupingDivisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!groupingSeasonId" @change="onGroupingDivisionChange">
              <el-option v-for="item in groupingDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="赛制阶段">
            <el-select v-model="groupingQuery.stageId" clearable filterable placeholder="选择阶段" style="width: 200px" :disabled="!groupingDivisionId" @change="onGroupingStageChange">
              <el-option v-for="item in stageOptions" :key="item.id" :label="formatStageLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="groupingQuery.groupingStatus" clearable style="width: 140px">
              <el-option v-for="item in GROUPING_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadGroupingBatches">搜索</el-button>
            <el-button icon="Refresh" @click="resetGroupingQuery">重置</el-button>
            <el-button type="primary" plain icon="Plus" @click="openCreateGroupingBatch" v-hasPermi="['basket:v4Grouping:edit']" :disabled="!groupingDivisionId || !groupingQuery.stageId">新建分组</el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="formatSnapshot" :title="`赛制快照：${formatSnapshot.sourceTemplateName || '-'} v${formatSnapshot.snapshotVersion || '-'}（${formatSnapshot.snapshotStatusName || formatSnapshot.snapshotStatus}）`" type="info" :closable="false" class="mb8" />

        <el-table v-loading="groupingLoading" :data="groupingList" border>
          <el-table-column label="批次ID" prop="groupingBatchId" width="80" />
          <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
          <el-table-column label="阶段" min-width="140">
            <template #default="scope">{{ scope.row.stageName || scope.row.stageCode }}</template>
          </el-table-column>
          <el-table-column label="版本" prop="batchVersion" width="70" align="center" />
          <el-table-column label="分组模式" width="120">
            <template #default="scope">{{ v4GroupingModeLabel(scope.row.groupingMode) }}</template>
          </el-table-column>
          <el-table-column label="小组数" prop="groupCount" width="80" align="center" />
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <el-tag :type="groupingStatusTagType(scope.row.groupingStatus)" size="small" class="nowrap-tag">
                {{ v4GroupingStatusLabel(scope.row.groupingStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="确认时间" width="160">
            <template #default="scope">{{ parseTime(scope.row.confirmedTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openGroupingDetail(scope.row)" v-hasPermi="['basket:v4Grouping:list']">详情</el-button>
              <el-button link type="danger" @click="handleCancelGroupingBatch(scope.row)" v-hasPermi="['basket:v4Grouping:edit']" v-if="scope.row.groupingStatus === 'DRAFT'">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="groupingTotal" v-model:page="groupingQuery.pageNum" v-model:limit="groupingQuery.pageSize" @pagination="loadGroupingBatches" />
      </el-tab-pane>
    </el-tabs>

    <!-- 分组批次详情 -->
    <el-drawer v-model="groupingDetailOpen" title="赛事分组详情" size="960px" append-to-body @closed="groupingDetail = undefined">
      <template v-if="groupingDetail">
        <el-descriptions :column="3" border class="mb8">
          <el-descriptions-item label="批次ID">{{ groupingDetail.groupingBatchId }}</el-descriptions-item>
          <el-descriptions-item label="赛区">{{ groupingDetail.divisionName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="groupingStatusTagType(groupingDetail.groupingStatus)" size="small">
              {{ v4GroupingStatusLabel(groupingDetail.groupingStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="阶段">{{ groupingDetail.stageName || groupingDetail.stageCode }}</el-descriptions-item>
          <el-descriptions-item label="分组模式">{{ v4GroupingModeLabel(groupingDetail.groupingMode) }}</el-descriptions-item>
          <el-descriptions-item label="小组数">{{ groupingDetail.groupCount }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-toolbar mb8" v-if="groupingDetail.groupingStatus === 'DRAFT'">
          <el-button type="success" @click="handleLockGroupingBatch" v-hasPermi="['basket:v4Grouping:lock']">锁定分组</el-button>
        </div>

        <el-row :gutter="16">
          <el-col :span="16">
            <el-divider content-position="left">小组分配</el-divider>
            <div v-for="group in groupingDetail.groups || []" :key="group.groupId" class="group-card mb8">
              <div class="group-header">
                <span class="group-title">{{ group.groupName || group.groupCode }}（晋级 {{ group.qualifyCount ?? '-' }} 队）</span>
                <el-button link type="primary" @click="openAssignTeam(group)" v-hasPermi="['basket:v4Grouping:edit']" v-if="groupingDetail.groupingStatus === 'DRAFT'">分配球队</el-button>
              </div>
              <el-table :data="group.teams || []" size="small" border>
                <el-table-column label="参赛球队" prop="teamNameSnapshot" min-width="140" />
                <el-table-column label="种子号" prop="seedNo" width="80" align="center" />
                <el-table-column label="抽签序" prop="drawOrder" width="80" align="center" />
                <el-table-column label="操作" width="80" v-if="groupingDetail.groupingStatus === 'DRAFT'">
                  <template #default="scope">
                    <el-button link type="danger" @click="handleRemoveGroupTeam(scope.row)" v-hasPermi="['basket:v4Grouping:edit']">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-col>
          <el-col :span="8">
            <el-divider content-position="left">未分配球队</el-divider>
            <el-table :data="unassignedTeams" size="small" border max-height="480">
              <el-table-column label="ID" prop="id" width="60" />
              <el-table-column label="球队" prop="teamNameSnapshot" min-width="120" />
              <el-table-column label="编号" prop="eventTeamNo" width="90" />
            </el-table>
          </el-col>
        </el-row>
      </template>
    </el-drawer>

    <!-- 新建分组批次 -->
    <el-dialog v-model="createGroupingOpen" title="新建赛事分组批次" width="480px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="赛区">
          <span>{{ selectedStageLabel }}</span>
        </el-form-item>
        <el-form-item label="结果来源">
          <el-select v-model="createGroupingForm.resultSource" style="width: 100%">
            <el-option label="人工分组" value="MANUAL" />
            <el-option label="线下抽签" value="OFFLINE_RANDOM" />
            <el-option label="种子分组" value="SEEDED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createGroupingOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCreateGroupingBatch">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配球队 -->
    <el-dialog v-model="assignTeamOpen" title="分配参赛球队" width="520px" append-to-body>
      <el-form :model="assignTeamForm" label-width="100px">
        <el-form-item label="参赛球队" required>
          <el-select v-model="assignTeamForm.eventTeamId" filterable placeholder="选择未分配球队" style="width: 100%">
            <el-option v-for="item in unassignedTeams" :key="item.id" :label="`${item.teamNameSnapshot} (#${item.id})`" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="种子号">
          <el-input-number v-model="assignTeamForm.seedNo" :min="1" />
        </el-form-item>
        <el-form-item label="抽签序">
          <el-input-number v-model="assignTeamForm.drawOrder" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignTeamOpen = false">取消</el-button>
        <el-button type="primary" @click="submitAssignTeam">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="teamDetailOpen" title="参赛队伍详情" size="720px" append-to-body>
      <template v-if="teamDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="参赛队 ID">{{ teamDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="编号">{{ teamDetail.eventTeamNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="队名">{{ teamDetail.teamNameSnapshot }}</el-descriptions-item>
          <el-descriptions-item label="队长">{{ teamDetail.captainPlayerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="赛季">{{ teamDetail.seasonTitle }}</el-descriptions-item>
          <el-descriptions-item label="赛区">{{ teamDetail.divisionName }}（{{ teamDetail.platformCode }}）</el-descriptions-item>
          <el-descriptions-item label="来源">
            <el-tag v-if="teamDetail.sourceTeamId" size="small">长期球队 #{{ teamDetail.sourceTeamId }}</el-tag>
            <el-tag v-else type="warning" size="small">临时参赛队（无 basket_team）</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="报名 ID">{{ teamDetail.registrationId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ teamDetail.eventTeamStatus === 'ACTIVE' ? '有效' : teamDetail.eventTeamStatus }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ parseTime(teamDetail.createTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">本届阵容</el-divider>
        <el-table v-loading="teamDetailPlayerLoading" :data="teamDetailPlayers" size="small" border>
          <el-table-column label="球员" prop="playerNameSnapshot" />
          <el-table-column label="Game ID" prop="gameIdSnapshot" />
          <el-table-column label="位置" prop="position" width="80" />
          <el-table-column label="队长" width="70">
            <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="scope">{{ scope.row.eventPlayerStatus === 'ACTIVE' ? '有效' : scope.row.eventPlayerStatus }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4EventTeam">
import { computed, onMounted, reactive, ref } from 'vue'
import { getV4EventTeam, listV4EventPlayer, listV4EventTeam, type BasketEventPlayerListVo, type BasketEventPlayerQuery, type BasketEventTeamListVo, type BasketEventTeamQuery } from '@/api/cnpa/event-v4/event-team'
import {
  assignV4DivisionGroupTeam,
  cancelV4DivisionGroupingBatch,
  createV4DivisionGroupingBatch,
  getV4DivisionGroupingBatchDetail,
  listV4DivisionGroupingBatch,
  lockV4DivisionGroupingBatch,
  removeV4DivisionGroupAssignment,
  type BasketDivisionGroupVo,
  type BasketDivisionGroupingBatchDetailVo,
  type BasketDivisionGroupingBatchListVo,
  type BasketDivisionGroupingBatchQuery
} from '@/api/cnpa/event-v4/division-grouping'
import { getFormatSnapshot, type BasketDivisionFormatSnapshotVo, type BasketDivisionStageVo } from '@/api/cnpa/event-v4/format-template'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  V4_GROUPING_STATUS_FILTER_OPTIONS,
  v4GroupingModeLabel,
  v4GroupingStatusLabel
} from '@/utils/v4AdminDisplay'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const activeTab = ref('team')
const teamLoading = ref(false)
const playerLoading = ref(false)
const teamList = ref<any[]>([])
const playerList = ref<any[]>([])
const teamTotal = ref(0)
const playerTotal = ref(0)
const teamQuery = reactive<BasketEventTeamQuery>({ pageNum: 1, pageSize: 10 })
const playerQuery = reactive<BasketEventPlayerQuery>({ pageNum: 1, pageSize: 10 })
const seasonOptions = ref<V4AdminOptionVo[]>([])
const teamDivisionOptions = ref<V4AdminOptionVo[]>([])
const playerDivisionOptions = ref<V4AdminOptionVo[]>([])

// ---- 赛事分组 ----
const groupingLoading = ref(false)
const groupingTotal = ref(0)
const groupingList = ref<BasketDivisionGroupingBatchListVo[]>([])
const groupingDetailOpen = ref(false)
const groupingDetail = ref<BasketDivisionGroupingBatchDetailVo>()
const groupingSeasonId = ref<number>()
const groupingDivisionId = ref<number>()
const groupingDivisionOptions = ref<V4AdminOptionVo[]>([])
const groupingQuery = reactive<BasketDivisionGroupingBatchQuery>({ pageNum: 1, pageSize: 10 })
const GROUPING_STATUS_OPTIONS = V4_GROUPING_STATUS_FILTER_OPTIONS
const formatSnapshot = ref<BasketDivisionFormatSnapshotVo>()
const stageOptions = ref<BasketDivisionStageVo[]>([])
const divisionEventTeams = ref<BasketEventTeamListVo[]>([])
const createGroupingOpen = ref(false)
const assignTeamOpen = ref(false)
const currentGroup = ref<BasketDivisionGroupVo>()
const createGroupingForm = reactive({ resultSource: 'MANUAL' })
const assignTeamForm = reactive({ eventTeamId: undefined as number | undefined, seedNo: undefined as number | undefined, drawOrder: undefined as number | undefined })

const selectedStageLabel = computed(() => {
  const stage = stageOptions.value.find(s => s.id === groupingQuery.stageId)
  return stage ? formatStageLabel(stage) : '-'
})

const assignedEventTeamIds = computed(() => {
  const ids = new Set<number>()
  for (const group of groupingDetail.value?.groups || []) {
    for (const team of group.teams || []) {
      if (team.eventTeamId) ids.add(team.eventTeamId)
    }
  }
  return ids
})

const unassignedTeams = computed(() =>
  divisionEventTeams.value.filter(t => t.id && !assignedEventTeamIds.value.has(t.id))
)

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

function formatStageLabel(stage: BasketDivisionStageVo) {
  const type = stage.stageTypeName || stage.stageType || ''
  return stage.stageName ? `${stage.stageName}${type ? `（${type}）` : ''}` : stage.stageCode || String(stage.id)
}

function groupingStatusTagType(status?: string) {
  switch ((status || '').toUpperCase()) {
    case 'LOCKED': return 'success'
    case 'DRAFT': return 'warning'
    case 'CANCELLED': return 'info'
    default: return 'info'
  }
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
  if (seasonOptions.value.length) {
    const allDivisions = await Promise.all(seasonOptions.value.map(s => listV4AdminDivisionOptions(s.id)))
    playerDivisionOptions.value = allDivisions.flatMap(r => r.data || [])
  }
}

async function loadTeamDivisions(seasonId?: number) {
  teamDivisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  teamDivisionOptions.value = res.data || []
}

function onTeamSeasonChange(seasonId?: number) {
  teamQuery.divisionId = undefined
  loadTeamDivisions(seasonId)
}

function loadTeams() {
  teamLoading.value = true
  listV4EventTeam(teamQuery).then(res => {
    teamList.value = res.rows || []
    teamTotal.value = res.total || 0
  }).finally(() => { teamLoading.value = false })
}

function loadPlayers() {
  playerLoading.value = true
  listV4EventPlayer(playerQuery).then(res => {
    playerList.value = res.rows || []
    playerTotal.value = res.total || 0
  }).finally(() => { playerLoading.value = false })
}

function viewPlayers(row: any) {
  activeTab.value = 'player'
  playerQuery.eventTeamId = row.id
  playerQuery.divisionId = row.divisionId
  playerQuery.pageNum = 1
  loadPlayers()
}

const teamDetailOpen = ref(false)
const teamDetail = ref<BasketEventTeamListVo>()
const teamDetailPlayers = ref<BasketEventPlayerListVo[]>([])
const teamDetailPlayerLoading = ref(false)

function openTeamDetail(row: BasketEventTeamListVo) {
  if (!row.id) return
  teamDetailOpen.value = true
  teamDetail.value = row
  teamDetailPlayers.value = []
  teamDetailPlayerLoading.value = true
  getV4EventTeam(row.id).then(res => {
    if (res.data) teamDetail.value = res.data
  }).finally(() => {
    listV4EventPlayer({ eventTeamId: row.id, pageNum: 1, pageSize: 100 }).then(res => {
      teamDetailPlayers.value = res.rows || []
    }).finally(() => { teamDetailPlayerLoading.value = false })
  })
}

async function onGroupingSeasonChange(seasonId?: number) {
  groupingDivisionId.value = undefined
  groupingQuery.divisionId = undefined
  groupingQuery.stageId = undefined
  groupingDivisionOptions.value = []
  stageOptions.value = []
  formatSnapshot.value = undefined
  if (seasonId) {
    const res = await listV4AdminDivisionOptions(seasonId)
    groupingDivisionOptions.value = res.data || []
  }
}

async function onGroupingDivisionChange(divisionId?: number) {
  groupingQuery.divisionId = divisionId
  groupingQuery.stageId = undefined
  stageOptions.value = []
  formatSnapshot.value = undefined
  divisionEventTeams.value = []
  if (!divisionId) return
  const [snapshotRes, teamsRes] = await Promise.all([
    getFormatSnapshot(divisionId),
    listV4EventTeam({ divisionId, pageNum: 1, pageSize: 500 })
  ])
  formatSnapshot.value = snapshotRes.data
  divisionEventTeams.value = teamsRes.rows || []
  stageOptions.value = (formatSnapshot.value?.stages || []).filter(
    s => s.groupingMode && s.groupingMode !== 'NONE' && (s.groupCount ?? 0) > 0
  )
}

function onGroupingStageChange() {
  loadGroupingBatches()
}

function loadGroupingBatches() {
  groupingLoading.value = true
  listV4DivisionGroupingBatch({
    ...groupingQuery,
    divisionId: groupingDivisionId.value
  }).then(res => {
    groupingList.value = res.rows || []
    groupingTotal.value = res.total || 0
  }).finally(() => { groupingLoading.value = false })
}

function resetGroupingQuery() {
  groupingSeasonId.value = undefined
  groupingDivisionId.value = undefined
  Object.assign(groupingQuery, { pageNum: 1, pageSize: 10, divisionId: undefined, stageId: undefined, groupingStatus: undefined })
  groupingDivisionOptions.value = []
  stageOptions.value = []
  formatSnapshot.value = undefined
  divisionEventTeams.value = []
  loadGroupingBatches()
}

function openCreateGroupingBatch() {
  createGroupingForm.resultSource = 'MANUAL'
  createGroupingOpen.value = true
}

function submitCreateGroupingBatch() {
  createV4DivisionGroupingBatch({
    divisionId: groupingDivisionId.value,
    stageId: groupingQuery.stageId,
    resultSource: createGroupingForm.resultSource
  }).then(res => {
    proxy.$modal.msgSuccess('分组批次已创建')
    createGroupingOpen.value = false
    loadGroupingBatches()
    if (res.data) openGroupingDetail({ groupingBatchId: res.data as number })
  })
}

function openGroupingDetail(row: BasketDivisionGroupingBatchListVo | { groupingBatchId: number }) {
  const id = 'groupingBatchId' in row ? row.groupingBatchId : (row as any).groupingBatchId
  getV4DivisionGroupingBatchDetail(id!).then(res => {
    groupingDetail.value = res.data
    groupingDetailOpen.value = true
  })
}

async function refreshGroupingDetail() {
  if (groupingDetail.value?.groupingBatchId) {
    const res = await getV4DivisionGroupingBatchDetail(groupingDetail.value.groupingBatchId)
    groupingDetail.value = res.data
  }
  loadGroupingBatches()
}

function handleCancelGroupingBatch(row: BasketDivisionGroupingBatchListVo) {
  proxy.$modal.confirm(`确认取消分组批次 #${row.groupingBatchId}？`).then(() =>
    cancelV4DivisionGroupingBatch(row.groupingBatchId!)
  ).then(() => {
    proxy.$modal.msgSuccess('已取消')
    loadGroupingBatches()
  }).catch(() => {})
}

function handleLockGroupingBatch() {
  proxy.$modal.confirm('锁定后将不可再修改分组，确认锁定？').then(() =>
    lockV4DivisionGroupingBatch(groupingDetail.value!.groupingBatchId!)
  ).then(() => {
    proxy.$modal.msgSuccess('分组已锁定')
    refreshGroupingDetail()
  }).catch(() => {})
}

function openAssignTeam(group: BasketDivisionGroupVo) {
  currentGroup.value = group
  Object.assign(assignTeamForm, { eventTeamId: undefined, seedNo: undefined, drawOrder: undefined })
  assignTeamOpen.value = true
}

function submitAssignTeam() {
  if (!assignTeamForm.eventTeamId) {
    proxy.$modal.msgError('请选择参赛球队')
    return
  }
  assignV4DivisionGroupTeam(groupingDetail.value!.groupingBatchId!, currentGroup.value!.groupId!, {
    eventTeamId: assignTeamForm.eventTeamId,
    seedNo: assignTeamForm.seedNo,
    drawOrder: assignTeamForm.drawOrder,
    assignmentSource: 'MANUAL'
  }).then(() => {
    proxy.$modal.msgSuccess('球队已分配')
    assignTeamOpen.value = false
    refreshGroupingDetail()
  })
}

function handleRemoveGroupTeam(row: any) {
  proxy.$modal.confirm('确认移除该球队分配？').then(() =>
    removeV4DivisionGroupAssignment(groupingDetail.value!.groupingBatchId!, row.assignmentId)
  ).then(() => {
    proxy.$modal.msgSuccess('已移除')
    refreshGroupingDetail()
  }).catch(() => {})
}

function onTabChange(name: string | number) {
  if (name === 'grouping' && !groupingList.value.length) loadGroupingBatches()
}

onMounted(async () => {
  await loadSeasonOptions()
  loadTeams()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
.drawer-toolbar { display: flex; gap: 8px; }
.group-card { border: 1px solid var(--el-border-color-lighter); border-radius: 4px; padding: 8px 12px; }
.group-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.group-title { font-weight: 600; font-size: 14px; }
</style>
