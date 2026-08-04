<template>
  <div class="app-container">
    <el-alert
      title="新版赛事管理：赛事系列 → 赛季 → 平台赛区"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    >
      <template #default>
        <div>当前使用新版赛事结构：赛事系列 → 赛季 → 平台赛区。</div>
        <div>当前开放球队报名；个人报名暂未开放。</div>
        <div>当前开放常规赛单循环；小组赛/淘汰赛后续开放。</div>
        <div>多平台通过平台赛区分别管理。</div>
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
      <template #header>
        <div class="card-header">
          <span>赛季列表</span>
          <el-button type="primary" plain icon="Plus" @click="handleAddSeason" v-hasPermi="['basket:v4Season:add']" :disabled="!selectedSeriesId">新建赛季</el-button>
        </div>
      </template>

      <el-form :model="seasonQuery" :inline="true" class="mb8">
        <el-form-item label="年份">
          <el-input-number v-model="seasonQuery.seasonYear" :min="2020" :max="2099" controls-position="right" />
        </el-form-item>
        <el-form-item label="赛季名称">
          <el-input v-model="seasonQuery.seasonName" clearable placeholder="赛季名称" />
        </el-form-item>
        <el-form-item label="发布状态">
          <el-select v-model="seasonQuery.publishStatus" clearable placeholder="发布状态" style="width: 140px">
            <el-option v-for="item in PUBLISH_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadSeasonList">搜索</el-button>
          <el-button icon="Refresh" @click="resetSeasonQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="seasonLoading" :data="seasonList" border highlight-current-row @current-change="handleSeasonSelect">
        <el-table-column label="赛事系列" prop="seriesName" min-width="100" />
        <el-table-column label="年份" prop="seasonYear" width="80" />
        <el-table-column label="赛季名称" prop="seasonName" min-width="120" />
        <el-table-column label="前台标题" prop="displayTitle" min-width="140" />
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
        <el-table-column label="操作" width="300" fixed="right" class-name="small-padding fixed-width">
          <template #default="scope">
            <div class="table-actions">
              <el-button link type="primary" @click="handleOpenSeasonDetail(scope.row)">赛季详情</el-button>
              <el-button link type="primary" @click="handleEditSeason(scope.row)" v-hasPermi="['basket:v4Season:edit']">编辑</el-button>
              <el-button link type="primary" @click="handleSelectSeason(scope.row)">查看赛区</el-button>
              <el-button link type="success" @click="handlePublishSeason(scope.row)" v-hasPermi="['basket:v4Season:publish']" v-if="scope.row.publishStatus === 'DRAFT'">发布</el-button>
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
        <el-table-column label="平台" prop="platformCode" width="80" />
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

    <el-dialog :title="seasonDialogTitle" v-model="seasonDialogOpen" width="720px" append-to-body>
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
          <el-input v-model="seasonForm.seasonName" placeholder="如 S1" />
        </el-form-item>
        <el-form-item label="前台展示标题" prop="displayTitle">
          <el-input v-model="seasonForm.displayTitle" placeholder="如 CNPA League 2026 S1" />
        </el-form-item>
        <el-form-item label="赛事简介">
          <el-input v-model="seasonForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="封面图文件ID">
          <el-input-number v-model="seasonForm.coverFileId" :min="1" controls-position="right" style="width: 100%" placeholder="暂可填写文件ID，后续接上传组件" />
          <div class="el-form-item__tip">暂可填写文件ID，后续接上传组件</div>
        </el-form-item>
        <el-form-item label="Banner图文件ID">
          <el-input-number v-model="seasonForm.bannerFileId" :min="1" controls-position="right" style="width: 100%" />
          <div class="el-form-item__tip">暂可填写文件ID，后续接上传组件</div>
        </el-form-item>
        <el-form-item label="报名方式" prop="registrationMode">
          <el-radio-group v-model="seasonForm.registrationMode">
            <el-radio label="TEAM">球队报名</el-radio>
            <el-radio label="PERSONAL" disabled>个人报名（暂未开放）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="赛制" prop="formatType">
          <el-radio-group v-model="seasonForm.formatType">
            <el-radio label="REGULAR_ROUND_ROBIN">常规赛单循环</el-radio>
            <el-radio label="GROUP_KNOCKOUT" disabled>小组赛+淘汰赛（暂未开放）</el-radio>
            <el-radio label="KNOCKOUT" disabled>单败淘汰（暂未开放）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报名开始时间" prop="registrationStartTime">
          <el-date-picker v-model="seasonForm.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择报名开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报名结束时间" prop="registrationEndTime">
          <el-date-picker v-model="seasonForm.registrationEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择报名结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="比赛开始时间" prop="matchStartTime">
          <el-date-picker v-model="seasonForm.matchStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择比赛开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="比赛结束时间" prop="matchEndTime">
          <el-date-picker v-model="seasonForm.matchEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择比赛结束时间" style="width: 100%" />
        </el-form-item>
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
            <el-descriptions-item label="报名方式">{{ v4RegistrationModeLabel(seasonDetail.registrationMode) }}</el-descriptions-item>
            <el-descriptions-item label="基础赛制">{{ v4FormatTypeLabel(seasonDetail.formatType) }}</el-descriptions-item>
            <el-descriptions-item label="报名时间" :span="2">{{ formatV4CompactRange(seasonDetail.registrationStartTime, seasonDetail.registrationEndTime) }}</el-descriptions-item>
            <el-descriptions-item label="比赛时间" :span="2">{{ formatV4CompactRange(seasonDetail.matchStartTime, seasonDetail.matchEndTime) }}</el-descriptions-item>
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
          <el-alert type="info" :closable="false" show-icon class="mb8" title="League 动态竞赛流程（常规赛单循环 → 积分排名 → 季后赛）" />
          <el-descriptions :column="1" border>
            <el-descriptions-item label="常规赛">单循环（{{ seasonDetail?.formatType === 'REGULAR_ROUND_ROBIN' ? '已启用' : '未启用' }}）</el-descriptions-item>
            <el-descriptions-item label="积分规则">胜/负/弃权积分、同分排序 — 后续开放配置</el-descriptions-item>
            <el-descriptions-item label="季后赛">开启后排名前 N 晋级 — 后续开放配置</el-descriptions-item>
            <el-descriptions-item label="对阵规则">单败淘汰 — 后续开放配置</el-descriptions-item>
            <el-descriptions-item label="系列赛 BO">普通轮次 BO3、总决赛 BO5 — 后续开放配置</el-descriptions-item>
            <el-descriptions-item label="三四名赛">后续开放配置</el-descriptions-item>
            <el-descriptions-item label="规则快照">Division 发布时继承赛季规则并固化，避免模板变更影响进行中赛事</el-descriptions-item>
          </el-descriptions>
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
            <el-descriptions-item label="操作提示">发布前请确认至少创建一个平台赛区；详细规则可在「赛制规则」页签查看，完整参数配置后续开放。</el-descriptions-item>
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
            <el-option label="PS" value="PS" />
            <el-option label="XBOX" value="XBOX" />
          </el-select>
        </el-form-item>
        <el-form-item label="赛区名称" prop="divisionName">
          <el-input v-model="divisionForm.divisionName" placeholder="如 League 2026 S1 - PS" />
        </el-form-item>
        <el-form-item label="报名方式" prop="registrationMode">
          <el-radio-group v-model="divisionForm.registrationMode">
            <el-radio label="TEAM">球队报名</el-radio>
            <el-radio label="PERSONAL" disabled>个人报名（暂未开放）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="赛制" prop="formatType">
          <el-radio-group v-model="divisionForm.formatType">
            <el-radio label="REGULAR_ROUND_ROBIN">常规赛单循环</el-radio>
            <el-radio label="KNOCKOUT" disabled>单淘汰（暂未开放）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="队伍上限" prop="maxTeamCount">
          <el-input-number v-model="divisionForm.maxTeamCount" :min="1" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="阵容最少人数" prop="lineupMinCount">
          <el-input-number v-model="divisionForm.lineupMinCount" :min="5" :max="8" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="阵容最多人数" prop="lineupMaxCount">
          <el-input-number v-model="divisionForm.lineupMaxCount" :min="5" :max="8" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="报名开始">
          <el-date-picker v-model="divisionForm.registrationStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
          <div class="el-form-item__tip">留空则继承 Season 时间</div>
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
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, onMounted, reactive, ref } from 'vue'
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
  V4_PUBLISH_STATUS_FILTER_OPTIONS,
  formatV4CompactRange,
  v4DivisionStatusLabel,
  v4DivisionStatusTagType,
  v4FormatTypeLabel,
  v4FormatTypeTagType,
  v4PublishStatusLabel,
  v4PublishStatusTagType,
  v4RegistrationModeLabel,
  v4RegistrationModeTagType,
  v4SeriesDisplayLabel,
  v4SeasonStatusLabel,
  v4SeasonStatusTagType
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

