<template>
  <div class="standard-template-panel">
    <div class="std-context-bar mb8">
      <div class="std-context-series">当前赛事系列：{{ seriesLabel }}</div>
      <div class="std-context-hint">标准赛制模板：请选择本届赛事采用的标准赛制方案。选择模板不会修改赛事系列。</div>
    </div>
    <div class="scheme-card-grid mb8">
      <el-card
        v-for="item in orderedTemplates"
        :key="item.templateCode"
        shadow="hover"
        class="scheme-card std-template-card"
        :class="{ 'scheme-card--active': selectedCode === item.templateCode }"
        role="button"
        tabindex="0"
        @click="selectTemplate(item.templateCode)"
        @keydown.enter.prevent="selectTemplate(item.templateCode)"
      >
        <div v-if="selectedCode === item.templateCode" class="scheme-card-check">✓ 已选择</div>
        <div class="scheme-card-name">{{ cardMeta(item).title }}</div>
        <div class="scheme-card-summary">{{ cardMeta(item).flow }}</div>
        <div class="scheme-card-tags">
          <el-tag v-for="tag in cardMeta(item).tags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
        </div>
        <div class="scheme-card-bo">{{ boSummary(item) }}</div>
      </el-card>
    </div>

    <el-card v-if="selected" shadow="never" class="quick-params-card mb8">
      <template #header>
        <div class="params-header">
          <span>{{ cardMeta(selected).paramTitle }}</span>
          <span class="params-header-sub">待应用模板：{{ cardMeta(selected).title }}</span>
        </div>
      </template>
      <el-form label-width="120px" class="quick-params-form">
        <div class="std-param-grid mb8">
          <el-form-item v-if="gameMode" label="比赛模式">
            <span>{{ v4GameModeLabel(gameMode) }}</span>
          </el-form-item>
        </div>
        <div v-for="group in optionGroups" :key="group.title" class="param-group">
          <div class="param-group-title">{{ group.title }}</div>
          <div class="std-param-grid">
            <el-form-item v-for="opt in group.options" :key="opt.key" :label="v4StandardTemplateOptionLabel(opt.key, opt.label, selected.templateCode)">
              <el-select
                v-if="opt.inputType === 'SELECT'"
                :model-value="form[opt.key || '']"
                style="width: 100%"
                :disabled="!mutable"
                @change="(val) => setOption(opt.key, val)"
              >
                <el-option
                  v-for="val in opt.allowedValues || []"
                  :key="val"
                  :label="v4StandardTemplateOptionValueLabel(opt.key, val)"
                  :value="val"
                />
              </el-select>
              <el-select
                v-else-if="opt.inputType === 'NUMBER' && (opt.allowedValues || []).length"
                :model-value="String(form[opt.key || ''] ?? opt.defaultValue)"
                style="width: 100%"
                :disabled="!mutable"
                @change="(val) => setOption(opt.key, Number(val))"
              >
                <el-option
                  v-for="val in opt.allowedValues || []"
                  :key="val"
                  :label="v4StandardTemplateOptionValueLabel(opt.key, val)"
                  :value="val"
                />
              </el-select>
              <el-input-number
                v-else
                :model-value="Number(form[opt.key || ''] ?? opt.defaultValue)"
                :min="opt.minValue"
                :max="opt.maxValue"
                :disabled="!mutable"
                controls-position="right"
                @change="(val) => setOption(opt.key, val)"
              />
            </el-form-item>
          </div>
        </div>
        <el-alert
          v-if="selected.templateCode === 'LEAGUE_STANDARD_V1'"
          class="mb8"
          type="info"
          :closable="false"
          title="季后赛按积分排名取 Top N，淘汰赛采用高排名对低排名。"
        />
        <el-alert
          v-if="isPersonal"
          class="mb8"
          type="info"
          :closable="false"
          title="个人报名将在报名审核后进行 Formation 临时组队。Formation 不是正式比赛阶段。"
        />
        <el-form-item>
          <el-button type="primary" :loading="applying" :disabled="!mutable || applying" @click="apply">应用此标准模板</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="selected" shadow="never" class="preview-card mb8">
      <template #header>赛制预览</template>
      <div class="scheme-flow-mini mb8">
        <span v-for="(step, si) in previewSteps" :key="step + si" class="scheme-flow-step">
          {{ step }}<span v-if="si < previewSteps.length - 1" class="scheme-flow-arrow">→</span>
        </span>
      </div>
      <p class="el-form-item__tip">
        正式比赛阶段：
        <span v-for="(stage, si) in selected.stages || []" :key="stage.stageCode">
          {{ stage.stageName }}（{{ v4CompetitionFormatLabel(stage.competitionFormat) }}）<span v-if="si < (selected.stages || []).length - 1"> → </span>
        </span>
      </p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  applyStandardTemplate,
  listStandardTemplates,
  type StandardTemplateApplyReq,
  type StandardTemplateDefinition,
  type StandardTemplateOptionDefinition
} from '@/api/cnpa/event-v4/format-template'
import {
  V4_STANDARD_TEMPLATE_CODES,
  v4CompetitionFormatLabel,
  v4GameModeLabel,
  v4StandardTemplateBoSummary,
  v4StandardTemplateCardMeta,
  v4StandardTemplateEnsureOptions,
  v4StandardTemplateOptionGroups,
  v4StandardTemplateOptionLabel,
  v4StandardTemplateOptionValueLabel,
  v4StandardTemplatePreviewSteps,
  v4SeriesDisplayLabel
} from '@/utils/v4AdminDisplay'

