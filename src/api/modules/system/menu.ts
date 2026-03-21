import axios from '@/api/axios'
import type { SysMenu } from '@/types/system'

export const systemMenuApi = {
  listTree() {
    return axios.get<SysMenu[]>('/api/v1/supply/system/menus/tree')
  },
  create(data: Partial<SysMenu>) {
    return axios.post<boolean>('/api/v1/supply/system/menus', data)
  },
  update(id: string, data: Partial<SysMenu>) {
    return axios.put<boolean>(`/api/v1/supply/system/menus/${id}`, data)
  },
  remove(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/menus/${id}`)
  },
  batchEnable(ids: string[], enabled: boolean) {
    return axios.post<boolean>('/api/v1/supply/system/menus/batch-enable', { ids, enabled })
  },
  exportConfig() {
    return axios.get('/api/v1/supply/system/menus/export', { responseType: 'blob' })
  },
  importConfig(payload: FormData) {
    return axios.post<boolean>('/api/v1/supply/system/menus/import', payload)
  },
}
