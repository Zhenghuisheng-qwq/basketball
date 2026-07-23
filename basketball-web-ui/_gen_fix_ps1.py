# -*- coding: utf-8 -*-
"""Generate fix-account-utf8.ps1 from agent transcript clean content."""
import json
import re
from pathlib import Path

ROOT = Path(r'd:\code\dz\basketball\scaffolding-v2\basketball-web-ui')
TRANSCRIPT_DIR = Path(
    r'C:\Users\Administrator\.cursor\projects\d-code-dz-basketball-scaffolding-v2\agent-transcripts'
)
OUT_PS1 = ROOT / 'fix-account-utf8.ps1'

TARGET_FILES = [
    'components/AuthStepper.vue',
    'components/PlayerVerificationForm.vue',
    'components/PlayerIdentityApplyForm.vue',
    'components/PlayerVerificationSummary.vue',
    'components/ProfileCompleteness.vue',
    'my-tournaments/index.vue',
    'my-tournaments/open/apply.vue',
    'my-tournaments/open/status.vue',
    'player-identity/index.vue',
    'player-identity/approved.vue',
    'player-identity/eligibility.vue',
    'player-identity/pending.vue',
    'player-identity/platforms.vue',
    'player-identity/rejected.vue',
    'player-verification/index.vue',
    'profile/index.vue',
    'security/index.vue',
    'settings/index.vue',
    'team-relations/index.vue',
    'notifications/index.vue',
    'RegisterVerifyEmail.vue',
    'RegisterSuccess.vue',
    'player-identity/change-history.vue',
    'player-identity/apply.vue',
    'player-identity/status.vue',
    'overview/index.vue',
    'components/TeamInvitationDialog.vue',
]

