import fs from 'node:fs'
import path from 'node:path'

const root = path.resolve('src/views/account')

function decode(source) {
  return source.replace(/\\u([0-9a-fA-F]{4})/g, (_, hex) => String.fromCharCode(parseInt(hex, 16)))
}

function writeVue(rel, source) {
  const file = path.join(root, rel)
  const content = decode(source)
  fs.writeFileSync(file, content, 'utf8')
  const hasQ = content.includes('????')
  const ok =
    rel.includes('security')
      ? content.includes('\u8d26\u53f7\u5b89\u5168')
      : content.includes('\u7403\u5458\u8eab\u4efd')
  console.log(rel, hasQ ? 'BAD' : 'ok', 'cn=', ok, 'len=' + content.length)
}

writeVue(
  'security/index.vue',
  `<script setup lang="ts">
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
  <div class="account-view">
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
      status-text="\\u5b89\\u5168\\u72b6\\u6001\\u6b63\\u5e38"
      status-type="success"
    />

    <div class="account-grid-2 security-layout">
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

.account-view {
  width: 100%;
  min-width: 0;
  max-width: none;
  color: var(--account-text, #111);
  background: transparent;
}

.security-layout {
  width: 100%;
}

.security-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.security-form__field {
  display: flex;
  flex-direction: column;
  gap: 8px;

  label {
    font-size: 16px;
    font-weight: 800;
    color: #111;
  }

  :deep(.auth-input) {
    height: 50px;
    background: rgba(255, 255, 255, 0.72);
    color: #111;
    border-color: rgba(0, 0, 0, 0.22);
  }
}

.security-form__hint {
  margin: 0;
  font-size: 13px;
  color: #666;
}

.security-form__error {
  margin: 0;
  font-size: 13px;
  color: var(--account-danger);
}

.security-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;

  :deep(.account-btn-primary) {
    min-width: 220px;
    width: min(100%, 280px);
    min-height: 48px;
    height: 48px;
    font-size: 17px;
  }

  :deep(.account-btn-secondary) {
    min-width: 120px;
    min-height: 48px;
    height: 48px;
    font-size: 16px;
    background: rgba(255, 255, 255, 0.7);
  }
}

.security-tip {
  width: 100%;
  margin: 24px 0 0;
  padding: 16px 20px;
  border: 1px solid var(--account-border, #d6d0c6);
  background: rgba(255, 255, 255, 0.4);
  font-size: 15px;
  font-weight: 700;
  color: #555;
  box-sizing: border-box;
}

@media (max-width: 1200px) {
  .security-layout {
    grid-template-columns: 1fr;
  }
}
</style>
`,
)

