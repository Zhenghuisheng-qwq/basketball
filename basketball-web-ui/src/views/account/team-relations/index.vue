<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import TeamInvitationDialog from '../components/TeamInvitationDialog.vue'
import { useAccountStore } from '@/stores/account'
import type { TeamInvitation, TournamentLineupRelation } from '@/types/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const showCreateDialog = ref(false)
const showInvitationDialog = ref(false)
const activeInvitation = ref<TeamInvitation | null>(null)

const createForm = reactive({
  teamName: '',
  platform: 'PC',
  description: '',
})

const teamRelations = computed(() => accountStore.teamRelations)

const activeLineups = computed(() =>
  teamRelations.value?.lineupRelations.filter(
    (item) => item.status === 'ACTIVE' || item.status === 'WAITING_TEAM_REGISTRATION',
  ) ?? [],
)

const pendingInvitations = computed(() =>
  teamRelations.value?.invitations.filter((item) => item.status === 'PENDING') ?? [],
)

const lineupStatusLabel: Record<TournamentLineupRelation['status'], string> = {
  ACTIVE: '生效中',
  WAITING_TEAM_REGISTRATION: '待球队报名',
  ENDED: '已结束',
}

function openInvitation(invitation: TeamInvitation) {
  activeInvitation.value = invitation
  showInvitationDialog.value = true
}

function viewTeam(item: TournamentLineupRelation) {
  router.push(`/tournament/team/${item.teamId}/overview`)
}

function submitCreateRequest() {
  if (!createForm.teamName.trim()) {
    ElMessage.warning('请输入拟创建球队名称')
    return
  }
  accountStore.submitTeamCreateRequest({
    teamName: createForm.teamName.trim(),
    platform: createForm.platform,
    description: createForm.description.trim(),
  })
  showCreateDialog.value = false
  createForm.teamName = ''
  createForm.description = ''
  ElMessage.success('创建球队申请已提交，等待管理员审核')
}

watch(
  () => route.query.invitationId,
  (id) => {
    if (!id || typeof id !== 'string') return
    const invitation = pendingInvitations.value.find((item) => item.id === id)
    if (invitation) openInvitation(invitation)
  },
  { immediate: true },
)

function handleAccept(id: string) {
  accountStore.acceptTeamInvitation(id)
  showInvitationDialog.value = false
  ElMessage.success('已接受球队邀请')
}

