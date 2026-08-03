<template>
  <div class="app-container">
    <el-alert title="[已废弃] 旧比分录入入口，请使用 V4 比赛数据提交（Web API /basket/v4/player/match-submission）" type="warning" :closable="false" show-icon class="mb8" />
    <el-form :inline="true" label-width="88px" class="mb8">
      <el-form-item label="比赛ID">
        <el-input-number v-model="matchId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadScore" :disabled="!matchId">加载比分</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="scoreData" :column="4" border class="mb8">
      <el-descriptions-item label="比赛状态">{{ scoreData.matchStatusName }}</el-descriptions-item>
      <el-descriptions-item label="数据状态">{{ scoreData.dataStatusName }}</el-descriptions-item>
      <el-descriptions-item label="主队总分">{{ previewHomeTotal }}</el-descriptions-item>
      <el-descriptions-item label="客队总分">{{ previewAwayTotal }}</el-descriptions-item>
    </el-descriptions>

    <el-row v-if="scoreData" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="addOvertimePeriod" v-hasPermi="['basket:score:edit']">加加时</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="primary" @click="savePeriodScores" v-hasPermi="['basket:score:edit']" :loading="saving">保存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" @click="submitScore" v-hasPermi="['basket:score:submit']">提交</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" @click="confirmScore" v-hasPermi="['basket:score:confirm']">确认</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" @click="openReopen" v-hasPermi="['basket:score:reopen']">重开</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" @click="openForfeit" v-hasPermi="['basket:score:edit']">判负</el-button>
      </el-col>
    </el-row>

    <el-table v-if="periods.length" :data="periods" v-loading="loading" size="small">
      <el-table-column label="节次" prop="periodNo" width="70" />
      <el-table-column label="类型" prop="periodTypeName" width="100" />
      <el-table-column label="主队得分" min-width="120">
        <template #default="scope">
          <el-input-number v-model="scope.row.homeScore" :min="0" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="客队得分" min-width="120">
        <template #default="scope">
          <el-input-number v-model="scope.row.awayScore" :min="0" controls-position="right" style="width: 100%" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" @click="removePeriod(scope.$index)" v-if="scope.row.periodType === 'OT'" v-hasPermi="['basket:score:edit']" />
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="重开比分" v-model="reopenOpen" width="480px" append-to-body>
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

    <el-dialog title="判负处理" v-model="forfeitOpen" width="480px" append-to-body>
      <el-form ref="forfeitRef" :model="forfeitForm" :rules="forfeitRules" label-width="100px">
        <el-form-item label="判胜球队ID" prop="walkoverTeamId">
          <el-input-number v-model="forfeitForm.walkoverTeamId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="forfeitForm.reason" type="textarea" placeholder="请输入判负原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forfeitOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForfeit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketMatchScore">
import { getMatch } from '@/api/basket/matchSchedule'
import {
  confirmMatchScore,
  forfeitMatch,
  getMatchScore,
  reopenMatchScore,
  saveMatchPeriodScore,
  submitMatchScore
} from '@/api/basket/matchScore'
import type { MatchForfeitReq, MatchPeriodScore, MatchReopenReq, MatchScore } from '@/types'

const { proxy } = getCurrentInstance()
const route = useRoute()

const matchId = ref<number>()
const loading = ref(false)
const saving = ref(false)
const scoreData = ref<MatchScore>()
const periods = ref<MatchPeriodScore[]>([])

const reopenOpen = ref(false)
const forfeitOpen = ref(false)
const reopenForm = ref<MatchReopenReq>({ reason: '' })
const forfeitForm = ref<MatchForfeitReq>({ walkoverTeamId: undefined as unknown as number })
const reopenRules = { reason: [{ required: true, message: '重开原因不能为空', trigger: 'blur' }] }
const forfeitRules = { walkoverTeamId: [{ required: true, message: '判胜球队不能为空', trigger: 'blur' }] }

const previewHomeTotal = computed(() =>
  periods.value.reduce((sum, p) => sum + (p.homeScore || 0), 0)
)
const previewAwayTotal = computed(() =>
  periods.value.reduce((sum, p) => sum + (p.awayScore || 0), 0)
)

function ensureDefaultPeriods(existing?: MatchPeriodScore[]) {
  if (existing && existing.length > 0) {
    periods.value = existing.map(p => ({ ...p }))
    return
  }
  periods.value = [1, 2, 3, 4].map(no => ({
    periodNo: no,
    periodType: 'REG',
    periodTypeName: `第${no}节`,
    homeScore: 0,
    awayScore: 0
  }))
}

function loadScore() {
  if (!matchId.value) return
  loading.value = true
  getMatchScore(matchId.value).then(res => {
    scoreData.value = res.data
    ensureDefaultPeriods(res.data?.periods)
  }).finally(() => {
    loading.value = false
  })
}

function addOvertimePeriod() {
  const otCount = periods.value.filter(p => p.periodType === 'OT').length + 1
  periods.value.push({
    periodNo: 4 + otCount,
    periodType: 'OT',
    periodTypeName: `加时${otCount}`,
    homeScore: 0,
    awayScore: 0
  })
}

function removePeriod(index: number) {
  periods.value.splice(index, 1)
}

function savePeriodScores() {
  if (!matchId.value) return
  saving.value = true
  saveMatchPeriodScore(matchId.value, {
    periods: periods.value.map(p => ({
      periodNo: p.periodNo,
      periodType: p.periodType,
      homeScore: p.homeScore,
      awayScore: p.awayScore
    }))
  }).then(() => {
    proxy.$modal.msgSuccess('保存成功')
    loadScore()
  }).finally(() => {
    saving.value = false
  })
}

function submitScore() {
  if (!matchId.value) return
  proxy.$modal.confirm('确认提交比分？').then(() => submitMatchScore(matchId.value!)).then(() => {
    proxy.$modal.msgSuccess('提交成功')
    loadScore()
  }).catch(() => {})
}

function confirmScore() {
  if (!matchId.value) return
  proxy.$modal.confirm('确认比分无误？').then(() => confirmMatchScore(matchId.value!)).then(() => {
    proxy.$modal.msgSuccess('已确认')
    loadScore()
  }).catch(() => {})
}

function openReopen() {
  reopenForm.value = { reason: '' }
  reopenOpen.value = true
}

function submitReopen() {
  proxy.$refs.reopenRef.validate((valid: boolean) => {
    if (!valid || !matchId.value) return
    reopenMatchScore(matchId.value, reopenForm.value).then(() => {
      proxy.$modal.msgSuccess('已重开')
      reopenOpen.value = false
      loadScore()
    })
  })
}

function openForfeit() {
  forfeitForm.value = { walkoverTeamId: undefined as unknown as number, reason: '' }
  if (matchId.value) {
    getMatch(matchId.value).then(res => {
      forfeitForm.value.walkoverTeamId = res.data?.homeTeamId!
    })
  }
  forfeitOpen.value = true
}

function submitForfeit() {
  proxy.$refs.forfeitRef.validate((valid: boolean) => {
    if (!valid || !matchId.value) return
    proxy.$modal.confirm('确认执行判负？').then(() =>
      forfeitMatch(matchId.value!, forfeitForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('判负已处理')
      forfeitOpen.value = false
      loadScore()
    }).catch(() => {})
  })
}

if (route.query.matchId) {
  matchId.value = Number(route.query.matchId)
  loadScore()
}
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
