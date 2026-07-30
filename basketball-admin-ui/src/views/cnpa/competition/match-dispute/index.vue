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
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDetail(scope.row.id)">详情</el-button>
          <el-button link type="success" v-if="scope.row.disputeStatus === 'PENDING'" @click="openCorrection(scope.row.id)" v-hasPermi="['basket:matchDispute:correct']">采纳并修正</el-button>
          <el-button link type="danger" v-if="scope.row.disputeStatus === 'PENDING'" @click="openReject(scope.row.id)" v-hasPermi="['basket:matchDispute:reject']">驳回</el-button>
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

    <el-dialog v-model="detailOpen" title="数据争议详情" width="920px" top="5vh">
      <div v-if="detail" v-loading="detailLoading">
        <el-descriptions :column="3" border size="small" class="mb8">
          <el-descriptions-item label="争议编号">{{ detail.dispute?.disputeNo }}</el-descriptions-item>
          <el-descriptions-item label="比赛编号">{{ detail.matchNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.dispute?.disputeStatus }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.dispute?.disputeType }}</el-descriptions-item>
          <el-descriptions-item label="比分">{{ detail.homeScore }} - {{ detail.awayScore }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ parseTime(detail.dispute?.disputeDeadlineTime) }}</el-descriptions-item>
          <el-descriptions-item label="争议说明" :span="3">{{ detail.dispute?.disputeReason }}</el-descriptions-item>
          <el-descriptions-item label="期望值" :span="3">{{ detail.dispute?.expectedValue || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">证据材料</el-divider>
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

        <el-divider content-position="left">处理记录</el-divider>
        <el-table :data="processingHistory" size="small" border max-height="200">
          <el-table-column label="结果" prop="result" width="100" />
          <el-table-column label="处理人" prop="operator" width="120" />
          <el-table-column label="时间" prop="time" width="170" />
          <el-table-column label="备注" prop="remark" />
          <el-table-column label="驳回原因" prop="rejectReason" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关闭</el-button>
        <el-button type="success" v-if="detail?.dispute?.disputeStatus === 'PENDING'" @click="openCorrection(detail.dispute.id)" v-hasPermi="['basket:matchDispute:correct']">采纳并修正</el-button>
        <el-button type="danger" v-if="detail?.dispute?.disputeStatus === 'PENDING'" @click="openReject(detail.dispute.id)" v-hasPermi="['basket:matchDispute:reject']">驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectOpen" title="驳回数据争议" width="480px" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="danger" @click="submitReject" :loading="rejecting">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="correctionOpen" title="采纳争议并修正数据" width="960px" top="5vh" append-to-body>
      <div v-loading="correctionLoading">
        <el-form label-width="96px" class="mb8">
          <el-form-item label="处理备注">
            <el-input v-model="correctionForm.handleRemark" type="textarea" :rows="2" placeholder="请填写处理备注" />
          </el-form-item>
          <el-form-item label="修正说明">
            <el-input v-model="correctionForm.correctionReason" type="textarea" :rows="2" placeholder="可选" />
          </el-form-item>
        </el-form>
        <el-descriptions :column="2" border size="small" class="mb8">
          <el-descriptions-item label="比分">
            <el-input-number v-model="correctionForm.homeScore" :min="0" controls-position="right" style="width: 100px" />
            :
            <el-input-number v-model="correctionForm.awayScore" :min="0" controls-position="right" style="width: 100px" />
          </el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">球员数据（可编辑助攻等）</el-divider>
        <el-table :data="correctionForm.playerStats" size="small" border max-height="320">
          <el-table-column label="球员ID" prop="playerId" width="160" />
          <el-table-column label="球队ID" prop="teamId" width="100" />
          <el-table-column label="得分" prop="points" width="80" />
          <el-table-column label="篮板" prop="rebounds" width="80" />
          <el-table-column label="助攻" width="100">
            <template #default="scope">
              <el-input-number v-model="scope.row.assists" :min="0" controls-position="right" size="small" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="correctionOpen = false">取消</el-button>
        <el-button type="success" @click="submitCorrection" :loading="correcting">提交修正</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listMatchDispute,
  createMatchDispute,
  getDisputeDetail,
  rejectMatchDispute,
  applyDisputeCorrection
} from '@/api/basket/matchDispute'
import { getMatchPlayerStat, getMatchTeamStat } from '@/api/basket/matchStat'
import { getMatchScore } from '@/api/basket/matchScore'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const query = reactive<{ matchId?: number; disputeStatus?: string }>({})
const createOpen = ref(false)
const form = reactive({ matchId: undefined as number | undefined, disputeType: 'SCORE', disputeReason: '' })

const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<any>(null)
const currentDisputeId = ref<number>()

const rejectOpen = ref(false)
const rejectReason = ref('')
const rejecting = ref(false)

const correctionOpen = ref(false)
const correctionLoading = ref(false)
const correcting = ref(false)
const correctionForm = reactive({
  handleRemark: '',
  correctionReason: '',
  homeScore: 0,
  awayScore: 0,
  periodScores: [] as any[],
  teamStats: [] as any[],
  playerStats: [] as any[]
})

const processingHistory = computed(() => {
  const d = detail.value?.dispute
  if (!d || d.disputeStatus === 'PENDING') return []
  return [{
    result: d.disputeStatus,
    operator: d.processedByName || d.handlerId || '-',
    time: parseTime(d.processedTime || d.handleTime),
    remark: d.handleRemark || d.processResult || '-',
    rejectReason: d.rejectReason || '-'
  }]
})

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

async function openDetail(id: number) {
  currentDisputeId.value = id
  detailOpen.value = true
  detailLoading.value = true
  try {
    const res = await getDisputeDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function openReject(id: number) {
  currentDisputeId.value = id
  rejectReason.value = ''
  rejectOpen.value = true
}

async function submitReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  if (!currentDisputeId.value) return
  rejecting.value = true
  try {
    await rejectMatchDispute(currentDisputeId.value, {
      handleRemark: rejectReason.value.trim(),
      handleResult: 'REJECTED'
    })
    ElMessage.success('已驳回')
    rejectOpen.value = false
    detailOpen.value = false
    loadList()
  } finally {
    rejecting.value = false
  }
}

async function openCorrection(id: number) {
  currentDisputeId.value = id
  correctionOpen.value = true
  correctionLoading.value = true
  correctionForm.handleRemark = ''
  correctionForm.correctionReason = ''
  try {
    const res = await getDisputeDetail(id)
    const d = res.data
    const matchId = d.dispute?.matchId
    correctionForm.homeScore = d.homeScore ?? 0
    correctionForm.awayScore = d.awayScore ?? 0
    if (matchId) {
      const [scoreRes, teamRes, playerRes] = await Promise.all([
        getMatchScore(matchId),
        getMatchTeamStat(matchId),
        getMatchPlayerStat(matchId)
      ])
      correctionForm.periodScores = (scoreRes.data?.periods || []).map((p: any) => ({
        periodNo: p.periodNo,
        periodType: p.periodType || 'REGULAR',
        homeScore: p.homeScore,
        awayScore: p.awayScore
      }))
      correctionForm.teamStats = (teamRes.data || []).map((t: any) => ({
        teamId: t.teamId,
        points: t.points,
        rebounds: t.rebounds,
        offensiveRebounds: t.offensiveRebounds,
        defensiveRebounds: t.defensiveRebounds,
        assists: t.assists,
        steals: t.steals,
        blocks: t.blocks,
        turnovers: t.turnovers,
        fouls: t.fouls,
        fieldGoalsMade: t.fieldGoalsMade,
        fieldGoalsAttempted: t.fieldGoalsAttempted,
        threePointsMade: t.threePointsMade,
        threePointsAttempted: t.threePointsAttempted,
        freeThrowsMade: t.freeThrowsMade,
        freeThrowsAttempted: t.freeThrowsAttempted
      }))
      correctionForm.playerStats = (playerRes.data || []).map((p: any) => ({
        teamId: p.teamId,
        playerId: p.playerId,
        points: p.points,
        rebounds: p.rebounds,
        offensiveRebounds: p.offensiveRebounds,
        defensiveRebounds: p.defensiveRebounds,
        assists: p.assists,
        steals: p.steals,
        blocks: p.blocks,
        turnovers: p.turnovers,
        fouls: p.fouls,
        fieldGoalsMade: p.fieldGoalsMade,
        fieldGoalsAttempted: p.fieldGoalsAttempted,
        threePointsMade: p.threePointsMade,
        threePointsAttempted: p.threePointsAttempted,
        freeThrowsMade: p.freeThrowsMade,
        freeThrowsAttempted: p.freeThrowsAttempted
      }))
    }
  } finally {
    correctionLoading.value = false
  }
}

async function submitCorrection() {
  if (!correctionForm.handleRemark.trim()) {
    ElMessage.warning('请填写处理备注')
    return
  }
  if (!currentDisputeId.value) return
  correcting.value = true
  try {
    await applyDisputeCorrection(currentDisputeId.value, {
      handleRemark: correctionForm.handleRemark.trim(),
      correctionReason: correctionForm.correctionReason.trim() || undefined,
      homeScore: correctionForm.homeScore,
      awayScore: correctionForm.awayScore,
      periodScores: correctionForm.periodScores,
      teamStats: correctionForm.teamStats,
      playerStats: correctionForm.playerStats
    })
    ElMessage.success('争议已采纳并完成数据修正')
    correctionOpen.value = false
    detailOpen.value = false
    loadList()
  } finally {
    correcting.value = false
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
