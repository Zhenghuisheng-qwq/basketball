import { storeToRefs } from 'pinia'
import { useLocaleStore } from '@/stores/locale'

export function useAppLocale() {
  const store = useLocaleStore()
  const { locale } = storeToRefs(store)
  return {
    locale,
    setLocale: store.setLocale,
  }
}
