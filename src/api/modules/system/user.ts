import axios from '@/api/axios'
import type { PermissionSnapshot, SysUser } from '@/types/system'

export const systemUserApi = {
  list() {
    return axios.get<SysUser[]>('/api/v1/supply/system/users')
  },
  save(data: Partial<SysUser>) {
    return axios.post<boolean>('/api/v1/supply/system/users', data)
  },
  update(id: string, data: Partial<SysUser>) {
    return axios.put<boolean>(`/api/v1/supply/system/users/${id}`, data)
  },
  remove(id: string) {
    return axios.delete<boolean>(`/api/v1/supply/system/users/${id}`)
  },
  assignRoles(userId: string, roleIds: string[]) {
    return axios.post<boolean>(`/api/v1/supply/system/users/${userId}/roles`, { roleIds })
  },
  resetPassword(userId: string) {
    return axios.post<boolean>(`/api/v1/supply/system/users/${userId}/reset-password`)
  },
  permissionSnapshot() {
    return axios.get<PermissionSnapshot>('/api/v1/supply/system/users/me/permissions')
  },
}
