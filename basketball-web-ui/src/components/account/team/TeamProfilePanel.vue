<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountTeamLogo from '@/components/account/team/AccountTeamLogo.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import { uploadAccountFile } from '@/api/accountFile'
import { submitTeamCertification, updateTeam } from '@/api/accountTeam'
import {
  cancelTeamProfileChange,
  getTeamProfileChangeDetail,
  getTeamProfileChangeHistory,
  getTeamProfileEdit,
  submitTeamProfileChange,
} from '@/api/teamProfileChange'
import type { TeamDetailDto } from '@/api/types/teamAccount'
import type {
  TeamProfileChangeDetailDto,
  TeamProfileChangeItemDto,
} from '@/api/types/teamProfileChange'
import { formatDateTime } from '@/utils/formatDate'

const { t } = useI18n()

const props = defineProps<{
  teamId: string
  teamDetail?: TeamDetailDto | null
  canManage: boolean
  isApproved: boolean
}>()

const emit = defineEmits<{
  refreshed: []
}>()

function profileChangeLabel(status?: number) {
  if (status === 1) return t('accountTeam.profile.change.pending')
  if (status === 2) return t('accountTeam.profile.change.reviewing')
  if (status === 3) return t('accountTeam.profile.change.approved')
  if (status === 4) return t('accountTeam.profile.change.rejected')
  if (status === 5) return t('accountTeam.profile.change.cancelled')
  return ''
}

const historyStatusOptions = computed(() => [
  { label: t('accountTeam.profile.filter.all'), value: '' as const },
  { label: t('accountTeam.profile.filter.pending'), value: 1 },
  { label: t('accountTeam.profile.filter.approved'), value: 3 },
  { label: t('accountTeam.profile.filter.rejected'), value: 4 },
  { label: t('accountTeam.profile.filter.cancelled'), value: 5 },
])

const loading = ref(false)
const actionLoading = ref(false)
const editDialogLoading = ref(false)
const pendingChange = ref<TeamProfileChangeDetailDto>()
const hasPendingChange = ref(false)
const recentHistory = ref<TeamProfileChangeItemDto[]>([])

const profileForm = reactive({
  teamName: '',
  shortName: '',
  description: '',
  publicStatus: 1,
  changeReason: '',
})
const profileLogoFileId = ref('')
const profileLogoPreviewUrl = ref('')

const showEditDialog = ref(false)
const showHistoryDialog = ref(false)
const historyLoading = ref(false)
const historyRecords = ref<TeamProfileChangeItemDto[]>([])
const historyPage = reactive({ pageNum: 1, pageSize: 5, total: 0, pages: 0 })
const historyAuditStatus = ref<number | ''>('')
const historyKeyword = ref('')

const showDetailDialog = ref(false)
const detailLoading = ref(false)
const changeDetail = ref<TeamProfileChangeDetailDto>()

const officialProfile = computed(() => props.teamDetail)
const certStatus = computed(() => props.teamDetail?.auditStatus)
const isCertDraft = computed(() => certStatus.value === 4)
const isCertPending = computed(() => certStatus.value === 0 || certStatus.value === 3)
const isCertRejected = computed(() => certStatus.value === 2)
const isCertApproved = computed(() => certStatus.value === 1 || props.isApproved)
/** 仅已认证球队走资料变更审核 */
const useAuditFlow = computed(() => isCertApproved.value && props.canManage)
/** 未认证草稿/驳回：可直接改待认证资料 */
const canEditCertDraft = computed(() =>
  props.canManage && (isCertDraft.value || isCertRejected.value),
)
const canSubmitCertification = computed(() => canEditCertDraft.value)
const certificationRejectRemark = computed(() =>
  props.teamDetail?.certificationRejectRemark || '',
)

const displayAuditStatusText = computed(() => {
  if (hasPendingChange.value) {
    const status = pendingChange.value?.auditStatus
    return status != null
      ? profileChangeLabel(status) || pendingChange.value?.auditStatusText || t('accountTeam.profile.change.pending')
      : t('accountTeam.profile.change.pending')
  }
  const latest = recentHistory.value[0]
  if (latest?.auditStatus === 4) return t('accountTeam.profile.change.rejected')
  if (latest?.auditStatus === 3) return t('accountTeam.profile.change.approved')
  return t('accountTeam.profile.nonePending')
})

