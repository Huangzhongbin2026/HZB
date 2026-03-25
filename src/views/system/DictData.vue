<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createDictData, deleteDictData, listDictData, listDictTypes, updateDictData } from '../../api/system'
import type { DictDataItem, DictTypeItem } from '../../types/auth'

const loading = ref(false)
const tableData = ref<DictDataItem[]>([])
const dictTypes = ref<DictTypeItem[]>([])
const dialogVisible = ref(false)
const editing = ref(false)

const form = reactive<DictDataItem>({
  id: 0,
  dictCode: '',
  label: '',
  value: '',
  sort: 1,
  status: 'enabled',
})

const dictCodeNameMap = computed(() =>
  dictTypes.value.reduce((acc, item) => {
    acc[item.code] = item.name
    return acc
  }, {} as Record<string, string>),
)

const loadData = async () => {
  loading.value = true
  const [data, types] = await Promise.all([listDictData(), listDictTypes()])
  tableData.value = data
  dictTypes.value = types
  loading.value = false
}

const openCreate = () => {
  editing.value = false
  Object.assign(form, { id: 0, dictCode: '', label: '', value: '', sort: 1, status: 'enabled' })
  dialogVisible.value = true
}

const openEdit = (row: DictDataItem) => {
  editing.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const saveData = async () => {
  if (editing.value) {
    await updateDictData({ ...form })
    ElMessage.success('字典数据已更新')
  } else {
    const { id: _id, ...payload } = form
    await createDictData(payload)
    ElMessage.success('字典数据已新增')
  }
  dialogVisible.value = false
  await loadData()
}

const removeData = async (id: number) => {
  await ElMessageBox.confirm('确认删除该字典数据？', '提示', { type: 'warning' })
  await deleteDictData(id)
  ElMessage.success('删除成功')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>数据字典 - 字典数据管理</span>
        <div>
          <el-button v-permission="'btn:dict:data:query'" @click="loadData">刷新</el-button>
          <el-button v-permission="'btn:dict:data:add'" type="primary" @click="openCreate">新增数据</el-button>
        </div>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="dictCode" label="字典类型" min-width="180">
        <template #default="scope">
          {{ dictCodeNameMap[scope.row.dictCode] || scope.row.dictCode }}
        </template>
      </el-table-column>
      <el-table-column prop="label" label="标签" />
      <el-table-column prop="value" label="值" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'enabled' ? 'success' : 'info'">
            {{ scope.row.status === 'enabled' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="scope">
          <el-button v-permission="'btn:dict:data:edit'" link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button
            v-permission="'btn:dict:data:delete'"
            link
            type="danger"
            @click="removeData(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑字典数据' : '新增字典数据'" width="560px">
      <el-form label-width="90px">
        <el-form-item label="字典类型">
          <el-select v-model="form.dictCode" placeholder="请选择字典类型">
            <el-option v-for="item in dictTypes" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签"><el-input v-model="form.label" /></el-form-item>
        <el-form-item label="值"><el-input v-model="form.value" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="1" /></el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            active-value="enabled"
            inactive-value="disabled"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>
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
