<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import PlayerVerificationForm from '../components/PlayerVerificationForm.vue'
import PlayerVerificationSummary from '../components/PlayerVerificationSummary.vue'
import PlayerVerificationStatusCard from '../components/PlayerVerificationStatusCard.vue'
import { useAccountStore } from '@/stores/account'
import type { PlayerVerificationSnapshot } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const accountStore = useAccountStore()

const showSubmitConfirm = ref(false)
const showWithdrawConfirm = ref(false)
const showChangeForm = ref(false)
const pendingSubmit = ref<PlayerVerificationSnapshot | null>(null)

const profile = computed(() => accountStore.profile)
const verification = computed(() => accountStore.verification)

const status = computed(() => verification.value?.status || 'NOT_APPLIED')

const effectiveData = computed(() => {
  if (verification.value?.currentEffectiveData) return verification.value.currentEffectiveData
  if (status.value === 'APPROVED' || status.value === 'CHANGE_PENDING') {
    return verification.value
  }
  return null
})

const pendingData = computed(() => verification.value?.pendingChangeData)

const bannerConfig = computed(() => {
  switch (status.value) {
    case 'NOT_APPLIED':
      return {
        title: '尚未申请球员认证',
        description: '提交游戏资料后由赛事管理员审核',
        statusText: '未认证',
        statusType: 'warning' as const,
      }
    case 'PENDING':
      return {
        title: '认证申请审核中',
        description: '赛事管理员正在审核你提交的认证资料',
        statusText: '审核中',
        statusType: 'warning' as const,
      }
    case 'APPROVED':
      return {
        title: '认证资料已生效',
        description: '当前资料已通过赛事管理员审核',
        statusText: '认证球员',
        statusType: 'success' as const,
      }
    case 'CHANGE_PENDING':
      return {
        title: '认证资料变更审核中',
        description: '当前已通过资料继续生效',
        statusText: '认证球员',
        statusType: 'success' as const,
      }
    case 'REJECTED':
      return {
        title: '认证未通过',
        description: '请根据审核意见修改资料后重新提交',
        statusText: '需修改',
        statusType: 'danger' as const,
      }
    default:
      return {
        title: '球员认证',
        description: '',
        statusText: '',
        statusType: 'default' as const,
      }
  }
})

function requestSubmit(data: PlayerVerificationSnapshot) {
  pendingSubmit.value = data
  showSubmitConfirm.value = true
}