const displayAuditStatusType = computed(() => {
  if (hasPendingChange.value) {
    const status = pendingChange.value?.auditStatus
    if (status === 1 || status === 2 || status == null) return 'warning'
    if (status === 4) return 'danger'
    if (status === 3) return 'success'
    return 'default'
  }
  const latest = recentHistory.value[0]
  if (latest?.auditStatus === 4) return 'danger'
  if (latest?.auditStatus === 3) return 'success'
  return 'default'
})

const showPendingHint = computed(() =>
  useAuditFlow.value
  && hasPendingChange.value
  && (pendingChange.value?.auditStatus === 1 || pendingChange.value?.auditStatus === 2),
)

const latestRejectedRemark = computed(() => {
  if (!useAuditFlow.value) return ''
  if (pendingChange.value?.auditStatus === 4) return pendingChange.value.auditRemark || ''
  return recentHistory.value.find(item => item.auditStatus === 4)?.auditRemark || ''
})

const showRejectedHint = computed(() =>
  useAuditFlow.value && !hasPendingChange.value && Boolean(latestRejectedRemark.value),
)

const historyHasFilter = computed(() =>
  historyAuditStatus.value !== '' || historyKeyword.value.trim().length > 0,
)

const historyEmptyText = computed(() =>
  historyHasFilter.value ? t('accountTeam.profile.historyFilteredEmpty') : t('accountTeam.profile.historyEmpty'),
)

const submitButtonText = computed(() => {
  if (useAuditFlow.value) return t('accountTeam.profile.submitChange')
  return t('accountTeam.profile.save')
})

const canOpenEditDialog = computed(() => {
  if (!props.canManage) return false
  if (isCertPending.value) return false
  if (useAuditFlow.value) return !showPendingHint.value
  return canEditCertDraft.value
})

function historyStatusType(status?: number): 'success' | 'warning' | 'danger' | 'default' {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 4) return 'danger'
  return 'default'
}

function publicStatusText(status?: number) {
  return status === 1 ? t('accountTeam.profile.public') : t('accountTeam.profile.private')
}

function applyFormFromOfficial(detail?: TeamDetailDto | null) {
  if (!detail) return
  profileForm.teamName = detail.teamName || ''
  profileForm.shortName = detail.shortName || ''
  profileForm.description = detail.description || ''
  profileForm.publicStatus = detail.publicStatus ?? 1
  profileForm.changeReason = ''
  profileLogoPreviewUrl.value = detail.logoUrl || ''
  profileLogoFileId.value = ''
}

function applyFormFromChange(change: TeamProfileChangeDetailDto) {
  profileForm.teamName = change.newTeamName || profileForm.teamName
  profileForm.shortName = change.newTeamShortName || ''
  profileForm.description = change.newDescription || ''
  profileForm.changeReason = change.changeReason || ''
  profileLogoPreviewUrl.value = change.newLogoUrl || profileLogoPreviewUrl.value
  profileLogoFileId.value = ''
}

async function loadProfileEditData() {
  if (!props.teamId) return
  if (!useAuditFlow.value) {
    pendingChange.value = undefined
    hasPendingChange.value = false
    recentHistory.value = []
    return
  }
  loading.value = true
  try {
    const res = await getTeamProfileEdit(props.teamId)
    pendingChange.value = res.data.pendingChange
    hasPendingChange.value = Boolean(res.data.hasPendingChange)
    recentHistory.value = res.data.recentHistory || []
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.loadAuditFailed'))
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.teamId, props.isApproved, props.canManage] as const,
  () => {
    loadProfileEditData().catch(() => undefined)
  },
  { immediate: true },
)

async function prepareEditForm() {
  applyFormFromOfficial(officialProfile.value)
  if (!useAuditFlow.value) return

  if (hasPendingChange.value && pendingChange.value) {
    applyFormFromChange(pendingChange.value)
    return
  }

  const rejected = recentHistory.value.find(item => item.auditStatus === 4)
  if (!rejected?.changeId || !props.teamId) return

  editDialogLoading.value = true
  try {
    const res = await getTeamProfileChangeDetail(props.teamId, rejected.changeId)
    applyFormFromChange(res.data)
  } catch {
    // 驳回内容加载失败时仍使用正式资料
  } finally {
    editDialogLoading.value = false
  }
}

async function openEditDialog() {
  if (!canOpenEditDialog.value) {
    if (isCertPending.value) {
      ElMessage.warning(t('accountTeam.profile.certPendingWarn'))
    } else if (showPendingHint.value) {
      ElMessage.warning(t('accountTeam.profile.pendingWarn'))
    }
    return
  }
  showEditDialog.value = true
  await prepareEditForm()
}

