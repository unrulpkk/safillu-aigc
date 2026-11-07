<template>
  <Dialog :title="t(dialogTitle)" v-model="dialogVisible">
    <el-form
      :validate-on-rule-change="false"
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="auto"
      v-loading="formLoading"
    >
      <el-form-item :label="t('native.n9eb72dfe30')" prop="code">
        <el-input v-model="formData.code" :placeholder="t('native.n616563d370')" />
      </el-form-item>
      <el-form-item :label="t('native.nf7816a356d')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.nfc316525e7')" />
      </el-form-item>
      <el-form-item :label="t('native.n544ab76df1')" prop="nickname">
        <el-input v-model="formData.nickname" :placeholder="t('native.nb5ee554274')" />
      </el-form-item>
      <el-form-item :label="t('native.n8f42018d05')" prop="content">
        <el-input
          type="textarea"
          v-model="formData.content"
          :placeholder="t('native.n270d847c31')"
        />
      </el-form-item>
      <el-form-item :label="t('native.nba40014ff4')" prop="type">
        <el-select v-model="formData.type" :placeholder="t('native.n8d928c5beb')">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_NOTIFY_TEMPLATE_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
      <el-form-item :label="t('native.ndaede98817')" prop="remark">
        <el-input v-model="formData.remark" :placeholder="t('native.n1004c72456')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">
        {{ t('native.n8d6e0c32e5') }}
      </el-button>
      <el-button @click="dialogVisible = false"> {{ t('native.n1f4d617f83') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

const { t } = useI18n()

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as NotifyTemplateApi from '@/api/system/notify/template'
import { CommonStatusEnum } from '@/utils/constants'
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型
const formData = ref<NotifyTemplateApi.NotifyTemplateVO>({
  id: undefined,
  name: '',
  nickname: '',
  code: '',
  content: '',
  type: undefined,
  params: '',
  status: CommonStatusEnum.ENABLE,
  remark: ''
})
const formRules = reactive({
  type: [
    {
      required: true,
      get message() {
        return t('native.na03918348c')
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
  nickname: [
    {
      required: true,
      get message() {
        return t('native.ned35f555eb')
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
      formData.value = await NotifyTemplateApi.getNotifyTemplate(id)
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
  formLoading.value = true
  try {
    const data = formData.value as unknown as NotifyTemplateApi.NotifyTemplateVO
    if (formType.value === 'create') {
      await NotifyTemplateApi.createNotifyTemplate(data)
      message.success(t('native.nde3a44a29b'))
    } else {
      await NotifyTemplateApi.updateNotifyTemplate(data)
      message.success(t('native.n3602152b0f'))
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
    nickname: '',
    code: '',
    content: '',
    type: undefined,
    params: '',
    status: CommonStatusEnum.ENABLE,
    remark: ''
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
