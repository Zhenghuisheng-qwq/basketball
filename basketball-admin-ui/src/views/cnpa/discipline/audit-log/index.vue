<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="业务审计记录"
      description="记录系统关键业务操作的审计日志，包括操作人、操作时间、业务类型、关联对象、修改前后数据和操作原因，用于问题追溯和责任确认。本页仅提供只读追溯，不提供业务审批或数据修改操作。"
    />

    <el-form :inline="true" class="mb8">
      <el-form-item label="业务类型">
        <el-input v-model="query.businessModule" clearable placeholder="如 DISCIPLINE" style="width: 160px" />
      </el-form-item>
      <el-form-item label="操作动作">
        <el-input v-model="query.operationType" clearable placeholder="如 ACTIVATE" style="width: 140px" />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="摘要/审计编号" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:businessAudit:list']">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="审计编号" prop="auditNo" min-width="150" />
      <el-table-column label="业务类型" prop="businessModule" width="120" />
      <el-table-column label="关联对象" min-width="140">
        <template #default="scope">{{ formatTarget(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="操作动作" prop="operationType" width="110" />
      <el-table-column label="操作人" prop="operatorName" width="100" />
      <el-table-column label="操作时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.operationTime) }}</template>
      </el-table-column>
      <el-table-column label="操作结果" prop="operationResult" width="100" />
      <el-table-column label="操作原因" prop="operationReason" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetail(scope.row.id)" v-hasPermi="['basket:businessAudit:query']">查看详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !rows.length" description="暂无审计记录" />

    <pagination :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-drawer v-model="detailOpen" title="业务审计详情" size="560px">
      <el-descriptions v-if="detail" :column="1" border class="mb8">
        <el-descriptions-item label="审计编号">{{ detail.auditNo }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ detail.businessModule }}</el-descriptions-item>
        <el-descriptions-item label="操作动作">{{ detail.operationType }}</el-descriptions-item>
        <el-descriptions-item label="关联对象">{{ formatTarget(detail) }}</el-descriptions-item>
        <el-descriptions-item label="变更摘要">{{ detail.changeSummary }}</el-descriptions-item>
        <el-descriptions-item label="操作原因">{{ detail.operationReason }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">字段变更明细</el-divider>
      <el-table :data="detail?.fields || []" border>
        <el-table-column label="字段" prop="fieldLabel" />
        <el-table-column label="修改前" prop="oldValue" show-overflow-tooltip />
        <el-table-column label="修改后" prop="newValue" show-overflow-tooltip />
      </el-table>
      <el-empty v-if="detail && !(detail.fields || []).length" description="暂无字段变更明细" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="BasketBusinessAudit">
import { ref, reactive, onMounted } from 'vue'
import { getBusinessAudit, listBusinessAudit } from '@/api/basket/businessAudit'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const query = reactive<{ pageNum: number; pageSize: number; businessModule?: string; operationType?: string; keyword?: string }>({
  pageNum: 1,
  pageSize: 10
})
const detailOpen = ref(false)
const detail = ref<any>(null)

function formatTarget(row: any) {
  if (!row) return '-'
  const type = row.targetType || '-'
  const id = row.targetId ?? '-'
  return `${type} #${id}`
}

async function loadList() {
  loading.value = true
  try {
    const res = await listBusinessAudit(query)
    rows.value = res.rows || []
    total.value = res.total ?? 0
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
