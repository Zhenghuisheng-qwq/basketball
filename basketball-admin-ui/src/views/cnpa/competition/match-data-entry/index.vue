<template>
  <div class="app-container">
    <el-alert type="warning" title="旧域已废弃" description="请使用「比赛列表」菜单。旧比赛数据录入 R4 起不再维护。" :closable="false" show-icon class="mb8" />
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="比赛数据录入"
      description="管理员兜底代录完整比赛数据，包含比分、球队数据与球员数据。常规流程请优先使用比赛数据审核。"
    />
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="比赛ID">
        <el-input-number v-model="matchId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadStats" :disabled="!matchId">加载数据</el-button>
      </el-form-item>
    </el-form>

    <el-row v-if="matchId" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" @click="saveTeamStats" v-hasPermi="['basket:matchStat:edit']" :loading="teamSaving">保存球队数据</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="submitTeamStats" v-hasPermi="['basket:matchStat:submit']">提交球队数据</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" @click="savePlayerStats" v-hasPermi="['basket:matchStat:edit']" :loading="playerSaving">保存球员数据</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="submitPlayerStats" v-hasPermi="['basket:matchStat:submit']">提交球员数据</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" @click="confirmResult" v-hasPermi="['basket:matchStat:confirm']">确认结果</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" @click="openReopen" v-hasPermi="['basket:matchStat:reopen']">重开结果</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Document" @click="loadAuditLog" v-hasPermi="['basket:matchStat:audit']">审计日志</el-button>
      </el-col>
    </el-row>

    <el-divider content-position="left">球队技术统计</el-divider>
    <el-table :data="teamStats" v-loading="loading" size="small" border>
      <el-table-column label="球队" prop="teamName" min-width="120" fixed />
      <el-table-column label="得分" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.points" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="篮板" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.rebounds" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="助攻" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.assists" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="抢断" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.steals" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="盖帽" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.blocks" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="失误" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.turnovers" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="犯规" width="90">
        <template #default="scope"><el-input-number v-model="scope.row.fouls" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="状态" prop="dataStatusName" width="90" />
    </el-table>

    <el-divider content-position="left">球员技术统计</el-divider>
    <el-table :data="playerStats" v-loading="loading" size="small" border max-height="480">
      <el-table-column label="球队" prop="teamName" width="100" fixed />
      <el-table-column label="球员" prop="playerName" width="100" fixed />
      <el-table-column label="首发" width="70">
        <template #default="scope"><el-switch v-model="scope.row.startedFlag" :active-value="1" :inactive-value="0" size="small" /></template>
      </el-table-column>
      <el-table-column label="时间(秒)" width="100">
        <template #default="scope"><el-input-number v-model="scope.row.minutesSeconds" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="得分" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.points" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="篮板" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.rebounds" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="助攻" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.assists" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="抢断" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.steals" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="盖帽" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.blocks" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="失误" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.turnovers" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
      <el-table-column label="犯规" width="80">
        <template #default="scope"><el-input-number v-model="scope.row.fouls" :min="0" controls-position="right" size="small" /></template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="auditOpen" title="比赛数据审计日志" size="760px" append-to-body>
      <el-divider content-position="left">状态变更</el-divider>
      <el-table :data="auditLog?.statusLogs || []" size="small">
        <el-table-column label="变更前" prop="beforeStatusName" width="90" />
        <el-table-column label="变更后" prop="afterStatusName" width="90" />
        <el-table-column label="类型" prop="changeTypeName" width="100" />
        <el-table-column label="原因" prop="changeReason" min-width="120" />
        <el-table-column label="操作人" prop="operatorName" width="90" />
        <el-table-column label="时间" prop="changeTime" width="160">
          <template #default="scope">{{ parseTime(scope.row.changeTime) }}</template>
        </el-table-column>
      </el-table>
      <el-divider content-position="left">数据变更</el-divider>
      <el-table :data="auditLog?.statChanges || []" size="small">
        <el-table-column label="字段" prop="fieldName" width="100" />
        <el-table-column label="变更前" prop="beforeValue" min-width="100" />
        <el-table-column label="变更后" prop="afterValue" min-width="100" />
        <el-table-column label="原因" prop="changeReason" min-width="120" />
        <el-table-column label="操作人" prop="operatorName" width="90" />
        <el-table-column label="时间" prop="createTime" width="160">
          <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog title="重开比赛结果" v-model="reopenOpen" width="480px" append-to-body>
      <el-form ref="reopenRef" :model="reopenForm" :rules="reopenRules" label-width="88px">
        <el-form-item label="重开原因" prop="reason">
          <el-input v-model="reopenForm.reason" type="textarea" placeholder="请输入重开原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reopenOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReopen">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketMatchStat">
import {
  confirmMatchResult,
  getMatchAuditLog,
  getMatchPlayerStat,
  getMatchTeamStat,
  reopenMatchResult,
  saveMatchPlayerStat,
  saveMatchTeamStat,
  submitMatchPlayerStat,
  submitMatchTeamStat
} from '@/api/basket/matchStat'
import type {
  MatchAuditLog,
  MatchPlayerStat,
  MatchReopenReq,
  MatchTeamStat
} from '@/types'