const props = defineProps<{
  divisionId: number
  seriesId?: number
  seriesCode?: string
  seriesName?: string
  mutable?: boolean
  gameMode?: string
  registrationMode?: string
}>()

const emit = defineEmits<{
  applied: [payload: { templateCode: string }]
}>()

const applying = ref(false)
const templates = ref<StandardTemplateDefinition[]>([])
const selectedCode = ref('')
const defaultCode = ref('')
const catalogSeriesCode = ref('')
const form = reactive<Record<string, string | number | undefined>>({})

const seriesLabel = computed(() => v4SeriesDisplayLabel(props.seriesName, props.seriesCode || catalogSeriesCode.value) || '-')

const selected = computed(() => templates.value.find((item) => item.templateCode === selectedCode.value))

const orderedTemplates = computed(() => {
  const rank = (code?: string) => {
    const idx = (V4_STANDARD_TEMPLATE_CODES as readonly string[]).indexOf(code || '')
    return idx < 0 ? 99 : idx
  }
  return [...templates.value].sort((a, b) => rank(a.templateCode) - rank(b.templateCode))
})

const isPersonal = computed(() => String(form.registrationMode || selected.value?.registrationMode) === 'PERSONAL')

const displayOptions = computed(() => v4StandardTemplateEnsureOptions(selected.value) as StandardTemplateOptionDefinition[])

const optionGroups = computed(() => {
  const current = selected.value
  if (!current) return []
  const opts = displayOptions.value
  const groups = v4StandardTemplateOptionGroups(current.templateCode)
  const used = new Set<string>()
  const result: { title: string; options: StandardTemplateOptionDefinition[] }[] = []
  for (const group of groups) {
    const options = opts.filter((opt) => opt.key && group.keys.includes(opt.key))
    options.forEach((opt) => { if (opt.key) used.add(opt.key) })
    if (options.length) result.push({ title: group.title, options })
  }
  const leftover = opts.filter((opt) => opt.key && !used.has(opt.key))
  if (leftover.length) result.push({ title: '其他参数', options: leftover })
  return result
})

const previewSteps = computed(() => v4StandardTemplatePreviewSteps(selected.value?.templateCode, form))

function cardMeta(item?: StandardTemplateDefinition) {
  return v4StandardTemplateCardMeta(item?.templateCode)
}

function boSummary(item: StandardTemplateDefinition) {
  return v4StandardTemplateBoSummary(item.templateCode, item.options, selectedCode.value === item.templateCode ? form : undefined)
}

watch(() => props.seriesId, async (seriesId) => {
  if (!seriesId) {
    templates.value = []
    return
  }
  const res = await listStandardTemplates(seriesId)
  catalogSeriesCode.value = res.data?.seriesCode || ''
  templates.value = res.data?.templates || []
  defaultCode.value = res.data?.defaultTemplateCode || ''
  selectedCode.value = defaultCode.value || templates.value[0]?.templateCode || ''
  hydrateForm()
}, { immediate: true })

