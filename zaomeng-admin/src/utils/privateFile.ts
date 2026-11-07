import { computed, onScopeDispose, ref, watch } from 'vue'
import request from '@/config/axios'

export const isStoredFileUrl = (value: string) =>
  /^(https?:\/\/|\/admin-api\/infra\/file\/)/i.test(value)
export const storedFileContent = (url: string) =>
  request.download<Blob>({ url: '/aigc/manage/files/resolve', params: { url }, timeout: 120000 })

// Display URLs only. Persist the original file reference, never a temporary Blob URL.
export function usePrivateFiles(source: () => string[]) {
  const resolved = ref<Record<string, string>>({})
  let disposed = false
  const pending = new Set<string>()
  const owned = new Set<string>()
  const wanted = () => new Set(source().filter(Boolean))
  watch(
    () => JSON.stringify([...wanted()].sort()),
    () => {
      const current = wanted()
      for (const [key, value] of Object.entries(resolved.value))
        if (!current.has(key)) {
          if (owned.delete(value)) URL.revokeObjectURL(value)
          delete resolved.value[key]
        }
      for (const raw of current) {
        if (!isStoredFileUrl(raw)) {
          resolved.value[raw] = raw
          continue
        }
        if (resolved.value[raw] || pending.has(raw)) continue
        pending.add(raw)
        storedFileContent(raw)
          .then((blob) => {
            if (disposed || !wanted().has(raw)) return
            const objectUrl = URL.createObjectURL(blob)
            owned.add(objectUrl)
            resolved.value[raw] = objectUrl
          })
          .catch(() => {
            /* The request layer displays the localized error. */
          })
          .finally(() => pending.delete(raw))
      }
    },
    { immediate: true }
  )
  onScopeDispose(() => {
    disposed = true
    owned.forEach((url) => URL.revokeObjectURL(url))
    owned.clear()
  })
  return (raw: string) => resolved.value[raw] || ''
}
export function usePrivateFile(source: () => string) {
  const resolve = usePrivateFiles(() => [source()])
  return computed(() => resolve(source()))
}
export async function downloadStoredFile(url = '', name = 'file') {
  if (!url) return
  if (!isStoredFileUrl(url)) {
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    return
  }
  const blob = await storedFileContent(url),
    objectUrl = URL.createObjectURL(blob),
    anchor = document.createElement('a')
  anchor.href = objectUrl
  anchor.download = name
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
}
