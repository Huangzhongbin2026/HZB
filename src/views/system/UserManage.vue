<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, deleteUser, listUsers, updateUser } from '../../api/system'
import type { UserItem } from '../../types/auth'
import { useFieldPermission } from '../../utils/fieldPermission'

const { isFieldReadonly, isFieldVisible } = useFieldPermission()

const loading = ref(false)
const tableData = ref<UserItem[]>([])
const dialogVisible = ref(false)
const editing = ref(false)

const roleOptions = [
  { label: '管理员', value: 'admin' },
  { label: '运营', value: 'operator' },
  { label: '审计', value: 'auditor' },
]

const form = reactive<UserItem>({
  id: 0,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  status: 'enabled',
  roleCodes: [],
})

const visibleColumns = computed(() => ({
  username: isFieldVisible('user', 'username'),
  nickname: isFieldVisible('user', 'nickname'),
  phone: isFieldVisible('user', 'phone'),
  email: isFieldVisible('user', 'email'),
  status: isFieldVisible('user', 'status'),
}))

const resetForm = () => {
  form.id = 0
  form.username = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.status = 'enabled'
  form.roleCodes = []
}

const loadData = async () => {
  loading.value = true
  tableData.value = await listUsers()
  loading.value = false
}

const openCreate = () => {
  editing.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row: UserItem) => {
  editing.value = true
  Object.assign(form, row)
  form.roleCodes = [...row.roleCodes]
  dialogVisible.value = true
}

const saveUser = async () => {
  const payload = {
    username: form.username,
    nickname: form.nickname,
    email: form.email,
    phone: form.phone,
    status: form.status,
    roleCodes: [...form.roleCodes],
  }

  if (editing.value) {
    await updateUser({ ...payload, id: form.id })
    ElMessage.success('用户已更新')
  } else {
    await createUser(payload)
    ElMessage.success('用户已新增')
  }

  dialogVisible.value = false
  await loadData()
}

const removeUser = async (id: number) => {
  await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
  await deleteUser(id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>用户管理（含角色分配、状态管理、字段权限）</span>
        <div>
          <el-button v-permission="'btn:user:query'" @click="loadData">刷新</el-button>
          <el-button v-permission="'btn:user:add'" type="primary" @click="openCreate">新增用户</el-button>
        </div>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column v-if="visibleColumns.username" prop="username" label="用户名" />
      <el-table-column v-if="visibleColumns.nickname" prop="nickname" label="昵称" />
      <el-table-column v-if="visibleColumns.phone" prop="phone" label="手机号" />
      <el-table-column v-if="visibleColumns.email" prop="email" label="邮箱" />
      <el-table-column v-if="visibleColumns.status" prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === 'enabled' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" width="160">
        <template #default="scope">
          {{ scope.row.roleCodes.join(', ') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button v-permission="'btn:user:edit'" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button v-permission="'btn:user:delete'" link type="danger" @click="removeUser(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑用户' : '新增用户'"
      width="640px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item v-if="visibleColumns.username" label="用户名">
          <el-input v-model="form.username" :readonly="isFieldReadonly('user', 'username')" />
        </el-form-item>
        <el-form-item v-if="visibleColumns.nickname" label="昵称">
          <el-input v-model="form.nickname" :readonly="isFieldReadonly('user', 'nickname')" />
        </el-form-item>
        <el-form-item v-if="visibleColumns.phone" label="手机号">
          <el-input v-model="form.phone" :readonly="isFieldReadonly('user', 'phone')" />
        </el-form-item>
        <el-form-item v-if="visibleColumns.email" label="邮箱">
          <el-input v-model="form.email" :readonly="isFieldReadonly('user', 'email')" />
        </el-form-item>
        <el-form-item v-if="visibleColumns.status" label="状态">
          <el-switch
            v-model="form.status"
            active-value="enabled"
            inactive-value="disabled"
            active-text="启用"
            inactive-text="禁用"
            :disabled="isFieldReadonly('user', 'status')"
          />
        </el-form-item>
        <el-form-item label="角色分配">
          <el-select v-model="form.roleCodes" multiple placeholder="请选择角色">
            <el-option v-for="option in roleOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser">保存</el-button>
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
