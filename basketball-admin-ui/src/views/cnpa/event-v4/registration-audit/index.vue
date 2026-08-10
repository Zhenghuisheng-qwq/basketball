<template>
  <div class="app-container">
    <el-alert title="报名审核：球队审核通过后生成参赛快照；个人报名审核通过后进入线下抽签组队" type="info" :closable="false" show-icon class="mb8" />

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 球队报名 -->
      <el-tab-pane label="球队报名" name="team">
        <el-form :model="queryParams" :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="queryParams.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="queryParams.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!queryParams.seasonId">
              <el-option v-for="item in divisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.registrationStatus" clearable style="width: 160px">
              <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="球队名称">
            <el-input v-model="queryParams.teamName" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getList">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="loading" :data="dataList" border>
          <el-table-column label="报名ID" prop="registrationId" width="90" />
          <el-table-column label="赛季" prop="seasonTitle" min-width="140" />
          <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="球队" prop="teamName" min-width="120" />
          <el-table-column label="队长" prop="captainPlayerName" min-width="100" />
          <el-table-column label="阵容人数" prop="lineupCount" width="90" align="center" />
          <el-table-column label="状态" prop="registrationStatus" width="110" align="center">
            <template #default="scope">
              <el-tag :type="v4RegistrationStatusTagType(scope.row.registrationStatus)" size="small" class="nowrap-tag">
                {{ v4RegistrationStatusLabel(scope.row.registrationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submitTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row)" v-hasPermi="['basket:v4RegistrationAudit:query']">详情</el-button>
              <el-button link type="success" @click="handleApprove(scope.row)" v-hasPermi="['basket:v4RegistrationAudit:approve']" v-if="scope.row.registrationStatus === 'PENDING_REVIEW'">通过</el-button>
              <el-button link type="danger" @click="openReject(scope.row)" v-hasPermi="['basket:v4RegistrationAudit:reject']" v-if="scope.row.registrationStatus === 'PENDING_REVIEW'">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </el-tab-pane>

      <!-- 个人报名 -->
      <el-tab-pane label="个人报名" name="personal">
        <el-form :model="personalQuery" :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="personalQuery.seasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onPersonalSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="personalQuery.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!personalQuery.seasonId">
              <el-option v-for="item in personalDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="personalQuery.registrationStatus" clearable style="width: 160px">
              <el-option v-for="item in PERSONAL_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="球员姓名">
            <el-input v-model="personalQuery.playerName" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getPersonalList">搜索</el-button>
            <el-button icon="Refresh" @click="resetPersonalQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="personalLoading" :data="personalList" border>
          <el-table-column label="报名ID" prop="personalRegistrationId" width="90" />
          <el-table-column label="赛季" prop="seasonTitle" min-width="140" />
          <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
          <el-table-column label="平台" prop="platformCode" width="80" />
          <el-table-column label="球员" prop="playerName" min-width="120" />
          <el-table-column label="实际平台" prop="actualPlatformCode" width="90" />
          <el-table-column label="意向位置" prop="preferredPosition" width="90" />
          <el-table-column label="状态" prop="registrationStatus" width="110" align="center">
            <template #default="scope">
              <el-tag :type="v4PersonalRegistrationStatusTagType(scope.row.registrationStatus)" size="small" class="nowrap-tag">
                {{ v4PersonalRegistrationStatusLabel(scope.row.registrationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submitTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openPersonalDetail(scope.row)" v-hasPermi="['basket:v4PersonalReg:list']">详情</el-button>
              <el-button link type="success" @click="handlePersonalApprove(scope.row)" v-hasPermi="['basket:v4PersonalReg:audit']" v-if="scope.row.registrationStatus === 'PENDING_REVIEW'">通过</el-button>
              <el-button link type="danger" @click="openPersonalReject(scope.row)" v-hasPermi="['basket:v4PersonalReg:audit']" v-if="scope.row.registrationStatus === 'PENDING_REVIEW'">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="personalTotal" v-model:page="personalQuery.pageNum" v-model:limit="personalQuery.pageSize" @pagination="getPersonalList" />
      </el-tab-pane>

      <!-- 个人组队结果 -->
      <el-tab-pane label="个人组队结果" name="formation">
        <el-form :model="drawQuery" :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="drawSeasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onDrawSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="drawQuery.divisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!drawSeasonId">
              <el-option v-for="item in drawDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="批次状态">
            <el-select v-model="drawQuery.batchStatus" clearable style="width: 140px">
              <el-option v-for="item in DRAW_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="getDrawList">搜索</el-button>
            <el-button icon="Refresh" @click="resetDrawQuery">重置</el-button>
            <el-button type="primary" plain icon="Plus" @click="openCreateDrawBatch" v-hasPermi="['basket:v4PersonalFormation:edit']" :disabled="!drawQuery.divisionId">新建批次</el-button>
          </el-form-item>
        </el-form>

        <el-table v-loading="drawLoading" :data="drawList" border>
          <el-table-column label="批次ID" prop="drawBatchId" width="80" />
          <el-table-column label="平台赛区" prop="divisionName" min-width="120" />
          <el-table-column label="批次编码" prop="batchCode" min-width="120" />
          <el-table-column label="批次名称" prop="batchName" min-width="140" />
          <el-table-column label="版本" prop="batchVersion" width="70" align="center" />
          <el-table-column label="状态" prop="batchStatus" width="100" align="center">
            <template #default="scope">
              <el-tag :type="drawBatchTagType(scope.row.batchStatus)" size="small" class="nowrap-tag">
                {{ v4DrawBatchStatusLabel(scope.row.batchStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="确认时间" prop="confirmedTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.confirmedTime) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="160">
            <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openDrawDetail(scope.row)" v-hasPermi="['basket:v4PersonalFormation:list']">详情</el-button>
              <el-button link type="danger" @click="handleCancelDrawBatch(scope.row)" v-hasPermi="['basket:v4PersonalFormation:edit']" v-if="scope.row.batchStatus === 'DRAFT'">取消</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination :total="drawTotal" v-model:page="drawQuery.pageNum" v-model:limit="drawQuery.pageSize" @pagination="getDrawList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 球队报名详情 -->
    <el-drawer v-model="detailOpen" title="球队报名审核详情" size="760px" append-to-body>
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报名ID">{{ detail.registrationId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="v4RegistrationStatusTagType(detail.registrationStatus)" size="small">{{ v4RegistrationStatusLabel(detail.registrationStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="赛事">{{ detail.seasonTitle }}</el-descriptions-item>
          <el-descriptions-item label="平台赛区">{{ detail.divisionName }}（{{ detail.platformCode }}）</el-descriptions-item>
          <el-descriptions-item label="球队">{{ detail.teamName }}</el-descriptions-item>
          <el-descriptions-item label="队长">{{ detail.captainPlayerName }}</el-descriptions-item>
          <el-descriptions-item label="阵容人数">{{ detail.lineupCount }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(detail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">{{ detail.rejectReason }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">阵容列表</el-divider>
        <el-table :data="detail.players || []" size="small" border>
          <el-table-column label="球员" prop="playerName" />
          <el-table-column label="Game ID" prop="gameIdSnapshot" />
          <el-table-column label="位置" prop="position" />
          <el-table-column label="队长" width="70">
            <template #default="scope">{{ scope.row.isCaptain === 1 ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <el-divider content-position="left">审核流水</el-divider>
        <el-table :data="detail.audits || []" size="small" border>
          <el-table-column label="动作" width="100">
            <template #default="scope">{{ v4AuditActionLabel(scope.row.auditAction) }}</template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="scope">{{ v4AuditResultLabel(scope.row.auditResult) }}</template>
          </el-table-column>
          <el-table-column label="前状态" width="110">
            <template #default="scope">{{ v4RegistrationStatusLabel(scope.row.beforeStatus) }}</template>
          </el-table-column>
          <el-table-column label="后状态" width="110">
            <template #default="scope">{{ v4RegistrationStatusLabel(scope.row.afterStatus) }}</template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" prop="auditRemark" min-width="140" />
        </el-table>
      </template>
    </el-drawer>

    <!-- 个人报名详情 -->
    <el-drawer v-model="personalDetailOpen" title="个人报名审核详情" size="680px" append-to-body>
      <template v-if="personalDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="报名ID">{{ personalDetail.personalRegistrationId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="v4PersonalRegistrationStatusTagType(personalDetail.registrationStatus)" size="small">
              {{ v4PersonalRegistrationStatusLabel(personalDetail.registrationStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="赛事">{{ personalDetail.seasonTitle }}</el-descriptions-item>
          <el-descriptions-item label="平台赛区">{{ personalDetail.divisionName }}（{{ personalDetail.platformCode }}）</el-descriptions-item>
          <el-descriptions-item label="球员">{{ personalDetail.playerName }}</el-descriptions-item>
          <el-descriptions-item label="Game 账号">{{ personalDetail.gameAccountId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际平台">{{ personalDetail.actualPlatformCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="意向位置">{{ personalDetail.preferredPosition || '-' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ parseTime(personalDetail.submitTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核通过时间">{{ parseTime(personalDetail.approvedTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="personalDetail.rejectReason" label="驳回原因" :span="2">{{ personalDetail.rejectReason }}</el-descriptions-item>
        </el-descriptions>
        <el-divider content-position="left">审核流水</el-divider>
        <el-table :data="personalDetail.audits || []" size="small" border>
          <el-table-column label="动作" width="100">
            <template #default="scope">{{ v4AuditActionLabel(scope.row.auditAction) }}</template>
          </el-table-column>
          <el-table-column label="结果" width="100">
            <template #default="scope">{{ v4AuditResultLabel(scope.row.auditResult) }}</template>
          </el-table-column>
          <el-table-column label="前状态" width="110">
            <template #default="scope">{{ v4PersonalRegistrationStatusLabel(scope.row.beforeStatus) }}</template>
          </el-table-column>
          <el-table-column label="后状态" width="110">
            <template #default="scope">{{ v4PersonalRegistrationStatusLabel(scope.row.afterStatus) }}</template>
          </el-table-column>
          <el-table-column label="审核人" prop="auditUserName" width="100" />
          <el-table-column label="时间" width="160">
            <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
          </el-table-column>
          <el-table-column label="备注" prop="auditRemark" min-width="120" />
        </el-table>
      </template>
    </el-drawer>

    <!-- 个人组队批次详情 -->
    <el-drawer v-model="drawDetailOpen" title="个人组队批次详情" size="900px" append-to-body @closed="drawDetail = undefined">
      <template v-if="drawDetail">
        <el-descriptions :column="3" border class="mb8">
          <el-descriptions-item label="批次ID">{{ drawDetail.drawBatchId }}</el-descriptions-item>
          <el-descriptions-item label="赛区">{{ drawDetail.divisionName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="drawBatchTagType(drawDetail.batchStatus)" size="small">{{ v4DrawBatchStatusLabel(drawDetail.batchStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="编码">{{ drawDetail.batchCode }}</el-descriptions-item>
          <el-descriptions-item label="名称" :span="2">{{ drawDetail.batchName }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="3">{{ drawDetail.resultRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="drawer-toolbar mb8" v-if="drawDetail.batchStatus === 'DRAFT'">
          <el-button type="primary" plain icon="Plus" @click="openCreateTeam" v-hasPermi="['basket:v4PersonalFormation:edit']">新建临时队</el-button>
          <el-button type="success" @click="openConfirmDrawBatch" v-hasPermi="['basket:v4PersonalFormation:edit']">确认批次</el-button>
        </div>

        <el-table :data="drawDetail.teams || []" border size="small">
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-block">
                <el-table :data="scope.row.assignments || []" size="small" border>
                  <el-table-column label="球员" prop="playerName" />
                  <el-table-column label="角色" width="80">
                    <template #default="s">{{ s.row.assignmentRole === 'CAPTAIN' ? '队长' : '队员' }}</template>
                  </el-table-column>
                  <el-table-column label="加入状态" prop="joinStatus" width="120" />
                  <el-table-column label="操作" width="80" v-if="drawDetail.batchStatus === 'DRAFT'">
                    <template #default="s">
                      <el-button link type="danger" @click="handleRemoveAssignment(scope.row, s.row)" v-hasPermi="['basket:v4PersonalFormation:edit']">移除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="临时队编码" prop="temporaryTeamCode" width="120" />
          <el-table-column label="临时队名称" prop="temporaryTeamName" min-width="140" />
          <el-table-column label="队长" prop="captainPlayerName" width="100" />
          <el-table-column label="人数" width="80" align="center">
            <template #default="scope">{{ scope.row.actualMemberCount ?? 0 }}/{{ scope.row.expectedMemberCount ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="组队状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="v4FormationStatusTagType(scope.row.formationStatus)" size="small">
                {{ v4FormationStatusLabel(scope.row.formationStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="绑定本届球队" prop="longTermTeamName" min-width="120" />
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="openAddAssignment(scope.row)" v-hasPermi="['basket:v4PersonalFormation:edit']" v-if="drawDetail.batchStatus === 'DRAFT'">添加成员</el-button>
              <el-button link type="primary" @click="openBindTeam(scope.row)" v-hasPermi="['basket:v4PersonalFormation:edit']" v-if="canBindTeam(scope.row)">绑定球队</el-button>
              <el-button link type="success" @click="handleConfirmFormation(scope.row)" v-hasPermi="['basket:v4PersonalFormation:confirm']" v-if="canConfirmFormation(scope.row)">确认组队</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <!-- 球队驳回 -->
    <el-dialog v-model="rejectOpen" title="驳回球队报名" width="480px" append-to-body>
      <el-form :model="rejectForm" label-width="90px">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectForm.rejectReason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 个人驳回 -->
    <el-dialog v-model="personalRejectOpen" title="驳回个人报名" width="480px" append-to-body>
      <el-form :model="personalRejectForm" label-width="90px">
        <el-form-item label="驳回原因" required>
          <el-input v-model="personalRejectForm.rejectReason" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="personalRejectOpen = false">取消</el-button>
        <el-button type="primary" @click="submitPersonalReject">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建抽签批次 -->
    <el-dialog v-model="createDrawOpen" title="新建个人组队批次" width="520px" append-to-body>
      <el-form :model="createDrawForm" label-width="100px">
        <el-form-item label="批次编码" required>
          <el-input v-model="createDrawForm.batchCode" placeholder="如 DRAW-2026-01" />
        </el-form-item>
        <el-form-item label="批次名称" required>
          <el-input v-model="createDrawForm.batchName" placeholder="如 第一轮抽签" />
        </el-form-item>
        <el-form-item label="结果来源">
          <el-select v-model="createDrawForm.resultSource" style="width: 100%">
            <el-option label="线下随机" value="OFFLINE_RANDOM" />
            <el-option label="人工调整" value="MANUAL_ADJUSTMENT" />
            <el-option label="导入" value="IMPORT" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createDrawForm.resultRemark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDrawOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCreateDrawBatch">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新建临时队 -->
    <el-dialog v-model="createTeamOpen" title="新建临时队伍" width="520px" append-to-body>
      <el-form :model="createTeamForm" label-width="110px">
        <el-form-item label="队伍编码" required>
          <el-input v-model="createTeamForm.temporaryTeamCode" />
        </el-form-item>
        <el-form-item label="队伍名称" required>
          <el-input v-model="createTeamForm.temporaryTeamName" />
        </el-form-item>
        <el-form-item label="预期人数">
          <el-input-number v-model="createTeamForm.expectedMemberCount" :min="1" :max="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createTeamOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCreateTeam">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加成员 -->
    <el-dialog v-model="addAssignmentOpen" title="添加队员" width="520px" append-to-body>
      <el-form :model="addAssignmentForm" label-width="110px">
        <el-form-item label="个人报名" required>
          <el-select v-model="addAssignmentForm.personalRegistrationId" filterable placeholder="选择已通过审核的报名" style="width: 100%">
            <el-option v-for="item in approvedPersonalOptions" :key="item.personalRegistrationId" :label="`${item.playerName} (#${item.personalRegistrationId})`" :value="item.personalRegistrationId!" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addAssignmentForm.assignmentRole" style="width: 100%">
            <el-option label="队员" value="MEMBER" />
            <el-option label="队长" value="CAPTAIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="addAssignmentForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addAssignmentOpen = false">取消</el-button>
        <el-button type="primary" @click="submitAddAssignment">确定</el-button>
      </template>
    </el-dialog>

    <!-- 确认批次 -->
    <el-dialog v-model="confirmDrawOpen" title="确认个人组队批次" width="520px" append-to-body>
      <el-form :model="confirmDrawForm" label-width="110px">
        <el-form-item label="未分配策略">
          <el-select v-model="confirmDrawForm.unassignedPolicy" style="width: 100%">
            <el-option label="留待下批" value="KEEP_FOR_NEXT" />
            <el-option label="取消资格" value="DISQUALIFY" />
            <el-option label="仅备注" value="REMARK" />
          </el-select>
        </el-form-item>
        <el-form-item label="未分配备注">
          <el-input v-model="confirmDrawForm.unassignedRemark" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="confirmDrawOpen = false">取消</el-button>
        <el-button type="primary" @click="submitConfirmDrawBatch">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定球队 -->
    <el-dialog v-model="bindTeamOpen" title="绑定本届赛事球队" width="520px" append-to-body>
      <el-form :model="bindTeamForm" label-width="100px">
        <el-form-item label="球队" required>
          <el-select v-model="bindTeamForm.longTermTeamId" filterable placeholder="选择球队" style="width: 100%">
            <el-option v-for="item in teamOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindTeamOpen = false">取消</el-button>
        <el-button type="primary" @click="submitBindTeam">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4RegistrationAudit">
import { onMounted, reactive, ref } from 'vue'
import {
  approveV4RegistrationAudit,
  getV4RegistrationAuditDetail,
  listV4RegistrationAudit,
  rejectV4RegistrationAudit,
  type BasketRegistrationAuditDetailVo,
  type BasketRegistrationAuditQuery
} from '@/api/cnpa/event-v4/registration-audit'
import {
  approveV4PersonalRegistration,
  getV4PersonalRegistrationDetail,
  listV4PersonalRegistration,
  rejectV4PersonalRegistration,
  type BasketPersonalRegistrationDetailVo,
  type BasketPersonalRegistrationListVo,
  type BasketPersonalRegistrationQuery
} from '@/api/cnpa/event-v4/personal-registration'
import {
  addV4PersonalDrawAssignment,
  bindV4PersonalFormationTeam,
  cancelV4PersonalDrawBatch,
  confirmV4PersonalDrawBatch,
  confirmV4PersonalFormation,
  createV4PersonalDrawBatch,
  createV4PersonalDrawTeam,
  getV4PersonalDrawBatchDetail,
  listV4PersonalDrawBatch,
  removeV4PersonalDrawAssignment,
  type BasketPersonalDrawBatchDetailVo,
  type BasketPersonalDrawBatchListVo,
  type BasketPersonalDrawBatchQuery,
  type BasketPersonalDrawTeamVo
} from '@/api/cnpa/event-v4/personal-draw'
import { listV4AdminDivisionOptions, listV4AdminSeasonOptions, listV4AdminTeamOptions, type V4AdminOptionVo } from '@/api/cnpa/event-v4/options'
import {
  V4_DRAW_BATCH_STATUS_FILTER_OPTIONS,
  V4_PERSONAL_REGISTRATION_STATUS_FILTER_OPTIONS,
  V4_REGISTRATION_STATUS_FILTER_OPTIONS,
  v4AuditActionLabel,
  v4AuditResultLabel,
  v4DrawBatchStatusLabel,
  v4FormationStatusLabel,
  v4FormationStatusTagType,
  v4PersonalRegistrationStatusLabel,
  v4PersonalRegistrationStatusTagType,
  v4RegistrationStatusLabel,
  v4RegistrationStatusTagType
} from '@/utils/v4AdminDisplay'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const activeTab = ref('team')

// ---- 球队报名 ----
const loading = ref(false)
const total = ref(0)
const dataList = ref<any[]>([])
const detailOpen = ref(false)
const rejectOpen = ref(false)
const detail = ref<BasketRegistrationAuditDetailVo>()
const currentId = ref<number>()
const rejectForm = reactive({ rejectReason: '' })
const seasonOptions = ref<V4AdminOptionVo[]>([])
const divisionOptions = ref<V4AdminOptionVo[]>([])
const queryParams = reactive<BasketRegistrationAuditQuery>({ pageNum: 1, pageSize: 10 })
const STATUS_OPTIONS = V4_REGISTRATION_STATUS_FILTER_OPTIONS

// ---- 个人报名 ----
const personalLoading = ref(false)
const personalTotal = ref(0)
const personalList = ref<BasketPersonalRegistrationListVo[]>([])
const personalDetailOpen = ref(false)
const personalRejectOpen = ref(false)
const personalDetail = ref<BasketPersonalRegistrationDetailVo>()
const personalCurrentId = ref<number>()
const personalRejectForm = reactive({ rejectReason: '' })
const personalDivisionOptions = ref<V4AdminOptionVo[]>([])
const personalQuery = reactive<BasketPersonalRegistrationQuery>({ pageNum: 1, pageSize: 10 })
const PERSONAL_STATUS_OPTIONS = V4_PERSONAL_REGISTRATION_STATUS_FILTER_OPTIONS

// ---- 个人组队 ----
const drawLoading = ref(false)
const drawTotal = ref(0)
const drawList = ref<BasketPersonalDrawBatchListVo[]>([])
const drawDetailOpen = ref(false)
const drawDetail = ref<BasketPersonalDrawBatchDetailVo>()
const drawSeasonId = ref<number>()
const drawDivisionOptions = ref<V4AdminOptionVo[]>([])
const drawQuery = reactive<BasketPersonalDrawBatchQuery>({ pageNum: 1, pageSize: 10 })
const DRAW_STATUS_OPTIONS = V4_DRAW_BATCH_STATUS_FILTER_OPTIONS
const createDrawOpen = ref(false)
const createTeamOpen = ref(false)
const addAssignmentOpen = ref(false)
const confirmDrawOpen = ref(false)
const bindTeamOpen = ref(false)
const currentDrawTeam = ref<BasketPersonalDrawTeamVo>()
const approvedPersonalOptions = ref<BasketPersonalRegistrationListVo[]>([])
const teamOptions = ref<V4AdminOptionVo[]>([])
const createDrawForm = reactive({ batchCode: '', batchName: '', resultSource: 'OFFLINE_RANDOM', resultRemark: '' })
const createTeamForm = reactive({ temporaryTeamCode: '', temporaryTeamName: '', expectedMemberCount: 5 })
const addAssignmentForm = reactive({ personalRegistrationId: undefined as number | undefined, assignmentRole: 'MEMBER', sortOrder: 0 })
const confirmDrawForm = reactive({ unassignedPolicy: 'KEEP_FOR_NEXT', unassignedRemark: '' })
const bindTeamForm = reactive({ longTermTeamId: undefined as number | undefined })

function formatDivisionLabel(item: V4AdminOptionVo) {
  return item.platformCode ? `${item.label}（${item.platformCode}）` : item.label
}

function drawBatchTagType(status?: string) {
  switch ((status || '').toUpperCase()) {
    case 'CONFIRMED':
    case 'CONVERTED': return 'success'
    case 'DRAFT': return 'warning'
    case 'CANCELLED': return 'info'
    default: return 'info'
  }
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadDivisions(seasonId?: number, target: 'team' | 'personal' | 'draw' = 'team') {
  const list = seasonId ? (await listV4AdminDivisionOptions(seasonId)).data || [] : []
  if (target === 'team') divisionOptions.value = list
  else if (target === 'personal') personalDivisionOptions.value = list
  else drawDivisionOptions.value = list
}

function onSeasonChange(seasonId?: number) {
  queryParams.divisionId = undefined
  loadDivisions(seasonId, 'team')
}

function onPersonalSeasonChange(seasonId?: number) {
  personalQuery.divisionId = undefined
  loadDivisions(seasonId, 'personal')
}

function onDrawSeasonChange(seasonId?: number) {
  drawQuery.divisionId = undefined
  loadDivisions(seasonId, 'draw')
}

function getList() {
  loading.value = true
  listV4RegistrationAudit(queryParams).then(res => {
    dataList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => { loading.value = false })
}

function resetQuery() {
  Object.assign(queryParams, { pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, registrationStatus: undefined, teamName: undefined })
  divisionOptions.value = []
  getList()
}

function openDetail(row: any) {
  getV4RegistrationAuditDetail(row.registrationId).then(res => {
    detail.value = res.data
    detailOpen.value = true
  })
}

function handleApprove(row: any) {
  proxy.$modal.confirm(`确认通过报名 #${row.registrationId}？`).then(() => approveV4RegistrationAudit(row.registrationId)).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getList()
  }).catch(() => {})
}

function openReject(row: any) {
  currentId.value = row.registrationId
  rejectForm.rejectReason = ''
  rejectOpen.value = true
}

function submitReject() {
  if (!rejectForm.rejectReason?.trim()) {
    proxy.$modal.msgError('驳回原因不能为空')
    return
  }
  rejectV4RegistrationAudit(currentId.value!, { rejectReason: rejectForm.rejectReason.trim() }).then(() => {
    proxy.$modal.msgSuccess('已驳回')
    rejectOpen.value = false
    getList()
  })
}

function getPersonalList() {
  personalLoading.value = true
  listV4PersonalRegistration(personalQuery).then(res => {
    personalList.value = res.rows || []
    personalTotal.value = res.total || 0
  }).finally(() => { personalLoading.value = false })
}

function resetPersonalQuery() {
  Object.assign(personalQuery, { pageNum: 1, pageSize: 10, seasonId: undefined, divisionId: undefined, registrationStatus: undefined, playerName: undefined })
  personalDivisionOptions.value = []
  getPersonalList()
}

function openPersonalDetail(row: BasketPersonalRegistrationListVo) {
  getV4PersonalRegistrationDetail(row.personalRegistrationId!).then(res => {
    personalDetail.value = res.data
    personalDetailOpen.value = true
  })
}

function handlePersonalApprove(row: BasketPersonalRegistrationListVo) {
  proxy.$modal.confirm(`确认通过个人报名 #${row.personalRegistrationId}？`).then(() => approveV4PersonalRegistration(row.personalRegistrationId!)).then(() => {
    proxy.$modal.msgSuccess('审核通过')
    getPersonalList()
  }).catch(() => {})
}

function openPersonalReject(row: BasketPersonalRegistrationListVo) {
  personalCurrentId.value = row.personalRegistrationId
  personalRejectForm.rejectReason = ''
  personalRejectOpen.value = true
}

function submitPersonalReject() {
  if (!personalRejectForm.rejectReason?.trim()) {
    proxy.$modal.msgError('驳回原因不能为空')
    return
  }
  rejectV4PersonalRegistration(personalCurrentId.value!, { rejectReason: personalRejectForm.rejectReason.trim() }).then(() => {
    proxy.$modal.msgSuccess('已驳回')
    personalRejectOpen.value = false
    getPersonalList()
  })
}

function getDrawList() {
  drawLoading.value = true
  listV4PersonalDrawBatch(drawQuery).then(res => {
    drawList.value = res.rows || []
    drawTotal.value = res.total || 0
  }).finally(() => { drawLoading.value = false })
}

function resetDrawQuery() {
  drawSeasonId.value = undefined
  Object.assign(drawQuery, { pageNum: 1, pageSize: 10, divisionId: undefined, batchStatus: undefined })
  drawDivisionOptions.value = []
  getDrawList()
}

function openCreateDrawBatch() {
  Object.assign(createDrawForm, { batchCode: '', batchName: '', resultSource: 'OFFLINE_RANDOM', resultRemark: '' })
  createDrawOpen.value = true
}

function submitCreateDrawBatch() {
  if (!createDrawForm.batchCode?.trim() || !createDrawForm.batchName?.trim()) {
    proxy.$modal.msgError('批次编码与名称不能为空')
    return
  }
  createV4PersonalDrawBatch({
    divisionId: drawQuery.divisionId,
    batchCode: createDrawForm.batchCode.trim(),
    batchName: createDrawForm.batchName.trim(),
    resultSource: createDrawForm.resultSource,
    resultRemark: createDrawForm.resultRemark
  }).then(res => {
    proxy.$modal.msgSuccess('批次已创建')
    createDrawOpen.value = false
    getDrawList()
    if (res.data) openDrawDetail({ drawBatchId: res.data as number })
  })
}

function openDrawDetail(row: BasketPersonalDrawBatchListVo | { drawBatchId: number }) {
  const id = 'drawBatchId' in row ? row.drawBatchId : (row as any).drawBatchId
  getV4PersonalDrawBatchDetail(id!).then(res => {
    drawDetail.value = res.data
    drawDetailOpen.value = true
  })
}

function refreshDrawDetail() {
  if (drawDetail.value?.drawBatchId) {
    getV4PersonalDrawBatchDetail(drawDetail.value.drawBatchId).then(res => {
      drawDetail.value = res.data
    })
  }
  getDrawList()
}

function handleCancelDrawBatch(row: BasketPersonalDrawBatchListVo) {
  proxy.$modal.confirm(`确认取消批次 #${row.drawBatchId}？`).then(() => cancelV4PersonalDrawBatch(row.drawBatchId!)).then(() => {
    proxy.$modal.msgSuccess('已取消')
    getDrawList()
  }).catch(() => {})
}

function openCreateTeam() {
  Object.assign(createTeamForm, { temporaryTeamCode: '', temporaryTeamName: '', expectedMemberCount: 5 })
  createTeamOpen.value = true
}

function submitCreateTeam() {
  if (!createTeamForm.temporaryTeamCode?.trim() || !createTeamForm.temporaryTeamName?.trim()) {
    proxy.$modal.msgError('队伍编码与名称不能为空')
    return
  }
  createV4PersonalDrawTeam(drawDetail.value!.drawBatchId!, {
    temporaryTeamCode: createTeamForm.temporaryTeamCode.trim(),
    temporaryTeamName: createTeamForm.temporaryTeamName.trim(),
    expectedMemberCount: createTeamForm.expectedMemberCount
  }).then(() => {
    proxy.$modal.msgSuccess('临时队伍已创建')
    createTeamOpen.value = false
    refreshDrawDetail()
  })
}

async function loadApprovedPersonalOptions() {
  if (!drawDetail.value?.divisionId) return
  const res = await listV4PersonalRegistration({
    divisionId: drawDetail.value.divisionId,
    registrationStatus: 'APPROVED',
    pageNum: 1,
    pageSize: 500
  })
  approvedPersonalOptions.value = res.rows || []
}

function openAddAssignment(team: BasketPersonalDrawTeamVo) {
  currentDrawTeam.value = team
  Object.assign(addAssignmentForm, { personalRegistrationId: undefined, assignmentRole: 'MEMBER', sortOrder: 0 })
  loadApprovedPersonalOptions()
  addAssignmentOpen.value = true
}

function submitAddAssignment() {
  if (!addAssignmentForm.personalRegistrationId) {
    proxy.$modal.msgError('请选择个人报名')
    return
  }
  addV4PersonalDrawAssignment(drawDetail.value!.drawBatchId!, currentDrawTeam.value!.drawTeamId!, {
    personalRegistrationId: addAssignmentForm.personalRegistrationId,
    assignmentRole: addAssignmentForm.assignmentRole,
    sortOrder: addAssignmentForm.sortOrder
  }).then(() => {
    proxy.$modal.msgSuccess('成员已添加')
    addAssignmentOpen.value = false
    refreshDrawDetail()
  })
}

function handleRemoveAssignment(team: BasketPersonalDrawTeamVo, assignment: any) {
  proxy.$modal.confirm('确认移除该成员？').then(() =>
    removeV4PersonalDrawAssignment(drawDetail.value!.drawBatchId!, assignment.assignmentId)
  ).then(() => {
    proxy.$modal.msgSuccess('已移除')
    refreshDrawDetail()
  }).catch(() => {})
}

function openConfirmDrawBatch() {
  Object.assign(confirmDrawForm, { unassignedPolicy: 'KEEP_FOR_NEXT', unassignedRemark: '' })
  confirmDrawOpen.value = true
}

function submitConfirmDrawBatch() {
  confirmV4PersonalDrawBatch(drawDetail.value!.drawBatchId!, confirmDrawForm).then(() => {
    proxy.$modal.msgSuccess('批次已确认')
    confirmDrawOpen.value = false
    refreshDrawDetail()
  })
}

function canBindTeam(team: BasketPersonalDrawTeamVo) {
  return drawDetail.value?.batchStatus === 'CONFIRMED' && !team.longTermTeamId
    && ['CAPTAIN_ASSIGNED', 'TEAM_FORMING', 'READY_TO_CONFIRM'].includes(team.formationStatus || '')
}

function canConfirmFormation(team: BasketPersonalDrawTeamVo) {
  return drawDetail.value?.batchStatus === 'CONFIRMED' && team.longTermTeamId
    && ['READY_TO_CONFIRM', 'TEAM_FORMING', 'CAPTAIN_ASSIGNED'].includes(team.formationStatus || '')
}

async function openBindTeam(team: BasketPersonalDrawTeamVo) {
  currentDrawTeam.value = team
  bindTeamForm.longTermTeamId = undefined
  if (drawDetail.value?.divisionId) {
    const res = await listV4AdminTeamOptions(drawDetail.value.divisionId)
    teamOptions.value = res.data || []
  }
  bindTeamOpen.value = true
}

function submitBindTeam() {
  if (!bindTeamForm.longTermTeamId) {
    proxy.$modal.msgError('请选择球队')
    return
  }
  bindV4PersonalFormationTeam(currentDrawTeam.value!.drawTeamId!, { longTermTeamId: bindTeamForm.longTermTeamId }).then(() => {
    proxy.$modal.msgSuccess('球队已绑定')
    bindTeamOpen.value = false
    refreshDrawDetail()
  })
}

function handleConfirmFormation(team: BasketPersonalDrawTeamVo) {
  proxy.$modal.confirm(`确认组队「${team.temporaryTeamName}」？`).then(() => confirmV4PersonalFormation(team.drawTeamId!)).then(() => {
    proxy.$modal.msgSuccess('组队已确认')
    refreshDrawDetail()
  }).catch(() => {})
}

function onTabChange(name: string | number) {
  if (name === 'personal' && !personalList.value.length) getPersonalList()
  if (name === 'formation' && !drawList.value.length) getDrawList()
}

onMounted(async () => {
  await loadSeasonOptions()
  getList()
})
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.nowrap-tag { white-space: nowrap; }
.drawer-toolbar { display: flex; gap: 8px; }
.expand-block { padding: 8px 16px; }
</style>
