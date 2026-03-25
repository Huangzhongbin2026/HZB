import type {
  DictDataItem,
  DictTypeItem,
  LoginLogItem,
  LogItem,
  RoleItem,
  UserItem,
} from '../types/auth'

const delay = async (ms = 120) => new Promise(resolve => setTimeout(resolve, ms))

let roleSeed = 3
let userSeed = 4
let dictTypeSeed = 2
let dictDataSeed = 4

let roles: RoleItem[] = [
  {
    id: 1,
    roleName: '系统管理员',
    roleCode: 'admin',
    status: 'enabled',
    remark: '拥有全部权限',
    permissions: ['*'],
  },
  {
    id: 2,
    roleName: '运营人员',
    roleCode: 'operator',
    status: 'enabled',
    remark: '负责业务录入与维护',
    permissions: ['page:system:user', 'btn:user:query', 'btn:user:edit'],
  },
  {
    id: 3,
    roleName: '审计人员',
    roleCode: 'auditor',
    status: 'enabled',
    remark: '仅可查看日志和报表',
    permissions: ['page:system:log:operation', 'page:system:log:login', 'btn:log:query'],
  },
]

let users: UserItem[] = [
  {
    id: 1,
    username: 'admin',
    nickname: '超级管理员',
    email: 'admin@example.com',
    phone: '13800000000',
    status: 'enabled',
    roleCodes: ['admin'],
  },
  {
    id: 2,
    username: 'operator01',
    nickname: '运营A',
    email: 'operator@example.com',
    phone: '13900000001',
    status: 'enabled',
    roleCodes: ['operator'],
  },
  {
    id: 3,
    username: 'auditor01',
    nickname: '审计A',
    email: 'auditor@example.com',
    phone: '13700000002',
    status: 'enabled',
    roleCodes: ['auditor'],
  },
  {
    id: 4,
    username: 'operator02',
    nickname: '运营B',
    email: 'operator2@example.com',
    phone: '13900000003',
    status: 'disabled',
    roleCodes: ['operator'],
  },
]

let operationLogs: LogItem[] = [
  {
    id: 1,
    operator: 'admin',
    module: '用户管理',
    action: '新增',
    ip: '10.10.0.1',
    time: '2026-03-24 09:12:03',
    detail: '新增用户 operator02',
  },
  {
    id: 2,
    operator: 'operator01',
    module: '字典管理',
    action: '修改',
    ip: '10.10.0.12',
    time: '2026-03-24 10:22:11',
    detail: '修改字典项 status.disabled 文案',
  },
]

let loginLogs: LoginLogItem[] = [
  {
    id: 1,
    username: 'admin',
    ip: '10.10.0.1',
    location: '上海',
    status: 'success',
    time: '2026-03-24 08:30:10',
  },
  {
    id: 2,
    username: 'auditor01',
    ip: '10.10.0.18',
    location: '北京',
    status: 'success',
    time: '2026-03-24 09:21:58',
  },
  {
    id: 3,
    username: 'test',
    ip: '10.10.0.98',
    location: '未知',
    status: 'failed',
    time: '2026-03-24 09:25:41',
  },
]

let dictTypes: DictTypeItem[] = [
  { id: 1, name: '用户状态', code: 'user_status', status: 'enabled', remark: '启用/禁用' },
  { id: 2, name: '通用状态', code: 'common_status', status: 'enabled', remark: '系统通用状态' },
]

let dictData: DictDataItem[] = [
  { id: 1, dictCode: 'user_status', label: '启用', value: 'enabled', sort: 1, status: 'enabled' },
  { id: 2, dictCode: 'user_status', label: '禁用', value: 'disabled', sort: 2, status: 'enabled' },
  { id: 3, dictCode: 'common_status', label: '是', value: '1', sort: 1, status: 'enabled' },
  { id: 4, dictCode: 'common_status', label: '否', value: '0', sort: 2, status: 'enabled' },
]

export const listRoles = async (): Promise<RoleItem[]> => {
  await delay()
  return [...roles]
}

export const createRole = async (payload: Omit<RoleItem, 'id'>): Promise<RoleItem> => {
  await delay()
  const row = { ...payload, id: ++roleSeed }
  roles = [row, ...roles]
  return row
}

export const updateRole = async (payload: RoleItem): Promise<RoleItem> => {
  await delay()
  roles = roles.map(item => (item.id === payload.id ? payload : item))
  return payload
}

export const deleteRole = async (id: number): Promise<void> => {
  await delay()
  roles = roles.filter(item => item.id !== id)
}

export const listUsers = async (): Promise<UserItem[]> => {
  await delay()
  return [...users]
}

export const createUser = async (payload: Omit<UserItem, 'id'>): Promise<UserItem> => {
  await delay()
  const row = { ...payload, id: ++userSeed }
  users = [row, ...users]
  return row
}

export const updateUser = async (payload: UserItem): Promise<UserItem> => {
  await delay()
  users = users.map(item => (item.id === payload.id ? payload : item))
  return payload
}

export const deleteUser = async (id: number): Promise<void> => {
  await delay()
  users = users.filter(item => item.id !== id)
}

export const listOperationLogs = async (): Promise<LogItem[]> => {
  await delay()
  return [...operationLogs]
}

export const listLoginLogs = async (): Promise<LoginLogItem[]> => {
  await delay()
  return [...loginLogs]
}

export const listDictTypes = async (): Promise<DictTypeItem[]> => {
  await delay()
  return [...dictTypes]
}

export const createDictType = async (payload: Omit<DictTypeItem, 'id'>): Promise<DictTypeItem> => {
  await delay()
  const row = { ...payload, id: ++dictTypeSeed }
  dictTypes = [row, ...dictTypes]
  return row
}

export const updateDictType = async (payload: DictTypeItem): Promise<DictTypeItem> => {
  await delay()
  dictTypes = dictTypes.map(item => (item.id === payload.id ? payload : item))
  return payload
}

export const deleteDictType = async (id: number): Promise<void> => {
  await delay()
  dictTypes = dictTypes.filter(item => item.id !== id)
}

export const listDictData = async (): Promise<DictDataItem[]> => {
  await delay()
  return [...dictData]
}

export const createDictData = async (payload: Omit<DictDataItem, 'id'>): Promise<DictDataItem> => {
  await delay()
  const row = { ...payload, id: ++dictDataSeed }
  dictData = [row, ...dictData]
  return row
}

export const updateDictData = async (payload: DictDataItem): Promise<DictDataItem> => {
  await delay()
  dictData = dictData.map(item => (item.id === payload.id ? payload : item))
  return payload
}

export const deleteDictData = async (id: number): Promise<void> => {
  await delay()
  dictData = dictData.filter(item => item.id !== id)
}
