import axios from '@/api/axios'
import type { SysRole } from '@/types/system'

export const systemRoleApi = {
  list() {
    return axios.get<SysRole[]>('/api/v1/supply/system/roles')
  },
  save(data: Partial<SysRole>) {
    return axios.post<boolean>('/api/v1/supply/system/roles', data)
  },
  update(id: string, data: Partial<SysRole>) {
    return axios.put<boolean>(`/api/v1/supply/system/roles/${id}`, data)
  },
  remove(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/roles/${id}`)
  },
  savePermissions(roleId: string, payload: Record<string, unknown>) {
    return axios.post<boolean>(`/api/v1/supply/system/roles/${roleId}/permissions`, payload)
  },
  previewPermissions(roleId: string) {
    return axios.get<Record<string, unknown>>(`/api/v1/supply/system/roles/${roleId}/permissions/preview`)
  },
  batchPermission(payload: Record<string, unknown>) {
    return axios.post<boolean>('/api/v1/supply/system/roles/permissions/batch', payload)
  },
}