function openPendingEdit() {
  if (!pendingChange.value?.changeId) return
  openDetail(pendingChange.value.changeId).catch(() => undefined)
}

async function handleLogoUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  try {
    const res = await uploadAccountFile(file, 'TEAM_LOGO')
    profileLogoFileId.value = res.data.fileId
    profileLogoPreviewUrl.value = res.data.accessUrl
    if (!useAuditFlow.value && props.teamId) {
      const { bindTeamLogo } = await import('@/api/accountTeam')
      const updated = await bindTeamLogo(props.teamId, { fileId: res.data.fileId })
      profileLogoPreviewUrl.value = updated.data.logoUrl || res.data.accessUrl
      ElMessage.success(t('accountTeam.profile.logoUpdated'))
      emit('refreshed')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.logoFailed'))
  }
}

async function savePublicStatusIfNeeded() {
  if (!props.teamId || !officialProfile.value) return
  if (profileForm.publicStatus === officialProfile.value.publicStatus) return
  await updateTeam(props.teamId, { publicStatus: profileForm.publicStatus })
}

async function submitDirectProfile() {
  if (!props.teamId) return
  await updateTeam(props.teamId, {
    teamName: profileForm.teamName.trim(),
    shortName: profileForm.shortName.trim() || undefined,
    description: profileForm.description.trim() || undefined,
    publicStatus: profileForm.publicStatus,
  })
  ElMessage.success(isCertRejected.value
    ? t('accountTeam.profile.savedResubmit')
    : t('accountTeam.profile.savedSubmit'))
  showEditDialog.value = false
  emit('refreshed')
}

