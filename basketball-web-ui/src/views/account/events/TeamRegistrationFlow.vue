<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { StepItem } from '@/components/account/AccountStepper.vue'
import { useTeamStore } from '@/stores/team'
import { mapTeamRegistrationStatusText } from '@/utils/accountMappers'
import { resolveSeasonRosterLimits } from '@/constants/rosterRules'
import type { AvailableSeasonItemDto, TeamRegistrationPlayerItemDto } from '@/api/types/teamRegistration'

type FlowStep = 'season' | 'info' | 'roster' | 'submit'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()

const pageLoading = ref(true)
const loadError = ref('')
const currentStep = ref<FlowStep>('season')
const registrationId = ref('')

const draftForm = reactive({
  seasonId: '',
  platformCode: '',
  contactName: '',
  contactMobile: '',
  contactEmail: '',
  remark: '',
})

const editForm = reactive({
  contactName: '',
  contactMobile: '',
  contactEmail: '',
  remark: '',
})

const rosterSelections = ref<TeamRegistrationPlayerItemDto[]>([])

const tournamentId = computed(() => String(route.params.tournamentId || ''))
const teamId = computed(() => teamStore.myTeamSummary?.teamId || '')
const detail = computed(() => teamStore.registrationDetail)
const isRosterLocked = computed(() => Boolean(detail.value?.rosterLocked || detail.value?.registrationStatus === 3))
const rosterLockedHint = computed(() =>
  detail.value?.rosterLockedHint || '报名已审核通过，阵容已锁定。如需调整，请重新提交审核或联系赛事管理员。',
)
const selectedSeason = computed(() =>
  teamStore.availableSeasons.find((item) => item.seasonId === draftForm.seasonId),
)
const selectedSeasonRosterLimits = computed(() =>
  resolveSeasonRosterLimits(
    selectedSeason.value?.minRosterCount,
    selectedSeason.value?.maxRosterCount,
    detail.value?.teamPlayCount,
  ),
)

const stepItems = computed<StepItem[]>(() => {
  const order: FlowStep[] = ['season', 'info', 'roster', 'submit']
  const currentIndex = order.indexOf(currentStep.value)
  return [
    { label: '选择赛事', state: currentIndex > 0 ? 'done' : currentIndex === 0 ? 'current' : 'upcoming' },
    { label: '填写信息', state: currentIndex > 1 ? 'done' : currentIndex === 1 ? 'current' : 'upcoming' },
    { label: '选择阵容', state: currentIndex > 2 ? 'done' : currentIndex === 2 ? 'current' : 'upcoming' },
    { label: '提交审核', state: currentIndex === 3 ? 'current' : 'upcoming' },
  ]
})

const bannerStatus = computed(() => {
  if (pageLoading.value) return { text: '加载中', type: 'info' as const }
  if (loadError.value) return { text: '加载失败', type: 'danger' as const }
  if (detail.value?.registrationStatusText) {
    return { text: detail.value.registrationStatusText, type: 'warning' as const }
  }
  return { text: '待提交', type: 'info' as const }
})

function pickDefaultSeason(seasons: AvailableSeasonItemDto[]) {
  if (!seasons.length) return
  const matched = seasons.find(
    (item) =>
      item.seasonId === tournamentId.value
      || item.tournamentId === tournamentId.value,
  )
  draftForm.seasonId = matched?.seasonId || seasons[0].seasonId
  syncDraftPlatform(matched || seasons[0])
}

function syncDraftPlatform(season?: AvailableSeasonItemDto) {
  if (!season) return
  const queryPlatform = typeof route.query.platformCode === 'string' ? route.query.platformCode : ''
  draftForm.platformCode = queryPlatform || season.platformCode || ''
}

