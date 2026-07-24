<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { MOCK_TOURNAMENT_ID } from '@/mock/eventTeamMock'
import type { OpenApplicationStatus } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()

onMounted(async () => {
  await Promise.all([
    accountStore.loadMyTournaments().catch(() => undefined),
    accountStore.loadPlayerCertification().catch(() => undefined),
  ])
})

const tournaments = computed(() => accountStore.myTournaments)
const isVerified = computed(() => accountStore.isVerifiedPlayer)
const openTournament = computed(() => accountStore.openTournament)
const openApplication = computed(() => accountStore.openApplication)
const stats = computed(() => accountStore.openRegistrationStats)

const captainTournament = computed(() =>
  accountStore.myTournaments.find((item) => item.id === MOCK_TOURNAMENT_ID),
)

const showCaptainPanel = computed(
  () => isVerified.value && captainTournament.value?.role === '赛事队长',
)

const invited = computed(() => tournaments.value.filter((item) => item.status === 'INVITED'))

const openStatus = computed(() => openApplication.value?.status || 'NOT_SUBMITTED')

const bannerStatus = computed(() => {
  if (accountStore.tournamentsLoading) return '加载中'
  if (stats.value.pending > 0) return '有待审核'
  if (stats.value.rejected > 0) return '有未通过'
  if (stats.value.joined > 0) return '已参加赛事'
  if (stats.value.available > 0) return '可参与报名'
  return tournaments.value.length ? '已有参赛记录' : '暂无赛事'
})

const openAction = computed(() => {
  const map: Record<OpenApplicationStatus, { label: string; route: string } | null> = {
    NOT_SUBMITTED: { label: '立即报名', route: 'AccountOpenApplicationApply' },
    PENDING: { label: '查看审核进度', route: 'AccountOpenApplicationPending' },
    REJECTED: { label: '查看未通过原因', route: 'AccountOpenApplicationRejected' },
    APPROVED: { label: '查看报名结果', route: 'AccountOpenApplicationApproved' },
    CANCELLED: { label: '查看状态', route: 'AccountOpenApplicationStatusRedirect' },
    CLOSED: null,
  }
  return map[openStatus.value]
})

function goPlayerIdentity() {
  router.push('/account/player-identity')
}

function goOpen(routeName?: string) {
  if (!isVerified.value) {
    router.push('/account/player-identity')
    return
  }
  router.push({ name: routeName || openAction.value?.route || 'AccountOpenApplicationApply' })
}

function goEligibility() {
  router.push('/account/player-identity/eligibility')
}

function goTeamManagement() {
  router.push(`/account/my-tournaments/${MOCK_TOURNAMENT_ID}/team`)
}

function acceptInvite(id: string) {
  accountStore.acceptTournamentInvitation(id)
}

function rejectInvite(id: string) {
  accountStore.rejectTournamentInvitation(id)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="我的赛事"
      en-title="MY TOURNAMENTS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事' },
      ]"
    />

    <AccountStatusBanner
      title="赛事报名与参赛状态"
      description="集中管理可参与赛事、报名进度与球队邀请"
      :status-text="bannerStatus"
      :status-type="stats.rejected > 0 ? 'danger' : stats.pending > 0 ? 'warning' : 'info'"
    />

    <div class="tournament-stats">
      <div class="tournament-stats__item"><strong>{{ stats.joined }}</strong><span>已参加</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.pending }}</strong><span>审核中</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.rejected }}</strong><span>未通过</span></div>
      <div class="tournament-stats__item"><strong>{{ tournaments.length }}</strong><span>全部记录</span></div>
    </div>

    <AccountPanel v-if="!isVerified" title="先完成球员认证">
      <AccountEmptyState
        large
        message="尚未完成球员认证"
        subtitle="完成球员认证后，即可查看可参与赛事、接受球队邀请并提交报名"
      />
      <div class="account-panel__action">
        <button type="button" class="account-btn-primary" @click="goPlayerIdentity">前往球员认证</button>
      </div>
    </AccountPanel>

    <template v-else>
      <AccountPanel v-if="showCaptainPanel" title="本届球队 · 队长入口">
        <div class="open-tournament-card">
          <div class="open-tournament-card__head">
            <strong>{{ captainTournament?.name }}</strong>
            <AccountStatusBadge type="success" text="赛事队长" />
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">球队</span><span>{{ captainTournament?.teamName || '-' }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">身份</span><span>{{ captainTournament?.role }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">当前状态</span><span>{{ captainTournament?.statusText }}</span></div>
          </div>
          <div class="account-panel__action">
            <button type="button" class="account-btn-primary" @click="goTeamManagement">
              进入本届球队管理
            </button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="Open 个人报名">
        <div class="open-tournament-card">
          <div class="open-tournament-card__head">
            <strong>{{ openTournament.name }}</strong>
            <AccountStatusBadge
              :type="openTournament.status === 'REGISTRATION_OPEN' ? 'success' : 'warning'"
              :text="openTournament.statusText"
            />
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ openTournament.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">报名方式</span><span>{{ openTournament.registrationTypeText }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">报名截止</span><span>{{ openTournament.registrationDeadline }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">名额进度</span><span>{{ openTournament.quotaUsed }} / {{ openTournament.quotaTotal }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">报名状态</span><span>{{ openApplication?.statusText || '未提交' }}</span></div>
          </div>
          <div class="account-panel__action">
            <button
              v-if="openAction"
              type="button"
              class="account-btn-primary"
              @click="goOpen(openAction.route)"
            >
              {{ openAction.label }}
            </button>
            <button v-else type="button" class="account-btn-secondary" disabled>
              报名已关闭
            </button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel v-if="invited.length" title="待确认球队邀请">
        <div v-for="item in invited" :key="item.id" class="tournament-invite-card">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">赛事名称</span><span>{{ item.name }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">邀请球队</span><span>{{ item.teamName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">邀请身份</span><span>{{ item.role }}</span></div>
          </div>
          <div class="tournament-invite-card__actions">
            <button type="button" class="account-btn-primary" @click="acceptInvite(item.invitationId || '')">接受邀请</button>
            <button type="button" class="account-btn-secondary" @click="rejectInvite(item.invitationId || '')">拒绝邀请</button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="赛事列表">
        <div v-if="accountStore.tournamentsLoading" class="account-view__hint">加载赛事列表中...</div>
        <AccountEmptyState
          v-else-if="!tournaments.length"
          large
          message="暂无参赛记录"
          subtitle="完成球员认证并报名后，这里将展示你的赛事参赛状态"
        />
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>赛事名称</th>
                <th>赛季</th>
                <th>当前状态</th>
                <th>球队</th>
                <th>身份</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in tournaments" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ item.period || '-' }}</td>
                <td>{{ item.statusText }}</td>
                <td>{{ item.teamName || '-' }}</td>
                <td>{{ item.role || '-' }}</td>
                <td>
                  <button
                    v-if="item.id === MOCK_TOURNAMENT_ID && item.role === '赛事队长'"
                    type="button"
                    class="account-btn-primary"
                    @click="goTeamManagement"
                  >
                    管理球队
                  </button>
                  <button
                    v-else-if="item.eligibility === 'LIMITED'"
                    type="button"
                    class="account-btn-secondary"
                    @click="goEligibility"
                  >
                    查看资格状态
                  </button>
                  <button
                    v-else-if="item.status === 'INVITED'"
                    type="button"
                    class="account-btn-primary"
                    @click="acceptInvite(item.invitationId || '')"
                  >
                    接受邀请
                  </button>
                  <span v-else>-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </AccountPanel>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
