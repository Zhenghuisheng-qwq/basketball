<template>
  <div v-loading="panelLoading" class="format-rules-panel">
    <div class="rules-top-bar mb8">
      <el-radio-group
        :model-value="rulesMode"
        :disabled="!snapshot?.mutable"
        @change="(val) => handleRulesModeChange(val as RulesMode)"
      >
        <el-radio-button label="quick">快速创建</el-radio-button>
        <el-radio-button label="custom">自定义赛制</el-radio-button>
      </el-radio-group>
      <div class="rules-status-wrap">
        <span class="rules-status-label">赛制状态：</span>
        <el-tag :type="formatDisplayStatus.tagType" size="small">
          {{ formatDisplayStatus.label }}
        </el-tag>
      </div>
    </div>

    <el-alert
      v-if="snapshot && !snapshot.mutable"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
    >
      <template #title>赛制已确认并启用，后续报名和比赛将按此赛制执行。</template>
    </el-alert>

    <div v-if="snapshot?.mutable" class="rules-actions mb8">
      <el-button
        type="success"
        plain
        :loading="locking"
        :disabled="actionBusy"
        v-if="snapshot?.lockable"
        v-hasPermi="['basket:v4FormatRule:lock']"
        @click="handleLockSnapshot"
      >
        确认并启用赛制
      </el-button>
    </div>

    <el-tabs v-model="rulesSectionTab" type="border-card" class="rules-section-tabs mb8">
      <el-tab-pane label="快速创建" name="quick" :disabled="rulesMode === 'custom'">
        <div v-if="rulesMode === 'custom'" class="section-hint mb8">
          当前为自定义赛制模式。如需使用预设方案，请切换至「快速创建」模式。
        </div>
        <template v-else>
          <div class="scheme-section-title">单阶段赛制</div>
          <div class="scheme-card-grid mb8">
            <el-card
              v-for="item in singleStageSchemes"
              :key="item.id"
              shadow="hover"
              class="scheme-card"
              :class="{ 'scheme-card--active': selectedScheme === item.id }"
              @click="handleSelectScheme(item.id)"
            >
              <div class="scheme-card-name">{{ item.name }}</div>
              <div class="scheme-card-summary">{{ item.summary }}</div>
              <div class="scheme-card-scene">{{ item.scene }}</div>
              <div class="scheme-flow-mini">
                <span v-for="(step, si) in item.flowSteps" :key="si" class="scheme-flow-step">
                  {{ step }}<span v-if="si < item.flowSteps.length - 1" class="scheme-flow-arrow">→</span>
                </span>
              </div>
            </el-card>
          </div>
          <div class="scheme-section-title">组合阶段赛制</div>
          <div class="scheme-card-grid mb8">
            <el-card
              v-for="item in comboStageSchemes"
              :key="item.id"
              shadow="hover"
              class="scheme-card"
              :class="{ 'scheme-card--active': selectedScheme === item.id }"
              @click="handleSelectScheme(item.id)"
            >
              <div class="scheme-card-name">{{ item.name }}</div>
              <div class="scheme-card-summary">{{ item.summary }}</div>
              <div class="scheme-card-scene">{{ item.scene }}</div>
              <div class="scheme-flow-mini">
                <span v-for="(step, si) in item.flowSteps" :key="si" class="scheme-flow-step">
                  {{ step }}<span v-if="si < item.flowSteps.length - 1" class="scheme-flow-arrow">→</span>
                </span>
              </div>
            </el-card>
          </div>

          <el-card v-if="selectedScheme && selectedScheme !== 'CUSTOM'" shadow="never" class="quick-params-card mb8">
            <template #header>关键参数</template>
            <el-form label-width="140px" class="quick-params-form">
              <el-form-item label="预计参赛队伍">
                <span>{{ maxTeamCount }} 支</span>
                <span class="el-form-item__tip ml8">取自赛区「队伍上限」</span>
              </el-form-item>
              <template v-if="isGroupScheme(selectedScheme)">
                <el-form-item label="分组数量">
                  <el-input-number v-model="quickParams.groupCount" :min="2" :max="maxTeamCount" controls-position="right" @change="emitChange" />
                </el-form-item>
                <el-form-item label="分组产生方式">
                  <el-select v-model="quickParams.groupingMode" style="width: 100%" @change="emitChange">
                    <el-option v-for="opt in WIZARD_GROUPING_MODE_OPTIONS.filter(o => o.value !== 'NONE')" :key="opt.value" :label="opt.label" :value="opt.value" />
                  </el-select>
                </el-form-item>
                <el-form-item label="每组循环次数">
                  <el-input-number v-model="quickParams.roundRobinCycles" :min="1" controls-position="right" @change="emitChange" />
                </el-form-item>
                <el-form-item v-if="selectedScheme === 'GROUP_TO_ELIM'" label="每组晋级数量">
                  <el-input-number v-model="quickParams.advancePerGroup" :min="1" controls-position="right" @change="emitChange" />
                </el-form-item>
                <el-form-item v-if="selectedScheme === 'GROUP_TO_ELIM'" label="晋级总数">
                  <span>{{ calcV4AdvancementTotal(quickParams, 'GROUP_TO_ELIM') }} 支</span>
                  <span class="el-form-item__tip ml8">= 分组数量 × 每组晋级数量</span>
                </el-form-item>
              </template>
              <template v-if="selectedScheme === 'ROUND_ROBIN' || selectedScheme === 'ROUND_ROBIN_TO_ELIM'">
                <el-form-item label="循环次数">
                  <el-input-number v-model="quickParams.roundRobinCycles" :min="1" controls-position="right" @change="emitChange" />
                </el-form-item>
              </template>
              <el-form-item v-if="selectedScheme === 'ROUND_ROBIN_TO_ELIM'" label="晋级名额">
                <el-input-number v-model="quickParams.overallAdvanceCount" :min="2" :max="maxTeamCount" controls-position="right" @change="emitChange" />
              </el-form-item>
              <el-form-item v-if="hasEliminationStage(selectedScheme)" label="淘汰赛对阵方式">
                <el-select v-model="quickParams.pairingRule" style="width: 100%" @change="emitChange">
                  <el-option label="高排名对低排名" value="HIGH_VS_LOW" />
                  <el-option label="随机配对" value="RANDOM" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="applyingScheme" :disabled="actionBusy" @click="applyQuickCreateScheme">应用赛制方案</el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-alert v-for="(msg, mi) in quickValidation.messages" :key="mi" type="error" :closable="false" show-icon class="mb8" :title="msg" />

          <el-card v-if="selectedScheme && selectedScheme !== 'CUSTOM'" shadow="never" class="preview-card mb8">
            <template #header>赛制预览</template>
            <p class="preview-nl">{{ quickPreviewText }}</p>
            <el-descriptions :column="2" border size="small" class="mt8">
              <el-descriptions-item v-if="quickEstimate.groupCount" label="小组数量">{{ quickEstimate.groupCount }} 组</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.teamsPerGroup" label="每组预计队伍">{{ quickEstimate.teamsPerGroup }} 支</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.groupStageMatches" label="小组赛预计场次">{{ quickEstimate.groupStageMatches }} 场</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.advanceTotal" label="晋级队伍数">{{ quickEstimate.advanceTotal }} 支</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.eliminationStartRound" label="淘汰赛起始轮次">{{ quickEstimate.eliminationStartRound }}</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.eliminationMatches" label="淘汰赛预计场次">{{ quickEstimate.eliminationMatches }} 场</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.totalMatches" label="总预计场次" :span="2">{{ quickEstimate.totalMatches }} 场（最终以实际审核队伍数为准）</el-descriptions-item>
            </el-descriptions>
          </el-card>
        </template>
      </el-tab-pane>

      <el-tab-pane label="调整阶段" name="stages">
        <el-alert v-for="(msg, vi) in stageValidation.messages" :key="vi" type="warning" :closable="false" show-icon class="mb8" :title="msg" />

        <div v-if="sortedStages.length" class="stage-flow-list">
          <template v-for="(stage, sIndex) in sortedStages" :key="stage.id || stage.stageCode">
            <el-card shadow="hover" class="stage-flow-card" @click="snapshot?.mutable && openEditStage(stage)">
              <div class="stage-flow-card-head">
                <span class="stage-flow-title">阶段 {{ stage.stageOrder }}：{{ stage.stageName }}</span>
                <div v-if="snapshot?.mutable" class="stage-card-actions" @click.stop>
                  <el-button link type="primary" @click="openEditStage(stage)">编辑</el-button>
                  <el-button link type="danger" @click="handleDeleteStage(stage)">删除</el-button>
                  <el-button link :disabled="sIndex === 0" @click="moveStage(stage, -1)">上移</el-button>
                  <el-button link :disabled="sIndex === sortedStages.length - 1" @click="moveStage(stage, 1)">下移</el-button>
                </div>
              </div>
              <div class="stage-flow-brief">{{ formatV4StageFlowCardSummary(stage, sortedStages, maxTeamCount) }}</div>
              <ul class="stage-summary-list compact">
                <li><span class="stage-summary-label">参赛来源：</span>{{ formatV4StageParticipantSource(stage, sortedStages) }}</li>
                <li><span class="stage-summary-label">比赛形式：</span>{{ formatV4StageCompetitionFormLabel(stage) }}</li>
                <li><span class="stage-summary-label">积分规则：</span>{{ formatV4StagePointsRule(stage) }}</li>
                <li><span class="stage-summary-label">晋级安排：</span>{{ formatV4StageAdvancementArrangement(stage, sortedStages) }}</li>
                <li v-if="stage.competitionFormat === 'ROUND_ROBIN'">
                  <span class="stage-summary-label">同分规则：</span>{{ formatV4TieBreakSummaryArrow(stage.tieBreakRules) }}
                </li>
              </ul>
            </el-card>
            <div v-if="formatV4StageAdvanceCountLabel(stage)" class="stage-flow-connector">
              <span class="stage-flow-arrow-icon">↓</span>
              <span class="stage-flow-advance">{{ formatV4StageAdvanceCountLabel(stage) }}</span>
            </div>
          </template>
        </div>
        <el-empty v-else description="暂无比赛阶段，请在「快速创建」选择方案或手动添加" />

        <div v-if="snapshot?.mutable" class="mt8">
          <el-button type="primary" plain icon="Plus" v-hasPermi="['basket:v4FormatRule:edit']" @click="openAddStage">添加下一比赛阶段</el-button>
        </div>

        <el-card v-if="sortedStages.length" shadow="never" class="preview-card mt8">
          <template #header>完整赛制预览</template>
          <p class="preview-nl">{{ formatV4FormatNaturalLanguageFromStages(sortedStages, maxTeamCount) }}</p>
          <p class="preview-flow">{{ formatV4FormatFlowFromStages(sortedStages) }}</p>
          <el-descriptions :column="2" border size="small" class="mt8">
            <el-descriptions-item v-if="stageEstimate.groupCount" label="小组数量">{{ stageEstimate.groupCount }} 组</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.teamsPerGroup" label="每组预计队伍">{{ stageEstimate.teamsPerGroup }} 支</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.groupStageMatches" label="小组赛预计场次">{{ stageEstimate.groupStageMatches }} 场</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.advanceTotal" label="晋级队伍数">{{ stageEstimate.advanceTotal }} 支</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.eliminationStartRound" label="淘汰赛起始轮次">{{ stageEstimate.eliminationStartRound }}</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.eliminationMatches" label="淘汰赛预计场次">{{ stageEstimate.eliminationMatches }} 场</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.totalMatches" label="总预计场次" :span="2">{{ stageEstimate.totalMatches }} 场（最终以实际审核队伍数为准）</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="高级设置" name="advanced">
        <el-collapse>
          <el-collapse-item title="赛制模板与恢复" name="template">
            <el-form label-width="120px">
              <el-form-item label="底层模板">
                <el-select
                  :model-value="formatTemplateId"
                  style="width: 100%"
                  placeholder="请选择赛制模板"
                  :disabled="!snapshot?.mutable"
                  @change="handleTemplateChange"
                >
                  <el-option v-for="item in enabledFormatTemplates" :key="item.id" :label="item.templateName" :value="item.id!" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="warning"
                  plain
                  :loading="rebuilding"
                  :disabled="actionBusy"
                  v-if="snapshot?.mutable"
                  v-hasPermi="['basket:v4FormatRule:edit']"
                  @click="handleRebuildSnapshot"
                >
                  恢复模板默认配置
                </el-button>
              </el-form-item>
            </el-form>
          </el-collapse-item>
          <el-collapse-item v-for="stage in sortedStages" :key="stage.id || stage.stageCode" :title="`阶段 ${stage.stageOrder}：${stage.stageName}`" :name="String(stage.stageOrder)">
            <ul class="stage-summary-list">
              <li><span class="stage-summary-label">阶段名称：</span>{{ stage.stageName }}</li>
              <li><span class="stage-summary-label">胜/负积分：</span>{{ formatV4StagePointsRule(stage) }}</li>
              <li><span class="stage-summary-label">晋级方式：</span>{{ formatV4StageAdvancementArrangement(stage, sortedStages) }}</li>
              <li v-if="isV4GroupCompetitionStage(stage)"><span class="stage-summary-label">分组产生方式：</span>{{ v4GroupingModeLabel(stage.groupingMode) }}</li>
              <li v-if="stage.competitionFormat === 'ROUND_ROBIN'"><span class="stage-summary-label">同分规则：</span>{{ formatV4TieBreakSummaryArrow(stage.tieBreakRules) }}</li>
            </ul>
            <el-button v-if="snapshot?.mutable" link type="primary" @click="openEditStage(stage)">编辑此阶段高级参数</el-button>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-dialog title="添加下一比赛阶段" v-model="stagePresetDialogOpen" width="560px" append-to-body destroy-on-close>
    <p class="stage-preset-tip">请选择要添加的比赛阶段形式，系统将自动建立阶段顺序。</p>
    <div class="stage-preset-grid">
      <el-card
        v-for="item in STAGE_PRESET_OPTIONS"
        :key="item.value"
        shadow="hover"
        class="stage-preset-card"
        @click="handleSelectStagePreset(item.value)"
      >
        <div class="stage-preset-title">{{ item.label }}</div>
        <div class="stage-preset-desc">{{ item.description }}</div>
      </el-card>
    </div>
  </el-dialog>

  <el-dialog :title="stageDialogTitle" v-model="stageDialogOpen" width="720px" append-to-body destroy-on-close>
    <el-form ref="stageFormRef" :model="stageForm" :rules="stageRules" label-width="120px">
      <el-form-item label="阶段形式" prop="stageFormShape">
        <el-select v-model="stageFormShape" style="width: 100%" @change="handleStageFormShapeChange">
          <el-option v-for="item in STAGE_SHAPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="参赛来源">
        <div class="readonly-info-line">
          <span class="readonly-info-value">{{ stageParticipantSourcePreview }}</span>
        </div>
      </el-form-item>
      <template v-if="stageForm.competitionFormat === 'ROUND_ROBIN' && !isStageGroupMode">
        <el-form-item label="循环次数">
          <el-input-number v-model="stageForm.roundRobinCycles" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="胜/负积分">
          <el-col :span="11"><el-input-number v-model="stageForm.winPoints" :min="0" controls-position="right" style="width: 100%" /></el-col>
          <el-col :span="2" class="text-center">/</el-col>
          <el-col :span="11"><el-input-number v-model="stageForm.lossPoints" :min="0" controls-position="right" style="width: 100%" /></el-col>
        </el-form-item>
      </template>
      <template v-if="isStageGroupMode">
        <el-form-item label="分组产生方式" prop="groupingMode">
          <el-select v-model="stageForm.groupingMode" style="width: 100%">
            <el-option v-for="item in WIZARD_GROUPING_MODE_OPTIONS.filter(o => o.value !== 'NONE')" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="分组数量">
          <el-input-number v-model="stageForm.groupCount" :min="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每组循环次数">
          <el-input-number v-model="stageForm.roundRobinCycles" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="胜/负积分">
          <el-col :span="11"><el-input-number v-model="stageForm.winPoints" :min="0" controls-position="right" style="width: 100%" /></el-col>
          <el-col :span="2" class="text-center">/</el-col>
          <el-col :span="11"><el-input-number v-model="stageForm.lossPoints" :min="0" controls-position="right" style="width: 100%" /></el-col>
        </el-form-item>
      </template>
      <template v-if="stageForm.competitionFormat === 'SINGLE_ELIMINATION'">
        <el-form-item label="配对规则">
          <el-select v-model="stageForm.pairingRule" style="width: 100%">
            <el-option label="高排名对低排名" value="HIGH_VS_LOW" />
            <el-option label="随机配对" value="RANDOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="人工确认结果">
          <el-switch v-model="stageForm.requiresManualConfirmation" :active-value="1" :inactive-value="0" />
          <span class="el-form-item__tip ml8">开启后淘汰赛晋级需运营确认</span>
        </el-form-item>
      </template>
      <el-form-item label="对阵方式">
        <div class="readonly-info-line">
          <span class="readonly-info-value">{{ formatV4StagePairingRule(stageForm) }}</span>
          <span class="readonly-info-tip">BO3、BO5 暂未开放，当前均为 BO1。</span>
        </div>
      </el-form-item>
      <el-form-item label="晋级安排">
        <el-radio-group v-model="stageAdvancementMode" @change="handleAdvancementModeChange">
          <el-radio label="FINAL">本阶段为最终阶段</el-radio>
          <el-radio label="NEXT">晋级下一阶段</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="stageAdvancementMode === 'NEXT'">
        <el-form-item label="晋级方式">
          <el-select v-model="stageForm.advancementType" style="width: 100%">
            <el-option v-for="item in stageAdvancementTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="isStageGroupMode ? '每组晋级数量' : '晋级名额'">
          <el-input-number v-model="stageForm.advancementCount" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="下一比赛阶段">
          <el-select v-model="stageForm.nextStageCode" style="width: 100%" placeholder="请选择后续比赛阶段" clearable>
            <el-option
              v-for="item in subsequentStageOptions"
              :key="item.stageCode"
              :label="item.stageName"
              :value="item.stageCode!"
            />
          </el-select>
        </el-form-item>
      </template>
      <el-collapse class="stage-advanced-collapse">
        <el-collapse-item title="高级设置" name="advanced">
          <el-form-item label="阶段名称">
            <el-input v-model="stageForm.stageName" placeholder="如 常规赛" />
          </el-form-item>
          <template v-if="stageForm.competitionFormat === 'ROUND_ROBIN'">
            <el-divider content-position="left">同分规则</el-divider>
            <p class="tie-break-intro">球队积分相同时，系统按照以下顺序决定排名。</p>
            <div class="tie-break-fixed-row">
              <span class="tie-break-order">1</span>
              <span class="tie-break-name">胜场数</span>
              <el-tag size="small" type="info">固定首位</el-tag>
            </div>
            <div v-for="(rule, index) in tieBreakMiddleRules" :key="rule.ruleCode" class="tie-break-middle-row">
              <span class="tie-break-order">{{ index + 2 }}</span>
              <span class="tie-break-name">{{ v4TieBreakRuleLabel(rule.ruleCode) }}</span>
              <el-switch v-model="rule.enabled" :active-value="1" :inactive-value="0" />
              <el-button link :disabled="index === 0" @click="moveTieBreakRule(index, -1)">上移</el-button>
              <el-button link :disabled="index === tieBreakMiddleRules.length - 1" @click="moveTieBreakRule(index, 1)">下移</el-button>
            </div>
            <div class="tie-break-fixed-row">
              <span class="tie-break-order">{{ tieBreakMiddleRules.filter(r => r.enabled === 1).length + 2 }}</span>
              <span class="tie-break-name">人工裁定</span>
              <el-switch v-model="tieBreakManualEnabled" :active-value="1" :inactive-value="0" />
              <el-tag size="small" type="info">固定末位</el-tag>
            </div>
            <el-alert type="info" :closable="false" show-icon class="mt8" :title="tieBreakPreviewText" />
          </template>
        </el-collapse-item>
      </el-collapse>
    </el-form>
    <template #footer>
      <el-button @click="stageDialogOpen = false">取消</el-button>
      <el-button type="primary" @click="submitStageForm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, reactive, ref, watch } from 'vue'
