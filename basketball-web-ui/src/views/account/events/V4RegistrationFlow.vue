<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import {
  getV4MyRegistrations,
  getV4RegistrationDetail,
  getV4RegistrationDivisionContext,
  saveV4Registration,
  submitV4Registration,
  withdrawV4Registration,
} from '@/api/v4PlayerEvents'
import type {
  V4RegistrationDivisionContext,
  V4RegistrationRosterPlayer,
} from '@/api/types/v4PlayerEvents'
import { v4PlatformLabel, v4GameModeLabel, v4RosterRuleText } from '@/utils/v4Display'
import { v4RegistrationStatusLabel } from '@/utils/v4AccountEventsMappers'
import { formatDateTimeShort } from '@/utils/participationMappers'

const route = useRoute()
const router = useRouter()

const pageLoading = ref(true)
const loadError = ref('')
const saving = ref(false)
const submitting = ref(false)
const division = ref<V4RegistrationDivisionContext | null>(null)
const selectedPlayerIds = ref<string[]>([])
const registrationId = ref('')
const divisionId = computed(() => String(route.params.divisionId || ''))

const rosterMin = computed(() => division.value?.lineupMinCount ?? (division.value?.gameMode === 'THREE_V_THREE' ? 3 : 5))
const rosterMax = computed(() => division.value?.lineupMaxCount ?? 8)
const rosterRuleText = computed(() => v4RosterRuleText(rosterMin.value, rosterMax.value, division.value?.gameMode))

const eligiblePlayers = computed<V4RegistrationRosterPlayer[]>(() => division.value?.eligiblePlayers || [])
const ineligiblePlayers = computed<V4RegistrationRosterPlayer[]>(() => division.value?.ineligiblePlayers || [])
const teamMemberCount = computed(() => division.value?.teamMemberCount ?? 0)
const eligibleCount = computed(() => division.value?.eligibleRosterCount ?? eligiblePlayers.value.length)
const selectedCount = computed(() => selectedPlayerIds.value.length)

const canMutate = computed(() => Boolean(division.value?.canRegister))
const isReadonly = computed(() => {
  const status = (division.value?.myRegistrationStatus || '').toUpperCase()
  if (!canMutate.value) return true
  return status === 'PENDING_REVIEW' || status === 'APPROVED' || status === 'CANCELLED'
})

const canSubmit = computed(() => {
  if (!canMutate.value || isReadonly.value) return false
  return selectedCount.value >= rosterMin.value && selectedCount.value <= rosterMax.value
})

const bannerStatus = computed(() => {
  const regStatus = (division.value?.myRegistrationStatus || '').toUpperCase()
  if (regStatus) {
    return {
      text: v4RegistrationStatusLabel(regStatus),
      type: regStatus === 'APPROVED' ? 'success' as const : regStatus === 'REJECTED' ? 'danger' as const : 'info' as const,
    }
  }
  const windowLabel = division.value?.registrationWindowStatusLabel
  const windowCode = (division.value?.registrationWindowStatus || '').toUpperCase()
  if (windowLabel) {
    return {
      text: windowLabel,
      type: windowCode === 'OPEN' ? 'success' as const : 'info' as const,
    }
  }
  if (division.value?.cannotRegisterReason) {
    return { text: '不可报名', type: 'danger' as const }
  }
  return { text: '未报名', type: 'info' as const }
})

const eligibilityHint = computed(() => {
  if (!division.value) return ''
  if (division.value.canRegister) return ''
  if (division.value.cannotRegisterReason) return division.value.cannotRegisterReason
  if ((division.value.registrationWindowStatus || '').toUpperCase() === 'CLOSED' && division.value.registrationEndTime) {
    return `报名已于 ${formatDateTimeShort(division.value.registrationEndTime)} 截止`
  }
  return ''
})

const emptyRosterHint = computed(() => {
  if (!division.value) return ''
  if (teamMemberCount.value <= 0) {
    return '当前球队暂无成员。请先前往「我的球队 → 成员管理」邀请成员补充阵容。'
  }
  if (eligibleCount.value <= 0) {
    const platform = v4PlatformLabel(division.value.platformCode) || '对应平台'
    return `当前球队成员暂无符合 ${platform} 赛事资格的球员。请先完成游戏平台身份认证。`
  }
  return '暂无可选参赛球员'
})

const submitHint = computed(() => {
  if (selectedCount.value < rosterMin.value) {
    return `至少选择 ${rosterMin.value} 名符合资格球员才能提交审核（当前 ${selectedCount.value} / ${rosterMin.value}）`
  }
  if (selectedCount.value > rosterMax.value) {
    return `阵容最多 ${rosterMax.value} 人`
  }
  return `草稿可不足最低人数；提交审核需要 ${rosterMin.value}~${rosterMax.value} 人`
})

