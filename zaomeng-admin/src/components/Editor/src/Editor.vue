<script lang="ts" setup>
import { computed, nextTick, ref, type PropType } from 'vue'
import type { IDomEditor, IEditorConfig } from '@wangeditor-next/editor'
import { generateUUID } from '@/utils'
import { propTypes } from '@/utils/propTypes'
import { useLocaleStore } from '@/store/modules/locale'
import EditorCore from './EditorCore.vue'
defineOptions({ name: 'Editor', inheritAttrs: false })
const props = defineProps({
  editorId: propTypes.string.def(''),
  height: propTypes.oneOfType([Number, String]).def('500px'),
  editorConfig: { type: Object as PropType<Partial<IEditorConfig>>, default: () => undefined },
  readonly: propTypes.bool.def(false),
  modelValue: propTypes.string.def(''),
  directory: propTypes.string.def('editor-default')
})
const emit = defineEmits(['change', 'update:modelValue']),
  locale = useLocaleStore(),
  core = ref<InstanceType<typeof EditorCore>>()
const generatedId = 'wangEditor-' + generateUUID()
const resolvedId = computed(() => props.editorId || generatedId)
const language = computed(() => locale.getCurrentLocale.lang)
// wangEditor binds a toolbar to one editor instance. Recreate the pair together,
// keeping the HTML in the parent form's model instead of reusing a destroyed toolbar.
const getEditorRef = async (): Promise<IDomEditor> => {
  await nextTick()
  return core.value!.getEditorRef()
}
defineExpose({ getEditorRef })
</script>
<template>
  <EditorCore
    ref="core"
    :key="language + resolvedId"
    v-bind="{ ...$attrs, ...props, editorId: resolvedId }"
    @update:model-value="emit('update:modelValue', $event)"
    @change="emit('change', $event)"
  />
</template>
