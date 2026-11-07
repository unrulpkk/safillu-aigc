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
      <el-form-item :label="t('native.n8fe0a72e91')" prop="channelId">
        <el-select v-model="formData.channelId" :placeholder="t('native.n4ff60f51fc')">
          <el-option
            v-for="channel in channelList"
            :key="channel.id"
            :label="
              channel.signature +
              `【 ${getDictLabel(DICT_TYPE.SYSTEM_SMS_CHANNEL_CODE, channel.code)}】`
            "
            :value="channel.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.n117df90e5e')" prop="type">
        <el-select v-model="formData.type" :placeholder="t('native.ne072faedc9')">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_SMS_TEMPLATE_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.n4fa9ed5a84')" prop="code">
        <el-input v-model="formData.code" :placeholder="t('native.n9c5d7004f8')" />
      </el-form-item>
      <el-form-item :label="t('native.nf7816a356d')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n1f62845bc0')" />
      </el-form-item>
      <el-form-item :label="t('native.n8f42018d05')" prop="content">
        <el-input
          v-model="formData.content"
          :placeholder="t('native.n270d847c31')"
          type="textarea"
        />
      </el-form-item>
      <el-form-item :label="t('native.n490e3e01a3')" prop="status">
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
      <el-form-item :label="t('native.n7f8d6c0663')" prop="apiTemplateId">
        <el-input v-model="formData.apiTemplateId" :placeholder="t('native.n52f5c44e5e')" />
      </el-form-item>
      <el-form-item :label="t('native.ndaede98817')" prop="remark">
        <el-input v-model="formData.remark" :placeholder="t('native.n1004c72456')" />
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

import { DICT_TYPE, getDictLabel, getIntDictOptions } from '@/utils/dict'
import * as SmsTemplateApi from '@/api/system/sms/smsTemplate'
import * as SmsChannelApi from '@/api/system/sms/smsChannel'
import { CommonStatusEnum } from '@/utils/constants'

defineOptions({ name: 'SystemSmsTemplateForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型
const formData = ref<SmsTemplateApi.SmsTemplateVO>({
  id: undefined,
  type: undefined,
  status: CommonStatusEnum.ENABLE,
  code: '',
  name: '',
  content: '',
  remark: '',
  apiTemplateId: '',
  channelId: undefined
})
const formRules = reactive({
  type: [
    {
      required: true,
      get message() {
        return t('native.n622cfca29b')
      },
      trigger: 'change'
    }
  ],
  status: [
    {
      required: true,
      get message() {
        return t('native.nb96b333b98')
      },
      trigger: 'blur'
    }
  ],
  code: [
    {
      required: true,
      get message() {
        return t('native.nd8267a2343')
      },
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      get message() {
        return t('native.nda00e8dcc1')
      },
      trigger: 'blur'
    }
  ],
  content: [
    {
      required: true,
      get message() {
        return t('native.n6fce765304')
      },
      trigger: 'blur'
    }
  ],
  apiTemplateId: [
    {
      required: true,
      get message() {
        return t('native.ned505c86f8')
      },
      trigger: 'blur'
    }
  ],
  channelId: [
    {
      required: true,
      get message() {
        return t('native.n6e20c24730')
      },
      trigger: 'change'
    }
  ]
})
const formRef = ref() // 表单 Ref
const channelList = ref<SmsChannelApi.SmsChannelVO[]>([]) // 短信渠道列表

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = 'action.' + type
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await SmsTemplateApi.getSmsTemplate(id)
    } finally {
      formLoading.value = false
    }
  }
  // 加载渠道列表
  channelList.value = await SmsChannelApi.getSimpleSmsChannelList()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    const data = formData.value as SmsTemplateApi.SmsTemplateVO
    if (formType.value === 'create') {
      await SmsTemplateApi.createSmsTemplate(data)
      message.success(t('common.createSuccess'))
    } else {
      await SmsTemplateApi.updateSmsTemplate(data)
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
    type: undefined,
    status: CommonStatusEnum.ENABLE,
    code: '',
    name: '',
    content: '',
    remark: '',
    apiTemplateId: '',
    channelId: undefined
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
