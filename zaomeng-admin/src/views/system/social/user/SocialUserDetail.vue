<template>
  <Dialog v-model="dialogVisible" :title="t('native.n979a332955')" width="800">
    <el-descriptions :column="1" border>
      <el-descriptions-item :label="t('native.n0d7ef7a81f')" min-width="160">
        <dict-tag :type="DICT_TYPE.SYSTEM_SOCIAL_TYPE" :value="detailData.type" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n71574c9a99')" min-width="120">
        {{ detailData.nickname }}
      </el-descriptions-item>
      <el-descriptions :label="t('native.n8501bb8e47')" min-width="120">
        <el-image :src="detailData.avatar" class="h-30px w-30px" />
      </el-descriptions>
      <el-descriptions-item :label="t('native.n3ab0b0c761')" min-width="120">
        {{ detailData.token }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n1a75c1645e')" min-width="120">
        <el-input
          v-model="detailData.rawTokenInfo"
          :autosize="{ maxRows: 20 }"
          :readonly="true"
          type="textarea"
        />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nb80b6c0522')" min-width="120">
        <el-input
          v-model="detailData.rawUserInfo"
          :autosize="{ maxRows: 20 }"
          :readonly="true"
          type="textarea"
        />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n0cfcd77c02')" min-width="120">
        {{ detailData.code }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n66bd829225')" min-width="120">
        {{ detailData.state }}
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE } from '@/utils/dict'
import * as SocialUserApi from '@/api/system/social/user'

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as SocialUserApi.SocialUserVO) // 详情数据

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  // 设置数据
  try {
    detailData.value = await SocialUserApi.getSocialUser(id)
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
