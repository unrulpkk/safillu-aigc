<template>
  <ContentWrap>
    <el-form inline @submit.prevent="applyFilters">
      <el-form-item :label="t('admin.explore.title')"
        ><el-input
          v-model="search"
          :placeholder="t('admin.explore.searchPlaceholder')"
          clearable
          @keyup.enter="applyFilters"
      /></el-form-item>
      <el-form-item :label="t('admin.explore.type')"
        ><el-select
          v-model="mode"
          clearable
          :placeholder="t('admin.explore.allTypes')"
          style="width: 190px"
          ><el-option
            v-for="item in modes"
            :key="item.value"
            :label="item.label"
            :value="item.value" /></el-select
      ></el-form-item>
      <el-form-item :label="t('admin.explore.status')"
        ><el-select
          v-model="status"
          clearable
          :placeholder="t('admin.explore.allStatuses')"
          style="width: 180px"
          ><el-option :label="t('admin.explore.published')" value="PUBLISHED" /><el-option
            :label="t('admin.explore.draft')"
            value="DRAFT" /></el-select
      ></el-form-item>
      <el-form-item
        ><el-button @click="applyFilters"
          ><Icon icon="ep:search" />{{ t('admin.explore.search') }}</el-button
        ><el-button @click="reload"
          ><Icon icon="ep:refresh" />{{ t('admin.explore.refresh') }}</el-button
        ><el-button v-hasPermi="['aigc:explore:manage']" type="primary" @click="openEditor()"
          ><Icon icon="ep:plus" />{{ t('admin.explore.add') }}</el-button
        ></el-form-item
      >
    </el-form>
    <el-alert :title="t('admin.explore.publicationHint')" type="info" :closable="false" />
  </ContentWrap>
  <ContentWrap>
    <el-tabs v-model="tab" @tab-change="changeTab"
      ><el-tab-pane :label="t('admin.explore.list')" name="active" /><el-tab-pane
        :label="t('admin.explore.trash')"
        name="trash"
    /></el-tabs>
    <el-table
      v-loading="loading"
      :data="pageRows"
      row-key="id"
      :empty-text="t('admin.explore.empty')"
      border
    >
      <el-table-column prop="id" :label="t('admin.explore.id')" width="80" />
      <el-table-column :label="t('admin.explore.title')" min-width="220"
        ><template #default="{ row }"
          ><strong>{{ contentTitle(row) }}</strong
          ><div
            v-for="language in locales.filter(
              (item) => item.value !== localeStore.currentLocale.lang
            )"
            :key="language.value"
            class="translation"
            >{{ language.label }}: {{ row.titles[language.value] }}</div
          ></template
        ></el-table-column
      >
      <el-table-column :label="t('admin.explore.type')" min-width="100"
        ><template #default="{ row }">{{ modeLabel(row.mode) }}</template></el-table-column
      >
      <el-table-column :label="t('admin.explore.status')" min-width="120"
        ><template #default="{ row }"
          ><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{
            row.status === 'PUBLISHED' ? t('admin.explore.published') : t('admin.explore.draft')
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column prop="sort" :label="t('admin.explore.sort')" min-width="90" />
      <el-table-column :label="t('admin.explore.actions')" min-width="400"
        ><template #default="{ row }">
          <template v-if="tab === 'active'"
            ><el-button link type="primary" @click="preview(row)">{{
              t('admin.explore.preview')
            }}</el-button
            ><el-button
              v-hasPermi="['aigc:explore:manage']"
              link
              type="primary"
              @click="openEditor(row)"
              >{{ t('admin.explore.edit') }}</el-button
            ><el-button
              v-hasPermi="['aigc:explore:manage']"
              link
              :type="row.status === 'PUBLISHED' ? 'warning' : 'success'"
              :disabled="busyIds.has(row.id!)"
              @click="toggle(row)"
              >{{
                row.status === 'PUBLISHED'
                  ? t('admin.explore.unpublish')
                  : t('admin.explore.publish')
              }}</el-button
            ><el-button
              v-hasPermi="['aigc:explore:manage']"
              link
              type="danger"
              :disabled="busyIds.has(row.id!)"
              @click="remove(row)"
              >{{ t('admin.explore.moveToTrash') }}</el-button
            ></template
          >
          <el-button
            v-else
            v-hasPermi="['aigc:explore:manage']"
            link
            type="primary"
            :disabled="busyIds.has(row.id!)"
            @click="restore(row)"
            >{{ t('admin.explore.restore') }}</el-button
          >
        </template></el-table-column
      >
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :page-size="pageSize"
      :total="filteredRows.length"
      layout="total, prev, pager, next"
      class="mt-16px justify-end"
    />
  </ContentWrap>
  <Dialog
    v-model="editorVisible"
    :title="form.id ? t('admin.explore.editTitle') : t('admin.explore.addTitle')"
    width="900"
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    :before-close="beforeEditorClose"
  >
    <el-form ref="formRef" :model="form" label-width="auto" :disabled="saving">
      <el-tabs v-model="editorLocale">
        <el-tab-pane
          v-for="locale in locales"
          :key="locale.value"
          :label="locale.label"
          :name="locale.value"
        >
          <el-form-item
            :label="
              locale.value === 'zh-CN' ? t('admin.explore.chineseTitle') : t('admin.explore.title')
            "
            :prop="`titles.${locale.value}`"
            :rules="
              locale.value === 'zh-CN'
                ? [
                    {
                      required: true,
                      message: t('admin.explore.requiredChineseTitle'),
                      trigger: 'blur'
                    }
                  ]
                : []
            "
            ><el-input v-model="form.titles[locale.value]" maxlength="160" show-word-limit
          /></el-form-item>
          <el-form-item :label="t('admin.explore.description')"
            ><el-input
              v-model="form.descriptions[locale.value]"
              type="textarea"
              :rows="2"
              maxlength="1000"
              show-word-limit
          /></el-form-item>
        </el-tab-pane>
      </el-tabs>
      <el-alert
        :title="t('admin.explore.translationHint')"
        type="info"
        :closable="false"
        class="mb-16px"
      />
      <el-row :gutter="20"
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.creationType')"
            ><el-select v-model="form.mode" @change="modeChanged"
              ><el-option
                v-for="item in modes"
                :key="item.value"
                :label="item.label"
                :value="item.value" /></el-select></el-form-item></el-col
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.model')"
            ><el-select
              v-model="form.parameters.capabilityId"
              clearable
              :placeholder="t('admin.explore.modelPlaceholder')"
              @change="modelChanged"
              ><el-option
                v-for="c in availableCapabilities"
                :key="c.id"
                :label="c.name"
                :value="c.id" /></el-select></el-form-item></el-col
      ></el-row>
      <el-form-item
        :label="t('admin.explore.prompt')"
        prop="prompt"
        :rules="[{ required: true, message: t('admin.explore.requiredPrompt'), trigger: 'blur' }]"
        ><el-input
          v-model="form.prompt"
          type="textarea"
          :rows="4"
          maxlength="12000"
          show-word-limit
      /></el-form-item>
      <el-row :gutter="20" v-if="['image', 'video'].includes(form.mode)"
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.ratio')"
            ><el-input
              v-model="form.parameters.ratio"
              :placeholder="t('admin.explore.ratioExample')" /></el-form-item></el-col
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.resolution')"
            ><el-input
              v-model="form.parameters.resolution"
              :placeholder="t('admin.explore.resolutionExample')" /></el-form-item></el-col
      ></el-row>
      <el-row :gutter="20"
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.count')"
            ><el-input-number
              v-model="form.parameters.count"
              :min="1"
              :max="4"
              :step="1" /></el-form-item></el-col
        ><el-col :span="12" v-if="['video', 'music'].includes(form.mode)"
          ><el-form-item :label="t('admin.explore.duration')"
            ><el-input-number
              v-model="seconds"
              :min="form.mode === 'music' ? 5 : 1"
              :max="120" /></el-form-item></el-col
      ></el-row>
      <el-form-item :label="t('admin.explore.negativePrompt')"
        ><el-input
          v-model="form.parameters.negativePrompt"
          type="textarea"
          :rows="2"
          maxlength="12000"
      /></el-form-item>
      <template v-if="form.mode === 'audio'"
        ><el-form-item :label="t('admin.explore.voiceDesign')"
          ><el-input v-model="form.parameters.voiceDesign" maxlength="4000" /></el-form-item
        ><el-form-item :label="t('admin.explore.speed')"
          ><el-input-number
            v-model="form.parameters.speed"
            :min="0.5"
            :max="2"
            :step="0.1" /></el-form-item
      ></template>
      <el-form-item :label="t('admin.explore.cover')"
        ><label class="file-picker"
          ><span class="el-button">{{ t('admin.explore.chooseFile') }}</span
          ><span>{{ coverFileName || t('admin.explore.noFile') }}</span
          ><input
            type="file"
            accept="image/png,image/jpeg,image/webp"
            :aria-label="t('admin.explore.cover')"
            @change="selectFile($event, 'cover')" /></label
        ><el-tag v-if="form.coverMediaId" class="ml-10px" type="info">{{
          t('admin.explore.savedMedia', { id: form.coverMediaId })
        }}</el-tag></el-form-item
      >
      <el-form-item :label="t('admin.explore.original')"
        ><label class="file-picker"
          ><span class="el-button">{{ t('admin.explore.chooseFile') }}</span
          ><span>{{ originalFileName || t('admin.explore.noFile') }}</span
          ><input
            type="file"
            accept="image/png,image/jpeg,image/webp,video/*,audio/*"
            :aria-label="t('admin.explore.original')"
            @change="selectFile($event, 'original')" /></label
        ><el-tag v-if="form.mediaId" class="ml-10px" type="info">{{
          t('admin.explore.savedMedia', { id: form.mediaId })
        }}</el-tag></el-form-item
      >
      <el-alert :title="t('admin.explore.mediaHint')" :closable="false" class="mb-16px" />
      <el-row :gutter="20"
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.sort')"
            ><el-input-number v-model="form.sort" :min="-100000" :max="100000" /><span
              class="ml-8px text-gray-500"
              >{{ t('admin.explore.sortHint') }}</span
            ></el-form-item
          ></el-col
        ><el-col :span="12"
          ><el-form-item :label="t('admin.explore.publishStatus')"
            ><el-select v-model="form.status"
              ><el-option :label="t('admin.explore.draft')" value="DRAFT" /><el-option
                :label="t('admin.explore.published')"
                value="PUBLISHED" /></el-select></el-form-item></el-col
      ></el-row>
      <el-form-item :label="t('admin.explore.downloadSettings')"
        ><el-checkbox v-model="form.downloadEnabled">{{
          t('admin.explore.downloadEnabled')
        }}</el-checkbox></el-form-item
      >
      <el-alert v-if="saveError" :title="errorText(saveError)" type="error" :closable="false" />
    </el-form>
    <template #footer
      ><el-button @click="editorVisible = false" :disabled="saving">{{
        t('admin.explore.cancel')
      }}</el-button
      ><el-button type="primary" :loading="saving" @click="save">{{
        t('admin.explore.save')
      }}</el-button></template
    >
  </Dialog>
  <Dialog
    v-model="previewVisible"
    :title="(previewRow && contentTitle(previewRow)) || t('admin.explore.previewTitle')"
    width="850"
    @closed="clearPreview"
  >
    <div v-loading="previewLoading" class="preview-stage"
      ><img
        v-if="previewUrl && previewRow?.mode === 'image'"
        :src="previewUrl"
        :alt="t('admin.explore.previewTitle')" /><video
        v-else-if="previewUrl && previewRow?.mode === 'video'"
        :src="previewUrl"
        controls /><audio v-else-if="previewUrl" :src="previewUrl" controls
    /></div>
    <h4>{{ t('admin.explore.prompt') }}</h4
    ><p class="prompt">{{ previewRow?.prompt }}</p>
  </Dialog>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, toRaw, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { useI18n } from '@/hooks/web/useI18n'
import { useLocaleStore } from '@/store/modules/locale'
import { errorText } from '@/utils/apiError'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import * as api from '@/api/aigc/explore'
import type { ExploreContent, Capability } from '@/api/aigc/explore'
defineOptions({ name: 'AigcExploreContent' })
const { t } = useI18n()
const localeStore = useLocaleStore()
const modes = computed(() => [
  { value: 'image', label: t('admin.explore.image') },
  { value: 'video', label: t('admin.explore.video') },
  { value: 'music', label: t('admin.explore.music') },
  { value: 'audio', label: t('admin.explore.audio') }
])
const contentTitle = (row: ExploreContent) =>
  row.titles[localeStore.currentLocale.lang] || row.titles['zh-CN']
const locales = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en', label: 'English' },
  { value: 'es', label: 'Español' }
]
const rows = ref<ExploreContent[]>([]),
  capabilities = ref<Capability[]>([]),
  loading = ref(false),
  tab = ref('active')
