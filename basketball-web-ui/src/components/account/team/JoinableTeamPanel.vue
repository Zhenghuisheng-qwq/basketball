<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import { useTeamStore } from '@/stores/team'
import type { TeamSummaryDto } from '@/api/types/teamAccount'
import { v4GameModeLabel, v4PlatformLabel } from '@/utils/v4Display'

const { t } = useI18n()
const teamStore = useTeamStore()
const router = useRouter()

const searchKeyword = ref('')
const appliedKeyword = ref('')
const showApplyDialog = ref(false)
const applyingTeam = ref<TeamSummaryDto | null>(null)
const applyReason = ref('')
const submitting = ref(false)

const pageSizeOptions = [10, 20, 50]
const paginationMeta = computed(() => t('accountTeam.joinable.meta', { total: teamStore.joinableTotal }))

function memberCountText(team: TeamSummaryDto) {
  const current = team.memberCount ?? team.currentMemberCount ?? 0
  return t('accountTeam.common.people', { count: current })
}

function teamLogoInitial(team: TeamSummaryDto) {
  return (team.shortName || team.teamName || '?').slice(0, 1)
}

function formatEventContext(team: TeamSummaryDto) {
  const platform = team.divisionPlatformCode || team.platformCode
  const parts = [
    team.seriesName,
    team.seasonName,
    team.divisionName,
    platform ? v4PlatformLabel(platform) : '',
    team.gameMode ? v4GameModeLabel(team.gameMode) : '',
  ].filter(Boolean)
  return parts.join(' · ') || t('accountTeam.joinable.eventMissing')
}

function resolveTeamStatus(team: TeamSummaryDto): { text: string; type: 'success' | 'warning' | 'danger' | 'default' } {
  if (team.hasPendingApply || team.disabledReason === '申请审核中') {
    return { text: t('accountTeam.joinable.applied'), type: 'warning' }
  }
  if (team.disabledReason === '球队已满') {
    return { text: t('accountTeam.joinable.full'), type: 'danger' }
  }
  if (team.canApply !== false) {
    return { text: t('accountTeam.joinable.canApply'), type: 'success' }
  }
  return { text: t('accountTeam.joinable.cannotApply'), type: 'default' }
}

function resolveApplyButtonText(team: TeamSummaryDto) {
  if (team.hasPendingApply || team.disabledReason === '申请审核中') {
    return t('accountTeam.joinable.applying')
  }
  if (team.disabledReason === '球队已满') {
    return t('accountTeam.joinable.teamFull')
  }
  if (team.canApply === false) {
    return t('accountTeam.joinable.cannotApply')
  }
  return t('accountTeam.joinable.apply')
}

function isApplyDisabled(team: TeamSummaryDto) {
  return team.canApply === false
}

function applyTooltip(team: TeamSummaryDto) {
  return team.disabledReason || team.cannotApplyReason || undefined
}

async function loadJoinableTeams(page = teamStore.joinablePage) {
  await teamStore.loadJoinableTeams({
    currentPage: page,
    pageSize: teamStore.joinablePageSize,
    keyword: appliedKeyword.value.trim() || undefined,
  })
}

async function searchJoinable() {
  if (teamStore.joinableLoading) return
  appliedKeyword.value = searchKeyword.value.trim()
  await loadJoinableTeams(1)
}

async function resetSearch() {
  if (teamStore.joinableLoading) return
  searchKeyword.value = ''
  appliedKeyword.value = ''
  await loadJoinableTeams(1)
}

async function changePage(page: number) {
  if (teamStore.joinableLoading) return
  if (page < 1 || (teamStore.joinableTotalPages > 0 && page > teamStore.joinableTotalPages)) {
    return
  }
  await loadJoinableTeams(page)
}

async function changePageSize(size: number) {
  if (teamStore.joinableLoading) return
  if (![10, 20, 50].includes(size)) return
  await teamStore.loadJoinableTeams({
    currentPage: 1,
    pageSize: size,
    keyword: appliedKeyword.value.trim() || undefined,
  })
}

function viewTeamDetail(teamId: string) {
  router.push(`/tournament/team/${teamId}/overview`)
}

function openApplyDialog(team: TeamSummaryDto) {
  if (isApplyDisabled(team)) return
  applyingTeam.value = team
  applyReason.value = ''
  showApplyDialog.value = true
}

function closeApplyDialog() {
  if (submitting.value) return
  showApplyDialog.value = false
  applyingTeam.value = null
  applyReason.value = ''
}

