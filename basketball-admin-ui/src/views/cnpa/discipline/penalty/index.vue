<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="处罚管理"
      description="用于记录和处理球员、球队在赛事中的纪律处罚，包括警告、禁赛、取消资格、扣分和处罚解除。处罚记录将进入对应球员或球队档案，并影响报名、参赛或阵容资格。本页不处理比赛数据审核、阵容变更审核或赛程调整。"
    />

    <el-tabs v-model="activeTab" class="mb8" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="ALL" />
      <el-tab-pane label="球员处罚" name="PLAYER" />
      <el-tab-pane label="球队处罚" name="TEAM" />
    </el-tabs>

    <el-form :inline="true" class="mb8">
      <el-form-item label="赛季ID">
        <el-input-number v-model="query.seasonId" :min="1" controls-position="right" style="width: 140px" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.recordStatus" clearable style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="生效中" value="ACTIVE" />
          <el-option label="已解除" value="REVOKED" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" clearable placeholder="处罚编号/标题" style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadList" v-hasPermi="['basket:discipline:list']">查询</el-button>
        <el-button type="success" icon="Plus" @click="openCreate" v-hasPermi="['basket:discipline:add']">新增处罚</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="displayRows" v-loading="loading" border>
      <el-table-column label="处罚编号" prop="recordNo" min-width="150" />
      <el-table-column label="处罚对象类型" prop="targetType" width="120">
        <template #default="scope">{{ formatTargetType(scope.row.targetType) }}</template>
      </el-table-column>
      <el-table-column label="处罚对象" min-width="120">
        <template #default="scope">{{ formatTargetName(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="关联赛事" prop="seasonId" width="90">
        <template #default="scope">赛季 {{ scope.row.seasonId ?? '-' }}</template>
      </el-table-column>
      <el-table-column label="处罚类型" prop="disciplineType" width="110" />
      <el-table-column label="处罚措施" prop="penaltyCode" width="150" />
      <el-table-column label="处罚原因" prop="reasonText" min-width="140" show-overflow-tooltip />
      <el-table-column label="生效时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.effectiveStartTime) }}</template>
      </el-table-column>
      <el-table-column label="失效时间" width="170">
        <template #default="scope">{{ parseTime(scope.row.effectiveEndTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" prop="recordStatus" width="90">
        <template #default="scope">{{ formatRecordStatus(scope.row.recordStatus) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="showDetail(scope.row.id)" v-hasPermi="['basket:discipline:query']">详情</el-button>
          <el-button link type="success" v-if="scope.row.recordStatus === 'DRAFT'" @click="doActivate(scope.row.id)" v-hasPermi="['basket:discipline:activate']">生效</el-button>
          <el-button link type="danger" v-if="scope.row.recordStatus === 'ACTIVE'" @click="doRevoke(scope.row.id)" v-hasPermi="['basket:discipline:revoke']">解除处罚</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && !displayRows.length" description="暂无处罚记录，可点击「新增处罚」创建" />

    <pagination :total="displayTotal" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />

    <el-dialog v-model="createOpen" title="新增处罚" width="640px">
      <el-form label-width="108px">
        <el-form-item label="处罚对象类型">
          <el-select v-model="form.targetType" style="width: 100%">
            <el-option label="球员" value="PLAYER" />
            <el-option label="历史球队" value="PERMANENT_TEAM" />
            <el-option label="赛事球队" value="EVENT_TEAM" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛季ID"><el-input-number v-model="form.seasonId" :min="1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="球员ID"><el-input-number v-model="form.playerId" :min="1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="球队ID"><el-input-number v-model="form.teamId" :min="1" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="处罚类型"><el-input v-model="form.disciplineType" placeholder="如 DISCIPLINE" /></el-form-item>
        <el-form-item label="处罚措施">
          <el-select v-model="form.penaltyCode" style="width: 100%">
            <el-option v-for="item in penaltyOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="处罚原因"><el-input v-model="form.reasonText" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="禁赛场次"><el-input-number v-model="form.matchSuspensionCount" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="扣分"><el-input-number v-model="form.pointDeduction" :min="0" controls-position="right" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailOpen" title="处罚详情" size="520px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="处罚编号">{{ detail.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="处罚对象类型">{{ formatTargetType(detail.targetType) }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="处罚措施">{{ detail.penaltyCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ formatRecordStatus(detail.recordStatus) }}</el-descriptions-item>
        <el-descriptions-item label="处罚原因">{{ detail.reasonText }}</el-descriptions-item>
        <el-descriptions-item label="剩余禁赛">{{ detail.matchSuspensionRemaining }}</el-descriptions-item>
        <el-descriptions-item label="扣分">{{ detail.pointDeduction }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="BasketDisciplinePenalty">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { activateDiscipline, createDiscipline, getDiscipline, listDiscipline, revokeDiscipline } from '@/api/basket/discipline'
import { parseTime } from '@/utils/ruoyi'

const loading = ref(false)
const rows = ref<any[]>([])
const total = ref(0)
const activeTab = ref('ALL')
const query = reactive<{ pageNum: number; pageSize: number; seasonId?: number; recordStatus?: string; keyword?: string; targetType?: string }>({
  pageNum: 1,
  pageSize: 10
})
const createOpen = ref(false)
const detailOpen = ref(false)
const detail = ref<any>(null)
const penaltyOptions = ['WARNING', 'MATCH_SUSPENSION', 'DATE_RANGE_SUSPENSION', 'POINT_DEDUCTION', 'REGISTRATION_BAN', 'TRANSFER_BAN', 'MATCH_ELIGIBILITY_CANCEL', 'EVENT_DISQUALIFICATION', 'TEAM_SUSPENSION', 'PLAYER_QUALIFICATION_SUSPENSION', 'CAPTAIN_BAN']
const form = reactive<any>({ targetType: 'PLAYER', disciplineType: 'DISCIPLINE', matchSuspensionCount: 0, pointDeduction: 0 })

const TEAM_TARGET_TYPES = ['PERMANENT_TEAM', 'EVENT_TEAM']

const displayRows = computed(() => {
  if (activeTab.value !== 'TEAM') return rows.value
  return rows.value.filter(item => TEAM_TARGET_TYPES.includes(item.targetType))
})

const displayTotal = computed(() => {
  if (activeTab.value !== 'TEAM') return total.value
  return displayRows.value.length
})

function formatTargetType(value?: string) {
  const map: Record<string, string> = {
    PLAYER: '球员',
    PERMANENT_TEAM: '历史球队',
    EVENT_TEAM: '赛事球队'
  }
  return map[value || ''] || value || '-'
}

function formatRecordStatus(value?: string) {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    ACTIVE: '生效中',
    REVOKED: '已解除'
  }
  return map[value || ''] || value || '-'
}

