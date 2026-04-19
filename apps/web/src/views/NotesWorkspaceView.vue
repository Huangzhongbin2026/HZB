<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppSidebar from '../components/AppSidebar.vue'
import NoteDirectoryPanel from '../components/NoteDirectoryPanel.vue'
import NoteEditorPanel from '../components/NoteEditorPanel.vue'
import { useWorkspaceStore } from '../stores/workspace'

const route = useRoute()
const router = useRouter()
const store = useWorkspaceStore()

async function syncRouteToNote() {
  await store.bootstrap()
  if (store.isAuthenticated && store.notes.length === 0) {
    await store.loadWorkspace()
  }

  const slug = typeof route.params.slug === 'string' ? route.params.slug : ''
  if (slug) {
    await store.selectNote(slug)
    return
  }

  if (store.sortedNotes.length > 0) {
    router.replace(`/notes/${store.sortedNotes[0].slug}`)
  }
}

onMounted(async () => {
  await syncRouteToNote()
})

watch(
  () => route.params.slug,
  async () => {
    await syncRouteToNote()
  },
)
</script>

<template>
  <main class="page-shell">
    <AppSidebar />
    <section class="page-main page-main--notes">
      <div class="notes-grid">
        <NoteDirectoryPanel />
        <NoteEditorPanel />
      </div>
    </section>
  </main>
</template>
