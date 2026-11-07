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
      <el-form-item :label="t('native.n6773310492')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.ne50df896fc')" />
      </el-form-item>
      <el-form-item :label="t('native.n89e35e662a')" prop="score">
        <el-input v-model="formData.score" :placeholder="t('native.n875d71d1d3')" />
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
<script setup lang="ts">
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

import { Demo03StudentApi, Demo03Course } from '@/api/infra/demo/demo03/erp'

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<Demo03Course>({
  id: undefined,
  studentId: undefined,
  name: undefined,
  score: undefined
})
const formRules = reactive({
  studentId: [
    {
      required: true,
      get message() {
        return t('native.na77dc02454')
      },
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      get message() {
        return t('native.n6aa3b1bd38')
      },
      trigger: 'blur'
    }
  ],
  score: [
    {
      required: true,
      get message() {
        return t('native.n5526ba812a')
      },
      trigger: 'blur'
    }
  ]
})
const formRef = ref() // 表单 Ref

/** 打开弹窗 */
const open = async (type: string, id?: number, studentId?: number) => {
  dialogVisible.value = true
  dialogTitle.value = 'action.' + type
  formType.value = type
  resetForm()
  formData.value.studentId = studentId
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await Demo03StudentApi.getDemo03Course(id)
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
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = formData.value
    if (formType.value === 'create') {
      await Demo03StudentApi.createDemo03Course(data)
      message.success(t('common.createSuccess'))
    } else {
      await Demo03StudentApi.updateDemo03Course(data)
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
    studentId: undefined,
    name: undefined,
    score: undefined
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
