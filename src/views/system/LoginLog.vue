<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listLoginLogs } from '../../api/system'
import type { LoginLogItem } from '../../types/auth'

const loading = ref(false)
const tableData = ref<LoginLogItem[]>([])

const loadData = async () => {
  loading.value = true
  tableData.value = await listLoginLogs()
  loading.value = false
}

onMounted(loadData)
</script>

<template>
  <el-card>
    <template #header>
      <div class="header-row">
        <span>系统日志 - 登录日志</span>
        <el-button v-permission="'btn:log:query'" @click="loadData">刷新</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="location" label="登录地点" width="120" />
      <el-table-column prop="status" label="结果" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'">
            {{ scope.row.status === 'success' ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="time" label="时间" width="180" />
    </el-table>
  </el-card>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
