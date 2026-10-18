<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
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
  submitV4RosterChange,
  withdrawV4Registration,
} from '@/api/v4PlayerEvents'
import type {
  V4RegistrationDivisionContext,
  V4RegistrationRosterPlayer,
} from '@/api/types/v4PlayerEvents'
import { v4PlatformLabel, v4GameModeLabel, v4RosterRuleText } from '@/utils/v4Display'
import { v4RegistrationStatusLabel } from '@/utils/v4AccountEventsMappers'
import { formatDateTimeShort } from '@/utils/participationMappers'
import { isKnownReasonCode } from '@/utils/apiError'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()

function rosterReason(player: V4RegistrationRosterPlayer) {
  if (isKnownReasonCode(player.reasonCode)) {
    return t(`errors.${player.reasonCode}`)
  }
  if (!String(locale.value).toLowerCase().startsWith('en') && player.reasonMessage) {
    return player.reasonMessage
  }
  return t('accountEvents.registration.ineligible')
}

const pageLoading = ref(true)
const loadError = ref('')
const saving = ref(false)
const submitting = ref(false)
const division = ref<V4RegistrationDivisionContext | null>(null)
const selectedPlayerIds = ref<string[]>([])
const approvedPlayerIds = ref<string[]>([])
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
const rosterChangeMode = ref(false)
const canRequestRosterChange = computed(() => Boolean(division.value?.rosterChangeAllowed && registrationId.value))
const pendingRosterCount = computed(() => {
  if (!canRequestRosterChange.value) return 0
  const approved = new Set(approvedPlayerIds.value)
  return eligiblePlayers.value.filter((player) => player.playerId && !approved.has(String(player.playerId))).length
})
const isReadonly = computed(() => {
  if (rosterChangeMode.value) return false
  const status = (division.value?.myRegistrationStatus || '').toUpperCase()
  if (!canMutate.value) return true
  return status === 'PENDING_REVIEW' || status === 'APPROVED' || status === 'CANCELLED'
})

const canSubmit = computed(() => {
  if (!canMutate.value || isReadonly.value) return false
  return selectedCount.value >= rosterMin.value && selectedCount.value <= rosterMax.value
})

