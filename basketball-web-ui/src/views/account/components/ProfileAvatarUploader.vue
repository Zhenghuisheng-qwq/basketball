<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{
  modelValue: string
  name: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  error: [message: string]
  'file-selected': [file: File]
}>()

const previewUrl = ref(props.modelValue)
const imageError = ref(false)

watch(
  () => props.modelValue,
  (value) => {
    previewUrl.value = value
    imageError.value = false
  },
)

const showImage = computed(() => Boolean(previewUrl.value?.trim()) && !imageError.value)

function compressImage(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const img = new Image()
      img.onload = () => {
        const maxSize = 512
        let { width, height } = img
        const scale = Math.min(1, maxSize / Math.max(width, height))
        width = Math.round(width * scale)
        height = Math.round(height * scale)
        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        if (!ctx) {
          reject(new Error('??????'))
          return
        }
        ctx.drawImage(img, 0, 0, width, height)
        resolve(canvas.toDataURL('image/jpeg', 0.82))
      }
      img.onerror = () => reject(new Error('??????'))
      img.src = reader.result as string
    }
    reader.onerror = () => reject(new Error('??????'))
    reader.readAsDataURL(file)
  })
}

async function handleChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const allowed = ['image/jpeg', 'image/jpg', 'image/png']
  if (!allowed.includes(file.type)) {
    emit('error', '????JPG?JPEG?PNG ??')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    emit('error', '???????? 5MB')
    return
  }

  try {
    const dataUrl = await compressImage(file)
    previewUrl.value = dataUrl
    imageError.value = false
    emit('update:modelValue', dataUrl)
    emit('file-selected', file)
  } catch {
    emit('error', '??????????')
  }
}

function handleImageError() {
  imageError.value = true
}
</script>

<template>
  <div class="profile-avatar-uploader">
    <div class="profile-avatar-uploader__preview">
      <img
        v-if="showImage"
        :src="previewUrl"
        alt="????"
        @error="handleImageError"
      />
      <span v-else class="profile-avatar-uploader__placeholder" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="44" height="44">
          <path
            fill="currentColor"
            d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
          />
        </svg>
      </span>
    </div>
    <label class="profile-avatar-uploader__label">
      <input type="file" accept=".jpg,.jpeg,.png,image/jpeg,image/png" @change="handleChange" />
      ????
    </label>
    <p class="profile-avatar-uploader__hint">?? JPG / PNG????5MB??????</p>
  </div>
</template>

<style scoped lang="scss">
.profile-avatar-uploader {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.profile-avatar-uploader__preview {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  border: 2px solid rgba(20, 20, 20, 0.15);
  overflow: hidden;
  display: grid;
  place-items: center;
  background: #e8e8e8;
  color: rgba(80, 80, 80, 0.72);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.profile-avatar-uploader__label {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid var(--account-black);
  border-radius: 3px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;

  input {
    display: none;
  }
}

.profile-avatar-uploader__hint {
  margin: 0;
  font-size: 12px;
  color: #696969;
}
</style>
