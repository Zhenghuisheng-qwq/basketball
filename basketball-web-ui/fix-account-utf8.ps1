# fix-account-utf8.ps1 - Restore corrupted Chinese UTF-8 in account Vue files
$ErrorActionPreference = "Stop"
$root = "d:/code/dz/basketball/scaffolding-v2/basketball-web-ui"
$utf8 = New-Object System.Text.UTF8Encoding $false
$fixed = @()

# --- components/AuthStepper.vue ---
$content_components_AuthStepper_vue = @'
<script setup lang="ts">
defineProps<{
  current: 1 | 2 | 3
}>()

const steps = [
  { id: 1, num: '01', label: '创建账号' },
  { id: 2, num: '02', label: '邮箱验证' },
  { id: 3, num: '03', label: '完成' },
] as const
</script>

<template>
  <div class="auth-steps" aria-label="注册步骤">
    <template v-for="(step, index) in steps" :key="step.id">
      <div
        class="auth-step"
        :class="{
          'auth-step--active': current === step.id,
          'auth-step--done': current > step.id,
        }"
      >
        <strong>{{ current > step.id ? '✓' : step.num }}</strong>
        <span>{{ step.label }}</span>
      </div>
      <span v-if="index < steps.length - 1" class="auth-step-line" aria-hidden="true" />
    </template>
  </div>
</template>
'@
$path = Join-Path $root "src\views\account\components\AuthStepper.vue"
[System.IO.File]::WriteAllText($path, $content_components_AuthStepper_vue, $utf8)
$fixed += "components/AuthStepper.vue"

# --- components/PlayerVerificationForm.vue ---
$content_components_PlayerVerificationForm_vue = @'
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
  backupContact: props.initial?.backupContact || '',
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
    backupContact: form.backupContact.trim(),
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

    <div class="verification-form__field">
      <label for="backupContact">备用联系方式</label>
      <input id="backupContact" v-model="form.backupContact" type="text" maxlength="64" />
      <p class="verification-form__hint">选填，不会公开显示，仅用于赛事管理联系</p>
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
'@
$path = Join-Path $root "src\views\account\components\PlayerVerificationForm.vue"
[System.IO.File]::WriteAllText($path, $content_components_PlayerVerificationForm_vue, $utf8)
$fixed += "components/PlayerVerificationForm.vue"

# --- components/PlayerIdentityApplyForm.vue ---
$content_components_PlayerIdentityApplyForm_vue = @'
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
'@
$path = Join-Path $root "src\views\account\components\PlayerIdentityApplyForm.vue"
[System.IO.File]::WriteAllText($path, $content_components_PlayerIdentityApplyForm_vue, $utf8)
$fixed += "components/PlayerIdentityApplyForm.vue"

# --- components/PlayerVerificationSummary.vue ---
$content_components_PlayerVerificationSummary_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import type { PlayerVerificationSnapshot } from '@/types/account'
import { GAME_PLATFORM_LABELS, GAME_POSITION_LABELS } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const props = defineProps<{
  nickname: string
  avatarUrl?: string
  data: PlayerVerificationSnapshot
  submittedAt?: string
  applicationNo?: string
  highlightFields?: string[]
  title?: string
}>()

const initials = computed(() => props.nickname.slice(0, 2).toUpperCase())
</script>

<template>
  <div class="verification-summary">
    <h4 v-if="title" class="verification-summary__title">{{ title }}</h4>
    <div class="verification-summary__head">
      <div class="verification-summary__avatar">
        <img v-if="avatarUrl" :src="avatarUrl" :alt="nickname" />
        <span v-else>{{ initials }}</span>
      </div>
      <strong>{{ nickname }}</strong>
    </div>
    <dl class="account-info-list">
      <div class="account-field-row">
        <dt>主要游戏平台</dt>
        <dd>{{ GAME_PLATFORM_LABELS[data.primaryPlatform] }}</dd>
      </div>
      <div class="account-field-row" :class="{ 'field-error': highlightFields?.includes('gameId') }">
        <dt>游戏 ID</dt>
        <dd>{{ data.gameId }}</dd>
      </div>
      <div class="account-field-row" :class="{ 'field-error': highlightFields?.includes('gameIdScreenshotUrl') }">
        <dt>游戏 ID 截图</dt>
        <dd>{{ data.gameIdScreenshotUrl ? '已上传' : '未上传' }}</dd>
      </div>
      <div class="account-field-row">
        <dt>2K 角色位置</dt>
        <dd>{{ GAME_POSITION_LABELS[data.gamePosition] }}</dd>
      </div>
      <div class="account-field-row">
        <dt>常用位置</dt>
        <dd>{{ GAME_POSITION_LABELS[data.commonPosition] }}</dd>
      </div>
      <div v-if="data.backupContact" class="account-field-row">
        <dt>备用联系方式</dt>
        <dd>{{ data.backupContact }}</dd>
      </div>
      <div v-if="submittedAt" class="account-field-row">
        <dt>提交时间</dt>
        <dd>{{ formatDateTime(submittedAt) }}</dd>
      </div>
      <div v-if="applicationNo" class="account-field-row">
        <dt>申请编号</dt>
        <dd>{{ applicationNo }}</dd>
      </div>
    </dl>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.verification-summary__title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 900;
}

.verification-summary__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.verification-summary__avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: #ddd;
  font-weight: 800;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.field-error dd {
  color: var(--account-danger);
}
</style>
'@
$path = Join-Path $root "src\views\account\components\PlayerVerificationSummary.vue"
[System.IO.File]::WriteAllText($path, $content_components_PlayerVerificationSummary_vue, $utf8)
$fixed += "components/PlayerVerificationSummary.vue"

# --- components/ProfileCompleteness.vue ---
$content_components_ProfileCompleteness_vue = @'
<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  percent: number
  items: { key: string; label: string; done: boolean; weight: number }[]
}>()

const displayPercent = computed(() => `${props.percent}%`)
</script>

<template>
  <div class="profile-completeness">
    <div class="profile-completeness__ring" :style="{ '--percent': percent }">
      <span>{{ displayPercent }}</span>
    </div>
    <p class="profile-completeness__title">资料完整度</p>
    <ul class="profile-completeness__list">
      <li v-for="item in items" :key="item.key" :class="{ 'profile-completeness__item--done': item.done }">
        <span class="profile-completeness__mark" aria-hidden="true">{{ item.done ? '✓' : '' }}</span>
        <span>{{ item.label }}（{{ item.weight }}%）</span>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.profile-completeness {
  text-align: center;
}

.profile-completeness__ring {
  --percent: 0;
  width: 120px;
  height: 120px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at center, var(--cnpa-paper-bg) 58%, transparent 59%),
    conic-gradient(var(--account-red, #d5080d) calc(var(--percent) * 1%), rgba(20, 20, 20, 0.08) 0);
  font-size: 24px;
  font-weight: 900;
}

.profile-completeness__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 900;
}

.profile-completeness__list {
  list-style: none;
  margin: 0;
  padding: 0;
  text-align: left;
}

.profile-completeness__list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  color: #696969;
}

.profile-completeness__item--done {
  color: var(--account-text);
  font-weight: 700;
}

.profile-completeness__mark {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid rgba(20, 20, 20, 0.25);
  display: grid;
  place-items: center;
  font-size: 10px;
  flex-shrink: 0;
}

.profile-completeness__item--done .profile-completeness__mark {
  border-color: var(--account-red);
  background: var(--account-red);
  color: #fff;
}
</style>
'@
$path = Join-Path $root "src\views\account\components\ProfileCompleteness.vue"
[System.IO.File]::WriteAllText($path, $content_components_ProfileCompleteness_vue, $utf8)
$fixed += "components/ProfileCompleteness.vue"

# --- my-tournaments/index.vue ---
$content_my_tournaments_index_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import type { OpenApplicationStatus } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()

const tournaments = computed(() => accountStore.myTournaments)
const isVerified = computed(() => accountStore.isVerifiedPlayer)
const openTournament = computed(() => accountStore.openTournament)
const openApplication = computed(() => accountStore.openApplication)
const stats = computed(() => accountStore.openRegistrationStats)

const invited = computed(() => tournaments.value.filter((item) => item.status === 'INVITED'))

const openStatus = computed(() => openApplication.value?.status || 'NOT_SUBMITTED')

const bannerStatus = computed(() => {
  if (stats.value.pending > 0) return '报名审核中'
  if (stats.value.rejected > 0) return '报名未通过'
  if (stats.value.joined > 0) return '参赛资格正常'
  if (stats.value.available > 0) return '可报名赛事'
  return '暂无赛事'
})

const openAction = computed(() => {
  const map: Record<OpenApplicationStatus, { label: string; route: string } | null> = {
    NOT_SUBMITTED: { label: '立即报名', route: 'AccountOpenApplicationApply' },
    PENDING: { label: '查看审核状态', route: 'AccountOpenApplicationPending' },
    REJECTED: { label: '修改并重新提交', route: 'AccountOpenApplicationRejected' },
    APPROVED: { label: '查看参赛状态', route: 'AccountOpenApplicationApproved' },
    CANCELLED: { label: '查看状态', route: 'AccountOpenApplicationStatusRedirect' },
    CLOSED: null,
  }
  return map[openStatus.value]
})

function goPlayerIdentity() {
  router.push('/account/player-identity')
}

function goOpen(routeName?: string) {
  if (!isVerified.value) {
    ElMessage.warning('请先完成球员身份认证')
    router.push('/account/player-identity')
    return
  }
  const block = accountStore.getOpenRegistrationBlockReason()
  if (openStatus.value === 'NOT_SUBMITTED' && block) {
    ElMessage.warning(block)
    if (block.includes('资格受限')) router.push('/account/player-identity/eligibility')
    return
  }
  router.push({ name: routeName || openAction.value?.route || 'AccountOpenApplicationApply' })
}

function goEligibility() {
  router.push('/account/player-identity/eligibility')
}

function acceptInvite(id: string) {
  if (!isVerified.value) {
    ElMessage.warning('请先完成球员身份认证')
    return
  }
  accountStore.acceptTournamentInvitation(id)
}

