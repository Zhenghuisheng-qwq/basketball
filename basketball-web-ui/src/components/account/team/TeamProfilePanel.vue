<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountTeamLogo from '@/components/account/team/AccountTeamLogo.vue'
import MyEventsPagination from '@/components/account/events/MyEventsPagination.vue'
import { uploadAccountFile } from '@/api/accountFile'
import { updateTeam } from '@/api/accountTeam'
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

const props = defineProps<{
  teamId: string
  teamDetail?: TeamDetailDto | null
  canManage: boolean
  isApproved: boolean
}>()

const emit = defineEmits<{
  refreshed: []
}>()

const PROFILE_CHANGE_STATUS: Record<number, string> = {
  1: '待审核',
  2: '审核中',
  3: '已通过',
  4: '已驳回',
  5: '已取消',
}

const HISTORY_STATUS_OPTIONS = [
  { label: '全部', value: '' as const },
  { label: '待审核', value: 1 },
  { label: '已通过', value: 3 },
  { label: '已驳回', value: 4 },
  { label: '已取消', value: 5 },
]

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
const useAuditFlow = computed(() => props.isApproved && props.canManage)

const displayAuditStatusText = computed(() => {
  if (hasPendingChange.value) {
    const status = pendingChange.value?.auditStatus
    return status != null
      ? PROFILE_CHANGE_STATUS[status] || pendingChange.value?.auditStatusText || '待审核'
      : '待审核'
  }
  const latest = recentHistory.value[0]
  if (latest?.auditStatus === 4) return '已驳回'
  if (latest?.auditStatus === 3) return '已通过'
  return '无待审核'
})

const displayAuditStatusType = computed(() => {
  const text = displayAuditStatusText.value
  if (text === '待审核' || text === '审核中') return 'warning'
  if (text === '已驳回') return 'danger'
  if (text === '已通过') return 'success'
  return 'default'
})

const showPendingHint = computed(() =>
  hasPendingChange.value
  && (pendingChange.value?.auditStatus === 1 || pendingChange.value?.auditStatus === 2),
)

const latestRejectedRemark = computed(() => {
  if (pendingChange.value?.auditStatus === 4) return pendingChange.value.auditRemark
  return recentHistory.value.find(item => item.auditStatus === 4)?.auditRemark
})

const showRejectedHint = computed(() =>
  !hasPendingChange.value && Boolean(latestRejectedRemark.value),
)

const historyHasFilter = computed(() =>
  historyAuditStatus.value !== '' || historyKeyword.value.trim().length > 0,
)

const historyEmptyText = computed(() =>
  historyHasFilter.value ? '暂无符合条件的审核历史' : '暂无审核历史记录',
)

const submitButtonText = computed(() => {
  if (!useAuditFlow.value) return '保存资料'
  return hasPendingChange.value ? '保存修改并继续待审' : '提交审核'
})

function historyStatusType(status?: number): 'success' | 'warning' | 'danger' | 'default' {
  if (status === 3) return 'success'
  if (status === 1 || status === 2) return 'warning'
  if (status === 4) return 'danger'
  return 'default'
}

function publicStatusText(status?: number) {
  return status === 1 ? '公开' : '非公开'
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
    ElMessage.error(error instanceof Error ? error.message : '加载资料审核信息失败')
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
  if (!props.canManage) return
  showEditDialog.value = true
  await prepareEditForm()
}

function openPendingEdit() {
  openEditDialog().catch(() => undefined)
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
      ElMessage.success('Logo 已更新')
      emit('refreshed')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Logo 上传失败')
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
  ElMessage.success('球队资料已保存')
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
    ElMessage.success(hasPendingChange.value ? '变更申请已更新' : '变更申请已提交，等待审核')
    showEditDialog.value = false
    profileLogoFileId.value = ''
    await loadProfileEditData()
    emit('refreshed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleSubmit() {
  if (!props.canManage) return
  if (!profileForm.teamName.trim()) {
    ElMessage.warning('请输入球队名称')
    return
  }
  if (useAuditFlow.value) {
    await submitAuditProfile()
  } else {
    actionLoading.value = true
    try {
      await submitDirectProfile()
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '保存失败')
    } finally {
      actionLoading.value = false
    }
  }
}

