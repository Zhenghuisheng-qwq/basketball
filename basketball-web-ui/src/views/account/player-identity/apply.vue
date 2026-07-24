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
const cert = computed(() => accountStore.playerCert)

const draft = reactive({
  realName: '',
  idType: 'ID_CARD',
  platform: '' as '' | 'PC' | 'PS' | 'XBOX',
  gameAccount: '',
  gameNickname: '',
  position: '' as '' | 'PG' | 'SG' | 'SF' | 'PF' | 'C',
  contactMobile: '',
})

onMounted(async () => {
  try {
    await accountStore.loadPlayerCertification()
    if (cert.value) {
      draft.realName = cert.value.realName || profile.value?.nickname || ''
      draft.idType = cert.value.idType || 'ID_CARD'
      draft.gameAccount = cert.value.gameAccount || ''
      draft.gameNickname = cert.value.gameNickname || profile.value?.nickname || ''
      draft.contactMobile = cert.value.contactMobile || ''
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
  realName: draft.realName.trim() || '未填写',
  platform: draft.platform ? IDENTITY_PLATFORM_LABELS[draft.platform] : '未选择',
  gameAccount: draft.gameAccount.trim() || '未填写',
  position: draft.position || '未选择',
  contact: draft.contactMobile.trim() || '未填写',
}))

async function handleSubmit(data: PlayerCertFormPayload) {
  if (submitting.value) return
  submitting.value = true
  try {
    await accountStore.submitPlayerVerificationFromForm({
      realName: data.realName,
      idType: data.idType,
      idNo: data.idNo,
      platform: data.platform,
      gameAccount: data.gameAccount,
      gameNickname: data.gameNickname,
      positionCode: data.position,
      contactMobile: data.contactMobile,
      idFrontFile: data.idFrontFile,
      idBackFile: data.idBackFile,
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
      description="填写认证资料并提交赛事管理员审核"
      :status-text="cert?.applicationStatusText || '资料未提交'"
      status-type="warning"
    />

    <div class="identity-apply-grid">
      <AccountPanel title="认证资料">
        <PlayerIdentityApplyForm
          :initial="{
            realName: draft.realName,
            idType: draft.idType,
            platform: draft.platform || undefined,
            gameAccount: draft.gameAccount,
            gameNickname: draft.gameNickname,
            position: draft.position || undefined,
            contactMobile: draft.contactMobile,
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
            <li>审核通过后生成永久球员 ID，永久球员 ID 不可修改。</li>
            <li>同一平台的游戏 ID 不得被多个球员重复绑定。</li>
            <li>提交后进入管理员审核，审核期间不能重复提交。</li>
            <li>球员认证与具体球队、赛事报名相互独立。</li>
          </ul>
        </AccountPanel>

        <AccountPanel title="当前资料状态">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">姓名</span><span>{{ statusSummary.realName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ statusSummary.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏账号</span><span>{{ statusSummary.gameAccount }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ statusSummary.position }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">联系电话</span><span>{{ statusSummary.contact }}</span></div>
          </div>
        </AccountPanel>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
