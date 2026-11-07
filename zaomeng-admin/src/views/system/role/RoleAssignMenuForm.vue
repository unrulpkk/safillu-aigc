<template>
  <Dialog v-model="dialogVisible" :title="t('native.nfd15105b10')">
    <el-form ref="formRef" v-loading="formLoading" :model="formData" label-width="auto">
      <el-form-item :label="t('native.n519006fe53')">
        <el-tag>{{ formData.name }}</el-tag>
      </el-form-item>
      <el-form-item :label="t('native.n1bb6ba42be')">
        <el-tag>{{ formData.code }}</el-tag>
      </el-form-item>
      <el-form-item :label="t('native.nfd15105b10')">
        <el-card class="w-full h-400px !overflow-y-scroll" shadow="never">
          <template #header>
            {{ t('native.n5c3a3ac6da') }}
            <el-switch
              v-model="treeNodeAll"
              :active-text="t('native.nb5141d3d19')"
              :inactive-text="t('native.n0c70665b6e')"
              inline-prompt
              @change="handleCheckedTreeNodeAll"
            />
            {{ t('native.na218df9dc0') }}
            <el-switch
              v-model="menuExpand"
              :active-text="t('native.n00bd3960fe')"
              :inactive-text="t('native.n84a07f292f')"
              inline-prompt
              @change="handleCheckedTreeExpand"
            />
          </template>
          <el-tree
            ref="treeRef"
            :data="menuOptions"
            :props="defaultProps"
            :empty-text="t('native.n87ab7ffee7')"
            node-key="id"
            show-checkbox
          />
        </el-card>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">
        {{ t('native.n8d6e0c32e5') }}
      </el-button>
      <el-button @click="dialogVisible = false"> {{ t('native.n1f4d617f83') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { defaultProps, handleTree } from '@/utils/tree'
import * as RoleApi from '@/api/system/role'
import * as MenuApi from '@/api/system/menu'
import * as PermissionApi from '@/api/system/permission'

defineOptions({ name: 'SystemRoleAssignMenuForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formData = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  menuIds: []
})
const formRef = ref() // 表单 Ref
const menuOptions = ref<any[]>([]) // 菜单树形结构
const menuExpand = ref(false) // 展开/折叠
const treeRef = ref() // 菜单树组件 Ref
const treeNodeAll = ref(false) // 全选/全不选

/** 打开弹窗 */
const open = async (row: RoleApi.RoleVO) => {
  dialogVisible.value = true
  resetForm()
  // 加载 Menu 列表。注意，必须放在前面，不然下面 setChecked 没数据节点
  menuOptions.value = handleTree(await MenuApi.getSimpleMenusList())
  // 设置数据
  formData.id = row.id
  formData.name = row.name
  formData.code = row.code
  formLoading.value = true
  try {
    formData.menuIds = await PermissionApi.getRoleMenuList(row.id)
    // 设置选中
    formData.menuIds.forEach((menuId: number) => {
      treeRef.value.setChecked(menuId, true, false)
    })
  } finally {
    formLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  // 提交请求
  formLoading.value = true
  try {
    const data = {
      roleId: formData.id!,
      menuIds: [
        ...(treeRef.value.getCheckedKeys(false) as unknown as Array<number>), // 获得当前选中节点
        ...(treeRef.value.getHalfCheckedKeys() as unknown as Array<number>) // 获得半选中的父节点
      ]
    }
    await PermissionApi.assignRoleMenu(data)
    message.success(t('common.updateSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  // 重置选项
  treeNodeAll.value = false
  menuExpand.value = false
  // 重置表单
  Object.assign(formData, {
    id: undefined,
    name: '',
    code: '',
    menuIds: []
  })
  treeRef.value?.setCheckedNodes([])
  formRef.value?.resetFields()
}

/** 全选/全不选 */
const handleCheckedTreeNodeAll = () => {
  treeRef.value.setCheckedNodes(treeNodeAll.value ? menuOptions.value : [])
}

/** 展开/折叠全部 */
const handleCheckedTreeExpand = () => {
  const nodes = treeRef.value?.store.nodesMap
  for (let node in nodes) {
    if (nodes[node].expanded === menuExpand.value) {
      continue
    }
    nodes[node].expanded = menuExpand.value
  }
}
</script>