STATUS_VUE = r'''<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS, GAME_PLATFORM_LABELS } from '@/types/account'
import { identityQueryToVerificationStatus, isIdentityRestrictedQuery } from '@/types/account'
import { mapVerificationPlatform, mockRejectedApplication } from '@/mock/playerIdentityMock'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const props = defineProps<{
  statusMode: 'pending' | 'rejected' | 'approved'
}>()

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

watch(
  () => route.query.status,
  async (queryStatus) => {
    if (!import.meta.env.DEV || typeof queryStatus !== 'string') return
    const mapped = identityQueryToVerificationStatus(queryStatus)
    if (!mapped) return
    await accountStore.loadAccountMock(mapped, isIdentityRestrictedQuery(queryStatus))
  },
  { immediate: true },
)

onMounted(() => {
  const currentStatus = verification.value?.status
  if (props.statusMode === 'pending' && currentStatus !== 'PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'rejected' && currentStatus !== 'REJECTED') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'approved' && currentStatus !== 'APPROVED' && currentStatus !== 'CHANGE_PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
})

const platformLabel = computed(() => {
  const p = verification.value?.primaryPlatform
  if (!p) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(p)] || GAME_PLATFORM_LABELS[p]
})

const reviewRemark = computed(
  () => verification.value?.reviewerComment || mockRejectedApplication.reviewRemark,
)

const pageConfig = computed(() => {
  if (props.statusMode === 'pending') {
    return {
      zhTitle: '球员认证',
      enTitle: 'PLAYER VERIFICATION',
      crumb: '审核中',
      bannerTitle: '球员认证申请',
      bannerDesc: '认证资料已提交，当前由管理员审核',
      bannerStatus: '审核中',
      bannerType: 'warning' as const,
      steps: [
        { label: '提交资料', state: 'done' },
        { label: '管理员审核', state: 'current' },
        { label: '认证生效', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '本次认证资料',
      badgeType: 'warning' as const,
      badgeText: '审核中',
      footnote: '撤回后可以修改认证资料；再次提交将生成新的审核记录。',
    }
  }
  if (props.statusMode === 'rejected') {
    return {
      zhTitle: '球员认证',
      enTitle: 'PLAYER VERIFICATION',
      crumb: '未通过',
      bannerTitle: '球员认证未通过',
      bannerDesc: '请查看管理员备注，修改资料后重新提交',
      bannerStatus: '未通过',
      bannerType: 'danger' as const,
      steps: [
        { label: '提交资料', state: 'done' },
        { label: '管理员审核', state: 'failed', subLabel: '未通过' },
        { label: '认证生效', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '原提交资料',
      badgeType: 'danger' as const,
      badgeText: '未通过',
      footnote: '认证未通过时不会生成永久球员 ID；重新提交将建立新的审核记录。',
    }
  }
  return {
    zhTitle: '球员认证',
    enTitle: 'PLAYER VERIFICATION',
    crumb: '已通过',
    bannerTitle: '永久球员身份',
    bannerDesc: '球员认证已通过，认证资料长期有效',
    bannerStatus: '已认证',
    bannerType: 'success' as const,
    steps: [
      { label: '提交资料', state: 'done' },
      { label: '管理员审核', state: 'done' },
      { label: '认证生效', state: 'done' },
    ] as StepItem[],
    leftTitle: '有效认证资料',
    badgeType: 'success' as const,
    badgeText: '认证球员',
    footnote: '球员认证长期有效，无需按赛事重复认证，永久球员 ID 不可修改。',
  }
})

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showWithdraw.value = false
  ElMessage.success('已撤回申请，可修改资料后重新提交')
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goPlatforms() {
  router.push({ name: 'AccountPlatformGameId' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="pageConfig.zhTitle"
      :en-title="pageConfig.enTitle"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: pageConfig.crumb },
      ]"
    />

    <AccountStatusBanner
      :title="pageConfig.bannerTitle"
      :description="pageConfig.bannerDesc"
      :status-text="pageConfig.bannerStatus"
      :status-type="pageConfig.bannerType"
    />

    <AccountStepper :steps="pageConfig.steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="pageConfig.leftTitle">
        <div class="identity-review-profile">
          <div
            class="identity-review-profile__avatar"
            :class="{
              'identity-review-profile__avatar--success': statusMode === 'approved',
              'identity-review-profile__avatar--error': statusMode === 'rejected',
            }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge :type="pageConfig.badgeType" :text="pageConfig.badgeText" />
          </div>
        </div>
        <div class="account-info-list">
          <template v-if="statusMode === 'approved'">
            <div class="account-info-row"><span class="account-info-row__label">已绑定平台</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台状态</span><span class="account-status-text--success">已生效</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">认证状态</span><span class="account-status-text--success">长期有效</span></div>
          </template>
          <template v-else>
            <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 1 份</span></div>
            <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
          </template>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <div
          v-if="statusMode === 'pending'"
          class="identity-review-result identity-review-result--pending"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>球员认证审核中</strong>
          <AccountStatusBadge type="warning" text="审核中" />
          <p>管理员将对整份认证资料进行审核</p>
          <p>审核期间不能重复提交</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">撤回修改</button>
        </div>

        <div
          v-else-if="statusMode === 'rejected'"
          class="identity-review-result identity-review-result--failed"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>球员认证未通过</strong>
          <AccountStatusBadge type="danger" text="未通过" />
          <p class="identity-review-result__remark-label">管理员备注</p>
          <p class="identity-review-result__remark">{{ reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goApply">修改并重新提交</button>
        </div>

        <div
          v-else
          class="identity-review-result identity-review-result--success"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">永久球员 ID</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo }}</strong>
          <p>认证状态：已生效</p>
          <p>永久球员 ID 不可修改</p>
          <button type="button" class="account-btn-secondary" @click="goPlatforms">申请修改认证资料</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ pageConfig.footnote }}</p>

    <div v-if="statusMode === 'approved'" class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="goPlatforms">平台与游戏 ID 管理</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">资料变更记录</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">我的赛事</button>
    </div>

    <AccountConfirmDialog
      v-if="statusMode === 'pending'"
      v-model="showWithdraw"
      title="撤回认证申请"
      message="确认撤回当前审核中的认证申请？撤回后可修改资料并重新提交。"
      confirm-text="确认撤回"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'''

