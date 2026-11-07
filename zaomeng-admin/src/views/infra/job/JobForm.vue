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
      <el-form-item :label="t('native.n2479560deb')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n1f01b74c34')" />
      </el-form-item>
      <el-form-item :label="t('native.nb401f25585')" prop="handlerName">
        <el-input
          :readonly="formData.id !== undefined"
          v-model="formData.handlerName"
          :placeholder="t('native.n44511eb565')"
        />
      </el-form-item>
      <el-form-item :label="t('native.n1450e7ee26')" prop="handlerParam">
        <el-input v-model="formData.handlerParam" :placeholder="t('native.n3bbee746c3')" />
      </el-form-item>
      <el-form-item :label="t('native.nf6b382a662')" prop="cronExpression">
        <crontab v-model="formData.cronExpression" />
      </el-form-item>
      <el-form-item :label="t('native.n9f28a8874c')" prop="retryCount">
        <el-input v-model="formData.retryCount" :placeholder="t('native.n68bfd7336b')" />
      </el-form-item>
      <el-form-item :label="t('native.n3e861112e5')" prop="retryInterval">
        <el-input v-model="formData.retryInterval" :placeholder="t('native.n16665e0162')" />
      </el-form-item>
      <el-form-item :label="t('native.n11e8b3b52a')" prop="monitorTimeout">
        <el-input v-model="formData.monitorTimeout" :placeholder="t('native.n8430a75c1f')" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitForm" :loading="formLoading">
        {{ t('native.n8d6e0c32e5') }}
      </el-button>
      <el-button @click="dialogVisible = false"> {{ t('native.n1f4d617f83') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

import * as JobApi from '@/api/infra/job'

defineOptions({ name: 'JobForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: '',
  handlerName: '',
  handlerParam: '',
  cronExpression: '',
  retryCount: undefined,
  retryInterval: undefined,
  monitorTimeout: undefined
})
const formRules = reactive({
  name: [
    {
      required: true,
      get message() {
        return t('native.nd9ee9e5a9b')
      },
      trigger: 'blur'
    }
  ],
  handlerName: [
    {
      required: true,
      get message() {
        return t('native.n0eb4ad4cbc')
      },
      trigger: 'blur'
    }
  ],
  cronExpression: [
    {
      required: true,
      get message() {
        return t('native.n7ce372780e')
      },
      trigger: 'blur'
    }
  ],
  retryCount: [
    {
      required: true,
      get message() {
        return t('native.n5e0923a497')
      },
      trigger: 'blur'
    }
  ],
  retryInterval: [
    {
      required: true,
      get message() {
        return t('native.ne597696b95')
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
      formData.value = await JobApi.getJob(id)
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交按钮 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value as unknown as JobApi.JobVO
    if (formType.value === 'create') {
      await JobApi.createJob(data)
      message.success(t('common.createSuccess'))
    } else {
      await JobApi.updateJob(data)
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
    handlerName: '',
    handlerParam: '',
    cronExpression: '',
    retryCount: undefined,
    retryInterval: undefined,
    monitorTimeout: undefined
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
