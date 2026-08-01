<template>
  <div class="app-container">
    <el-alert
      title="首页 Banner 管理：不会替换官网静态资源 home_top.png，仅管理运营可配置的横幅位。"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="位置" prop="positionCode">
        <el-select v-model="queryParams.positionCode" placeholder="位置" clearable style="width: 160px">
          <el-option v-for="item in HOME_BANNER_POSITION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="bannerStatus">
        <el-select v-model="queryParams.bannerStatus" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="item in HOME_BANNER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="名称/标题" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:homeContent:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="名称" prop="bannerName" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="标题" prop="title" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="位置" prop="positionCode" width="120" />
      <el-table-column label="图片" width="90">
        <template #default="scope">
          <el-image v-if="scope.row.imageUrl" :src="scope.row.imageUrl" style="width: 56px; height: 36px" fit="cover" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="跳转" prop="targetType" width="110" />
      <el-table-column label="状态" prop="bannerStatus" width="100">
        <template #default="scope">
          <dict-tag :options="HOME_BANNER_STATUS_OPTIONS" :value="scope.row.bannerStatus" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortNo" width="70" />
      <el-table-column label="生效时间" min-width="200">
        <template #default="scope">
          {{ parseTime(scope.row.startTime) || '-' }} ~ {{ parseTime(scope.row.endTime) || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:homeContent:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:homeContent:edit']" v-if="[0, 2].includes(scope.row.bannerStatus!)">修改</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:homeContent:publish']" v-if="[0, 2].includes(scope.row.bannerStatus!)">发布</el-button>
          <el-button link type="warning" icon="Download" @click="handleOffline(scope.row)" v-hasPermi="['basket:homeContent:offline']" v-if="scope.row.bannerStatus === 1">下线</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:homeContent:remove']" v-if="[0, 2].includes(scope.row.bannerStatus!)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="横幅详情" size="560px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="名称">{{ detail.bannerName }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title || '-' }}</el-descriptions-item>
        <el-descriptions-item label="副标题">{{ detail.subtitle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="位置">{{ detail.positionCode }}</el-descriptions-item>
        <el-descriptions-item label="跳转类型">{{ detail.targetType }}</el-descriptions-item>
        <el-descriptions-item label="跳转值">{{ detail.targetValue || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.bannerStatusName }}</el-descriptions-item>
        <el-descriptions-item label="图片">
          <el-image v-if="detail.imageUrl" :src="detail.imageUrl" style="width: 240px; height: 120px" fit="cover" />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="名称" prop="bannerName">
          <el-input v-model="form.bannerName" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" />
        </el-form-item>
        <el-form-item label="图片文件ID" prop="imageFileId">
          <el-input-number v-model="form.imageFileId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图片预览">
          <image-upload v-model="imagePreview" :limit="1" :file-size="5" />
          <div class="el-upload__tip">预览上传走 /common/upload；提交请填写 basket_file_asset 文件 ID。不会替换 home_top.png。</div>
        </el-form-item>
        <el-form-item label="位置">
          <el-select v-model="form.positionCode" style="width: 100%">
            <el-option v-for="item in HOME_BANNER_POSITION_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转类型">
          <el-select v-model="form.targetType" style="width: 100%">
            <el-option v-for="item in HOME_BANNER_TARGET_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转值">
          <el-input v-model="form.targetValue" placeholder="赛事/公告/球队/球员 ID 或外链 URL" />
        </el-form-item>
        <el-form-item label="生效开始">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效结束">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketHomeContent">
import {
  addHomeBanner,
  delHomeBanner,
  getHomeBanner,
  listHomeBanner,
  offlineHomeBanner,
  publishHomeBanner,
  updateHomeBanner
} from '@/api/basket/homeContent'
import type { HomeBannerAddReq, HomeBannerListItem, HomeBannerQuery } from '@/types'
import {
  HOME_BANNER_POSITION_OPTIONS,
  HOME_BANNER_STATUS_OPTIONS,
  HOME_BANNER_TARGET_TYPE_OPTIONS
} from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<HomeBannerListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<HomeBannerListItem>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const imagePreview = ref('')

const queryParams = ref<HomeBannerQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<HomeBannerAddReq>({
  bannerName: '',
  positionCode: 'HOME_TOP',
  targetType: 'NONE',
  sortNo: 0
})

const rules = {
  bannerName: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  imageFileId: [{ required: true, message: '图片文件ID不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listHomeBanner(queryParams.value).then(res => {
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
    bannerName: '',
    title: undefined,
    subtitle: undefined,
    imageFileId: undefined,
    positionCode: 'HOME_TOP',
    targetType: 'NONE',
    targetValue: undefined,
    startTime: undefined,
    endTime: undefined,
    sortNo: 0
  }
  imagePreview.value = ''
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增首页横幅'
  dialogOpen.value = true
}

function handleUpdate(row: HomeBannerListItem) {
  resetFormData()
  currentId.value = row.id
  getHomeBanner(row.id).then(res => {
    const data = res.data!
    form.value = {
      bannerName: data.bannerName || '',
      title: data.title,
      subtitle: data.subtitle,
      imageFileId: data.imageFileId,
      positionCode: data.positionCode || 'HOME_TOP',
      targetType: data.targetType || 'NONE',
      targetValue: data.targetValue,
      startTime: data.startTime,
      endTime: data.endTime,
      sortNo: data.sortNo ?? 0
    }
    imagePreview.value = data.imageUrl || ''
    dialogTitle.value = '修改首页横幅'
    dialogOpen.value = true
  })
}

function handleDetail(row: HomeBannerListItem) {
  getHomeBanner(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateHomeBanner(currentId.value, form.value)
      : addHomeBanner(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: HomeBannerListItem) {
  proxy.$modal.confirm(`确认发布横幅「${row.bannerName}」？`).then(() =>
    publishHomeBanner(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  }).catch(() => {})
}

function handleOffline(row: HomeBannerListItem) {
  proxy.$modal.confirm(`确认下线横幅「${row.bannerName}」？`).then(() =>
    offlineHomeBanner(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('下线成功')
    getList()
  }).catch(() => {})
}

function handleDelete(row: HomeBannerListItem) {
  proxy.$modal.confirm(`确认删除横幅「${row.bannerName}」？`).then(() =>
    delHomeBanner(row.id)
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
