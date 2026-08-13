<template>
  <div class="standard-template-panel">
    <div class="scheme-card-grid mb8">
      <el-card
        v-for="item in templates"
        :key="item.templateCode"
        shadow="hover"
        class="scheme-card"
        :class="{ 'scheme-card--active': selectedCode === item.templateCode }"
        @click="selectTemplate(item.templateCode)"
      >
        <div class="scheme-card-name">{{ item.templateName }}</div>
        <div class="scheme-card-summary">{{ item.previewSummary }}</div>
        <div class="scheme-card-scene">{{ item.seriesCode }} / {{ item.defaultRegistrationMode }}</div>
        <div class="scheme-flow-mini">
          <span v-for="(stage, si) in item.stages || []" :key="stage.stageCode" class="scheme-flow-step">
            {{ stage.stageName }}<span v-if="si < (item.stages || []).length - 1" class="scheme-flow-arrow">-></span>
          </span>
        </div>
      </el-card>
    </div>

    <el-card v-if="selected" shadow="never" class="quick-params-card mb8">
      <template #header>STANDARD options</template>
      <el-form label-width="140px" class="quick-params-form">
        <el-form-item v-if="gameMode" label="GameMode">
          <span>{{ gameMode }}</span>
        </el-form-item>
        <el-form-item v-for="opt in selected.options || []" :key="opt.key" :label="opt.label">
          <el-select
            v-if="opt.inputType === 'SELECT'"
            :model-value="form[opt.key || '']"
            style="width: 100%"
            :disabled="!mutable"
            @change="(val) => setOption(opt.key, val)"
          >
            <el-option v-for="val in opt.allowedValues || []" :key="val" :label="val" :value="val" />
          </el-select>
          <el-select
            v-else-if="opt.inputType === 'NUMBER' && (opt.allowedValues || []).length"
            :model-value="String(form[opt.key || ''] ?? opt.defaultValue)"
            style="width: 100%"
            :disabled="!mutable"
            @change="(val) => setOption(opt.key, Number(val))"
          >
            <el-option v-for="val in opt.allowedValues || []" :key="val" :label="val" :value="val" />
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
        <el-form-item>
          <el-button type="primary" :loading="applying" :disabled="!mutable || applying" @click="apply">Apply STANDARD template</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="selected" shadow="never" class="preview-card mb8">
      <template #header>Stage Graph Preview</template>
      <div class="scheme-flow-mini mb8">
        <span v-for="(stage, si) in selected.stages || []" :key="stage.stageCode" class="scheme-flow-step">
          {{ stage.stageName }} ({{ stage.competitionFormat }})
          <span v-if="si < (selected.stages || []).length - 1" class="scheme-flow-arrow">-></span>
        </span>
      </div>
      <p v-if="formationNote" class="preview-nl">{{ formationNote }}</p>
      <p class="el-form-item__tip">Formation is roster preprocessing, not a competition stage.</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  applyStandardTemplate,
  listStandardTemplates,
  type StandardTemplateApplyReq,
  type StandardTemplateDefinition
} from '@/api/cnpa/event-v4/format-template'

const props = defineProps<{
  divisionId: number
  seriesId?: number
  mutable?: boolean
  gameMode?: string
  registrationMode?: string
}>()

const emit = defineEmits<{
  applied: []
}>()

const applying = ref(false)
const templates = ref<StandardTemplateDefinition[]>([])
const selectedCode = ref('')
const defaultCode = ref('')
const form = reactive<Record<string, string | number | undefined>>({})

const selected = computed(() => templates.value.find((item) => item.templateCode === selectedCode.value))

const formationNote = computed(() => {
  if (!selected.value) return ''
  if (String(form.registrationMode || selected.value.registrationMode) === 'PERSONAL') {
    return selected.value.formationNote || ''
  }
  return ''
})

watch(() => props.seriesId, async (seriesId) => {
  if (!seriesId) {
    templates.value = []
    return
  }
  const res = await listStandardTemplates(seriesId)
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
  for (const opt of current.options || []) {
    if (!opt.key) continue
    form[opt.key] = coerce(opt.defaultValue, opt.inputType)
  }
  if (props.registrationMode) {
    form.registrationMode = props.registrationMode
  }
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
    emit('applied')
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