function rejectInvite(id: string) {
  accountStore.rejectTournamentInvitation(id)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="我的赛事"
      en-title="MY TOURNAMENTS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事' },
      ]"
    />

    <AccountStatusBanner
      title="当前赛事与报名资格"
      description="查看已报名赛事、报名审核状态与参赛关系"
      :status-text="bannerStatus"
      :status-type="stats.rejected > 0 ? 'danger' : stats.pending > 0 ? 'warning' : 'info'"
    />

    <div class="tournament-stats">
      <div class="tournament-stats__item"><strong>{{ stats.joined }}</strong><span>已参加赛事</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.pending }}</strong><span>报名审核中</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.rejected }}</strong><span>报名未通过</span></div>
      <div class="tournament-stats__item"><strong>{{ stats.available }}</strong><span>可报名赛事</span></div>
    </div>

    <AccountPanel v-if="!isVerified" title="球员身份未认证">
      <AccountEmptyState
        large
        message="暂无赛事记录"
        subtitle="完成球员认证后，可在这里查看可报名赛事与参赛状态。"
      />
      <div class="account-panel__action">
        <button type="button" class="account-btn-primary" @click="goPlayerIdentity">前往球员身份认证</button>
      </div>
    </AccountPanel>

    <template v-else>
      <AccountPanel title="当前可报名赛事">
        <div class="open-tournament-card">
          <div class="open-tournament-card__head">
            <strong>{{ openTournament.name }}</strong>
            <AccountStatusBadge
              :type="openTournament.status === 'REGISTRATION_OPEN' ? 'success' : 'warning'"
              :text="openTournament.statusText"
            />
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ openTournament.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">报名方式</span><span>{{ openTournament.registrationTypeText }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">报名截止</span><span>{{ openTournament.registrationDeadline }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">名额状态</span><span>{{ openTournament.quotaUsed }} / {{ openTournament.quotaTotal }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">报名状态</span><span>{{ openApplication?.statusText || '未提交' }}</span></div>
          </div>
          <div class="account-panel__action">
            <button
              v-if="openAction"
              type="button"
              class="account-btn-primary"
              @click="goOpen(openAction.route)"
            >
              {{ openAction.label }}
            </button>
            <button
              v-else
              type="button"
              class="account-btn-secondary"
              disabled
            >
              报名已关闭
            </button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel v-if="invited.length" title="待处理球队邀请">
        <div v-for="item in invited" :key="item.id" class="tournament-invite-card">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">赛事名称</span><span>{{ item.name }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">球队名称</span><span>{{ item.teamName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">邀请角色</span><span>{{ item.role }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">邀请时间</span><span>{{ item.inviteTime }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">有效期</span><span>{{ item.inviteExpires }}</span></div>
          </div>
          <div class="tournament-invite-card__actions">
            <button type="button" class="account-btn-primary" @click="acceptInvite(item.invitationId || '')">接受邀请</button>
            <button type="button" class="account-btn-secondary" @click="rejectInvite(item.invitationId || '')">拒绝邀请</button>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="历史赛事">
        <AccountEmptyState
          v-if="tournaments.filter((item) => item.id !== openTournament.id).length === 0 && openStatus !== 'APPROVED'"
          large
          message="暂无其他赛事记录"
          subtitle="后续收到球队邀请或报名其他赛事后将在此展示"
        />
        <div v-else class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th>赛事名称</th>
                <th>赛事类型</th>
                <th>赛事时间</th>
                <th>当前状态</th>
                <th>球队</th>
                <th>角色</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in tournaments" :key="item.id">
                <td>{{ item.name }}</td>
                <td>{{ item.type || '-' }}</td>
                <td>{{ item.period || '-' }}</td>
                <td>{{ item.statusText }}</td>
                <td>{{ item.teamName || '-' }}</td>
                <td>{{ item.role || '-' }}</td>
                <td>
                  <button
                    v-if="item.id === openTournament.id"
                    type="button"
                    class="account-btn-secondary"
                    @click="goOpen()"
                  >
                    {{ openAction?.label || '查看' }}
                  </button>
                  <button
                    v-else-if="item.eligibility === 'LIMITED'"
                    type="button"
                    class="account-btn-secondary"
                    @click="goEligibility"
                  >
                    查看资格状态
                  </button>
                  <button
                    v-else-if="item.status === 'INVITED'"
                    type="button"
                    class="account-btn-primary"
                    @click="acceptInvite(item.invitationId || '')"
                  >
                    接受邀请
                  </button>
                  <span v-else>-</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </AccountPanel>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\my-tournaments\index.vue"
[System.IO.File]::WriteAllText($path, $content_my_tournaments_index_vue, $utf8)
$fixed += "my-tournaments/index.vue"

# --- my-tournaments/open/apply.vue ---
$content_my_tournaments_open_apply_vue = @'
<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import TournamentApplicationStepper from '@/components/account/TournamentApplicationStepper.vue'
import TournamentInfoCard from '@/components/account/TournamentInfoCard.vue'
import OpenPlayerProfileCard from '@/components/account/OpenPlayerProfileCard.vue'
import RegistrationConfirmCard from '@/components/account/RegistrationConfirmCard.vue'
import { useAccountStore } from '@/stores/account'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const router = useRouter()
const accountStore = useAccountStore()

const tournament = computed(() => accountStore.openTournament)
const application = computed(() => accountStore.openApplication)
const player = computed(() => accountStore.certifiedPlayerForOpen)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const blockReason = computed(() => accountStore.getOpenRegistrationBlockReason())
const canSubmit = computed(() => accountStore.canSubmitOpenApplication())

const steps: StepItem[] = [
  { label: '提交报名', state: 'current' },
  { label: '管理员审核', state: 'upcoming' },
  { label: '正式参赛', state: 'upcoming' },
]

function redirectByStatus() {
  const status = application.value?.status
  if (status === 'PENDING') {
    router.replace({ name: 'AccountOpenApplicationPending' })
  } else if (status === 'REJECTED') {
    router.replace({ name: 'AccountOpenApplicationRejected' })
  } else if (status === 'APPROVED') {
    router.replace({ name: 'AccountOpenApplicationApproved' })
  }
}

onMounted(() => {
  if (!isVerified.value) {
    ElMessage.warning('请先完成球员身份认证')
    router.replace('/account/player-identity')
    return
  }
  if (application.value?.status === 'PENDING' || application.value?.status === 'APPROVED') {
    redirectByStatus()
  }
})

function handleSubmit() {
  if (!canSubmit.value) {
    ElMessage.warning(blockReason.value || '当前无法提交报名')
    return
  }
  accountStore.submitOpenApplication()
  ElMessage.success('Open 个人报名已提交')
  router.push({ name: 'AccountOpenApplicationPending' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="Open 个人报名"
      en-title="OPEN INDIVIDUAL REGISTRATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-tournaments' },
        { label: tournament.name },
      ]"
    />

    <AccountStatusBanner
      :title="`${tournament.name} · 个人报名`"
      description="认证球员直接提交个人赛事报名"
      :status-text="tournament.statusText"
      status-type="success"
    />

    <TournamentApplicationStepper :steps="steps" />

    <div class="open-apply-grid">
      <AccountPanel title="赛事信息">
        <TournamentInfoCard :tournament="tournament" />
      </AccountPanel>

      <AccountPanel title="个人报名资料">
        <OpenPlayerProfileCard v-if="player" :player="player" />
      </AccountPanel>

      <AccountPanel title="报名确认">
        <RegistrationConfirmCard
          :disabled="!canSubmit"
          @submit="handleSubmit"
        />
        <p v-if="blockReason && application?.status !== 'REJECTED'" class="open-apply-block-reason">{{ blockReason }}</p>
      </AccountPanel>
    </div>

    <div class="open-apply-footnotes">
      <p>不保存草稿</p>
      <p>同一赛事不能重复报名</p>
      <p>参加其他同期赛事不影响 Open 报名</p>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\my-tournaments\open\apply.vue"
[System.IO.File]::WriteAllText($path, $content_my_tournaments_open_apply_vue, $utf8)
$fixed += "my-tournaments/open/apply.vue"

# --- my-tournaments/open/status.vue ---
$content_my_tournaments_open_status_vue = @'
<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import TournamentApplicationStepper from '@/components/account/TournamentApplicationStepper.vue'
import OpenApplicationStatusCard from '@/components/account/OpenApplicationStatusCard.vue'
import OpenPlayerProfileCard from '@/components/account/OpenPlayerProfileCard.vue'
import { useAccountStore } from '@/stores/account'
import { isOpenRegistrationDeadlinePassed } from '@/mock/openRegistrationMock'
import type { StepItem } from '@/components/account/AccountStepper.vue'
import { openStatusQueryToEnum } from '@/types/account'

const props = defineProps<{
  statusMode: 'pending' | 'rejected' | 'approved'
}>()

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const tournament = computed(() => accountStore.openTournament)
const application = computed(() => accountStore.openApplication)

const playerSnapshot = computed(() => {
  if (!application.value) return null
  return {
    userNo: accountStore.currentAccount?.userNo || '',
    playerNo: application.value.playerNo,
    nickname: application.value.nickname,
    avatarUrl: accountStore.currentAccount?.avatarUrl || '',
    identityStatus: 'APPROVED' as const,
    identityStatusText: '认证球员',
    platform: application.value.platform,
    gameId: application.value.gameId,
  }
})

watch(
  () => route.query.status,
  async (queryStatus) => {
    if (!import.meta.env.DEV || typeof queryStatus !== 'string') return
    const mapped = openStatusQueryToEnum(queryStatus)
    if (!mapped) return
    accountStore.applyDevOpenStatusOverride(mapped)
  },
  { immediate: true },
)

onMounted(() => {
  if (application.value?.status === 'NOT_SUBMITTED') {
    router.replace({ name: 'AccountOpenApplicationApply' })
  }
})

const pageConfig = computed(() => {
  if (props.statusMode === 'pending') {
    return {
      zhTitle: 'Open 报名状态',
      enTitle: 'OPEN APPLICATION STATUS',
      bannerDesc: '报名资料已提交，当前由管理员审核',
      bannerStatus: '审核中',
      bannerType: 'warning' as const,
      steps: [
        { label: '已提交', state: 'done' },
        { label: '管理员审核', state: 'current' },
        { label: '正式参赛', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '提交资料',
      footnote: '正式提交后不能主动撤销，不能重复提交。',
    }
  }
  if (props.statusMode === 'rejected') {
    return {
      zhTitle: 'Open 报名状态',
      enTitle: 'OPEN APPLICATION STATUS',
      bannerDesc: '报名审核未通过，请在报名期内修改后重新提交',
      bannerStatus: '未通过',
      bannerType: 'danger' as const,
      steps: [
        { label: '已提交', state: 'done' },
        { label: '管理员审核', state: 'failed', subLabel: '未通过' },
        { label: '正式参赛', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '原报名资料',
      footnote: `重新提交使用新的提交时间；报名截止 ${tournament.value.registrationDeadline}，截止后不能重交。`,
    }
  }
  return {
    zhTitle: 'Open 报名状态',
    enTitle: 'OPEN APPLICATION STATUS',
    bannerDesc: '个人报名审核已通过，正式参赛资格已生效',
    bannerStatus: '审核通过',
    bannerType: 'success' as const,
    steps: [
      { label: '已提交', state: 'done' },
      { label: '管理员审核', state: 'done' },
      { label: '正式参赛', state: 'done' },
    ] as StepItem[],
    leftTitle: '正式个人参赛关系',
    footnote: '报名通过后已进入「已参加赛事」；个人不能主动撤销，只能由管理员取消。',
  }
})

const deadlinePassed = computed(() =>
  isOpenRegistrationDeadlinePassed(tournament.value.registrationDeadline),
)

function goMyTournaments() {
  router.push('/account/my-tournaments')
}

function goResubmit() {
  if (deadlinePassed.value) return
  router.push({ name: 'AccountOpenApplicationApply' })
}

function goEvent() {
  router.push('/tournament')
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="pageConfig.zhTitle"
      :en-title="pageConfig.enTitle"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的赛事', to: '/account/my-tournaments' },
        { label: tournament.name },
      ]"
    />

    <AccountStatusBanner
      :title="`${tournament.name} · 个人报名`"
      :description="pageConfig.bannerDesc"
      :status-text="pageConfig.bannerStatus"
      :status-type="pageConfig.bannerType"
    />

    <TournamentApplicationStepper :steps="pageConfig.steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="pageConfig.leftTitle">
        <div v-if="playerSnapshot" class="open-status-profile">
          <OpenPlayerProfileCard :player="playerSnapshot" readonly />
          <div class="account-info-list">
            <div v-if="statusMode === 'approved'" class="account-info-row">
              <span class="account-info-row__label">赛事</span>
              <span>{{ application?.tournamentName }}</span>
            </div>
            <div v-if="statusMode === 'approved'" class="account-info-row">
              <span class="account-info-row__label">参赛方式</span>
              <span>个人参赛</span>
            </div>
            <div v-if="statusMode === 'approved'" class="account-info-row">
              <span class="account-info-row__label">当前状态</span>
              <span class="account-status-text--success">已生效</span>
            </div>
            <div class="account-info-row">
              <span class="account-info-row__label">{{ statusMode === 'rejected' ? '原提交时间' : '提交时间' }}</span>
              <span>{{ application?.submitTime }}</span>
            </div>
            <div v-if="application?.applicationNo" class="account-info-row">
              <span class="account-info-row__label">报名编号</span>
              <span>{{ application.applicationNo }}</span>
            </div>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <OpenApplicationStatusCard
          v-if="statusMode === 'pending'"
          variant="pending"
          title="个人报名审核中"
          description="管理员正在核对球员认证与赛事资格；审核完成后状态将自动更新"
          badge-text="审核中"
          @action="goMyTournaments"
        />
        <OpenApplicationStatusCard
          v-else-if="statusMode === 'rejected'"
          variant="rejected"
          title="个人报名审核未通过"
          description="当前报名资格未通过，请核对报名资料后重新提交。"
          :review-remark="application?.reviewRemark"
          badge-text="未通过"
          @action="goResubmit"
        />
        <div v-if="statusMode === 'rejected' && deadlinePassed" class="open-apply-block-reason">
          报名已截止，无法重新提交
        </div>
        <OpenApplicationStatusCard
          v-else-if="statusMode === 'approved'"
          variant="approved"
          title="Open 个人报名审核通过"
          badge-text="正式参赛"
          :review-time="application?.reviewTime"
          :grouping-status-text="application?.groupingStatusText"
          description="参赛资格：已确认"
          @action="goEvent"
        />
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ pageConfig.footnote }}</p>
  </div>
</template>

<style scoped lang="scss">
@import '../../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\my-tournaments\open\status.vue"
[System.IO.File]::WriteAllText($path, $content_my_tournaments_open_status_vue, $utf8)
$fixed += "my-tournaments/open/status.vue"

# --- player-identity/index.vue ---
$content_player_identity_index_vue = @'
<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { identityQueryToVerificationStatus } from '@/types/account'
import type { PlayerVerificationStatus } from '@/types/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const isDev = import.meta.env.DEV

const identity = computed(() => accountStore.playerIdentity)

const status = computed(() => identity.value?.status || 'NOT_SUBMITTED')

watch(
  () => route.query.status,
  async (queryStatus) => {
    if (!import.meta.env.DEV || typeof queryStatus !== 'string') return
    const mapped = identityQueryToVerificationStatus(queryStatus)
    if (!mapped) return
    await accountStore.loadAccountMock(mapped, queryStatus === 'limited')
  },
  { immediate: true },
)

watch(
  [status, () => route.name],
  ([nextStatus, routeName]) => {
    if (routeName !== 'AccountPlayerIdentity') return
    if (nextStatus === 'PENDING') router.replace({ name: 'AccountPlayerIdentityPending' })
    else if (nextStatus === 'REJECTED') router.replace({ name: 'AccountPlayerIdentityRejected' })
    else if (nextStatus === 'APPROVED' || nextStatus === 'ELIGIBILITY_LIMITED') {
      if (nextStatus === 'ELIGIBILITY_LIMITED' && route.query.status === 'limited') {
        router.replace({ name: 'AccountPlayerEligibility' })
        return
      }
      router.replace({ name: 'AccountPlayerIdentityApproved' })
    }
  },
  { immediate: true },
)

const banner = computed(() => ({
  title: 'CNPA 永久球员身份',
  description: '完成一次认证，建立长期有效的球员档案',
  statusText: '未认证',
  statusType: 'warning' as const,
}))

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

async function switchDevStatus(next: PlayerVerificationStatus) {
  if (!import.meta.env.DEV) return
  await router.replace({ query: { status: next === 'NOT_APPLIED' ? 'not-submitted' : next.toLowerCase() } })
  await accountStore.loadAccountMock(next)
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员身份"
      en-title="PLAYER IDENTITY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份' },
      ]"
    />

    <AccountStatusBanner v-bind="banner" />

    <div v-if="isDev" class="verification-dev-switcher">
      <span>开发态状态预览</span>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('NOT_APPLIED')">未提交</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('PENDING')">审核中</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('REJECTED')">未通过</button>
      <button type="button" class="verification-dev-switcher__btn" @click="switchDevStatus('APPROVED')">已通过</button>
    </div>

    <div class="identity-entry-grid">
      <AccountPanel title="当前状态">
        <div class="identity-entry-state">
          <div class="identity-entry-state__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
            </svg>
          </div>
          <AccountStatusBadge type="warning" text="未认证" />
          <p class="identity-entry-state__title">尚未申请球员认证</p>
        </div>
      </AccountPanel>

      <AccountPanel title="申请 CNPA 球员认证">
        <ul class="identity-entry-rules">
          <li>审核通过后将获得不可修改的永久球员 ID</li>
          <li>认证资料用于确认游戏平台、游戏 ID 与球员赛事资格</li>
          <li>认证长期有效，无需在每届赛事中重复申请</li>
        </ul>
        <p class="identity-entry-hint">
          未认证账号不能报名、接受球队邀请、进入阵容或参与转会
        </p>
        <div class="account-panel__action">
          <button type="button" class="account-btn-primary" @click="goApply">申请球员认证</button>
        </div>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\index.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_index_vue, $utf8)
$fixed += "player-identity/index.vue"

# --- player-identity/approved.vue ---
$content_player_identity_approved_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import { mapVerificationPlatform } from '@/mock/playerIdentityMock'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const identity = computed(() => accountStore.playerIdentity)

const steps = [
  { label: '提交资料', state: 'done' as const },
  { label: '管理员审核', state: 'done' as const },
  { label: '认证生效', state: 'done' as const },
]

const platformLabel = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  if (!data?.primaryPlatform) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(data.primaryPlatform)]
})

const gameId = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  return data?.gameId || '-'
})

const position = computed(() => {
  const data = verification.value?.currentEffectiveData || verification.value
  return data?.gamePosition || data?.commonPosition || '-'
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '已通过' },
      ]"
    />

    <AccountStatusBanner
      title="永久球员身份"
      description="球员认证已通过，认证资料长期有效"
      status-text="已认证"
      status-type="success"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel title="有效认证资料">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar identity-review-profile__avatar--success">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge type="success" text="认证球员" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">已绑定平台</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">平台状态</span><span class="account-status-text--success">已生效</span></div>
          <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ position }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">认证状态</span><span class="account-status-text--success">长期有效</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="永久球员身份">
        <div class="identity-review-result identity-review-result--success">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">永久球员 ID</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo || identity?.playerNo }}</strong>
          <p>认证状态：已生效</p>
          <p>永久球员 ID 不可修改</p>
          <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/apply')">
            申请修改认证资料
          </button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">球员认证长期有效，无需按赛事重复认证，永久球员 ID 不可修改。</p>

    <div class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/platforms')">
        平台与游戏 ID 管理
      </button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">
        资料变更记录
      </button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">
        我的赛事
      </button>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\approved.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_approved_vue, $utf8)
