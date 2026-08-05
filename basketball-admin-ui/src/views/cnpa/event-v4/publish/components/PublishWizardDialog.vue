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
            <el-select v-model="seasonForm.seriesId" style="width: 100%" :disabled="!!seasonForm.id">
              <el-option v-for="item in seriesList" :key="item.id" :label="v4SeriesDisplayLabel(item.seriesName, item.seriesCode)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="赛季年份" prop="seasonYear">
            <el-input-number v-model="seasonForm.seasonYear" :min="2020" :max="2099" controls-position="right" style="width: 100%" />
          </el-form-item>
          <el-form-item label="赛季名称" prop="seasonName">
            <el-input v-model="seasonForm.seasonName" placeholder="如 S1、春季赛、2026 第一赛季" maxlength="128" show-word-limit />
          </el-form-item>
          <el-form-item label="前台展示标题" prop="displayTitle">
            <el-input v-model="seasonForm.displayTitle" placeholder="如 CNPA League 2026 S1" maxlength="128" show-word-limit />
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
            <div class="el-form-item__tip">表示赛季展示周期，不代表各平台赛区的具体比赛时间</div>
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
              <el-button link type="danger" @click="removeDivisionDraft(index)" :disabled="divisionDrafts.length <= 1">删除</el-button>
            </div>
          </template>
          <el-form :ref="(el) => setDivisionFormRef(div.localKey, el)" :model="div" :rules="divisionBasicRules" label-width="120px">
            <el-form-item label="平台" prop="platformCode">
              <el-select v-model="div.platformCode" style="width: 100%">
                <el-option label="PC" value="PC" />
                <el-option label="PS/Xbox" value="PS_XBOX" />
              </el-select>
            </el-form-item>
            <el-form-item label="比赛模式" prop="gameMode">
              <el-radio-group v-model="div.gameMode">
                <el-radio label="FIVE_V_FIVE">5V5</el-radio>
                <el-radio label="THREE_V_THREE">3V3</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="赛区名称" prop="divisionName">
              <el-input v-model="div.divisionName" placeholder="如 League 2026 S1 - PS" />
            </el-form-item>
            <el-form-item label="报名方式" prop="registrationMode">
              <el-radio-group v-model="div.registrationMode">
                <el-radio label="TEAM">球队报名</el-radio>
                <el-radio label="PERSONAL">个人报名</el-radio>
              </el-radio-group>
              <div v-if="div.registrationMode === 'PERSONAL'" class="el-form-item__tip">个人报名审核通过后，由运营线下抽签组队并录入结果。</div>
            </el-form-item>
            <el-form-item label="队伍上限" prop="maxTeamCount">
              <el-input-number v-model="div.maxTeamCount" :min="1" controls-position="right" style="width: 100%" />
            </el-form-item>
            <el-form-item label="阵容人数">
              <el-input :model-value="v4RosterRuleText(div.gameMode)" disabled />
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <!-- Step 3: Division 时间 -->
      <div v-show="currentStep === 2" class="form-main form-main--wide">
        <el-tabs v-model="timeTabKey" type="card">
          <el-tab-pane v-for="(div, index) in divisionDrafts" :key="div.localKey" :label="divisionTabLabel(div, index)" :name="div.localKey">
            <el-form :ref="(el) => setTimeFormRef(div.localKey, el)" :model="div" :rules="divisionTimeRules" label-width="120px">
              <el-form-item label="报名开始" prop="registrationStartTime">
                <el-date-picker v-model="div.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
              <el-form-item label="报名结束" prop="registrationEndTime">
                <el-date-picker v-model="div.registrationEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
              <el-form-item label="比赛开始" prop="matchStartTime">
                <el-date-picker v-model="div.matchStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
              <el-form-item label="比赛结束" prop="matchEndTime">
                <el-date-picker v-model="div.matchEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- Step 4: 赛制规则 -->
      <div v-show="currentStep === 3" class="form-main form-main--wide">
        <el-tabs v-model="rulesTabKey" type="card" @tab-change="handleRulesTabChange">
          <el-tab-pane v-for="(div, index) in divisionDrafts" :key="div.localKey" :label="divisionTabLabel(div, index)" :name="div.localKey">
            <el-form label-width="120px" class="mb8">
              <el-form-item label="赛制模板">
                <el-select v-model="div.formatTemplateId" style="width: 100%" placeholder="请选择赛制模板" @change="() => handleTemplateChange(div)">
                  <el-option v-for="item in formatTemplateList" :key="item.id" :label="`${item.templateName} (${item.templateCode})`" :value="item.id" />
                </el-select>
                <div class="el-form-item__tip">支持单循环、小组赛、BO1 单败淘汰等模板；开放报名前需锁定规则快照。</div>
              </el-form-item>
            </el-form>

            <div v-if="div.id" class="rules-actions mb8">
              <el-button type="warning" plain @click="handleRebuildSnapshot(div)" v-if="currentSnapshot(div)?.mutable">重建快照</el-button>
              <el-button type="success" plain @click="handleLockSnapshot(div)" v-if="currentSnapshot(div)?.lockable">锁定规则</el-button>
            </div>

            <el-alert v-if="currentSnapshot(div) && !currentSnapshot(div)?.mutable" type="warning" :closable="false" show-icon class="mb8" title="规则已锁定，不可编辑" />

            <el-descriptions v-if="currentSnapshot(div)" :column="2" border size="small" class="mb8">
              <el-descriptions-item label="模板">{{ currentSnapshot(div)?.sourceTemplateName }} v{{ currentSnapshot(div)?.sourceTemplateVersion }}</el-descriptions-item>
              <el-descriptions-item label="快照状态">
                <el-tag :type="v4SnapshotStatusTagType(currentSnapshot(div)?.snapshotStatus)" size="small">
                  {{ currentSnapshot(div)?.snapshotStatusName || v4SnapshotStatusLabel(currentSnapshot(div)?.snapshotStatus) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="规则摘要" :span="2">{{ currentSnapshot(div)?.formatSummary || '-' }}</el-descriptions-item>
            </el-descriptions>

            <div v-if="sortedStages(div).length" class="stage-card-list">
              <el-card v-for="(stage, sIndex) in sortedStages(div)" :key="stage.id || stage.stageCode" shadow="hover" class="stage-card">
                <template #header>
                  <div class="stage-card-header">
                    <span>{{ stage.stageOrder }}. {{ stage.stageName }}（{{ stage.stageCode }}）</span>
                    <div v-if="currentSnapshot(div)?.mutable" class="stage-card-actions">
                      <el-button link type="primary" @click="openEditStage(div, stage)">编辑</el-button>
                      <el-button link type="danger" @click="handleDeleteStage(div, stage)">删除</el-button>
                      <el-button link :disabled="sIndex === 0" @click="moveStage(div, stage, -1)">上移</el-button>
                      <el-button link :disabled="sIndex === sortedStages(div).length - 1" @click="moveStage(div, stage, 1)">下移</el-button>
                    </div>
                  </div>
                </template>
                <el-descriptions :column="2" size="small">
                  <el-descriptions-item label="阶段类型">{{ stage.stageTypeName || v4StageTypeLabel(stage.stageType) }}</el-descriptions-item>
                  <el-descriptions-item label="比赛方式">{{ stage.competitionFormatName || v4CompetitionFormatLabel(stage.competitionFormat) }}</el-descriptions-item>
                  <el-descriptions-item label="分组模式">{{ stage.groupingModeName || v4GroupingModeLabel(stage.groupingMode) }}</el-descriptions-item>
                  <el-descriptions-item label="组数">{{ stage.groupCount ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="BO">{{ stage.bestOf ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="积分">{{ stage.winPoints != null ? `${stage.winPoints}/${stage.lossPoints}` : '-' }}</el-descriptions-item>
                  <el-descriptions-item label="晋级">{{ stage.advancementTypeName || v4AdvancementTypeLabel(stage.advancementType) }}</el-descriptions-item>
                  <el-descriptions-item label="晋级名额">{{ stage.advancementCount ?? '-' }}</el-descriptions-item>
                </el-descriptions>
                <div v-if="stage.competitionFormat === 'ROUND_ROBIN' && stage.tieBreakRules?.length" class="tie-break-summary">
                  <span class="tie-break-label">同分规则：</span>
                  <el-tag v-for="rule in stage.tieBreakRules" :key="rule.id" size="small" class="mr4" :type="rule.enabled === 1 ? '' : 'info'">
                    {{ rule.priorityNo }}. {{ rule.ruleCodeName || v4TieBreakRuleLabel(rule.ruleCode) }}
                  </el-tag>
                </div>
              </el-card>
            </div>
            <el-empty v-else-if="div.id" description="暂无阶段，请从模板重建或新增阶段" />
            <el-empty v-else description="请先完成前两步并保存赛区" />

            <div v-if="currentSnapshot(div)?.mutable && div.id" class="mt8">
              <el-button type="primary" plain icon="Plus" @click="openAddStage(div)">新增阶段</el-button>
            </div>
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

        <div class="confirm-section-title">平台赛区（{{ divisionDrafts.length }}）</div>
        <el-table :data="divisionDrafts" border size="small" class="mb8">
          <el-table-column label="平台" width="90">
            <template #default="scope">{{ v4PlatformLabel(scope.row.platformCode) }}</template>
          </el-table-column>
          <el-table-column label="模式" width="70">
            <template #default="scope">{{ v4GameModeLabel(scope.row.gameMode) }}</template>
          </el-table-column>
          <el-table-column label="赛区名称" prop="divisionName" min-width="140" />
          <el-table-column label="报名方式" width="100">
            <template #default="scope">{{ v4RegistrationModeLabel(scope.row.registrationMode) }}</template>
          </el-table-column>
          <el-table-column label="队伍上限" prop="maxTeamCount" width="90" align="center" />
          <el-table-column label="报名时间" min-width="160">
            <template #default="scope">{{ formatV4CompactRange(scope.row.registrationStartTime, scope.row.registrationEndTime) }}</template>
          </el-table-column>
          <el-table-column label="规则状态" width="96" align="center">
            <template #default="scope">
              <el-tag :type="v4SnapshotStatusTagType(snapshotMap[scope.row.id!]?.snapshotStatus)" size="small">
                {{ v4SnapshotStatusLabel(snapshotMap[scope.row.id!]?.snapshotStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="就绪" width="72" align="center">
            <template #default="scope">
              <el-icon v-if="isDivisionReady(scope.row)" color="var(--el-color-success)"><CircleCheck /></el-icon>
              <el-icon v-else color="var(--el-color-warning)"><Warning /></el-icon>
            </template>
          </el-table-column>
        </el-table>

        <el-alert type="info" :closable="false" show-icon>
          <template #title>发布说明</template>
          <ul class="confirm-tip-list">
            <li>保存草稿：保留当前 Season 与 Division 配置，不发布前台。</li>
            <li>发布赛季：前台展示赛季内容；无有效赛区时不展示报名入口。</li>
            <li>发布并开放报名：发布赛季后，对配置完整且规则已锁定的赛区开放报名（以后端校验为准）。</li>
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
          <el-button v-if="currentStep > 0" @click="goPrev">上一步</el-button>
          <el-button v-if="currentStep < 4" type="primary" @click="goNext" :loading="saving">下一步</el-button>
          <template v-if="currentStep === 4">
            <el-button type="primary" @click="handlePublishSeason" :loading="saving" v-hasPermi="['basket:v4Season:publish']">发布赛事</el-button>
            <el-button type="success" @click="handlePublishAndOpen" :loading="saving" v-hasPermi="['basket:v4Division:openRegistration']">发布并开放报名</el-button>
          </template>
        </div>
      </div>
    </template>
  </el-dialog>

  <!-- 阶段编辑弹窗（独立于主向导弹窗） -->
  <el-dialog :title="stageDialogTitle" v-model="stageDialogOpen" width="720px" append-to-body destroy-on-close>
      <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-width="120px">
        <el-form-item label="阶段编码" prop="stageCode">
          <el-input v-model="stageForm.stageCode" :disabled="!!stageEditingId" placeholder="如 REGULAR" />
        </el-form-item>
        <el-form-item label="阶段名称" prop="stageName">
          <el-input v-model="stageForm.stageName" placeholder="如 常规赛" />
        </el-form-item>
        <el-form-item label="阶段类型" prop="stageType">
          <el-select v-model="stageForm.stageType" style="width: 100%">
            <el-option v-for="item in STAGE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="比赛方式" prop="competitionFormat">
          <el-select v-model="stageForm.competitionFormat" style="width: 100%">
            <el-option v-for="item in COMPETITION_FORMAT_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组模式" prop="groupingMode">
          <el-select v-model="stageForm.groupingMode" style="width: 100%">
            <el-option v-for="item in GROUPING_MODE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="组数">
          <el-input-number v-model="stageForm.groupCount" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="循环轮次">
          <el-input-number v-model="stageForm.roundRobinCycles" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="BO">
          <el-input-number v-model="stageForm.bestOf" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="胜/负积分">
          <el-col :span="11"><el-input-number v-model="stageForm.winPoints" :min="0" controls-position="right" style="width: 100%" /></el-col>
          <el-col :span="2" class="text-center">/</el-col>
          <el-col :span="11"><el-input-number v-model="stageForm.lossPoints" :min="0" controls-position="right" style="width: 100%" /></el-col>
        </el-form-item>
        <el-form-item label="晋级类型">
          <el-select v-model="stageForm.advancementType" style="width: 100%">
            <el-option v-for="item in ADVANCEMENT_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="晋级名额" v-if="needsAdvancementCount">
          <el-input-number v-model="stageForm.advancementCount" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下一阶段编码">
          <el-input v-model="stageForm.nextStageCode" placeholder="留空表示无下一阶段" />
        </el-form-item>
        <el-form-item label="人工确认">
          <el-switch v-model="stageForm.requiresManualConfirmation" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <template v-if="stageForm.competitionFormat === 'ROUND_ROBIN'">
          <el-divider content-position="left">同分规则（循环赛）</el-divider>
          <el-table :data="stageForm.tieBreakRules" size="small" border>
            <el-table-column label="优先级" width="80">
              <template #default="scope">
                <el-input-number v-model="scope.row.priorityNo" :min="1" controls-position="right" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="规则">
              <template #default="scope">{{ v4TieBreakRuleLabel(scope.row.ruleCode) }}</template>
            </el-table-column>
            <el-table-column label="启用" width="80" align="center">
              <template #default="scope">
                <el-switch v-model="scope.row.enabled" :active-value="1" :inactive-value="0" />
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="stageDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitStageForm">确定</el-button>
      </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import { CircleCheck, Warning } from '@element-plus/icons-vue'
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
  addFormatStage,
  deleteFormatStage,
  getFormatSnapshot,
  listV4FormatTemplates,
  lockFormatSnapshot,
  rebuildFormatSnapshot,
  reorderFormatStages,
  saveTieBreakRules,
  updateFormatStage,
  type BasketDivisionFormatSnapshotVo,
  type BasketDivisionStageSaveReq,
  type BasketDivisionStageVo,
  type BasketFormatTemplateListVo
} from '@/api/cnpa/event-v4/format-template'
import {
  V4_ADVANCEMENT_TYPE_OPTIONS,
  V4_COMPETITION_FORMAT_OPTIONS,
  V4_GROUPING_MODE_OPTIONS,
  V4_STAGE_TYPE_OPTIONS,
  V4_TIE_BREAK_RULE_OPTIONS,
  formatV4CompactRange,
  v4AdvancementTypeLabel,
  v4CompetitionFormatLabel,
  v4GameModeLabel,
  v4GroupingModeLabel,
  v4PlatformLabel,
  v4RegistrationModeLabel,
  v4RosterRuleText,
  v4SeriesDisplayLabel,
  v4SnapshotStatusLabel,
  v4SnapshotStatusTagType,
  v4StageTypeLabel,
  v4TieBreakRuleLabel
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

const STAGE_TYPE_OPTIONS = V4_STAGE_TYPE_OPTIONS
const COMPETITION_FORMAT_OPTIONS = V4_COMPETITION_FORMAT_OPTIONS
const GROUPING_MODE_OPTIONS = V4_GROUPING_MODE_OPTIONS
const ADVANCEMENT_TYPE_OPTIONS = V4_ADVANCEMENT_TYPE_OPTIONS
const TIE_BREAK_DEFAULTS = V4_TIE_BREAK_RULE_OPTIONS.map((item, idx) => ({
  ruleCode: item.value,
  priorityNo: idx + 1,
  enabled: item.value === 'MANUAL' ? 0 : 1
}))

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

const seasonRules = {
  seriesId: [{ required: true, message: '请选择赛事系列', trigger: 'change' }],
  seasonYear: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  seasonName: [{ required: true, message: '请输入赛季名称', trigger: 'blur' }],
  displayTitle: [{ required: true, message: '请输入前台展示标题', trigger: 'blur' }]
}

const divisionDrafts = ref<WizardDivisionDraft[]>([])
const divisionFormRefs = new Map<string, FormInstance>()
const timeFormRefs = new Map<string, FormInstance>()
const timeTabKey = ref('')
const rulesTabKey = ref('')
const formatTemplateList = ref<BasketFormatTemplateListVo[]>([])
const snapshotMap = reactive<Record<number, BasketDivisionFormatSnapshotVo>>({})

const divisionBasicRules = {
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  gameMode: [{ required: true, message: '请选择比赛模式', trigger: 'change' }],
  divisionName: [{ required: true, message: '请输入赛区名称', trigger: 'blur' }],
  registrationMode: [{ required: true, message: '请选择报名方式', trigger: 'change' }],
  maxTeamCount: [{ required: true, message: '请输入队伍上限', trigger: 'blur' }]
}

const divisionTimeRules = {
  registrationStartTime: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEndTime: [{ required: true, message: '请选择报名结束时间', trigger: 'change' }],
  matchStartTime: [{ required: true, message: '请选择比赛开始时间', trigger: 'change' }],
  matchEndTime: [{ required: true, message: '请选择比赛结束时间', trigger: 'change' }]
}

const stageDialogOpen = ref(false)
const stageDialogTitle = ref('')
const stageFormRef = ref<FormInstance>()
const stageEditingId = ref<number>()
const stageEditingDivision = ref<WizardDivisionDraft>()
const stageForm = reactive<BasketDivisionStageSaveReq & { tieBreakRules?: Array<{ ruleCode: string; priorityNo: number; enabled: number }> }>({
  stageCode: '',
  stageName: '',
  stageType: 'REGULAR',
  competitionFormat: 'ROUND_ROBIN',
  groupingMode: 'NONE',
  groupCount: undefined,
  roundRobinCycles: 1,
  bestOf: 1,
  winPoints: 2,
  lossPoints: 1,
  advancementType: 'NONE',
  advancementCount: undefined,
  pairingRule: 'NONE',
  nextStageCode: '',
  requiresManualConfirmation: 0,
  tieBreakRules: []
})

const stageRules = {
  stageCode: [{ required: true, message: '请输入阶段编码', trigger: 'blur' }],
  stageName: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }],
  stageType: [{ required: true, message: '请选择阶段类型', trigger: 'change' }],
  competitionFormat: [{ required: true, message: '请选择比赛方式', trigger: 'change' }],
  groupingMode: [{ required: true, message: '请选择分组模式', trigger: 'change' }]
}

const needsAdvancementCount = computed(() =>
  stageForm.advancementType === 'OVERALL_TOP_N' || stageForm.advancementType === 'GROUP_TOP_N'
)

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
  const defaultTemplate = formatTemplateList.value.find((t) => t.isDefault === 1) || formatTemplateList.value[0]
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

function addDivisionDraft() {
  divisionDrafts.value.push(createEmptyDivision())
  dirty.value = true
}

function removeDivisionDraft(index: number) {
  divisionDrafts.value.splice(index, 1)
  dirty.value = true
}

function divisionTabLabel(div: WizardDivisionDraft, index: number) {
  const name = div.divisionName || `赛区 ${index + 1}`
  return `${name}（${v4PlatformLabel(div.platformCode)}）`
}

function currentSnapshot(div: WizardDivisionDraft) {
  return div.id ? snapshotMap[div.id] : undefined
}

function sortedStages(div: WizardDivisionDraft) {
  const snap = currentSnapshot(div)
  return [...(snap?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
}

function isDivisionReady(div: WizardDivisionDraft) {
  if (!div.id) return false
  const snap = snapshotMap[div.id]
  return !!div.registrationStartTime
    && !!div.registrationEndTime
    && !!div.matchStartTime
    && !!div.matchEndTime
    && (snap?.snapshotStatus === 'LOCKED' || snap?.locked)
}

function validateUniquePlatforms(): boolean {
  const platforms = divisionDrafts.value.map((d) => d.platformCode)
  const dup = platforms.find((p, i) => platforms.indexOf(p) !== i)
  if (dup) {
    proxy!.$modal.msgError(`平台 ${v4PlatformLabel(dup)} 重复，同一赛季下每个平台只能创建一个赛区`)
    return false
  }
  return true
}

function validateDivisionTimes(div: WizardDivisionDraft): boolean {
  if (div.registrationStartTime >= div.registrationEndTime) {
    proxy!.$modal.msgError(`「${div.divisionName}」报名结束时间必须晚于开始时间`)
    return false
  }
  if (div.matchStartTime >= div.matchEndTime) {
    proxy!.$modal.msgError(`「${div.divisionName}」比赛结束时间必须晚于开始时间`)
    return false
  }
  return true
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
  timeTabKey.value = divisionDrafts.value[0]?.localKey || ''
  rulesTabKey.value = divisionDrafts.value[0]?.localKey || ''
}

function inferResumeStep(divisions: WizardDivisionDraft[]): number {
  if (!divisions.length || divisions.every((d) => !d.id)) return 1
  const missingTimes = divisions.some((d) =>
    !d.registrationStartTime || !d.registrationEndTime || !d.matchStartTime || !d.matchEndTime
  )
  if (missingTimes) return 2
  const allLocked = divisions.every((d) => {
    if (!d.id) return false
    const snap = snapshotMap[d.id]
    return snap?.snapshotStatus === 'LOCKED' || snap?.locked
  })
  if (!allLocked) return 3
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
          sortOrder: d.sortOrder || 0
        }))
      }
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
    }
  } finally {
    wizardLoading.value = false
  }
}

function buildSeasonPayload(): BasketSeasonCreateReq {
  return {
    seriesId: seasonForm.seriesId,
    seasonYear: seasonForm.seasonYear,
    seasonName: seasonForm.seasonName,
    displayTitle: seasonForm.displayTitle,
    description: seasonForm.description,
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
    divisionName: div.divisionName,
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
  if (!validateUniquePlatforms()) return false

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
      await loadSnapshot(div)
    }
  }
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
        proxy!.$modal.msgError(`请完善「${div.divisionName || '赛区'}」的时间配置`)
        return false
      }
    }
    if (!validateDivisionTimes(div)) return false
    if (!div.id) continue
    await updateV4Division(div.id, {
      registrationStartTime: div.registrationStartTime,
      registrationEndTime: div.registrationEndTime,
      matchStartTime: div.matchStartTime,
      matchEndTime: div.matchEndTime
    })
  }
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
      await loadAllSnapshots()
    } else if (currentStep.value === 3) {
      await loadAllSnapshots()
      const unlocked = divisionDrafts.value.filter((d) => d.id && !isDivisionReady(d))
      if (unlocked.length) {
        proxy!.$modal.msgWarning('部分赛区规则尚未锁定，可在确认页发布赛季，开放报名需全部赛区规则锁定且配置完整')
      }
      completedSteps.value[3] = true
    }
    currentStep.value += 1
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function goPrev() {
  if (currentStep.value > 0) currentStep.value -= 1
}

