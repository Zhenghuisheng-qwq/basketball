<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import type { GamePosition, IdentityPlatform } from '@/types/account'
import { IDENTITY_PLATFORM_LABELS, GAME_POSITION_LABELS } from '@/types/account'

export interface PlayerCertFormPayload {
  realName: string
  idType: string
  idNo: string
  platform: IdentityPlatform
  gameAccount: string
  gameNickname: string
  position: GamePosition
  contactMobile: string
  idFrontFile?: File
  idBackFile?: File
  gameScreenshotFile?: File
}

const props = defineProps<{
  initial?: Partial<{
    realName: string
    idType: string
    platform: IdentityPlatform
    gameAccount: string
    gameNickname: string
    position: GamePosition
    contactMobile: string
  }>
}>()

const emit = defineEmits<{
  submit: [data: PlayerCertFormPayload]
}>()

const realName = ref(props.initial?.realName || '')
const idType = ref(props.initial?.idType || 'ID_CARD')
const platform = ref<IdentityPlatform>(props.initial?.platform || 'XBOX')
const gameAccount = ref(props.initial?.gameAccount || '')
const gameNickname = ref(props.initial?.gameNickname || '')
const position = ref<GamePosition>(props.initial?.position || 'PG')
const contactMobile = ref(props.initial?.contactMobile || '')
const idNo = ref('')

const idFrontFile = ref<File | null>(null)
const idBackFile = ref<File | null>(null)
const gameScreenshotFile = ref<File | null>(null)

const idFrontName = computed(() => idFrontFile.value?.name || '')
const idBackName = computed(() => idBackFile.value?.name || '')
const screenshotName = computed(() => gameScreenshotFile.value?.name || '')

const errors = reactive<Record<string, string>>({})

const platforms = Object.entries(IDENTITY_PLATFORM_LABELS) as [IdentityPlatform, string][]
const positions = Object.entries(GAME_POSITION_LABELS) as [GamePosition, string][]

const isComplete = computed(() =>
  Boolean(
    realName.value.trim()
    && idType.value
    && idNo.value.trim()
    && platform.value
    && gameAccount.value.trim()
    && gameNickname.value.trim()
    && position.value
    && contactMobile.value.trim()
    && idFrontFile.value
    && gameScreenshotFile.value
    && (idType.value !== 'ID_CARD' || idBackFile.value),
  ),
)

function pickFile(key: 'idFrontFile' | 'idBackFile' | 'gameScreenshotFile', event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (key === 'idFrontFile') idFrontFile.value = file
  if (key === 'idBackFile') idBackFile.value = file
  if (key === 'gameScreenshotFile') gameScreenshotFile.value = file
  errors[key] = ''
}

function submit() {
  if (!isComplete.value) return
  emit('submit', {
    realName: realName.value.trim(),
    idType: idType.value,
    idNo: idNo.value.trim(),
    platform: platform.value,
    gameAccount: gameAccount.value.trim(),
    gameNickname: gameNickname.value.trim(),
    position: position.value,
    contactMobile: contactMobile.value.trim(),
    idFrontFile: idFrontFile.value || undefined,
    idBackFile: idBackFile.value || undefined,
    gameScreenshotFile: gameScreenshotFile.value || undefined,
  })
}
</script>

<template>
  <div class="identity-apply-form">
    <div class="identity-apply-form__field">
      <label>真实姓名</label>
      <input v-model="realName" type="text" class="identity-apply-form__input" placeholder="与证件一致" />
    </div>

    <div class="identity-apply-form__field">
      <label>证件类型</label>
      <select v-model="idType" class="identity-apply-form__input">
        <option value="ID_CARD">身份证</option>
        <option value="PASSPORT">护照</option>
      </select>
    </div>

    <div class="identity-apply-form__field">
      <label>证件号码</label>
      <input v-model="idNo" type="text" class="identity-apply-form__input" placeholder="请输入证件号码" />
    </div>

    <div class="identity-apply-form__field">
      <label>联系电话</label>
      <input v-model="contactMobile" type="tel" class="identity-apply-form__input" placeholder="用于审核联系" />
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
      <label>游戏账号</label>
      <input v-model="gameAccount" type="text" class="identity-apply-form__input" placeholder="平台游戏账号" />
    </div>

    <div class="identity-apply-form__field">
      <label>游戏昵称</label>
      <input v-model="gameNickname" type="text" class="identity-apply-form__input" placeholder="游戏内昵称" />
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
      <label>证件正面</label>
      <label class="account-btn-secondary identity-apply-form__upload-btn">
        {{ idFrontName || '上传证件正面' }}
        <input type="file" accept="image/*" hidden @change="pickFile('idFrontFile', $event)" />
      </label>
    </div>

    <div v-if="idType === 'ID_CARD'" class="identity-apply-form__field">
      <label>证件反面</label>
      <label class="account-btn-secondary identity-apply-form__upload-btn">
        {{ idBackName || '上传证件反面' }}
        <input type="file" accept="image/*" hidden @change="pickFile('idBackFile', $event)" />
      </label>
    </div>

    <div class="identity-apply-form__field">
      <label>游戏截图证明</label>
      <div class="identity-apply-form__proof">
        <p>上传能够清晰显示游戏平台与游戏 ID 的截图</p>
        <label class="account-btn-secondary identity-apply-form__upload-btn">
          {{ screenshotName || '上传游戏截图' }}
          <input type="file" accept="image/*" hidden @change="pickFile('gameScreenshotFile', $event)" />
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
</style>
