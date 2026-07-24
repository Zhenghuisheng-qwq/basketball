<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAccountStore } from '@/stores/account'
import { validatePasswordRules } from '@/utils/passwordRules'
import { getLoginLogPage, getOperationLogPage } from '@/api/accountSecurity'
import type { LoginLogItemDto, SecurityLogItemDto } from '@/api/types/accountSecurity'
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
    ElMessage.error(error instanceof Error ? error.message : '加载登录日志失败')
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
    ElMessage.error(error instanceof Error ? error.message : '加载操作日志失败')
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
    await loadOperationLogs()
  } finally {
    loading.value = false
  }
}

function loginResultText(result?: number) {
  return result === 1 ? '成功' : '失败'
}
</script>

<template>
  <div class="account-page account-center-vars">
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
      description="管理登录密码并查看安全记录"
      status-text="账号状态正常"
      status-type="success"
    />

    <div class="account-grid-2 security-layout">
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

    <div class="account-grid-2 security-layout security-logs">
      <AccountPanel title="登录日志">
        <div v-if="logsLoading && !loginLogs.length" class="security-form__hint">加载中...</div>
        <div v-else-if="!loginLogs.length" class="security-form__hint">暂无登录记录</div>
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>方式</th>
                <th>结果</th>
                <th>地区</th>
                <th>终端</th>
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

      <AccountPanel title="安全操作日志">
        <div v-if="logsLoading && !operationLogs.length" class="security-form__hint">加载中...</div>
        <div v-else-if="!operationLogs.length" class="security-form__hint">暂无操作记录</div>
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>事件</th>
                <th>变更</th>
                <th>终端</th>
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

    <p class="security-tip">请勿向任何人透露密码或邮箱验证码</p>
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
