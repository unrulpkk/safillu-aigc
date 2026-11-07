<template>
  <el-tree-select
    :model-value="modelValue"
    :data="deptTree"
    :props="defaultProps"
    :multiple="multiple"
    :disabled="disabled"
    :clearable="clearable"
    :filterable="filterable"
    :loading="loading"
    :placeholder="placeholder || t('native.n3a3f6e116a')"
    check-strictly
    node-key="id"
    :render-after-expand="false"
    @update:model-value="handleChange"
  />
</template>

<script lang="ts" setup>
const { t } = useI18n()

import { parseId, isIdType } from '@/utils/id'

import * as DeptApi from '@/api/system/dept'
import { defaultProps, handleTree } from '@/utils/tree'

defineOptions({ name: 'DeptSelect' })

withDefaults(
  defineProps<{
    modelValue?: number | number[]
    multiple?: boolean
    disabled?: boolean
    clearable?: boolean
    filterable?: boolean
    placeholder?: string
  }>(),
  {
    multiple: false,
    disabled: false,
    clearable: true,
    filterable: true,
    placeholder: ''
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: number | number[] | undefined]
}>()

const deptTree = ref<Tree[]>([]) // 部门树
const loading = ref(false) // 部门树的加载中

/** 选中变化 */
const handleChange = (value: unknown) => {
  if (Array.isArray(value)) {
    emit('update:modelValue', value.map(parseId).filter(isIdType))
    return
  }
  emit('update:modelValue', parseId(value))
}

/** 获得部门树 */
const getDeptTree = async () => {
  loading.value = true
  try {
    deptTree.value = handleTree(await DeptApi.getSimpleDeptList())
  } finally {
    loading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  getDeptTree()
})
</script>
