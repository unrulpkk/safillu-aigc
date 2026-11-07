<template>
  <Dialog v-model="dialogVisible" :title="t('native.nbb5caa9c59')">
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :action="uploadUrl"
      :auto-upload="false"
      :data="data"
      :disabled="formLoading"
      :limit="1"
      :on-change="handleFileChange"
      :on-error="submitFormError"
      :on-exceed="handleExceed"
      :on-progress="handleProgress"
      :on-success="submitFormSuccess"
      :http-request="httpRequest"
      accept=".jpg, .png, .gif"
      drag
    >
      <i class="el-icon-upload"></i>
      <div class="el-upload__text">
        {{ t('native.n0cffcf78ad') }} <em> {{ t('native.n61058d5170') }} </em></div
      >
      <template #tip>
        <div class="el-upload__tip" style="color: red"> {{ t('native.nf2aca304c4') }} </div>
      </template>
    </el-upload>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitFileForm">
        {{ t('native.n8d6e0c32e5') }}
      </el-button>
      <el-button @click="dialogVisible = false"> {{ t('native.n1f4d617f83') }} </el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import { useUpload } from '@/components/UploadFile/src/useUpload'
import { UploadFile, UploadProgressEvent } from 'element-plus/es/components/upload/src/upload'

defineOptions({ name: 'InfraFileForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中
const fileList = ref([]) // 文件列表
const data = ref({ path: '' })
const uploadRef = ref()

const { uploadUrl, httpRequest } = useUpload()

/** 打开弹窗 */
const open = async () => {
  dialogVisible.value = true
  resetForm()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 处理上传的文件发生变化 */
const handleFileChange = (file: UploadFile) => {
  data.value.path = file.name
}

/** 处理文件上传进度显示 */
const handleProgress = (upEvt: UploadProgressEvent, file: UploadFile) => {
  file.percentage = upEvt.percent
}

/** 提交表单 */
const submitFileForm = () => {
  if (fileList.value.length == 0) {
    message.error(t('native.n1e16294a32'))
    return
  }
  formLoading.value = true
  unref(uploadRef)?.submit()
}

/** 文件上传成功处理 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitFormSuccess = () => {
  // 清理
  dialogVisible.value = false
  formLoading.value = false
  unref(uploadRef)?.clearFiles()
  // 提示成功，并刷新
  message.success(t('common.createSuccess'))
  emit('success')
}

/** 上传错误提示 */
const submitFormError = (): void => {
  message.error(t('native.n4c03a534b2'))
  formLoading.value = false
}

/** 重置表单 */
const resetForm = () => {
  // 重置上传状态和文件
  formLoading.value = false
  uploadRef.value?.clearFiles()
}

/** 文件数超出提示 */
const handleExceed = (): void => {
  message.error(t('native.n51bc7c39c3'))
}
</script>
