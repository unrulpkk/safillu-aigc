<template><ExternalModule :title="t('business.apiDocs')" config-key="url.swagger" /></template>
<script setup lang="ts">
import ExternalModule from '../ExternalModule.vue'
import { useI18n } from '@/hooks/web/useI18n'
const { t } = useI18n()
defineOptions({ name: 'InfraSwagger' })
</script>
