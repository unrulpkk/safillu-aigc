<template>
  <header v-if="visible" class="module-page-heading">
    <div
      ><h1>{{ t(String(route.meta.title || '')) }}</h1
      ><p class="module-page-description">{{
        t(
          example
            ? 'modules.exampleDescription'
            : isSystem
              ? 'modules.systemDescription'
              : 'modules.infraDescription'
        )
      }}</p></div
    >
    <el-tag v-if="example" effect="plain" type="info">{{ t('modules.example') }}</el-tag>
  </header>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from '@/hooks/web/useI18n'
const route = useRoute(),
  { t } = useI18n()
const visible = computed(() => /^\/(system|infra)(\/|$)/.test(route.path))
const isSystem = computed(() => route.path.startsWith('/system'))
const example = computed(() => String(route.meta.moduleComponent || '').startsWith('infra/demo/'))
</script>
<style scoped>
.module-page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin: 0 0 20px;
}
.module-page-heading h1 {
  margin: 0;
  font-size: 24px;
  line-height: 1.4;
  font-weight: 650;
  color: var(--el-text-color-primary);
}
.module-page-group:empty {
  display: none;
}
.module-page-description {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-secondary);
  max-width: 850px;
}
</style>
