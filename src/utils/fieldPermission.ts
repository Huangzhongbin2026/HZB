import { useAuthStore } from '../stores/auth'

export const useFieldPermission = () => {
  const authStore = useAuthStore()

  const isFieldVisible = (moduleName: string, fieldName: string) => {
    return authStore.getFieldMode(moduleName, fieldName) !== 'hidden'
  }

  const isFieldReadonly = (moduleName: string, fieldName: string) => {
    return authStore.getFieldMode(moduleName, fieldName) === 'readonly'
  }

  const isFieldEditable = (moduleName: string, fieldName: string) => {
    return authStore.getFieldMode(moduleName, fieldName) === 'editable'
  }

  return {
    isFieldVisible,
    isFieldReadonly,
    isFieldEditable,
  }
}
