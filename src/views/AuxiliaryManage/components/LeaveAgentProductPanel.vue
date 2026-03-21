<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { LeaveAgentProductItem } from '@/types/auxiliary'
import { auxiliaryApi } from '@/api/modules/system/auxiliary'
import TemplateDownload from './common/TemplateDownload.vue'
import ExcelImport from './common/ExcelImport.vue'

const list = ref<LeaveAgentProductItem[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive<Partial<LeaveAgentProductItem>>({ status: true })
const query = reactive({ pageNo: 1, pageSize: 10, productModel: '', originalUserName: '', agentUserName: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await auxiliaryApi.queryLeaveAgentProducts(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const save = async () => {
  await auxiliaryApi.saveLeaveAgentProduct(form)
  ElMessage.success('代理产品配置保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async (id: string) => {
  await auxiliaryApi.deleteLeaveAgentProduct(id)
  ElMessage.success('删除成功')
  loadData()
}

const onParsed = async (rows: Record<string, unknown>[]) => {
  await auxiliaryApi.importLeaveAgentProducts(rows as unknown as LeaveAgentProductItem[])
  ElMessage.success('导入成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header><div class="panel-title">请假代理产品型号管理</div></template>
    <el-form inline>
      <el-form-item label="产品型号"><el-input v-model="query.productModel" /></el-form-item>
      <el-form-item label="原统筹"><el-input v-model="query.originalUserName" /></el-form-item>
      <el-form-item label="代理统筹"><el-input v-model="query.agentUserName" /></el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      <el-form-item><el-button @click="dialogVisible = true">新增</el-button></el-form-item>
      <el-form-item><TemplateDownload name="leave-agent-product" :columns="['productModel','originalUserId','originalUserName','agentUserId','agentUserName']" /></el-form-item>
      <el-form-item><ExcelImport @parsed="onParsed" /></el-form-item>
      <el-form-item><el-button @click="auxiliaryApi.exportLeaveAgentProducts(query)">导出</el-button></el-form-item>
    </el-form>

    <el-table :data="list" border row-key="id" v-loading="loading">
      <el-table-column prop="productModel" label="产品型号" min-width="160" />
      <el-table-column prop="originalUserName" label="原产品统筹" min-width="130" />
      <el-table-column prop="agentUserName" label="代理产品统筹" min-width="130" />
      <el-table-column label="操作" width="100"><template #default="scope"><el-button link type="danger" @click="remove(scope.row.id)">删除</el-button></template></el-table-column>
    </el-table>

    <div class="pager-wrap"><el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @change="loadData" /></div>

    <el-dialog v-model="dialogVisible" title="请假代理产品" width="520px">
      <el-form label-width="120px">
        <el-form-item label="产品型号"><el-input v-model="form.productModel" /></el-form-item>
        <el-form-item label="原统筹ID"><el-input v-model="form.originalUserId" /></el-form-item>
        <el-form-item label="原统筹名称"><el-input v-model="form.originalUserName" /></el-form-item>
        <el-form-item label="代理统筹ID"><el-input v-model="form.agentUserId" /></el-form-item>
        <el-form-item label="代理统筹名称"><el-input v-model="form.agentUserName" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.panel-title { font-size: 16px; font-weight: 700; }
.pager-wrap { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