async function handlePublishSeason() {
  saving.value = true
  try {
    await persistSeason()
    await persistDivisionsBasic()
    await persistDivisionTimes()
    if (!seasonForm.id) throw new Error('赛季未创建')
    await publishV4Season(seasonForm.id)
    proxy!.$modal.msgSuccess('赛季已发布')
    visibleProxy.value = false
    emit('success')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '发布失败')
  } finally {
    saving.value = false
  }
}

async function handlePublishAndOpen() {
  saving.value = true
  try {
    await persistSeason()
    await persistDivisionsBasic()
    await persistDivisionTimes()
    if (!seasonForm.id) throw new Error('赛季未创建')
    await publishV4Season(seasonForm.id)

    const readyDivisions = divisionDrafts.value.filter((d) => d.id)
    const opened: string[] = []
    const failed: string[] = []
    for (const div of readyDivisions) {
      try {
        await openV4DivisionRegistration(div.id!)
        opened.push(div.divisionName)
      } catch (e: any) {
        failed.push(`${div.divisionName}: ${e?.msg || e?.message || '开放失败'}`)
      }
    }

    if (opened.length && !failed.length) {
      proxy!.$modal.msgSuccess(`赛季已发布，${opened.length} 个赛区已开放报名`)
    } else if (opened.length && failed.length) {
      proxy!.$modal.msgWarning(`赛季已发布。已开放：${opened.join('、')}。未开放：${failed.join('；')}`)
    } else {
      proxy!.$modal.msgError(`赛季已发布，但开放报名失败：${failed.join('；')}`)
    }
    visibleProxy.value = false
    emit('success')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '发布失败')
  } finally {
    saving.value = false
  }
}

