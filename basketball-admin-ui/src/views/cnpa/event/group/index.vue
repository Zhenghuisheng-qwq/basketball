<template>
  <div class="app-container">
    <el-alert
      title="【已废弃】旧赛事分组入口"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
      description="V4 当前不使用旧 group 模型，本页面将在后续轮次移除。"
    />
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="所属赛事">
        <el-select v-model="filterTournamentId" placeholder="请选择赛事" filterable style="width: 220px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属赛季">
        <el-input :model-value="operationalSeasonLabel" disabled placeholder="请先选择赛事" style="width: 200px" />
      </el-form-item>
      <el-form-item label="所属阶段">
        <el-select v-model="selectedStageId" placeholder="请选择阶段" filterable style="width: 200px" @change="getList">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList" :disabled="!selectedStageId">查询</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:group:add']" :disabled="!selectedStageId">新增分组</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="分组编码" prop="groupCode" min-width="110" />
      <el-table-column label="分组名称" prop="groupName" min-width="140" />
      <el-table-column label="出线名额" prop="qualifyCount" width="90" />
      <el-table-column label="球队数" prop="teamCount" width="80" />
      <el-table-column label="状态" prop="groupStatus" width="100">
        <template #default="scope">
          <dict-tag :options="GROUP_STATUS_OPTIONS" :value="scope.row.groupStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sequenceNo" width="70" />
      <el-table-column label="操作" align="center" width="320" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:group:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:group:edit']">修改</el-button>
          <el-button link type="success" icon="User" @click="openAssignDialog(scope.row)" v-hasPermi="['basket:group:assignTeam']">分配球队</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:group:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailOpen" title="分组详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="2" border>
        <el-descriptions-item label="分组编码">{{ detail.groupCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.groupStatusName }}</el-descriptions-item>
        <el-descriptions-item label="分组名称">{{ detail.groupName }}</el-descriptions-item>
        <el-descriptions-item label="所属阶段">{{ detail.stageName }}</el-descriptions-item>
        <el-descriptions-item label="出线名额">{{ detail.qualifyCount }}</el-descriptions-item>
        <el-descriptions-item label="排序">{{ detail.sequenceNo }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">已分配球队</el-divider>
      <el-table :data="assignedTeams" size="small" v-loading="teamLoading">
        <el-table-column label="球队ID" prop="teamId" width="90" />
        <el-table-column label="球队名称" prop="teamName" min-width="140" />
        <el-table-column label="种子序号" prop="seedNo" width="90" />
      </el-table>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分组名称" prop="groupName">
          <el-input v-model="form.groupName" placeholder="请输入分组名称" />
        </el-form-item>
        <el-form-item label="出线名额">
          <el-input-number v-model="form.qualifyCount" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sequenceNo" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.groupStatus">
            <el-radio v-for="item in GROUP_STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="分配球队" v-model="assignOpen" width="640px" append-to-body>
      <el-transfer
        v-model="selectedTeamIds"
        :data="transferTeams"
        :titles="['可选球队', '已选球队']"
        filterable
        filter-placeholder="搜索球队"
        :props="{ key: 'teamId', label: 'teamName' }"
      />
      <template #footer>
        <el-button @click="assignOpen = false">取消</el-button>
        <el-button type="primary" @click="submitAssign" :loading="assignSubmitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketGroup">
import {
  addGroup,
  assignGroupTeams,
  delGroup,
  getGroup,
  listAvailableTeams,
  listGroupByStage,
  listGroupTeams,
  updateGroup
} from '@/api/basket/tournamentGroup'
import { listStage } from '@/api/basket/tournamentStage'
import { listTournament } from '@/api/basket/tournament'
import type {
  AvailableTeam,
  GroupCreateReq,
  GroupDetail,
  GroupListItem,
  GroupTeam,
  StageListItem,
  TournamentListItem
} from '@/types'
import { GROUP_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const dataList = ref<GroupListItem[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const stageOptions = ref<StageListItem[]>([])
const filterTournamentId = ref<number>()
const filterSeasonId = ref<number>()
const selectedStageId = ref<number>()

const operationalSeasonLabel = computed(() => {
  const tournament = tournamentOptions.value.find(item => item.id === filterTournamentId.value)
  if (!tournament) return ''
  if (!tournament.operationalSeasonId) return '该赛事尚未初始化运营赛季'
  const seasonName = tournament.competitionSeasonName || '运营赛季'
  return tournament.competitionSeasonYear
    ? `${seasonName} (${tournament.competitionSeasonYear})`
    : seasonName
})

const detailOpen = ref(false)
const detail = ref<GroupDetail>()
const assignedTeams = ref<GroupTeam[]>([])
const teamLoading = ref(false)

const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const form = ref<GroupCreateReq>({
  groupName: '',
  qualifyCount: 2,
  sequenceNo: 0,
  groupStatus: 1
})
const rules = {
  groupName: [{ required: true, message: '分组名称不能为空', trigger: 'blur' }]
}

const assignOpen = ref(false)
const assignGroupId = ref<number>()
const selectedTeamIds = ref<number[]>([])
const transferTeams = ref<AvailableTeam[]>([])
const assignSubmitting = ref(false)

function resolveOperationalSeasonId(tournamentId?: number) {
  return tournamentOptions.value.find(item => item.id === tournamentId)?.operationalSeasonId
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows
    if (res.rows.length > 0) {
      filterTournamentId.value = res.rows[0].id
      handleTournamentChange()
    }
  })
}

function handleTournamentChange() {
  selectedStageId.value = undefined
  stageOptions.value = []
  dataList.value = []
  filterSeasonId.value = resolveOperationalSeasonId(filterTournamentId.value)
  if (!filterSeasonId.value) {
    return
  }
  listStage({ pageNum: 1, pageSize: 500, seasonId: filterSeasonId.value }).then(res => {
    stageOptions.value = res.rows
    if (stageOptions.value.length > 0) {
      selectedStageId.value = stageOptions.value[0].id
      getList()
    }
  })
}

function getList() {
  if (!selectedStageId.value) return
  loading.value = true
  listGroupByStage(selectedStageId.value).then(res => {
    dataList.value = res.data || []
  }).finally(() => {
    loading.value = false
  })
}

function resetFormData() {
  form.value = {
    groupName: '',
    qualifyCount: 2,
    sequenceNo: 0,
    groupStatus: 1
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增分组'
  dialogOpen.value = true
}

function handleUpdate(row: GroupListItem) {
  resetFormData()
  currentId.value = row.id
  getGroup(row.id).then(res => {
    const data = res.data!
    form.value = {
      groupName: data.groupName || '',
      qualifyCount: data.qualifyCount,
      sequenceNo: data.sequenceNo ?? 0,
      groupStatus: data.groupStatus ?? 1
    }
    dialogTitle.value = '修改分组'
    dialogOpen.value = true
  })
}

function handleDetail(row: GroupListItem) {
  detailOpen.value = true
  assignedTeams.value = []
  getGroup(row.id).then(res => {
    detail.value = res.data
  })
  teamLoading.value = true
  listGroupTeams(row.id).then(res => {
    assignedTeams.value = res.data || []
  }).finally(() => {
    teamLoading.value = false
  })
}

function submitForm() {
  if (!selectedStageId.value) return
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateGroup(currentId.value, form.value)
      : addGroup(selectedStageId.value, form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handleDelete(row: GroupListItem) {
  proxy.$modal.confirm(`确认删除分组「${row.groupName}」？`).then(() => delGroup(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function openAssignDialog(row: GroupListItem) {
  assignGroupId.value = row.id
  selectedTeamIds.value = []
  transferTeams.value = []
  assignOpen.value = true
  Promise.all([
    listGroupTeams(row.id),
    listAvailableTeams(row.id)
  ]).then(([assignedRes, availableRes]) => {
    const assigned = assignedRes.data || []
    const available = availableRes.data || []
    const map = new Map<number, AvailableTeam>()
    available.forEach(t => map.set(t.teamId, t))
    assigned.forEach(t => {
      if (t.teamId && !map.has(t.teamId)) {
        map.set(t.teamId, { teamId: t.teamId, teamName: t.teamName })
      }
    })
    transferTeams.value = Array.from(map.values())
    selectedTeamIds.value = assigned.map(t => t.teamId!).filter(Boolean)
  })
}

function submitAssign() {
  if (!assignGroupId.value) return
  assignSubmitting.value = true
  assignGroupTeams(assignGroupId.value, { teamIds: selectedTeamIds.value }).then(() => {
    proxy.$modal.msgSuccess('分配成功')
    assignOpen.value = false
    getList()
  }).finally(() => {
    assignSubmitting.value = false
  })
}

loadTournaments()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
