<template>
  <div v-loading="panelLoading" class="format-rules-panel">
    <el-alert
      v-if="snapshot && !snapshot.mutable"
      type="warning"
      :closable="false"
      show-icon
      class="mb8"
    >
      <template #title>赛事规则已发布，当前版本不可修改。</template>
    </el-alert>

    <div v-if="snapshot" class="current-scheme-bar mb8">
      <span>{{ currentSchemeSummary }}</span>
      <el-button link type="primary" @click="handleRulesSectionTabChange('advanced')">查看技术信息</el-button>
    </div>

    <el-tabs :model-value="rulesSectionTab" type="border-card" class="rules-section-tabs mb8" @tab-change="handleRulesSectionTabChange">
      <el-tab-pane label="标准模板" name="standard">
        <template v-if="rulesSectionTab === 'standard'">
          <StandardTemplateConfigPanel
            :division-id="divisionId"
            :series-id="seriesId"
            :series-code="seriesCode"
            :series-name="seriesName"
            :mutable="!!snapshot?.mutable"
            :game-mode="divisionMeta?.gameMode"
            :registration-mode="snapshot?.registrationMode || divisionMeta?.registrationMode"
            @applied="handleStandardTemplateApplied"
          />
        </template>
      </el-tab-pane>
      <el-tab-pane label="快速创建" name="quick">
        <template v-if="rulesSectionTab === 'quick'">
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
              <div v-if="selectedScheme === item.id" class="scheme-card-check">✓ 已选择</div>
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
              <div v-if="selectedScheme === item.id" class="scheme-card-check">✓ 已选择</div>
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
              <template v-if="selectedScheme === 'PLAYOFF'">
                <el-form-item label="参赛组织">
                  <span>不分组</span>
                </el-form-item>
                <el-form-item label="种子来源">
                  <el-select v-model="quickParams.seedSource" style="width: 100%" @change="emitChange">
                    <el-option
                      v-for="opt in V4_PLAYOFF_SEED_SOURCE_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                      :disabled="opt.value === 'STANDING'"
                    />
                  </el-select>
                  <div class="el-form-item__tip mt4">{{ playoffSeedTip }}</div>
                </el-form-item>
                <el-form-item label="首轮对阵">
                  <span>固定种子对阵（高位对低位象限）</span>
                </el-form-item>
                <el-form-item label="轮次 BO">
                  <div class="playoff-bo-list">
                    <div v-for="(size, idx) in playoffRoundSizes" :key="size" class="playoff-bo-row">
                      <span class="playoff-bo-name">{{ formatV4PlayoffStageName(size) }}</span>
                      <el-select
                        :model-value="(quickParams.playoffRoundBestOf || [])[idx] || 1"
                        style="width: 120px"
                        @change="(val) => updatePlayoffBestOf(idx, val)"
                      >
                        <el-option v-for="opt in V4_PLAYOFF_BEST_OF_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                      </el-select>
                    </div>
                  </div>
                </el-form-item>
              </template>
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
                  <el-option
                    v-for="opt in V4_ELIMINATION_PAIRING_RULE_OPTIONS"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="applyingScheme" :disabled="actionBusy || !canApplyScheme" @click="applyQuickCreateScheme">应用赛制方案</el-button>
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
              <el-descriptions-item v-if="quickEstimate.eliminationMatches" label="淘汰赛预计系列数">{{ quickEstimate.eliminationMatches }} 场（N-1）</el-descriptions-item>
              <el-descriptions-item v-if="quickEstimate.totalMatches" label="总预计场次" :span="2">{{ quickEstimate.totalMatches }} 场（最终以实际审核队伍数为准）</el-descriptions-item>
            </el-descriptions>
            <div v-if="selectedScheme === 'PLAYOFF' && playoffRoundSizes.length" class="playoff-bracket-preview mt8">
              <div class="scheme-section-title">季后赛对阵预览</div>
              <PlayoffBracketPreview
                :team-count="quickParams.expectedTeamCount || maxTeamCount"
                :best-of-list="quickParams.playoffRoundBestOf"
                :round-names="playoffRoundSizes.map((size) => formatV4PlayoffStageName(size))"
                :seed-source="quickParams.seedSource || 'TEAM_CREATE_TIME'"
              />
            </div>
          </el-card>
        </template>
      </el-tab-pane>

      <el-tab-pane label="自定义赛制" name="custom">
        <el-alert
          v-if="isStandardSnapshot"
          type="info"
          :closable="false"
          show-icon
          class="mb8"
          title="当前规则来自 STANDARD 模板，核心阶段结构不可删改。如需自由组合阶段，请改用自定义赛制。"
        />
        <el-alert
          v-for="(msg, vi) in stageValidation.messages"
          :key="'err-' + vi"
          type="error"
          :closable="false"
          show-icon
          class="mb8"
          :title="msg"
        />
        <el-alert
          v-for="(msg, wi) in stageValidation.warnings"
          :key="'warn-' + wi"
          type="warning"
          :closable="false"
          show-icon
          class="mb8"
          :title="msg"
        />

        <div v-if="sortedStages.length && rulesSectionTab === 'custom'" class="stage-flow-list">
          <template v-for="(stage, sIndex) in sortedStages" :key="stage.id || stage.stageCode">
            <template v-if="isPlayoffBlockStart(sIndex)">
              <el-card shadow="hover" class="stage-config-card playoff-config-card">
                <div class="stage-flow-card-head">
                  <div class="stage-flow-title-wrap">
                    <span class="stage-flow-order">季后赛</span>
                    <el-tag type="danger" size="small">{{ playoffBlock?.teamCount }} 支 → 冠军</el-tag>
                  </div>
                </div>
                <el-form label-width="120px" class="stage-inline-form" :disabled="!snapshot?.mutable">
                  <el-form-item label="竞赛方式">
                    <el-radio-group model-value="PLAYOFF" @change="(val: string) => collapsePlayoffBlock(val)">
                      <el-radio v-for="opt in V4_CUSTOM_COMPETITION_FORMAT_OPTIONS" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="参赛组织">
                    <span class="readonly-info-value">不分组</span>
                  </el-form-item>
                  <el-form-item label="参赛来源">
                    <span class="readonly-info-value">{{ playoffParticipantSourceLabel }}</span>
                  </el-form-item>
                  <el-form-item label="季后赛参赛规模">
                    <el-select
                      v-if="playoffSizeEditable"
                      :model-value="playoffBlock?.teamCount"
                      style="width: 180px"
                      :disabled="!snapshot?.mutable"
                      @change="handlePlayoffTeamCountChange"
                    >
                      <el-option v-for="n in PLAYOFF_UI_TEAM_COUNTS" :key="n" :label="`${n} 支`" :value="n" />
                    </el-select>
                    <span v-else class="readonly-info-value">{{ playoffBlock?.teamCount }} 支</span>
                    <div class="el-form-item__tip mt4">{{ playoffSizeTip }}</div>
                  </el-form-item>
                  <el-form-item label="种子来源">
                    <el-select :model-value="playoffSeedSource" style="width: 100%" @change="handlePlayoffSeedSourceChange">
                      <el-option
                        v-for="opt in V4_PLAYOFF_SEED_SOURCE_OPTIONS"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                        :disabled="opt.value === 'STANDING' && !playoffStandingStages.length"
                      />
                    </el-select>
                    <div class="el-form-item__tip mt4">{{ playoffCustomSeedTip }}</div>
                  </el-form-item>
                  <el-form-item v-if="playoffSeedSource === 'STANDING' && playoffStandingStages.length" label="积分来源阶段">
                    <el-select :model-value="playoffSourceStageCode" disabled style="width: 100%">
                      <el-option v-for="s in playoffStandingStages" :key="s.stageCode" :label="s.stageName" :value="s.stageCode" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="首轮对阵">
                    <span class="readonly-info-value">固定种子对阵</span>
                  </el-form-item>
                  <el-form-item label="后续阶段">
                    <span class="readonly-info-value">固定路径晋级</span>
                  </el-form-item>
                  <el-form-item label="阶段配置">
                    <div class="playoff-bo-list">
                      <div v-for="(pStage, idx) in (playoffBlock?.stages || [])" :key="pStage.id || pStage.stageCode" class="playoff-stage-row">
                        <el-input
                          v-if="snapshot?.mutable"
                          v-model="pStage.stageName"
                          class="playoff-stage-name"
                          @change="() => saveStageInline(pStage)"
                        />
                        <span v-else>{{ pStage.stageName }}</span>
                        <span class="playoff-stage-size">{{ playoffRoundSize(idx) }} → {{ playoffRoundSize(idx) / 2 }}</span>
                        <el-select :model-value="pStage.bestOf ?? 1" style="width: 110px" @change="(val: number) => handleBestOfChange(pStage, val)">
                          <el-option v-for="opt in V4_PLAYOFF_BEST_OF_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                        </el-select>
                      </div>
                    </div>
                  </el-form-item>
                </el-form>
              </el-card>
            </template>
            <template v-else-if="isInsidePlayoffBlock(sIndex)"></template>
            <template v-else>
            <el-card shadow="hover" class="stage-config-card">
              <div class="stage-flow-card-head">
                <div class="stage-flow-title-wrap">
                  <span class="stage-flow-order">阶段 {{ stage.stageOrder }}</span>
                  <el-input
                    v-if="snapshot?.mutable"
                    v-model="stage.stageName"
                    class="stage-name-input"
                    placeholder="阶段名称"
                    @change="() => saveStageInline(stage)"
                  />
                  <span v-else class="stage-flow-title">{{ stage.stageName }}</span>
                  <el-tag v-if="isFinalStageIndex(sIndex)" type="danger" size="small" class="ml8">最终阶段</el-tag>
                </div>
                <div v-if="snapshot?.mutable && !isStandardSnapshot" class="stage-card-actions">
                  <el-button link :disabled="sortedStages.length <= 1" type="danger" @click="handleDeleteStage(stage)">删除</el-button>
                  <el-button link :disabled="sIndex === 0" @click="moveStage(stage, -1)">上移</el-button>
                  <el-button link :disabled="sIndex === sortedStages.length - 1" @click="moveStage(stage, 1)">下移</el-button>
                  <el-button
                    v-if="stage.competitionFormat === 'ROUND_ROBIN'"
                    link
                    type="primary"
                    @click="openTieBreakEditor(stage)"
                  >
                    同分规则
                  </el-button>
                </div>
              </div>

              <p class="stage-card-summary">{{ formatV4StageCardSummary(stage, sortedStages) }}</p>

              <el-form label-width="120px" class="stage-inline-form" :disabled="!snapshot?.mutable">
                <el-form-item label="参赛来源">
                  <span class="readonly-info-value">{{ formatV4StageParticipantSource(stage, sortedStages) }}</span>
                </el-form-item>
                <el-form-item label="参赛组织方式">
                  <el-radio-group
                    :model-value="resolveStageOrganizationMode(stage)"
                    @change="(val: string) => handleOrganizationModeChange(stage, val)"
                  >
                    <el-radio v-for="opt in V4_ORGANIZATION_MODE_OPTIONS" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="竞赛方式">
                  <el-radio-group
                    :model-value="stage.competitionFormat"
                    @change="(val: string) => handleCompetitionFormatChange(stage, val)"
                  >
                    <el-radio
                      v-for="opt in V4_CUSTOM_COMPETITION_FORMAT_OPTIONS"
                      :key="opt.value"
                      :label="opt.value"
                      :disabled="(opt.value === 'SINGLE_ELIMINATION' || opt.value === 'PLAYOFF') && resolveStageOrganizationMode(stage) === 'GROUPED'"
                    >
                      {{ opt.label }}
                    </el-radio>
                  </el-radio-group>
                  <DoubleEliminationDemoPanel v-if="stage.competitionFormat === 'DOUBLE_ELIMINATION' || showDoubleElimDemo" class="mt8" />
                </el-form-item>
                <el-form-item label="BO 规则">
                  <el-radio-group
                    :model-value="stage.bestOf ?? 1"
                    @change="(val: number) => handleBestOfChange(stage, val)"
                  >
                    <el-radio
                      v-for="opt in resolveStageBestOfOptions(stage)"
                      :key="opt.value"
                      :label="opt.value"
                    >
                      {{ opt.label }}
                    </el-radio>
                  </el-radio-group>
                </el-form-item>

                <template v-if="resolveStageOrganizationMode(stage) === 'GROUPED'">
                  <el-form-item label="分组数量">
                    <el-input-number
                      :model-value="stage.groupCount"
                      :min="2"
                      :max="maxTeamCount"
                      controls-position="right"
                      @change="(val: number) => patchStageField(stage, 'groupCount', val)"
                    />
                  </el-form-item>
                  <el-form-item label="分组方式">
                    <el-select
                      :model-value="stage.groupingMode"
                      style="width: 100%"
                      @change="(val: string) => patchStageField(stage, 'groupingMode', val)"
                    >
                      <el-option v-for="opt in V4_GROUPING_MODE_GROUPED_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                    </el-select>
                  </el-form-item>
                </template>

                <template v-if="stage.competitionFormat === 'ROUND_ROBIN'">
                  <el-form-item label="循环次数">
                    <el-input-number
                      :model-value="stage.roundRobinCycles ?? 1"
                      :min="1"
                      controls-position="right"
                      @change="(val: number) => patchStageField(stage, 'roundRobinCycles', val)"
                    />
                    <span class="el-form-item__tip ml8">{{ (stage.roundRobinCycles ?? 1) > 1 ? '双循环' : '单循环' }}</span>
                  </el-form-item>
                  <el-form-item label="胜/负积分">
                    <el-col :span="11">
                      <el-input-number
                        :model-value="stage.winPoints ?? 2"
                        :min="0"
                        controls-position="right"
                        style="width: 100%"
                        @change="(val: number) => patchStageField(stage, 'winPoints', val)"
                      />
                    </el-col>
                    <el-col :span="2" class="text-center">/</el-col>
                    <el-col :span="11">
                      <el-input-number
                        :model-value="stage.lossPoints ?? 1"
                        :min="0"
                        controls-position="right"
                        style="width: 100%"
                        @change="(val: number) => patchStageField(stage, 'lossPoints', val)"
                      />
                    </el-col>
                  </el-form-item>
                </template>

                <template v-if="stage.competitionFormat === 'SINGLE_ELIMINATION'">
                  <el-form-item label="对阵规则">
                    <template v-if="isPlayoffBracketInheritStage(stage, sIndex)">
                      <span class="readonly-info-value">继承淘汰树</span>
                      <div class="el-form-item__tip mt4">后续淘汰阶段严格继承上一轮 Series Winner 路径，不再重新抽签或按排名重排。</div>
                    </template>
                    <el-select
                      v-else
                      :model-value="normalizeV4EliminationPairingRule(stage.pairingRule)"
                      style="width: 100%"
                      @change="(val: string) => patchStageField(stage, 'pairingRule', val)"
                    >
                      <el-option
                        v-for="opt in V4_CUSTOM_ELIMINATION_PAIRING_OPTIONS"
                        :key="opt.value"
                        :label="opt.label"
                        :value="opt.value"
                      />
                    </el-select>
                  </el-form-item>
                </template>

                <el-form-item v-if="!isFinalStageIndex(sIndex)" label="晋级规则">
                  <template v-if="stage.competitionFormat === 'SINGLE_ELIMINATION'">
                    <span class="readonly-info-value">胜者晋级</span>
                  </template>
                  <template v-else-if="resolveStageOrganizationMode(stage) === 'GROUPED'">
                    <span>每组前</span>
                    <el-input-number
                      :model-value="stage.advancementCount"
                      :min="1"
                      controls-position="right"
                      class="ml8 mr8"
                      @change="(val: number) => patchStageField(stage, 'advancementCount', val)"
                    />
                    <span>名晋级</span>
                    <span v-if="stage.groupCount && stage.advancementCount" class="el-form-item__tip ml8">
                      预计晋级 {{ stage.groupCount * stage.advancementCount }} 支
                    </span>
                  </template>
                  <template v-else>
                    <span>总排名前</span>
                    <el-input-number
                      :model-value="stage.advancementCount"
                      :min="1"
                      :max="maxTeamCount"
                      controls-position="right"
                      class="ml8 mr8"
                      @change="(val: number) => patchStageField(stage, 'advancementCount', val)"
                    />
                    <span>名晋级</span>
                  </template>
                </el-form-item>
                <el-form-item v-else label="阶段结果">
                  <span class="readonly-info-value">本阶段结束后决出最终结果，不再晋级下一阶段。</span>
                </el-form-item>
              </el-form>
            </el-card>
            <div v-if="sIndex < sortedStages.length - 1" class="stage-flow-connector">
              <span class="stage-flow-arrow-icon">↓</span>
              <span v-if="formatV4StageAdvanceCountLabel(stage)" class="stage-flow-advance">{{ formatV4StageAdvanceCountLabel(stage) }}</span>
            </div>
            </template>
          </template>
        </div>
        <el-empty v-else-if="rulesSectionTab === 'custom'" description="暂无比赛阶段，请添加阶段 1" />

        <div v-if="snapshot?.mutable && rulesSectionTab === 'custom' && !playoffBlock" class="mt8">
          <el-button type="primary" plain icon="Plus" v-hasPermi="['basket:v4FormatRule:edit']" @click="handleAddNextStage">+ 添加下一比赛阶段</el-button>
        </div>

        <el-card v-if="sortedStages.length" shadow="never" class="preview-card mt8">
          <template #header>完整赛制预览</template>
          <div class="custom-preview-chain">
            <template v-for="(item, pi) in customFormatPreview" :key="pi">
              <div class="custom-preview-step">
                <div class="custom-preview-title">{{ item.title }}</div>
                <div class="custom-preview-summary">{{ item.summary }}</div>
                <div v-if="item.advance" class="custom-preview-advance">{{ item.advance }}</div>
                <el-tag v-if="item.isFinal" type="danger" size="small">最终阶段</el-tag>
              </div>
              <div v-if="pi < customFormatPreview.length - 1" class="custom-preview-arrow">↓</div>
            </template>
          </div>
          <el-descriptions :column="2" border size="small" class="mt8">
            <el-descriptions-item v-if="stageEstimate.groupCount" label="小组数量">{{ stageEstimate.groupCount }} 组</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.teamsPerGroup" label="每组预计队伍">{{ stageEstimate.teamsPerGroup }} 支</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.groupStageMatches" label="小组赛预计场次">{{ stageEstimate.groupStageMatches }} 场</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.advanceTotal" label="晋级队伍数">{{ stageEstimate.advanceTotal }} 支</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.eliminationStartRound" label="淘汰赛起始轮次">{{ stageEstimate.eliminationStartRound }}</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.eliminationMatches" label="淘汰赛预计系列数">{{ stageEstimate.eliminationMatches }} 场（N-1）</el-descriptions-item>
            <el-descriptions-item v-if="stageEstimate.totalMatches" label="总预计场次" :span="2">{{ stageEstimate.totalMatches }} 场（最终以实际审核队伍数为准）</el-descriptions-item>
          </el-descriptions>
          <div v-if="playoffBlock" class="mt8">
            <div class="scheme-section-title">季后赛对阵预览</div>
            <PlayoffBracketPreview
              :team-count="playoffBlock.teamCount"
              :best-of-list="playoffBestOfList"
              :round-names="playoffRoundNames"
              :seed-source="playoffSeedSource"
            />
          </div>
        </el-card>

      </el-tab-pane>

      <el-tab-pane label="高级设置" name="advanced">
        <el-collapse>
          <el-collapse-item title="技术信息" name="tech">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="模板类型">{{ v4TemplateTypeLabel(snapshot?.sourceTemplateType) }}（{{ snapshot?.sourceTemplateType || '-' }}）</el-descriptions-item>
              <el-descriptions-item label="Template Code">{{ snapshot?.sourceTemplateCode || '-' }}</el-descriptions-item>
              <el-descriptions-item label="报名方式">{{ v4RegistrationModeLabel(snapshot?.registrationMode) }}（{{ snapshot?.registrationMode || '-' }}）</el-descriptions-item>
              <el-descriptions-item label="比赛模式">{{ v4GameModeLabel(divisionMeta?.gameMode) }}（{{ divisionMeta?.gameMode || '-' }}）</el-descriptions-item>
            </el-descriptions>
          </el-collapse-item>
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
            <el-option
              v-for="opt in V4_ELIMINATION_PAIRING_RULE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
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
import PlayoffBracketPreview from './PlayoffBracketPreview.vue'
import StandardTemplateConfigPanel from './StandardTemplateConfigPanel.vue'
import DoubleEliminationDemoPanel from '../../components/DoubleEliminationDemoPanel.vue'
import {
  addFormatStage,
  applyFormatPlan,
  deleteFormatStage,
  getFormatSnapshot,
  listV4FormatTemplates,
  rebuildFormatSnapshot,
  reorderFormatStages,
  saveTieBreakRules,
  updateFormatStage,
  type BasketDivisionFormatSnapshotVo,
  type BasketDivisionFormatPlanApplyReq,
  type BasketDivisionStageSaveReq,
  type BasketDivisionStageVo,
  type BasketFormatTemplateListVo
} from '@/api/cnpa/event-v4/format-template'
import {
  V4_FORMAT_SCHEME_OPTIONS,
  V4_ORGANIZATION_MODE_OPTIONS,
  V4_CUSTOM_COMPETITION_FORMAT_OPTIONS,
  V4_CUSTOM_ELIMINATION_PAIRING_OPTIONS,
  V4_BEST_OF_OPTIONS,
  V4_ELIMINATION_PAIRING_RULE_OPTIONS,
  V4_PLAYOFF_SEED_SOURCE_OPTIONS,
  V4_PLAYOFF_BEST_OF_OPTIONS,
  V4_GROUPING_MODE_GROUPED_OPTIONS,
  V4_WIZARD_STAGE_PRESET_OPTIONS,
  V4_WIZARD_TIE_BREAK_DEFAULTS,
  V4_WIZARD_GROUPING_MODE_OPTIONS,
  V4_WIZARD_TIE_BREAK_MIDDLE_OPTIONS,
  buildDefaultV4QuickCreateParams,
  buildV4PlayoffRoundSizes,
  buildV4SchemeStagePayloads,
  formatV4PlayoffStageName,
  buildV4WizardStagePresetForm,
  calcV4AdvancementTotal,
  calcV4FormatEstimateFromQuickParams,
  calcV4FormatEstimateFromStages,
  createDefaultV4CustomStage,
  formatV4CustomFormatPreview,
  formatV4BestOfLabel,
  formatV4QuickCreateNaturalLanguage,
  formatV4StageAdvanceCountLabel,
  formatV4StageAdvancementArrangement,
  formatV4StageCardSummary,
  formatV4StageCompetitionFormLabel,
  formatV4StageParticipantSource,
  formatV4StagePointsRule,
  formatV4TieBreakPreview,
  formatV4TieBreakSummaryArrow,
  generateV4StageCode,
  getV4FormatSchemeOption,
  inferV4FormatSchemeFromStages,
  inferV4StageCodePreset,
  isV4FinalStageIndex,
  isV4GroupCompetitionStage,
  isV4WizardTemplateAllowed,
  normalizeV4EliminationPairingRule,
  normalizeV4WizardTieBreakRules,
  rebuildV4StageLinearChain,
  resolveV4StagePairingRuleForSave,
  resolveV4OrganizationMode,
  validateV4QuickCreateParams,
  validateV4StageCombinationDetailed,
  v4CurrentFormatSchemeSummary,
  v4GameModeLabel,
  v4GroupingModeLabel,
  v4OrganizationModeLabel,
  v4RegistrationModeLabel,
  v4StandardTemplateProductName,
  v4TemplateTypeLabel,
  v4TieBreakRuleLabel,
  v4WizardAdvancementMode,
  v4WizardAdvancementTypeOptions,
  type V4FormatSchemeId,
  type V4QuickCreateParams,
  type V4SchemeStagePayload,
  type V4StageSummaryInput,
  type V4WizardStagePreset
} from '@/utils/v4AdminDisplay'
import {
  buildV4PlayoffStages,
  findPlayoffBlock,
  inferPlayoffSeedSource,
  isV4PlayoffTeamCount,
  isV4StandingCapableStage,
  mapPlayoffBestOfByRoundSize,
  normalizeV4PlayoffBestOf,
  PLAYOFF_UI_TEAM_COUNTS,
  resolvePlayoffInputTeamCount,
  type V4PlayoffSeedSource
} from '@/utils/v4PlayoffFormat'

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
  seriesCode?: string
  seriesName?: string
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