import type { FormInstance } from 'element-plus'
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
  V4_FORMAT_SCHEME_OPTIONS,
  V4_WIZARD_STAGE_PRESET_OPTIONS,
  V4_WIZARD_TIE_BREAK_DEFAULTS,
  V4_WIZARD_GROUPING_MODE_OPTIONS,
  V4_WIZARD_TIE_BREAK_MIDDLE_OPTIONS,
  assessV4DivisionReadiness,
  buildDefaultV4QuickCreateParams,
  buildV4SchemeStagePayloads,
  buildV4WizardStagePresetForm,
  calcV4AdvancementTotal,
  calcV4FormatEstimateFromQuickParams,
  calcV4FormatEstimateFromStages,
  formatV4FormatFlowFromStages,
  formatV4FormatNaturalLanguageFromStages,
  formatV4LockConfirmPreview,
  formatV4QuickCreateNaturalLanguage,
  formatV4StageAdvanceCountLabel,
  formatV4StageAdvancementArrangement,
  formatV4StageCompetitionFormLabel,
  formatV4StageFlowCardSummary,
  formatV4StagePairingRule,
  formatV4StageParticipantSource,
  formatV4StagePointsRule,
  formatV4TieBreakPreview,
  formatV4TieBreakSummaryArrow,
  generateV4StageCode,
  getV4FormatSchemeOption,
  inferV4FormatSchemeFromStages,
  inferV4StageCodePreset,
  isV4GroupCompetitionStage,
  isV4WizardTemplateAllowed,
  normalizeV4WizardTieBreakRules,
  resolveV4WizardFormatDisplayStatus,
  validateV4QuickCreateParams,
  validateV4StageCombination,
  v4GroupingModeLabel,
  v4TieBreakRuleLabel,
  v4WizardAdvancementMode,
  v4WizardAdvancementTypeOptions,
  type V4FormatSchemeId,
  type V4QuickCreateParams,
  type V4SchemeStagePayload,
  type V4WizardStagePreset
} from '@/utils/v4AdminDisplay'

