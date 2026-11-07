import { useI18n } from '@/hooks/web/useI18n'
const translate = (key: string) => useI18n().t(key)
const selectRule = [
  {
    type: 'select',
    field: 'selectType',
    get title() {
      return translate('modules.n4474162261')
    },
    value: 'select',
    options: [
      {
        get label() {
          return translate('native.n7795013e81')
        },
        value: 'select'
      },
      {
        get label() {
          return translate('native.n4f941dece5')
        },
        value: 'radio'
      },
      {
        get label() {
          return translate('modules.ne943c40070')
        },
        value: 'checkbox'
      }
    ],
    // 参考 https://www.form-create.com/v3/guide/control 组件联动，单选框和多选框不需要多选属性
    control: [
      {
        value: 'select',
        condition: '==',
        method: 'hidden',
        rule: [
          'multiple',
          'clearable',
          'collapseTags',
          'multipleLimit',
          'allowCreate',
          'filterable',
          'noMatchText',
          'remote',
          'remoteMethod',
          'reserveKeyword',
          'defaultFirstOption',
          'automaticDropdown'
        ]
      }
    ]
  },
  {
    type: 'switch',
    field: 'filterable',
    get title() {
      return translate('modules.n7c773fcdf2')
    }
  },
  {
    type: 'switch',
    field: 'multiple',
    get title() {
      return translate('modules.nb6294256bd')
    }
  },
  {
    type: 'switch',
    field: 'disabled',
    get title() {
      return translate('modules.n8da75514d7')
    }
  },
  {
    type: 'switch',
    field: 'clearable',
    get title() {
      return translate('modules.n52771e7c66')
    }
  },
  {
    type: 'switch',
    field: 'collapseTags',
    get title() {
      return translate('modules.n79c09b9379')
    }
  },
  {
    type: 'inputNumber',
    field: 'multipleLimit',
    get title() {
      return translate('modules.n4b9d6110f0')
    },
    props: { min: 0 }
  },
  {
    type: 'input',
    field: 'autocomplete',
    get title() {
      return translate('modules.nca1ccaacc8')
    }
  },
  {
    type: 'input',
    field: 'placeholder',
    get title() {
      return translate('modules.necd79f3880')
    }
  },
  {
    type: 'switch',
    field: 'allowCreate',
    get title() {
      return translate('modules.na5b042a706')
    }
  },
  {
    type: 'input',
    field: 'noMatchText',
    get title() {
      return translate('modules.n9d666f6505')
    }
  },
  {
    type: 'input',
    field: 'noDataText',
    get title() {
      return translate('modules.n9db9f3bfb5')
    }
  },
  {
    type: 'switch',
    field: 'reserveKeyword',
    get title() {
      return translate('modules.n9f5acec6ca')
    }
  },
  {
    type: 'switch',
    field: 'defaultFirstOption',
    get title() {
      return translate('modules.n284282b150')
    }
  },
  {
    type: 'switch',
    field: 'popperAppendToBody',
    get title() {
      return translate('modules.n9facde7ccb')
    },
    value: true
  },
  {
    type: 'switch',
    field: 'automaticDropdown',
    get title() {
      return translate('modules.n24cf6c04e2')
    }
  }
]

const apiSelectRule = [
  {
    type: 'input',
    field: 'url',
    get title() {
      return translate('modules.na9fc70ce92')
    },
    props: {
      placeholder: '/system/user/simple-list'
    }
  },
  {
    type: 'select',
    field: 'method',
    get title() {
      return translate('modules.ndfadca6f47')
    },
    value: 'GET',
    options: [
      { label: 'GET', value: 'GET' },
      { label: 'POST', value: 'POST' }
    ],
    control: [
      {
        value: 'GET',
        condition: '!=',
        method: 'hidden',
        rule: [
          {
            type: 'input',
            field: 'data',
            get title() {
              return translate('modules.n99dee0e43e')
            },
            props: {
              autosize: true,
              type: 'textarea',
              placeholder: '{"type": 1}'
            }
          }
        ]
      }
    ]
  },
  {
    type: 'input',
    field: 'labelField',
    get title() {
      return translate('modules.nb10129ceb0')
    },
    get info() {
      return translate('modules.n28bce176fe')
    },
    props: {
      placeholder: 'nickname'
    }
  },
  {
    type: 'input',
    field: 'valueField',
    get title() {
      return translate('modules.n458b913c89')
    },
    get info() {
      return translate('modules.n28bce176fe')
    },
    props: {
      placeholder: 'id'
    }
  },
  {
    type: 'input',
    field: 'parseFunc',
    get title() {
      return translate('modules.n3b3f85291f')
    },
    info: `data 为接口返回值,需要写一个匿名函数解析返回值为选择器 options 列表
    (data: any)=>{ label: string; value: any }[]`,
    props: {
      autosize: true,
      rows: { minRows: 2, maxRows: 6 },
      type: 'textarea',
      placeholder: `
        function (data) {
            console.log(data)
            return data.list.map(item=> ({label: item.nickname,value: item.id}))
        }`
    }
  },
  {
    type: 'switch',
    field: 'remote',
    get info() {
      return translate('modules.n7c773fcdf2')
    },
    get title() {
      return translate('modules.n05d082a6a6')
    }
  },
  {
    type: 'input',
    field: 'remoteField',
    get title() {
      return translate('native.n9541ee3d4b')
    },
    get info() {
      return translate('modules.n5f99579b4b')
    }
  }
]

export { selectRule, apiSelectRule }
