<!--
菜单：账号与认证 > 用户管理（menu_id=2210, path=account-player）
-->
<template>
  <div class="app-container account-player-page">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="96px" class="search-form">
      <el-form-item label="用户 ID" prop="memberId">
        <el-input v-model="queryParams.memberId" placeholder="用户 ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" placeholder="登录邮箱" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员编号" prop="playerNo">
        <el-input v-model="queryParams.playerNo" placeholder="CNPA 球员编号" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="账号状态" prop="accountStatus">
        <el-select v-model="queryParams.accountStatus" placeholder="账号状态" clearable style="width: 120px">
          <el-option v-for="item in MEMBER_ACCOUNT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="球员认证" prop="certStage">
        <el-select v-model="queryParams.certStage" placeholder="认证状态" clearable style="width: 130px">
          <el-option v-for="item in MEMBER_CERT_STAGE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="队长授权" prop="hasCaptainQual">
        <el-select v-model="queryParams.hasCaptainQual" placeholder="当前授权" clearable style="width: 110px">
          <el-option v-for="item in YES_NO_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="当前球队" prop="currentTeamName">
        <el-input v-model="queryParams.currentTeamName" placeholder="球队名称" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>

      <template v-if="showAdvanced">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="queryParams.nickname" placeholder="用户昵称" clearable style="width: 150px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="游戏ID" prop="gameId">
          <el-input v-model="queryParams.gameId" placeholder="平台游戏 ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="游戏平台" prop="gameTerminal">
          <el-select v-model="queryParams.gameTerminal" placeholder="游戏平台" clearable style="width: 120px">
            <el-option v-for="item in GAME_PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="已入队" prop="hasTeam">
          <el-select v-model="queryParams.hasTeam" placeholder="是否入队" clearable style="width: 110px">
            <el-option v-for="item in YES_NO_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </template>

      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button link type="primary" @click="showAdvanced = !showAdvanced">
          {{ showAdvanced ? '收起高级筛选' : '高级筛选' }}
        </el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="24">
        <el-alert
          title="列表仅展示用户核心状态；平台游戏 ID、队长授权明细、球队与报名记录请在详情中查看。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList" class="account-player-table">
      <el-table-column label="用户信息" min-width="180" width="200" fixed>
        <template #default="scope">
          <div class="user-cell">
            <el-avatar :size="32" :src="resolveMemberAvatarUrl(scope.row.avatarUrl)" class="user-cell__avatar">
              {{ avatarFallback(scope.row.nickname || scope.row.username) }}
            </el-avatar>
            <el-tooltip
              :content="scope.row.nickname || '未设置昵称'"
              placement="top"
              :disabled="!scope.row.nickname"
            >
              <span class="user-cell__name ellipsis">{{ scope.row.nickname || '未设置昵称' }}</span>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="登录邮箱" min-width="180" width="200" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.email || '-' }}</template>
      </el-table-column>
      <el-table-column label="用户ID" min-width="180" width="200" :show-overflow-tooltip="true">
        <template #default="scope">
          <span class="user-id-text">{{ scope.row.userId || scope.row.memberId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="球员编号" prop="playerNo" width="130" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.playerNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="绑定游戏ID" min-width="180">
        <template #default="scope">
          <el-tooltip
            :content="scope.row.gameIdSummary || '未绑定'"
            placement="top"
            :disabled="!(scope.row.gameIdSummary && scope.row.gameIdSummary !== '未绑定')"
          >
            <span class="game-id-cell">{{ scope.row.gameIdCompactSummary || scope.row.gameIdSummary || '未绑定' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="邮箱验证" width="92" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.emailVerified === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.emailVerified === 1 ? '已验证' : '未验证' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" width="92" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.accountStatus === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.accountStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="球员认证" width="100" align="center">
        <template #default="scope">
          <el-tag :type="certStageTagType(scope.row.certDisplayStatus)" size="small">
            {{ scope.row.certDisplayStatusName || '未认证' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前球队" min-width="130" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.currentTeamName || '-' }}</template>
      </el-table-column>
      <el-table-column label="当前队长授权" min-width="140">
        <template #default="scope">
          <template v-if="scope.row.hasCaptainQual === 1">
            <el-tag type="warning" size="small">有</el-tag>
            <div v-if="scope.row.activeCaptainGrantEventName" class="sub-text ellipsis">
              {{ scope.row.activeCaptainGrantEventName }}
            </div>
          </template>
          <el-tag v-else type="info" size="small">无</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近登录" width="160">
        <template #default="scope">{{ parseTime(scope.row.lastLoginTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="注册时间" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="88" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:accountPlayer:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog
      v-model="detailOpen"
      title="用户详情"
      width="920px"
      top="6vh"
      append-to-body
      destroy-on-close
      class="account-player-dialog"
    >
      <div v-loading="detailLoading" class="account-player-dialog__body">
        <template v-if="detail">
          <div class="detail-header">
            <el-avatar :size="52" :src="resolveMemberAvatarUrl(detail.account?.avatarUrl || detail.profile?.avatarUrl)" class="detail-header__avatar">
              {{ avatarFallback(detail.account?.nickname || detail.account?.username) }}
            </el-avatar>
            <div class="detail-header__meta">
              <div class="detail-header__name">{{ detail.account?.nickname || detail.account?.username || '未设置昵称' }}</div>
              <div class="detail-header__sub">{{ detail.account?.email || '-' }}</div>
              <div class="detail-header__tags">
                <el-tag :type="detail.account?.accountStatus === 1 ? 'success' : 'danger'" size="small">
                  {{ detail.account?.accountStatusName || '-' }}
                </el-tag>
                <el-tag :type="certStageTagType(profileCertStage(detail))" size="small">
                  {{ detail.profile?.certificationStatusName || '未认证' }}
                </el-tag>
              </div>
            </div>
          </div>

          <el-tabs v-model="activeDetailTab" class="detail-tabs">
            <el-tab-pane label="基础信息" name="account">
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="用户ID">{{ detail.account?.userId || detail.account?.memberId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="内部兼容标识">{{ detail.account?.username || '-' }}</el-descriptions-item>
                <el-descriptions-item label="登录邮箱">{{ detail.account?.email || '-' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱验证">{{ detail.account?.emailVerified === 1 ? '已验证' : '未验证' }}</el-descriptions-item>
                <el-descriptions-item label="账号状态">{{ detail.account?.accountStatusName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="注册时间">{{ parseTime(detail.account?.createTime) || '-' }}</el-descriptions-item>
                <el-descriptions-item label="最近登录" :span="2">{{ parseTime(detail.account?.lastLoginTime) || '-' }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <el-tab-pane label="球员档案" name="profile">
              <el-descriptions :column="2" border size="small" class="mb12">
                <el-descriptions-item label="球员档案ID">{{ detail.profile?.playerId || '-' }}</el-descriptions-item>
                <el-descriptions-item label="球员编号">{{ detail.profile?.playerNo || '-' }}</el-descriptions-item>
                <el-descriptions-item label="昵称">{{ detail.profile?.nickname || detail.profile?.gameNickname || '-' }}</el-descriptions-item>
                <el-descriptions-item label="认证状态">{{ detail.profile?.certificationStatusName || '未认证' }}</el-descriptions-item>
                <el-descriptions-item label="主位置">{{ detail.profile?.positionCode || '-' }}</el-descriptions-item>
                <el-descriptions-item label="副位置">-</el-descriptions-item>
                <el-descriptions-item label="认证时间" :span="2">{{ parseTime(detail.profile?.certifiedTime) || '-' }}</el-descriptions-item>
              </el-descriptions>
              <div class="section-title">认证申请历史</div>
              <el-table :data="detail.certHistory || []" size="small" border>
                <el-table-column label="申请编号" prop="applicationNo" min-width="130" show-overflow-tooltip />
                <el-table-column label="平台" prop="gameTerminal" width="80" />
                <el-table-column label="游戏ID" prop="gameAccount" min-width="120" show-overflow-tooltip />
                <el-table-column label="状态" prop="applicationStatus" width="100">
                  <template #default="scope">
                    <dict-tag :options="PLAYER_CERT_STATUS_OPTIONS" :value="scope.row.applicationStatus" />
                  </template>
                </el-table-column>
                <el-table-column label="提交时间" prop="submittedTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.submittedTime) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!(detail.certHistory && detail.certHistory.length)" description="暂无认证申请" />
            </el-tab-pane>

            <el-tab-pane label="平台游戏 ID" name="platform">
              <el-table :data="detail.platformAccounts || []" size="small" border>
                <el-table-column label="平台" prop="platform" width="90" />
                <el-table-column label="游戏 ID" prop="displayGameId" min-width="140" show-overflow-tooltip />
                <el-table-column label="审核状态" prop="accountStatusName" width="100" />
                <el-table-column label="生效状态" width="90">
                  <template #default="scope">
                    {{ scope.row.accountStatus === 1 ? '已生效' : '未生效' }}
                  </template>
                </el-table-column>
                <el-table-column label="更新时间" prop="updateTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.updateTime || scope.row.effectiveTime) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default>
                    <el-button link type="primary" @click="goPlatformChange">查看变更记录</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!(detail.platformAccounts && detail.platformAccounts.length)" description="暂无平台游戏 ID" />
            </el-tab-pane>

            <el-tab-pane label="当前球队" name="team">
              <template v-if="currentTeamRows.length">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="球队名称">{{ currentTeamRows[0].teamName || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="成员角色">{{ formatRoleType(currentTeamRows[0].roleType) }}</el-descriptions-item>
                  <el-descriptions-item label="加入时间">{{ parseTime(currentTeamRows[0].joinTime) || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="成员状态">{{ currentTeamRows[0].membershipStatusName || '-' }}</el-descriptions-item>
                </el-descriptions>
                <div v-if="detail.teamSummary && detail.teamSummary.length > 1" class="section-title mt12">历史球队</div>
                <el-table v-if="detail.teamSummary && detail.teamSummary.length > 1" :data="historyTeamRows" size="small" border>
                  <el-table-column label="球队名称" prop="teamName" min-width="140" show-overflow-tooltip />
                  <el-table-column label="角色" width="90">
                    <template #default="scope">{{ formatRoleType(scope.row.roleType) }}</template>
                  </el-table-column>
                  <el-table-column label="成员状态" prop="membershipStatusName" width="100" />
                  <el-table-column label="加入时间" prop="joinTime" width="160">
                    <template #default="scope">{{ parseTime(scope.row.joinTime) }}</template>
                  </el-table-column>
                </el-table>
              </template>
              <el-empty v-else description="暂无球队" />
            </el-tab-pane>

            <el-tab-pane label="队长授权记录" name="captain">
              <el-table :data="detail.captainGrants || []" size="small" border>
                <el-table-column label="授权赛事" prop="eventName" min-width="140" show-overflow-tooltip />
                <el-table-column label="关联球队" prop="teamName" min-width="120" show-overflow-tooltip />
                <el-table-column label="授权状态" prop="qualificationStatusName" width="100" />
                <el-table-column label="缴费确认" width="90">
                  <template #default="scope">{{ scope.row.paymentConfirmed === 1 ? '已确认' : '未确认' }}</template>
                </el-table-column>
                <el-table-column label="缴费参考号" prop="paymentReference" min-width="120" show-overflow-tooltip />
                <el-table-column label="授权时间" prop="grantedTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.grantedTime) }}</template>
                </el-table-column>
                <el-table-column label="失效时间" prop="validTo" width="160">
                  <template #default="scope">{{ parseTime(scope.row.validTo) || '-' }}</template>
                </el-table-column>
                <el-table-column label="授权人" prop="grantedByName" width="100" show-overflow-tooltip />
                <el-table-column label="收回时间" prop="revokedTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.revokedTime) || '-' }}</template>
                </el-table-column>
                <el-table-column label="收回原因" prop="revokeReason" min-width="120" show-overflow-tooltip />
              </el-table>
              <el-empty v-if="!(detail.captainGrants && detail.captainGrants.length)" description="暂无队长授权记录" />
            </el-tab-pane>

            <el-tab-pane label="赛事报名记录" name="registration">
              <el-table :data="detail.registrations || []" size="small" border>
                <el-table-column label="赛事名称" prop="eventName" min-width="140" show-overflow-tooltip />
                <el-table-column label="报名平台" prop="platformCode" width="90" />
                <el-table-column label="球队" prop="teamName" min-width="120" show-overflow-tooltip />
                <el-table-column label="报名状态" prop="registrationStatusName" width="100" />
                <el-table-column label="审核状态" prop="auditStatusName" width="100" />
                <el-table-column label="提交时间" prop="submittedTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.submittedTime) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!(detail.registrations && detail.registrations.length)" description="暂无赛事报名记录" />
            </el-tab-pane>

            <el-tab-pane label="系统通知" name="notification">
              <el-table :data="detail.notifications || []" size="small" border>
                <el-table-column label="通知标题" prop="title" min-width="180" show-overflow-tooltip />
                <el-table-column label="通知类型" prop="messageType" width="120" show-overflow-tooltip />
                <el-table-column label="已读状态" prop="readStatusName" width="90" />
                <el-table-column label="创建时间" prop="createTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
                </el-table-column>
              </el-table>
              <el-empty v-if="!(detail.notifications && detail.notifications.length)" description="暂无系统通知" />
            </el-tab-pane>

            <el-tab-pane label="操作日志" name="log">
              <el-table :data="detail.operationLogs || []" size="small" border>
                <el-table-column label="操作类型" prop="eventType" width="120" show-overflow-tooltip />
                <el-table-column label="操作标题" prop="eventTitle" min-width="140" show-overflow-tooltip />
                <el-table-column label="操作人" prop="operatorName" width="100" show-overflow-tooltip />
                <el-table-column label="操作时间" prop="eventTime" width="160">
                  <template #default="scope">{{ parseTime(scope.row.eventTime) }}</template>
                </el-table-column>
                <el-table-column label="操作备注" prop="remark" min-width="140" show-overflow-tooltip />
              </el-table>
              <el-empty v-if="!(detail.operationLogs && detail.operationLogs.length)" description="暂无操作日志" />
            </el-tab-pane>
          </el-tabs>
        </template>
      </div>
      <template #footer>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="BasketMemberUser">
import { useRouter } from 'vue-router'
import { getAccountPlayer, listAccountPlayer } from '@/api/basket/accountPlayer'
import type { AccountPlayerFullDetail, AccountPlayerListItem, AccountPlayerQuery, AccountPlayerTeamSummary } from '@/types'
import {
  GAME_PLATFORM_OPTIONS,
  MEMBER_ACCOUNT_STATUS_OPTIONS,
  MEMBER_CERT_STAGE_OPTIONS,
  PLAYER_CERT_STATUS_OPTIONS,
  YES_NO_OPTIONS
} from '@/types'
import { applyTablePage } from '@/utils/table'
import { resolveMemberAvatarUrl } from '@/utils/memberAvatar'

const router = useRouter()
const { proxy } = getCurrentInstance()

const dataList = ref<AccountPlayerListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const showAdvanced = ref(false)
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<AccountPlayerFullDetail>()
const activeDetailTab = ref('account')

const queryParams = ref<AccountPlayerQuery>({
  pageNum: 1,
  pageSize: 10,
  memberId: undefined,
  email: undefined,
  playerNo: undefined,
  certStage: undefined,
  accountStatus: undefined,
  hasCaptainQual: undefined,
  currentTeamName: undefined,
  nickname: undefined,
  gameId: undefined,
  gameTerminal: undefined,
  hasTeam: undefined
})

const currentTeamRows = computed(() => {
  const rows = detail.value?.teamSummary || []
  const active = rows.filter(item => item.isCurrent === 1 && item.membershipStatus === 1)
  return active.length ? active : rows.slice(0, 1)
})

const historyTeamRows = computed(() => {
  const rows = detail.value?.teamSummary || []
  const currentIds = new Set(currentTeamRows.value.map(item => item.teamId))
  return rows.filter(item => !currentIds.has(item.teamId))
})

function avatarFallback(name?: string) {
  const text = (name || '?').trim()
  return text ? text.charAt(0).toUpperCase() : '?'
}

function certStageTagType(stage?: string) {
  if (stage === 'APPROVED') return 'success'
  if (stage === 'PENDING') return 'warning'
  if (stage === 'REJECTED') return 'danger'
  if (stage === 'NO_PROFILE') return 'info'
  return ''
}

function profileCertStage(d: AccountPlayerFullDetail) {
  const status = d.profile?.certificationStatus
  if (!d.profile?.playerId) return 'NO_PROFILE'
  if (status === 3) return 'APPROVED'
  return ''
}

function formatRoleType(role?: string) {
  if (role === 'CAPTAIN') return '队长'
  if (role === 'MEMBER') return '队员'
  return role || '-'
}

function getList() {
  loading.value = true
  listAccountPlayer(queryParams.value).then(response => {
    applyTablePage(response, dataList, total)
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  showAdvanced.value = false
  handleQuery()
}

function handleDetail(row: AccountPlayerListItem) {
  detailOpen.value = true
  detailLoading.value = true
  activeDetailTab.value = 'account'
  detail.value = undefined
  getAccountPlayer(String(row.memberId)).then(res => {
    detail.value = res.data
  }).finally(() => {
    detailLoading.value = false
  })
}

function goPlatformChange() {
  router.push('/identity/platform-change')
}

getList()
</script>

<style scoped>
.account-player-page .search-form {
  margin-bottom: 8px;
}

.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.user-id-text {
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.user-cell__name {
  min-width: 0;
  flex: 1;
  font-weight: 600;
  line-height: 1.4;
}

.user-cell__avatar,
.detail-header__avatar {
  flex-shrink: 0;
  background: var(--el-color-primary);
  color: #fff;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.detail-header__name {
  font-size: 16px;
  font-weight: 600;
}

.detail-header__sub {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.detail-header__tags {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.detail-tabs {
  margin-top: 4px;
}

.account-player-dialog__body {
  max-height: calc(88vh - 140px);
  overflow: auto;
  padding-right: 4px;
}

.section-title {
  margin: 12px 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}

.mb12 {
  margin-bottom: 12px;
}

.mt12 {
  margin-top: 12px;
}

.account-player-table :deep(.el-table__cell) {
  vertical-align: middle;
}

.game-id-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}
</style>
