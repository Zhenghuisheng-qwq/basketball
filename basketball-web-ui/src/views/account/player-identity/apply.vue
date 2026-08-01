<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PlayerIdentityApplyForm from '../components/PlayerIdentityApplyForm.vue'
import type { PlayerCertFormPayload } from '../components/PlayerIdentityApplyForm.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()
const submitting = ref(false)

const profile = computed(() => accountStore.profile)
const account = computed(() => accountStore.currentAccount)
const cert = computed(() => accountStore.playerCert)

const draft = reactive({
  avatarUrl: '',
  platform: '' as '' | 'PC' | 'PS' | 'XBOX',
  gameAccount: '',
  position: '' as '' | 'PG' | 'SG' | 'SF' | 'PF' | 'C',
})

onMounted(async () => {
  try {
    if (!profile.value) {
      await accountStore.loadProfile()
    }
    draft.avatarUrl = profile.value?.avatarUrl || account.value?.avatarUrl || ''
    await accountStore.loadPlayerCertification()
    if (cert.value) {
      draft.gameAccount = cert.value.gameAccount || ''
      if (cert.value.positionCode) draft.position = cert.value.positionCode as typeof draft.position
      if (cert.value.gameTerminal) {
        const terminal = cert.value.gameTerminal.toUpperCase()
        if (terminal.includes('PS')) draft.platform = 'PS'
        else if (terminal.includes('XBOX')) draft.platform = 'XBOX'
        else draft.platform = 'PC'
      }
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载认证资料失败')
  }
})

const statusSummary = computed(() => ({
  nickname: profile.value?.nickname || account.value?.nickname || '未填写',
  email: profile.value?.maskedEmail || account.value?.maskedEmail || '未填写',
  avatar: draft.avatarUrl.trim() ? '已设置' : '未上传',
  platform: draft.platform ? IDENTITY_PLATFORM_LABELS[draft.platform] : '未选择',
  gameAccount: draft.gameAccount.trim() || '未填写',
  position: draft.position || '未选择',
}))

async function handleSubmit(data: PlayerCertFormPayload) {
  if (submitting.value) return
  submitting.value = true
  try {
    await accountStore.submitPlayerVerificationFromForm({
      platform: data.platform,
      gameAccount: data.gameAccount,
      positionCode: data.position,
      avatarFile: data.avatarFile,
      gameScreenshotFile: data.gameScreenshotFile,
    })
    ElMessage.success('认证申请已提交')
    router.push({ name: 'AccountPlayerIdentityStatusPending' })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
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
      description="填写赛事球员档案资料并提交管理员审核"
      :status-text="cert?.applicationStatusText || '资料未提交'"
      status-type="warning"
    />

    <div class="identity-apply-grid">
      <AccountPanel title="认证资料">
        <PlayerIdentityApplyForm
          :initial="{
            avatarUrl: draft.avatarUrl,
            platform: draft.platform || undefined,
            gameAccount: draft.gameAccount,
            position: draft.position || undefined,
          }"
          @submit="handleSubmit"
        >
          <template #actions="{ complete, submit }">
            <div class="identity-apply-footer">
              <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
                返回球员身份
              </button>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="!complete || submitting"
                @click="submit()"
              >
                {{ submitting ? '提交中...' : '提交认证申请' }}
              </button>
              <p v-if="!complete" class="identity-apply-footer__hint">请完成全部必填资料</p>
            </div>
          </template>
        </PlayerIdentityApplyForm>
      </AccountPanel>

      <div class="identity-apply-side">
        <AccountPanel title="提交说明">
          <ul class="identity-entry-rules">
            <li>请填写你当前常用的平台游戏ID，方便赛事方确认你在游戏中的身份。认证通过后会生成永久 CNPA 球员ID，后续可继续补充或变更其他平台游戏ID。</li>
            <li>审核通过后生成永久球员 ID，永久球员 ID 不可修改。</li>
            <li>同一平台的游戏 ID 不得被多个球员重复绑定。</li>
            <li>提交后进入管理员审核，审核期间不能重复提交。</li>
            <li>球员认证与具体球队、赛事报名相互独立；游戏平台不代表只能参加该平台赛事。</li>
          </ul>
        </AccountPanel>

        <AccountPanel title="当前资料状态">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">昵称</span><span>{{ statusSummary.nickname }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">邮箱</span><span>{{ statusSummary.email }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">头像</span><span>{{ statusSummary.avatar }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ statusSummary.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ statusSummary.gameAccount }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ statusSummary.position }}</span></div>
          </div>
        </AccountPanel>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
