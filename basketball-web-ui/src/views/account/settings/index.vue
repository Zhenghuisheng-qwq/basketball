<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
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
import { resolvePasswordChangeError, validatePasswordRules } from '@/utils/passwordRules'
import { forceRelogin } from '@/utils/forceRelogin'
import { formatDateTime } from '@/utils/formatDate'
import type { NotificationSettingDto, PrivacySettingDto } from '@/api/types/accountSetting'

type SettingsTab = 'profile' | 'privacy' | 'notification' | 'security'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const activeTab = ref<SettingsTab>('security')
const settingsLoading = ref(false)
const settingsSaving = ref(false)

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'profile' || tab === 'privacy' || tab === 'notification') {
      activeTab.value = tab
    } else {
      activeTab.value = 'security'
    }
  },
  { immediate: true },
)

function setTab(tab: SettingsTab) {
  activeTab.value = tab
  router.replace({ query: tab === 'security' ? {} : { tab } })
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
  if (!trimmed) return '请输入新邮箱'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed)) return '邮箱格式不正确'
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
    ElMessage.success('验证码已发送，请查收新邮箱')
    startEmailCountdown(res.data.resendSeconds || 60)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '验证码发送失败，请稍后重试')
  } finally {
    emailSending.value = false
  }
}

async function saveEmail() {
  emailErrors.newEmail = validateNewEmail(emailForm.newEmail)
  emailErrors.verifyCode = emailForm.verifyCode.trim() ? '' : '请输入验证码'
  if (emailErrors.newEmail || emailErrors.verifyCode || emailLoading.value) return
  if (!emailVerifyBizNo.value) {
    ElMessage.warning('请先发送验证码')
    return
  }
  emailLoading.value = true
  try {
    await changeMemberEmail({
      email: emailForm.newEmail.trim().toLowerCase(),
      verifyBizNo: emailVerifyBizNo.value,
      verifyCode: emailForm.verifyCode.trim(),
    })
    ElMessage.success('邮箱修改成功，请重新登录')
    await forceRelogin({
      reason: 'email_changed',
      redirect: '/account/settings',
      skipLogoutApi: true,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '邮箱修改失败')
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
  if (!form.currentPassword) errors.currentPassword = '请输入当前密码'
  const ruleError = validatePasswordRules(form.newPassword)
  if (ruleError) errors.newPassword = ruleError
  if (!form.confirmPassword) errors.confirmPassword = '请确认新密码'
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = '两次密码输入不一致'
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
    ElMessage.warning('请检查表单填写是否正确')
    return
  }
  loading.value = true
  try {
    const result = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (result.error) {
      applyPasswordChangeError(result.error)
      return
    }
    ElMessage.success('密码修改成功，请重新登录')
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
  if (!trimmed) return '请输入昵称'
  if (trimmed.length < 2 || trimmed.length > 20) return '昵称长度应为 2 至 20 个字符'
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9_]+$/.test(trimmed)) {
    return '昵称仅支持中文、英文、数字和下划线'
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
    ElMessage.warning('你已有资料修改申请正在审核，请等待审核完成。')
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
    ElMessage.warning('资料没有发生变化')
    return
  }

  profileSaving.value = true
  try {
    await accountStore.updateProfile({
      nickname,
      avatarFile: avatarFile.value || undefined,
    })
    avatarFile.value = null
    ElMessage.success('资料修改申请已提交，请等待管理员审核')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    profileSaving.value = false
  }
}