const { proxy } = getCurrentInstance()
const route = useRoute()

const matchId = ref<number>()
const loading = ref(false)
const teamSaving = ref(false)
const playerSaving = ref(false)
const teamStats = ref<MatchTeamStat[]>([])
const playerStats = ref<MatchPlayerStat[]>([])

const auditOpen = ref(false)
const auditLog = ref<MatchAuditLog>()
const reopenOpen = ref(false)
const reopenForm = ref<MatchReopenReq>({ reason: '' })
const reopenRules = { reason: [{ required: true, message: '重开原因不能为空', trigger: 'blur' }] }

function loadStats() {
  if (!matchId.value) return
  loading.value = true
  Promise.all([
    getMatchTeamStat(matchId.value),
    getMatchPlayerStat(matchId.value)
  ]).then(([teamRes, playerRes]) => {
    teamStats.value = (teamRes.data || []).map(r => ({ ...r }))
    playerStats.value = (playerRes.data || []).map(r => ({ ...r }))
  }).finally(() => {
    loading.value = false
  })
}

function saveTeamStats() {
  if (!matchId.value) return
  teamSaving.value = true
  saveMatchTeamStat(matchId.value, {
    stats: teamStats.value.map(r => ({
      teamId: r.teamId,
      points: r.points,
      rebounds: r.rebounds,
      offensiveRebounds: r.offensiveRebounds,
      defensiveRebounds: r.defensiveRebounds,
      assists: r.assists,
      steals: r.steals,
      blocks: r.blocks,
      turnovers: r.turnovers,
      fouls: r.fouls,
      fieldGoalsMade: r.fieldGoalsMade,
      fieldGoalsAttempted: r.fieldGoalsAttempted,
      threePointsMade: r.threePointsMade,
      threePointsAttempted: r.threePointsAttempted,
      freeThrowsMade: r.freeThrowsMade,
      freeThrowsAttempted: r.freeThrowsAttempted,
      possessions: r.possessions,
      efficiency: r.efficiency
    }))
  }).then(() => {
    proxy.$modal.msgSuccess('球队数据已保存')
    loadStats()
  }).finally(() => {
    teamSaving.value = false
  })
}

function submitTeamStats() {
  if (!matchId.value) return
  proxy.$modal.confirm('确认提交球队技术统计？').then(() => submitMatchTeamStat(matchId.value!)).then(() => {
    proxy.$modal.msgSuccess('球队数据已提交')
    loadStats()
  }).catch(() => {})
}

function savePlayerStats() {
  if (!matchId.value) return
  playerSaving.value = true
  saveMatchPlayerStat(matchId.value, {
    stats: playerStats.value.map(r => ({
      teamId: r.teamId,
      playerId: r.playerId,
      rosterId: r.rosterId,
      startedFlag: r.startedFlag,
      minutesSeconds: r.minutesSeconds,
      points: r.points,
      rebounds: r.rebounds,
      offensiveRebounds: r.offensiveRebounds,
      defensiveRebounds: r.defensiveRebounds,
      assists: r.assists,
      steals: r.steals,
      blocks: r.blocks,
      turnovers: r.turnovers,
      fouls: r.fouls,
      fieldGoalsMade: r.fieldGoalsMade,
      fieldGoalsAttempted: r.fieldGoalsAttempted,
      threePointsMade: r.threePointsMade,
      threePointsAttempted: r.threePointsAttempted,
      freeThrowsMade: r.freeThrowsMade,
      freeThrowsAttempted: r.freeThrowsAttempted,
      plusMinus: r.plusMinus,
      efficiency: r.efficiency
    }))
  }).then(() => {
    proxy.$modal.msgSuccess('球员数据已保存')
    loadStats()
  }).finally(() => {
    playerSaving.value = false
  })
}

function submitPlayerStats() {
  if (!matchId.value) return
  proxy.$modal.confirm('确认提交球员技术统计？').then(() => submitMatchPlayerStat(matchId.value!)).then(() => {
    proxy.$modal.msgSuccess('球员数据已提交')
    loadStats()
  }).catch(() => {})
}

function confirmResult() {
  if (!matchId.value) return
  proxy.$modal.confirm('确认比赛结果与技术统计？').then(() => confirmMatchResult(matchId.value!)).then(() => {
    proxy.$modal.msgSuccess('结果已确认')
    loadStats()
  }).catch(() => {})
}

function openReopen() {
  reopenForm.value = { reason: '' }
  reopenOpen.value = true
}

function submitReopen() {
  proxy.$refs.reopenRef.validate((valid: boolean) => {
    if (!valid || !matchId.value) return
    reopenMatchResult(matchId.value, reopenForm.value).then(() => {
      proxy.$modal.msgSuccess('结果已重开')
      reopenOpen.value = false
      loadStats()
    })
  })
}

function loadAuditLog() {
  if (!matchId.value) return
  getMatchAuditLog(matchId.value).then(res => {
    auditLog.value = res.data
    auditOpen.value = true
  })
}

if (route.query.matchId) {
  matchId.value = Number(route.query.matchId)
  loadStats()
}
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
