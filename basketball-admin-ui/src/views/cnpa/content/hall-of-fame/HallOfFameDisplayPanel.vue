<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="所属赛事">
        <el-select v-model="queryParams.tournamentId" placeholder="请选择赛事" filterable clearable style="width: 220px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季" prop="seasonId">
        <el-select v-model="queryParams.seasonId" placeholder="赛季" filterable clearable style="width: 200px">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="范围" prop="honorScope">
        <el-select v-model="queryParams.honorScope" placeholder="荣誉范围" clearable style="width: 140px">
          <el-option label="赛事" value="TOURNAMENT" />
          <el-option label="球队" value="TEAM" />
          <el-option label="球员" value="PLAYER" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="displayStatus">
        <el-select v-model="queryParams.displayStatus" placeholder="展示状态" clearable style="width: 140px">
          <el-option v-for="item in HONOR_DISPLAY_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="标题/对象" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:hallOfFame:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Download" @click="openImportDialog" v-hasPermi="['basket:hallOfFame:add']">从荣誉导入</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-alert title="名人堂基于荣誉记录展示；列表默认仅看已发布，可通过状态下拉切换草稿/下线。" type="info" :closable="false" show-icon class="mb8" />

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="荣誉" prop="honorTitle" min-width="150" :show-overflow-tooltip="true" />
      <el-table-column label="编码" prop="honorCode" width="140" />
      <el-table-column label="来源" prop="sourceType" width="120" />
      <el-table-column label="来源ID" prop="sourceId" width="90" />
      <el-table-column label="范围" prop="honorScope" width="100" />
      <el-table-column label="球队" prop="teamName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="球员" prop="playerName" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="displayStatus" width="100">
        <template #default="scope">
          <dict-tag :options="HONOR_DISPLAY_STATUS_OPTIONS" :value="scope.row.displayStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="displayOrder" width="70" />
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:hallOfFame:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:hallOfFame:edit']" v-if="[0, 2].includes(scope.row.displayStatus!)">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:hallOfFame:publish']" v-if="[0, 2].includes(scope.row.displayStatus!)">发布</el-button>
          <el-button link type="warning" icon="Download" @click="handleOffline(scope.row)" v-hasPermi="['basket:hallOfFame:offline']" v-if="scope.row.displayStatus === 1">下线</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:hallOfFame:remove']" v-if="scope.row.displayStatus !== 1">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="名人堂详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="荣誉标题">{{ detail.honorTitle }}</el-descriptions-item>
        <el-descriptions-item label="编码">{{ detail.honorCode }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ detail.sourceType || '-' }} / {{ detail.sourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="生成批次">{{ detail.generationBatchId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球队">{{ detail.teamName || detail.teamId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球员">{{ detail.playerName || detail.playerId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.displayStatusName }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="封面">
          <el-image v-if="detail.coverUrl" :src="detail.coverUrl" style="width: 120px; height: 80px" fit="cover" />
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="阵容" v-if="detail.lineup?.length">
          <div v-for="item in detail.lineup" :key="item.id">{{ item.playerName || item.playerId }} ({{ item.roleType || '-' }})</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog title="从荣誉导入名人堂" v-model="importOpen" width="860px" append-to-body>
      <el-form :inline="true" class="mb8">
        <el-form-item label="赛事">
          <el-select v-model="importQuery.tournamentId" filterable clearable style="width: 200px" @change="loadImportCandidates">
            <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛季">
          <el-select v-model="importQuery.seasonId" filterable clearable style="width: 180px" @change="loadImportCandidates">
            <el-option v-for="item in importSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table v-loading="importLoading" :data="importCandidates" border max-height="420">
        <el-table-column label="荣誉" prop="honorTitle" min-width="140" />
        <el-table-column label="编码" prop="honorCode" width="130" />
        <el-table-column label="来源" prop="sourceType" width="120" />
        <el-table-column label="球队" prop="teamName" min-width="100" />
        <el-table-column label="球员" prop="playerName" min-width="100" />
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="handleImport(scope.row)">导入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属赛事">
          <el-select v-model="form.tournamentId" filterable clearable style="width: 100%" @change="loadFormSeasons">
            <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛季">
          <el-select v-model="form.seasonId" filterable clearable style="width: 100%">
            <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="荣誉编码" prop="honorCode">
          <el-input v-model="form.honorCode" placeholder="如 PLAYER_HONOR / TEAM_HONOR / CHAMPION" />
        </el-form-item>
        <el-form-item label="球队ID">
          <el-input-number v-model="form.teamId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="球员ID">
          <el-input-number v-model="form.playerId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="荣誉标题" prop="honorTitle">
          <el-input v-model="form.honorTitle" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.honorSubtitle" />
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
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="HallOfFameDisplayPanel">
import {
  addHallOfFame,
  delHallOfFame,
  getHallOfFame,
  importHallFromHonor,
  listHallImportCandidates,
  listHallOfFame,
  offlineHallOfFame,
  publishHallOfFame,
  updateHallOfFame
} from '@/api/basket/hallOfFame'
import type {
  HonorRecordAddReq,
  HonorRecordDetail,
  HonorRecordListItem,
  HonorRecordQuery,
  SeasonListItem,
  TournamentListItem
} import { listV4Series } from '@/api/cnpa/event-v4/series'
import { listV4Season } from '@/api/cnpa/event-v4/season'
from '@/types'
import { HONOR_DISPLAY_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<HonorRecordListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<HonorRecordDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const coverPreview = ref('')
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonOptions = ref<SeasonListItem[]>([])
const formSeasonOptions = ref<SeasonListItem[]>([])
const importOpen = ref(false)
const importLoading = ref(false)
const importCandidates = ref<HonorRecordListItem[]>([])
const importSeasonOptions = ref<SeasonListItem[]>([])
const importQuery = ref<{ tournamentId?: number; seasonId?: number }>({})

const queryParams = ref<HonorRecordQuery>({
  pageNum: 1,
  pageSize: 10,
  displayStatus: 1
})

const form = ref<HonorRecordAddReq>({
  honorCode: 'PLAYER_HONOR',
  displayOrder: 0
})

const rules = {
  honorCode: [{ required: true, message: '请填写荣誉编码', trigger: 'blur' }],
  honorTitle: [{ required: true, message: '请填写荣誉标题', trigger: 'blur' }]
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows || []
    getList()
  })
}

function handleTournamentChange() {
  seasonOptions.value = []
  queryParams.value.seasonId = undefined
  if (!queryParams.value.tournamentId) return
  listSeasonByTournament(queryParams.value.tournamentId).then(res => {
    seasonOptions.value = res.data || []
  })
}

function loadFormSeasons() {
  form.value.seasonId = undefined
  formSeasonOptions.value = []
  if (!form.value.tournamentId) return
  listSeasonByTournament(form.value.tournamentId).then(res => {
    formSeasonOptions.value = res.data || []
  })
}

function getList() {
  loading.value = true
  listHallOfFame(queryParams.value).then(res => {
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
  queryParams.value.displayStatus = 1
  handleQuery()
}

function resetFormData() {
  form.value = {
    tournamentId: queryParams.value.tournamentId,
    seasonId: queryParams.value.seasonId,
    honorCode: 'PLAYER_HONOR',
    teamId: undefined,
    playerId: undefined,
    honorTitle: undefined,
    honorSubtitle: undefined,
    coverFileId: undefined,
    displayOrder: 0,
    remark: undefined
  }
  coverPreview.value = ''
  formSeasonOptions.value = seasonOptions.value
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增名人堂条目'
  dialogOpen.value = true
}

function handleUpdate(row: HonorRecordListItem) {
  resetFormData()
  currentId.value = row.id
  getHallOfFame(row.id).then(res => {
    const data = res.data!
    form.value = {
      tournamentId: data.tournamentId,
      seasonId: data.seasonId,
      honorCode: data.honorCode,
      teamId: data.teamId,
      playerId: data.playerId,
      honorTitle: data.honorTitle,
      honorSubtitle: data.honorSubtitle,
      coverFileId: data.coverFileId,
      displayOrder: data.displayOrder ?? 0,
      remark: data.remark
    }
    coverPreview.value = data.coverUrl || ''
    if (data.tournamentId) {
      listSeasonByTournament(data.tournamentId).then(sRes => {
        formSeasonOptions.value = sRes.data || []
      })
    }
    dialogTitle.value = '修改名人堂条目'
    dialogOpen.value = true
  })
}

function handleDetail(row: HonorRecordListItem) {
  getHallOfFame(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateHallOfFame(currentId.value, form.value)
      : addHallOfFame(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认发布「${row.honorTitle || row.id}」到名人堂？`).then(() =>
    publishHallOfFame(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  }).catch(() => {})
}

function handleOffline(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认下线「${row.honorTitle || row.id}」？`).then(() =>
    offlineHallOfFame(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('下线成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认删除「${row.honorTitle || row.id}」？`).then(() =>
    delHallOfFame(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function openImportDialog() {
  importQuery.value = {
    tournamentId: queryParams.value.tournamentId,
    seasonId: queryParams.value.seasonId
  }
  importSeasonOptions.value = seasonOptions.value
  importOpen.value = true
  loadImportCandidates()
}

function loadImportCandidates() {
  importLoading.value = true
  if (importQuery.value.tournamentId && importSeasonOptions.value.length === 0) {
    listSeasonByTournament(importQuery.value.tournamentId).then(res => {
      importSeasonOptions.value = res.data || []
    })
  }
  listHallImportCandidates(importQuery.value).then(res => {
    importCandidates.value = res.data || []
  }).finally(() => {
    importLoading.value = false
  })
}

function handleImport(row: HonorRecordListItem) {
  proxy.$modal.confirm(`确认将荣誉「${row.honorTitle || row.id}」导入名人堂？`).then(() =>
    importHallFromHonor(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('导入成功，请在列表中发布')
    importOpen.value = false
    queryParams.value.displayStatus = 0
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
