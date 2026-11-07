import request from '@/config/axios'
import { service } from '@/config/axios/service'

export interface ExploreContent {
  id?: string
  titles: Record<string, string>
  descriptions: Record<string, string>
  mode: string
  prompt: string
  parameters: Record<string, any>
  coverMediaId?: string | null
  mediaId?: string | null
  sort: number
  status: string
  revision?: number
  downloadEnabled: boolean
}
export interface Capability {
  id: string
  name: string
  operation: string
  defaults: Record<string, any>
  limits: Record<string, any>
}
const base = '/aigc/explore/manage'
export const getContents = (trash = false) =>
  request.get<{ items: ExploreContent[] }>({ url: base, params: { trash } })
export const getCapabilities = () => request.get<Capability[]>({ url: base + '/capabilities' })
export const saveContent = (data: ExploreContent) =>
  data.id ? request.put({ url: `${base}/${data.id}`, data }) : request.post({ url: base, data })
export const setStatus = async (row: ExploreContent) =>
  (
    await service({
      url: `${base}/${row.id}/status`,
      method: 'PATCH',
      data: { revision: row.revision, status: row.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED' }
    })
  ).data
export const trashContent = (row: ExploreContent) =>
  request.delete({ url: `${base}/${row.id}`, params: { revision: row.revision } })
export const restoreContent = (row: ExploreContent) =>
  request.post({ url: `${base}/${row.id}/restore`, params: { revision: row.revision } })
export const uploadMedia = async (file: File) => {
  const data = new FormData()
  data.append('file', file)
  return request.post<{ id: string; name: string }>({
    url: base + '/media',
    data,
    headersType: 'multipart/form-data',
    timeout: 120000
  })
}
export const previewMedia = (id: string, slot: 'cover' | 'original') =>
  request.download<Blob>({ url: `${base}/${id}/media/${slot}`, timeout: 120000 })