type RulesSectionTab = 'standard' | 'quick' | 'custom' | 'advanced'

const STAGE_PRESET_OPTIONS = V4_WIZARD_STAGE_PRESET_OPTIONS
const STAGE_SHAPE_OPTIONS = V4_WIZARD_STAGE_PRESET_OPTIONS.map((item) => ({ label: item.label, value: item.value }))
const WIZARD_GROUPING_MODE_OPTIONS = V4_WIZARD_GROUPING_MODE_OPTIONS
const TIE_BREAK_DEFAULTS = V4_WIZARD_TIE_BREAK_DEFAULTS.map((item) => ({ ...item }))
const singleStageSchemes = V4_FORMAT_SCHEME_OPTIONS.filter((s) => s.category === 'single')
const comboStageSchemes = V4_FORMAT_SCHEME_OPTIONS.filter((s) => s.category === 'combo' || s.category === 'custom')

const panelLoading = ref(false)
const showDoubleElimDemo = ref(false)
const actionBusy = ref(false)
const rebuilding = ref(false)
const applyingScheme = ref(false)

const snapshot = ref<BasketDivisionFormatSnapshotVo>()
const formatTemplateList = ref<BasketFormatTemplateListVo[]>([])
const isStandardSnapshot = computed(() =>
  snapshot.value?.sourceTemplateType === 'STANDARD'
  || ['LEAGUE_STANDARD_V1', 'ARENA_STANDARD_V1', 'CUP_STANDARD_V1', 'OPEN_STANDARD_V1'].includes(snapshot.value?.sourceTemplateCode || '')
)