OVERVIEW_VUE = r'''<script setup lang="ts">
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
  accountStore.notifications.filter((item) =>
    ['欢迎加入 CNPA', '邮箱验证成功', '球员认证审核通过'].includes(item.title),
  ).slice(0, 3),
)

const joinedCount = computed(
  () => accountStore.myTournaments.filter((item) => item.status === 'JOINED').length,
)

const bannerStatusText = computed(() => {
  if (isVerified.value) return '认证球员'
  return '基础账号'
})

const bannerStatusExtra = computed(() =>
  account.value?.emailVerified ? '邮箱已验证' : '邮箱未验证',
)

const identityStatusText = computed(() => {
  const status = verification.value?.status
  if (status === 'PENDING') return '审核中'
  if (status === 'REJECTED') return '未通过'
  if (isVerified.value) return '已认证'
  return '未认证'
})

const playerIdText = computed(() =>
  isVerified.value && account.value?.playerNo ? account.value.playerNo : '未生成',
)

const pendingTasks = computed(() => {
  if (isVerified.value) {
    const invited = accountStore.myTournaments.filter((item) => item.status === 'INVITED').length
    if (invited > 0) {
      return {
        title: `你有 ${invited} 条赛事邀请`,
        action: '查看邀请',
        path: '/account/my-tournaments',
      }
    }
    return null
  }
  return {
    title: '完成球员认证',
    status: '待处理',
    desc: '认证通过后将生成永久球员 ID，并解锁赛事报名与球队加入。',
    action: '申请球员认证',
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
      zh-title="个人中心"
      en-title="ACCOUNT CENTER"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心' },
      ]"
    />

    <AccountStatusBanner
      :title="`欢迎回来，${account?.nickname}`"
      description="管理账号资料、认证状态与当前待办"
      :status-text="`${bannerStatusText} · ${bannerStatusExtra}`"
      :status-type="isVerified ? 'success' : 'warning'"
    />

    <div class="account-overview-grid">
      <AccountPanel title="球员身份状态">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">账号状态</span>
            <span class="account-info-row__value account-status-text--success">正常</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">邮箱状态</span>
            <span class="account-info-row__value account-status-text--success">已验证</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">球员身份</span>
            <span class="account-info-row__value">{{ identityStatusText }}</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">永久球员 ID</span>
            <span class="account-info-row__value">{{ playerIdText }}</span>
          </div>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="goPlayerIdentity">
            查看球员身份
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="我的赛事">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">已加入赛事</span>
            <span class="account-info-row__value">{{ joinedCount }} / 3</span>
          </div>
        </div>
        <p class="account-view__desc">{{ joinedCount ? `当前参与 ${joinedCount} 项赛事` : '暂未加入赛事' }}</p>
        <p class="account-view__hint">完成球员认证后，可报名 Open 个人赛或接受球队邀请。</p>
        <div v-if="isVerified" class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="go('/account/my-tournaments')">
            查看我的赛事
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="当前待办">
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
          <p>暂无待办事项</p>
        </div>
      </AccountPanel>

      <AccountPanel title="最近通知">
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
          <p>暂无通知</p>
        </div>
        <div class="account-notification-footer">
          <button type="button" class="account-btn-secondary" @click="go('/account/notifications')">
            查看全部通知
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
'''

