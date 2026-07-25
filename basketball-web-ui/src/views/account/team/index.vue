<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useTeamStore } from '@/stores/team'
import { useAccountStore } from '@/stores/account'
import { mapPlatformToGameTerminal, mapTeamRegistrationStatusText } from '@/utils/accountMappers'
import { formatDateTime } from '@/utils/formatDate'
import { uploadAccountFile } from '@/api/accountFile'
import type { TeamInvitationItemDto } from '@/api/types/teamInvitation'
import type { TeamMemberItemDto } from '@/api/types/teamMember'
import type { TeamRegistrationPlayerItemDto } from '@/api/types/teamRegistration'

type TeamTab =
  | 'discover'
  | 'create'
  | 'overview'
  | 'profile'
  | 'members'
  | 'invitations'
  | 'applications'
  | 'registration'

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()
const accountStore = useAccountStore()

const activeTab = ref<TeamTab>('discover')
const pageLoading = ref(true)
const loadError = ref('')

const joinKeyword = ref('')
const applyReason = ref('')
const inviteKeyword = ref('')
const inviteMessage = ref('')
const selectedInvitePlayerId = ref('')

const createForm = reactive({
  teamName: '',
  shortName: '',
  platform: 'PC',
  description: '',
  publicStatus: 1,
  logoFileId: '',
  logoPreviewUrl: '',
})

const profileForm = reactive({
  teamName: '',
  shortName: '',
  platform: 'PC',
  description: '',
  publicStatus: 1,
})

const memberEdits = reactive<Record<string, { jerseyNo: string; rosterType: string; remark: string }>>({})
const registrationDraftForm = reactive({
  seasonId: '',
  contactName: '',
  contactMobile: '',
  contactEmail: '',
  remark: '',
})
const registrationEditForm = reactive({
  contactName: '',
  contactMobile: '',
  contactEmail: '',
  remark: '',
})
const withdrawReason = ref('')
const selectedRegistrationId = ref('')
const rosterSelections = ref<TeamRegistrationPlayerItemDto[]>([])

const showInvitationDialog = ref(false)
const activeInvitation = ref<TeamInvitationItemDto | null>(null)

const hasTeam = computed(() => teamStore.hasTeam)
const isCaptain = computed(() => teamStore.isCaptain)
const canCreateTeam = computed(() => teamStore.canCreateTeam)
const captainQual = computed(() => teamStore.captainQualification)
const hasCaptainQualRecord = computed(() => Boolean(captainQual.value?.hasQualification))
const isCaptainQualExpired = computed(() => {
  const reason = captainQual.value?.invalidReason || ''
  return hasCaptainQualRecord.value && reason.includes('过期')
})
const teamId = computed(() => teamStore.myTeamSummary?.teamId || '')
const teamDetail = computed(() => teamStore.teamDetail)

const availableTabs = computed<TeamTab[]>(() => {
  if (!hasTeam.value && canCreateTeam.value) return ['create', 'discover']
  if (!hasTeam.value) return ['discover']
  if (isCaptain.value) {
    return ['overview', 'profile', 'members', 'invitations', 'applications', 'registration']
  }
  return ['overview', 'members']
})

const bannerTitle = computed(() => {
  if (hasTeam.value) return teamStore.myTeamSummary?.teamName || '我的球队'
  if (canCreateTeam.value) return '我的球队'
  return '加入球队'
})

const bannerDescription = computed(() => {
  if (hasTeam.value && isCaptain.value) return '管理球队资料、成员、邀请与赛事报名'
  if (hasTeam.value) return '查看球队信息与成员列表'
  if (canCreateTeam.value) return '你已拥有队长资格，可以创建并管理自己的球队；也可以加入其他球队'
  if (isCaptainQualExpired.value) return '队长资格已过期，请联系运营重新确认队长资格；你仍可申请加入其他球队'
  return '浏览可加入球队、处理邀请或提交加入申请'
})

const bannerStatusText = computed(() => {
  if (hasTeam.value && isCaptain.value) return '队长'
  if (hasTeam.value) return '队员'
  if (canCreateTeam.value) return '队长资格有效'
  if (isCaptainQualExpired.value) return '队长资格已过期'
  if (hasCaptainQualRecord.value) return captainQual.value?.invalidReason || '队长资格无效'
  return '暂无队长资格'
})

const bannerStatusType = computed(() => {
  if (hasTeam.value) return 'success'
  if (canCreateTeam.value) return 'success'
  if (isCaptainQualExpired.value) return 'warning'
  return 'info'
})

watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab === 'string' && availableTabs.value.includes(tab as TeamTab)) {
      activeTab.value = tab as TeamTab
    }
  },
  { immediate: true },
)

