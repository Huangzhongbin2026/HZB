import axios from '@/api/axios'
import type { SysDictItem, SysDictType } from '@/types/system'

export const systemDictApi = {
  listTypes() {
    return axios.get<SysDictType[]>('/api/v1/supply/system/dicts/types')
  },
  saveType(data: Partial<SysDictType>) {
    return axios.post<boolean>('/api/v1/supply/system/dicts/types', data)
  },
  listItems(typeCode: string, keyword = '') {
    return axios.get<SysDictItem[]>('/api/v1/supply/system/dicts/items', { params: { typeCode, keyword } })
  },
  saveItem(data: Partial<SysDictItem>) {
    return axios.post<boolean>('/api/v1/supply/system/dicts/items', data)
  },
  batchEnable(ids: string[], enabled: boolean) {
    return axios.post<boolean>('/api/v1/supply/system/dicts/items/batch-enable', { ids, enabled })
  },
  exportDict(typeCode: string) {
    return axios.get('/api/v1/supply/system/dicts/export', { params: { typeCode }, responseType: 'blob' })
  },
}