async function submitApply() {
  if (!applyingTeam.value || submitting.value) return
  submitting.value = true
  try {
    await teamStore.applyJoinTeamAction(
      applyingTeam.value.teamId,
      applyReason.value.trim() || undefined,
    )
    ElMessage.success(t('accountTeam.joinable.submitted'))
    showApplyDialog.value = false
    applyingTeam.value = null
    applyReason.value = ''
    await Promise.all([
      loadJoinableTeams(teamStore.joinablePage),
      teamStore.loadMyJoinApplications(),
    ])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.joinable.failed'))
  } finally {
    submitting.value = false
  }
}

defineExpose({ loadCurrentPlatform: () => loadJoinableTeams(1), loadJoinableTeams })
</script>

<template>
  <AccountPanel :title="t('accountTeam.joinable.title')">
    <p class="joinable-hint">{{ t('accountTeam.joinable.hint') }}</p>

    <section class="joinable-toolbar" :aria-label="t('accountTeam.joinable.searchAria')">
      <input
        v-model="searchKeyword"
        type="search"
        class="joinable-toolbar__input"
        :placeholder="t('accountTeam.joinable.searchPlaceholder')"
        @keyup.enter="searchJoinable"
      >
      <button
        type="button"
        class="account-btn-primary joinable-toolbar__btn"
        :disabled="teamStore.joinableLoading"
        @click="searchJoinable"
      >
        {{ t('accountTeam.common.search') }}
      </button>
      <button
        type="button"
        class="account-btn-secondary joinable-toolbar__btn"
        :disabled="teamStore.joinableLoading"
        @click="resetSearch"
      >
        {{ t('accountTeam.common.reset') }}
      </button>
    </section>

    <section class="joinable-list" :aria-label="t('accountTeam.joinable.listAria')">
      <div v-if="teamStore.joinableLoading" class="joinable-list__loading">
        {{ t('accountTeam.joinable.loading') }}
      </div>

      <template v-else-if="teamStore.joinableTeams.length">
        <div class="team-table-wrap">
          <table class="team-table joinable-table">
            <thead>
              <tr>
                <th>{{ t('accountTeam.joinable.teamName') }}</th>
                <th>{{ t('accountTeam.joinable.event') }}</th>
                <th>{{ t('accountTeam.joinable.shortName') }}</th>
                <th>{{ t('accountTeam.joinable.captain') }}</th>
                <th>{{ t('accountTeam.joinable.memberCount') }}</th>
                <th>{{ t('accountTeam.joinable.status') }}</th>
                <th class="joinable-table__actions-col">{{ t('accountTeam.joinable.actions') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="team in teamStore.joinableTeams" :key="team.teamId">
                <td class="joinable-table__name">
                  <div class="joinable-table__team">
                    <img
                      v-if="team.logoUrl"
                      :src="team.logoUrl"
                      :alt="team.teamName"
                      class="joinable-table__logo"
                    >
                    <span
                      v-else
                      class="joinable-table__logo joinable-table__logo--placeholder"
                      aria-hidden="true"
                    >
                      {{ teamLogoInitial(team) }}
                    </span>
                    <span class="joinable-table__team-name">{{ team.teamName }}</span>
                  </div>
                </td>
                <td>{{ formatEventContext(team) }}</td>
                <td>{{ team.shortName || '-' }}</td>
                <td>{{ team.captainName || '-' }}</td>
                <td>{{ memberCountText(team) }}</td>
                <td>
                  <AccountStatusBadge
                    :text="resolveTeamStatus(team).text"
                    :type="resolveTeamStatus(team).type"
                  />
                </td>
                <td class="joinable-table__actions">
                  <button
                    type="button"
                    class="account-btn-secondary team-table__btn"
                    @click="viewTeamDetail(team.teamId)"
                  >
                    {{ t('accountTeam.joinable.viewDetail') }}
                  </button>
                  <button
                    type="button"
                    class="account-btn-primary team-table__btn"
                    :disabled="isApplyDisabled(team)"
                    :title="applyTooltip(team)"
                    @click="openApplyDialog(team)"
                  >
                    {{ resolveApplyButtonText(team) }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <MyEventsPagination
          :page="teamStore.joinablePage"
          :page-size="teamStore.joinablePageSize"
          :total="teamStore.joinableTotal"
          :page-size-options="pageSizeOptions"
          :meta-text="paginationMeta"
          :disabled="teamStore.joinableLoading"
          :aria-label="t('accountTeam.joinable.aria')"
          @update:page="changePage"
          @update:page-size="changePageSize"
        />
      </template>

      <AccountEmptyState
        v-else
        :message="t('accountTeam.joinable.empty')"
      />
    </section>

    <Teleport to="body">
      <div v-if="showApplyDialog && applyingTeam" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" :aria-label="t('accountTeam.joinable.close')" @click="closeApplyDialog" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true" aria-labelledby="join-apply-title">
          <h3 id="join-apply-title">{{ t('accountTeam.joinable.dialogTitle') }}</h3>
          <div v-if="applyingTeam" class="joinable-apply-header">
            <img
              v-if="applyingTeam.logoUrl"
              :src="applyingTeam.logoUrl"
              :alt="applyingTeam.teamName"
              class="joinable-apply-header__logo"
            >
            <span
              v-else
              class="joinable-apply-header__logo joinable-apply-header__logo--placeholder"
              aria-hidden="true"
            >
              {{ teamLogoInitial(applyingTeam) }}
            </span>
            <div>
              <p class="joinable-apply-header__name">{{ applyingTeam.teamName }}</p>
              <p class="joinable-apply-header__meta">{{ t('accountTeam.joinable.currentMembers', { count: memberCountText(applyingTeam) }) }}</p>
            </div>
          </div>
          <label class="joinable-apply-field">
            <span class="joinable-apply-field__label">{{ t('accountTeam.joinable.reason') }}</span>
            <textarea
              v-model="applyReason"
              rows="4"
              maxlength="200"
              :placeholder="t('accountTeam.joinable.reasonPlaceholder')"
            />
          </label>
          <p class="joinable-apply-tip">{{ t('accountTeam.joinable.reasonTip') }}</p>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" :disabled="submitting" @click="closeApplyDialog">
              {{ t('accountTeam.common.cancel') }}
            </button>
            <button type="button" class="account-btn-primary" :disabled="submitting" @click="submitApply">
              {{ submitting ? t('accountTeam.joinable.submitting') : t('accountTeam.joinable.submit') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </AccountPanel>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.joinable-hint {
  margin: 0 0 1rem;
  color: var(--account-text-muted, #696969);
  font-size: 14px;
}

.joinable-hint--warn {
  color: #b45309;
}

.joinable-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(20, 20, 20, 0.1);
}

.joinable-toolbar__input {
  flex: 1 1 240px;
  min-width: 0;
  min-height: 44px;
  padding: 0 14px;
  border: 1px solid rgba(20, 20, 20, 0.18);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 14px;
}

.joinable-toolbar__btn {
  min-height: 44px;
  min-width: 96px;
}

.joinable-list {
  min-height: 160px;
}

.joinable-list__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  color: #696969;
  font-size: 14px;
}

.joinable-table__name {
  min-width: 180px;
}

.joinable-table__team {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.joinable-table__logo {
  width: 40px;
  height: 40px;
  border-radius: 3px;
  object-fit: cover;
  border: 1px solid rgba(20, 20, 20, 0.12);
  flex-shrink: 0;
}

.joinable-table__logo--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.45);
  color: #696969;
  font-size: 16px;
  font-weight: 800;
}

.joinable-table__team-name {
  font-weight: 700;
  line-height: 1.4;
}

.joinable-table__actions-col {
  min-width: 200px;
}

.joinable-table__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.joinable-table tbody td {
  vertical-align: middle;
}

.joinable-apply-field {
  display: block;
  margin-bottom: 8px;
}

.joinable-apply-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 12px 0 4px;
  padding: 14px;
  border: 1px solid rgba(20, 20, 20, 0.12);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.35);
}

.joinable-apply-header__logo {
  width: 56px;
  height: 56px;
  border-radius: 3px;
  object-fit: cover;
  border: 1px solid rgba(20, 20, 20, 0.12);
  flex-shrink: 0;
}

.joinable-apply-header__logo--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  color: #696969;
  font-size: 22px;
  font-weight: 800;
}

.joinable-apply-header__name {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
}

.joinable-apply-header__meta {
  margin: 4px 0 0;
  font-size: 13px;
  color: #696969;
}

.joinable-apply-field__label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 700;
}

.joinable-apply-field textarea {
  width: 100%;
  min-height: 112px;
  padding: 12px 14px;
  border: 1px solid rgba(20, 20, 20, 0.18);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
}

.joinable-apply-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #696969;
}

.team-dialog {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
}

.team-dialog__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.team-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(520px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 8px;
    font-size: 18px;
    font-weight: 800;
  }
}

.team-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 767px) {
  .joinable-table__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
