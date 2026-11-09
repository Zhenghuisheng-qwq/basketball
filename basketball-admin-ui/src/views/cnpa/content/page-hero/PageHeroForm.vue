<template>
  <div v-loading="loading">
    <el-alert
      v-if="published"
      title="当前头图已在线上。再保存会立刻换成下面这张图。"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
    />
    <el-descriptions v-if="seasonName" :column="2" border class="mb8">
      <el-descriptions-item label="赛季">{{ seasonName }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ statusText }}</el-descriptions-item>
    </el-descriptions>
    <el-form label-width="120px">
      <el-form-item label="模式">
        <el-radio-group v-model="form.mode">
          <el-radio label="BACKGROUND_TEXT">背景加文字</el-radio>
          <el-radio label="FULL_IMAGE">只显示图片</el-radio>
        </el-radio-group>
        <div class="el-upload__tip">只显示图片时，赛事名不再叠在头图上，名称和时间仍在头图下面的原有区域。背景加文字时，名称、时间和阶段继续读赛季数据。</div>
      </el-form-item>
      <el-form-item label="电脑端图片">
        <content-image-upload v-model="form.imageFileId" v-model:preview="imagePreview" usage-type="PAGE_HERO" tip="建议 21:9。保存后官网立刻使用这张图。" />
      </el-form-item>
      <el-form-item label="手机端图片">
        <content-image-upload v-model="form.mobileImageFileId" v-model:preview="mobilePreview" usage-type="PAGE_HERO" tip="可选。不传时窄屏使用电脑端图片。" />
      </el-form-item>
      <el-form-item label="替代文字">
        <el-input v-model="form.altText" maxlength="256" show-word-limit />
      </el-form-item>
      <el-form-item label="水平焦点">
        <el-slider v-model="form.focusX" :min="0" :max="100" />
      </el-form-item>
      <el-form-item label="垂直焦点">
        <el-slider v-model="form.focusY" :min="0" :max="100" />
      </el-form-item>
      <el-form-item label="遮罩">
        <el-slider v-model="form.maskOpacity" :min="0" :max="100" />
        <div class="el-upload__tip">只压暗头图，不改页面其他区域的颜色。</div>
      </el-form-item>
      <el-form-item label="上架">
        <el-switch v-model="form.draftEnabled" :active-value="1" :inactive-value="0" />
        <div class="el-upload__tip">打开后保存，官网立刻换成这张图。关闭后保存，页面回到原来的静态头图。</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave" v-hasPermi="['basket:pageHero:edit', 'basket:pageHero:publish', 'basket:v4Season:edit', 'basket:v4Season:publish']">保存</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { getPageHero, savePageHero } from '@/api/basket/pageHero'
import ContentImageUpload from '@/components/ContentImageUpload/index.vue'
import type { PageHeroResp, PageHeroSaveReq } from '@/types'

const props = defineProps<{
  pageKey: string
  seasonId?: number
  seasonName?: string
}>()

const { proxy } = getCurrentInstance()
const loading = ref(false)
const published = ref(false)
const statusText = ref('未配置')
const imagePreview = ref('')
const mobilePreview = ref('')
const form = ref<PageHeroSaveReq>(blankForm())

function blankForm(): PageHeroSaveReq {
  return {
    pageKey: props.pageKey,
    seasonId: props.seasonId,
    versionNo: 0,
    mode: 'BACKGROUND_TEXT',
    focusX: 50,
    focusY: 50,
    maskOpacity: 40,
    draftEnabled: 1,
    altText: ''
  }
}

function applyDetail(data: PageHeroResp) {
  const useDraft = data.hasDraft === 1
  published.value = data.heroStatus === 1
  statusText.value = data.pendingChangeLabel || data.heroStatusName || '未配置'
  form.value = {
    pageKey: props.pageKey,
    seasonId: props.seasonId,
    versionNo: data.versionNo ?? 0,
    mode: (useDraft ? data.draftMode : data.mode) || 'BACKGROUND_TEXT',
    imageFileId: useDraft ? (data.draftImageFileId ?? data.imageFileId) : data.imageFileId,
    mobileImageFileId: useDraft ? data.draftMobileImageFileId : data.mobileImageFileId,
    altText: (useDraft ? data.draftAltText : data.altText) || '',
    focusX: (useDraft ? data.draftFocusX : data.focusX) ?? 50,
    focusY: (useDraft ? data.draftFocusY : data.focusY) ?? 50,
    maskOpacity: (useDraft ? data.draftMaskOpacity : data.maskOpacity) ?? 40,
    draftEnabled: data.hasDraft === 1 ? (data.draftEnabled ?? 1) : (data.heroStatus === 2 ? 0 : 1)
  }
  imagePreview.value = (useDraft ? data.draftImageUrl || data.imageUrl : data.imageUrl) || ''
  mobilePreview.value = (useDraft ? data.draftMobileImageUrl : data.mobileImageUrl) || ''
}

function load() {
  loading.value = true
  getPageHero(props.pageKey, props.pageKey === 'EVENT_SEASON' ? props.seasonId : undefined).then(res => {
    applyDetail(res.data || {})
  }).finally(() => {
    loading.value = false
  })
}

function payload(): PageHeroSaveReq {
  return {
    ...form.value,
    pageKey: props.pageKey,
    seasonId: props.pageKey === 'EVENT_SEASON' ? props.seasonId : undefined,
    altText: form.value.altText || ''
  }
}

function handleSave() {
  const shelved = form.value.draftEnabled !== 0
  savePageHero(payload()).then(res => {
    proxy.$modal.msgSuccess(shelved ? '已上架' : '已下架，官网回到原来的头图')
    if (res.data) applyDetail(res.data)
  })
}

watch(() => [props.pageKey, props.seasonId], load, { immediate: true })
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