function formatTargetName(row: any) {
  if (row.targetType === 'PLAYER') return row.playerId ? `球员 #${row.playerId}` : '-'
  if (row.targetType === 'PERMANENT_TEAM') return row.teamId ? `球队 #${row.teamId}` : '-'
  if (row.targetType === 'EVENT_TEAM') return row.eventTeamId ? `赛事球队 #${row.eventTeamId}` : '-'
  return '-'
}

function handleTabChange() {
  query.pageNum = 1
  if (activeTab.value === 'PLAYER') {
    query.targetType = 'PLAYER'
  } else {
    query.targetType = undefined
  }
  loadList()
}

async function loadList() {
  loading.value = true
  try {
    const res = await listDiscipline(query)
    rows.value = res.rows || []
    total.value = res.total ?? 0
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { targetType: 'PLAYER', disciplineType: 'DISCIPLINE', seasonId: query.seasonId, title: '', reasonText: '', matchSuspensionCount: 0, pointDeduction: 0, penaltyCode: 'WARNING' })
  createOpen.value = true
}

async function submitCreate() {
  if (!form.title || !form.reasonText || !form.penaltyCode) {
    ElMessage.warning('请填写完整信息')
    return
  }
  await createDiscipline({ ...form, idempotencyKey: `UI-${Date.now()}` })
  ElMessage.success('处罚已创建')
  createOpen.value = false
  loadList()
}

async function showDetail(id: number) {
  const res = await getDiscipline(id)
  detail.value = res.data
  detailOpen.value = true
}

async function doActivate(id: number) {
  await ElMessageBox.confirm('确认使该处罚生效？', '提示')
  await activateDiscipline(id)
  ElMessage.success('处罚已生效')
  loadList()
}

async function doRevoke(id: number) {
  const { value } = await ElMessageBox.prompt('请输入解除原因', '解除处罚', { inputType: 'textarea' })
  await revokeDiscipline(id, { revokeReason: value })
  ElMessage.success('处罚已解除')
  loadList()
}

onMounted(loadList)
</script>
