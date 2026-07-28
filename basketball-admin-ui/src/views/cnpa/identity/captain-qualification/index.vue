<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="授权编号" prop="qualificationNo">
        <el-input v-model="queryParams.qualificationNo" placeholder="请输入授权编号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="用户ID" prop="memberId">
        <el-input v-model="queryParams.memberId" placeholder="请输入用户ID" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员档案ID" prop="playerId">
        <el-input v-model="queryParams.playerId" placeholder="球员档案ID" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="球员昵称" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="授权赛事" prop="seasonId">
        <el-select v-model="queryParams.seasonId" placeholder="全部赛事" clearable filterable style="width: 220px">
          <el-option v-for="item in seasonOptions" :key="item.seasonId" :label="item.displayName || item.seasonName" :value="item.seasonId" />
        </el-select>
      </el-form-item>
      <el-form-item label="授权状态" prop="qualificationStatus">
        <el-select v-model="queryParams.qualificationStatus" placeholder="授权状态" clearable style="width: 120px">
          <el-option v-for="item in CAPTAIN_QUALIFICATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="缴费参考号" prop="paymentReference">
        <el-input v-model="queryParams.paymentReference" placeholder="缴费参考号" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="授权时间" prop="grantedTimeRange">
        <el-date-picker
          v-model="grantedTimeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 360px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openGrant" v-hasPermi="['basket:captainQualification:grant']">授予队长权限</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="User" @click="openEligible" v-hasPermi="['basket:captainQualification:list']">可授权球员</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="授权编号" prop="qualificationNo" min-width="130" />
      <el-table-column label="用户信息" min-width="180">
        <template #default="scope">
          <div>{{ scope.row.nickname || scope.row.username || '未设置昵称' }}</div>
          <div class="sub-text">ID: {{ scope.row.userId || scope.row.memberId || '-' }}</div>
          <div class="sub-text">{{ scope.row.email || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="球员编号" prop="playerNo" min-width="110" />
      <el-table-column label="授权赛事" min-width="180">
        <template #default="scope">
          <div>{{ scope.row.tournamentName || '-' }}</div>
          <div class="sub-text">{{ scope.row.seasonName || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="关联球队" prop="teamName" min-width="120" show-overflow-tooltip />
      <el-table-column label="缴费确认" prop="paymentConfirmed" width="90">
        <template #default="scope">{{ scope.row.paymentConfirmed === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="缴费参考号" prop="paymentReference" min-width="120" show-overflow-tooltip />
      <el-table-column label="授权状态" prop="qualificationStatus" width="90">
        <template #default="scope">
          <dict-tag :options="CAPTAIN_QUALIFICATION_STATUS_OPTIONS" :value="scope.row.qualificationStatus" />
        </template>
      </el-table-column>
      <el-table-column label="当前有效" prop="runtimeValid" width="90">
        <template #default="scope">
          <el-tooltip v-if="scope.row.runtimeValid !== 1 && scope.row.runtimeInvalidReason" :content="scope.row.runtimeInvalidReason" placement="top">
            <span>{{ scope.row.runtimeValid === 1 ? '是' : '否' }}</span>
          </el-tooltip>
          <span v-else>{{ scope.row.runtimeValid === 1 ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="授权时间" prop="grantedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.grantedTime) }}</template>
      </el-table-column>
      <el-table-column label="失效时间" prop="validTo" width="160">
        <template #default="scope">{{ scope.row.validTo ? parseTime(scope.row.validTo) : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="240" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:captainQualification:query']">详情</el-button>
          <el-button link type="primary" icon="Link" @click="openBindTeam(scope.row)" v-hasPermi="['basket:captainQualification:edit']" v-if="scope.row.qualificationStatus === 1 && !scope.row.teamId">绑定球队</el-button>
          <el-button link type="danger" icon="Remove" @click="openRevoke(scope.row)" v-hasPermi="['basket:captainQualification:revoke']" v-if="scope.row.qualificationStatus === 1">收回权限</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="队长授权详情" size="600px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="授权编号">{{ detail.qualificationNo }}</el-descriptions-item>
        <el-descriptions-item label="用户账号">{{ detail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球员编号">{{ detail.playerNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="球员昵称">{{ detail.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授权赛事">{{ detail.tournamentName ? `${detail.tournamentName} / ${detail.seasonName}` : (detail.seasonName || '-') }}</el-descriptions-item>
        <el-descriptions-item label="关联球队">{{ detail.teamName || '未绑定' }}</el-descriptions-item>
        <el-descriptions-item label="授权状态">
          <dict-tag :options="CAPTAIN_QUALIFICATION_STATUS_OPTIONS" :value="detail.qualificationStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="当前有效">{{ detail.runtimeValid === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="无效原因" v-if="detail.runtimeValid !== 1">{{ detail.runtimeInvalidReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="缴费确认">{{ detail.paymentConfirmed === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="缴费参考号">{{ detail.paymentReference || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授权时间">{{ parseTime(detail.grantedTime) }}</el-descriptions-item>
        <el-descriptions-item label="失效时间">{{ detail.validTo ? parseTime(detail.validTo) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="授权周期">{{ parseTime(detail.validFrom) || '-' }} ~ {{ detail.validTo ? parseTime(detail.validTo) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="授予备注">{{ detail.grantReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="收回时间">{{ detail.revokedTime ? parseTime(detail.revokedTime) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="收回原因">{{ detail.revokeReason || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="detail-actions" v-if="detail">
        <el-button v-if="detail.qualificationStatus === 1 && !detail.teamId" type="primary" plain @click="openBindTeam(detail)">绑定球队</el-button>
        <el-button v-if="detail.qualificationStatus === 1" type="danger" plain @click="openRevoke(detail)">收回权限</el-button>
        <el-button @click="detailOpen = false">返回列表</el-button>
      </div>
    </el-drawer>

    <el-dialog title="授予队长权限" v-model="grantOpen" width="580px" append-to-body>
      <el-form ref="grantRef" :model="grantForm" :rules="grantRules" label-width="108px">
        <el-form-item label="授权赛事" prop="seasonId">
          <el-select v-model="grantForm.seasonId" placeholder="请选择授权赛事" filterable style="width: 100%" @change="handleSeasonChange">
            <el-option v-for="item in seasonOptions" :key="item.seasonId" :label="item.displayName || item.seasonName" :value="item.seasonId" />
          </el-select>
        </el-form-item>
        <el-form-item label="球员" prop="playerId">
          <el-select v-model="grantForm.playerId" placeholder="请选择球员" filterable style="width: 100%">
            <el-option v-for="item in eligibleList" :key="item.playerId" :label="`${captainDisplayName(item)} (${item.playerNo})`" :value="item.playerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费确认" prop="paymentConfirmed">
          <el-radio-group v-model="grantForm.paymentConfirmed">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="缴费参考号">
          <el-input v-model="grantForm.paymentReference" placeholder="请输入缴费参考号" />
        </el-form-item>
        <el-form-item label="授权周期">
          <span class="validity-hint">{{ grantPeriodHint }}</span>
        </el-form-item>
        <el-form-item label="关联球队">
          <el-input v-model="grantForm.teamId" placeholder="可选，填写本人担任队长的球队 ID" clearable />
        </el-form-item>
        <el-form-item label="授予备注">
          <el-input v-model="grantForm.grantRemark" type="textarea" placeholder="请输入授予备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantOpen = false">取消</el-button>
        <el-button type="primary" @click="submitGrant">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="可授权球员" v-model="eligibleOpen" width="640px" append-to-body>
      <el-table :data="eligibleList" v-loading="eligibleLoading" size="small">
        <el-table-column label="球员编号" prop="playerNo" width="120" />
        <el-table-column label="昵称" min-width="120">
          <template #default="scope">{{ captainDisplayName(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="用户ID" prop="userId" width="100">
          <template #default="scope">{{ scope.row.userId || scope.row.memberId || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button link type="primary" @click="selectEligible(scope.row)" v-hasPermi="['basket:captainQualification:grant']">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog title="收回队长权限" v-model="revokeOpen" width="480px" append-to-body>
      <el-form ref="revokeRef" :model="revokeForm" :rules="revokeRules" label-width="88px">
        <el-form-item label="收回原因" prop="revokeReason">
          <el-input v-model="revokeForm.revokeReason" type="textarea" placeholder="请输入收回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeOpen = false">取消</el-button>
        <el-button type="primary" @click="submitRevoke">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="绑定球队" v-model="bindOpen" width="480px" append-to-body>
      <el-form ref="bindRef" :model="bindForm" :rules="bindRules" label-width="88px">
        <el-form-item label="球队 ID" prop="teamId">
          <el-input v-model="bindForm.teamId" placeholder="请输入本人担任队长的球队 ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindOpen = false">取消</el-button>
        <el-button type="primary" @click="submitBindTeam">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketCaptainGrant">
import {
  bindCaptainQualificationTeam,
  getCaptainQualification,
  grantCaptainQualification,
  listCaptainQualification,
  listEligibleCaptains,
  listGrantSeasonOptions,
  revokeCaptainQualification
} from '@/api/basket/captainQualification'
import type {
  CaptainGrantSeasonOption,
  CaptainQualificationDetail,
  CaptainQualificationGrantReq,
  CaptainQualificationListItem,
  CaptainQualificationQuery,
  CaptainQualificationRevokeReq,
  EligibleCaptain
} from '@/types'
import { CAPTAIN_QUALIFICATION_STATUS_OPTIONS } from '@/types'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance()

const dataList = ref<CaptainQualificationListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<CaptainQualificationDetail>()
const grantOpen = ref(false)
const grantForm = ref<Partial<CaptainQualificationGrantReq>>({ paymentConfirmed: 0 })
const seasonOptions = ref<CaptainGrantSeasonOption[]>([])
const grantPeriodHint = ref('请先选择授权赛事')
const grantRules = {
  seasonId: [{ required: true, message: '请选择授权赛事', trigger: 'change' }],
  playerId: [{ required: true, message: '请选择球员', trigger: 'change' }],
  paymentConfirmed: [{ required: true, message: '请确认缴费状态', trigger: 'change' }]
}
const eligibleOpen = ref(false)
const eligibleList = ref<EligibleCaptain[]>([])
const eligibleLoading = ref(false)
const currentId = ref<number>()
const revokeOpen = ref(false)
const revokeForm = ref<CaptainQualificationRevokeReq>({ revokeReason: '' })
const revokeRules = {
  revokeReason: [{ required: true, message: '收回原因不能为空', trigger: 'blur' }]
}
const bindOpen = ref(false)
const bindForm = ref<{ teamId?: number | string }>({})
const bindRules = {
  teamId: [{ required: true, message: '请输入球队 ID', trigger: 'blur' }]
}
const grantedTimeRange = ref<string[]>([])

const queryParams = ref<CaptainQualificationQuery>({
  pageNum: 1,
  pageSize: 10
})

function captainDisplayName(item: EligibleCaptain) {
  return item.gameNickname || item.nickname || item.playerNo || '-'
}

function loadSeasonOptions() {
  return listGrantSeasonOptions().then(res => {
    seasonOptions.value = res.data || []
  })
}

function updateGrantPeriodHint(seasonId?: number) {
  const season = seasonOptions.value.find(item => item.seasonId === seasonId)
  if (!season) {
    grantPeriodHint.value = '请先选择授权赛事'
    return
  }
  const nowText = parseTime(new Date()) || ''
  const endText = season.seasonEndTime ? parseTime(season.seasonEndTime) : '-'
  grantPeriodHint.value = `自 ${nowText} 起，至 ${endText} 止（随赛事结束自动失效）`
}

function handleSeasonChange(seasonId?: number) {
  updateGrantPeriodHint(seasonId)
}

function getList() {
  loading.value = true
  if (grantedTimeRange.value?.length === 2) {
    queryParams.value.grantedTimeBegin = grantedTimeRange.value[0]
    queryParams.value.grantedTimeEnd = grantedTimeRange.value[1]
  } else {
    queryParams.value.grantedTimeBegin = undefined
    queryParams.value.grantedTimeEnd = undefined
  }
  listCaptainQualification(queryParams.value).then(response => {
    dataList.value = response.rows
    total.value = response.total
  }).finally(() => {
    loading.value = false
  })
}

function loadEligible() {
  eligibleLoading.value = true
  return listEligibleCaptains().then(res => {
    eligibleList.value = res.data || []
  }).finally(() => {
    eligibleLoading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  grantedTimeRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleDetail(row: CaptainQualificationListItem) {
  getCaptainQualification(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function openGrant() {
  grantForm.value = { paymentConfirmed: 0, paymentReference: '', grantRemark: '' }
  grantPeriodHint.value = '请先选择授权赛事'
  Promise.all([loadSeasonOptions(), loadEligible()]).then(() => {
    grantOpen.value = true
  })
}

function openEligible() {
  eligibleOpen.value = true
  loadEligible()
}

function selectEligible(row: EligibleCaptain) {
  grantForm.value = {
    ...grantForm.value,
    playerId: row.playerId,
    paymentConfirmed: 0,
    paymentReference: '',
    grantRemark: ''
  }
  eligibleOpen.value = false
  if (!grantOpen.value) {
    loadSeasonOptions().then(() => {
      grantOpen.value = true
    })
  }
}

function submitGrant() {
  if (grantForm.value.paymentConfirmed !== 1) {
    proxy.$modal.msgWarning('请先确认缴费后再授予队长权限')
    return
  }
  proxy.$refs.grantRef.validate((valid: boolean) => {
    if (!valid) return
    const payload: CaptainQualificationGrantReq = {
      seasonId: Number(grantForm.value.seasonId),
      playerId: Number(grantForm.value.playerId),
      paymentConfirmed: 1,
      paymentReference: grantForm.value.paymentReference,
      grantRemark: grantForm.value.grantRemark
    }
    if (grantForm.value.teamId) {
      payload.teamId = Number(grantForm.value.teamId)
    }
    grantCaptainQualification(payload).then(() => {
      proxy.$modal.msgSuccess('授予队长权限成功')
      grantOpen.value = false
      getList()
    })
  })
}

function openRevoke(row: CaptainQualificationListItem | CaptainQualificationDetail) {
  currentId.value = row.id
  revokeForm.value = { revokeReason: '' }
  revokeOpen.value = true
}

function submitRevoke() {
  proxy.$refs.revokeRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    proxy.$modal.confirm('确认收回该用户的队长权限？').then(() =>
      revokeCaptainQualification(currentId.value!, revokeForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('收回队长权限成功')
      revokeOpen.value = false
      detailOpen.value = false
      getList()
    }).catch(() => {})
  })
}

function openBindTeam(row: CaptainQualificationListItem | CaptainQualificationDetail) {
  currentId.value = row.id
  bindForm.value = { teamId: undefined }
  bindOpen.value = true
}

function submitBindTeam() {
  proxy.$refs.bindRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    bindCaptainQualificationTeam(currentId.value!, { teamId: Number(bindForm.value.teamId) }).then(() => {
      proxy.$modal.msgSuccess('绑定球队成功')
      bindOpen.value = false
      if (detailOpen.value && detail.value?.id === currentId.value) {
        handleDetail({ id: currentId.value! } as CaptainQualificationListItem)
      }
      getList()
    })
  })
}

loadSeasonOptions()
getList()
</script>

<style scoped>
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.validity-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.detail-actions {
  margin-top: 16px;
  display: flex;
  gap: 8px;
}
</style>
