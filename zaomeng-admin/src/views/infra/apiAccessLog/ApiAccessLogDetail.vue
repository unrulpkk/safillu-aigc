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
      <el-descriptions-item :label="t('native.nbd06f735f4')">
        {{ detailData.traceId }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nc131777b2f')">
        {{ detailData.applicationName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n426c4548f6')">
        {{ detailData.userId }}
        <dict-tag :type="DICT_TYPE.USER_TYPE" :value="detailData.userType" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.ned5dcbf763')">
        {{ detailData.userIp }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n6b53ecd207')">
        {{ detailData.userAgent }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nfddcc34878')">
        {{ detailData.requestMethod }} {{ detailData.requestUrl }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n9541ee3d4b')">
        {{ detailData.requestParams }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n929660eb76')">
        {{ detailData.responseBody }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n444e4ac452')">
        {{ formatDate(detailData.beginTime) }} ~ {{ formatDate(detailData.endTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nbcf0ce10de')"
        >{{ detailData.duration }} ms</el-descriptions-item
      >
      <el-descriptions-item :label="t('native.n4347dbc9b6')">
        <div v-if="detailData.resultCode === 0"> {{ t('native.n296de0e31f') }} </div>
        <div v-else-if="detailData.resultCode > 0">
          {{ t('native.n5e1a69ab71') }} {{ detailData.resultCode }} | {{ detailData.resultMsg }}
        </div>
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.ndb4b8d1cce')">
        {{ detailData.operateModule }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n5eb9cb4510')">
        {{ detailData.operateName }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n5eb9cb4510')">
        <dict-tag :type="DICT_TYPE.INFRA_OPERATE_TYPE" :value="detailData.operateType" />
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>

<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as ApiAccessLog from '@/api/infra/apiAccessLog'

defineOptions({ name: 'ApiAccessLogDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单地加载中
const detailData = ref({} as ApiAccessLog.ApiAccessLogVO) // 详情数据

/** 打开弹窗 */
const open = async (data: ApiAccessLog.ApiAccessLogVO) => {
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
