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
      <el-form-item :label="t('native.n084c7f1c1b')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.neb54fbbf20')" />
      </el-form-item>
      <el-form-item :label="t('native.ndaede98817')" prop="remark">
        <el-input v-model="formData.remark" :placeholder="t('native.n1004c72456')" />
      </el-form-item>
      <el-form-item :label="t('native.n70d8ef6f0f')" prop="storage">
        <el-select
          v-model="formData.storage"
          :disabled="formData.id !== undefined"
          :placeholder="t('native.nebfebce23d')"
        >
          <el-option
            v-for="dict in getDictOptions(DICT_TYPE.INFRA_FILE_STORAGE)"
            :key="dict.value"
            :label="dict.label"
            :value="parseInt(dict.value)"
          />
        </el-select>
      </el-form-item>
      <!-- DB -->
      <!-- Local / FTP / SFTP -->
      <el-form-item
        v-if="formData.storage >= 10 && formData.storage <= 12"
        :label="t('native.na4891ce19d')"
        prop="config.basePath"
      >
        <el-input v-model="formData.config.basePath" :placeholder="t('native.n7d0d29cc5c')" />
      </el-form-item>
      <el-form-item
        v-if="formData.storage >= 11 && formData.storage <= 12"
        :label="t('native.n317c133e7a')"
        prop="config.host"
      >
        <el-input v-model="formData.config.host" :placeholder="t('native.nebfb3ad219')" />
      </el-form-item>
      <el-form-item
        v-if="formData.storage >= 11 && formData.storage <= 12"
        :label="t('native.n5c474b41c4')"
        prop="config.port"
      >
        <el-input-number
          v-model="formData.config.port"
          :min="0"
          :placeholder="t('native.nf430a11477')"
        />
      </el-form-item>
      <el-form-item
        v-if="formData.storage >= 11 && formData.storage <= 12"
        :label="t('native.n1a3f0617d6')"
        prop="config.username"
      >
        <el-input v-model="formData.config.username" :placeholder="t('native.n728a7b601c')" />
      </el-form-item>
      <el-form-item
        v-if="formData.storage >= 11 && formData.storage <= 12"
        :label="t('native.na621ab606d')"
        prop="config.password"
      >
        <el-input v-model="formData.config.password" :placeholder="t('native.n728a7b601c')" />
      </el-form-item>
      <el-form-item
        v-if="formData.storage === 11"
        :label="t('native.nebfdc49079')"
        prop="config.mode"
      >
        <el-radio-group v-model="formData.config.mode">
          <el-radio key="Active" value="Active"> {{ t('native.n2646271423') }} </el-radio>
          <el-radio key="Passive" value="Passive"> {{ t('native.n5234f1c1bf') }} </el-radio>
        </el-radio-group>
      </el-form-item>
      <!-- S3 -->
      <el-form-item
        v-if="formData.storage === 20"
        :label="t('native.n18249bbbe4')"
        prop="config.endpoint"
      >
        <el-input v-model="formData.config.endpoint" :placeholder="t('native.n8ac84a10f0')" />
      </el-form-item>
      <el-form-item
        v-if="formData.storage === 20"
        :label="t('native.n99ebc7c599')"
        prop="config.bucket"
      >
        <el-input v-model="formData.config.bucket" :placeholder="t('native.n6de45e8697')" />
      </el-form-item>
      <el-form-item v-if="formData.storage === 20" label="accessKey" prop="config.accessKey">
        <el-input v-model="formData.config.accessKey" :placeholder="t('native.n7b6ddb162f')" />
      </el-form-item>
      <el-form-item v-if="formData.storage === 20" label="accessSecret" prop="config.accessSecret">
        <el-input v-model="formData.config.accessSecret" :placeholder="t('native.ne150838f40')" />
      </el-form-item>
      <el-form-item
        v-if="formData.storage === 20"
        :label="t('native.nbd97504a9e')"
        prop="config.enablePathStyleAccess"
      >
        <el-radio-group v-model="formData.config.enablePathStyleAccess">
          <el-radio key="true" :value="true"> {{ t('native.nf4f0ead111') }} </el-radio>
          <el-radio key="false" :value="false"> {{ t('native.n7df5c456c7') }} </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        v-if="formData.storage === 20"
        :label="t('native.nf4eaf17e4c')"
        prop="config.enablePublicAccess"
      >
        <el-radio-group v-model="formData.config.enablePublicAccess">
          <el-radio key="true" :value="true"> {{ t('native.n7a73baf0ce') }} </el-radio>
          <el-radio key="false" :value="false"> {{ t('native.n94df451074') }} </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="formData.storage === 20" :label="t('native.n3382870bc4')">
        <!-- 选填，无需参数校验 -->
        <el-input v-model="formData.config.region" :placeholder="t('native.n532927594a')" />
      </el-form-item>
      <!-- 通用 -->
      <el-form-item v-if="formData.storage === 20" :label="t('native.ndfb26d4a2c')">
        <!-- 无需参数校验，所以去掉 prop -->
        <el-input v-model="formData.config.domain" :placeholder="t('native.nd804444341')" />
      </el-form-item>
      <el-form-item
        v-else-if="formData.storage"
        :label="t('native.ndfb26d4a2c')"
        prop="config.domain"
      >
        <el-input v-model="formData.config.domain" :placeholder="t('native.nd804444341')" />
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

