<script lang="ts" setup>
import { useLocaleStore } from '@/store/modules/locale'
import { useLocale } from '@/hooks/web/useLocale'
import { propTypes } from '@/utils/propTypes'
import { useDesign } from '@/hooks/web/useDesign'

defineOptions({ name: 'LocaleDropdown' })

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('locale-dropdown')

defineProps({
  color: propTypes.string.def('')
})

const localeStore = useLocaleStore()
const { t } = useI18n()
const { changeLocale } = useLocale()

const langMap = computed(() => localeStore.getLocaleMap)

const currentLang = computed(() => localeStore.getCurrentLocale)

const setLang = (lang: LocaleType) => {
  if (lang === unref(currentLang).lang) return
  changeLocale(lang)
}
</script>

<template>
  <ElDropdown
    :class="prefixCls"
    trigger="click"
    :popper-style="{ zIndex: 5001 }"
    @command="setLang"
  >
    <button
      type="button"
      :aria-label="t('admin.language')"
      :title="t('admin.language')"
      class="language-trigger"
      :style="{ color: color || 'inherit' }"
    >
      <Icon :class="$attrs.class" :size="18" icon="ep:connection" />
      <span>{{ langMap.find((item) => item.lang === currentLang.lang)?.name }}</span>
    </button>
    <template #dropdown>
      <ElDropdownMenu>
        <ElDropdownItem
          v-for="item in langMap"
          :key="item.lang"
          :command="item.lang"
          :disabled="item.lang === currentLang.lang"
        >
          {{ item.name }}
        </ElDropdownItem>
      </ElDropdownMenu>
    </template>
  </ElDropdown>
</template>

<style scoped>
.language-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: 0;
  background: transparent;
  font: inherit;
  cursor: pointer;
  white-space: nowrap;
}
.language-trigger:focus-visible {
  outline: 2px solid var(--el-color-primary);
  border-radius: 4px;
}
</style>
