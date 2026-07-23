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
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = '两次输入的密码不一致'
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
      if (error.includes('当前密码')) errors.currentPassword = error
      else errors.newPassword = error
      return
    }
    ElMessage.success('密码修改成功')
    cancelForm()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="account-view">
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
      title="保护你的 CNPA 账号"
      description="管理登录邮箱与账号密码"
      status-text="安全状态正常"
      status-type="success"
    />

    <div class="security-grid">
      <AccountPanel title="修改密码">
        <form class="account-password-form security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="settings-current-password">当前密码</label>
            <PasswordInput
              id="settings-current-password"
              v-model="form.currentPassword"
              autocomplete="current-password"
              :invalid="Boolean(errors.currentPassword)"
            />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>

          <div class="security-form__field">
            <label for="settings-new-password">新密码</label>
            <PasswordInput
              id="settings-new-password"
              v-model="form.newPassword"
              autocomplete="new-password"
              :invalid="Boolean(errors.newPassword)"
            />
            <p class="security-form__hint">8 至 20 位，至少包含字母和数字</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>

          <div class="security-form__field">
            <label for="settings-confirm-password">确认新密码</label>
            <PasswordInput
              id="settings-confirm-password"
              v-model="form.confirmPassword"
              autocomplete="new-password"
              :invalid="Boolean(errors.confirmPassword)"
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
            <dd>{{ formatDateTime(account?.lastLoginAt) }}</dd>
          </div>
          <div class="account-field-row">
            <dt>密码更新时间</dt>
            <dd>{{ formatDateTime(security?.passwordUpdatedAt) }}</dd>
          </div>
        </dl>
      </AccountPanel>
    </div>

    <AccountPanel title="安全提示">
      <p class="security-tip">请勿向任何人透露密码或邮箱验证码</p>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.security-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-form__field label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 800;
}

.security-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #696969;
}

.security-form__error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--account-danger);
}

.security-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.security-tip {
  margin: 0;
  font-size: 13px;
  color: #696969;
}
</style>