<template>
  <div class="honor-icon-panel">
    <el-alert
      title="每个赛事对应一张球员荣誉头像。上传后，该赛事下的球员荣誉卡会显示这张图。"
      type="info"
      :closable="false"
      show-icon
    />
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column label="赛事" prop="seriesName" min-width="180" />
      <el-table-column label="编码" prop="seriesCode" width="160" />
      <el-table-column label="当前头像" width="120" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.honorIconUrl"
            :src="row.honorIconUrl"
            fit="contain"
            class="honor-icon-panel__preview"
            :preview-src-list="[row.honorIconUrl]"
            preview-teleported
          />
          <span v-else class="honor-icon-panel__empty">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="上传 / 更换" min-width="220">
        <template #default="{ row }">
          <ImageUpload
            :model-value="row.honorIconUrl || ''"
            :limit="1"
            :drag="false"
            :is-show-tip="false"
            action="/common/upload"
            @update:model-value="(url: string) => save(row, url)"
          />
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listSeriesHonorIcons, updateSeriesHonorIcon, type BasketSeriesVo } from '@/api/cnpa/event-v4/series'

const loading = ref(false)
const rows = ref<BasketSeriesVo[]>([])

async function load() {
  loading.value = true
  try {
    const res = await listSeriesHonorIcons()
    rows.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function save(row: BasketSeriesVo, url: string) {
  const next = (url || '').trim()
  const prev = row.honorIconUrl || ''
  if (!row.id || next === prev) return
  try {
    await updateSeriesHonorIcon(row.id, next)
    row.honorIconUrl = next
    ElMessage.success(next ? '头像已保存' : '头像已清除')
  } catch {
    row.honorIconUrl = prev
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.honor-icon-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.honor-icon-panel__preview {
  width: 72px;
  height: 72px;
  background: #111;
}

.honor-icon-panel__empty {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

:deep(.el-upload--picture-card),
:deep(.el-upload-list--picture-card .el-upload-list__item) {
  width: 72px;
  height: 72px;
}
</style>
