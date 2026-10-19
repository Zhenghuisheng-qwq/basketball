<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import ProfileAvatarUploader from '@/views/account/components/ProfileAvatarUploader.vue'
import { sendChangeEmailCode, changeMemberEmail } from '@/api/memberAuth'
import { useAccountStore } from '@/stores/account'
import { resolveMemberAvatarUrl } from '@/utils/memberAvatar'
import { validatePasswordRules, type PasswordChangeErrorField } from '@/utils/passwordRules'
import { forceRelogin } from '@/utils/forceRelogin'
import { formatDateTime } from '@/utils/formatDate'
import type { NotificationSettingDto, PrivacySettingDto } from '@/api/types/accountSetting'

type SettingsTab = 'profile' | 'privacy' | 'notification' | 'security'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const accountStore = useAccountStore()

const activeTab = ref<SettingsTab>('profile')
const settingsLoading = ref(false)
const settingsSaving = ref(false)

function isSettingsTab(tab: unknown): tab is SettingsTab {
  return tab === 'profile' || tab === 'privacy' || tab === 'notification' || tab === 'security'
}

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = isSettingsTab(tab) ? tab : 'profile'
  },
  { immediate: true },
)

function setTab(tab: SettingsTab) {
  activeTab.value = tab
  router.replace({ query: tab === 'profile' ? {} : { tab } })
}

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
const emailLoading = ref(false)
const emailSending = ref(false)
const emailCountdown = ref(0)
const emailVerifyBizNo = ref('')
let emailTimer: ReturnType<typeof setInterval> | null = null

const emailForm = reactive({
  newEmail: '',
  verifyCode: '',
})

const emailErrors = reactive({
  newEmail: '',
  verifyCode: '',
})

const profile = computed(() => accountStore.profile)
const security = computed(() => accountStore.security)
const account = computed(() => accountStore.currentAccount)

const profileForm = reactive({
  nickname: accountStore.profile?.nickname || accountStore.currentAccount?.nickname || '',
  avatarUrl: accountStore.profile?.avatarUrl || accountStore.currentAccount?.avatarUrl || '',
})
const avatarFile = ref<File | null>(null)
const avatarError = ref('')
const profileSaving = ref(false)

const privacyForm = reactive<PrivacySettingDto>({
  profileVisible: 1,
  allowTeamInvite: 1,
  showTeamHistory: 1,
  showMatchData: 1,
})

const notificationForm = reactive<NotificationSettingDto>({
  systemNotice: 1,
  certificationNotice: 1,
  tournamentNotice: 1,
  teamNotice: 1,
  announcementNotice: 1,
})

const hasPendingChange = computed(() => Boolean(profile.value?.hasPendingChange && profile.value.pendingChange))
const pendingChange = computed(() => profile.value?.pendingChange || null)
const latestRejectedChange = computed(() => {
  if (hasPendingChange.value) return null
  return profile.value?.latestRejectedChange || null
})
const formalAvatarUrl = computed(() => resolveMemberAvatarUrl(profile.value?.avatarUrl || profileForm.avatarUrl))
const canEditPublicProfile = computed(() => !hasPendingChange.value)

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.settings.title') },
])

const settingsTabs = computed(() => [
  { key: 'profile' as const, label: t('accountShell.settings.tabs.profile') },
  { key: 'privacy' as const, label: t('accountShell.settings.tabs.privacy') },
  { key: 'notification' as const, label: t('accountShell.settings.tabs.notification') },
  { key: 'security' as const, label: t('accountShell.settings.tabs.security') },
])

const privacyItems = computed(() => [
  { key: 'profileVisible' as const, label: t('accountShell.settings.privacy.profileVisible') },
  { key: 'allowTeamInvite' as const, label: t('accountShell.settings.privacy.allowTeamInvite') },
  { key: 'showTeamHistory' as const, label: t('accountShell.settings.privacy.showTeamHistory') },
  { key: 'showMatchData' as const, label: t('accountShell.settings.privacy.showMatchData') },
])