$fixed += "player-identity/approved.vue"

# --- player-identity/eligibility.vue ---
$content_player_identity_eligibility_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const limit = computed(() => accountStore.eligibility)
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员资格状态"
      en-title="PLAYER ELIGIBILITY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '资格状态' },
      ]"
    />

    <AccountStatusBanner
      title="赛事参赛资格受限"
      description="请查看赛事方处理结果与联系说明"
      status-text="资格受限"
      status-type="danger"
    />

    <div class="identity-review-grid">
      <AccountPanel title="资格状态">
        <div class="identity-review-result identity-review-result--warning">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg>
          </div>
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">永久球员 ID</span><span>{{ account?.playerNo || limit?.playerNo }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">当前状态</span><span>{{ limit?.statusText }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">对应赛事</span><span>{{ limit?.tournamentName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">永久球员身份</span><span class="account-status-text--success">{{ limit?.permanentIdentityStatus }}</span></div>
          </div>
        </div>
      </AccountPanel>

      <AccountPanel title="赛事方处理结果">
        <strong>{{ limit?.resultTitle }}</strong>
        <AccountStatusBadge type="danger" :text="limit?.resultStatus || '已生效'" />
        <p>{{ limit?.reason }}</p>
      </AccountPanel>
    </div>

    <AccountPanel title="联系赛事方说明">
      <p>如需了解处理依据或提交情况说明，请通过对应赛事公告公布的官方联系方式联系赛事方。</p>
      <p class="identity-eligibility-notice">{{ limit?.notice }}</p>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
          返回球员身份
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\eligibility.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_eligibility_vue, $utf8)
$fixed += "player-identity/eligibility.vue"

# --- player-identity/pending.vue ---
$content_player_identity_pending_vue = @'
<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS, GAME_PLATFORM_LABELS } from '@/types/account'
import { mapVerificationPlatform } from '@/mock/playerIdentityMock'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

const steps = [
  { label: '提交资料', state: 'done' as const },
  { label: '管理员审核', state: 'current' as const },
  { label: '认证生效', state: 'upcoming' as const },
]

const platformLabel = computed(() => {
  const p = verification.value?.primaryPlatform
  if (!p) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(p)] || GAME_PLATFORM_LABELS[p]
})

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showWithdraw.value = false
  ElMessage.success('已撤回申请，可修改资料后重新提交')
  router.push({ name: 'AccountPlayerIdentityApply' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '审核中' },
      ]"
    />

    <AccountStatusBanner
      title="球员认证申请"
      description="认证资料已提交，当前由管理员审核"
      status-text="审核中"
      status-type="warning"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel title="本次认证资料">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge type="warning" text="审核中" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ platformLabel }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 1 份</span></div>
          <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <div class="identity-review-result identity-review-result--pending">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>球员认证审核中</strong>
          <AccountStatusBadge type="warning" text="审核中" />
          <p>管理员将对整份认证资料进行审核</p>
          <p>审核期间不能重复提交</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">撤回修改</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">撤回后可以修改认证资料；再次提交将生成新的审核记录。</p>

    <AccountConfirmDialog
      v-model="showWithdraw"
      title="撤回认证申请"
      message="确认撤回当前审核中的认证申请？撤回后可修改资料并重新提交。"
      confirm-text="确认撤回"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\pending.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_pending_vue, $utf8)
$fixed += "player-identity/pending.vue"

# --- player-identity/platforms.vue ---
$content_player_identity_platforms_vue = @'
<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS, type IdentityPlatform } from '@/types/account'
import { ElMessage } from 'element-plus'

const accountStore = useAccountStore()
const showAddDialog = ref(false)
const showPendingDialog = ref(false)

const account = computed(() => accountStore.currentAccount)
const records = computed(() => accountStore.platformGameIds)

const addForm = reactive({
  platform: 'PC' as IdentityPlatform,
  gameId: '',
  proofUploaded: false,
})

const pendingRecord = computed(() => records.value.find((item) => item.status === 'PENDING'))

function openAdd(platform: IdentityPlatform) {
  addForm.platform = platform
  addForm.gameId = ''
  addForm.proofUploaded = false
  showAddDialog.value = true
}

function submitAdd() {
  if (!addForm.gameId.trim()) {
    ElMessage.warning('请填写游戏 ID')
    return
  }
  if (!addForm.proofUploaded) {
    ElMessage.warning('请上传证明材料')
    return
  }
  accountStore.submitPlatformGameId({ platform: addForm.platform, gameId: addForm.gameId.trim() })
  showAddDialog.value = false
  ElMessage.success('已提交审核')
}

function mockUploadProof() {
  addForm.proofUploaded = true
}

function viewPending() {
  showPendingDialog.value = true
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="平台与游戏 ID"
      en-title="PLATFORM & GAME ID"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '平台与游戏 ID' },
      ]"
    />

    <AccountStatusBanner
      :title="`永久球员身份 ${account?.playerNo}`"
      description="在同一球员档案下管理各平台游戏 ID"
      status-text="球员认证已生效"
      status-type="success"
    />

    <AccountPanel title="平台身份记录">
      <div class="platform-table-wrap">
        <table class="platform-table">
          <thead>
            <tr>
              <th>平台</th>
              <th>游戏 ID</th>
              <th>审核状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in records" :key="row.platform">
              <td>{{ IDENTITY_PLATFORM_LABELS[row.platform] }}</td>
              <td>{{ row.gameId || '未绑定' }}</td>
              <td>{{ row.status === 'UNBOUND' ? '-' : row.statusText }}</td>
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
                  查看申请
                </button>
                <button
                  v-else-if="row.status === 'UNBOUND'"
                  type="button"
                  class="account-btn-primary"
                  @click="openAdd(row.platform)"
                >
                  新增游戏 ID
                </button>
                <button
                  v-else
                  type="button"
                  class="account-btn-secondary"
                  @click="openAdd(row.platform)"
                >
                  申请修改
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </AccountPanel>

    <AccountPanel title="身份管理规则">
      <ul class="identity-entry-rules">
        <li>同一平台游戏 ID 不可重复绑定。</li>
        <li>新增或修改平台身份必须经过管理员审核。</li>
        <li>正在用于锁定赛事的平台与游戏 ID 不能修改。</li>
        <li>审核通过前继续使用当前已生效 ID。</li>
        <li>审核中的修改不会出现在资料变更记录中。</li>
      </ul>
    </AccountPanel>

    <div v-if="showAddDialog" class="account-dialog-backdrop" @click.self="showAddDialog = false">
      <div class="account-dialog">
        <h3>{{ addForm.platform === records.find(r => r.status === 'UNBOUND')?.platform ? '新增' : '修改' }}游戏 ID</h3>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ IDENTITY_PLATFORM_LABELS[addForm.platform] }}</span></div>
        </div>
        <input v-model="addForm.gameId" class="identity-apply-form__input" placeholder="请输入游戏 ID" />
        <button type="button" class="account-btn-secondary" @click="mockUploadProof">
          {{ addForm.proofUploaded ? '证明材料已上传' : '上传证明材料' }}
        </button>
        <div class="account-dialog__actions">
          <button type="button" class="account-btn-secondary" @click="showAddDialog = false">取消</button>
          <button type="button" class="account-btn-primary" @click="submitAdd">提交审核</button>
        </div>
      </div>
    </div>

    <div v-if="showPendingDialog && pendingRecord" class="account-dialog-backdrop" @click.self="showPendingDialog = false">
      <div class="account-dialog">
        <h3>审核中详情</h3>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ IDENTITY_PLATFORM_LABELS[pendingRecord.platform] }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ pendingRecord.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">状态</span><span>{{ pendingRecord.statusText }}</span></div>
        </div>
        <button type="button" class="account-btn-secondary" @click="showPendingDialog = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\platforms.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_platforms_vue, $utf8)
$fixed += "player-identity/platforms.vue"

# --- player-identity/rejected.vue ---
$content_player_identity_rejected_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useAccountStore } from '@/stores/account'
import { mockRejectedApplication } from '@/mock/playerIdentityMock'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

const rejected = computed(() => ({
  ...mockRejectedApplication,
  playerName: account.value?.nickname || mockRejectedApplication.playerName,
  reviewRemark: verification.value?.reviewerComment || mockRejectedApplication.reviewRemark,
  submitTime: verification.value?.submittedAt ? formatDateTime(verification.value.submittedAt) : mockRejectedApplication.submitTime,
}))

const steps = [
  { label: '提交资料', state: 'done' as const },
  { label: '管理员审核', state: 'failed' as const, subLabel: '未通过' },
  { label: '认证生效', state: 'upcoming' as const },
]

