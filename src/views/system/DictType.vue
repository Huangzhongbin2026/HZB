<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDictType, deleteDictType, listDictTypes, updateDictType } from '../../api/system'
import type { DictTypeItem } from '../../types/auth'

const loading = ref(false)
const tableData = ref<DictTypeItem[]>([])
const dialogVisible = ref(false)
const editing = ref(false)

const form = reactive<DictTypeItem>({
  id: 0,
  name: '',
  code: '',
  status: 'enabled',
  remark: '',
})

const loadData = async () => {
  loading.value = true
  tableData.value = await listDictTypes()
  loading.value = false
}

const openCreate = () => {
  editing.value = false
  Object.assign(form, { id: 0, name: '', code: '', status: 'enabled', remark: '' })
  dialogVisible.value = true
}

const openEdit = (row: DictTypeItem) => {
  editing.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveData = async () => {
  if (editing.value) {
    await updateDictType({ ...form })
    ElMessage.success('字典类型已更新')
  } else {
    const { id: _id, ...payload } = form
    await createDictType(payload)
    ElMessage.success('字典类型已新增')
  }
  dialogVisible.value = false
  await loadData()
}

const removeData = async (id: number) => {
  await ElMessageBox.confirm('确认删除该字典类型？', '提示', { type: 'warning' })
  await deleteDictType(id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>数据字典 - 字典类型管理</span>
        <div>
          <el-button v-permission="'btn:dict:type:query'" @click="loadData">刷新</el-button>
          <el-button v-permission="'btn:dict:type:add'" type="primary" @click="openCreate">新增类型</el-button>
        </div>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="类型名称" />
      <el-table-column prop="code" label="类型编码" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === 'enabled' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="scope">
          <el-button v-permission="'btn:dict:type:edit'" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button
            v-permission="'btn:dict:type:delete'"
            link
            type="danger"
            @click="removeData(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑字典类型' : '新增字典类型'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="类型名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型编码"><el-input v-model="form.code" :disabled="editing" /></el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            active-value="enabled"
            inactive-value="disabled"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveData">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
