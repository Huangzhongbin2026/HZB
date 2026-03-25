<script setup lang="ts">
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import type { FieldMode } from '../../types/auth'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const pagePermissions = [
  'page:system:user',
  'page:system:role',
  'page:system:permission',
  'page:system:log:operation',
  'page:system:log:login',
  'page:system:dict:type',
  'page:system:dict:data',
]

const buttonPermissions = [
  'btn:user:add',
  'btn:user:edit',
  'btn:user:delete',
  'btn:role:add',
  'btn:role:edit',
  'btn:role:delete',
  'btn:role:assign',
  'btn:dict:type:add',
  'btn:dict:type:edit',
  'btn:dict:type:delete',
  'btn:dict:data:add',
  'btn:dict:data:edit',
  'btn:dict:data:delete',
  'btn:permission:save',
  'btn:log:query',
]

const editableFieldRows = reactive(
  Object.entries(authStore.fieldPermissions).map(([key, mode]) => ({
    key,
    mode,
  })),
)

const permissionBadgeType = (code: string) => (authStore.hasPermission(code) ? 'success' : 'info')

const modeOptions: { label: string; value: FieldMode }[] = [
  { label: '隐藏', value: 'hidden' },
  { label: '只读', value: 'readonly' },
  { label: '可编辑', value: 'editable' },
]

const currentProfileName = computed(() => authStore.currentProfile.label)

const saveFieldPermission = () => {
  editableFieldRows.forEach(row => authStore.updateFieldPermission(row.key, row.mode))
  ElMessage.success('字段权限已更新，页面将立即生效')
}
</script>

<template>
  <div class="permission-page">
    <el-card>
      <template #header>
        <div class="header-row">
          <span>页面级权限（路由拦截 + 菜单显隐）</span>
          <el-tag type="warning">当前视角：{{ currentProfileName }}</el-tag>
        </div>
      </template>

      <div class="permission-grid">
        <el-tag v-for="code in pagePermissions" :key="code" :type="permissionBadgeType(code)">
          {{ code }}
        </el-tag>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <span>按钮级权限（v-permission 指令控制）</span>
      </template>
      <div class="permission-grid">
        <el-tag v-for="code in buttonPermissions" :key="code" :type="permissionBadgeType(code)">
          {{ code }}
        </el-tag>
      </div>
    </el-card>

    <el-card>
      <template #header>
        <div class="header-row">
          <span>字段级权限（表单/表格显隐 + 只读 + 编辑）</span>
          <el-button v-permission="'btn:permission:save'" type="primary" @click="saveFieldPermission">
            保存字段权限
          </el-button>
        </div>
      </template>

      <el-table :data="editableFieldRows" border>
        <el-table-column prop="key" label="字段键" min-width="220" />
        <el-table-column label="权限模式" width="240">
          <template #default="scope">
            <el-select v-model="scope.row.mode" style="width: 180px">
              <el-option
                v-for="option in modeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.permission-page {
  display: grid;
  gap: 14px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.permission-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
