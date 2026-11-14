<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
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
import {
  goToEventTeamManagement,
  goToRegistrationEditFlow,
} from '@/utils/registrationNavigation'
import { formatDateTime } from '@/utils/formatDate'
import { uploadAccountFile } from '@/api/accountFile'
import type { TeamInvitationItemDto } from '@/api/types/teamInvitation'
import type { CandidatePlayerItemDto, TeamMemberItemDto } from '@/api/types/teamMember'
import type { AvailableSeasonItemDto, TeamRegistrationItemDto } from '@/api/types/teamRegistration'
import type { InheritableTeamSummaryDto } from '@/api/types/teamInherit'

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

const { t } = useI18n()
const KNOWN_POSITIONS = ['PG', 'SG', 'SF', 'PF', 'C'] as const

function formatEventContext(item?: {
  seriesName?: string
  seasonName?: string
  divisionName?: string
  platformCode?: string
  divisionPlatformCode?: string
  gameMode?: string
}) {
  if (!item) return t('accountTeam.list.eventMissing')
  const platform = item.platformCode || item.divisionPlatformCode
  const seriesOrSeason = [item.seriesName, item.seasonName].filter(Boolean).join(' · ')
  const divisionBits = [
    item.divisionName,
    platform ? v4PlatformLabel(platform) : '',
    item.gameMode ? v4GameModeLabel(item.gameMode) : '',
  ].filter(Boolean).join(' · ')
  const text = [seriesOrSeason, divisionBits].filter(Boolean).join(' / ')
  return text || t('accountTeam.list.eventMissing')
}

const listPageSizeOptions = [10, 20, 50]

function roleLabel(role?: string, isCaptain?: boolean) {
  if (isCaptain || role === 'CAPTAIN') return t('accountTeam.role.captain')
  if (role === 'MEMBER' || role) return t('accountTeam.role.member')
  return '-'
}

function teamAuditLabel(status?: number) {
  if (status === 0) return t('accountTeam.teamAudit.pending')
  if (status === 1) return t('accountTeam.teamAudit.approved')
  if (status === 2) return t('accountTeam.teamAudit.rejected')
  if (status === 3) return t('accountTeam.teamAudit.reviewing')
  if (status === 4) return t('accountTeam.teamAudit.draft')
  return t('accountTeam.common.unknown')
}

function formatPlayerCertStatus(status?: number) {
  if (status === 3) return t('accountTeam.playerCert.verified')
  return t('accountTeam.playerCert.unverified')
}

function formatPositionCell(code?: string, name?: string) {
  if (!code) return t('accountTeam.position.unset')
  const normalized = code.trim().toUpperCase()
  if ((KNOWN_POSITIONS as readonly string[]).includes(normalized)) {
    return `${normalized} ${t(`accountTeam.position.${normalized}`)}`
  }
  return name ? `${normalized} ${name}` : normalized
}

function registrationStatusText(status?: number | null) {
  if (status == null) return t('accountTeam.registrationStatus.unknown')
  if (status === 0) return t('accountTeam.registrationStatus.draft')
  if (status === 1 || status === 2) return t('accountTeam.registrationStatus.reviewing')
  if (status === 3) return t('accountTeam.registrationStatus.approved')
  if (status === 4) return t('accountTeam.registrationStatus.rejected')
  if (status === 5) return t('accountTeam.registrationStatus.withdrawn')
  if (status === 6) return t('accountTeam.registrationStatus.cancelled')
  return t('accountTeam.registrationStatus.draft')
}

const TEAM_TAB_PAGE_SIZE = 5

const inviteCandidatePage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })
const historyInvitePage = reactive({ pageNum: 1, pageSize: TEAM_TAB_PAGE_SIZE, total: 0 })
const historyInviteKeyword = ref('')
const historyInviteStatus = ref<number | ''>('')
const historyInviteClientMode = ref(false)
const historyInviteClientRecords = ref<TeamInvitationItemDto[]>([])
const showHistoryInviteDialog = ref(false)
const historyInviteStatusOptions = computed(() => [
  { label: t('accountTeam.invite.filter.all'), value: '' as const },
  { label: t('accountTeam.invite.filter.pending'), value: 0 },
  { label: t('accountTeam.invite.filter.accepted'), value: 1 },
  { label: t('accountTeam.invite.filter.declined'), value: 2 },
  { label: t('accountTeam.invite.filter.expired'), value: 3 },
  { label: t('accountTeam.invite.filter.revoked'), value: 4 },
])
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

watch(
  () => createForm.divisionId,
  (divisionId) => {
    if (!divisionId) {
      teamStore.inheritableTeams.splice(0, teamStore.inheritableTeams.length)
      return
    }
    teamStore.loadInheritableTeams(divisionId).catch(() => undefined)
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
  viewingHistory.value
  && Boolean(teamStore.teamDetail?.isCaptain || teamStore.myTeamSummary?.isCaptain),
)
/** 退出球队只给当前赛季里的普通队员。历史队长和已结束赛事不提供退出。 */
const showLeaveTeam = computed(() =>
  hasSelectedTeam.value
  && !viewingHistory.value
  && !canManageCurrentTeam.value
  && !isHistoricalCaptain.value,
)
const canCreateTeam = computed(() => teamStore.canCreateTeam)
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
const isTeamCertPending = computed(() => {
  const status = teamDetail.value?.auditStatus
  return status === 0 || status === 3
})
const isTeamCertRejected = computed(() => teamDetail.value?.auditStatus === 2)
const isTeamCertDraft = computed(() => teamDetail.value?.auditStatus === 4)
const teamAuditStatusText = computed(() => {
  const status = teamDetail.value?.auditStatus
  return status != null ? teamAuditLabel(status) : '-'
})
const certificationRejectRemark = computed(() =>
  teamDetail.value?.certificationRejectRemark || '',
)

const registrationCaptainEmail = computed(() =>
  accountStore.currentAccount?.maskedEmail
  || accountStore.profile?.maskedEmail
  || teamStore.registrationDetail?.contactEmailMasked
  || '-',
)

const registrationDetailDialogTitle = computed(() => {
  const detail = teamStore.registrationDetail
  if (!detail) return t('accountTeam.registration.detailTitle')
  const eventLabel = detail.tournamentName || detail.seasonName || t('accountTeam.common.event')
  const platformLabel = detail.platformName || detail.platformCode || '-'
  return t('accountTeam.registration.detailTitleWithEvent', { event: eventLabel, platform: platformLabel })
})

const registrationDetailStatusText = computed(() =>
  registrationStatusText(teamStore.registrationDetail?.registrationStatus),
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
      eligibility: t('accountTeam.registration.included'),
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
    eligibility: t('accountTeam.registration.included'),
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
  if (listScope.value === 'history') {
    return hasSelectedTeam.value ? ['overview', 'members'] : []
  }
  if (hasSelectedTeam.value && !viewingHistory.value) {
    const accountTabs: TeamTab[] = ['joinableTeams', 'create', 'pendingInvites', 'myApplications']
    if (canManageCurrentTeam.value) {
      return ['overview', 'profile', 'members', 'invitations', 'applications', 'registration', ...accountTabs]
    }
    return ['overview', 'members', 'registration', ...accountTabs]
  }
  const preTeamTabs: TeamTab[] = ['joinableTeams', 'pendingInvites', 'myApplications', 'create']
  return preTeamTabs
})