function handleCancel() {
  if (dirty.value || seasonForm.id) {
    proxy!.$modal.confirm('关闭向导？已保存的草稿可在赛季列表中继续编辑。').then(() => {
      visibleProxy.value = false
    }).catch(() => undefined)
  } else {
    visibleProxy.value = false
  }
}

function handleBeforeClose(done: () => void) {
  if (dirty.value) {
    proxy!.$modal.confirm('是否保存草稿后关闭？', '提示', {
      confirmButtonText: '保存草稿',
      cancelButtonText: '直接关闭',
      distinguishCancelAndClose: true,
      type: 'warning'
    }).then(async () => {
      await handleSaveDraft()
      done()
    }).catch((action: string) => {
      if (action === 'cancel') done()
    })
  } else {
    done()
  }
}

async function handleRulesTabChange() {
  const div = divisionDrafts.value.find((d) => d.localKey === rulesTabKey.value)
  if (div?.id) await loadSnapshot(div)
}

async function handleTemplateChange(div: WizardDivisionDraft) {
  if (!div.id || !div.formatTemplateId) return
  dirty.value = true
  await rebuildFormatSnapshot(div.id, div.formatTemplateId)
  await loadSnapshot(div)
  proxy!.$modal.msgSuccess('已切换模板并重建快照')
}

