<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="比赛ID">
        <el-input-number v-model="query.matchId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.disputeStatus" clearable style="width: 140px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已成立" value="UPHELD" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:matchDispute:list']">查询</el-button>
        <el-button type="success" icon="Plus" @click="openCreate" v-hasPermi="['basket:matchDispute:add']">发起争议</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="争议编号" prop="disputeNo" min-width="140" />
      <el-table-column label="比赛编号" prop="matchNo" width="120" />
      <el-table-column label="类型" prop="disputeType" width="120" />
      <el-table-column label="状态" prop="disputeStatus" width="100" />
      <el-table-column label="说明" prop="disputeReason" min-width="200" show-overflow-tooltip />
      <el-table-column label="发起时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="success" @click="handle(scope.row, 'uphold')" v-if="scope.row.disputeStatus === 'PENDING'" v-hasPermi="['basket:matchDispute:handle']">成立</el-button>
          <el-button link type="danger" @click="handle(scope.row, 'reject')" v-if="scope.row.disputeStatus === 'PENDING'" v-hasPermi="['basket:matchDispute:handle']">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createOpen" title="发起数据争议" width="520px">
      <el-form label-width="96px">
        <el-form-item label="比赛ID"><el-input-number v-model="form.matchId" :min="1" controls-position="right" /></el-form-item>
        <el-form-item label="争议类型">
          <el-select v-model="form.disputeType" style="width: 100%">
            <el-option label="比分" value="SCORE" />
            <el-option label="球员统计" value="PLAYER_STAT" />
            <el-option label="球队统计" value="TEAM_STAT" />
            <el-option label="比赛结果" value="RESULT" />
          </el-select>
        </el-form-item>
        <el-form-item label="争议说明"><el-input v-model="form.disputeReason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMatchDispute, createMatchDispute, rejectMatchDispute, upholdMatchDispute } from '@/api/basket/matchDispute'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const query = reactive<{ matchId?: number; disputeStatus?: string }>({})
const createOpen = ref(false)
const form = reactive({ matchId: undefined as number | undefined, disputeType: 'SCORE', disputeReason: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await listMatchDispute(query)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.matchId = query.matchId
  form.disputeReason = ''
  createOpen.value = true
}

async function submitCreate() {
  if (!form.matchId || !form.disputeReason.trim()) {
    ElMessage.warning('请填写完整信息')
    return
  }
  await createMatchDispute({ ...form })
  ElMessage.success('争议已提交')
  createOpen.value = false
  loadList()
}

async function handle(row: any, action: 'uphold' | 'reject') {
  const { value } = await ElMessageBox.prompt('请填写处理备注', '处理争议', { inputType: 'textarea' })
  const payload = { handleRemark: value, handleResult: action === 'uphold' ? 'UPHELD' : 'REJECTED' }
  if (action === 'uphold') await upholdMatchDispute(row.id, payload)
  else await rejectMatchDispute(row.id, payload)
  ElMessage.success('处理完成')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
