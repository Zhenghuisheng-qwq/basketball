<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
import AccountTeamLogo from '@/components/account/team/AccountTeamLogo.vue'
import TeamProfilePanel from '@/components/account/team/TeamProfilePanel.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import TeamListPagination from '@/components/account/team/TeamListPagination.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import { useTeamStore } from '@/stores/team'
import { useAccountStore } from '@/stores/account'
import { v4GameModeLabel, v4PlatformLabel } from '@/utils/v4Display'
import { mapTeamRegistrationStatusText } from '@/utils/accountMappers'
import {
  goToEventTeamManagement,
  goToRegistrationEditFlow,
} from '@/utils/registrationNavigation'
import {
  formatPositionLabel,
} from '@/constants/rosterRules'
import { formatDateTime } from '@/utils/formatDate'
import { uploadAccountFile } from '@/api/accountFile'
import type { TeamInvitationItemDto } from '@/api/types/teamInvitation'
import type { CandidatePlayerItemDto, TeamMemberItemDto } from '@/api/types/teamMember'
import type { AvailableSeasonItemDto, TeamRegistrationItemDto } from '@/api/types/teamRegistration'

type TeamListScope = 'current' | 'history'

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

function formatEventContext(item?: {
  seriesName?: string
  seasonName?: string
  divisionName?: string
  platformCode?: string
  divisionPlatformCode?: string
  gameMode?: string
}) {
  if (!item) return '赛事信息暂无'
  const platform = item.platformCode || item.divisionPlatformCode
  const seriesOrSeason = [item.seriesName, item.seasonName].filter(Boolean).join(' · ')
  const divisionBits = [
    item.divisionName,
    platform ? v4PlatformLabel(platform) : '',
    item.gameMode ? v4GameModeLabel(item.gameMode) : '',
  ].filter(Boolean).join(' · ')
  const text = [seriesOrSeason, divisionBits].filter(Boolean).join(' / ')
  return text || '赛事信息暂无'
}

const listPageSizeOptions = [10, 20, 50]

function roleLabel(role?: string, isCaptain?: boolean) {
  if (isCaptain || role === 'CAPTAIN') return '队长'
  if (role === 'MEMBER' || role) return '成员'
  return '-'
}

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

const TEAM_TAB_PAGE_SIZE = 5

const inviteCandidatePage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })
const historyInvitePage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })
const historyInviteKeyword = ref('')
const historyInviteStatus = ref<number | ''>('')
const historyInviteClientMode = ref(false)
const historyInviteClientRecords = ref<TeamInvitationItemDto[]>([])
const showHistoryInviteDialog = ref(false)
const HISTORY_INVITE_STATUS_OPTIONS = [
  { label: '全部', value: '' as const },
  { label: '待响应', value: 0 },
  { label: '已接受', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '已失效', value: 3 },
  { label: '已撤回', value: 4 },
]
const joinAuditPage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })
const availableSeasonPage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })
const registrationListPage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })

const route = useRoute()
const router = useRouter()
const teamStore = useTeamStore()
const accountStore = useAccountStore()

const listScope = ref<TeamListScope>('current')
const activeTab = ref<TeamTab>('joinableTeams')
const pageLoading = ref(true)
const loadError = ref('')
const historyLoaded = ref(false)

const joinablePanelRef = ref<InstanceType<typeof JoinableTeamPanel> | null>(null)

const inviteKeyword = ref('')
const showInviteConfirmDialog = ref(false)
const inviteModalPlayer = ref<CandidatePlayerItemDto | null>(null)
const inviteModalMessage = ref('')
const inviteModalError = ref('')

const createForm = reactive({
  divisionId: '',
  teamName: '',
  shortName: '',
  description: '',
  publicStatus: 1,
  logoFileId: '',
  logoPreviewUrl: '',
})

const creatableGrants = computed(() => teamStore.creatableQualifications)

watch(
  creatableGrants,
  (grants) => {
    if (!createForm.divisionId && grants.length === 1 && grants[0]?.divisionId) {
      createForm.divisionId = grants[0].divisionId
    }
  },
  { immediate: true },
)

const profileForm = reactive({
  teamName: '',
  shortName: '',
  description: '',
  publicStatus: 1,
})
const profileLogoPreviewUrl = ref('')

const registrationDraftForm = reactive({
  seasonId: '',
  platformCode: '',
  remark: '',
})
const showPlatformPicker = ref(false)
const pendingSeasonId = ref('')
const selectedPlatformCode = ref('')
const withdrawReason = ref('')
const selectedRegistrationId = ref('')
const showRegistrationDetailDialog = ref(false)
const registrationDetailLoading = ref(false)

const showInvitationDialog = ref(false)
const activeInvitation = ref<TeamInvitationItemDto | null>(null)

const currentTeams = computed(() => teamStore.currentTeams)
const historyTeams = computed(() => teamStore.historyTeams)
const selectedTeam = computed(() => teamStore.teamDetail || teamStore.myTeamSummary)
const hasSelectedTeam = computed(() => Boolean(selectedTeam.value?.teamId))
const viewingHistory = computed(() =>
  listScope.value === 'history'
  || Boolean(teamStore.teamDetail?.isHistorical)
  || Boolean(teamStore.myTeamSummary?.isHistorical),
)
const canManageCurrentTeam = computed(() =>
  !viewingHistory.value && teamStore.canManageCurrentTeam,
)
const isCaptainMember = computed(() => teamStore.isCaptainMember)
const isHistoricalCaptain = computed(() =>
  Boolean(teamStore.teamDetail?.isCaptain || teamStore.myTeamSummary?.isCaptain)
  && !canManageCurrentTeam.value,
)
const canCreateTeam = computed(() => teamStore.canCreateTeam)
const canJoinTeam = computed(() => teamStore.canJoinTeam)
const allGrants = computed(() => teamStore.captainQualifications)
const captainQual = computed(() => teamStore.captainQualification)
const teamId = computed(() => selectedTeam.value?.teamId || '')
const teamDetail = computed(() => teamStore.teamDetail)
const hasTeam = computed(() => hasSelectedTeam.value)
const displayTeamLogoUrl = computed(() =>
  profileLogoPreviewUrl.value
  || teamDetail.value?.logoUrl
  || teamStore.myTeamSummary?.logoUrl
  || '',
)
const displayTeamShortName = computed(() =>
  teamDetail.value?.shortName || teamStore.myTeamSummary?.shortName || '',
)
const displayTeamName = computed(() =>
  teamDetail.value?.teamName || teamStore.myTeamSummary?.teamName || '',
)
const isTeamAuditApproved = computed(() => teamDetail.value?.auditStatus === 1)
const teamAuditStatusText = computed(() => {
  const status = teamDetail.value?.auditStatus
  return status != null ? TEAM_AUDIT_STATUS[status] || '未知' : '-'
})

const registrationCaptainEmail = computed(() =>
  accountStore.currentAccount?.maskedEmail
  || accountStore.profile?.maskedEmail
  || teamStore.registrationDetail?.contactEmailMasked
  || '-',
)

const registrationDetailDialogTitle = computed(() => {
  const detail = teamStore.registrationDetail
  if (!detail) return '报名详情'
  const eventLabel = detail.tournamentName || detail.seasonName || '赛事'
  const platformLabel = detail.platformName || detail.platformCode || '-'
  return `报名详情 - ${eventLabel} / ${platformLabel}`
})

const registrationDetailStatusText = computed(() =>
  mapTeamRegistrationStatusText(teamStore.registrationDetail?.registrationStatus),
)

const registrationDetailAuditTime = computed(() => {
  const audits = teamStore.registrationAudits
  const lastAudit = audits.length ? audits[audits.length - 1] : undefined
  return formatDateTime(lastAudit?.auditTime) || '-'
})

const registrationDetailLastAuditor = computed(() => {
  const audits = teamStore.registrationAudits
  if (!audits.length) return '-'
  return audits[audits.length - 1]?.auditorName || '-'
})

const registrationDetailRosterRows = computed(() => {
  const detail = teamStore.registrationDetail
  if (!detail) return []

  if (detail.rosterLocked || detail.registrationStatus === 3) {
    return (detail.rosterItems || []).map((item) => ({
      key: item.teamMemberId,
      name: item.gameNickname || item.playerNo || '-',
      position: formatPositionCell(item.positionCode, undefined),
      platformId: '-',
      eligibility: '已纳入',
    }))
  }

  const selectedCandidates = teamStore.rosterCandidates.filter((candidate) => candidate.canSelect)
  if (selectedCandidates.length) {
    return selectedCandidates.map((candidate) => ({
      key: candidate.teamMemberId,
      name: candidate.gameNickname || candidate.nickname || candidate.playerNo || '-',
      position: formatPositionCell(candidate.positionCode, candidate.positionName),
      platformId: formatCandidatePlatformDisplay(candidate),
      eligibility: formatCandidateEligibility(candidate),
    }))
  }

  return (detail.rosterItems || []).map((item) => ({
    key: item.teamMemberId,
    name: item.gameNickname || item.playerNo || '-',
    position: formatPositionCell(item.positionCode, undefined),
    platformId: '-',
    eligibility: '已纳入',
  }))
})

