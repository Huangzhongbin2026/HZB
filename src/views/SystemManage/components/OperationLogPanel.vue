<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemLogApi } from '@/api/modules/system/log'
import type { SysOperationLog } from '@/types/system'

const logs = ref<SysOperationLog[]>([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  operUser: '',
  operType: '',
  operModule: '',
  operIp: '',
  keyword: '',
  startTime: '',
  endTime: '',
})

const loadLogs = async () => {
  const res = await systemLogApi.query(query)
  logs.value = res.data.list
  total.value = res.data.total
}

const exportLogs = async () => {
  await systemLogApi.export(query)
  ElMessage.success('日志导出任务已触发')
}

const cleanLogs = async () => {
  await ElMessageBox.confirm('仅超级管理员可清理，是否继续?', '提示', { type: 'warning' })
  await systemLogApi.clean(query.startTime, query.endTime)
  ElMessage.success('日志清理完成')
  loadLogs()
}

onMounted(loadLogs)
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="panel-title">系统日志管理</div>
    </template>

    <el-form inline>
      <el-form-item label="操作人"><el-input v-model="query.operUser" /></el-form-item>
      <el-form-item label="类型"><el-input v-model="query.operType" /></el-form-item>
      <el-form-item label="模块"><el-input v-model="query.operModule" /></el-form-item>
      <el-form-item label="IP"><el-input v-model="query.operIp" /></el-form-item>
      <el-form-item label="关键字"><el-input v-model="query.keyword" /></el-form-item>
      <el-form-item><el-button type="primary" @click="loadLogs">查询</el-button></el-form-item>
      <el-form-item><el-button @click="exportLogs">导出Excel</el-button></el-form-item>
      <el-form-item><el-button type="danger" @click="cleanLogs">清理日志</el-button></el-form-item>
    </el-form>

    <el-table :data="logs" border row-key="id">
      <el-table-column prop="operUser" label="操作人" min-width="100" />
      <el-table-column prop="operTime" label="操作时间" min-width="170" />
      <el-table-column prop="operIp" label="IP" min-width="120" />
      <el-table-column prop="operType" label="类型" min-width="100" />
      <el-table-column prop="operModule" label="模块" min-width="140" />
      <el-table-column prop="operContent" label="内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="operResult" label="结果" min-width="90" />
    </el-table>

    <div class="pager-wrap">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @change="loadLogs"
      />
    </div>
  </el-card>
</template>

<style scoped>
.panel-title {
  font-size: 16px;
  font-weight: 700;
}

.pager-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
