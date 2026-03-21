import type { PermissionSnapshot } from '@/types/system'

const CACHE_KEY = 'supply-system-permissions'

export const permissionCache = {
  save(snapshot: PermissionSnapshot) {
    localStorage.setItem(CACHE_KEY, JSON.stringify(snapshot))
  },
  load(): PermissionSnapshot | null {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as PermissionSnapshot
    } catch {
      return null
    }
  },
  clear() {
    localStorage.removeItem(CACHE_KEY)
  },
}
