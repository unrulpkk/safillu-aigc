<template>
  <ExternalModule :title="t('business.serverMonitor')" config-key="url.spring-boot-admin" />
</template>
<script setup lang="ts">
import ExternalModule from '../ExternalModule.vue'
import { useI18n } from '@/hooks/web/useI18n'
const { t } = useI18n()
defineOptions({ name: 'InfraServer' })
</script>