const currentSchemeSummary = computed(() => {
  const inferred = inferV4FormatSchemeFromStages(sortedStages.value)
  const schemeName = inferred && inferred !== 'CUSTOM' ? getV4FormatSchemeOption(inferred)?.name : ''
  return v4CurrentFormatSchemeSummary({
    sourceTemplateType: snapshot.value?.sourceTemplateType,
    sourceTemplateCode: snapshot.value?.sourceTemplateCode,
    registrationMode: snapshot.value?.registrationMode || props.divisionMeta?.registrationMode,
    gameMode: props.divisionMeta?.gameMode,
    schemeName
  })
})

const rulesSectionTab = ref<RulesSectionTab>('standard')
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

const quickValidation = computed(() => {
  if (!selectedScheme.value || selectedScheme.value === 'CUSTOM') return { valid: true, messages: [] as string[] }
  return validateV4QuickCreateParams(selectedScheme.value, quickParams)
})

const stageValidation = computed(() =>
  validateV4StageCombinationDetailed(sortedStages.value, props.maxTeamCount)
)

const customFormatPreview = computed(() =>
  formatV4CustomFormatPreview(sortedStages.value, props.maxTeamCount)
)

const canApplyScheme = computed(() => {
  if (!selectedScheme.value || selectedScheme.value === 'CUSTOM') return false
  return quickValidation.value.valid
})

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

