<script setup lang="ts">
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
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="账号安全"
      en-title="ACCOUNT SECURITY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '账号安全' },
      ]"
    />

    <AccountStatusBanner
      title="保护你的 CNPA 账号"
      description="管理登录邮箱与账号密码"
      status-text="安全状态正常"
      status-type="success"
    />

    <div class="account-grid-2 security-layout">
      <AccountPanel title="修改密码">
        <form class="security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="current-password">当前密码</label>
            <PasswordInput
              id="current-password"
              v-model="form.currentPassword"
              autocomplete="current-password"
            />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="new-password">新密码</label>
            <PasswordInput
              id="new-password"
              v-model="form.newPassword"
              autocomplete="new-password"
            />
            <p class="security-form__hint">8 至 20 位，需包含字母与数字</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>
          <div class="security-form__field">
            <label for="confirm-password">确认新密码</label>
            <PasswordInput
              id="confirm-password"
              v-model="form.confirmPassword"
              autocomplete="new-password"
            />
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
          <div class="account-field-row">
            <dt>邮箱状态</dt>
            <dd class="account-status-text--success">已验证</dd>
          </div>
          <div class="account-field-row">
            <dt>账号状态</dt>
            <dd class="account-status-text--success">正常</dd>
          </div>
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
