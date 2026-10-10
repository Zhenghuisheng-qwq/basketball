<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { GamePosition, IdentityPlatform } from '@/types/account'
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

const { t } = useI18n()

const avatarUrl = ref(props.initial?.avatarUrl || '')
const avatarFile = ref<File | null>(null)
const avatarError = ref('')
const platform = ref<IdentityPlatform>(props.initial?.platform || 'XBOX')
const gameAccount = ref(props.initial?.gameAccount || '')
const position = ref<GamePosition>(props.initial?.position || 'PG')
const gameScreenshotFile = ref<File | null>(null)

const screenshotName = computed(() => gameScreenshotFile.value?.name || '')

const hasAvatar = computed(() => Boolean(avatarUrl.value.trim()))

const platformCodes: IdentityPlatform[] = ['PC', 'PS', 'XBOX']
const positionCodes: GamePosition[] = ['PG', 'SG', 'SF', 'PF', 'C']

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
      <label>{{ t('accountIdentity.fields.playerAvatar') }}</label>
      <ProfileAvatarUploader
        v-model="avatarUrl"
        :name="t('accountIdentity.fields.playerAvatar')"
        @file-selected="handleAvatarSelected"
        @error="avatarError = $event"
      />
      <p v-if="avatarError" class="identity-apply-form__error">{{ avatarError }}</p>
      <p v-else-if="!hasAvatar" class="identity-apply-form__hint">{{ t('accountIdentity.apply.avatarRequired') }}</p>
    </div>

    <div class="identity-apply-form__field">
      <label>{{ t('accountIdentity.fields.gamePlatform') }}</label>
      <div class="identity-apply-form__chips">
        <button
          v-for="code in platformCodes"
          :key="code"
          type="button"
          class="identity-apply-form__chip"
          :class="{ 'identity-apply-form__chip--active': platform === code }"
          @click="platform = code"
        >
          {{ t(`accountIdentity.platform.${code}`) }}
        </button>
      </div>
    </div>

    <div class="identity-apply-form__field">
      <label>{{ t('accountIdentity.fields.gameId') }}</label>
      <input v-model="gameAccount" type="text" class="identity-apply-form__input" :placeholder="t('accountIdentity.apply.gameIdPlaceholder')" />
    </div>

    <div class="identity-apply-form__field">
      <label>{{ t('accountIdentity.fields.position') }}</label>
      <div class="identity-apply-form__chips">
        <button
          v-for="code in positionCodes"
          :key="code"
          type="button"
          class="identity-apply-form__chip identity-apply-form__chip--compact"
          :class="{ 'identity-apply-form__chip--active': position === code }"
          @click="position = code"
        >
          {{ code }}
        </button>
      </div>
    </div>

    <div class="identity-apply-form__field">
      <label>{{ t('accountIdentity.fields.proofAccount') }}</label>
      <div class="identity-apply-form__proof">
        <p>{{ t('accountIdentity.apply.proofHint') }}</p>
        <label class="account-btn-secondary identity-apply-form__upload-btn">
          {{ screenshotName || t('accountIdentity.apply.uploadProof') }}
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