const notificationItems = computed(() => [
  { key: 'systemNotice' as const, label: t('accountShell.settings.notification.systemNotice') },
  { key: 'certificationNotice' as const, label: t('accountShell.settings.notification.certificationNotice') },
  { key: 'tournamentNotice' as const, label: t('accountShell.settings.notification.tournamentNotice') },
  { key: 'teamNotice' as const, label: t('accountShell.settings.notification.teamNotice') },
  { key: 'announcementNotice' as const, label: t('accountShell.settings.notification.announcementNotice') },
])

const emailStatusText = computed(() =>
  profile.value?.emailVerified ? t('accountShell.common.verified') : t('accountShell.common.unverified'),
)

watch(
  () => [accountStore.profile?.nickname, accountStore.profile?.avatarUrl] as const,
  ([nickname, avatarUrl]) => {
    if (nickname) profileForm.nickname = nickname
    profileForm.avatarUrl = avatarUrl || ''
    avatarFile.value = null
    avatarError.value = ''
  },
)

onMounted(async () => {
  settingsLoading.value = true
  try {
    await Promise.all([
      accountStore.loadProfile().catch(() => undefined),
      accountStore.loadPrivacy().then((data) => Object.assign(privacyForm, data)).catch(() => undefined),
      accountStore.loadNotificationSetting().then((data) => Object.assign(notificationForm, data)).catch(() => undefined),
    ])
  } finally {
    settingsLoading.value = false
  }
})

onUnmounted(() => {
  if (emailTimer) clearInterval(emailTimer)
})

function validateNewEmail(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return t('accountShell.settings.newEmailRequired')
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return t('accountShell.settings.emailInvalid')
  return ''
}

function startEmailCountdown(seconds = 60) {
  emailCountdown.value = seconds
  if (emailTimer) clearInterval(emailTimer)
  emailTimer = setInterval(() => {
    if (emailCountdown.value <= 1) {
      emailCountdown.value = 0
      if (emailTimer) clearInterval(emailTimer)
      emailTimer = null
      return
    }
    emailCountdown.value -= 1
  }, 1000)
}

async function handleSendChangeEmailCode() {
  emailErrors.newEmail = validateNewEmail(emailForm.newEmail)
  if (emailErrors.newEmail || emailSending.value || emailCountdown.value > 0) return
  emailSending.value = true
  try {
    const res = await sendChangeEmailCode(emailForm.newEmail.trim().toLowerCase())
    emailVerifyBizNo.value = res.data.bizNo
    ElMessage.success(t('accountShell.settings.codeSent'))
    startEmailCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.settings.codeSendFailed'))
  } finally {
    emailSending.value = false
  }
}

