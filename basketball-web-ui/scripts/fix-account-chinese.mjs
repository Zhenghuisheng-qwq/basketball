import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve('src/views/account')

function u(parts) {
  return parts
}

function decode(source) {
  return source.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
}

function writeVue(rel, source) {
  const file = path.join(root, rel)
  const content = decode(source)
  fs.writeFileSync(file, content, 'utf8')
  const ok = !content.includes('????') && fs.readFileSync(file, 'utf8').includes(content.slice(0, 20))
  console.log(rel, content.includes('\\u') ? 'still-escaped' : 'ok', 'len=' + content.length)
  return content
}

const overview = writeVue('overview/index.vue', `<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { useAccountStore } from '@/stores/account'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const recentNotifications = computed(() =>
  accountStore.notifications
    .filter((item) =>
      ['\\u6b22\\u8fce\\u52a0\\u5165 CNPA', '\\u90ae\\u7bb1\\u9a8c\\u8bc1\\u6210\\u529f', '\\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1\\u540e\\u5373\\u53ef\\u53c2\\u4e0e\\u8d5b\\u4e8b'].includes(item.title),
    )
    .slice(0, 3),
)

const joinedCount = computed(
  () => accountStore.myTournaments.filter((item) => item.status === 'JOINED').length,
)

const bannerStatusText = computed(() => {
  if (isVerified.value) return '\\u8ba4\\u8bc1\\u7403\\u5458'
  return '\\u7403\\u5458\\u672a\\u8ba4\\u8bc1'
})

const bannerStatusExtra = computed(() =>
  account.value?.emailVerified ? '\\u90ae\\u7bb1\\u5df2\\u9a8c\\u8bc1' : '\\u90ae\\u7bb1\\u672a\\u9a8c\\u8bc1',
)

const identityStatusText = computed(() => {
  const status = verification.value?.status
  if (status === 'PENDING') return '\\u5ba1\\u6838\\u4e2d'
  if (status === 'REJECTED') return '\\u672a\\u901a\\u8fc7'
  if (isVerified.value) return '\\u5df2\\u8ba4\\u8bc1'
  return '\\u672a\\u8ba4\\u8bc1'
})

const playerIdText = computed(() =>
  isVerified.value && account.value?.playerNo ? account.value.playerNo : '\\u5c1a\\u672a\\u751f\\u6210',
)

const pendingTasks = computed(() => {
  if (isVerified.value) {
    const invited = accountStore.myTournaments.filter((item) => item.status === 'INVITED').length
    if (invited > 0) {
      return {
        title: \`\\u6536\\u5230 \${invited} \\u6761\\u7403\\u961f\\u9080\\u8bf7\`,
        action: '\\u524d\\u5f80\\u5904\\u7406',
        path: '/account/my-events?tab=applications',
      }
    }
    return null
  }
  return {
    title: '\\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1',
    status: '\\u5f85\\u5904\\u7406',
    desc: '\\u8ba4\\u8bc1\\u540e\\u53ef\\u62a5\\u540d\\u8d5b\\u4e8b\\u3001\\u63a5\\u53d7\\u7403\\u961f\\u9080\\u8bf7\\u5e76\\u8fdb\\u5165\\u6b63\\u5f0f\\u9635\\u5bb9',
    action: '\\u7533\\u8bf7\\u7403\\u5458\\u8ba4\\u8bc1',
    path: '/account/player-identity/apply',
  }
})

function go(path: string) {
  router.push(path)
}

function goPlayerIdentity() {
  router.push(resolvePlayerIdentityRoute(accountStore))
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="\\u4e2a\\u4eba\\u4e2d\\u5fc3"
      en-title="ACCOUNT CENTER"
      :breadcrumb-items="[
        { label: '\\u9996\\u9875', to: '/home' },
        { label: '\\u4e2a\\u4eba\\u4e2d\\u5fc3' },
      ]"
    />

    <AccountStatusBanner
      :title="\`\\u6b22\\u8fce\\u56de\\u6765\\uff0c\${account?.nickname}\`"
      description="\\u7ba1\\u7406\\u8d26\\u53f7\\u8d44\\u6599\\u3001\\u8ba4\\u8bc1\\u72b6\\u6001\\u4e0e\\u5f53\\u524d\\u5f85\\u529e"
      :status-text="\`\${bannerStatusText} \\u00b7 \${bannerStatusExtra}\`"
      :status-type="isVerified ? 'success' : 'warning'"
    />

    <div class="account-overview-grid">
      <AccountPanel title="\\u8d26\\u53f7\\u4e0e\\u8eab\\u4efd">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">\\u8d26\\u53f7\\u72b6\\u6001</span>
            <span class="account-info-row__value account-status-text--success">\\u6b63\\u5e38</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">\\u90ae\\u7bb1\\u72b6\\u6001</span>
            <span class="account-info-row__value account-status-text--success">\\u5df2\\u9a8c\\u8bc1</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">\\u7403\\u5458\\u8eab\\u4efd</span>
            <span class="account-info-row__value">{{ identityStatusText }}</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">\\u6c38\\u4e45\\u7403\\u5458 ID</span>
            <span class="account-info-row__value">{{ playerIdText }}</span>
          </div>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="goPlayerIdentity">
            \\u67e5\\u770b\\u7403\\u5458\\u8eab\\u4efd
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="\\u5f53\\u524d\\u8d5b\\u4e8b">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">\\u5df2\\u53c2\\u52a0\\u8d5b\\u4e8b</span>
            <span class="account-info-row__value">{{ joinedCount }} / 3</span>
          </div>
        </div>
        <p class="account-view__desc">
          {{ joinedCount ? \`\\u5df2\\u53c2\\u52a0 \${joinedCount} \\u573a\\u8d5b\\u4e8b\` : '\\u6682\\u65e0\\u5df2\\u53c2\\u52a0\\u8d5b\\u4e8b' }}
        </p>
        <p class="account-view__hint">
          \\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1\\u540e\\uff0c\\u53ef\\u5728\\u201c\\u6211\\u7684\\u8d5b\\u4e8b\\u201d\\u67e5\\u770b\\u7b26\\u5408\\u8d44\\u683c\\u7684\\u8d5b\\u4e8b
        </p>
        <div v-if="isVerified" class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="go('/account/my-events?tab=joined')">
            \\u67e5\\u770b\\u6211\\u7684\\u8d5b\\u4e8b
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="\\u5f85\\u5904\\u7406\\u4e8b\\u9879">
        <template v-if="pendingTasks">
          <p class="account-view__desc">{{ pendingTasks.title }}</p>
          <p v-if="pendingTasks.status" class="account-pending-tag">{{ pendingTasks.status }}</p>
          <p v-if="pendingTasks.desc" class="account-view__hint">{{ pendingTasks.desc }}</p>
          <div class="account-panel__action">
            <button type="button" class="account-btn-primary" @click="go(pendingTasks.path)">
              {{ pendingTasks.action }}
            </button>
          </div>
        </template>
        <div v-else class="account-pending-empty">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
          </svg>
          <p>\\u6682\\u65e0\\u5f85\\u529e\\u4e8b\\u9879</p>
        </div>
      </AccountPanel>

      <AccountPanel title="\\u6700\\u8fd1\\u901a\\u77e5">
        <ul v-if="recentNotifications.length" class="account-notice-list">
          <li v-for="item in recentNotifications" :key="item.id">
            <span
              class="account-notice-list__dot"
              :class="{ 'account-notice-list__dot--unread': !item.read }"
            />
            <div>
              <strong>{{ item.title }}</strong>
              <time>{{ formatDateTime(item.createdAt) }}</time>
            </div>
          </li>
        </ul>
        <div v-else class="account-notification-empty">
          <p>\\u6682\\u65e0\\u901a\\u77e5</p>
        </div>
        <div class="account-notification-footer">
          <button type="button" class="account-btn-secondary" @click="go('/account/notifications')">
            \\u67e5\\u770b\\u5168\\u90e8\\u901a\\u77e5
          </button>
        </div>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view__desc {
  margin: 0 0 8px;
  font-size: clamp(18px, 1.6vw, 22px);
  font-weight: 800;
  color: var(--account-text);
}

.account-view__hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #696969;
  line-height: 1.6;
}

.account-pending-tag {
  display: inline-block;
  margin: 0 0 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(196, 30, 58, 0.12);
  color: var(--account-danger);
  font-size: 12px;
  font-weight: 800;
}
</style>
`)

