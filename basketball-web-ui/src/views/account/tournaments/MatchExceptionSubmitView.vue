<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { getExceptionAvailable, submitMatchException } from '@/api/matchException'
import { uploadAccountFile, getAccountFileUrl } from '@/api/accountFile'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage({ requireCaptain: true })

const matchId = computed(() => route.params.matchId as string)
const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const available = ref<Record<string, any> | null>(null)
const materialPreviews = ref<{ fileId: number; name: string; url: string }[]>([])

const form = ref({
  exceptionType: 'FORFEIT',
  exceptionReason: '',
  remark: '',
  materialFileIds: [] as number[]
})

const exceptionTypes = [
  { value: 'FORFEIT', label: '对方弃赛' },
  { value: 'NETWORK_ERROR', label: '网络异常' },
  { value: 'POSTPONE', label: '双方约定延期' },
  { value: 'UNFINISHED', label: '比赛无法完成' },
  { value: 'RESULT_ABNORMAL', label: '结果异常' },
  { value: 'OTHER', label: '其他' }
]

async function loadAvailable() {
  loading.value = true
  try {
    const res = await getExceptionAvailable(matchId.value)
    available.value = res.data as any
    if (available.value?.currentApplicationId) {
      router.replace(teamPath(`/match-data/${matchId.value}/exception-status`))
      return
    }
    if (!available.value?.canSubmit && available.value?.cannotReason) {
      ElMessage.warning(available.value.cannotReason)
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function onPickMaterial(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadAccountFile(file, 'MATCH_DATA_SCREENSHOT')
    const fileId = Number(res.data?.fileId)
    if (!fileId) throw new Error('上传失败')
    form.value.materialFileIds.push(fileId)
    materialPreviews.value.push({
      fileId,
      name: res.data?.originalName || file.name,
      url: getAccountFileUrl(fileId)
    })
    ElMessage.success('材料上传成功')
  } catch (err: any) {
    ElMessage.error(err?.message || '上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function removeMaterial(fileId: number) {
  form.value.materialFileIds = form.value.materialFileIds.filter((id) => id !== fileId)
  materialPreviews.value = materialPreviews.value.filter((m) => m.fileId !== fileId)
}

async function handleSubmit() {
  if (submitting.value) return
  if (!form.value.exceptionReason.trim()) {
    ElMessage.warning('请填写异常说明')
    return
  }
  if (!form.value.materialFileIds.length) {
    ElMessage.warning('请至少上传一张证明材料')
    return
  }
  submitting.value = true
  try {
    await submitMatchException(matchId.value, {
      exceptionType: form.value.exceptionType,
      exceptionReason: form.value.exceptionReason.trim(),
      materialFileIds: form.value.materialFileIds.map(String),
      remark: form.value.remark || undefined
    })
    ElMessage.success('异常申请已提交，等待管理员裁定')
    router.push(teamPath(`/match-data/${matchId.value}/exception-status`))
  } catch (e: any) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await eventTeamStore.ensureLoaded(String(route.params.tournamentId))
  loadAvailable()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="异常比赛申请"
      en-title="MATCH EXCEPTION"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: '异常申请' }]"
    />

    <AccountPanel v-loading="loading" title="异常申请">
      <template v-if="available">
        <p v-if="available.homeTeamName && available.awayTeamName">
          <strong>{{ available.homeTeamName }} vs {{ available.awayTeamName }}</strong>
        </p>
        <p v-if="available.scheduledStartTime" class="event-team-muted">
          比赛时间：{{ available.scheduledStartTime }}
        </p>
        <p v-if="!available.canSubmit" class="warn">{{ available.cannotReason || '当前不可提交异常申请' }}</p>

        <template v-if="available.canSubmit">
          <h4>异常类型</h4>
          <select v-model="form.exceptionType" class="full-select">
            <option v-for="t in exceptionTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>

          <h4>异常说明</h4>
          <textarea v-model="form.exceptionReason" rows="4" placeholder="请详细说明异常情况" />

          <h4>证明材料</h4>
          <div class="material-row">
            <label class="account-btn-secondary upload-btn">
              {{ uploading ? '上传中...' : '上传材料' }}
              <input type="file" accept="image/*" hidden :disabled="uploading" @change="onPickMaterial" />
            </label>
            <div v-for="m in materialPreviews" :key="m.fileId" class="material-item">
              <img :src="m.url" :alt="m.name" class="material-thumb" />
              <button type="button" class="link-btn" @click="removeMaterial(m.fileId)">移除</button>
            </div>
          </div>

          <label class="remark-row">备注<textarea v-model="form.remark" rows="2" placeholder="可选" /></label>
        </template>
      </template>

      <div class="account-panel__action">
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath(`/match-data/${matchId}/status`))">
          返回状态
        </button>
        <button
          v-if="available?.canSubmit"
          type="button"
          class="account-btn-primary"
          :disabled="submitting || uploading"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交申请' }}
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.full-select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-bottom: 12px;
}

input, textarea, select {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-top: 4px;
}

.warn {
  color: #c45656;
  margin-bottom: 12px;
}

.material-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.upload-btn {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}

.material-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: center;
}

.material-thumb {
  width: 120px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #ddd;
}

.link-btn {
  background: none;
  border: none;
  color: #c45656;
  cursor: pointer;
  font-size: 12px;
}

.remark-row {
  display: block;
  margin-bottom: 16px;
}

h4 {
  margin: 16px 0 8px;
  font-size: 14px;
}
</style>
