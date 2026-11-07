<template>
  <Dialog v-model="dialogVisible" :title="t(dialogTitle)">
    <el-form
      :validate-on-rule-change="false"
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item :label="t('native.n4adeb7d216')" prop="type">
        <el-input
          v-model="formData.dictType"
          :disabled="typeof formData.id !== 'undefined'"
          :placeholder="t('native.n74b94d2bd0')"
        />
      </el-form-item>
      <el-form-item :label="t('native.nfed396786a')" prop="label">
        <el-input v-model="formData.label" :placeholder="t('native.n698f91e949')" />
      </el-form-item>
      <el-form-item :label="t('native.nc82a734c6b')" prop="value">
        <el-input v-model="formData.value" :placeholder="t('native.nbafc45f223')" />
      </el-form-item>
      <el-form-item :label="t('native.n7d8c6e7807')" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item :label="t('native.n6320b4a872')" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('native.n8ee2f6b80f')" prop="colorType">
        <el-select v-model="formData.colorType">
          <el-option
            v-for="item in colorTypeOptions"
            :key="item.value"
            :label="item.label + '(' + item.value + ')'"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="CSS Class" prop="cssClass">
        <el-input v-model="formData.cssClass" :placeholder="t('native.n9e9545e0ac')" />
      </el-form-item>
      <el-form-item :label="t('native.ndaede98817')" prop="remark">
        <el-input
          v-model="formData.remark"
          :placeholder="t('native.n606ae522e6')"
          type="textarea"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">
        {{ t('native.n8d6e0c32e5') }}
      </el-button>
      <el-button @click="dialogVisible = false"> {{ t('native.n1f4d617f83') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as DictDataApi from '@/api/system/dict/dict.data'
import { CommonStatusEnum } from '@/utils/constants'

defineOptions({ name: 'SystemDictDataForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<DictDataApi.DictDataVO>({
  id: undefined,
  sort: undefined,
  label: '',
  value: '',
  dictType: '',
  status: CommonStatusEnum.ENABLE,
  colorType: '',
  cssClass: '',
  remark: ''
})
const formRules = reactive({
  label: [
    {
      required: true,
      get message() {
        return t('native.n98c425ce0b')
      },
      trigger: 'blur'
    }
  ],
  value: [
    {
      required: true,
      get message() {
        return t('native.n285da0e9bf')
      },
      trigger: 'blur'
    }
  ],
  sort: [
    {
      required: true,
      get message() {
        return t('native.nbe2732b87f')
      },
      trigger: 'blur'
    }
  ],
  status: [
    {
      required: true,
      get message() {
        return t('native.ncd0a03ebb7')
      },
      trigger: 'change'
    }
  ]
})
const formRef = ref() // 表单 Ref

// 数据标签回显样式
const colorTypeOptions = readonly([
  {
    value: 'default',
    get label() {
      return t('native.n844b8cc8df')
    }
  },
  {
    value: 'primary',
    get label() {
      return t('native.n5ccd91f418')
    }
  },
  {
    value: 'success',
    get label() {
      return t('native.n053461ce86')
    }
  },
  {
    value: 'info',
    get label() {
      return t('native.ne7028601e7')
    }
  },
  {
    value: 'warning',
    get label() {
      return t('native.na8b7a44804')
    }
  },
  {
    value: 'danger',
    get label() {
      return t('native.nbf2aee38a2')
    }
  }
])

/** 打开弹窗 */
const open = async (type: string, id?: number, dictType?: string) => {
  dialogVisible.value = true
  dialogTitle.value = 'action.' + type
  formType.value = type
  resetForm()
  if (dictType) {
    formData.value.dictType = dictType
  }
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await DictDataApi.getDictData(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await DictDataApi.createDictData(data)
      message.success(t('common.createSuccess'))
    } else {
      await DictDataApi.updateDictData(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    id: undefined,
    sort: undefined,
    label: '',
    value: '',
    dictType: '',
    status: CommonStatusEnum.ENABLE,
    colorType: '',
    cssClass: '',
    remark: ''
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