async function handleCancelPending() {
  if (!props.teamId || !pendingChange.value?.changeId) return
  try {
    await ElMessageBox.confirm(
      '确认取消本次球队资料变更申请？取消后正式资料不会变化。',
      '取消申请',
      { type: 'warning' },
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await cancelTeamProfileChange(props.teamId, pendingChange.value.changeId)
    ElMessage.success('变更申请已取消')
    showEditDialog.value = false
    await loadProfileEditData()
    emit('refreshed')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '取消失败')
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
    ElMessage.error(error instanceof Error ? error.message : '加载历史记录失败')
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
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
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
    <AccountPanel title="当前正式资料">
      <template v-if="canManage" #extra>
        <div class="team-profile-panel__actions">
          <button type="button" class="account-btn-secondary" @click="openEditDialog">编辑资料</button>
          <button
            v-if="useAuditFlow"
            type="button"
            class="account-btn-secondary"
            @click="openHistoryDialog"
          >
            审核历史
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
            <dt>球队名称</dt>
            <dd>{{ officialProfile?.teamName || '-' }}</dd>
          </div>
          <div class="account-field-row">
            <dt>球队简称</dt>
            <dd>{{ officialProfile?.shortName || '-' }}</dd>
          </div>
          <div class="account-field-row">
            <dt>公开状态</dt>
            <dd>{{ publicStatusText(officialProfile?.publicStatus) }}</dd>
          </div>
          <div class="account-field-row">
            <dt>球队介绍</dt>
            <dd>{{ officialProfile?.description || '暂无' }}</dd>
          </div>
        </dl>
      </div>

      <div v-if="useAuditFlow" class="team-profile-status-bar">
        <div class="team-profile-status-bar__main">
          <p v-if="showPendingHint" class="team-profile-status-bar__text">
            当前存在待审核的球队资料变更，审核通过前不会影响正式资料。
          </p>
          <p v-else-if="showRejectedHint" class="team-profile-status-bar__text team-profile-status-bar__text--danger">
            最近一次变更被驳回{{ latestRejectedRemark ? `：${latestRejectedRemark}` : '' }}。
          </p>
          <p v-else class="team-profile-status-bar__text team-profile-status-bar__text--muted">
            资料变更需提交审核，通过后才会更新正式展示内容。
          </p>

          <div v-if="showPendingHint" class="team-profile-status-bar__links">
            <button type="button" class="team-profile-link-btn" @click="openPendingEdit">查看待审内容</button>
            <button type="button" class="team-profile-link-btn team-profile-link-btn--danger" @click="handleCancelPending">
              取消申请
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
      title="编辑球队资料"
      width="92%"
      class="team-profile-dialog"
      append-to-body
      destroy-on-close
    >
      <div v-loading="editDialogLoading" class="team-profile-edit">
        <p v-if="showPendingHint" class="team-profile-edit__hint">
          当前已有待审核申请，本次修改将更新同一条待审核记录，不会新增重复申请。
        </p>
        <p v-else-if="showRejectedHint" class="team-profile-edit__hint team-profile-edit__hint--danger">
          上次变更被驳回，请修改后重新提交{{ latestRejectedRemark ? `（原因：${latestRejectedRemark}）` : '' }}。
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
              更换 Logo
              <input type="file" accept="image/*" hidden @change="handleLogoUpload" />
            </label>
            <p class="team-tip">
              {{ useAuditFlow ? '上传后需提交审核，通过后才会更新正式 Logo。' : '支持 JPG / PNG，上传后立即保存。' }}
            </p>
          </div>
        </div>

        <div class="team-form-grid">
          <label>球队名称<input v-model="profileForm.teamName" type="text" maxlength="30" /></label>
          <label>球队简称<input v-model="profileForm.shortName" type="text" maxlength="12" /></label>
          <label>
            公开状态
            <select v-model.number="profileForm.publicStatus">
              <option :value="1">公开</option>
              <option :value="0">非公开</option>
            </select>
          </label>
          <label class="team-form-full">
            球队介绍
            <textarea v-model="profileForm.description" rows="3" maxlength="500" />
          </label>
          <label v-if="useAuditFlow" class="team-form-full">
            变更说明（可选）
            <textarea v-model="profileForm.changeReason" rows="2" maxlength="200" placeholder="简要说明本次资料变更原因" />
          </label>
        </div>

        <p v-if="useAuditFlow" class="team-tip">公开状态修改后立即生效，无需审核。</p>
      </div>

      <template #footer>
        <div class="team-profile-edit__footer">
          <div v-if="useAuditFlow && showPendingHint" class="team-profile-edit__footer-left">
            <button type="button" class="team-profile-link-btn team-profile-link-btn--danger" @click="handleCancelPending">
              取消申请
            </button>
          </div>
          <div class="team-profile-edit__footer-right">
            <button type="button" class="account-btn-secondary" @click="showEditDialog = false">取消</button>
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
      title="球队资料审核历史"
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
            审核状态
            <select v-model="historyAuditStatus" class="team-profile-history-filters__select" @change="handleHistoryFilter">
              <option v-for="item in HISTORY_STATUS_OPTIONS" :key="String(item.value)" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="team-profile-history-filters__keyword">
            关键词
            <input
              v-model="historyKeyword"
              type="text"
              class="team-profile-history-filters__input"
              placeholder="变更编号 / 备注"
              @keyup.enter="handleHistoryFilter"
            />
          </label>
          <div class="team-profile-history-filters__actions">
            <button type="button" class="account-btn-secondary" @click="handleHistoryFilter">搜索</button>
            <button type="button" class="account-btn-secondary" @click="handleHistoryReset">重置</button>
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
            <el-table-column label="变更编号" prop="changeNo" min-width="120" show-overflow-tooltip />
            <el-table-column label="变更摘要" prop="changeSummary" min-width="100" show-overflow-tooltip />
            <el-table-column label="审核状态" width="92">
              <template #default="{ row }">
                <AccountStatusBadge
                  :text="row.auditStatusText || '-'"
                  :type="historyStatusType(row.auditStatus)"
                />
              </template>
            </el-table-column>
            <el-table-column label="提交时间" min-width="132">
              <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
            </el-table-column>
            <el-table-column label="审核时间" min-width="132">
              <template #default="{ row }">{{ row.auditTime ? formatDateTime(row.auditTime) : '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="88" align="center">
              <template #default="{ row }">
                <button type="button" class="team-profile-link-btn" @click="openDetail(row.changeId)">查看详情</button>
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
        <button type="button" class="account-btn-secondary" @click="showHistoryDialog = false">关闭</button>
      </template>
    </el-dialog>

    <!-- 历史详情弹窗 -->
    <el-dialog
      v-model="showDetailDialog"
      title="变更详情"
      width="64%"
      align-center
      class="team-profile-detail-dialog"
      modal-class="team-profile-history-overlay"
      append-to-body
    >
      <div v-loading="detailLoading" class="team-profile-change-detail team-profile-detail-body">
        <template v-if="changeDetail">
          <div class="team-profile-detail-head">
            <p>变更编号：{{ changeDetail.changeNo }}</p>
            <AccountStatusBadge
              :text="changeDetail.auditStatusText || '-'"
              :type="historyStatusType(changeDetail.auditStatus)"
            />
          </div>
          <div class="team-profile-change-detail__compare">
            <div class="team-profile-change-detail__col">
              <h4>变更前</h4>
              <p>球队名称：{{ changeDetail.oldTeamName || '-' }}</p>
              <p>球队简称：{{ changeDetail.oldTeamShortName || '-' }}</p>
              <p>公开状态：{{ publicStatusText(changeDetail.oldPublicStatus) }}</p>
              <p>球队介绍：{{ changeDetail.oldDescription || '暂无' }}</p>
              <p class="team-profile-change-detail__logo-label">Logo</p>
              <AccountTeamLogo
                :logo-url="changeDetail.oldLogoUrl"
                :team-name="changeDetail.oldTeamName"
                size="sm"
              />
              <p v-if="!changeDetail.oldLogoUrl" class="team-tip">未设置</p>
            </div>
            <div class="team-profile-change-detail__col">
              <h4>变更后</h4>
              <p>球队名称：{{ changeDetail.newTeamName || '-' }}</p>
              <p>球队简称：{{ changeDetail.newTeamShortName || '-' }}</p>
              <p>公开状态：{{ publicStatusText(changeDetail.newPublicStatus) }}</p>
              <p>球队介绍：{{ changeDetail.newDescription || '暂无' }}</p>
              <p class="team-profile-change-detail__logo-label">Logo</p>
              <AccountTeamLogo
                :logo-url="changeDetail.newLogoUrl"
                :team-name="changeDetail.newTeamName"
                size="sm"
              />
              <p v-if="!changeDetail.newLogoUrl" class="team-tip">未设置</p>
            </div>
          </div>

          <div class="team-profile-change-detail__rows">
            <p>球队名称：{{ formatCompare(changeDetail.oldTeamName, changeDetail.newTeamName) }}</p>
            <p>球队简称：{{ formatCompare(changeDetail.oldTeamShortName, changeDetail.newTeamShortName) }}</p>
            <p>公开状态：{{ formatCompare(publicStatusText(changeDetail.oldPublicStatus), publicStatusText(changeDetail.newPublicStatus)) }}</p>
            <p>球队介绍：{{ formatCompare(changeDetail.oldDescription, changeDetail.newDescription) }}</p>
          </div>

          <div class="team-profile-change-detail__meta">
            <p>提交人：{{ changeDetail.submitUserName || changeDetail.submitPlayerNo || '-' }}</p>
            <p>提交时间：{{ formatDateTime(changeDetail.submitTime) }}</p>
            <p>审核状态：{{ changeDetail.auditStatusText }}</p>
            <p v-if="changeDetail.auditTime">审核时间：{{ formatDateTime(changeDetail.auditTime) }}</p>
            <p v-if="changeDetail.auditRemark">审核备注：{{ changeDetail.auditRemark }}</p>
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
