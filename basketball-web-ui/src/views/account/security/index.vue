<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAccountStore } from '@/stores/account'
import { resolvePasswordChangeError, validatePasswordRules } from '@/utils/passwordRules'
import { forceRelogin } from '@/utils/forceRelogin'
import { getLoginLogPage, getOperationLogPage } from '@/api/accountSecurity'
import type { LoginLogItemDto, SecurityLogItemDto } from '@/api/types/accountSecurity'
import { formatDateTime } from '@/utils/formatDate'

const { t } = useI18n()
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
const logsLoading = ref(false)
const loginLogs = ref<LoginLogItemDto[]>([])
const operationLogs = ref<SecurityLogItemDto[]>([])
const loginPage = ref(1)
const operationPage = ref(1)
const loginTotal = ref(0)
const operationTotal = ref(0)

const profile = computed(() => accountStore.profile)
const security = computed(() => accountStore.security)
const account = computed(() => accountStore.currentAccount)

onMounted(async () => {
  await Promise.all([
    accountStore.loadProfile().catch(() => undefined),
    loadLoginLogs(),
    loadOperationLogs(),
  ])
})

async function loadLoginLogs() {
  logsLoading.value = true
  try {
    const res = await getLoginLogPage({ currentPage: loginPage.value, pageSize: 10 })
    loginLogs.value = res.data.records || []
    loginTotal.value = res.data.total || 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.security.loadLoginFailed'))
  } finally {
    logsLoading.value = false
  }
}

async function loadOperationLogs() {
  logsLoading.value = true
  try {
    const res = await getOperationLogPage({ currentPage: operationPage.value, pageSize: 10 })
    operationLogs.value = res.data.records || []
    operationTotal.value = res.data.total || 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.security.loadOperationFailed'))
  } finally {
    logsLoading.value = false
  }
}

function resetErrors() {
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
}

function validateForm(): boolean {
  resetErrors()
  if (!form.currentPassword) errors.currentPassword = t('accountShell.security.currentPasswordRequired')
  const ruleError = validatePasswordRules(form.newPassword)
  if (ruleError) errors.newPassword = ruleError
  if (!form.confirmPassword) errors.confirmPassword = t('accountShell.security.confirmPasswordRequired')
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = t('accountShell.security.passwordMismatch')
  return !errors.currentPassword && !errors.newPassword && !errors.confirmPassword
}

function cancelForm() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  resetErrors()
}

function applyPasswordChangeError(message: string) {
  resetErrors()
  const { field, message: text } = resolvePasswordChangeError(message)
  if (field === 'currentPassword') errors.currentPassword = text
  else if (field === 'confirmPassword') errors.confirmPassword = text
  else if (field === 'newPassword') errors.newPassword = text
  else ElMessage.warning(text)
}

async function savePassword() {
  if (loading.value) return
  if (!validateForm()) {
    ElMessage.warning(t('accountShell.security.checkForm'))
    return
  }
  loading.value = true
  try {
    const result = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (result.error) {
      applyPasswordChangeError(result.error)
      return
    }
    ElMessage.success(t('accountShell.security.passwordChanged'))
    await forceRelogin({
      reason: 'password_changed',
      redirect: '/account/settings',
      skipLogoutApi: true,
    })
  } finally {
    loading.value = false
  }
}

function loginResultText(result?: number) {
  return result === 1 ? t('accountShell.security.success') : t('accountShell.security.failure')
}

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.security.title') },
])

