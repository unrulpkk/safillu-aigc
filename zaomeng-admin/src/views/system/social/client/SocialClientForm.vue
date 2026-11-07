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
      <el-form-item :label="t('native.nc131777b2f')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n37b3ad1ff3')" />
      </el-form-item>
      <el-form-item :label="t('native.n0d7ef7a81f')" prop="socialType">
        <el-radio-group v-model="formData.socialType">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_SOCIAL_TYPE)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('native.naec6d9b3d2')" prop="userType">
        <el-radio-group v-model="formData.userType">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.USER_TYPE)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('native.n0f8836ccbd')" prop="clientId">
        <el-input v-model="formData.clientId" :placeholder="t('native.n0ab84dca23')" />
      </el-form-item>
      <el-form-item :label="t('native.n7183484f9a')" prop="clientSecret">
        <el-input v-model="formData.clientSecret" :placeholder="t('native.n466db8e164')" />
      </el-form-item>
      <el-form-item label="agentId" prop="agentId" v-if="formData!.socialType === 30">
        <el-input v-model="formData.agentId" :placeholder="t('native.n2537ab4a52')" />
      </el-form-item>
      <el-form-item label="publicKey" prop="publicKey" v-if="formData!.socialType === 40">
        <el-input v-model="formData.publicKey" :placeholder="t('native.n381b5237a7')" />
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
import * as SocialClientApi from '@/api/system/social/client'

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: undefined,
  socialType: undefined,
  userType: undefined,
  clientId: undefined,
  clientSecret: undefined,
  publicKey: undefined,
  agentId: undefined,
  status: 0
})
const formRules = reactive({
  name: [
    {
      required: true,
      get message() {
        return t('native.n90f2b1c826')
      },
      trigger: 'blur'
    }
  ],
  socialType: [
    {
      required: true,
      get message() {
        return t('native.nba2772a090')
      },
      trigger: 'blur'
    }
  ],
  userType: [
    {
      required: true,
      get message() {
        return t('native.n87bea12553')
      },
      trigger: 'blur'
    }
  ],
  clientId: [
    {
      required: true,
      get message() {
        return t('native.n45c092359a')
      },
      trigger: 'blur'
    }
  ],
  clientSecret: [
    {
      required: true,
      get message() {
        return t('native.n2e69af2a14')
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
      formData.value = await SocialClientApi.getSocialClient(id)
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
    const data = formData.value as unknown as SocialClientApi.SocialClientVO
    if (formType.value === 'create') {
      await SocialClientApi.createSocialClient(data)
      message.success(t('common.createSuccess'))
    } else {
      await SocialClientApi.updateSocialClient(data)
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
    name: undefined,
    socialType: undefined,
    userType: undefined,
    clientId: undefined,
    clientSecret: undefined,
    publicKey: undefined,
    agentId: undefined,
    status: 0
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
