<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="合作伙伴" prop="partnerName">
        <el-input v-model="queryParams.partnerName" placeholder="名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="支持方式" prop="supportType">
        <el-select v-model="queryParams.supportType" placeholder="支持方式" clearable style="width: 140px">
          <el-option v-for="item in PARTNER_SUPPORT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="公开状态" prop="publishStatus">
        <el-select v-model="queryParams.publishStatus" placeholder="状态" clearable style="width: 120px">
          <el-option v-for="item in PARTNER_SUPPORT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="支持日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          style="width: 240px"
        />
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
      <el-table-column label="ID" prop="id" width="80" />
      <el-table-column label="合作伙伴" prop="partnerName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="Logo" width="80">
        <template #default="scope">
          <el-image v-if="scope.row.partnerLogoUrl" :src="scope.row.partnerLogoUrl" style="width: 40px; height: 40px" fit="contain" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="支持方式" prop="supportTypeName" width="110" />
      <el-table-column label="支持金额" width="120">
        <template #default="scope">
          <span v-if="scope.row.supportType === 'CASH'">{{ formatAmount(scope.row.supportAmount) }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="支持日期" prop="supportDate" width="120" />
      <el-table-column label="公开状态" width="100">
        <template #default="scope">
          <dict-tag :options="PARTNER_SUPPORT_STATUS_OPTIONS" :value="scope.row.publishStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" />
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

    <el-drawer v-model="detailOpen" title="支持记录详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="合作伙伴">{{ detail.partnerName }}</el-descriptions-item>
        <el-descriptions-item label="支持方式">{{ detail.supportTypeName }}</el-descriptions-item>
        <el-descriptions-item label="支持金额">{{ detail.supportType === 'CASH' ? formatAmount(detail.supportAmount) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="支持日期">{{ detail.supportDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开说明">{{ detail.publicDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.publishStatusName }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="合作伙伴" prop="partnerId">
          <el-select v-model="form.partnerId" filterable placeholder="选择合作伙伴" style="width: 100%">
            <el-option v-for="item in partnerOptions" :key="item.id" :label="item.partnerName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="支持方式" prop="supportType">
          <el-select v-model="form.supportType" style="width: 100%">
            <el-option v-for="item in PARTNER_SUPPORT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支持金额" prop="supportAmount" v-if="form.supportType === 'CASH'">
          <el-input-number v-model="form.supportAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支持日期" prop="supportDate">
          <el-date-picker v-model="form.supportDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="公开说明">
          <el-input v-model="form.publicDescription" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="是否公开">
          <el-radio-group v-model="form.publishStatus">
            <el-radio v-for="item in PARTNER_SUPPORT_STATUS_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" controls-position="right" style="width: 100%" />
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
import { listPartner } from '@/api/basket/partner'
import {
  addPartnerSupport,
  delPartnerSupport,
  getPartnerSupport,
  listPartnerSupport,
  updatePartnerSupport,
  updatePartnerSupportStatus
} from '@/api/basket/partnerSupport'
import type {
  PartnerListItem,
  PartnerSupportAddReq,
  PartnerSupportListItem,
  PartnerSupportQuery
} from '@/types'
import {
  PARTNER_SUPPORT_STATUS_OPTIONS,
  PARTNER_SUPPORT_TYPE_OPTIONS
} from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<PartnerSupportListItem[]>([])
const partnerOptions = ref<PartnerListItem[]>([])
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
  partnerId: undefined,
  supportType: 'CASH',
  supportAmount: undefined,
  supportDate: undefined,
  publicDescription: undefined,
  publishStatus: 1,
  sortOrder: 0
})

const rules = {
  partnerId: [{ required: true, message: '请选择合作伙伴', trigger: 'change' }],
  supportType: [{ required: true, message: '请选择支持方式', trigger: 'change' }],
  supportAmount: [{ required: true, message: '请输入支持金额', trigger: 'blur' }],
  supportDate: [{ required: true, message: '请选择支持日期', trigger: 'change' }]
}

function formatAmount(value?: number) {
  const num = Number(value ?? 0)
  return Number.isFinite(num) ? num.toFixed(2) : '0.00'
}

function loadPartnerOptions() {
  listPartner({ pageNum: 1, pageSize: 500 }).then(res => {
    partnerOptions.value = res.rows || []
  })
}

function getList() {
  loading.value = true
  const params = { ...queryParams.value }
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
    partnerId: undefined,
    supportType: 'CASH',
    supportAmount: undefined,
    supportDate: undefined,
    publicDescription: undefined,
    publishStatus: 1,
    sortOrder: 0
  }
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增支持记录'
  dialogOpen.value = true
}

function handleUpdate(row: PartnerSupportListItem) {
  resetFormData()
  currentId.value = row.id
  getPartnerSupport(row.id).then(res => {
    const data = res.data!
    form.value = {
      partnerId: data.partnerId,
      supportType: data.supportType || 'CASH',
      supportAmount: data.supportAmount,
      supportDate: data.supportDate,
      publicDescription: data.publicDescription,
      publishStatus: data.publishStatus ?? 1,
      sortOrder: data.sortOrder ?? 0
    }
    dialogTitle.value = '编辑支持记录'
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
    const req = currentId.value
      ? updatePartnerSupport(currentId.value, form.value)
      : addPartnerSupport(form.value)
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
  proxy.$modal.confirm(`确认公开支持记录 #${row.id}？`).then(() =>
    updatePartnerSupportStatus(row.id, 1)
  ).then(() => {
    proxy.$modal.msgSuccess('启用成功')
    getList()
  }).catch(() => {})
}

function handleDisable(row: PartnerSupportListItem) {
  proxy.$modal.confirm(`确认停用支持记录 #${row.id}？`).then(() =>
    updatePartnerSupportStatus(row.id, 0)
  ).then(() => {
    proxy.$modal.msgSuccess('停用成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row: PartnerSupportListItem) {
  proxy.$modal.confirm(`确认删除支持记录 #${row.id}？`).then(() =>
    delPartnerSupport(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

loadPartnerOptions()
getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
