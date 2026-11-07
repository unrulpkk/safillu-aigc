<template>
  <ContentWrap
    ><div v-loading="loading"
      ><el-result
        :icon="failed ? 'error' : 'info'"
        :title="failed ? t('modules.error') : url ? title : t('business.unconfigured')"
        :sub-title="url ? '' : t('business.configureService')"
        ><template #extra
          ><p
            >{{ t('business.configurationKey') }}: <code>{{ configKey }}</code></p
          ><el-button v-if="failed" @click="load">{{ t('modules.retry') }}</el-button
          ><el-button v-if="url" type="primary" @click="openService">{{
            t('business.openService')
          }}</el-button
          ><el-button @click="router.push('/infra/config')">{{
            t('business.openConfig')
          }}</el-button></template
        ></el-result
      ></div
    ></ContentWrap
  >
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from '@/hooks/web/useI18n'
import * as ConfigApi from '@/api/infra/config'
const props = defineProps<{ title: string; configKey: string }>(),
  { t } = useI18n(),
  router = useRouter(),
  url = ref(''),
  loading = ref(true),
  failed = ref(false)
const openService = () => window.open(url.value, '_blank', 'noopener,noreferrer')
const load = async () => {
  loading.value = true
  failed.value = false
  url.value = ''
  try {
    const value = await ConfigApi.getConfigKey(props.configKey)
    if (value) {
      const parsed = new URL(value)
      if (['http:', 'https:'].includes(parsed.protocol)) url.value = parsed.toString()
    }
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
