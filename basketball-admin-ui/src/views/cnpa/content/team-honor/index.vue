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
        <el-input v-model="queryParams.keyword" placeholder="标题/球队" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:teamHonor:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="赛季" prop="seasonName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="荣誉" prop="honorTitle" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="编码" prop="honorCode" width="130" />
      <el-table-column label="球队" prop="teamName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="displayStatus" width="100">
        <template #default="scope">
          <dict-tag :options="HONOR_DISPLAY_STATUS_OPTIONS" :value="scope.row.displayStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="displayOrder" width="70" />
      <el-table-column label="颁发日期" prop="awardDate" width="120">
        <template #default="scope">{{ parseTime(scope.row.awardDate, '{y}-{m}-{d}') }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:teamHonor:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:teamHonor:edit']" v-if="[0, 2].includes(scope.row.displayStatus!)">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:teamHonor:publish']" v-if="[0, 2].includes(scope.row.displayStatus!)">发布</el-button>
          <el-button link type="danger" icon="RefreshLeft" @click="handleRevoke(scope.row)" v-hasPermi="['basket:teamHonor:revoke']" v-if="[0, 1].includes(scope.row.displayStatus!)">撤销</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:teamHonor:remove']" v-if="scope.row.displayStatus !== 1">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="球队荣誉详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="荣誉标题">{{ detail.honorTitle }}</el-descriptions-item>
        <el-descriptions-item label="编码">{{ detail.honorCode }}</el-descriptions-item>
        <el-descriptions-item label="球队">{{ detail.teamName || detail.teamId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.displayStatusName }}</el-descriptions-item>
        <el-descriptions-item label="副标题">{{ detail.honorSubtitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授予日期">{{ parseTime(detail.awardDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.honorCode === 'CHAMPION'" label="冠军阵容">
          <div v-if="detail.lineup?.length">
            <div v-for="item in detail.lineup" :key="item.playerId">
              {{ item.sortNo }} · {{ item.playerName || item.playerId }} · {{ item.roleType }}
            </div>
          </div>
          <span v-else>未配置</span>
          <el-button link type="primary" @click="openLineup" v-hasPermi="['basket:teamHonor:edit']">维护阵容</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="封面">
          <el-image v-if="detail.coverUrl" :src="detail.coverUrl" style="width: 120px; height: 80px" fit="cover" />
          <span v-else>-</span>
        </el-descriptions-item>
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
          <el-select v-model="form.seasonId" filterable style="width: 100%" @change="loadCandidates">
            <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="荣誉编码" prop="honorCode">
          <el-select v-model="form.honorCode" style="width: 100%">
            <el-option v-for="item in codeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="球队" prop="teamId">
          <el-select v-model="form.teamId" filterable style="width: 100%">
            <el-option v-for="item in candidateTeams" :key="item.teamId" :label="item.teamName" :value="item.teamId" />
          </el-select>
        </el-form-item>
        <el-form-item label="荣誉标题">
          <el-input v-model="form.honorTitle" placeholder="默认取分类名称" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.honorSubtitle" />
        </el-form-item>
        <el-form-item label="授予日期" prop="awardDate">
          <el-date-picker v-model="form.awardDate" type="date" value-format="YYYY-MM-DD" placeholder="选择授予日期" style="width: 100%" />
        </el-form-item>
        <el-divider content-position="left">英文内容</el-divider>
        <div class="el-form-item__tip bilingual-tip">英文选填，不改变荣誉归属。留空时官网显示中文标题。</div>
        <el-form-item label="英文标题">
          <el-input v-model="form.honorTitleEn" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="英文副标题">
          <el-input v-model="form.honorSubtitleEn" maxlength="256" show-word-limit />
        </el-form-item>
        <el-form-item label="封面文件ID">
          <el-input-number v-model="form.coverFileId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封面预览">
          <image-upload v-model="coverPreview" :limit="1" :file-size="5" />
          <div class="el-upload__tip">预览上传走 /common/upload；提交请填写 basket_file_asset 文件 ID</div>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.displayOrder" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="冠军阵容" v-model="lineupOpen" width="640px" append-to-body>
      <el-form label-width="88px">
        <el-form-item label="赛季球员">
          <el-select v-model="lineupPlayerIds" multiple filterable style="width: 100%" placeholder="按出场顺序选择">
            <el-option v-for="item in lineupPlayers" :key="item.sourcePlayerId" :label="lineupPlayerLabel(item)" :value="item.sourcePlayerId!" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="lineupOpen = false">取消</el-button>
        <el-button type="primary" @click="saveLineup">保存阵容</el-button>
      </template>
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

<script setup lang="ts" name="BasketTeamHonor">
import {
  addTeamHonor,
  delTeamHonor,
  getTeamHonor,
  listTeamHonor,
  publishTeamHonor,
  replaceTeamHonorLineup,
  revokeTeamHonor,
  updateTeamHonor
} from '@/api/basket/teamHonor'
import type {
  HonorRecordAddReq,
  HonorRecordDetail,
  HonorRecordListItem,
  HonorRecordQuery,
  HonorRecordRevokeReq,
  SeasonCandidateTeam,
  SeasonListItem,
  TournamentListItem
} from '@/types'
import { HONOR_DISPLAY_STATUS_OPTIONS, TEAM_HONOR_CODE_OPTIONS } from '@/types'
import { listV4EventPlayer, listV4EventTeam, type BasketEventPlayerListVo } from '@/api/cnpa/event-v4/event-team'
import { listV4Series } from '@/api/cnpa/event-v4/series'
import { listV4Season } from '@/api/cnpa/event-v4/season'

const props = withDefaults(defineProps<{ lockedCodes?: string[] }>(), {
  lockedCodes: () => []
})

const { proxy } = getCurrentInstance()

const codeOptions = computed(() => {
  if (!props.lockedCodes.length) return TEAM_HONOR_CODE_OPTIONS
  return TEAM_HONOR_CODE_OPTIONS.filter(item => props.lockedCodes.includes(item.value))
})
const singleLock = computed(() => props.lockedCodes.length === 1)

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
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const formSeasonOptions = ref<SeasonListItem[]>([])
const candidateTeams = ref<SeasonCandidateTeam[]>([])
const selectedTournamentId = ref<number>()
const lineupOpen = ref(false)
const lineupPlayerIds = ref<number[]>([])
const lineupPlayers = ref<BasketEventPlayerListVo[]>([])

const queryParams = ref<HonorRecordQuery>({
  pageNum: 1,
  pageSize: 10,
  seasonId: undefined,
  honorCode: undefined,
  displayStatus: undefined,
  keyword: undefined
})

const form = ref<HonorRecordAddReq>({
  honorCode: 'TEAM_HONOR',
  displayOrder: 0
})

const revokeForm = ref<HonorRecordRevokeReq>({ revokeReason: '' })
const rules = {
  tournamentId: [{ required: true, message: '请选择赛事', trigger: 'change' }],
  seasonId: [{ required: true, message: '请选择赛季', trigger: 'change' }],
  honorCode: [{ required: true, message: '请选择荣誉编码', trigger: 'change' }],
  teamId: [{ required: true, message: '请选择球队', trigger: 'change' }],
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
  })
}

