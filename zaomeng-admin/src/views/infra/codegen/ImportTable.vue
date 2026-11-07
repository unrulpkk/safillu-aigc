<template>
  <Dialog v-model="dialogVisible" :title="t('native.n01d117c02c')" width="800px">
    <!-- 搜索栏 -->
    <el-form ref="queryFormRef" :inline="true" :model="queryParams" label-width="auto">
      <el-form-item :label="t('native.n186be2232e')" prop="dataSourceConfigId">
        <el-select
          v-model="queryParams.dataSourceConfigId"
          class="!w-240px"
          :placeholder="t('native.n84daa77d30')"
        >
          <el-option
            v-for="config in dataSourceConfigList"
            :key="config.id"
            :label="config.name"
            :value="config.id!"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.na46f29876a')" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-240px"
          clearable
          :placeholder="t('native.ne197573f48')"
          @keyup.enter="getList"
        />
      </el-form-item>
      <el-form-item :label="t('native.n7ed5c7629a')" prop="comment">
        <el-input
          v-model="queryParams.comment"
          class="!w-240px"
          clearable
          :placeholder="t('native.n5deafd6fcf')"
          @keyup.enter="getList"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="getList">
          <Icon class="mr-5px" icon="ep:search" /> {{ t('native.n44ce7ae909') }}
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" /> {{ t('native.ncb5d682bac') }}
        </el-button>
      </el-form-item>
    </el-form>
    <!-- 列表 -->
    <el-row>
      <el-table
        ref="tableRef"
        v-loading="dbTableLoading"
        :data="dbTableList"
        height="260px"
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column
          min-width="160"
          :show-overflow-tooltip="true"
          :label="t('native.na46f29876a')"
          prop="name"
        />
        <el-table-column
          min-width="160"
          :show-overflow-tooltip="true"
          :label="t('native.n7ed5c7629a')"
          prop="comment"
        />
      </el-table>
    </el-row>
    <!-- 操作 -->
    <template #footer>
      <el-button
        :disabled="tableList.length === 0 || dbTableLoading"
        type="primary"
        @click="handleImportTable"
      >
        {{ t('native.n576d81bb06') }}
      </el-button>
      <el-button @click="close"> {{ t('native.n3fd47edce4') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import * as CodegenApi from '@/api/infra/codegen'
import * as DataSourceConfigApi from '@/api/infra/dataSourceConfig'
import { ElTable } from 'element-plus'

defineOptions({ name: 'InfraCodegenImportTable' })

const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dbTableLoading = ref(true) // 数据源的加载中
const dbTableList = ref<CodegenApi.DatabaseTableVO[]>([]) // 表的列表
const queryParams = reactive({
  name: undefined,
  comment: undefined,
  dataSourceConfigId: 0
})
const dataSourceConfigList = ref<DataSourceConfigApi.DataSourceConfigVO[]>([]) // 数据源列表

/** 查询表数据 */
const getList = async () => {
  dbTableLoading.value = true
  try {
    dbTableList.value = await CodegenApi.getSchemaTableList(queryParams)
  } finally {
    dbTableLoading.value = false
  }
}

/** 重置操作 */
const resetQuery = async () => {
  queryParams.name = undefined
  queryParams.comment = undefined
  queryParams.dataSourceConfigId = dataSourceConfigList.value[0].id as number
  await getList()
}

/** 打开弹窗 */
const open = async () => {
  // 加载数据源的列表
  dataSourceConfigList.value = await DataSourceConfigApi.getDataSourceConfigList()
  queryParams.dataSourceConfigId = dataSourceConfigList.value[0].id as number
  dialogVisible.value = true
  // 加载表的列表
  await getList()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 关闭弹窗 */
const close = () => {
  dialogVisible.value = false
  tableList.value = []
}

const tableRef = ref<typeof ElTable>() // 表格的 Ref
const tableList = ref<string[]>([]) // 选中的表名

/** 处理某一行的点击 */
const handleRowClick = (row) => {
  unref(tableRef)?.toggleRowSelection(row)
}

/** 多选框选中数据 */
const handleSelectionChange = (selection) => {
  tableList.value = selection.map((item) => item.name)
}

/** 导入按钮操作 */
const handleImportTable = async () => {
  dbTableLoading.value = true
  try {
    await CodegenApi.createCodegenList({
      dataSourceConfigId: queryParams.dataSourceConfigId,
      tableNames: tableList.value
    })
    message.success(t('native.n9911f5dcd8'))
    emit('success')
    close()
  } finally {
    dbTableLoading.value = false
  }
}
const emit = defineEmits(['success'])
</script>
