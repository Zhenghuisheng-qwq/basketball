<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="名称" prop="partnerName">
        <el-input v-model="queryParams.partnerName" placeholder="赞助伙伴名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:partnerSupport:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="名称" prop="partnerName" min-width="160" :show-overflow-tooltip="true" />
      <el-table-column label="赞助金额" width="140">
        <template #default="scope">
          {{ formatAmount(scope.row.supportAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="赞助日期" prop="supportDate" width="120" />
      <el-table-column label="公开状态" width="100">
        <template #default="scope">
          <dict-tag :options="PARTNER_SUPPORT_STATUS_OPTIONS" :value="scope.row.publishStatus" />
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" align="center" width="260" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:partnerSupport:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:partnerSupport:edit']">编辑</el-button>
          <el-button link type="success" icon="Open" @click="handleEnable(scope.row)" v-hasPermi="['basket:partnerSupport:status']" v-if="scope.row.publishStatus === 0">启用</el-button>
          <el-button link type="warning" icon="TurnOff" @click="handleDisable(scope.row)" v-hasPermi="['basket:partnerSupport:status']" v-if="scope.row.publishStatus === 1">停用</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:partnerSupport:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="赞助记录详情" size="480px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="名称">{{ detail.partnerName }}</el-descriptions-item>
        <el-descriptions-item label="赞助金额">{{ formatAmount(detail.supportAmount) }}</el-descriptions-item>
        <el-descriptions-item label="赞助日期">{{ detail.supportDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.publishStatusName }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="520px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="partnerName">
          <el-input v-model="form.partnerName" placeholder="赞助伙伴名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="赞助日期" prop="supportDate">
          <el-date-picker v-model="form.supportDate" type="date" value-format="YYYY-MM-DD" placeholder="选择赞助日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赞助金额" prop="supportAmount">
          <el-input-number v-model="form.supportAmount" :min="0.01" :precision="2" controls-position="right" style="width: 100%" />
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
  delPartnerSupport,
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
  publishStatus: 1
})

const rules = {
  partnerName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  supportAmount: [{ required: true, message: '请输入赞助金额', trigger: 'blur' }],
  supportDate: [{ required: true, message: '请选择赞助日期', trigger: 'change' }]
}

function formatAmount(value?: number) {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? `¥${num.toFixed(2)}` : '¥0.00'
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
    supportDate: undefined,
    publishStatus: 1
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增赞助记录'
  dialogOpen.value = true
}

function handleUpdate(row: PartnerSupportListItem) {
  resetFormData()
  currentId.value = row.id
  getPartnerSupport(row.id).then(res => {
    const data = res.data!
    form.value = {
      partnerName: data.partnerName || '',
      supportType: 'CASH',
      supportAmount: data.supportAmount,
      supportDate: data.supportDate,
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
    submitLoading.value = true
    const payload: PartnerSupportAddReq = {
      partnerName: form.value.partnerName?.trim(),
      supportType: 'CASH',
      supportAmount: form.value.supportAmount,
      supportDate: form.value.supportDate,
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
    proxy.$modal.msgSuccess('启用成功')
    getList()
  }).catch(() => {})
}

function handleDisable(row: PartnerSupportListItem) {
  proxy.$modal.confirm(`确认停用「${row.partnerName}」的赞助记录？`).then(() =>
    updatePartnerSupportStatus(row.id, 0)
  ).then(() => {
    proxy.$modal.msgSuccess('停用成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row: PartnerSupportListItem) {
  proxy.$modal.confirm(`确认删除「${row.partnerName}」的赞助记录？`).then(() =>
    delPartnerSupport(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