const search = ref(''),
  mode = ref(''),
  status = ref(''),
  page = ref(1),
  pageSize = 15
const busyIds = reactive(new Set<string>())
const filteredRows = computed(() =>
  rows.value.filter(
    (r) =>
      (!mode.value || r.mode === mode.value) &&
      (!status.value || r.status === status.value) &&
      Object.values(r.titles).some((t) => t.toLowerCase().includes(search.value.toLowerCase()))
  )
)
const pageRows = computed(() =>
  filteredRows.value.slice((page.value - 1) * pageSize, page.value * pageSize)
)
const modeLabel = (value: string) => modes.value.find((m) => m.value === value)?.label || value
const applyFilters = () => {
  page.value = 1
}
let requestSerial = 0
async function reload() {
  const serial = ++requestSerial
  loading.value = true
  try {
    const result = await api.getContents(tab.value === 'trash')
    if (serial === requestSerial) {
      rows.value = result.items
      page.value = 1
    }
  } finally {
    if (serial === requestSerial) loading.value = false
  }
}
const changeTab = () => {
  rows.value = []
  reload()
}
const blank = (): ExploreContent => ({
  titles: { 'zh-CN': '', en: '', es: '' },
  descriptions: { 'zh-CN': '', en: '', es: '' },
  mode: 'image',
  prompt: '',
  parameters: { count: 1, ratio: '16:9', resolution: '2K', speed: 0.9 },
  sort: 0,
  status: 'DRAFT',
  downloadEnabled: true
})
const form = ref<ExploreContent>(blank()),
  formRef = ref<FormInstance>(),
  editorVisible = ref(false),
  editorLocale = ref('zh-CN'),
  saving = ref(false),
  saveError = ref<unknown>(null),
  seconds = ref(5)