export interface DivisionFormatRulesMeta {
  id?: number
  maxTeamCount: number
  formatTemplateId?: number
  platformCode?: string
  gameMode?: string
  divisionName?: string
  registrationMode?: string
  registrationStartTime?: string
  registrationEndTime?: string
  matchStartTime?: string
  matchEndTime?: string
}

const props = withDefaults(defineProps<{
  divisionId: number
  maxTeamCount: number
  formatTemplateId?: number
  seriesId?: number
  divisionMeta?: DivisionFormatRulesMeta
  contextKey?: string
}>(), {
  divisionMeta: () => ({ maxTeamCount: 16 })
})

const emit = defineEmits<{
  'update:formatTemplateId': [value?: number]
  'snapshot-change': [snapshot: BasketDivisionFormatSnapshotVo]
  change: []
}>()

const { proxy } = getCurrentInstance()!

type RulesMode = 'quick' | 'custom'
type RulesSectionTab = 'quick' | 'stages' | 'advanced'

const STAGE_PRESET_OPTIONS = V4_WIZARD_STAGE_PRESET_OPTIONS
const STAGE_SHAPE_OPTIONS = V4_WIZARD_STAGE_PRESET_OPTIONS.map((item) => ({ label: item.label, value: item.value }))
const WIZARD_GROUPING_MODE_OPTIONS = V4_WIZARD_GROUPING_MODE_OPTIONS
const TIE_BREAK_DEFAULTS = V4_WIZARD_TIE_BREAK_DEFAULTS.map((item) => ({ ...item }))
const singleStageSchemes = V4_FORMAT_SCHEME_OPTIONS.filter((s) => s.category === 'single')
const comboStageSchemes = V4_FORMAT_SCHEME_OPTIONS.filter((s) => s.category === 'combo' || s.category === 'custom')