async function handleRebuildSnapshot(div: WizardDivisionDraft) {
  if (!div.id || !div.formatTemplateId) return
  await proxy!.$modal.confirm('重建将保留历史快照版本，当前草稿规则将被替换。是否继续？')
  await rebuildFormatSnapshot(div.id, div.formatTemplateId)
  await loadSnapshot(div)
  proxy!.$modal.msgSuccess('重建成功')
}

async function handleLockSnapshot(div: WizardDivisionDraft) {
  if (!div.id) return
  await proxy!.$modal.confirm('锁定后规则不可再编辑，开放报名前需锁定。是否继续？')
  try {
    await lockFormatSnapshot(div.id)
    await loadSnapshot(div)
    proxy!.$modal.msgSuccess('锁定成功')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '锁定失败，请检查规则配置是否完整')
  }
}

function resetStageForm() {
  Object.assign(stageForm, {
    stageCode: '',
    stageName: '',
    stageType: 'REGULAR',
    competitionFormat: 'ROUND_ROBIN',
    groupingMode: 'NONE',
    groupCount: undefined,
    roundRobinCycles: 1,
    bestOf: 1,
    winPoints: 2,
    lossPoints: 1,
    advancementType: 'NONE',
    advancementCount: undefined,
    pairingRule: 'NONE',
    nextStageCode: '',
    requiresManualConfirmation: 0,
    tieBreakRules: TIE_BREAK_DEFAULTS.map((r) => ({ ...r }))
  })
}

