import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
import { generateUUID } from '@/utils'
import { localeProps, makeRequiredRule } from '@/components/FormCreate/src/utils'

export const useUploadImgsRule = () => {
  const label = translate('modules.ndbb64af04e')
  const name = 'UploadImgs'
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
          type: 'switch',
          field: 'drag',
          get title() {
            return translate('modules.n9a587ae292')
          },
          value: false
        },
        {
          type: 'select',
          field: 'fileType',
          get title() {
            return translate('modules.n9c6b87cfcb')
          },
          value: ['image/jpeg', 'image/png', 'image/gif'],
          options: [
            { label: 'image/apng', value: 'image/apng' },
            { label: 'image/bmp', value: 'image/bmp' },
            { label: 'image/gif', value: 'image/gif' },
            { label: 'image/jpeg', value: 'image/jpeg' },
            { label: 'image/pjpeg', value: 'image/pjpeg' },
            { label: 'image/svg+xml', value: 'image/svg+xml' },
            { label: 'image/tiff', value: 'image/tiff' },
            { label: 'image/webp', value: 'image/webp' },
            { label: 'image/x-icon', value: 'image/x-icon' }
          ],
          props: {
            multiple: true
          }
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
          type: 'input',
          field: 'height',
          get title() {
            return translate('modules.n9f52d61e09')
          },
          value: '150px'
        },
        {
          type: 'input',
          field: 'width',
          get title() {
            return translate('modules.n5c836ad5c5')
          },
          value: '150px'
        },
        {
          type: 'input',
          field: 'borderradius',
          get title() {
            return translate('modules.n90be476211')
          },
          value: '8px'
        }
      ])
    }
  }
}
