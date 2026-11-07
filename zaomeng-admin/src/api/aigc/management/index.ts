import request from '@/config/axios'
export const overview = () => request.get<Record<string, number>>({ url: '/aigc/manage/overview' })
export const page = (resource: string, params: Record<string, unknown>) =>
  request.get<{ list: Record<string, any>[]; total: number }>({
    url: `/aigc/manage/${resource}/page`,
    params
  })
export const detail = (resource: string, id: string) =>
  request.get<Record<string, any>>({ url: `/aigc/manage/${resource}/${id}` })
export const credits = (id: string, data: Record<string, unknown>) =>
  request.post({ url: `/aigc/manage/workspaces/${id}/credits`, data })
export const model = (id: string, data: Record<string, unknown>) =>
  request.put({ url: `/aigc/manage/models/${id}`, data })
export const workspaceStatus = (id: string, status: string) =>
  request.put({ url: `/aigc/manage/workspaces/${id}/status`, data: { status } })
export const assetStatus = (id: string, trashed: boolean) =>
  request.put({ url: `/aigc/manage/assets/${id}/trash`, data: { trashed } })
export const assetContent = (id: string) =>
  request.download<Blob>({ url: `/aigc/manage/assets/${id}/content`, timeout: 120000 })
export const fileContent = (id: string) =>
  request.download<Blob>({ url: `/aigc/manage/files/${id}/content`, timeout: 120000 })