const bannerTitle = computed(() => {
  if (hasSelectedTeam.value) return selectedTeam.value?.teamName || t('accountTeam.title')
  return listScope.value === 'history' ? t('accountTeam.scope.history') : t('accountTeam.title')
})

const bannerDescription = computed(() => {
  if (hasSelectedTeam.value && canManageCurrentTeam.value) {
    if (isTeamAuditApproved.value) return t('accountTeam.banner.manageApproved')
    if (isTeamCertPending.value) return t('accountTeam.banner.certPending')
    if (isTeamCertRejected.value) return t('accountTeam.banner.certRejected')
    if (isTeamCertDraft.value) return t('accountTeam.banner.certDraft')
    return t('accountTeam.banner.certDraft')
  }
  if (hasSelectedTeam.value && viewingHistory.value) {
    return t('accountTeam.banner.historyReadonly')
  }
  if (hasSelectedTeam.value) return t('accountTeam.banner.viewCurrent')
  if (listScope.value === 'history') {
    return t('accountTeam.banner.historyHint')
  }
  if (canCreateTeam.value) return t('accountTeam.banner.canCreate')
  return t('accountTeam.banner.noTeam')
})

const bannerStatusText = computed(() => {
  if (hasSelectedTeam.value && canManageCurrentTeam.value) return t('accountTeam.role.captain')
  if (hasSelectedTeam.value && isHistoricalCaptain.value) return t('accountTeam.role.historicalCaptain')
  if (hasSelectedTeam.value && isCaptainMember.value) return t('accountTeam.role.captain')
  if (hasSelectedTeam.value) return t('accountTeam.role.member')
  if (currentTeams.value.length) return t('accountTeam.banner.currentCount', { count: currentTeams.value.length })
  if (canCreateTeam.value) return t('accountTeam.banner.canCreateStatus')
  return t('accountTeam.role.visitor')
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
  if (!tabs.includes(activeTab.value)) {
    activeTab.value = tabs[0] || 'overview'
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
    loadError.value = error instanceof Error ? error.message : t('accountTeam.common.loadFailed')
  } finally {
    pageLoading.value = false
    if (typeof route.query.tab !== 'string') {
      if (hasSelectedTeam.value && listScope.value !== 'history' && availableTabs.value.includes('members')) {
        activeTab.value = 'members'
      } else if (!hasSelectedTeam.value) {
        activeTab.value = availableTabs.value.includes('joinableTeams')
          ? 'joinableTeams'
          : (availableTabs.value[0] || 'create')
      }
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
    activeTab.value = availableTabs.value.includes('members') ? 'members' : 'overview'
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
    return t('accountTeam.invite.filteredEmpty')
  }
  return t('accountTeam.invite.historyEmpty')
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
    await Promise.all([
      loadInviteCandidateList(),
      teamStore.loadHistoricalTeammates(teamId.value),
    ])
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
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.create.logoFailed'))
  }
}

const showInheritHistory = ref(false)
const recommendedInheritTeam = computed(() => teamStore.inheritableTeams.find((team) => team.recommended))
const historicalInheritTeams = computed(() => teamStore.inheritableTeams.filter((team) => !team.recommended))

async function inheritExistingTeam(team: InheritableTeamSummaryDto) {
  if (!createForm.divisionId || !team.teamId || team.canInherit === false) return
  try {
    await ElMessageBox.confirm(
      t('accountTeam.create.inheritConfirm', { name: team.teamName || team.teamNo || '' }),
      t('accountTeam.create.inheritConfirmTitle'),
      { type: 'warning', confirmButtonText: t('accountTeam.create.inheritAction') },
    )
  } catch {
    return
  }
  try {
    await teamStore.inheritTeamAction(team.teamId, createForm.divisionId)
    ElMessage.success(t('accountTeam.create.inheritSuccess'))
    createForm.divisionId = ''
    listScope.value = 'current'
    await teamStore.loadQualification()
    await teamStore.loadCurrentTeams()
    if (teamStore.myTeamSummary?.teamId) {
      await teamStore.selectTeam(teamStore.myTeamSummary.teamId)
    }
    setTab('invitations')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.create.inheritFailed'))
  }
}

async function submitCreateTeam() {
  if (!createForm.divisionId) {
    ElMessage.warning(t('accountTeam.create.selectDivisionWarning'))
    return
  }
  if (!createForm.teamName.trim()) {
    ElMessage.warning(t('accountTeam.create.teamNameRequired'))
    return
  }
  if (!createForm.shortName.trim()) {
    ElMessage.warning(t('accountTeam.create.shortNameRequired'))
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
    ElMessage.success(t('accountTeam.create.success'))
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
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.create.failed'))
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
    ElMessage.success(t('accountTeam.members.updated'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.members.updateFailed'))
  }
}

async function removeMember(member: TeamMemberItemDto) {
  if (!teamId.value) return
  try {
    await teamStore.removeMemberAction(teamId.value, member.teamMemberId)
    ElMessage.success(t('accountTeam.members.removedDone'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.members.removeFailed'))
  }
}

async function handleLeaveTeam() {
  if (!teamId.value) return
  try {
    await ElMessageBox.confirm(t('accountTeam.overview.leaveConfirm'), t('accountTeam.overview.leaveTitle'), {
      type: 'warning',
      confirmButtonText: t('accountTeam.common.confirm'),
      cancelButtonText: t('accountTeam.common.cancel'),
    })
    await teamStore.leaveTeamAction(teamId.value)
    ElMessage.success(t('accountTeam.overview.left'))
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
    const result = await teamStore.respondInvitationAction(activeInvitation.value.invitationId, accept)
    showInvitationDialog.value = false
    const awaitingAudit = accept && result?.invitationStatus === 5
    ElMessage.success(accept
      ? t(awaitingAudit ? 'accountTeam.invite.awaitingAudit' : 'accountTeam.invite.joinedRoster')
      : t('accountTeam.invite.declined'))
    await bootstrap()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.invite.actionFailed'))
  }
}

async function cancelApplication(applicationId: string) {
  try {
    await teamStore.cancelJoinApplicationAction(applicationId)
    ElMessage.success(t('accountTeam.application.cancelled'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.application.cancelFailed'))
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
    ElMessage.success(t('accountTeam.invite.sent'))
    closeInviteConfirmModal()
    const refreshTasks = [
      loadInviteCandidateList(inviteCandidatePage.pageNum),
      teamStore.loadHistoricalTeammates(teamId.value),
    ]
    if (showHistoryInviteDialog.value) {
      historyInvitePage.pageNum = 1
      refreshTasks.push(loadHistoryInviteList(1))
    }
    await Promise.all(refreshTasks)
  } catch (error) {
    inviteModalError.value = error instanceof Error ? error.message : t('accountTeam.invite.failed')
  }
}

async function searchCandidates() {
  if (!teamId.value) return
  inviteCandidatePage.pageNum = 1
  await loadInviteCandidateList(1)
}

function formatInviteModalCanInvite(player: CandidatePlayerItemDto) {
  return player.canInvite !== false ? t('accountTeam.common.yes') : t('accountTeam.common.no')
}

function formatInviteModalCannotReason(player: CandidatePlayerItemDto) {
  return player.cannotInviteReason || '-'
}

function formatInviteModalPosition(player: CandidatePlayerItemDto) {
  return formatPositionCell(player.positionCode, player.positionName)
}

function formatCandidateTeamStatus(player: CandidatePlayerItemDto) {
  return player.currentTeamStatusText || (player.currentTeamId ? t('accountTeam.invite.joinedTeam') : t('accountTeam.invite.notJoinedTeam'))
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
    ElMessage.success(approve ? t('accountTeam.application.approved') : t('accountTeam.application.rejected'))
    await loadJoinAuditList(joinAuditPage.pageNum)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.application.processFailed'))
  }
}

function startRegistrationForSeason(season: AvailableSeasonItemDto) {
  if (season.canRegister === false) {
    ElMessage.warning(season.cannotRegisterReason || t('accountTeam.registration.createFailed'))
    return
  }
  const divisionId = String(season.seasonId || '').trim()
  if (!divisionId) return
  router.push({
    name: 'AccountV4Registration',
    params: { divisionId },
    query: season.platformCode ? { platformCode: season.platformCode } : undefined,
  })
}

async function confirmPlatformAndCreateDraft() {
  if (!teamId.value || !pendingSeasonId.value || !selectedPlatformCode.value) {
    ElMessage.warning(t('accountTeam.registration.selectPlatform'))
    return
  }
  registrationDraftForm.seasonId = pendingSeasonId.value
  registrationDraftForm.platformCode = selectedPlatformCode.value
  showPlatformPicker.value = false
  await createRegistrationDraft()
}

async function createRegistrationDraft() {
  if (!teamId.value || !registrationDraftForm.seasonId) {
    ElMessage.warning(t('accountTeam.registration.selectSeason'))
    return
  }
  try {
    const detail = await teamStore.createRegistrationDraftAction(teamId.value, {
      seasonId: registrationDraftForm.seasonId,
      platformCode: registrationDraftForm.platformCode || undefined,
      remark: registrationDraftForm.remark.trim() || undefined,
    })
    ElMessage.success(t('accountTeam.registration.saved'))
    await refreshRegistrationLists()
    goToRegistrationEditFlow(router, {
      registrationId: detail.registrationId,
      seasonId: detail.seasonId || registrationDraftForm.seasonId,
      platformCode: detail.platformCode || registrationDraftForm.platformCode,
    })
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.registration.createFailed'))
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
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.registration.detailFailed'))
    closeRegistrationDetailDialog()
  } finally {
    registrationDetailLoading.value = false
  }
}

function displayRegistrationStatus(status?: number) {
  return registrationStatusText(status)
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
      actions.push({ label: t('accountTeam.registration.continue'), handler: () => continueRegistration(item), primary: true })
      actions.push({ label: t('accountTeam.registration.abandon'), handler: () => abandonRegistrationDraft(item) })
    } else {
      actions.push({ label: t('accountTeam.registration.viewDetail'), handler: () => viewRegistrationDetail(item.registrationId) })
    }
    return actions
  }

  if (status === 1 || status === 2) {
    actions.push({ label: t('accountTeam.registration.viewDetail'), handler: () => viewRegistrationDetail(item.registrationId) })
    if (item.canWithdraw && canManageCurrentTeam.value) {
      actions.push({ label: t('accountTeam.registration.withdrawEdit'), handler: () => viewRegistrationDetail(item.registrationId) })
    }
    return actions
  }

  if (status === 3) {
    actions.push({ label: t('accountTeam.registration.viewDetail'), handler: () => viewRegistrationDetail(item.registrationId) })
    if (canManageCurrentTeam.value) {
      actions.push({ label: t('accountTeam.registration.manageEventTeam'), handler: () => manageEventTeam(item), primary: true })
    }
    return actions
  }

  if (status === 4) {
    actions.push({ label: t('accountTeam.registration.viewDetail'), handler: () => viewRegistrationDetail(item.registrationId) })
    if (canManageCurrentTeam.value && item.canEdit !== false) {
      actions.push({ label: t('accountTeam.registration.resubmit'), handler: () => resubmitRegistration(item), primary: true })
    }
    return actions
  }

  actions.push({ label: t('accountTeam.registration.viewDetail'), handler: () => viewRegistrationDetail(item.registrationId) })
  return actions
}

function formatCandidateEligibility(candidate: import('@/api/types/teamRegistration').RegistrationRosterCandidateDto) {
  if (candidate.canSelect) {
    return t('accountTeam.registration.includedSignup')
  }
  return candidate.cannotSelectReason || candidate.platformIdentityStatusText || t('accountTeam.registration.cannotInclude')
}

function formatCandidatePlatformDisplay(candidate: import('@/api/types/teamRegistration').RegistrationRosterCandidateDto) {
  if (candidate.platformGameId && candidate.canSelect) {
    const platform = teamStore.registrationDetail?.platformName || teamStore.registrationDetail?.platformCode || ''
    return platform ? t('accountTeam.common.platformId', { platform, id: candidate.platformGameId }) : candidate.platformGameId
  }
  return candidate.platformIdentityStatusText || candidate.platformGameId || '-'
}

async function abandonRegistrationDraft(item: TeamRegistrationItemDto) {
  try {
    await ElMessageBox.confirm(t('accountTeam.registration.abandonConfirm'), t('accountTeam.registration.abandonTitle'), {
      type: 'warning',
      confirmButtonText: t('accountTeam.common.confirm'),
      cancelButtonText: t('accountTeam.common.cancel'),
    })
    await teamStore.deleteRegistrationDraftAction(item.registrationId)
    if (selectedRegistrationId.value === item.registrationId) {
      closeRegistrationDetailDialog()
    }
    ElMessage.success(t('accountTeam.registration.abandoned'))
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
    ElMessage.success(t('accountTeam.registration.withdrawn'))
    closeRegistrationDetailDialog()
    if (teamId.value) await refreshRegistrationLists()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.registration.withdrawFailed'))
  }
}

async function deleteRegistrationDraft() {
  if (!selectedRegistrationId.value) return
  const currentId = selectedRegistrationId.value
  try {
    await ElMessageBox.confirm(t('accountTeam.registration.abandonConfirm'), t('accountTeam.registration.abandonTitle'), {
      type: 'warning',
      confirmButtonText: t('accountTeam.common.confirm'),
      cancelButtonText: t('accountTeam.common.cancel'),
    })
    await teamStore.deleteRegistrationDraftAction(currentId)
    closeRegistrationDetailDialog()
    ElMessage.success(t('accountTeam.registration.abandoned'))
    if (teamId.value) await refreshRegistrationLists()
  } catch {
    // cancelled or failed
  }
}

const tabLabels = computed<Record<TeamTab, string>>(() => ({
  pendingInvites: t('accountTeam.tabs.pendingInvites'),
  joinableTeams: t('accountTeam.tabs.joinableTeams'),
  myApplications: t('accountTeam.tabs.myApplications'),
  create: t('accountTeam.tabs.create'),
  overview: t('accountTeam.tabs.overview'),
  profile: t('accountTeam.tabs.profile'),
  members: canManageCurrentTeam.value ? t('accountTeam.tabs.membersManage') : (isHistoricalCaptain.value ? t('accountTeam.tabs.membersHistory') : t('accountTeam.tabs.membersList')),
  invitations: t('accountTeam.tabs.invitations'),
  applications: t('accountTeam.tabs.applications'),
  registration: canManageCurrentTeam.value ? t('accountTeam.tabs.registrationManage') : (isHistoricalCaptain.value ? t('accountTeam.tabs.registrationHistory') : t('accountTeam.tabs.registrationRecord')),
}))
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountTeam.title')"
      :en-title="t('accountTeam.titleEn')"
      :breadcrumb-items="[
        { label: t('accountTeam.breadcrumb.home'), to: '/home' },
        { label: t('accountTeam.breadcrumb.account'), to: '/account/overview' },
        { label: t('accountTeam.breadcrumb.team') },
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
    <p v-else-if="pageLoading" class="team-page-loading">{{ t('accountTeam.common.loading') }}</p>

    <template v-else>
      <nav class="team-tabs" :aria-label="t('accountTeam.scope.aria')">
        <button
          type="button"
          class="team-tabs__item"
          :class="{ 'team-tabs__item--active': listScope === 'current' }"
          @click="setListScope('current')"
        >
          {{ t('accountTeam.scope.current') }}
        </button>
        <button
          type="button"
          class="team-tabs__item"
          :class="{ 'team-tabs__item--active': listScope === 'history' }"
          @click="setListScope('history')"
        >
          {{ t('accountTeam.scope.history') }}
        </button>
      </nav>

      <AccountPanel v-if="listScope === 'current'" :title="t('accountTeam.list.currentTitle')">
        <div v-if="teamStore.currentLoading" class="team-page-loading">{{ t('accountTeam.common.loading') }}</div>
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
                {{ t('accountTeam.list.myRole', { role: roleLabel(team.myRole, team.isCaptain) }) }}
                · {{ team.displayStatusText || t('accountTeam.list.currentFallback') }}
              </p>
            </div>
            <div class="team-manage-actions">
              <button type="button" class="account-btn-secondary" @click="openTeamCard(team)">
                {{ t('accountTeam.list.viewTeam') }}
              </button>
              <router-link
                v-if="team.divisionId"
                class="account-btn-primary"
                :to="{ path: '/account/events' }"
              >
                {{ t('accountTeam.list.enterEvents') }}
              </router-link>
            </div>
          </article>
        </div>
        <AccountEmptyState
          v-else
          :message="t('accountTeam.list.currentEmpty')"
          :subtitle="t('accountTeam.list.currentEmptyHint')"
        />
      </AccountPanel>

      <AccountPanel v-else :title="t('accountTeam.list.historyTitle')">
        <div v-if="teamStore.historyLoading" class="team-page-loading">{{ t('accountTeam.common.loading') }}</div>
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
                  {{ t('accountTeam.list.myRole', { role: roleLabel(team.myRole, team.isCaptain) }) }}
                  · {{ team.displayStatusText || t('accountTeam.list.historyFallback') }}
                </p>
              </div>
              <button type="button" class="account-btn-secondary" @click="openTeamCard(team)">
                {{ t('accountTeam.list.viewDetail') }}
              </button>
            </article>
          </div>
          <MyEventsPagination
            :page="teamStore.historyPage"
            :page-size="teamStore.historyPageSize"
            :total="teamStore.historyTotal"
            :page-size-options="listPageSizeOptions"
            :meta-text="t('accountTeam.list.historyMeta', { total: teamStore.historyTotal })"
            :disabled="teamStore.historyLoading"
            :aria-label="t('accountTeam.list.historyAria')"
            @update:page="changeHistoryPage"
            @update:page-size="changeHistoryPageSize"
          />
        </template>
        <AccountEmptyState
          v-else
          :message="t('accountTeam.list.historyEmpty')"
          :subtitle="t('accountTeam.list.historyEmptyHint')"
        />
      </AccountPanel>

      <nav v-if="availableTabs.length" class="team-tabs" :aria-label="t('accountTeam.tabs.aria')">
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

      <template v-if="availableTabs.includes(activeTab)">
      <template v-if="activeTab === 'pendingInvites'">
        <AccountPanel :title="t('accountTeam.tabs.pendingInvites')">
          <template v-if="teamStore.myInvitations.length">
            <div class="team-card-list">
              <article v-for="item in teamStore.myInvitations" :key="item.invitationId" class="team-card">
                <div>
                  <p><strong>{{ t('accountTeam.invite.team') }}</strong> {{ item.teamName }}</p>
                  <p><strong>{{ t('accountTeam.invite.event') }}</strong> {{ formatEventContext(item) }}</p>
                  <p><strong>{{ t('accountTeam.invite.note') }}</strong> {{ item.invitationMessage || t('accountTeam.invite.defaultMessage') }}</p>
                  <p><strong>{{ t('accountTeam.invite.expireTime') }}</strong> {{ formatDateTime(item.expireTime) }}</p>
                  <AccountStatusBadge :text="item.invitationStatusText || t('accountTeam.invite.pending')" type="warning" />
                </div>
                <button
                  v-if="item.canRespond"
                  type="button"
                  class="account-btn-primary"
                  @click="openInvitation(item)"
                >
                  {{ t('accountTeam.invite.handle') }}
                </button>
              </article>
            </div>
            <MyEventsPagination
              :page="teamStore.invitePage"
              :page-size="teamStore.invitePageSize"
              :total="teamStore.inviteTotal"
              :page-size-options="listPageSizeOptions"
              :meta-text="t('accountTeam.invite.meta', { total: teamStore.inviteTotal })"
              :disabled="teamStore.invitationsLoading"
              :aria-label="t('accountTeam.invite.aria')"
              @update:page="(p) => teamStore.loadMyInvitations({ currentPage: p, pageSize: teamStore.invitePageSize, status: 0 })"
              @update:page-size="(s) => teamStore.loadMyInvitations({ currentPage: 1, pageSize: s, status: 0 })"
            />
          </template>
          <AccountEmptyState v-else :message="t('accountTeam.invite.empty')" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'joinableTeams'">
        <JoinableTeamPanel ref="joinablePanelRef" />
      </template>

      <template v-else-if="activeTab === 'myApplications'">
        <AccountPanel :title="t('accountTeam.application.title')">
          <template v-if="teamStore.myJoinApplications.length">
            <div class="team-table-wrap">
              <table class="team-table">
                <thead>
                  <tr>
                    <th>{{ t('accountTeam.application.team') }}</th>
                    <th>{{ t('accountTeam.application.status') }}</th>
                    <th>{{ t('accountTeam.application.time') }}</th>
                    <th>{{ t('accountTeam.application.actions') }}</th>
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
                        {{ t('accountTeam.application.cancel') }}
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
              :meta-text="t('accountTeam.application.meta', { total: teamStore.myApplicationTotal })"
              :disabled="teamStore.joinApplicationsLoading"
              :aria-label="t('accountTeam.application.aria')"
              @update:page="(p) => teamStore.loadMyJoinApplications({ currentPage: p, pageSize: teamStore.myApplicationPageSize })"
              @update:page-size="(s) => teamStore.loadMyJoinApplications({ currentPage: 1, pageSize: s })"
            />
          </template>
          <AccountEmptyState v-else :message="t('accountTeam.application.empty')" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'create'">
        <AccountPanel :title="t('accountTeam.create.grantsTitle')">
          <p v-if="!allGrants.length" class="team-tip">
            {{ t('accountTeam.create.noGrant') }}
          </p>
          <div v-else class="team-card-list">
            <article
              v-for="grant in allGrants"
              :key="grant.divisionId || grant.qualificationNo"
              class="team-card"
            >
              <div>
                <p><strong>{{ grant.seriesName || grant.seasonName || t('accountTeam.common.event') }}</strong>
                  <span v-if="grant.seasonName && grant.seriesName"> · {{ grant.seasonName }}</span>
                </p>
                <p>
                  {{ [grant.divisionName, grant.platformCode, grant.gameMode].filter(Boolean).join(' · ') || 'Division' }}
                </p>
                <p>
                  {{ t('accountTeam.create.grantLabel', { status: grant.canCreateTeam ? t('accountTeam.create.grantValid') : (grant.teamId ? t('accountTeam.create.grantHasTeam') : (grant.runtimeValid ? t('accountTeam.create.grantValid') : t('accountTeam.create.grantExpired'))) }) }}
                </p>
              </div>
              <button
                v-if="grant.canCreateTeam && grant.divisionId"
                type="button"
                class="account-btn-primary"
                @click="createForm.divisionId = grant.divisionId || ''"
              >
                {{ t('accountTeam.create.selectAndCreate') }}
              </button>
              <button
                v-else-if="grant.teamId"
                type="button"
                class="account-btn-secondary"
                @click="openTeamCard({ teamId: grant.teamId })"
              >
                {{ t('accountTeam.list.viewTeam') }}
              </button>
              <AccountStatusBadge v-else :text="t('accountTeam.create.cannotCreate')" type="default" />
            </article>
          </div>
        </AccountPanel>

        <AccountPanel v-if="createForm.divisionId" :title="t('accountTeam.create.inheritTitle')">
          <p class="team-tip">{{ t('accountTeam.create.inheritHint') }}</p>
          <p v-if="teamStore.inheritableLoading" class="team-tip">{{ t('accountTeam.common.loading') }}</p>
          <p v-else-if="!teamStore.inheritableTeams.length" class="team-tip">
            {{ t('accountTeam.create.inheritEmpty') }}
          </p>
          <div v-else>
            <p v-if="recommendedInheritTeam" class="team-tip">{{ t('accountTeam.create.inheritRecommended') }}</p>
            <div v-if="recommendedInheritTeam" class="team-card-list">
            <article class="team-card">
              <div class="team-inherit-main">
                <AccountTeamLogo
                  :logo-url="recommendedInheritTeam.logoUrl"
                  :short-name="recommendedInheritTeam.shortName"
                  :team-name="recommendedInheritTeam.teamName"
                  size="sm"
                />
                <div>
                  <p><strong>{{ recommendedInheritTeam.teamName }}</strong> <span v-if="recommendedInheritTeam.teamNo">{{ recommendedInheritTeam.teamNo }}</span></p>
                  <p>{{ recommendedInheritTeam.previousEventName || t('accountTeam.common.event') }}</p>
                  <p>
                    {{ t('accountTeam.create.inheritMembers', { count: recommendedInheritTeam.currentMemberCount ?? 0 }) }}
                    · {{ t('accountTeam.create.inheritHonors', { count: recommendedInheritTeam.honorCount ?? 0 }) }}
                  </p>
                  <p v-if="recommendedInheritTeam.cannotInheritReason" class="team-inherit-reason">{{ recommendedInheritTeam.cannotInheritReason }}</p>
                </div>
              </div>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="teamStore.actionLoading || recommendedInheritTeam.canInherit === false"
                @click="inheritExistingTeam(recommendedInheritTeam)"
              >
                {{ t('accountTeam.create.inheritAction') }}
              </button>
            </article>
            </div>
            <div v-if="historicalInheritTeams.length" class="team-inherit-history">
              <button type="button" class="account-btn-secondary" @click="showInheritHistory = !showInheritHistory">
                {{ showInheritHistory ? t('accountTeam.create.inheritHideHistory') : t('accountTeam.create.inheritShowHistory') }}
              </button>
              <p v-if="showInheritHistory || !recommendedInheritTeam" class="team-tip">{{ t('accountTeam.create.inheritHistoryHint') }}</p>
            <div v-if="showInheritHistory || !recommendedInheritTeam" class="team-card-list">
            <article
              v-for="team in historicalInheritTeams"
              :key="team.teamId"
              class="team-card"
            >
              <div class="team-inherit-main">
                <AccountTeamLogo
                  :logo-url="team.logoUrl"
                  :short-name="team.shortName"
                  :team-name="team.teamName"
                  size="sm"
                />
                <div>
                  <p><strong>{{ team.teamName }}</strong> <span v-if="team.teamNo">{{ team.teamNo }}</span></p>
                  <p>{{ team.previousEventName || t('accountTeam.common.event') }}</p>
                  <p>
                    {{ t('accountTeam.create.inheritMembers', { count: team.currentMemberCount ?? 0 }) }}
                    · {{ t('accountTeam.create.inheritHonors', { count: team.honorCount ?? 0 }) }}
                  </p>
                  <p v-if="team.cannotInheritReason" class="team-inherit-reason">{{ team.cannotInheritReason }}</p>
                </div>
              </div>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="teamStore.actionLoading || team.canInherit === false"
                @click="inheritExistingTeam(team)"
              >
                {{ t('accountTeam.create.inheritAction') }}
              </button>
            </article>
            </div>
            </div>
          </div>
        </AccountPanel>

        <AccountPanel :title="t('accountTeam.create.title')">
          <p v-if="canCreateTeam" class="team-tip">{{ t('accountTeam.create.hint') }}</p>
          <p v-else class="team-tip">
            {{ t('accountTeam.create.noGrant') }}
          </p>
          <div class="team-create-logo">
            <AccountTeamLogo
              :logo-url="createForm.logoPreviewUrl"
              :short-name="createForm.shortName"
              :team-name="createForm.teamName"
              size="md"
            />
            <label class="account-btn-secondary team-logo-upload">
              {{ t('accountTeam.create.uploadLogo') }}
              <input type="file" accept="image/*" hidden @change="handleLogoUpload" />
            </label>
          </div>
          <div class="team-form-grid">
            <label class="team-form-full">
              {{ t('accountTeam.create.event') }}
              <select v-model="createForm.divisionId" required>
                <option value="" disabled>{{ t('accountTeam.create.selectDivision') }}</option>
                <option
                  v-for="grant in creatableGrants"
                  :key="grant.divisionId || grant.qualificationNo"
                  :value="grant.divisionId"
                >
                  {{ [grant.seriesName || grant.seasonName, grant.platformCode, grant.gameMode].filter(Boolean).join(' · ') }}
                </option>
              </select>
            </label>
            <label>{{ t('accountTeam.create.teamName') }}<input v-model="createForm.teamName" type="text" maxlength="32" required /></label>
            <label>{{ t('accountTeam.create.shortName') }}<input v-model="createForm.shortName" type="text" maxlength="16" required /></label>
            <label>
              {{ t('accountTeam.create.recruitStatus') }}
              <select v-model.number="createForm.publicStatus">
                <option :value="1">{{ t('accountTeam.create.recruiting') }}</option>
                <option :value="0">{{ t('accountTeam.create.notRecruiting') }}</option>
              </select>
            </label>
            <label class="team-form-full">{{ t('accountTeam.create.intro') }}<textarea v-model="createForm.description" rows="3" maxlength="500" :placeholder="t('accountTeam.create.introPlaceholder')" /></label>
          </div>
          <button
            type="button"
            class="account-btn-primary"
            :disabled="teamStore.actionLoading || !canCreateTeam || !createForm.divisionId"
            :title="canCreateTeam ? undefined : t('accountTeam.create.disabledTitle')"
            @click="submitCreateTeam"
          >
            {{ t('accountTeam.create.submit') }}
          </button>
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'overview'">
        <AccountPanel :title="t('accountTeam.overview.title')">
          <div
            v-if="isHistoricalCaptain"
            class="team-audit-banner"
          >
            <AccountStatusBadge :text="t('accountTeam.role.historicalCaptain')" type="warning" />
            <p class="team-tip">
              {{ captainQual?.cannotManageReason || t('accountTeam.overview.noGrant') }}
            </p>
          </div>
          <div
            v-else-if="canManageCurrentTeam && !isTeamAuditApproved"
            class="team-audit-banner"
          >
            <AccountStatusBadge
              :text="isTeamCertRejected ? t('accountTeam.overview.certRejectedBadge') : (isTeamCertPending ? t('accountTeam.overview.certPendingBadge') : teamAuditStatusText)"
              :type="isTeamCertRejected ? 'danger' : 'warning'"
            />
            <p v-if="isTeamCertPending" class="team-tip">
              {{ t('accountTeam.overview.certPending') }}
            </p>
            <p v-else-if="isTeamCertRejected" class="team-tip">
              {{ certificationRejectRemark
                ? t('accountTeam.overview.certRejectedWithRemark', { remark: certificationRejectRemark })
                : t('accountTeam.overview.certRejected') }}
            </p>
            <p v-else class="team-tip">
              {{ t('accountTeam.overview.certDraft') }}
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
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.teamNo') }}</dt><dd>{{ teamDetail.teamNo || '-' }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.teamName') }}</dt><dd>{{ teamDetail.teamName }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.event') }}</dt><dd>{{ formatEventContext(teamDetail) }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.status') }}</dt><dd>{{ teamDetail.displayStatusText || (viewingHistory ? t('accountTeam.list.historyFallback') : t('accountTeam.list.currentFallback')) }}</dd></div>
            <div v-if="canManageCurrentTeam" class="account-field-row"><dt>{{ t('accountTeam.overview.certStatus') }}</dt><dd>{{ teamAuditStatusText }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.shortName') }}</dt><dd>{{ teamDetail.shortName || '-' }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.region') }}</dt><dd>{{ teamDetail.regionName || '-' }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.members') }}</dt><dd>{{ t('accountTeam.common.people', { count: teamDetail.currentMemberCount ?? 0 }) }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.myRole') }}</dt><dd>{{ roleLabel(teamDetail.myRole, teamDetail.isCaptain) }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.createdAt') }}</dt><dd>{{ formatDateTime(teamDetail.createTime) }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.overview.intro') }}</dt><dd>{{ teamDetail.description || t('accountTeam.common.empty') }}</dd></div>
          </dl>
          <AccountEmptyState v-else :message="t('accountTeam.overview.loading')" />
          <div v-if="showLeaveTeam" class="team-manage-actions">
            <button type="button" class="account-btn-secondary" @click="handleLeaveTeam">{{ t('accountTeam.overview.leave') }}</button>
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
        <AccountPanel :title="t('accountTeam.invite.historicalTitle')">
          <p class="team-tip">{{ t('accountTeam.invite.historicalHint') }}</p>
          <p v-if="teamStore.historicalTeammatesLoading" class="team-tip">{{ t('accountTeam.common.loading') }}</p>
          <div v-else-if="teamStore.historicalTeammates.length" class="team-table-wrap">
            <table class="team-table team-table--invite">
              <thead>
                <tr>
                  <th>{{ t('accountTeam.invite.nickname') }}</th>
                  <th>{{ t('accountTeam.invite.playerNo') }}</th>
                  <th>{{ t('accountTeam.invite.certStatus') }}</th>
                  <th>{{ t('accountTeam.invite.currentTeam') }}</th>
                  <th>{{ t('accountTeam.invite.platformGameId') }}</th>
                  <th class="team-table__actions-col">{{ t('accountTeam.common.actions') }}</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="player in teamStore.historicalTeammates"
                  :key="player.memberId || player.playerId"
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
                  <td>{{ player.currentTeamStatusText || t('accountTeam.invite.notJoinedThisEvent') }}</td>
                  <td>{{ formatCandidatePlatformId(player) }}</td>
                  <td class="team-table__actions-col">
                    <button
                      type="button"
                      class="account-btn-primary team-table__btn team-table__btn--invite"
                      :disabled="player.canInvite === false"
                      :title="player.cannotInviteReason || undefined"
                      @click="openInviteConfirmModal(player)"
                    >
                      {{ t('accountTeam.invite.invite') }}
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AccountEmptyState
            v-else-if="!teamStore.historicalTeammatesLoading"
            :message="t('accountTeam.invite.historicalEmpty')"
          />
        </AccountPanel>
        <AccountPanel :title="t('accountTeam.invite.candidatesTitle')">
          <template #extra>
            <button type="button" class="account-btn-secondary team-invite-history-btn" @click="openHistoryInviteModal">
              {{ t('accountTeam.invite.history') }}
            </button>
          </template>
          <div class="team-search-row">
            <input v-model="inviteKeyword" type="search" :placeholder="t('accountTeam.invite.searchPlaceholder')" />
            <button type="button" class="account-btn-secondary" @click="searchCandidates">{{ t('accountTeam.common.search') }}</button>
          </div>
          <div v-if="teamStore.candidatePlayers.length" class="team-table-wrap">
            <table class="team-table team-table--invite">
              <thead>
                <tr>
                  <th>{{ t('accountTeam.invite.nickname') }}</th>
                  <th>{{ t('accountTeam.invite.playerNo') }}</th>
                  <th>{{ t('accountTeam.invite.certStatus') }}</th>
                  <th>{{ t('accountTeam.invite.currentTeam') }}</th>
                  <th>{{ t('accountTeam.invite.platformGameId') }}</th>
                  <th class="team-table__actions-col">{{ t('accountTeam.common.actions') }}</th>
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
                      {{ t('accountTeam.invite.invite') }}
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
            :message="teamStore.candidateEmptyReason || t('accountTeam.invite.emptyCandidates')"
          />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'applications'">
        <AccountPanel :title="t('accountTeam.application.pendingTitle')">
          <div v-if="teamStore.teamJoinApplications.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>{{ t('accountTeam.invite.nickname') }}</th>
                  <th>{{ t('accountTeam.invite.playerNo') }}</th>
                  <th>{{ t('accountTeam.application.position') }}</th>
                  <th>{{ t('accountTeam.invite.certStatus') }}</th>
                  <th>{{ t('accountTeam.application.reason') }}</th>
                  <th>{{ t('accountTeam.application.time') }}</th>
                  <th>{{ t('accountTeam.common.actions') }}</th>
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
                      {{ item.positionCode || t('accountTeam.position.unset') }}
                    </span>
                  </td>
                  <td>{{ formatPlayerCertStatus(item.certificationStatus) }}</td>
                  <td>{{ item.applyReason || t('accountTeam.common.none') }}</td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td v-if="item.canProcess" class="team-card-actions">
                    <button type="button" class="account-btn-primary team-table__btn" @click="processApplication(item.applicationId, true)">{{ t('accountTeam.application.approve') }}</button>
                    <button type="button" class="account-btn-secondary team-table__btn" @click="processApplication(item.applicationId, false)">{{ t('accountTeam.application.reject') }}</button>
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
          <AccountEmptyState v-if="!teamStore.teamJoinApplications.length" :message="t('accountTeam.application.empty')" />
        </AccountPanel>
      </template>

      <template v-else-if="activeTab === 'registration'">
        <AccountPanel :title="canManageCurrentTeam ? t('accountTeam.registration.availableTitle') : t('accountTeam.registration.recordTitle')">
          <div v-if="canManageCurrentTeam && teamStore.availableSeasons.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>{{ t('accountTeam.registration.event') }}</th>
                  <th>{{ t('accountTeam.registration.season') }}</th>
                  <th>{{ t('accountTeam.registration.platform') }}</th>
                  <th>{{ t('accountTeam.registration.deadline') }}</th>
                  <th>{{ t('accountTeam.common.actions') }}</th>
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
                      {{ t('accountTeam.registration.register') }}
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
            :message="t('accountTeam.registration.empty')"
            :subtitle="teamStore.availableSeasonEmptyReason || t('accountTeam.registration.emptyHint')"
          />
        </AccountPanel>

        <AccountPanel :title="t('accountTeam.registration.recordsTitle')">
          <div v-if="teamStore.registrations.length" class="team-table-wrap">
            <table class="team-table">
              <thead>
                <tr>
                  <th>{{ t('accountTeam.registration.eventName') }}</th>
                  <th>{{ t('accountTeam.registration.seasonStage') }}</th>
                  <th>{{ t('accountTeam.registration.signupPlatform') }}</th>
                  <th>{{ t('accountTeam.registration.signupStatus') }}</th>
                  <th>{{ t('accountTeam.registration.submittedAt') }}</th>
                  <th>{{ t('accountTeam.registration.auditedAt') }}</th>
                  <th>{{ t('accountTeam.common.actions') }}</th>
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
          <AccountEmptyState v-if="!teamStore.registrations.length" :message="t('accountTeam.registration.recordsEmpty')" />
        </AccountPanel>
      </template>
      </template>
    </template>

    <Teleport to="body">
      <div v-if="showInviteConfirmDialog && inviteModalPlayer" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" :aria-label="t('accountTeam.common.close')" @click="closeInviteConfirmModal" />
        <div class="team-dialog__panel team-dialog__panel--invite" role="dialog" aria-modal="true">
          <h3>{{ t('accountTeam.invite.confirmTitle') }}</h3>
          <p class="team-invite-modal-tip">
            {{ t('accountTeam.invite.confirmTip') }}
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
              <dt>{{ t('accountTeam.overview.certStatus') }}</dt>
              <dd>{{ inviteModalPlayer.certificationStatusText || formatPlayerCertStatus(inviteModalPlayer.certificationStatus) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountTeam.invite.currentTeam') }}</dt>
              <dd>{{ formatCandidateTeamStatus(inviteModalPlayer) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountTeam.invite.usualPosition') }}</dt>
              <dd>{{ formatInviteModalPosition(inviteModalPlayer) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountTeam.invite.platformGameId') }}</dt>
              <dd>{{ formatCandidatePlatformId(inviteModalPlayer) }}</dd>
            </div>
            <div class="account-field-row">
              <dt>{{ t('accountTeam.invite.canInvite') }}</dt>
              <dd>{{ formatInviteModalCanInvite(inviteModalPlayer) }}</dd>
            </div>
            <div v-if="inviteModalPlayer.cannotInviteReason" class="account-field-row">
              <dt>{{ t('accountTeam.invite.cannotInviteReason') }}</dt>
              <dd>{{ formatInviteModalCannotReason(inviteModalPlayer) }}</dd>
            </div>
          </dl>
          <label class="team-invite-modal-message">
            {{ t('accountTeam.invite.message') }}
            <textarea
              v-model="inviteModalMessage"
              rows="3"
              maxlength="200"
              :placeholder="t('accountTeam.invite.messagePlaceholder')"
            />
          </label>
          <p v-if="inviteModalError" class="team-invite-modal-error">{{ inviteModalError }}</p>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" :disabled="teamStore.actionLoading" @click="closeInviteConfirmModal">{{ t('accountTeam.common.cancel') }}</button>
            <button
              type="button"
              class="account-btn-primary"
              :disabled="teamStore.actionLoading"
              @click="confirmSendInvite"
            >
              {{ teamStore.actionLoading ? t('accountTeam.invite.sending') : t('accountTeam.invite.confirm') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showHistoryInviteDialog" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" :aria-label="t('accountTeam.common.close')" @click="closeHistoryInviteModal" />
        <div class="team-dialog__panel team-dialog__panel--history" role="dialog" aria-modal="true" aria-labelledby="history-invite-title">
          <h3 id="history-invite-title">{{ t('accountTeam.invite.history') }}</h3>
          <div class="team-history-invite-toolbar">
            <div class="team-search-row team-history-invite-search">
              <input
                v-model="historyInviteKeyword"
                type="search"
                :placeholder="t('accountTeam.invite.historySearchPlaceholder')"
                @keyup.enter="searchHistoryInvites"
              />
              <button type="button" class="account-btn-secondary" @click="searchHistoryInvites">{{ t('accountTeam.common.search') }}</button>
            </div>
            <div class="team-history-invite-filters">
              <button
                v-for="option in historyInviteStatusOptions"
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
            <p v-if="teamStore.invitationsLoading" class="team-history-invite-loading">{{ t('accountTeam.common.loading') }}</p>
            <div v-else-if="historyInvitePagedRecords.length" class="team-table-wrap team-history-invite-table-wrap">
              <table class="team-table team-table--history">
                <thead>
                  <tr>
                    <th>{{ t('accountTeam.invite.invitee') }}</th>
                    <th>{{ t('accountTeam.invite.invitationNo') }}</th>
                    <th>{{ t('accountTeam.invite.platformGameId') }}</th>
                    <th>{{ t('accountTeam.invite.inviteStatus') }}</th>
                    <th>{{ t('accountTeam.invite.inviteTime') }}</th>
                    <th>{{ t('accountTeam.invite.responseTime') }}</th>
                    <th>{{ t('accountTeam.common.actions') }}</th>
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
                        :text="item.invitationStatusText || t('accountTeam.common.unknown')"
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
            <button type="button" class="account-btn-secondary" @click="closeHistoryInviteModal">{{ t('accountTeam.common.close') }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showInvitationDialog && activeInvitation" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" :aria-label="t('accountTeam.common.close')" @click="showInvitationDialog = false" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true">
          <h3>{{ t('accountTeam.invite.dialogTitle') }}</h3>
          <dl class="account-info-list">
            <div class="account-field-row"><dt>{{ t('accountTeam.invite.team') }}</dt><dd>{{ activeInvitation.teamName }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.invite.note') }}</dt><dd>{{ activeInvitation.invitationMessage || t('accountTeam.invite.defaultMessage') }}</dd></div>
            <div class="account-field-row"><dt>{{ t('accountTeam.invite.expireTime') }}</dt><dd>{{ formatDateTime(activeInvitation.expireTime) }}</dd></div>
          </dl>
          <div class="team-dialog__actions">
            <p v-if="activeInvitation.cannotAcceptReason" class="team-tip">{{ activeInvitation.cannotAcceptReason }}</p>
            <button type="button" class="account-btn-secondary" @click="respondInvitation(false)">{{ t('accountTeam.invite.reject') }}</button>
            <button
              v-if="activeInvitation.canAccept !== false"
              type="button"
              class="account-btn-primary"
              @click="respondInvitation(true)"
            >
              {{ t('accountTeam.invite.accept') }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showPlatformPicker" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" :aria-label="t('accountTeam.common.close')" @click="showPlatformPicker = false" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true">
          <h3>{{ t('accountTeam.registration.platformTitle') }}</h3>
          <p class="team-tip">{{ t('accountTeam.registration.platformHint') }}</p>
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
            <button type="button" class="account-btn-secondary" @click="showPlatformPicker = false">{{ t('accountTeam.common.cancel') }}</button>
            <button type="button" class="account-btn-primary" @click="confirmPlatformAndCreateDraft">{{ t('accountTeam.registration.start') }}</button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div v-if="showRegistrationDetailDialog" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" :aria-label="t('accountTeam.common.close')" @click="closeRegistrationDetailDialog" />
        <div
          class="team-dialog__panel team-dialog__panel--registration-detail"
          role="dialog"
          aria-modal="true"
          :aria-labelledby="'registration-detail-title'"
        >
          <h3 id="registration-detail-title">{{ registrationDetailDialogTitle }}</h3>

          <div class="team-registration-detail-body">
            <p v-if="registrationDetailLoading" class="team-registration-detail-loading">{{ t('accountTeam.registration.loading') }}</p>

            <template v-else-if="teamStore.registrationDetail">
              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">{{ t('accountTeam.registration.basic') }}</h4>
                <dl class="account-info-list">
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.registrationNo') }}</dt><dd>{{ teamStore.registrationDetail.registrationNo || '-' }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.eventName') }}</dt><dd>{{ teamStore.registrationDetail.tournamentName || '-' }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.seasonStage') }}</dt><dd>{{ teamStore.registrationDetail.seasonName || '-' }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.signupPlatformField') }}</dt><dd>{{ teamStore.registrationDetail.platformName || teamStore.registrationDetail.platformCode || '-' }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.signupStatus') }}</dt><dd>{{ registrationDetailStatusText }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.submittedAt') }}</dt><dd>{{ formatDateTime(teamStore.registrationDetail.submittedTime) || '-' }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.auditedAt') }}</dt><dd>{{ registrationDetailAuditTime }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.deadlineField') }}</dt><dd>{{ formatDateTime(teamStore.registrationDetail.registrationEndTime) || '-' }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.contactEmail') }}</dt><dd>{{ registrationCaptainEmail }}</dd></div>
                  <div v-if="teamStore.registrationDetail.remark && teamStore.registrationDetail.registrationStatus !== 4" class="account-field-row">
                    <dt>{{ t('accountTeam.registration.remark') }}</dt><dd>{{ teamStore.registrationDetail.remark }}</dd>
                  </div>
                </dl>
              </section>

              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">{{ t('accountTeam.registration.roster') }}</h4>
                <p v-if="teamStore.registrationDetail.rosterLocked" class="team-tip team-roster-lock-tip">
                  {{ t('accountTeam.registration.rosterLocked') }}
                </p>
                <div v-if="registrationDetailRosterRows.length" class="team-table-wrap">
                  <table class="team-table">
                    <thead>
                      <tr>
                        <th>{{ t('accountTeam.registration.player') }}</th>
                        <th>{{ t('accountTeam.application.position') }}</th>
                        <th>{{ t('accountTeam.invite.platformGameId') }}</th>
                        <th>{{ t('accountTeam.registration.eligibility') }}</th>
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
                <AccountEmptyState v-else :message="t('accountTeam.registration.rosterEmpty')" />
              </section>

              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">{{ t('accountTeam.registration.auditResult') }}</h4>
                <dl class="account-info-list">
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.auditStatus') }}</dt><dd>{{ registrationDetailStatusText }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.auditor') }}</dt><dd>{{ registrationDetailLastAuditor }}</dd></div>
                  <div class="account-field-row"><dt>{{ t('accountTeam.registration.auditedAt') }}</dt><dd>{{ registrationDetailAuditTime }}</dd></div>
                  <div
                    v-if="teamStore.registrationDetail.registrationStatus === 4 && teamStore.registrationDetail.remark"
                    class="account-field-row"
                  >
                    <dt>{{ t('accountTeam.registration.rejectReason') }}</dt><dd>{{ teamStore.registrationDetail.remark }}</dd>
                  </div>
                  <div
                    v-if="teamStore.registrationDetail.registrationStatus === 3"
                    class="account-field-row team-roster-lock-tip"
                  >
                    <dt>{{ t('accountTeam.registration.adminRemark') }}</dt>
                    <dd>{{ teamStore.registrationAudits.at(-1)?.auditRemark || teamStore.registrationDetail.remark || t('accountTeam.registration.approvedRemark') }}</dd>
                  </div>
                </dl>
              </section>

              <section class="team-registration-detail-section">
                <h4 class="team-subtitle">{{ t('accountTeam.registration.auditLog') }}</h4>
                <div v-if="teamStore.registrationAudits.length" class="team-table-wrap">
                  <table class="team-table">
                    <thead>
                      <tr>
                        <th>{{ t('accountTeam.registration.action') }}</th>
                        <th>{{ t('accountTeam.registration.auditor') }}</th>
                        <th>{{ t('accountTeam.registration.note') }}</th>
                        <th>{{ t('accountTeam.registration.time') }}</th>
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
                <AccountEmptyState v-else :message="t('accountTeam.registration.auditEmpty')" />
              </section>

              <label
                v-if="teamStore.registrationDetail.canWithdraw && canManageCurrentTeam"
                class="team-inline-field team-registration-detail-withdraw"
              >
                {{ t('accountTeam.registration.withdrawReason') }}
                <input v-model="withdrawReason" type="text" maxlength="200" :placeholder="t('accountTeam.registration.withdrawPlaceholder')" />
              </label>
            </template>
          </div>

          <div class="team-dialog__actions team-dialog__actions--registration-detail">
            <button type="button" class="account-btn-secondary" @click="closeRegistrationDetailDialog">{{ t('accountTeam.common.close') }}</button>
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
                {{ t('accountTeam.registration.continue') }}
              </button>
              <button
                v-if="teamStore.registrationDetail.canDeleteDraft"
                type="button"
                class="account-btn-secondary"
                @click="deleteRegistrationDraft"
              >
                {{ t('accountTeam.registration.abandon') }}
              </button>
              <button
                v-if="teamStore.registrationDetail.canWithdraw"
                type="button"
                class="account-btn-secondary"
                @click="withdrawRegistration"
              >
                {{ t('accountTeam.registration.withdrawEdit') }}
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
                {{ t('accountTeam.registration.resubmit') }}
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
                {{ t('accountTeam.registration.manageEventTeam') }}
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

.team-inherit-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.team-inherit-reason {
  color: #a33b32;
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
