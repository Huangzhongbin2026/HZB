<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemRoleApi } from '@/api/modules/system/role'
import type { SysRole } from '@/types/system'

const roleList = ref<SysRole[]>([])
const drawerVisible = ref(false)
const previewVisible = ref(false)
const permissionPreview = ref<Record<string, unknown>>({})

const roleForm = reactive<Partial<SysRole>>({
  roleName: '',
  roleCode: '',
  deptCode: '',
  description: '',
  isEnabled: true,
})

const permissionForm = reactive({
  menuIds: [] as string[],
  buttonCodes: [] as string[],
  dataScopeType: 'SELF',
  customDeptCodes: [] as string[],
  fieldPermissions: [] as Array<{ bizTable: string; fieldCode: string; permissionType: string }>,
})

const loadRoles = async () => {
  const res = await systemRoleApi.list()
  roleList.value = res.data
}

const saveRole = async () => {
  await systemRoleApi.save(roleForm)
  ElMessage.success('角色保存成功')
  Object.assign(roleForm, { roleName: '', roleCode: '', deptCode: '', description: '', isEnabled: true })
  loadRoles()
}

const openPermission = () => {
  drawerVisible.value = true
}

const savePermission = async (roleId: string) => {
  await systemRoleApi.savePermissions(roleId, permissionForm)
  ElMessage.success('权限配置成功')
  drawerVisible.value = false
}

const previewPermission = async (roleId: string) => {
  const res = await systemRoleApi.previewPermissions(roleId)
  permissionPreview.value = res.data
  previewVisible.value = true
}

onMounted(loadRoles)
</script>

<template>
  <div class="panel-stack">
    <el-card shadow="never">
      <template #header>
        <div class="panel-title">角色配置</div>
      </template>
      <el-form inline>
        <el-form-item label="角色名称"><el-input v-model="roleForm.roleName" /></el-form-item>
        <el-form-item label="角色编码"><el-input v-model="roleForm.roleCode" /></el-form-item>
        <el-form-item label="所属部门"><el-input v-model="roleForm.deptCode" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="roleForm.isEnabled" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveRole">保存角色</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="panel-title">角色列表</div>
      </template>
      <el-table :data="roleList" border row-key="id">
        <el-table-column prop="roleName" label="角色名称" min-width="160" />
        <el-table-column prop="roleCode" label="角色编码" min-width="180" />
        <el-table-column prop="deptCode" label="所属部门" min-width="120" />
        <el-table-column label="操作" width="260">
          <template #default="scope">
            <el-button link type="primary" @click="openPermission">配置权限</el-button>
            <el-button link type="success" @click="previewPermission(scope.row.id)">权限预览</el-button>
            <el-button link type="warning" @click="savePermission(scope.row.id)">保存当前权限</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="drawerVisible" title="角色权限配置" size="40%">
      <el-form label-width="150px">
        <el-form-item label="菜单权限ID集合">
          <el-select v-model="permissionForm.menuIds" multiple allow-create filterable style="width: 100%" />
        </el-form-item>
        <el-form-item label="按钮权限标识">
          <el-select v-model="permissionForm.buttonCodes" multiple allow-create filterable style="width: 100%" />
        </el-form-item>
        <el-form-item label="数据范围">
          <el-radio-group v-model="permissionForm.dataScopeType">
            <el-radio label="ALL">全部数据</el-radio>
            <el-radio label="DEPT">本部门</el-radio>
            <el-radio label="SELF">本人</el-radio>
            <el-radio label="CUSTOM_DEPT">自定义部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="自定义部门">
          <el-select v-model="permissionForm.customDeptCodes" multiple allow-create filterable style="width: 100%" />
        </el-form-item>
        <el-form-item label="字段权限(JSON示例)">
          <el-input
            type="textarea"
            :rows="6"
            model-value='[{"bizTable":"t_task_main","fieldCode":"customer_price","permissionType":"HIDDEN"}]'
            disabled
          />
        </el-form-item>
      </el-form>
    </el-drawer>

    <el-dialog v-model="previewVisible" title="权限预览" width="50%">
      <pre>{{ JSON.stringify(permissionPreview, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
}
</style>
