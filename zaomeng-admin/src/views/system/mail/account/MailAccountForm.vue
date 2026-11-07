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
      <el-form-item :label="t('native.n73075237fd')" prop="mail">
        <el-input v-model="formData.mail" :placeholder="t('native.n59c9ed95eb')" />
      </el-form-item>
      <el-form-item :label="t('native.n1a3f0617d6')" prop="username">
        <el-input v-model="formData.username" :placeholder="t('native.nc723b1fab5')" />
      </el-form-item>
      <el-form-item :label="t('native.na621ab606d')" prop="password">
        <el-input
          v-model="formData.password"
          :placeholder="t('native.n728a7b601c')"
          type="password"
          show-password
        />
      </el-form-item>
      <el-form-item :label="t('native.n09c2a50301')" prop="host">
        <el-input v-model="formData.host" :placeholder="t('native.n33199dbe75')" />
      </el-form-item>
      <el-form-item :label="t('native.n60004ee2e4')" prop="port">
        <el-input-number
          v-model="formData.port"
          :placeholder="t('native.n4d7241d66c')"
          :min="1"
          :max="65535"
        />
      </el-form-item>
      <el-form-item :label="t('native.n7db22e1b51')">
        <el-radio-group v-model="formData.sslEnable">
          <el-radio
            v-for="dict in getBoolDictOptions(DICT_TYPE.INFRA_BOOLEAN_STRING)"
            :key="String(dict.value)"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('native.nb52708ff85')">
        <el-radio-group v-model="formData.starttlsEnable">
          <el-radio
            v-for="dict in getBoolDictOptions(DICT_TYPE.INFRA_BOOLEAN_STRING)"
            :key="String(dict.value)"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
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
import * as MailAccountApi from '@/api/system/mail/account'

defineOptions({ name: 'SystemMailAccountForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<MailAccountApi.MailAccountVO>({
  id: undefined,
  mail: '',
  username: '',
  password: '',
  host: '',
  port: 465,
  sslEnable: true,
  starttlsEnable: false
})
const formRules = reactive({
  mail: [
    {
      required: true,
      get message() {
        return t('native.n28e55875b3')
      },
      trigger: 'blur'
    },
    {
      type: 'email',
      get message() {
        return t('native.n32bc2f8d93')
      },
      trigger: ['blur', 'change']
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
  ],
  host: [
    {
      required: true,
      get message() {
        return t('native.nbffc2fe6dd')
      },
      trigger: 'blur'
    }
  ],
  port: [
    {
      required: true,
      get message() {
        return t('native.n09c69797a5')
      },
      trigger: 'blur'
    }
  ],
  sslEnable: [
    {
      required: true,
      get message() {
        return t('native.n3830d6f7ae')
      },
      trigger: 'blur'
    }
  ],
  starttlsEnable: [
    {
      required: true,
      get message() {
        return t('native.ne125e8d99d')
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
      formData.value = await MailAccountApi.getMailAccount(id)
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
      await MailAccountApi.createMailAccount(data)
      message.success(t('common.createSuccess'))
    } else {
      await MailAccountApi.updateMailAccount(data)
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
    mail: '',
    username: '',
    password: '',
    host: '',
    port: 465,
    sslEnable: true,
    starttlsEnable: false
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
