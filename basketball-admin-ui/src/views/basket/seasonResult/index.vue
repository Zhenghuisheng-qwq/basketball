<template>
  <div class="app-container">
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="所属赛事">
        <el-select v-model="selectedTournamentId" placeholder="请选择赛事" filterable clearable style="width: 260px" @change="handleTournamentChange">
          <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="赛季">
        <el-select v-model="selectedSeasonId" placeholder="请选择赛季" filterable style="width: 260px" @change="loadDetail">
          <el-option v-for="item in seasonList" :key="item.seasonId" :label="item.seasonName" :value="item.seasonId" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadDetail" :disabled="!selectedSeasonId" v-hasPermi="['basket:seasonResult:query']">查询详情</el-button>
        <el-button type="warning" icon="CircleCheck" @click="handleEndCheck" :disabled="!selectedSeasonId" v-hasPermi="['basket:seasonResult:calculate']" :loading="checking">结束条件检查</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="listLoading" :data="seasonList" border class="mb8" highlight-current-row @current-change="handleRowChange">
      <el-table-column label="赛季" prop="seasonName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="赛季状态" prop="seasonStatus" width="110">
        <template #default="scope">
          <dict-tag :options="SEASON_STATUS_OPTIONS" :value="scope.row.seasonStatus" />
        </template>
      </el-table-column>
      <el-table-column label="名次状态" prop="resultStatusName" width="130" />
      <el-table-column label="冠军" prop="championTeamName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="亚军" prop="runnerUpTeamName" min-width="140" :show-overflow-tooltip="true" />
    </el-table>

    <template v-if="detail">
      <el-descriptions :column="3" border class="mb8">
        <el-descriptions-item label="赛事">{{ detail.tournamentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="名次状态">{{ detail.resultStatusName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="冠军">{{ detail.championTeamName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="亚军">{{ detail.runnerUpTeamName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="季军">{{ detail.thirdPlaceTeamName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="已确认比赛">{{ detail.confirmedMatchCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="未完成 / 重开">{{ detail.unfinishedMatchCount ?? 0 }} / {{ detail.reopenedMatchCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="已结束未确认">{{ detail.finishedUnconfirmedCount ?? 0 }}</el-descriptions-item>
      </el-descriptions>

      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" @click="openConfirm" :disabled="!selectedSeasonId" v-hasPermi="['basket:seasonResult:confirm']">确认名次</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" @click="handlePublish" :disabled="!selectedSeasonId" v-hasPermi="['basket:seasonResult:publish']">发布</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" @click="openRevoke" :disabled="!selectedSeasonId" v-hasPermi="['basket:seasonResult:revoke']">撤销</el-button>
        </el-col>
      </el-row>

      <el-alert
        v-if="endCheck"
        :title="endCheck.canConfirm ? '结束条件满足，可确认名次' : '结束条件未满足，暂不可确认'"
        :type="endCheck.canConfirm ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="mb8"
      />
      <el-alert
        v-for="(msg, idx) in endCheck?.blockers || []"
        :key="'b' + idx"
        :title="msg"
        type="error"
        :closable="false"
        class="mb8"
      />
      <el-alert
        v-for="(msg, idx) in endCheck?.warnings || []"
        :key="'w' + idx"
        :title="msg"
        type="warning"
        :closable="false"
        class="mb8"
      />

      <el-divider content-position="left">候选球队</el-divider>
      <el-table :data="candidates" border class="mb8">
        <el-table-column label="球队ID" prop="teamId" width="100" />
        <el-table-column label="球队名称" prop="teamName" min-width="180" />
        <el-table-column label="Logo" width="80">
          <template #default="scope">
            <el-image v-if="scope.row.logoUrl" :src="scope.row.logoUrl" style="width: 36px; height: 36px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-divider v-if="detail.honors?.length" content-position="left">关联荣誉</el-divider>
      <el-table v-if="detail.honors?.length" :data="detail.honors" border>
        <el-table-column label="荣誉" prop="honorTitle" min-width="140" />
        <el-table-column label="编码" prop="honorCode" width="140" />
        <el-table-column label="球队" prop="teamName" min-width="140" />
        <el-table-column label="状态" prop="displayStatusName" width="100" />
      </el-table>
    </template>

    <el-empty v-if="!detail && !detailLoading" description="请选择赛事与赛季后查询名次详情" />

    <el-dialog title="确认赛季名次" v-model="confirmOpen" width="560px" append-to-body>
      <el-form ref="confirmRef" :model="confirmForm" :rules="confirmRules" label-width="100px">
        <el-form-item label="冠军" prop="championTeamId">
          <el-select v-model="confirmForm.championTeamId" filterable clearable style="width: 100%">
            <el-option v-for="item in candidates" :key="'c' + item.teamId" :label="item.teamName" :value="item.teamId" />
          </el-select>
        </el-form-item>
        <el-form-item label="亚军" prop="runnerUpTeamId">
          <el-select v-model="confirmForm.runnerUpTeamId" filterable clearable style="width: 100%">
            <el-option v-for="item in candidates" :key="'r' + item.teamId" :label="item.teamName" :value="item.teamId" />
          </el-select>
        </el-form-item>
        <el-form-item label="季军">
          <el-select v-model="confirmForm.thirdPlaceTeamId" filterable clearable style="width: 100%">
            <el-option v-for="item in candidates" :key="'t' + item.teamId" :label="item.teamName" :value="item.teamId" />
          </el-select>
        </el-form-item>
        <el-form-item label="确认备注" :prop="endCheck?.requireConfirmRemark ? 'confirmRemark' : undefined">
          <el-input v-model="confirmForm.confirmRemark" type="textarea" :rows="3" placeholder="确认备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmOpen = false">取消</el-button>
        <el-button type="primary" @click="submitConfirm" :loading="confirming">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog title="撤销赛季名次" v-model="revokeOpen" width="480px" append-to-body>
      <el-form ref="revokeRef" :model="revokeForm" :rules="revokeRules" label-width="100px">
        <el-form-item label="撤销原因" prop="revokeReason">
          <el-input v-model="revokeForm.revokeReason" type="textarea" :rows="3" placeholder="请输入撤销原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeOpen = false">取消</el-button>
        <el-button type="danger" @click="submitRevoke" :loading="revoking">确认撤销</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketSeasonResult">
import {
  checkSeasonEnd,
  confirmSeasonResult,
  getSeasonResult,
  listSeasonCandidateTeam,
  listSeasonResult,
  publishSeasonResult,
  revokeSeasonResult
} from '@/api/basket/seasonResult'
import { listTournament } from '@/api/basket/tournament'
import type {
  SeasonCandidateTeam,
  SeasonEndCheck,
  SeasonResultConfirmReq,
  SeasonResultDetail,
  SeasonResultListItem,
  SeasonResultRevokeReq,
  TournamentListItem
} from '@/types'
import { SEASON_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const listLoading = ref(false)
const detailLoading = ref(false)
const checking = ref(false)
const confirming = ref(false)
const revoking = ref(false)
const tournamentOptions = ref<TournamentListItem[]>([])
const seasonList = ref<SeasonResultListItem[]>([])
const selectedTournamentId = ref<number>()
const selectedSeasonId = ref<number>()
const detail = ref<SeasonResultDetail>()
const endCheck = ref<SeasonEndCheck>()
const candidates = ref<SeasonCandidateTeam[]>([])
const confirmOpen = ref(false)
const revokeOpen = ref(false)

const confirmForm = ref<SeasonResultConfirmReq>({})
const revokeForm = ref<SeasonResultRevokeReq>({ revokeReason: '' })

const confirmRules = {
  championTeamId: [{ required: true, message: '请选择冠军球队', trigger: 'change' }],
  runnerUpTeamId: [{ required: true, message: '请选择亚军球队', trigger: 'change' }],
  confirmRemark: [{ required: true, message: '请填写确认备注', trigger: 'blur' }]
}
const revokeRules = {
  revokeReason: [{ required: true, message: '请填写撤销原因', trigger: 'blur' }]
}

function loadTournaments() {
  listTournament({ pageNum: 1, pageSize: 500 }).then(res => {
    tournamentOptions.value = res.rows || []
    if (tournamentOptions.value.length > 0) {
      selectedTournamentId.value = tournamentOptions.value[0].id
      loadSeasonList()
    }
  })
}

function loadSeasonList(keepSeasonId?: number) {
  detail.value = undefined
  endCheck.value = undefined
  candidates.value = []
  const preferId = keepSeasonId ?? selectedSeasonId.value
  selectedSeasonId.value = undefined
  seasonList.value = []
  if (!selectedTournamentId.value) return
  listLoading.value = true
  listSeasonResult(selectedTournamentId.value).then(res => {
    seasonList.value = res.data || []
    selectedSeasonId.value = seasonList.value.find(s => s.seasonId === preferId)?.seasonId
      || seasonList.value[0]?.seasonId
    if (selectedSeasonId.value) {
      loadDetail()
    }
  }).finally(() => {
    listLoading.value = false
  })
}

function handleTournamentChange() {
  loadSeasonList()
}

function handleRowChange(row?: SeasonResultListItem) {
  if (!row?.seasonId) return
  selectedSeasonId.value = row.seasonId
  loadDetail()
}

function loadDetail() {
  if (!selectedSeasonId.value) return
  detailLoading.value = true
  endCheck.value = undefined
  Promise.all([
    getSeasonResult(selectedSeasonId.value),
    listSeasonCandidateTeam(selectedSeasonId.value)
  ]).then(([detailRes, candidateRes]) => {
    detail.value = detailRes.data
    candidates.value = candidateRes.data || []
  }).finally(() => {
    detailLoading.value = false
  })
}

function handleEndCheck() {
  if (!selectedSeasonId.value) return
  checking.value = true
  checkSeasonEnd(selectedSeasonId.value).then(res => {
    endCheck.value = res.data
    if (res.data?.canConfirm) {
      proxy.$modal.msgSuccess('结束条件满足')
    } else {
      proxy.$modal.msgWarning('结束条件未满足')
    }
  }).finally(() => {
    checking.value = false
  })
}

function openConfirm() {
  if (!selectedSeasonId.value) return
  confirmForm.value = {
    championTeamId: detail.value?.championTeamId,
    runnerUpTeamId: detail.value?.runnerUpTeamId,
    thirdPlaceTeamId: detail.value?.thirdPlaceTeamId,
    confirmRemark: undefined
  }
  const ensureCandidates = candidates.value.length
    ? Promise.resolve()
    : listSeasonCandidateTeam(selectedSeasonId.value).then(res => {
      candidates.value = res.data || []
    })
  const ensureCheck = endCheck.value
    ? Promise.resolve()
    : checkSeasonEnd(selectedSeasonId.value).then(res => {
      endCheck.value = res.data
    })
  Promise.all([ensureCandidates, ensureCheck]).then(() => {
    confirmOpen.value = true
  })
}

function submitConfirm() {
  proxy.$refs.confirmRef.validate((valid: boolean) => {
    if (!valid || !selectedSeasonId.value) return
    proxy.$modal.confirm('确认后将写入赛季冠亚军并生成荣誉草稿，是否继续？').then(() => {
      confirming.value = true
      return confirmSeasonResult(selectedSeasonId.value!, confirmForm.value)
    }).then(() => {
      proxy.$modal.msgSuccess('确认成功')
      confirmOpen.value = false
      loadSeasonList(selectedSeasonId.value)
    }).catch(() => {}).finally(() => {
      confirming.value = false
    })
  })
}

function handlePublish() {
  if (!selectedSeasonId.value) return
  proxy.$modal.confirm('发布后将对官网可见，确认发布？').then(() =>
    publishSeasonResult(selectedSeasonId.value!)
  ).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    loadSeasonList(selectedSeasonId.value)
  }).catch(() => {})
}

function openRevoke() {
  revokeForm.value = { revokeReason: '' }
  revokeOpen.value = true
}

function submitRevoke() {
  proxy.$refs.revokeRef.validate((valid: boolean) => {
    if (!valid || !selectedSeasonId.value) return
    proxy.$modal.confirm('撤销将回退已确认/已发布名次，确认继续？').then(() => {
      revoking.value = true
      return revokeSeasonResult(selectedSeasonId.value!, revokeForm.value)
    }).then(() => {
      proxy.$modal.msgSuccess('撤销成功')
      revokeOpen.value = false
      loadSeasonList(selectedSeasonId.value)
    }).catch(() => {}).finally(() => {
      revoking.value = false
    })
  })
}

loadTournaments()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
