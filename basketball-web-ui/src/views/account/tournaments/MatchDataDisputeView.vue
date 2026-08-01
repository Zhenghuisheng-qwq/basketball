<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { getDisputeAvailable, submitMatchDispute } from '@/api/matchDispute'
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
  disputeType: 'SCORE_ERROR',
  disputeReason: '',
  expectedHomeScore: undefined as number | undefined,
  expectedAwayScore: undefined as number | undefined,
  remark: '',
  materialFileIds: [] as number[]
})

const disputeTypes = [
  { value: 'SCORE_ERROR', label: '比分错误' },
  { value: 'PERIOD_SCORE_ERROR', label: '分节比分错误' },
  { value: 'TEAM_STAT_ERROR', label: '球队数据错误' },
  { value: 'PLAYER_STAT_ERROR', label: '球员数据错误' },
  { value: 'MATERIAL_NOT_CLEAR', label: '材料不清晰' },
  { value: 'OTHER', label: '其他' }
]

const remainingText = computed(() => {
  const sec = available.value?.remainingSeconds
  if (sec == null || sec <= 0) return ''
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h} 小时 ${m} 分钟` : `${m} 分钟`
})

async function loadAvailable() {
  loading.value = true
  try {
    const res = await getDisputeAvailable(matchId.value)
    available.value = res.data as any
    if (available.value?.currentDisputeId) {
      router.replace(teamPath(`/match-data/${matchId.value}/dispute-status`))
      return
    }
    if (!available.value?.canDispute && available.value?.cannotReason) {
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
  if (!form.value.disputeReason.trim()) {
    ElMessage.warning('请填写争议说明')
    return
  }
  if (!form.value.materialFileIds.length) {
    ElMessage.warning('请至少上传一张证明材料')
    return
  }
  submitting.value = true
  try {
    await submitMatchDispute(matchId.value, {
      disputeType: form.value.disputeType,
      disputeReason: form.value.disputeReason.trim(),
      materialFileIds: form.value.materialFileIds.map(String),
      expectedHomeScore: form.value.expectedHomeScore,
      expectedAwayScore: form.value.expectedAwayScore,
      remark: form.value.remark || undefined
    })
    ElMessage.success('争议已提交，等待管理员处理')
    router.push(teamPath(`/match-data/${matchId.value}/dispute-status`))
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
      zh-title="发起数据争议"
      en-title="MATCH DATA DISPUTE"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: '数据争议' }]"
    />

    <AccountPanel v-loading="loading" title="争议申请">
      <template v-if="available">
        <p v-if="available.homeTeamName && available.awayTeamName">
          <strong>{{ available.homeTeamName }} vs {{ available.awayTeamName }}</strong>
        </p>
        <p v-if="available.homeScore != null">
          <strong>当前正式比分：</strong>{{ available.homeScore }} - {{ available.awayScore }}
        </p>
        <p v-if="available.deadlineTime" class="event-team-muted">
          争议截止时间：{{ available.deadlineTime }}
          <span v-if="remainingText">（剩余 {{ remainingText }}）</span>
        </p>
        <p v-if="!available.canDispute" class="warn">{{ available.cannotReason || '当前不可发起争议' }}</p>

        <template v-if="available.canDispute">
          <h4>争议类型</h4>
          <select v-model="form.disputeType" class="full-select">
            <option v-for="t in disputeTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>

          <h4>期望比分（可选）</h4>
          <div class="form-grid">
            <label>主队<input v-model.number="form.expectedHomeScore" type="number" min="0" placeholder="可选" /></label>
            <label>客队<input v-model.number="form.expectedAwayScore" type="number" min="0" placeholder="可选" /></label>
          </div>

          <h4>争议说明</h4>
          <textarea v-model="form.disputeReason" rows="4" placeholder="请详细说明争议内容" />

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
          v-if="available?.canDispute"
          type="button"
          class="account-btn-primary"
          :disabled="submitting || uploading"
          @click="handleSubmit"
        >
          {{ submitting ? '提交中...' : '提交争议' }}
        </button>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

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
