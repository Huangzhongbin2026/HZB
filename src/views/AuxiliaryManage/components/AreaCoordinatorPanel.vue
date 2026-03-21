<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { AreaCoordinatorItem } from '@/types/auxiliary'
import { auxiliaryApi } from '@/api/modules/system/auxiliary'
import TemplateDownload from './common/TemplateDownload.vue'
import ExcelImport from './common/ExcelImport.vue'
import MatchHint from './common/MatchHint.vue'

const list = ref<AreaCoordinatorItem[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const matchResult = ref('')

const form = reactive<Partial<AreaCoordinatorItem>>({ priorityNo: 100, status: true })
const query = reactive({ pageNo: 1, pageSize: 10, saleDeptCode: '', provinceCode: '', coordinatorUserName: '' })

const matchQuery = reactive({ saleDeptCode: '', provinceCode: '', deptKeyword: '', projectKeyword: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await auxiliaryApi.queryAreaCoordinators(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const save = async () => {
  await auxiliaryApi.saveAreaCoordinator(form)
  ElMessage.success('区域统筹规则保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async (id: string) => {
  await auxiliaryApi.deleteAreaCoordinator(id)
  ElMessage.success('删除成功')
  loadData()
}

const onParsed = async (rows: Record<string, unknown>[]) => {
  await auxiliaryApi.importAreaCoordinators(rows as unknown as AreaCoordinatorItem[])
  ElMessage.success('导入成功')
  loadData()
}

const doMatch = async () => {
  const res = await auxiliaryApi.matchAreaCoordinator(matchQuery)
  matchResult.value = `匹配结果：${res.data.coordinatorUserName || '未命中'} (${res.data.coordinatorUserId || '-'})`
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header><div class="panel-title">区域统筹划分管理</div></template>
    <el-form inline>
      <el-form-item label="销售部门"><el-input v-model="query.saleDeptCode" /></el-form-item>
      <el-form-item label="省份"><el-input v-model="query.provinceCode" /></el-form-item>
      <el-form-item label="区域统筹"><el-input v-model="query.coordinatorUserName" /></el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      <el-form-item><el-button @click="dialogVisible = true">新增</el-button></el-form-item>
      <el-form-item><TemplateDownload name="area-coordinator" :columns="['saleDeptCode','provinceCode','deptKeyword','projectKeyword','coordinatorUserId','coordinatorUserName','priorityNo']" /></el-form-item>
      <el-form-item><ExcelImport @parsed="onParsed" /></el-form-item>
      <el-form-item><el-button @click="auxiliaryApi.exportAreaCoordinators(query)">导出</el-button></el-form-item>
    </el-form>

    <el-divider />

    <el-form inline>
      <el-form-item label="匹配部门"><el-input v-model="matchQuery.saleDeptCode" /></el-form-item>
      <el-form-item label="匹配省份"><el-input v-model="matchQuery.provinceCode" /></el-form-item>
      <el-form-item label="部门关键词"><el-input v-model="matchQuery.deptKeyword" /></el-form-item>
      <el-form-item label="项目关键词"><el-input v-model="matchQuery.projectKeyword" /></el-form-item>
      <el-form-item><el-button @click="doMatch">匹配验证</el-button></el-form-item>
    </el-form>
    <MatchHint v-if="matchResult" :text="matchResult" type="success" />

    <el-table :data="list" border row-key="id" v-loading="loading">
      <el-table-column prop="saleDeptCode" label="销售部门" min-width="120" />
      <el-table-column prop="provinceCode" label="省份" min-width="100" />
      <el-table-column prop="deptKeyword" label="部门关键词" min-width="120" />
      <el-table-column prop="projectKeyword" label="项目关键词" min-width="120" />
      <el-table-column prop="coordinatorUserName" label="区域统筹" min-width="120" />
      <el-table-column prop="priorityNo" label="优先级" width="90" />
      <el-table-column label="操作" width="100"><template #default="scope"><el-button link type="danger" @click="remove(scope.row.id)">删除</el-button></template></el-table-column>
    </el-table>

    <div class="pager-wrap"><el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @change="loadData" /></div>

    <el-dialog v-model="dialogVisible" title="区域统筹规则" width="560px">
      <el-form label-width="120px">
        <el-form-item label="销售部门"><el-input v-model="form.saleDeptCode" /></el-form-item>
        <el-form-item label="省份"><el-input v-model="form.provinceCode" /></el-form-item>
        <el-form-item label="部门关键词"><el-input v-model="form.deptKeyword" /></el-form-item>
        <el-form-item label="项目关键词"><el-input v-model="form.projectKeyword" /></el-form-item>
        <el-form-item label="统筹ID"><el-input v-model="form.coordinatorUserId" /></el-form-item>
        <el-form-item label="统筹名称"><el-input v-model="form.coordinatorUserName" /></el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="form.priorityNo" :min="1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.panel-title { font-size: 16px; font-weight: 700; }
.pager-wrap { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
