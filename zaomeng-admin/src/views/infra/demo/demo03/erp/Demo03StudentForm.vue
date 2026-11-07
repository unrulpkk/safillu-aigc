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
      <el-form-item :label="t('native.na752a7d8c1')" prop="sex">
        <el-radio-group v-model="formData.sex">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_USER_SEX)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('native.n1c54caca8e')" prop="birthday">
        <el-date-picker
          v-model="formData.birthday"
          type="date"
          value-format="x"
          :placeholder="t('native.ne2587b63a6')"
        />
      </el-form-item>
      <el-form-item :label="t('native.n8131eafd02')" prop="description">
        <Editor v-model="formData.description" height="150px" />
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

import { getIntDictOptions, DICT_TYPE } from '@/utils/dict'
import { Demo03StudentApi, Demo03Student } from '@/api/infra/demo/demo03/erp'

/** 学生 表单 */
defineOptions({ name: 'Demo03StudentForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref<Demo03Student>({
  id: undefined,
  name: undefined,
  sex: undefined,
  birthday: undefined,
  description: undefined
})
const formRules = reactive({
  name: [
    {
      required: true,
      get message() {
        return t('native.n6aa3b1bd38')
      },
      trigger: 'blur'
    }
  ],
  sex: [
    {
      required: true,
      get message() {
        return t('native.ne5f77e6cea')
      },
      trigger: 'blur'
    }
  ],
  birthday: [
    {
      required: true,
      get message() {
        return t('native.n28df24a175')
      },
      trigger: 'blur'
    }
  ],
  description: [
    {
      required: true,
      get message() {
        return t('native.n6761526a06')
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
      formData.value = await Demo03StudentApi.getDemo03Student(id)
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
      await Demo03StudentApi.createDemo03Student(data)
      message.success(t('common.createSuccess'))
    } else {
      await Demo03StudentApi.updateDemo03Student(data)
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
    sex: undefined,
    birthday: undefined,
    description: undefined
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