const emailStatusText = computed(() =>
  profile.value?.emailVerified ? t('accountShell.common.verified') : t('accountShell.common.unverified'),
)
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      :zh-title="t('accountShell.security.title')"
      en-title="ACCOUNT SECURITY"
      :breadcrumb-items="breadcrumbs"
    />

    <AccountStatusBanner
      :title="t('accountShell.security.bannerTitle')"
      :description="t('accountShell.security.bannerDesc')"
      :status-text="t('accountShell.security.bannerStatus')"
      status-type="success"
    />

    <div class="account-grid-2 security-layout">
      <AccountPanel :title="t('accountShell.security.changePassword')">
        <form class="security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="current-password">{{ t('accountShell.security.currentPassword') }}</label>
            <PasswordInput id="current-password" v-model="form.currentPassword" autocomplete="current-password" />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-password">{{ t('accountShell.security.newPassword') }}</label>
            <PasswordInput id="new-password" v-model="form.newPassword" autocomplete="new-password" />
            <p class="security-form__hint">{{ t('accountShell.security.passwordHint') }}</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="confirm-password">{{ t('accountShell.security.confirmPassword') }}</label>
            <PasswordInput id="confirm-password" v-model="form.confirmPassword" autocomplete="new-password" />
            <p v-if="errors.confirmPassword" class="security-form__error">{{ errors.confirmPassword }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="loading">{{ t('accountShell.security.savePassword') }}</button>
            <button type="button" class="account-btn-secondary" @click="cancelForm">{{ t('accountShell.common.cancel') }}</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel :title="t('accountShell.security.accountInfo')">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>{{ t('accountShell.common.registeredEmail') }}</dt><dd>{{ profile?.maskedEmail }}</dd></div>
          <div class="account-field-row">
            <dt>{{ t('accountShell.common.emailStatus') }}</dt>
            <dd :class="profile?.emailVerified ? 'account-status-text--success' : ''">
              {{ emailStatusText }}
            </dd>
          </div>
          <div class="account-field-row"><dt>{{ t('accountShell.common.accountStatus') }}</dt><dd class="account-status-text--success">{{ t('accountShell.common.normal') }}</dd></div>
          <div class="account-field-row">
            <dt>{{ t('accountShell.common.lastLogin') }}</dt>
            <dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd>
          </div>
          <div class="account-field-row">
            <dt>{{ t('accountShell.common.passwordUpdatedAt') }}</dt>
            <dd>{{ formatDateTime(security?.passwordUpdatedAt || '') }}</dd>
          </div>
        </dl>
        <p class="security-form__hint">{{ t('accountShell.common.notYou') }}</p>
      </AccountPanel>
    </div>

    <div class="account-grid-2 security-layout security-logs">
      <AccountPanel :title="t('accountShell.security.loginLogs')">
        <div v-if="logsLoading && !loginLogs.length" class="security-form__hint">{{ t('accountShell.common.loading') }}</div>
        <div v-else-if="!loginLogs.length" class="security-form__hint">{{ t('accountShell.security.noLoginLogs') }}</div>
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>{{ t('accountShell.security.time') }}</th>
                <th>{{ t('accountShell.security.method') }}</th>
                <th>{{ t('accountShell.security.result') }}</th>
                <th>{{ t('accountShell.security.region') }}</th>
                <th>{{ t('accountShell.security.terminal') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in loginLogs" :key="item.logId">
                <td>{{ formatDateTime(item.loginTime || '') }}</td>
                <td>{{ item.loginType || '-' }}</td>
                <td>{{ loginResultText(item.loginResult) }}</td>
                <td>{{ item.clientRegion || item.clientIpMasked || '-' }}</td>
                <td>{{ item.terminalType || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </AccountPanel>

      <AccountPanel :title="t('accountShell.security.operationLogs')">
        <div v-if="logsLoading && !operationLogs.length" class="security-form__hint">{{ t('accountShell.common.loading') }}</div>
        <div v-else-if="!operationLogs.length" class="security-form__hint">{{ t('accountShell.security.noOperationLogs') }}</div>
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>{{ t('accountShell.security.time') }}</th>
                <th>{{ t('accountShell.security.event') }}</th>
                <th>{{ t('accountShell.security.change') }}</th>
                <th>{{ t('accountShell.security.terminal') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in operationLogs" :key="item.logId">
                <td>{{ formatDateTime(item.eventTime || '') }}</td>
                <td>{{ item.eventTitle || item.eventType || '-' }}</td>
                <td>{{ item.newValueMasked || item.oldValueMasked || '-' }}</td>
                <td>{{ item.terminalType || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </AccountPanel>
    </div>

    <p class="security-tip">{{ t('accountShell.common.securityTip') }}</p>
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
  gap: var(--account-card-gap);
  width: 100%;
}

.security-logs {
  margin-top: var(--account-card-gap);
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