function goResubmit() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '未通过' },
      ]"
    />

    <AccountStatusBanner
      title="球员认证未通过"
      description="请查看管理员备注，修改资料后重新提交"
      status-text="未通过"
      status-type="danger"
    />

    <AccountStepper :steps="steps" />

    <div class="identity-review-grid">
      <AccountPanel title="原提交资料">
        <div class="identity-review-profile">
          <div class="identity-review-profile__avatar identity-review-profile__avatar--error">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ rejected.playerName }}</strong>
            <AccountStatusBadge type="danger" text="未通过" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ IDENTITY_PLATFORM_LABELS[rejected.platform] }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ rejected.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ rejected.position }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 {{ rejected.proofCount }} 份</span></div>
          <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ rejected.submitTime }}</span></div>
        </div>
      </AccountPanel>

      <AccountPanel title="审核结果">
        <div class="identity-review-result identity-review-result--failed">
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>球员认证未通过</strong>
          <AccountStatusBadge type="danger" text="未通过" />
          <p class="identity-review-result__remark-label">管理员备注</p>
          <p class="identity-review-result__remark">{{ rejected.reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goResubmit">修改并重新提交</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">认证未通过时不会生成永久球员 ID；重新提交将建立新的审核记录。</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\rejected.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_rejected_vue, $utf8)
$fixed += "player-identity/rejected.vue"

# --- player-verification/index.vue ---
$content_player_verification_index_vue = @'
<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import PlayerVerificationForm from '../components/PlayerVerificationForm.vue'
import PlayerVerificationSummary from '../components/PlayerVerificationSummary.vue'
import PlayerVerificationStatusCard from '../components/PlayerVerificationStatusCard.vue'
import { useAccountStore } from '@/stores/account'
import type { PlayerVerificationSnapshot } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const accountStore = useAccountStore()

const showSubmitConfirm = ref(false)
const showWithdrawConfirm = ref(false)
const showChangeForm = ref(false)
const pendingSubmit = ref<PlayerVerificationSnapshot | null>(null)

const profile = computed(() => accountStore.profile)
const verification = computed(() => accountStore.verification)

const status = computed(() => verification.value?.status || 'NOT_APPLIED')

const effectiveData = computed(() => {
  if (verification.value?.currentEffectiveData) return verification.value.currentEffectiveData
  if (status.value === 'APPROVED' || status.value === 'CHANGE_PENDING') {
    return verification.value
  }
  return null
})

const pendingData = computed(() => verification.value?.pendingChangeData)

const bannerConfig = computed(() => {
  switch (status.value) {
    case 'NOT_APPLIED':
      return {
        title: '尚未申请球员认证',
        description: '提交游戏资料后由赛事管理员审核',
        statusText: '未认证',
        statusType: 'warning' as const,
      }
    case 'PENDING':
      return {
        title: '认证申请审核中',
        description: '赛事管理员正在审核你提交的认证资料',
        statusText: '审核中',
        statusType: 'warning' as const,
      }
    case 'APPROVED':
      return {
        title: '认证资料已生效',
        description: '当前资料已通过赛事管理员审核',
        statusText: '认证球员',
        statusType: 'success' as const,
      }
    case 'CHANGE_PENDING':
      return {
        title: '认证资料变更审核中',
        description: '当前已通过资料继续生效',
        statusText: '认证球员',
        statusType: 'success' as const,
      }
    case 'REJECTED':
      return {
        title: '认证未通过',
        description: '请根据审核意见修改资料后重新提交',
        statusText: '需修改',
        statusType: 'danger' as const,
      }
    default:
      return {
        title: '球员认证',
        description: '',
        statusText: '',
        statusType: 'default' as const,
      }
  }
})

function requestSubmit(data: PlayerVerificationSnapshot) {
  pendingSubmit.value = data
  showSubmitConfirm.value = true
}

function confirmSubmit() {
  if (!pendingSubmit.value) return
  if (status.value === 'REJECTED') {
    accountStore.resubmitPlayerVerification(pendingSubmit.value)
  } else if (showChangeForm.value) {
    accountStore.submitVerificationChange(pendingSubmit.value)
    showChangeForm.value = false
  } else {
    accountStore.submitPlayerVerification(pendingSubmit.value)
  }
  pendingSubmit.value = null
  ElMessage.success('认证申请已提交')
}

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showChangeForm.value = false
  ElMessage.success('已撤回申请')
}
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      zh-title="球员认证"
      en-title="PLAYER VERIFICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员认证' },
      ]"
    />

    <AccountStatusBanner v-bind="bannerConfig" />

    <!-- NOT_APPLIED -->
    <div v-if="status === 'NOT_APPLIED'" class="verification-layout">
      <AccountPanel title="认证说明">
        <p class="verification-intro">
          提交你的游戏平台、游戏 ID 及角色位置，审核通过后可加入球队并参与赛事。
        </p>
      </AccountPanel>
      <AccountPanel title="申请表">
        <PlayerVerificationForm @submit="requestSubmit" />
      </AccountPanel>
    </div>

    <!-- PENDING -->
    <div v-else-if="status === 'PENDING'" class="verification-layout verification-layout--split">
      <AccountPanel title="本次提交资料">
        <PlayerVerificationSummary
          v-if="verification"
          :nickname="profile?.nickname || ''"
          :avatar-url="profile?.avatarUrl"
          :data="verification"
          :submitted-at="verification.submittedAt"
          :application-no="verification.applicationNo"
        />
      </AccountPanel>
      <PlayerVerificationStatusCard
        title="当前审核状态"
        status-text="审核中"
        status-type="warning"
        description="等待赛事管理员审核。审核结果将通过消息通知发送。"
      >
        <div class="verification-rules">
          <p>审核期间</p>
          <ol>
            <li>首次认证审核期间暂不生效。</li>
            <li>如需修改，可以撤回本次申请后重新提交。</li>
          </ol>
        </div>
        <button type="button" class="account-btn-secondary" @click="showWithdrawConfirm = true">
          撤回并修改
        </button>
      </PlayerVerificationStatusCard>
    </div>

    <!-- APPROVED -->
    <div v-else-if="status === 'APPROVED' && !showChangeForm" class="verification-layout verification-layout--split">
      <AccountPanel title="当前生效资料">
        <PlayerVerificationSummary
          v-if="effectiveData"
          :nickname="profile?.nickname || ''"
          :avatar-url="profile?.avatarUrl"
          :data="effectiveData"
        />
        <button type="button" class="account-btn-primary verification-action" @click="showChangeForm = true">
          申请修改球员认证
        </button>
      </AccountPanel>
      <div class="verification-side">
        <PlayerVerificationStatusCard
          title="当前状态"
          status-text="审核已通过"
          status-type="success"
          :description="`生效时间：${formatDateTime(verification?.effectiveAt)}`"
        />
        <AccountPanel title="修改规则">
          <ol class="verification-rules">
            <li>认证资料允许申请修改。</li>
            <li>每次修改均需赛事管理员重新审核。</li>
            <li>审核期间当前已通过资料继续生效。</li>
            <li>新申请未通过时，当前资料保持不变。</li>
          </ol>
        </AccountPanel>
      </div>
    </div>

    <!-- CHANGE_PENDING -->
    <div v-else-if="status === 'CHANGE_PENDING'" class="verification-layout">
      <div class="verification-layout verification-layout--split">
        <AccountPanel title="当前生效版本">
          <PlayerVerificationSummary
            v-if="effectiveData"
            :nickname="profile?.nickname || ''"
            :data="effectiveData"
            title="当前生效资料"
          />
        </AccountPanel>
        <AccountPanel title="待审核修改资料">
          <PlayerVerificationSummary
            v-if="pendingData"
            :nickname="profile?.nickname || ''"
            :data="pendingData"
            :submitted-at="verification?.submittedAt"
            :application-no="verification?.applicationNo"
            title="新提交版本"
          />
        </AccountPanel>
      </div>
      <button type="button" class="account-btn-secondary" @click="showWithdrawConfirm = true">
        撤回本次修改
      </button>
    </div>

    <!-- REJECTED -->
    <div v-else-if="status === 'REJECTED'" class="verification-layout verification-layout--split">
      <div class="verification-reject">
        <AccountPanel title="审核结果">
          <p class="verification-reject__title">本次认证未通过</p>
          <p>审核时间：{{ formatDateTime(verification?.reviewedAt) }}</p>
          <p class="verification-reject__hint">账号不会被锁定，可以修改后重新提交</p>
        </AccountPanel>
        <AccountPanel title="管理员审核意见">
          <p>{{ verification?.reviewerComment }}</p>
        </AccountPanel>
        <AccountPanel title="本次提交资料">
          <PlayerVerificationSummary
            v-if="verification"
            :nickname="profile?.nickname || ''"
            :data="verification"
            :highlight-fields="verification.rejectedFields"
          />
        </AccountPanel>
      </div>
      <div class="verification-side">
        <AccountPanel title="下一步操作">
          <ol class="verification-rules">
            <li>查看管理员审核意见</li>
            <li>修改标记的问题资料</li>
            <li>重新提交认证审核</li>
          </ol>
          <p class="verification-reject__hint">重新提交会生成新的审核记录。原申请与审核结果由系统保留。</p>
        </AccountPanel>
        <AccountPanel title="修改并重新提交">
          <PlayerVerificationForm
            :initial="verification || undefined"
            :rejected-fields="verification?.rejectedFields"
            submit-label="修改并重新提交"
            @submit="requestSubmit"
          />
        </AccountPanel>
      </div>
    </div>

    <!-- Change form from APPROVED -->
    <div v-if="showChangeForm && status === 'APPROVED'" class="verification-layout">
      <AccountPanel title="申请修改球员认证">
        <PlayerVerificationForm
          :initial="effectiveData || undefined"
          submit-label="提交修改申请"
          @submit="requestSubmit"
          @cancel="showChangeForm = false"
        />
      </AccountPanel>
    </div>

    <AccountConfirmDialog
      v-model="showSubmitConfirm"
      title="确认提交"
      message="提交后将进入审核，审核期间不可直接修改。确认提交球员认证？"
      confirm-text="确认提交"
      @confirm="confirmSubmit"
    />

    <AccountConfirmDialog
      v-model="showWithdrawConfirm"
      title="确认撤回"
      message="撤回后本次审核终止，重新提交后重新排队。确认撤回？"
      confirm-text="确认撤回"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-page {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
}

.verification-layout {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-layout--split {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.verification-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-intro {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: #696969;
}

.verification-rules {
  margin: 12px 0;
  padding-left: 18px;
  font-size: 13px;
  color: #696969;
  line-height: 1.7;
}

.verification-action {
  margin-top: 16px;
}

.verification-reject {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.verification-reject__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 900;
  color: var(--account-danger);
}

.verification-reject__hint {
  font-size: 13px;
  color: #696969;
}

@media (max-width: 991px) {
  .verification-layout--split {
    grid-template-columns: 1fr;
  }
}
</style>
'@
$path = Join-Path $root "src\views\account\player-verification\index.vue"
[System.IO.File]::WriteAllText($path, $content_player_verification_index_vue, $utf8)
$fixed += "player-verification/index.vue"

# --- profile/index.vue ---
$content_profile_index_vue = @'
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
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
  avatarError.value = ''
  isEditing.value = false
  router.replace({ query: {} })
}

function validateNickname(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return '请输入个人昵称'
  if (trimmed.length < 2 || trimmed.length > 20) return '昵称长度应为 2 至 20 个字符'
  if (!/^[\u4e00-\u9fa5a-zA-Z0-9_]+$/.test(trimmed)) {
    return '昵称仅支持中文、英文、数字和下划线'
  }
  return ''
}

const nicknameError = ref('')

function saveProfile() {
  nicknameError.value = validateNickname(editForm.nickname)
  if (nicknameError.value) return

  accountStore.updateProfile({
    nickname: editForm.nickname.trim(),
    avatarUrl: editForm.avatarUrl,
  })
  userStore.setUserInfo({
    id: profile.value?.userId,
    nickname: editForm.nickname.trim(),
    avatar: editForm.avatarUrl || undefined,
  })
  snapshot.nickname = editForm.nickname.trim()
  snapshot.avatarUrl = editForm.avatarUrl
  isEditing.value = false
  router.replace({ query: {} })
  ElMessage.success('资料已保存')
}
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      zh-title="个人资料"
      en-title="PERSONAL PROFILE"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '个人资料' },
      ]"
    />

    <AccountStatusBanner
      :title="isEditing ? '完善基础资料' : '基础资料已完善'"
      description="这些资料用于账号识别与个人中心展示"
      :status-text="profile?.emailVerified ? '资料已完成' : '邮箱已验证'"
      status-type="success"
    />

    <div class="profile-layout">
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
                <dd>{{ profile?.maskedEmail }} · 已验证</dd>
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
                <span aria-hidden="true">🔒</span>
              </div>
            </div>

            <div class="profile-form__field">
              <label>注册邮箱</label>
              <div class="profile-form__readonly">
                <span>{{ profile?.maskedEmail }} · 已验证</span>
              </div>
              <p class="profile-form__hint">修改邮箱需前往账号安全</p>
            </div>

            <div class="profile-form__actions">
              <button type="submit" class="account-btn-primary">保存修改</button>
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

.account-page {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
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
'@
$path = Join-Path $root "src\views\account\profile\index.vue"
[System.IO.File]::WriteAllText($path, $content_profile_index_vue, $utf8)
$fixed += "profile/index.vue"

# --- security/index.vue ---
$content_security_index_vue = @'
<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAccountStore } from '@/stores/account'
import { validatePasswordRules } from '@/mock/account/security'
import { formatDateTime } from '@/utils/formatDate'

const accountStore = useAccountStore()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const errors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const loading = ref(false)

const profile = computed(() => accountStore.profile)
const security = computed(() => accountStore.security)
const account = computed(() => accountStore.currentAccount)

function resetErrors() {
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
}

