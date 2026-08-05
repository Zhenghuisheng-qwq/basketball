<template>
  <div class="app-container">
    <el-alert
      title="新版赛事结构"
      type="info"
      :closable="false"
      show-icon
      class="mb8 page-tip-alert"
    >
      <template #default>
        <ul class="page-tip-list">
          <li>层级：赛事系列 → 赛季 → 平台赛区。</li>
          <li>同一赛季可创建多个平台赛区，各赛区独立配置平台、比赛模式、报名方式、人数、名额、报名时间和规则模板。</li>
          <li>当前支持单循环、小组赛和 BO1 单败淘汰。</li>
          <li>BO3、BO5、双败及淘汰赛轮空暂未开放。</li>
          <li>个人报名采用后台录入线下抽签组队结果，不是系统在线自动抽签。</li>
        </ul>
      </template>
    </el-alert>

    <el-card shadow="never" class="mb8">
      <template #header><span>赛事系列</span></template>
      <el-radio-group v-model="selectedSeriesId" @change="handleSeriesChange">
        <el-radio-button v-for="item in seriesList" :key="item.id" :label="item.id">
          {{ v4SeriesDisplayLabel(item.seriesName, item.seriesCode) }}
        </el-radio-button>
      </el-radio-group>
    </el-card>

    <el-card shadow="never" class="mb8">
      <template #header><span>赛季列表</span></template>

      <div class="season-query-bar">
        <el-form :model="seasonQuery" class="season-query-form" @submit.prevent>
          <el-form-item label="年份">
            <el-input-number v-model="seasonQuery.seasonYear" :min="2020" :max="2099" controls-position="right" class="query-year" />
          </el-form-item>
          <el-form-item label="赛季名称">
            <el-input v-model="seasonQuery.seasonName" clearable placeholder="赛季名称" class="query-name" />
          </el-form-item>
          <el-form-item label="发布状态">
            <el-select v-model="seasonQuery.publishStatus" clearable placeholder="发布状态" class="query-status">
              <el-option v-for="item in PUBLISH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item class="query-actions">
            <el-button type="primary" icon="Search" @click="loadSeasonList">搜索</el-button>
            <el-button icon="Refresh" @click="resetSeasonQuery">重置</el-button>
            <el-button type="primary" icon="Plus" @click="handleCreateEvent" v-hasPermi="['basket:v4Season:add']" :disabled="!selectedSeriesId">新建赛事</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-loading="seasonLoading" :data="seasonList" border highlight-current-row @current-change="handleSeasonSelect">
        <el-table-column label="赛事系列" prop="seriesName" min-width="100" />
        <el-table-column label="年份" prop="seasonYear" width="80" />
        <el-table-column label="赛季名称" prop="seasonName" min-width="120" />
        <el-table-column label="前台标题" prop="displayTitle" min-width="140" />
        <el-table-column label="赛季周期" min-width="168" show-overflow-tooltip>
          <template #default="scope">
            <span class="time-range">{{ formatV4CompactRange(scope.row.seasonStartTime, scope.row.seasonEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" prop="publishStatus" width="88" align="center">
          <template #default="scope">
            <el-tag :type="v4PublishStatusTagType(scope.row.publishStatus)" size="small" class="nowrap-tag">
              {{ v4PublishStatusLabel(scope.row.publishStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="赛季状态" prop="seasonStatus" width="96" align="center">
          <template #default="scope">
            <el-tag :type="v4SeasonStatusTagType(scope.row.seasonStatus)" size="small" class="nowrap-tag">
              {{ v4SeasonStatusLabel(scope.row.seasonStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="平台赛区数" prop="divisionCount" width="100" align="center" />
        <el-table-column label="操作" width="380" fixed="right" class-name="small-padding fixed-width">
          <template #default="scope">
            <div class="table-actions">
              <el-button link type="primary" @click="handleOpenSeasonDetail(scope.row)">赛季详情</el-button>
              <el-button link type="primary" @click="handleContinuePublish(scope.row)" v-if="scope.row.publishStatus === 'DRAFT'">继续发布</el-button>
              <el-button link type="primary" @click="handleEditSeason(scope.row)" v-hasPermi="['basket:v4Season:edit']" v-if="scope.row.seasonStatus !== 'ARCHIVED'">编辑基础资料</el-button>
              <el-button link type="primary" @click="handleSelectSeason(scope.row)">查看赛区</el-button>
              <el-button link type="success" @click="handlePublishSeason(scope.row)" v-hasPermi="['basket:v4Season:publish']" v-if="scope.row.publishStatus === 'DRAFT' || scope.row.publishStatus === 'OFFLINE'">发布</el-button>
              <el-button link type="warning" @click="handleOfflineSeason(scope.row)" v-hasPermi="['basket:v4Season:offline']" v-if="scope.row.publishStatus === 'PUBLISHED'">下线</el-button>
              <el-button link type="danger" @click="handleDeleteSeason(scope.row)" v-hasPermi="['basket:v4Season:remove']" v-if="scope.row.publishStatus === 'DRAFT'">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <pagination :total="seasonTotal" v-model:page="seasonQuery.pageNum" v-model:limit="seasonQuery.pageSize" @pagination="loadSeasonList" />
    </el-card>

    <el-card shadow="never" v-if="selectedSeason">
      <template #header>
        <div class="card-header">
          <span>平台赛区列表 — {{ selectedSeason.displayTitle || selectedSeason.seasonName }}</span>
          <el-button type="primary" plain icon="Plus" @click="handleAddDivision" v-hasPermi="['basket:v4Division:add']">新建赛区</el-button>
        </div>
      </template>

      <el-table v-loading="divisionLoading" :data="divisionList" border>
        <el-table-column label="平台" prop="platformCode" width="100">
          <template #default="scope">{{ v4PlatformLabel(scope.row.platformCode) }}</template>
        </el-table-column>
        <el-table-column label="比赛模式" prop="gameMode" width="90" align="center">
          <template #default="scope">{{ v4GameModeLabel(scope.row.gameMode) }}</template>
        </el-table-column>
        <el-table-column label="赛区名称" prop="divisionName" min-width="140" />
        <el-table-column label="报名方式" prop="registrationMode" width="108" align="center">
          <template #default="scope">
            <el-tag :type="v4RegistrationModeTagType(scope.row.registrationMode)" size="small" class="nowrap-tag">
              {{ v4RegistrationModeLabel(scope.row.registrationMode) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="赛制" prop="formatType" width="128" align="center">
          <template #default="scope">
            <el-tag :type="v4FormatTypeTagType(scope.row.formatType)" size="small" class="nowrap-tag">
              {{ v4FormatTypeLabel(scope.row.formatType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="赛制模板" prop="formatTemplateName" min-width="130" show-overflow-tooltip />
        <el-table-column label="规则状态" width="96" align="center">
          <template #default="scope">{{ v4SnapshotStatusLabel(scope.row.formatSnapshotStatus) }}</template>
        </el-table-column>
        <el-table-column label="队伍上限" prop="maxTeamCount" width="90" align="center" />
        <el-table-column label="阵容人数" width="100" align="center">
          <template #default="scope">{{ scope.row.lineupMinCount }}-{{ scope.row.lineupMaxCount }}</template>
        </el-table-column>
        <el-table-column label="报名时间" min-width="168" show-overflow-tooltip>
          <template #default="scope">
            <span class="time-range">{{ formatV4CompactRange(scope.row.registrationStartTime, scope.row.registrationEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="比赛时间" min-width="168" show-overflow-tooltip>
          <template #default="scope">
            <span class="time-range">{{ formatV4CompactRange(scope.row.matchStartTime, scope.row.matchEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="divisionStatus" width="96" align="center">
          <template #default="scope">
            <el-tag :type="v4DivisionStatusTagType(scope.row.divisionStatus)" size="small" class="nowrap-tag">
              {{ v4DivisionStatusLabel(scope.row.divisionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right" class-name="small-padding fixed-width">
          <template #default="scope">
            <div class="table-actions">
              <el-button link type="primary" @click="handleEditDivision(scope.row)" v-hasPermi="['basket:v4Division:edit']">编辑</el-button>
              <el-button link type="success" @click="handleOpenDivision(scope.row)" v-hasPermi="['basket:v4Division:openRegistration']" v-if="scope.row.divisionStatus === 'DRAFT'">开放报名</el-button>
              <el-button link type="warning" @click="handleCloseDivision(scope.row)" v-hasPermi="['basket:v4Division:closeRegistration']" v-if="scope.row.divisionStatus === 'REGISTRATION_OPEN'">关闭报名</el-button>
              <el-button link type="danger" @click="handleDeleteDivision(scope.row)" v-hasPermi="['basket:v4Division:remove']" v-if="scope.row.divisionStatus === 'DRAFT'">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <pagination :total="divisionTotal" v-model:page="divisionQuery.pageNum" v-model:limit="divisionQuery.pageSize" @pagination="loadDivisionList" />
    </el-card>

    <el-dialog :title="seasonDialogTitle" v-model="seasonDialogOpen" width="760px" append-to-body>
      <el-form ref="seasonFormRef" :model="seasonForm" :rules="seasonRules" label-width="120px">
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
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="封面图">
              <image-upload v-model="coverPreview" :limit="1" :file-size="5" />
              <div class="el-form-item__tip">支持上传预览；文件资产 ID 绑定待后续接通</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Banner 图">
              <image-upload v-model="bannerPreview" :limit="1" :file-size="5" />
              <div class="el-form-item__tip">支持上传预览；文件资产 ID 绑定待后续接通</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="seasonDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitSeasonForm">确定</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="seasonDetailOpen" :title="seasonDetailTitle" size="860px" append-to-body destroy-on-close>
      <el-tabs v-model="seasonDetailTab" v-loading="seasonDetailLoading">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions v-if="seasonDetail" :column="2" border class="mb8">
            <el-descriptions-item label="赛事系列">{{ seasonDetail.seriesName }}</el-descriptions-item>
            <el-descriptions-item label="年份">{{ seasonDetail.seasonYear }}</el-descriptions-item>
            <el-descriptions-item label="赛季名称">{{ seasonDetail.seasonName }}</el-descriptions-item>
            <el-descriptions-item label="前台标题">{{ seasonDetail.displayTitle }}</el-descriptions-item>
            <el-descriptions-item label="发布状态">
              <el-tag :type="v4PublishStatusTagType(seasonDetail.publishStatus)" size="small">{{ v4PublishStatusLabel(seasonDetail.publishStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="赛季状态">
              <el-tag :type="v4SeasonStatusTagType(seasonDetail.seasonStatus)" size="small">{{ v4SeasonStatusLabel(seasonDetail.seasonStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="赛季周期" :span="2">{{ formatV4CompactRange(seasonDetail.seasonStartTime, seasonDetail.seasonEndTime) }}</el-descriptions-item>
            <el-descriptions-item label="平台赛区数">{{ seasonDetail.divisionCount ?? seasonDetail.divisions?.length ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="简介" :span="2">{{ seasonDetail.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="赛区管理" name="division">
          <el-table :data="seasonDetail?.divisions || []" border size="small">
            <el-table-column label="平台" prop="platformCode" width="80" />
            <el-table-column label="赛区名称" prop="divisionName" min-width="140" />
            <el-table-column label="队伍上限" prop="maxTeamCount" width="90" align="center" />
            <el-table-column label="状态" prop="divisionStatus" width="110" align="center">
              <template #default="scope">{{ v4DivisionStatusLabel(scope.row.divisionStatus) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!(seasonDetail?.divisions?.length)" description="尚未创建平台赛区" />
        </el-tab-pane>
        <el-tab-pane label="赛制规则" name="rules">
          <el-form :inline="true" class="mb8">
            <el-form-item label="平台赛区">
              <el-select v-model="rulesDivisionId" placeholder="选择赛区" style="width: 260px" @change="loadRulesSnapshot">
                <el-option v-for="d in seasonDetail?.divisions || []" :key="d.id" :label="`${d.divisionName}（${v4PlatformLabel(d.platformCode)}）`" :value="d.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="rulesSnapshot?.mutable">
              <el-button type="primary" plain icon="Plus" @click="openAddStage" v-hasPermi="['basket:v4FormatRule:edit']">新增阶段</el-button>
              <el-button type="warning" plain @click="openRebuildSnapshot" v-hasPermi="['basket:v4FormatRule:edit']">重建快照</el-button>
              <el-button type="success" plain @click="handleLockSnapshot" v-hasPermi="['basket:v4FormatRule:lock']" v-if="rulesSnapshot?.lockable">锁定规则</el-button>
            </el-form-item>
          </el-form>

          <el-alert v-if="rulesSnapshot && !rulesSnapshot.mutable" type="warning" :closable="false" show-icon class="mb8" title="规则已锁定，不可编辑" />

          <el-descriptions v-if="rulesSnapshot" :column="2" border class="mb8">
            <el-descriptions-item label="模板">{{ rulesSnapshot.sourceTemplateName }} v{{ rulesSnapshot.sourceTemplateVersion }}</el-descriptions-item>
            <el-descriptions-item label="快照版本">v{{ rulesSnapshot.snapshotVersion ?? 1 }}</el-descriptions-item>
            <el-descriptions-item label="快照状态">
              <el-tag :type="v4SnapshotStatusTagType(rulesSnapshot.snapshotStatus)" size="small">
                {{ rulesSnapshot.snapshotStatusName || v4SnapshotStatusLabel(rulesSnapshot.snapshotStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="可编辑">{{ rulesSnapshot.mutable ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="已锁定">{{ rulesSnapshot.locked ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="规则摘要" :span="2">{{ rulesSnapshot.formatSummary || '-' }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="sortedRuleStages.length" class="stage-card-list">
            <el-card v-for="(stage, index) in sortedRuleStages" :key="stage.id || stage.stageCode" shadow="hover" class="stage-card">
              <template #header>
                <div class="stage-card-header">
                  <span>{{ stage.stageOrder }}. {{ stage.stageName }}（{{ stage.stageCode }}）</span>
                  <div v-if="rulesSnapshot?.mutable" class="stage-card-actions">
                    <el-button link type="primary" @click="openEditStage(stage)">编辑</el-button>
                    <el-button link type="danger" @click="handleDeleteStage(stage)">删除</el-button>
                    <el-button link :disabled="index === 0" @click="moveStage(stage, -1)">上移</el-button>
                    <el-button link :disabled="index === sortedRuleStages.length - 1" @click="moveStage(stage, 1)">下移</el-button>
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
                <el-descriptions-item label="晋级名额">
                  <span v-if="isLeagueRegularStage(stage)" class="advancement-highlight">{{ stage.advancementCount ?? '-' }}</span>
                  <span v-else>{{ stage.advancementCount ?? '-' }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="下一阶段">{{ stage.nextStageCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="人工确认">{{ stage.requiresManualConfirmation === 1 ? '是' : '否' }}</el-descriptions-item>
              </el-descriptions>
              <div v-if="stage.competitionFormat === 'ROUND_ROBIN' && stage.tieBreakRules?.length" class="tie-break-summary">
                <span class="tie-break-label">同分规则：</span>
                <el-tag v-for="rule in stage.tieBreakRules" :key="rule.id" size="small" class="mr4" :type="rule.enabled === 1 ? '' : 'info'">
                  {{ rule.priorityNo }}. {{ rule.ruleCodeName || v4TieBreakRuleLabel(rule.ruleCode) }}
                </el-tag>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="请选择赛区或在 Division 创建后查看规则快照" />
          <div class="el-form-item__tip mt8">草稿状态下可编辑规则；开放报名前请锁定快照。LEAGUE_STANDARD_V1 常规赛阶段可配置晋级名额。</div>
        </el-tab-pane>
        <el-tab-pane label="发布设置" name="publish">
          <el-descriptions v-if="seasonDetail" :column="1" border>
            <el-descriptions-item label="发布状态">
              <el-tag :type="v4PublishStatusTagType(seasonDetail.publishStatus)" size="small">{{ v4PublishStatusLabel(seasonDetail.publishStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="赛季状态">
              <el-tag :type="v4SeasonStatusTagType(seasonDetail.seasonStatus)" size="small">{{ v4SeasonStatusLabel(seasonDetail.seasonStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="平台赛区数">{{ seasonDetail.divisionCount ?? seasonDetail.divisions?.length ?? 0 }}</el-descriptions-item>
            <el-descriptions-item label="操作提示">可先发布赛季用于内容预告；仅当存在有效平台赛区且赛区开放报名时，前台才展示报名入口。</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog :title="divisionDialogTitle" v-model="divisionDialogOpen" width="720px" append-to-body>
      <el-form ref="divisionFormRef" :model="divisionForm" :rules="divisionRules" label-width="120px">
        <el-form-item label="赛季">
          <el-input :model-value="selectedSeason?.displayTitle || selectedSeason?.seasonName" disabled />
        </el-form-item>
        <el-form-item label="平台" prop="platformCode">
          <el-select v-model="divisionForm.platformCode" style="width: 100%">
            <el-option label="PC" value="PC" />
            <el-option label="PS/Xbox" value="PS_XBOX" />
          </el-select>
        </el-form-item>
        <el-form-item label="比赛模式" prop="gameMode">
          <el-radio-group v-model="divisionForm.gameMode">
            <el-radio label="FIVE_V_FIVE">5V5</el-radio>
            <el-radio label="THREE_V_THREE">3V3</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="赛区名称" prop="divisionName">
          <el-input v-model="divisionForm.divisionName" placeholder="如 League 2026 S1 - PS" />
        </el-form-item>
        <el-form-item label="报名方式" prop="registrationMode">
          <el-radio-group v-model="divisionForm.registrationMode">
            <el-radio label="TEAM">球队报名</el-radio>
            <el-radio label="PERSONAL">个人报名</el-radio>
          </el-radio-group>
          <div v-if="divisionForm.registrationMode === 'PERSONAL'" class="el-form-item__tip">个人报名审核通过后，由运营线下抽签组队并录入结果。</div>
        </el-form-item>
        <el-form-item label="赛制模板" prop="formatTemplateId">
          <el-select v-model="divisionForm.formatTemplateId" style="width: 100%" placeholder="请选择赛制模板">
            <el-option v-for="item in formatTemplateList" :key="item.id" :label="`${item.templateName} (${item.templateCode})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="阵容人数规则">
          <el-input :model-value="v4RosterRuleText(divisionForm.gameMode)" disabled />
        </el-form-item>
        <el-form-item label="赛制（过渡）" prop="formatType">
          <el-radio-group v-model="divisionForm.formatType">
            <el-radio label="REGULAR_ROUND_ROBIN">常规赛单循环</el-radio>
          </el-radio-group>
          <div class="el-form-item__tip">过渡字段，实际执行规则以模板快照为准</div>
        </el-form-item>
        <el-form-item label="队伍上限" prop="maxTeamCount">
          <el-input-number v-model="divisionForm.maxTeamCount" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报名开始">
          <el-date-picker v-model="divisionForm.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报名结束">
          <el-date-picker v-model="divisionForm.registrationEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="比赛开始">
          <el-date-picker v-model="divisionForm.matchStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="比赛结束">
          <el-date-picker v-model="divisionForm.matchEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="divisionForm.sortOrder" :min="0" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="divisionDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="submitDivisionForm">确定</el-button>
      </template>
    </el-dialog>

    <PublishWizardDialog
      v-model="wizardOpen"
      :series-list="seriesList"
      :series-id="selectedSeriesId"
      :season-id="wizardSeasonId"
      @success="handleWizardSuccess"
    />

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
          <div v-if="isLeagueRegularForm" class="el-form-item__tip">LEAGUE_STANDARD_V1 常规赛阶段：配置总排名前 N 晋级名额</div>
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
  </div>
</template>

<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import PublishWizardDialog from './components/PublishWizardDialog.vue'
import { listV4Series, type BasketSeriesVo } from '@/api/cnpa/event-v4/series'
import {
  addV4Season,
  delV4Season,
  getV4Season,
  listV4Season,
  offlineV4Season,
  publishV4Season,
  updateV4Season,
  type BasketSeasonCreateReq,
  type BasketSeasonDetailVo,
  type BasketSeasonListVo
} from '@/api/cnpa/event-v4/season'
import {
  addV4Division,
  closeV4DivisionRegistration,
  delV4Division,
  listV4Division,
  openV4DivisionRegistration,
  updateV4Division,
  type BasketDivisionCreateReq,
  type BasketDivisionListVo
} from '@/api/cnpa/event-v4/division'
import {
  addFormatStage,
  deleteFormatStage,
  getFormatSnapshot,
  getV4FormatTemplate,
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
  V4_PUBLISH_STATUS_FILTER_OPTIONS,
  V4_STAGE_TYPE_OPTIONS,
  V4_TIE_BREAK_RULE_OPTIONS,
  formatV4CompactRange,
  v4AdvancementTypeLabel,
  v4CompetitionFormatLabel,
  v4DivisionStatusLabel,
  v4DivisionStatusTagType,
  v4FormatTypeLabel,
  v4FormatTypeTagType,
  v4GameModeLabel,
  v4GroupingModeLabel,
  v4PlatformLabel,
  v4PublishStatusLabel,
  v4PublishStatusTagType,
  v4RegistrationModeLabel,
  v4RegistrationModeTagType,
  v4RosterRuleText,
  v4SeriesDisplayLabel,
  v4SeasonStatusLabel,
  v4SeasonStatusTagType,
  v4SnapshotStatusLabel,
  v4SnapshotStatusTagType,
  v4StageTypeLabel,
  v4TieBreakRuleLabel
} from '@/utils/v4AdminDisplay'

const { proxy } = getCurrentInstance()!

const PUBLISH_STATUS_OPTIONS = V4_PUBLISH_STATUS_FILTER_OPTIONS

const seriesList = ref<BasketSeriesVo[]>([])
const selectedSeriesId = ref<number>()
const seasonList = ref<BasketSeasonListVo[]>([])
const seasonTotal = ref(0)
const seasonLoading = ref(false)
const selectedSeason = ref<BasketSeasonListVo>()
const divisionList = ref<BasketDivisionListVo[]>([])
const divisionTotal = ref(0)
const divisionLoading = ref(false)
const formatTemplateList = ref<BasketFormatTemplateListVo[]>([])
const seasonDetailSnapshot = ref<BasketDivisionFormatSnapshotVo>()

const rulesDivisionId = ref<number>()
const rulesSnapshot = ref<BasketDivisionFormatSnapshotVo>()
const stageDialogOpen = ref(false)
const stageDialogTitle = ref('')
const stageFormRef = ref()
const stageEditingId = ref<number>()
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

const STAGE_TYPE_OPTIONS = V4_STAGE_TYPE_OPTIONS
const COMPETITION_FORMAT_OPTIONS = V4_COMPETITION_FORMAT_OPTIONS
const GROUPING_MODE_OPTIONS = V4_GROUPING_MODE_OPTIONS
const ADVANCEMENT_TYPE_OPTIONS = V4_ADVANCEMENT_TYPE_OPTIONS
const TIE_BREAK_DEFAULTS = V4_TIE_BREAK_RULE_OPTIONS.map((item, idx) => ({
  ruleCode: item.value,
  priorityNo: idx + 1,
  enabled: item.value === 'MANUAL' ? 0 : 1
}))

const stageRules = {
  stageCode: [{ required: true, message: '请输入阶段编码', trigger: 'blur' }],
  stageName: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }],
  stageType: [{ required: true, message: '请选择阶段类型', trigger: 'change' }],
  competitionFormat: [{ required: true, message: '请选择比赛方式', trigger: 'change' }],
  groupingMode: [{ required: true, message: '请选择分组模式', trigger: 'change' }]
}

const sortedRuleStages = computed(() =>
  [...(rulesSnapshot.value?.stages || [])].sort((a, b) => (a.stageOrder || 0) - (b.stageOrder || 0))
)

const needsAdvancementCount = computed(() =>
  stageForm.advancementType === 'OVERALL_TOP_N' || stageForm.advancementType === 'GROUP_TOP_N'
)

const isLeagueRegularForm = computed(() =>
  rulesSnapshot.value?.sourceTemplateCode === 'LEAGUE_STANDARD_V1' && stageForm.stageType === 'REGULAR'
)

function isLeagueRegularStage(stage: BasketDivisionStageVo) {
  return rulesSnapshot.value?.sourceTemplateCode === 'LEAGUE_STANDARD_V1'
    && stage.stageType === 'REGULAR'
    && stage.advancementType === 'OVERALL_TOP_N'
}

const seasonQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  seriesId: undefined as number | undefined,
  seasonYear: undefined as number | undefined,
  seasonName: '',
  publishStatus: ''
})

const divisionQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  seasonId: undefined as number | undefined
})

const wizardOpen = ref(false)
const wizardSeasonId = ref<number>()

const seasonDialogOpen = ref(false)
const seasonDialogTitle = ref('')
const seasonFormRef = ref()
const coverPreview = ref('')
const bannerPreview = ref('')
const seasonPeriodRange = ref<[string, string] | null>(null)
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

watch(seasonPeriodRange, (range) => {
  if (range && range.length === 2) {
    seasonForm.seasonStartTime = range[0]
    seasonForm.seasonEndTime = range[1]
  } else {
    seasonForm.seasonStartTime = ''
    seasonForm.seasonEndTime = ''
  }
})

const divisionDialogOpen = ref(false)
const divisionDialogTitle = ref('')
const divisionFormRef = ref()
const divisionForm = reactive<BasketDivisionCreateReq & { id?: number }>({
  seasonId: undefined,
  platformCode: 'PS_XBOX',
  gameMode: 'FIVE_V_FIVE',
  divisionName: '',
  registrationMode: 'TEAM',
  formatType: 'REGULAR_ROUND_ROBIN',
  formatTemplateId: undefined,
  maxTeamCount: 16,
  registrationStartTime: '',
  registrationEndTime: '',
  matchStartTime: '',
  matchEndTime: '',
  sortOrder: 0
})

const divisionRules = {
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  gameMode: [{ required: true, message: '请选择比赛模式', trigger: 'change' }],
  divisionName: [{ required: true, message: '请输入赛区名称', trigger: 'blur' }],
  registrationMode: [{ required: true, message: '请选择报名方式', trigger: 'change' }],
  formatTemplateId: [{ required: true, message: '请选择赛制模板', trigger: 'change' }],
  maxTeamCount: [{ required: true, message: '请输入队伍上限', trigger: 'blur' }]
}

const seasonDetailOpen = ref(false)
const seasonDetailTab = ref('basic')
const seasonDetailTitle = ref('')
const seasonDetailLoading = ref(false)
const seasonDetail = ref<BasketSeasonDetailVo>()

async function loadFormatTemplates(seriesId?: number) {
  if (!seriesId) {
    formatTemplateList.value = []
    return
  }
  const res = await listV4FormatTemplates(seriesId)
  formatTemplateList.value = res.data || []
  if (!divisionForm.formatTemplateId && formatTemplateList.value.length) {
    const def = formatTemplateList.value.find((t) => t.isDefault === 1) || formatTemplateList.value[0]
    divisionForm.formatTemplateId = def.id
  }
}

async function loadSeasonDetailSnapshot(seasonDetailVo?: BasketSeasonDetailVo) {
  seasonDetailSnapshot.value = undefined
  const firstDivision = seasonDetailVo?.divisions?.[0]
  if (firstDivision?.id) {
    const res = await getV4DivisionFormatSnapshot(firstDivision.id)
    seasonDetailSnapshot.value = res.data
  } else if (formatTemplateList.value.length) {
    const tpl = formatTemplateList.value.find((t) => t.isDefault === 1) || formatTemplateList.value[0]
    if (tpl?.id) {
      const detail = await import('@/api/cnpa/event-v4/format-template').then((m) => m.getV4FormatTemplate(tpl.id!))
      seasonDetailSnapshot.value = {
        sourceTemplateName: tpl.templateName,
        sourceTemplateVersion: tpl.versionNo,
        snapshotStatus: 'DRAFT',
        snapshotStatusName: '草稿（模板预览）',
        stages: detail.data?.stages
      }
    }
  }
}

function resetSeasonForm() {
  Object.assign(seasonForm, {
    id: undefined,
    seriesId: selectedSeriesId.value,
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
}

function applySeasonFormRow(row: BasketSeasonListVo) {
  Object.assign(seasonForm, {
    id: row.id,
    seriesId: row.seriesId,
    seasonYear: row.seasonYear,
    seasonName: row.seasonName,
    displayTitle: row.displayTitle,
    description: row.description,
    coverFileId: row.coverFileId,
    bannerFileId: row.bannerFileId,
    seasonStartTime: row.seasonStartTime || '',
    seasonEndTime: row.seasonEndTime || ''
  })
  coverPreview.value = row.coverUrl || ''
  bannerPreview.value = row.bannerUrl || ''
  seasonPeriodRange.value = row.seasonStartTime && row.seasonEndTime
    ? [row.seasonStartTime, row.seasonEndTime]
    : null
}

async function loadSeries() {
  const res = await listV4Series({ enabled: 1 })
  seriesList.value = res.data || []
  if (!selectedSeriesId.value && seriesList.value.length) {
    selectedSeriesId.value = seriesList.value[0].id
    seasonQuery.seriesId = selectedSeriesId.value
    loadSeasonList()
  }
}

function handleSeriesChange(seriesId: number) {
  seasonQuery.seriesId = seriesId
  seasonQuery.seasonYear = undefined
  seasonQuery.seasonName = ''
  seasonQuery.publishStatus = ''
  seasonQuery.pageNum = 1
  selectedSeason.value = undefined
  divisionList.value = []
  loadSeasonList()
}

async function loadSeasonList() {
  seasonLoading.value = true
  try {
    const res = await listV4Season(seasonQuery)
    seasonList.value = res.rows || []
    seasonTotal.value = res.total || 0
  } finally {
    seasonLoading.value = false
  }
}

function resetSeasonQuery() {
  seasonQuery.seasonYear = undefined
  seasonQuery.seasonName = ''
  seasonQuery.publishStatus = ''
  seasonQuery.pageNum = 1
  loadSeasonList()
}

function handleCreateEvent() {
  if (!selectedSeriesId.value) {
    proxy!.$modal.msgWarning('请先选择赛事系列')
    return
  }
  wizardSeasonId.value = undefined
  wizardOpen.value = true
}

function handleContinuePublish(row: BasketSeasonListVo) {
  wizardSeasonId.value = row.id
  wizardOpen.value = true
}

function handleWizardSuccess() {
  loadSeasonList()
  if (selectedSeason.value?.id === wizardSeasonId.value) {
    loadDivisionList()
  }
}

function handleEditSeason(row: BasketSeasonListVo) {
  resetSeasonForm()
  applySeasonFormRow(row)
  seasonDialogTitle.value = '编辑赛季基础资料'
  seasonDialogOpen.value = true
}

function submitSeasonForm() {
  seasonFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    if (seasonForm.seasonStartTime && seasonForm.seasonEndTime
      && seasonForm.seasonStartTime >= seasonForm.seasonEndTime) {
      proxy!.$modal.msgError('赛季结束时间必须晚于赛季开始时间')
      return
    }
    const payload: BasketSeasonCreateReq = {
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
    if (seasonForm.id) {
      await updateV4Season(seasonForm.id, payload)
      proxy!.$modal.msgSuccess('更新成功')
    } else {
      await addV4Season(payload)
      proxy!.$modal.msgSuccess('创建成功')
    }
    seasonDialogOpen.value = false
    loadSeasonList()
  })
}

function handleSelectSeason(row: BasketSeasonListVo) {
  selectedSeason.value = row
  divisionQuery.seasonId = row.id
  divisionQuery.pageNum = 1
  loadDivisionList()
}

async function handleOpenSeasonDetail(row: BasketSeasonListVo) {
  seasonDetailTitle.value = `赛季详情 — ${row.displayTitle || row.seasonName}`
  seasonDetailTab.value = 'basic'
  seasonDetailOpen.value = true
  seasonDetailLoading.value = true
  try {
    await loadFormatTemplates(row.seriesId)
    const res = await getV4Season(row.id!)
    seasonDetail.value = res.data
    rulesDivisionId.value = res.data?.divisions?.[0]?.id
    await loadSeasonDetailSnapshot(res.data)
    if (rulesDivisionId.value) {
      await loadRulesSnapshot()
    }
  } finally {
    seasonDetailLoading.value = false
  }
}

async function loadRulesSnapshot() {
  if (!rulesDivisionId.value) {
    rulesSnapshot.value = undefined
    return
  }
  const res = await getFormatSnapshot(rulesDivisionId.value)
  rulesSnapshot.value = res.data
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

function openAddStage() {
  if (!rulesDivisionId.value) return
  stageEditingId.value = undefined
  stageDialogTitle.value = '新增阶段'
  resetStageForm()
  stageDialogOpen.value = true
}

function openEditStage(stage: BasketDivisionStageVo) {
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
  if (!rulesDivisionId.value) return
  await stageFormRef.value?.validate()
  const payload: BasketDivisionStageSaveReq = { ...stageForm }
  delete (payload as any).tieBreakRules
  if (stageEditingId.value) {
    await updateFormatStage(rulesDivisionId.value, stageEditingId.value, payload)
    if (stageForm.competitionFormat === 'ROUND_ROBIN' && stageEditingId.value) {
      await saveTieBreakRules(rulesDivisionId.value, {
        stageId: stageEditingId.value,
        rules: stageForm.tieBreakRules
      })
    }
    proxy!.$modal.msgSuccess('更新成功')
  } else {
    const res = await addFormatStage(rulesDivisionId.value, payload)
    const newStageId = res.data
    if (stageForm.competitionFormat === 'ROUND_ROBIN' && newStageId) {
      await saveTieBreakRules(rulesDivisionId.value, { stageId: newStageId, rules: stageForm.tieBreakRules })
    }
    proxy!.$modal.msgSuccess('新增成功')
  }
  stageDialogOpen.value = false
  await loadRulesSnapshot()
}

async function handleDeleteStage(stage: BasketDivisionStageVo) {
  if (!rulesDivisionId.value || !stage.id) return
  await proxy!.$modal.confirm(`确认删除阶段「${stage.stageName}」？`)
  await deleteFormatStage(rulesDivisionId.value, stage.id)
  proxy!.$modal.msgSuccess('删除成功')
  await loadRulesSnapshot()
}

async function moveStage(stage: BasketDivisionStageVo, delta: number) {
  if (!rulesDivisionId.value || !stage.id) return
  const stages = sortedRuleStages.value
  const index = stages.findIndex((s) => s.id === stage.id)
  const target = index + delta
  if (target < 0 || target >= stages.length) return
  const ids = stages.map((s) => s.id!)
  ;[ids[index], ids[target]] = [ids[target], ids[index]]
  await reorderFormatStages(rulesDivisionId.value, ids)
  await loadRulesSnapshot()
}

async function openRebuildSnapshot() {
  if (!rulesDivisionId.value) return
  const division = seasonDetail.value?.divisions?.find((d) => d.id === rulesDivisionId.value)
  const templateId = division?.formatTemplateId || formatTemplateList.value[0]?.id
  if (!templateId) {
    proxy!.$modal.msgWarning('请先选择赛制模板')
    return
  }
  await proxy!.$modal.confirm('重建将保留历史快照版本，当前草稿规则将被替换。是否继续？')
  await rebuildFormatSnapshot(rulesDivisionId.value, templateId)
  proxy!.$modal.msgSuccess('重建成功')
  await loadRulesSnapshot()
}

async function handleLockSnapshot() {
  if (!rulesDivisionId.value) return
  await proxy!.$modal.confirm('锁定后规则不可再编辑，开放报名前需锁定。是否继续？')
  await lockFormatSnapshot(rulesDivisionId.value)
  proxy!.$modal.msgSuccess('锁定成功')
  await loadRulesSnapshot()
}

function handleSeasonSelect(row: BasketSeasonListVo | undefined) {
  if (row) handleSelectSeason(row)
}

async function handlePublishSeason(row: BasketSeasonListVo) {
  const hasDivision = (row.divisionCount ?? 0) > 0
  const tip = hasDivision
    ? '确认发布该赛季？前台将根据已创建的平台赛区展示内容。'
    : '当前尚无平台赛区，发布后前台仅展示赛季预告，不展示报名入口。是否继续？'
  await proxy!.$modal.confirm(tip)
  await publishV4Season(row.id!)
  proxy!.$modal.msgSuccess('发布成功')
  loadSeasonList()
}

async function handleOfflineSeason(row: BasketSeasonListVo) {
  await proxy!.$modal.confirm('确认下线该赛季？下线后前台不再展示。')
  await offlineV4Season(row.id!)
  proxy!.$modal.msgSuccess('下线成功')
  loadSeasonList()
}

async function handleDeleteSeason(row: BasketSeasonListVo) {
  await proxy!.$modal.confirm('确认删除该草稿赛季？')
  await delV4Season(row.id!)
  proxy!.$modal.msgSuccess('删除成功')
  if (selectedSeason.value?.id === row.id) {
    selectedSeason.value = undefined
    divisionList.value = []
  }
  loadSeasonList()
}

async function loadDivisionList() {
  if (!divisionQuery.seasonId) return
  divisionLoading.value = true
  try {
    const res = await listV4Division(divisionQuery)
    divisionList.value = res.rows || []
    divisionTotal.value = res.total || 0
  } finally {
    divisionLoading.value = false
  }
}

function resetDivisionForm() {
  Object.assign(divisionForm, {
    id: undefined,
    seasonId: selectedSeason.value?.id,
    platformCode: 'PS_XBOX',
    gameMode: 'FIVE_V_FIVE',
    divisionName: '',
    registrationMode: 'TEAM',
    formatType: 'REGULAR_ROUND_ROBIN',
    formatTemplateId: undefined,
    maxTeamCount: 16,
    registrationStartTime: '',
    registrationEndTime: '',
    matchStartTime: '',
    matchEndTime: '',
    sortOrder: 0
  })
  loadFormatTemplates(selectedSeriesId.value)
}

function handleAddDivision() {
  if (!selectedSeason.value) {
    proxy!.$modal.msgWarning('请先选择一个赛季')
    return
  }
  resetDivisionForm()
  divisionDialogTitle.value = '新建赛区'
  divisionDialogOpen.value = true
}

function handleEditDivision(row: BasketDivisionListVo) {
  Object.assign(divisionForm, row)
  divisionForm.id = row.id
  divisionDialogTitle.value = '编辑赛区'
  divisionDialogOpen.value = true
}

function submitDivisionForm() {
  divisionFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    if (divisionForm.id) {
      await updateV4Division(divisionForm.id, divisionForm)
      proxy!.$modal.msgSuccess('更新成功')
    } else {
      await addV4Division(divisionForm)
      proxy!.$modal.msgSuccess('创建成功')
    }
    divisionDialogOpen.value = false
    loadDivisionList()
    loadSeasonList()
  })
}

async function handleOpenDivision(row: BasketDivisionListVo) {
  await openV4DivisionRegistration(row.id!)
  proxy!.$modal.msgSuccess('已开放报名')
  loadDivisionList()
}

async function handleCloseDivision(row: BasketDivisionListVo) {
  await closeV4DivisionRegistration(row.id!)
  proxy!.$modal.msgSuccess('已关闭报名')
  loadDivisionList()
}

async function handleDeleteDivision(row: BasketDivisionListVo) {
  await proxy!.$modal.confirm('确认删除该草稿赛区？')
  await delV4Division(row.id!)
  proxy!.$modal.msgSuccess('删除成功')
  loadDivisionList()
  loadSeasonList()
}

onMounted(() => {
  loadSeries()
})
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.season-query-bar {
  margin-bottom: 16px;
}
.season-query-form {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.season-query-form :deep(.el-form-item) {
  margin-bottom: 0;
  margin-right: 0;
}
.season-query-form :deep(.query-year) {
  width: 120px;
}
.season-query-form :deep(.query-name) {
  width: 180px;
}
.season-query-form :deep(.query-status) {
  width: 140px;
}
.season-query-form :deep(.query-actions .el-form-item__content) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.mb8 {
  margin-bottom: 8px;
}
.nowrap-tag {
  white-space: nowrap;
}
.time-range {
  font-size: 13px;
  white-space: nowrap;
}
.table-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 8px;
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
.advancement-highlight {
  color: var(--el-color-primary);
  font-weight: 600;
}
.mr4 {
  margin-right: 4px;
}
.mt8 {
  margin-top: 8px;
}
.text-center {
  text-align: center;
}
.page-tip-alert :deep(.el-alert__content) {
  padding-top: 0;
}
.page-tip-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
  font-size: 13px;
}
.page-tip-list li + li {
  margin-top: 2px;
}
</style>
