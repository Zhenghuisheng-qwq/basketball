<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { GamePlatform, GamePosition, PlayerVerificationSnapshot } from '@/types/account'
import { GAME_PLATFORM_LABELS, GAME_POSITION_LABELS } from '@/types/account'

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

const platforms = Object.entries(GAME_PLATFORM_LABELS) as [GamePlatform, string][]
const positions = Object.entries(GAME_POSITION_LABELS) as [GamePosition, string][]

function validate(): boolean {
  errors.primaryPlatform = form.primaryPlatform ? '' : '请选择主要游戏平台'
  const gameId = form.gameId.trim()
  if (!gameId) errors.gameId = '请输入游戏 ID'
  else if (gameId.length < 2 || gameId.length > 32) errors.gameId = '游戏 ID 长度应为 2 至 32 个字符'
  else errors.gameId = ''
  errors.gameIdScreenshotUrl = form.gameIdScreenshotUrl ? '' : '请上传游戏 ID 截图'
  errors.gamePosition = form.gamePosition ? '' : '请选择 2K 角色位置'
  errors.commonPosition = form.commonPosition ? '' : '请选择常用位置'
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
          reject(new Error('无法处理图片'))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.8))
      }
      img.onerror = () => reject(new Error('图片加载失败'))
      img.src = reader.result as string
    }
    reader.onerror = () => reject(new Error('读取失败'))
    reader.readAsDataURL(file)
  })
}

async function handleScreenshot(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
    errors.gameIdScreenshotUrl = '仅支持 JPG、JPEG、PNG'
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    errors.gameIdScreenshotUrl = '截图大小不能超过 8MB'
    return
  }
  try {
    const dataUrl = await compressScreenshot(file)
    form.gameIdScreenshotUrl = dataUrl
    screenshotPreview.value = dataUrl
    errors.gameIdScreenshotUrl = ''
  } catch {
    errors.gameIdScreenshotUrl = '截图上传失败'
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
      <label for="primaryPlatform">主要游戏平台</label>
      <select id="primaryPlatform" v-model="form.primaryPlatform">
        <option v-for="[value, label] in platforms" :key="value" :value="value">{{ label }}</option>
      </select>
      <p v-if="errors.primaryPlatform" class="verification-form__error">{{ errors.primaryPlatform }}</p>
    </div>

    <div class="verification-form__field">
      <label for="gameId">游戏 ID</label>
      <input id="gameId" v-model="form.gameId" type="text" maxlength="32" />
      <p v-if="errors.gameId" class="verification-form__error">{{ errors.gameId }}</p>
    </div>

    <div class="verification-form__field" :class="{ 'verification-form__field--error': rejectedFields?.includes('gameIdScreenshotUrl') }">
      <label>游戏 ID 截图</label>
      <div v-if="screenshotPreview" class="verification-form__preview">
        <img :src="screenshotPreview" alt="游戏 ID 截图预览" />
        <button type="button" class="account-btn-secondary" @click="removeScreenshot">删除</button>
      </div>
      <p class="verification-form__hint">请上传清晰的游戏 ID 截图，需包含平台昵称与 ID 信息</p>
      <label v-else class="verification-form__upload">
        <input type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" @change="handleScreenshot" />
        上传截图
      </label>
      <p v-if="rejectedFields?.includes('gameIdScreenshotUrl')" class="verification-form__error">需重新上传</p>
      <p v-if="errors.gameIdScreenshotUrl" class="verification-form__error">{{ errors.gameIdScreenshotUrl }}</p>
    </div>

    <div class="verification-form__field">
      <label for="gamePosition">2K 角色位置</label>
      <select id="gamePosition" v-model="form.gamePosition">
        <option v-for="[value, label] in positions" :key="value" :value="value">{{ label }}</option>
      </select>
    </div>

    <div class="verification-form__field">
      <label for="commonPosition">常用位置</label>
      <select id="commonPosition" v-model="form.commonPosition">
        <option v-for="[value, label] in positions" :key="value" :value="value">{{ label }}</option>
      </select>
    </div>

    <div class="verification-form__actions">
      <button type="submit" class="account-btn-primary" :disabled="loading">
        {{ submitLabel || '提交认证' }}
      </button>
      <button v-if="showCancel" type="button" class="account-btn-secondary" @click="emit('cancel')">取消</button>
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