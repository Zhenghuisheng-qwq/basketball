<template>
  <div class="app-container">
    <el-alert title="报名与参赛名单：审核通过不等于已经参赛。TEAM 需分配正式/候补后再锁定；PERSONAL 审核后进入 Formation 临时组队。" type="info" :closable="false" show-icon class="mb8" />
    <DivisionEventSelector v-if="!divisionId" entry="roster" @select="row => selectDivision(row.divisionId)" />
    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <WorkflowBlockedState
        v-if="blocked"
        :title="blocked"
        detail="请先完成赛事规则发布后再整理参赛名单。"
        action-text="去配置赛事规则"
        @action="goPublishRule"
      />
      <template v-else>

    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <!-- 球队报名 -->
      <el-tab-pane v-if="workspaceMode !== 'PERSONAL'" label="球队报名" name="team">
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

      <!-- 球队最终名单 -->
      <el-tab-pane v-if="workspaceMode !== 'PERSONAL'" label="最终名单" name="roster">
        <el-form :inline="true" class="mb8">
          <el-form-item label="赛季">
            <el-select v-model="rosterSeasonId" clearable filterable placeholder="选择赛季" style="width: 200px" @change="onRosterSeasonChange">
              <el-option v-for="item in seasonOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="平台赛区">
            <el-select v-model="rosterDivisionId" clearable filterable placeholder="选择赛区" style="width: 200px" :disabled="!rosterSeasonId" @change="loadRosterWorkspace">
              <el-option v-for="item in rosterDivisionOptions" :key="item.id" :label="formatDivisionLabel(item)" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadRosterWorkspace">刷新</el-button>
            <el-button type="warning" :disabled="!rosterWorkspace?.canEdit" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="handleAutoAllocate">自动分配</el-button>
            <el-button type="success" :disabled="!rosterWorkspace?.canLock" v-hasPermi="['basket:v4Division:edit']" @click="handleLockRoster">锁定最终名单</el-button>
            <el-button :disabled="rosterWorkspace?.rosterStatus !== 'LOCKED'" v-hasPermi="['basket:v4Division:edit']" @click="openPromote">候补递补</el-button>
          </el-form-item>
        </el-form>

        <el-descriptions v-if="rosterWorkspace" :column="4" border class="mb8">
          <el-descriptions-item label="赛区">{{ rosterWorkspace.divisionName }}</el-descriptions-item>
          <el-descriptions-item label="Rule">{{ rosterWorkspace.ruleVersion?.ruleVersionStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Roster">{{ rosterWorkspace.rosterStatus }}</el-descriptions-item>
          <el-descriptions-item label="正式名额">{{ rosterWorkspace.formalCount || 0 }} / {{ rosterWorkspace.formalCap || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-tabs v-if="rosterWorkspace" v-model="rosterInnerTab">
          <el-tab-pane label="全部审核合格" name="approved" />
          <el-tab-pane label="正式参赛" name="formal" />
          <el-tab-pane name="candidate">
            <template #label>
              <el-tooltip content="当正式球队无法参赛时，候补球队可按顺序递补。不是候补球员或替补名单。" placement="top">
                <span>候补球队</span>
              </el-tooltip>
            </template>
          </el-tab-pane>
          <el-tab-pane label="本届不参赛" name="excluded" />
          <el-tab-pane label="审核未通过" name="rejected" />
          <el-tab-pane label="名单版本" name="versions" />
        </el-tabs>

        <el-table v-if="rosterInnerTab !== 'versions'" v-loading="rosterLoading" :data="rosterTableRows" border>
          <el-table-column label="球队" prop="teamName" min-width="140" />
          <el-table-column label="报名时间" prop="submitTime" width="170">
            <template #default="scope">{{ parseTime(scope.row.submitTime) }}</template>
          </el-table-column>
          <el-table-column label="审核状态" width="110" align="center">
            <template #default="scope">
              <el-tag :type="v4RegistrationStatusTagType(scope.row.auditStatus)" size="small">{{ v4RegistrationStatusLabel(scope.row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="名单角色" width="120" align="center">
            <template #default="scope">
              <el-tag :type="v4ParticipationRoleTagType(scope.row.participationRole)" size="small">{{ v4ParticipationRoleLabel(scope.row.participationRole) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="候补顺序" prop="candidateOrder" width="90" align="center" />
          <el-table-column label="操作" width="340" fixed="right">
            <template #default="scope">
              <el-button link type="primary" v-if="scope.row.registrationId" v-hasPermi="['basket:v4RegistrationAudit:query']" @click="openDetail(scope.row)">详情</el-button>
              <template v-if="rosterWorkspace?.canEdit && scope.row.auditStatus === 'APPROVED'">
                <el-button link type="success" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="handleAssign(scope.row, 'FORMAL')">设为正式</el-button>
                <el-button link type="warning" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="handleAssign(scope.row, 'CANDIDATE')">设为候补</el-button>
                <el-button link type="info" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="handleAssign(scope.row, 'EXCLUDED')">设为不参赛</el-button>
              </template>
              <template v-if="rosterWorkspace?.canEdit && scope.row.participationRole === 'CANDIDATE'">
                <el-button link type="primary" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="handleMoveCandidate(scope.row, 'UP')">上移</el-button>
                <el-button link type="primary" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="handleMoveCandidate(scope.row, 'DOWN')">下移</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <el-table v-if="rosterInnerTab === 'versions'" :data="overview?.roster?.versions || []" border>
          <el-table-column label="版本" width="90">
            <template #default="scope">V{{ scope.row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="状态" min-width="140">
            <template #default="scope">{{ scope.row.statusName }}</template>
          </el-table-column>
          <el-table-column label="正式" prop="formalCount" width="80" />
          <el-table-column label="候补球队" prop="candidateCount" width="100" />
          <el-table-column label="不参赛" prop="excludedCount" width="80" />
          <el-table-column label="当前" width="80">
            <template #default="scope">{{ scope.row.isCurrent === 1 ? '是' : '' }}</template>
          </el-table-column>
          <el-table-column label="锁定时间" prop="lockedTime" min-width="160" />
          <el-table-column label="操作人" prop="lockedBy" width="100" />
          <el-table-column label="操作" width="100">
            <template #default="scope">
              <el-button link type="primary" v-if="scope.row.rosterVersionId" @click="openRosterVersion(scope.row.rosterVersionId)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane v-if="workspaceMode !== 'TEAM'" label="个人报名" name="personal">
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
      <el-tab-pane v-if="workspaceMode !== 'TEAM'" label="Formation 临时组队" name="formation">
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

        <div v-if="formationWorkspace" v-loading="formationLoading" class="formation-workspace mb8">
          <el-descriptions :column="4" border class="mb8">
            <el-descriptions-item label="赛区">{{ formationWorkspace.divisionName }}</el-descriptions-item>
            <el-descriptions-item label="Series">{{ formationWorkspace.seriesName || formationWorkspace.seriesCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="GameMode">{{ formationWorkspace.gameMode }}</el-descriptions-item>
            <el-descriptions-item label="报名模式">{{ formationWorkspace.registrationMode }}</el-descriptions-item>
            <el-descriptions-item label="Rule">{{ formationWorkspace.ruleStatus }}</el-descriptions-item>
            <el-descriptions-item label="Roster">{{ formationWorkspace.rosterStatus }}{{ formationWorkspace.currentRosterVersionNo ? ' V' + formationWorkspace.currentRosterVersionNo : '' }}</el-descriptions-item>
            <el-descriptions-item label="Formation">{{ v4DrawBatchStatusLabel(formationWorkspace.formationStatus) }}</el-descriptions-item>
            <el-descriptions-item label="人数规则">{{ formationWorkspace.minTeamSize }}-{{ formationWorkspace.maxTeamSize }} / 至少 {{ formationWorkspace.minimumValidPersonCount }} 人</el-descriptions-item>
            <el-descriptions-item label="有效个人">{{ formationWorkspace.validPersonCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="临时队">{{ formationWorkspace.temporaryTeamCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="未分配">{{ formationWorkspace.unassignedCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="批次">{{ formationWorkspace.currentBatchId || '-' }}</el-descriptions-item>
          </el-descriptions>
          <div class="mb8">
            <el-button type="primary" :disabled="!formationWorkspace.canGenerate" v-hasPermi="['basket:v4PersonalFormation:edit']" @click="handleGenerateFormation">自动 Formation</el-button>
            <el-button :disabled="!formationWorkspace.canGenerate || !formationWorkspace.currentBatchId" v-hasPermi="['basket:v4PersonalFormation:edit']" @click="handleRegenerateFormation">重新生成</el-button>
            <el-button type="success" :disabled="!formationWorkspace.canLock" v-hasPermi="['basket:v4PersonalFormation:confirm']" @click="handleLockFormation">锁定临时队伍</el-button>
          </div>
          <div class="formation-board">
            <div class="formation-unassigned">
              <h4>有效个人 / 未分配（{{ formationWorkspace.unassignedCount || 0 }}）</h4>
              <div v-if="!(formationWorkspace.unassignedList || []).length" class="formation-empty">暂无未分配</div>
              <div v-for="person in formationWorkspace.unassignedList || []" :key="person.personalRegistrationId" class="formation-person">
                <span>{{ person.playerName || person.personalRegistrationId }}</span>
                <el-select v-if="formationWorkspace.canAdjust" size="small" placeholder="分配到" style="width: 140px" @change="(toId) => handleMoveFormationMember(person.personalRegistrationId, null, toId)">
                  <el-option v-for="team in formationWorkspace.teams || []" :key="team.drawTeamId" :label="team.temporaryTeamName" :value="team.drawTeamId" />
                </el-select>
              </div>
            </div>
            <div class="formation-teams">
              <el-card v-for="team in formationWorkspace.teams || []" :key="team.drawTeamId" shadow="never">
                <template #header>
                  <div class="formation-team-head">
                    <strong>{{ team.temporaryTeamName }}</strong>
                    <el-tag size="small" type="warning">临时参赛球队，仅用于本届赛事</el-tag>
                    <span>{{ team.memberCount || 0 }} 人</span>
                  </div>
                  <div class="formation-captain">
                    临时队长：{{ team.captainPlayerName || '-' }}
                    <el-tooltip content="临时队长按该临时球队成员账号创建时间最早者自动确定，仅在本届赛事内生效。" placement="top">
                      <span class="captain-tip">?</span>
                    </el-tooltip>
                  </div>
                  <el-button v-if="formationWorkspace.canAdjust" link type="primary" @click="handleRenameFormationTeam(team)">改名</el-button>
                </template>
                <div v-for="member in team.members || []" :key="member.personalRegistrationId" class="formation-person">
                  <span>{{ member.playerName || member.personalRegistrationId }}{{ member.assignmentRole === 'CAPTAIN' ? '（队长）' : '' }}</span>
                  <el-select v-if="formationWorkspace.canAdjust" size="small" placeholder="移动到" style="width: 120px" @change="(toId) => handleMoveFormationMember(member.personalRegistrationId, team.drawTeamId, toId)">
                    <el-option label="未分配" :value="0" />
                    <el-option v-for="other in otherFormationTeams(team.drawTeamId)" :key="other.drawTeamId" :label="other.temporaryTeamName" :value="other.drawTeamId" />
                  </el-select>
                  <el-select v-if="formationWorkspace.canAdjust" size="small" placeholder="交换" style="width: 120px" @change="(rightId) => handleSwapFormationMember(member.personalRegistrationId, rightId)">
                    <el-option v-for="other in otherFormationMembers(member.personalRegistrationId)" :key="other.personalRegistrationId" :label="(other.playerName || other.personalRegistrationId) as string" :value="other.personalRegistrationId" />
                  </el-select>
                </div>
              </el-card>
            </div>
          </div>
        </div>

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

    <el-dialog v-model="promoteOpen" title="候补递补" width="520px" append-to-body>
      <el-form :model="promoteForm" label-width="110px">
        <el-form-item label="正式球队" required>
          <el-select v-model="promoteForm.formalRegistrationId" filterable placeholder="选择被替换的正式球队" style="width: 100%">
            <el-option v-for="item in rosterWorkspace?.formalList || []" :key="item.registrationId" :label="`${item.teamName} (#${item.registrationId})`" :value="item.registrationId!" />
          </el-select>
        </el-form-item>
        <el-form-item label="候补球队" required>
          <el-select v-model="promoteForm.candidateRegistrationId" filterable placeholder="选择递补候补球队" style="width: 100%">
            <el-option v-for="item in rosterWorkspace?.candidateList || []" :key="item.registrationId" :label="`#${item.candidateOrder} ${item.teamName}`" :value="item.registrationId!" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因" required>
          <el-input v-model="promoteForm.reason" type="textarea" placeholder="例如：正式球队退赛 / 禁赛 / 无法参赛" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="promoteOpen = false">取消</el-button>
        <el-button type="primary" @click="submitPromote">确定递补</el-button>
      </template>
    </el-dialog>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4RegistrationAudit">
import { computed, getCurrentInstance, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import WorkflowBlockedState from '../components/WorkflowBlockedState.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'
import {
  assignV4TeamRoster,
  autoAllocateV4TeamRoster,
  getV4TeamRosterVersion,
  getV4TeamRosterWorkspace,
  lockV4TeamRoster,
  moveV4TeamRosterCandidate,
  promoteV4TeamRosterCandidate,
  type TeamRosterItemVo,
  type TeamRosterWorkspaceVo
} from '@/api/cnpa/event-v4/team-roster'
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
  generateV4PersonalFormation,
  getV4PersonalFormationWorkspace,
  lockV4PersonalFormation,
  moveV4PersonalFormationMember,
  regenerateV4PersonalFormation,
  renameV4PersonalFormationTeam,
  swapV4PersonalFormationMembers,
  type PersonalFormationTeamCardVo,
  type PersonalFormationWorkspaceVo
} from '@/api/cnpa/event-v4/personal-formation'
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
  v4ParticipationRoleLabel,
  v4ParticipationRoleTagType,
  v4RegistrationStatusLabel,
  v4RegistrationStatusTagType
} from '@/utils/v4AdminDisplay'
import { parseTime } from '@/utils/ruoyi'

const { proxy } = getCurrentInstance() as any
const router = useRouter()
const { divisionId, overview, blocked, selectDivision, clearDivision } = useDivisionWorkspace('roster')
const workspaceMode = computed(() => overview.value?.context?.registrationMode || '')
const activeTab = ref('team')

watch([divisionId, workspaceMode], () => {
  if (!divisionId.value) return
  const seasonId = overview.value?.context?.seasonId
  if (seasonId) {
    queryParams.seasonId = seasonId
    rosterSeasonId.value = seasonId
    personalQuery.seasonId = seasonId
    drawSeasonId.value = seasonId
  }
  queryParams.divisionId = divisionId.value
  rosterDivisionId.value = divisionId.value
  personalQuery.divisionId = divisionId.value
  drawQuery.divisionId = divisionId.value
  if (workspaceMode.value === 'PERSONAL' && (activeTab.value === 'team' || activeTab.value === 'roster')) {
    activeTab.value = 'personal'
  }
  if (workspaceMode.value === 'TEAM' && (activeTab.value === 'personal' || activeTab.value === 'formation')) {
    activeTab.value = 'team'
  }
  getList()
  loadRosterWorkspace()
  loadFormationWorkspace()
})

function goPublishRule() {
  router.push({ path: '/event/event-v4-publish', query: { divisionId: String(divisionId.value) } })
}

function openRosterVersion(rosterVersionId: number) {
  if (!divisionId.value) return
  getV4TeamRosterVersion(divisionId.value, rosterVersionId).then(res => {
    const data = res.data
    proxy?.$alert(
      `正式 ${data?.formalCount || 0} / 候补球队 ${data?.candidateCount || 0} / 不参赛 ${data?.excludedCount || 0}`,
      `名单 V${data?.versionNo}`
    )
  })
}

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

const rosterLoading = ref(false)
const rosterSeasonId = ref<number>()
const rosterDivisionId = ref<number>()
const rosterDivisionOptions = ref<V4AdminOptionVo[]>([])
const rosterWorkspace = ref<TeamRosterWorkspaceVo>()
const rosterInnerTab = ref('approved')
const promoteOpen = ref(false)
const promoteForm = reactive({
  formalRegistrationId: undefined as number | undefined,
  candidateRegistrationId: undefined as number | undefined,
  reason: ''
})
const rosterTableRows = computed<TeamRosterItemVo[]>(() => {
  const ws = rosterWorkspace.value
  if (!ws) return []
  if (rosterInnerTab.value === 'formal') return ws.formalList || []
  if (rosterInnerTab.value === 'candidate') return ws.candidateList || []
  if (rosterInnerTab.value === 'excluded') return ws.excludedList || []
  if (rosterInnerTab.value === 'rejected') return ws.rejectedList || []
  return ws.approvedList || []
})

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
const formationLoading = ref(false)
const formationWorkspace = ref<PersonalFormationWorkspaceVo>()
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
    case 'CONVERTED':
    case 'LOCKED': return 'success'
    case 'GENERATED':
    case 'ADJUSTING':
    case 'DRAFT': return 'warning'
    case 'CANCELLED': return 'info'
    default: return 'info'
  }
}

async function loadSeasonOptions() {
  const res = await listV4AdminSeasonOptions()
  seasonOptions.value = res.data || []
}

async function loadDivisions(seasonId?: number, target: 'team' | 'personal' | 'draw' | 'roster' = 'team') {
  const list = seasonId ? (await listV4AdminDivisionOptions(seasonId)).data || [] : []
  if (target === 'team') divisionOptions.value = list
  else if (target === 'personal') personalDivisionOptions.value = list
  else if (target === 'roster') rosterDivisionOptions.value = list
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
  formationWorkspace.value = undefined
  loadDivisions(seasonId, 'draw')
}

function otherFormationTeams(drawTeamId?: number) {
  return (formationWorkspace.value?.teams || []).filter(item => item.drawTeamId !== drawTeamId)
}

function otherFormationMembers(personalRegistrationId?: number) {
  return (formationWorkspace.value?.teams || [])
    .flatMap(team => team.members || [])
    .filter(item => item.personalRegistrationId !== personalRegistrationId)
}

function loadFormationWorkspace() {
  if (!drawQuery.divisionId) {
    formationWorkspace.value = undefined
    return
  }
  formationLoading.value = true
  getV4PersonalFormationWorkspace(drawQuery.divisionId).then(res => {
    formationWorkspace.value = res.data
  }).catch(() => {
    formationWorkspace.value = undefined
  }).finally(() => { formationLoading.value = false })
}

function handleGenerateFormation() {
  if (!drawQuery.divisionId) return
  proxy.$modal.confirm('将按当前有效个人自动生成临时队伍，未锁定前可调整。确认生成？').then(() => generateV4PersonalFormation(drawQuery.divisionId!)).then(() => {
    proxy.$modal.msgSuccess('已生成临时队伍')
    loadFormationWorkspace()
    getDrawList()
  }).catch(() => {})
}

function handleRegenerateFormation() {
  if (!drawQuery.divisionId) return
  proxy.$modal.confirm('重新生成将覆盖当前未锁定的组队结果。确认重新生成？').then(() => regenerateV4PersonalFormation(drawQuery.divisionId!)).then(() => {
    proxy.$modal.msgSuccess('已重新生成')
    loadFormationWorkspace()
    getDrawList()
  }).catch(() => {})
}

function handleLockFormation() {
  if (!drawQuery.divisionId) return
  proxy.$modal.confirm('锁定后将生成正式临时参赛快照，且当前组队不可再直接修改。确认锁定？').then(() => lockV4PersonalFormation(drawQuery.divisionId!)).then(() => {
    proxy.$modal.msgSuccess('临时队伍已锁定')
    loadFormationWorkspace()
    getDrawList()
  }).catch(() => {})
}

function handleRenameFormationTeam(team: PersonalFormationTeamCardVo) {
  if (!drawQuery.divisionId || !team.drawTeamId) return
  proxy.$modal.prompt('请输入临时队名', '修改队名', { inputValue: team.temporaryTeamName || '' }).then(({ value }: { value: string }) => {
    return renameV4PersonalFormationTeam(drawQuery.divisionId!, team.drawTeamId!, value)
  }).then(() => {
    proxy.$modal.msgSuccess('已修改队名')
    loadFormationWorkspace()
  }).catch(() => {})
}

function handleMoveFormationMember(personalRegistrationId?: number, fromDrawTeamId?: number | null, toDrawTeamId?: number | null) {
  if (!drawQuery.divisionId || !personalRegistrationId) return
  const toId = toDrawTeamId === 0 ? null : toDrawTeamId
  moveV4PersonalFormationMember(drawQuery.divisionId, {
    personalRegistrationId,
    fromDrawTeamId: fromDrawTeamId || undefined,
    toDrawTeamId: toId
  }).then(() => loadFormationWorkspace())
}

function handleSwapFormationMember(leftPersonalRegistrationId?: number, rightPersonalRegistrationId?: number) {
  if (!drawQuery.divisionId || !leftPersonalRegistrationId || !rightPersonalRegistrationId) return
  swapV4PersonalFormationMembers(drawQuery.divisionId, {
    leftPersonalRegistrationId,
    rightPersonalRegistrationId
  }).then(() => loadFormationWorkspace())
}

watch(() => drawQuery.divisionId, () => {
  loadFormationWorkspace()
})

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

function onRosterSeasonChange(seasonId?: number) {
  rosterDivisionId.value = undefined
  rosterWorkspace.value = undefined
  loadDivisions(seasonId, 'roster')
}

async function loadRosterWorkspace() {
  if (!rosterDivisionId.value) {
    rosterWorkspace.value = undefined
    return
  }
  rosterLoading.value = true
  try {
    const res = await getV4TeamRosterWorkspace(rosterDivisionId.value)
    rosterWorkspace.value = res.data
  } finally {
    rosterLoading.value = false
  }
}

function handleAssign(row: TeamRosterItemVo, role: string) {
  if (!rosterDivisionId.value || !row.registrationId) return
  assignV4TeamRoster(rosterDivisionId.value, { registrationId: row.registrationId, participationRole: role }).then(() => {
    proxy.$modal.msgSuccess('已调整名单角色')
    loadRosterWorkspace()
  })
}

function handleAutoAllocate() {
  if (!rosterDivisionId.value) return
  proxy.$modal.confirm('按报名时间将前 N 支设为正式，其余设为候补？').then(() => autoAllocateV4TeamRoster(rosterDivisionId.value!)).then(() => {
    proxy.$modal.msgSuccess('已自动分配')
    loadRosterWorkspace()
  }).catch(() => {})
}

function handleMoveCandidate(row: TeamRosterItemVo, direction: string) {
  if (!rosterDivisionId.value || !row.registrationId) return
  moveV4TeamRosterCandidate(rosterDivisionId.value, { registrationId: row.registrationId, direction }).then(() => {
    loadRosterWorkspace()
  })
}

function handleLockRoster() {
  if (!rosterDivisionId.value) return
  proxy.$modal.confirm('锁定后将生成正式参赛快照，且当前名单不可再直接修改。确认锁定？').then(() => lockV4TeamRoster(rosterDivisionId.value!)).then(() => {
    proxy.$modal.msgSuccess('最终名单已锁定')
    loadRosterWorkspace()
  }).catch(() => {})
}

function openPromote() {
  Object.assign(promoteForm, { formalRegistrationId: undefined, candidateRegistrationId: undefined, reason: '' })
  promoteOpen.value = true
}

function submitPromote() {
  if (!rosterDivisionId.value || !promoteForm.formalRegistrationId || !promoteForm.candidateRegistrationId || !promoteForm.reason.trim()) {
    proxy.$modal.msgError('请选择双方球队并填写递补原因')
    return
  }
  promoteV4TeamRosterCandidate(rosterDivisionId.value, {
    formalRegistrationId: promoteForm.formalRegistrationId,
    candidateRegistrationId: promoteForm.candidateRegistrationId,
    reason: promoteForm.reason.trim()
  }).then(() => {
    proxy.$modal.msgSuccess('已递补并生成新名单版本')
    promoteOpen.value = false
    loadRosterWorkspace()
  })
}

function onTabChange(name: string | number) {
  if (name === 'personal' && !personalList.value.length) getPersonalList()
  if (name === 'formation') {
    if (!drawList.value.length) getDrawList()
    loadFormationWorkspace()
  }
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
.formation-board { display: flex; gap: 16px; align-items: flex-start; }
.formation-unassigned { width: 260px; flex-shrink: 0; border: 1px solid var(--el-border-color); border-radius: 4px; padding: 12px; }
.formation-teams { flex: 1; display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 12px; }
.formation-team-head { display: flex; justify-content: space-between; gap: 8px; }
.formation-captain { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.formation-person { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 8px; }
.formation-empty { color: var(--el-text-color-secondary); font-size: 13px; }
.formation-unassigned h4 { margin: 0 0 8px; }
</style>
