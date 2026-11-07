<template>
  <Dialog v-model="dialogVisible" :title="t('native.n6aa8f49cc9')">
    <el-form
      :validate-on-rule-change="false"
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item :label="t('native.n8f42018d05')" prop="content">
        <el-input
          v-model="formData.content"
          :placeholder="t('native.n270d847c31')"
          readonly
          type="textarea"
        />
      </el-form-item>
      <el-form-item :label="t('native.n6f52cc94db')" prop="mobile">
        <el-input v-model="formData.mobile" :placeholder="t('native.n2f74a3ffc8')" />
      </el-form-item>
      <el-form-item
        v-for="param in formData.params"
        :key="param"
        :label="t('native.na80a5b61e0', { p0: param })"
        :prop="'templateParams.' + param"
      >
        <el-input
          v-model="formData.templateParams[param]"
          :placeholder="t('native.n5fd778aba5', { p0: param })"
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

const { t } = useI18n()

import * as SmsTemplateApi from '@/api/system/sms/smsTemplate'

defineOptions({ name: 'SystemSmsTemplateSendForm' })

const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用

// 发送短信表单相关
const formData = ref({
  content: '',
  params: {},
  mobile: '',
  templateCode: '',
  templateParams: new Map()
})
const formRules = reactive({
  mobile: [
    {
      required: true,
      get message() {
        return t('native.n72d2fe9674')
      },
      trigger: 'blur'
    }
  ],
  templateCode: [
    {
      required: true,
      get message() {
        return t('native.n0bc174ad80')
      },
      trigger: 'blur'
    }
  ],
  templateParams: {}
})
const formRef = ref() // 表单 Ref

const open = async (id: number) => {
  dialogVisible.value = true
  resetForm()
  // 设置数据
  formLoading.value = true
  try {
    const data = await SmsTemplateApi.getSmsTemplate(id)
    // 设置动态表单
    formData.value.content = data.content
    formData.value.params = data.params
    formData.value.templateCode = data.code
    formData.value.templateParams = data.params.reduce((obj, item) => {
      obj[item] = '' // 给每个动态属性赋值，避免无法读取
      return obj
    }, {})
    formRules.templateParams = data.params.reduce((obj, item) => {
      obj[item] = {
        required: true,
        message: t('native.ne331a94277', { p0: item }),
        trigger: 'blur'
      }
      return obj
    }, {})
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as SmsTemplateApi.SendSmsReqVO
    const logId = await SmsTemplateApi.sendSms(data)
    if (logId) {
      message.success(t('native.nd17fdb25a9', { p0: logId }))
    }
    dialogVisible.value = false
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    content: '',
    params: {},
    mobile: '',
    templateCode: '',
    templateParams: new Map()
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
