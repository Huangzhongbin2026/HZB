<script setup lang="ts">
import { onMounted } from 'vue'

import AppSidebar from '../components/AppSidebar.vue'
import GraphMapPanel from '../components/GraphMapPanel.vue'
import { useWorkspaceStore } from '../stores/workspace'

const store = useWorkspaceStore()

onMounted(async () => {
  await store.bootstrap()
  if (store.isAuthenticated && store.notes.length === 0) {
    await store.loadWorkspace()
  }
})
</script>

<template>
  <main class="page-shell">
    <AppSidebar />
    <section class="page-main">
      <GraphMapPanel />
    </section>
  </main>
</template>