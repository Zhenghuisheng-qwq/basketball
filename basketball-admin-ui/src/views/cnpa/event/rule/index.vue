<template>
  <div class="app-container">
    <el-alert
      title="【已废弃】旧赛事规则入口"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
      description="V4 赛制当前在 Season/Division 维护，本页面将在后续轮次移除。"
    />
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="所属赛事">
        <el-select v-model="selectedTournamentId" placeholder="请选择赛事" filterable style="width: 360px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="formatEventOptionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="所属赛季">
        <el-input :model-value="operationalSeasonLabel" disabled placeholder="请先选择赛事" style="width: 260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="getList" :disabled="!selectedSeasonId">查询</el-button>
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:tournamentRule:add']" :disabled="!selectedSeasonId">新增规则</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="规则标题" prop="ruleTitle" min-width="160" />
      <el-table-column label="规则类型" prop="ruleType" width="120" />
      <el-table-column label="版本号" prop="versionNo" width="100" />
      <el-table-column label="生效时间" prop="effectiveTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.effectiveTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="ruleStatus" width="80">
        <template #default="scope">{{ scope.row.ruleStatus === 1 ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column label="排序" prop="displayOrder" width="80" />
      <el-table-column label="操作" align="center" width="180" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:tournamentRule:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:tournamentRule:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="规则标题" prop="ruleTitle">
          <el-input v-model="form.ruleTitle" placeholder="请输入规则标题" />
        </el-form-item>
        <el-form-item label="规则类型">
          <el-input v-model="form.ruleType" placeholder="请输入规则类型" />
        </el-form-item>
        <el-form-item label="版本号">
          <el-input v-model="form.versionNo" placeholder="请输入版本号" />
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="form.effectiveTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择生效时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.ruleStatus">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.displayOrder" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="规则内容">
          <el-input v-model="form.ruleContent" type="textarea" :rows="8" placeholder="请输入规则内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketTournamentRule">
import { addTournamentRule, delTournamentRule, listTournamentRule, updateTournamentRule } from '@/api/basket/tournamentRule'
import { listTournament } from '@/api/basket/tournament'
import type { TournamentListItem, TournamentRule, TournamentRuleCreateReq } from '@/types'
import { formatEventOptionLabel } from '@/types'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const dataList = ref<TournamentRule[]>([])
const tournamentOptions = ref<TournamentListItem[]>([])
const selectedTournamentId = ref<number>()
const selectedSeasonId = ref<number>()

const operationalSeasonLabel = computed(() => {
  const tournament = tournamentOptions.value.find(item => item.id === selectedTournamentId.value)
  if (!tournament) return ''
  if (!tournament.operationalSeasonId) return '该赛事尚未初始化运营赛季'
  const seasonName = tournament.competitionSeasonName || '运营赛季'
  return tournament.competitionSeasonYear
    ? `${seasonName} (${tournament.competitionSeasonYear})`
    : seasonName
})
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()

const form = ref<TournamentRuleCreateReq>({
  ruleTitle: '',
  ruleStatus: 1,
  displayOrder: 0
})

const rules = {
  ruleTitle: [{ required: true, message: '规则标题不能为空', trigger: 'blur' }]
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows
    if (res.rows.length > 0) {
      selectedTournamentId.value = res.rows[0].id
      handleTournamentChange()
    }
  })
}

function handleTournamentChange() {
  const tournament = tournamentOptions.value.find(item => item.id === selectedTournamentId.value)
  selectedSeasonId.value = tournament?.operationalSeasonId
  if (selectedSeasonId.value) {
    getList()
  } else {
    dataList.value = []
  }
}

function getList() {
  if (!selectedSeasonId.value) return
  loading.value = true
  listTournamentRule(selectedSeasonId.value).then(res => {
    dataList.value = res.data || []
  }).finally(() => {
    loading.value = false
  })
}

function resetFormData() {
  form.value = {
    ruleTitle: '',
    ruleType: '',
    ruleContent: '',
    versionNo: '',
    effectiveTime: undefined,
    ruleStatus: 1,
    displayOrder: 0
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增规则'
  dialogOpen.value = true
}

function handleUpdate(row: TournamentRule) {
  resetFormData()
  currentId.value = row.id
  form.value = {
    ruleType: row.ruleType,
    ruleTitle: row.ruleTitle || '',
    ruleContent: row.ruleContent,
    versionNo: row.versionNo,
    effectiveTime: row.effectiveTime,
    ruleStatus: row.ruleStatus ?? 1,
    displayOrder: row.displayOrder ?? 0
  }
  dialogTitle.value = '修改规则'
  dialogOpen.value = true
}

function submitForm() {
  if (!selectedSeasonId.value) return
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateTournamentRule(currentId.value, form.value)
      : addTournamentRule(selectedSeasonId.value, form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handleDelete(row: TournamentRule) {
  proxy.$modal.confirm(`确认删除规则「${row.ruleTitle}」？`).then(() => delTournamentRule(row.id)).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

loadTournaments()
</script>
