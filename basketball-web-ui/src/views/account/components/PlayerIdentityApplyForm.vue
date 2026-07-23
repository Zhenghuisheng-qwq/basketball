<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { GamePosition, IdentityPlatform } from '@/types/account'
import { IDENTITY_PLATFORM_LABELS, GAME_POSITION_LABELS } from '@/types/account'
import { mapIdentityPlatformToVerification } from '@/mock/playerIdentityMock'
import type { PlayerVerificationSnapshot } from '@/types/account'

const props = defineProps<{
  initial?: Partial<{
    avatarUrl: string
    platform: IdentityPlatform
    gameId: string
    position: GamePosition
    proofImageUrl: string
  }>
  rejectedFields?: string[]
}>()

const emit = defineEmits<{
  submit: [data: PlayerVerificationSnapshot & { avatarUrl: string }]
}>()

const avatarUrl = ref(props.initial?.avatarUrl || '')
const platform = ref<IdentityPlatform>(props.initial?.platform || 'XBOX')
const gameId = ref(props.initial?.gameId || '')
const position = ref<GamePosition>(props.initial?.position || 'PG')
const proofImageUrl = ref(props.initial?.proofImageUrl || '')
const proofUploaded = computed(() => Boolean(proofImageUrl.value))

const errors = reactive<Record<string, string>>({})

const platforms = Object.entries(IDENTITY_PLATFORM_LABELS) as [IdentityPlatform, string][]
const positions = Object.entries(GAME_POSITION_LABELS) as [GamePosition, string][]

const isComplete = computed(() =>
  Boolean(avatarUrl.value && platform.value && gameId.value.trim() && position.value && proofImageUrl.value),
)

function isRejected(field: string) {
  return props.rejectedFields?.includes(field)
}

async function handleAvatar(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    avatarUrl.value = reader.result as string
    errors.avatarUrl = ''
  }
  reader.readAsDataURL(file)
}

async function handleProof(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    proofImageUrl.value = reader.result as string
    errors.proofImageUrl = ''
  }
  reader.readAsDataURL(file)
}

function submit() {
  if (!isComplete.value) return
  const mappedPlatform = mapIdentityPlatformToVerification(platform.value)
  emit('submit', {
    avatarUrl: avatarUrl.value,
    primaryPlatform: mappedPlatform,
    gameId: gameId.value.trim(),
    gameIdScreenshotUrl: proofImageUrl.value,
    gamePosition: position.value,
    commonPosition: position.value,
    backupContact: '',
  })
}
</script>

<template>
  <div class="identity-apply-form">
    <div class="identity-apply-form__field" :class="{ 'identity-apply-form__field--error': isRejected('proofImage') }">
      <label>A. 球员头像</label>
      <div class="identity-apply-form__avatar">
        <div class="identity-apply-form__avatar-preview">
          <img v-if="avatarUrl" :src="avatarUrl" alt="球员头像预览" />
          <svg v-else viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
          </svg>
        </div>
        <label class="account-btn-secondary identity-apply-form__upload-btn">
          上传头像
          <input type="file" accept="image/*" hidden @change="handleAvatar" />
        </label>
      </div>
    </div>

    <div class="identity-apply-form__field">
      <label>B. 游戏平台</label>
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
      <label>C. 游戏 ID</label>
      <input
        v-model="gameId"
        type="text"
        class="identity-apply-form__input"
        placeholder="请输入所选平台使用的游戏 ID"
      />
    </div>

    <div class="identity-apply-form__field">
      <label>D. 常用位置</label>
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

    <div class="identity-apply-form__field" :class="{ 'identity-apply-form__field--error': isRejected('proofImage') }">
      <label>E. 必要证明材料</label>
      <div class="identity-apply-form__proof">
        <p>上传能够清晰显示游戏平台与游戏 ID 的证明材料</p>
        <label class="account-btn-secondary identity-apply-form__upload-btn">
          {{ proofUploaded ? '重新上传' : '上传证明材料' }}
          <input type="file" accept="image/*" hidden @change="handleProof" />
        </label>
        <span class="identity-apply-form__proof-status">{{ proofUploaded ? '已上传' : '未上传' }}</span>
      </div>
    </div>

    <div class="identity-apply-form__actions">
      <slot name="actions" :complete="isComplete" :submit="submit" />
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>