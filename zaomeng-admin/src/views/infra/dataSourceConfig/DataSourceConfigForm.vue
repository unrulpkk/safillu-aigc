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
      <el-form-item :label="t('native.n59ed60fd80')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n74b94d2bd0')" />
      </el-form-item>
      <el-form-item :label="t('native.n9d584c7824')" prop="url">
        <el-input v-model="formData.url" :placeholder="t('native.ncf8f880c0d')" />
      </el-form-item>
      <el-form-item :label="t('native.n1a3f0617d6')" prop="username">
        <el-input v-model="formData.username" :placeholder="t('native.nc723b1fab5')" />
      </el-form-item>
      <el-form-item :label="t('native.na621ab606d')" prop="password">
        <el-input v-model="formData.password" :placeholder="t('native.n728a7b601c')" />
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

import * as DataSourceConfigApi from '@/api/infra/dataSourceConfig'

defineOptions({ name: 'InfraDataSourceConfigForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<DataSourceConfigApi.DataSourceConfigVO>({
  id: undefined,
  name: '',
  url: '',
  username: '',
  password: ''
})
const formRules = reactive({
  name: [
    {
      required: true,
      get message() {
        return t('native.na4977dd4ef')
      },
      trigger: 'blur'
    }
  ],
  url: [
    {
      required: true,
      get message() {
        return t('native.n78fc0c3d21')
      },
      trigger: 'blur'
    }
  ],
  username: [
    {
      required: true,
      get message() {
        return t('native.n390ccdec9f')
      },
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      get message() {
        return t('native.n3aa7a9e035')
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
      formData.value = await DataSourceConfigApi.getDataSourceConfig(id)
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
    const data = formData.value as DataSourceConfigApi.DataSourceConfigVO
    if (formType.value === 'create') {
      await DataSourceConfigApi.createDataSourceConfig(data)
      message.success(t('common.createSuccess'))
    } else {
      await DataSourceConfigApi.updateDataSourceConfig(data)
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
    name: '',
    url: '',
    username: '',
    password: ''
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
