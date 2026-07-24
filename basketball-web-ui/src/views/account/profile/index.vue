<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import ProfileCompleteness from '../components/ProfileCompleteness.vue'
import ProfileAvatarUploader from '../components/ProfileAvatarUploader.vue'
import { useAccountStore } from '@/stores/account'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const userStore = useUserStore()

const isEditing = ref(route.query.mode === 'edit')
const avatarError = ref('')
const saving = ref(false)
const avatarFile = ref<File | null>(null)

const profile = computed(() => accountStore.profile)
const completeness = computed(() => accountStore.getProfileCompleteness())

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
    ElMessage.error(error instanceof Error ? error.message : '加载资料失败')
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
  if (!trimmed) return '请输入个人昵称'
  if (trimmed.length < 2 || trimmed.length > 20) return '昵称长度应为 2 至 20 个字符'
  if (!/^[一-龥a-zA-Z0-9_]+$/.test(trimmed)) {
    return '昵称仅支持中文、英文、数字和下划线'
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
    userStore.setUserInfo({
      id: profile.value?.userId,
      nickname: editForm.nickname.trim(),
      avatar: editForm.avatarUrl || undefined,
    })
    snapshot.nickname = editForm.nickname.trim()
    snapshot.avatarUrl = editForm.avatarUrl
    avatarFile.value = null
    isEditing.value = false
    router.replace({ query: {} })
    ElMessage.success('资料已保存')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

function handleAvatarSelected(file: File) {
  avatarFile.value = file
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="个人资料"
      en-title="PROFILE"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '个人资料' },
      ]"
    />

    <AccountStatusBanner
      :title="isEditing ? '完善基础资料' : '基础资料'"
      description="这些资料用于账号识别与个人中心展示"
      :status-text="profile?.emailVerified ? '邮箱已验证' : '邮箱未验证'"
      :status-type="profile?.emailVerified ? 'success' : 'warning'"
    />

    <div v-if="accountStore.profileLoading && !profile" class="account-view__loading">加载中...</div>

    <div v-else class="profile-layout">
      <AccountPanel title="基础资料">
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
              <div class="account-field-row"><dt>个人昵称</dt><dd>{{ profile?.nickname }}</dd></div>
              <div class="account-field-row"><dt>用户编号</dt><dd>{{ profile?.userNo }}</dd></div>
              <div class="account-field-row">
                <dt>注册邮箱</dt>
                <dd>{{ profile?.maskedEmail }} · {{ profile?.emailVerified ? '已验证' : '未验证' }}</dd>
              </div>
            </dl>
            <button type="button" class="account-btn-primary" @click="startEdit">修改个人资料</button>
          </div>
        </template>

        <template v-else>
          <form class="profile-form" @submit.prevent="saveProfile">
            <div class="profile-form__field">
              <label>头像</label>
              <ProfileAvatarUploader
                v-model="editForm.avatarUrl"
                :name="editForm.nickname || profile?.nickname || 'CN'"
                @error="avatarError = $event"
                @file-selected="handleAvatarSelected"
              />
              <p v-if="avatarError" class="profile-form__error">{{ avatarError }}</p>
            </div>

            <div class="profile-form__field">
              <label for="profile-nickname">个人昵称</label>
              <input
                id="profile-nickname"
                v-model="editForm.nickname"
                type="text"
                maxlength="20"
                autocomplete="nickname"
              />
              <p class="profile-form__hint">用于个人中心显示，不等同于赛事游戏 ID</p>
              <p v-if="nicknameError" class="profile-form__error">{{ nicknameError }}</p>
            </div>

            <div class="profile-form__field">
              <label>用户编号</label>
              <div class="profile-form__readonly">
                <span>{{ profile?.userNo }}</span>
              </div>
            </div>

            <div class="profile-form__field">
              <label>注册邮箱</label>
              <div class="profile-form__readonly">
                <span>{{ profile?.maskedEmail }} · {{ profile?.emailVerified ? '已验证' : '未验证' }}</span>
              </div>
              <p class="profile-form__hint">修改邮箱需前往账号安全</p>
            </div>

            <div class="profile-form__actions">
              <button type="submit" class="account-btn-primary" :disabled="saving">保存修改</button>
              <button type="button" class="account-btn-secondary" @click="cancelEdit">取消</button>
            </div>
          </form>
        </template>
      </AccountPanel>

      <AccountPanel v-if="!isEditing" title="资料说明">
        <ProfileCompleteness :percent="completeness.percent" :items="completeness.items" />
        <ul class="profile-notes">
          <li>用户编号和注册邮箱不会公开展示。</li>
          <li>个人昵称仅用于账号与个人中心识别。</li>
          <li>头像和昵称不等同于赛事认证资料。</li>
          <li>游戏平台、游戏 ID 和角色位置需要在球员认证中提交。</li>
        </ul>
        <router-link to="/account/player-verification" class="profile-notes__link">
          查看球员认证说明
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
