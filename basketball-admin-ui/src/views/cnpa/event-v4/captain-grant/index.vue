<template>
  <div class="app-container">
    <el-alert title="队长授权：按平台赛区授予已认证球员赛事期内队长权限（授权时无需已有球队）" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="queryParams" :inline="true" class="mb8">
      <el-form-item label="赛季">
        <el-select v-model="queryParams.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onQuerySeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="queryParams.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!queryParams.seasonId">
          <el-option v-for="item in queryDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="球员昵称">
        <el-input v-model="queryParams.playerName" clearable placeholder="球员昵称" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.grantStatus" clearable style="width: 140px">
          <el-option v-for="item in GRANT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-collapse class="mb8">
      <el-collapse-item title="高级筛选（ID）" name="advanced">
        <el-form :model="queryParams" :inline="true">
          <el-form-item label="Player ID"><el-input-number v-model="queryParams.playerId" :min="1" controls-position="right" /></el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openAdd" v-hasPermi="['basket:v4CaptainGrant:add']">新增授权</el-button>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="赛季" prop="seasonTitle" min-width="140" />
      <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
      <el-table-column label="平台" prop="platformCode" width="80" />
      <el-table-column label="授权球员" prop="playerName" min-width="120" />
      <el-table-column label="账号" min-width="140">
        <template #default="scope">
          {{ scope.row.memberNickname ? `${scope.row.memberNickname}（#${scope.row.memberId}）` : (scope.row.memberId ? `#${scope.row.memberId}` : '-') }}
        </template>
      </el-table-column>
      <el-table-column label="Player ID" prop="playerId" width="100" />
      <el-table-column label="后续使用球队" min-width="140">
        <template #default="scope">{{ scope.row.teamBoundLabel || scope.row.teamName || '尚未绑定球队' }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="grantStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4GrantStatusTagType(scope.row.grantStatus)" size="small" class="nowrap-tag" :title="scope.row.grantStatus">
            {{ v4GrantStatusLabel(scope.row.grantStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" prop="grantSource" width="110" />
      <el-table-column label="有效期" min-width="180">
        <template #default="scope">{{ formatRange(scope.row.validFrom, scope.row.validTo) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="danger" @click="handleRevoke(scope.row)" v-hasPermi="['basket:v4CaptainGrant:revoke']" v-if="scope.row.grantStatus === 'ACTIVE'">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="addOpen" title="新增队长授权" width="560px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="选择赛季" prop="seasonId">
          <el-select v-model="formSeasonId" filterable placeholder="选择赛季" style="width: 100%" @change="onFormSeasonChange">
            <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台赛区" prop="divisionId">
          <el-select v-model="form.divisionId" filterable placeholder="选择赛区" style="width: 100%" :disabled="!formSeasonId" @change="onFormDivisionChange">
            <el-option v-for="item in formDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择球员" prop="playerId">
          <el-select v-model="form.playerId" filterable placeholder="选择已认证球员" style="width: 100%" :disabled="!form.divisionId" @change="onFormPlayerChange">
            <el-option
              v-for="item in playerOptions"
              :key="item.playerId"
              :label="playerOptionLabel(item)"
              :value="item.playerId"
              :disabled="Boolean(item.hasActiveGrant) || !item.certified"
            />
          </el-select>
          <div v-if="platformIdentityWarning" class="el-form-item__tip warning-tip">{{ platformIdentityWarning }}</div>
        </el-form-item>
        <el-form-item label="用户账号 / Member ID">
          <el-input :model-value="memberDisplay" disabled />
        </el-form-item>
        <el-form-item label="球员 ID">
          <el-input :model-value="form.playerId ? String(form.playerId) : '-'" disabled />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="form.validFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="留空则立即生效" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-input :model-value="resolvedValidToDisplay" disabled />
          <div class="el-form-item__tip">随 Division / 赛季结束时间自动计算</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addOpen = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4CaptainGrant">
import { computed, onMounted, reactive, ref } from 'vue'
import { addV4CaptainGrant, listV4CaptainGrant, resolveV4CaptainGrantValidTo, revokeV4CaptainGrant, type BasketCaptainGrantCreateReq, type BasketCaptainGrantQuery } from '@/api/cnpa/event-v4/captain-grant'
import { listV4AdminDivisionOptions, listV4AdminGrantablePlayerOptions, listV4AdminSeasonOptions, type V4AdminGrantablePlayerOptionVo, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import { V4_GRANT_STATUS_FILTER_OPTIONS, v4GrantStatusLabel, v4GrantStatusTagType } from '@/utils/v4AdminDisplay'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const total = ref(0)
const dataList = ref<any[]>([])
const addOpen = ref(false)
const formRef = ref()
const seasonOptions = ref<V4AdminOptionVo[]>([])
const queryDivisionOptions = ref<V4AdminOptionVo[]>([])
const formDivisionOptions = ref<V4AdminOptionVo[]>([])
const playerOptions = ref<V4AdminGrantablePlayerOptionVo[]>([])
const formSeasonId = ref<number>()
const resolvedValidTo = ref<string>()
const queryParams = reactive<BasketCaptainGrantQuery & { playerName?: string }>({ pageNum: 1, pageSize: 10 })
const form = reactive<BasketCaptainGrantCreateReq & { seasonId?: number }>({})
const GRANT_STATUS_OPTIONS = V4_GRANT_STATUS_FILTER_OPTIONS

const selectedPlayer = computed(() => playerOptions.value.find(p => p.playerId === form.playerId))
const memberDisplay = computed(() => {
  const p = selectedPlayer.value
  if (!p?.memberId) return '-'
  return p.memberNickname ? `${p.memberNickname}（Member #${p.memberId}）` : `Member #${p.memberId}`
})
const resolvedValidToDisplay = computed(() => parseTime(resolvedValidTo.value) || '-')
const platformIdentityWarning = computed(() => {
  const p = selectedPlayer.value
  if (!p || !form.divisionId) return ''
  if (!p.certified) return '该球员尚未完成认证，无法授权'
  if (p.hasActiveGrant) return p.cannotSelectReason || '该球员在此 Division 已有有效授权'
  if (!p.hasPlatformIdentity) return '该球员未绑定当前平台账号，提交时后端将校验拦截'
  return ''
})

const rules = {
  divisionId: [{ required: true, message: '请选择平台赛区', trigger: 'change' }],
  playerId: [{ required: true, message: '请选择球员', trigger: 'change' }]
}

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

function formatRange(start?: string, end?: string) {
  if (!start && !end) return '-'
  return `${parseTime(start) || '-'} ~ ${parseTime(end) || '-'}`
}

function playerOptionLabel(item: V4AdminGrantablePlayerOptionVo) {
  const account = item.memberNickname ? ` / ${item.memberNickname}` : ''
  if (item.hasActiveGrant) return `${item.playerName}${account}（已授权）`
  if (!item.certified) return `${item.playerName}（未认证）`
  if (!item.hasPlatformIdentity) return `${item.playerName}${account}（未绑定平台）`
  return `${item.playerName}${account} (#${item.playerId})`
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadQueryDivisions(seasonId?: number) {
  queryDivisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  queryDivisionOptions.value = res.data || []
}

async function loadFormDivisions(seasonId?: number) {
  formDivisionOptions.value = []
  if (!seasonId) return
  const res = await listV4AdminDivisionOptions(seasonId)
  formDivisionOptions.value = res.data || []
}

async function loadPlayers(divisionId?: number) {
  playerOptions.value = []
  resolvedValidTo.value = undefined
  if (!divisionId) return
  const [playersRes, validToRes] = await Promise.all([
    listV4AdminGrantablePlayerOptions(divisionId),
    resolveV4CaptainGrantValidTo(divisionId)
  ])
  playerOptions.value = playersRes.data || []
  resolvedValidTo.value = validToRes.data as string
}

function onQuerySeasonChange(seasonId?: number) {
  queryParams.divisionId = undefined
  loadQueryDivisions(seasonId)
}

function onFormSeasonChange(seasonId?: number) {
  form.divisionId = undefined
  form.playerId = undefined
  form.memberId = undefined
  loadFormDivisions(seasonId)
}

function onFormDivisionChange() {
  form.playerId = undefined
  form.memberId = undefined
  loadPlayers(form.divisionId)
}

function onFormPlayerChange(playerId?: number) {
  const p = playerOptions.value.find(item => item.playerId === playerId)
  form.memberId = p?.memberId
}

function getList() {
  loading.value = true
  listV4CaptainGrant(queryParams).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, playerId: undefined, grantStatus: undefined, playerName: undefined })
  queryDivisionOptions.value = []
  getList()
}

function openAdd() {
  formSeasonId.value = undefined
  Object.assign(form, { divisionId: undefined, playerId: undefined, memberId: undefined, validFrom: undefined, remark: undefined })
  formDivisionOptions.value = []
  playerOptions.value = []
  resolvedValidTo.value = undefined
  addOpen.value = true
}

function submitAdd() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const p = selectedPlayer.value
    if (p && !p.certified) {
      proxy.$modal.msgError('请选择已认证球员')
      return
    }
    if (p?.hasActiveGrant) {
      proxy.$modal.msgError(p.cannotSelectReason || '该球员已有有效授权')
      return
    }
    addV4CaptainGrant(form).then(() => {
      proxy.$modal.msgSuccess('授权成功')
      addOpen.value = false
      getList()
    })
  })
}

function handleRevoke(row: any) {
  proxy.$modal.confirm(`确认撤销球员「${row.playerName}」在「${row.divisionName}」的队长授权？`).then(() => {
    return revokeV4CaptainGrant(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('已撤销')
    getList()
  }).catch(() => {})
}

onMounted(() => {
  loadSeasonOptions()
  getList()
})
</script>

<style scoped>
.warning-tip { color: var(--el-color-warning); font-size: 12px; margin-top: 4px; }
</style>
