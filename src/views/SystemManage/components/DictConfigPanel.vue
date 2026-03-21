<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemDictApi } from '@/api/modules/system/dict'
import type { SysDictItem, SysDictType } from '@/types/system'

const typeList = ref<SysDictType[]>([])
const itemList = ref<SysDictItem[]>([])
const currentType = ref('')

const loadTypes = async () => {
  const res = await systemDictApi.listTypes()
  typeList.value = res.data
  if (typeList.value.length && !currentType.value) {
    currentType.value = typeList.value[0].dictCode
    loadItems()
  }
}

const loadItems = async (keyword = '') => {
  if (!currentType.value) return
  const res = await systemDictApi.listItems(currentType.value, keyword)
  itemList.value = res.data
}

const addType = async () => {
  await systemDictApi.saveType({ dictName: '新字典', dictCode: `DICT_${Date.now()}`, sortNo: 1, isEnabled: true })
  ElMessage.success('字典分类新增成功')
  loadTypes()
}

const addItem = async () => {
  await systemDictApi.saveItem({ dictTypeId: '', itemName: '新字典项', itemCode: `ITEM_${Date.now()}`, itemValue: '1', sortNo: 1, isEnabled: true })
  ElMessage.success('字典项新增成功')
  loadItems()
}

onMounted(loadTypes)
</script>

<template>
  <div class="panel-grid">
    <el-card shadow="never">
      <template #header>
        <div class="head-wrap">
          <span class="panel-title">字典分类</span>
          <el-button type="primary" size="small" @click="addType">新增分类</el-button>
        </div>
      </template>
      <el-table :data="typeList" border row-key="id" @row-click="(row) => { currentType = row.dictCode; loadItems() }">
        <el-table-column prop="dictName" label="分类名称" min-width="140" />
        <el-table-column prop="dictCode" label="分类编码" min-width="160" />
        <el-table-column prop="sortNo" label="排序" width="80" />
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="head-wrap">
          <span class="panel-title">字典项</span>
          <el-space>
            <el-input placeholder="模糊搜索" style="width: 220px" @input="(v) => loadItems(String(v || ''))" />
            <el-button type="primary" size="small" @click="addItem">新增字典项</el-button>
          </el-space>
        </div>
      </template>
      <el-table :data="itemList" border row-key="id">
        <el-table-column prop="itemName" label="名称" min-width="160" />
        <el-table-column prop="itemCode" label="编码" min-width="160" />
        <el-table-column prop="itemValue" label="值" width="100" />
        <el-table-column prop="sortNo" label="排序" width="80" />
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

.head-wrap {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
