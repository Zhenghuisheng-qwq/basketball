<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import {
  getV4AvailableDivisions,
  getV4MyRegistrations,
  getV4RegistrationDetail,
  saveV4Registration,
  submitV4Registration,
  withdrawV4Registration,
} from '@/api/v4PlayerEvents'
import type { V4AvailableDivision } from '@/api/types/v4PlayerEvents'
import { useTeamStore } from '@/stores/team'
import { v4PlatformLabel } from '@/utils/v4Display'
import { v4RegistrationStatusLabel } from '@/utils/v4AccountEventsMappers'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()

const pageLoading = ref(true)
const loadError = ref('')
const saving = ref(false)
const submitting = ref(false)
const division = ref<V4AvailableDivision | null>(null)
const selectedPlayerIds = ref<string[]>([])
const registrationId = ref('')

const divisionId = computed(() => String(route.params.divisionId || ''))
const isReadonly = computed(() => {
  const status = (division.value?.myRegistrationStatus || '').toUpperCase()
  return status === 'PENDING_REVIEW' || status === 'APPROVED' || status === 'CANCELLED'
})

const bannerStatus = computed(() => {
  const status = division.value?.myRegistrationStatus
  return {
    text: v4RegistrationStatusLabel(status),
    type: status === 'APPROVED' ? 'success' as const : status === 'REJECTED' ? 'danger' as const : 'info' as const,
  }
})

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    await teamStore.bootstrap()
    if (!teamStore.isCaptain) {
      loadError.value = '仅队长可发起球队报名'
      return
    }
    const [divisionsRes, registrationsRes] = await Promise.all([
      getV4AvailableDivisions(),
      getV4MyRegistrations(),
    ])
    division.value = (divisionsRes.data || []).find((item) => item.divisionId === divisionId.value) || null
    if (!division.value) {
      loadError.value = '未找到可报名 Division'
      return
    }
    const existing = (registrationsRes.data || []).find((item) => item.divisionId === divisionId.value)
    registrationId.value = existing?.registrationId || String(route.query.registrationId || '')
    if (registrationId.value) {
      try {
        const detailRes = await getV4RegistrationDetail(registrationId.value)
        const players = detailRes.data?.players || []
        if (players.length) {
          selectedPlayerIds.value = players.map((p) => String(p.playerId))
        }
      } catch {
        // ignore detail load failure; user can re-select lineup
      }
    }
    const teamId = division.value.grantTeamId || teamStore.myTeamSummary?.teamId
    if (teamId) {
      await teamStore.loadTeamDetail(String(teamId))
    }
    if (!selectedPlayerIds.value.length) {
      const captainMember = teamStore.members.find((m) => m.roleType === 'CAPTAIN') || teamStore.members[0]
      if (captainMember?.playerId) {
        selectedPlayerIds.value = [String(captainMember.playerId)]
      }
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    pageLoading.value = false
  }
}

function togglePlayer(playerId: string) {
  if (isReadonly.value) return
  const id = String(playerId)
  if (selectedPlayerIds.value.includes(id)) {
    selectedPlayerIds.value = selectedPlayerIds.value.filter((item) => item !== id)
  } else {
    selectedPlayerIds.value = [...selectedPlayerIds.value, id]
  }
}

async function handleSave() {
  if (!division.value?.grantTeamId) {
    ElMessage.warning('无有效队长授权')
    return
  }
  if (selectedPlayerIds.value.length < 5) {
    ElMessage.warning('阵容至少 5 人')
    return
  }
  saving.value = true
  try {
    const res = await saveV4Registration({
      divisionId: divisionId.value,
      teamId: division.value.grantTeamId,
      playerIds: selectedPlayerIds.value,
    })
    registrationId.value = String(res.data || registrationId.value)
    ElMessage.success('草稿已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!registrationId.value) {
    await handleSave()
  }
  if (!registrationId.value) return
  submitting.value = true
  try {
    await submitV4Registration({ registrationId: registrationId.value })
    ElMessage.success('已提交审核')
    router.push('/account/my-events?tab=applications')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  if (!registrationId.value) return
  try {
    await withdrawV4Registration({ registrationId: registrationId.value })
    ElMessage.success('已撤回')
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  }
}

onMounted(() => {
  void bootstrap()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="V4 球队报名"
      en-title="TEAM REGISTRATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-events?tab=available' },
        { label: '球队报名' },
      ]"
    />

    <AccountStatusBanner
      title="V4 球队报名"
      :description="division?.seasonTitle || '选择阵容并提交审核'"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <p v-if="pageLoading" class="v4-reg-loading">加载中…</p>
    <p v-else-if="loadError" class="v4-reg-error">{{ loadError }}</p>

    <template v-else-if="division">
      <AccountPanel title="赛事信息">
        <p>{{ division.seriesName }} · {{ division.seasonTitle }}</p>
        <p>{{ division.divisionName }} · {{ v4PlatformLabel(division.platformCode) }}</p>
        <p v-if="division.cannotRegisterReason && !division.canRegister" class="v4-reg-hint">{{ division.cannotRegisterReason }}</p>
      </AccountPanel>

      <AccountPanel title="选择阵容">
        <p class="v4-reg-hint">审核中不占名额；审核通过后进入已参加赛事。</p>
        <div class="v4-reg-roster">
          <label
            v-for="player in teamStore.members"
            :key="player.playerId"
            class="v4-reg-roster-item"
          >
            <input
              type="checkbox"
              :checked="selectedPlayerIds.includes(String(player.playerId))"
              :disabled="isReadonly"
              @change="togglePlayer(String(player.playerId))"
            />
            <span>{{ player.nickname || player.gameNickname || player.playerNo }}</span>
            <AccountStatusBadge v-if="player.roleType === 'CAPTAIN'" text="队长" />
          </label>
        </div>
      </AccountPanel>

      <div class="v4-reg-actions">
        <button
          v-if="!isReadonly"
          type="button"
          class="account-btn-secondary"
          :disabled="saving"
          @click="handleSave"
        >
          保存草稿
        </button>
        <button
          v-if="!isReadonly"
          type="button"
          class="account-btn-primary"
          :disabled="submitting"
          @click="handleSubmit"
        >
          提交审核
        </button>
        <button
          v-if="division.myRegistrationStatus === 'PENDING_REVIEW'"
          type="button"
          class="account-btn-secondary"
          @click="handleWithdraw"
        >
          撤回
        </button>
        <button type="button" class="account-btn-secondary" @click="router.push('/account/my-events?tab=applications')">
          返回我的报名
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.v4-reg-loading,
.v4-reg-error,
.v4-reg-hint {
  font-size: 14px;
}

.v4-reg-error {
  color: #b42318;
}

.v4-reg-hint {
  color: #666;
  margin: 0 0 12px;
}

.v4-reg-roster {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.v4-reg-roster-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.v4-reg-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}
</style>