function handleReject(id: string) {
  accountStore.rejectTeamInvitation(id)
  showInvitationDialog.value = false
  ElMessage.success('已拒绝球队邀请')
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球队关系"
      en-title="TEAM RELATIONSHIP"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球队关系' },
      ]"
    />

    <AccountStatusBanner
      title="管理当前球队事务"
      description="查看赛事阵容、处理球队邀请与管理球队创建状态"
      status-text="参赛资格正常"
      status-type="success"
    />

    <AccountPanel title="当前赛事阵容">
      <div v-if="activeLineups.length" class="team-table-wrap">
        <table class="team-table">
          <thead>
            <tr>
              <th>赛事</th>
              <th>球队</th>
              <th>身份</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in activeLineups" :key="item.id">
              <td>{{ item.tournamentName }}</td>
              <td>{{ item.teamName }}</td>
              <td>{{ item.roleName }}</td>
              <td>{{ lineupStatusLabel[item.status] }}</td>
              <td>
                <button type="button" class="account-btn-secondary team-table__btn" @click="viewTeam(item)">
                  查看
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AccountEmptyState v-else message="当前没有有效赛事阵容" />
    </AccountPanel>

    <AccountPanel title="待处理事项">
      <div v-if="pendingInvitations.length" class="invitation-list">
        <article v-for="item in pendingInvitations" :key="item.id" class="invitation-card">
          <div>
            <p><strong>赛事</strong> {{ item.tournamentName }}</p>
            <p><strong>球队</strong> {{ item.teamName }}</p>
            <p><strong>邀请说明</strong> 邀请你担任 {{ item.inviteRole }}</p>
            <AccountStatusBadge text="待处理" type="warning" />
          </div>
          <button type="button" class="account-btn-primary" @click="openInvitation(item)">处理邀请</button>
        </article>
      </div>
      <AccountEmptyState v-else message="暂无待处理球队邀请" />
    </AccountPanel>

    <AccountPanel title="球队管理状态">
      <dl class="account-info-list">
        <div class="account-field-row">
          <dt>当前管理球队</dt>
          <dd>{{ teamRelations?.managedTeamName || '无' }}</dd>
        </div>
        <div class="account-field-row">
          <dt>队长权限</dt>
          <dd>{{ teamRelations?.hasCaptainPermission ? '已获得' : '未获得' }}</dd>
        </div>
        <div class="account-field-row">
          <dt>创建申请</dt>
          <dd>
            {{
              teamRelations?.createRequestStatus === 'PENDING'
                ? '审核中'
                : teamRelations?.createRequestStatus === 'APPROVED'
                  ? '已通过'
                  : teamRelations?.createRequestStatus === 'REJECTED'
                    ? '未通过'
                    : '无'
            }}
          </dd>
        </div>
        <div class="account-field-row">
          <dt>创建功能</dt>
          <dd>{{ teamRelations?.createFeatureEnabled ? '已开放' : '未开放' }}</dd>
        </div>
      </dl>
      <div class="team-manage-actions">
        <button type="button" class="account-btn-primary" @click="showCreateDialog = true">创建球队</button>
        <button type="button" class="account-btn-secondary">了解球队流程</button>
      </div>
    </AccountPanel>

    <p class="team-tip">
      禁赛状态与赛事阵容关系分别管理。发生禁赛时，阵容关系不会自动删除。当前阵容关系不影响球队创建申请资格。
    </p>

    <Teleport to="body">
      <div v-if="showCreateDialog" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="showCreateDialog = false" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true" aria-label="创建球队申请">
          <h3>创建球队申请</h3>
          <p class="team-dialog__hint">提交后将等待管理员审核，不会直接授予队长权限。</p>
          <label>
            拟创建球队名称
            <input v-model="createForm.teamName" type="text" maxlength="32" />
          </label>
          <label>
            赛事平台
            <select v-model="createForm.platform">
              <option value="PC">PC</option>
              <option value="PS5">PlayStation 5</option>
              <option value="XBOX">Xbox</option>
              <option value="MOBILE">移动端</option>
            </select>
          </label>
          <label>
            说明
            <textarea v-model="createForm.description" rows="3" maxlength="200" />
          </label>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" @click="showCreateDialog = false">取消</button>
            <button type="button" class="account-btn-primary" @click="submitCreateRequest">提交申请</button>
          </div>
        </div>
      </div>
    </Teleport>

    <TeamInvitationDialog
      v-model="showInvitationDialog"
      :invitation="activeInvitation"
      @accept="handleAccept"
      @reject="handleReject"
    />
  </div>
</template>

<script lang="ts">
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
export default {
  components: { AccountStatusBadge },
}
</script>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.team-table-wrap {
  overflow-x: auto;
}

.team-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th,
  td {
    padding: 12px 10px;
    border-bottom: 1px solid rgba(20, 20, 20, 0.1);
    text-align: left;
  }

  th {
    font-size: 12px;
    color: #696969;
  }
}

.team-table__btn {
  min-height: 32px;
  padding-inline: 12px;
}

.invitation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.invitation-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;

  p {
    margin: 0 0 6px;
    font-size: 13px;
  }
}

.team-manage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.team-tip {
  margin: 0;
  font-size: 13px;
  color: #696969;
  line-height: 1.6;
}

.team-dialog {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
}

.team-dialog__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.team-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(480px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 8px;
  }

  label {
    display: block;
    margin-top: 14px;
    font-size: 13px;
    font-weight: 700;
  }

  input,
  select,
  textarea {
    width: 100%;
    margin-top: 6px;
    padding: 10px 12px;
    border: 1px solid var(--account-border);
    border-radius: 3px;
    font-family: inherit;
    box-sizing: border-box;
  }
}

.team-dialog__hint {
  margin: 0;
  font-size: 13px;
  color: #696969;
}

.team-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
</style>