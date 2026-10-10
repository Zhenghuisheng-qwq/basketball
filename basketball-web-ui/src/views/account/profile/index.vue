<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import ProfileCompleteness from '../components/ProfileCompleteness.vue'
import ProfileAvatarUploader from '../components/ProfileAvatarUploader.vue'
import { useAccountStore } from '@/stores/account'

const route = useRoute()
const router = useRouter()
const { t, locale } = useI18n()
const accountStore = useAccountStore()

const isEditing = ref(route.query.mode === 'edit')
const avatarError = ref('')
const saving = ref(false)
const avatarFile = ref<File | null>(null)

const profile = computed(() => accountStore.profile)
const completeness = computed(() => {
  locale.value
  return accountStore.getProfileCompleteness()
})

const editForm = reactive({
  nickname: '',
  avatarUrl: '',
})

const snapshot = reactive({
  nickname: '',
  avatarUrl: '',
})

onMounted(async () => {
  try {
    await accountStore.loadProfile()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.profile.loadFailed'))
  }
})

watch(
  () => route.query.mode,
  (mode) => {
    isEditing.value = mode === 'edit'
  },
)

watch(
  profile,
  (value) => {
    if (!value) return
    editForm.nickname = value.nickname
    editForm.avatarUrl = value.avatarUrl
    snapshot.nickname = value.nickname
    snapshot.avatarUrl = value.avatarUrl
  },
  { immediate: true },
)

function startEdit() {
  isEditing.value = true
  router.replace({ query: { mode: 'edit' } })
}

function cancelEdit() {
  editForm.nickname = snapshot.nickname
  editForm.avatarUrl = snapshot.avatarUrl
  avatarFile.value = null
  avatarError.value = ''
  isEditing.value = false
  router.replace({ query: {} })
}

function validateNickname(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return t('accountShell.profile.nicknameRequired')
  if (trimmed.length < 2 || trimmed.length > 20) return t('accountShell.profile.nicknameLength')
  if (!/^[一-龥a-zA-Z0-9_]+$/.test(trimmed)) {
    return t('accountShell.profile.nicknameCharset')
  }
  return ''
}

const nicknameError = ref('')

async function saveProfile() {
  nicknameError.value = validateNickname(editForm.nickname)
  if (nicknameError.value) return

  saving.value = true
  try {
    await accountStore.updateProfile({
      nickname: editForm.nickname.trim(),
      avatarUrl: editForm.avatarUrl,
      avatarFile: avatarFile.value || undefined,
    })
    // loadProfile 已同步正式资料；禁止把待审昵称/头像写入 Header
    snapshot.nickname = profile.value?.nickname || editForm.nickname.trim()
    snapshot.avatarUrl = profile.value?.avatarUrl || ''
    editForm.nickname = snapshot.nickname
    editForm.avatarUrl = snapshot.avatarUrl
    avatarFile.value = null
    isEditing.value = false
    router.replace({ query: {} })
    ElMessage.success(
      accountStore.profile?.hasPendingChange
        ? t('accountShell.profile.pendingSubmitted')
        : t('accountShell.profile.saved'),
    )
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : t('accountShell.common.saveFailed'))
  } finally {
    saving.value = false
  }
}

function handleAvatarSelected(file: File) {
  avatarFile.value = file
}

const breadcrumbs = computed(() => [
  { label: t('accountShell.common.home'), to: '/home' },
  { label: t('accountShell.common.accountCenter'), to: '/account/overview' },
  { label: t('accountShell.profile.title') },
])

const profileNotes = computed(() => [
  t('accountShell.profile.notePrivate'),
  t('accountShell.profile.noteNickname'),
  t('accountShell.profile.noteNotCert'),
  t('accountShell.profile.noteCert'),
])

const emailStatusText = computed(() =>
  profile.value?.emailVerified ? t('accountShell.common.verified') : t('accountShell.common.unverified'),
)

