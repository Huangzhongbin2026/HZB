export interface SysMenu {
  id: string
  menuName: string
  menuType: 'DIR' | 'MENU' | 'BUTTON'
  parentId: string
  routePath: string
  componentPath?: string
  permissionCode: string
  icon?: string
  sortNo: number
  isVisible: boolean
  isEnabled: boolean
}

export interface SysRole {
  id: string
  roleName: string
  roleCode: string
  deptCode?: string
  description?: string
  isEnabled: boolean
}

export interface SysUser {
  id: string
  userName: string
  account: string
  mobile: string
  feishuId: string
  email?: string
  deptCode?: string
  status: boolean
  lastLoginAt?: string
}

export interface SysDictType {
  id: string
  dictName: string
  dictCode: string
  sortNo: number
  isEnabled: boolean
}

export interface SysDictItem {
  id: string
  dictTypeId: string
  itemName: string
  itemCode: string
  itemValue: string
  sortNo: number
  isEnabled: boolean
}

export interface SysOperationLog {
  id: string
  operUser: string
  operTime: string
  operIp: string
  operType: string
  operModule: string
  operContent: string
  operResult: string
  remark?: string
}

export interface PermissionSnapshot {
  menuCodes: string[]
  buttonCodes: string[]
  dataScopes: Record<string, string>
  fieldPermissions: Record<string, Record<string, 'HIDDEN' | 'VISIBLE' | 'EDITABLE'>>
}
