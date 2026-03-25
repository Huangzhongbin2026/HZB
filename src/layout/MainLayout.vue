<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { routes } from '../router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const menuItems = computed(() => {
  const root = routes.find(item => item.path === '/')
  const children = root?.children ?? []

  return children
    .filter(item => item.meta?.menu)
    .filter(item => authStore.hasPagePermission(item.meta?.permissionCode as string | undefined))
    .map(item => ({
      path: `/${item.path}`,
      title: item.meta?.title as string,
    }))
})

const activeMenu = computed(() => route.path)

const handleProfileChange = (key: string) => {
  authStore.switchProfile(key)
  if (!authStore.hasPagePermission(route.meta.permissionCode as string | undefined)) {
    const nextMenu = menuItems.value[0]
    if (nextMenu) {
      router.push(nextMenu.path)
    }
  }
}
</script>

<template>
  <el-container class="app-shell">
    <el-aside class="aside" width="230px">
      <div class="logo">锐小蜜 · 系统管理</div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          {{ item.title }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <h2>RBAC + 页面/按钮/字段三级权限控制</h2>
        </div>
        <div class="header-right">
          <span>预览角色：</span>
          <el-select
            :model-value="authStore.currentProfileKey"
            style="width: 180px"
            @update:model-value="handleProfileChange"
          >
            <el-option
              v-for="profile in authStore.availableProfiles"
              :key="profile.key"
              :label="profile.label"
              :value="profile.key"
            />
          </el-select>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f5f7fa;
}

.aside {
  border-right: 1px solid #e4e7ed;
  background: linear-gradient(180deg, #1f2937 0%, #111827 100%);
  color: #fff;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.menu {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item) {
  color: #d1d5db;
}

:deep(.el-menu-item.is-active) {
  color: #60a5fa;
  background-color: rgba(96, 165, 250, 0.1);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.header-left h2 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.main {
  padding: 16px;
}

@media (max-width: 900px) {
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    height: auto;
    padding-top: 10px;
    padding-bottom: 10px;
  }
}
</style>
