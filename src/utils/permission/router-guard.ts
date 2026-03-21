import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { permissionCache } from '@/utils/permission/cache'

export const systemBeforeEach = (
  to: RouteLocationNormalized,
  _from: RouteLocationNormalized,
  next: NavigationGuardNext,
) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    next('/login')
    return
  }

  const snapshot = permissionCache.load()
  if (to.meta.permissionCode) {
    const code = String(to.meta.permissionCode)
    const allowed = !!snapshot && snapshot.menuCodes.includes(code)
    next(allowed ? true : '/403')
    return
  }

  next()
}