function isFinalStageIndex(index: number): boolean {
  return isV4FinalStageIndex(index, sortedStages.value.length)
}

function resolveStageOrganizationMode(stage: BasketDivisionStageVo): string {
  return resolveV4OrganizationMode(stage)
}

function normalizeStagesForUi() {
  const stages = snapshot.value?.stages
  if (!stages?.length) return
  const sorted = [...stages].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
  for (let i = 0; i < sorted.length; i += 1) {
    const stage = sorted[i]
    if (!stage.organizationMode) {
      stage.organizationMode = resolveV4OrganizationMode(stage)
    }
    stage.pairingRule = resolveV4StagePairingRuleForSave(stage)
    // 连续淘汰后续轮强制继承淘汰树
    if (isPlayoffBracketInheritStage(stage, i) && stage.pairingRule !== 'PLAYOFF_BRACKET') {
      stage.pairingRule = 'PLAYOFF_BRACKET'
    }
  }
}

function buildStageSavePayload(stage: BasketDivisionStageVo): BasketDivisionStageSaveReq {
  const chain = rebuildV4StageLinearChain(sortedStages.value)
  const chained = chain.find((s) => s.stageCode === stage.stageCode) || stage
  const orgMode = resolveV4OrganizationMode(chained)
  const payload: BasketDivisionStageSaveReq = {
    stageCode: chained.stageCode,
    stageName: chained.stageName,
    stageOrder: chained.stageOrder,
    stageType: 'CUSTOM',
    organizationMode: orgMode,
    competitionFormat: chained.competitionFormat,
    groupingMode: orgMode === 'GROUPED' ? (chained.groupingMode || 'MANUAL') : 'NONE',
    groupCount: orgMode === 'GROUPED' ? chained.groupCount : undefined,
    roundRobinCycles: chained.roundRobinCycles,
    bestOf: chained.bestOf ?? 1,
    winPoints: chained.winPoints,
    lossPoints: chained.lossPoints,
    advancementType: chained.advancementType,
    advancementCount: chained.advancementCount,
    pairingRule: resolveV4StagePairingRuleForSave(chained),
    nextStageCode: chained.nextStageCode || undefined,
    requiresManualConfirmation: chained.requiresManualConfirmation ?? 0
  }
  return payload
}

function ensureNonFinalAdvancementCount(stage: { advancementType?: string; advancementCount?: number; competitionFormat?: string; groupingMode?: string; organizationMode?: string }) {
  const adv = (stage.advancementType || '').toUpperCase()
  if (adv === 'OVERALL_TOP_N' && !(stage.advancementCount && stage.advancementCount > 0)) {
    stage.advancementCount = props.maxTeamCount
  }
  if (adv === 'GROUP_TOP_N' && !(stage.advancementCount && stage.advancementCount > 0)) {
    stage.advancementCount = 1
  }
}

async function persistAllStages() {
  const chain = rebuildV4StageLinearChain(sortedStages.value)
  for (const chained of chain) {
    ensureNonFinalAdvancementCount(chained)
    const existing = sortedStages.value.find((s) => s.stageCode === chained.stageCode)
    if (!existing?.id) continue
    const payload = buildStageSavePayload(existing)
    payload.stageOrder = chained.stageOrder
    payload.nextStageCode = chained.nextStageCode
    payload.advancementType = chained.advancementType
    payload.advancementCount = chained.advancementCount
    ensureNonFinalAdvancementCount(payload)
    await updateFormatStage(props.divisionId, existing.id, payload)
  }
  await loadSnapshot()
}

async function saveStageInline(stage: BasketDivisionStageVo) {
  if (!stage.id || actionBusy.value) return
  actionBusy.value = true
  try {
    // 仅 persistAllStages：避免对同一 stage 连续两次相同 PUT 触发 request 防重复提交
    await persistAllStages()
    emitChange()
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '保存失败')
    await loadSnapshot()
  } finally {
    actionBusy.value = false
  }
}

async function patchStageField(stage: BasketDivisionStageVo, field: keyof BasketDivisionStageVo, value: unknown) {
  if (!stage.id) return
  ;(stage as any)[field] = value
  await saveStageInline(stage)
}

