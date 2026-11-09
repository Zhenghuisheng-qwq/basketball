<template>
  <div v-loading="loading">
    <el-alert
      v-if="published"
      title="当前头图已在线上。保存只写入草稿，官网要等发布后才改变。发布失败时线上仍是上一张。"
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
      <el-form-item label="电脑图文件ID">
        <el-input-number v-model="form.imageFileId" :min="1" :value-on-clear="null" controls-position="right" style="width: 100%" />
      </el-form-item>
      <el-form-item label="电脑图预览">
        <image-upload v-model="imagePreview" :limit="1" :file-size="5" />
        <div class="el-upload__tip">建议 21:9。/common/upload 只返回图片地址，没有 basket_file_asset.id，预览不会写入文件 ID。</div>
      </el-form-item>
      <el-form-item label="手机图文件ID">
        <el-input-number v-model="form.mobileImageFileId" :min="1" :value-on-clear="null" controls-position="right" style="width: 100%" />
        <el-button v-if="form.mobileImageFileId" link type="primary" @click="form.mobileImageFileId = undefined">清除</el-button>
      </el-form-item>
      <el-form-item label="手机图预览">
        <image-upload v-model="mobilePreview" :limit="1" :file-size="5" />
        <div class="el-upload__tip">可选。不填时窄屏使用电脑图。</div>
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
      <el-form-item label="发布后显示">
        <el-switch v-model="form.draftEnabled" :active-value="1" :inactive-value="0" />
        <div class="el-upload__tip">关闭后发布，官网回到这页原来的静态头图。</div>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave" v-hasPermi="['basket:pageHero:edit', 'basket:v4Season:edit']">保存草稿</el-button>
        <el-button type="success" @click="handlePublish" v-hasPermi="['basket:pageHero:publish', 'basket:v4Season:publish']">发布头图</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { getPageHero, publishPageHero, savePageHero } from '@/api/basket/pageHero'
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
  savePageHero(payload()).then(res => {
    proxy.$modal.msgSuccess('草稿已保存，官网不变')
    if (res.data) applyDetail(res.data)
  })
}

function handlePublish() {
  proxy.$modal.confirm('发布后这页头图换成当前内容。发布失败时线上不变。').then(() =>
    publishPageHero(payload())
  ).then((res: { msg?: string }) => {
    proxy.$modal.msgSuccess(res?.msg || '头图已发布')
    load()
  }).catch(() => {})
}

watch(() => [props.pageKey, props.seasonId], load, { immediate: true })
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