let coverFile: File | undefined, originalFile: File | undefined
const coverFileName = ref(''),
  originalFileName = ref('')
const availableCapabilities = computed(() =>
  capabilities.value.filter((c) => c.operation.toLowerCase() === form.value.mode)
)
function openEditor(row?: ExploreContent) {
  form.value = row ? structuredClone(toRaw(row)) : blank()
  coverFile = undefined
  originalFile = undefined
  coverFileName.value = ''
  originalFileName.value = ''
  saveError.value = null
  editorLocale.value = 'zh-CN'
  seconds.value =
    form.value.parameters.videoSeconds ||
    form.value.parameters.musicSeconds ||
    (form.value.mode === 'music' ? 30 : 5)
  editorVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
    document.querySelectorAll<HTMLInputElement>('.el-dialog input[type=file]').forEach((input) => {
      input.value = ''
    })
  })
}
function modelChanged() {
  const c = capabilities.value.find((c) => c.id === form.value.parameters.capabilityId)
  if (!c) return
  for (const key of ['ratio', 'resolution'])
    if (c.defaults[key]) form.value.parameters[key] = c.defaults[key]
  form.value.parameters.count = Math.min(form.value.parameters.count || 1, c.limits.maxCount || 1)
  if (form.value.mode === 'video') seconds.value = c.limits.minSeconds || 5
}
function modeChanged() {
  form.value.parameters.capabilityId = undefined
  form.value.mediaId = null
  originalFile = undefined
  originalFileName.value = ''
  seconds.value = form.value.mode === 'music' ? 30 : 5
  form.value.parameters.count = 1
}
function selectFile(event: Event, slot: string) {
  const input = event.target as HTMLInputElement,
    file = input.files?.[0]
  if (file && file.size > 50 * 1024 * 1024) {
    ElMessage.error(t('admin.explore.fileTooLarge'))
    input.value = ''
    return
  }
  if (slot === 'cover') {
    coverFile = file
    coverFileName.value = file?.name || ''
  } else {
    originalFile = file
    originalFileName.value = file?.name || ''
  }
}
function beforeEditorClose(done: () => void) {
  if (!saving.value) done()
}
async function save() {
  if (saving.value) return
  if (!form.value.titles['zh-CN'].trim()) {
    editorLocale.value = 'zh-CN'
    ElMessage.warning(t('admin.explore.requiredChineseTitle'))
    return
  }
  if (!(await formRef.value?.validate().catch(() => false))) return
  saving.value = true
  saveError.value = null
  try {
    if (coverFile) {
      form.value.coverMediaId = (await api.uploadMedia(coverFile)).id
      coverFile = undefined
    }
    if (originalFile) {
      form.value.mediaId = (await api.uploadMedia(originalFile)).id
      originalFile = undefined
    }
    const data = structuredClone(toRaw(form.value))
    if (data.mode === 'video') data.parameters.videoSeconds = seconds.value
    if (data.mode === 'music') data.parameters.musicSeconds = seconds.value
    await api.saveContent(data)
    editorVisible.value = false
    ElMessage.success(t('admin.explore.saved'))
    await reload()
  } catch (error) {
    saveError.value = error
  } finally {
    saving.value = false
  }
}
async function run(row: ExploreContent, action: () => Promise<unknown>, text: string) {
  if (busyIds.has(row.id!)) return
  busyIds.add(row.id!)
  try {
    await action()
    ElMessage.success(text)
    await reload()
  } finally {
    busyIds.delete(row.id!)
  }
}
const toggle = (row: ExploreContent) =>
  run(
    row,
    () => api.setStatus(row),
    row.status === 'PUBLISHED' ? t('admin.explore.unpublished') : t('admin.explore.published')
  )
