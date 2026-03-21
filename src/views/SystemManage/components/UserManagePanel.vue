<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemUserApi } from '@/api/modules/system/user'
import { vPermission } from '@/directives/permission'
import type { SysUser } from '@/types/system'

const users = ref<SysUser[]>([])
const assignDialog = ref(false)
const selectedUserId = ref('')
const selectedRoles = ref<string[]>([])

const userForm = reactive<Partial<SysUser>>({
  userName: '',
  account: '',
  mobile: '',
  feishuId: '',
  email: '',
  deptCode: '',
  status: true,
})

const loadUsers = async () => {
  const res = await systemUserApi.list()
  users.value = res.data
}

const saveUser = async () => {
  await systemUserApi.save(userForm)
  ElMessage.success('用户保存成功')
  loadUsers()
}

const openAssignRoles = (userId: string) => {
  selectedUserId.value = userId
  selectedRoles.value = []
  assignDialog.value = true
}

const saveAssignRoles = async () => {
  await systemUserApi.assignRoles(selectedUserId.value, selectedRoles.value)
  ElMessage.success('角色分配成功')
  assignDialog.value = false
}

const resetPassword = async (userId: string) => {
  await systemUserApi.resetPassword(userId)
  ElMessage.success('密码重置成功')
}

onMounted(loadUsers)
</script>

<template>
  <div class="panel-stack">
    <el-card shadow="never">
      <template #header>
        <div class="panel-title">用户信息</div>
      </template>
      <el-form inline>
        <el-form-item label="姓名"><el-input v-model="userForm.userName" /></el-form-item>
        <el-form-item label="账号"><el-input v-model="userForm.account" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="userForm.mobile" /></el-form-item>
        <el-form-item label="飞书ID"><el-input v-model="userForm.feishuId" /></el-form-item>
        <el-form-item label="部门"><el-input v-model="userForm.deptCode" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="userForm.status" /></el-form-item>
        <el-form-item><el-button type="primary" @click="saveUser">保存用户</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="panel-title">用户列表</div>
      </template>
      <el-table :data="users" border row-key="id">
        <el-table-column prop="userName" label="姓名" min-width="100" />
        <el-table-column prop="account" label="账号" min-width="120" />
        <el-table-column prop="mobile" label="手机号" min-width="130" />
        <el-table-column prop="feishuId" label="飞书ID" min-width="150" />
        <el-table-column prop="deptCode" label="部门" min-width="120" />
        <el-table-column prop="lastLoginAt" label="最后登录" min-width="180" />
        <el-table-column label="操作" width="260">
          <template #default="scope">
            <el-button v-permission="'sys:user:assignRole'" link type="primary" @click="openAssignRoles(scope.row.id)">分配角色</el-button>
            <el-button v-permission="'sys:user:resetPwd'" link type="warning" @click="resetPassword(scope.row.id)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="assignDialog" title="角色分配" width="420px">
      <el-form label-width="90px">
        <el-form-item label="角色ID集合">
          <el-select v-model="selectedRoles" multiple allow-create filterable style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAssignRoles">保存</el-button>
      </template>
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