function openAddStage(div: WizardDivisionDraft) {
  stageEditingDivision.value = div
  stageEditingId.value = undefined
  stageDialogTitle.value = '新增阶段'
  resetStageForm()
  stageDialogOpen.value = true
}

function openEditStage(div: WizardDivisionDraft, stage: BasketDivisionStageVo) {
  stageEditingDivision.value = div
  stageEditingId.value = stage.id
  stageDialogTitle.value = `编辑阶段 — ${stage.stageName}`
  Object.assign(stageForm, {
    stageCode: stage.stageCode,
    stageName: stage.stageName,
    stageType: stage.stageType,
    competitionFormat: stage.competitionFormat,
    groupingMode: stage.groupingMode,
    groupCount: stage.groupCount,
    roundRobinCycles: stage.roundRobinCycles,
    bestOf: stage.bestOf,
    winPoints: stage.winPoints,
    lossPoints: stage.lossPoints,
    advancementType: stage.advancementType,
    advancementCount: stage.advancementCount,
    pairingRule: stage.pairingRule || 'NONE',
    nextStageCode: stage.nextStageCode || '',
    requiresManualConfirmation: stage.requiresManualConfirmation ?? 0,
    tieBreakRules: (stage.tieBreakRules?.length
      ? stage.tieBreakRules.map((r) => ({ ruleCode: r.ruleCode!, priorityNo: r.priorityNo!, enabled: r.enabled ?? 1 }))
      : TIE_BREAK_DEFAULTS.map((r) => ({ ...r })))
  })
  stageDialogOpen.value = true
}

