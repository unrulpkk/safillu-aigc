<template>
  <el-table ref="dragTable" :data="formData" :max-height="tableHeight" row-key="columnId">
    <el-table-column
      :show-overflow-tooltip="true"
      :label="t('native.n6ebab7adf3')"
      min-width="10%"
      prop="columnName"
    />
    <el-table-column :label="t('native.n5fa37523f5')" min-width="10%">
      <template #default="scope">
        <el-input v-model="scope.row.columnComment" />
      </template>
    </el-table-column>
    <el-table-column
      :show-overflow-tooltip="true"
      :label="t('native.n018bc1a8a2')"
      min-width="10%"
      prop="dataType"
    />
    <el-table-column :label="t('native.n4925f9b091')" min-width="11%">
      <template #default="scope">
        <el-select v-model="scope.row.javaType">
          <el-option label="Long" value="Long" />
          <el-option label="String" value="String" />
          <el-option label="Integer" value="Integer" />
          <el-option label="Double" value="Double" />
          <el-option label="BigDecimal" value="BigDecimal" />
          <el-option label="LocalDateTime" value="LocalDateTime" />
          <el-option label="Boolean" value="Boolean" />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n23a5607dd0')" min-width="10%">
      <template #default="scope">
        <el-input v-model="scope.row.javaField" />
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n4268493cd3')" min-width="4%">
      <template #default="scope">
        <el-checkbox v-model="scope.row.createOperation" false-value="false" true-value="true" />
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n0518365699')" min-width="4%">
      <template #default="scope">
        <el-checkbox v-model="scope.row.updateOperation" false-value="false" true-value="true" />
      </template>
    </el-table-column>
    <el-table-column :label="t('native.naedd6814ff')" min-width="4%">
      <template #default="scope">
        <el-checkbox
          v-model="scope.row.listOperationResult"
          false-value="false"
          true-value="true"
        />
      </template>
    </el-table-column>
    <el-table-column :label="t('native.nbcd6771e08')" min-width="4%">
      <template #default="scope">
        <el-checkbox v-model="scope.row.listOperation" false-value="false" true-value="true" />
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n3e8a780477')" min-width="10%">
      <template #default="scope">
        <el-select v-model="scope.row.listOperationCondition">
          <el-option label="=" value="=" />
          <el-option label="!=" value="!=" />
          <el-option label=">" value=">" />
          <el-option label=">=" value=">=" />
          <el-option label="<" value="<>" />
          <el-option label="<=" value="<=" />
          <el-option label="LIKE" value="LIKE" />
          <el-option label="BETWEEN" value="BETWEEN" />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n69347b8462')" min-width="5%">
      <template #default="scope">
        <el-checkbox v-model="scope.row.nullable" false-value="false" true-value="true" />
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n925f703e36')" min-width="12%">
      <template #default="scope">
        <el-select v-model="scope.row.htmlType">
          <el-option :label="t('native.nbeade57a72')" value="input" />
          <el-option :label="t('native.nfa196b339e')" value="textarea" />
          <el-option :label="t('native.n7795013e81')" value="select" />
          <el-option :label="t('native.n4f941dece5')" value="radio" />
          <el-option :label="t('native.n48b5075906')" value="checkbox" />
          <el-option :label="t('native.nbb38456f27')" value="datetime" />
          <el-option :label="t('native.n07c9f74c99')" value="imageUpload" />
          <el-option :label="t('native.nd870a31beb')" value="fileUpload" />
          <el-option :label="t('native.n93faf81906')" value="editor" />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n4adeb7d216')" min-width="12%">
      <template #default="scope">
        <el-select
          v-model="scope.row.dictType"
          :value-on-clear="''"
          clearable
          filterable
          :placeholder="t('native.nda590a8fe3')"
        >
          <template #header>
            <div class="flex justify-end">
              <el-popover class="box-item" :content="t('native.n850116fc73')" placement="top-start">
                <template #reference>
                  <el-button :icon="Refresh" size="small" circle @click="getDictOptions" class="" />
                </template>
              </el-popover>
            </div>
          </template>
          <el-option
            v-for="dict in dictOptions"
            :key="dict.id"
            :label="dict.name"
            :value="dict.type"
          />
        </el-select>
      </template>
    </el-table-column>
    <el-table-column :label="t('native.n0d53f08678')" min-width="10%">
      <template #default="scope">
        <el-input v-model="scope.row.example" />
      </template>
    </el-table-column>
  </el-table>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { PropType } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as CodegenApi from '@/api/infra/codegen'
import * as DictDataApi from '@/api/system/dict/dict.type'

defineOptions({ name: 'InfraCodegenColumInfoForm' })

const props = defineProps({
  columns: {
    type: Array as unknown as PropType<CodegenApi.CodegenColumnVO[]>,
    default: () => null
  }
})

const formData = ref<CodegenApi.CodegenColumnVO[]>([])
const tableHeight = document.documentElement.scrollHeight - 350 + 'px'

/** 查询字典下拉列表 */
const dictOptions = ref<DictDataApi.DictTypeVO[]>()
const getDictOptions = async () => {
  dictOptions.value = await DictDataApi.getSimpleDictTypeList()
}

watch(
  () => props.columns,
  (columns) => {
    if (!columns) return
    formData.value = columns
  },
  {
    deep: true,
    immediate: true
  }
)

onMounted(async () => {
  await getDictOptions()
})
</script>