REGISTER_SUCCESS = r'''<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail, getRegisterEmail } from '@/composables/useRegisterFlow'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()
const maskedEmail = computed(() => getMaskedRegisterEmail())

function goLogin() {
  router.push({
    path: '/login',
    query: { email: getRegisterEmail() },
  })
}

function goHome() {
  router.push('/home')
}
</script>

<template>
  <div class="auth-page auth-complete">
    <AuthStepper :current="3" />

    <header class="auth-register__title">
      <h1 class="auth-register__title-cn">注册完成</h1>
      <p class="auth-register__title-en">ACCOUNT READY</p>
    </header>

    <div class="auth-complete__icon" aria-hidden="true">
      <span class="auth-complete__check" />
    </div>

    <h2 class="auth-complete__heading">CNPA 账号创建成功</h2>
    <p class="auth-complete__email">绑定邮箱：{{ maskedEmail }}</p>

    <button type="button" class="auth-primary-button" @click="goLogin">立即登录</button>
    <button type="button" class="auth-secondary-button" @click="goHome">返回首页</button>

    <p class="auth-form__hint">登录后可进入个人中心并申请球员认证</p>

    <div class="auth-notice-strip">
      <span aria-hidden="true">✓</span>
      <span>你的 CNPA 账号已就绪，登录后即可管理个人资料与球员认证</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>
'''


def extract_from_transcripts() -> dict[str, str]:
    files: dict[str, str] = {}
    for jsonl in TRANSCRIPT_DIR.rglob('*.jsonl'):
        with open(jsonl, 'r', encoding='utf-8') as f:
            for line in f:
                try:
                    obj = json.loads(line)
                except json.JSONDecodeError:
                    continue
                for part in obj.get('message', {}).get('content', []):
                    if part.get('type') != 'tool_use' or part.get('name') != 'Write':
                        continue
                    path = part.get('input', {}).get('path', '').replace('\\', '/')
                    if 'views/account/' not in path or not path.endswith('.vue'):
                        continue
                    rel = path.split('views/account/')[-1]
                    content = part.get('input', {}).get('contents', '')
                    if not content or re.search(r'\?{3,}', content) or '\ufffd' in content:
                        continue
                    if rel not in files or len(content) > len(files[rel]):
                        files[rel] = content
    return files


def postprocess(rel: str, content: str) -> str:
    content = content.replace(
        "{ label: '个人中心', to: '/account' }",
        "{ label: '个人中心', to: '/account/overview' }",
    )
    content = re.sub(r'(<AccountStatusBadge[^>]*?) label=', r'\1 text=', content)
    content = re.sub(r'(<AccountStatusBadge[^>]*?):label=', r'\1:text=', content)

    if rel == 'components/PlayerVerificationForm.vue':
        if 'showCancel' not in content:
            content = content.replace(
                '  submitLabel?: string\n}>()',
                '  submitLabel?: string\n  showCancel?: boolean\n}>()',
            )
        content = content.replace(
            'class="verification-form"',
            'class="player-verification-form verification-form"',
        )
        content = content.replace(
            '<button v-if="$attrs.onCancel"',
            '<button v-if="showCancel"',
        )
        hint = '      <p class="verification-form__hint">请上传清晰的游戏 ID 截图，需包含平台昵称与 ID 信息</p>\n'
        if hint.strip() not in content and 'verification-form__hint' not in content.split('gameIdScreenshotUrl')[1][:400]:
            content = content.replace(
                '      <label v-else class="verification-form__upload">',
                hint + '      <label v-else class="verification-form__upload">',
            )
        style_idx = content.find('<style scoped')
        if style_idx >= 0:
            content = content[:style_idx] + '''<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.verification-form__field--error input,
.verification-form__field--error select {
  border-color: var(--account-danger);
}

.verification-form__preview img {
  max-width: 100%;
  max-height: 200px;
  border: 1px solid rgba(20, 20, 20, 0.15);
  border-radius: 3px;
}
</style>
'''

    return content