async function saveEmail() {
  emailErrors.newEmail = validateNewEmail(emailForm.newEmail)
  emailErrors.verifyCode = emailForm.verifyCode.trim() ? '' : t('accountShell.settings.codeRequired')
  if (emailErrors.newEmail || emailErrors.verifyCode || emailLoading.value) return
  if (!emailVerifyBizNo.value) {
    ElMessage.warning(t('accountShell.settings.sendCodeFirst'))
    return
  }
  emailLoading.value = true
  try {
    await changeMemberEmail({
      email: emailForm.newEmail.trim().toLowerCase(),
      verifyBizNo: emailVerifyBizNo.value,
      verifyCode: emailForm.verifyCode.trim(),
    })
    ElMessage.success(t('accountShell.settings.emailChanged'))
    await forceRelogin({
      reason: 'email_changed',
      redirect: '/account/settings',
      skipLogoutApi: true,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.settings.emailChangeFailed'))
  } finally {
    emailLoading.value = false
  }
}

function resetErrors() {
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
}

function validateForm(): boolean {
  resetErrors()
  if (!form.currentPassword) errors.currentPassword = t('accountShell.settings.currentPasswordRequired')
  const ruleError = validatePasswordRules(form.newPassword)
  if (ruleError) errors.newPassword = ruleError
  if (!form.confirmPassword) errors.confirmPassword = t('accountShell.settings.confirmPasswordRequired')
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = t('accountShell.settings.passwordMismatch')
  return !errors.currentPassword && !errors.newPassword && !errors.confirmPassword
}

function cancelForm() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  resetErrors()
}

function applyPasswordChangeError(result: { error?: string; field?: PasswordChangeErrorField }) {
  resetErrors()
  const text = result.error || t('accountShell.profile.passwordChangeFailed')
  if (result.field === 'currentPassword' || result.field === 'newPassword' || result.field === 'confirmPassword') {
    errors[result.field] = text
    return
  }
  ElMessage.warning(text)
}

async function savePassword() {
  if (loading.value) return
  if (!validateForm()) {
    ElMessage.warning(t('accountShell.settings.checkForm'))
    return
  }
  loading.value = true
  try {
    const result = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (result.error) {
      applyPasswordChangeError(result)
      return
    }
    ElMessage.success(t('accountShell.settings.passwordChanged'))
    await forceRelogin({
      reason: 'password_changed',
      redirect: '/account/settings',
      skipLogoutApi: true,
    })
  } finally {
    loading.value = false
  }
}

function validateNickname(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return t('accountShell.settings.nicknameRequired')
  if (trimmed.length < 2 || trimmed.length > 20) return t('accountShell.settings.nicknameLength')
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9_]+$/.test(trimmed)) {
    return t('accountShell.settings.nicknameCharset')
  }
  return ''
}

function handleAvatarSelected(file: File) {
  avatarFile.value = file
  avatarError.value = ''
}

