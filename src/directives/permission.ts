import type { Directive } from 'vue'
import { permissionCache } from '@/utils/permission/cache'

export const vPermission: Directive = {
  mounted(el, binding) {
    const snapshot = permissionCache.load()
    const needCode = String(binding.value || '')
    if (!snapshot || !snapshot.buttonCodes.includes(needCode)) {
      el.parentNode?.removeChild(el)
    }
  },
}
