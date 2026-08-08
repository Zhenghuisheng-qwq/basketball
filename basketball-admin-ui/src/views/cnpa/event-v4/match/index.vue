<template>
  <div class="app-container">
    <el-alert title="比赛列表：查询/编辑草稿比赛时间、场地、备注" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="query" :inline="true" class="mb8">
      <el-form-item label="视图">
        <el-radio-group v-model="viewMode">
          <el-radio-button value="match">单场列表</el-radio-button>
          <el-radio-button value="series">系列赛</el-radio-button>
        </el-radio-group>
      </el-form-item>
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
      <el-form-item v-if="viewMode === 'series'" label="阶段">
        <el-select v-model="seriesStageId" clearable filterable placeholder="选择阶段" style="width: 200px" :disabled="!query.divisionId">
          <el-option v-for="item in stageOptions" :key="item.id" :label="item.stageName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
      </el-form-item>
    </el-form>

    <MatchSeriesPanel
      v-if="viewMode === 'series'"
      ref="seriesPanelRef"
      :stage-id="seriesStageId"
      @open-match="openEditById"
    />

    <el-table v-if="viewMode === 'match'" v-loading="loading" :data="list" border>
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
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button
            link
            type="warning"
            v-if="scope.row.matchStatus === 'SCHEDULED' || scope.row.matchStatus === 'PLAYING'"
            v-hasPermi="['basket:v4Stage:exception']"
            @click="openForfeit(scope.row)"
          >判负</el-button>
          <el-button link type="danger" v-if="scope.row.publishStatus === 'DRAFT'" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-if="viewMode === 'match'" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="forfeitVisible" title="判定本局弃赛/判负" width="480px">
      <el-alert type="warning" :closable="false" show-icon class="mb8"
        :title="`将对 ${forfeitRow?.homeTeamName} vs ${forfeitRow?.awayTeamName} 执行判负，胜者为对方球队。`" />
      <el-form label-width="100px">
        <el-form-item label="判负球队" required>
          <el-radio-group v-model="forfeitLoserId">
            <el-radio :value="forfeitRow?.homeEventTeamId">{{ forfeitRow?.homeTeamName }}</el-radio>
            <el-radio :value="forfeitRow?.awayEventTeamId">{{ forfeitRow?.awayTeamName }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="forfeitReason" type="textarea" rows="3" placeholder="必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forfeitVisible = false">取消</el-button>
        <el-button type="danger" @click="submitForfeit">确认判负</el-button>
      </template>
    </el-dialog>

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
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { forfeitV4Match } from '@/api/cnpa/event-v4/stage-exception'
import { deleteV4Match, getV4Match, listV4Match, updateV4Match } from '@/api/cnpa/event-v4/schedule-batch'
import { listV4StageExecution } from '@/api/cnpa/event-v4/stage-execution'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import MatchSeriesPanel from './MatchSeriesPanel.vue'
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
const viewMode = ref<'match' | 'series'>('match')
const seriesStageId = ref<number>()
const stageOptions = ref<Array<{ id: number; stageName: string }>>([])
const seriesPanelRef = ref<InstanceType<typeof MatchSeriesPanel>>()
const forfeitVisible = ref(false)
const forfeitRow = ref<any>()
const forfeitLoserId = ref<number>()
const forfeitReason = ref('')

watch(() => query.divisionId, async (divisionId) => {
  seriesStageId.value = undefined
  stageOptions.value = []
  if (!divisionId) return
  const res = await listV4StageExecution(divisionId)
  stageOptions.value = (res.data || []).map((s: any) => ({ id: s.id, stageName: s.stageName }))
  if (stageOptions.value.length === 1) {
    seriesStageId.value = stageOptions.value[0].id
  }
})

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
  if (viewMode.value === 'series') {
    seriesPanelRef.value?.reload()
    return
  }
  loading.value = true
  listV4Match(query).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function openEditById(matchId?: number) {
  if (!matchId) return
  getV4Match(matchId).then(res => {
    if (res.data) openEdit(res.data)
  })
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

function openForfeit(row: any) {
  forfeitRow.value = row
  forfeitLoserId.value = row.awayEventTeamId
  forfeitReason.value = ''
  forfeitVisible.value = true
}

function submitForfeit() {
  if (!forfeitRow.value?.id || !forfeitLoserId.value || !forfeitReason.value?.trim()) {
    ElMessage.warning('请选择判负球队并填写原因')
    return
  }
  ElMessageBox.confirm('确认执行判负？此操作将记录审计日志。', '二次确认', { type: 'warning' }).then(() => {
    forfeitV4Match({
      matchId: forfeitRow.value.id,
      loserEventTeamId: forfeitLoserId.value!,
      reason: forfeitReason.value.trim()
    }).then(() => {
      ElMessage.success('已判定弃赛/判负')
      forfeitVisible.value = false
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
