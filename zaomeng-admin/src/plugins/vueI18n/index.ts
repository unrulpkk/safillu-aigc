import type { App } from 'vue'
import { createI18n } from 'vue-i18n'
import { useLocaleStoreWithOut } from '@/store/modules/locale'
import { setHtmlPageLang } from './helper'
import zh from '@/locales/zh-CN'
import en from '@/locales/en'
import es from '@/locales/es'

// All supported dictionaries are ready before render; switching cannot race or reload a form.
export let i18n: ReturnType<typeof createI18n>
export const setupI18n = async (app: App<Element>) => {
  const store = useLocaleStoreWithOut()
  const locale = store.getCurrentLocale.lang
  store.setCurrentLocale({ lang: locale })
  setHtmlPageLang(locale)
  i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'zh-CN',
    messages: { 'zh-CN': zh, en, es },
    missingWarn: false,
    fallbackWarn: false
  })
  app.use(i18n)
}
