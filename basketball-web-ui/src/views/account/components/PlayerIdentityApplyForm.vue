<script setup lang="ts">
import { computed, ref } from 'vue'
import type { GamePosition, IdentityPlatform } from '@/types/account'
import { IDENTITY_PLATFORM_LABELS, GAME_POSITION_LABELS } from '@/types/account'
import ProfileAvatarUploader from './ProfileAvatarUploader.vue'

export interface PlayerCertFormPayload {
  platform: IdentityPlatform
  gameAccount: string
  position: GamePosition
  avatarUrl: string
  avatarFile?: File
  gameScreenshotFile?: File
}

const props = defineProps<{
  initial?: Partial<{
    avatarUrl: string
    platform: IdentityPlatform
    gameAccount: string
    position: GamePosition
  }>
}>()

const emit = defineEmits<{
  submit: [data: PlayerCertFormPayload]
}>()

const avatarUrl = ref(props.initial?.avatarUrl || '')
const avatarFile = ref<File | null>(null)
const avatarError = ref('')
const platform = ref<IdentityPlatform>(props.initial?.platform || 'XBOX')
const gameAccount = ref(props.initial?.gameAccount || '')
const position = ref<GamePosition>(props.initial?.position || 'PG')
const gameScreenshotFile = ref<File | null>(null)

const screenshotName = computed(() => gameScreenshotFile.value?.name || '')

const hasAvatar = computed(() => Boolean(avatarUrl.value.trim()))

const platforms = Object.entries(IDENTITY_PLATFORM_LABELS) as [IdentityPlatform, string][]
const positions = Object.entries(GAME_POSITION_LABELS) as [GamePosition, string][]

const isComplete = computed(() =>
  Boolean(
    hasAvatar.value
    && platform.value
    && gameAccount.value.trim()
    && position.value
    && gameScreenshotFile.value,
  ),
)

function handleAvatarSelected(file: File) {
  avatarFile.value = file
  avatarError.value = ''
}

function pickScreenshot(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  gameScreenshotFile.value = file
}

function submit() {
  if (!isComplete.value) return
  emit('submit', {
    platform: platform.value,
    gameAccount: gameAccount.value.trim(),
    position: position.value,
    avatarUrl: avatarUrl.value,
    avatarFile: avatarFile.value || undefined,
    gameScreenshotFile: gameScreenshotFile.value || undefined,
  })
}
</script>

<template>
  <div class="identity-apply-form">
    <div class="identity-apply-form__field">
      <label>球员头像</label>
      <ProfileAvatarUploader
        v-model="avatarUrl"
        name="球员头像"
        @file-selected="handleAvatarSelected"
        @error="avatarError = $event"
      />
      <p v-if="avatarError" class="identity-apply-form__error">{{ avatarError }}</p>
      <p v-else-if="!hasAvatar" class="identity-apply-form__hint">提交前请先上传球员头像</p>
    </div>

    <div class="identity-apply-form__field">
      <label>游戏平台</label>
      <div class="identity-apply-form__chips">
        <button
          v-for="[key, label] in platforms"
          :key="key"
          type="button"
          class="identity-apply-form__chip"
          :class="{ 'identity-apply-form__chip--active': platform === key }"
          @click="platform = key"
        >
          {{ label }}
        </button>
      </div>
    </div>

    <div class="identity-apply-form__field">
      <label>游戏 ID</label>
      <input v-model="gameAccount" type="text" class="identity-apply-form__input" placeholder="平台游戏 ID" />
    </div>

    <div class="identity-apply-form__field">
      <label>常用位置</label>
      <div class="identity-apply-form__chips">
        <button
          v-for="[key] in positions"
          :key="key"
          type="button"
          class="identity-apply-form__chip identity-apply-form__chip--compact"
          :class="{ 'identity-apply-form__chip--active': position === key }"
          @click="position = key"
        >
          {{ key }}
        </button>
      </div>
    </div>

    <div class="identity-apply-form__field">
      <label>游戏账号证明</label>
      <div class="identity-apply-form__proof">
        <p>请上传能够清晰显示游戏平台与游戏 ID 的证明截图。</p>
        <label class="account-btn-secondary identity-apply-form__upload-btn">
          {{ screenshotName || '上传证明截图' }}
          <input type="file" accept="image/*" hidden @change="pickScreenshot" />
        </label>
      </div>
    </div>

    <div class="identity-apply-form__actions">
      <slot name="actions" :complete="isComplete" :submit="submit" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.identity-apply-form__error {
  margin: 0;
  font-size: 12px;
  color: var(--account-red);
}

.identity-apply-form__hint {
  margin: 0;
  font-size: 12px;
  color: #696969;
}
</style>