const panelLoading = ref(false)
const actionBusy = ref(false)
const locking = ref(false)
const rebuilding = ref(false)
const applyingScheme = ref(false)

const snapshot = ref<BasketDivisionFormatSnapshotVo>()
const formatTemplateList = ref<BasketFormatTemplateListVo[]>([])

const rulesMode = ref<RulesMode>('quick')
const rulesSectionTab = ref<RulesSectionTab>('quick')
const selectedScheme = ref<V4FormatSchemeId>('GROUP_TO_ELIM')
const quickParams = reactive<V4QuickCreateParams>(buildDefaultV4QuickCreateParams('GROUP_TO_ELIM', 16))

const stagePresetDialogOpen = ref(false)
const stageDialogOpen = ref(false)
const stageDialogTitle = ref('')
const stageFormRef = ref<FormInstance>()
const stageEditingId = ref<number>()
const stageFormShape = ref<V4WizardStagePreset>('ROUND_ROBIN')
const stageAdvancementMode = ref<'FINAL' | 'NEXT'>('FINAL')
const tieBreakMiddleRules = ref<Array<{ ruleCode: string; enabled: number }>>([])
const tieBreakManualEnabled = ref(1)
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
  stageName: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }],
  groupingMode: [{ required: true, message: '请选择分组产生方式', trigger: 'change' }]
}

