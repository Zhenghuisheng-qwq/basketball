<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import {
  getV4MyPersonalRegistrations,
  getV4PersonalAvailableDivisions,
  saveV4PersonalRegistration,
  submitV4PersonalRegistration,
  withdrawV4PersonalRegistration,
} from '@/api/v4PlayerEvents'
import type { V4PersonalAvailableDivision } from '@/api/types/v4PlayerEvents'
import { v4GameModeLabel, v4PlatformLabel, v4RosterRuleText } from '@/utils/v4Display'
import { v4PersonalRegistrationStatusLabel } from '@/utils/v4AccountEventsMappers'

const route = useRoute()
const router = useRouter()

const pageLoading = ref(true)
const loadError = ref('')
const saving = ref(false)
const submitting = ref(false)
const division = ref<V4PersonalAvailableDivision | null>(null)
const preferredPosition = ref('')
const personalRegistrationId = ref('')

const divisionId = computed(() => String(route.params.divisionId || ''))
const rosterRuleText = computed(() =>
  v4RosterRuleText(division.value?.lineupMinCount, division.value?.lineupMaxCount, division.value?.gameMode)
)
const isReadonly = computed(() => {
  const status = (division.value?.myRegistrationStatus || '').toUpperCase()
  return ['PENDING_REVIEW', 'APPROVED', 'ASSIGNED', 'CONVERTED'].includes(status)
})

const bannerStatus = computed(() => {
  const status = division.value?.myRegistrationStatus
  return {
    text: v4PersonalRegistrationStatusLabel(status),
    type: status === 'APPROVED' || status === 'CONVERTED' ? 'success' as const
      : status === 'REJECTED' ? 'danger' as const : 'info' as const,
  }
})

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    const [divisionsRes, registrationsRes] = await Promise.all([
      getV4PersonalAvailableDivisions(),
      getV4MyPersonalRegistrations(),
    ])
    division.value = (divisionsRes.data || []).find((item) => item.divisionId === divisionId.value) || null
    if (!division.value) {
      loadError.value = '未找到可个人报名的 Division'
      return
    }
    const existing = (registrationsRes.data || []).find((item) => item.divisionId === divisionId.value)
    personalRegistrationId.value = existing?.personalRegistrationId || ''
    preferredPosition.value = existing?.preferredPosition || ''
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    pageLoading.value = false
  }
}

async function handleSave() {
  if (!division.value) return
  saving.value = true
  try {
    const res = await saveV4PersonalRegistration({
      divisionId: divisionId.value,
      preferredPosition: preferredPosition.value || undefined,
    })
    personalRegistrationId.value = res.data || personalRegistrationId.value
    ElMessage.success('草稿已保存')
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!personalRegistrationId.value) {
    await handleSave()
  }
  if (!personalRegistrationId.value) return
  submitting.value = true
  try {
    await submitV4PersonalRegistration({ personalRegistrationId: personalRegistrationId.value })
    ElMessage.success('提交成功，请等待审核')
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  if (!personalRegistrationId.value) return
  submitting.value = true
  try {
    await withdrawV4PersonalRegistration({ personalRegistrationId: personalRegistrationId.value })
    ElMessage.success('已撤回')
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.push({ name: 'AccountMyEvents', query: { tab: 'available' } })
}

onMounted(() => {
  void bootstrap()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="个人报名"
      en-title="Personal Registration"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-events' },
        { label: '个人报名' },
      ]"
    />

    <AccountStatusBanner
      title="个人报名说明"
      description="报名审核通过后，将由赛事运营线下抽签组队；系统仅展示和承接最终分组结果。"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <p v-if="pageLoading" class="page-loading">加载中…</p>
    <p v-else-if="loadError" class="page-error">{{ loadError }}</p>

    <AccountPanel v-else-if="division" title="报名信息">
      <div class="info-grid">
        <div><span>赛事</span><strong>{{ division.seasonTitle }}</strong></div>
        <div><span>赛区</span><strong>{{ division.divisionName }}</strong></div>
        <div><span>平台</span><strong>{{ v4PlatformLabel(division.platformCode) }}</strong></div>
        <div><span>模式</span><strong>{{ v4GameModeLabel(division.gameMode) }}</strong></div>
        <div><span>人数要求</span><strong>{{ rosterRuleText }}</strong></div>
      </div>

      <div class="form-block">
        <label for="preferredPosition">偏好位置（可选）</label>
        <input
          id="preferredPosition"
          v-model="preferredPosition"
          class="account-input"
          :disabled="isReadonly"
          placeholder="如 PG / SG / SF"
        />
      </div>

      <div class="actions">
        <button type="button" class="account-btn-secondary" @click="goBack">返回</button>
        <template v-if="!isReadonly">
          <button type="button" class="account-btn-secondary" :disabled="saving" @click="handleSave">保存草稿</button>
          <button type="button" class="account-btn-primary" :disabled="submitting" @click="handleSubmit">提交审核</button>
        </template>
        <button
          v-else-if="division.myRegistrationStatus === 'PENDING_REVIEW'"
          type="button"
          class="account-btn-secondary"
          :disabled="submitting"
          @click="handleWithdraw"
        >
          撤回
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
.page-loading,
.page-error {
  padding: 24px 0;
  color: rgba(0, 0, 0, 0.65);
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  margin-bottom: 20px;
  span {
    display: block;
    font-size: 13px;
    color: rgba(0, 0, 0, 0.55);
  }
  strong {
    font-size: 15px;
  }
}
.form-block {
  margin-bottom: 20px;
  label {
    display: block;
    margin-bottom: 8px;
    font-size: 14px;
  }
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