watch(selectedCode, () => hydrateForm())

function hydrateForm() {
  const current = selected.value
  Object.keys(form).forEach((key) => delete form[key])
  if (!current) return
  for (const opt of v4StandardTemplateEnsureOptions(current)) {
    if (!opt.key) continue
    form[opt.key] = coerce(opt.defaultValue, opt.inputType)
  }
  form.registrationMode = current.defaultRegistrationMode
    || current.options?.find((opt) => opt.key === 'registrationMode')?.defaultValue
    || current.registrationMode
    || form.registrationMode
}

function coerce(value: string | undefined, inputType?: string) {
  if (inputType === 'NUMBER' && value != null && value !== '') return Number(value)
  return value
}

function selectTemplate(code?: string) {
  if (code) selectedCode.value = code
}

function setOption(key: string | undefined, value: string | number) {
  if (!key) return
  form[key] = value
}

async function apply() {
  if (!props.divisionId || !selectedCode.value) return
  applying.value = true
  try {
    const payload: StandardTemplateApplyReq = {
      templateCode: selectedCode.value,
      registrationMode: String(form.registrationMode || ''),
      gameMode: props.gameMode,
      roundRobinCycles: num(form.roundRobinCycles),
      playoffSlots: num(form.playoffSlots),
      groupCount: num(form.groupCount),
      groupCycles: num(form.groupCycles),
      advancePerGroup: num(form.advancePerGroup),
      groupingMode: str(form.groupingMode),
      eliminationPlacement: str(form.eliminationPlacement),
      seedRule: str(form.seedRule),
      drawRule: str(form.drawRule),
      byeRule: str(form.byeRule),
      regularBestOf: num(form.regularBestOf),
      knockoutBestOf: num(form.knockoutBestOf),
      semifinalBestOf: num(form.semifinalBestOf),
      finalBestOf: num(form.finalBestOf)
    }
    await applyStandardTemplate(props.divisionId, payload)
    emit('applied', { templateCode: selectedCode.value })
  } finally {
    applying.value = false
  }
}

function num(value: string | number | undefined) {
  if (value === undefined || value === '') return undefined
  const n = Number(value)
  return Number.isNaN(n) ? undefined : n
}

function str(value: string | number | undefined) {
  if (value === undefined || value === '') return undefined
  return String(value)
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }

.std-context-bar {
  padding: 10px 12px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
}

.std-context-series {
  font-weight: 600;
  margin-bottom: 4px;
}

.std-context-hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.scheme-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.scheme-card {
  position: relative;
  cursor: pointer;
  border-radius: 8px;
  transition: border-color 0.2s, box-shadow 0.2s, background-color 0.2s;
}

.scheme-card:hover {
  border-color: var(--el-color-primary-light-5);
}

.scheme-card--active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-5);
}

.scheme-card :deep(.el-card__body) {
  padding: 14px 16px 12px;
  display: flex;
  flex-direction: column;
  min-height: 156px;
}

.std-template-card :deep(.el-card__body) {
  padding-right: 72px;
}

.scheme-card-check {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.scheme-card-name {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 6px;
}

.scheme-card-summary {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  margin-bottom: 8px;
}

.scheme-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.scheme-card-bo {
  margin-top: auto;
  font-size: 12px;
  color: var(--el-color-primary);
  line-height: 1.4;
}

.params-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.params-header-sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--el-text-color-secondary);
}

.param-group {
  margin-bottom: 8px;
}

.param-group-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin: 4px 0 8px;
}

.std-param-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 0 16px;
}

.quick-params-card :deep(.el-card__header),
.preview-card :deep(.el-card__header) {
  padding: 10px 16px;
  font-weight: 600;
}

.scheme-flow-mini {
  font-size: 13px;
  color: var(--el-color-primary);
}

.scheme-flow-step {
  white-space: nowrap;
}

.scheme-flow-arrow {
  margin: 0 4px;
  color: var(--el-text-color-placeholder);
}
</style>
