<script setup lang="ts">
import { ref } from 'vue'
import LeaveConfigPanel from './components/LeaveConfigPanel.vue'
import VirtualProductPanel from './components/VirtualProductPanel.vue'
import MessagePushPanel from './components/MessagePushPanel.vue'
import LeaveAgentProductPanel from './components/LeaveAgentProductPanel.vue'
import AreaCoordinatorPanel from './components/AreaCoordinatorPanel.vue'

const activeTab = ref('leave')
</script>

<template>
  <div class="page-wrap">
    <header class="page-header">
      <h1>辅助功能模块</h1>
      <p>请假配置、虚拟产品、消息推送、代理产品、区域统筹统一配置</p>
    </header>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane name="leave" label="请假配置"><LeaveConfigPanel /></el-tab-pane>
      <el-tab-pane name="virtual" label="虚拟产品管理"><VirtualProductPanel /></el-tab-pane>
      <el-tab-pane name="message" label="消息推送管理"><MessagePushPanel /></el-tab-pane>
      <el-tab-pane name="agent" label="请假代理产品型号管理"><LeaveAgentProductPanel /></el-tab-pane>
      <el-tab-pane name="area" label="区域统筹划分管理"><AreaCoordinatorPanel /></el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.page-wrap {
  min-height: 100vh;
  padding: 16px;
  background: linear-gradient(160deg, #ecfeff, #f8fafc 40%, #ecfccb);
}
.page-header {
  margin-bottom: 16px;
  padding: 18px;
  border: 1px solid #bbf7d0;
  border-radius: 14px;
  background: #ffffffd9;
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
}
</style>
