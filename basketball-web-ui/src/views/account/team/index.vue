<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import TeamMemberList from '@/components/account/team/TeamMemberList.vue'
import JoinableTeamPanel from '@/components/account/team/JoinableTeamPanel.vue'
import CaptainGrantPanel from '@/components/account/team/CaptainGrantPanel.vue'
import { useTeamStore } from '@/stores/team'
import { mapTeamRegistrationStatusText } from '@/utils/accountMappers'
import {
  formatPositionLabel,
  CNPA_REQUIRED_STARTER_COUNT,
  CNPA_MIN_BENCH_COUNT,
  CNPA_TEAM_MAX_MEMBER_COUNT,
  STARTING_POSITION_CODES,
} from '@/constants/rosterRules'
import { formatDateTime } from '@/utils/formatDate'
import { uploadAccountFile } from '@/api/accountFile'
import type { TeamInvitationItemDto } from '@/api/types/teamInvitation'
import type { CandidatePlayerItemDto, TeamMemberItemDto } from '@/api/types/teamMember'
import type { AvailableSeasonItemDto, TeamRegistrationPlayerItemDto } from '@/api/types/teamRegistration'

type TeamTab =
  | 'pendingInvites'
  | 'joinableTeams'
  | 'myApplications'
  | 'create'
  | 'overview'
  | 'profile'
  | 'members'
  | 'invitations'
  | 'applications'
  | 'registration'

const TEAM_AUDIT_STATUS: Record<number, string> = {
  0: '待审核',
  1: '已通过',
  2: '已驳回',
  3: '审核中',
}

function formatPlayerCertStatus(status?: number) {
  if (status === 3) return '已认证'
  return '未认证'
}

function formatPositionCell(code?: string, name?: string) {
  if (!code) return '未设置'
  return name ? `${code} ${name}` : formatPositionLabel(code)
}

const teamMemberLimitHint = `${CNPA_TEAM_MAX_MEMBER_COUNT} 人（${CNPA_REQUIRED_STARTER_COUNT} 首发 + ${CNPA_MIN_BENCH_COUNT} 替补）`

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()

const activeTab = ref<TeamTab>('joinableTeams')
const pageLoading = ref(true)
const loadError = ref('')

const joinablePanelRef = ref<InstanceType<typeof JoinableTeamPanel> | null>(null)

const inviteKeyword = ref('')
const inviteMessage = ref('')
const selectedInvitePlayerId = ref('')

const createForm = reactive({
  teamName: '',
  shortName: '',
  description: '',
  publicStatus: 1,
  logoFileId: '',
  logoPreviewUrl: '',
})

const profileForm = reactive({
  teamName: '',
  shortName: '',
  description: '',
  publicStatus: 1,
})

const registrationDraftForm = reactive({
  seasonId: '',
  platformCode: '',
  contactName: '',
  contactMobile: '',
  contactEmail: '',
  remark: '',
})
const showPlatformPicker = ref(false)
const pendingSeasonId = ref('')
const selectedPlatformCode = ref('')
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
const canManageCurrentTeam = computed(() => teamStore.canManageCurrentTeam)
const isCaptainMember = computed(() => teamStore.isCaptainMember)
const isHistoricalCaptain = computed(() => teamStore.isHistoricalCaptain)
const isRegularMember = computed(() => teamStore.isRegularMember)
const canCreateTeam = computed(() => teamStore.canCreateTeam)
const canInheritTeam = computed(() => teamStore.canInheritTeam)
const canJoinTeam = computed(() => teamStore.canJoinTeam)
const captainQual = computed(() => teamStore.captainQualification)
const hasCaptainQualRecord = computed(() => Boolean(captainQual.value?.hasQualification))
const isCaptainQualExpired = computed(() => Boolean(captainQual.value?.grantRevoked))
const isCaptainQualInvalid = computed(() => hasCaptainQualRecord.value && !captainQual.value?.runtimeValid && !isCaptainQualExpired.value)
const teamId = computed(() => teamStore.myTeamSummary?.teamId || '')
const teamDetail = computed(() => teamStore.teamDetail)
const isTeamAuditApproved = computed(() => teamDetail.value?.auditStatus === 1)
const teamAuditStatusText = computed(() => {
  const status = teamDetail.value?.auditStatus
  return status != null ? TEAM_AUDIT_STATUS[status] || '未知' : '-'
})

