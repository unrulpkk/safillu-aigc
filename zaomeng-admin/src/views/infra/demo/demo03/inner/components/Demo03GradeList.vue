<template>
  <!-- 列表 -->
  <ContentWrap>
    <el-table
      row-key="id"
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
    >
      <el-table-column min-width="85" :label="t('native.nfc14772145')" align="center" prop="id" />
      <el-table-column
        min-width="160"
        :label="t('native.n6773310492')"
        align="center"
        prop="name"
      />
      <el-table-column
        min-width="160"
        :label="t('native.n61a642e7aa')"
        align="center"
        prop="teacher"
      />
      <el-table-column
        :label="t('native.n07ec86e0f1')"
        align="center"
        prop="createTime"
        :formatter="dateFormatter"
        width="180px"
      />
    </el-table>
  </ContentWrap>
</template>
<script setup lang="ts">
const { t } = useI18n()

import { dateFormatter } from '@/utils/formatTime'
import { Demo03Grade, Demo03StudentApi } from '@/api/infra/demo/demo03/inner'

const props = defineProps<{
  studentId?: number // 学生编号（主表的关联字段）
}>()
const loading = ref(false) // 列表的加载中
const list = ref<Demo03Grade[]>([]) // 列表的数据

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await Demo03StudentApi.getDemo03GradeByStudentId(props.studentId)
    if (!data) {
      return
    }
    list.value.push(data)
  } finally {
    loading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
