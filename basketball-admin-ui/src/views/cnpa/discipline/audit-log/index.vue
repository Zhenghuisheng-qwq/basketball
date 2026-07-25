<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="业务模块">
        <el-input v-model="query.businessModule" clearable placeholder="如 DISCIPLINE" style="width: 160px" />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-input v-model="query.operationType" clearable placeholder="如 ACTIVATE" style="width: 140px" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="摘要/编号" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:businessAudit:list']">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="审计编号" prop="auditNo" min-width="150" />
      <el-table-column label="模块" prop="businessModule" width="120" />
      <el-table-column label="操作类型" prop="operationType" width="110" />
      <el-table-column label="对象类型" prop="targetType" width="120" />
      <el-table-column label="对象ID" prop="targetId" width="90" />
      <el-table-column label="摘要" prop="changeSummary" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作人" prop="operatorName" width="100" />
      <el-table-column label="时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.operationTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetail(scope.row.id)" v-hasPermi="['basket:businessAudit:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailOpen" title="业务审计详情" size="560px">
      <el-descriptions v-if="detail" :column="1" border class="mb8">
        <el-descriptions-item label="审计编号">{{ detail.auditNo }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ detail.businessModule }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ detail.operationType }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ detail.changeSummary }}</el-descriptions-item>
        <el-descriptions-item label="原因">{{ detail.operationReason }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail?.fields || []" border>
        <el-table-column label="字段" prop="fieldLabel" />
        <el-table-column label="旧值" prop="oldValue" show-overflow-tooltip />
        <el-table-column label="新值" prop="newValue" show-overflow-tooltip />
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getBusinessAudit, listBusinessAudit } from '@/api/basket/businessAudit'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const query = reactive<{ businessModule?: string; operationType?: string; keyword?: string }>({})
const detailOpen = ref(false)
const detail = ref<any>(null)

async function loadList() {
  loading.value = true
  try {
    const res = await listBusinessAudit(query)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function showDetail(id: number) {
  const res = await getBusinessAudit(id)
  detail.value = res.data
  detailOpen.value = true
}

onMounted(loadList)
</script>