const seasonDialogOpen = ref(false)
const seasonDialogTitle = ref('')
const seasonFormRef = ref()
const seasonForm = reactive<BasketSeasonCreateReq & { id?: number }>({
  seriesId: undefined,
  seasonYear: new Date().getFullYear(),
  seasonName: '',
  displayTitle: '',
  description: '',
  coverFileId: undefined,
  bannerFileId: undefined,
  registrationMode: 'TEAM',
  formatType: 'REGULAR_ROUND_ROBIN',
  registrationStartTime: '',
  registrationEndTime: '',
  matchStartTime: '',
  matchEndTime: ''
})

const seasonRules = {
  seriesId: [{ required: true, message: '请选择赛事系列', trigger: 'change' }],
  seasonYear: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  seasonName: [{ required: true, message: '请输入赛季名称', trigger: 'blur' }],
  displayTitle: [{ required: true, message: '请输入前台展示标题', trigger: 'blur' }],
  registrationMode: [{ required: true, message: '请选择报名方式', trigger: 'change' }],
  formatType: [{ required: true, message: '请选择赛制', trigger: 'change' }],
  registrationStartTime: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEndTime: [{ required: true, message: '请选择报名结束时间', trigger: 'change' }],
  matchStartTime: [{ required: true, message: '请选择比赛开始时间', trigger: 'change' }],
  matchEndTime: [{ required: true, message: '请选择比赛结束时间', trigger: 'change' }]
}