watch(availableTabs, (tabs) => {
  if (!tabs.includes(activeTab.value)) {
    activeTab.value = tabs[0]
  }
})

watch(teamDetail, (detail) => {
  if (!detail) return
  profileForm.teamName = detail.teamName || ''
  profileForm.shortName = detail.shortName || ''
  profileForm.platform = detail.gameTerminal || 'PC'
  profileForm.description = detail.description || ''
  profileForm.publicStatus = detail.publicStatus ?? 1
}, { immediate: true })

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    await teamStore.bootstrap()
    if (!hasTeam.value) {
      await Promise.all([
        teamStore.loadMyInvitations({ status: 0 }),
        teamStore.loadJoinableTeams(),
        teamStore.loadMyJoinApplications(),
      ])
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    pageLoading.value = false
  }
}

onMounted(bootstrap)

function setTab(tab: TeamTab) {
  activeTab.value = tab
  router.replace({ query: tab === availableTabs.value[0] ? {} : { tab } })
}

async function refreshTabData(tab: TeamTab) {
  if (tab === 'discover') {
    await Promise.all([
      teamStore.loadMyInvitations({ status: 0 }),
      teamStore.loadJoinableTeams({ keyword: joinKeyword.value || undefined }),
      teamStore.loadMyJoinApplications(),
    ])
    return
  }
  if (!teamId.value) return
  if (tab === 'overview' || tab === 'profile') {
    await teamStore.loadTeamDetail(teamId.value)
  }
  if (tab === 'members') await teamStore.loadMembers(teamId.value)
  if (tab === 'invitations') {
    await Promise.all([
      teamStore.loadTeamInvitations(teamId.value),
      teamStore.loadCandidatePlayers(teamId.value, { keyword: inviteKeyword.value || undefined }),
    ])
  }
  if (tab === 'applications') await teamStore.loadTeamJoinApplications(teamId.value, { status: 0 })
  if (tab === 'registration') {
    await Promise.all([
      teamStore.loadAvailableSeasons(teamId.value),
      teamStore.loadRegistrations(teamId.value),
    ])
  }
}

watch(activeTab, (tab) => {
  if (tab === 'create') {
    const platform = accountStore.playerIdentity?.primaryPlatform
    if (platform) {
      createForm.platform = platform
    }
  }
  refreshTabData(tab).catch(() => undefined)
})

async function handleLogoUpload(event: Event, mode: 'create' | 'profile') {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const res = await uploadAccountFile(file, 'TEAM_LOGO')
    if (mode === 'create') {
      createForm.logoFileId = res.data.fileId
      createForm.logoPreviewUrl = res.data.accessUrl
    } else if (teamId.value) {
      await teamStore.bindLogoAction(teamId.value, res.data.fileId)
      ElMessage.success('Logo 已更新')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Logo 上传失败')
  }
}

