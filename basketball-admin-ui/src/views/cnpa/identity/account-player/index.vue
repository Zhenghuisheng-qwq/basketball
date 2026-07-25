<!--
Menu SQL (run separately, parent_id=2151 账号与认证):
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component,
  query, route_name, is_frame, is_cache, menu_type, visible, status,
  perms, icon, create_by, create_time, remark
) VALUES
(2210, '账号与球员管理', 2151, 4, 'account-player', 'cnpa/identity/account-player/index', '', 'BasketAccountPlayer', 1, 0, 'C', '0', '0', 'basket:accountPlayer:list', 'user', 'admin', sysdate(), 'CNPA账号与球员管理');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 2210);
-->
<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="88px">
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="queryParams.email" placeholder="请输入邮箱" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="球员编号" prop="playerNo">
        <el-input v-model="queryParams.playerNo" placeholder="请输入球员编号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="认证状态" prop="certificationStatus">
        <el-select v-model="queryParams.certificationStatus" placeholder="认证状态" clearable style="width: 200px">
          <el-option v-for="item in PLAYER_CERT_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="游戏平台" prop="gameTerminal">
        <el-select v-model="queryParams.gameTerminal" placeholder="游戏平台" clearable style="width: 200px">
          <el-option v-for="item in GAME_PLATFORM_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="会员ID" prop="memberId" width="90" />
      <el-table-column label="账号" min-width="160">
        <template #default="scope">
          <div>{{ scope.row.accountNo || '-' }}</div>
          <div class="sub-text">{{ scope.row.email || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="昵称" prop="nickname" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="球员编号" prop="playerNo" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="主平台" prop="gameTerminal" width="90" />
      <el-table-column label="主游戏ID" prop="gameAccount" min-width="120" :show-overflow-tooltip="true" />
      <el-table-column label="认证状态" prop="certificationStatus" width="100">
        <template #default="scope">
          <dict-tag :options="PLAYER_CERT_STATUS_OPTIONS" :value="scope.row.certificationStatus" />
        </template>
      </el-table-column>
      <el-table-column label="认证时间" prop="certifiedTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.certifiedTime) }}</template>
      </el-table-column>
      <el-table-column label="注册时间" prop="createTime" width="160">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['basket:accountPlayer:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-drawer v-model="detailOpen" title="账号与球员详情" size="760px" append-to-body>
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-divider content-position="left">账号信息</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="会员ID">{{ detail.account?.memberId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号编号">{{ detail.account?.accountNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="用户名">{{ detail.account?.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ detail.account?.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账号状态">{{ accountStatusLabel(detail.account?.accountStatus) }}</el-descriptions-item>
            <el-descriptions-item label="注册时间">{{ parseTime(detail.account?.createTime) }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">球员档案</el-divider>
          <el-descriptions :column="2" border class="detail-desc">
            <el-descriptions-item label="球员ID">{{ detail.profile?.playerId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="球员编号">{{ detail.profile?.playerNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ detail.profile?.nickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="认证状态">{{ detail.profile?.certificationStatusName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主平台">{{ detail.profile?.gameTerminal || '-' }}</el-descriptions-item>
            <el-descriptions-item label="主游戏ID">{{ detail.profile?.gameAccount || '-' }}</el-descriptions-item>
            <el-descriptions-item label="游戏昵称">{{ detail.profile?.gameNickname || '-' }}</el-descriptions-item>
            <el-descriptions-item label="位置">{{ detail.profile?.positionCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="认证时间">{{ parseTime(detail.profile?.certifiedTime) }}</el-descriptions-item>
            <el-descriptions-item label="当前球队ID">{{ detail.profile?.currentTeamId || '-' }}</el-descriptions-item>
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

          <el-divider content-position="left">认证历史</el-divider>
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
          <el-empty v-if="!(detail.certHistory && detail.certHistory.length)" description="暂无认证历史" />

          <template v-if="detail.captainQual">
            <el-divider content-position="left">队长资格</el-divider>
            <el-descriptions :column="2" border class="detail-desc">
              <el-descriptions-item label="资格编号">{{ detail.captainQual.qualificationNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ detail.captainQual.qualificationStatusName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="授予时间">{{ parseTime(detail.captainQual.grantedTime) }}</el-descriptions-item>
              <el-descriptions-item label="有效期至">{{ parseTime(detail.captainQual.validTo) }}</el-descriptions-item>
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

<script setup lang="ts" name="BasketAccountPlayer">
import { getAccountPlayer, listAccountPlayer } from '@/api/basket/accountPlayer'
import type { AccountPlayerFullDetail, AccountPlayerListItem, AccountPlayerQuery } from '@/types'
import {
  GAME_PLATFORM_OPTIONS,
  PLATFORM_ACCOUNT_STATUS_OPTIONS,
  PLAYER_CERT_STATUS_OPTIONS,
  TEAM_AUDIT_STATUS_OPTIONS
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
  playerNo: undefined,
  certificationStatus: undefined,
  gameTerminal: undefined
})

function accountStatusLabel(status?: number) {
  if (status === 0) return '禁用'
  if (status === 1) return '正常'
  return status != null ? String(status) : '-'
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
  getAccountPlayer(row.memberId).then(res => {
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
</style>
