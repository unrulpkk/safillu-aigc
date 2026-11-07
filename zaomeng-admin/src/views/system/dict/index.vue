<template>
  <!-- 搜索工作栏 -->
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="auto"
    >
      <el-form-item :label="t('native.nba834852d2')" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-240px"
          clearable
          :placeholder="t('native.n227ef9d803')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('native.n4adeb7d216')" prop="type">
        <el-input
          v-model="queryParams.type"
          class="!w-240px"
          clearable
          :placeholder="t('native.nf4418d471b')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('native.n6320b4a872')" prop="status">
        <el-select
          v-model="queryParams.status"
          class="!w-240px"
          clearable
          :placeholder="t('native.nc2c6686685')"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
        <el-button
          v-hasPermi="['system:dict:create']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" /> {{ t('native.n0006d696d8') }}
        </el-button>
        <el-button
          v-hasPermi="['system:dict:export']"
          :loading="exportLoading"
          plain
          type="success"
          @click="handleExport"
        >
          <Icon class="mr-5px" icon="ep:download" /> {{ t('native.n76420433f2') }}
        </el-button>
        <el-button
          v-hasPermi="['system:dict:delete']"
          :disabled="checkedIds.length === 0"
          plain
          type="danger"
          @click="handleDeleteBatch"
        >
          <Icon class="mr-5px" icon="ep:delete" /> {{ t('native.nddae7a0fc5') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" />
      <el-table-column min-width="85" align="center" :label="t('native.ne79e5f898a')" prop="id" />
      <el-table-column
        min-width="160"
        align="center"
        :label="t('native.nba834852d2')"
        prop="name"
        show-overflow-tooltip
      />
      <el-table-column align="center" :label="t('native.n4adeb7d216')" prop="type" width="300" />
      <el-table-column
        min-width="110"
        align="center"
        :label="t('native.n6320b4a872')"
        prop="status"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        min-width="160"
        align="center"
        :label="t('native.ndaede98817')"
        prop="remark"
      />
      <el-table-column
        :formatter="dateFormatter"
        align="center"
        :label="t('native.n07ec86e0f1')"
        prop="createTime"
        width="180"
      />
      <el-table-column
        fixed="right"
        min-width="160"
        align="center"
        :label="t('native.ned31fbb483')"
      >
        <template #default="scope">
          <el-button
            v-hasPermi="['system:dict:update']"
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
          >
            {{ t('native.n37090f4565') }}
          </el-button>
          <router-link :to="'/dict/type/data/' + scope.row.type">
            <el-button link type="primary"> {{ t('native.n5440f7424f') }} </el-button>
          </router-link>
          <el-button
            v-hasPermi="['system:dict:delete']"
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
          >
            {{ t('native.n2f9daa8289') }}
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

  <!-- 表单弹窗：添加/修改 -->
  <DictTypeForm ref="formRef" @success="getList" />
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as DictTypeApi from '@/api/system/dict/dict.type'
import DictTypeForm from './DictTypeForm.vue'
import download from '@/utils/download'

defineOptions({ name: 'SystemDictType' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 字典表格数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: '',
  type: '',
  status: undefined,
  createTime: []
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中

/** 查询字典类型列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await DictTypeApi.getDictTypePage(queryParams)
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

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await DictTypeApi.deleteDictType(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (rows: DictTypeApi.DictTypeVO[]) => {
  checkedIds.value = rows.map((row) => row.id!)
}

const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起批量删除
    await DictTypeApi.deleteDictTypeList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await DictTypeApi.exportDictType(queryParams)
    download.excel(data, t('native.n168ccb12f3'))
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
