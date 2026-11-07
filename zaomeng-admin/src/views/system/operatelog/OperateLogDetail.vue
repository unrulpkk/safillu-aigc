<template>
  <Dialog
    v-model="dialogVisible"
    :max-height="500"
    :scroll="true"
    :title="t('native.n979a332955')"
    width="800"
  >
    <el-descriptions :column="1" border>
      <el-descriptions-item :label="t('native.n1a893a7d52')" min-width="120">
        {{ detailData.id }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nbd06f735f4')" v-if="detailData.traceId">
        {{ detailData.traceId }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nbe75e84d3a')">
        {{ detailData.userId }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.ncca00c1b1a')">
        {{ detailData.userName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nf7ce800edc')">
        {{ detailData.userIp }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n17952074fb')">
        {{ detailData.userAgent }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.ndb4b8d1cce')">
        {{ detailData.type }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n5eb9cb4510')">
        {{ detailData.subType }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n695d2c3939')">
        {{ detailData.action }}
      </el-descriptions-item>
      <el-descriptions-item v-if="detailData.extra" :label="t('native.n60d97e2589')">
        {{ detailData.extra }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n4026240260')">
        {{ detailData.requestMethod }} {{ detailData.requestUrl }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n8f3f6ffb7d')">
        {{ formatDate(detailData.createTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n350fcc33fe')">
        {{ detailData.bizId }}
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { formatDate } from '@/utils/formatTime'
import * as OperateLogApi from '@/api/system/operatelog'

defineOptions({ name: 'SystemOperateLogDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as OperateLogApi.OperateLogVO) // 详情数据

/** 打开弹窗 */
const open = async (data: OperateLogApi.OperateLogVO) => {
  dialogVisible.value = true
  // 设置数据
  detailLoading.value = true
  try {
    detailData.value = data
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
