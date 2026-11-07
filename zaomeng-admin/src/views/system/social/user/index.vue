<template>
  <doc-alert :title="t('native.n50b072fb32')" url="https://doc.iocoder.cn/social-user/" />

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="auto"
    >
      <el-form-item :label="t('native.n0d7ef7a81f')" prop="type">
        <el-select
          v-model="queryParams.type"
          class="!w-240px"
          clearable
          :placeholder="t('native.nb24561baf8')"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_SOCIAL_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.n71574c9a99')" prop="nickname">
        <el-input
          v-model="queryParams.nickname"
          class="!w-240px"
          clearable
          :placeholder="t('native.n0cb61af3a6')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('native.n9ebed29022')" prop="openid">
        <el-input
          v-model="queryParams.openid"
          class="!w-240px"
          clearable
          :placeholder="t('native.n96ff2cef46')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('native.n07ec86e0f1')" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
          :end-placeholder="t('native.n895cd52fbb')"
          :start-placeholder="t('native.n760506491e')"
          type="daterange"
          value-format="YYYY-MM-DD HH:mm:ss"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" /> {{ t('native.n44ce7ae909') }}
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" /> {{ t('native.ncb5d682bac') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" :show-overflow-tooltip="true" :stripe="true">
      <el-table-column min-width="160" align="center" :label="t('native.n0d7ef7a81f')" prop="type">
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.SYSTEM_SOCIAL_TYPE" :value="scope.row.type" />
        </template>
      </el-table-column>
      <el-table-column
        min-width="160"
        align="center"
        :label="t('native.n9ebed29022')"
        prop="openid"
      />
      <el-table-column
        min-width="160"
        align="center"
        :label="t('native.n71574c9a99')"
        prop="nickname"
      />
      <el-table-column
        min-width="160"
        align="center"
        :label="t('native.n8501bb8e47')"
        prop="avatar"
      >
        <template #default="{ row }">
          <el-image :src="row.avatar" class="h-30px w-30px" @click="imagePreview(row.avatar)" />
        </template>
      </el-table-column>
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('native.n07ec86e0f1')"
        prop="createTime"
        width="180px"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('native.n0a5f9a8929')"
        prop="updateTime"
        width="180px"
      />
      <el-table-column
        min-width="160"
        align="center"
        fixed="right"
        :label="t('native.ned31fbb483')"
      >
        <template #default="scope">
          <el-button
            v-hasPermi="['system:social-user:query']"
            link
            type="primary"
            @click="openDetail(scope.row.id)"
          >
            {{ t('native.n979a332955') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：详情 -->
  <SocialUserDetail ref="detailRef" />
</template>

<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as SocialUserApi from '@/api/system/social/user'
import SocialUserDetail from './SocialUserDetail.vue'
import { createImageViewer } from '@/components/ImageViewer'

defineOptions({ name: 'SocialUser' })

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  type: undefined,
  openid: undefined,
  nickname: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await SocialUserApi.getSocialUserPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const imagePreview = (imgUrl: string) => {
  createImageViewer({
    urlList: [imgUrl]
  })
}

/** 详情操作 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
