<template>
  <div class="app-container">
    <el-alert title="比赛列表：查询/编辑草稿比赛时间、场地、备注" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="query" :inline="true" class="mb8">
      <el-form-item label="赛季">
        <el-select v-model="query.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onSeasonChange">
          <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="平台赛区">
        <el-select v-model="query.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!query.seasonId">
          <el-option v-for="item in divisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="批次">
        <el-input-number v-model="query.scheduleBatchId" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="轮次">
        <el-input-number v-model="query.roundNo" :min="1" controls-position="right" />
      </el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="query.publishStatus" clearable style="width:120px">
          <el-option v-for="item in PUBLISH_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="比赛状态">
        <el-select v-model="query.matchStatus" clearable style="width:120px">
          <el-option v-for="item in MATCH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="球队名称">
        <el-input v-model="query.teamName" clearable placeholder="主/客队名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="比赛ID" prop="id" width="80" />
      <el-table-column label="轮次" prop="roundNo" width="60" align="center" />
      <el-table-column label="比赛编号" prop="matchNo" min-width="130" />
      <el-table-column label="比赛时间" prop="matchTime" width="160" />
      <el-table-column label="主队" prop="homeTeamName" min-width="100" />
      <el-table-column label="客队" prop="awayTeamName" min-width="100" />
      <el-table-column label="比分" width="100" align="center">
        <template #default="scope">{{ formatV4MatchScore(scope.row.homeScore, scope.row.awayScore) }}</template>
      </el-table-column>
      <el-table-column label="平台赛区" prop="divisionName" min-width="110" />
      <el-table-column label="比赛状态" prop="matchStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4MatchStatusTagType(scope.row.matchStatus)" size="small" class="nowrap-tag" :title="scope.row.matchStatus">
            {{ v4MatchStatusLabel(scope.row.matchStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布状态" prop="publishStatus" width="100" align="center">
        <template #default="scope">
          <el-tag :type="v4PublishStatusTagType(scope.row.publishStatus)" size="small" class="nowrap-tag" :title="scope.row.publishStatus">
            {{ v4PublishStatusLabel(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" v-if="scope.row.publishStatus === 'DRAFT'" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="editVisible" title="编辑草稿比赛" width="480px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="比赛时间"><el-date-picker v-model="editForm.matchTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="场地"><el-input v-model="editForm.venueText" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Match">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteV4Match, listV4Match, updateV4Match } from '@/api/cnpa/event-v4/schedule-batch'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  V4_MATCH_STATUS_FILTER_OPTIONS,
  V4_PUBLISH_STATUS_FILTER_OPTIONS,
  formatV4MatchScore,
  v4MatchStatusLabel,
  v4MatchStatusTagType,
  v4PublishStatusLabel,
  v4PublishStatusTagType
} from '@/utils/v4AdminDisplay'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10 })
const editVisible = ref(false)
const editId = ref<number>()
const editForm = reactive({ matchTime: '', venueText: '', remark: '' })
const seasonOptions = ref<V4AdminOptionVo[]>([])
const divisionOptions = ref<V4AdminOptionVo[]>([])
const PUBLISH_OPTIONS = V4_PUBLISH_STATUS_FILTER_OPTIONS
const MATCH_STATUS_OPTIONS = V4_MATCH_STATUS_FILTER_OPTIONS

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
  query.divisionId = undefined
  loadDivisions(seasonId)
}

function loadList() {
  loading.value = true
  listV4Match(query).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function openEdit(row: any) {
  editId.value = row.id
  editForm.matchTime = row.matchTime
  editForm.venueText = row.venueText || ''
  editForm.remark = row.remark || ''
  editVisible.value = true
}

function submitEdit() {
  if (!editId.value) return
  updateV4Match(editId.value, { ...editForm }).then(() => {
    ElMessage.success('已保存')
    editVisible.value = false
    loadList()
  })
}

function handleDelete(row: any) {
  ElMessageBox.confirm('确认删除该草稿比赛？', '提示', { type: 'warning' }).then(() => {
    deleteV4Match(row.id).then(() => {
      ElMessage.success('已删除')
      loadList()
    })
  })
}

onMounted(async () => {
  await loadSeasonOptions()
  loadList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
</style>
