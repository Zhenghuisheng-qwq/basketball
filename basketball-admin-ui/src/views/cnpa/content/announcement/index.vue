<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="分类" prop="categoryCode">
        <el-select v-model="queryParams.categoryCode" placeholder="公告分类" clearable style="width: 160px">
          <el-option v-for="item in ANNOUNCEMENT_CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="announcementStatus">
        <el-select v-model="queryParams.announcementStatus" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="item in ANNOUNCEMENT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="置顶" prop="isTop">
        <el-select v-model="queryParams.isTop" placeholder="置顶" clearable style="width: 120px">
          <el-option label="是" :value="1" />
          <el-option label="否" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="标题/摘要" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:announcement:add']">新增</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="编号" prop="announcementNo" width="140" />
      <el-table-column label="标题" prop="title" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="分类" prop="categoryName" width="110" />
      <el-table-column label="赛季" prop="seasonName" width="120" :show-overflow-tooltip="true" />
      <el-table-column label="规则版本" prop="ruleVersion" width="100" />
      <el-table-column label="状态" prop="announcementStatus" width="100">
        <template #default="scope">
          <dict-tag :options="ANNOUNCEMENT_STATUS_OPTIONS" :value="scope.row.announcementStatus" />
        </template>
      </el-table-column>
      <el-table-column label="置顶" prop="isTop" width="70">
        <template #default="scope">{{ scope.row.isTop === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="重要" prop="isImportant" width="70">
        <template #default="scope">{{ scope.row.isImportant === 1 ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="发布时间" prop="publishTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.publishTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="420" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:announcement:query']">详情</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:announcement:edit']" v-if="[0, 2].includes(scope.row.announcementStatus!)">修改</el-button>
          <el-button link type="warning" icon="RefreshRight" @click="openSupersede(scope.row)" v-hasPermi="['basket:announcement:edit']" v-if="scope.row.categoryCode === 'RULE' && [0, 2].includes(scope.row.announcementStatus!)">替代旧规则</el-button>
          <el-button link type="success" icon="Upload" @click="handlePublish(scope.row)" v-hasPermi="['basket:announcement:publish']" v-if="[0, 2].includes(scope.row.announcementStatus!)">发布</el-button>
          <el-button link type="warning" icon="Download" @click="handleOffline(scope.row)" v-hasPermi="['basket:announcement:offline']" v-if="scope.row.announcementStatus === 1">下线</el-button>
          <el-button link type="primary" icon="ArrowUp" @click="handleTop(scope.row)" v-hasPermi="['basket:announcement:top']" v-if="scope.row.announcementStatus === 1 && scope.row.isTop !== 1">置顶</el-button>
          <el-button link type="info" icon="ArrowDown" @click="handleUntop(scope.row)" v-hasPermi="['basket:announcement:top']" v-if="scope.row.isTop === 1">取消置顶</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:announcement:remove']" v-if="[0, 2].includes(scope.row.announcementStatus!)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="公告详情" size="640px" append-to-body>
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="编号">{{ detail.announcementNo }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName || detail.categoryCode }}</el-descriptions-item>
        <el-descriptions-item label="赛季">{{ detail.seasonName || detail.seasonId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规则版本">{{ detail.ruleVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="定时发布">{{ parseTime(detail.scheduledPublishTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="被替代为">{{ detail.supersededBy || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.announcementStatusName }}</el-descriptions-item>
        <el-descriptions-item label="摘要">{{ detail.summary || '-' }}</el-descriptions-item>
        <el-descriptions-item label="正文">
          <pre class="content-text">{{ detail.contentText || '-' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="封面">
          <el-image v-if="detail.coverUrl" :src="detail.coverUrl" style="width: 160px; height: 90px" fit="cover" />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类" prop="categoryCode">
          <el-select v-model="form.categoryCode" style="width: 100%">
            <el-option v-for="item in ANNOUNCEMENT_CATEGORY_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <template v-if="form.categoryCode === 'RULE'">
          <el-form-item label="所属赛事">
            <el-select v-model="form.tournamentId" filterable clearable style="width: 100%" @change="loadFormSeasons">
              <el-option v-for="item in tournamentOptions" :key="item.id" :label="item.tournamentName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="赛季">
            <el-select v-model="form.seasonId" filterable clearable style="width: 100%">
              <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="规则版本">
            <el-input v-model="form.ruleVersion" placeholder="如 v2026.1" maxlength="64" />
          </el-form-item>
          <el-form-item label="定时发布">
            <el-date-picker v-model="form.scheduledPublishTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="可选" style="width: 100%" />
          </el-form-item>
        </template>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="300" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="contentText">
          <el-input v-model="form.contentText" type="textarea" :rows="10" placeholder="纯文本正文，不支持 HTML" />
        </el-form-item>
        <el-form-item label="封面文件ID">
          <el-input-number v-model="form.coverFileId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="封面预览">
          <image-upload v-model="coverPreview" :limit="1" :file-size="5" />
          <div class="el-upload__tip">预览上传走 /common/upload；提交请填写 basket_file_asset 文件 ID</div>
        </el-form-item>
        <el-form-item label="重要">
          <el-switch v-model="form.isImportant" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog title="替代旧规则公告" v-model="supersedeOpen" width="560px" append-to-body>
      <el-form :model="supersedeForm" label-width="110px">
        <el-form-item label="新公告">{{ supersedeTarget?.title }}</el-form-item>
        <el-form-item label="旧公告ID" required>
          <el-input-number v-model="supersedeForm.oldAnnouncementId" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赛季">
          <el-select v-model="supersedeForm.seasonId" filterable clearable style="width: 100%">
            <el-option v-for="item in formSeasonOptions" :key="item.id" :label="item.seasonName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则版本">
          <el-input v-model="supersedeForm.ruleVersion" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supersedeOpen = false">取消</el-button>
        <el-button type="primary" @click="submitSupersede">确认绑定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketAnnouncement">
import {
  addAnnouncement,
  delAnnouncement,
  getAnnouncement,
  listAnnouncement,
  offlineAnnouncement,
  publishAnnouncement,
  supersedeRuleAnnouncement,
  topAnnouncement,
  untopAnnouncement,
  updateAnnouncement
} from '@/api/basket/announcement'
import { listV4Series } from '@/api/cnpa/event-v4/series'
import { listV4Season } from '@/api/cnpa/event-v4/season'
import type {
  AnnouncementAddReq,
  AnnouncementDetail,
  AnnouncementListItem,
  AnnouncementQuery,
  AnnouncementRuleSupersedeReq,
  SeasonListItem,
  TournamentListItem
} import { listV4Series } from '@/api/cnpa/event-v4/series'
import { listV4Season } from '@/api/cnpa/event-v4/season'
from '@/types'
import { ANNOUNCEMENT_CATEGORY_OPTIONS, ANNOUNCEMENT_STATUS_OPTIONS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<AnnouncementListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detail = ref<AnnouncementDetail>()
const dialogOpen = ref(false)
const dialogTitle = ref('')
const currentId = ref<number>()
const coverPreview = ref('')
const tournamentOptions = ref<TournamentListItem[]>([])
const formSeasonOptions = ref<SeasonListItem[]>([])
const supersedeOpen = ref(false)
const supersedeTarget = ref<AnnouncementListItem>()
const supersedeForm = ref<AnnouncementRuleSupersedeReq>({ oldAnnouncementId: 0 })

const queryParams = ref<AnnouncementQuery>({
  pageNum: 1,
  pageSize: 10
})

const form = ref<AnnouncementAddReq>({
  categoryCode: 'NOTICE',
  title: '',
  contentText: '',
  publicScope: 'PUBLIC',
  isImportant: 0
})

function loadTournaments() {
  listV4Series().then(res => {
    tournamentOptions.value = (res.data || []).map((s: any) => ({ id: s.id, tournamentName: s.seriesName }))
  })
}

function loadFormSeasons() {
  form.value.seasonId = undefined
  formSeasonOptions.value = []
  if (!form.value.tournamentId) return
  listV4Season({ seriesId: form.value.tournamentId, pageNum: 1, pageSize: 500 }).then(res => {
    formSeasonOptions.value = res.rows || []
  })
}

const rules = {
  categoryCode: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  contentText: [{ required: true, message: '正文不能为空', trigger: 'blur' }]
}

function getList() {
  loading.value = true
  listAnnouncement(queryParams.value).then(res => {
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
    categoryCode: 'NOTICE',
    title: '',
    summary: undefined,
    coverFileId: undefined,
    contentText: '',
    publicScope: 'PUBLIC',
    isImportant: 0
  }
  coverPreview.value = ''
  proxy.resetForm('formRef')
}

function handleAdd() {
  resetFormData()
  currentId.value = undefined
  dialogTitle.value = '新增公告'
  dialogOpen.value = true
}

function handleUpdate(row: AnnouncementListItem) {
  resetFormData()
  currentId.value = row.id
  getAnnouncement(row.id).then(res => {
    const data = res.data!
    form.value = {
      categoryCode: data.categoryCode,
      title: data.title || '',
      summary: data.summary,
      coverFileId: data.coverFileId,
      contentText: data.contentText || '',
      publicScope: data.publicScope || 'PUBLIC',
      isImportant: data.isImportant ?? 0,
      tournamentId: data.tournamentId,
      seasonId: data.seasonId,
      ruleVersion: data.ruleVersion,
      scheduledPublishTime: data.scheduledPublishTime
    }
    if (data.tournamentId) {
      listSeasonByTournament(data.tournamentId).then(res => {
        formSeasonOptions.value = res.data || []
      })
    }
    coverPreview.value = data.coverUrl || ''
    dialogTitle.value = '修改公告'
    dialogOpen.value = true
  })
}

function handleDetail(row: AnnouncementListItem) {
  getAnnouncement(row.id).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function submitForm() {
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    const req = currentId.value
      ? updateAnnouncement(currentId.value, form.value)
      : addAnnouncement(form.value)
    req.then(() => {
      proxy.$modal.msgSuccess(currentId.value ? '修改成功' : '新增成功')
      dialogOpen.value = false
      getList()
    })
  })
}

function handlePublish(row: AnnouncementListItem) {
  proxy.$modal.confirm(`确认发布公告「${row.title}」？`).then(() =>
    publishAnnouncement(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('发布成功')
    getList()
  }).catch(() => {})
}

function handleOffline(row: AnnouncementListItem) {
  proxy.$modal.confirm(`确认下线公告「${row.title}」？`).then(() =>
    offlineAnnouncement(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('下线成功')
    getList()
  }).catch(() => {})
}

function handleTop(row: AnnouncementListItem) {
  proxy.$modal.confirm(`确认置顶公告「${row.title}」？`).then(() =>
    topAnnouncement(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('置顶成功')
    getList()
  }).catch(() => {})
}

function handleUntop(row: AnnouncementListItem) {
  proxy.$modal.confirm(`确认取消置顶「${row.title}」？`).then(() =>
    untopAnnouncement(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('已取消置顶')
    getList()
  }).catch(() => {})
}

function handleDelete(row: AnnouncementListItem) {
  proxy.$modal.confirm(`确认删除公告「${row.title}」？`).then(() =>
    delAnnouncement(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function openSupersede(row: AnnouncementListItem) {
  supersedeTarget.value = row
  supersedeForm.value = {
    oldAnnouncementId: 0,
    seasonId: row.seasonId,
    tournamentId: row.tournamentId,
    ruleVersion: row.ruleVersion
  }
  if (row.tournamentId) {
    listSeasonByTournament(row.tournamentId).then(res => {
      formSeasonOptions.value = res.data || []
    })
  }
  supersedeOpen.value = true
}

function submitSupersede() {
  if (!supersedeTarget.value?.id || !supersedeForm.value.oldAnnouncementId) {
    proxy.$modal.msgWarning('请填写旧公告 ID')
    return
  }
  supersedeRuleAnnouncement(supersedeTarget.value.id, supersedeForm.value).then(() => {
    proxy.$modal.msgSuccess('已绑定替代关系，发布新公告后将自动下线旧规则')
    supersedeOpen.value = false
    getList()
  })
}

loadTournaments()
getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.content-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}
</style>
