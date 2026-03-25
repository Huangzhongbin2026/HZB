<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()

const summaryCards = computed(() => [
  {
    title: '当前角色',
    value: authStore.currentProfile.label,
  },
  {
    title: '页面权限数',
    value: authStore.permissionCodes.filter(code => code.startsWith('page:')).length,
  },
  {
    title: '按钮权限数',
    value: authStore.permissionCodes.filter(code => code.startsWith('btn:')).length,
  },
  {
    title: '字段权限项',
    value: Object.keys(authStore.fieldPermissions).length,
  },
])
</script>

<template>
  <div class="panel">
    <h3>系统管理模块演示</h3>
    <p>该模板提供角色、用户、权限、日志、字典五大模块，支持页面级、按钮级、字段级权限控制。</p>

    <el-row :gutter="14">
      <el-col v-for="card in summaryCards" :key="card.title" :xs="24" :sm="12" :md="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric-title">{{ card.title }}</div>
          <div class="metric-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert type="info" :closable="false" show-icon>
      可在顶部切换“管理员/运营/审计”视角，立即观察菜单、按钮和字段展示变化。
    </el-alert>
  </div>
</template>

<style scoped>
.panel {
  display: grid;
  gap: 14px;
}

h3 {
  margin: 0;
}

.metric-card {
  margin-bottom: 14px;
}

.metric-title {
  color: #909399;
  font-size: 13px;
}

.metric-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  color: #1f2937;
}
</style>
