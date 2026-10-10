<template>
  <div class="app-container">
    <el-alert
      title="配置官网社群及官方媒体入口，修改后将同步展示到官网。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />

    <el-table v-loading="loading" :data="dataList" border>
      <el-table-column label="平台" width="120">
        <template #default="scope">
          <span class="platform-cell">
            <span class="platform-dot" :class="'is-' + (scope.row.platformCode || '').toLowerCase()" />
            {{ scope.row.platformName || SOCIAL_PLATFORM_LABELS[scope.row.platformCode] || scope.row.platformCode }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="标题" prop="title" min-width="140" :show-overflow-tooltip="true" />
      <el-table-column label="描述" prop="subtitle" min-width="180" :show-overflow-tooltip="true" />
      <el-table-column label="按钮文案" prop="buttonText" width="140" :show-overflow-tooltip="true" />
      <el-table-column label="跳转链接" prop="jumpUrl" min-width="220" :show-overflow-tooltip="true">
        <template #default="scope">
          {{ scope.row.jumpUrl || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template #default="scope">
          <dict-tag :options="SOCIAL_LINK_STATUS_OPTIONS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column label="排序" prop="sortOrder" width="70" align="center" />
      <el-table-column label="更新时间" width="170" align="center">
        <template #default="scope">{{ parseTime(scope.row.updateTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="90" class-name="small-padding fixed-width" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['basket:socialLink:edit']">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="编辑社群入口" v-model="dialogOpen" width="640px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="平台">
          <el-input :model-value="platformLabel" disabled />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="描述" prop="subtitle">
          <el-input v-model="form.subtitle" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="按钮文字" prop="buttonText">
          <el-input v-model="form.buttonText" maxlength="32" show-word-limit />
        </el-form-item>
        <el-divider content-position="left">英文内容</el-divider>
        <div class="el-form-item__tip bilingual-tip">英文与这条链接共用平台编码、地址、排序和状态。留空时官网显示中文。</div>
        <el-form-item label="英文标题">
          <el-input v-model="form.titleEn" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="英文描述">
          <el-input v-model="form.subtitleEn" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="英文按钮">
          <el-input v-model="form.buttonTextEn" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="跳转链接" prop="jumpUrl">
          <el-input v-model="form.jumpUrl" maxlength="512" show-word-limit placeholder="可选，https:// 优先" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketSiteSocialLinks">
import { listSocialLinks, updateSocialLink } from '@/api/basket/socialLink'
import type { SiteSocialLinkEditReq, SiteSocialLinkItem } from '@/types'
import { SOCIAL_LINK_STATUS_OPTIONS, SOCIAL_PLATFORM_LABELS } from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<SiteSocialLinkItem[]>([])
const loading = ref(true)
const dialogOpen = ref(false)
const submitLoading = ref(false)
const currentId = ref<number>()
const currentPlatformCode = ref('')

const form = ref<SiteSocialLinkEditReq>({
  title: '',
  titleEn: '',
  subtitle: '',
  subtitleEn: '',
  buttonText: '',
  buttonTextEn: '',
  jumpUrl: '',
  sortOrder: 0,
  status: 1
})

const platformLabel = computed(() =>
  SOCIAL_PLATFORM_LABELS[currentPlatformCode.value] || currentPlatformCode.value
)

function isSafeJumpUrl(url?: string) {
  if (!url || !url.trim()) return true
  const trimmed = url.trim()
  if (trimmed.length > 512) return false
  const lower = trimmed.toLowerCase()
  if (
    lower.startsWith('javascript:') ||
    lower.startsWith('data:') ||
    lower.startsWith('file:') ||
    lower.startsWith('vbscript:')
  ) {
    return false
  }
  try {
    const parsed = new URL(trimmed)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:'
  } catch {
    return false
  }
}

const rules = {
  title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
  buttonText: [{ required: true, message: '按钮文字不能为空', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '排序不能为空', trigger: 'change' }],
  jumpUrl: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (isSafeJumpUrl(value)) {
          callback()
          return
        }
        callback(new Error('跳转链接仅允许 http/https，禁止 javascript/data/file/vbscript'))
      },
      trigger: 'blur'
    }
  ]
}

function getList() {
  loading.value = true
  listSocialLinks()
    .then(res => {
      dataList.value = res.data || []
    })
    .finally(() => {
      loading.value = false
    })
}

function handleUpdate(row: SiteSocialLinkItem) {
  currentId.value = row.id
  currentPlatformCode.value = row.platformCode
  form.value = {
    title: row.title || '',
    titleEn: row.titleEn || '',
    subtitle: row.subtitle || '',
    subtitleEn: row.subtitleEn || '',
    buttonText: row.buttonText || '',
    buttonTextEn: row.buttonTextEn || '',
    jumpUrl: row.jumpUrl || '',
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1
  }
  dialogOpen.value = true
}

function submitForm() {
  if (currentId.value == null) return
  proxy.$refs.formRef.validate((valid: boolean) => {
    if (!valid) return
    submitLoading.value = true
    updateSocialLink(currentId.value!, {
      title: form.value.title.trim(),
      titleEn: (form.value.titleEn || '').trim(),
      subtitle: (form.value.subtitle || '').trim(),
      subtitleEn: (form.value.subtitleEn || '').trim(),
      buttonText: form.value.buttonText.trim(),
      buttonTextEn: (form.value.buttonTextEn || '').trim(),
      jumpUrl: (form.value.jumpUrl || '').trim(),
      sortOrder: form.value.sortOrder,
      status: form.value.status
    })
      .then(() => {
        proxy.$modal.msgSuccess('保存成功')
        dialogOpen.value = false
        getList()
      })
      .finally(() => {
        submitLoading.value = false
      })
  })
}

getList()
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}

.platform-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.platform-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
}

.platform-dot.is-kook {
  background: #5865f2;
}

.platform-dot.is-wechat {
  background: #07c160;
}

.platform-dot.is-bilibili {
  background: #fb7299;
}

.platform-dot.is-douyin {
  background: #111;
}
</style>