/** 仅有效队长、当前可管理队长、历史队长展示队长权限卡片 */
const showCaptainGrantPanel = computed(() => {
  if (canManageCurrentTeam.value) return true
  if (isHistoricalCaptain.value) return true
  if (captainQual.value?.runtimeValid) return true
  return false
})

const availableTabs = computed<TeamTab[]>(() => {
  const joinableTabs: TeamTab[] = canJoinTeam.value ? ['joinableTeams'] : []
  if (hasTeam.value) {
    if (canManageCurrentTeam.value) {
      return ['overview', 'profile', 'members', 'invitations', 'applications', 'registration', ...joinableTabs]
    }
    if (isHistoricalCaptain.value) {
      return ['overview', 'members', 'registration', ...joinableTabs]
    }
    return ['overview', 'members', 'registration', ...joinableTabs]
  }
  const preTeamTabs: TeamTab[] = ['joinableTeams', 'pendingInvites', 'myApplications']
  if (canCreateTeam.value) preTeamTabs.unshift('create')
  if (canInheritTeam.value && teamStore.inheritableTeams.length) preTeamTabs.unshift('create')
  return preTeamTabs
})

const bannerTitle = computed(() => {
  if (hasTeam.value) return teamStore.myTeamSummary?.teamName || '我的球队'
  if (canCreateTeam.value) return '我的球队'
  return '加入球队'
})

const bannerDescription = computed(() => {
  if (hasTeam.value && canManageCurrentTeam.value) {
    if (isTeamAuditApproved.value) return '管理球队资料、成员、邀请与赛事报名'
    return '球队认证审核中，可先编辑资料并管理成员；赛事报名需审核通过后开放'
  }
  if (hasTeam.value && isHistoricalCaptain.value) {
    return '可查看历史球队与赛事记录，当前暂无有效队长权限'
  }
  if (hasTeam.value) return '查看当前球队信息与赛事记录'
  if (canCreateTeam.value) return '你已开通队长权限，可以创建并管理自己的球队。'
  if (canInheritTeam.value && teamStore.inheritableTeams.length) return '你有可继承的历史球队，也可浏览并申请加入其他球队。'
  if (isCaptainQualExpired.value) return '当前赛事周期已结束，队长权限已收回；你仍可申请加入其他球队。'
  return '浏览可加入球队、处理邀请或提交加入申请'
})

const bannerStatusText = computed(() => {
  if (hasTeam.value && canManageCurrentTeam.value) return '队长'
  if (hasTeam.value && isCaptainMember.value) return '历史队长'
  if (hasTeam.value) return '队员'
  if (showCaptainGrantPanel.value && captainQual.value?.runtimeValid) return '队长权限有效'
  if (showCaptainGrantPanel.value && isCaptainQualExpired.value) return '队长权限已收回'
  if (showCaptainGrantPanel.value && isCaptainQualInvalid.value) {
    return captainQual.value?.invalidReason || '队长权限无效'
  }
  return '球员'
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
    if (typeof tab !== 'string') return
    const normalized = tab === 'discover' ? 'joinableTeams' : tab
    if (availableTabs.value.includes(normalized as TeamTab)) {
      activeTab.value = normalized as TeamTab
    }
  },
  { immediate: true },
)

watch(availableTabs, (tabs) => {
  if (!tabs.includes(activeTab.value)) {
    activeTab.value = tabs[0]
  }
}, { immediate: true })

watch(teamDetail, (detail) => {
  if (!detail) return
  profileForm.teamName = detail.teamName || ''
  profileForm.shortName = detail.shortName || ''
  profileForm.description = detail.description || ''
  profileForm.publicStatus = detail.publicStatus ?? 1
}, { immediate: true })

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    await teamStore.bootstrap()
    if (canInheritTeam.value) {
      await teamStore.loadInheritableTeams().catch(() => undefined)
    }
    if (canJoinTeam.value || !hasTeam.value) {
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
    if (typeof route.query.tab !== 'string' && !hasTeam.value) {
      activeTab.value = availableTabs.value.includes('joinableTeams')
        ? 'joinableTeams'
        : availableTabs.value[0]
    }
    await openRegistrationFromQuery().catch(() => undefined)
  }
}

onMounted(bootstrap)

