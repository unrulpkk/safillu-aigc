<template>
  <Dialog v-model="dialogVisible" :title="t('native.n0d7731d116')" width="700px">
    <el-descriptions :column="1" border>
      <el-descriptions-item :label="t('native.nb9835ba22a')" min-width="60">
        {{ detailData.id }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n160691af2a')">
        {{ detailData.jobId }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nb401f25585')">
        {{ detailData.handlerName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n1450e7ee26')">
        {{ detailData.handlerParam }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n96bd5ada7c')">
        {{ detailData.executeIndex }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n45e03e436e')">
        {{ formatDate(detailData.beginTime) + ' ~ ' + formatDate(detailData.endTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n01ed02afa8')">
        {{ t('modules.n02af66b46b', { p0: detailData.duration }) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nf850d94925')">
        <dict-tag :type="DICT_TYPE.INFRA_JOB_LOG_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.ne04bd1b72f')">
        {{ detailData.result }}
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as JobLogApi from '@/api/infra/jobLog'

defineOptions({ name: 'JobLogDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as JobLogApi.JobLogVO) // 详情数据

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  // 查看，设置数据
  if (id) {
    detailLoading.value = true
    try {
      detailData.value = await JobLogApi.getJobLog(id)
    } finally {
      detailLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