async function submitStageForm() {
  const div = stageEditingDivision.value
  if (!div?.id) return
  await stageFormRef.value?.validate()
  const payload: BasketDivisionStageSaveReq = { ...stageForm }
  delete (payload as any).tieBreakRules
  if (stageEditingId.value) {
    await updateFormatStage(div.id, stageEditingId.value, payload)
    if (stageForm.competitionFormat === 'ROUND_ROBIN') {
      await saveTieBreakRules(div.id, { stageId: stageEditingId.value, rules: stageForm.tieBreakRules })
    }
  } else {
    const res = await addFormatStage(div.id, payload)
    if (stageForm.competitionFormat === 'ROUND_ROBIN' && res.data) {
      await saveTieBreakRules(div.id, { stageId: res.data, rules: stageForm.tieBreakRules })
    }
  }
  stageDialogOpen.value = false
  await loadSnapshot(div)
  proxy!.$modal.msgSuccess('保存成功')
}

async function handleDeleteStage(div: WizardDivisionDraft, stage: BasketDivisionStageVo) {
  if (!div.id || !stage.id) return
  await proxy!.$modal.confirm(`确认删除阶段「${stage.stageName}」？`)
  await deleteFormatStage(div.id, stage.id)
  await loadSnapshot(div)
  proxy!.$modal.msgSuccess('删除成功')
}

async function moveStage(div: WizardDivisionDraft, stage: BasketDivisionStageVo, delta: number) {
  if (!div.id || !stage.id) return
  const stages = sortedStages(div)
  const index = stages.findIndex((s) => s.id === stage.id)
  const target = index + delta
  if (target < 0 || target >= stages.length) return
  const ids = stages.map((s) => s.id!)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await reorderFormatStages(div.id, ids)
  await loadSnapshot(div)
}
</script>

<style scoped>
.publish-wizard-dialog :deep(.el-dialog) {
  width: min(1100px, 90vw);
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
