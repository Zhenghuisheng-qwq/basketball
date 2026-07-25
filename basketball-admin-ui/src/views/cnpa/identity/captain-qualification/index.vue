<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="资格编号" prop="qualificationNo">
        <el-input v-model="queryParams.qualificationNo" placeholder="请输入资格编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="会员ID" prop="memberId">
        <el-input v-model="queryParams.memberId" placeholder="请输入会员ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员ID" prop="playerId">
        <el-input v-model="queryParams.playerId" placeholder="请输入球员ID" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="qualificationStatus">
        <el-select v-model="queryParams.qualificationStatus" placeholder="资格状态" clearable style="width: 200px">
          <el-option v-for="item in CAPTAIN_QUALIFICATION_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="openGrant" v-hasPermi="['basket:captainQualification:grant']">授予资格</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="User" @click="openEligible" v-hasPermi="['basket:captainQualification:list']">可授予球员</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="资格编号" prop="qualificationNo" min-width="140" />
      <el-table-column label="会员ID" prop="memberId" width="100" />
      <el-table-column label="球员ID" prop="playerId" width="100" />
      <el-table-column label="状态" prop="qualificationStatus" width="100">
        <template #default="scope">
          <dict-tag :options="CAPTAIN_QUALIFICATION_STATUS_OPTIONS" :value="scope.row.qualificationStatus" />
        </template>
      </el-table-column>
      <el-table-column label="当前有效" prop="isCurrent" width="90">
        <template #default="scope">{{ scope.row.isCurrent === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="缴费确认" prop="paymentConfirmed" width="90">
        <template #default="scope">{{ scope.row.paymentConfirmed === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="授予时间" prop="grantedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.grantedTime) }}</template>
      </el-table-column>
      <el-table-column label="有效期至" prop="validTo" width="160">
        <template #default="scope">{{ parseTime(scope.row.validTo) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="220" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:captainQualification:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleEdit(scope.row)" v-hasPermi="['basket:captainQualification:edit']" v-if="scope.row.qualificationStatus === 1">编辑</el-button>
          <el-button link type="danger" icon="Remove" @click="openRevoke(scope.row)" v-hasPermi="['basket:captainQualification:revoke']" v-if="scope.row.qualificationStatus === 1">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="队长资格详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="资格编号">{{ detail.qualificationNo }}</el-descriptions-item>
        <el-descriptions-item label="会员ID">{{ detail.memberId }}</el-descriptions-item>
        <el-descriptions-item label="球员ID">{{ detail.playerId }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="CAPTAIN_QUALIFICATION_STATUS_OPTIONS" :value="detail.qualificationStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="缴费确认">{{ detail.paymentConfirmed === 1 ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="缴费参考号">{{ detail.paymentReference || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授予原因">{{ detail.grantReason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="授予时间">{{ parseTime(detail.grantedTime) }}</el-descriptions-item>
        <el-descriptions-item label="有效期">{{ parseTime(detail.validFrom) }} ~ {{ parseTime(detail.validTo) }}</el-descriptions-item>
        <el-descriptions-item label="撤销原因">{{ detail.revokeReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="grantTitle" v-model="grantOpen" width="560px" append-to-body>
      <el-form ref="grantRef" :model="grantForm" :rules="grantRules" label-width="100px">
        <el-form-item label="球员" prop="playerId">
          <el-select v-model="grantForm.playerId" placeholder="请选择球员" filterable style="width: 100%">
            <el-option v-for="item in eligibleList" :key="item.playerId" :label="`${captainDisplayName(item)} (${item.playerNo})`" :value="item.playerId" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费确认">
          <el-radio-group v-model="grantForm.paymentConfirmed">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="缴费参考号">
          <el-input v-model="grantForm.paymentReference" placeholder="请输入缴费参考号" />
        </el-form-item>
        <el-form-item label="有效期起">
          <el-date-picker v-model="grantForm.validFrom" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期止">
          <el-date-picker v-model="grantForm.validTo" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" style="width: 100%" />
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

    <el-dialog title="可授予队长资格球员" v-model="eligibleOpen" width="640px" append-to-body>
      <el-table :data="eligibleList" v-loading="eligibleLoading" size="small">
        <el-table-column label="球员编号" prop="playerNo" width="120" />
        <el-table-column label="昵称" min-width="120">
          <template #default="scope">{{ captainDisplayName(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="会员ID" prop="memberId" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="scope">
            <el-button link type="primary" @click="selectEligible(scope.row)" v-hasPermi="['basket:captainQualification:grant']">授予</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog title="撤销队长资格" v-model="revokeOpen" width="480px" append-to-body>
      <el-form ref="revokeRef" :model="revokeForm" :rules="revokeRules" label-width="88px">
        <el-form-item label="撤销原因" prop="revokeReason">
          <el-input v-model="revokeForm.revokeReason" type="textarea" placeholder="请输入撤销原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="revokeOpen = false">取消</el-button>
        <el-button type="primary" @click="submitRevoke">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketCaptainQualification">
import {
  getCaptainQualification,
  grantCaptainQualification,
  listCaptainQualification,
  listEligibleCaptains,
  revokeCaptainQualification,
  updateCaptainQualification
} from '@/api/basket/captainQualification'
import type {
  CaptainQualificationDetail,
  CaptainQualificationGrantReq,
  CaptainQualificationListItem,
  CaptainQualificationQuery,
  CaptainQualificationRevokeReq,
  CaptainQualificationUpdateReq,
  EligibleCaptain
} from '@/types'
import { CAPTAIN_QUALIFICATION_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<CaptainQualificationListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<CaptainQualificationDetail>()
const grantOpen = ref(false)
const grantTitle = ref('授予队长资格')
const grantForm = ref<CaptainQualificationGrantReq | CaptainQualificationUpdateReq>({ playerId: undefined, paymentConfirmed: 0 })
const grantRules = {
  playerId: [{ required: true, message: '请选择球员', trigger: 'change' }]
}
const eligibleOpen = ref(false)
const eligibleList = ref<EligibleCaptain[]>([])
const eligibleLoading = ref(false)
const currentId = ref<number>()
const isEdit = ref(false)
const revokeOpen = ref(false)
const revokeForm = ref<CaptainQualificationRevokeReq>({ revokeReason: '' })
const revokeRules = {
  revokeReason: [{ required: true, message: '撤销原因不能为空', trigger: 'blur' }]
}

const queryParams = ref<CaptainQualificationQuery>({
  pageNum: 1,
  pageSize: 10
})

function captainDisplayName(item: EligibleCaptain) {
  return item.gameNickname || item.nickname || item.playerNo || '-'
}

function getList() {
  loading.value = true
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
  isEdit.value = false
  grantTitle.value = '授予队长资格'
  grantForm.value = { playerId: undefined, paymentConfirmed: 0, paymentReference: '', grantRemark: '' }
  loadEligible().then(() => {
    grantOpen.value = true
  })
}

function openEligible() {
  eligibleOpen.value = true
  loadEligible()
}

function selectEligible(row: EligibleCaptain) {
  isEdit.value = false
  grantTitle.value = '授予队长资格'
  grantForm.value = { playerId: row.playerId, paymentConfirmed: 0, paymentReference: '', grantRemark: '' }
  eligibleOpen.value = false
  grantOpen.value = true
}

function handleEdit(row: CaptainQualificationListItem) {
  isEdit.value = true
  currentId.value = row.id
  grantTitle.value = '编辑队长资格'
  getCaptainQualification(row.id).then(res => {
    const data = res.data!
    grantForm.value = {
      paymentConfirmed: data.paymentConfirmed,
      paymentReference: data.paymentReference,
      validFrom: data.validFrom,
      validTo: data.validTo,
      grantRemark: data.grantReason
    }
    grantOpen.value = true
  })
}

function submitGrant() {
  if (isEdit.value) {
    if (!currentId.value) return
    updateCaptainQualification(currentId.value, grantForm.value as CaptainQualificationUpdateReq).then(() => {
      proxy.$modal.msgSuccess('修改成功')
      grantOpen.value = false
      getList()
    })
    return
  }
  proxy.$refs.grantRef.validate((valid: boolean) => {
    if (!valid) return
    grantCaptainQualification(grantForm.value as CaptainQualificationGrantReq).then(() => {
      proxy.$modal.msgSuccess('授予成功')
      grantOpen.value = false
      getList()
    })
  })
}

function openRevoke(row: CaptainQualificationListItem) {
  currentId.value = row.id
  revokeForm.value = { revokeReason: '' }
  revokeOpen.value = true
}

function submitRevoke() {
  proxy.$refs.revokeRef.validate((valid: boolean) => {
    if (!valid || !currentId.value) return
    proxy.$modal.confirm('确认撤销该队长资格？').then(() =>
      revokeCaptainQualification(currentId.value!, revokeForm.value)
    ).then(() => {
      proxy.$modal.msgSuccess('撤销成功')
      revokeOpen.value = false
      getList()
    }).catch(() => {})
  })
}

getList()
</script>
