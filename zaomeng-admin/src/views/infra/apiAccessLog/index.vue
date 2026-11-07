<template>
  <doc-alert :title="t('native.n366ada1d2f')" url="https://doc.iocoder.cn/system-log/" />

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="auto"
    >
      <el-form-item :label="t('native.n42dff475ce')" prop="userId">
        <el-input
          v-model="queryParams.userId"
          :placeholder="t('native.n4111712100')"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item :label="t('native.naec6d9b3d2')" prop="userType">
        <el-select
          v-model="queryParams.userType"
          :placeholder="t('native.n341bcd53d6')"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.USER_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.nc131777b2f')" prop="applicationName">
        <el-input
          v-model="queryParams.applicationName"
          :placeholder="t('native.n37b3ad1ff3')"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item :label="t('native.n444e4ac452')" prop="beginTime">
        <el-date-picker
          v-model="queryParams.beginTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          :start-placeholder="t('native.n760506491e')"
          :end-placeholder="t('native.n895cd52fbb')"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item :label="t('native.n01ed02afa8')" prop="duration">
        <el-input
          v-model="queryParams.duration"
          :placeholder="t('native.n43428bab74')"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item :label="t('native.nae7f5889b6')" prop="resultCode">
        <el-input
          v-model="queryParams.resultCode"
          :placeholder="t('native.nb04e27cbdc')"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"
          ><Icon icon="ep:search" class="mr-5px" /> {{ t('native.n44ce7ae909') }}
        </el-button>
        <el-button @click="resetQuery"
          ><Icon icon="ep:refresh" class="mr-5px" /> {{ t('native.ncb5d682bac') }}
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['infra:api-access-log:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> {{ t('native.n76420433f2') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
      <el-table-column
        :label="t('native.nb9835ba22a')"
        align="center"
        prop="id"
        width="100"
        fix="right"
      />
      <el-table-column
        min-width="160"
        :label="t('native.n42dff475ce')"
        align="center"
        prop="userId"
      />
      <el-table-column
        min-width="160"
        :label="t('native.naec6d9b3d2')"
        align="center"
        prop="userType"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.USER_TYPE" :value="scope.row.userType" />
        </template>
      </el-table-column>
      <el-table-column
        :label="t('native.nc131777b2f')"
        align="center"
        prop="applicationName"
        width="150"
      />
      <el-table-column
        :label="t('native.ncf206666a6')"
        align="center"
        prop="requestMethod"
        width="80"
      />
      <el-table-column
        :label="t('native.n65a1f5468e')"
        align="center"
        prop="requestUrl"
        width="500"
      />
      <el-table-column :label="t('native.n444e4ac452')" align="center" prop="beginTime" width="180">
        <template #default="scope">
          <span>{{ formatDate(scope.row.beginTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('native.n01ed02afa8')" align="center" prop="duration" width="180">
        <template #default="scope"> {{ scope.row.duration }} ms </template>
      </el-table-column>
      <el-table-column
        min-width="110"
        :label="t('native.n4347dbc9b6')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          {{
            scope.row.resultCode === 0
              ? t('native.n053461ce86')
              : t('modules.n4226873855', { p0: scope.row.resultMsg })
          }}
        </template>
      </el-table-column>
      <el-table-column
        :label="t('native.ndb4b8d1cce')"
        align="center"
        prop="operateModule"
        width="180"
      />
      <el-table-column
        :label="t('native.n5eb9cb4510')"
        align="center"
        prop="operateName"
        width="180"
      />
      <el-table-column
        min-width="160"
        :label="t('native.n1c80d4a064')"
        align="center"
        prop="operateType"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.INFRA_OPERATE_TYPE" :value="scope.row.operateType" />
        </template>
      </el-table-column>
      <el-table-column :label="t('native.ned31fbb483')" align="center" fixed="right" width="60">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openDetail(scope.row)"
            v-hasPermi="['infra:api-access-log:query']"
          >
            {{ t('native.nca3634c751') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页组件 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：详情 -->
  <ApiAccessLogDetail ref="detailRef" />
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import download from '@/utils/download'
import { formatDate } from '@/utils/formatTime'
import * as ApiAccessLogApi from '@/api/infra/apiAccessLog'
import ApiAccessLogDetail from './ApiAccessLogDetail.vue'

defineOptions({ name: 'InfraApiAccessLog' })

const message = useMessage() // 消息弹窗

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: null,
  userType: null,
  applicationName: null,
  requestUrl: null,
  duration: null,
  resultCode: null,
  beginTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ApiAccessLogApi.getApiAccessLogPage(queryParams)
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

/** 详情操作 */
const detailRef = ref()
const openDetail = (data: ApiAccessLogApi.ApiAccessLogVO) => {
  detailRef.value.open(data)
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await ApiAccessLogApi.exportApiAccessLog(queryParams)
    download.excel(data, t('native.n0a1b2c3864'))
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
