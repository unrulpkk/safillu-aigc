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
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('native.n71574c9a99')" prop="nickname">
            <el-input v-model="formData.nickname" :placeholder="t('native.n0cb61af3a6')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('native.n0fde3a6c63')" prop="deptId">
            <el-tree-select
              v-model="formData.deptId"
              :data="deptList"
              :props="defaultProps"
              check-strictly
              node-key="id"
              :placeholder="t('native.n8314c6adaa')"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('native.nf8c14d85b3')" prop="mobile">
            <el-input
              v-model="formData.mobile"
              maxlength="11"
              :placeholder="t('native.n2da3e67d89')"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('native.n73075237fd')" prop="email">
            <el-input
              v-model="formData.email"
              maxlength="50"
              :placeholder="t('native.n59c9ed95eb')"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item
            v-if="formData.id === undefined"
            :label="t('native.n6bb5d777ac')"
            prop="username"
          >
            <el-input v-model="formData.username" :placeholder="t('native.nd688333a2a')" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item
            v-if="formData.id === undefined"
            :label="t('native.nc43056dbe4')"
            prop="password"
          >
            <el-input
              v-model="formData.password"
              :placeholder="t('native.n7a6620b787')"
              show-password
              type="password"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="12">
          <el-form-item :label="t('native.n38d398726d')">
            <el-select v-model="formData.sex" :placeholder="t('native.nda590a8fe3')">
              <el-option
                v-for="dict in getIntDictOptions(DICT_TYPE.SYSTEM_USER_SEX)"
                :key="dict.value"
                :label="dict.label"
                :value="dict.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item :label="t('native.n39a0ae1ee3')">
            <el-select v-model="formData.postIds" multiple :placeholder="t('native.nda590a8fe3')">
              <el-option
                v-for="item in postList"
                :key="item.id"
                :label="item.name"
                :value="item.id!"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="24">
          <el-form-item :label="t('native.ndaede98817')">
            <el-input
              v-model="formData.remark"
              :placeholder="t('native.n606ae522e6')"
              type="textarea"
            />
          </el-form-item>
        </el-col>
      </el-row>
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
import * as PostApi from '@/api/system/post'
import * as DeptApi from '@/api/system/dept'
import * as UserApi from '@/api/system/user'
import { FormRules } from 'element-plus'

defineOptions({ name: 'SystemUserForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  nickname: '',
  deptId: '',
  mobile: '',
  email: '',
  id: undefined,
  username: '',
  password: '',
  sex: undefined,
  postIds: [],
  remark: '',
  status: CommonStatusEnum.ENABLE,
  roleIds: []
})
const formRules = reactive<FormRules>({
  username: [
    {
      required: true,
      get message() {
        return t('native.nafaee50aed')
      },
      trigger: 'blur'
    }
  ],
  nickname: [
    {
      required: true,
      get message() {
        return t('native.ndd2bfb9612')
      },
      trigger: 'blur'
    }
  ],
  password: [
    {
      required: true,
      get message() {
        return t('native.n3c0689dc32')
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
  mobile: [
    {
      pattern: /^1[3-9]\d{9}$/,
      get message() {
        return t('native.n43eb65c8eb')
      },
      trigger: 'blur'
    }
  ]
})
const formRef = ref() // 表单 Ref
const deptList = ref<Tree[]>([]) // 树形结构
const postList = ref([] as PostApi.PostVO[]) // 岗位列表

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
      formData.value = await UserApi.getUser(id)
    } finally {
      formLoading.value = false
    }
  }
  // 加载部门树
  deptList.value = handleTree(await DeptApi.getSimpleDeptList())
  // 加载岗位列表
  postList.value = await PostApi.getSimplePostList()
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
    const data = formData.value as unknown as UserApi.UserVO
    if (formType.value === 'create') {
      await UserApi.createUser(data)
      message.success(t('common.createSuccess'))
    } else {
      await UserApi.updateUser(data)
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
    nickname: '',
    deptId: '',
    mobile: '',
    email: '',
    id: undefined,
    username: '',
    password: '',
    sex: undefined,
    postIds: [],
    remark: '',
    status: CommonStatusEnum.ENABLE,
    roleIds: []
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