async function submitAuditProfile() {
  if (!props.teamId) return
  actionLoading.value = true
  try {
    await savePublicStatusIfNeeded()
    await submitTeamProfileChange(props.teamId, {
      teamName: profileForm.teamName.trim(),
      teamShortName: profileForm.shortName.trim() || undefined,
      logoFileId: profileLogoFileId.value || undefined,
      description: profileForm.description.trim() || undefined,
      changeReason: profileForm.changeReason.trim() || undefined,
    })
    ElMessage.success(t('accountTeam.profile.changeSubmitted'))
    showEditDialog.value = false
    profileLogoFileId.value = ''
    await loadProfileEditData()
    emit('refreshed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.submitFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function handleSubmitCertification() {
  if (!props.teamId || !canSubmitCertification.value) return
  actionLoading.value = true
  try {
    await submitTeamCertification(props.teamId)
    ElMessage.success(isCertRejected.value
      ? t('accountTeam.profile.certResubmitted')
      : t('accountTeam.profile.certSubmitted'))
    emit('refreshed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.certFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function handleSubmit() {
  if (!props.canManage) return
  if (!profileForm.teamName.trim()) {
    ElMessage.warning(t('accountTeam.profile.nameRequired'))
    return
  }
  if (useAuditFlow.value) {
    await submitAuditProfile()
  } else if (canEditCertDraft.value) {
    actionLoading.value = true
    try {
      await submitDirectProfile()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.saveFailed'))
    } finally {
      actionLoading.value = false
    }
  }
}

async function handleCancelPending() {
  if (!props.teamId || !pendingChange.value?.changeId) return
  try {
    await ElMessageBox.confirm(
      t('accountTeam.profile.cancelConfirm'),
      t('accountTeam.profile.cancelTitle'),
      {
        type: 'warning',
        confirmButtonText: t('accountTeam.common.confirm'),
        cancelButtonText: t('accountTeam.common.cancel'),
      },
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await cancelTeamProfileChange(props.teamId, pendingChange.value.changeId)
    ElMessage.success(t('accountTeam.profile.cancelled'))
    showEditDialog.value = false
    await loadProfileEditData()
    emit('refreshed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.cancelFailed'))
  } finally {
    actionLoading.value = false
  }
}

async function loadHistory() {
  if (!props.teamId) return
  historyLoading.value = true
  try {
    const res = await getTeamProfileChangeHistory(props.teamId, {
      pageNum: historyPage.pageNum,
      pageSize: historyPage.pageSize,
      auditStatus: historyAuditStatus.value === '' ? undefined : historyAuditStatus.value,
      keyword: historyKeyword.value.trim() || undefined,
    })
    historyRecords.value = res.data.records || []
    historyPage.total = res.data.total || 0
    historyPage.pageNum = res.data.current || res.data.pageNum || historyPage.pageNum
    historyPage.pageSize = res.data.size || res.data.pageSize || historyPage.pageSize
    historyPage.pages = res.data.pages || 0
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.historyFailed'))
  } finally {
    historyLoading.value = false
  }
}

function openHistoryDialog() {
  showHistoryDialog.value = true
  historyPage.pageNum = 1
  historyPage.pageSize = 5
  historyAuditStatus.value = ''
  historyKeyword.value = ''
  loadHistory().catch(() => undefined)
}

function handleHistoryFilter() {
  historyPage.pageNum = 1
  loadHistory().catch(() => undefined)
}

function handleHistoryReset() {
  historyAuditStatus.value = ''
  historyKeyword.value = ''
  historyPage.pageNum = 1
  historyPage.pageSize = 5
  loadHistory().catch(() => undefined)
}

function handleHistoryPageChange(pageNum: number) {
  historyPage.pageNum = pageNum
  loadHistory().catch(() => undefined)
}

function handleHistoryPageSizeChange(pageSize: number) {
  historyPage.pageSize = pageSize
  historyPage.pageNum = 1
  loadHistory().catch(() => undefined)
}

async function openDetail(changeId: string) {
  if (!props.teamId) return
  showDetailDialog.value = true
  detailLoading.value = true
  changeDetail.value = undefined
  try {
    const res = await getTeamProfileChangeDetail(props.teamId, changeId)
    changeDetail.value = res.data
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountTeam.profile.detailFailed'))
  } finally {
    detailLoading.value = false
  }
}

function formatCompare(oldVal?: string | null, newVal?: string | null) {
  const left = oldVal?.trim() || '-'
  const right = newVal?.trim() || '-'
  return `${left} → ${right}`
}
</script>

<template>
  <div class="team-profile-panel" v-loading="loading">
    <AccountPanel :title="isCertApproved ? t('accountTeam.profile.official') : t('accountTeam.profile.title')">
      <template v-if="canManage" #extra>
        <div class="team-profile-panel__actions">
          <button
            v-if="canOpenEditDialog"
            type="button"
            class="account-btn-secondary"
            @click="openEditDialog"
          >
            {{ isCertRejected ? t('accountTeam.profile.editRejected') : t('accountTeam.profile.edit') }}
          </button>
          <button
            v-if="canSubmitCertification"
            type="button"
            class="account-btn-primary"
            :disabled="actionLoading"
            @click="handleSubmitCertification"
          >
            {{ isCertRejected ? t('accountTeam.profile.resubmitCert') : t('accountTeam.profile.submitCert') }}
          </button>
          <button
            v-if="useAuditFlow"
            type="button"
            class="account-btn-secondary"
            @click="openHistoryDialog"
          >
            {{ t('accountTeam.profile.history') }}
          </button>
        </div>
      </template>

      <div class="team-profile-card">
        <AccountTeamLogo
          :logo-url="officialProfile?.logoUrl"
          :short-name="officialProfile?.shortName"
          :team-name="officialProfile?.teamName"
          size="md"
        />
        <dl class="account-info-list team-profile-card__info">
          <div class="account-field-row">
            <dt>{{ t('accountTeam.profile.teamName') }}</dt>
            <dd>{{ officialProfile?.teamName || '-' }}</dd>
          </div>
          <div class="account-field-row">
            <dt>{{ t('accountTeam.profile.shortName') }}</dt>
            <dd>{{ officialProfile?.shortName || '-' }}</dd>
          </div>
          <div class="account-field-row">
            <dt>{{ t('accountTeam.profile.publicStatus') }}</dt>
            <dd>{{ publicStatusText(officialProfile?.publicStatus) }}</dd>
          </div>
          <div class="account-field-row">
            <dt>{{ t('accountTeam.profile.intro') }}</dt>
            <dd>{{ officialProfile?.description || t('accountTeam.common.empty') }}</dd>
          </div>
        </dl>
      </div>

      <!-- 首次认证状态（未 APPROVED 时只展示认证语义，绝不混用资料变更文案） -->
      <div v-if="canManage && !isCertApproved" class="team-profile-status-bar">
        <div class="team-profile-status-bar__main">
          <p v-if="isCertPending" class="team-profile-status-bar__text">
            {{ t('accountTeam.profile.certPending') }}
          </p>
          <p v-else-if="isCertRejected" class="team-profile-status-bar__text team-profile-status-bar__text--danger">
            {{ certificationRejectRemark
              ? t('accountTeam.profile.certRejectedWithRemark', { remark: certificationRejectRemark })
              : t('accountTeam.profile.certRejected') }}
          </p>
          <p v-else-if="isCertDraft" class="team-profile-status-bar__text">
            {{ t('accountTeam.profile.certDraft') }}
          </p>
        </div>
        <AccountStatusBadge
          :text="isCertPending ? t('accountTeam.profile.badgePending') : (isCertRejected ? t('accountTeam.profile.badgeRejected') : t('accountTeam.profile.badgeDraft'))"
          :type="isCertRejected ? 'danger' : 'warning'"
        />
      </div>

      <div v-if="useAuditFlow" class="team-profile-status-bar">
        <div class="team-profile-status-bar__main">
          <p v-if="showPendingHint" class="team-profile-status-bar__text">
            {{ t('accountTeam.profile.pendingHint') }}
          </p>
          <p v-else-if="showRejectedHint" class="team-profile-status-bar__text team-profile-status-bar__text--danger">
            {{ latestRejectedRemark
              ? t('accountTeam.profile.rejectedHintWithRemark', { remark: latestRejectedRemark })
              : t('accountTeam.profile.rejectedHint') }}
          </p>
          <p v-else class="team-profile-status-bar__text team-profile-status-bar__text--muted">
            {{ t('accountTeam.profile.auditHint') }}
          </p>

          <div v-if="showPendingHint" class="team-profile-status-bar__links">
            <button type="button" class="team-profile-link-btn" @click="openPendingEdit">{{ t('accountTeam.profile.viewPending') }}</button>
            <button type="button" class="team-profile-link-btn team-profile-link-btn--danger" @click="handleCancelPending">
              {{ t('accountTeam.profile.cancelRequest') }}
            </button>
          </div>
        </div>
        <AccountStatusBadge
          :text="displayAuditStatusText"
          :type="displayAuditStatusType"
        />
      </div>
    </AccountPanel>

    <!-- 编辑资料弹窗 -->
    <el-dialog
      v-model="showEditDialog"
      :title="t('accountTeam.profile.editTitle')"
      width="92%"
      class="team-profile-dialog"
      append-to-body
      destroy-on-close
    >
      <div v-loading="editDialogLoading" class="team-profile-edit">
        <p v-if="isCertRejected && !useAuditFlow" class="team-profile-edit__hint team-profile-edit__hint--danger">
          {{ certificationRejectRemark
            ? t('accountTeam.profile.editRejectedHintWithRemark', { remark: certificationRejectRemark })
            : t('accountTeam.profile.editRejectedHint') }}
        </p>
        <p v-else-if="isCertDraft && !useAuditFlow" class="team-profile-edit__hint">
          {{ t('accountTeam.profile.editDraftHint') }}
        </p>
        <p v-else-if="showRejectedHint" class="team-profile-edit__hint team-profile-edit__hint--danger">
          {{ latestRejectedRemark
            ? t('accountTeam.profile.changeRejectedHintWithRemark', { remark: latestRejectedRemark })
            : t('accountTeam.profile.changeRejectedHint') }}
        </p>

        <div class="team-profile-edit__logo">
          <AccountTeamLogo
            :logo-url="profileLogoPreviewUrl || officialProfile?.logoUrl"
            :short-name="profileForm.shortName || officialProfile?.shortName"
            :team-name="profileForm.teamName || officialProfile?.teamName"
            size="md"
          />
          <div class="team-profile-edit__logo-actions">
            <label class="account-btn-secondary team-logo-upload">
              {{ t('accountTeam.profile.changeLogo') }}
              <input type="file" accept="image/*" hidden @change="handleLogoUpload" />
            </label>
            <p class="team-tip">
              {{ useAuditFlow ? t('accountTeam.profile.logoAuditHint') : t('accountTeam.profile.logoDraftHint') }}
            </p>
          </div>
        </div>

        <div class="team-form-grid">
          <label>{{ t('accountTeam.profile.teamName') }}<input v-model="profileForm.teamName" type="text" maxlength="30" /></label>
          <label>{{ t('accountTeam.profile.shortName') }}<input v-model="profileForm.shortName" type="text" maxlength="12" /></label>
          <label>
            {{ t('accountTeam.profile.publicStatus') }}
            <select v-model.number="profileForm.publicStatus">
              <option :value="1">{{ t('accountTeam.profile.public') }}</option>
              <option :value="0">{{ t('accountTeam.profile.private') }}</option>
            </select>
          </label>
          <label class="team-form-full">
            {{ t('accountTeam.profile.intro') }}
            <textarea v-model="profileForm.description" rows="3" maxlength="500" />
          </label>
          <label v-if="useAuditFlow" class="team-form-full">
            {{ t('accountTeam.profile.changeReason') }}
            <textarea v-model="profileForm.changeReason" rows="2" maxlength="200" :placeholder="t('accountTeam.profile.changeReasonPlaceholder')" />
          </label>
        </div>

        <p v-if="useAuditFlow" class="team-tip">{{ t('accountTeam.profile.publicImmediate') }}</p>
      </div>

      <template #footer>
        <div class="team-profile-edit__footer">
          <div class="team-profile-edit__footer-right">
            <button type="button" class="account-btn-secondary" @click="showEditDialog = false">{{ t('accountTeam.common.cancel') }}</button>
            <button
              type="button"
              class="account-btn-primary"
              :disabled="actionLoading"
              @click="handleSubmit"
            >
              {{ submitButtonText }}
            </button>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- 审核历史弹窗 -->
    <el-dialog
      v-model="showHistoryDialog"
      :title="t('accountTeam.profile.historyTitle')"
      width="72%"
      align-center
      class="team-profile-history-dialog"
      modal-class="team-profile-history-overlay"
      append-to-body
      destroy-on-close
    >
      <div class="team-profile-history-body">
        <div class="team-profile-history-filters">
          <label class="team-profile-history-filters__status">
            {{ t('accountTeam.profile.auditStatus') }}
            <select v-model="historyAuditStatus" class="team-profile-history-filters__select" @change="handleHistoryFilter">
              <option v-for="item in historyStatusOptions" :key="String(item.value)" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="team-profile-history-filters__keyword">
            {{ t('accountTeam.profile.keyword') }}
            <input
              v-model="historyKeyword"
              type="text"
              class="team-profile-history-filters__input"
              :placeholder="t('accountTeam.profile.keywordPlaceholder')"
              @keyup.enter="handleHistoryFilter"
            />
          </label>
          <div class="team-profile-history-filters__actions">
            <button type="button" class="account-btn-secondary" @click="handleHistoryFilter">{{ t('accountTeam.common.search') }}</button>
            <button type="button" class="account-btn-secondary" @click="handleHistoryReset">{{ t('accountTeam.common.reset') }}</button>
          </div>
        </div>

        <div class="team-profile-history-table-wrap">
          <el-table
            v-loading="historyLoading"
            :data="historyRecords"
            size="small"
            :empty-text="historyEmptyText"
            table-layout="fixed"
          >
            <el-table-column :label="t('accountTeam.profile.changeNo')" prop="changeNo" min-width="120" show-overflow-tooltip />
            <el-table-column :label="t('accountTeam.profile.changeSummary')" prop="changeSummary" min-width="100" show-overflow-tooltip />
            <el-table-column :label="t('accountTeam.profile.auditStatus')" width="92">
              <template #default="{ row }">
                <AccountStatusBadge
                  :text="row.auditStatusText || '-'"
                  :type="historyStatusType(row.auditStatus)"
                />
              </template>
            </el-table-column>
            <el-table-column :label="t('accountTeam.profile.submitTime')" min-width="132">
              <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
            </el-table-column>
            <el-table-column :label="t('accountTeam.profile.auditTime')" min-width="132">
              <template #default="{ row }">{{ row.auditTime ? formatDateTime(row.auditTime) : '-' }}</template>
            </el-table-column>
            <el-table-column :label="t('accountTeam.common.actions')" width="88" align="center">
              <template #default="{ row }">
                <button type="button" class="team-profile-link-btn" @click="openDetail(row.changeId)">{{ t('accountTeam.profile.viewDetail') }}</button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <MyEventsPagination
          v-if="historyPage.total > 0"
          :page="historyPage.pageNum"
          :page-size="historyPage.pageSize"
          :total="historyPage.total"
          @update:page="handleHistoryPageChange"
          @update:page-size="handleHistoryPageSizeChange"
        />
      </div>

      <template #footer>
        <button type="button" class="account-btn-secondary" @click="showHistoryDialog = false">{{ t('accountTeam.common.close') }}</button>
      </template>
    </el-dialog>

    <!-- 历史详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      :title="t('accountTeam.profile.detailTitle')"
      width="64%"
      align-center
      class="team-profile-detail-dialog"
      modal-class="team-profile-history-overlay"
      append-to-body
    >
      <div v-loading="detailLoading" class="team-profile-change-detail team-profile-detail-body">
        <template v-if="changeDetail">
          <div class="team-profile-detail-head">
            <p>{{ t('accountTeam.profile.changeNoLine', { no: changeDetail.changeNo }) }}</p>
            <AccountStatusBadge
              :text="changeDetail.auditStatusText || '-'"
              :type="historyStatusType(changeDetail.auditStatus)"
            />
          </div>
          <div class="team-profile-change-detail__compare">
            <div class="team-profile-change-detail__col">
              <h4>{{ t('accountTeam.profile.before') }}</h4>
              <p>{{ t('accountTeam.profile.nameLine', { value: changeDetail.oldTeamName || '-' }) }}</p>
              <p>{{ t('accountTeam.profile.shortNameLine', { value: changeDetail.oldTeamShortName || '-' }) }}</p>
              <p>{{ t('accountTeam.profile.publicLine', { value: publicStatusText(changeDetail.oldPublicStatus) }) }}</p>
              <p>{{ t('accountTeam.profile.introLine', { value: changeDetail.oldDescription || t('accountTeam.common.empty') }) }}</p>
              <p class="team-profile-change-detail__logo-label">{{ t('accountTeam.profile.logo') }}</p>
              <AccountTeamLogo
                :logo-url="changeDetail.oldLogoUrl"
                :team-name="changeDetail.oldTeamName"
                size="sm"
              />
              <p v-if="!changeDetail.oldLogoUrl" class="team-tip">{{ t('accountTeam.profile.logoUnset') }}</p>
            </div>
            <div class="team-profile-change-detail__col">
              <h4>{{ t('accountTeam.profile.after') }}</h4>
              <p>{{ t('accountTeam.profile.nameLine', { value: changeDetail.newTeamName || '-' }) }}</p>
              <p>{{ t('accountTeam.profile.shortNameLine', { value: changeDetail.newTeamShortName || '-' }) }}</p>
              <p>{{ t('accountTeam.profile.publicLine', { value: publicStatusText(changeDetail.newPublicStatus) }) }}</p>
              <p>{{ t('accountTeam.profile.introLine', { value: changeDetail.newDescription || t('accountTeam.common.empty') }) }}</p>
              <p class="team-profile-change-detail__logo-label">{{ t('accountTeam.profile.logo') }}</p>
              <AccountTeamLogo
                :logo-url="changeDetail.newLogoUrl"
                :team-name="changeDetail.newTeamName"
                size="sm"
              />
              <p v-if="!changeDetail.newLogoUrl" class="team-tip">{{ t('accountTeam.profile.logoUnset') }}</p>
            </div>
          </div>

          <div class="team-profile-change-detail__rows">
            <p>{{ t('accountTeam.profile.nameLine', { value: formatCompare(changeDetail.oldTeamName, changeDetail.newTeamName) }) }}</p>
            <p>{{ t('accountTeam.profile.shortNameLine', { value: formatCompare(changeDetail.oldTeamShortName, changeDetail.newTeamShortName) }) }}</p>
            <p>{{ t('accountTeam.profile.publicLine', { value: formatCompare(publicStatusText(changeDetail.oldPublicStatus), publicStatusText(changeDetail.newPublicStatus)) }) }}</p>
            <p>{{ t('accountTeam.profile.introLine', { value: formatCompare(changeDetail.oldDescription, changeDetail.newDescription) }) }}</p>
          </div>

          <div class="team-profile-change-detail__meta">
            <p>{{ t('accountTeam.profile.submitter', { name: changeDetail.submitUserName || changeDetail.submitPlayerNo || '-' }) }}</p>
            <p>{{ t('accountTeam.profile.submitTimeLine', { time: formatDateTime(changeDetail.submitTime) }) }}</p>
            <p>{{ t('accountTeam.profile.auditStatusLine', { status: changeDetail.auditStatusText }) }}</p>
            <p v-if="changeDetail.auditTime">{{ t('accountTeam.profile.auditTimeLine', { time: formatDateTime(changeDetail.auditTime) }) }}</p>
            <p v-if="changeDetail.auditRemark">{{ t('accountTeam.profile.auditRemarkLine', { remark: changeDetail.auditRemark }) }}</p>
          </div>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.team-profile-panel {
  max-width: 100%;
}

.team-profile-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.team-profile-card {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 20px;
  align-items: start;
}

.team-profile-card__info {
  margin: 0;
}

.team-profile-status-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding: 12px 14px;
  border: 1px solid rgba(196, 30, 58, 0.18);
  border-radius: 4px;
  background: rgba(196, 30, 58, 0.04);
}

.team-profile-status-bar__main {
  flex: 1;
  min-width: 0;
}

.team-profile-status-bar__text {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: #5c5c5c;
}

.team-profile-status-bar__text--danger {
  color: #a12828;
}

.team-profile-status-bar__text--muted {
  color: #888;
}

.team-profile-status-bar__links {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}

.team-profile-link-btn {
  padding: 0;
  border: none;
  background: none;
  color: var(--account-accent, #c41e3a);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.team-profile-link-btn--danger {
  color: #a12828;
}

.team-profile-edit__hint {
  margin: 0 0 14px;
  padding: 10px 12px;
  border-radius: 4px;
  background: rgba(196, 30, 58, 0.06);
  color: #6b4a12;
  font-size: 13px;
  line-height: 1.5;
}

.team-profile-edit__hint--danger {
  background: rgba(161, 40, 40, 0.06);
  color: #a12828;
}

.team-profile-edit__logo {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.team-profile-edit__logo-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 6px;
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
    border: 1px solid var(--account-border, #ddd);
    border-radius: 3px;
    font-family: inherit;
    box-sizing: border-box;
  }
}

.team-form-full {
  grid-column: 1 / -1;
}

.team-tip {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #888;
}

.team-logo-upload {
  cursor: pointer;
}

.team-profile-edit__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.team-profile-edit__footer-right {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.team-profile-history-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  margin-bottom: 14px;
}

.team-profile-history-filters__status,
.team-profile-history-filters__keyword {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.team-profile-history-filters__select {
  width: 160px;
  padding: 7px 10px;
  border: 1px solid var(--account-border, #ddd);
  border-radius: 3px;
  font-family: inherit;
  font-size: 13px;
  background: #fff;
}

.team-profile-history-filters__input {
  width: min(300px, 100%);
  min-width: 220px;
  padding: 7px 10px;
  border: 1px solid var(--account-border, #ddd);
  border-radius: 3px;
  font-family: inherit;
  font-size: 13px;
  background: #fff;
}

.team-profile-history-filters__actions {
  display: flex;
  gap: 8px;
}

.team-profile-history-body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  max-height: calc(80vh - 140px);
}

.team-profile-history-table-wrap {
  flex: 1;
  min-height: 120px;
  max-height: min(48vh, 360px);
  overflow: auto;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 4px;
  background: #fff;
}

.team-profile-detail-body {
  max-height: calc(80vh - 120px);
  overflow: auto;
}

.team-profile-detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  font-size: 13px;
  color: #444;
}

.team-profile-change-detail__compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.team-profile-change-detail__col h4 {
  margin: 0 0 10px;
  font-size: 14px;
}

.team-profile-change-detail__logo-label {
  margin: 10px 0 6px;
}

.team-profile-change-detail__rows {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 13px;
  line-height: 1.7;
  color: #444;
}

.team-profile-change-detail__meta {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  font-size: 13px;
  line-height: 1.7;
  color: #555;
}

:deep(.team-profile-dialog) {
  max-width: 640px;
}

:global(.team-profile-history-overlay) {
  background-color: rgba(0, 0, 0, 0.35) !important;
}

:deep(.team-profile-history-dialog.el-dialog) {
  width: min(72vw, 1080px) !important;
  min-width: min(720px, 92vw);
  max-height: 80vh;
  margin: 10vh auto !important;
  display: flex;
  flex-direction: column;
  background: #fff;
}

:deep(.team-profile-history-dialog .el-dialog__header) {
  margin-right: 0;
  padding-bottom: 8px;
}

:deep(.team-profile-history-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-top: 8px;
  color: #222;
}

:deep(.team-profile-history-dialog .el-dialog__footer) {
  padding-top: 8px;
}

:deep(.team-profile-detail-dialog.el-dialog) {
  width: min(64vw, 920px) !important;
  max-height: 80vh;
  margin: 10vh auto !important;
  display: flex;
  flex-direction: column;
  background: #fff;
}

:deep(.team-profile-detail-dialog .el-dialog__body) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  color: #222;
}

@media (max-width: 960px) {
  :deep(.team-profile-history-dialog.el-dialog) {
    width: min(80vw, 960px) !important;
    min-width: 0;
  }
}

@media (max-width: 720px) {
  :deep(.team-profile-history-dialog.el-dialog),
  :deep(.team-profile-detail-dialog.el-dialog) {
    width: 92vw !important;
    min-width: 0;
  }

  .team-profile-history-filters__input {
    min-width: 0;
    width: 100%;
  }

  .team-profile-card,
  .team-profile-change-detail__compare {
    grid-template-columns: 1fr;
  }

  .team-profile-status-bar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
