import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
import { generateUUID } from '@/utils'
import { localeProps, makeRequiredRule } from '@/components/FormCreate/src/utils'

/**
 * iframe 组件规则
 */
export const useIframeRule = () => {
  const label = translate('modules.n6add406bdf')
  const name = 'IframeComponent'

  return {
    icon: 'icon-link',
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
          field: 'url',
          get title() {
            return translate('modules.n2474084d17')
          },
          value: '',
          get info() {
            return translate('modules.n1ed58bf2b9')
          }
        },
        {
          type: 'input',
          field: 'height',
          get title() {
            return translate('modules.ne6f1112c23')
          },
          value: '500px',
          get info() {
            return translate('modules.nbb8fc85d47')
          }
        },
        {
          type: 'input',
          field: 'width',
          get title() {
            return translate('modules.n94613cf334')
          },
          value: '100%',
          get info() {
            return translate('modules.n073d231f29')
          }
        },
        {
          type: 'select',
          field: 'loading',
          get title() {
            return translate('modules.n455d7aafd8')
          },
          value: 'lazy',
          options: [
            {
              get label() {
                return translate('modules.nd05c55bc5b')
              },
              value: 'lazy'
            },
            {
              get label() {
                return translate('modules.n3ed1df2a8d')
              },
              value: 'eager'
            }
          ]
        },
        {
          type: 'switch',
          field: 'allowfullscreen',
          get title() {
            return translate('modules.n27040177a8')
          },
          value: true
        },
        {
          type: 'input',
          field: 'sandbox',
          get title() {
            return translate('modules.n86058f2bef')
          },
          value: '',
          get info() {
            return translate('modules.ndbed99cd09')
          }
        }
      ])
    }
  }
}