/** 仅有效队长、当前可管理队长、历史队长展示队长权限卡片 */
const showCaptainGrantPanel = computed(() => {
  if (canManageCurrentTeam.value) return true
  if (isHistoricalCaptain.value) return true
  if (captainQual.value?.runtimeValid) return true
  return false
})

const availableTabs = computed<TeamTab[]>(() => {
  const joinableTabs: TeamTab[] = listScope.value === 'current' && canJoinTeam.value ? ['joinableTeams'] : []
  if (listScope.value === 'history') {
    if (hasSelectedTeam.value) {
      return ['overview', 'members']
    }
    return []
  }
  if (hasSelectedTeam.value && !viewingHistory.value) {
    if (canManageCurrentTeam.value) {
      return ['overview', 'profile', 'members', 'invitations', 'applications', 'registration', ...joinableTabs, 'create', 'pendingInvites', 'myApplications']
    }
    return ['overview', 'members', 'registration', ...joinableTabs, 'create', 'pendingInvites', 'myApplications']
  }
  const preTeamTabs: TeamTab[] = ['joinableTeams', 'pendingInvites', 'myApplications', 'create']
  return preTeamTabs
})

const bannerTitle = computed(() => {
  if (hasSelectedTeam.value) return selectedTeam.value?.teamName || '我的球队'
  return listScope.value === 'history' ? '历史球队' : '我的球队'
})

const bannerDescription = computed(() => {
  if (hasSelectedTeam.value && canManageCurrentTeam.value) {
    if (isTeamAuditApproved.value) return '管理当前赛事球队资料、成员与邀请'
    return '球队认证审核中，可先编辑资料并管理成员'
  }
  if (hasSelectedTeam.value && viewingHistory.value) {
    return '只读查看历史赛事球队与成员记录'
  }
  if (hasSelectedTeam.value) return '查看当前赛事球队信息'
  if (listScope.value === 'history') {
    return '赛事结束或你退出球队后，相关记录会保留在这里'
  }
  if (canCreateTeam.value) return '获得赛事队长权限后可创建本届球队，也可接受邀请或申请加入'
  return '当前暂无参赛球队时，可浏览可加入球队、处理邀请或申请加入'
})

const bannerStatusText = computed(() => {
  if (hasSelectedTeam.value && canManageCurrentTeam.value) return '队长'
  if (hasSelectedTeam.value && isHistoricalCaptain.value) return '历史队长'
  if (hasSelectedTeam.value && isCaptainMember.value) return '队长'
  if (hasSelectedTeam.value) return '成员'
  if (currentTeams.value.length) return `当前 ${currentTeams.value.length} 支`
  if (canCreateTeam.value) return '可创建球队'
  return '球员'
})

const bannerStatusType = computed(() => {
  if (hasSelectedTeam.value && viewingHistory.value) return 'warning'
  if (hasSelectedTeam.value) return 'success'
  if (canCreateTeam.value) return 'success'
  return 'info'
})

watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab !== 'string') return
    if (tab === 'history' || tab === 'current') {
      listScope.value = tab
      return
    }
    const normalized = tab === 'discover' ? 'joinableTeams' : tab
    if (availableTabs.value.includes(normalized as TeamTab)) {
      activeTab.value = normalized as TeamTab
    }
  },
  { immediate: true },
)

watch(
  () => route.query.scope,
  (scope) => {
    if (scope === 'history' || scope === 'current') {
      listScope.value = scope
    }
  },
  { immediate: true },
)

watch(availableTabs, (tabs) => {
  if (!tabs.length) return
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
  profileLogoPreviewUrl.value = detail.logoUrl || ''
}, { immediate: true })

async function bootstrap() {
  pageLoading.value = true
  loadError.value = ''
  try {
    await teamStore.bootstrap()
    if (listScope.value === 'history') {
      await ensureHistoryLoaded()
    }
    if (listScope.value === 'current') {
      await Promise.all([
        teamStore.loadMyInvitations({ currentPage: 1, pageSize: 10, status: 0 }).catch(() => undefined),
        teamStore.loadJoinableTeams().catch(() => undefined),
        teamStore.loadMyJoinApplications({ currentPage: 1, pageSize: 10 }).catch(() => undefined),
      ])
    }
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    pageLoading.value = false
    if (typeof route.query.tab !== 'string' && !hasSelectedTeam.value) {
      activeTab.value = availableTabs.value.includes('joinableTeams')
        ? 'joinableTeams'
        : (availableTabs.value[0] || 'create')
    }
    await openRegistrationFromQuery().catch(() => undefined)
  }
}

async function ensureHistoryLoaded(force = false) {
  if (!force && historyLoaded.value && teamStore.historyTeams.length) return
  await teamStore.loadHistoryTeams({
    currentPage: teamStore.historyPage || 1,
    pageSize: teamStore.historyPageSize || 10,
  })
  historyLoaded.value = true
}

async function changeHistoryPage(page: number) {
  await teamStore.loadHistoryTeams({
    currentPage: page,
    pageSize: teamStore.historyPageSize,
  })
  historyLoaded.value = true
}

async function changeHistoryPageSize(size: number) {
  await teamStore.loadHistoryTeams({ currentPage: 1, pageSize: size })
  historyLoaded.value = true
}

async function setListScope(scope: TeamListScope) {
  listScope.value = scope
  await router.replace({
    query: {
      ...route.query,
      scope,
      tab: undefined,
    },
  })
  if (scope === 'history') {
    await ensureHistoryLoaded()
    teamStore.teamDetail = null
    teamStore.myTeamSummary = null
    activeTab.value = 'overview'
    return
  }
  await teamStore.loadCurrentTeams()
  if (teamStore.myTeamSummary?.teamId) {
    await teamStore.selectTeam(teamStore.myTeamSummary.teamId)
    activeTab.value = 'overview'
  } else {
    teamStore.teamDetail = null
    activeTab.value = availableTabs.value[0] || 'joinableTeams'
  }
}

async function openTeamCard(team: { teamId: string; isHistorical?: boolean }) {
  const historical = Boolean(team.isHistorical) || listScope.value === 'history'
  if (historical && listScope.value !== 'history') {
    listScope.value = 'history'
    await ensureHistoryLoaded()
  }
  await teamStore.selectTeam(team.teamId)
  activeTab.value = 'overview'
  await refreshTabData('overview')
  if (historical) {
    await teamStore.loadMembers(team.teamId, { includeHistory: true }).catch(() => undefined)
  }
}

onMounted(bootstrap)

function setTab(tab: TeamTab) {
  activeTab.value = tab
  router.replace({
    query: {
      ...route.query,
      scope: listScope.value,
      tab: tab === availableTabs.value[0] ? undefined : tab,
    },
  })
}

async function loadInviteCandidateList(page = inviteCandidatePage.pageNum) {
  if (!teamId.value) return
  const pageData = await teamStore.loadCandidatePlayers(teamId.value, {
    currentPage: page,
    pageSize: inviteCandidatePage.pageSize,
    keyword: inviteKeyword.value || undefined,
  })
  inviteCandidatePage.pageNum = pageData.current || page
  inviteCandidatePage.total = pageData.total ?? 0
}

function matchesHistoryInviteKeyword(item: TeamInvitationItemDto, keyword: string) {
  const q = keyword.toLowerCase()
  return (
    (item.inviteeNickname || '').toLowerCase().includes(q)
    || (item.invitationNo || '').toLowerCase().includes(q)
    || (item.invitationMessage || '').toLowerCase().includes(q)
  )
}

async function fetchAllTeamInvitations(status?: number) {
  if (!teamId.value) return [] as TeamInvitationItemDto[]
  const all: TeamInvitationItemDto[] = []
  let page = 1
  const pageSize = 100
  while (page <= 50) {
    const pageData = await teamStore.loadTeamInvitations(teamId.value, {
      currentPage: page,
      pageSize,
      status,
    })
    all.push(...pageData.records)
    const total = pageData.total ?? 0
    if (pageData.records.length === 0) break
    if (total > 0 && all.length >= total) break
    if (pageData.records.length < pageSize) break
    page += 1
  }
  return all
}

