<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountStatusBanner from '@/components/account/AccountStatusBanner.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { uploadAccountFile } from '@/api/accountFile'
import { useAccountStore } from '@/stores/account'
import { IDENTITY_PLATFORM_LABELS, type IdentityPlatform } from '@/types/account'
import { ElMessage } from 'element-plus'

const accountStore = useAccountStore()
const showAddDialog = ref(false)
const showPendingDialog = ref(false)
const submitting = ref(false)
const uploadingProof = ref(false)
const proofInputRef = ref<HTMLInputElement | null>(null)

const account = computed(() => accountStore.currentAccount)
const records = computed(() => accountStore.platformGameIds)

const addForm = reactive({
  platform: 'PC' as IdentityPlatform,
  gameId: '',
  proofFileId: '',
  proofFileName: '',
})

const pendingRecord = computed(() => records.value.find((item) => item.status === 'PENDING'))

const isAddMode = computed(() => {
  const row = records.value.find((item) => item.platform === addForm.platform)
  return !row?.platformAccountId || row.status === 'UNBOUND'
})

onMounted(async () => {
  try {
    await accountStore.loadPlayerPlatformState()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载平台数据失败')
  }
})

function openAdd(platform: IdentityPlatform) {
  addForm.platform = platform
  addForm.gameId = ''
  addForm.proofFileId = ''
  addForm.proofFileName = ''
  showAddDialog.value = true
}

function triggerProofUpload() {
  if (uploadingProof.value) return
  proofInputRef.value?.click()
}

async function handleProofUpload(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  if (!['image/jpeg', 'image/jpg', 'image/png'].includes(file.type)) {
    ElMessage.warning('仅支持 JPG、JPEG、PNG')
    return
  }
  if (file.size > 8 * 1024 * 1024) {
    ElMessage.warning('文件大小不能超过 8MB')
    return
  }
  uploadingProof.value = true
  try {
    const res = await uploadAccountFile(file, 'PLAYER_CERT_GAME_SCREENSHOT')
    addForm.proofFileId = res.data.fileId
    addForm.proofFileName = res.data.originalName || file.name
    ElMessage.success('证明材料已上传')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '上传失败')
  } finally {
    uploadingProof.value = false
  }
}

async function submitAdd() {
  if (!addForm.gameId.trim()) {
    ElMessage.warning('请填写游戏 ID')
    return
  }
  if (!addForm.proofFileId) {
    ElMessage.warning('请上传证明材料')
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await accountStore.submitPlatformGameId({
      platform: addForm.platform,
      gameId: addForm.gameId.trim(),
      proofFileId: addForm.proofFileId,
    })
    showAddDialog.value = false
    ElMessage.success('已提交审核')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
  }
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
        <h3>{{ isAddMode ? '新增' : '修改' }}游戏 ID</h3>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ IDENTITY_PLATFORM_LABELS[addForm.platform] }}</span></div>
        </div>
        <input v-model="addForm.gameId" class="identity-apply-form__input" placeholder="请输入游戏 ID" />
        <input
          ref="proofInputRef"
          type="file"
          class="platform-proof-upload__input"
          accept=".jpg,.jpeg,.png,image/jpeg,image/png"
          @change="handleProofUpload"
        />
        <button type="button" class="account-btn-secondary platform-proof-upload" :disabled="uploadingProof" @click="triggerProofUpload">
          {{ uploadingProof ? '上传中...' : addForm.proofFileId ? '重新上传证明材料' : '上传证明材料' }}
        </button>
        <p v-if="addForm.proofFileName" class="platform-proof-name">{{ addForm.proofFileName }}</p>
        <div class="account-dialog__actions">
          <button type="button" class="account-btn-secondary" @click="showAddDialog = false">取消</button>
          <button type="button" class="account-btn-primary" :disabled="submitting" @click="submitAdd">
            {{ submitting ? '提交中...' : '提交审核' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="showPendingDialog && pendingRecord" class="account-dialog-backdrop" @click.self="showPendingDialog = false">
      <div class="account-dialog">
        <h3>审核中详情</h3>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ IDENTITY_PLATFORM_LABELS[pendingRecord.platform] }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ pendingRecord.gameId || '待审核' }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">状态</span><span>{{ pendingRecord.statusText }}</span></div>
        </div>
        <button type="button" class="account-btn-secondary" @click="showPendingDialog = false">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.platform-proof-upload {
  display: inline-block;
  margin-top: 12px;
}

.platform-proof-upload__input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.platform-proof-name {
  margin: 8px 0 0;
  font-size: 13px;
  color: #666;
}
</style>