async function handleOrganizationModeChange(stage: BasketDivisionStageVo, mode: string) {
  if (resolveStageOrganizationMode(stage) === mode) return
  if (mode === 'GROUPED' && (stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION') {
    proxy!.$modal.msgWarning('当前版本暂不支持分组内淘汰赛')
    return
  }
  stage.organizationMode = mode
  if (mode === 'UNGROUPED') {
    stage.groupingMode = 'NONE'
    stage.groupCount = undefined
  } else {
    stage.groupingMode = stage.groupingMode && stage.groupingMode !== 'NONE' ? stage.groupingMode : 'MANUAL'
    stage.groupCount = stage.groupCount || 4
  }
  await saveStageInline(stage)
}

async function handleCompetitionFormatChange(stage: BasketDivisionStageVo, format: string) {
  if (format === 'DOUBLE_ELIMINATION') {
    showDoubleElimDemo.value = true
    proxy!.$modal.msgWarning('当前版本暂未开放双败淘汰正式执行。')
    return
  }
  showDoubleElimDemo.value = false
  if ((format === 'SINGLE_ELIMINATION' || format === 'PLAYOFF') && resolveStageOrganizationMode(stage) === 'GROUPED') {
    proxy!.$modal.msgWarning('当前版本暂不支持分组内淘汰赛')
    return
  }
  if (format === 'PLAYOFF') {
    await expandPlayoffFromStage(stage)
    return
  }
  stage.competitionFormat = format
  if (format === 'SINGLE_ELIMINATION') {
    stage.pairingRule = normalizeV4EliminationPairingRule(stage.pairingRule === 'PLAYOFF_BRACKET' ? 'HIGH_VS_LOW' : stage.pairingRule)
  } else {
    stage.pairingRule = 'NONE'
  }
  await saveStageInline(stage)
}

async function handleBestOfChange(stage: BasketDivisionStageVo, bestOf: number) {
  stage.bestOf = bestOf
  await saveStageInline(stage)
}

function openTieBreakEditor(stage: BasketDivisionStageVo) {
  if ((stage.competitionFormat || '').toUpperCase() !== 'ROUND_ROBIN') {
    proxy!.$modal.msgWarning('仅循环积分赛阶段可配置同分规则')
    return
  }
  openEditStage(stage)
}

async function handleAddNextStage() {
  if (actionBusy.value) return
  actionBusy.value = true
  try {
    const existingCodes = sortedStages.value.map((s) => s.stageCode || '').filter(Boolean)
    const order = sortedStages.value.length + 1
    const draft = createDefaultV4CustomStage(order, existingCodes)
    const chain = rebuildV4StageLinearChain([
      ...sortedStages.value,
      draft as V4StageSummaryInput
    ])
    const newStage = chain[chain.length - 1]
    const payload: BasketDivisionStageSaveReq = {
      stageCode: newStage.stageCode,
      stageName: newStage.stageName,
      stageOrder: newStage.stageOrder,
      stageType: 'CUSTOM',
      organizationMode: newStage.organizationMode,
      competitionFormat: newStage.competitionFormat,
      groupingMode: newStage.groupingMode,
      roundRobinCycles: newStage.roundRobinCycles,
      bestOf: newStage.bestOf,
      winPoints: newStage.winPoints,
      lossPoints: newStage.lossPoints,
      advancementType: 'NONE',
      pairingRule: newStage.pairingRule,
      requiresManualConfirmation: 0
    }
    const res = await addFormatStage(props.divisionId, payload)
    if (res.data) {
      await saveTieBreakRules(props.divisionId, {
        stageId: res.data,
        rules: TIE_BREAK_DEFAULTS.map((r) => ({ ...r }))
      })
    }
    await persistAllStages()
    emitChange()
    proxy!.$modal.msgSuccess('已添加比赛阶段')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '添加阶段失败')
    await loadSnapshot()
  } finally {
    actionBusy.value = false
  }
}

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