async function savePrivacy() {
  settingsSaving.value = true
  try {
    await accountStore.updatePrivacy({ ...privacyForm })
    ElMessage.success('隐私设置已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    settingsSaving.value = false
  }
}

async function saveNotificationSettings() {
  settingsSaving.value = true
  try {
    await accountStore.updateNotificationSetting({ ...notificationForm })
    ElMessage.success('通知设置已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
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
      zh-title="账号设置"
      en-title="ACCOUNT SETTINGS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '账号设置' },
      ]"
    />

    <AccountStatusBanner
      title="管理你的 CNPA 账号"
      description="维护账号资料、隐私与通知偏好"
      status-text="账号状态正常"
      status-type="success"
    />

    <nav class="account-settings-tabs" aria-label="账号设置分类">
      <button
        v-for="tab in ([
          ['profile', '账号资料'],
          ['privacy', '隐私设置'],
          ['notification', '通知设置'],
          ['security', '账号安全'],
        ] as const)"
        :key="tab[0]"
        type="button"
        class="account-settings-tabs__item"
        :class="{ 'account-settings-tabs__item--active': activeTab === tab[0] }"
        @click="setTab(tab[0])"
      >
        {{ tab[1] }}
      </button>
    </nav>

    <div v-if="settingsLoading" class="settings-loading">加载设置中...</div>

    <div v-else-if="activeTab === 'profile'" class="account-grid-2">
      <AccountPanel title="账号资料">
        <form class="security-form" @submit.prevent="saveProfile">
          <div class="security-form__field">
            <label>头像</label>
            <ProfileAvatarUploader
              v-model="profileForm.avatarUrl"
              :name="profile?.nickname || profileForm.nickname || 'CN'"
              action-label="更换头像"
              :disabled="!canEditPublicProfile"
              hint="新头像审核通过后才会公开展示"
              @error="avatarError = $event"
              @file-selected="handleAvatarSelected"
            />
            <p v-if="avatarError" class="security-form__error">{{ avatarError }}</p>
          </div>

          <div class="security-form__field">
            <label for="settings-nickname">昵称</label>
            <input
              id="settings-nickname"
              v-model="profileForm.nickname"
              class="auth-input"
              type="text"
              maxlength="20"
              autocomplete="nickname"
              :disabled="!canEditPublicProfile"
            />
            <p class="security-form__hint">昵称修改后需审核通过才会公开展示</p>
          </div>

          <div class="security-form__field">
            <label>注册邮箱</label>
            <p class="security-form__hint">{{ profile?.maskedEmail || account?.maskedEmail }}（登录凭据，只读）</p>
          </div>

          <div class="security-form__actions">
            <button
              type="submit"
              class="account-btn-primary"
              :disabled="profileSaving || !canEditPublicProfile"
            >
              {{ profileSaving ? '提交中...' : '保存资料' }}
            </button>
          </div>
        </form>
      </AccountPanel>

      <div class="settings-profile-side">
        <AccountPanel v-if="hasPendingChange && pendingChange" title="资料修改审核中">
          <dl class="account-info-list settings-audit-list">
            <div class="account-field-row">
              <dt>状态</dt>
              <dd class="account-status-text--warning">{{ pendingChange.auditStatusName || '审核中' }}</dd>
            </div>
            <div class="account-field-row">
              <dt>提交时间</dt>
              <dd>{{ formatDateTime(pendingChange.submitTime || '') }}</dd>
            </div>
            <div class="account-field-row settings-audit-list__compare">
              <dt>昵称</dt>
              <dd>
                <span>当前：{{ pendingChange.oldNickname || profile?.nickname || '-' }}</span>
                <span>申请：{{ pendingChange.newNickname || '-' }}</span>
              </dd>
            </div>
            <div class="account-field-row settings-audit-list__avatars">
              <dt>头像</dt>
              <dd>
                <div class="settings-avatar-compare">
                  <figure>
                    <img :src="resolveMemberAvatarUrl(pendingChange.oldAvatarUrl || formalAvatarUrl)" alt="当前头像" />
                    <figcaption>当前</figcaption>
                  </figure>
                  <figure>
                    <img :src="resolveMemberAvatarUrl(pendingChange.newAvatarUrl || formalAvatarUrl)" alt="申请头像" />
                    <figcaption>申请</figcaption>
                  </figure>
                </div>
              </dd>
            </div>
          </dl>
          <p class="security-form__hint">审核完成前不可再次提交修改。</p>
        </AccountPanel>

        <AccountPanel v-else-if="latestRejectedChange" title="资料修改未通过审核">
          <dl class="account-info-list">
            <div class="account-field-row">
              <dt>状态</dt>
              <dd class="account-status-text--danger">{{ latestRejectedChange.auditStatusName || '已驳回' }}</dd>
            </div>
            <div class="account-field-row">
              <dt>审核原因</dt>
              <dd>{{ latestRejectedChange.auditRemark || '未填写' }}</dd>
            </div>
            <div class="account-field-row">
              <dt>审核时间</dt>
              <dd>{{ formatDateTime(latestRejectedChange.auditTime || '') }}</dd>
            </div>
          </dl>
          <p class="security-form__hint">正式昵称 / 头像未变更，可修改后重新提交。</p>
        </AccountPanel>

        <AccountPanel title="账号概览">
          <dl class="account-info-list">
            <div class="account-field-row"><dt>账号状态</dt><dd class="account-status-text--success">正常</dd></div>
            <div class="account-field-row">
              <dt>邮箱状态</dt>
              <dd :class="profile?.emailVerified ? 'account-status-text--success' : ''">
                {{ profile?.emailVerified ? '已验证' : '未验证' }}
              </dd>
            </div>
            <div class="account-field-row">
              <dt>最近登录</dt>
              <dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd>
            </div>
          </dl>
        </AccountPanel>
      </div>
    </div>

    <div v-else-if="activeTab === 'privacy'" class="account-grid-2">
      <AccountPanel title="隐私设置">
        <form class="security-form" @submit.prevent="savePrivacy">
          <label v-for="item in [
            ['profileVisible', '公开个人资料'],
            ['allowTeamInvite', '允许球队邀请'],
            ['showTeamHistory', '展示球队历史'],
            ['showMatchData', '展示比赛数据'],
          ]" :key="item[0]" class="settings-toggle">
            <span>{{ item[1] }}</span>
            <input
              type="checkbox"
              :checked="privacyForm[item[0] as keyof PrivacySettingDto] === 1"
              @change="toggleSetting(privacyForm, item[0])"
            />
          </label>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="settingsSaving">保存隐私设置</button>
          </div>
        </form>
      </AccountPanel>
    </div>

    <div v-else-if="activeTab === 'notification'" class="account-grid-2">
      <AccountPanel title="通知设置">
        <form class="security-form" @submit.prevent="saveNotificationSettings">
          <label v-for="item in [
            ['systemNotice', '系统通知'],
            ['certificationNotice', '认证通知'],
            ['tournamentNotice', '赛事通知'],
            ['teamNotice', '球队通知'],
            ['announcementNotice', '公告通知'],
          ]" :key="item[0]" class="settings-toggle">
            <span>{{ item[1] }}</span>
            <input
              type="checkbox"
              :checked="notificationForm[item[0] as keyof NotificationSettingDto] === 1"
              @change="toggleSetting(notificationForm, item[0])"
            />
          </label>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="settingsSaving">保存通知设置</button>
          </div>
        </form>
      </AccountPanel>
    </div>

    <div v-else class="account-grid-2 security-layout">
      <AccountPanel title="修改密码">
        <form class="security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="current-password">当前密码</label>
            <PasswordInput id="current-password" v-model="form.currentPassword" autocomplete="current-password" />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-password">新密码</label>
            <PasswordInput id="new-password" v-model="form.newPassword" autocomplete="new-password" />
            <p class="security-form__hint">8 至 20 位，需包含字母与数字（登录凭据）</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="confirm-password">确认新密码</label>
            <PasswordInput id="confirm-password" v-model="form.confirmPassword" autocomplete="new-password" />
            <p v-if="errors.confirmPassword" class="security-form__error">{{ errors.confirmPassword }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="loading">保存新密码</button>
            <button type="button" class="account-btn-secondary" @click="cancelForm">取消</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel title="修改登录邮箱">
        <form class="security-form" @submit.prevent="saveEmail">
          <div class="security-form__field">
            <label>当前邮箱</label>
            <p class="security-form__hint">{{ profile?.maskedEmail || account?.maskedEmail }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-email">新邮箱</label>
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
            <label for="change-email-code">新邮箱验证码</label>
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
                {{ emailCountdown > 0 ? `${emailCountdown}s` : emailSending ? '发送中' : '发送验证码' }}
              </button>
            </div>
            <p v-if="emailErrors.verifyCode" class="security-form__error">{{ emailErrors.verifyCode }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="emailLoading">保存新邮箱</button>
          </div>
          <p class="security-form__hint">修改成功后需使用新邮箱与原密码重新登录</p>
        </form>
      </AccountPanel>

      <AccountPanel title="账号信息">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>注册邮箱</dt><dd>{{ profile?.maskedEmail }}</dd></div>
          <div class="account-field-row">
            <dt>邮箱状态</dt>
            <dd :class="profile?.emailVerified ? 'account-status-text--success' : ''">
              {{ profile?.emailVerified ? '已验证' : '未验证' }}
            </dd>
          </div>
          <div class="account-field-row"><dt>账号状态</dt><dd class="account-status-text--success">正常</dd></div>
          <div class="account-field-row">
            <dt>最近登录</dt>
            <dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd>
          </div>
          <div class="account-field-row">
            <dt>密码更新时间</dt>
            <dd>{{ formatDateTime(security?.passwordUpdatedAt || '') }}</dd>
          </div>
        </dl>
        <p class="security-form__hint">如非本人操作，请立即修改密码并联系管理员</p>
      </AccountPanel>
    </div>

    <p class="security-tip">请勿向任何人透露密码或邮箱验证码</p>
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
