<template>
  <div>
    <el-form :inline="true" class="mb8" @submit.prevent>
      <el-form-item label="赛事系列">
        <el-input v-model="query.seriesCode" clearable placeholder="LEAGUE / ARENA / CUP / OPEN" style="width: 220px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="报名方式">
        <el-select v-model="query.registrationMode" clearable placeholder="全部" style="width: 140px">
          <el-option label="球队报名" value="TEAM" />
          <el-option label="个人报名" value="PERSONAL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="load">搜索</el-button>
        <el-button icon="Refresh" @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border highlight-current-row @row-click="onRow">
      <el-table-column label="赛事系列" min-width="120">
        <template #default="scope">{{ scope.row.seriesName || scope.row.seriesCode || '-' }}</template>
      </el-table-column>
      <el-table-column label="赛季" prop="seasonTitle" min-width="140" />
      <el-table-column label="当前赛事赛区" prop="divisionName" min-width="140" />
      <el-table-column label="模板" width="110">
        <template #default="scope">{{ scope.row.templateType || '-' }}</template>
      </el-table-column>
      <el-table-column label="报名" width="90">
        <template #default="scope">{{ v4RegistrationModeLabel(scope.row.registrationMode) }}</template>
      </el-table-column>
      <el-table-column label="模式" width="70">
        <template #default="scope">{{ v4GameModeLabel(scope.row.gameMode) }}</template>
      </el-table-column>
      <el-table-column label="平台" prop="platformCode" width="80" />
      <el-table-column label="赛事规则" width="140">
        <template #default="scope">{{ scope.row.ruleStatusName || '-' }}</template>
      </el-table-column>
      <el-table-column label="参赛名单" width="130">
        <template #default="scope">{{ scope.row.rosterStatusName || '-' }}</template>
      </el-table-column>
      <el-table-column label="赛程结构" min-width="120" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.structureSummary || '-' }}</template>
      </el-table-column>
      <el-table-column label="排期/发布" min-width="130" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.scheduleSummary || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click.stop="emit('select', scope.row)">进入</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listV4DivisionSelector, type DivisionSelectorItemVo, type DivisionWorkspaceEntry } from '@/api/cnpa/event-v4/overview'
import { v4GameModeLabel, v4RegistrationModeLabel } from '@/utils/v4AdminDisplay'

const props = defineProps<{ entry: DivisionWorkspaceEntry }>()
const emit = defineEmits<{ select: [row: DivisionSelectorItemVo] }>()

const loading = ref(false)
const rows = ref<DivisionSelectorItemVo[]>([])
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  seriesCode: '',
  registrationMode: undefined as string | undefined
})

function load() {
  loading.value = true
  listV4DivisionSelector({ ...query, entry: props.entry }).then(res => {
    rows.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function reset() {
  query.seriesCode = ''
  query.registrationMode = undefined
  query.pageNum = 1
  load()
}

function onRow(row: DivisionSelectorItemVo) {
  if (row.selectable === false && row.blockReason) {
    ElMessage.warning(row.blockReason)
  }
  emit('select', row)
}

onMounted(load)
</script>