function validateForm(): boolean {
  resetErrors()
  if (!form.currentPassword) errors.currentPassword = '请输入当前密码'
  const ruleError = validatePasswordRules(form.newPassword)
  if (ruleError) errors.newPassword = ruleError
  if (!form.confirmPassword) errors.confirmPassword = '请确认新密码'
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = '两次输入的密码不一致'
  return !errors.currentPassword && !errors.newPassword && !errors.confirmPassword
}

function cancelForm() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  resetErrors()
}

async function savePassword() {
  if (loading.value || !validateForm()) return
  loading.value = true
  try {
    const error = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (error) {
      if (error.includes('当前密码')) errors.currentPassword = error
      else errors.newPassword = error
      return
    }
    ElMessage.success('密码修改成功')
    cancelForm()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      zh-title="账号安全"
      en-title="ACCOUNT SECURITY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '账号安全' },
      ]"
    />

    <AccountStatusBanner
      title="保护你的 CNPA 账号"
      description="管理登录邮箱与账号密码"
      status-text="安全状态正常"
      status-type="success"
    />

    <div class="security-layout">
      <AccountPanel title="修改密码">
        <form class="security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="current-password">当前密码</label>
            <PasswordInput
              id="current-password"
              v-model="form.currentPassword"
              autocomplete="current-password"
              :invalid="Boolean(errors.currentPassword)"
            />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>

          <div class="security-form__field">
            <label for="new-password">新密码</label>
            <PasswordInput
              id="new-password"
              v-model="form.newPassword"
              autocomplete="new-password"
              :invalid="Boolean(errors.newPassword)"
            />
            <p class="security-form__hint">8 至 20 位，至少包含字母和数字</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>

          <div class="security-form__field">
            <label for="confirm-password">确认新密码</label>
            <PasswordInput
              id="confirm-password"
              v-model="form.confirmPassword"
              autocomplete="new-password"
              :invalid="Boolean(errors.confirmPassword)"
            />
            <p v-if="errors.confirmPassword" class="security-form__error">{{ errors.confirmPassword }}</p>
          </div>

          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="loading">保存新密码</button>
            <button type="button" class="account-btn-secondary" @click="cancelForm">取消</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel title="账号信息">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>注册邮箱</dt><dd>{{ profile?.maskedEmail }}</dd></div>
          <div class="account-field-row">
            <dt>邮箱状态</dt>
            <dd class="account-status-text--success">已验证</dd>
          </div>
          <div class="account-field-row">
            <dt>账号状态</dt>
            <dd class="account-status-text--success">正常</dd>
          </div>
          <div class="account-field-row">
            <dt>最近登录</dt>
            <dd>{{ formatDateTime(account?.lastLoginAt) }}</dd>
          </div>
          <div class="account-field-row">
            <dt>密码更新时间</dt>
            <dd>{{ formatDateTime(security?.passwordUpdatedAt) }}</dd>
          </div>
        </dl>
        <p class="security-form__hint">登录邮箱用于账号验证和找回密码</p>
      </AccountPanel>
    </div>

    <p class="security-tip">请勿向任何人透露密码或邮箱验证码</p>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-page {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
}

.security-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.security-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-form__field label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 800;
}

.security-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #696969;
}

.security-form__error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--account-danger);
}

.security-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.security-tip {
  margin-top: 20px;
  font-size: 13px;
  color: #696969;
}

@media (max-width: 991px) {
  .security-layout {
    grid-template-columns: 1fr;
  }
}
</style>
'@
$path = Join-Path $root "src\views\account\security\index.vue"
[System.IO.File]::WriteAllText($path, $content_security_index_vue, $utf8)
$fixed += "security/index.vue"

# --- settings/index.vue ---
$content_settings_index_vue = @'
<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PasswordInput from '@/views/account/components/PasswordInput.vue'
import { useAccountStore } from '@/stores/account'
import { validatePasswordRules } from '@/mock/account/security'
import { formatDateTime } from '@/utils/formatDate'

const accountStore = useAccountStore()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const errors = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const loading = ref(false)

const profile = computed(() => accountStore.profile)
const security = computed(() => accountStore.security)
const account = computed(() => accountStore.currentAccount)

function resetErrors() {
  errors.currentPassword = ''
  errors.newPassword = ''
  errors.confirmPassword = ''
}

function validateForm(): boolean {
  resetErrors()
  if (!form.currentPassword) errors.currentPassword = '请输入当前密码'
  const ruleError = validatePasswordRules(form.newPassword)
  if (ruleError) errors.newPassword = ruleError
  if (!form.confirmPassword) errors.confirmPassword = '请确认新密码'
  else if (form.confirmPassword !== form.newPassword) errors.confirmPassword = '两次输入的密码不一致'
  return !errors.currentPassword && !errors.newPassword && !errors.confirmPassword
}

function cancelForm() {
  form.currentPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  resetErrors()
}

async function savePassword() {
  if (loading.value || !validateForm()) return
  loading.value = true
  try {
    const error = await accountStore.changePassword(form.currentPassword, form.newPassword)
    if (error) {
      if (error.includes('当前密码')) errors.currentPassword = error
      else errors.newPassword = error
      return
    }
    ElMessage.success('密码修改成功')
    cancelForm()
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="账号设置"
      en-title="ACCOUNT SETTINGS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '账号设置' },
      ]"
    />

    <AccountStatusBanner
      title="保护你的 CNPA 账号"
      description="管理登录邮箱与账号密码"
      status-text="安全状态正常"
      status-type="success"
    />

    <div class="security-grid">
      <AccountPanel title="修改密码">
        <form class="account-password-form security-form" @submit.prevent="savePassword">
          <div class="security-form__field">
            <label for="settings-current-password">当前密码</label>
            <PasswordInput
              id="settings-current-password"
              v-model="form.currentPassword"
              autocomplete="current-password"
              :invalid="Boolean(errors.currentPassword)"
            />
            <p v-if="errors.currentPassword" class="security-form__error">{{ errors.currentPassword }}</p>
          </div>

          <div class="security-form__field">
            <label for="settings-new-password">新密码</label>
            <PasswordInput
              id="settings-new-password"
              v-model="form.newPassword"
              autocomplete="new-password"
              :invalid="Boolean(errors.newPassword)"
            />
            <p class="security-form__hint">8 至 20 位，至少包含字母和数字</p>
            <p v-if="errors.newPassword" class="security-form__error">{{ errors.newPassword }}</p>
          </div>

          <div class="security-form__field">
            <label for="settings-confirm-password">确认新密码</label>
            <PasswordInput
              id="settings-confirm-password"
              v-model="form.confirmPassword"
              autocomplete="new-password"
              :invalid="Boolean(errors.confirmPassword)"
            />
            <p v-if="errors.confirmPassword" class="security-form__error">{{ errors.confirmPassword }}</p>
          </div>

          <div class="security-form__actions">
            <button type="submit" class="account-btn-primary" :disabled="loading">保存新密码</button>
            <button type="button" class="account-btn-secondary" @click="cancelForm">取消</button>
          </div>
        </form>
      </AccountPanel>

      <AccountPanel title="账号信息">
        <dl class="account-info-list">
          <div class="account-field-row"><dt>注册邮箱</dt><dd>{{ profile?.maskedEmail }}</dd></div>
          <div class="account-field-row">
            <dt>邮箱状态</dt>
            <dd class="account-status-text--success">已验证</dd>
          </div>
          <div class="account-field-row">
            <dt>账号状态</dt>
            <dd class="account-status-text--success">正常</dd>
          </div>
          <div class="account-field-row">
            <dt>最近登录</dt>
            <dd>{{ formatDateTime(account?.lastLoginAt) }}</dd>
          </div>
          <div class="account-field-row">
            <dt>密码更新时间</dt>
            <dd>{{ formatDateTime(security?.passwordUpdatedAt) }}</dd>
          </div>
        </dl>
      </AccountPanel>
    </div>

    <AccountPanel title="安全提示">
      <p class="security-tip">请勿向任何人透露密码或邮箱验证码</p>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.security-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-form__field label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 800;
}

.security-form__hint {
  margin: 6px 0 0;
  font-size: 12px;
  color: #696969;
}

.security-form__error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--account-danger);
}

.security-form__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.security-tip {
  margin: 0;
  font-size: 13px;
  color: #696969;
}
</style>
'@
$path = Join-Path $root "src\views\account\settings\index.vue"
[System.IO.File]::WriteAllText($path, $content_settings_index_vue, $utf8)
$fixed += "settings/index.vue"

# --- team-relations/index.vue ---
$content_team_relations_index_vue = @'
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import TeamInvitationDialog from '../components/TeamInvitationDialog.vue'
import { useAccountStore } from '@/stores/account'
import type { TeamInvitation, TournamentLineupRelation } from '@/types/account'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const showCreateDialog = ref(false)
const showInvitationDialog = ref(false)
const activeInvitation = ref<TeamInvitation | null>(null)

const createForm = reactive({
  teamName: '',
  platform: 'PC',
  description: '',
})

const teamRelations = computed(() => accountStore.teamRelations)

const activeLineups = computed(() =>
  teamRelations.value?.lineupRelations.filter(
    (item) => item.status === 'ACTIVE' || item.status === 'WAITING_TEAM_REGISTRATION',
  ) ?? [],
)

const pendingInvitations = computed(() =>
  teamRelations.value?.invitations.filter((item) => item.status === 'PENDING') ?? [],
)

const lineupStatusLabel: Record<TournamentLineupRelation['status'], string> = {
  ACTIVE: '生效中',
  WAITING_TEAM_REGISTRATION: '待球队报名',
  ENDED: '已结束',
}

function openInvitation(invitation: TeamInvitation) {
  activeInvitation.value = invitation
  showInvitationDialog.value = true
}

function viewTeam(item: TournamentLineupRelation) {
  router.push(`/tournament/team/${item.teamId}/overview`)
}

function submitCreateRequest() {
  if (!createForm.teamName.trim()) {
    ElMessage.warning('请输入拟创建球队名称')
    return
  }
  accountStore.submitTeamCreateRequest({
    teamName: createForm.teamName.trim(),
    platform: createForm.platform,
    description: createForm.description.trim(),
  })
  showCreateDialog.value = false
  createForm.teamName = ''
  createForm.description = ''
  ElMessage.success('创建球队申请已提交，等待管理员审核')
}

watch(
  () => route.query.invitationId,
  (id) => {
    if (!id || typeof id !== 'string') return
    const invitation = pendingInvitations.value.find((item) => item.id === id)
    if (invitation) openInvitation(invitation)
  },
  { immediate: true },
)

function handleAccept(id: string) {
  accountStore.acceptTeamInvitation(id)
  showInvitationDialog.value = false
  ElMessage.success('已接受球队邀请')
}

function handleReject(id: string) {
  accountStore.rejectTeamInvitation(id)
  showInvitationDialog.value = false
  ElMessage.success('已拒绝球队邀请')
}
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      zh-title="球队关系"
      en-title="TEAM RELATIONSHIP"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球队关系' },
      ]"
    />

    <AccountStatusBanner
      title="管理当前球队事务"
      description="查看赛事阵容、处理球队邀请与管理球队创建状态"
      status-text="参赛资格正常"
      status-type="success"
    />

    <AccountPanel title="当前赛事阵容">
      <div v-if="activeLineups.length" class="team-table-wrap">
        <table class="team-table">
          <thead>
            <tr>
              <th>赛事</th>
              <th>球队</th>
              <th>身份</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in activeLineups" :key="item.id">
              <td>{{ item.tournamentName }}</td>
              <td>{{ item.teamName }}</td>
              <td>{{ item.roleName }}</td>
              <td>{{ lineupStatusLabel[item.status] }}</td>
              <td>
                <button type="button" class="account-btn-secondary team-table__btn" @click="viewTeam(item)">
                  查看
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AccountEmptyState v-else message="当前没有有效赛事阵容" />
    </AccountPanel>

    <AccountPanel title="待处理事项">
      <div v-if="pendingInvitations.length" class="invitation-list">
        <article v-for="item in pendingInvitations" :key="item.id" class="invitation-card">
          <div>
            <p><strong>赛事</strong> {{ item.tournamentName }}</p>
            <p><strong>球队</strong> {{ item.teamName }}</p>
            <p><strong>邀请说明</strong> 邀请你担任 {{ item.inviteRole }}</p>
            <AccountStatusBadge text="待处理" type="warning" />
          </div>
          <button type="button" class="account-btn-primary" @click="openInvitation(item)">处理邀请</button>
        </article>
      </div>
      <AccountEmptyState v-else message="暂无待处理球队邀请" />
    </AccountPanel>

    <AccountPanel title="球队管理状态">
      <dl class="account-info-list">
        <div class="account-field-row">
          <dt>当前管理球队</dt>
          <dd>{{ teamRelations?.managedTeamName || '无' }}</dd>
        </div>
        <div class="account-field-row">
          <dt>队长权限</dt>
          <dd>{{ teamRelations?.hasCaptainPermission ? '已获得' : '未获得' }}</dd>
        </div>
        <div class="account-field-row">
          <dt>创建申请</dt>
          <dd>
            {{
              teamRelations?.createRequestStatus === 'PENDING'
                ? '审核中'
                : teamRelations?.createRequestStatus === 'APPROVED'
                  ? '已通过'
                  : teamRelations?.createRequestStatus === 'REJECTED'
                    ? '未通过'
                    : '无'
            }}
          </dd>
        </div>
        <div class="account-field-row">
          <dt>创建功能</dt>
          <dd>{{ teamRelations?.createFeatureEnabled ? '已开放' : '未开放' }}</dd>
        </div>
      </dl>
      <div class="team-manage-actions">
        <button type="button" class="account-btn-primary" @click="showCreateDialog = true">创建球队</button>
        <button type="button" class="account-btn-secondary">了解球队流程</button>
      </div>
    </AccountPanel>

    <p class="team-tip">
      禁赛状态与赛事阵容关系分别管理。发生禁赛时，阵容关系不会自动删除。当前阵容关系不影响球队创建申请资格。
    </p>

    <Teleport to="body">
      <div v-if="showCreateDialog" class="team-dialog">
        <button type="button" class="team-dialog__backdrop" aria-label="关闭" @click="showCreateDialog = false" />
        <div class="team-dialog__panel" role="dialog" aria-modal="true" aria-label="创建球队申请">
          <h3>创建球队申请</h3>
          <p class="team-dialog__hint">提交后将等待管理员审核，不会直接授予队长权限。</p>
          <label>
            拟创建球队名称
            <input v-model="createForm.teamName" type="text" maxlength="32" />
          </label>
          <label>
            赛事平台
            <select v-model="createForm.platform">
              <option value="PC">PC</option>
              <option value="PS5">PlayStation 5</option>
              <option value="XBOX">Xbox</option>
              <option value="MOBILE">移动端</option>
            </select>
          </label>
          <label>
            说明
            <textarea v-model="createForm.description" rows="3" maxlength="200" />
          </label>
          <div class="team-dialog__actions">
            <button type="button" class="account-btn-secondary" @click="showCreateDialog = false">取消</button>
            <button type="button" class="account-btn-primary" @click="submitCreateRequest">提交申请</button>
          </div>
        </div>
      </div>
    </Teleport>

    <TeamInvitationDialog
      v-model="showInvitationDialog"
      :invitation="activeInvitation"
      @accept="handleAccept"
      @reject="handleReject"
    />
  </div>
