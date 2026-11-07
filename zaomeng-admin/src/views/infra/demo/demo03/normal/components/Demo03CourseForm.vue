<template>
  <el-form
    :validate-on-rule-change="false"
    ref="formRef"
    :model="formData"
    :rules="formRules"
    v-loading="formLoading"
    label-width="auto"
    :inline-message="true"
  >
    <el-table :data="formData" class="-mt-10px">
      <el-table-column :label="t('native.n1fc12f2e41')" type="index" width="100" />
      <el-table-column :label="t('native.n6773310492')" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.name`" :rules="formRules.name" class="mb-0px!">
            <el-input v-model="row.name" :placeholder="t('native.ne50df896fc')" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column :label="t('native.n89e35e662a')" min-width="150">
        <template #default="{ row, $index }">
          <el-form-item :prop="`${$index}.score`" :rules="formRules.score" class="mb-0px!">
            <el-input v-model="row.score" :placeholder="t('native.n875d71d1d3')" />
          </el-form-item>
        </template>
      </el-table-column>
      <el-table-column align="center" fixed="right" :label="t('native.ned31fbb483')" width="60">
        <template #default="{ $index }">
          <el-button @click="handleDelete($index)" link>—</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-form>
  <el-row justify="center" class="mt-3">
    <el-button @click="handleAdd" round> {{ t('native.n0cf77f513b') }} </el-button>
  </el-row>
</template>
<script setup lang="ts">
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

const { t } = useI18n()

import { Demo03StudentApi } from '@/api/infra/demo/demo03/normal'

const props = defineProps<{
  studentId?: number // 学生编号（主表的关联字段）
}>()
const formLoading = ref(false) // 表单的加载中
const formData = ref<any[]>([])
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

/** 监听主表的关联字段的变化，加载对应的子表数据 */
watch(
  () => props.studentId,
  async (val) => {
    // 1. 重置表单
    formData.value = []
    // 2. val 非空，则加载数据
    if (!val) {
      return
    }
    try {
      formLoading.value = true
      formData.value = await Demo03StudentApi.getDemo03CourseListByStudentId(val)
    } finally {
      formLoading.value = false
    }
  },
  { immediate: true }
)

/** 新增按钮操作 */
const handleAdd = () => {
  const row = {
    id: undefined,
    studentId: undefined,
    name: undefined,
    score: undefined
  }
  row.studentId = props.studentId as any
  formData.value.push(row)
}

/** 删除按钮操作 */
const handleDelete = (index) => {
  formData.value.splice(index, 1)
}

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