function normalizeContext(ctx: V4RegistrationDivisionContext): V4RegistrationDivisionContext {
  return {
    ...ctx,
    divisionId: String(ctx.divisionId ?? divisionId.value),
    seasonId: ctx.seasonId != null ? String(ctx.seasonId) : '',
    grantTeamId: ctx.grantTeamId != null ? String(ctx.grantTeamId) : undefined,
    myRegistrationId: ctx.myRegistrationId != null ? String(ctx.myRegistrationId) : undefined,
    eligiblePlayers: (ctx.eligiblePlayers || []).map((p) => ({
      ...p,
      playerId: p.playerId != null ? String(p.playerId) : undefined,
      teamMemberId: p.teamMemberId != null ? String(p.teamMemberId) : undefined,
      memberId: p.memberId != null ? String(p.memberId) : undefined,
    })),
    ineligiblePlayers: (ctx.ineligiblePlayers || []).map((p) => ({
      ...p,
      playerId: p.playerId != null ? String(p.playerId) : undefined,
      teamMemberId: p.teamMemberId != null ? String(p.teamMemberId) : undefined,
      memberId: p.memberId != null ? String(p.memberId) : undefined,
    })),
  }
}

function defaultSelectedIds(players: V4RegistrationRosterPlayer[], existing: string[]) {
  if (existing.length) {
    const eligibleIds = new Set(players.map((p) => String(p.playerId)))
    const kept = existing.filter((id) => eligibleIds.has(id))
    if (kept.length) return kept
  }
  const captain = players.find((p) => p.captain || p.roleType === 'CAPTAIN')
  if (captain?.playerId) return [String(captain.playerId)]
  return []
}

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  division.value = null
  try {
    if (!divisionId.value) {
      loadError.value = '无效赛事'
      return
    }

    const [contextRes, registrationsRes] = await Promise.all([
      getV4RegistrationDivisionContext(divisionId.value),
      getV4MyRegistrations(),
    ])
    const ctx = contextRes.data
    if (!ctx || ctx.divisionExists === false) {
      loadError.value = '赛事不存在或已失效'
      return
    }
    division.value = normalizeContext(ctx)

    const existing = (registrationsRes.data || []).find(
      (item) => String(item.divisionId) === divisionId.value,
    )
    registrationId.value = String(
      division.value.myRegistrationId
      || existing?.registrationId
      || route.query.registrationId
      || '',
    )

    let existingSelected: string[] = []
    if (registrationId.value) {
      try {
        const detailRes = await getV4RegistrationDetail(registrationId.value)
        const players = detailRes.data?.players || []
        if (players.length) {
          existingSelected = players.map((p: { playerId?: string | number }) => String(p.playerId))
        }
      } catch {
        // ignore detail load failure; user can re-select lineup
      }
    }
    selectedPlayerIds.value = defaultSelectedIds(eligiblePlayers.value, existingSelected)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    pageLoading.value = false
  }
}

function isCaptainPlayer(player: V4RegistrationRosterPlayer) {
  return Boolean(player.captain || player.roleType === 'CAPTAIN')
}

function togglePlayer(player: V4RegistrationRosterPlayer) {
  if (isReadonly.value || !player.playerId) return
  if (isCaptainPlayer(player)) return
  const id = String(player.playerId)
  if (selectedPlayerIds.value.includes(id)) {
    selectedPlayerIds.value = selectedPlayerIds.value.filter((item) => item !== id)
  } else {
    selectedPlayerIds.value = [...selectedPlayerIds.value, id]
  }
}

