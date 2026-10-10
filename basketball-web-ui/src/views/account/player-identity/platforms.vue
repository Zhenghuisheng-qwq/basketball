<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { uploadAccountFile } from '@/api/accountFile'
import { useAccountStore } from '@/stores/account'
import type { IdentityPlatform, PlatformGameIdRecord } from '@/types/account'
import { ElMessage } from 'element-plus'

const accountStore = useAccountStore()
const { t, te } = useI18n()
const showAddDialog = ref(false)
const showPendingDialog = ref(false)
const submitting = ref(false)
const uploadingProof = ref(false)
const proofInputRef = ref<HTMLInputElement | null>(null)

const account = computed(() => accountStore.currentAccount)
const records = computed(() => accountStore.platformGameIds)

const addForm = reactive({
  platform: 'PC' as IdentityPlatform,
  gameId: '',
  proofFileId: '',
  proofFileName: '',
})

const pendingRecord = computed(() => records.value.find((item) => item.status === 'PENDING'))

const isAddMode = computed(() => {
  const row = records.value.find((item) => item.platform === addForm.platform)
  return !row?.platformAccountId || row.status === 'UNBOUND'
})

function platformLabel(code?: string) {
  if (!code) return '-'
  const key = `accountIdentity.platform.${code}`
  return te(key) ? t(key) : code
}

function platformStatusLabel(row: PlatformGameIdRecord) {
  if (row.status === 'UNBOUND') return t('accountIdentity.platformStatus.unbound')
  if (row.status === 'PENDING' && row.pendingKind === 'ADD') return t('accountIdentity.platformStatus.addPending')
  if (row.status === 'PENDING' && row.pendingKind === 'EDIT') return t('accountIdentity.platformStatus.editPending')
  if (row.status === 'PENDING') return t('accountIdentity.status.pending')
  if (row.status === 'EFFECTIVE') return t('accountIdentity.status.effective')
  return row.statusText || '-'
}

onMounted(async () => {
  try {
    await accountStore.loadPlayerPlatformState()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.loadPlatformFailed'))
  }
})

function openAdd(platform: IdentityPlatform) {
  addForm.platform = platform
  addForm.gameId = ''
  addForm.proofFileId = ''
  addForm.proofFileName = ''
  showAddDialog.value = true
}

function triggerProofUpload() {
  if (uploadingProof.value) return
  proofInputRef.value?.click()
}

