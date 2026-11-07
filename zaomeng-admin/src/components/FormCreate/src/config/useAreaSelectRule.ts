import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
import { generateUUID } from '@/utils'
import { localeProps, makeRequiredRule } from '@/components/FormCreate/src/utils'
import { AreaLevelEnum } from '@/utils/constants'

/**
 * 省市区选择器规则
 */
export const useAreaSelectRule = () => {
  const label = translate('modules.nc40e9f7ce4')
  const name = 'AreaSelect'

  return {
    icon: 'icon-location',
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
          field: 'level',
          get title() {
            return translate('modules.n4b62325436')
          },
          value: AreaLevelEnum.DISTRICT,
          options: [
            {
              get label() {
                return translate('modules.nf5dcf19da3')
              },
              value: AreaLevelEnum.PROVINCE
            },
            {
              get label() {
                return translate('modules.n8da2c8a1dd')
              },
              value: AreaLevelEnum.CITY
            },
            {
              get label() {
                return translate('modules.n6846089a29')
              },
              value: AreaLevelEnum.DISTRICT
            }
          ],
          get info() {
            return translate('modules.ne2fca58f2c')
          }
        },
        {
          type: 'input',
          field: 'placeholder',
          get title() {
            return translate('modules.necd79f3880')
          },
          value: translate('modules.n72f02df6d9')
        },
        {
          type: 'switch',
          field: 'clearable',
          get title() {
            return translate('modules.n062b4871ac')
          },
          value: true
        },
        {
          type: 'switch',
          field: 'showAllLevels',
          get title() {
            return translate('modules.n3129c1fdf6')
          },
          value: true,
          get info() {
            return translate('modules.n2d3c0935bb')
          }
        },
        {
          type: 'input',
          field: 'separator',
          get title() {
            return translate('modules.n0415f31a96')
          },
          value: '/',
          get info() {
            return translate('modules.na1fe7bc285')
          }
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