def apply_ts_fixes(rel: str, content: str) -> str:
    if rel == 'player-identity/index.vue':
        if 'const isDev' not in content:
            content = content.replace(
                'const accountStore = useAccountStore()',
                'const accountStore = useAccountStore()\nconst isDev = import.meta.env.DEV',
            )
        content = content.replace('v-if="import.meta.env.DEV"', 'v-if="isDev"')
        content = content.replace(
            'const identity = computed(() => accountStore.playerIdentity)\nconst verification = computed(() => accountStore.verification)\n',
            'const identity = computed(() => accountStore.playerIdentity)\n',
        )

    if rel == 'my-tournaments/open/apply.vue':
        content = content.replace('import { useRoute, useRouter }', 'import { useRouter }')
        content = content.replace('const route = useRoute()\n', '')

    if rel == 'my-tournaments/open/status.vue':
        content = content.replace(
            "import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'\n", ''
        )
        content = content.replace("import type { OpenApplicationStatus } from '@/types/account'\n", '')

    if rel == 'player-identity/apply.vue':
        content = re.sub(
            r'\nfunction syncDraft\(payload: Partial<typeof draft>\) \{\n  Object\.assign\(draft, payload\)\n\}',
            '',
            content,
        )

    if rel == 'player-verification/index.vue':
        content = content.replace(
            'const account = computed(() => accountStore.currentAccount)\n', ''
        )

    if rel == 'team-relations/index.vue':
        content = content.replace("import { formatDateTime } from '@/utils/formatDate'\n", '')

    return content


def is_corrupted(content: str) -> bool:
    return bool(re.search(r'\?{3,}', content)) or '\ufffd' in content


def main() -> None:
    extracted = extract_from_transcripts()

    auth_verify = ROOT / '_utf8_restore' / 'RegisterVerifyEmail-auth.vue'
    if auth_verify.exists():
        extracted['RegisterVerifyEmail.vue'] = auth_verify.read_text(encoding='utf-8')

    extracted['RegisterSuccess.vue'] = REGISTER_SUCCESS
    extracted['player-identity/status.vue'] = STATUS_VUE
    extracted['overview/index.vue'] = OVERVIEW_VUE

    team_dialog = ROOT / '_utf8_restore' / 'components' / 'TeamInvitationDialog.vue'
    if team_dialog.exists():
        extracted['components/TeamInvitationDialog.vue'] = team_dialog.read_text(encoding='utf-8')

    lines = [
        '# fix-account-utf8.ps1 - Restore corrupted Chinese UTF-8 in account Vue files',
        '$ErrorActionPreference = "Stop"',
        f'$root = "{ROOT.as_posix()}"',
        '$utf8 = New-Object System.Text.UTF8Encoding $false',
        '$fixed = @()',
        '',
    ]

    for rel in TARGET_FILES:
        if rel not in extracted:
            print(f'WARNING: missing content for {rel}')
            continue
        content = apply_ts_fixes(rel, postprocess(rel, extracted[rel]))
        if is_corrupted(content):
            print(f'WARNING: {rel} still has corruption after postprocess')
        var_name = 'content_' + rel.replace('/', '_').replace('-', '_').replace('.', '_')
        lines.append(f'# --- {rel} ---')
        lines.append(f'${var_name} = @\'')
        lines.append(content.rstrip('\n'))
        lines.append('\'@')
        lines.append(f'$path = Join-Path $root "src\\views\\account\\{rel.replace("/", chr(92))}"')
        lines.append(f'[System.IO.File]::WriteAllText($path, ${var_name}, $utf8)')
        lines.append(f'$fixed += "{rel}"')
        lines.append('')

    lines.extend([
        'Write-Host "Fixed $($fixed.Count) files:"',
        '$fixed | ForEach-Object { Write-Host "  $_" }',
    ])

    # UTF-8 BOM required so Windows PowerShell parses Chinese in here-strings correctly
    OUT_PS1.write_text('\n'.join(lines) + '\n', encoding='utf-8-sig')
    print(f'Generated {OUT_PS1} with {len([l for l in lines if l.startswith("$content_")])} file blocks')


if __name__ == '__main__':
    main()
