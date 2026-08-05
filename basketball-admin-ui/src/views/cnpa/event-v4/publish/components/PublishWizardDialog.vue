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
              <el-form-item label="报名开始" prop="registrationStartTime" :error="getDivisionTimeStatus(div).fieldErrors.registrationStartTime">
                <el-date-picker v-model="div.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
              <el-form-item label="报名结束" prop="registrationEndTime" :error="getDivisionTimeStatus(div).fieldErrors.registrationEndTime">
                <el-date-picker v-model="div.registrationEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
              <el-form-item label="比赛开始" prop="matchStartTime" :error="getDivisionTimeStatus(div).fieldErrors.matchStartTime">
                <el-date-picker v-model="div.matchStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
              <el-form-item label="比赛结束" prop="matchEndTime" :error="getDivisionTimeStatus(div).fieldErrors.matchEndTime">
                <el-date-picker v-model="div.matchEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- Step 4: 赛制规则 -->
      <div v-show="currentStep === 3" class="form-main form-main--wide">
        <el-tabs v-model="rulesTabKey" type="card" @tab-change="handleRulesTabChange">
          <el-tab-pane v-for="(div, index) in divisionDrafts" :key="div.localKey" :name="div.localKey">
            <template #label>
              <el-tooltip :content="formatV4DivisionTabLabel(div, index)" placement="top" :show-after="400">
                <span class="tab-label-text">{{ formatV4DivisionTabLabel(div, index) }}</span>
              </el-tooltip>
            </template>
            <el-form label-width="120px" class="mb8">
              <el-form-item label="赛制模板">
                <el-select v-model="div.formatTemplateId" style="width: 100%" placeholder="请选择赛制模板" @change="() => handleTemplateChange(div)">
                  <el-option v-for="item in enabledFormatTemplates" :key="item.id" :label="item.templateName" :value="item.id" />
                </el-select>
                <div class="el-form-item__tip">当前仅支持单循环、小组赛（分组循环）和 BO1 单败淘汰；开放报名前须手动锁定规则快照。</div>
              </el-form-item>
            </el-form>

            <el-alert
              v-if="div.id"
              :type="getDivisionReadiness(div).openRegistrationReady ? 'success' : 'warning'"
              :closable="false"
              show-icon
              class="mb8"
            >
              <template #title>赛区就绪状态</template>
              <ul class="readiness-list">
                <li>基础配置：{{ getDivisionReadiness(div).basicComplete ? '完整' : '未完成' }}</li>
                <li>时间配置：{{ getDivisionReadiness(div).timeValid ? '合法' : '未完成或不合法' }}</li>
                <li>规则快照：{{ v4SnapshotStatusLabel(snapshotMap[div.id!]?.snapshotStatus) }}</li>
                <li>阶段：{{ sortedStages(div).length ? `${sortedStages(div).length} 个阶段` : '无有效阶段' }}</li>
                <li>开放报名：{{ getDivisionReadiness(div).openRegistrationReady ? '满足条件' : '未满足' }}</li>
              </ul>
              <div v-if="getDivisionReadiness(div).reasons.length" class="readiness-reasons">
                {{ getDivisionReadiness(div).reasons.join('；') }}
              </div>
            </el-alert>

            <div v-if="div.id" class="rules-actions mb8">
              <el-button type="warning" plain :loading="rebuildingKey === div.localKey" :disabled="!!actionLoading" @click="handleRebuildSnapshot(div)" v-if="currentSnapshot(div)?.mutable">重建快照</el-button>
              <el-button type="success" plain :loading="lockingKey === div.localKey" :disabled="!!actionLoading" @click="handleLockSnapshot(div)" v-if="currentSnapshot(div)?.lockable">锁定规则</el-button>
              <span v-if="!currentSnapshot(div)?.mutable && currentSnapshot(div)?.snapshotStatus === 'DRAFT'" class="el-form-item__tip">已有正式业务数据或快照非草稿，不可重建</span>
            </div>

            <el-alert v-if="currentSnapshot(div) && !currentSnapshot(div)?.mutable" type="warning" :closable="false" show-icon class="mb8">
              <template #title>规则已锁定，后续报名和比赛将使用此快照。</template>
            </el-alert>

            <el-descriptions v-if="currentSnapshot(div)" :column="2" border size="small" class="mb8">
              <el-descriptions-item label="模板">{{ currentSnapshot(div)?.sourceTemplateName }} v{{ currentSnapshot(div)?.sourceTemplateVersion }}</el-descriptions-item>
              <el-descriptions-item label="快照状态">
                <el-tag :type="v4SnapshotStatusTagType(currentSnapshot(div)?.snapshotStatus)" size="small">
                  {{ v4SnapshotStatusLabel(currentSnapshot(div)?.snapshotStatus) }}
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
                  <el-descriptions-item label="单场局数">{{ stage.bestOf ?? 1 }} 局</el-descriptions-item>
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

        <div class="confirm-section-title">平台赛区状态（{{ divisionDrafts.length }}）</div>
        <el-table :data="divisionDrafts" border size="small" class="mb8">
          <el-table-column label="开放报名" width="88" align="center" fixed="left">
            <template #default="scope">
              <el-tooltip :disabled="getDivisionReadiness(scope.row).openRegistrationReady" :content="getDivisionReadiness(scope.row).reasons.join('；')" placement="top">
                <el-checkbox
                  v-model="openRegistrationSelection[scope.row.localKey]"
                  :disabled="!getDivisionReadiness(scope.row).openRegistrationReady"
                />
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column label="赛区" min-width="120">
            <template #default="scope">
              <div>{{ scope.row.divisionName || '未命名' }}</div>
              <div class="cell-sub">{{ v4PlatformLabel(scope.row.platformCode) }} · {{ v4GameModeLabel(scope.row.gameMode) }}</div>
            </template>
          </el-table-column>
          <el-table-column label="基础配置" width="96" align="center">
            <template #default="scope">
              <el-tag :type="getDivisionReadiness(scope.row).basicComplete ? 'success' : 'warning'" size="small">
                {{ getDivisionReadiness(scope.row).basicComplete ? '完整' : '未完成' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="时间配置" width="96" align="center">
            <template #default="scope">
              <el-tag :type="getDivisionReadiness(scope.row).timeValid ? 'success' : 'warning'" size="small">
                {{ getDivisionReadiness(scope.row).timeValid ? '合法' : '未完成' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="规则快照" width="96" align="center">
            <template #default="scope">
              <el-tag :type="v4SnapshotStatusTagType(snapshotMap[scope.row.id!]?.snapshotStatus)" size="small">
                {{ v4SnapshotStatusLabel(snapshotMap[scope.row.id!]?.snapshotStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="阶段数" width="96" align="center">
            <template #default="scope">
              <el-tag :type="getDivisionReadiness(scope.row).hasValidStage ? 'success' : 'warning'" size="small">
                {{ sortedStages(scope.row).length ? `${sortedStages(scope.row).length} 个` : '无' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="开放报名条件" width="110" align="center">
            <template #default="scope">
              <el-tag :type="getDivisionReadiness(scope.row).openRegistrationReady ? 'success' : 'info'" size="small">
                {{ getDivisionReadiness(scope.row).openRegistrationReady ? '满足' : '未满足' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="未就绪原因" min-width="220">
            <template #default="scope">
              <span v-if="getDivisionReadiness(scope.row).openRegistrationReady" class="text-muted">-</span>
              <span v-else class="reason-text">{{ getDivisionReadiness(scope.row).reasons.join('；') }}</span>
            </template>
          </el-table-column>
        </el-table>

        <el-alert v-if="publishAndOpenDisabledReason" type="warning" :closable="false" show-icon class="mb8" :title="publishAndOpenDisabledReason" />

        <el-alert type="info" :closable="false" show-icon>
          <template #title>发布说明</template>
          <ul class="confirm-tip-list">
            <li>保存草稿：保留当前赛季与平台赛区配置，不发布前台。</li>
            <li>发布赛季（暂不开放报名）：仅发布赛季前台展示，不会自动开放任何平台赛区报名。</li>
            <li>发布并开放报名：发布赛季后，仅对勾选且配置完整、规则快照已锁定的赛区开放报名。</li>
          </ul>
        </el-alert>
      </div>
      </div>
    </div>

    <template #footer>
      <div class="wizard-footer">
        <div class="wizard-footer-left">
          <el-button @click="handleSaveDraft" :loading="saving" :disabled="!!actionLoading">保存草稿</el-button>
        </div>
        <div class="wizard-footer-right">
          <el-button v-if="currentStep === 0" @click="handleCancel">取消</el-button>
          <el-button v-if="currentStep > 0" @click="goPrev" :disabled="saving || !!actionLoading">上一步</el-button>
          <el-button v-if="currentStep < 4" type="primary" @click="goNext" :loading="saving" :disabled="!!actionLoading">下一步</el-button>
          <template v-if="currentStep === 4">
            <el-button type="primary" @click="handlePublishSeason" :loading="saving" :disabled="!!actionLoading" v-hasPermi="['basket:v4Season:publish']">发布赛季（暂不开放报名）</el-button>
            <el-button
              type="success"
              @click="handlePublishAndOpen"
              :loading="saving"
              :disabled="!canPublishAndOpen || !!actionLoading"
              v-hasPermi="['basket:v4Division:openRegistration']"
            >发布并开放报名</el-button>
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
        <el-form-item label="单场局数">
          <el-input-number v-model="stageForm.bestOf" :min="1" :max="1" disabled controls-position="right" style="width: 100%" />
          <div class="el-form-item__tip">当前版本仅支持 BO1，BO3/BO5 尚未开放</div>
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
  V4_TIE_BREAK_RULE_OPTIONS,
  V4_WIZARD_ADVANCEMENT_TYPE_OPTIONS,
  V4_WIZARD_COMPETITION_FORMAT_OPTIONS,
  assessV4DivisionReadiness,
  buildV4DivisionDefaultName,
  buildV4SeasonDefaultDisplayTitle,
  buildV4SeasonDefaultSeasonName,
  formatV4CompactRange,
  formatV4DivisionComboLabel,
  formatV4DivisionTabLabel,
  isV4WizardTemplateAllowed,
  validateV4DivisionTimeRelations,
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
  v4TieBreakRuleLabel,
  V4_GROUPING_MODE_OPTIONS,
  V4_STAGE_TYPE_OPTIONS
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

const STAGE_TYPE_OPTIONS = V4_STAGE_TYPE_OPTIONS
const COMPETITION_FORMAT_OPTIONS = V4_WIZARD_COMPETITION_FORMAT_OPTIONS
const GROUPING_MODE_OPTIONS = V4_GROUPING_MODE_OPTIONS
const ADVANCEMENT_TYPE_OPTIONS = V4_WIZARD_ADVANCEMENT_TYPE_OPTIONS
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
const actionLoading = ref(false)
const lockingKey = ref('')
const rebuildingKey = ref('')
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
  && selectedOpenDivisions.value.every((div) => getDivisionReadiness(div).openRegistrationReady)
)

const publishAndOpenDisabledReason = computed(() => {
  if (canPublishAndOpen.value) return ''
  if (!seasonForm.seasonName?.trim() || !seasonForm.displayTitle?.trim()) {
    return '请先完善赛季基础信息'
  }
  if (!divisionDrafts.value.length) return '请至少添加一个平台赛区'
  const draftSelected = selectedOpenDivisions.value.some((div) => getDivisionReadiness(div).snapshotDraft)
  if (draftSelected) return '已勾选赛区中存在草稿状态规则快照，请先锁定规则后再开放报名'
  const hasReady = divisionDrafts.value.some((div) => getDivisionReadiness(div).openRegistrationReady)
  if (!hasReady) return '请选择至少一个配置完整且规则已锁定的赛区'
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

function getDivisionTimeStatus(div: WizardDivisionDraft) {
  return validateV4DivisionTimeRelations(div, seasonForm.seasonStartTime, seasonForm.seasonEndTime)
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
  for (const div of divisionDrafts.value) {
    const status = getDivisionTimeStatus(div)
    if (status.status !== 'valid') {
      if (!firstInvalidKey) firstInvalidKey = div.localKey
    }
  }
  if (firstInvalidKey) {
    timeTabKey.value = firstInvalidKey
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

function syncOpenRegistrationSelection() {
  for (const div of divisionDrafts.value) {
    if (!(div.localKey in openRegistrationSelection)) {
      openRegistrationSelection[div.localKey] = getDivisionReadiness(div).openRegistrationReady
    } else if (!getDivisionReadiness(div).openRegistrationReady) {
      openRegistrationSelection[div.localKey] = false
    }
  }
}

function isDivisionReady(div: WizardDivisionDraft) {
  return getDivisionReadiness(div).openRegistrationReady
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
        timeTabKey.value = div.localKey
        return false
      }
    }
  }
  if (!validateAllDivisionTimes()) return false
  for (const div of divisionDrafts.value) {
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
      const notReadyForRules = divisionDrafts.value.filter((d) => !d.id || !snapshotMap[d.id!]?.snapshotStatus || !sortedStages(d).length)
      if (notReadyForRules.length) {
        proxy!.$modal.msgError('每个平台赛区至少需要生成规则快照并包含一个阶段，请先完善赛制规则')
        return
      }
      const notReady = divisionDrafts.value.filter((d) => d.id && !getDivisionReadiness(d).openRegistrationReady)
      if (notReady.length) {
        const detail = notReady.map((d) => `「${d.divisionName}」：${getDivisionReadiness(d).reasons.join('；')}`).join('\n')
        proxy!.$modal.msgWarning(`部分赛区尚未满足开放报名条件（仍可继续确认发布）：\n${detail}`)
      }
      syncOpenRegistrationSelection()
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

async function handlePublishSeason() {
  if (saving.value || actionLoading.value) return
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
  if (!canPublishAndOpen.value || saving.value || actionLoading.value) {
    proxy!.$modal.msgError(publishAndOpenDisabledReason.value || '请先勾选满足开放报名条件的赛区')
    return
  }
  const targets = selectedOpenDivisions.value.filter((div) => div.id && getDivisionReadiness(div).openRegistrationReady)
  const lines = targets.map((div) => `- ${formatV4DivisionComboLabel(div.platformCode, div.gameMode, div.registrationMode)}`).join('\n')
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
  if (div?.id) await loadSnapshot(div)
}

async function handleTemplateChange(div: WizardDivisionDraft) {
  if (!div.id || !div.formatTemplateId || actionLoading.value) return
  actionLoading.value = true
  rebuildingKey.value = div.localKey
  try {
    dirty.value = true
    await rebuildFormatSnapshot(div.id, div.formatTemplateId)
    await loadSnapshot(div)
    proxy!.$modal.msgSuccess('已切换模板并重建快照')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '切换模板失败')
  } finally {
    actionLoading.value = false
    rebuildingKey.value = ''
  }
}

async function handleRebuildSnapshot(div: WizardDivisionDraft) {
  if (!div.id || !div.formatTemplateId || actionLoading.value) return
  try {
    await proxy!.$modal.confirm('重建将保留历史快照版本，当前草稿规则将被替换。是否继续？')
  } catch {
    return
  }
  actionLoading.value = true
  rebuildingKey.value = div.localKey
  try {
    await rebuildFormatSnapshot(div.id, div.formatTemplateId)
    await loadSnapshot(div)
    proxy!.$modal.msgSuccess('重建成功')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '重建失败')
  } finally {
    actionLoading.value = false
    rebuildingKey.value = ''
  }
}

async function handleLockSnapshot(div: WizardDivisionDraft) {
  if (!div.id || actionLoading.value) return
  if (!sortedStages(div).length) {
    proxy!.$modal.msgError('锁定前至少需要配置一个有效阶段')
    return
  }
  try {
    await proxy!.$modal.confirm('锁定后规则不可再编辑，开放报名前须锁定。是否继续？')
  } catch {
    return
  }
  actionLoading.value = true
  lockingKey.value = div.localKey
  try {
    await lockFormatSnapshot(div.id)
    await loadSnapshot(div)
    proxy!.$modal.msgSuccess('规则已锁定，后续报名和比赛将使用此快照。')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '锁定失败，请检查规则配置是否完整')
  } finally {
    actionLoading.value = false
    lockingKey.value = ''
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
  if (!div?.id || actionLoading.value) return
  actionLoading.value = true
  try {
    await stageFormRef.value?.validate()
    stageForm.bestOf = 1
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
  } catch (e: any) {
    if (e?.msg || e?.message) proxy!.$modal.msgError(e?.msg || e?.message)
  } finally {
    actionLoading.value = false
  }
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