const enabledFormatTemplates = computed(() =>
  formatTemplateList.value.filter((item) => isV4WizardTemplateAllowed(item.templateCode))
)

const sortedStages = computed(() =>
  [...(snapshot.value?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
)

const divisionReadiness = computed(() =>
  assessV4DivisionReadiness(
    {
      id: props.divisionId,
      maxTeamCount: props.maxTeamCount,
      formatTemplateId: props.formatTemplateId,
      platformCode: props.divisionMeta?.platformCode,
      gameMode: props.divisionMeta?.gameMode,
      divisionName: props.divisionMeta?.divisionName,
      registrationMode: props.divisionMeta?.registrationMode,
      registrationStartTime: props.divisionMeta?.registrationStartTime,
      registrationEndTime: props.divisionMeta?.registrationEndTime,
      matchStartTime: props.divisionMeta?.matchStartTime,
      matchEndTime: props.divisionMeta?.matchEndTime
    },
    snapshot.value
  )
)

const formatDisplayStatus = computed(() =>
  resolveV4WizardFormatDisplayStatus(snapshot.value, divisionReadiness.value, sortedStages.value.length)
)

const quickValidation = computed(() => {
  if (!selectedScheme.value || selectedScheme.value === 'CUSTOM') return { valid: true, messages: [] as string[] }
  return validateV4QuickCreateParams(selectedScheme.value, quickParams)
})

const stageValidation = computed(() =>
  validateV4StageCombination(sortedStages.value, props.maxTeamCount)
)

const quickPreviewText = computed(() => {
  if (!selectedScheme.value || selectedScheme.value === 'CUSTOM') return ''
  return formatV4QuickCreateNaturalLanguage(selectedScheme.value, quickParams)
})

const quickEstimate = computed(() => {
  if (!selectedScheme.value || selectedScheme.value === 'CUSTOM') return {}
  return calcV4FormatEstimateFromQuickParams(selectedScheme.value, quickParams)
})

const stageEstimate = computed(() =>
  calcV4FormatEstimateFromStages(sortedStages.value, props.maxTeamCount)
)

const isStageGroupMode = computed(() => isV4GroupCompetitionStage(stageForm))
const stageAdvancementTypeOptions = computed(() => v4WizardAdvancementTypeOptions(isStageGroupMode.value))

const subsequentStageOptions = computed(() => {
  const stages = sortedStages.value
  const currentOrder = stageEditingId.value
    ? stages.find((s) => s.id === stageEditingId.value)?.stageOrder || 0
    : stages.length
  return stages.filter((s) => (s.stageOrder || 0) > currentOrder && s.id !== stageEditingId.value)
})

const tieBreakPreviewText = computed(() => formatV4TieBreakPreview(buildStageTieBreakRules()))

const stageParticipantSourcePreview = computed(() => {
  const stages = sortedStages.value
  const editingOrder = stageEditingId.value
    ? stages.find((s) => s.id === stageEditingId.value)?.stageOrder
    : stages.length + 1
  return formatV4StageParticipantSource(
    { stageOrder: editingOrder, stageName: stageForm.stageName },
    stages
  )
})

function emitChange() {
  emit('change')
}

function isGroupScheme(schemeId: V4FormatSchemeId): boolean {
  return schemeId === 'GROUP_ROUND_ROBIN' || schemeId === 'GROUP_TO_ELIM'
}

function hasEliminationStage(schemeId: V4FormatSchemeId): boolean {
  return schemeId === 'SINGLE_ELIMINATION'
    || schemeId === 'ROUND_ROBIN_TO_ELIM'
    || schemeId === 'GROUP_TO_ELIM'
}

function ensureRulesUiState() {
  const inferred = inferV4FormatSchemeFromStages(sortedStages.value)
  if (inferred === 'CUSTOM' || !inferred) {
    rulesMode.value = sortedStages.value.length ? 'custom' : 'quick'
  }
  selectedScheme.value = inferred || 'GROUP_TO_ELIM'
  Object.assign(quickParams, buildDefaultV4QuickCreateParams(selectedScheme.value, props.maxTeamCount))
  quickParams.expectedTeamCount = props.maxTeamCount
}

function buildStageTieBreakRules() {
  return normalizeV4WizardTieBreakRules([
    { ruleCode: 'WIN_COUNT', priorityNo: 1, enabled: 1 },
    ...tieBreakMiddleRules.value.map((rule, idx) => ({ ...rule, priorityNo: idx + 2 })),
    { ruleCode: 'MANUAL', priorityNo: tieBreakMiddleRules.value.length + 2, enabled: tieBreakManualEnabled.value }
  ])
}

function syncTieBreakEditor(rules?: Array<{ ruleCode?: string; priorityNo?: number; enabled?: number }>) {
  const normalized = normalizeV4WizardTieBreakRules(rules)
  tieBreakMiddleRules.value = normalized
    .filter((r) => V4_WIZARD_TIE_BREAK_MIDDLE_OPTIONS.some((opt) => opt.value === r.ruleCode))
    .map((r) => ({ ruleCode: r.ruleCode, enabled: r.enabled }))
  const manual = normalized.find((r) => r.ruleCode === 'MANUAL')
  tieBreakManualEnabled.value = manual?.enabled ?? 1
}

function moveTieBreakRule(index: number, delta: number) {
  const target = index + delta
  if (target < 0 || target >= tieBreakMiddleRules.value.length) return
  const list = [...tieBreakMiddleRules.value]
  ;[list[index], list[target]] = [list[target], list[index]]
  tieBreakMiddleRules.value = list
}

function inferStageFormShape(stage: BasketDivisionStageVo): V4WizardStagePreset {
  if ((stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') return 'SINGLE_ELIMINATION'
  if (isV4GroupCompetitionStage(stage)) return 'GROUP_ROUND_ROBIN'
  return 'ROUND_ROBIN'
}

function applyStageFormShape(shape: V4WizardStagePreset) {
  const preset = buildV4WizardStagePresetForm(shape)
  Object.assign(stageForm, preset)
  if (stageAdvancementMode.value === 'FINAL') {
    stageForm.advancementType = 'NONE'
    stageForm.advancementCount = undefined
    stageForm.nextStageCode = ''
  } else if (shape === 'GROUP_ROUND_ROBIN' && stageForm.advancementType === 'NONE') {
    stageForm.advancementType = 'GROUP_TOP_N'
    stageForm.advancementCount = stageForm.advancementCount || 2
  }
}

function handleStageFormShapeChange(shape: V4WizardStagePreset) {
  applyStageFormShape(shape)
}

function handleAdvancementModeChange(mode: 'FINAL' | 'NEXT') {
  if (mode === 'FINAL') {
    stageForm.advancementType = 'NONE'
    stageForm.advancementCount = undefined
    stageForm.nextStageCode = ''
  } else if (stageForm.advancementType === 'NONE') {
    stageForm.advancementType = isStageGroupMode.value ? 'GROUP_TOP_N' : 'OVERALL_TOP_N'
    stageForm.advancementCount = isStageGroupMode.value ? 2 : 4
  }
}

async function loadFormatTemplates() {
  if (!props.seriesId) {
    formatTemplateList.value = []
    return
  }
  const res = await listV4FormatTemplates(props.seriesId)
  formatTemplateList.value = res.data || []
}

async function loadSnapshot() {
  if (!props.divisionId) return
  panelLoading.value = true
  try {
    const res = await getFormatSnapshot(props.divisionId)
    snapshot.value = res.data || {}
    emit('snapshot-change', snapshot.value!)
    ensureRulesUiState()
  } finally {
    panelLoading.value = false
  }
}

async function handleRulesModeChange(mode: RulesMode) {
  if (mode === rulesMode.value) return
  if (mode === 'quick' && rulesMode.value === 'custom') {
    if (snapshot.value?.mutable && sortedStages.value.length) {
      try {
        await proxy!.$modal.confirm('重新选择模板会覆盖当前未确认的赛制配置，是否继续？')
      } catch {
        return
      }
    }
  }
  rulesMode.value = mode
  if (mode === 'custom') {
    rulesSectionTab.value = 'stages'
    if (selectedScheme.value !== 'CUSTOM') {
      selectedScheme.value = 'CUSTOM'
    }
  } else {
    rulesSectionTab.value = 'quick'
  }
  emitChange()
}

async function handleSelectScheme(schemeId: V4FormatSchemeId) {
  if (schemeId === 'CUSTOM') {
    await handleRulesModeChange('custom')
    return
  }
  if (selectedScheme.value === schemeId) return
  selectedScheme.value = schemeId
  Object.assign(quickParams, buildDefaultV4QuickCreateParams(schemeId, props.maxTeamCount))
  emitChange()
}

async function syncStagesFromPayloads(payloads: V4SchemeStagePayload[]) {
  await loadSnapshot()
  let stages = [...(snapshot.value?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))

  while (stages.length > payloads.length) {
    const last = stages[stages.length - 1]
    if (!last?.id) break
    await deleteFormatStage(props.divisionId, last.id)
    await loadSnapshot()
    stages = [...(snapshot.value?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  }

  for (let i = 0; i < payloads.length; i += 1) {
    const { stage, tieBreakRules } = payloads[i]
    const payload = { ...stage }
    stages = [...(snapshot.value?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
    if (i < stages.length && stages[i]?.id) {
      await updateFormatStage(props.divisionId, stages[i].id!, payload)
      if (tieBreakRules?.length) {
        await saveTieBreakRules(props.divisionId, { stageId: stages[i].id, rules: tieBreakRules })
      }
    } else {
      if (!payload.stageCode) {
        const existingCodes = stages.map((s) => s.stageCode || '').filter(Boolean)
        const preset = inferV4StageCodePreset(stage)
        payload.stageCode = generateV4StageCode(preset, existingCodes)
      }
      const res = await addFormatStage(props.divisionId, payload)
      if (tieBreakRules?.length && res.data) {
        await saveTieBreakRules(props.divisionId, { stageId: res.data, rules: tieBreakRules })
      }
      await loadSnapshot()
    }
  }
  await loadSnapshot()
}

async function applyQuickCreateScheme() {
  if (actionBusy.value) return
  const schemeId = selectedScheme.value
  if (!schemeId || schemeId === 'CUSTOM') return
  const validation = quickValidation.value
  if (!validation.valid) {
    proxy!.$modal.msgError(validation.messages.join('\n'))
    return
  }
  const scheme = getV4FormatSchemeOption(schemeId)
  if (!scheme?.baseTemplateCode) {
    proxy!.$modal.msgError('该方案暂不可用')
    return
  }
  const template = formatTemplateList.value.find((t) => t.templateCode === scheme.baseTemplateCode)
  if (!template?.id) {
    proxy!.$modal.msgError('找不到对应的赛制模板，请联系管理员')
    return
  }

  applyingScheme.value = true
  actionBusy.value = true
  try {
    emit('update:formatTemplateId', template.id)
    await rebuildFormatSnapshot(props.divisionId, template.id)
    const payloads = buildV4SchemeStagePayloads(schemeId, quickParams)
    await syncStagesFromPayloads(payloads)
    rulesMode.value = 'quick'
    rulesSectionTab.value = 'stages'
    emitChange()
    proxy!.$modal.msgSuccess('赛制方案已应用，请在「调整阶段」查看流程预览')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '应用赛制方案失败')
  } finally {
    actionBusy.value = false
    applyingScheme.value = false
  }
}

async function handleTemplateChange(templateId: number) {
  if (!templateId || actionBusy.value) return
  actionBusy.value = true
  rebuilding.value = true
  try {
    emit('update:formatTemplateId', templateId)
    await rebuildFormatSnapshot(props.divisionId, templateId)
    await loadSnapshot()
    emitChange()
    proxy!.$modal.msgSuccess('已切换模板并恢复默认配置')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '切换模板失败')
  } finally {
    actionBusy.value = false
    rebuilding.value = false
  }
}

async function handleRebuildSnapshot() {
  if (!props.formatTemplateId || actionBusy.value) return
  try {
    await proxy!.$modal.confirm('恢复默认配置将保留历史版本，当前待确认的赛制将被模板默认配置替换。是否继续？')
  } catch {
    return
  }
  actionBusy.value = true
  rebuilding.value = true
  try {
    await rebuildFormatSnapshot(props.divisionId, props.formatTemplateId)
    await loadSnapshot()
    emitChange()
    proxy!.$modal.msgSuccess('已恢复模板默认配置')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '恢复默认配置失败')
  } finally {
    actionBusy.value = false
    rebuilding.value = false
  }
}

async function handleLockSnapshot() {
  if (actionBusy.value) return
  if (!sortedStages.value.length) {
    proxy!.$modal.msgError('确认前至少需要配置一个比赛阶段')
    return
  }
  const validation = stageValidation.value
  if (!validation.valid) {
    proxy!.$modal.msgError(validation.messages.join('\n'))
    return
  }
  const meta = props.divisionMeta || { maxTeamCount: props.maxTeamCount }
  try {
    await proxy!.$modal.confirm(formatV4LockConfirmPreview(meta, sortedStages.value))
  } catch {
    return
  }
  actionBusy.value = true
  locking.value = true
  try {
    await lockFormatSnapshot(props.divisionId)
    await loadSnapshot()
    emitChange()
    proxy!.$modal.msgSuccess('赛制已确认并启用，后续报名和比赛将按此赛制执行。')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '锁定失败，请检查规则配置是否完整')
  } finally {
    actionBusy.value = false
    locking.value = false
  }
}

function resetStageForm(preset: V4WizardStagePreset = 'ROUND_ROBIN') {
  stageFormShape.value = preset
  stageAdvancementMode.value = 'FINAL'
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
  applyStageFormShape(preset)
  syncTieBreakEditor(TIE_BREAK_DEFAULTS)
}

function openAddStage() {
  stageEditingId.value = undefined
  stagePresetDialogOpen.value = true
}

function handleSelectStagePreset(preset: V4WizardStagePreset) {
  stagePresetDialogOpen.value = false
  stageDialogTitle.value = '添加比赛阶段'
  resetStageForm(preset)
  stageAdvancementMode.value = preset === 'SINGLE_ELIMINATION' ? 'FINAL' : (sortedStages.value.length ? 'NEXT' : 'FINAL')
  if (stageAdvancementMode.value === 'NEXT') {
    handleAdvancementModeChange('NEXT')
  }
  stageDialogOpen.value = true
}

function openEditStage(stage: BasketDivisionStageVo) {
  stageEditingId.value = stage.id
  stageDialogTitle.value = `编辑比赛阶段 — ${stage.stageName}`
  stageFormShape.value = inferStageFormShape(stage)
  stageAdvancementMode.value = v4WizardAdvancementMode(stage.advancementType)
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
  syncTieBreakEditor(stageForm.tieBreakRules)
  stageDialogOpen.value = true
}

async function submitStageForm() {
  if (actionBusy.value) return
  actionBusy.value = true
  try {
    await stageFormRef.value?.validate()
    stageForm.bestOf = 1
    if (stageAdvancementMode.value === 'FINAL') {
      stageForm.advancementType = 'NONE'
      stageForm.advancementCount = undefined
      stageForm.nextStageCode = ''
    } else if (!stageForm.nextStageCode) {
      proxy!.$modal.msgError('请选择下一比赛阶段')
      return
    }
    if (!stageEditingId.value) {
      const existingCodes = sortedStages.value.map((s) => s.stageCode || '').filter(Boolean)
      const preset = inferV4StageCodePreset(stageForm)
      stageForm.stageCode = generateV4StageCode(preset, existingCodes)
    }
    stageForm.tieBreakRules = buildStageTieBreakRules()
    const payload: BasketDivisionStageSaveReq = { ...stageForm }
    delete (payload as any).tieBreakRules
    if (stageEditingId.value) {
      await updateFormatStage(props.divisionId, stageEditingId.value, payload)
      if (stageForm.competitionFormat === 'ROUND_ROBIN') {
        await saveTieBreakRules(props.divisionId, { stageId: stageEditingId.value, rules: stageForm.tieBreakRules })
      }
    } else {
      const res = await addFormatStage(props.divisionId, payload)
      if (stageForm.competitionFormat === 'ROUND_ROBIN' && res.data) {
        await saveTieBreakRules(props.divisionId, { stageId: res.data, rules: stageForm.tieBreakRules })
      }
    }
    stageDialogOpen.value = false
    await loadSnapshot()
    emitChange()
    proxy!.$modal.msgSuccess('保存成功')
  } catch (e: any) {
    if (e?.msg || e?.message) proxy!.$modal.msgError(e?.msg || e?.message)
  } finally {
    actionBusy.value = false
  }
}

async function handleDeleteStage(stage: BasketDivisionStageVo) {
  if (!stage.id) return
  await proxy!.$modal.confirm(`确认删除阶段「${stage.stageName}」？`)
  await deleteFormatStage(props.divisionId, stage.id)
  await loadSnapshot()
  emitChange()
  proxy!.$modal.msgSuccess('删除成功')
}

async function moveStage(stage: BasketDivisionStageVo, delta: number) {
  if (!stage.id) return
  const stages = sortedStages.value
  const index = stages.findIndex((s) => s.id === stage.id)
  const target = index + delta
  if (target < 0 || target >= stages.length) return
  const ids = stages.map((s) => s.id!)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await reorderFormatStages(props.divisionId, ids)
  await loadSnapshot()
  emitChange()
}

watch(
  () => [props.divisionId, props.seriesId] as const,
  async ([divisionId, seriesId]) => {
    if (!divisionId) return
    await loadFormatTemplates()
    quickParams.expectedTeamCount = props.maxTeamCount
    await loadSnapshot()
  },
  { immediate: true }
)

watch(
  () => props.maxTeamCount,
  (count) => {
    quickParams.expectedTeamCount = count
  }
)

defineExpose({ reload: loadSnapshot })
</script>

<style scoped>
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

.quick-params-card :deep(.el-card__header),
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

.stage-summary-list {
  margin: 0;
  padding-left: 0;
  list-style: none;
  font-size: 13px;
  line-height: 1.8;
}

.stage-summary-list.compact {
  font-size: 12px;
  line-height: 1.6;
}

.stage-summary-label {
  color: var(--el-text-color-secondary);
}

.stage-card-actions {
  display: flex;
  gap: 4px;
}

.stage-advanced-collapse {
  margin-top: 12px;
  border: none;
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

.readonly-info-line {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.readonly-info-value {
  font-size: 14px;
}

.readonly-info-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.rules-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.mb8 {
  margin-bottom: 8px;
}

.mt8 {
  margin-top: 8px;
}

.ml8 {
  margin-left: 8px;
}

.text-center {
  text-align: center;
}
</style>
