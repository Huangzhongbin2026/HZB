<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

import AppSidebar from '../components/AppSidebar.vue'
import ClipInboxPanel from '../components/ClipInboxPanel.vue'
import HomeOverviewPanel from '../components/HomeOverviewPanel.vue'
import HomeSearchPanel from '../components/HomeSearchPanel.vue'
import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()
let timer: number | undefined

onMounted(async () => {
  await store.bootstrap()
  if (store.isAuthenticated) {
    await store.loadWorkspace()
  }
  timer = window.setInterval(() => {
    store.pollSyncStatus()
  }, 4000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <main class="page-shell">
    <AppSidebar />
    <section class="page-main page-main--home">
      <div class="home-grid">
        <HomeSearchPanel />
        <ClipInboxPanel />
        <HomeOverviewPanel />
      </div>
    </section>
  </main>
</template>