</template>

<script lang="ts">
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
export default {
  components: { AccountStatusBadge },
}
</script>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-page {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.team-table-wrap {
  overflow-x: auto;
}

.team-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th,
  td {
    padding: 12px 10px;
    border-bottom: 1px solid rgba(20, 20, 20, 0.1);
    text-align: left;
  }

  th {
    font-size: 12px;
    color: #696969;
  }
}

.team-table__btn {
  min-height: 32px;
  padding-inline: 12px;
}

.invitation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.invitation-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;

  p {
    margin: 0 0 6px;
    font-size: 13px;
  }
}

.team-manage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.team-tip {
  margin: 0;
  font-size: 13px;
  color: #696969;
  line-height: 1.6;
}

.team-dialog {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: grid;
  place-items: center;
  padding: 20px;
}

.team-dialog__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.team-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(480px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 8px;
  }

  label {
    display: block;
    margin-top: 14px;
    font-size: 13px;
    font-weight: 700;
  }

  input,
  select,
  textarea {
    width: 100%;
    margin-top: 6px;
    padding: 10px 12px;
    border: 1px solid var(--account-border);
    border-radius: 3px;
    font-family: inherit;
    box-sizing: border-box;
  }
}

.team-dialog__hint {
  margin: 0;
  font-size: 13px;
  color: #696969;
}

.team-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
</style>
'@
$path = Join-Path $root "src\views\account\team-relations\index.vue"
[System.IO.File]::WriteAllText($path, $content_team_relations_index_vue, $utf8)
$fixed += "team-relations/index.vue"

# --- notifications/index.vue ---
$content_notifications_index_vue = @'
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useAccountStore } from '@/stores/account'
import type { AccountNotification, NotificationType } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

const PAGE_SIZE = 5

type Category = 'ALL' | 'UNREAD' | NotificationType

const category = ref<Category>((route.query.category as Category) || 'ALL')
const page = ref(Number(route.query.page) || 1)
const selectedId = ref<string>((route.query.notificationId as string) || '')
const mobileShowDetail = ref(false)

const notifications = computed(() => accountStore.notifications)

const filtered = computed(() => {
  let list = [...notifications.value]
  if (category.value === 'UNREAD') {
    list = list.filter((item) => !item.read)
  } else if (category.value !== 'ALL') {
    list = list.filter((item) => item.type === category.value)
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / PAGE_SIZE)))

const paged = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filtered.value.slice(start, start + PAGE_SIZE)
})

const selected = computed(() =>
  notifications.value.find((item) => item.id === selectedId.value) || null,
)

const stats = computed(() => ({
  all: notifications.value.length,
  unread: notifications.value.filter((item) => !item.read).length,
  pending: notifications.value.filter((item) => item.pendingAction && !item.read).length,
}))

const categories: { key: Category; label: string }[] = [
  { key: 'ALL', label: '全部' },
  { key: 'UNREAD', label: '未读' },
  { key: 'VERIFICATION', label: '认证通知' },
  { key: 'TEAM', label: '球队通知' },
  { key: 'EVENT', label: '赛事通知' },
  { key: 'SYSTEM', label: '系统通知' },
]

function syncQuery() {
  router.replace({
    query: {
      ...(category.value !== 'ALL' ? { category: category.value } : {}),
      ...(page.value > 1 ? { page: String(page.value) } : {}),
      ...(selectedId.value ? { notificationId: selectedId.value } : {}),
    },
  })
}

function setCategory(value: Category) {
  category.value = value
  page.value = 1
  syncQuery()
}

function selectNotification(item: AccountNotification) {
  selectedId.value = item.id
  accountStore.markNotificationRead(item.id)
  mobileShowDetail.value = true
  syncQuery()
}

function markAllRead() {
  accountStore.markAllNotificationsRead()
}

function goAction(item: AccountNotification) {
  if (item.actionType === 'VIEW_VERIFICATION') {
    router.push('/account/player-verification')
  } else if (item.actionType === 'HANDLE_TEAM_INVITATION') {
    router.push(`/account/team-relations?invitationId=${item.actionTargetId}`)
  } else if (item.actionType === 'VIEW_EVENT') {
    router.push('/tournament')
  }
}

watch([category, page], syncQuery)
</script>

<template>
  <div class="account-page account-center-vars">
    <AccountPageHeader
      zh-title="消息通知"
      en-title="NOTIFICATIONS"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '消息通知' },
      ]"
    />

    <AccountStatusBanner
      title="系统消息"
      description="认证、球队、赛事与账号通知统一送达"
      :status-text="`未读消息 ${stats.unread}`"
      status-type="info"
    />

    <div class="notification-stats">
      <div class="notification-stats__item">
        <strong>{{ stats.all }}</strong>
        <span>全部消息</span>
      </div>
      <div class="notification-stats__item">
        <strong>{{ stats.unread }}</strong>
        <span>未读消息</span>
      </div>
      <div class="notification-stats__item">
        <strong>{{ stats.pending }}</strong>
        <span>待处理事项</span>
      </div>
      <button type="button" class="account-btn-secondary" @click="markAllRead">全部标为已读</button>
    </div>

    <div class="notification-tabs">
      <button
        v-for="item in categories"
        :key="item.key"
        type="button"
        class="notification-tabs__btn"
        :class="{ 'notification-tabs__btn--active': category === item.key }"
        @click="setCategory(item.key)"
      >
        {{ item.label }}
      </button>
    </div>

    <div class="notification-layout">
      <section class="notification-list" :class="{ 'notification-list--hidden-mobile': mobileShowDetail }">
        <AccountEmptyState v-if="!paged.length" message="暂无消息" />
        <button
          v-for="item in paged"
          :key="item.id"
          type="button"
          class="notification-item"
          :class="{ 'notification-item--active': selectedId === item.id, 'notification-item--unread': !item.read }"
          @click="selectNotification(item)"
        >
          <span class="notification-item__dot" aria-hidden="true" />
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.summary }}</p>
            <time>{{ formatDateTime(item.createdAt) }}</time>
          </div>
        </button>

        <div v-if="totalPages > 1" class="notification-pagination">
          <button
            type="button"
            class="account-btn-secondary"
            :disabled="page <= 1"
            @click="page -= 1; syncQuery()"
          >
            上一页
          </button>
          <span>{{ page }} / {{ totalPages }}</span>
          <button
            type="button"
            class="account-btn-secondary"
            :disabled="page >= totalPages"
            @click="page += 1; syncQuery()"
          >
            下一页
          </button>
        </div>
      </section>

      <section class="notification-detail" :class="{ 'notification-detail--visible-mobile': mobileShowDetail }">
        <button
          v-if="mobileShowDetail"
          type="button"
          class="notification-detail__back account-btn-secondary"
          @click="mobileShowDetail = false"
        >
          返回列表
        </button>

        <AccountEmptyState v-if="!selected" message="请选择一条消息" />

        <article v-else class="notification-detail__content">
          <h2>{{ selected.title }}</h2>
          <p class="notification-detail__meta">
            {{ formatDateTime(selected.createdAt) }} · {{ selected.metadata.source || 'CNPA 系统' }}
          </p>
          <p class="notification-detail__body">{{ selected.content }}</p>
          <div class="notification-detail__actions">
            <button
              v-if="selected.actionType === 'VIEW_VERIFICATION'"
              type="button"
              class="account-btn-primary"
              @click="goAction(selected)"
            >
              查看球员认证
            </button>
            <button
              v-if="selected.actionType === 'HANDLE_TEAM_INVITATION'"
              type="button"
              class="account-btn-primary"
              @click="goAction(selected)"
            >
              处理球队邀请
            </button>
            <button
              v-if="selected.actionType === 'VIEW_EVENT'"
              type="button"
              class="account-btn-primary"
              @click="goAction(selected)"
            >
              查看赛事详情
            </button>
            <button
              v-if="!selected.read"
              type="button"
              class="account-btn-secondary"
              @click="accountStore.markNotificationRead(selected.id)"
            >
              标记为已读
            </button>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-page {
  padding: clamp(24px, 3vw, 40px) clamp(20px, 3vw, 48px) 48px;
}

.notification-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 16px 20px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.12);
}

.notification-stats__item {
  display: flex;
  flex-direction: column;
  min-width: 80px;

  strong {
    font-size: 22px;
    color: var(--account-red);
  }

  span {
    font-size: 12px;
    color: #696969;
  }
}

.notification-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.notification-tabs__btn {
  padding: 8px 14px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;

  &--active {
    background: var(--account-red);
    border-color: var(--account-red);
    color: #fff;
  }
}

.notification-layout {
  display: grid;
  grid-template-columns: 42% 58%;
  gap: 16px;
  min-height: 360px;
}

.notification-list,
.notification-detail {
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.12);
  padding: 12px;
  overflow-y: auto;
  max-height: 520px;
}

.notification-item {
  display: flex;
  gap: 10px;
  width: 100%;
  padding: 12px;
  border: 0;
  border-bottom: 1px solid rgba(20, 20, 20, 0.08);
  background: transparent;
  text-align: left;
  font-family: inherit;
  cursor: pointer;

  &--active {
    background: rgba(213, 8, 13, 0.08);
  }

  strong {
    display: block;
    margin-bottom: 4px;
    font-size: 14px;
  }

  p {
    margin: 0 0 4px;
    font-size: 12px;
    color: #696969;
  }

  time {
    font-size: 11px;
    color: #aaa;
  }
}

.notification-item__dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  border: 1px solid rgba(20, 20, 20, 0.2);
  flex-shrink: 0;
}

.notification-item--unread .notification-item__dot {
  background: var(--account-red);
  border-color: var(--account-red);
}

.notification-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding-top: 12px;
}

.notification-detail__content h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.notification-detail__meta {
  margin: 0 0 16px;
  font-size: 12px;
  color: #696969;
}

.notification-detail__body {
  margin: 0 0 20px;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.notification-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.notification-detail__back {
  display: none;
  margin-bottom: 12px;
}

@media (max-width: 991px) {
  .notification-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .notification-list--hidden-mobile {
    display: none;
  }

  .notification-detail {
    display: none;
  }

  .notification-detail--visible-mobile {
    display: block;
  }

  .notification-detail__back {
    display: inline-flex;
  }
}
</style>
'@
$path = Join-Path $root "src\views\account\notifications\index.vue"
[System.IO.File]::WriteAllText($path, $content_notifications_index_vue, $utf8)
$fixed += "notifications/index.vue"

# --- RegisterVerifyEmail.vue ---
$content_RegisterVerifyEmail_vue = @'
<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail } from '@/composables/useRegisterFlow'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()

const digits = ref(['', '', '', '', '', ''])
const inputRefs = ref<Array<HTMLInputElement | null>>([])
const countdown = ref(60)
const verifying = ref(false)
const maskedEmail = computed(() => getMaskedRegisterEmail())

let timer: ReturnType<typeof setInterval> | null = null

function startCountdown() {
  countdown.value = 60
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      if (timer) clearInterval(timer)
      timer = null
      return
    }
    countdown.value -= 1
  }, 1000)
}

startCountdown()

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

function setInputRef(el: HTMLInputElement | null, index: number) {
  inputRefs.value[index] = el
}

