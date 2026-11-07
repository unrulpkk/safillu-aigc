<template>
  <el-form
    :validate-on-rule-change="false"
    ref="formRef"
    :model="formData"
    :rules="rules"
    label-width="auto"
  >
    <el-row>
      <el-col :span="12">
        <el-form-item :label="t('native.na46f29876a')" prop="tableName">
          <el-input v-model="formData.tableName" :placeholder="t('native.n87f973f3f9')" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item :label="t('native.n7ed5c7629a')" prop="tableComment">
          <el-input v-model="formData.tableComment" :placeholder="t('native.n60120ff05f')" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="className">
          <template #label>
            <span>
              {{ t('native.nd1606c17d3') }}
              <el-tooltip :content="t('native.n464381b2d3')" placement="top">
                <Icon class="" icon="ep:question-filled" />
              </el-tooltip>
            </span>
          </template>
          <el-input v-model="formData.className" :placeholder="t('native.n60120ff05f')" />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item :label="t('native.n7d4146ca08')" prop="author">
          <el-input v-model="formData.author" :placeholder="t('native.n60120ff05f')" />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item :label="t('native.ndaede98817')" prop="remark">
          <el-input v-model="formData.remark" :rows="3" type="textarea" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>
<script lang="ts" setup>
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

const { t } = useI18n()

import * as CodegenApi from '@/api/infra/codegen'
import { PropType } from 'vue'

defineOptions({ name: 'InfraCodegenBasicInfoForm' })

const props = defineProps({
  table: {
    type: Object as PropType<Nullable<CodegenApi.CodegenTableSaveReqVO>>,
    default: () => null
  }
})

const formRef = ref()
const formData = ref<CodegenApi.CodegenTableSaveReqVO>(
  CodegenApi.createEmptyCodegenTableSaveReqVO()
)
const rules = reactive({
  tableName: [required],
  tableComment: [required],
  className: [required],
  author: [required]
})

/** 监听 table 属性，复制给 formData 属性 */
watch(
  () => props.table,
  (table) => {
    if (!table) return
    formData.value = table
  },
  {
    deep: true,
    immediate: true
  }
)

defineExpose({
  validate: async () => unref(formRef)?.validate()
})

useLocalizedFormValidation(formRef)
</script>
