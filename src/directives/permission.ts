import type { App, DirectiveBinding } from 'vue'
import { useAuthStore } from '../stores/auth'

const removeNode = (el: HTMLElement) => {
  if (el?.parentNode) {
    el.parentNode.removeChild(el)
  }
}

const parseBinding = (binding: DirectiveBinding<string | string[]>) => {
  if (Array.isArray(binding.value)) {
    return binding.value
  }
  if (typeof binding.value === 'string') {
    return [binding.value]
  }
  return []
}

export const registerPermissionDirective = (app: App) => {
  app.directive('permission', {
    mounted(el, binding) {
      const authStore = useAuthStore()
      const codes = parseBinding(binding)
      if (codes.length === 0) {
        removeNode(el)
        return
      }
      if (!authStore.hasAnyPermission(codes)) {
        removeNode(el)
      }
    },
    updated(el, binding) {
      const authStore = useAuthStore()
      const codes = parseBinding(binding)
      if (codes.length > 0 && !authStore.hasAnyPermission(codes)) {
        removeNode(el)
      }
    },
  })
}
