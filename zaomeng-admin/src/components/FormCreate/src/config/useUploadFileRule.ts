import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
import { generateUUID } from '@/utils'
import { localeProps, makeRequiredRule } from '@/components/FormCreate/src/utils'

export const useUploadFileRule = () => {
  const label = translate('native.nd870a31beb')
  const name = 'UploadFile'
  return {
    icon: 'icon-upload',
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
          type: 'select',
          field: 'fileType',
          get title() {
            return translate('native.n9a8457b3dc')
          },
          value: ['doc', 'xls', 'ppt', 'txt', 'pdf'],
          options: [
            { label: 'doc', value: 'doc' },
            { label: 'xls', value: 'xls' },
            { label: 'ppt', value: 'ppt' },
            { label: 'txt', value: 'txt' },
            { label: 'pdf', value: 'pdf' }
          ],
          props: {
            multiple: true
          }
        },
        {
          type: 'switch',
          field: 'autoUpload',
          get title() {
            return translate('modules.n2d40d7caad')
          },
          value: true
        },
        {
          type: 'switch',
          field: 'drag',
          get title() {
            return translate('modules.n9a587ae292')
          },
          value: false
        },
        {
          type: 'switch',
          field: 'isShowTip',
          get title() {
            return translate('modules.n1bb009dbc8')
          },
          value: true
        },
        {
          type: 'inputNumber',
          field: 'fileSize',
          get title() {
            return translate('modules.nce9ac13218')
          },
          value: 5,
          props: { min: 0 }
        },
        {
          type: 'inputNumber',
          field: 'limit',
          get title() {
            return translate('modules.naa36dc0957')
          },
          value: 5,
          props: { min: 0 }
        },
        {
          type: 'switch',
          field: 'disabled',
          get title() {
            return translate('modules.n8da75514d7')
          },
          value: false
        }
      ])
    }
  }
}
