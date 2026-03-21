import type { PermissionSnapshot } from '@/types/system'

export const canViewField = (snapshot: PermissionSnapshot | null, table: string, field: string) => {
  if (!snapshot) return false
  const tableMap = snapshot.fieldPermissions[table] || {}
  const permission = tableMap[field]
  return permission === 'VISIBLE' || permission === 'EDITABLE'
}

export const canEditField = (snapshot: PermissionSnapshot | null, table: string, field: string) => {
  if (!snapshot) return false
  const tableMap = snapshot.fieldPermissions[table] || {}
  return tableMap[field] === 'EDITABLE'
}
