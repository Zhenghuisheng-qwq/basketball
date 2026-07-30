<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="赛季ID">
        <el-input-number v-model="query.seasonId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item label="比赛ID">
        <el-input-number v-model="query.matchId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item label="申请状态">
        <el-select v-model="query.applicationStatus" clearable style="width: 140px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="异常类型">
        <el-select v-model="query.exceptionType" clearable style="width: 140px">
          <el-option label="弃赛" value="FORFEIT" />
          <el-option label="延期" value="POSTPONE" />
          <el-option label="取消" value="CANCEL" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:matchException:list']">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" border>
      <el-table-column label="申请编号" prop="applicationNo" min-width="140" />
      <el-table-column label="比赛编号" prop="matchNo" width="120" />
      <el-table-column label="异常类型" prop="exceptionType" width="100" />
      <el-table-column label="状态" prop="applicationStatus" width="100" />
      <el-table-column label="裁定类型" prop="decisionType" width="120" />
      <el-table-column label="说明" prop="exceptionReason" min-width="200" show-overflow-tooltip />
      <el-table-column label="申请时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button>
          <el-button link type="success" v-if="scope.row.applicationStatus === 'PENDING'" @click="openDecide(scope.row.id)" v-hasPermi="['basket:matchException:decide']">裁定</el-button>
          <el-button link type="danger" v-if="scope.row.applicationStatus === 'PENDING'" @click="openReject(scope.row.id)" v-hasPermi="['basket:matchException:reject']">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="detailOpen" title="异常比赛申请详情" width="920px" top="5vh">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small" class="mb8">
          <el-descriptions-item label="申请编号">{{ detail.application?.applicationNo }}</el-descriptions-item>
          <el-descriptions-item label="比赛编号">{{ detail.matchNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.application?.applicationStatus }}</el-descriptions-item>
          <el-descriptions-item label="异常类型">{{ detail.application?.exceptionType }}</el-descriptions-item>
          <el-descriptions-item label="申请球队ID">{{ detail.application?.applicantTeamId }}</el-descriptions-item>
          <el-descriptions-item label="对方球队ID">{{ detail.application?.opponentTeamId }}</el-descriptions-item>
          <el-descriptions-item label="异常说明" :span="3">{{ detail.application?.exceptionReason }}</el-descriptions-item>
          <el-descriptions-item label="裁定类型">{{ detail.application?.decisionType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="裁定说明" :span="2">{{ detail.application?.decisionResult || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">证明材料</el-divider>
        <div class="material-preview-list">
          <div v-for="m in detail.materials || []" :key="m.fileId" class="material-preview-item">
            <p>{{ m.materialType }} · {{ m.materialName || m.fileId }}</p>
            <el-image
              v-if="m.fileUrl"
              :src="m.fileUrl"
              :preview-src-list="[m.fileUrl]"
              fit="cover"
              style="width: 160px; height: 100px"
            />
            <span v-else class="text-muted">fileId: {{ m.fileId }}</span>
          </div>
          <span v-if="!(detail.materials || []).length" class="text-muted">暂无材料</span>
        </div>

        <el-divider content-position="left">审核记录</el-divider>
        <el-table :data="detail.audits || []" size="small" border max-height="200">
          <el-table-column label="动作" prop="auditAction" width="100" />
          <el-table-column label="前状态" prop="beforeStatus" width="90" />
          <el-table-column label="后状态" prop="afterStatus" width="90" />
          <el-table-column label="操作人" prop="operatorName" width="120" />
          <el-table-column label="时间" width="170">
            <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" prop="auditRemark" />
          <el-table-column label="驳回原因" prop="rejectReason" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关闭</el-button>
        <el-button type="success" v-if="detail?.application?.applicationStatus === 'PENDING'" @click="openDecide(detail.application.id)" v-hasPermi="['basket:matchException:decide']">裁定</el-button>
        <el-button type="danger" v-if="detail?.application?.applicationStatus === 'PENDING'" @click="openReject(detail.application.id)" v-hasPermi="['basket:matchException:reject']">驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectOpen" title="驳回异常申请" width="480px" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="rejecting">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="decideOpen" title="异常比赛裁定" width="560px" append-to-body>
      <el-form label-width="108px">
        <el-form-item label="裁定类型">
          <el-select v-model="decideForm.decisionType" style="width: 100%">
            <el-option label="弃赛(FORFEIT)" value="FORFEIT" />
            <el-option label="延期(POSTPONE)" value="POSTPONE" />
            <el-option label="取消(CANCEL)" value="CANCEL" />
            <el-option label="维持原结果(KEEP_RESULT)" value="KEEP_RESULT" />
            <el-option label="重赛(REMATCH)" value="REMATCH" />
            <el-option label="管理员裁定(ADMIN_DECISION)" value="ADMIN_DECISION" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="decideForm.decisionType === 'FORFEIT'" label="弃赛方球队ID">
          <el-input-number v-model="decideForm.walkoverTeamId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="decideForm.decisionType === 'ADMIN_DECISION'" label="主队比分">
          <el-input-number v-model="decideForm.decisionScoreHome" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item v-if="decideForm.decisionType === 'ADMIN_DECISION'" label="客队比分">
          <el-input-number v-model="decideForm.decisionScoreAway" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="裁定说明">
          <el-input v-model="decideForm.decisionResult" type="textarea" :rows="3" placeholder="请填写裁定说明" />
        </el-form-item>
        <el-form-item label="审核备注">
          <el-input v-model="decideForm.auditRemark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="decideOpen = false">取消</el-button>
        <el-button type="primary" @click="submitDecide" :loading="deciding">确认裁定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listMatchException,
  getExceptionDetail,
  rejectMatchException,
  decideMatchException
} from '@/api/basket/matchException'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const query = reactive<{
  seasonId?: number
  matchId?: number
  applicationStatus?: string
  exceptionType?: string
}>({ applicationStatus: 'PENDING' })

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const currentApplicationId = ref<number>()

const rejectOpen = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)

const decideOpen = ref(false)
const deciding = ref(false)
const decideForm = reactive({
  decisionType: 'FORFEIT',
  decisionResult: '',
  auditRemark: '',
  walkoverTeamId: undefined as number | undefined,
  decisionScoreHome: undefined as number | undefined,
  decisionScoreAway: undefined as number | undefined,
  winnerTeamId: undefined as number | undefined
})

async function loadList() {
  loading.value = true
  try {
    const res = await listMatchException(query)
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  currentApplicationId.value = id
  detailOpen.value = true
  detailLoading.value = true
  try {
    const res = await getExceptionDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function openReject(id: number) {
  currentApplicationId.value = id
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!currentApplicationId.value) return
  rejecting.value = true
  try {
    await rejectMatchException(currentApplicationId.value, {
      rejectReason: rejectReason.value.trim(),
      auditRemark: rejectReason.value.trim()
    })
    ElMessage.success('已驳回')
    rejectOpen.value = false
    detailOpen.value = false
    loadList()
  } finally {
    rejecting.value = false
  }
}

function openDecide(id: number) {
  currentApplicationId.value = id
  decideForm.decisionType = 'FORFEIT'
  decideForm.decisionResult = ''
  decideForm.auditRemark = ''
  decideForm.walkoverTeamId = detail.value?.application?.applicantTeamId
  decideForm.decisionScoreHome = undefined
  decideForm.decisionScoreAway = undefined
  decideForm.winnerTeamId = undefined
  decideOpen.value = true
}

async function submitDecide() {
  if (!decideForm.decisionResult.trim()) {
    ElMessage.warning('请填写裁定说明')
    return
  }
  if (!currentApplicationId.value) return
  deciding.value = true
  try {
    const payload: Record<string, unknown> = {
      decisionType: decideForm.decisionType,
      decisionResult: decideForm.decisionResult.trim(),
      auditRemark: decideForm.auditRemark.trim() || undefined
    }
    if (decideForm.decisionType === 'FORFEIT') {
      payload.walkoverTeamId = decideForm.walkoverTeamId
    }
    if (decideForm.decisionType === 'ADMIN_DECISION') {
      payload.correctionData = {
        homeScore: decideForm.decisionScoreHome,
        awayScore: decideForm.decisionScoreAway,
        periodScores: [],
        teamStats: [],
        playerStats: []
      }
      payload.winnerTeamId = decideForm.winnerTeamId
    }
    await decideMatchException(currentApplicationId.value, payload)
    ElMessage.success('裁定完成')
    decideOpen.value = false
    detailOpen.value = false
    loadList()
  } finally {
    deciding.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.material-preview-list { display: flex; flex-wrap: wrap; gap: 12px; }
.material-preview-item { border: 1px solid #eee; padding: 8px; border-radius: 4px; }
.text-muted { color: #999; font-size: 12px; }
</style>
