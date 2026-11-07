<template>
  <ContentWrap :title="t(`business.resources.${resource}`)">
    <el-alert :title="t('business.sharedData')" type="info" :closable="false" class="mb-16px" />
    <el-form inline @submit.prevent="search">
      <el-form-item :label="t('business.search')"
        ><el-input
          v-model="query.q"
          clearable
          :placeholder="t('business.searchPlaceholder')"
          style="width: 280px"
          @keyup.enter="search"
      /></el-form-item>
      <el-form-item v-if="statusOptions.length" :label="t('business.fields.status')"
        ><el-select
          v-model="query.status"
          clearable
          :placeholder="t('business.allStatuses')"
          style="width: 200px"
          @change="search"
          ><el-option
            v-for="status in statusOptions"
            :key="status"
            :label="statusText(status)"
            :value="status" /></el-select
      ></el-form-item>
      <el-form-item
        ><el-button @click="search"><Icon icon="ep:search" />{{ t('business.search') }}</el-button
        ><el-button @click="load"
          ><Icon icon="ep:refresh" />{{ t('business.refresh') }}</el-button
        ></el-form-item
      >
    </el-form>
    <el-alert
      v-if="resource === 'orders' || resource === 'packs'"
      :title="t('business.paymentHint')"
      type="warning"
      :closable="false"
      class="mb-16px"
    />
    <el-table
      :data="rows"
      v-loading="loading"
      border
      row-key="id"
      :empty-text="t('business.empty')"
    >
      <el-table-column prop="id" :label="t('business.fields.id')" width="85" />
      <el-table-column
        v-for="field in columns"
        :key="field"
        :label="t(`business.fields.${field}`)"
        :min-width="
          field === 'prompt' ? 280 : field === 'created' || field === 'updated' ? 175 : 140
        "
        show-overflow-tooltip
      >
        <template #default="{ row }"
          ><el-tag
            v-if="field === 'status' || field === 'entryType' || field === 'environment'"
            :type="
              ['FAILED', 'DISABLED', 'TRASHED', 'REVIEW'].includes(row[field]) ? 'danger' : 'info'
            "
            >{{
              field === 'status' && row.trashed ? statusText('TRASHED') : statusText(row[field])
            }}</el-tag
          ><span v-else>{{ format(field, row[field], row) }}</span></template
        >
      </el-table-column>
      <el-table-column
        :label="t('business.actions')"
        fixed="right"
        :min-width="resource === 'workspaces' ? 300 : 200"
      >
        <template #default="{ row }">
          <el-button link type="primary" @click="showDetail(row)">{{
            t('business.detail')
          }}</el-button>
          <el-button v-if="resource === 'assets'" link type="primary" @click="preview(row)">{{
            t('business.preview')
          }}</el-button>
          <el-button
            v-if="resource === 'assets'"
            v-hasPermi="['aigc:asset:manage']"
            link
            type="warning"
            @click="trash(row)"
            >{{ row.trashed ? t('business.restore') : t('business.trash') }}</el-button
          >
          <el-button
            v-if="resource === 'workspaces'"
            v-hasPermi="['aigc:credit:manage']"
            link
            type="primary"
            @click="openCredits(row)"
            >{{ t('business.adjustCredits') }}</el-button
          >
          <el-button
            v-if="resource === 'workspaces'"
            v-hasPermi="['aigc:workspace:manage']"
            link
            type="warning"
            @click="toggleWorkspace(row)"
            >{{
              row.status === 'ENABLED' ? t('business.disable') : t('business.enable')
            }}</el-button
          >
          <el-button
            v-if="resource === 'models'"
            v-hasPermi="['aigc:model:manage']"
            link
            type="primary"
            @click="openModel(row)"
            >{{ t('business.edit') }}</el-button
          >
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="query.pageNo"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[20, 50, 100]"
      layout="total,sizes,prev,pager,next"
      class="mt-16px justify-end"
      @current-change="load"
      @size-change="search"
    />
  </ContentWrap>
  <Dialog v-model="detailVisible" :title="t('business.detail')" width="900">
    <div v-loading="detailLoading">
      <el-descriptions :column="2" border
        ><el-descriptions-item
          v-for="(value, key) in detailFields"
          :key="key"
          :label="fieldLabel(key)"
          >{{ format(key, value, detailRow) }}</el-descriptions-item
        ></el-descriptions
      >
      <template
        v-for="key in [
          'prompt',
          'description',
          'parameters',
          'pricing',
          'defaults',
          'limits',
          'parameterSchema'
        ]"
        :key="key"
        ><div v-if="detailRow[key] !== undefined" class="mt-16px"
          ><h4>{{ fieldLabel(key) }}</h4
          ><pre class="detail-json">{{
            typeof detailRow[key] === 'object'
              ? JSON.stringify(detailRow[key], null, 2)
              : detailRow[key]
          }}</pre>
        </div></template
      >
      <template v-if="detailRow.events"
        ><h4>{{ t('business.events') }}</h4
        ><el-timeline
          ><el-timeline-item
            v-for="event in detailRow.events"
            :key="event.id"
            :timestamp="format('created', event.created, event)"
            ><strong>{{ statusText(event.fromStatus) }} → {{ statusText(event.toStatus) }}</strong
            ><p>{{ taskMessage(event.message) }}</p></el-timeline-item
          ></el-timeline
        ></template
      >
      <template v-if="detailRow.tasks"
        ><h4>{{ t('business.resources.tasks') }}</h4
        ><el-table :data="detailRow.tasks"
          ><el-table-column prop="id" :label="fieldLabel('id')" /><el-table-column
            prop="prompt"
            :label="fieldLabel('prompt')"
            show-overflow-tooltip
          /><el-table-column :label="fieldLabel('status')"
            ><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column
          ></el-table
        ></template
      >
    </div>
    <template #footer
      ><el-button @click="detailVisible = false">{{ t('common.close') }}</el-button></template
    >
  </Dialog>
  <Dialog
    v-model="creditVisible"
    :title="t('business.adjustCredits')"
    width="620"
    :close-on-click-modal="false"
  >
    <el-form label-width="auto" :disabled="saving" @submit.prevent="saveCredits"
      ><el-form-item :label="fieldLabel('username')">{{ selected?.username }}</el-form-item
      ><el-form-item :label="fieldLabel('available')">{{
        format('available', selected?.available, selected || {})
      }}</el-form-item
      ><el-alert
        :title="t('business.creditHint')"
        type="info"
        :closable="false"
        class="mb-16px" /><el-form-item :label="t('business.delta')"
        ><el-input-number
          v-model="creditForm.delta"
          :min="-1000000"
          :max="1000000"
          :precision="4" /></el-form-item
      ><el-form-item :label="t('business.reason')" required
        ><el-input
          v-model="creditForm.reason"
          type="textarea"
          :rows="3"
          maxlength="400"
          show-word-limit /></el-form-item
    ></el-form>
    <template #footer
      ><el-button :disabled="saving" @click="creditVisible = false">{{
        t('common.cancel')
      }}</el-button
      ><el-button type="primary" :loading="saving" @click="saveCredits">{{
        t('common.save')
      }}</el-button></template
    >
  </Dialog>
  <Dialog
    v-model="modelVisible"
    :title="t('business.editModel')"
    width="620"
    :close-on-click-modal="false"
  >
    <el-form label-width="auto" :disabled="saving"
      ><el-form-item :label="fieldLabel('name')"
        ><el-input v-model="modelForm.name" maxlength="100" /></el-form-item
      ><el-form-item :label="fieldLabel('pointsPerUnit')"
        ><el-input-number
          v-model="modelForm.pointsPerUnit"
          :disabled="selected?.operation === 'VIDEO'"
          :min="0"
          :max="1000000"
          :precision="4" /></el-form-item
      ><el-form-item :label="fieldLabel('status')"
        ><el-select v-model="modelForm.status"
          ><el-option :label="statusText('ENABLED')" value="ENABLED" /><el-option
            :label="statusText('DISABLED')"
            value="DISABLED" /></el-select></el-form-item
      ><el-alert
        :title="
          t(selected?.operation === 'VIDEO' ? 'business.videoRateHint' : 'business.modelHint')
        "
        :closable="false"
    /></el-form>
    <template #footer
      ><el-button :disabled="saving" @click="modelVisible = false">{{
        t('common.cancel')
      }}</el-button
      ><el-button type="primary" :loading="saving" @click="saveModel">{{
        t('common.save')
      }}</el-button></template
    >
  </Dialog>
  <Dialog v-model="previewVisible" :title="t('business.preview')" width="850" @closed="clearPreview"
    ><div v-loading="previewLoading" class="media-preview"
      ><img
        v-if="previewKind === 'IMAGE' && previewUrl"
        :src="previewUrl"
        :alt="t('business.preview')" /><video
        v-else-if="previewKind === 'VIDEO' && previewUrl"
        :src="previewUrl"
        controls /><audio v-else-if="previewUrl" :src="previewUrl" controls /></div
  ></Dialog>