writeVue('my-tournaments/index.vue', `<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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

const tournaments = computed(() => accountStore.myTournaments)
const isVerified = computed(() => accountStore.isVerifiedPlayer)
const openTournament = computed(() => accountStore.openTournament)
const openApplication = computed(() => accountStore.openApplication)
const stats = computed(() => accountStore.openRegistrationStats)

const captainTournament = computed(() =>
  accountStore.myTournaments.find((item) => item.id === MOCK_TOURNAMENT_ID),
)

const showCaptainPanel = computed(
  () => isVerified.value && captainTournament.value?.role === '\\u8d5b\\u4e8b\\u961f\\u957f',
)

const invited = computed(() => tournaments.value.filter((item) => item.status === 'INVITED'))

const openStatus = computed(() => openApplication.value?.status || 'NOT_SUBMITTED')

const bannerStatus = computed(() => {
  if (stats.value.pending > 0) return '\\u6709\\u5f85\\u5ba1\\u6838'
  if (stats.value.rejected > 0) return '\\u6709\\u672a\\u901a\\u8fc7'
  if (stats.value.joined > 0) return '\\u5df2\\u53c2\\u52a0\\u8d5b\\u4e8b'
  if (stats.value.available > 0) return '\\u53ef\\u53c2\\u4e0e\\u62a5\\u540d'
  return '\\u6682\\u65e0\\u8d5b\\u4e8b'
})

const openAction = computed(() => {
  const map: Record<OpenApplicationStatus, { label: string; route: string } | null> = {
    NOT_SUBMITTED: { label: '\\u7acb\\u5373\\u62a5\\u540d', route: 'AccountOpenApplicationApply' },
    PENDING: { label: '\\u67e5\\u770b\\u5ba1\\u6838\\u8fdb\\u5ea6', route: 'AccountOpenApplicationPending' },
    REJECTED: { label: '\\u67e5\\u770b\\u672a\\u901a\\u8fc7\\u539f\\u56e0', route: 'AccountOpenApplicationRejected' },
    APPROVED: { label: '\\u67e5\\u770b\\u62a5\\u540d\\u7ed3\\u679c', route: 'AccountOpenApplicationApproved' },
    CANCELLED: { label: '\\u67e5\\u770b\\u72b6\\u6001', route: 'AccountOpenApplicationStatusRedirect' },
    CLOSED: null,
  }
  return map[openStatus.value]
})

function goPlayerIdentity() {
  router.push('/account/player-identity')
}

function goOpen(routeName?: string) {
  if (!isVerified.value) {
    ElMessage.warning('\\u8bf7\\u5148\\u5b8c\\u6210\\u7403\\u5458\\u8eab\\u4efd\\u8ba4\\u8bc1')
    router.push('/account/player-identity')
    return
  }
  const block = accountStore.getOpenRegistrationBlockReason()
  if (openStatus.value === 'NOT_SUBMITTED' && block) {
    ElMessage.warning(block)
    if (block.includes('\\u8d44\\u683c')) router.push('/account/player-identity/eligibility')
    return
  }
  router.push({ name: routeName || openAction.value?.route || 'AccountOpenApplicationApply' })
}

function goEligibility() {
  router.push('/account/player-identity/eligibility')
}

function goTeamManagement() {
  router.push(\`/account/my-tournaments/\${MOCK_TOURNAMENT_ID}/team\`)
}

function acceptInvite(id: string) {
  if (!isVerified.value) {
    ElMessage.warning('\\u8bf7\\u5148\\u5b8c\\u6210\\u7403\\u5458\\u8eab\\u4efd\\u8ba4\\u8bc1')
    return
  }
  accountStore.acceptTournamentInvitation(id)
}

function rejectInvite(id: string) {
  accountStore.rejectTournamentInvitation(id)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="\\u6211\\u7684\\u8d5b\\u4e8b"
      en-title="MY TOURNAMENTS"
      :breadcrumb-items="[
        { label: '\\u9996\\u9875', to: '/home' },
        { label: '\\u4e2a\\u4eba\\u4e2d\\u5fc3', to: '/account/overview' },
        { label: '\\u6211\\u7684\\u8d5b\\u4e8b' },
      ]"
    />

    <AccountStatusBanner
      title="\\u8d5b\\u4e8b\\u62a5\\u540d\\u4e0e\\u53c2\\u8d5b\\u72b6\\u6001"
      description="\\u96c6\\u4e2d\\u7ba1\\u7406\\u53ef\\u53c2\\u4e0e\\u8d5b\\u4e8b\\u3001\\u62a5\\u540d\\u8fdb\\u5ea6\\u4e0e\\u7403\\u961f\\u9080\\u8bf7"
      :status-text="bannerStatus"
      :status-type="stats.rejected > 0 ? 'danger' : stats.pending > 0 ? 'warning' : 'info'"
    />

    <div class="tournament-stats">
      <div class="tournament-stats__item"><strong>{{ stats.joined }}</strong><span>\\u5df2\\u53c2\\u52a0</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.pending }}</strong><span>\\u5ba1\\u6838\\u4e2d</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.rejected }}</strong><span>\\u672a\\u901a\\u8fc7</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.available }}</strong><span>\\u53ef\\u62a5\\u540d</span></div>
    </div>

    <AccountPanel v-if="!isVerified" title="\\u5148\\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1">
      <AccountEmptyState
        large
        message="\\u5c1a\\u672a\\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1"
        subtitle="\\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1\\u540e\\uff0c\\u5373\\u53ef\\u67e5\\u770b\\u53ef\\u53c2\\u4e0e\\u8d5b\\u4e8b\\u3001\\u63a5\\u53d7\\u7403\\u961f\\u9080\\u8bf7\\u5e76\\u63d0\\u4ea4\\u62a5\\u540d"
      />
      <div class="account-panel__action">
        <button type="button" class="account-btn-primary" @click="goPlayerIdentity">\\u524d\\u5f80\\u7403\\u5458\\u8ba4\\u8bc1</button>
      </div>
    </AccountPanel>

    <template v-else>
      <AccountPanel v-if="showCaptainPanel" title="\\u672c\\u5c4a\\u7403\\u961f \\u00b7 \\u961f\\u957f\\u5165\\u53e3">
        <div class="open-tournament-card">
          <div class="open-tournament-card__head">
            <strong>{{ captainTournament?.name }}</strong>
            <AccountStatusBadge type="success" text="\\u8d5b\\u4e8b\\u961f\\u957f" />
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">\\u7403\\u961f</span><span>{{ captainTournament?.teamName || 'RNG' }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u8eab\\u4efd</span><span>{{ captainTournament?.role }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u5f53\\u524d\\u72b6\\u6001</span><span>{{ captainTournament?.statusText }}</span></div>
          </div>
          <div class="account-panel__action">
            <button type="button" class="account-btn-primary" @click="goTeamManagement">
              \\u8fdb\\u5165\\u672c\\u5c4a\\u7403\\u961f\\u7ba1\\u7406
            </button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="Open \\u4e2a\\u4eba\\u62a5\\u540d">
        <div class="open-tournament-card">
          <div class="open-tournament-card__head">
            <strong>{{ openTournament.name }}</strong>
            <AccountStatusBadge
              :type="openTournament.status === 'REGISTRATION_OPEN' ? 'success' : 'warning'"
              :text="openTournament.statusText"
            />
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">\\u5e73\\u53f0</span><span>{{ openTournament.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u62a5\\u540d\\u65b9\\u5f0f</span><span>{{ openTournament.registrationTypeText }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u62a5\\u540d\\u622a\\u6b62</span><span>{{ openTournament.registrationDeadline }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u540d\\u989d\\u8fdb\\u5ea6</span><span>{{ openTournament.quotaUsed }} / {{ openTournament.quotaTotal }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u62a5\\u540d\\u72b6\\u6001</span><span>{{ openApplication?.statusText || '\\u672a\\u63d0\\u4ea4' }}</span></div>
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
            <button
              v-else
              type="button"
              class="account-btn-secondary"
              disabled
            >
              \\u62a5\\u540d\\u5df2\\u5173\\u95ed
            </button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel v-if="invited.length" title="\\u5f85\\u786e\\u8ba4\\u7403\\u961f\\u9080\\u8bf7">
        <div v-for="item in invited" :key="item.id" class="tournament-invite-card">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">\\u8d5b\\u4e8b\\u540d\\u79f0</span><span>{{ item.name }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u9080\\u8bf7\\u7403\\u961f</span><span>{{ item.teamName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u9080\\u8bf7\\u8eab\\u4efd</span><span>{{ item.role }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u9080\\u8bf7\\u65f6\\u95f4</span><span>{{ item.inviteTime }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">\\u6709\\u6548\\u671f\\u81f3</span><span>{{ item.inviteExpires }}</span></div>
          </div>
          <div class="tournament-invite-card__actions">
            <button type="button" class="account-btn-primary" @click="acceptInvite(item.invitationId || '')">\\u63a5\\u53d7\\u9080\\u8bf7</button>
            <button type="button" class="account-btn-secondary" @click="rejectInvite(item.invitationId || '')">\\u62d2\\u7edd\\u9080\\u8bf7</button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="\\u8d5b\\u4e8b\\u5217\\u8868">
        <AccountEmptyState
          v-if="tournaments.filter((item) => item.id !== openTournament.id).length === 0 && openStatus !== 'APPROVED'"
          large
          message="\\u6682\\u65e0\\u5176\\u4ed6\\u53c2\\u8d5b\\u8bb0\\u5f55"
          subtitle="\\u5b8c\\u6210\\u7403\\u5458\\u8ba4\\u8bc1\\u5e76\\u62a5\\u540d\\u540e\\uff0c\\u8fd9\\u91cc\\u5c06\\u5c55\\u793a\\u4f60\\u7684\\u8d5b\\u4e8b\\u53c2\\u8d5b\\u72b6\\u6001"
        />
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>\\u8d5b\\u4e8b\\u540d\\u79f0</th>
                <th>\\u8d5b\\u4e8b\\u7c7b\\u578b</th>
                <th>\\u8d5b\\u5b63\\u5468\\u671f</th>
                <th>\\u5f53\\u524d\\u72b6\\u6001</th>
                <th>\\u7403\\u961f</th>
                <th>\\u8eab\\u4efd</th>
                <th>\\u64cd\\u4f5c</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in tournaments" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ item.type || '-' }}</td>
                <td>{{ item.period || '-' }}</td>
                <td>{{ item.statusText }}</td>
                <td>{{ item.teamName || '-' }}</td>
                <td>{{ item.role || '-' }}</td>
                <td>
                  <button
                    v-if="item.id === openTournament.id"
                    type="button"
                    class="account-btn-secondary"
                    @click="goOpen()"
                  >
                    {{ openAction?.label || '\\u67e5\\u770b' }}
                  </button>
                  <button
                    v-else-if="item.id === MOCK_TOURNAMENT_ID && item.role === '\\u8d5b\\u4e8b\\u961f\\u957f'"
                    type="button"
                    class="account-btn-primary"
                    @click="goTeamManagement"
                  >
                    \\u7ba1\\u7406\\u7403\\u961f
                  </button>
                  <button
                    v-else-if="item.eligibility === 'LIMITED'"
                    type="button"
                    class="account-btn-secondary"
                    @click="goEligibility"
                  >
                    \\u67e5\\u770b\\u8d44\\u683c\\u72b6\\u6001
                  </button>
                  <button
                    v-else-if="item.status === 'INVITED'"
                    type="button"
                    class="account-btn-primary"
                    @click="acceptInvite(item.invitationId || '')"
                  >
                    \\u63a5\\u53d7\\u9080\\u8bf7
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
`)

