import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
import { generateUUID } from '@/utils'
import { localeProps, makeRequiredRule } from '@/components/FormCreate/src/utils'

export const useEditorRule = () => {
  const label = translate('modules.na23fcd0b26')
  const name = 'Editor'
  return {
    icon: 'icon-editor',
    label,
    name,
    rule() {
      return {
        type: name,
        field: generateUUID(),
        title: label,
        info: '',
        $required: false
      }
    },
    props(_, { t }) {
      return localeProps(t, name + '.props', [
        makeRequiredRule(),
        {
          type: 'input',
          field: 'height',
          get title() {
            return translate('modules.n968dd036cb')
          }
        },
        {
          type: 'switch',
          field: 'readonly',
          get title() {
            return translate('modules.nf2ff686b7d')
          }
        }
      ])
    }
  }
}
