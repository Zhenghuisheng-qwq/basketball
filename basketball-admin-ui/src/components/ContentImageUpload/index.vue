<template>
  <div class="content-image-upload">
    <el-upload
      :http-request="doUpload"
      :show-file-list="false"
      :before-upload="beforeUpload"
      accept=".jpg,.jpeg,.png,.webp,.gif,.bmp"
    >
      <img v-if="preview" :src="preview" class="preview" alt="" />
      <el-icon v-else class="uploader-icon"><Plus /></el-icon>
    </el-upload>
    <div class="actions">
      <span class="tip">{{ tip }}</span>
      <el-button v-if="modelValue" link type="primary" @click="clear">清除</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { UploadRequestOptions } from 'element-plus'
import { ElMessage } from 'element-plus'
import { uploadContentImage } from '@/api/basket/contentImage'

const props = withDefaults(defineProps<{
  modelValue?: number
  preview?: string
  usageType: string
  tip?: string
  fileSize?: number
}>(), {
  preview: '',
  tip: '上传后直接使用这张图',
  fileSize: 5
})

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined]
  'update:preview': [value: string]
}>()

function beforeUpload(file: File) {
  const ext = file.name.includes('.') ? file.name.slice(file.name.lastIndexOf('.') + 1).toLowerCase() : ''
  if (!['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'].includes(ext)) {
    ElMessage.error('只支持 jpg、png、webp、gif、bmp')
    return false
  }
  if (file.size / 1024 / 1024 > props.fileSize) {
    ElMessage.error(`图片不能超过 ${props.fileSize}MB`)
    return false
  }
  return true
}

async function doUpload(options: UploadRequestOptions) {
  try {
    const res = await uploadContentImage(options.file, props.usageType)
    const data = res.data
    if (!data?.fileId) {
      ElMessage.error(res.msg || '图片上传失败')
      return
    }
    emit('update:modelValue', data.fileId)
    emit('update:preview', data.accessUrl || '')
  } catch {
    return
  }
}

function clear() {
  emit('update:modelValue', undefined)
  emit('update:preview', '')
}
</script>

<style scoped>
.content-image-upload :deep(.el-upload) {
  width: 148px;
  height: 148px;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  overflow: hidden;
}
.preview {
  width: 148px;
  height: 148px;
  object-fit: cover;
  display: block;
}
.uploader-icon {
  width: 148px;
  height: 148px;
  font-size: 28px;
  color: #8c939d;
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
}
</style>
