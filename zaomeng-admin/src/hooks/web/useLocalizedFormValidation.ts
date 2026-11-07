import { type Ref, watch } from 'vue'
import { useLocaleStore } from '@/store/modules/locale'

// A locale change updates labels; it is not a form submission or validation attempt.
export function useLocalizedFormValidation(form: Ref<any>) {
  const locale = useLocaleStore()
  watch(
    () => locale.getCurrentLocale.lang,
    () => {
      const forms = Array.isArray(form.value) ? form.value : [form.value]
      forms.forEach((instance) => instance?.clearValidate?.())
    },
    { flush: 'post' }
  )
}
