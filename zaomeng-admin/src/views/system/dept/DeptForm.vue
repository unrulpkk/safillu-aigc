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
      <el-form-item :label="t('native.nc43577a2f1')" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :data="deptTree"
          :props="defaultProps"
          check-strictly
          default-expand-all
          :placeholder="t('native.nc54dfcc3ab')"
          value-key="deptId"
        />
      </el-form-item>
      <el-form-item :label="t('native.nc029d81328')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n15386329b0')" />
      </el-form-item>
      <el-form-item :label="t('native.n7d8c6e7807')" prop="sort">
        <el-input-number v-model="formData.sort" :min="0" controls-position="right" />
      </el-form-item>
      <el-form-item :label="t('native.na525346277')" prop="leaderUserId">
        <el-select v-model="formData.leaderUserId" clearable :placeholder="t('native.n24906954b1')">
          <el-option
            v-for="item in userList"
            :key="item.id"
            :label="item.nickname"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.nd698182914')" prop="phone">
        <el-input v-model="formData.phone" maxlength="11" :placeholder="t('native.na5eca44f80')" />
      </el-form-item>
      <el-form-item :label="t('native.n73075237fd')" prop="email">
        <el-input v-model="formData.email" maxlength="50" :placeholder="t('native.n59c9ed95eb')" />
      </el-form-item>
      <el-form-item :label="t('native.n6320b4a872')" prop="status">
        <el-select v-model="formData.status" clearable :placeholder="t('native.n230a072be4')">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" @click="submitForm"> {{ t('native.n8d6e0c32e5') }} </el-button>
      <el-button @click="dialogVisible = false"> {{ t('native.n1f4d617f83') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { useLocalizedFormValidation } from '@/hooks/web/useLocalizedFormValidation'

import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { defaultProps, handleTree } from '@/utils/tree'
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { CommonStatusEnum } from '@/utils/constants'
import { FormRules } from 'element-plus'

defineOptions({ name: 'SystemDeptForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  title: '',
  parentId: undefined,
  name: undefined,
  sort: undefined,
  leaderUserId: undefined,
  phone: undefined,
  email: undefined,
  status: CommonStatusEnum.ENABLE
})
const formRules = reactive<FormRules>({
  parentId: [
    {
      required: true,
      get message() {
        return t('native.n0bfd8621e9')
      },
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      get message() {
        return t('native.n8feaf64f27')
      },
      trigger: 'blur'
    }
  ],
  sort: [
    {
      required: true,
      get message() {
        return t('native.nd482d4f4d3')
      },
      trigger: 'blur'
    }
  ],
  email: [
    {
      type: 'email',
      get message() {
        return t('native.n5b51370c32')
      },
      trigger: ['blur', 'change']
    }
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      get message() {
        return t('native.n43eb65c8eb')
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
  ]
})
const formRef = ref() // 表单 Ref
const deptTree = ref() // 树形结构
const userList = ref<UserApi.UserVO[]>([]) // 用户列表

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = 'action.' + type
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = await DeptApi.getDept(id)
    } finally {
      formLoading.value = false
    }
  }
  // 获得用户列表
  userList.value = await UserApi.getSimpleUserList()
  // 获得部门树
  await getTree()
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
    const data = formData.value as unknown as DeptApi.DeptVO
    if (formType.value === 'create') {
      await DeptApi.createDept(data)
      message.success(t('common.createSuccess'))
    } else {
      await DeptApi.updateDept(data)
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
  formData.value = {
    id: undefined,
    title: '',
    parentId: undefined,
    name: undefined,
    sort: undefined,
    leaderUserId: undefined,
    phone: undefined,
    email: undefined,
    status: CommonStatusEnum.ENABLE
  }
  formRef.value?.resetFields()
}

/** 获得部门树 */
const getTree = async () => {
  deptTree.value = []
  const data = await DeptApi.getSimpleDeptList()
  // 无数据（初始化）或能访问顶层部门时才显示"顶级部门"，后端已对受限用户过滤
  if (data.length === 0 || data.some((d) => d.parentId === 0)) {
    const dept: Tree = { id: 0, name: t('native.n457ada5438'), children: [] }
    dept.children = handleTree(data)
    deptTree.value.push(dept)
  } else {
    deptTree.value = handleTree(data)
  }
}

useLocalizedFormValidation(formRef)
</script>
