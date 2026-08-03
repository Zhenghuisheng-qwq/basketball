<template>
  <div class="app-container">
    <el-alert title="V4 比赛列表：查询/编辑草稿比赛时间、场地、备注" type="info" :closable="false" show-icon class="mb8" />

    <el-form :model="query" :inline="true" class="mb8">
      <el-form-item label="Season ID"><el-input-number v-model="query.seasonId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="Division ID"><el-input-number v-model="query.divisionId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="批次ID"><el-input-number v-model="query.scheduleBatchId" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="轮次"><el-input-number v-model="query.roundNo" :min="1" controls-position="right" /></el-form-item>
      <el-form-item label="发布状态">
        <el-select v-model="query.publishStatus" clearable style="width:120px">
          <el-option label="DRAFT" value="DRAFT" />
          <el-option label="PUBLISHED" value="PUBLISHED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="轮次" prop="roundNo" width="60" />
      <el-table-column label="编号" prop="matchNo" min-width="130" />
      <el-table-column label="时间" prop="matchTime" width="160" />
      <el-table-column label="主队" prop="homeTeamName" min-width="100" />
      <el-table-column label="客队" prop="awayTeamName" min-width="100" />
      <el-table-column label="Division" prop="divisionName" min-width="110" />
      <el-table-column label="状态" prop="matchStatus" width="90" />
      <el-table-column label="发布" prop="publishStatus" width="90" />
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteV4Match, listV4Match, updateV4Match } from '@/api/cnpa/event-v4/schedule-batch'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const query = reactive<Record<string, any>>({ pageNum: 1, pageSize: 10 })
const editVisible = ref(false)
const editId = ref<number>()
const editForm = reactive({ matchTime: '', venueText: '', remark: '' })

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

loadList()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
