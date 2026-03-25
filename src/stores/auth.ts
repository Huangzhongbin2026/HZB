import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { FieldMode, RoleProfile } from '../types/auth'

const allPermissions = [
  'page:system:user',
  'page:system:role',
  'page:system:permission',
  'page:system:log:operation',
  'page:system:log:login',
  'page:system:dict:type',
  'page:system:dict:data',
  'btn:user:query',
  'btn:user:add',
  'btn:user:edit',
  'btn:user:delete',
  'btn:role:query',
  'btn:role:add',
  'btn:role:edit',
  'btn:role:delete',
  'btn:role:assign',
  'btn:dict:type:query',
  'btn:dict:type:add',
  'btn:dict:type:edit',
  'btn:dict:type:delete',
  'btn:dict:data:query',
  'btn:dict:data:add',
  'btn:dict:data:edit',
  'btn:dict:data:delete',
  'btn:permission:save',
  'btn:log:query',
]

const adminFieldPermissions: Record<string, FieldMode> = {
  'user.username': 'editable',
  'user.nickname': 'editable',
  'user.phone': 'editable',
  'user.email': 'editable',
  'user.status': 'editable',
  'role.roleCode': 'readonly',
}

const operatorFieldPermissions: Record<string, FieldMode> = {
  'user.username': 'readonly',
  'user.nickname': 'editable',
  'user.phone': 'hidden',
  'user.email': 'readonly',
  'user.status': 'hidden',
  'role.roleCode': 'hidden',
}

const auditorFieldPermissions: Record<string, FieldMode> = {
  'user.username': 'readonly',
  'user.nickname': 'readonly',
  'user.phone': 'hidden',
  'user.email': 'hidden',
  'user.status': 'hidden',
  'role.roleCode': 'hidden',
}

const roleProfiles: RoleProfile[] = [
  {
    key: 'admin',
    label: '管理员视角',
    roles: ['admin'],
    permissionCodes: ['*', ...allPermissions],
    fieldPermissions: adminFieldPermissions,
  },
  {
    key: 'operator',
    label: '运营视角',
    roles: ['operator'],
    permissionCodes: [
      'page:system:user',
      'page:system:dict:type',
      'page:system:dict:data',
      'btn:user:query',
      'btn:user:edit',
      'btn:dict:type:query',
      'btn:dict:data:query',
      'btn:dict:data:edit',
    ],
    fieldPermissions: operatorFieldPermissions,
  },
  {
    key: 'auditor',
    label: '审计视角',
    roles: ['auditor'],
    permissionCodes: [
      'page:system:log:operation',
      'page:system:log:login',
      'btn:log:query',
      'page:system:user',
      'btn:user:query',
    ],
    fieldPermissions: auditorFieldPermissions,
  },
]

export const useAuthStore = defineStore('auth', () => {
  const currentProfileKey = ref('admin')
  const profileMap = ref<Record<string, RoleProfile>>(
    roleProfiles.reduce((acc, profile) => {
      acc[profile.key] = {
        ...profile,
        fieldPermissions: { ...profile.fieldPermissions },
      }
      return acc
    }, {} as Record<string, RoleProfile>),
  )

  const currentProfile = computed(() => profileMap.value[currentProfileKey.value])

  const permissionCodes = computed(() => currentProfile.value.permissionCodes)
  const fieldPermissions = computed(() => currentProfile.value.fieldPermissions)
  const availableProfiles = computed(() => Object.values(profileMap.value))

  const hasPermission = (code: string) => {
    const codes = permissionCodes.value
    return codes.includes('*') || codes.includes(code)
  }

  const hasAnyPermission = (codes: string[]) => codes.some(code => hasPermission(code))

  const hasPagePermission = (code?: string) => {
    if (!code) {
      return true
    }
    return hasPermission(code)
  }

  const getFieldMode = (moduleName: string, fieldName: string): FieldMode => {
    const key = `${moduleName}.${fieldName}`
    return fieldPermissions.value[key] ?? 'editable'
  }

  const switchProfile = (profileKey: string) => {
    if (!profileMap.value[profileKey]) {
      return
    }
    currentProfileKey.value = profileKey
  }

  const updateFieldPermission = (key: string, mode: FieldMode) => {
    profileMap.value[currentProfileKey.value].fieldPermissions[key] = mode
  }

  return {
    currentProfileKey,
    currentProfile,
    permissionCodes,
    fieldPermissions,
    availableProfiles,
    hasPermission,
    hasAnyPermission,
    hasPagePermission,
    getFieldMode,
    switchProfile,
    updateFieldPermission,
  }
})
