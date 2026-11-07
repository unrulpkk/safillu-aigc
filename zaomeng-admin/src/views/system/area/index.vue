<template>
  <doc-alert :title="t('native.n1d0ee9b0c8')" url="https://doc.iocoder.cn/area-and-ip/" />

  <!-- 操作栏 -->
  <ContentWrap>
    <el-button type="primary" plain @click="openForm()">
      <Icon icon="ep:plus" class="mr-5px" /> {{ t('native.n3c6acb97c9') }}
    </el-button>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <div style="width: 100%; height: 700px">
      <!-- AutoResizer 自动调节大小 -->
      <el-auto-resizer>
        <template #default="{ height, width }">
          <!-- Virtualized Table 虚拟化表格：高性能，解决表格在大数据量下的卡顿问题 -->
          <el-table-v2
            v-loading="loading"
            :columns="columns"
            :data="list"
            :width="width"
            :height="height"
            expand-column-key="id"
          />
        </template>
      </el-auto-resizer>
    </div>
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <AreaForm ref="formRef" />
</template>
<script setup lang="tsx">
const { t } = useI18n()

import { Column } from 'element-plus'
import AreaForm from './AreaForm.vue'
import * as AreaApi from '@/api/system/area'

defineOptions({ name: 'SystemArea' })

// 表格的 column 字段
const columns: Column[] = [
  {
    dataKey: 'id', // 需要渲染当前列的数据字段
    get title() {
      return t('native.nfc14772145')
    }, // 显示在单元格表头的文本
    width: 400, // 当前列的宽度，必须设置
    fixed: true, // 是否固定列
    key: 'id' // 树形展开对应的 key
  },
  {
    dataKey: 'name',
    get title() {
      return t('native.ne47de72feb')
    },
    width: 200
  }
]
const loading = ref(true) // 列表的加载中
const list = ref([]) // 表格的数据

/** 获得数据列表 */
const getList = async () => {
  loading.value = true
  try {
    list.value = await AreaApi.getAreaTree()
  } finally {
    loading.value = false
  }
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = () => {
  formRef.value.open()
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
