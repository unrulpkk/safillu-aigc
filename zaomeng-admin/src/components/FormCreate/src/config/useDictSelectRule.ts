import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
import { generateUUID } from '@/utils'
import * as DictDataApi from '@/api/system/dict/dict.type'
import { localeProps, makeRequiredRule } from '@/components/FormCreate/src/utils'
import { selectRule } from '@/components/FormCreate/src/config/selectRule'
import { cloneDeep } from 'lodash-es'

/**
 * 字典选择器规则，如果规则使用到动态数据则需要单独配置不能使用 useSelectRule
 */
export const useDictSelectRule = () => {
  const label = translate('modules.nb76bd01ad4')
  const name = 'DictSelect'
  const rules = cloneDeep(selectRule)
  const dictOptions = ref<{ label: string; value: string }[]>([]) // 字典类型下拉数据
  onMounted(async () => {
    const data = await DictDataApi.getSimpleDictTypeList()
    if (!data || data.length === 0) {
      return
    }
    dictOptions.value =
      data?.map((item: DictDataApi.DictTypeVO) => ({
        label: item.name,
        value: item.type
      })) ?? []
  })
  return {
    icon: 'icon-doc-text',
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
          field: 'dictType',
          get title() {
            return translate('native.n4adeb7d216')
          },
          value: '',
          options: dictOptions.value
        },
        {
          type: 'select',
          field: 'valueType',
          get title() {
            return translate('modules.n8b4c8daf80')
          },
          value: 'str',
          options: [
            {
              get label() {
                return translate('modules.nc16ec9274e')
              },
              value: 'int'
            },
            {
              get label() {
                return translate('modules.n6aaf0b1804')
              },
              value: 'str'
            },
            {
              get label() {
                return translate('modules.nc08142c140')
              },
              value: 'bool'
            }
          ]
        },
        ...rules
      ])
    }
  }
}
