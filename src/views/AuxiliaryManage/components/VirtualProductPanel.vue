<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { VirtualProductItem } from '@/types/auxiliary'
import { auxiliaryApi } from '@/api/modules/system/auxiliary'
import TemplateDownload from './common/TemplateDownload.vue'
import ExcelImport from './common/ExcelImport.vue'
import MatchHint from './common/MatchHint.vue'

const list = ref<VirtualProductItem[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const matchText = ref('')
const form = reactive<Partial<VirtualProductItem>>({ status: true })
const query = reactive({ pageNo: 1, pageSize: 10, productModel: '', startDate: '', endDate: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await auxiliaryApi.queryVirtualProducts(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const save = async () => {
  await auxiliaryApi.saveVirtualProduct(form)
  ElMessage.success('虚拟产品保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async (id: string) => {
  await auxiliaryApi.deleteVirtualProduct(id)
  ElMessage.success('删除成功')
  loadData()
}

const onParsed = async (rows: Record<string, unknown>[]) => {
  await auxiliaryApi.importVirtualProducts(rows as unknown as VirtualProductItem[])
  ElMessage.success('导入成功')
  loadData()
}

const verifyMatch = async () => {
  const res = await auxiliaryApi.matchVirtualProduct(query.productModel)
  matchText.value = `匹配回复内容：${res.data.autoReplyContent || '未命中'}`
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header><div class="panel-title">虚拟产品管理</div></template>
    <el-form inline>
      <el-form-item label="产品型号"><el-input v-model="query.productModel" /></el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      <el-form-item><el-button @click="verifyMatch">匹配验证</el-button></el-form-item>
      <el-form-item><el-button @click="dialogVisible = true">新增</el-button></el-form-item>
      <el-form-item><TemplateDownload name="virtual-product" :columns="['productModel','autoReplyContent','status']" /></el-form-item>
      <el-form-item><ExcelImport @parsed="onParsed" /></el-form-item>
      <el-form-item><el-button @click="auxiliaryApi.exportVirtualProducts(query)">导出</el-button></el-form-item>
    </el-form>

    <MatchHint v-if="matchText" :text="matchText" type="success" />

    <el-table :data="list" border row-key="id" v-loading="loading">
      <el-table-column prop="productModel" label="产品型号" min-width="160" />
      <el-table-column prop="autoReplyContent" label="自动回复内容" min-width="260" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="100"><template #default="scope"><el-button link type="danger" @click="remove(scope.row.id)">删除</el-button></template></el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @change="loadData" />
    </div>

    <el-dialog v-model="dialogVisible" title="虚拟产品" width="520px">
      <el-form label-width="100px">
        <el-form-item label="产品型号"><el-input v-model="form.productModel" /></el-form-item>
        <el-form-item label="自动回复"><el-input v-model="form.autoReplyContent" type="textarea" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.panel-title { font-size: 16px; font-weight: 700; }
.pager-wrap { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