const bannerStatus = computed(() => {
  if ((division.value?.eventEligibilityStatus || '').toUpperCase() === 'INELIGIBLE') {
    return {
      text: t('accountEvents.registration.rosterBelowMinimum', {
        current: division.value?.currentEventRosterCount ?? 0,
        min: rosterMin.value,
      }),
      type: 'danger' as const,
    }
  }
  const regStatus = (division.value?.myRegistrationStatus || '').toUpperCase()
  if (regStatus) {
    return {
      text: v4RegistrationStatusLabel(regStatus, division.value?.participationRole),
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
    return { text: t('accountEvents.common.cannotRegister'), type: 'danger' as const }
  }
  return { text: t('accountEvents.common.notRegistered'), type: 'info' as const }
})

const eligibilityHint = computed(() => {
  if (!division.value) return ''
  if (division.value.canRegister) return ''
  if (division.value.cannotRegisterReason) return division.value.cannotRegisterReason
  if ((division.value.registrationWindowStatus || '').toUpperCase() === 'CLOSED' && division.value.registrationEndTime) {
    return t('accountEvents.common.closedAt', { time: formatDateTimeShort(division.value.registrationEndTime) })
  }
  return ''
})

const emptyRosterHint = computed(() => {
  if (!division.value) return ''
  if (teamMemberCount.value <= 0) {
    return t('accountEvents.registration.emptyNoMembers')
  }
  if (eligibleCount.value <= 0) {
    const platform = v4PlatformLabel(division.value.platformCode) || t('accountEvents.registration.fallbackPlatform')
    return t('accountEvents.registration.emptyNoEligible', { platform })
  }
  return t('accountEvents.registration.emptyNoPlayers')
})

const submitHint = computed(() => {
  if (selectedCount.value < rosterMin.value) {
    return t('accountEvents.registration.minPlayersSubmit', { min: rosterMin.value, current: selectedCount.value })
  }
  if (selectedCount.value > rosterMax.value) {
    return t('accountEvents.registration.rosterMax', { max: rosterMax.value })
  }
  return t('accountEvents.registration.draftRange', { min: rosterMin.value, max: rosterMax.value })
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
      loadError.value = t('accountEvents.registration.invalidEvent')
      return
    }

    const [contextRes, registrationsRes] = await Promise.all([
      getV4RegistrationDivisionContext(divisionId.value),
      getV4MyRegistrations(),
    ])
    const ctx = contextRes.data
    if (!ctx || ctx.divisionExists === false) {
      loadError.value = t('accountEvents.registration.eventMissing')
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
    approvedPlayerIds.value = []
    if (registrationId.value) {
      try {
        const detailRes = await getV4RegistrationDetail(registrationId.value)
        const players = detailRes.data?.players || []
        if (players.length) {
          existingSelected = players.map((p: { playerId?: string | number }) => String(p.playerId))
        }
        approvedPlayerIds.value = existingSelected.filter((id) => eligiblePlayers.value.some((player) => String(player.playerId) === id))
      } catch {
        // ignore detail load failure; user can re-select lineup
      }
    }
    selectedPlayerIds.value = defaultSelectedIds(eligiblePlayers.value, existingSelected)
    if (route.query.rosterChange === '1') {
      if (canRequestRosterChange.value) {
        rosterChangeMode.value = true
      } else {
        ElMessage.warning(t('accountEvents.registration.changeRosterUnavailable'))
      }
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t('accountEvents.common.loadFailed')
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
    ElMessage.warning(eligibilityHint.value || t('accountEvents.registration.cannotRegisterNow'))
    return
  }
  if (!division.value?.grantTeamId) {
    ElMessage.warning(t('accountEvents.registration.noGrantTeam'))
    return
  }
  if (!selectedPlayerIds.value.length) {
    ElMessage.warning(t('accountEvents.registration.selectCaptain'))
    return
  }
  if (selectedPlayerIds.value.length > rosterMax.value) {
    ElMessage.warning(t('accountEvents.registration.rosterMax', { max: rosterMax.value }))
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
        ? t('accountEvents.registration.draftSavedPartial', { current: selectedCount.value, min: rosterMin.value })
        : t('accountEvents.common.draftSaved'),
    )
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function handleRosterChange() {
  if (!canRequestRosterChange.value || !registrationId.value || !division.value?.grantTeamId) return
  if (selectedCount.value < rosterMin.value || selectedCount.value > rosterMax.value) {
    ElMessage.warning(t('accountEvents.registration.minPlayersSubmit', { min: rosterMin.value, current: selectedCount.value }))
    return
  }
  submitting.value = true
  try {
    await submitV4RosterChange({
      registrationId: registrationId.value,
      playerIds: selectedPlayerIds.value,
    })
    ElMessage.success(t('accountEvents.registration.changeRosterSubmitted'))
    rosterChangeMode.value = false
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.submitFailed'))
  } finally {
    submitting.value = false
  }
}

async function handleSubmit() {
  if (!canMutate.value) {
    ElMessage.warning(eligibilityHint.value || t('accountEvents.registration.cannotRegisterNow'))
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
    ElMessage.warning(t('accountEvents.registration.minPlayers', { min: rosterMin.value }))
    return
  }
  submitting.value = true
  try {
    await submitV4Registration({ registrationId: registrationId.value })
    ElMessage.success(t('accountEvents.common.submittedReview'))
    router.push('/account/my-events?tab=applications')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.submitFailed'))
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  if (!registrationId.value) return
  try {
    await withdrawV4Registration({ registrationId: registrationId.value })
    ElMessage.success(t('accountEvents.common.withdrawn'))
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountEvents.common.withdrawFailed'))
  }
}

onMounted(() => {
  void bootstrap()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountEvents.registration.pageTitle')"
      en-title="TEAM REGISTRATION"
      :breadcrumb-items="[
        { label: t('accountEvents.breadcrumb.home'), to: '/home' },
        { label: t('accountEvents.breadcrumb.account'), to: '/account/overview' },
        { label: t('accountEvents.breadcrumb.myEvents'), to: '/account/my-events?tab=available' },
        { label: t('accountEvents.breadcrumb.teamRegistration') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountEvents.registration.pageTitle')"
      :description="division?.seasonTitle || t('accountEvents.registration.bannerDesc')"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <p v-if="pageLoading" class="v4-reg-loading">{{ t('accountEvents.registration.loading') }}</p>
    <p v-else-if="loadError" class="v4-reg-error">{{ loadError }}</p>

    <template v-else-if="division">
      <AccountPanel :title="t('accountEvents.registration.eventInfo')">
        <p>{{ division.seriesName }} · {{ division.seasonTitle }}</p>
        <p>{{ division.divisionName }} · {{ v4PlatformLabel(division.platformCode) }} · {{ v4GameModeLabel(division.gameMode) }}</p>
        <p class="v4-reg-hint">{{ rosterRuleText }}</p>
        <p v-if="division.registrationStartTime || division.registrationEndTime" class="v4-reg-hint">
          {{ t('accountEvents.registration.window', { start: formatDateTimeShort(division.registrationStartTime), end: formatDateTimeShort(division.registrationEndTime) }) }}
        </p>
        <p v-if="eligibilityHint" class="v4-reg-warn">{{ eligibilityHint }}</p>
      </AccountPanel>

      <AccountPanel :title="t('accountEvents.registration.selectRoster')">
        <div class="v4-reg-team-meta">
          <p>{{ t('accountEvents.registration.currentTeam') }}<strong>{{ division.grantTeamName || t('accountEvents.registration.noTeam') }}</strong></p>
          <p>{{ t('accountEvents.registration.teamSize', { count: teamMemberCount }) }}</p>
          <p v-if="division.currentEventRosterCount != null">{{ t('accountEvents.registration.approvedEventRoster', { count: division.currentEventRosterCount }) }}</p>
          <p>{{ t('accountEvents.registration.eligibleCount', { count: eligibleCount }) }}</p>
          <p v-if="division.myRegistrationStatus === 'APPROVED' && !division.rosterLocked" class="v4-reg-hint">{{ t('accountEvents.registration.autoSyncHint') }}</p>
          <p v-if="pendingRosterCount > 0" class="v4-reg-warn">{{ t('accountEvents.registration.pendingRosterMembers', { count: pendingRosterCount }) }}</p>
          <p>{{ t('accountEvents.registration.rosterNeed', { min: rosterMin, max: rosterMax }) }}</p>
          <p class="v4-reg-hint">{{ t('accountEvents.registration.reviewHint') }}</p>
          <p class="v4-reg-hint">{{ submitHint }}</p>
        </div>

        <div v-if="eligiblePlayers.length" class="v4-reg-roster">
          <p class="v4-reg-section-title">{{ t('accountEvents.registration.eligiblePlayers') }}</p>
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
              <span>{{ player.platformGameId || t('accountEvents.registration.unbound', { platform: v4PlatformLabel(division.platformCode) }) }}</span>
            </span>
            <AccountStatusBadge :text="player.roleTypeText || (isCaptainPlayer(player) ? t('accountEvents.common.captain') : t('accountEvents.common.member'))" />
          </label>
        </div>

        <div v-else class="v4-reg-empty">
          <p>{{ emptyRosterHint }}</p>
          <button
            type="button"
            class="account-btn-secondary"
            @click="router.push('/account/team')"
          >
            {{ t('accountEvents.registration.manageMembers') }}
          </button>
        </div>

        <div v-if="ineligiblePlayers.length" class="v4-reg-roster v4-reg-roster--ineligible">
          <p class="v4-reg-section-title">{{ t('accountEvents.registration.ineligiblePlayers') }}</p>
          <div
            v-for="player in ineligiblePlayers"
            :key="`ineligible-${player.playerId || player.teamMemberId}`"
            class="v4-reg-roster-item v4-reg-roster-item--disabled"
          >
            <span class="v4-reg-player-main">
              <strong>{{ player.playerNo || player.playerId || '-' }}</strong>
              <span>{{ player.displayName || '-' }}</span>
              <span class="v4-reg-warn-inline">{{ rosterReason(player) }}</span>
            </span>
            <AccountStatusBadge :text="player.roleTypeText || t('accountEvents.common.member')" />
          </div>
        </div>
      </AccountPanel>

      <div class="v4-reg-actions">
        <button
          v-if="!isReadonly && !rosterChangeMode"
          type="button"
          class="account-btn-secondary"
          :disabled="saving || !selectedPlayerIds.length"
          @click="handleSave"
        >
          {{ t('accountEvents.common.saveDraft') }}
        </button>
        <button
          v-if="canRequestRosterChange && !rosterChangeMode"
          type="button"
          class="account-btn-primary"
          @click="rosterChangeMode = true"
        >
          {{ t('accountEvents.registration.changeRoster') }}
        </button>
        <p v-if="rosterChangeMode" class="v4-reg-hint">{{ t('accountEvents.registration.changeRosterHint') }}</p>
        <button
          v-if="rosterChangeMode"
          type="button"
          class="account-btn-primary"
          :disabled="submitting"
          @click="handleRosterChange"
        >
          {{ t('accountEvents.registration.changeRoster') }}
        </button>
        <button
          v-if="!isReadonly && !rosterChangeMode"
          type="button"
          class="account-btn-primary"
          :disabled="submitting || !canSubmit"
          @click="handleSubmit"
        >
          {{ t('accountEvents.common.submitReview') }}
        </button>
        <button
          v-if="division.myRegistrationStatus === 'PENDING_REVIEW'"
          type="button"
          class="account-btn-secondary"
          @click="handleWithdraw"
        >
          {{ t('accountEvents.common.withdraw') }}
        </button>
        <button type="button" class="account-btn-secondary" @click="router.push('/account/my-events?tab=applications')">
          {{ t('accountEvents.common.backToApplications') }}
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
