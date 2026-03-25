export type FieldMode = 'hidden' | 'readonly' | 'editable'

export interface RoleItem {
  id: number
  roleName: string
  roleCode: string
  status: 'enabled' | 'disabled'
  remark?: string
  permissions: string[]
}

export interface UserItem {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  status: 'enabled' | 'disabled'
  roleCodes: string[]
}

export interface DictTypeItem {
  id: number
  name: string
  code: string
  status: 'enabled' | 'disabled'
  remark?: string
}

export interface DictDataItem {
  id: number
  dictCode: string
  label: string
  value: string
  sort: number
  status: 'enabled' | 'disabled'
}

export interface LogItem {
  id: number
  operator: string
  module: string
  action: string
  ip: string
  time: string
  detail: string
}

export interface LoginLogItem {
  id: number
  username: string
  ip: string
  location: string
  status: 'success' | 'failed'
  time: string
}

export interface RoleProfile {
  key: string
  label: string
  roles: string[]
  permissionCodes: string[]
  fieldPermissions: Record<string, FieldMode>
}
