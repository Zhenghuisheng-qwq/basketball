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
const selectedSeason = computed(() =>
  teamStore.availableSeasons.find((item) => item.seasonId === draftForm.seasonId),
)
const selectedSeasonRosterLimits = computed(() =>
  resolveSeasonRosterLimits(selectedSeason.value?.minRosterCount, selectedSeason.value?.maxRosterCount),
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
  return { text: '草稿', type: 'info' as const }
})

function pickDefaultSeason(seasons: AvailableSeasonItemDto[]) {
  if (!seasons.length) return
  const matched = seasons.find(
    (item) =>
      item.seasonId === tournamentId.value
      || item.tournamentId === tournamentId.value,
  )
  draftForm.seasonId = matched?.seasonId || seasons[0].seasonId
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
    await Promise.all([
      teamStore.loadAvailableSeasons(teamId.value),
      teamStore.loadMembers(teamId.value),
      teamStore.loadRegistrations(teamId.value),
    ])
    pickDefaultSeason(teamStore.availableSeasons)

    const existing = teamStore.availableSeasons.find(
      (item) => item.hasRegistration && item.registrationId,
    )
    if (existing?.registrationId) {
      registrationId.value = existing.registrationId
      await loadDetail(existing.registrationId)
      currentStep.value = detail.value?.canSubmit ? 'submit' : 'info'
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
  if (!rosterSelections.value.length) {
    rosterSelections.value = teamStore.members.map((member, index) => ({
      teamMemberId: member.teamMemberId,
      realName: member.realName,
      gameNickname: member.gameNickname,
      jerseyNo: member.jerseyNo,
      isStarting: index < 5 ? 1 : 0,
      isCaptain: member.roleType === 'CAPTAIN' ? 1 : 0,
      sortNo: index + 1,
    }))
  }
}

async function createDraft() {
  if (!teamId.value || !draftForm.seasonId) {
    ElMessage.warning('请选择赛季')
    return
  }
  try {
    const created = await teamStore.createRegistrationDraftAction(teamId.value, {
      seasonId: draftForm.seasonId,
      contactName: draftForm.contactName.trim() || undefined,
      contactMobile: draftForm.contactMobile.trim() || undefined,
      contactEmail: draftForm.contactEmail.trim() || undefined,
      remark: draftForm.remark.trim() || undefined,
    })
    await loadDetail(created.registrationId)
    currentStep.value = 'info'
    ElMessage.success('报名草稿已创建')
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
  if (!rosterSelections.value.length) {
    ElMessage.warning('请至少选择一名球员')
    return
  }
  try {
    await teamStore.saveRegistrationRosterAction(registrationId.value, {
      rosterItems: rosterSelections.value.map((item, index) => ({
        teamMemberId: item.teamMemberId,
        jerseyNo: item.jerseyNo,
        isStarting: item.isStarting ?? 0,
        isCaptain: item.isCaptain ?? 0,
        sortNo: item.sortNo ?? index + 1,
      })),
    })
    ElMessage.success('阵容已保存')
    currentStep.value = 'submit'
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function submitRegistration() {
  if (!registrationId.value) return
  try {
    await teamStore.submitRegistrationAction(registrationId.value)
    ElMessage.success('报名已提交，正在等待后台审核')
    router.push('/account/my-events?tab=applications')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

function toggleMember(teamMemberId: string) {
  const exists = rosterSelections.value.find((item) => item.teamMemberId === teamMemberId)
  if (exists) {
    rosterSelections.value = rosterSelections.value.filter((item) => item.teamMemberId !== teamMemberId)
    return
  }
  const member = teamStore.members.find((item) => item.teamMemberId === teamMemberId)
  if (!member) return
  rosterSelections.value.push({
    teamMemberId: member.teamMemberId,
    realName: member.realName,
    gameNickname: member.gameNickname,
    jerseyNo: member.jerseyNo,
    isStarting: rosterSelections.value.length < 5 ? 1 : 0,
    isCaptain: member.roleType === 'CAPTAIN' ? 1 : 0,
    sortNo: rosterSelections.value.length + 1,
  })
}

function isMemberSelected(teamMemberId: string) {
  return rosterSelections.value.some((item) => item.teamMemberId === teamMemberId)
}

watch(
  () => draftForm.seasonId,
  (seasonId) => {
    const season = teamStore.availableSeasons.find((item) => item.seasonId === seasonId)
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
      description="队长提交球队赛季报名与阵容"
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
            {{ registrationId ? '继续填写' : '创建报名草稿' }}
          </button>
        </div>
      </AccountPanel>

      <AccountPanel v-else-if="currentStep === 'info'" title="报名信息">
        <div class="team-registration-form">
          <label>
            <span>联系人</span>
            <input v-model="editForm.contactName" type="text" class="identity-apply-form__input" />
          </label>
          <label>
            <span>联系电话</span>
            <input v-model="editForm.contactMobile" type="text" class="identity-apply-form__input" placeholder="留空则沿用账号资料" />
          </label>
          <label>
            <span>联系邮箱</span>
            <input v-model="editForm.contactEmail" type="email" class="identity-apply-form__input" placeholder="留空则沿用账号资料" />
          </label>
          <label>
            <span>备注</span>
            <textarea v-model="editForm.remark" rows="3" class="identity-apply-form__input" />
          </label>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="currentStep = 'season'">上一步</button>
          <button type="button" class="account-btn-primary" @click="saveInfo">保存并下一步</button>
        </div>
      </AccountPanel>

      <AccountPanel v-else-if="currentStep === 'roster'" title="选择阵容">
        <p class="team-registration-hint">
          已选 {{ rosterSelections.length }} 人
          · 最少 {{ selectedSeasonRosterLimits.minRosterCount }} 人
          · 最多 {{ selectedSeasonRosterLimits.maxRosterCount }} 人
        </p>
        <div class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th />
                <th>球员</th>
                <th>球衣号</th>
                <th>身份</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in teamStore.members" :key="member.teamMemberId">
                <td>
                  <input
                    type="checkbox"
                    :checked="isMemberSelected(member.teamMemberId)"
                    @change="toggleMember(member.teamMemberId)"
                  />
                </td>
                <td>{{ member.gameNickname || member.realName || member.teamMemberId }}</td>
                <td>{{ member.jerseyNo || '-' }}</td>
                <td>{{ member.roleType === 'CAPTAIN' ? '队长' : '队员' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="currentStep = 'info'">上一步</button>
          <button type="button" class="account-btn-primary" @click="saveRoster">保存并下一步</button>
        </div>
      </AccountPanel>

      <AccountPanel v-else title="提交审核">
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
          <button type="button" class="account-btn-secondary" @click="currentStep = 'roster'">上一步</button>
          <button
            type="button"
            class="account-btn-primary"
            :disabled="!detail?.canSubmit"
            @click="submitRegistration"
          >
            提交报名
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