const restore = (row: ExploreContent) =>
  run(row, () => api.restoreContent(row), t('admin.explore.restored'))
async function remove(row: ExploreContent) {
  try {
    await ElMessageBox.confirm(
      t('admin.explore.trashConfirmation'),
      t('admin.explore.moveToTrash'),
      { type: 'warning', confirmButtonText: t('common.ok'), cancelButtonText: t('common.cancel') }
    )
    await run(row, () => api.trashContent(row), t('admin.explore.trashed'))
  } catch {}
}
const previewVisible = ref(false),
  previewLoading = ref(false),
  previewUrl = ref(''),
  previewRow = ref<ExploreContent>()
let previewSerial = 0
function clearPreview() {
  previewSerial++
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}
async function preview(row: ExploreContent) {
  clearPreview()
  const serial = ++previewSerial
  previewRow.value = row
  previewVisible.value = true
  previewLoading.value = true
  try {
    const blob = await api.previewMedia(row.id!, 'original')
    if (serial === previewSerial) previewUrl.value = URL.createObjectURL(blob)
  } finally {
    if (serial === previewSerial) previewLoading.value = false
  }
}
onMounted(async () => {
  await Promise.all([
    reload(),
    api.getCapabilities().then((result) => {
      capabilities.value = result
    })
  ])
})
watch(
  () => localeStore.currentLocale.lang,
  () => {
    nextTick(() => formRef.value?.clearValidate())
  }
)
onBeforeUnmount(clearPreview)
</script>
<style scoped>
.editor-language {
  display: inline-block;
  margin-right: 12px;
}
.file-picker {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  overflow-wrap: anywhere;
}
.file-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
  width: 100%;
}
.file-picker:focus-within {
  outline: 2px solid var(--el-color-primary);
  border-radius: 4px;
}
.translation {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-top: 4px;
}
.preview-stage {
  min-height: 120px;
  text-align: center;
}
.preview-stage img,
.preview-stage video {
  max-width: 100%;
  max-height: 55vh;
}
.preview-stage audio {
  width: 100%;
}
.prompt {
  white-space: pre-wrap;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
</style>