writeVue(
  'player-identity/index.vue',
  `<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { identityQueryToVerificationStatus } from '@/types/account'
import type { PlayerVerificationStatus } from '@/types/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const isDev = import.meta.env.DEV

const identity = computed(() => accountStore.playerIdentity)

const status = computed(() => identity.value?.status || 'NOT_SUBMITTED')

watch(
  () => route.query.status,
  async (queryStatus) => {
    if (!import.meta.env.DEV || typeof queryStatus !== 'string') return
    const mapped = identityQueryToVerificationStatus(queryStatus)
    if (!mapped) return
    await accountStore.loadAccountMock(mapped, queryStatus === 'limited')
  },
  { immediate: true },
)

watch(
  [status, () => route.name],
  ([nextStatus, routeName]) => {
    if (routeName !== 'AccountPlayerIdentity') return
    if (nextStatus === 'PENDING') router.replace({ name: 'AccountPlayerIdentityPending' })
    else if (nextStatus === 'REJECTED') router.replace({ name: 'AccountPlayerIdentityRejected' })
    else if (nextStatus === 'APPROVED' || nextStatus === 'ELIGIBILITY_LIMITED') {
      if (nextStatus === 'ELIGIBILITY_LIMITED' && route.query.status === 'limited') {
        router.replace({ name: 'AccountPlayerEligibility' })
        return
      }
      router.replace({ name: 'AccountPlayerIdentityApproved' })
    }
  },
  { immediate: true },
)

const banner = computed(() => ({
  title: 'CNPA \\u6c38\\u4e45\\u7403\\u5458\\u8eab\\u4efd',
  description: '\\u5b8c\\u6210\\u4e00\\u6b21\\u8ba4\\u8bc1\\uff0c\\u5efa\\u7acb\\u957f\\u671f\\u6709\\u6548\\u7684\\u7403\\u5458\\u6863\\u6848',
  statusText: '\\u672a\\u8ba4\\u8bc1',
  statusType: 'warning' as const,
}))

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

async function switchDevStatus(next: PlayerVerificationStatus) {
  if (!import.meta.env.DEV) return
  await router.replace({ query: { status: next === 'NOT_APPLIED' ? 'not-submitted' : next.toLowerCase() } })
  await accountStore.loadAccountMock(next)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="\\u7403\\u5458\\u8eab\\u4efd"
      en-title="PLAYER IDENTITY"
      :breadcrumb-items="[
        { label: '\\u9996\\u9875', to: '/home' },
        { label: '\\u4e2a\\u4eba\\u4e2d\\u5fc3', to: '/account/overview' },
        { label: '\\u7403\\u5458\\u8eab\\u4efd' },
      ]"
    />

    <AccountStatusBanner v-bind="banner" />

    <div class="identity-entry-grid">
      <AccountPanel title="\\u5f53\\u524d\\u72b6\\u6001">
        <div class="identity-entry-state">
          <div class="identity-entry-state__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
            </svg>
          </div>
          <AccountStatusBadge type="warning" text="\\u672a\\u8ba4\\u8bc1" />
          <p class="identity-entry-state__title">\\u5c1a\\u672a\\u7533\\u8bf7\\u7403\\u5458\\u8ba4\\u8bc1</p>
        </div>
      </AccountPanel>

      <AccountPanel title="\\u7533\\u8bf7 CNPA \\u7403\\u5458\\u8ba4\\u8bc1">
        <ul class="identity-entry-rules">
          <li>\\u5ba1\\u6838\\u901a\\u8fc7\\u540e\\u5c06\\u83b7\\u5f97\\u4e0d\\u53ef\\u4fee\\u6539\\u7684\\u6c38\\u4e45\\u7403\\u5458 ID</li>
          <li>\\u8ba4\\u8bc1\\u8d44\\u6599\\u7528\\u4e8e\\u786e\\u8ba4\\u6e38\\u620f\\u5e73\\u53f0\\u3001\\u6e38\\u620f ID \\u4e0e\\u7403\\u5458\\u8d5b\\u4e8b\\u8d44\\u683c</li>
          <li>\\u8ba4\\u8bc1\\u957f\\u671f\\u6709\\u6548\\uff0c\\u65e0\\u9700\\u5728\\u6bcf\\u5c4a\\u8d5b\\u4e8b\\u4e2d\\u91cd\\u590d\\u7533\\u8bf7</li>
        </ul>
        <p class="identity-entry-hint">
          \\u672a\\u8ba4\\u8bc1\\u8d26\\u53f7\\u4e0d\\u80fd\\u62a5\\u540d\\u3001\\u63a5\\u53d7\\u7403\\u961f\\u9080\\u8bf7\\u3001\\u8fdb\\u5165\\u9635\\u5bb9\\u6216\\u53c2\\u4e0e\\u8f6c\\u4f1a
        </p>
        <div class="account-panel__action">
          <button type="button" class="account-btn-primary" @click="goApply">\\u7533\\u8bf7\\u7403\\u5458\\u8ba4\\u8bc1</button>
        </div>
      </AccountPanel>
    </div>

    <details v-if="isDev" class="verification-dev-switcher">
      <summary>DEV</summary>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('NOT_APPLIED')">\\u672a\\u63d0\\u4ea4</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('PENDING')">\\u5ba1\\u6838\\u4e2d</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('REJECTED')">\\u672a\\u901a\\u8fc7</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('APPROVED')">\\u5df2\\u901a\\u8fc7</button>
    </details>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
`,
)