async function submitCreateTeam() {
  if (!createForm.teamName.trim()) {
    ElMessage.warning('请输入球队名称')
    return
  }
  if (!createForm.shortName.trim()) {
    ElMessage.warning('请输入球队简称')
    return
  }
  if (!createForm.platform) {
    ElMessage.warning('请选择比赛终端')
    return
  }
  try {
    await teamStore.createTeamAction({
      teamName: createForm.teamName.trim(),
      shortName: createForm.shortName.trim(),
      gameTerminal: mapPlatformToGameTerminal(createForm.platform),
      description: createForm.description.trim() || undefined,
      publicStatus: createForm.publicStatus,
      logoFileId: createForm.logoFileId || undefined,
    })
    ElMessage.success('球队创建成功')
    setTab('overview')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

async function submitProfileUpdate() {
  if (!teamId.value) return
  try {
    await teamStore.updateTeamAction(teamId.value, {
      teamName: profileForm.teamName.trim(),
      shortName: profileForm.shortName.trim() || undefined,
      gameTerminal: mapPlatformToGameTerminal(profileForm.platform),
      description: profileForm.description.trim() || undefined,
      publicStatus: profileForm.publicStatus,
    })
    ElMessage.success('球队资料已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

function ensureMemberEdit(member: TeamMemberItemDto) {
  if (!memberEdits[member.teamMemberId]) {
    memberEdits[member.teamMemberId] = {
      jerseyNo: member.jerseyNo || '',
      rosterType: member.rosterType || 'OFFICIAL',
      remark: member.remark || '',
    }
  }
  return memberEdits[member.teamMemberId]
}

async function saveMember(member: TeamMemberItemDto) {
  if (!teamId.value) return
  const edit = ensureMemberEdit(member)
  try {
    await teamStore.updateMemberAction(teamId.value, member.teamMemberId, {
      jerseyNo: edit.jerseyNo || undefined,
      rosterType: edit.rosterType || undefined,
      remark: edit.remark || undefined,
    })
    ElMessage.success('成员信息已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

async function removeMember(member: TeamMemberItemDto) {
  if (!teamId.value) return
  try {
    await ElMessageBox.confirm(`确认移除成员 ${member.nickname || member.realName || ''}？`, '移除成员', {
      type: 'warning',
    })
    await teamStore.removeMemberAction(teamId.value, member.teamMemberId)
    ElMessage.success('成员已移除')
  } catch {
    // cancelled or failed
  }
}

async function handleLeaveTeam() {
  if (!teamId.value) return
  try {
    await ElMessageBox.confirm('确认退出当前球队？', '离队确认', { type: 'warning' })
    await teamStore.leaveTeamAction(teamId.value)
    ElMessage.success('已退出球队')
    setTab('discover')
    await bootstrap()
  } catch {
    // cancelled or failed
  }
}

function openInvitation(item: TeamInvitationItemDto) {
  activeInvitation.value = item
  showInvitationDialog.value = true
}

async function respondInvitation(accept: boolean) {
  if (!activeInvitation.value) return
  try {
    await teamStore.respondInvitationAction(activeInvitation.value.invitationId, accept)
    showInvitationDialog.value = false
    ElMessage.success(accept ? '已接受邀请' : '已拒绝邀请')
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function searchJoinable() {
  await teamStore.loadJoinableTeams({ keyword: joinKeyword.value || undefined })
}

async function applyJoin(teamIdValue: string) {
  try {
    await teamStore.applyJoinTeamAction(teamIdValue, applyReason.value.trim() || undefined)
    ElMessage.success('加入申请已提交')
    applyReason.value = ''
    await teamStore.loadMyJoinApplications()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '申请失败')
  }
}

async function cancelApplication(applicationId: string) {
  try {
    await teamStore.cancelJoinApplicationAction(applicationId)
    ElMessage.success('申请已取消')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '取消失败')
  }
}

async function sendInvite() {
  if (!teamId.value || !selectedInvitePlayerId.value) {
    ElMessage.warning('请选择候选球员')
    return
  }
  try {
    await teamStore.sendInvitationAction(teamId.value, {
      inviteePlayerId: selectedInvitePlayerId.value,
      invitationMessage: inviteMessage.value.trim() || undefined,
    })
    ElMessage.success('邀请已发送')
    inviteMessage.value = ''
    selectedInvitePlayerId.value = ''
    await teamStore.loadTeamInvitations(teamId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '邀请失败')
  }
}

async function searchCandidates() {
  if (!teamId.value) return
  await teamStore.loadCandidatePlayers(teamId.value, { keyword: inviteKeyword.value || undefined })
}

async function processApplication(applicationId: string, approve: boolean) {
  try {
    await teamStore.processJoinApplicationAction(applicationId, approve)
    ElMessage.success(approve ? '已通过申请' : '已驳回申请')
    if (teamId.value) await teamStore.loadTeamJoinApplications(teamId.value, { status: 0 })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '处理失败')
  }
}

async function createRegistrationDraft() {
  if (!teamId.value || !registrationDraftForm.seasonId) {
    ElMessage.warning('请选择赛季')
    return
  }
  try {
    const detail = await teamStore.createRegistrationDraftAction(teamId.value, {
      seasonId: registrationDraftForm.seasonId,
      contactName: registrationDraftForm.contactName.trim() || undefined,
      contactMobile: registrationDraftForm.contactMobile.trim() || undefined,
      contactEmail: registrationDraftForm.contactEmail.trim() || undefined,
      remark: registrationDraftForm.remark.trim() || undefined,
    })
    selectedRegistrationId.value = detail.registrationId
    await openRegistrationDetail(detail.registrationId)
    ElMessage.success('报名草稿已创建')
    await teamStore.loadRegistrations(teamId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

async function openRegistrationDetail(registrationId: string) {
  selectedRegistrationId.value = registrationId
  const detail = await teamStore.loadRegistrationDetail(registrationId)
  registrationEditForm.contactName = detail.contactName || ''
  registrationEditForm.contactMobile = ''
  registrationEditForm.contactEmail = ''
  registrationEditForm.remark = detail.remark || ''
  rosterSelections.value = (detail.rosterItems || []).map((item) => ({ ...item }))
  await teamStore.loadRegistrationAudits(registrationId)
  if (!rosterSelections.value.length && teamStore.members.length === 0 && teamId.value) {
    await teamStore.loadMembers(teamId.value)
  }
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

async function saveRegistrationInfo() {
  if (!selectedRegistrationId.value) return
  try {
    await teamStore.updateRegistrationAction(selectedRegistrationId.value, {
      contactName: registrationEditForm.contactName.trim() || undefined,
      contactMobile: registrationEditForm.contactMobile.trim() || undefined,
      contactEmail: registrationEditForm.contactEmail.trim() || undefined,
      remark: registrationEditForm.remark.trim() || undefined,
    })
    ElMessage.success('报名信息已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function saveRegistrationRoster() {
  if (!selectedRegistrationId.value) return
  try {
    await teamStore.saveRegistrationRosterAction(selectedRegistrationId.value, {
      rosterItems: rosterSelections.value.map((item, index) => ({
        teamMemberId: item.teamMemberId,
        jerseyNo: item.jerseyNo,
        isStarting: item.isStarting ?? 0,
        isCaptain: item.isCaptain ?? 0,
        sortNo: item.sortNo ?? index + 1,
      })),
    })
    ElMessage.success('阵容已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function submitRegistration() {
  if (!selectedRegistrationId.value) return
  try {
    await teamStore.submitRegistrationAction(selectedRegistrationId.value)
    ElMessage.success('报名已提交')
    if (teamId.value) await teamStore.loadRegistrations(teamId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  }
}

async function withdrawRegistration() {
  if (!selectedRegistrationId.value) return
  try {
    await teamStore.withdrawRegistrationAction(selectedRegistrationId.value, {
      withdrawalReason: withdrawReason.value.trim() || undefined,
    })
    ElMessage.success('报名已撤回')
    if (teamId.value) await teamStore.loadRegistrations(teamId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  }
}

async function deleteRegistrationDraft() {
  if (!selectedRegistrationId.value) return
  try {
    await teamStore.deleteRegistrationDraftAction(selectedRegistrationId.value)
    selectedRegistrationId.value = ''
    ElMessage.success('草稿已删除')
    if (teamId.value) await teamStore.loadRegistrations(teamId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

const tabLabels: Record<TeamTab, string> = {
  discover: '发现球队',
  create: '创建球队',
  overview: '概览',
  profile: '编辑资料',
  members: '成员',
  invitations: '邀请球员',
  applications: '加入申请',
  registration: '赛事报名',
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="我的球队"
      en-title="MY TEAM"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的球队' },
      ]"
    />

    <AccountStatusBanner
      :title="bannerTitle"
      :description="bannerDescription"
      :status-text="bannerStatusText"
      :status-type="bannerStatusType"
    />

    <p v-if="loadError" class="team-page-error">{{ loadError }}</p>
    <p v-else-if="pageLoading" class="team-page-loading">加载中…</p>

    <template v-else>
      <nav class="team-tabs" aria-label="球队功能">
        <button
          v-for="tab in availableTabs"
          :key="tab"
          type="button"
          class="team-tabs__item"
          :class="{ 'team-tabs__item--active': activeTab === tab }"
          @click="setTab(tab)"
        >
          {{ tabLabels[tab] }}
        </button>
      </nav>

      <template v-if="activeTab === 'discover'">
        <AccountPanel v-if="canCreateTeam" title="创建球队">
          <p class="team-tip">你已拥有队长资格，可以创建并管理自己的球队。</p>
          <button type="button" class="account-btn-primary" @click="setTab('create')">创建球队</button>
        </AccountPanel>

        <AccountPanel v-else-if="isCaptainQualExpired" title="队长资格">
          <AccountEmptyState message="队长资格已过期，请联系运营重新确认队长资格。" />
        </AccountPanel>

        <AccountPanel v-else-if="!hasCaptainQualRecord" title="队长资格">
          <AccountEmptyState message="你尚未获得队长资格，可先申请加入现有球队。" />
        </AccountPanel>

        <AccountPanel v-else-if="captainQual?.invalidReason" title="队长资格">
          <AccountEmptyState :message="captainQual.invalidReason" />
        </AccountPanel>

        <AccountPanel title="待处理邀请">
          <div v-if="teamStore.myInvitations.length" class="team-card-list">
            <article v-for="item in teamStore.myInvitations" :key="item.invitationId" class="team-card">
              <div>
                <p><strong>球队</strong> {{ item.teamName }}</p>
                <p><strong>说明</strong> {{ item.invitationMessage || '邀请加入球队' }}</p>
                <p><strong>过期时间</strong> {{ formatDateTime(item.expireTime) }}</p>
                <AccountStatusBadge :text="item.invitationStatusText || '待响应'" type="warning" />
              </div>
              <button
                v-if="item.canRespond"
                type="button"
                class="account-btn-primary"
                @click="openInvitation(item)"
              >
                处理邀请
              </button>
            </article>
          </div>
          <AccountEmptyState v-else message="暂无待处理邀请" />
        </AccountPanel>

        <AccountPanel title="可加入球队">
          <div class="team-search-row">
            <input v-model="joinKeyword" type="search" placeholder="搜索球队名称" />
            <button type="button" class="account-btn-secondary" @click="searchJoinable">搜索</button>
          </div>
          <div v-if="teamStore.joinableTeams.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>球队</th>
                  <th>地区</th>
                  <th>终端</th>
                  <th>人数</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="team in teamStore.joinableTeams" :key="team.teamId">
                  <td>{{ team.teamName }}</td>
                  <td>{{ team.regionName || '-' }}</td>
                  <td>{{ team.gameTerminal || '-' }}</td>
                  <td>{{ team.currentMemberCount ?? 0 }}/{{ team.maxMemberCount ?? '-' }}</td>
                  <td>
                    <button type="button" class="account-btn-primary team-table__btn" @click="applyJoin(team.teamId)">
                      申请加入
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无可加入的公开球队" />
          <label class="team-inline-field">
            申请说明
            <textarea v-model="applyReason" rows="2" maxlength="200" placeholder="可选，向队长说明加入理由" />
          </label>
        </AccountPanel>

        <AccountPanel title="我的加入申请">
          <div v-if="teamStore.myJoinApplications.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>球队</th>
                  <th>状态</th>
                  <th>申请时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teamStore.myJoinApplications" :key="item.applicationId">
                  <td>{{ item.teamName }}</td>
                  <td>{{ item.applicationStatusText }}</td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td>
                    <button
                      v-if="item.canCancel"
                      type="button"
                      class="account-btn-secondary team-table__btn"
                      @click="cancelApplication(item.applicationId)"
                    >
                      取消
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无加入申请" />
        </AccountPanel>

        <AccountPanel v-if="canCreateTeam" title="加入球队">
          <p class="team-tip">你也可以选择加入其他球队。</p>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'create'">
        <AccountPanel title="创建球队">
          <p class="team-tip">填写球队基础信息并上传 Logo，创建后你将成为队长。</p>
          <div class="team-create-logo">
            <img v-if="createForm.logoPreviewUrl" :src="createForm.logoPreviewUrl" alt="球队 Logo 预览" />
            <label class="account-btn-secondary team-logo-upload">
              上传 Logo
              <input type="file" accept="image/*" hidden @change="handleLogoUpload($event, 'create')" />
            </label>
          </div>
          <div class="team-form-grid">
            <label>球队名称<input v-model="createForm.teamName" type="text" maxlength="32" required /></label>
            <label>球队简称<input v-model="createForm.shortName" type="text" maxlength="16" required /></label>
            <label>
              比赛终端
              <select v-model="createForm.platform" required>
                <option value="PC">PC</option>
                <option value="PS5">PlayStation 5</option>
                <option value="XBOX">Xbox</option>
                <option value="MOBILE">移动端</option>
              </select>
            </label>
            <label>
              招募状态
              <select v-model.number="createForm.publicStatus">
                <option :value="1">公开招募</option>
                <option :value="0">不公开招募</option>
              </select>
            </label>
            <label class="team-form-full">球队简介<textarea v-model="createForm.description" rows="3" maxlength="500" placeholder="可选，用于展示球队介绍" /></label>
          </div>
          <button type="button" class="account-btn-primary" :disabled="teamStore.actionLoading" @click="submitCreateTeam">
            创建球队
          </button>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'overview'">
        <AccountPanel title="球队概览">
          <dl v-if="teamDetail" class="account-info-list">
            <div class="account-field-row"><dt>球队编号</dt><dd>{{ teamDetail.teamNo || '-' }}</dd></div>
            <div class="account-field-row"><dt>球队名称</dt><dd>{{ teamDetail.teamName }}</dd></div>
            <div class="account-field-row"><dt>简称</dt><dd>{{ teamDetail.shortName || '-' }}</dd></div>
            <div class="account-field-row"><dt>地区</dt><dd>{{ teamDetail.regionName || '-' }}</dd></div>
            <div class="account-field-row"><dt>比赛终端</dt><dd>{{ teamDetail.gameTerminal || '-' }}</dd></div>
            <div class="account-field-row"><dt>成员</dt><dd>{{ teamDetail.currentMemberCount ?? 0 }}/{{ teamDetail.maxMemberCount ?? '-' }}</dd></div>
            <div class="account-field-row"><dt>我的角色</dt><dd>{{ teamDetail.myRole || (isCaptain ? '队长' : '队员') }}</dd></div>
            <div class="account-field-row"><dt>创建时间</dt><dd>{{ formatDateTime(teamDetail.createTime) }}</dd></div>
            <div class="account-field-row"><dt>介绍</dt><dd>{{ teamDetail.description || '暂无' }}</dd></div>
          </dl>
          <AccountEmptyState v-else message="球队信息加载中" />
          <div v-if="!isCaptain" class="team-manage-actions">
            <button type="button" class="account-btn-secondary" @click="handleLeaveTeam">退出球队</button>
          </div>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'profile'">
        <AccountPanel title="编辑球队资料">
          <div class="team-create-logo">
            <img v-if="teamDetail?.logoUrl" :src="teamDetail.logoUrl" alt="球队 Logo" />
            <label class="account-btn-secondary team-logo-upload">
              更换 Logo
              <input type="file" accept="image/*" hidden @change="handleLogoUpload($event, 'profile')" />
            </label>
          </div>
          <div class="team-form-grid">
            <label>球队名称<input v-model="profileForm.teamName" type="text" maxlength="32" /></label>
            <label>球队简称<input v-model="profileForm.shortName" type="text" maxlength="16" /></label>
            <label>
              比赛终端
              <select v-model="profileForm.platform">
                <option value="PC">PC</option>
                <option value="PS5">PlayStation 5</option>
                <option value="XBOX">Xbox</option>
                <option value="MOBILE">移动端</option>
              </select>
            </label>
            <label>
              公开状态
              <select v-model.number="profileForm.publicStatus">
                <option :value="1">公开</option>
                <option :value="0">非公开</option>
              </select>
            </label>
            <label class="team-form-full">球队介绍<textarea v-model="profileForm.description" rows="3" maxlength="500" /></label>
          </div>
          <button type="button" class="account-btn-primary" :disabled="teamStore.actionLoading" @click="submitProfileUpdate">
            保存资料
          </button>
          <p class="team-tip">队长转让功能后续开放。</p>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'members'">
        <AccountPanel title="球队成员">
          <div v-if="teamStore.members.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>昵称</th>
                  <th>角色</th>
                  <th>阵容</th>
                  <th>球衣号</th>
                  <th v-if="isCaptain">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in teamStore.members" :key="member.teamMemberId">
                  <td>{{ member.nickname || member.gameNickname || member.realName || '-' }}</td>
                  <td>{{ member.roleType || '-' }}</td>
                  <td>
                    <template v-if="isCaptain">
                      <select v-model="ensureMemberEdit(member).rosterType">
                        <option value="OFFICIAL">正式</option>
                        <option value="TRIAL">试训</option>
                      </select>
                    </template>
                    <template v-else>{{ member.rosterType || '-' }}</template>
                  </td>
                  <td>
                    <input
                      v-if="isCaptain"
                      v-model="ensureMemberEdit(member).jerseyNo"
                      type="text"
                      maxlength="3"
                      class="team-input-inline"
                    />
                    <template v-else>{{ member.jerseyNo || '-' }}</template>
                  </td>
                  <td v-if="isCaptain">
                    <button type="button" class="account-btn-secondary team-table__btn" @click="saveMember(member)">保存</button>
                    <button
                      v-if="member.roleType !== 'CAPTAIN'"
                      type="button"
                      class="account-btn-secondary team-table__btn"
                      @click="removeMember(member)"
                    >
                      移除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无成员" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'invitations'">
        <AccountPanel title="邀请候选球员">
          <div class="team-search-row">
            <input v-model="inviteKeyword" type="search" placeholder="搜索球员昵称/编号" />
            <button type="button" class="account-btn-secondary" @click="searchCandidates">搜索</button>
          </div>
          <div class="team-form-grid">
            <label class="team-form-full">
              选择球员
              <select v-model="selectedInvitePlayerId">
                <option value="">请选择</option>
                <option
                  v-for="player in teamStore.candidatePlayers"
                  :key="player.playerId"
                  :value="player.playerId"
                >
                  {{ player.nickname || player.gameNickname || player.playerNo }} ({{ player.playerNo || player.playerId }})
                </option>
              </select>
            </label>
            <label class="team-form-full">
              邀请说明
              <textarea v-model="inviteMessage" rows="2" maxlength="200" />
            </label>
          </div>
          <button type="button" class="account-btn-primary" :disabled="teamStore.actionLoading" @click="sendInvite">
            发送邀请
          </button>
        </AccountPanel>
        <AccountPanel title="已发送邀请">
          <div v-if="teamStore.teamInvitations.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>被邀请人</th>
                  <th>状态</th>
                  <th>发送时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teamStore.teamInvitations" :key="item.invitationId">
                  <td>{{ item.inviteeNickname || '-' }}</td>
                  <td>{{ item.invitationStatusText }}</td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无邀请记录" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'applications'">
        <AccountPanel title="待处理加入申请">
          <div v-if="teamStore.teamJoinApplications.length" class="team-card-list">
            <article v-for="item in teamStore.teamJoinApplications" :key="item.applicationId" class="team-card">
              <div>
                <p><strong>申请人</strong> {{ item.applicantNickname }}</p>
                <p><strong>说明</strong> {{ item.applyReason || '无' }}</p>
                <p><strong>时间</strong> {{ formatDateTime(item.createTime) }}</p>
              </div>
              <div v-if="item.canProcess" class="team-card-actions">
                <button type="button" class="account-btn-primary" @click="processApplication(item.applicationId, true)">通过</button>
                <button type="button" class="account-btn-secondary" @click="processApplication(item.applicationId, false)">驳回</button>
              </div>
            </article>
          </div>
          <AccountEmptyState v-else message="暂无待处理申请" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'registration'">
        <AccountPanel title="可报名赛季">
          <div v-if="teamStore.availableSeasons.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>赛事</th>
                  <th>赛季</th>
                  <th>报名时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="season in teamStore.availableSeasons" :key="season.seasonId">
                  <td>{{ season.tournamentName }}</td>
                  <td>{{ season.seasonName }}</td>
                  <td>{{ formatDateTime(season.registrationEndTime) }}</td>
                  <td>
                    <button
                      v-if="season.hasRegistration && season.registrationId"
                      type="button"
                      class="account-btn-secondary team-table__btn"
                      @click="openRegistrationDetail(season.registrationId!)"
                    >
                      查看报名
                    </button>
                    <button
                      v-else
                      type="button"
                      class="account-btn-primary team-table__btn"
                      @click="registrationDraftForm.seasonId = season.seasonId; createRegistrationDraft()"
                    >
                      创建草稿
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="当前没有开放报名的赛季" />
        </AccountPanel>

        <AccountPanel title="报名记录">
          <div v-if="teamStore.registrations.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>赛事</th>
                  <th>赛季</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teamStore.registrations" :key="item.registrationId">
                  <td>{{ item.tournamentName }}</td>
                  <td>{{ item.seasonName }}</td>
                  <td>{{ item.registrationStatusText || mapTeamRegistrationStatusText(item.registrationStatus) }}</td>
                  <td>
                    <button type="button" class="account-btn-secondary team-table__btn" @click="openRegistrationDetail(item.registrationId)">
                      管理
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无报名记录" />
        </AccountPanel>

        <AccountPanel v-if="teamStore.registrationDetail" title="报名详情">
          <dl class="account-info-list">
            <div class="account-field-row"><dt>报名编号</dt><dd>{{ teamStore.registrationDetail.registrationNo }}</dd></div>
            <div class="account-field-row"><dt>状态</dt><dd>{{ teamStore.registrationDetail.registrationStatusText }}</dd></div>
            <div class="account-field-row"><dt>截止时间</dt><dd>{{ formatDateTime(teamStore.registrationDetail.registrationEndTime) }}</dd></div>
          </dl>
          <div class="team-form-grid">
            <label>联系人<input v-model="registrationEditForm.contactName" type="text" :disabled="!teamStore.registrationDetail.canEdit" /></label>
            <label>联系电话<input v-model="registrationEditForm.contactMobile" type="text" :disabled="!teamStore.registrationDetail.canEdit" placeholder="留空则保持不变" /></label>
            <label>联系邮箱<input v-model="registrationEditForm.contactEmail" type="email" :disabled="!teamStore.registrationDetail.canEdit" placeholder="留空则保持不变" /></label>
            <label class="team-form-full">备注<textarea v-model="registrationEditForm.remark" rows="2" :disabled="!teamStore.registrationDetail.canEdit" /></label>
          </div>
          <div class="team-manage-actions">
            <button v-if="teamStore.registrationDetail.canEdit" type="button" class="account-btn-secondary" @click="saveRegistrationInfo">保存信息</button>
            <button v-if="teamStore.registrationDetail.canSubmit" type="button" class="account-btn-primary" @click="submitRegistration">提交报名</button>
            <button v-if="teamStore.registrationDetail.canWithdraw" type="button" class="account-btn-secondary" @click="withdrawRegistration">撤回报名</button>
            <button v-if="teamStore.registrationDetail.canDeleteDraft" type="button" class="account-btn-secondary" @click="deleteRegistrationDraft">删除草稿</button>
          </div>
          <label v-if="teamStore.registrationDetail.canWithdraw" class="team-inline-field">
            撤回原因
            <input v-model="withdrawReason" type="text" maxlength="200" />
          </label>

          <h4 class="team-subtitle">报名阵容</h4>
          <div v-if="rosterSelections.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>球员</th>
                  <th>球衣号</th>
                  <th>首发</th>
                  <th>场上队长</th>
                  <th>排序</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in rosterSelections" :key="item.teamMemberId">
                  <td>{{ item.realName || item.gameNickname || item.teamMemberId }}</td>
                  <td><input v-model="item.jerseyNo" type="text" maxlength="3" class="team-input-inline" :disabled="!teamStore.registrationDetail.canEdit" /></td>
                  <td><input v-model.number="item.isStarting" type="checkbox" :true-value="1" :false-value="0" :disabled="!teamStore.registrationDetail.canEdit" /></td>
                  <td><input v-model.number="item.isCaptain" type="checkbox" :true-value="1" :false-value="0" :disabled="!teamStore.registrationDetail.canEdit" /></td>
                  <td><input v-model.number="item.sortNo" type="number" min="1" class="team-input-inline" :disabled="!teamStore.registrationDetail.canEdit" /></td>
                </tr>
              </tbody>
            </table>
          </div>
          <button v-if="teamStore.registrationDetail.canEdit" type="button" class="account-btn-primary" @click="saveRegistrationRoster">保存阵容</button>

          <h4 class="team-subtitle">审核记录</h4>
          <div v-if="teamStore.registrationAudits.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>动作</th>
                  <th>审核人</th>
                  <th>说明</th>
                  <th>时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="audit in teamStore.registrationAudits" :key="audit.auditId">
                  <td>{{ audit.auditAction }}</td>
                  <td>{{ audit.auditorName || '-' }}</td>
                  <td>{{ audit.auditRemark || '-' }}</td>
                  <td>{{ formatDateTime(audit.auditTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无审核记录" />
        </AccountPanel>
      </template>
    </template>

    <Teleport to="body">
      <div v-if="showInvitationDialog && activeInvitation" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="showInvitationDialog = false" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true">
          <h3>处理球队邀请</h3>
          <dl class="account-info-list">
            <div class="account-field-row"><dt>球队</dt><dd>{{ activeInvitation.teamName }}</dd></div>
            <div class="account-field-row"><dt>说明</dt><dd>{{ activeInvitation.invitationMessage || '邀请加入球队' }}</dd></div>
            <div class="account-field-row"><dt>过期时间</dt><dd>{{ formatDateTime(activeInvitation.expireTime) }}</dd></div>
          </dl>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" @click="respondInvitation(false)">拒绝</button>
            <button type="button" class="account-btn-primary" @click="respondInvitation(true)">接受</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view {
  width: 100%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.team-page-error {
  margin: 0;
  color: #b42318;
  font-size: 13px;
}

.team-page-loading {
  margin: 0;
  color: #696969;
  font-size: 13px;
}

.team-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.team-tabs__item {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: #fff;
  font: inherit;
  cursor: pointer;
}

.team-tabs__item--active {
  border-color: var(--cnpa-red);
  color: var(--cnpa-red);
  font-weight: 700;
}

.team-table-wrap {
  overflow-x: auto;
}

.team-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th,
  td {
    padding: 12px 10px;
    border-bottom: 1px solid rgba(20, 20, 20, 0.1);
    text-align: left;
  }

  th {
    font-size: 12px;
    color: #696969;
  }
}

.team-table__btn {
  min-height: 32px;
  padding-inline: 12px;
}

.team-card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;

  p {
    margin: 0 0 6px;
    font-size: 13px;
  }
}

.team-card-actions {
  display: flex;
  gap: 8px;
}

.team-manage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.team-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #696969;
  line-height: 1.6;
}

.team-search-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;

  input {
    flex: 1;
    min-width: 0;
    padding: 10px 12px;
    border: 1px solid var(--account-border);
    border-radius: 3px;
  }
}

.team-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;

  label {
    display: block;
    font-size: 13px;
    font-weight: 700;
  }

  input,
  select,
  textarea {
    width: 100%;
    margin-top: 6px;
    padding: 10px 12px;
    border: 1px solid var(--account-border);
    border-radius: 3px;
    font-family: inherit;
    box-sizing: border-box;
  }
}

.team-form-full {
  grid-column: 1 / -1;
}

.team-inline-field {
  display: block;
  margin-top: 12px;
  font-size: 13px;
  font-weight: 700;

  textarea,
  input {
    width: 100%;
    margin-top: 6px;
    padding: 10px 12px;
    border: 1px solid var(--account-border);
    border-radius: 3px;
    box-sizing: border-box;
    font-family: inherit;
  }
}

.team-create-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;

  img {
    width: 72px;
    height: 72px;
    object-fit: cover;
    border: 1px solid var(--account-border);
    border-radius: 3px;
  }
}

.team-logo-upload {
  cursor: pointer;
}

.team-input-inline {
  width: 72px;
  padding: 6px 8px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
}

.team-subtitle {
  margin: 18px 0 10px;
  font-size: 14px;
  font-weight: 800;
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
  width: min(480px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 8px;
  }
}

.team-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .team-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