const divisionDialogOpen = ref(false)
const divisionDialogTitle = ref('')
const divisionFormRef = ref()
const divisionForm = reactive<BasketDivisionCreateReq & { id?: number }>({
  seasonId: undefined,
  platformCode: 'PS',
  divisionName: '',
  registrationMode: 'TEAM',
  formatType: 'REGULAR_ROUND_ROBIN',
  maxTeamCount: 16,
  lineupMinCount: 5,
  lineupMaxCount: 8,
  registrationStartTime: '',
  registrationEndTime: '',
  matchStartTime: '',
  matchEndTime: '',
  sortOrder: 0
})

const divisionRules = {
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  divisionName: [{ required: true, message: '请输入赛区名称', trigger: 'blur' }],
  maxTeamCount: [{ required: true, message: '请输入队伍上限', trigger: 'blur' }],
  lineupMinCount: [{ required: true, message: '请输入阵容最少人数', trigger: 'blur' }],
  lineupMaxCount: [{ required: true, message: '请输入阵容最多人数', trigger: 'blur' }]
}

const seasonDetailOpen = ref(false)
const seasonDetailTab = ref('basic')
const seasonDetailTitle = ref('')
const seasonDetailLoading = ref(false)
const seasonDetail = ref<BasketSeasonDetailVo>()

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
    registrationMode: 'TEAM',
    formatType: 'REGULAR_ROUND_ROBIN',
    registrationStartTime: '',
    registrationEndTime: '',
    matchStartTime: '',
    matchEndTime: ''
  })
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

function handleAddSeason() {
  resetSeasonForm()
  seasonDialogTitle.value = '新建赛季'
  seasonDialogOpen.value = true
}

function handleEditSeason(row: BasketSeasonListVo) {
  Object.assign(seasonForm, row)
  seasonForm.id = row.id
  seasonDialogTitle.value = '编辑赛季'
  seasonDialogOpen.value = true
}

function submitSeasonForm() {
  seasonFormRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    if (seasonForm.id) {
      await updateV4Season(seasonForm.id, seasonForm)
      proxy!.$modal.msgSuccess('更新成功')
    } else {
      await addV4Season(seasonForm)
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
    const res = await getV4Season(row.id!)
    seasonDetail.value = res.data
  } finally {
    seasonDetailLoading.value = false
  }
}

function handleSeasonSelect(row: BasketSeasonListVo | undefined) {
  if (row) handleSelectSeason(row)
}

async function handlePublishSeason(row: BasketSeasonListVo) {
  await proxy!.$modal.confirm('发布前请确认已创建至少一个平台赛区，且基础信息完整。是否继续？')
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
    platformCode: 'PS',
    divisionName: '',
    registrationMode: 'TEAM',
    formatType: 'REGULAR_ROUND_ROBIN',
    maxTeamCount: 16,
    lineupMinCount: 5,
    lineupMaxCount: 8,
    registrationStartTime: '',
    registrationEndTime: '',
    matchStartTime: '',
    matchEndTime: '',
    sortOrder: 0
  })
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
    if (divisionForm.lineupMinCount! > divisionForm.lineupMaxCount!) {
      proxy!.$modal.msgError('阵容最少人数不能大于最多人数')
      return
    }
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
</style>
