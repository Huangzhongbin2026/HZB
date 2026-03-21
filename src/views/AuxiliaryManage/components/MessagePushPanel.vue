<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { MessagePushItem } from '@/types/auxiliary'
import { auxiliaryApi } from '@/api/modules/system/auxiliary'
import TemplateDownload from './common/TemplateDownload.vue'
import ExcelImport from './common/ExcelImport.vue'

const list = ref<MessagePushItem[]>([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const form = reactive<Partial<MessagePushItem>>({ isEnabled: true })
const query = reactive({ pageNo: 1, pageSize: 10, pushName: '', isEnabled: '', startDate: '', endDate: '' })

const loadData = async () => {
  loading.value = true
  try {
    const res = await auxiliaryApi.queryMessagePushes(query)
    list.value = res.data.list
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const save = async () => {
  await auxiliaryApi.saveMessagePush(form)
  ElMessage.success('消息推送配置保存成功')
  dialogVisible.value = false
  loadData()
}

const remove = async (id: string) => {
  await auxiliaryApi.deleteMessagePush(id)
  ElMessage.success('删除成功')
  loadData()
}

const onParsed = async (rows: Record<string, unknown>[]) => {
  await auxiliaryApi.importMessagePushes(rows as unknown as MessagePushItem[])
  ElMessage.success('导入成功')
  loadData()
}

onMounted(loadData)
</script>

<template>
  <el-card shadow="never">
    <template #header><div class="panel-title">消息推送管理</div></template>
    <el-form inline>
      <el-form-item label="推送名称"><el-input v-model="query.pushName" /></el-form-item>
      <el-form-item label="是否开启"><el-select v-model="query.isEnabled" clearable style="width: 120px"><el-option label="是" value="true" /><el-option label="否" value="false" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">查询</el-button></el-form-item>
      <el-form-item><el-button @click="dialogVisible = true">新增</el-button></el-form-item>
      <el-form-item><TemplateDownload name="message-push" :columns="['pushName','routeCode','feishuTemplateCode','isEnabled']" /></el-form-item>
      <el-form-item><ExcelImport @parsed="onParsed" /></el-form-item>
      <el-form-item><el-button @click="auxiliaryApi.exportMessagePushes(query)">导出</el-button></el-form-item>
    </el-form>

    <el-table :data="list" border row-key="id" v-loading="loading">
      <el-table-column prop="pushName" label="推送名称" min-width="150" />
      <el-table-column prop="routeCode" label="路由代码" min-width="170" />
      <el-table-column prop="feishuTemplateCode" label="飞书模板" min-width="150" />
      <el-table-column prop="isEnabled" label="是否开启" width="100" />
      <el-table-column label="操作" width="100"><template #default="scope"><el-button link type="danger" @click="remove(scope.row.id)">删除</el-button></template></el-table-column>
    </el-table>

    <div class="pager-wrap"><el-pagination v-model:current-page="query.pageNo" v-model:page-size="query.pageSize" :total="total" layout="total, prev, pager, next" @change="loadData" /></div>

    <el-dialog v-model="dialogVisible" title="消息推送配置" width="520px">
      <el-form label-width="120px">
        <el-form-item label="推送名称"><el-input v-model="form.pushName" /></el-form-item>
        <el-form-item label="路由代码"><el-input v-model="form.routeCode" /></el-form-item>
        <el-form-item label="飞书模板编码"><el-input v-model="form.feishuTemplateCode" /></el-form-item>
        <el-form-item label="推送规则"><el-input v-model="form.pushRule" type="textarea" /></el-form-item>
        <el-form-item label="开启"><el-switch v-model="form.isEnabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.panel-title { font-size: 16px; font-weight: 700; }
.pager-wrap { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
