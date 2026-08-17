import { onUnmounted, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { fetchAccountFileBlobUrl } from '@/api/accountFile'

export function useAccountFilePreview(fileIds: MaybeRefOrGetter<Array<string | number | undefined | null>>) {
  const urls = ref<Record<string, string>>({})
  const previewUrl = ref('')
  const objectUrls: string[] = []

  function revokeAll() {
    objectUrls.splice(0).forEach((url) => URL.revokeObjectURL(url))
  }

  async function reload() {
    revokeAll()
    const next: Record<string, string> = {}
    for (const id of toValue(fileIds)) {
      const key = String(id || '').trim()
      if (!key) continue
      try {
        const url = await fetchAccountFileBlobUrl(key)
        next[key] = url
        objectUrls.push(url)
      } catch {
        next[key] = ''
      }
    }
    urls.value = next
    if (previewUrl.value && !Object.values(next).includes(previewUrl.value)) {
      previewUrl.value = ''
    }
  }

  watch(() => toValue(fileIds).map((id) => String(id || '')).join(','), () => {
    void reload()
  }, { immediate: true })

  onUnmounted(() => {
    previewUrl.value = ''
    revokeAll()
  })

  function openPreview(fileId: string | number) {
    previewUrl.value = urls.value[String(fileId)] || ''
  }

  function closePreview() {
    previewUrl.value = ''
  }

  return { urls, previewUrl, openPreview, closePreview }
}

