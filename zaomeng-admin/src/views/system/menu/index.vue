<template>
  <doc-alert :title="t('native.ndf0752fa82')" url="https://doc.iocoder.cn/resource-permission" />
  <doc-alert :title="t('native.nf4ec27594f')" url="https://doc.iocoder.cn/vue3/route/" />

  <!-- 搜索工作栏 -->
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      :inline="true"
      :model="queryParams"
      class="-mb-15px"
      label-width="auto"
    >
      <el-form-item :label="t('native.n3ecf60fd07')" prop="name">
        <el-input
          v-model="queryParams.name"
          class="!w-240px"
          clearable
          :placeholder="t('native.n460515fd1d')"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item :label="t('native.n6320b4a872')" prop="status">
        <el-select
          v-model="queryParams.status"
          class="!w-240px"
          clearable
          :placeholder="t('native.n066a05a721')"
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
        <el-button @click="handleQuery">
          <Icon class="mr-5px" icon="ep:search" /> {{ t('native.n44ce7ae909') }}
        </el-button>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" /> {{ t('native.ncb5d682bac') }}
        </el-button>
        <el-button
          v-hasPermi="['system:menu:create']"
          plain
          type="primary"
          @click="openForm('create')"
        >
          <Icon class="mr-5px" icon="ep:plus" /> {{ t('native.n0006d696d8') }}
        </el-button>
        <el-button plain type="danger" @click="toggleExpandAll">
          <Icon class="mr-5px" icon="ep:sort" /> {{ t('native.nf5bdbc1eec') }}
        </el-button>
        <el-button plain @click="refreshMenu">
          <Icon class="mr-5px" icon="ep:refresh" /> {{ t('native.nfb5a486d38') }}
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-auto-resizer>
      <template #default="{ width }">
        <el-table-v2
          v-model:expanded-row-keys="expandedRowKeys"
          :columns="columns"
          :data="list"
          :expand-column-key="String(columns[0].key)"
          :height="1000"
          :width="width"
          fixed
          row-key="id"
        />
      </template>
    </el-auto-resizer>
  </ContentWrap>

  <!-- 表单弹窗：添加/修改 -->
  <MenuForm ref="formRef" @success="getList" />
</template>
<script lang="tsx" setup>
const { t } = useI18n()
import { h } from 'vue'
import { nativeKeyByChinese } from '@/locales/native'

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { handleTree } from '@/utils/tree'
import * as MenuApi from '@/api/system/menu'
import { MenuVO } from '@/api/system/menu'
import MenuForm from './MenuForm.vue'
import DictTag from '@/components/DictTag/src/DictTag.vue'
import { Icon } from '@/components/Icon'
import { ElButton, TableV2FixedDir, ElSwitch, type Column } from 'element-plus'
import { checkPermi } from '@/utils/permission'
import { CommonStatusEnum } from '@/utils/constants'
import { CACHE_KEY, useCache } from '@/hooks/web/useCache'
import type { VNode } from 'vue'

defineOptions({ name: 'SystemMenu' })

