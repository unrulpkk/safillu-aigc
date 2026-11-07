<template>
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
    <el-form-item :label="t('native.n61a642e7aa')" prop="teacher">
      <el-input v-model="formData.teacher" :placeholder="t('native.n6ea1d90507')" />
    </el-form-item>
  </el-form>
</template>
<script setup lang="ts">
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

const { t } = useI18n()

import { Demo03StudentApi } from '@/api/infra/demo/demo03/inner'

const props = defineProps<{
  studentId?: number // 学生编号（主表的关联字段）
}>()
const formLoading = ref(false) // 表单的加载中
const formData = ref<any>({})
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
  teacher: [
    {
      required: true,
      get message() {
        return t('native.na28dcd1e22')
      },
      trigger: 'blur'
    }
  ]
})
const formRef = ref() // 表单 Ref

/** 监听主表的关联字段的变化，加载对应的子表数据 */
watch(
  () => props.studentId,
  async (val) => {
    // 1. 重置表单
    formData.value = {
      id: undefined,
      studentId: undefined,
      name: undefined,
      teacher: undefined
    }
    // 2. val 非空，则加载数据
    if (!val) {
      return
    }
    try {
      formLoading.value = true
      const data = await Demo03StudentApi.getDemo03GradeByStudentId(val)
      if (!data) {
        return
      }
      formData.value = data
    } finally {
      formLoading.value = false
    }
  },
  { immediate: true }
)

/** 表单校验 */
const validate = () => {
  return formRef.value.validate()
}

/** 表单值 */
const getData = () => {
  return formData.value
}

defineExpose({ validate, getData })

useLocalizedFormValidation(formRef)
</script>
