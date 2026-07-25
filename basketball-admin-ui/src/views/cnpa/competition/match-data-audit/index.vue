<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="赛季ID">
        <el-input-number v-model="query.seasonId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item label="数据状态">
        <el-select v-model="query.dataStatus" clearable style="width: 140px">
          <el-option label="已提交" :value="1" />
          <el-option label="已驳回" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:matchAudit:list']">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="比赛编号" prop="matchNo" min-width="120" />
      <el-table-column label="赛季" prop="seasonName" min-width="120" />
      <el-table-column label="阶段" prop="stageName" width="100" />
      <el-table-column label="主队" prop="homeTeamName" min-width="120" />
      <el-table-column label="客队" prop="awayTeamName" min-width="120" />
      <el-table-column label="比分" width="100">
        <template #default="scope">{{ scope.row.homeScore }} - {{ scope.row.awayScore }}</template>
      </el-table-column>
      <el-table-column label="数据状态" prop="dataStatusName" width="100" />
      <el-table-column label="提交时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="goEntry(scope.row.matchId)">录入/审核</el-button>
          <el-button link type="danger" @click="openReject(scope.row)" v-hasPermi="['basket:matchAudit:reject']">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="rejectOpen" title="驳回比赛数据" width="480px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="rejecting">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPendingMatchAudit } from '@/api/basket/matchAudit'
import { rejectMatchResult } from '@/api/basket/matchStat'
import { parseTime } from '@/utils/ruoyi'

const router = useRouter()
const loading = ref(false)
const rows = ref<any[]>([])
const query = reactive({ seasonId: undefined as number | undefined, dataStatus: 1 })
const rejectOpen = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)
const rejectTarget = ref<any>(null)

async function loadList() {
  loading.value = true
  try {
    const res = await listPendingMatchAudit({ ...query })
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

function goEntry(matchId: number) {
  router.push({ path: '/cnpa/competition/match-data-entry', query: { matchId: String(matchId) } })
}

function openReject(row: any) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  rejecting.value = true
  try {
    await rejectMatchResult(rejectTarget.value.matchId, { reason: rejectReason.value.trim() })
    ElMessage.success('已驳回')
    rejectOpen.value = false
    loadList()
  } finally {
    rejecting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
