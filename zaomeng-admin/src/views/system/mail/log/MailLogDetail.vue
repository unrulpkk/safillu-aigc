<template>
  <Dialog
    v-model="dialogVisible"
    :max-height="500"
    :scroll="true"
    :title="t('native.n979a332955')"
    width="800"
  >
    <el-descriptions v-loading="detailLoading" :column="1" border>
      <el-descriptions-item :label="t('native.n1a893a7d52')" min-width="120">
        {{ detailData.id }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n9cf9c2b3f9')">
        {{ accountList.find((account) => account.id === detailData.accountId)?.mail }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n3e24ad2624')">
        {{ detailData.templateId }} | {{ detailData.templateCode }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nf8c612be37')">
        {{ detailData.templateNickname }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n64931b0697')">
        <span v-if="detailData.userType && detailData.userId">
          <dict-tag :type="DICT_TYPE.USER_TYPE" :value="detailData.userType" />
          ({{ detailData.userId }})
        </span>
        <span v-else> {{ t('native.n484d556139') }} </span>
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n44054c244b')">
        <div>
          <div v-if="detailData.toMails && detailData.toMails.length > 0">
            {{ t('native.n1422513d1a') }}
            <span v-for="(mail, index) in detailData.toMails" :key="mail">
              {{ mail }}<span v-if="Number(index) < detailData.toMails.length - 1">、</span>
            </span>
          </div>
          <div v-if="detailData.ccMails && detailData.ccMails.length > 0">
            {{ t('native.n96f54ef6e9') }}
            <span v-for="(mail, index) in detailData.ccMails" :key="mail">
              {{ mail }}<span v-if="Number(index) < detailData.ccMails.length - 1">、</span>
            </span>
          </div>
          <div v-if="detailData.bccMails && detailData.bccMails.length > 0">
            {{ t('native.ne628ba3121') }}
            <span v-for="(mail, index) in detailData.bccMails" :key="mail">
              {{ mail }}<span v-if="Number(index) < detailData.bccMails.length - 1">、</span>
            </span>
          </div>
        </div>
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n80a0919046')">
        {{ detailData.templateTitle }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n31f379a753')">
        <div v-dompurify-html="detailData.templateContent"></div>
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n0d808f4222')">
        {{ detailData.templateParams }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n07ec86e0f1')">
        {{ formatDate(detailData.createTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.ne4cda07d3f')">
        <dict-tag :type="DICT_TYPE.SYSTEM_MAIL_SEND_STATUS" :value="detailData.sendStatus" />
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n4b474d3771')">
        {{ formatDate(detailData.sendTime) }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.nd55a4bd30d')">
        {{ detailData.sendMessageId }}
      </el-descriptions-item>
      <el-descriptions-item :label="t('native.n8432a1ffc2')">
        {{ detailData.sendException }}
      </el-descriptions-item>
    </el-descriptions>
  </Dialog>
</template>
<script lang="ts" setup>
const { t } = useI18n()

import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as MailLogApi from '@/api/system/mail/log'
import * as MailAccountApi from '@/api/system/mail/account'

defineOptions({ name: 'SystemMailLogDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单的加载中
const detailData = ref({} as MailLogApi.MailLogVO) // 详情数据
const accountList = ref<MailAccountApi.MailAccountVO[]>([]) // 邮箱账号列表

/** 打开弹窗 */
const open = async (id: number) => {
  dialogVisible.value = true
  // 设置数据
  detailLoading.value = true
  try {
    detailData.value = {} as MailLogApi.MailLogVO
    detailData.value = await MailLogApi.getMailLog(id)
    accountList.value = await MailAccountApi.getSimpleMailAccountList()
  } finally {
    detailLoading.value = false
  }
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
