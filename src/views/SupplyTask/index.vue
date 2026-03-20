<script setup lang="ts">
import { ref } from 'vue'
import TaskSubmitForm from './components/TaskSubmitForm.vue'
import TaskListPanel from './components/TaskListPanel.vue'
import ConfigPanel from './components/ConfigPanel.vue'
import DashboardPanel from './components/DashboardPanel.vue'

const activeTab = ref('submit')
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <h1>供应链统筹任务管理平台</h1>
      <p>订单加急、未下单评估、客期变更统一在线处理</p>
    </header>

    <el-tabs v-model="activeTab" class="main-tabs" type="border-card">
      <el-tab-pane name="submit" label="任务提交">
        <TaskSubmitForm />
      </el-tab-pane>
      <el-tab-pane name="list" label="任务列表">
        <TaskListPanel />
      </el-tab-pane>
      <el-tab-pane name="config" label="配置管理">
        <ConfigPanel />
      </el-tab-pane>
      <el-tab-pane name="dashboard" label="数据大屏">
        <DashboardPanel />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.page-wrap {
  min-height: 100vh;
  padding: 16px;
  background: linear-gradient(160deg, #ecfeff, #f8fafc 40%, #fffbeb);
}

.page-header {
  margin-bottom: 16px;
  padding: 18px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: #ffffffcc;
  backdrop-filter: blur(2px);
}

.page-header h1 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  font-weight: 800;
}

.page-header p {
  margin: 6px 0 0;
  color: #475569;
  font-size: 14px;
}

.main-tabs :deep(.el-tabs__content) {
  padding: 14px;
}
</style>
