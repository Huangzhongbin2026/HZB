<script setup lang="ts">
import { computed, ref } from 'vue'
import UserTaskWorkbench from './components/user/UserTaskWorkbench.vue'
import OrderUrgentManagement from './components/user/OrderUrgentManagement.vue'
import UnorderedConsultManagement from './components/user/UnorderedConsultManagement.vue'
import DeliveryChangeManagement from './components/user/DeliveryChangeManagement.vue'
import UserOperationRecordManagement from './components/user/UserOperationRecordManagement.vue'
import AuxiliaryManageIndex from '@/views/AuxiliaryManage/index.vue'
import SystemManageIndex from '@/views/SystemManage/index.vue'

const isManagement = computed(() => {
  const search = new URLSearchParams(window.location.search)
  return search.get('mode') === 'management'
})

const queryTab = new URLSearchParams(window.location.search).get('tab')
const activeManagementTab = ref(
  queryTab === 'urgent' || queryTab === 'delivery' || queryTab === 'operation' || queryTab === 'auxiliary' || queryTab === 'system'
    ? queryTab
    : 'unordered',
)

const menuItems = [
  { key: 'unordered', label: '未下单交期评估任务' },
  { key: 'urgent', label: '已下单加急任务' },
  { key: 'delivery', label: '客期变更任务' },
  { key: 'operation', label: '用户操作记录' },
  { key: 'auxiliary', label: '辅助功能' },
  { key: 'system', label: '系统管理' },
] as const

const switchTab = (tab: (typeof menuItems)[number]['key']) => {
  activeManagementTab.value = tab
  const url = new URL(window.location.href)
  url.searchParams.set('mode', 'management')
  url.searchParams.set('tab', tab)
  window.history.replaceState({}, '', url)
}
</script>

<template>
  <div v-if="isManagement" class="manage-shell">
    <header class="manage-header">
      <div class="header-title">任务管理平台</div>
      <div class="header-subtitle">经典管理后台视图 · 统一菜单与模块切换</div>
    </header>

    <div class="manage-page">
      <aside class="manage-sidebar">
        <div class="menu-group-title">功能导航</div>
        <button
          v-for="item in menuItems"
          :key="item.key"
          class="menu-item"
          :class="{ active: activeManagementTab === item.key }"
          @click="switchTab(item.key)"
        >
          {{ item.label }}
        </button>
      </aside>

      <section class="manage-main">
        <UnorderedConsultManagement v-if="activeManagementTab === 'unordered'" />
        <OrderUrgentManagement v-else-if="activeManagementTab === 'urgent'" />
        <DeliveryChangeManagement v-else-if="activeManagementTab === 'delivery'" />
        <UserOperationRecordManagement v-else-if="activeManagementTab === 'operation'" />
        <AuxiliaryManageIndex v-else-if="activeManagementTab === 'auxiliary'" />
        <SystemManageIndex v-else />
      </section>
    </div>
  </div>
  <UserTaskWorkbench v-else />
</template>

<style scoped>
.manage-shell {
  position: fixed;
  inset: 0;
  z-index: 70;
  background: #f3f5f9;
  display: grid;
  grid-template-rows: 62px 1fr;
}

.manage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  border-bottom: 1px solid #e6ebf3;
  background: #ffffff;
}

.header-title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2a44;
}

.header-subtitle {
  font-size: 13px;
  color: #6b7892;
}

.manage-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px 1fr;
}

.manage-sidebar {
  background: linear-gradient(180deg, #1d2e57, #1a2543 55%, #17203a);
  color: #dce6fb;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.08);
}

.menu-group-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  opacity: 0.78;
  padding: 8px 10px;
}

.menu-item {
  border: 0;
  text-align: left;
  color: #d6def3;
  background: transparent;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.menu-item:hover {
  background: rgba(130, 164, 236, 0.2);
}

.menu-item.active {
  color: #fff;
  background: linear-gradient(90deg, #3f63c9, #3150aa);
  box-shadow: 0 6px 14px rgba(43, 83, 181, 0.35);
}

.manage-main {
  padding: 14px 16px;
  overflow: auto;
}

@media (max-width: 960px) {
  .manage-shell {
    grid-template-rows: auto 1fr;
  }

  .manage-header {
    padding: 10px 12px;
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }

  .manage-page {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }

  .manage-sidebar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .menu-group-title {
    grid-column: 1 / -1;
    padding: 0;
  }
}
</style>
