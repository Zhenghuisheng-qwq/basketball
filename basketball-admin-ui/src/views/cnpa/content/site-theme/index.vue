<template>
  <div class="app-container">
    <el-alert
      title="选全站会同时换首页和其余公开页。某一届赛事可以单独再发一次。球队页、球员页和个人中心不跟某一届赛事的主题走。恢复默认只解除绑定，不删除主题。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />
    <el-row class="mb8">
      <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['basket:siteTheme:edit']">新增主题</el-button>
    </el-row>
    <el-table v-loading="loading" :data="themes" border>
      <el-table-column label="名称" prop="themeName" min-width="140" />
      <el-table-column label="状态" width="120">
        <template #default="scope">{{ scope.row.pendingChangeLabel || scope.row.themeStatusName }}</template>
      </el-table-column>
      <el-table-column label="当前绑定" min-width="220">
        <template #default="scope">
          <template v-if="scope.row.bindings?.length">
            <el-tag v-for="item in scope.row.bindings" :key="item.id" class="bind-tag" closable @close="handleUnbind(item)">
              {{ item.scopeLabel }}
            </el-tag>
          </template>
          <span v-else>未绑定</span>
        </template>
      </el-table-column>
      <el-table-column label="主色" width="90">
        <template #default="scope">
          <span class="swatch" :style="{ background: scope.row.colors?.primary || '#e10613' }" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <el-button link type="primary" @click="handleEdit(scope.row)" v-hasPermi="['basket:siteTheme:edit']">修改</el-button>
          <el-button link type="primary" @click="handleCopy(scope.row)" v-hasPermi="['basket:siteTheme:edit']">复制</el-button>
          <el-button link type="success" @click="openPublish(scope.row)" v-hasPermi="['basket:siteTheme:publish']">发布</el-button>
          <el-button link type="warning" @click="handleOffline(scope.row)" v-hasPermi="['basket:siteTheme:publish']" v-if="scope.row.themeStatus === 1">停用</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row)" v-hasPermi="['basket:siteTheme:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editorOpen" :title="form.id ? '修改主题' : '新增主题'" width="760px" append-to-body>
      <el-alert v-if="published" title="已发布主题再保存，只写入草稿，官网不变。" type="warning" :closable="false" show-icon class="mb8" />
      <el-form label-width="120px">
        <el-form-item label="名称"><el-input v-model="form.themeName" maxlength="64" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" maxlength="500" type="textarea" /></el-form-item>
        <el-form-item label="背景色"><el-color-picker v-model="form.bgColor" /></el-form-item>
        <el-form-item label="背景图文件ID">
          <el-input-number v-model="form.bgImageFileId" :min="1" :value-on-clear="null" controls-position="right" />
        </el-form-item>
        <el-form-item label="手机背景文件ID">
          <el-input-number v-model="form.mobileBgImageFileId" :min="1" :value-on-clear="null" controls-position="right" />
        </el-form-item>
        <el-form-item label="展示方式">
          <el-select v-model="form.bgDisplay" style="width: 220px">
            <el-option label="顶部居中" value="TOP_CENTER" />
            <el-option label="随页面滚动" value="SCROLL" />
            <el-option label="固定" value="FIXED" />
          </el-select>
        </el-form-item>
        <el-form-item v-for="item in colorFields" :key="item.key" :label="item.label">
          <el-color-picker v-model="form.colors[item.key]" />
        </el-form-item>
        <el-form-item label="状态预览">
          <span class="preview" :style="previewStyle('hover')">悬停</span>
          <span class="preview" :style="previewStyle('active')">按下</span>
          <span class="preview" :style="previewStyle('focus')">焦点</span>
          <span class="preview" :style="previewStyle('disabled')">禁用</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorOpen = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="publishOpen" title="发布主题" width="520px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="发布到">
          <el-radio-group v-model="publishScope">
            <el-radio label="GLOBAL">全站</el-radio>
            <el-radio label="HOME">首页</el-radio>
            <el-radio label="BASE">基础主题</el-radio>
            <el-radio label="SEASON">某一届赛事</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="publishScope === 'SEASON'" label="选择赛季">
          <el-select
            v-model="publishSeasonId"
            filterable
            clearable
            placeholder="搜索赛季名称"
            style="width: 100%"
            :loading="seasonLoading"
          >
            <el-option
              v-for="item in seasonOptions"
              :key="item.id"
              :label="seasonOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
          <div class="field-tip">选中后，这一届赛事的前台页面使用该主题。</div>
        </el-form-item>
        <el-alert v-if="impactText" :title="impactText" type="warning" :closable="false" />
      </el-form>
      <template #footer>
        <el-button @click="loadImpact">查看影响范围</el-button>
        <el-button type="success" @click="handlePublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketSiteTheme">
import {
  copySiteTheme,
  deleteSiteTheme,
  getSiteTheme,
  impactSiteTheme,
  listSiteTheme,
  listSiteThemeSeasonOptions,
  offlineSiteTheme,
  publishSiteTheme,
  saveSiteTheme,
  unbindSiteTheme,
  type SiteThemeSeasonOption
} from '@/api/basket/siteTheme'
import type { SiteThemeBinding, SiteThemeResp, SiteThemeSaveReq, ThemeColorReq } from '@/types'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const themes = ref<SiteThemeResp[]>([])
const editorOpen = ref(false)
const publishOpen = ref(false)
const published = ref(false)
const publishScope = ref('HOME')
const publishSeasonId = ref<number>()
const seasonOptions = ref<SiteThemeSeasonOption[]>([])
const seasonLoading = ref(false)
const impactText = ref('')
const form = ref<SiteThemeSaveReq>(blank())

const colorFields: { key: keyof ThemeColorReq, label: string }[] = [
  { key: 'primary', label: '主色' },
  { key: 'secondary', label: '辅助色' },
  { key: 'accent', label: '强调色' },
  { key: 'content', label: '内容区' },
  { key: 'card', label: '卡片' },
  { key: 'tableHead', label: '表头' },
  { key: 'border', label: '边框' },
  { key: 'text', label: '主文字' },
  { key: 'textMuted', label: '辅助文字' },
  { key: 'onAccent', label: '强调面文字' }
]

function defaultColors(): ThemeColorReq {
  return {
    primary: '#E10613',
    secondary: '#080808',
    accent: '#FF3C23',
    content: '#E9E7E7',
    card: '#FFFFFF',
    tableHead: '#111111',
    border: '#D9D4D0',
    text: '#171717',
    textMuted: '#666666',
    onAccent: '#FFFFFF'
  }
}

function blank(): SiteThemeSaveReq {
  return {
    themeName: '',
    description: '',
    bgColor: '#050505',
    bgDisplay: 'TOP_CENTER',
    versionNo: 0,
    colors: defaultColors()
  }
}

function load() {
  loading.value = true
  listSiteTheme().then(res => {
    themes.value = res.data || []
  }).finally(() => {
    loading.value = false
  })
}

function handleAdd() {
  form.value = blank()
  published.value = false
  editorOpen.value = true
}

function handleEdit(row: SiteThemeResp) {
  getSiteTheme(row.id!).then(res => {
    const data = res.data || {}
    published.value = data.themeStatus === 1
    form.value = {
      id: data.id,
      versionNo: data.versionNo ?? 0,
      themeName: data.themeName || '',
      description: data.description || '',
      bgColor: data.bgColor || '#050505',
      bgImageFileId: data.bgImageFileId,
      mobileBgImageFileId: data.mobileBgImageFileId,
      bgDisplay: data.bgDisplay || 'TOP_CENTER',
      colors: { ...defaultColors(), ...(data.colors || {}) }
    }
    editorOpen.value = true
  })
}

function handleSave() {
  saveSiteTheme(form.value).then(res => {
    proxy.$modal.msgSuccess('草稿已保存，官网不变')
    editorOpen.value = false
    if (res.data?.id) handleEdit(res.data)
    load()
  })
}

function handleCopy(row: SiteThemeResp) {
  copySiteTheme(row.id!).then(() => {
    proxy.$modal.msgSuccess('已复制成新主题，原绑定不变')
    load()
  })
}

function openPublish(row: SiteThemeResp) {
  getSiteTheme(row.id!).then(res => {
    const data = res.data || {}
    form.value = {
      id: data.id,
      versionNo: data.versionNo ?? 0,
      themeName: data.themeName || '',
      description: data.description || '',
      bgColor: data.bgColor || '#050505',
      bgImageFileId: data.bgImageFileId,
      mobileBgImageFileId: data.mobileBgImageFileId,
      bgDisplay: data.bgDisplay || 'TOP_CENTER',
      colors: { ...defaultColors(), ...(data.colors || {}) }
    }
    publishScope.value = 'GLOBAL'
    publishSeasonId.value = undefined
    impactText.value = ''
    publishOpen.value = true
    loadSeasonOptions()
  })
}

function loadSeasonOptions() {
  seasonLoading.value = true
  listSiteThemeSeasonOptions().then(res => {
    seasonOptions.value = res.data || []
  }).finally(() => {
    seasonLoading.value = false
  })
}

function seasonOptionLabel(item: SiteThemeSeasonOption) {
  const title = (item.label || '').trim()
  const extra = (item.subLabel || '').trim()
  if (!title) return extra || String(item.id)
  if (!extra || title.includes(extra)) return title
  if (extra.includes(title)) return extra
  return `${title}（${extra}）`
}

function requireSeason() {
  if (publishScope.value === 'SEASON' && !publishSeasonId.value) {
    proxy.$modal.msgError('请选择赛季')
    return false
  }
  return true
}

function loadImpact() {
  if (!form.value.id) {
    proxy.$modal.msgError('请先保存主题')
    return
  }
  if (!requireSeason()) return
  impactSiteTheme(form.value.id, publishScope.value, publishScope.value === 'SEASON' ? publishSeasonId.value : undefined).then(res => {
    const affected = res.data?.affected?.join('、') || '当前位置'
    impactText.value = `发布后会影响：${affected}。${res.data?.replaceHint || ''}`
  })
}

function handlePublish() {
  if (!form.value.id) {
    proxy.$modal.msgError('请先保存主题')
    return
  }
  if (!requireSeason()) return
  publishSiteTheme({
    ...form.value,
    scopeType: publishScope.value,
    seasonId: publishScope.value === 'SEASON' ? publishSeasonId.value : undefined
  }).then((res: { msg?: string }) => {
    proxy.$modal.msgSuccess(res?.msg || '主题已发布')
    publishOpen.value = false
    editorOpen.value = false
    load()
  })
}

function handleOffline(row: SiteThemeResp) {
  proxy.$modal.confirm(`停用「${row.themeName}」后，正在使用它的页面回到基础主题。`).then(() =>
    offlineSiteTheme(row.id!, row.versionNo ?? 0)
  ).then((res: { msg?: string }) => {
    proxy.$modal.msgSuccess(res?.msg || '已停用')
    load()
  }).catch(() => {})
}

function handleDelete(row: SiteThemeResp) {
  proxy.$modal.confirm(`删除「${row.themeName}」并解除它的绑定？`).then(() =>
    deleteSiteTheme(row.id!)
  ).then(() => {
    proxy.$modal.msgSuccess('已删除')
    load()
  }).catch(() => {})
}

function handleUnbind(item: SiteThemeBinding) {
  proxy.$modal.confirm(`解除「${item.scopeLabel}」的绑定？主题不会删除。`).then(() =>
    unbindSiteTheme(item.scopeType || '', item.scopeType === 'SEASON' ? item.scopeId : undefined)
  ).then(() => {
    proxy.$modal.msgSuccess('已恢复默认')
    load()
  }).catch(() => {})
}

function previewStyle(kind: 'hover' | 'active' | 'focus' | 'disabled') {
  const primary = form.value.colors?.primary || '#E10613'
  const onAccent = form.value.colors?.onAccent || '#FFFFFF'
  const background = kind === 'hover'
    ? mix(primary, '#ffffff', 0.16)
    : kind === 'active'
      ? mix(primary, '#000000', 0.18)
      : kind === 'disabled'
        ? mix(primary, '#9b9b9b', 0.62)
        : primary
  return {
    background,
    color: onAccent,
    outline: kind === 'focus' ? `2px solid ${rgba(primary, 0.4)}` : 'none'
  }
}

function mix(source: string, target: string, amount: number) {
  const a = rgb(source)
  const b = rgb(target)
  if (!a || !b) return source
  const channel = (from: number, to: number) => Math.max(0, Math.min(255, Math.round(from + (to - from) * amount)))
  return `#${[channel(a.r, b.r), channel(a.g, b.g), channel(a.b, b.b)].map(item => item.toString(16).padStart(2, '0')).join('')}`
}

function rgba(source: string, alpha: number) {
  const color = rgb(source)
  return color ? `rgba(${color.r}, ${color.g}, ${color.b}, ${alpha})` : source
}

function rgb(value: string) {
  const matched = /^#([0-9a-fA-F]{6})$/.exec(value)
  if (!matched) return null
  const n = Number.parseInt(matched[1], 16)
  return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255 }
}

load()
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.bind-tag { margin: 0 6px 6px 0; }
.swatch { display: inline-block; width: 28px; height: 18px; border: 1px solid #ddd; }
.preview { display: inline-flex; align-items: center; min-width: 64px; height: 32px; margin-right: 8px; padding: 0 10px; }
.field-tip { color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.4; margin-top: 4px; }
</style>