async function saveProfile() {
  if (profileSaving.value) return
  if (hasPendingChange.value) {
    ElMessage.warning(t('accountShell.settings.pendingExists'))
    return
  }

  const nickname = profileForm.nickname.trim()
  const nicknameError = validateNickname(nickname)
  if (nicknameError) {
    ElMessage.warning(nicknameError)
    return
  }

  const formalNickname = (profile.value?.nickname || '').trim()
  const nicknameChanged = nickname !== formalNickname
  const avatarChanged = Boolean(avatarFile.value)
  if (!nicknameChanged && !avatarChanged) {
    ElMessage.warning(t('accountShell.settings.noChange'))
    return
  }

  profileSaving.value = true
  try {
    await accountStore.updateProfile({
      nickname,
      avatarFile: avatarFile.value || undefined,
    })
    avatarFile.value = null
    ElMessage.success(t('accountShell.settings.profileSubmitted'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.settings.submitFailed'))
  } finally {
    profileSaving.value = false
  }
}

async function savePrivacy() {
  settingsSaving.value = true
  try {
    await accountStore.updatePrivacy({ ...privacyForm })
    ElMessage.success(t('accountShell.settings.privacySaved'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.common.saveFailed'))
  } finally {
    settingsSaving.value = false
  }
}

async function saveNotificationSettings() {
  settingsSaving.value = true
  try {
    await accountStore.updateNotificationSetting({ ...notificationForm })
    ElMessage.success(t('accountShell.settings.notificationSaved'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.common.saveFailed'))
  } finally {
    settingsSaving.value = false
  }
}

function toggleSetting(target: Record<string, number | undefined>, key: string) {
  target[key] = target[key] === 1 ? 0 : 1
}
</script>

<template>
  <div class="account-view account-view--settings">
    <AccountPageHeader
      :zh-title="t('accountShell.settings.title')"
      en-title="ACCOUNT SETTINGS"
      :breadcrumb-items="breadcrumbs"
    />

    <AccountStatusBanner
      :title="t('accountShell.settings.bannerTitle')"
      :description="t('accountShell.settings.bannerDesc')"
      :status-text="t('accountShell.settings.bannerStatus')"
      status-type="success"
    />

    <nav class="account-settings-tabs" :aria-label="t('accountShell.settings.tabsLabel')">
      <button
        v-for="tab in settingsTabs"
        :key="tab.key"
        type="button"
        class="account-settings-tabs__item"
        :class="{ 'account-settings-tabs__item--active': activeTab === tab.key }"
        @click="setTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <div v-if="settingsLoading" class="settings-loading">{{ t('accountShell.settings.loading') }}</div>

    <div v-else-if="activeTab === 'profile'" class="account-grid-2">
      <AccountPanel :title="t('accountShell.settings.profileTitle')">
        <form class="security-form" @submit.prevent="saveProfile">
          <div class="security-form__field">
            <label>{{ t('accountShell.settings.avatar') }}</label>
            <ProfileAvatarUploader
              v-model="profileForm.avatarUrl"
              :name="profile?.nickname || profileForm.nickname || 'CN'"
              :action-label="t('accountShell.settings.changeAvatar')"
              :disabled="!canEditPublicProfile"
              :hint="t('accountShell.settings.avatarHint')"
              @error="avatarError = $event"
              @file-selected="handleAvatarSelected"
            />
            <p v-if="avatarError" class="security-form__error">{{ avatarError }}</p>
          </div>

          <div class="security-form__field">
            <label for="settings-nickname">{{ t('accountShell.settings.nickname') }}</label>
            <input
              id="settings-nickname"
              v-model="profileForm.nickname"
              class="auth-input"
              type="text"
              maxlength="20"
              autocomplete="nickname"
              :disabled="!canEditPublicProfile"
            />
            <p class="security-form__hint">{{ t('accountShell.settings.nicknameHint') }}</p>
          </div>

          <div class="security-form__field">
            <label>{{ t('accountShell.common.registeredEmail') }}</label>
            <p class="security-form__hint">{{ t('accountShell.settings.emailReadonly', { email: profile?.maskedEmail || account?.maskedEmail || '' }) }}</p>
          </div>

          <div class="security-form__actions">
            <button
              type="submit"
              class="account-btn-primary"
              :disabled="profileSaving || !canEditPublicProfile"
            >
              {{ profileSaving ? t('accountShell.settings.submitting') : t('accountShell.settings.saveProfile') }}
            </button>
          </div>
        </form>
      </AccountPanel>

      <div class="settings-profile-side">
        <AccountPanel v-if="hasPendingChange && pendingChange" :title="t('accountShell.settings.pendingTitle')">
          <dl class="account-info-list settings-audit-list">
            <div class="account-field-row">
              <dt>{{ t('accountShell.settings.status') }}</dt>
              <dd class="account-status-text--warning">{{ pendingChange.auditStatusName || t('accountShell.common.pending') }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountShell.settings.submitTime') }}</dt>
              <dd>{{ formatDateTime(pendingChange.submitTime || '') }}</dd>
            </div>
            <div class="account-field-row settings-audit-list__compare">
              <dt>{{ t('accountShell.settings.nicknameLabel') }}</dt>
              <dd>
                <span>{{ t('accountShell.settings.currentValue', { value: pendingChange.oldNickname || profile?.nickname || '-' }) }}</span>
                <span>{{ t('accountShell.settings.requestedValue', { value: pendingChange.newNickname || '-' }) }}</span>
              </dd>
            </div>
            <div class="account-field-row settings-audit-list__avatars">
              <dt>{{ t('accountShell.settings.avatarLabel') }}</dt>
              <dd>
                <div class="settings-avatar-compare">
                  <figure>
                    <img :src="resolveMemberAvatarUrl(pendingChange.oldAvatarUrl || formalAvatarUrl)" :alt="t('accountShell.settings.currentAvatarAlt')" />
                    <figcaption>{{ t('accountShell.settings.current') }}</figcaption>
                  </figure>
                  <figure>
                    <img :src="resolveMemberAvatarUrl(pendingChange.newAvatarUrl || formalAvatarUrl)" :alt="t('accountShell.settings.requestedAvatarAlt')" />
                    <figcaption>{{ t('accountShell.settings.requested') }}</figcaption>
                  </figure>
                </div>
              </dd>
            </div>
          </dl>
          <p class="security-form__hint">{{ t('accountShell.settings.pendingHint') }}</p>
        </AccountPanel>

        <AccountPanel v-else-if="latestRejectedChange" :title="t('accountShell.settings.rejectedTitle')">
          <dl class="account-info-list">
            <div class="account-field-row">
              <dt>{{ t('accountShell.settings.status') }}</dt>
              <dd class="account-status-text--danger">{{ latestRejectedChange.auditStatusName || t('accountShell.settings.rejectedFallback') }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountShell.settings.auditReason') }}</dt>
              <dd>{{ latestRejectedChange.auditRemark || t('accountShell.settings.notFilled') }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountShell.settings.auditTime') }}</dt>
              <dd>{{ formatDateTime(latestRejectedChange.auditTime || '') }}</dd>
            </div>
          </dl>
          <p class="security-form__hint">{{ t('accountShell.settings.rejectedHint') }}</p>
        </AccountPanel>

        <AccountPanel :title="t('accountShell.settings.overviewTitle')">
          <dl class="account-info-list">
            <div class="account-field-row"><dt>{{ t('accountShell.common.accountStatus') }}</dt><dd class="account-status-text--success">{{ t('accountShell.common.normal') }}</dd></div>
            <div class="account-field-row">
              <dt>{{ t('accountShell.common.emailStatus') }}</dt>
              <dd :class="profile?.emailVerified ? 'account-status-text--success' : ''">
                {{ emailStatusText }}
              </dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountShell.common.lastLogin') }}</dt>
              <dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd>
            </div>
          </dl>
        </AccountPanel>
      </div>
    </div>

    <div v-else-if="activeTab === 'privacy'" class="account-grid-2">
      <AccountPanel :title="t('accountShell.settings.privacyTitle')">
        <form class="security-form" @submit.prevent="savePrivacy">
          <label v-for="item in privacyItems" :key="item.key" class="settings-toggle">
            <span>{{ item.label }}</span>
            <input
              type="checkbox"
              :checked="privacyForm[item.key] === 1"
              @change="toggleSetting(privacyForm, item.key)"
            />
          </label>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="settingsSaving">{{ t('accountShell.settings.savePrivacy') }}</button>
          </div>
        </form>
      </AccountPanel>
    </div>

    <div v-else-if="activeTab === 'notification'" class="account-grid-2">
      <AccountPanel :title="t('accountShell.settings.notificationTitle')">
        <form class="security-form" @submit.prevent="saveNotificationSettings">
          <label v-for="item in notificationItems" :key="item.key" class="settings-toggle">
            <span>{{ item.label }}</span>
            <input
              type="checkbox"
              :checked="notificationForm[item.key] === 1"
              @change="toggleSetting(notificationForm, item.key)"
            />
          </label>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="settingsSaving">{{ t('accountShell.settings.saveNotification') }}</button>
          </div>
        </form>
      </AccountPanel>
    </div>

    <div v-else class="account-grid-2 security-layout">
      <AccountPanel :title="t('accountShell.settings.changePassword')">
        <form class="security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="current-password">{{ t('accountShell.settings.currentPassword') }}</label>
            <PasswordInput id="current-password" v-model="form.currentPassword" autocomplete="current-password" />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-password">{{ t('accountShell.settings.newPassword') }}</label>
            <PasswordInput id="new-password" v-model="form.newPassword" autocomplete="new-password" />
            <p class="security-form__hint">{{ t('accountShell.settings.passwordHint') }}</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="confirm-password">{{ t('accountShell.settings.confirmPassword') }}</label>
            <PasswordInput id="confirm-password" v-model="form.confirmPassword" autocomplete="new-password" />
            <p v-if="errors.confirmPassword" class="security-form__error">{{ errors.confirmPassword }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="loading">{{ t('accountShell.settings.savePassword') }}</button>
            <button type="button" class="account-btn-secondary" @click="cancelForm">{{ t('accountShell.common.cancel') }}</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel :title="t('accountShell.settings.changeEmail')">
        <form class="security-form" @submit.prevent="saveEmail">
          <div class="security-form__field">
            <label>{{ t('accountShell.settings.currentEmail') }}</label>
            <p class="security-form__hint">{{ profile?.maskedEmail || account?.maskedEmail }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-email">{{ t('accountShell.settings.newEmail') }}</label>
            <input
              id="new-email"
              v-model="emailForm.newEmail"
              class="auth-input"
              type="email"
              autocomplete="email"
              @blur="emailErrors.newEmail = validateNewEmail(emailForm.newEmail)"
            />
            <p v-if="emailErrors.newEmail" class="security-form__error">{{ emailErrors.newEmail }}</p>
          </div>
          <div class="security-form__field">
            <label for="change-email-code">{{ t('accountShell.settings.emailCode') }}</label>
            <div class="settings-code-row">
              <input
                id="change-email-code"
                v-model="emailForm.verifyCode"
                class="auth-input"
                type="text"
                inputmode="numeric"
                maxlength="6"
                autocomplete="one-time-code"
              />
              <button
                type="button"
                class="account-btn-secondary settings-code-btn"
                :disabled="emailSending || emailCountdown > 0"
                @click="handleSendChangeEmailCode"
              >
                {{ emailCountdown > 0 ? `${emailCountdown}s` : emailSending ? t('accountShell.settings.sending') : t('accountShell.settings.sendCode') }}
              </button>
            </div>
            <p v-if="emailErrors.verifyCode" class="security-form__error">{{ emailErrors.verifyCode }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="emailLoading">{{ t('accountShell.settings.saveEmail') }}</button>
          </div>
          <p class="security-form__hint">{{ t('accountShell.settings.emailReloginHint') }}</p>
        </form>
      </AccountPanel>

      <AccountPanel :title="t('accountShell.settings.accountInfo')">
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

    <p class="security-tip">{{ t('accountShell.common.securityTip') }}</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view--settings {
  width: 100%;
  max-width: none;
  background: transparent;
  color: var(--account-text, #111);
}

.settings-loading {
  padding: 32px 0;
  color: #666;
}

.account-settings-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 22px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.14);
}

.account-settings-tabs__item {
  position: relative;
  min-height: 56px;
  padding: 0 4px 12px;
  border: 0;
  background: transparent;
  color: #222;
  font-size: 18px;
  font-weight: 800;
  cursor: pointer;

  &--active {
    color: var(--account-red, #d90000);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: -1px;
      height: 3px;
      background: var(--account-red, #d90000);
    }
  }
}

.settings-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 15px;
  font-weight: 700;
}

.settings-code-row {
  display: flex;
  gap: 10px;
}

.settings-code-row .auth-input {
  flex: 1;
}

.settings-code-btn {
  flex: 0 0 112px;
  white-space: nowrap;
}

.settings-profile-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.settings-audit-list__compare dd,
.settings-audit-list__avatars dd {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings-avatar-compare {
  display: flex;
  gap: 16px;

  figure {
    margin: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
  }

  img {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    object-fit: cover;
    border: 1px solid rgba(0, 0, 0, 0.12);
    background: #e8e8e8;
  }

  figcaption {
    font-size: 12px;
    color: #666;
  }
}

.account-status-text--warning {
  color: #b45309;
  font-weight: 700;
}

.account-status-text--danger {
  color: var(--account-danger, #c62828);
  font-weight: 700;
}

.security-form__field input:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  background: rgba(245, 245, 245, 0.9);
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
  }

  .auth-input,
  :deep(.auth-input) {
    width: 100%;
    height: 48px;
    padding: 0 14px;
    border: 1px solid rgba(0, 0, 0, 0.22);
    background: rgba(255, 255, 255, 0.72);
    color: #111;
    font-size: 16px;
    box-sizing: border-box;
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
  margin-top: 8px;
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
</style>
