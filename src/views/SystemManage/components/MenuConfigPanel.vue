<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemMenuApi } from '@/api/modules/system/menu'
import { systemLogApi } from '@/api/modules/system/log'
import type { SysMenu } from '@/types/system'

const loading = ref(false)
const menus = ref<SysMenu[]>([])
const selectedIds = ref<string[]>([])

const form = reactive<Partial<SysMenu>>({
  menuName: '',
  menuType: 'MENU',
  parentId: '0',
  routePath: '',
  permissionCode: '',
  icon: 'Menu',
  sortNo: 1,
  isVisible: true,
  isEnabled: true,
})

const loadMenus = async () => {
  loading.value = true
  try {
    const res = await systemMenuApi.listTree()
    menus.value = res.data
  } finally {
    loading.value = false
  }
}

const saveMenu = async () => {
  await systemMenuApi.create(form)
  await systemLogApi.record({ operModule: 'MENU', operType: 'CREATE', operContent: `新增菜单:${form.menuName}` })
  ElMessage.success('菜单保存成功')
  loadMenus()
}

const removeMenu = async (id: string) => {
  await systemMenuApi.remove(id)
  await systemLogApi.record({ operModule: 'MENU', operType: 'DELETE', operContent: `删除菜单:${id}` })
  ElMessage.success('菜单删除成功')
  loadMenus()
}

const batchEnable = async (enabled: boolean) => {
  await systemMenuApi.batchEnable(selectedIds.value, enabled)
  ElMessage.success(enabled ? '批量启用成功' : '批量禁用成功')
  loadMenus()
}

const handleSelectionChange = (rows: SysMenu[]) => {
  selectedIds.value = rows.map((row) => row.id)
}

onMounted(loadMenus)
</script>

<template>
  <div class="panel-grid">
    <el-card shadow="never">
      <template #header>
        <div class="panel-title">菜单配置</div>
      </template>
      <el-form label-width="110px">
        <el-form-item label="菜单名称"><el-input v-model="form.menuName" /></el-form-item>
        <el-form-item label="菜单类型">
          <el-radio-group v-model="form.menuType">
            <el-radio label="DIR">目录</el-radio>
            <el-radio label="MENU">菜单</el-radio>
            <el-radio label="BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="父级菜单"><el-input v-model="form.parentId" /></el-form-item>
        <el-form-item label="路由地址"><el-input v-model="form.routePath" /></el-form-item>
        <el-form-item label="权限标识"><el-input v-model="form.permissionCode" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortNo" :min="1" /></el-form-item>
        <el-form-item label="是否显示"><el-switch v-model="form.isVisible" /></el-form-item>
        <el-form-item label="是否启用"><el-switch v-model="form.isEnabled" /></el-form-item>
      </el-form>
      <el-space>
        <el-button type="primary" @click="saveMenu">保存菜单</el-button>
        <el-button @click="batchEnable(true)">批量启用</el-button>
        <el-button @click="batchEnable(false)">批量禁用</el-button>
      </el-space>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="panel-title">菜单树</div>
      </template>
      <el-table :data="menus" border row-key="id" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="menuName" label="名称" min-width="150" />
        <el-table-column prop="menuType" label="类型" width="90" />
        <el-table-column prop="routePath" label="路由" min-width="180" />
        <el-table-column prop="permissionCode" label="权限标识" min-width="180" />
        <el-table-column prop="sortNo" label="排序" width="80" />
        <el-table-column label="操作" width="120">
          <template #default="scope">
            <el-button type="danger" link @click="removeMenu(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.panel-grid {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
}
</style>
