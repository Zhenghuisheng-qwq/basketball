<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="状态" prop="sendStatus">
        <el-select v-model="queryParams.sendStatus" placeholder="发送状态" clearable style="width: 140px">
          <el-option v-for="item in SEND_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="标题" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="通知标题" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:mailNotice:add']">新建通知</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="标题" prop="title" min-width="200" align="center" :show-overflow-tooltip="true" />
      <el-table-column label="状态" prop="sendStatus" width="110" align="center">
        <template #default="scope">
          <el-tag :type="statusTag(scope.row.sendStatus)">{{ scope.row.sendStatusName || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" prop="createTime" min-width="160" align="center">
        <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="成功发送" prop="successCount" width="110" align="center">
        <template #default="scope">
          <el-button link type="primary" @click="openRecipients(scope.row.id, 'all')" v-hasPermi="['basket:mailNotice:query']">{{ scope.row.successCount || 0 }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="280" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="openRecipients(scope.row.id, 'success')" v-hasPermi="['basket:mailNotice:query']">详情</el-button>
          <el-button link type="primary" icon="Document" @click="handlePreview(scope.row)" v-hasPermi="['basket:mailNotice:query']">内容</el-button>
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:mailNotice:edit']" v-if="scope.row.sendStatus === 0">修改</el-button>
          <el-button
            link
            :type="scope.row.sendStatus === 0 ? 'success' : 'info'"
            icon="Promotion"
            :disabled="scope.row.sendStatus !== 0"
            @click="handleSend(scope.row)"
            v-hasPermi="['basket:mailNotice:send']"
          >发送</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['basket:mailNotice:remove']" v-if="scope.row.sendStatus === 0">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="dialogTitle" v-model="dialogOpen" width="860px" align-center append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="邮件主题" />
        </el-form-item>
        <el-form-item label="图文内容" prop="contentHtml">
          <editor v-model="form.contentHtml" :min-height="260" />
        </el-form-item>
      </el-form>
      <p class="form-tip">这里只保存草稿，不会发邮件。发送请到列表里点「发送」，按每秒 5 封发出。同一个邮箱只发一次，开始发送后不能再修改。</p>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="previewOpen" title="通知内容" width="760px" align-center append-to-body>
      <h3 class="preview-title">{{ previewTitle }}</h3>
      <div class="mail-preview" v-html="previewHtml"></div>
    </el-dialog>

    <el-dialog v-model="resultOpen" :title="resultTitle" width="920px" align-center append-to-body @closed="onResultClosed">
      <div class="result-summary">
        <div class="stat-card">
          <span>计划发送</span>
          <strong>{{ resultStats.recipientCount || 0 }}</strong>
        </div>
        <div class="stat-card success">
          <span>已接收</span>
          <strong>{{ resultStats.successCount || 0 }}</strong>
        </div>
        <div class="stat-card danger">
          <span>发送失败</span>
          <strong>{{ resultStats.failCount || 0 }}</strong>
        </div>
        <div class="stat-card">
          <span>待发送</span>
          <strong>{{ pendingCount }}</strong>
        </div>
      </div>
      <el-alert
        class="result-alert"
        :title="resultAlert"
        :type="resultStats.sendStatus === 1 ? 'warning' : 'success'"
        :closable="false"
        show-icon
      />

      <el-form :inline="true" class="result-filter">
        <el-form-item v-if="resultMode === 'all'" label="名单">
          <el-radio-group v-model="recipientFilter" @change="changeRecipientFilter">
            <el-radio-button value="all">全部</el-radio-button>
            <el-radio-button value="success">发送成功</el-radio-button>
            <el-radio-button value="fail">发送失败</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="recipientQuery.keyword" placeholder="邮箱 / 用户名 / 昵称" clearable style="width: 220px" @keyup.enter="changeRecipientFilter" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="changeRecipientFilter">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="recipientLoading" :data="recipientList" border max-height="420" :empty-text="recipientEmptyText">
        <el-table-column label="用户名" prop="username" min-width="120" align="center" :show-overflow-tooltip="true">
          <template #default="scope">{{ scope.row.username || '-' }}</template>
        </el-table-column>
        <el-table-column label="昵称" prop="nickname" min-width="120" align="center" :show-overflow-tooltip="true">
          <template #default="scope">{{ scope.row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="200" align="center" :show-overflow-tooltip="true" />
        <el-table-column label="结果" prop="sendStatusName" width="100" align="center">
          <template #default="scope">
            <el-tag :type="recipientTag(scope.row.sendStatus)">{{ scope.row.sendStatusName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" prop="failReason" min-width="140" align="center" :show-overflow-tooltip="true">
          <template #default="scope">{{ scope.row.failReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="发送时间" prop="sendTime" min-width="160" align="center">
          <template #default="scope">{{ parseTime(scope.row.sendTime) || '-' }}</template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="recipientTotal > 0"
        :total="recipientTotal"
        v-model:page="recipientQuery.pageNum"
        v-model:limit="recipientQuery.pageSize"
        :auto-scroll="false"
        @pagination="refreshResult()"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketMailNotice">
import {
  addMailNotice,
  delMailNotice,
  getMailNotice,
  listMailNotice,
  listMailNoticeRecipients,
  sendMailNotice,
  updateMailNotice
} from '@/api/basket/mailNotice'
import type { MailNoticeItem, MailNoticeQuery, MailNoticeRecipient, MailNoticeRecipientQuery, MailNoticeSaveReq } from '@/types'

const SEND_STATUS_OPTIONS = [
  { value: 0, label: '草稿' },
  { value: 1, label: '发送中' },
  { value: 2, label: '已完成' }
]

const { proxy } = getCurrentInstance()!

const dataList = ref<MailNoticeItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const dialogOpen = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const currentId = ref<number>()
const formRef = ref()
const previewOpen = ref(false)
const previewTitle = ref('')
const previewHtml = ref('')
const resultOpen = ref(false)
const resultId = ref<number>()
const resultStats = ref<MailNoticeItem>({} as MailNoticeItem)
const recipientLoading = ref(false)
const recipientList = ref<MailNoticeRecipient[]>([])
const recipientTotal = ref(0)
const resultMode = ref<'success' | 'all'>('all')
const recipientFilter = ref<'success' | 'fail' | 'all'>('all')

const queryParams = ref<MailNoticeQuery>({
  pageNum: 1,
  pageSize: 10
})
const form = ref<MailNoticeSaveReq>({
  title: '',
  contentHtml: ''
})
const recipientQuery = ref<MailNoticeRecipientQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: ''
})

const rules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  contentHtml: [{ required: true, message: '请填写图文内容', trigger: 'blur' }]
}

const pendingCount = computed(() => {
  const planned = resultStats.value.recipientCount || 0
  const success = resultStats.value.successCount || 0
  const fail = resultStats.value.failCount || 0
  return Math.max(planned - success - fail, 0)
})

const resultTitle = computed(() => (resultMode.value === 'success' ? '发送成功' : '全部发送'))

const resultAlert = computed(() => {
  const success = resultStats.value.successCount || 0
  if (resultMode.value === 'success') {
    if (resultStats.value.sendStatus === 1) {
      return `正在发送。目前已有 ${success} 人成功收到这封邮件。`
    }
    return `共 ${success} 人成功收到这封邮件。`
  }
  const planned = resultStats.value.recipientCount || 0
  if (resultStats.value.sendStatus === 1) {
    return `正在发送。计划 ${planned} 人，目前 ${success} 人成功收到。`
  }
  if (resultStats.value.sendStatus === 0) {
    return '尚未发送。'
  }
  return `发送完成。计划 ${planned} 人，成功 ${success} 人。`
})

const recipientEmptyText = computed(() => {
  if (recipientFilter.value === 'success') return '还没有发送成功的邮箱'
  if (recipientFilter.value === 'fail') return '没有发送失败的记录'
  return '暂无收件记录'
})

let pollTimer: ReturnType<typeof setTimeout> | undefined
let refreshSeq = 0

function statusTag(status?: number) {
  if (status === 1) return 'warning'
  if (status === 2) return 'success'
  return 'info'
}

function recipientTag(status?: number) {
  if (status === 1) return 'success'
  if (status === 2) return 'danger'
  return 'info'
}

function hasContent(html?: string) {
  if (!html) return false
  if (/<img[\s>]/i.test(html)) return true
  const text = html.replace(/<[^>]+>/g, '').replace(/&nbsp;/gi, ' ').replace(/\u00a0/g, ' ').trim()
  return text.length > 0
}

function getList() {
  loading.value = true
  listMailNotice(queryParams.value).then(res => {
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
  form.value = { title: '', contentHtml: '<p></p>' }
  currentId.value = undefined
  nextTick(() => formRef.value?.clearValidate?.())
}

function handleAdd() {
  resetFormData()
  dialogTitle.value = '新建通知'
  dialogOpen.value = true
}

function handleUpdate(row: MailNoticeItem) {
  getMailNotice(row.id).then(res => {
    const data = res.data
    currentId.value = row.id
    form.value = {
      title: data?.title || '',
      contentHtml: data?.contentHtml || '<p></p>'
    }
    dialogTitle.value = '修改通知'
    dialogOpen.value = true
  })
}

function handlePreview(row: MailNoticeItem) {
  getMailNotice(row.id).then(res => {
    previewTitle.value = res.data?.title || ''
    previewHtml.value = res.data?.contentHtml || '<p>暂无内容</p>'
    previewOpen.value = true
  })
}

function submitForm() {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    if (!hasContent(form.value.contentHtml)) {
      proxy.$modal.msgError('请填写图文内容')
      return
    }
    submitLoading.value = true
    const request = currentId.value
      ? updateMailNotice(currentId.value, form.value)
      : addMailNotice(form.value)
    request.then(() => {
      dialogOpen.value = false
      proxy.$modal.msgSuccess('已保存')
      getList()
    }).catch(() => {}).finally(() => {
      submitLoading.value = false
    })
  })
}

function handleSend(row: MailNoticeItem) {
  proxy.$modal.confirm(`确认向全部已注册邮箱发送「${row.title}」？将按每秒 5 封发出，发送后不能再修改。`).then(() =>
    sendMailNotice(row.id)
  ).then(() => {
    getList()
    openRecipients(row.id, 'success')
  }).catch(() => {})
}

function handleDelete(row: MailNoticeItem) {
  proxy.$modal.confirm(`确认删除通知「${row.title}」？`).then(() =>
    delMailNotice(row.id)
  ).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

function recipientStatusParam() {
  if (resultMode.value === 'success') return 1
  if (recipientFilter.value === 'fail') return 2
  if (recipientFilter.value === 'success') return 1
  return undefined
}

function clearPollTimer() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

function stopPoll() {
  refreshSeq += 1
  clearPollTimer()
}

function onResultClosed() {
  const status = resultStats.value.sendStatus
  stopPoll()
  if (status === 1 || status === 2) getList()
}

function changeRecipientFilter() {
  recipientQuery.value.pageNum = 1
  refreshResult()
}

function openRecipients(id: number, mode: 'success' | 'all') {
  stopPoll()
  resultMode.value = mode
  resultId.value = id
  resultStats.value = {} as MailNoticeItem
  recipientList.value = []
  recipientTotal.value = 0
  recipientFilter.value = mode === 'success' ? 'success' : 'all'
  recipientQuery.value = { pageNum: 1, pageSize: 10, keyword: '' }
  resultOpen.value = true
  refreshResult()
}

function refreshResult() {
  if (!resultId.value) return
  clearPollTimer()
  const seq = ++refreshSeq
  const silent = recipientList.value.length > 0
  if (!silent) recipientLoading.value = true
  const noticeId = resultId.value
  Promise.all([
    getMailNotice(noticeId),
    listMailNoticeRecipients(noticeId, {
      pageNum: recipientQuery.value.pageNum,
      pageSize: recipientQuery.value.pageSize,
      keyword: recipientQuery.value.keyword,
      sendStatus: recipientStatusParam()
    })
  ]).then(([detailRes, pageRes]) => {
    if (seq !== refreshSeq || !resultOpen.value) return
    const previousStatus = resultStats.value.sendStatus
    resultStats.value = detailRes.data || ({} as MailNoticeItem)
    recipientList.value = pageRes.rows || []
    recipientTotal.value = pageRes.total || 0
    if (detailRes.data?.sendStatus === 1) {
      pollTimer = setTimeout(() => {
        if (seq === refreshSeq) refreshResult()
      }, 2000)
    } else if (previousStatus === 1) {
      getList()
    }
  }).catch(() => {
    stopPoll()
  }).finally(() => {
    if (seq === refreshSeq) recipientLoading.value = false
  })
}

onBeforeUnmount(() => {
  stopPoll()
})

getList()
</script>

<style scoped>
.form-tip {
  margin: 0 0 0 88px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.preview-title {
  margin: 0 0 12px;
}

.mail-preview {
  min-height: 120px;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  line-height: 1.7;
  overflow: auto;
}

.mail-preview :deep(img) {
  max-width: 100%;
  height: auto;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.stat-card {
  padding: 12px 14px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.stat-card span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  line-height: 1.2;
}

.stat-card.success strong {
  color: var(--el-color-success);
}

.stat-card.danger strong {
  color: var(--el-color-danger);
}

.result-alert {
  margin-bottom: 12px;
}

.result-filter {
  margin-bottom: 8px;
}
</style>