/** 连续淘汰阶段的后续轮：固定继承淘汰树，禁止再选 HIGH_VS_LOW / RANDOM_DRAW */
function isPlayoffBracketInheritStage(stage: { competitionFormat?: string }, sIndex: number): boolean {
  if (sIndex <= 0) return false
  if ((stage.competitionFormat || '').toUpperCase() !== 'SINGLE_ELIMINATION') return false
  const prev = sortedStages.value[sIndex - 1]
  return !!prev && (prev.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION'
}

const playoffRoundSizes = computed(() => {
  if (selectedScheme.value !== 'PLAYOFF') return [] as number[]
  return buildV4PlayoffRoundSizes(quickParams.expectedTeamCount || props.maxTeamCount)
})

const playoffSeedTip = computed(() => {
  const hit = V4_PLAYOFF_SEED_SOURCE_OPTIONS.find((o) => o.value === (quickParams.seedSource || 'TEAM_CREATE_TIME'))
  if ((quickParams.seedSource || '') === 'STANDING') {
    return '当前无可用积分排名阶段，请使用球队创建时间顺序或其他种子方式。'
  }
  return hit?.tip || ''
})

const playoffBlock = computed(() => findPlayoffBlock(sortedStages.value))

const playoffSeedSource = computed<V4PlayoffSeedSource>(() => {
  const block = playoffBlock.value
  if (!block) return 'TEAM_CREATE_TIME'
  return inferPlayoffSeedSource(block.stages[0], block.startIndex === 0)
})

const playoffStandingStages = computed(() => {
  const block = playoffBlock.value
  if (!block) return [] as BasketDivisionStageVo[]
  return sortedStages.value.slice(0, block.startIndex).filter((s) => isV4StandingCapableStage(s))
})

const playoffSourceStageCode = computed(() => playoffStandingStages.value[playoffStandingStages.value.length - 1]?.stageCode || '')

const playoffCustomSeedTip = computed(() => {
  if (playoffSeedSource.value === 'STANDING') {
    if (!playoffStandingStages.value.length) return '当前无可用积分排名阶段。'
    return '按所选前置积分阶段最终排名生成 1～N 号种子。'
  }
  if (playoffBlock.value && playoffBlock.value.startIndex === 0) {
    return '当前无可用积分排名阶段。默认按球队创建时间顺序排种子。'
  }
  return '按球队创建时间顺序生成 1～N 号种子。'
})

const playoffBestOfList = computed(() => (playoffBlock.value?.stages || []).map((s) => s.bestOf ?? 1))

const playoffRoundNames = computed(() => (playoffBlock.value?.stages || []).map((s) => s.stageName || ''))

const playoffParticipantSourceLabel = computed(() => {
  const block = playoffBlock.value
  if (block && block.startIndex > 0) return '上一阶段晋级球队'
  return '本赛区正式参赛队伍'
})

const playoffSizeEditable = computed(() => {
  const block = playoffBlock.value
  return !!block && block.startIndex === 0 && !!snapshot.value?.mutable
})

const playoffSizeTip = computed(() => {
  if (!playoffSizeEditable.value) {
    return '由前置阶段晋级人数自动决定，不可手工改为其它规模。'
  }
  return '优先与赛区预计正式参赛队伍数量联动；草稿态可调整为 4 / 8 / 16 / 32。'
})

function isPlayoffBlockStart(sIndex: number): boolean {
  return !!playoffBlock.value && playoffBlock.value.startIndex === sIndex
}

function isInsidePlayoffBlock(sIndex: number): boolean {
  const block = playoffBlock.value
  if (!block) return false
  return sIndex > block.startIndex && sIndex < block.startIndex + block.stages.length
}

function playoffRoundSize(idx: number): number {
  const n = playoffBlock.value?.teamCount || 0
  return n / (2 ** idx)
}

function stageToSchemePayload(stage: BasketDivisionStageVo): V4SchemeStagePayload {
  const orgMode = resolveV4OrganizationMode(stage)
  return {
    stage: {
      stageCode: stage.stageCode || '',
      stageName: stage.stageName || '',
      stageOrder: stage.stageOrder || 1,
      stageType: stage.stageType || 'CUSTOM',
      organizationMode: orgMode,
      competitionFormat: stage.competitionFormat || 'ROUND_ROBIN',
      groupingMode: orgMode === 'GROUPED' ? (stage.groupingMode || 'MANUAL') : 'NONE',
      groupCount: orgMode === 'GROUPED' ? stage.groupCount : undefined,
      roundRobinCycles: stage.roundRobinCycles,
      bestOf: stage.bestOf ?? 1,
      winPoints: stage.winPoints,
      lossPoints: stage.lossPoints,
      advancementType: stage.advancementType || 'NONE',
      advancementCount: stage.advancementCount,
      pairingRule: resolveV4StagePairingRuleForSave(stage),
      nextStageCode: stage.nextStageCode || undefined,
      requiresManualConfirmation: stage.requiresManualConfirmation ?? 0
    }
  }
}

async function expandPlayoffFromStage(stage: BasketDivisionStageVo) {
  if (actionBusy.value) return
  const sorted = sortedStages.value
  const startIndex = sorted.findIndex((s) => (stage.id && s.id === stage.id) || s.stageCode === stage.stageCode)
  if (startIndex < 0) return
  const prefix = sorted.slice(0, startIndex)
  const following = sorted.slice(startIndex + 1)
  const inputCount = resolvePlayoffInputTeamCount(prefix, props.maxTeamCount)
  if (!isV4PlayoffTeamCount(inputCount)) {
    proxy!.$modal.msgWarning('当前标准季后赛人数需为 2 的次幂；非标准人数请使用轮空/高级赛制。')
    return
  }
  if (following.some((s) => !isV4PlayoffBracketStageLike(s))) {
    try {
      await proxy!.$modal.confirm('选择季后赛将把当前及后续阶段展开为完整淘汰树，是否继续？')
    } catch {
      return
    }
  }
  const standing = prefix.filter((s) => isV4StandingCapableStage(s))
  const seedSource: V4PlayoffSeedSource = standing.length ? 'STANDING' : 'TEAM_CREATE_TIME'
  const existingBlock = playoffBlock.value
  const existingBos = existingBlock && existingBlock.startIndex === startIndex
    ? existingBlock.stages.map((s) => s.bestOf ?? 1)
    : undefined
  const bestOfList = mapPlayoffBestOfByRoundSize(inputCount, existingBlock?.teamCount, existingBos)
  const playoffPayloads = buildV4PlayoffStages({
    teamCount: inputCount,
    bestOfList,
    seedSource,
    startOrder: startIndex + 1,
    existingCodes: prefix.map((s) => s.stageCode || '').filter(Boolean)
  })
  const prefixPayloads = prefix.map((s) => stageToSchemePayload(s))
  const combined = finalizePlayoffGraphPayloads([...prefixPayloads, ...playoffPayloads], inputCount)
  actionBusy.value = true
  panelLoading.value = true
  try {
    await syncStagesFromPayloads(combined)
    emitChange()
    proxy!.$modal.msgSuccess('已展开季后赛阶段')
  } catch (e: any) {
    // axios 拦截器已对 500 弹过一次；此处不再重复 toast
    await loadSnapshot()
  } finally {
    panelLoading.value = false
    actionBusy.value = false
  }
}

function isV4PlayoffBracketStageLike(stage: { competitionFormat?: string; pairingRule?: string }) {
  return (stage.competitionFormat || '').toUpperCase() === 'SINGLE_ELIMINATION'
    && (stage.pairingRule || '').toUpperCase() === 'PLAYOFF_BRACKET'
}

async function collapsePlayoffBlock(format: string) {
  if (format === 'PLAYOFF') return
  const block = playoffBlock.value
  if (!block || actionBusy.value) return
  const prefix = sortedStages.value.slice(0, block.startIndex)
  const first = block.stages[0]
  const kept: V4SchemeStagePayload = {
    stage: {
      stageCode: first.stageCode || 'PLAYOFF_1',
      stageName: format === 'SINGLE_ELIMINATION' ? '淘汰赛' : '循环赛',
      stageOrder: block.startIndex + 1,
      stageType: 'CUSTOM',
      organizationMode: 'UNGROUPED',
      competitionFormat: format,
      groupingMode: 'NONE',
      roundRobinCycles: format === 'ROUND_ROBIN' ? 1 : undefined,
      bestOf: 1,
      winPoints: format === 'ROUND_ROBIN' ? 2 : undefined,
      lossPoints: format === 'ROUND_ROBIN' ? 1 : undefined,
      advancementType: 'NONE',
      pairingRule: format === 'SINGLE_ELIMINATION' ? 'HIGH_VS_LOW' : 'NONE',
      requiresManualConfirmation: 0
    }
  }
  const prefixPayloads = prefix.map((s) => stageToSchemePayload(s))
  const combined = finalizePlayoffGraphPayloads([...prefixPayloads, kept], props.maxTeamCount)
  actionBusy.value = true
  panelLoading.value = true
  try {
    await syncStagesFromPayloads(combined)
    emitChange()
  } catch {
    await loadSnapshot()
  } finally {
    panelLoading.value = false
    actionBusy.value = false
  }
}

async function handlePlayoffSeedSourceChange(val: V4PlayoffSeedSource) {
  if (val === 'STANDING' && !playoffStandingStages.value.length) {
    proxy!.$modal.msgWarning('当前无可用积分排名阶段。')
    return
  }
  const block = playoffBlock.value
  if (!block || actionBusy.value) return
  const prefix = sortedStages.value.slice(0, block.startIndex)
  const payloads = buildV4PlayoffStages({
    teamCount: block.teamCount,
    bestOfList: block.stages.map((s) => s.bestOf ?? 1),
    seedSource: val,
    startOrder: block.startIndex + 1,
    existingCodes: prefix.map((s) => s.stageCode || '').filter(Boolean)
  })
  payloads.forEach((p, i) => {
    if (block.stages[i]?.stageName) p.stage.stageName = block.stages[i].stageName as string
  })
  const prefixPayloads = prefix.map((s) => stageToSchemePayload(s))
  const combined = finalizePlayoffGraphPayloads([...prefixPayloads, ...payloads], block.teamCount)
  actionBusy.value = true
  panelLoading.value = true
  try {
    await syncStagesFromPayloads(combined)
    emitChange()
  } catch {
    await loadSnapshot()
  } finally {
    panelLoading.value = false
    actionBusy.value = false
  }
}

async function handlePlayoffTeamCountChange(val: number | string) {
  if (actionBusy.value) return
  const n = Number(val)
  const block = playoffBlock.value
  if (!block || block.startIndex !== 0) return
  if (!isV4PlayoffTeamCount(n)) {
    proxy!.$modal.msgWarning('当前标准季后赛人数需为 2 的次幂；非标准人数请使用轮空/高级赛制。')
    return
  }
  if (n === block.teamCount) return
  try {
    await proxy!.$modal.confirm('修改季后赛参赛规模将重新生成阶段配置（同轮次 BO 尽量保留）。是否继续？')
  } catch {
    return
  }
  const prefix = sortedStages.value.slice(0, block.startIndex)
  const bestOfList = mapPlayoffBestOfByRoundSize(
    n,
    block.teamCount,
    block.stages.map((s) => s.bestOf ?? 1)
  )
  const payloads = buildV4PlayoffStages({
    teamCount: n,
    bestOfList,
    seedSource: playoffSeedSource.value,
    startOrder: 1,
    existingCodes: prefix.map((s) => s.stageCode || '').filter(Boolean)
  })
  const prefixPayloads = prefix.map((s) => stageToSchemePayload(s))
  const combined = finalizePlayoffGraphPayloads([...prefixPayloads, ...payloads], n)
  actionBusy.value = true
  panelLoading.value = true
  try {
    await syncStagesFromPayloads(combined)
    emitChange()
    proxy!.$modal.msgSuccess(`已按 ${n} 支重新生成季后赛`)
  } catch {
    await loadSnapshot()
  } finally {
    panelLoading.value = false
    actionBusy.value = false
  }
}

function updatePlayoffBestOf(idx: number, val: number) {
  const sizes = playoffRoundSizes.value
  const list = [...(quickParams.playoffRoundBestOf || sizes.map(() => 1))]
  while (list.length < sizes.length) list.push(1)
  list[idx] = normalizeV4PlayoffBestOf(val)
  quickParams.playoffRoundBestOf = list
  emitChange()
}

function ensureRulesUiState() {
  if (isStandardSnapshot.value) {
    rulesSectionTab.value = 'standard'
    return
  }
  const inferred = inferV4FormatSchemeFromStages(sortedStages.value)
  if (inferred === 'CUSTOM' || !inferred) {
    if (sortedStages.value.length) rulesSectionTab.value = 'custom'
    else rulesSectionTab.value = 'standard'
  }
  selectedScheme.value = inferred || 'GROUP_TO_ELIM'
  Object.assign(quickParams, buildDefaultV4QuickCreateParams(selectedScheme.value, props.maxTeamCount))
  quickParams.expectedTeamCount = props.maxTeamCount
}

async function handleStandardTemplateApplied(payload?: { templateCode?: string }) {
  await loadSnapshot()
  emitChange()
  const name = v4StandardTemplateProductName(payload?.templateCode || snapshot.value?.sourceTemplateCode)
  proxy!.$modal.msgSuccess(`已应用${name}标准模板`)
}

function resolveStageBestOfOptions(stage: BasketDivisionStageVo) {
  if ((stage.competitionFormat || '').toUpperCase() === 'ROUND_ROBIN') {
    return V4_BEST_OF_OPTIONS.filter((opt) => opt.value === 1)
  }
  return V4_BEST_OF_OPTIONS
}

async function handleRulesSectionTabChange(tab: string) {
  const nextTab = tab as RulesSectionTab
  const prevTab = rulesSectionTab.value
  if (nextTab === prevTab) return

  if (nextTab === 'quick' && prevTab === 'custom') {
    if (snapshot.value?.mutable && sortedStages.value.length) {
      try {
        await proxy!.$modal.confirm('切换至快速创建可能覆盖当前未确认的赛制配置，是否继续？')
      } catch {
        return
      }
    }
  }

  rulesSectionTab.value = nextTab

  if (nextTab === 'custom') {
    selectedScheme.value = 'CUSTOM'
    if (!sortedStages.value.length && snapshot.value?.mutable) {
      await handleAddNextStage()
    }
  }

  emitChange()
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
    normalizeStagesForUi()
    emit('snapshot-change', snapshot.value!)
    ensureRulesUiState()
  } finally {
    panelLoading.value = false
  }
}

