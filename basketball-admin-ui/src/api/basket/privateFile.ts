import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'
import { blobValidate } from '@/utils/ruoyi'

const baseURL = import.meta.env.VITE_APP_BASE_API

/** 预览私有认证材料文件 */
export async function previewPrivateFile(fileId: number | string) {
  try {
    const res = await axios.get(`${baseURL}/basket/private-file/${fileId}`, {
      responseType: 'blob',
      headers: { Authorization: 'Bearer ' + getToken() }
    })
    if (blobValidate(res.data)) {
      const url = window.URL.createObjectURL(new Blob([res.data]))
      window.open(url, '_blank')
      setTimeout(() => window.URL.revokeObjectURL(url), 60000)
      return
    }
    const text = await (res.data as Blob).text()
    const json = JSON.parse(text)
    ElMessage.error(json.msg || '文件预览失败')
  } catch {
    ElMessage.error('文件预览失败')
  }
}