function loadCandidateTeams(seasonId?: number) {
  candidateTeams.value = []
  if (!seasonId) return Promise.resolve()
  return listV4EventTeam({ seasonId, pageNum: 1, pageSize: 500 }).then(res => {
    const seen = new Set<number>()
    candidateTeams.value = (res.rows || []).reduce<SeasonCandidateTeam[]>((acc, item) => {
      const teamId = item.sourceTeamId
      if (!teamId || seen.has(teamId)) return acc
      seen.add(teamId)
      acc.push({ teamId, teamName: item.teamNameSnapshot })
      return acc
    }, [])
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
  listTeamHonor(queryParams.value).then(res => {
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

function loadFormSeasons() {
  form.value.seasonId = undefined
  formSeasonOptions.value = []
  candidateTeams.value = []
  if (!form.value.tournamentId) return
  listV4Season({ seriesId: form.value.tournamentId, pageNum: 1, pageSize: 500 }).then(res => {
    formSeasonOptions.value = res.rows || []
  })
}

function loadCandidates() {
  form.value.teamId = undefined
  loadCandidateTeams(form.value.seasonId)
}

function resetFormData() {
  form.value = {
    tournamentId: selectedTournamentId.value,
    seasonId: queryParams.value.seasonId,
    honorCode: codeOptions.value[0]?.value || 'TEAM_HONOR',
    teamId: undefined,
    honorTitle: undefined,
    honorTitleEn: '',
    honorSubtitle: undefined,
    honorSubtitleEn: '',
    awardDate: undefined,
    coverFileId: undefined,
    displayOrder: 0,
    remark: undefined
  }
  coverPreview.value = ''
  formSeasonOptions.value = seasonOptions.value
  if (form.value.seasonId) {
    loadCandidateTeams(form.value.seasonId)
  } else {
    candidateTeams.value = []
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增球队荣誉'
  dialogOpen.value = true
}

function handleUpdate(row: HonorRecordListItem) {
  resetFormData()
  currentId.value = row.id
  getTeamHonor(row.id).then(res => {
    const data = res.data!
    form.value = {
      tournamentId: data.tournamentId,
      seasonId: data.seasonId,
      honorCode: data.honorCode,
      teamId: data.teamId,
      honorTitle: data.honorTitle,
      honorTitleEn: data.honorTitleEn || '',
      honorSubtitle: data.honorSubtitle,
      honorSubtitleEn: data.honorSubtitleEn || '',
      awardDate: dateOnly(data.awardDate),
      coverFileId: data.coverFileId,
      displayOrder: data.displayOrder ?? 0,
      remark: data.remark
    }
    coverPreview.value = data.coverUrl || ''
    if (data.tournamentId) {
      listV4Season({ seriesId: data.tournamentId, pageNum: 1, pageSize: 500 }).then(sRes => {
        formSeasonOptions.value = sRes.rows || []
      })
    }
    if (data.seasonId) {
      loadCandidateTeams(data.seasonId)
    }
    dialogTitle.value = '修改球队荣誉'
    dialogOpen.value = true
  })
}

function handleDetail(row: HonorRecordListItem) {
  getTeamHonor(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateTeamHonor(currentId.value, form.value)
      : addTeamHonor(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认发布荣誉「${row.honorTitle || row.id}」？`).then(() =>
    publishTeamHonor(row.id)
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
      revokeTeamHonor(revokeId.value!, revokeForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('撤销成功')
      revokeOpen.value = false
      getList()
    }).catch(() => {})
  })
}

function lineupPlayerLabel(item: BasketEventPlayerListVo) {
  const name = item.playerNameSnapshot || item.gameIdSnapshot || item.sourcePlayerId
  return item.teamNameSnapshot ? `${name} · ${item.teamNameSnapshot}` : String(name)
}

function openLineup() {
  if (!detail.value?.seasonId) {
    proxy.$modal.msgWarning('这条冠军记录没有赛季，无法加载阵容候选')
    return
  }
  lineupPlayerIds.value = (detail.value.lineup || []).map(item => item.playerId!).filter(Boolean)
  listV4EventPlayer({ seasonId: detail.value.seasonId, pageNum: 1, pageSize: 500 }).then(res => {
    const seen = new Set<number>()
    lineupPlayers.value = (res.rows || []).filter(item => {
      if (!item.sourcePlayerId || seen.has(item.sourcePlayerId)) return false
      seen.add(item.sourcePlayerId)
      return true
    })
    lineupOpen.value = true
  })
}

function saveLineup() {
  if (!detail.value?.id) return
  const rows = lineupPlayerIds.value.map((playerId, index) => ({
    playerId,
    teamId: detail.value?.teamId,
    roleType: index < 5 ? 'STARTER' : 'SUBSTITUTE',
    sortNo: index + 1
  }))
  replaceTeamHonorLineup(detail.value.id, rows).then(() => {
    proxy.$modal.msgSuccess('冠军阵容已保存')
    lineupOpen.value = false
    return getTeamHonor(detail.value!.id)
  }).then(res => {
    detail.value = res.data
  })
}

function handleDelete(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认删除荣誉「${row.honorTitle || row.id}」？`).then(() =>
    delTeamHonor(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

loadTournaments()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
