<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createRole, deleteRole, listRoles, updateRole } from '../../api/system'
import type { RoleItem } from '../../types/auth'

const loading = ref(false)
const tableData = ref<RoleItem[]>([])
const dialogVisible = ref(false)
const editing = ref(false)

const form = reactive<Omit<RoleItem, 'permissions'> & { permissionsText: string }>({
  id: 0,
  roleName: '',
  roleCode: '',
  status: 'enabled',
  remark: '',
  permissionsText: '',
})

const loadData = async () => {
  loading.value = true
  tableData.value = await listRoles()
  loading.value = false
}

const resetForm = () => {
  form.id = 0
  form.roleName = ''
  form.roleCode = ''
  form.status = 'enabled'
  form.remark = ''
  form.permissionsText = ''
}

const openCreate = () => {
  editing.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row: RoleItem) => {
  editing.value = true
  form.id = row.id
  form.roleName = row.roleName
  form.roleCode = row.roleCode
  form.status = row.status
  form.remark = row.remark ?? ''
  form.permissionsText = row.permissions.join(',')
  dialogVisible.value = true
}

const saveRole = async () => {
  const payload = {
    roleName: form.roleName,
    roleCode: form.roleCode,
    status: form.status,
    remark: form.remark,
    permissions: form.permissionsText
      .split(',')
      .map(item => item.trim())
      .filter(Boolean),
  }

  if (editing.value) {
    await updateRole({ ...payload, id: form.id })
    ElMessage.success('角色已更新')
  } else {
    await createRole(payload)
    ElMessage.success('角色已新增')
  }

  dialogVisible.value = false
  await loadData()
}

const removeRole = async (id: number) => {
  await ElMessageBox.confirm('确定删除该角色吗？', '提示', { type: 'warning' })
  await deleteRole(id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>角色管理</span>
        <div>
          <el-button v-permission="'btn:role:query'" @click="loadData">刷新</el-button>
          <el-button v-permission="'btn:role:add'" type="primary" @click="openCreate">新增角色</el-button>
        </div>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="roleCode" label="角色编码" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === 'enabled' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button v-permission="'btn:role:assign'" link type="warning">分配权限</el-button>
          <el-button v-permission="'btn:role:edit'" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button v-permission="'btn:role:delete'" link type="danger" @click="removeRole(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑角色' : '新增角色'"
      width="620px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="form.roleCode" :disabled="editing" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            active-value="enabled"
            inactive-value="disabled"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        <el-form-item label="权限码">
          <el-input
            v-model="form.permissionsText"
            type="textarea"
            placeholder="示例：page:system:user,btn:user:add"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
