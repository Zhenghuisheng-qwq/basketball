<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PlayerIdentityApplyForm from '../components/PlayerIdentityApplyForm.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import type { PlayerVerificationSnapshot } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()

const profile = computed(() => accountStore.profile)
const draft = reactive({
  avatarUrl: profile.value?.avatarUrl || '',
  platform: '' as '' | 'PC' | 'PS' | 'XBOX',
  gameId: '',
  position: '' as '' | 'PG' | 'SG' | 'SF' | 'PF' | 'C',
  proofImageUrl: '',
})

const statusSummary = computed(() => ({
  avatar: draft.avatarUrl ? '已上传' : '未上传',
  platform: draft.platform ? IDENTITY_PLATFORM_LABELS[draft.platform] : '未选择',
  gameId: draft.gameId.trim() || '未填写',
  position: draft.position || '未选择',
  proof: draft.proofImageUrl ? '已上传' : '未上传',
}))

function handleSubmit(data: PlayerVerificationSnapshot & { avatarUrl?: string }) {
  if (data.avatarUrl) {
    accountStore.updateProfile({ avatarUrl: data.avatarUrl })
  }
  accountStore.submitPlayerVerification(data)
  ElMessage.success('认证申请已提交')
  router.push({ name: 'AccountPlayerIdentityPending' })
}

</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证申请"
      en-title="PLAYER VERIFICATION APPLICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '认证申请' },
      ]"
    />

    <AccountStatusBanner
      title="创建永久球员身份"
      description="填写认证资料并提交赛事管理员审核"
      status-text="资料未提交"
      status-type="warning"
    />

    <div class="identity-apply-grid">
      <AccountPanel title="认证资料">
        <PlayerIdentityApplyForm @submit="handleSubmit">
          <template #actions="{ complete, submit }">
            <div class="identity-apply-footer">
              <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
                返回球员身份
              </button>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="!complete"
                @click="submit()"
              >
                提交认证申请
              </button>
              <p v-if="!complete" class="identity-apply-footer__hint">请完成全部必填资料</p>
            </div>
          </template>
        </PlayerIdentityApplyForm>
      </AccountPanel>

      <div class="identity-apply-side">
        <AccountPanel title="提交说明">
          <ul class="identity-entry-rules">
            <li>审核通过后生成永久球员 ID，永久球员 ID 不可修改。</li>
            <li>同一平台的游戏 ID 不得被多个球员重复绑定。</li>
            <li>提交后进入管理员审核，审核期间不能重复提交。</li>
            <li>球员认证与具体球队、赛事报名相互独立。</li>
          </ul>
        </AccountPanel>

        <AccountPanel title="当前资料状态">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">头像</span><span>{{ statusSummary.avatar }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ statusSummary.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ statusSummary.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ statusSummary.position }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>{{ statusSummary.proof }}</span></div>
          </div>
        </AccountPanel>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>