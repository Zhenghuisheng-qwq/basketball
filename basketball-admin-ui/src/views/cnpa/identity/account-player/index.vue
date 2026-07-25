<!--
菜单：账号与认证 > 用户管理（menu_id=2210, path=account-player）
-->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" placeholder="请输入邮箱" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="queryParams.nickname" placeholder="请输入昵称" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员编号" prop="playerNo">
        <el-input v-model="queryParams.playerNo" placeholder="永久球员编号" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="游戏ID" prop="gameId">
        <el-input v-model="queryParams.gameId" placeholder="主平台或绑定ID" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="认证阶段" prop="certStage">
        <el-select v-model="queryParams.certStage" placeholder="认证阶段" clearable style="width: 130px">
          <el-option v-for="item in MEMBER_CERT_STAGE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="账号状态" prop="accountStatus">
        <el-select v-model="queryParams.accountStatus" placeholder="账号状态" clearable style="width: 120px">
          <el-option v-for="item in MEMBER_ACCOUNT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
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
      <el-form-item label="队长资格" prop="hasCaptainQual">
        <el-select v-model="queryParams.hasCaptainQual" placeholder="队长资格" clearable style="width: 110px">
          <el-option v-for="item in YES_NO_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="24">
        <el-alert
          title="展示所有官网注册用户及其球员档案主数据；认证申请审核请前往「球员认证审核」。"
          type="info"
          :closable="false"
          show-icon
        />
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="用户" min-width="220" fixed>
        <template #default="scope">
          <div class="user-cell">
            <el-avatar :size="36" :src="scope.row.avatarUrl" class="user-cell__avatar">
              {{ avatarFallback(scope.row.nickname) }}
            </el-avatar>
            <div class="user-cell__meta">
              <div class="user-cell__name">{{ scope.row.nickname || '未设置昵称' }}</div>
              <div class="sub-text">账号 {{ scope.row.username || '-' }}</div>
              <div class="sub-text">{{ scope.row.email || '-' }}</div>
              <div class="sub-text">ID {{ scope.row.memberId }} · {{ scope.row.accountNo || '-' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="邮箱验证" prop="emailVerified" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.emailVerified === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.emailVerified === 1 ? '已验证' : '未验证' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" prop="accountStatus" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.accountStatus === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.accountStatusName || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="认证阶段" prop="certDisplayStatusName" width="100" align="center">
        <template #default="scope">
          <el-tag :type="certStageTagType(scope.row.certDisplayStatus)" size="small">
            {{ scope.row.certDisplayStatusName || '未认证' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="球员编号" prop="playerNo" min-width="120" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.playerNo || '-' }}</template>
      </el-table-column>
      <el-table-column label="主平台/游戏ID" min-width="150">
        <template #default="scope">
          <div>{{ scope.row.gameTerminal || '-' }}</div>
          <div class="sub-text">{{ scope.row.gameAccount || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="当前球队" prop="currentTeamName" min-width="140" :show-overflow-tooltip="true">
        <template #default="scope">{{ scope.row.currentTeamName || '-' }}</template>
      </el-table-column>
      <el-table-column label="队长资格" prop="hasCaptainQual" width="90" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.hasCaptainQual === 1 ? 'warning' : 'info'" size="small">
            {{ scope.row.hasCaptainQual === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近登录" prop="lastLoginTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.lastLoginTime) || '-' }}</template>
      </el-table-column>
      <el-table-column label="注册时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:accountPlayer:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="用户详情" size="760px" append-to-body>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-divider content-position="left">账号信息</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="会员ID">{{ detail.account?.memberId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号编号">{{ detail.account?.accountNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="登录账号">{{ detail.account?.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ detail.account?.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.account?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱验证">{{ detail.account?.emailVerified === 1 ? '已验证' : '未验证' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">{{ detail.account?.accountStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ parseTime(detail.account?.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="最近登录">{{ parseTime(detail.account?.lastLoginTime) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="最近登录IP" :span="2">{{ detail.account?.lastLoginIp || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">球员档案</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="球员ID">{{ detail.profile?.playerId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="球员编号">{{ detail.profile?.playerNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="认证状态">{{ detail.profile?.certificationStatusName || '未认证' }}</el-descriptions-item>
            <el-descriptions-item label="认证时间">{{ parseTime(detail.profile?.certifiedTime) || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主平台">{{ detail.profile?.gameTerminal || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主游戏ID">{{ detail.profile?.gameAccount || '-' }}</el-descriptions-item>
            <el-descriptions-item label="游戏昵称">{{ detail.profile?.gameNickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ detail.profile?.positionCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="当前球队" :span="2">
              {{ detail.profile?.currentTeamName || detail.profile?.currentTeamId || '-' }}
            </el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">平台游戏ID</el-divider>
          <el-table :data="detail.platformAccounts || []" size="small" border>
            <el-table-column label="平台" prop="platform" width="90" />
            <el-table-column label="游戏ID" prop="displayGameId" min-width="140" />
            <el-table-column label="状态" prop="accountStatus" width="90">
              <template #default="scope">
                <dict-tag :options="PLATFORM_ACCOUNT_STATUS_OPTIONS" :value="scope.row.accountStatus" />
              </template>
            </el-table-column>
            <el-table-column label="主平台" prop="isPrimary" width="80">
              <template #default="scope">{{ scope.row.isPrimary === 1 ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column label="生效时间" prop="effectiveTime" width="160">
              <template #default="scope">{{ parseTime(scope.row.effectiveTime) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!(detail.platformAccounts && detail.platformAccounts.length)" description="暂无平台账号" />

          <el-divider content-position="left">认证申请历史</el-divider>
          <el-table :data="detail.certHistory || []" size="small" border>
            <el-table-column label="申请编号" prop="applicationNo" min-width="140" />
            <el-table-column label="平台" prop="gameTerminal" width="90" />
            <el-table-column label="游戏ID" prop="gameAccount" min-width="120" />
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

          <template v-if="detail.captainQual">
            <el-divider content-position="left">队长资格</el-divider>
            <el-descriptions :column="2" border class="detail-desc">
              <el-descriptions-item label="资格编号">{{ detail.captainQual.qualificationNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ detail.captainQual.qualificationStatusName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="当前有效">{{ detail.captainQual.runtimeValid === 1 ? '是' : '否' }}</el-descriptions-item>
              <el-descriptions-item label="无效原因" v-if="detail.captainQual.runtimeValid !== 1">{{ detail.captainQual.runtimeInvalidReason || '-' }}</el-descriptions-item>
              <el-descriptions-item label="授予时间">{{ parseTime(detail.captainQual.grantedTime) }}</el-descriptions-item>
              <el-descriptions-item label="有效期至">{{ detail.captainQual.validTo ? parseTime(detail.captainQual.validTo) : '永久' }}</el-descriptions-item>
            </el-descriptions>
          </template>

          <template v-if="detail.teamSummary && detail.teamSummary.length">
            <el-divider content-position="left">球队归属</el-divider>
            <el-table :data="detail.teamSummary" size="small" border>
              <el-table-column label="球队编号" prop="teamNo" min-width="120" />
              <el-table-column label="球队名称" prop="teamName" min-width="140" />
              <el-table-column label="角色" prop="roleType" width="90" />
              <el-table-column label="审核状态" prop="auditStatus" width="100">
                <template #default="scope">
                  <dict-tag :options="TEAM_AUDIT_STATUS_OPTIONS" :value="scope.row.auditStatus" />
                </template>
              </el-table-column>
              <el-table-column label="加入时间" prop="joinTime" width="160">
                <template #default="scope">{{ parseTime(scope.row.joinTime) }}</template>
              </el-table-column>
            </el-table>
          </template>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts" name="BasketMemberUser">
import { getAccountPlayer, listAccountPlayer } from '@/api/basket/accountPlayer'
import type { AccountPlayerFullDetail, AccountPlayerListItem, AccountPlayerQuery } from '@/types'
import {
  GAME_PLATFORM_OPTIONS,
  MEMBER_ACCOUNT_STATUS_OPTIONS,
  MEMBER_CERT_STAGE_OPTIONS,
  PLATFORM_ACCOUNT_STATUS_OPTIONS,
  PLAYER_CERT_STATUS_OPTIONS,
  TEAM_AUDIT_STATUS_OPTIONS,
  YES_NO_OPTIONS
} from '@/types'

const { proxy } = getCurrentInstance()

const dataList = ref<AccountPlayerListItem[]>([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const detailOpen = ref(false)
const detailLoading = ref(false)
const detail = ref<AccountPlayerFullDetail>()

const queryParams = ref<AccountPlayerQuery>({
  pageNum: 1,
  pageSize: 10,
  email: undefined,
  nickname: undefined,
  playerNo: undefined,
  gameId: undefined,
  certStage: undefined,
  accountStatus: undefined,
  gameTerminal: undefined,
  hasTeam: undefined,
  hasCaptainQual: undefined
})

function avatarFallback(nickname?: string) {
  const text = (nickname || '?').trim()
  return text ? text.charAt(0).toUpperCase() : '?'
}

function certStageTagType(stage?: string) {
  if (stage === 'APPROVED') return 'success'
  if (stage === 'PENDING') return 'warning'
  if (stage === 'REJECTED') return 'danger'
  if (stage === 'NO_PROFILE') return 'info'
  return ''
}

function getList() {
  loading.value = true
  listAccountPlayer(queryParams.value).then(response => {
    dataList.value = response.rows
    total.value = response.total
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
  handleQuery()
}

function handleDetail(row: AccountPlayerListItem) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = undefined
  getAccountPlayer(String(row.memberId)).then(res => {
    detail.value = res.data
  }).finally(() => {
    detailLoading.value = false
  })
}

getList()
</script>

<style scoped>
.sub-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.detail-desc {
  margin-bottom: 8px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell__meta {
  min-width: 0;
}

.user-cell__name {
  font-weight: 600;
  line-height: 1.4;
}

.user-cell__avatar {
  flex-shrink: 0;
  background: #d5080d;
  color: #fff;
}
</style>