</template>
<script lang="ts" setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from '@/hooks/web/useI18n'
import { useLocaleStore } from '@/store/modules/locale'
import * as api from '@/api/aigc/management'
import { generateUUID } from '@/utils'
defineOptions({ name: 'AigcManagement' })
const props = defineProps<{ resource: string }>(),
  { t } = useI18n(),
  localeStore = useLocaleStore()
const schemas: Record<string, string[]> = {
  workspaces: ['username', 'nickname', 'status', 'available', 'reserved', 'consumed', 'created'],
  tasks: [
    'taskNo',
    'username',
    'operation',
    'modelName',
    'status',
    'progress',
    'prompt',
    'created'
  ],
  assets: ['title', 'username', 'kind', 'source', 'status', 'created'],
  models: [
    'name',
    'model',
    'operation',
    'platform',
    'status',
    'billingUnit',
    'pointsPerUnit',
    'configVersion'
  ],
  ledger: [
    'username',
    'workspaceId',
    'taskId',
    'entryType',
    'availableDelta',
    'reservedDelta',
    'consumedDelta',
    'availableAfter',
    'remark',
    'created'
  ],
  orders: [
    'purchaseNo',
    'username',
    'packName',
    'environment',
    'amountMinor',
    'creditPoints',
    'status',
    'credited',
    'created'
  ],
  packs: ['code', 'name', 'environment', 'amountMinor', 'creditPoints', 'enabled'],
  conversations: ['title', 'username', 'pinned', 'updated'],
  subjects: ['name', 'username', 'kind', 'status', 'version', 'created'],
  voices: ['name', 'username', 'visibility', 'gender', 'language', 'status'],
  motions: ['name', 'username', 'visibility', 'durationMs', 'status']
}
const statusSets: Record<string, string[]> = {
  workspaces: ['ENABLED', 'DISABLED'],
  tasks: [
    'PENDING',
    'QUEUED',
    'SUBMITTING',
    'RUNNING',
    'MATERIALIZING',
    'SUCCEEDED',
    'PARTIAL_SUCCEEDED',
    'FAILED',
    'CANCELED',
    'TIMED_OUT'
  ],
  assets: ['READY', 'UPLOADING', 'FAILED', 'TRASHED'],
  models: ['ENABLED', 'DISABLED'],
  ledger: ['GRANT', 'RESERVE', 'SETTLE', 'RELEASE', 'ADJUST'],
  orders: ['CREATING', 'OPEN', 'PROCESSING', 'PAID', 'EXPIRED', 'FAILED', 'REVIEW'],
  packs: ['TEST', 'LIVE'],
  subjects: ['DRAFT', 'READY', 'DISABLED'],
  voices: ['ENABLED', 'DISABLED'],
  motions: ['ENABLED', 'DISABLED']
}
const columns = computed(() => schemas[props.resource] || []),
  statusOptions = computed(() => statusSets[props.resource] || [])
