<template>
  <div>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="名称" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="合作伙伴名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="编号" prop="partnerCode">
        <el-input v-model="queryParams.partnerCode" placeholder="合作伙伴编号" clearable style="width: 160px" @keyup.enter="handleQuery" />
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
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:partner:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="编号" prop="partnerCode" width="140" />
      <el-table-column label="名称" prop="partnerName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="Logo" width="80">
        <template #default="scope">
          <el-image v-if="scope.row.logoUrl" :src="scope.row.logoUrl" style="width: 40px; height: 40px" fit="contain" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="官网" prop="websiteUrl" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="partnerStatus" width="100">
        <template #default="scope">
          <dict-tag :options="PARTNER_STATUS_OPTIONS" :value="scope.row.partnerStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortNo" width="70" />
      <el-table-column label="创建时间" prop="createTime" width="170" />
      <el-table-column label="更新时间" prop="updateTime" width="170" />
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:partner:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:partner:edit']" v-if="[0, 2].includes(scope.row.partnerStatus!)">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:partner:publish']" v-if="[0, 2].includes(scope.row.partnerStatus!)">启用</el-button>
          <el-button link type="warning" icon="Download" @click="handleOffline(scope.row)" v-hasPermi="['basket:partner:offline']" v-if="scope.row.partnerStatus === 1">停用</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:partner:remove']" v-if="[0, 2].includes(scope.row.partnerStatus!)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="合作伙伴详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="编号">{{ detail.partnerCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.partnerName }}</el-descriptions-item>
        <el-descriptions-item label="官网">{{ detail.websiteUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="公开简介">{{ detail.publicDescription || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.partnerStatusName }}</el-descriptions-item>
        <el-descriptions-item label="内部备注">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Logo">
          <el-image v-if="detail.logoUrl" :src="detail.logoUrl" style="width: 120px; height: 80px" fit="contain" />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="partnerName">
          <el-input v-model="form.partnerName" />
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="form.websiteUrl" placeholder="https://" />
        </el-form-item>
        <el-form-item label="公开简介">
          <el-input v-model="form.publicDescription" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="Logo">
          <image-upload v-model="logoPreview" :limit="1" :file-size="5" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="内部备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
  addPartner,
  delPartner,
  getPartner,
  listPartner,
  offlinePartner,
  publishPartner,
  updatePartner
} from '@/api/basket/partner'
import type { PartnerAddReq, PartnerListItem, PartnerQuery } from '@/types'
import { PARTNER_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<PartnerListItem[]>([])
const loading = ref(true)
const submitLoading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<PartnerListItem>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const logoPreview = ref('')

const queryParams = ref<PartnerQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<PartnerAddReq>({
  partnerName: '',
  sortNo: 0
})

const rules = {
  partnerName: [{ required: true, message: '名称不能为空', trigger: 'blur' }]
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
      sortNo: data.sortNo ?? 0,
      remark: data.remark
    }
    logoPreview.value = data.logoUrl || ''
    dialogTitle.value = '修改合作伙伴'
    dialogOpen.value = true
  })
}

function handleDetail(row: PartnerListItem) {
  getPartner(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    const req = currentId.value
      ? updatePartner(currentId.value, form.value)
      : addPartner(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

function handlePublish(row: PartnerListItem) {
  proxy.$modal.confirm(`确认启用合作伙伴「${row.partnerName}」？`).then(() =>
    publishPartner(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('启用成功')
    getList()
  }).catch(() => {})
}

function handleOffline(row: PartnerListItem) {
  proxy.$modal.confirm(`确认停用合作伙伴「${row.partnerName}」？`).then(() =>
    offlinePartner(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('停用成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row: PartnerListItem) {
  proxy.$modal.confirm(`确认删除合作伙伴「${row.partnerName}」？`).then(() =>
    delPartner(row.id)
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