function resolveStepFromDetail(): FlowStep {
  if (!detail.value) return 'season'
  if (detail.value.rosterLocked || detail.value.registrationStatus === 3) return 'submit'
  if (detail.value.canSubmit) return 'submit'
  if ((detail.value.rosterItems || []).length) return 'roster'
  if (detail.value.contactName || detail.value.remark) return 'info'
  return 'info'
}

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    await teamStore.bootstrap()
    if (!teamStore.hasTeam) {
      loadError.value = '请先创建或加入球队'
      return
    }
    if (!teamStore.isCaptain) {
      loadError.value = '仅队长可发起球队报名'
      return
    }
    if (!teamId.value) return

    const queryRegistrationId = typeof route.query.registrationId === 'string'
      ? route.query.registrationId
      : ''
    if (queryRegistrationId) {
      await loadDetail(queryRegistrationId)
      currentStep.value = resolveStepFromDetail()
      return
    }

    await Promise.all([
      teamStore.loadRegistrations(teamId.value),
      teamStore.loadMembers(teamId.value),
    ])

    const seasonKey = tournamentId.value
    const existingFromRegistrations = teamStore.registrations.find(
      (item) => item.seasonId === seasonKey,
    )
    if (existingFromRegistrations?.registrationId) {
      await loadDetail(existingFromRegistrations.registrationId)
      currentStep.value = resolveStepFromDetail()
      return
    }

    await teamStore.loadAvailableSeasons(teamId.value)
    pickDefaultSeason(teamStore.availableSeasons)
    const queryPlatform = typeof route.query.platformCode === 'string' ? route.query.platformCode : ''
    if (queryPlatform) {
      draftForm.platformCode = queryPlatform
    } else if (!draftForm.platformCode && selectedSeason.value?.platformCode) {
      draftForm.platformCode = selectedSeason.value.platformCode
    }

    const existing = teamStore.availableSeasons.find(
      (item) =>
        (item.seasonId === seasonKey || item.tournamentId === seasonKey)
        && item.hasRegistration
        && item.registrationId,
    )
    if (existing?.registrationId) {
      await loadDetail(existing.registrationId)
      currentStep.value = resolveStepFromDetail()
      return
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    pageLoading.value = false
  }
}

async function loadDetail(id: string) {
  registrationId.value = id
  const loaded = await teamStore.loadRegistrationDetail(id)
  editForm.contactName = loaded.contactName || ''
  editForm.contactMobile = ''
  editForm.contactEmail = ''
  editForm.remark = loaded.remark || ''
  rosterSelections.value = (loaded.rosterItems || []).map((item) => ({ ...item }))
  await teamStore.loadRosterCandidates(id)
  await syncRegistrationRosterFromCandidates()
}

async function syncRegistrationRosterFromCandidates() {
  const currentDetail = teamStore.registrationDetail
  if (!registrationId.value || !currentDetail?.canEdit || currentDetail.rosterLocked) return

  const max = selectedSeasonRosterLimits.value.maxRosterCount
  const eligible = teamStore.rosterCandidates.filter((candidate) => candidate.canSelect).slice(0, max)
  const next = eligible.map((candidate) => ({
    teamMemberId: candidate.teamMemberId,
    gameNickname: candidate.gameNickname || candidate.nickname,
    positionCode: candidate.positionCode,
  }))

  const currentIds = new Set(rosterSelections.value.map((item) => item.teamMemberId))
  const nextIds = new Set(next.map((item) => item.teamMemberId))
  const sameSet = currentIds.size === nextIds.size && [...currentIds].every((id) => nextIds.has(id))
  const needsPersist = !sameSet || (next.length > 0 && (currentDetail.lineupCount ?? 0) === 0)
  if (!needsPersist) return

  rosterSelections.value = next
  if (!next.length) return

  const saved = await teamStore.saveRegistrationRosterAction(registrationId.value, {
    rosterItems: next.map((item) => ({ teamMemberId: item.teamMemberId })),
  })
  rosterSelections.value = (saved.rosterItems || []).map((item) => ({ ...item }))
}