function confirmSubmit() {
  if (!pendingSubmit.value) return
  if (status.value === 'REJECTED') {
    accountStore.resubmitPlayerVerification(pendingSubmit.value)
  } else if (showChangeForm.value) {
    accountStore.submitVerificationChange(pendingSubmit.value)
    showChangeForm.value = false
  } else {
    accountStore.submitPlayerVerification(pendingSubmit.value)
  }
  pendingSubmit.value = null
  ElMessage.success('认证申请已提交')
}

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showChangeForm.value = false
  ElMessage.success('已撤回申请')
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员认证' },
      ]"
    />

    <AccountStatusBanner v-bind="bannerConfig" />

    <!-- NOT_APPLIED -->
    <div v-if="status === 'NOT_APPLIED'" class="verification-layout">
      <AccountPanel title="认证说明">
        <p class="verification-intro">
          提交你的游戏平台、游戏 ID 及角色位置，审核通过后可加入球队并参与赛事。
        </p>
      </AccountPanel>
      <AccountPanel title="申请表">
        <PlayerVerificationForm @submit="requestSubmit" />
      </AccountPanel>
    </div>

    <!-- PENDING -->
    <div v-else-if="status === 'PENDING'" class="verification-layout verification-layout--split">
      <AccountPanel title="本次提交资料">
        <PlayerVerificationSummary
          v-if="verification"
          :nickname="profile?.nickname || ''"
          :avatar-url="profile?.avatarUrl"
          :data="verification"
          :submitted-at="verification.submittedAt"
          :application-no="verification.applicationNo"
        />
      </AccountPanel>
      <PlayerVerificationStatusCard
        title="当前审核状态"
        status-text="审核中"
        status-type="warning"
        description="等待赛事管理员审核。审核结果将通过消息通知发送。"
      >
        <div class="verification-rules">
          <p>审核期间</p>
          <ol>
            <li>首次认证审核期间暂不生效。</li>
            <li>如需修改，可以撤回本次申请后重新提交。</li>
          </ol>
        </div>
        <button type="button" class="account-btn-secondary" @click="showWithdrawConfirm = true">
          撤回并修改
        </button>
      </PlayerVerificationStatusCard>
    </div>

    <!-- APPROVED -->
    <div v-else-if="status === 'APPROVED' && !showChangeForm" class="verification-layout verification-layout--split">
      <AccountPanel title="当前生效资料">
        <PlayerVerificationSummary
          v-if="effectiveData"
          :nickname="profile?.nickname || ''"
          :avatar-url="profile?.avatarUrl"
          :data="effectiveData"
        />
        <button type="button" class="account-btn-primary verification-action" @click="showChangeForm = true">
          申请修改球员认证
        </button>
      </AccountPanel>
      <div class="verification-side">
        <PlayerVerificationStatusCard
          title="当前状态"
          status-text="审核已通过"
          status-type="success"
          :description="`生效时间：${formatDateTime(verification?.effectiveAt)}`"
        />
        <AccountPanel title="修改规则">
          <ol class="verification-rules">
            <li>认证资料允许申请修改。</li>
            <li>每次修改均需赛事管理员重新审核。</li>
            <li>审核期间当前已通过资料继续生效。</li>
            <li>新申请未通过时，当前资料保持不变。</li>
          </ol>
        </AccountPanel>
      </div>
    </div>

    <!-- CHANGE_PENDING -->
    <div v-else-if="status === 'CHANGE_PENDING'" class="verification-layout">
      <div class="verification-layout verification-layout--split">
        <AccountPanel title="当前生效版本">
          <PlayerVerificationSummary
            v-if="effectiveData"
            :nickname="profile?.nickname || ''"
            :data="effectiveData"
            title="当前生效资料"
          />
        </AccountPanel>
        <AccountPanel title="待审核修改资料">
          <PlayerVerificationSummary
            v-if="pendingData"
            :nickname="profile?.nickname || ''"
            :data="pendingData"
            :submitted-at="verification?.submittedAt"
            :application-no="verification?.applicationNo"
            title="新提交版本"
          />
        </AccountPanel>
      </div>
      <button type="button" class="account-btn-secondary" @click="showWithdrawConfirm = true">
        撤回本次修改
      </button>
    </div>

    <!-- REJECTED -->
    <div v-else-if="status === 'REJECTED'" class="verification-layout verification-layout--split">
      <div class="verification-reject">
        <AccountPanel title="审核结果">
          <p class="verification-reject__title">本次认证未通过</p>
          <p>审核时间：{{ formatDateTime(verification?.reviewedAt) }}</p>
          <p class="verification-reject__hint">账号不会被锁定，可以修改后重新提交</p>
        </AccountPanel>
        <AccountPanel title="管理员审核意见">
          <p>{{ verification?.reviewerComment }}</p>
        </AccountPanel>
        <AccountPanel title="本次提交资料">
          <PlayerVerificationSummary
            v-if="verification"
            :nickname="profile?.nickname || ''"
            :data="verification"
            :highlight-fields="verification.rejectedFields"
          />
        </AccountPanel>
      </div>
      <div class="verification-side">
        <AccountPanel title="下一步操作">
          <ol class="verification-rules">
            <li>查看管理员审核意见</li>
            <li>修改标记的问题资料</li>
            <li>重新提交认证审核</li>
          </ol>
          <p class="verification-reject__hint">重新提交会生成新的审核记录。原申请与审核结果由系统保留。</p>
        </AccountPanel>
        <AccountPanel title="修改并重新提交">
          <PlayerVerificationForm
            :initial="verification || undefined"
            :rejected-fields="verification?.rejectedFields"
            submit-label="修改并重新提交"
            @submit="requestSubmit"
          />
        </AccountPanel>
      </div>
    </div>

    <!-- Change form from APPROVED -->
    <div v-if="showChangeForm && status === 'APPROVED'" class="verification-layout">
      <AccountPanel title="申请修改球员认证">
        <PlayerVerificationForm
          :initial="effectiveData || undefined"
          submit-label="提交修改申请"
          @submit="requestSubmit"
          @cancel="showChangeForm = false"
        />
      </AccountPanel>
    </div>

    <AccountConfirmDialog
      v-model="showSubmitConfirm"
      title="确认提交"
      message="提交后将进入审核，审核期间不可直接修改。确认提交球员认证？"
      confirm-text="确认提交"
      @confirm="confirmSubmit"
    />

    <AccountConfirmDialog
      v-model="showWithdrawConfirm"
      title="确认撤回"
      message="撤回后本次审核终止，重新提交后重新排队。确认撤回？"
      confirm-text="确认撤回"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view {
  width: 100%;
  min-width: 0;
}

.verification-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-layout--split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.verification-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-intro {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #696969;
}

.verification-rules {
  margin: 12px 0;
  padding-left: 18px;
  font-size: 13px;
  color: #696969;
  line-height: 1.7;
}

.verification-action {
  margin-top: 16px;
}

.verification-reject {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-reject__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 900;
  color: var(--account-danger);
}

.verification-reject__hint {
  font-size: 13px;
  color: #696969;
}

@media (max-width: 991px) {
  .verification-layout--split {
    grid-template-columns: 1fr;
  }
}
</style>