function handleDigitInput(index: number, event: Event) {
  const target = event.target as HTMLInputElement
  const value = target.value.replace(/\D/g, '').slice(-1)
  digits.value[index] = value
  target.value = value

  if (value && index < digits.value.length - 1) {
    inputRefs.value[index + 1]?.focus()
  }
}

function handleDigitKeydown(index: number, event: KeyboardEvent) {
  if (event.key === 'Enter') {
    event.preventDefault()
    handleVerify()
    return
  }
  if (event.key !== 'Backspace') return
  if (digits.value[index]) {
    digits.value[index] = ''
    return
  }
  if (index > 0) {
    inputRefs.value[index - 1]?.focus()
    digits.value[index - 1] = ''
  }
}

function handlePaste(event: ClipboardEvent) {
  event.preventDefault()
  const pasted = event.clipboardData?.getData('text').replace(/\D/g, '').slice(0, 6) ?? ''
  pasted.split('').forEach((char, index) => {
    digits.value[index] = char
  })
  inputRefs.value[Math.min(pasted.length, 5)]?.focus()
}

async function handleVerify() {
  if (verifying.value) return
  verifying.value = true
  await new Promise((resolve) => window.setTimeout(resolve, 320))
  verifying.value = false
  router.push('/register/success')
}

function handleResend() {
  if (countdown.value > 0) return
  startCountdown()
}

function handleChangeEmail() {
  router.push('/register')
}

function goLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="auth-page auth-verify">
    <AuthStepper :current="2" />

    <header class="auth-register__title">
      <h1 class="auth-register__title-cn">邮箱验证</h1>
      <p class="auth-register__title-en">VERIFY EMAIL</p>
    </header>

    <div class="auth-verify__icon" aria-hidden="true">@</div>

    <p class="auth-verify__desc">验证码已发送至</p>
    <p class="auth-verify__email">{{ maskedEmail }}</p>
    <p class="auth-verify__timer">验证码 10 分钟内有效</p>

    <div class="verify-code" @paste="handlePaste">
      <input
        v-for="(_, index) in digits"
        :key="index"
        :ref="(el) => setInputRef(el as HTMLInputElement | null, index)"
        class="verify-code__input"
        type="text"
        inputmode="numeric"
        maxlength="1"
        :value="digits[index]"
        :aria-label="`验证码第 ${index + 1} 位`"
        @input="handleDigitInput(index, $event)"
        @keydown="handleDigitKeydown(index, $event)"
      />
    </div>

    <button
      type="button"
      class="auth-primary-button"
      :disabled="verifying"
      @click="handleVerify"
    >
      {{ verifying ? '验证中...' : '验证邮箱' }}
    </button>

    <div class="auth-verify__actions">
      <button type="button" class="auth-text-link" :disabled="countdown > 0" @click="handleResend">
        {{ countdown > 0 ? `${countdown}s 后重发` : '重新发送验证码' }}
      </button>
      <button type="button" class="auth-text-link" @click="handleChangeEmail">更换邮箱</button>
    </div>

    <p class="auth-form__hint">请检查垃圾邮件或广告邮件文件夹</p>

    <p class="auth-form__footer-link">
      已有账号？
      <button type="button" class="auth-text-link" @click="goLogin">立即登录</button>
    </p>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>
'@
$path = Join-Path $root "src\views\account\RegisterVerifyEmail.vue"
[System.IO.File]::WriteAllText($path, $content_RegisterVerifyEmail_vue, $utf8)
$fixed += "RegisterVerifyEmail.vue"

# --- RegisterSuccess.vue ---
$content_RegisterSuccess_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { getMaskedRegisterEmail, getRegisterEmail } from '@/composables/useRegisterFlow'
import AuthStepper from './components/AuthStepper.vue'

const router = useRouter()
const maskedEmail = computed(() => getMaskedRegisterEmail())

function goLogin() {
  router.push({
    path: '/login',
    query: { email: getRegisterEmail() },
  })
}

function goHome() {
  router.push('/home')
}
</script>

<template>
  <div class="auth-page auth-complete">
    <AuthStepper :current="3" />

    <header class="auth-register__title">
      <h1 class="auth-register__title-cn">注册完成</h1>
      <p class="auth-register__title-en">ACCOUNT READY</p>
    </header>

    <div class="auth-complete__icon" aria-hidden="true">
      <span class="auth-complete__check" />
    </div>

    <h2 class="auth-complete__heading">CNPA 账号创建成功</h2>
    <p class="auth-complete__email">绑定邮箱：{{ maskedEmail }}</p>

    <button type="button" class="auth-primary-button" @click="goLogin">立即登录</button>
    <button type="button" class="auth-secondary-button" @click="goHome">返回首页</button>

    <p class="auth-form__hint">登录后可进入个人中心并申请球员认证</p>

    <div class="auth-notice-strip">
      <span aria-hidden="true">✓</span>
      <span>你的 CNPA 账号已就绪，登录后即可管理个人资料与球员认证</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import './styles/auth-shared.scss';
</style>
'@
$path = Join-Path $root "src\views\account\RegisterSuccess.vue"
[System.IO.File]::WriteAllText($path, $content_RegisterSuccess_vue, $utf8)
$fixed += "RegisterSuccess.vue"

# --- player-identity/change-history.vue ---
$content_player_identity_change_history_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const history = computed(() => accountStore.identityChangeHistory)
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="资料变更记录"
      en-title="IDENTITY CHANGE HISTORY"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '资料变更记录' },
      ]"
    />

    <AccountStatusBanner
      :title="`永久球员身份 ${account?.playerNo}`"
      description="查看平台与游戏 ID 已生效的历史变更"
      status-text="只读记录"
      status-type="info"
    />

    <AccountPanel title="平台与游戏 ID 变更记录">
      <AccountEmptyState
        v-if="!history.length"
        large
        message="暂无变更记录"
        subtitle="已生效的平台与游戏 ID 变更将在此展示"
      />
      <div v-else class="platform-table-wrap">
        <table class="platform-table">
          <thead>
            <tr>
              <th>生效时间</th>
              <th>平台</th>
              <th>原游戏 ID</th>
              <th>新游戏 ID</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in history" :key="`${row.effectiveTime}-${index}`">
              <td>{{ row.effectiveTime }}</td>
              <td>{{ IDENTITY_PLATFORM_LABELS[row.platform] }}</td>
              <td>{{ row.oldGameId }}</td>
              <td>{{ row.newGameId }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="identity-footnote">历史赛事继续显示当届平台与游戏 ID 快照；审核中的修改不会作为已生效记录展示。</p>
      <div class="account-panel__action account-panel__action--right">
        <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/platforms')">
          返回平台与游戏 ID
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\change-history.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_change_history_vue, $utf8)
$fixed += "player-identity/change-history.vue"

# --- player-identity/apply.vue ---
$content_player_identity_apply_vue = @'
<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import PlayerIdentityApplyForm from '../components/PlayerIdentityApplyForm.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS } from '@/types/account'
import type { PlayerVerificationSnapshot } from '@/types/account'

const router = useRouter()
const accountStore = useAccountStore()

const profile = computed(() => accountStore.profile)
const draft = reactive({
  avatarUrl: profile.value?.avatarUrl || '',
  platform: '' as '' | 'PC' | 'PS' | 'XBOX',
  gameId: '',
  position: '' as '' | 'PG' | 'SG' | 'SF' | 'PF' | 'C',
  proofImageUrl: '',
})

const statusSummary = computed(() => ({
  avatar: draft.avatarUrl ? '已上传' : '未上传',
  platform: draft.platform ? IDENTITY_PLATFORM_LABELS[draft.platform] : '未选择',
  gameId: draft.gameId.trim() || '未填写',
  position: draft.position || '未选择',
  proof: draft.proofImageUrl ? '已上传' : '未上传',
}))

function handleSubmit(data: PlayerVerificationSnapshot & { avatarUrl?: string }) {
  if (data.avatarUrl) {
    accountStore.updateProfile({ avatarUrl: data.avatarUrl })
  }
  accountStore.submitPlayerVerification(data)
  ElMessage.success('认证申请已提交')
  router.push({ name: 'AccountPlayerIdentityPending' })
}

</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="球员认证申请"
      en-title="PLAYER VERIFICATION APPLICATION"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: '认证申请' },
      ]"
    />

    <AccountStatusBanner
      title="创建永久球员身份"
      description="填写认证资料并提交赛事管理员审核"
      status-text="资料未提交"
      status-type="warning"
    />

    <div class="identity-apply-grid">
      <AccountPanel title="认证资料">
        <PlayerIdentityApplyForm @submit="handleSubmit">
          <template #actions="{ complete, submit }">
            <div class="identity-apply-footer">
              <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity')">
                返回球员身份
              </button>
              <button
                type="button"
                class="account-btn-primary"
                :disabled="!complete"
                @click="submit()"
              >
                提交认证申请
              </button>
              <p v-if="!complete" class="identity-apply-footer__hint">请完成全部必填资料</p>
            </div>
          </template>
        </PlayerIdentityApplyForm>
      </AccountPanel>

      <div class="identity-apply-side">
        <AccountPanel title="提交说明">
          <ul class="identity-entry-rules">
            <li>审核通过后生成永久球员 ID，永久球员 ID 不可修改。</li>
            <li>同一平台的游戏 ID 不得被多个球员重复绑定。</li>
            <li>提交后进入管理员审核，审核期间不能重复提交。</li>
            <li>球员认证与具体球队、赛事报名相互独立。</li>
          </ul>
        </AccountPanel>

        <AccountPanel title="当前资料状态">
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">头像</span><span>{{ statusSummary.avatar }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ statusSummary.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ statusSummary.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ statusSummary.position }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>{{ statusSummary.proof }}</span></div>
          </div>
        </AccountPanel>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\apply.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_apply_vue, $utf8)
$fixed += "player-identity/apply.vue"

# --- player-identity/status.vue ---
$content_player_identity_status_vue = @'
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStepper from '@/components/account/AccountStepper.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import AccountConfirmDialog from '@/components/account/AccountConfirmDialog.vue'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS, GAME_PLATFORM_LABELS } from '@/types/account'
import { identityQueryToVerificationStatus, isIdentityRestrictedQuery } from '@/types/account'
import { mapVerificationPlatform, mockRejectedApplication } from '@/mock/playerIdentityMock'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'
import type { StepItem } from '@/components/account/AccountStepper.vue'

const props = defineProps<{
  statusMode: 'pending' | 'rejected' | 'approved'
}>()

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()
const showWithdraw = ref(false)

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)

watch(
  () => route.query.status,
  async (queryStatus) => {
    if (!import.meta.env.DEV || typeof queryStatus !== 'string') return
    const mapped = identityQueryToVerificationStatus(queryStatus)
    if (!mapped) return
    await accountStore.loadAccountMock(mapped, isIdentityRestrictedQuery(queryStatus))
  },
  { immediate: true },
)