async function loadHistoryInviteList(page = historyInvitePage.pageNum) {
  if (!teamId.value) return
  const keyword = historyInviteKeyword.value.trim()
  const status = historyInviteStatus.value === '' ? undefined : Number(historyInviteStatus.value)

  if (keyword) {
    historyInviteClientMode.value = true
    const all = await fetchAllTeamInvitations(status)
    const filtered = all.filter((item) => matchesHistoryInviteKeyword(item, keyword))
    historyInviteClientRecords.value = filtered
    historyInvitePage.total = filtered.length
    const maxPage = Math.max(1, Math.ceil(filtered.length / historyInvitePage.pageSize) || 1)
    historyInvitePage.pageNum = Math.min(page, maxPage)
    if (filtered.length === 0) historyInvitePage.pageNum = 1
    return
  }

  historyInviteClientMode.value = false
  historyInviteClientRecords.value = []
  const pageData = await teamStore.loadTeamInvitations(teamId.value, {
    currentPage: page,
    pageSize: historyInvitePage.pageSize,
    status,
  })
  historyInvitePage.pageNum = pageData.current || page
  historyInvitePage.total = pageData.total ?? 0
  if (!teamStore.teamInvitations.length && page > 1) {
    historyInvitePage.pageNum = page - 1
    await loadHistoryInviteList(page - 1)
  }
}

function resetHistoryInviteFilters() {
  historyInviteKeyword.value = ''
  historyInviteStatus.value = ''
  historyInvitePage.pageNum = 1
  historyInvitePage.total = 0
  historyInviteClientMode.value = false
  historyInviteClientRecords.value = []
}

function openHistoryInviteModal() {
  resetHistoryInviteFilters()
  showHistoryInviteDialog.value = true
  void loadHistoryInviteList(1)
}

function closeHistoryInviteModal() {
  showHistoryInviteDialog.value = false
  resetHistoryInviteFilters()
}

async function searchHistoryInvites() {
  historyInvitePage.pageNum = 1
  await loadHistoryInviteList(1)
}

async function changeHistoryInviteStatus(value: number | '') {
  historyInviteStatus.value = value
  historyInvitePage.pageNum = 1
  await loadHistoryInviteList(1)
}

function changeHistoryInvitePage(page: number) {
  if (page === historyInvitePage.pageNum) return
  historyInvitePage.pageNum = page
  if (!historyInviteClientMode.value) {
    void loadHistoryInviteList(page)
  }
}

const historyInvitePagedRecords = computed(() => {
  if (historyInviteClientMode.value) {
    const start = (historyInvitePage.pageNum - 1) * historyInvitePage.pageSize
    return historyInviteClientRecords.value.slice(start, start + historyInvitePage.pageSize)
  }
  return teamStore.teamInvitations
})

const historyInviteHasKeyword = computed(() => historyInviteKeyword.value.trim().length > 0)
const historyInviteHasFilter = computed(() => historyInviteStatus.value !== '')

const historyInviteEmptyMessage = computed(() => {
  if (historyInviteHasKeyword.value || historyInviteHasFilter.value) {
    return '暂无符合条件的邀请记录'
  }
  return '暂无邀请记录'
})

async function loadJoinAuditList(page = joinAuditPage.pageNum) {
  if (!teamId.value) return
  const pageData = await teamStore.loadTeamJoinApplications(teamId.value, {
    currentPage: page,
    pageSize: joinAuditPage.pageSize,
    status: 0,
  })
  joinAuditPage.pageNum = pageData.current || page
  joinAuditPage.total = pageData.total ?? 0
  if (!teamStore.teamJoinApplications.length && page > 1) {
    joinAuditPage.pageNum = page - 1
    await loadJoinAuditList(page - 1)
  }
}

async function loadAvailableSeasonList(page = availableSeasonPage.pageNum) {
  if (!teamId.value || !canManageCurrentTeam.value) return
  const pageData = await teamStore.loadAvailableSeasons(teamId.value, {
    currentPage: page,
    pageSize: availableSeasonPage.pageSize,
  })
  availableSeasonPage.pageNum = pageData.current || page
  availableSeasonPage.total = pageData.total ?? 0
}

async function loadRegistrationList(page = registrationListPage.pageNum) {
  if (!teamId.value) return
  const pageData = await teamStore.loadRegistrations(teamId.value, {
    currentPage: page,
    pageSize: registrationListPage.pageSize,
  })
  registrationListPage.pageNum = pageData.current || page
  registrationListPage.total = pageData.total ?? 0
}

async function refreshRegistrationLists() {
  await Promise.all([
    loadAvailableSeasonList(availableSeasonPage.pageNum),
    loadRegistrationList(registrationListPage.pageNum),
  ])
}

async function refreshTabData(tab: TeamTab) {
  if (tab === 'pendingInvites') {
    await teamStore.loadMyInvitations({
      currentPage: teamStore.invitePage || 1,
      pageSize: teamStore.invitePageSize || 10,
      status: 0,
    })
    return
  }
  if (tab === 'joinableTeams') {
    await joinablePanelRef.value?.loadCurrentPlatform()
    return
  }
  if (tab === 'myApplications') {
    await teamStore.loadMyJoinApplications({
      currentPage: teamStore.myApplicationPage || 1,
      pageSize: teamStore.myApplicationPageSize || 10,
    })
    return
  }
  if (!teamId.value) return
  if (tab === 'overview' || tab === 'profile') {
    await teamStore.loadTeamDetail(teamId.value)
  }
  if (tab === 'members') {
    await teamStore.loadMembers(teamId.value, { includeHistory: viewingHistory.value })
  }
  if (tab === 'invitations') {
    await loadInviteCandidateList()
  }
  if (tab === 'applications') await loadJoinAuditList()
  if (tab === 'registration') {
    await refreshRegistrationLists()
  }
}

watch(activeTab, (tab) => {
  refreshTabData(tab).catch(() => undefined)
})

async function handleLogoUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const res = await uploadAccountFile(file, 'TEAM_LOGO')
    createForm.logoFileId = res.data.fileId
    createForm.logoPreviewUrl = res.data.accessUrl
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Logo 上传失败')
  }
}

async function submitCreateTeam() {
  if (!createForm.divisionId) {
    ElMessage.warning('请选择要创建球队的赛事 Division')
    return
  }
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
      divisionId: createForm.divisionId,
      teamName: createForm.teamName.trim(),
      shortName: createForm.shortName.trim(),
      description: createForm.description.trim() || undefined,
      publicStatus: createForm.publicStatus,
      logoFileId: createForm.logoFileId || undefined,
    })
    ElMessage.success('球队创建成功，等待审核')
    createForm.divisionId = ''
    createForm.teamName = ''
    createForm.shortName = ''
    createForm.description = ''
    createForm.logoFileId = ''
    createForm.logoPreviewUrl = ''
    listScope.value = 'current'
    await teamStore.loadQualification()
    await teamStore.loadCurrentTeams()
    if (teamStore.myTeamSummary?.teamId) {
      await teamStore.selectTeam(teamStore.myTeamSummary.teamId)
    }
    setTab('overview')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

