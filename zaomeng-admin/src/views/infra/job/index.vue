<template>
  <doc-alert :title="t('native.n3c39e9bde1')" url="https://doc.iocoder.cn/job/" />
  <doc-alert :title="t('native.nc60be646dc')" url="https://doc.iocoder.cn/async-task/" />
  <doc-alert :title="t('native.n5b53d6e8f4')" url="https://doc.iocoder.cn/message-queue/" />

  <ContentWrap>
    <!-- 搜索工作栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="auto"
    >
      <el-form-item :label="t('native.n2479560deb')" prop="name">
        <el-input
          v-model="queryParams.name"
          :placeholder="t('native.n1f01b74c34')"
          clearable
          @keyup.enter="handleQuery"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item :label="t('native.nf850d94925')" prop="status">
        <el-select
          v-model="queryParams.status"
          :placeholder="t('native.n852b71928b')"
          clearable
          class="!w-240px"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.INFRA_JOB_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.nb401f25585')" prop="handlerName">
        <el-input
          v-model="queryParams.handlerName"
          :placeholder="t('native.n44511eb565')"
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
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['infra:job:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> {{ t('native.n0006d696d8') }}
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="checkedIds.length === 0"
          @click="handleDeleteBatch"
          v-hasPermi="['infra:job:delete']"
        >
          <Icon icon="ep:delete" class="mr-5px" /> {{ t('native.nddae7a0fc5') }}
        </el-button>
        <el-button
          type="success"
          plain
          @click="handleExport"
          :loading="exportLoading"
          v-hasPermi="['infra:job:export']"
        >
          <Icon icon="ep:download" class="mr-5px" /> {{ t('native.n76420433f2') }}
        </el-button>
        <el-button type="info" plain @click="handleJobLog()" v-hasPermi="['infra:job:query']">
          <Icon icon="ep:zoom-in" class="mr-5px" /> {{ t('native.ne63358b9fa') }}
        </el-button>
        <el-button
          type="warning"
          plain
          @click="handleSyncJob()"
          :loading="syncLoading"
          v-hasPermi="['infra:job:create']"
        >
          <Icon icon="ep:refresh" class="mr-5px" /> {{ t('native.n341950f5ab') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" @selection-change="handleRowCheckboxChange">
      <el-table-column type="selection" width="55" />
      <el-table-column min-width="85" :label="t('native.n160691af2a')" align="center" prop="id" />
      <el-table-column
        min-width="160"
        :label="t('native.n2479560deb')"
        align="center"
        prop="name"
      />
      <el-table-column
        min-width="110"
        :label="t('native.nf850d94925')"
        align="center"
        prop="status"
      >
        <template #default="scope">
          <dict-tag :type="DICT_TYPE.INFRA_JOB_STATUS" :value="scope.row.status" />
        </template>
      </el-table-column>
      <el-table-column
        min-width="160"
        :label="t('native.nb401f25585')"
        align="center"
        prop="handlerName"
      />
      <el-table-column
        min-width="160"
        :label="t('native.n1450e7ee26')"
        align="center"
        prop="handlerParam"
      />
      <el-table-column
        min-width="160"
        :label="t('native.nf6b382a662')"
        align="center"
        prop="cronExpression"
      />
      <el-table-column fixed="right" :label="t('native.ned31fbb483')" align="center" width="200">
        <template #default="scope">
          <el-button
            type="primary"
            link
            @click="openForm('update', scope.row.id)"
            v-hasPermi="['infra:job:update']"
          >
            {{ t('native.n37090f4565') }}
          </el-button>
          <el-button
            type="primary"
            link
            @click="handleChangeStatus(scope.row)"
            v-hasPermi="['infra:job:update']"
          >
            {{
              scope.row.status === InfraJobStatusEnum.STOP
                ? t('native.n8da97ddda9')
                : t('native.n8d12fc0d4e')
            }}
          </el-button>
          <el-button
            type="danger"
            link
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['infra:job:delete']"
          >
            {{ t('native.n2f9daa8289') }}
          </el-button>
          <el-dropdown
            @command="(command) => handleCommand(command, scope.row)"
            v-hasPermi="['infra:job:trigger', 'infra:job:query']"
          >
            <el-button type="primary" link
              ><Icon icon="ep:d-arrow-right" /> {{ t('native.n38844b135c') }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="handleRun" v-if="checkPermi(['infra:job:trigger'])">
                  {{ t('native.n05ca716e4a') }}
                </el-dropdown-item>
                <el-dropdown-item command="openDetail" v-if="checkPermi(['infra:job:query'])">
                  {{ t('native.n0d7731d116') }}
                </el-dropdown-item>
                <el-dropdown-item command="handleJobLog" v-if="checkPermi(['infra:job:query'])">
                  {{ t('native.n38267025f1') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
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

  <!-- 表单弹窗：添加/修改 -->
  <JobForm ref="formRef" @success="getList" />
  <!-- 表单弹窗：查看 -->
  <JobDetail ref="detailRef" />
</template>
<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { checkPermi } from '@/utils/permission'
import JobForm from './JobForm.vue'
import JobDetail from './JobDetail.vue'
import download from '@/utils/download'
import * as JobApi from '@/api/infra/job'
import { InfraJobStatusEnum } from '@/utils/constants'

defineOptions({ name: 'InfraJob' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗
const { push } = useRouter() // 路由

const loading = ref(true) // 列表的加载中
const total = ref(0) // 列表的总页数
const list = ref([]) // 列表的数据
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  status: undefined,
  handlerName: undefined
})
const queryFormRef = ref() // 搜索的表单
const exportLoading = ref(false) // 导出的加载中
const syncLoading = ref(false) // 同步的加载中

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await JobApi.getJobPage(queryParams)
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

/** 导出按钮操作 */
const handleExport = async () => {
  try {
    // 导出的二次确认
    await message.exportConfirm()
    // 发起导出
    exportLoading.value = true
    const data = await JobApi.exportJob(queryParams)
    download.excel(data, t('native.nc32823fb77'))
  } catch {
  } finally {
    exportLoading.value = false
  }
}

/** 同步任务到 Quartz */
const handleSyncJob = async () => {
  try {
    await message.confirm(t('native.n5d371ad7d5'), t('common.reminder'))
    syncLoading.value = true
    await JobApi.syncJob()
    message.success(t('native.nbca900e904'))
    await getList()
  } catch {
  } finally {
    syncLoading.value = false
  }
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number) => {
  formRef.value.open(type, id)
}

/** 修改状态操作 */
const handleChangeStatus = async (row: JobApi.JobVO) => {
  try {
    // 修改状态的二次确认
    const text =
      row.status === InfraJobStatusEnum.STOP ? t('native.n8da97ddda9') : t('native.n3fd47edce4')
    await message.confirm(t('native.n40645bc47e', { p0: text, p1: row.id }), t('common.reminder'))
    const status =
      row.status === InfraJobStatusEnum.STOP ? InfraJobStatusEnum.NORMAL : InfraJobStatusEnum.STOP
    await JobApi.updateJobStatus(row.id, status)
    message.success(t('native.n4a6b6335ca', { p0: text }))
    // 刷新列表
    await getList()
  } catch {}
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await JobApi.deleteJob(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 批量删除按钮操作 */
const checkedIds = ref<number[]>([])
const handleRowCheckboxChange = (rows: JobApi.JobVO[]) => {
  checkedIds.value = rows.map((row) => row.id)
}

const handleDeleteBatch = async () => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起批量删除
    await JobApi.deleteJobList(checkedIds.value)
    checkedIds.value = []
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** '更多'操作按钮 */
const handleCommand = (command, row) => {
  switch (command) {
    case 'handleRun':
      handleRun(row)
      break
    case 'openDetail':
      openDetail(row.id)
      break
    case 'handleJobLog':
      handleJobLog(row?.id)
      break
    default:
      break
  }
}

/** 执行一次 */
const handleRun = async (row: JobApi.JobVO) => {
  try {
    // 二次确认
    await message.confirm(t('native.n7cde97a149', { p0: row.name }), t('common.reminder'))
    // 提交执行
    await JobApi.runJob(row.id)
    message.success(t('native.nae3fb35412'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 查看操作 */
const detailRef = ref()
const openDetail = (id: number) => {
  detailRef.value.open(id)
}

/** 跳转执行日志 */
const handleJobLog = (id?: number) => {
  if (id && id > 0) {
    push('/job/job-log?id=' + id)
  } else {
    push('/job/job-log')
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