async function handleSave() {
  if (!canMutate.value) {
    ElMessage.warning(eligibilityHint.value || '当前不可报名')
    return
  }
  if (!division.value?.grantTeamId) {
    ElMessage.warning('无有效队长授权球队')
    return
  }
  if (!selectedPlayerIds.value.length) {
    ElMessage.warning('请至少选择队长进入阵容草稿')
    return
  }
  if (selectedPlayerIds.value.length > rosterMax.value) {
    ElMessage.warning(`阵容最多 ${rosterMax.value} 人`)
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
    ElMessage.success(
      selectedCount.value < rosterMin.value
        ? `草稿已保存（当前 ${selectedCount.value}/${rosterMin.value}，提交前需补齐）`
        : '草稿已保存',
    )
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSubmit() {
  if (!canMutate.value) {
    ElMessage.warning(eligibilityHint.value || '当前不可报名')
    return
  }
  if (!canSubmit.value) {
    ElMessage.warning(submitHint.value)
    return
  }
  if (!registrationId.value) {
    await handleSave()
  }
  if (!registrationId.value) return
  if (selectedCount.value < rosterMin.value) {
    ElMessage.warning(`至少选择 ${rosterMin.value} 名符合资格球员`)
    return
  }
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
      zh-title="球队报名"
      en-title="TEAM REGISTRATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-events?tab=available' },
        { label: '球队报名' },
      ]"
    />

    <AccountStatusBanner
      title="球队报名"
      :description="division?.seasonTitle || '选择参赛阵容并提交报名审核'"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <p v-if="pageLoading" class="v4-reg-loading">加载中…</p>
    <p v-else-if="loadError" class="v4-reg-error">{{ loadError }}</p>

    <template v-else-if="division">
      <AccountPanel title="赛事信息">
        <p>{{ division.seriesName }} · {{ division.seasonTitle }}</p>
        <p>{{ division.divisionName }} · {{ v4PlatformLabel(division.platformCode) }} · {{ v4GameModeLabel(division.gameMode) }}</p>
        <p class="v4-reg-hint">{{ rosterRuleText }}</p>
        <p v-if="division.registrationStartTime || division.registrationEndTime" class="v4-reg-hint">
          报名时间：{{ formatDateTimeShort(division.registrationStartTime) }}
          ~
          {{ formatDateTimeShort(division.registrationEndTime) }}
        </p>
        <p v-if="eligibilityHint" class="v4-reg-warn">{{ eligibilityHint }}</p>
      </AccountPanel>

      <AccountPanel title="选择阵容">
        <div class="v4-reg-team-meta">
          <p>当前报名球队：<strong>{{ division.grantTeamName || '暂无球队' }}</strong></p>
          <p>当前球队人数：{{ teamMemberCount }} 人</p>
          <p>符合报名资格：{{ eligibleCount }} 人</p>
          <p>阵容要求：{{ rosterMin }} ~ {{ rosterMax }} 人</p>
          <p class="v4-reg-hint">审核中不占名额；审核通过后进入已参加赛事。</p>
          <p class="v4-reg-hint">{{ submitHint }}</p>
        </div>

        <div v-if="eligiblePlayers.length" class="v4-reg-roster">
          <p class="v4-reg-section-title">可选球员</p>
          <label
            v-for="player in eligiblePlayers"
            :key="String(player.playerId)"
            class="v4-reg-roster-item"
          >
            <input
              type="checkbox"
              :checked="selectedPlayerIds.includes(String(player.playerId))"
              :disabled="isReadonly || isCaptainPlayer(player)"
              @change="togglePlayer(player)"
            />
            <span class="v4-reg-player-main">
              <strong>{{ player.playerNo || player.playerId }}</strong>
              <span>{{ player.displayName || '-' }}</span>
              <span>{{ player.platformGameId || `${v4PlatformLabel(division.platformCode)} 未绑定` }}</span>
            </span>
            <AccountStatusBadge :text="player.roleTypeText || (isCaptainPlayer(player) ? '队长' : '队员')" />
          </label>
        </div>

        <div v-else class="v4-reg-empty">
          <p>{{ emptyRosterHint }}</p>
          <button
            type="button"
            class="account-btn-secondary"
            @click="router.push('/account/team')"
          >
            管理球队成员
          </button>
        </div>

        <div v-if="ineligiblePlayers.length" class="v4-reg-roster v4-reg-roster--ineligible">
          <p class="v4-reg-section-title">不可选成员</p>
          <div
            v-for="player in ineligiblePlayers"
            :key="`ineligible-${player.playerId || player.teamMemberId}`"
            class="v4-reg-roster-item v4-reg-roster-item--disabled"
          >
            <span class="v4-reg-player-main">
              <strong>{{ player.playerNo || player.playerId || '-' }}</strong>
              <span>{{ player.displayName || '-' }}</span>
              <span class="v4-reg-warn-inline">{{ player.reasonMessage || '不符合参赛资格' }}</span>
            </span>
            <AccountStatusBadge :text="player.roleTypeText || '队员'" />
          </div>
        </div>
      </AccountPanel>

      <div class="v4-reg-actions">
        <button
          v-if="!isReadonly"
          type="button"
          class="account-btn-secondary"
          :disabled="saving || !selectedPlayerIds.length"
          @click="handleSave"
        >
          保存草稿
        </button>
        <button
          v-if="!isReadonly"
          type="button"
          class="account-btn-primary"
          :disabled="submitting || !canSubmit"
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
.v4-reg-hint,
.v4-reg-warn {
  font-size: 14px;
}

.v4-reg-error,
.v4-reg-warn,
.v4-reg-warn-inline {
  color: #b42318;
}

.v4-reg-hint {
  color: #666;
  margin: 0 0 8px;
}

.v4-reg-warn {
  margin: 0 0 12px;
}

.v4-reg-team-meta {
  margin-bottom: 16px;

  p {
    margin: 0 0 6px;
    font-size: 14px;
  }
}

.v4-reg-section-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
}

.v4-reg-roster {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 18px;
}

.v4-reg-roster-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.v4-reg-roster-item--disabled {
  opacity: 0.85;
}

.v4-reg-player-main {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  font-size: 13px;
}

.v4-reg-empty {
  padding: 16px 0 8px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-start;
  color: #666;
  font-size: 14px;
}

.v4-reg-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}
</style>