const bannerStatusText = computed(() =>
  profile.value?.emailVerified ? t('accountShell.common.emailVerified') : t('accountShell.common.emailUnverified'),
)
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="t('accountShell.profile.title')"
      en-title="PROFILE"
      :breadcrumb-items="breadcrumbs"
    />

    <AccountStatusBanner
      :title="isEditing ? t('accountShell.profile.completeTitle') : t('accountShell.profile.basicTitle')"
      :description="t('accountShell.profile.description')"
      :status-text="bannerStatusText"
      :status-type="profile?.emailVerified ? 'success' : 'warning'"
    />

    <div v-if="accountStore.profileLoading && !profile" class="account-view__loading">{{ t('accountShell.common.loading') }}</div>

    <div v-else class="profile-layout">
      <AccountPanel :title="t('accountShell.profile.panelTitle')">
        <template v-if="!isEditing">
          <div class="profile-read">
            <div class="profile-read__avatar">
              <img
                v-if="profile?.avatarUrl"
                :src="profile.avatarUrl"
                :alt="profile.nickname"
              />
              <span v-else>{{ profile?.nickname?.slice(0, 2).toUpperCase() }}</span>
            </div>
            <dl class="account-info-list">
              <div class="account-field-row"><dt>{{ t('accountShell.profile.nickname') }}</dt><dd>{{ profile?.nickname }}</dd></div>
              <div class="account-field-row"><dt>{{ t('accountShell.profile.userNo') }}</dt><dd>{{ profile?.userNo }}</dd></div>
              <div class="account-field-row">
                <dt>{{ t('accountShell.common.registeredEmail') }}</dt>
                <dd>{{ profile?.maskedEmail }} · {{ emailStatusText }}</dd>
              </div>
            </dl>
            <button type="button" class="account-btn-primary" @click="startEdit">{{ t('accountShell.profile.edit') }}</button>
          </div>
        </template>

        <template v-else>
          <form class="profile-form" @submit.prevent="saveProfile">
            <div class="profile-form__field">
              <label>{{ t('accountShell.profile.avatar') }}</label>
              <ProfileAvatarUploader
                v-model="editForm.avatarUrl"
                :name="editForm.nickname || profile?.nickname || 'CN'"
                :action-label="t('accountShell.profile.uploadAvatar')"
                :hint="t('accountShell.profile.avatarFileHint')"
                @error="avatarError = $event"
                @file-selected="handleAvatarSelected"
              />
              <p v-if="avatarError" class="profile-form__error">{{ avatarError }}</p>
            </div>

            <div class="profile-form__field">
              <label for="profile-nickname">{{ t('accountShell.profile.nickname') }}</label>
              <input
                id="profile-nickname"
                v-model="editForm.nickname"
                type="text"
                maxlength="20"
                autocomplete="nickname"
              />
              <p class="profile-form__hint">{{ t('accountShell.profile.nicknameHint') }}</p>
              <p v-if="nicknameError" class="profile-form__error">{{ nicknameError }}</p>
            </div>

            <div class="profile-form__field">
              <label>{{ t('accountShell.profile.userNo') }}</label>
              <div class="profile-form__readonly">
                <span>{{ profile?.userNo }}</span>
              </div>
            </div>

            <div class="profile-form__field">
              <label>{{ t('accountShell.common.registeredEmail') }}</label>
              <div class="profile-form__readonly">
                <span>{{ profile?.maskedEmail }} · {{ emailStatusText }}</span>
              </div>
              <p class="profile-form__hint">{{ t('accountShell.profile.emailHint') }}</p>
            </div>

            <div class="profile-form__actions">
              <button type="submit" class="account-btn-primary" :disabled="saving">{{ t('accountShell.profile.save') }}</button>
              <button type="button" class="account-btn-secondary" @click="cancelEdit">{{ t('accountShell.common.cancel') }}</button>
            </div>
          </form>
        </template>
      </AccountPanel>

      <AccountPanel v-if="!isEditing" :title="t('accountShell.profile.notesTitle')">
        <ProfileCompleteness :percent="completeness.percent" :items="completeness.items" />
        <ul class="profile-notes">
          <li v-for="note in profileNotes" :key="note">{{ note }}</li>
        </ul>
        <router-link to="/account/player-verification" class="profile-notes__link">
          {{ t('accountShell.profile.viewCert') }}
        </router-link>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
}

.account-view__loading {
  padding: 48px 0;
  text-align: center;
  color: #696969;
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(260px, 0.8fr);
  gap: 18px;
}

.profile-read__avatar {
  width: 96px;
  height: 96px;
  margin-bottom: 16px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: #ddd;
  font-weight: 900;
  font-size: 22px;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.profile-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-form__field label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 800;
}

.profile-form__field input {
  width: 100%;
  height: 48px;
  padding: 0 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.5);
  font-family: inherit;
  font-size: 14px;
  box-sizing: border-box;
}

.profile-form__readonly {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid rgba(20, 20, 20, 0.12);
  border-radius: 3px;
  background: rgba(20, 20, 20, 0.04);
  color: #696969;
}

.profile-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #696969;
}

.profile-form__error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--account-danger);
}

.profile-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.profile-notes {
  margin: 16px 0;
  padding-left: 18px;
  font-size: 13px;
  color: #696969;
  line-height: 1.7;
}

.profile-notes__link {
  color: var(--account-red);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}

@media (max-width: 991px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}
</style>