function setTab(tab: TeamTab) {
  activeTab.value = tab
  router.replace({ query: tab === availableTabs.value[0] ? {} : { tab } })
}

async function refreshTabData(tab: TeamTab) {
  if (tab === 'pendingInvites') {
    await teamStore.loadMyInvitations({ status: 0 })
    return
  }
  if (tab === 'joinableTeams') {
    await joinablePanelRef.value?.loadCurrentPlatform()
    return
  }
  if (tab === 'myApplications') {
    await teamStore.loadMyJoinApplications()
    return
  }
  if (tab === 'create' && canInheritTeam.value) {
    await teamStore.loadInheritableTeams().catch(() => undefined)
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
    const tasks: Promise<unknown>[] = [teamStore.loadRegistrations(teamId.value)]
    if (canManageCurrentTeam.value) {
      tasks.unshift(teamStore.loadAvailableSeasons(teamId.value))
    }
    await Promise.all(tasks)
  }
}

watch(activeTab, (tab) => {
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
  try {
    await teamStore.createTeamAction({
      teamName: createForm.teamName.trim(),
      shortName: createForm.shortName.trim(),
      description: createForm.description.trim() || undefined,
      publicStatus: createForm.publicStatus,
      logoFileId: createForm.logoFileId || undefined,
    })
    ElMessage.success('球队创建成功，等待审核')
    await bootstrap()
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
      description: profileForm.description.trim() || undefined,
      publicStatus: profileForm.publicStatus,
    })
    ElMessage.success('球队资料已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

async function saveMember(
  member: TeamMemberItemDto,
  payload: { jerseyNo: string; rosterType: string },
) {
  if (!teamId.value) return
  try {
    await teamStore.updateMemberAction(teamId.value, member.teamMemberId, {
      jerseyNo: payload.jerseyNo,
      rosterType: payload.rosterType,
    })
    ElMessage.success('成员信息已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

async function removeMember(member: TeamMemberItemDto) {
  if (!teamId.value) return
  try {
    await teamStore.removeMemberAction(teamId.value, member.teamMemberId)
    ElMessage.success('成员已移除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移除失败')
  }
}

async function handleLeaveTeam() {
  if (!teamId.value) return
  try {
    await ElMessageBox.confirm('确认退出当前球队？', '离队确认', { type: 'warning' })
    await teamStore.leaveTeamAction(teamId.value)
    ElMessage.success('已退出球队')
    await bootstrap()
    setTab(availableTabs.value[0] || 'joinableTeams')
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

function selectInvitePlayer(player: CandidatePlayerItemDto) {
  if (player.canInvite === false) return
  selectedInvitePlayerId.value = player.playerId
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

async function startRegistrationForSeason(season: AvailableSeasonItemDto) {
  if (!teamId.value) return
  registrationDraftForm.seasonId = season.seasonId
  registrationDraftForm.platformCode = season.platformCode || ''
  try {
    const detail = await teamStore.createRegistrationDraftAction(teamId.value, {
      seasonId: season.seasonId,
      platformCode: season.platformCode || undefined,
      contactName: registrationDraftForm.contactName.trim() || undefined,
      contactMobile: registrationDraftForm.contactMobile.trim() || undefined,
      contactEmail: registrationDraftForm.contactEmail.trim() || undefined,
      remark: registrationDraftForm.remark.trim() || undefined,
    })
    selectedRegistrationId.value = detail.registrationId
    await openRegistrationDetail(detail.registrationId)
    ElMessage.success('报名草稿已创建')
    await teamStore.loadRegistrations(teamId.value)
    await teamStore.loadAvailableSeasons(teamId.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

async function openPlatformPicker(seasonId: string) {
  pendingSeasonId.value = seasonId
  selectedPlatformCode.value = ''
  try {
    await teamStore.loadSeasonPlatforms(seasonId)
    showPlatformPicker.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载赛事平台失败')
  }
}

async function confirmPlatformAndCreateDraft() {
  if (!teamId.value || !pendingSeasonId.value || !selectedPlatformCode.value) {
    ElMessage.warning('请选择报名平台')
    return
  }
  registrationDraftForm.seasonId = pendingSeasonId.value
  registrationDraftForm.platformCode = selectedPlatformCode.value
  showPlatformPicker.value = false
  await createRegistrationDraft()
}

async function createRegistrationDraft() {
  if (!teamId.value || !registrationDraftForm.seasonId) {
    ElMessage.warning('请选择赛季')
    return
  }
  try {
    const detail = await teamStore.createRegistrationDraftAction(teamId.value, {
      seasonId: registrationDraftForm.seasonId,
      platformCode: registrationDraftForm.platformCode || undefined,
      contactName: registrationDraftForm.contactName.trim() || undefined,
      contactMobile: registrationDraftForm.contactMobile.trim() || undefined,
      contactEmail: registrationDraftForm.contactEmail.trim() || undefined,
      remark: registrationDraftForm.remark.trim() || undefined,
    })
    selectedRegistrationId.value = detail.registrationId
    await openRegistrationDetail(detail.registrationId)
    ElMessage.success('报名草稿已创建')
    await teamStore.loadRegistrations(teamId.value)
    await teamStore.loadAvailableSeasons(teamId.value)
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
  await teamStore.loadRosterCandidates(registrationId)
}

function resolveRosterMissingPositions() {
  const selectedPositions = new Set(
    rosterSelections.value
      .map((item) => item.positionCode?.trim().toUpperCase())
      .filter(Boolean),
  )
  return STARTING_POSITION_CODES.filter((code) => !selectedPositions.has(code))
}

function toggleRosterCandidate(candidate: import('@/api/types/teamRegistration').RegistrationRosterCandidateDto) {
  if (candidate.canSelect === false) return
  const exists = rosterSelections.value.find((item) => item.teamMemberId === candidate.teamMemberId)
  if (exists) {
    rosterSelections.value = rosterSelections.value.filter((item) => item.teamMemberId !== candidate.teamMemberId)
    return
  }
  if (rosterSelections.value.length >= 8) {
    ElMessage.warning('阵容最多 8 人')
    return
  }
  rosterSelections.value.push({
    teamMemberId: candidate.teamMemberId,
    gameNickname: candidate.gameNickname || candidate.nickname,
    positionCode: candidate.positionCode,
    jerseyNo: candidate.jerseyNo,
    isStarting: rosterSelections.value.length < 5 ? 1 : 0,
    isCaptain: 0,
    sortNo: rosterSelections.value.length + 1,
  })
}

async function inheritTeam(teamIdToBind: string) {
  try {
    await teamStore.inheritTeamAction(teamIdToBind)
    ElMessage.success('历史球队继承成功')
    await bootstrap()
    setTab('overview')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '继承失败')
  }
}

async function openRegistrationFromQuery() {
  const registrationId = route.query.registrationId
  if (typeof registrationId !== 'string' || !registrationId) return
  if (!hasTeam.value || !teamId.value) return
  if (!availableTabs.value.includes('registration')) return
  if (activeTab.value !== 'registration') {
    activeTab.value = 'registration'
  }
  await refreshTabData('registration')
  if (selectedRegistrationId.value === registrationId && teamStore.registrationDetail) {
    await nextTick()
    document.getElementById('team-registration-detail')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  await openRegistrationDetail(registrationId)
  await nextTick()
  document.getElementById('team-registration-detail')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

watch(
  () => route.query.registrationId,
  () => {
    if (pageLoading.value) return
    void openRegistrationFromQuery()
  },
)

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
  const missingPositions = resolveRosterMissingPositions()
  if (rosterSelections.value.length < 5) {
    ElMessage.warning('阵容至少需要 5 人')
    return
  }
  if (missingPositions.length) {
    const labels = missingPositions.map((code) => formatPositionLabel(code)).join('、')
    ElMessage.warning(`当前阵容缺少 ${labels}，无法提交报名阵容`)
    return
  }
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
  const missingPositions = resolveRosterMissingPositions()
  if (missingPositions.length) {
    const labels = missingPositions.map((code) => formatPositionLabel(code)).join('、')
    ElMessage.warning(`当前阵容缺少 ${labels}，无法提交报名阵容`)
    return
  }
  try {
    await teamStore.submitRegistrationAction(selectedRegistrationId.value)
    ElMessage.success('报名已提交，正在等待后台审核')
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

const tabLabels = computed<Record<TeamTab, string>>(() => ({
  pendingInvites: '待处理邀请',
  joinableTeams: '可加入球队',
  myApplications: '我的加入申请',
  create: '创建球队',
  overview: '概览',
  profile: '球队资料',
  members: canManageCurrentTeam.value ? '成员管理' : (isHistoricalCaptain.value ? '历史成员' : '成员列表'),
  invitations: '邀请成员',
  applications: '加入审核',
  registration: canManageCurrentTeam.value ? '赛事报名' : (isHistoricalCaptain.value ? '历史赛事' : '赛事记录'),
}))
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

    <CaptainGrantPanel
      v-if="showCaptainGrantPanel"
      :qualification="captainQual"
      :loading="teamStore.qualificationLoading"
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

      <template v-if="activeTab === 'pendingInvites'">
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
      </template>

      <template v-else-if="activeTab === 'joinableTeams'">
        <JoinableTeamPanel ref="joinablePanelRef" />
      </template>

      <template v-else-if="activeTab === 'myApplications'">
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
      </template>

      <template v-else-if="activeTab === 'create'">
        <AccountPanel v-if="canInheritTeam && teamStore.inheritableTeams.length" title="继承历史球队">
          <p class="team-tip">你有可继承的历史球队，可将当前队长权限绑定到历史球队继续管理。</p>
          <div class="team-card-list">
            <article v-for="team in teamStore.inheritableTeams" :key="team.teamId" class="team-card">
              <div>
                <p><strong>{{ team.teamName }}</strong></p>
                <p>成员 {{ team.currentMemberCount ?? 0 }} 人 · {{ team.auditStatusText || '审核状态未知' }}</p>
                <p v-if="team.cannotInheritReason" class="team-tip">{{ team.cannotInheritReason }}</p>
              </div>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="team.canInherit === false || teamStore.actionLoading"
                @click="inheritTeam(team.teamId)"
              >
                继承球队
              </button>
            </article>
          </div>
        </AccountPanel>

        <AccountPanel title="创建球队">
          <p v-if="canCreateTeam" class="team-tip">你已开通队长权限，可以创建新的球队本体。</p>
          <p v-else class="team-tip">
            当前暂无有效队长权限，暂不能创建球队。队长权限由平台根据赛事缴费结果后台开通。
          </p>
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
              招募状态
              <select v-model.number="createForm.publicStatus">
                <option :value="1">公开招募</option>
                <option :value="0">不公开招募</option>
              </select>
            </label>
            <label class="team-form-full">球队简介<textarea v-model="createForm.description" rows="3" maxlength="500" placeholder="可选，用于展示球队介绍" /></label>
          </div>
          <button
            type="button"
            class="account-btn-primary"
            :disabled="teamStore.actionLoading || !canCreateTeam"
            :title="canCreateTeam ? undefined : '当前暂无有效队长权限，暂不能创建球队'"
            @click="submitCreateTeam"
          >
            创建球队
          </button>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'overview'">
        <AccountPanel title="球队概览">
          <div
            v-if="isHistoricalCaptain"
            class="team-audit-banner"
          >
            <AccountStatusBadge text="历史队长" type="warning" />
            <p class="team-tip">
              {{ captainQual?.cannotManageReason || '当前暂无有效队长权限，暂不能管理球队。' }}
            </p>
          </div>
          <div
            v-else-if="canManageCurrentTeam && !isTeamAuditApproved"
            class="team-audit-banner"
          >
            <AccountStatusBadge :text="teamAuditStatusText" type="warning" />
            <p class="team-tip">
              球队认证{{ teamAuditStatusText }}，可先管理成员与资料；赛事报名需审核通过后开放。
            </p>
          </div>
          <dl v-if="teamDetail" class="account-info-list">
            <div class="account-field-row"><dt>球队编号</dt><dd>{{ teamDetail.teamNo || '-' }}</dd></div>
            <div class="account-field-row"><dt>球队名称</dt><dd>{{ teamDetail.teamName }}</dd></div>
            <div v-if="canManageCurrentTeam" class="account-field-row"><dt>认证状态</dt><dd>{{ teamAuditStatusText }}</dd></div>
            <div class="account-field-row"><dt>简称</dt><dd>{{ teamDetail.shortName || '-' }}</dd></div>
            <div class="account-field-row"><dt>地区</dt><dd>{{ teamDetail.regionName || '-' }}</dd></div>
            <div class="account-field-row"><dt>成员</dt><dd>{{ teamDetail.currentMemberCount ?? 0 }}/{{ teamDetail.maxMemberCount ?? CNPA_TEAM_MAX_MEMBER_COUNT }}<span class="team-member-hint"> · 正式阵容 {{ teamMemberLimitHint }}</span></dd></div>
            <div class="account-field-row"><dt>我的角色</dt><dd>{{ teamDetail.myRole || (canManageCurrentTeam || isCaptainMember ? '队长' : '队员') }}</dd></div>
            <div class="account-field-row"><dt>创建时间</dt><dd>{{ formatDateTime(teamDetail.createTime) }}</dd></div>
            <div class="account-field-row"><dt>介绍</dt><dd>{{ teamDetail.description || '暂无' }}</dd></div>
          </dl>
          <AccountEmptyState v-else message="球队信息加载中" />
          <div v-if="!canManageCurrentTeam" class="team-manage-actions">
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
              公开状态
              <select v-model.number="profileForm.publicStatus">
                <option :value="1">公开</option>
                <option :value="0">非公开</option>
              </select>
            </label>
            <label class="team-form-full">球队介绍<textarea v-model="profileForm.description" rows="3" maxlength="500" /></label>
          </div>
          <button
            type="button"
            class="account-btn-primary"
            :disabled="teamStore.actionLoading || !canManageCurrentTeam"
            @click="submitProfileUpdate"
          >
            保存资料
          </button>
          <p class="team-tip">队长转让功能后续开放。</p>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'members'">
        <AccountPanel :title="tabLabels.members">
          <TeamMemberList
            :members="teamStore.members"
            :loading="teamStore.membersLoading"
            :is-captain="canManageCurrentTeam"
            :team-id="teamId"
            @save="saveMember"
            @remove="removeMember"
          />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'invitations'">
        <AccountPanel title="邀请候选球员">
          <div class="team-search-row">
            <input v-model="inviteKeyword" type="search" placeholder="搜索用户账号、邮箱、球员编号或昵称" />
            <button type="button" class="account-btn-secondary" @click="searchCandidates">搜索</button>
          </div>
          <div v-if="teamStore.candidatePlayers.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>昵称</th>
                  <th>球员编号</th>
                  <th>认证状态</th>
                  <th>当前球队</th>
                  <th>平台游戏 ID</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="player in teamStore.candidatePlayers"
                  :key="player.playerId"
                  :class="{ 'team-table__row--selected': selectedInvitePlayerId === player.playerId }"
                >
                  <td>{{ player.nickname || player.gameNickname || '-' }}</td>
                  <td>{{ player.playerNo || '-' }}</td>
                  <td>{{ player.certificationStatusText || formatPlayerCertStatus(player.certificationStatus) }}</td>
                  <td>{{ player.currentTeamStatusText || (player.currentTeamId ? '已加入球队' : '未加入球队') }}</td>
                  <td>{{ player.platformAccountSummary || player.platformGameId || '-' }}</td>
                  <td>
                    <button
                      type="button"
                      class="account-btn-secondary team-table__btn"
                      :disabled="player.canInvite === false"
                      :title="player.cannotInviteReason || undefined"
                      @click="selectInvitePlayer(player)"
                    >
                      {{ selectedInvitePlayerId === player.playerId ? '已选择' : '选择' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState
            v-else
            :message="teamStore.candidateEmptyReason || '当前暂无可邀请的已认证球员'"
          />
          <div class="team-form-grid">
            <label class="team-form-full">
              邀请说明
              <textarea v-model="inviteMessage" rows="2" maxlength="200" />
            </label>
          </div>
          <button
            type="button"
            class="account-btn-primary"
            :disabled="teamStore.actionLoading || !selectedInvitePlayerId"
            @click="sendInvite"
          >
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
          <div v-if="teamStore.teamJoinApplications.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>昵称</th>
                  <th>球员编号</th>
                  <th>位置</th>
                  <th>认证状态</th>
                  <th>申请说明</th>
                  <th>申请时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teamStore.teamJoinApplications" :key="item.applicationId">
                  <td>{{ item.applicantNickname || '-' }}</td>
                  <td>{{ item.playerNo || '-' }}</td>
                  <td>
                    <span
                      class="team-position"
                      :title="formatPositionCell(item.positionCode, item.positionName)"
                    >
                      {{ item.positionCode || '未设置' }}
                    </span>
                  </td>
                  <td>{{ formatPlayerCertStatus(item.certificationStatus) }}</td>
                  <td>{{ item.applyReason || '无' }}</td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td v-if="item.canProcess" class="team-card-actions">
                    <button type="button" class="account-btn-primary team-table__btn" @click="processApplication(item.applicationId, true)">通过</button>
                    <button type="button" class="account-btn-secondary team-table__btn" @click="processApplication(item.applicationId, false)">驳回</button>
                  </td>
                  <td v-else>-</td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无待处理申请" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'registration'">
        <AccountPanel :title="canManageCurrentTeam ? '可报名赛事' : '赛事记录'">
          <div v-if="canManageCurrentTeam && teamStore.availableSeasons.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>赛事</th>
                  <th>所属赛季</th>
                  <th>参赛端</th>
                  <th>报名截止</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="season in teamStore.availableSeasons" :key="`${season.seasonId}-${season.platformCode || 'default'}`">
                  <td>{{ season.tournamentName }}</td>
                  <td>{{ season.seasonName }}</td>
                  <td>{{ season.platformName || season.platformCode || '-' }}</td>
                  <td>{{ formatDateTime(season.registrationEndTime) }}</td>
                  <td>
                    <button
                      type="button"
                      class="account-btn-primary team-table__btn"
                      @click="startRegistrationForSeason(season)"
                    >
                      报名
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState
            v-else-if="canManageCurrentTeam"
            message="当前暂无可报名赛事"
            :subtitle="teamStore.availableSeasonEmptyReason || '请确认队长权限已关联赛事且处于报名期'"
          />
        </AccountPanel>

        <AccountPanel title="报名记录">
          <div v-if="teamStore.registrations.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>赛事</th>
                  <th>赛季</th>
                  <th>报名平台</th>
                  <th>状态</th>
                  <th>提交时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teamStore.registrations" :key="item.registrationId">
                  <td>{{ item.tournamentName }}</td>
                  <td>{{ item.seasonName }}</td>
                  <td>{{ item.platformName || item.platformCode || '-' }}</td>
                  <td>{{ item.registrationStatusText || mapTeamRegistrationStatusText(item.registrationStatus) }}</td>
                  <td>{{ formatDateTime(item.submittedTime || item.createTime) }}</td>
                  <td>
                    <button type="button" class="account-btn-secondary team-table__btn" @click="openRegistrationDetail(item.registrationId)">
                      {{ canManageCurrentTeam ? '管理' : '查看' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无报名记录" />
        </AccountPanel>

        <div v-if="teamStore.registrationDetail && (canManageCurrentTeam || selectedRegistrationId)" id="team-registration-detail">
        <AccountPanel title="报名详情">
          <dl class="account-info-list">
            <div class="account-field-row"><dt>报名编号</dt><dd>{{ teamStore.registrationDetail.registrationNo }}</dd></div>
            <div class="account-field-row"><dt>报名平台</dt><dd>{{ teamStore.registrationDetail.platformName || teamStore.registrationDetail.platformCode || '-' }}</dd></div>
            <div class="account-field-row"><dt>状态</dt><dd>{{ teamStore.registrationDetail.registrationStatusText }}</dd></div>
            <div v-if="teamStore.registrationDetail.registrationStatus === 2 || teamStore.registrationDetail.registrationStatus === 1" class="account-field-row">
              <dt>审核说明</dt><dd>报名已提交，正在等待后台审核。</dd>
            </div>
            <div v-if="teamStore.registrationDetail.registrationStatus === 4 && teamStore.registrationDetail.remark" class="account-field-row">
              <dt>驳回原因</dt><dd>{{ teamStore.registrationDetail.remark }}</dd>
            </div>
            <div v-if="teamStore.registrationDetail.rosterLocked" class="account-field-row team-roster-lock-tip">
              <dt>阵容状态</dt>
              <dd>
                {{ teamStore.registrationDetail.rosterLockStatusText || '已锁定' }}。
                {{ teamStore.registrationDetail.rosterLockedHint || '报名已审核通过，阵容已锁定。如需调整，请重新提交审核。' }}
              </dd>
            </div>
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
            <button
              v-if="teamStore.registrationDetail.rosterLocked"
              type="button"
              class="account-btn-secondary"
              disabled
              title="阵容变更审核流程暂未开放，请联系赛事管理员"
            >申请调整阵容</button>
          </div>
          <label v-if="teamStore.registrationDetail.canWithdraw" class="team-inline-field">
            撤回原因
            <input v-model="withdrawReason" type="text" maxlength="200" />
          </label>

          <h4 class="team-subtitle">{{ teamStore.registrationDetail.rosterLocked ? '报名阵容（只读）' : '阵容候选成员' }}</h4>
          <p v-if="teamStore.registrationDetail.rosterLocked" class="team-tip team-roster-lock-tip">
            报名已审核通过，阵容已锁定。如需调整，请重新提交审核或联系赛事管理员。
          </p>
          <p v-else class="team-tip">仅拥有当前报名平台已生效游戏 ID 的成员可选入阵容。</p>
          <div v-if="teamStore.rosterCandidates.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>球员</th>
                  <th>位置</th>
                  <th>平台游戏 ID</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="candidate in teamStore.rosterCandidates"
                  :key="candidate.teamMemberId"
                  :class="{ 'team-table__row--disabled': candidate.canSelect === false }"
                >
                  <td>{{ candidate.gameNickname || candidate.nickname || candidate.playerNo }}</td>
                  <td>{{ formatPositionCell(candidate.positionCode, candidate.positionName) }}</td>
                  <td>{{ candidate.platformGameId || '-' }}</td>
                  <td>
                    <button
                      type="button"
                      class="account-btn-secondary team-table__btn"
                      :disabled="candidate.canSelect === false || !teamStore.registrationDetail.canEdit"
                      :title="candidate.cannotSelectReason || undefined"
                      @click="toggleRosterCandidate(candidate)"
                    >
                      {{ rosterSelections.some((item) => item.teamMemberId === candidate.teamMemberId) ? '已选' : '选择' }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState v-else message="暂无候选成员" />

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
        </div>
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
            <p v-if="activeInvitation.cannotAcceptReason" class="team-tip">{{ activeInvitation.cannotAcceptReason }}</p>
            <button type="button" class="account-btn-secondary" @click="respondInvitation(false)">拒绝</button>
            <button
              v-if="activeInvitation.canAccept !== false"
              type="button"
              class="account-btn-primary"
              @click="respondInvitation(true)"
            >
              接受
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showPlatformPicker" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="showPlatformPicker = false" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true">
          <h3>选择报名平台</h3>
          <p class="team-tip">报名赛事时需选择参赛端（PC / PS / Xbox）；同一球队可按不同参赛端分别报名。</p>
          <div class="team-platform-picker">
            <label
              v-for="platform in teamStore.seasonPlatforms"
              :key="platform.platformCode"
              class="team-platform-picker__item"
              :class="{ 'team-platform-picker__item--disabled': !platform.open }"
            >
              <input
                v-model="selectedPlatformCode"
                type="radio"
                name="registration-platform"
                :value="platform.platformCode"
                :disabled="!platform.open"
              />
              <span>{{ platform.platformName || platform.platformCode }}</span>
              <small v-if="platform.disabledReason">{{ platform.disabledReason }}</small>
            </label>
          </div>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" @click="showPlatformPicker = false">取消</button>
            <button type="button" class="account-btn-primary" @click="confirmPlatformAndCreateDraft">创建报名草稿</button>
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

.team-audit-banner {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(214, 138, 22, 0.35);
  border-radius: 4px;
  background: rgba(214, 138, 22, 0.08);
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

.team-table__row--selected {
  background: rgba(64, 158, 255, 0.08);
}

.team-position {
  cursor: default;
  border-bottom: 1px dotted rgba(20, 20, 20, 0.25);
}

.team-member-hint {
  margin-left: 4px;
  font-size: 12px;
  color: #696969;
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

.team-platform-picker {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1rem;
}

.team-platform-picker__item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: 1px solid var(--account-border, #d9dee8);
  border-radius: 4px;
  cursor: pointer;

  small {
    flex-basis: 100%;
    color: #b45309;
    font-size: 0.85rem;
  }

  &--disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
}

.team-table__row--disabled {
  opacity: 0.55;
}

@media (max-width: 768px) {
  .team-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
