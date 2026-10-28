<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="名称" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="合作伙伴名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="partnerStatus">
        <el-select v-model="queryParams.partnerStatus" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="item in PARTNER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:partner:add']">新增合作伙伴</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" class="even-table even-table--7" :data="dataList" border>
      <el-table-column label="Logo" align="center">
        <template #default="scope">
          <el-image v-if="scope.row.logoUrl" :src="scope.row.logoUrl" style="width: 40px; height: 40px" fit="contain" />
          <div v-else class="partner-logo-fallback">{{ partnerInitial(scope.row.partnerName) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="合作伙伴" align="center" :show-overflow-tooltip="true">
        <template #default="scope">
          <div class="partner-name">{{ scope.row.partnerName }}</div>
          <div v-if="scope.row.partnerCode" class="partner-code">{{ scope.row.partnerCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="累计赞助" align="center">
        <template #default="scope">
          <span class="amount-text">{{ formatAmount(scope.row.totalSupportAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="赞助次数" align="center">
        <template #default="scope">
          {{ Number(scope.row.supportCount || 0) }} 次
        </template>
      </el-table-column>
      <el-table-column label="最近赞助" prop="latestSupportTime" align="center">
        <template #default="scope">
          {{ formatDate(scope.row.latestSupportTime) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="partnerStatus" align="center">
        <template #default="scope">
          <dict-tag :options="PARTNER_STATUS_OPTIONS" :value="scope.row.partnerStatus" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:partner:query']">详情</el-button>
          <el-button link type="success" icon="Plus" @click="openSupportDialog(scope.row)" v-hasPermi="['basket:partnerSupport:add']">新增赞助</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:partner:edit']" v-if="[0, 2].includes(scope.row.partnerStatus!)">编辑</el-button>
          <el-button link type="success" @click="handlePublish(scope.row)" v-hasPermi="['basket:partner:publish']" v-if="[0, 2].includes(scope.row.partnerStatus!)">展示</el-button>
          <el-button link type="warning" @click="handleOffline(scope.row)" v-hasPermi="['basket:partner:offline']" v-if="scope.row.partnerStatus === 1">不展示</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="detailOpen" title="合作伙伴详情" width="640px" align-center center append-to-body>
      <template v-if="detail">
        <el-descriptions class="center-desc" :column="1" border>
          <el-descriptions-item label="合作伙伴">{{ detail.partnerName }}</el-descriptions-item>
          <el-descriptions-item label="Logo">
            <el-image v-if="detail.logoUrl" :src="detail.logoUrl" style="width: 120px; height: 80px" fit="contain" />
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.partnerStatusName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="官网">{{ detail.websiteUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简介">{{ detail.publicDescription || '-' }}</el-descriptions-item>
          <el-descriptions-item label="累计赞助">
            <span class="amount-text">{{ formatAmount(detail.totalSupportAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="赞助次数">{{ Number(detail.supportCount || 0) }} 次</el-descriptions-item>
        </el-descriptions>

        <div class="detail-history-title">历史赞助记录</div>
        <el-table v-loading="detailSupportsLoading" class="even-table even-table--3" :data="detailSupports" border size="small" empty-text="暂无赞助记录">
          <el-table-column label="赞助日期" prop="supportDate" align="center" />
          <el-table-column label="赞助金额" align="center">
            <template #default="scope">
              {{ formatAmount(scope.row.supportAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="备注" prop="publicDescription" align="center" :show-overflow-tooltip="true" />
        </el-table>
      </template>
    </el-dialog>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="560px" align-center center append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="名称" prop="partnerName">
          <el-input v-model="form.partnerName" maxlength="100" show-word-limit placeholder="合作伙伴名称" />
        </el-form-item>
        <el-form-item label="Logo">
          <image-upload v-model="logoPreview" :limit="1" :file-size="5" />
        </el-form-item>
        <el-form-item v-if="!currentId" label="展示状态" prop="partnerStatus">
          <el-radio-group v-model="form.partnerStatus">
            <el-radio :label="1">展示</el-radio>
            <el-radio :label="0">暂不展示</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="form.websiteUrl" placeholder="可选，https://" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.publicDescription" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
        <el-collapse v-if="currentId" class="more-settings">
          <el-collapse-item title="更多设置" name="more">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortNo" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="内部备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="新增赞助" v-model="supportDialogOpen" width="480px" align-center center append-to-body>
      <el-form ref="supportFormRef" :model="supportForm" :rules="supportRules" label-width="100px">
        <el-form-item label="合作伙伴">
          <el-input :model-value="supportPartnerName" disabled />
        </el-form-item>
        <el-form-item label="赞助金额" prop="supportAmount">
          <el-input-number
            v-model="supportForm.supportAmount"
            :min="0.01"
            :precision="2"
            :step="1000"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="赞助日期" prop="supportDate">
          <el-date-picker
            v-model="supportForm.supportDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择赞助日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="supportForm.publicDescription" type="textarea" :rows="2" placeholder="可选，如赛事赞助说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supportDialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="supportSubmitLoading" @click="submitSupport">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {
  addPartner,
  getPartner,
  listPartner,
  offlinePartner,
  publishPartner,
  updatePartner
} from '@/api/basket/partner'
import { addPartnerSupport, listPartnerSupport } from '@/api/basket/partnerSupport'
import type {
  PartnerAddReq,
  PartnerListItem,
  PartnerQuery,
  PartnerSupportAddReq,
  PartnerSupportListItem
} from '@/types'
import { PARTNER_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()
const emit = defineEmits<{ (e: 'support-changed'): void }>()

const dataList = ref<PartnerListItem[]>([])
const loading = ref(true)
const submitLoading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<PartnerListItem>()
const detailSupports = ref<PartnerSupportListItem[]>([])
const detailSupportsLoading = ref(false)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const logoPreview = ref('')

const supportDialogOpen = ref(false)
const supportSubmitLoading = ref(false)
const supportPartnerId = ref<number>()
const supportPartnerName = ref('')
const supportForm = ref<PartnerSupportAddReq>({
  partnerId: undefined,
  supportType: 'CASH',
  supportAmount: undefined,
  supportDate: undefined,
  publicDescription: undefined,
  publishStatus: 1
})

const queryParams = ref<PartnerQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<PartnerAddReq>({
  partnerName: '',
  partnerStatus: 1,
  sortNo: 0
})

const rules = {
  partnerName: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
}

const supportRules = {
  supportAmount: [{ required: true, message: '请输入赞助金额', trigger: 'change' }],
  supportDate: [{ required: true, message: '请选择赞助日期', trigger: 'change' }]
}

function formatAmount(value?: number | string) {
  const num = Number(value ?? 0)
  if (!Number.isFinite(num)) return '¥0.00'
  return `¥${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function formatDate(value?: string) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function partnerInitial(name?: string) {
  const text = (name || '合').trim()
  return text.charAt(0).toUpperCase()
}

function todayStr() {
  const d = new Date()
  const m = `${d.getMonth() + 1}`.padStart(2, '0')
  const day = `${d.getDate()}`.padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function getList() {
  loading.value = true
  listPartner(queryParams.value).then(res => {
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
  proxy.resetForm('queryRef')
  handleQuery()
}

function resetFormData() {
  form.value = {
    partnerName: '',
    logoFileId: undefined,
    websiteUrl: undefined,
    publicDescription: undefined,
    partnerStatus: 1,
    sortNo: 0,
    remark: undefined
  }
  logoPreview.value = ''
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增合作伙伴'
  dialogOpen.value = true
}

function handleUpdate(row: PartnerListItem) {
  resetFormData()
  currentId.value = row.id
  getPartner(row.id).then(res => {
    const data = res.data!
    form.value = {
      partnerName: data.partnerName || '',
      logoFileId: data.logoFileId,
      websiteUrl: data.websiteUrl,
      publicDescription: data.publicDescription,
      partnerStatus: data.partnerStatus,
      sortNo: data.sortNo ?? 0,
      remark: data.remark
    }
    logoPreview.value = data.logoUrl || ''
    dialogTitle.value = '编辑合作伙伴'
    dialogOpen.value = true
  })
}

function handleDetail(row: PartnerListItem) {
  detailSupports.value = []
  getPartner(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
  detailSupportsLoading.value = true
  listPartnerSupport({ partnerId: row.id, pageNum: 1, pageSize: 50, supportType: 'CASH' }).then(res => {
    detailSupports.value = res.rows || []
  }).finally(() => {
    detailSupportsLoading.value = false
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    const payload: PartnerAddReq = {
      ...form.value,
      partnerName: form.value.partnerName?.trim()
    }
    const req = currentId.value
      ? updatePartner(currentId.value, payload)
      : addPartner(payload)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

function openSupportDialog(row: PartnerListItem) {
  supportPartnerId.value = row.id
  supportPartnerName.value = row.partnerName || ''
  supportForm.value = {
    partnerId: row.id,
    supportType: 'CASH',
    supportAmount: undefined,
    supportDate: todayStr(),
    publicDescription: undefined,
    publishStatus: 1
  }
  supportDialogOpen.value = true
  nextTick(() => proxy.resetForm('supportFormRef'))
}

function submitSupport() {
  proxy.$refs.supportFormRef.validate((valid: boolean) => {
    if (!valid) return
    const amount = Number(supportForm.value.supportAmount)
    if (!Number.isFinite(amount) || amount <= 0) {
      proxy.$modal.msgError('赞助金额必须大于 0')
      return
    }
    supportSubmitLoading.value = true
    addPartnerSupport({
      partnerId: supportPartnerId.value,
      supportType: 'CASH',
      supportAmount: amount,
      supportDate: supportForm.value.supportDate,
      publicDescription: supportForm.value.publicDescription,
      publishStatus: 1
    }).then(() => {
      proxy.$modal.msgSuccess('赞助记录已新增')
      supportDialogOpen.value = false
      getList()
      emit('support-changed')
    }).finally(() => {
      supportSubmitLoading.value = false
    })
  })
}

function handlePublish(row: PartnerListItem) {
  proxy.$modal.confirm(`确认公开展示合作伙伴「${row.partnerName}」？`).then(() =>
    publishPartner(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('已展示')
    getList()
  }).catch(() => {})
}

function handleOffline(row: PartnerListItem) {
  proxy.$modal.confirm(`确认将「${row.partnerName}」设为不公开展示？历史赞助记录仍会保留。`).then(() =>
    offlinePartner(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('已设为不展示')
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
.partner-logo-fallback {
  width: 40px;
  height: 40px;
  margin: 0 auto;
  border-radius: 6px;
  background: #f0f2f5;
  color: #606266;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}
.partner-name {
  font-weight: 600;
  color: #303133;
  text-align: center;
}
.partner-code {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.amount-text {
  font-weight: 600;
  color: #b88230;
}
.detail-history-title {
  margin: 20px 0 10px;
  font-weight: 600;
  text-align: center;
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
  width: calc(100% / 7) !important;
}
.even-table--3 :deep(col:not([name="gutter"])) {
  width: calc(100% / 3) !important;
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
.more-settings {
  border: none;
}
</style>