const query = reactive({ pageNo: 1, pageSize: 20, q: '', status: '' }),
  rows = ref<Record<string, any>[]>([]),
  total = ref(0),
  loading = ref(false)
const knownFields = new Set([
  'id',
  ...Object.values(schemas).flat(),
  'width',
  'height',
  'durationMs',
  'description',
  'errorCode',
  'errorMessage',
  'retryOfTaskId',
  'count',
  'finished',
  'parameters',
  'pricing',
  'defaults',
  'limits',
  'parameterSchema',
  'modelId',
  'workspaceId',
  'userId',
  'payOrderId',
  'checkoutSessionId',
  'paymentIntentId',
  'payOrderStatus',
  'paid',
  'refundMinor',
  'currency',
  'creator',
  'businessKey',
  'consumedAfter',
  'reservedAfter',
  'trashed',
  'isDefault',
  'coverAssetId',
  'ageGroup',
  'billingHold'
])
const fieldLabel = (key: string) => (knownFields.has(key) ? t(`business.fields.${key}`) : key)
const statusText = (value: any) => {
  if (value === null || value === undefined || value === '') return '—'
  const key = `business.statuses.${value}`,
    label = t(key)
  return label === key ? String(value) : label
}
function taskMessage(value: string) {
  const fixed: Record<string, string> = {
    正在提交模型服务: 'submittingMessage',
    '模型正在生成，等待服务商返回结果': 'runningMessage',
    '生成完成，正在将原始文件归档到个人资产': 'archivingMessage'
  }
  if (fixed[value]) return t('business.' + fixed[value])
  let match = value?.match(/^已进入队列，预占 ([0-9.]+) 积分$/)
  if (match) return t('business.queueMessage', { points: match[1] })
  match = value?.match(/^结果已保存至个人资产，结算 ([0-9.]+) 积分$/)
  if (match) return t('business.settledMessage', { points: match[1] })
  return value
}
function format(key: string, value: any, row: Record<string, any>) {
  if (value === null || value === undefined || value === '') return '—'
  if (['created', 'updated', 'finished', 'paid'].includes(key))
    return new Intl.DateTimeFormat(localeStore.currentLocale.lang, {
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(new Date(Number(value)))
  if (key === 'errorMessage') return taskMessage(String(value))
  if (typeof value === 'boolean') return t(value ? 'business.yes' : 'business.no')
  if (['amountMinor', 'refundMinor'].includes(key)) {
    try {
      return new Intl.NumberFormat(localeStore.currentLocale.lang, {
        style: 'currency',
        currency: row.currency || 'USD'
      }).format(Number(value) / 100)
    } catch {
      return String(value)
    }
  }
  if (
    [
      'status',
      'entryType',
      'environment',
      'operation',
      'kind',
      'source',
      'visibility',
      'billingUnit'
    ].includes(key)
  )
    return statusText(value)
  if (typeof value === 'number')
    return new Intl.NumberFormat(localeStore.currentLocale.lang, {
      maximumFractionDigits: 4
    }).format(value)
  return typeof value === 'object' ? JSON.stringify(value) : String(value)
}
let serial = 0,
  disposed = false
async function load() {
  const current = ++serial
  loading.value = true
  try {
    const result = await api.page(props.resource, { ...query })
    if (current === serial && !disposed) {
      rows.value = result.list
      total.value = result.total
    }
  } finally {
    if (current === serial) loading.value = false
  }
}
const search = () => {
  query.pageNo = 1
  load()
}
const detailVisible = ref(false),
  detailLoading = ref(false),
  detailRow = ref<Record<string, any>>({})
const detailFields = computed(() =>
  Object.fromEntries(
    Object.entries(detailRow.value).filter(
      ([key, value]) =>
        ![
          'prompt',
          'description',
          'parameters',
          'pricing',
          'defaults',
          'limits',
          'parameterSchema',
          'events',
          'tasks'
        ].includes(key) && typeof value !== 'object'
    )
  )
)
async function showDetail(row: Record<string, any>) {
  detailRow.value = {}
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailRow.value = await api.detail(props.resource, row.id)
  } finally {
    detailLoading.value = false
  }
}
const selected = ref<Record<string, any>>(),
  creditVisible = ref(false),
  modelVisible = ref(false),
  saving = ref(false)
const creditForm = reactive({ delta: 100, reason: '', idempotencyKey: '' }),
  modelForm = reactive({ name: '', pointsPerUnit: 0, status: 'ENABLED', configVersion: 0 })
function openCredits(row: Record<string, any>) {
  selected.value = row
  creditForm.delta = 100
  creditForm.reason = ''
  creditForm.idempotencyKey = generateUUID()
  creditVisible.value = true
}
async function saveCredits() {
  if (saving.value) return
  if (!creditForm.reason.trim() || !creditForm.delta) {
    ElMessage.warning(t('business.creditValidation'))
    return
  }
  saving.value = true
  try {
    await api.credits(selected.value!.id, { ...creditForm })
    creditVisible.value = false
    ElMessage.success(t('business.saved'))
    await load()
  } finally {
    saving.value = false
  }
}
function openModel(row: Record<string, any>) {
  selected.value = row
  Object.assign(modelForm, {
    name: row.name,
    pointsPerUnit: Number(row.pointsPerUnit),
    status: row.status,
    configVersion: row.configVersion
  })
  modelVisible.value = true
}
async function saveModel() {
  if (saving.value) return
  if (!modelForm.name.trim()) {
    ElMessage.warning(t('business.modelValidation'))
    return
  }
  saving.value = true
  try {
    await api.model(selected.value!.id, { ...modelForm })
    modelVisible.value = false
    ElMessage.success(t('business.saved'))
    await load()
  } finally {
    saving.value = false
  }
}
async function confirm(message: string) {
  try {
    await ElMessageBox.confirm(message, t('common.confirmTitle'), {
      confirmButtonText: t('common.ok'),
      cancelButtonText: t('common.cancel'),
      type: 'warning'
    })
    return true
  } catch {
    return false
  }
}
async function toggleWorkspace(row: Record<string, any>) {
  if (!(await confirm(t('business.workspaceConfirm')))) return
  await api.workspaceStatus(row.id, row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED')
  ElMessage.success(t('business.saved'))
  await load()
}
async function trash(row: Record<string, any>) {
  if (!(await confirm(t(row.trashed ? 'business.restoreConfirm' : 'business.trashConfirm')))) return
  await api.assetStatus(row.id, !row.trashed)
  ElMessage.success(t('business.saved'))
  await load()
}
const previewVisible = ref(false),
  previewLoading = ref(false),
  previewUrl = ref(''),
  previewKind = ref('')
let previewSerial = 0
function clearPreview() {
  previewSerial++
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}
async function preview(row: Record<string, any>) {
  clearPreview()
  const current = ++previewSerial
  previewKind.value = row.kind
  previewVisible.value = true
  previewLoading.value = true
  try {
    const blob = await api.assetContent(row.id)
    if (current === previewSerial && !disposed) previewUrl.value = URL.createObjectURL(blob)
  } finally {
    if (current === previewSerial) previewLoading.value = false
  }
}
let poll: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  load()
  if (props.resource === 'tasks')
    poll = setInterval(() => {
      if (!loading.value && !detailVisible.value && document.visibilityState === 'visible') load()
    }, 8000)
})
onBeforeUnmount(() => {
  disposed = true
  serial++
  if (poll) clearInterval(poll)
  clearPreview()
})
</script>
<style scoped>
.detail-json {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  background: var(--el-fill-color-light);
  padding: 12px;
  max-height: 400px;
  overflow: auto;
  line-height: 1.6;
}
.media-preview {
  min-height: 120px;
  text-align: center;
}
.media-preview img,
.media-preview video {
  max-width: 100%;
  max-height: 60vh;
}
.media-preview audio {
  width: 100%;
}
</style>
