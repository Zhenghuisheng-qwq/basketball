<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="合作伙伴" prop="partnerName">
        <el-input v-model="queryParams.partnerName" placeholder="合作伙伴名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="赞助日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="公开状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="item in PARTNER_SUPPORT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:partnerSupport:add']">新增赞助</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" class="even-table even-table--6" :data="dataList" border>
      <el-table-column label="合作伙伴" prop="partnerName" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="赞助金额" align="center">
        <template #default="scope">
          <span class="amount-text">{{ formatAmount(scope.row.supportAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="赞助日期" prop="supportDate" align="center" />
      <el-table-column label="备注" prop="publicDescription" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="创建时间" prop="createTime" align="center" />
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:partnerSupport:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:partnerSupport:edit']">编辑</el-button>
          <el-button link type="warning" @click="handleDisable(scope.row)" v-hasPermi="['basket:partnerSupport:status']" v-if="scope.row.publishStatus === 1">停用公开</el-button>
          <el-button link type="success" @click="handleEnable(scope.row)" v-hasPermi="['basket:partnerSupport:status']" v-if="scope.row.publishStatus === 0">公开</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="detailOpen" title="赞助记录详情" width="520px" align-center center append-to-body>
      <el-descriptions v-if="detail" class="center-desc" :column="1" border>
        <el-descriptions-item label="合作伙伴">{{ detail.partnerName }}</el-descriptions-item>
        <el-descriptions-item label="赞助金额">{{ formatAmount(detail.supportAmount) }}</el-descriptions-item>
        <el-descriptions-item label="赞助日期">{{ detail.supportDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.publicDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开状态">{{ detail.publishStatusName }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="520px" align-center center append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="合作伙伴" prop="partnerName">
          <el-input v-model="form.partnerName" placeholder="合作伙伴名称" maxlength="100" show-word-limit :disabled="!!currentId" />
        </el-form-item>
        <el-form-item label="赞助金额" prop="supportAmount">
          <el-input-number v-model="form.supportAmount" :min="0.01" :precision="2" :step="1000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赞助日期" prop="supportDate">
          <el-date-picker v-model="form.supportDate" type="date" value-format="YYYY-MM-DD" placeholder="选择赞助日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.publicDescription" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  addPartnerSupport,
  getPartnerSupport,
  listPartnerSupport,
  updatePartnerSupport,
  updatePartnerSupportStatus
} from '@/api/basket/partnerSupport'
import type {
  PartnerSupportAddReq,
  PartnerSupportListItem,
  PartnerSupportQuery
} from '@/types'
import { PARTNER_SUPPORT_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<PartnerSupportListItem[]>([])
const loading = ref(true)
const submitLoading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<PartnerSupportListItem>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const dateRange = ref<string[]>([])

const queryParams = ref<PartnerSupportQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<PartnerSupportAddReq>({
  partnerName: '',
  supportType: 'CASH',
  supportAmount: undefined,
  supportDate: undefined,
  publicDescription: undefined,
  publishStatus: 1
})

const rules = {
  partnerName: [{ required: true, message: '合作伙伴不能为空', trigger: 'blur' }],
  supportAmount: [{ required: true, message: '请输入赞助金额', trigger: 'change' }],
  supportDate: [{ required: true, message: '请选择赞助日期', trigger: 'change' }]
}

function formatAmount(value?: number) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '¥0.00'
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function todayStr() {
  const d = new Date()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function getList() {
  loading.value = true
  const params = { ...queryParams.value, supportType: 'CASH' }
  params.supportDateBegin = dateRange.value?.[0]
  params.supportDateEnd = dateRange.value?.[1]
  listPartnerSupport(params).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm('queryRef')
  handleQuery()
}

function resetFormData() {
  form.value = {
    partnerName: '',
    supportType: 'CASH',
    supportAmount: undefined,
    supportDate: todayStr(),
    publicDescription: undefined,
    publishStatus: 1
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增赞助'
  dialogOpen.value = true
}

function handleUpdate(row: PartnerSupportListItem) {
  resetFormData()
  currentId.value = row.id
  getPartnerSupport(row.id).then(res => {
    const data = res.data!
    form.value = {
      partnerId: data.partnerId,
      partnerName: data.partnerName || '',
      supportType: 'CASH',
      supportAmount: data.supportAmount,
      supportDate: data.supportDate,
      publicDescription: data.publicDescription,
      publishStatus: data.publishStatus ?? 1
    }
    dialogTitle.value = '编辑赞助记录'
    dialogOpen.value = true
  })
}

function handleDetail(row: PartnerSupportListItem) {
  getPartnerSupport(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const amount = Number(form.value.supportAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      proxy.$modal.msgError('赞助金额必须大于 0')
      return
    }
    submitLoading.value = true
    const payload: PartnerSupportAddReq = {
      partnerId: form.value.partnerId,
      partnerName: form.value.partnerName?.trim(),
      supportType: 'CASH',
      supportAmount: amount,
      supportDate: form.value.supportDate,
      publicDescription: form.value.publicDescription,
      publishStatus: 1
    }
    const req = currentId.value
      ? updatePartnerSupport(currentId.value, payload)
      : addPartnerSupport(payload)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

function handleEnable(row: PartnerSupportListItem) {
  proxy.$modal.confirm(`确认公开「${row.partnerName}」的赞助记录？`).then(() =>
    updatePartnerSupportStatus(row.id, 1)
  ).then(() => {
    proxy.$modal.msgSuccess('已公开')
    getList()
  }).catch(() => {})
}

function handleDisable(row: PartnerSupportListItem) {
  proxy.$modal.confirm(`确认停用公开「${row.partnerName}」的赞助记录？记录仍会保留。`).then(() =>
    updatePartnerSupportStatus(row.id, 0)
  ).then(() => {
    proxy.$modal.msgSuccess('已停用公开')
    getList()
  }).catch(() => {})
}

defineExpose({ getList })

getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.amount-text {
  font-weight: 600;
  color: #b88230;
}
.even-table {
  width: 100%;
}
.even-table :deep(.el-table__header),
.even-table :deep(.el-table__body) {
  width: 100% !important;
  table-layout: fixed !important;
}
.even-table :deep(col:not([name="gutter"])) {
  width: calc(100% / 6) !important;
}
.even-table :deep(.cell) {
  text-align: center;
  white-space: normal;
  word-break: break-word;
}
.center-desc :deep(.el-descriptions__label),
.center-desc :deep(.el-descriptions__content) {
  text-align: center !important;
}
</style>