async function createDraft() {
  if (!teamId.value || !draftForm.seasonId) {
    ElMessage.warning('请选择赛季')
    return
  }
  try {
    const created = await teamStore.createRegistrationDraftAction(teamId.value, {
      seasonId: draftForm.seasonId,
      platformCode: draftForm.platformCode || selectedSeason.value?.platformCode,
      contactName: draftForm.contactName.trim() || undefined,
      contactMobile: draftForm.contactMobile.trim() || undefined,
      contactEmail: draftForm.contactEmail.trim() || undefined,
      remark: draftForm.remark.trim() || undefined,
    })
    await loadDetail(created.registrationId)
    currentStep.value = 'info'
    ElMessage.success('报名信息已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

async function saveInfo() {
  if (!registrationId.value) return
  try {
    await teamStore.updateRegistrationAction(registrationId.value, {
      contactName: editForm.contactName.trim() || undefined,
      contactMobile: editForm.contactMobile.trim() || undefined,
      contactEmail: editForm.contactEmail.trim() || undefined,
      remark: editForm.remark.trim() || undefined,
    })
    ElMessage.success('报名信息已保存')
    currentStep.value = 'roster'
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function saveRoster() {
  if (!registrationId.value) return
  try {
    await syncRegistrationRosterFromCandidates()
    if (rosterSelections.value.length < selectedSeasonRosterLimits.value.minRosterCount) {
      ElMessage.warning(`阵容至少需要 ${selectedSeasonRosterLimits.value.minRosterCount} 人`)
      return
    }
    ElMessage.success('阵容已确认')
    currentStep.value = 'submit'
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function submitRegistration() {
  if (!registrationId.value) return
  try {
    await syncRegistrationRosterFromCandidates()
    if (rosterSelections.value.length < selectedSeasonRosterLimits.value.minRosterCount) {
      ElMessage.warning(`阵容至少需要 ${selectedSeasonRosterLimits.value.minRosterCount} 人`)
      return
    }
    await teamStore.submitRegistrationAction(registrationId.value)
    ElMessage.success('报名已提交，正在等待后台审核')
    router.push('/account/my-events?tab=applications')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

function formatCandidateEligibility(candidate: import('@/api/types/teamRegistration').RegistrationRosterCandidateDto) {
  if (candidate.canSelect) return '已纳入报名'
  return candidate.cannotSelectReason || candidate.platformIdentityStatusText || '不可纳入'
}

watch(
  () => draftForm.seasonId,
  (seasonId) => {
    const season = teamStore.availableSeasons.find((item) => item.seasonId === seasonId)
    syncDraftPlatform(season)
    if (season?.hasRegistration && season.registrationId) {
      void loadDetail(season.registrationId)
    }
  },
)

onMounted(bootstrap)
</script>

<template>
  <div class="account-view account-view--team-registration">
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
      :title="selectedSeason?.tournamentName || selectedSeason?.seasonName || '球队赛事报名'"
      :description="isRosterLocked ? rosterLockedHint : '队长提交球队赛季报名与阵容'"
      :status-text="bannerStatus.text"
      :status-type="bannerStatus.type"
    />

    <AccountStepper :steps="stepItems" />

    <p v-if="loadError" class="team-registration-error">{{ loadError }}</p>
    <p v-else-if="pageLoading" class="team-registration-loading">加载中…</p>

    <template v-else>
      <AccountPanel v-if="currentStep === 'season'" title="选择赛季">
        <div v-if="!teamStore.availableSeasons.length" class="team-registration-empty">
          当前暂无可报名赛事
        </div>
        <div v-else class="team-registration-season-list">
          <label
            v-for="season in teamStore.availableSeasons"
            :key="season.seasonId"
            class="team-registration-season"
            :class="{ 'team-registration-season--active': draftForm.seasonId === season.seasonId }"
          >
            <input v-model="draftForm.seasonId" type="radio" :value="season.seasonId" />
            <div>
              <strong>{{ season.tournamentName || season.seasonName }}</strong>
              <p>{{ season.seasonName }}</p>
              <p v-if="season.registrationEndTime">截止 {{ season.registrationEndTime }}</p>
              <AccountStatusBadge
                v-if="season.hasRegistration"
                type="warning"
                text="已有报名"
              />
            </div>
          </label>
        </div>
        <div class="account-panel__action">
          <button
            type="button"
            class="account-btn-primary"
            :disabled="!draftForm.seasonId"
            @click="registrationId ? (currentStep = 'info') : createDraft()"
          >
            {{ registrationId ? '继续填写' : '开始报名' }}
          </button>
        </div>
      </AccountPanel>

      <AccountPanel v-else-if="currentStep === 'info'" title="报名信息">
        <p v-if="isRosterLocked" class="team-registration-hint">{{ rosterLockedHint }}</p>
        <div class="team-registration-form">
          <label>
            <span>联系人</span>
            <input v-model="editForm.contactName" type="text" class="identity-apply-form__input" :disabled="isRosterLocked" />
          </label>
          <label>
            <span>联系电话</span>
            <input v-model="editForm.contactMobile" type="text" class="identity-apply-form__input" placeholder="留空则沿用账号资料" :disabled="isRosterLocked" />
          </label>
          <label>
            <span>联系邮箱</span>
            <input v-model="editForm.contactEmail" type="email" class="identity-apply-form__input" placeholder="留空则沿用账号资料" :disabled="isRosterLocked" />
          </label>
          <label>
            <span>备注</span>
            <textarea v-model="editForm.remark" rows="3" class="identity-apply-form__input" :disabled="isRosterLocked" />
          </label>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="currentStep = 'season'">上一步</button>
          <button v-if="!isRosterLocked" type="button" class="account-btn-primary" @click="saveInfo">保存并下一步</button>
        </div>
      </AccountPanel>

      <AccountPanel v-else-if="currentStep === 'roster'" :title="isRosterLocked ? '查看阵容' : '报名阵容'">
        <p v-if="isRosterLocked" class="team-registration-hint">{{ rosterLockedHint }}</p>
        <p v-else class="team-registration-hint">
          具备当前赛事平台已生效游戏 ID 且已认证的队员将自动纳入本次报名阵容。
          当前报名阵容 {{ rosterSelections.length }} 人
          · 要求 {{ selectedSeasonRosterLimits.minRosterCount }} - {{ selectedSeasonRosterLimits.maxRosterCount }} 人
        </p>
        <div class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>球员</th>
                <th>位置</th>
                <th>资格状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="candidate in teamStore.rosterCandidates" :key="candidate.teamMemberId">
                <td>{{ candidate.gameNickname || candidate.nickname || candidate.playerNo }}</td>
                <td>{{ candidate.positionName || candidate.positionCode || '-' }}</td>
                <td>{{ formatCandidateEligibility(candidate) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="currentStep = 'info'">上一步</button>
          <button v-if="!isRosterLocked" type="button" class="account-btn-primary" @click="saveRoster">下一步</button>
          <button v-else type="button" class="account-btn-primary" @click="currentStep = 'submit'">查看提交信息</button>
        </div>
      </AccountPanel>

      <AccountPanel v-else :title="isRosterLocked ? '报名详情' : '提交审核'">
        <p v-if="isRosterLocked" class="team-registration-hint">{{ rosterLockedHint }}</p>
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">赛季</span>
            <span>{{ detail?.seasonName || selectedSeason?.seasonName }}</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">球队</span>
            <span>{{ detail?.teamName || teamStore.myTeamSummary?.teamName }}</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">阵容人数</span>
            <span>{{ detail?.lineupCount ?? rosterSelections.length }}</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">当前状态</span>
            <span>{{ detail?.registrationStatusText || mapTeamRegistrationStatusText(detail?.registrationStatus) }}</span>
          </div>
        </div>
        <div class="account-panel__action">
          <button v-if="!isRosterLocked" type="button" class="account-btn-secondary" @click="currentStep = 'roster'">上一步</button>
          <button
            v-if="!isRosterLocked"
            type="button"
            class="account-btn-primary"
            :disabled="!detail?.canSubmit"
            @click="submitRegistration"
          >
            提交报名
          </button>
          <button
            v-else
            type="button"
            class="account-btn-secondary"
            disabled
            title="阵容变更审核流程暂未开放，请联系赛事管理员"
          >
            申请调整阵容
          </button>
        </div>
      </AccountPanel>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.team-registration-error {
  color: #b42318;
  font-size: 13px;
}

.team-registration-loading,
.team-registration-empty,
.team-registration-hint {
  margin: 0 0 12px;
  color: #666;
  font-size: 13px;
}

.team-registration-season-list {
  display: grid;
  gap: 12px;
}

.team-registration-season {
  display: flex;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  cursor: pointer;

  &--active {
    border-color: var(--account-red, #d30910);
    background: rgba(211, 9, 16, 0.04);
  }

  strong {
    display: block;
    margin-bottom: 4px;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #666;
  }
}

.team-registration-form {
  display: grid;
  gap: 12px;

  label {
    display: grid;
    gap: 6px;

    span {
      font-size: 13px;
      font-weight: 700;
    }
  }
}
</style>