async function saveMember(
  member: TeamMemberItemDto,
  payload: { rosterType: string },
) {
  if (!teamId.value) return
  try {
    await teamStore.updateMemberAction(teamId.value, member.teamMemberId, {
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

function openInviteConfirmModal(player: CandidatePlayerItemDto) {
  if (player.canInvite === false) return
  inviteModalPlayer.value = player
  inviteModalMessage.value = ''
  inviteModalError.value = ''
  showInviteConfirmDialog.value = true
}

function closeInviteConfirmModal() {
  showInviteConfirmDialog.value = false
  inviteModalPlayer.value = null
  inviteModalMessage.value = ''
  inviteModalError.value = ''
}

async function confirmSendInvite() {
  if (!teamId.value || !inviteModalPlayer.value) return
  inviteModalError.value = ''
  try {
    await teamStore.sendInvitationAction(teamId.value, {
      inviteePlayerId: inviteModalPlayer.value.playerId,
      invitationMessage: inviteModalMessage.value.trim() || undefined,
    })
    ElMessage.success('邀请已发送')
    closeInviteConfirmModal()
    const refreshTasks = [loadInviteCandidateList(inviteCandidatePage.pageNum)]
    if (showHistoryInviteDialog.value) {
      historyInvitePage.pageNum = 1
      refreshTasks.push(loadHistoryInviteList(1))
    }
    await Promise.all(refreshTasks)
  } catch (error) {
    inviteModalError.value = error instanceof Error ? error.message : '邀请失败'
  }
}

async function searchCandidates() {
  if (!teamId.value) return
  inviteCandidatePage.pageNum = 1
  await loadInviteCandidateList(1)
}

function formatInviteModalCanInvite(player: CandidatePlayerItemDto) {
  return player.canInvite !== false ? '是' : '否'
}

function formatInviteModalCannotReason(player: CandidatePlayerItemDto) {
  return player.cannotInviteReason || '-'
}

function formatInviteModalPosition(player: CandidatePlayerItemDto) {
  return formatPositionCell(player.positionCode, player.positionName)
}

function formatCandidateTeamStatus(player: CandidatePlayerItemDto) {
  return player.currentTeamStatusText || (player.currentTeamId ? '已加入球队' : '未加入球队')
}

function formatCandidatePlatformId(player: CandidatePlayerItemDto) {
  return player.platformAccountSummary || player.platformGameId || '-'
}

function formatCandidateNickname(player: CandidatePlayerItemDto) {
  return player.nickname || player.gameNickname || '-'
}

function resolveInvitePlayerAvatarUrl(avatar?: string | null) {
  return avatar || undefined
}

async function processApplication(applicationId: string, approve: boolean) {
  try {
    await teamStore.processJoinApplicationAction(applicationId, approve)
    ElMessage.success(approve ? '已通过申请' : '已驳回申请')
    await loadJoinAuditList(joinAuditPage.pageNum)
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
      remark: registrationDraftForm.remark.trim() || undefined,
    })
    ElMessage.success('报名信息已保存')
    await refreshRegistrationLists()
    goToRegistrationEditFlow(router, {
      registrationId: detail.registrationId,
      seasonId: detail.seasonId || season.seasonId,
      platformCode: detail.platformCode || season.platformCode,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
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
      remark: registrationDraftForm.remark.trim() || undefined,
    })
    ElMessage.success('报名信息已保存')
    await refreshRegistrationLists()
    goToRegistrationEditFlow(router, {
      registrationId: detail.registrationId,
      seasonId: detail.seasonId || registrationDraftForm.seasonId,
      platformCode: detail.platformCode || registrationDraftForm.platformCode,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败')
  }
}

function closeRegistrationDetailDialog() {
  showRegistrationDetailDialog.value = false
  withdrawReason.value = ''
}

async function viewRegistrationDetail(registrationId: string) {
  selectedRegistrationId.value = registrationId
  showRegistrationDetailDialog.value = true
  registrationDetailLoading.value = true
  withdrawReason.value = ''
  try {
    await teamStore.loadRegistrationDetail(registrationId)
    await Promise.all([
      teamStore.loadRegistrationAudits(registrationId),
      teamStore.loadRosterCandidates(registrationId),
    ])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载报名详情失败')
    closeRegistrationDetailDialog()
  } finally {
    registrationDetailLoading.value = false
  }
}

function displayRegistrationStatus(status?: number) {
  return mapTeamRegistrationStatusText(status)
}

function resolveRegistrationAuditTime(item: TeamRegistrationItemDto) {
  return formatDateTime(item.auditTime || item.approvedTime) || '-'
}

function continueRegistration(item: TeamRegistrationItemDto) {
  goToRegistrationEditFlow(router, {
    registrationId: item.registrationId,
    seasonId: item.seasonId,
    platformCode: item.platformCode,
  })
}

function resubmitRegistration(item: TeamRegistrationItemDto) {
  continueRegistration(item)
}

function manageEventTeam(item: TeamRegistrationItemDto) {
  goToEventTeamManagement(router, { seasonId: item.seasonId })
}

type RegistrationListAction = {
  label: string
  handler: () => void
  primary?: boolean
}

function registrationListActions(item: TeamRegistrationItemDto): RegistrationListAction[] {
  const status = item.registrationStatus
  const actions: RegistrationListAction[] = []

  if (status === 0) {
    if (canManageCurrentTeam.value) {
      actions.push({ label: '继续报名', handler: () => continueRegistration(item), primary: true })
      actions.push({ label: '放弃本次报名', handler: () => abandonRegistrationDraft(item) })
    } else {
      actions.push({ label: '查看详情', handler: () => viewRegistrationDetail(item.registrationId) })
    }
    return actions
  }

  if (status === 1 || status === 2) {
    actions.push({ label: '查看详情', handler: () => viewRegistrationDetail(item.registrationId) })
    if (item.canWithdraw && canManageCurrentTeam.value) {
      actions.push({ label: '撤回修改', handler: () => viewRegistrationDetail(item.registrationId) })
    }
    return actions
  }

  if (status === 3) {
    actions.push({ label: '查看详情', handler: () => viewRegistrationDetail(item.registrationId) })
    if (canManageCurrentTeam.value) {
      actions.push({ label: '管理本届球队', handler: () => manageEventTeam(item), primary: true })
    }
    return actions
  }

  if (status === 4) {
    actions.push({ label: '查看详情', handler: () => viewRegistrationDetail(item.registrationId) })
    if (canManageCurrentTeam.value && item.canEdit !== false) {
      actions.push({ label: '修改重提', handler: () => resubmitRegistration(item), primary: true })
    }
    return actions
  }

  actions.push({ label: '查看详情', handler: () => viewRegistrationDetail(item.registrationId) })
  return actions
}

function formatCandidateEligibility(candidate: import('@/api/types/teamRegistration').RegistrationRosterCandidateDto) {
  if (candidate.canSelect) {
    return '已纳入报名'
  }
  return candidate.cannotSelectReason || candidate.platformIdentityStatusText || '不可纳入'
}

function formatCandidatePlatformDisplay(candidate: import('@/api/types/teamRegistration').RegistrationRosterCandidateDto) {
  if (candidate.platformGameId && candidate.canSelect) {
    const platform = teamStore.registrationDetail?.platformName || teamStore.registrationDetail?.platformCode || ''
    return platform ? `${platform}：${candidate.platformGameId}` : candidate.platformGameId
  }
  return candidate.platformIdentityStatusText || candidate.platformGameId || '-'
}

async function abandonRegistrationDraft(item: TeamRegistrationItemDto) {
  try {
    await ElMessageBox.confirm('确认放弃本次报名填写？', '放弃报名', { type: 'warning' })
    await teamStore.deleteRegistrationDraftAction(item.registrationId)
    if (selectedRegistrationId.value === item.registrationId) {
      closeRegistrationDetailDialog()
    }
    ElMessage.success('已放弃本次报名填写')
    if (teamId.value) await refreshRegistrationLists()
  } catch {
    // cancelled or failed
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

  const fromList = teamStore.registrations.find((item) => item.registrationId === registrationId)
  if (fromList && (fromList.registrationStatus === 0 || fromList.registrationStatus === 4)) {
    goToRegistrationEditFlow(router, {
      registrationId,
      seasonId: fromList.seasonId,
      platformCode: fromList.platformCode,
    })
    return
  }

  try {
    const detail = await teamStore.loadRegistrationDetail(registrationId)
    if (detail.registrationStatus === 0 || (detail.registrationStatus === 4 && detail.canEdit)) {
      goToRegistrationEditFlow(router, {
        registrationId,
        seasonId: detail.seasonId,
        platformCode: detail.platformCode,
      })
      return
    }
    await viewRegistrationDetail(registrationId)
  } catch {
    // ignore invalid registration id in query
  }
}

watch(
  () => route.query.registrationId,
  () => {
    if (pageLoading.value) return
    void openRegistrationFromQuery()
  },
)

async function withdrawRegistration() {
  if (!selectedRegistrationId.value) return
  try {
    await teamStore.withdrawRegistrationAction(selectedRegistrationId.value, {
      withdrawalReason: withdrawReason.value.trim() || undefined,
    })
    ElMessage.success('报名已撤回')
    closeRegistrationDetailDialog()
    if (teamId.value) await refreshRegistrationLists()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '撤回失败')
  }
}

async function deleteRegistrationDraft() {
  if (!selectedRegistrationId.value) return
  const currentId = selectedRegistrationId.value
  try {
    await ElMessageBox.confirm('确认放弃本次报名填写？', '放弃报名', { type: 'warning' })
    await teamStore.deleteRegistrationDraftAction(currentId)
    closeRegistrationDetailDialog()
    ElMessage.success('已放弃本次报名填写')
    if (teamId.value) await refreshRegistrationLists()
  } catch {
    // cancelled or failed
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
      :logo-url="hasSelectedTeam ? displayTeamLogoUrl : undefined"
      :logo-short-name="hasSelectedTeam ? displayTeamShortName : undefined"
      :logo-team-name="hasSelectedTeam ? displayTeamName : undefined"
    />

    <CaptainGrantPanel
      v-if="showCaptainGrantPanel && listScope === 'current'"
      :qualification="captainQual"
      :loading="teamStore.qualificationLoading"
    />

    <p v-if="loadError" class="team-page-error">{{ loadError }}</p>
    <p v-else-if="pageLoading" class="team-page-loading">加载中…</p>

    <template v-else>
      <nav class="team-tabs" aria-label="当前与历史球队">
        <button
          type="button"
          class="team-tabs__item"
          :class="{ 'team-tabs__item--active': listScope === 'current' }"
          @click="setListScope('current')"
        >
          当前球队
        </button>
        <button
          type="button"
          class="team-tabs__item"
          :class="{ 'team-tabs__item--active': listScope === 'history' }"
          @click="setListScope('history')"
        >
          历史球队
        </button>
      </nav>

      <AccountPanel v-if="listScope === 'current'" title="当前参赛球队">
        <div v-if="teamStore.currentLoading" class="team-page-loading">加载中…</div>
        <div v-else-if="currentTeams.length" class="team-card-list">
          <article
            v-for="team in currentTeams"
            :key="team.teamId"
            class="team-card"
            :class="{ 'team-card--active': team.teamId === teamId }"
          >
            <div>
              <p><strong>{{ team.teamName }}</strong>
                <span v-if="team.shortName"> · {{ team.shortName }}</span>
              </p>
              <p>{{ formatEventContext(team) }}</p>
              <p>
                我的身份：{{ roleLabel(team.myRole, team.isCaptain) }}
                · {{ team.displayStatusText || '当前球队' }}
              </p>
            </div>
            <div class="team-manage-actions">
              <button type="button" class="account-btn-secondary" @click="openTeamCard(team)">
                查看球队
              </button>
              <router-link
                v-if="team.divisionId"
                class="account-btn-primary"
                :to="{ path: '/account/events' }"
              >
                进入我的赛事
              </router-link>
            </div>
          </article>
        </div>
        <AccountEmptyState
          v-else
          message="当前暂无参赛球队"
          subtitle="获得赛事队长权限后可创建本届球队，也可以接受队长邀请或申请加入当前赛事球队。"
        />
      </AccountPanel>

      <AccountPanel v-else title="历史球队">
        <div v-if="teamStore.historyLoading" class="team-page-loading">加载中…</div>
        <template v-else-if="historyTeams.length">
          <div class="team-card-list">
            <article
              v-for="team in historyTeams"
              :key="team.teamId"
              class="team-card"
              :class="{ 'team-card--active': team.teamId === teamId }"
            >
              <div>
                <p><strong>{{ team.teamName }}</strong>
                  <span v-if="team.shortName"> · {{ team.shortName }}</span>
                </p>
                <p>{{ formatEventContext(team) }}</p>
                <p>
                  我的身份：{{ roleLabel(team.myRole, team.isCaptain) }}
                  · {{ team.displayStatusText || '历史球队' }}
                </p>
              </div>
              <button type="button" class="account-btn-secondary" @click="openTeamCard(team)">
                查看详情
              </button>
            </article>
          </div>
          <MyEventsPagination
            :page="teamStore.historyPage"
            :page-size="teamStore.historyPageSize"
            :total="teamStore.historyTotal"
            :page-size-options="listPageSizeOptions"
            :meta-text="`共 ${teamStore.historyTotal} 支历史球队`"
            :disabled="teamStore.historyLoading"
            aria-label="历史球队分页"
            @update:page="changeHistoryPage"
            @update:page-size="changeHistoryPageSize"
          />
        </template>
        <AccountEmptyState
          v-else
          message="暂无历史球队"
          subtitle="赛事结束或你退出球队后，相关球队记录会保留在这里。"
        />
      </AccountPanel>

      <nav v-if="availableTabs.length" class="team-tabs" aria-label="球队功能">
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
          <template v-if="teamStore.myInvitations.length">
            <div class="team-card-list">
              <article v-for="item in teamStore.myInvitations" :key="item.invitationId" class="team-card">
                <div>
                  <p><strong>球队</strong> {{ item.teamName }}</p>
                  <p><strong>赛事</strong> {{ formatEventContext(item) }}</p>
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
            <MyEventsPagination
              :page="teamStore.invitePage"
              :page-size="teamStore.invitePageSize"
              :total="teamStore.inviteTotal"
              :page-size-options="listPageSizeOptions"
              :meta-text="`共 ${teamStore.inviteTotal} 条待处理邀请`"
              :disabled="teamStore.invitationsLoading"
              aria-label="待处理邀请分页"
              @update:page="(p) => teamStore.loadMyInvitations({ currentPage: p, pageSize: teamStore.invitePageSize, status: 0 })"
              @update:page-size="(s) => teamStore.loadMyInvitations({ currentPage: 1, pageSize: s, status: 0 })"
            />
          </template>
          <AccountEmptyState v-else message="暂无待处理邀请" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'joinableTeams'">
        <JoinableTeamPanel ref="joinablePanelRef" />
      </template>

      <template v-else-if="activeTab === 'myApplications'">
        <AccountPanel title="我的加入申请">
          <template v-if="teamStore.myJoinApplications.length">
            <div class="team-table-wrap">
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
                    <td>
                      <div>{{ item.teamName }}</div>
                      <div class="team-tip">{{ formatEventContext(item) }}</div>
                    </td>
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
            <MyEventsPagination
              :page="teamStore.myApplicationPage"
              :page-size="teamStore.myApplicationPageSize"
              :total="teamStore.myApplicationTotal"
              :page-size-options="listPageSizeOptions"
              :meta-text="`共 ${teamStore.myApplicationTotal} 条加入申请`"
              :disabled="teamStore.joinApplicationsLoading"
              aria-label="我的加入申请分页"
              @update:page="(p) => teamStore.loadMyJoinApplications({ currentPage: p, pageSize: teamStore.myApplicationPageSize })"
              @update:page-size="(s) => teamStore.loadMyJoinApplications({ currentPage: 1, pageSize: s })"
            />
          </template>
          <AccountEmptyState v-else message="暂无加入申请" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'create'">
        <AccountPanel title="可创建球队的赛事">
          <p v-if="!allGrants.length" class="team-tip">
            当前暂无可用于创建球队的赛事队长权限。队长权限由管理员线下缴费后开通。
          </p>
          <div v-else class="team-card-list">
            <article
              v-for="grant in allGrants"
              :key="grant.divisionId || grant.qualificationNo"
              class="team-card"
            >
              <div>
                <p><strong>{{ grant.seriesName || grant.seasonName || '赛事' }}</strong>
                  <span v-if="grant.seasonName && grant.seriesName"> · {{ grant.seasonName }}</span>
                </p>
                <p>
                  {{ [grant.divisionName, grant.platformCode, grant.gameMode].filter(Boolean).join(' · ') || 'Division' }}
                </p>
                <p>
                  队长权限：{{ grant.canCreateTeam ? '有效' : (grant.teamId ? '已创建球队' : (grant.runtimeValid ? '有效' : '已过期/无效')) }}
                </p>
              </div>
              <button
                v-if="grant.canCreateTeam && grant.divisionId"
                type="button"
                class="account-btn-primary"
                @click="createForm.divisionId = grant.divisionId || ''"
              >
                选择并创建
              </button>
              <button
                v-else-if="grant.teamId"
                type="button"
                class="account-btn-secondary"
                @click="openTeamCard({ teamId: grant.teamId })"
              >
                查看球队
              </button>
              <AccountStatusBadge v-else text="不可创建" type="default" />
            </article>
          </div>
        </AccountPanel>

        <AccountPanel title="创建球队">
          <p v-if="canCreateTeam" class="team-tip">请基于上方已选赛事权限创建本届球队，divisionId 来自队长授权。</p>
          <p v-else class="team-tip">
            当前暂无可用于创建球队的赛事队长权限。队长权限由管理员线下缴费后开通。
          </p>
          <div class="team-create-logo">
            <AccountTeamLogo
              :logo-url="createForm.logoPreviewUrl"
              :short-name="createForm.shortName"
              :team-name="createForm.teamName"
              size="md"
            />
            <label class="account-btn-secondary team-logo-upload">
              上传 Logo
              <input type="file" accept="image/*" hidden @change="handleLogoUpload" />
            </label>
          </div>
          <div class="team-form-grid">
            <label class="team-form-full">
              所属赛事
              <select v-model="createForm.divisionId" required>
                <option value="" disabled>请选择 Division</option>
                <option
                  v-for="grant in creatableGrants"
                  :key="grant.divisionId || grant.qualificationNo"
                  :value="grant.divisionId"
                >
                  {{ [grant.seriesName || grant.seasonName, grant.platformCode, grant.gameMode].filter(Boolean).join(' · ') }}
                </option>
              </select>
            </label>
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
            :disabled="teamStore.actionLoading || !canCreateTeam || !createForm.divisionId"
            :title="canCreateTeam ? undefined : '当前暂无可创建球队的赛事权限'"
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
          <div v-if="teamDetail" class="team-overview-header">
            <AccountTeamLogo
              :logo-url="displayTeamLogoUrl"
              :short-name="teamDetail.shortName"
              :team-name="teamDetail.teamName"
              size="md"
            />
            <div class="team-overview-header__info">
              <h3 class="team-overview-header__name">{{ teamDetail.teamName }}</h3>
              <p class="team-overview-header__meta">
                <span>{{ teamDetail.teamNo || '-' }}</span>
                <span v-if="teamDetail.shortName"> · {{ teamDetail.shortName }}</span>
                <span v-if="canManageCurrentTeam"> · {{ teamAuditStatusText }}</span>
              </p>
            </div>
          </div>
          <dl v-if="teamDetail" class="account-info-list">
            <div class="account-field-row"><dt>球队编号</dt><dd>{{ teamDetail.teamNo || '-' }}</dd></div>
            <div class="account-field-row"><dt>球队名称</dt><dd>{{ teamDetail.teamName }}</dd></div>
            <div class="account-field-row"><dt>赛事</dt><dd>{{ formatEventContext(teamDetail) }}</dd></div>
            <div class="account-field-row"><dt>状态</dt><dd>{{ teamDetail.displayStatusText || (viewingHistory ? '历史球队' : '当前球队') }}</dd></div>
            <div v-if="canManageCurrentTeam" class="account-field-row"><dt>认证状态</dt><dd>{{ teamAuditStatusText }}</dd></div>
            <div class="account-field-row"><dt>简称</dt><dd>{{ teamDetail.shortName || '-' }}</dd></div>
            <div class="account-field-row"><dt>地区</dt><dd>{{ teamDetail.regionName || '-' }}</dd></div>
            <div class="account-field-row"><dt>成员</dt><dd>{{ teamDetail.currentMemberCount ?? 0 }} 人</dd></div>
            <div class="account-field-row"><dt>我的身份</dt><dd>{{ roleLabel(teamDetail.myRole, teamDetail.isCaptain) }}</dd></div>
            <div class="account-field-row"><dt>创建时间</dt><dd>{{ formatDateTime(teamDetail.createTime) }}</dd></div>
            <div class="account-field-row"><dt>介绍</dt><dd>{{ teamDetail.description || '暂无' }}</dd></div>
          </dl>
          <AccountEmptyState v-else message="球队信息加载中" />
          <div v-if="!canManageCurrentTeam && !viewingHistory" class="team-manage-actions">
            <button type="button" class="account-btn-secondary" @click="handleLeaveTeam">退出球队</button>
          </div>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'profile'">
        <TeamProfilePanel
          v-if="teamId"
          :team-id="teamId"
          :team-detail="teamDetail"
          :can-manage="canManageCurrentTeam"
          :is-approved="isTeamAuditApproved"
          @refreshed="teamStore.loadTeamDetail(teamId).catch(() => undefined)"
        />
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
          <template #extra>
            <button type="button" class="account-btn-secondary team-invite-history-btn" @click="openHistoryInviteModal">
              历史邀请
            </button>
          </template>
          <div class="team-search-row">
            <input v-model="inviteKeyword" type="search" placeholder="搜索用户账号、邮箱、球员编号或昵称" />
            <button type="button" class="account-btn-secondary" @click="searchCandidates">搜索</button>
          </div>
          <div v-if="teamStore.candidatePlayers.length" class="team-table-wrap">
            <table class="team-table team-table--invite">
              <thead>
                <tr>
                  <th>昵称</th>
                  <th>球员编号</th>
                  <th>认证状态</th>
                  <th>当前球队</th>
                  <th>平台游戏 ID</th>
                  <th class="team-table__actions-col">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="player in teamStore.candidatePlayers"
                  :key="player.playerId"
                  :class="{ 'team-table__row--disabled': player.canInvite === false }"
                >
                  <td>
                    <div class="team-invite-player-cell">
                      <PlayerAvatar
                        :name="formatCandidateNickname(player)"
                        :avatar-url="resolveInvitePlayerAvatarUrl(player.avatar)"
                        :player-no="player.playerNo"
                        :player-id="player.playerId"
                        size="sm"
                      />
                      <span>{{ formatCandidateNickname(player) }}</span>
                    </div>
                  </td>
                  <td>{{ player.playerNo || '-' }}</td>
                  <td>{{ player.certificationStatusText || formatPlayerCertStatus(player.certificationStatus) }}</td>
                  <td>{{ formatCandidateTeamStatus(player) }}</td>
                  <td>{{ formatCandidatePlatformId(player) }}</td>
                  <td class="team-table__actions-col">
                    <button
                      type="button"
                      class="account-btn-primary team-table__btn team-table__btn--invite"
                      :disabled="player.canInvite === false"
                      :title="player.cannotInviteReason || undefined"
                      @click="openInviteConfirmModal(player)"
                    >
                      邀请
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <TeamListPagination
            :page="inviteCandidatePage.pageNum"
            :page-size="inviteCandidatePage.pageSize"
            :total="inviteCandidatePage.total"
            :loading="teamStore.candidatesLoading"
            @update:page="(page) => { inviteCandidatePage.pageNum = page; loadInviteCandidateList(page) }"
          />
          <AccountEmptyState
            v-if="!teamStore.candidatesLoading && !teamStore.candidatePlayers.length"
            :message="teamStore.candidateEmptyReason || '当前暂无可邀请的已认证球员'"
          />
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
          <TeamListPagination
            :page="joinAuditPage.pageNum"
            :page-size="joinAuditPage.pageSize"
            :total="joinAuditPage.total"
            :loading="teamStore.joinApplicationsLoading"
            @update:page="(page) => { joinAuditPage.pageNum = page; loadJoinAuditList(page) }"
          />
          <AccountEmptyState v-if="!teamStore.teamJoinApplications.length" message="暂无加入申请" />
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
          <TeamListPagination
            v-if="canManageCurrentTeam"
            :page="availableSeasonPage.pageNum"
            :page-size="availableSeasonPage.pageSize"
            :total="availableSeasonPage.total"
            :loading="teamStore.registrationLoading"
            @update:page="(page) => { availableSeasonPage.pageNum = page; loadAvailableSeasonList(page) }"
          />
          <AccountEmptyState
            v-if="canManageCurrentTeam && !teamStore.availableSeasons.length"
            message="当前暂无可报名赛事"
            :subtitle="teamStore.availableSeasonEmptyReason || '请确认队长权限已关联赛事且处于报名期'"
          />
        </AccountPanel>

        <AccountPanel title="报名记录">
          <div v-if="teamStore.registrations.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>赛事名称</th>
                  <th>赛季 / 阶段</th>
                  <th>报名平台</th>
                  <th>报名状态</th>
                  <th>提交时间</th>
                  <th>审核时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in teamStore.registrations" :key="item.registrationId">
                  <td>{{ item.tournamentName || '-' }}</td>
                  <td>{{ item.seasonName || '-' }}</td>
                  <td>{{ item.platformName || item.platformCode || '-' }}</td>
                  <td>{{ displayRegistrationStatus(item.registrationStatus) }}</td>
                  <td>{{ formatDateTime(item.submittedTime || item.createTime) }}</td>
                  <td>{{ resolveRegistrationAuditTime(item) }}</td>
                  <td>
                    <div class="team-registration-actions">
                      <button
                        v-for="action in registrationListActions(item)"
                        :key="`${item.registrationId}-${action.label}`"
                        type="button"
                        :class="action.primary ? 'account-btn-primary team-table__btn' : 'account-btn-secondary team-table__btn'"
                        @click="action.handler()"
                      >
                        {{ action.label }}
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <TeamListPagination
            :page="registrationListPage.pageNum"
            :page-size="registrationListPage.pageSize"
            :total="registrationListPage.total"
            :loading="teamStore.registrationLoading"
            @update:page="(page) => { registrationListPage.pageNum = page; loadRegistrationList(page) }"
          />
          <AccountEmptyState v-if="!teamStore.registrations.length" message="暂无报名记录" />
        </AccountPanel>
      </template>
    </template>

    <Teleport to="body">
      <div v-if="showInviteConfirmDialog && inviteModalPlayer" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="closeInviteConfirmModal" />
        <div class="team-dialog__panel team-dialog__panel--invite" role="dialog" aria-modal="true">
          <h3>确认邀请球员</h3>
          <p class="team-invite-modal-tip">
            请确认球员信息无误后发送邀请。邀请发送后，球员将在消息通知中确认是否加入球队。
          </p>
          <div class="team-invite-modal-head">
            <PlayerAvatar
              :name="formatCandidateNickname(inviteModalPlayer)"
              :avatar-url="resolveInvitePlayerAvatarUrl(inviteModalPlayer.avatar)"
              :player-no="inviteModalPlayer.playerNo"
              :player-id="inviteModalPlayer.playerId"
              size="lg"
            />
            <div class="team-invite-modal-head__info">
              <strong>{{ formatCandidateNickname(inviteModalPlayer) }}</strong>
              <span>{{ inviteModalPlayer.playerNo || '-' }}</span>
            </div>
          </div>
          <dl class="account-info-list team-invite-modal-info">
            <div class="account-field-row">
              <dt>认证状态</dt>
              <dd>{{ inviteModalPlayer.certificationStatusText || formatPlayerCertStatus(inviteModalPlayer.certificationStatus) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>当前球队</dt>
              <dd>{{ formatCandidateTeamStatus(inviteModalPlayer) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>常用位置</dt>
              <dd>{{ formatInviteModalPosition(inviteModalPlayer) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>平台游戏 ID</dt>
              <dd>{{ formatCandidatePlatformId(inviteModalPlayer) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>当前是否可邀请</dt>
              <dd>{{ formatInviteModalCanInvite(inviteModalPlayer) }}</dd>
            </div>
            <div v-if="inviteModalPlayer.cannotInviteReason" class="account-field-row">
              <dt>不可邀请原因</dt>
              <dd>{{ formatInviteModalCannotReason(inviteModalPlayer) }}</dd>
            </div>
          </dl>
          <label class="team-invite-modal-message">
            邀请说明
            <textarea
              v-model="inviteModalMessage"
              rows="3"
              maxlength="200"
              placeholder="可选，向球员说明邀请原因"
            />
          </label>
          <p v-if="inviteModalError" class="team-invite-modal-error">{{ inviteModalError }}</p>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" :disabled="teamStore.actionLoading" @click="closeInviteConfirmModal">取消</button>
            <button
              type="button"
              class="account-btn-primary"
              :disabled="teamStore.actionLoading"
              @click="confirmSendInvite"
            >
              {{ teamStore.actionLoading ? '发送中…' : '确认邀请' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showHistoryInviteDialog" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="closeHistoryInviteModal" />
        <div class="team-dialog__panel team-dialog__panel--history" role="dialog" aria-modal="true" aria-labelledby="history-invite-title">
          <h3 id="history-invite-title">历史邀请</h3>
          <div class="team-history-invite-toolbar">
            <div class="team-search-row team-history-invite-search">
              <input
                v-model="historyInviteKeyword"
                type="search"
                placeholder="搜索球员昵称、邀请编号或邀请说明"
                @keyup.enter="searchHistoryInvites"
              />
              <button type="button" class="account-btn-secondary" @click="searchHistoryInvites">搜索</button>
            </div>
            <div class="team-history-invite-filters">
              <button
                v-for="option in HISTORY_INVITE_STATUS_OPTIONS"
                :key="String(option.value)"
                type="button"
                class="team-history-invite-filter"
                :class="{ 'team-history-invite-filter--active': historyInviteStatus === option.value }"
                @click="changeHistoryInviteStatus(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </div>
          <div class="team-history-invite-body">
            <p v-if="teamStore.invitationsLoading" class="team-history-invite-loading">加载中…</p>
            <div v-else-if="historyInvitePagedRecords.length" class="team-table-wrap team-history-invite-table-wrap">
              <table class="team-table team-table--history">
                <thead>
                  <tr>
                    <th>被邀请球员</th>
                    <th>邀请编号</th>
                    <th>平台游戏 ID</th>
                    <th>邀请状态</th>
                    <th>邀请时间</th>
                    <th>响应时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in historyInvitePagedRecords" :key="item.invitationId">
                    <td>
                      <div class="team-invite-player-cell">
                        <PlayerAvatar
                          :name="item.inviteeNickname || '-'"
                          :avatar-url="item.inviteeAvatarUrl"
                          :player-id="item.inviteeMemberId"
                          size="sm"
                        />
                        <span>{{ item.inviteeNickname || '-' }}</span>
                      </div>
                    </td>
                    <td>{{ item.invitationNo || '-' }}</td>
                    <td>-</td>
                    <td>
                      <AccountStatusBadge
                        :text="item.invitationStatusText || '未知'"
                        :type="item.invitationStatus === 0 ? 'warning' : 'default'"
                      />
                    </td>
                    <td>{{ formatDateTime(item.createTime) }}</td>
                    <td>-</td>
                    <td>-</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <AccountEmptyState
              v-else
              :message="historyInviteEmptyMessage"
            />
            <TeamListPagination
              v-if="!teamStore.invitationsLoading && historyInvitePage.total > historyInvitePage.pageSize"
              :page="historyInvitePage.pageNum"
              :page-size="historyInvitePage.pageSize"
              :total="historyInvitePage.total"
              :loading="teamStore.invitationsLoading"
              @update:page="changeHistoryInvitePage"
            />
          </div>
          <div class="team-dialog__actions team-dialog__actions--history">
            <button type="button" class="account-btn-secondary" @click="closeHistoryInviteModal">关闭</button>
          </div>
        </div>
      </div>
    </Teleport>

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
            <button type="button" class="account-btn-primary" @click="confirmPlatformAndCreateDraft">开始报名</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showRegistrationDetailDialog" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="closeRegistrationDetailDialog" />
        <div
          class="team-dialog__panel team-dialog__panel--registration-detail"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="'registration-detail-title'"
        >
          <h3 id="registration-detail-title">{{ registrationDetailDialogTitle }}</h3>

          <div class="team-registration-detail-body">
            <p v-if="registrationDetailLoading" class="team-registration-detail-loading">加载中…</p>

            <template v-else-if="teamStore.registrationDetail">
              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">基础信息</h4>
                <dl class="account-info-list">
                  <div class="account-field-row"><dt>报名编号</dt><dd>{{ teamStore.registrationDetail.registrationNo || '-' }}</dd></div>
                  <div class="account-field-row"><dt>赛事名称</dt><dd>{{ teamStore.registrationDetail.tournamentName || '-' }}</dd></div>
                  <div class="account-field-row"><dt>赛季 / 阶段</dt><dd>{{ teamStore.registrationDetail.seasonName || '-' }}</dd></div>
                  <div class="account-field-row"><dt>报名平台</dt><dd>{{ teamStore.registrationDetail.platformName || teamStore.registrationDetail.platformCode || '-' }}</dd></div>
                  <div class="account-field-row"><dt>报名状态</dt><dd>{{ registrationDetailStatusText }}</dd></div>
                  <div class="account-field-row"><dt>提交时间</dt><dd>{{ formatDateTime(teamStore.registrationDetail.submittedTime) || '-' }}</dd></div>
                  <div class="account-field-row"><dt>审核时间</dt><dd>{{ registrationDetailAuditTime }}</dd></div>
                  <div class="account-field-row"><dt>截止时间</dt><dd>{{ formatDateTime(teamStore.registrationDetail.registrationEndTime) || '-' }}</dd></div>
                  <div class="account-field-row"><dt>联系邮箱</dt><dd>{{ registrationCaptainEmail }}</dd></div>
                  <div v-if="teamStore.registrationDetail.remark && teamStore.registrationDetail.registrationStatus !== 4" class="account-field-row">
                    <dt>备注</dt><dd>{{ teamStore.registrationDetail.remark }}</dd>
                  </div>
                </dl>
              </section>

              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">报名阵容</h4>
                <p v-if="teamStore.registrationDetail.rosterLocked" class="team-tip team-roster-lock-tip">
                  报名已审核通过，阵容已锁定。
                </p>
                <div v-if="registrationDetailRosterRows.length" class="team-table-wrap">
                  <table class="team-table">
                    <thead>
                      <tr>
                        <th>球员</th>
                        <th>位置</th>
                        <th>平台游戏 ID</th>
                        <th>资格状态</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="row in registrationDetailRosterRows" :key="row.key">
                        <td>{{ row.name }}</td>
                        <td>{{ row.position }}</td>
                        <td>{{ row.platformId }}</td>
                        <td>{{ row.eligibility }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <AccountEmptyState v-else message="暂无报名阵容" />
              </section>

              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">审核结果</h4>
                <dl class="account-info-list">
                  <div class="account-field-row"><dt>审核状态</dt><dd>{{ registrationDetailStatusText }}</dd></div>
                  <div class="account-field-row"><dt>审核人</dt><dd>{{ registrationDetailLastAuditor }}</dd></div>
                  <div class="account-field-row"><dt>审核时间</dt><dd>{{ registrationDetailAuditTime }}</dd></div>
                  <div
                    v-if="teamStore.registrationDetail.registrationStatus === 4 && teamStore.registrationDetail.remark"
                    class="account-field-row"
                  >
                    <dt>驳回原因</dt><dd>{{ teamStore.registrationDetail.remark }}</dd>
                  </div>
                  <div
                    v-if="teamStore.registrationDetail.registrationStatus === 3"
                    class="account-field-row team-roster-lock-tip"
                  >
                    <dt>管理员备注</dt>
                    <dd>{{ teamStore.registrationAudits.at(-1)?.auditRemark || teamStore.registrationDetail.remark || '审核已通过' }}</dd>
                  </div>
                </dl>
              </section>

              <section class="team-registration-detail-section">
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
                        <td>{{ audit.auditAction || '-' }}</td>
                        <td>{{ audit.auditorName || '-' }}</td>
                        <td>{{ audit.auditRemark || '-' }}</td>
                        <td>{{ formatDateTime(audit.auditTime) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <AccountEmptyState v-else message="暂无审核记录" />
              </section>

              <label
                v-if="teamStore.registrationDetail.canWithdraw && canManageCurrentTeam"
                class="team-inline-field team-registration-detail-withdraw"
              >
                撤回原因
                <input v-model="withdrawReason" type="text" maxlength="200" placeholder="可选，填写撤回原因" />
              </label>
            </template>
          </div>

          <div class="team-dialog__actions team-dialog__actions--registration-detail">
            <button type="button" class="account-btn-secondary" @click="closeRegistrationDetailDialog">关闭</button>
            <template v-if="teamStore.registrationDetail && canManageCurrentTeam">
              <button
                v-if="teamStore.registrationDetail.registrationStatus === 0"
                type="button"
                class="account-btn-primary"
                @click="continueRegistration({
                  registrationId: teamStore.registrationDetail.registrationId,
                  seasonId: teamStore.registrationDetail.seasonId,
                  platformCode: teamStore.registrationDetail.platformCode,
                })"
              >
                继续报名
              </button>
              <button
                v-if="teamStore.registrationDetail.canDeleteDraft"
                type="button"
                class="account-btn-secondary"
                @click="deleteRegistrationDraft"
              >
                放弃本次报名
              </button>
              <button
                v-if="teamStore.registrationDetail.canWithdraw"
                type="button"
                class="account-btn-secondary"
                @click="withdrawRegistration"
              >
                撤回修改
              </button>
              <button
                v-if="teamStore.registrationDetail.registrationStatus === 4 && teamStore.registrationDetail.canEdit"
                type="button"
                class="account-btn-primary"
                @click="resubmitRegistration({
                  registrationId: teamStore.registrationDetail.registrationId,
                  seasonId: teamStore.registrationDetail.seasonId,
                  platformCode: teamStore.registrationDetail.platformCode,
                })"
              >
                修改重提
              </button>
              <button
                v-if="teamStore.registrationDetail.registrationStatus === 3"
                type="button"
                class="account-btn-primary"
                @click="manageEventTeam({
                  registrationId: teamStore.registrationDetail.registrationId,
                  seasonId: teamStore.registrationDetail.seasonId,
                  platformCode: teamStore.registrationDetail.platformCode,
                })"
              >
                管理本届球队
              </button>
            </template>
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

.team-card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.team-card--active {
  border-color: #c41e3a;
  box-shadow: 0 0 0 1px rgba(196, 30, 58, 0.25);
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
}

.team-profile-logo {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.team-profile-logo__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
}

.team-overview-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.team-overview-header__info {
  min-width: 0;
}

.team-overview-header__name {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #111;
}

.team-overview-header__meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: #696969;
  line-height: 1.5;
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
  z-index: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.55);
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

.team-dialog__panel--invite {
  width: min(600px, 100%);
}

.team-dialog__panel--registration-detail {
  width: min(75vw, 1280px);
  min-width: min(760px, 100%);
  max-height: 75vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 24px 24px 20px;
  background: #fff;
  color: #111;

  h3 {
    flex-shrink: 0;
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 800;
    color: #111;
  }

  .team-subtitle {
    color: #111;
  }
}

.team-registration-detail-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding-right: 4px;
}

.team-registration-detail-section + .team-registration-detail-section {
  margin-top: 20px;
}

.team-registration-detail-loading {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.team-registration-detail-withdraw {
  display: block;
  margin-top: 16px;
}

.team-registration-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.team-dialog__actions--registration-detail {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--account-border);
}

@media (max-width: 900px) {
  .team-dialog__panel--registration-detail {
    width: 92vw;
    min-width: auto;
  }
}

.team-dialog__panel--history {
  width: min(920px, 90vw);
  max-height: min(85vh, 760px);
  display: flex;
  flex-direction: column;
  padding: 24px 24px 20px;
  background: #fff;
  color: #111;
  border-color: rgba(0, 0, 0, 0.12);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.28);

  h3 {
    flex-shrink: 0;
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 800;
    color: #111;
  }

  .team-search-row input[type='search'] {
    color: #111;
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.18);

    &::placeholder {
      color: #9ca3af;
    }
  }

  .team-table {
    th {
      color: #333;
      font-weight: 700;
    }

    td {
      color: #222;
    }
  }

  .team-invite-player-cell span {
    color: #222;
  }

  .team-list-pagination__meta {
    color: #666;
  }
}

.team-history-invite-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 2px;
}

.team-history-invite-loading {
  margin: 0;
  padding: 28px 0;
  font-size: 14px;
  color: #666;
  text-align: center;
}

.team-dialog__actions--history {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.team-invite-history-btn {
  white-space: nowrap;
}

.team-history-invite-toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.team-history-invite-search {
  margin-bottom: 0;
}

.team-history-invite-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.team-history-invite-filter {
  padding: 6px 12px;
  border: 1px solid rgba(0, 0, 0, 0.16);
  border-radius: 3px;
  background: #fff;
  color: #333;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 0.15s ease, color 0.15s ease, background-color 0.15s ease;

  &--active {
    border-color: #c41e3a;
    background: #fff5f5;
    color: #c41e3a;
  }
}

.team-history-invite-table-wrap {
  margin-bottom: 12px;
  overflow-x: auto;
}

.team-table--history {
  min-width: 720px;
}

.team-invite-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.team-table--invite {
  .team-table__actions-col {
    width: 108px;
    min-width: 108px;
    text-align: center;
    white-space: nowrap;
  }
}

.team-table__btn--invite {
  min-width: 72px;
}

.team-invite-modal-tip {
  margin: 0 0 14px;
  font-size: 13px;
  line-height: 1.6;
  color: #696969;
}

.team-invite-player-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  span {
    min-width: 0;
    word-break: break-word;
  }
}

.team-invite-modal-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--account-border);
}

.team-invite-modal-head__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong {
    font-size: 16px;
    line-height: 1.4;
    word-break: break-word;
  }

  span {
    font-size: 13px;
    color: #696969;
  }
}

.team-invite-modal-info {
  margin-bottom: 14px;
}

.team-invite-modal-message {
  display: block;
  font-size: 13px;
  font-weight: 700;

  textarea {
    width: 100%;
    margin-top: 6px;
    padding: 10px 12px;
    border: 1px solid var(--account-border);
    border-radius: 3px;
    font-family: inherit;
    box-sizing: border-box;
    resize: vertical;
    min-height: 84px;
    max-height: 120px;
  }
}

.team-invite-modal-error {
  margin: 10px 0 0;
  font-size: 13px;
  color: #c41e3a;
  line-height: 1.5;
}

.team-invite-sent-loading {
  margin: 0 0 12px;
  font-size: 13px;
  color: #696969;
  text-align: center;
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

.team-registration-rule-summary {
  margin-bottom: 16px;
  padding: 12px 14px;
  border: 1px solid var(--account-border, #d9dee8);
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.02);

  p {
    margin: 0 0 6px;
    font-size: 13px;
    line-height: 1.5;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.team-tip--warn {
  color: #b45309;
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
