<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useTeamStore } from '@/stores/team'
import type { TeamSummaryDto } from '@/api/types/teamAccount'

const teamStore = useTeamStore()
const router = useRouter()

const searchKeyword = ref('')
const appliedKeyword = ref('')
const showApplyDialog = ref(false)
const applyingTeam = ref<TeamSummaryDto | null>(null)
const applyReason = ref('')
const submitting = ref(false)

const showPagination = computed(() => teamStore.joinableTotal > teamStore.joinablePageSize)

function memberCountText(team: TeamSummaryDto) {
  const current = team.memberCount ?? team.currentMemberCount ?? 0
  return `${current} 人`
}

function teamLogoInitial(team: TeamSummaryDto) {
  return (team.shortName || team.teamName || '?').slice(0, 1)
}

function formatEventContext(team: TeamSummaryDto) {
  const parts = [
    team.seriesName,
    team.seasonName,
    team.divisionName,
    team.divisionPlatformCode || team.platformCode,
    team.gameMode,
  ].filter(Boolean)
  return parts.join(' · ') || '赛事信息暂无'
}

function resolveTeamStatus(team: TeamSummaryDto): { text: string; type: 'success' | 'warning' | 'danger' | 'default' } {
  if (team.hasPendingApply || team.disabledReason === '申请审核中') {
    return { text: '已申请', type: 'warning' }
  }
  if (team.disabledReason === '球队已满') {
    return { text: '已满', type: 'danger' }
  }
  if (team.canApply !== false) {
    return { text: '可申请', type: 'success' }
  }
  return { text: '不可申请', type: 'default' }
}

function resolveApplyButtonText(team: TeamSummaryDto) {
  if (team.hasPendingApply || team.disabledReason === '申请审核中') {
    return '申请中'
  }
  if (team.disabledReason === '球队已满') {
    return '球队已满'
  }
  if (team.canApply === false) {
    return '不可申请'
  }
  return '申请加入'
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
  appliedKeyword.value = searchKeyword.value.trim()
  await loadJoinableTeams(1)
}

async function resetSearch() {
  searchKeyword.value = ''
  appliedKeyword.value = ''
  await loadJoinableTeams(1)
}

async function changePage(page: number) {
  if (page < 1 || (teamStore.joinableTotalPages > 0 && page > teamStore.joinableTotalPages)) {
    return
  }
  await loadJoinableTeams(page)
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
    ElMessage.success('加入申请已提交')
    showApplyDialog.value = false
    applyingTeam.value = null
    applyReason.value = ''
    await Promise.all([
      loadJoinableTeams(teamStore.joinablePage),
      teamStore.loadMyJoinApplications(),
    ])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '申请失败')
  } finally {
    submitting.value = false
  }
}

defineExpose({ loadCurrentPlatform: () => loadJoinableTeams(1), loadJoinableTeams })
</script>

<template>
  <AccountPanel title="可加入球队">
    <p class="joinable-hint">仅展示当前未结束赛事中开放招募的球队；同一 Division 只能加入一支。</p>

    <section class="joinable-toolbar" aria-label="搜索可加入球队">
      <input
        v-model="searchKeyword"
        type="search"
        class="joinable-toolbar__input"
        placeholder="请输入球队名称 / 简称"
        @keyup.enter="searchJoinable"
      >
      <button
        type="button"
        class="account-btn-primary joinable-toolbar__btn"
        :disabled="teamStore.joinableLoading"
        @click="searchJoinable"
      >
        搜索
      </button>
      <button
        type="button"
        class="account-btn-secondary joinable-toolbar__btn"
        :disabled="teamStore.joinableLoading"
        @click="resetSearch"
      >
        重置
      </button>
    </section>

    <section class="joinable-list" aria-label="可加入球队列表">
      <div v-if="teamStore.joinableLoading" class="joinable-list__loading">
        加载中…
      </div>

      <template v-else-if="teamStore.joinableTeams.length">
        <div class="team-table-wrap">
          <table class="team-table joinable-table">
            <thead>
              <tr>
                <th>球队名称</th>
                <th>所属赛事</th>
                <th>简称</th>
                <th>队长</th>
                <th>当前人数</th>
                <th>状态</th>
                <th class="joinable-table__actions-col">操作</th>
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
                    查看详情
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

        <nav v-if="showPagination" class="joinable-pagination" aria-label="可加入球队分页">
          <span class="joinable-pagination__meta">
            共 {{ teamStore.joinableTotal }} 条，第 {{ teamStore.joinablePage }} / {{ teamStore.joinableTotalPages || 1 }} 页
          </span>
          <div class="joinable-pagination__controls">
            <button
              type="button"
              class="account-btn-secondary"
              :disabled="teamStore.joinablePage <= 1 || teamStore.joinableLoading"
              @click="changePage(teamStore.joinablePage - 1)"
            >
              上一页
            </button>
            <button
              v-for="p in teamStore.joinableTotalPages"
              :key="p"
              type="button"
              class="joinable-pagination__page"
              :class="{ 'joinable-pagination__page--active': p === teamStore.joinablePage }"
              :disabled="teamStore.joinableLoading"
              @click="changePage(p)"
            >
              {{ p }}
            </button>
            <button
              type="button"
              class="account-btn-secondary"
              :disabled="teamStore.joinablePage >= teamStore.joinableTotalPages || teamStore.joinableLoading"
              @click="changePage(teamStore.joinablePage + 1)"
            >
              下一页
            </button>
          </div>
        </nav>
      </template>

      <AccountEmptyState
        v-else
        message="暂无可加入球队"
      />
    </section>

    <Teleport to="body">
      <div v-if="showApplyDialog && applyingTeam" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="closeApplyDialog" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true" aria-labelledby="join-apply-title">
          <h3 id="join-apply-title">申请加入球队</h3>
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
              <p class="joinable-apply-header__meta">当前人数 {{ memberCountText(applyingTeam) }}</p>
            </div>
          </div>
          <label class="joinable-apply-field">
            <span class="joinable-apply-field__label">申请原因</span>
            <textarea
              v-model="applyReason"
              rows="4"
              maxlength="200"
              placeholder="请简单说明加入球队的原因，方便队长审核"
            />
          </label>
          <p class="joinable-apply-tip">请简单说明加入球队的原因，方便队长审核。</p>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" :disabled="submitting" @click="closeApplyDialog">
              取消
            </button>
            <button type="button" class="account-btn-primary" :disabled="submitting" @click="submitApply">
              {{ submitting ? '提交中…' : '提交申请' }}
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
  gap: 8px;
  white-space: nowrap;
}

.joinable-pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(20, 20, 20, 0.1);
}

.joinable-pagination__meta {
  font-size: 14px;
  color: #696969;
}

.joinable-pagination__controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.joinable-pagination__page {
  min-width: 40px;
  height: 40px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: transparent;
  color: #111;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;

  &--active {
    border-color: var(--account-red, #d90000);
    color: var(--account-red, #d90000);
  }

  &:hover:not(&--active):not(:disabled) {
    border-color: rgba(217, 0, 0, 0.45);
    color: var(--account-red, #d90000);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
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

  .joinable-pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .joinable-pagination__controls {
    justify-content: center;
  }
}
</style>
