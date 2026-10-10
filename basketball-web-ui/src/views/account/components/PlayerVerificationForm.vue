<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { GamePlatform, GamePosition, PlayerVerificationSnapshot } from '@/types/account'

const props = defineProps<{
  initial?: Partial<PlayerVerificationSnapshot>
  rejectedFields?: string[]
  submitLabel?: string
  showCancel?: boolean
}>()

const emit = defineEmits<{
  submit: [data: PlayerVerificationSnapshot]
  cancel: []
}>()

const { t } = useI18n()

const form = reactive<PlayerVerificationSnapshot>({
  primaryPlatform: props.initial?.primaryPlatform || 'PC',
  gameId: props.initial?.gameId || '',
  gameIdScreenshotUrl: props.initial?.gameIdScreenshotUrl || '',
  gamePosition: props.initial?.gamePosition || 'PG',
  commonPosition: props.initial?.commonPosition || 'PG',
  backupContact: '',
})

const errors = reactive<Record<string, string>>({})
const screenshotPreview = ref(form.gameIdScreenshotUrl)
const loading = ref(false)

const platformCodes: GamePlatform[] = ['PC', 'PS5', 'XBOX', 'MOBILE']
const positionCodes: GamePosition[] = ['PG', 'SG', 'SF', 'PF', 'C']

function validate(): boolean {
  errors.primaryPlatform = form.primaryPlatform ? '' : t('accountIdentity.feedback.selectPlatform')
  const gameId = form.gameId.trim()
  if (!gameId) errors.gameId = t('accountIdentity.feedback.enterGameIdField')
  else if (gameId.length < 2 || gameId.length > 32) errors.gameId = t('accountIdentity.feedback.gameIdLength')
  else errors.gameId = ''
  errors.gameIdScreenshotUrl = form.gameIdScreenshotUrl ? '' : t('accountIdentity.feedback.uploadScreenshot')
  errors.gamePosition = form.gamePosition ? '' : t('accountIdentity.feedback.selectGamePosition')
  errors.commonPosition = form.commonPosition ? '' : t('accountIdentity.feedback.selectPosition')
  return !Object.values(errors).some(Boolean)
}

async function compressScreenshot(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const img = new Image()
      img.onload = () => {
        const maxSize = 1024
        let { width, height } = img
        const scale = Math.min(1, maxSize / Math.max(width, height))
        width = Math.round(width * scale)
        height = Math.round(height * scale)
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error(t('accountIdentity.feedback.imageProcessFailed')))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.8))
      }
      img.onerror = () => reject(new Error(t('accountIdentity.feedback.imageLoadFailed')))
      img.src = reader.result as string
    }
    reader.onerror = () => reject(new Error(t('accountIdentity.feedback.readFailed')))
    reader.readAsDataURL(file)
  })
}

async function handleScreenshot(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
    errors.gameIdScreenshotUrl = t('accountIdentity.feedback.imagesOnly')
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    errors.gameIdScreenshotUrl = t('accountIdentity.feedback.screenshotTooLarge')
    return
  }
  try {
    const dataUrl = await compressScreenshot(file)
    form.gameIdScreenshotUrl = dataUrl
    screenshotPreview.value = dataUrl
    errors.gameIdScreenshotUrl = ''
  } catch {
    errors.gameIdScreenshotUrl = t('accountIdentity.feedback.screenshotFailed')
  }
}

function removeScreenshot() {
  form.gameIdScreenshotUrl = ''
  screenshotPreview.value = ''
}

function handleSubmit() {
  if (loading.value || !validate()) return
  loading.value = true
  emit('submit', {
    ...form,
    gameId: form.gameId.trim(),
    backupContact: '',
  })
  loading.value = false
}
</script>

<template>
  <form class="player-verification-form verification-form" @submit.prevent="handleSubmit">
    <div class="verification-form__field">
      <label for="primaryPlatform">{{ t('accountIdentity.fields.primaryPlatform') }}</label>
      <select id="primaryPlatform" v-model="form.primaryPlatform">
        <option v-for="code in platformCodes" :key="code" :value="code">{{ t(`accountIdentity.platform.${code}`) }}</option>
      </select>
      <p v-if="errors.primaryPlatform" class="verification-form__error">{{ errors.primaryPlatform }}</p>
    </div>

    <div class="verification-form__field">
      <label for="gameId">{{ t('accountIdentity.fields.gameId') }}</label>
      <input id="gameId" v-model="form.gameId" type="text" maxlength="32" />
      <p v-if="errors.gameId" class="verification-form__error">{{ errors.gameId }}</p>
    </div>

    <div class="verification-form__field" :class="{ 'verification-form__field--error': rejectedFields?.includes('gameIdScreenshotUrl') }">
      <label>{{ t('accountIdentity.fields.screenshot') }}</label>
      <div v-if="screenshotPreview" class="verification-form__preview">
        <img :src="screenshotPreview" :alt="t('accountIdentity.verification.screenshotAlt')" />
        <button type="button" class="account-btn-secondary" @click="removeScreenshot">{{ t('accountIdentity.actions.delete') }}</button>
      </div>
      <p class="verification-form__hint">{{ t('accountIdentity.verification.screenshotHint') }}</p>
      <label v-else class="verification-form__upload">
        <input type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" @change="handleScreenshot" />
        {{ t('accountIdentity.verification.uploadScreenshot') }}
      </label>
      <p v-if="rejectedFields?.includes('gameIdScreenshotUrl')" class="verification-form__error">{{ t('accountIdentity.feedback.reuploadRequired') }}</p>
      <p v-if="errors.gameIdScreenshotUrl" class="verification-form__error">{{ errors.gameIdScreenshotUrl }}</p>
    </div>

    <div class="verification-form__field">
      <label for="gamePosition">{{ t('accountIdentity.fields.gamePosition2k') }}</label>
      <select id="gamePosition" v-model="form.gamePosition">
        <option v-for="code in positionCodes" :key="code" :value="code">{{ t(`accountIdentity.position.${code}`) }}</option>
      </select>
    </div>

    <div class="verification-form__field">
      <label for="commonPosition">{{ t('accountIdentity.fields.position') }}</label>
      <select id="commonPosition" v-model="form.commonPosition">
        <option v-for="code in positionCodes" :key="`common-${code}`" :value="code">{{ t(`accountIdentity.position.${code}`) }}</option>
      </select>
    </div>

    <div class="verification-form__actions">
      <button type="submit" class="account-btn-primary" :disabled="loading">
        {{ submitLabel || t('accountIdentity.actions.submitCert') }}
      </button>
      <button v-if="showCancel" type="button" class="account-btn-secondary" @click="emit('cancel')">{{ t('accountIdentity.actions.cancel') }}</button>
    </div>
  </form>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.verification-form__field--error input,
.verification-form__field--error select {
  border-color: var(--account-danger);
}

.verification-form__preview img {
  max-width: 100%;
  max-height: 200px;
  border: 1px solid rgba(20, 20, 20, 0.15);
  border-radius: 3px;
}
</style>
