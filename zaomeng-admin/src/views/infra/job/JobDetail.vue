<template>
  <Dialog v-model="dialogVisible" :title="t('native.n0d7731d116')" width="700px">
    <el-descriptions :column="1" border>
      <el-descriptions-item :label="t('native.n160691af2a')" min-width="60">
        {{ detailData.id }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n2479560deb')">
        {{ detailData.name }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nf850d94925')">
        <dict-tag :type="DICT_TYPE.INFRA_JOB_STATUS" :value="detailData.status" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nb401f25585')">
        {{ detailData.handlerName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n1450e7ee26')">
        {{ detailData.handlerParam }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nc9657db58a')">
        {{ detailData.cronExpression }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n9f28a8874c')">
        {{ detailData.retryCount }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n3e861112e5')">
        {{ t('modules.n02af66b46b', { p0: detailData.retryInterval }) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n11e8b3b52a')">
        {{
          detailData.monitorTimeout > 0
            ? t('modules.n02af66b46b', { p0: detailData.monitorTimeout })
            : t('modules.n3cffa9757b')
        }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n441b809638')">
        <el-timeline>
          <el-timeline-item
            v-for="(nextTime, index) in nextTimes"
            :key="index"
            :timestamp="formatDate(nextTime)"
          >
            {{ t('native.naf50a0fe2a') }} {{ index + 1 }} {{ t('native.n172fb7e67b') }}
          </el-timeline-item>
        </el-timeline>
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as JobApi from '@/api/infra/job'

defineOptions({ name: 'InfraJobDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as JobApi.JobVO) // 详情数据
const nextTimes = ref([]) // 下一轮执行时间的数组

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  // 查看，设置数据
  if (id) {
    detailLoading.value = true
    try {
      detailData.value = await JobApi.getJob(id)
      // 获取下一次执行时间
      nextTimes.value = await JobApi.getJobNextTimes(id)
    } finally {
      detailLoading.value = false
    }
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
