<template>
  <Dialog v-model="dialogVisible" :title="t(dialogTitle)" max-height="500px" scroll>
    <el-form
      :validate-on-rule-change="false"
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="auto"
    >
      <el-form-item :label="t('native.n0f8836ccbd')" prop="secret">
        <el-input v-model="formData.clientId" :placeholder="t('native.n1dbee61142')" />
      </el-form-item>
      <el-form-item :label="t('native.n7183484f9a')" prop="secret">
        <el-input v-model="formData.secret" :placeholder="t('native.nd69797e5e1')" />
      </el-form-item>
      <el-form-item :label="t('native.nc131777b2f')" prop="name">
        <el-input v-model="formData.name" :placeholder="t('native.n37b3ad1ff3')" />
      </el-form-item>
      <el-form-item :label="t('native.nc96905bc77')">
        <UploadImg v-model="formData.logo" :limit="1" />
      </el-form-item>
      <el-form-item :label="t('native.nf9aa3562b6')">
        <el-input
          v-model="formData.description"
          :placeholder="t('native.n37b3ad1ff3')"
          type="textarea"
        />
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
      <el-form-item :label="t('native.n699b4727f3')" prop="accessTokenValiditySeconds">
        <el-input-number
          v-model="formData.accessTokenValiditySeconds"
          :placeholder="t('native.n161a84634f')"
        />
      </el-form-item>
      <el-form-item :label="t('native.n038979ee16')" prop="refreshTokenValiditySeconds">
        <el-input-number
          v-model="formData.refreshTokenValiditySeconds"
          :placeholder="t('native.n161a84634f')"
        />
      </el-form-item>
      <el-form-item :label="t('native.n517075aaa1')" prop="authorizedGrantTypes">
        <el-select
          v-model="formData.authorizedGrantTypes"
          filterable
          multiple
          :placeholder="t('native.naa33ff5881')"
          class="w-full"
        >
          <el-option
            v-for="dict in getDictOptions(DICT_TYPE.SYSTEM_OAUTH2_GRANT_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.nb8cd22094f')" prop="scopes">
        <el-input-tag
          v-model="formData.scopes"
          :placeholder="t('native.n0811f9cf93')"
          class="w-full"
        />
      </el-form-item>
      <el-form-item :label="t('native.n0c422b7f50')" prop="autoApproveScopes">
        <el-select
          v-model="formData.autoApproveScopes"
          filterable
          multiple
          :placeholder="t('native.n757e925b93')"
          class="w-full"
        >
          <el-option v-for="scope in formData.scopes" :key="scope" :label="scope" :value="scope" />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('native.n933a1c3e10')" prop="redirectUris">
        <el-input-tag
          v-model="formData.redirectUris"
          :placeholder="t('native.n966cf4e9e7')"
          class="w-full"
        />
      </el-form-item>
      <el-form-item :label="t('native.n978cbca626')" prop="authorities">
        <el-input-tag
          v-model="formData.authorities"
          :placeholder="t('native.n93ec77464b')"
          class="w-full"
        />
      </el-form-item>
      <el-form-item :label="t('native.ndaa6d02a55')" prop="resourceIds">
        <el-input-tag
          v-model="formData.resourceIds"
          :placeholder="t('native.n6a8d72698d')"
          class="w-full"
        />
      </el-form-item>
      <el-form-item :label="t('native.n5c1d9e0aaf')" prop="additionalInformation">
        <el-input
          v-model="formData.additionalInformation"
          :placeholder="t('native.n0bd5b9f44f')"
          type="textarea"
        />
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

import { DICT_TYPE, getDictOptions, getIntDictOptions } from '@/utils/dict'
import { CommonStatusEnum } from '@/utils/constants'
import * as ClientApi from '@/api/system/oauth2/client'

defineOptions({ name: 'SystemOAuth2ClientForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改
const formData = ref({
  id: undefined,
  clientId: undefined,
  secret: undefined,
  name: undefined,
  logo: undefined,
  description: undefined,
  status: CommonStatusEnum.ENABLE,
  accessTokenValiditySeconds: 30 * 60,
  refreshTokenValiditySeconds: 30 * 24 * 60,
  redirectUris: [],
  authorizedGrantTypes: [],
  scopes: [],
  autoApproveScopes: [],
  authorities: [],
  resourceIds: [],
  additionalInformation: undefined
})
const formRules = reactive({
  clientId: [
    {
      required: true,
      get message() {
        return t('native.n45c092359a')
      },
      trigger: 'blur'
    }
  ],
  secret: [
    {
      required: true,
      get message() {
        return t('native.n2e69af2a14')
      },
      trigger: 'blur'
    }
  ],
  name: [
    {
      required: true,
      get message() {
        return t('native.n90f2b1c826')
      },
      trigger: 'blur'
    }
  ],
  logo: [
    {
      required: true,
      get message() {
        return t('native.nb365be58a8')
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
  accessTokenValiditySeconds: [
    {
      required: true,
      get message() {
        return t('native.n74bf60847f')
      },
      trigger: 'blur'
    }
  ],
  refreshTokenValiditySeconds: [
    {
      required: true,
      get message() {
        return t('native.n367356b5c6')
      },
      trigger: 'blur'
    }
  ],
  redirectUris: [
    {
      required: true,
      get message() {
        return t('native.nbf98bdd23e')
      },
      trigger: 'blur'
    }
  ],
  authorizedGrantTypes: [
    {
      required: true,
      get message() {
        return t('native.nb32a65434c')
      },
      trigger: 'blur'
    }
  ]
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
      formData.value = await ClientApi.getOAuth2Client(id)
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
    const data = formData.value as unknown as ClientApi.OAuth2ClientVO
    if (formType.value === 'create') {
      await ClientApi.createOAuth2Client(data)
      message.success(t('common.createSuccess'))
    } else {
      await ClientApi.updateOAuth2Client(data)
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
    clientId: undefined,
    secret: undefined,
    name: undefined,
    logo: undefined,
    description: undefined,
    status: CommonStatusEnum.ENABLE,
    accessTokenValiditySeconds: 30 * 60,
    refreshTokenValiditySeconds: 30 * 24 * 60,
    redirectUris: [],
    authorizedGrantTypes: [],
    scopes: [],
    autoApproveScopes: [],
    authorities: [],
    resourceIds: [],
    additionalInformation: undefined
  }
  formRef.value?.resetFields()
}

useLocalizedFormValidation(formRef)
</script>
