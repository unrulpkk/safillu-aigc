import { watchEffect, ref } from 'vue'
import { i18n } from '@/plugins/vueI18n'
const pageKey = ref('')
let watching = false
export const useTitle = (newTitle?: string) => {
  pageKey.value = newTitle || ''
  if (!watching) {
    watching = true
    watchEffect(() => {
      const t = i18n.global.t as (key: string) => string
      document.title = pageKey.value
        ? `${t('admin.brand')} - ${t(pageKey.value)}`
        : t('admin.brand')
    })
  }
  return pageKey
}
