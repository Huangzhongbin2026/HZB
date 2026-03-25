import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
    component: () => import('../layout/MainLayout.vue'),
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/dashboard/HomeView.vue'),
        meta: { title: '工作台', menu: true, permissionCode: undefined },
      },
      {
        path: 'system/user',
        name: 'system-user',
        component: () => import('../views/system/UserManage.vue'),
        meta: { title: '用户管理', menu: true, permissionCode: 'page:system:user' },
      },
      {
        path: 'system/role',
        name: 'system-role',
        component: () => import('../views/system/RoleManage.vue'),
        meta: { title: '角色管理', menu: true, permissionCode: 'page:system:role' },
      },
      {
        path: 'system/permission',
        name: 'system-permission',
        component: () => import('../views/system/PermissionManage.vue'),
        meta: { title: '权限管理', menu: true, permissionCode: 'page:system:permission' },
      },
      {
        path: 'system/log/operation',
        name: 'system-log-operation',
        component: () => import('../views/system/OperationLog.vue'),
        meta: { title: '操作日志', menu: true, permissionCode: 'page:system:log:operation' },
      },
      {
        path: 'system/log/login',
        name: 'system-log-login',
        component: () => import('../views/system/LoginLog.vue'),
        meta: { title: '登录日志', menu: true, permissionCode: 'page:system:log:login' },
      },
      {
        path: 'system/dict/type',
        name: 'system-dict-type',
        component: () => import('../views/system/DictType.vue'),
        meta: { title: '字典类型', menu: true, permissionCode: 'page:system:dict:type' },
      },
      {
        path: 'system/dict/data',
        name: 'system-dict-data',
        component: () => import('../views/system/DictData.vue'),
        meta: { title: '字典数据', menu: true, permissionCode: 'page:system:dict:data' },
      },
    ],
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('../views/system/ForbiddenView.vue'),
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(to => {
  const authStore = useAuthStore()
  const permissionCode = to.meta.permissionCode as string | undefined

  if (!authStore.hasPagePermission(permissionCode)) {
    return '/403'
  }

  return true
})

export { routes }
export default router