import { DICT_TYPE, getDictOptions } from '@/utils/dict'
import * as FileConfigApi from '@/api/infra/fileConfig'
import { FormRules } from 'element-plus'

defineOptions({ name: 'InfraFileConfigForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  name: '',
  storage: 0,
  remark: '',
  config: {} as FileConfigApi.FileClientConfig
})
const formRules = reactive<FormRules>({
  name: [
    {
      required: true,
      get message() {
        return t('native.nbd79ff0197')
      },
      trigger: 'blur'
    }
  ],
  storage: [
    {
      required: true,
      get message() {
        return t('native.n53c7f5f8c5')
      },
      trigger: 'change'
    }
  ],
  config: {
    basePath: [
      {
        required: true,
        get message() {
          return t('native.n344ac4fd4a')
        },
        trigger: 'blur'
      }
    ],
    host: [
      {
        required: true,
        get message() {
          return t('native.n6edfb317ff')
        },
        trigger: 'blur'
      }
    ],
    port: [
      {
        required: true,
        get message() {
          return t('native.na3d10385b6')
        },
        trigger: 'blur'
      }
    ],
    username: [
      {
        required: true,
        get message() {
          return t('native.n390ccdec9f')
        },
        trigger: 'blur'
      }
    ],
    password: [
      {
        required: true,
        get message() {
          return t('native.n3aa7a9e035')
        },
        trigger: 'blur'
      }
    ],
    mode: [
      {
        required: true,
        get message() {
          return t('native.nffc0faf476')
        },
        trigger: 'change'
      }
    ],
    endpoint: [
      {
        required: true,
        get message() {
          return t('native.n041fd4247e')
        },
        trigger: 'blur'
      }
    ],
    bucket: [
      {
        required: true,
        get message() {
          return t('native.n0a06287a51')
        },
        trigger: 'blur'
      }
    ],
    accessKey: [
      {
        required: true,
        get message() {
          return t('native.n337647a4d5')
        },
        trigger: 'blur'
      }
    ],
    accessSecret: [
      {
        required: true,
        get message() {
          return t('native.nf8f4f662e7')
        },
        trigger: 'blur'
      }
    ],
    enablePathStyleAccess: [
      {
        required: true,
        get message() {
          return t('native.nb7e739933d')
        },
        trigger: 'change'
      }
    ],
    enablePublicAccess: [
      {
        required: true,
        get message() {
          return t('native.n7358416f36')
        },
        trigger: 'change'
      }
    ],
    domain: [
      {
        required: true,
        get message() {
          return t('native.n7260e606dc')
        },
        trigger: 'blur'
      }
    ]
  } as FormRules
})
const formRef = ref() // 表单 Ref

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
      formData.value = await FileConfigApi.getFileConfig(id)
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
    const data = formData.value as unknown as FileConfigApi.FileConfigVO
    if (formType.value === 'create') {
      await FileConfigApi.createFileConfig(data)
      message.success(t('common.createSuccess'))
    } else {
      await FileConfigApi.updateFileConfig(data)
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
    name: '',
    storage: undefined!,
    remark: '',
    config: {} as FileConfigApi.FileClientConfig
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