async function handleSelectScheme(schemeId: V4FormatSchemeId) {
  if (schemeId === 'CUSTOM') {
    rulesSectionTab.value = 'custom'
    selectedScheme.value = 'CUSTOM'
    if (!sortedStages.value.length && snapshot.value?.mutable) {
      await handleAddNextStage()
    }
    emitChange()
    return
  }
  if (selectedScheme.value === schemeId) return
  selectedScheme.value = schemeId
  Object.assign(quickParams, buildDefaultV4QuickCreateParams(schemeId, props.maxTeamCount))
  emitChange()
}

function readSortedSnapshotStages(): BasketDivisionStageVo[] {
  return [...(snapshot.value?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
}

/**
 * 一次性构造完整 Stage Graph：前缀不再是 Final，next/qualification 与真实 stageCode 对齐后再写入。
 */
function finalizePlayoffGraphPayloads(payloads: V4SchemeStagePayload[], fallbackAdvanceCount: number): V4SchemeStagePayload[] {
  const chained = rebuildV4StageLinearChain(payloads.map((p) => p.stage))
  return payloads.map((payload, i) => {
    const stage = { ...payload.stage, ...chained[i] }
    const adv = (stage.advancementType || '').toUpperCase()
    if (adv === 'OVERALL_TOP_N' && !(stage.advancementCount && stage.advancementCount > 0)) {
      stage.advancementCount = fallbackAdvanceCount
    }
    if (adv === 'GROUP_TOP_N' && !(stage.advancementCount && stage.advancementCount > 0)) {
      stage.advancementCount = 1
    }
    if (adv === 'NONE') {
      stage.nextStageCode = undefined
      stage.advancementCount = undefined
    }
    return { ...payload, stage }
  })
}

/** 解除 nextStage 引用，避免 rewrite 过程中校验到尚未写入的编码。 */
function unlinkStageSaveReq(stage: BasketDivisionStageVo): BasketDivisionStageSaveReq {
  const orgMode = resolveV4OrganizationMode(stage)
  const format = (stage.competitionFormat || '').toUpperCase()
  return {
    stageCode: stage.stageCode,
    stageName: stage.stageName,
    stageOrder: stage.stageOrder,
    stageType: 'CUSTOM',
    organizationMode: orgMode,
    competitionFormat: stage.competitionFormat,
    groupingMode: orgMode === 'GROUPED' ? (stage.groupingMode || 'MANUAL') : 'NONE',
    groupCount: orgMode === 'GROUPED' ? stage.groupCount : undefined,
    roundRobinCycles: format === 'ROUND_ROBIN' ? (stage.roundRobinCycles ?? 1) : stage.roundRobinCycles,
    bestOf: stage.bestOf ?? 1,
    winPoints: format === 'ROUND_ROBIN' ? (stage.winPoints ?? 2) : stage.winPoints,
    lossPoints: format === 'ROUND_ROBIN' ? (stage.lossPoints ?? 1) : stage.lossPoints,
    advancementType: 'NONE',
    advancementCount: undefined,
    pairingRule: resolveV4StagePairingRuleForSave(stage),
    nextStageCode: '',
    requiresManualConfirmation: stage.requiresManualConfirmation ?? 0
  }
}

function asUnlinkedStagePayload(stage: V4SchemeStagePayload['stage']): BasketDivisionStageSaveReq {
  return {
    ...stage,
    nextStageCode: '',
    advancementType: 'NONE',
    advancementCount: undefined
  }
}

/**
 * 两阶段写入：先保证所有 stageCode 都在库里（无悬空 next），再一次性挂上 next 链。
 * 禁止「先写 Stage1.next=PLAYOFF_8、此时 PLAYOFF_8 还不存在」。
 */
async function syncStagesFromPayloads(payloads: V4SchemeStagePayload[]) {
  await loadSnapshot()
  let stages = readSortedSnapshotStages()

  for (const existing of stages) {
    if (!existing.id || !existing.nextStageCode) continue
    await updateFormatStage(props.divisionId, existing.id, unlinkStageSaveReq(existing))
  }
  await loadSnapshot()
  stages = readSortedSnapshotStages()

  while (stages.length > payloads.length) {
    const last = stages[stages.length - 1]
    if (!last?.id) break
    await deleteFormatStage(props.divisionId, last.id)
    await loadSnapshot()
    stages = readSortedSnapshotStages()
  }

  for (let i = 0; i < payloads.length; i += 1) {
    const { stage, tieBreakRules } = payloads[i]
    const stub = asUnlinkedStagePayload(stage)
    if (!stub.stageCode) {
      const existingCodes = readSortedSnapshotStages().map((s) => s.stageCode || '').filter(Boolean)
      const preset = inferV4StageCodePreset(stage)
      stub.stageCode = generateV4StageCode(preset, existingCodes)
      stage.stageCode = stub.stageCode
    }
    stages = readSortedSnapshotStages()
    if (i < stages.length && stages[i]?.id) {
      await updateFormatStage(props.divisionId, stages[i].id!, stub)
      if (tieBreakRules?.length) {
        await saveTieBreakRules(props.divisionId, { stageId: stages[i].id, rules: tieBreakRules })
      }
    } else {
      const res = await addFormatStage(props.divisionId, stub)
      if (tieBreakRules?.length && res.data) {
        await saveTieBreakRules(props.divisionId, { stageId: res.data, rules: tieBreakRules })
      }
    }
    await loadSnapshot()
  }

  stages = readSortedSnapshotStages()
  for (let i = 0; i < payloads.length; i += 1) {
    const existing = stages[i]
    if (!existing?.id) continue
    const linked = { ...payloads[i].stage }
    if (!linked.nextStageCode) linked.nextStageCode = ''
    await updateFormatStage(props.divisionId, existing.id, linked)
  }
  await loadSnapshot()
}

function buildFormatPlanApplyReq(schemeId: V4FormatSchemeId): BasketDivisionFormatPlanApplyReq | null {
  if (schemeId === 'GROUP_TO_ELIM') {
    return {
      planCode: 'GROUP_TO_ELIM',
      expectedTeamCount: props.maxTeamCount,
      groupCount: quickParams.groupCount ?? 4,
      roundRobinLegs: quickParams.roundRobinCycles ?? 1,
      qualifiersPerGroup: quickParams.advancePerGroup ?? 2,
      eliminationPairingRule: normalizeV4EliminationPairingRule(quickParams.pairingRule),
      winPoints: 2,
      lossPoints: 1
    }
  }
  if (schemeId === 'ROUND_ROBIN_TO_ELIM') {
    return {
      planCode: 'ROUND_ROBIN_TO_ELIM',
      expectedTeamCount: props.maxTeamCount,
      roundRobinLegs: quickParams.roundRobinCycles ?? 1,
      overallAdvanceCount: quickParams.overallAdvanceCount ?? 4,
      eliminationPairingRule: normalizeV4EliminationPairingRule(quickParams.pairingRule),
      winPoints: 2,
      lossPoints: 1
    }
  }
  return null
}

function isComboScheme(schemeId: V4FormatSchemeId): boolean {
  return schemeId === 'GROUP_TO_ELIM' || schemeId === 'ROUND_ROBIN_TO_ELIM'
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

  applyingScheme.value = true
  actionBusy.value = true
  panelLoading.value = true
  try {
    if (isComboScheme(schemeId)) {
      const planReq = buildFormatPlanApplyReq(schemeId)
      if (!planReq) {
        proxy!.$modal.msgError('该组合方案暂不可用')
        return
      }
      const res = await applyFormatPlan(props.divisionId, planReq)
      snapshot.value = res.data || {}
      emit('update:formatTemplateId', snapshot.value?.sourceTemplateId)
      emit('snapshot-change', snapshot.value!)
      ensureRulesUiState()
    } else {
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
      emit('update:formatTemplateId', template.id)
      await rebuildFormatSnapshot(props.divisionId, template.id)
      const payloads = buildV4SchemeStagePayloads(schemeId, quickParams)
      await syncStagesFromPayloads(payloads)
    }
    rulesSectionTab.value = 'custom'
    emitChange()
    proxy!.$modal.msgSuccess('赛制方案已应用，请在「自定义赛制」查看流程预览')
  } catch (e: any) {
    await loadSnapshot()
    proxy!.$modal.msgError(e?.msg || e?.message || '应用赛制方案失败')
  } finally {
    panelLoading.value = false
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
    stageType: stage.stageType || 'CUSTOM',
    organizationMode: resolveV4OrganizationMode(stage),
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
    const payload: BasketDivisionStageSaveReq = {
      ...stageForm,
      stageType: 'CUSTOM',
      organizationMode: stageForm.organizationMode || resolveV4OrganizationMode(stageForm)
    }
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
  if (sortedStages.value.length <= 1) {
    proxy!.$modal.msgWarning('至少需要保留一个比赛阶段')
    return
  }
  await proxy!.$modal.confirm(`确认删除阶段「${stage.stageName}」？`)
  actionBusy.value = true
  try {
    const remaining = sortedStages.value.filter((s) => s.id !== stage.id)
    const chain = rebuildV4StageLinearChain(remaining)
    for (let i = 0; i < remaining.length; i += 1) {
      const existing = remaining[i]
      if (!existing.id) continue
      const chained = chain[i]
      const payload = buildStageSavePayload(existing)
      payload.stageOrder = chained.stageOrder
      payload.nextStageCode = chained.nextStageCode
      payload.advancementType = chained.advancementType
      payload.advancementCount = chained.advancementCount
      await updateFormatStage(props.divisionId, existing.id, payload)
    }
    await deleteFormatStage(props.divisionId, stage.id)
    await loadSnapshot()
    emitChange()
    proxy!.$modal.msgSuccess('删除成功')
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '删除失败')
    await loadSnapshot()
  } finally {
    actionBusy.value = false
  }
}

async function moveStage(stage: BasketDivisionStageVo, delta: number) {
  if (!stage.id || actionBusy.value) return
  const stages = sortedStages.value
  const index = stages.findIndex((s) => s.id === stage.id)
  const target = index + delta
  if (target < 0 || target >= stages.length) return
  actionBusy.value = true
  try {
    const reordered = [...stages]
    const [moved] = reordered.splice(index, 1)
    reordered.splice(target, 0, moved)
    await reorderFormatStages(props.divisionId, reordered.map((s) => s.id!))
    await loadSnapshot()
    await persistAllStages()
    emitChange()
  } catch (e: any) {
    proxy!.$modal.msgError(e?.msg || e?.message || '移动失败')
    await loadSnapshot()
  } finally {
    actionBusy.value = false
  }
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

watch(
  () => {
    const block = playoffBlock.value
    if (!block || !snapshot.value?.mutable) return ''
    const prefix = sortedStages.value.slice(0, block.startIndex)
    const input = resolvePlayoffInputTeamCount(prefix, props.maxTeamCount)
    return `${input}:${block.teamCount}`
  },
  async (key, prev) => {
    if (!key || !prev || actionBusy.value || panelLoading.value) return
    const [input, current] = key.split(':').map(Number)
    if (!input || input === current) return
    if (!isV4PlayoffTeamCount(input)) {
      proxy!.$modal.msgWarning('当前标准季后赛人数需为 2 的次幂；非标准人数请使用轮空/高级赛制。')
      return
    }
    try {
      await proxy!.$modal.confirm(
        `赛区/晋级人数已变为 ${input} 支，当前季后赛为 ${current} 支。是否按 ${input} 支重新生成阶段配置（同轮次 BO 尽量保留）？`
      )
    } catch {
      return
    }
    const first = playoffBlock.value?.stages[0]
    if (!first) return
    await expandPlayoffFromStage(first as BasketDivisionStageVo)
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

.scheme-card-check {
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 12px;
  color: var(--el-color-primary);
  font-weight: 600;
}

.scheme-card :deep(.el-card__body) {
  padding: 14px 16px 12px;
  padding-right: 72px;
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

.current-scheme-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.bo-capability-hint {
  margin: 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.stage-flow-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.stage-flow-card {
  cursor: pointer;
}

.stage-config-card {
  border-left: 3px solid #c41e3a;
}

.stage-config-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.stage-flow-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.stage-flow-order {
  font-weight: 700;
  color: #c41e3a;
  font-size: 14px;
  white-space: nowrap;
}

.stage-name-input {
  max-width: 220px;
}

.stage-card-summary {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-color-primary);
  line-height: 1.6;
}

.stage-inline-form {
  margin-top: 8px;
}

.bo-hint-alert :deep(.el-alert__title) {
  font-size: 12px;
  line-height: 1.5;
}

.custom-preview-chain {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.custom-preview-step {
  width: 100%;
  max-width: 520px;
  padding: 12px 16px;
  background: var(--el-fill-color-light);
  border-radius: 6px;
  text-align: center;
}

.custom-preview-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.custom-preview-summary {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.custom-preview-advance {
  font-size: 12px;
  color: var(--el-color-primary);
  margin-top: 4px;
}

.custom-preview-arrow {
  font-size: 18px;
  color: var(--el-text-color-placeholder);
  padding: 4px 0;
}

.lock-checklist {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 13px;
  line-height: 2;
}

.lock-checklist li.ok {
  color: var(--el-color-success);
}

.lock-checklist li.warn {
  color: var(--el-color-warning);
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

.stage-config-card {
  border: 1px solid var(--el-border-color-light);
  background: linear-gradient(180deg, #1a1a1a 0%, #242424 48px, #fff 48px);
}

.stage-config-card :deep(.el-card__body) {
  padding-top: 12px;
}

.stage-flow-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.stage-flow-order {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #c41e3a;
  border-radius: 4px;
}

.stage-name-input {
  width: 200px;
}

.stage-card-summary {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--el-color-primary);
}

.stage-inline-form {
  margin-top: 4px;
}

.custom-preview-chain {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.custom-preview-step {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}

.custom-preview-title {
  font-weight: 600;
  margin-bottom: 4px;
}

.custom-preview-summary {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.custom-preview-advance {
  font-size: 12px;
  color: var(--el-color-primary);
  margin-top: 4px;
}

.custom-preview-arrow {
  font-size: 18px;
  color: var(--el-text-color-secondary);
  padding: 4px 0;
}

.lock-checklist {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: 13px;
  line-height: 2;
}

.lock-checklist li.ok {
  color: var(--el-color-success);
}

.lock-checklist li.warn {
  color: var(--el-color-warning);
}

.bo-hint-alert {
  margin-top: 8px;
}
.playoff-bo-list { display: flex; flex-direction: column; gap: 8px; width: 100%; }
.playoff-bo-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.playoff-bo-name { min-width: 72px; color: var(--el-text-color-regular); }
.playoff-bracket-preview { padding: 8px 12px; background: var(--el-fill-color-lighter); border-radius: 6px; }
.playoff-bracket-line { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; line-height: 1.7; }
.playoff-stage-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.playoff-stage-name { width: 160px; }
.playoff-stage-size {
  min-width: 72px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
