<template>
  <Dialog v-model="dialogVisible" :title="t(dialogTitle)">
    <el-form
      :validate-on-rule-change="false"
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item :label="t('native.n7c2fc04265')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.nd88d9e13d2')" />
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
      <el-form-item :label="t('native.n6320b4a872')" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="t('native.ndaede98817')" prop="remark">
        <el-input v-model="formData.remark" :placeholder="t('native.n1004c72456')" />
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
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import { defaultProps, handleTree } from '@/utils/tree'
import * as TenantPackageApi from '@/api/system/tenantPackage'
import * as MenuApi from '@/api/system/menu'
import { ElTree } from 'element-plus'

defineOptions({ name: 'SystemTenantPackageForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: null,
  name: null,
  remark: null,
  menuIds: [],
  status: CommonStatusEnum.ENABLE
})
const formRules = reactive({
  name: [
    {
      required: true,
      get message() {
        return t('native.n879d7074ba')
      },
      trigger: 'blur'
    }
  ],
  status: [
    {
      required: true,
      get message() {
        return t('native.ncd0a03ebb7')
      },
      trigger: 'blur'
    }
  ],
  menuIds: [
    {
      required: true,
      get message() {
        return t('native.n70579e5e67')
      },
      trigger: 'blur'
    }
  ]
})
const formRef = ref() // 表单 Ref
const menuOptions = ref<any[]>([]) // 树形结构数据
const menuExpand = ref(false) // 展开/折叠
const treeRef = ref<InstanceType<typeof ElTree>>() // 树组件 Ref
const treeNodeAll = ref(false) // 全选/全不选

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = 'action.' + type
  formType.value = type
  resetForm()
  // 加载 Menu 列表。注意，必须放在前面，不然下面 setChecked 没数据节点
  menuOptions.value = handleTree(await MenuApi.getSimpleMenusList())
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      const res = await TenantPackageApi.getTenantPackage(id)
      // 设置选中
      formData.value = res
      // 设置选中
      res.menuIds.forEach((menuId: number) => {
        treeRef.value!.setChecked(menuId, true, false)
      })
    } finally {
      formLoading.value = false
    }
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
    const data = formData.value as unknown as TenantPackageApi.TenantPackageVO
    data.menuIds = [
      ...(treeRef.value!.getCheckedKeys(false) as unknown as Array<number>), // 获得当前选中节点
      ...(treeRef.value!.getHalfCheckedKeys() as unknown as Array<number>) // 获得半选中的父节点
    ]
    if (formType.value === 'create') {
      await TenantPackageApi.createTenantPackage(data)
      message.success(t('common.createSuccess'))
    } else {
      await TenantPackageApi.updateTenantPackage(data)
      message.success(t('common.updateSuccess'))
    }
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
  formData.value = {
    id: null,
    name: null,
    remark: null,
    menuIds: [],
    status: CommonStatusEnum.ENABLE
  }
  treeRef.value?.setCheckedNodes([])
  formRef.value?.resetFields()
}

/** 全选/全不选 */
const handleCheckedTreeNodeAll = () => {
  treeRef.value!.setCheckedNodes(treeNodeAll.value ? menuOptions.value : [])
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

useLocalizedFormValidation(formRef)
</script>