// 虚拟列表表格（Column<T> 的 T 为单元格数据 cellData 的类型，各列取自 MenuVO 不同字段）
const columns: Column<MenuVO[keyof MenuVO]>[] = [
  {
    key: 'name',
    get title() {
      return t('native.n3ecf60fd07')
    },
    dataKey: 'name',
    cellRenderer: ({ cellData }) =>
      h(
        'span',
        nativeKeyByChinese[String(cellData)]
          ? t(nativeKeyByChinese[String(cellData)])
          : String(cellData)
      ),
    width: 250,
    fixed: TableV2FixedDir.LEFT
  },
  {
    key: 'icon',
    get title() {
      return t('native.n0d720eeea2')
    },
    dataKey: 'icon',
    width: 100,
    align: 'center',
    cellRenderer: ({ cellData: icon }) => (
      <Icon icon={typeof icon === 'string' ? icon : undefined} />
    )
  },
  {
    key: 'sort',
    get title() {
      return t('native.na96c9a8541')
    },
    dataKey: 'sort',
    width: 60
  },
  {
    key: 'permission',
    get title() {
      return t('native.n38cfa59a2a')
    },
    dataKey: 'permission',
    width: 300
  },
  {
    key: 'component',
    get title() {
      return t('native.n723827c828')
    },
    dataKey: 'component',
    width: 500
  },
  {
    key: 'componentName',
    get title() {
      return t('native.nf725ae98dd')
    },
    dataKey: 'componentName',
    width: 200
  },
  {
    key: 'status',
    get title() {
      return t('native.n6320b4a872')
    },
    dataKey: 'status',
    width: 60,
    fixed: TableV2FixedDir.RIGHT,
    cellRenderer: ({ rowData }) => {
      // 检查权限
      if (!checkPermi(['system:menu:update'])) {
        return <DictTag type={DICT_TYPE.COMMON_STATUS} value={rowData.status} />
      }

      // 如果有权限，渲染 ElSwitch
      return (
        <ElSwitch
          v-model={rowData.status}
          active-value={CommonStatusEnum.ENABLE}
          inactive-value={CommonStatusEnum.DISABLE}
          loading={menuStatusUpdating.value[rowData.id]}
          class="ml-4px"
          onChange={(val) => {
            // active/inactive-value 均为数字状态（0 开启、1 禁用），收窄 change 的联合类型
            if (typeof val === 'number') handleStatusChanged(rowData, val)
          }}
        />
      )
    }
  },
  {
    key: 'operations',
    get title() {
      return t('native.ned31fbb483')
    },
    align: 'center',
    width: 160,
    fixed: TableV2FixedDir.RIGHT,
    cellRenderer: ({ rowData }) => {
      // 定义按钮列表
      const buttons: VNode[] = []

      // 检查权限并添加按钮
      if (checkPermi(['system:menu:update'])) {
        buttons.push(
          <ElButton key="edit" link type="primary" onClick={() => openForm('update', rowData.id)}>
            {t('native.n37090f4565')}
          </ElButton>
        )
      }
      if (checkPermi(['system:menu:create'])) {
        buttons.push(
          <ElButton
            key="create"
            link
            type="primary"
            onClick={() => openForm('create', undefined, rowData.id)}
          >
            {t('native.n0006d696d8')}
          </ElButton>
        )
      }
      if (checkPermi(['system:menu:delete'])) {
        buttons.push(
          <ElButton key="delete" link type="danger" onClick={() => handleDelete(rowData.id)}>
            {t('native.n2f9daa8289')}
          </ElButton>
        )
      }
      // 如果没有权限，返回空内容（cellRenderer 需返回 VNode）
      if (buttons.length === 0) {
        return <></>
      }
      // 渲染按钮列表
      return <>{buttons}</>
    }
  }
]

const { wsCache } = useCache()
const message = useMessage() // 消息弹窗

const loading = ref(true) // 列表的加载中
const list = ref<any[]>([]) // 列表的数据
const queryParams = reactive({
  name: undefined,
  status: undefined
})
const queryFormRef = ref() // 搜索的表单
const isExpandAll = ref(false) // 是否展开，默认全部折叠

// 添加展开行控制
const expandedRowKeys = ref<number[]>([])

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await MenuApi.getMenuList(queryParams)
    list.value = handleTree(data)
  } finally {
    loading.value = false
  }
}

/** 搜索按钮操作 */
const handleQuery = () => {
  getList()
}

/** 重置按钮操作 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 添加/修改操作 */
const formRef = ref()
const openForm = (type: string, id?: number, parentId?: number) => {
  formRef.value.open(type, id, parentId)
}

/** 展开/折叠操作 */
const toggleExpandAll = () => {
  if (!isExpandAll.value) {
    // 展开所有
    expandedRowKeys.value = list.value.map((item) => item.id)
  } else {
    // 折叠所有
    expandedRowKeys.value = []
  }
  isExpandAll.value = !isExpandAll.value
}

/** 刷新菜单缓存按钮操作 */
const refreshMenu = async () => {
  try {
    await message.confirm(t('native.nc83bf04619'), t('native.nfb5a486d38'))
    // 清空，从而触发刷新
    wsCache.delete(CACHE_KEY.USER)
    wsCache.delete(CACHE_KEY.ROLE_ROUTERS)
    // 刷新浏览器
    location.reload()
  } catch {}
}

/** 删除按钮操作 */
const handleDelete = async (id: number) => {
  try {
    // 删除的二次确认
    await message.delConfirm()
    // 发起删除
    await MenuApi.deleteMenu(id)
    message.success(t('common.delSuccess'))
    // 刷新列表
    await getList()
  } catch {}
}

/** 开启/关闭菜单的状态 */
const menuStatusUpdating = ref({}) // 菜单状态更新中的 menu 映射。key：菜单编号，value：是否更新中
const handleStatusChanged = async (menu: MenuVO, val: number) => {
  // 1. 标记 menu.id 更新中
  menuStatusUpdating.value[menu.id] = true
  try {
    // 2. 发起更新状态
    menu.status = val
    await MenuApi.updateMenu(menu)
  } finally {
    // 3. 标记 menu.id 更新完成
    menuStatusUpdating.value[menu.id] = false
  }
}

/** 初始化 **/
onMounted(() => {
  getList()
})
</script>
