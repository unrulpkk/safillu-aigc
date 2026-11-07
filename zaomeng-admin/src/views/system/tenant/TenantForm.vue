<template>
  <Dialog v-model="dialogVisible" :title="t(dialogTitle)" width="50%">
    <el-form
      :validate-on-rule-change="false"
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item :label="t('native.na836a5d419')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n9850261ed4')" />
      </el-form-item>
      <el-form-item :label="t('native.n7291d85e25')" prop="packageId">
        <el-select v-model="formData.packageId" clearable :placeholder="t('native.n9b519d669b')">
          <el-option
            v-for="item in packageList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.n3303b56982')" prop="contactName">
        <el-input v-model="formData.contactName" :placeholder="t('native.nb1814fc06f')" />
      </el-form-item>
      <el-form-item :label="t('native.n52cf7b0464')" prop="contactMobile">
        <el-input v-model="formData.contactMobile" :placeholder="t('native.n6f4fc2aed4')" />
      </el-form-item>
      <el-form-item
        v-if="formData.id === undefined"
        :label="t('native.n6bb5d777ac')"
        prop="username"
      >
        <el-input v-model="formData.username" :placeholder="t('native.nd688333a2a')" />
      </el-form-item>
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
      <el-form-item :label="t('native.n55f721d677')" prop="accountCount">
        <el-input-number
          v-model="formData.accountCount"
          :min="0"
          controls-position="right"
          :placeholder="t('native.n5e3fc812a0')"
        />
      </el-form-item>
      <el-form-item :label="t('native.na8e5f17166')" prop="expireTime">
        <el-date-picker
          v-model="formData.expireTime"
          clearable
          :placeholder="t('native.n2fc68f21c2')"
          type="date"
          value-format="x"
        />
      </el-form-item>
      <el-form-item :label="t('native.ndd799f925f')" prop="websites">
        <el-input-tag
          v-model="formData.websites"
          :placeholder="t('native.n31e15a3b44')"
          class="w-full"
        />
      </el-form-item>
      <el-form-item :label="t('native.n010163742b')" prop="status">
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
import * as TenantApi from '@/api/system/tenant'
import { CommonStatusEnum } from '@/utils/constants'
import * as TenantPackageApi from '@/api/system/tenantPackage'

defineOptions({ name: 'SystemTenantForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗
const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: undefined,
  packageId: undefined,
  contactName: undefined,
  contactMobile: undefined,
  accountCount: undefined,
  expireTime: undefined,
  websites: [],
  status: CommonStatusEnum.ENABLE,
  // 新增专属
  username: undefined,
  password: undefined
})
const formRules = reactive({
  name: [
    {
      required: true,
      get message() {
        return t('native.nafc83e9837')
      },
      trigger: 'blur'
    }
  ],
  packageId: [
    {
      required: true,
      get message() {
        return t('native.n406319ac34')
      },
      trigger: 'blur'
    }
  ],
  contactName: [
    {
      required: true,
      get message() {
        return t('native.n8b8892d126')
      },
      trigger: 'blur'
    }
  ],
  status: [
    {
      required: true,
      get message() {
        return t('native.n3ce6baa2c6')
      },
      trigger: 'blur'
    }
  ],
  accountCount: [
    {
      required: true,
      get message() {
        return t('native.n8c9997a0f4')
      },
      trigger: 'blur'
    }
  ],
  expireTime: [
    {
      required: true,
      get message() {
        return t('native.n81f18d591a')
      },
      trigger: 'blur'
    }
  ],
  username: [
    {
      required: true,
      get message() {
        return t('native.nafaee50aed')
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
  ]
})
const formRef = ref() // 表单 Ref
const packageList = ref([] as TenantPackageApi.TenantPackageVO[]) // 租户套餐

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
      formData.value = await TenantApi.getTenant(id)
    } finally {
      formLoading.value = false
    }
  }
  // 加载套餐列表
  packageList.value = await TenantPackageApi.getTenantPackageList()
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
    const data = formData.value as unknown as TenantApi.TenantVO
    if (formType.value === 'create') {
      await TenantApi.createTenant(data)
      message.success(t('common.createSuccess'))
    } else {
      await TenantApi.updateTenant(data)
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
    name: undefined,
    packageId: undefined,
    contactName: undefined,
    contactMobile: undefined,
    accountCount: undefined,
    expireTime: undefined,
    websites: [],
    status: CommonStatusEnum.ENABLE,
    username: undefined,
    password: undefined
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
