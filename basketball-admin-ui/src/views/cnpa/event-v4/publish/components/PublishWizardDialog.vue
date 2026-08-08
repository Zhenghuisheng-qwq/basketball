<template>
  <el-dialog
    v-model="visibleProxy"
    :title="dialogTitle"
    class="publish-wizard-dialog"
    append-to-body
    destroy-on-close
    align-center
    :close-on-click-modal="false"
    :before-close="handleBeforeClose"
  >
    <div class="wizard-shell">
      <div class="wizard-steps-wrap">
        <el-steps :active="currentStep" finish-status="success" align-center class="wizard-steps">
          <el-step
            v-for="(step, index) in WIZARD_STEPS"
            :key="step.key"
            :title="step.title"
            :status="stepStatus(index)"
            :class="{ 'step-clickable': isStepClickable(index) }"
            @click="handleStepClick(index)"
          />
        </el-steps>
      </div>

      <div v-loading="wizardLoading" class="dialog-content">
        <!-- Step 1: 赛季基础信息 -->
        <div v-show="currentStep === 0" class="form-main">
          <el-form ref="seasonFormRef" :model="seasonForm" :rules="seasonRules" label-width="120px" class="wizard-form">
          <el-form-item label="赛事系列" prop="seriesId">
            <el-select v-model="seasonForm.seriesId" style="width: 100%" :disabled="!!seasonForm.id" @change="() => applySeasonAutoDefaults()">
              <el-option v-for="item in seriesList" :key="item.id" :label="v4SeriesDisplayLabel(item.seriesName, item.seriesCode)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="赛季年份" prop="seasonYear">
            <el-input-number v-model="seasonForm.seasonYear" :min="2020" :max="2099" controls-position="right" style="width: 100%" @change="() => applySeasonAutoDefaults()" />
          </el-form-item>
          <el-form-item label="赛季名称" prop="seasonName">
            <el-input v-model="seasonForm.seasonName" placeholder="如 S1、春季赛" maxlength="128" show-word-limit @input="seasonNameManual = true" />
          </el-form-item>
          <el-form-item label="前台展示标题" prop="displayTitle">
            <el-input v-model="seasonForm.displayTitle" placeholder="如 CNPA League 2026 S1" maxlength="128" show-word-limit @input="displayTitleManual = true" />
          </el-form-item>
          <el-form-item label="赛季简介">
            <el-input v-model="seasonForm.description" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="可选，用于前台赛季介绍" />
          </el-form-item>
          <el-form-item label="赛季周期">
            <el-date-picker
              v-model="seasonPeriodRange"
              type="datetimerange"
              value-format="YYYY-MM-DD HH:mm:ss"
              range-separator="至"
              start-placeholder="赛季开始"
              end-placeholder="赛季结束"
              style="width: 100%"
            />
            <div class="el-form-item__tip">表示赛季展示周期，不代表各平台赛区的具体报名和比赛时间。</div>
          </el-form-item>
          <el-row :gutter="16" class="image-upload-row">
            <el-col :xs="24" :sm="12">
              <el-form-item label="封面图">
                <image-upload v-model="coverPreview" :limit="1" :file-size="5" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="Banner 图">
                <image-upload v-model="bannerPreview" :limit="1" :file-size="5" />
              </el-form-item>
            </el-col>
          </el-row>
          </el-form>
        </div>

      <!-- Step 2: 平台赛区 -->
      <div v-show="currentStep === 1" class="form-main form-main--wide">
        <div class="step-toolbar">
          <span class="step-desc">同一赛季可创建多个平台赛区，各赛区独立配置平台、模式与报名方式。</span>
          <el-button type="primary" plain icon="Plus" @click="addDivisionDraft">添加赛区</el-button>
        </div>
        <el-empty v-if="!divisionDrafts.length" description="请至少添加一个平台赛区" />
        <el-card v-for="(div, index) in divisionDrafts" :key="div.localKey" shadow="never" class="division-card">
          <template #header>
            <div class="division-card-header">
              <span>赛区 {{ index + 1 }}</span>
              <el-button link type="danger" @click="removeDivisionDraft(index)">删除</el-button>
            </div>
          </template>
          <el-form :ref="(el) => setDivisionFormRef(div.localKey, el)" :model="div" :rules="divisionBasicRules" label-width="120px">
            <el-form-item label="平台" prop="platformCode">
              <el-select v-model="div.platformCode" style="width: 100%" @change="() => handleDivisionFieldAutoUpdate(div)">
                <el-option label="PC" value="PC" />
                <el-option label="PS/Xbox" value="PS_XBOX" />
              </el-select>
            </el-form-item>
            <el-form-item label="比赛模式" prop="gameMode">
              <el-radio-group v-model="div.gameMode" @change="() => handleDivisionFieldAutoUpdate(div)">
                <el-radio label="FIVE_V_FIVE">5V5</el-radio>
                <el-radio label="THREE_V_THREE">3V3</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="赛区名称" prop="divisionName">
              <el-input v-model="div.divisionName" placeholder="如 CNPA League 2026 S1 - PS/Xbox 5V5" maxlength="128" show-word-limit @input="() => markDivisionNameManual(div)" />
            </el-form-item>
            <el-form-item label="报名方式" prop="registrationMode">
              <el-radio-group v-model="div.registrationMode" @change="() => handleDivisionFieldAutoUpdate(div)">
                <el-radio label="TEAM">球队报名</el-radio>
                <el-radio label="PERSONAL">个人报名</el-radio>
              </el-radio-group>
              <div v-if="div.registrationMode === 'PERSONAL'" class="el-form-item__tip">个人报名审核通过后，由运营线下抽签组队并录入结果。</div>
            </el-form-item>
            <el-form-item label="队伍上限" prop="maxTeamCount">
              <el-input-number v-model="div.maxTeamCount" :min="1" :max="512" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="阵容人数">
              <div class="readonly-info-line">
                <span class="readonly-info-value">阵容人数：{{ v4RosterRuleText(div.gameMode) }}</span>
                <span class="readonly-info-tip">人数范围由比赛模式自动确定，无需手动设置。</span>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- Step 3: Division 时间 -->
      <div v-show="currentStep === 2" class="form-main form-main--wide">
        <el-tabs v-model="timeTabKey" type="card">
          <el-tab-pane v-for="(div, index) in divisionDrafts" :key="div.localKey" :name="div.localKey">
            <template #label>
              <span class="tab-label-with-status">
                <el-icon v-if="getDivisionTimeStatus(div).status === 'valid'" class="tab-status-icon tab-status-icon--valid"><CircleCheck /></el-icon>
                <el-icon v-else-if="getDivisionTimeStatus(div).status === 'incomplete'" class="tab-status-icon tab-status-icon--warn"><Warning /></el-icon>
                <el-icon v-else class="tab-status-icon tab-status-icon--error"><CircleClose /></el-icon>
                <el-tooltip :content="formatV4DivisionTabLabel(div, index)" placement="top" :show-after="400">
                  <span class="tab-label-text">{{ formatV4DivisionTabLabel(div, index) }}</span>
                </el-tooltip>
              </span>
            </template>
            <el-form :ref="(el) => setTimeFormRef(div.localKey, el)" :model="div" :rules="divisionTimeRules" label-width="120px">
              <el-form-item label="报名时间" prop="registrationStartTime" :error="divisionTimeRangeError(div, 'registration')">
                <el-date-picker
                  :model-value="divisionRegistrationRange(div)"
                  type="datetimerange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  range-separator="至"
                  start-placeholder="报名开始"
                  end-placeholder="报名结束"
                  style="width: 100%"
                  @update:model-value="(val) => updateDivisionRegistrationRange(div, val)"
                />
              </el-form-item>
              <el-form-item label="比赛时间" prop="matchStartTime" :error="divisionTimeRangeError(div, 'match')">
                <el-date-picker
                  :model-value="divisionMatchRange(div)"
                  type="datetimerange"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  range-separator="至"
                  start-placeholder="比赛开始"
                  end-placeholder="比赛结束"
                  style="width: 100%"
                  @update:model-value="(val) => updateDivisionMatchRange(div, val)"
                />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- Step 4: 赛制规则 -->
      <div v-if="currentStep === 3" class="form-main form-main--wide">
        <el-tabs v-model="rulesTabKey" type="card" @tab-change="handleRulesTabChange">
          <el-tab-pane v-for="(div, index) in divisionDrafts" :key="div.localKey" :name="div.localKey">
            <template #label>
              <el-tooltip :content="formatV4DivisionTabLabel(div, index)" placement="top" :show-after="400">
                <span class="tab-label-text">{{ formatV4DivisionTabLabel(div, index) }}</span>
              </el-tooltip>
            </template>

            <el-empty v-if="!div.id" description="请先完成前两步并保存赛区" />

            <template v-else>
              <DivisionFormatRulesPanel
                :key="div.localKey"
                :context-key="div.localKey"
                :division-id="div.id!"
                :max-team-count="div.maxTeamCount"
                :format-template-id="div.formatTemplateId"
                :division-meta="div"
                :series-id="seasonForm.seriesId"
                @update:format-template-id="(id) => { div.formatTemplateId = id; dirty = true }"
                @snapshot-change="(snap) => onDivisionSnapshotChange(div.id!, snap)"
                @change="dirty = true"
              />
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- Step 5: 确认发布 -->
      <div v-show="currentStep === 4" class="form-main form-main--wide">
        <el-descriptions title="赛季信息" :column="2" border class="mb8">
          <el-descriptions-item label="赛事系列">{{ currentSeriesLabel }}</el-descriptions-item>
          <el-descriptions-item label="年份">{{ seasonForm.seasonYear }}</el-descriptions-item>
          <el-descriptions-item label="赛季名称">{{ seasonForm.seasonName }}</el-descriptions-item>
          <el-descriptions-item label="前台标题">{{ seasonForm.displayTitle }}</el-descriptions-item>
          <el-descriptions-item label="赛季周期" :span="2">{{ formatV4CompactRange(seasonForm.seasonStartTime, seasonForm.seasonEndTime) }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ seasonForm.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="confirm-section-title">发布前确认（{{ divisionDrafts.length }} 个赛区）</div>
        <el-table :data="divisionDrafts" border size="small" class="mb8">
          <el-table-column label="开放报名" width="88" align="center" fixed="left">
            <template #default="scope">
              <el-tooltip :disabled="canSelectDivisionForOpen(scope.row)" :content="getDivisionReadiness(scope.row).reasons.join('；')" placement="top">
                <el-checkbox
                  v-model="openRegistrationSelection[scope.row.localKey]"
                  :disabled="!canSelectDivisionForOpen(scope.row)"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="赛区" min-width="160">
            <template #default="scope">
              <div>{{ scope.row.divisionName || '未命名' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="平台与模式" min-width="140">
            <template #default="scope">
              <div>{{ v4PlatformLabel(scope.row.platformCode) }} · {{ v4GameModeLabel(scope.row.gameMode) }}</div>
              <div class="cell-sub">{{ v4RegistrationModeLabel(scope.row.registrationMode) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="报名时间" min-width="168">
            <template #default="scope">
              <div class="cell-sub">{{ formatV4CompactRange(scope.row.registrationStartTime, scope.row.registrationEndTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="比赛时间" min-width="168">
            <template #default="scope">
              <div class="cell-sub">{{ formatV4CompactRange(scope.row.matchStartTime, scope.row.matchEndTime) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="队伍上限" width="88" align="center">
            <template #default="scope">
              {{ scope.row.maxTeamCount }}
            </template>
          </el-table-column>
          <el-table-column label="赛制摘要" min-width="240">
            <template #default="scope">
              <div v-if="!sortedStages(scope.row).length" class="cell-sub">尚未配置</div>
              <div v-else class="confirm-format-chain">
                <template v-for="(item, fi) in formatV4CustomFormatPreview(sortedStages(scope.row), scope.row.maxTeamCount)" :key="fi">
                  <div class="confirm-format-step">
                    <div class="confirm-format-title">{{ item.title }}</div>
                    <div class="cell-sub">{{ item.summary }}</div>
                  </div>
                  <div v-if="fi < sortedStages(scope.row).length - 1" class="confirm-format-arrow">↓</div>
                </template>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="配置状态" width="96" align="center">
            <template #default="scope">
              <el-tag :type="getFormatDisplayStatus(scope.row).tagType" size="small">
                {{ getFormatDisplayStatus(scope.row).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="可开放报名" width="96" align="center">
            <template #default="scope">
              <span v-if="canSelectDivisionForOpen(scope.row)" class="text-success">是</span>
              <span v-else class="text-muted">否</span>
            </template>
          </el-table-column>
          <el-table-column label="当前问题" min-width="180">
            <template #default="scope">
              <span v-if="canSelectDivisionForOpen(scope.row)" class="text-muted">-</span>
              <span v-else class="reason-text">{{ formatV4WizardPrimaryIssue(getDivisionReadiness(scope.row)) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="96" align="center" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="jumpToRulesStep(scope.row)">返回配置</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-alert v-if="publishAndOpenDisabledReason" type="warning" :closable="false" show-icon class="mb8" :title="publishAndOpenDisabledReason" />

        <el-alert type="info" :closable="false" show-icon>
          <template #title>发布说明</template>
          <ul class="confirm-tip-list">
            <li>保存草稿：保留当前赛季与平台赛区配置，不发布前台。</li>
            <li>发布赛季（暂不开放报名）：仅发布赛季前台展示，不会自动开放任何平台赛区报名。</li>
            <li>发布并开放报名：发布赛季后，将自动确认赛制，并对勾选且配置完整的赛区开放报名。</li>
          </ul>
        </el-alert>
      </div>
      </div>
    </div>

    <template #footer>
      <div class="wizard-footer">
        <div class="wizard-footer-left">
          <el-button @click="handleSaveDraft" :loading="saving">保存草稿</el-button>
        </div>
        <div class="wizard-footer-right">
          <el-button v-if="currentStep === 0" @click="handleCancel">取消</el-button>
          <el-button v-if="currentStep > 0" @click="goPrev" :disabled="saving">上一步</el-button>
          <el-button v-if="currentStep < 4" type="primary" @click="goNext" :loading="saving">下一步</el-button>
          <template v-if="currentStep === 4">
            <el-button type="primary" @click="handlePublishSeason" :loading="saving" v-hasPermi="['basket:v4Season:publish']">发布赛季（暂不开放报名）</el-button>
            <el-button
              type="success"
              @click="handlePublishAndOpen"
              :loading="saving"
              :disabled="!canPublishAndOpen"
              v-hasPermi="['basket:v4Division:openRegistration']"
            >发布并开放报名</el-button>
          </template>
        </div>
      </div>
    </template>
  </el-dialog>

</template>

<script setup lang="ts">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import DivisionFormatRulesPanel from './DivisionFormatRulesPanel.vue'
import type { FormInstance } from 'element-plus'
import type { BasketSeriesVo } from '@/api/cnpa/event-v4/series'
import {
  addV4Season,
  getV4Season,
  publishV4Season,
  updateV4Season,
  type BasketSeasonCreateReq,
  type BasketSeasonDetailVo
} from '@/api/cnpa/event-v4/season'
import {
  addV4Division,
  openV4DivisionRegistration,
  updateV4Division,
  type BasketDivisionCreateReq
} from '@/api/cnpa/event-v4/division'
import {
  getFormatSnapshot,
  listV4FormatTemplates,
  lockFormatSnapshot,
  type BasketDivisionFormatSnapshotVo,
  type BasketFormatTemplateListVo
} from '@/api/cnpa/event-v4/format-template'
import {
  assessV4DivisionReadiness,
  buildV4DivisionDefaultName,
  buildV4SeasonDefaultDisplayTitle,
  buildV4SeasonDefaultSeasonName,
  formatV4CompactRange,
  formatV4CustomFormatPreview,
  formatV4DivisionComboLabel,
  formatV4DivisionTabLabel,
  formatV4WizardPrimaryIssue,
  isV4WizardTemplateAllowed,
  resolveV4WizardFormatDisplayStatus,
  validateV4DivisionTimeRelations,
  validateV4StageCombinationDetailed,
  v4GameModeLabel,
  v4PlatformLabel,
  v4RegistrationModeLabel,
  v4RosterRuleText,
  v4SeriesDisplayLabel,
  type V4DivisionTimeValidation
} from '@/utils/v4AdminDisplay'

export interface WizardDivisionDraft {
  localKey: string
  id?: number
  platformCode: string
  gameMode: string
  divisionName: string
  registrationMode: string
  maxTeamCount: number
  formatTemplateId?: number
  formatType: string
  registrationStartTime: string
  registrationEndTime: string
  matchStartTime: string
  matchEndTime: string
  sortOrder: number
  divisionNameManual?: boolean
}

const props = defineProps<{
  modelValue: boolean
  seriesList: BasketSeriesVo[]
  seriesId?: number
  seasonId?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const { proxy } = getCurrentInstance()!

const WIZARD_STEPS = [
  { key: 'season', title: '赛季基础' },
  { key: 'divisions', title: '平台赛区' },
  { key: 'times', title: '赛区时间' },
  { key: 'rules', title: '赛制规则' },
  { key: 'confirm', title: '确认发布' }
]

let localKeySeed = 0
function nextLocalKey() {
  localKeySeed += 1
  return `div-${localKeySeed}`
}

const visibleProxy = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const dialogTitle = computed(() => (props.seasonId ? '继续发布赛事' : '新建赛事'))
const currentSeriesLabel = computed(() => {
  const series = props.seriesList.find((s) => s.id === seasonForm.seriesId)
  return series ? v4SeriesDisplayLabel(series.seriesName, series.seriesCode) : '-'
})

const wizardLoading = ref(false)
const saving = ref(false)
const currentStep = ref(0)
const completedSteps = ref<boolean[]>([false, false, false, false, false])
const dirty = ref(false)

const seasonFormRef = ref<FormInstance>()
const seasonPeriodRange = ref<[string, string] | null>(null)
const coverPreview = ref('')
const bannerPreview = ref('')
const seasonForm = reactive<BasketSeasonCreateReq & { id?: number }>({
  seriesId: undefined,
  seasonYear: new Date().getFullYear(),
  seasonName: '',
  displayTitle: '',
  description: '',
  coverFileId: undefined,
  bannerFileId: undefined,
  seasonStartTime: '',
  seasonEndTime: ''
})

const seasonNameManual = ref(false)
const displayTitleManual = ref(false)
const autoSeasonName = ref('')
const autoDisplayTitle = ref('')

function trimRequired(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (!value || !String(value).trim()) {
    callback(new Error('不能为空或仅包含空格'))
    return
  }
  callback()
}

function validateSeasonNameField(_rule: unknown, value: string, callback: (err?: Error) => void) {
  if (!value || !String(value).trim()) {
    callback(new Error('请输入赛季名称'))
    return
  }
  if (String(value).trim().length > 128) {
    callback(new Error('赛季名称不能超过 128 个字符'))
    return
  }
  callback()
}

const seasonRules = {
  seriesId: [{ required: true, message: '请选择赛事系列', trigger: 'change' }],
  seasonYear: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  seasonName: [{ validator: validateSeasonNameField, trigger: 'blur' }],
  displayTitle: [{ validator: trimRequired, trigger: 'blur' }]
}

const divisionDrafts = ref<WizardDivisionDraft[]>([])
const divisionFormRefs = new Map<string, FormInstance>()
const timeFormRefs = new Map<string, FormInstance>()
const timeTabKey = ref('')
const rulesTabKey = ref('')
const formatTemplateList = ref<BasketFormatTemplateListVo[]>([])
const snapshotMap = reactive<Record<number, BasketDivisionFormatSnapshotVo>>({})
const openRegistrationSelection = reactive<Record<string, boolean>>({})

const divisionTimeStatusMap = computed(() => {
  const map = new Map<string, V4DivisionTimeValidation>()
  for (const div of divisionDrafts.value) {
    map.set(div.localKey, validateV4DivisionTimeRelations(div, seasonForm.seasonStartTime, seasonForm.seasonEndTime))
  }
  return map
})

function getDivisionTimeStatus(div: WizardDivisionDraft) {
  return divisionTimeStatusMap.value.get(div.localKey)
    || validateV4DivisionTimeRelations(div, seasonForm.seasonStartTime, seasonForm.seasonEndTime)
}

const enabledFormatTemplates = computed(() =>
  formatTemplateList.value.filter((item) => isV4WizardTemplateAllowed(item.templateCode))
)

function getCurrentSeriesCode() {
  return props.seriesList.find((s) => s.id === seasonForm.seriesId)?.seriesCode
}

function applySeasonAutoDefaults(force = false) {
  const seriesCode = getCurrentSeriesCode()
  const nextSeasonName = buildV4SeasonDefaultSeasonName(seriesCode)
  const nextDisplayTitle = buildV4SeasonDefaultDisplayTitle(seriesCode, seasonForm.seasonYear, seasonForm.seasonName || nextSeasonName)
  autoSeasonName.value = nextSeasonName
  autoDisplayTitle.value = nextDisplayTitle
  if (force || (!seasonNameManual.value && !seasonForm.seasonName?.trim())) {
    seasonForm.seasonName = nextSeasonName
    seasonNameManual.value = false
  } else if (!seasonNameManual.value && seasonForm.seasonName === autoSeasonName.value) {
    seasonForm.seasonName = nextSeasonName
  }
  if (force || (!displayTitleManual.value && !seasonForm.displayTitle?.trim())) {
    seasonForm.displayTitle = buildV4SeasonDefaultDisplayTitle(seriesCode, seasonForm.seasonYear, seasonForm.seasonName)
    displayTitleManual.value = false
  } else if (!displayTitleManual.value) {
    seasonForm.displayTitle = buildV4SeasonDefaultDisplayTitle(seriesCode, seasonForm.seasonYear, seasonForm.seasonName)
  }
  autoDisplayTitle.value = seasonForm.displayTitle
}

function applyDivisionAutoName(div: WizardDivisionDraft, force = false) {
  const autoName = buildV4DivisionDefaultName(seasonForm.displayTitle, div.platformCode, div.gameMode)
  if (force || !div.divisionNameManual || !div.divisionName?.trim() || div.divisionName === autoName) {
    div.divisionName = autoName
    div.divisionNameManual = false
  }
}

function markDivisionNameManual(div: WizardDivisionDraft) {
  div.divisionNameManual = true
}

function handleDivisionFieldAutoUpdate(div: WizardDivisionDraft) {
  applyDivisionAutoName(div)
  dirty.value = true
}

const selectedOpenDivisions = computed(() =>
  divisionDrafts.value.filter((div) => openRegistrationSelection[div.localKey])
)

const canPublishAndOpen = computed(() =>
  selectedOpenDivisions.value.length > 0
  && selectedOpenDivisions.value.every((div) => canSelectDivisionForOpen(div))
)

const publishAndOpenDisabledReason = computed(() => {
  if (canPublishAndOpen.value) return ''
  if (!seasonForm.seasonName?.trim() || !seasonForm.displayTitle?.trim()) {
    return '请先完善赛季基础信息'
  }
  if (!divisionDrafts.value.length) return '请至少添加一个平台赛区'
  const hasReady = divisionDrafts.value.some((div) => canSelectDivisionForOpen(div))
  if (!hasReady) return '请选择至少一个配置完整的赛区'
  if (!selectedOpenDivisions.value.length) return '请勾选至少一个满足开放报名条件的赛区'
  return '已勾选赛区配置不完整，请查看未就绪原因'
})

function validateDivisionNameField(_rule: unknown, value: string, callback: (err?: Error) => void) {
  const trimmed = (value || '').trim()
  if (!trimmed) {
    callback(new Error('请输入赛区名称'))
    return
  }
  if (/^\d+$/.test(trimmed)) {
    callback(new Error('赛区名称不能为纯数字'))
    return
  }
  callback()
}

function validateMaxTeamCountField(_rule: unknown, value: number, callback: (err?: Error) => void) {
  if (value == null || !Number.isInteger(value) || value <= 0) {
    callback(new Error('队伍上限必须为正整数'))
    return
  }
  if (value > 512) {
    callback(new Error('队伍上限不能超过 512'))
    return
  }
  callback()
}

const divisionBasicRules = {
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  gameMode: [{ required: true, message: '请选择比赛模式', trigger: 'change' }],
  divisionName: [{ validator: validateDivisionNameField, trigger: 'blur' }],
  registrationMode: [{ required: true, message: '请选择报名方式', trigger: 'change' }],
  maxTeamCount: [{ validator: validateMaxTeamCountField, trigger: 'blur' }]
}

const divisionTimeRules = {
  registrationStartTime: [{ required: true, message: '请选择报名时间', trigger: 'change' }],
  registrationEndTime: [{ required: true, message: '请选择报名时间', trigger: 'change' }],
  matchStartTime: [{ required: true, message: '请选择比赛时间', trigger: 'change' }],
  matchEndTime: [{ required: true, message: '请选择比赛时间', trigger: 'change' }]
}

function divisionRegistrationRange(div: WizardDivisionDraft): [string, string] | null {
  if (div.registrationStartTime && div.registrationEndTime) {
    return [div.registrationStartTime, div.registrationEndTime]
  }
  return null
}

function divisionMatchRange(div: WizardDivisionDraft): [string, string] | null {
  if (div.matchStartTime && div.matchEndTime) {
    return [div.matchStartTime, div.matchEndTime]
  }
  return null
}

function updateDivisionRegistrationRange(div: WizardDivisionDraft, range: [string, string] | null) {
  if (range && range.length === 2) {
    div.registrationStartTime = range[0]
    div.registrationEndTime = range[1]
  } else {
    div.registrationStartTime = ''
    div.registrationEndTime = ''
  }
  dirty.value = true
}

function updateDivisionMatchRange(div: WizardDivisionDraft, range: [string, string] | null) {
  if (range && range.length === 2) {
    div.matchStartTime = range[0]
    div.matchEndTime = range[1]
  } else {
    div.matchStartTime = ''
    div.matchEndTime = ''
  }
  dirty.value = true
}

function divisionTimeRangeError(div: WizardDivisionDraft, kind: 'registration' | 'match'): string {
  const status = getDivisionTimeStatus(div)
  const { fieldErrors } = status
  if (kind === 'registration') {
    return fieldErrors.registrationEndTime
      || fieldErrors.registrationStartTime
      || status.messages.find((msg) => msg.includes('报名'))
      || ''
  }
  return fieldErrors.matchEndTime
    || fieldErrors.matchStartTime
    || status.messages.find((msg) => msg.includes('比赛'))
    || ''
}

function jumpToRulesStep(div: WizardDivisionDraft) {
  currentStep.value = 3
  rulesTabKey.value = div.localKey
}

function onDivisionSnapshotChange(divisionId: number, snap: BasketDivisionFormatSnapshotVo) {
  snapshotMap[divisionId] = snap
}

watch(
  [seasonForm, divisionDrafts],
  () => { dirty.value = true },
  { deep: true }
)

watch(seasonPeriodRange, (range) => {
  if (range && range.length === 2) {
    seasonForm.seasonStartTime = range[0]
    seasonForm.seasonEndTime = range[1]
  } else {
    seasonForm.seasonStartTime = ''
    seasonForm.seasonEndTime = ''
  }
  dirty.value = true
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      initWizard()
    }
  }
)

function stepStatus(index: number): '' | 'wait' | 'process' | 'finish' | 'error' | 'success' {
  if (index === currentStep.value) return 'process'
  if (completedSteps.value[index]) return 'success'
  if (index < currentStep.value) return 'finish'
  return 'wait'
}

function isStepClickable(index: number) {
  return index !== currentStep.value && (index < currentStep.value || completedSteps.value[index])
}

function handleStepClick(index: number) {
  if (!isStepClickable(index)) return
  currentStep.value = index
}

function setDivisionFormRef(key: string, el: unknown) {
  if (el) divisionFormRefs.set(key, el as FormInstance)
  else divisionFormRefs.delete(key)
}

function setTimeFormRef(key: string, el: unknown) {
  if (el) timeFormRefs.set(key, el as FormInstance)
  else timeFormRefs.delete(key)
}

function createEmptyDivision(): WizardDivisionDraft {
  const defaultTemplate = enabledFormatTemplates.value.find((t) => t.isDefault === 1) || enabledFormatTemplates.value[0]
  return {
    localKey: nextLocalKey(),
    platformCode: 'PS_XBOX',
    gameMode: 'FIVE_V_FIVE',
    divisionName: '',
    registrationMode: 'TEAM',
    maxTeamCount: 16,
    formatTemplateId: defaultTemplate?.id,
    formatType: 'REGULAR_ROUND_ROBIN',
    registrationStartTime: '',
    registrationEndTime: '',
    matchStartTime: '',
    matchEndTime: '',
    sortOrder: 0
  }
}

watch(
  () => seasonForm.seasonName,
  () => {
    if (!displayTitleManual.value) {
      applySeasonAutoDefaults()
    }
    divisionDrafts.value.forEach((div) => {
      if (!div.divisionNameManual) applyDivisionAutoName(div)
    })
  }
)

function addDivisionDraft() {
  const div = createEmptyDivision()
  applyDivisionAutoName(div, true)
  divisionDrafts.value.push(div)
  dirty.value = true
}

function removeDivisionDraft(index: number) {
  if (divisionDrafts.value.length <= 1) {
    proxy!.$modal.msgWarning('至少需要保留一个平台赛区。')
    return
  }
  divisionDrafts.value.splice(index, 1)
  dirty.value = true
}

function validateUniqueDivisionCombos(): boolean {
  const seen = new Map<string, WizardDivisionDraft>()
  for (const div of divisionDrafts.value) {
    const key = `${div.platformCode}|${div.gameMode}|${div.registrationMode}`
    const existing = seen.get(key)
    if (existing) {
      proxy!.$modal.msgError(`当前赛季已存在 ${formatV4DivisionComboLabel(div.platformCode, div.gameMode, div.registrationMode)} 赛区。`)
      return false
    }
    seen.set(key, div)
  }
  return true
}

function validateAllDivisionTimes(): boolean {
  let firstInvalidKey = ''
  let firstInvalidMessages: string[] = []
  for (const div of divisionDrafts.value) {
    const status = getDivisionTimeStatus(div)
    if (status.status !== 'valid') {
      if (!firstInvalidKey) {
        firstInvalidKey = div.localKey
        firstInvalidMessages = status.messages
      }
    }
  }
  if (firstInvalidKey) {
    timeTabKey.value = firstInvalidKey
    proxy!.$modal.msgError(firstInvalidMessages[0] || '赛区时间配置无效，请检查各时间字段')
    return false
  }
  return true
}

function currentSnapshot(div: WizardDivisionDraft) {
  return div.id ? snapshotMap[div.id] : undefined
}

function sortedStages(div: WizardDivisionDraft) {
  const snap = currentSnapshot(div)
  return [...(snap?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
}

function getDivisionReadiness(div: WizardDivisionDraft) {
  const snap = div.id ? snapshotMap[div.id] : undefined
  return assessV4DivisionReadiness(div, snap)
}

function canSelectDivisionForOpen(div: WizardDivisionDraft) {
  const readiness = getDivisionReadiness(div)
  return readiness.basicComplete
    && readiness.timeValid
    && readiness.teamLimitValid
    && readiness.rosterValid
    && readiness.hasValidStage
}

function getFormatDisplayStatus(div: WizardDivisionDraft) {
  const snap = div.id ? snapshotMap[div.id] : undefined
  const readiness = getDivisionReadiness(div)
  return resolveV4WizardFormatDisplayStatus(snap, readiness, sortedStages(div).length)
}

function syncOpenRegistrationSelection() {
  for (const div of divisionDrafts.value) {
    if (!(div.localKey in openRegistrationSelection)) {
      openRegistrationSelection[div.localKey] = canSelectDivisionForOpen(div)
    } else if (!canSelectDivisionForOpen(div)) {
      openRegistrationSelection[div.localKey] = false
    }
  }
}

function isDivisionReady(div: WizardDivisionDraft) {
  return canSelectDivisionForOpen(div)
}

async function loadFormatTemplates(seriesId?: number) {
  if (!seriesId) {
    formatTemplateList.value = []
    return
  }
  const res = await listV4FormatTemplates(seriesId)
  formatTemplateList.value = res.data || []
}

async function loadSnapshot(div: WizardDivisionDraft) {
  if (!div.id) return
  const res = await getFormatSnapshot(div.id)
  snapshotMap[div.id] = res.data || {}
}

async function loadAllSnapshots() {
  await Promise.all(divisionDrafts.value.filter((d) => d.id).map((d) => loadSnapshot(d)))
}

function resetWizardState() {
  currentStep.value = 0
  completedSteps.value = [false, false, false, false, false]
  dirty.value = false
  Object.keys(snapshotMap).forEach((k) => delete snapshotMap[Number(k)])
  Object.keys(openRegistrationSelection).forEach((k) => delete openRegistrationSelection[k])
  divisionFormRefs.clear()
  timeFormRefs.clear()
  Object.assign(seasonForm, {
    id: undefined,
    seriesId: props.seriesId,
    seasonYear: new Date().getFullYear(),
    seasonName: '',
    displayTitle: '',
    description: '',
    coverFileId: undefined,
    bannerFileId: undefined,
    seasonStartTime: '',
    seasonEndTime: ''
  })
  coverPreview.value = ''
  bannerPreview.value = ''
  seasonPeriodRange.value = null
  divisionDrafts.value = [createEmptyDivision()]
  applyDivisionAutoName(divisionDrafts.value[0], true)
  seasonNameManual.value = false
  displayTitleManual.value = false
  applySeasonAutoDefaults(true)
  timeTabKey.value = divisionDrafts.value[0]?.localKey || ''
  rulesTabKey.value = divisionDrafts.value[0]?.localKey || ''
}

function inferResumeStep(divisions: WizardDivisionDraft[]): number {
  if (!divisions.length || divisions.every((d) => !d.id)) return 1
  const missingTimes = divisions.some((d) => getDivisionTimeStatus(d).status !== 'valid')
  if (missingTimes) return 2
  const missingSnapshot = divisions.some((d) => !d.id || !snapshotMap[d.id!]?.snapshotStatus)
  const missingStage = divisions.some((d) => !d.id || !sortedStages(d).length)
  if (missingSnapshot || missingStage) return 3
  return 4
}

async function initWizard() {
  wizardLoading.value = true
  try {
    await loadFormatTemplates(props.seriesId)
    resetWizardState()
    if (props.seasonId) {
      const res = await getV4Season(props.seasonId)
      const detail = res.data!
      Object.assign(seasonForm, {
        id: detail.id,
        seriesId: detail.seriesId,
        seasonYear: detail.seasonYear,
        seasonName: detail.seasonName,
        displayTitle: detail.displayTitle,
        description: detail.description,
        coverFileId: detail.coverFileId,
        bannerFileId: detail.bannerFileId,
        seasonStartTime: detail.seasonStartTime || '',
        seasonEndTime: detail.seasonEndTime || ''
      })
      coverPreview.value = detail.coverUrl || ''
      bannerPreview.value = detail.bannerUrl || ''
      seasonPeriodRange.value = detail.seasonStartTime && detail.seasonEndTime
        ? [detail.seasonStartTime, detail.seasonEndTime]
        : null
      await loadFormatTemplates(detail.seriesId)
      if (detail.divisions?.length) {
        divisionDrafts.value = detail.divisions.map((d) => ({
          localKey: nextLocalKey(),
          id: d.id,
          platformCode: d.platformCode || 'PS_XBOX',
          gameMode: d.gameMode || 'FIVE_V_FIVE',
          divisionName: d.divisionName || '',
          registrationMode: d.registrationMode || 'TEAM',
          maxTeamCount: d.maxTeamCount || 16,
          formatTemplateId: d.formatTemplateId,
          formatType: d.formatType || 'REGULAR_ROUND_ROBIN',
          registrationStartTime: d.registrationStartTime || '',
          registrationEndTime: d.registrationEndTime || '',
          matchStartTime: d.matchStartTime || '',
          matchEndTime: d.matchEndTime || '',
          sortOrder: d.sortOrder || 0,
          divisionNameManual: !!d.divisionName
        }))
      }
      seasonNameManual.value = true
      displayTitleManual.value = true
      timeTabKey.value = divisionDrafts.value[0]?.localKey || ''
      rulesTabKey.value = divisionDrafts.value[0]?.localKey || ''
      await loadAllSnapshots()
      completedSteps.value[0] = true
      currentStep.value = inferResumeStep(divisionDrafts.value)
      if (currentStep.value >= 2) completedSteps.value[1] = true
      if (currentStep.value >= 3) completedSteps.value[2] = true
      if (currentStep.value >= 4) completedSteps.value[3] = true
    } else {
      seasonForm.seriesId = props.seriesId
      applySeasonAutoDefaults(true)
    }
  } finally {
    wizardLoading.value = false
  }
}

function buildSeasonPayload(): BasketSeasonCreateReq {
  return {
    seriesId: seasonForm.seriesId,
    seasonYear: seasonForm.seasonYear,
    seasonName: seasonForm.seasonName.trim(),
    displayTitle: seasonForm.displayTitle.trim(),
    description: seasonForm.description?.trim() || undefined,
    coverFileId: seasonForm.coverFileId,
    bannerFileId: seasonForm.bannerFileId,
    seasonStartTime: seasonForm.seasonStartTime || undefined,
    seasonEndTime: seasonForm.seasonEndTime || undefined
  }
}

function buildDivisionPayload(div: WizardDivisionDraft, includeTimes: boolean): BasketDivisionCreateReq {
  const payload: BasketDivisionCreateReq = {
    seasonId: seasonForm.id,
    platformCode: div.platformCode,
    gameMode: div.gameMode,
    divisionName: div.divisionName.trim(),
    registrationMode: div.registrationMode,
    formatType: div.formatType,
    formatTemplateId: div.formatTemplateId,
    maxTeamCount: div.maxTeamCount,
    sortOrder: div.sortOrder
  }
  if (includeTimes) {
    payload.registrationStartTime = div.registrationStartTime
    payload.registrationEndTime = div.registrationEndTime
    payload.matchStartTime = div.matchStartTime
    payload.matchEndTime = div.matchEndTime
  }
  return payload
}

async function persistSeason(): Promise<boolean> {
  if (seasonForm.seasonStartTime && seasonForm.seasonEndTime
    && seasonForm.seasonStartTime >= seasonForm.seasonEndTime) {
    proxy!.$modal.msgError('赛季结束时间必须晚于赛季开始时间')
    return false
  }
  const payload = buildSeasonPayload()
  if (seasonForm.id) {
    await updateV4Season(seasonForm.id, payload)
  } else {
    const res = await addV4Season(payload)
    seasonForm.id = res.data
  }
  dirty.value = false
  return true
}

async function persistDivisionsBasic(): Promise<boolean> {
  if (!seasonForm.id) {
    proxy!.$modal.msgError('请先保存赛季基础信息')
    return false
  }
  if (!divisionDrafts.value.length) {
    proxy!.$modal.msgError('请至少添加一个平台赛区')
    return false
  }
  if (!validateUniqueDivisionCombos()) return false

  for (const div of divisionDrafts.value) {
    const formRef = divisionFormRefs.get(div.localKey)
    if (formRef) {
      try {
        await formRef.validate()
      } catch {
        return false
      }
    }
  }

  for (const div of divisionDrafts.value) {
    const payload = buildDivisionPayload(div, false)
    if (div.id) {
      await updateV4Division(div.id, payload)
    } else {
      const res = await addV4Division(payload)
      div.id = res.data
    }
  }
  await Promise.all(divisionDrafts.value.filter((d) => d.id && !snapshotMap[d.id!]).map((d) => loadSnapshot(d)))
  dirty.value = false
  return true
}

async function persistDivisionTimes(): Promise<boolean> {
  for (const div of divisionDrafts.value) {
    const formRef = timeFormRefs.get(div.localKey)
    if (formRef) {
      try {
        await formRef.validate()
      } catch {
        timeTabKey.value = div.localKey
        return false
      }
    }
  }
  if (!validateAllDivisionTimes()) return false
  await Promise.all(divisionDrafts.value.filter((div) => div.id).map((div) =>
    updateV4Division(div.id!, {
      registrationStartTime: div.registrationStartTime,
      registrationEndTime: div.registrationEndTime,
      matchStartTime: div.matchStartTime,
      matchEndTime: div.matchEndTime
    })
  ))
  dirty.value = false
  return true
}

async function handleSaveDraft() {
  saving.value = true
  try {
    if (currentStep.value === 0) {
      await seasonFormRef.value?.validate()
      await persistSeason()
    } else {
      await seasonFormRef.value?.validate().catch(() => undefined)
      if (seasonForm.id || currentStep.value > 0) {
        if (!seasonForm.id) {
          const ok = await persistSeason()
          if (!ok) return
        } else {
          await persistSeason()
        }
      }
      if (currentStep.value >= 1) {
        await persistDivisionsBasic()
      }
      if (currentStep.value >= 2) {
        await persistDivisionTimes()
      }
    }
    proxy!.$modal.msgSuccess('草稿已保存')
    emit('success')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function goNext() {
  saving.value = true
  try {
    if (currentStep.value === 0) {
      await seasonFormRef.value?.validate()
      const ok = await persistSeason()
      if (!ok) return
      completedSteps.value[0] = true
    } else if (currentStep.value === 1) {
      const ok = await persistDivisionsBasic()
      if (!ok) return
      completedSteps.value[1] = true
      if (!timeTabKey.value) timeTabKey.value = divisionDrafts.value[0]?.localKey || ''
      if (!rulesTabKey.value) rulesTabKey.value = divisionDrafts.value[0]?.localKey || ''
    } else if (currentStep.value === 2) {
      const ok = await persistDivisionTimes()
      if (!ok) return
      completedSteps.value[2] = true
      currentStep.value += 1
      wizardLoading.value = true
      try {
        await loadAllSnapshots()
      } finally {
        wizardLoading.value = false
      }
      return
    } else if (currentStep.value === 3) {
      await loadAllSnapshots()
      const formatErrors: string[] = []
      for (const div of divisionDrafts.value) {
        const label = div.divisionName || formatV4DivisionComboLabel(div.platformCode, div.gameMode, div.registrationMode)
        if (!div.id) {
          formatErrors.push(`「${label}」尚未保存`)
          continue
        }
        const stages = sortedStages(div)
        if (!stages.length) {
          formatErrors.push(`「${label}」至少需要配置一个比赛阶段`)
          continue
        }
        const validation = validateV4StageCombinationDetailed(stages, div.maxTeamCount)
        if (!validation.valid) {
          formatErrors.push(`「${label}」：${validation.messages.join('；')}`)
        }
      }
      if (formatErrors.length) {
        proxy!.$modal.msgError(formatErrors.join('\n'))
        return
      }
      completedSteps.value[3] = true
    }
    currentStep.value += 1
    if (currentStep.value === 4) {
      await loadAllSnapshots()
      syncOpenRegistrationSelection()
    }
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function goPrev() {
  if (currentStep.value > 0) currentStep.value -= 1
}

async function ensureDivisionFormatsLocked() {
  for (const div of divisionDrafts.value) {
    if (!div.id) continue
    const snap = snapshotMap[div.id]
    if (!snap) continue
    const status = (snap.snapshotStatus || '').toUpperCase()
    if (status !== 'DRAFT') continue
    const stages = sortedStages(div)
    const validation = validateV4StageCombinationDetailed(stages, div.maxTeamCount)
    if (!validation.valid) {
      const label = div.divisionName || formatV4DivisionComboLabel(div.platformCode, div.gameMode, div.registrationMode)
      throw new Error(`「${label}」赛制配置不完整：${validation.messages.join('；')}`)
    }
    await lockFormatSnapshot(div.id)
  }
  await loadAllSnapshots()
}

async function handlePublishSeason() {
  if (saving.value) return
  try {
    await proxy!.$modal.confirm('本次仅发布赛季展示内容，不开放任何平台赛区报名，是否继续？')
  } catch {
    return
  }
  saving.value = true
  try {
    await persistSeason()
    await persistDivisionsBasic()
    await persistDivisionTimes()
    await ensureDivisionFormatsLocked()
    if (!seasonForm.id) throw new Error('赛季未创建')
    await publishV4Season(seasonForm.id)
    proxy!.$modal.msgSuccess('赛季已发布（未开放任何赛区报名）')
    dirty.value = false
    visibleProxy.value = false
    emit('success')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '发布失败')
  } finally {
    saving.value = false
  }
}

async function handlePublishAndOpen() {
  if (!canPublishAndOpen.value || saving.value) {
    proxy!.$modal.msgError(publishAndOpenDisabledReason.value || '请先勾选满足开放报名条件的赛区')
    return
  }
  const targets = selectedOpenDivisions.value.filter((div) => div.id && canSelectDivisionForOpen(div))
  const lines = targets.map((div) => `- ${div.divisionName || formatV4DivisionComboLabel(div.platformCode, div.gameMode, div.registrationMode)}`).join('\n')
  try {
    await proxy!.$modal.confirm(`将发布赛季，并开放以下赛区报名：\n${lines}\n\n是否继续？`)
  } catch {
    return
  }
  saving.value = true
  try {
    await persistSeason()
    await persistDivisionsBasic()
    await persistDivisionTimes()
    await ensureDivisionFormatsLocked()
    if (!seasonForm.id) throw new Error('赛季未创建')
    await publishV4Season(seasonForm.id)

    for (const div of targets) {
      await openV4DivisionRegistration(div.id!)
    }

    proxy!.$modal.msgSuccess(`赛季已发布，${targets.length} 个赛区已开放报名`)
    dirty.value = false
    visibleProxy.value = false
    emit('success')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '发布失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  if (dirty.value) {
    handleBeforeClose(() => { visibleProxy.value = false })
  } else {
    visibleProxy.value = false
  }
}

function handleBeforeClose(done: () => void) {
  if (!dirty.value) {
    done()
    return
  }
  proxy!.$modal.confirm('当前存在未保存修改，是否保存为草稿后退出？', '提示', {
    confirmButtonText: '保存并退出',
    cancelButtonText: '放弃修改',
    distinguishCancelAndClose: true,
    type: 'warning'
  }).then(async () => {
    await handleSaveDraft()
    done()
  }).catch((action: string) => {
    if (action === 'cancel') {
      dirty.value = false
      done()
    }
  })
}

async function handleRulesTabChange() {
  const div = divisionDrafts.value.find((d) => d.localKey === rulesTabKey.value)
  if (div?.id) {
    await loadSnapshot(div)
  }
}
</script>

<style scoped>
.publish-wizard-dialog :deep(.el-dialog) {
  width: min(1200px, 94vw);
  max-height: 88vh;
  margin: 6vh auto;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.publish-wizard-dialog :deep(.el-dialog__header) {
  flex-shrink: 0;
  padding-bottom: 12px;
}

.publish-wizard-dialog :deep(.el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 0;
}

.publish-wizard-dialog :deep(.el-dialog__footer) {
  flex-shrink: 0;
  border-top: 1px solid var(--el-border-color-lighter);
  padding: 12px 20px;
}

.wizard-shell {
  display: flex;
  flex-direction: column;
  max-height: calc(88vh - 130px);
  overflow: hidden;
}

.wizard-steps-wrap {
  flex-shrink: 0;
  padding: 0 24px 12px;
}

.wizard-steps {
  width: 100%;
}

.wizard-steps :deep(.el-step__title) {
  white-space: nowrap;
  font-size: 13px;
  line-height: 1.4;
}

.wizard-steps :deep(.el-step.step-clickable) {
  cursor: pointer;
}

.wizard-steps :deep(.el-step.step-clickable .el-step__title:hover) {
  color: var(--el-color-primary);
}

.dialog-content {
  flex: 1;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 8px 24px 24px;
}

.form-main {
  max-width: 900px;
  margin: 0 auto;
}

.form-main--wide {
  max-width: 100%;
}

.wizard-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.image-upload-row {
  margin-top: 4px;
}

.wizard-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
  flex-wrap: wrap;
}

.wizard-footer-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.step-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.step-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.division-card {
  margin-bottom: 12px;
}
.division-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stage-card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rules-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.rules-status-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rules-status-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.rules-section-tabs :deep(.el-tabs__content) {
  padding: 12px;
}

.scheme-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin: 8px 0;
}

.scheme-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.scheme-card {
  cursor: pointer;
  transition: border-color 0.2s;
}

.scheme-card--active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-5);
}

.scheme-card-name {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 6px;
}

.scheme-card-summary {
  font-size: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.5;
  margin-bottom: 6px;
}

.scheme-card-scene {
  font-size: 11px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
  margin-bottom: 8px;
}

.scheme-flow-mini {
  font-size: 11px;
  color: var(--el-color-primary);
}

.scheme-flow-step {
  white-space: nowrap;
}

.scheme-flow-arrow {
  margin: 0 4px;
  color: var(--el-text-color-placeholder);
}

.quick-params-card :deep(.el-card__header) {
  padding: 10px 16px;
  font-weight: 600;
}

.preview-card :deep(.el-card__header) {
  padding: 10px 16px;
  font-weight: 600;
}

.preview-nl {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-primary);
}

.preview-flow {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-color-primary);
}

.section-hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stage-flow-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.stage-flow-card {
  cursor: pointer;
}

.stage-flow-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.stage-flow-title {
  font-weight: 600;
  font-size: 14px;
}

.stage-flow-brief {
  font-size: 13px;
  color: var(--el-color-primary);
  margin-bottom: 8px;
}

.stage-flow-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  color: var(--el-text-color-secondary);
}

.stage-flow-arrow-icon {
  font-size: 18px;
  line-height: 1;
}

.stage-flow-advance {
  font-size: 12px;
  margin-top: 2px;
}

.stage-summary-list.compact {
  font-size: 12px;
  line-height: 1.6;
}

.stage-advanced-collapse {
  margin-top: 12px;
  border: none;
}

.ml8 {
  margin-left: 8px;
}

.text-success {
  color: var(--el-color-success);
}
.stage-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stage-card-actions {
  display: flex;
  gap: 4px;
}
.tie-break-summary {
  margin-top: 8px;
  font-size: 13px;
}
.tie-break-label {
  color: var(--el-text-color-secondary);
  margin-right: 4px;
}
.stage-summary-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
  font-size: 13px;
  line-height: 1.8;
}

.stage-summary-label {
  color: var(--el-text-color-secondary);
}

.stage-preset-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.stage-preset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.stage-preset-card {
  cursor: pointer;
}

.stage-preset-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.stage-preset-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.tie-break-intro {
  margin: 0 0 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.tie-break-fixed-row,
.tie-break-middle-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.tie-break-order {
  width: 24px;
  color: var(--el-text-color-secondary);
}

.tie-break-name {
  flex: 1;
}

.mr4 {
  margin-right: 4px;
}
.mt8 {
  margin-top: 8px;
}
.mb8 {
  margin-bottom: 8px;
}
.text-center {
  text-align: center;
}
.confirm-section-title {
  font-weight: 600;
  margin: 16px 0 8px;
}
.confirm-tip-list {
  margin: 4px 0 0;
  padding-left: 18px;
  line-height: 1.7;
  font-size: 13px;
}
.rules-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
.readiness-list {
  margin: 4px 0 0;
  padding-left: 18px;
  line-height: 1.7;
  font-size: 13px;
}
.readiness-reasons {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-color-warning);
}
.cell-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.reason-text {
  font-size: 12px;
  color: var(--el-color-warning);
  line-height: 1.5;
}
.text-muted {
  color: var(--el-text-color-secondary);
}

.readonly-info-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 13px;
}

.readonly-info-value {
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.readonly-info-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.tab-label-with-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 220px;
}

.tab-label-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
  display: inline-block;
  vertical-align: bottom;
}

.tab-status-icon--valid {
  color: var(--el-color-success);
}

.tab-status-icon--warn {
  color: var(--el-color-warning);
}

.tab-status-icon--error {
  color: var(--el-color-danger);
}

.confirm-format-chain {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.confirm-format-step {
  line-height: 1.4;
}

.confirm-format-title {
  font-size: 13px;
  font-weight: 500;
}

.confirm-format-arrow {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1;
  padding: 2px 0;
}

@media (max-width: 768px) {
  .publish-wizard-dialog :deep(.el-dialog) {
    width: 94vw !important;
    margin: 4vh auto;
    max-height: 92vh;
  }

  .wizard-shell {
    max-height: calc(92vh - 120px);
  }

  .wizard-steps-wrap {
    padding: 0 12px 10px;
  }

  .wizard-steps :deep(.el-step__title) {
    font-size: 12px;
  }

  .dialog-content {
    padding: 8px 16px 20px;
  }

  .wizard-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .wizard-footer-left,
  .wizard-footer-right {
    justify-content: center;
  }
}
</style>
