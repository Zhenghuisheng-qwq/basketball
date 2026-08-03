<template>
  <div class="app-container">
    <el-alert title="V4 赛程管理：基于 Division EventTeam 生成单循环草稿赛程，确认后发布" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="query" :inline="true" class="mb8">
      <el-form-item label="Season ID"><el-input-number v-model="query.seasonId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="Division ID"><el-input-number v-model="query.divisionId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="query.publishStatus" clearable placeholder="全部" style="width:120px">
          <el-option label="DRAFT" value="DRAFT" />
          <el-option label="PUBLISHED" value="PUBLISHED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
        <el-button type="success" icon="Plus" @click="openGenerate">生成草稿</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="Season" prop="seasonTitle" min-width="120" />
      <el-table-column label="Division" prop="divisionName" min-width="120" />
      <el-table-column label="平台" prop="platformCode" width="70" />
      <el-table-column label="赛制" prop="formatType" width="140" />
      <el-table-column label="队数" prop="teamCount" width="60" />
      <el-table-column label="场数" prop="matchCount" width="60" />
      <el-table-column label="生成状态" prop="generateStatus" width="90" />
      <el-table-column label="发布状态" prop="publishStatus" width="90" />
      <el-table-column label="生成时间" prop="generateTime" width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="viewDetail(scope.row)">详情</el-button>
          <el-button link type="success" v-if="scope.row.publishStatus === 'DRAFT'" @click="handlePublish(scope.row)">发布</el-button>
          <el-button link type="danger" v-if="scope.row.publishStatus === 'DRAFT'" @click="handleCancel(scope.row)">作废</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="generateVisible" title="生成草稿赛程" width="480px">
      <el-form :model="generateForm" label-width="100px">
        <el-form-item label="Division ID" required><el-input-number v-model="generateForm.divisionId" :min="1" controls-position="right" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="generateForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGenerate">确认生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="批次详情" size="60%">
      <el-descriptions v-if="detail" :column="2" border size="small" class="mb8">
        <el-descriptions-item label="批次ID">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="批次号">{{ detail.batchNo }}</el-descriptions-item>
        <el-descriptions-item label="Division">{{ detail.divisionName }}</el-descriptions-item>
        <el-descriptions-item label="队数/场数">{{ detail.teamCount }} / {{ detail.matchCount }}</el-descriptions-item>
        <el-descriptions-item label="规则">{{ detail.ruleCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.publishStatus }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.matches || []" border size="small">
        <el-table-column label="轮次" prop="roundNo" width="60" />
        <el-table-column label="编号" prop="matchNo" min-width="130" />
        <el-table-column label="时间" prop="matchTime" width="160" />
        <el-table-column label="主队" prop="homeTeamName" min-width="100" />
        <el-table-column label="客队" prop="awayTeamName" min-width="100" />
        <el-table-column label="状态" prop="matchStatus" width="90" />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Schedule">
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  cancelV4ScheduleBatch, generateV4ScheduleBatch, getV4ScheduleBatch,
  listV4ScheduleBatch, publishV4ScheduleBatch,
  type BasketScheduleBatchDetailVo, type BasketScheduleBatchQuery
} from '@/api/cnpa/event-v4/schedule-batch'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<BasketScheduleBatchQuery>({ pageNum: 1, pageSize: 10 })
const generateVisible = ref(false)
const generateForm = reactive({ divisionId: undefined as number | undefined, remark: '' })
const detailVisible = ref(false)
const detail = ref<BasketScheduleBatchDetailVo | null>(null)

function loadList() {
  loading.value = true
  listV4ScheduleBatch(query).then(res => {
    list.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function openGenerate() {
  generateForm.divisionId = query.divisionId
  generateForm.remark = ''
  generateVisible.value = true
}

function submitGenerate() {
  if (!generateForm.divisionId) {
    ElMessage.warning('请填写 Division ID')
    return
  }
  generateV4ScheduleBatch({ divisionId: generateForm.divisionId, remark: generateForm.remark }).then(res => {
    ElMessage.success('生成成功，批次ID=' + res.data)
    generateVisible.value = false
    loadList()
  })
}

function viewDetail(row: any) {
  getV4ScheduleBatch(row.id).then(res => {
    detail.value = res.data
    detailVisible.value = true
  })
}

function handlePublish(row: any) {
  ElMessageBox.confirm('确认发布该赛程？发布后 Division 将变为 SCHEDULED', '提示').then(() => {
    publishV4ScheduleBatch(row.id).then(() => {
      ElMessage.success('发布成功')
      loadList()
    })
  })
}

function handleCancel(row: any) {
  ElMessageBox.confirm('确认作废该草稿批次？关联 DRAFT 比赛将一并删除', '提示', { type: 'warning' }).then(() => {
    cancelV4ScheduleBatch(row.id).then(() => {
      ElMessage.success('已作废')
      loadList()
    })
  })
}

loadList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
