<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MenuConfigPanel from './components/MenuConfigPanel.vue'
import DictConfigPanel from './components/DictConfigPanel.vue'
import RoleManagePanel from './components/RoleManagePanel.vue'
import UserManagePanel from './components/UserManagePanel.vue'
import OperationLogPanel from './components/OperationLogPanel.vue'
import { systemUserApi } from '@/api/modules/system/user'
import { permissionCache } from '@/utils/permission/cache'

const activeTab = ref('menu')

onMounted(async () => {
  const res = await systemUserApi.permissionSnapshot()
  permissionCache.save(res.data)
})
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <h1>系统管理模块</h1>
      <p>菜单、权限、用户、字典、日志统一配置与治理</p>
    </header>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="菜单配置" name="menu"><MenuConfigPanel /></el-tab-pane>
      <el-tab-pane label="数据字典" name="dict"><DictConfigPanel /></el-tab-pane>
      <el-tab-pane label="角色管理" name="role"><RoleManagePanel /></el-tab-pane>
      <el-tab-pane label="用户管理" name="user"><UserManagePanel /></el-tab-pane>
      <el-tab-pane label="系统日志" name="log"><OperationLogPanel /></el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.page-wrap {
  min-height: 100vh;
  padding: 16px;
  background: linear-gradient(155deg, #f0f9ff, #f8fafc 45%, #ecfccb);
}

.page-header {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #bae6fd;
  background: #ffffffd9;
}

.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.page-header p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #475569;
}
</style>