async function handleProofUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
    ElMessage.warning(t('accountIdentity.feedback.imagesOnly'))
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning(t('accountIdentity.feedback.fileTooLarge'))
    return
  }
  uploadingProof.value = true
  try {
    const res = await uploadAccountFile(file, 'PLAYER_CERT_GAME_SCREENSHOT')
    addForm.proofFileId = res.data.fileId
    addForm.proofFileName = res.data.originalName || file.name
    ElMessage.success(t('accountIdentity.feedback.proofUploaded'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.uploadFailed'))
  } finally {
    uploadingProof.value = false
  }
}

async function submitAdd() {
  if (!addForm.gameId.trim()) {
    ElMessage.warning(t('accountIdentity.feedback.enterGameId'))
    return
  }
  if (!addForm.proofFileId) {
    ElMessage.warning(t('accountIdentity.feedback.uploadProofRequired'))
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await accountStore.submitPlatformGameId({
      platform: addForm.platform,
      gameId: addForm.gameId.trim(),
      proofFileId: addForm.proofFileId,
    })
    showAddDialog.value = false
    ElMessage.success(t('accountIdentity.feedback.submittedForReview'))
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountIdentity.feedback.submitFailed'))
  } finally {
    submitting.value = false
  }
}

function viewPending() {
  showPendingDialog.value = true
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountIdentity.platforms.pageTitle')"
      en-title="PLATFORM & GAME ID"
      :breadcrumb-items="[
        { label: t('nav.home'), to: '/home' },
        { label: t('nav.account'), to: '/account/overview' },
        { label: t('accountIdentity.title'), to: '/account/player-identity' },
        { label: t('accountIdentity.platforms.crumb') },
      ]"
    />

    <AccountStatusBanner
      :title="t('accountIdentity.platforms.bannerTitle', { id: account?.playerNo || '' })"
      :description="t('accountIdentity.platforms.bannerDesc')"
      :status-text="t('accountIdentity.status.certActive')"
      status-type="success"
    />

    <AccountPanel :title="t('accountIdentity.platforms.recordsTitle')">
      <div class="platform-table-wrap">
        <table class="platform-table">
          <thead>
            <tr>
              <th>{{ t('accountIdentity.fields.platform') }}</th>
              <th>{{ t('accountIdentity.fields.gameId') }}</th>
              <th>{{ t('accountIdentity.fields.reviewStatus') }}</th>
              <th>{{ t('accountIdentity.fields.action') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in records" :key="row.platform">
              <td>{{ platformLabel(row.platform) }}</td>
              <td>{{ row.gameId || t('accountIdentity.platformStatus.unbound') }}</td>
              <td>{{ row.status === 'UNBOUND' ? '-' : platformStatusLabel(row) }}</td>
              <td>
                <button
                  v-if="row.locked"
                  type="button"
                  class="account-btn-secondary"
                  disabled
                >
                  {{ row.lockText }}
                </button>
                <button
                  v-else-if="row.status === 'PENDING'"
                  type="button"
                  class="account-btn-secondary"
                  @click="viewPending"
                >
                  {{ t('accountIdentity.actions.viewRequest') }}
                </button>
                <button
                  v-else-if="row.status === 'UNBOUND'"
                  type="button"
                  class="account-btn-primary"
                  @click="openAdd(row.platform)"
                >
                  {{ t('accountIdentity.actions.addGameId') }}
                </button>
                <button
                  v-else
                  type="button"
                  class="account-btn-secondary"
                  @click="openAdd(row.platform)"
                >
                  {{ t('accountIdentity.actions.requestChange') }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AccountPanel>

    <AccountPanel :title="t('accountIdentity.platforms.rulesTitle')">
      <ul class="identity-entry-rules">
        <li>{{ t('accountIdentity.platforms.ruleUnique') }}</li>
        <li>{{ t('accountIdentity.platforms.ruleReview') }}</li>
        <li>{{ t('accountIdentity.platforms.ruleLocked') }}</li>
        <li>{{ t('accountIdentity.platforms.ruleKeepCurrent') }}</li>
        <li>{{ t('accountIdentity.platforms.ruleHidden') }}</li>
      </ul>
    </AccountPanel>

    <div v-if="showAddDialog" class="account-dialog-backdrop" @click.self="showAddDialog = false">
      <div class="account-dialog">
        <h3>{{ isAddMode ? t('accountIdentity.actions.addGameId') : t('accountIdentity.actions.editGameId') }}</h3>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.platform') }}</span><span>{{ platformLabel(addForm.platform) }}</span></div>
        </div>
        <input v-model="addForm.gameId" class="identity-apply-form__input" :placeholder="t('accountIdentity.platforms.gameIdPlaceholder')" />
        <input
          ref="proofInputRef"
          type="file"
          class="platform-proof-upload__input"
          accept=".jpg,.jpeg,.png,image/jpeg,image/png"
          @change="handleProofUpload"
        />
        <button type="button" class="account-btn-secondary platform-proof-upload" :disabled="uploadingProof" @click="triggerProofUpload">
          {{ uploadingProof ? t('accountIdentity.actions.uploading') : addForm.proofFileId ? t('accountIdentity.platforms.reuploadProof') : t('accountIdentity.platforms.uploadProof') }}
        </button>
        <p v-if="addForm.proofFileName" class="platform-proof-name">{{ addForm.proofFileName }}</p>
        <div class="account-dialog__actions">
          <button type="button" class="account-btn-secondary" @click="showAddDialog = false">{{ t('accountIdentity.actions.cancel') }}</button>
          <button type="button" class="account-btn-primary" :disabled="submitting" @click="submitAdd">
            {{ submitting ? t('accountIdentity.actions.submitting') : t('accountIdentity.actions.submitReview') }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showPendingDialog && pendingRecord" class="account-dialog-backdrop" @click.self="showPendingDialog = false">
      <div class="account-dialog">
        <h3>{{ t('accountIdentity.platforms.pendingTitle') }}</h3>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.platform') }}</span><span>{{ platformLabel(pendingRecord.platform) }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.gameId') }}</span><span>{{ pendingRecord.gameId || t('accountIdentity.platformStatus.awaiting') }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">{{ t('accountIdentity.fields.status') }}</span><span>{{ platformStatusLabel(pendingRecord) }}</span></div>
        </div>
        <button type="button" class="account-btn-secondary" @click="showPendingDialog = false">{{ t('accountIdentity.actions.close') }}</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.platform-proof-upload {
  display: inline-block;
  margin-top: 12px;
}

.platform-proof-upload__input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.platform-proof-name {
  margin: 8px 0 0;
  font-size: 13px;
  color: #666;
}
</style>