onMounted(() => {
  const currentStatus = verification.value?.status
  if (props.statusMode === 'pending' && currentStatus !== 'PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'rejected' && currentStatus !== 'REJECTED') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
  if (props.statusMode === 'approved' && currentStatus !== 'APPROVED' && currentStatus !== 'CHANGE_PENDING') {
    router.replace(resolvePlayerIdentityRoute(accountStore))
  }
})

const platformLabel = computed(() => {
  const p = verification.value?.primaryPlatform
  if (!p) return '-'
  return IDENTITY_PLATFORM_LABELS[mapVerificationPlatform(p)] || GAME_PLATFORM_LABELS[p]
})

const reviewRemark = computed(
  () => verification.value?.reviewerComment || mockRejectedApplication.reviewRemark,
)

const pageConfig = computed(() => {
  if (props.statusMode === 'pending') {
    return {
      zhTitle: '球员认证',
      enTitle: 'PLAYER VERIFICATION',
      crumb: '审核中',
      bannerTitle: '球员认证申请',
      bannerDesc: '认证资料已提交，当前由管理员审核',
      bannerStatus: '审核中',
      bannerType: 'warning' as const,
      steps: [
        { label: '提交资料', state: 'done' },
        { label: '管理员审核', state: 'current' },
        { label: '认证生效', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '本次认证资料',
      badgeType: 'warning' as const,
      badgeText: '审核中',
      footnote: '撤回后可以修改认证资料；再次提交将生成新的审核记录。',
    }
  }
  if (props.statusMode === 'rejected') {
    return {
      zhTitle: '球员认证',
      enTitle: 'PLAYER VERIFICATION',
      crumb: '未通过',
      bannerTitle: '球员认证未通过',
      bannerDesc: '请查看管理员备注，修改资料后重新提交',
      bannerStatus: '未通过',
      bannerType: 'danger' as const,
      steps: [
        { label: '提交资料', state: 'done' },
        { label: '管理员审核', state: 'failed', subLabel: '未通过' },
        { label: '认证生效', state: 'upcoming' },
      ] as StepItem[],
      leftTitle: '原提交资料',
      badgeType: 'danger' as const,
      badgeText: '未通过',
      footnote: '认证未通过时不会生成永久球员 ID；重新提交将建立新的审核记录。',
    }
  }
  return {
    zhTitle: '球员认证',
    enTitle: 'PLAYER VERIFICATION',
    crumb: '已通过',
    bannerTitle: '永久球员身份',
    bannerDesc: '球员认证已通过，认证资料长期有效',
    bannerStatus: '已认证',
    bannerType: 'success' as const,
    steps: [
      { label: '提交资料', state: 'done' },
      { label: '管理员审核', state: 'done' },
      { label: '认证生效', state: 'done' },
    ] as StepItem[],
    leftTitle: '有效认证资料',
    badgeType: 'success' as const,
    badgeText: '认证球员',
    footnote: '球员认证长期有效，无需按赛事重复认证，永久球员 ID 不可修改。',
  }
})

function confirmWithdraw() {
  accountStore.withdrawPlayerVerification()
  showWithdraw.value = false
  ElMessage.success('已撤回申请，可修改资料后重新提交')
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goApply() {
  router.push({ name: 'AccountPlayerIdentityApply' })
}

function goPlatforms() {
  router.push({ name: 'AccountPlatformGameId' })
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      :zh-title="pageConfig.zhTitle"
      :en-title="pageConfig.enTitle"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '球员身份', to: '/account/player-identity' },
        { label: pageConfig.crumb },
      ]"
    />

    <AccountStatusBanner
      :title="pageConfig.bannerTitle"
      :description="pageConfig.bannerDesc"
      :status-text="pageConfig.bannerStatus"
      :status-type="pageConfig.bannerType"
    />

    <AccountStepper :steps="pageConfig.steps" />

    <div class="identity-review-grid">
      <AccountPanel :title="pageConfig.leftTitle">
        <div class="identity-review-profile">
          <div
            class="identity-review-profile__avatar"
            :class="{
              'identity-review-profile__avatar--success': statusMode === 'approved',
              'identity-review-profile__avatar--error': statusMode === 'rejected',
            }"
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
            </svg>
          </div>
          <div>
            <strong>{{ account?.nickname }}</strong>
            <AccountStatusBadge :type="pageConfig.badgeType" :text="pageConfig.badgeText" />
          </div>
        </div>
        <div class="account-info-list">
          <template v-if="statusMode === 'approved'">
            <div class="account-info-row"><span class="account-info-row__label">已绑定平台</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">平台状态</span><span class="account-status-text--success">已生效</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">认证状态</span><span class="account-status-text--success">长期有效</span></div>
          </template>
          <template v-else>
            <div class="account-info-row"><span class="account-info-row__label">游戏平台</span><span>{{ platformLabel }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ verification?.gameId }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">常用位置</span><span>{{ verification?.gamePosition }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">证明材料</span><span>已上传 1 份</span></div>
            <div class="account-info-row"><span class="account-info-row__label">提交时间</span><span>{{ formatDateTime(verification?.submittedAt) }}</span></div>
          </template>
        </div>
      </AccountPanel>

      <AccountPanel title="当前状态">
        <div
          v-if="statusMode === 'pending'"
          class="identity-review-result identity-review-result--pending"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/></svg>
          </div>
          <strong>球员认证审核中</strong>
          <AccountStatusBadge type="warning" text="审核中" />
          <p>管理员将对整份认证资料进行审核</p>
          <p>审核期间不能重复提交</p>
          <button type="button" class="account-btn-secondary" @click="showWithdraw = true">撤回修改</button>
        </div>

        <div
          v-else-if="statusMode === 'rejected'"
          class="identity-review-result identity-review-result--failed"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/></svg>
          </div>
          <strong>球员认证未通过</strong>
          <AccountStatusBadge type="danger" text="未通过" />
          <p class="identity-review-result__remark-label">管理员备注</p>
          <p class="identity-review-result__remark">{{ reviewRemark }}</p>
          <button type="button" class="account-btn-primary" @click="goApply">修改并重新提交</button>
        </div>

        <div
          v-else
          class="identity-review-result identity-review-result--success"
        >
          <div class="identity-review-result__icon" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/></svg>
          </div>
          <span class="identity-review-result__label">永久球员 ID</span>
          <strong class="identity-review-result__player-no">{{ account?.playerNo }}</strong>
          <p>认证状态：已生效</p>
          <p>永久球员 ID 不可修改</p>
          <button type="button" class="account-btn-secondary" @click="goPlatforms">申请修改认证资料</button>
        </div>
      </AccountPanel>
    </div>

    <p class="identity-footnote">{{ pageConfig.footnote }}</p>

    <div v-if="statusMode === 'approved'" class="identity-shortcuts">
      <button type="button" class="account-btn-secondary" @click="goPlatforms">平台与游戏 ID 管理</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/player-identity/change-history')">资料变更记录</button>
      <button type="button" class="account-btn-secondary" @click="router.push('/account/my-tournaments')">我的赛事</button>
    </div>

    <AccountConfirmDialog
      v-if="statusMode === 'pending'"
      v-model="showWithdraw"
      title="撤回认证申请"
      message="确认撤回当前审核中的认证申请？撤回后可修改资料并重新提交。"
      confirm-text="确认撤回"
      @confirm="confirmWithdraw"
    />
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
'@
$path = Join-Path $root "src\views\account\player-identity\status.vue"
[System.IO.File]::WriteAllText($path, $content_player_identity_status_vue, $utf8)
$fixed += "player-identity/status.vue"

# --- overview/index.vue ---
$content_overview_index_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { useAccountStore } from '@/stores/account'
import { formatDateTime } from '@/utils/formatDate'
import { resolvePlayerIdentityRoute } from '@/utils/playerIdentityNavigate'

const router = useRouter()
const accountStore = useAccountStore()

const account = computed(() => accountStore.currentAccount)
const verification = computed(() => accountStore.verification)
const isVerified = computed(() => accountStore.isVerifiedPlayer)

const recentNotifications = computed(() =>
  accountStore.notifications.filter((item) =>
    ['欢迎加入 CNPA', '邮箱验证成功', '球员认证审核通过'].includes(item.title),
  ).slice(0, 3),
)

const joinedCount = computed(
  () => accountStore.myTournaments.filter((item) => item.status === 'JOINED').length,
)

const bannerStatusText = computed(() => {
  if (isVerified.value) return '认证球员'
  return '基础账号'
})

const bannerStatusExtra = computed(() =>
  account.value?.emailVerified ? '邮箱已验证' : '邮箱未验证',
)

const identityStatusText = computed(() => {
  const status = verification.value?.status
  if (status === 'PENDING') return '审核中'
  if (status === 'REJECTED') return '未通过'
  if (isVerified.value) return '已认证'
  return '未认证'
})

const playerIdText = computed(() =>
  isVerified.value && account.value?.playerNo ? account.value.playerNo : '未生成',
)

const pendingTasks = computed(() => {
  if (isVerified.value) {
    const invited = accountStore.myTournaments.filter((item) => item.status === 'INVITED').length
    if (invited > 0) {
      return {
        title: `你有 ${invited} 条赛事邀请`,
        action: '查看邀请',
        path: '/account/my-tournaments',
      }
    }
    return null
  }
  return {
    title: '完成球员认证',
    status: '待处理',
    desc: '认证通过后将生成永久球员 ID，并解锁赛事报名与球队加入。',
    action: '申请球员认证',
    path: '/account/player-identity/apply',
  }
})

function go(path: string) {
  router.push(path)
}

function goPlayerIdentity() {
  router.push(resolvePlayerIdentityRoute(accountStore))
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="个人中心"
      en-title="ACCOUNT CENTER"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心' },
      ]"
    />

    <AccountStatusBanner
      :title="`欢迎回来，${account?.nickname}`"
      description="管理账号资料、认证状态与当前待办"
      :status-text="`${bannerStatusText} · ${bannerStatusExtra}`"
      :status-type="isVerified ? 'success' : 'warning'"
    />

    <div class="account-overview-grid">
      <AccountPanel title="球员身份状态">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">账号状态</span>
            <span class="account-info-row__value account-status-text--success">正常</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">邮箱状态</span>
            <span class="account-info-row__value account-status-text--success">已验证</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">球员身份</span>
            <span class="account-info-row__value">{{ identityStatusText }}</span>
          </div>
          <div class="account-info-row">
            <span class="account-info-row__label">永久球员 ID</span>
            <span class="account-info-row__value">{{ playerIdText }}</span>
          </div>
        </div>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="goPlayerIdentity">
            查看球员身份
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="我的赛事">
        <div class="account-info-list">
          <div class="account-info-row">
            <span class="account-info-row__label">已加入赛事</span>
            <span class="account-info-row__value">{{ joinedCount }} / 3</span>
          </div>
        </div>
        <p class="account-view__desc">{{ joinedCount ? `当前参与 ${joinedCount} 项赛事` : '暂未加入赛事' }}</p>
        <p class="account-view__hint">完成球员认证后，可报名 Open 个人赛或接受球队邀请。</p>
        <div v-if="isVerified" class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="go('/account/my-tournaments')">
            查看我的赛事
          </button>
        </div>
      </AccountPanel>

      <AccountPanel title="当前待办">
        <template v-if="pendingTasks">
          <p class="account-view__desc">{{ pendingTasks.title }}</p>
          <p v-if="pendingTasks.status" class="account-pending-tag">{{ pendingTasks.status }}</p>
          <p v-if="pendingTasks.desc" class="account-view__hint">{{ pendingTasks.desc }}</p>
          <div class="account-panel__action">
            <button type="button" class="account-btn-primary" @click="go(pendingTasks.path)">
              {{ pendingTasks.action }}
            </button>
          </div>
        </template>
        <div v-else class="account-pending-empty">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" />
          </svg>
          <p>暂无待办事项</p>
        </div>
      </AccountPanel>

      <AccountPanel title="最近通知">
        <ul v-if="recentNotifications.length" class="account-notice-list">
          <li v-for="item in recentNotifications" :key="item.id">
            <span
              class="account-notice-list__dot"
              :class="{ 'account-notice-list__dot--unread': !item.read }"
            />
            <div>
              <strong>{{ item.title }}</strong>
              <time>{{ formatDateTime(item.createdAt) }}</time>
            </div>
          </li>
        </ul>
        <div v-else class="account-notification-empty">
          <p>暂无通知</p>
        </div>
        <div class="account-notification-footer">
          <button type="button" class="account-btn-secondary" @click="go('/account/notifications')">
            查看全部通知
          </button>
        </div>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view__desc {
  margin: 0 0 8px;
  font-size: clamp(18px, 1.6vw, 22px);
  font-weight: 800;
  color: var(--account-text);
}

.account-view__hint {
  margin: 0 0 16px;
  font-size: 14px;
  color: #696969;
  line-height: 1.6;
}

.account-pending-tag {
  display: inline-block;
  margin: 0 0 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(196, 30, 58, 0.12);
  color: var(--account-danger);
  font-size: 12px;
  font-weight: 800;
}
</style>
'@
$path = Join-Path $root "src\views\account\overview\index.vue"
[System.IO.File]::WriteAllText($path, $content_overview_index_vue, $utf8)
$fixed += "overview/index.vue"

# --- components/TeamInvitationDialog.vue ---
$content_components_TeamInvitationDialog_vue = @'
<script setup lang="ts">
import { computed } from 'vue'
import type { TeamInvitation } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const props = defineProps<{
  modelValue: boolean
  invitation: TeamInvitation | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  accept: [id: string]
  reject: [id: string]
}>()

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

function close() {
  open.value = false
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open && invitation" class="invitation-dialog">
      <button type="button" class="invitation-dialog__backdrop" aria-label="关闭" @click="close" />
      <div class="invitation-dialog__panel" role="dialog" aria-modal="true" aria-label="处理球队邀请">
        <h3>球队邀请</h3>
        <dl class="account-info-list">
          <div class="account-field-row"><dt>球队名称</dt><dd>{{ invitation.teamName }}</dd></div>
          <div class="account-field-row"><dt>赛事名称</dt><dd>{{ invitation.tournamentName }}</dd></div>
          <div class="account-field-row"><dt>邀请位置</dt><dd>{{ invitation.inviteRole }}</dd></div>
          <div class="account-field-row"><dt>邀请时间</dt><dd>{{ formatDateTime(invitation.createdAt) }}</dd></div>
          <div class="account-field-row"><dt>过期时间</dt><dd>{{ formatDateTime(invitation.expiresAt) }}</dd></div>
        </dl>
        <div class="invitation-dialog__actions">
          <button type="button" class="account-btn-secondary" @click="emit('reject', invitation.id); close()">
            拒绝邀请
          </button>
          <button type="button" class="account-btn-primary" @click="emit('accept', invitation.id); close()">
            接受邀请
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.invitation-dialog {
  position: fixed;
  inset: 0;
  z-index: 3100;
  display: grid;
  place-items: center;
  padding: 20px;
}

.invitation-dialog__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.invitation-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(460px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 900;
  }
}

.invitation-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>
'@
$path = Join-Path $root "src\views\account\components\TeamInvitationDialog.vue"
[System.IO.File]::WriteAllText($path, $content_components_TeamInvitationDialog_vue, $utf8)
$fixed += "components/TeamInvitationDialog.vue"

Write-Host "Fixed $($fixed.Count) files:"
$fixed | ForEach-Object { Write-Host "  $_" }
