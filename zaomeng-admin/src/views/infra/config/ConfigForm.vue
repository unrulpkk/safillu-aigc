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
      <el-form-item :label="t('native.n7513dfb2a5')" prop="category">
        <el-input v-model="formData.category" :placeholder="t('native.n426320cdbb')" />
      </el-form-item>
      <el-form-item :label="t('native.n45a311c441')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n74b94d2bd0')" />
      </el-form-item>
      <el-form-item :label="t('native.n59c57f4261')" prop="key">
        <el-input v-model="formData.key" :placeholder="t('native.nfa0f5f8bf4')" />
      </el-form-item>
      <el-form-item :label="t('native.nfb065303a5')" prop="value">
        <el-input v-model="formData.value" :placeholder="t('native.nf5bbe708d7')" />
      </el-form-item>
      <el-form-item :label="t('native.ndd14c17134')" prop="visible">
        <el-radio-group v-model="formData.visible">
          <el-radio
            v-for="dict in getBoolDictOptions(DICT_TYPE.INFRA_BOOLEAN_STRING)"
            :key="String(dict.value)"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
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

import { DICT_TYPE, getBoolDictOptions } from '@/utils/dict'
import * as ConfigApi from '@/api/infra/config'

defineOptions({ name: 'InfraConfigForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  category: '',
  name: '',
  key: '',
  value: '',
  visible: true,
  remark: ''
})
const formRules = reactive({
  category: [
    {
      required: true,
      get message() {
        return t('native.n8c545e5ba3')
      },
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      get message() {
        return t('native.na1cd20b68f')
      },
      trigger: 'blur'
    }
  ],
  key: [
    {
      required: true,
      get message() {
        return t('native.n7ac86d440f')
      },
      trigger: 'blur'
    }
  ],
  value: [
    {
      required: true,
      get message() {
        return t('native.nda10674ce2')
      },
      trigger: 'blur'
    }
  ],
  visible: [
    {
      required: true,
      get message() {
        return t('native.ncac166d4d8')
      },
      trigger: 'blur'
    }
  ]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = 'action.' + type
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await ConfigApi.getConfig(id)
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
    const data = formData.value as ConfigApi.ConfigVO
    if (formType.value === 'create') {
      await ConfigApi.createConfig(data)
      message.success(t('common.createSuccess'))
    } else {
      await ConfigApi.updateConfig(data)
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
    category: '',
    name: '',
    key: '',
    value: '',
    visible: true,
    remark: ''
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
