<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAccountStore } from '@/stores/account'
import { validatePasswordRules } from '@/mock/account/security'
import { formatDateTime } from '@/utils/formatDate'

type SettingsTab = 'profile' | 'security'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const activeTab = ref<SettingsTab>('security')

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = tab === 'profile' ? 'profile' : 'security'
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
const profile = computed(() => accountStore.profile)
const security = computed(() => accountStore.security)
const account = computed(() => accountStore.currentAccount)

const profileForm = reactive({
  nickname: accountStore.profile?.nickname || accountStore.currentAccount?.nickname || '',
})

watch(
  () => accountStore.profile?.nickname,
  (v) => {
    if (v) profileForm.nickname = v
  },
)

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

async function savePassword() {
  if (loading.value || !validateForm()) return
  loading.value = true
  try {
    const error = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (error) {
      if (error.includes('密码')) errors.currentPassword = error
      else errors.newPassword = error
      return
    }
    ElMessage.success('密码已更新')
    cancelForm()
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  const nickname = profileForm.nickname.trim()
  if (!nickname) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (typeof accountStore.updateProfile === 'function') {
    await accountStore.updateProfile({ nickname })
  }
  ElMessage.success('资料已保存')
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
      description="维护账号资料与登录安全"
      status-text="账号状态正常"
      status-type="success"
    />

    <nav class="account-settings-tabs" aria-label="账号设置分类">
      <button
        type="button"
        class="account-settings-tabs__item"
        :class="{ 'account-settings-tabs__item--active': activeTab === 'profile' }"
        @click="setTab('profile')"
      >
        账号资料
      </button>
      <button
        type="button"
        class="account-settings-tabs__item"
        :class="{ 'account-settings-tabs__item--active': activeTab === 'security' }"
        @click="setTab('security')"
      >
        账号安全
      </button>
    </nav>

    <div v-if="activeTab === 'profile'" class="account-grid-2">
      <AccountPanel title="基本资料">
        <form class="security-form" @submit.prevent="saveProfile">
          <div class="security-form__field">
            <label for="settings-nickname">昵称</label>
            <input id="settings-nickname" v-model="profileForm.nickname" class="auth-input" type="text" />
          </div>
          <div class="security-form__field">
            <label>注册邮箱</label>
            <p class="security-form__hint">{{ profile?.maskedEmail || account?.maskedEmail }}</p>
          </div>
          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary">保存资料</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel title="账号概览">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>账号状态</dt><dd class="account-status-text--success">正常</dd></div>
          <div class="account-field-row"><dt>邮箱状态</dt><dd class="account-status-text--success">已验证</dd></div>
          <div class="account-field-row"><dt>最近登录</dt><dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd></div>
        </dl>
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
            <p class="security-form__hint">8 至 20 位，需包含字母与数字</p>
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

      <AccountPanel title="账号信息">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>注册邮箱</dt><dd>{{ profile?.maskedEmail }}</dd></div>
          <div class="account-field-row"><dt>邮箱状态</dt><dd class="account-status-text--success">已验证</dd></div>
          <div class="account-field-row"><dt>账号状态</dt><dd class="account-status-text--success">正常</dd></div>
          <div class="account-field-row"><dt>最近登录</dt><dd>{{ formatDateTime(security?.lastLoginAt || account?.lastLoginAt || '') }}</dd></div>
          <div class="account-field-row"><dt>密码更新时间</dt><dd>{{ formatDateTime(security?.passwordUpdatedAt || '') }}</dd></div>
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

.account-settings-tabs {
  display: flex;
  gap: 36px;
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

  :deep(.account-btn-primary) {
    min-width: 220px;
    width: min(100%, 280px);
    min-height: 48px;
  }

  :deep(.account-btn-secondary) {
    min-height: 48px;
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
</style>
