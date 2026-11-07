<template>
  <ExternalModule :title="t('business.databaseMonitor')" config-key="url.druid" />
</template>
<script setup lang="ts">
import ExternalModule from '../ExternalModule.vue'
import { useI18n } from '@/hooks/web/useI18n'
const { t } = useI18n()
defineOptions({ name: 'InfraDruid' })
</script>
