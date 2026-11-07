<template>
  <doc-alert :title="t('native.nfb3ca12aee')" url="https://doc.iocoder.cn/oauth2/" />

  <!-- 搜索 -->
  <ContentWrap>
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="auto"
    >
      <el-form-item :label="t('native.nc131777b2f')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="t('native.n37b3ad1ff3')"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item :label="t('native.n6320b4a872')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="t('native.n230a072be4')"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"
          ><Icon icon="ep:search" class="mr-5px" /> {{ t('native.n44ce7ae909') }}
        </el-button>
        <el-button @click="resetQuery"
          ><Icon icon="ep:refresh" class="mr-5px" /> {{ t('native.ncb5d682bac') }}
        </el-button>
        <el-button
          plain
          type="primary"
          @click="openForm('create')"
          v-hasPermi="['system:oauth2-client:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> {{ t('native.n0006d696d8') }}
        </el-button>
        <el-button
          plain
          type="danger"
          :disabled="checkedIds.length === 0"
          @click="handleDeleteBatch"
          v-hasPermi="['system:oauth2-client:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" /> {{ t('native.nddae7a0fc5') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" />
      <el-table-column
        min-width="160"
        :label="t('native.n0f8836ccbd')"
        align="center"
        prop="clientId"
      />
      <el-table-column
        min-width="160"
        :label="t('native.n7183484f9a')"
        align="center"
        prop="secret"
      />
      <el-table-column
        min-width="160"
        :label="t('native.nc131777b2f')"
        align="center"
        prop="name"
      />
      <el-table-column min-width="160" :label="t('native.nc96905bc77')" align="center" prop="logo">
        <template #default="scope">
          <img width="40px" height="40px" :src="scope.row.logo" />
        </template>
      </el-table-column>
      <el-table-column
        min-width="110"
        :label="t('native.n6320b4a872')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        min-width="160"
        :label="t('native.n699b4727f3')"
        align="center"
        prop="accessTokenValiditySeconds"
      >
        <template #default="scope"
          >{{ scope.row.accessTokenValiditySeconds }} {{ t('native.n9dcdc2b289') }}
        </template>
      </el-table-column>
      <el-table-column
        min-width="160"
        :label="t('native.n038979ee16')"
        align="center"
        prop="refreshTokenValiditySeconds"
      >
        <template #default="scope"
          >{{ scope.row.refreshTokenValiditySeconds }} {{ t('native.n9dcdc2b289') }}
        </template>
      </el-table-column>
      <el-table-column
        min-width="160"
        :label="t('native.n517075aaa1')"
        align="center"
        prop="authorizedGrantTypes"
      >
        <template #default="scope">
          <el-tag
            :disable-transitions="true"
            :key="index"
            v-for="(authorizedGrantType, index) in scope.row.authorizedGrantTypes"
            :index="index"
            class="mr-5px"
          >
            {{ authorizedGrantType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        :label="t('native.n07ec86e0f1')"
        align="center"
        prop="createTime"
        width="180"
        :formatter="dateFormatter"
      />
      <el-table-column
        fixed="right"
        min-width="160"
        :label="t('native.ned31fbb483')"
        align="center"
      >
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['system:oauth2-client:update']"
          >
            {{ t('native.n0518365699') }}
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['system:oauth2-client:delete']"
          >
            {{ t('native.n2f9daa8289') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <ClientForm ref="formRef" @success="getList" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import * as ClientApi from '@/api/system/oauth2/client'
import ClientForm from './ClientForm.vue'

defineOptions({ name: 'SystemOAuth2Client' })

const message = useMessage() // 消息弹窗
const { t } = useI18n() // 国际化

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: null,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await ClientApi.getOAuth2ClientPage(queryParams)
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
    await ClientApi.deleteOAuth2Client(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (rows: ClientApi.OAuth2ClientVO[]) => {
  checkedIds.value = rows.map((row) => row.id)
}

const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起批量删除
    await ClientApi.deleteOAuth2ClientList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