writeVue('security/index.vue', `<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAccountStore } from '@/stores/account'
import { validatePasswordRules } from '@/mock/account/security'
import { formatDateTime } from '@/utils/formatDate'

const accountStore = useAccountStore()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const errors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const loading = ref(false)

const profile = computed(() => accountStore.profile)
const security = computed(() => accountStore.security)
const account = computed(() => accountStore.currentAccount)

function resetErrors() {
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
}

function validateForm(): boolean {
  resetErrors()
  if (!form.currentPassword) errors.currentPassword = '\\u8bf7\\u8f93\\u5165\\u5f53\\u524d\\u5bc6\\u7801'
  const ruleError = validatePasswordRules(form.newPassword)
  if (ruleError) errors.newPassword = ruleError
  if (!form.confirmPassword) errors.confirmPassword = '\\u8bf7\\u786e\\u8ba4\\u65b0\\u5bc6\\u7801'
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = '\\u4e24\\u6b21\\u5bc6\\u7801\\u8f93\\u5165\\u4e0d\\u4e00\\u81f4'
  return !errors.currentPassword && !errors.newPassword && !errors.confirmPassword
}

function cancelForm() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  resetErrors()
}

async function savePassword() {
  if (loading.value || !validateForm()) return
  loading.value = true
  try {
    const error = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (error) {
      if (error.includes('\\u5bc6\\u7801')) errors.currentPassword = error
      else errors.newPassword = error
      return
    }
    ElMessage.success('\\u5bc6\\u7801\\u5df2\\u66f4\\u65b0')
    cancelForm()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      zh-title="\\u8d26\\u53f7\\u5b89\\u5168"
      en-title="ACCOUNT SECURITY"
      :breadcrumb-items="[
        { label: '\\u9996\\u9875', to: '/home' },
        { label: '\\u4e2a\\u4eba\\u4e2d\\u5fc3', to: '/account/overview' },
        { label: '\\u8d26\\u53f7\\u5b89\\u5168' },
      ]"
    />

    <AccountStatusBanner
      title="\\u4fdd\\u62a4\\u4f60\\u7684 CNPA \\u8d26\\u53f7"
      description="\\u7ba1\\u7406\\u767b\\u5f55\\u90ae\\u7bb1\\u4e0e\\u8d26\\u53f7\\u5bc6\\u7801"
      status-text="\\u8d26\\u53f7\\u72b6\\u6001\\u6b63\\u5e38"
      status-type="success"
    />

    <div class="security-layout">
      <AccountPanel title="\\u4fee\\u6539\\u5bc6\\u7801">
        <form class="security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="current-password">\\u5f53\\u524d\\u5bc6\\u7801</label>
            <PasswordInput
              id="current-password"
              v-model="form.currentPassword"
              autocomplete="current-password"
            />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-password">\\u65b0\\u5bc6\\u7801</label>
            <PasswordInput
              id="new-password"
              v-model="form.newPassword"
              autocomplete="new-password"
            />
            <p class="security-form__hint">8 \\u81f3 20 \\u4f4d\\uff0c\\u9700\\u5305\\u542b\\u5b57\\u6bcd\\u4e0e\\u6570\\u5b57</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="confirm-password">\\u786e\\u8ba4\\u65b0\\u5bc6\\u7801</label>
            <PasswordInput
              id="confirm-password"
              v-model="form.confirmPassword"
              autocomplete="new-password"
            />
            <p v-if="errors.confirmPassword" class="security-form__error">{{ errors.confirmPassword }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="loading">\\u4fdd\\u5b58\\u65b0\\u5bc6\\u7801</button>
            <button type="button" class="account-btn-secondary" @click="cancelForm">\\u53d6\\u6d88</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel title="\\u8d26\\u53f7\\u4fe1\\u606f">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>\\u6ce8\\u518c\\u90ae\\u7bb1</dt><dd>{{ profile?.maskedEmail }}</dd></div>
          <div class="account-field-row">
            <dt>\\u90ae\\u7bb1\\u72b6\\u6001</dt>
            <dd class="account-status-text--success">\\u5df2\\u9a8c\\u8bc1</dd>
          </div>
          <div class="account-field-row">
            <dt>\\u8d26\\u53f7\\u72b6\\u6001</dt>
            <dd class="account-status-text--success">\\u6b63\\u5e38</dd>
          </div>
          <div class="account-field-row">
            <dt>\\u6700\\u8fd1\\u767b\\u5f55</dt>
            <dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd>
          </div>
          <div class="account-field-row">
            <dt>\\u5bc6\\u7801\\u66f4\\u65b0\\u65f6\\u95f4</dt>
            <dd>{{ formatDateTime(security?.passwordUpdatedAt || '') }}</dd>
          </div>
        </dl>
        <p class="security-form__hint">\\u5982\\u975e\\u672c\\u4eba\\u64cd\\u4f5c\\uff0c\\u8bf7\\u7acb\\u5373\\u4fee\\u6539\\u5bc6\\u7801\\u5e76\\u8054\\u7cfb\\u7ba1\\u7406\\u5458</p>
      </AccountPanel>
    </div>

    <p class="security-tip">\\u8bf7\\u52ff\\u5411\\u4efb\\u4f55\\u4eba\\u900f\\u9732\\u5bc6\\u7801\\u6216\\u90ae\\u7bb1\\u9a8c\\u8bc1\\u7801</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-page {
  width: 100%;
  min-width: 0;
}

.security-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.security-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-form__field {
  display: flex;
  flex-direction: column;
  gap: 8px;

  label {
    font-size: 14px;
    font-weight: 700;
  }
}

.security-form__hint {
  margin: 0;
  font-size: 12px;
  color: #777;
}

.security-form__error {
  margin: 0;
  font-size: 12px;
  color: var(--account-danger);
}

.security-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.security-tip {
  margin: 18px 0 0;
  font-size: 13px;
  color: #777;
}

@media (max-width: 899px) {
  .security-layout {
    grid-template-columns: 1fr;
  }
}
</style>
`)

const overviewCheck = fs.readFileSync(path.join(root, 'overview/index.vue'), 'utf8')
const tourneyCheck = fs.readFileSync(path.join(root, 'my-tournaments/index.vue'), 'utf8')
const securityCheck = fs.readFileSync(path.join(root, 'security/index.vue'), 'utf8')
console.log('overview has CN:', overviewCheck.includes('\u4e2a\u4eba\u4e2d\u5fc3'))
console.log('tournaments has CN:', tourneyCheck.includes('\u6211\u7684\u8d5b\u4e8b'))
console.log('security has CN:', securityCheck.includes('\u8d26\u53f7\u5b89\u5168'))
console.log('any ???? left:', [overviewCheck, tourneyCheck, securityCheck].some((t) => t.includes('????')))
void u
