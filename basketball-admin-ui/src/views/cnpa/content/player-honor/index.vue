<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="所属赛事">
        <el-select v-model="selectedTournamentId" placeholder="请选择赛事" filterable clearable style="width: 220px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季" prop="seasonId">
        <el-select v-model="queryParams.seasonId" placeholder="请选择赛季" filterable clearable style="width: 200px">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!singleLock" label="荣誉编码" prop="honorCode">
        <el-select v-model="queryParams.honorCode" placeholder="荣誉编码" clearable style="width: 160px">
          <el-option v-for="item in codeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="displayStatus">
        <el-select v-model="queryParams.displayStatus" placeholder="展示状态" clearable style="width: 140px">
          <el-option v-for="item in HONOR_DISPLAY_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="标题/球员" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:playerHonor:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button v-if="showCandidate" type="warning" plain icon="Search" @click="openCandidate" v-hasPermi="['basket:playerHonor:query']">候选查询</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="赛季" prop="seasonName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="荣誉" prop="honorTitle" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="编码" prop="honorCode" width="150" />
      <el-table-column label="球员" prop="playerName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="球队" prop="teamName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="displayStatus" width="100">
        <template #default="scope">
          <dict-tag :options="HONOR_DISPLAY_STATUS_OPTIONS" :value="scope.row.displayStatus" />
        </template>
      </el-table-column>
      <el-table-column label="统计值" prop="honorValue" width="90" />
      <el-table-column label="授予日期" prop="awardDate" width="120">
        <template #default="scope">{{ parseTime(scope.row.awardDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:playerHonor:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:playerHonor:edit']" v-if="[0, 2].includes(scope.row.displayStatus!)">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:playerHonor:publish']" v-if="[0, 2].includes(scope.row.displayStatus!)">发布</el-button>
          <el-button link type="danger" icon="RefreshLeft" @click="handleRevoke(scope.row)" v-hasPermi="['basket:playerHonor:revoke']" v-if="[0, 1].includes(scope.row.displayStatus!)">撤销</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:playerHonor:remove']" v-if="scope.row.displayStatus !== 1">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球员荣誉详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="荣誉标题">{{ detail.honorTitle }}</el-descriptions-item>
        <el-descriptions-item label="编码">{{ detail.honorCode }}</el-descriptions-item>
        <el-descriptions-item label="球员">{{ detail.playerName || detail.playerId }}</el-descriptions-item>
        <el-descriptions-item label="球队">{{ detail.teamName || detail.teamId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.displayStatusName }}</el-descriptions-item>
        <el-descriptions-item label="授予日期">{{ parseTime(detail.awardDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="副标题">{{ detail.honorSubtitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属赛事" prop="tournamentId">
          <el-select v-model="form.tournamentId" filterable style="width: 100%" @change="loadFormSeasons">
            <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛季" prop="seasonId">
          <el-select v-model="form.seasonId" filterable style="width: 100%" @change="onFormSeasonChange">
            <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="荣誉编码" prop="honorCode">
          <el-select v-model="form.honorCode" style="width: 100%">
            <el-option v-for="item in codeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="球员" prop="playerId">
          <el-select v-model="form.playerId" filterable style="width: 100%" @change="syncPlayerTeam">
            <el-option
              v-for="item in seasonPlayers"
              :key="item.sourcePlayerId"
              :label="playerOptionLabel(item)"
              :value="item.sourcePlayerId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="球队">
          <el-select v-model="form.teamId" filterable clearable style="width: 100%">
            <el-option
              v-for="item in seasonTeams"
              :key="item.teamId"
              :label="item.teamName"
              :value="item.teamId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="荣誉标题">
          <el-input v-model="form.honorTitle" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.honorSubtitle" :placeholder="form.honorCode === 'WEEKLY_BEST' ? '如：第 5 比赛周' : ''" />
        </el-form-item>
        <el-form-item label="授予日期" prop="awardDate">
          <el-date-picker v-model="form.awardDate" type="date" value-format="YYYY-MM-DD" placeholder="选择授予日期" style="width: 100%" />
        </el-form-item>
        <el-divider content-position="left">英文内容</el-divider>
        <div class="el-form-item__tip bilingual-tip">英文选填，不改变球员、球队和赛季归属。留空时官网显示中文。</div>
        <el-form-item label="英文标题">
          <el-input v-model="form.honorTitleEn" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="英文副标题">
          <el-input v-model="form.honorSubtitleEn" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="荣誉值">
          <el-input v-model="form.honorValue" placeholder="如场均得分" />
        </el-form-item>
        <el-form-item label="封面文件ID">
          <el-input-number v-model="form.coverFileId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封面预览">
          <image-upload v-model="coverPreview" :limit="1" :file-size="5" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.displayOrder" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="手动球员荣誉必填备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="球员荣誉候选" v-model="candidateOpen" width="860px" append-to-body>
      <el-form :inline="true" class="mb8">
        <el-form-item label="赛季">
          <el-select v-model="candidateQuery.seasonId" filterable style="width: 200px">
            <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="奖项">
          <el-select v-model="candidateQuery.awardType" style="width: 160px">
            <el-option v-for="item in PLAYER_HONOR_AWARD_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadCandidates" v-hasPermi="['basket:playerHonor:query']">查询候选</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="candidateLoading" :data="candidates" border>
        <el-table-column label="排名" prop="rankNo" width="70" />
        <el-table-column label="球员" prop="displayName" min-width="120" />
        <el-table-column label="球队" prop="teamName" min-width="140" />
        <el-table-column label="统计值" prop="statValue" width="100" />
        <el-table-column label="场次" prop="games" width="70" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button link type="primary" @click="applyCandidate(scope.row)" v-hasPermi="['basket:playerHonor:add']">选用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog title="撤销荣誉" v-model="revokeOpen" width="480px" append-to-body>
      <el-form ref="revokeRef" :model="revokeForm" :rules="revokeRules" label-width="100px">
        <el-form-item label="撤销原因" prop="revokeReason">
          <el-input v-model="revokeForm.revokeReason" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeOpen = false">取消</el-button>
        <el-button type="danger" @click="submitRevoke">确认撤销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketPlayerHonor">
import {
  addPlayerHonor,
  delPlayerHonor,
  getPlayerHonor,
  listPlayerHonor,
  listPlayerHonorCandidate,
  publishPlayerHonor,
  revokePlayerHonor,
  updatePlayerHonor
} from '@/api/basket/playerHonor'
import type {
  HonorRecordAddReq,
  HonorRecordDetail,
  HonorRecordListItem,
  HonorRecordQuery,
  HonorRecordRevokeReq,
  PlayerHonorCandidate,
  PlayerHonorCandidateQuery,
  SeasonListItem,
  TournamentListItem
} from '@/types'
import {
  HONOR_DISPLAY_STATUS_OPTIONS,
  PLAYER_HONOR_AWARD_TYPE_OPTIONS,
  PLAYER_HONOR_CODE_OPTIONS
} from '@/types'
import { listV4Series } from '@/api/cnpa/event-v4/series'
import { listV4Season } from '@/api/cnpa/event-v4/season'
import { listV4EventPlayer, listV4EventTeam, type BasketEventPlayerListVo } from '@/api/cnpa/event-v4/event-team'
import type { SeasonCandidateTeam } from '@/types'

const props = withDefaults(defineProps<{ lockedCodes?: string[] }>(), {
  lockedCodes: () => []
})

const { proxy } = getCurrentInstance()

const codeOptions = computed(() => {
  if (!props.lockedCodes.length) return PLAYER_HONOR_CODE_OPTIONS
  return PLAYER_HONOR_CODE_OPTIONS.filter(item => props.lockedCodes.includes(item.value))
})
const singleLock = computed(() => props.lockedCodes.length === 1)
const showCandidate = computed(() =>
  codeOptions.value.some(item => ['SCORING_CHAMPION', 'REBOUND_CHAMPION', 'ASSIST_CHAMPION', 'STEAL_CHAMPION', 'BLOCK_CHAMPION'].includes(item.value))
)

const dataList = ref<HonorRecordListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<HonorRecordDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const revokeOpen = ref(false)
const revokeId = ref<number>()
const coverPreview = ref('')
const candidateOpen = ref(false)
const candidateLoading = ref(false)
const candidates = ref<PlayerHonorCandidate[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const formSeasonOptions = ref<SeasonListItem[]>([])
const seasonPlayers = ref<BasketEventPlayerListVo[]>([])
const seasonTeams = ref<SeasonCandidateTeam[]>([])
const selectedTournamentId = ref<number>()

const queryParams = ref<HonorRecordQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<HonorRecordAddReq>({
  honorCode: 'PLAYER_HONOR',
  displayOrder: 0,
  awardDate: undefined
})

const candidateQuery = ref<PlayerHonorCandidateQuery>({
  seasonId: undefined,
  awardType: 'SCORING'
})

const revokeForm = ref<HonorRecordRevokeReq>({ revokeReason: '' })
const rules = {
  tournamentId: [{ required: true, message: '请选择赛事', trigger: 'change' }],
  seasonId: [{ required: true, message: '请选择赛季', trigger: 'change' }],
  honorCode: [{ required: true, message: '请选择荣誉编码', trigger: 'change' }],
  playerId: [{ required: true, message: '请选择球员', trigger: 'change' }],
  awardDate: [{ required: true, message: '请选择授予日期', trigger: 'change' }]
}

function applyHonorLock() {
  if (!props.lockedCodes.length) return
  if (props.lockedCodes.length === 1) {
    queryParams.value.honorCode = props.lockedCodes[0]
    queryParams.value.honorCodes = undefined
    return
  }
  queryParams.value.honorCodes = props.lockedCodes.join(',')
  if (queryParams.value.honorCode && !props.lockedCodes.includes(queryParams.value.honorCode)) {
    queryParams.value.honorCode = undefined
  }
}

function dateOnly(value?: string) {
  return value ? String(value).slice(0, 10) : undefined
}
const revokeRules = {
  revokeReason: [{ required: true, message: '请填写撤销原因', trigger: 'blur' }]
}

function loadTournaments() {
  listV4Series().then(res => {
    tournamentOptions.value = (res.data || []).map((s: any) => ({ id: s.id, tournamentName: s.seriesName }))
    if (tournamentOptions.value.length > 0) {
      selectedTournamentId.value = tournamentOptions.value[0].id
      queryParams.value.tournamentId = selectedTournamentId.value
      loadSeasons().then(() => getList())
    } else {
      getList()
    }
  })
}

function loadSeasons() {
  seasonOptions.value = []
  queryParams.value.seasonId = undefined
  if (!selectedTournamentId.value) return Promise.resolve()
  return listV4Season({ seriesId: selectedTournamentId.value, pageNum: 1, pageSize: 500 }).then(res => {
    seasonOptions.value = res.rows || []
    queryParams.value.seasonId = seasonOptions.value[0]?.id
    if (typeof candidateQuery !== 'undefined') candidateQuery.value.seasonId = queryParams.value.seasonId
  })
}


function handleTournamentChange() {
  queryParams.value.tournamentId = selectedTournamentId.value
  loadSeasons().then(() => handleQuery())
}

function getList() {
  loading.value = true
  applyHonorLock()
  queryParams.value.tournamentId = selectedTournamentId.value
  listPlayerHonor(queryParams.value).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function playerOptionLabel(item: BasketEventPlayerListVo) {
  const name = item.playerNameSnapshot || item.gameIdSnapshot || item.sourcePlayerId
  return item.teamNameSnapshot ? `${name} · ${item.teamNameSnapshot}` : String(name)
}

function loadSeasonRoster(seasonId?: number) {
  seasonPlayers.value = []
  seasonTeams.value = []
  if (!seasonId) return Promise.resolve()
  const players = listV4EventPlayer({ seasonId, pageNum: 1, pageSize: 500 }).then(res => {
    const seen = new Set<number>()
    seasonPlayers.value = (res.rows || []).filter(item => {
      if (!item.sourcePlayerId || seen.has(item.sourcePlayerId)) return false
      seen.add(item.sourcePlayerId)
      return true
    })
  })
  const teams = listV4EventTeam({ seasonId, pageNum: 1, pageSize: 500 }).then(res => {
    const seen = new Set<number>()
    seasonTeams.value = (res.rows || []).reduce<SeasonCandidateTeam[]>((acc, item) => {
      if (!item.sourceTeamId || seen.has(item.sourceTeamId)) return acc
      seen.add(item.sourceTeamId)
      acc.push({ teamId: item.sourceTeamId, teamName: item.teamNameSnapshot || String(item.sourceTeamId) })
      return acc
    }, [])
  })
  return Promise.all([players, teams])
}

function syncPlayerTeam() {
  const player = seasonPlayers.value.find(item => item.sourcePlayerId === form.value.playerId)
  if (!player?.eventTeamId) return
  const team = seasonTeams.value.find(item => item.teamName === player.teamNameSnapshot)
  if (team) form.value.teamId = team.teamId
}

function onFormSeasonChange() {
  form.value.playerId = undefined
  form.value.teamId = undefined
  loadSeasonRoster(form.value.seasonId)
}

function loadFormSeasons() {
  form.value.seasonId = undefined
  form.value.playerId = undefined
  form.value.teamId = undefined
  formSeasonOptions.value = []
  seasonPlayers.value = []
  seasonTeams.value = []
  if (!form.value.tournamentId) return
  listV4Season({ seriesId: form.value.tournamentId, pageNum: 1, pageSize: 500 }).then(res => {
    formSeasonOptions.value = (res.rows || []).map((s: any) => ({ id: s.id, seasonName: s.seasonName || s.displayTitle }))
  })
}

function resetFormData() {
  form.value = {
    tournamentId: selectedTournamentId.value,
    seasonId: queryParams.value.seasonId,
    honorCode: codeOptions.value[0]?.value || 'PLAYER_HONOR',
    playerId: undefined,
    teamId: undefined,
    honorTitle: undefined,
    honorTitleEn: '',
    honorSubtitle: undefined,
    honorSubtitleEn: '',
    honorValue: undefined,
    awardDate: undefined,
    coverFileId: undefined,
    displayOrder: 0,
    remark: undefined
  }
  coverPreview.value = ''
  formSeasonOptions.value = seasonOptions.value
  if (form.value.seasonId) loadSeasonRoster(form.value.seasonId)
  else {
    seasonPlayers.value = []
    seasonTeams.value = []
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增球员荣誉'
  dialogOpen.value = true
}

function handleUpdate(row: HonorRecordListItem) {
  resetFormData()
  currentId.value = row.id
  getPlayerHonor(row.id).then(res => {
    const data = res.data!
    form.value = {
      tournamentId: data.tournamentId,
      seasonId: data.seasonId,
      honorCode: data.honorCode,
      playerId: data.playerId,
      teamId: data.teamId,
      honorTitle: data.honorTitle,
      honorTitleEn: data.honorTitleEn || '',
      honorSubtitle: data.honorSubtitle,
      honorSubtitleEn: data.honorSubtitleEn || '',
      honorValue: data.honorValue,
      awardDate: dateOnly(data.awardDate),
      coverFileId: data.coverFileId,
      displayOrder: data.displayOrder ?? 0,
      remark: data.remark
    }
    coverPreview.value = data.coverUrl || ''
    if (data.tournamentId) {
      listV4Season({ seriesId: data.tournamentId, pageNum: 1, pageSize: 500 }).then(sRes => {
        formSeasonOptions.value = (sRes.rows || []).map((s: any) => ({ id: s.id, seasonName: s.seasonName || s.displayTitle }))
      })
    }
    if (data.seasonId) {
      loadSeasonRoster(data.seasonId).then(() => {
        if (data.playerId && !seasonPlayers.value.some(item => item.sourcePlayerId === data.playerId)) {
          seasonPlayers.value.unshift({
            sourcePlayerId: data.playerId,
            playerNameSnapshot: data.playerName || String(data.playerId),
            teamNameSnapshot: data.teamName
          })
        }
        if (data.teamId && !seasonTeams.value.some(item => item.teamId === data.teamId)) {
          seasonTeams.value.unshift({ teamId: data.teamId!, teamName: data.teamName || String(data.teamId) })
        }
      })
    }
    dialogTitle.value = '修改球员荣誉'
    dialogOpen.value = true
  })
}

function handleDetail(row: HonorRecordListItem) {
  getPlayerHonor(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updatePlayerHonor(currentId.value, form.value)
      : addPlayerHonor(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认发布荣誉「${row.honorTitle || row.id}」？`).then(() =>
    publishPlayerHonor(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  }).catch(() => {})
}

function handleRevoke(row: HonorRecordListItem) {
  revokeId.value = row.id
  revokeForm.value = { revokeReason: '' }
  revokeOpen.value = true
}

function submitRevoke() {
  proxy.$refs.revokeRef.validate((valid: boolean) => {
    if (!valid || !revokeId.value) return
    proxy.$modal.confirm('确认撤销该荣誉？').then(() =>
      revokePlayerHonor(revokeId.value!, revokeForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('撤销成功')
      revokeOpen.value = false
      getList()
    }).catch(() => {})
  })
}

function handleDelete(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认删除荣誉「${row.honorTitle || row.id}」？`).then(() =>
    delPlayerHonor(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function openCandidate() {
  candidateQuery.value.seasonId = queryParams.value.seasonId
  candidateOpen.value = true
  if (candidateQuery.value.seasonId && candidateQuery.value.awardType) {
    loadCandidates()
  }
}

function loadCandidates() {
  if (!candidateQuery.value.seasonId || !candidateQuery.value.awardType) {
    proxy.$modal.msgWarning('请选择赛季与奖项')
    return
  }
  candidateLoading.value = true
  listPlayerHonorCandidate(candidateQuery.value).then(res => {
    candidates.value = res.data || []
  }).finally(() => {
    candidateLoading.value = false
  })
}

function applyCandidate(row: PlayerHonorCandidate) {
  const awardMap: Record<string, string> = {
    SCORING: 'SCORING_CHAMPION',
    REBOUND: 'REBOUND_CHAMPION',
    ASSIST: 'ASSIST_CHAMPION',
    STEAL: 'STEAL_CHAMPION',
    BLOCK: 'BLOCK_CHAMPION'
  }
  resetFormData()
  form.value.honorCode = awardMap[candidateQuery.value.awardType || ''] || 'PLAYER_HONOR'
  form.value.playerId = row.playerId
  form.value.teamId = row.teamId
  form.value.honorTitle = PLAYER_HONOR_AWARD_TYPE_OPTIONS.find(i => i.value === candidateQuery.value.awardType)?.label
  form.value.honorValue = row.statValue != null ? String(row.statValue) : undefined
  currentId.value = undefined
  candidateOpen.value = false
  dialogTitle.value = '新增球员荣誉（候选）'
  dialogOpen.value = true
}

loadTournaments()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
