<template>
  <header class="workbench-heading"
    ><div
      ><h1>{{ t('business.workbench') }}</h1
      ><p>{{ t('modules.intro') }}</p></div
    ><el-button :loading="loading" @click="load"
      ><Icon icon="ep:refresh" class="mr-6px" />{{ t('business.refresh') }}</el-button
    ></header
  >
  <el-alert
    v-if="failed"
    type="error"
    :closable="false"
    :title="t('modules.error')"
    class="mb-16px"
  />
  <div v-if="Object.keys(stats).length" v-loading="loading" class="overview-grid">
    <article v-for="(value, key) in stats" :key="key" class="metric-card"
      ><div class="metric-label"
        ><Icon :icon="metricIcons[key] || 'ep:data-analysis'" :size="18" /><span>{{
          key === 'users' ? t('business.users') : t(`business.resources.${key}`)
        }}</span></div
      ><strong>{{ number(value) }}</strong></article
    >
  </div>
  <section class="module-directory">
    <div class="directory-toolbar"
      ><h2>{{ t('business.navigation') }}</h2
      ><el-input
        v-model="query"
        clearable
        :placeholder="t('modules.searchModules')"
        :aria-label="t('modules.searchModules')"
        ><template #prefix><Icon icon="ep:search" /></template></el-input
    ></div>
    <div class="directory-tabs" role="tablist" :aria-label="t('business.navigation')"
      ><button
        v-for="group in [{ path: 'all', meta: { title: 'modules.all' } }, ...groups]"
        :key="group.path"
        type="button"
        role="tab"
        :aria-selected="selected === group.path"
        :class="{ active: selected === group.path }"
        @click="selected = group.path"
        >{{ t(String(group.meta?.title || '')) }}</button
      ></div
    >
    <el-empty
      v-if="!filteredGroups.length"
      :description="t(groups.length ? 'modules.noMatches' : 'modules.noAccess')"
    />
    <section v-for="group in filteredGroups" :key="group.path" class="module-group"
      ><div class="module-group-heading"
        ><Icon :icon="String(group.meta?.icon || 'ep:grid')" :size="19" /><h3>{{
          t(String(group.meta?.title || ''))
        }}</h3
        ><span>{{ number(group.entries.length) }}</span></div
      ><div class="module-grid"
        ><RouterLink
          v-for="entry in group.entries"
          :key="entry.path"
          class="module-link"
          :to="{ path: entry.path, query: entry.meta?.query, hash: entry.meta?.hash }"
          ><span class="module-icon"
            ><Icon :icon="String(entry.meta?.icon || 'ep:grid')" :size="20" /></span
          ><span class="module-link-label">{{ t(String(entry.meta?.title || '')) }}</span
          ><Icon icon="ep:arrow-right" :size="14" class="module-arrow" /></RouterLink></div
    ></section>
  </section>
</template>
<script lang="ts" setup>
import { computed, ref, onMounted } from 'vue'
import { usePermissionStore } from '@/store/modules/permission'
import { useLocaleStore } from '@/store/modules/locale'
import { useI18n } from '@/hooks/web/useI18n'
import { overview } from '@/api/aigc/management'
defineOptions({ name: 'Home' })
const { t } = useI18n(),
  permissionStore = usePermissionStore(),
  localeStore = useLocaleStore()
const stats = ref<Record<string, number>>({}),
  query = ref(''),
  selected = ref('all'),
  loading = ref(false),
  failed = ref(false)
const metricIcons: Record<string, string> = {
  users: 'ep:user',
  tasks: 'ep:video-play',
  assets: 'ep:picture',
  workspaces: 'ep:coin',
  orders: 'ep:wallet',
  models: 'ep:cpu'
}
const groups = computed(() =>
  permissionStore.getRouters.filter(
    (route) => !route.meta?.hidden && route.children?.length && route.path !== '/'
  )
)
function entries(group: any) {
  const result: any[] = []
  const walk = (node: any, parent: string) => {
    for (const child of node.children || []) {
      if (child.meta?.hidden) continue
      const path = child.path.startsWith('/')
        ? child.path
        : parent.replace(/\/$/, '') + '/' + child.path
      if (child.children?.length) walk(child, path)
      else if (child.meta?.title) result.push({ ...child, path })
    }
  }
  walk(group, group.path)
  return result
}
const normalize = (value: string) =>
  value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLocaleLowerCase()
    .trim()
const filteredGroups = computed(() =>
  groups.value
    .filter((g) => selected.value === 'all' || selected.value === g.path)
    .map((g) => ({
      ...g,
      entries: entries(g).filter(
        (e) =>
          !query.value ||
          normalize(
            t(String(e.meta?.title)) + ' ' + (e.meta?.menuName || '') + ' ' + e.path
          ).includes(normalize(query.value))
      )
    }))
    .filter((g) => g.entries.length)
)
const number = (value: number | string) =>
  new Intl.NumberFormat(localeStore.currentLocale.lang).format(Number(value))
async function load() {
  loading.value = true
  failed.value = false
  try {
    stats.value = await overview()
  } catch {
    failed.value = true
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.workbench-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
}
.workbench-heading h1 {
  font-size: 26px;
  font-weight: 650;
  margin: 0;
  line-height: 1.4;
}
.workbench-heading p {
  margin: 8px 0 0;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 24px;
}
.metric-card {
  background: var(--el-bg-color);
  padding: 20px 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
}
.metric-label {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.5;
  min-height: 40px;
}
.metric-label :deep(.v-icon) {
  color: var(--el-color-primary);
  flex-shrink: 0;
}
.metric-card strong {
  display: block;
  margin-top: 6px;
  font-size: 27px;
  line-height: 1.3;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}
.module-directory {
  padding: 24px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-light);
  border-radius: 10px;
}
.directory-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}
.directory-toolbar h2 {
  font-size: 18px;
  font-weight: 650;
  margin: 0;
}
.directory-toolbar .el-input {
  width: 300px;
  max-width: 100%;
}
.directory-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
  padding: 20px 0 16px;
  margin-bottom: 24px;
}
.directory-tabs button {
  border: 0;
  background: transparent;
  border-radius: 6px;
  padding: 8px 13px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
}
.directory-tabs button.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}
.directory-tabs button:hover {
  background: var(--el-fill-color-light);
}
.module-group + .module-group {
  margin-top: 28px;
}
.module-group-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.module-group-heading h3 {
  font-size: 15px;
  font-weight: 600;
  margin: 0;
}
.module-group-heading > span {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}
.module-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 14px;
  min-height: 72px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  color: var(--el-text-color-primary);
  text-decoration: none;
  transition:
    background-color 0.15s,
    border-color 0.15s;
}
.module-link:hover {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}
.module-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  color: var(--el-color-primary);
}
.module-link-label {
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;
  flex: 1;
}
.module-arrow {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}
.module-link:focus-visible,
.directory-tabs button:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 3px;
}
@media (max-width: 1280px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 640px) {
  .workbench-heading {
    align-items: flex-start;
  }
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
  .metric-card {
    padding: 16px 12px;
  }
  .directory-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
  .directory-toolbar .el-input {
    width: 100%;
  }
  .module-directory {
    padding: 16px;
  }
  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
